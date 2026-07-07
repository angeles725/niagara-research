# Bloque 198 — `sidebars/`: el cell-sheet, los cell editors y el árbol de widgets (corazón operativo del editor)

> Research del focus **`px-editor-deep`** (gap D1, PRIMERO): el paquete `pxEditor-wb/.../editor/sidebars/`
> (66 clases) — el property/binding editor REAL donde el usuario edita un widget. B191 nombró el editor como
> herramienta (canvas, paleta→wizard); ESTE bloque abre el panel lateral: cómo se presentan las propiedades y
> bindings de un widget seleccionado, cómo cada celda resuelve su editor, cómo un cambio se vuelve undo/redo, y
> cómo el árbol de widgets sincroniza selección con el canvas. NO cubre el `studio/` (D2) ni el `make/` (D3).
>
> Sources (preservados §5): `sources/decompiled/pxEditor-wb/sidebars/` — árbol completo de 66 `.java` (Vineflower,
> módulo `pxEditor-wb`): `cellsheet/` (+`celleditors/`, +`commands/`), `tree/`, `propsheet/`, `layersheet/`,
> `binding/`. Barrido delegado (sonnet) 2026-07-06; token-check de 4 citas load-bearing verificado literal.
> Method: lectura READ-ONLY del decompilado. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
> Tipo: EVIDENCE block (decompilación). Citas relativas a `sidebars/`.
>
> Capa PX (herramienta / edición inline). Connects [Block 191] (editor como herramienta, undo=Workbench Command),
> [Block 192] (catálogo widgets bajaui), [Block 193] (los 9 bindings kitPx), [Block 197] (síntesis 7 capas).

---

## 198.1 — `BPxCellSheet`: el panel de propiedades + bindings de un widget `[CERT]`

`BPxCellSheet extends BPxSideBar implements PxListener` (`sidebars/cellsheet/BPxCellSheet.java:97`). `BPxSideBar`
(de `javax.baja.px.editor`) es la base dockable que TODO sidebar de este paquete extiende (`BPxTreePane`,
`BPxPropSheet`, `BPxLayerSheet`, `BBoundOrds` — §198.4/§198.5/§198.6). Su contenido es un `BEdgePane` con un
toolbar arriba (label + comandos Add-Binding/Alphabetize/Categorize) y un `BScrollPane` al centro
(`BPxCellSheet.java:107,159-162`). `[CERT]`

**Pipeline de presentación** (`editWidgets()`→`load()`, `BPxCellSheet.java:463-528`): al seleccionar widget(s),

- **1 widget** → `widget = allWidgets[0]`; bindea el label al nombre y saca `bindings = widget.getChildren(BBinding.class)` (`:511-517`).
- **N widgets** → construye una instancia sintética de la clase base común (`baseClassInstance()`, `:485-492`,
  vía `AbstractStubGen.getConcreteClass`) SOLO para enumerar las propiedades compartidas. `[CERT]`
- Luego arma `getAlphaScrollContents` (una `BPxCellTable` plana, `:530-543`) **o** `getCategoryScrollContents`
  (una `BPxCellTable` por `Type` declarante, agrupadas en un `TreeMap<Type,List<Property>>`, `:545-590`) — según
  el toggle alfabético/categorizado. Cada tabla va envuelta en un `BCellSheetExpandablePane` y apilada en un
  **`BStackedPane`** (`:534,570`). `[CERT]`
- `loadBindings()` (`:592-608`) agrega, tras un `BSeparator`, una `BPxBindingTable`+`BBindingExpandablePane` por
  cada `BBinding` vivo. `[CERT]`

**Las piezas** (jerarquías `[CERT]`, relación `[INFER]` sobre esos constructores):

| Clase | Rol | file:line |
|---|---|---|
| `BStackedPane extends BPane` | contenedor tonto: apila `BCellSheetExpandablePane`/`BSeparator` y pinta líneas divisorias; NO conoce el cell-sheet | `cellsheet/BStackedPane.java:15,58-84` |
| `BCellSheetExpandablePane` | disclosure genérico (`summary`/`expansion` + topic `expanderEvent`); reusado para "un grupo de props" Y "un binding" | `cellsheet/BCellSheetExpandablePane.java:44` |
| `BBindingExpandablePane extends BCellSheetExpandablePane` | añade botón "X" que invoca `new DeleteBinding(...).invoke()` al click | `cellsheet/BBindingExpandablePane.java:18,105` |
| `BPxCellTable extends BTable implements CellEditorContainer` | la grilla real de 2 columnas (nombre/editor) de las props de un widget | `cellsheet/BPxCellTable.java:33` |
| `BPxBindingTable extends BPxCellTable` | la MISMA grilla pero scoped a las props de UN `BBinding` | `cellsheet/BPxBindingTable.java:27` |

**Gotcha — cross-deselección entre tablas.** `BPxCellTable.doHandleSelectionModified()` (`BPxCellTable.java:67-79`)
fuerza deselect en TODAS las otras tablas hermanas cuando una recibe selección, usando un guard de reentrancia
`ignoreModified`. Así solo UN cell editor está "activo" a la vez a través de todas las tablas apiladas. `[CERT]`

**Filtro de propiedades** (`properties()`, `BPxCellSheet.java:628-644`): una prop se muestra solo si no es dynamic
(salvo var de "PxInclude" o web-property), no es `Flags.isHidden`, no es en sí un `BWidget` (los widgets anidados
se dibujan en el canvas, no en la hoja), y —para valores `BLayout`— solo si `context.allowLayoutEdit()` lo permite. `[CERT]`

## 198.2 — Los cell editors: un branch de 3 vías, NO un contrato "CE" `[CERT]`

La selección del editor de celda vive en `BPxCellSheet.newCellEditor()` (`BPxCellSheet.java:723-733`), y es un
**branch de 3 vías** (verificado literal):

```java
BConverter conv = Reflector.converter(prop.getName(), bindings);
if (conv == null) {
   PxProperty[] groups = this.context.getPxPropertyComponents().getPxProperties(this.allWidgets, prop.getName());
   return groups.length == 0 ? this.editor.getController().getCellEditor(this.widget, prop) : new BPxPropertyCE(groups);
} else {
   BConverterCE ce = new BConverterCE(this.editorPane, this, conv);  // ...
}
```

1. La prop tiene un `BConverter` en su binding → **`BConverterCE`**.
2. La prop está agrupada en una PX custom-property → **`BPxPropertyCE`**.
3. Si no → lookup genérico `editor.getController().getCellEditor(widget, prop)`, que resuelve el mecanismo
   estándar de **cell-editor agent del Workbench** (`javax.baja.workbench.celleditor.BWbCellEditor` +
   registro `@AgentOn`). Ahí `BLayoutCE`/`BSizeCE`/`BWidgetPropertyCE`/`BWidgetEventCE`/`BActionArgCE`/
   `BVariablesCE`/`BVariableTextCE` se enganchan IMPLÍCITAMENTE por tipo — `BPxCellSheet` NO los instancia. `[CERT]`

**No existe una interfaz "contrato CE" en este paquete** `[INFER]`: cada CE extiende una base stock del Workbench
(`BButtonCE`, `BListDropDownCE`, o `BWbCellEditor` crudo), todas de `javax.baja.workbench.celleditor`.

| CE | Base | file:line | Qué edita / mecanismo |
|---|---|---|---|
| `BPxPropertyCE` | `BButtonCE` | `celleditors/BPxPropertyCE.java:14` | prop linkeada a un grupo PX custom-property. `dialog()` devuelve `null` (`:46-48`): es un INDICADOR read-only (fondo verde `187,255,187`), el edit va por el menú Link/Unlink |
| `BWidgetPropertyCE` | `BListDropDownCE` | `celleditors/BWidgetPropertyCE.java:17` | elige un NOMBRE de prop frozen de un widget cuyo tipo matchea `allowable`; dropdown de `getFrozenPropertiesArray()` filtrado (`:24-32`), save devuelve `BString` |
| `BConverterCE` | `BButtonCE` | `celleditors/BConverterCE.java:23` | el `BConverter` de un binding (fondo amarillo). `dialog()` = `BWbFieldEditor.makeFor(value)`; casó especial `BIEnumToSimpleFE` resolviendo los facets del enum del TARGET bound vía `resolveBindingTarget(...)` (`:52-66`) |
| `BLayoutCE` | `BButtonCE` | `celleditors/BLayoutCE.java:19` | `@AgentOn bajaui:Layout/LayoutDimension`; `BWbFieldEditor.dialog()` con `min=-1000,max=10000` |
| `BSizeCE` | `BButtonCE` | `celleditors/BSizeCE.java:19` | `@AgentOn gx:Size`; mismo patrón `min=0,max=10000` |
| `BPxLayerCE` | `BListDropDownCE` | `celleditors/BPxLayerCE.java:15` | el tag `layer` del widget; instanciado DIRECTO por `BPxCellSheet.makeCellEditors()` como fila sintética extra cuando `includeLayers=true` (`BPxCellSheet.java:662-671`) |
| `BActionArgCE` | `BButtonCE` | `celleditors/BActionArgCE.java:16` | el valor de un parámetro de `Action`; envuelve el field-editor en `BActionArg` (edge-pane con checkbox "null?", `celleditors/BActionArg.java:25,49-61`) para togglear "preguntar en runtime" (`BString.DEFAULT`) vs. literal |
| `BVariableTextCE` | `BWbCellEditor` | `celleditors/BVariableTextCE.java:38` | texto con placeholders `${variable}`; `BMiniTextField`+botón que abre `BOrdFE` para inyectar una ORD como texto (`:161-185`) |
| `BVariablesCE` | `BButtonCE` | `celleditors/BVariablesCE.java:19` | el mapa `BFacets` de variables de un `BPxInclude`; abre `BVariablesEditor.open()` (`:77-79`); `getVariableKeys()` recorre el árbol juntando los `${var}` referenciados por cualquier `BOrd` (`:57-71`) — descubre las claves sin schema |
| `BWidgetEventCE` | `BListDropDownCE` | `celleditors/BWidgetEventCE.java:18` | elige un nombre de evento (Action/Topic frozen) del widget; `getActionsArray()`/`getTopicsArray()` filtrados `isFrozen()` (`:26-40`) |

## 198.3 — Un edit → undo/redo: todo delega en `javax.baja.ui.Command` `[CERT]`

Cada comando de `cellsheet/commands/` **extends `javax.baja.ui.Command`** y devuelve una inner class
`implements CommandArtifact` desde `doInvoke()` — confirma B191: el PX editor delega undo/redo ENTERAMENTE en el
contrato `Command`/`CommandArtifact` del Workbench; no hay pila de undo propia de PX. Confirmados `extends Command`:
`AddBinding`, `AddConverter`, `ChangeBinding`, `ChangeConverter`, `ChangeLayer`, `ChangeProperty`, `CopyCell`,
`DeleteBinding`, `DeleteConverter`, `LinkPxProperty`, `PasteCell` (+`implements TransferConst`), `UnlinkPxProperty`. `[CERT]`

**El dispatcher edit→command.** `DefaultCellSheetContext.cellModified(BWbCellEditor ce)`
(`cellsheet/DefaultCellSheetContext.java:151-171`) es el embudo por el que pasa el save de todo cell editor:

```java
ce.saveValue();
if      (ce instanceof BConverterCE) new ChangeConverter(...).invoke();
else if (ce instanceof BPxLayerCE)   new ChangeLayer(...).invoke();
else                                 new ChangeProperty(...).invoke();
```

y `bindingPropertyChanged()` (`:174-176`) → `new ChangeBinding(...).invoke()` para las tablas de binding. `[CERT]`

**Forma do/undo representativa** — `ChangeProperty` (`cellsheet/commands/ChangeProperty.java:12-78`): `doInvoke()`
crea `Artifact` y llama `redo()`; `Artifact.redo()`=`perform(newValues)`, `undo()`=`perform(oldValues)`. Los valores
old/new se snapshotean EAGERLY en el **constructor** (`:26-32`, capturando `widgets[i].get(propertyName)` antes de
mutar) — patrón general en `ChangeBinding`/`ChangeConverter`/`ChangeLayer`. `[CERT]`

**Gotcha 1 — auto-supresión del listener al fire.** `ChangeProperty.Artifact.perform()` (y `ChangeBinding`) hace
`removePxListener(cellSheet)` antes de firear el `PxEvent` resultante, luego re-agrega — SOLO en la primera invocación
(flag `initialInvocation`, `ChangeProperty.java:68-72`). Evita que la hoja reaccione a su propio edit y se reconstruya
a mitad; redo/undo posteriores (Ctrl+Z) sí fírean normal, porque ahí SÍ se espera refrescar la hoja. `[CERT]`

**Gotcha 2 — Copy/Paste NO son undoables.** `CopyCell.doInvoke()` devuelve `null` (`commands/CopyCell.java:25-31`) y
`PasteCell.doInvoke()` también (`commands/PasteCell.java:45-76`) tras mutar in-place — el clipboard bypassa la pila
de undo; solo el commit SUBSIGUIENTE (`doPaste()`→`cellModified`) produce un `Command` undoable real. `[CERT]`

**Gotcha 3 — `AddBinding` es un comando de dos fases con diálogo modal DENTRO de `doInvoke()`.**
`AddBinding.doInvoke()` (`commands/AddBinding.java:37-51`) abre un `BDialog` modal con un `BList` de tipos de binding
candidatos, resueltos vía `w.getAgents().filter(AgentFilter.is(BBinding.TYPE))` (descubrimiento agent-based, `:53-56`)
ANTES de construir el `Artifact` — el redo/undo solo envuelve el binding YA elegido; el type-picker no es replayable. `[CERT]`

`LinkPxProperty`/`UnlinkPxProperty` no rutean por `BPxCellSheet`: llaman `context.pxPropertyLinked/Unlinked()` directo,
que en `DefaultCellSheetContext` (`:184-195`) actualiza `PxPropertyComponentArray` y firea un `PxWidgetEvent`/
`PxBindingEvent(id=2)` para notificar el cambio de link-state. `[CERT]`

## 198.4 — El árbol de widgets (`tree/`): modelo, nodos y sync de selección `[CERT]`

**Modelo.** `PxTreeModel extends TreeModel` (`tree/PxTreeModel.java:11`, package-private) mantiene un `rootNode` +
un `Map<BWidget,WidgetNode> hash` para lookup widget→nodo O(1) (`getNode()`, `:36-38`). `buildRoot()` elige tipo de
nodo por forma: `rootWidget instanceof BPane` → `PaneNode`, si no `LeafNode` (`:50-55`). `updateNodes()` hace diffing
incremental (si el root cambió identidad, rebuild total; si no, delega a `PaneNode.updateChildNodes()`). `[CERT]`

**Nodos.** `abstract class WidgetNode extends TreeNode` (`tree/WidgetNode.java:10`) tiene el `BWidget` compartido,
`text`/`icon` cacheados, y se auto-registra en el hash del modelo al construir (`model.putNode(this)`, `:24,34`).

- **`PaneNode extends WidgetNode`** (`tree/PaneNode.java:16`): tiene hijos. `childWidgets()` branchea en
  `Reflector.isFreeFormPane(widget)` — panes free-form (canvas/flow) enumeran hijos vía props `BWidget` no-frozen
  no-hidden en orden FORWARD (`freeFormChildren`, `:114-140`); panes "canned" (`BBorderPane`) enumeran todas las
  props widget no-dynamic no-hidden en orden **REVERSE** (`cannedChildren`, `:97-112`). El reverse es no-obvio
  (matchea orden de z-order/paint, no de declaración). `[CERT]`
- **`LeafNode extends WidgetNode`** (`tree/LeafNode.java:8`): `getChildCount()`=0; `dragOver()` solo permite drop si
  el widget `instanceof BNullWidget` (slot placeholder vacío) (`:27`). `[CERT]`
- Corrección al plan D1: `WidgetNode` es la base abstracta, no un tercer tipo concreto — el split real es
  `WidgetNode`(abstract) → {`PaneNode`, `LeafNode`}. `[CERT]`

**`BPxTree extends BTree implements BIPxTransferWidget, PxListener`** (`tree/BPxTree.java:67`) posee el triple
modelo+selección+controller (`:92-94`). Reacciona a toda la taxonomía `PxEvent` (`pxEvent()`, `:98-174`): add/remove
de componentes → `updateNodes()`+`updateSelection()`; cambios de prop/nombre → el más barato `refreshNodeText()`. `[CERT]`

**Sync de selección (el puente bidireccional).** `PxTreeSelection extends TreeSelection` (`tree/PxTreeSelection.java:10`)
sobreescribe `select()`/`deselectAll()`/`deselect()` para empujar al modelo compartido `SelectedWidgets` Y firear un
`PxSelectionEvent` en el editor (`:27-50`); expone `superSelect()`/`superDeselectAll()` package-private para que
`BPxTree.updateSelection()` (`:190-205`) re-sincronice la selección VISUAL del árbol desde `SelectedWidgets` SIN
re-disparar el event loop — así evita ciclos de feedback canvas-click↔tree-click. `[CERT]`

**`PxTreeController extends TreeController`** (`tree/PxTreeController.java:18`): doble-click delega a
`editor.getController().getDoubleClickCommand()`, Ctrl+Up/Down reordena z-order vía `new Reorg(...)` (`:76-100`).
**`BPxTreePane extends BPxSideBar`** (`tree/BPxTreePane.java:17`) envuelve con toolbar (bring-to-front/back vía `Reorg`)
+ `BTreePane(new BPxTree(...))` (`:33-50`). `[CERT]`

**Gotcha — el árbol es un drop-target de primera clase, no un navegador read-only.** `BPxTree` maneja toda la lógica
de drag/drop-insertion (`insertTransferData`/`drop`/`dragOver`, `:241-296`), incluida la semántica duplicar-con-offset
(`+10,+10`px, `MoveWidget.toZero`, `:453-467`) y el caso `BNullWidget` (slots frozen placeholder) vs. drop en pane
free-form. `[CERT]`

## 198.5 — `propsheet/` y `layersheet/`: PX custom-properties y layers `[CERT]`

`BPxPropSheet extends BPxSideBar implements PxListener` (`propsheet/BPxPropSheet.java:52`) edita
`BPxEditor.getPxProperties()` — las **PX custom-properties** (grupos nombre+tipo+valor usados para el linking
cross-widget de §198.2/§198.3). Usa `com.tridium.workbench.celltable.BLabeledCellTable`, una fila por `PxProperty`,
cell editor genérico vía `BWbCellEditor.makeFor(prop.getValue())` (`:228-245`). Commits por `ChangePxPropertyValue`
(`propsheet/ChangePxPropertyValue.java:13`, patrón redo/undo estándar con guard `firstTime`, `:43-58`); Add/Remove/
Rename son `Command`s separados (`AddPxProperty` abre diálogo modal nombre+`BTypeSpecFE`, `propsheet/AddPxProperty.java:47-97`). `[CERT]`

`BPxLayerSheet extends BPxSideBar implements PxListener` (`layersheet/BPxLayerSheet.java:53`) es estructuralmente
idéntico pero edita el `PxLayer[]` del editor — **layers** de visibilidad/z-order (`BLayerStatus`, con `BEnumCE` en la
columna status, `:5,90`). Comandos `AddPxLayer`/`RemovePxLayer`/`RenamePxLayer`/`ChangePxLayerValue` (misma forma). `[CERT]`

## 198.6 — `binding/`: reescritura de ORDs (relativize / replace / neqlize) `[CERT]`

`BOrdChanger extends BDialog` (`binding/BOrdChanger.java:29`, abstract) es el shell compartido: tabla de 3 columnas
(check / ord-antes / ord-después) sobre `BOrd before[]`, calculando `after[]` vía los hooks abstractos
`after(BOrd)` y `selectable(int)` (`:60-62`). Tres editores concretos:

- **`BRelativizeOrds`** (`binding/BRelativizeOrds.java:14`): reescribe ORDs `slot:` a rutas relativas `..` contra un
  `baseNames` (`relativizeOrd()`, `:39-85`) — al mover/copiar un subárbol para que sus bindings queden relativos y no
  se rompan ni apunten absoluto al origen. `[CERT]`
- **`BReplaceOrds`** (`binding/BReplaceOrds.java:22`): find/replace textual sobre el texto de la ORD
  (`TextUtil.replace(ord.toString(), from, to)`, `:53-57`), con `BMruTextDropDown` from/to. `[CERT]`
- **`BNeqlizeOrds`** (`binding/BNeqlizeOrds.java:53`): reescribe ORDs a forma tag/relation ("n:") usando
  `javax.baja.tag.Relation`/`Entity`/`Tags` (`:33-39`) — normaliza bindings acoplados a station-path al esquema
  tag/entity (gate mínimo `MIN_NEQLIZE_VER = "4.9"`, `BBoundOrds.java:65`): portabilidad ante renombres/movimientos
  vía identidad de tag en vez de slot-path. `[CERT]`

`ChangeOrds extends Command` (`binding/ChangeOrds.java:16`) es el commit de las tres: array de `Entry(component, prop,
oldOrd, newOrd)`, separa binding-entries de widget-entries y aplica con `Artifact` redo/undo estándar
(`:32-111`), fireando `EventUtil.bindingsChanged`/`widgetsChanged`. `BBoundOrds extends BPxSideBar` (`binding/BBoundOrds.java:62`)
es el sidebar que hostea las tres (inner `Command`s `Relativize`/`Replace`/`Neqlize`, `:341,409,475`) piped a
`new ChangeOrds(...).doInvoke()`. `[CERT]`

## 198.7 — Connections

- **[Block 191]** (editor como herramienta): confirmado aquí que TODO el undo/redo del cell-sheet delega en
  `javax.baja.ui.Command`/`CommandArtifact` del Workbench (§198.3) — sin pila propia de PX, como anticipó B191.
- **[Block 192]** (catálogo widgets bajaui): los cell editors resuelven `@AgentOn` por tipo bajaui (`bajaui:Layout`,
  `gx:Size` — §198.2); el filtro de props excluye widgets anidados porque van al canvas, no a la hoja (§198.1).
- **[Block 193]** (los 9 bindings kitPx): `loadBindings()` presenta un `BPxBindingTable` por `BBinding` hijo (§198.1);
  `BConverterCE` edita el `BConverter` de un binding y `ChangeBinding`/`ChangeConverter` lo commitean (§198.2/§198.3).
- **[Block 197]** (síntesis 7 capas): este bloque profundiza la capa "herramienta/edición" que B197 mapeó a alto nivel.
- **Fuera de `sidebars/`** (nombrados, no abiertos): `com.tridium.px.editor.BPxEditorPane` (getSelectedWidgets/
  getPxPropertyComponents/getTreeStudio), `javax.baja.px.editor.BPxEditor`+tipos `PxEvent` (el bus pub/sub que todo
  sidebar escucha vía `PxListener`), `javax.baja.workbench.celleditor.{BWbCellEditor,BButtonCE,BListDropDownCE}` +
  `fieldeditor.BWbFieldEditor` (framework backing de casi todo CE), y `studio/commands.{MoveWidget,Reorg}` +
  `editor/commands.{Delete,Insert,Rename}` (la capa de manipulación que `BPxTree` delega) — feeds de los gaps D2/D4.
