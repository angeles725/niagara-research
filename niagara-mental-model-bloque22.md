# Bloque 22 — PX views + BajaUI widget runtime + BajaScript browser + Bajadoc

Fecha: 2026-04-23
Fuentes empíricas: decompilados JARs + `niagara-help/devguide-clean/` + `px/` samples.
JARs primarios: `workbench-rt/wb`, `bajaui-rt/ux/wb`, `bajaScript-ux`, `box-rt`, `docDeveloper-doc`, `CentralineAhuPx-wb`, `galileoKitPx-wb`, `webChart-*`, `webEditors-*`.

Cubre la **capa de presentación end-to-end**: XML declarativo (PX), widget framework Java (BajaUI), runtime JavaScript en browser (BajaScript), y generación de documentación developer (Bajadoc).

---

## 22.1 PX (Presentation XML) — formato y schema

### Estructura de un archivo `.px`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<px version="1.0" media="workbench:WbPxMedia">
  <import>
    <module name="baja"/>
    <module name="bajaui"/>
    <module name="gx"/>
  </import>
  <content>
    <!-- Un único widget root, con árbol recursivo -->
    <ScrollPane>
      <CanvasPane name="content"
                  viewSize="1000.0,800.0"
                  scale="fitRatio"
                  minScaleFactor="0.5"
                  maxScaleFactor="1.0"/>
    </ScrollPane>
  </content>
</px>
```

### Elementos root

- `<px version="1.0" media="..."/>` — atributo `media` selecciona target:
  - `workbench:WbPxMedia` — solo desktop (Workbench, Swing/AWT)
  - `hx:HxPxMedia` — web (HTML5, subset de widgets)
- `<import>` — lista `<module name="X"/>`, cada módulo resuelve símbolos prefijados (`X:TypeName` → `module:X:type.TypeName`)
- `<content>` — UN árbol único de widgets. Sin content vacío permitido.
- `<properties>` — opcional, define `PxProperty[]` parametrizables
- `<layers>` — opcional, capas (`PxLayer[]`) para organización visual

### Properties en widgets

- **Congeladas** (frozen): atributos del elemento (`layout="20,20,100,20"`, `text="Hello"`)
- **Dinámicas / complejas**: elementos hijo con `name` attribute (`<BLabel name="label">...</BLabel>`)

### Archivo de referencia real

`/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/defaults/workbench/newfiles/PxFile.px` (template base).

### Samples para reporting

`defaults/workbench/newfiles/ReportPxFile.px` (template para reports, incluye media mixto).

---

## 22.2 PX runtime — Decoder / Encoder / Include

### PxDecoder (`javax.baja.ui.px.PxDecoder` extends XParser)

```java
class PxDecoder extends XParser {
  String[]          modules;            // import list
  PxProperty[]      props;              // <properties>
  PxLayer[]         layers;             // <layers>
  HashMap<String,TypeInfo> types;       // registry resuelto
  BOrd              baseOrd;            // contexto de resolución relativa
  Context           cx;

  BWidget decodeDocument();
  BWidget decodeDocument(boolean);
  PxProperty[] getPxProperties();
  PxLayer[]    getPxLayers();
}
```

Flujo:
1. `parseXML()` (XParser) → `XElem` tree
2. `decodeHeader()` → lee `<px version media>`
3. `decodeImport()` → resuelve modules → `TypeInfo` registry
4. `decodeContent()` → recursivo `BWidget` instantiation:
   - `toType(XElem)` → lookup `TypeInfo`
   - instanciar constructor sin args
   - `decodeProps(XElem)` → set frozen properties desde atributos (vía `BValue.decodeFromString(String)` polimórfico)
   - recursar para slots complejos (children con `name`)

### PxEncoder (`javax.baja.ui.px.PxEncoder` extends XWriter)

```java
class PxEncoder extends XWriter {
  boolean preserveIdentities;
  void encodeDocument(BWidget);
  void encodeDocument(BWidget, PxProperty[], BAbstractPxView);
  void encodeDocument(BWidget, PxProperty[], PxLayer[], BAbstractPxView);
  void encodeDocumentWithMedia(BWidget, PxProperty[], PxLayer[], BTypeSpec);
}
```

Simétrico al decoder — serializa widget tree + PxProperty + PxLayer al mismo XML schema.

### BPxInclude (widget que carga PX externo)

```java
class BPxInclude extends BWidget {
  BOrd       ord;                       // target .px
  BFacets    variables;                 // params inyectables
  // private:
  volatile boolean loading;
  volatile boolean loaded;
  long             lastModified;
  BIFile           pxFile;
  Object           lock;
  Condition        loadCondition;
  BWidget          rootWidget;

  BWidget getRootWidget();
  BOrd    getBaseOrd();
  void    setBaseOrd(BOrd);
  void    reload();
  boolean isLoaded();
  void    sync();
  boolean isReloadRequired(Context);    // compara lastModified
}
```

Subclass Tridium: **BNPxInclude** (`com.tridium.ui.BNPxInclude`) — agrega:
- `fill` (BColor) — colorización dinámica
- `rotation` (int), `flip`, `mirror` (boolean)
- `colorize()` — aplica colores al SVG/px incluido (estados → colores)

### PxCache + PxIncludeManager

```java
final class PxCache {
  HashMap<Key,Item>  cache;             // LRU
  static int         MAX_CACHE_SIZE;    // configurable
  static Item get(OrdTarget, BAbstractPxView);
  static void trimAll();                // LRU eviction
}
// Key = (OrdTarget, baseOrd, media)
// Item = (BComponentSpace, resolved bindings)

final class PxIncludeManager {
  static PxIncludeCache pxIncludeCache;
  static Collection<String> nonCachedTypes;   // tipos que no se cachean
  static void load(BPxInclude);
  static void trimAll();
}
```

Múltiples `BPxInclude` del mismo ord reutilizan el widget tree cacheado.

### PxProperty + PxLayer

```java
class PxProperty {
  String      name;
  BTypeSpec   type;
  BValue      value;
  SlotPath[]  targets;                  // multi-binding
  OrdTarget[] resolved;                 // cache
  void apply(BComponent);
  void apply(BComponent, BValue);
}

class PxLayer {
  String        name;
  BLayerStatus  status;
}
```

Multi-target binding vía `SlotPath[]` permite parametrizar múltiples slots desde un solo PxProperty.

---

## 22.3 PX lifecycle (parse → bind → layout → paint)

```
Parse (PxDecoder.decodeDocument)
 ├ parseXML() → XElem tree
 ├ decodeHeader() → <px version media>
 ├ decodeImport() → TypeInfo registry
 └ decodeContent() → BWidget tree (recursive)

Resolve (post-processing)
 ├ props = decoder.getPxProperties()
 ├ layers = decoder.getPxLayers()
 └ baseOrd = BPxInclude context

Bind (framework invoca)
 para cada BBinding en tree:
  ├ binding.bind() (privado)
  ├ parse BOrd string → BOrd
  ├ resolve → OrdTarget (retry con baseOrd si relativo)
  ├ subscribe a property changes
  └ binding.started()
 si ORD no resuelve:
  └ isDegraded = true; applyDegradeBehavior()

Layout
 widget.layout() → doLayout(children)
  ├ container.computePreferredSize()
  ├ cada child → setBounds(x,y,w,h)
  └ recursivo + mark dirty areas

Paint
 widget.paint(Graphics)
  ├ pintar self
  ├ paintChildren(Graphics) → recursar
  └ Graphics flush a canvas

BPxInclude async load (background thread)
 loading = true
 pxFile = resolve ORD
 root = new PxDecoder(pxFile).decodeDocument()
 setBaseOrd(root, baseOrd)
 props, layers = decoder.getPxProperties/Layers
 loaded = true
 loadCondition.signalAll()
```

### Workbench view editor

`javax.baja.workbench.px.BWbPxView extends BWbView`:
```java
BAbstractPxView agent;      // dynamic view
BPxMedia        media;      // WbPxMedia | HxPxMedia
BWidget         widget;
BIFile          pxFile;
BOrd            pxOrd;
String          pxSource;   // XML source para editor
```

Ciclo: user abre `.px` → `BWbPxView` → `loadPx()` → `PxDecoder.decodeDocument()` → renderizado en canvas Workbench.

---

## 22.4 Binding model

### BBinding (`javax.baja.ui.BBinding`, 7.0 KB, abstract, implements BIAgent)

```java
abstract class BBinding extends BComponent implements BIAgent {
  Property ord;                         // BOrd a target
  Property degradeBehavior;             // BDegradeBehavior enum
  Topic    targetChanged;

  // Lifecycle:
  void started();                       // subscribe al target
  void stopped();                       // unsubscribe
  void targetChanged();                 // callback cuando valor cambia

  // Utilitarios (final):
  BWidget     getWidget();
  BWidgetShell getShell();
  boolean     isBound();
  OrdTarget   getTarget();
  BObject     get();
  boolean     isDegraded();
  void        applyDegradeBehavior();
}
```

### Subclases concretas

- `BValueBinding` — property simple (double/string/etc.). Props: `hyperlink` (BOrd), `summary` (BFormat), `popupEnabled` (boolean). Action `updateStatus`. Métodos: `getOnWidget(Property)`, `changedOnWidget(Property, Context)`.
- `BTableBinding` — binding de tabla a collection
- `BFieldEditorBinding` — bindings para campos de formulario

### BDegradeBehavior (enum)

Valores típicos: `DISABLE`, `OPACITY`, `HIDE`, `DEFAULT`. Aplicado automáticamente por `applyDegradeBehavior()` cuando `isDegraded() == true`.

### Conexión widget ↔ bindings

En BWidget:
```java
BBinding[]  bindings;                   // array
BBinding[]  getBindings();
boolean     hasBindings();
void        bindingsChanged();
boolean     isOverriddenByBinding(Property);
BValue      getOverride(Property);      // private

void fireChangedOnBindings(Property, Context);
void fireInvokedOnBindings(Action, BValue, Context);
void fireFiredOnBindings(Topic, BValue, Context);
```

### ORD resolution en bindings

`baseOrd` (de BPxInclude) actúa como contexto relativo:
- `../OtherComponent/slot` → relativo al parent
- `./slot` → mismo nivel
- `//path` → absolute desde station
- `|` → mismo componente

### Converters

Widgets pueden usar converters para transformar value ↔ display:
```xml
<BoundLabel>
  <BValueBinding ord="...">
    <ObjectToString name="text"/>       <!-- converter -->
  </BValueBinding>
</BoundLabel>
```

`ObjectToString` llama `Object.toString()` sobre el binding target. No hay scripting embedded nativo (`BPxScript` NO existe en N4.14 PX).

---

## 22.5 BajaUI widget framework — jerarquía

### BWidget (javax.baja.ui.BWidget, 23.2 KB)

Base universal. Extiende BComponent + implements IStylable.

**Props principales:**
```
visible       boolean
enabled       boolean
layout        BLayout       constraints immutables
styleClasses  String        CSS classes
styleId       String        CSS id
```

**Topics (eventos publicados):**
```
keyEvent     BKeyEvent
mouseEvent   BMouseEvent
focusEvent   BFocusEvent
```

**Coordenadas:**
```
private: x, y, width, height, prefWidth, prefHeight  (double)
setLocation(x,y), setSize(w,h), setBounds(x,y,w,h), setPreferredSize(w,h)
translateToChild, translateFromChild, translateToScreen
```

**Estado de rendering:**
```
private boolean needsLayout;
void relayout();              // async
void relayoutSync();          // sync (blocking)
void childCalledRelayout(BWidget);
```

### Widgets concretos (jerarquía)

```
BWidget
 ├ BLabel (14.2 KB)                — text + image + blink + wordWrap
 ├ BAbstractButton (12.3 KB)
 │  ├ BButton (3.3 KB)              — click action
 │  └ BToggleButton
 │     ├ BCheckBox                  — bi-state
 │     └ BRadioButton               — group-mutex
 ├ BTextEditor
 │  └ BTextField (8.1 KB)           — single-line input
 ├ BTable (27.8 KB)                 — data grid con cell renderers
 ├ BAbstractPane (layout managers)
 │  ├ BGridPane (10.9 KB)
 │  ├ BBorderPane (9.6 KB)
 │  ├ BFlowPane (10.8 KB)
 │  ├ BScrollPane (13.7 KB)
 │  ├ BSplitPane (13.0 KB)
 │  ├ BTabbedPane (19.1 KB)
 │  ├ BCanvasPane                   — absolute positioning
 │  ├ BConstrainedPane
 │  ├ BResponsivePane
 │  ├ BEdgePane
 │  └ BTransformPane
 ├ BTransferWidget                  — drag & drop + clipboard
 │  └ BSwingWidget (12.7 KB)        — Swing/AWT embed
 └ BPxInclude                       — dynamic PX load
```

---

## 22.6 Layout managers

### BLayout (value object immutable)

```java
class BLayout {
  // Units:
  static final int ABS = 0;           // píxeles
  static final int PERCENT = 1;       // % del contenedor
  static final int PREF = 2;          // preferred del widget
  // state:
  double x, y, w, h;
  int xUnit, yUnit, wUnit, hUnit;
  String string;                      // cached

  static BLayout make(...);           // factory
  static BLayout makeAbs(...);
  static BLayout make(String pattern); // "10,20,100%,50p"
  static final BLayout FILL;          // {0%,0%,100%,100%}
  static final BLayout DEFAULT;
}
```

### BGridPane (10.9 KB)

```
columnCount       int
valign, halign    default alignment
rowAlign, columnAlign   per-row/column override
rowGap, columnGap
uniformRowHeight, uniformColumnWidth  boolean
stretchRow, stretchColumn             int index
colorRows         boolean
bandBrush         BBrush
// internal:
double xo, yo;
double[] cw;                          // column widths calculadas
double[] rh;                          // row heights
```

### BBorderPane (9.6 KB)

```
label     BWidget (típicamente BLabel)
content   BWidget principal
margin    BInsets                    // exterior al border
padding   BInsets                    // interior al border
border    BBorder                    // decorativo
fill      BBrush                     // fondo
// cache:
RectGeom  background;
```

Constructores compactos para `(content)`, `(content, labelText)`, `(content, label, border)`, `(content, margin...)`.

### BScrollPane (13.7 KB)

```
content         BWidget
hscrollBar, vscrollBar   BScrollBar (null si oculta)
hpolicy, vpolicy         BScrollBarPolicy  NEVER | AS_NEEDED | ALWAYS
viewportBackground       BBrush
borderPolicy             BScrollBarPolicy
// internals:
RectGeom  viewport;
RectGeom  scrollTo;
boolean   vsbShow, hsbShow;
// actions:
hscroll(BScrollEvent), vscroll(BScrollEvent)
// métodos:
RectGeom getViewport();
void     scrollToVisible(RectGeom);
void     pulseViewport(Point, double);   // mouse wheel
```

### BSplitPane / BTabbedPane

- Split: `leftComponent|rightComponent` (o top/bottom), `dividerLocation`, `dividerSize`.
- Tabbed: tabs array, `selectedIndex`, placement (TOP/BOTTOM/LEFT/RIGHT).

---

## 22.7 Event model BajaUI

### Jerarquía

```
BStruct
 └ BWidgetEvent (1.2 KB)
    Constants: MODIFIED, ACTION_PERFORMED
    private: int id; BWidget source;
    Accessors: getId(), getWidget()

    ├ BInputEvent (4.3 KB)
    │   private: int modifiers;  long timestamp;
    │
    │   ├ BKeyEvent (5.9 KB)
    │   │   Constants: KEY_TYPED, KEY_PRESSED, KEY_RELEASED
    │   │   Virtual Keys (VK_*): 0-9, NUMPAD 0-9, A-Z, F1-F12,
    │   │   HOME/END/PAGE_UP/PAGE_DOWN/UP/DOWN/LEFT/RIGHT,
    │   │   SHIFT/CONTROL/ALT/CAPS_LOCK/NUM_LOCK,
    │   │   ENTER/ESCAPE/DELETE/INSERT/BACK_SPACE
    │   │   isAltDown/isControlDown/isShiftDown
    │   │
    │   └ BMouseEvent (4.5 KB)
    │       Constants: MOUSE_PRESSED, MOUSE_RELEASED, MOUSE_MOVED,
    │       MOUSE_ENTERED, MOUSE_EXITED, MOUSE_DRAGGED, MOUSE_WHEEL,
    │       MOUSE_PULSED, MOUSE_DRAG_STARTED, MOUSE_HOVER
    │       private: double x,y; int clickCount; boolean isPopupTrigger;
    │       isButton1Down/isButton2Down/isButton3Down
    │
    └ BFocusEvent (1.9 KB)
        Constants: FOCUS_GAINED, FOCUS_LOST
        private: boolean temporary;
```

### Propagación (callback directo, NO bubbling)

Cada widget implementa métodos handler (Template Method), no listener pattern:

```java
void fireKeyEvent(BKeyEvent e);
void fireMouseEvent(BMouseEvent e);
void fireFocusEvent(BFocusEvent e);

// Override en subclases:
void keyPressed(BKeyEvent);
void keyReleased(BKeyEvent);
void keyTyped(BKeyEvent);
void mousePressed(BMouseEvent);
void mouseReleased(BMouseEvent);
void mouseMoved(BMouseEvent);
void mouseDragged(BMouseEvent);
...
```

Lifecycle de evento mouse:
```
fireMouseEvent(e)
 → distribuye según e.getId()
 → childAt(x,y) → widget receptor
 → translateToChild() → ajusta coords
 → llama mousePressed/Released/etc en target
```

**GOTCHA**: NO hay bubbling automático. Propagación explícita al parent: `parent.fireKeyEvent(e)`.

---

## 22.8 Command framework

### Command (7.7 KB)

```java
class Command {
  String        keyBase;             // i18n key base
  String        label;               // localized
  BImage        icon;                // 16x16 o 32x32
  BAccelerator  accelerator;         // Ctrl+S etc
  String        description;         // tooltip
  BWidget       owner;
  WeakHashMap<BWidget,Object> registry;  // widgets usando command
  boolean       enabled;

  BWidget       getOwner();
  BAccelerator  getAccelerator();
  synchronized boolean isEnabled();
  synchronized void    setEnabled(boolean);
  synchronized BWidget[] getRegistry();
  synchronized void    register(BWidget);
  synchronized void    unregister(BWidget);

  final void invoke();
  final void invoke(CommandEvent);
  CommandArtifact doInvoke(CommandEvent) throws Exception;
  Command merge(Command other);
}

class CommandEvent {
  BWidget source;
  int     modifiers;                 // Shift|Ctrl|Alt mask
  long    timestamp;
}
```

Subclases: `ToggleCommand` (on/off). `BAbstractButton` registra el command asociado en su `registry`.

---

## 22.9 Graphics + theming

### Graphics2D abstraction (`javax.baja.gx`)

```
BFont     tipografía
BBrush    pincel (sólido, gradiente, patrón)
BColor    RGBA
BInsets   (top,left,bottom,right)
BImage    raster
BStroke   line (width, style, caps, joins)
RectGeom  rectangle geometry (clipping + bounds)
```

Pipeline rendering en BWidget:
```
repaint() / repaint(x,y,w,h)       schedule
paint(Graphics g)                   override en subclases
paintChildren(Graphics g)           itera children
paintChild(Graphics g, BWidget c)   paint individual
animate()                           frame-based (ej blink)
animateChildren()
```

### Theme singleton (`com.tridium.ui.theme.Theme`, 18.0 KB)

Factory methods:
```
Theme.borderPane() | button() | checkBox() | gridPane() | label()
     | scrollPane() | table() | textField() | ... (28 tipos)
```

Install:
```
String getInstalledThemeName();
void   installTheme(BModule module, String themeName);
void   installCustomTheme(BModule module, String themePath);
void   installDefaultTheme();
void   installFromEnum(BDynamicEnum themeEnum);
```

JavaFX CSS support:
```
String   getFxCss();
String[] getFontPaths();
```

172 clases de tema totales. Default: **Palladium** con subclases por widget (`PalladiumWidgetTheme`, `PalladiumGridPaneTheme`, `PalladiumTreeTheme`, `PalladiumMenuItemTheme`, etc).

---

## 22.10 Workbench (bajaui-wb) vs Web (bajaui-ux)

### bajaui-wb (1.36 MB) — Workbench

- Rendering: **Swing/AWT** (`BSwingWidget` integra `JRootPane`)
- Threading: engine thread + **EDT** (Event Dispatch Thread)
- Graphics: `javax.baja.gx.Graphics` (abstracción sobre AWT Graphics2D)
- 100+ widgets
- Temas: Palladium (172 clases)
- Eventos: direct callback (BKeyEvent/BMouseEvent/BFocusEvent)
- Binding: BBinding system con OrdTarget
- Transfer: BTransferWidget con drag/drop + clipboard

### bajaui-ux (271 KB) — Web adapters

Cada BUx* es **singleton adapter** (NO widget real) que implementa `javax.baja.bajaux.BIJavaScriptWidget`:
```java
interface BIJavaScriptWidget {
  JsInfo getJsInfo(Context);   // metadata para codegen JS
}
```

Conversión workbench → web:
```
BLabel      → BUxLabel.INSTANCE       → <div class="baja-label">
BButton     → BUxButton.INSTANCE      → <button class="baja-button">
BGridPane   → BUxGridPane.INSTANCE    → CSS Grid layout
```

Clases BUx (generadores codegen):
- Containers: `BUxBorderPane`, `BUxGridPane`, `BUxFlowPane`, `BUxScrollPane`, `BUxSplitPane`, `BUxTabbedPane`, `BUxConstrainedPane`, `BUxResponsivePane`, `BUxCanvasPane`, `BUxEdgePane`, `BUxTransformPane`
- Input: `BUxTextField`, `BUxButton`, `BUxToggleButton`, `BUxCheckBox`, `BUxRadioButton`, `BUxSlider`
- Display: `BUxLabel`, `BUxHyperlinkLabel`, `BUxSeparator`, `BUxPicture`, `BUxWebBrowser`
- Data: `BUxBoundTable`
- Include: `BUxPxInclude`
- Utils: `BUxNullWidget`, `BUxEmptyWidget`
- Shapes (vector): `BUxLine`, `BUxRect`, `BUxEllipse`, `BUxPolygon`, `BUxPath`

**Ventaja**: reutiliza lógica widget Workbench sin duplicar.

---

## 22.11 BajaScript runtime (browser)

### Estructura bajaScript-ux.jar

205 archivos JS distribuidos en:
```
rc/
 ├ sys.js                  106 KB   Type System core + registry
 ├ comm.js                  40 KB   Communications layer (Callback/Batch)
 ├ nav.js / obj.js / bson.js
 ├ baja/
 │  ├ comp/                         Component, Subscriber (25 KB), Property, ControlPoint
 │  ├ ord/                          Ord.js (28 KB) + schemes (Station/Slot/Hierarchy/Local/Fox)
 │  ├ sys/                          BaseBajaObj, Type System, inherit, bajaUtils
 │  ├ obj/                          Date, Double, Format, Icon, TimeZone
 │  ├ alarm/ hist/ coll/ tag/ virt/ file/
 │  └ comm/                         Callback, BoxFrame, BoxError, ServerSession
 ├ env/
 │  ├ browser.js            13 KB   DOM ready + WebSocket setup
 │  ├ WebSocketConnection.js 2.9 KB WSS/WS abstraction
 │  ├ BrowserCommsManager.js 10 KB  connection pool + reconnect
 │  ├ ConnectionManager.js  10 KB   lifecycle
 │  └ mux/
 │     ├ BoxEnvelope.js     10 KB   fragmentation
 │     ├ BoxEnvelopeMux.js   4 KB   outgoing
 │     ├ BoxEnvelopeDemux.js 4 KB   incoming reassembly
 │     └ BoxMessageRelay.js  9 KB   implicit batching (debounce ~10ms)
 ├ ctypes.js                 16 KB  embedded type registry JSON
 ├ plugin/baja.js            9 KB   RequireJS entry
 └ bs.built.min.js          360 KB  minified bundle completo
```

Metadatos módulo: version `4.14.0.162`, preferred symbol `bs`. Deps: box-rt, fox-rt, baja, jetty-rt, web-rt, control-rt, history-rt, alarm-rt.

### baja namespace core

```javascript
baja.outln(msg)                    // debug (consola + #bajaScriptOut)
baja.error(err)                    // errors (LocalizableError)
baja.subclass(Child, Parent)       // herencia prototípica
baja.def(value, default)           // coalesce con type check
baja.strictArg(arg, Type)          // runtime type validation

baja.Type                          // type resolver en runtime
baja.$ctypes                       // embedded type registry
baja.clock.schedule(fn, delay)     // scheduling, retorna ticket
baja.clock.expiredTicket           // sentinela cancelación
```

### Type System (baja.Type)

Carga lazy vía network RPC si tipo no en `baja.$ctypes`:
```javascript
baja.Type.getType('baja:Component')
  .then(type => {
    type.getSlots();              // iterator
    type.newInstance();
    type.decodeFromString("42");
    type.getSlot('name').getFacets();  // {editable, hidden, ...}
  });
```

Embedded registry (`rc/ctypes.js`) — persistido en localStorage como `bsRegStorage`:
```javascript
baja.$ctypes = {
  "baja:Component": {
    isv: true,     // isValue
    isx: true,     // isComplex
    isc: true,     // isComponent
    c: [],         // children slots
    it: ["baja:ISpaceNode","baja:IProtected",...],  // interfaces
    p: "baja:Complex"  // parent type
  },
  "baja:Integer": { isv:true, iss:true, isn:true, p:"baja:Number" },
  ...
};
```

### Ord resolution

```javascript
baja.Ord.make("station:|slot:/Folder/NumericWritable")
  .resolve({
    lease: true,
    leaseTime: 30000,
    subscriber: sub
  })
  .then(target => {
    const comp = target.getComponent();
    const value = comp.getOutDisplay();
  })
  .catch(err => console.error(err));
```

Anatomía ORD:
```
station:|slot:/Folder/Point
^      ^ ^    ^
scheme | | path (navegación recursiva por slot)
       | sessionId (null = local)
       separator pipe
```

Schemes soportados: `station:`, `local:`, `slot:`, `hierarchy:`, `history:`, `alarm:`, `fox:`, `http:`.

Client-side vs server-side:
```javascript
Ord.prototype.resolve = function(obj) {
  const ordQueries = this.parse();
  const canLocal = ordQueries.isClientResolvable() && !obj.forceServerResolve;
  if (canLocal) {
    ordQueries.getCursor().resolveNext(target, options);
  } else {
    // RPC a BOX /ord channel
    const cb = new Callback(...);
    cb.addReq("ord", "resolve", { ord: ordString });
    cb.commit();
  }
};
```

Schemes JS: `rc/baja/ord/{Station,Slot,Hierarchy,Local,Fox}Scheme.js`. FoxScheme delega al servidor siempre.

### Subscriber (suscripciones live)

```javascript
const sub = new baja.Subscriber();
sub.subscribe({ comps: [comp1, comp2] })
   .then(() => console.log("subscribed"));

sub.attach("changed", function(prop, cx) {
  console.log(prop.getName(), "→", this.getOutDisplay());
});

sub.attach("subscribed", function(cx) { ... });

sub.detach("changed");
sub.unsubscribe();
```

Eventos: `changed, added, removed, renamed, flagsChanged, facetsChanged, topicFired, subscribed, unsubscribed, unmount, componentRenamed, componentFlagsChanged, componentReordered`.

Push server-side: `com.tridium.box.BComponentSpaceSessionHandler` envía unsolicited (`r:'u'`). Client-side `BrowserCommsManager` enruta a listeners.

Lease:
```javascript
component.lease({ time: 30000 });   // auto-cancela tras 30s
```

Polling fallback: si WebSocket cae → `baja.comm.poll()` cada 2.5s vía HTTP POST.

---

## 22.12 Protocolo BOX (Building Object eXchange)

Cubierto brevemente en Bloque 19.17 — aquí la vista cliente-side.

### Mensaje BOX

```javascript
const message = {
  r: 'rt',          // request type: 'rt'|'rp'|'e'|'u'
  c: 'sys',         // channel: sys|ord|registry|componentSpace|history|alarm
  k: 'hello',       // key: método remoto
  n: 42,            // # mensaje (monotónico)
  b: { arg:value }  // body JSON
};
```

Tipos: `rp` reply, `e` error (BoxError {code,message,details}), `u` unsolicited (push).

### Frame BOX

```javascript
const frame = {
  p: 'box',         // protocol
  v: '2.3',         // version
  n: 100,           // frame ID
  m: [msg1, msg2]   // array mensajes
};
```

### Muxing para payloads grandes

`BoxEnvelope.js` fragmenta payload:
```javascript
const envelope = BoxEnvelope.makeOutgoing({
  envelopeId: 1,
  sessionId: 'ABC123',
  maxMessageSize: 65536,       // WebSocket max (configurable box.ws.maxTextMessageSize)
  maxEnvelopeSize: 10485760,   // 10 MB total
  payload: largePayload        // Uint8Array
});
const fragments = envelope.getFragments();  // Array<Uint8Array>
// Cada fragment <65536 con overhead 'F2.3;sid;eid;...'
```

Demux server-side: `com.tridium.box.mux.BoxEnvelopeDemux`.

Implicit batching: `BoxMessageRelay.js` debounce ~10ms para coalescer mensajes.

### Handshake

```
1. HTTP POST /box        → crea BServerSession (Java), retorna sessionId
2. WebSocket upgrade     → ws://host/wsbox?sessionId=ABC123
3. Message flow bidi     → servidor puede enviar unsolicited
```

CSRF: sessionId = token; server valida en cada mensaje. Auth cookie HttpOnly+Secure vía login HTTP estándar previo.

### BOX ↔ Fox

- Fox: protocolo binario nativo Niagara (multicast+unicast, station↔station).
- BOX: JSON-over-WebSocket para browsers, muxing + implicit batching.
- Bridge: `com.tridium.box.BFoxBoxChannel` puentea BOX ↔ Fox nativo (Fox scheme en ORDs siempre delega al servidor).

---

## 22.13 Communication patterns (Callback, Batch)

```javascript
const cb = new Callback(
  function ok(result) { ... },
  function fail(err)  { ... }
);
cb.addOk(function(ok, fail, result) {
  ok(transformedResult);          // chain transforms
});
cb.addReq("sys", "hello", {});    // agrega al batch implícito
cb.commit();                       // envía BOX frame

// Promise ES6:
cb.promise().then(...).catch(...);
```

Batch manual:
```javascript
const batch = new baja.comm.Batch();
batch.addReq("componentSpace", "read",  { ord:"..." });
batch.addReq("componentSpace", "write", { ord:"...", value:42 });
batch.commit(callback);
```

**GOTCHA latencia**: debounce BoxMessageRelay ~10ms → puede introducir delay inesperado para mensajes críticos. Usar Batch manual si latency crítica.

---

## 22.14 Component model en browser

```javascript
const comp = target.getComponent();
comp.getName()
comp.getType()                     // baja.Type
comp.getSlot(name)                 // Property | Action | Topic
comp.getSlots()                    // iterator
comp.add(slot)                     // slot dinámico
comp.remove(name)
comp.getNavOrd()
comp.getNavDisplayName()
comp.getNavIcon()                  // module://path/to/icon.png
comp.mount(parent, name)
comp.unmount()
comp.getFlags()                    // readonly/hidden/summary/...
comp.getFacets()                   // metadata: range/units/format
```

Slot types:
```javascript
// Property
comp.getSlot("value").get()  → Promise<value>
comp.getSlot("value").set(newVal) → Promise<void>

// Action
comp.getSlot("invoke").invoke() → Promise<result>

// Topic (pub/sub)
comp.getSlot("topicName").fire(event)
sub.attach("topicFired", handler)

// ControlPoint (writable + history)
comp.getSlot("out").get()/set(newVal)

// DynamicProperty (runtime add/remove)
comp.add(new baja.Property("dynamic", baja.Integer.make(0)))
```

---

## 22.15 BajaUI en browser (widget binding)

```javascript
// rc/binding/impl/WidgetBinding.js
const binding = new WidgetBinding({
  widget: domElement,
  targetOrd: "station:|slot:/Panel",
  property: "foreground",
  converter: colorConverter
});
binding.attach().then(() => {
  // widget refleja valor remoto, cambios → RPC al servidor
});
```

Tipos de binding browser: `ValueBinding` (prop↔DOM), `LoadBinding` (carga inicial), `WbViewBinding` (Workbench views), `WbFieldEditorBinding` (forms).

Layout JS:
```javascript
const layout = new bajaui.Layout({
  type: "grid",
  rows: 3, cols: 2,
  hgap: 10, vgap: 10,
  children: [
    { widget, constraints: {row:0, col:0} },
    { widget, constraints: {row:0, col:1} }
  ]
});
```

Sizes: `relative(%)`, `fixed(px)`, `preferred()`.

---

## 22.16 Bajadoc — generación de documentación

### Pipeline Java → TypeSpec JSON

Clase generadora: `com.tridium.bajascript.ux.util.CommonTypeLibGenerator`.

Introspeccion vía reflection de `@NiagaraType` y `@NiagaraProperty`:
```java
@NiagaraType(name = "MyComponent", module = "myModule")
public class BMyComponent extends BComponent {
  @NiagaraProperty(
    displayName = "Output Value",
    facets = @Facets(min=0, max=100, units="units$percent"),
    flags = @Flags(readonly = true)
  )
  private BNumericWritable out;
}
```

Output JSON (embed en `ctypes.js`):
```json
{
  "myModule:MyComponent": {
    "name": "MyComponent",
    "displayName": "My Component",
    "description": "...",
    "slots": {
      "out": {
        "displayName": "Output Value",
        "type": "baja:NumericWritable",
        "facets": { "min":0, "max":100, "units":"units$percent" },
        "flags": { "readonly":true }
      }
    },
    "baseType": "baja:Component"
  }
}
```

### HTML output — docDeveloper-doc.jar

JSDoc Toolkit integrado en gradle build → HTML con TOC + búsqueda + indexación.

JSDoc samples embebidos en JS source:
```javascript
/**
 * Resolve an ORD to a Component.
 * @param {Object} obj
 * @param {String} obj.ord
 * @param {Boolean} [obj.lease=false]
 * @param {Number} [obj.leaseTime]
 * @returns {Promise<baja.OrdTarget>}
 */
Ord.prototype.resolve = function(obj) { ... };
```

Salida: `doc/jsdoc/bajaScript-ux/{baja.Component,baja.Subscriber,module-baja_ord_Ord,baja.Type,...}.html` (500+ files).

### Module loader (RequireJS)

```javascript
define(["bajaPromises", "bajaScript/baja/comm/BoxError", ...],
  function(bajaPromises, BoxError, ...) {
    return baja;
  }
);
require(['bajaScript/sys'], function(baja) {
  window.baja = baja;
});
```

Resolución:
- `bajaScript/sys` → `rc/sys.js`
- `bajaScript/baja/ord/Ord` → `rc/baja/ord/Ord.js`
- Dev: HTTP GET por módulo (DevServer servlet) + source maps
- Prod: `bs.built.min.js` (bundle completo minificado, 360 KB → ~80 KB gzipped)

---

## 22.17 Workbench-browser embedding (JxBrowser)

Install path `C:\Honeywell\OptimizerSupervisor-N4.14.0.162\JxBrowser\` — Workbench embebe **Chromium** vía JxBrowser para renderizar PX/Hx views nativamente dentro del Workbench.

Acceso API plataforma desde BajaScript-en-Workbench: `niagara.env.*` injections (no disponibles en browser puro).

---

## 22.18 CentralineAhuPx (custom widget suite Honeywell)

`modules/CentralineAhuPx-wb.jar` (392 KB). Widget library custom HVAC — categorías `.px` + paletas:
- Coils (bobinas HVAC)
- Dampers (compuertas)
- Ducts (conductos)
- Fans (ventiladores)
- Filters
- HeatRecovery
- Humidifier
- Sensors

Cada categoría: iconos `.px` + `module.palette` con widgets pre-configurados.

Qué agrega:
- Iconografía HVAC estándar Honeywell
- `BNPxInclude` con `fill` → mapear estados a colores
- Composición de componentes HVAC
- Pre-built bindings a puntos estándar AHU (temp, pressure, status)

Otros similares: `galileoKitPx-wb.jar`, `CentralineHtgPx-wb.jar`.

---

## 22.19 PX vs Hx (web migration)

| Aspecto | PX (Workbench) | Hx (Web) |
|---|---|---|
| Format | XML `.px` | mismo XML + media hint |
| Runtime | Swing/AWT | HTML5/Canvas/DOM |
| Widget suite | Full (100+) | Subset (los con HxAgent) |
| Layout | Absolute + flow | CSS-based |
| Styling | BBrush/BColor/BFont | CSS classes |
| Responsive | Menor | Mejor (BUxResponsivePane) |
| Caching | PxCache en-memoria | CDN + browser cache |

Media target en XML:
```xml
<px media="workbench:WbPxMedia">    <!-- desktop only -->
<px media="hx:HxPxMedia">            <!-- web compatible -->
```

PxEditor valida: si widget usado no tiene HxAgent y media = HxPxMedia → warning.

Migración PX → web:
1. Reemplazar widgets no-HxAgent con HxAgent equivalents
2. Convertir absolute layouts a CSS flex/grid
3. Testear en HxPxMediaMode preview

---

## 22.20 Security en PX/Web

### PX view

```java
class BAbstractPxView {
  Property requiredPermissions (BPermissions);
}
```

Chequeos:
- `BPxInclude.load()` → read permission en file
- `OrdTarget.bind()` → read permission en binding target
- `BValueBinding` → hide hyperlink si no write

Issues históricos (changelogs):
- NCCB-632: HxPx Binding no chequeaba read permissions
- NCCB-643: HxPxSlider sin permission check
- NCCB-758: pxInclude no enforzaba permissions
- #16259: BHxPxValueBinding mangled permissions

### BajaScript CSRF

- sessionId = CSRF token implícito en BOX envelope
- Auth cookie HttpOnly+Secure (vía login HTTP previo)
- Server-side: cada BBoxChannel respeta RLS user
- Client-side: ORD resolution falla si no permissions
- NO hay cryptografía en BOX → confía en HTTPS/WSS

### Display permissions

No hay atributo `password` nativo en PX. Custom `BTextInputWidget` + binding con display formatter. `BFormat.display(value, user, permissions)` oculta si no read perm.

---

## 22.21 Gotchas operacionales transversales

### PX rendering

1. **ORD resolution sincrónico en render** — si target tarda (network lookup), UI bloquea. Cache result en OrdTarget, retry async.
2. **Subscriptions leak** — cada BBinding mantiene suscripción. 100+ bindings = 100+ subscriptions. Cleanup via `BBinding.stopped()` cuando widget destroy.
3. **PxCache MAX_CACHE_SIZE configurable** — eviction LRU; aumentar si hay muchos dashboards grandes.
4. **File mtime hotload** — `BPxInclude.isReloadRequired()` compara `lastModified`. Auto-reload en Workbench live editing puede flickerar si edits rápidos.
5. **Layout thrashing** — cambios múltiples a properties congeladas → múltiples layout passes. Batch con BEGIN_BATCH/END_BATCH topics.
6. **Bindings override widget** — `BWidget.changed(Property, Context)` chequea `isOverriddenByBinding` antes de propagar. Widget value local es ignorado si hay binding.

### BajaUI threading

7. **EDT vs engine thread** — mouse/key events en EDT; `BBinding.started()` puede correr en engine thread. Sincronizar shared state.
8. **Memory leaks: BBinding.registry (WeakHashMap)** mitiga pero verify en debug.
9. **Circular refs widget → binding → target → widget** — cuidado con `stopped()` cleanup.
10. **Layout infinite loop** — llamar `relayout()` en `computePreferredSize()` causa recursión. `BBorderPane.getAccumulatedBorder()` también peligroso.
11. **NO bubbling de eventos** — cada widget maneja el suyo. Propagación explícita con `parent.fireKeyEvent(e)`.

### BajaScript browser

12. **Single-threaded event loop** vs station multi-threaded → cambios concurrentes reordenables en tránsito.
13. **Message ordering** — BOX preserva orden dentro de frame pero unsolicited intercala. Usar request IDs para matching.
14. **Type lazy resolution** — network RPC si tipo no en ctypes. Chequear tipo early en promesa.
15. **Subscription no persiste tras hard refresh** — UI debe guardar ORDs, NO referencias a componentes.
16. **BoxEnvelope limit** — `box.ws.maxTextMessageSize` default ~64KB, configurable.
17. **Implicit batching debounce ~10ms** — delay inesperado. Manual Batch para crítico.
18. **FoxScheme siempre RPC** — ORDs `fox:` nunca resuelven localmente, impacto en topologías multi-estación.
19. **BSON, no JSON puro** — box-rt usa BSON para complex objects. Client BsonDecoderPlugin descodifica.
20. **RequireJS case-sensitive** — `bajaScript/sys` ≠ `bajaScript/Sys`. Errores silenciosos si mal nombrado.
21. **Stale refs post-reconnect** — referencias guardadas mueren con `location.reload()`. Re-resolver ORD siempre:
    ```js
    // MAL: oldComponent.getSlot("value").get() tras reload → error
    // BIEN: baja.Ord.make("...").resolve({lease:true}).then(t => t.getComponent())
    ```
22. **JxBrowser en Workbench** — acceso a `niagara.env.*` injections que NO existen en browser puro.

---

## Fuentes primarias leídas

1. `modules/bajaui-rt.jar` + `bajaui-wb.jar` (1.36 MB) + `bajaui-ux.jar` (271 KB)
2. `modules/workbench-rt.jar` + `workbench-wb.jar` (BWbPxView, BPxEditor)
3. `modules/bajaScript-ux.jar` (205 JS files, 360 KB bundle)
4. `modules/box-rt.jar` (BBoxService, BComponentSpaceSessionHandler, BFoxBoxChannel, mux/BoxEnvelopeDemux)
5. `modules/docDeveloper-doc.jar` (doc/jsdoc/bajaScript-ux/*.html 500+)
6. `modules/CentralineAhuPx-wb.jar` (custom HVAC widgets)
7. `modules/galileoKitPx-wb.jar`, `CentralineHtgPx-wb.jar`
8. `modules/webChart-*.jar`, `webEditors-*.jar`
9. `defaults/workbench/newfiles/{PxFile,ReportPxFile}.px` (templates reales)
10. `niagara-help/devguide/px.html` + `devguide-clean/px.txt`

Total: ≈1500 clases decompiladas, 205 JS files analizados, specs BOX protocol verificadas, Theme system (172 clases) + 100+ widget classes + 3 pipelines (PX parse/bind/paint) documentados.
