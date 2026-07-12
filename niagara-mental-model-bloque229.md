# Block 229 — Reflow dashboard-builder (XIII): floorplans (planos con overlays de puntos en vivo)

> **Qué documenta.** El subsistema de **floorplans** de Reflow: cómo el usuario "diseña" un plano de planta —una
> imagen subida con overlays de puntos, iconos, zonas y flechas— y cómo esos overlays muestran valores en vivo.
> Gap BG15 (reapertura grupo A). Es la capacidad de "diseño" más cercana a lo que el usuario preguntó al inicio.
>
> **Alcance.** El modelo floor/element, el render, la edición drag-place, el binding en vivo y la navegación/zoom.
> El auto-binding de los puntos que se colocan es B228; el mecanismo de imágenes es B219.
>
> **Fuentes (primarias).** SPA beautificada (1:1 `app.4509efb4.js` sha256 `81b82b83…`):
> `scratchpad/reflow-app.beauty.js`, citada `BF:`. Barrido delegado (sonnet); tokens re-verificados por el driver.
>
> **Método / markers.** `[CERT]` = fuente primaria (`BF:` 1:1). `[INFER]` = deducción.

---

## 229.1 — Modelo de datos: floorplans → floors → elements `[CERT]`

Módulo Vuex `floorplans` (`BF:7408`): `{enabled, rememberZoom, hideNavigationArrows, hidePhotoOverlay,
photoTextColor, floors:[]}`. Cada **floor** (factory `BF:7382`):
`{id, name, enabled, image, thumbnail, imageWidth, imageHeight, maintainAspectRatio, useImageSize, canvasWidth/Height,
hasCanvas, backgroundColor, elements:[], states:[{name:"Base", elements:{}}], type:"reflow"|"niagara", pxView}`.

Dos tipos de floor `[CERT]`: **`reflow`** (canvas nativo con overlays propios) o **`niagara`** (embebe una vista PX
— los valores legacy `"px"` se migran a `"niagara"`, `BF:3310`). El campo **`states`** son estados visuales
alternos (p. ej. día/noche, ocupado/desocupado) con overrides de estilo por-elemento keyed por id.

## 229.2 — Los 8 tipos de elemento (overlay) `[CERT]`

Factories por tipo en el módulo `floorEditor` (`BF:12402-12622`): **`icon, button, group, label, image, text, zone,
arrow`** `[CERT]`. Todos los rectangulares llevan `x, y, width, height` en pixel-space del plano (relativo a
`imageWidth/imageHeight`, escalado por `scaleFactor`):

| Tipo | Qué es | Campos clave |
|---|---|---|
| `label` | **overlay de punto EN VIVO** | `value` (ORD del punto), `device`, `pointId`, `statusIndicator`, `title/line1/line2` |
| `icon` | icono FontAwesome + hyperlink | `icon:{name,style}`, `hyperlink`, `action`, `color` |
| `button` | botón con acción/deep-link | `title`, `hyperlink`, `action`, `backgroundColor` |
| `image` | imagen sobrepuesta | `image` (ORD), `repeat/size/position`, `tintOpacity` |
| `text` | texto estático (o device) | `text`, `style`, `size/weight/align` |
| `zone` | **polígono SVG** (resaltar área) | `points:[]`, `color`, `fillOpacity/strokeWidth` |
| `arrow` | **flecha SVG** (anotación) | `pointStart/pointEnd`, `headLength/headWidth` |
| `group` | contenedor de otros elementos | `name`, links por `group` |

## 229.3 — Render: imagen + overlays por vue-drag-resize `[CERT]`

`FloorPlanCanvas` resuelve la imagen del plano IGUAL que toda imagen de Reflow: `$ord.image(floor.image)` (`BF:44106`,
`floor.image` = ORD `file:^Imagenes/…`, cross-ref B219). Itera `elementsReversed` y despacha por `element.type` a un
componente Vue dedicado (`ElementLabel/ElementImage/ElementText/ElementButton/ElementIcon/ElementSVGPolygon/
ElementSVGArrow`). Cada rectangular se envuelve en un **`vue-drag-resize`** (`BF:41416`) atado a `x/y/width/height`
del elemento y escalado por `parent-scale` (el zoom) — este es el uso de `vue-drag-resize` que B223 §223.4 ubicó
"solo en floorplans". Zonas y flechas se dibujan como SVG (polígono/línea), no drag-resize.

## 229.4 — Edición: agregar, mover, redimensionar, auto-poblar `[CERT]`

- **Gate de edición**: `vue-drag-resize` recibe `is-draggable/is-resizable = !isLocked` y `prevent-active-behavior
  = !editable` — el viewer no-config obtiene un overlay estático; solo en modo edición hay handles.
- **Agregar**: toolbar dispatcha `floorEditor/addElementIcon|Button|Group|Label|Text|Image|SVGPolygon|SVGArrow`
  (`BF:74220`) → `addElement` (`BF:12402`) posiciona en el centro del plano y auto-selecciona.
- **Mover/redimensionar**: `vue-drag-resize` emite preview transitorio → `dragstop`/`resizestop` →
  `floorEditor/commitDrag`/`commitResize` (`BF:12292`) commitea `x/y` final (o `pointStart/pointEnd` para flechas,
  `points` para zonas) vía `MUTATE_ELEMENT_INDEX`; los `locked` se saltan.
- **Auto-poblar puntos** (`addEquipmentLabels`, `BF:12996`): recorre cada equipo del floor, resuelve un "featured
  point" (del mapeo de B228) y crea un `label` por device con `value:pointOrd`, auto-ordenados en grilla — el usuario
  luego los arrastra al lugar correcto del plano. Es el puente entre el auto-binding (B228) y el diseño del plano.

## 229.5 — El overlay en vivo: `BoundLabel` con subscripción baja `[CERT]`

El `label` (el overlay de dato) usa `BoundLabel` (`BF:36067`), que se suscribe al punto por **BajaScript nativo**:
`$baja.Ord.make(value).get({subscriber})` + `Subscriber.attach("changed", …)` sobre el slot `"out"` (`BF:36236`) —
**subscripción en vivo real, no polling** `[CERT]`. Renderiza `title/line1/line2`, el `value` vivo, y un
**indicador de estado** (dot o icono FontAwesome) coloreado desde los flags `BStatus`
(`isDown/isFault/isStale/isDisabled/isAlarm/isOverridden/isOk`, `BF:36172`). Los `icon/button/image` son overlays
estáticos/hyperlink (deep-link a otra página Reflow o URL externa). `zone`/`arrow` son anotaciones (color/opacidad);
`zone.value` existe pero su uso no se confirmó (`[INFER]` thin, posiblemente legacy).

## 229.6 — Navegación y zoom `[CERT]`

- **Multi-floor**: lista `FloorsFull`/`FloorsCompact`; el main render usa `FloorPlan` (type `reflow`) o `FloorPlanPx`
  (type `niagara`, embebe un archivo PX — cross-ref subsistema PX B194) keyed por `currentFloor.id`.
- **Flechas prev/next** (`previousFloor`/`nextFloor`), ocultas si `hideNavigationArrows` (`BF:36995`).
- **Zoom persistente** (`rememberZoom`): guarda `{zoom, x, y, timeStamp}` por floor en
  `localStorage["floorPlanZoomState"]`, expira a 24h (`864e5` ms, `BF:44801`); se saltea en viewport chico/mobile.
- `hidePhotoOverlay`/`photoTextColor` controlan el tint y color de texto de los **thumbnails** de selección de floor,
  no el canvas principal.

## 229.7 — Conexiones

- **[Block 228]** — `addEquipmentLabels` coloca en el plano los puntos ya auto-mapeados; §229 es el "dónde se ven".
- **[Block 219]** §219.4 — `floor.image` (`file:^Imagenes/…`) se resuelve por el mismo `$ord.image()`.
- **[Block 223]** §223.4 — confirma el uso de `vue-drag-resize` (posición libre) exclusivo de floorplans.
- **[Block 194]** (px-editor) — el floor `type:"niagara"` embebe una vista PX; nexo con el subsistema PX.
- **Hacia chihuahua (B226)**: chihuahua tiene su HomeMap con zonas SVG hardcoded (`ZONES_JSON`) — el floorplan
  editable de Reflow es exactamente la capacidad que a chihuahua le falta (overlays arrastrables sobre imagen).
- **Cierre grupo A**: BG14 (auto-binding) + BG15 (floorplans) completos. Siguen B (ciclo de vida), C (dinámico), D
  (módulos + vistas).
