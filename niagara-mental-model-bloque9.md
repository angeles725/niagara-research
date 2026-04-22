# Niagara N4 — Mental Model · Bloque 9: UI Stack (Workbench + Px + BajaScript + hx + Servlets)

**Sesión**: 2026-04-22
**Método**: Investigación empírica READ-ONLY (3 sub-agents Explore en paralelo)
**Fuentes**: devguide (workbench, bajaui, gx, px, hx, hx-*, requirejs, web, servlets, velocity, uiFromAxToN4), source javax.baja.{web,sys,gx,ui}, decompilado workbench-wb, bajaui-*, gx-*, px-*, hx-*, web-rt.

---

## 9.1 Workbench + gx + Px

### 9.1.1 Workbench architecture

**`BWbShell`** = ventana completa del workbench. Componentes de layout:
- **MenuBar** / **ToolbarBar**: menús y botones (standard + per-view).
- **LocatorBar** (BOrdInput): text field con ORD activo, navegación como URL.
- **SideBar**: paneles pluggables (navigator, paleta).
- **View**: herramienta principal que edita/visualiza el objeto activo.
- **Console**: ejecución CLI (ej. Java compiler).
- **StatusBar**: mensajes.

**Modelo navegación tipo browser**: ORD actual = "active ord" → resuelve a BObject → plugin `BWbView` adecuado se carga.

**BWbView** extendible:
- Subclase `BWbView` (o `BWbComponentView` para BComponents).
- Override `doLoadValue(BObject, Context)` para cargar data.
- Llamar `setModified()` en ediciones.
- Override `doSaveValue(BObject, Context)` para persistir.
- Registrar como agent sobre tipos: `@Agent(types="...")`.

**Plugin types**: `WbViews`, `WbFieldEditor`, `WbSideBar`, `WbTools`, `WbService`.

**Perfiles** (`BWbProfile`): customización completa. Reemplazan components de layout, filtran plugins. Lanzar con `wb -profile:{typespec}`.

### 9.1.2 gx — graphics 2D

Sistema de coordenadas vectorial basado en doubles (x, y). Origen (0,0) esquina superior izquierda. Logical coordinate space → mapea a device space.

**Primitivos**:
- `BColor` (RGBA): CSS3 keywords, rgb/rgba, hash #rrggbbaa.
- `BFont`: formato `[italic||bold||underline] {size}pt {name}`. Métricas: baseline, height, ascent, descent.
- `BBrush`: solid, inverse (XOR), gradientes (lineales/radiales), bitmap.
- `BPen`: width, cap (Butt/Round/Square), join (Miter/Round/Bevel), dash pattern.

**Geometrías** (IGeom/Geom/BSimple):
- `Point`, `Size`, `Insets` (CSS margin), `LineGeom`, `RectGeom`, `EllipseGeom`, `PolygonGeom`, `PathGeom` (SVG syntax M/L/H/V/Z/C/S/Q/T/A).

**Transforms**: translate, scale, skew, rotate (SVG-based).

**Graphics API**: `fill(IGeom)`, `stroke(IGeom)`, `drawString()`, `drawImage()`. Soporta compositing (alpha blending), clipping, push/pop state stack.

**BImage**: carga async GIFs/PNGs/JPEGs desde ORDs. Caché por tamaño y recency. `animate()` para GIFs a 10fps.

### 9.1.3 Px — declarative UI XML

Archivos `.px` tipo `file:PxFile`. Árbol de widgets bajaui con data bindings.

**Sintaxis**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<px version="1.0" media="workbench:WbPxMedia">
  <import>
    <module name="bajaui"/>
    <module name="gx"/>
    <module name="converters"/>
  </import>
  <content>
    <!-- widget tree root -->
  </content>
</px>
```

- `<import>`: módulos Niagara para resolver type names.
- `<content>`: root widget.
- Props frozen como atributos, props complejos/dinámicos como elementos hijo.
- Atributo `name` especifica slot dentro del padre.
- Dynamic simple properties usan atributo `value`.

**Bindings**: `BBinding` y subclases (`BValueBinding`) se añaden como dynamic child slots. Ejemplo:
```xml
<ValueBinding ord="station:|slot:/value"/>
```
ORDs relativas al `ord` activo, permiten reuse across componentes.

**BConverters**: conversión entre valor y property UI (ej. `ObjectToString`).

**PxMedia** (`BPxMedia`): `workbench:WbPxMedia` vs `hx:HxPxMedia`. PxEditor advierte si se usan widgets no soportados por el media target.

### 9.1.4 bajaui controls

Widget toolkit sobre `BWidget` (BComponents). Root típicamente `BFrame` o `BDialog`.

**Panes**: `BCanvasPane` (absolute positioning), `BBorderPane` (CSS box: margin/border/padding), `BEdgePane` (top/bottom/left/right/center), `BGridPane`, `BSplitPane`, `BTabbedPane`, `BScrollPane`.

**Layout**: bounds (x, y, width, height) relativo al padre. `BLayout` property frozen con formato `"x,y,width,height"` soportando píxeles, %, `pref`, `fill`. Refresh async via `relayout()`.

**Painting**: `paint(Graphics)` con origen 0,0 en esquina del widget. Clipping automático. `paintChildren()`. Z-order inverso a property order (primer elemento = fondo).

**Input events**:
- `BKeyEvent`: keyPressed/Released/Typed.
- `BMouseEvent`: mouseEntered/Exited/Pressed/Released/Moved/Dragged/DragStarted/Hover/Pulse/Wheel.
- `BFocusEvent`: focusGained/Lost.

**Data widgets**: BTable, BTree, BTreeTable, BTextEditor usan Model-Controller-Renderer-Selection pattern.

**Commands**: `Command`, `ToggleCommand` para operaciones centralizadas con enable/disable auto-propagation. Localization via keyBase + lexicon. Undo/redo via `CommandArtifact`.

### 9.1.5 Velocity templating en Px

Apache Velocity integrado desde N4.14 para scripting dinámico via VTL (Velocity Template Language).

**`VelocityServlet`** accesible via `/velocity/{name}`. **`VelocityDoc`** referencia archivo `.vm`, contenedor de `VelocityContextOrdElement`. Context elements via ORD exponen componentes al VTL (ej. `$boolPoint`).

**`VelocityPxView`** (axvelocity:VelocityPxView): genera Px Views dinámicamente desde `.pxvm` (Px con VTL embebido):
```velocity
#set($kids = $util.getChildren($ax.obj, "baja:Component"))
#foreach($k in $kids)
  <Label>
    <ValueBinding ord="$k.getSlotPathOrd()"/>
  </Label>
#end
```

**Media queries**: `$ax.pxView.isMobileMedia()`, `isHxMedia()`, `isReportMedia()`, `isWorkbenchMedia()`.

**`PxInclude`**: master-detail. `.pxvm` (script-heavy, no-editable en Workbench) incluye `.px` files editables:
```xml
<PxInclude ord="file:^px/Include.px" variables="var=value"/>
```
Variables referenciables con `$(varName)` en el Px incluido.

**License**: feature `axvelocity` requerida.

---

## 9.2 BajaScript + ux + hx + RequireJS + JxBrowser

### 9.2.1 BajaScript — JS SDK moderno

JS SDK v2 asincrónico para interactuar con Station vía **Fox-over-HTTP** (WebSockets). Cargado via RequireJS (`baja!` loader).

**API core**:
- **Subscription**: `baja.Subscriber` se registra en topics de properties/actions. `baja.subscribe(ord, property, callback)` para updates real-time.
- **Object resolution**: `baja.Ord.make(ordString).get()` → referencias remotas. ORDs: `station:|`, `slot:`, `nspace:`, `file:`, etc.
- **Action invocation**: `component.invoke(actionName, args)` async.
- **Batch operations**: `baja.comm.Batch` agrupa ops en transacción única (reduce latencia).

**Namespaces**:
- `baja.obj.*` (tipos simples).
- `baja.comp.*` (Component, Property, Action).
- `baja.nav.*` (NavNodes).
- `baja.ord.*` (ORD schemes).
- `baja.coll.*` (Table, QueryCursor).
- `baja.tag.*` (Tags, Relations).

**Diferencia vs v1 (AX)**: v1 = script tags síncronos + SOAP-over-HTTP. v2 = 100% async, WebSocket-first, AMD modules, sin XML parsing cliente.

### 9.2.2 ux profile — web views nativas

Perfil Jetty + bajaux entregando UI HTML5/JS puro desde Station.

**Dos direcciones**:
1. **Módulos `-ux`** (bajaui-ux, gx-ux): recursos web vía `/module/...` servlet. Widgets bajaux (JS classes), CSS, images. BWebServlets entregan Px graphics como JSON o HTML views.
2. **Jetty + profiles HTML5**: Niagara 4 migró de web server casero a Jetty. Profiles pueden ser `BServletView` (bajaui) o `BWebServlet` (HTTP estándar).

**Lightweight model**:
- UI rendering se desplaza del JACE al navegador → reduce CPU de la station.
- UxMedia (N4.10+) entrega Px pages como JSON con widget tree → bajaux renderiza en cliente sin AST station-side.
- No requiere Web Launcher, no Java applets (deprecated).

**Perfiles**:
- `HTML5 Hx Profile`: rendered server-side (legacy, Hx → HTML).
- `HTML5 Profile` (bajaux native): web-first, direct DOM. Audition Mode para pre-ver UxMedia.

### 9.2.3 hx — HTML5 framework legacy

**Propósito**: traer bajaui (Java UI) al navegador generando HTML/JS strings server-side. Thin-client model.

**Arquitectura 4 capas**:
1. **HxProfile** (BHxProfile): genera HTML document (html/head/body), inyecta chrome. Métodos: `writeDocument()`, `updateDocument()`, `processDocument()`, `saveDocument()`. Hooks para toolbar/pathbar/theme.
2. **HxView** (BHxView extends BServletView): genera HTML snippet. Ciclo: `write()` renderiza, `update()` refresh async, `process()` background events, `save()` POST→model.
3. **HxOp**: state bag per request. Form data, paths únicos via `op.scope(name)`. Métodos: `getFormValue()` (unscopes auto), `getHtmlWriter()`, `make()` (sub-op para child views).
4. **Dialog/Command**: modals via `Dialog.writeContent()`, commands registrados en profile.

**Scoping (composability)**:
- HxView nested obtiene sub-HxOp via `op.make("name", parentOp)`.
- `op.scope("field")` genera paths: `uid1.editor.uid5.field`.
- `op.getFormValue("field")` unscopes automático.
- **Crítico**: HxOps deben crearse en el MISMO ORDEN en write/save/update/process.

**Eventos Hx**:
- Background events (XmlHttp, sin reload) via `HxView.process()`.
- `Command extends Event`: user-triggered. `Event` as base async.
- Respuesta = JavaScript ejecutable, no content HTML.

**Dialogs**: `Dialog extends HxComponent`, `writeContent(HxOp)`, retorna form data a `Command.handle()`.

**Theming**: CSS-based. Default `module://hx/default.css`. Classes override. Profile stylesheet LAST para precedencia.

**Status**: Hx sigue soportado pero development parado. Futuro = bajaux 100%.

### 9.2.4 RequireJS

AMD (Asynchronous Module Definition). Niagara configura RequireJS automáticamente.

```javascript
define(['baja!', 'bajaux/Widget', 'css!nmodule/myModule/rc/styles.css'],
  function(baja, Widget, css) {
    return class MyWidget extends Widget {};
  });

require(['nmodule/myModule/rc/MyWidget'], function(MyWidget) {
  new MyWidget().render();
});
```

**Loaders especiales (prefijos)**:
- `baja!` — BajaScript singleton.
- `nmodule/MODULE/path/...` — recursos `-ux` module via `/module/` servlet.
- `css!...` — CSS plugin, inyecta `<link>`.
- `lex!...` — lexicon plugin (i18n).
- `log!...` — logging plugin (console + station logs).

**Build step**: RequireJS optimizer (`r.js`) bundlea en single file → cache, menos HTTP requests. `grunt-niagara` para TDD + build.

**Integración**:
- bajaux: RequireJS provisto automáticamente.
- Hx views: `op.requireJs()` en `write()`.
- Velocity templates: `$util.requirejs()`.

### 9.2.5 JxBrowser — Workbench embed

Embedded Chromium en Workbench (reemplaza Web Launcher/applets):
- Bajaux views side-by-side con bajaui (Java).
- Navigation back/forward integrada en toolbar.
- Commands desde bajaux Widget → Workbench toolbar/menu.
- Drag-drop desde nav tree → bajaux view.
- Java↔JavaScript interop layer.

No requiere external browser. Px pages display nativo en HTML5 Profile sin plugin.

---

## 9.3 Servlets + Jetty + Web services

### 9.3.1 Jetty embebido

Jetty 9+ (Servlet spec 2.4). Integrado en módulo `web-rt`. **`BWebService`** (`javax.baja.web`, desde N4.13) encapsula config HTTP/HTTPS.

**Puertos** (configurables via `BServerPort`):
- HTTP: default 80. Props: `httpPort`, `httpEnabled`.
- HTTPS: default 443. Props: `httpsPort`, `httpsEnabled`, `httpsOnly` (fuerza HTTPS).

**TLS/SSL**:
- `mainCertAliasAndPassword` (reemplaza deprecated `httpsCert`).
- `httpsMinProtocol` (BSslTlsEnum): TLS 1.2, 1.3.
- `cipherSuiteGroup` (BTlsCipherSuiteGroup).
- `getServerCertificateHealth()` para dashboard de seguridad.

**Connector**: Jetty maneja conectores. ORD `/ord?...` mapea a URI namespace. Contexto per-módulo via `jetty-web.xml` (customiza `contextPath`, default = nombre del módulo).

### 9.3.2 BWebService + BWebServlet

**BWebService** (singleton, extends BAbstractService, implementa BISecurityInfoSource):
- `getMainService()` estática.
- Props: httpPort, httpsPort, mainCertAliasAndPassword, logFileDirectory, cacheConfig, gzipEnabled.
- `serviceStarted()` valida certs y despliega módulos web registrados.

**BWebServlet** (persistible, agregado a Station via palette):
- Property `servletName` (readonly): define namespace URI.
- Extiende HttpServlet implícitamente. Override con parámetro `WebOp` (en vez de HttpServletRequest/Response).
- Lifecycle: `started()` registra en URI namespace, `stopped()` desregistra.
- Acceso: `https://localhost/servletName`.

**Módulos Web estándar (N4.0+)**:
- Estructura: `src/WEB-INF/web.xml` + classes.
- Gradle: `jar { from('src') { include 'WEB-INF/*.xml' } }`.
- Servlet mapping: `<servlet-name>` + `<url-pattern>/test/*</url-pattern>`.
- URL: `http://localhost/moduleName/test/whatever`.
- `jetty-web.xml` (opcional): `<Set name="contextPath">/custom</Set>`.
- Filters (`javax.servlet.Filter`) soportados. **No** soportados: FilterChain, RequestDispatcher, algunos métodos de ServletContext.

**Threading**:
- Jetty worker threads (pool async). `doGet(WebOp)` corre en Jetty thread, **NO** en engine thread.
- Engine thread = BComponent. Web tier independiente.
- Acceso a componentes desde servlet → usar FOX/BOX calls (remote).

### 9.3.3 Servlet Views

**BServletView** (extends BSingleton): única instancia global per tipo.
- Override `doGet(WebOp op)` para renderizar HTML server-side.
- Registrar como `@Agent(types="...")` sobre BComponent type.
- URI: `/ord?slot:/MyComponent|view:module:BMyServletView`.
- `WebOp` contiene target object (OrdTarget), contexto web, HttpServletRequest/Response wrapeados.

**ServletView vs client-side**:
- **ServletView**: server-side HTML (stack viejo).
- **Hx**: HTML5 + JS + CSS standards + WebSocket/AJAX dinámico.
- **BajaUI (nuevo web)**: lightweight reusable components, canvas rendering.

Web Launcher deprecated. Migrar todo a BajaUI + Hx.

### 9.3.4 Exporter servlet

**`BExporter`** exporta a stream (CSV, PDF). Invocable via ORD:
```
https://{host}/ord/{ordToData}|view:{typeSpecOfExporter}?fileName={name};{prop}={value}
```

**Ejemplos**:
- CSV: `.../ord/station:%7Cslot:/%7Cbql:select%20toPathString%20from%20baja:Component%7Cview:file:ITableToCsv?fileName=myCsvFile`
- PDF: `...view:pdf:ITableToPdf?fileName=myPdfFile;pageSize=20%2C30`

Properties del exporter encoded como query params. Values decoded a BSimple. Soporta: CSV (ITableToCsv), PDF (ITableToPdf), custom `BIWbViewExporter`.

### 9.3.5 Web authentication

**`BAuthenticationService`** centraliza authn (fox + web). Múltiples schemes simultáneamente. Cada User tiene `authenticationSchemeName` property.

Schemes por defecto: `DigestAuthenticationScheme`, `AXDigestAuthenticationScheme` (N4↔AX compat). Extensibles: SAML, OAuth, Kerberos, Google Auth.

**Web-specific handlers**:
- `BWebCallbackHandler`: procesa HttpServletRequest/Response para credenciales. State machine hasta `READY`.
- `BILoginHTMLForm`: genera HTML login form. Cada scheme customiza UI.
- `BHttpHeaderCallbackHandler`: authn via HTTP headers (HELLO message protocol).

**Session management** (props BWebService):
- `allowUsernameAutocomplete`.
- `rememberUserIdCookie`: "remember me".
- `requireHttpsForPasswords`: fuerza HTTPS si hay password en request.
- `sameSite` (Strict/Lax/None): SameSite cookie attribute.
- Cookie seguridad: httpOnly + secure flags.

**User resolution en servlet**: `HttpServletRequest.getUserPrincipal()` → BUser. `getLocale()` → BajaLexicon via BasicContext.

### 9.3.6 NiagaraRPC

**`@NiagaraRpc`** (desde N4.1) decora métodos remotamente invocables (static o instance).

**Transportes**: FOX, BOX (BajaScript), Web servlet. JSON encoding/decoding automático.

**Parámetros/return tipos permitidos**: Map, List, Number (double), Boolean, String, Context (requerido último arg).

**Props anotación**:
- `permissions`: `"RWI"` (read/write/invoke) o `"unrestricted"` (métodos estáticos sin protected targets).
- `transports`: `@Transport(type=web/box/fox)`.
- `isSecure`: solo invocable sobre transport cifrado.
- `protectedTargets`: ORDs que requieren permisos adicionales.

**Context facets**:
- `isSecure`: transport encriptado.
- `remoteAddr`.
- `transportType` (N4.6+): "web", "box", "fox".

**Web invocation**: via `NiagaraRpcServlet`, POST con JSON body. User autenticado antes. `BUser.getCurrentAuthenticatedUser()` accesible en RPC method.

### 9.3.7 REST API moderno

**Niagara Analytics Web API** (N4.10+):
- HTTP + JSON messaging.
- Endpoints: Query, GetValue, GetNode, Subscribe, PollSubscription, Invoke, GetRollup.
- Componente `BWebApi` en `AnalyticsService`.
- Tipos mapeados a JSON: BDouble→number, BBoolean→boolean, BString→string.
- Auth token-based.

**Hx framework** (web UI moderno recomendado):
- HTML5 + JS + CSS.
- Real-time binding via WebSocket/polling.
- BajaScript access nativo al ORD engine.
- Sin Java Plugin.

**Vendor extensions**: Honeywell OptimizerSupervisor expone proprietary REST. `AnalyticsWebAPI_Protocol` especifica JSON time-series.

---

## Síntesis del bloque

### Capas del stack UI

| Capa | Tecnología | Dónde corre | Uso |
|------|-----------|-------------|-----|
| Workbench | Swing (bajaui, gx) | JVM cliente (desktop) | Admin/config desktop |
| Px declarativo | XML + bindings | Workbench + web (via PxMedia) | Dashboards y gráficos reutilizables |
| bajaux | JS classes + HTML5 | Browser | Web UI moderna |
| BajaScript | JS SDK async | Browser (via WebSocket) | Cliente del FOX-over-HTTP |
| hx legacy | HTML gen server-side | Station Jetty | Thin-client viejo |
| RequireJS | AMD loader | Browser | Module loading |
| Jetty | embedded servlet container | Station | HTTP/HTTPS |
| BWebServlet | servlet spec | Jetty thread | Endpoints custom |
| NiagaraRPC | annotation-based | FOX/BOX/Web | RPC multi-transport |
| Velocity | VTL templates | Station (render) | Dashboards dinámicos |

### Flujos

**Moderno (UxMedia)**: Px page (model) → JSON widget tree → Station → Browser → RequireJS carga bajaux → BajaScript subscriptions → real-time updates.

**Legacy (HxPx)**: Px page → HxProfile genera HTML/JS → Station HTTP response → Browser renders Hx HTML.

**Servlet custom**: BWebServlet → Jetty thread → `doGet(WebOp)` → render HTML / JSON / CSV → response.

### Conexiones con bloques anteriores

- **Bloque 3 (Security)**: Jetty + TLS configurado via BWebService. Certs del keystore system (Bloque 3.7).
- **Bloque 4 (Baja Object Model)**: widgets son BComponents. Bindings leen Properties via ORD. Slots de bajaui controlan UI state.
- **Bloque 5 (ORD)**: navegación workbench es ORD resolution. BValueBinding usa ORDs relativos.
- **Bloque 6.1 (Engine thread)**: Jetty worker threads son distintos del engine thread. Acceso a componentes desde servlets requiere cuidado (FOX/BOX calls).
- **Bloque 11 (Auth, próximo)**: BAuthenticationService, BUser y roles se consumen acá.

### Gotchas críticos

1. **Engine thread vs Jetty thread**: nunca llamar directo a `.get()` de BComponent en servlet; riesgo de deadlock. Usar FOX/BOX calls o `post()` al componente.
2. **HxOps mismo orden en write/save/update/process** — crítico para scoping correcto.
3. **`@Agent` sobre type registra view** — si olvidás el annotation, el view no aparece en navegador.
4. **Web Launcher deprecated** — migrar a bajaux/Hx. Java applets no soportados.
5. **RequireJS optimizer no es automático** — en development sin optimize, N HTTP requests lentos.
6. **NiagaraRPC protectedTargets** requiere ORDs exactos; un typo = acceso abierto o bloqueo inesperado.
7. **SameSite=None requiere Secure** (HTTPS) en browsers modernos. Si está None sin HTTPS, cookie se rechaza.
8. **Velocity axvelocity license** requerida — sin ella, `.pxvm` no renderiza.
9. **Px media mismatch** (workbench vs hx) — widgets del otro media no renderizan. PxEditor advierte pero a veces no.
10. **Hx path scoping**: `op.scope()` obligatorio en views nested, sino collision de form fields.

### Qué habilita

Con Bloques 1-9 podés:
- Construir dashboard Px con bindings a puntos BACnet/history.
- Implementar un servlet custom + UI bajaux que consume BajaScript subscriptions.
- Debuggear por qué un Hx view muestra datos stale (process vs update).
- Exponer endpoint NiagaraRPC seguro multi-transport.
- Entender por qué un Px graphic funciona en Workbench pero falla en browser (media mismatch).

**Próximo**: Bloque 10 — Platform & Station lifecycle.

---

## Engram topic keys

- `niagara/ui/workbench-px-gx` — BWbShell, plugins, gx primitivos, Px XML, bajaui widgets, Velocity.
- `niagara/ui/bajascript-ux-hx` — BajaScript v2, ux profile, hx 4 capas, RequireJS AMD, JxBrowser.
- `niagara/ui/servlets-jetty-webservices` — Jetty embed, BWebService, BWebServlet, exporters, NiagaraRPC, REST, auth web.

---

**Sesión cerrada**: 2026-04-22 — Bloque 9 consolidado.
