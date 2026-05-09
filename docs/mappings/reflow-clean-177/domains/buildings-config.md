# Buildings + Config Domain — reflow-frontend/src/components/{buildings,config}/

**Files**: 51 (buildings: 27, config: 24)
**Fidelity (GAP-ANALYSIS.md)**: buildings domain FAIR (store parcialmente reconstruido); config domain GOOD
**Backend coupling**: `BReflowSyncService.java` para config.json persistence + multi-user locking, `ConfigIO.java` para delta sync con cache, `SocketServlet.java` + `BReflowWebSocketAcceptor.java` para broadcast en tiempo real, `api/websocket.js` frontend client

---

## 1. Overview

El dominio buildings gestiona la jerarquía site/building/floor/zone que estructura toda la navegacion del SPA: `BuildingIndex` muestra el portfolio completo con lista + mapa interactivo; al hacer click se entra al detalle via `BuildingLayout` que sirve de shell con `BuildingSubNav` para las secciones internas (Dashboard, Floors, Alarms, Histories, Schedules, Location). El store `store/buildings.js` (783 LOC, FAIR) centraliza el estado de la jerarquia pero tiene fidelidad limitada — la reconstruccion del arbol completo puede ser parcial. El dominio config es el motor de configuracion del SPA entero: `ConfigView` + `ConfigMenu`/`MenuNode` forman el modo edicion que expone secciones tipadas, cada celda editada pasa por `ConfigCell` (391 LOC) que actua como dispatcher hacia 14 variantes tipadas (text, number, select, switch, color, icon, image, delete, save, etc). Al guardar, el stack frontend serializa el estado Vuex via `configSerializer.js`, lo pasa por `deepMerge.js` y lo persiste en `^/reflow/config.json` via WebSocket + `BReflowSyncService`. Los dos dominios comparten el patron config-item: `BuildingListConfigItem` usa el mismo row/cell schema que los config cells, y `buildingConfigMixin.js` conecta formularios de buildings con helpers de config. La migracion de schema entre versiones la maneja `configMigration.js` (653 LOC).

---

## 2. Entry Points

| Archivo | Tipo | Rol |
|---|---|---|
| `reflow-frontend/src/components/buildings/BuildingIndex.vue` | vue-component | Root view buildings — lista + mapa + contadores |
| `reflow-frontend/src/components/buildings/BuildingList.vue` | vue-component | Lista scrollable con filtro/busqueda |
| `reflow-frontend/src/components/buildings/BuildingLayout.vue` | vue-component | Shell layout para detalle de building |
| `reflow-frontend/src/components/buildings/BuildingEditor.vue` | vue-component | Edicion CRUD building (no en index.json — puede estar como BuildingForm) |
| `reflow-frontend/src/components/config/ConfigView.vue` | vue-component | Root view config mode — menu + seccion activa |
| `reflow-frontend/src/components/config/ConfigMenu.vue` | vue-component | Navigation tree del config mode |
| `reflow-frontend/src/store/modules/buildings.js` | js-store | Vuex persistente — jerarquia buildings/floors/zones (783 LOC) |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/sync/BReflowSyncService.java` | java-class | Config sync service — locking multiusuario + broadcast WebSocket |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/sync/ConfigIO.java` | java-class | Read/write config.json con cache — 66KB payload |
| `reflow-frontend/src/api/websocket.js` | js-api | Cliente WebSocket frontend — suscripciones canal + config-delta sync |

---

## 3. Components

### 3.1 Buildings — List & Card (~7)

| Archivo | LOC | Fidelity | Proposito |
|---|---|---|---|
| `BuildingIndex.vue` | 116 | FAIR | Root index: combina lista, mapa y contadores |
| `BuildingIndexCounts.vue` | 117 | FAIR | Badges de resumen (total buildings, alarmas activas) |
| `BuildingList.vue` | 103 | FAIR | Lista scrollable con controles de busqueda y filtro |
| `BuildingListItem.vue` | 95 | FAIR | Fila unica — nombre, estado, acciones rapidas |
| `BuildingListConfigItem.vue` | 123 | FAIR | Fila de config para customizar visibilidad de columnas en la lista |
| `BuildingCard.vue` | 384 | FAIR | Card widget — nombre, estado, metricas KPI resumidas |
| `BuildingGrid.vue` | 351 | FAIR | Grid layout para multiples building cards |

### 3.2 Buildings — Editor & Forms (~6)

| Archivo | LOC | Fidelity | Proposito |
|---|---|---|---|
| `BuildingForm.vue` | 189 | FAIR | Create/edit form — nombre, direccion, timezone |
| `BuildingGroupForm.vue` | 97 | FAIR | Form para crear/editar building group (portfolio) |
| `BuildingPicker.vue` | 93 | FAIR | Dropdown searchable para seleccionar building |
| `BuildingLocation.vue` | 71 | FAIR | Display + edicion de coordenadas lat/lng |
| `BuildingHeroContent.vue` | 97 | FAIR | Hero section de detalle — metricas headline + imagen |
| `StatusWrap.vue` | 76 | FAIR | Wrapper generico que aplica styling basado en status |

### 3.3 Buildings — Layout & Navigation (~6)

| Archivo | LOC | Fidelity | Proposito |
|---|---|---|---|
| `BuildingLayout.vue` | 195 | FAIR | Shell layout para paginas de detalle — nav + slot regions |
| `BuildingSubNav.vue` | 211 | FAIR | Tabs secundarios de detalle (Dashboard, Floors, Alarms, etc.) |
| `BuildingDashboard.vue` | 70 | FAIR | Overview page con KPI widgets |
| `BuildingStatusDisplay.vue` | 153 | FAIR | Badge / indicador de estado operacional |
| `BuildingAlarms.vue` | 165 | FAIR | Panel de alarmas activas del building |
| `BuildingSchedules.vue` | 142 | FAIR | Listado y gestion de schedules operacionales |

### 3.4 Buildings — Map & Hierarchy (~8)

| Archivo | LOC | Fidelity | Proposito |
|---|---|---|---|
| `BuildingMap.vue` | 84 | FAIR | Mapa interactivo de ubicacion del building |
| `BuildingMapMarker.vue` | 89 | FAIR | Marker custom para pin de building individual |
| `BuildingIndexMarker.vue` | 160 | FAIR | Marker en el mapa del index portfolio |
| `BuildingIndexMarkerList.vue` | 106 | FAIR | Lista scrollable de markers dentro de un popover del mapa |
| `BuildingIndexPopovers.vue` | 72 | FAIR | Gestiona overlays popover en markers del index map |
| `BuildingGroupMarker.vue` | 154 | FAIR | Marker de cluster para building group en mapa |
| `BuildingFloors.vue` | 129 | FAIR | Lista de floors con navegacion a cada floor plan |
| `BuildingHistories.vue` | 97 | FAIR | Panel de historicos con trend charts a nivel building |

### 3.5 Config — View & Menu (~5)

| Archivo | LOC | Fidelity | Proposito |
|---|---|---|---|
| `ConfigView.vue` | 394 | GOOD | Root view config mode — renderiza menu + seccion activa |
| `ConfigMenu.vue` | 234 | GOOD | Navigation menu tree del config panel, driven by menuTree data |
| `MenuNode.vue` | 317 | GOOD | Nodo recursivo del config menu — renderiza items con hijos |
| `ConfigOptions.vue` | 15 | GOOD | Panel de opciones globales accesible desde config mode |
| `ConfigReset.vue` | 113 | GOOD | Boton + dialogo de confirmacion para resetear seccion a defaults |

### 3.6 Config — Typed Cells (~14)

| Archivo | LOC | Fidelity | Proposito |
|---|---|---|---|
| `ConfigCell.vue` | 391 | GOOD | Core cell — dispatcher que rutea al sub-tipo correcto segun schema |
| `ConfigCellText.vue` | 47 | GOOD | Variante texto libre |
| `ConfigCellNumber.vue` | 71 | GOOD | Variante numerica con min/max/step constraints |
| `ConfigCellSelect.vue` | 80 | GOOD | Variante dropdown con options list predefinida |
| `ConfigCellSwitch.vue` | 44 | GOOD | Variante toggle boolean |
| `ConfigCellColor.vue` | 45 | GOOD | Variante color picker hex |
| `ConfigCellColorPreset.vue` | 94 | GOOD | Variante color con palette de swatches presets |
| `ConfigCellPreferredColor.vue` | 39 | GOOD | Variante seleccion de color de tema desde palette definida |
| `ConfigCellIcon.vue` | 48 | GOOD | Variante selector de icono desde icon library |
| `ConfigCellImage.vue` | 41 | GOOD | Variante upload/seleccion de asset de imagen |
| `ConfigCellInfo.vue` | 46 | GOOD | Variante read-only informacional |
| `ConfigCellButton.vue` | 45 | GOOD | Variante boton de accion clickeable |
| `ConfigCellEmitButton.vue` | 157 | GOOD | Variante que emite evento custom definido por el cell schema |
| `ConfigCellDelete.vue` | 124 | GOOD | Variante delete con paso de confirmacion |
| `ConfigCellSave.vue` | 123 | GOOD | Variante save que dispara persist con loading state |
| `ConfigCellTitle.vue` | 19 | GOOD | Variante header/titulo de seccion en la config table |

### 3.7 Config — Actions & Backup (~5)

| Archivo | LOC | Fidelity | Proposito |
|---|---|---|---|
| `ConfigButton.vue` | 86 | GOOD | Boton de accion reusable con loading + disabled states |
| `ConfigBackupControl.vue` | 19 | GOOD | Panel de control para trigger y gestion de backups |
| `ConfigBackupItem.vue` | 18 | GOOD | Fila de backup — timestamp, size, accion restore |
| `ConfigOptions.vue` | 15 | GOOD | (ver 3.5) opciones globales |

---

## 4. Cross-references

- `BReflowSyncService.java` (599 LOC, sync-config) — gestiona el lock multiusuario via `grantConfigControl`, hace broadcast WebSocket on save; toda edicion del config mode pasa por aqui antes de llegar a disco.
- `ConfigIO.java` (252 LOC, sync-config) — persiste el 66KB config.json en el filesystem de la station con cache layer; es la capa I/O que `BReflowSyncService` delega para leer/escribir.
- `reflow-frontend/src/api/websocket.js` (155 LOC, api) — cliente WebSocket frontend; maneja channel subscriptions para config-delta sync y señales de control multiusuario.
- `SocketServlet.java` / `BReflowWebSocketAcceptor.java` — entry point de WebSocket en Niagara; despacha `IReflowCommand` y maneja config sync broadcast.
- `reflow-frontend/src/lib/configMigration.js` (653 LOC, lib) — runner de migraciones de version; aplica steps ordenados para actualizar config.json de versiones anteriores del schema.
- `reflow-frontend/src/lib/configSerializer.js` (26 LOC, lib) — serializa el estado Vuex stripeando keys transient/excluded antes de persistir a Niagara.
- `reflow-frontend/src/lib/deepMerge.js` (21 LOC, lib) — deep-merge recursivo usado en `LOAD_STATE` para mezclar config cargada sobre el estado Vuex existente.
- `reflow-frontend/src/store/modules/buildings.js` (783 LOC, store) — modulo Vuex persistente; FAIR fidelity segun GAP-ANALYSIS — la reconstruccion del arbol buildings puede ser parcial.
- `reflow-frontend/src/mixins/buildingConfigMixin.js` (43 LOC, mixins) — mixin con helpers de config para formularios de buildings y dispatch wrappers.
- `reflow-frontend/src/plugins/configMode.js` (11 LOC, plugins) — plugin Vue que expone `$configMode` reactive flag (view vs edit) en el prototype.
- `reflow-frontend/src/components/profiles/ConfigViewUsers.vue` (217 LOC, profiles) — admin view de config que lista usuarios Niagara y asignaciones de perfil; usa el mismo ConfigView shell.
- RBAC: `authorizeLink` plugin y `isPathAvailable` desde profiles controlan que secciones del config menu son visibles por rol.

---

## 5. Notes & Gotchas

- **store/buildings FAIR**: el Vuex module de buildings (783 LOC) fue marcado FAIR en GAP-ANALYSIS, indicando que la reconstruccion del estado de la jerarquia building/floor/zone puede ser incompleta. En MX60 hay que validar que las mutaciones de arbol (add/remove floor, reorder zone) esten completamente mapeadas.
- **ConfigCell como dispatcher**: `ConfigCell.vue` (391 LOC) no renderiza nada por si mismo — su logica entera es determinar que variante typed renderizar segun el campo `type` del cell schema. En Pinia/Vue 3 esto se puede simplificar con dynamic component + defineAsyncComponent.
- **Lock multiusuario**: antes de entrar en modo edicion de config, el frontend debe adquirir el lock via WebSocket (`grantConfigControl` en `BReflowSyncService`). Si otro usuario ya tiene el lock, el modo edicion queda bloqueado. Esta señal llega por `api/websocket.js`. Sin el lock, los cambios se descartan en backend.
- **configMigration.js (653 LOC)**: el runner de migraciones es el archivo js-lib mas grande del proyecto. Cada version de schema agrega un step; en MX60 hay que integrar este runner en el ciclo de inicializacion del store antes de que cualquier componente lea el estado.
- **config/ tiene 24 entradas**: coincide con el conteo real de Batch A (vs estimado original de 22). `CSVWizard` fue movido al dominio equipment — no pertenece a config/.
- **BuildingListConfigItem.vue**: vive en `buildings/` pero implementa el mismo patron row/cell que los `ConfigCell` variants — es el punto de acoplamiento directo entre los dos dominios.
- **Patron celda tipada compartido**: en la migracion MX60, los ConfigCell variants y el patron de BuildingListConfigItem pueden unificarse en un unico sistema de schema-driven cells, reduciendo duplicacion entre los dos dominios.
- **ConfigBackupControl/Item son muy pequenos** (19 y 18 LOC): posiblemente wrappers delgados sobre la API de backup de `BReflowSyncService`. Verificar si la logica de backup esta en el backend o en el frontend.
