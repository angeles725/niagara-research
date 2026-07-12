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
<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 233
gaps_closed: 0
known_gaps: 0
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->


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

- **Métrica**: base **12 / 12 CERRADO** (BG1-BG11 + BG13). **REABIERTO 2026-07-12** (autorizado por usuario) con
  16 gaps nuevos en 5 grupos A→B→C→D→E (budget adicional, §8 reopen): **A** auto-binding + floorplans, **B** ciclo de
  vida (licensing-producto/backups/migración/diff-versiones), **C** dinámico (experimento escritura + editor browser),
  **D** (pedido usuario 2026-07-12) módulos que usa + sistema de vistas Workbench/navegador. Métrica reapertura: **12 / 16 — A+B+D + BG26/27/28/29 (B228-B239). Grupo E en curso (BG29✓; BG24/25 pendientes). Grupo C PENDIENTE de OK del usuario.**
- **Bloques del focus**: B216 (BG1 stack), B217 (BG2 modelo **[CERT-live]**), B218 (BG5 catálogo), B219 (BG7 assets), B220 (BG8 upload), B221 (BG3 motor+control), B222 (BG9 Mapbox=2D), B223 (BG4 editor+masonry), B224 (BG6 render gauge/chart + §14 corrige B216/B218).
- **Correcciones §14** (B224): B216 §216.4 (d3 NO ausente, aliaseado) + B218 §218.3 (circle=iView wrapper, no SVG custom). Notas insertadas en ambos origen.
- **Reordenamiento**: BG5 se adelantó a BG3/BG4 al aparecer el dashboard real de disco `HoneywellMX605132026` (26 cards, 10 tipos) — evidencia primaria fuerte para el catálogo. BG3 (motor JSON-Patch) y BG4 (editor/layout) siguen pendientes.
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
| — | BG3 · **Motor de update en vivo (JSON Patch)**: `flipkart-zjsonpatch` RFC-6902 apply bajo doPrivileged, rollback de timestamp, broadcast `delta`, control multiusuario cooperativo (configControl token, grant/revoke/request), persistencia debounced | Java `-rt` | **cerrado B221** |
| — | BG4 · **Editor visual + layout**: edit-mode = 2 mounts (isConfig) + iframe live-preview; layout = directiva `v-masonry` (Masonry.js) + cardClass width/height→clases + heightStyle px (60/15); reorder = drag en lista sidebar (vuedraggable) → recommit ids → `dashboard-redraw`; resize = Selects (no handle); vue-drag-resize solo floorplan; pages ADD_ITEM | SPA (beautify) | **cerrado B223** |
| — | BG5 · **Catálogo de widgets/cards**: 20 tipos ofrecidos (registro `cardTypes`) + alias legacy `gauge`; switch `type→component` (v-if); schema/defaults por tipo; add-flow = dropdown en drawer (no paleta drag); especiales hx/url=iframe, building-map=Mapbox | SPA (beautify) + config real disco | **cerrado B218** |
| — | BG6 · **Render gauges/charts**: `Gauge`(gage/gauge)=SVG bespoke (dashArray sobre path fijo); `circle`=wrapper iView `<Circle>`; `historyChart`/sparkline=**D3.js** (`<d3chart>`, d3-selection aliaseado); chartTypes area/line/bar/heatmap/scatter. §14 corrige B216(d3)/B218(circle) | SPA (beautify) | **cerrado B224** |
| — | BG7 · **Bibliotecas de assets embebidas**: `image-library` (25 JPG HVAC, nav-RPC no REST), FontAwesome icon-picker (1853), `point-matrix.json` (109, auto-bind), `sound-library` (11 MP3); **mecanismo ORD→URL** `$ord.image()`: `module://`→`/module/`, `file:^`→`/ord/` (servlets nativos Niagara, no el custom) | Java `-rt` + assets + SPA | **cerrado B219** |
| — | BG8 · **Assets propios del usuario (upload)**: **veredicto: NO hay upload in-app** (doPost 4 rutas, 0 multipart; bundle 0 FileReader/FormData); fotos llegan out-of-band al file space (Workbench), Reflow las referencia `file:^Imagenes/…`; picker = nav-RPC `station:\|file:^`; formatos jpg/jpeg/png/svg/gif | Java `-rt` + SPA + disco | **cerrado B220** |
| — | BG9 · **Vista geo "3D" Mapbox = 2D**: veredicto tajante (0 pitch/bearing/fill-extrusion en código Reflow; solo center/zoom/fitBounds); building-map markers `[lon,lat]` desde módulo buildings; 6 estilos planos configurables; weather-map=2 superficies (PNG estática + raster tiles) gate `license.limits.maps`; cloud niagaramodules + hostId | SPA + Java `-rt` | **cerrado B222** |
| — | BG10 · **SÍNTESIS Parte A**: flujo end-to-end (crear→editar→persistir→propagar→enriquecer) + stack por capa + 6 hilos transversales (server delgado, apoyo en plataforma, doc+patches, editar-sobre-vista-viva, todo gateado, lección §14) | síntesis (design) | **cerrado B225** |
| — | BG11 · **Parte B — chihuahua como builder + portabilidad**: documentado con las mismas dimensiones; **veredicto: chihuahua NO tiene capacidad builder** (dashboard FIJO, layout hardcoded en constantes Java, "no drag-drop" deliberado); tabla comparativa 11 dimensiones; **chihuahua tiene 3D real (Three.js) que Reflow no**; lidera en RBAC/audit; plan de portabilidad 5 piezas ranked | design (chihuahua fuente propia) | **cerrado B226** |
| — | BG13 · **Modernización del stack** (pedido usuario): diagnóstico (Vue2 EOL, Vuex→Pinia, iView discontinuado, Masonry→CSS Grid, mapbox→MapLibre, +Vite/TS); tabla de-qué-a-qué; mejoras arquitectónicas (tipar config, upload real, editor sin iframe, RBAC); qué mantener (doc+JSON-Patch, d3, delegar a plataforma) | design/análisis (B216 + web) | **cerrado B227** |

## Reapertura 2026-07-12 — backlog A→B→C (autorizado por usuario)

| Grupo | Gap | Tipo/fuente | Estado |
|---|---|---|---|
| **A** | BG14 · **Auto-binding de puntos**: tabla point-matrix (109 puntos, 10 tipos, regex); template filtra n/a + prioridad→5 grupos; matcher `ua` (regex/name/hash-#); flujo `MAP_DEVICE` (LOAD_NIAGARA_POINTS→match→assign ord, 3 modos); override per-punto + lockFromRemap; persist compacto `{pointId:ord}` | SPA + point-matrix.json | **cerrado B228** |
| **A** | BG15 · **Floorplans**: modelo floorplans/floor/element (8 tipos: label/icon/button/image/text/zone/arrow/group), floor type reflow|niagara(PX), render + vue-drag-resize, edición add/move/resize + auto-poblar (`addEquipmentLabels`), overlay live `BoundLabel` (baja subscription + BStatus), zoom localStorage 24h | SPA (beautify) | **cerrado B229** |
| B | BG16 · **Licensing como producto**: 5 límites (buildings/floors/pages/equipment=conteos con canAddComponent hard-block + banner; maps=toggle weather-map); tiers trial(caps 1/3/3/10/false)/officeDemo(9999)/pago(jace/jacepro/supervisor/site/enterprise); activación AUTO host-ID (LicenseClient GET api.niagaramodules.com, timer 24h, sin key manual); límites del license file (Feature attr, none=9999, buildings+add-ons) | SPA + Java `-rt` | **cerrado B232** |
| B | BG17 · **Versionado/backups del dashboard**: 4 tipos (Daily overwrite / Incremental 1h+skip-if-default / Pre-remap antes de auto-binding / Manual); BackupManager create/apply(restore)/rename/destroy en threads; 6 Backup*Response; 13 pre-remap reales en disco | Java `-rt` + disco | **cerrado B231** |
| B | BG18 · **Migración de config**: schema La=14; migrateState en load() si version<14; backup Pre-migration ANTES; loop 13 transforms versionados (v2→14, puros); handshake Client-Migration salta backup incremental; save no-op durante migrationActive | SPA | **cerrado B233** |
| B | BG19 · **Evolución de producto 1.5→1.7**: v1.7.5 estaba unlicensed (diff licensing=restauración); cambios reales = history (threading privilegiado + HistoryGhostSubscriber cross-station fix), AlarmQuery GET→POST (breaking), demo deep-link; **builder/modelo dashboard ESTABLE (schema v14)**; UX 0 cambios | DIFF preservado + bloques | **cerrado B230** |
| C | BG20 · **Experimento de escritura (dinámico §12)**: crear un dashboard más completo en la station viva, observar JSON-Patch en vivo (backup `bf70f28f…` listo) | station viva (write supervisado) | queued (requiere OK write) |
| C | BG21 · **Editor en el browser (dinámico §12)**: ver/interactuar el modo edición real con chrome-devtools (requiere Chrome con debug port) | station viva (browser) | queued (requiere Chrome) |
| D | BG22 · **Módulos que usa nmodsReflow**: -rt 12 deps Tridium (baja/web/history/alarm/control/schedule/bql/bacnet/driver/net/box/platform) + para qué cada uno; -ux +nmodsreflow-rt; tipos (service/scheme reflow:/8 commands/channel/ws/sync + 3 views); Reflow=agregador transversal | module.xml + bloques | **cerrado B234** |
| D | BG23 · **Sistema de vistas**: 3 vistas BSingleton+BIJavaScript+**BIFormFactorMax** (@AgentOn ReflowService); OrdServlet rutea form-factor-max→WbServlet.serviceView; Workbench=iframe+injectBaja(fromWorkbench,widget) vs navegador=SPA top-level self-injectBaja; nmods-app(viewer isConfig=false)/nmods-config(editor isConfig=true)/redirect; mobile 100% client-side (no gating PX-style); index.html único entry, config.html vestigial | Java `-ux`+web-rt+SPA | **cerrado B235** |
| **E** | BG24 · **UI de alarmas cliente**: widget `alarm` + página de alarmas (consola, ack/ackAll, prioridades, filtros, sonidos `sound-library`) — B142 solo backend | SPA (beautify) | queued |
| **E** | BG25 · **Schedules (frontend)**: widget `schedule-list` + editor WebScheduler embebido + horarios/excepciones | SPA + schedule-rt | queued |
| — | BG26 · **Weather**: componente `Weather` compartido (current+forecast), provider **AerisWeather** vía cloud-proxy `weather.niagaramodules.com/observations`+`/forecasts` (host=hostId); ubicación por station (city/zip/coord) + override per-card; gate `weather.enabled` | SPA | **cerrado B236** |
| — | BG27 · **History pipeline + CSV**: HistoryChartDataResponse→HistoryData cursor SIN downsampling (vuelca rango crudo, vs chihuahua stride B174); BDateRangeEnum 15 rangos por tag; client fetch http/BQL; CSV: opencsv importa point-map + alarm CSV (NO history export, que es client-side data:text/csv). §14 corrige B216 | Java `-rt` + SPA | **cerrado B237** |
| — | BG28 · **Users/themes/favorites/profiles**: user=username/roles(passthrough station)/isConfig, SIN capability flags; theme global admin + colors tinycolor editor, SIN userThemes (dif chihuahua); favorites per-usuario WS command filename server-derived; **autorización=profiles** (route-restriction gating, no booleanos per-acción) | SPA + Java + disco | **cerrado B238** |
| — | BG29 · **Navigation + equipment model**: nav árbol 2-niveles (items[]+subnavs[], type=UI-selector, ADD_ITEM_FOR_PAGE auto; restricción via profiles); equipment types[](10 templates) vs items[](devices), encoding we() compacto {pointId:ord} delta-vs-template, servedBy; jerarquía building→floors/equipment (confirma B217) | SPA + disco | **cerrado B239** |

## Blocked / thin-source gaps (con lo que necesitan)

- **BG8 (payload)** — el mecanismo de note-update es static-legible, pero la afirmación "las fotos viajan base64
  dentro del note JSON" es `[INFER]` hasta ver un payload real → **block-on-thin-source** para esa sub-afirmación
  (o confirmar en fase dinámica/live-network). El mecanismo (byte-passthrough, sin multipart) SÍ es static-CERT.
- **BG6 (HistoryChart lib)** — el nombre de la lib de charting está perdido a la minificación; requiere beautify
  dirigido del chunk o inspección de red en vivo. El gauge SVG custom SÍ es static-CERT.
- **BG9 (3D runtime)** — la ausencia de pitch/extrusion en el código de Reflow es evidencia static fuerte, pero un
  "nunca 3D" tajante necesitaría confirmación live (un style Mapbox inyectado por config podría habilitarlo).

## Stop control (primario = read-only-investigable, METHODOLOGY §8)

- **Open gaps — read-only investigable**: **0**. Los 12 gaps cerrados.
- **Open gaps — requires-execution**: 0.
- **Fase dinámica ABIERTA (§12, opcional)**: station N4 VIVA (localhost, usuario `API`/HTTPBasicScheme + `API2`/DIGEST;
  Reflow **1.7.5-43**) + station de disco `HoneywellMX605132026` (dashboard real de 26 cards). Ya se usó para validar
  BG2 `[CERT-live]` (B217 §217.8) y como fuente del catálogo real (B218). **Pendiente opcional**: experimento de
  ESCRITURA supervisado (crear un dashboard más completo end-to-end; backup `bf70f28f…` listo como revert) — requiere
  OK del usuario, es lo único que queda y sale del read-only.
- **STOP DECLARADO** (primario: read-only-investigable = 0). Focus `nmodsreflow-builder` COMPLETO 12/12. Terminal:
  síntesis Parte A = B225; §18 self-retrospective pendiente de correr.
- Budget cap: none.

## Iteration history

| # | Fecha | Gap cerrado | Bloque | delegado? · modelo | Nuevos gaps |
|---|---|---|---|---|---|
| (bootstrap) | 2026-07-12 | — | — | sí · audit sweep matriz (sonnet) | 11 gaps derivados de la matriz de 24 subsistemas |
| 1 | 2026-07-12 | BG1 stack & librerías | B216 | no · inline (sobre matriz) | 0 (inventario; alimenta BG2/BG3/BG6/BG7/BG9) |
| 2 | 2026-07-12 | BG2 modelo dashboard + persistencia | B217 | sí · sweep SPA (sonnet) + validación live (no·inline) | 0 (validado [CERT-live] contra station viva; abre experimento de escritura dinámico) |
| 3 | 2026-07-12 | BG5 catálogo widgets (adelantado) | B218 | sí · sweep bundle (sonnet) + config real disco (no·inline) | 0 (20 tipos + type→component + defaults + add-flow; nueva fuente: dashboard real HoneywellMX605132026) |
| 4 | 2026-07-12 | BG7 assets embebidos + ORD→URL | B219 | sí · sweep imágenes (sonnet) | 0 (image-library nav-RPC, FontAwesome 1853, point-matrix 109, $ord.image resolver; sweep cubre también BG8) |
| 5 | 2026-07-12 | BG8 upload fotos propias | B220 | no · inline (sobre sweep B219) | 0 (veredicto: sin upload in-app; out-of-band + nav-RPC picker) |
| 6 | 2026-07-12 | BG3 motor JSON-Patch + control multiusuario | B221 | no · inline (lectura directa Java) | +1 (BG13 modernización, pedido usuario; BG11 ampliado a chihuahua-builder) |
| 7 | 2026-07-12 | BG9 vista geo Mapbox ("3D"=2D) | B222 | sí · sweep Mapbox (sonnet) | 0 (veredicto 2D tajante; weather-map 2 superficies + cloud niagaramodules) |
| 8 | 2026-07-12 | BG4 editor visual + layout masonry | B223 | sí · sweep editor (sonnet) | 0 (2 mounts+iframe preview, v-masonry, reorder lista, resize Selects; abre correcciones §14 a B216/B218 en BG6) |
| 9 | 2026-07-12 | BG6 render gauge/chart | B224 | sí · sweep editor/render (sonnet) | 0 (§14: d3 presente aliaseado corrige B216; circle=iView corrige B218; Gauge SVG bespoke) |
| 10 | 2026-07-12 | BG10 síntesis Parte A | B225 | no · inline (síntesis) | 0 (flujo end-to-end + stack por capa + 6 hilos transversales) |
| 11 | 2026-07-12 | BG13 modernización stack | B227 | no · inline + 1 WebSearch (snapshot) | 0 (Vue2 EOL→Vue3/Pinia/Vite/TS, iView→SVG propio, mapbox→MapLibre, Masonry→CSS Grid; mantener doc+JSON-Patch+d3) |
| 12 | 2026-07-12 | BG11 chihuahua-builder + portabilidad | B226 | sí · sweep chihuahua (sonnet) | 0 (**FOCUS COMPLETO 12/12**; chihuahua=dashboard fijo sin builder, tiene 3D Three.js, lidera RBAC; plan portabilidad 5 piezas) |

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
- **B218**: tokens load-bearing grep-confirmados en beautified temp (`cardTypes` registro BF:100416 · dispatch
  v-if BF:92544 · alias `gauge` BF:92554 · `newCard`/ADD_CARD BF:91353 · Select "Card Type" BF:92432 ·
  `cardTypeChanged` BF:100536 · `weather-map defaultConfig` BF:26882 · hx-iframe BF:23047 · url-iframe BF:23319 ·
  badgeForCard/nameForCard BF:91255/91301) + catálogo real del config de disco (10 tipos + schema por tipo, jq).
  verify-block exit 0. `[CERT]` 10 · `[INFER]` 3. Ratio 0.30 (evidencia, sano). 20 tipos ofrecidos + alias legacy.
  Probe sanitizado `sources/probes/B218-dashboard-catalog-real-20260712.txt`. Divergencia versión notada.
- **B219**: tokens load-bearing grep-confirmados — `$ord.image()` resolver BF:3766 (module://→/module/, file:→/ord/),
  icon picker BF:61772 (icon-categories/icon-search.json, far), point-matrix BF:4919, nav-RPC image-library BF:38851,
  default map BF:5033, backgroundImage BF:21233 + Java (ImageLibraryResponse:31 `module://…/image-library`,
  ImageListResponse:49 formatos jpg/png/svg/gif/jpeg). verify-block ok. `[CERT]` 10 · `[INFER]` 1. Ratio 0.10
  (evidencia sólida). Hallazgo clave: Reflow NO sirve imágenes — delega a servlets nativos Niagara `/module/` y
  `/ord/`; ImageLibrary/ImageList REST son vestigiales (bundle usa nav-RPC).
- **B220**: tokens load-bearing grep-confirmados — BaseServlet doPost 4 rutas (:273-285, sin multipart/getParts),
  bundle 0 hits FileReader/multipart/FormData, ImageBrowser BF:40681 (file:^/module://, emite {ord,width,height}),
  nav root station BF:38839 (`station:|file:^`), EquipmentNoteUpdate :20-27 (^reflow/notes/, byte-passthrough),
  ImageListResponse :49 formatos. Evidencia disco: shared/Imagenes reales. verify-block ok. `[CERT]` 9 · `[INFER]`
  4. Ratio 0.44 (evidencia+diseño chihuahua). Veredicto: SIN upload in-app, fotos out-of-band vía Workbench.
