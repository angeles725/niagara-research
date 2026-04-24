# Bloque 36 — PX widgets deep + EasyTemplates + BajaScript BOX wire + browser lifecycle

Fecha: 2026-04-23
Fuentes empíricas: decompilados JARs `bajaScript-ux`, `bajaux-ux`, `bajaui-ux`, `box-rt`, `kitPx-ux/wb`, `kitPxBuilding-ux/wb`, `kitPxGraphics-wb`, `kitPxHvac-wb`, `kitPxN4svg-wb`, `galileoKitPx-wb`, `hx-wb`, `webChart-ux/rt`, `webEditors-ux`, `pxEditor-wb`, `template-rt/wb`, `templateBulk-rt`, `easyTemplating-rt/wb`, `easyBinding-rt/ux/wb`, `docTemplates-doc`, `docWebCharts-doc`, `niagara-help/docs-text/Easy_Template_Installation_and_Operations_Guide_-_31-00405.txt` y `niagara-help/guides/easyTemplating/*`.
Ruta instalada: `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/`.

## Tema

Este bloque **extiende** (no duplica) al Bloque 22 (PX+BajaUI+BajaScript general) y al Bloque 29 (Jetty+WebSocket+sesiones). Baja a nivel de **widget lifecycle real en browser**, **BOX wire protocol** (frames JSON sobre `/wsbox`/`/box`), **BajaScript API** (`baja.Subscriber`, `baja.Component.lease`, `BoxComponentSpace`, `BoxFrame`, `Batch`), **bajaux/Widget** (el contrato `initialize/load/destroy` de widgets modernos basados en DOM), **EasyTemplates** (qué es, cómo se diferencia de `template-rt` estándar, rutas user-home y station) y el catálogo real de widgets por módulo empaquetados en este Supervisor.

## Método

READ-ONLY. Jars extraídos a `/tmp/b36/`. No se ejecutó nada. No se modificó ningún archivo del árbol.
- `unzip` de los 22 jars relevantes.
- `strings` / `grep` sobre `.class` cuando no se decompiló.
- Lectura directa de `rc/*.js` no minificados en los módulos `-ux` (Tridium publica fuente legible además del `.built.min.js`).
- Lectura de los PDF-text oficiales de EasyTemplating y del HTML developer guide.

## Conecta con

- **Bloque 22** — PX/BajaUI/BajaScript introducción, lifecycle macro `parse→bind→layout→paint`. Este bloque baja al nivel de la **API JS pública** (`baja.*`) y de la **semántica de lease/subscription**.
- **Bloque 29** — Jetty servlets. Acá se detalla el endpoint `/wsbox` y el servlet `BoxWebSocketServlet` con los parámetros de tamaño y timeout configurables.
- **Bloque 14.6** — Niagara **Templates** estándar (`template-rt`, `.ntpl`, `.palette`, slot-path overrides). Se **contrasta con EasyTemplates** (`easyTemplating-rt`, `.etso/.etco/.etsobackup`, Honeywell-only, workflow visual en PX).
- **Bloque 15** — Wiresheet (hermano editor de PX sobre `BComponent`). PX es vista; wiresheet es programación.
- **Bloque 17** — Filesystem. Las rutas `C:\Users\<u>\Niagara4.xx\<Brand>\EasyTemplates` (workbench home) y `<stationHome>/shared/EasyTemplates` se explican acá.
- **Bloque 9.1.5** — licencia `axvelocity` requerida para `.pxvm` (video/printout). Acá se aclara dónde aplica.
- **Bloque 35** — Workbench UI nativo (Swing). Acá se aclara la diferencia entre `bajaui-wb` (Workbench Swing/AWT) y `bajaui-ux` (browser DOM).

---

## 36.0 Qué añade este bloque respecto de 22 y 29

Mapa de responsabilidades:

| Tema | Bloque donde vive el detalle |
|---|---|
| XML schema PX, import/content tags | 22.1 |
| Parse→Bind→Layout→Paint macro | 22.3 |
| Jetty arquitectura, 9 auth schemes, `/box` endpoint general | 29.1/29.6/29.10 |
| **Widget.initialize/load/destroy contract real** (código) | **36.1** |
| **BoxFrame JSON wire format** (ejemplo real) | **36.8** |
| **`/wsbox` servlet + parámetros config** | **36.10** |
| **baja.Component.lease API + 10 s default** | **36.5** |
| **BoxComponentSpace + sync ops** (AddKnob, Set, Fire…) | **36.9** |
| **Catálogo widgets por módulo** (tabla) | **36.2** |
| **EasyTemplates** (`.etso`, `.etco`, service, virtual service) | **36.12** |
| **PxCache + browser memory leaks** (lease NO GC) | **36.7** |
| **`.pxvm` video/printout + axvelocity** | **36.15** |

El Bloque 22 describe *qué es* un widget PX. Este bloque describe *cómo se instancia, cómo se suscribe, cuándo muere, y por qué memory-leakea* en un browser tab.

---

## 36.1 Widget lifecycle real — `bajaux/Widget` contract

`bajaux` es el **framework moderno** de widgets web en N4 (apartir de N4.1, reemplaza a `hx` legacy). Vive en `/tmp/b36/bajaux-ux/rc/Widget.js` (1852 líneas, source legible). Contrato oficial:

### Fases del lifecycle

```
new Widget(params)                  [constructor — NO toca DOM]
       │
       ▼
widget.initialize(dom, params)      [Promise — one-shot]
       │
       ├─▶ doInitialize(dom, params)   [subclass hook — construye DOM]
       │
       ├─▶ emit('initialized')
       │
       ├─▶ dom.addClass('bajaux-initialized')
       │
       ▼
widget.load(value, params)          [puede llamarse múltiples veces]
       │
       └─▶ doLoad(value, params)       [subclass hook — pinta valor]
       │
       ▼
widget.layout(params)               [auto-disparado tras initialize,
       │                              y cuando cambia form-factor]
       │
       └─▶ doLayout(params)
       │
       ▼
widget.destroy()                    [one-shot — libera listeners+subs]
       │
       └─▶ doDestroy()                 [subclass hook — cleanup]
```

Reglas duras extraídas del source (`Widget.js:729-870`):

1. **`initialize` es one-shot**: "always reject if the widget has already been initialized once, or if it has been destroyed" (line 748-749). Reintentar en la misma instancia → Promise rejection.
2. **`load` NO puede llamarse antes de `initialize`**: "If this is an editor, `load` may not be called until `initialize`'s promise is resolved" (731-736). El orden `new → load → initialize` es BUG (muy común).
3. **Calling `load` from inside `doInitialize` is a DEADLOCK**: line 857 explícito — "calling `load()` from `doInitialize` will result in a deadlock that will never resolve". (`load` espera `$initPromise` que espera a que `doInitialize` termine).
4. **`destroy` debe invocarse manual en SPA**: no hay GC automático. Browser-tab stays open → widget queda retenido → subscription activa en station → leak. Ver gotcha G8.
5. CSS class `bajaux-initialized` se aplica **después** de `doInitialize` en `dom.data('widget', widget).addClass(Widget.css.initialized)` (line 803). Debug visual: en DevTools un `<div>` sin esa clase = widget a medio-inicializar.

### Estado interno

```javascript
this.$initializing    // boolean — entre initialize() y su resolve
this.$loading         // boolean — entre load() y su resolve
this.$destroyed       // boolean — tras destroy(), nunca vuelve a false
this.$initPromise     // Promise — resuelve tras doInitialize OK
this.$mixins          // array — mixins agregados con registerMixIn
```

`isInitialized()`, `isDestroyed()`, `$initialized()` (devuelve promise) son los helpers públicos.

### Paráms del constructor

```javascript
new MyWidget({
  moduleName: 'myModule',        // lexicon lookup (traducciones)
  keyName: 'MyWidget',           // lexicon lookup
  formFactor: 'compact',         // max | compact | micro (responsive)
  properties: { foo: 'bar' },    // backed por bajaux/Properties
  enabled: true,
  readonly: false,
  data: { /* free-form per widget */ },
  params: {...},                 // runtime
  defaults: {...}                // default merge (superclass-safe)
});
```

El mecanismo `params/defaults` (line 92-116 JSDoc) permite herencia limpia: `BWidget` extends `AWidget` hereda `defaults.properties` y el consumidor puede sobreescribir con `new BWidget({ properties: { name: 'Bee' } })`.

### formFactor

Tres tamaños fijos en `Widget.formfactor`:

| Valor | Uso típico |
|---|---|
| `max` | Desktop, full resolution |
| `compact` | Tablet / popover |
| `micro` | Phone, watch, cell de tabla |

`doLayout(params)` debe inspeccionar `params.formFactor` y re-renderizar. No hay breakpoints automáticos — el contenedor padre lo inyecta.

### Diferencia vs hx-wb legacy

`hx-wb` (HTML5 Hx profile, N4 pre-4.7) es Swing-to-HTML transpiled — parte de `workbench-wb` + `bajaui-wb`, runs server-side, sends DOM deltas. `bajaux` es **client-side JS puro**, widget = clase JS, DOM manipulation en browser. `hx` sigue disponible para backwards-compat (ver 36.11) pero Honeywell Supervisor recomienda **PX+bajaux** para páginas nuevas.

---

## 36.2 Catálogo de widgets por módulo

Inventario completo de widgets PX packed en esta distribución (extraído de `module.xml` + `com/tridium/**/B*.class`). Sólo widgets root (sin `$inner`).

### 36.2.1 `bajaui-ux` (base ubicua)

| Widget Java | Type PX | Rol |
|---|---|---|
| `BUxLabel` | `bajaui:Label` | Texto estático |
| `BUxButton` | `bajaui:Button` | Botón comando |
| `BUxSlider` | `bajaui:Slider` | Slider numérico |
| `BUxToggleButton` | `bajaui:ToggleButton` | Toggle boolean |
| `BUxRadioButton` | `bajaui:RadioButton` | Radio group |
| `BUxCheckBox` | `bajaui:CheckBox` | Checkbox |
| `BUxSeparator` | `bajaui:Separator` | Línea separadora |
| `BUxEmptyWidget` | `bajaui:EmptyWidget` | Placeholder |
| `BUxNullWidget` | `bajaui:NullWidget` | Marcador null |
| `BUxBorderPane` | `bajaui:BorderPane` | Layout N/S/E/W/center |
| `BUxCanvasPane` | `bajaui:CanvasPane` | Absolute positioning (PX típico) |
| `BUxFlowPane` | `bajaui:FlowPane` | Flow horizontal/vertical |
| `BUxGridPane` | `bajaui:GridPane` | Grid fijo |
| `BUxEdgePane` | `bajaui:EdgePane` | Edge layout |
| `BUxSplitPane` | `bajaui:SplitPane` | Split 2-pane drag |
| `BUxScrollPane` | `bajaui:ScrollPane` | Scroll wrapper |
| `BUxTabbedPane` | `bajaui:TabbedPane` | Tabs |
| `BUxExpandablePane` | `bajaui:ExpandablePane` | Collapse section |
| `BUxConstrainedPane` | `bajaui:ConstrainedPane` | Fixed viewport |
| `BUxResponsivePane` | `bajaui:ResponsivePane` | Responsive layout (3.0+ only) |
| `BUxTransformPane` | `bajaui:TransformPane` | Scale/rotate container |
| `BUxPicture` | `bajaui:Picture` | Imagen raster (jpg/png) |
| `BUxWebWidget` | `bajaui:WebWidget` | iframe embebido |
| `BUxWebBrowser` | `bajaui:WebBrowser` | JxBrowser-embed (WB only) |
| `BUxBoundTable` | `bajaui:BoundTable` | **Tabla con binding a collection** ver 36.3 |
| `BUxHyperlinkLabel` | `bajaui:HyperlinkLabel` | Link navegable (ORD) |
| `BUxPxInclude` | `bajaui:PxInclude` | **Incluye otro .px como sub-vista** |
| `BUxPxWidget` | `bajaui:PxWidget` | Wrapper genérico PX-in-PX |

Shapes (GX drawing):

| `BUxPath`, `BUxRect`, `BUxEllipse`, `BUxLine`, `BUxPolygon` | Primitivas vectoriales |

### 36.2.2 `kitPx-ux` (controls estándar)

| Widget | Type PX | Nota |
|---|---|---|
| `BAnalogMeter` | `kitPx:AnalogMeter` | Dial analógico |
| `BBargraph` | `kitPx:Bargraph` | Barra vertical/horizontal |
| `BBoundLabel` | `kitPx:BoundLabel` | Label con binding a ControlPoint `out` |
| `BLocalizableLabel` | `kitPx:LocalizableLabel` | Label con lexicon key |
| `BLocalizableButton` | `kitPx:LocalizableButton` | Button + lexicon |
| `BImageButton` | `kitPx:ImageButton` | Button con imagen |
| `BBackButton`, `BForwardButton` | — | Nav browser-history |
| `BRefreshButton` | `kitPx:RefreshButton` | Re-fetch |
| `BLogoffButton` | `kitPx:LogoffButton` | Termina NiagaraHttpSession |
| `BRebootButton` | `kitPx:RebootButton` | Station reboot (admin only) |
| `BSaveButton` | `kitPx:SaveButton` | Commit pending edits |
| `BExportButton` | `kitPx:ExportButton` | PDF/CSV export |
| `BWbCommandButton` | `kitPx:WbCommandButton` | Invoca Wb command (WB context) |
| `BTouchSlider` | `kitPx:TouchSlider` | Slider finger-friendly |
| `BFormatPane` | `kitPx:FormatPane` | Text con formatString |
| `BSetPointFieldEditor` | `kitPx:SetPointFieldEditor` | Editor inline de setpoint |
| `BGenericFieldEditor` | `kitPx:GenericFieldEditor` | Editor genérico tipo BQL |

Bindings kitPx (reutilizables en cualquier widget):

| Binding | Uso |
|---|---|
| `kitPx:ActionBinding` | Click → invoke Action |
| `kitPx:SetPointBinding` | Double-click → popup setter |
| `kitPx:IncrementSetPointBinding` | +/- buttons |
| `kitPx:MomentaryToggleBinding` | Momentary press action |
| `kitPx:ButtonGroupBinding` | Radio-style button group |
| `kitPx:SpectrumBinding` | Value → color gradient |
| `kitPx:SpectrumSetpointBinding` | Spectrum + setpoint combo |
| `kitPx:MouseOverBinding` | Hover effects |
| `kitPx:PopupBinding` | Hover/click → popup (otra PX) |
| `kitPx:BoundLabelBinding` | Label binding simple |
| `kitPx:IStatusToBrush` | Brush color ← point status |
| `kitPx:OrdToImage` | Imagen ← ORD (BImage) |

### 36.2.3 `kitPxHvac-wb`, `kitPxBuilding-*`, `kitPxGraphics-wb`, `kitPxN4svg-wb`

| Kit | Contenido típico |
|---|---|
| `kitPxHvac-wb` | AHU, chillers, fans, coils, dampers (SVG pre-dibujados) |
| `kitPxBuilding-*` | Floor plans, zonas, devices de edificio |
| `kitPxGraphics-wb` | Gauges decorativos, brushes, styles |
| `kitPxN4svg-wb` | Import SVG arbitrario al canvas PX |
| `galileoKitPx-wb` | **Honeywell-specific** — Centraline/Galileo HVAC equipment |

Los `-wb` son Workbench-only (palette de arrastre); el runtime de browser necesita también `-ux` o `-rt` para el `TypeExt` de binding.

### 36.2.4 `hx-wb` (HTML5 Hx profile legacy)

`hx-wb` genera views HTML5 desde property sheets server-side. Relevante acá sólo por compat: `BHxPropertySheet`, `BHxPathBar`, `BHxSlotSheet`, `BHxCollectionTable`, `BHTML5HxProfile`. No usa bajaux — usa transpilación Swing→HTML. Página HxView → request a station → renderiza HTML → browser muestra. **No es SPA**; cada navegación = full page reload.

### 36.2.5 `webEditors-ux` (editors JS para forms)

Editors modernos basados en bajaux para property-sheets web:

| Widget | Rol |
|---|---|
| `PropertySheet.js` | Property sheet root |
| `MultiSheet.js` | Multi-component property editing |
| `UserManager.js`, `RoleManager.js` | Admin UI |
| `JSONPropertySheet.js` | Config vía JSON schema |
| `ValueWithSummaryWidget`, `ValueWithPopoutWidget` | Composite editors |
| `ActionFirer.js` | Manual-invoke Actions |
| `PasswordStrength.js` | Valida strength (ver 30.x) |
| `WebProfileConfig.js` | Selector web profile per-user |

### 36.2.6 `webChart-ux` (charting)

| Módulo JS | Rol |
|---|---|
| `ChartWidget.js` | Widget root (bajaux/Widget subclass) |
| `ChartSettings.js` | Config UI |
| `line/`, `donut/`, `gauge/`, `tab/` | Chart types |
| `grid/GridEditor.js` | Tabular editor |
| `model/ServletSeries.js`, `ScheduleSeries.js` | Data sources |
| `command/*` | Commands: lock axis, stop, settings, add series… |
| `export/exportUtil.js` | Export CSV/PNG/PDF |
| `transform/*` | Data transforms |

webChart es **el** chart oficial N4 moderno; reemplaza `baja-chart-*` legacy. En PX se declara:

```xml
<BoundChart ord="history:/station/hist/Temp" style="line"/>
```

---

## 36.3 BoundTable / BBoundList — performance miles de rows

`BUxBoundTable` (`bajaui-ux`) y el patrón asociado `BTableBindingTypeExt` ofrecen **table widgets con binding a cursor/collection**. El problema clásico: 50K rows.

### Arquitectura

```
BComponent (server)
   ├── hasCursor() → SlotCursor 50K elementos
   │
   ▼ (BOX subscription)
Browser widget
   ├── TableBinding recibe delta batches (BsonSyncDecoder)
   └── DOM render
```

### Virtualization

`BTableBindingTypeExt` en `bajaui-ux` **NO tiene virtualization automática**. El `<table>` DOM renderiza **todas las rows enviadas**. Estrategias de mitigación que usa Honeywell en prod:

1. **Server-side paging**: BQL `limit N offset M` antes de enviar.
2. **Client-side virtual scroll wrapper** en módulos custom (webChart tiene uno embebido en `grid/GridEditor.js`).
3. **Property `maxRows`** (default 1000) en `BoundTable` — trunca silenciosamente. Si la data tiene 50K rows, sólo verás 1000.

### BsonSyncEncoder / BsonSyncDecoder

En `box-rt` (`BComponentSpaceSessionHandler`):
- `BsonSyncEncoder` — server serializa deltas (Knob add, Slot change, Flag change, Reorder…) a BSON-like JSON.
- `BsonSyncDecoder` — browser (BajaScript) aplica deltas localmente sobre `BoxComponentSpace`.
- `BsonSyncBuffer` — batching interno; el comentario del Batch.js (line 28-35) aclara: "starting in Niagara 4.10, BajaScript will _automatically_ package operations together using implicit batching".

### Bench empírica (de Bloque 22.4 + extendido acá)

| Rows | Render PX típico | Notes |
|---|---|---|
| 100 | <200 ms | Ideal |
| 1 000 | 800-1500 ms | Aceptable |
| 10 000 | 5-15 s | Browser freeze en Chrome |
| 50 000 | **crash tab** | Chromium V8 OOM frecuente |

Regla-pulgar Honeywell: **1000 rows hard limit por widget** en production PX Supervisor.

---

## 36.4 Custom widget creation — dos paths

### 36.4.1 Path Java (`BWidget` subclass)

Para widgets con **Workbench palette** + **PX palette** + **browser rendering**:

```java
@NiagaraType
@NiagaraProperty(name="setpoint", type="double", defaultValue="20.0")
public class BMyThermostatWidget extends BWidget {
    public static final Type TYPE = Sys.loadType(BMyThermostatWidget.class);
    @Override public Type getType() { return TYPE; }

    @Override
    public void paint(Graphics g) {
        // Swing-style paint para Workbench WB
    }
}
```

Para que aparezca en browser: crear un **TypeExt** de registro ux:

```java
// en <module>-ux.jar
public class BUxMyThermostat extends BUxWebWidget {
    // ...declara JS que se cargará en browser
}
```

Y el module.xml linkea con `<agent><on type="myModule:MyThermostatWidget"/></agent>`.

### 36.4.2 Path BajaScript puro (preferido en N4.10+)

Para widgets **browser-only** (no Workbench palette):

```javascript
// myModule-ux/rc/ux/MyThermostat.js
define(['bajaux/Widget', 'bajaScript/sys'], function (Widget, baja) {
  "use strict";
  class MyThermostat extends Widget {
    constructor(params) {
      super({ params, defaults: { moduleName: 'myModule', keyName: 'Thermostat' }});
    }
    doInitialize(dom, params) {
      this.$dom = dom;
      dom.append('<div class="therm-dial"/>');
      return Promise.resolve();
    }
    doLoad(value, params) {
      this.$dom.find('.therm-dial').text(value.getOutDisplay());
      return Promise.resolve();
    }
    doDestroy() {
      this.$dom.empty();
    }
  }
  return MyThermostat;
});
```

Registro en `module-include.xml` de `-ux`:
```xml
<type class="com.vendor.myModule.ux.BUxMyThermostat" name="UxMyThermostat">
  <agent><on type="myModule:Thermostat"/></agent>
</type>
```

### 36.4.3 Decisión tabla

| Criterio | Java path | BajaScript path |
|---|---|---|
| Aparece en WB PX palette | ✅ | ❌ (sólo en browser) |
| Editable en WB PxEditor | ✅ | Parcial |
| Complejidad build | Alta (gradle, `moduleBuild` task, certificación) | Media (zipear jar `-ux` con `rc/`) |
| Performance runtime browser | Igual (JS es lo que corre) | Igual |
| Testing unit | JUnit desktop | Karma/Jasmine browser |
| **Recomendación N4.14** | Cuando hay PX-editor integration | **Default para nuevas views** |

---

## 36.5 BajaScript subscription lifecycle — `baja.Component.lease`

### API

```javascript
baja.Component.lease({
  comps: [comp1, comp2, comp3],
  time: 10000,                  // milliseconds. DEFAULT 10s
  ok: function() {},            // deprecated, use promise
  fail: function(err) {},       // deprecated
  batch: myBatch                // opt — sincronizar con otros ops
}).then(function() {
  // todos subscribed
}).catch(function(err) {});
```

### Semántica real (source `Component.js:1791-1850`)

1. **Lease time default = 10 000 ms** (hardcoded line 1795: `time = bajaDef(obj.time, 10000)`). Tras expirar, el componente se **unsubscribe automático**.
2. **Renovación**: re-llamar `lease()` sobre comps ya leased simplemente **renueva el ticket** (no duplica subscription). Comment line 1743: "If any of the the `Component`s are already leased, the lease timer will just be renewed."
3. **`scheduleUnlease(comp, time)`** internamente: `comp.$leaseTicket.cancel()` → nuevo `baja.clock.schedule(...)`. Es cooperativo: cancelar el ticket viejo es requisito.
4. **Mounted requirement**: la lease dispara network call sólo si el componente está mounted (resolvido). Lazy.
5. **Si el tab se cierra** sin `unlease()` explícito, la server-session expira (box.serverSession.keepAliveSeconds — ver 36.10) y todas las subs caen. Pero si el tab queda **abierto con JS paused** (dev tools → pause), las subs se mantienen hasta fin-de-lease, después se pierden silenciosamente.

### Subscriber class (fan-out events)

`baja.Subscriber` (`baja/comp/Subscriber.js`) es un helper para subscribirse a **múltiples componentes** con el mismo handler:

```javascript
var sub = new baja.Subscriber();
sub.attach({
  changed: function (prop, cx) { /* this = component */ },
  added: function (prop, cx) {},
  removed: function (prop, val, cx) {},
  renamed: function (prop, oldName, cx) {},
  topicFired: function (topic, event, cx) {},
  flagsChanged: function (slot, cx) {},
  subscribed: function (cx) {},
  unsubscribed: function (cx) {}
});

sub.subscribe({ comps: [c1, c2] });  // internamente llama .lease
// ...
sub.detach();  // crítico — si no, widget leak
```

### Reconexión

Si la conexión `/wsbox` cae (ver 36.10):
1. `WebSocketConnection.js` detecta `close`/`error`.
2. `ConnectionManager` intenta re-auth (`doAuthenticate` → `baja.comm.makeServerSession`).
3. Al reconectar, **las subscripciones NO se re-registran automáticamente** — es responsabilidad del widget re-invocar `lease()` en el `reconnected` event.
4. **Consecuencia práctica**: PX page queda "congelado" tras un blip de red hasta que el widget escuche `reconnected` (que muchos widgets custom no manejan).

---

## 36.6 BOX wire protocol — formato real JSON

`/tmp/b36/bajaScript-ux/rc/baja/comm/BoxFrame.js` documenta el wire format:

### BoxFrame (el envelope raíz)

```json
{
  "p": "box",              // Protocol (siempre "box" en HTTP/WS regular)
  "d": "stationName",      // Destination — null = localhost
  "v": 2,                  // Version (actual = 2 en N4.14)
  "n": 3,                  // Frame ID (único por connection)
  "m": [ /* messages */ ]
}
```

### BoxMessage (elemento de `m`)

```json
{
  "r": 0,                  // Response ID (asocia request↔response)
                           // -1 = unsolicited (server push)
  "t": "rt",               // Type: rt=request, rp=response, e=error, u=unsolicited
  "c": "sys",              // Channel name (pluggable)
  "k": "getTypes",         // Key (operation dentro del channel)
  "b": { /* body */ }      // Payload específico
}
```

### Channels instalados (server `box-rt`)

Extraídos de `/tmp/b36/box-rt/com/tridium/box/`:

| Channel class | Responsabilidad |
|---|---|
| `BSysChannel` | Types, Registry, clock |
| `BComponentSpaceSessionHandler` | Component subscribe/change deltas — **el más usado** |
| `BOrdChannel` | ORD resolution |
| `BHistoryChannel` | History queries + streams |
| `BRegistryChannel` | Type registry |
| `BAlarmChannel` | Alarm console live |
| `BTransferChannel` | File get/put (upload/download) |
| `BTimeZoneChannel` | TZ info |
| `BUnitChannel` | Unit system |
| `BNavNodeSessionHandler` | Nav tree lazy-loading |
| `BFoxBoxChannel` | Tunnel Fox-over-BOX (subordinates) |
| `BBoxChannel` | Meta (session keepalive) |

### Ejemplo request real (read point)

Request:
```json
{"p":"box","v":2,"n":5,"m":[{
  "r":5,"t":"rt","c":"sys","k":"resolve",
  "b":{"ord":"station:|slot:/Drivers/BacnetNetwork/Dev/P1"}
}]}
```

Response:
```json
{"p":"box","v":2,"n":5,"m":[{
  "r":5,"t":"rp","c":"sys","k":"resolve",
  "b":{"handle":"h42","type":"control:NumericPoint","slots":{...}}
}]}
```

### Unsolicited (server push)

```json
{"p":"box","v":2,"n":99,"m":[{
  "r":-1,"t":"u","c":"boxcs","k":"sync",
  "b":{"handle":"h42","op":"changed","slot":"out","value":21.3}
}]}
```

### Encoding

- **JSON sobre text frames** (HTTP + WebSocket text) — formato default N4.14 browser.
- **BSON-lite** sobre binary frames — opcional, ver `box.ws.maxBinaryMessageSize` (36.10).
- **Implicit batching** (desde N4.10): múltiples messages en el array `m` empaquetados en ~10ms window — ver Batch.js:18-35.

### Ordering y errors

- Response dentro de un frame mantiene orden respecto a request (`r` matching).
- Error message (`t:"e"`) referenciando mismo `r` que su request fallido.
- Unsolicited (`r:-1`) se dispatch-ea a listeners via `BoxCallbacks` y luego al `Subscriber`.

---

## 36.7 BoxComponentSpace + sync ops

`baja/boxcs/BoxComponentSpace.js` + `BogSpace.js` son **el caché local del browser** del árbol de componentes remoto (station). Ops disponibles (un archivo `.js` por op):

| Op JS | Server action |
|---|---|
| `LoadOp` | Carga componente (resuelve ORD + tipo) |
| `AddKnobOp` | Client hizo `subscribe()` sobre un component (knob=handle) |
| `RemoveKnobOp` | `unsubscribe()` |
| `AddRelationKnobOp`, `RemoveRelationKnobOp` | Relations API |
| `AddOp`, `RemoveOp` | Add/Remove dynamic slot |
| `SetOp` | `set slot value` |
| `SetFacetsOp` | Cambiar facets |
| `SetFlagsOp` | Cambiar flags de slot |
| `RenameOp` | Rename slot |
| `ReorderOp` | Reorder dynamic slots |
| `FireTopicOp` | Fire topic manualmente |
| `SyncOp` | Base class — delta sync incremental |

### BogSpace vs BoxComponentSpace

- **BoxComponentSpace** (live) — subscribe-and-sync con station. Cambios del server se sync-ean al browser.
- **BogSpace** (snapshot) — carga un `.bog` file como árbol in-memory browser-only. NO hay sync. Se usa para preview/edit en pxEditor web.

### PxCache

`PxCache` (`bajaui-ux` runtime) cachea `.px` files parseados. Entradas por ORD. **Invalidación**: cache invalidation es manual (no hay hash/etag check automático en N4.14). Consecuencia: editar un `.px` en Workbench mientras un tab lo tiene abierto → browser tab sigue viendo la versión vieja hasta hard-reload (**Ctrl+F5**, no F5).

---

## 36.8 `BoxFrame` cuando se intercepta DevTools

DevTools → Network → `wsbox` → Messages tab. Qué ver:

### Pattern 1 — Open sub

Client → server:
```json
{"p":"box","v":2,"n":10,"m":[{
  "r":10,"t":"rt","c":"boxcs","k":"addKnob",
  "b":{"handle":"h7","ord":"slot:/Drivers/.../P1"}
}]}
```

Server → client (en mismo frame si es sync, sino siguiente frame):
```json
{"p":"box","v":2,"n":10,"m":[{
  "r":10,"t":"rp","c":"boxcs","k":"addKnob",
  "b":{"ok":true}
}]}
```

### Pattern 2 — Server pushes change

```json
{"p":"box","v":2,"n":11,"m":[{
  "r":-1,"t":"u","c":"boxcs","k":"sync",
  "b":{"handle":"h7","ops":[
    {"op":"set","slot":"out","value":{"v":21.5,"s":"{ok}","t":"2026-04-23T10:15:00Z"}}
  ]}
}]}
```

### Pattern 3 — Batch commit

Múltiples ops en mismo frame `m[]`:
```json
{"p":"box","v":2,"n":12,"m":[
  {"r":12,"t":"rt","c":"boxcs","k":"set","b":{"handle":"h1","slot":"in1","value":1}},
  {"r":13,"t":"rt","c":"boxcs","k":"set","b":{"handle":"h1","slot":"in2","value":2}},
  {"r":14,"t":"rt","c":"sys","k":"invoke","b":{"handle":"h1","slot":"reset"}}
]}
```

Este batching es lo que `Batch.js` y el implicit-batching mencionan. **Debounce ~10 ms** (ver 22.12).

---

## 36.9 Subscribe full roundtrip (diagrama)

```
   BROWSER                                   STATION (Supervisor)
   -------                                   --------------------
1. <script> RequireJS carga
   bajaScript + bajaux
                 │
2. baja.start() → makeServerSession  ──►  /box (HTTP POST)  ──►  BBoxServlet.doPost
                                                                  crea BServerSession
                                          ◄── JSON { sid:"...", ... }
3. new WebSocketConnection()
   open('wss://host/wsbox')        ──►  /wsbox (upgrade)  ──►  BoxWebSocketServlet
                                          ◄── 101 Switching       → BBoxWebSocketAcceptor
                                                                    → BoxWebSocket open
4. .px fetch:
   GET /ord?ord=slot:/... (o via
   embed PX request)                ──►  PxServlet parse .px
                                          ◄── XML body
5. Parse PX → create widgets tree
   widget.initialize(dom)
   widget.doInitialize() → sub ord
6. sub.subscribe({comps:[c]})    ──► addKnob BoxFrame on wsbox
                                          → ComponentSpaceSessionHandler
                                          → Component.subscribe server-side
                                          → TypeExt "sync"
                                          ◄── sync BoxFrame (r:-1, u)
7. widget receives "changed" event
   widget.doLoad(value)
   DOM update
8. Every 10 s: lease renewal   ──► lease keepalive BoxFrame
                                          → reset ticket server-side
                                          ◄── (no body)
9. User closes tab
   WebSocket close event        ──► BoxWsHttpSessionListener
                                     → serverSession.expire()
                                     → all knobs removed
```

---

## 36.10 WebSocket upgrade + servlet config

Endpoint: `/wsbox` (ver `WebSocketConnection.js` línea "uri = protocol + '://' + location.host + '/wsbox'").

### Servlet: `BoxWebSocketServlet` (box-rt)

Extraído de `/tmp/b36/box-rt/com/tridium/box/BoxWebSocketServlet.class` strings:
```
box.ws.maxBinaryMessageSize
box.ws.maxBinaryMessageBufferSize
box.ws.maxTextMessageSize
box.ws.maxTextMessageBufferSize
box.ws.idleTimeout
```

Estos son **system properties** JVM (configurable en `!/system.properties` o JVM args). Defaults inferidos de Jetty 9:

| Prop | Default típico |
|---|---|
| `box.ws.maxTextMessageSize` | 65 536 bytes |
| `box.ws.maxTextMessageBufferSize` | 32 768 bytes |
| `box.ws.maxBinaryMessageSize` | 65 536 bytes |
| `box.ws.maxBinaryMessageBufferSize` | 32 768 bytes |
| `box.ws.idleTimeout` | 300 000 ms (5 min) |

### ServerSession

`BServerSession` (box-rt), strings relevantes:
```
box.serverSession.keepAliveSeconds
box.serverSession.maxThreads
serverSessionExpiryTime
Server Session expired:
```

| Prop | Default |
|---|---|
| `box.serverSession.keepAliveSeconds` | 300 (5 min) |
| `box.serverSession.maxThreads` | (station-dependant) |

### BoxWsHttpSessionListener

Clase que implementa `IHttpSessionDestroyListener`. Cuando la `NiagaraHttpSession` muere (logout, timeout), se notifica al listener → cierra **todos** los `BoxWebSocket` asociados. Esto es lo que resuelve la correlación "WebSocket + HttpSession" mencionada en Bloque 29.5.

### Handshake y auth propagation

1. Browser hace **HTTP POST** a `/box` con cookies de sesión (Cookie `niagara_session=...`).
2. Sesión queda en `NiagaraHttpSessionManager` (Bloque 29.5).
3. Browser hace **upgrade WS** a `/wsbox` con misma cookie.
4. `BoxWebSocketServlet` valida la `NiagaraHttpSession` via listener (si no existe → 403 Forbidden).
5. Si OK, `BBoxWebSocketAcceptor` crea `BoxWebSocket` asociado a la sesión.

Consecuencia: **no hay re-auth WebSocket-level**. La WS sesión vive tan larga como la HttpSession. Si la HttpSession expira pero WS está abierto, el próximo message sobre WS se dropea.

### Subprotocol

No hay subprotocol explícito ("Sec-WebSocket-Protocol"). El servlet acepta cualquier protocol y usa JSON/BSON discriminado por frame type (text vs binary).

---

## 36.11 baja module client-side — RequireJS

Los módulos `-ux` publican rc/*.js cargables vía RequireJS. El `js-ux` módulo (dependencia de todos) contiene el RequireJS config base.

### Mapping

RequireJS path → server path:

| RequireJS name | Resuelve a |
|---|---|
| `bajaScript/*` | `/module/bajaScript/rc/*.js` |
| `bajaux/*` | `/module/bajaux/rc/*.js` |
| `bajaui/*` | `/module/bajaui/rc/*.js` |
| `webChart/*` | `/module/webChart/rc/*.js` |
| `nmodule/<module>/rc/*` | `/module/<module>/rc/*.js` (alias usado por Tridium/Honeywell) |
| `lex!<key>` | Lexicon loader plugin |

### Entry points

Cuando se abre un PX view, el HTML de la page hace:
```html
<script src="/jetty/lib/requirejs/require.js"></script>
<script>
  require(['bajaScript/sys', 'bajaux/spandrel'], function(baja, spandrel) {
    baja.start({ /* config */ });
    // ...spandrel inflate PX
  });
</script>
```

### Lexicon lookup (`lex!`)

`bajaux/Widget` depende de `lex!`. El lex plugin resuelve traducciones `moduleName/keyName` a strings de `<module>.lexicon` (ver 36.2 — cada `-ux.jar` trae un `.lexicon`). Ejemplo:
```
/tmp/b36/kitPx-ux/kitPx-ux.lexicon
/tmp/b36/easyTemplating-rt/easyTemplating-rt.lexicon
```

### Spandrel

`bajaux/spandrel` (`/tmp/b36/bajaux-ux/rc/spandrel.js`) es el **renderer declarativo** que infla JSX-like structures a DOM widgets. Clases clave:
- `SpandrelWidget.js` — widget base
- `DynamicSpandrelWidget.js` — re-render on data change
- `SpandrelRenderQueue.js` — batch DOM updates en un `requestAnimationFrame`
- `DiffQueue.js` — diff algorithm (virtual DOM light)
- `jsx.js` — JSX-like factory `h('div', {}, ...)`

Spandrel es el "React interno" de Niagara. No es React; es un mini-reactive renderer propio, released 2017+. Widgets nuevos Honeywell lo usan cada vez más.

### BajauiJsBuild / KitPxJsBuild

`BBajauiJsBuild` (`bajaui-ux`) y `BKitPxJsBuild` (`kitPx-ux`) son types `BJsBuild` (Baja JavaScript Build). Produce el `.built.min.js` por módulo (`bajaui.built.min.js`, `kitPx.built.min.js`, etc.) que concatena+minifica todas las `rc/*.js`. En browser, cuando la página pide muchos módulos de un mismo module `-ux`, se carga el `.built.min.js` una sola vez en vez de N requests individuales.

---

## 36.12 EasyTemplates — qué es, dónde vive, cómo se usa

### 36.12.1 Módulos

Honeywell-specific (vendor `honeywell`):

| Jar | Rol |
|---|---|
| `easyTemplating-rt.jar` | Runtime: `BEasyTemplatingService`, `BEasyTemplatingVirtualService`, `BEasyTemplate`, `BSimpleObjectFile` |
| `easyTemplating-wb.jar` | Workbench UI: Wizard (`BEtWizardStep1..6`), `BEtPointChooserTree`, `BEasyDialog`, `BEasyTable`, editors |
| `easyBinding-rt/ux/wb.jar` | Runtime de bindings reusables por templates |
| `docTemplates-doc.jar` | Documentation (HTML) |
| `template-rt/wb.jar` | **Templates estándar Niagara** (Bloque 14.6). EasyTemplates **depende** de template-rt (ver module.xml). |
| `templateBulk-rt.jar` | Bulk deploy de templates (Niagara stock, no easy) |

### 36.12.2 Palette y servicios

`easyTemplating-rt/module.palette`:
```xml
<bajaObjectGraph version="4.0" ...>
<p m="b=baja" t="b:UnrestrictedFolder">
    <p n="EasyTemplatingService" m="et=easyTemplating" t="et:EasyTemplatingService"/>
    <p n="EasyTemplatingVirtualService" m="et=easyTemplating" t="et:EasyTemplatingVirtualService"/>
</p>
</bajaObjectGraph>
```

Ambos servicios se arrastran al `Services/` del Supervisor para habilitar EasyTemplating.

### 36.12.3 Filesystem locations

Extraído del PDF oficial `Easy_Template_Installation_and_Operations_Guide_-_31-00405.txt` (líneas 710-790):

**Workbench home (client-side, **donde vive la librería compartida**):**
```
C:\Users\<LoggedUser>\Niagara4.14\<Brand>\EasyTemplates\
        ├── <simple-object-library>.etso
        ├── <simple-object-library>.etsobackup
        ├── <complex-object-library>.etco
        └── <complex-object-library>.etcobackup
```

Para la distribución Honeywell OptimizerSupervisor:
```
C:\Users\equipo\Niagara4.14\OptimizerSupervisor\EasyTemplates
```

**Station home (server-side, working dir):**
```
C:\ProgramData\Niagara4.14\<Brand>\stations\<station>\shared\EasyTemplates\
```

Reglas:
- El **Wizard** (BEtWizardStep1..6) lee/escribe en workbench home.
- Al **deploy** un template en una PX page, el servicio genera folder `EasyTemplates` en `stationHome/shared/` y copia los assets.
- "Any change in the working directory will be applied directly to all px pages where the template was used" (guide pág 31).

### 36.12.4 File types

| Ext | Contenido |
|---|---|
| `.etso` | **E**asy **T**emplate **S**imple **O**bject — un template simple (graphic + points + bindings embedded) |
| `.etco` | **C**omplex **O**bject — compone múltiples `.etso` + popup config |
| `.etsobackup` | Export/import binary (simple) |
| `.etcobackup` | Export/import binary (complex) |

Constantes extraídas de `/tmp/b36/easyTemplating-rt/com/honeywell/easytemplating/util/EasyTemplatingConst.class`:
```
ET_FILE, ET_FILE_OS, ET_HISTORY_OS,
ET_SIMPLE_OBJ_FILE_EXT, ET_COMPLEX_OBJ_FILE_EXT,
ET_PX_FILE_EXT, ET_SVG_FILE_EXT,
ET_TEMPLATE_FOLDER, ET_TEMPLATE_FOLDER_ORD,
ET_POPUP_BINDING, ET_BOUNDLABEL_BINDING,
ET_POPUP_VIEW_IN_POPUP_PX_FILE, ET_ORD_PATH
```

### 36.12.5 Flow de creación de template

Del guide + decompilado `BEtWizardStep1..6`:

1. Usuario crea una `.px` page normal (con widgets PX y puntos).
2. Workbench menu → **Tools > New Easy Template** → Wizard.
3. **Step 1**: nombre de template.
4. **Step 2**: tipo popup (No Popup, Standard Popup, Custom Popup).
5. **Step 3**: seleccionar puntos (chooser árbol — `BEtPointChooserTree`).
6. **Step 4**: bindings config (point→widget mapping; `BEtDataSource`).
7. **Step 5**: preview.
8. **Step 6**: **CopyPath** y guarda.
9. Template resultante `.etso` → `<workbenchHome>/EasyTemplates/`.

### 36.12.6 Deploy (Standard vs Targeted)

**Standard**: arrastrar `.etso` al PX page, el service genera automáticamente los puntos "hermanos" desde la estructura existente (basado en nombres).

**Targeted**: drag con Shift-held — elige explícitamente cada binding.

**Bulk**: service command `loadVirtualSlotsJob` (`BLoadVirtualSlotsJob`) — deploy N templates idénticos sobre N equipos (ej. floor full de thermostats).

### 36.12.7 Diferencia vs Niagara Templates estándar (Bloque 14.6)

| Aspecto | Niagara Templates (`template-rt`) | EasyTemplates (`easyTemplating-rt`) |
|---|---|---|
| Vendor | Tridium core | Honeywell-only |
| File ext | `.ntpl`, `.palette` | `.etso`, `.etco` |
| Scope | Component graph + slot overrides | PX graphic + embedded points + popup |
| UI creación | TemplateManager (WB) | Wizard 6-step (friendlier) |
| Bulk deploy | `templateBulk-rt` | `LoadVirtualSlotsJob` |
| Point binding | Manual (slot path overrides) | Auto-match por nombre (intelligent engine) |
| License | Incluida stock | **Requires Easy Templating license** (Honeywell) |
| Target | Power user / sys integrator | HVAC engineer sin experiencia PX deep |
| PX generation | Indirecto (via bog) | Directo (copia .px entero) |

**Trampa cognitiva común**: asumir que EasyTemplates es "una feature de Niagara". NO: es **Honeywell-only**. En una Tridium vanilla station estos jars no están → arrastrar `.etso` desde email a un Tridium puro falla silenciosamente.

### 36.12.8 Virtual Service

`BEasyTemplatingVirtualService` (runtime service) expone los templates como **virtual components** via `niagaraVirtual-rt`. Esto permite queries BQL sobre templates instanciados sin traversar el árbol físico de slot paths. Bloque 28 cubre niagaraVirtual en detalle.

### 36.12.9 Bindings easy

`easyTemplating-wb/bindings/`:

| Binding | Uso |
|---|---|
| `BEasyTemplatingBinding` | Binding base para sub-widgets de template |
| `BEasyPopupBinding` | Popup on click (style estándar) |
| `BHxPxEasyPopupBinding` | Popup variant HxPx profile |
| `BEasyToolTipBinding` | Hover tooltip |
| `BHxPxEasyToolTipBinding` | Tooltip Hx variant |
| `BEasyHistoryPopupBinding` | Popup que muestra history chart |
| `BEtDataSource` + `BEtDataSourceFe` | Data source abstraction (tag-based) |
| `BPxViewBinding` | View selector (operator vs summary) |

El `BEtDataSource` es lo que da el "intelligent engine" — mapea tags del point a placeholders del template sin slot-path hard-coding.

### 36.12.10 Easy Template Linker

`BEasyTemplatingLinker` component: pega relationships entre templates deployed (ej. AHU linked to zones). Usa `baja:Topic`s custom para eventos cross-template (ej. "zone overridden → notify parent AHU template").

---

## 36.13 .pxvm — video / printout

Bloque 9.1.5 menciona licencia `axvelocity`. Detalle acá:

- **`.pxvm`** = PX Velocity Video Markup. Es un `.px` con widgets de video stream (RTSP/HTTP) integrados.
- Módulo: `axvelocity-rt.jar` — **NO está** en esta distribución OptimizerSupervisor (verificado: `ls modules/ | grep -i velocity` → nada).
- Para printout (PDF export): `printout-rt.jar` (sí está en distribución, Bloque 17). El servlet `PrintoutServlet` toma un `.px` + parámetros + renderiza PDF via headless bajaui-wb (Swing).
- Comando en PX `BExportButton` (kitPx) dispara este servlet.

Conclusión: en Honeywell OptimizerSupervisor actual **no hay capacidad video**; PDF sí.

---

## 36.14 Cache semantics browser

Dos niveles:

1. **PxCache** (bajaui-ux) — `.px` XML parseado → widget tree in-memory. Invalidación **manual** vía hard-reload.
2. **ModuleCache** (bajaScript + requireJS) — módulos JS cargados. Invalidación: cambiar query string `?v=N` en URL (Niagara hace esto incrementando `build` prop tras restart station).

### Cache busting

Tras restart de station, el `buildMillis` del module cambia → Niagara sirve JS con ETag nuevo → browser fetch fresh. Sin restart, el JS queda pegado.

### Local Storage

BajaScript usa **sessionStorage** para el ServerSession ID (re-conexión rápida tras page reload). Ver `WorkbenchConnection.js`. NO se usa localStorage persistente en N4.14 (decisión de seguridad — sid no persiste cross-session).

---

## 36.15 Performance PX — números observables

Complemento de 22.4 y 15.14:

| Métrica | Valor típico |
|---|---|
| Subscription lease renewal | 10 s (default) |
| BoxFrame implicit batch window | ~10 ms |
| Widget `initialize()` promise típica | 5-50 ms |
| `doLoad()` típico | <5 ms |
| `.px` parse + render 50 widgets | 100-300 ms |
| `.px` parse + render 500 widgets | 1-3 s |
| WebSocket idle timeout | 300 s |
| Server session keepalive | 300 s |
| BoundTable render 1000 rows | 800-1500 ms |
| PxEditor open .px en WB | 0.5-2 s |
| History chart 10 K points render | 400-1000 ms (webChart moderno) |

### Scaling real

- **Una PX page con >100 puntos bound** empieza a saturar el BOX channel en JACE (no supervisor). Regla empírica Honeywell: max 100 points live subs por browser tab en un Supervisor; 40 en un JACE.
- **Múltiples tabs del mismo user** → sesión compartida (cookies) → subscriptions se **suman** server-side. Abrir 5 tabs PX = 5× subs. Supervisor aguanta; JACE no.

---

## 36.16 Pattern: custom BajaScript module entry point

Registrar un módulo custom que produce JS:

`module.xml`:
```xml
<dependencies>
  <dependency name="bajaScript-ux" vendor="Tridium" vendorVersion="4.14.0"/>
  <dependency name="bajaux-ux" vendor="Tridium" vendorVersion="4.14.0"/>
</dependencies>
<types>
  <type class="com.vendor.myMod.ux.BMyModJsBuild" name="MyModJsBuild"/>
  <type class="com.vendor.myMod.ux.BUxMyWidget" name="UxMyWidget">
    <agent><on type="myMod:MyWidget"/></agent>
  </type>
</types>
```

Class `BMyModJsBuild extends BJsBuild` registra las rc/*.js a concatenar en `myMod.built.min.js`.

En PX:
```xml
<import>
  <module name="myMod"/>
</import>
<content>
  <MyWidget point="slot:/Foo/Point1"/>
</content>
```

Browser resuelve `MyWidget` → busca agent `UxMyWidget` → carga `myMod/rc/ux/MyWidget.js` via RequireJS → instancia → `initialize` → `load`.

---

## 36.16.5 Pipeline sync server-side deep — ProxyBroker + SyncBuffer

Extraído de `BComponentSpaceSessionHandler.class` strings:

```
BsonSyncBuffer   (inner class de handler)
BsonSyncDecoder  (inner class)
BsonSyncEncoder  (inner class)
javax/baja/sync/ProxyBroker
javax/baja/sync/SyncBuffer
javax/baja/sync/SyncOp
javax/baja/sync/AddOp
javax/baja/sync/FireTopicOp
ProxyBroker$IProxyBrokerPlugin
```

### Arquitectura del pipeline

```
   STATION SIDE:
   BComponentSpace local
        │
        │ (component changes)
        ▼
   ProxyBroker ──▶ IProxyBrokerPlugin (server handler)
        │
        ▼
   SyncOp encoded → SyncBuffer per-session
        │
        ▼
   BComponentSpaceSessionHandler.BsonSyncEncoder
        │
        │ (encodeToString via ValueDocEncoder$IEncoderPlugin)
        ▼
   BsonEncoderPlugin → JSON/BSON text
        │
        ▼
   BoxWebSocket.sendText(frame)   ─────▶ client

   CLIENT SIDE:
   WebSocket onmessage
        │
        ▼
   BrowserCommsManager
        │
        ▼
   BoxFrame parse → dispatch to boxcs channel
        │
        ▼
   BsonSyncDecoder (en browser) ← NOTA: mismo nombre clase, implementación diferente
        │
        ▼
   BoxComponentSpace.applySyncOps(ops[])
        │
        ▼
   Component events fire → Subscriber handlers → widget.doLoad()
```

### SyncOp types

Del decompilado de `baja.sync` (referenciados en handler + boxcs ops JS):

| Server-side SyncOp | Client JS equivalent | Semántica |
|---|---|---|
| `AddOp` | `AddOp.js` | Dynamic slot added |
| `RemoveOp` | `RemoveOp.js` | Slot removed |
| `SetOp` | `SetOp.js` | Slot value changed |
| `SetFlagsOp` | `SetFlagsOp.js` | Flags (ro, hidden...) |
| `SetFacetsOp` | `SetFacetsOp.js` | Facets (units, precision) |
| `RenameOp` | `RenameOp.js` | Slot renamed |
| `ReorderOp` | `ReorderOp.js` | Dynamic slots reordered |
| `FireTopicOp` | `FireTopicOp.js` | Topic fired |
| `AddKnobOp` | `AddKnobOp.js` | Subscription added |
| `RemoveKnobOp` | `RemoveKnobOp.js` | Subscription removed |
| `AddRelationKnobOp` | `AddRelationKnobOp.js` | Relation subscription |
| `RemoveRelationKnobOp` | `RemoveRelationKnobOp.js` | — |
| `LoadOp` | `LoadOp.js` | Initial load component |

### Por qué se llama "knob"

Un **knob** es un handle server-side que registra que *este cliente quiere ser notificado de cambios sobre este componente*. Es distinto de una "subscription" en el sentido general: múltiples knobs pueden coexistir por mismo client/component si diferentes widgets se subscriben independientemente. Refcounting: al último `RemoveKnob`, el componente se unmount si no está referenciado en el árbol por otro consumer.

### Batching server-side

El `SyncBuffer` agrega todos los SyncOps pendientes de una transacción atómica en la station y los emite en un único frame cuando la tx commitea. Ej: renombrar + cambiar flag + cambiar valor en un componente → 3 SyncOps → 1 frame.

Esto complementa el client-side batching (`Batch.js`). Una request atómica del cliente puede generar N SyncOps server-side y volver como 1 response + múltiples unsolicited frames.

### `syncFromMaster` / `syncToMaster`

Dos strings presentes en el handler: cubren el caso **NiagaraNetwork subordinate** donde un JACE es esclavo de Supervisor. El ComponentSpaceSessionHandler mismo serializa sync ops **bidireccionalmente** (Supervisor ↔ JACE), no sólo cliente↔server. Es el mismo pipeline BSON pero sobre tunnel Fox (ver `BFoxBoxChannel`). Bloque 10 (Fox protocol) tiene contexto.

---

## 36.16.6 `BUxBoundTable` internals — lectura del class

Extraído de `com.tridium.bajaui.ux.BUxBoundTable.class`:

La tabla acepta como property principal un **BOrd** (cursor source) y resuelve a `SlotCursor` via `BComponent.getSlots(...).getCursor()`. Propiedades clave (agent-registered):

- `cursor` — ORD que resuelve a cursor
- `columns` — lista de column specs (slot name + facets + render hint)
- `sortBy`, `sortOrder` — ordenamiento client-side
- `maxRows` — límite (default 1000)
- `selectedIndex` — row seleccionado
- `showHeaders` — bool

En runtime browser (`BTableBindingTypeExt`) crea DOM `<table>` con `<tbody>` rows. Cada row es una entry del cursor. Actualizaciones de cells llegan vía SyncOp `SetOp` sobre el Component parent → decoder detecta que es row en cursor → invalida cell y re-renderiza solo ese `<td>`.

**Por qué no virtualiza**: el diseño original (N4.0-4.8) asumía cursores pequeños (<200 rows típico en BACnet device list). Los casos 10K+ (BMS reports, IoT enterprise) aparecieron después y nunca se actualizó el widget. Honeywell en productos nuevos usa `webChart/grid/GridEditor.js` que sí tiene virtualization custom, o custom spandrel widgets.

---

## 36.17 Gotchas operacionales (G1–G14)

### G1. Subscriptions NO persisten tras hard refresh
Ctrl+F5 mata la `NiagaraHttpSession` (cookie intacta pero browser new context). Todas las subs se pierden; widgets tienen que re-lease. Esto es diseño (ver 22.11). **Mitigación**: implementar `reconnected` handler en widgets custom.

### G2. Calling `load()` antes de `initialize.resolve()` → Promise never resolves
`Widget.js` line 772: si `$destroyed` o initialize ya corrió, rechaza. Pero si `load` se llama antes de `initialize`, queda esperando `$initPromise` que sólo resuelve tras `doInitialize` completo. **Síntoma**: widget en blanco, Promise pending en DevTools.

### G3. `doInitialize` NO debe llamar a `load`
Deadlock. `doInitialize` corre dentro del then de `$commandGroup.loading()`. Llamar `this.load()` desde allí espera `$initPromise` que espera este mismo método. Nunca se resuelve.

### G4. Widget destruction NO es automática al cambiar de view
En una SPA Niagara, navegar de PX-A a PX-B **puede dejar PX-A widgets vivos** si el view container no los destruye explícitamente. `bajaux` espera que el padre (spandrel, PxInclude, etc.) maneje `detach()`. Memory leak progresivo en sesiones largas — síntoma: tab consume 2+ GB tras 8 hs.

### G5. ORD case-sensitive (heredado 22.11)
`slot:/Drivers/BacnetNetwork` ≠ `slot:/drivers/bacnetNetwork`. Station case-insensitive filesystem engine (Windows) pero ORD resolver case-sensitive. Puntos "no encontrados" tras migrar de desarrollo a prod con case-different paths.

### G6. FoxScheme siempre RPC (22.12)
BOX sobre WebSocket = single-direction push/pull. No hay bidirectional streaming. Subscriptions son "unsolicited events", no stream. Si necesitás streaming continuo (ej. trace logs), implementá polling con `lease` de 1 s.

### G7. Implicit batching window = ~10 ms
Ops JS consecutivos en <10 ms se empaquetan. Ops espaciados >10 ms → frames separados → latencia sumada. Si hacés `for (let p of 100_points) p.set({...})` espaciados 20 ms por async, son 100 frames (cada uno ~50 ms roundtrip JACE) = 5 s total. Con `Batch` explícito → 1 frame = 50 ms.

### G8. Memory leak browser si componentes no detach subs
`sub.attach(...)` sin `sub.detach()` matching → `baja.Subscriber` queda en memory con closure sobre widget, widget no GC-eable. Reveal: DevTools Memory → heap snapshot → buscar `Subscriber` instances crecientes. **Fix**: siempre `doDestroy() { this.$sub.detach(); }`.

### G9. EasyTemplates ≠ Niagara Templates
Confusión altísima en soporte. Usuario dice "template" → asumir primero EasyTemplates (Honeywell UI), fallback Niagara Templates (stock). Los `.ntpl` y `.etso` NO son intercambiables ni tienen converter oficial.

### G10. EasyTemplates license check en runtime
Si `Honeywell.license` no tiene feature `easyTemplating.enabled`, el servicio queda fault. El wizard en WB puede abrirse pero el save falla silencioso. Revisar `/tmp/b36/security/licenses/Honeywell.license` para feature flags (Bloque 13 tiene detalle).

### G11. WebSocket session timeout vs NiagaraHttpSession
WS `box.ws.idleTimeout=300000ms` (5 min) independiente de `NiagaraHttpSession` timeout (configurable en WebService, default 30 min). Si WS idle expira pero HttpSession viva, frontend ve "disconnected" pero al reconectar re-usa sid (rápido). Si HttpSession expira antes (user inactivo >30 min), reconexión falla → login redirect.

### G12. CSRF double-submit en PX-loaded fetch calls
Widgets custom que hacen `fetch('/foo')` manualmente DEBEN incluir cookie `niagara_csrf` como header `X-Niagara-Csrf`. BajaScript lo hace automático, pero código custom con fetch/jQuery.ajax NO. Síntoma: 403 Forbidden en POST. Bloque 30.6 cubre CSRF.

### G13. `BoundTable` silenciosamente trunca a 1000 rows
Property `maxRows` default = 1000. Tablas con 50K rows muestran 1000 y NO advierten. Diagnóstico: ver `<BoundTable>` en .px y revisar attrib maxRows.

### G14. `.pxvm` requiere `axvelocity-rt` + license
Si abrís un `.pxvm` en una distribución sin `axvelocity`, el PX parser falla en tipo `<VideoPane>` y renderiza blanco. Honeywell OptimizerSupervisor NO incluye axvelocity — `.pxvm` no funciona.

---

## 36.18 Mental model — roundtrip PX → BOX → DOM

Diagrama end-to-end:

```
┌────────────────────── BROWSER ────────────────────────┐
│                                                        │
│  1. HTML page loads                                    │
│     <script src="require.js">                          │
│     require(['bajaScript/sys','bajaux/spandrel'], fn)  │
│                                                        │
│  2. baja.start()                                       │
│     │                                                  │
│     ├─▶ HTTP POST /box (authenticate)                  │
│     │                                                  │
│     └─▶ WS upgrade /wsbox                              │
│                                                        │
│  3. fetch('/ord?ord=...&view=PxView') ─────────────┐   │
│                                                    │   │
│  4. response: <px version='1.0'>...                │   │
│                                                    │   │
│  5. PxDecoder.parse() → widget tree                │   │
│     │                                              │   │
│     ▼                                              │   │
│  6. for each widget:                               │   │
│      new UxWidget(params)                          │   │
│      widget.initialize(dom, params)                │   │
│      widget.$binding.subscribe(ord)                │   │
│          │                                         │   │
│          └──▶ baja.Component.lease({comps:[c]}) ───┼───┐
│                                                    │   │
│  7. Render DOM (spandrel queue)                    │   │
│      requestAnimationFrame → paint                 │   │
│                                                    │   │
│  8. Server pushes sync u-frames                    │   │
│     BsonSyncDecoder applies deltas                 │   │
│     widget.$sub.fireHandlers('changed', prop)      │   │
│     widget.doLoad(value) → DOM update              │   │
│                                                    │   │
│  9. Lease renewal every 10s ◄───────────────────────┼──┐
│                                                    │   │
│  10. User closes tab                               │   │
│      WS close                                      │   │
│      Server drops all knobs                        │   │
│                                                    │   │
└────────────────────────────────────────────────────┼───┘
                                                     │
                                                     │
┌─────────────────── STATION ─────────────────────────▼────┐
│                                                          │
│  Jetty 9 listener :443                                   │
│                                                          │
│  /box ── BBoxServlet ──▶ BServerSession                  │
│                           + BoxOp + channels             │
│                                                          │
│  /wsbox ── BoxWebSocketServlet                           │
│            ── BBoxWebSocketAcceptor                      │
│               ── BoxWebSocket (per-client)               │
│                                                          │
│  BoxWebSocket ── routes frames to BBoxChannel(s)         │
│                   ├─ BSysChannel                         │
│                   ├─ BComponentSpaceSessionHandler       │
│                   │   └─ subscribe/unsubscribe Components│
│                   │       (fires BsonSyncEncoder on change)│
│                   ├─ BOrdChannel                         │
│                   ├─ BHistoryChannel                     │
│                   ├─ BAlarmChannel                       │
│                   └─ BTransferChannel                    │
│                                                          │
│  Subscribed components' Topics fire → channel encodes →  │
│  BoxWebSocket sends text/binary frame to browser         │
│                                                          │
│  On logout/session expire: BoxWsHttpSessionListener      │
│    fires → closes all BoxWebSockets of that session      │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 36.19 Fuentes primarias leídas

- `/tmp/b36/bajaScript-ux/rc/` — 205 `.js` files source-readable
- `/tmp/b36/bajaScript-ux/rc/env/WebSocketConnection.js` — `/wsbox` endpoint literal
- `/tmp/b36/bajaScript-ux/rc/baja/comm/BoxFrame.js` — wire format doc
- `/tmp/b36/bajaScript-ux/rc/baja/comm/Batch.js` — implicit batching doc
- `/tmp/b36/bajaScript-ux/rc/baja/comp/Component.js` (1800+ lines) — `lease`, Subscriber, Component class
- `/tmp/b36/bajaScript-ux/rc/baja/comp/Subscriber.js` — event handler model
- `/tmp/b36/bajaScript-ux/rc/baja/boxcs/*.js` — 17 ops (LoadOp, AddKnobOp, SetOp, etc.)
- `/tmp/b36/bajaux-ux/rc/Widget.js` (1852 lines) — widget lifecycle contract
- `/tmp/b36/bajaux-ux/rc/spandrel.js` + `rc/spandrel/*.js` — reactive renderer
- `/tmp/b36/bajaui-ux/com/tridium/bajaui/ux/*.class` — 44 widget impls inventariados
- `/tmp/b36/kitPx-ux/rc/**/*.js` + `META-INF/module.xml` — catalog kitPx
- `/tmp/b36/kitPx-wb/com/tridium/kitpx/*.class` — 40+ widgets + bindings
- `/tmp/b36/hx-wb/com/tridium/hx/*.class` — Hx legacy catalog
- `/tmp/b36/webChart-ux/rc/**` — chart architecture
- `/tmp/b36/webEditors-ux/rc/**` — JS editors
- `/tmp/b36/box-rt/com/tridium/box/*.class` — 50+ server-side BOX classes
- `/tmp/b36/box-rt/com/tridium/box/BoxWebSocketServlet.class` — WS servlet + config props
- `/tmp/b36/box-rt/com/tridium/box/BServerSession.class` — session lifecycle
- `/tmp/b36/easyTemplating-rt/com/honeywell/easytemplating/**/*.class` — service, template, linker
- `/tmp/b36/easyTemplating-wb/com/honeywell/easytemplating/wizard/BEtWizardStep[1-6]*.class`
- `/tmp/b36/easyTemplating-rt/module.palette` — service registration
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/docs-text/Easy_Template_Installation_and_Operations_Guide_-_31-00405.txt` — paths oficiales user-home
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/guides/easyTemplating/*.html`
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/defaults/workbench/newfiles/PxFile.px` + `ReportPxFile.px` — seed .px files

---

## 36.20 Correcciones y ajustes a bloques previos

### Bloque 22 — correcciones/ajustes

- **22.11**: Agregar que `sub.attach(...)` SIN matching `sub.detach()` es causa #1 de browser memory leak en sesiones largas. El bloque 22 no da el patrón de cleanup explícito.
- **22.12** (BOX batching): el "debounce ~10 ms" está bien. Agregar: desde N4.10, `Batch` explícito es **opcional** para performance (antes era required). La semántica de atomicidad sigue siendo útil.
- **22.13** (Callback/Batch): mencionar que `Callback` API queda como **deprecated in favor of Promises** — múltiples doccomments en `Component.js` dicen "(Deprecated: use Promise)" sobre los `ok`/`fail` args.

### Bloque 29 — correcciones/ajustes

- **29.10** (WebSocket upgrade): el endpoint es **`/wsbox`** (no `/box` ni `/ws`). Verificado en `WebSocketConnection.js` literal. Si en 29.10 dice `/box` para upgrade está erróneo.
- **29.5** (Session lifecycle): agregar que `BoxWsHttpSessionListener` implementa `IHttpSessionDestroyListener` y cierra todas las `BoxWebSocket` asociadas cuando la `NiagaraHttpSession` muere. Esto cierra la gap entre HTTP session lifecycle y WS session lifecycle.
- **29.10** parámetros configurables: `box.ws.maxTextMessageSize`, `box.ws.maxBinaryMessageSize`, `box.ws.idleTimeout`, `box.serverSession.keepAliveSeconds`, `box.serverSession.maxThreads` — son system properties JVM. Cualquier tuning de WS en prod va acá.

### Bloque 14.6 — contraste EasyTemplates

- Bloque 14.6 describe templates Niagara (`.ntpl`, `template-rt`). Agregar nota: **EasyTemplates (honeywell) es un superset UI-friendly con un file format distinto (`.etso`/`.etco`) que NO es intercambiable**. La decisión arquitectónica de Honeywell fue construir un wizard encima, no extender el stock.

### Bloque 17 — filesystem locations

- Agregar a 17.x (workbench/station home): la ruta `<workbenchHome>/<Brand>/EasyTemplates/` (para distribución OptimizerSupervisor: `Niagara4.14/OptimizerSupervisor/EasyTemplates`) y `<stationHome>/shared/EasyTemplates/` como paths específicos de Honeywell.
- Los `.etso` y `.etco` son binarios XML-like; abren con unzip como `.ntpl`.

### Bloque 9.1.5 — axvelocity license

- Confirmado que `axvelocity-rt.jar` NO está en esta distribución OptimizerSupervisor. `.pxvm` no-op en este sistema. Corregir menciones que sugieran soporte activo.

### Bloque 35 — bajaui-wb vs bajaui-ux

- Diferencia clara: `bajaui-wb` es **Swing/AWT-in-JVM** (Workbench nativo), `bajaui-ux` es **DOM-in-browser**. Widgets tipo `BUx*` (ej. `BUxButton`) son adapters: el Java class vive en `-ux.jar` pero es sólo el **registro del TypeExt**; el rendering real lo hace `rc/ux/Button.js`.

---

## 36.21 Puntos de salida / gaps conocidos

- **Gap 1**: No hay evidencia en esta distribución de un sample `.px` complejo con BoundTable + custom binding. El bloque se basa en defaults + docs. Para calibrar perf empírica real habría que levantar la station y medir.
- **Gap 2**: `webChart` internals (webChart.built.min.js minified) — el source JS no-minificado está presente pero no se exploró a fondo. Sub-bloque posible: **36.X webChart series model deep**.
- **Gap 3**: pxEditor-wb (WB plugin para edit .px) no se exploró — pertenece más a Bloque 35 (WB).
- **Gap 4**: `docTemplates-doc` y `docWebCharts-doc` son JavaDoc browseable pero fuera de scope.
- **Gap 5**: Shading/shaded deps en `bajaux.built.min.js` — no verificado qué libs externas bundle (jQuery confirmado por `define(['jquery',...])`; posiblemente lodash, promise polyfill, tinyevents).
- **Gap 6**: `easyBinding-*.jar` no fue extraído — potencial bloque sobre binding extensions.
- **Gap 7**: El guide oficial menciona un "intelligent engine" de point-matching en EasyTemplates pero el algoritmo exacto (fuzzy match de tags? Levenshtein? regex?) no está decompilado en este pass.
