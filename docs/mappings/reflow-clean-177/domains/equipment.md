# Equipment Domain — reflow-frontend/src/components/equipment/

**Files**: 44 Vue components  
**Fidelity (per GAP-ANALYSIS.md)**: FAIR overall; DeviceCard y DeviceRow calificados POOR  
**Backend coupling**: store `equipment` (POOR fidelity — 9 getters faltantes, cache system ausente), puntos vía plugin `$niagara` + subscriber, comandos de equipo vía `api/box.js`

---

## 1. Overview

El dominio equipment cubre la totalidad del ciclo de vida de dispositivos BMS en Reflow: navegación por listas y grillas de equipos agrupados por tipo o edificio, tarjetas de dispositivo con valores en vivo (subscriber a station), editor principal por tabs (puntos, adjuntos, gráfico, relaciones served-by/serves), formulario de creación/edición, remapeo de tipos, importación masiva vía CSV wizard, y pickers reutilizables de dispositivo/tipo. Todo el dominio opera en scope de building+floor multi-tenant; los tipos de equipo definen el schema de puntos y el ícono visual que se hereda en cada instancia.

---

## 2. Entry Points

| Archivo | Tipo | Ruta de activación | Notas |
|---|---|---|---|
| `EquipmentIndex.vue` | contenedor de módulo | `/equipment` (route raíz) | controla modo de vista (grid/list/table) y barra de búsqueda |
| `EquipmentList.vue` | wrapper de lista | `/equipment` (dashboard card) | envuelve `EquipmentItemList`, usado también como `BaseCard` dashboard widget |
| `EquipmentEditor.vue` | editor principal | abierto desde filas/tarjetas | tabs: puntos, adjuntos, gráfico, serves, served-by; usa `ConfigCell` del dominio config |
| `CSVWizard.vue` | wizard de importación | modal desde `EquipmentTypeSummary` | stub (15 LOC); depende de `BReflowFileCommands.uploadCSV` BOX method |

---

## 3. Components

### 3.1 List & Browse (10 componentes)

| Archivo | LOC | Fidelity | Propósito |
|---|---|---|---|
| `EquipmentIndex.vue` | 299 | FAIR | Contenedor top-level con search y control de view-mode |
| `EquipmentList.vue` | 272 | FAIR | Dashboard card wrapping `EquipmentItemList`; aplica `$color` theme |
| `EquipmentItemList.vue` | 559 | FAIR | Lista paginada con filtros, selección y slot de header |
| `EquipmentGrid.vue` | 307 | FAIR | Grilla que alterna entre `DeviceCard` y `DeviceRow` según view-mode |
| `DeviceGrid.vue` | 239 | FAIR | Grilla de múltiples `DeviceCard`; sin paginación propia |
| `CompactGroups.vue` | 123 | FAIR | Lista compacta de grupos de equipo para layouts densos |
| `CompactPointGroup.vue` | 177 | FAIR | Un grupo compacto de data-points dentro de un equipo |
| `GroupedDevicesDisplay.vue` | 149 | FAIR | Dispositivos agrupados por tipo/categoría con secciones colapsables |
| `DeviceTable.vue` | 28 | FAIR | Contenedor de tabla que orquesta `DeviceRow` con sorting/filtering |
| `DisplayStyle.vue` | 14 | FAIR | Toggle de estilo de display (card / table / list) |

### 3.2 Cards & Rows (8 componentes — DeviceCard y DeviceRow: POOR)

| Archivo | LOC | Fidelity | Propósito |
|---|---|---|---|
| `DeviceCard.vue` | 483 | **POOR** | Tarjeta con valores de punto en vivo vía subscriber; reconstrucción incompleta (mapState/mapGetters presentes pero lógica de subscribe parcial) |
| `DeviceRow.vue` | 314 | **POOR** | Fila de tabla con acciones inline; subscribe a station similar a DeviceCard — misma causa raíz |
| `DeviceTitle.vue` | 358 | FAIR | Header editable de dispositivo con ícono y nombre |
| `DeviceTip.vue` | 81 | FAIR | Tooltip/popover con detalles de punto al hover |
| `GroupCard.vue` | 346 | FAIR | Tarjeta de grupo de equipos con item count y resumen |
| `GroupRow.vue` | 189 | FAIR | Fila de grupo con lista de items expandible |
| `EquipmentTypeSummary.vue` | 170 | FAIR | Tarjeta read-only de tipo de equipo para páginas de overview |
| `EquipmentTypeSummaryEditor.vue` | 414 | FAIR | Versión editable de `EquipmentTypeSummary` con save/cancel inline |

### 3.3 Editor & Config Forms (11 componentes)

| Archivo | LOC | Fidelity | Propósito |
|---|---|---|---|
| `EquipmentEditor.vue` | 379 | FAIR | Editor principal con tabs (puntos, adjuntos, gráfico, serves, served-by); usa `ConfigCell` del dominio config |
| `EquipmentEditorPoints.vue` | 213 | FAIR | Tab de gestión de point-bindings del equipo |
| `EquipmentEditorAttachments.vue` | 228 | FAIR | Tab de listado de adjuntos existentes |
| `EquipmentEditorAddAttachments.vue` | 254 | FAIR | Sub-panel de upload y linkeo de adjuntos |
| `EquipmentEditorGraphic.vue` | 282 | FAIR | Tab de asignación/edición del asset gráfico/floorplan |
| `EquipmentEditorServedBy.vue` | 96 | FAIR | Tab de equipos upstream que sirven a este dispositivo |
| `EquipmentEditorServes.vue` | 116 | FAIR | Tab de equipos downstream servidos por este dispositivo |
| `EquipmentAdd.vue` | 290 | FAIR | Dialog/panel para agregar un nuevo item de equipo |
| `DeviceForm.vue` | 199 | FAIR | Form de creación/edición con campo ORD → `$niagara` nav + `ConfigCell` |
| `EquipmentTypeForm.vue` | 344 | FAIR | Form de creación/edición de un tipo de equipo con schema de puntos |
| `EquipmentTypeSettings.vue` | 101 | FAIR | Panel de settings de tipo (units, display, thresholds) |

### 3.4 CSV Wizard (2 componentes)

| Archivo | LOC | Fidelity | Propósito |
|---|---|---|---|
| `CSVWizard.vue` | 15 | FAIR | Shell del wizard multi-step; stub — sólo slot y props `visible`/`typeId` |
| `EquipmentItemRemap.vue` | 335 | FAIR | UI de remapeo de items a distintos tipos o grupos (relacionado al flujo CSV) |

> Nota: el wizard está incompleto en la reconstrucción. La lógica real de upload invoca `BReflowFileCommands.uploadCSV` vía BOX method en el backend Java.

### 3.5 Tipos de Equipo (4 componentes)

| Archivo | LOC | Fidelity | Propósito |
|---|---|---|---|
| `EquipmentType.vue` | 235 | FAIR | Vista de un tipo de equipo con sus items y configuración |
| `EquipmentGroupOrder.vue` | 123 | FAIR | Drag-and-drop para reordenar grupos de equipo |
| `EquipmentBadgeForm.vue` | 244 | FAIR | Form de creación/edición de badge/label en un item |
| `EquipmentBadgeList.vue` | 273 | FAIR | Lista de badges asociados a un item de equipo |

### 3.6 Pickers & Misc (9 componentes)

| Archivo | LOC | Fidelity | Propósito |
|---|---|---|---|
| `PickerModal.vue` | 582 | FAIR | Modal genérico usado por device y type pickers |
| `DevicePicker.vue` | 133 | FAIR | Modal/inline para seleccionar un dispositivo |
| `DeviceSelect.vue` | 38 | FAIR | Dropdown simple de un dispositivo |
| `TypeSelect.vue` | 14 | FAIR | Dropdown de tipo de equipo |
| `TypesPicker.vue` | 15 | FAIR | Multi-select de tipos de equipo |
| `IconSelect.vue` | 82 | FAIR | Icon picker para asignar ícono a tipo o item |
| `ItemEditStyle.vue` | 14 | FAIR | Editor inline de estilo visual de un item |
| `TableOptionsMenu.vue` | 14 | FAIR | Menú dropdown con acciones de tabla (export, column visibility) |
| `ViewsMenu.vue` | 15 | FAIR | Menú para cambiar entre vistas guardadas (grid, list, table) |

---

## 4. Cross-references

- **Vuex store `equipment`** (POOR) — 9 getters faltantes (`authorizedForDevice`, `authorizedForGroup`, `emptyGroup`, `getDevicesBetter`, `getGroupById`, `getGroupDeviceTypes`, `getNotesForDevice`, `pointMapForTemplate`, `pointsForTemplate`); cache de puntos ausente; `getPoints` es un stub de 6 líneas (bundle usa 42)
- **Plugin `$niagara`** — usado por `DeviceCard` y `DeviceRow` para subscribe a valores de punto en vivo desde la station Niagara; también en `DeviceForm` para resolución de ORDs
- **`api/box.js`** — comandos de equipo (create, update, delete device/group/type); `BReflowFileCommands.uploadCSV` para el CSV wizard
- **`BReflowNavCommands`** (backend) — resolución de ORDs para el campo Device en `DeviceForm` y `EquipmentEditorGraphic`
- **`ConfigCell`** del dominio `config` — reutilizado en `EquipmentEditor`, `DeviceForm`, y varios formularios del dominio
- **Store `equipmentData`** (GOOD desde S53) — preferencias de vista UI por tipo (columns, view mode); separado del store `equipment` que tiene los datos reales

---

## 5. Notes & Gotchas

- **store `equipment` es POOR**: la reconstrucción tiene 9 getters ausentes y el sistema de cache de puntos está omitido. `getPoints` retorna stub; `getItemById` usa O(n) en vez del O(1) del bundle. Cualquier feature que lean punto-a-punto de este store va a comportarse diferente al bundle original.
- **DeviceCard / DeviceRow POOR**: el subscribe vía `$niagara` está parcialmente reconstruido. Los valores en vivo pueden no actualizarse correctamente. Causa raíz: subscriber lifecycle (created/beforeDestroy) incompleto.
- **CSVWizard es un stub**: 15 LOC, sólo shell. El wizard multi-step real del bundle depende de `BReflowFileCommands.uploadCSV` (BOX method Java) y tiene pasos de validación de columnas, preview de datos, y confirmación. No implementado en la reconstrucción.
- **EquipmentEditor usa ConfigCell del dominio config**: dependencia cross-domain; si `ConfigCell` tiene issues de fidelity, se propagan a todos los tabs del editor.
- **Ruta fantasma `/equipment/floor/:id`**: presente en la reconstrucción pero ausente en el bundle. Debe eliminarse.
- **3 route component mismatches**: `/equipment/type/:id` apunta a `EquipmentIndex` en la reconstrucción, pero el bundle usa `EquipmentTypeSummary` (vista dedicada con ViewsMenu, PointPicker, CSV export).
- **PickerModal.vue (582 LOC)** es el más grande del dominio y es un modal genérico compartido por múltiples pickers — cambios en él impactan DevicePicker y TypesPicker.
- **EquipmentItemList.vue (559 LOC)** es el componente de lista más complejo; cualquier bug de paginación o filtrado se origina aquí.
- **DeviceTitle.vue (358 LOC)**: mayor que lo esperado para un header — probablemente incluye lógica de edición inline + validación + ORD binding.
