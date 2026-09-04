# B749 · How Honeywell organizes its N4 modules — the block-distribution taxonomy across ~30 modules (the 10 recurring patterns, code-grounded)

> **Scope**: the operator asked to see how the Honeywell modules DISTRIBUTE/ORGANIZE their blocks, exhaustively.
> This block is the SYNTHESIS of a 5-sweep code census (Plant Controller/IRM · Spyder/FB · drivers/Device
> Managers · wall-modules/sensors/Sylk · Venom apps/graphics/import) covering ~30 Honeywell modules. It
> extracts the recurring ORGANIZATION patterns — the containment spine, domain split, config/state/wire
> separation, folders, frozen-vs-dynamic, reuse via macro/palette, and the management/semantic layers — with
> file:line evidence. The applied playbook for OUR modules is B750. Foco: **interactive-composition**
> (IC5-IC9 → this taxonomy; IC10 closes with B750).
>
> **Sources**: FUENTE 3 — decompiled Honeywell modules under `organized/*/vineflower/` (class/inheritance
> census via `module_nav.py`), cited file:line per pattern. FUENTE 1 — baseline B7 (driver framework
> Network→Device→Ext→Point), B737 (composition), B746 (palette templates); prior Honeywell coverage B32/B77/
> B88/B96/B109. Every containment fact is [CERT] with a class + file:line; pattern generalizations are [INFER].

---

## 749.1 — The census (what was inspected) `[CERT]`

Five families, ~30 modules. Verdict per module on whether it defines a containment spine:

| Family | Modules with a real containment tree | Role-only / add-on / palette |
|---|---|---|
| Plant Controller / IRM | honPlantController (service tree), honPlantControllerHMI (service + HMI driver), honIrmConfig (BACnet device + Program) | honIrmControl (FB library), honIrmAppl (palette apps), 3 migrators (thin), honDescriptionUtility (1 point ext) |
| Spyder / FB | honeywellSpyderTool (the engine), honeywellBacnetSpyder / honeywellLonSpyder (thin device subclasses) | honeywellFunctionBlocks (flat FB runtime, no app container) |
| Drivers / Device Mgr | honBACnetUtilities (full spine), honMqttDriver (full spine) | honeywell*DeviceManager ×3 (managers, no containers), honBacnetHelper (export), Venom*, honRemoteConfig, honLonsockClient |
| Wall / Sensor / Sylk | honeywellSylkDevice, honeywellBacnetSmartSensor, honeywellModbusSmartSensor, honeywellBacnetWallModule, honeywellTB3026BWizard, honeywellTCThermostatWizard | honIOBase (shared base library), SylkActuatorAnalytics (service), honAdvWirelessCfg (platform settings) |
| Venom apps / misc | honImporter (mirror tree) | honeywellVenomBacnetApps / …LonApps (palette-only, 0 Java), honeywellVenomGraphics (px-only), honTagDictionary (semantic overlay), honAlarmExt/Console, honProjectExport, fcModelSync, honEagleHawkHMI (views) |

## 749.2 — The 10 recurring organization patterns `[CERT facts / INFER generalizations]`

### P1 — Reuse the driver Points spine; do NOT invent a container model `[CERT]`
Even complex function-block applications are built ON the standard `BDeviceNetwork → BDevice → BDeviceExt →
BControlPoint/BPointFolder` spine (B7), not a bespoke container:
- Spyder: `BApplicationLogic extends BPointDeviceExt` is the app root; `BMacro extends BPointFolder` is the
  reusable folder (`logicContainers/BApplicationLogic.java:142`, `BMacro.java:132`).
- IRM: `BIrmProgram extends BIrmFolder extends BFolder`, hung under `BIrmBacnetDevice extends BBacnetDevice`
  (`manager/BIrmProgram.java:328`, `BIrmBacnetDevice.java:591`).
- **Lesson**: the containment vocabulary is the framework's; Honeywell subclasses it, never replaces it.

### P2 — Split concerns into DOMAIN device-extensions `[CERT]`
A device does not carry a flat mix; it carries separate `BDeviceExt` children per DATA DOMAIN. The stock
`BBacnetDevice` already splits points/alarms/schedules/trendLogs/config; Honeywell extends the split:
- honBACnetUtilities: alarm domain → `BHonBacnetAlarmDeviceExt`; config domain promoted to a first-class
  extension `BBacnetDeviceParameters → BParameterFolder → BParameter` (`deviceconfig/BBacnetDeviceParameters.java:88`).
- IRM device carries points/alarms/schedules/trendLogs/config PLUS irm-specific `irmProxyDataComponent`,
  `ioComponents`, `errorBackupComponents`, `irmFileHandler` as frozen children (`BIrmBacnetDevice.java:638-652`).
- **This is the direct answer to "cómo tener todo bien distribuido"**: one child component per domain/concern.

### P3 — Three planes: config vs live-state vs wire-address, as SEPARATE blocks `[CERT]`
The strongest recurring idiom (wall/sensor family):
- **State** lives on the point/component (value + `BStatus`).
- **Config** lives in a FROZEN child `BComponent` (`honIOBase`: `BSmartSensorPoint` has frozen
  `config:BSmartSensorConfig`; `BSmartSensorConfig` has frozen `alarmLimits:BAlarmLimitsConfig` —
  `smartedgedevices/.../BSmartSensorConfig.java:46-48`).
- **Wire-address** is a THIRD, separate type: a `BStruct` mapping each config name → the field object/register
  (`*BacnetConfigObjects` name→`BBacnetObjectIdentifier`, `BTC500BacnetConfigObjects.java:1374`;
  `SmartSensorModbusRegisterDetails` for Modbus). Editing config and addressing the wire are deliberately two
  blocks, joined by `propertyId`.

### P4 — Grouping = TYPED folders with self-validating parent/child rules `[CERT]`
Grouping is not a generic bag; it is a typed folder that enforces what it may contain:
- `BParameterFolder.isParentLegal → DeviceParameters||ParameterFolder`, `isChildLegal → Folder||Parameter`
  (`deviceconfig/BParameterFolder.java:38-44`); `BHonBacnetNotificationClassFolder` self-nests
  (`alarm/....java:109-113`); `BIrmFolder`/`BIrmSubFolder`, `BHMIDeviceFolder extends BDeviceFolder`,
  `BHon*PointFolder`. The tree validates its own shape (the `configFatal`/`isParentLegal` invariant, B7).

### P5 — FROZEN for the skeleton, DYNAMIC for the population `[CERT]`
The frozen/dynamic choice is deliberate and tracks whether the content is KNOWN or VARIABLE:
- Known hardware → frozen: honMqttDriver freezes each LoRa sensor model's points as a `BMqttClientDriverPointFolder`
  subclass (`BR718ALoraSensor.java:36-38`); TB3026B/TC wizards freeze `appConfig`+`points` so instancing the
  device yields a ready tree.
- Arbitrary logic → dynamic: Spyder/IRM FBs are `add()`ed onto the sheet (IRM sample app reaches
  `functionBlockCount=1215`), even the FB pins are generated dynamically (`BAnd.generateAndProperties()`).

### P6 — Reuse via Macro/sub-app packaged into a Library/Palette `[CERT]`
Reuse is a first-class organizational layer:
- Spyder: `BMacro`/`BApplication` (recursive sub-apps) saved into `BAppLibrary`/`BLibFolder`
  (`library/BLibFolder.java:42`) via a `SaveToLibCommand`.
- IRM: `honIrmAppl` is a **palette-only** module — `module.palette` (127k lines) of pre-filled `irmn:IrmProgram`
  templates per controller model.
- Venom: `honeywellVenomBacnetApps`/`…LonApps` ship **zero Java** — their entire value is a `module.palette`
  BOG tree of pre-wired assemblies (`b:UnrestrictedFolder` → `honbs:BacnetSpyder` device → `points`/
  `ControlProgram` → recursive `honst:Application`), grouped by equipment class (Cvahu/Vav/Rio). This is
  literally B746's "assembly template" idea at industrial scale.

### P7 — Separate MANAGEMENT/commissioning/firmware from the containers `[CERT]`
`honeywellDeviceManager`/`…BacnetDeviceManager`/`…ModbusDeviceManager` hold **no container types** — only the
manager views, `BHonDeviceConfig` (a commission-config component mounted onto a device), firmware structs, and
jobs. The device CONTAINERS live in the driver modules (honBACnetUtilities, stock modbus). Management is a
separate module from containment.

### P8 — Category is PALETTE/PACKAGE taxonomy, not a runtime container `[CERT]`
The 7 Spyder FB categories (analog/control/logic/math/dataFunction/zoneControl/builtIn) are Java sub-packages
+ palette folders; the runtime app tree is FLAT and ordered by an `ExecutionOrder` property, not by category
folders (`honeywellFunctionBlocks BFunctionBlock.java:52-82`). Don't confuse the browse taxonomy with the
runtime tree.

### P9 — Semantic overlay (tags/relations) is SEPARATE from containment `[CERT]`
`honTagDictionary` (`BHonTagDictionary extends BSmartTagDictionary`) does not nest equipment — it DECORATES
the existing tree with tags (`BEquipmentTypeTag`, `BHonTypeTag`), auto-apply rules
(`BIsPointProxyTypeRule`+`BIsPointProxyTypeCondition`), and relations (`BCustomRelation`). Organization by
MEANING is layered over organization by CONTAINMENT, not merged into it.

### P10 — A shared BASE substrate library `[CERT]`
`honIOBase` defines no device root — it is the reusable point/config substrate (`BIONumericPoint`,
`BIOPointConfig`, `BSmartSensorPoint`, `BSmartSensorConfig`, `BSmartSensorDeviceConfig`). Both the BACnet and
Modbus smart-sensor modules build their leaves on it, so their trees are near-identical (they differ only in
file handler + discovery/register wiring). A common substrate = consistent organization across sibling modules.

## 749.3 — The one-picture reference tree `[INFER, distilled from P1-P10]`

The canonical Honeywell equipment module, distilled:
```
BXxxNetwork (reuse stock)                              P1
 └─ BXxxDevice (BBacnetDevice/…)                       P1
     ├─ points    : BPointDeviceExt  → BPointFolder → points   P2/P4
     ├─ alarms    : BAlarmDeviceExt  → NotificationClassFolder  P2
     ├─ schedules : BScheduleDeviceExt                          P2
     ├─ config    : BDeviceParameters → BParameterFolder → BParameter   P2/P3
     ├─ <program> : BFolder → function blocks (dynamic)         P5/P6
     ├─ deviceConfiguration : BComponent (frozen)  ── config plane   P3
     ├─ wireMap  : BStruct name→objectId          ── address plane   P3
     └─ fileHandler / jobs                          ── I/O plane       P3/P7
   [+ semantic tags/relations overlaid separately]   P9
   [+ management/firmware in a SEPARATE module]       P7
   [+ shipped as a pre-wired palette template]        P6
```

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | ~30 Honeywell modules censused; only a subset define a containment spine (table) | [CERT] | 5-sweep census, per-module file:line |
| 2 | P1 FB apps reuse the driver Points spine (BApplicationLogic=BPointDeviceExt; BIrmProgram=BFolder) | [CERT] | BApplicationLogic.java:142; BIrmProgram.java:328 |
| 3 | P2 concerns split into domain device-extensions (points/alarms/sched/history/config + irm-specific) | [CERT] | BIrmBacnetDevice.java:638-652; BBacnetDeviceParameters.java:88 |
| 4 | P3 three planes: state on point, frozen config child, separate BStruct wire-map | [CERT] | BSmartSensorConfig.java:46-48; BTC500BacnetConfigObjects.java:1374 |
| 5 | P4 typed folders self-validate via isParentLegal/isChildLegal | [CERT] | BParameterFolder.java:38-44 |
| 6 | P5 frozen skeleton vs dynamic population (frozen sensor points vs dynamic FBs) | [CERT] | BR718ALoraSensor.java:36-38; IRM functionBlockCount=1215 |
| 7 | P6 reuse via Macro/sub-app + Library/Palette; Venom/IRM apps are palette-only | [CERT] | BLibFolder.java:42; honIrmAppl/honeywellVenom*Apps module.xml `<types/>` empty |
| 8 | P7 device managers hold no containers; P8 categories are package/palette only; P9 tags overlay; P10 honIOBase substrate | [CERT] | honeywell*DeviceManager census; BFunctionBlock.java:52; BHonTagDictionary; honIOBase bases |
| 9 | The distilled reference tree (749.3) | [INFER] | synthesis of P1-P10 |

**Tally**: 8 [CERT], 1 [INFER]. No unmarked claims. Depth per family lives in the sweep evidence (SOURCES).

## Connections
- **B7** (the driver spine every pattern reuses), **B737**/**B744** (composition), **B746** (palette templates
  = P6), **B735** (flags/facets that curate these trees), **B77**/**B88**/**B96**/**B109**/**B32** (prior
  Honeywell-module coverage). Forward: **B750** (applying P1-P10 to ColdRoomPan/CompPan/DashboardPan).

## Open gaps
- **B749-G1**: honeywellSpyderTool is 752 classes; only its containment spine was mapped, not the compiler/
  download/resource-manager internals (out of scope — B106/B116 cover the tool). 
- **B749-G2**: the `honst:Application` recursive template semantics (versioning, model-gating) seen in the
  Venom palette — mapped structurally, not its runtime instantiation rules.
