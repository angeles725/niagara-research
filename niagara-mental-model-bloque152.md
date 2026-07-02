# Block 152 — nmodsreflow.77 (`-ux`): cadena de loaders JS (widgets bajaux → iframe → SPA, ORD scheme `|reflow:`, proxy de hyperlink)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), los 6 archivos JS del módulo `-ux`**: cómo los 3
> view-agents de B151 arrancan y montan la SPA en el browser/Workbench. Cubre `reflow.js`,
> `reflow_config.js`, `reflow_redirect.js` (los 3 widgets bajaux) y `lib/loader.js`, `lib/resolver.js`,
> `lib/hyperlink.js` (la mecánica). NO cubre la SPA embarcada en sí (U3, minificada) ni el wiring HTTP interno
> de la SPA (U4).
>
> Focus: **nmodsreflow-ux** (capa cliente `-ux`). Cierra el gap **U2**. Corpus language: Spanish (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `JS/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-ux/vineflower/niagara`
> (`reflow.js`, `reflow_config.js`, `reflow_redirect.js`, `lib/loader.js`, `lib/resolver.js`, `lib/hyperlink.js`).
>
> Método: lectura directa completa de los 6 archivos (65+62+67+71+22+113 líneas). Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción anclada a líneas `[CERT]`.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 151] (los 3 `BIJavaScript` que apuntan a estos loaders),
> [Block 138]/[Block 149] (la SPA se sirve desde `/nmodsreflow/`, el path del `BaseServlet`), [Block 145] (el
> puente `injectBaja` es cómo la SPA obtiene su sesión — cara cliente del contexto que B145 vio en headers),
> [Block 51] (v1.7.5 para diff frontend).

---

## 152.1 — Los 3 widgets bajaux: iframe + puente `inject*` `[CERT]`

Los 3 loaders son módulos AMD `define([...])` que devuelven un **Widget bajaux** (`extends bajaux/Widget`)
`[CERT]` `JS/reflow.js:1-20,34-42`. Dependen todos de: `baja!`, `subscriberMixIn`, `bajaux/Widget`,
`lib/loader`, `lib/resolver`, una plantilla Handlebars `hbs!.../lib/widget`, y
`/module/web/rc/util/activityMonitor.js` (el monitor de actividad del módulo web de Tridium) `[CERT]`
`reflow.js:1-9`.

Ciclo de vida común `[CERT]` (ej. `reflow.js:47-63`):
- `doInitialize(dom)` → renderiza la plantilla Handlebars que contiene un **`<iframe>`** (`iframeIdentifier`)
  `[CERT]` `reflow.js:22,48-51`.
- `doLoad(component)` → `mountReflowWidget` → `reflowLoader.mount(iframeIdentifier, widget, component)`
  `[CERT]` `reflow.js:32,57-58`.
- `doDestroy()` → `reflowLoader.destroy(iframeIdentifier)` `[CERT]` `reflow.js:61-63`.
- `prototype.resolve = reflowResolver.resolve` (resolver custom del ORD scheme, §152.3) `[CERT]` `reflow.js:45`.

| Loader | iframe id | monta vía | global que llama en la SPA | Cita |
|---|---|---|---|---|
| `reflow.js` | `nmods_iframe` | `loader.mount` | `injectBaja` | `reflow.js:22,32` · `loader.js:19-20` |
| `reflow_config.js` | `nmods_config_iframe` | `loader.mountConfig` | `injectConfig` | `reflow_config.js:22,28` · `loader.js:33-34` |
| `reflow_redirect.js` | `nmods_iframe` | `loader.mount` | `injectBaja` | `reflow_redirect.js:22,34` |

`[INFER]` La SPA NO se ejecuta en el mismo documento del widget: corre **dentro de un iframe**, y el host
bajaux le inyecta el puente Baja llamando a un global (`injectBaja`/`injectConfig`) que la SPA (U3) debe
exponer. Es un patrón host↔iframe con contrato de globals.

## 152.2 — `loader.js`: cómo se carga el iframe y el puente de sesión `[CERT]`

`mount(iframeId, widget, component)` `[CERT]` `JS/loader.js:13-26`:
1. `hyperlink.patch(iframeId)` (§152.4).
2. `src = '/nmodsreflow/#' + widget.$reflowPath` `[CERT]` `loader.js:15` — la SPA se carga desde
   **`/nmodsreflow/`** (el path del `BaseServlet`, B138/B149) con la ruta en el **hash** (`#...`).
3. En `iframe.onload`, si la SPA expuso `injectBaja`, llama `iframeWindow.window.injectBaja(fromWorkbench, widget)`
   `[CERT]` `loader.js:18-23`.

`mountConfig` es idéntico pero llama `injectConfig` `[CERT]` `loader.js:33-34`. `[INFER]` **El puente
`injectBaja(fromWorkbench, widget)` es cómo la SPA obtiene su contexto de sesión Baja**: el `widget` bajaux
lleva la sesión Niagara ya autenticada del Workbench, y se la pasa al Vue app adentro del iframe — la SPA no
re-autentica. (Cara cliente del contexto de sesión que B140/B145 vieron del lado servidor; los headers
`Client-*` de B145 los arma la SPA, U4/U5, no estos loaders.)

Detalles `[CERT]`: timeout de 3 min que muestra un mensaje de error de Workbench si la carga se cuelga
(`loader.js:11,41-51`); `cleanup()` oculta el spinner (`loader.js:53-60`); `destroy()` llama
`iframeWindow.window.destroyApp()` en la SPA (`loader.js:62-68`). `[INFER]` `injectBaja`/`injectConfig`/
`destroyApp` forman el **contrato de globals** host↔SPA (a verificar en U3).

## 152.3 — `resolver.js`: el ORD scheme `|reflow:` `[CERT]`

El resolver custom parsea el ORD Niagara para extraer la ruta de la SPA `[CERT]` `JS/resolver.js:7-19`:

```
const parts = data.split('|reflow:');            // :9
if (parts.length > 1) {
  if (parts[1].indexOf('|view') !== -1) this.$reflowPath = parts[1].split('|view')[0];  // :11-13
  else this.$reflowPath = parts[1];              // :16
}
```

`[INFER]` El ORD `station:|slot:/…|reflow:<path>` mapea `<path>` a `this.$reflowPath`, que `loader.mount` usa
como hash del iframe (`/nmodsreflow/#<path>`, §152.2). Es decir: el **ORD scheme `|reflow:` es el puente entre
la navegación Niagara y el router (hash) de la SPA Vue**. Default `$reflowPath = '/'` `[CERT]` `resolver.js:8`.

## 152.4 — `hyperlink.js`: proxy de `niagara.env` para interceptar navegación `[CERT]`

`patch(iframeId)` envuelve `window.niagara.env` en un **`Proxy`** `[CERT]` `JS/hyperlink.js:108-110` que
intercepta la función `hyperlink` `[CERT]` `hyperlink.js:42-44`. Comportamiento del interceptor:

- ORD especial `'RestrictConfig'` → reescribe a `station:|slot:|reflow:` `[CERT]` `hyperlink.js:47-52`.
- Cualquier ORD con `|reflow:` → `decodeURI(ordStr)`, split por `|reflow:`, y navega el iframe:
  `iframeWindow.window.location.href = '/nmodsreflow/#' + unescape(path)`, luego dispatch de un evento
  `reflow-iframe-hyperlink` `[CERT]` `hyperlink.js:55-66`.
- ORDs con `targetGuid` → resuelve vía `niagara.env.toHyperlink(ordStr)` y fuerza `fullScreen=true` en links
  `/ord` `[CERT]` `hyperlink.js:71-93`.

`[INFER]` **Nota de seguridad (client-side, acotada):** el path del ORD se `unescape`-a y se asigna directo a
`location.href` (`hyperlink.js:64`), pero **scopeado al hash de `/nmodsreflow/`** — es routing client-side del
Vue router, no un request servidor ni un redirect a origen arbitrario, así que el riesgo de open-redirect es
bajo (el prefijo `/nmodsreflow/#` es fijo). Se cruza con U6 (redirect/hyperlink). El `clone()` de la función
original (`hyperlink.js:1-16,34`) permite `destroy()` restaurar el `niagara.env.hyperlink` nativo
(`hyperlink.js:21-27`).

## 152.5 — Workbench vs browser: el redirect `[CERT]`

`fromWorkbench = window.niagara.env.type === 'wb'` `[CERT]` `reflow.js:23`. Divergencia clave:

- **Workbench**: la SPA se monta **dentro del iframe** embebido en el widget bajaux (§152.1-152.2).
- **Browser (no-wb)**: se redirige a `window.top.location.href = '/nmodsreflow'` `[CERT]`. `reflow_redirect.js`
  redirige **siempre** (`reflow_redirect.js:28-29`); `reflow.js` sólo si `component.getRedirectReflowView()`
  `[CERT]` `reflow.js:27-28`. `reflow_config.js` no redirige (config es sólo Workbench) `[INFER]`.

`[INFER]` O sea: el mismo SPA sirve dos modos — embebido en Workbench (iframe + puente inject), o standalone en
browser vía el servlet `/nmodsreflow` (B138/B149). El `getRedirectReflowView()` es una prop del `ReflowService`
(backend) que decide el comportamiento en browser.

## 152.6 — Connections

- **[Block 151]** — los 3 `BIJavaScript` view-agents (`BReflow`/`BReflowConfig`/`BReflowRedirect`) apuntan a
  estos `reflow*.js` vía `module://`; este bloque documenta qué hacen esos loaders.
- **[Block 138]/[Block 149]** — la SPA se carga desde `/nmodsreflow/` (el path del `BaseServlet`); en browser
  el redirect va a ese mismo servlet. El iframe consume el mismo SPA que `FileResponse` sirve desde `rc/`.
- **[Block 140]/[Block 145]** — `injectBaja(fromWorkbench, widget)` es cómo la SPA hereda la sesión Niagara
  autenticada; los headers `Client-*` (B145) los arma la SPA con ese contexto (U4/U5), no estos loaders.
- **[Block 51]** — auditó el frontend v1.7.5; acá está la cadena de arranque `.77` con rigor `file:line`.

`[INFER]` Próximo gap: U3 — la SPA embarcada `.77` (`app.4509efb4.js`, minificada). Requiere provisionar un
beautifier JS (§10) para verificar el contrato de globals `injectBaja`/`injectConfig`/`destroyApp` y el router
hash que este bloque dejó como contrato.
