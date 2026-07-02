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

- **Covered blocks (este focus)**: 2 — B151 (esqueleto `-ux`), B152 (cadena de loaders JS: widgets bajaux → iframe → SPA).
- **Coverage metric**: 2 / 7 gaps cerrados.
- **Last iteration**: 2026-07-02 — U2 cerrado (loaders JS): los 3 widgets bajaux montan la SPA **dentro de un
  iframe** (`src='/nmodsreflow/#'+$reflowPath`, el path del BaseServlet B138/B149) y la puentean vía globals
  `injectBaja`/`injectConfig`(+`destroyApp`) — así la SPA hereda la sesión Niagara autenticada sin re-login. El
  ORD scheme `|reflow:` (resolver.js) mapea a rutas hash del Vue router. `hyperlink.js` parcha `niagara.env` con
  un Proxy para interceptar navegación (`unescape(path)`→`location.href`, pero scopeado a `/nmodsreflow/#`, bajo
  riesgo). Browser (no-wb) se redirige a `/nmodsreflow`; Workbench usa el iframe.

## Gap-backlog (priorizado)

| Prioridad | Gap | Tipo/fuente | Estado |
|---|---|---|---|
| — | U1 · esqueleto `-ux`: `BReflow`/`BReflowConfig`/`BReflowRedirect` (tipos BComponent/profile, registro de vista) + `module.palette` + `module.xml` | Java `-ux` | **cerrado B151** |
| — | U2 · cadena de loaders JS: `reflow.js`/`reflow_config.js`/`reflow_redirect.js` + `lib/{loader,resolver,hyperlink}.js` — cómo BajaScript bootstrapea y monta la SPA | JS `-ux` | **cerrado B152** |
| high | U3 · SPA embarcada `.77`: identidad/build de `app.4509efb4.js` (minificada) + `chunk-vendors` — framework (Vue 2.7 per B50), diff forense frontend 1.7.5↔1.7.7 vs B51 | JS `-rt/rc` (minificado → beautifier) | pending |
| medium | U4 · wiring frontend↔backend: cómo la SPA llama al REST/WS (cara cliente del contrato B149 + canal WS B140) — capa fetch/axios, endpoints, headers | JS SPA + cross-ref B149/B140 | pending |
| medium | U5 · postura de seguridad cliente: ¿la SPA envía los headers `Client-Username`/`Client-Id` (audit forjable B145)?; cómo arma los params `file`/`query`/`config` (cara cliente de B142/B144/B145/B147); secretos/tokens en el bundle; interplay CSP (B149) | JS SPA + cross-ref B145/B147/B149 | pending |
| low | U6 · redirect/hyperlink: `BReflowRedirect` + `reflow_redirect.js` + `lib/hyperlink.js` — deep-linking / navegación ORD (posible superficie de open-redirect) | Java+JS `-ux` | pending |
| low | U7 · config cliente: `BReflowConfig` + `reflow_config.js` — contrato de config del lado cliente (cross-ref config.json B143/B145 + B51) | Java+JS `-ux` | pending |

## Blocked gaps (con lo que necesitan)

- **U3/U4/U5 (parcial)** — la SPA `.77` (`app.4509efb4.js`) está **minificada a 1 línea**. Legible pero
  necesita un **beautifier JS** (js-beautify/prettier) para rigor `file:line`; a provisionar (§10) en la
  iteración que la ataque. NO bloquea el focus (U1/U2/U6/U7 son directamente legibles); sólo condiciona U3-U5.

## Stop control (primario = read-only-investigable = 0, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 5 (U3-U7; U3-U5 condicionados a beautifier, provisionable)
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked** (hardware/live/NDA): 0
- Iteraciones consecutivas con backlog vacío (secundario): 0/2
- Próximo gap (según prioridad): **U3 · SPA embarcada `.77`** (`app.4509efb4.js` minificada + `chunk-vendors`). REQUIERE provisionar un beautifier JS (js-beautify/prettier vía install-tool.sh o npx) antes de citar `file:line`. Contrato a verificar (de B152): globals `injectBaja`/`injectConfig`/`destroyApp` + router hash.
- Budget cap: none

## Iteration history

| # | Fecha | Gap cerrado | Bloque | Nuevos gaps |
|---|---|---|---|---|
| 1 | 2026-07-02 | U1 esqueleto `-ux` | B151 | 0 (confirma la cadena de loaders para U2) |
| 2 | 2026-07-02 | U2 loaders JS | B152 | 0 (deja el contrato de globals inject*/destroyApp + router hash para U3) |

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
