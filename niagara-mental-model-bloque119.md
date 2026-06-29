# Block 119 — `honeywellSpyderTool` / XL10NextGen: the UI / wizard / application-generation layer, CODE-SIDE

> **Language note**: written in **English** (standing corpus-language preference for new Spyder-ecosystem blocks; legacy Blocks 1–114 are Spanish, cross-referenced by number).
>
> Empirical READ-ONLY research of the **UI + wizard + code-generation** layer of the Honeywell **`honeywellSpyderTool`** Workbench module (`com.honeywell.honeywellXL10NextGen.*.ui` + `factory` + `library` + the Kingfisher `ui/uiframework`). ~250+ `.java` across `functionalBlocks/ui` (44), `deviceModes/common/ui` (52), `xl10Controller/ui` (70), `io/ui` (45, covered in [Block 118]), `points/ui` (18), `logicContainers/ui` (13), `network/ui` (8), the three `deviceModes/{engineering,simulation,onlineDebugging}/ui`, plus the Kingfisher wall-module config UI (`functionalBlocks/blocks/builtIn/kingfisher/ui` + `uivalidations` + `tr4x/ui`). This is the **code-side answer to [Block 106]'s pending #3** (the full UI/wizard layer). It answers four questions: (1) how the editor/wiresheet UI is layered, (2) how a *model/wizard produces a whole application*, (3) what the TASO wizard models actually are in code, (4) how all of it builds on **[Block 102]**'s `genericUIFramework`.
>
> Sources (READ-ONLY, decompiled — vineflower/CFR clean):
> `/home/cristian/modules/Prototipos/modulos/organized/honeywellSpyderTool/honeywellSpyderTool/vineflower/com/honeywell/honeywellXL10NextGen/...`
> Cited as `<relative-path>.java:line` from that package root (e.g. `factory/FactoryManager.java:126`, `deviceModes/common/ui/BPiranhaWireSheet.java:21`, `functionalBlocks/blocks/builtIn/kingfisher/ui/uiframework/...`).
> Method: direct reading of the model-factory dispatch, the model-name predicates, the wizard frame + step framework, the Kingfisher UI-gen engine, and the editor view hierarchy; one Explore sub-agent mapped `functionalBlocks/ui` / `deviceModes/common/ui` / `xl10Controller/ui` (every load-bearing `extends`/line re-verified by me); grep token-confirmation of every load-bearing `[CERT]`.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official downloaded doc (`docHoneywellSpyder` help, via [Block 116]) · `[INFER]` deduction. (No `[CERT-a]`/`[CERT-web]` here — this gap is code-grounded.)
>
> Layer 22 (deobfuscated OEM). **Strong connections**: [Block 106] (the SpyderTool — closes its pending #3), [Block 102] (`genericUIFramework` — escalated here from `[INFER]` to `[CERT]`), [Block 116] (vendor doc — the 3 modes + workflow cross-verified), [Block 117] (FB descriptor engine — the dialogs edit these), [Block 118] (`io/` — the `io/ui` editors), [Block 88] (Sylk/TR wall modules — the Kingfisher config wizard targets them), [Block 77] (Spyder drivers — the model factories live in those driver modules).

---

## 119.1 — What the UI layer is, and how it splits into three sub-systems `[CERT]`

`honeywellSpyderTool` is `runtimeProfile=wb` ([Block 106] §106.1): **all of this runs in Workbench, none on the JACE**. The ~250-class UI surface is not one monolith — it is **three architecturally distinct sub-systems**, each built on a different base:

| Sub-system | Packages | Built on | Role |
|---|---|---|---|
| **(A) FB wiresheet editor + device-mode UI** | `deviceModes/common/ui` (52), `functionalBlocks/ui` (44), the 3 mode UIs, `xl10Controller/ui` (70), `io/ui`, `points/ui` | **pure Niagara Workbench** (`BWireSheet`, `BWbComponentView`, `BDialog`) | the engineer's editing canvas, property dialogs, controller manager views |
| **(B) Application-generation backbone** | `factory/` (`FactoryManager`, `IModelFactory`), `util/BUtilityClass`, `library/` | reflection + abstract-factory, **sibling modules** | turns a *model selection* into a fully-populated controller application |
| **(C) Kingfisher wall-module config wizard + UI-gen** | `functionalBlocks/blocks/builtIn/kingfisher/{ui,uivalidations,tr4x/ui,configuration}` + `ui/uiframework/{wizardgenframework,uigenframework,common}` | **[Block 102] `genericUIFramework`** (MVC + `BajaUICreator`) **+** an XML-layout engine | step-based wizard that configures the TR4x wall-module HMI screens |

The key structural insight: **(A) is vanilla Niagara, (C) is where the Honeywell wizard/code-gen machinery actually lives, and (B) is the dispatch glue that connects a wizard's "model" choice to the managers that build the app.** The 183 files that reference `genericUIFramework` (§119.7) are almost entirely in (C).

## 119.2 — Sub-system (A): the wiresheet editor & device-mode UI `[CERT]`

**The wiresheet is Niagara's, not a custom canvas.** The editing surface is `BPiranhaWireSheet extends BWireSheet` (`deviceModes/common/ui/BPiranhaWireSheet.java:21`) and the split view `BPiranhaSplitWireSheetView extends BWireSheet` (`deviceModes/common/ui/BPiranhaSplitWireSheetView.java:80`) `[CERT]` — both subclass Niagara's standard `javax.baja.wiresheet.BWireSheet`. "Piranha" is the internal codename for the Spyder wiresheet. So FB placement/linking reuses the Niagara wiresheet engine; Honeywell only customizes it.

**The view base** is `BSpyderComponentView extends BWbComponentView implements ISpyderView` (`deviceModes/common/ui/BSpyderComponentView.java:33`) `[CERT]` — every controller-level view subclasses it: `BControllerSummaryView`, `BAlarmsView`, `BWiringDiagram`, `BApplicationDetails` (all `extends BSpyderComponentView`, `xl10Controller/ui/`) `[CERT]`.

**The three device modes** are one abstract base `BModeUI extends BComponent` (`deviceModes/common/ui/BModeUI.java:20`) with three subclasses `[CERT]`:

| Mode UI | Class | File:line |
|---|---|---|
| Engineering | `BEngineeringModeUI extends BModeUI` | `deviceModes/engineering/ui/BEngineeringModeUI.java:20` |
| Simulation | `BSimulationModeUI extends BModeUI` | `deviceModes/simulation/ui/BSimulationModeUI.java:22` |
| Online Debugging | `BOnlineDebuggingModeUI extends BModeUI` | `deviceModes/onlineDebugging/ui/BOnlineDebuggingModeUI.java:23` |

**Cross-verify with [Block 116] workflow** `[CERT-doc]`: B116 documented exactly three workflow modes — *Engineering / Simulation / OnlineDebugging* — with **Force Values** in Simulation. **MATCH code-side**: the shared `CommonViewToolBar` carries `SimCommand`, `SimulationTypeCommand`, `ForceInputConfigCommand`, `oDCommand` (`deviceModes/common/ui/CommonViewToolBar.java`) `[CERT]`, and `BForcedInputValueScreen` + `BWatchWindowSidebar` (`deviceModes/common/ui/`) implement the Force-Values / watch behavior the doc describes `[CERT]`.

**FB configuration dialogs** form a clean hierarchy `[CERT]`: `BXL10Dialog extends BDialog` (`functionalBlocks/ui/BXL10Dialog.java:28`) → `BXL10NextGenFbConfigurationScreen extends BXL10Dialog` (`:130`) → per-FB editors `BXL10NextGenPIDConfigurationScreen` (`:23`) and `BXL10NextGenTSCConfigurationScreen` (`:51`) both `extends BXL10NextGenFbConfigurationScreen`; siblings `BXL10NextGenAlarmConfigurationScreen`, `BXL10NextGenEncodeConfigurationScreen`, `BXL10NextGenStgDriverConfigurationScreen` `extends BXL10Dialog`. These are the per-block parameter editors for the FB descriptors of [Block 117]. On-canvas FB widgets build on `BPaneBase extends BAXComponentBase` (`functionalBlocks/ui/BPaneBase.java:81`), where `BAXComponentBase extends BTransferWidget` (`functionalBlocks/ui/BAXComponentBase.java:45`) — Niagara's drag/transfer widget `[CERT]`.

## 119.3 — Sub-system (B): the application-generation backbone is the **model factory** `[CERT]`

This is the answer to *"how does picking a model/wizard produce an application?"* — and it is the single most important finding of this block.

**`FactoryManager implements IFactory`** (`factory/FactoryManager.java:15`) is a string→factory dispatcher. `getModelFactory(String)` maps a **model name** to a concrete `IModelFactory`, loaded **by reflection from a sibling module** via `BUtilityClass.loadClass(module, className)` + `getInstance()` (`factory/FactoryManager.java:28-53, 56-145`) `[CERT]`:

| Model name(s) | Module loaded | Factory class | File:line |
|---|---|---|---|
| `Model1`–`Model7`, `MicroModel1`–`5` | `honeywellLonSpyder` | `com.honeywell.lonSpyder.factory.ModelFactoryN` | `:60-95,120` |
| `ModelBACnet1`–`4`, `ModelMicroBACnet1`–`5` | `honeywellBacnetSpyder` | `com.honeywell.bacnetSpyder.factory.ModelBACnetFactoryN` | `:96-125` |
| `TASOWizModel1`–`4`, `TASOWizBacnetModel1`–`2` | **`tasowizSupport`** | `com.honeywell.tasowiz.factory.TASOWizModelFactoryN` | `:126-143` |
| `Dummy` | (local) | `DefaultModelFactory` | `:57-59` |

The contract the factory must satisfy — **`IModelFactory`** (`factory/IModelFactory.java:42-104`) — is the *whole application toolkit* `[CERT]`: `getFBManager()`, `getFBResourceManager()`, `getApplicationManager()`, `getIOManager()`, `getIOResourceManager()`, `getNIManager()`, `getPointManager()`, `getTerminalAssignmentHandler()` (the `byte[]` handler of [Block 118]), `getIoInfoObject()`, `getModelInfo(int)`, `getControllerSpecificInfo()`, `getValidator()`, `getNetworkCompiler()`, `getPointDialogUI(...)`, `createPoint(...)`, `getIOObject(BIOTypesEnum)`, `getModelDescription(...)`, etc. (29 methods).

**So an "application" is produced like this** `[CERT]` + `[INFER]`: the engineer/wizard chooses a controller *model* → `FactoryManager` reflectively loads that model's `IModelFactory` from the LON / BACnet / TASO module → that factory hands back the full set of managers (FB, IO, points, network-interface, resource, terminal-assignment, validation, compiler) → the editor (§119.2) and the compiler ([Block 106] §106.3) operate through those managers. The `honeywellSpyderTool` JAR is therefore a **model-agnostic shell**; the per-model definitions and the TASO application templates live in *other* modules. (The dispatch is `[CERT]`; "this is the app-generation flow" is `[INFER]` over the wiring.)

## 119.4 — Sub-system (B): the TASO wizard models — a doc↔code delta + an escalation of [Block 106] `[CERT]`

[Block 106] §106 listed the wizard-assisted models — *`TASOWizModel1`=CV/AHU, Model2/3=VAV, BacnetModel1/2=VAV BACnet, Model4=LCBS* — but marked them **`[CERT-a]`** (it had not pinned them to code). This block resolves both halves honestly:

**(1) The model→HVAC-type mapping IS in this module's code → ESCALATE `[CERT-a]` → `[CERT]`.** `util/BUtilityClass.java` carries the predicates `[CERT]`:

| Predicate | Models | HVAC application | File:line |
|---|---|---|---|
| `isLonTASOWizCVAHUModel` | `TASOWizModel1` | **CV / AHU** | `util/BUtilityClass.java:5969-5974` |
| `isLonTASOWizVAVModel` | `TASOWizModel2`, `TASOWizModel3` | **VAV (LON)** | `:5965-5967` |
| `isBacnetTasowizVAVModel` | `TASOWizBacnetModel1`, `TASOWizBacnetModel2` | **VAV (BACnet)** | `:5981-5983` |
| `isLCBSModel` | `TASOWizModel4` | **LCBS** | `:5985-5990` |
| `isConfigurableSpyderModel` | any of the above | (the wizard-configurable set) | `:5997-5999` |

This exactly confirms B106's attribution — now from primary source.

**(2) The TASO model *implementations* are NOT in this corpus → honest gap.** The factory classes (`com.honeywell.tasowiz.factory.TASOWizModelFactory1..4`, `TASOWizBacnetModelFactory1..2`) and the device/blocks (`com.honeywell.tasowiz.device.BTasoWizLonSpyder` / `BTasoWizBacnetSpyder`, `com.honeywell.tasowiz.blocks.{BLogicalInputSelect,BLogicalOutputSelect,zeleny.BZeleny}`) all live in a **separate `tasowizSupport` module that is absent from the organized corpus** `[CERT]` (it is only ever referenced by string + reflection: `factory/FactoryManager.java:127-142`, `library/BSpyderPaletteRoot.java:99-115`, `library/{c,i,l}.java`). So *what each TASO wizard generates internally* (the pre-wired CV-AHU / VAV / LCBS application logic) stays **uninvestigable code-side** until `tasowizSupport` is added — this is the residual gap of G5 (→ G5b). The palette root also conditionally injects a `"TasowizUtil"` folder + the logical-select/Zeleny blocks **only when `tasowizSupport` is installed** (`library/BSpyderPaletteRoot.java:99-115`) `[CERT]`.

## 119.5 — Sub-system (C): the step-based wizard framework `[CERT]`

The reusable wizard *dialog* is a clean Niagara `BFrame` hierarchy in the Kingfisher UI framework `[CERT]`:

- **`BWizardFrame extends BFrame`** (abstract, `functionalBlocks/blocks/builtIn/kingfisher/ui/uiframework/wizardgenframework/BWizardFrame.java:64-65`): a classic **Next / Previous / Finish / Reset** wizard. A title pane (`createTitlePane`), a **two-pane body** (`createMiddlePane`: left nav scroll-pane + right step-content scroll-pane via `BSplitPane`), and a button bar (`createButtonPane`, lexicon keys `WizardFrame.Next/Previous/Finish/Reset`). State is driven by a **button mask**: `NEXT_ENABLE=1`, `PREVIOUS_ENABLE=2`, `FINISH_ENABLE=4` (`:91-93`), applied by `setButtonMask` (`:123-133`); abstract `reset()/next()/previous()/finish()` (`:135-141`) `[CERT]`.
- **`BTabbedWizardFrame extends BWizardFrame`** (`.../wizardgenframework/BTabbedWizardFrame.java:58`) → **`BDynamicTabbedWizardFrame extends BTabbedWizardFrame`** (`.../BDynamicTabbedWizardFrame.java:70`) — tabbed and dynamically-tabbed variants `[CERT]`.
- **`BStepKF extends BComponent`** (`.../wizardgenframework/BStepKF.java:26`): one wizard step = `(stepId, stepName, stepDesc, BWidget, BIcon)` (ctor `:43`) that **fires the `stepModified` Topic** when its content changes (`:32`, `fire(stepModified, ...)` `:95`), plus `BStepModifiedEvent` `[CERT]`. The consumer of `BWizardFrame` is the obfuscated Kingfisher helper `functionalBlocks/blocks/builtIn/kingfisher/ui/ak.java` `[CERT]`.

## 119.6 — Sub-system (C): the Kingfisher wall-module **UI-generation / code-gen** engine `[CERT]`

This is the actual "code generation": the engineer lays out the **TR4x wall-module HMI screens** in a design-mode editor and the layout is serialized to/from XML.

- **`BAXViewKF extends BWbComponentView`** (`.../ui/uiframework/uigenframework/BAXViewKF.java:49`): a Workbench view with a toolbar of `DesignModeCmd` / `RunModeCmd` (toggling a `design_mode` String property "yes"/"no", `:75-77`), `WriteToXmlCmd` (only added in design mode, `:80`), plus `ConvertToBaja` / `ConvertToCustom` / `Cut` / `Paste` / `Increase|DecreaseWidth|Height` / `RestoreLayout` commands `[CERT]`. `WriteToXmlCmd.doInvoke()` calls `((BAXContainerKF)container).doWriteToXml()` (`BAXViewKF$WriteToXmlCmd.java:28`) `[CERT]`.
- **`BAXContainerKF extends BComponent implements IConfigurationStepContainer, IStaticReferenceCallback`** (`.../uigenframework/BAXContainerKF.java:43-44`): the step container. Its wizard steps are the **wall-module HMI screens** — the icon set is the tell: `General, Categories, Home, Occupancy, Fan, System, Schedule, Password, Preview` (`stepIconStrArr`, `:58`) `[CERT]`. `doConvertXml` walks `xRoot.elems()` flipping each widget's `type` attribute between Baja and Custom variants (`:147-189`); `doWriteToXml`/`doRestoreLayout`/`doResize` delegate to `BLayoutUtilKF` (`:127-145`) `[CERT]`. `BKFContainer extends BAXContainerKF` (`.../kingfisher/ui/BKFContainer.java:83`) is the concrete Kingfisher instance `[CERT]`.
- **`BLayoutUtilKF extends BObject`** (`.../ui/uiframework/common/BLayoutUtilKF.java:71`) is the **XML-layout engine** — the headline of (C) `[CERT]`: the wall-module UI is **declared in an XML layout resource**, not hard-coded. The ctor opens the layout `BOrd` → `BIFile` InputStream → `XParser.make(...).parse()` into `xRoot` and reads its `version` (`:93-104`); `initPanes()` **instantiates each widget by reflection from the XML `type` attribute** — `Class.forName(xElem.get("type")).newInstance()` (`:114`, again `:161`) — wiring panes by element name and tagging `BKFPaneBase.setPaneName(...)` (`:108-128`); `restore()` re-parses a default layout and writes it out via `XWriter` to a `File`, **version-gated** (only overwrites when the on-disk `version` differs, `:239-261, 272-277`) `[CERT]`.

**This mirrors [Block 102]'s `BajaUICreator` pattern** (PX → `BWidget` tree by reflection) but with a **Kingfisher-specific XML scheme** instead of `.px` — same idea (data-driven UI by reflective class loading), different serialization. `[INFER]` (architectural parallel; the two do not share code).

## 119.7 — How (C) builds on [Block 102] `genericUIFramework` — escalation `[INFER]`→`[CERT]`

[Block 102] §102.5 *inferred* ("`[INFER]`") that the Honeywell Workbench wizards consume `genericUIFramework` — `BajaUICreator` (framework B) for the widget tree + the Struts-like MVC engine (framework A) for step flow. **This block confirms it code-side `[CERT]`.** **183** files in `honeywellSpyderTool` import `com.honeywell.generic.ui.*` or `com.honeywell.baja.ui.creator.*`, concentrated in the Kingfisher config wizard `[CERT]`:

| Package | Files using genericUIFramework | What it is |
|---|---|---|
| `kingfisher/uivalidations/actions` | 81 | the wizard's **MVC Actions** — `extends AbstractAction` (B102 framework A) |
| `kingfisher/ui` | 71 | the wall-module config screens |
| `kingfisher/uivalidations/validations` | 17 | the wizard's **Validators** — `extends AbstractValidator` |
| `kingfisher/tr4x/ui` (+`/actions`) | 7 | TR4x step UI (`BajaUICreator` + MVC) |

Concrete `[CERT]` evidence:
- **Framework A (MVC)**: the `uivalidations/actions` classes are real `AbstractAction` subclasses with HVAC-semantic names — `ActionFanStateSelection`, `ActionCategoryReorder`, `ActionGenSettingSelectModel`, `ActionOccEnableStandby`, `ActionDoNotShowOccOverStatus`, … (`extends AbstractAction`, `uivalidations/actions/*.java`); `uivalidations/validations/*` are `extends AbstractValidator` `[CERT]`. The MVC controller is invoked by name: `BSBusConfigDialog` does `UIFwController.getInstance("zio")` (`functionalBlocks/blocks/builtIn/kingfisher/BSBusConfigDialog.java:211`) then `c.createRequest(sessionId, "loadSbusWallModuleAction")` / `"previewUnLoadAction"` (`:223, :311`) — exactly B102's request/action/forward flow, controller instance keyed `"zio"` (S-Bus/Sylk-IO) `[CERT]`.
- **Framework B (`BajaUICreator`)**: `BResourceUsage` builds its view with `new BajaUICreator(BOrd.make("module://honeywellSpyderTool/com/honeywell/honeywellXL10NextGen/logicContainers/ui/ResourceUsage.px"))` (`logicContainers/ui/BResourceUsage.java:175`) `[CERT]` — a PX loaded through B102's loader, indexed by widget ID exactly as B102 §102.3 described.

**Net**: sub-system (A) (FB editor + controller views) is **pure Niagara**; sub-system (C) (the wall-module config wizard) is the **genericUIFramework consumer**. B102 §102.5's `[INFER]` is now `[CERT]`.

## 119.8 — Self-verify

- **Token checks (load-bearing `[CERT]`, grep-confirmed against source — 12/12 pass)**:
  1. `BPiranhaWireSheet extends BWireSheet` → `deviceModes/common/ui/BPiranhaWireSheet.java:21`; split view `:80` ✓
  2. `BSpyderComponentView extends BWbComponentView implements ISpyderView` → `:33` ✓
  3. `BModeUI extends BComponent` `:20` + 3 subclasses (Engineering `:20`/Simulation `:22`/OnlineDebugging `:23`) ✓
  4. `FactoryManager` TASO dispatch → `factory/FactoryManager.java:126-143` (`tasowizSupport`, `TASOWizModelFactory1..4`) ✓
  5. `IModelFactory` 29-method contract → `factory/IModelFactory.java:42-104` (getFBManager/getIOManager/getTerminalAssignmentHandler…) ✓
  6. TASO model predicates → `util/BUtilityClass.java:5965-5990` (Model1=CV/AHU, 2/3=VAV, Bacnet1/2=VAV, Model4=LCBS) ✓
  7. `BWizardFrame extends BFrame` + masks NEXT=1/PREVIOUS=2/FINISH=4 → `.../wizardgenframework/BWizardFrame.java:64,91-93` ✓
  8. `BStepKF extends BComponent` + `stepModified` Topic → `.../wizardgenframework/BStepKF.java:26,32,95` ✓
  9. `BAXViewKF extends BWbComponentView` + Design/Run/WriteToXml cmds → `.../uigenframework/BAXViewKF.java:49,75-80` ✓
  10. `BAXContainerKF` step icons (Home/Occupancy/Fan/System/Schedule/…) → `.../uigenframework/BAXContainerKF.java:58`; `BKFContainer extends BAXContainerKF` `kingfisher/ui/BKFContainer.java:83` ✓
  11. `BLayoutUtilKF` reflective widget load `Class.forName(xElem.get("type")).newInstance()` → `.../common/BLayoutUtilKF.java:114,161`; XParser/version `:93-104,261` ✓
  12. genericUIFramework usage: `UIFwController.getInstance("zio")` `kingfisher/BSBusConfigDialog.java:211`; `BajaUICreator(...ResourceUsage.px)` `logicContainers/ui/BResourceUsage.java:175`; `extends AbstractAction` across `uivalidations/actions/*` ✓
- **Marker tally**: `[CERT]` ≈ 47 · `[CERT-doc]` ≈ 2 (B116 modes/workflow, inherited) · `[INFER]` ≈ 3 · `[CERT-a]`/`[CERT-web]` 0. **`[INFER]`/`[CERT]` ratio ≈ 0.06** — evidence is rich and code-grounded, **not** exhausted. The one genuine code-side dead-end is the absent `tasowizSupport` module (§119.4), which is a *different artifact* (separate module), not exhaustion of this one.
- **Cross-block corrections (§14)**: ESCALATED [Block 106] §106 TASO model→HVAC mapping `[CERT-a]`→`[CERT]` (BUtilityClass predicates); ESCALATED [Block 102] §102.5 "wizards consume genericUIFramework" `[INFER]`→`[CERT]` (183 files; UIFwController + BajaUICreator + AbstractAction subclasses).
- **Honest residual gap (G5b)**: the `tasowizSupport` module (TASO factory + device + blocks `com.honeywell.tasowiz.*`) is **not in the corpus**; the *internal application logic each TASO wizard emits* (the pre-wired CV-AHU/VAV/LCBS sheets) is therefore uninvestigable code-side here. Decompiling `tasowizSupport` would close it.
- **Artifacts**: this block file created; CATALOG regenerated; INDEX + RESEARCH-STATE updated; engram mirrored.

## 119.9 — Connections

- **[Block 106]** — closes its **pending #3** (full UI/wizard layer, `functionalBlocks/ui` + `kingfisher/ui` + wizardgen, ~250 cls): editor hierarchy, the model-factory app-generation backbone, the wizard framework, and the Kingfisher UI-gen engine are now distilled code-side. (B106's other pendings #1/#2 closed by [Block 117]/[Block 118].)
- **[Block 102]** — `genericUIFramework`: §102.5's `[INFER]` that Honeywell wizards consume framework A (MVC) + framework B (`BajaUICreator`) is **escalated to `[CERT]`** here — the Kingfisher config wizard is its principal consumer (183 files; `UIFwController.getInstance("zio")`, `AbstractAction`/`AbstractValidator` subclasses, `ResourceUsage.px`).
- **[Block 116]** — vendor doc: the 3 workflow modes (Engineering/Simulation/OnlineDebugging) + Force Values are cross-verified against `BModeUI` subclasses + `CommonViewToolBar`/`BForcedInputValueScreen` — MATCH; the TASO model list is corroborated by `BUtilityClass`.
- **[Block 117]** — FB descriptor engine: the `BXL10NextGen*ConfigurationScreen` dialogs (PID/TSC/Alarm/StgDriver) are the parameter editors for those FB descriptors; the wiresheet (`BPiranhaWireSheet`) is where the FBs are wired.
- **[Block 118]** — `io/`: `io/ui` (45 cls, covered there) is the I/O sub-editor; the model factory's `getTerminalAssignmentHandler()`/`getIoInfoObject()` (§119.3) are the `io/` types B118 distilled.
- **[Block 88]** — Sylk/S-Bus TR wall modules: the Kingfisher config wizard (sub-system C) targets the **TR4x wall-module HMI**; `BSBusConfigDialog` controller `"zio"` loads/previews the S-Bus wall module.
- **[Block 77]** — Spyder BACnet/LON drivers: the `IModelFactory` implementations for `ModelN`/`ModelBACnetN` live **inside** `honeywellLonSpyder`/`honeywellBacnetSpyder` (the driver modules) — the tool reflectively loads them (§119.3).
