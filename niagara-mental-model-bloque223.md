# Block 223 — Reflow dashboard-builder (VIII): el editor visual y el layout (masonry)

> **Qué documenta.** Cómo funciona el EDITOR de Reflow: cómo se entra en modo edición, cómo se ordena el layout
> (grilla masonry), cómo se mueve/redimensiona una tarjeta y cómo se crean páginas. Gap BG4 del focus
> `nmodsreflow-builder`. Responde "cómo se modifica y se va armando el dashboard".
>
> **Alcance.** El editor + el sistema de layout. El catálogo de tipos es B218; el render interno de cada widget es
> B224 (BG6); la persistencia B217/B221.
>
> **Fuentes (primarias).** SPA beautificada (1:1 `app.4509efb4.js` sha256 `81b82b83…`):
> `scratchpad/reflow-app.beauty.js`, citada `BF:`. Barrido delegado (sonnet); tokens re-verificados por el driver.
>
> **Método / markers.** `[CERT]` = fuente primaria (`BF:` 1:1). `[INFER]` = deducción. **Thin**: las reglas CSS de
> ancho de columna (`.grid-item--*`) viven en un CSS extraído fuera del bundle JS → los px/% exactos por columna son
> `[INFER]` aquí.

---

## 223.1 — Modo edición = DOS apps montadas + un iframe de preview en vivo `[CERT]`

"Modo edición" NO es un toggle en una sola SPA — son **dos montajes separados** keyed por el flag Vuex
`user.isConfig` (`SET_IS_CONFIG`, `BF:13117,13136`, expuesto como `Vue.prototype.$isConfig`):

- **Editor shell** (`window.vueApp`, montado en `#nmods-config`): commitea `SET_IS_CONFIG` **true** (`BF:121912`).
- **Viewer/runtime shell**: lo commitea **false** (`BF:121846`).

La clave arquitectónica `[CERT]`: el editor shell hospeda un **iframe de preview en vivo** (`#contentFrame`,
`src="/nmodsreflow/"`, `BF:118240`) al que le fuerza `isConfig=true` en su `window` ANTES de que arranque su bundle,
y monta ahí una segunda instancia Vue (`ReflowPreview`). Resultado: el editor = **chrome externo envolviendo un
render REAL del dashboard dentro de un iframe**; el app externo selecciona/hoverea las tarjetas dentro del iframe vía
estado Vuex compartido (`mouseData.selectedCards`/`hoveredCards`, `BF:2176`). No es un canvas de diseño aparte: **se
edita sobre una vista viva del dashboard** `[INFER]`. En modo edición aparecen: contorno de selección + sombra de la
card (`BF:2168`), banners de página/edificio deshabilitado, y un menú contextual.

## 223.2 — Layout: grilla MASONRY (directiva vue-masonry, no cálculo propio) `[CERT]`

El `DashboardLayout` (`BF:27115`) usa la **directiva `v-masonry`** (Masonry.js, `BF:1986`) con
`item-selector:".grid-item"`, `column-width:".column-width"`, `gutter:".gutter-width"`, `percent-position:true`;
cada card lleva `v-masonry-tile`. No hay matemática de layout propia — se apoya en Masonry.js.

**Ancho → clase** (`cardClass`, `BF:27225`): `divider`→`grid-item grid-item--full`; según `width` →
`grid-item--{full|double|single}`; según `height` se sufija `quarter-height`/`half-height`/`single-height` `[CERT]`.
**Columnas responsivas** `[CERT]`: prop `maxColumns` (default 4, de `landing.maximumDashboardColumns`), renderizada
como clase `max-<n>`; editable en el editor. **Altura → pixel inline** (`BaseCard.heightStyle`, `BF:2178`): unidad
`60px`, gutter `15px`; `quarter`=60, `half`=135, `single`=255 (4×60+3×15), `double`=525 (8×60+7×15);
`exact`/`min`/`max` usan `card.heightValue` sobre la misma base `[CERT]`. Las reglas CSS de ANCHO de columna
(`.grid-item--single/double/full`) están en CSS extraído (thin, `[INFER]` los px exactos).

## 223.3 — Mover/ordenar: drag en la LISTA del sidebar, no en la grilla `[CERT]`

Reordenar NO se hace arrastrando en la grilla masonry. Se hace en un panel de LISTA del editor: `CardList`
(`BF:90657`) envuelve `<draggable>` (vuedraggable/SortableJS) sobre filas `ConfigCell` con grip de arrastre. Al
soltar, `commit()` (`BF:91221`) extrae el orden de IDs (`cards.map(c=>c.id)`), lo recommitea al array del contenedor
(`landing.cards`/`page.cards`) y emite el bus global `dashboard-redraw`. `DashboardLayout` escucha ese evento y llama
`resetGridKey()` (`$redrawVueMasonry`) para re-maquetar la masonry en el nuevo orden `[CERT]`. Es decir:
**reordenar = arrastrar en la lista → recommit del array de IDs → evento redraw → masonry re-renderiza**.

## 223.4 — Redimensionar: dropdowns Select (no handle de arrastre) `[CERT]`

El tamaño se cambia con `<Select>` en el drawer del editor, NO con un handle de resize:
- **Ancho**: `Select` con opciones `single/double/full` (`BF:92458`), handler `cardWidthChanged`.
- **Alto**: `Select` con `auto/quarter/half/single/double/exact/min/max` (`BF:92485`); al elegir `min`/`exact`/`max`
  aparece un `InputNumber` (min 100, step 10) que alimenta `card.heightValue` (usado en la fórmula de §223.2).

`vue-drag-resize` (registrado global `VueDragResize`, `BF:121730`) **NO se usa en las tarjetas del dashboard** — todos
sus usos están gateados por `t.element` en templates de overlay de **floorplan/point-map** (`BF:41426`), i.e.
posición libre de markers sobre planos de planta. Confirma la nota de B216 (vue-drag-resize = secundario, no la
grilla).

## 223.5 — Editar páginas y navegación `[CERT]`

Nueva página: mutación `pages/ADD_ITEM` desde `addPageItem()` (`BF:90513`) o desde un diálogo que además setea
`newPageId` y emite `config-navigation` para llevar el editor a la config de la página nueva (`BF:91826`).
Duplicar página clona el objeto y recommitea `ADD_ITEM`. La agrupación/borrado en el árbol de nav usa
`navigation/ADD_ITEM(_FOR_PAGE)` / `REMOVE_NAV_FOR_PAGE` (`BF:91860`). Cada acción dispara el auto-save (B217/B221).

## 223.6 — Conexiones

- **[Block 218]** §218.4 — el add-flow (newCard→drawer Select) es la contraparte de creación; §223 cubre mover/
  redimensionar/ordenar/paginar.
- **[Block 217]** §217.4 — cada mutación del editor dispara el auto-save debounced; §223.3 muestra el
  `dashboard-redraw` que re-maqueta tras un reorden.
- **[Block 216]** §216.2 — SortableJS (reorder listas) y vue-drag-resize (floorplan) confirmados en su rol real aquí.
- **Hacia BG11 (chihuahua)**: chihuahua NO tiene editor visual (frontend ES5 IIFE fijo, sin modo edición ni masonry
  ni drawer) — es la brecha central de capacidad builder; se documenta en BG11.
- **Hacia adelante**: BG6 (render interno gauge/chart — con la corrección d3), BG10 (síntesis), BG13 (modernización:
  masonry/iframe vs enfoques modernos).
