# Bloque 202 — Field editors inline (kitPx): paridad Wb-Swing ↔ Ux-web por type+agent, semántica en el binding

> Research del focus **`px-editor-deep`** (gap D5): los 4 **field editors inline** de kitPx — el par Workbench-Swing
> (`BGenericFieldEditor`/`BSetPointFieldEditor`) y sus gemelos bajaux-web (`BUxGenericFieldEditor`/
> `BUxSetPointFieldEditor`). Un field editor deja al usuario RUNTIME editar (no solo mostrar) un valor bound en una
> página PX. Hallazgo arquitectural: NO hay interfaz Java compartida entre las dos superficies — la paridad es por
> **type + per-surface agent** (mismo patrón de B192/B194), y la semántica "setpoint" vive en el BINDING, no en el
> field editor. NO cubre `studio/` (D2) ni `commands/` (D4).
>
> Sources (preservados §5): `sources/decompiled/kitPx-fe/{wb,ux}/` — 6 `.java` + 3 `.js` (Vineflower). Barrido
> delegado (sonnet) 2026-07-06; 6 citas load-bearing token-checked literal. Method: lectura READ-ONLY del decompilado.
> Markers (§3): `[CERT]` `file:line` · `[INFER]`. Tipo: EVIDENCE block. Citas por basename (ver header para el árbol).
>
> Capa PX (edición inline). Connects [Block 192] (widgets bajaui por type+agent), [Block 193] (BSetPointBinding),
> [Block 194] (Wb vs bajaux sin PxMedia), [Block 199] (webChart, mismo shim BSingleton), [Block 198] (cell editors).

---

## 202.1 — `BGenericFieldEditor` (Wb-Swing): shell sobre el framework de field editors `[CERT]`

`BGenericFieldEditor extends BWbFieldEditor` (`BGenericFieldEditor.java:26`) — NO un `BWidget` a secas: es miembro de
primera clase del framework de field editors del Workbench (`javax.baja.workbench.fieldeditor.BWbFieldEditor extends
BWbEditor`). Es un **shell genérico** que hostea el editor concreto correcto para el tipo del valor bound. `[CERT]`

**Resolución de tipo — delega a `BWbFieldEditor.makeFor`** (`BGenericFieldEditor.java:63-71`): en `doLoadValue`, si no
hay inner editor o el tipo cambió, llama `makeFor(value, context)`, lo linkea (`linkTo` propaga los topics
`actionPerformed`/`setModified` del inner al outer) y lo pone como content. `makeFor` resuelve el editor concreto vía el
**framework de agentes Niagara** (`obj.getAgents().filter(FEAgentFilter)` re-filtrado por perfil `BWbProfile`, luego
`agents.getDefault().getInstance()`). El commit (`doSaveValue`, `:77-80`) solo forwardea al inner editor resuelto.
`BGenericFieldEditor` en sí no tiene acoplamiento a binding — lo maneja el `BValueBinding` del widget contenedor. `[CERT]`

## 202.2 — `BSetPointFieldEditor` (Wb-Swing): coopera con `BSetPointBinding` `[CERT]`

`BSetPointFieldEditor extends BGenericFieldEditor` (`BSetPointFieldEditor.java:20`) y agrega EXACTAMENTE una cosa:
cooperación con `BSetPointBinding`. `saveSetPoint()` (`:40-47`) commitea SOLO si el editor fue modificado
(`if (this.isModified())`), delegando la escritura real al binding (`binding.saveSetPoint(value, cx)`). El load coacciona
el tipo (desenvuelve `BIBoolean`/`BINumeric`/`BIEnum`/`BIStatusValue`/`BComponent.out`) para que el editor siempre vea un
valor plano editable + facets (`:49-88`). `[CERT]`

**La escritura vive en el binding** — `BSetPointBinding` (B193) es el driver:
- `save(cx)`: `if (widget instanceof BSetPointFieldEditor) ((BSetPointFieldEditor)widget).saveSetPoint(this, cx)` (`BSetPointBinding.java:117-124`).
- `saveSetPoint(BValue, ctx)` (`:185-207`) intenta **write directo de propiedad** primero (`saveProperty`, `:215-233`) y
  cae a **invocar una acción `"set"`** (`saveAction`, `:235-266`) — dual-path. `[CERT]`
- **Permission gating** `[CERT]`: property-path bloquea slots readonly (`else if (Flags.isReadonly(c, path[0])) return
  false`, `:220`); el gate grueso `isDegraded()` = `!isBound() || !getTarget().canWrite()` (`:108-109`) delega a la ACL
  del ORD-target (`OrdTarget.canWrite()`) — ahí vive el operator-write real, NO en un check kitPx propio. Los bounds
  `min`/`max` se verifican aparte (`verifyBounds`, `:268-296`, lanza `CannotSaveException`).

## 202.3 — `BUxGenericFieldEditor` (bajaux-web): un shim `BSingleton` → JS `[CERT]`

El gemelo web es un **shim `BSingleton` + `BIJavaScriptWidget`**, exactamente el patrón de `BChartWidget` de webChart
(B199) — no renderiza él mismo:

```java
// BUxGenericFieldEditor.java:14-23
@NiagaraType(agent = {@AgentOn(types = {"kitPx:GenericFieldEditor"})})
@NiagaraSingleton
public class BUxGenericFieldEditor extends BSingleton implements BIJavaScriptWidget {
   private static final JsInfo JS_INFO = JsInfo.make(BOrd.make("module://kitPx/rc/fe/GenericFieldEditor.js"), BKitPxJsBuild.TYPE);
```

`@AgentOn(types={"kitPx:GenericFieldEditor"})` registra esta clase Java como el **agente de superficie web** para el MISMO
type Niagara (`kitPx:GenericFieldEditor`) que `BGenericFieldEditor` (Wb) ES. Toda la lógica vive en el JS. `[CERT]`

**Resolución (JS, el análogo real de `makeFor`)** — `GenericFieldEditor.js:189-193` usa el registro de field editors del
propio `nmodule/webEditors/rc/fe/fe` (`fe.getDefaultConstructor(value.getType(), {formFactors:['mini']})`, con fallback
a `PropertySheet` para tipos complejos) — un mecanismo de resolución **genuinamente distinto** del `obj.getAgents()` de
Swing: mismo concepto, registro paralelo. El commit (`doSave`/`doRead`) delega al `.save()`/`.read()` del widget bajaux
interno, sin diálogos/acciones Swing. `[CERT]`

## 202.4 — `BUxSetPointFieldEditor` (bajaux-web): el JS no agrega nada, el write está en el binding `[CERT]`

Mismo shim Java (agente sobre `kitPx:SetPointFieldEditor`, JS `module://kitPx/rc/fe/SetPointFieldEditor.js`,
`BUxSetPointFieldEditor.java:14-23`). **Gotcha**: `SetPointFieldEditor.js` NO agrega comportamiento sobre
`GenericFieldEditor.js` — solo overridea `rootCssClass: 'ux-SetPointFieldEditor'` (`SetPointFieldEditor.js:34,40`,
`_inherits(SetPointFieldEditor, GenericFieldEditor)`). Toda la semántica "setpoint" web vive en el BINDING. `[CERT]`

El write: el agente web de `kitPx:SetPointBinding` es `BSetPointBindingTypeExt` (`BBajaScriptTypeExt`, apunta a
`rc/binding/SetPointBinding.js`, `BSetPointBindingTypeExt.java:14-23`). `SetPointBinding.js.save()` **espeja el dual-path
de Swing**: si `baja:Component` → `saveSetPointAction` (invoca acción `"set"`), si no → `saveSetPointProperty` (write
directo), guardado por `verifyBounds` (min/max). `[CERT]`

**Hazard de permisos (client-side).** `isDegraded()` (`SetPointBinding.js:202-206`) = super || `!target.canWrite()` —
pero NO hay equivalente client-visible del check `Flags.isReadonly(c, path[0])` de Swing: esa enforcement pasa
SERVER-SIDE cuando `comp.set(...)` round-trippea (BajaScript RPC). El cliente **optimistamente permite el intento de
write** y solo el servidor lo rechaza autoritativamente. Es una diferencia de superficie, no un agujero (el server sigue
gateando), pero el modelo de confianza difiere del Wb. `[CERT]` `[INFER: server enforcement]`

## 202.5 — Síntesis: la paridad Wb ↔ Ux es por type+agent, sin interfaz compartida `[CERT]`

**NO hay interfaz/clase abstracta Java que abarque ambos stacks.** La paridad se logra por el patrón **type + agente por
superficie**, el mismo de los widgets bajaui (B192) y el dispatch de media (B194):

- **Un type Niagara por concepto**: `kitPx:GenericFieldEditor`, `kitPx:SetPointFieldEditor`, `kitPx:SetPointBinding`.
- **Superficie Wb**: agente = una clase Java `BWbFieldEditor`/`BValueBinding` real que participa del framework Swing
  (`makeFor`, filtrado `BWbProfile`, modelo diálogo/acción).
- **Superficie Ux**: agente = un shim fino `BSingleton implements BIJavaScriptWidget` cuyo único trabajo es el registro
  `@AgentOn` + `JsInfo` a un módulo JS bajaux. Toda la resolución (`fe.getDefaultConstructor`), render y commit
  (`.save()`/`.read()`) se reimplementa independiente en JS — confirma B194: las superficies bajaux NO rutean por
  `PxMedia`/Wb, son una implementación paralela keyed por el mismo type name. `[CERT]`
- Ambas superficies **duplican independientemente** las mismas dos estrategias de write (property directo vs acción
  `"set"`) y la misma forma de permission gate (`canWrite()`/`Flags.isReadonly` en Wb; solo `canWrite()` client-side en
  Ux) — evidencia fuerte de que el concepto se diseñó una vez y se hand-porteó por superficie, consistente con la
  convención kitPx "clase Java por widget Wb + clase JS paralela por widget Ux" ya vista en webChart (B199) y B192. `[CERT]`
- En AMBOS stacks `SetPointFieldEditor` es una subclase delgada sin lógica de editor-content: Swing agrega solo métodos
  de cooperación con el binding (`saveSetPoint`/`loadSetPoint`); el JS agrega solo una clase CSS. La semántica setpoint
  real (write, gating degraded/readonly) vive en el **binding** (`BSetPointBinding` vs `SetPointBinding.js`), no en el
  field editor — **el punto arquitectural load-bearing del bloque**. `[CERT]`

**Tercer actor (footnote)**: `BSetPointEditor` (`kitPx.ux.fe.BSetPointEditor` → `SetPointEditor.js`, NO
`SetPointFieldEditor.js`) es un singleton `BIFormFactorMini`+`BIOffline` separado cuyo doc dice "only works with
`HxSetPointFE`" — el editor mini para contextos Hx/tabla, distinto del par PX-hosted. `[INFER]`

## 202.6 — Connections

- **[Block 192]** (widgets bajaui por type+agent): la paridad Wb↔Ux de los field editors es el MISMO patrón type+agente
  que el catálogo bajaui — un type, dos agentes de superficie (§202.5).
- **[Block 193]** (BSetPointBinding): confirmado que la semántica setpoint (dual-path write property/acción, permission
  gate) vive en `BSetPointBinding`, no en el field editor; el FE solo coopera (`saveSetPoint` if-modified, §202.2).
- **[Block 194]** (Wb vs bajaux sin PxMedia): las superficies Ux (`BUx*`) no rutean por PxMedia — shim `BSingleton`→JS,
  implementación paralela (§202.3/§202.5), exactamente lo que B194 anticipó.
- **[Block 199]** (webChart): mismo shim `BSingleton implements BIJavaScriptWidget` + `JsInfo`→módulo JS que
  `BChartWidget`; los field editors web y webChart comparten el patrón de puente bajaux.
- **[Block 198]** (cell editors): los CE del cell-sheet (`BConverterCE` etc.) también hostean `BWbFieldEditor.makeFor` —
  el mismo framework de field editors que `BGenericFieldEditor` usa aquí, en otro contexto (edición en el editor vs runtime).
- **Fuera de scope** (nombrados): `javax.baja.workbench.fieldeditor.BWbFieldEditor`, `nmodule/webEditors/rc/fe/fe`
  (registro JS), `BKitPxJsBuild`, `BBajaScriptTypeExt` — framework backing de ambas superficies.
