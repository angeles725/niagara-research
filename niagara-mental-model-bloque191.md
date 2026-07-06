# Bloque 191 — El PX Editor como herramienta (pxEditor-wb): árbol de widgets, paleta→canvas, opciones

> Research del focus **`px-editor`** (gap E1): el módulo `pxEditor-wb` — la HERRAMIENTA que edita un `.px`
> visualmente. Complementa `px-menu` (que documentó el FORMATO) con cómo el editor construye/edita el árbol
> de widgets, el flujo paleta→canvas (el "Make Widget wizard") y las opciones. NO cubre el catálogo de
> widgets (E2) ni bindings (E3).
>
> Sources (preservados §5): `sources/decompiled/pxEditor-wb/` — `BPxEditor.java`, `BPxEditorPane.java`,
> `BMwFromPalette.java`, `BMakeWidget.java`, `PxEditorController.java`, `NavNodeFactory.java` (Vineflower,
> módulo `pxEditor-wb`). Barrido delegado (sonnet) 2026-07-06.
> Method: lectura READ-ONLY del decompilado. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (herramienta). Connects [Block 180] (workflow oficial), [Block 181] (PxEncoder/Decoder), [Block 182] (EdgePane/panes).

---

## 191.1 — `BPxEditor extends BWbPxView`; load/save round-trip por el codec ya documentado `[CERT]`

`BPxEditor` (`BPxEditor.java:55`) extiende `BWbPxView` — la vista px del Workbench (confirma la capa que la
doc oficial de B180 describía). `[CERT]`

- **Load**: `doLoadValue` (`BPxEditor.java:82-97`) carga el widget con `loadPx(value, cx)` y monta la UI de
  edición: `setContent(editorPane = new BPxEditorPane(this)); editorPane.initSideBarPane(); editorPane.load(cx)`. `[CERT]`
- **Save**: `doSaveValue` (`BPxEditor.java:99-114`) delega en `editorPane.save(value, cx)`, que round-trip-ea
  por `PxEncoder`/`PxFile`/`PxDecoder` (`BPxEditorPane.java:445-493`) — el MISMO codec de B181. `[CERT]`

Dato clave: el árbol editado es un grafo de `BWidget` plano rooteado en un `BRootContainer`
(`BPxEditorPane.java:339-343`), **NO un árbol de `BComponent`** de la station. `[CERT]`

## 191.2 — `BPxEditorPane`: el canvas WYSIWYG `[CERT]`

`BPxEditorPane` (`BPxEditorPane.java:125`, extends `BEdgePane` — el pane de 5 regiones de B182 §182.4) es la
superficie WYSIWYG. Envuelve el root en `BZoomPane`/`BScrollPane` (`:204-205`) y canaliza mouse/paint por un
**`BStudio`** (`this.studio = new BStudio(editor, this)`, `:203`) — una fachada sobre
`CommandStudio`/`PainterStudio`/`TrackerStudio`/`TreeStudio`/`RootStudio`. `[CERT]`

- Selección: `SelectedWidgets selected` (`BPxEditorPane.java:146,200`). `[CERT]`
- Modo edición vs herramienta de dibujo: campo `BDrawingTool tool` togglea entre `NormalTool`/`GeometryTool`
  (`:932-984`), que instalan trackers/painters en el studio (p.ej. "add polygon" → `AddPolygonTracker`). `[CERT]`
- **No hay un swap de "view mode"** en el pane: `doLoadValue` solo construye el editorPane si el valor es
  editable; si no, cae a un `setContent(loadWidget)` read-only (`BPxEditor.java:82-88`); el estado `readonly`
  gatea los comandos de transfer/tool (`BPxEditorPane.java:344-357,600-614`). `[CERT]`

## 191.3 — Paleta→canvas: `WidgetFactory` chain → `BMakeWidget` wizard → `BMwFromPalette` `[CERT]`

Dos caminos para materializar un widget: `[CERT]`
1. **Drag-drop directo** al canvas: `BStudio`→`PxEditorController.insertTransferData/drop`
   (`PxEditorController.java:229-284`) lee los objetos del transfer `Mark` y llama
   `getWidgetInserter(this, objects)` (`PxEditorController.java:106-115`), que itera un `WidgetFactory[]`
   (`getDefaultWidgetFactories`, `:79-90`) hasta que uno `canConvert` matchea.
2. Para un nav-node de paleta (componente de módulo/bog), `NavNodeFactory.canConvert`
   (`NavNodeFactory.java:33-55`) matchea y `make()` **abre el `BMakeWidget` wizard** (`NavNodeFactory.java:26-29`).

Dentro del wizard, `BMwFromPalette` (`BMwFromPalette.java:45`, extends `BMwConfig`) es el tab de config por
defecto (`BMakeWidget.java:213-256`). Tiene su propio `BPaletteSideBar` embebido (reusa el sidebar del
Workbench, `BMwFromPalette.java:51`); al elegir un widget prototipo, `setWorkingWidget()`
(`BMwFromPalette.java:126-160`) lo clona y le estampa un ord placeholder (§191.4). La inserción final va por
`BMakeWidget.getWidgetInserter()` (`BMakeWidget.java:311-324`) → un `WidgetInserter` + `LinkWidgets`, que los
comandos `Insert`/drop splicean en el `BCanvasPane`/`BRootContainer`. `[CERT]`

Drag widget→widget (copiar uno existente, no de paleta) usa `WidgetCloningFactory` — clon directo sin wizard. `[CERT]`

## 191.4 — El clone por encode→decode + el placeholder ord `[CERT]`

Para clonar un subárbol de widgets, el editor NO copia objetos: **serializa y re-parsea** con el codec:
`cloneWidget` (`BPxEditor.java:271-287`) — `PxEncoder enc=...; enc.encodeDocument(oldWidget); ... new
PxDecoder(...).decodeDocument();`. `[CERT]` Es el mismo mecanismo de B181, usado como deep-clone. `[INFER]`

Al clonar de paleta, se toma el primer hijo `BBinding` y se le pone un ord placeholder:
`bnd.setOrd(BMakeWidget.placeholder)` con `placeholder = BOrd.make("<ord>")` (`BMakeWidget.java:56`,
`BMwFromPalette.java:135`) — el ord real se completa después vía el cell-sheet. `[CERT]` Esto explica por qué un
widget recién arrastrado trae el binding con ord `<ord>` sin resolver. `[INFER]`

## 191.5 — `BPxEditorOptions` (per-usuario, no per-file) `[CERT]`

`BPxEditorOptions` (`BPxEditorOptions.java:119`, extends `BUserOptions`) es un bag de opciones POR USUARIO: `[CERT]`
grid on/off+size+color (`:120-122`), snap-to-grid+size (`:123-124`), hatch (`:125-126`), `preserveIdentities`
para PxEncoder (`:127`, leído en `BPxEditorPane.java:490`), **`animateBindings`** (`:128`, gatea el
live-preview de `BPxEditor.PxEditorBinder`, `BPxEditor.java:348-361`), knobs NEQL (`:129-147`) y el
`BPxMedia` target por defecto (`:148`). Los cambios disparan `propertyChanged` (`:275-277`) y
`BPxEditorPane.doOptionChanged` los aplica en vivo (`BPxEditorPane.java:256-268`). `[CERT]`

## 191.6 — Subsistemas + undo (delegado al Workbench) `[CERT]`

Otras piezas del módulo (nombradas): `BStudio` (fachada del canvas), `SelectedWidgets` (modelo de selección),
`BPxCellSheet`+celleditors (la grilla property-sheet/binding, usada live y dentro del wizard,
`BMakeWidget.java:125`), `BPxTreePane`/`PxTreeModel` (sidebar del árbol de widgets), `BMakeWidget` (shell del
wizard que hospeda estrategias `BMwConfig`: `BMwFromPalette`, `BMwBoundLabel`, `BMwPxInclude`,
`BMwWorkbenchView`, `BMwChart`, `BMwTimePlot`…), y comandos `MoveWidget`/`MorphWidget`/`Align`/`Reorg`/`Delete`. `[CERT]`

**Gap honesto**: NO hay un undo-manager propio en el módulo — undo/redo se delega al framework
`Command`/`CommandArtifact` del Workbench (fuera de `pxEditor-wb`). `[CERT]`

## 191.x — Connections

- **[Block 180]** — workflow oficial del editor: este bloque da el CÓDIGO detrás de la paleta/property-sheet/drop que la doc describía.
- **[Block 181]** — `PxEncoder`/`PxDecoder`: el editor los usa para load/save Y para clonar widgets (§191.4).
- **[Block 182]** — `BEdgePane` (base de `BPxEditorPane`) + `BCanvasPane` (destino del insert).
- **[Block 188]** — `BMwPxInclude` es una de las estrategias del wizard (§191.6) → include reutilizable.
- **E2** (próximo) — catálogo de widgets: los prototipos que la paleta ofrece.
