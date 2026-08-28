# Block 550 — Resolving the CmDamper_Control_Signal safety [INFER]: the mixing-damper block DOES guard the absent-sensor case (refines B543 §543.2)

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC13-G2 — resolve the B543 §543.2 [INFER] on CmDamper_Control_Signal)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of one class (the sweep that produced B543 reported this class body
"not visible"; it IS decompiled — a SCOPING-JUDGMENT hypothesis worth one iteration to test).
**Primary source** `[CERT]`:
`organized/clHVACAirConditioning/clHVACAirConditioning-rt/vineflower/cl/hvac/air_conditioning/CmDamper_Control_Signal.java`.

**Scope**: [Block 543] (KC13) §543.2 left an [INFER] — whether `CmDamper_Control_Signal` handles a `999`
(absent-sensor) setpoint SAFELY was "not visible in the decompilation" (its potential-gap #4/#6). This block
reads that class and RESOLVES the [INFER]. It is the one investigable residue found when re-opening the focus
"a fondo"; after it, the focus re-STOPs at investigable=0.

---

## 550.1 The finding — the block is NOT blind to sensor absence [CERT]

`CmDamper_Control_Signal.Execute()` (305 lines) `[CERT]` builds the damper command through a chain of
`CfInputMultiplexer` selectors that GATE the control signal on availability and enable flags — it does not feed
a raw `999` into the damper math. The output mux chain `[CERT] :209-246`:

- **Absent-sensor branch** `[CERT] :209-212`: `func_25.inputSelect = input_69` (the supply-air-sensor-absent
  flag, derived upstream from the `999` comparison, [Block 540] §540.3). `input[0]` = the normal control signal
  (`func_27.output`); `input[1]` = a DEDICATED alternate branch (`func_136.output`). So when the SA sensor is
  absent, the block SWITCHES to `func_136`, it does NOT compute on the `999` value.
- **Plant-enable fail-safe** `[CERT] :213-216`: `func_26.inputSelect = input_70` (plant enable); `input[0] =
  0.0F`. Plant NOT enabled → output **0.0** (damper to recirc/closed) — an explicit fail-safe zero.
- **Frost branch** `[CERT] :217-220`: `func_84.inputSelect = input_83` (frost protection) selects a frost path
  (`func_235`).
- **Output CLAMP [0,100]** `[CERT] :225-230`: `func_230`/`func_232` bound the result to 0…100 — a `999` leak
  could not produce an out-of-range damper command.
- **Mode gate** `[CERT] :231-246`: `func_28.inputSelect = FloatToInt(input_65)` (plant mode) — modes 0/1/2/6+
  force `0.0`, mode 3 forces `100.0`, only active AHU modes 4/5 pass the computed (clamped) signal. `output_71`
  = this gated value.

Even the SP-through path is bounded: `input_66` (the SP, possibly 999) flows through `func_153`→`func_162`
(`CmIDT20` parameter tables with fixed 0/100/−100 rails) `[CERT] :125-158`, not a raw multiply.

## 550.2 §14 refinement to [Block 543] §543.2 [CERT]

[Block 543] §543.2 recorded, as potential-gap #4/#6: "whether `CmDamper_Control_Signal` handles a 999 setpoint
safely is not visible in the decompilation [INFER]." **RESOLVED — REFINED, not refuted, in the SAFE direction**:
the block DOES guard the absent-sensor case (a dedicated `input_69`-selected branch), clamps its output to
[0,100], and fails safe to 0.0 when the plant is disabled. So the `BCmDMB_MixingDamper` substitution of `999`
into `parameter_27` ([Block 540] §540.3, [Block 543] §543.2) does NOT propagate as garbage into the damper
command — the downstream block detects the absence and re-routes.

This DOWNGRADES the operator-facing risk: of B543's six default-unsafe gaps, the clHVAC-damper-specific worry
(#4, the 999 leak) is now shown to be GUARDED at the block level. The GENERIC gap #6 (clHVAC strips the Baja
status envelope, so only the `999` sentinel + enable flags carry safety) STANDS — the guard works precisely
BECAUSE the sentinel/flags are honored, which is the convention, not a framework guarantee. The operator
recommendations in B543 §543.9 are unchanged; this narrows one worry from "unknown" to "guarded by convention."

## 550.3 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | input_69 (SA-sensor-absent) selects a dedicated branch func_136, not the 999 value | [CERT] | CmDamper_Control_Signal.java:209-212 | token-checked ✓ |
| 2 | Plant-not-enabled (input_70) → output 0.0 (fail-safe closed) | [CERT] | :213-216 | token-checked ✓ |
| 3 | Output clamped [0,100]; mode-gated (most modes force 0.0) | [CERT] | :225-246 | token-checked ✓ |
| 4 | SP path bounded via CmIDT20 parameter tables (fixed rails), not raw | [CERT] | :125-158 | token-checked ✓ |
| 5 | Refines B543 §543.2 [INFER]→[CERT]: damper guards absent-sensor; gap #6 (status-strip) stands | [CERT] | §550.2 | logic-checked |

**Marker tally**: [CERT] ×4 · [INFER] ×0 (resolves a prior [INFER]). Block TYPE = EVIDENCE (decompilation),
COMPACT §14 refinement. All rows token-verified inline.

## Connections

- **[Block 543]** §543.2 (KC13) — the [INFER] this block RESOLVES; B543 gets a back-pointer. Refines its
  potential-gap #4 to "guarded"; gap #6 stands.
- **[Block 540]** §540.3 — `BCmDMB_MixingDamper` + the 999 substitution whose downstream handling this traces.
- **[Block 547]** — `BReliability` sensor-fault vocabulary (the status side the clHVAC loop does NOT carry).

## Open gaps (this block)

- None new. Focus re-STOPs at investigable=0; the only remaining item is KC13-G1 (requires-execution live
  audit). The generic status-envelope-strip (B543 gap #6) is a design property, not an investigable gap.
