# Bloque 68 — Transplante alarmas + history+charts Reflow → MX60: contrato backend copiable + lecciones S50-S56 + patrones frontend Vue 3 + Pinia + 16 implications #228..#243

**Fecha**: 2026-05-09
**Método**: Consolidación post-mapping (`docs/mappings/reflow-clean-177/` con status temporal corregido en esta sesión: legend `OPEN/FIXED-SXX/DESIGN/INFO/OPEN-MX60` + corrección stale `historyCache REWRITE` → `[FIXED-S56]`). Define **el blueprint de transplante** específico para alarmas + history+charts hacia MX60. **NO incluye código copy-paste** — contratos, patrones, lecciones. Source `Reflow-Clean-177/` permanece **READ-ONLY** (sdd-init/niagara-research engram); todo polish va en la copia MX60, no en el source.

**Fuentes primarias**:
- Mapping `docs/mappings/reflow-clean-177/` (post-correction 2026-05-09):
  - `domains/alarms.md` con 10 status tags
  - `domains/history.md` con 13 status tags (incluye corrección historyCache)
  - `domains/frontend.md` (capa cross-cutting)
  - `xref.json` 547 entries — backend ↔ frontend wiring
  - `index.json` 547 entries (entry historyCache verified 2026-05-09)
- `GAP-ANALYSIS.md` (Reflow-Clean-177) — fixes históricos S50-S57
- Bloques previos:
  - **#44** Alarm Console pipeline frontend (BAlarmSpaceConnection + canal BOX alarm + ack/clear)
  - **#45** History/Trend chart consumption (WebChart 3 GET + NDJSON + boxcs real-time)
  - **#62** Alarmas Reflow dedicado (1137L: 22 components + AP-72..78 + reglas 20-22) — audit; este bloque es transplante
  - **#63** Frontend Reflow Vue 2.7 audit (AP-79..86 + reglas 23-31 + **decisión Pinia 1:1 con Vuex**)
  - **#65** Cierre Reflow + síntesis backlog MX60 final (89 antipatterns + 38 reglas + 200 implications + 7 decisiones arquitectónicas)

**Versión analizada**: Reflow-Clean-177 (clean-room) + GAP-ANALYSIS.md fixes hasta S57.

---

## 68.0 Resumen ejecutivo — tabla de transplante

### One-glance: qué pasa de Reflow a MX60

| Capa / Pieza | HEREDA 1:1 | REESCRIBE en MX60 | EVITA (deuda) | NUEVO en MX60 |
|---|---|---|---|---|
| **Backend Java** | | | | |
| `BReflowAlarmCommands.java` (113L, 7 BOX methods) | ✅ Renombre package | — | — | — |
| `BReflowHistoryCommands.java` (112L, 7 BOX methods) | ✅ Renombre package | — | — | — |
| `AlarmData.java` (439L) | ✅ Casi 1:1 | — | — | — |
| `HistoryData.java` (663L) | — | ✅ Split obligatorio en 3 (Query/Cache/Serialize) | Mezcla concerns 663L | — |
| `history/json/` package (6 files, `HistoryObjectMapper` central) | ✅ 1:1 | — | — | — |
| `IHistorySeralizer.java` typo | — | — | Decisión: preservar typo en MX60 (compatible) o renombrar atómico | — |
| `RangeCalculator` + `CompareRangeCalculator` | ✅ 1:1 | — | — | — |
| `HistoryGhostSubscriber.java` (26L) | ✅ 1:1 (Niagara workaround obligatorio) | — | — | — |
| `BReflowChannelService` + `BReflowWebSocketAcceptor` | ✅ 1:1 | — | — | — |
| HTTP `BaseServlet` + 7 responses | ✅ 1:1 con renombre package | — | — | — |
| **Frontend Vue** | | | | |
| Forma de 22 components history (props/emits/jerarquía) | ✅ Forma | ✅ Implementación a Vue 3 + Composition API | — | — |
| Forma de 27 components alarms (props/emits/jerarquía) | ✅ Forma | ✅ Implementación a Vue 3 + Composition API | — | — |
| Forma de 11 components charts | ✅ Forma | ✅ Implementación a Vue 3 + Composition API | — | — |
| Vuex modules → Pinia 1:1 (decisión bloque #63) | — | ✅ alarms / alarmData / historyCache / dashboardCards / histories | Inventar shape — usar shape POST-fixes (S54 strip + S55 patches + S56 rewrite) | — |
| 18 Mixins Options API | — | ✅ Convertir a composables (`useHistoryList`, `useSubscriber`, `useCheckedItems`, etc.) | — | — |
| `D3chart.vue` global registration (3114L) | ✅ Patrón global | ✅ `app.component('D3chart', D3chart)` en `main.ts` MX60 (Vite + Vue 3) | Registro implícito tipo Vue 2 que rompe tree-shaking | — |
| `api/box.js` (331L) → `api/box.ts` con types | — | ✅ TypeScript + types por método BOX | — | ✅ Types de respuesta Java↔TS |
| `api/rest.js` (353L) → `api/rest.ts` con types | — | ✅ Idem | — | ✅ |
| `api/websocket.js` (155L) | — | ✅ Activar desde día 1 (no comentar como Reflow) | WS commented en main.js (Phase 5+) | — |
| **Patrones a NO repetir (lecciones S50-S56)** | | | | |
| `alarmData` 8 props inventadas | — | — | ✅ EVITA absoluto | — |
| `alarms` `ignoredAlarmClasses` raw | — | — | ✅ EVITA — usar filter+map+dedup desde día 1 | — |
| `alarms` `getStylesForConsole` sin defaults | — | — | ✅ EVITA — mergear 7 defaults desde día 1 | — |
| `alarms` `REMOVE_CONSOLE` cross-module commits desde action | — | — | ✅ EVITA — cross-module dentro de la mutation/action atómica | — |
| `historyCache` shape pre-rewrite | — | — | ✅ EVITA — usar shape post-S56 | — |
| `SourceGroupsTable` 656L monolítico | — | ✅ Split en componentes + composables | Monolito acoplado a `eventBus` | — |
| `HistoriesHome` 655L monolítico | — | ✅ Split + extracción a composables | Monolito | — |
| **Nuevo en MX60** | | | | |
| Tests unitarios (Reflow no tiene `.test|.spec` en alarms ni history) | — | — | — | ✅ Vitest + Vue Test Utils desde día 1 |
| TypeScript end-to-end | — | — | — | ✅ |
| WebSocket activo (no Phase 5+) | — | — | — | ✅ |
| Pinia DevTools | — | — | — | ✅ |

### Decisión arquitectónica MX60-alarms+history

- **Backend Java**: HEREDA 95% (~1.500 LOC Java copiables casi 1:1 entre 6 helpers + 4 commands + 7 HTTP responses + 6 serializers + utils). Único polish OBLIGATORIO en la copia MX60: **split `HistoryData.java`** en 3 clases (Query / Cache / Serialize). Source Reflow queda intacto.
- **Frontend Vue**: REESCRIBE 100% a Vue 3 + Composition API + Pinia + TypeScript + Vite, **PERO replicando la forma** (props/emits/jerarquía/nombres) de los ~60 componentes (22 history + 27 alarms + 11 charts).
- **Stores**: Pinia 1:1 con Vuex post-fixes (decisión bloque #63). NO replicar shape pre-S54/S55/S56.
- **WebSocket**: ACTIVO desde sprint 1, no comentado.
- **Tests**: obligatorios desde día 1 (Reflow tiene 0 tests en estos dominios).

**Timeline estimado MX60-alarms+history+charts**: **~6-8 semanas con 1-2 devs**, asumiendo MX60 service skeleton ya existe (de bloques #66/#67 Analytics).

---

## 68.1 Backend Java — contrato copiable casi 1:1

### 68.1.1 BOX RPC — 14 métodos heredables

**`BReflowAlarmCommands.java` — 7 métodos (HEREDA 1:1, renombre package)**:

| Método | Args | Retorno | Uso frontend |
|---|---|---|---|
| `getClasses` | — | `BAlarmClass[]` | Filtro por clase de alarma |
| `query` | `AlarmUuidArgs` | JSON paginated | Listado consola tabla/cards |
| `querySources` | `AlarmUuidArgs` | JSON | Aggregation multi-source |
| `getActiveAlarmCounts` | — | `Map<priority, count>` | BuildingAlarmSummary badges |
| `getUnackedAlarmCounts` | — | idem | Total badge |
| `getAlarmsSinceTime` | `BAbsTime` | JSON | Catch-up post-WS-reconnect |
| `canAcknowledgeAlarms` | `AlarmUuidArgs` | `boolean` + nota requerida flag | RequiredNoteModal trigger |

**`BReflowHistoryCommands.java` — 7 métodos (HEREDA 1:1)**:

| Método | Args | Retorno | Uso frontend |
|---|---|---|---|
| `getList` | filter | history catalog JSON | HistoriesHome/Menu |
| `getQuickList` | filter | catalog mínimo | HistoryPicker |
| `getData` | `RangeCalculator` args | time-series JSON | HistoryChart/Spark |
| `getGroupNames` | — | `string[]` | HistoryGroupPicker |
| `getGroupTree` | — | tree JSON | HistoriesMenu |
| `getDeviceTree` | — | tree JSON | HistoryDevicePicker |
| `getDevices` | — | `string[]` | HistoryBuilder |

**Implementación BOX en MX60**: replicar el patrón de `@Slot` + reflexión BajaScript que Reflow usa. xref.json muestra `usage_count: 0` en estos commands porque los importadores son dinámicos vía reflection — falso negativo esperable. **No "limpiar" estos commands creyendo que son dead code**.

**#228**: **Heredar contrato BOX literal** — los 14 métodos son la API estable que la SPA llamará. Cambiar nombres rompe el frontend transplantado. Mantener firma exacta (args + retornos) en MX60.

> ⚠️ **Cross-check con bloque #54**: bloque #54 documentó **9 métodos** exportados (vs 7 confirmados en mapping). Validar empíricamente — posible diferencia entre `BReflowAlarmCommands` clase (mapping cuenta 7 públicos) vs callable surface BajaScript (bloque #54 cuenta 9 con reflection sites). Si son 9, agregar `getAlarmByUuid` + `getUuidsForSources` a la tabla.

### 68.1.2 HTTP REST + WebSocket — 3 caminos coexistentes

**REST responses (HEREDA con renombre package)**:
- `http/BaseServlet.java` — padre, importa los 7 responses
- `http/responses/AlarmCSVResponse.java` — `/nmodsreflow/alarms` modo CSV
- `http/responses/AlarmQueryResponse.java` — modo JSON
- `http/responses/HistoryListResponse.java`
- `http/responses/HistoryGroupsResponse.java`
- `http/responses/HistoryChartDataResponse.java`
- `http/responses/HistoryDataResponse.java`

**WebSocket (HEREDA 1:1)**:
- `http/sockets/BReflowWebSocketAcceptor.java`
- `http/sockets/BReflowChannelService.java`
- `BReflowService.java` (15 importers — clase central, también HEREDA)

**#229**: **3 caminos en MX60** — BOX para queries on-demand, REST para CSV export + chart data en bloque, WS para push de alarmas live. NO simplificar a un solo camino: cada uno tiene su razón (BOX = RPC tipado, REST = streams + cacheable, WS = push).

### 68.1.3 Domain logic Java — copiable con polish puntual

```
alarms/
├── AlarmData.java         (439L)  → HEREDA 1:1
├── QueryFilter.java       (158L)  → HEREDA 1:1
├── AlarmSourceCollection  (83L)   → HEREDA 1:1
├── ReflowAlarmSource.java (25L)   → HEREDA 1:1 (wrapper ORD a BIAlarmSource)
└── AlarmUuidArgs.java     (74L)   → HEREDA 1:1

history/
├── HistoryData.java         (663L) → ⚠️ SPLIT en MX60 (ver §68.1.6)
├── HistoryList.java         (355L) → HEREDA 1:1
├── HistoryGroups.java       (112L) → HEREDA 1:1
├── HistoryGhostSubscriber   (26L)  → HEREDA 1:1 (workaround Niagara — ver §68.1.5)
└── HistoryIO.java           (103L) → HEREDA 1:1

util/
├── RangeCalculator.java          → HEREDA 1:1
└── CompareRangeCalculator.java   → HEREDA 1:1 (modo compare chart)
```

### 68.1.4 JSON serializers — paquete `history/json/` aislado

```
history/json/
├── HistoryObjectMapper.java     (20L)  → HEREDA 1:1 (registry central)
├── HistoryRecordSerializer.java (122L) → HEREDA 1:1
├── HistoryDeviceSerializer.java (65L)  → HEREDA 1:1
├── HistoryFolderSerializer.java (55L)  → HEREDA 1:1
├── HistoryRecordOptions.java    (29L)  → HEREDA 1:1
└── IHistorySeralizer.java       (80L)  → HEREDA 1:1 (typo histórico — ver §68.1.6)
```

**#230**: **`HistoryObjectMapper` es el registry único** — toda la serialización Jackson pasa por ahí. En MX60 idem: un solo punto de configuración. No fragmentar serializers en múltiples object mappers.

### 68.1.5 `HistoryGhostSubscriber` — pattern obligatorio Niagara

Crea suscripción "ghost" (vacía) sobre `BHistoryDatabase` para mantener viva la sesión de history en el station durante queries pesados. Sin esto, el station cierra la sesión por timeout antes de que la respuesta esté lista.

xref.json marca `usage_count: 0` — falso negativo. Es instanciado dinámicamente o vía slot. **Replicar tal cual en MX60**, no eliminar pensando que es dead code.

### 68.1.6 Polish pre-transplante — SOLO en la copia MX60

**`Reflow-Clean-177/` source es READ-ONLY** (sdd-init/niagara-research engram constraint). Los siguientes polish van **únicamente** en la copia MX60, NO en el source:

**#231**: **Split `HistoryData.java` 663L** en MX60 desde día 1:
- `HistoryQueryEngine.java` — Builder pattern + queries `BHistoryDatabase` (~300L)
- `HistoryDataCache.java` — caché separado de query (~150L)
- `HistoryJsonSerializer.java` — delegación a `HistoryObjectMapper` (capa fina, ~200L)

Cada clase con responsabilidad única + tests unitarios por clase.

**Decisión typo `IHistorySeralizer`** (preservar vs renombrar):
- **Opción A — preservar**: typo se mantiene en MX60 (`IHistorySeralizer.java`). Compatible con futuro merge de mejoras desde Reflow. Riesgo: typo se perpetúa.
- **Opción B — renombrar atómico**: corregir a `IHistorySerializer.java` + actualizar TODOS los call sites (interface impls + ObjectMapper register) en el mismo commit.

**Recomendación**: **Opción A** durante el período de migración (alinea con Reflow); renombrar en sprint dedicado post-go-live MX60.

---

## 68.2 Frontend Vue 3 + Pinia — patrones a replicar (NO código)

### 68.2.1 Forma de componentes — props/emits/jerarquía

**Regla rectora**: copiar **la forma**, no la implementación. La forma se mantiene en Vue 3 Composition API; la implementación se reescribe en `<script setup lang="ts">` con composables.

**Jerarquía history (22 componentes)**:
```
HistoriesHome (router root, 655L → split obligatorio §68.6.3)
├── HistoriesMenu (lateral nav, 295L)
├── HistoriesFull / HistoriesCompact (vista)
├── HistoryStationCache (renderless pre-warm, render: () => null)
└── (children por contexto)
    ├── HistoryBuilder (wizard 384L)
    │   ├── HistoryDevicePicker
    │   ├── HistoryTypePicker
    │   └── TimeRangePicker (← cross-domain con alarms)
    ├── HistoryChart (full chart con toolbar/delta/export, 408L)
    │   └── ChartToolBar
    ├── HistorySpark (sparkline dashboard, 395L)
    ├── HistoryTable (vista tabular, 307L)
    ├── HistoryCard / HistoryRow (list items)
    └── HistoryForm (metadata edit, 311L)
```

**Jerarquía alarms (5 grupos funcionales)**:
1. **Consola y tabla (6)**: `AlarmDisplay` orchestrator + `AlarmsTable` / `AlarmCards` / `AlarmList` / `SourceGroupsTable` (656L → split §68.6.2) / `Total`
2. **Acknowledge / notas (4)**: `AlarmAckConfirm`, `RequiredNoteModal`, `AlarmNotes`, `AlarmNotesModal`
3. **Prioridad / clase / fuente (6)**: `AlarmClassList`, `AlarmPrioritiesForm`, `AlarmPriorityPicker`, `AlarmPriorityType`, `AlarmStatusPicker`, `BuildingAlarmSummary`
4. **Sonido (2)**: `AlarmSoundsForm`, `AlarmSoundsPicker`
5. **Sub-formularios configuración (9)**: `AlarmConsoleForm`, `AlarmConsoleList`, `AlarmIconsForm`, `AlarmRowStyleForm`, `AlarmSummaryForm`, `AlarmsTableForm`, `ConsoleRefreshRateForm`, `PriorityColorsForm`, `SourcesTableForm`

**Props/emits a preservar**: nombres exactos de props (`cardId`, `value`, `multiple`, `compact`, `unackOnly`) y emits (`input`, `dive`, `ack`, `load-alarms`, `load-next-page`, `update:unackOnly`).

### 68.2.2 Vuex → Pinia 1:1 (decisión heredada bloque #63)

**#232**: **Pinia 1:1 con Vuex** — decisión arquitectónica de bloque #63. Cada `store/modules/<name>.js` Vuex se convierte en un `stores/<name>.ts` Pinia con:

- `state()` ← Vuex `state`
- `getters` ← Vuex `getters` (puro)
- `actions` ← Vuex `actions` + `mutations` consolidadas (Pinia no separa mutations)
- Persistencia ← `pinia-plugin-persistedstate` para los stores marcados `persistent: true`

**Stores del dominio (5)**:

| Store | Reflow Vuex | MX60 Pinia | Persistent |
|---|---|---|---|
| `alarms` | 238L Vuex post-S55 | `useAlarmsStore` | ✅ |
| `alarmData` | 155L Vuex post-S54 (2 state, 2 mut, 3 get) | `useAlarmDataStore` | ❌ |
| `historyCache` | 516L Vuex post-S56 (`xa` cache + `Sa` service + `Ia` builder + minimal Vuex) | `useHistoryCacheStore` | ❌ |
| `histories` | 139L (PERFECT 1:1 con bundle) | `useHistoriesStore` | ✅ |
| `dashboardCards` | 169L | `useDashboardCardsStore` | ✅ |

**Crítico**: replicar el shape POST-fixes, no el pre-fix (ver §68.3).

### 68.2.3 Mixins → Composables (mapping requerido)

Vue 3 no tiene mixins idiomáticamente. Los 18 mixins de Reflow se convierten a composables. Los relevantes para alarmas + history+charts:

| Mixin Reflow | LOC | Composable MX60 | Notas |
|---|---|---|---|
| `historyListMixin.js` | 361 | `useHistoryList()` | Más usado del dominio (HistoriesHome + HistoryCard + HistoryRow) |
| `subscriberMixin.js` | 131 | `useSubscriber()` | BajaScript subscribe lifecycle |
| `stateBaseMixin.js` | 79 | `useStateBase()` | Polling base |
| `checkedItemsMixin.js` | 48 | `useCheckedItems()` | Multi-select (AlarmSoundsForm) |
| `tweenMixin.js` | 61 | `useTween()` | Number animation (charts) |

**#233**: **Composables retornan refs + funciones**, no `data()`. La forma del API público debe coincidir con lo que el mixin exponía como Options API (mismo nombre de método y semántica) para minimizar cambios en componentes consumidores.

### 68.2.4 D3chart como global component — gotcha Vite + Vue 3

**#234**: **`HistoryChart.vue` y `HistorySpark.vue` NO importan `D3chart`** — lo consumen como componente registrado globalmente en `main.js:64` (verificado: `Vue.component('D3chart', D3chart)`).

En Reflow esto funciona por Vue 2 + Webpack (registro implícito). En MX60 con Vite + Vue 3 + tree-shaking esto rompe en silencio:

```ts
// MX60 main.ts — REGISTRO EXPLÍCITO obligatorio
import { createApp } from 'vue'
import D3chart from '@/components/charts/D3chart.vue'

const app = createApp(App)
app.component('D3chart', D3chart)  // ← sin esto, HistoryChart/HistorySpark no rendea
```

Alternativa: `unplugin-vue-components` con auto-import. Decisión: **registro explícito** para que el coupling sea visible (auto-import oculta el patrón).

### 68.2.5 API layer — TypeScript con types Java↔TS

`api/box.ts` — wrapper BOX RPC con tipos por método. Una interface por command class (AlarmCommands, HistoryCommands).

`api/rest.ts` — axios + interceptor 403→CSRF→retry (replicar `plugins/http.js` de Reflow + agregar types).

`api/websocket.ts` — **ACTIVO desde sprint 1**, no Phase 5+. Integración con `BReflowChannelService` para push de alarmas live.

---

## 68.3 Lecciones S50-S56 — antibugs explícitos para MX60

Extracto de `GAP-ANALYSIS.md` (Reflow-Clean-177): los siguientes bugs aparecieron en la reconstrucción del bundle Reflow producción → fueron arreglados en sprints S50-S57. **Replicarlos en MX60 sería re-introducir deuda ya pagada**.

### 68.3.1 `alarmData` [FIXED-S54] — 8 props + 9 mutations + 5 getters a NO inventar

**#235**: **NO inventar shape extendido en `alarmData`**. El bundle original es chico:

- **State (2)**: `loading: boolean`, `currentTimeRanges: TimeRange[]` (array NO objeto — el upsert depende de array semantics)
- **Mutations consolidadas en actions Pinia (2)**: `SET_LOADING`, `SET_CURRENT_TIME_RANGE` (con upsert array)
- **Getters (3)**: `priorityForRecord` (con default `"medium"` no `"low"`, usa `rootGetters["alarms/getConsoleById"]`), `inAlarmCount` (parámetro `{priority, id, classList}`, filtra por active building + ignored classes + priority), `currentTimeRange` (fallback a `consoleById.defaultTimeRange`, no `null`)
- **Actions (0)**

**EVITAR** (la reconstrucción Reflow inventó esto y se removió en S54):
- `records`, `totalRecords`, `currentPage`, `pageSize` — esos viven en componentes locales
- `queryFilters`, `error`, `lastQuery`, `timeRanges` — fuera del store
- 9 mutations adicionales — solo 2

### 68.3.2 `alarms` store [FIXED-S55] — 5 reglas concretas

**#236**: **5 reglas obligatorias para el store `alarms` en MX60** (no replicar la versión pre-S55):

1. **`ignoredAlarmClasses`** filtra `enabled === false`, mapea `.class`, deduplica via `Set`. NO devolver lista raw.
2. **`getStylesForConsole`** acepta `string | object`, mergea 7 defaults (alert/fault/offnormal/normal/ack/unack/ackState con colors/pulse/border/background/text/action). NO retornar parcial.
3. **`REMOVE_CONSOLE`** ejecuta cross-module commits dentro de la mutation atómica con `this.commit()` (en Pinia: dentro de la action como single-step). NO desde otra action separada.
4. **Acción `migrateAlarmConsoleRestrictionType`** existe — toggles allow/deny list para alarm classes. NO omitir.
5. **`getNewConsole`** usa **UUID v4** + naming sequencial `"Alarm Console N"`. NO `Date.now() + Math.random()` + hardcoded "New Alarm Console".

### 68.3.3 `AlarmsTable` + `AlarmCards` [FIXED-S50] — V-P0-8/V-P0-9

**#237**: **`AlarmsTable.vue`**: 5 columnas exactas con multi-select + ack + notas + detalle. La reconstrucción tenía una columna mal en V-P0-8 — verificar exactitud contra bundle al portar a MX60.

**`AlarmCards.vue`**: watchers + events deben coincidir con los del bundle original (V-P0-9). Los emits son `ack`, `load-next-page`, `load-previous-page`. Watchers re-querean cuando cambia filtros o building activo.

### 68.3.4 `historyCache` [FIXED-S56] — shape post-rewrite

**#238**: **`historyCache` en MX60 sigue el shape POST-S56**, NO el pre-rewrite. La reescritura S56 entregó:

1. **Module-level cache** (no Pinia state — mantener afuera del store reactivo): `Map<HistoryKey, CachedSeries>` (`xa` en Reflow). Esto es lógica pura, no reactiva.
2. **Service object** (lógica pura, no Pinia action): `historyService` con métodos `list`, `loadList`, `loadGroups`, `loadDevices`, `generate`, `data`, `buildHistoryQueryString`, `d3Options`. ~324 LOC.
3. **Index builder recursivo** (`Ia` en Reflow): `groupsIndexBuilder` función pura que arma índice de grupos.
4. **Pinia store minimal**: solo `invalid: boolean` + action `SET_INVALID` + action `refresh()` que limpia el cache module-level y reset invalid.

**EVITAR**: inventar getters/actions/state que dupliquen `historyService`. El servicio es lógica pura, fuera del store. Mezclar cache reactivo con cache module-level fue lo que hizo el pre-S56 pesado y bugy.

---

## 68.4 Charts ≠ History — taxonomía de dependencias

### 68.4.1 D3chart como engine global de toda la app

**#239**: **`D3chart.vue` 3114L es engine, NO componente de uso directo**. Solo `main.js` lo importa; los demás lo consumen como `<D3chart>` global. Patrón a preservar en MX60 (con `app.component()` explícito — §68.2.4).

### 68.4.2 Charts coupled a history (9 componentes)

| Componente | LOC | Consumers (xref verificado) |
|---|---|---|
| `D3chart.vue` | 3114 | global (`main.js`) |
| `Chart.vue` | 469 | LandingHistories, HistoriesFull, HistoryBuilder, HistoryChart |
| `ChartToolBar.vue` | 356 | D3chart, HistoryChart |
| `Sparkline.vue` | 431 | FeaturedCard, HistoriesCompact, HistoryCard, HistoryForm, HistoryRow, HistorySpark |
| `ChartExportPicker.vue` | 102 | (vía Chart/toolbar) |
| `ChartTypePicker.vue` | 134 | (vía Chart/toolbar) |
| `ContextMenu.vue` | 127 | (vía D3chart) |
| `TimeRangePicker.vue` | 190 | HistoryBuilder, **AlarmDetails**, **AlarmsHome** ← cross-domain |
| `DeltaSymbol.vue` | 41 | (vía Chart) |

### 68.4.3 Charts NO acoplados a history (2 componentes)

**#240**: **`GraphicReflow.vue` (534L)** y **`GraphicSelect.vue` (306L)** NO son del dominio history. Pertenecen a:
- `GraphicReflow` → `views/DeviceDetailsView.vue`
- `GraphicSelect` → `EquipmentAdd`, `EquipmentEditorGraphic`

En MX60 estos quedan en su dominio respectivo (equipment / device-views), NO en `components/charts/` con los demás. Considerar mover a `components/equipment-graphics/` o `components/devices/` para evitar el coupling falso de "todos los gráficos viven juntos".

### 68.4.4 `TimeRangePicker` cross-domain alarms ↔ history

`TimeRangePicker.vue` en `components/charts/` lo usan 3 sites:
- `HistoryBuilder.vue` (history)
- `AlarmsHome.vue` (alarms)
- `AlarmDetails.vue` (alarms)

En MX60: el composable `useTimeRange()` debe vivir en `composables/` (compartido), y `<TimeRangePicker>` debe quedar genérico (no acoplado a un dominio específico). Está bien que viva en `components/charts/` o `components/shared/` — pero no debe importar nada de `alarms/` ni de `histories/`.

---

## 68.5 Order de implementación + estimación

### 68.5.1 Dependency graph

```
Sprint 1-2 — Backend Java (foundation)
  └─ BReflowService skeleton + module.xml + servlet chain
     ├─ BReflowAlarmCommands + AlarmData + 4 helpers
     ├─ BReflowHistoryCommands + HistoryData split (3 clases)
     ├─ history/json/ package (6 serializers)
     ├─ HTTP responses (BaseServlet + 7 responses)
     └─ WebSocket layer (acceptor + ChannelService)

Sprint 3-4 — Frontend foundation (Vue 3 + Pinia + composables)
  └─ App shell + router + plugins ($baja, $niagara, $http) en TS
     ├─ Pinia stores: alarms, alarmData, historyCache, histories, dashboardCards
     ├─ Composables: useHistoryList, useSubscriber, useCheckedItems, useStateBase
     ├─ api/box.ts + api/rest.ts + api/websocket.ts (WS activo)
     └─ D3chart global registration en main.ts

Sprint 5-6 — Components (history + charts)
  ├─ Charts (11): D3chart, Chart, ChartToolBar, Sparkline, TimeRangePicker, ...
  └─ History (22): HistoriesHome (split obligatorio), HistoryChart, HistorySpark, ...

Sprint 7-8 — Components (alarms 27 + 2 views)
  ├─ Consola y tabla (6) — incluye SourceGroupsTable split
  ├─ Acknowledge / notas (4)
  ├─ Prioridad / clase / fuente (6)
  ├─ Sonido (2)
  └─ Sub-forms configuración (9)
```

**#242**: **Backend antes que frontend**. Backend Java es ~1.500 LOC heredables; sin él, el frontend no tiene contra qué llamar. Hacer backend completo + WS activo antes de tocar Vue.

### 68.5.2 Estimación realista

- **Backend Java**: ~2 sprints (10 días) — copy + renombre package + split HistoryData + tests Java
- **Frontend foundation**: ~2 sprints — stores Pinia + composables + API layer + plugins + D3chart global
- **Components history+charts**: ~2 sprints — 33 componentes (22 history + 11 charts)
- **Components alarms**: ~2 sprints — 29 componentes (27 alarms + 2 views) incluyendo SourceGroupsTable split

**Total: ~6-8 semanas con 1-2 devs**, asumiendo MX60 service skeleton ya existe.

### 68.5.3 Pre-flight checklist (antes de sprint 1)

- [ ] MX60 backend module skeleton existente (de bloques #66/#67 Analytics)
- [ ] `nmodsmx60-rt/` + `nmodsmx60-ux/` directories + gradle setup
- [ ] Vitest + Vue Test Utils config + 1 test smoke pasando
- [ ] TypeScript strict mode habilitado
- [ ] ESLint + Prettier alineados con bloque #65 stack final
- [ ] Pinia DevTools instalado
- [ ] D3.js + dayjs versiones decididas (alineadas con Reflow para minimizar deltas en port)

---

## 68.6 Riesgos heredados [OPEN] + decisiones MX60

### 68.6.1 WebSocket commented [OPEN] — reactivar antes del transplante

**#243**: **`api/websocket.js` (155L) está comentado en `main.js` Reflow** (flag Phase 5+). Si transplantás "tal cual" sin activar, MX60 hereda el problema.

**Decisión MX60**: WS activo desde sprint 1 (backend `BReflowWebSocketAcceptor` + `BReflowChannelService` ya existen y son copiables 1:1). Frontend `api/websocket.ts` se escribe completo, no commented.

### 68.6.2 SourceGroupsTable 656 LOC — split obligatorio

`SourceGroupsTable.vue` (656L verificado por `wc -l`) es el componente más pesado del dominio alarms. Acoplado a `alarmData` store + `eventBus`. Sin tests.

**Decisión MX60**: split en sub-componentes (`SourceGroupRow`, `SourceGroupHeader`, `BulkAckBar`) + composable `useBulkAck()`. Reemplazar `eventBus` por Pinia store o evento custom `<emit>`.

### 68.6.3 HistoriesHome 655 LOC — split obligatorio

Componente Vue más grande del dominio history. Funciona en Reflow, **debe dividirse al portar a MX60**.

**Decisión MX60**: extraer a:
- `HistoriesHome.vue` (orchestrator ~150L)
- `useHistoryListView()` composable (~120L)
- `<HistoriesHomeFilters>` sub-component (~100L)
- `<HistoriesHomeTable>` sub-component (~150L)
- `<HistoriesHomeMenu>` o reuso de `HistoriesMenu` (~100L)

### 68.6.4 IHistorySeralizer typo

Decisión: preservar typo en MX60 durante migration (Opción A §68.1.6); renombrar atómico en sprint dedicado post-go-live.

### 68.6.5 Discrepancia mapping vs bloque #62 sobre live updates — FLAG empírica

**#241**: **Inconsistencia detectada** durante este bloque entre síntesis del mapping y audit empírico previo — implication: validar empíricamente antes de transplantar el patrón de live-updates a MX60:

- `domains/alarms.md` §5 (mapping) describe el patrón como `$niagara.alarmSubscribe → ChannelService → push WebSocket reactivo`.
- **Bloque #62** §62.9.3 (audit empírico) dice **"alarmas NO usan WebSocket"** — usan **POLLING** (`alarmCache.js` cada 20s default).

El audit empírico (bloque #62) tiene mayor peso que la síntesis del mapping. **Antes de transplantar el patrón de live-updates a MX60, validar empíricamente**:

```bash
# Verificación pre-transplant — confirmar mecanismo
rg "alarmSubscribe|setInterval.*alarm|CHANNEL_ALARM" reflow-frontend/src/plugins/niagara.js
rg "polling|setInterval" reflow-frontend/src/lib/alarmCache.js
```

**Decisión MX60**: independiente del veredicto, MX60 usa **WebSocket activo** desde sprint 1. Si Reflow usa polling, MX60 mejora. Si Reflow usa WS comentado, MX60 lo activa. Sin esta validación, el riesgo es heredar polling sin saberlo (peor latencia + carga station innecesaria).

**Acción**: corregir `domains/alarms.md` §5 si el audit empírico confirma polling — el mapping quedaría stale en este punto igual que `historyCache REWRITE` antes de esta sesión.

---

## 68.7 Cross-references a bloques previos

- **Bloque #44** — Alarm Console pipeline frontend (BAlarmSpaceConnection + canal BOX alarm + ack/clear) → contexto histórico arquitectural Niagara nativo
- **Bloque #45** — History/Trend chart consumption (WebChart 3 GET + NDJSON + boxcs real-time) → contrato chart-data Niagara nativo, antes de Reflow
- **Bloque #54** — BReflowAlarmCommands audit arquitectónico (9 métodos exportados; el mapping cuenta 7 — verificar diferencia §68.1.1)
- **Bloque #62** — Alarmas Reflow dedicado (1137L: 22 components + AP-72..78 + reglas 20-22) → fuente primaria audit alarmas; este bloque #68 es transplante-blueprint
- **Bloque #63** — Frontend Reflow Vue 2.7 audit (AP-79..86 + reglas 23-31 + **decisión Pinia 1:1 con Vuex**) → decisión arquitectónica heredada
- **Bloque #65** — Cierre Reflow + síntesis backlog MX60 final (89 antipatterns + 38 reglas + 200 implications + 7 decisiones arquitectónicas) → catálogo total a no contradecir
- **Bloque #66/#67** — Analytics module Niagara N4 oficial (MX60-Analytics inspired) → backend service skeleton MX60 (precondición sprint 1)

---

## 68.8 Resumen final — entregables del bloque

✅ Tabla one-glance HEREDA / REESCRIBE / EVITA / NUEVO (§68.0)
✅ Backend Java contract — ~1.500 LOC copiables casi 1:1 (§68.1)
✅ Frontend Vue 3 + Pinia patterns (NO código) (§68.2)
✅ Lecciones S50-S56 antibugs explícitos (§68.3)
✅ Charts ≠ History taxonomía (§68.4)
✅ Order de implementación + 6-8 sprint estimate (§68.5)
✅ Riesgos heredados + decisiones MX60 (§68.6)
✅ 16 implications #228..#243

**Status**: bloque listo. **Siguiente paso operacional**: cuando arranque MX60-alarms+history+charts, abrir este bloque + el mapping `docs/mappings/reflow-clean-177/` lado a lado. Backend en sprint 1-2 (heredando con renombre package + split HistoryData), frontend en sprints 3-8 (replicando forma + reescribiendo implementación a Vue 3 + Pinia + TypeScript).

**Validación pendiente** (§68.6.5): confirmar empíricamente si Reflow usa WS o polling para alarmas live antes de transplantar el patrón.

---

**Sesión cerrada con bloque 68**: el mental model Niagara N4 + Reflow + MX60 ahora tiene **blueprint operacional** específico para alarmas + history+charts. Capa 17 audit Reflow extendida a Capa 18 (Analytics + transplante). 16 nuevos implications #228..#243. Mapping `docs/mappings/reflow-clean-177/` queda como source-of-truth fáctico (status temporal corregido en esta misma sesión: legend `OPEN/FIXED-SXX/DESIGN/INFO/OPEN-MX60` aplicada en 23 gotchas + corrección `historyCache REWRITE → FIXED-S56`).
