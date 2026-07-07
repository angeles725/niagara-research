# Block 210 — El bus de eventos del PX Editor: `javax.baja.px.editor.event` (PxEvent / PxListener + dispatch en BPxEditor + EventUtil)

> Research de la **INFRAESTRUCTURA de `pxEditor-wb`** — foco `px-editor-core`, gap **C1**. Documenta el
> **bus pub/sub de eventos** del PX Editor: la abstracción base `PxEvent`, la interfaz `PxListener`, la
> taxonomía de 10 subtipos, los eventos *compound* (batch), el mecanismo de dispatch (que NO vive en el
> paquete `event` sino en el hub `BPxEditor`), y la factory `EventUtil`. Este es el sistema nervioso que
> sincroniza sidebars ↔ canvas ↔ árbol de componentes — nombrado repetidamente en B198/B201/B205/B206
> pero nunca abierto hasta aquí. NO cubre util/property (gap C4) ni los field-editors de converters (C5).
>
> Sources (decompilado Vineflower, READ-ONLY):
> `/home/cristian/modules/Prototipos/modulos/organized/pxEditor/pxEditor-wb/vineflower/` — subárboles
> `javax/baja/px/editor/` (event/ + BPxEditor root) y `com/tridium/px/editor/` (util/, sidebars/).
> Las citas `file:line` son relativas a esos subárboles (p.ej. `event/PxEvent.java:3` =
> `.../vineflower/javax/baja/px/editor/event/PxEvent.java`).
> Method: lectura directa del decompilado (12 clases del paquete `event` + `EventUtil` + el hub `BPxEditor`
> + 4 sidebars como consumidores). Markers (canónico METHODOLOGY §3):
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción.
>
> Capa infraestructura pxEditor-wb (núcleo). Connects [Block 198] (sidebars — los emisores/oyentes),
> [Block 205] (studio/canvas — repinta al recibir eventos), [Block 191] (BPxEditor como herramienta),
> [Block 202] (field-editors de converters — el cruce EventUtil↔BConverter).

---

## 210.1 — La abstracción base: `PxEvent` (clase abstracta + 9 categorías) `[CERT]`

`PxEvent` es una **clase abstracta** (no interfaz), con un único campo `private int type` que clasifica el
evento en una de 9 categorías top-level. Los constantes de categoría y el ctor protegido:

```java
// event/PxEvent.java:3-18
public abstract class PxEvent {
   public static final int EDITOR = 0;
   public static final int PX_PROPERTY = 1;
   public static final int SELECTION_CHANGED = 2;
   public static final int WIDGET = 3;
   public static final int BINDING = 4;
   public static final int COMPOUND_WIDGET = 5;
   public static final int COMPOUND_BINDING = 6;
   public static final int USER_DEFINED = 7;
   public static final int PX_LAYER = 8;
   private int type;
   protected PxEvent(int type) { ... }
```

| Elemento | Detalle | Cita |
|---|---|---|
| Tipo | clase **abstract**, no interfaz | `event/PxEvent.java:3` |
| Campo | `private int type` (categoría, NO el widget/valor — eso lo lleva cada subtipo) | `event/PxEvent.java:16` |
| Ctor | `protected PxEvent(int type)` | `event/PxEvent.java:18` |
| Accessor | `getEventType()` | `event/PxEvent.java:27` |
| `toString()` | mapea `type` → `ID_STRINGS[type]` (tabla de nombres) | `event/PxEvent.java:23-25` |

Las 9 constantes son la taxonomía de **categoría** (no llevan old/new value por instancia; el payload fino
lo lleva cada subtipo, §210.3). El `type` es redundante con la clase concreta (cada subtipo hardcodea su
categoría en `super(N)`), pero permite un `switch(getEventType())` sin `instanceof` en el dispatch (§210.5).

## 210.2 — `PxListener`: interfaz SAM de un solo método `[CERT]`

El listener es una **interfaz funcional de un solo método** (Single Abstract Method), NO multi-callback:

```java
// event/PxListener.java:3-5
public interface PxListener {
   void pxEvent(PxEvent var1);
}
```

Consecuencia de diseño: todo el fan-out es un único `pxEvent(PxEvent)`. La discriminación por subtipo
ocurre DENTRO de cada implementación de listener (vía `getEventType()` / `instanceof`), no por métodos de
callback distintos. Es el patrón "un canal, N categorías" — más simple que el `EventListener` multi-método
de Swing/AWT `[INFER]` (comparación de diseño, no citada en fuente).

## 210.3 — Taxonomía: DOS jerarquías, no 10 planas `[CERT]`

Los 10 subtipos NO son planos bajo `PxEvent`. Existe una **clase intermedia abstracta `PxComponentEvent`**
compartida por los eventos de widget y binding, que agrega el payload rico id + `String[] propNames` +
`BValue[] values` (arrays paralelos):

```java
// event/PxComponentEvent.java:5-24
public abstract class PxComponentEvent extends PxEvent {
   public static final int ADDED = 0, REMOVED = 1, CHANGED = 2, RENAMED = 3, REORDERED = 4;
   private String[] propNames;
   private BValue[] values;       // arrays paralelos: nombre-de-slot ↔ valor Baja
   // ctor valida propNames.length == values.length, si no: IllegalStateException
```

Los subtipos, con lo que extienden, su payload distintivo y sus constantes:

| Subtipo | Extiende (categoría) | Payload distintivo | Constantes | Cita |
|---|---|---|---|---|
| `PxWidgetEvent` | `PxComponentEvent` (type=3) | `private BWidget widget` + prop/value heredados | ADDED..REORDERED (heredadas) | `event/PxWidgetEvent.java:7-13` |
| `PxBindingEvent` | `PxComponentEvent` (type=4) | `private BBinding binding` + prop/value heredados | ADDED..REORDERED (heredadas) | `event/PxBindingEvent.java:7-8` |
| `PxPropertyEvent` | `PxEvent` (type=1) | `private PxProperty pxProperty` | propias `ADDED=0,REMOVED=1,CHANGED=2,RENAMED=3` | `event/PxPropertyEvent.java:5-12` |
| `PxLayerEvent` | `PxEvent` (type=8) | `private PxLayer pxLayer` | propias `ADDED..RENAMED` (misma forma) | `event/PxLayerEvent.java:5-12` |
| `PxSelectionEvent` | `PxEvent` (type=2) | `private BWidget[] widgets` (selección) | ninguna (ctor trivial) | `event/PxSelectionEvent.java:6-11` |
| `PxEditorEvent` | `PxEvent` (type=0) | `private int id; private Object value` | `LOADED=0,SAVED=1,OPTION_CHANGED=2,TOOL_CHANGED=3` | `event/PxEditorEvent.java:3-10` |
| `PxUserDefinedEvent` | `PxEvent` (type=7) | `private Object data` (escape hatch opaco) | ninguna | `event/PxUserDefinedEvent.java:3-4` |
| `PxCompoundWidgetEvent` | `PxEvent` (type=5) | array de `PxWidgetEvent` (§210.4) | — | `event/PxCompoundWidgetEvent.java:6-19` |
| `PxCompoundBindingEvent` | `PxEvent` (type=6) | array de `PxBindingEvent` (§210.4) | — | `event/PxCompoundBindingEvent.java:6-19` |

Observaciones:
- `PxWidgetEvent`/`PxBindingEvent` son los únicos "ricos" (heredan la forma prop/value de `PxComponentEvent`);
  el resto agrega un solo campo distintivo.
- `PxPropertyEvent` y `PxLayerEvent` son gemelos estructurales (mismo set de constantes, distinto tipo de
  objeto envuelto — `PxProperty` vs `PxLayer` de `javax.baja.ui.px`).
- El payload usa tipos Baja, no Java crudo: `BValue[]` (slots), `BWidget`/`BBinding` (bajaux). Ver §210.6 y
  el cruce con converters.

## 210.4 — Eventos *compound*: batch multi-widget/binding en un solo fire `[CERT]`

Los dos eventos "compound" son literalmente **arrays de eventos hijo del tipo hoja correspondiente**, con
un invariante: todos los hijos comparten el mismo `id`:

```java
// event/PxCompoundWidgetEvent.java:6-19
private int id;
private PxWidgetEvent[] events;
public PxCompoundWidgetEvent(int id, PxWidgetEvent[] events) {
   super(5);
   this.id = id; this.events = events;
   for (int i = 0; i < events.length; i++)
      if (events[i].getEventId() != id) throw new IllegalStateException();
}
```

`PxCompoundBindingEvent.java:6-19` es idéntico con `PxBindingEvent[]`. **Para qué existen**: permiten que una
sola acción de UI que afecta a MÚLTIPLES widgets/bindings a la vez (p.ej. editar una propiedad sobre una
multi-selección) se dispare como UN `pxEvent()` en vez de N — confirmado por las factories de `EventUtil`
que sólo colapsan a compound cuando `length > 1` (§210.6).

## 210.5 — Dispatch: registro y fire viven en `BPxEditor`, no en el paquete `event` `[CERT]`

El paquete `event` sólo define los datos. El **registro/fire vive en el hub `BPxEditor`** (root de
`javax/baja/px/editor/`):

```java
// BPxEditor.java:63
private final List<PxListener> listeners = new ArrayList<>();
// BPxEditor.java:143-155
public void addPxListener(PxListener l)    { if (!listeners.contains(l)) listeners.add(l); }
public void removePxListener(PxListener l) { listeners.remove(l); }
public PxListener[] getPxListeners()       { return listeners.toArray(new PxListener[0]); }
```

`firePxEvent(PxEvent)` (`BPxEditor.java:157-234`) hace **dos cosas**:

1. **El editor es oyente privilegiado de su propio bus**: un `switch(event.getEventType())` reacciona
   internamente (repaint, `forceRootLayout()`, `doUpdate()`, reset de handles). Para eventos widget/binding
   afina con `EventUtil.getEventType((PxComponentEvent)event)` (`BPxEditor.java:188`) a categorías finas
   (§210.6).
2. **Fan-out síncrono con aislamiento de excepciones** (`BPxEditor.java:225-233`):

```java
PxListener[] x = this.getPxListeners();
for (int i = 0; i < x.length; i++) {
   try { x[i].pxEvent(event); }
   catch (Throwable t) { t.printStackTrace(); }   // un listener que lanza NO bloquea al resto
}
```

Entrega **síncrona, en orden de registro, con aislamiento por-listener**. Los consumidores se registran como
`this` implementando `PxListener`. Los 4 sidebars son **a la vez oyentes Y emisores** (así se sincronizan
selección de canvas ↔ sidebars):

| Sidebar / componente | Rol | Cita |
|---|---|---|
| `BPxTree` (árbol) | oyente (`implements PxListener`, `editor.addPxListener(this)`) | `sidebars/tree/BPxTree.java:67,90` |
| `PxTreeController` | emisor (`firePxEvent(new PxSelectionEvent(...))`) | `sidebars/tree/PxTreeController.java:72` |
| `BPxPropSheet` (props) | oyente **y** emisor | `sidebars/propsheet/BPxPropSheet.java:52,87,151,199` |
| `BPxLayerSheet` (capas) | oyente **y** emisor | `sidebars/layersheet/BPxLayerSheet.java:53,89,161,200` |
| `BPxCellSheet` (celdas) | oyente | `sidebars/cellsheet/BPxCellSheet.java:97,142` |
| `BPxEditorPane` (canvas) | emisor (`PxEditorEvent`: property/tool changed) | `BPxEditorPane.java:267,963,981` |

Esto **confirma en fuente** lo que B198/B201/B205/B206 nombraban sin abrir: `BPxEditor` es la única instancia
del bus; los sidebars y el canvas usan `PxSelectionEvent` + eventos de cambio para mantener sincronizados
selección y vistas.

## 210.6 — `EventUtil`: reclasificación fina + factories de compound `[CERT]`

`com/tridium/px/editor/util/EventUtil.java` (137 líneas) tiene dos responsabilidades:

**(a) Reclasificación** — `getEventType(PxComponentEvent)` (`util/EventUtil.java:45-93`) mapea un
`PxWidgetEvent`/`PxBindingEvent` + su `id` + el supertipo común de su array `values`
(vía `ClassUtil.getCommonSuperType`, cruce `com.tridium.util.ClassUtil`) a **13 constantes finas**
`UNKNOWN=0 .. CONVERTER_REMOVED=12` (`util/EventUtil.java:16-28`). Ejemplo: un `PxWidgetEvent` con
`id==ADDED` cuyo valor es un `BBinding` → `BINDING_ADDED`; si el valor es un `BWidget` → `WIDGET_ADDED`.
Es lo que `BPxEditor.firePxEvent` invoca en `:188` para decidir repaint/update interno.

**(b) Factories de batch/compound**:

```java
// util/EventUtil.java:105-119
public static PxEvent widgetsChanged(BWidget[] widgets, String[] props) {
   if (widgets.length != props.length) throw new IllegalStateException();
   else if (widgets.length == 1)
      return new PxWidgetEvent(2, widgets[0], props[0], widgets[0].get(props[0]));   // CHANGED, 1 → evento hoja
   else { ...; return new PxCompoundWidgetEvent(2, events); }                        // N → compound
}
// util/EventUtil.java:121-135 — bindingsChanged(BBinding[], String[]): misma forma → PxCompoundBindingEvent
```

Notas:
- La llamada directa `widgets[i].get(props[i])` (`util/EventUtil.java:114`) confirma que el "nuevo valor" se
  fotografía al crear el evento vía el API estándar de slots Baja `BComponent.get(String)` (cruce bajaux).
- `EventUtil` sólo construye los eventos de widget/binding (hoja o compound). `PxSelectionEvent`,
  `PxLayerEvent`, `PxEditorEvent`, `PxPropertyEvent` y `PxUserDefinedEvent` se construyen directo en los
  call-sites (§210.5), no por la factory.
- `getEventType` cruza `javax.baja.util.BConverter.TYPE` para la sub-clasificación converter-add/change/remove
  — **atando este bus de eventos al subsistema de converters** que documentan los field-editors (gaps C5 / B202).

## 210.x — Connections

- **[Block 198]** (sidebars) — los sidebars que aquel bloque documentó (`BPxTree`, `BPxPropSheet`,
  `BPxLayerSheet`, `BPxCellSheet`) son exactamente los oyentes/emisores del bus reconstruido aquí; B198
  nombró `EventUtil`/los PxEvents sin abrirlos — §210.5/§210.6 los cierran.
- **[Block 205]** (studio/canvas) — el canvas repinta y re-layouta como reacción del `switch` interno de
  `firePxEvent` (§210.5.1); el bus es lo que conecta la máquina de dibujo con las vistas.
- **[Block 191]** (BPxEditor como herramienta) — `BPxEditor` es el hub que aloja `listeners`/`firePxEvent`;
  este bloque abre su rol de bus además de su rol de herramienta.
- **[Block 202]** (field-editors de converters) — el cruce `EventUtil.getEventType`↔`BConverter.TYPE`
  (§210.6) es la costura entre el bus y el subsistema de converters; profundiza en el gap C5.
- **[Block 209]** (síntesis px-editor-deep) — esta infra "nombrada-no-abierta" era una de las deudas que la
  síntesis dejó marcada; C1 la salda.
