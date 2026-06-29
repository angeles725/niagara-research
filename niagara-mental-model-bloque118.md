# Block 118 — `honeywellSpyderTool` / XL10NextGen: the detailed I/O layer (`io/`) — physical points, sensor model, terminal assignment & model-gating, CODE-SIDE

> **Language note**: written in **English** (standing corpus-language preference for new Spyder-ecosystem blocks; legacy Blocks 1–114 are Spanish, cross-referenced by number).
>
> Empirical READ-ONLY research of the `io/` package of the Honeywell **`honeywellSpyderTool`** module (`com.honeywell.honeywellXL10NextGen.io`, **87 `.java`**: 37 top-level data/model classes, `manager/` 4, `ui/` 46). This is the **tool-side physical-points / I-O model** of the Spyder controller: how a wiresheet point binds to a controller terminal, how analog sensors are scaled/linearized, and how I/O availability is **gated by the selected controller model**. It is the **code-side answer to [Block 106]'s pending #2** and the cross-verification of **[Block 116] §116.4**'s `[CERT-doc]` physical-points/model-gating view.
>
> Sources (READ-ONLY, decompiled — vineflower/CFR clean):
> `/home/cristian/modules/Prototipos/modulos/organized/honeywellSpyderTool/honeywellSpyderTool/vineflower/com/honeywell/honeywellXL10NextGen/io/...`
> (cited as `io/<File>.java:line`; sibling packages `points/`, `xl10Controller/`, `functionalBlocks/blocks/enums/` cited with their relative path).
> Method: direct reading of the 37 top-level classes + the 4 managers + the enums + the 3770-line `BTerminalAssignmentHandler`; grep token-confirmation of every load-bearing `[CERT]`; cross-check against B116 (vendor doc) and B106/B117 (compiler + FB engine).
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official downloaded doc (`docHoneywellSpyder` help, cited via [Block 116]) · `[INFER]` deduction.
>
> Layer 22 (deobfuscated OEM). **Strong connections**: [Block 106] (the SpyderTool — this closes its pending #2), [Block 116] (vendor I/O doc — cross-verified here), [Block 117] (FB descriptor engine — physical points feed FB pins), [Block 104] (`honIOBase` — architectural contrast: runtime driver IO vs tool-side IO model), [Block 77] (Spyder BACnet/LON drivers — the compiled output goes down the wire), [Block 88] (Sylk/S-Bus wall modules — Sylk IO is folded into this layer).

---

## 118.1 — What the `io/` layer is (and is NOT) `[CERT]`

`io/` is the **engineering-time model of the controller's physical wiring**. It does **not** run on the JACE and it is **not** the runtime driver: the parent module is `runtimeProfile=wb` ([Block 106] §106.1). A "physical point" here is an editor object that the compiler later serializes into the binary download image ([Block 106] §106.3, confirmed §118.8).

Three sub-packages `[CERT]` (`find io -name '*.java'` = 87):

| Sub-package | Count | Role | Key classes |
|---|---|---|---|
| `io/` (top level) | 37 | data/model: point components + XML-loaded catalog + terminal handler | `BIOComponent`, `BModulatingInput/Output`, `BBinaryInput/Output`, `BTerminalAssignmentHandler`, `IOModelInfo`, `IOModelSpecificInfo`, `IOCommonInfo`, `SpyderIOModels`, `IOSensorType` |
| `io/manager/` | 4 | lifecycle + resource/memory accounting | `BIOManager`, `BIOResourceManager` + their interfaces `BIIOManager`/`BIIOResourceManager` |
| `io/ui/` | 46 | Workbench dialogs + linearization editor | `BModulatingInputDialog`, `BCustomSensorConfigurationDialog`, `BLinearizationPointBean`, `BIOMenu`, 26 single-letter obfuscated helpers |

**Scope boundary (honest)**: `io/` covers **physical points only** (Binary/Modulating In/Out) **+ Sylk IO**. B116 §116.4 also lists "Software Points" (Constants, Network In/Setpoint/Out) — those are **NOT** in `io/`; they live in `points/`/`network*`. So `io/` = the *hardware terminal* half of the controller I/O model `[INFER]` (from the absence of any Network/Constant class in `io/`, `ls io/`).

## 118.2 — The headline finding: the I/O model is **XML-driven**, not hard-coded `[CERT]`

The entire I/O catalog — point types, their dynamic properties, sensor tables, enum ranges — is **loaded from XML resources at runtime**, not expressed as Java fields. This is the architectural key to the whole layer.

- **Common catalog** (`SpyderIOModels.load`, `io/SpyderIOModels.java:71`): parses `local:|module://honeywellSpyderTool/com/honeywell/honeywellXL10NextGen/io/IO_Common.xml` into an `IOCommonInfo` singleton `[CERT]`. `IOCommonInfo.load` (`io/IOCommonInfo.java:73-98`) reads four sections — `Types/ModulatingOutput`, `Types/ModulatingInput`, `Types/BinaryOutput`, `Types/BinaryInput`, plus an `Enums` block — into `IOModulatingInputInfo`/`IOModulatingOutputInfo`/`IOBinaryInputInfo`/`IOBinaryOutputInfo` `[CERT]`.
- **Per-model catalog** (`SpyderIOModels.getIOModelSpecInfo`, `io/SpyderIOModels.java:62-66` → `IOModelInfo.load`, `io/IOModelInfo.java:50-179`): parses a **model-specific** XML (path from `BIMacro.getModelInfo().getModelSpecificIOXmlName()`) into per-model `Custom`/`Fixed`/`Std` subtype lists for each point type `[CERT]`.
- **Fixed-IO catalog** (`SpyderIOModels.getFixedIOInfo`, `io/SpyderIOModels.java:32-43` → `IOFixedIOInfo.load`, `io/IOFixedIOInfo.java:29-52`): parses a `lonXL10NextGenFixedIo` XML with `FixedIo` (per fixed point: `Terminal1`/`Terminal2`/`SensorType`/`TravelTime`) and `DontSensorCalibrate` entries `[CERT]`.

**Consequence for this research**: the *mechanism* is fully in code (`[CERT]`), but the *concrete per-model values* (which model exposes which fixed pin) live in those **XML data files**, which are **not** Java and were **not** read in this block. Those specific values remain `[CERT-doc]` from [Block 116] §116.4 (e.g. PVL6436AS = Actuator + on-board pressure sensor; PVL6438NS = pressure sensor only). **This is the honest doc↔code split** — see §118.7.

## 118.3 — Point types & classification `[CERT]`

**Point types** — `BIOTypesEnum` (`functionalBlocks/blocks/enums/BIOTypesEnum.java:17-31`):

| Ordinal | Const | Java component |
|---|---|---|
| 0 | `BinaryInput` | `BBinaryInput` (trivial type-marker, `io/BBinaryInput.java`) |
| 1 | `BinaryOutput` | `BBinaryOutput` (trivial, `io/BBinaryOutput.java`) |
| 2 | `ModulatingInput` | `BModulatingInput` (analog input, sensor logic) |
| 3 | `ModulatingOutputAnalog` | `BModulatingOutput` |
| 4 | `ModulatingOutputFloating` | `BModulatingOutput` (uses Terminal2) |
| 5 | `ModulatingOutputPwm` | `BModulatingOutput` |
| 255 | `IODefault` | sentinel |

**Cross-verify with [Block 116] §116.4** `[CERT-doc]`: the vendor doc lists **four** physical-point types (Binary In, Binary Out, Modulating In, Modulating Out). **MATCH with a refinement**: the code splits *Modulating Output* into **three electrical drive flavors** (Analog / Floating / PWM, ordinals 3/4/5) `[CERT]`. Not a contradiction — the doc counts user-facing categories; the code distinguishes the output hardware. **Floating** (3-wire actuator) is the one that consumes a **second terminal** (`Terminal2`, see §118.6).

**Classification** — `BIOClassficationEnum` (`functionalBlocks/blocks/enums/BIOClassficationEnum.java`): **`Fixed`=0**, **`Custom`=1** `[CERT]`. *Fixed* = a model-default point welded to a specific terminal (from the fixed-IO XML); *Custom* = a user-defined sensor/scaling on a free terminal. This maps exactly onto B116 §116.4's "default **(Fixed)** Physical points" `[CERT-doc]`.

**Binary-input sub-mode** — `BEnumDIInputType` (`io/BEnumDIInputType.java:17-26`): `Maintained`=0, `Momentary`=1, `Counter`=2, `PulseMeter`=3, `UnDefined`=255 `[CERT]`.

## 118.4 — The component model: `BIOComponent` is an `IOComp` child, not a control point `[CERT]`

`BIOComponent extends BComponent implements BIPhysicalPointInterface` (`io/BIOComponent.java:72-75`). It is **not** a `BControlPoint`; it is attached as a **child slot named `"IOComp"`** to a `BInputPoint`/`BOutputPoint` (the real wiresheet point) — see `BIOManager.createIOObject` (`io/manager/BIOManager.java:807-833`, `add("IOComp", ...)`) `[CERT]`. The wiresheet point also carries `"Pin1"`/`"Pin2"` string slots that display the terminal label (`BIOComponent.changed` rewrites `Pin1` to the terminal label or `"Unassigned"`, `io/BIOComponent.java:264-276`) `[CERT]`.

Frozen slots on `BIOComponent` (`io/BIOComponent.java:76-79`): `Terminal1` (int, default **-1** = unassigned), `IoClassification` (`BIOClassficationEnum`, default `Custom`), `sylkAddress` (int, default -1), `IsDynamicPropertyGenerated` (bool) `[CERT]`.

**Dynamic properties**: all the analog fields (`InputLow/InputHigh/OutputLow/OutputHigh/SensorType/InputUnit/SensorLow/SensorHigh`, etc.) are **not** frozen slots — they are **added at runtime** by `generateDynamicProperties → a()` (`io/BIOComponent.java:168-223`), reading field name/value/type/flags from the XML via `IOCommonInfo.getDynamicFields` (`io/IOCommonInfo.java:134-151`). Supported dynamic types: `float`, `String`, `int`, `boolean`, `BStatusNumeric`, `BUnit`, `BEnumDIInputType` `[CERT]`. This is why a Binary point and a Modulating point share one Java class family but expose different slots.

`BBinaryInput`/`BBinaryOutput` are **empty type-marker subclasses** (`io/BBinaryInput.java:15-30`) — all behavior is the XML-driven `BIOComponent` base `[CERT]`.

## 118.5 — Sensor model & linearization (the analog heart) `[CERT]`

**Sensor catalog** — `IOSensorType` (`io/IOSensorType.java`) carries: `name`, `sensorType` (short id), `DataCategory`, `DataType`, `InputLow/InputHigh`, `OutputLow/OutputHigh`, `SensorLow/SensorHigh`, `InputUnit`, precision, `subType` `[CERT]`. Loaded from the XML `Sensors` element by `IOModulatingInputInfo.load` (`io/IOModulatingInputInfo.java:40-58`) `[CERT]`.

**Two-point linear scaling (the core math)** — `BModulatingInput` computes a straight-line transform from the raw sensor input span to the engineering-unit output span (`io/BModulatingInput.java:213-214`, `436-437`):

```
g (gain)   = (OutputHigh - OutputLow) / (InputHigh - InputLow)
h (offset) =  OutputLow - g * InputLow
engValue   =  g * rawValue + h
```

`setSensorLimits` (`io/BModulatingInput.java:217-245`) applies it: `SensorLow = g*sensorLow + h`, `SensorHigh = g*sensorHigh + h`, swapping if inverted, then rounding to the unit's precision `[CERT]`. Degenerate spans (Output-span = 0 or Input-span = 0) throw `ArithmeticException` and reset to defaults (`io/BModulatingInput.java:199-212`) `[CERT]`.

**Input electrical mode** — `BAnalogInputModeEnum` via `getInputUnit` (`io/BModulatingInput.java:90-101`): `"volts"` → **Voltage**, `"ohms"` → **Resistive**, else **Undefined** `[CERT]`.

**Custom / piecewise linearization** — `BCustomSensorConfigurationDetails` (`io/BCustomSensorConfigurationDetails.java`) holds a `lzPtBeanVector` `BVector` of **`BLinearizationPointBean`** (each = `inputValue`,`outputValue` pair, `io/ui/BLinearizationPointBean.java:19-20`) — a **multi-point linearization table** `[CERT]`. The two custom flavors are `BCustomSensorTypeEnum`: **Voltage**=0, **Resistive**=1 (`io/ui/BCustomSensorTypeEnum.java:17-19`) `[CERT]`. For the named special sensors (`CustomResistive`/`CustomVoltage`/`CustomSensor`, `io/BModulatingInput.java:187-189`) the limits come from a real linearization table: `SpyderUnits.getLinearizationTableData` (`getSensorHighAndLowLimit`, `io/BModulatingInput.java:303-321`), and **resistive values are scaled ×1000** (ohms→kΩ) `[CERT]`. Specific Honeywell sensor part-numbers **`C7400A`,`C7632A`,`C7632B`,`C7600B`,`H7655A`** are special-cased to use the linear transform directly rather than a unit conversion (`io/BModulatingInput.java:371-377`, `462-465`) `[CERT]` — these are the genuine on-board sensors of B116 §116.4.

**Sylk sensors folded in** `[CERT]`: `IOModulatingInputInfo.loadSylkIO` (`io/IOModulatingInputInfo.java:254-273`) appends `SylkUtility.getSylkTypeAvailable(BSylkDeviceTypeEnum.sensor)`; `IOModulatingOutputInfo.loadSylkIO` (`io/IOModulatingOutputInfo.java:104-119`) appends `...actuator`. This is the **S-Bus wall-module IO of [Block 88]** surfacing as ordinary sensor/actuator types (B116's SIO4022/SIO6042/SIO12000/C7400S `[CERT-doc]`) — **MATCH**.

## 118.6 — Terminal assignment: the `byte[]` terminal map `[CERT]`

`BTerminalAssignmentHandler` (3770 lines, the largest `io/` class) is the allocator that binds points to physical terminals. It models the controller as a flat **`byte[]`**, one byte per terminal index.

**Array construction** — `createTerminalArray` (`io/BTerminalAssignmentHandler.java:112-130`): size = base controller terminal count (`IIOInfoInterface.getNumberOfTerminals()`) **plus** each attached Sylk I/O device's pin count, appended; a helper class `a` (`io/a.java`) maps `deviceAddress → [startIndex, endIndex]` `[CERT]`.

**Byte encoding** (`initializeByteArray` `io/BTerminalAssignmentHandler.java:206-247`; `setAddresses` `:384-427`; decode `getAddressDetails` `:783`):

| Bits | Meaning |
|---|---|
| low 3 bits (`b & 7`) | **type identifier** of the terminal: AI / DI / AO / DO / Misc |
| `0x20` | terminal **reserved** (`isAddressReserved`) |
| `0x80` | terminal **used / assigned** |
| `0xC0` | both (assigned **and** the special-reserved condition) |

Type identifiers are model-supplied, not constant: `getAITypeIdentifier/getDITypeIdentifier/getAOTypeIdentifier/getDOTypeIdentifier/getMiscTypeIdentifier` (`io/IIOInfoInterface.java:16-24`) `[CERT]`. The public contract enumerates the four electrical classes `ANALOG_INPUT=0, DIGITAL_INPUT=1, ANALOG_OUTPUT=2, DIGITAL_OUTPUT=3` (`io/BITerminalAssignmentHandler.java:34-37`) `[CERT]`.

**Floating output uses two terminals**: in `setAddresses` the `ModulatingOutputFloating` case (ordinal 4) marks **both** `nArray[0]` and `nArray[1]` used (`io/BTerminalAssignmentHandler.java:413-419`) `[CERT]` — confirming §118.3's Terminal2.

**Fixed vs Custom allocation** (`getIODetails` `io/BTerminalAssignmentHandler.java:811-852`; `getAddressAvailabilityForCustom` `:859-900`; `findFixedPinAndAssign` interface `io/BITerminalAssignmentHandler.java:96-98`): a **Fixed** point is forced onto the model's predetermined pin; a **Custom** point gets the next free terminal of the right type via `checkAvailabilityOfAddresses`. Note the MO fallback cascade: an Analog request can fall back to PWM, PWM to Analog, Floating to PWM-then-Analog (`getAddressAvailabilityForCustom` `:870-894`) `[CERT]`.

**Lease/restore & sync**: `getTerminalAssignmentArray()` (`io/BTerminalAssignmentHandler.java:3117`) exposes the raw `byte[]` for downstream consumers; `reassignAddresses()`/`unassignAddresses()` rebuild it when points are added/removed.

## 118.7 — Model-gating: cross-verification with [Block 116] §116.4 `[CERT]` + `[CERT-doc]`

B116 §116.4 stated (vendor doc) that point availability is **model-gated**: *"The Honeywell SpyderTool automatically validates rules, based on the model selected"* `[CERT-doc]`. **The code confirms this mechanism end-to-end** `[CERT]`:

1. **Every** availability/terminal query resolves the model first: `bIMacro.getModelInfo().getModelSpecificIOXmlName()` → `SpyderIOModels.getIOModelSpecInfo(...)` (counted **9** call sites across `io/`: `BTerminalAssignmentHandler`×4, `IOModelInfo`×3, `BIOComponent`×1, `IOModelSpecificInfo`×1, `BModulatingOutput`×1) `[CERT]`.
2. **Per-model terminal counts** come from the model's wiring spec: `getMaxConfigurableIO` → `ModelInfo.getWiring().getAnalogInputs().getCount()` (and `DigitalInputs/AnalogOutputs/DigitalOutputs/Misc`) (`io/IOModelSpecificInfo.java:147-163`); `getAddressIndex` → `getWiring()...getTerminals()[n].getV()` (`:187`) `[CERT]`.
3. **Auto-validation / auto-downgrade**: if a point's type is not supported by the chosen model, it is **silently converted to Custom** — `IOModelInfo.convertToCustomIfIONotSupported` (`io/IOModelInfo.java:214-235`) → `BModulatingInput.convertToCustomIO` (`io/BModulatingInput.java:157-179`); the manager raises the `"FixedIONotSupported"` notification (`io/manager/BIOManager.java:718-723`) `[CERT]`. **This is literally the "automatically validates rules" behavior the doc describes.**

**Honest doc↔code ledger**:

| Claim | Status | Evidence |
|---|---|---|
| 4 physical-point types (Binary/Modulating In/Out) | **MATCH (+refinement)** | code adds MO Analog/Floating/PWM split `[CERT]` BIOTypesEnum |
| Model-gated availability | **MATCH** | mechanism in code `[CERT]`; per-model **values** in XML (`[CERT-doc]` only) |
| "default (Fixed)" points | **MATCH** | `BIOClassficationEnum.Fixed=0` `[CERT]` |
| Sylk IO models (SIO/C7400S) | **MATCH** | `loadSylkIO` `[CERT]` ↔ B116 About page `[CERT-doc]` |
| Per-model fixed-point lists (e.g. PVL6436AS vs PVL6438NS) | **UNVERIFIABLE code-side** | values live in model XML, not Java → stays `[CERT-doc]` |

## 118.8 — Ties: FB engine [B117], compiler [B106], resource managers, contrast with `honIOBase` [B104]

**To the FB engine ([Block 117]/[Block 106] §106.4)** `[CERT]`: a physical point is a `BInputPoint`/`BOutputPoint` carrying an `IOComp` `BIOComponent` child + `Pin1`/`Pin2` labels; it sits on the wiresheet and links to FB descriptor pins like any other slot. `BIOManager.getIODetailsOnPointConversion` handles converting a Network/Constant point into a physical IO point (`io/manager/BIOManager.java:233-317`) `[CERT]`.

**To the compiler ([Block 106] §106.3)** `[CERT]`: `BSpyderIICompilation` reads `(BIOComponent)point.get("IOComp")`, `.getTerminal1()`, `.getSylkAddress()`, `.getOutputType(ioModelInfo)` and resolves the Sylk pin via `device.getPin(terminal1)` to emit the binary download image (`xl10Controller/compilation/BSpyderIICompilation.java:203-235`, `424-425`); `compileKingfisherInputs/Outputs` (`:450-581`) serialize S-Bus wall-module IO `[CERT]`. So **`io/` is the data source the compiler serializes** — the terminal `byte[]` (§118.6) and the sensor scaling (§118.5) become the controller's wiring config.

**Resource accounting** `[CERT]`: `BIOManager` (lifecycle `ioStarted/ioStopped/ioRemoved`, point-type conversions AI↔DI↔AO↔DO↔NV↔Constant) and `BIOResourceManager.calculateAndSetMemory` (`io/manager/BIOManager.java` calls it on every IO add/remove) mean **each physical point consumes a tracked slice of the controller memory budget**; the `BITerminalAssignmentHandler` instance is obtained per-macro from the resource manager (`xl10Controller/resourceManager/BIResourceManager.java:147`, `BLibResourceManager.java:110-125`) `[CERT]`.

**Contrast with `honIOBase` ([Block 104])** `[INFER]`: B104's `honIOBase` is the **runtime, on-JACE** IO base for Honeywell controllers (proxy points actually polling hardware). This `io/` layer is the **opposite end**: a Workbench-only, compile-time **description** of the same wiring that never executes — it produces the binary that a runtime driver ([Block 77]) downloads. Same domain (controller terminals), opposite lifecycle stage. (Marked `[INFER]` — the relationship is architectural, not a shared code path; the two do not reference each other.)

## 118.9 — Self-verify

- **Token checks (load-bearing `[CERT]`, grep-confirmed against source — 8/8 pass)**:
  1. linearization `g = (f5-f4)/(f3-f2)`, `h = f4 - g*f2` → `io/BModulatingInput.java:213-214,436-437` ✓
  2. byte flags `0x20`/`0xFFFFFF80`(=0x80)/`0xFFFFFFC0`(=0xC0) + decode `& 7` → `io/BTerminalAssignmentHandler.java:212,390,396,783` ✓
  3. `IO_Common.xml` resource path → `io/SpyderIOModels.java:71` ✓
  4. `getModelSpecificIOXmlName` 9 call-sites across 5 `io/` files ✓
  5. per-model counts `getWiring().getAnalogInputs().getCount()` → `io/IOModelSpecificInfo.java:149,152` ✓
  6. `ANALOG_INPUT=0..DIGITAL_OUTPUT=3` → `io/BITerminalAssignmentHandler.java:34-37` ✓
  7. `convertToCustomIfIONotSupported` → `io/IOModelInfo.java:214` + `io/manager/BIOManager.java:718` ✓
  8. `loadSylkIO` sensor/actuator → `io/IOModulatingInputInfo.java:255` + `io/IOModulatingOutputInfo.java:105` ✓
  - Also verified: `BIOTypesEnum` ordinals 0-5/255 (`BIOTypesEnum.java:17-31`), `BIOClassficationEnum` Fixed=0/Custom=1, `BEnumDIInputType` 0-3/255, `BCustomSensorTypeEnum` Voltage=0/Resistive=1, `IOComp` child slot (`BIOManager.java:807-833`).
- **Marker tally**: `[CERT]` ≈ 41 · `[CERT-doc]` ≈ 6 (all inherited via B116) · `[INFER]` ≈ 3 · `[CERT-web]`/`[CERT-a]` 0. **`[INFER]`/`[CERT]` ratio ≈ 0.07** — evidence is rich and code-grounded, **not** exhausted (the code IS the ground truth; the only gaps are the XML data files, which are a different artifact type).
- **Honest residual gap**: the **model-specific XML resource files** (`IO_Common.xml`, the per-model `*IO.xml`, the `lonXL10NextGenFixedIo` XML) were **not** read — they are data, not Java, and bundled inside the module. They hold the concrete per-model terminal maps / sensor tables. The *parsing mechanism* is `[CERT]`; the *values* stay `[CERT-doc]` (B116) or open. → feeds a possible future micro-gap (G4b: extract the IO XML resources).
- **Artifacts**: this block file created; CATALOG regenerated; INDEX + RESEARCH-STATE updated; engram mirrored.

## 118.10 — Connections

- **[Block 106]** — closes its **pending #2** (detailed I/O layer, `io/` ~87 classes): terminal assignment, linearization, sensor types now distilled code-side.
- **[Block 116]** — §116.4's `[CERT-doc]` physical-points / model-gating view is here cross-verified against code: MATCH on point types (with the MO 3-way refinement), Fixed/Custom, model-gating mechanism, Sylk IO; the per-model fixed-point *values* remain doc-only (XML not read).
- **[Block 117]** — the FB descriptor engine: physical points are wiresheet objects linked to FB pins; `IOComp` child + `Pin1/Pin2` labels are how a terminal binding shows on the FB sheet.
- **[Block 106] §106.3 compiler** — `BSpyderIICompilation` reads `BIOComponent.getTerminal1()/getSylkAddress()/getOutputType()` + the terminal `byte[]` to emit the binary download; `io/` is its data source.
- **[Block 104]** — `honIOBase` runtime IO contrast: that is on-JACE proxy IO; this is compile-time IO description (`[INFER]`, architectural).
- **[Block 88]** — Sylk/S-Bus wall modules: their sensors/actuators are folded into this layer via `loadSylkIO`.
- **[Block 77]** — the Spyder BACnet/LON drivers download the compiled image this layer feeds.
