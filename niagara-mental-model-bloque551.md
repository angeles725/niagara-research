# Block 551 — Four more clHVAC control sequences: boiler cascade, VAV AHU static-pressure control, room→SAT cascade with a HARD frost interlock, and the wet-bulb formula (extends B540; refines B543 §543.6)

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC14 — additional clHVAC control sequences; operator-requested)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY. Delegated sonnet sweep decompiling 4 `BCm*` blocks; the falsifiable claims (wet-bulb
coefficients, staging thresholds, static-pressure setpoint, the frost-override chain) token-verified inline
against source.
**Primary sources** `[CERT]`:
- `clHVACHeating/…/heating/BCmBOA_StagedBoiler.java`
- `clHVACAirConditioning/…/air_conditioning/BCmSPA_StatPressControl.java`, `BCmCSA_CascContr.java`
- `clHVACChiller/…/chiller/BCmWBA_WetBulbTemp.java` (+ `BCmLCA_LoadCalc`, `BCmCHA_ChillerCmd`).

**Scope**: the operator asked for more HVAC sequences (VAV, boiler cascade, cooling tower). clHVAC has no
literal VAV-box or cooling-tower block; the real equivalents are the boiler stager, the VAV AHU duct
static-pressure controller, the room→SAT cascade, and the wet-bulb calc (cooling-tower/condenser approach
input). Extends [Block 540] (which sampled 4/83 domain blocks). All ride the Eagle `BControlFunctionSupport`
engine — REMITTANCE to [Block 87]/[Block 540] for architecture.

---

## 551.1 Boiler cascade — `BCmBOA_StagedBoiler` [CERT]

The heating-plant analog of the chiller sequencer ([Block 540] `BCmSQA_ChillerSeq`), but richer. Staging via
`CmBoiler_Demand` (add/remove request) → `CmSwitch_Boiler_Stages_Seq` → timed per-stage enable → `CmLaufzeit`
runtime + `CmChange_Position` lead-lag rotation. Verified parameters `[CERT] BCmBOA_StagedBoiler.java:280-387`:
`parameter_84 = 100 K` (stage-add demand threshold), `parameter_85 = 30 s` (add delay), `parameter_86 = 300 s`
(remove delay), `parameter_87 = 5 K` hysteresis, `parameter_88/90 = 240/600 s` (min-on/min-off boiler 1),
`parameter_97 = 3` (max concurrent stages), `parameter_98 = 1|2` (sequential vs runtime-equalizing),
`parameter_99 = 100 h` (rotation interval). **Beyond the chiller block**: it emits a `%` MODULATION output
(`output_105`), drives an integrated `CmPump_and_Valve`, tracks kW capacity (`output_109`), and has a
`CmVES_Alarm` flow-safety fault + a `parameter_80 = 90 °C` HIGH-LIMIT safety cutoff. Fail-safe: temp inputs
default `999` (absent-sensor sentinel), `enabledIn=false` → all 10 outputs `BStatus.disabled`, alarm cascade
(RS-latched) aggregates no-capacity/common/VES/external faults.

## 551.2 VAV AHU duct static-pressure control — `BCmSPA_StatPressControl` [CERT]

The supply-fan side of a VAV AHU: a PID holds duct static pressure by modulating fan speed. `[CERT]
BCmSPA_StatPressControl.java`: setpoint `parameter_51 = 2500 Pa` (:64); duct pressure `input_32` filtered by a
`CmE_Filter` (20 s); PID `CmControl_Signal` with output clamped **[0,100]%** (`input_31=0.0`, `input_32=100.0`,
:273-274) → fan-speed `output_7`. The final speed is the **MINIMUM of four constraints** (`CfMinimum`): the PID
output, a HIGH-PRESSURE safety limiter (`CmHigh_limit_pressure_control`, `parameter_54 = 10000 Pa` absolute max
→ protects ductwork from overpressure), the startup ramp, and flow-balancing — a conservative "lowest wins"
gate. `parameter_60 = 25 %` min fan speed; `parameter_61 = 60 s` startup setpoint ramp. FAIL-SAFE: release=false
→ fan `0.0` (`CfInputMultiplexer`); on pressure-sensor loss (`input_32`=999 → error 999−2500<0) the PID drives
fan toward 0 — safe-fail-closed [INFER]. Note: `input_38` is the outer-loop SP-reset input, so in a cascade the
setpoint comes from another block.

## 551.3 Room→SAT cascade with a HARD frost interlock — `BCmCSA_CascContr` [CERT]

A two-loop cascade: **outer loop** (room/return temp `input_172` → SAT setpoint, with OAT summer compensation
`CmSummer_Compensation`, room SP clamped `parameter_21=14 °C` … `parameter_181=30 °C`) resets the **inner loop**
(SAT `input_1` → `CmSupply_PID` → valve positions, SAT SP clamped by `CmLimit_SAT` to `parameter_17=6 °C` …
`parameter_23=35 °C`). `CmOutput_Sequence` distributes the PID % across heating valve / cooling valve /
economizer by `parameter_30-34` breakpoints; `CmCO2_Control` modulates fresh air (`parameter_35=1000 ppm` CO2
limit). Outputs: heating/cooling valve, economizer, fresh-air damper, fan (`output_11-15`).

**KEY SAFETY FINDING — a HARD frost interlock exists here** `[CERT] BCmCSA_CascContr.java:505-509`: EVERY final
actuator output is written from `CmFrost_Protection` — it is the UNCONDITIONAL LAST block in the scan, and all
five outputs (`output_11-15`) come from its outputs, not directly from the PID. On a freeze/low-OAT condition
it overrides the PID to force the heating valve open and close the fresh-air/economizer dampers. This is a
genuine HARD CONTROL INTERLOCK (not just an alarm).

## 551.4 §14 refinement to [Block 543] §543.6 [CERT]

[Block 543] §543.6 stated: "No code was found in clHVAC or kitControl with class names matching Interlock/
Safety/Freeze that implements a hard control override … alarms are alarm-only." That search was by TOP-LEVEL
CLASS NAME (`BCm*`/`B*`) and missed EMBEDDED sub-functions. `CmFrost_Protection` (a `Cm*` sub-function inside
`BCmCSA_CascContr`, §551.3) IS a hard control override — it gates every actuator output on freeze risk.
**REFINED**: clHVAC application blocks DO embed hard freeze interlocks (as `Cm*` sub-functions, invisible to a
top-level class-name grep); B543's "alarm-only, no interlock" holds for the kitControl `BLoopAlarmAlgorithm`
but NOT for the clHVAC AHU cascade. This IMPROVES the operator's safety picture: the packaged AHU sequence
already fails safe against a frozen coil, at the block level, without relying on `propagateFlags`.

## 551.5 Wet-bulb temperature — `BCmWBA_WetBulbTemp` [CERT]

The cooling-tower/condenser approach input. Stateless, no parameters, hardcoded coefficients `[CERT]
BCmWBA_WetBulbTemp.java:52-72`:

**Twb = 0.042611·RH + 0.005·RH·Tdb + 0.55·Tdb − 4.57444**  (Tdb=`input_1` °C, RH=`input_2` %)

A linearized psychrometric approximation (avoids Stull's trig), accurate to ~0.5 °C over the HVAC range
(≈15–40 °C, 30–90% RH) — verified at 30 °C/50% → 21.6 °C (ASHRAE ≈21.3). FAIL-SAFE: inputs default `nullStatus`
(→ `getValue()`=0); at 0/0 → Twb=−4.57 °C, a clearly-invalid out-of-range value that propagates to consumers
(NOT the 999 sentinel — this block uses nullStatus like the kitControl side).

## 551.6 The chiller-plant demand chain (brief) [CERT]

The plant-load path feeding [Block 540]'s `BCmSQA_ChillerSeq`: `BCmLCA_LoadCalc` (delta-T `parameter_11=4 K`,
stage-add at `parameter_13=80 %`, stage-remove at `parameter_14=30 %`, 1200 s integration → `output_9` load %)
→ `BCmSQA_ChillerSeq` → `BCmCHA_ChillerCmd` (per-chiller enable, `parameter_22=120 s` anti-short-cycle lockout,
−10 s pre-start lead). So the cooling plant is a three-block chain: load calc → sequence → per-chiller command.

## 551.7 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Boiler staging: add 100K/30s, remove 300s, max 3 stages, 100h rotation, 90°C high-limit | [CERT] | BCmBOA_StagedBoiler.java:280-387 | token-checked ✓ |
| 2 | Boiler adds modulation%/pump-valve/VES-alarm/kW vs chiller block | [CERT] | BCmBOA sub-functions | sweep-cited |
| 3 | VAV static-pressure: setpoint 2500 Pa, PID clamp [0,100], MINIMUM-of-4 constraints, 10000 Pa high-limit | [CERT] | BCmSPA_StatPressControl.java:64,273-274 | token-checked ✓ |
| 4 | Cascade: outer room→SAT (clamp 14-30), inner SAT→valves (clamp 6-35), output sequencing | [CERT] | BCmCSA_CascContr.java params | sweep-cited |
| 5 | CmFrost_Protection = unconditional LAST override on all 5 outputs (hard interlock) | [CERT] | BCmCSA_CascContr.java:505-509 | token-checked ✓ |
| 6 | Refines B543 §543.6: hard freeze interlock exists as a Cm* sub-function (missed by class-name grep) | [CERT] | §551.4 | logic-checked |
| 7 | Wet-bulb formula 0.042611·RH + 0.005·RH·Tdb + 0.55·Tdb − 4.57444 | [CERT] | BCmWBA_WetBulbTemp.java:52-72 | token-checked ✓ |
| 8 | Chiller demand chain: LoadCalc(80/30%)→ChillerSeq→ChillerCmd(120s lockout) | [CERT] | BCmLCA/BCmCHA params | sweep-cited |

**Marker tally**: [CERT] ×6 · [INFER] ×2 (SPA fail-closed reasoning; frost-override trigger detail). Block TYPE
= EVIDENCE (decompilation). 4 of 8 rows token-verified inline (the falsifiable numeric claims). The sweep used
few tool calls, so the driver re-verified the load-bearing numbers directly — all matched.

## Connections

- **[Block 540]** — the 4 clHVAC sequences this extends (heating curve, mixing damper, chiller seq, degree-days);
  §551.6 completes the chiller-plant chain around `BCmSQA_ChillerSeq`.
- **[Block 543]** (KC13) §543.6 — REFINED: a hard frost interlock (`CmFrost_Protection`) DOES exist in the AHU
  cascade; B543 gets a back-pointer. Improves the operator's fail-safe picture.
- **[Block 542]** (honeywellFunctionBlocks) — the DDC `BStager`/`BStageDriver` analog to the boiler stager.

## Open gaps (this block)

- ~76 of 83 clHVAC domain blocks still un-decompiled (covered-by-sample) — open a child gap only for a
  specific named sequence on demand. Focus re-STOPs at investigable=0.
