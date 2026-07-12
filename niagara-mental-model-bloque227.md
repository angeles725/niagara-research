# Block 227 — Reflow dashboard-builder (XI): modernización del stack — qué usar hoy y qué mejorar

> **Qué documenta.** Un análisis de MODERNIZACIÓN del stack de Reflow: qué piezas están viejas/EOL, cuál sería el
> stack equivalente hoy (2026), y qué mejoras arquitectónicas valdría la pena — respondiendo el pedido explícito
> del usuario. Gap BG13 del focus `nmodsreflow-builder`. Bloque de DISEÑO/análisis.
>
> **Alcance.** Recomendaciones sobre el stack documentado en B216/B222/B224. No es un plan de migración detallado
> (eso sería un focus de implementación); es el mapa "de qué a qué" con tradeoffs.
>
> **Fuentes.** El stack real (B216, B222, B224) + estado actual de las librerías
> (`sources/web-snapshots/B227-vue2-eol-ecosystem-20260712.md`).
>
> **Método / markers.** `[CERT]` = stack real (bloques previos). `[CERT-web]` = estado de una librería (snapshot).
> `[INFER]` = recomendación de diseño (predomina — es un bloque de análisis; ratio alto ESPERADO, §11).

---

## 227.1 — Diagnóstico: qué está viejo `[CERT]`/`[CERT-web]`

El stack de Reflow (build 1.7.7, ~2023) está construido sobre la generación **Vue 2**, hoy end-of-life:

| Pieza | Estado hoy (2026) | Señal |
|---|---|---|
| **Vue 2.6.14** | **EOL 2023-12-31** — sin parches de seguridad `[CERT-web]` | soporte extendido sólo pago (HeroDevs NES) |
| **Vuex 3.5.1** | Reemplazado por **Pinia** en Vue 3 `[CERT-web]` | Vuex en modo mantenimiento |
| **vue-router 3** | Requiere v4 para Vue 3 | atado a Vue 2 |
| **iView (`<Circle>`, B224)** | **View UI / iView está discontinuado / sin mantenimiento activo**, atado a Vue 2 `[INFER]` (ecosistema Vue2) | el `circle` gauge depende de él |
| **Masonry.js + vue-masonry** (B223) | Funciona pero es una lib de layout imperativa pre-CSS-Grid `[INFER]` | CSS Grid/`grid-template` nativo hoy cubre el caso |
| **wrapper HTTP propio (no axios, B216)** | Reinventa `fetch` con forma axios `[INFER]` | `fetch` nativo + un wrapper fino basta |
| **mapbox-gl (token compartido, B222)** | mapbox-gl GL JS v2+ cambió a licencia con costo por carga de mapa `[INFER]` | riesgo de costo/lock-in; el token es único para todos los tenants |
| **build (webpack/Vue CLI)** | Vue CLI en mantenimiento; **Vite** es el estándar `[CERT-web]` | builds más lentas |

## 227.2 — El stack equivalente hoy (de qué a qué) `[INFER]`

| Capa | Reflow (2023) | Equivalente 2026 | Por qué |
|---|---|---|---|
| Framework | Vue 2.6 | **Vue 3.4+ (Composition API)** | soportado, reactividad Proxy, mejor TS |
| Estado | Vuex 3 | **Pinia** | oficial para Vue 3, tipado, menos boilerplate |
| Router | vue-router 3 (hash) | vue-router 4 (o history mode) | par de Vue 3 |
| Build | Vue CLI/webpack | **Vite** | dev instantáneo, bundles más chicos |
| Lenguaje | JS | **TypeScript** | el `config.json` opaco (B217) se beneficia enormemente de tipos por card-type |
| Layout | Masonry.js | **CSS Grid / `grid-template` + container queries** | nativo, responsive sin JS de layout |
| Gauges | iView `<Circle>` + SVG a mano | un componente SVG propio o una lib viva (p. ej. una de gauges Vue3) | quita la dependencia EOL |
| Charts | D3.js (`<d3chart>`) | **D3 sigue vigente** — mantener, o Observable Plot/ECharts para menos código | D3 no es el problema |
| Mapa | mapbox-gl (token compartido) | **MapLibre GL** (fork open-source, sin licencia por carga) | evita costo/lock-in Mapbox v2 |
| HTTP | wrapper propio | `fetch` + wrapper fino (o `ky`) | menos código, estándar |
| JSON-Patch | fast-json-patch / flipkart-zjsonpatch | mantener (RFC-6902 es estable) | el diseño es bueno |

## 227.3 — Mejoras arquitectónicas (más allá de versiones) `[INFER]`

1. **Tipar el documento del dashboard**: hoy `config.json` es un blob opaco (B217 §217.3) y las cards se validan por
   fallbacks perezosos (B218). Un **schema TypeScript/Zod por card-type** daría validación, autocompletado y
   migraciones seguras — el punto más débil del diseño actual.
2. **Upload de imágenes in-app real**: hoy no hay upload; las fotos se cargan out-of-band por Workbench (B220). Un
   endpoint multipart (o base64 controlado) haría el flujo "agregar foto" autocontenido — la mayor brecha de UX.
3. **Editor sin iframe**: el editor-sobre-iframe (B223 §223.1) es frágil (sincronización de globals entre ventanas).
   Vue 3 + teleport/portal o un store compartido permitiría editar sin el doble-mount.
4. **Paleta drag real**: hoy "agregar widget" es un dropdown (B218 §218.4). Una paleta drag-and-drop a la grilla
   sería más intuitiva (aunque menos determinista).
5. **Auth y assets ya están bien**: delegar a la plataforma Niagara (`/module/`, `/ord/`, SCRAM) es correcto — NO
   reinventarlo (B219/B220). Es la parte más sólida.
6. **Seguridad** (cross-ref B150): el config-write sin permission-check y el `doPrivileged` ancho (B221/B143) deben
   cerrarse en cualquier reescritura — un RBAC write-gate como el de chihuahua (B164).

## 227.4 — Qué NO tocar (lo que está bien) `[INFER]`

- El **modelo documento-JSON + JSON-Patch** (B217/B221): event-sourcing ligero, colaboración en caliente, backups.
  Es un buen diseño; mantenerlo.
- **D3** para charts (B224): vigente y potente.
- **Delegar assets/auth a la plataforma** (B219/B220): correcto, cero mantenimiento propio.
- El **server delgado** (B216): que el Java sólo persista/parchee/sirva es una separación limpia.

## 227.5 — Conexiones

- **[Block 216]/[Block 222]/[Block 224]** — el stack real que este bloque evalúa.
- **[Block 150]/[Block 143]** — la deuda de seguridad a saldar en cualquier reescritura.
- **Hacia BG11 (chihuahua, B226)**: chihuahua ya usa un stack más simple (ES5 IIFE, sin framework) con RBAC — la
  comparación matiza qué "modernizar" significa según el objetivo (builder rico vs dashboard fijo seguro).
