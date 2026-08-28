# Block 542 — honeywellFunctionBlocks DDC catalog: 36 concrete FBs across 8 fbs/ packages, and the SCAN execution model (Sequenced Control Engine) that separates it from kitControl

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC7 — the honeywellFunctionBlocks per-FB catalog)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY. Delegated sonnet sweep over code + official doc; counts, base class, and the scan
execution model token-verified inline.
**Primary sources** `[CERT]`:
- `organized/honeywellFunctionBlocks/honeywellFunctionBlocks-rt/vineflower/com/honeywell/honfunctionblocks/`
  — `fbs/BFunctionBlock.java`, `fbs/BExecutionParams.java`, `fbs/control/BPid.java`, `.../BStager.java`,
  `.../BStageDriver.java`, `fbs/zonecontrol/BTemperatureSetpointCalculator.java`.
- DOC `[CERT-doc]`: `docHoneywellFunctionBlocks/.../doc/honeywellFunctionBlocks-Pid.html` (50 HTML, 47 FB pages).

**Scope**: the per-block catalog of Honeywell's DDC Function-Block library — the Spyder/IPC control primitives
that run IN the N4 station. [Block 103] established the ENGINE (`BFunctionBlock`, converters, Negatable
datatypes); KC7 ENUMERATES the blocks, decompiles the load-bearing control primitives, and pins down the
EXECUTION MODEL (which B103 did not detail). B103's engine is REMITTANCE.

---

## 542.1 Inventory + the count clarified (resolves B103) [CERT]

`honeywellFunctionBlocks-rt` = **146 classes** in `com.honeywell.honfunctionblocks`; ux=7, wb=5 → **158 total**
`[CERT]`. So B103's "158 java" counted rt+ux+wb; the FB ENGINE (`-rt`) is 146. Of those, **36 concrete
function blocks extend `BFunctionBlock`** `[CERT]` (the rest are 32 converters, datatypes, enums, and helpers).
Not a B103 error — a scope clarification (same pattern as clHVAC 264=rt+wb vs 250=rt in [Block 540]).

fbs/ sub-package breakdown (90 files in fbs/):

| fbs sub-package | files | concrete FBs |
|-----------------|-------|--------------|
| control | 17 | BPid, BAia, BCycler, BFlowControl, BRateLimit, BStager, BStageDriver |
| zonecontrol | 18 | BTemperatureSetpointCalculator, BGeneralSetpointCalculator, BOccupancyArbitrator, BSetTemperatureMode |
| math | 18 | BAdd BSubtract BMultiply BDivide BLimit BRatio BSquareRoot BLogarithm BExponential BEnthalpy BFlowVelocity BDigitalFilter BReset |
| analog | 15 | BCompare BSelect BSwitch BMaximum BMinimum BPrioritySelect BAverage BAnalogLatch BEdge BEncode BHystereticRelay BDecisionBox BMinMaxAverageBlock |
| logic | 5 | BAnd BOr BXor BOneShot (base BLogicBlock) |
| datafunction | 3 | BCounter BOverride BRuntimeAccumulate |
| builtin | 1 | BTuncos (Time Until Next Change Of State) |
| io | 4 | (enums only — no FB classes) |

The doc ships 47 FB reference pages; a few doc-only names (PassThru→`utils/BPassThru`, SystemTime, TextBlock)
have no `fbs/` class here. B103's "~158 blocks" conflated the module class count with the Spyder FIRMWARE
catalog — the N4 Java module has ~36 concrete FBs.

## 542.2 The SCAN execution model — the key contrast with kitControl [CERT]

`BFunctionBlock extends BComponent implements IHoneywellExecutionBlock, IHoneywellComponent` `[CERT]
BFunctionBlock.java:77`. Unlike kitControl (event-driven `onExecute` on change, [Block 6] §6.1), Honeywell FBs
run on a **periodic SCAN**:
- Each FB carries an `ExecutionOrder` int property `[CERT] :78` and an `executeBlock(BExecutionParams)` action
  `[CERT] :81`.
- `BExecutionParams extends BStruct` carries `iterationInterval` (ms, default 1000, range 100–65535) `[CERT]
  BExecutionParams.java:32-33` — the SCAN PERIOD, passed into every execute call and used for time-domain math
  (integral accumulation, timer advance).
- A separate **Sequenced Control Engine** component invokes `executeBlock` on each FB in `ExecutionOrder`
  sequence, once per scan tick `[CERT-doc] honeywellFunctionBlocks-Pid.html` ("Sequenced Control engine
  stop/start"). Changing a tuning parameter at runtime requires stop/start of the engine to reinitialize block
  state (integrator reset) — a scan-firmware idiom, not event semantics.
- Override is FB-level: `doExecuteBlock` skips a block whose OUTPUT slots are `isOverridden()` — coarser than
  kitControl's per-level priority array ([Block 536]).

**Architectural verdict**: honeywellFunctionBlocks reproduces the **Spyder/Excel DDC scan loop** inside the N4
station — a fixed-interval, ordered scan — whereas kitControl and clHVAC are event-driven/roster-driven. This
is the THIRD control ecosystem in N4 (kitControl [B6], clHVAC/Eagle [B87/B540], honeywell DDC [B103/here]),
and the only one with an explicit scan period + execution order.

## 542.3 Deep-dive: the DDC control primitives [CERT]

**BPid** `[CERT] BPid.java:222` — DDC-convention PID. Inputs `sensor`, `setPt`, `tr` (PROPORTIONAL BAND, not
gain — `Kp = 100/tr`), `intgTime` (s, 0=off), `dervTime` (s), `deadBand` + `dbDelay` (dead-band with a delay
timer), `disable` (Negatable). Config `revAct` (direct/reverse), `bias` (0–100% offset), `outputRange`
(0–100% or ±200%). Output `OUTPUT` %. Formula `[CERT-doc] Pid.html`: `Output% = Bias + Kp·Err + Kp/Ti +
Kp·Td·dErr/dt`, `Err = Sensor − SetPt`. Integrator anti-windup clamped to `[0−bias, 100−bias]`; dead-band
holds last output when `|err| < deadBand` past `dbDelay`.

**Contrast with kitControl BLoopPoint** [Block 539]: BLoopPoint uses proportional/integral/derivative CONSTANTS
(gain-based, kI in repeats/min), a `throttlingRange`-derived kP, and runs on the event engine. BPid uses the
DDC **proportional-BAND** convention (`Kp=100/tr`), a built-in dead-band timer, a `revAct` flag, and a `bias`
offset — the Spyder/Excel controller parameter set — and runs on the scan. Same PID math, different parameter
culture and execution model.

**BStager** `[CERT] BStager.java` — converts a % demand (typically a PID OUTPUT) into an integer STAGE COUNT
(0–`maxStgs`, up to 255). Stage-up requires `demand > stagesActive·(100/maxStgs)` AND `offTimer > minOff` AND
`onTimer > intstgOn`; stage-down requires crossing below `(stagesActive−1)·(100/maxStgs) − hyst` AND
`onTimer > minOn` AND `offTimer > intstgOff`. So it encodes hysteresis + min-on/min-off + inter-stage delays —
the equipment-protection timing.

**BStageDriver** `[CERT] BStageDriver.java` — maps the integer stage count to N discrete boolean outputs with
lead-lag rotation (`BLeadLagStrategyEnum`: Fixed/Rotation/Runtime) and runtime accumulation for equalization.
The BStager→BStageDriver pair is Honeywell's staging analog of clHVAC's `BCmSQA_ChillerSeq` ([Block 540]).

**BTemperatureSetpointCalculator** `[CERT] BTemperatureSetpointCalculator.java` — zone effective-setpoint calc.
Selects occupied/standby/unoccupied heat & cool setpoints (defaults 70/67/55 heat, 75/78/85 cool) by occupancy
state, applies a user Setpoint as offset (<10) or center (≥10), and runs a **recovery ramp** before a scheduled
occupancy transition: `Δsetpoint = (ScheduleTUNCOS − 10)·rampRate/60` (the 10-min "mesa" via `BTuncos`).
Outputs `EFF_HEAT_SETPT`/`EFF_COOL_SETPT`. It is the setpoint side only; airflow/VAV would chain
`→ BPid → BStager → BStageDriver` [INFER].

## 542.4 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | rt=146 classes (158=rt+ux+wb), 36 concrete FBs extend BFunctionBlock | [CERT] | find/rg counts | token-checked ✓ |
| 2 | fbs/ = 90 files across 8 sub-packages (control/zonecontrol/math/analog/logic/datafunction/builtin/io) | [CERT] | fbs/ listing | sweep-cited |
| 3 | BFunctionBlock extends BComponent; ExecutionOrder + executeBlock(BExecutionParams) action | [CERT] | BFunctionBlock.java:77-81 | token-checked ✓ |
| 4 | Scan model: BExecutionParams.iterationInterval (ms, default 1000) = scan period | [CERT] | BExecutionParams.java:32-33 | token-checked ✓ |
| 5 | Sequenced Control Engine drives FBs in ExecutionOrder; stop/start to reinit | [CERT-doc] | Pid.html | sweep-cited |
| 6 | BPid: Kp=100/tr (proportional band), deadBand+dbDelay timer, revAct, bias, ±200%/0-100% | [CERT]/[CERT-doc] | BPid.java:222,247-280; Pid.html | token-checked (class+props) ✓ |
| 7 | BStager: %demand→stage count, min-on/off + interstage + hysteresis | [CERT] | BStager.java | sweep-cited |
| 8 | BStageDriver: stage→N booleans w/ lead-lag rotation | [CERT] | BStageDriver.java | sweep-cited |
| 9 | BTemperatureSetpointCalculator: occ/standby/unocc setpoints + TUNCOS recovery ramp | [CERT] | BTemperatureSetpointCalculator.java | sweep-cited |
| 10 | FB-level override (skip if outputs overridden), vs kitControl slot priority | [CERT] | BFunctionBlock doExecuteBlock | sweep-cited |

**Marker tally**: [CERT] ×8 · [CERT-doc] ×2 · [INFER] ×1 (VAV chaining). Block TYPE = EVIDENCE/CATALOG. 5 of 10
rows token-verified inline (counts, base class, scan params, BPid class/props).

## Connections

- **[Block 103]** — the honeywellFunctionBlocks ENGINE (REMITTANCE); clarifies the "158" count (rt+ux+wb) and
  adds the per-FB catalog + the scan execution model B103 did not detail.
- **[Block 539]** (BLoopPoint) — the kitControl PID this block contrasts (gain vs proportional-band; event vs
  scan).
- **[Block 540]** (clHVAC BCmSQA_ChillerSeq) — the Eagle staging analog to BStager/BStageDriver.
- **[Block 6]** — the event-driven engine that honeywell DDC does NOT use (it scans).
- **Forward**: KC10 (honIrmControl per-FB catalog — the third OEM control library), KC13 (safety — BPid
  disable=output 0 is a fail-safe example).

## Open gaps (this block)

- 30 of 36 FBs enumerated but not individually decompiled (6 load-bearing done) — covered-by-enumeration; open
  a child gap for a specific block (BAia adaptive control, BEnthalpy) only on demand.
- The Sequenced Control Engine component itself (the scan driver) is named, not decompiled — a child gap if
  the scan scheduler internals are later needed.
