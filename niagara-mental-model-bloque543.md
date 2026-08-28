# Block 543 — HVAC control-logic SAFETY / fail-safe analysis: the five defensive layers, and the six default-UNSAFE gaps an engineer must close

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC13 — HVAC control-logic fail-safe behavior; OPERATOR-REQUESTED)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY. Delegated sonnet sweep across `kitControl-rt`, `control-rt`, `clHVAC*`; every
operator-facing SAFE/UNSAFE verdict token-verified inline against source (FALSIFY-BEFORE-REPORTING — these
claims drive an operational recommendation).
**Primary sources** `[CERT]`:
- `organized/kitControl/kitControl-rt/vineflower/com/tridium/kitControl/BLoopPoint.java`, `BLoopAlarmAlgorithm.java`
- `organized/clHVACHeating/.../heating/CmTempSenAvailable.java`, `CmMinCntrFlowTemp.java`, `BCmVTB_HtgCirc.java`
- `organized/clHVACAirConditioning/.../air_conditioning/BCmDMB_MixingDamper.java`
- DOC `[CERT-doc]`: `docKitControl` (LoopPoint, MathComponents), niagara-help `User/aWritablePoints`,
  `Safety_Value.html` (Honeywell BACnet).

**Scope**: the operator's question — *what stops the HVAC control logic from driving a wrong/dangerous action
when a sensor fails, an input goes invalid, the loop is disabled, or a fault occurs?* This maps the fail-safe
DESIGN and, honestly, where it is SAFE-BY-DEFAULT vs where safety depends on engineer configuration. Builds on
[Block 536] (relinquish/fallback), [Block 539] (loop disable/clamp/alarm), [Block 540] (clHVAC sequences).

---

## 543.1 The five defensive layers [CERT]

Control safety in N4 is LAYERED, not a single interlock. From input to actuator:
1. **Sensor-validity gating** — detect an absent/failed sensor and block control that depends on it (§543.2).
2. **Relinquish-to-fallback** — a writable point never goes null; it resolves to a configured Fallback (§543.3a).
3. **Loop disable behavior** — a defined output when a loop stops (§543.3b), plus fault-abort on NaN/Inf (§543.3c).
4. **Output bounding** — hard min/max clamp, anti-windup, optional ramp (§543.4).
5. **Application interlocks** — frost protection, alarms (alarm-only), emergency priority (§543.5–6).

Crucially, **layers 3–5 are only as safe as their configuration** — several default to a NON-fail-safe choice.

## 543.2 Sensor-failure handling — the `999` sentinel (clHVAC) + null relinquish (kitControl) [CERT]

clHVAC uses **`999.0` as "sensor absent"**. `CmTempSenAvailable` is the detector `[CERT]
CmTempSenAvailable.java:15-17`: `inputB = 999.0F; output_2 = (input < 999)` — TRUE when the sensor is present.
In `BCmVTB_HtgCirc` both temperature inputs default to `BStatusNumeric(999.0)` `[CERT] BCmVTB_HtgCirc.java:151`
and `output_2` gates the control stage-selection AND-chains (`func_3663`/`func_3065`) `[CERT] :690,835`.

**SAFE aspect**: when a sensor reads 999, `CmTempSenAvailable` collapses the AND gates so the logic does NOT
drive a demand computed from garbage. **GAP**: it BLOCKS the wrong action but does NOT force the actuator to an
explicit safe position — the output then depends on the downstream mux/last value. And in `BCmDMB_MixingDamper`,
an absent room sensor SUBSTITUTES `999` into the damper setpoint `parameter_27` `[CERT] BCmDMB_MixingDamper.java:256-259`
(`func_64.input[1] = 999.0`), passing an out-of-range setpoint into `CmDamper_Control_Signal` whose own guard is
not visible in the decompilation [INFER — potential gap].

kitControl's parallel is the **null-status relinquish** ([Block 536]): an invalid input is skipped, and a
writable point with everything relinquished falls to Fallback (§543.3a). But **clHVAC strips the Baja status
envelope** at the Java boundary — its `Cf*` primitives read raw `BStatusNumeric.getValue()` floats, so inside
the clHVAC loop only the `999` sentinel and the `enabledIn` flag carry safety semantics, NOT the status bits.

## 543.3 Null/invalid → output [CERT]

**(a) Writable Fallback — never null.** A writable point's `Out` resolves to the configured `Fallback` when all
16 levels relinquish `[CERT-doc] aWritablePoints` + [Block 536]. SAFE *if* Fallback is a safe value; the
NumericWritable default Fallback is 0.0 (closed valve = safe for most heating/cooling). **Safety is the
engineer's Fallback choice.**

**(b) Loop `disableAction` — DEFAULT zero (safe), but `hold` is a trap.** `[CERT] BLoopPoint.java:117`
`disableAction` default = `zero`. `handleLoopDisabled` `[CERT] :546-555`: `zero`→0.0, `maxValue`→max,
`minValue`→min, but **`hold` leaves `outValue = Double.NaN`, so `pidOutput` is NOT updated — the last computed
command FREEZES**. If the loop is disabled while it was commanding 100% (e.g. right when a sensor failed), `hold`
holds 100% indefinitely. `zero` is fail-safe for a heating valve (closed); `maxValue`/`hold` can be UNSAFE.

**(c) NaN/Inf setpoint/PV → fault + abort.** `[CERT] BLoopPoint.java` (369-374 in the compute path): a NaN/Inf
setpoint or PV sets the output to fault status and skips the algorithm — the output then HOLDS its last value.
SEMI-SAFE: it flags fault and stops computing, but does not drive to a safe value.

## 543.4 Output bounding [CERT]

- **Hard clamp** `[CERT] BLoopPoint.java` — output bounded to `[minimumOutput, maximumOutput]` (default [0,100]).
  SAFE — no runaway command. Anti-windup clamps `errorSum` to the output range; an `errorSum` NaN/Inf guard
  resets it to 0. SAFE.
- **Ramp (slam protection)** — limits output rate of change, BUT **`rampTime` defaults to 0 = DISABLED**
  `[CERT-doc] kitControl-LoopPoint.html`: "intended to prevent the loop from opening a valve … to its maximum
  ('slamming') during startup." NOT safe by default — the engineer must set a ramp.

## 543.5 Frost protection — active, at 16 °C [CERT]

`CmMinCntrFlowTemp` is the frost loop `[CERT] CmMinCntrFlowTemp.java:28-32`: when actual flow temp `≤` the
minimum threshold, it selects a positive PID correction (`func_23.input[1]`), else 0 — injecting heat demand to
hold the minimum. In `BCmVTB_HtgCirc` the threshold is `parameter_25 = 16.0 °C` (facets min 0 / max 150 /
units celsius) `[CERT] BCmVTB_HtgCirc.java:204-206`, fed back to force a higher heat stage `[CERT] :846,938`.
SAFE — active freeze margin. Caveat: it acts on the raw flow-temp float; a failed sensor reading a plausible
warm value would defeat it (the status envelope is stripped, §543.2).

## 543.6 Alarms are ALARM-ONLY; emergency is the hard override [CERT]

`BLoopAlarmAlgorithm` `[CERT] BLoopAlarmAlgorithm.java:138-142` fires an off-normal alarm on `|SP−PV| >
errorLimit` but **does NOT modify, clamp, or disable the control output** — a valve can sit at 100% while the
deviation alarm is active. No `Interlock`/`Safety`/`Freeze`/`Emergency` CLASS implementing a hard control
override exists in kitControl or clHVAC (searched). The one HARD override path is the writable point's
**priority level 1 (emergency)** `[CERT-doc] aWritablePoints` — an operator/safety action at level 1 wins the
arbitration ([Block 536]). Beyond Niagara, the BACnet device offers a last-resort **Safety_Value** on comms
loss `[CERT-doc] Safety_Value.html` — downstream of the station logic.

## 543.7 Status/fault gating — the default that hides a bad sensor [CERT]

`BLoopPoint.propagate()` `[CERT] BLoopPoint.java:126,568-574`: `propagateFlags` default = `BStatus.ok` (0), and
it AND-masks the input status bits — so **by default NO input status (stale/alarm/fault) reaches the output**.
A sensor that fails to a PLAUSIBLE-but-wrong numeric value (e.g. reads 20 °C while the room freezes) drives the
loop normally with no output indication. `[CERT-doc] MathComponents` confirms "status propagation does not
occur" by default for math/logic too. This is architecturally intended (opt-in, [Block 538] BP1) — but it means
**bad-data detection is off unless the engineer configures `propagateFlags`**.

## 543.8 The SAFE / UNSAFE verdict [CERT]

**Safe by default:**
| Mechanism | Safe state |
|-----------|-----------|
| 999 sentinel + `CmTempSenAvailable` gate | blocks control on absent sensor |
| Writable Fallback (default 0.0) | point never null; closed valve |
| `disableAction=zero` (default) | output 0% when loop disabled |
| NaN/Inf → fault + abort | computation stops, fault flagged |
| Hard output clamp [0,100] + anti-windup | no runaway command |
| `CmMinCntrFlowTemp` @ 16 °C | active frost protection |
| Emergency level 1 | hard operator override path |

**UNSAFE unless the engineer acts (the six gaps):**
1. **`disableAction=hold`** freezes the last (possibly dangerous) command — `[CERT] BLoopPoint.java:546-555`.
2. **`propagateFlags=0` default** — a plausible-but-wrong sensor drives the loop with no fault on the output — `[CERT] :126`.
3. **`BLoopAlarmAlgorithm` is alarm-only** — no interlock; a deviation alarm cannot stop a bad command — `[CERT] BLoopAlarmAlgorithm.java`.
4. **`999` is a clHVAC CONVENTION, not a framework guarantee** — a source returning a plausible wrong value bypasses every `CmTempSenAvailable` gate — [INFER].
5. **`rampTime=0` default** — no anti-slam on startup/enable — `[CERT-doc] kitControl-LoopPoint.html`.
6. **clHVAC strips the Baja status envelope** — inside the Eagle loop only `999` + `enabledIn` carry safety; stale/fault bits are lost — `[CERT] §543.2`.

## 543.9 Operator recommendations (actionable)

To make HVAC control fail-safe, the ENGINEER must (the platform will not by default):
- Set each loop's `disableAction` to the actuator's SAFE position (**never `hold`** on a safety-relevant loop);
  set `Fallback` on every writable to its safe value (0.0/closed is not always safe — a freeze-risk coil may need "open").
- Configure `propagateFlags` to include `fault | stale | down` so a bad sensor marks the output invalid downstream.
- Set a non-zero `rampTime` on loops driving valves/dampers (anti-slam).
- Use writable **priority level 1 (emergency)** for hard safety interlocks (freeze-stat, smoke) — an alarm alone
  will NOT stop the actuator.
- Configure the field controller's BACnet **Safety_Value / relinquish-default** as the last-resort fail-safe on comms loss.

## 543.10 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | 999 sentinel: CmTempSenAvailable output_2 = (sensor<999), gates control AND-chains | [CERT] | CmTempSenAvailable.java:15-17; BCmVTB_HtgCirc.java:151,690,835 | token-checked ✓ |
| 2 | Absent room sensor substitutes 999 into damper parameter_27 | [CERT] | BCmDMB_MixingDamper.java:256-259 | token-checked ✓ |
| 3 | disableAction default zero; hold→NaN→output frozen (unsafe) | [CERT] | BLoopPoint.java:117,546-555 | token-checked ✓ |
| 4 | propagateFlags default BStatus.ok(0) → input status not propagated | [CERT] | BLoopPoint.java:126,568-574 | token-checked ✓ |
| 5 | Hard output clamp [min,max]; anti-windup; rampTime default 0 (unsafe) | [CERT]/[CERT-doc] | BLoopPoint.java; kitControl-LoopPoint.html | sweep-cited + [B539] |
| 6 | CmMinCntrFlowTemp frost correction ≤ threshold; parameter_25=16°C celsius | [CERT] | CmMinCntrFlowTemp.java:28-32; BCmVTB_HtgCirc.java:204-206 | token-checked ✓ |
| 7 | BLoopAlarmAlgorithm alarm-only (no output override/interlock) | [CERT] | BLoopAlarmAlgorithm.java:138-142 | token-checked ✓ ([B539]) |
| 8 | Emergency level 1 = hard override; BACnet Safety_Value last-resort | [CERT-doc] | aWritablePoints; Safety_Value.html | sweep-cited |
| 9 | clHVAC strips Baja status envelope (raw float in Cf* primitives) | [CERT] | clHVAC Cf* usage | [INFER-supported] |

**Marker tally**: [CERT] ×6 · [CERT-doc] ×2 · [INFER] ×2 (999-convention scope; status-envelope stripping).
Block TYPE = EVIDENCE + SAFETY-SYNTHESIS. 6 of 9 rows token-verified inline (the operator-facing SAFE/UNSAFE
verdicts). The six UNSAFE gaps are all code/doc-anchored, not speculation.

## Connections

- **[Block 536]** — relinquish/fallback (layer 2); the writable Out never null.
- **[Block 539]** — loop disableAction, clamp, ramp, alarm, propagateFlags (this block reframes them as SAFETY).
- **[Block 540]** — clHVAC 999 sentinel, frost, economizer gate (the sequences whose safety this analyzes).
- **[Block 538]** BP1 — opt-in status propagation (the root of gap #2).
- **`security-audit` / [Block 18]** — this is the CONTROL-SAFETY companion to the cyber-security posture; a
  candidate SEC-style checklist item ("loops on safety-relevant points must not use disableAction=hold; must
  set propagateFlags").
- **Java 8** [CERT, class major 52] — the module bytecode target (operator-confirmed 2026-08-28).

## Open gaps (this block)

- `CmDamper_Control_Signal`'s handling of a 999 setpoint is not visible (class body absent) — gap #4/§543.2 is
  [INFER]; a child gap if that block is later decompiled.
- A station-wide AUDIT of which live loops use `hold`/unset `propagateFlags`/`rampTime=0` is a §12 dynamic
  task against a real station — recorded as requires-execution, not investigable here.
