# Block 239 — Reflow: navegación/menú y el modelo de datos de equipment

> **Qué documenta.** El árbol de navegación (menú) de Reflow y el modelo de datos `equipment` (types vs items,
> encoding compacto). Gap BG29 (reapertura grupo E). Cierra el modelo de datos junto con B217 (dashboard) y B228
> (auto-binding).
>
> **Alcance.** El módulo `navigation` + el módulo `equipment`. La jerarquía building/floor se confirma (B217).
>
> **Fuentes (primarias).** SPA beautificada (`BF:`). Config real de disco (estructura). Barrido delegado (sonnet);
> tokens re-verificados.
>
> **Método / markers.** `[CERT]` = fuente primaria. `[INFER]` = deducción. SECRETS: estructura, no valores del sitio.

---

## 239.1 — Navegación: árbol de dos niveles `[CERT]`

Módulo `navigation` (`BF:7914`): `{enabled, maxVisible, items:[…], subnavs:[…]}`. Árbol de dos niveles:
- **`items[]`** (top-level, plano): cada `{id, name, enabled, type?, link:{linkType:"reflow", reflowLink:{label,
  children, id}}}`. Seeds por defecto: Home, Alarms, Schedules, Building, Equipment, Histories — los
  `link.reflowLink.id` son stems de ruta (`"alarms/"`, `"schedules/"`, `"buildings/"`, `"equipment/"`, `"histories/"`).
- **`subnavs[]`**: barras secundarias, cada una con sus `navItems[]`, atadas a entidades por `subnavId` (los buildings
  llevan un `subnavId`).

El campo `type` es un **selector de widget de UI**, no una categoría semántica (`BF:28208`): `"dropdown"`→`NavDropdown`,
`"dropdown-building"`→`NavDropdownBuilding`, else→link plano. Mutaciones: `ADD/UPDATE/REMOVE/REORDER_ITEM`,
`*_SUBNAV`, `*_NAVITEM_IN_SUBNAV`, y **`ADD_ITEM_FOR_PAGE`/`REMOVE_NAV_FOR_PAGE`** (`BF:8171`) — las páginas
auto-registran/desregistran su entrada de nav al crearse/borrarse (`reflowLink.id === "pages/"+pageId`). Getters
`itemsForPage`/`pageHasNavigation` (reverse lookup, para avisar al editor cuando borra una página aún linkeada).

**La restricción NO vive en `navigation`** `[CERT]`: es el módulo `profiles` (B238 §238.4) el que filtra el árbol al
renderizar (`authorizedNavigationItems`/`isNavItemAvailable`). `navigation` es el árbol CRUDO. El módulo `menu`
(`BF:9689`) es aparte y chico: solo `{mobileVisible, mobileOverflowOnly}` (estado del hamburger mobile).

## 239.2 — El modelo `equipment`: types (templates) vs items (devices) `[CERT]`

Módulo `equipment` (`BF:5664`) tiene AMBOS:
- **`types[]`** (`BF:5712`): 10 templates built-in (ahu/boiler/chiller/coolingTower/fcu/mua/rtu/vav/vfd/whp), cada
  `{id, name, reflow, defaultThumbnail, points, groups, featured, lockRemap, summaryColumnType, visible}`. Es la
  tabla de la que sale el auto-binding (B228).
- **`items[]`**: instancias de device reales. Keys: `id, type, name, ord, points, groups, badges, displayNames,
  floor, room, status, schedule, servedBy, featured, graphic, attachments, lockFromRemap`.

**Encoding compacto (extiende B228)** `[CERT]`: `we(item, type)` (`BF:3872`) al `ADD_ITEM`/`UPDATE_ITEM` convierte el
array `points[]` a un mapa `{pointId: ORD-relativo}` (`fe.relativize` contra el ORD base del item — strings más
cortos); `displayNames`/`badges`/`externals` se guardan SOLO para los puntos que difieren del template (delta puro).
La lectura reconstruye on-demand (`getPoints`, `BF:6186`, con un `Proxy` que intercepta `.points`/`.groups`, memoizado).
Relación `servedBy` (`BF:5942`): equipo-sirve-a-equipo (p. ej. AHU→VAV), limpiada al `REMOVE_ITEM`; `devices` = ORDs
Niagara crudos del item.

## 239.3 — Jerarquía building → floors + equipment `[CERT]`

Confirmado en disco (estructura): `buildings.items[]` lleva listas de ids `floors[]` + `equipment[]` + `subnavId`;
`equipment.items[].floor` es la back-ref al floor; `floorplans.items` tiene los floor records. Coincide con B217
§217.2 (building → floors[] + equipment[]; equipment → floor back-ref) — confirmado sobre datos reales de disco.
Todos los módulos (26 en total, root store `BF:13954`) se serializan juntos al `config.json` (B217 §217.3).

## 239.4 — Conexiones

- **[Block 217]** §217.2/217.3 — la jerarquía de superficies y el pool de cards; §239 agrega el árbol de nav y el
  modelo equipment que lo alimentan.
- **[Block 228]** — el auto-binding usa `equipment.types[]`; §239 muestra el encoding compacto del binding en
  `equipment.items[]`.
- **[Block 238]** §238.4 — `profiles` filtra el árbol de `navigation` crudo de §239.1.
- **Cierre parcial grupo E**: faltan BG24 (alarmas UI) y BG25 (schedules) para cerrar el grupo.
