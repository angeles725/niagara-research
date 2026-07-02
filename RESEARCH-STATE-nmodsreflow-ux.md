# nmodsreflow-ux (NiagaraMods Reflow v1.7.7 `-ux`) — Research State

> Focus: **capa cliente/browser del módulo `nmodsreflow` build 1.7.7.75** — el módulo `-ux` fino de Niagara
> (registro de vista/perfil + loaders JS que bootstrapean la SPA) Y la SPA Vue embarcada que sirve el `-rt`
> desde `rc/`. Reconstruye cómo el UI se registra, arranca y consume el contrato del backend. Vista de la
> ARQUITECTURA FRONTEND, complementaria al focus `nmodsreflow` (backend `-rt`, B138-B150, CERRADO).
> READ-ONLY. Corpus language: Spanish (technical EN).
>
> Source roots (primarios, JAR embarcado build .75, decompile Vineflower):
> `UX/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-ux/vineflower`
>   (3 clases Java `com/niagaramods/nmodsreflow/ux/BReflow*.java` + `niagara/*.js` loaders + `module.palette`).
> `SPA/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/rc/`
>   (la SPA Vite embarcada: `js/app.4509efb4.js` **minificada 1 línea**, `js/chunk-vendors.3fecdb47.js`, assets).
> Referencia NO autoritativa (dev tree v1.7.5, deobfuscado): B50/B51 (`app-readable.js`, Reflow-Clean-177).
> Tools: `decompile-java.sh` (ya aplicado) + lectura directa + grep + (para la SPA minificada) beautifier JS
> a provisionar si hace falta (§10).
> Mirrored in engram (project `niagara-research`): `research/niagara/nmodsreflow-ux/gaps`, `.../progress`.

## Why this focus exists

El focus backend `nmodsreflow` (B138-B150) mapeó el `-rt` completo con rigor `file:line` y cerró el hilo de
seguridad. El frontend quedó cubierto sólo parcialmente: B50/B51 lo auditaron a nivel *discovery* pero para
**v1.7.5** (dev tree + bundle deobfuscado), NO para el build `.77`. Este focus da **paridad**: barre la capa
cliente del `.77` con el mismo rigor, y cierra el contrato frontend↔backend contra B149 (data contract) y la
cadena de seguridad B150. El usuario pidió arrancarlo (2026-07-02).

## Angle (confirmado con el usuario, 2026-07-02)

**"Capa cliente `-ux` del build .77"**: módulo `-ux` fino (registro + loaders) + SPA embarcada, cruzando
contra B149/B150. NO es re-auditar v1.7.5 (eso es B50/B51); es el build `.77` con rigor `file:line`.

## Coverage

- **Covered blocks (este focus)**: 4 — B151 (esqueleto `-ux`), B152 (loaders JS), B153 (SPA embarcada), B154 (wiring cliente↔backend: endpoints REST + comandos WS).
- **Coverage metric**: 4 / 7 gaps cerrados.
- **Last iteration**: 2026-07-02 — U3 cerrado (SPA embarcada): bundle `app.4509efb4.js` (sha256 `81b82b83…`,
  2.63 MB) beautificado con js-beautify. Build stamp `v1.7.7.75 RC5`. **Framework: Vue 2.6.14** (§14: CORRIGE/
  refina B50 que decía 2.7.16 — era el dev-tree v1.7.5), vue-router 3.4.5 hash, Vuex, axios. Contrato de globals
  `injectBaja`/`injectConfig`/`destroyApp` VERIFICADO (B152). Router hash `/nmodsreflow/#`. Confirmado desde el
  cliente que **`Client-Username` es estado Vuex mutable** → audit forjable (B145). Secreto: token Mapbox
  público hardcodeado (`app.beauty.js:118864`). WS a `/nmodsreflow/ws` canal `reflow`, `sync-delta` como comando
  (B140/B143). El barrido dejó U4/U5 mayormente pre-respondidos.
- **Last iteration**: 2026-07-02 — U4 cerrado (wiring): mapa completo endpoint→método→backend de la SPA (tabla
  en B154 §154.1) + set de comandos WS sobre el canal `reflow`. **Confirma B144 desde el cliente**: backups
  create/rename/apply/destroy/reset son GET con `?file=` (mutación destructiva GET-shaped / CSRF). Confirma B143
  (favorites-read/write, sync-delta, config-control son los comandos WS) y B145 (config_update/delta POST +
  headers Client-Id[server]/Client-Username[mutable]/Client-Migration[nuevo]).

## Gap-backlog (priorizado)

| Prioridad | Gap | Tipo/fuente | Estado |
|---|---|---|---|
| — | U1 · esqueleto `-ux`: `BReflow`/`BReflowConfig`/`BReflowRedirect` (tipos BComponent/profile, registro de vista) + `module.palette` + `module.xml` | Java `-ux` | **cerrado B151** |
| — | U2 · cadena de loaders JS: `reflow.js`/`reflow_config.js`/`reflow_redirect.js` + `lib/{loader,resolver,hyperlink}.js` — cómo BajaScript bootstrapea y monta la SPA | JS `-ux` | **cerrado B152** |
| — | U3 · SPA embarcada `.77`: identidad/build de `app.4509efb4.js` (minificada) + `chunk-vendors` — framework (Vue 2.6.14, corrige B50), diff forense frontend 1.5↔1.7 vs B51 | JS `-rt/rc` (beautify js-beautify) | **cerrado B153** |
| — | U4 · wiring frontend↔backend: cómo la SPA llama al REST/WS (cara cliente del contrato B149 + canal WS B140) — capa fetch/axios, endpoints, headers | JS SPA + cross-ref B149/B140 | **cerrado B154** |
| medium | U5 · postura de seguridad cliente: ¿la SPA envía los headers `Client-Username`/`Client-Id` (audit forjable B145)?; cómo arma los params `file`/`query`/`config` (cara cliente de B142/B144/B145/B147); secretos/tokens en el bundle; interplay CSP (B149) | JS SPA + cross-ref B145/B147/B149 | pending |
| low | U6 · redirect/hyperlink: `BReflowRedirect` + `reflow_redirect.js` + `lib/hyperlink.js` — deep-linking / navegación ORD (posible superficie de open-redirect) | Java+JS `-ux` | pending |
| low | U7 · config cliente: `BReflowConfig` + `reflow_config.js` — contrato de config del lado cliente (cross-ref config.json B143/B145 + B51) | Java+JS `-ux` | pending |

## Blocked gaps (con lo que necesitan)

- **U3/U4/U5 (parcial)** — la SPA `.77` (`app.4509efb4.js`) está **minificada a 1 línea**. Legible pero
  necesita un **beautifier JS** (js-beautify/prettier) para rigor `file:line`; a provisionar (§10) en la
  iteración que la ataque. NO bloquea el focus (U1/U2/U6/U7 son directamente legibles); sólo condiciona U3-U5.

## Stop control (primario = read-only-investigable = 0, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 3 (U5 seguridad cliente, U6 redirect/hyperlink, U7 config cliente — varios solapados con B152/B153/B154, se cerrarán con criterio; posible consolidación)
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked** (hardware/live/NDA): 0
- Iteraciones consecutivas con backlog vacío (secundario): 0/2
- Próximo gap (según prioridad): **U5 · postura de seguridad cliente** (construcción de params `file`/`query`/`config`, el `Client-Username` mutable ya visto, el token Mapbox, y la síntesis de cómo la SPA alimenta la cadena de sinks de B150) sobre `scratchpad/app.beauty.js`. Luego U6 (redirect/hyperlink, mayormente B152) y U7 (config cliente).
- Budget cap: none

## Iteration history

| # | Fecha | Gap cerrado | Bloque | Nuevos gaps |
|---|---|---|---|---|
| 1 | 2026-07-02 | U1 esqueleto `-ux` | B151 | 0 (confirma la cadena de loaders para U2) |
| 2 | 2026-07-02 | U2 loaders JS | B152 | 0 (deja el contrato de globals inject*/destroyApp + router hash para U3) |
| 3 | 2026-07-02 | U3 SPA embarcada | B153 | 0 (§14 corrige B50 Vue 2.7→2.6.14; deja U4/U5 pre-respondidos) |
| 4 | 2026-07-02 | U4 wiring cliente↔backend | B154 | 0 (confirma B143/B144/B145 desde el cliente; mapa endpoint+WS completo) |

## Self-verify

- **B151**: lectura directa completa de las 3 clases (35 líneas c/u) + `module.xml` + `module.palette` (todo
  `file:line`, fuente primaria). `[CERT]` ~22 · `[INFER]` 7 (todos anclados). Ratio `[INFER]/[CERT]` ≈ 0.32.
  Hallazgo notable: el `-ux` es puro registro de vistas (3 `BIJavaScript` view-agents sobre `ReflowService`);
  el gate `"rw"` de `BReflowConfig` es de VISTA (cosmético vs el REST sin gate de B145, patrón B146 §146.5).
- **B152**: lectura directa completa de los 6 archivos JS (65+62+67+71+22+113 líneas, fuente primaria). `[CERT]`
  ~30 · `[INFER]` 9 (todos anclados). Ratio `[INFER]/[CERT]` ≈ 0.30. Hallazgos: SPA en iframe con puente de
  globals `injectBaja`/`injectConfig`/`destroyApp` (hereda sesión Niagara sin re-login); ORD scheme `|reflow:`
  → router hash; Proxy de `niagara.env` en `hyperlink.js` (navegación scopeada a `/nmodsreflow/#`, open-redirect
  bajo); redirect browser→`/nmodsreflow`. Deja el contrato de globals para verificar en U3 (SPA).
- **B153**: barrido delegado (sonnet) + beautify js-beautify de la SPA (2.63 MB → temp) + verificación directa
  de 8 grupos de tokens load-bearing sobre `app.beauty.js`/`vendors.beauty.js` (build stamp `:121900`, globals
  `:121783/121864/121928`, Vue `vendors:7394`, router `vendors:37147`, Client-Username `:14160/87135`, Mapbox
  `:118864`, WS `:4087`, hash `:121825`). `[CERT]` ~28 · `[INFER]` 8 (anclados). Ratio ≈ 0.29. **Corrige B50**
  (Vue 2.7.16 dev-tree → 2.6.14 shipped, §14). Confirma B152 (contrato de globals) y B145 (Client-Username
  mutable). Identidad re-medida en vivo (sha256). Tool js-beautify registrado en INSTALLED-TOOLS.md.
- **B154**: grep dirigido + lectura de ventanas del beautified-temp (fuente primaria 1:1). `[CERT]` ~30
  (endpoints con `file:line` de axios verb, comandos WS, headers) · `[INFER]` 6 (anclados). Ratio ≈ 0.20.
  Mapa endpoint→método→backend completo; confirma B143 (comandos WS), B144 (backups GET `?file=`), B145
  (config POST + `Client-*`). Header nuevo `Client-Migration`.
