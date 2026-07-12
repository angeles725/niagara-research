# Block 216 — Reflow dashboard-builder (I): stack y librerías, y la función de cada una

> **Qué documenta.** El STACK completo del módulo NiagaraMods Reflow v1.7.7 (.75) visto desde el ángulo
> de PRODUCTO/BUILDER: qué librerías embarca —lado servidor Java `-rt` y lado navegador (SPA Vue)— y QUÉ
> FUNCIÓN cumple cada una en la construcción/edición del dashboard. Primer bloque del focus
> `nmodsreflow-builder`. Responde directamente la parte "stack, librerías, cuáles son las funciones de esas
> librerías" del pedido.
>
> **Alcance.** Inventario de dependencias + rol de cada una. NO detalla aún el modelo de dashboard (BG2), el
> editor (BG4) ni el catálogo de widgets (BG5) — esos son bloques siguientes. Corrige dos supuestos previos:
> el stack NO incluye **d3** ni **axios** (grep-refutado, §216.4).
>
> **Fuentes (primarias, build .75).**
> - Java `-rt`: `com/niagaramods/nmodsreflow/**` (imports reales `file:line`) en
>   `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/`.
> - SPA (bundles minificados, 1 línea — se cita por BANNER de librería, no por línea):
>   `rc/js/app.4509efb4.js` (sha256 `81b82b839ec8058c3cf0c491fd4c7582907c91bdf95edbe96dbd130761df8fb9`,
>   2 631 974 bytes — identidad estable, coincide con B153) y `rc/js/chunk-vendors.3fecdb47.js`
>   (sha256 `b82c3527316fc0bc4574ee2a06479ef4aa74c07d0fad0ed6740b5db7533318ca`, 2 795 017 bytes).
>
> **Método / markers.** `[CERT]` = leído en la fuente primaria (`file:line` para Java; string de BANNER de
> versión para las libs JS, dado que el bundle es 1 línea). `[INFER]` = deducción del rol (no literal en la
> fuente). Ausencias verificadas por grep negativo sobre ambos bundles.

---

## 216.1 — Librerías del servidor (`-rt`, Java) y su función `[CERT]`

Reflow `-rt` es un módulo Niagara sobre `javax.baja.*` + `com.tridium.*` (framework host). Encima embarca
cinco dependencias de terceros/utilitarias, cada una con un rol acotado en el flujo del dashboard:

| Librería | Import verificado (`file:line`) | Sitios | Función en Reflow |
|---|---|---|---|
| **Jackson** (`com.fasterxml.jackson`) | 30 clases importan `com.fasterxml.jackson.*` | 30 | Serializar/parsear JSON: el `config.json` del dashboard, los responses de history/alarms/equipment. Es el motor JSON dominante del servidor `[INFER]` rol. |
| **flipkart-zjsonpatch** (`com.flipkart.zjsonpatch.JsonPatch`) | `sync/BReflowSyncService.java:7` (import) · `:420` (`JsonPatch.apply(delta, service.config)`) | 1 | **El motor "editá-y-se-actualiza".** Aplica un **JSON Patch RFC-6902** (el `delta` que manda el cliente) sobre el `config.json` en memoria — merge incremental multiusuario. Núcleo de BG3. |
| **OpenCSV** (`com.opencsv`) | `commands/BReflowCSVCommands.java` (única clase) | 1 | Exportar datos de history a CSV (descarga tabular). Periférico al builder. |
| **Apache Commons-IO** (`org.apache.commons.io.output.TeeOutputStream`) | `sync/ConfigIO.java:15,121` · `http/responses/HistoryGroupsResponse.java:20,65` · `http/responses/HistoryListResponse.java:20,66` | 3 | `TeeOutputStream` = escribir a DOS destinos a la vez (cache en disco **y** el stream de respuesta/GZIP simultáneamente). Sostiene el patrón cache-and-serve del `config.json` y del history. |
| **com.tridium.json** (`JSONArray`/`JSONObject` — del host Niagara, NO de terceros) | `http/responses/ImageLibraryResponse.java:3-4` · `ImageListResponse.java:3-4` | 2 | JSON del listado de imágenes (biblioteca embebida + imágenes del usuario). Convive con Jackson: las respuestas de assets usan el JSON del host, no Jackson. |

**Lectura de conjunto** `[INFER]`: el servidor es DELGADO respecto del dashboard — no modela el dashboard como
árbol de objetos Java. Su trabajo es (a) **persistir/servir** un blob JSON opaco (`config.json`, vía Jackson +
Commons-IO Tee), (b) **aplicar deltas** a ese blob (zjsonpatch), y (c) **servir assets** (imágenes/sonidos/
iconos). Toda la inteligencia de composición vive en el cliente (§216.2). Esto encaja con B138/B143/B145
(persistencia sin auth, `applyConfig`) pero aquí framea el ROL de producto, no el hueco de seguridad.

## 216.2 — Librerías del navegador (SPA) y su función `[CERT]`

La SPA es una app Vue empaquetada con Vite/webpack. Versiones leídas del BANNER de cada lib en los bundles
(el bundle es 1 sola línea; se cita `<archivo>: "<banner>"`):

| Librería | Banner verificado | Función en el builder |
|---|---|---|
| **Vue 2.6.14** | `chunk-vendors.3fecdb47.js: "Vue.js v2.6.14"` | Framework de UI reactivo. Cada tarjeta/widget del dashboard es un componente Vue; el modo edición es estado reactivo (B151/B153 ya fijaron 2.6.14, corrigiendo B50). |
| **Vuex 3.5.1** | `chunk-vendors.3fecdb47.js: "vuex v3.5.1"` | Store central de estado. El array de tarjetas del dashboard (`dashboardCards`) vive en Vuex — es la fuente de verdad en cliente antes de persistir (BG2). |
| **vue-router 3.4.5** | `chunk-vendors.3fecdb47.js: "vue-router v3.4.5"` | Ruteo hash (`/nmodsreflow/#…`, B152/B154). Las páginas del dashboard y las vistas de edición son rutas; NO hay ruta `/editor` dedicada (el editor es inline, BG4). |
| **SortableJS 1.10.2** (vía `vuedraggable`) | `chunk-vendors.3fecdb47.js: "Sortable 1.10.2"` | Drag-and-drop de REORDENAMIENTO de listas (páginas, grupos, schedules). NO es el posicionamiento de widgets en el lienzo. |
| **vue-drag-resize** | `app.4509efb4.js: "vue-drag-resize"` (5 hits) · `"VueDragResize"` (1) | Componente de arrastrar/redimensionar de POSICIÓN LIBRE — usado para elementos flotantes (p. ej. markers del mapa / paneles libres), SECUNDARIO a la grilla masonry de tarjetas (BG4). |
| **mapbox-gl** | `chunk-vendors.3fecdb47.js` (strings del propio lib: `"Mapbox GL JS…"`; API `Mgl*` en app.js) — versión exacta no extraíble del banner minificado `[INFER]` 2.x | Renderiza la vista geo `building-map` (WebGL). Reflow la maneja como **mapa 2D** con markers de edificios/equipos (BG9). Es la única pieza WebGL; es lo que el usuario percibe como "3D" (§216.3). |
| **FontAwesome** (light/regular/solid) | `rc/fonts/fa-{light-300,regular-400,solid-900}.woff2` · 814 refs `fa-*` en `app.4509efb4.js` | Iconografía de widgets/equipos. Alimenta el selector de iconos buscable (`icon-search.json`, 1853 iconos — BG7). |

## 216.3 — La pieza "3D": qué es realmente `[CERT]`/`[INFER]`

El pedido menciona "diseños 3D". En el stack **no hay motor 3D**: grep negativo de `THREE.`/`three.js`/
`babylon`/`BABYLON`/`WebGLRenderer`/`PerspectiveCamera` = **0 hits** en ambos bundles `[CERT]` (grep negativo).
La única capa WebGL es **mapbox-gl**. Las primitivas pseudo-3D de Mapbox (`fill-extrusion`, `pitch`, `extrude`)
aparecen SÓLO dentro de `chunk-vendors.js` (la librería en sí) y **cero en `app.4509efb4.js`** (el código propio
de Reflow) `[CERT]` (grep). Es decir: Reflow usa Mapbox como **mapa 2D interactivo** (markers de edificios), con
la capacidad 3D presente-pero-no-usada en la librería. Detalle completo en BG9; se adelanta aquí porque redefine
qué significa "3D" en este producto: **no es modelado 3D, es una vista de mapa WebGL 2D** `[INFER]` (síntesis).

## 216.4 — Ausencias verificadas (corrige supuestos previos) `[CERT]`

| Supuesto | Verdict | Evidencia |
|---|---|---|
| Usa **d3** para charts/gauges | **AUSENTE** | `d3-selection`/`d3-scale`/`d3-shape`/`d3-array`/`d3-axis` = 0 hits en ambos bundles `[CERT]` (grep negativo). Los "d3…" que aparecían eran ids de módulo webpack / colores hex. Los gauges son **SVG custom** (BG6). |
| Usa **axios** para HTTP | **AUSENTE** | `axios` = 0 hits en ambos bundles `[CERT]` (grep negativo). El cliente usa un wrapper POST propio (mecanismo exacto → BG2/BG4). |

Ambas ausencias importan para la Parte B (portabilidad a chihuahua): no hay que replicar d3/axios, y el gauge es
reproducible con SVG plano.

## 216.5 — Conexiones

- **[Block 151]/[Block 153]** — fijaron Vue 2.6.14 + router hash + Vuex desde el ángulo cliente/seguridad; aquí
  se reencuadra su FUNCIÓN de producto y se agrega el inventario completo (Sortable, vue-drag-resize, mapbox,
  FontAwesome) con su rol.
- **[Block 143]/[Block 145]** — documentaron `applyConfig`/`ConfigDeltaResponse` como superficie de escritura
  sin auth; §216.1 identifica la LIBRERÍA detrás (`flipkart-zjsonpatch`, RFC-6902) y su rol de producto (motor
  de update en vivo) — se profundiza en BG3.
- **[Block 149]** — `FileResponse`/`ImageLibrary`/`EquipmentNote` como sinks; §216.1 fija que las respuestas de
  imágenes usan `com.tridium.json`, no Jackson — base para BG7 (assets embebidos).
- **Hacia adelante**: BG2 (modelo de dashboard `cards[]` + persistencia), BG3 (zjsonpatch en detalle), BG9 (la
  vista Mapbox), BG11 (qué de este stack se replica o se evita al portar a chihuahua).
