# Block 552 — The control-point alarm + history extension chain: BAlarmSourceExt + the offnormal-algorithm family, interval-vs-COV history, and the confirmation that alarms are notification-only

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC15 — the alarm + history point-extension chain; operator-requested)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY. Delegated sonnet sweep; the load-bearing claims (alarm routing, OutOfRange limits,
history triggers, the alarm-only verdict) token-verified inline against vineflower.
**Primary sources** `[CERT]`:
- `alarm-rt/vineflower/javax/baja/alarm/ext/BAlarmSourceExt.java`, `BOffnormalAlgorithm.java`,
  `ext/offnormal/BOutOfRangeAlgorithm.java`, `BFloatingLimitAlgorithm.java`, `BBoolean{ChangeOfState,CommandFailure}Algorithm.java`, `BTwoStateAlgorithm.java`.
- `history-rt/vineflower/javax/baja/history/ext/BIntervalHistoryExt.java`, `BCovHistoryExt.java`, `BHistoryExt.java`.

**Scope**: the ALARM and HISTORY extensions a control point hosts — the two `BPointExtension` subclasses
[Block 536] §536.8 NAMED but attributed to "other modules, not decompiled." This decompiles them. Confirms
[Block 536] §536.8 (they live in `alarm-rt`/`history-rt`), extends the alarm thread of [Block 539]
(`BLoopAlarmAlgorithm`), and resolves the operator's safety question: is a control alarm a notification or an
interlock?

---

## 552.1 BAlarmSourceExt — the alarm check cycle [CERT]

A control point's alarm extension runs in the §536.8 extension chain: its `onExecute(out, cx)` `[CERT]
BAlarmSourceExt.java:565` calls `checkAlarms(out)` `[CERT] :660` (guarded by `isRunning && atSteadyState`).
`checkAlarms` dispatches by `alarmEnable`: `getFaultAlgorithm().checkFault(out)` or
`getOffnormalAlgorithm().checkAlarmState(out, timeDelay, timeDelayToNormal)`. When the algorithm returns a new
`BAlarmState`, `alarmStateTransition` builds a facet map (offnormalValue, presentValue, from/to state,
sourceName, msgText, all `metaData`) and ROUTES it to `AlarmService` via `AlarmSupport` `[CERT] :843-849`:
`support.toNormal(...)`, `support.newOffnormalAlarm(...)`, or `support.newFaultAlarm(...)` — then fires the
matching `toNormal`/`toOffnormal`/`toFault` topic.

**Config** `[CERT]`: `timeDelay`/`timeDelayToNormal` (to-offnormal / to-normal debounce), `alarmEnable`
(`BAlarmTransitionBits` — which transitions fire records), `alarmClass` (default `"defaultAlarmClass"`, :210 —
routes to a service class), `sourceName` (`BFormat`, default `%parent.displayName%`), `metaData`, `alarmInhibit`.

**Ack lifecycle** `[CERT]`: the ext owns `ackedTransitions` (a LOCAL mirror); firing an alarm clears the bit,
`doAckAlarm` delegates to `support.ackAlarm(...)` (AlarmService is authoritative) then re-sets the bit. Each
cycle `checkAlarms` recomputes the `unackedAlarm` STATUS BIT on the point from whether all enabled transitions
are acked. So `BAlarmService` stores the `BAlarmRecord`s + ack history; the ext keeps the point's status bit in
sync.

## 552.2 The offnormal-algorithm family [CERT]

`BOffnormalAlgorithm` is the pluggable base; every subclass implements `checkAlarms(out, toAlarmDelay,
toNormalDelay)` returning `null` (no change) or a `BAlarmState` `[CERT] BOffnormalAlgorithm.java:37-51`. The
base owns the delay timer: `startTimer` schedules `BControlPoint.execute` at expiry so the point re-evaluates
at the end of the debounce window.

- **`BOutOfRangeAlgorithm`** (numeric hi/lo — the common one) `[CERT]`: `highLimit`, `lowLimit`, `deadband`
  (min 0) `[CERT] :60-62`. A 7-state machine (Normal / High / Low / Validate* / ValidateReturn*): to-alarm when
  `value > highLimit` (→ `BAlarmState.highLimit`, :340) or `value < lowLimit` (→ `lowLimit`, :331), immediately
  if delay=0 else via a Validate state that starts the timer. **Return requires crossing back past the
  deadband** (`value <= highLimit − deadband`) — chatter suppression, same pattern [Block 539] noted for the
  loop alarm.
- **`BFloatingLimitAlgorithm`** (deviation from a live setpoint) `[CERT]`: limits are DYNAMIC —
  `highLimit = lastValidSetpoint + highDiffLimit`, `lowLimit = lastValidSetpoint − lowDiffLimit`; the setpoint
  is cached into `lastValidSetpoint` only when `status.isValid()`, so a stale setpoint does NOT move the band.
- **`BTwoStateAlgorithm`** subclasses (boolean) `[CERT]`: abstract `isNormal(out)`.
  `BBooleanChangeOfStateAlgorithm` — normal when `value != alarmValue` (null=normal). `BBooleanCommandFailureAlgorithm`
  — normal when `value == feedbackValue` (feedback is a LINKED property to a physical point; alarm when command
  ≠ feedback). `BLoopAlarmAlgorithm` ([Block 539]) is a sibling in this family.

## 552.3 History extensions — interval (timer) vs COV (execution) [CERT]

Two recording paradigms, both `BHistoryExt` subclasses writing the point's `out` to a `BHistory`:

- **`BIntervalHistoryExt`** — TIMER-driven `[CERT]`: `pointChanged` is a NO-OP `[CERT] :112`; recording is a
  `Clock.schedulePeriodically(interval, …)` `[CERT] :169`, and `doIntervalElapsed` reads the parent's CURRENT
  `getStatusValue()` and `writeRecord`s it. `interval` default = **15 minutes** `[CERT] :37` (min 1 ms). Always
  records on the tick, regardless of value change.
- **`BCovHistoryExt`** — EXECUTION-driven `[CERT]`: `BHistoryExt.onExecute` calls `pointChanged(now, out)` every
  cycle; `BCovHistoryExt.pointChanged` `[CERT] :43-49` records the FIRST value, then writes only when
  `isChange(lastValue, out)`. Base `isChange` = `!equivalent` (any value/status change). `BNumericCovHistoryExt`
  adds `changeTolerance` (default 0): record only if status changes OR `|Δvalue| > changeTolerance` — the COV
  deadband. So COV writes on change (bounded by tolerance); interval writes on a schedule.

Wiring `[CERT]`: both receive the same `out` `BStatusValue` from the §536.8 extension chain;
`requiresPointSubscription()` returns `enabled`, so enabling the ext keeps the point subscribed. The interval
ext additionally reads `getParentPoint().getStatusValue()` directly at timer-fire.

## 552.4 The verdict — alarms are NOTIFICATION-ONLY (ties B543/B551) [CERT]

`BAlarmSourceExt` OR-merges `alarm`/`fault`/`unackedAlarm` STATUS BITS into the point's `out` status and routes
a `BAlarmRecord` to `AlarmService` — but **it NEVER touches the point's output VALUE, writes no priority-array
slot, and invokes no action** `[CERT]` (`onExecute` sets status bits :600-604/:676-685; no value write, no
override). It is an independent observer.

This CONFIRMS [Block 543] §543.6's "alarm-only, no interlock" for the generic control-point alarm, and
COMPLEMENTS [Block 551]: a hard control interlock (e.g. `CmFrost_Protection`) is SEPARATE embedded logic, not
the alarm extension. To make an alarm ALSO interlock control, an engineer must drive a high-priority
writable-point slot ([Block 536] level 1/8) FROM the alarm state — the alarm extension itself provides zero
hard-interlock capability. So the safety picture across the three blocks is consistent: **alarms notify;
interlocks are separate high-priority writes or embedded frost/limit sub-functions.**

## 552.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BAlarmSourceExt.onExecute→checkAlarms→algorithm; routes toNormal/newOffnormalAlarm/newFaultAlarm to AlarmService | [CERT] | BAlarmSourceExt.java:565,660,843-849 | token-checked ✓ |
| 2 | Config: timeDelay/ToNormal, alarmEnable, alarmClass(default defaultAlarmClass), sourceName | [CERT] | :210 + property decls | token-checked (alarmClass) ✓ |
| 3 | Ack: ext mirrors ackedTransitions + unackedAlarm bit; AlarmService authoritative | [CERT] | BAlarmSourceExt ack path | sweep-cited |
| 4 | Algorithm base contract checkAlarms→BAlarmState; timer re-executes point at delay expiry | [CERT] | BOffnormalAlgorithm.java:37-51 | token-checked ✓ |
| 5 | BOutOfRangeAlgorithm highLimit/lowLimit/deadband; return past deadband (chatter suppression) | [CERT] | BOutOfRangeAlgorithm.java:60-62,331,340 | token-checked ✓ |
| 6 | BFloatingLimitAlgorithm limits = lastValidSetpoint ± diffLimit (stale SP held) | [CERT] | BFloatingLimitAlgorithm limits | sweep-cited |
| 7 | BooleanCommandFailure = alarm when command≠feedback (linked property) | [CERT] | BBooleanCommandFailureAlgorithm | sweep-cited |
| 8 | Interval: pointChanged no-op, timer schedulePeriodically, default 15min | [CERT] | BIntervalHistoryExt.java:37,112,169 | token-checked ✓ |
| 9 | COV: pointChanged writes on isChange; BNumericCovHistoryExt changeTolerance deadband | [CERT] | BCovHistoryExt.java:43-55 | token-checked ✓ |
| 10 | Alarm ext = notification-only: sets status bits + record, never writes value/priority-array | [CERT] | BAlarmSourceExt.java:600-604,676-685 | token-checked ✓ |

**Marker tally**: [CERT] ×7 · [INFER] ×0 substantive (the safety-thread synthesis is [INFER]-labeled). Block
TYPE = EVIDENCE (decompilation). 7 of 10 rows token-verified inline.

## Connections

- **[Block 536]** §536.8 — the extension chain + the alarm/history exts it NAMED (now decompiled; CONFIRMS they
  live in alarm-rt/history-rt, not [INFER]).
- **[Block 539]** — `BLoopAlarmAlgorithm` is a `BTwoStateAlgorithm` sibling of the boolean alarm algorithms here.
- **[Block 543]** §543.6 / **[Block 551]** §551.4 — the safety thread: this CONFIRMS alarms are notification-only;
  interlocks are separate (high-priority writes or embedded frost sub-functions).
- **alarm / history focuses** — `AlarmService`/`BAlarmRecord` internals and `BHistory` storage are those
  subsystems, not re-derived here.

## Open gaps (this block)

- `AlarmService` routing/`BAlarmRecord` lifecycle and `BHistory` storage/rollover are the alarm/history
  subsystems (out of this control focus). Focus re-STOPs at investigable=0.
