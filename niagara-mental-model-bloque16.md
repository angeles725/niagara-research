# Niagara N4 — Bloque 16: Analytics Framework + Provisioning Service

Parte del mental model. Ver [INDEX.md](INDEX.md). Relacionado con Bloque 8 (Alarm/History/Schedule — BAnalyticAlert implements BIAlarmSource), Bloque 9.3.7 (Analytics Web API overview), Bloque 10.3 (Backup + Station Copier), Bloque 13.1 (Niagara Network federation + Supervisor bottleneck ~50), Bloque 14.15 (Batch Editor vs Provisioning Service scale diff), Bloque 18.5-18.6 (CSRF + HELLO/SCRAM auth).

Este bloque cierra 2 gaps complementarios que habían sido mencionados sin profundizar: (1) **Niagara Analytics Framework** — servicio algorithmic analytics con rules engine + alerts + Web API REST; (2) **Provisioning Service** — orquestación Supervisor-scale sobre N subordinate stations via `BNiagaraNetworkJob` multi-stage.

---

## 16.1 Analytics Framework overview

### 16.1.1 Módulos + clases + jerarquía

**JARs verificados** en install:
- `modules/analytics-rt.jar` — runtime core (service + algorithms + alerts + pollers)
- `modules/analytics-ux.jar` — UI/web (chart widgets, BNaServlet)
- `modules/analytics-wb.jar` — Workbench (editors)
- `modules/lonHoneywellAnalytics-rt.jar` — Honeywell addon MÍNIMO (solo 2 LNML def files para IAQ/CO2 sensors LON)

**Package principal**: `com.tridiumx.analytics` (nótese la **x** en `tridiumx`, NO `com.tridium`). Consistente en todo el framework analytics.

**Clase raíz**: `com.tridiumx.analytics.BAnalyticService` (nótese **SIN 's'** final). Extends `javax.baja.sys.BAbstractService`.

**Ubicación config**: `/Services/AnalyticService` en config.bog (no `/Services/AnalyticsService`).

### 16.1.2 Arquitectura `BAnalyticService`

Sub-componentes children (BComponent folders):
- `BAlertFolder` — contenedor alerts
- `BAlgorithmFolder` — contenedor algorithms
- `BAnalyticDataFolder` — definiciones data fuentes (NEQL queries con tags)
- `BPollerFolder` — pollers de ejecución
- `BAnalyticReportFolder` — reports agregados

Properties verificadas via javap:
- `alerts`, `algorithms`, `definitions`, `pollers` — sub-folders
- `alertCount`, `algorithmCount`, `pointCount`, `proxyExtCount` — contadores
- `caching`, `cacheBuilt`, `cachingStartupDelay` — control de caché
- `threadPriority`, `triggeredPollerConcurrency` — concurrencia
- `useHierarchyCache`, `autoTagAnalyticPoint`, `limitAutoTagging`
- `debugMode`, `missingDataStrategy`, `chartRenderCapacity`
- `subscriptionAlarm` (BAnalyticsSubscription) — tracking de expiry cuando license subscription-based

Tracking global: `AtomicLong aPointCountGL` enforced contra license feature `analytics.point.limit` (Bloque 14.3).

**Thread pools** (privados):
- `POLLER_TPOOL_NAME` — shared pool para BCyclicPoller runnables
- `ASERVICE_TPOOL_NAME` — pool para BTriggeredPoller immediate execute()

### 16.1.3 License gate

Feature `analytics` en Webs.license (Bloque 14.3):
```xml
<feature name="analytics" expiration="2027-03-31"
  alerts="none" algorithms="none"
  algorithm.limit="none" point.limit="none"
  proxyext.limit="none" device.limit="none" alert.limit="none"/>
```

Todos `"none"` = unlimited. En install Honeywell analytics opera sin límites runtime.

---

## 16.2 Algorithm framework

### 16.2.1 `BAlgorithm` base + block types

Clase base: `com.tridiumx.analytics.algorithm.BAlgorithm` extends `BConfigurableObject` implements `Algorithm`, `AnalyticDataPolicy`.

**55+ block types** verificados en module.xml (analytics-rt.jar) — enumerados:

**Data source blocks**:
- `BDataSourceBlock` — lee puntos vivos vía nav
- `BValueTagDataSourceBlock` — acceso vía value tags

**Math blocks**:
- `BBiMathBlock` (binary: sum, sub, mult, div, min, max, power) + `BBiMathOperator` enum
- `BUniMathBlock` (unary: sqrt, log, abs, neg, etc.) + `BUniMathOperator` enum

**Aggregation/Series**:
- `BRollupBlock` — rollup histórico (avg, sum, min, max, stdev, count, median, mode, range, loadFactor)
- `BSlidingWindowBlock` — ventana deslizante
- `BIntervalCountBlock` — conteo en intervalo
- `BConsumptionToDemandBlock` / `BDemandToConsumptionBlock` — conversiones energía (kWh ↔ kW)

**Logic/Filtering**:
- `BLogicFilterBlock`, `BLogicFolderBlock` — lógica booleana
- `BRangeFilterBlock` / `BRangeSwitchBlock` — threshold filtering / on-off por rango
- `BDeadbandFilterBlock` / `BDeadbandSwitchBlock` — histéresis
- `BCovSwitchBlock` — change-of-value trigger (generates boolean on COV)
- `BInvalidValueFilterBlock` — manejo status NaN/null

**Advanced (HVAC específicos)**:
- `BPsychrometricBlock` — cálculos psicrométricos (humedad relativa, entalpia, punto de rocío, bulbo húmedo)
- `BTimeFilterBlock` — activación por rango horario
- `BDayBuilderBlock` — construcción tiempo/fecha
- `BValueDurationBlock` — duración en estado
- `BValueMapBlock` — value mapping con tabla
- `BFunctionBlock` — funciones custom

**String processing**:
- `BStringConcatBlock`, `BStringReplaceBlock`

**Temporal**:
- `BIntersectionBlock` — intersección temporal
- `BRequestOverridesBlock` — override requests
- `BRuntimeBlock` — info runtime

**Constants**:
- `BNumericConstantBlock`, `BBooleanConstantBlock`, `BStringConstantBlock`, `BEnumConstantBlock`

**Output + debug**:
- `BResultBlock` — output pin final del algorithm
- `BDebugBlock` — debugging output

### 16.2.2 Configuration + input/output

Properties del `BAlgorithm`:
- `result` — `BResultBlock` (output final)
- `facets` — `BFacets` (Bloque 4.3.2)
- `makesTrends` — boolean (¿genera trending?)
- `aggregation` — `BCombination`
- `rollup` — `BCombination`
- `minInterval`, `maxInterval` — `BInterval` (ventana tiempo)

Data sources soportados (4 tipos):
- `NavDataSource` — node navigation (lee estado vivo puntos)
- `AlgorithmDataSource` — chaining (output algorithm X → input algorithm Y)
- `AggregateDataSource` — queries a `BHistoryService` (Bloque 8.2) vía `BComplexTimeRange`
- `ValueTagDataSource` — acceso tags

Output modes:
- Trend (histórico): serie temporal persistida
- Value (puntual): valor instantáneo
- Alert trigger: fires alert event

**Trend classes** (`com.tridiumx.analytics.trend`):
- `NiagaraTrend` — wraps history data
- `AggregateTrend`, `IgnoredAggregateTrend` — computed rollups
- `DeltaTrend` — rate-of-change
- `FilteredNiagaraTrend` — apply status filters
- `ImputedTrend` — missing value interpolation (estrategia configurable via `missingDataStrategy`)
- `IntervalTrend` — windowed aggregation

---

## 16.3 Rules engine — pollers + alerts (NO `BRule` explícito)

### 16.3.1 Hallazgo: no existe `BRule`

**Corrección al scope original**: el corpus NO tiene clase `BRule` ni `BAnalyticsRule`. Las "reglas" se modelan como combinación de:
- `BAnalyticPoller` — ejecutor temporal/disparado
- `BAnalyticAlert` — condición + output alarm

### 16.3.2 `BAnalyticPoller` base

Clase: `com.tridiumx.analytics.poll.BAnalyticPoller`

Properties:
- `enabled`, `size` (items registrados), `progress`
- `lastCycle`, `avgCycle`, `maxCycle`, `minInterval` — estadísticas timing
- `resetStatistics()` action
- `cycleComplete` topic (event fired at end of cycle)

Interna: `IdentityHashMap<AnalyticRunnable, AnalyticRunnable> allItems` — queue de ejecutables.

### 16.3.3 2 subclases poller

**`BCyclicPoller`** (time-based):
- Property `rate` (`BRelTime`) — periodo ejecución
- Property `maxInterval` — máximo gap entre ejecuciones
- Implements `Runnable` — ejecuta en thread pool `POLLER_TPOOL_NAME`
- Patrón: "ejecutar cada 15 min"

**`BTriggeredPoller`** (event-based):
- Action `execute()` — invocación manual/programática
- Ejecuta en thread pool `ASERVICE_TPOOL_NAME`
- Patrón: "ejecutar cuando cambio de valor detectado en fuente"

### 16.3.4 Triggering mechanisms

Inferidos de cómo se conectan bloques + pollers:
- **COV trigger**: `BCovSwitchBlock` detecta change-of-value → triggers poller
- **Alarm trigger**: `BAlertMode` define "run when alarm fires en otro punto"
- **Condition-based**: `BLogicFilterBlock` evalúa expression booleana → gate del poller
- **Time-based**: `BCyclicPoller.rate` scheduler interno

### 16.3.5 Persistence

Alerts + algorithms en `config.bog` bajo `/Services/AnalyticService/Alerts/` y `/Services/AnalyticService/Algorithms/`.
Trend data via `getTrend(AnalyticContext)` persistido en history DB (Bloque 8.2) → `stations/{name}/history/*.hdb`.

---

## 16.4 Alert framework

### 16.4.1 `BAnalyticAlert` — clase + lifecycle

Clase: `com.tridiumx.analytics.alert.BAnalyticAlert` extends `BConfigurableObject` **implements `BIAlarmSource`**.

Este implements es crítico — integra **directo** con `BAlarmService` del Bloque 8.1 como alarm source.

Properties:
- `roots` — target node(s) origen del alert
- `exclusions` — nodes excluidos
- `nodeFilter` — NEQL filter expression (Bloque 5.3)
- `nodeCount`, `alertCount` — contadores
- `data` — data fuente (string NEQL)
- `dataFilter` — filter sobre data
- `timeRange`, `aggregation`, `interval`, `rollup`, `totalize` — config temporal
- `alertMode` — `BAlertMode` (threshold, pattern, custom)
- `alarm` — boolean ¿generar alarma acoplada al BAlarmService?
- `alarmClass` — clase alarma Niagara (BHighPriorityAlarm, etc.)
- `alarmMessage` — mensaje alarm
- `sourceName` — BFormat naming
- `missingDataStrategy` — manejo missing values
- `relatedData` — puntos relacionados
- `cost` — métrica computational cost

Lifecycle (patrón estándar Niagara alarm):
1. **Raised** — alert condition detected by poller evaluation
2. **Acknowledged** — `doAckAlarm(BAlarmRecord, Context)` action invoked
3. **Resolved** — condition clears, alert closed

### 16.4.2 Coupling modes con BAlarmService

**Tight**: `alarm=true` + `alarmClass` specified → genera `BAlarmRecord` en `BAlarmService` (Bloque 8.1). Alert fluye por pipeline alarm source → class → recipient igual que cualquier otra alarma.

**Loose**: `alarm=false` → alert vive independiente, no dispara alarma central. Sólo visible en Analytics UI + Web API.

**Subscription**: `WsAnalyticsContext` puede subscribirse a alerts vía Fox subscription para live updates en dashboards.

Actions expuestas:
- `findRelatedData()` — localizar data dependiente
- `invalidateTargets()` — clear cached outputs
- `listNodes()` — enumerar input nodes
- `resyncPoller()` — sync poller subscription
- `ackAlarm()` — acknowledge alarm

---

## 16.5 Analytics Web API — servlet + endpoints

### 16.5.1 Servlets + URL patterns

**Servlet principal**: `com.tridiumx.analytics.ws.BNaServlet` (nótese `tridiumx` + `Na` = "Niagara Analytics"). Default name `na`, URL base `http://{host}/na` o `https://{host}/na`.

**Servlets adicionales** (via web.xml en analytics-rt.jar):
- `aquery` (`com.tridiumx.analytics.util.AnalyticQueryServlet`) → `/aquery/*` — endpoint dedicado queries
- `file` (`com.tridiumx.analytics.chart.AnalyticsChartFileServlet`) → `/file/*` — chart file serving con CSRF filter obligatorio en POST

Configuración Workbench: drag `BWebApi` del palette analytics al nodo `AnalyticService`.

**HTTP methods**:
- GET: `?json={...}` para debug via browser URL
- POST: body JSON + `Content-Type: text/plain` (peculiaridad — NO `application/json`)

### 16.5.2 Autenticación

- **HTTPBasicScheme**: `Authorization: Basic base64(user:pass)` (no HELLO/SCRAM en este endpoint, es Basic auth directo)
- Roles Niagara: `NA_API`, `NA_charts` (permission specific al analytics)
- Session via cookies JSESSIONID (httpOnly, secure)
- **CSRF**: `x-niagara-csrfToken` en POST/PUT/DELETE a `/file/*` (Bloque 18.5)

### 16.5.3 Endpoints — envelope request/response

Formato request general:
```json
{
  "requests": [
    { "message": "<endpoint>", ...params },
    { "message": "<endpoint>", ...params }
  ]
}
```
Batch: múltiples requests en single POST.

Response:
```json
{
  "responses": [
    { "message": "<endpoint>", ...result },
    ...
  ]
}
```

### 16.5.4 Endpoints principales (7 verificados)

**Query** (BQL/NEQL sobre ORD tree):
```json
{"message":"Query","node":"slot:/","query":"bql:select * from nAnalytics:AnalyticsNumericInput"}
```
Response: `columns[]` + `rows[]`.

**GetValue** (single point + status + aggregation):
```json
{"message":"GetValue","node":"slot:/nAnalyticTree/East","data":"hs:power"}
```
Opciones aggregation/rollup: `first, last, min, max, avg, sum, count, median, mode, range, std dev, load factor`.

**GetNode** (tree navigation + metadata):
```json
{"message":"GetNode","node":"slot:/TridiumEMEA/MainPower"}
```
Response: `name`, `icon`, `hasChildren`, `actions[]`, `attributes{}`, `data[]` (available definitions).

**Subscribe** (crea subscription named):
```json
{"message":"Subscribe","name":"mySubName","values":[
  {"message":"GetValue","uid":"temp_01","node":"slot:/...","data":"definition:HVAC/Temperature",
   "tags":["hvac","zone_a"],"tagMode":"all",
   "aggregation":"max","rollup":"average","interval":"oneMinute"}
]}
```

**PollSubscription** (pending events):
```json
{"message":"PollSubscription","name":"mySubName"}
```
Response: solo deltas (changes) desde último poll. Primer poll retorna initial values.

**Invoke** (action REST):
```json
{"message":"Invoke","node":"slot:/Logic/Alarm","action":"override",
 "parameter":{"duration":300000,"value":76}}
```

**GetRollup** (aggregation temporal):
```json
{"message":"GetRollup","node":"hierarchy:/TridiumBuildings","data":"hs:power",
 "rollup":"avg","aggregation":"sum","interval":"oneDay","timeRange":"lastWeek"}
```

Intervals soportados: `oneSecond, oneMinute, fiveMinutes, fifteenMinutes, thirtyMinutes, oneHour, oneDay, oneWeek, oneMonth, oneYear`.

Time ranges: `today, yesterday, thisWeek, thisMonth, thisYear, lastDay, lastWeek, lastMonth, lastYear, custom absolute`.

### 16.5.5 Subscription model — TTL 60 segundos

**Crítico**: subscriptions auto-expire si no polled > 60 seg. Cliente debe poll cada 1-60 seg (recomendado 2-5 seg).

Multiplexing: 1 subscription contiene N uids distintos. Server mantiene cola server-side, return solo deltas.

Limit concurrent: `foxStream.limit` license (Bloque 14.3).

### 16.5.6 Error handling

HTTP codes: 200, 400 (bad JSON), 401 (no session), 403 (no permission), 404, 500.

Application-level error JSON:
```json
{"error":{"code":403,"message":"Access denied: user does not have NA_API role",
          "type":"permissionError"}}
```

Types: `clientError, invalidOrd, permissionError, serverError, unknownSubscription`.

---

## 16.6 Dashboard widgets + client SDK

### 16.6.1 UX chart widgets (analytics-ux.jar)

Base: `BBaseC3UxChart` — wraps C3.js charts. 10+ variants:
- `BAggregationUxChart` — aggregation visualizations
- `BRankingUxChart` — top-N ranking
- `BSpectrumUxChart` — spectrum analysis
- `BRelativeContributionUxChart` — contribution %
- `BLoadDurationUxChart` — load duration curves
- `BAverageProfileUxChart` — load profile
- `BEquipmentOperationUxChart` — HVAC equipment op %
- `BAnalyticsBoundTable` — tabular display
- `BAnalyticsChartFactory` — factory
- `BAnalyticsWebChartJsBuild` — JS build config

**Binding pattern**: widget binds a `/Services/AnalyticService/Algorithms/<name>/output` o `/Services/AnalyticService/Alerts/<name>/alarm`.

**Live updates**: `WsAnalyticsContext` + `WsSubscription$SubscriptionPoller` → Fox subscriptions.

### 16.6.2 BajaScript consumer

```javascript
require(['baja!'], function(baja) {
  baja.getService('AnalyticService').then(function(svc) {
    svc.subscribe('mySub', [{
      uid: 'temp_01',
      node: 'slot:/Sensors/Temperature',
      data: 'definition:HVAC/Temp'
    }]).then(function(sub) {
      setInterval(function() {
        svc.pollSubscription('mySub').then(processResults);
      }, 2000);
    });
  });
});
```

### 16.6.3 External Python client pattern

```python
import requests
payload = {
  "requests":[{
    "message":"Subscribe","name":"dashTemps",
    "values":[{"message":"GetValue","uid":"z01",
               "node":"slot:/Sensors/Zone_01",
               "data":"definition:HVAC/Temperature"}]
  }]
}
resp = requests.post("https://station/na", json=payload,
                     auth=('user','pass'),
                     headers={'Content-Type':'text/plain'})
```

Después poll cada 2s a `PollSubscription` con mismo `name`.

---

## 16.7 Honeywell analytics + Skyspark (findings)

**`lonHoneywellAnalytics-rt.jar`** es MÍNIMO — solo 2 LNML device definitions:
- `lonworks.80 00 0c 0a 46 04 04 01` = `IAQMulti.lnml` (multi-param IAQ)
- `lonworks.80 00 0c 0a 46 04 04 02` = `IAQCo2.lnml` (CO2-específico)

NO aporta algorithms ni analytics específicos. Son mappings LON para IAQ/CO2 sensores Honeywell que se integran con Analytics framework core.

Honeywell probablemente provee templates pre-configurados en `/Services/AnalyticService/Algorithms/` para energy dashboards (ej. `HoneyEnergyConsumptionByZone`) via application templates (Bloque 14.10) — pero **no via módulo analytics dedicado**.

**Skyspark integration**: BÚSQUEDA empírica en corpus no encontró `skyspark*` o `*SkysparkExporter*`. Solo `export-rt.jar` + `exportTags-rt.jar` genéricos. **NO hay connector Skyspark nativo en este install**.

---

## 16.8 Performance + limits Analytics

Features de performance:
- `triggeredPollerConcurrency` (property BAnalyticService) — thread pool size para `BTriggeredPoller` (default inferido 4-8)
- `threadPriority` — Thread.Priority de pollers
- `requestCache` (KeyedCache) — request deduplication
- `chartRenderCapacity` — limit de rendering charts

CPU-intensive ops:
- `BPsychrometricBlock` — psychrometric calculations complejas
- `BRollupBlock` con large history windows
- `AggregateDataSource.query()` sobre 1000s de history points
- Chart rendering en `WsAnalyticsContext`

Memory footprint estimado:
- 10 algorithms concurrent × 100 points each = 1000 subscriptions
- ~1 KB per subscription = ~1 MB baseline

---

## 16.9 Provisioning Service — arquitectura

### 16.9.1 `BProvisioningService` + dependencies

Supervisor-scoped. Ubicado en `/Services/ProvisioningService`. Extends `BAbstractService`.

License gate: feature `provisioning` en supervisor + subscription o license en cada subordinate target.

Dependencies:
- `BNiagaraNetwork` (Bloque 13.1) — federated network de subordinates
- `BBatchJobService` — job orchestration framework (same base que Batch Editor Bloque 14.11)
- `BLicenseService` — license verification + sync
- `PlatformDaemon` — comunicación remota via platform protocol 5011 (Bloque 10.1) + Fox wire (Bloque 13.2)
- `BBackupService` — persistence en stations (Bloque 10.3)

JARs verificados:
- `modules/provisioningNiagara-wb.jar` (Workbench)
- `modules/provisioningNiagara-ux.jar` (UI web)

Factory público: `ProvisioningNiagaraManager.make(BObject base)` retorna interface abstract con:
- `getDeviceNetwork()` — acceso subordinates
- `getBackupManager()` — backup/restore orchestration
- `getStationManager(BDevice station)` — control lifecycle station remota

---

## 16.10 Job framework — `BNiagaraNetworkJob` + stages

### 16.10.1 Estructura 2-stage

`javax.baja.provisioningNiagara.BNiagaraNetworkJob` extends `BDeviceNetworkJob`.

**Stage 1 — Initial** (`BNetworkJobStage`):
- Corre UNA vez por network (no per-station)
- Steps heredan `BNetworkJobStep`
- Uso típico: query server-side (license server, certificate authority)
- Ejemplo: `BUpdateLicensesJobStep` — network-wide license comparison

**Stage 2 — ForEachStation** (`BForEachStationStage extends BForEachDeviceStage`):
- Itera sobre stations target
- Steps heredan `BDeviceJobStep`
- Ejecutables individuales per station
- Si step falla en station X: resto de steps en X skipped, continúa next station (NO aborta job entero)

Métodos builder:
- `addStep(BNetworkJobStep)` — add initial stage step
- `addStep(BDeviceJobStep)` — add foreach stage step
- `getStationState(String stationName)` — query estado station (unknown/success/failed/canceled)
- `setStationState(String, BJobState)` — framework internal

Constructores:
- `BNiagaraNetworkJob()` — empty
- `BNiagaraNetworkJob(String stationName)` — single target
- `BNiagaraNetworkJob(String[] stationNames)` — multiple targets

### 16.10.2 `BForEachStationStage` — scoping + parallel

Herencia: `BForEachStationStage extends BForEachDeviceStage`.

Filtering strategies:
- Todos los subordinates (default)
- Tagged subset (category filter, ej. "HVAC_stations")
- Manual selection (checkbox UI)

Parallel execution heredado de `BForEachDeviceStage`:
- Serial (1 station at a time) — safe, slow
- Parallel N (hasta N concurrent) — fast, alto resource Supervisor
- Config: `provisioning.jobQueue.maxConcurrent` (default ~4)

Métodos:
- `getCombinedSteps(DeviceNetworkJobOp)` — fetch resolved steps array
- `canPassDefaultCheck(BDevice, BDeviceJobStep)` — privilege validation para credential-less execution

---

## 16.11 Step types

### 16.11.1 `BProvisioningBackupStep`

`javax.baja.provisioningNiagara.backup.BProvisioningBackupStep` extends `BDeviceJobStep` implements `BFoxClientConnection$Interest`.

Modos:
- **Online backup**: station running, **excluye `.hdb/.adb`** (Bloque 10.3.3 gotcha) — solo config
- **Offline backup**: station stopped, snapshot completo

Configurables: compression, encryption (mandatory — genera encrypted `.dist` requiere supervisor credentials restore), rotation policy (retention days).

Storage convention verificada:
```
^provisioningNiagara/stationData/{stationName}/backups/backup_{stationName}_{YYYYMMDD_HHmmss.sss}.dist
```

Métodos:
- `makeJob(String stationName)` — factory standalone job
- `makeDetails(BDevice)` — create step details
- `doRun(BBatchJobService, BDeviceStepDetails, BDevice, DeviceNetworkJobOp)` — execute
- `deviceJobStepComplete(...)` — callback
- `getParallelExecutionConflicts(...)` — returns conflicting devices si parallel

### 16.11.2 `BUpdateLicensesJobStep`

`javax.baja.provisioningNiagara.license.BUpdateLicensesJobStep` extends `BNetworkJobStep` (Initial stage). Implements `ICancelHint`, `NiagaraNetworkJobOp$InstallListener`.

Properties:
- `changeBrand` (boolean) — allow brand swap si license server autoriza
- `brandName` (String) — target brand
- `restartRequired` (boolean, N4.12+) — restart stations post-license install

Workflow:
1. Compara installed licenses per station vs latest en license server
2. Single network round-trip al server (efficient)
3. Queue installations para foreach-station stage
4. Handles conflicts: local override vs server master

### 16.11.3 Otros steps

**`BProvisioningRestoreStep`**: reverse de backup. Instala `.dist` a remote station. Requires pre-restore backup (safety). Acceso vía `ProvisioningBackupManager.startRestoreJob(BDeviceStepDetails)`.

**`BProvisioningCopyStep`** / `FileCopyStep`: wraps Station Copier (Bloque 10.3.5) batch-wise. Source → N targets con transform (path replacements, ORD rewrites).

**`BProvisioningUpgradeStep`** / `UpgradeOutOfDateStep`: compara versions en supervisor software DB vs installed en station. Installs higher versions. Dependency resolution.

**`BProvisioningCertificateStep`**: deploy TLS certs renovados a N stations. Target `daemon/security/tls-server.jceks` (Bloque 17.3.2).

**`BProvisioningReport`**: diagnostics aggregation. Collects from N stations → central report.

**Otros componentes**:
- `RebootJobStep` — reboot platform daemon + wait comeback
- `RunRobotStep` — custom program code execution via station `BProgramService`
- `InstallBySpecStep` — software module install per spec
- `SoftwareStationExt` — inventory + upgrade tracking

---

## 16.12 Scheduling + automation

Modos:
- **On-demand**: user trigger via Workbench UI (`NiagaraNetworkJobBuilder`)
- **Scheduled**: `BAbstractSchedule` (Bloque 8.3) + trigger action sobre prototype job
- **Cron-like**: via `TriggerSchedule` (ej. daily 2am backup)
- **Event-driven**: ej. on license expire → `BUpdateLicensesJobStep`

UI plugins:
- `provisioningNiagara-NiagaraNetworkJobBuilder` — one-time job editor
- `provisioningNiagara-NiagaraNetworkPrototypeView` — scheduled job editor
- `provisioningNiagara-PrototypeJobList` — template library

Retention: prototype job history configurable.

---

## 16.13 Rollback + error handling

Políticas:
- **Continue** (default): step falla station X → next station ejecuta. Job completa con warnings.
- **Abort**: step falla → entire job aborta. Menos común.
- **Retry-then-continue**: retry N times, then skip station.

Per-station error log:
- `getStationState(stationName)` → `BJobState` (success/failed/canceled)
- Logs persistidos en BatchJobService history

Rollback automático:
- Si `BProvisioningBackupStep` falla → no intenta restore (no hay nada que restaurar)
- Si `BProvisioningRestoreStep` falla → auto-trigger revert al previous backup
- Manual rollback vía `ProvisioningBackupManager.startRestoreJob()`

---

## 16.14 UI Workbench (plugins)

Plugins view verificados:
- `provisioningNiagara-ProvisioningManager` — main entry point, supervisor-scoped
- `provisioningNiagara-NiagaraNetworkJobList` — tabla subordinates + job status
- `provisioningNiagara-NiagaraNetworkJobBuilder` — one-time job editor (initial + foreach stages)
- `provisioningNiagara-NiagaraNetworkPrototypeView` — prototype template editor + schedule
- `provisioningNiagara-BackupStepDetailsView` — backup history + restore trigger
- `provisioningNiagara-NetworkLicenseSummary` — license sync status
- `provisioningNiagara-SupervisorLicenseManager` — supervisor license database
- `provisioningNiagara-ProvisioningStationDirector` — per-station control (start/stop/reboot)
- `provisioningNiagara-StationJobList` — station-specific job history
- `provisioningNiagara-ProvisioningRobotEditor` — custom code editor

Tabla display típica: station name, status (running/idle), license state, last backup timestamp. Job queue: Running / Queued / Completed tabs.

---

## 16.15 Platform daemon integration

`ProvisioningStationManager.getPlatformDaemon()` da acceso directo al daemon remoto. Métodos:
- `poll()` — health check
- `canStart()`, `canRestart()`, `canReboot()` — preconditions
- `rebootHost()` — reboot platform daemon completo
- `startStation()`, `saveStation()`, `stopStation()`, `killStation()` — station lifecycle control

Autenticación:
- Supervisor platform credentials (user/pass o cert)
- Per-station override credentials opcional
- License `provisioning` verificada pre-job

---

## 16.16 Scale limits + gotchas

Limits:
- Max ~50 subordinates per Supervisor (Bloque 13.1.7 bottleneck)
- Concurrent jobs: `provisioning.jobQueue.maxConcurrent` (default ~4)
- Network bandwidth: ~1-5 Mbps per backup (compression + encryption overhead)
- Backup file size: típicamente 10-100 MB per station (excluye `.hdb`)

Gotchas:
- Backup en producción impacta performance station (I/O contention)
- Encrypted `.dist` requiere supervisor credentials para restore → credential leak risk si supervisor comprometido
- License server connectivity required para `BUpdateLicensesJobStep`; fallback a local DB si off-line
- Default credentials inseguros si shared supervisor — recomendado per-station override
- Job history NO auto-cleanup — retention policy manual necesaria

---

## 16.17 Relación con Station Copier + Batch Editor

Escala creciente:

| Scope | Tool | License | Uso |
|-------|------|---------|-----|
| 1 componente | Property Sheet | — | Config individual |
| N componentes 1 station | Point Manager | — | Comisionamiento |
| N componentes 1 station | Batch Editor | `provisioning` | Updates masivos tabular |
| 1 → 1 station | Station Copier | — | Dev → prod promoción |
| 1 → N stations | BProvisioningCopyStep | `provisioning` | Batch deployment |
| N stations orquestadas | BNiagaraNetworkJob | `provisioning` | Backup/license/cert lifecycle |

Cada tool construye sobre el anterior. `BProvisioningCopyStep` **embed Station Copier** como unidad de ejecución dentro del stage; la orquestación + parallel execution + error handling es del framework provisioning.

---

## 16.18 Workflow end-to-end — 50 HVAC stations

Scenario: Supervisor + 50 subordinates HVAC OptimizerSupervisor-N4.14.0.162.

**Nightly Backup Job** (scheduled 2am):
- Prototype + TriggerSchedule cron `0 2 * * *`
- Initial: `BUpdateLicensesJobStep` (verify licenses)
- ForEachStation: `BProvisioningBackupStep` (offline, compressed, encrypted) + retention 30 días
- Parallel 3 concurrent
- Storage: `^provisioningNiagara/stationData/HVAC-001/backups/backup_HVAC-001_20260422_020000.000.dist`

**License Sync Monthly** (1st, 3am):
- Initial: `BUpdateLicensesJobStep` network-wide query
- ForEachStation: auto-queued installations
- Parallel 5 concurrent

**Emergency Rollback** (on-demand):
- One-time job
- ForEachStation: `BProvisioningRestoreStep` desde last-known-good backup
- Serial (1 station at time)
- Safety: pre-restore confirmation dialog

**Deploy Template** (on-demand):
- 10 stations tagged "template-deployment"
- ForEachStation: `BProvisioningCopyStep` source HVAC-001 → targets con path transform
- Parallel 2 concurrent

**TLS Cert Renewal** (yearly Jan 1):
- All 50 stations
- `BProvisioningCertificateStep` + restart required
- Parallel 5 concurrent

**Execution flow típico nightly backup**:
```
T=02:00 AM: Scheduler triggers prototype
├─ Initial: BUpdateLicensesJobStep
│   └─ Network-wide license query (supervisor → license server)
│       Result: 48/50 valid, 2 expired → WARNING, continue
├─ ForEachStation: Parallel 3 concurrent
│   ├─ Thread 1: HVAC-001 online backup → 15 MB .dist
│   ├─ Thread 2: HVAC-002 (same)
│   ├─ Thread 3: HVAC-003 (same)
│   ├─ Thread N: subsequent after completion
│   └─ HVAC-041, HVAC-042 skipped (no license, step failed)
└─ Job Complete: 48/50 success + log en BatchJobService
```

---

## 16.19 Hallazgos críticos del bloque

1. **Package `com.tridiumx.analytics`** (con **x**) — distinto del estándar `com.tridium.*` del resto del framework. Histórico probable: adquisición o spin-off de módulo.

2. **Clase principal es `BAnalyticService`** (SIN 's' final). Ubicación `/Services/AnalyticService`. Convención naming intencional Tridium.

3. **NO existe clase `BRule` explícita** — el rules engine se modela como `BAnalyticPoller` (cyclic o triggered) + `BAnalyticAlert` (condición con BIAlarmSource). Corrección al scope original del bloque.

4. **`BAnalyticAlert implements BIAlarmSource`** — integración directa con `BAlarmService` del Bloque 8.1. Alerts pueden operar tight-coupled (generando alarm record) o loose (sólo analytics UI).

5. **55+ algorithm block types** catalogados. `BPsychrometricBlock` es HVAC-specific. `BConsumptionToDemandBlock` y su reverse convierten kWh↔kW (energy analytics clásico).

6. **Servlet principal `BNaServlet`** (`Na` = Niagara Analytics) en package `com.tridiumx.analytics.ws`. URL `/na`. Body `Content-Type: text/plain` — peculiaridad que rompe expectativas REST standard `application/json`.

7. **Subscription TTL = 60 seg** — auto-expire si cliente no polls. Requiere poll activo cada 2-5 seg para mantener viva. Pattern distinto de WebSocket true streaming.

8. **HTTPBasicScheme en Analytics Web API** — NO usa HELLO+SCRAM (Bloque 18.6). Solo Basic auth + session cookie. Endpoint diferente del Fox-over-HTTP general.

9. **Roles específicos analytics**: `NA_API`, `NA_charts` — permission system separado del RBAC core (Bloque 11). Fine-grained por feature analytics.

10. **`lonHoneywellAnalytics-rt.jar` es MÍNIMO** — solo 2 LNML device definitions para IAQ/CO2 sensors. NO aporta algorithms Honeywell-específicos. Honeywell entrega templates via application templates (Bloque 14.10), no via módulo analytics dedicado.

11. **NO Skyspark connector** en corpus. Solo `export-rt.jar` + `exportTags-rt.jar` genéricos. Integración Skyspark requiere desarrollo custom o connector de terceros.

12. **Provisioning tiene 2-stage fixed structure** — Initial + ForEachStation. No arbitrary stages. Simplifica mental model.

13. **`BProvisioningBackupStep` online excluye `.hdb/.adb`** — coherente con Bloque 10.3.3 gotcha. Para integridad total usar offline backup.

14. **Storage path convention**:
   ```
   ^provisioningNiagara/stationData/{stationName}/backups/backup_{stationName}_{YYYYMMDD_HHmmss.sss}.dist
   ```
   El prefix `^` indica "station file space" (Bloque 5.1 ORD schemes).

15. **Encrypted `.dist` requires supervisor credentials** — credential leak risk si supervisor comprometido. Operational consideration para hardening.

16. **`BUpdateLicensesJobStep` es Initial stage** — corre UNA vez network-wide (single round-trip al license server). Más eficiente que per-station query.

17. **`BNetworkJobStep` vs `BDeviceJobStep`** — diferencian stage. Initial = Network, ForEach = Device. Error en una stage no cancela la otra.

18. **Parallel execution config**: `provisioning.jobQueue.maxConcurrent` default ~4. Balance entre throughput y resource Supervisor.

19. **`BProvisioningCopyStep` embed Station Copier** (Bloque 10.3.5) como ejecución interna. Batch Editor → Station Copier → ProvisioningCopyStep es escalera de escala.

20. **Default credentials vs per-station override**: default credentials simple pero inseguro. Per-station override recomendado para compartido supervisor (credential segregation).

---

## 16.20 Conexiones con otros bloques

- **Bloque 4.3.2 (Facets)**: BAlgorithm.facets usa `BFacets` para output rendering.
- **Bloque 5.3 (NEQL)**: `BAnalyticAlert.nodeFilter` + `data` son NEQL expressions.
- **Bloque 8.1 (Alarm)**: `BAnalyticAlert implements BIAlarmSource` → integración directa con pipeline alarm.
- **Bloque 8.2 (History)**: `AggregateDataSource` consume `BHistoryService.query()`.
- **Bloque 8.3 (Schedule)**: prototype jobs con `BAbstractSchedule` trigger.
- **Bloque 9.3.7 (Analytics Web API)**: este bloque profundiza lo que ese mencionó a alto nivel.
- **Bloque 10.1 (Platform daemon)**: Provisioning usa `PlatformDaemon` via 5011 para station control.
- **Bloque 10.3 (Backup)**: `BProvisioningBackupStep` + Station Copier integrados.
- **Bloque 11 (RBAC)**: analytics tiene roles adicionales (NA_API, NA_charts) complementarios al RBAC core.
- **Bloque 13.1 (Niagara Network)**: Provisioning orquesta sobre `BNiagaraNetwork` — subordinates federation.
- **Bloque 13.1.7 (Supervisor bottleneck)**: ~50 subordinates aplica aquí por bandwidth + concurrency limit.
- **Bloque 13.2 (Fox wire)**: Provisioning usa Fox para comandos + results transport.
- **Bloque 14.3 (License features)**: feature `analytics` + `provisioning` + limits.
- **Bloque 14.10 (Templates)**: Honeywell entrega analytics templates via application templates, no módulo.
- **Bloque 14.11 (Batch Editor)**: station-scoped sibling del Provisioning Service supervisor-scoped.
- **Bloque 14.15 (Provisioning forward)**: este bloque cierra ese gap.
- **Bloque 17.3.2 (TLS certs)**: `BProvisioningCertificateStep` deploys `daemon/security/tls-server.jceks`.
- **Bloque 18.5 (CSRF)**: `/file/*` Analytics endpoint requires CSRF token.

---

## Engram topic keys

- `niagara/analytics/framework-core-algorithms-pollers-alerts`
- `niagara/analytics/web-api-rest-servlet-subscription`
- `niagara/provisioning/service-niagaranetworkjob-stages-steps`
