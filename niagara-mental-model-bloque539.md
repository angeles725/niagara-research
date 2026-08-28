# Block 539 — BLoopPoint deep: ramp-aware anti-windup, direct/reverse sign, the four disable actions, the deviation loop alarm, propagateFlags masking, and the official P/PI/PID tuning methodology

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC4 — the PID loop primitive, deep)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY. Delegated sonnet sweep over `BLoopPoint`/`BLoopAlarmAlgorithm` + official `docKitControl`
tuning pages; load-bearing code lines and doc quotes token-verified inline.
**Primary sources**:
- CODE `[CERT]`: `organized/kitControl/kitControl-rt/vineflower/com/tridium/kitControl/BLoopPoint.java`,
  `BLoopAlarmAlgorithm.java`, `enums/BLoopAction.java`, `enums/BDisableAction.java`.
- DOC `[CERT-doc]`: `docKitControl/.../extracted/doc/` — `kitControl-LoopPoint.html`,
  `Proportional-onlyControl-24A3A1A6.html`, `ProportionalWithIntegralPIControl-24A6068F.html`,
  `ProportionalWithIntegralAndDerivati-24A75A7A.html`.

**Scope**: the operational depth of the PID loop that [Block 537] §537.4 only summarized — ramp, execute-time
bounds, action sign, disable behavior, the loop deviation alarm, output-status masking, and the VENDOR'S
stated tuning method. [Block 537]'s structural props/algorithm are REMITTANCE (cited, not re-derived).
**niagara-help has NO kitControl PID tuning guide** — `guide-search "PID"/"tuning"` returns only network-driver
tuning; the tuning doctrine lives entirely in the `docKitControl` HTML `[CERT-doc]`.

---

## 539.1 Ramp — rate-of-change limiter with ramp-aware anti-windup [CERT + CERT-doc]

Three fields drive the ramp `[CERT] BLoopPoint.java:138-140`: `rampStartTicks`, `rampEndTicks`,
`rampConst = |maximumOutput − minimumOutput|` (the full output span). While `now < rampEndTicks`
`[CERT] :444-458`:
```java
double maxChange = rampConst * delta / getRampTime().getMillis();   // units allowed to move THIS cycle
// clamp pv to out ± maxChange; and rescale the integral to match:
errorSum = errorSum * clampedOut / desiredPv;
```
So the output cannot traverse its full range faster than `rampTime` ms. The KEY subtlety: when the ramp
clamps `pv`, it **also rescales `errorSum` proportionally** — ramp-aware anti-windup that keeps the integral
consistent with the output actually emitted (no windup during the ramp). Armed at `started()` (startup) and on
`loopEnable` false→true and on `rampTime` edits `[CERT] :330,516-524`. Default `rampTime = 0 ms` → ramp
disabled until set. Official purpose `[CERT-doc] kitControl-LoopPoint.html`: prevent "slamming" a valve to its
limit at startup ("minimum time that the output can ramp completely from Minimum Output to Maximum Output").

## 539.2 executeTime bounds [CERT]

`[CERT] BLoopPoint.java:142-143` `LOOP_MIN_EXECUTE_TIME = 100L`, `LOOP_MAX_EXECUTE_TIME = 60000L`; clamped on
edit `[CERT] :495-498` to **[100 ms, 60 s]**. Default 500 ms `[CERT] :114`. The loop runs on its own periodic
timer at `executeTime`, independent of the event-driven engine — a control loop is one of the few N4 elements
with a fixed cadence rather than pure change-propagation.

## 539.3 direct vs reverse — the sign is the ONLY difference [CERT + CERT-doc]

`[CERT] enums/BLoopAction.java:15-18`: `DIRECT=0` (default), `REVERSE=1`. The PID sum is computed identically;
`direct` negates it `[CERT] BLoopPoint.java:414-415` (`pv = -pv`), with `error = setpoint − controlledVariable`
`[CERT] :386`:
- **reverse** (no negation): output rises as PV falls below SP → **HEATING** `[CERT-doc]
  kitControl-LoopPoint.html`: "Reverse increases the loop output as the controlled variable decreases to less
  than the setpoint … a heating application."
- **direct** (negated): output rises as PV climbs above SP → **COOLING** `[CERT-doc]`: "Direct increases the
  loop output as the controlled variable increases to greater than the setpoint … a cooling application."

## 539.4 disableAction — four behaviors + bumpless-transfer pre-load [CERT]

`[CERT] enums/BDisableAction.java:19-22`: `maxValue=0, minValue=1, hold=2, zero=3`. **Enum DEFAULT is
maxValue, but the BLoopPoint property default is `zero`** `[CERT] :65,117`. When `loopEnable` goes false the
periodic timer is cancelled and `handleLoopDisabled()` runs `[CERT] :529-565`:

| disableAction | Output | errorSum side-effect |
|---------------|--------|----------------------|
| `zero` (default) | 0.0 | pre-loaded 0 |
| `maxValue` | maximumOutput | pre-loaded to the boundary |
| `minValue` | minimumOutput | pre-loaded to the boundary |
| `hold` | pidOutput UNCHANGED (NaN-guard skips setValue) | unchanged (bumpless hold) |

For zero/max/min the code **pre-loads `errorSum` to the value that would reproduce that output**
(`errorSum = ±outValue/kPkIconst`, sign per action) — so on re-enable the integral starts bumpless rather than
jumping. `lastExecuteTime = 0L` guarantees a clean delta on re-enable.

## 539.5 BLoopAlarmAlgorithm — a PV-deviation alarm, not output saturation [CERT]

`[CERT] BLoopAlarmAlgorithm.java:38` extends `javax.baja.alarm.ext.offnormal.BTwoStateAlgorithm` (inherits the
two-state off-normal machine + time delay). It monitors **`error = setpoint − controlledVariable`**, NOT the
output `[CERT] :138-142`. Config: `errorLimit` (high threshold), `deadband` (hysteresis on return-to-normal),
`lowDiffLimitEnabled` + `lowDiffLimit` (asymmetric low bound; symmetric ±errorLimit by default). `isNormal()`:
off-normal when `|SP−PV|` exceeds the limit; when already off-normal, return-to-normal requires re-entering
`±(errorLimit − deadband)` (chatter suppression). `isGrandparentLegal()` `[CERT] :115` restricts it to sit as
a grandchild of a `BLoopPoint` (inside the loop's alarm extension). So the loop's alarm answers "is the
process holding setpoint?", independent of how saturated the output is.

## 539.6 out clamp + propagateFlags status masking [CERT]

Output pipeline order `[CERT] BLoopPoint.java:444-465`: ramp limiter → **final unconditional min/max clamp**
(`pv>max→max; pv<min→min`) → `pidOutput.setValue(pv)`. A NaN/Inf setpoint or PV sets fault on the output and
skips the algorithm `[CERT] :369-374`.

`propagateFlags` is a **whitelist mask**, default `BStatus.ok` (= 0) `[CERT] :126,568-574`:
```java
propagatedStatus = (loopEnable | setpoint | controlledVariable).statusBits
propagatedStatus &= propagateFlags.bits          // AND-mask: whitelist
out.status = pidOutput.status | propagatedStatus  // own fault always survives (it's in pidOutput.status)
```
RULE: **by default NO input status bit reaches `out`** — the loop's output status reflects only the loop's own
health (its NaN/Inf fault). To let, say, a `down`/`stale` bit from the setpoint signal flow downstream, the
engineer must add that bit to `propagateFlags`. This is the loop-level instance of the opt-in status rule
[Block 538] BP1 documented for math/logic blocks — and it prevents an "overridden" bit on a commanded setpoint
from spuriously marking the whole downstream chain.

## 539.7 The OFFICIAL tuning methodology [CERT-doc]

The vendor doctrine (token-verified quotes):

- **Proportional constant kP** `[CERT-doc] Proportional-onlyControl-24A3A1A6.html`:
  `kP = (maxOutput − minOutput) / throttlingRange` — e.g. 100% output over a 20°F throttling range → kP = 5.
- **P-only**: `Output = kP·ES + bias`; set `bias` to the output midpoint (the output when PV = SP); kI = kD = 0.
  Too-high kP → constant oscillation.
- **PI is RECOMMENDED for most loops** `[CERT-doc] ProportionalWithIntegralPIControl-24A6068F.html`
  (token-verified): "recommended for most control loops, because the integral term eliminates the setpoint
  offset inherent" in P-only. Set `bias = 0` (the integral becomes the adjustable bias); typically REDUCE kP
  vs P-only because the integral corrects offset over time. **integralConstant kI is in REPEATS PER MINUTE**
  (reset rate); "0.5 is a good starting point for many loops" (token-verified); smaller = slower response,
  less overshoot. Windup is "prevented by limiting the ErrorSum value based on Maximum/Minimum Output" — the
  doc face of §539.1/[B537]'s anti-windup clamp.
- **PID is SELDOM USED** `[CERT-doc] ProportionalWithIntegralAndDerivati-24A75A7A.html` (token-verified):
  "difficult to tune and (often for this reason) seldom used." Use only for a "long reaction time" process
  (large thermal mass) where derivative curbs the overshoot PI would leave. **derivativeConstant kD is in
  SECONDS** ("differs from some systems using derivative in minutes"); "less than 10 seconds should be tried
  first" then increase only if steady-state stays stable. Full form:
  `Output = kP·(ES + kI·ErrorSum + kD·(ES − LastES)/Δt)`.

This maps directly onto the code (§537.4): kP=`proportionalConstant`, kI=`integralConstant` (÷60 → per-minute
→ per-second in code), kD=`derivativeConstant`, `bias` added only when kI=0 (P-only).

## 539.8 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Ramp: maxChange = rampConst·Δ/rampTime; rescales errorSum (ramp-aware anti-windup) | [CERT] | BLoopPoint.java:444-458 | sweep-cited + logic-checked |
| 2 | Ramp armed at startup + enable-transition + rampTime edit; default 0=off | [CERT] | :330,516-524 | sweep-cited |
| 3 | executeTime clamp [100 ms, 60 s], default 500 ms | [CERT] | :142-143,495-498,114 | token-checked ✓ |
| 4 | direct negates PID sum (pv=-pv); DIRECT=0 default | [CERT] | :414-415; BLoopAction.java:15-18 | token-checked ✓ |
| 5 | direct=cooling, reverse=heating | [CERT-doc] | kitControl-LoopPoint.html | sweep-cited (quote) |
| 6 | disableAction 4 values (max0/min1/hold2/zero3); property default zero; hold=NaN-hold | [CERT] | BDisableAction.java:19-22; BLoopPoint.java:65,117,545-565 | token-checked ✓ |
| 7 | disable pre-loads errorSum for bumpless re-enable | [CERT] | BLoopPoint.java:545-565 | sweep-cited |
| 8 | BLoopAlarmAlgorithm extends BTwoStateAlgorithm; alarms on SP−PV deviation w/ deadband hysteresis | [CERT] | BLoopAlarmAlgorithm.java:38,138-142 | token-checked ✓ |
| 9 | propagateFlags = whitelist AND-mask, default 0 → no input status to out; own fault always survives | [CERT] | BLoopPoint.java:126,568-574 | sweep-cited + logic-checked |
| 10 | kP=(maxOut−minOut)/throttlingRange; PI recommended; kI repeats/min start 0.5 | [CERT-doc] | Proportional-only + PI doc pages | token-checked ✓ |
| 11 | PID seldom used/hard to tune; kD in SECONDS, start <10s | [CERT-doc] | PID doc page | token-checked ✓ |

**Marker tally**: [CERT] ×7 · [CERT-doc] ×4 · [INFER] ×0 substantive (one design-intent note on propagateFlags
marked [INFER]). Block TYPE = EVIDENCE (code + doc). 7 of 11 rows token-verified inline against source/doc.

## Connections

- **[Block 537]** §537.4 — the BLoopPoint structure/algorithm this block deepens (REMITTANCE).
- **[Block 536]** — `BLoopPoint extends BNumericPoint`; its `out` is a control-point output, but a LoopPoint is
  READONLY (computed), not one of the writable arbitrated points.
- **[Block 538]** BP1 — the opt-in status rule; §539.6 is its loop-level instance.
- **Forward**: KC5 (clHVAC apps — many wrap loops/sequences), KC7 (honeywellFunctionBlocks has its own
  control-loop blocks — compare), KC8 (loop `out` → writable-point priority level).

## Open gaps (this block)

- The alarm-extension attachment chain (`BAlarmSourceExt` hosting `BLoopAlarmAlgorithm`) is named, not
  decompiled — belongs to the alarm subsystem, not this focus. Recorded, not a kitControl gap.
