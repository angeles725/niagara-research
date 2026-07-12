# Block 222 — Reflow dashboard-builder (VII): la vista geo "3D" (Mapbox GL, en realidad 2D)

> **Qué documenta.** La capacidad geo/mapa de Reflow —lo que el usuario percibe como "3D"— con veredicto
> definitivo sobre si es 3D real: la card `building-map` (Mapbox GL), la card `weather-map`, el token/estilos, y
> los datos de marker. Gap BG9 del focus `nmodsreflow-builder`. Responde de lleno la parte "diseños 3D" del pedido.
>
> **Alcance.** Las dos superficies de mapa (building-map interactivo + weather-map). NO cubre el módulo Vuex
> `buildings` completo ni el editor de ubicaciones en detalle.
>
> **Fuentes (primarias).**
> - SPA beautificada (1:1 `app.4509efb4.js` sha256 `81b82b83…`): `scratchpad/reflow-app.beauty.js`, citada `BF:`.
> - Java RT: `com/niagaramods/nmodsreflow/http/responses/WeatherMapResponse.java`.
> - Barrido delegado (sonnet); tokens re-verificados por el driver.
>
> **Método / markers.** `[CERT]` = fuente primaria (`BF:` 1:1 · Java `file:line`). `[INFER]` = deducción.

---

## 222.1 — Veredicto: NO es 3D. Es un mapa Mapbox GL 2D (top-down) `[CERT]`

Confirmación tajante (grep sobre el código PROPIO de Reflow, `app.js`): **0 hits** de `setPitch` / `pitch:` /
`fill-extrusion` / `setBearing` / `bearing:` `[CERT]` (grep negativo). Las únicas primitivas de cámara que Reflow
invoca son:
- `fitBounds(bounds, opts)` en la building-map (`BF:15840`, `zoomToBuilding` `BF:15758`, `resetZoom` `BF:15840`),
- `flyTo({center, duration, minZoom})` sólo en el modal de ubicación (sin keys `pitch`/`bearing`),
- `zoom`/`center`/`bounds` como props two-way.

Es decir: Reflow maneja Mapbox estrictamente con **centro + zoom + ajuste de límites**, cámara plana a pitch 0. Las
primitivas 3D de Mapbox (`fill-extrusion`, `pitch`) existen SÓLO dentro de la librería vendor (`chunk-vendors.js`),
nunca invocadas por el código de Reflow (confirma y ajusta B216 §216.3). **La "vista 3D" del producto es un mapa
geoespacial WebGL 2D con markers de edificios** `[INFER]` (síntesis del veredicto).

## 222.2 — La card `building-map`: render y markers `[CERT]`

Árbol de componentes (`BF:2588-2720`): `BuildingMapCard` → `Map` → `MglMap` (wrapper vue-mapbox, `BF:2588`) que
contiene `MglAttributionControl`, un `MglRasterLayer` opcional (overlay de clima, §222.4) y un `v-for` sobre
`visibleMarkers` → `MglMarker` (`BF:2622`) con `IconMarker` + `MglPopup` (`BuildingCard`/`GroupCard`).

**Los markers salen del módulo Vuex `buildings`** `[CERT]`: `mappedBuildings` (`BF:15678`) filtra edificios con
`lat`/`lon` presentes; `buildVisibleMarkers()` (`BF:15808`) mapea a `[parseFloat(lon), parseFloat(lat)]` (`BF:15823`)
— pares planos `[lon,lat]`, **sin elevación/z**. Por eso la card real tenía `config` vacío (B218 §218.5): el estado
del mapa vive en `buildings`, no en `card.config`.

**Interactividad**: en la card de DISPLAY los markers NO son arrastrables (solo click→popup/nav, `BF:15915`). El
arrastrar-para-colocar ocurre en un componente SEPARADO, el **modal de ubicación** (`ModalPicker`, `BF:75657`),
donde `MglMarker` sí es `draggable` con handler `update:coordinates` (`BF:75708`) + búsqueda por dirección
(`MglGeocoderControl`, §222.5).

## 222.3 — Token Mapbox y estilos (configurable, todos planos) `[CERT]`

Token hardcodeado en el plugin `$maps` (`BF:118864`, `token:"pk.eyJ…"`), instalado como `Vue.prototype.$maps`
(`BF:118886`). Es UN token compartido por todas las instalaciones de Reflow (cuenta Mapbox de NiagaraModules), no
por-tenant `[CERT]`/`[INFER]`. Los 6 estilos (`BF:118865`) son todos stock planos de Mapbox:
`dark-v10, satellite-v9, satellite-streets-v11, streets-v11, light-v10` (+ fallback `light-v10`).

**Configurable por el admin** `[CERT]`: panel "Map Style" `<Select>` (`BF:106477`) escribe `buildings.mapStyle` vía
`SET_MAP_STYLE`; la card lo consume en `mapActiveStyle`→`$maps.style(...)`. Incluso "Satellite" es un basemap
raster/vector plano a pitch 0 — **no hay opción de extrusión 3D** ni aunque el usuario cambie el estilo.

## 222.4 — La card `weather-map`: dos superficies de clima + gate de licencia `[CERT]`

Hay DOS superficies de clima distintas:

1. **Card `weather-map`** = imagen PNG estática, **sin Mapbox** `[CERT]`. `defaultConfig` (`BF:26882`,
   `lat/lon/zoom, type:"radar", style:"dark", roads/interstates/counties`) + `aerisMapUrl` (`BF:9987`) construye una
   URL directa cliente a `https://weather.niagaramodules.com/maps` (`BF:9845`): un PNG 300×300 de radar
   (`…/current.png?host=<hostId>`). Se refetchea por evento.
2. **Overlay de clima en building-map** = `MglRasterLayer` (`BF:2610`, gateado por `t.weather`) con tiles
   `…/radar/{z}/{x}/{y}/current.png?host=<hostId>` (`weatherMapUrl`, `BF:9998`) — radar en vivo teselado sobre el
   mapa. Pipeline distinto de (1).

**Servidor** `WeatherMapResponse.java`: construye la misma URL upstream + `?host=getHostId()`, cachea a
`^reflow/weather.png` con ventana de 1h (`diff < 3600000L`). El cliente NO lo llama por ninguna ruta visible en
`app.js` (0 hits) → es un camino server-side `[INFER]` para render no-browser (export/PDF).

**Gate de licencia** `[CERT]`: `license.limits.maps` (booleano) gatea SÓLO `weather-map` (`BF:92635`; estado
deshabilitado renderiza "Weather maps are not enabled in your license", `BF:92703`). El `building-map` NO tiene ese
gate (grep confirma sin condicional de licencia). Dependencia externa notable: ambas superficies de clima dependen
del **cloud de NiagaraModules** (`weather.niagaramodules.com`) con el `hostId` de la station como parámetro.

## 222.5 — Datos del marker y colocación de edificios `[CERT]`

Un marker de edificio/grupo lleva (estructura, sin valores del cliente): `id, name, type` (building|group),
`lat, lon` (strings), flags de visibilidad (`markerEnabled, markerVisible, markerZoomTo, hideChildMarkers, zlevel`),
y un `markerStyle` resuelto `{color, statusColor, icon, image, imageSize, hideShadow…}` que hereda del grupo padre
si no está seteado (`BF:6966-7008`). El color puede ser **dinámico por estado**: un punto `status:{ord}` suscrito en
vivo tiñe el marker (down/fault/stale) cuando `statusType==="status"` (`BF:15911`). También lleva `alarms` para el
badge de conteo. **Colocar un edificio** = modal de ubicación: arrastrar el pin, tipear lat/lon, o buscar dirección
(el geocoder re-centra la cámara pero el usuario debe click/drag/tipear para fijar el pin, `BF:75983`) `[CERT]`.

## 222.6 — Conexiones

- **[Block 216]** §216.3 — adelantó "3D=Mapbox 2D"; §222.1 lo cierra con el grep negativo de cámara sobre el
  código propio de Reflow.
- **[Block 218]** §218.1/218.5 — `building-map`/`weather-map` en el catálogo; `weather-map` gateado por licencia.
- **[Block 155]** — el token Mapbox público ya notado; §222.3 agrega los estilos y que es una cuenta compartida.
- **Hacia BG11 (chihuahua)**: chihuahua NO tiene vista de mapa geo (es un dashboard MX60 de plantas); portar la
  capacidad "building-map" implicaría mapbox-gl + un módulo de ubicaciones — se evalúa en BG11.
- **Hacia adelante**: BG4 (editor/layout), BG6 (gauges/charts render), BG10 (síntesis), BG13 (modernización: mapbox
  vs alternativas), BG11 (chihuahua).
