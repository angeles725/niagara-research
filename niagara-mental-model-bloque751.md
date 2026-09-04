# B751 · How modules AUTHOR the Workbench (-wb) layer — the "how much wb is enough" ladder + the Manager/View/FieldEditor/Command recipes (code-grounded across Tridium + Honeywell)

> **Scope**: the operator asked to investigate everything about the WB and UX layers of modules. This block is
> the WB half: a code census of how real `-wb` parts are AUTHORED (Tridium core: driver/bacnet/alarm/history/
> program/kitControl/control; Honeywell: the device-manager framework + TB3026B/TC/wireless/importer/venom/
> eaglehawk). It extracts the decision rule ("how much wb does a component need") and the concrete recipes
> for each rung, with file:line. Not the wb FRAMEWORK internals (B427-432 = REMITTANCE) — the authored parts.
> UX half = B752; the playbook for our modules = B753. Foco: **wb-ux-authoring** (WBUX1+WBUX2).
>
> **Sources**: FUENTE 3 — decompiled `-wb` module parts under `organized/*/vineflower/` (+ `META-INF/module.xml`
> for agent registration, the reliable source in a decompiled corpus). FUENTE 1 — B427 (BWidget/gx), B428
> (shell), B429 (wire sheet), B430 (property sheet + field-editor dispatch), B431 (BAbstractManager), B432
> (commands/undo), B707 (wb best practices). Every recipe cites a class + file:line; generalizations [INFER].

---

## 751.1 — The decision rule: how much wb does a component need? `[CERT, cross-module]`
A ladder, cheapest first. Climb ONLY when the rung below is inadequate — proven by the census (kitControl
ships 152 rt types + 53 ux widgets but only **2** wb FEs; control ships a big rt but only **5** wb FEs):

| Rung | When | What you author | Evidence |
|---|---|---|---|
| **0 — nothing** | standard slots + standard value types | NOTHING — the default property sheet + wire sheet render it | B707 baseline; kitControl-wb=2, control-wb=5 over huge rt |
| **1 — a FieldEditor** | ONE composite/opaque value type renders poorly in the default editor | `BWbFieldEditor` `@AgentOn(that value type)` — nothing else | `BOverrideFE`/`BTriggerModeFE` (control); `BBacnetUnsignedFE` (bacnet); `BCapacityFE` (history) |
| **2 — a Manager** | the component is a CONTAINER of learned/discovered children (network/device/point) | `BAbstractManager`/`BDeviceManager` + Model(columns) + Controller(dbl-click→hyperlink) + Learn(discovery BJob + BJobBar) | `driver-wb` (base template), `bacnet-wb` (showcase) |
| **3 — a custom View/Editor** | interaction is fundamentally non-tabular (chart, code editor, console, tabbed config) | `BWbComponentView`/`BWbView` + a typed Command family | history (charts), program (code editor), alarm (console), TB3026B (tabbed config) |

## 751.2 — Rung 1: the FieldEditor recipe `[CERT]`
`extends BWbFieldEditor`; build the widget(s) in the ctor; wire dirtiness with `linkTo(widget,
BTextField.textModified, setModified)`; override `doLoadValue(BObject,Context)` (populate),
`doSaveValue(BObject,Context)` (reconstruct the value), `doSetReadonly` (cascade). Obtain a child editor for a
sub-value via `BWbFieldEditor.makeFor(value)`; re-facet with `reloadFacets(BWbFieldEditor[], BFacets)`.
- Simple: `BOverrideFE.java:30-75` (control), `BBacnetUnsignedFE.java:29,51,63,143` (bacnet),
  `BTB3026BPinCodeFE.java:22-60` (Honeywell), `BBeatsMacAddressFE.java:55-88` (Honeywell wireless).
- Type-swapping: `BTriggerModeFE` uses a `BListDropDown` + `BWidgetSwapper extends BPane` to hot-swap the
  sub-editor per subtype via `@NiagaraAction updateSubEditor` + `AgentList`/`AgentFilter`
  (`BTriggerModeFE.java:31-121`). `BWbPlugin` is the base for a non-value custom editor (enum-radio,
  `BTB3026BEnumRadioEditor.java:26`).

## 751.3 — Rung 2: the Manager recipe + the Honeywell plugin twist `[CERT]`
**The Tridium recipe** (`driver-wb` is THE template): `extends BAbstractManager`; override
`makeModel()`/`makeController()`; inner `Model extends MgrModel` overrides `makeColumns()` returning
`MgrColumn[]{new Name(), new MgrColumn.Type(), new Prop(...)}` + `getNewTypes()`/`getBaseNewType()`; inner
`Controller extends MgrController` overrides `cellDoubleClicked()` → `shell.hyperlink(new HyperlinkInfo(...))`.
A `BDeviceManager`/`BPointManager` adds `makeLearn()` (discovery) + `MgrTemplate`
(`BDriverManager.java:33-85`). `bacnet-wb` is the full showcase: `BBacnetDeviceManager` overrides
`makeModel/makeController/makeLearn/makeState`, `toTypes()` computes legal child types via
`Sys.getRegistry().getConcreteTypes(...)` filtered by `isParentLegal`; `BacnetDeviceLearn extends MgrLearn`
consumes a `BBacnetDiscoverDevicesJob` in `jobComplete(BJob)`, a `BJobBar` attached to the EdgePane top
(`BBacnetDeviceManager.java:57-100`, `BacnetDeviceLearn.java:43,211-249`).

**The Honeywell innovation — a device-model PLUGIN framework** `[CERT]`: rather than each device module writing
a Manager, `honeywellDeviceManager-wb` is a framework; a per-device module contributes only a
`BIHonBacnetDeviceModel`/`BIHonDeviceModel` (its columns + supported model-names + commands) that the
framework discovers from the registry (`HonDeviceModel.java:23` column engine, `HonDeviceCache` registry
lookup, `BHonMgrTable.java:14` event-driven column show/hide). So there are **two ways to author a device
manager**: (a) subclass the Tridium manager + override `makeModel/makeController/makeLearn` — Honeywell
bacnet/modbus do this (`BHoneywellBacnetDeviceManager.java:38-81`); (b) contribute ONLY a device-model plugin
— the TC thermostat does this (`BThermostatDeviceModel extends BObject implements BIHonBacnetDeviceModel`,
`getSupportedModels/getSupportedColumns/getSupportedCommands/getDeviceType`, `BThermostatDeviceModel.java:22-53`).
Recipe (b) is the reusable Honeywell pattern: a new device gets a manager row/columns/commands with zero
Manager code.

## 751.4 — Rung 3: the custom View + Command-family recipe `[CERT]`
`extends BWbComponentView` (or `BWbView`/`BDaemonSessionView`); build the pane tree in
`doLoadValue`/`buildDefaultContent` — `BScrollPane→BCanvasPane→BEdgePane`, a `BTabbedPane` for multi-section
config, a bottom `BGridPane` of `BSaveButton`/`BRefreshButton`; async modules add `IAsyncLoadableUI` +
`AsyncLoader` + `BLoadingProgressBar`. Examples: `BProgramEditor` (code editor + tabs,
`BProgramEditor.java:53-232`), `BHistoryChartBuilder`/`BHistoryEditor` (charts), `BAlarmConsole` (console),
`BTB3026BConfigurationView` (tabbed config "wizard", `BTB3026BConfigurationView.java:86-156`).

**Command family**: a view holds a typed command holder (`ProgramEditorCommands`) whose members
`extends Command` override `CommandArtifact doInvoke()` (`ProgramEditorCommands.java:147-320`); managers use
`MgrCommand`+`MgrEdit`. **Mutation always goes through the space**: batch =
`((BComponentSpace)getCurrentValueSpace()).newTransaction(); … tx.commit()`
(`BHistoryExtManager.java:32,371-377`); undo is inherited from the `Command`/`MgrEdit` framework, never
hand-rolled.

**Honeywell command→job variant**: `HonMgrCommand.doCommand` packages a `BCommandJobConfig` (command name +
device `BVector`) and calls `deviceConfig.executeCommand(cfg)` (RPC → `BJob`) (`HonMgrCommand.java:126-155`);
wizard-heavy TB3026B instead launches explicit `BJob`s from an agent `BMenu`
(`BTB3026BMenu` → `BTB3026BDownloadConfigJob`).

## 751.5 — "Wizard" is usually a tabbed VIEW, not a BWizard `[CERT]`
The Honeywell "Wizard" modules are misnamed: TB3026B/TC configuration is a **tabbed `BWbComponentView`**
(step-panes = tabs) backed by rt `BJob`s, and the device promotes its own views via
`AgentList.toTop("…:TB3026BConfigurationView")` in `getAgents()` (`BTB3026B.java:191-194`). The one true
step-wizard is `honImporter`: `BHonImportWizard extends BDialog` with `int _Step` + next/prev actions
swapping step panes, launched by `BImportTool extends BWbTool` from the Tools menu, with a custom
`BImportNavTree extends BNavTree` (`BHonImportWizard.java:53-84`, `BImportTool.java:9-19`).

## 751.6 — Agent registration + permission letters `[CERT]`
Register with `@NiagaraType(agent=@AgentOn(types={…}, requiredPermissions="…", defaultAgent=PREFERRED))`; the
COMPILED truth is `META-INF/module.xml` `<agent requiredPermissions="…"><on type="…"/></agent>` — the reliable
source in a decompiled corpus (the annotation is sometimes dropped by the decompiler). Permission letters
seen: `W`/`w` (write) on device/config views, `rwi` on platform wireless config, `ri`/`r` on notification
handlers/consoles. A base class (e.g. `BDeviceManager`) carries NO `@AgentOn` (extended, not registered); the
authored subclass adds it.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | The 4-rung "how much wb" ladder; climb only when the rung below is inadequate | [CERT] | kitControl-wb=2 FE / control-wb=5 FE over huge rt; B707 |
| 2 | FieldEditor recipe: ctor widgets + linkTo(setModified) + doLoadValue/doSaveValue/doSetReadonly; makeFor/reloadFacets | [CERT] | BOverrideFE.java:30-75; BBacnetUnsignedFE.java:29-143; BTB3026BPinCodeFE.java:22-60 |
| 3 | Manager recipe: BAbstractManager + Model(columns)/Controller(dbl-click hyperlink)/Learn(BJob+BJobBar); toTypes via isParentLegal | [CERT] | BDriverManager.java:33-85; BBacnetDeviceManager.java:57-100; BacnetDeviceLearn.java:211-249 |
| 4 | Honeywell device-model PLUGIN framework: contribute a BIHonDeviceModel, framework discovers it (2 ways to author a mgr) | [CERT] | HonDeviceModel.java:23; BThermostatDeviceModel.java:22-53; BHoneywellBacnetDeviceManager.java:38-81 |
| 5 | Custom View + typed Command family; mutation via newTransaction/tx.commit; undo inherited | [CERT] | BProgramEditor.java:53-232; ProgramEditorCommands.java:147-320; BHistoryExtManager.java:371-377 |
| 6 | Honeywell command→BJob (BCommandJobConfig/executeCommand) or BJob from agent BMenu | [CERT] | HonMgrCommand.java:126-155; BTB3026BMenu |
| 7 | "Wizard" = tabbed BWbComponentView (TB3026B) except honImporter = real BDialog step-wizard + BWbTool | [CERT] | BTB3026BConfigurationView.java:86-156; BHonImportWizard.java:53-84 |
| 8 | Registration via @AgentOn / module.xml <agent requiredPermissions>; letters W/w/rwi/ri | [CERT] | module.xml agent blocks across modules |

**Tally**: 8 [CERT]. No unmarked claims. Our own -wb parts (chihuahua/DashboardPan) are NOT in the decompiled
corpus (they live in our module repos).

## Connections
- **B427-B432** (the wb framework these author against), **B707** (wb best practices / when-needed),
  **B430** (field-editor dispatch), **B431** (BAbstractManager). Forward: **B752** (UX half), **B753**
  (playbook), and the build-n4-module kit `types/wb-widgets.md` (seed → this is the content to grow it).

## Open gaps
- **B751-G1**: our own `-wb` authoring has no exemplar in the corpus — if we ever build one, capture it.
- **B751-G2**: the `AsyncLoader`/`IAsyncLoadableUI` async-view pattern (TB3026B/wireless) mapped structurally,
  not its threading contract — a deepening if we author an async view.
