# B805 · RT control-logic exemplars — how Tridium implements PID / deadband / latch / protection, the control-physics WHY behind each line, a one-bit fault trace from point to console, and the honest verdict that Tridium ships NO ODE/matrix math `[CERT]`

> **Scope** (Excavador Técnico depth): the AUTHOR-side control-logic patterns Tridium's own kitControl/alarm modules
> ship, each dismantled to (1) the implementation line, (2) the control-physics reason it exists + the failure mode at
> the actuator if it were absent, (3) where it runs and at what rate. Plus a single fault BIT traced end-to-end
> (sensor→BStatus→alarm record→BAlarmService→console→ack) and the honest answer to "does Tridium integrate ODEs / use
> matrices?" (NO — scalar Euler only). Evidence base for the C8-PR7 SKILL.md/METHODOLOGY control profile.
>
> **Sources**: FUENTE 3 (read-only, file:line [CERT], 4.14.0.162 `organized/…/vineflower`) — `com.tridium.kitControl`
> `BLoopPoint`, `hvac/BTstat`, `util/BLatch`/`BNumericLatch`, `math/*`; `javax.baja.alarm.ext.BAlarmSourceExt` +
> `ext/offnormal/BOutOfRangeAlgorithm`; `javax.baja.alarm.{BAlarmService,BAlarmRecord}`; `javax.baja.test.BTest`.
> FUENTE 1 (REMITTANCE): [B729]/[B730]/[B737] (execute/changed split + engine thread), [B775] (BAbstractMonitor watchdog),
> [B776] (action protection flags), [B787] (timer-ticket lint), [B801] (Clock delay floor), [B789] (poll-vs-subscribe).
> Every load-bearing cite grep-verified at the enclosing method this session (a delegated map is a hypothesis until read).

---

## 805.1 — PID loop: `BLoopPoint` `[CERT for code; INFER for the physics WHY]`
`BLoopPoint extends BNumericPoint` self-drives a PID loop on a repeating clock. **Where it runs**: `initTimer()`
`Clock.schedulePeriodically(this, executeTime, timerExpired, null)` (`:340`, cancels the old ticket first :337 — the
[B787] discipline) → `doTimerExpired` → `calculatePoint()` (:350). The math (`:410-414`):
`P = error·Kp`; `I = Kp·Ki·errorSum/60`; `D = Kp·Kd·(error−lastError)/Δs`; `pv = P+I+D`. Tuning slots
`proportionalConstant/integralConstant/derivativeConstant/bias/maximumOutput(=100)/minimumOutput(=0)` (`:119-124`).
This is the INTERACTING form (Kp multiplies all three terms); Ki is repeats/minute (the `/60`).

- **Anti-windup** (`:391-401`): after accumulating `errorSum`, clamp it so `Kp·Ki·errorSum/60` stays within
  `[minOutput,maxOutput]`. **WHY (physics)**: while the actuator is SATURATED (valve already 100%), the error persists
  but the plant cannot respond, so a naive integrator keeps accumulating. Without the clamp, on recovery that giant
  `errorSum` drives a massive overshoot — the actuator slams to the opposite rail, the process oscillates, and a
  pressure/temperature protection can trip. The clamp bounds the integral to what the output range can actually deliver,
  so recovery is immediate and bumpless. **Absent → integral windup → overshoot → actuator hammering / nuisance trip.**
- **NaN/fault input guard** (`:369-374`): if `setpoint` or `controlledVariable` is `NaN`/`Infinite`, set
  `pidOutput.setStatusFault(true)` and SKIP the math — the last clamped output HOLDS. **WHY**: a NaN in the PID sum
  propagates to the actuator command (undefined valve position); holding the last-good output + flagging fault is
  fail-safe. **Absent → NaN reaches the actuator.**
- **Direct vs reverse** (`BLoopAction`, `:414`): `direct` negates `pv`. **WHY**: cooling (raise output as PV rises) vs
  heating (lower output as PV rises) — same loop, opposite sense; wrong sense = positive feedback = runaway.
- **Bumpless transfer** (`:545-566`, ramp `:444-459`; `changed()` rescales `errorSum` on Kp/Ki change `:502-515`):
  disabling seeds `errorSum` to the anti-windup value so re-enable does not STEP the actuator; a tuning change rescales
  the accumulated integral so the output does not jump. **WHY**: an actuator step is a mechanical/process shock; bumpless
  keeps transitions smooth. (No formal "manual mode" slot — `loopEnable` + `disableAction` is the enable.)

## 805.2 — Deadband / hysteresis: `hvac/BTstat` `[CERT; INFER for WHY]`
`calculate()` (`:118-139`): `high = sp + diff/2`, `low = sp − diff/2`; output latches TRUE at `cv ≥ high`, FALSE at
`cv ≤ low`, HOLDS between (`inControl` when `low < cv < high`); direct/reverse by double-negate. **WHY**: a two-position
output with no band CHATTERS — sensor noise crossing a single setpoint toggles the relay/compressor many times a second,
destroying contactors and short-cycling a compressor (which needs minutes between starts). The differential `diff` is
the anti-chatter / anti-short-cycle band. **Absent → relay chatter, compressor short-cycle, equipment failure.**

## 805.3 — Latch: `util/BLatch` (a D-latch, NOT SR) `[CERT — incl. a negative]`
`changed()` on a RISING clock edge (`currentClock && !lastClock`, `:74`) captures `in→out`
(`setOutStatusValue(getInStatusValue())`); `doLatch()` is the explicit sample. Typed subclasses `BNumericLatch`/
`BBooleanLatch`/`BEnumLatch`/`BStringLatch`. **HONEST NEGATIVE**: there is NO `BSRLatch`/`BStatusLatch`/set-reset-priority
latch anywhere in kitControl (searched). Tridium ships only the edge-triggered sample-and-hold. An SR interlock latch
(set-dominant / reset-dominant, for a safety trip that must stay tripped until reset) is AUTHOR-BUILT, not a stock block.

## 805.4 — Protections: the alarm EXTENSION on a point `[CERT; INFER for WHY]`
**WHO throws**: `BAlarmSourceExt extends BPointExtension implements BIAlarmSource` (`:184`) — a protection is a point
EXTENSION you add under a `BControlPoint` (same placement model as [B772]/[B804]). Its `offnormalAlgorithm` slot (`:209`)
holds the limit logic; `BOutOfRangeAlgorithm extends BOffnormalAlgorithm` (`:59`) carries `highLimit`/`lowLimit`/
`deadband`(min 0)/`limitEnable` (`:60-65`). `checkAlarms(out)` (`:660`) → `evaluateNewAlarmState` (`:667`).
**HOW raised**: it SETS BITS on the point's `BStatus` — `BStatus.makeAlarm(status,true)` (`:676`),
`makeFault(status,true)` (`:682`), `makeUnackedAlarm(status,true)` (`:718`) — and creates a `BAlarmRecord`.
**WHO watches**: `BAlarmService.routeAlarm(record)` (`:244` → `doRouteAlarm :577` → `doRouteToRecipient :573`) fans out
to the record's `BAlarmClass` → recipients (console/email/SMS). Service found by `Sys.getService(BAlarmService.TYPE)`
([B802]). **Operator feedback**: `BAlarmRecord` fields (timestamp/uuid/sourceState/alarmClass/priority/normalTime/
ackTime/alarmData); ack via `doAckAlarm(record)` (`:503`) → `makeUnackedAlarm(status,false)` (`:521`) clears the bit.
**WHY the separation**: the ext DETECTS + flags on the point (local, fast); the service ROUTES (central, policy). Absent
the deadband on the limit → the alarm itself chatters at the threshold (same physics as §805.2).

## 805.5 — The one-bit trace: a fault flag from sensor to console and back `[CERT]`
Follow ONE bit — the point's `BStatus` fault/alarm/unacked flag — end to end:
1. **Sensor bad / out of limit** → the value's `BStatus` goes fault (a driver proxy sets it) OR the alarm ext's limit
   trips. `BLoopPoint` on a NaN input sets `pidOutput.setStatusFault(true)` (`:372`); a limit ext sets
   `BStatus.makeAlarm/makeFault/makeUnackedAlarm(...,true)` on the point (`BAlarmSourceExt:676,682,718`).
2. **Record** → the ext builds a `BAlarmRecord` (uuid, timestamp, sourceState, presentValue, msgText).
3. **Route** → `BAlarmService.routeAlarm` (`:244`) → `doRouteAlarm` (`:577`) → `doRouteToRecipient` (`:573`) → the
   `BAlarmClass` → recipients.
4. **Console/operator** → the recipient (alarm console / email) shows the record; the point's live status also carries
   the bit, so any view of the point shows fault/alarm/unacked.
5. **Ack** → operator invokes ack → `doAckAlarm` (`:503`) routes an ack record AND `makeUnackedAlarm(status,false)`
   (`:521`) CLEARS the unacked bit on the live point. The bit's journey: point BStatus → record → service → recipient →
   operator → back to the point BStatus. One bit governs detection, annunciation, and acknowledgement.

## 805.6 — Honest verdict: Tridium ships NO ODE / matrix / state-space math `[CERT — negative]`
Searched kitControl + control-rt + analytics-rt for `matrix|ODE|differential|integrate|stateSpace|Runge|eigenvalue`:
- **No standalone integral/derivative/rate block** — the only time-integral in the corpus is `BLoopPoint.errorSum` (a
  scalar Euler accumulator, `/60` per-minute). `kitControl/math/*` = pure algebra (Add/Mul/Sin/Sqrt/Pow…), no time term.
- **No matrix/vector numeric type, no state-space, no Runge-Kutta.** `BAnalyticVector` is a value wrapper; analytics-rt
  `BAlgorithmBlock` is a HISTORICAL/offline DSP node over `AnalyticTrend`, not a real-time solver.
**Implication for the kit**: our model-based `CompressorControl.step()` (a physics model evaluated per cycle) is a MORE
advanced approach than anything Tridium ships — it is legitimate first-principles engineering, not a failure to reuse a
stock block, because no stock ODE/model block exists. The kit's "pure step() model" doctrine is consistent with (and
beyond) Tridium's own scalar-PID practice; say so, don't imply a missing reuse.

## 805.7 — Structure & the engine (REMITTANCE [B729]/[B730]/[B737]) `[CERT]`
Two paths on a control component: the **timed cycle** (`doTimerExpired → calculatePoint → doExecute → onExecute(out,cx)`)
where ALL real-time math runs, and the **event path** (`changed(p,cx)`) which only RE-CONFIGURES (timer reinit, errorSum
rescale, ramp restart) — never runs the loop math. `executeTime` defaults 500 ms, floored 100 ms–60 s (the [B801] Clock
floor). Frozen slots for the fixed schema; the loop state (`errorSum`,`lastError`) is transient fields, not persisted.

## 805.8 — Verification: `javax.baja.test.BTest` `[CERT]`
`BTest extends BObject` (`:29`); a test is a plain Java class whose methods start with `"test"` (discovered by
`getMethods().filter(startsWith "test")` `:48`, NO annotation); `verify(cond[,msg])` (`:51,59`) asserts;
`createTestStation(bogXml)` (`:281`) encodes a BOG string to an in-process `BStation` so a `BComponent`'s control logic
is testable OFF a real station (bridges to TestNG when `junit=true`). This is Tridium's own answer to "how do you verify
a control component" — and it matches the kit's pure-seam testing ([B762]/[B743]): extract the math, test it in-process.

## 805.9 — Flowchart / flujograma convention (copy-ready kit template) `[INFER]`
A control component's doc should carry ONE flowchart with these fixed lanes (so every component is documented the same
way and a reviewer can find the protection path in seconds):
```
STATES:      enum of operating modes (Off/Auto/Manual/Fault/Lockout)
INPUTS:      each input + its null/fault handling (hold? fail-safe value?)
TIMERS:      interval(s) + the floor (≥1s, B801); who cancels on stop (B787)
CONTROL:     the per-cycle math (the step / PID / deadband) — one box
PROTECTIONS: each limit → which BStatus bit it sets → which alarm class routes it
OUTPUTS:     clamped range + the fail-safe value on fault (never NaN, never last-if-stale)
FEEDBACK:    status bits (fault/alarm/overridden/unacked) + alarm record + ack path
```
Rendered as a state diagram (mermaid `stateDiagram-v2`): normal → (limit trip) → alarm[bit set + record routed] →
(operator ack) → normal; plus a fault sink (bad input → hold + fault bit). The invariant a reviewer checks: EVERY output
path has a defined value on fault, and EVERY protection sets a bit AND routes a record.

## 805.10 — Kit implication `[INFER, grounded in 805.1-9]`
PROPOSED `types/logic.md` §"control-logic patterns" — copy-ready shapes, each citing the Tridium exemplar:
- **PID** → `BLoopPoint` shape: timed `Clock.schedulePeriodically`, anti-windup clamp on the accumulator, NaN-guard that
  holds+faults, direct/reverse, bumpless on enable/tune. Never integrate without clamping to the output range.
- **Deadband** → `BTstat` shape: `sp ± diff/2`, latch on cross, hold between — mandatory for any two-position output on a
  compressor/relay (anti-short-cycle).
- **Latch** → `BLatch` D-latch for sample-hold; an SR safety-interlock latch is author-built (Tridium ships none).
- **Protection** → a `BAlarmSourceExt`-style point extension: limit + deadband + time-delay → set the BStatus bit → build
  a `BAlarmRecord` → route via the alarm service; give the ack path.
- **Output contract**: every output has a fail-safe value on fault (never NaN, never a silently-stale hold without a
  fault bit). **Verify** with a `BTest`-style in-process station + a pure-seam unit test.
- **Model math**: a physics `step()` model is legitimate and beyond stock kitControl (no ODE block exists) — keep it pure.
- Adopt the §805.9 flowchart template for every documented control component.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | BLoopPoint PID = P+I(/60)+D on a timed clock; tuning slots Kp/Ki/Kd/bias/max/min; timer Clock.schedulePeriodically(cancel-on-rearm) | [CERT] | BLoopPoint.java:119-124,340,350,410-414 |
| 2 | Anti-windup clamps errorSum to the output range; NaN/Infinite input → setStatusFault(true) + hold last output; direct action negates pv | [CERT] | BLoopPoint.java:369-374,391-401,414 |
| 3 | Bumpless: disable seeds errorSum, tuning-change rescales errorSum, rampTime limits rate | [CERT] | BLoopPoint.java:502-515,545-566,444-459 |
| 4 | BTstat deadband: high=sp+diff/2, low=sp−diff/2, latch on cross + hold between (anti-chatter) | [CERT] | BTstat.java:118-139 |
| 5 | BLatch is an edge-triggered D-latch (sample-hold); NO SR/set-reset latch exists in kitControl | [CERT]+negative | BLatch.java:69-84; corpus search |
| 6 | Protection = BAlarmSourceExt (point ext) + BOutOfRangeAlgorithm (high/low/deadband); raises by setting BStatus alarm/fault/unacked bits + BAlarmRecord; routes via BAlarmService.routeAlarm; ack clears the unacked bit | [CERT] | BAlarmSourceExt.java:184,209,660,676,682,718,503,521; BOutOfRangeAlgorithm.java:59-65; BAlarmService.java:244,573,577 |
| 7 | Tridium ships NO ODE/matrix/state-space/standalone-integral — only BLoopPoint's scalar Euler errorSum; analytics = historical DSP | [CERT — negative] | corpus search; kitControl/math (algebra only); analytics BAlgorithmBlock |
| 8 | BTest: plain class, methods start with "test" (no annotation), createTestStation = in-process station for off-station control tests | [CERT] | BTest.java:29,48,51,281 |

**Tally**: 6 [CERT] · 2 [CERT]+negative. All file:line grep-verified this session. The control-physics WHY (§805.1-4)
and the §805.9-10 kit deltas are [INFER] grounded in the [CERT] mechanism + control theory. Dedupe: engine/execute-changed
split + monitor + action flags + timer lint are REMITTANCE ([B729]/[B730]/[B737]/[B775]/[B776]/[B787]/[B801]); this block
adds the CONTROL-LOGIC patterns, the physics rationale, the one-bit trace, and the honest ODE negative.

## Connections
- **[B729]/[B730]/[B737]** (execute/changed + engine thread + rt idioms — the substrate), **[B775]** (BAbstractMonitor
  watchdog — the alarm ext is the point-local sibling), **[B776]** (action protection flags), **[B787]** (timer-ticket
  lint — BLoopPoint cancels on re-arm), **[B801]** (Clock delay floor — executeTime floor), **[B804]** (history ext —
  same point-extension placement model), **[B762]/[B743]** (pure-seam testing — BTest is Tridium's version), **[B802]**
  (Sys.getService for BAlarmService). Kit: `types/logic.md` §"control-logic patterns" + the flowchart template.

## Open gaps
- **B805-G1** (bounded): the `BOffnormalAlgorithm` time-delay state machine (offnormal→normal debounce) and the
  `BLimitEnable` per-limit gating — named, not fully traced; the raise/route path is [CERT].
- **B805-G2** (bounded): kitControl's staged-equipment blocks (`BInterstartDelayMaster`, `BLeadLagRuntime`, `BSequence`)
  as sequencing exemplars — located, not opened; a follow-up if the kit needs a staging pattern.
