# Bloque 204 — El framework bajaux: `Widget` + spandrel (virtual-DOM propio) + el puente rt→web

> Research del focus **`px-editor-deep`** (gap X5): el **framework bajaux** — la base HTML5/JS sobre la que apoyan
> webChart (B199) y los field editors web kitPx (B202); todo widget web es un `bajaux/Widget`. Documenta el framework
> EN SÍ: el lifecycle de `Widget`, el modelo de props/eventos, **spandrel** (el virtual-DOM propio con focus-safety), el
> `WidgetManager`/resolución RequireJS, y el puente rt→web (`BIJavaScriptWidget`/`JsInfo`/servlet/`NiagaraEnv` + la
> receta de bundling donde se enganchan webChart/kitPx). Cierra el hilo bajaux abierto en B194. NO cubre `svgBatik` (X4).
>
> Sources (preservados §5): `sources/decompiled/bajaux/rc/` (20 `.js` legibles, sin `.built.min.js`) +
> `sources/decompiled/bajaux/rt/` (8 `.java`). Barrido delegado (sonnet) 2026-07-06; 6 citas load-bearing token-checked
> literal. Method: lectura READ-ONLY del decompilado/extracted. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
> Tipo: EVIDENCE block. Citas relativas a `bajaux/` (`rc/`=JS ux, `rt/`=Java).
>
> Capa PX (render web / framework). Connects [Block 194] (bajaux sin PxMedia), [Block 199] (webChart JsBuild),
> [Block 202] (BUx* field editors), [Block 192] (widgets por type+agent), [Block 197] (síntesis).

---

## 204.1 — `bajaux/Widget`: el lifecycle template-method sobre jQuery `[CERT]`

`bajaux/Widget` (módulo RequireJS `rc/Widget.js:30`) es la base de TODO widget web. El lifecycle es un conjunto de
métodos públicos "template" que delegan a hooks `do*` overridables (patrón Template Method):

| API pública | Hook | Propósito | file:line |
|---|---|---|---|
| `initialize(dom,params)` | `doInitialize` | bindea el widget a un elemento jQuery (`this.$jq`), construye el HTML. Una sola vez. | `rc/Widget.js:764,870` |
| `layout(params)` | `doLayout` | tras init y en cambios de form-factor/container | `rc/Widget.js:539` |
| `load(value,params)` | `doLoad` | puebla el HTML con un valor; setea `this.$value` | `rc/Widget.js:1314,1387` |
| `read()` | `doRead` | lee la representación UI de vuelta | `rc/Widget.js:1502` |
| `save(params)` | `doSave` | valida (`validate()`=`read()`+validators) y guarda | `rc/Widget.js:1438,1482` |
| `destroy(params)` | `doDestroy` | teardown: vacía DOM, quita `.data('widget')`, desactiva commands | `rc/Widget.js:905,978` |

El elemento se obtiene con `this.jq()` (`rc/Widget.js:988`); la instancia se guarda como jQuery `.data('widget', this)`
(`:803`) y se recupera con el estático `Widget.in(el)` (`:722`). Cada transición dispara un evento DOM jQuery
(`bajaux:initialize`/`load`/`save`…) vía `trigger` (`:1077`). `[CERT]`

**Gotcha — no es ES6 class.** El constructor advierte literal: *"DO NOT convert to ES6 class - this will break
`Widget.apply(this)` pattern everyone uses"* (`rc/Widget.js:120`). Todo el ecosistema (webChart, field editors) subclasea
con `Super.apply(this,...)` prototipal, no `class extends`. Los **form factors** son de primera clase:
`Widget.formfactor = {max, compact, mini}` (`:451`) con clases CSS `bajaux-max/compact/mini` (`:476`) — el mismo
`formFactor` de los field editors de B202. `[CERT]`

## 204.2 — Props, eventos, validators: el modelo de datos del widget `[CERT]`

`Properties.js` (`rc/Properties.js:13`) es un bag key/value TIPADO: cada entry `{name, value, typeSpec, metadata, hidden,
transient, readonly, defaultValue}`. El `typeSpec` se auto-infiere del tipo JS si se omite (`baja:Double/Boolean/String`,
`toBajaTypeSpec`, `:125`) — el puente JS↔Baja. `Properties.extend(...)` mergea con later-wins (`:321`, usado por el patrón
`params`/`defaults` del constructor de Widget). **4.14**: props con key Symbol son auto transient+hidden y excluidas de
`.each()`/serialización (`:362-365`) — Widget usa esto para que un `rootCssClass` default no filtre al property-sheet del
editor PX (`rc/Widget.js:1838-1849`). `[CERT]`

`events.js` es un namespace plano de constantes de nombre de evento jQuery prefijadas `bajaux:` — NO un bus pub/sub, solo
strings disparados por el sistema de eventos nativo de jQuery sobre el elemento raíz. `Validators.js` corre validadores en
secuencia sobre el output de `read()` (throw/reject = falla), fireando `bajaux:valid`/`invalid` sobre el Widget dueño. `[CERT]`

## 204.3 — Spandrel: el virtual-DOM propio de bajaux `[CERT]`

`spandrel.js:19-20` declara su propósito: *"a reasonably pure-functional, diffable method of defining a nested structure
of bajaux Widgets and supporting HTML"* — la respuesta casera de bajaux a React, consciente del árbol de widgets.
`spandrel(arg)` branchea: función → `DynamicSpandrelWidget` (re-renderiza), objeto/array → `SpandrelWidget` (estático). El
DOM se declara como árbol de objetos `{dom, properties, value, kids}` o **JSX** vía `spandrel.jsx` como pragma de Babel
(`jsx.js` NO es un parser JSX propio — es el target del transform estándar de Babel, como `React.createElement`). `[CERT]`

**El diff NO es genérico** (`spandrel/diff.js:91-92`): *"deep-diff had the problem of not being able to handle circular
references… every BajaScript Complex has a circular reference, so diffing a Component as a value locked up the browser"*.
En su lugar hace un **diff shallow por-key conocido** (`configEqualityTesters` para complex/dom/value/properties/
formFactor…, `diff.js:32-87`) + diffs de add/remove sobre `members`. `[CERT]`

**La reconciliación (lo interesante)** — `spandrel/DynamicSpandrelWidget.js`:
- Primer render sin diff (build fresh); renders siguientes → `diffBuildContexts` + `applyDiffs`. Cada `path` mapea a una
  acción: `dom`→patch DOM in-place (`updateElement` si no `requiresRebuild`) o rebuild; `value`→resuelve constructor y
  decide reuse-vs-rebuild. `[CERT]`
- **Gotcha — focus preservation**: un diff de value de un hijo se DECLINA si el widget `isModified()` y `hasFocus`
  (`DynamicSpandrelWidget.js:876`, comentario *"never wipe changes while a user is typing"*) — el reconciler protege las
  ediciones en curso de ser pisadas por un rerender entrante. `[CERT]`
- **Gotcha — stale-render races**: cada render lleva un tick monótono `RENDER_TICKS`; si un hijo se re-renderizó
  independientemente DESPUÉS del tick del padre, el padre declina recargarlo (anti-race, bugs NCCB-54825). `[CERT]`

**Batching** — `DiffQueue.js:26-45`: 5 buckets estrictamente ordenados (before/delete → prework/DOM → work/props →
postwork/rerender-o-rebuild → after/add → finalize); `queueForRebuild` cancela work/postwork. `SpandrelRenderQueue.js`:
`load()` es autoritativo y no-coalescente; `rerender()` coalesce (varios se funden en un ciclo). `[CERT]`

**Veredicto**: spandrel es un reconciler estilo virtual-DOM hecho a medida, hand-tuneado para los objetos Complex con
referencias circulares de BajaScript, focus-safety y lifecycles async — más special-cased que un vDOM React de manual. `[INFER]`

## 204.4 — `WidgetManager` + resolución RequireJS `[CERT]`

`lifecycle/WidgetManager.js:34` orquesta: `buildContext` → `resolveConstructor` → `instantiate` → `initialize` → `load` →
`destroy`. La resolución de constructor (`:127-167`): `params.type` explícito (función o **module-id RequireJS** string,
resuelto async vía `asyncUtils.doRequire`), si no `Registry.resolveFirst(params.value)` (lookup por tipo), si no
`ToStringWidget` default. `makeFor` (solo build) vs `buildFor` (build+init+load). Hooks pre/post init/load. El
`ElementTranslator` es pluggable (`JQueryElementTranslator` default envuelve en `$(dom)`; el comentario anticipa "a React
virtual DOM node" a futuro). Las clases de widget SIEMPRE se direccionan como module-id RequireJS, resueltos lazy vía AMD
`require` — matcheando el mapeo `module://.../X.js` → RequireJS-id del lado servidor (§204.5). `[CERT]`

## 204.5 — El puente rt→web: agente, JsInfo, servlet y `NiagaraEnv` `[CERT]`

`BIJavaScriptWidget` (`rt/BIJavaScriptWidget.java:12`, `interface extends BIJavaScript`) — la interfaz que todo shim de
widget web implementa (webChart B199, BUx* B202). `BIJavaScript` (módulo `web`, no bajaux) extiende `BIAgent` y declara
`JsInfo getJsInfo(Context)`. `forType(type,cx)` resuelve el agente de widget para un Type vía el registro de agentes
(`getAgents(typeInfo).filter(...is(TYPE))`) — el MISMO mecanismo agent-registry de las vistas Niagara. `[CERT]`

`JsInfo` (módulo `web`) envuelve un `BOrd` (`module://.../X.js`) + buildId; `toRequireJsId(ord)` lo convierte a id
RequireJS vía `NiagaraRequireJsMapper` — el puente exacto server↔client de nombres. `BBajauxJsBuild`
(`rt/BBajauxJsBuild.java:12`, singleton `BJsBuild`) declara el bundle `bajaux.built.min.js` + dep `BBajauxCssResource`;
`BJsBuild.getJsInfo` es `final`, así que las subclases (build de webChart, de kitPx) solo aportan su ORD e id — el MISMO
patrón. `[CERT]`

**`WbWebWidgetServlet`** (`rt/WbWebWidgetServlet.java`) sirve la página HTML del widget: resuelve el ORD → `TypeInfo`,
exige `.is(BIJavaScript.TYPE)`, llama `getJsInfo(cx)`, y emite un doc con bootstrap RequireJS + el mount `wbContainer`
(`#bajaux-widget`/`#bajaux-toolbar`/`#bajaux-error`). El boot: `require(['bajaux/container/wb/wbContainer',
<widgetJsId>], function(container, Widget){ container.initialize(new Widget(moduleName, typeName, formFactorTag), params) })`.
`[CERT]`

**Gotcha — cómo llegan las props del servidor al widget JS**: NO por parámetros de constructor, sino por un objeto GLOBAL
de página. `NiagaraEnv.toJavaScript` (`rt/NiagaraEnv.java:105-106`) escribe `window.niagara.env.<key> = <value>`
(`profile`, `themeName`, `type` hx/hxPx/mobile/wb, `user`, `timeZoneId`, + custom), cada valor XSS-encodeado
(OWASP `Encode.forJavaScript`), ANTES de que corra el script del widget. `[CERT]`

**Gotcha — la receta de bundling (dónde se enganchan webChart/kitPx)**: `BWbOrdTargetResolver` sirve el bundle combinado
`/vfile/wb/app.js`; su init estático define el orden de concatenación de TODO el stack web:
`js → web → bajaScript → bajaux → export → converters → webEditors → gx → bajaui → bql → history → webChart → … → kitPx`.
Ahí `webChart.built.min.js` y `kitPx.built.min.js` se sientan JUNTO a `bajaux.built.min.js`, cada uno envuelto en un
`define('<rjs-id>', [], <bundle>)` sintético. Concretamente así "se enchufan" el `BChartWidget` de B199 y los `BUx*` de
B202: son entradas nombradas más en esta concatenación ordenada (salvo en modo WebDev, donde el servlet los fetchea
individualmente). `[CERT]`

## 204.6 — bajaux vs PxMedia/Swing: superficies paralelas (confirma B194) `[CERT]`

Grep repo-wide sobre `bajaux-rt/vineflower` de `javax.baja.gx`/`swing`/`PxMedia`/`BPxView`/`awt.` → **cero matches** en
Java source (el único `awt` es `AWTPermission` para clipboard en `module.xml`, no render). Grep sobre `bajaux-ux/rc/*.js`
de `PxEditor`/`BPxView`/`PxMedia` → **cero**. `[CERT, resultado negativo]`

Esto **confirma y refuerza B194**: bajaux tiene CERO coupling code-level con PxMedia/Swing. El único punto de integración con
el resto de Niagara es el contrato agent-registry (`BIJavaScriptWidget` como `BIAgent` sobre un Type) + la resolución
ORD/servlet — ambos content-agnostic. El render ocurre ENTERAMENTE en el browser (`Widget` + spandrel + jQuery DOM), sin
loop AWT/Graphics2D, sin `BPxView`, sin la maquinaria de vistas Swing del módulo `gx`. Las dos superficies (Swing PxMedia y
bajaux/web) son **paralelas e independientes**, unificadas solo en la capa de Type-registration/URL, no en el render. `[CERT]`

## 204.7 — Connections

- **[Block 194]** (bajaux sin PxMedia): CONFIRMADO con evidencia negativa (grep cero-match, §204.6) — bajaux y PxMedia/Swing
  son superficies de render totalmente paralelas, unidas solo por el registro de agentes por Type.
- **[Block 199]** (webChart) y **[Block 202]** (field editors BUx*): ambos son `BIJavaScriptWidget` shims cuyo `JsBuild`
  (subclase de `BBajauxJsBuild`/`BJsBuild`) se concatena en la receta de bundling de §204.5; sus widgets JS son
  `bajaux/Widget` con lifecycle §204.1 y (webChart) render vía spandrel/D3.
- **[Block 192]** (widgets por type+agent): el puente rt→web (`BIJavaScriptWidget.forType` vía agent-registry) es el mismo
  patrón type+agente; un Type puede tener agente Swing Y agente JS-widget simultáneos.
- **[Block 197]** (síntesis 7 capas): X5 documenta la capa "render web moderno" que la síntesis marcó como boundary.
- **Fuera de scope** (nombrados): `javax.baja.web.js.{BIJavaScript,BJsBuild,JsInfo}` (módulo `web`),
  `NiagaraRequireJsMapper`, `nmodule/webEditors/rc/fe/fe`, jQuery/Promise/Underscore — el backing del framework.
