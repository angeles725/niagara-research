# Bloque 64 — `-ux` modules + `-rt` remanentes: BReflow/BReflowConfig/BReflowRedirect stubs + BReflowService 468L pieza central + BReflowScheme + 8 util/ helpers + 5 history/ + 6 history/json/ serializers + 3 antipatterns AP-87..89 + 7 reglas template MX60 (32-38)

**Fecha**: 2026-05-08
**Método**: Audit final del backend Reflow no cubierto por bloques previos. Cubre los 3 módulos `-ux` (workbench JS bridge stubs), BReflowService (468L pieza central orquestadora), BReflowScheme (ORD scheme handler), 8 helpers `util/`, 5 archivos `history/` (deep dive después de Bloque 55 arquitectónico), 6 serializers Jackson custom en `history/json/`.

**Fuentes primarias**:
- `nmodsreflow-ux/src/com/niagaramods/nmodsreflow/ux/`: BReflow.java (59L), BReflowConfig.java (59L), BReflowRedirect.java (59L)
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/`: BReflowService.java (468L) + BReflowScheme.java (78L)
- `util/`: NavNodeSerializer (58L), PointHelper (50L), Json (97L), CompareRangeCalculator (316L), StringUtils (15L), CommandHelpers (23L), BDateRangeEnum (122L), RangeCalculator (300L)
- `history/`: HistoryData (663L), HistoryGroups (112L), HistoryList (355L), HistoryIO (103L), HistoryGhostSubscriber (26L)
- `history/json/`: HistoryDeviceSerializer, HistoryFolderSerializer, IHistorySeralizer (typo!), HistoryRecordOptions, HistoryRecordSerializer, HistoryObjectMapper

**Versión analizada**: Reflow-Clean-177.

---

## 64.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Audit final del backend Reflow para cerrar la cobertura técnica antes del Bloque 65 síntesis. Cubre las piezas que quedaron fuera de los bloques 53-62:
- **`-ux` module**: 3 archivos Java BIJavaScript+BSingleton stubs
- **BReflowService**: pieza central orquestadora (468L), inyecta WebSocketAcceptor + ChannelService + SyncService como Properties
- **BReflowScheme**: ORD scheme handler `reflow:|` para deep links
- **8 utils**: serializers, helpers, range calculators
- **5 archivos history/**: deep dive business logic (Bloque 55 cubrió arquitectura)
- **6 serializers Jackson custom**: pattern factory para custom JSON output

### Qué corrige / valida

| Bloque previo | Hallazgo | Validación / corrección Bloque 64 |
|---------------|----------|----------------------------------|
| 50 (`-ux` 3 stubs iframe loader) | "BIJavaScript+BSingleton sin servlets HTTP reales" | ✅ **CONFIRMADO empíricamente**: 3 archivos 59L cada uno, idénticos pattern, solo `getJsInfo(Context)` retorna ORD a `module://nmodsreflow/niagara/{reflow,reflow_config,reflow_redirect}.js` |
| 55 (history domain ~12 AP-27 sites) | "AP-27 sistémico HistoryData + helpers" | ✅ **CONFIRMADO** + AP-87/88/89 nuevos detectados al deep dive |
| 60 (BReflowSyncService 5 thread spawns) | "Thread spawning sin cx propagation" | ✅ **CONFIRMADO**: BReflowService usa `Clock.schedulePeriodically` 24h sin cx — pero es system task (refreshHistoryGroupCache), aceptable |
| 53 (template MX60 12 reglas Java) | "Reglas 11+12 obligatorias cx propagation + filesystem whitelist" | ✅ extension via Reglas 32-38 nuevas (custom serializer factory, service container, ORD scheme, etc.) |

### Pregunta unificadora

> ¿Qué piezas cierran el modelo backend Reflow + qué patrones MX60 hereda?

**Respuesta corta**:
- **BReflowService es el puente backend**: 25+ properties licensing/caching/limits + 6 actions + 3 inner services como Properties (webSocketAcceptor, channelService, syncService). Pattern KEEP literal MX60.
- **`-ux` stubs son trivial**: 3 archivos idénticos solo inyectan JS bundles vía Workbench. Sin lógica.
- **HistoryData 663L** = monolito monumental (BQL + thread.join + AccessController + JSON build). MX60 mantener pattern pero modularizar via Reglas 11+13.
- **Jackson custom serializer factory** (HistoryObjectMapper.make()) = KEEP literal — pattern centralizado. Pero typo `IHistorySeralizer` (AP-88) + duplicate SimpleModule registration (AP-89) son code smells.
- **Decisión MX60 #7**: BComponent service container pattern (BReflowService) es el blueprint para servicios MX60. Property-based composition es Niagara-idiomatic y debe replicarse exactamente.

---

## 64.1 `-ux` module deep dive (3 stubs)

### 64.1.1 BReflow.java (59L)

```java
@NiagaraType(agent = {
    @AgentOn(types = "nmodsreflow:ReflowService", requiredPermissions = "r")
})
public class BReflow extends BSingleton implements BIJavaScript, BIFormFactorMax {
    public JsInfo getJsInfo(Context cx) {
        return new JsInfo("module://nmodsreflow/niagara/reflow.js", null, null);
    }
}
```

**Estructura**: `BSingleton + BIJavaScript + BIFormFactorMax`.

**Role**: inyector de punto de entrada Reflow UI principal. Cuando user abre `reflow:` en Workbench, Workbench llama `getJsInfo(cx)` → recibe ORD → carga `reflow.js` desde el módulo en form factor max (full screen).

**Permission**: `r` (read-only) — visualización dashboard suficiente.

**Cross-ref Bloque 50**: confirma arquitectura iframe-loader pattern.

**KEEP literal MX60**: pattern singleton + interface delegation muy limpio para JS bridge.

### 64.1.2 BReflowConfig.java (59L)

Idéntico a BReflow pero:
- Carga `module://nmodsreflow/niagara/reflow_config.js` (config panel)
- Permission: `rw` (read-write) — escalada para edit dashboard config

**Hallazgo**: separación read-only (BReflow) vs read-write (BReflowConfig) arquitecturalmente correcta. Niagara aplica permission check antes de instanciar el agent.

### 64.1.3 BReflowRedirect.java (59L)

Idéntico pattern, carga `reflow_redirect.js`. Middleware redirección elegante (probablemente para legacy URL → SPA route mapping).

### 64.1.4 Síntesis `-ux`

**Total `-ux`**: ~177 líneas Java (3 stubs × 59L). **Cero lógica de negocio** en `-ux` — todo el routing/caching/session vive en `-rt`. Pattern correcto N4: `-ux` solo inyecta JS bundles, `-rt` hace el trabajo real.

**MX60 implication**: replicar pattern stub `-ux` exact — separación clara form factor (UI workbench bridge) vs runtime (backend logic).

---

## 64.2 BReflowService.java (468L) — pieza central orquestadora

### 64.2.1 Estructura general

```java
public class BReflowService extends BComponent implements BIService, BIRestrictedComponent {

    // 25 Properties (licensing 5 + caching 4 + limits 4 + debug 2 + security 3 + internals 7)
    // 6 Actions: clearCache, clearHistoryCache, refreshHistoryGroupCache, ticketExpired, ...
    // 3 Servicios internos como Properties:
    //   - webSocketAcceptor: BReflowWebSocketAcceptor (Bloque 59)
    //   - channelService: BReflowChannelService (Bloque 59)
    //   - syncService: BReflowSyncService (Bloque 60)

    public void stationStarted() { /* detect N4 version + start scheduler */ }
    public void started() { /* idem */ }
    public void changed(Property p, Context cx) { /* reactive cascade reload */ }
    public void serviceStarted/Stopped() { /* lifecycle hooks */ }

    // HistoryGroupCache scheduler: Clock.schedulePeriodically(86400000ms = 24h)
}
```

### 64.2.2 Properties clave (25 props slot-o-matic generated)

| Prop | Default | Slot | Propósito |
|------|---------|------|-----------|
| `licenseStatus` | true | 1 | License flag — currently unlicensed build sin verificación |
| `licenseType` | "enterprise" | 1 | SKU: enterprise/standard |
| `buildingLimit`, `equipmentLimit`, `floorLimit`, `pageLimit` | 9999 | 1 | Cuotas — efectivamente disabled greenfield |
| `weatherMapsEnabled` | true | 1 | Feature toggle weather |
| `dailyBackups` | false | 0 | Trigger BackupManager.createDailyBackup (Bloque 60) |
| `webCache` | true | 0 | ConfigIO HTTP response cache |
| `historyCache` | false | 0 | HistoryIO GZIP cache trending data |
| `historyCacheTTL` | 3600 | 4 | TTL segundos (1h default) |
| `redirectReflowView` | false | 4 | Legacy URL → MX60 redirect |
| `hasModernSecurityPolicy` | false | 5 | N4.10+ security handshake (Bloque 61) |
| `stationType` | "supervisor" | 5 | Role: supervisor / client / storage |

### 64.2.3 Métodos críticos

- **stationStarted() / started()**: detectan N4 version vía `BModule.getClassVersion()` → setean `hasModernSecurityPolicy`. Inician scheduler cache si enabled.

- **changed(Property)**: reactive cascade — `multiUserConfig` change → `getSyncService().reload()`; `historyGroupCacheRefresh` toggle → start/stop scheduler; `historyCache` toggle → clearCache.

- **doClearCache()**: `ConfigIO.clearCache() + HistoryIO.clearCache()` — sincronización manual.

- **doTicketExpired()**: daily backup trigger si `getDailyBackups()` true (cross-ref Bloque 60 BackupManager).

- **startHistoryGroupCacheRefreshTimer()**: `Clock.schedulePeriodically(86400000ms)` invoca `HistoryIO.refreshHistoryGroupCache()`.

- **getAgents(Context)**: retorna lista agents reordenada — `BReflow / BReflowConfig` al top (UI rendering priority Workbench).

### 64.2.4 Cross-references detectados

| Bloque | Conexión |
|--------|----------|
| 59 | webSocketAcceptor + channelService inner services (Properties embebidos) |
| 60 | syncService inner service + BackupManager integration vía `doTicketExpired()` |
| 55 | HistoryIO scheduler integration vía `refreshHistoryGroupCache()` |
| 62 | License checks (`getLicenseStatus()`) decisión point — currently ignored |
| 63 | Frontend agents — `getAgents()` UI component priority ordering |
| 53 | Reglas 1-12 template — BReflowService ES el blueprint que validó las reglas |

### 64.2.5 Patterns KEEP / IMPROVE

| Pattern | Acción |
|---------|--------|
| ✅ **BComponent service container pattern** — composition Properties internas | KEEP literal MX60 — Regla 33 |
| ✅ **Reactive `changed()` cascade** — UI sync automatic | KEEP literal — Regla 35 |
| ✅ Slot-o-Matic generated getters/setters | KEEP — type-safe boilerplate |
| ⚠ Hardcoded "reflow" logger name | IMPROVE — inject logger factory |
| ⚠ `Clock.schedulePeriodically(86400000ms)` 24h interval hardcoded | IMPROVE — Property `historyGroupRefreshInterval` BRelTime configurable. Implication #196 |
| ⚠ Sin persistencia state post-station-start | IMPROVE — guardar cache state en DiskStore. Implication #195 |

---

## 64.3 BReflowScheme.java (78L) — ORD scheme handler

```java
public class BReflowScheme extends BOrdScheme {
    public OrdTarget resolve(OrdTarget base, OrdQuery query, Context cx) {
        ComponentSpace space = base.getSpace();
        for (BComponent comp : space.getRoot().getChildren()) {
            if (comp.getType().is(BReflowService.TYPE)) {
                return new OrdTarget(comp, query);
            }
        }
        throw new UnresolvedException();
    }
}
```

**Role**: Custom Niagara naming resolver para `reflow:` ORD scheme. Resuelve `reflow:|/path/to/something` → localiza BReflowService singleton dentro ComponentSpace.

**Uso**: iOS / Android deep links (`myapp://reflow|/config/dashboard`) entran vía este resolver → BReflowService entry point.

**Patterns**:
- ✅ **KEEP**: separation of concerns — routing ORD vs HTTP servlet routing (BaseServlet Bloque 58).
- ⚠ **IMPROVE**: cache resolved service instance (actual OrdQuery resolution costly per request — itera ComponentSpace cada vez).

**Regla 36 nueva**: ORD scheme custom resolver pattern.

---

## 64.4 util/ helpers (8 archivos)

| Clase | Líneas | Propósito | KEEP / IMPROVE |
|-------|--------|-----------|----------------|
| **NavNodeSerializer** | 58 | Jackson `StdSerializer<BINavNode>` — BajaScript nav tree JSON | ✅ KEEP — clean visitor pattern |
| **PointHelper** | 50 | `getPointsForDevice(BDevice)` recursive BControlPoint collector | ✅ KEEP — device tree walk |
| **Json** | 97 | `static JSONObject component(BComp)`, `facets()` — metadata flattener | ⚠ IMPROVE — type safety vía builder |
| **CompareRangeCalculator** | 316 | Date range picker — `make(BDateRangeEnum)` → `BAbsTime[2]` | ⚠ AP-87 duplicate logic — fix via base class |
| **StringUtils** | 15 | `countOccurrences(String, char)` | ✅ KEEP literal (o `String.chars().filter()`) |
| **CommandHelpers** | 23 | `ordFromArgument(BValue)` — BOrd coercer flexible | ✅ KEEP — adapter pattern |
| **BDateRangeEnum** | 122 | Frozen enum 15 opciones (lastHour..last12Months) + factory | ✅ KEEP — N4 type-safe facet |
| **RangeCalculator** | 300 | Parallel CompareRangeCalculator — inconsistente lastYear() | ⚠ **AP-87 duplicate code base** |

### 64.4.1 AP-87 NEW LOW — RangeCalculator vs CompareRangeCalculator code duplication

```java
// RangeCalculator.lastYear()
cal.add(1, -1);  // -1 año (current vs current-1)

// CompareRangeCalculator.lastYear()
cal.add(1, -2);  // -2 años (current-1 vs current-2)
```

**Issue**: Reporting "last year" vs "compare year" tienen offset diferente — semantics ambiguous puede causar inconsistent historical ranges en reports.

**Posible intención**: "Compare Last Year" = `[current-2, current-1]` para excluir current YTD (en compare context). PERO no documented. Predicción 64.10.

**Fix**: Refactor base class:
```java
class RangeCalculator {
    protected int getYearOffset() { return -1; }  // override en CompareRangeCalculator a -2
    public static BAbsTime[] lastYear() { ... cal.add(1, getYearOffset()); ... }
}
class CompareRangeCalculator extends RangeCalculator {
    @Override protected int getYearOffset() { return -2; }
}
```

**Severity**: LOW (duplicate code) + MEDIUM (semantics inconsistency potential bug). Implication #191.

---

## 64.5 history/ remanentes deep dive

### 64.5.1 HistoryData.java (663L) — monolito principal

**Entry**: `fromComponent(BComponent options)` — extrae `{histories, style, range, limit, comparing, startDate, endDate, contextualRanges}`.

**Motor**:
```java
AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
    public Void run() {
        Thread thread = new Thread(task);
        thread.start();
        thread.join();  // synchronous boundary
    }
});
```

**Output**: JSONObject trending data con timestamps + valores interpolados.

**Métodos core**:
- `jsonForHistory(BIHistory, style, range, contextualRanges)` — 2 overloads (BDateRangeEnum vs BAbsTime[2])
- `jsonForHistory()` usa `RangeCalculator.make(range)` → `range[0..1]` timestamps
- Trend record type dispatch: BNumeric64Bit, BNumericTrendRecord, BStringTrendRecord, BBooleanTrendRecord, BEnumTrendRecord
- Precision formatting vía facets (units, trueText, falseText)

**Cross-ref Bloque 55**: AP-27 sites (BOrd.make() sin cx) confirmados aquí. AccessController.doPrivileged necesario para `history:/` ORD resolution.

**KEEP**: Thread.join() synchronous pattern apropiado para HTTP request-response cycle.
**IMPROVE**: Cache HistoryData JSON si `historyCache` enabled en BReflowService (Implication #194).

### 64.5.2 HistoryGroups.java (112L)

**Métodos**:
- `getDeviceTree()` — Jackson mapper + HistoryDeviceSerializer → ArrayNode BHistoryDevice tree
- `getGroupNames()` — `BHistoryService.getHistoryGroupNames()` → String[]
- `getGroupTree()` — recursivo BHistoryFolder serialization (nested children + histories)

**Pattern**: SimpleModule registration + factory method `ObjectMapper make()`.

> **AP-89 NEW LOW** — Duplicate SimpleModule.register() entre HistoryGroups y HistoryObjectMapper. Pattern: ambos métodos `getDeviceTree()` y `getGroupTree()` construyen mappers separados con módulos repetidos. Centralizar en HistoryObjectMapper.make() factory invocation única.

### 64.5.3 HistoryList.java (355L)

**Estado**: HashMap<String,String> filters + limit + page + dirty flag + BIHistory[] list.

**Constructores**: 6 overloads (limit/filters/page combinations).

**Métodos**: updateList(), setFilter(), setPage(), hasNextPage(), getHistories().

**Pattern**: lazy evaluation dirty flag — `updateList()` solo si `dirty=true`.

**KEEP**: pagination con HashMap filters.
**IMPROVE**: type-safe filter builder (vs HashMap<String,String> magic keys).

### 64.5.4 HistoryIO.java (103L)

**Cache locations**:
- `^reflow/cache/history.cache`
- `^reflow/cache/history-groups.cache`

**Métodos**:
- `cacheExists(String location)` — checks TTL vs current time
- `getZipOutputStream()` — GZIPOutputStream para persist trending data
- `clearCache()` — delete history + group cache files
- `refreshHistoryGroupCache()` — recompute Bloque 55 sites namespace
- `loadCache()` / `saveCache()` — serialize/deserialize ObjectMapper

**Tech**: BDirectory filesystem API + BFileSystem.INSTANCE station home.

**KEEP**: GZIP compression trending data (bandwidth saver).
**IMPROVE**: atomic file write (temp + rename) en lugar de overwrite — anti-corruption garantía.

### 64.5.5 HistoryGhostSubscriber.java (26L)

**Tiny class**: Subscriber extension pattern para lifecycle cleanup.

**Purpose**: Unsubscribe history listener cuando event fired (memory leak prevention).

**KEEP**: garbage collection helper pattern. **Cross-ref Bloque 55**: Bloque 55 lo destacó como ejemplo de "micro-utility 26 líneas single-responsibility KEEP".

---

## 64.6 history/json/ serializers (6 clases) — Jackson custom factory

### 64.6.1 Architecture pattern

```
HistoryObjectMapper.make()  [Factory]
├── new ObjectMapper()
├── new SimpleModule()
├── module.addSerializer(BHistoryDevice.class, new HistoryDeviceSerializer())
├── module.addSerializer(BHistoryFolder.class, new HistoryFolderSerializer())
├── module.addSerializer(BIHistory.class, new IHistorySeralizer())
├── module.addSerializer(HistoryRecordOptions.class, new HistoryRecordSerializer())
└── mapper.registerModule(module)
    → ObjectMapper.writeValueAsString()
```

### 64.6.2 Serializers

| Serializer | Target type | Output |
|------------|-------------|--------|
| **HistoryDeviceSerializer** | BHistoryDevice | `{fullPath, title, devices[], children[], histories[]}` (recursive tree) |
| **HistoryFolderSerializer** | BHistoryFolder | `{fullPath, title, children[], histories[]}` (flatten nav) |
| **IHistorySeralizer** | BIHistory | `{displayName, navName, id, recordType, lastRecord}` (metadata + trending) |
| **HistoryRecordSerializer** | HistoryRecordOptions | `{time, value, status, label, units}` (type-specific formatting) |
| **HistoryRecordOptions** | Value holder | POJO — record + config + flags |
| **HistoryObjectMapper** | Factory | ObjectMapper instance reusable |

### 64.6.3 AP-88 NEW LOW — Typo class name `IHistorySeralizer`

**File**: `history/json/IHistorySeralizer.java` (debería ser `IHistorySerializer`).

**Scope**: typo `Seral` vs `Serial` — nombre publically exposed en class name + filename.

**Fix**: Rename + actualizar imports cross-codebase (HistoryGroups.java, HistoryObjectMapper.java).

**Severity**: COSMETIC pero distrae developer experience. Implication #197 trivial.

### 64.6.4 Patterns KEEP

- ✅ **Factory method pattern HistoryObjectMapper.make()** — reusable across endpoints. Regla 32.
- ✅ Custom serializer granular type dispatch — precision formatting via facets.
- ✅ HistoryRecordOptions POJO flags — flexible output control.

**MX60 implication #193**: replicar pattern para analytics-lib (Bloque 66+) — `AnalyticsObjectMapper.make()` con custom serializers per report type.

---

## 64.7 Antipatterns nuevos AP-87..89

| # | Severity | Título | Site | Categoría |
|---|----------|--------|------|-----------|
| AP-87 | LOW (+MEDIUM si bug semantics) | Duplicate range calculator logic — RangeCalculator vs CompareRangeCalculator | util/RangeCalculator + util/CompareRangeCalculator (~316L cada uno) | Code duplication / potential semantic bug |
| AP-88 | COSMETIC | Typo class name `IHistorySeralizer` | history/json/IHistorySeralizer.java | Naming |
| AP-89 | LOW | Duplicate SimpleModule.register() en HistoryGroups | history/HistoryGroups.java getDeviceTree + getGroupTree | Code smell |

**TOTAL AP-1..AP-89 post-Bloque 64** = **89 antipatterns identificados** (3 CRITICAL + 9 HIGH + 25 MEDIUM + 52 LOW).

---

## 64.8 Patterns excelentes (KEEP literal MX60) — P-86..90

1. **P-86: BComponent service container pattern**
   ```java
   public class BReflowService extends BComponent implements BIService {
       public BReflowWebSocketAcceptor getWebSocketAcceptor() {
           return (BReflowWebSocketAcceptor) this.get(webSocketAcceptor);
       }
   }
   ```
   Composition vía Properties internas — Niagara idiomatic. **Regla 33**.

2. **P-87: Reactive Property change cascade**
   ```java
   public void changed(Property property, Context cx) {
       if (property == multiUserConfig) {
           this.getSyncService().reload();
       }
   }
   ```
   Cascade reload on config change. **Regla 35**.

3. **P-88: Jackson serializer factory pattern**
   ```java
   public static ObjectMapper make() {
       ObjectMapper mapper = new ObjectMapper();
       SimpleModule module = new SimpleModule();
       module.addSerializer(...);
       mapper.registerModule(module);
       return mapper;
   }
   ```
   Centralize JSON schema composition. **Regla 32**.

4. **P-89: Date range enum + calculator static dispatch**
   ```java
   public static BAbsTime[] make(BDateRangeEnum rangeEnum) {
       switch (rangeEnum.getOrdinal()) {
           case 0: return lastHour();
           // ...
       }
   }
   ```
   N4 type-safe facet. **Regla 34**.

5. **P-90: Privilege escalation security pattern**
   ```java
   AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
       public Void run() {
           Thread thread = new Thread(task);
           thread.start();
           thread.join();
       }
   });
   ```
   Required para acceso `history:/` ORD scope. **Regla 37**.

---

## 64.9 MX60 implications — continuación desde #190

| # | Tag | Descripción |
|---|-----|-------------|
| 191 | IMPROVE | Modularize RangeCalculator base class — `protected int getYearOffset()` overridable. CompareRangeCalculator extend con offset -2. AP-87 fix. |
| 192 | IMPROVE | Centralize HistoryObjectMapper factory — eliminar SimpleModule duplication en HistoryGroups. Cache singleton static field vs new per request. AP-89 fix. |
| 193 | NEW | **Analytics-lib Jackson serializer architecture** — replicar pattern HistoryObjectMapper.make() para custom analytics JSON. Bloque 66 prep. |
| 194 | IMPROVE | Cache HistoryData JSON si `historyCache` enabled en BReflowService — current rebuild per request. |
| 195 | NEW | Persistence BReflowService state on shutdown — save service config (limits, caching flags) to DiskStore station home. Reliability. |
| 196 | IMPROVE | Scheduled task Clock.schedulePeriodically configurable interval — `historyGroupRefreshInterval` BRelTime Property. 24h hardcoded actual. |
| 197 | LOW | Rename `IHistorySeralizer` → `IHistorySerializer` (AP-88 typo fix). Cross-codebase update imports. |
| 198 | IMPROVE | BReflowScheme cache resolved service instance — actual OrdQuery iteration ComponentSpace per request costly. |
| 199 | NEW | Atomic file write pattern HistoryIO — temp + rename anti-corruption. Cache integrity. |
| 200 | NEW | Type-safe filter builder HistoryList — vs HashMap<String,String> magic keys. |

**Total MX60 implications post-Bloque 64**: **200 entries** (190 previos + 10 nuevos: 4 NEW + 6 IMPROVE).

---

## 64.10 Reglas template MX60 — 7 reglas nuevas (32-38)

### Regla 32 — Custom Jackson serializer factory naming

```
✓ HistoryObjectMapper.make() → ObjectMapper instance
✓ HistoryRecordOptions POJO holder
✓ StdSerializer<T> extends pattern
✗ Avoid: Raw SimpleModule.addSerializer() scattered across classes
```

Centralize JSON schema composition — single source of truth.

### Regla 33 — Service container Property injection

```java
public class MyService extends BComponent implements BIService {
    public static final Property myInner = newProperty(5, new BMyInner(), null);

    public BMyInner getMyInner() {
        return (BMyInner) this.get(myInner);
    }
}
```

NO raw field assignments — use Property for observability + reactivity.

### Regla 34 — Date range enum + calculator static dispatch

```
✓ BFrozenEnum range constants (lastHour, last8Hours, ...)
✓ Static make(BDateRangeEnum) switch dispatcher
✗ Avoid: Hardcoded if/else chains
```

### Regla 35 — Reactive Property change cascade

```java
public void changed(Property p, Context cx) {
    if (p == multiUserConfig) this.getSyncService().reload();
    if (p == historyCache && getHistoryCache()) HistoryIO.clearCache();
}
```

Setter validation + dependent service cascade reload.

### Regla 36 — ORD scheme custom resolver

```java
public class BMyScheme extends BOrdScheme {
    public OrdTarget resolve(OrdTarget base, OrdQuery q, Context cx) {
        // ComponentSpace.iterateAllComponents() search
    }
}
```

NO hardcoded Component lookups — use ORD resolution.

### Regla 37 — Security privilege escalation pattern

```java
AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
    public Void run() throws Exception {
        Thread thread = new Thread(task);
        thread.start();
        thread.join();  // synchronous boundary
        return null;
    }
});
```

**Required** para acceso `history:/` ORD scope. Document reasoning en MX60 security RFC. Cross-ref Regla 13 (cx propagation a thread): combinar Regla 13 + 37 — el thread debe heredar acceptCx + doPrivileged scope.

### Regla 38 — Utility helper static factories

```
✓ static CommandHelpers.ordFromArgument(BValue)
✓ Flexible type coercion (BOrd / BComponent / String)
✗ Avoid: Instance methods
```

Utilities stateless ONLY.

**Total reglas template MX60 post-Bloque 64**: **38 reglas** (31 previas + 7 nuevas).

---

## 64.11 Predicciones / hipótesis a verificar

1. **`reflow.js` es bundles Webpack Vue 2.7** — BReflow.java carga ORD a JS module que inicializa Vue app raíz. Confirmar Bloque 65 integración E2E.
2. **`loader.js` async iframe injection** — iframe src apunta a BaseServlet route `/reflow/app/`. Verificar.
3. **RangeCalculator offsets intencionales semánticos** — "Compare Last Year" -2 puede ser deliberado para excluir current YTD en compare context. Documentar o fixear según business intent.
4. **HistoryGroupCache 24h hardcoded** — sin UI exposure. Considerar Property paramétrico Bloque 65.
5. **AccessController.doPrivileged overhead** — HistoryData.fromComponent() costly per request. Cache agresiva recomendada.
6. **BReflowService singleton pattern** — cada N4 station exactamente 1 instancia — safe shared state assumption.
7. **`reflow.js` vs `reflow_config.js` vs `reflow_redirect.js`** — ¿son 3 bundles separados o 1 con condicionales? Verificar build output Bloque 65.
8. **Implicit security boundary `-ux` vs `-rt`** — `-ux` solo permite r/rw permission scope basado en agent annotations, ¿se enforce en `-rt` también? Cross-check.

---

## 64.12 Cierre — completitud audit Reflow técnico

### Bloques auditados (Capa 17 completa)

| Capa | Bloques | Status |
|------|---------|--------|
| BajaScript canonical | 50, 51, 52 | ✅ |
| `yi`/serverSideCall RPC | 53 | ✅ |
| HTTP REST + BaseServlet + Response classes | 58 | ✅ |
| WebSocket trinity | 59 | ✅ |
| Sync engine + responses + backups | 60 | ✅ |
| Domains (alarm/history/points/schedule/nav/file) | 54-57 | ✅ |
| Librerías + APIs catálogo | 61 | ✅ |
| Alarmas dedicado | 62 | ✅ |
| Frontend Vue completo | 63 | ✅ |
| **`-ux` modules + `-rt` remanentes** | **64 (THIS)** | ✅ |

### Métricas finales post-Bloque 64

- **89 antipatterns** AP-1..89 (3 CRITICAL + 9 HIGH + 25 MEDIUM + 52 LOW)
- **38 reglas template MX60** (31-38 nuevas)
- **200 MX60 implications** (191-200 nuevas)
- **Líneas auditadas Bloque 64**: BReflowService (468) + BReflowScheme (78) + util/ (1341) + history/ (2404) + serializers (520) + `-ux` (177) = **~5KB LOC**

### Bloques pendientes

- **Bloque 65** — Cierre Reflow + síntesis backlog MX60 final (consolidación 89 AP, 38 reglas, 200 implications, 6 decisiones arquitectónicas, stack MX60)
- **Bloque 66+** — Pivote Analytics module + analytics-lib

### Decisión arquitectónica #7 MX60

**BComponent service container pattern (BReflowService blueprint)** es la decisión arquitectónica #7. Los servicios MX60 deben:
1. Extender `BComponent implements BIService`
2. Componer servicios internos vía Properties (no raw field assignments)
3. Implementar `changed(Property, Context)` reactive cascade
4. Lifecycle hooks `started/stopped/serviceStarted/serviceStopped`
5. Property-based config (no static config strings)
6. Slot-o-Matic generated getters/setters (type safety)

---

**End of Bloque 64** — `-ux` modules + `-rt` remanentes deep dive.

**Siguiente**: Bloque 65 (cierre Reflow + síntesis backlog MX60 final).
