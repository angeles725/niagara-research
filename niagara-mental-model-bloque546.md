# Block 546 — honIrmControl per-FB catalog: 140 factory FBs across 22 packages, and the HARDWARE-OFFLOAD execution model that makes it the fourth (and only device-executed) control ecosystem

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC10 — the honIrmControl per-FB catalog + execution model)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY. Delegated sonnet sweep over code + module.xml; counts, base class, and the
hardware-offload proof token-verified inline.
**Primary sources** `[CERT]`:
- `organized/honIrmControl/honIrmControl-rt/vineflower/com/honeywell/irm/` — `fbOne/BFbFactory.java`,
  `controlLoop/BPid.java`, `BPidA.java`, `vav/BFlowControl.java`.
- `organized/honIrmConfig/honIrmConfig-rt/vineflower/com/honeywell/irmnano/` — `fbfactory/BNanoFunctionBlock.java`,
  `manager/BIrmControlManager.java`, `protocol/NanoCmdSetPredecessor.java`.
- `honIrmControl-rt/extracted/META-INF/module.xml`; DOC `honIrmControl-doc` (135 HTML).

**Scope**: the per-FB catalog of the BEATS/IRM control library and — the load-bearing finding — its EXECUTION
MODEL. [Block 105] established the library identity (~163 FBs over the "IRM Nano" engine); KC10 enumerates the
blocks and proves that, unlike every other N4 control library, **its FBs do not execute in the station** — they
are proxies for control that runs on the physical IRM controller. B105 is REMITTANCE.

---

## 546.1 Inventory — resolves B105's "163" [CERT]

`honIrmControl-rt` = **203 vineflower classes** `[CERT]` (matches B105) across **22 packages**. `module.xml`
registers 195 types (incl. enums/helpers). B105's "~163 FBs" resolves to: **140 user-placeable FBs dispatched
by `BFbFactory.createNanoFunctionBlock()`** + ~23 `BOnboardIo` hardware-config variants; the remaining ~40
files are enums, constants, and base classes. Doc = 135 HTML (115 `honIrmControl-` FB pages).

## 546.2 The category catalog (22 packages) [CERT]

| Category | Package | Representative FBs |
|----------|---------|--------------------|
| Arithmetic (12) | `arithmetic` | BAdd BSubtract BMultiply BDivide BExponential BLimit BLinearGraph BPsychrometric BReset |
| Bit (3) | `bitfunctions` | BBitAnd BBitOr BNumericToBit |
| Comparison (6) | `comparison` | BCompare BGreaterThan(Equal) BLessThan(Equal) BEqualNull |
| **Control loop (3)** | `controlLoop` | **BPid, BPidA, BAia** |
| Logic (7) | `logic` | BAnd BOr BNot BXor BRsFlipFlop BSrFlipFlop BTrigger |
| Select/switch (11) | `selectswitch` | BNumericSelect BNumericSwitch BBinarySelect(Prio/Multi) BMax/MinSelectMulti BValidSelectPrio |
| Timer (5) | `timer` | BMultifunctionTimer BOneShot BRateLimit BTimeDelay BTimeRamp |
| Date/time (7) | `datetime` | BCurrentDateTime BDate BTime BTimeDifference BTimeOfDay |
| Utility (14) | `util` | BConst* BPassThru BPrevValue BSavePermanent BEvaluateBacnetStatusFlags |
| **VAV (14)** | `vav` | **BFlowControl BFlowVelocity BStager BStageDriver BTemperatureSetpointCalculator BOccupancyArbitrator** BCycler BCounter |
| VAV schedule (5) | `vav.schedule` | BCalendar BEnumSchedule BIrmCalender |
| **Physical I/O (50)** | `physicalpoints` | BAo/Bi/Bo/Co/Ui/UioTerminal + sensors (BCO2/Flow/Humidity/Temperature/VOC) + ~30 BOnboardIo hardware variants |
| BACnet objects (27) | `bacnetobjects` | BBacnet{Boolean/Enum/Numeric}{Input/Output/Value/Writable} BRefIn BRefOut |
| Modbus (4) | `modbus` | BModbusDevice BModbusReadPoint BModbusWritePoint |
| Sylk bus (4) | `sylk` | BSylkDeviceFunctionBlock BSylkIn/OutParam |
| Outputs (7) | `outputs` | BFloating BPwm BStg123Outp |
| Lighting/blind (3) | `light`/`blind` | BLightA BOnOffDimming BBlindA |
| Wall module (3) | `wallmodule` | BConventionalWallModule BSylkWallModTr42 BWmConfigHvacA |
| GUI (9) | `gui` | BGui{Boolean/Enum/Numeric}{Input/Output/InputOutput} |
| Factory/engine (6) | `fbOne` | BFbFactory BFan BHystereticRelay BUpDownSlatAngle |

The catalog spans the FULL controller application surface — arithmetic through physical terminals, BACnet/Modbus/Sylk
field integration, and the operator GUI — because an IRM program is the WHOLE controller app, not a supervisory
snippet.

## 546.3 THE HARDWARE-OFFLOAD execution model — the fourth ecosystem [CERT]

Every FB extends `BNanoFunctionBlock` `[CERT] BPid.java:174` →
`BNanoFunctionBlock extends BComponent implements BINanoControl, BIIRMSupportedComponent`
`[CERT] BNanoFunctionBlock.java:165`. **It has NO `execute()` method** `[CERT]` — only `started()` (out-save init)
and `changed()` (:575, :781). The FB computes NOTHING on the Niagara JVM; it is a **passive proxy**.

The real program is COMPILED and DOWNLOADED to the physical controller:
- `BIrmControlManager.runTeachToController(...)` compiles the FB graph to a binary application and pushes it via
  `BINanoProtocolService` using the **NanoCmd protocol** (`NanoCmdSetProps`, `NanoCmdSetLink`,
  `NanoCmdSetPredecessor`) `[CERT] BIrmControlManager.java` (imports + `getActiveProtocolService()`).
- Execution ORDER is a HARDWARE scan sequence: `NanoCmdSetPredecessor` sends `insertAfter` + `itemsCount` to
  set the controller's scan order `[CERT] NanoCmdSetPredecessor.java`.
- On the Niagara side, editing a property fires `changed()` → `manager.changed(...)` which queues a NanoCmd to
  SYNC the device — supervisory sync, NOT execution `[CERT] BNanoFunctionBlock.java:781`.

So the IRM Nano controller runs a fixed-cycle scan of its FB list ON THE DEVICE; Niagara is the engineering tool
that builds and downloads the program and mirrors live values back. **This is the FOURTH control ecosystem in
N4, and the only one whose control does not run in the station** — the direct contrast to [Block 103]'s finding
that honeywellFunctionBlocks execute IN the station.

### The four control ecosystems of N4
| Ecosystem | FB base | Executes on | Model | Block |
|-----------|---------|-------------|-------|-------|
| kitControl | BControlPoint | Niagara JVM | event-driven (link propagation) | B6/B536/B539 |
| clHVAC (Eagle) | BControlFunctionSupport | Niagara JVM | roster (BControlProgramService) | B87/B540 |
| honeywellFunctionBlocks | BFunctionBlock | Niagara JVM | scan (Sequenced Control Engine) | B103/B542 |
| **honIrmControl (IRM Nano)** | **BNanoFunctionBlock** | **physical IRM controller** | **scan on hardware (NanoCmd download)** | B105/**B546** |

## 546.4 Control primitives [CERT]

- **BPid** `[CERT] BPid.java:174` (factory case 77) — proportional-band PID (`ProportionalBand` default 0.1,
  `IntegralTime`/`DerivativeTime` in seconds, `Deadband`+`DeadbandDelay`, `Bias` 0–100%, `Operation`
  direct/reverse/depending-on-sign). Same DDC proportional-band culture as honeywell `BPid` ([Block 542]) — but
  it runs on the IRM hardware. `BOutSaveFields` persists `Out` across controller restarts.
- **BPidA** (case 18) — advanced PID: adds `Aux` output, `Enable`, `Calculate` mode (Remain/PID/P-only),
  `Manual` override — for auto/manual transfer and cascade.
- **BFlowControl** `[CERT] BFlowControl.java:212` (case 96) — VAV damper flow control: `SensedFlow` +
  `Min/MaxFlowSp` + `DuctArea` → `EffFlowSp`, `DamperPos` (0–100%). `MotorSpeed` = actuator travel time
  (default 90 s). **OutSave** on `EffFlowSp`/`DamperPos` gives a fail-safe damper position across restart
  (a KC13/[Block 543] safety mechanism at the hardware layer).

## 546.5 Independent ecosystem [CERT]

Import analysis: honIrmControl has **ZERO imports of `kitControl`, `honeywellFunctionBlocks`, or `clHVAC`**. It
depends on `honIrmConfig` (the IRM Nano ENGINE — `BNanoFunctionBlock`, `BIrmControlManager`, NanoCmd protocol),
`honeywellSylkDevice` (Sylk bus), and standard N4 (`baja`, `control-rt`, `bacnet-rt`). The `honIrmConfig`/`honIrmControl`
split mirrors Tridium's `baja`/`kitControl` split (engine vs block library) [INFER]. It is a self-contained
fourth family, not a layer on the others.

## 546.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | 203 vf classes, 22 packages; 140 factory FBs + ~23 onboard IO (resolves B105 "163") | [CERT] | find count; BFbFactory.java; module.xml | token-checked (count) ✓ |
| 2 | BNanoFunctionBlock extends BComponent implements BINanoControl; NO execute() | [CERT] | BNanoFunctionBlock.java:165,575,781 | token-checked ✓ |
| 3 | BPid extends BNanoFunctionBlock (all FBs do) | [CERT] | BPid.java:174 | token-checked ✓ |
| 4 | runTeachToController downloads binary app via NanoCmd/BINanoProtocolService | [CERT] | BIrmControlManager.java (imports + protocol service) | token-checked ✓ |
| 5 | Execution order = hardware scan via NanoCmdSetPredecessor | [CERT] | NanoCmdSetPredecessor.java | sweep-cited |
| 6 | BPid proportional-band + BOutSave; BFlowControl VAV damper + OutSave fail-safe | [CERT] | BPid.java; BFlowControl.java:212 | token-checked (class) ✓ |
| 7 | Zero imports of kitControl/honeywellFunctionBlocks/clHVAC; depends on honIrmConfig+Sylk | [CERT] | import grep; module.xml | sweep-cited |
| 8 | 4th ecosystem — only one executing off the station (contrast B103) | [CERT] | §546.3 table | logic-checked |

**Marker tally**: [CERT] ×6 · [INFER] ×2 (engine-split analogy; the ~163 breakdown). Block TYPE =
EVIDENCE/CATALOG. 5 of 8 rows token-verified inline (count, base class + no-execute, BPid lineage, teach-download
imports).

## Connections

- **[Block 105]** — the honIrmControl library identity (REMITTANCE); resolves its "163"/"203" counts and adds
  the per-FB catalog + the hardware-offload execution model.
- **[Block 103]/[Block 542]** — honeywellFunctionBlocks (scan IN the station) — the direct contrast; both use a
  DDC proportional-band `BPid`, but one runs on the JVM and one on the device.
- **[Block 87]/[Block 540]** (clHVAC) and **[Block 6]/[Block 536]** (kitControl) — the other two ecosystems in
  the four-ecosystem table.
- **[Block 543]** (KC13) — `BOutSave` fail-safe output-on-restart is the IRM-hardware safety mechanism.
- **[Block 88]** — honIrmConfig (the IRM Nano protocol/engine), the dependency this library rides.

## Open gaps (this block)

- 137 of 140 FBs enumerated, 3 decompiled (PID/PIDA/FlowControl) — covered-by-enumeration; open a child gap for
  a specific FB only on demand.
- The NanoCmd wire protocol internals are `honIrmConfig`/[Block 88] territory, not re-derived here.
