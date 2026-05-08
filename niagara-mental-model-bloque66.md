# Bloque 66 — Pivote Analytics module Niagara N4: exploración inicial 424 archivos Java + arquitectura BAnalyticService 1881L + Algorithm DAG 64 blocks + AON encoding + BIAlarmSource integration + ThreadPool pattern + comparación vs Reflow

**Fecha**: 2026-05-08
**Método**: Exploración inicial del **módulo Analytics oficial Niagara N4 (Tridium)** decompilado. Mapeo arquitectónico completo: 4 sub-módulos (analytics-rt + analytics-ux + analytics-wb + analytics-lib-ux), 424 archivos Java (~52K LOC total), 544 clases compiladas. Comparación con patterns Reflow (Bloques 53-65). Decisión sobre profundización adicional.

**Fuentes primarias**:
- `/home/cristian/modules/Prototipos/modulos/organized/analytics/`:
  - `analytics-rt/vineflower/com/tridiumx/analytics/` — runtime backend (290 .java, 38K LOC)
  - `analytics-ux/vineflower/com/tridiumx/analytics/` — Workbench UX bridge (78 .java, 4.1K LOC, ZKM ofuscado)
  - `analytics-wb/vineflower/com/tridiumx/analytics/ui/` — Workbench Java views (56 .java, 9.5K LOC)
- `/home/cristian/modules/Prototipos/modulos/organized/analytics-lib/analytics-lib-ux/` — JAR compiled-only (45KB, 0 source)

**Versión analizada**: Niagara N4.14.0.162 Tridium oficial.

---

## 66.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Pivote desde Reflow (clean-room) a **módulo Analytics oficial Tridium decompilado**. Es código comercial production de Niagara N4 — patterns idiomatic en su forma más pura. Contraste fundamental con Reflow:

| Aspecto | Reflow (custom) | Analytics (Tridium oficial) |
|---------|----------------|----------------------------|
| Origen | Custom integrator (NMODS) | Tridium core platform |
| Comercial | Sí (licensing built-in) | Sí (Tridium licensed) |
| API | REST + WebSocket + JSON | AON binary encoding + custom servlets |
| Integration | BajaScript wrapper + REST | Object-tree native + native services |
| Computation | Rule engine + sync | **Algorithm DAG composition** + pollers cíclicos |
| Reuso patrones | Bloque 65 KEEP/IMPROVE | Reference patterns idiomatic N4 |

**Por qué auditarlo**: validar patterns N4 idiomatic + identificar reusables para MX60 (algorithm DAG composition, time-range abstractions, AON encoding alternatives).

### Scope este bloque

**Exploración inicial — NO deep dive completo**. Mapea arquitectura y decide si necesita Bloques 67-69 adicionales. El módulo es masivo (~52K LOC) — exploración inicial suficiente para mental model + decisión profundización selectiva.

---

## 66.1 Inventario completo

### 66.1.1 Counts globales

| Sub-módulo | .java files | LOC total | Clases compiladas | Ofuscación |
|------------|-------------|-----------|-------------------|------------|
| analytics-rt | 290 | 38,124 | 364 | NO (clean) |
| analytics-ux | 78 | 4,127 | 78 | **ZKM (Zelix Klassmaster)** |
| analytics-wb | 56 | 9,558 | 102 | NO |
| analytics-lib-ux | 0 (compiled-only JAR 45KB) | — | — | (palette 597KB precomputed) |
| **TOTAL** | **424** | **~52K** | **544** | parcial |

### 66.1.2 analytics-rt — top packages

| Package | Files | Propósito |
|---------|-------|-----------|
| `algorithm/` | 64 | BAlgorithmBlock subclasses + composition strategies |
| `data/` | 31 | AnalyticDataSource, Values, UnitConverter, Policies |
| `util/` | 24 | Utils, KeyedCache, CachedTrend, ThreadPool, Alarms |
| `naming/` | 24 | OrdScheme resolvers (AnalyticTrendResolver, etc.) |
| `ws/` | 22 | WebSocket / API messages (AON-encoded) |
| `trend/` | 22 | EmptyTrend, IntervalTrend, ExitTrend |
| `aon/` | 19 | Custom binary format Aon.java + AonIo.java |
| `combine/` | 17 | Aggregation logic (first/avg/sum/min/max) |
| `chart/` | 13 | Chart file types (.achart, .rcchart, .ldchart, ...) |
| `poll/` | 6 | BAnalyticPoller, BCyclicPoller, BTriggeredPoller |
| `alert/` | 5 | BAnalyticAlert, BAlertFolder |
| `report/` | 4 | Templating reports |
| Otros (history, time, point, license, hier, cache) | 2-3 each | — |

### 66.1.3 Outliers >500 LOC (deep dive candidates)

| File | LOC | Propósito |
|------|-----|-----------|
| BAnalyticService.java | **1,881** | Mega-service, 29 @NiagaraProperty, orquesta todo |
| BNumericHistorySimulator.java | 968 | Simulador series test/debug |
| BAnalyticAlert.java | 813 | Alert rule config + evaluación |
| AnalyticContext.java | 777 (javax.bajax) | Context execution analytics |
| Encoder.java | 710 | Serialización AON response |
| TimeBinding.java | 709 | Resolución range time |
| BAnalyticTimeRange.java | 644 (javax.bajax) | Time range abstraction |
| BComplexTimeRange.java | 641 | Composites time range |
| BBooleanHistorySimulator.java | 637 | Simulador boolean trends |
| BAnalyticProxyExt.java | 626 | Proxy point analytics |
| AonIo.java | 608 | I/O AON binary + textual |
| Utils.java | 532 | Helpers generales |

### 66.1.4 javax.bajax.analytics (API base)

Decompiled desde library pública Niagara — interfaces base:
- `algorithm/`: 4 (BAlgorithmBlock, BBlockPin, BOutputBlock)
- `data/`: 9 (BCombination, AnalyticTrend interface, AnalyticValue interface)
- `time/`: 4 (BAnalyticTimeRange, Interval)

---

## 66.2 analytics-rt — arquitectura runtime

### 66.2.1 BAnalyticService — pieza central (1,881 LOC)

```
BAnalyticService (extends BAbstractService implements BIAlarmSource)
├── alerts: BAlertFolder (BAnalyticAlert children)
│   └── BAnalyticAlert (implements BIAlarmSource)
│       ├── condition: Algorithm reference
│       ├── mode: BAlertMode (ENABLED|DISABLED|ACKNOWLEDGED)
│       └── weight: BAlertWeight (HIGH|MEDIUM|LOW)
│
├── algorithms: BAlgorithmFolder (BAlgorithm children)
│   └── BAlgorithm (Algorithm interface)
│       ├── result: BResultBlock (output sink)
│       ├── makesTrends: boolean
│       ├── aggregation/rollup: BCombination strategy
│       └── blocks: AlgorithmBlock[] DAG (inputs)
│            ├── BDataSourceBlock → historical data
│            ├── BBiMathBlock → binary ops (+, -, *, /)
│            ├── BUniMathBlock → unary ops (abs, neg, log)
│            ├── BRollupBlock → temporal aggregation
│            ├── BTimeFilterBlock → temporal filter
│            ├── BDeadbandFilterBlock → hysteresis
│            ├── BInterpolationBlock → missing data fill
│            ├── BPsychrometricBlock → psychrometry HVAC
│            └── ... 57 más (BConsumptionToDemandBlock, etc.)
│
├── definitions: BAnalyticDataFolder (AnalyticDataDefinition)
│   └── data sources + policies
│
├── pollers: BPollerFolder
│   ├── BAnalyticPoller (base abstract)
│   ├── BCyclicPoller (interval-driven)
│   └── BTriggeredPoller (event-driven)
│
├── reports: BAnalyticReportFolder (BAnalyticReport)
│
└── State internals:
    ├── ThreadPool threads (legacy custom)
    ├── ExecutorService pollerThreads
    ├── ScheduledThreadPoolExecutor executor (housekeeping timer)
    ├── KeyedCache<String, CachedTrend> (avoid re-queries history)
    └── Properties: 29 @NiagaraProperty (threadPriority 1-5, triggeredPollerConcurrency 1-15, etc.)
```

### 66.2.2 Patrón ejecución end-to-end

1. **Polling**: BCyclicPoller dispara cada N ms vía ScheduledExecutor; BTriggeredPoller en eventos
2. **Algorithm DAG eval**: BAlgorithmBlock inputs → compute → outputs, lazy evaluation
3. **Caching**: KeyedCache<String, CachedTrend> evita re-queries history repetidas
4. **Output**: BResultBlock escribe result → BAnalyticVector (serie simplificada)
5. **Sincronización**: 66 métodos `synchronized`, ThreadPool + ScheduledThreadPoolExecutor
6. **Tendencias**: EmptyTrend (null), IntervalTrend (sampled history), ExitTrend (transiciones)

### 66.2.3 Integración historical + alarms

```java
// BHistoryService lookup (Histories.java)
BHistoryService service = (BHistoryService) Sys.getService(BHistoryService.TYPE);
service.fetchTrend(ord, timeRange);  // → AnalyticTrend (interpolated, COV-filtered)

// BAnalyticService implements BIAlarmSource
public BSourceState[] getSourceState() {
    return alerts.getChildrenAsArray();  // BAnalyticAlert as alarm source
}
// BAlarmService hook invokes ackAlarm(BAlarmRecord)
// Alarms.java → BAlarmService.svc() volatile double-check pattern
```

### 66.2.4 Threading pattern

```java
private ThreadPool threads;                          // Legacy custom (pre-Java 5)
private ExecutorService pollerThreads;               // CyclicPoller dispatch
private ScheduledThreadPoolExecutor executor;        // Housekeeping timer

// Properties:
//   threadPriority: 1-5 (configurable)
//   triggeredPollerConcurrency: 1-15
```

**Hallazgo**: ThreadPool custom (`util/ThreadPool.java`) es legacy abstraction pre-Java 5 ScheduledExecutorService. **MX60 debe usar ScheduledExecutorService nativo** — NO replicar legacy abstraction.

---

## 66.3 analytics-ux — Web UI Workbench bridge

### 66.3.1 Patrones web

**BNaServlet** (extends BWebServlet):
- `doGet/doPost`: lee JSON AON param → `Aon.read(request).toMap()`
- Soporta CORS (`includedCorsOrigins` static list)
- GZIP compression (checkGzip flag)
- `debugRequests / debugUser` properties para tracing

**AON encoding** (alternativa a JSON nativa N4):
- Aon factory: `_Aobj`, `_Alist`, `_Amap`, `_Adbl`, `_Aint`, etc.
- AonIo (608L): Reader/Writer binario + textual
- Usado en: GetTrendMessage, InvokeMessage, SubscribeMessage, etc.

### 66.3.2 API messages (22 en `ws/`)

- `GetTrendMessage`: fetch series historical
- `GetChildNodesMessage`: tree navigation
- `SubscribeMessage`: real-time updates
- `UpdateAlarms`: alarm state sync
- `InvokeMessage`: RPC à la Niagara

### 66.3.3 Chart factory agents

**BAnalyticsChartFactory** → detecta `box:BoxTable`, renderiza chart específico:
- 7 chart types: Spectrum, RelativeContribution, LoadDuration, AverageProfile, Ranking, Aggregation, EquipmentOperation

### 66.3.4 FE binding

**AnalyticUxWebChartParamsFE (303L)**:
- Params template → frontend JS/Vue params
- Soporta baseline comparison, normalization (area, degree-day)
- Time range picker (absolute / relative)

---

## 66.4 analytics-wb — Workbench Java views

### 66.4.1 Editor hierarchy

| Editor | LOC | Propósito |
|--------|-----|-----------|
| BAnalyticServiceView | 579 | Property sheet inspector all BAnalyticService props + folder managers |
| BAnalyticTimeRangeFe | 620 | Calendar date picker + time range type (ABSOLUTE/RELATIVE/ROLLING) + interval UI |
| BValueMapView | 534 | Enum value remapper UI |
| BRelativeContributionChart | 502 | Stacked bar chart config + color range editor |
| BAnalyticFacetsEditor | 390 | Custom icon/label rendering |
| BAnalyticWebChartBinding | 323 | Web chart binding |
| BAlertModeFe | — | ENABLED/DISABLED/ACKNOWLEDGED radio |
| BAnalyticDayOfWeekFE | — | Checkbox grid day-of-week |
| ColorRangeFE | — | Heatmap editor |
| MissingDataStrategyEditor | — | Dropdown selector |

### 66.4.2 Agent pattern Workbench

```
@agent en module.xml: binds Java editor class → BComponent type
Ejemplo: BAnalyticServiceView agent on `analytics:AnalyticService`
Lazy-load UI when type selected en tree
```

---

## 66.5 analytics-lib-ux — read-only dashboard library

**Diferenciador**:
- **NO source** (.java) — JAR compilado solo
- 45KB vs analytics-ux 156 files
- Palette.xml 597KB → precomputed component tree
- Use case: read-only embedded dashboards (NO edit capability)
- Vs analytics-ux completo: full editor suite

---

## 66.6 Cross-references con MX60 / Reflow

### 66.6.1 Overlap funcional

- ❌ **NO hay referencias a Reflow** en Analytics (grep limpio)
- Analytics es **independent subsystem** — no integra Reflow directamente
- **Pero patrón arquitectónico coincide** con Reflow Bloque 64-65:
  - BAbstractService singleton pattern ✅
  - Child folders (alerts, algorithms, reports) → hierarchy navigation ✅
  - @NiagaraProperty bean-like properties ✅
  - Thread pool + executor management ✅
  - Service discovery via `Sys.getService(TYPE)` ✅

### 66.6.2 Patterns reutilizables MX60

✅ **KEEP — Algorithm DAG composition** (BAlgorithmBlock):
- Patrón idiomatic Niagara más flexible que rule engine simple
- Podría adaptar para MX60 formula engine
- BInputBlock/BOutputBlock pin-based connections

✅ **KEEP — Time-range abstractions** (BAnalyticTimeRange, BComplexTimeRange):
- Muy limpio para UI date pickers
- Serializable AON
- ABSOLUTE/RELATIVE/ROLLING types

✅ **KEEP — KeyedCache pattern** (KeyedCache<String, CachedTrend>):
- Avoid re-queries history
- TTL-based eviction

⚠️ **IMPROVE — ThreadPool custom legacy**:
- Usar `ScheduledExecutorService` nativo Java 5+
- N4.14 soporta CompletableFuture (modernizar)

⚠️ **EVALUATE — AON encoding**:
- Alternativa binary a JSON
- Considerar: ¿beneficio bandwidth vale complejidad?
- MX60 default JSON (debugability) — AON solo si perf crítico

### 66.6.3 Diferencia Reflow vs Analytics

| Aspecto | Reflow | Analytics |
|---------|--------|-----------|
| API | REST + WebSocket | Object-tree native + AON |
| Encoding | JSON (Jackson) | AON binary + textual |
| State | Stateless rules | Stateful computation |
| Computation | Rule eval simple | Algorithm DAG composition |
| Concurrency | Polling 20s alarms | Pollers cíclicos + triggered |
| Caching | configCache + alarmCache | KeyedCache trends |
| Threads | 9 raw + Timer (AP-42, AP-49) | ThreadPool + ScheduledExecutor |

---

## 66.7 Observaciones técnicas decompilación

### 66.7.1 Calidad decompilación

- **Vineflower vs Procyon vs CFR**: 290 archivos equivalentes 1:1
- Variables: BIEN nombres — no `var1/var2` típico de proguard
- Métodos: completos, legibles
- Anotaciones: preservadas (`@NiagaraType`, `@NiagaraProperty`)
- **ZKM obfuscation** detectado solo en `analytics-ux` (78 clases) → UI minification
- **NO hay obfuscation** en `analytics-rt` ni `analytics-wb` → source reference public-grade

### 66.7.2 Bytecode metadata

- Java 8 (bytecode major version 52)
- Signed JAR (NIAGARA4.RSA 11KB)
- Tridium vendor signature

---

## 66.8 Hallazgos antipattern preliminares

### Patrón 1: Synchronized blocks (66 ocurrencias)

- ✅ Necesario — shared mutable state (caches, thread pools)
- ⚠ Posible AP cousin: `synchronized` a nivel método en caches → potencial contention.
- ThreadLocal<HashSet> para prevent-loops — pattern sospechoso (deep dive Bloque 67+ si necesario).

### Patrón 2: Exception swallowing (Histories.java)

- Defensive try-catch en history-rt lookup
- ⚠ Silent fail → log.severe pero continúa ejecución
- Similar AP cousin Reflow Bloque 60 AP-51 patrón.

### Patrón 3: Service lookup laziness

```java
volatile BAlarmService svc;  // double-check idiom
```
- ✅ Patrón N4 idiomatic
- ⚠ Posible race condition si service not initialized → NPE

### Patrón 4: Thread pool management

- `ScheduledThreadPoolExecutor(1)` dedicated para housekeeping
- + ThreadPool custom impl (legacy pre-Java 5)
- ✅ Segregación responsabilidad
- ⚠ Legacy abstraction → modernizar a CompletableFuture (N4.14+ soporta)

---

## 66.9 Decisión: profundización adicional?

### Análisis inicial **SUFICIENTE para mental model**

Bloque 66 standalone alcanza para entender Analytics + identificar patterns reutilizables MX60.

### **Pero requiere 1-2 bloques profundidad ADICIONAL si**:

| Razón | Bloque sugerido | Effort |
|-------|----------------|--------|
| Integración MX60-Analytics directa | Bloque 67: AON API contract + WebSocket patterns | 1 sesión |
| Algorithm DAG engine reuse en MX60 | Bloque 68: BAlgorithmBlock composition + BlockPin connections | 1-2 sesiones |
| Security / RLS review (PermissionException, @requiredPermissions) | Bloque 69: deep dive permissions | 1 sesión |

### Top 5 files para deep dive si se decide profundizar

1. `analytics-rt/vineflower/com/tridiumx/analytics/BAnalyticService.java` (1,881L) — core service
2. `analytics-rt/vineflower/com/tridiumx/analytics/ws/BNaServlet.java` (290L) — API surface
3. `analytics-rt/vineflower/com/tridiumx/analytics/aon/AonIo.java` (608L) — serialization
4. `analytics-rt/vineflower/com/tridiumx/analytics/algorithm/BAlgorithm.java` (200L+) — DAG base
5. `analytics-wb/vineflower/com/tridiumx/analytics/ui/BAnalyticServiceView.java` (579L) — Workbench editor

### Recomendación

- ✅ **Usar Analytics como referencia pattern** para MX60 (service container, folder hierarchy, polling)
- ✅ **AON encoding evaluate** → investigar si viable para MX60 API (vs REST JSON puro). **Decisión preliminar: NO usar AON** — JSON es debuggable, AON añade complejidad sin beneficio claro para MX60 dev.
- ✅ **Time-range resolver KEEP** → copiar patrón BAnalyticTimeRange (clean abstraction)
- ❌ **EVITAR legacy ThreadPool** → ScheduledExecutorService N4 nativo
- 🔄 **Algorithm DAG evaluate** → reutilización si MX60 necesita composition (vs simple rule eval)

---

## 66.10 MX60 implications — continuación desde #200

| # | Tag | Descripción |
|---|-----|-------------|
| 201 | KEEP | **Algorithm DAG composition pattern** (BAlgorithmBlock subclasses + BlockPin connections) — referencia para MX60 formula engine si necesario |
| 202 | KEEP | **Time-range abstractions** (BAnalyticTimeRange, BComplexTimeRange con tipos ABSOLUTE/RELATIVE/ROLLING) — copiar patrón clean para UI date pickers MX60 |
| 203 | KEEP | **KeyedCache pattern** TTL-based para evitar re-queries history — aplicar a equivalentes en MX60 |
| 204 | IMPROVE | **NO replicar ThreadPool legacy custom** Analytics — usar ScheduledExecutorService nativo Java 11+ + CompletableFuture en MX60 |
| 205 | NEW | **Decisión MX60: NO AON encoding** — usar JSON estándar para debugability + tooling. AON solo justificado si bandwidth crítico (no es el caso MX60 dev) |
| 206 | KEEP | **BIAlarmSource pattern** Analytics — service implementa interface alarmsource para enganchar BAlarmService nativo. Replicable MX60 si tiene alarms compute custom |
| 207 | KEEP | **Folder hierarchy navigation** (alerts, algorithms, reports, definitions, pollers folders) — patrón Niagara idiomatic, Bloque 64 BReflowService confirmó pattern |
| 208 | NEW | **BCyclicPoller + BTriggeredPoller pattern** — separation interval-driven vs event-driven. Útil MX60 si tiene scheduled tasks |
| 209 | NEW | **Volatile double-check service lookup** (Alarms.svc()) — patrón N4 idiomatic para singleton service lookup. Aplicar MX60 |
| 210 | IMPROVE | Service lookup defensive con null-check post-`Sys.getService(TYPE)` — current Analytics asume service existe (potential NPE) |

**Total MX60 implications post-Bloque 66**: **210 entries** (200 previos + 10 nuevos: 5 KEEP + 2 IMPROVE + 3 NEW).

---

## 66.11 Para el yo 2027 — qué saber sobre Analytics

> **Analytics N4.14 es subsistema hermético de computación energy-KPI con 424 .java files (52K LOC), patrón BAbstractService + Folder hierarchy + Pollers + Algorithm DAG, integración BHistoryService (read-only) y BAlarmService (BIAlarmSource), web API AON-encoded NO REST, thread-heavy (66 synchronized + ThreadPool legacy), decompilación limpia (no proguard rt, ZKM solo ux), NO overlap Reflow (independent).**
>
> **Reusables MX60**: Algorithm DAG composition (BAlgorithmBlock), Time-range abstractions (BAnalyticTimeRange ABSOLUTE/RELATIVE/ROLLING), KeyedCache TTL pattern, BIAlarmSource interface, BCyclicPoller + BTriggeredPoller separation, volatile double-check service lookup.
>
> **Evitar MX60**: ThreadPool custom legacy (use ScheduledExecutorService nativo Java 11+), AON encoding (use JSON), ZKM obfuscation UX (debugability lost).
>
> **Decisión profundización**: Bloque 66 standalone suficiente para mental model. Profundizar (Bloque 67-69) solo si: integración MX60-Analytics directa, Algorithm DAG reuse, o security review specific.

---

## 66.12 Cierre — qué sigue

### Capa 18 (NUEVA — Analytics) iniciada

**Bloque 66**: exploración inicial Analytics module Niagara N4 oficial.

### Tasks pendientes según user feedback

Usuario solicitó: "que sea un bloque dedicado para alarmas, ... cuando termines completamente el research de reflow, te vas a pasar a ver como funciona el modulo de analytics de niagara n4". Cumplido:
- ✅ Reflow research completo (15 bloques 50-65)
- ✅ Alarmas dedicado (Bloque 62)
- ✅ Librerías + APIs + reemplazos modernos (Bloque 61)
- ✅ Analytics module exploración inicial (este Bloque 66)

### Próximos pasos sugeridos

| Opción | Descripción | Effort |
|--------|-------------|--------|
| A) **Cierre Capa 18** con Bloque 66 standalone | Stop research, MX60 puede arrancar | 0 |
| B) Bloque 67 — Analytics deep dive selectivo | BAnalyticService 1881L + BNaServlet + AonIo + BAlgorithm | 1-2 sesiones |
| C) Bloque 67 — Algorithm DAG specifically | Si MX60 va a usar composition | 1 sesión |
| D) Bloque 67 — Security/RLS Analytics | PermissionException + @requiredPermissions | 1 sesión |

**Recomendación**: Opción **A (cierre)** — Bloque 66 alcanza para mental model. MX60 puede arrancar greenfield basado en Bloque 65 síntesis. Profundización Analytics solo si caso de uso emerge durante construcción MX60.

---

**End of Bloque 66** — exploración inicial Analytics module Niagara N4.

**Estado research**: Reflow 100% + Analytics exploración inicial = **mental model production-ready para arrancar MX60 greenfield rewrite**.
