# nmodsreflow-builder (NiagaraMods Reflow v1.7.7 — capacidad de CONSTRUCTOR de dashboards) — Research State

> Focus: **Reflow como dashboard-builder de producto** — cómo un usuario final CREA, EDITA y ACTUALIZA
> un dashboard en el navegador y le va agregando contenido (gráficos de equipos, fotos propias, iconos,
> gauges/widgets, la vista geo Mapbox), TODO empaquetado dentro del propio módulo. Es el ángulo de
> PRODUCTO/CAPACIDAD, complementario y distinto de los focus cerrados de SEGURIDAD/arquitectura:
> `nmodsreflow` (backend `-rt`, B138-B150) y `nmodsreflow-ux` (cliente `-ux`, B151-B155).
> READ-ONLY. Corpus language: Spanish (technical EN), por continuidad del corpus niagara.
>
> Source roots (primarios, JAR embarcado build .75, decompile Vineflower):
> `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/`
>   - Java: `com/niagaramods/nmodsreflow/` (79 clases; esp. `http/responses/*`, `sync/*`).
>   - Assets embebidos: `image-library/` (arte HVAC), `sound-library/`, `icons/points/`,
>     `rc/icon-categories.json`, `rc/icon-search.json`, `rc/point-matrix.json`, `rc/background-default.jpg`.
> `SPA/` = `RT/rc/js/app.4509efb4.js` (2.63 MB, **minificada 1 línea** → beautify js-beautify a scratchpad para
>   rigor `file:line`) + `chunk-vendors.3fecdb47.js` (Vue 2.6.14 + vue-router + Vuex + SortableJS + vue-drag-resize
>   + mapbox-gl + FontAwesome; **sin d3, sin axios** — grep-refutado).
> `UX/` = `nmodsreflow77-ux/vineflower/` (registro de vistas fino, ya cubierto en B151-B155).
> Tools: `decompile-java.sh` (ya aplicado) + lectura directa + grep + js-beautify (SPA) + CodeGraph.
> Mirrored in engram (project `niagara-research`): `research/niagara/nmodsreflow-builder/{gaps,progress}`.

## Why this focus exists

Reflow es, en el mercado Niagara, ante todo un **constructor visual de dashboards**: el operador arma páginas
de equipos, arrastra/configura tarjetas, incrusta arte de equipos, iconos y una vista de mapa, y publica; el
cambio se propaga en vivo. Los focus previos mapearon el `-rt` y el `-ux` con rigor `file:line` pero desde el
ángulo de SEGURIDAD (config-write sin auth, traversal, doPrivileged). La CAPACIDAD DE PRODUCTO —el editor, el
modelo de dashboard, las bibliotecas de assets, el catálogo de widgets, la vía de actualización en vivo— quedó
sin reconstruir. Este focus la reconstruye, para (a) responder la pregunta del usuario "qué hace Reflow y cómo",
y (b) habilitar el bloque de diseño POSTERIOR: cómo portar esa capacidad al módulo propio `chihuahua`.

## Angle (declarado 2026-07-12)

**"Reflow como dashboard-builder"**: stack/librerías (+función de cada una) → modelo de dashboard editable y su
persistencia → motor de update en vivo (JSON Patch) → editor visual y layout → catálogo de widgets → render de
gauges/charts → bibliotecas de assets embebidas → assets propios del usuario (listado + upload de fotos) → vista
geo Mapbox ("3D") → síntesis de producto → **diseño de portabilidad a chihuahua** (Parte B, bloque applied).

## Coverage

- **Métrica**: 2 / 11 gaps cerrados (0.18).
- **Bloques del focus**: B216 (BG1 stack & librerías), B217 (BG2 modelo dashboard + persistencia + update en vivo, **validado [CERT-live]**).
- **Last iteration**: 2026-07-12 — BG1 cerrado (B216, stack & librerías): RT Java = jackson (JSON, 30 clases) +
  flipkart-zjsonpatch (motor JSON-Patch RFC-6902, el "editá-y-se-actualiza") + opencsv (CSV export) +
  apache-commons-io TeeOutputStream (cache-and-serve) + com.tridium.json (JSON de assets). SPA = Vue 2.6.14 +
  Vuex 3.5.1 + vue-router 3.4.5 + SortableJS 1.10.2 (reorder listas) + vue-drag-resize (posición libre
  secundaria) + mapbox-gl (mapa 2D WebGL="3D") + FontAwesome (814 refs). **Ausencias verificadas**: d3=0,
  axios=0, three/babylon=0. Servidor DELGADO respecto del dashboard (persiste/sirve/parchea blob opaco; la
  composición vive en el cliente).

## Gap-backlog (priorizado)

| Prioridad | Gap | Tipo/fuente | Estado |
|---|---|---|---|
| — | BG1 · **Stack & librerías + función de cada una**: RT (jackson, flipkart-zjsonpatch, opencsv, apache-commons-io, com.tridium.json) + JS (Vue 2.6.14, vue-router, Vuex, SortableJS 1.10.2, vue-drag-resize, mapbox-gl, FontAwesome; ausencia de d3/axios) | Java `-rt` + SPA (banners/imports) | **cerrado B216** |
| — | BG2 · **Modelo de dashboard editable + persistencia**: dashboard = array de `cards {id,type,config,width}`, blob opaco a Java (`^reflow/config.json`), Vuex `dashboardCards`; save full (`ConfigUpdateResponse`) + push live WS `config-reload`; delta multiusuario (JSON-Patch, merge en caliente) | Java `sync/`+`http/responses/` + SPA | **cerrado B217 (+[CERT-live])** |
| alta | BG3 · **Motor de update en vivo (JSON Patch)**: `flipkart-zjsonpatch` RFC-6902, `ConfigDeltaResponse`→`BReflowSyncService.apply()`; merge multiusuario "editá y se actualiza" | Java `-rt` | pending |
| alta | BG4 · **Editor visual / edit mode + layout**: `editMode`, edición inline por card (`cardTypeChanged`/`cardWidthChanged`/`config-menu`), masonry + enum `single/double/full`, `vue-drag-resize` secundario, SortableJS reorder; NO hay paleta drag estilo Canva | SPA (beautify) | pending |
| alta | BG5 · **Catálogo de widgets/cards**: los ~19-21 tipos (alarm, building-map, gage, equipment-list, point-display, historyChart, weather-*, table, toggle, circle, hyperlink, schedule-list, divider…) + switch `type→component` + `card.config` schema por tipo | SPA (beautify) | pending |
| media | BG6 · **Render de gauges/charts**: gauge = SVG custom (`circleStyle`, `stroke-dasharray`/`linecap`); `HistoryChart` lib perdida a minificación → beautify dirigido del chunk | SPA (beautify) · parcial thin | pending |
| alta | BG7 · **Bibliotecas de assets embebidas en el módulo**: `image-library` (25 JPG HVAC, 8 cat, `ImageLibraryResponse`, `module://`), `sound-library` (11 MP3), FontAwesome icon-picker (`icon-search.json` 1853 + `icon-categories.json` 75), `point-matrix.json` (109 puntos + regex auto-bind), point badges (5), backgrounds — cómo se sirven y referencian desde un widget | Java `-rt` + assets | pending |
| media | BG8 · **Assets propios del usuario: listado + upload de fotos**: `ImageListResponse` escanea `BFileSystem` desde `^`; `EquipmentNoteUpdateResponse` byte-passthrough a `^reflow/notes/`; NO hay endpoint multipart → mecanismo static, payload base64-in-JSON = thin/needs-live | Java `-rt` + SPA | pending (thin en payload) |
| media | BG9 · **Vista geo "3D" Mapbox**: `Mgl*` wrappers, card `building-map`, markers 2D, sin pitch/extrusion en código Reflow (3D latente no usado); `WeatherMapResponse` radar PNG desde cloud niagaramodules; `MglRasterLayer` | SPA + Java `-rt` · nuance needs-live | pending |
| media | BG10 · **SÍNTESIS Parte A**: "cómo Reflow construye un dashboard editable end-to-end" — flujo de producto completo, cross-ref BG1-BG9 | síntesis (design) | pending |
| design | BG11 · **Parte B — portabilidad a chihuahua**: cómo añadir la capacidad de builder a `chihuahua` (ES5 IIFE `window.MX60`, sin Vue, con RBAC write-gate); brechas, opciones, plan | applied/design (READ-ONLY sobre chihuahua) | pending |

## Blocked / thin-source gaps (con lo que necesitan)

- **BG8 (payload)** — el mecanismo de note-update es static-legible, pero la afirmación "las fotos viajan base64
  dentro del note JSON" es `[INFER]` hasta ver un payload real → **block-on-thin-source** para esa sub-afirmación
  (o confirmar en fase dinámica/live-network). El mecanismo (byte-passthrough, sin multipart) SÍ es static-CERT.
- **BG6 (HistoryChart lib)** — el nombre de la lib de charting está perdido a la minificación; requiere beautify
  dirigido del chunk o inspección de red en vivo. El gauge SVG custom SÍ es static-CERT.
- **BG9 (3D runtime)** — la ausencia de pitch/extrusion en el código de Reflow es evidencia static fuerte, pero un
  "nunca 3D" tajante necesitaría confirmación live (un style Mapbox inyectado por config podría habilitarlo).

## Stop control (primario = read-only-investigable, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 9 (BG3-BG11; BG6/BG8/BG9 con componentes thin acotados, el resto full-static).
- **Open gaps — requires-execution**: 0.
- **Fase dinámica ABIERTA (§12)**: station N4 VIVA disponible (localhost, usuario `API`/HTTPBasicScheme + `API2`/DIGEST;
  Reflow **1.7.5-43**) + station de disco `HoneywellMX605132026` (Reflow completo, dashboard medianamente armado).
  Habilita validación `[CERT-live]` y un experimento de ESCRITURA supervisado (backup `bf70f28f…` listo).
- **STOP**: NO declarado — 9 gaps investigables (BG3 siguiente) + fase dinámica en curso.
- Budget cap: none.

## Iteration history

| # | Fecha | Gap cerrado | Bloque | delegado? · modelo | Nuevos gaps |
|---|---|---|---|---|---|
| (bootstrap) | 2026-07-12 | — | — | sí · audit sweep matriz (sonnet) | 11 gaps derivados de la matriz de 24 subsistemas |
| 1 | 2026-07-12 | BG1 stack & librerías | B216 | no · inline (sobre matriz) | 0 (inventario; alimenta BG2/BG3/BG6/BG7/BG9) |
| 2 | 2026-07-12 | BG2 modelo dashboard + persistencia | B217 | sí · sweep SPA (sonnet) + validación live (no·inline) | 0 (validado [CERT-live] contra station viva; abre experimento de escritura dinámico) |

## Self-verify

- **B216**: tokens load-bearing grep-confirmados en fuente primaria — imports Java (jackson 30 clases ·
  zjsonpatch `BReflowSyncService.java:7`+`:420` · opencsv `BReflowCSVCommands` · TeeOutputStream
  `ConfigIO.java:15,121`/`HistoryGroupsResponse:20,65`/`HistoryListResponse:20,66` · com.tridium.json
  `ImageLibraryResponse:3-4`/`ImageListResponse:3-4`) + banners JS (`Vue.js v2.6.14` · `vuex v3.5.1` ·
  `vue-router v3.4.5` · `Sortable 1.10.2` · `vue-drag-resize` ×5 · 814 `fa-` refs) + ausencias (d3=0, axios=0,
  three/babylon/WebGLRenderer=0). verify-block exit 0. `[CERT]` 8 (adj) · `[INFER]` 5 (adj). Ratio ≈ 0.62 —
  bloque de INVENTARIO-con-roles (cada lib = 1 [CERT] presencia + 1 [INFER] rol deducido); no señala
  agotamiento. Identidad SPA anclada por sha256 (`81b82b83…` = B153).
- **B217**: tokens load-bearing grep-confirmados en beautified temp (`dashboardCards` BF:13952 · cards/ADD_CARD
  BF:8378 · Na()/removeUndefined BF:13938 · saveState/config_update BF:14144 · saveDelta/config_delta/sendFullState
  BF:14184 · STATE_DELTA/delta-sync BF:14029 · "Reload Required"/requiresReload BF:118191 · enum
  single/double/full/quarter/half BF:92460) + Java (`ConfigUpdateResponse.java` config.json:33/Config-Timestamp:91/
  config-reload:96/broadcast:101 · `BReflowSyncService.java` JsonPatch.apply:420/delta:438-446). **Validación
  [CERT-live]** (station viva, GET config 200): forma `cards[]`={id,type,enabled,config} + config alarm=
  {display,displayType,title} + `landing.cards==pool ids` (match=true) + 18 keys top-level → todos CONFIRMED.
  verify-block exit 0. `[CERT-live]` 3 · `[CERT]` 15 · `[INFER]` 5. Ratio 0.28 (evidencia, sano). Divergencia de
  versión notada (station 1.7.5-43 vs corpus static 1.7.7.75; schema config v14 común). Probe sanitizado en
  `sources/probes/B217-live-config-structure-20260712.txt` (estructura, cero datos del cliente).
