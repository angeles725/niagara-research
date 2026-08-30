# B687 — JACE_UMBRELLA NrioNetwork (SC3): the field controller drives exactly ONE physical point (a relay output), on an IO-34 that was DOWN at last snapshot

> Focus: **jace-station-config** · Gap **SC3** (NrioNetwork deployed config — the physical field IO). Sources:
> `config.bog` file.xml `/Drivers/NrioNetwork` (L873–953), extracted READ-ONLY from the JACE-8000 boot microSD.
> Redacted evidence: `sources/probes/B685-jace-station-config/nrionetwork-io.txt`.
> **SECRETS DISCIPLINE (live-install):** structure only; module UID + station IP masked. Marker `[CERT-hw]`
> (SD artifact). NRIO DRIVER internals (libplatnrio JNI/wire) = REMITTANCE focus `jace8000-qnx-native`
> [Block 680]; this block is the DEPLOYED wiring, not the driver.

## 687.1 — Transport: one RS-485 bus on COM1

[CERT-hw] `<p n="NrioNetwork" m="nrio=nrio" t="nrio:NrioNetwork">` (L873), `portName="COM1"` (L893). Bound to
the platform serial service `platSerialQnx:SerialPortPlatformServiceQnx` and `platNrio:NrioPlatformServiceQnx`
(station services NameList, L819). Health monitor `d:PingMonitor` with `pingFrequency=30000` ms (L879). Poll =
`basic:BasicPollScheduler` (L891); write path = `basic:BasicCoalescingWorker` (L889); `nrio:OutputFailsafeConfig`
present but empty → defaults (L894).

[INFER] Baud rate is NOT serialized into the BOG (`grep -c baud` = 0) — the NRIO RS-485 line runs at the driver
default (not recorded on disk; would need the live serial service or the driver doc to confirm the exact rate).

## 687.2 — One IO-34 module, dual-address, offline at last snapshot

[CERT-hw]

| Module | type | bus address | FW | health |
|---|---|---|---|---|
| io34_1_2 | nrio:Nrio34Module | 1 (primary) | 2.2 | `lastFailCause="nrioService reports device is down"` (L907) |
| io34_1_2/io34Sec | nrio:Nrio34SecModule | 2 (secondary) | 2.2 | `lastFailCause="…device is down"` (L934) |

The IO-34 is a **dual-address** NRIO board: primary + secondary occupy two consecutive RS-485 addresses (1 and
2, L914/L941). Both share one hardware UID (masked). **Both reported DOWN** at the last health snapshot
(2026-08-19T02:14 −06:00) — the same timestamp the station was last persisted (B685). So at the moment this SD
was imaged, the field module was not communicating. [INFER: this is a point-in-time health record on disk, not
proof the module is permanently dead — a live ping would be needed to confirm current state.]

## 687.3 — Exactly one commissioned point: a relay output

[CERT-hw] Under the primary module's `nrio:Nrio34PriSecPoints` (L918), a **single** point is configured:
`<p n="ro1" t="c:BooleanWritable">` (L920) with `proxyExt t="nrio:NrioRelayOutputProxyExt"` (L921) and
`fallback` value `false` (L924). The secondary module's points container (L945) is **empty**.

- `ro1` is a **relay OUTPUT** (`BooleanWritable` + `NrioRelayOutputProxyExt`) — a control point, not a sensor
  read. Boolean on/off. No conversion, no alarm ext, no history ext attached.
- **Total commissioned physical IO on this JACE = 1 point** (1 relay output; 0 digital inputs, 0 universal
  inputs, 0 analog outputs configured).

[INFER] Functionally, a lone relay output = an on/off actuator (contactor, motor starter, valve, or a status
lamp). The IO-34 hardware carries far more channels than the one wired here; the exact channel inventory of the
IO-34 board is not in this config (it is a hardware/datasheet fact, not a BOG fact) — not load-bearing for the
deployed-config question.

## 687.4 — What this says about the deployment

[CERT-hw]+[INFER] One IO-34 (down), one commissioned relay point, plus the provisioning-template marker
(`NewJACEProvisioningStation.ntpl v1.5`, B685 §685.4) and the standalone NiagaraNetwork (B686): this is a
**minimally-commissioned seed station**, not a fully populated field controller. It was provisioned from a
template, given one relay output, and (per the down status + no supervisor join) was not yet in production
service — or was captured mid-commissioning. This is consistent across all three SC gaps closed so far: template
defaults everywhere, near-zero site-specific build-out.

## Connections

- NRIO driver internals (libplatnrio, JNI, /dev, wire) → focus `jace8000-qnx-native` [Block 680]; framework
  driver model → focus `framework-drivers`. Field-bus platform services (BACnet MS/TP, NRIO, serial) →
  [Block 680]. Station skeleton + template marker → [Block 685]; standalone NiagaraNetwork → [Block 686]
  (this focus).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | NrioNetwork on COM1, PingMonitor 30 s | [CERT-hw] | L873/L893/L879 | grep-confirmed |
| 2 | one IO-34, addresses 1 (pri) + 2 (sec), FW 2.2 | [CERT-hw] | L904/L914/L931/L941 | grep-confirmed |
| 3 | both boards lastFailCause "device is down" | [CERT-hw] | L907/L934 | grep-confirmed |
| 4 | exactly one point: ro1 = BooleanWritable relay OUTPUT | [CERT-hw] | L920/L921 | grep-confirmed |
| 5 | secondary module points empty; 1 total commissioned IO | [CERT-hw] | L945 | grep-confirmed |
| 6 | baud not in BOG (driver default) | [INFER] | grep -c baud = 0 | measured |
| 7 | minimally-commissioned seed station | [INFER] | 687.2–687.3 + B685/B686 | reasoned |

**Tally:** [CERT-hw] ×5 · [INFER] ×2. Ratio 0.4. Block TYPE = **EVIDENCE**. The IO subtree is small and fully
read — this gap's on-disk evidence is exhausted (1 module, 1 point); the ratio reflects the two honest
[INFER]s (baud default, seed-station read), not missing evidence. 5/5 load-bearing citations grep-confirmed.
Evidence file secret-scan: clean (UID + IP masked).

## Open gaps (this focus)

SC3 CLOSED. Next: **SC4** (deployed RBAC — UserService/RoleService/CategoryService/AuthenticationService: the
real users, roles, categories, auth schemes; structure only).
