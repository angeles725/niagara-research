# Bloque 50 — Reflow-Clean-177 audit cross-stack (Par A: reflow-frontend ↔ nmodsreflow-ux)

**Fecha**: 2026-05-04
**Método**: Investigación empírica READ-ONLY. Lectura directa de fuentes Java, JS, y configuraciones del proyecto Reflow-Clean-177 en `/home/cristian/modules/Prototipos/Reflow-Clean-177/`. Sin ejecución de builds ni tests.
**Scope**: `reflow-frontend/` (SPA Vue 2.7) + `nmodsreflow/nmodsreflow-ux/` (3 clases Java + 6 archivos JS Niagara). Referencias cruzadas al `-rt` solo para confirmar contratos, sin profundizar.

---

## 50.0 Contexto y scope

Este bloque cierra el gap entre la investigación abstracta de Niagara (Bloques 1–41) y el código real del proyecto Reflow v1.7.7. Es el primer bloque con evidencia de dos puntas del cable.

**Proyecto**: Reflow es un módulo Niagara personalizado que actúa como dashboard BAS (Building Automation System). Tiene tres capas:

1. **`reflow-frontend/`** — SPA Vue 2.7 compilada con Vite 5. Vive como asset estático dentro del módulo Niagara.
2. **`nmodsreflow-ux/`** — Módulo Niagara UX (runtimeProfile=ux). Contiene 3 clases Java (`BReflow`, `BReflowConfig`, `BReflowRedirect`) + 6 archivos JS RequireJS. Actúa como bridge entre el Niagara Workbench/HX y el SPA.
3. **`nmodsreflow-rt/`** — Módulo Niagara RT (runtimeProfile=rt). Contiene los servlets HTTP reales, WebSocket, comandos BOX, lógica de historia/alarmas/backups. **Out of scope profundo** para este bloque — se referencia para confirmar contratos.

**Topología en producción**:

```
Browser / WB
  └─ Niagara HX frame (hx://)
       └─ BReflow/BReflowConfig widget (nmodsreflow-ux)
            └─ <iframe id="nmods_iframe">
                 └─ /nmodsreflow/ (SPA Vue)
                      ├─ HTTP REST → /nmodsreflow/* (BaseServlet in -rt)
                      └─ WebSocket → /nmodsreflow/ws (SocketServlet in -rt)
```

---

## 50.1 Frontend stack (Vite/Vue)

### 50.1.1 Build & deploy

**Stack confirmado** (CONFIRMADO, `package.json:1`):
- Vue 2.7.16 (NO Vue 3)
- Vite 5.4.14 con `@vitejs/plugin-vue2` 2.3.3
- Vuex 3.6.2 + Vue Router 3.6.5
- axios 0.21.4 (HTTP client)
- socket.io-client 2.5.0 (WebSocket)
- view-design 4.7.0 (componente UI iView)
- D3 7.9.0 (charts)
- dayjs 1.11.13 (fechas)

**Build output** (CONFIRMADO, `vite.config.js:96-120` + `nmodsreflow-rt/src/rc/`):
- `dist/js/app.[hash].js` → renombrado a `rc/js/app.[hash].js` en el módulo
- `dist/js/chunk-vendors.[hash].js` → `rc/js/chunk-vendors.[hash].js`
- `dist/css/[name].[hash].css` → `rc/css/`
- `dist/fonts/` → `rc/fonts/` (FontAwesome Pro light/regular/solid)
- `dist/img/` → `rc/img/`
- `rc/index.html`, `rc/config.html`, `rc/favicon.ico`, `rc/point-matrix.json`, `rc/icon-categories.json`, `rc/icon-search.json` — archivos estáticos adicionales empaquetados en el -rt

**El bundle compilado vive dentro del JAR del módulo -rt**, dentro del path `rc/`. El servlet `FileResponse.java` los sirve desde `module://nmodsreflow/rc{path}` (CONFIRMADO, `FileResponse.java:49`).

**Base URL en producción**: `/nmodsreflow/` (CONFIRMADO, `vite.config.js:44`).

**RequireJS injection** (CONFIRMADO, `vite.config.js:15-19`):
En producción el plugin `niagaraHtmlPlugin` inyecta en `index.html` antes de `</head>`:
```html
<link href="/requirejs/config.js" rel="preload" as="script">
<link href="/module/js/com/tridium/js/ext/require/require.js" rel="preload" as="script">
<script src="/requirejs/config.js"></script>
<script src="/module/js/com/tridium/js/ext/require/require.js"></script>
```
Esto carga el contexto RequireJS de Niagara para que `window.injectBaja()` funcione cuando la SPA corre dentro del iframe.

**Dev server** (CONFIRMADO, `vite.config.js:74-91`):
Puerto 3000. En modo dev, las rutas `/nmodsreflow/*` son interceptadas por un middleware local que sirve fixtures JSON desde `mock/fixtures/nmodsreflow/`. Los proxies a Niagara están desactivados en dev para evitar `ECONNREFUSED`.

**VITE_* env vars**: NO encontradas — no hay `.env` ni referencias a `import.meta.env.VITE_*` en el código auditado. La única variable usada es `import.meta.env.DEV` en `main.js:116` para arrancar el store sin backend.

**G50-1 GOTCHA BUILD**: El bundle ya compilado en `nmodsreflow-rt/src/rc/` tiene hashes hardcodeados (`app.4509efb4.js`, `chunk-vendors.3fecdb47.js`). Si se re-compila `reflow-frontend/` y no se copia el output a `nmodsreflow-rt/src/rc/`, el módulo sirve el bundle viejo. NO hay script de sincronización automático visible en el workspace.

### 50.1.2 Auth flow

**CONFIRMADO**: Reflow NO maneja autenticación propia. Depende 100% del sistema de autenticación de Niagara.

El flujo es:
1. El usuario accede a Niagara via HX (browser) — autenticado por Niagara via HELLO/SCRAM-SHA256 (Bloque 18).
2. El navegador establece una sesión HTTP con Niagara (cookie `JSESSIONID` del Jetty de Niagara).
3. Cuando el widget `BReflow` (nmodsreflow-ux) se carga en el HX frame, monta un `<iframe>` apuntando a `/nmodsreflow/` — **mismo origen** porque es el mismo Jetty.
4. El iframe hereda la sesión del navegador. Todas las llamadas REST y WebSocket llevan el mismo `JSESSIONID`.

**Evidencia código**:
- `BReflowWebSocketAcceptor.onConnect()` extrae `this.cx = req.getHttpServletRequest().getAttribute("niagara.context")` — usa el contexto Niagara de la sesión HTTP existente (CONFIRMADO, `BReflowWebSocketAcceptor.java:229`).
- `this.acceptCx.getUser().getUsername()` en `makeClientInfo()` confirma que el usuario viene de la sesión Niagara (CONFIRMADO, `BReflowWebSocketAcceptor.java:405`).

**Cookies**: La SPA usa `vue-cookies` (`main.js:47-58`), pero en los plugins mocks no se gestiona ninguna cookie de autenticación propia. INFERIDO — las cookies de autenticación son las del Jetty de Niagara (JSESSIONID). NO hay evidencia de cookies Reflow-specific con SameSite/Secure configuradas por el módulo.

**CSRF**: El plugin `niagara.js` NO maneja CSRF tokens. INFERIDO — el CSRF lo maneja el Niagara Jetty globalmente via `CsrfProtectedFilter` (Bloque 18). La SPA corre en el mismo origen (same-origin: `/nmodsreflow/` está en el mismo Jetty).

**Bearer tokens**: NO. No hay evidencia de Bearer tokens ni Authorization headers en ningún plugin ni en el api layer.

**G50-2 GOTCHA AUTH**: En modo "redirect" (`BReflowRedirect`), la SPA corre fuera del iframe: `window.top.location.href = '/nmodsreflow/'`. En este caso, el SPA se abre directamente como página principal en el browser — también same-origin. Funciona porque Niagara ya autenticó al usuario. Pero el redirect se hace sin pasar contexto de widget al iframe, lo cual rompe `injectBaja()`. Ver sección 50.3.3.

### 50.1.3 Subscriptions live data

**CANAL 1 — BOX (`api/box.js`)** (CONFIRMADO, `box.js:1-15`):

La SPA usa BOX (Building Object eXchange) de Niagara para queries y operaciones. Patrón:
```js
window.top.niagara.box.serverSideCall(typeSpec, method, args, callback)
```
Donde `typeSpec` es algo como `'nmodsreflow:ReflowAlarmCommands'` y `method` es `'getActiveAlarmCounts'`.

Los 7 typeSpecs confirmados:
- `nmodsreflow:ReflowNavCommands` — `getNavChildren`, `bformat`
- `nmodsreflow:ReflowFileCommands` — `listFiles`
- `nmodsreflow:ReflowCSVCommands` — `loadPointMap`
- `nmodsreflow:ReflowHistoryCommands` — `getData`, `getDevices`, `getDeviceTree`, `getList`, `getQuickList`, `getGroupNames`, `getGroupTree`
- `nmodsreflow:ReflowAlarmCommands` — `getClasses`, `querySources`, `getUuidsForSources`, `getAlarmByUuid`, `getActiveAlarmCounts`, `getUnackedAlarmCounts`, `canAcknowledgeAlarms`, `getAlarmsSinceTime`
- `nmodsreflow:ReflowUserCommands` — `getRoles`, `getAllRoles`
- `nmodsreflow:ReflowBQLCommands` — `query`
- `nmodsreflow:ReflowLicenseCommands` — `licenseData`, `refreshLicense`

Total: 21 BOX methods (CONFIRMADO, `box.js` — completo).

El BOX usa el WebSocket `/wsbox` de Niagara (protocolo Tridium BOX, NO el WebSocket propio de Reflow `/ws`). Confirmado por `window.top.niagara.box` — se accede al contexto del frame padre del Workbench/HX.

**CANAL 2 — WebSocket propio (`api/websocket.js`)** (CONFIRMADO, `websocket.js:1-3`):

Endpoint: `ws[s]://{host}/nmodsreflow/ws` (CONFIRMADO, `web.xml:6-11`).
Protocolo: socket.io 2.5.0 (NO socket.io nativo — es Jetty WebSocket con protocolo personalizado JSON).
Comandos cliente→servidor: `ping` (keepalive 30s), `join`, `route`, `config-route`, `config-control` (request/accept/reject/who), `sync-delta`, `favorites-read`, `favorites-write`.

El WebSocket sirve para:
1. **Presencia multi-usuario** — quién está conectado, quién controla la configuración.
2. **Sincronización config** — deltas JSON Patch entre clientes cuando `multiUserConfig=true`.
3. **Favoritos** — read/write por usuario.

**CONFIRMADO en servidor**: `BReflowWebSocketAcceptor` implementa los handlers via Jetty WebSocket API (`@OnWebSocketConnect`, `@OnWebSocketMessage`, etc.). El servidor NO usa socket.io — es WebSocket puro con JSON. El cliente usa socket.io-client 2.5.0 sobre WebSocket — potencial mismatch de protocolo (socket.io añade framing propio sobre WebSocket). INFERIDO — o el servidor implementa el protocolo socket.io, o hay una capa de adaptación no visible en el código auditado.

**G50-3 GOTCHA WEBSOCKET**: El servidor es Jetty WebSocket puro (`WebSocketServlet`), pero el cliente usa `socket.io-client`. socket.io 2.x usa el protocolo Engine.IO que NO es WebSocket puro — añade un handshake inicial HTTP de polling antes de upgradear a WebSocket. Si el servidor no implementa Engine.IO, la conexión fallará silenciosamente y quedará en polling HTTP. **Esto es un mismatch arquitectural crítico** que debe verificarse en runtime.

**CANAL 3 — BajaScript directo (`api/bajascript.js`)** (CONFIRMADO, `bajascript.js:1-72`):

3 acciones que van directamente via BajaScript (RequireJS + `baja!`):
- `clearCache` / `clearHistoryCache` — invoke action en el `$component` (el `BReflowService`)
- `ackAlarms` / `addNoteToAlarms` / `getAlarmNotes` — via `baja.Ord.make('alarm:').get()` + métodos del `BAlarmService`

El patrón real es `this.$component.invoke({ slot: 'clearCache' })` — esto es BajaScript sobre el protocolo BOX (mismo `window.top.niagara.box`) pero via el API de componente, no `serverSideCall`. Diferencia: `serverSideCall` llama un método Java específico; `invoke` llama una Action del Baja Object Model.

**CANAL 4 — REST HTTP (`api/rest.js`)** (CONFIRMADO, `rest.js:1-347`):

28 endpoints REST en `/nmodsreflow/*` (detallados en sección 50.2.2).

**CANAL 5 — External APIs (`api/external.js`)** (CONFIRMADO, `external.js:1-63`):

3 URLs externas:
- `https://api.niagaramodules.com/products/reflow/current` — version check al startup
- `https://weather.niagaramodules.com/observations/{location}` — observaciones
- `https://weather.niagaramodules.com/forecasts/{location}` — pronóstico
- `https://weather.niagaramodules.com/maps{config}?host={hostId}` — mapa (proxeado via `/nmodsreflow/weather-map`)

**Subscriber API de Niagara**: La SPA tiene `$niagara.subscriber` con métodos `resolve/subscribe/unsubscribe` (CONFIRMADO, `niagara.js:44-55`). Son stubs en el código auditado. En producción, esto usa `baja.Subscriber` (BajaScript) para subscribe a ORDs y recibir cambios push via BOX/wsbox. **NO hay implementación real visible** — queda como stub marcado "Phase 5+".

**G50-4 GOTCHA SUBSCRIBER**: El plugin `$niagara` tiene `subscriber.unsubscribe` como función silenciosa (no-op comentado "called in beforeDestroy, don't spam console"). Esto implica que en producción los unsubscribes se hacen en el lifecycle hook Vue `beforeDestroy`. Si algún componente no hace cleanup, hay leak de subscriptions BOX. No auditable sin runtime.

**Reconexión**: El store tiene `socketAutoReconnect: true` y `socketTimeout: 10000` (CONFIRMADO, `store/index.js`). La lógica real de reconexión no está implementada en el código auditado (stub).

### 50.1.4 Writes

**CONFIRMADO**: La SPA expone `$niagara.points.set()` y `$niagara.invokeAction()` como stubs en el plugin mock.

```js
// niagara.js:214-218
points: {
  get: asyncNull('points.get'),
  set: asyncNull('points.set'),        // write a Writable point
  invoke: asyncNull('points.invoke')   // invoke Action
}
invokeAction: function (ord) { ... }   // niagara.js:248
```

En producción, el write a puntos Niagara va via BajaScript:
```js
baja.Ord.make(ord).get().then(comp => comp.set(value, priority))
```
Esto usa el protocolo BOX/wsbox de Niagara (NO el WebSocket propio de Reflow).

**Priority array**: NO hay evidencia de manejo explícito del priority array (16 niveles) en el código frontend auditado. El `$niagara.points.set()` tiene firma `asyncNull('points.set')` — la implementación real (Phase 5+) deberá pasar priority level como argumento.

**BOverride duration**: NO encontrado en el código auditado.

**G50-5 GOTCHA WRITES**: El frontend tiene `$niagara.points.set()` pero NO hay un endpoint REST `POST /nmodsreflow/*/write`. Todos los writes van via BOX/BajaScript directamente — esto es correcto para un módulo Niagara, pero significa que los writes NO pasan por el servlet custom. La latencia del write depende del pipeline BOX (WebSocket `/wsbox` de Niagara). No hay retry logic visible.

### 50.1.5 Alarmas

**Dual-channel** (CONFIRMADO):

**Via REST** (para queries paginadas con filtros):
- `POST /nmodsreflow/station/alarms/query` — body: `{ query: "range=...&ackState=...&sourceOrd=...&limit=50&offset=0" }` → `{ records: [], count: 0, offset: 0, limit: 50 }`
- `GET /nmodsreflow/station/alarms/csv` — descarga CSV (tipo `alarms` o `source`)

**Via BOX** (para datos live y operaciones):
- `ReflowAlarmCommands.getClasses` — lista de clases de alarma
- `ReflowAlarmCommands.querySources` — ORDs fuente de alarma
- `ReflowAlarmCommands.getUuidsForSources` — UUIDs por fuente
- `ReflowAlarmCommands.getAlarmByUuid` — registro individual
- `ReflowAlarmCommands.getActiveAlarmCounts` — conteo activas
- `ReflowAlarmCommands.getUnackedAlarmCounts` — conteo no-ack
- `ReflowAlarmCommands.canAcknowledgeAlarms` — permiso del usuario (retorna BBoolean)
- `ReflowAlarmCommands.getAlarmsSinceTime(epochMs)` — novedades desde timestamp (para polling)

**Via BajaScript** (para operaciones de escritura):
- `alarm:.ackAlarms({ ids })` — ACK por UUIDs via `BAlarmService.ackAlarms`
- `alarm:.addNoteToAlarms({ ids, notes })` — agregar nota
- `alarm:.getNotes({ uuid })` — leer notas

**Paginación**: El query REST tiene `limit` y `offset` (CONFIRMADO, `rest.js:79`). El componente `AlarmDisplay.vue` usa `selectedPriorities` como filtro — las prioridades son Reflow-specific (high/medium/low), no las 16 del priority array Niagara.

**ACK flow**: `ackAlarms(ids)` usa BajaScript directo sobre `alarm:` — esto invoca la acción nativa del `BAlarmService`. Requiere `operatorWrite` permission en el servicio de alarmas (comentado en `bajascript.js:44`).

**G50-6 GOTCHA ALARMAS**: `canAcknowledgeAlarms` retorna `BBoolean` (NOT `BString`) — único entre todos los BOX endpoints (comentado explícitamente en `box.js:227`). Si el deserializador del frontend asume que todos los BOX returns son strings (como es el caso con `BString.make()`), este endpoint falla silenciosamente.

### 50.1.6 Schedules

**Via REST únicamente** (CONFIRMADO, `rest.js:292-298`):
- `GET /nmodsreflow/station/schedules` — retorna todos los `WeeklySchedule` components via BQL

El comentario en el stub es explícito: "Get all WeeklySchedule components via BQL". En el servidor, `SchedulesDataResponse` ejecuta una BQL query y serializa los resultados.

**Writes a schedules**: NO hay endpoint REST ni BOX para escribir schedules. INFERIDO — si se implementa, sería via BajaScript directo sobre el componente `BWeeklySchedule`, o no está implementado aún.

**G50-7 GOTCHA SCHEDULES**: El frontend solo tiene `getSchedules()` (read-only). No hay endpoint de write visible para modificar schedules desde la SPA. Esto es un gap funcional real.

### 50.1.7 History/Charts

**Dual-channel** (CONFIRMADO):

**Via REST**:
- `GET /nmodsreflow/station/histories` — metadata lista
- `GET /nmodsreflow/station/histories/{name}` — data por nombre (o CSV de múltiples separados por coma)
- `GET /nmodsreflow/station/history-data?histories=...&style=...&range=...&limit=...&comparing=...&compareHistories=...&start=...&end=...&contextualRanges=...` — datos para chart (CONFIRMADO, `HistoryChartDataResponse.java:34-75`)
- `GET /nmodsreflow/station/history-groups` — árbol jerárquico

**Via BOX**:
- `ReflowHistoryCommands.getData(params)` — idéntico a REST history-data (dual implementation)
- `ReflowHistoryCommands.getDevices()` / `getDeviceTree()` — árbol de devices
- `ReflowHistoryCommands.getList()` / `getQuickList()` — lista con metadata / lista simplificada
- `ReflowHistoryCommands.getGroupNames()` / `getGroupTree()` — grupos

**Parámetros de query history-data** (CONFIRMADO, `HistoryChartDataResponse.java:36-75`):
- `histories` — nombres separados por coma
- `style` — tipo de renderizado
- `range` — rango predefinido (last15/lastHour/etc., mínimo 15 — ver `niagara.js:222-235`)
- `limit` — máximo registros
- `comparing` — booleano comparación
- `compareHistories` — histories de comparación
- `start` / `end` — timestamps custom
- `contextualRanges` — rangos contextuales

**Charts**: D3 7.9.0 (`D3chart` componente global registrado en `main.js:62`).

**Cache en servidor**: `BReflowService.historyCache` (default: false) + `historyCacheTTL` (default: 3600s). La acción `clearHistoryCache` va via BajaScript (CONFIRMADO, `bajascript.js:26-31`). `historyGroupCacheRefresh` con refresh diario a `historyGroupRefreshTime` (CONFIRMADO, `BReflowService.java:614-631`).

---

## 50.2 UX Servlet (Java/Niagara)

### 50.2.1 Servlets registrados

**El módulo -ux NO tiene servlets HTTP**. Las 3 clases Java del -ux son `BIJavaScript` widgets (NOT `BWebServlet`). Los servlets HTTP reales están en el módulo -rt.

Lo que el -ux registra (CONFIRMADO, `module-include.xml` del -ux):

| Clase | Tipo | Agent-on | Permiso requerido |
|-------|------|----------|-------------------|
| `BReflow` | `BIJavaScript`, `BIFormFactorMax` | `nmodsreflow:ReflowService` | `r` |
| `BReflowConfig` | `BIJavaScript`, `BIFormFactorMax` | `nmodsreflow:ReflowService` | `rw` |
| `BReflowRedirect` | `BIJavaScript`, `BIFormFactorMax` | `nmodsreflow:ReflowService` | `r` |

Las 3 son `BSingleton` (patrón Niagara — una instancia por JVM via static INSTANCE). Se registran como agents sobre `ReflowService`, accesibles desde el Property Sheet del servicio.

La diferencia entre las 3:
- **`BReflow`** — vista normal: monta el SPA en iframe via `reflowLoader.mount()`. Si `redirectReflowView=true` y NO es Workbench, redirige a `/nmodsreflow` directamente.
- **`BReflowConfig`** — vista de configuración: monta via `reflowLoader.mountConfig()`. Siempre carga el config mode del SPA.
- **`BReflowRedirect`** — solo redirect: si NO es Workbench, redirige a `/nmodsreflow` inmediatamente sin montar iframe.

### 50.2.2 Endpoints expuestos (matriz completa)

Los endpoints HTTP están en el módulo -rt (`BaseServlet` + `SocketServlet`). Se documentan aquí porque son el contrato que el -ux (via iframe) expone al SPA.

**web.xml** (CONFIRMADO, `WEB-INF/web.xml`):
- `SocketServlet` → `/nmodsreflow/ws` (WebSocket upgrade)
- `BaseServlet` → `/nmodsreflow/*` (HTTP GET + POST)

**GET endpoints** (CONFIRMADO, `BaseServlet.doGet()`):

| Path | Handler | Response |
|------|---------|----------|
| `/` o paths no mapeados | `FileResponse.serve("/index.html")` | SPA HTML |
| `/config` | `ConfigResponse` | JSON: config.json desde `^reflow/config.json` |
| `/demos` | `DemoResponse` | JSON: demo.json |
| `/weather-map` | `WeatherMapResponse` | PNG proxeado desde weather.niagaramodules.com |
| `/station/equipment-notes` | `EquipmentNoteResponse` | JSON: notas por device (header `Equipment-Id`) |
| `/station/backups` | `BackupListResponse` | JSON: lista de backups |
| `/station/backups/create` | `BackupCreateResponse` | JSON: `{ success, filename }` |
| `/station/backups/apply` | `BackupApplyResponse` | JSON: `{ success }` |
| `/station/backups/destroy` | `BackupDestroyResponse` | JSON: `{ success }` |
| `/station/backups/rename` | `BackupRenameResponse` | JSON: `{ success }` |
| `/station/backups/reset` | `BackupResetResponse` | JSON: `{ success }` |
| `/station/images` | `ImageListResponse` | JSON: `[{ ord, name }]` |
| `/station/files` | `FileTreeResponse` | JSON: árbol recursivo `{ ord, name, icon, type, children }` |
| `/station/image-library` | `ImageLibraryResponse` | JSON: árbol de images del módulo |
| `/station/schedules` | `SchedulesDataResponse` | JSON: BWeeklySchedule components via BQL |
| `/station/histories` | `HistoryListResponse` | JSON: metadata de histories |
| `/station/histories/{name}` | `HistoryDataResponse` | JSON: data de historia por nombre |
| `/station/history-data` | `HistoryChartDataResponse` | JSON: datos para chart (query string params) |
| `/station/history-groups` | `HistoryGroupsResponse` | JSON: árbol de grupos |
| `/station/alarms/csv` | `AlarmCSVResponse` | text/csv |
| `/{static}` | `FileResponse.serve(path)` | Recurso estático desde `module://nmodsreflow/rc{path}` |
| `/demo/.*` | fallback → `/index.html` | SPA HTML (SPA routing) |

**POST endpoints** (CONFIRMADO, `BaseServlet.doPost()`):

| Path | Handler | Body | Response |
|------|---------|------|----------|
| `/config_update` | `ConfigUpdateResponse` | JSON: estado serializado Vuex | vacío/status |
| `/config_delta` | `ConfigDeltaResponse` | JSON: `{ delta: [...RFC6902 ops] }` | `{ timestamp, patched }` |
| `/station/equipment-notes-update` | `EquipmentNoteUpdateResponse` | JSON: notas | vacío |
| `/station/alarms/query` | `AlarmQueryResponse` | JSON: `{ query: "range=...&..." }` | JSON: `{ records, count, offset, limit }` |

**WebSocket `/nmodsreflow/ws`** (CONFIRMADO, `SocketServlet.java` + `BReflowWebSocketAcceptor.java`):

Configuración del servidor:
- `idleTimeout`: 60s
- `maxTextMessageBufferSize`: 64 KB
- `maxTextMessageSize`: 256 KB
- `maxBinaryMessageBufferSize`: 64 KB
- `maxBinaryMessageSize`: 128 KB

Mensajes JSON (CONFIRMADO, `BReflowWebSocketAcceptor.onMessage()`):
- `{ command: "join", channel: "reflow"|"reflow-config" }` → canal status
- `{ command: "leave", channel }` → leave canal
- `{ command: "who", channel }` → lista de miembros
- `{ command: "route", action, route }` → broadcast route a otros
- `{ command: "config-route", action, route }` → broadcast config route
- `{ command: "client-info" }` → retorna `{ type, clientId, configControl, username }`
- `{ command: "licenseCommand" }` → retorna `{ isLicensed }` (desde `BReflowService.ServiceCommand`)
- `{ command: "broadcast", channel }` → broadcast forzado
- `{ command: "sync-delta", ... }` → via `BReflowSyncService`
- `{ command: "favorites-read" }` → via `ReflowOrdTreeFavoritesRead`
- `{ command: "favorites-write", ... }` → via `ReflowOrdTreeFavoritesWrite`

On connect, servidor envía automáticamente `{ type: "client-info", clientId, configControl, username }`.

### 50.2.3 Auth + permissions

**Auth HTTP**: Todas las peticiones al servlet pasan por el Jetty de Niagara antes de llegar a `BaseServlet`. El `niagara.context` está disponible en los `HttpServletRequest` attributes — el servlet lo usa en el contexto WebSocket (CONFIRMADO, `BReflowWebSocketAcceptor.java:229`).

**No hay Unauthenticated endpoint**: El servlet NO tiene ninguna anotación `@Unauthenticated` ni bypasses de auth. Depende del Jetty de Niagara para la autenticación.

**Permisos de agents** (CONFIRMADO, `module-include.xml` del -rt):
- `BReflowAlarmCommands`, `BReflowBQLCommands`, `BReflowCSVCommands`, `BReflowFileCommands`, `BReflowHistoryCommands`, `BReflowNavCommands`, `BReflowUserCommands`, `BReflowLicenseCommands` — todos requieren `requiredPermissions="r"` sobre `ReflowService`.
- `BReflowConfig` (UX) — requiere `rw`.
- `BReflow`, `BReflowRedirect` (UX) — requieren `r`.

**CSP header** (CONFIRMADO, `BaseServlet.setContentSecurityPolicy()`):
Solo se aplica si `hasModernSecurityPolicy=true` (Niagara 4.10+). Si está activo, la CSP permite `default-src 'self' ws: wss: blob: data: niagaramodules.com *.niagaramodules.com ... 'unsafe-inline' 'unsafe-eval'`.

**G50-8 GOTCHA SECURITY**: La CSP incluye `'unsafe-inline' 'unsafe-eval'` explícitamente. Esto invalida la protección XSS de la CSP. Es probablemente necesario para el funcionamiento de RequireJS y los ORDs dinámicos, pero es una debilidad documentada.

### 50.2.4 Static resources (bundle Vite)

**CONFIRMADO** (CONFIRMADO, `FileResponse.java:48-70`):

El SPA bundle (Vite dist) está empaquetado en el módulo -rt bajo `rc/`. El servlet `FileResponse` sirve cualquier path no reconocido como `/nmodsreflow/{path}` buscando `module://nmodsreflow/rc{path}`.

El fallback es SPA-aware: si el archivo no existe en `rc/`, sirve `/index.html` para soportar HTML5 History routing (`FileResponse.serve("/index.html")`). Esto es routing del SPA (CONFIRMADO, `BaseServlet.doGet():249-260`).

**GZIP cache** (CONFIRMADO, `FileResponse.java:22-46`):
Cuando `webCache=true` (default), los assets con extensiones `js/json/csv/css/html` se comprimen con GZIP y se cachean en `^reflow/cache/resources/{md5hash}` en el station home. Subsiguientes requests con `Accept-Encoding: gzip` reciben el archivo cacheado.

**Cache-Control**: NO hay evidencia de headers `Cache-Control` explícitos en `FileResponse`. INFERIDO — el Jetty de Niagara puede añadir headers default, pero el servlet no los configura.

**G50-9 GOTCHA CACHE**: Los nombres de assets Vite incluyen hash de contenido (ej. `app.4509efb4.js`), lo que garantiza invalidación correcta en re-deploys. Sin embargo, `index.html` NO tiene hash — si Niagara cachea `index.html` agresivamente, los usuarios recibirán el HTML viejo apuntando a los JS/CSS viejos.

### 50.2.5 Bridge a -rt (referencias, NO profundización)

El -ux solo hace bridge via:
1. `reflowLoader.mount()` / `mountConfig()` — inyecta el SPA en un `<iframe>` y expone `injectBaja()` / `injectConfig()` como hooks en el `iframeWindow.window`
2. `reflowResolver.resolve()` — parsea ORDs con el custom scheme `reflow:` para extraer la ruta interna del SPA

El -rt es responsable de:
- Todos los servlets HTTP
- El WebSocket propio (`/nmodsreflow/ws`)
- Todas las `BReflow*Commands` classes (BOX handlers)
- `BReflowService` (el BComponent service root)
- `BReflowScheme` (ORD scheme `reflow:`)
- Persistencia config (`^reflow/config.json`)
- Backup management (`^reflow/backups/`)
- Historia y alarmas (via `BHistoryService` / `BAlarmService` de Niagara)

---

## 50.3 Cross-stack contract

### 50.3.1 Tabla endpoints frontend ↔ ux/rt

| # | Método/Tipo | Frontend caller | Path/TypeSpec | Request | Response esperada | Contraparte servidor |
|---|------------|----------------|---------------|---------|-------------------|---------------------|
| 1 | GET REST | `rest.getConfig()` | `/nmodsreflow/config` | — | Objeto config completo (versión 14) | `ConfigResponse` → `^reflow/config.json` |
| 2 | POST REST | `rest.saveConfig()` | `/nmodsreflow/config_update` | Body: estado Vuex serializado; Headers: `Client-Id`, `Client-Username`, `Client-Migration` | vacío | `ConfigUpdateResponse` |
| 3 | POST REST | `rest.saveConfigDelta()` | `/nmodsreflow/config_delta` | Body: `{ delta: RFC6902[] }`; Headers: `Client-Id`, `Client-Username` | `{ timestamp, patched }` | `ConfigDeltaResponse` |
| 4 | POST REST | `rest.queryAlarms()` | `/nmodsreflow/station/alarms/query` | Body: `{ query: "range=...&ackState=...&limit=50&offset=0" }` | `{ records: [], count: 0, offset, limit }` | `AlarmQueryResponse` → `AlarmData.query()` |
| 5-6 | GET REST | `rest.getAlarmsCsv()` | `/nmodsreflow/station/alarms/csv` | Query: `type`, filtros | text/csv | `AlarmCSVResponse` |
| 7 | GET REST | `rest.getHistories()` | `/nmodsreflow/station/histories` | — | `[{metadata}]` | `HistoryListResponse` |
| 8 | GET REST | `rest.getHistoryChartData()` | `/nmodsreflow/station/history-data` | Query: `histories,style,range,limit,...` | `{name: {data}}` | `HistoryChartDataResponse` → `HistoryData.fromComponent()` |
| 9 | GET REST | `rest.getHistoryGroups()` | `/nmodsreflow/station/history-groups` | — | `[{group nodes}]` | `HistoryGroupsResponse` |
| 10 | GET REST | `rest.getBackups()` | `/nmodsreflow/station/backups` | — | `[{name, date}]` | `BackupListResponse` |
| 11 | GET REST | `rest.createBackup()` | `/nmodsreflow/station/backups/create` | Query: `filename` | `{ success, filename }` | `BackupCreateResponse` |
| 12 | GET REST | `rest.applyBackup()` | `/nmodsreflow/station/backups/apply` | Query: `filename` | `{ success }` | `BackupApplyResponse` |
| 13 | GET REST | `rest.destroyBackup()` | `/nmodsreflow/station/backups/destroy` | Query: `filename` | `{ success }` | `BackupDestroyResponse` |
| 14 | GET REST | `rest.renameBackup()` | `/nmodsreflow/station/backups/rename` | Query: `oldName,newName` | `{ success }` | `BackupRenameResponse` |
| 15 | GET REST | `rest.resetConfig()` | `/nmodsreflow/station/backups/reset` | Headers: clientId, username | `{ success }` | `BackupResetResponse` |
| 16 | GET REST | `rest.getEquipmentNotes()` | `/nmodsreflow/station/equipment-notes` | Header: `Equipment-Id` | `[{id, text, author, date}]` | `EquipmentNoteResponse` |
| 17 | POST REST | `rest.updateEquipmentNotes()` | `/nmodsreflow/station/equipment-notes-update` | Header: `Equipment-Id`; Body: notas | vacío | `EquipmentNoteUpdateResponse` |
| 18 | GET REST | `rest.getImages()` | `/nmodsreflow/station/images` | — | `[{ord, name}]` | `ImageListResponse` |
| 19 | GET REST | `rest.getFileTree()` | `/nmodsreflow/station/files` | — | árbol recursivo | `FileTreeResponse` |
| 20 | GET REST | `rest.getImageLibrary()` | `/nmodsreflow/station/image-library` | — | árbol module images | `ImageLibraryResponse` |
| 21 | GET REST | `rest.getSchedules()` | `/nmodsreflow/station/schedules` | — | `[BWeeklySchedule]` | `SchedulesDataResponse` → BQL |
| 22 | GET REST | `rest.getWeatherMap()` | `/nmodsreflow/weather-map` | Query: `config`, `force` | Blob PNG | `WeatherMapResponse` → proxy externo |
| 23 | GET REST | `rest.getDemos()` | `/nmodsreflow/demos` | — | demo config obj | `DemoResponse` → `^reflow/demo.json` |
| 24 | GET REST | `rest.getHistoryByName()` | `/nmodsreflow/station/histories/{name}` | Query: `style,range,limit` | datos historia | `HistoryDataResponse` |
| 26 | GET REST | `rest.getPointMatrix()` | `/nmodsreflow/point-matrix.json` | — | point matrix obj | `FileResponse` → `module://nmodsreflow/rc/point-matrix.json` |
| 27 | GET REST | `rest.getIconCategories()` | `/nmodsreflow/icon-categories.json` | — | categorías | `FileResponse` → `module://nmodsreflow/rc/icon-categories.json` |
| 28 | GET REST | `rest.getIconSearch()` | `/nmodsreflow/icon-search.json` | — | índice búsqueda | `FileResponse` → `module://nmodsreflow/rc/icon-search.json` |
| 37 | BOX | `box.getNavChildren()` | `nmodsreflow:ReflowNavCommands.getNavChildren` | `{ ord, typeFilter }` | `[{name, displayName, ord, icon, type, hasChildren, validType}]` | `BReflowNavCommands.getNavChildren()` → `BINavNode` |
| 38 | BOX | `box.bformat()` | `nmodsreflow:ReflowNavCommands.bformat` | `{ ord, format }` | `string` formateado | `BReflowNavCommands.bformat()` → `BFormat.format()` |
| 39 | BOX | `box.listFiles()` | `nmodsreflow:ReflowFileCommands.listFiles` | `path` string | `[{ord, name, icon, type, children}]` | `BReflowFileCommands.listFiles()` |
| 40 | BOX | `box.loadPointMap()` | `nmodsreflow:ReflowCSVCommands.loadPointMap` | `filePath` | `[{displayName, identifier, group, featured, hidden}]` | `BReflowCSVCommands.loadPointMap()` |
| 41-47 | BOX | `box.historyGet*()` | `nmodsreflow:ReflowHistoryCommands.*` | varios | varios | `BReflowHistoryCommands.*` |
| 48-55 | BOX | `box.alarm*()` | `nmodsreflow:ReflowAlarmCommands.*` | varios | varios — nota #54 retorna BBoolean | `BReflowAlarmCommands.*` |
| 56-57 | BOX | `box.getUserRoles()` / `getAllRoles()` | `nmodsreflow:ReflowUserCommands.*` | — | CSV string de roles | `BReflowUserCommands.*` |
| 58 | BOX | `box.bqlQuery()` | `nmodsreflow:ReflowBQLCommands.query` | `{ query, validateTypes, page, limit }` | `{ items, limit, page, pageCount, total }` | `BReflowBQLCommands.query()` → BQL engine |
| 59-60 | BOX | `box.getLicenseData()` / `refreshLicense()` | `nmodsreflow:ReflowLicenseCommands.*` | — | objeto license | `BReflowLicenseCommands.*` |
| 61-62 | BajaScript | `bajascript.clearCache()` / `clearHistoryCache()` | `$component.invoke({ slot: 'clearCache'/'clearHistoryCache' })` | — | void | `BReflowService.doClearCache()` / `doClearHistoryCache()` |
| 63-65 | BajaScript | `bajascript.ackAlarms()` / `addNoteToAlarms()` / `getAlarmNotes()` | `baja.Ord.make('alarm:').get().*` | `{ ids }` / `{ ids, notes }` / `{ uuid }` | void / `{ notes }` | `BAlarmService` (Niagara nativo, NO en -rt) |
| WS | WebSocket | `websocket.join()` etc. | `ws[s]://{host}/nmodsreflow/ws` | JSON command frames | JSON response frames | `BReflowWebSocketAcceptor.onMessage()` |

### 50.3.2 Wire protocol summary

| Canal | Protocolo | Endpoint | Dirección | Cuándo se usa |
|-------|-----------|----------|-----------|---------------|
| REST | HTTP/HTTPS (Niagara Jetty) | `/nmodsreflow/*` | request/response | Config, backups, alarmas paginadas, historias, imágenes, schedules, notas |
| WebSocket propio | Jetty WebSocket + socket.io-client 2.5.0 | `/nmodsreflow/ws` | bidireccional | Presencia multi-usuario, sync config, favoritos |
| BOX Niagara | HTTP POST `/box` upgrade a WebSocket `/wsbox` | `/wsbox` | bidireccional | Queries BOX (comandos Reflow), subscriptions live Niagara, writes de puntos |
| BajaScript directo | BOX (misma conexión) | via `window.top.niagara.box` | bidireccional | Actions BComponent (clearCache), AlarmService (ack) |
| External | HTTPS | `api.niagaramodules.com`, `weather.niagaramodules.com` | request/response | Version check, weather |

**Payload size estimado**:
- Config JSON: variable, típicamente 50–500 KB (14 módulos de estado)
- History chart data: 10–500 KB según rango y cantidad de puntos
- Alarm query: < 50 KB para 50 registros
- WebSocket message: < 1 KB (JSON control frames)
- BOX frames: < 64 KB por mensaje (limit del servidor)

### 50.3.3 Mismatches y gaps

**GAP-1 — socket.io vs Jetty WebSocket puro** (CRÍTICO):
Frontend usa `socket.io-client 2.5.0` (`package.json:13`). El servidor implementa `WebSocketServlet` de Jetty puro (`SocketServlet.java:12`). socket.io 2.x necesita un servidor socket.io o Engine.IO para el handshake inicial (polling HTTP). Sin un servidor socket.io del lado Java, la conexión puede fallar o degradar. Verificar si `nmodsreflow-rt` tiene algún adapter Engine.IO no visible en el source auditado.

**GAP-2 — `config.html` sin contraparte en el SPA** (MENOR):
Existe `rc/config.html` en los recursos estáticos del módulo, pero en el frontend el modo config se carga via la misma SPA (Vue Router + `isConfig` state en el store). INFERIDO — `config.html` puede ser un archivo legacy o para el Workbench config view. Verificar si `BReflowConfig` lo sirve explícitamente.

**GAP-3 — `injectBaja` / `injectConfig` — contrato incompleto** (ALTO):
`loader.js` llama `iframeWindow.window.injectBaja(fromWorkbench, widget)` o `injectConfig(fromWorkbench, widget)` después de que el iframe carga. Estas funciones deben existir en el contexto global de la SPA. En el frontend auditado (`main.js`, `App.vue`), NO se exponen `window.injectBaja` ni `window.injectConfig` — solo existen como referencias en comentarios y plugin mocks. Esto es una pieza crítica del bootstrap que está marcada como "Phase 5+". Sin esta función, el SPA carga pero queda desconectado del contexto Niagara.

**GAP-4 — `window.destroyApp` ausente** (MEDIO):
`loader.destroy()` llama `iframeWindow.window.destroyApp()`. Esta función tampoco está expuesta en el frontend auditado. Sin cleanup, hay leaks de subscriptions BOX cuando el usuario navega fuera del widget Niagara.

**GAP-5 — Writes a schedules ausentes** (FUNCIONAL):
`GET /station/schedules` existe. No hay `POST /station/schedules/update` ni BOX command para write. La UI puede mostrar schedules pero no modificarlos via la SPA (o los modifica via BajaScript directo sin wrapper documentado).

**GAP-6 — Priority array ignorado en writes** (FUNCIONAL):
`$niagara.points.set()` no tiene parámetro de priority level en el stub. El sistema Niagara tiene 16 niveles de prioridad. Sin especificar el nivel, el write va al nivel por defecto (típicamente nivel 8). Si hay overrides activos en niveles superiores, el write silenciosamente no tiene efecto.

**GAP-7 — Headers `Client-Id` y `Client-Username`** (MEDIO):
`config_update` y `config_delta` esperan headers `Client-Id` y `Client-Username` (CONFIRMADO, `rest.js:48-67`). Los stubs actuales del frontend no los envían. En producción, `clientId` es el `clientSignature` del store (base64 random, CONFIRMADO `store/index.js`) y `username` viene de `$baja.acceptCx.getUser().getUsername()`. Sin estos headers, el servidor puede rechazar las operaciones multi-usuario o no poder identificar al editor.

---

## 50.4 Antipatterns detectados (numerados)

**AP-1 — Socket.io sobre WebSocket puro (protocolo mismatch)**
`socket.io-client 2.5.0` (frontend) vs `WebSocketServlet` Jetty puro (servidor). Socket.io 2.x usa Engine.IO que NO es compatible con WebSocket puro. Si esto funciona en producción, hay una capa adapter en el -rt no auditada; si no hay adapter, la conexión falla silenciosamente.
**Riesgo**: Alto. En producción el WS puede estar permanentemente en polling HTTP fallback, sin error visible.

**AP-2 — `injectBaja` / `injectConfig` / `destroyApp` sin implementar**
Los tres hooks críticos del ciclo de vida del iframe no están implementados en el SPA (están marcados como "Phase 5+"). La SPA arranca en dev sin estos hooks via `store.commit('LOAD_STATE', {})`. En producción, si `injectBaja` no está expuesto como `window.injectBaja`, el widget UX queda en un spinner de 3 minutos y luego muestra error.
**Riesgo**: Crítico en producción.

**AP-3 — Stubs en el 100% del API layer**
Todos los métodos en `api/rest.js`, `api/box.js`, `api/bajascript.js`, `api/websocket.js` retornan `Promise.resolve()` stubs. La SPA funciona en dev vía mocks, pero NO hay integración real implementada. El código documenta esto como "Real implementation: Phase 5+".
**Riesgo**: Bloqueante para producción.

**AP-4 — `c.in(input.read(buf))` doble cierre en `FileResponse`**
En `FileResponse.java:44`, hay `in.close()` duplicado tras el GZIP copy. El segundo cierre sobre un stream ya cerrado lanza `IOException` en algunos JVMs silenciosamente (no afecta el resultado pero indica código de copia descuidada).
**Riesgo**: Bajo, pero symptomático de calidad del código de respuesta.

**AP-5 — Error handling en BaseServlet: todos los 500 retornan HTML**
Los bloques catch en `BaseServlet.doGet()` responden con `text/html` y `<h1>ERROR 500</h1>`. El frontend usa axios que parsea respuestas como JSON. Cuando ocurre una excepción, axios recibe HTML y el parse falla, probablemente mostrando un error genérico sin contexto.
**Riesgo**: Medio. Dificulta debugging de errores en producción.

**AP-6 — Falta `Cache-Control` en assets estáticos**
`FileResponse` sirve JS/CSS del bundle Vite sin `Cache-Control` header. Los nombres de archivo tienen hash (correctos para cache busting), pero sin el header, el comportamiento del cache del browser es indeterminado.
**Riesgo**: Bajo en funcionalidad, medio en performance (re-downloads innecesarios).

**AP-7 — `crossOriginHosts` configurable sin validación**
`BReflowService.getCrossOriginHosts()` retorna un string libre que se concatena directamente en el header CSP. Si un admin pone un valor malicioso (ej. `*`), la CSP queda completamente permisiva.
**Riesgo**: Medio. Requiere acceso de admin, pero el patrón es inseguro.

**AP-8 — `$niagara.subscriber.unsubscribe` es no-op en mock**
El comentario en `niagara.js:51` dice "silent — called in beforeDestroy, don't spam console". En producción, si el unsubscribe real falla (ej. componente destruido antes de que la promise resuelva), las subscriptions quedan activas en el servidor BOX. El no-op mock enmascaró este bug potencial durante el desarrollo.
**Riesgo**: Medio. En navegación intensiva puede acumularse memoria en el servidor BOX.

**AP-9 — `ReflowAlarmCommands.canAcknowledgeAlarms` retorna BBoolean (no BString)**
Único BOX endpoint que retorna `BBoolean` (CONFIRMADO en comentario `box.js:227`). Si el deserializador del frontend trata todos los retornos como string, el valor `true`/`false` puede ser mal parseado. No hay lógica de deserialización en los stubs actuales — el riesgo aplica a la implementación futura.
**Riesgo**: Medio.

**AP-10 — Backup operations via GET (no POST)**
`/station/backups/create`, `/backups/apply`, `/backups/destroy`, `/backups/rename`, `/backups/reset` son todas operaciones destructivas servidas via GET HTTP. Esto viola REST conventions y puede ser invocado por pre-fetchers de browser o crawlers. Deberían ser POST.
**Riesgo**: Bajo en producción Niagara (no hay crawlers), pero es mal diseño.

**AP-11 — `alarm:` ORD scheme hardcodeado en BajaScript calls**
`baja.Ord.make('alarm:').get()` para ack/notes. El scheme `alarm:` resuelve al `BAlarmService` en la station. Si la station no tiene `BAlarmService` configurado (station mínima o licencia sin alarmas), la promise rechaza sin mensaje de error útil.
**Riesgo**: Bajo en entornos normales.

**AP-12 — `window.top.niagara.box` sin fallback**
Las llamadas BOX asumen que `window.top.niagara` existe. Si la SPA se abre directamente (modo redirect, no en iframe), `window.top.niagara` puede ser undefined, crasheando todo el API layer. El plugin `niagara.js` mockea esto, pero la implementación real no tiene este fallback.
**Riesgo**: Alto en modo redirect (BReflowRedirect).

---

## 50.5 Refinamiento Bloques 42–49 (post-Reflow)

### Bloque 42 — Subscriber lifecycle

**(a) Qué Reflow ya resolvió**: El patrón de `subscribe/unsubscribe` en lifecycle hooks Vue está diseñado pero no implementado. `$niagara.subscriber.unsubscribe` se llama en `beforeDestroy`. El cleanup `reflowLoader.destroy()` llama `destroyApp()` en el iframe para cleanup global.

**(b) Qué falta investigar**: Cómo `baja.Subscriber` maneja reconexiones BOX cuando `/wsbox` se cae. Qué pasa con las subscriptions cuando el iframe es destruido pero el WebSocket BOX no se cierra inmediatamente. El `gap` de `window.destroyApp` no implementado.

**(c) Prioridad ajustada**: **ALTA**. Reflow demuestra que el cleanup de subscriptions es un gap real — sin `destroyApp` implementado, hay leaks en cada navegación en Workbench. Investigar antes que Bloques 44-45.

### Bloque 43 — Schedule frontend

**(a) Qué Reflow ya resolvió**: Un endpoint REST simple `GET /station/schedules` via BQL. Solo lectura.

**(b) Qué falta investigar**: Cómo serializa el servidor un `BWeeklySchedule` a JSON. Qué campos incluye. Cómo se haría un write (el SPA no tiene endpoint para ello). La relación entre `BWeeklySchedule` y el `driverSchedule` framework (exportar a BACnet, LON, etc.).

**(c) Prioridad ajustada**: **BAJA**. Reflow solo lee schedules. El write es un gap funcional pero no está en el MVP del módulo.

### Bloque 44 — Alarm Console

**(a) Qué Reflow ya resolvió**: El patrón dual REST+BOX está claramente establecido. El query con filtros es `POST /station/alarms/query` con body `{ query: "range=...&ackState=...&limit=50&offset=0" }`. El ACK es via BajaScript directo `alarm:.ackAlarms({ ids })`. Los conteos live son via BOX `getActiveAlarmCounts` + `getUnackedAlarmCounts`.

**(b) Qué falta investigar**: El formato exacto de `AlarmData.query()` (el JSON de un `AlarmRecord` serializado). La estructura del campo `records` en la respuesta. Cómo funciona `getAlarmsSinceTime(epochMs)` — si es polling puro o si hay notificación push. El formato de `ackState` y `range` en el query string.

**(c) Prioridad ajustada**: **ALTA**. Los alarm components ya están en el frontend (`AlarmDisplay.vue`, `AlarmCards.vue`, `AlarmConsoleForm.vue`). Investigar el wire exact shape para conectarlos.

### Bloque 45 — History/Charts

**(a) Qué Reflow ya resolvió**: El endpoint `GET /station/history-data` con query params `histories,style,range,limit,start,end` está confirmado. D3 es el renderer. La cache de grupos es un feature extra (`historyGroupCacheRefresh`). El BOX alternativo existe como dual path.

**(b) Qué falta investigar**: El formato JSON exacto de `HistoryData.fromComponent()` — qué shape tiene el response (¿`{name: [{timestamp, value}]}` o algo más complejo?). El campo `style` — qué valores acepta. El campo `contextualRanges`. Cómo maneja history data con rollups (aggregation).

**(c) Prioridad ajustada**: **ALTA**. D3chart ya está como componente global. El history data format es el bloqueante crítico para implementar los charts.

### Bloque 46 — Writes priority array

**(a) Qué Reflow ya resolvió**: Que el write va via BajaScript (`$niagara.points.set()`), NO via REST. Que el priority level no está en los stubs actuales y es una deuda técnica explícita.

**(b) Qué falta investigar**: La API exacta de `baja.Component.set(value, priority)` en BajaScript. Cómo pasar un BStatusNumeric vs un raw number. Cómo manejar el relinquish (set al nivel null para liberar priority). El BOverride duration para Modbus/BACnet overrides temporales.

**(c) Prioridad ajustada**: **ALTA**. Es el feature más crítico para un BAS dashboard operativo. Sin esto, los operadores no pueden controlar setpoints.

### Bloque 47 — Bootstrap headless (CRÍTICO)

**(a) Qué Reflow ya resolvió**: El patrón real del bootstrap está ahora completamente documentado:
1. El -ux monta un iframe apuntando a `/nmodsreflow/#/path`
2. El iframe espera hasta que `iframe.onload` se dispara
3. El -ux llama `iframeWindow.window.injectBaja(fromWorkbench, widget)`
4. La SPA debe exponer `window.injectBaja` que recibe el widget Niagara y conecta el store
5. En producción, `injectBaja` llama `store.dispatch('load')` que hace `GET /nmodsreflow/config` y luego `store.commit('LOAD_STATE', data)`
6. El widget pasa la referencia `$component` (el `BReflowService`) a la SPA
7. La SPA usa `$component.invoke()` para acciones BajaScript y `window.top.niagara.box` para BOX

**(b) Qué falta investigar**: Cómo `injectBaja` conecta `baja!` (RequireJS) al runtime Vue. Qué es exactamente el parámetro `widget` que recibe — parece ser el `bajaux/Widget` JS object. Cómo `$baja.Subscriber` se inicializa con el contexto correcto. El timeout de 3 minutos en Workbench (`loader.timeout: 3 * 60 * 1000`) — qué lo causa.

**(c) Prioridad ajustada**: **CRÍTICO / ALTA**. Este es el primero a investigar después de este bloque. Sin `injectBaja` implementado, nada del API layer real funciona. Es el puente entre el mundo Vue y el mundo Niagara.

### Bloque 48 — RBAC visibility

**(a) Qué Reflow ya resolvió**: `BReflowUserCommands.getRoles()` retorna un CSV de roles del usuario actual. `getAllRoles()` retorna todos los roles de la station. La vista de config (`BReflowConfig`) requiere permiso `rw` vs `r` de la vista normal.

**(b) Qué falta investigar**: Cómo el SPA usa los roles para ocultar/mostrar elementos. Si hay roles hardcodeados tipo `admin`, `operator`, `viewer` en el frontend. El binding entre roles Niagara (strings libres) y permisos de UI Reflow.

**(c) Prioridad ajustada**: **MEDIA**. La infraestructura de roles está (BOX commands). La lógica de UI binding es el gap.

### Bloque 49 — Facets / i18n

**(a) Qué Reflow ya resolvió**: `$niagara.util.facets.parse()` y `facets.format()` están en el plugin mock. Los rangos de tiempo están hardcodeados en inglés en `$niagara.util.timerange.labels` (15 valores). El módulo lexicon está vacío (CONFIRMADO, `module.lexicon` del -ux). No hay i18n propia de Reflow.

**(b) Qué falta investigar**: Cómo `BFacets` del servidor se serializa y se envía al frontend. Si la SPA usa las facets del BComponent para formatear valores (unidades, decimales, etc.) o si tiene su propio sistema. Los rangos de tiempo son hardcodeados en inglés — si hay plan de localización.

**(c) Prioridad ajustada**: **BAJA**. El módulo lexicon vacío confirma que no hay i18n activa. Las facets son necesarias para display correcto de unidades pero no son bloqueantes para la funcionalidad core.

---

## 50.6 TODOs honestos (qué NO pude verificar)

**TODO-1**: Si `socket.io-client 2.5.0` realmente conecta al `SocketServlet` Jetty. El mismatch Engine.IO vs WebSocket puro es un riesgo crítico que solo es verificable en runtime. Buscar si hay algún adapter Engine.IO en el -rt (no auditado completamente).

**TODO-2**: El shape exacto del JSON de `AlarmData.query()` — los campos del objeto `AlarmRecord` serializado. Solo el campo container `{records, count, offset, limit}` está confirmado; el objeto individual de alarm record no fue auditado.

**TODO-3**: El shape exacto de `HistoryData.fromComponent()` — el JSON de los datos de chart. El mock fixture `station/history-data.json` está vacío (`{}`). El source de `HistoryData.java` no fue auditado en detalle.

**TODO-4**: Si `window.injectBaja` y `window.injectConfig` están implementados en el bundle compilado (`rc/js/app.4509efb4.js`) pero no en el source de esta reconstrucción. El bundle pre-compilado puede tener una implementación real que la reconstrucción marca como "Phase 5+". El archivo `app-readable.js` en `rc/js/` podría confirmar esto — no fue auditado.

**TODO-5**: La implementación de `BReflowSyncService` y los comandos `sync-delta` / favoritos — no fueron auditados (solo referenciados).

**TODO-6**: El mecanismo de routing del `reflow:` ORD scheme en el context Niagara. `BReflowScheme.resolve()` itera todos los `BComponent` del `BComponentSpace` buscando un `BReflowService`. Esto puede ser lento en stations grandes con muchos componentes.

**TODO-7**: Headers de autenticación en las llamadas REST desde la SPA. Como la SPA corre en iframe mismo-origen, los headers HTTP estándar (incluido JSESSIONID cookie) se envían automáticamente. Pero si hay `withCredentials` desactivado en axios por algún motivo, las llamadas pueden fallar.

**TODO-8**: El archivo `app-readable.js` (versión deobfuscada del bundle) en `nmodsreflow-rt/src/rc/js/` — contiene el source del bundle original deobfuscado. Auditar este archivo en un bloque futuro puede revelar la implementación real de `injectBaja` y el wire protocol completo.

---

## 50.7 Próximos pasos recomendados

1. **[INMEDIATO] Auditar `app-readable.js`** en `nmodsreflow-rt/src/rc/js/app-readable.js` — este archivo es el bundle original deobfuscado y contiene la implementación real de `injectBaja`, el subscriber lifecycle, y todos los stubs marcados como "Phase 5+". Es la fuente de verdad de cómo funciona Reflow en producción.

2. **[ALTA] Verificar el protocolo WebSocket** — abrir el código de `nmodsreflow-rt` para `BReflowSyncService`, `ReflowOrdTreeFavoritesRead`, `ReflowOrdTreeFavoritesWrite` y verificar si hay algún adapter Engine.IO/socket.io en el servidor.

3. **[ALTA] Auditar `HistoryData.java` y `AlarmData.java`** — confirmar el JSON shape exacto de history chart data y alarm records para poder conectar los componentes Vue.

4. **[ALTA] Implementar `window.injectBaja`** en el SPA — es el bloqueante crítico para todo el API layer real. El patrón ya está documentado: recibe `(fromWorkbench, widget)`, conecta `$baja`, `$component`, `$bajaUsername`, y dispara `store.dispatch('load')`.

5. **[MEDIA] Documentar el `BReflowScheme`** — el ORD custom `reflow:` que permite navegar a Reflow desde cualquier ORD Niagara. Entender el routing completo `station:|slot:|reflow:/Equipment/building-1/floor-1`.

6. **[MEDIA] Resolver GAP-3 del multi-usuario** — los headers `Client-Id` y `Client-Username` en los requests REST. Confirmar el flujo de config control (quién tiene el `configControl=true` en el WebSocket).

7. **[BAJA] Auditar la CSP** — el `crossOriginHosts` configurable sin validación es un vector potencial. Documentar la configuración correcta para deployments con servidores externos (mapbox, weather).

---

## 50.8 Corrigendum 2026-05-06 (re-audit Tier 5)

**Origen**: re-audit empírico del Bloque 50 con metodología multiline-aware (engram topic_key `niagara-mental-model/bloque50-reaudit-2026-05-06`). El codebase Reflow-Clean-177 drifteó entre 2026-05-04 (fecha original del bloque) y 2026-05-06. Los puntos abajo son CORRECCIONES — el resto del bloque sigue vigente.

### 50.8.1 AP-10 — OUTDATED-FIXED + EXTENDED

**Status original**: "Backup operations via GET (no POST). Riesgo bajo en producción Niagara."

**Status actual** (verificado 2026-05-06):
- Server: `BaseServlet.java:235-310` — backups POST-only con `CsrfGuard.validate(req)` en línea 236. GET retorna HTTP 405 con header `Allow: POST` (líneas 138-147). Comentario inline cita "harden-backup-csrf, AP-10".
- Frontend: `plugins/http.js` — shared axios instance con CSRF interceptor (request adjuncta `x-niagara-csrfToken`, response retry-once on 403 csrf body). Usado por `rest.js:180,194,205,217,230` para los 5 endpoints de backup.

**Cross-bloque**: el patrón completo CSRF cross-frame está documentado en Bloque 52.

### 50.8.2 AP-3 — OUTDATED-DRIFTED (ya NO es 100% stubs)

**Status original**: "Stubs en el 100% del API layer".

**Status actual** (recuento empírico 2026-05-06):

| Archivo | Promise.resolve stubs | axios real | % drifted a real |
|---------|----------------------|------------|------------------|
| `src/api/rest.js` | 22 | 5 (backup endpoints) | ~18% |
| `src/api/box.js` | 26 | 0 | 0% |
| `src/api/bajascript.js` | 6 | 0 | 0% |
| `src/api/websocket.js` | 11 | 0 | 0% |
| `src/api/external.js` | 5 | 0 | 0% |
| **Total** | **70** | **5** | **~7%** |

El API sigue mayoritariamente stub (~93%), pero los 5 endpoints de backup post-AP-10 son producción real. La afirmación original "100%" es ahora **PARTIAL**.

### 50.8.3 AP-2 — CONFIRMED (sigue siendo gap real)

**Verificación 2026-05-06**: 0 declaraciones de `window.injectBaja`, `window.injectConfig`, `window.destroyApp` en `reflow-frontend/src/`. Solo 3 referencias en COMMENTS:
- `src/plugins/workbench.js:4` — comment de doc
- `src/plugins/baja.js:3` — comment de doc
- `src/lib/bajaHeartbeat.js:89` — Phase D wiring esperado

`bajaHeartbeat.js` infraestructura (`start/stop/add`) existe pero "Phase D" todavía no wired. AP-2 sigue siendo bug bloqueante para producción.

### 50.8.4 Hallazgo nuevo — CSRF infrastructure frontend completa

**Fuente**: no documentada en Bloque 50 original (no existía 2026-05-04).

**Componentes** (verificados 2026-05-06):
- `src/plugins/http.js` — shared axios instance con request interceptor (attach `x-niagara-csrfToken` en non-GET) + response interceptor (refresh + retry-once on 403 csrf body). 51 líneas.
- `src/lib/csrf.js` — `getToken()` + `refresh()` (no auditado en detalle).
- Pairs con `CsrfGuard.validate()` server-side en `BaseServlet.java`.

**Cobertura cross-bloque**: el patrón canónico CSRF en SPA-in-iframe está en Bloque 52 (cross-frame token injection). Este Bloque 50 corrigendum solo registra que la infrastructure HOY existe — los detalles arquitecturales viven en 52.

### 50.8.5 Síntesis veredictos por antipattern

| AP | Bloque 50 original | Re-audit 2026-05-06 |
|----|---------------------|---------------------|
| AP-1 (socket.io vs Jetty puro) | Riesgo Alto | CONFIRMED + NEEDS_RUNTIME |
| AP-2 (injectBaja/destroyApp) | Riesgo Crítico | CONFIRMED (sigue) |
| AP-3 (stubs 100%) | Riesgo Bloqueante | OUTDATED-DRIFTED (~93% stubs, no 100%) |
| AP-4 (FileResponse:42-43 doble close) | Riesgo Bajo | CONFIRMED |
| AP-5 (500 → HTML) | Riesgo Medio | CONFIRMED + matiz (1/8 path retorna JSON, mezcla) |
| AP-6 (sin Cache-Control) | Riesgo Bajo | CONFIRMED |
| AP-7 (CSP sin validación) | Riesgo Medio | PARTIAL (atenuado por `default-src 'self'`) |
| AP-8 (unsubscribe no-op) | Riesgo Medio | CONFIRMED |
| AP-9 (canAcknowledgeAlarms BBoolean) | Riesgo Medio | CONFIRMED |
| AP-10 (backups GET) | Riesgo Bajo | **OUTDATED-FIXED + EXTENDED** (POST + CSRF cliente+servidor) |
| AP-11 (alarm: hardcoded) | Riesgo Bajo | CONFIRMED |
| AP-12 (window.top.niagara.box sin fallback) | Riesgo Alto | CONFIRMED + ejemplo concreto en `OrdEmbed.vue:191` |

### 50.8.6 Lección metodológica del re-audit

**Sub-agents pueden replicar el blind spot del audit original**. El sub-agent inicial reportó "AP-3 CONFIRMED 100% stubs" sin contar las 5 `axios.post()` reales en `rest.js:180,194,205,217,230` — leyó el comentario header del archivo "Stubs returning Promise.resolve()" y se quedó ahí. Verificación independiente del orchestrator con `grep -c "Promise\.resolve"` y `grep -nE "axios\.(get|post)"` detectó la drift.

**Refuerzo de la lección Bloque 51 #11**: incluso un re-audit explícitamente diseñado para detectar bugs missed por el audit anterior puede tener su propio blind spot. La triangulación (orchestrator + sub-agent independientes con scope similar) es defensa contra esto.

### 50.8.7 TODOs honestos del Bloque 50 — status actual

- TODO-1 (socket.io engine.IO adapter en -rt) → NEEDS_RUNTIME, sigue
- TODO-2 (AlarmRecord shape) → no auditado en re-audit
- TODO-3 (HistoryData shape) → no auditado en re-audit
- TODO-4 (`app-readable.js` deobfuscado) → **archivo confirmado existe** (`nmodsreflow-rt/src/rc/js/app-readable.js`, 5.8 MB) — pendiente auditar en bloque futuro
- TODO-5 (BReflowSyncService) → ya cubierto parcialmente por engram #941 finding #12 (race condition AP-17)
- TODO-6 (BReflowScheme.resolve performance) → no auditado
- TODO-7 (auth headers en REST) → resuelto cualitativamente: same-origin + CSRF interceptor
- TODO-8 (auditar `app-readable.js`) → mismo que TODO-4

---

*Archivo producido: `/home/cristian/niagara-research/niagara-mental-model-bloque50.md`*
*Fuentes principales auditadas*:
- `reflow-frontend/package.json`, `vite.config.js`, `index.html`, `src/main.js`, `src/App.vue`
- `reflow-frontend/src/api/{index,rest,websocket,box,bajascript,external}.js`
- `reflow-frontend/src/plugins/{baja,niagara,http,cookies,workbench,gbo,reflowLink,ord,configMode,colorUtils,utils,timePlugin}.js`
- `reflow-frontend/src/lib/{ord,eventBus,configSerializer,deepMerge,configMigration,alarmCache}.js`
- `reflow-frontend/src/store/index.js` + 29 módulos Vuex
- `nmodsreflow-ux/src/com/niagaramods/nmodsreflow/ux/{BReflow,BReflowConfig,BReflowRedirect}.java`
- `nmodsreflow-ux/src/niagara/{reflow,reflow_config,reflow_redirect}.js`
- `nmodsreflow-ux/src/niagara/lib/{loader,resolver,hyperlink}.js`
- `nmodsreflow-ux/{module-include.xml,module-permissions.xml,module.palette,module.lexicon,nmodsreflow-ux.gradle.kts}`
- `nmodsreflow-rt/src/WEB-INF/web.xml`
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/{BReflowService,BReflowScheme}.java`
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/{BaseServlet,responses/*}.java`
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/sockets/{SocketServlet,BReflowWebSocketAcceptor,BReflowChannelService}.java`
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowAlarmCommands.java` (parcial)
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowNavCommands.java` (parcial)
- `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/sync/ConfigIO.java` (parcial)
- `nmodsreflow-rt/src/rc/` — estructura de assets estáticos empaquetados
