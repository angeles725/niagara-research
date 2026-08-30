# Cold-Room Control Module — Design (N4 custom module, JACE-9000)

> **Deliverable of focus `cold-room-module` (design/application, not new discovery).**
> This is Option 2: a **compiled, signed `-rt` custom module** defining a reusable
> `BColdRoom` component TYPE, instantiated once per physical room. It reuses the
> already-closed corpus rather than re-investigating Niagara internals.
>
> **Grounding focuses:** `station-organization` (`docs/station-organization.md`),
> `module-dev-workflow` (`docs/module-dev-workflow.md`), `module-best-practices`
> (`docs/module-best-practices.md`), plus the kitControl / history / audit mappings
> summarized below with block citations.
>
> Marker key: **[CERT]** = verbatim in decompiled source (file:line); **[CERT-doc]**
> = stated in a closed-focus doc citing blocks; **[CERT-live]** = live JACE-9000
> probe; **[INFER]** = design synthesis.

---

## 1. Locked requirements (source of truth for this design)

Four independent cold rooms. Common trigger everywhere: **zone temperature with a
differential (hysteresis)** — start cooling when `zone ≥ setpoint + differential`,
stop when `zone ≤ setpoint`. Common actuation order everywhere: **open valve first,
then start the evaporator after a per-evaporator configurable delay** (each
evaporator has its own delay).

| Room | Equipment | Zone sensors | Rule |
|---|---|---|---|
| **1** | 3 evaporators + 3 valves | 2 | Stage 1 (zone-1 call): units 1 & 2. Stage 2 (zone-2 call): unit 3. **Unit 2 runs on an OR of both zone sensors.** Single shared room setpoint. |
| **2** | 1 evaporator + 1 valve | 1 | Zone call → unit 1. |
| **3** | 2 evaporators + 2 valves + defrost resistances | (zone) | Zone call → both units. **Defrost** (heaters via relay): configurable, two modes — *by interval* or *by schedule*. Terminate by time **or** by resistance temp sensor. **Interlock: never two units in defrost at once**; the waiting unit starts its defrost a configurable delay (default 4 min) after the other resumes normal operation. Defrost sequence per unit: close solenoid valve → stop evaporator → energize resistance. |
| **4** | 1 evaporator + 1 valve | 1 | Zone call → open valve, start evaporator after configurable delay. |

**Sensors:** one temperature sensor per evaporator (same sensor serves the fan-running
indication and the high/low temp alarm).

**Alarms — visual only, they do NOT interact with control logic:** evaporator temp too
low, evaporator temp too high, room temp too high, fan-not-spinning.

**Everything configurable:** setpoints, differentials, alarm thresholds, per-evaporator
actuation delays, defrost parameters (interval/schedule, duration, resistance threshold,
inter-unit stagger delay). No time or threshold is hard-coded.

**Additional data requirements (JACE-9000):**
- Temperature history (trend log) for every zone/evaporator sensor.
- ON/OFF event history for every defrost resistance.
- Audit trail of *who* changed any setpoint / time / parameter.

---

## 2. Where it lives in the station [CERT-doc]

Two-layer model from `docs/station-organization.md` §1–§3:

- **Field points** live under their device in the driver tree, points-only:
  `/Drivers/<Network>/<Device>/points/…`. No logic here. §1.5 [B650, B501]
- **Control logic** lives in the station component space (`/Config` or `/Services`),
  **NOT** under `/Drivers`. §2

For this module: one `BColdRoom` instance per room, each tagged `equip` (semantic-query
boundary), placed under `/Config` (e.g. `/Config/ColdRooms/Room1…Room4`), co-located near
the points it serves (Tridium Philosophy B). Logic reads input points and writes output
points **through `BLink`s** to the driver's writable points. §3 [B6]

---

## 3. Component model (the reusable TYPE)

Composition over a monolith. Three compiled types in the `-rt` jar:

### 3.1 `BColdRoom` (equip container) — one per room

| Slot | Type | Default / facets | Purpose |
|---|---|---|---|
| `setpoint` | `BStatusNumeric` | facet `units=°C` | Room target temperature. |
| `differential` | `double` (BStatusNumeric) | e.g. 1.0, `min=0` | Hysteresis band (see §4.1). |
| `roomHighAlarmLimit` | `double` | configurable | Feeds the room high-temp visual alarm. |
| `zoneSensors[]` | link inputs (`BStatusNumeric`) | 1–2 per room | Zone temperature inputs (linked from proxy points). |
| `units[]` | child `BEvaporatorUnit` | 1..3 | The evaporator units (below). |
| `stagingMode` | `BEnum` | `single` / `staged` | Room 1 = staged (2 sensors); others = single. |
| `defrost` | child `BDefrostController` | optional | Present only for Room 3. |
| `cooling` | `BStatusBoolean` (readonly out) | — | Computed room cooling demand. |

`BColdRoom.execute()` [INFER]:
1. Compute the cooling call per zone sensor using the hysteresis rule (§4.1).
2. Apply the staging map (§4.2) to decide which units should run.
3. Drive each `BEvaporatorUnit.runCmd`.

### 3.2 `BEvaporatorUnit` — one per evaporator

| Slot | Type | Default | Purpose |
|---|---|---|---|
| `runCmd` | `BStatusBoolean` (in) | — | Cooling demand for this unit, set by the parent. |
| `startDelay` | `BRelTime` | e.g. 2s (Room 1) / 5s (Room 4), **per unit** | Valve→evaporator delay. |
| `valveOut` | `BStatusBoolean` (out → link) | — | Command to the solenoid valve writable. |
| `evapOut` | `BStatusBoolean` (out → link) | — | Command to the evaporator (fan/compressor) writable. |
| `coilTemp` | `BStatusNumeric` (in) | — | This evaporator's temp sensor (alarms only). |
| `evapHighAlarmLimit` / `evapLowAlarmLimit` | `double` | configurable | Feed the visual temp alarms. |
| `hasDefrost` | `boolean` | false | Room 3 units = true. |

`BEvaporatorUnit` actuation sequence (mirrors kitControl `BBooleanDelay`) [CERT]:
on `runCmd` rising → set `valveOut=true` immediately, start `startDelay`, on expiry set
`evapOut=true`. On `runCmd` falling → `evapOut=false` then `valveOut=false`. The Java
implementation reproduces `BBooleanDelay`'s independent on/off delay
(`com.tridium.kitControl.timer.BBooleanDelay`, props `onDelay`/`offDelay`,
`timer/BBooleanDelay.java:59`).

### 3.3 `BDefrostController` — Room 3 only (§5)

Holds the defrost schedule/interval config, the resistance outputs, the termination
config, and the inter-unit interlock. Detailed in §5.

> **Why typed slots matter for your audit requirement:** because every configurable value
> is a component **slot**, any operator edit to `setpoint`, `differential`, `startDelay`,
> defrost times, etc. is captured automatically by `BAuditHistoryService` at the slot-map
> layer (§7) — old value → new value → user → timestamp, with zero extra wiring. [INFER from §7]

---

## 4. Cooling control logic

### 4.1 Hysteresis (setpoint + differential) [CERT algorithm]

The native primitive is `BTstat` (`com.tridium.kitControl.hvac.BTstat`). Verified deadband
at `hvac/BTstat.java:115-136`:

```
highValue = sp + diff/2
lowValue  = sp - diff/2
if (cv >= highValue) out = true
else if (cv <= lowValue) out = false     // else HOLD previous → hysteresis
```

The module reproduces this in Java per zone sensor (`cv`=zone temp, `sp`=`setpoint`,
`diff`=`differential`), `direct` action = cooling call on rising above the high threshold.
This eliminates compressor short-cycling. (Manual equivalent, if asymmetric thresholds are
ever wanted: `BGreaterThan` + `BLessThan` + `BBooleanLatch` — both paths exist.)

### 4.2 Per-room staging map [INFER application]

- **Room 1 (staged, 2 sensors, shared setpoint):**
  - `call1 = hysteresis(zone1, setpoint, diff)`
  - `call2 = hysteresis(zone2, setpoint, diff)`
  - Unit 1 runs on `call1`. Unit 3 runs on `call2`.
  - **Unit 2 runs on `call1 OR call2`** (native `BOr`, `com.tridium.kitControl.logic.BOr`).
- **Room 2 (single):** unit 1 runs on `call`.
- **Room 3 (single, both together):** both units run on `call` (defrost overrides per §5).
- **Room 4 (single):** unit 1 runs on `call`.

---

## 5. Defrost subsystem (Room 3) — `BDefrostController`

### 5.1 Configurable slots

| Slot | Type | Purpose |
|---|---|---|
| `mode` | `BEnum {interval, schedule}` | Selects trigger source. |
| `interval` | `BRelTime` | "Defrost every N hours" (interval mode). |
| `schedule` | `BBooleanSchedule` ref | Wall-clock defrost times (schedule mode). |
| `duration` | `BRelTime` | Max defrost length. |
| `terminateOnResistanceTemp` | `boolean` | If true, also end on `resistanceTemp ≥ threshold`. |
| `resistanceTempThreshold` | `double` | End-of-defrost temperature. |
| `staggerDelay` | `BRelTime` | **Default 4 min, configurable.** Inter-unit stagger. |

### 5.2 Trigger [CERT for the primitives]

- **`mode = schedule`** → drive defrost initiation from a `BBooleanSchedule`
  (`javax.baja.schedule`, weekly boolean, `niagara-mental-model-bloque24.md:42`). Operators
  edit it with the native WebScheduler view.
- **`mode = interval`** → free-running timer. Two verified options:
  - kitControl `BMultiVibrator` (`util/BMultiVibrator.java:55`, props `period` + `dutyCycle`), or
  - native `BTimeTrigger` + `BIntervalTriggerMode` (`niagara-mental-model-bloque123.md:117`).
  Recommend `BMultiVibrator` for a simple "every N hours" wired in the module.

### 5.3 Per-unit defrost sequence [INFER, per spec]

When a unit enters defrost: `valveOut=false` (close solenoid) → `evapOut=false` (stop
evaporator) → `resistanceOut=true` (energize heater). Exit when `duration` elapses **OR**
(`terminateOnResistanceTemp` AND `resistanceTemp ≥ resistanceTempThreshold`), whichever
first → `resistanceOut=false`, then the unit returns to normal cooling control.

### 5.4 Interlock — never two units in defrost at once [INFER, per spec]

State machine in `BDefrostController.execute()`:
- At most one unit may hold the defrost token.
- When unit A is due and unit B is currently in defrost, A **waits**.
- When B exits defrost and resumes normal operation, A starts a `staggerDelay` timer
  (default 4 min). On expiry, A takes the token and begins its own defrost.
- The unit NOT in defrost keeps cooling normally throughout.

The stagger timer is a `BOneShot`/`BBooleanDelay`-style timed transition (kitControl
`timer/BOneShot.java:59`); the mutual exclusion is a latch/token guarded by `BAnd`/`BNot`.
`BInterstartDelayControl` (kitControl compressor anti-short-cycle) is an available native
analog if you prefer to compose rather than hand-code the stagger. [INFER relevance]

---

## 6. Points & linking (I/O) [CERT-doc]

Physical I/O = proxy points under the device in the driver tree; the module links to them.
From `docs/station-organization.md` §1.2 / §3:

- **Inputs (readonly):** `BNumericPoint` for each temperature sensor (zone + coil).
- **Outputs (writable, 16-level priority array):** `BBooleanWritable` for each solenoid
  valve, evaporator, and defrost resistance.
- **Linking:** the module's `valveOut`/`evapOut`/`resistanceOut` slots link via `BLink` to
  the corresponding writable's priority level — recommended **`in8`** (program default),
  leaving higher levels for manual/emergency overrides. `in1`/`in8`/`fallback` persist;
  others are transient. §3.1 [B6, B544, B716; `RESEARCH-STATE-kitControl.md:50`]
- Bulk-wire at commissioning with `BBatchLinkEditor` (dry-run `checkLink`); keep links
  handle/tag-stable so re-addressing a device touches only the proxyExt, not the logic.

**I/O count (for point provisioning):**

| Room | Numeric IN (sensors) | Boolean OUT |
|---|---|---|
| 1 | 2 zone + 3 coil = 5 | 3 valve + 3 evap = 6 |
| 2 | 1 zone + 1 coil = 2 | 1 valve + 1 evap = 2 |
| 3 | zone + 2 coil + 2 resistance-temp = 5* | 2 valve + 2 evap + 2 resistance = 6 |
| 4 | 1 zone + 1 coil = 2 | 1 valve + 1 evap = 2 |

\* Room 3 resistance-temp inputs only if `terminateOnResistanceTemp` is used.

---

## 7. Histories (data logging) [CERT]

Add point history extensions (`javax.baja.history.ext`, module `history-rt`) — these attach
to points like any point extension:

- **Temperature trend** (zone + coil) → `BNumericIntervalHistoryExt` (fixed-interval numeric
  logging). Interval configurable via `BHistoryConfig.interval`.
- **Resistance ON/OFF events** → `BBooleanCovHistoryExt` (records on each state change, with
  timestamp). *(There is no "ChangeOfState" class; boolean state changes are logged by the
  COV extension.)*

**Capacity / retention** (`BHistoryConfig`, `.../history/BHistoryConfig.java:114-129,386`):
- `capacity` default **500 records** (`BCapacity.makeByRecordCount(500)`), configurable by
  record count or storage size.
- `fullPolicy` default **`roll`** (in-place circular eviction — oldest record trimmed) vs
  `stop` (stop recording when full).

**JACE-9000 storage envelope** [CERT-live]: 8 GB eMMC + 2 GB RAM (`bloque665.md:66,81,110`).
No documented dedicated history partition or record ceiling exists in the corpus; sizing is
per-history (`capacity`) bounded by total media shared with OS/station/config. **Design note:**
size each history's `capacity` to the retention you actually need (e.g. temp at 1-min interval
for 30 days ≈ 43,200 records/point) rather than leaving the 500 default, and use `roll` so old
data evicts cleanly. [INFER]

---

## 8. Audit trail (who changed what) [CERT]

Service: **`BAuditHistoryService`** (`com.tridium.history.audit`, module `history-rt`), a
station Service singleton (typically `/Services/AuditHistoryService`).

Per config change it records an `AuditEvent`
(`operation, target, slotName, oldValue, value, userName, timestamp`) —
`bloque564.md:30-101` (`AuditEvent.java:10-39`). It audits at the **slot-map layer**
(`ComponentSlotMap`/`ComplexSlotMap`), so it is comprehensive by construction: **every edit
to a module slot** (a setpoint, a differential, a defrost time) fires an audited event with
the operator's username — no per-feature opt-in. Login/security events are the companion
`BSecurityAuditHistorySource` stream.

Audit records become a queryable `BHistory` (BQL, AuditHistory view). **Action item:** confirm
the AuditHistoryService is present in the JACE-9000 station — the auditor is a null-guarded
sink (`Sys.setAuditor()`); if absent, events are silently dropped (`bloque564.md`, `Sys.java:174-183`).
For off-box retention, enable syslog offload (`BSyslogSettings.enabled=true` + `serverHost`);
otherwise audit is local-only. [CERT]

---

## 9. Alarms (visual only) [CERT-doc]

Mechanism = one `BAlarmSourceExt` (`javax.baja.alarm.ext`, module `alarm-rt`) per monitored
point, holding a pluggable off-normal algorithm (`bloque552.md`):

- **Evaporator temp too high / too low** → `BAlarmSourceExt` + `BOutOfRangeAlgorithm`
  (`highLimit`/`lowLimit`/`deadband`) on each coil temp point.
- **Room temp too high** → `BOutOfRangeAlgorithm` (highLimit only) on the zone point.
- **Fan not spinning** → the coil temp point deviating abnormally, or a boolean fault point
  with `BBooleanChangeOfStateAlgorithm`.

Confirmed **notification-only**: the alarm extension sets status bits + creates an alarm
record routed to `AlarmService`; it **never writes a value or the priority array**
(`bloque552.md:108`, `BAlarmSourceExt.java:600-604,676-685`). This matches the "visual only,
does not affect control" requirement exactly. Add in bulk via Point Manager → "Add Extensions".

---

## 10. Build & deploy to the JACE-9000

Follow the runbook `docs/module-dev-workflow.md` (edit → build → sign → deploy → test → debug):

1. Author the three `BComponent` types with `@NiagaraType`/`@NiagaraProperty` slots; run
   Slotomatic codegen.
2. `gradle-niagara` build → produce the `coldRoom-rt.jar`.
3. **Sign** the module (required; JACE-9000 runs signed modules — see
   `docs/niagara-signing-hardening-guide.md`).
4. Deploy the signed jar to the JACE-9000, install it in the station's `!modules`.
5. Instantiate `BColdRoom` ×4 under `/Config/ColdRooms`, configure slots per room, link to
   proxy points, attach history extensions, confirm AuditHistoryService.
6. Test on target; debug via station console / logs.

---

## 11. Open design decisions (surface before implementation)

1. **Java-native vs embedded-kitControl-composite** for the internal logic. This design
   assumes Java-native `execute()` (typed, testable, one clean TYPE). Alternative: ship a
   preconfigured composite of kitControl blocks wired by `BLink`. Recommend Java-native for a
   reusable OEM type. [decision]
2. **One generic `BColdRoom` vs per-room subtypes.** This design uses one generic type
   parameterized by `stagingMode` + child unit count + optional `defrost`. Confirm that fits
   all four rooms, or split Room 3 into a `BDefrostColdRoom` subtype. [decision]
3. **Resistance-temp sensors for Room 3** — confirm they are physically installed if
   `terminateOnResistanceTemp` is to be used; otherwise defrost terminates by `duration` only.
4. **History retention targets** per point (interval + days) to size `capacity` (§7).

---

*Citations consolidated from the kitControl mapping (decompiled `organized/kitControl/…`) and
the history/audit mapping (`organized/history/…`, blocks 552/564/665/24/123, `docs/station-organization.md`).
All [CERT] claims trace to file:line or §section as noted inline.*
