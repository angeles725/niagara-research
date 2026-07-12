# Block 226 — chihuahua como (no-)builder: comparación con Reflow y plan de portabilidad

> **Qué documenta.** El módulo propio `chihuahua` (`com.angeles.chihuahua`) documentado con las MISMAS dimensiones
> que el focus Reflow-builder, la comparación de capacidad "constructor de dashboards" chihuahua↔Reflow, y el plan
> priorizado para portar la capacidad builder a chihuahua. Gap BG11 (Parte B, pedido del usuario). Bloque de
> documentación + comparación + diseño. **Cierra el focus `nmodsreflow-builder` (12/12).**
>
> **Alcance.** chihuahua visto como producto-dashboard. Complementa (no repite) el focus `chihuahua` B163-B177
> (que lo documentó por subsistemas/seguridad); aquí el eje es la capacidad de EDICIÓN/builder.
>
> **Fuentes (primarias).** Fuente propia de chihuahua (lectura directa, `file:line`):
> `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-{rt,ux,wb}/src`.
> Bloques existentes B163/B170/B171/B172/B177 (reutilizados). Barrido delegado (sonnet); tokens re-verificados.
>
> **Método / markers.** `[CERT]` = fuente primaria chihuahua o Reflow (bloques). `[INFER]` = plan/recomendación.

---

## 226.1 — Stack de chihuahua `[CERT]`

ES5 IIFE puro servido estático desde el JAR, **sin framework, sin build system, sin transpiler** (no hay
`package.json`/`webpack.config` en el árbol) `[CERT]`. Libs de terceros dropeadas a mano (no npm):
- **Chart.js** (`rc/ext/chartjs/chart.umd.min.js` + adapter date-fns) — charts de historia.
- **Three.js** (`rc/ext/threejs/three.module.js` + addons OrbitControls) — **escenas 3D de equipos** en
  `UpDetail.js` (`:13` import OrbitControls, `:1286` `new THREE.Scene()`, `:1287` background) y `CarcamoDetail.js`.
- Backend: servlet Java plano, JSON a mano (`ChiJsonUtil`, sin librería JSON).

Contraste con Reflow (Vue 2.6.14 + Vuex + D3 + Mapbox + iView + Masonry, build transpileado, B216/B225): **niveles
de tooling categóricamente distintos** — chihuahua no tiene NADA de la maquinaria de framework/build.

## 226.2 — Modelo de dashboard: HARD-CODED, no editable `[CERT]`

A diferencia del `config.json` editable de Reflow (B217), el dashboard de chihuahua está **horneado en constantes
Java**:
- `GET /api/config` devuelve un "hardcoded CONFIG_JSON constant" (comentario literal, `BChiServlet.java:34`),
  ensamblado de `CONFIG_JSON_BASE_HEAD` (`:83`) + `CONFIG_JSON_BASE_TAIL` (`:115`), impreso en `:393` — labels de
  navegación, mapa de URLs de API, colores, intervalos de poll; NO un modelo de layout/widgets.
- Las zonas del floorplan (polígonos SVG del home de 6 plantas) son la constante `ZONES_JSON`
  (`ChiEquipmentReader.java:74`), con el comentario "Zone polygon coordinates extracted from HomeMap.js" (`:72`) —
  transcrito a mano una vez y congelado.
- Lo único dinámico es `monitorOrds` (18 ORDs generados para 6 plantas × 3 tipos) y el array `plantas` — identidad
  de EQUIPOS, no layout ni qué-mostrar.

## 226.3 — Editor visual: AUSENTE (evidencia negativa directa) `[CERT]`

No hay modo edición, ni CRUD de tarjetas, ni vía de escritura de layout en `-ux`/`-wb`. Comentario de código que
confirma la ausencia INTENCIONAL: `UpDetail.js:3031` — "No drag-drop; the three big mode buttons…". Los únicos
hits de `drag`/`resize` en el árbol son listeners de window-resize / observers del visor 3D, no edición de layout.
`BBatchLinkEditor` (Workbench, B172) es una **herramienta de ingeniería** (cableado de BLinks en Workbench), NO un
editor de dashboard de usuario final — no toca esta brecha.

## 226.4 — Catálogo de widgets: páginas fijas hechas a mano `[CERT]`

Set de páginas fijo enumerado en `DashboardApp.js:460` (`['home','alarms','schedules','equipment','detail',
'configuracion','audit']`); cada una es un módulo JS hand-built (`HomeMap.js`, `UpDetail.js` 4049 líneas,
`CarcamoDetail.js`, `DataloggerDetail.js`, `AlarmsPage.js`, `ScheduleView.js`). Agregar un tipo = un DEVELOPER
escribe un `.js` nuevo; no hay registry en runtime que el usuario elija. `EquipmentCard.js` tiene un `TYPE_CONFIG`
fijo (up/carcamo/datalogger) — 3 renderers de tipo hardcoded, no un catálogo genérico.

## 226.5 — Assets/imágenes: estáticos en el JAR, sin upload ni biblioteca `[CERT]`

Imágenes bajo `rc/img/` (logos de branding; 1 foto de floorplan `MAQUILA_COMPLETA.jpeg`; 15 JPGs pre-renderizados
de datalogger por estado de color, `DataloggerDetail.js` elige el archivo por color de umbral). Todo embarcado en
el JAR a build-time — **sin endpoint de upload, sin UI de biblioteca de imágenes, sin esquema `file:^`** como el
`Imagenes` + image-library de Reflow (B219/B220).

## 226.6 — Comparación de capacidad builder chihuahua ↔ Reflow

| Dimensión | chihuahua | Reflow | Brecha |
|---|---|---|---|
| Stack | ES5 IIFE, sin build, Chart.js + **Three.js** | Vue 2 + Vuex + D3 + Mapbox + iView, transpilado | chihuahua no tiene framework/build |
| Modelo de dashboard | **hardcoded** (`CONFIG_JSON_BASE_*`, `ZONES_JSON`) | `config.json` editable (B217) | sin modelo editable |
| Editor visual | **ausente** ("no drag-drop", `UpDetail.js:3031`) | presente (drawer + masonry, B223) | **brecha central** |
| Catálogo de widgets | 7 páginas fijas + 3 tiles hardcoded | 20 tipos elegibles (B218) | sin catálogo/picker |
| Assets/imágenes | estáticos en JAR, sin upload | `file:^Imagenes` + image-library (B219/B220) | sin esquema de assets |
| 3D | **Three.js real** (escenas 3D de equipos) | **Mapbox 2D** (no 3D real, B222) | **chihuahua LIDERA** |
| Charts | Chart.js | D3.js (B224) | equivalente |
| Update de datos | baja push + REST fallback (B170) | baja vía reactividad Vue | funcionalmente similar |
| Update de LAYOUT | **ninguno** (no hay canal) | WS + JSON-Patch en vivo (B221) | sin equivalente |
| RBAC write-gate | `checkCanWrite` fail-closed (B164) | ninguno (B160) | **chihuahua LIDERA** |
| Audit | ring buffer + merge login (`auditLog`, `BChiDashboardService.java:70,156`) | ninguno | **chihuahua LIDERA** |

## 226.7 — El hallazgo del "3D": chihuahua SÍ hace 3D real, Reflow no `[CERT]`

Punto notable para el pedido del usuario sobre "diseños 3D": **es chihuahua —no Reflow— el que tiene 3D real**.
chihuahua usa **Three.js** (WebGL) para escenas 3D de equipos (`UpDetail.js:1286` `new THREE.Scene()` +
OrbitControls). Reflow NO tiene motor 3D (B216/B222: 0 three.js/babylon; su "3D" es Mapbox 2D). Si el objetivo es
visualización 3D de equipos, chihuahua ya tiene la base que Reflow carece.

## 226.8 — Veredicto y plan de portabilidad `[CERT]`/`[INFER]`

**Veredicto** `[CERT]`: chihuahua **NO tiene capacidad de dashboard-builder** — es un dashboard FIJO,
developer-authored, single-site. Cada decisión de layout (páginas, tipos de widget, zonas de floorplan, imágenes)
está horneada en constantes Java o JS a build-time. Extiende B177 ("config end-user: vistas fijas a medida") con
prueba `file:line` de que la fijeza es DELIBERADA (comentario "no drag-drop", transcripción manual de `ZONES_JSON`).

**Plan de portabilidad** (piezas para portar la capacidad builder de Reflow → chihuahua, ranked por esfuerzo)
`[INFER]`:

1. **Modelo de dashboard editable** (mayor, fundacional): reemplazar `CONFIG_JSON_BASE_*`/`ZONES_JSON` por un schema
   editable (páginas/cards/zonas como DATO) + un slot de persistencia. **chihuahua ya tiene el patrón**: `auditLog`/
   `userThemes` son precedente de JSON-en-slot-`BString` en `BChiDashboardService`.
2. **Editor visual** (mayor, depende de #1): construir desde cero (drag/resize/CRUD), **gateado por el
   `checkCanWrite`/RBAC existente** — ventaja real: chihuahua podría shipearlo MÁS SEGURO que Reflow (que no tiene gate).
3. **Abstracción de catálogo de widgets** (medio): convertir las 7 páginas y 3 tiles hardcoded en un registry de
   definiciones pluggables; refactor no trivial de `DashboardApp.js`/`EquipmentCard.js`, pero la frontera de
   módulos ES5 ya existe para construir encima.
4. **Esquema de assets/imágenes** (medio): store de assets + resolución de referencias (el patrón `file:^Imagenes`
   de Reflow es el modelo). Hoy chihuahua tiene cero.
5. **Sync de layout en vivo** (medio-bajo, reusa infra): chihuahua ya tiene `SubscriptionPool`/
   `EquipmentSnapshotStore` (B170) para DATOS; extenderlo (o un canal WS paralelo) para deltas de LAYOUT es menor
   que #1-#3, y heredaría el RBAC que Reflow no tiene.

**Lo que chihuahua ya tiene y ayuda** `[CERT]`: RBAC fail-closed (`ChiRbacHelper`), audit ring + merge login,
routing servlet testeable (B165), pipeline de subscripción con throttling ya resuelto (B170), y el patrón
validate-then-atomic-commit de `BBatchLinkEditor` (B172) reutilizable para un flujo "validar diff de layout, commit
atómico".

## 226.9 — Conexiones

- **[Block 225]** — la síntesis de la capacidad Reflow que aquí se contrasta.
- **[Block 177]** — comparación chihuahua↔Reflow por seguridad/arquitectura; §226 agrega el eje builder con prueba
  de fijeza deliberada.
- **[Block 170]/[Block 171]/[Block 172]** — subscripción, write-path, WB tool reutilizados para el plan.
- **[Block 227]** — la modernización aplica a AMBOS: un builder portado a chihuahua debería nacer moderno (schema
  tipado, editor sin iframe) y aprovechar su RBAC.
- **Cierre del focus**: 12/12 gaps. Parte A (Reflow builder, B216-B225) + Parte B (chihuahua + portabilidad, §226) +
  modernización (B227) completas.
