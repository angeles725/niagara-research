# Block 850 — "The license update broke comms": a JACE-8000 outage that was really a hung I/O subsystem (fix = cold power-cycle)

> **Focus:** backup-licensing-ops (document-mode §20). **Scope:** a full field-incident post-mortem — after a
> **license import + reboot** on a Distech EC-Net **JACE-8000 (QNX TITAN, N4.3.58.18)**, *all* BACnet devices
> dropped (both MS/TP and BACnet/IP). The operator's hypothesis was the license; the evidence walked it back,
> step by step, to a **hung serial + IP I/O subsystem on the JACE**, and the fix was a **physical
> power-cycle (de-energize / re-energize)** — not a station restart, not a config change, not a license
> revert. Captures the exoneration chain (so the next person doesn't chase the license) and the diagnostic
> decision tree. Sibling to B838 (backup/restore/licensing mechanics) and B849 (opening a copied station).
>
> **Live context `[CERT-live]` (operator, HARBOR / Distech-Mérida, 2026-09-08→09):** station **HM_Central**,
> Distech EC-Net 4 Pro JACE-8000, `Qnx-TITAN-788A-3418-A025-621D`, serial `80037018`, N4.3.58.18. Runs a
> **Distech BCS v3** BACnet stack: **BACnet/IP** (local dnet1 = 192.168.1.102, remote dnet2001 via a router
> at 192.168.1.68) + **6 MS/TP trunks** (dnet2–7, ports COM1–COM6; COM6 = ModbusAsyncNetwork). Field is
> Distech `ECB-PTU-107` / `ECB-203` room controllers + Alerton VLC + Setra, MACs 1–91.
>
> **Sources:**
> - **FUENTE 1 (corpus):** B838 (license = XML entitlement, ceiling-not-floor; restore wipes TCP/IP;
>   station-restart≠reboot), B849 (copied-station module/BOG walls). SECRETS DISCIPLINE: Host IDs are public
>   license identifiers, cited; no secret values.
> - **FUENTE 2 (official doc + vendor + forums, `[CERT-doc]`/`[CERT-web]`):** JACE-8000 `NPB-8000-232 Install
>   Sheet` (COM1/COM2 onboard fixed, option modules COM3+ by physical proximity, max 4 modules); Distech
>   `ECB-PTU-107` datasheet (BACnet MS/TP B-ASC room controller); Schneider/HVAC-Talk/Chipkin threads on
>   JACE serial-ports-after-upgrade, "Cannot send Bacnet/IP packet", MS/TP Max Master / duplicate MAC.
> - **FUENTE 3 (operator artifacts, `[CERT-live]`):** the station console logs (BACnet timeouts, `Host is
>   down`, `devctl rx frame failed`, `DCMD_MSTP_TX_FRAME errno 264`, `PortDeniedException`); the two
>   `distech.license` files (JACE + Supervisor); the Bacnet Router Table + per-port MstpPort views. Hashes in
>   `sources/SOURCES.md`.

---

## 1. The event and the symptom

A license was imported on the JACE on 2026-09-08 (no station reset chosen at import). Afterward the client
reported "lost comms with many controllers." The station log showed a steady storm of BACnet failures across
**every** subsystem:

- **BACnet/IP:** `SEVERE [bacnet.link.ip] Cannot send Bacnet/IP packet! java.io.IOException: Host is down`
  at `DatagramSocket.send` — an **OS-level** failure to route UDP to the field (e.g. `GM04ilum` at
  192.168.1.24), even though the JACE's own primary is 192.168.1.102 on the same /24.
- **BACnet MS/TP:** `Transaction Timed out` pinging devices on dnet2–6, plus at the link layer
  `[bacnet.link.mstpN] Cannot send MSTP packet! commStarted=false`, `devctl rx frame failed`,
  `DCMD_MSTP_TX_FRAME returned 11 (errno 264)`.

Devices down spanned ECB-PTU-107, ECB-203, Alerton, Setra across all trunks + IP — i.e. essentially
everything at once. `[CERT-live]`

## 2. License exoneration (twice)

The operator suspected the new license and wanted to revert to the old one. Two independent proofs it was NOT
the license:

1. **Static diff `[CERT-live]`:** old `v4.4` (SMA 2022-09-07, expired) vs new `v4.15` (SMA 2027-09-06). The
   new license is a **strict superset**: 0 features removed, +9 added (bacnetSc, qnx7, jre8J8000Azul,
   provisioning, securityDashboard, syslog, samlDP, bulkCertSigner/certSigningService). Same
   `hostId`/serial/brand=distech. Every driver `bacnet`/`mstp`/`modbusAsync`/`lonworks`/`niagaraDriver`
   present with `device.limit="none"`; `globalCapacity` identical (5000 pts / 101 devices). A license grants
   MORE, not less.
2. **Empirical `[CERT-live]`:** the operator **reinstalled the old license and nothing changed.** Definitive.

Corollary `[INFER]`: the 9 added features cannot be the cause either — a license feature is a **permission,
not a switch**; it installs/enables nothing. Confirmed the station uses none of them (transport is classic
BACnet/IP + 6 MS/TP + Modbus; `bacnetSc` "hits" in the config were substrings of `BacnetSchedule*`). And the
log proves the station is still on QNX 6.5 / `jre8qnx` / N4.3, so `qnx7`/`Azul` entitlements are dormant.

## 3. The false trails (and why each was wrong)

- **Max Master:** the JACE ports read `Max Master = 60` and a field device sits at `MAC 91`. Tempting, but
  the **known-good backup also had 60** and worked (its top MAC was 62, field controllers report maxMaster
  127, which sustains the ring). Not the cause — leave it at 60. `[CERT-live]`
- **COM renumbering:** ports had shifted (backup BACnet on COM2–COM6, live on COM1–COM5). Re-pointing a port
  to the adjacent COM **did not help** (`PortDeniedException: COMx already owned by BacnetMstp`). Not the fix.
- **Duplicate network number:** ruled out — router table dnets are unique (1, 2, 3, 4, 5, 6, 7). `[CERT-live]`
- **Bacnet Router Table = all "Ok"** on dnet2–6 + IP — the network/config layer was fine; the failure was
  below it, at the hardware TX/RX (`devctl rx frame failed`).

## 4. The real cause: a hung I/O subsystem on the JACE

Two OS-level signatures, both transports at once:
- Serial: `devctl rx frame failed` + `commStarted=false` + `DCMD_MSTP_TX_FRAME errno 264` = the QNX RS-485
  device driver / `platmstp` daemon could not TX/RX frames.
- IP: `Host is down` on UDP send = the network stack had no live route to the field subnet.

The router table showed the ports *configured* Ok, but nothing actually moved on the wire. This is a
**platform I/O hang**, not a BACnet, config, or license condition. It was **triggered** by the reboot around
the license import (the import forces/accompanies a restart), but the license *content* had nothing to do
with it. `[CERT-live]`

## 5. The fix: a physical power-cycle

**De-energize the JACE (power off ~60 s) and re-energize.** That — and only that — resets the QNX serial
driver, the `platmstp` daemon, the UARTs, and the IP stack. **Station restarts did not clear it**, and every
config edit was a detour. After the cold boot, all trunks + IP recovered. `[CERT-live]` 2026-09-09.

## 6. Diagnostic decision tree (for next time)

When a JACE-8000 loses **all** BACnet (MS/TP + IP) after a reboot/import and station-restarts don't help:

1. **Read the log for the LAYER.** `Host is down` (IP) + `devctl rx frame failed` / `commStarted=false`
   (serial) = OS/hardware I/O, **not** BACnet config. `license`/`limit` in a fault = license (rare).
2. **Do the cheap definitive tests before touching config:** revert the license if suspected (if nothing
   changes, it's exonerated — stop chasing it); check the Router Table (all "Ok" ⇒ config layer is fine).
3. **Cold power-cycle the JACE FIRST** (physical de-energize). Highest-yield for a hung I/O subsystem.
4. Only if that fails: field power + shared switch/repeaters (a common-mode outage explains all-trunks+IP
   down), then per-trunk RS-485 (A/B polarity, ref/shield, EOL/bias, MAC uniqueness, Max Master≥highest MAC
   on the field), then binary-search isolation, then reseat the Dual-RS485 option modules, then hardware
   replacement.
5. **Don't** raise Max Master, remap COM ports, or re-import licenses on a hunch — they don't fix an I/O hang
   and they add drift to recover from later.

The two `distech.license` files were verified valid and complete (JACE `Qnx-TITAN-788A-3418-A025-621D` +
Supervisor `Win-D5C2-B74B-692F-A7CE`, both v4.15, SMA 2027, correct host IDs). Note: they are **4.15**
licenses running a **4.3** JACE — fine (version is a ceiling), but do NOT let that tempt a 4.15 upgrade as
part of incident recovery (that is the major QNX6.5→7 + Distech OEM recompile migration; see B838 §7).

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | Outage hit BACnet/IP (`Host is down`) AND MS/TP (`devctl rx frame failed`) simultaneously | [CERT-live] | operator console log 2026-09-09 |
| 2 | New license is a strict superset of old (0 removed, +9 added, same host/drivers/capacity, SMA renewed) | [CERT-live] | diff of the two distech.license files |
| 3 | Reinstalling the OLD license changed nothing → license empirically exonerated | [CERT-live] | operator action 2026-09-09 |
| 4 | A license feature is an entitlement, not a switch; the 9 new features are unused by the station | [INFER]/[CERT-live] | derived from B838 license model + config grep (transport = IP + 6 MSTP + Modbus) |
| 5 | Max Master 60 was the known-good value (backup had 60, top MAC 62, field maxMaster 127) | [CERT-live] | HM_Central 07-Sep backup config |
| 6 | Router Table dnet2–6 + IP all "Ok"; network numbers unique | [CERT-live] | operator Router Table view |
| 7 | Real cause = hung QNX serial driver + IP stack (I/O layer), triggered by the reboot | [CERT-live] | log signatures `devctl rx frame failed`, `errno 264`, `Host is down` |
| 8 | Fix = physical power-cycle of the JACE; station restart does NOT clear it | [CERT-live] | operator confirmed resolution 2026-09-09 |
| 9 | JACE-8000: COM1/COM2 onboard fixed; option modules COM3+ by physical proximity; max 4 modules | [CERT-doc] | NPB-8000-232 Install Sheet |
| 10 | ECB-PTU-107 = Distech BACnet MS/TP B-ASC room controller | [CERT-web] | Distech ECB-PTU-107 datasheet |
| 11 | Both licenses valid/complete: JACE + Supervisor, v4.15, SMA 2027, host IDs match | [CERT-live] | the two distech.license files |

**Tally:** 11 claims — 8 [CERT-live], 1 [CERT-doc], 1 [CERT-web], 1 [INFER]/[CERT-live]. No unmarked assertions.

## Connections
- **B838** — backup/restore/licensing mechanics: license = XML entitlement (ceiling, not floor), restore
  wipes TCP/IP, station-restart≠JACE-reboot, 4.8 = QNX6.5→7 boundary (§7 upgrade warning).
- **B849** — opening a copied station offline (passphrase / modules / clean-folder / password-encoder); same
  HARBOR job, HM_BMS side.
- **jace8000 / jace8000-sd / license-diff** — the platform-RE and license-differential angles B838 lists.

## Open gaps (RESEARCH-STATE-backup-licensing-ops)
- **B850-G1** *(investigable)* — the root cause of the QNX `platmstp` / IP I/O hang on the JACE-8000 after a
  license-import reboot: is it a known Distech/Tridium defect (SD/secure-storage, option-module re-enum, or
  daemon race), and is there a build where it is fixed? Would let us prevent it, not just recover.
