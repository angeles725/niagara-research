# Alarms Domain — reflow-frontend/src/components/alarms/ (+ backend integration)

**Frontend files**: 27 Vue components in `reflow-frontend/src/components/alarms/`
**Backend coupling**: `BReflowAlarmCommands` (BOX RPC), `AlarmData` (BQL queries + CSV export), `QueryFilter`, `AlarmSourceCollection`, `ReflowAlarmSource` (BIAlarmSource wrapper), `AlarmUuidArgs`
**Fidelity (per GAP-ANALYSIS.md, verified 2026-05-09)**: GOOD para todos los componentes; ninguno marcado EXCELLENT individual pero el dominio es funcional end-to-end. Stores: `alarms` **[FIXED-S55]** (5 bugs estructurales arreglados) + `alarmData` **[FIXED-S54]** (massively over-built → stripped a bundle match). Componentes: `AlarmsTable` **[FIXED-S50 V-P0-8]** + `AlarmCards` **[FIXED-S50 V-P0-9]**.

**Status legend**: `[OPEN]` unresolved in Reflow source · `[FIXED-SXX]` resolved in sprint XX · `[DESIGN]` intentional Niagara/architectural decision · `[INFO]` descriptive note, no temporal status · `[OPEN-MX60]` concern applies only when migrating to MX60.

---

## 1. Overview

El dominio de alarmas implementa una consola de alarmas configurable multi-fuente sobre Niagara N4. La UI permite al operador ver alarmas en modo tabla (`AlarmsTable`) o tarjeta (`AlarmCards`), filtrar por prioridad/estado/fuente, reconocer individualmente o en lote con nota requerida (`RequiredNoteModal`, `AlarmAckConfirm`), reproducir sonidos por prioridad (`AlarmSoundsForm`, `AlarmSoundsPicker`) y ver resúmenes por edificio (`BuildingAlarmSummary`, `Total`). La configuración de cada consola (columnas, estilos de fila, iconos, prioridades, fuentes) se edita en un conjunto de sub-formularios (`AlarmConsoleForm` + 8 hijos). En el backend, `BReflowAlarmCommands` expone métodos BOX que emiten BQL sobre `BAlarmDatabase` a través de `AlarmData`; `ReflowAlarmSource` envuelve una referencia ORD a un `BIAlarmSource` de Niagara para que el motor pueda enumerar las fuentes configuradas en la consola; la exportación CSV se produce server-side dentro de `AlarmData.exportCSV`.

---

## 2. Entry Points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `reflow-frontend/src/components/alarms/AlarmDisplay.vue` | vue-component | Orquestador principal de la consola: coordina tabla + cards + paginación + ack |
| `reflow-frontend/src/components/alarms/AlarmsTable.vue` | vue-component | Vista tabla con multi-select, ack, notas, detalle; 427 LOC |
| `reflow-frontend/src/components/alarms/AlarmCards.vue` | vue-component | Vista tarjetas con paginación y acciones; 371 LOC |
| `reflow-frontend/src/components/alarms/BuildingAlarmSummary.vue` | vue-component | Entry point del dashboard: resumen por edificio con dive link |
| `nmodsreflow/.../commands/BReflowAlarmCommands.java` | java-class | BOX entry point backend; expone 7 métodos RPC al frontend |

---

## 3. Components

### 3.1 Consola y tabla (~6)

| Archivo | LOC | Fidelidad | Propósito |
|---------|-----|-----------|-----------|
| `AlarmDisplay.vue` | 295 | GOOD | Orquestador: tabla/cards, stores `alarms`+`alarmData`+`colors`+`documentData` |
| `AlarmsTable.vue` | 427 | GOOD | Tabla paginada, multi-select ack/notas/detalle; plugins `dayjs` |
| `AlarmCards.vue` | 371 | GOOD | Vista cards con pagination; emits `ack`, `load-next-page`, `load-previous-page` |
| `AlarmList.vue` | 16 | GOOD | Wrapper delgado que renderiza lista de records para una consola |
| `SourceGroupsTable.vue` | 656 | GOOD | Tabla de grupos de fuente con ack-all/ack-recent; plugins `dayjs`, `eventBus` |
| `Total.vue` | 128 | GOOD | Badge con conteo total de alarmas activas entre todas las consolas |

### 3.2 Acknowledge / Confirm (~3)

| Archivo | LOC | Fidelidad | Propósito |
|---------|-----|-----------|-----------|
| `AlarmAckConfirm.vue` | 92 | GOOD | Diálogo de confirmación de ack; valida nota opcional; emits `input`, `load-alarms` |
| `RequiredNoteModal.vue` | 95 | GOOD | Modal que exige nota antes del ack; emit `ack` |
| `AlarmNotes.vue` | 124 | GOOD | Editor inline de nota para un alarm record; emits `error`, `success` |
| `AlarmNotesModal.vue` | 64 | GOOD | Modal que envuelve el editor de notas; emit `input` |

### 3.3 Prioridad / Clase / Fuente (~6)

| Archivo | LOC | Fidelidad | Propósito |
|---------|-----|-----------|-----------|
| `AlarmClassList.vue` | 160 | GOOD | Lista de alarm classes para configuración de consola |
| `AlarmPrioritiesForm.vue` | 81 | GOOD | Mapeo de prioridades a labels y orden; emit `dive` |
| `AlarmPriorityPicker.vue` | 160 | GOOD | Dropdown para seleccionar nivel de prioridad; emit `input` |
| `AlarmPriorityType.vue` | 279 | GOOD | Badge visual de tipo/prioridad (color + icono); stores `alarms`+`colors` |
| `AlarmStatusPicker.vue` | 158 | GOOD | Control filtro ack/unack; emits `input`, `update:unackOnly` |
| `BuildingAlarmSummary.vue` | 114 | GOOD | Resumen por edificio con breakdown por prioridad; stores `alarms`+`buildings` |

### 3.4 Sonido / Notificación (~2)

| Archivo | LOC | Fidelidad | Propósito |
|---------|-----|-----------|-----------|
| `AlarmSoundsForm.vue` | 252 | GOOD | Asigna audio a prioridades; mixin `checkedItemsMixin` |
| `AlarmSoundsPicker.vue` | 88 | GOOD | Picker de clip de sonido para una prioridad; emit `input` |

### 3.5 Sub-formularios de configuración de consola (~9)

| Archivo | LOC | Fidelidad | Propósito |
|---------|-----|-----------|-----------|
| `AlarmConsoleForm.vue` | 287 | GOOD | Form crear/editar consola; emit `dive` |
| `AlarmConsoleList.vue` | 93 | GOOD | Lista draggable de consolas; plugin `vuedraggable`; emit `dive` |
| `AlarmIconsForm.vue` | 130 | GOOD | Configura iconos por prioridad |
| `AlarmRowStyleForm.vue` | 333 | GOOD | Colores y fuentes por estado de alarma |
| `AlarmSummaryForm.vue` | 90 | GOOD | Configura card de resumen por edificio |
| `AlarmsTableForm.vue` | 149 | GOOD | Personaliza columnas y opciones de la tabla |
| `ConsoleRefreshRateForm.vue` | 47 | GOOD | Intervalo de auto-refresh de la consola |
| `PriorityColorsForm.vue` | 108 | GOOD | Colores custom por nivel de prioridad; stores `alarms`+`colors` |
| `SourcesTableForm.vue` | 150 | GOOD | Configura filtro de fuentes de la consola |

### 3.6 Backend (~6)

| Clase | Rol | LOC | Acciones / BOX methods |
|-------|-----|-----|------------------------|
| `BReflowAlarmCommands.java` | BOX command entry point | 113 | `getClasses`, `query`, `querySources`, `getActiveAlarmCounts`, `getUnackedAlarmCounts`, `getAlarmsSinceTime`, `canAcknowledgeAlarms` |
| `AlarmData.java` | Capa de datos: BQL + CSV export | 439 | Ejecuta queries BQL sobre `BAlarmDatabase`, genera CSV export, UUID lookup |
| `QueryFilter.java` | Value object filtro | 158 | Encapsula time range, severity, source para BQL queries |
| `AlarmSourceCollection.java` | Contenedor multi-fuente | 83 | Agrega referencias a múltiples `ReflowAlarmSource` para query aggregation |
| `ReflowAlarmSource.java` | Wrapper ORD → BIAlarmSource | 25 | Referencia a fuente de alarmas Niagara; usado en enumeración por el motor |
| `AlarmUuidArgs.java` | Value object UUID args | 74 | Transporta UUIDs de alarma para llamadas BOX de ack y query |

---

## 4. Cross-references

- `BReflowAlarmCommands` es el único BOX command class para alarmas; el frontend lo invoca vía `api/box.js` con `box.invoke('BReflowAlarmCommands', method, args)`.
- Store Vuex `alarms` (persistent): persiste configuración de consolas, preferencias de tabla, prioridades y sonidos.
- Store Vuex `alarmData` (transient): cache en memoria de los registros de alarma paginados para la sesión activa.
- `eventBus` usado en `SourceGroupsTable` para comunicar eventos de ack entre componentes no padres-hijos.
- Plugin `$niagara` — método `alarmSubscribe` para suscripción live a nuevas alarmas vía WebSocket/ChannelService backend.
- `alarmCache` (lib): puente entre queries BQL síncronas del backend y el store reactivo de Vuex; evita re-queries redundantes en cambios de vista.
- `dayjs` plugin usado en `AlarmsTable` y `SourceGroupsTable` para formateo de timestamps de alarma.
- `vuedraggable` en `AlarmConsoleList` para reordenamiento drag-and-drop de consolas.
- Backend `ChannelService` emite eventos de alarma activa al frontend a través del canal WebSocket; el plugin `$niagara.alarmSubscribe` los consume.

---

## 5. Notes & Gotchas

- **[DESIGN] AlarmSubscriber pattern**: el frontend no hace polling; usa `$niagara.alarmSubscribe` que internamente conecta al `ChannelService` backend. El store `alarmData` se actualiza reactivamente cuando llegan eventos push. El método `getAlarmsSinceTime` en `BReflowAlarmCommands` cubre el caso de reconexión/gap. **Caveat [OPEN]**: el cliente WebSocket (`api/websocket.js`) está comentado en `main.js` (Phase 5+); en producción real la suscripción depende de que esa capa se reactive — sin WS no hay push live.
- **[DESIGN] alarmCache como puente**: el módulo `alarmCache` de lib actúa como caché local para queries BQL — evita re-query al backend en cada cambio de tab/view. Es transient (no persistido en localStorage). Patrón válido — replicar tal cual en MX60.
- **[DESIGN] ReflowAlarmSource vs BIAlarmSource nativo**: `ReflowAlarmSource.java` es un wrapper delgado (25 LOC) sobre una referencia ORD, no un `BComponent` propio. Esto significa que Reflow expone las fuentes de alarma existentes de Niagara, no crea una nueva jerarquía; la enumeración queda integrada con el árbol estándar de `BAlarmDatabase`. Decisión arquitectónica intencional — copiable.
- **[DESIGN] CSV export server-side**: `AlarmData.exportCSV` genera el stream en el servidor dentro de la JVM de Niagara. El frontend dispara la descarga vía una llamada BOX con `querySources`; no hay generación client-side con blobs. Replicable.
- **[DESIGN] ack con nota requerida**: la lógica de negocio que determina si la nota es obligatoria vive en `AlarmAckConfirm` (validación client) y también en backend (`canAcknowledgeAlarms` BOX method). El componente `RequiredNoteModal` es el paso forzado cuando `canAcknowledgeAlarms` retorna que se requiere nota. Doble validación intencional — preservar la coordinación cliente↔servidor.
- **[OPEN-MX60] SourceGroupsTable es el componente más pesado** (656 LOC verificados por `wc -l`): agrega múltiples fuentes en una tabla unificada con ack masivo; acoplado a `alarmData` store + `eventBus`. Funciona en Reflow; al portar a MX60 conviene split + reemplazo de `eventBus` por composable / Pinia store. Sin tests unitarios en el directorio (`find` no encuentra `.test|.spec`).
- **[INFO] Fidelidad general GOOD post-fixes**: ningún componente fue catalogado EXCELLENT, pero **el dominio es funcional end-to-end**. Los sub-formularios de configuración (9 componentes) dependen del store `alarms` sin props drilling, lo que los hace testeables solo con store mockeado — patrón a romper en la migración a MX60 con Composition API + props explícitas.
- **[FIXED-S50/S54/S55] Historial de fixes del dominio**: el dominio llegó a GOOD a fuerza de fixes nominados. Resumen para evitar reintroducir los mismos bugs en MX60:
  - **S50** — `AlarmsTable` 5 columns + selection (V-P0-8); `AlarmCards` watchers + events (V-P0-9); rewrite de `notify` store; `mouseData` array-based.
  - **S54** — `alarmData` stripped a bundle match: 2 state (`loading`, `currentTimeRanges[]` array no objeto), 2 mutations (`SET_LOADING`, `SET_CURRENT_TIME_RANGE` array-upsert), 3 getters (`priorityForRecord` con class/range, `inAlarmCount` con building/ignored/priority filters, `currentTimeRange` con fallback a console default). **Lección MX60**: NO inventar `records`/`totalRecords`/`currentPage`/`pageSize`/`queryFilters` en el store — esos vivian en componentes locales en el bundle original.
  - **S55** — 5 bugs del store `alarms`: (1) `ignoredAlarmClasses` ahora filtra `enabled===false`, mapea `.class`, deduplica; (2) `getStylesForConsole` mergea los 7 defaults (alert/fault/offnormal/normal/ack/unack/ackState) y soporta dispatch string-vs-object; (3) `REMOVE_CONSOLE` hace cross-module commits dentro de la mutation con `this.commit()`, no en una action separada; (4) agregada action `migrateAlarmConsoleRestrictionType`; (5) `getNewConsole` usa UUID + sequential naming "Alarm Console N". **Lección MX60**: replicar las 5 reglas, no la versión pre-S55.
