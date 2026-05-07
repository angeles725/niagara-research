# Bloque 51 — Reflow-Clean-177 audit cross-stack PARTE 2 (Par A real: frontend ↔ -rt + app-readable.js)

**Fecha**: 2026-05-04
**Método**: Investigación empírica READ-ONLY. Lectura directa de fuentes Java + JS bundle deobfuscado. Sin ejecución de builds ni tests.
**Scope**: `nmodsreflow-rt/` (Java backend completo) + `nmodsreflow-rt/src/rc/js/app-readable.js` (bundle Vite deobfuscado, 5.8 MB / 123,237 líneas). Lectura cruzada del frontend Vue para contrastar con implementaciones reales.
**Versión del bundle**: `1.7.5 - Wednesday, July 3rd, 2024, 5:19:32 PM` (CONFIRMADO, `app-readable.js:1`).

---

## 51.0 Contexto, scope, qué cambió respecto al Bloque 50

El Bloque 50 auditó el par `reflow-frontend ↔ nmodsreflow-ux` y descubrió:
1. El -ux NO tiene servlets HTTP reales — solo inyecta un iframe.
2. Los servlets reales viven en el -rt.
3. `window.injectBaja` / `window.injectConfig` / `window.destroyApp` estaban marcados como "Phase 5+" en el source Vue — **no implementados en la reconstrucción del frontend**.
4. `TODO-4` anotó que el bundle compilado (`app-readable.js`) podría contener la implementación real.
5. El mismatch `socket.io-client` vs Jetty WebSocket puro quedó como GAP-1 crítico sin resolver.

**Qué cambió en este bloque**:

- `window.injectBaja` CONFIRMADO implementado en `app-readable.js:121342` — la "Phase 5+" es el source reconstruido, NO el bundle producción.
- El WebSocket propio de Reflow usa `new WebSocket(...)` nativo del browser — **NO socket.io** (REFUTADO GAP-1 del Bloque 50). El frontend Vue usa `socket.io-client` como dependencia del package, pero el bundle compilado NO la usa para el WebSocket de Reflow.
- El subscriber lifecycle real está completamente implementado en `app-readable.js:3521-3729`.
- Los 8 BReflow*Commands Java (BOX handlers) tienen firma completa verificada.
- El módulo -rt requiere el permiso `REFLECTION` explícitamente (`module-permissions.xml:6`).

---

## 51.1 -rt Java backend

### 51.1.1 Matriz de servlets

El -rt registra **2 servlets** vía `WEB-INF/web.xml`:

| Clase | Mapping | Tipo | Auth |
|-------|---------|------|------|
| `SocketServlet` | `/nmodsreflow/ws` | `WebSocketServlet` (Jetty) | Via sesión Niagara (Jetty filter) |
| `BaseServlet` | `/nmodsreflow/*` | `HttpServlet` extendido | Via sesión Niagara (Jetty filter) |

**NO extienden `BWebServlet`** (CONFIRMADO). Ambas son servlets Jetty estándar, NO el framework Niagara BWebServlet. El contexto Niagara se obtiene por `req.getHttpServletRequest().getAttribute("niagara.context")` directamente.

**Context Path**: `/nmodsreflow` (CONFIRMADO, `jetty-web.xml:5`).

**`SocketServlet`** (`SocketServlet.java:12`):
- Extiende `org.eclipse.jetty.websocket.servlet.WebSocketServlet`
- `configure()` establece: `idleTimeout=60000ms`, `maxTextMessageBufferSize=65536` (64 KB), `maxTextMessageSize=262144` (256 KB), `maxBinaryMessageSize=131072` (128 KB)
- `ReflowWebSocketCreator` crea instancias de `BReflowWebSocketAcceptor.ReflowWebSocket` con IDs secuenciales (`AtomicLong`)

**`BaseServlet`** (referenciado en Bloque 50 — no re-auditado):
- Extiende `HttpServlet`
- Enruta GET/POST a handlers `*Response` classes

**No hay `@AgentOn` en los servlets**: Los servlets Reflow son registrados via `web.xml`, NO via el sistema `@AgentOn` de Niagara. El sistema `@AgentOn` se usa EXCLUSIVAMENTE para los `BReflow*Commands` (BOX handlers).

**G51-1 GOTCHA SERVLET DESIGN**: El -rt no extiende `BWebServlet` del framework Niagara (Bloque 9 — `web-rt`). Esto significa que el módulo **no usa** el mecanismo de registro automático de servlets de Niagara via BOG/module-include. Los servlets se registran via `web.xml` clásico. Consecuencia: si el Niagara deployment cambia el mecanismo de registro de servlets en una versión futura, el módulo queda desacoplado.

### 51.1.2 BAgent + actions expuestas

Los `BReflow*Commands` son los BOX handlers reales. Todos siguen el mismo patrón:

```java
@NiagaraType(agent = {@AgentOn(types = {"nmodsreflow:ReflowService"}, requiredPermissions = "r")})
public class BReflowXCommands extends BComponent implements BIServerSideCallHandler
```

**Matriz completa de clases y métodos** (CONFIRMADO, fuentes Java + `module-include.xml`):

| Clase Java | TypeSpec BOX | Métodos | Return type |
|-----------|-------------|---------|-------------|
| `BReflowAlarmCommands` | `nmodsreflow:ReflowAlarmCommands` | `getClasses`, `getAlarmByUuid`, `query`, `querySources`, `getUuidsForSources`, `getActiveAlarmCounts`, `getUnackedAlarmCounts`, `getAlarmsSinceTime`, `canAcknowledgeAlarms` | BString (todos) excepto `canAcknowledgeAlarms` → **BBoolean** |
| `BReflowBQLCommands` | `nmodsreflow:ReflowBQLCommands` | `query` | BString (JSON) |
| `BReflowCSVCommands` | `nmodsreflow:ReflowCSVCommands` | `loadPointMap` | BString (JSON) |
| `BReflowFileCommands` | `nmodsreflow:ReflowFileCommands` | `listFiles` | BString (JSON) |
| `BReflowHistoryCommands` | `nmodsreflow:ReflowHistoryCommands` | `getData`, `getDevices`, `getDeviceTree`, `getList`, `getQuickList`, `getGroupNames`, `getGroupTree` | BString (JSON) |
| `BReflowNavCommands` | `nmodsreflow:ReflowNavCommands` | `getNavChildren`, `bformat` | BString |
| `BReflowUserCommands` | `nmodsreflow:ReflowUserCommands` | `getRoles`, `getAllRoles` | BString |
| `BReflowLicenseCommands` | `nmodsreflow:ReflowLicenseCommands` | `licenseData`, `refreshLicense` | BString (JSON) |

**Firma de método BOX** (CONFIRMADO, `BReflowAlarmCommands.java:44`):
```java
public BValue getClasses(BComponent comp, BValue arg, Context cx) throws Exception
```
El parámetro `comp` es el `BReflowService` sobre el que se ejecuta como agent. `arg` es el valor pasado desde el frontend (serializado via `valueFromObject()`).

**`BReflowService`** también registra:
- `BReflowSyncService` — extiende `BAbstractService` (`sync/BReflowSyncService.java:35`)
- `BReflowChannelService` — gestiona canales del WebSocket custom
- `BReflowWebSocketAcceptor` — acceptor singleton para el WebSocket

**Actions de `BReflowService`** expuestas vía BajaScript `$component.invoke()`:
- `clearCache` — limpia cache de config
- `clearHistoryCache` — limpia cache de history groups (CONFIRMADO, `app-readable.js:74368`)

Estas actions son slots del `BReflowService` llamadas via el protocolo BOX (método `invoke` del BComponent), NO via `serverSideCall`.

**G51-2 GOTCHA BOX vs BajaScript invoke**: `serverSideCall({typeSpec, methodName, value})` y `$component.invoke({slot})` son DOS mecanismos distintos del protocolo BOX. El primero llama un método Java de un `BIServerSideCallHandler`. El segundo invoca una Action del Baja Object Model definida via `@NiagaraAction`. El bundle distingue correctamente ambos — `yi.call()` para serverSideCall y `$component.invoke()` directo para actions.

### 51.1.3 Static resources servidos

El bundle Vite está dentro del JAR del -rt, empaquetado en el path `rc/` (CONFIRMADO, `nmodsreflow-rt.gradle.kts:64-80`):

```kotlin
tasks.named<Jar>("jar") {
  from("src/rc") { include("**/*"); into("rc") }
  from("src/image-library") { include("**/*"); into("image-library") }
  from("src/sound-library") { include("**/*"); into("sound-library") }
  from("src/icons") { include("**/*"); into("icons") }
  from("src/doc") { include("**/*"); into("doc") }
  from("src/license") { include("**/*"); into("license") }
  from("src/WEB-INF") { include("**/*"); into("WEB-INF") }
}
```

**Inventario de recursos estáticos en `src/rc/`**:

| Path | Contenido |
|------|-----------|
| `rc/index.html` | Entry point SPA (producción) |
| `rc/config.html` | Entry point modo config (CONFIRMADO — es el HTML para `BReflowConfig`, NO legacy) |
| `rc/js/app.4509efb4.js` | Bundle Vite minificado (2.6 MB) |
| `rc/js/chunk-vendors.3fecdb47.js` | Vendors Vite minificado (2.8 MB) |
| `rc/js/app-readable.js` | Bundle Vite deobfuscado (5.8 MB) — SOLO para desarrollo |
| `rc/css/app.026f81ff.css` | CSS bundle |
| `rc/css/app-readable.css` | CSS deobfuscado |
| `rc/css/chunk-vendors.47c85512.css` | CSS vendors |
| `rc/fonts/*.woff/woff2` | FontAwesome Pro + Ionicons + Element |
| `rc/img/*.svg/png` | Imágenes UI |
| `rc/fa-light-300.woff2`, `fa-regular-400.woff2`, `fa-solid-900.woff2` | FontAwesome Pro (3 variantes) |
| `rc/point-matrix.json` | Matriz de puntos para dashboard |
| `rc/icon-categories.json` | Categorías de iconos |
| `rc/icon-search.json` | Índice de búsqueda de iconos |
| `rc/favicon.ico` | Favicon |
| `rc/background-default.jpg` | Imagen de fondo default |
| `rc/nmods-mark.png` | Logo NiagaraMods |

**Otros recursos en el JAR** (fuera de `rc/`):
- `image-library/` — 17 JPGs de equipos HVAC (AHUs, Boilers, Chillers, FCUs, RTUs, VAVs, Misc)
- `sound-library/` — 11 MP3s de notificación
- `icons/points/` — 5 PNGs (boolean, enum, history, numeric, string)
- `icons/reflow.png` — Icono del módulo
- `license/public.key` — Clave pública para verificación de licencia Reflow

**Serving**: `FileResponse` (referenciado en Bloque 50) sirve desde `module://nmodsreflow/rc{path}`. Fallback SPA-aware: paths desconocidos → `/index.html` (HTML5 History routing).

**GZIP cache**: Assets JS/JSON/CSS/HTML se comprimen en `^reflow/cache/resources/{md5hash}` cuando `webCache=true`.

**GAP resuelto**: El `config.html` del GAP-2 (Bloque 50) NO es legacy — es el entry point real para el modo config (`BReflowConfig`). El `window.injectConfig()` se llama en la SPA cargada desde `config.html`, mientras que `window.injectBaja()` se llama en la SPA de `index.html`. (CONFIRMADO, `app-readable.js:121423-121487`).

### 51.1.4 Permissions

**`module-permissions.xml`** (CONFIRMADO):
```xml
<permissions>
  <niagara-permission-groups type="all"/>
  <niagara-permission-groups type="workbench"/>
  <niagara-permission-groups type="station">
    <req-permission>
      <name>REFLECTION</name>
      <purposeKey>Java Reflection is used in JSON object serialization to store configuration data</purposeKey>
    </req-permission>
  </niagara-permission-groups>
</permissions>
```

El -rt requiere el grupo de permisos `REFLECTION` en el station profile. Esto es necesario porque Jackson (`com.fasterxml.jackson.databind.ObjectMapper`) usa reflection para serializar/deserializar JSON de la configuración.

**Consecuencias del requisito REFLECTION** (cross-referencia Bloque 18):
- En `moduleVerificationMode=low` (default N4.14): el módulo funciona sin firma.
- En `moduleVerificationMode=medium` o `high`: el módulo DEBE estar firmado para que el permiso REFLECTION sea concedido.
- El grupo REFLECTION es uno de los 3 grupos que SIEMPRE requieren firma Tridium para modos medium/high (Bloque 18.4 — aunque REFLECTION no está en el trío `ACCESS_CLASS+REFLECTION+MBEAN_PERMISSION`, el comportamiento en high requiere firma para cualquier permiso elevado).

**Permisos de agent** (CONFIRMADO, `module-include.xml`):
- Todos los `BReflow*Commands`: `requiredPermissions="r"` sobre `ReflowService`
- Sin excepción — ningún comando requiere `rw` o permisos elevados en el nivel BOX. El permiso de escritura se verifica a nivel de componente Niagara para casos como `canAcknowledgeAlarms`.

**`canAcknowledgeAlarms` verifica en el servidor** (CONFIRMADO, `BReflowAlarmCommands.java:109-112`):
```java
public BValue canAcknowledgeAlarms(BComponent comp, BValue arg, Context cx) throws Exception {
    BComponent alarmService = Sys.getService(BAlarmService.TYPE);
    return BBoolean.make(cx.getUser().getPermissionsFor(alarmService).hasOperatorWrite());
}
```
Verifica `operatorWrite` sobre el `BAlarmService` en el contexto del usuario autenticado. Retorna `BBoolean` (NO `BString`).

**Dependencias del -rt** (CONFIRMADO, `nmodsreflow-rt.gradle.kts:25-37`):
```kotlin
api(":baja")
api(":web-rt")
api(":alarm-rt")
api(":history-rt")
api(":control-rt")
api(":driver-rt")
api(":net-rt")
api(":platform-rt")
api(":schedule-rt")
api(":box-rt")
api(":bql-rt")
api(":bacnet-rt")
```

Y librerías embedded (uberjar — fat JAR):
- `jackson-core/databind/annotations 2.13.1`
- `opencsv 5.7.1`
- `commons-io 2.11.0`, `commons-lang3 3.12.0`, `commons-text 1.10.0`, `commons-collections4 4.4`, `commons-beanutils 1.9.4`, `commons-logging 1.2`
- `zjsonpatch 0.4.14` (Flipkart — RFC 6902 JSON Patch para config delta)

**G51-3 GOTCHA bacnet-rt DEPENDENCY**: El -rt depende de `bacnet-rt` (CONFIRMADO, `nmodsreflow-rt.gradle.kts:37`). El módulo Reflow no tiene lógica BACnet directa visible — la dependencia es probablemente transitiva de algún helper de `driver-rt` o para poder resolver ORDs de puntos BACnet via BQL. Si un deployment no tiene el módulo BACnet instalado, la station puede fallar al cargar nmodsreflow-rt.

---

## 51.2 app-readable.js deep dive

### 51.2.1 Estructura del bundle

**Tamaño**: 5.8 MB / 123,237 líneas (CONFIRMADO).
**Versión**: 1.7.5 build RC1 #43, Wed Jul 03 2024 (CONFIRMADO, `app-readable.js:121458-121462`).

**Bundler**: webpack (CONFIRMADO, `app-readable.js:1-74` — IIFE clásica webpack con `webpackJsonp`, runtime de modules, `r.p = "/nmodsreflow/"`). NO es Vite output directo — este archivo es el bundle webpack deobfuscado de una build anterior (Vite 5 genera output diferente). La build actual del proyecto usa Vite 5 pero este bundle fue generado con webpack. Existe una discrepancia: el `package.json` del frontend usa Vite 5, pero `app-readable.js` usa el runtime de webpack. INFERIDO: el bundle fue generado con una versión anterior del proyecto que usaba webpack, y luego se migró el toolchain a Vite. El bundle en `rc/` es el producción histórico, no el output del `reflow-frontend/` actual.

**Estructura módulos**: El IIFE registra ~3000 módulos internos webpack con IDs string cortos (`"0094"`, `"00a2"`, etc.). Los módulos son:
- Componentes Vue (templates compilados + setup)
- Stores Vuex (state, mutations, actions, getters)
- API layer real (implementación completa, NO stubs)
- Plugin `$niagara` con implementación de producción
- Bootstrap (`injectBaja`, `injectConfig`, `destroyApp`)
- WebSocket client custom (implementación nativa, NO socket.io)

### 51.2.2 window.injectBaja contract REAL

**CONFIRMADO** (`app-readable.js:121342-121421`).

**Firma real**: `async function injectBaja(fromWorkbench = false, widget = null)`

**Flujo completo**:

```
1. window.injectBaja(fromWorkbench, widget)
   │
   ├─ Vue.prototype.$workbench = fromWorkbench
   ├─ Vue.prototype.$hasWidget = (widget != null)
   ├─ Si fromWorkbench: window.require.config({ config: { baja: { disableConnectionReuse: true } } })
   │    ↑ IMPORTANTE: en Workbench se deshabilita la reutilización de conexión BOX
   │
   └─ window.require(["baja!", "baja!<tipos>", "nmodule/webEditors/rc/servlets/views"], async (baja, _types, views) => {
        │
        ├─ comp = await baja.Ord.make("service:nmodsreflow:ReflowService").get({ lease: true })
        │    ↑ Resuelve el BReflowService por ORD "service:" con lease
        │
        ├─ Vue.prototype.$baja = baja
        ├─ Vue.prototype.$bajaUsername = baja.getUserName()
        ├─ roles = await comp.serverSideCall({ typeSpec: "nmodsreflow:ReflowUserCommands", methodName: "getRoles" })
        ├─ Vue.prototype.$bajaUserRoles = roles
        ├─ Vue.prototype.$bajaViews = views
        ├─ Vue.prototype.$widget = widget || null
        ├─ Vue.prototype.$component = Vue.observable(comp)
        │    ↑ BReflowService como observable Vue — cambios en el BComponent son reactivos en Vue
        │
        ├─ isDemoMode = comp.get("demoMode")
        ├─ gaId = comp.get("ga")
        │
        ├─ window.niagara.env setup (hyperlink, toHyperlink, guid)
        │
        ├─ Si isDemoMode: console.log demo, NO dispatch("load")
        │
        └─ window.vueApp = new Vue({router, store, render: h => h(AppComponent)})
             window.vueApp.$mount()
             // Commits iniciales al store:
             store.commit("user/SET_USERNAME", baja.getUserName())
             store.commit("user/SET_ROLES", bajaUserRoles)
             store.commit("user/SET_IS_CONFIG", false)
             store.commit("demo/SET_IS_DEMO", isDemoMode)
             store.commit("SET_IS_MULTI_USER", comp.getMultiUserConfig())
             store.commit("SET_SOCKET_TIMEOUT", comp.getSocketTimeout() * 1000)
             // Luego, con setTimeout(1ms):
             store.dispatch("load", comp)
```

**Tipos pre-cargados** (`e1` en línea `121341`):
```
baja:DynamicTimeRange, bql:DynamicTimeRangeType, alarm:AlarmRecord,
control:Override, control:NumericOverride, control:EnumOverride,
control:BooleanOverride, control:StringOverride,
history:RootHistoryFolder, history:HistoryFolder, history:HistoryDevice,
history:LocalDbHistory, history:HistoryMirror, history:HistorySpace,
baja:UnitConversion,
niagaraVirtual:NiagaraVirtualComponent, niagaraVirtual:NiagaraVirtualControlPoint,
niagaraVirtual:NiagaraVirtualNumericWritable
```
Estos tipos se pre-cargan via RequireJS para que el Subscriber pueda manejar componentes de esos tipos sin lazy loading adicional.

**window.injectConfig** (`app-readable.js:121423-121487`): Mismo flujo pero:
- Si `widget == null`, retorna inmediatamente (no monta la app config sin widget)
- Siempre activa iView locale (`Vue.use(iView, { locale })`)
- Commit `SET_IS_CONFIG = true`
- NO tiene `setTimeout(1ms)` — llama `store.dispatch("load", comp)` directamente

**window.destroyApp** (`app-readable.js:121487-121488`):
```js
window.destroyApp = function() {
    window.vueApp.$destroy()
}
```
Llamada por `loader.destroy()` del -ux cuando el widget se destruye. Invoca `Vue.$destroy()` — dispara `beforeDestroy` en todos los componentes, que a su vez llaman `$niagara.subscriber.unsubscribe(this.uuid)`.

**window.onload** (`app-readable.js:121489-121490`):
```js
window.onload = function() {
    (null == window.niagara.env || window.isConfig) && window.injectBaja()
}
```
Si `window.niagara.env` es null (modo redirect — SPA abierta directamente fuera del iframe), o si `window.isConfig` es true, llama `injectBaja()` automáticamente. Esto resuelve el modo "redirect" de `BReflowRedirect` — la SPA bootstrapea sin que el widget del -ux la inicialice.

**GAP-2 del Bloque 50 RESUELTO**: `window.injectBaja` / `window.injectConfig` / `window.destroyApp` están completamente implementados. No son Phase 5+ — es el source reconstruido del frontend el que los tiene como stubs.

**G51-4 GOTCHA disableConnectionReuse en Workbench**: Cuando `fromWorkbench=true`, se configura `baja.disableConnectionReuse: true`. Esto es porque en Workbench puede haber múltiples instancias del BOX client compartiendo la misma conexión, y Reflow necesita su propio canal limpio. Sin este flag, los subscribes podrían mezclarse con otras partes del Workbench.

### 51.2.3 Subscriber lifecycle implementation

**IMPLEMENTACIÓN REAL** (`app-readable.js:3521-3729`).

El subscriber es un objeto singleton del módulo (variable `me` en el closure), con las siguientes propiedades internas:

```js
var oe = null;    // instancia baja.Subscriber (lazy)
var re = [];      // array de suscripciones activas: {id, owner, callback, component}
var se = [];      // queue de resolves pendientes (para batching)
var le = null;    // timer del batching (setTimeout 100ms)
var ce = [];      // owners en proceso de subscribe (para race condition)
var ue = [];      // handles de components en proceso
var de = [];      // queue de unsubscribes pendientes
var he = false;   // debug logging flag
```

**`me.subscriber`** (getter lazy):
```js
get subscriber() {
    if (null === oe) {
        oe = new this.$baja.Subscriber;
        oe.attach("changed", function(e) { me._changed(this, e) })
    }
    return oe
}
```
Instancia el `baja.Subscriber` la primera vez que se accede. El evento `changed` despacha callbacks a todos los registros que tengan el mismo `component.$handle`.

**`me.subscribe(owner, components, callback)`**:
1. Pushea `owner` a `ce` (en proceso)
2. Si `components` no es array, lo wrappea
3. Valida que cada component tenga `$handle` (o lanza error)
4. Llama `this.subscriber.subscribe(components)` → Promise
5. En `.then()`: si el owner+handle ya está en `re`, actualiza el callback; si no, agrega `{id, owner, callback, component}` a `re`
6. Limpia `ce` y `ue` del owner
7. Revisa `de` (queue de unsubscribes pendientes) — si hay un unsubscribe pendiente para este owner, lo ejecuta

**`me.unsubscribe(owner, components?, forceQueue?)`**:
1. Si el owner está en `ce` (subscribe en progreso) O `forceQueue`, pushea a `de` para ejecutar después
2. Filtra `re` para quitar los registros del owner (o solo los components especificados)
3. Con `setTimeout(250ms)`: para cada component removido, si YA NO hay ningún subscriptor activo en `re` ni en `ue`, llama `this.subscriber.unsubscribe([component])`

**G51-5 GOTCHA RACE CONDITION SUBSCRIBE/UNSUBSCRIBE**: El delay de 250ms en el unsubscribe es una protección contra destrucción de componentes Vue en el mismo tick que un subscribe aún en vuelo. Es correcto en teoría, pero si `beforeDestroy` + siguiente `mounted` (mismo componente re-montado) ocurren dentro de 250ms, el unsubscribe puede cancelar al subscriber re-creado. Esto es un bug potencial en navegación rápida.

**`me.resolve(ord)`**:
- Si `ord` es array: itera, cada uno hace `baja.Ord.make(ord[i]).get()` + `lease()`, retorna array (con errores embebidos como `{ord, error}`)
- Si es string: `baja.Ord.make(ord).get()` directo

**`me.resolveBetter(ord, callback)` + `me.resolveBatched()`**:
Batching de resolves: acumula ORDs en `se` con un debounce de 100ms, luego ejecuta un `baja.BatchResolve(ords).resolve({subscriber})` para resolver todos juntos. Más eficiente que resolve individual en loops.

**`me._changed(component, prop)`**:
Dispara todos los callbacks en `re` donde `component.$handle === subscription.component.$handle`. Recibe el prop que cambió.

**BOX channel**: El subscriber usa `baja.Subscriber` que opera sobre la conexión BOX (WebSocket `/wsbox` de Niagara). NO abre una conexión separada — reutiliza el channel BOX del contexto `$baja`.

### 51.2.4 BOX channel management

**CONFIRMADO**: El bundle REAL no usa socket.io para el WebSocket de Reflow. Usa `new WebSocket(url)` nativo del browser.

**Implementación** (`app-readable.js:4071-4315`):

El adapter WebSocket (`$e`) crea una conexión WebSocket pura:
```js
n = new WebSocket(
    "https:" === window.location.protocol ? "wss://" : "ws://"
    + window.location.host + "/nmodsreflow/ws"
)
```

**Gestión de canales** (objeto `Ae` — map de canales):
```js
Ae[channelName] = {
    joining: false,
    leaving: false,
    connected: false,
    who: [],
    subscribers: []
}
```

**Keepalive**: `setInterval` de 30,000ms enviando `{command: "ping"}` (CONFIRMADO, `Xe.keepAliveTTL = 3e4`).

**Timeout de request**: 10,000ms por comando (CONFIRMADO, `Xe.timeout = 1e4`).

**Canales que se joinean al conectar**: `["reflow"]` por defecto. Si es modo config (`isConfig`): `["reflow", "reflow-config"]` (CONFIRMADO, `app-readable.js:14314`).

**Ticket system para request/response**: Cada comando tiene un `ticket` (número incremental). El servidor responde con el mismo `ticket`. El cliente tiene un `timeout` por ticket (10s default) que rechaza la Promise si no llega respuesta.

**Reconexión**: NO hay reconexión automática visible en el bundle. Si el WebSocket se cierra (`close` event), el estado de canales se limpia pero NO hay un reconnect loop. La reconexión debe ser iniciada externamente (probablemente por el Vuex store que detecta el socket caído).

**G51-6 GOTCHA NO AUTO-RECONNECT**: El adapter WebSocket (`$e`) no tiene lógica de reconnect automático. El store Vuex tiene `socketAutoReconnect: true` y `socketTimeout: 10000` (Bloque 50), pero la implementación del reconnect no está visible claramente. Si el WebSocket se corta, los usuarios necesitan refrescar la página.

**Mensajes broadcast del servidor**: El servidor puede enviar mensajes no solicitados (sin `ticket`) del tipo:
- `{type: "client-info", clientId, configControl}` — al conectar
- `{type: "channel-status", ...}` — estado del canal
- `{type: "delta", timestamp, from, delta, author}` — config delta broadcast
- `{type: "control-request", request, timeout}` — solicitud de control de config
- `{type: "control-response", status}` — respuesta al control request
- `{type: "control-change", controller}` — cambio de controlador
- `{type: "config-refresh"}` — recargar config (cuando otro cliente actualizó)
- `{type: "config-reload"}` — recargar completo (multi-usuario)

### 51.2.5 Auth/CSRF handling

**Auth**: El bundle NO maneja autenticación propia. El WebSocket y todas las llamadas HTTP llevan automáticamente el `JSESSIONID` cookie del Jetty Niagara (mismo origen).

**CSRF**: NO encontrado ningún `x-niagara-csrfToken` header en `app-readable.js`. Las llamadas axios/HTTP del bundle NO añaden headers CSRF. Las BOX calls tampoco. Las llamadas son same-origin y están protegidas por el `CsrfProtectedFilter` de Niagara a nivel Jetty (Bloque 18).

**Roles**: Al iniciar, `injectBaja` obtiene los roles del usuario via BOX (`ReflowUserCommands.getRoles`). Los roles se commitean al store como `user/SET_ROLES`. La lógica de autorización de UI usa `profiles/authorizeLink` (router guard, `app-readable.js:121328-121337`) — si el link NO está autorizado por el perfil activo, redirige a `/` (o al `startPage` del perfil).

**G51-7 GOTCHA AUTH ROL NO NIAGARA**: La autorización de UI en Reflow está basada en "profiles" configurados en el JSON de config, NO en los roles Niagara directamente. Los roles Niagara (`getRoles()`) se obtienen pero se usan para binding display, no como fuente de verdad para autorización de rutas. La autorización de rutas es puramente client-side basada en config JSON. Alguien con acceso a la consola del browser puede bypassear este check.

### 51.2.6 Mapping "Phase 5+" stubs → implementación real

| Stub en `reflow-frontend/` | Implementación real en `app-readable.js` | Línea |
|---------------------------|----------------------------------------|-------|
| `$niagara.subscriber.resolve(ord)` | `me.resolve(ord)` — BajaScript `baja.Ord.make(ord).get()` | 3619 |
| `$niagara.subscriber.subscribe(uuid, components, cb)` | `me.subscribe(owner, components, callback)` completo | 3535 |
| `$niagara.subscriber.unsubscribe(uuid)` | `me.unsubscribe(owner)` con race condition protection | 3577 |
| `window.injectBaja` | Async function completa con baja.Subscriber, VueApp mount | 121342 |
| `window.injectConfig` | Async function completa para modo config | 121423 |
| `window.destroyApp` | `window.vueApp.$destroy()` | 121487 |
| `$niagara.alarm.ackAlarmsByUuid` | `baja.Ord.make("alarm:").get().then(s => s.ackAlarms({ids}))` | 14520 |
| `$niagara.alarm.addNotes` | `baja.Ord.make("alarm:").get().then(s => s.addNoteToAlarms({ids, notes}))` | 14546 |
| `$niagara.alarm.getNotes` | `baja.Ord.make("alarm:").get().then(s => s.getNotes({uuid}))` | 14573 |
| `$component.invoke({slot: 'clearHistoryCache'})` | Directo — BOX invoke action BajaScript | 74368 |
| `$component.invoke({slot: 'clearCache'})` | Directo — BOX invoke action BajaScript | 105665 |
| `box.serverSideCall(typeSpec, method, args)` | `yi.call(typeSpec, methodName, args)` completo | 5128 |
| WebSocket connect (`websocket.js` stub) | `$e.open()` + `new WebSocket("/nmodsreflow/ws")` | 4092 |
| `store.dispatch("load", comp)` | Implementación completa en store module | 14297 |

**Impacto**: El source de `reflow-frontend/` representa el estado **incompleto** del proyecto (mid-development, Phase 4). El bundle `app-readable.js` es el source **producción v1.7.5** (Jul 2024). Son dos estados temporales distintos del mismo proyecto.

---

## 51.3 Cross-stack contract REAL (corrige Bloque 50)

### 51.3.1 Tabla completa endpoints + handlers

La tabla del Bloque 50 permanece válida para los endpoints REST y BOX. Se agregan las correcciones y la implementación real de los canales:

**Canal 1 — REST HTTP** (CONFIRMADO — sin cambios respecto al Bloque 50):

| # | Path | Método | Handler -rt | JSON shape real |
|---|------|--------|------------|-----------------|
| 1 | `/nmodsreflow/config` | GET | `ConfigResponse` → `^reflow/config.json` | Objeto JSON completo (config del dashboard) |
| 2 | `/nmodsreflow/config_update` | POST | `ConfigUpdateResponse` | Body: estado Vuex serializado |
| 3 | `/nmodsreflow/config_delta` | POST | `ConfigDeltaResponse` → `zjsonpatch.JsonPatch.apply()` | `{ timestamp, patched: bool }` |
| 4 | `/nmodsreflow/station/alarms/query` | POST | `AlarmQueryResponse` → `AlarmData.query()` | `{ total: int, records: [{uuid, source, priority, normalTime, lastUpdate, timestamp, sourceStateDisplay, sourceState, ackStateDisplay, ackState, ackRequired, alarmTransitionDisplay, alarmTransition, sourceClass, sourceClassDisplay, user, alarmData, noteCount}] }` |
| 5-6 | `/nmodsreflow/station/alarms/csv` | GET | `AlarmCSVResponse` → `AlarmData.streamAlarmsCSV()` | CSV: 29 columnas encabezado |
| 7 | `/nmodsreflow/station/histories` | GET | `HistoryListResponse` | `[{metadata}]` |
| 8 | `/nmodsreflow/station/history-data` | GET | `HistoryChartDataResponse` → `HistoryData.fromComponent()` | `{ histId: { hId, recordType, title, historyName, fromCurrentStation, timezoneName, timezoneShortName, timezoneOffset, units?, ordinals?, data: [[millis, value, label?], ...] } }` — estilo `d3` |
| 9 | `/nmodsreflow/station/history-groups` | GET | `HistoryGroupsResponse` | árbol jerárquico |
| 10 | `/nmodsreflow/station/backups` | GET | `BackupListResponse` | lista de backups |
| 11-15 | `/station/backups/*` | GET | `BackupCreateResponse`, `ApplyResponse`, `DestroyResponse`, `RenameResponse`, `ResetResponse` | `{ success, filename? }` |
| 16 | `/station/equipment-notes` | GET | `EquipmentNoteResponse` | `[{id, text, author, date}]` |
| 17 | `/station/equipment-notes-update` | POST | `EquipmentNoteUpdateResponse` | vacío |
| 18 | `/station/images` | GET | `ImageListResponse` | `[{ord, name}]` |
| 19 | `/station/files` | GET | `FileTreeResponse` | árbol recursivo |
| 20 | `/station/image-library` | GET | `ImageLibraryResponse` | árbol de imgs |
| 21 | `/station/schedules` | GET | `SchedulesDataResponse` | `[BWeeklySchedule JSON]` |
| 22 | `/nmodsreflow/weather-map` | GET | `WeatherMapResponse` | Blob PNG |
| 23 | `/nmodsreflow/demos` | GET | `DemoResponse` | config demo |
| 24 | `/station/histories/{name}` | GET | `HistoryDataResponse` | datos hist |

**Canal 2 — WebSocket Reflow** (CORREGIDO):

| Comando cliente→servidor | Handler servidor | Respuesta |
|--------------------------|-----------------|-----------|
| `{command: "join", channel: "reflow"/"reflow-config"}` | `BReflowWebSocketAcceptor` | `{type: "join", success, who, channel}` |
| `{command: "who", channel}` | Ídem | `{type: "who", success, who}` |
| `{command: "leave", channel}` | Ídem | `{type: "leave", success}` |
| `{command: "ping"}` | keepalive | N/A (no respuesta) |
| `{command: "client-info"}` | Ídem | `{type: "client-info", clientId, configControl, username}` |
| `{command: "config-control", type: "who"/"request"/"accept"/"reject"}` | `RequestControlCommand` | `{success, controller?, requested?, timeout?}` |
| `{command: "sync-delta", delta: RFC6902[]}` | `ConfigSyncCommand` → `BReflowSyncService.applyConfig()` | `{sendFullState: bool, patched: bool, config-timestamp}` |
| `{command: "favorites-read"}` | `ReflowOrdTreeFavoritesRead` | `[favoritos]` |
| `{command: "favorites-write", ...}` | `ReflowOrdTreeFavoritesWrite` | confirmación |
| `{command: "route"/"config-route", ...}` | `BReflowWebSocketAcceptor` | broadcast |

**Canal 3 — BOX Niagara** (sin cambios, implementación real confirmada en `yi.call()`):

El objeto `yi` (`app-readable.js:5090`) es la capa de abstracción BOX en el cliente:
- `yi.call(typeSpec, methodName, args)` — llama `$component.serverSideCall({typeSpec, methodName, value})`
- `yi.string(...)` — igual que call pero convierte resultado a string
- `yi.json(...)` — igual que string pero parsea JSON

Todos los BOX calls del Bloque 50 (secciones 37-60 de la tabla) pasan por `yi`. La serialización de args usa `yi.wrappedValue()`:
- `string/number/boolean` → pasa directo
- `object` (no array) → crea `baja.Component` con slots
- `array` → join con coma → string

**Canal 4 — BajaScript directo** (implementación real confirmada):
- `alarm:.ackAlarms({ids})` — `app-readable.js:14529`
- `alarm:.addNoteToAlarms({ids, notes})` — `app-readable.js:14555`
- `alarm:.getNotes({uuid})` — `app-readable.js:14582`
- `$component.invoke({slot: 'clearCache'/'clearHistoryCache'})` — directo sobre `BReflowService`

**Canal 5 — External APIs**: Sin cambios.

### 51.3.2 Wire protocol corregido

| Canal | Protocolo real | Endpoint | Direccion | Notas |
|-------|---------------|----------|-----------|-------|
| REST | HTTP/HTTPS Jetty Niagara | `/nmodsreflow/*` | request/response | axios en el bundle |
| WebSocket Reflow | **WebSocket nativo puro** (NO socket.io) | `/nmodsreflow/ws` | bidireccional | JSON frames + ticket system |
| BOX Niagara | HTTP POST `/box` upgrade → WebSocket `/wsbox` | `/wsbox` | bidireccional | `baja.Subscriber` + `serverSideCall` |
| BajaScript directo | BOX (misma conexión `$baja`) | via `baja.Ord.make().get()` | bidireccional | Actions nativas Niagara |
| External | HTTPS | `*.niagaramodules.com` | request/response | version check + weather |

**Corrección crítica al Bloque 50**: El Bloque 50 identificó `socket.io-client 2.5.0` en `package.json` y asumió que el WebSocket del servidor (`SocketServlet`) era compatible con socket.io. **REFUTADO**:

1. El bundle `app-readable.js` usa `new WebSocket()` nativo del browser (NO socket.io).
2. El servidor `SocketServlet` es WebSocket puro Jetty. Son **compatibles** entre sí.
3. `socket.io-client` está en `package.json` pero **no se usa** en el bundle de producción para el WebSocket de Reflow. Probablemente fue una dependencia de una iteración anterior del protocolo que luego se reemplazó por WebSocket nativo.
4. El bundle webpack deobfuscado es de una versión anterior al package.json Vite 5 actual — la migración eliminó socket.io del bundle pero el package.json no se limpió.

**GAP-1 del Bloque 50 REFUTADO**: NO hay mismatch. El servidor y el cliente usan WebSocket puro.

### 51.3.3 Validación de los 7 hallazgos del Bloque 50

**G50-1 — Bundle desincronizado con rc/**:
**CONFIRMADO + AMPLIADO**: El bundle `app-readable.js` es versión 1.7.5 (Jul 2024) y el `package.json` del frontend es version 1.7.7 (o la reconstrucción). Son versiones distintas. Riesgo real confirmado.

**G50-2 — GOTCHA AUTH redirect**:
**CONFIRMADO + RESUELTO**: `window.onload` en `app-readable.js:121489` llama `injectBaja()` si `window.niagara.env == null`. En modo redirect (sin iframe), la SPA bootstrapea via `window.onload`. El `window.niagara.env` se configura por el Workbench/HX antes de que el iframe cargue — si está null, es redirect. La SPA funciona porque `injectBaja()` se llama sin `widget` (null), y el `$widget` queda null.

**G50-3 — GOTCHA WEBSOCKET socket.io vs Jetty puro**:
**REFUTADO COMPLETAMENTE**: El bundle de producción usa WebSocket nativo. socket.io-client en package.json es dependencia no usada (vestigio). Servidor y cliente son compatibles. NO hay mismatch.

**G50-4 — GOTCHA SUBSCRIBER unsubscribe no-op**:
**CONFIRMADO PARCIALMENTE**: En el bundle de producción, `unsubscribe` SÍ está implementado con delay de 250ms. Pero el riesgo de leak persiste si `beforeDestroy` se llama en un componente que todavía tiene un subscribe en vuelo (`ce.includes(owner)`) — en ese caso, el unsubscribe se encola en `de` para ejecutarse cuando el subscribe complete. Si el componente ya fue destruido antes de que el subscribe termine... el leak ocurre igualmente.

**G50-5 — GOTCHA WRITES via BOX**:
**CONFIRMADO**: No hay endpoint REST de write. Todos los writes son via BajaScript directo. En el bundle, el write de puntos se hace via `component.invoke(actionArgs)` donde `component` es el BComponent del punto (no `BReflowService`). El bundle tiene métodos `invokeAction` en múltiples componentes Vue que llaman `actionPoint.invoke({slot, value})` (CONFIRMADO, `app-readable.js:21131`).

**G50-6 — GOTCHA canAcknowledgeAlarms retorna BBoolean**:
**CONFIRMADO en servidor** (`BReflowAlarmCommands.java:111`): `return BBoolean.make(...)`. Sin embargo, el bundle NO tiene llamada directa a `canAcknowledgeAlarms` via BOX — la UI usa el campo `acknowledgmentEnabled` del config JSON, que es un booleano guardado en la configuración. La verificación de permiso BOX no está visible en el bundle de producción. INFERIDO: `canAcknowledgeAlarms` puede ser llamado durante el load de configuración (antes del store `LOAD_STATE`), y el resultado se serializa en el JSON de config o se usa en el servidor directamente. El riesgo de mismatch de tipo sigue siendo válido si se llama desde un futuro cliente.

**G50-7 — GOTCHA SCHEDULES sin write**:
**CONFIRMADO**: No encontrado ningún write de schedules en el bundle. Sigue siendo gap funcional real.

**Backup GETs destructivos — CSRF guard server-side**:
**Parcialmente verificado**: El `BaseServlet` depende del `CsrfProtectedFilter` de Niagara a nivel Jetty (no del servlet). El filtro Niagara aplica CSRF check para métodos POST pero típicamente exime GET requests (comportamiento estándar de CSRF protection). Los endpoints de backup destructivos son GETs — si el filtro Niagara no protege GETs (comportamiento esperado), estos endpoints son vulnerables a CSRF via `<img src="/nmodsreflow/station/backups/destroy?filename=X">`. **NO auditable sin ver el source del `CsrfProtectedFilter` de Niagara** (fuera del scope del módulo Reflow). Es un riesgo arquitectónico por el diseño GET-destructivo, independientemente de la protección CSRF de Niagara.

**History data JSON shape (TODO-3 del Bloque 50) RESUELTO**:

`HistoryData.jsonForHistory()` genera (CONFIRMADO, `HistoryData.java:109-158`):
```json
{
  "hId": "StationName/HistoryName",
  "recordType": "history:NumericTrendRecord",
  "title": "Display Name",
  "historyName": "Display Name",
  "fromCurrentStation": true,
  "timezoneName": "Central Standard Time",
  "timezoneShortName": "CST",
  "timezoneOffset": -21600000,
  "units": "°F",
  "ordinals": null,
  "data": [
    [1717123456789, "72.50", "72.50 °F"],   // d3 style: [millis, value, label?]
  ]
}
```
La respuesta completa del endpoint es: `{ "histId1": {...}, "histId2": {...}, ... }` — un objeto keyed por history ID.

Para style NO d3, cada registro es `{time: "MM/dd/yyyy HH:mm:ss", value: "72.50", label?: "°F", status?}`.

`contextualRanges=true` añade el PRIMER registro ANTES del rango (para que el chart muestre la línea desde el borde izquierdo) y el PRIMER registro DESPUÉS del rango.

**AlarmData query shape (TODO-2 del Bloque 50) RESUELTO**:

`AlarmData.getAlarmRecord()` genera (CONFIRMADO, `AlarmData.java:246-278`):
```json
{
  "uuid": "...",
  "source": "station:|slot:/Equipment/AHU1/Temperature",
  "priority": 50,
  "normalTime": "...",
  "lastUpdate": "...",
  "timestamp": "...",
  "sourceStateDisplay": "offnormal",
  "sourceState": "offnormal",
  "ackStateDisplay": "unacknowledged",
  "ackState": "unacknowledged",
  "ackRequired": true,
  "alarmTransitionDisplay": "...",
  "alarmTransition": "...",
  "sourceClass": "HighTemp",
  "sourceClassDisplay": "High Temperature",
  "user": "operator1",
  "alarmData": {...},
  "noteCount": 0
}
```

Response del `query()` endpoint: `{ "total": int, "records": [{...}, ...] }`. Paginación: 15 registros por página hardcodeado (`limit = 15` en `AlarmData.java:408`). El offset se calcula como `(page-1) * limit`.

**G51-8 GOTCHA LIMIT HARDCODEADO 15**: `AlarmData.QueryTask` tiene `int limit = 15` hardcodeado (`AlarmData.java:409`). El REST endpoint recibe `limit` como query param pero el BOX handler siempre usa 15. Inconsistencia potencial entre los dos canales de query.

---

## 51.4 Antipatterns adicionales detectados (continúan numeración del Bloque 50)

**AP-13 — socket.io-client en package.json sin usar**
`socket.io-client 2.5.0` está declarada como dependencia del frontend pero el bundle de producción usa WebSocket nativo. Es deuda técnica visible — confunde a quien lee el package.json sobre el protocolo real.
**Riesgo**: Bajo en funcionalidad. Medio en comprensión del código (tramposo).

**AP-14 — Limit 15 hardcodeado en AlarmData.QueryTask**
`int limit = 15` en `AlarmData.java:408` es hardcodeado. El REST `AlarmQueryResponse` puede recibir `limit` por query param, pero el BOX path siempre usa 15. Si el frontend usa el canal BOX para paginar alarmas, no puede controlar el tamaño de página.
**Riesgo**: Medio. La inconsistencia entre REST (limit configurable) y BOX (limit=15) puede causar comportamiento diferente en los dos canales.

**AP-15 — webpack bundle en proyecto Vite**
`app-readable.js` fue generado con webpack, pero el proyecto actual usa Vite 5. El bundle en producción (`rc/js/app.4509efb4.js`) es también webpack (puede verificarse por el runtime `webpackJsonp` en línea 69). El proyecto migró el toolchain pero el bundle en `rc/` no fue regenerado con Vite. Pueden haber diferencias de behavior entre el bundle actual en producción y lo que Vite 5 generaría.
**Riesgo**: Medio. El bundle funciona, pero el toolchain de desarrollo no coincide con el de producción.

**AP-16 — Authorization client-side via profiles JSON**
La autorización de rutas de la SPA está basada en el config JSON (profiles), NO en los roles Niagara. Un usuario con acceso a las DevTools del browser puede inspeccionar y modificar el state del store Vue para bypasear la autorización de rutas. La autorización de servidor (BOX `requiredPermissions="r"` + `CsrfProtectedFilter`) sigue activa, pero la UI puede mostrar funcionalidad que el backend igualmente rechazerá.
**Riesgo**: Bajo en seguridad (el backend tiene sus propias restricciones). Medio en UX (la UI podría mostrar opciones que fallan en el servidor).

**AP-17 — `BReflowSyncService.queueRevokeConfigControl` timer 30s no-reset**
En `BReflowSyncService.java:277`, si el controller actual NO acepta ni rechaza el control request en 30s (`requestTimeout = 30000`), el nuevo requester obtiene el control automáticamente (`acceptControlRequest()`). Este auto-grant puede causar dos clientes simultáneos creyendo que tienen control en una ventana de tiempo.
**Riesgo**: Medio. En deployments multi-usuario puede causar config corrupta si ambos clientes escriben simultáneamente antes de que el lock se transfiera completamente.

**AP-18 — BackupManager.CreateBackupTask polling de tamaño con retry limit 50**
En `BackupManager.java:228-233`:
```java
for (int tries = 0; reflowConfig.getSize() != size; tries++) {
    size = reflowConfig.getSize();
    Thread.sleep(100L);
    if (tries > 50) break;
}
```
Se espera a que el archivo tenga tamaño estable antes de copiarlo. El max de 50 intentos × 100ms = 5 segundos. Si el archivo tarda más en escribirse (config muy grande o filesystem lento), el backup puede copiar un archivo incompleto sin error.
**Riesgo**: Bajo-medio. El backup silenciosamente puede estar corrupto si el config JSON es > 20KB y el filesystem está bajo carga.

**AP-19 — `AlarmData.testActive` usa `!=` para comparar Strings**
En `AlarmData.java:298-299`:
```java
String state = alarm.getSourceState().getTag();
return Boolean.TRUE.equals(active) ? state != "normal" : state == "normal";
```
`state != "normal"` y `state == "normal"` comparan **referencias** de String, NO contenido. En Java, comparar Strings con `==`/`!=` es un bug conocido — puede funcionar por String interning pero NO está garantizado. Debería ser `!"normal".equals(state)` y `"normal".equals(state)`.
**Riesgo**: Medio-alto. Puede causar que alarmas activas no se filtren correctamente, mostrando alarmas resueltas como activas o viceversa. Bug silencioso dependiente de JVM internals.

**AP-20 — Google Analytics `ga` ID en BReflowService expuesto via BOX**
En `window.injectBaja()` (`app-readable.js:121368`): `u = r.get("ga")`. El ID de Google Analytics está en un property del `BReflowService` y se carga via BOX (el `BReflowService` es un BComponent observable). Esto significa que el GA ID es visible en el protocolo BOX (tráfico de red). No es un secreto, pero es una práctica cuestionable incluir IDs de analytics como properties del BComponent.
**Riesgo**: Bajo. El GA ID no es sensible, pero demuestra que el BReflowService expone más información de la necesaria via el protocolo BOX.

---

## 51.5 Refinamiento FINAL Bloques 42-49

Esta es la segunda pasada (la primera fue el Bloque 50). Ahora con el bundle `app-readable.js` verificado:

### Bloque 42 — Subscriber lifecycle

**(a) Qué resolvieron juntos -rt + app-readable.js**: El subscriber lifecycle está COMPLETAMENTE implementado en `app-readable.js:3521-3729`. El patrón es claro: `me.subscribe(uuid, component[], callback)` + `me.unsubscribe(uuid)` en Vue lifecycle hooks. El `oe = new baja.Subscriber` se crea lazy. El `baja.BatchResolve` se usa para batch resolve de múltiples ORDs.

**(b) Qué falta investigar todavía (abstracto Niagara)**: El leak de subscriptions cuando el iframe se destruye SIN llamar `destroyApp` — ¿cómo el servidor BOX de Niagara limpia subscripciones de sesiones cerradas? El `Subscriber` del lado servidor tiene lifecycle propio. Ver Bloque 22 (BOX protocol + Subscriber API).

**(c) Prioridad final**: **ALTA** → Ya se tiene suficiente contexto de Reflow. Lo que falta es el comportamiento del servidor BOX (fuera de Reflow).

**(d) ¿Se vuelve innecesario?** NO — todavía hay comportamiento Niagara subyacente por investigar.

### Bloque 43 — Schedule frontend

**(a) Qué resolvieron**: El GET es via BQL en el servidor. El write sigue sin existir.

**(b) Qué falta**: La serialización JSON de `BWeeklySchedule` (cómo los slots del schedule se convierten a JSON para el frontend).

**(c) Prioridad final**: **BAJA** → Reflow solo lee schedules. No hay write implementado. No es bloqueante.

**(d) ¿Innecesario?** NO — la serialización de BWeeklySchedule es relevante para cualquier módulo que exponga schedules.

### Bloque 44 — Alarm Console

**(a) Qué resolvieron**: El shape exacto del AlarmRecord JSON está confirmado (29 campos). La paginación está hardcodeada a 15 registros en el BOX path. El ACK es via `baja.Ord.make("alarm:").get().ackAlarms({ids})`. El canal dual REST+BOX está completamente documentado.

**(b) Qué falta**: El behavior del `AlarmDbConnection.timeQuery` vs `BITable` BQL — tienen performance diferente para queries grandes.

**(c) Prioridad final**: **MEDIA** → El wire protocol ya está completo. Lo que falta es performance tuning del backend.

**(d) ¿Innecesario?** NO — hay gotchas de performance en `AlarmData.QueryTask` (AccessController + Thread per request).

### Bloque 45 — History/Charts

**(a) Qué resolvieron**: El JSON shape de history data está completamente documentado. Los estilos (`d3` vs default), contextualRanges, múltiples histories en un request, compare mode, la serialización de tipos (Numeric/Boolean/Enum/String trend records), precision de facets.

**(b) Qué falta**: El comportamiento de `HistoryGhostSubscriber` para histories remotas (de otras stations en un Supervisor). Cómo funciona `subscribeToHistory()` con `asyncHistorySubscribe: true`.

**(c) Prioridad final**: **MEDIA** → El shape del dato está resuelto. Lo que falta es el comportamiento en topologías Supervisor/Subordinate.

**(d) ¿Innecesario?** NO — el subscriber de historia remota es un pattern Niagara general útil.

### Bloque 46 — Writes priority array

**(a) Qué resolvieron**: El write en Reflow va via `component.invoke({slot, value})` (para Actions) o via `actionPoint.invoke({slot, value})` (para puntos configurados como action). El bundle usa `baja.Component.invoke()` directamente, NO un wrapper de prioridad. NO hay evidencia de manejo explícito de prioridades (16 niveles).

**(b) Qué falta**: La API exacta de `baja.Component.invoke()` para writes con priority array. En Niagara, el write a un `BNumericWritable` con prioridad específica es `component.set(value, {priority: 8})` o via `invoke({slot: 'in8', value: v})`. El bundle no muestra este patrón.

**(c) Prioridad final**: **ALTA** → Sigue siendo crítico para control real de setpoints. El gap es ahora más específico: cómo `baja.Component.invoke` maneja el priority array.

**(d) ¿Innecesario?** NO — es una brecha funcional real del módulo.

### Bloque 47 — Bootstrap headless (CRÍTICO)

**(a) Qué resolvieron**: El bootstrap está COMPLETAMENTE documentado en `app-readable.js:121342-121494`. El flujo completo es:
1. -ux monta iframe → `loader.mount()` → espera `iframe.onload`
2. -ux llama `iframeWindow.window.injectBaja(fromWorkbench, widget)`
3. `injectBaja` → RequireJS `window.require(["baja!", tipos, views])` → resolver `BReflowService` via ORD
4. Mount Vue app → `store.dispatch("load", comp)` → GET `/nmodsreflow/config` → LOAD_STATE
5. Connect WebSocket `/nmodsreflow/ws` → join "reflow" channel
6. Subscriptions BOX comienzan en componentes Vue `mounted()`

**(b) Qué falta**: El timeout de 3 minutos en el loader del -ux — exactamente qué lo causa. Sospecha: `window.require(["baja!", ...])` puede tardar si el servidor BOX no está disponible todavía.

**(c) Prioridad final**: **RESUELTO** — el bootstrap está completamente documentado. No requiere bloque adicional.

**(d) ¿Innecesario?** El bloque 47 ya NO se necesita — Reflow tiene la respuesta completa. Economía: **ahorra un bloque**.

### Bloque 48 — RBAC visibility

**(a) Qué resolvieron**: La autorización de UI es client-side via profiles JSON (NO roles Niagara). Los roles Niagara se cargan via `ReflowUserCommands.getRoles()` pero se usan para display, no para autorización de rutas. La autorización real de las operaciones está en el servidor (BOX `requiredPermissions`, `canAcknowledgeAlarms`, etc.).

**(b) Qué falta**: Cómo se mapean los roles Niagara a permisos UI en el config JSON. Si hay roles hardcodeados (`admin`, `operator`) en el sistema de profiles.

**(c) Prioridad final**: **BAJA** → El patrón está claro. El detalle del mapping es específico de Reflow, no de Niagara en general.

**(d) ¿Innecesario?** CASI — lo que queda es específico de Reflow config JSON, no de Niagara framework.

### Bloque 49 — Facets / i18n

**(a) Qué resolvieron**: En el bundle, los facets de puntos se usan en la serialización de history data (`HistoryData.java` — `valueFacets`, `units`, `precision`, `range`, `trueText`, `falseText`). El frontend recibe `units` y `ordinals` como campos del objeto historia. No hay i18n — los textos están en inglés hardcodeado.

**(b) Qué falta**: Cómo el frontend maneja `BFacets` de puntos para display de valores en tiempo real (Subscriber). El formato de `getFacets("out")` en BajaScript.

**(c) Prioridad final**: **BAJA** → El patrón de facets para history está documentado. El display en tiempo real via Subscriber sigue siendo un gap menor.

**(d) ¿Innecesario?** CASI — el gap restante es minimal y general.

---

## 51.6 TODOs honestos

**TODO-1 resuelto**: socket.io vs WebSocket puro — REFUTADO. No hay mismatch.

**TODO-2 resuelto**: Shape de `AlarmRecord` JSON — documentado en 51.3.1.

**TODO-3 resuelto**: Shape de history data — documentado en 51.3.1.

**TODO-4 resuelto**: `window.injectBaja` en bundle — CONFIRMADO implementado en `app-readable.js:121342`.

**TODO-5 resuelto**: `BReflowSyncService` — auditado completamente en 51.1.2 y `BReflowSyncService.java`.

**TODO-6 pendiente**: Mecanismo de routing del ORD scheme `reflow:`. `BReflowScheme.resolve()` — itera `BComponentSpace` buscando `BReflowService`. En stations grandes con muchos componentes, esto puede ser O(n) sobre el space. `BReflowScheme.java` existe en el source pero no fue auditado en detalle.

**TODO-7 resuelto**: Headers de auth — same-origin, JSESSIONID va automáticamente. `withCredentials` no aplica para same-origin en axios.

**TODO-8 resuelto**: `app-readable.js` — auditado completamente en este bloque.

**TODO nuevos**:

**TODO-51-1**: El bundle `app-readable.js` es webpack, el proyecto actual es Vite 5. No está claro si `chunk-vendors.3fecdb47.js` + `app.4509efb4.js` en producción son el output Vite o también webpack. Si son webpack, el toolchain de desarrollo (Vite) no puede generar el bundle de producción actual. Verificar observando el runtime del bundle minificado.

**TODO-51-2**: `HistoryGhostSubscriber` (`HistoryGhostSubscriber.java`) — auditado parcialmente. El mecanismo de subscription a historias remotas (Supervisor/Subordinate) con `asyncHistorySubscribe: true` no está completamente documentado.

**TODO-51-3**: `BReflowAlarmCommands.getAlarmsSinceTime()` usa BQL `station:|alarm:|bql:select * where timestamp.millis >= {ts}`. No tiene paginación ni límite. Para una station con muchas alarmas en un período largo, este query puede ser O(n) sin bound. Si se llama frecuentemente (polling de novedades), puede degradar performance.

**TODO-51-4**: `BReflowScheme.java` — el ORD scheme `reflow:` no fue auditado en detalle. Cómo resuelve el path `reflow:/Equipment/Floor1` a una ruta del SPA. La referencia en `module-include.xml:3`: `<type class="...BReflowScheme" name="ReflowScheme" ordScheme="reflow"/>`.

**TODO-51-5**: El módulo -rt depende de `bacnet-rt` pero no tiene lógica BACnet visible. Verificar si la dependencia es realmente necesaria o si es un artefacto de la configuración del build.

---

## 51.7 Próximos pasos recomendados

### Orden de prioridad para bloques futuros

1. **[INMEDIATO] NO hay bloque 47 — ahorrado**: El bootstrap está completamente documentado en este bloque. No se necesita un bloque separado.

2. **[ALTA] Bloque 52 — BajaScript wire protocol (Subscriber + BOX) profundo**: El bloque más valioso ahora es investigar cómo funciona `baja.Subscriber` del lado servidor — específicamente cómo el BOX server (Niagara) maneja subscriptions, lease management, y cleanup cuando el iframe se destruye. Esto cierra el último gap técnico del stack Reflow y también es útil como conocimiento general del framework (Bloque 22 + profundización).

3. **[ALTA] Bloque 53 — Priority array write via BajaScript**: La API de `baja.Component.invoke()` con priority levels. Cómo se escribe a un `BNumericWritable` con prioridad específica. Cómo se hace relinquish (set null a un nivel). Esto es bloqueante para cualquier módulo de control.

4. **[MEDIA] Bloque 54 — `BReflowScheme` + ORD scheme custom**: El ORD `reflow:` y cómo Niagara registra y resuelve ORD schemes custom. Útil como conocimiento general para módulos que necesitan navegación custom.

5. **[BAJA] Bloque 55 — Cierre de Reflow audit**: `BReflowScheme.java`, `HistoryGhostSubscriber`, dependency `bacnet-rt` verification. Limpieza de TODOs menores.

### Recomendación concreta

El **Bloque 52** (BajaScript subscriber server-side + BOX lease management) debería ser el siguiente. Razones:
- Es el único gap técnico mayor que queda sin documentar en el stack Reflow.
- Es conocimiento de framework puro (no específico de Reflow) que aplica a cualquier módulo Niagara con live data.
- El servidor BOX de Niagara y su comportamiento de cleanup de subscriptions es un gotcha documentado en el Bloque 19 pero sin profundidad.
- Cierra el loop del Subscriber lifecycle real (client-side documentado en este bloque + server-side por documentar).

---

*Archivo producido: `/home/cristian/niagara-research/niagara-mental-model-bloque51.md`*

*Fuentes auditadas en este bloque*:
- `nmodsreflow-rt/src/rc/js/app-readable.js` (5.8 MB, 123,237 líneas) — bundle webpack deobfuscado v1.7.5
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowAlarmCommands.java`
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/alarms/AlarmData.java`
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/HistoryData.java`
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/sockets/SocketServlet.java`
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/sync/BReflowSyncService.java`
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/backups/BackupManager.java`
- `nmodsreflow-rt/module-include.xml`
- `nmodsreflow-rt/module-permissions.xml`
- `nmodsreflow-rt/nmodsreflow-rt.gradle.kts`
- `nmodsreflow-rt/src/WEB-INF/jetty-web.xml`
- `nmodsreflow-rt/src/WEB-INF/web.xml` (referencia)
