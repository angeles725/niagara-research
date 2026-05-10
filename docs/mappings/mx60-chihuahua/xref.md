# Cross-Reference Layer — mx60-chihuahua

**Generated**: 2026-05-09
**Total edges**: 88
**Edge schema**: `from_id` → `to_id` via `usage_kind` (evidence: file:lineN)

## Usage kinds

| Kind | Description |
|------|-------------|
| `defines` | File writes a `MX60.*` global to the namespace |
| `reads-global` | File reads a `MX60.*` global written by another file |
| `invokes-java` | UX Java class imports/references an RT Java class (FQN ref) |

## Summary statistics

| Metric | Value |
|--------|-------|
| Total edges | 88 |
| defines edges | 40 |
| reads-global edges | 36 |
| invokes-java edges | 12 |
| UpDetail.js outgoing | 10 |
| index.html outgoing | 3 |

## High-degree nodes (outgoing reads-global)

| File | Outgoing reads |
|------|----------------|
| `UpDetail.js` | 10 (EquipmentDetail, EquipmentData, Router, Toast, Confirm, ModoOverrideStore, OutputOverrideStore, UpThresholdStore, LiveHistoryBuffer, writePoint) |
| `EquipmentCard.js` | 7 (EquipmentData, DashboardApp, Router, StatusResolver, UpThresholdStore, CarcamoThresholdStore, DataloggerThresholdStore) |
| `AlarmsPage.js` | 7 (AlarmsManager, AlarmCards, BulkActionBar, AlarmLatchStore, CsvExport, TimeRangePicker, DashboardApp) |
| `EquipmentData.js` | 7 (SubscriptionPool, ConfigManager, Toast, UpThresholdStore, CarcamoThresholdStore, DataloggerThresholdStore, AlarmLatchStore) |
| `Configuracion.js` | 5 (EquipmentData, ModoOverrideStore, writePoint, Toast, DashboardApp) |

## Edge table (defines)

| From | To | Evidence |
|------|----|----------|
| `AlarmCards.js` | `AlarmCards.js` (self — writes MX60.AlarmCards) | AlarmCards.js:361 |
| `AlarmDetailPage.js` | (writes MX60.AlarmDetailPage) | AlarmDetailPage.js:473 |
| `AlarmDetailsTable.js` | (writes MX60.AlarmDetailsTable) | AlarmDetailsTable.js:371 |
| `AlarmLatchStore.js` | (writes MX60.AlarmLatchStore) | AlarmLatchStore.js:255 |
| `AlarmModalActions.js` | (writes MX60.AlarmModalActions) | AlarmModalActions.js:233 |
| `AlarmNotesModal.js` | (writes MX60.AlarmNotesModal) | AlarmNotesModal.js:244 |
| `AlarmsManager.js` | (writes MX60.AlarmsManager) | AlarmsManager.js:313 |
| `AlarmsPage.js` | (writes MX60.AlarmsPage) | AlarmsPage.js:816 |
| `BulkActionBar.js` | (writes MX60.BulkActionBar) | BulkActionBar.js:104 |
| `CarcamoDetail.js` | (writes MX60.CarcamoDetail) | CarcamoDetail.js:1031 |
| `CarcamoThresholdStore.js` | (writes MX60.CarcamoThresholdStore) | CarcamoThresholdStore.js:203 |
| `ConfigManager.js` | (writes MX60.ConfigManager) | ConfigManager.js:132 |
| `Configuracion.js` | (writes MX60.ConfiguracionPage) | Configuracion.js:527 |
| `Confirm.js` | (writes MX60.Confirm) | Confirm.js:112 |
| `DashboardApp.js` | (writes MX60.DashboardApp, MX60.PageStub) | DashboardApp.js:309 |
| `DataloggerDetail.js` | (writes MX60.DataloggerDetail) | DataloggerDetail.js:696 |
| `DataloggerThresholdStore.js` | (writes MX60.DataloggerThresholdStore) | DataloggerThresholdStore.js:194 |
| `EquipmentCard.js` | (writes MX60.EquipmentPage) | EquipmentCard.js:637 |
| `EquipmentData.js` | (writes MX60.EquipmentData) | EquipmentData.js:393 |
| `EquipmentDetail.js` | (writes MX60.EquipmentDetail, MX60.DetailPage) | EquipmentDetail.js:157 |
| `EquipmentSnapshotStore.js` | (writes MX60.EquipmentSnapshotStore) | EquipmentSnapshotStore.js:325 |
| `HomeMap.js` | (writes MX60.HomePage) | HomeMap.js:881 |
| `LiveHistoryBuffer.js` | (writes MX60.LiveHistoryBuffer) | LiveHistoryBuffer.js:200 |
| `ModoOverrideStore.js` | (writes MX60.ModoOverrideStore) | ModoOverrideStore.js:58 |
| `OutputOverrideStore.js` | (writes MX60.OutputOverrideStore) | OutputOverrideStore.js:85 |
| `ParticleAnimation.js` | (writes MX60.ParticleAnimation) | ParticleAnimation.js:160 |
| `Router.js` | (writes MX60.Router) | Router.js:159 |
| `ScheduleView.js` | (writes MX60.ScheduleView) | ScheduleView.js:530 |
| `StatusResolver.js` | (writes MX60.StatusResolver) | StatusResolver.js:43 |
| `SubscriptionPool.js` | (writes MX60.SubscriptionPool) | SubscriptionPool.js:570 |
| `TimeRangePicker.js` | (writes MX60.TimeRangePicker) | TimeRangePicker.js:148 |
| `Toast.js` | (writes MX60.Toast) | Toast.js:116 |
| `UpDetail.js` | (writes MX60.UpDetail, MX60.HistoryIndex, MX60.HistoryListCache) | UpDetail.js:3830 |
| `UpThresholdStore.js` | (writes MX60.UpThresholdStore) | UpThresholdStore.js:188 |
| `WritePoint.js` | (writes MX60.writePoint) | WritePoint.js:151 |
| `CsvExport.js` | (writes MX60.util.CsvExport) | CsvExport.js:19 |
| `Dropdown.js` | (writes MX60.util.Dropdown) | Dropdown.js:21 |
| `Popover.js` | (writes MX60.util.Popover) | Popover.js:21 |
| `RelativeTime.js` | (writes MX60.util.RelativeTime) | RelativeTime.js:24 |

## Key dependency chains

```
index.html → DashboardApp → ConfigManager
                         → Router
DashboardApp → ConfigManager (config fetch)

AlarmsPage → AlarmsManager → ConfigManager
                           → AlarmLatchStore
           → AlarmCards → AlarmDetailsTable → RelativeTime
                        → AlarmNotesModal → Toast
           → BulkActionBar
           → TimeRangePicker → Dropdown

UpDetail → EquipmentDetail → EquipmentData → SubscriptionPool → ConfigManager
                                           → ThresholdStores (3x)
                                           → AlarmLatchStore
         → LiveHistoryBuffer
         → ModoOverrideStore / OutputOverrideStore / UpThresholdStore
         → writePoint → Toast
         → Confirm

EquipmentCard → EquipmentData → SubscriptionPool
              → StatusResolver → AlarmLatchStore
              → ThresholdStores (3x)
```

## Java invokes-java edges

| From (UX) | To (RT) | Evidence |
|-----------|---------|----------|
| `BChiServlet.java` | `BChiDashboardService.java` | BChiServlet.java:739 |
| `BChiServlet.java` | `BChiUp.java` | BChiServlet.java:1055 |
| `BChiServlet.java` | `BChiCarcamo.java` | BChiServlet.java:1324 |
| `ChiScheduleHelper.java` | `BChiUp.java` | ChiScheduleHelper.java:3 |
| `ChiEquipmentReader.java` | `BChiUp.java` | ChiEquipmentReader.java:1 |
| `ChiEquipmentReader.java` | `BChiCarcamo.java` | ChiEquipmentReader.java:1 |
| `ChiEquipmentReader.java` | `BChiDatalogger.java` | ChiEquipmentReader.java:1 |
| `ChiThresholdHelper.java` | `BChiUp.java` | ChiThresholdHelper.java:1 |
| `ChiThresholdHelper.java` | `BChiCarcamo.java` | ChiThresholdHelper.java:1 |
| `ChiThresholdHelper.java` | `BChiDatalogger.java` | ChiThresholdHelper.java:1 |
| `ChiAlarmHelper.java` | `BChiUp.java` | ChiAlarmHelper.java:1 |
| `ChiServletDispatch.java` | `BChiDashboardService.java` | ChiServletDispatch.java:1 |
