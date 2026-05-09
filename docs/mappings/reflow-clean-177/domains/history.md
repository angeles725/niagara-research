# History Domain — reflow-frontend/src/components/histories/ + nmodsreflow-rt/.../history/

**Frontend files**: 22 Vue components in `reflow-frontend/src/components/histories/`
**Backend files**: 12 Java classes — 6 in `nmodsreflow-rt/.../history/` + 6 in `history/json/` + 1 BOX command class in `commands/`
**Fidelity (per GAP-ANALYSIS.md, verified 2026-05-09)**: EXCELLENT overall (all 22 frontend entries rated EXCELLENT). Store `historyCache` previously flagged REWRITE → **[FIXED-S56]** full rewrite shipped (`historyData` cache + `historyService` 324 LOC + `groupsIndexBuilder` + minimal Vuex). Current rating: GOOD.

**Status legend**: `[OPEN]` unresolved in Reflow source · `[FIXED-SXX]` resolved in sprint XX · `[DESIGN]` intentional Niagara/architectural decision · `[INFO]` descriptive note, no temporal status · `[OPEN-MX60]` concern applies only when migrating to MX60.

---

## 1. Overview

El dominio history cubre todo el ciclo de vida de datos históricos de estación en Reflow: desde la navegación y selección de puntos (HistoriesHome + HistoriesMenu con tabs de grupos y dispositivos), pasando por el builder wizard (HistoryBuilder) que orquesta la selección de dispositivo, tipo y rango de tiempo, hasta la visualización en gráficos de series temporales (HistoryChart envolviendo D3chart global) y sparklines de dashboard (HistorySpark). En backend, HistoryData (663 LOC, clase más pesada del dominio) implementa el patrón Builder para armar consultas contra `BHistoryDatabase` y retorna JSON via Jackson; HistoryList enumera y cachea el catálogo de historias disponibles; HistoryGroups organiza la jerarquía por carpeta/dispositivo; HistoryGhostSubscriber mantiene viva la sesión de historia durante queries largos previniendo timeouts del station. El sub-paquete `history/json/` aísla toda la capa de serialización con un `HistoryObjectMapper` central que registra serializers específicos por tipo de nodo (record, device, folder).

---

## 2. Entry Points

| Artifact | Kind | LOC | Role |
|---|---|---|---|
| `HistoriesHome.vue` | Vue route component | 655 | Ruta principal `histories`; combina lista, chart y panel de filtros; usa `historyListMixin` y `eventBus` |
| `HistoryChart.vue` | Vue component | 408 | Chart full-featured con toolbar, modo delta y export; prop `cardId`; usa store `dashboardCards` |
| `HistorySpark.vue` | Vue component | 395 | Sparkline card para dashboard; prop `cardId`; usa `historyCache` + `dashboardCards` |
| `HistoryBuilder.vue` | Vue component | 384 | Wizard para construir query: selección de device, tipo y rango |
| `HistoryData.java` | Java class | 663 | Recuperación de records con Builder pattern + Jackson serialization; clase más pesada del dominio |
| `HistoryList.java` | Java class | 355 | Enumeración y caché de historias disponibles en la estación |
| `BReflowHistoryCommands.java` | Java BOX class | 112 | Expone RPCs a BajaScript: `getList`, `getQuickList`, `getData`, `getGroupNames`, `getGroupTree`, `getDeviceTree`, `getDevices` |

---

## 3. Components / Classes

### 3.1 Frontend — Home, Menu & Container (~5)

| File | LOC | Fidelity | Purpose |
|---|---|---|---|
| `HistoriesHome.vue` | 655 | EXCELLENT | Ruta `histories`; orquesta lista + chart + filtros; `historyListMixin`, `eventBus` |
| `HistoriesMenu.vue` | 295 | EXCELLENT | Navegación lateral: grupos, dispositivos, favoritos |
| `HistoriesFull.vue` | 129 | EXCELLENT | Full-page history browser con todos los controles de filtro/orden |
| `HistoriesCompact.vue` | 139 | EXCELLENT | Vista compacta para embedding en dashboard cards; prop `compact` |
| `HistoryStationCache.vue` | 97 | EXCELLENT | Renderless — pre-warms el station history cache en mount |

### 3.2 Frontend — Builder & Form (~4)

| File | LOC | Fidelity | Purpose |
|---|---|---|---|
| `HistoryBuilder.vue` | 384 | EXCELLENT | Wizard multi-paso: device → tipo → rango |
| `HistoryForm.vue` | 311 | EXCELLENT | Form para editar metadata: nombre, grupo, building, opciones de display; v-model `value` |
| `HistoryBuildingPicker.vue` | 134 | EXCELLENT | Dropdown building filter; deps `historyCache` + `buildings`; emits `input` |
| `HistoryGroupPicker.vue` | 166 | EXCELLENT | Picker para seleccionar o crear un history group; emits `input` |

### 3.3 Frontend — Chart & Sparkline (~3)

| File | LOC | Fidelity | Purpose |
|---|---|---|---|
| `HistoryChart.vue` | 408 | EXCELLENT | Time-series chart full; toolbar, delta mode, export; prop `cardId`; D3chart global |
| `HistorySpark.vue` | 395 | EXCELLENT | Sparkline card de dashboard; reutiliza motor D3chart en modo spark; prop `cardId` |
| `HistoryTable.vue` | 307 | EXCELLENT | Vista tabular de datos con columnas ordenables y export CSV; v-model `value` |

### 3.4 Frontend — List Items & Pickers (~7)

| File | LOC | Fidelity | Purpose |
|---|---|---|---|
| `HistoryCard.vue` | 232 | EXCELLENT | Card tile: selección, star, info summary; props `history`, `noHover`, `checkmark`, etc.; `historyListMixin` |
| `HistoryRow.vue` | 172 | EXCELLENT | Fila de tabla con acciones inline; props `history`, `value`; `historyListMixin` |
| `HistoryPicker.vue` | 140 | EXCELLENT | Picker genérico con search y multi-select; props `value`, `multiple`; emits `input` |
| `HistoryDevicePicker.vue` | 123 | EXCELLENT | Picker de device Niagara; emits `input`, `on-change` |
| `HistoryTypePicker.vue` | 139 | EXCELLENT | Filtro por tipo (numeric, boolean, enum, string); emits `input`, `on-change` |
| `HistoryOrderPicker.vue` | 120 | EXCELLENT | Sort order picker (nombre, fecha, tipo); emits `input` |
| `HistoryRefresh.vue` | 75 | EXCELLENT | Control de refresh manual; v-model `value` |

### 3.5 Frontend — Misc (~3)

| File | LOC | Fidelity | Purpose |
|---|---|---|---|
| `FeaturedHistoryList.vue` | 113 | EXCELLENT | Lista de historias pinneadas para acceso rápido desde home; emits `dive` |
| `AuditHistory.vue` | 15 | EXCELLENT | Audit trail log de estación/device |
| `LogHistory.vue` | 15 | EXCELLENT | Records tipo log (string/event) en lista cronológica |

### 3.6 Backend Core (~6)

| Class | Role | LOC | Key methods / notes |
|---|---|---|---|
| `HistoryData.java` | Record retrieval + Jackson serialization | 663 | Builder pattern; consulta `BHistoryDatabase`; delega JSON a `HistoryObjectMapper` |
| `HistoryList.java` | Catalogue + caching | 355 | Enumera historias disponibles; caché opcional para performance |
| `HistoryGroups.java` | Grouping / hierarchy | 112 | Organiza records por folder/device para la UI |
| `HistoryGhostSubscriber.java` | Session keepalive | 26 | Ghost subscription sobre `BHistoryDatabase` para evitar timeout en queries largos |
| `HistoryIO.java` | I/O utilities | 103 | Lee/escribe history records a/desde JSON streams; delega a `HistoryData` |
| `BReflowHistoryCommands.java` | BOX command class | 112 | Expone RPCs BajaScript: `getList`, `getQuickList`, `getData`, `getGroupNames`, `getGroupTree`, `getDeviceTree`, `getDevices` |

### 3.7 Backend JSON Serializers — `history/json/` (~6)

| Class | Role | LOC | Notes |
|---|---|---|---|
| `HistoryObjectMapper.java` | Jackson ObjectMapper central | 20 | Registra todos los serializers; punto único de configuración |
| `HistoryRecordSerializer.java` | Serializer de records individuales | 122 | Convierte `BHistoryRecord` a JSON timestamped; usa `HistoryRecordOptions` |
| `HistoryDeviceSerializer.java` | Serializer de nodos device | 65 | Produce device JSON objects para el history tree |
| `HistoryFolderSerializer.java` | Serializer de nodos folder | 55 | Produce folder JSON objects para el history tree |
| `HistoryRecordOptions.java` | Value object de opciones | 29 | Format, timezone, precision para serialización de records |
| `IHistorySeralizer.java` | Interface contrato | 80 | Define `serialize()` implementado por todos los serializers (typo en nombre original: `Seralizer`) |

---

## 4. Cross-references

- **BReflowHistoryCommands** es el único punto de entrada BOX desde BajaScript/frontend: `getList`, `getQuickList`, `getData`, `getGroupNames`, `getGroupTree`, `getDeviceTree`, `getDevices`
- **`api/box.js`** — cliente JS que wrappea las llamadas BOX hacia `BReflowHistoryCommands`
- **`store/historyCache`** (Vuex module, **[FIXED-S56]** — full rewrite con `historyData` cache module-level (`xa`) + `historyService` 324 LOC (`Sa`: list/loadList/loadGroups/loadDevices/generate/data/buildHistoryQueryString/d3Options) + `groupsIndexBuilder` (`Ia`) + minimal Vuex store (`invalid` + `SET_INVALID` + `refresh`)) — consumido por prácticamente todos los 22 componentes del dominio; estado central del catálogo y datos de series. Rating actual: GOOD.
- **`store/dashboardCards`** — usado por `HistoryChart.vue` y `HistorySpark.vue` para asociar charts a cards de dashboard
- **D3chart (global)** — componente registrado globalmente en `main.js`; `HistoryChart` y `HistorySpark` lo envuelven con configuraciones distintas
- **`historyListMixin`** — compartido por `HistoriesHome`, `HistoryCard`, `HistoryRow`; abstrae lógica de selección/acción en lista
- **`$niagara.historyQuery`** — plugin global que abstrae la query de historia contra el station; usado desde el Builder
- **`RangeCalculator`** — util para calcular rangos de tiempo absolutos desde relativos (last-7d, etc.)
- **`CompareRangeCalculator`** — variante de `RangeCalculator` para modo compare (superpone dos rangos en el mismo chart)
- **`eventBus`** — plugin global de event bus; usado en `HistoriesHome` para comunicación cross-component sin props

---

## 5. Notes & Gotchas

- **[DESIGN] HistoryGhostSubscriber pattern**: crea una suscripción "ghost" (vacía) sobre `BHistoryDatabase` para mantener viva la sesión de history en el station durante queries pesados; sin esto, el station cierra la sesión por timeout antes de que la respuesta esté lista. Hack necesario por la arquitectura Niagara — replicar tal cual en MX60.
- **[OPEN-MX60] HistoryData.java 663 LOC**: clase más pesada del dominio backend; mezcla lógica de query, caché y serialización. Compila y funciona en Reflow; candidata a split en MX60 (al menos query / caché / serialize en clases separadas).
- **[INFO] Paquete separado `history/json/`**: los 6 serializers Jackson tienen su propio package. `HistoryObjectMapper` actúa como registry; toda la configuración de serialización pasa por ahí. Diseño limpio — copiable casi 1:1.
- **[INFO] Typo en `IHistorySeralizer.java`**: el nombre del archivo/clase tiene el typo `Seralizer` (falta la `i`). Es deuda cosmética del source de Reflow; **preservar al migrar para no romper referencias compiladas** salvo que se renombren todos los call sites a la vez.
- **[OPEN-MX60] D3chart es global**: `HistoryChart` y `HistorySpark` no importan D3chart directamente — lo consumen como componente global registrado en `main.js:64` (`Vue.component('D3chart', D3chart)`). El patrón funciona en Reflow (Vue 2 + Webpack); en MX60 con Vite + Vue 3 + Pinia hay que manejar con `app.component()` explícito o con auto-import — copy-paste sin atender esto rompe rendering en silencio.
- **[FIXED-S56] `historyCache` rewriteado**: el store que GAP-ANALYSIS había marcado para REWRITE se reescribió completo en S56. Estado actual: **GOOD**. La reescritura entregó: `historyData` cache module-level (`xa`), `historyService` 324 LOC (`Sa`: list/loadList/loadGroups/loadDevices/generate/data/buildHistoryQueryString/d3Options), `groupsIndexBuilder` recursivo (`Ia`), Vuex minimal (`invalid` + `SET_INVALID` + `refresh`). **Riesgo histórico, ya resuelto** — al transplantar a MX60, copiar el shape post-rewrite, no el pre-rewrite.
- **[INFO] `HistorySpark` vs `HistoryChart`**: ambos usan D3chart pero con configuraciones muy distintas. Spark es miniatura sin toolbar ni eje Y; Chart es full con delta mode, zoom y export. No son intercambiables.
- **[INFO] `CompareRangeCalculator`**: solo se activa en modo compare del chart (superpone dos series temporales de periodos distintos). Lógica separada de `RangeCalculator` base — si tocás `RangeCalculator`, mirá ambos.
- **[DESIGN] `HistoryStationCache.vue` renderless**: no renderiza nada; su única responsabilidad es disparar el pre-warm del cache al montarse en el árbol de componentes. Patrón renderless intencional con `render: () => null` — si lo borrás "porque no rendea", rompés el pre-warm.
- **[OPEN-MX60] LOC total frontend**: ~4.270 LOC distribuidos en 22 componentes (media ~194 LOC/componente). `HistoriesHome.vue` a 655 LOC es el componente más grande — funciona en Reflow, pero debería dividirse al portar a MX60 (Composition API permite extraer composables limpios).
