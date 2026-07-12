# Block 235 — Reflow: el sistema de vistas (Workbench, navegador, perfiles)

> **Qué documenta.** Cómo Reflow registra sus vistas y cómo la MISMA app SPA corre en Workbench, en el navegador,
> y en distintos perfiles Niagara. Gap BG23 (reapertura grupo D, pedido del usuario). Profundiza B151/B152
> (registro + loaders) con el mecanismo completo Workbench-vs-navegador.
>
> **Alcance.** El registro de vistas, el dispatch Niagara, los dos mount paths (WB/browser), el bridge BajaScript y
> los entry points. Los loaders JS en detalle están en B152; el subsistema PX (contraste) en B194.
>
> **Fuentes (primarias).** Java `-ux`: `com/niagaramods/nmodsreflow/ux/BReflow{,Config,Redirect}.java` +
> `META-INF/module.xml`. `web-rt` `javax/baja/web/{BIFormFactorMax,OrdServlet}.java`. Loaders
> `nmodsreflow77-ux/vineflower/niagara/{reflow.js,reflow_config.js,reflow_redirect.js,lib/loader.js,lib/hyperlink.js}`.
> `-rt` `rc/index.html`, `http/BaseServlet.java`. SPA beautificada (`BF:`). Barrido delegado (sonnet).
>
> **Método / markers.** `[CERT]` = fuente primaria. `[INFER]` = deducción.

---

## 235.1 — Las 3 vistas: un patrón idéntico `[CERT]`

Las 3 vistas se registran como `BSingleton` agents sobre `nmodsreflow:ReflowService`, implementando **`BIJavaScript`
+ `BIFormFactorMax`** (`BReflow.java:10-12`), cada una con un `JsInfo` que apunta a su loader:

| Vista | Permiso | Loader (`JsInfo.make`) | Rol |
|---|---|---|---|
| `BReflow` | `"r"` | `module://nmodsreflow/niagara/reflow.js` | dashboard viewer |
| `BReflowConfig` | `"rw"` (`BReflowConfig.java:17`) | `…/reflow_config.js` (`:23`) | editor |
| `BReflowRedirect` | `"r"` | `…/reflow_redirect.js` | redirect/deep-link |

El módulo `-ux` es `runtimeProfile="ux"` (host UX genérico de Niagara), NO un split PX por-perfil.

## 235.2 — `BIFormFactorMax`: "renderizo en todos lados" (vs el gating de PX) `[CERT]`/`[INFER]`

`BIFormFactorMax` (`web-rt` `javax/baja/web/BIFormFactorMax.java`) es una **interfaz marcadora** (extends
`BIFormFactor`, sin métodos). Su consumidor es `OrdServlet` (`com/tridium/web/servlets/OrdServlet.java:141` per el
barrido): cuando el tipo de una vista `is(BIFormFactorMax) && is(BIJavaScript)`, el servlet la rutea por
`WbServlet.serviceView(...)` — el dispatch genérico de vista JS-hosteada, sin restricción de form-factor.

**Diferencia arquitectónica con PX (B194)** `[INFER]`: PX gatea POR PERFIL con clases/media distintas (Wb permisivo,
Hx agent-gated, Mobile whitelist). Reflow usa **UNA clase por rol** marcada "max form factor" (= "renderizo en
cualquier form-factor") y hace TODO el branching Workbench/browser/mobile **dentro del bundle JS**, no por selección
declarativa de view-agent Niagara. Es un enfoque más "SPA-first": Niagara solo entrega el bundle, el bundle decide.

## 235.3 — Workbench vs navegador: la misma SPA, dos caminos de montaje `[CERT]`

Ambos convergen en la MISMA app servida en `/nmodsreflow/`:

- **Workbench**: `reflow.js`/`reflow_config.js` → `lib/loader.js:13` (`mount`) arma un `<iframe>` con
  `src = '/nmodsreflow/#' + widget.$reflowPath` (`loader.js:15,25`) — es decir, incluso en Workbench se navega al
  MISMO servlet SPA. En `iframe.onload` llama `iframeWindow.injectBaja(this.fromWorkbench, widget)` (`loader.js:20`).
- **Navegador**: navegación directa a `/nmodsreflow/` carga `index.html`, cuyo bootstrap hace
  `window.niagara = window.parent.niagara || {}` (`index.html:12`); sin parent, `niagara.env` queda undefined, así
  que el bundle se auto-arranca: `window.onload = () => (null==niagara.env || isConfig) && injectBaja()`
  (`BF:121930`) — el mismo `injectBaja`, pero llamado por la app misma sin args.

**La diferencia** `[CERT]`: Workbench envuelve la SPA en un iframe dentro de un `Widget` bajaux e **inyecta la
sesión desde el host** (`injectBaja(fromWorkbench=true, widget)`); el navegador corre la SPA top-level y
**auto-inyecta su propia sesión baja** (`injectBaja()` sin args). El código de la SPA es idéntico.

## 235.4 — Los 3 roles y sus divs `[CERT]`

`index.html` contiene AMBOS divs de montaje (`#nmods-app` + `#nmods-config`, `index.html:24-25`) y carga el bundle
una sola vez; cuál se puebla lo decide QUÉ función JS se invoca, no el ruteo de URL:

- `BReflow` → `loader.mount` → `injectBaja` → monta la raíz Vue en `#nmods-app`, `SET_IS_CONFIG(false)` (`BF:121843`).
- `BReflowConfig` → `loader.mountConfig` → `injectConfig` → monta en `#nmods-config`, `SET_IS_CONFIG(true)`
  (`BF:121909`) — **este es el mount `isConfig` del editor de B223 §223.1** `[CERT]`.
- `BReflowRedirect` → como `Reflow` pero redirige INCONDICIONALMENTE al navegador a `/nmodsreflow`
  (`reflow_redirect.js`), vs el redirect CONDICIONAL de `BReflow` (chequea `getRedirectReflowView()`,
  `BReflowService.java:332`).

## 235.5 — El bridge BajaScript `[CERT]`

`injectBaja`/`injectConfig` (`BF:121783-121928`) hacen `require(['baja!', …, 'nmodule/webEditors/rc/servlets/views'])`
y luego `Ord.make("service:nmodsreflow:ReflowService").get({lease:true})` para obtener una referencia baja VIVA y
leaseada, la guardan como `Vue.prototype.$baja`/`$component`, traen `$bajaUserRoles` vía
`ReflowUserCommands.getRoles`, hacen **monkey-patch de `window.niagara.env.hyperlink`** para interceptar los ORDs
`|reflow:` y mandarlos al Vue Router propio o recargar el iframe, y finalmente montan la app Vue. `destroyApp` =
`vueApp.$destroy()`. Del lado host (Workbench), `lib/hyperlink.js` hace el trabajo espejo: envuelve
`niagara.env.hyperlink` en un Proxy para que un link `|reflow:` clickeado en la UI de Workbench se redirija al
iframe de Reflow en vez de abrir una vista WB nueva (cross-ref B152). Este bridge es lo que da a la SPA la sesión
baja en vivo que usa para suscribirse a puntos (B229 `BoundLabel`, B217).

## 235.6 — Entry points y perfil `[CERT]`/`[INFER]`

- **`index.html`** (`rc/index.html`) es el ÚNICO entry real: preload de `require.js`/`config.js`, carga
  `chunk-vendors.js` + `app.4509efb4.js`, y los dos divs de montaje. Todas las rutas no-matcheadas caen en
  `FileResponse.serve("/index.html")` (`BaseServlet.java:60,252`) — catch-all de SPA (client-side router).
- **`config.html`** es vestigial `[INFER]`: `BaseServlet.doGet` intercepta el path literal `/config` y lo sirve como
  JSON REST (`ConfigResponse.serve`, `BaseServlet.java:63-67`) ANTES de resolver el archivo estático, así que el
  stub `config.html` nunca se alcanza por el router.
- **Perfil/mobile**: NO hay clases per-profile PX-style; toda la adaptación mobile es **client-side** (el bundle
  commitea `documentData/SET_WIDTH`/`HEIGHT` en `onresize`, `BF:57804`; los menús chequean `isMobile` en cliente).
  El `documentData.height<=420` es un toggle de fullscreen del preview de floorplan, no un gate de media Niagara
  `[CERT]` (corrige la hipótesis de un "path mobile" a nivel plataforma).

## 235.7 — Conexiones

- **[Block 151]/[Block 152]** — el registro de las 3 vistas y los loaders; §235 agrega el mecanismo
  `BIFormFactorMax` + el mount path Workbench-vs-navegador completo.
- **[Block 223]** §223.1 — el mount `#nmods-config` (isConfig) del editor = `BReflowConfig`/`injectConfig` (§235.4).
- **[Block 194]** (px-editor) — el CONTRASTE: PX gatea por perfil/media con clases distintas; Reflow usa una clase
  "form-factor max" y decide en el JS.
- **[Block 234]** — el `ordScheme reflow:` (registrado en el `-rt`) es lo que el bridge intercepta (`|reflow:`).
- **Cierre grupo D**: BG22 (módulos) + BG23 (vistas) completos. Sigue grupo E (6 gaps de producto). Grupo C
  (dinámico) queda pendiente de decisión del usuario.
