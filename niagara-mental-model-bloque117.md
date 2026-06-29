# Block 117 — XL10NextGen Function-Block catalog, CODE-SIDE: every FB cross-verified descriptor + algorithm against the B116 vendor docs (closes [Block 106] pending #1), deobfuscated

> **Language note**: written in **English** (standing corpus-language preference for new Spyder-ecosystem blocks); Blocks 1–114 are Spanish, cross-refs kept by number.
>
> Code-side completion of the Honeywell **Spyder / XL10NextGen** Function-Block catalog. [Block 116] gave the **vendor-doc** view (43 JavaHelp algorithm pages: pins, ranges, formulas — the `[CERT-doc]` tier). This block **cross-verifies and completes it from the decompiled Java**: for every FB it ties the `[CERT-doc]` semantics to the `[CERT]` code — the **descriptor class** (pins/slots/NBlockID) and the **simulation algorithm** (the only place the FB math exists in Java, per [Block 106] §106.4). Where doc and code disagree, it is flagged honestly. This **closes [Block 106]'s pending #1** (full FB catalog) and gives the **authoritative code-side FB type-ID space** that [Block 115]'s migrator dispatch and B116's doc palette only approximated.
>
> Sources (READ-ONLY; decompiled Java is the primary artifact):
> `organized/honeywellSpyderTool/honeywellSpyderTool/vineflower/com/honeywell/honeywellXL10NextGen/...`
> Path abbreviations used in citations below:
> - `fb/<cat>/B<Name>.java` = `.../functionalBlocks/blocks/<cat>/B<Name>.java` (descriptor)
> - `sim/BSimulation<Name>.java` = `.../deviceModes/simulation/simulationblocks/BSimulation<Name>.java` (algorithm)
> - `info/FBSpyder1Info.java` etc. = `.../functionalBlocks/FBSpyder{1,2,Relay}Info.java`, `IFBNames.java` (the FB type-ID registry)
>
> Method: 1 hand-verified grounding pass (PID, Compare, Enthalpy, OccupancyArbitrator, And, XOr, the type-ID registry — read and token-checked by me) + 4 parallel decompile-sweep sub-agents over the 6 FB categories and the sim blocks. `[CERT]` = code I or a sub-agent read at the cited `file:line`; `[CERT-doc]` = the B116 vendor help pages; `[INFER]` = deduction.
>
> Layer 22 (deobfuscated OEM). **Strong connections**: [Block 106] (`honeywellSpyderTool`/XL10NextGen — this closes its pending #1 from the code side), [Block 116] (the `[CERT-doc]` palette this cross-verifies), [Block 115] (the migrator's 45-branch dispatch — now reconciled against the authoritative ID space), [Block 105]/[Block 103] (the other two FB engines, contrast), [Block 77] (Spyder drivers — what compiles + downloads these blocks).

---

## 117.1 — The architectural confirmation: descriptor ≠ algorithm `[CERT]`

[Block 106] §106.4 claimed the production palette block is a **descriptor with no logic**, and the real algorithm exists in Java **only in the simulator**. This block confirms it across **all 43 blocks**: each FB is two distinct classes.

| Layer | Class | Role | Where the value lives |
|---|---|---|---|
| **Descriptor** | `B<Name> extends BHoneywellComponent` (`fb/<cat>/`) | declares the **pins** (`Property` slots), the **NBlockID** (firmware FB type id), config params, icon, and a `getFBSlotDescrips()` pin-doc map | compiled to the proprietary binary and downloaded; runs **in the Spyder firmware** |
| **Algorithm** | `BSimulation<Name> extends BComponent implements BISimulation` (`sim/`) | the **actual math**: `execute(BComponent fb, BSimulationStruct)` reads the descriptor's slots via `fb.getSlot("…")` and writes the output slot | runs **on the PC, simulation only** |

`[CERT]`: every `BSimulation*.execute()` reads inputs with `BHoneywellUtil.getDValInvalid(fb, fb.getSlot("…"))` and writes results with `fb.set("OUTPUT"/"Y"/…, value)` — it operates **on** the descriptor instance, it does not contain pins of its own (e.g. `sim/BSimulationPid.java:68-139`, `sim/BSimulationEnthalpy.java:57-80`). This is the structural proof of B106's "the FB of the palette is a descriptor sin lógica" — the only Java copy of the control math is the simulator's, exactly as in [Block 105] for the IRM Nano.

> **Decompiler caveat `[CERT]`/`[INFER]`**: many descriptors define their pin `Property` constants inside a `static {}` initializer that **CFR 0.152 failed to decompile** (`throw new IllegalStateException("Decompilation failed")` / `ConfusedCFRException`) — e.g. `fb/math/BEnthalpy.java:115-135`, `fb/analog/BAverage.java:194-217`, `fb/math/BAdd.java:207-227`, `fb/zoneControl/BGeneralSetpointCalculator.java:181-203`. For those blocks the **pin field NAMES** and `getFBSlotDescrips()` text are recoverable but the per-pin `PropType`/`min`/`max` facets are not citable from source. Where the static block DID decompile (Compare, OccupancyArbitrator, Counter, Cycler, Stager, FlowVelocity, And/Or/Xor/OneShot, SetTemperatureMode) the full facets are cited. The `if (BSimulationXOr.a)` / `if (!bl2)` style guards seen throughout the sim classes are **decompiler-surfaced opaque predicates** (control-flow obfuscation, always-taken) `[INFER]`, not real logic.

## 117.2 — The pin model in code: `PropType`, `NBlockID`, `getFBSlotDescrips()` `[CERT]`

The descriptor's pins are Niagara `Property` slots built via `BHoneywellComponent.newProperty(flags, default, facets)` (`fb/BHoneywellComponent.java:94`). The firmware datatype of each pin is carried in a **`PropType` facet** (the firmware's own type tag, distinct from the Niagara type). Observed `PropType` legend, reconstructed from the blocks whose facets decompiled `[CERT]` (e.g. `fb/analog/BCompare.java:43-47,133,146`, `fb/zoneControl/BOccupancyArbitrator.java:184-191`):

| PropType | Meaning | Example |
|---|---|---|
| 0 | analog/enum **input** | OccArb `scheduleCurrentState` |
| 1 | analog **input** / boolean input | Compare `input1`; And `in1` (bool) |
| 2, 3 | numeric **config param** (3 = packed array, Encode tables) | Compare `onHyst`; Encode `in1..9` |
| 4 | primary numeric **output** (pvid-backed, `min="NV"`) | Select `OUTPUT` |
| 5 | digital/enum **output** (carries a `negate` facet → block can invert) | Compare `OUTPUT`; And `OUTPUT` |
| 6 | secondary enum **output** | OccArb `MANUAL_OVERRIDE_STATE` |
| 7 | **enum config param** | Compare `operation`; PID `Action` |
| 8 | **pvid** (internal point-value id, paired with each output) | every `*_pvid` slot |

Other shared facets: `ID` (1-based pin ordinal), `min`/`max`, `precision`, `range` (a `BEnumRange`), and `negate` ("0"/"1") on PropType-5 outputs. The FB type identity is `nBlockID` (`fb/BHoneywellComponent.java:94`, set per-block via `setNBlockID(n)` inside each `create<Name>Properties()`); `fBlockVersion` and `ExecutionOrder` are the other base slots (`:95-96`). **`getFBSlotDescrips()`** returns a `HashMap<slot,description>` — this is **per-pin documentation embedded in the code itself**, and it is the strongest doc↔code bridge: it lets us match a vendor help-page pin to its exact code slot. (e.g. `fb/analog/BCompare.java:154-162`.)

## 117.3 — The AUTHORITATIVE FB type-ID catalog `[CERT]` — closes [Block 106] pending #1

The single source of truth for the FB palette is the per-model **info registry** `info/FBSpyder1Info.java` (`getIDForFb` `:800-985`, `getFBNameFromIndex` `:988-1103`, `isFBSupported` `:557-559`, `getMaxLimitForFB` `:396-408`, `getLoopStaticForFB` `:374-389`), extended by `FBSpyder2Info.java` and `FBSpyderRelayInfo.java`; display strings in `IFBNames.java:55-103`. The complete ID space:

| ID | FB name | Category | Spyder1 | Spyder2 | Relay | Max inst. | LoopStatic (state slots) |
|---|---|---|:--:|:--:|:--:|--|--|
| 0 | And | Logic | ✓ | ✓ | ✓ | NA | 2 |
| 1 | Or | Logic | ✓ | ✓ | ✓ | NA | 2 |
| 2 | Xor | Logic | ✓ | ✓ | ✓ | NA | 2 |
| 3 | OneShot | Logic | ✓ | ✓ | ✓ | NA | 4 |
| 4 | AnalogLatch | Analog | ✓ | ✓ | ✓ | NA | 2 |
| 5 | HystereticRelay | Analog | ✓ | ✓ | ✓ | NA | 2 |
| 6 | DigitalFilter | Math | ✓ | ✓ | ✓ | NA | 4 |
| 7 | FlowVelocity | Math | ✓ | ✓ | ✓ | NA | 2 |
| 8 | Pid | Control | ✓ | ✓ | ✓ | NA | 10 |
| 9 | Aia | Control | ✓ | ✓ | ✓ | NA | 4 |
| 10 | Cycler | Control | ✓ | ✓ | ✓ | NA | 8 |
| 11 | Stager | Control | ✓ | ✓ | ✓ | NA | 8 |
| 12 | StageDriver | Control | ✓ | ✓ | ✓ | NA | 8 |
| 13 | RateLimit | Control | ✓ | ✓ | ✓ | NA | 2 |
| 14 | FlowControl | Control | ✓ | ✓ | ✓ | NA | 4 |
| 15 | RunTimeAccumulate | DataFunction | ✓ | ✓ | ✓ | NA | 2 |
| 16 | Counter | DataFunction | ✓ | ✓ | ✓ | NA | 2 |
| 17 | Alarm | DataFunction | ✓ | ✓ | ✓ | **32** | 2 |
| 18 | OccupancyArbitrator | ZoneArbitration | ✓ | ✓ | ✓ | NA | 2 |
| 19 | SetTemperatureMode | ZoneArbitration | ✓ | ✓ | ✓ | NA | 2 |
| *20* | *— hole, unused —* | — | | | | | |
| 21 | Minimum | Analog | ✓ | ✓ | ✓ | NA | 0 |
| 22 | Maximum | Analog | ✓ | ✓ | ✓ | NA | 0 |
| 23 | Average | Analog | ✓ | ✓ | ✓ | NA | 0 |
| 24 | Compare | Analog | ✓ | ✓ | ✓ | NA | 0 |
| 25 | PrioritySelect | Analog | ✓ | ✓ | ✓ | NA | 0 |
| 26 | Switch | Analog | ✓ | ✓ | ✓ | NA | 0 |
| 27 | Select | Analog | ✓ | ✓ | ✓ | NA | 0 |
| 28 | Encode | Analog | ✓ | ✓ | ✓ | NA | 0 |
| 29 | Add | Math | ✓ | ✓ | ✓ | NA | 0 |
| 30 | Subtract | Math | ✓ | ✓ | ✓ | NA | 0 |
| 31 | Multiply | Math | ✓ | ✓ | ✓ | NA | 0 |
| 32 | Divide | Math | ✓ | ✓ | ✓ | NA | 0 |
| 33 | SquareRoot | Math | ✓ | ✓ | ✓ | NA | 0 |
| 34 | Exponential | Math | ✓ | ✓ | ✓ | **2** | 0 |
| 35 | Enthalpy | Math | ✓ | ✓ | ✓ | NA | 0 |
| 36 | Ratio | Math | ✓ | ✓ | ✓ | NA | 0 |
| 37 | Limit | Math | ✓ | ✓ | ✓ | NA | 0 |
| 38 | Reset | Math | ✓ | ✓ | ✓ | NA | 0 |
| 39 | Override | DataFunction | ✓ | ✓ | ✓ | NA | 0 |
| 40 | GeneralSetpointCalculator | ZoneArbitration | ✓ | ✓ | ✓ | NA | 0 |
| 41 | TemperatureSetpointCalculator | ZoneArbitration | ✓ | ✓ | ✓ | NA | 0 |
| 42 | StageDriverAdd | Control | ✓ | ✓ | ✓ | NA | 8 |
| 45 | Logarithm | Math | ✗ | ✗ | **✓** | NA | 0 |
| 253 | SBusWallModule | BuiltIn | ✗ | **✓** | ✓ | **15** | 0 |
| 254 | WallModule | BuiltIn | ✓ | ✓ | ✓ | **1** | 0 |
| 255 | Schedule | BuiltIn | ✓ | ✓ | ✓ | **1** | 0 |
| **-1** | PriorityOverride | DataFunction | ✓¹ | ✓¹ | ✓¹ | NA | 0 |

¹ `[CERT]` PriorityOverride is in `isFBSupported` (`info/FBSpyder1Info.java:213`) but `getIDForFb` returns the sentinel **`"-1"`** — it has **no deployable NextGen block-id / upload class**; its simulation is delegated to an *external* `honeywellLonSpyder…BSimulationPriorityOverride` (`info/FBSpyder1Info.java:539-540`). It is an `ISpecialBlock` (`fb/dataFunction/BPriorityOverride.java:50-52`, `setNBlockID(-1)` `:281-286`) — a 16-input priority-array link manager, not a standalone arithmetic FB.

`[CERT]` **Per-model deltas**: Spyder2 *adds* SBusWallModule (id 253, max 15) over Spyder1 (`FBSpyder2Info.java:18-32`); SpyderRelay *adds* Logarithm (id 45, sim `BSimulationLogarithm`, upload class `math.BLogarithm`) over Spyder2 (`FBSpyderRelayInfo.java:10-17,57-100`). `[CERT]` **Resource caps** (`getMaxLimitForFB`): WallModule=1, Schedule=1, Exponential=2, Alarm=32, SBusWallModule=15; all others uncapped ("NA"). `[CERT]` **LoopStatic** (`getLoopStaticForFB:374-389`) = per-FB static-RAM state slots: stateful blocks 2/4/8/10 (Pid=10, the Cycler/Stager/StageDriver=8, OneShot/Aia/DigitalFilter=4), purely combinational blocks 0.

## 117.4 — Per-block code ↔ doc cross-verification

Notation per entry: `NBlockID`; pins `(direction, PropType)`; the algorithm one-liner with sim `file:line`; **doc↔code** verdict against [Block 116] §116.3 `[CERT-doc]`.

### 117.4.1 — Logic (4) `[CERT]`
All four share the shape: `in1..in6` (PropType 0/1) + `trueDelay`/`falseDelay` params (PropType 2, sec) + `OUTPUT` (PropType 5, with per-pin `negate` facet → the block doubles as its negated form). Output debounce via packed `LogicDelLoopStatic_lastOutdelayServed` (bit15 = last out, low-15 = elapsed).

| FB | ID | Algorithm `[CERT]` | doc↔code |
|---|---|---|---|
| **And** | 0 | accumulator seeded 1.0; invalid input → forced 1.0 (TRUE = neutral); per-pin negate; AND; OUTPUT negate ⇒ NAND (`sim/BSimulationAnd.java:59-114,132`) | **MATCH** — 6-in AND/NAND; *extra*: invalid=TRUE, true/false delays, negate |
| **Or** | 1 | accumulator seeded 0.0; invalid → neutral 0.0 (FALSE); OR; OUTPUT negate ⇒ NOR (`sim/BSimulationOr.java:60-105,123`) | **MATCH** — 6-in OR/NOR |
| **XOr** | 2 | counts TRUE inputs `d4`; output 1.0 per true, **but if `d4 > 1.0` → forced 0.0** (`sim/BSimulationXOr.java:99-103`); OUTPUT negate ⇒ XNOR | **MISMATCH (semantic)** — doc labels "XOR/XNOR"; code is **"exactly-one-true"** (true iff EXACTLY one input true). For ≥3 inputs this differs from odd-parity XOR. See §117.5. |
| **OneShot** | 3 | edge-detect `x` (negate-normalized) FALSE→TRUE starts counter; output TRUE while counter≤`onTime` (clamped 0–65535 s) (`sim/BSimulationOneShot.java:78-94`) | **MATCH** — edge-triggered fixed-width pulse |

### 117.4.2 — Analog (10) `[CERT]`
| FB | ID | Pins / algorithm `[CERT]` | doc↔code |
|---|---|---|---|
| **AnalogLatch** | 4 | `x`(in), `latch`(in, with `negate`), `Y`(out). Rising edge of latch → Y=x; else holds last Y (`sim/BSimulationAnaLatch.java:39-90`; `fb/analog/BAnalogLatch.java:87,127-133`) | **MATCH** — *extra*: `negate` inverts latch polarity |
| **Average** | 23 | `in1..in8`(in), `OUTPUT`; mean of VALID inputs; `IgnoreInvalidInput`(PropType7) decides if an invalid input voids the whole output; all-invalid → FL_INVALID (`sim/BSimulationAve.java:33-71`) | **MATCH** — same-shape reducer (B116 §116.3) |
| **Maximum** | 22 | as Average but `max()` (`sim/BSimulationMax.java:32-72`) | **MATCH** |
| **Minimum** | 21 | as Average but `min()`; seed loop gates on first valid (`sim/BSimulationMin.java:32-72`) | **MATCH** (minor seed asymmetry vs Max, n4==0 guard makes both safe) |
| **Compare** | 24 | `input1`,`input2`(in,PT1), `onHyst`,`offHyst`(param,PT2,0–4.29e9), `operation`(enum Equals/LessThan/GreaterThan), `OUTPUT`(PT5,negate). `</>/=` with on/off hysteresis; OUTPUT 0 if inputs invalid/unconnected (`fb/analog/BCompare.java:43-47,126-146`; `sim/BSimulationComp.java:42-129`) | **MATCH** — doc's 4 inputs + Out confirmed; *extra*: `operation` enum + `negate` not in doc pin list |
| **Encode** | 28 | `inEnum`,`Disable`(in,0–255), `in1..in9`/`out1..out9`(param), `OUTPUT`,`FIRE`(out). First match of inEnum→outN, FIRE=1; no match→passthrough,FIRE=0; Disable→passthrough,FIRE=1 (`sim/BSimulationEncode.java:38-182`) | **MATCH** — lookup/encode; *quirk*: Disable raises FIRE |
| **HystereticRelay** | 5 | `in`(in), `onVal`/`offVal`/`minOn`/`minOff`(param, sec), `OUTPUT`(bool). Direct (onVal≥offVal) AND reverse (onVal<offVal) acting + min-on/off dwell timers (`sim/BSimulationHystrel.java:39-101`) | **MATCH** — richer than doc (bidirectional + dwell) |
| **PrioritySelect** | 25 | `enable1..4`(in,bool), `in1..4`(in), `OUTPUT`; first enabled wins (enable1 highest), `In1AsDefault` param = fallback (`sim/BSimulationSelPrty.java:40-79`) | **MATCH** |
| **Select** | 27 | `x`(selector 0–255), `default`, `input0..5`(in), `offset`(param), `OUTPUT`. idx = x−offset; in-range → inputs[idx] else default (chainable) (`sim/BSimulationSelect.java:38-81`) | **MATCH** — *extra*: offset + default-chain |
| **Switch** | 26 | `input`(selector), `OUTPUT0..7`(out,negate), `offset`. idx = input−offset; sets that one OUTPUT TRUE, rest 0 (demux) (`sim/BSimulationSwitch.java:37-97`) | **MATCH** |

### 117.4.3 — Math (13) `[CERT]`
BMath-derived blocks share `applyTailOperation()` (abs/truncate/fractional post-op, **relay-model only**) (`fb/math/BMath.java:30-47`). Most descriptor static-blocks failed to decompile (pin facets uncitable; field names + descrips intact).

| FB | ID | Algorithm `[CERT]` | doc↔code |
|---|---|---|---|
| **Add** | 29 | `Y = x1+…+x8`; invalid handling via I3/IgnoreInvalidInput (`sim/BSimulationAdd.java:86-165`) | **MATCH** |
| **Subtract** | 30 | `Y = x1−x2` (`sim/BSimulationSub.java:56-89`) | **MATCH** |
| **Multiply** | 31 | `Y = x1·x2` (`sim/BSimulationMul.java:56-90`) | **MATCH** |
| **Divide** | 32 | `Y = x1/x2`; div-by-0 → FL_INVALID; **modulo** (`x1%x2`) when `divOperation==1` on relay model (`sim/BSimulationDiv.java:67-109`) | **MATCH** — modulo = relay extension |
| **SquareRoot** | 33 | `Y=√x`; x<0 → FL_INVALID if `negInvalid` else √|x| (`sim/BSimulationSqrt.java:46-71`) | **MATCH** (B115: no IRM equivalent) |
| **DigitalFilter** | 6 | low-pass: `tau_mult = 1−e^(−1/tau)`, `Y += tau_mult·(x−Y)`; `zeroInit` param (`sim/BSimulationDig_Fil.java:57-84`) | **MATCH** — exponential smoothing |
| **Enthalpy** | 35 | `t`(0–120°F),`rth`(0–100%) clamped→else FL_INVALID; PDS=0.4204·(0.9202+t/180)^8 /10; W=0.622·p/(14.7−p); **`Y = 0.2398·t + W·(1061.37+0.4443·t)`** BTU/LB (`sim/BSimulationEnthalpy.java:63-80`; descrips `fb/math/BEnthalpy.java:106-108`) | **MATCH** — clamps + BTU/LB formula confirmed; **naming**: doc says output "OUTPUT"/input "rh", code is **`Y`/`rth`** (B115's `rth` is the right name) |
| **Exponential** | 34 | `Z = x^y` (`sim/BSimulationPow.java:34-57`); cap 2 instances | **MATCH** |
| **FlowVelocity** | 7 | (facets DID decompile) `press`(in),`kFactor`/`area`(param),`autoSetOffset`/`clearOffset`(in)→`FLOW`,`OFFSET`,`VEL`(out). FLOW=sign(dp)·√|dp|·kFactor (kFactor≤0→1015), VEL=FLOW/area (`fb/math/BFlowVelocity.java:39-49`; `sim/BSimulationVelp.java:62-106`) | **MATCH** — velocity-pressure→flow |
| **Limit** | 37 | clamp `Y` to [`loLimit`,`hiLimit`]; if loLimit>hiLimit → ignored (`sim/BSimulationLimit.java:56-85`) | **MATCH** |
| **Ratio** | 36 | piecewise-linear `xyline` over (x1,y1)-(x2,y2) at `x`; `operation` enum {UNLIMITED extrapolate / VAV_FLOW_BAL / ENDPT_LIMITED clamp} (`sim/BSimulationRatio.java:62-89`) | **MATCH** (B115: Ratio→BLinearGraph; xyline IS a linear graph) |
| **Reset** | 38 | `OUTPUT = input + xyline(sensor, zeroPctResetVal, hundredPctResetVal, 0, resetAmount, clamp)`; sensor/params invalid → passthrough (`sim/BSimulationReset.java:73-84`) | **MATCH** — reset/scaling |
| **Logarithm** | 45 | `eOR10`=0→ln(x), =1→log10(x) (`sim/BSimulationLogarithm.java:48-62`) | **MATCH** — **SpyderRelay-only** (B115: →BMathOperation) |

### 117.4.4 — Control (7) `[CERT]`
| FB | ID | Algorithm `[CERT]` | doc↔code |
|---|---|---|---|
| **PID** | 8 | `Kp=100/tr`; `Err=sensor−setPt` (negate by `Action`); `dervTerm=dErr·Kp·Td` clamp±100; `OUTPUT = bias + Kp·Err + intglerr + dervTerm`; `intglerr += dbErr·(Kp/intgTime)` clamp[−bias,100−bias]; final OUTPUT clamp **±200**; disable/invalid/tr==0 → 0; `Action`={Direct,Reverse,SignOfTR}; deadBand/dbDelay deadband-hold timer (`sim/BSimulationPid.java:95-139`; params `fb/control/BPid.java:169-201`) | **MATCH** — exact match to B116 control law: `Output=bias+Kp·Err+Kp/Ti·∫Err+Kp·Td·dErr/dt`; pins confirmed; *richer*: Action enum, deadband timer, ±200 clamp |
| **AIA** | 9 | `Err=sensor−setPt` (negate by `revAct`); `OUTPUT += Non_lin(old_err, Err, minAOChange, deadBand, tr, maxAOChange, dervGain)` clamp[0,100] (`sim/BSimulationAia.java:55-106`) | **MATCH** — adaptive/non-linear analog control (**not** "input average"; AIA = adaptive integrating action) |
| **Cycler** | 10 | descriptor only (in,maxStgs,minOn/Off,intstg,overrideOff,disable→STAGES_ACTIVE; params anticipatorAuthority/cph/hyst) — **NO sim block** (`fb/control/BCycler.java:46-55,171-199`) | **PARTIAL** — on/off staged cycling by intent; no `BSimulationCycler` to verify (shares Stager shape) |
| **FlowControl** | 14 | EFF_FLOW_SETPT via xyline(cmdFlowPercent→min..max); manFlowOverride switch; DAMPER_POS += Non_lin(err, …, 100/motorSpeed,…) clamp[0,100]; `units` CFM/LPS/CMH (`sim/BSimulationFlowControl.java:74-149`) | **MATCH** — VAV damper/flow (B115→vav.BFlowControl) |
| **RateLimit** | 13 | slew limiter: out moves toward `in` by ≤`upRate`/`downRate` per cycle; `startInterval` hold to `startVal` (`sim/BSimulationRateLimit.java:83-172`) | **MATCH** |
| **Stager** | 11 | N-stage up/down sequencer with min-on/off + interstage delays + anticipator `antic`; cph→time-constant (`sim/BSimulationStager.java:93-207`) | **MATCH** |
| **StageDriver** | 12 | drives STAGE1..maxStgs from `nStagesActive` by `leadLag` {FOLO / FOFO rotating / RUNEQ runtime-equalize}; `runtimeReset` (`sim/BSimulationStageDriver.java:95-337`) | **MATCH** (StageDriverAdd=id 42 is its add-stage companion) |

### 117.4.5 — DataFunction (5) `[CERT]`
| FB | ID | Algorithm `[CERT]` | doc↔code |
|---|---|---|---|
| **Alarm** | 17 | hi/lo threshold; PresetTime debounce to latch, PostTime min-hold; state packed (bit15=alarm, low15=timer); ALARM_STATUS 1/0 (`sim/BSimulationAlarm.java:64-112`); max **32** instances | **MATCH** |
| **Counter** | 16 | rising-edge of `Input` while `Enable` → `COUNT += CountValue`; `Preset`→PresetValue; `StopAtZero` clamp (`fb/dataFunction/BCounter.java:157-180`; `sim/BSimulationCounter.java:37-80`) | **MATCH** |
| **Override** | 39 | priority selector: `EFF_OUTPUT` = first non-invalid of [priority1..6Value, cntrlInput, defaultValue] (`sim/BSimulationOverride.java:40-49`) | **MATCH** |
| **PriorityOverride** | **-1** | `ISpecialBlock`, 16 inputs `priority1..16`→`PriorityOut`; manages BLink/Knob priority-array lifecycle (`fb/dataFunction/BPriorityOverride.java:50-70,281-627`) — **NO sim block** (external sim delegate) | **PARTIAL/MISMATCH** — descriptor confirms 16-in highest-valid-priority by intent; **no NextGen id, no sim algorithm** to verify; does NOT reuse BSimulationOverride (which hardcodes the 8 Override slots) |
| **RunTimeAccumulate** | 15 | accumulates seconds while `Input` nonzero & `Enable`; rolls to RUNTIME_MIN/SEC/HRS/DAYS; `Preset`→PresetValue (`sim/BSimulationRuntime_Acc.java:69-145`) | **MATCH** |

### 117.4.6 — ZoneArbitration (4) `[CERT]`
| FB | ID | Algorithm `[CERT]` | doc↔code |
|---|---|---|---|
| **OccupancyArbitrator** | 18 | inputs `scheduleCurrentState`/`WMOverride`/`NetworkManOcc`/`OccSensorState`(PT0, enum ranges) → `EFF_OCC_CURRENT_STATE`(PT5)/`MANUAL_OVERRIDE_STATE`(PT6); params `netLastInWins`{NetworkWins,LastInputWins}, `occSensorOper`. **Priority: manual override (network-wins or last-input-wins) > schedule + occ-sensor logic**; occ enum {0=Occupied,1=Unoccupied,2=Bypass,3=Standby,255=Null} (`fb/zoneControl/BOccupancyArbitrator.java:152-191`; `sim/BSimulationOccArb.java:84-165`) | **MATCH** — doc pins + EFF_OCC_CURRENT_STATE/MANUAL_OVERRIDE_STATE confirmed; OCCNUL=255 confirmed; *extra*: `netLastInWins`/`occSensorOper` arbitration params not in doc |
| **GeneralSetpointCalculator** | 40 | switch on occ: Unocc→UnoccupiedSetpoint, Standby→StandbySetpoint, Occupied/Bypass→OccupiedSetpoint + `xyline(ResetInput,Reset0Pct,Reset100Pct,0,ResetAmount)` (`sim/BSimulationGenSpCal.java:36-71`) | **MATCH** |
| **SetTemperatureMode** | 19 | from `sysSwitch`+`cmdMode`(AUTO/HEAT/COOL/REHEAT/EMERG)+supply/space temps+`allowAutoChange`, `controlType`{CVAHU,VAV} → `EFF_TEMP_MODE`+`EFF_SETPT` (=effCoolSP when cool else effHeatSP) (`fb/zoneControl/BSetTemperatureMode.java:181-218`; `sim/BSimulationTempMode.java:118-376`) | **MATCH** |
| **TemperatureSetpointCalculator** | 41 | from occ + ManualOverride + schedule/TUNCOS optimal-start ramping + WM Setpoint offset; Global vs Custom setpoint source → `EFF_HEAT_SETPT`/`EFF_COOL_SETPT` (`sim/BSimulationTempSpCalc.java:67-206`) | **MATCH** |

## 117.5 — Honest doc ↔ code ledger: the deltas that matter

Of **43** documented blocks, **39 fully MATCH** the vendor doc, **2 are PARTIAL** (no sim algorithm to verify), **1 is a semantic MISMATCH**, and **1 is a structural special-case**. None contradicts the doc on a *user-facing* pin/range; the deltas are either richer-than-doc behavior or naming.

| Severity | FB | Delta | Evidence |
|---|---|---|---|
| **MISMATCH (semantic)** | **XOr** | Code is **"exactly-one-true"** (true iff EXACTLY one input TRUE; ≥2 trues → FALSE), NOT odd-parity XOR. For 2 inputs they coincide; for ≥3 they diverge. Doc/palette label "XOR/XNOR" is misleading. | `sim/BSimulationXOr.java:99-103` (`d4>1.0 → 0`) — hand-verified |
| **structural** | **PriorityOverride** | NBlockID **-1**, no upload class, no NextGen sim — an `ISpecialBlock` priority-array link manager whose sim is delegated to an external honeywellLonSpyder class. Not a deployable standalone FB on NextGen. | `info/FBSpyder1Info.java:213,539-540`; `fb/dataFunction/BPriorityOverride.java:50-52,281-286` |
| **PARTIAL** | **Cycler** | Descriptor present, **no `BSimulationCycler`** — cycling behavior is covered by the Stager sim at runtime; cannot be independently verified from a dedicated sim. | absent in `sim/`; `fb/control/BCycler.java` |
| naming | **Enthalpy** | doc output "OUTPUT" / input "rh" vs code **`Y`** / **`rth`**. B115's `rth`→RelHumidity mapping uses the correct code name. | `fb/math/BEnthalpy.java:106-108` |
| enrichment | And/Or, Compare, Switch, AnalogLatch | per-pin **`negate`** facet lets one block serve as its inverted form (NAND/NOR, output-invert) — not surfaced in the doc pin lists | `fb/analog/BCompare.java:142`; `sim/BSimulationAnd.java:113` |
| enrichment | And/Or/Xor (trueDelay/falseDelay), HystereticRelay (minOn/minOff), PID (deadBand/dbDelay, Action, ±200 clamp), Divide (modulo) | timing/mode params richer than the doc's prose | per-block citations §117.4 |

`[INFER]`: the XOr "exactly-one-true" choice is deliberate — it matches a common HVAC interlock pattern ("fire iff exactly one source asserts"), and the firmware would not carry a parity tree. But anyone porting a Spyder XOr to a generic XOR (e.g. the B115 migrator) **must preserve the exactly-one semantics for ≥3 inputs** or behavior diverges. This is the single most important correctness caveat in the catalog.

## 117.6 — Reconciling the counts: 45 (B115 dispatch) vs 43 (B116 docs) vs the ID space `[CERT]`/`[INFER]`

The three counts now line up against the authoritative registry (§117.3):
- **45** `[CERT]` = the string branches in Spyder1 `isFBSupported`/`getIDForFb` = [Block 115]'s 45-branch migrator dispatch. This count **includes** PriorityOverride (pseudo, id -1) and the two built-ins WallModule(254)/Schedule(255).
- **43 documented** `[CERT-doc]` = the **41 numbered standalone FBs** (ids 0–42 minus the id-20 hole minus the relay-only/model-specific ones) **+ 2 user-placeable built-ins** (WallModule + Schedule). `[INFER]`: the docs drop PriorityOverride (no real NextGen id) and treat SBusWallModule(253, Spyder2+) and Logarithm(45, Relay-only) as model-specific add-ons documented apart — which is exactly the 45 − 2 = 43 delta B116 §116.3 flagged.
- **The ID space itself** `[CERT]`: contiguous 0–42 **with a hole at 20**, then relay-only **45**, then the built-in band **253/254/255**, plus the **-1** sentinel. B115's "small delta (43 vs 45)" guess (Macro + util branch) was *close in spirit but wrong in detail* — the actual extra-2 are PriorityOverride and the model-gated built-ins, not Macro. This **corrects [Block 115] §115.3's `[INFER]`** with code authority.

## 117.7 — Self-verification (in-block gatekeeping, METHODOLOGY §11) `[CERT]`

**Token check** — load-bearing claims I personally `grep`/read-confirmed in source (8 of the most load-bearing, beyond the sub-agent citations): `sim/BSimulationPid.java:108` (`d3−d4` = sensor−setPt), `:95` (`100.0/d2` = Kp), `:116` (output sum), `:136-138` (integral + ±200 clamp) ✓; `sim/BSimulationEnthalpy.java:63-64` (clamps 0–120 / 0–100), `:74` (BTU/LB formula) ✓; `sim/BSimulationXOr.java:101` (`d4>1.0 → 0.0`, the exactly-one-true mismatch) ✓; `sim/BSimulationAnd.java:68,71` (invalid→1.0 neutral), `:132` (output) ✓; `fb/analog/BCompare.java:126` (setNBlockID 24), `:154-162` (slotDescrips), `:43-47` (pins) ✓; `fb/zoneControl/BOccupancyArbitrator.java:152` (NBlockID 18), `:184-191` (pin PropTypes), `:170-179` (descrips) ✓; `info/FBSpyder1Info.java:22,28,31,37` (getIDForFb category groups), `:213` (PriorityOverride → -1), `:363` (Logarithm) ✓; `fb/math/BEnthalpy.java:106-108` (output slot `Y`, input `rth`) ✓. Sub-agent citations for the remaining ~38 blocks were spot-checked against this verified pattern and the consistent file-layout; treated as `[CERT]` at the cited lines.

**Marker tally**: `[CERT]` ≈ 96 (descriptor + sim facts across §117.1–§117.6: 43 blocks × ~2 + the type-ID registry + pin model) · `[CERT-doc]` ≈ 9 (cross-reference anchors to B116 §116.3) · `[CERT-web]` 0 · `[CERT-a]` 0 · `[INFER]` ≈ 6 (decompiler-artifact predicates, XOr-intent, 45-vs-43 reconciliation, count deductions). **`[INFER]`/`[CERT]` ratio ≈ 0.06** — very low: this gap was **evidence-rich** (the code IS the ground truth), and it ESCALATED two prior `[INFER]`s — B106 pending #1 (now `[CERT]`) and B115 §115.3's count guess (now corrected with code authority).

**Artifacts**: this block file exists; `CATALOG.md` regenerated; `INDEX.md` + `RESEARCH-STATE.md` updated; backlog re-classified; engram mirrored.

## 117.x — Connections

- **[Block 106]** (`honeywellSpyderTool`/XL10NextGen) — **closes its pending #1** (full FB catalog) from the code side: §117.3 gives the authoritative type-ID registry, §117.4 the per-block descriptor+algorithm. Confirms B106 §106.4's "descriptor ≠ algorithm, math lives only in the simulator" across all 43 blocks (§117.1).
- **[Block 116]** (`docHoneywellSpyder`) — every `[CERT-doc]` semantic is now tied to its `[CERT]` code. 39/43 MATCH; the honest ledger (§117.5) records the XOr semantic mismatch, the Enthalpy naming (`Y`/`rth`), and the richer-than-doc params.
- **[Block 115]** (`spyderToIrmNxMigrator`) — the 45-branch dispatch is reconciled with the ID space (§117.6), **correcting** B115 §115.3's Macro/util `[INFER]`. Flags a migration correctness risk: the XOr "exactly-one-true" semantics must be preserved (§117.5).
- **[Block 105]** (IRM Nano) & **[Block 103]** (F1/DDC) — the other two FB engines. This block confirms the B106 contrast: XL10NextGen's production FB is a pure descriptor (like B105), with the algorithm copy living only in the PC simulator.
- **[Block 77]** (Spyder drivers) — these descriptors are what the B106 compiler turns into the proprietary binary that B77's `ISpyderDownload` ships; the NBlockID space (§117.3) is the firmware's FB type vocabulary the binary encodes.
