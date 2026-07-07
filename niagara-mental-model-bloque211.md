# Block 211 — La API base del PX Editor: `javax.baja.px.editor` (root, 7 clases): BPxEditor, BPxSideBar, controller, selección, tool, profile, transfer

> Research de la **INFRAESTRUCTURA de `pxEditor-wb`** — foco `px-editor-core`, gap **C2**. Documenta las 7
> clases del paquete RAÍZ `javax.baja.px.editor`: las **abstracciones base que todo el editor extiende** —
> `BPxEditor` (la vista-herramienta), `BPxSideBar` (base de TODO sidebar), `PxEditorController` (superficie
> input→modelo), `PxEditorSelection` (modelo de selección), `BDrawingTool` (enum de herramienta activa),
> `BPxProfile` (el hook OEM de perfil) y `BIPxTransferWidget` (contrato de widget transferible). El bus de
> eventos de `BPxEditor` NO se re-deriva aquí — está en B210 (remisión). NO cubre factory/WidgetInserter
> (gap C3) ni util (C4).
>
> Sources (decompilado Vineflower, READ-ONLY):
> `/home/cristian/modules/Prototipos/modulos/organized/pxEditor/pxEditor-wb/vineflower/javax/baja/px/editor/`
> (citas `file:line` relativas: `BPxEditor.java:55` = `.../javax/baja/px/editor/BPxEditor.java`). Cruce a
> `workbench-wb/vineflower/.../BWbPxView.java` (superclase, otro módulo).
> Method: lectura directa del decompilado (7 clases root + verificación de la superclase `BWbPxView`).
> Markers (canónico METHODOLOGY §3): `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción.
>
> Capa infraestructura pxEditor-wb (núcleo). Connects [Block 210] (el bus de eventos que `BPxEditor` aloja),
> [Block 191] (BPxEditor como herramienta / load-save), [Block 198] (sidebars que extienden BPxSideBar),
> [Block 194] (perfiles Wb/Hx/Mobile — `BPxProfile` es su hook), [Block 205] (studio/dibujo — consume BDrawingTool).

---

## 211.1 — `BPxEditor`: la vista-herramienta (extiende `BWbPxView`, agent sobre Component/PxFile) `[CERT]`

`BPxEditor` **NO** es un `BWbEditor` directo: extiende `BWbPxView` (que a su vez extiende `BWbView`):

```java
// BPxEditor.java:55
public class BPxEditor extends BWbPxView {
```

`BWbPxView` (módulo `workbench-wb`, `BWbPxView.java:85`) es quien realmente posee los campos
`PxProperty[] pxProperties` / `PxLayer[] pxLayers` y la maquinaria genérica `loadPx`/`loadPxFile`/`PxDecoder`
(`BWbPxView.java:90-91,203,345`). `BPxEditor` los hereda y los puebla vía `setPxProperties`/`setPxLayers`
(`BPxEditor.java:332-338`) — o sea, **el parseo crudo de PX (propiedades/capas) es responsabilidad de la
superclase**, no de esta clase.

| Aspecto | Detalle | Cita |
|---|---|---|
| Registro | agent view: `@AgentOn(types={"baja:Component","file:PxFile"})` — se registra como vista de Workbench para componentes y `.px`, NO como componente con slots propios | `BPxEditor.java:49-54` |
| Slots propios | **ninguno** `@NiagaraProperty` directo | (ausencia verificada) |
| `doLoadValue` | `loadPx()` heredado → construye `BPxEditorPane`, cablea `viewMenus`/`viewToolBar`/`viewStatus` desde `getPxProfile()`, `editorPane.load(cx)`, dispara `PxEditorEvent(0)` (open) por el bus | `BPxEditor.java:82-97` |
| `doSaveValue` | delega en `editorPane.save(value,cx)`, `PxIncludeManager.trimAll()`, dispara `PxEditorEvent(1)` (save) | `BPxEditor.java:99-114` |
| Cloning | ÚNICO uso directo de `new PxEncoder(...).encodeDocument` + `new PxDecoder(...).decodeDocument` — round-trip de un widget para clipboard/duplicar (NO para persistencia) | `BPxEditor.java:271-287` |

**Ownership** (lo que la API base "es dueña"):
- **Controller**: campo directo `private PxEditorController controller = new PxEditorController(this)` + `getController()`/`setController()` (`BPxEditor.java:65,340-346`).
- **Selección**: SIN campo propio — `getSelection()` delega en `editorPane.getSelectedWidgets()` devolviendo un `PxEditorSelection` (`BPxEditor.java:328-330`). El dueño real de la selección es el `BPxEditorPane`; `BPxEditor` es pass-through.
- **Profile**: `private BPxProfile pxProfile` construido lazy en `getPxProfile()` (`BPxEditor.java:311-326`) buscando el agent del `BWbProfile` filtrado a `BPxProfile.TYPE` y construyéndolo reflexivamente con un arg `BPxEditor` — así enchufa un `BPxProfile` OEM/site (ver §211.6 y B194).

El rol de **bus de eventos** (`listeners`, `add/removePxListener`, `firePxEvent`, `BPxEditor.java:63,143-234`)
**se remite a [Block 210]** — sin sustancia nueva aquí.

## 211.2 — `BPxSideBar`: la base de TODO sidebar (extiende `BPane`) `[CERT]`

Base abstracta que extienden los 4 sidebars (tree/propsheet/layersheet/cellsheet de B198). Es un `BPane`
(pane bajaux/workbench), NO un `BWidget` crudo:

```java
// BPxSideBar.java:23
public abstract class BPxSideBar extends BPane
```

**Contrato que da a las subclases**:

| Elemento | Detalle | Cita |
|---|---|---|
| Slot | `@NiagaraProperty content: BWidget` (default `BNullWidget`) + get/set | `BPxSideBar.java:17-34` |
| Abstractos | `abstract BImage getSideBarIcon()` + `abstract String getSideBarDescription()` (pueblan icono/tooltip de la pestaña) | `BPxSideBar.java:44,46` |
| Back-ref editor | ctor `BPxSideBar(BPxEditor editor)` → `getPxEditor()` — cada sidebar recibe la referencia al editor gratis | `BPxSideBar.java:40-42,76-78` |
| Layout template | `computePreferredSize()` dimensiona al content; `doLayout()` estira content al bounds (o zero si null) | `BPxSideBar.java:55-68` |
| Paint template | pinta content + traza borde temado `Theme.toolPane()` | `BPxSideBar.java:70-74` |
| Helper | `protected BAbstractButton newButton(Command)` — botón sin foco/sin estilo para controles internos | `BPxSideBar.java:48-53` |

Confirma en fuente la tesis de B198: los sidebars comparten esta base (icono+descripción+editor+layout),
por eso son intercambiables como pestañas.

## 211.3 — `PxEditorController`: la superficie input→modelo (clase plana, no `BWbController`) `[CERT]`

`public class PxEditorController` — **clase plana** sin `extends`/`implements`, poseída 1:1 por `BPxEditor`
(`private final BPxEditor editor`, ctor `PxEditorController(BPxEditor editor)`, `PxEditorController.java:41,45,48-51`).
Controla **tres superficies**:

1. **Resolución de cell-editor** (para el props sheet): `getCellEditor(BComponent, Property)` — caso especial
   `BBinding` → `makeBindingCE`, `BPxInclude.variables` → `makePxIncludeVariablesCE`, resto → `BWbCellEditor.makeFor` (`PxEditorController.java:53-65`).
2. **Registro de factories / inserción drag-drop**: `List<WidgetFactory> factories` sembrado por
   `getDefaultWidgetFactories()` (8 factories: Label, Picture, WidgetCloning, PxFile, ImageFile, JsFile,
   AliasNavNode, NavNode) + `add/removeWidgetFactory` + `getWidgetInserter(BIPxTransferWidget, BObject[])`
   que busca la factory cuyo `canConvert(objects)` matchea y devuelve su `WidgetInserter`
   (`PxEditorController.java:79-115`). **Esto adelanta el gap C3** (factory/WidgetInserter).
3. **Hooks de interacción transfer-widget**: `getPopupMenu(...)` (delega a `transferWidget.getDefaultPopupMenu`),
   `allowDrop(...)` (siempre `true`), `getDoubleClickCommand(...)` (`new EditPropertiesContext(editor)`)
   (`PxEditorController.java:67-77`).

O sea: eventos de mouse/doble-clic/drop sobre transfer-widgets se enrutan por acá hacia comandos; los payloads
de paste/drag se enrutan por el registro de factories hacia `WidgetInserter`s.

## 211.4 — `PxEditorSelection`: interfaz de selección basada en `BWidget[]` `[CERT]`

Interfaz pura (sin campos), la selección se modela como **array** (no `Set`), con CRUD completo:

```java
// PxEditorSelection.java:5-17
public interface PxEditorSelection {
   BWidget[] getWidgets();
   void setWidgets(BWidget[] var1);   // reemplazo bulk
   void select(BWidget var1);         // add incremental
   void deselect(BWidget var1);       // remove incremental
   void deselectAll();                // clear
   boolean isSelected(BWidget var1);  // membership
}
```

No hay `getSelected()` — la consulta es `getWidgets()`. **Relación con `PxSelectionEvent` (B210)** `[INFER]`:
la interfaz misma NO dispara eventos; la implementación concreta (el objeto de selección del `BPxEditorPane`,
referido como `editorPane.getSelectedWidgets()` en `BPxEditor.java:329,176,194,211`) es presumiblemente quien
llama `select`/`deselect`/`setWidgets` y luego levanta un `PxSelectionEvent` por `firePxEvent` — consistente
con las reacciones `resetHandles()`/`forceRootLayout()`/`doUpdate()` de `firePxEvent` casos 3-6
(`BPxEditor.java:186-213`). Complementa a B210, no lo duplica.

## 211.5 — `BDrawingTool`: la herramienta activa como `BFrozenEnum` (5 estados) `[CERT]`

La abstracción "drawing tool" es un **enum congelado**, NO una interfaz de manejo de mouse:

```java
// BDrawingTool.java:11-14
@NiagaraEnum(range = {@Range("normal"), @Range("addPolygon"), @Range("addPath"),
                      @Range("addPoint"), @Range("deletePoint")})
public final class BDrawingTool extends BFrozenEnum
```

5 estados: `normal`(0, default) · `addPolygon`(1) · `addPath`(2) · `addPoint`(3) · `deletePoint`(4)
(`BDrawingTool.java:15-25`). Factories `make(int)`/`make(String)` (`:28-34`), predicado `isNormal()`==ordinal 0
(`:44-46`). Es un **token de estado** (qué herramienta está activa: selección/normal vs. 4 modos de edición
de polígono/path/punto); el mouse-dispatch real vive en el editor-pane/painter (B205), no en el enum.

## 211.6 — `BPxProfile`: el hook OEM de perfil (`BObject implements BIAgent`) `[CERT]`

`public class BPxProfile extends BObject implements BIAgent`, registrado como agent sobre perfiles de
Workbench: `@AgentOn(types={"workbench:WbProfile"}, requiredPermissions="r")` (`BPxProfile.java:13-19`). Es
exactamente el lookup que hace `BPxEditor.getPxProfile()` (`BPxEditor.java:314`) — confirma el **mecanismo de
hook OEM**: un perfil OEM/media (estilo Wb/Hx/Mobile de B194) registra un agent `BPxProfile`, y `BPxEditor` lo
instancia reflexivamente con `this` como arg.

Superficie de configuración — **template-methods default-passthrough** que una subclase override para
customizar el chrome del editor:

```java
// BPxProfile.java:31-45
public BPxSideBar[] getSideBars(BPxSideBar[] defaultSideBars)              // add/quita/reordena pestañas sidebar
public BMenu[] getViewMenus(BMenu[] defaultMenus)                          // override menús
public BToolBar getViewToolBar(BToolBar defaultToolBar)                    // override toolbar
public BWidget getViewStatusBarSupplement(BWidget defaultSupplement)       // suplemento de status
```

`getViewMenus`/`getViewToolBar`/`getViewStatusBarSupplement` los llama directo `BPxEditor.doLoadValue`
(`BPxEditor.java:90-92`). `getSideBars` lo consume `BPxEditorPane` (no en estas 7 clases). Sin slots — clase
fina, sólo back-ref `getPxEditor()` (`BPxProfile.java:27-29,47-49`). **Este es el punto de extensión por
license/OEM** que B194 nombró para los perfiles.

## 211.7 — `BIPxTransferWidget`: contrato de widget transferible `[CERT]`

Interfaz Baja fina (`extends BInterface`, `@NiagaraType`), un solo método:

```java
// BIPxTransferWidget.java:11-15
public interface BIPxTransferWidget extends BInterface {
   BMenu getDefaultPopupMenu(BMouseEvent var1);
}
```

NO define métodos de drag/drop propios (eso vive en `javax.baja.ui.transfer.TransferContext`/`Transferable`).
Su único aporte es el **contrato del menú contextual** (clic derecho): cualquier wrapper de widget que
participe en transfer/selección del editor debe dar un popup default. Lo consumen
`PxEditorController.getPopupMenu` (delegación directa) y `getDoubleClickCommand`/`allowDrop`
(`PxEditorController.java:67-77`) — es el **tipo-marcador/contrato** contra el que el controller despacha los
widgets bajo manipulación drag-drop/selección en el canvas.

## 211.x — Connections

- **[Block 210]** (bus de eventos) — `BPxEditor` aloja el bus documentado allí; §211.1 remite la parte de
  listeners/fire a B210 y abre el resto (lifecycle, ownership, cloning).
- **[Block 191]** (BPxEditor como herramienta / load-save) — confirma y refina: el load/save real vive en
  `BWbPxView`/`BPxEditorPane`; `BPxEditor` sólo toca `PxEncoder`/`PxDecoder` para clonar widgets (§211.1).
- **[Block 198]** (sidebars) — `BPxSideBar` (§211.2) es la base que aquellos 4 sidebars extienden; cierra el
  "de dónde heredan icono/descripción/editor/layout".
- **[Block 194]** (perfiles Wb/Hx/Mobile) — `BPxProfile` (§211.6) es el hook agent que enchufa esos perfiles;
  aquí se ve el mecanismo reflexivo `@AgentOn(workbench:WbProfile)` que los materializa.
- **[Block 205]** (studio/dibujo) — `BDrawingTool` (§211.5) es el token de estado que la máquina de dibujo
  de B205 consume para decidir el comportamiento del mouse.
- **[C3 factory]** — `PxEditorController` (§211.3) ya expone el registro de 8 `WidgetFactory` + `WidgetInserter`;
  el gap C3 profundizará esas clases.
