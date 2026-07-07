# Bloque 201 — El wizard "Make Widget" (`make/`): estrategias agent-based que convierten un ord en widget

> Research del focus **`px-editor-deep`** (gap D3): el paquete `make/` (16 clases) — el wizard "Make Widget" que
> convierte un componente/ord dropeado en un widget PX. B191 documentó `BMwFromPalette` (paleta→canvas) y la entrada
> `BMakeWidget`; ESTE bloque cubre las OTRAS estrategias `BMw*` y el mecanismo de selección. Hallazgo transversal:
> el wizard "Make Chart" produce el chart **clásico** `javax.baja.chart.BChartPane` (un BWidget PX), NO el webChart
> bajaux de B199 — son dos sistemas de charts distintos. NO cubre `commands/` (D4) ni field editors (D5).
>
> Sources (preservados §5): `sources/decompiled/pxEditor-wb/make/` — 16 `.java` (Vineflower). Barrido delegado
> (sonnet) 2026-07-06; 6 citas load-bearing token-checked literal. Method: lectura READ-ONLY del decompilado.
> Markers (§3): `[CERT]` `file:line` · `[INFER]`. Tipo: EVIDENCE block. Citas relativas a `make/`.
>
> Capa PX (herramienta / wizard). Connects [Block 191] (BMakeWidget/BMwFromPalette), [Block 198] (cell-sheet
> reutilizado), [Block 193] (bindings kitPx), [Block 199] (webChart, contraste §14), [Block 188] (BPxInclude).

---

## 201.1 — El wizard: `BMakeWidget` (un `BEdgePane`) + selección agent-based `[CERT]`

`BMakeWidget` **no es un modelo abstracto de wizard — ES un `BWidget`** (`make/BMakeWidget.java:52`,
`extends BEdgePane`), construido como la UI raíz del wizard con un `BWizardHeader` arriba (`:182`). `[CERT]`

**La selección de estrategia es agent-based, no un switch.** La base de estrategia `BMwConfig`
(`make/BMwConfig.java:27`, `extends BWidget implements BIAgent`) se descubre consultando el registro de agentes sobre
el tipo del objeto dropeado, filtrado a agentes `BMwConfig`:

```java
// make/BMakeWidget.java:99-106
AgentList filter = objects[i].getAgents().filter(AgentFilter.is(BMwConfig.TYPE));
for (AgentInfo agent : filter.list()) { if (!configAgents.contains(agent)) configAgents.add(agent); }
```

Ocho estrategias conocidas se siembran siempre en el set candidato (`:86-94`: BoundLabel, PxInclude, FromPalette,
WorkbenchView, PropertyBatch, ActionBatch, TimePlot, Chart), y se le unen los agentes `@AgentOn` registrados por
cualquier módulo sobre el tipo dropeado — el mecanismo es **genuinamente extensible** (cada `BMw*` salvo FromPalette/
Batch declara `@AgentOn(types={"baja:Object"})`, ej. `BMwChart.java:42-46`). La **aplicabilidad** se filtra DESPUÉS
por el `load()` de cada estrategia (`BMwConfig.java:73`, abstract): habilita su `ToggleCommand`/radio según los objetos
reales (Chart exige `BITable`, TimePlot exige `BINumeric`, §201.2). `[CERT]` `[INFER: intención de extensibilidad]`

**`MakeWidgetContext`** (`make/MakeWidgetContext.java:24`, `implements CellSheetContext`) es el puente que deja al
wizard **reutilizar el cell-sheet del editor** (`BPxCellSheet`, B198/D1) para editar el widget-en-progreso: lleva un
back-ref a `mw` y un **clon de trabajo** del property-component array de la página (`:26,30`,
`mw.getEditorPane().getPxPropertyComponents().deepClone()`). Sus binding-targets están pinneados a los objetos
dropeados y el add/delete de bindings está **prohibido** (`bindingAdded/Deleted`→`IllegalStateException`, `:34,54-61`):
el wizard crea UN binding por widget él mismo, no deja al usuario agregar arbitrarios. `[CERT]`

La materialización final es lazy en `getWidgetInserter()` (`BMakeWidget.java:311-324`): construye un `WidgetCopier`,
llama `cfg.makePxWidgets(wc)` en la config seleccionada, y envuelve el resultado + un comando `LinkWidgets` (undo/redo)
+ `cfg.columnCount()` en un `javax.baja.px.editor.factory.WidgetInserter`. La "última estrategia usada" se persiste vía
`PropertyManager` pickle keyed `"mwRadio"` (`:212-269`). `[CERT]`

## 201.2 — Las estrategias de valor `[CERT]`

| Estrategia | Input (qué exige) | Produce | Método |
|---|---|---|---|
| **`BMwChart`** | todos los objetos `javax.baja.collection.BITable` (`BMwChart.java:101-104`) | un `javax.baja.chart.BChartPane` con un `BLineChart` + un **`BTableChartBinding`** por objeto (historial/tabla), columnas tipeadas + query de rango en la ord | `makePxWidgets` `BMwChart.java:112-148` |
| **`BMwTimePlot`** | todos `BINumeric` (`BMwTimePlot.java:67-69`) | el MISMO `BChartPane`/`BLineChart`, pero con **`BValueChartBinding`** (valor live/real-time) + `BAxisSpec` con min/max del usuario | `makePxWidgets` `:77-114` |
| **`BMwBoundLabel`** | cualquier objeto (fallback default) | un `kitPx:BoundLabel` + `kitPx:BoundLabelBinding` (B193), con format-text/status/mouse-over/hyperlink/border reflect-invocados opcionalmente | `setWorkingWidget` `:346-362`; `makePxWidgets` `:369-403` |
| **`BMwPxInclude`** | cualquier objeto | un `javax.baja.ui.px.BPxInclude` (B188) por objeto, con el `.px` externo elegido por `BOrdFE` y las **variables** (`BFacets`) pobladas de las keys extraídas decodificando el px target | `setWorkingWidget` `:138-146`; `makePxWidgets` `:149-205` |
| **`BMwWorkbenchView`** | objetos con vistas Workbench comunes (intersección `WbSys.getFilteredViewList`, excluye vistas del propio `pxEditor` y `BAbstractPxView` para evitar recursión) | embebe un `BWbView` (o `BWebWidget` si `BIFormFactorMax`) con un **`BWbViewBinding`** | `setWorkingWidget` `:104-124` |

**Gotcha §14 — DOS sistemas de charts.** `BMwChart`/`BMwTimePlot` usan `javax.baja.chart.BChartPane`+`BLineChart`
(`BMwChart.java:9,12,13`) — el chart **clásico PX** (un `BWidget` Swing/gx que se dibuja en el canvas del `.px`),
diferenciados solo por la clase de binding (`BTableChartBinding` historial vs `BValueChartBinding` live). Este NO es el
`webChart` de B199 (bajaux/D3/JS, `com.tridium.webChart.ux.BChartWidget`): Niagara tiene **dos infraestructuras de chart
paralelas** — el chart clásico embebible en `.px` (este) y el webChart HTML5 moderno (B199). El wizard produce el
clásico. `[CERT]` (clarifica el scope de B199: aquél era webChart, éste es javax.baja.chart)

**Gotcha** — ni `BMwChart` ni `BMwTimePlot` implementan `setWorkingWidget()` (ambos `throw IllegalStateException`):
saltean el paso de preview en el cell-sheet, a diferencia de toda otra estrategia de widget único. `[CERT]`

## 201.3 — Las estrategias batch `[CERT]`

`BMwBatch` (`make/BMwBatch.java:13`, `abstract extends BMwConfig`) es la base de "hacer muchos widgets de una": posee un
`BTree`/`BTreePane` y un walker `orderedSelectedNodes()` (`:29-47`). Su `setWorkingWidget()` está sin implementar
(`IllegalStateException`, `:26`) — un batch no tiene un "working widget" único para previsualizar; construye directo en
`makePxWidgets`. `[CERT]`

- **`BMwActionBatch`** (`make/BMwActionBatch.java:40`): el árbol lista los `Action` no-hidden de cada objeto;
  aplicabilidad = **intersección de acciones** entre todos los objetos (`:74-80`). Por cada acción × objeto emite un
  `kitPx:ImageButton` con `kitPx:ActionBinding` de ord `<objectOrd>|slot:<actionName>` (`:94-133`), en grilla (objetos
  como columnas, acciones como filas). `[CERT]`
- **`BMwPropertyBatch`** (`make/BMwPropertyBatch.java:53`): el árbol se arma con `PropertyNode`/`PropertyNodeFactory`
  (§201.4) sobre las props simple/struct de cada objeto, intersectadas. Por nodo puede emitir cualquier combinación de:
  un `BLabel` (nombre), un widget arrastrado de un `BPaletteSideBar` embebido (bound por `<objectOrd>`), y/o un
  `kitPx:GenericFieldEditor`+`BWbFieldEditorBinding` (ord `<objectOrd>|slot:<propPath>`), toggleados por 3 checkboxes con
  layout fila-vs-columna (`:190-296`). **La estrategia más compleja del paquete** — un mini generador property-sheet→PX. `[CERT]`

Ambos batch caen a `BMwBoundLabel` cuando el set intersectado quedaría vacío (`BMwActionBatch.java:81-89`,
`BMwPropertyBatch.java:178-186`). **Gotcha** — `BMwPropertyBatch` embebe un `BPaletteSideBar` vivo DENTRO del wizard:
anida recursivamente otra sidebar (D1) como control de entrada de una de sus estrategias. `[CERT]`

## 201.4 — Soporte + convenciones `[CERT]`

- **`WidgetCopier`** (`make/WidgetCopier.java:14`): clona vía `BPxEditor.cloneWidget()` + un pase `ImageFileFactory` para
  imágenes embebidas (`:22-33`), y **memoiza el mapping old→new** en un `Map<BWidget,List<BWidget>>` para que
  `makePxWidgets` reconstruya un `PxPropertyComponentArray` apuntando a los widgets CLONADOS. Eso es lo que deja al
  comando `BMakeWidget.LinkWidgets` (`BMakeWidget.java:409-432`) swappear los `PxPropertyComponents` de la página
  atómicamente con undo/redo al insertar. `[CERT]`
- **`PropertyNode`/`PropertyNodeFactory`** (`PropertyNode.java:8`, `PropertyNodeFactory.java:15`): árbol lazy sobre las
  props `BComplex` de un objeto, restringido a `BSimple`/`BStruct`, excluyendo `BLink`/`BWsAnnotation`/`BPxView`
  (`PropertyNodeFactory.java:26-30`). Usado solo por `BMwPropertyBatch`. `[CERT]`
- **`BCellPane`** (`BCellPane.java:16`, `extends BPane`): wrapper de tamaño fijo sobre UN `BWbCellEditor`, para preview
  read-only de cell editors dentro de las UIs de estrategia; puro shim de sizing/theming. `[CERT]`
- **`BMwConfig`** (`make/BMwConfig.java:27`): la base abstract de estrategia. Define el contrato (`load()`,
  `setWorkingWidget()`, `makePxWidgets(WidgetCopier)`, `pickle/unpickle`, `columnCount()`=1 default) + dos helpers
  compartidos: **`addBinding()`** que marca los slots `ord`/`hyperlink` como **locked/frozen** (`bnd.setFlags(slot,
  getFlags(slot) | 1)`, `:90,93`) — el usuario NO puede editar a mano la ord auto-generada — y `makeDefaultWidgets()`
  (clone-por-objeto apilando `y`, `:99-131`), reusado verbatim por FromPalette y WorkbenchView. `[CERT]`

## 201.5 — Connections

- **[Block 191]** (BMakeWidget/BMwFromPalette): completa la parte que B191 dejó nombrada — las 8 estrategias del wizard
  y su selección agent-based; `BMwFromPalette` era solo una.
- **[Block 198]** (sidebars/cell-sheet): `MakeWidgetContext implements CellSheetContext` reutiliza el `BPxCellSheet` de
  B198 para editar el widget-en-progreso; `BMwPropertyBatch` embebe un `BPaletteSideBar` (D1).
- **[Block 193]** (bindings kitPx): las estrategias generan bindings kitPx — `BoundLabelBinding`, `ActionBinding`
  (§201.2/§201.3); `addBinding()` los deja con ord frozen.
- **[Block 199]** (webChart): **contraste §14** — `BMwChart` produce el chart CLÁSICO `javax.baja.chart.BChartPane`
  (BWidget PX), NO el webChart bajaux de B199. Dos sistemas de charts paralelos en Niagara.
- **[Block 188]** (BPxInclude): `BMwPxInclude` genera un `BPxInclude` con variables `BFacets` pobladas de las keys del
  px target — el mecanismo de includes de B188 desde el wizard.
- **Fuera de scope** (nombrados): `javax.baja.chart.{BChartPane,BLineChart,binding.*}` (chart clásico — candidato a gap
  propio), `javax.baja.px.editor.factory.WidgetInserter`, `BPxEditor.cloneWidget()` — feeds futuros.
