# Block 115 — `spyderToIrmNxMigrator`: the offline Spyder XL10 → IRM Nx application migrator (BOG-to-BOG, the FB transpiler that bridges [Block 106] → [Block 105]), deobfuscated

> **Language note**: this block is written in **English** (standing corpus-language preference for new blocks), even though Blocks 1–114 of this corpus are in Spanish. Cross-references to prior Spanish blocks are kept by number.
>
> Empirical research of the Honeywell OEM module **`spyderToIrmNxMigrator`** (`spyderToIrmNxMigrator-wb`, package `com.honeywell.irmnx.migrator`, 21 `.java` / ~13,820 LOC). `module.xml`: *"Spyder to IRM Application Migrator"*, symbol `stim`, vendor Honeywell `4.14.0.3.2.0.6`, build **2025-01** (`buildMillis=1736497733784`), **`runtimeProfile=wb`** (Workbench-only engineering tool — it does NOT run on the JACE).
>
> **What it is**: a one-way **transpiler** that takes a programmed **Honeywell Spyder (XL10 NextGen)** control application and rewrites it as an equivalent **IRM (BEATS Nano)** application — function block by function block, link by link — producing a Niagara `.bog`. It is the missing bridge between the two FB engines documented separately: the **source** is [Block 106] (`honeywellSpyderTool` / XL10NextGen), the **target** is [Block 105] (`honIrmControl` / IRM Nano).
>
> **Sibling of [Block 100]** (`ipcMigrator`): same source family (Spyder XL10), different target. B100 migrates Spyder → **IPC 3036**; this migrates Spyder → **IRM Nx (BEATS)**.
>
> Sources (deobfuscated, READ-ONLY, ZKM strings decrypted, names clean):
> `/home/cristian/modules/Prototipos/modulos/organized/spyderToIrmNxMigrator/spyderToIrmNxMigrator-wb/vineflower/com/honeywell/irmnx/migrator/...`
> + bundled BOG templates under `.../spyderToIrmNxMigrator-wb/{vineflower,extracted}/template/bog/{stations,devices,libraries}/`.
> Method: direct reading of the migrator pipeline, the 45-branch FB dispatch, and per-block constructor targets (verified by `grep` of the `new com.honeywell.irm.*` constructors). `[CERT]` = verified verbatim by me in the cited file:line; `[CERT-a]` = mapping detail read but not exhaustively re-confirmed pin-by-pin; `[INFER]` = deduction.
>
> Layer 22 (deobfuscated OEM). **Strong connections**: [Block 106] (source FB engine — and this block **partially closes B106's pending #1**: the full XL10NextGen FB catalog), [Block 105] (target FB engine — IRM Nano taxonomy), [Block 100] (`ipcMigrator`, the sibling Spyder→IPC migrator), [Block 77] (Spyder BACnet driver — dependency), [Block 88] (Sylk wall modules — `SylkDeviceMigrator`), [Block 94] (Device Manager — dependency).

---

## 115.1 — What it is and how it runs `[CERT]`

`spyderToIrmNxMigrator` is a **Workbench tool** (`runtimeProfile="wb"`, `module.xml`) — a `BWbNavNodeTool` named `SpyderToIrmNxMigrator` (`migrator/ui/BSpyderToIrmNxMigrator.java:50`). The engineer invokes it from the Workbench nav tree; it opens a dialog (`BMigratorDialog`) where they pick a **migration source type** and a source/output directory `[CERT, BSpyderToIrmNxMigrator.java:239-246]`:

| Source type | Lexicon key | What it reads |
|---|---|---|
| Station | `migrator.bog.type.station` | a station folder containing `config.bog` (`checkIfStation`, `:364-378`) |
| Library | `migrator.bog.type.library` | a Spyder library dir (`UserDefined/` + `CommonObjects/Attachments` + `index.xml`, `:466-484`) |
| Exported Library | `migrator.bog.type.exported.library` | a `.slb` file (a zip, unpacked by `UnzipUtil`, `:432`) |
| Palette | `migrator.bog.type.palette` | a `.palette` file (`:309-310`) |

This is an **offline, file-to-file** tool: it never talks to a live controller. It decodes the **source Spyder application from `.bog`** and writes a **new IRM application `.bog`** into the output directory, alongside a human-readable `MigrationReport.txt` `[CERT, ui/MigratingJob.java:284]`. The actual work runs inside a `MigratingJob extends BSimpleJob` (`BSpyderToIrmNxMigrator.java:272-275`).

**Dependencies** (`module.xml`) tie the two engines together: `honeywellSpyderTool` + `honeywellBacnetSpyder` (the SOURCE, [Block 106]/[Block 77]), `honIrmControl-rt` + `honIrmConfig-rt` (the TARGET, [Block 105]), `honeywellSylkDevice-rt` ([Block 88]), `honeywellBacnetDeviceManager-rt` ([Block 94]), plus Tridium `control-rt`, `bacnet-rt`, `kitControl-rt`, `schedule-rt`, `bql-rt`.

---

## 115.2 — The 7-phase migration pipeline `[CERT]`

The heart is `IrmNxMigrator.migrate()` (`migrator/IrmNxMigrator.java:77-166`). Per source Spyder device it instantiates a fresh **`BIrmBacnetDevice`** target and runs a fixed pipeline. Order matters — **points and folders are created first, links last**, because links can only be rewired once every endpoint exists:

| # | Phase | Class | Pass | Citation |
|---|---|---|---|---|
| 1 | Software points (create) | `SoftwarePointMigrator` | `create=true, links=false` | `IrmNxMigrator.java:112` |
| 2 | Physical points (create) | `PhysicalPointMigrator` | create | `:118` |
| 3 | **Function blocks** | `FunctionBlockMigrator` | — | `:123` |
| 4 | Software points (links) | `SoftwarePointMigrator` | `create=false, links=true` | `:132` |
| 5 | Physical points (links) | `PhysicalPointMigrator` | links | `:137` |
| 6 | Sylk FBs (wall modules) | `SylkDeviceMigrator.migrateSylkFunctionBlocks` | — | `:143` |
| 7 | **Links** | `LinkMigrator` | — | `:147` |

Post-processing once links succeed `[CERT, :149-151]`: `SylkDeviceMigrator.handlePostMigrate(...)`, `reorderCompositedSlots(...)` (re-imposes the source slot order on the target composited folders), and `updateResourceCount()`.

**Target structure** `[CERT, :92-98]`: the migrator stamps `applicationType = Radix32.generateRandom()` and builds, under the IRM device's `IrmProgram → "Periodic program"`, one `BIrmFolder` per migrated device. `updateResourceCount()` (`:285-316`) then recomputes the IRM control manager's tallies with **BQL `COUNT(*)`** queries: `NumberOfFunctionBlocks/Folders/Links` (from `IrmControlCounter`) plus per-type BACnet object counts (`AI/AO/AV/BI/BO/BV/MO/MV`, calendar, schedule) — e.g. `setBacnetAICount(getComponentCount(BBacnetNumericInput.TYPE))`.

---

## 115.3 — The FB transpiler: 45-block dispatch and the verified source→target map `[CERT]`

`FunctionBlockMigrator` (5,660 LOC) is the value core. `migrate()` walks the Spyder `ControlProgram` (a `BApplicationLogic`) in **three passes** over `migrateFolder()` — input pass-thrus, then real FBs (`skipPassThrus`), then output pass-thrus `[CERT, FunctionBlockMigrator.java:148-168, 208-256]`. Folders (`BMacro`/`BApplicationLogic`) recurse; `BWsTextBlock` annotations migrate too.

Each real block hits `addMigratedFunctionBlock()` — a **45-branch `instanceof` dispatch** `[CERT, :284-376]` — one branch per Spyder FB type, each calling a dedicated `addMigratedX`. The target constructor of each branch was verified (`new com.honeywell.irm.*`). The complete mapping (Spyder source FB → IRM Nano target FB):

| Spyder FB (`honeywellXL10NextGen.functionalBlocks.blocks.*`) | → IRM Nano target (`com.honeywell.irm[nano].*`) | Citation (file:line) |
|---|---|---|
| `logic.BAnd` | `irm.logic.BAnd` | `:429` |
| `logic.BOr` | `irm.logic.BOr` | `:339` dispatch |
| `logic.BXor` | `irm.logic.BXor` | `:373` / `:620` |
| `logic.BOneShot` | `irm.timer.BOneShot` | `:337` |
| `control.BPid` | `irm.controlLoop.BPid` | `:2144` |
| `control.BAia` | `irm.controlLoop.BAia` | `:5274` |
| `control.BRateLimit` | `irm.timer.BRateLimit` | `:349` |
| `control.BStager` | `irm.vav.BStager` | `:1116` |
| `control.BStageDriver` | `irm.vav.BStageDriver` | `:1217` |
| `control.BCycler` | `irm.vav.BCycler` | `:4666` |
| `control.BFlowControl` | `irm.vav.BFlowControl` | `:4255` |
| `math.BAdd` | `irm.arithmetic.BAdd` | `:317` |
| `math.BSubtract` | `irm.arithmetic.BSubtract` | `:329` |
| `math.BMultiply` | `irm.arithmetic.BMultiply` | `:325` |
| `math.BDivide` | `irm.arithmetic.BDivide` | `:319` |
| `math.BExponential` | `irm.arithmetic.BExponential` | `:321` |
| `math.BLimit` | `irm.arithmetic.BLimit` | `:315` |
| `math.BReset` | `irm.arithmetic.BReset` | `:353` |
| `math.BLogarithm` | `irm.arithmetic.BMathOperation` | `:3456` |
| `math.BRatio` | `irm.arithmetic.BLinearGraph` | `:1666` |
| `math.BSquareRoot` | **`BIrmSubFolder`** (decomposed) | `:3224` |
| `math.BEnthalpy` | `irm.arithmetic.BPsychrometric` (slots remapped) | `:4487` |
| `math.BDigitalFilter` | `irm.vav.BDigitalFilter` | `:4593` |
| `math.BFlowVelocity` | `irm.vav.BFlowVelocity` | `:4375` |
| `analog.BAverage` | `irm.arithmetic.BAggregation` | `:5020` |
| `analog.BMaximum` | `irm.arithmetic.BAggregation` | `:3004` |
| `analog.BMinimum` | `irm.arithmetic.BAggregation` | `:2895` |
| `analog.BCompare` | `irm.comparison.BCompare` | `:295` |
| `analog.BSelect` | `irm.selectswitch.BNumericSelect` | `:1344` |
| `analog.BSwitch` | `irm.selectswitch.BNumericSwitch` | `:1016` |
| `analog.BPrioritySelect` | `irm.selectswitch.BBinarySelectPrio` | `:1973` |
| `analog.BAnalogLatch` | `irm.selectswitch.BBinarySelect` | `:5136` |
| `analog.BHystereticRelay` | `irm.outputs.BStg123Outp` | `:4016` |
| `dataFunction.BAlarm` | `irm.vav.BAlarm` | `:5497` |
| `dataFunction.BCounter` | `irm.vav.BCounter` | `:4771` |
| `dataFunction.BRunTimeAccumulate` | `irm.vav.BRunTimeAccumulate` | `:1405` |
| `dataFunction.BOverride` | **`BIrmSubFolder`** (decomposed) | `:2379` |
| `dataFunction.BPriorityOverride` | **`BIrmSubFolder`** (decomposed) | `:2077` |
| `zoneControl.BOccupancyArbitrator` | `irm.vav.BOccupancyArbitrator` | `:2819` |
| `zoneControl.BGeneralSetpointCalculator` | `irm.vav.BGeneralSetpointCalculator` | `:4149` |
| `zoneControl.BSetTemperatureMode` | `irm.vav.BSetTemperatureMode` | `:1253` |
| `zoneControl.BTemperatureSetpointCalculator` | `irm.vav.BTemperatureSetpointCalculator` | `:856` |
| `builtIn.BSchedule` | (delegated to `ScheduleBlockMigrator`) | `:357` |
| `builtIn.BWallModule` | (delegated to `SylkDeviceMigrator`) | `:371`, `:799` |
| `util.BPassThru` | `irm.util.BPassThru` | `:404` |

> **Architectural reading `[CERT]`**: this is NOT a generic graph copy — it is a **hand-written, per-block transpiler** (one method per FB type). Most blocks map **1:1** with a same-named IRM block (And→And, Pid→Pid, FlowControl→FlowControl), but the mapping is **lossy/structural in several cases**: `BAverage`/`BMaximum`/`BMinimum` all collapse onto a single configurable `BAggregation`; `BEnthalpy` becomes `BPsychrometric` with explicit pin renaming (`t→Temperature`, `rth→RelHumidity`, `Y→Out`, `:4502-4514`); `BLogarithm`→`BMathOperation`, `BRatio`→`BLinearGraph`; and `BSquareRoot`, `BOverride`, `BPriorityOverride` have **no single IRM equivalent** so the migrator expands each into a `BIrmSubFolder` containing several primitive IRM blocks wired together.

---

## 115.4 — Mapping mechanics: OutSave, wiresheet geometry, link rewiring, constant decomposition `[CERT]`

Every `addMigratedX` repeats the same four-part discipline (verified across `addMigratedAnd :425-433`, `addMigratedPid :2140-2199`, `addMigratedEnthalpy :4484-4515`):

1. **OutSave carry-over** `[CERT]`. The Spyder marks a block output as "save to permanent memory" with a slot flag; the migrator detects it via `isOutSaveSet(comp, slot)` = `(getFlags(slot) & 0x20000000) > 0` `[CERT, :395-399]`, then recreates it on the IRM block with `irmFb.getOutSave().createOutSaveFields(true, new OutSaveFieldDefinition(slot, isOutSaveSupported && isOutSaveEnabled))`. The carry-over is **gated by `isOutSaveSupported`** — a per-target capability flag passed in the migration context (`IrmNxMigrator` ctor / `KEY_IS_OUT_SAVE_SUPPORTED`, `:63,69`). This is the direct tie to the EEPROM-wear limit in §115.5.
2. **Wiresheet geometry** `[CERT]`. The source `wsAnnotation` (x/y position) is preserved and re-laid-out through `FBPositionCalculator` (`:402-410`, `:4493-4498`) so the migrated wiresheet looks like the original.
3. **Slot-map for the link phase** `[CERT]`. Because slot names differ between engines, each mapping records old→new endpoints in a `HashMap<ComponentSlotMapEntity,ComponentSlotMapEntity>` (`componentSlotMigrationMap`), keyed by folder/component/slot/direction (`ComponentSlotMapEntity.java`; e.g. PassThru `:413-422`, Enthalpy `:4500-4514`). `LinkMigrator` (phase 7) consumes this map to rewire every Spyder link onto the correct IRM pin. Before migrating, `activateLinksInSpyderApplication()` force-activates all source links so they are visible (`:170-179`).
4. **Constant-input decomposition** `[CERT]`. If a Spyder block has constant (un-linked) inputs, the migrator wraps the target in a `BIrmSubFolder` and injects IRM constant blocks (`BConst1Numeric`/`BConst2Numeric`/`BConst5Numeric`) linked into the corresponding pins — e.g. `addMigratedPid` builds a subfolder and adds a `BConst2Numeric` feeding `BPid.ControlledValue` when `sensor` is constant (`:2160-2198`). A single source FB can thus expand into a small subgraph.

---

## 115.5 — Hard constraints and the LON exclusion `[CERT]`

`MigrationConstants.java` encodes the **target controller's physical limits** that the migrator must respect `[CERT, MigrationConstants.java:11-13]`:

| Constant | Value | Meaning |
|---|---|---|
| `MAX_IRM_FOLDER_COUNT` | **32** | max top-level `BIrmFolder`s before overflow |
| `MAX_IRM_FOLDER_DEPTH` | **5** | max folder nesting depth |
| `SAVE_PERMANENT_MAX_WRITE_PER_WEEK` | **150** | EEPROM write budget for persisted (OutSave) outputs |

The folder caps are enforced live in `getFolderComponent()` `[CERT, FunctionBlockMigrator.java:194-206]`: once `folderCount >= 32` **or** `isTargetFolderDepthAllowed(path)` is false (path has ≥5 segments, `IrmNxMigrator.java:347-350`), it stops creating `BIrmFolder`s and emits `BIrmSubFolder`s instead, logging *"Exceeded either IrmFolder count of 32 or IrmFolder Depth of 5, creating SubFolder"*. The `SAVE_PERMANENT` budget connects to the OutSave carry-over of §115.4 and the IRM `BSavePermanent` block — the IRM's persistent outputs are wear-limited, which is why OutSave is capability-gated rather than copied blindly.

> **LON Spyder is NOT migratable `[CERT, ui/MigratingJob.java:292-295]`**: the device loop explicitly checks `spyDev.getType().getTypeSpec().equals(BTypeSpec.make("honeywellLonSpyder:LonSpyder"))` and, if so, records `cant.migrate.lon.spyder` and skips it. Only **BACnet** Spyder applications convert to IRM. This is consistent with the IRM/BEATS target being a BACnet device ([Block 105]/[Block 88]).

---

## 115.6 — The offline file flow and the bundled BOG templates `[CERT]`

The module **ships skeleton IRM artifacts as BOG resources** and clones them per migration `[CERT, ui/MigratingJob.java]`:

- `module://spyderToIrmNxMigrator/template/bog/stations/IrmStationTemplate.bog` — an empty IRM **station** (decoded into a `BStation` with a `Drivers/BacnetNetwork`) (`:286-290`).
- `module://spyderToIrmNxMigrator/template/bog/devices/IrmBacnetDevice.bog` — a blank **`BIrmBacnetDevice`**, instantiated once per Spyder device (`:353-357`, also `:657`, `:765`, `:807` for library/palette paths).
- `module://spyderToIrmNxMigrator/template/bog/libraries/IrmLibTemplate.bog` — the IRM **library** skeleton (`:538`); plus prebuilt wall-module libs `TR7770_TR20.bog`, `T7770s_TR20_Wallmodule.bog`.

**Station flow** (`migrateStation`, `:286-322`): decode `IrmStationTemplate.bog` → for each Spyder device, `attachControlProgram()` loads its logic from `<station>/shared/virtualXL10/<name>.bog` (`:325-349`) and adds it as the device's `ControlProgram` slot → `migrateDevice()` clones a fresh `BIrmBacnetDevice` from the template, copies the source device into a scratch `BComponentSpace` via `Mark.copyTo` (`:362-364`), runs `IrmNxMigrator.migrate()`, and attaches it under the IRM BACnet network → finally the whole `BStation` is re-serialized to `outputDir/config.bog` with `ValueDocEncoder` using facet `skipEncodingSensitive=true` (`:314-318`). The per-device outcome (migrated / failed / control-program-not-found / LON-not-supported) is written to `MigrationReport.txt`.

> The Spyder source application lives as a **virtual XL10 component** (`shared/virtualXL10/*.bog`) — confirming [Block 106]'s model where the XL10NextGen application is a portable, serializable component tree, and [Block 77]'s `getVirtualFileName()` on the device.

---

## 115.7 — Quality / security notes `[CERT-a]`

- **No crypto, no native, no exec**: the module is pure Niagara/Java BOG manipulation — no `loadLibrary`/JNI, no `Cipher`/`MessageDigest`, no `Runtime.exec()`. It is an offline file transformer; its trust surface is the input `.bog` and the bundled templates.
- **Stack traces to stdout**: the top-level `migrate()` catch does `var.printStackTrace()` and dumps the exception class/message into the migration report (`IrmNxMigrator.java:156-159`) — verbose but not a security issue (engineering tool).
- **Lossy-by-design**: blocks with no IRM equivalent are reconstructed from primitives (§115.3); the `MigrationReport.txt` is the only record of what was approximated. A migrated application is **not guaranteed bit-equivalent** to the source — it must be re-validated on the IRM. (This is `requires-execution` to confirm — see gap-backlog.)

---

## 115.x — Connections

- **[Block 106]** (`honeywellSpyderTool` / XL10NextGen) — the **SOURCE engine**. This migrator imports its FB classes directly (`com.honeywell.honeywellXL10NextGen.functionalBlocks.blocks.{logic,math,analog,control,dataFunction,zoneControl,builtIn,util}`, `BApplicationLogic`, `BMacro`). The 45-entry table in §115.3 **partially closes B106's pending item #1** ("full Function-Block catalog"): it enumerates the Spyder FB palette by category as actually consumed by a real client.
- **[Block 105]** (`honIrmControl` / IRM Nano) — the **TARGET engine**. The right column of §115.3 is the IRM Nano FB taxonomy (`irm.{arithmetic,comparison,controlLoop,logic,selectswitch,timer,util,vav,outputs}`, `irmnano.fbfactory.BNanoFunctionBlock`, `irmnano.manager.BIrmBacnetDevice/BIrmFolder`). Confirms B105's "≈163 function blocks on the IRM Nano engine" from the migrator's consumer side.
- **[Block 100]** (`ipcMigrator`) — the **sibling migrator**: Spyder XL10 → IPC 3036. Same source family, different target. Together they show Honeywell built **per-target transpilers** off the common XL10 source rather than one universal exporter.
- **[Block 77]** (Spyder BACnet driver) — dependency `honeywellBacnetSpyder`; supplies `ISpyderDevice`, `BTerminalDetails`, `getVirtualFileName()`, and the BACnet-only constraint (§115.5).
- **[Block 88]** (`honeywellSylkDevice`) — `SylkDeviceMigrator` (phase 6) + `addMigratedWallModule` migrate the Sylk wall-module layer; bundled `TR7770_TR20` / `T7770s_TR20` libraries.
- **[Block 94]** (`honeywellBacnetDeviceManager`) — declared dependency, used while standing up the target IRM BACnet device.
