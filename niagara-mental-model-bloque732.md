# B732 · Authoring real Niagara alarms from a control module — `BAlarmSourceExt` is a point extension, the offnormal/fault algorithm family, and where our temp limits actually belong

> **Scope**: the AUTHORING pattern for making a value raise a real Niagara alarm (into the Alarm Console —
> ack/log/escalate), and the concrete conclusion for the PANCCADIA León modules whose alarm-limit slots
> today produce nothing (B731 §731.3). Closes B731-G2. Distinct from B44 (Alarm Console SPA) and B244 (OEM
> honAlarmExt) — those study consumers/OEM layers; this is "how do I SOURCE an alarm."
>
> **Sources**: FUENTE 3 docSource `organized/docSource/docSource-doc/extracted/alarm-rt/javax/baja/alarm/ext/`
> (`BAlarmSourceExt.java`, `offnormal/BOutOfRangeAlgorithm.java`, and the algorithm family), read this session.
> FUENTE 1 corpus: B731 (our-module audit), B44/B244 (alarm consumers/OEM), B4 (slots), B5 (ORD — alarm
> source ORD). Client code: our BColdRoom/BEvaporatorUnit/BCompressorControl (BStatusNumeric temps, no ext).

---

## 732.1 — The standard alarm-source mechanism `[CERT]`

**`BAlarmSourceExt extends BPointExtension`** (`BAlarmSourceExt.java:295`, imports
`javax.baja.control.BControlPoint`, `BPointExtension`). So a real Niagara alarm source is a **POINT
EXTENSION**: you attach it to a `BControlPoint`; it is NOT a free-standing thing you drop on an arbitrary
`BComponent`. Its config slots (all framework-driven, no custom code):
- **`offnormalAlgorithm`** (`BOffnormalAlgorithm`, :238/827) — pluggable "is it offnormal?" strategy.
- **`faultAlgorithm`** (`BFaultAlgorithm`, :230/801) — pluggable "is it faulted?" strategy.
- **`alarmClass`** (:246) — routes the record to a `BAlarmClass` (priority, notification, escalation).
- **`timeDelay` / `timeDelayToNormal`** (:96/105) — debounce before asserting/clearing.
- **`toOffnormalText` / `toFaultText` / `toNormalText`** (:168/177/186), **`sourceName`** (:160) — the
  message + source label carried on the `BAlarmRecord`.

When the algorithm trips, the framework fires `toOffnormal`/`toFault`/`toNormal` `BAlarmRecord`s into the
alarm database automatically (see the `fire(toOffnormal/toFault/toNormal, event, …)` calls, B730 §730-G1).
The record's SOURCE is the point's ORD — which is exactly the ORD the oBIX encoder later resolves (the
TC500 flood, B-oBIX finding: a stale source ORD → UnresolvedException).

## 732.2 — The algorithm family (pick per value type) `[CERT]`

`alarm/ext/offnormal/` + `alarm/ext/fault/`:
- **`BOutOfRangeAlgorithm`** — numeric HIGH/LOW limits. Slots: `highLimit`, `lowLimit`, **`deadband`**
  ("subtracted from highLimit and added to lowLimit" = hysteresis, :67-70), `highLimitText`, `lowLimitText`,
  `limitEnable` (`BLimitEnable` — enable high/low independently). **This is exactly our room/coil/discharge
  temperature case.**
- `BFloatingLimitAlgorithm` — limits that track a setpoint (band around a moving target).
- `BBooleanChangeOfStateAlgorithm` / `BNumericChangeOfStateAlgorithm` / `BEnumChangeOfStateAlgorithm` /
  `BStringChangeOfStateAlgorithm` — alarm on a state change (e.g., a fault flag going true).
- `BBooleanCommandFailureAlgorithm` / `BEnumCommandFailureAlgorithm` — commanded vs actual mismatch
  (start-prove! a compressor commanded ON but no amperage → command-failure alarm).
- `BStatusAlgorithm` — alarm on BStatus bits (fault/down/stale).
- Fault side: `BTwoStateFaultAlgorithm`, `BEnumFaultAlgorithm`, `BStatusFaultAlgorithm`,
  `BOutOfRangeFaultAlgorithm`.

## 732.3 — Where our alarms actually belong `[CERT/INFER]`

**Constraint**: `BAlarmSourceExt` needs a `BControlPoint` parent. Our temperatures (`coilTemp`,
`resistanceTemp`, room zone temps) are **`BStatusNumeric` PROPERTIES on our custom BComponents — not
control points** — so **you cannot attach a `BAlarmSourceExt` to them directly** `[CERT]`.

The idiomatic answer (zero module code — station engineering) `[INFER, grounded in §732.1]`:
- The field temperatures arrive from **proxy points in the driver tree** (the TC500-style sensor points).
  Those **ARE `BControlPoint`s** → attach the `BAlarmSourceExt` + `BOutOfRangeAlgorithm` **on the proxy
  point**, set `highLimit`/`lowLimit`/`deadband` + `alarmClass`. This is where the TC500 alarm already lived
  (which is why its stale source ORD flooded oBIX). Alarms then enter the console with the point as source —
  ack/log/escalate for free.
- Our custom **alarm-limit slots** (`roomHighAlarmLimit`, `evapHighAlarmLimit`, `evapLowAlarmLimit`) are the
  **wrong locus**: they are plain config doubles the control never reads and no source ext consumes → they
  produce no alarm. Two clean options: (a) DROP them and put the limits on the proxy-point alarm ext
  (recommended — one home for the limit); or (b) if operators must tune limits from OUR component/dashboard,
  LINK our limit slot → the proxy point's `BOutOfRangeAlgorithm.highLimit`/`lowLimit` (the slot becomes a
  remote setter, the ext still does the alarming).
- `dischargeHighAlarm` / `stuckAlarm` are computed **status booleans**; to make them console alarms, wire a
  `BBooleanChangeOfStateAlgorithm` (or a command-failure algorithm for start-prove) on a boolean point fed
  by that flag. Better: model discharge-high and start-prove as command-failure / out-of-range alarms on the
  proper points rather than as ad-hoc flags.

**Do NOT hand-roll `fire(BAlarmRecord)` in our components** — it reinvents timeDelay/algorithm/class routing
that `BAlarmSourceExt` gives for free, and it re-creates the stale-source-ORD hazard.

## 732.4 — If component-level alarms without points are ever required
Options, worst to best: (c) custom `fire()` code — discouraged (reinvents the framework); (b) model the
value as a real `BControlPoint` inside the component and give it the ext — heavy but idiomatic; (a) — the
§732.3 proxy-point route — best. For our plant, (a) covers every temperature/pressure alarm with no module
change.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | BAlarmSourceExt is a BPointExtension → attaches to a BControlPoint, not an arbitrary BComponent | [CERT] | BAlarmSourceExt.java:295 extends BPointExtension; imports BControlPoint |
| 2 | It has pluggable offnormalAlgorithm + faultAlgorithm, alarmClass, timeDelay(s), to*Text, sourceName | [CERT] | BAlarmSourceExt.java:230,238,246,96,105,160,168,177,186 |
| 3 | BOutOfRangeAlgorithm = numeric high/low with deadband hysteresis + limitEnable | [CERT] | BOutOfRangeAlgorithm.java:52-91 |
| 4 | The framework fires to Offnormal/Fault/Normal BAlarmRecords; source = point ORD | [CERT] | fire() calls (B730 §730-G1); alarm source ORD (B5, oBIX TC500 finding) |
| 5 | Our temps are BStatusNumeric properties, not control points → can't take a BAlarmSourceExt directly | [CERT] | our rt source (coilTemp/resistanceTemp BStatusNumeric); grep BAlarmSourceExt=0 (B731) |
| 6 | Idiomatic fix = alarm ext on the driver proxy points; our limit slots are the wrong locus | [INFER] | §732.1 constraint + standard driver-point alarm modeling; no counter-evidence |

**Tally**: 5 [CERT], 1 [INFER]. No unmarked claims.

## Connections
- **B731** §731.3 (our modules produce no alarms) — this block answers "how to fix it."
- **B44/B244** — the CONSUMER side (Alarm Console SPA, OEM honAlarmExt delay/suppression).
- **B4** (slots), **B5** (ORD — the alarm source ORD), B730 §730-G1 (Topic `fire`).
- oBIX TC500 finding (engram) — the stale-source-ORD flood is the failure mode of an orphaned alarm source.

## Open gaps
- **B732-G1**: `BAlarmClass` routing/priority/notification + `BAlarmService` recipient wiring — the
  downstream of `alarmClass`; touched in B44/B16, not re-derived here.
- **B732-G2**: the command-failure algorithm as the proper model for compressor start-prove (vs the current
  `stuckAlarm` flag) — a focused design item if the operator wants start-prove in the console.
