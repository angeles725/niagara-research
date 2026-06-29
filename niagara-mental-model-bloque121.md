# Block 121 — `honeywellSpyderTool` / Kingfisher: the TR wall-module tool-side (Sylk/S-Bus model, the compiler chain, the LCD/HMI state machine), CODE-SIDE

> **Language note**: written in **English** (standing corpus-language preference for new Spyder-ecosystem blocks; legacy Blocks 1–114 are Spanish, cross-referenced by number).
>
> Empirical READ-ONLY research of the **Kingfisher** subsystem of the Honeywell **`honeywellSpyderTool`** Workbench module — the tool-side machinery that models, configures and *compiles to binary* the **TR-series Smart Room / wall modules** (TR23/TR40/TR42/TR70/TR71/TR75 + SIO + Sylk actuator) that hang off a Spyder controller over the **S-Bus (Sylk)** link. [Block 106] touched this subsystem (it confirmed the names "Kingfisher", `compileKingfisherInputs/Outputs`, the `BBasicKFCompiler→BZioEnhCompiler→BZioplusCompiler` chain, the `BKFStateMachine` LCD sim) but marked most of it **`[CERT-a]`** (sub-agent, unverified). This block **distills and verifies it directly**, closing [Block 106]'s pending angle on Kingfisher/Sylk tool-side.
>
> Scope: `functionalBlocks/blocks/builtIn/kingfisher/**` (~603 `.java`: the FB-engine wall-module classes, `compilation/`, `tr4x/{compilation,stores,ui}`, `datatypes/`, `resourceLimits/`, `resourceCounter/`, `simulation/`) **minus** the `kingfisher/ui` editor/wizard layer already distilled in [Block 119]; and `sylk/fw/**` (28 + `sylk/fw/ti/` 11) — the S-Bus tool-side framework. It answers: (1) what the TR wall-module model is in code, (2) how the Spyder↔Sylk-device data binding (PVID links) works, (3) how the Kingfisher compiler chain maps to the device families and produces the sectioned binary, (4) how the LCD/HMI is simulated, (5) how it ties to the FB engine ([Block 106]) and the wizard ([Block 119]), and (6) the integrity/security posture.
>
> Sources (READ-ONLY, decompiled — vineflower tree, CFR-clean headers):
> `/home/cristian/modules/Prototipos/modulos/organized/honeywellSpyderTool/honeywellSpyderTool/vineflower/com/honeywell/honeywellXL10NextGen/`
> Cited as `<relative-path>.java:line` from that package root (e.g. `sylk/fw/ti/BSylkTIDeviceModelEnum.java:24`, `functionalBlocks/blocks/builtIn/kingfisher/compilation/BKFCompilerFactory.java:36`).
> Method: direct reading + grep token-confirmation of every load-bearing `[CERT]` (class headers/extends chains, the model enum, the compiler-factory dispatch, the resource-limit values, the CRC call sites, the LCD segment/key model). One Explore sub-agent swept the `tr4x/stores` and `datatypes` breadth; every load-bearing line below was re-verified by me.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. (No `[CERT-doc]`/`[CERT-web]`/`[CERT-a]` — this gap is fully code-grounded; where it escalates a prior `[CERT-a]` from [Block 106] that is noted inline.)
>
> Layer 22 (deobfuscated OEM). **Strong connections**: [Block 106] (the SpyderTool — verifies/escalates its Kingfisher+Sylk `[CERT-a]`), [Block 119] (the Kingfisher *UI/wizard* — this is the model+compiler **behind** that wizard), [Block 116] (vendor doc — the TR wall-module operator reference), [Block 120] (the driver wire protocol that ships the binary this subsystem produces), [Block 88] (`honeywellSylkDevice` — the **runtime** S-Bus driver; this is the **tool-side** counterpart).

---

## 121.1 — What "Kingfisher" is: the TR wall-module tool-side (not a chip) `[CERT]`

"Kingfisher" is the internal codename for the **tool-side subsystem that configures and compiles the TR-series wall modules** (Smart Room Controllers / room sensors) that connect to a Spyder over S-Bus (Sylk). It is NOT a chip and NOT a driver — it is the part of the Workbench tool (`runtimeProfile=wb`, see [Block 106] §106.1) that lets the engineer lay out the wall-module's parameters, screens and labels and turn them into a downloadable binary.

The wall module is itself a **first-class function block** in the Spyder FB engine — it extends the same `BHoneywellComponent` root as every other Spyder FB ([Block 106] §106.2) `[CERT]`:

```
BSBusWallModule (kingfisher/BSBusWallModule.java:130)
  extends BAbstractSylkDeviceFB (kingfisher/BSBusWallModule.java:131)
  implements BILibItem        (:132)
     │
     └─ BAbstractSylkDeviceFB (sylk/fw/BAbstractSylkDeviceFB.java:15)
          extends BHoneywellComponent   (:16)     ← the FB-engine root (B106)
          implements BISylkDevice        (:17)
```

So a TR wall module *is* an FB on the wiresheet, but one that also implements `BISylkDevice` — the interface that knows how to **compile itself to / decompile itself from** an S-Bus binary `[CERT]` (sylk/fw/BISylkDevice.java). Its configuration is held in a `BKFConfig` bean: `BKFConfig extends BComponent` (configuration/BKFConfig.java:32) with `generalSettingsConfig` (:37), `getCategoryParameterConfig()` (:56), `getFanCommandConfig()` (:64) and a `getModifiedFlag()` `BBitSet` for incremental download (:48) `[CERT]`.

> **Nomenclature, now verified `[CERT]`** (escalates [Block 106] §106.1 `[CERT-a]`): the Kingfisher families split into two compiler lineages — **TR4x** (TR40/TR42) and **TR7x** (TR70/TR71/TR75, internally "Zio" / "Zio+"). Both are "Sylk TI devices" (`BSylkTIDevice`); "TI" is the S-Bus device abstraction, not Texas Instruments.

---

## 121.2 — The TR device model: the model enum `[CERT]`

The authoritative registry of every wall module the tool knows is `BSylkTIDeviceModelEnum` (sylk/fw/ti/BSylkTIDeviceModelEnum.java). The 21 ordinals (`:24`–`:44`) `[CERT]`:

| Ordinal | Constant | Family | Notes |
|---|---|---|---|
| 0 | `TR_23H` | TR4x-class | base sensor (H = humidity) |
| 1–4 | `TR_40`, `TR_40H`, `TR_40CO_2`, `TR_40HCO_2` | **TR4x** | TR40 + humidity / CO₂ variants |
| 5–8 | `TR_42`, `TR_42H`, `TR_42CO_2`, `TR_42HCO_2` | **TR4x** | TR42 + humidity / CO₂ variants |
| 9–10 | `TR_70`, `TR_70H` | **TR7x (Zio)** | LCD wall module |
| 11–12 | `TR_71`, `TR_71H` | **TR7x (Zio Enh)** | larger LCD wall module |
| 13–14 | `TR_75`, `TR_75H` | **TR7x (Zio+)** | largest LCD wall module |
| 15 | `UNKNOWN` | — | |
| 16–18 | `SIO_12000`, `SIO_6042`, `SIO_4022` | Sylk IO | Sylk-bus I/O expansion modules |
| 19 | `SYLK_ACTUATOR` | actuator | Sylk damper/valve actuator |
| 20 | `C_7400S` | sensor | C7400S enthalpy/air-quality sensor |

`BSylkTIDevice` is the base device class (`BZioTIDevice extends BSylkTIDevice`, kingfisher/BZioTIDevice.java:75-76) `[CERT]`; `SylkTIDeviceFactory` (sylk/fw/ti/) dispatches a model ordinal to its device class. The H/CO₂ suffix variants are *capability flags on the same base*, not separate hardware drivers — they collapse to the same compiler (see §121.4) `[INFER]` (grounded by the factory's ordinal grouping below).

Each model also carries an **on-wire device-type identity** in `BSBusWallModule` `[CERT]`: the TR7x family is `DEVICE_TYPE_MAJOR = 3` with minors `TR70=0`, `TR71=10`, `TR75=15` (kingfisher/BSBusWallModule.java:157-160); the TR4x family uses distinct majors `TR40_DEVICE_TYPE_MAJOR = 14` / `TR42_DEVICE_TYPE_MAJOR = 15`, both minor `= 10` (:161-164). This major/minor pair is how the controller distinguishes the two compiler lineages on the bus.

The device's **S-Bus bandwidth budget** differs by family — `BZioTIDevice` computes it two ways: `getTR4XSylkBandwidth(BSBusWallModule)` (kingfisher/BZioTIDevice.java:111) vs `getTR7XSylkBandwidth(BSBusWallModule)` (:118), selected at :100/:104 and stored via `setSylkBandwidthRequired(n2)` (:107) `[CERT]`. This verifies [Block 106] §106.5's `[CERT-a]` "`BZioTIDevice` calcula bandwidth S-Bus (TR4X vs TR7X)".

---

## 121.3 — Sylk / S-Bus tool-side framework (`sylk/fw/`) `[CERT]`

`sylk/fw/` is the tool-side S-Bus framework — the **compile/decompile + data-binding** layer (distinct from the runtime physical driver `honeywellSylkDevice` of [Block 88]).

### The device contract — `BISylkDevice` `[CERT]`
Every Sylk device implements this interface (sylk/fw/BISylkDevice.java):

| Method | Line | Role |
|---|---|---|
| `int getSylkAddress()` / `setSylkAddress(int)` | :35/:37 | the device's address on the S-Bus |
| `BSylkDeviceStore compile(SylkLinkTable, int, ISpyderDevice)` | :43 | **emit the binary** for download |
| `boolean compileFailDetectTable(SylkLinkTable)` | :45 | the fail-detect data block |
| `void fullDecompile(BBlob[], BIApplication, LinksInfo, int, BSylkDeviceDescriptor)` | :47 | **read binary back** (upload, full) |
| `void quickDecompile(BBlob[], BIApplication, BSylkDeviceDescriptor)` | :49 | upload, fast path |
| `int getSizeInProxyFile()` / `getHiddenINParamCount()` | :65/:67 | sizing for the Spyder proxy file |

### The Spyder↔Sylk data binding — `SylkLink` / `SylkLinkTable` `[CERT]`
A `SylkLink` (sylk/fw/SylkLink.java) is one PVID-to-PVID mapping between the Spyder and the wall module:
- `getSpyderPVID()` (:30) ↔ `getSylkDevPVID()` (:38) — the two endpoints `[CERT]`.
- direction constants: `DIRECTION_SPY_TO_SYLK_DEV = ">"` (:18), `DIRECTION_SYLK_DEV_TO_SPY = "<"` (:19), `DIRECTION_TWO_WAY` (:20); `PVID_TO_BE_DEFINED = -1` (:21) `[CERT]`.
- well-known time keys (`KEY_TIME_OF_DAY`, `KEY_TIME_HOURS/MINUTES/DAY_OF_MONTH/MONTH/YEAR`, :12–:17) — the Spyder pushes clock to the wall-module LCD.

`SylkLinkTable` (sylk/fw/SylkLinkTable.java:22) is the collection: `private Array a = new Array(SylkLink.class)` (:28), `addLink(SylkLink)` (:34), `clearAllLinks()` (:30), with filter criteria `CRITERIA_DIRECTION/IS_HIDDEN/KEY/SPY_FD/SYLK_DEV_FD` (:23–:27) `[CERT]`. This `SylkLinkTable` is exactly the argument threaded into `compile(...)` and `compileKingfisher*(...)` (§121.5) — it is the contract that says *which Spyder points feed/read which wall-module PVIDs*. This verifies [Block 106] §106.5 ("`SylkLinkTable`/`SylkLink` = mapeo PVID Spyder ↔ PVID dispositivo Sylk").

### The compiled-binary layout — nested `BVector` stores `[CERT]`
The Sylk binary is a tree of Niagara `BVector`s serialized to `BBlob`:
- `BSylkDeviceStore extends BVector` (sylk/fw/BSylkDeviceStore.java:19); its total size is the sum of child `BSylkDeviceFileStore.getFileLengthInBytes()` (:33).
- `BSylkDeviceFileSection extends BVector` (sylk/fw/BSylkDeviceFileSection.java:15) and `BSylkDeviceFileStore extends BVector` (BSylkDeviceFileStore.java:15) — the section/file containers.
- Supporting: `BProxyFileDescriptorStore`, `BProxyFileDeviceData`, `BSylkFileZeroStore`, `BSylkVersionInfo`, `BFileModificationInfo`/`BFileModificationEntry` (the per-section modified-offset bookkeeping that drives incremental download).

This is the *Sylk-device* half of the same `FileSection0..5` model [Block 106] §106.3 described for the Spyder controller — confirmed concretely in the compilers (§121.4).

### The compile vocabulary — `ICompilationConstants` / `IScheduleConstants` `[CERT]`
The numeric grammar of the binary is fixed by two constant interfaces `[CERT]`:
- **Reserved sensor PVIDs** (TR7x): `TEMP_SENSOR_PVID = 8192`, `HUMIDITY_SENSOR_PVID = 8193`, `CO2_SENSOR_PVID = 8194` (compilation/ICompilationConstants.java:13-15), `TIME_OF_DAY_PVID = 49408` (:22); the TR4x family remaps them low: `TR4X_TEMP_SENSOR_PVID = 512`, `TR4X_HUMIDITY_SENSOR_PVID = 513`, `TR4X_CO2_SENSOR_PVID = 514` (:324-326).
- **File / proxy layout**: `FILE_ZERO_SIZE = 14`, `FILE_THREE_SIZE = 16` (:45-46), `PROXY_FILE_DESCRIPTOR_OFFSET = 46`, `PROXY_FILE_DATA_OFFSET = 170` (:56-57) — the byte offsets at which the device's proxy file (on the Spyder side) carries the wall-module descriptor and data.
- **LCD screen types**: `HOME_SCREEN = 1`, `PARAM_VIEW_SCREEN = 2`, `PARAM_EDIT_SCREEN = 3`, `PASSWORD_INPUT_SCREEN = 4` (:101-104) — the same screen taxonomy the state machine drives (§121.6).
- **Scheduling** (TR75/Zio+ only): `SCHEDULE_PARAM_PVID = 65534`, `MAX_EVENTS = 4`, `MAX_DAY_SCHD = 8` (IScheduleConstants.java:7,42,45).

---

## 121.4 — The compiler chain, mapped to the device families `[CERT]`

[Block 106] §106.3 reported the chain `BBasicKFCompiler → BZioEnhCompiler → BZioplusCompiler` as `[CERT-a]`. Verified here, **and pinned to the device generations** — the central refinement of this block.

### The inheritance chain `[CERT]`
```
BBasicKFCompiler (compilation/BBasicKFCompiler.java:106)
  extends BComponent (:107)  implements IKFCompiler, IKFResAdditionCallback (:108-109)
     └─ BZioEnhCompiler (compilation/BZioEnhCompiler.java:66) extends BBasicKFCompiler (:67)
            └─ BZioplusCompiler (compilation/BZioplusCompiler.java:52) extends BZioEnhCompiler (:53)
```
Each subclass *adds stores* for the richer device: `BZioEnhCompiler` adds `objEnumerationStore` / `objEnumerationLabelStore` (:69-70); `BZioplusCompiler` adds `BDayScheduleStore` / `BEventScheduleStore` (:55-56) — i.e. TR75 (Zio+) gains scheduling/calendar that TR70 lacks `[CERT]`.

The `IKFCompiler` contract (compilation/IKFCompiler.java): `BSylkDeviceStore compileKF(BSBusWallModule, SylkLinkTable, int, ISpyderDevice)` (:12), `compileFailDetectTable(SylkLinkTable)` (:14), `cleanup()` (:16) `[CERT]`.

### The dispatch — `BKFCompilerFactory` `[CERT]`
The factory picks the compiler **by the selected model's ordinal** (compilation/BKFCompilerFactory.java:33-49) `[CERT]`:

| Model ordinals | → Compiler | Family |
|---|---|---|
| 9, 10 (`TR_70`, `TR_70H`) | `BBasicKFCompiler` (:39) | TR7x Zio |
| 11, 12 (`TR_71`, `TR_71H`) | `BZioEnhCompiler` (:41) | TR7x Zio Enh |
| 13, 14 (`TR_75`, `TR_75H`) | `BZioplusCompiler` (:43) | TR7x Zio+ |
| 1, 2, 3, 4 (TR40 family) | `BTr40Compiler` (:45) | TR4x |
| 5, 6, 7, 8 (TR42 family) | `BTr42Compiler` (:47) | TR4x |

So the **Basic→ZioEnh→Zio+ inheritance chain is exactly the TR70→TR71→TR75 capability ladder** `[CERT]` — each step inherits the previous device's binary layout and adds the new device's extra data blocks. The **TR4x family is a separate, flatter lineage**: `BTr40Compiler extends BComponent` (tr4x/compilation/BTr40Compiler.java:64-65) and `BTr42Compiler extends BComponent` (tr4x/compilation/BTr42Compiler.java:132-133) — they do NOT inherit from the KF chain `[CERT]`. The decompile (upload) side mirrors it: `BKFDeCompilerFactory`, `BZioEnhDeCompiler`, `BZioplusDeCompiler`, `BTr40Decompiler`, `BTr42Decompiler`.

### The sectioned output + CRC `[CERT]`
Both lineages emit the same `FileSection`-keyed `BSylkDeviceStore`. `BTr40Compiler` builds `FileSection0` from a `BKFFileSectionZeroStore` holding the proxy-file `descriptor` (tr4x/compilation/BTr40Compiler.java:139-141), then `FileSection1` = `BTr4xFile1Store` carrying a `BTr4xFailDetectTable` (:93-94) `[CERT]`. The `BBasicKFCompiler` path CRCs each section: `char c2 = this.objChecksumGenerator.calculateCRCChecksum(byArray2, byArray2.length); bKFFileSectionOneStore.setChecksum(c2)` (compilation/BBasicKFCompiler.java:1393-1394), same for section two (:1448-1449) and the file header (:1503) `[CERT]`. `objChecksumGenerator` is `BChecksumGenerator` from `xl10Controller` (imported :83, field :130) — the **same CRC-16 generator** [Block 106] §106.3 verified for the Spyder controller. **No crypto** appears anywhere in `kingfisher/**` or `sylk/**` (grep `MessageDigest|java.security.Signature|javax.crypto|Cipher` = 0 files) `[CERT]`.

The `tr4x/stores/` package (68 classes) is the TR4x binary's data-block vocabulary `[CERT]`: `BTr4xCategoriesTable`/`...Entry`, `BTr4xCategoryItemsTable`, `BTr4xCompositePVIDTable`, `BTr4xDynamicActionsTable`/`BTr4xDynamicObjTable`, `BTr4xEnumerationsTable`, `BTr4xFailDetectTable`, `BTr4xLabelsTable`/`BTr4xLabelsDataBlockTable`, plus `BTr4xFile1Store`/`BTr4xFile3Store`/`BTr40File2Store`/`BTr42File2Store` — i.e. the categories/menu, PVID composites, dynamic actions/objects, enumerations, fail-detect and LCD labels that make up a TR4x screen, each emitted as a numbered file section.

---

## 121.5 — Resource limits, accounting, and the tie to the FB-engine compile `[CERT]`

### Per-device capacity ceilings `[CERT]`
`resourceLimits/` enforces what fits on each wall module. `getTotalFileSize()` and `getInParamaterLimit()` grow monotonically across the TR7x ladder `[CERT]`:

| Device | `getTotalFileSize()` | `getInParamaterLimit()` | Source |
|---|---|---|---|
| TR70 | 1020 | 30 | resourceLimits/BTR70ResourceLimits.java:71,76 |
| TR71 | 2000 | 250 | resourceLimits/BTR71ResourceLimits.java:71,76 |
| TR75 | 4950 | 250 | resourceLimits/BTR75ResourceLimits.java:71,76 |

`getFileSize(section, …)` returns per-section byte budgets via a switch over the section index, adjusted by booleans (e.g. TR70 §1 base 900, −22 if a flag, −40 if another; section 0 = 14-byte header; −4 trailer) — `resourceLimits/BTR70ResourceLimits.java:25-71` `[CERT]`. The per-resource ceilings also climb with the family and explain the compiler-chain split `[CERT]`: TR70 allows `getInParamaterLimit()=30` / `getOutParameterLimit()=19` / 10 in-out / 3 sensor / 96 PVID-send-group indices and **0 schedule PVIDs** (BTR70ResourceLimits.java:78,83,88,93,103,111-style); TR75 raises in/out params to 250/255 and crucially `getSchedulePVIDsLimit()=48` (BTR75ResourceLimits.java:110-111) — i.e. only the Zio+ device has a scheduler, which is exactly why `BZioplusCompiler` (and only it) adds `BDayScheduleStore`/`BEventScheduleStore` (§121.4). `ResourceLimitsFactory` selects the limits object by model. At runtime `resourceCounter/ZioResourceCounter` + `BKFResourceCounter` tally actual usage against these ceilings (and `sylk/fw/SylkResourceCounter`/`FixedSylkResourceCounter`/`SylkIODeviceResourceCounter` do the same on the Sylk side) `[CERT]`.

### Where the FB engine drives it `[CERT]`
The Spyder controller compiler reaches into Kingfisher at compile time — `BSpyderIICompilation` (the `BCompilation` subclass of [Block 106] §106.3) calls `compileKingfisherOutputs((BHoneywellComponent)…, sylkLinkTable, n3)` (xl10Controller/compilation/BSpyderIICompilation.java:378, def :450) and `compileKingfisherInputs(BISylkDevice, sylkLinkTable)` (:388, def :581) `[CERT]`. This verifies the exact hook [Block 106] §106.1 named (`BSpyderIICompilation.compileKingfisherInputs()/compileKingfisherOutputs()`). The flow: the Spyder app compile walks its wall-module FBs, hands each a `SylkLinkTable`, and the device's `IKFCompiler`/`BISylkDevice.compile()` emits its `BSylkDeviceStore` to be embedded in the Spyder's own download payload ([Block 120]).

---

## 121.6 — The LCD / HMI state machine: simulating the wall module on the PC `[CERT]`

`simulation/BKFStateMachine` is the **3066-line** simulation of the TR wall-module LCD + keypad (wc -l = 3066; class `extends`… `implements BIKFStateMachine`, simulation/BKFStateMachine.java:57-59) `[CERT]`. It lets the engineer drive the real HMI navigation on screen before download — the HMI analogue of [Block 106] §106.4's `BSimulationPid` (the FB-math simulator).

> **doc↔code delta `[CERT]`**: [Block 106] §106.4 estimated `BKFStateMachine` at "~1800 líneas" (`[CERT-a]`). It is **3066 lines**. The estimate was low; the class is larger and is the single biggest file in the Kingfisher subsystem.

The HMI contract `BIKFStateMachine` (simulation/BIKFStateMachine.java) is the 5-button keypad + navigation `[CERT]`: `dispLeftKeyPress()` (:18), `dispRightKeyPress()` (:20), `dispMiddleKeyPress()` (:22), `dispUpKeyPress()` (:24), `dispDownKeyPress()` (:26), `dispSelectedViewOption(int)` (:28), `powerUpNavigation()` (:30), `setKFConfigObject(BBasicKFCompiler)` (:32), `setKFDisplayObject(BIKFDisplay)` (:34). `BKFStateMachine` implements them as the navigation tree: `powerUpNavigation()` (:~250), `dispSetHomeScreen()` (:1246), `dispHomeScreen(int,int,int)` (:1267), `dispCategoryScreen(int)` (:1311), and large key-press case machines (`dispLeftKeyPress` :368, `dispMiddleKeyPress` :622, `dispRightKeyPress` :922) `[CERT]`.

The pixel-level LCD is modeled in `simulation/BLcdUtils` `[CERT]`: a `static int[] bitSetArray = new int[168]` LCD buffer (:33), `MAX_DATA_LEN = 20` (:30), `FL_INVALID = 0x7F7FFFFE` (:32), with 7-segment and 10-segment character renderers `dispLabel7`/`dispLabel10` (:81/:39), `UpdateAlpha/DigitTenSeg` (:109/:175), `UpdateAlpha/DigitSevenSeg` (:199/:244), and `setDisplaySegments`/`clrDisplaySegment`/`clearLCDBuffer` (:285/:292/:275). The named segments live in `simulation/BSegmentEnums` — the actual TR display layout: `SEG_FAN_SYMBOL`, `SEG_FAN_POSITION1..3`, `SEG_FAN_ON/AUTO/OFF`, `SEG_TEMPERATURE`, `SEG_STR10_H1..H10`, `SEG_PREV`, `SEG_SET_HOME_SCREEN` (BSegmentEnums.java:17-36) `[CERT]`. Supporting sim classes: `BKFCoarseTimerEnum` (screen timers), `BKFCategoryConfigInfo`/`BKFParameterConfigInfo`, `BUtilEnums` (`FanType`, `OverrideValues`, `PosIndex`, `UpdateDirection`), `BFtoa`, `BUIHelper`.

> **Architectural read `[INFER]`**: the tool carries a *full software re-implementation of the wall-module firmware HMI* (segment map, key navigation, screen timers) purely to preview it — exactly the same "algorithm-in-Java-only-for-simulation" pattern [Block 106] §106.4 found for the FB math. The shipped binary contains data (labels, categories, PVID links), not this navigation code.

---

## 121.7 — The configuration datatypes (`kingfisher/datatypes/`) `[CERT]`

The 62-class `datatypes/` package is the Java model of everything the engineer configures on a wall module — each a `BVector`/`BKFStore` whose fields use Honeywell `BCustomeElementType` facets (u8/u16/f32) that fix the on-wire width `[CERT]`:

| Type | Holds | Source |
|---|---|---|
| `BParameter` | a PVID — `Property PVID` u16 range 0–65535 (`getPVID()/setPVID()`) | datatypes/BParameter.java:23,52-57 |
| `BPVIDGroupTable` / `BPVIDSendTable` (+ `…Store`) | the S-Bus PVID group / send mapping tables | datatypes/BPVIDGroupTable.java, BPVIDSendTable.java |
| `BSensorCalibration` | `tempCalibration`/`humidityCalibration`/`co2Calibration` (f32) | datatypes/BSensorCalibration.java:25-27 |
| `BWMConfig` | wall-module config — e.g. `currentHomeScreen` (u8 0–255), `extends BKFStore` | datatypes/BWMConfig.java:24-26 |
| `BTopAlphaNumericAlertLabel` / `BMiddleAlphaNumericAlertLabel` (+ stores) | LCD alert label strings (top/middle line) | datatypes/B*AlertLabel*.java |
| `BEnumerationLabel` / `BEnumerationLabelStore` / `BEnumSchdLabels` | enum + schedule label strings for the LCD | datatypes/BEnumeration*.java |
| `BFailDetectTable` / `BHomeScreenTimeOutTableStore` | fail-detect + home-screen timeout data blocks | datatypes/BFailDetectTable.java, BHomeScreenTimeOutTableStore.java |
| `BToolConfig` | tool-side config | datatypes/BToolConfig.java |

This verifies [Block 106] §106.5's `[CERT-a]` ("Kingfisher añade `BParameter` (PVID), `BPVIDGroupTable/SendTable`, `BKFLabel`, `BSensorCalibration`"); the label class is realized as the `…AlphaNumericAlertLabel` / `BEnumerationLabel` family rather than a single `BKFLabel` (minor naming delta, §121.9).

---

## 121.8 — Integrity / security posture (Kingfisher path) `[CERT]`

- **[SECURITY CERT]** The wall-module binary is protected by **CRC-16 only, no cryptographic signature** — `BChecksumGenerator.calculateCRCChecksum(...)` per file section (compilation/BBasicKFCompiler.java:1393-1394, 1448-1449, 1503), and **zero** `MessageDigest`/`Signature`/`Cipher`/`javax.crypto` references across `kingfisher/**` + `sylk/**` (grep = 0 files). This extends [Block 106] §106.3's finding to the **TR wall-module** payload specifically and to its **S-Bus transport** ([Block 88]): anyone who can speak the Sylk download protocol and recompute the CRC can inject arbitrary wall-module configuration/labels/links.
- **Debug-to-stdout in production `[CERT]`**: **54** `System.out.println` calls remain in `kingfisher/**` (concentrated in `resourceCounter/ZioResourceCounter` dumping `file0Size`/`file1Size`/`file2Size` at :1143-1145) — same leak class [Block 106] §106.6 flagged, now localized to the Kingfisher resource counter.
- **No native/exec/JNI** in the Kingfisher or Sylk path (consistent with [Block 106] §106.6).

---

## 121.9 — doc↔code deltas (honest)

1. **`BKFStateMachine` size**: [Block 106] said "~1800 líneas" `[CERT-a]`; actual **3066** `[CERT]` (§121.6). Estimate corrected upward.
2. **Class counts**: the prompt/[Block 106] cited "`kingfisher/tr4x` ~101 cls" and "`sylk/fw` ~39". Measured: `kingfisher/tr4x` recursive = **114** `.java` (stores 68 + ui 39 + compilation 5 + ui/actions 2); `sylk/fw` = **28** direct + **11** in `sylk/fw/ti` = **39** `[CERT]`. The tr4x figure was low (UI included); the sylk figure matches once `ti/` is counted.
3. **Label datatype name**: [Block 106] §106.5 named `BKFLabel`; the actual classes are `B{Top,Middle}AlphaNumericAlertLabel` + `BEnumerationLabel`/`BEnumSchdLabels` (§121.7). Same role (LCD strings), different class names — no `BKFLabel.java` exists in `datatypes/` `[CERT]`.
4. **Compiler-chain semantics sharpened (not a contradiction)**: [Block 106] listed the chain generically as "wall modules Kingfisher"; this block pins each link to a specific TR7x device and shows TR4x uses a *separate* lineage (§121.4). This is a refinement that escalates the prior `[CERT-a]` to `[CERT]`.

No outright contradictions of [Block 106]'s claims were found; every `[CERT-a]` checked (compiler chain, `compileKingfisher*` hook, `SylkLinkTable` mapping, `BZioTIDevice` bandwidth, CRC-16/no-signature, TASO-family naming, stdout debug) **held and escalated to `[CERT]`**.

---

## 121.10 — Connections

- **[Block 106]** (`honeywellSpyderTool` core) — this block **verifies and escalates** B106's Kingfisher/Sylk `[CERT-a]`: the `BBasicKFCompiler→BZioEnhCompiler→BZioplusCompiler` chain (now mapped to TR70/71/75), `compileKingfisherInputs/Outputs` (call sites pinned), `BKFStateMachine` (size corrected), `BISylkDevice`/`SylkLinkTable`, `BZioTIDevice` bandwidth, and CRC-16-only/no-signature. It is the deep-dive of B106 §106.5's "Sylk / S-Bus tool-side".
- **[Block 119]** (Kingfisher *UI/wizard*) — the **model + compiler behind that wizard**. B119's `kingfisher/ui` + `tr4x/ui` step-wizard edits the `BKFConfig`/`datatypes` described here (§121.1, §121.7) and, on finish, triggers the `BKFCompilerFactory`→`compile()` path (§121.4). B119 = how the engineer drives it; B121 = what it builds and emits.
- **[Block 116]** (`docHoneywellSpyder`) — the vendor doc's TR wall-module operator/HMI reference is the `[CERT-doc]` counterpart to the code-side LCD model here (§121.6).
- **[Block 120]** (driver wire protocol) — the `BSylkDeviceStore`/`FileSection` binary this subsystem compiles (§121.3-4) is the payload B120 ships to the controller (BACnet AtomicWriteFile / LON file-transfer); the wall-module sections ride inside the Spyder's own download via `compileKingfisher*` (§121.5).
- **[Block 88]** (`honeywellSylkDevice`) — the **runtime** S-Bus driver (28 wall modules, Nano-over-BACnet host). This is the **tool-side** twin: B88 talks to the wall module on the live bus; B121 models/compiles/decompiles its binary and simulates its LCD offline. The split B106 §106.5 asserted (`sylk/fw` = tool-side vs `honeywellSylkDevice` = runtime) is confirmed here `[CERT]`.

---

## 121.11 — Pending (not distilled here)

- The exhaustive field-by-field byte layout of each `tr4x/stores` data block (categories/composite-PVID/dynamic-actions wire formats) — readable but voluminous; would only deepen §121.4.
- `kingfisher/uivalidations` (98 cls actions+validations) and `kingfisher/ui/uiframework` belong to the **UI layer ([Block 119])**, intentionally excluded here.
- Live confirmation of a compiled wall-module binary on real TR70/TR75 hardware → DYNAMIC phase (with [Block 88]'s runtime driver), if hardware appears.
