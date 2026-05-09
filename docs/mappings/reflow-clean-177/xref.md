# Reflow-Clean-177 — Cross-Reference Index

**Schema version**: 1.0  
**Sibling**: `index.json` (mapping)  
**Total entries**: 615  
**Total edges**: 1482  
**Unused symbols**: 227 (36.9%)  
**Generated**: 2026-05-09T13:46:34Z

**Companions**: `xref.json` (canonical), `xref-schema.md`, `xref-README.md`

## Summary by Kind

| Kind | Entries | Edges | Unused |
|------|--------:|------:|------:|
| box-method | 24 | 5 | 19 |
| java-class | 77 | 113 | 20 |
| lib-utility | 10 | 97 | 0 |
| mixin | 18 | 83 | 0 |
| plugin | 13 | 160 | 2 |
| rest-function | 26 | 10 | 17 |
| rest-url | 26 | 14 | 13 |
| store-module | 29 | 222 | 1 |
| vue-component | 378 | 778 | 141 |
| ws-command | 14 | 0 | 14 |

## Top 20 Most-Used Symbols

| Symbol | Kind | Defined At | Usage Count |
|--------|------|-----------|-----------:|
| `ConfigCell` | vue-component | `reflow-frontend/src/components/config/ConfigCell.vue` | 78 |
| `LoadingScreen` | vue-component | `reflow-frontend/src/components/common/LoadingScreen.vue` | 70 |
| `eventBus` | lib-utility | `reflow-frontend/src/lib/eventBus.js` | 62 |
| `$niagara` | plugin | `reflow-frontend/src/plugins/niagara.js` | 53 |
| `IconTip` | vue-component | `reflow-frontend/src/components/common/IconTip.vue` | 32 |
| `$utils` | plugin | `reflow-frontend/src/plugins/utils.js` | 32 |
| `ExpandTransition` | vue-component | `reflow-frontend/src/components/common/ExpandTransition.vue` | 29 |
| `equipment` | store-module | `reflow-frontend/src/store/modules/equipment.js` | 26 |
| `buildings` | store-module | `reflow-frontend/src/store/modules/buildings.js` | 25 |
| `PreferredColorPicker` | vue-component | `reflow-frontend/src/components/common/PreferredColorPicker.vue` | 22 |
| `ConfigCellDelete` | vue-component | `reflow-frontend/src/components/config/ConfigCellDelete.vue` | 22 |
| `$ord` | plugin | `reflow-frontend/src/plugins/ord.js` | 22 |
| `notify` | store-module | `reflow-frontend/src/store/modules/notify.js` | 21 |
| `propertiesMixin` | mixin | `reflow-frontend/src/mixins/propertiesMixin.js` | 21 |
| `Underline` | vue-component | `reflow-frontend/src/components/common/Underline.vue` | 20 |
| `BaseCard` | vue-component | `reflow-frontend/src/components/cards/BaseCard.vue` | 20 |
| `floorEditor` | store-module | `reflow-frontend/src/store/modules/floorEditor.js` | 17 |
| `ConfigButton` | vue-component | `reflow-frontend/src/components/config/ConfigButton.vue` | 16 |
| `floorplans` | store-module | `reflow-frontend/src/store/modules/floorplans.js` | 16 |
| `OrdTree` | vue-component | `reflow-frontend/src/components/common/OrdTree.vue` | 15 |

## Unused Symbols (sample 30 of 227)

| Symbol | Kind | Defined At |
|--------|------|-----------|
| `alarmCanAcknowledge` | box-method | `reflow-frontend/src/api/box.js` |
| `alarmGetActiveCounts` | box-method | `reflow-frontend/src/api/box.js` |
| `alarmGetByUuid` | box-method | `reflow-frontend/src/api/box.js` |
| `alarmGetClasses` | box-method | `reflow-frontend/src/api/box.js` |
| `alarmGetSinceTime` | box-method | `reflow-frontend/src/api/box.js` |
| `alarmGetUnackedCounts` | box-method | `reflow-frontend/src/api/box.js` |
| `alarmGetUuidsForSources` | box-method | `reflow-frontend/src/api/box.js` |
| `alarmQuerySources` | box-method | `reflow-frontend/src/api/box.js` |
| `bformat` | box-method | `reflow-frontend/src/api/box.js` |
| `bqlQuery` | box-method | `reflow-frontend/src/api/box.js` |
| `getAllRoles` | box-method | `reflow-frontend/src/api/box.js` |
| `getLicenseData` | box-method | `reflow-frontend/src/api/box.js` |
| `getNavChildren` | box-method | `reflow-frontend/src/api/box.js` |
| `getUserRoles` | box-method | `reflow-frontend/src/api/box.js` |
| `historyGetData` | box-method | `reflow-frontend/src/api/box.js` |
| `historyGetDeviceTree` | box-method | `reflow-frontend/src/api/box.js` |
| `historyGetGroupNames` | box-method | `reflow-frontend/src/api/box.js` |
| `historyGetQuickList` | box-method | `reflow-frontend/src/api/box.js` |
| `listFiles` | box-method | `reflow-frontend/src/api/box.js` |
| `BReflow` | java-class | `nmodsreflow/nmodsreflow-ux/src/com/niagaramods/nmodsreflow/ux/BReflow.java` |
| `BReflowAlarmCommands` | java-class | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowAlarmCommands.java` |
| `BReflowBQLCommands` | java-class | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowBQLCommands.java` |
| `BReflowCSVCommands` | java-class | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowCSVCommands.java` |
| `BReflowConfig` | java-class | `nmodsreflow/nmodsreflow-ux/src/com/niagaramods/nmodsreflow/ux/BReflowConfig.java` |
| `BReflowFileCommands` | java-class | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowFileCommands.java` |
| `BReflowHistoryCommands` | java-class | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowHistoryCommands.java` |
| `BReflowNavCommands` | java-class | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowNavCommands.java` |
| `BReflowRedirect` | java-class | `nmodsreflow/nmodsreflow-ux/src/com/niagaramods/nmodsreflow/ux/BReflowRedirect.java` |
| `BReflowScheme` | java-class | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowScheme.java` |
| `BReflowUserCommands` | java-class | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowUserCommands.java` |

_Full unused list: `jq '.entries[] | select(.unused) | .symbol' xref.json`_

## Top Consumers per Kind

### java-class (top 5)

| Symbol | Usage | Top User |
|--------|-----:|----------|
| `BReflowService` | 15 | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowScheme.java` |
| `ConfigIO` | 7 | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowService.java` |
| `BackupManager` | 7 | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowService.java` |
| `Query` | 6 | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/AlarmCSVResponse.java` |
| `BReflowWebSocketAcceptor` | 5 | `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowService.java` |

### plugin (top 5)

| Symbol | Usage | Top User |
|--------|-----:|----------|
| `$niagara` | 53 | `reflow-frontend/src/mixins/dynamicColorMixin.js` |
| `$utils` | 32 | `reflow-frontend/src/views/SchedulesHome.vue` |
| `$ord` | 22 | `reflow-frontend/src/mixins/navigationMixin.js` |
| `$reflowLink` | 13 | `reflow-frontend/src/mixins/navigationMixin.js` |
| `$gbo` | 13 | `reflow-frontend/src/mixins/equipmentMixin.js` |

### store-module (top 5)

| Symbol | Usage | Top User |
|--------|-----:|----------|
| `equipment` | 26 | `reflow-frontend/src/components/cards/GroupCardForm.vue` |
| `buildings` | 25 | `reflow-frontend/src/components/alarms/BuildingAlarmSummary.vue` |
| `notify` | 21 | `reflow-frontend/src/App.vue` |
| `floorEditor` | 17 | `reflow-frontend/src/components/floorplans/CanvasForm.vue` |
| `floorplans` | 16 | `reflow-frontend/src/components/buildings/BuildingFloors.vue` |

### mixin (top 5)

| Symbol | Usage | Top User |
|--------|-----:|----------|
| `propertiesMixin` | 21 | `reflow-frontend/src/components/floorplans/ActionsTab.vue` |
| `elementMixin` | 8 | `reflow-frontend/src/components/floorplans/ElementButton.vue` |
| `subscriberMixin` | 7 | `reflow-frontend/src/components/buildings/BuildingStatusDisplay.vue` |
| `profileMixin` | 7 | `reflow-frontend/src/components/profiles/ProfileBanner.vue` |
| `canvasDragResizeMixin` | 7 | `reflow-frontend/src/components/floorplans/ElementButton.vue` |

### lib-utility (top 5)

| Symbol | Usage | Top User |
|--------|-----:|----------|
| `eventBus` | 62 | `reflow-frontend/src/mixins/navigationMixin.js` |
| `uuid` | 13 | `reflow-frontend/src/components/navigation/NavigationList.vue` |
| `utils` | 13 | `reflow-frontend/src/components/buildings/BuildingGroupForm.vue` |
| `AlarmCache` | 3 | `reflow-frontend/src/components/buildings/BuildingMap.vue` |
| `ord` | 1 | `reflow-frontend/src/plugins/ord.js` |

### vue-component (top 5)

| Symbol | Usage | Top User |
|--------|-----:|----------|
| `ConfigCell` | 78 | `reflow-frontend/src/components/alarms/AlarmConsoleForm.vue` |
| `LoadingScreen` | 70 | `reflow-frontend/src/views/SchedulesHome.vue` |
| `IconTip` | 32 | `reflow-frontend/src/components/histories/HistoryStationCache.vue` |
| `ExpandTransition` | 29 | `reflow-frontend/src/main.js` |
| `PreferredColorPicker` | 22 | `reflow-frontend/src/components/navigation/SubnavColors.vue` |

### rest-function (top 5)

| Symbol | Usage | Top User |
|--------|-----:|----------|
| `getAlarmsCsv` | 2 | `reflow-frontend/src/views/AlarmDetails.vue` |
| `updateEquipmentNotes` | 1 | `reflow-frontend/src/components/cards/NoteGrid.vue` |
| `getPointMatrix` | 1 | `reflow-frontend/src/store/modules/pointMapData.js` |
| `getIconSearch` | 1 | `reflow-frontend/src/components/common/IconBrowser.vue` |
| `getIconCategories` | 1 | `reflow-frontend/src/components/common/IconBrowser.vue` |

### box-method (top 5)

| Symbol | Usage | Top User |
|--------|-----:|----------|
| `refreshLicense` | 1 | `reflow-frontend/src/store/modules/license.js` |
| `loadPointMap` | 1 | `reflow-frontend/src/store/modules/pointMapData.js` |
| `historyGetList` | 1 | `reflow-frontend/src/mixins/historyListMixin.js` |
| `historyGetGroupTree` | 1 | `reflow-frontend/src/mixins/historyListMixin.js` |
| `historyGetDevices` | 1 | `reflow-frontend/src/mixins/historyListMixin.js` |

