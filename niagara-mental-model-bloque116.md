# Block 116 — `docHoneywellSpyder`: the bundled SpyderTool help module — the OFFICIAL operator + Function-Block I/O reference (the `[CERT-doc]` source that closes B106's FB-catalog/I-O pendings), deobfuscated

> **Language note**: this block is written in **English** (standing corpus-language preference for new Spyder-ecosystem blocks), even though Blocks 1–114 of this corpus are in Spanish. Cross-references to prior Spanish blocks are kept by number.
>
> Empirical research of the Honeywell OEM module **`docHoneywellSpyder`** — the **bundled JavaHelp documentation** for the Honeywell SpyderTool. Unlike its siblings, this module carries **NO Java code at all**: it is a signed JAR whose payload is **173 HTML help pages + images/CSS** plus a JavaHelp `toc.xml`. `module.xml`: *"Honeywell SpyderTool's help documents"*, symbol `dochonst`, vendor **Honeywell `4.14.0.10.5.64`**, build **2024-10** (`buildMillis=1729001053332`), **`runtimeProfile=doc`** (a documentation-only profile; depends only on Tridium `baja 4.0`).
>
> **Why it matters**: this is the **single best non-code, vendor-authoritative source in the whole Spyder corpus**. [Block 106] left three pendings (full FB catalog #1, detailed I/O layer #2, UI/wizard #3) and [Block 115] could only enumerate the 45 FB *types* from the migrator's dispatch — neither documented each block's **pins, ranges, and algorithm**. This module documents exactly that, **as the manufacturer specifies it**. It substantially closes the per-block half of **G3** (FB catalog) and feeds **G4** (I/O layer).
>
> Sources (READ-ONLY; the module's own bundled docs are the primary artifact):
> `/home/cristian/modules/Prototipos/modulos/organized/docHoneywellSpyder/docHoneywellSpyder/extracted/{META-INF,doc}/...`
> Method: direct reading of `module.xml`, `MANIFEST.MF`, `toc.xml`, and the HTML help pages; HTML stripped to text for reading, line numbers taken from the raw HTML.
>
> **Marker convention for this block**: facts about the *module artifact itself* (manifest, signature, file inventory, profile) are `[CERT]` (local primary source, `file:line`). Facts read out of the *help pages* are `[CERT-doc]` — they are **official Honeywell documentation** (vendor-authored JavaHelp), cited by their real bundled path `:line` (the doc IS preserved locally inside the module under study; it was not downloaded by me). `[INFER]` = my deduction.
>
> Layer 22 (deobfuscated OEM). **Strong connections**: [Block 106] (`honeywellSpyderTool`/XL10NextGen — this doc is its help bundle; closes per-block FB catalog + I/O), [Block 115] (`spyderToIrmNxMigrator` — the 45-branch FB dispatch is now cross-checked against the documented palette), [Block 77] (Spyder BACnet/LON drivers — the driver-side views are documented here), [Block 101] (`airFlowBalancer`), [Block 88] (Sylk wall modules), [Block 94] (Device Manager).

---

## 116.1 — What it is: a code-free, signed documentation module `[CERT]`

| Attribute | Value | Citation |
|---|---|---|
| Module name / symbol | `docHoneywellSpyder` / `dochonst` | `extracted/META-INF/module.xml:2` |
| Vendor / version | Honeywell / `4.14.0.10.5.64` | `module.xml:2` |
| Description | *"Honeywell SpyderTool's help documents"* | `module.xml:2` |
| Runtime profile | **`doc`** (documentation-only) | `module.xml:2` |
| Dependencies | only `baja` (Tridium) `4.0` | `module.xml:2` |
| Build | `buildMillis=1729001053332` (2024-10-15), `buildHost=IE3BVWECCGV01` | `module.xml:2` |
| Java code | **none** — recon confirms `class_count: 0`, `tiene_codigo_java: false` | `pipeline/fase1-recon.json` |
| Payload | 173 `.html` + GIF/PNG/JPG/CSS + `toc.xml` | `find extracted/doc` inventory |
| Signature | **signed** (347 `SHA-256-Digest` entries in `MANIFEST.MF`; `SERVER1.SF` + `SERVER1.RSA`) | `extracted/META-INF/MANIFEST.MF`, `SERVER1.RSA` |

**Reading `[CERT]`**: this is the Niagara `doc` runtime profile — a JAR that ships only context-help content, surfaced inside Workbench when the user clicks Help on a Spyder view. The empty `<types></types>` and `module.palette` with a single empty `b:Folder` confirm it registers **no BComponent types** (`module.palette`). It is signed like every other Honeywell OEM module (relevant to the code-signing arc B75/B113: even the help bundle carries a Honeywell signature). The internal lexicon header still says *"Lexicon for the docHoneywellXL10NextGen module"* (`extracted/docHoneywellSpyder.lexicon`) — a leftover confirming the SpyderTool's internal name is **XL10NextGen** (corroborates [Block 106]/[Block 115]).

## 116.2 — The documentation map (`toc.xml` JavaHelp tree) `[CERT-doc]`

`toc.xml` is a Sun JavaHelp 1.0 TOC (`toc.xml:4-5`) that organizes the whole help set. Top-level sections (`toc.xml:6-119`):

| Section | Covers | TOC anchor |
|---|---|---|
| Getting Started | Scenarios, About Spyder | `toc.xml:6-9` |
| Device | Actions on BACnet/LON, Compile, Device Icons, **Generate XIF File** | `toc.xml:12-18` |
| Engineering Mode | offline programming mode | `toc.xml:23` |
| **Function Blocks** | Add/Delete + 7 categories (Analog, BuiltIn, Control, DataFunction, Logic, Math, ZoneArbitration) | `toc.xml:28-101` |
| **Inputs Outputs** (Physical Points) | Binary/Modulating/Network In&Out, Editing Software Points | `toc.xml:102-110` |
| Sylk Device Support | Sylk device + advanced properties | `toc.xml:113-116` |
| Macros / Library | reusable logic + library mgmt | `toc.xml:117-118` |

Beyond the TOC, the `doc/` root also carries **per-view context-help** pages named after the actual Workbench views: `honeywellSpyderTool-WireSheet.html`, `-WiringDiagram.html`, `-AlarmsView.html`, `-ResourceUsage.html`, `-ControllerSummaryView.html`, `-PiranhaSplitWireSheetView.html`, plus `Application*View.html` and `Macro*View.html` (`ls extracted/doc`). These are the help targets for [Block 106]'s UI layer (its pending #3).

## 116.3 — The Function-Block catalog AS THE MANUFACTURER SPECIFIES IT — pins, ranges, algorithm `[CERT-doc]`

This is the load-bearing contribution. The help set documents **43 function-block algorithm pages** across 6 functional categories (BuiltIn objects counted separately), each with a prose description, an **Inputs** table (pin name · Low/High range · behavior when unconnected/invalid/out-of-range) and, where applicable, the **algorithm formula**:

| Category | Count | Documented blocks | Overview page |
|---|---|---|---|
| Analog | 10 | AnalogLatch, Average, **Compare**, Encode, HystereticRelay, Maximum, Minimum, PrioritySelect, Select, Switch | `doc/FunctionBlocks/Analog/Analog.html` |
| Control | 7 | AIA, Cycler, FlowControl, **PID**, RateLimit, Stager, StageDriver | `.../Control/ControlFunctionBlocks.html` |
| DataFunction | 5 | Alarm, Counter, Override, PriorityOverride, RunTimeAccumulate | `.../DataFunction/DataFunctionFunctionBlocks.html` |
| Logic | 4 | And, Or, XOr, OneShot | `.../Logic/LogicFunctionBlocks.html` |
| Math | 13 | Add, Subtract, Divide, Multiply, SquareRoot, DigitalFilter, **Enthalpy**, Exponential, FlowVelocity, Limit, Ratio, Reset, Logarithm | `.../Math/MathFunctionBlocks.html` |
| ZoneArbitration | 4 | GeneralSetPointCalculator, **OccupancyArbitrator**, SetTemperatureMode, TemperatureSetpointCalculator | `.../ZoneArbitration/ZoneControlFunctionBlocks.html` |

**Cross-check with [Block 115] `[CERT]`/`[INFER]`**: B115 §115.3 reconstructed a **45-branch FB dispatch** from the migrator. The documented palette here (43 algorithm pages + the BuiltIn `Schedule`/wall-module objects) **independently confirms that taxonomy from the vendor side** — same categories (`logic, math, analog, control, dataFunction, zoneControl, builtIn`) named in B106 §9/B115. Every lossy mapping B115 flagged is now explained by the docs: e.g. B115 collapsed `Average`/`Maximum`/`Minimum` onto one IRM `BAggregation` — the docs confirm these three are distinct same-shape analog reducers. `[INFER]`: the small delta (43 documented vs 45 dispatched) is the migrator additionally handling `Macro` and a util/passthrough branch, not extra user-facing blocks.

Worked examples (the depth available for **every** block):

- **PID** `[CERT-doc]` — full control law documented verbatim: `Err = Sensor - Set Point` (`Control/PID.html:162`); `Kp = 100/Proportional Band` (`:164`); `Ti = Integral Time (s)`, `Td = Derivative Time (s)`, `Bias = proportional offset (%)`; `Output (%) = bias + Kp*Err + Kp/Ti·∫Err + Kp*Td*dErr/dt` (`:172`). Inputs `disable` (logic), `sensor`, `setPt`, `tr` with explicit unconnected/invalid → "PID disabled, Output 0" semantics.
- **Compare** (Analog) `[CERT-doc]` — "provided with **4 input pins — In1, In2, Onhyst, Offhyst** and 1 output pin, **Out**" (`Analog/COMPARE.html:145-146`); does `<`/`>`/`=` with on/off hysteresis; "Output returns 0 if no inputs connected or all invalid".
- **Enthalpy** (Math) `[CERT-doc]` — inputs `t`, `rh` → output `OUTPUT`; computes **enthalpy (BTU/LB)** from temperature (Deg.F) and relative humidity (%); `rh` clamped 0–100%, `t` clamped **0–120 Deg.F** (`Math/ENTHALPY.html:147-149`). This is the source FB that B115 maps to IRM `BPsychrometric` with pin renames `t→Temperature`, `rth→RelHumidity` — the doc gives the authoritative pin names.
- **OccupancyArbitrator** (ZoneArbitration) `[CERT-doc]` — inputs `scheduledCurrentState`, `WMOverride`, `NetworkManOcc`, `OccSensorState`; outputs **`EFF_OCC_CURRENT_STATE`**, `MANUAL_OVERRIDE_STATE` (`ZoneArbitration/OccupancyArbitrator.html:145-146,291`). Documents the OCC/OCCNUL (0/255) occupancy enum semantics.

## 116.4 — The I/O layer (feeds G4): Physical Points, NVs, Sylk `[CERT-doc]`

The "Inputs Outputs" section documents the controller I/O model that [Block 106]'s pending #2 (`io/` ~87 classes) left uncovered from the code side:

- **Four physical-point types** the user can configure: **Binary Inputs, Binary Outputs, Modulating Inputs, Modulating Outputs**, plus **Software Points** (Constants, Network Input, Network Setpoint, Network Output) (`PhysicalPoints/PhysicalPoints.html:157-168`). Documented sub-pages exist for each: `BinaryInputs/Outputs`, `ModulatingInputs/Outputs`, `NetworkInputs/Outputs`, `NetworkSetpoints`, `Constants`, `EditingSoftwarePoints` (`ls PhysicalPoints/`).
- **Model-gated point availability** `[CERT-doc]`: "Depending on the model selected, default (Fixed) Physical points ... are made available. The Honeywell SpyderTool **automatically validates rules, based on the model selected**" — e.g. LonSpyder II PVL6436AS exposes Actuator + On-Board Pressure Sensor as fixed points; PVL6438NS only the pressure sensor (`PhysicalPoints/PhysicalPoints.html:148-155`). This documents the per-model terminal-assignment logic that lives in B106's `io/`.
- **Network Variables (LON) / BACnet Objects** `[CERT-doc]`: a full `NVs/` section — `AddNVI/AddNVO/AddNCI`, `AddManytoOneNV`, `GroupNV`, and the BACnet-object equivalents `ObjectInput/ObjectOutput/ObjectSetpoint`, `BacnetObjects` (`ls NVs/`). Confirms the dual LON-NV / BACnet-object data model of [Block 77].
- **Sylk IO** `[CERT-doc]`: `SylkDeviceSupport` + `SylkAdvancedProperties` (`ls SylkIO/`); the About page lists the **new Sylk IO models** SIO4022, SIO6042, SIO12000, Sylk Actuator, Sylk Sensor C7400S (`General/AboutHoneywellSpyder.html:167`) — the wall-module/Sylk surface of [Block 88].

## 116.5 — Operational workflow: the three modes, calibration, compile/XIF `[CERT-doc]`

The help documents the SpyderTool's operating lifecycle, corroborating [Block 106]'s engine/simulator and [Block 77]'s download path:

- **Three modes** with toolbar switching: **Engineering Mode** (offline programming), **Simulation Mode**, **Online Debugging Mode** (`Simulation/Simulation.html:184`, `OnlineDebugging/OnlineDebuggingMode.html:143`).
- **Offline simulation / Force Values** `[CERT-doc]`: "In the **Simulation mode alone, you can override Functional block outputs**" via the Force Values dialog; forcing one FB output overrides all outputs of that block to NaN (non-Enums) / first item (Enums) (`Simulation/ForceInputConfiguration.html:153`). This is the doc-side view of B106's `deviceModes/simulation` engine.
- **Online debugging / write-to-controller**: `ForceWritePointsToController.html`, `ModesOfOperation.html`, `SelectPointsToDebug*` (`ls OnlineDebugging/`) — the read/write probe surface; ties to [Block 77]'s download/upload (`ISpyderDownload`/`ISpyderUpload`).
- **Sensor & Flow calibration**: dedicated `SensorCalibration/` and `FlowCalibration/` sections — the input-linearization feature B106 attributed to `io/`.
- **Compile + XIF generation** `[CERT-doc]`: `Device/Compile.html` and **`Device/GenerateXIFFile.html`** document the build step and the **XIF (XML Interface File)** export. This **escalates B77 §77's `[INFER]` on `BXIFGenerator`/XIF**: the doc confirms XIF generation is a real, user-facing Device action (LON external interface definition), not just an inferred code path.
- **Library & Macros**: `Library/` (Add/Import/Export/Load/Delete library items) and `Macros/` document reuse of FB sub-graphs — the `BMacro`/library packages B106 and B115 reference.

## 116.6 — Driver-side context help — corroborates [Block 77] `[CERT-doc]`

The root carries view-help for both Spyder drivers, confirming the manager/view surface of [Block 77]:

- **BACnet Spyder**: `BacnetSpyderDeviceManagerView`, `BacnetSpyderPointManager`, `BacnetObjectManagerView`, `BacnetLinkManager`, `BacnetControllerSummaryView`, `BacnetAlarmsView`, **`SpyderBatchOperationsView`** (`ls extracted/doc | grep honeywellBacnetSpyder-*`).
- **LON Spyder**: `LonSpyderDeviceManagerView`, `LonNvManagerView`, `LonAlarmsView` (`...honeywellLonSpyder-*`).

`[CERT-doc]` capacity limits (About page): a **Lon Spyder Relay supports max 300 function blocks; a BACnet Spyder Relay max 200** (`General/AboutHoneywellSpyder.html:183`). The same page enumerates the **9 required JARs** of the Spyder tool chain: `honeywellSpyderTool.jar`, `docHoneywellSpyder.jar`, `honeywellLonSpyder.jar`, `honeywellBacnetSpyder.jar`, `genericUIFramework.jar`, `wsVavBalancer.jar`, `wsStdLonDeviceTemplates.jar`, `honeywellSpyderMigrator.jar`, `airFlowBalancer.jar` (`AboutHoneywellSpyder.html:190` ff) — a vendor-stated dependency closure tying together [Block 106], [Block 77], [Block 115] (migrator), and [Block 101] (airFlowBalancer).

## 116.7 — Self-verification (in-block gatekeeping, METHODOLOGY §11)

**Token check** — every load-bearing citation was `grep`-confirmed in its source. Confirmed (15 checks): `module.xml:2` vendorVersion `4.14.0.10.5.64` + `runtimeProfile='doc'` ✓; `pipeline/fase1-recon.json` `class_count:0`/`tiene_codigo_java:false` ✓; `MANIFEST.MF` 347 SHA-256 digests ✓; `toc.xml:4` JavaHelp DTD ✓; `PID.html:162,164,172` (Err/Kp/Output formula) ✓; `COMPARE.html:145-146` (4 pins In1/In2/Onhyst/Offhyst, Out) ✓; `ENTHALPY.html:147-149` (BTU/LB, 0–120 Deg.F, 0–100%) ✓; `OccupancyArbitrator.html:145-146,291` (pins + EFF_OCC_CURRENT_STATE) ✓; `PhysicalPoints.html:155,157,168` (four types + model validation + software IOs) ✓; `Simulation/ForceInputConfiguration.html:153` (override FB outputs) ✓; `AboutHoneywellSpyder.html:167` (Sylk IO models) ✓; `:183` (300/200 FB limits) ✓; `:190` (9 JARs) ✓; `Device/GenerateXIFFile.html` exists ✓; FB page counts per category (10/7/5/4/13/4) via `ls` ✓.

**Marker tally**: `[CERT]` ≈ 11 (module-artifact facts in §116.1) · `[CERT-doc]` ≈ 28 (help-page facts across §116.2–§116.6) · `[CERT-web]` 0 · `[CERT-a]` 0 · `[INFER]` ≈ 3. **`[INFER]`/`[CERT]+[CERT-doc]` ratio ≈ 0.08** — very low: this gap is **evidence-rich** (vendor documentation), the opposite of exhausted. The corpus value is the new `[CERT-doc]` tier itself, which can ESCALATE prior `[INFER]`/`[CERT-a]` claims in B77/B106.

**Artifacts**: this block file exists; `CATALOG.md` regenerated; `INDEX.md` + `RESEARCH-STATE.md` updated; backlog re-classified.

## 116.x — Connections

- **[Block 106]** (`honeywellSpyderTool`/XL10NextGen) — this module **is its help bundle**. It substantially closes B106 **pending #1** (per-block FB catalog: §116.3 gives every block's pins/ranges/algorithm from the vendor) and feeds **pending #2** (I/O layer: §116.4 documents the four physical-point types + model-gated terminal rules). The per-view help pages (§116.2) map B106 **pending #3** (UI views) to their documented behavior.
- **[Block 115]** (`spyderToIrmNxMigrator`) — the documented FB palette (§116.3) **independently confirms** B115's reconstructed 45-branch dispatch and supplies the authoritative source-side pin names B115 had to map (e.g. Enthalpy `t`/`rth` → IRM `Temperature`/`RelHumidity`).
- **[Block 77]** (Spyder BACnet/LON drivers) — §116.6 documents the driver views; §116.5 **escalates B77's `[INFER]` on XIF generation** to `[CERT-doc]` (`GenerateXIFFile.html`) and corroborates the download/online-debug path; §116.6 adds vendor capacity limits (300/200 FBs).
- **[Block 101]** (`airFlowBalancer`) & **[Block 88]** (Sylk wall modules) — both named in the 9-JAR closure (§116.6) and the Sylk IO surface (§116.4).
- **[Block 94]** (BACnet Device Manager) — the `BacnetSpyderDeviceManagerView` help (§116.6) is the doc side of the device-manager dependency B115 also relies on.
