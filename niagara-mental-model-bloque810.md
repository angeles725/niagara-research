# B810 · Driver-module authoring — the BNetwork→BDevice→BPointDeviceExt→BProxyExt hierarchy, BPingMonitor as the device-health "quién vigila", and the critical finding that a write to a DOWN device is SILENTLY DROPPED (our in8 command is lost unless writeOnUp) `[CERT]`

> **Scope** (Excavador depth): the AUTHOR-side of a custom Niagara driver — the 4-class hierarchy you subclass, who
> watches a device's health and how DOWN propagates to its points, the read/write paths + the 16-level priority array,
> and the physically-critical behavior when a device goes DOWN: **a write is dropped, not queued** — so a `BStatusBoolean`
> our control logic BLinks into a proxy point at `in8` never reaches the field on comm loss, and is lost forever unless the
> tuning policy's `writeOnUp` is set. Answers "what happens at the actuator when the device is down." Feeds the kit's
> driver-authoring doctrine + a "link our outputs to proxy points safely" checklist + write-without-fallback lints.
>
> **Sources**: FUENTE 3 (read-only, file:line [CERT], Tridium javadoc `organized/docSource/docSource-doc/vineflower/
> driver-rt/` + `control-rt/` + `modbusTcp-rt`). FUENTE 1 (REMITTANCE, cited not re-derived): [B4] (Baja model), [B15]
> (point/device MANAGER = WB editing, not the hierarchy), [B127] (native driver DLLs), [B496]-[B506] (the 10 Tridium
> protocol drivers as a system), [B761] (Spyder→JACE connect+discovery), [B772] (point extensions — a BProxyExt is one),
> [B775]/[B805] (monitor/alarm — the ping monitor is the driver-level sibling). All load-bearing cites grep-verified.

---

## 810.1 — The hierarchy you subclass `[CERT]`
Four abstract bases, each declaring the type-contract methods an author implements:
- **`BDeviceNetwork extends BComponent implements BIDeviceFolder,BIStatus,BIPingable,BILicensed`** (`BDeviceNetwork.java:129-131`)
  — the driver root; owns the comm + the `monitor` (a `BPingMonitor`). Author overrides `getDeviceType()` (`:354`) and
  `getDeviceFolderType()` (`:359`) — the types its children must be.
- **`BDevice extends BComponent implements BIStatus,BIPingable`** (`BDevice.java:106`) — one field unit. Author overrides
  `getNetworkType()` (`:295`, parentage check) and **`doPing()`** (`:552`, the health probe — must call `pingOk`/`pingFail`).
- **`BPointDeviceExt extends BDeviceExt`** (`BPointDeviceExt.java:33`) — the "Points" folder on a device. Author overrides
  `getProxyExtType()` (`:65`) so the folder enumerates only its own proxy points.
- **`BProxyExt extends BAbstractProxyExt implements BIStatus,BITunable`** (`BProxyExt.java:120`) — the per-point read/write
  bridge; it IS a point extension ([B772] placement). Author overrides `getMode()` (`:368`, readonly/readWrite/writeonly),
  **`readSubscribed(cx)`** (`:859`), **`readUnsubscribed(cx)`**, and **`write(cx)`** (`:883`); the framework calls back
  `readOk`/`readFail`/`writeOk`/`writeFail`. **WHY this shape**: the network centralizes the transport + the health poll;
  the device is the addressable unit that can go down; the point folder groups the proxies; the proxy ext is the only place
  device I/O touches the station graph. Subclass by placement, no central registry ([B802] idiom).

## 810.2 — Who watches a device: `BPingMonitor` + `BPingHealth` (the driver-level "quién vigila") `[CERT; INFER for WHY]`
The network's `monitor` (`BPingMonitor`, `pingEnabled` default true `:128`, `pingFrequency` default 5 min) wakes on a
schedule and calls `doPing()` on the network and each device. `BPingHealth.pingFail(cause)` (`:293`) → `setDown(true)`
(`:305`) → `parent.updateStatus()` (`:306`) → `getMonitor().pingFail(parent)` (`:311`, fires the offnormal alarm after
`numRetriesUntilPingFail`). **DOWN then PROPAGATES to every proxy point**: `BDevice.updateStatus()` (`:367`) →
`exts[i].updateStatus()` (`:400`) → `BPointDeviceExt.updateStatus()` (`:122`) → `((BProxyExt)ext).updateStatus()` (`:129`)
→ `BProxyExt.updateStatus()` (`:462`) sets `if (device.isDown()) newStatus |= BStatus.DOWN` (`:475-476`). **WHY**: a silent
comm failure must SURFACE — the ping is the device heartbeat, and the DOWN bit is how every point learns its source is
unreachable so consumers stop trusting the value. This is the driver-level watcher; the alarm ext ([B805]) and the
author-built liveness monitor (B812) are its siblings at the point and application levels.

## 810.3 — Read path `[CERT]`
On subscribe, `readSubscribed(cx)` (`:859`) registers COV or starts polling; when a value arrives the driver calls
`readOk(value)` (`:895`) → copies to `readValue`, clears stale, `executePoint()`; `onExecute()` converts device→proxy and
sets the point `out`. **WHY subscribed-only**: poll only what is being watched — the resource-budget discipline (companero B806).

## 810.4 — THE CRITICAL FINDING: write path, priority array, and the DOWN drop `[CERT]`
Our control logic writes a `BStatusBoolean` that a `BLink` carries into a `BBooleanWritable` proxy point at **`in8`** — the
READONLY OPERATOR level (`BBooleanWritable.java:490`; numeric sibling `BNumericWritable.in8:443`). The 16-level PRIORITY
ARRAY: effective output = the lowest-numbered `inN` whose status is non-null; if ALL are null → **`fallback`**
(`:726`/`BNumericWritable.fallback:679`). The write reaches the device via `Tuning.writeDesired()` → `Tuning.write()` →
`BProxyExt.write(cx)`.
**THE DROP** `[CERT]`: `Tuning.write()` (`Tuning.java:321`) opens with
`if (writePending() || !isOperational(state()) || !tunable.getMode().isWrite()) return;` (`:332-333`), and
`isOperational` is false whenever the state carries `UNOPERATIONAL = FATAL | DOWN | DISABLED | STOP`. `writeDesired()`
likewise bails: `if (!isOperational(state())) return;` (`:253`). **So when the device is DOWN/FAULT/DISABLED, the write is
SILENTLY DROPPED — it is NOT queued.** The point's `out` keeps showing the last `readValue` with the DOWN bit merged (and
STALE once `staleTime` elapses).
**RECOVERY** `[CERT]`: `transition()` detects DOWN→UP (`isDownToUp`) and re-schedules a write `writeOnDelay` (~5 s)
later (`:201-204`) — but only if the tuning policy's `writeOnUp` is true. **`writeOnUp` DEFAULTS to true**
(`BTuningPolicy.java:206`; siblings `writeOnStart:177`, `writeOnEnabled:235` also default true), so the DOWN→UP re-send is
the DEFAULT — the device-DOWN case is self-correcting unless a policy explicitly disables it (confirmed live: PANCCADIA
inherits the default, §810.8). **If a policy sets `writeOnUp=false`, the field device keeps its pre-outage value AND our
commanded `in8` is never re-sent — a silent, permanent divergence between what our control logic COMMANDED and what the
actuator actually DID.** (The commoner real-world hazard is NOT device-DOWN but a NULL command on stop/reload — §810.8.) **WHY this is the dangerous one (physics/safety)**: our
`step()`/PID thinks the output was delivered (in8 is set, the point shows a value), but on a comm blip the actuator never
moved and won't be corrected — a compressor left running, a valve left open, with no error unless we watch the point's
DOWN/STALE bit. This is the actuator-level failure mode the kit must make impossible to author by accident.

## 810.5 — Discovery / learn `[CERT — brief]`
`driver-rt` has no single discovery interface; discovery is a protocol-specific `BJob`
(`BBacnetDiscoverDevicesJob extends BDeviceManagerJob`) that broadcasts a Who-Is, collects results into a learn table in
the DeviceManager, and the user adds typed devices/points. REMITTANCE to the protocol driver ([B496]-[B506]).

## 810.6 — Minimal custom driver `[CERT]`
Concrete shape (modbusTcp): `BModbusTcpNetwork extends BModbusClientNetwork` (→ BDeviceNetwork) with `getDeviceType()`
(`:25,43`). A minimal driver ships: a `-rt` jar with `BXxxNetwork`/`BXxxDevice`/`BXxxPointDeviceExt`/`BXxxProxyExt` (+ point
types + a `BTuningPolicyMap` named `tuningPolicies` on the network); a `-wb` jar with `BXxxDeviceManager`/`BXxxPointManager`
views ([B809] @AgentOn on the narrow types); a `module.xml` depending on `driver` + `control`; a palette pre-wiring the
network/device/point for drag-drop.

## 810.7 — Kit implication `[INFER, grounded in 810.1-6]`
PROPOSED `types/logic-authoring.md` §"authoring a driver" + a **"link our outputs to proxy points safely" checklist** (for
when our rt logic drives a field point):
1. Write to a chosen priority level (`in8` operator), never `in1` (emergency/persisted).
2. Set the writable point's **`fallback` to a SAFE value** — it is what the point presents when all inputs go null.
3. Set the tuning policy **`writeOnUp = true`** so a device recovery RE-SENDS our commanded value; otherwise the write is
   lost through the outage (§810.4).
4. **Never assume the write landed** — a `BLink` into a DOWN proxy point silently drops the write; our logic must read the
   proxy point's DOWN/STALE bit and treat the command as unconfirmed.
5. Configure the **device-side relinquishDefault** to a safe value (all-inputs-null → RELEASE → the device falls to it).
6. Give the network a `pingEnabled=true` monitor with a sane `pingFrequency` — no ping = no DOWN detection.
**Lint candidates** (extends the [B788]/[B805] lintable/advisory split):
- **HARD**: a writable proxy target driven by OUR output with a null `fallback` (on stop/reload the relay holds its last
  state — resistance/compressor left ON; the top real-world hazard, proven live §810.8); a `BDeviceNetwork` with
  `pingEnabled=false`/`pingFrequency=0`; a `BDeviceNetwork` missing a `tuningPolicies` map.
- **WARN**: a tuning policy with `writeOnUp=false` (lower priority — it DEFAULTS true, so this only fires on an explicit override).
- **REVIEW (not statically decidable)**: a driver `write()` doing synchronous device I/O on the engine thread; a
  `readSubscribed()` with no fallback poll if COV fails; a proxy/device mounted under the wrong parent (runtime
  `checkFatalFault` only).

## 810.8 — Applied to the live PANCCADIA bog `[CERT-live]` (checklist validation)
The §810.7 checklist run against the real PANCCADIA station (bog read by the lead this campaign):
- **22 of our outputs** (`valveOut`/`evapOut`/`resistanceOut`/`condenserN`) BLink into `c:BooleanWritable` relay points
  `ro1..ro10` on THREE `nrio:NrioNetwork` modules on COM1 (`pingFrequency` 30 s; `lastFailTime` 2026-09-03 15:56).
  Bog handles: NrioNetwork `h=444f2`, `tuningPolicies h=444f5`, `defaultPolicy h=444f6`.
- **writeOnUp = OK by default**: the network's `defaultPolicy` has no explicit children, so all 22 inherit the class
  default `writeOnUp=true` (§810.4 / `BTuningPolicy.java:206`) — the DOWN→UP re-send IS in place; the device-DOWN case is
  covered without config.
- **THE REAL GAP [CERT-live]**: NONE of the 22 targets declares a `fallback` (left at null). So on a component STOP or a
  module RELOAD — NOT a device outage — our BLink source goes null, all `inN` go null, and with `fallback` also null the
  NRIO relay **HOLDS its last state** (resistance or compressor left ON). This is the checklist's "explicit SAFE fallback"
  line proven live: `writeOnUp` protects the comm-loss path, but only a non-null safe `fallback` protects the
  stop/reload/null-command path. Recorded as a PANCCADIA station-config change on issue #49.
- **Sharpened kit rule**: the two paths are DISTINCT — comm-DOWN (writeOnUp, default-safe) vs command-NULL on stop/reload
  (fallback, default-UNSAFE). The lint that bites is **"a writable proxy target driven by our output with a null
  `fallback`"** (WARN→HARD for a relay driving heat/compression), not the writeOnUp one (already default-true).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Hierarchy: BDeviceNetwork(getDeviceType) → BDevice(getNetworkType/doPing) → BPointDeviceExt(getProxyExtType) → BProxyExt(getMode/readSubscribed/write); a BProxyExt is a point ext | [CERT] | BDeviceNetwork.java:129,354; BDevice.java:295,552; BPointDeviceExt.java:33,65; BProxyExt.java:120,368,859,883 |
| 2 | BPingMonitor pings on schedule; pingFail sets down + updateStatus; DOWN propagates network→device→pointExt→proxy, setting BStatus.DOWN on the point | [CERT] | BPingMonitor.java:128; BPingHealth.java:293,305-311; BDevice.java:400; BPointDeviceExt.java:129; BProxyExt.java:475 |
| 3 | Our outputs land at in8 (READONLY operator level); priority array = lowest non-null inN, else fallback | [CERT] | BBooleanWritable.java:490,726; BNumericWritable.java:443,679 |
| 4 | Write to a DOWN/FAULT/DISABLED device is DROPPED not queued (Tuning.write !isOperational → return); UNOPERATIONAL=FATAL\|DOWN\|DISABLED\|STOP | [CERT] | Tuning.java:253,332-333 |
| 5 | Recovery via writeOnUp (DOWN→UP re-schedules a write); writeOnUp/writeOnStart/writeOnEnabled DEFAULT true → DOWN case self-corrects by default | [CERT] | Tuning.java:201-204; BTuningPolicy.java:177,206,235 |
| 6 | Minimal driver = BXxxNetwork/Device/PointDeviceExt/ProxyExt (+tuningPolicies) + wb managers + module.xml deps driver+control | [CERT] | BModbusTcpNetwork.java:25,43 |
| 7 | PANCCADIA: 22 of our outputs drive c:BooleanWritable relays on 3 nrio nets; writeOnUp inherits default-true (OK); NONE declares a fallback → relay HOLDS on stop/reload (real gap, issue #49) | [CERT-live] | PANCCADIA bog §810.8 (NrioNetwork h=444f2, defaultPolicy h=444f6) |

**Tally**: 6 [CERT] · 1 [CERT-live]. All decompiled file:line grep-verified this session (12-cite driver map confirmed at the enclosing method); the PANCCADIA numbers are the lead's live bog read.
§810.7 checklist + lints + the WHY reasoning are [INFER] grounded in the [CERT] mechanism. Dedupe: the driver framework/
protocol/wire/discovery are REMITTANCE ([B4]/[B15]/[B127]/[B496]-[B506]/[B761]); this block adds the AUTHOR-side hierarchy,
the health→point propagation, the write-drop finding, and the output-linking safety.

## Connections
- **[B772]** (point extensions — BProxyExt is one), **[B805]** (alarm ext / protection — the point-level sibling of the
  ping monitor; both set BStatus bits), **[B775]**/**B812** (monitor / liveness watchdog — application-level sibling),
  **[B801]** (Clock floor — pingFrequency/writeOnDelay are timed), **[B802]** (Sys.getService — driver services), **[B806]**
  (resource budget — subscribed-only polling), **[B809]** (wb DeviceManager views), **[B15]**/[B496]-[B506]/[B761]
  (framework/protocol/operational — REMITTANCE). Kit: `types/logic-authoring.md` §"authoring a driver" + the proxy-linking
  checklist + the writeOnUp/fallback lints; own residue: audit our modules' proxy links for writeOnUp + safe fallback.

## Open gaps
- **B810-G1** (requires-execution): confirm on a live station that a write to a DOWN device is dropped and re-sent on
  recovery ONLY with `writeOnUp=true` — the code path is [CERT], the live behavior + the 5 s writeOnDelay are worth a smoke
  test (pairs with our modules' proxy links).
- **B810-G2** (bounded): the `TuningPolicy` slot set (staleTime, minWriteTime, pollFrequency buckets) — named, not fully
  traced; a follow-up if the kit needs a tuning-policy recipe.
