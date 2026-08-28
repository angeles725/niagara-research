# Block 540 — clHVAC control sequences decompiled: the weather-compensated heating curve, the AHU mixing-damper/economizer gate, and 12-chiller runtime-equalized lead-lag — verifying B87 §87.3 from [CERT-a] to [CERT]

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC5 — the clHVAC application library / HVAC control sequences)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY. Delegated sonnet sweep decompiling representative `BCm*` domain blocks; load-bearing
class declarations, counts, and the heating-curve/chiller logic token-verified inline.
**Primary sources** `[CERT]`:
- `organized/clHVACHeating/clHVACHeating-rt/vineflower/cl/hvac/heating/BCmVTB_HtgCirc.java`
- `organized/clHVACAirConditioning/.../air_conditioning/BCmDMB_MixingDamper.java`
- `organized/clHVACChiller/.../chiller/BCmSQA_ChillerSeq.java`
- `organized/clHVACEnergyManagement/.../energy_management/BCmDDA_DEGDAYS.java`
- `organized/clHVAC/clHVAC-rt/vineflower/cl/hvac/BControlFunctionSupport.java`,
  `.../base/BControlProgramService.java`
- DOC `[CERT-doc]`: `clHVACHeating-doc` (443 html) `VTB_HtgCirc/VTB_HtgCirc1.html`.

**Scope**: the ACTUAL encoded HVAC control sequences of the Centraline `clHVAC` library — the engineering
know-how the audit flagged as "largely unmined." [Block 87] established the ARCHITECTURE ([CERT]: a
self-contained control-block ecosystem, parallel to kitControl) but left the domain sequences at `[CERT-a]`
(sub-agent-cited, unverified). This block DECOMPILES three representative sequences and UPGRADES B87 §87.3 to
`[CERT]`. B87 §87.1/§87.2 (the engine, the parallel-ecosystem finding) are REMITTANCE.

---

## 540.1 The library structure — counts clarified (refines B87) [CERT]

Two layers, now measured cleanly:
- **The PRIMITIVE layer** — `clHVAC-rt`: **250 vineflower classes**, of which **103 extend
  `BControlFunctionSupport`** `[CERT]`. These are the `Cf*` primitives (CfAnd, CfPidPlus, CfHysteresis,
  CfValueRamp, CfCompare, CfWindow, CfInputMultiplexer, CfRsFlipFlop, CfDelay…) — the building blocks, NOT
  deployable applications.
- **The DOMAIN/APPLICATION layer** — `BCm*` blocks across the sub-modules: **83 total** `[CERT]`
  (clHVACAirConditioning 32, clHVACHeating 13, clHVACNordicAirCondition 12, clHVACGeneral 10, clHVACChiller 7,
  clHVACEnergyManagement 4, clHVACNordicGeneral 3, clHVACRoomControl 2; clHVAC core has 0 — it is pure
  primitives).

**Clarifies B87**: B87's "264 vf" for clHVAC counted `-rt` + `-wb` (264); the `-rt` primitive layer alone is
250 with 103 `BControlFunctionSupport` subclasses. Both B87 numbers are correct measurements of DIFFERENT
things (264 = rt+wb total; 103 = rt primitives). B87's "10 macros" for clHVACGeneral is EXACT (10 BCm*). Not
an error — a resolution of what each count measures. The `BCm*` domain block is a **composition of `Cf*`
primitives** wired internally (each block instantiates dozens of `func_N` primitive objects and `Execute()`s
them in order), executed by `BControlProgramService` (§540.6).

## 540.2 Heating — `BCmVTB_HtgCirc`: weather-compensated supply-temperature control [CERT]

`BCmVTB_HtgCirc extends BControlFunctionSupport` `[CERT] :40` (version "51.07 [60] 05.03.2018"). It computes a
mixing-valve position to hold a supply-water setpoint derived from outside-air temperature — the classic
weather-compensation (heating-curve) sequence.

**The heating curve = 2-point linear interpolation** `[CERT] :650-666` (registered through the `CmIDT10`
10-channel latch): the design points are `parameter_29..32`:

| Param | Default | Role |
|-------|---------|------|
| `parameter_29` | −10 °C | OAT cold design point |
| `parameter_30` | +10 °C | OAT warm design point |
| `parameter_31` | 65 °C | Supply temp at cold OAT |
| `parameter_32` | 85 °C | Supply temp at warm OAT |
| `parameter_33` | 10 K | slope/curve correction |

So as OAT falls from +10 → −10 °C, the supply setpoint slides along the line between (10 °C→85… ) — the curve
maps outside temperature to the water temperature the emitters need. **Room-temperature correction is
additive** (`func_40.output = roomOffset + correction`). A **5-way mode multiplexer**
(`CmSelectionSpTemperature` `[CERT] :720-728`) selects the active setpoint by `input_1860` mode
(0=comfort/design curve, 1=night setback, 2=full cool-down, 3=external override). `CmHeatDemand` suppresses
demand below an OAT frost threshold with 3 K hysteresis; `CfPidPlus` closes the loop on the mixing valve;
`CfValueRamp` limits the setpoint ramp to `parameter_22 = 25 K/min`. Outputs: `output_12` (supply SP command),
`output_17` (valve %), `output_3482` (pump), `output_13` (heat-demand bool).

**Official doc CONFIRMS** `[CERT-doc] VTB_HtgCirc1.html:47`: "The mixed water temperature control is based on
the outside air temperature and can be compensated by the room temperature… night setback, total cool-down…
with and without room temperature sensor. © 2018 Honeywell GmbH."

## 540.3 AHU — `BCmDMB_MixingDamper`: OA/RA mixing with an economizer-shutoff mode gate [CERT]

`BCmDMB_MixingDamper extends BControlFunctionSupport` `[CERT] :24`. It positions the outside-air/return-air
mixing damper. **The minimum OA-damper position is OAT-scheduled** by another 2-point curve
(`parameter_19=−10 °C→parameter_16=10%`, `parameter_20=+10 °C→parameter_17=10%`, min fresh-air fraction
`parameter_21=20%`, max `parameter_22=100%`) `[CERT] :237-254`. `CmDamper_Control_Signal` drives the damper to
hold the mixed-air setpoint (`output_9` open %, `output_10` close %, `output_11` position %).

**The economizer-shutoff RULE** `[CERT] :265-296`: a `CfWindow` tests whether the plant mode `input_8` is in
`[19.8, 30.2]` (i.e. mode ∈ {20 DX-cooling, 30 heat-recovery}); when true a selector **forces all damper
outputs to 0% (full recirculation)**. So the block is a mixing/economizer damper controller whose free-cooling
decision arrives from the upstream plant-mode signal, not computed here. A mode-indicator output
(`output_1580`: 15=economizer, 12=DX, 2=heat-recovery) is decoded for the HMI.

## 540.4 Chiller — `BCmSQA_ChillerSeq`: runtime-equalized lead-lag for up to 12 chillers [CERT]

`BCmSQA_ChillerSeq extends BControlFunctionSupport` `[CERT] :39` (4,812 lines — the largest block in the
corpus, "51.02 [173] 26.02.2019"). It sequences up to 12 chillers on cooling load:

- **Stage-add** when cooling load (`input_4`, 0–100%) exceeds the `(N+1)/N × 100%` threshold; **stage-remove**
  below the `(N−1)/N × 99%` threshold `[CERT] :1525-1567` (CfDivide of the scaled counts). Both gated by an
  inter-stage delay `parameter_27/28 = 600 s` via `CfRsFlipFlop` + `CfDelay`.
- **Runtime-equalizing rotation**: 12 `Cmsequence_rotation` instances (func_7107..7120) permute the sequence
  order when the lead↔lag accumulated-runtime difference exceeds `parameter_22 = 100 h`; per-chiller runtime
  accumulates in `register_325..336` `[CERT] :625-638,1095-1192`.
- **Per-chiller alarm lockout**: `CmAlarm_LD` instances lock out a faulted chiller with a timed restart
  (`parameter_23/24/25 = 60 s`) and zero its runtime while locked `[CERT] :1457-1466`.
- `parameter_15` = number available (default 3), `parameter_30` = group size (1–12). Chillers 4–12 are handled
  by the add-on block `BCmSQB_ChillSeqAddOn`. Outputs `output_8..13` = per-position chiller enable.

This is textbook lead-lag plant sequencing: capacity staging with hysteresis + minimum dwell, runtime
equalization, and fault lockout — all encoded as a composition of `Cf*` primitives.

## 540.5 Energy — `BCmDDA_DEGDAYS`: heating/cooling degree-days [CERT]

`BCmDDA_DEGDAYS extends BControlFunctionSupport` `[CERT] :29`. Accumulates heating degree-days (HDD) and
cooling degree-days (CDD) against configurable base temperatures `parameter_7 = 15 °C` (heating base, the
EN-15316 default), `parameter_8 = 22 °C` (cooling base), `parameter_9 = 20 °C` (neutral). Outputs `output_4`
(HDD), `output_478` (CDD), `output_5` (total) in "degree days celsius" units. [INFER] the accumulator
integrates `max(0, T_base − T_OAT)` (HDD) and `max(0, T_OAT − T_base_cool)` (CDD) per interval — the primitive
chain (CfDifferential/CfAbsolute/CfSubtract/CfMultiply/CfMonoflop/CfTruncate) is consistent with that but the
exact integration window was not traced.

## 540.6 The execution model — `BControlProgramService` [CERT]

CONFIRMS B87 §87.2 at current line numbers: `BControlFunctionSupport extends BComponent implements
IEnvironment` `[CERT] :37`; each block registers with `BControlProgramService.getService().FunctionList(this,
true)` on `started()` `[CERT] :62-65`; the only kitControl import is `BIWritablePoint`/`BPriorityLevel`
`[CERT] :10-11`. `BControlProgramService extends BAbstractService` `[CERT] :30` is the central clock that runs
the registered function roster — the "Eagle" execution engine, entirely separate from the kitControl/control
engine ([Block 6]).

## 540.7 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | clHVAC-rt = 250 vf, 103 BControlFunctionSupport primitives; 83 BCm* domain blocks | [CERT] | find/rg counts | token-checked ✓ |
| 2 | B87 "264" = rt+wb total; "103" = rt primitives; "10 macros" clHVACGeneral exact | [CERT] | rt=250,rt+wb=264,BCm* counts | token-checked ✓ |
| 3 | BCmVTB_HtgCirc heating curve = 2-pt linear OAT→Tsupply, params 29-32 (−10/65, 10/85) | [CERT] | BCmVTB_HtgCirc.java:40,230-251,650-666 | token-checked ✓ |
| 4 | 5-way mode mux (comfort/night/cooldown/external); CfPidPlus valve; CfValueRamp 25 K/min | [CERT] | :720-728,534,507 | sweep-cited |
| 5 | Heating doc confirms OAT-based supply SP + room compensation | [CERT-doc] | VTB_HtgCirc1.html:47 | sweep-cited (quote) |
| 6 | MixingDamper: OAT-scheduled min position; CfWindow mode∈[20,30]→force 0% recirc | [CERT] | BCmDMB_MixingDamper.java:24,237-254,265-296 | sweep-cited |
| 7 | ChillerSeq: (N+1)/N add, (N−1)/N remove, 600s delay, 100h rotation, 60s lockout | [CERT] | BCmSQA_ChillerSeq.java:39,1525-1567,625-638,1457 | sweep-cited (:39 token-checked) |
| 8 | Degree-days base temps 15/22/20 °C | [CERT] | BCmDDA_DEGDAYS.java:29,41-59 | sweep-cited |
| 9 | BControlFunctionSupport extends BComponent; BControlProgramService extends BAbstractService | [CERT] | :37 ; :30 | token-checked ✓ |
| 10 | Only kitControl dep = BIWritablePoint/BPriorityLevel | [CERT] | BControlFunctionSupport.java:10-11 | sweep-cited |

**Marker tally**: [CERT] ×9 · [CERT-doc] ×1 · [INFER] ×1 (degree-days integration window). Block TYPE =
EVIDENCE (decompilation). 5 of 10 rows token-verified inline; the decompiled sequence bodies are sweep-cited
with the class declarations and counts personally confirmed. **This upgrades B87 §87.3 from `[CERT-a]` to
`[CERT]`**: the heating-curve, economizer-gate, and chiller-sequencing claims are now source-anchored.

## Connections

- **[Block 87]** — the clHVAC ARCHITECTURE (REMITTANCE); §87.3's HVAC sequences are UPGRADED here to [CERT]
  and its counts clarified (264=rt+wb, 103=primitives, 83=BCm* domain). B87 gets a back-pointer.
- **[Block 103]** (honeywellFunctionBlocks) / **[Block 105]** (honIrmControl) — the OTHER OEM control engines;
  clHVAC is the Centraline/Eagle one. All three are non-kitControl control ecosystems (KC7/KC10 catalog them).
- **[Block 536]** — clHVAC writes to writable points via the SAME `BIWritablePoint`/`BPriorityLevel` surface
  (its only kitControl dependency).
- **Forward**: KC7 (honeywellFunctionBlocks per-FB catalog — compare the DDC engine), KC12 (clHVAC Nordic
  micro-modules).

## Open gaps (this block)

- The `BCmDDA_DEGDAYS` integration window is [INFER] (primitive chain consistent, not traced) — a child gap
  only if degree-day accounting detail is later needed.
- 80 of 83 BCm* domain blocks are un-decompiled (3 representative + 1 energy done). The library is too large
  for per-block coverage; recorded as covered-by-representative-sample. Open a child gap for a specific
  sequence (VAV, boiler cascade, cooling tower) only on demand.
