# Block 537 — The kitControl function-block catalog: 151 classes → ~130 deployable blocks across 10 packages, with the PID loop, the latch/switch/select mux family, and the multi-input null contract decompiled

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC2 — the native function-block catalog, enumerated block-by-block)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY. Delegated sonnet sweep over code + official doc, cross-referenced; counts and load-bearing
blocks token-verified inline against source.
**Primary sources**:
- CODE: `organized/kitControl/kitControl-rt/vineflower/com/tridium/kitControl/` — the block classes.
- OFFICIAL DOC: `organized/docKitControl/docKitControl-doc/extracted/doc/` — 163 HTML reference pages
  (116 per-block `kitControl-*.html` + concept/example pages). This is the `[CERT-doc]` source for behavior.

**Scope**: the definitive category-by-category enumeration of the native kitControl library — the blocks an
engineer links together to build control logic. It is the code-level completion of the by-category list in
**[Block 6] §6.3.3** (REMITTANCE: B6 named ~100 blocks; this block MEASURES the full set, maps it to code
packages, and decompiles the load-bearing control primitives B6 only named). The writable-point model those
blocks WRITE INTO is [Block 536] (KC1).

---

## 537.1 Relationship to [Block 6] §6.3.3 — CONFIRMED + refined

B6 §6.3.3 gave a palette-oriented by-category list and named ~100 blocks including 7 HVAC/energy specials.
**All 7 named HVAC/energy blocks EXIST as classes — none were hallucinated** (§537.6). B6's "100+ blocks" is
CONFIRMED (measured ~130 deployable). Two refinements this block adds:
- **Package ≠ palette.** B6 grouped by the Workbench palette; the CODE organizes differently. Notably
  `BLoopPoint` (PID) lives in the ROOT package `com.tridium.kitControl`, NOT `hvac`, even though the palette
  files it under HVAC. `BRaiseLower`, `BSequence*`, `BTstat`, `BLeadLag*` are the real `hvac` package.
- **The "control" category B6 listed is not a code package** — those blocks are split across `hvac` (staging,
  raise/lower, thermostat), `util` (latches, mux), and the root (`BLoopPoint`).

## 537.2 Measured inventory [CERT]

`find kitControl-rt/vineflower/com/tridium/kitControl -name 'B*.java' | wc -l` = **151 classes** [CERT].
Per-package counts (token-verified):

| Package | B-classes | What it holds |
|---------|-----------|---------------|
| `math` | 26 | arithmetic + trig + math bases |
| `util` | 37 | latches, mux/switch/select, string ops, demux, waveform generators |
| `conversion` | 20 | type/unit converters |
| `enums` | 16 | **enum TYPES, not blocks** (BLoopAction, BDisableAction, …) |
| `logic` | 13 | boolean logic + comparisons |
| `energy` | 10 | demand/optimization/psychrometric |
| `hvac` | 7 | staging, lead-lag, raise/lower, thermostat |
| `timer` | 5 | delays, one-shot, current-time, time-diff |
| `constants` | 4 | numeric/boolean/enum/string constant sources |
| `<root>` | 13 | `BLoopPoint` + 4 alarm algorithms + 2 interstart + `BAlarmCountToRelay` + 5 base classes |

**Deployable-block reconciliation**: 151 − 16 enums − ~5 abstract base classes (`BKitNumeric`,
`BKitNumericPoint`, `BKitBooleanPoint`, `BKitEnumPoint`, `BExtensionName`) ≈ **130 deployable function
blocks**. The official doc ships **116 per-block reference pages**; the ~14 delta is the alarm algorithms +
interstart helpers + `BAlarmCountToRelay`, which have no standalone palette page. [CERT-doc]
`docKitControl/.../` page count 163 (116 block + concept pages).

## 537.3 The category catalog (by code package)

- **math (26)** [CERT-doc `MathComponents-2BC86F7A.html`]: `BAdd BSubtract BMultiply BDivide BAbsValue
  BAverage BMaximum BMinimum BModulus BNegative BPower BSquareRoot BFactorial BExponential BLogNatural
  BLogBase10 BSine BCosine BTangent BArcSine BArcCosine BArcTangent` + bases `BMath BUnaryMath BBinaryMath
  BQuadMath`.
- **logic (13)** [CERT-doc `LogicComponents-28252981.html`]: `BAnd BOr BNot BXor` + comparisons `BEqual
  BNotEqual BGreaterThan BGreaterThanEqual BLessThan BLessThanEqual` + bases `BLogic BComparison BQuadLogic`.
- **conversion (20)** [CERT-doc `ConversionComponents-268B4DEA.html`]: the `BStatus*To*` matrix +
  `BNumericUnitConverter` (via BUnit catalog) + `BStatusValueToValue`.
- **util (37)** [CERT-doc `UtilComponents-28306EF1.html`]: latches `BBooleanLatch BNumericLatch BEnumLatch
  BStringLatch` (base `BLatch`); mux `BNumericSwitch BBooleanSwitch BEnumSwitch` + `BNumericSelect
  BBooleanSelect BEnumSelect BStringSelect` (bases `BSwitch BMuxSwitch`); `BCounter BReset BMinMaxAvg`;
  bitwise `BNumericBitAnd/Or/Xor BNumericMask BNumericToBitsDemux BStatusDemux BDigitalInputDemux`;
  generators `BRamp BRampWaveform BSineWave BMultiVibrator BRandom`; string `BStringConcat BStringLen
  BStringSubstring BStringIndexOf BStringTrim BStringTest`; `BDecaInputNumeric BBqlExprComponent`.
- **timer (5)** [CERT-doc `TimerComponents-282F0D26.html`]: `BBooleanDelay BNumericDelay BOneShot
  BCurrentTime BTimeDifference`.
- **constants (4)** [CERT-doc `ConstantComponents-268ADC12.html`]: `BNumericConst BBooleanConst BEnumConst
  BStringConst`.
- **energy (10)** [CERT-doc `EnergyComponents-26B48B3B.html`]: `BOptimizedStartStop BNightPurge
  BOutsideAirOptimization BPsychrometric BElectricalDemandLimit BSlidingWindowDemandCalc BShedControl
  BSetpointLoadShed BSetpointOffset BDegreeDays`.
- **hvac (7)** [CERT-doc `HVACComponents-26B4F895.html`]: `BLeadLagCycles BLeadLagRuntime BRaiseLower
  BSequence BSequenceBinary BSequenceLinear BTstat`.
- **root**: `BLoopPoint` (PID, §537.4) + alarm algorithms `BLoopAlarmAlgorithm
  BChangeOfStateCountAlarmAlgorithm BDiscreteTotalizerAlarmAlgorithm BElapsedActiveTimeAlarmAlgorithm` +
  `BInterstartDelayControl BInterstartDelayMaster BAlarmCountToRelay`.
- **enums (16)** — NOT deployable blocks; the enum types the blocks reference (`BLoopAction` direct/reverse,
  `BDisableAction`, `BNightPurgeMode`, `BOutsideAirOptimizationMode`, `BRaiseLowerFunction`, …).

## 537.4 BLoopPoint — the PID control primitive [CERT + CERT-doc]

`com/tridium/kitControl/BLoopPoint.java` (root package) extends `BNumericPoint`. [CERT-doc
`ProportionalWithIntegralAndDerivati-24A75A7A.html`, `PIDLoopConfiguration-24A2E8F0.html`]

**Inputs**: `controlledVariable` (PV, :112), `setpoint` (SP, :113), plus `loopEnable`. **Config**: `loopAction`
(`direct`|`reverse`, default `direct`, :116), `disableAction` (`maxValue`|`minValue`|`hold`|`zero`, default
`zero`, :117), `proportionalConstant` kP (:119), `integralConstant` kI (:120), `derivativeConstant` kD (:121),
`bias` (:122), `maximumOutput` (default 100, :123), `minimumOutput`, `executeTime`, `rampTime`. **Output**:
`out` (clamped to [min,max]).

**Algorithm** [CERT] `BLoopPoint.java:386-401`:
```java
double error = getSetpoint().getValue() - getControlledVariable().getValue();   // :386
double iError = deltaSecs * error; errorSum += iError;                          // :388
if (-errorSum > maxOutput / kPkIconst) { /* anti-windup clamp */ }              // :392
```
The terms are `P = error·kP`, `I = kP·kI·errorSum/60`, `D = kP·kD·Δerror/Δt`; `direct` action negates the sum;
`bias` is added only when P-only (kI=0). RULES [CERT]:
- **Anti-windup**: `errorSum` is clamped to `±(maxOutput / kPkIconst)` BEFORE the output limit (:392) — the
  integral cannot wind past what the output range can express.
- **`hold` disable is implicit**: `handleLoopDisabled()` explicitly handles maxValue/minValue/zero; `hold`
  falls through to `outValue=NaN`, and the NaN guard skips the output update — a de-facto hold.
- **Bumpless kP retune**: changing `proportionalConstant` at runtime rescales `errorSum` by kP_old/kP_new.
- **`resetIntegral` action** clears `errorSum` for a bumpless manual→auto transfer.
- NaN/Inf on PV or SP → output gets fault status, algorithm skipped.

## 537.5 The mux/latch family and the multi-input NULL contract [CERT]

**Latches** (`util/BLatch` base + typed `BBooleanLatch`/`BNumericLatch`/`BEnumLatch`/`BStringLatch`):
sample-and-hold. Two trigger routes [CERT-doc `kitControl-BooleanLatch.html`]: the `clock` slot latches
`in`→`out` on a **rising edge only** (`currentClock && !lastClock`), while the `latch` **action** slot fires
on **any** invocation (both edges). `out` holds its last latched value until the next trigger — it does NOT
track `in`.

**Switches** (2-way mux, `BNumericSwitch`/`BBooleanSwitch`/`BEnumSwitch`): `inSwitch` (boolean) routes
`inTrue` or `inFalse` to `out`. If `inSwitch` status is invalid → `out` **holds last value but is flagged
invalid** [CERT] `BBooleanSwitch.java:72-90`.

**Selects** (N-way mux, `BNumericSelect` + siblings, bases `BSwitch`/`BMuxSwitch`): a `select` enum ordinal
routes one of up to 10 inputs `inA..inJ`; `numberValues` (3–10) sets visible slots; `zeroBasedSelect` toggles
0- vs 1-based indexing [CERT] `BSwitch.java:125-142`. Invalid `select` → hold-with-invalid-flag.

**The multi-input NULL contract** (the rule B6 §6.3.3 asserted, now CODE-CONFIRMED) — `math/BQuadMath.java`
and `logic/BQuadLogic.java` share it [CERT] `BQuadMath.java:95-98`:
- `inA..inD` default to `new BStatusNumeric(0, BStatus.nullStatus)` — relinquished until linked.
- Each execute counts `nonNullCount` over inputs where `status.isValid()`.
- If `nonNullCount < minInputs()` (`BAdd.minInputs()=1`, `BQuadMath.java`) → `forceNull`, output = NaN +
  `nullStatus`.
- Otherwise `calculate()` processes **only the valid inputs** (nulls skipped as absent, NOT as zero/false),
  and the output status is the OR of the input status bits.

So `BAdd` with only `inA` and `inC` linked returns `inA+inC` (not `inA+0+inC+0`); with nothing linked it
outputs null. `BAnd` adds `nullOnInactive`: when true and the AND result is false, the output is forced to
null-status (the signal "disappears" when inactive) [CERT] `BQuadLogic.java` + `BAnd.java:47`. This is the
mechanism behind conditional kitControl chains.

## 537.6 HVAC/energy block verification — B6 §6.3.3 fully CONFIRMED [CERT]

Every HVAC/energy block B6 named exists as a class (file path confirmed; no hallucination):

| Block | Package | Key I/O (from source) |
|-------|---------|-----------------------|
| `BOptimizedStartStop` | energy | in: heatCoolMode, schedule/nextEvent, outsideTemp, spaceTemp → out: start/stopTimeCommand |
| `BNightPurge` | energy | in: outside/inside temp+humidity+enthalpy, nightSetpoint → out: freeCooling, currentMode |
| `BOutsideAirOptimization` | energy | in: outside/inside temp+humidity+enthalpy → out: freeCooling (economizer) |
| `BPsychrometric` | energy | in: unitSelect, temp, humidity → out: dewPoint, enthalpy |
| `BElectricalDemandLimit` | energy | in: powerInput; config: demandInterval, limits 1/2/3 → out: shedOut |
| `BSlidingWindowDemandCalc` | energy | in: currentPulseCount, kwhPerPulse → out: demand5/15/30, kwh |
| `BLeadLagCycles` | hvac | in: enable, feedback; config: numberOutputs, maxRuntime → out: outA..outE |

## 537.7 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | 151 B*.java total; per-package counts (math26/util37/conv20/logic13/energy10/hvac7/timer5/const4/enums16/root13) | [CERT] | find + per-pkg wc | token-checked ✓ |
| 2 | ~130 deployable (−16 enums −5 bases); 116 doc block pages | [CERT]/[CERT-doc] | inventory + doc ls | ✓ |
| 3 | BLoopPoint is in ROOT package, not hvac | [CERT] | kitControl/BLoopPoint.java path | token-checked ✓ |
| 4 | PID props: loopAction=direct, disableAction=zero, kP/kI/kD, maxOutput=100 | [CERT] | BLoopPoint.java:116-123 | token-checked ✓ |
| 5 | PID: error=SP−PV, errorSum integral, anti-windup clamp maxOutput/kPkIconst | [CERT] | BLoopPoint.java:386-392 | token-checked ✓ |
| 6 | Multi-input null: nonNullCount over isValid inputs; <minInputs→null; nulls skipped | [CERT] | BQuadMath.java:95-98; BAdd minInputs=1 | token-checked ✓ |
| 7 | Latch: clock=rising-edge, latch action=both edges; sample-and-hold | [CERT-doc]/[CERT] | kitControl-BooleanLatch.html + BLatch | ✓ |
| 8 | Switch/Select invalid selector → hold-with-invalid-flag | [CERT] | BBooleanSwitch.java:72-90; BSwitch.java:125-142 | ✓ (sweep-cited) |
| 9 | All 7 B6 §6.3.3 HVAC/energy blocks exist (no hallucination) | [CERT] | energy/hvac package files | ✓ |
| 10 | enums pkg (16) are types, not deployable blocks | [CERT] | enums/ listing | ✓ |

**Marker tally**: [CERT] ×8 · [CERT-doc] ×2 (several category rows carry [CERT-doc] page cites) · [INFER] ×0.
Block TYPE = EVIDENCE/CATALOG. [INFER]/[CERT] ratio = 0 — fully anchored; NOT an exhaustion signal (10 gaps
remain). Tokens checked inline: 6 load-bearing groups (total count, package counts, BLoopPoint location+props,
PID body, BQuadMath null gate) resolved against source.

## Connections

- **[Block 536]** (KC1) — the writable-point arbitration these blocks feed via links.
- **[Block 6]** §6.3.3 — the by-category list this block MEASURES and CONFIRMS (all HVAC/energy blocks exist);
  refines the package-vs-palette grouping.
- **Forward**: KC4 (BLoopPoint deep — tuning/ramp/windup beyond this summary), KC3 (the linking RULES that wire
  these blocks together), KC8 (the block-write → priority-array path), KC5 (clHVAC — the OEM app libraries
  built on the same pattern).

## Open gaps (this block)

- KC4 will deepen `BLoopPoint` (this block covers its structure + core algorithm; tuning guidance, ramp
  detail, and the loop alarm algorithm remain).
- The 20 conversion blocks and the string/bitwise util blocks are enumerated but not individually
  decompiled — recorded as covered-by-enumeration; open a child gap only if a specific converter's semantics
  are later needed.
