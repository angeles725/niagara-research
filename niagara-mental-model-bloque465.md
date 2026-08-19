# B465 — JACE-8000 QNX boot sequence and on-flash layout: factory image in read-only NVRAM, /opt/niagara + /home/niagara, niagarad → JVM → stations, and SRAM playback on every boot (focus jace8000, J2)

> **Focus:** `jace8000` (§16). **Gap:** J2 — how does the JACE boot, and how is its flash laid out?
> **Phase:** §12 dynamic + `[CERT-doc]`/`[CERT]`. Read-only. `live-install` → SECRETS DISCIPLINE.
> **Block type: EVIDENCE (synthesis of doc + corpus).**
> **Sources:** `[CERT]` corpus [Block 124] (native boot chain) · `[CERT-doc]` niagara-help
> (`AXtoN4Migration/ControllerPlatformHomes`, `DataRecoverySvc/aDataRecoverySvc_DataSettings`) · [Block 462]/
> [Block 463] (this focus).
>
> **Bottom line:** the JACE boots **QNX Neutrino from onboard flash**, runs the **niagarad** daemon which
> starts a **JVM** and then launches each station as a **separate OS process**; a pristine **factory image
> sits in read-only NVRAM** (the source of factory recovery), and the **battery-backed SRAM** is *played back*
> on every boot for crash-safe data. The boot loader also hosts the **Alternate Boot Options** recovery menu
> (ESC during boot) that J7 uses.

## §465.1 — The boot chain

Onboard boot (QNX): IPL/boot loader → **QNX Neutrino kernel** → the Niagara **niagarad** platform daemon.
The corpus decompiled this chain on the Windows supervisor; the JACE runs the QNX build of the same binaries,
so the *symbol-level* sequence is corpus-certified and the *OS wrapper* is QNX:

- `[CERT]` [Block 124] — `niagarad` → `njre` `JavaLauncher` starts the **JVM** → `NiagaraDaemon` (Java) loads
  `nre` as a JNI library → `daemonize0` registers the platform service. Verbatim daemon trace: "**niagarad:
  Niagara service startup initiated** → … **successfully daemonized** → … **startup complete, set service
  status to running**" ([Block 124] §, from the binary's debug strings).
- `[CERT]` [Block 124] — the daemon does **not** exec the station in-process; **`station` is its own launcher
  EXE booting an independent JVM** running `com.tridium.sys.station.Station`. **The daemon manages stations as
  separate OS processes** (auto-start/auto-restart via `daemon.properties`, [Block 123]). On the JACE this
  means: one niagarad process + one JVM process per running station.
- `[CERT]` [Block 124] — **HostId and signature checks run inside the native launcher, before/around VM
  creation** (`checkFileSignature`/`isProductionBuild` from `dsfspi`), so licensing/integrity gate the boot
  itself — the JACE equivalent binds to hardware (J10).

## §465.2 — On-flash layout

| Region | Purpose | Evidence |
|---|---|---|
| **QNX OS partition** | the Neutrino kernel + platform binaries (`niagarad`, `station`, `nre`) | [Block 459]/[Block 124] |
| **`/opt/niagara`** (System Home) | the Niagara install: runtime, modules, JRE | `[CERT-doc]` [Block 462] |
| **`/home/niagara`** (daemon User Home) | **configuration + the installed/running station** (`stations/<n>/config.bog`, `etc/`) | `[CERT-doc]` [Block 462] |
| **Factory image** | a **pristine image in non-volatile, read-only memory** | `[CERT-doc]` [Block 463] §463.4 |
| **Battery-backed NVSRAM** | RTC + crash-safe recorded data, *played back on boot* | `[CERT-doc]` §465.3 |

The read-only factory image is the architectural reason factory recovery ([Block 463]) needs no USB/cable:
the system "pulls the factory image from non-volatile, read-only memory." User data (`/home/niagara`) and the
install (`/opt/niagara`) are separate writable regions, which is why credential-reset recovery can wipe
*credentials* while preserving the *station* on the writable side.

## §465.3 — SRAM playback on every boot (crash-safe data)

`[CERT-doc]` `DataRecoverySvc/aDataRecoverySvc_DataSettings.txt:63,69` — the DataRecoveryService reacts "**upon
any controller boot sequence in which SRAM recorded data is discovered and played back**." The JACE's
battery-backed SRAM captures recent state so that after a power loss the boot **replays** it into the station
(histories/points that would otherwise be lost between save cycles). This is a hardware feature a PC install
does not have, and it is why the JACE tolerates abrupt power loss better than a soft OS.

## §465.4 — The recovery hook lives in the boot loader

`[CERT-doc]` [Block 463] §463.2 — pressing **ESC during boot** enters **recovery mode → Alternate Boot
Options**, the menu whose option 8 resets platform credentials. So the same boot path that normally reaches
niagarad is also the entry to the serial recovery console (J7). Factory recovery (BACKUP button held at
power-up, [Block 463] §463.4) is the other boot-time branch. Both are pre-OS, below any Niagara login.

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | niagarad → JVM → NiagaraDaemon → nre JNI → daemonize | [CERT] | [Block 124] | ✓ corpus |
| 2 | stations run as separate OS processes managed by the daemon | [CERT] | [Block 124]/[Block 123] | ✓ corpus |
| 3 | HostId + signature checked in the native launcher at boot | [CERT] | [Block 124] | ✓ corpus |
| 4 | /opt/niagara install + /home/niagara config+station | [CERT-doc] | [Block 462] | ✓ |
| 5 | factory image in non-volatile read-only memory | [CERT-doc] | [Block 463] §463.4 | ✓ |
| 6 | SRAM recorded data played back on every boot | [CERT-doc] | aDataRecoverySvc_DataSettings:63,69 | ✓ token |
| 7 | ESC at boot → Alternate Boot Options recovery menu | [CERT-doc] | [Block 463] §463.2 | ✓ |

Marker tally: [CERT] ×3 (corpus) · [CERT-doc] ×4 · [INFER] 0 load-bearing (QNX wrapper of the Windows-observed
symbol chain is stated as such). **Block type: EVIDENCE.** Ratio ≈ 0.

## Connections

- **[Block 124]/[Block 123]** (`platform-native`) — the decompiled boot chain (Windows); this is its QNX-side,
  flash-layout complement.
- **[Block 462]** — the filesystem roots; **[Block 463]** — the boot-time recovery branches.
- Forward: **J10** (HostId at boot / licensing), **J6** (passphrase-sealed data on the writable side).

## Open gaps

Queued: J6, J10, J11, J9, and the child gaps J3-G1/J5-G1/J7-G1/J8-G1 (requires-execution). New child
**J2-G1** (requires-execution): the exact QNX partition/mount table (`/mnt*`) — a live serial-console capture.
