# Block 140 — nmodsreflow.77 (`-rt`): canal WebSocket (acceptor, sesiones, pub/sub de canales, dispatch de comandos)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), paquete `http/sockets/` del runtime `-rt`**: cómo
> Reflow abre y mantiene el canal WebSocket, cómo autentica/identifica cada socket contra la sesión
> Niagara, cómo modela canales pub/sub (`reflow`, `reflow-config`), y cómo despacha comandos entrantes
> a los `IReflowCommand` registrados. Cubre `SocketServlet`, `BReflowWebSocketAcceptor` (+ inner
> `ReflowWebSocket` / `WebSocketInfo`), `BReflowChannelService` (+ inner `ReflowChannel`),
> `IReflowCommand`, `AsyncReflowCommand`, `ReflowWsHttpSessionListener`, y el montaje real vía
> `WEB-INF/web.xml`. NO cubre la implementación de cada comando concreto (eso es R10) ni el subsistema
> sync/config-control salvo su gancho en el acceptor (R7).
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Cierra el gap R2 y **resuelve el "servlet mount GAP"
> que B138 §138.4 dejó abierto**. Corpus language: Spanish (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow`
> `SOCK/` = `RT/http/sockets`. Config: `RT/../WEB-INF/web.xml`, `RT/../META-INF/module.xml`.
>
> Método: decompile Vineflower del JAR embarcado + lectura directa + grep de callers. Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[CERT-doc]` doc oficial · `[INFER]` deducción.
> Nota de decompilado: Vineflower dejó nombres ofuscados `method_NNN` en las llamadas Jackson
> (`method_311`=`put(String)`, `method_312`=`put(boolean)`, `method_297`=`set`/`putPOJO`,
> `method_295`=`has`, `method_291`=`get`, `method_316`=`add`); se citan tal cual aparecen en el `.java`.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 138] (service central + espina HTTP; este bloque
> cierra su GAP de montaje), [Block 139] (el `licenseCommand` es un `IReflowCommand` registrado aquí),
> [Block 9] (stack de servlets/web-rt de Niagara), [Block 75]/[Block 113] (skipModuleValidation /
> code-signing — relevante porque el dispatch corre bajo `AccessController.doPrivileged`).

---

## 140.1 — Montaje real del canal (cierra el GAP de B138 §138.4) `[CERT]`

B138 §138.4 marcó como GAP *cómo* los servlets llegan a Jetty. La respuesta está en el
**`WEB-INF/web.xml`** embarcado en el JAR `-rt` `[CERT]` `RT/../WEB-INF/web.xml:1-24`:

- `nmodsWsServlet` → clase `SocketServlet`, `url-pattern` **`/ws`** `[CERT]` `web.xml:4-11`.
- `nmodsBaseServlet` → clase `BaseServlet`, `url-pattern` **`/*`** `[CERT]` `web.xml:13-20` (el REST/SPA de B138).
- Un `<listener>` **`ReflowWsHttpSessionListener`** `[CERT]` `web.xml:22-24` (§140.6).

Es un `web.xml` estándar de módulo Niagara (webProfile), no registro programático: por eso B138 no lo
encontró en el código del service. El path completo del upgrade WebSocket, combinando la base de servlet
del módulo (`/module/nmodsreflow/…` en el stack `web-rt`, [Block 9]) con `/ws`, es
**`/module/nmodsreflow/ws`** `[INFER]` (base no re-verificada en vivo; el `/ws` sí es `[CERT]`).

## 140.2 — `SocketServlet`: policy Jetty y creación de sockets `[CERT]`

`SocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet` `[CERT]`
`SOCK/SocketServlet.java:12`. En `configure(factory)` fija la `WebSocketPolicy` `[CERT]`
`SOCK/SocketServlet.java:20-28`:

| Parámetro | Valor | Cita |
|---|---|---|
| `idleTimeout` | `60000L` ms (60 s) | `SocketServlet.java:14,22` |
| `maxTextMessageBufferSize` | `65536` | `:15,23` |
| `maxTextMessageSize` | `262144` (256 KiB) | `:16,24` |
| `maxBinaryMessageBufferSize` | `65536` | `:17,25` |
| `maxBinaryMessageSize` | `131072` (128 KiB) | `:18,26` |

El `WebSocketCreator` es el inner `ReflowWebSocketCreator`: cada upgrade instancia
`new BReflowWebSocketAcceptor.ReflowWebSocket(id)` con un `id` incremental de un `AtomicLong` estático
(`createID()`) `[CERT]` `SocketServlet.java:30-49`. **El `clientId` es un contador global de proceso, no un
UUID por sesión** `[INFER]` (basado en `AtomicLong idCounter` compartido `:31,37`) — se reinicia a 0 al
reiniciar la station.

## 140.3 — `BReflowWebSocketAcceptor`: registro de sockets y de comandos `[CERT]`

`BReflowWebSocketAcceptor extends BAbstractService` `[CERT]` `SOCK/BReflowWebSocketAcceptor.java:49`;
`isParentLegal` exige que el padre sea `BReflowService` `[CERT]` `:69-71`. Es el sub-servicio frozen
`webSocketAcceptor` de B138. Mantiene dos colecciones:

- `Array<WebSocketInfo> webSockets` — **registro de conexiones vivas** `[CERT]` `:51`; acceso siempre bajo
  `synchronized(this.webSockets)` `[CERT]` `:75,96,180,218`. `stopped()` cierra todas y limpia `[CERT]` `:73-83`.
- `ArrayList<IReflowCommand> commands` — **tabla de comandos registrados** `[CERT]` `:53`.

`addCommand(cmd)` deduplica por la tupla **(`getName()`, `getOwnerId()`)**: no agrega si ya existe uno con
ese par `[CERT]` `:124-137`. `removeCommand` usa el mismo predicado `[CERT]` `:139-145`. Hay un spy page
(`method_367`, ex `spy(SpyWriter)`) que tabula las conexiones abiertas (URI, encrypted, accept/open/last
message) `[CERT]` `:93-122` — instrumentación de diagnóstico vía la Spy de Niagara.

## 140.4 — `ReflowWebSocket`: identidad, ciclo de vida y parser de mensajes `[CERT]`

El endpoint Jetty es el inner estático `@WebSocket ReflowWebSocket` `[CERT]` `:147-148`, que además
implementa `ReflowWsHttpSessionListener.IHttpSessionDestroyListener` (§140.6).

**`@OnWebSocketConnect onConnect`** `[CERT]` `:165-193`:
1. Resuelve el service central por `Sys.getService(BReflowService.TYPE)` `[CERT]` `:168`.
2. Extrae el atributo **`"niagara.context"`** del `HttpServletRequest` del upgrade y construye un
   `BasicContext acceptCx` sobre él `[CERT]` `:174-178`. **Esto ata la identidad del socket a la sesión
   Niagara autenticada**: toda la lógica posterior lee el usuario vía `acceptCx.getUser().getUsername()`
   `[CERT]` `:176,188,200,361,375`. `[INFER]`: la autenticación/autorización NO la hace este módulo — la
   heredó el request HTTP que Jetty ya autenticó (el `web-rt` de Niagara), y el WS solo la reutiliza.
3. Registra un `WebSocketInfo` en `webSockets`, se suscribe al listener de destrucción de sesión, y envía
   un mensaje inicial `client-info` `[CERT]` `:179-192`.

**`@OnWebSocketMessage onMessage(session, str)`** `[CERT]` `:223-368` — parser central:
- Parsea el texto con Jackson `ObjectMapper.readTree` `[CERT]` `:228-229`. **Si falta el campo `command`,
  ignora el mensaje** (`return`) `[CERT]` `:230-233`.
- Extrae `ticket`, `command`, `action`, `route` `[CERT]` `:235-251`; actualiza telemetría de la conexión
  (`lastAction`, `lastActionTime`, `lastRoute`, `configRoute`) y dispara `channelStatus("reflow")` /
  `channelStatus("reflow-config")` para reflejar actividad a los suscriptores `[CERT]` `:239-258`.
- **Dispatch de comandos**: recorre `commands`; para el que matchee `getName().equals(command)` ejecuta
  `cmd.method_0(this, obj, ticket)` **dentro de `AccessController.doPrivileged(PrivilegedExceptionAction)`**
  `[CERT]` `:260-269`. Ver nota de seguridad en §140.7.
- **Comandos built-in** manejados inline (no vía tabla): `route`/`config-route` (responde `success`+ticket)
  `[CERT]` `:271-284`; `client-info` (reenvía `makeClientInfo`) `[CERT]` `:286-295`; y las operaciones de
  canal `join`/`leave`/`who`/`broadcast` cuando el mensaje trae campo `channel` `[CERT]` `:297-364`.

Salida: `send(str)` usa `session.getRemote().sendStringByFuture(str)` + `flush()` `[CERT]` `:401-408`.
Existe además `sendSync(ReflowSyncResponse)` que **stremea** la respuesta grande por `PipedInputStream`/
`PipedOutputStream` con dos hilos (uno serializa el POJO con Jackson, otro lee y hace
`sendPartialString(..., false)` en chunks de 1024 B, cerrando con `sendPartialString(" ", true)`) `[CERT]`
`:410-456` — es el mecanismo para no materializar el dump de sync completo en memoria. Cruza a R7 (sync).

## 140.5 — `BReflowChannelService` + `ReflowChannel`: pub/sub `[CERT]`

`BReflowChannelService extends BAbstractService` `[CERT]` `SOCK/BReflowChannelService.java:19`, padre legal
`BReflowService` `[CERT]` `:38-40`. Modelo: `HashMap<String, ReflowChannel> channels` `[CERT]` `:22`. Cada
`ReflowChannel` guarda un `ArrayList<ReflowWebSocket> _subs` `[CERT]` `:160-167`.

Operaciones del service (todas idempotentes sobre el map):
- `join(channel, socket, ticket)` — crea el canal on-demand, suscribe, y responde `channel-status` con el
  roster `who` `[CERT]` `:91-114`. A nivel `ReflowChannel.join`, además **broadcastea** a los demás un evento
  `type:"join"` con `clientId`/`username`/`who` `[CERT]` `:169-180`.
- `leave` / `leaveAll(socket)` — desuscribe de uno o de todos los canales `[CERT]` `:116-144`,`182-191`.
- `method_368` (ex `who`) — responde solo al socket que preguntó `[CERT]` `:51-71`; `channelStatus` — hace
  broadcast del roster a todo el canal `[CERT]` `:73-89`.
- `broadcast(channel, msg)` — sobrecargado para `com.tridium.json.JSONObject` y para Jackson `ObjectNode`
  `[CERT]` `:146-158`.

El roster `who` (`method_364`) serializa por socket: `name`(username), `clientId`, `lastActionTime`,
`lastAction`, `lastMessageTime`, `configRoute`, `lastRoute`, `connected`(acceptTime) `[CERT]` `:194-212` —
es la fuente del "presence"/multiusuario que el frontend Vue muestra.

**Canales conocidos**: `"reflow"` (actividad general) y `"reflow-config"` (edición de config, con control
exclusivo — §140.8) `[CERT]` `:198-199,254-257,309`. Cruza a R7/R9.

**[INFER] Bug de broadcast con lista `except`**: en `ReflowChannel.broadcast(data, except)` el guard usa
`return` (no `continue`) cuando `except.contains(sock)` `[CERT]` `:223-225,243-245`. Efecto: al topar con
el primer socket excluido, **aborta el broadcast a TODOS los suscriptores restantes** en vez de solo
saltarlo. Como `except` típicamente contiene al propio emisor, el orden en `_subs` determina si los demás
reciben el evento `join`/`leave`. Es un defecto real de fan-out, no cosmético. `[INFER]` (análisis de flujo
sobre líneas `[CERT]`).

## 140.6 — `ReflowWsHttpSessionListener`: WS atado a la sesión HTTP `[CERT]`

`ReflowWsHttpSessionListener implements javax.servlet.http.HttpSessionListener` `[CERT]`
`SOCK/ReflowWsHttpSessionListener.java:8`. Mantiene un `HashSet<IHttpSessionDestroyListener>` estático
`[CERT]` `:9`; en `sessionDestroyed` notifica a todos `[CERT]` `:14-23`. Cada `ReflowWebSocket` se
registra/desregistra (`addDestroyListener`/`removeDestroyListener`) en connect/close/error `[CERT]`
`BReflowWebSocketAcceptor.java:185,216,385`. En `onHttpSessionDestroyed`, si la sesión destruida coincide
con la del socket, hace `leaveAll` y **cierra el socket** `[CERT]` `:388-395`. **Consecuencia**: expirar o
cerrar la sesión Niagara (logout/timeout) mata el WS; el canal no sobrevive a la auth HTTP `[INFER]`.

## 140.7 — Dispatch privilegiado (nota de seguridad) `[INFER]`

Cada comando de la tabla corre bajo `AccessController.doPrivileged` `[CERT]`
`BReflowWebSocketAcceptor.java:262-267`. `[INFER]`: esto ejecuta la lógica del comando con el
`ProtectionDomain` del módulo Reflow (firmado), no con el del caller, de modo que las operaciones de FS/
station del comando no fallen por el `SecurityManager` de Niagara. La autorización efectiva del *usuario*
recae por completo en (a) que Jetty haya autenticado el request del upgrade (§140.4) y (b) en la lógica
interna de cada comando — este dispatcher **no** re-chequea permisos por comando `[INFER]`. Cruza a
[Block 75]/[Block 113] (code-signing / `skipModuleValidation`): el `doPrivileged` solo tiene fuerza si el
JAR pasa la validación de firma del framework; con `skipModuleValidation` un JAR forjado obtendría el mismo
privilegio. Cruza a [Block 139]: el `licenseCommand` (§140.9) se despacha por esta misma vía.

## 140.8 — Config-control single-user (gancho a R7) `[CERT]`

Cuando `!service.getMultiUserConfig()`, el acceptor arbitra **control exclusivo** del canal
`reflow-config`: al `join`, si es el único socket concede control (`grantConfigControl`), y si hay varios
verifica que exista un controlador `[CERT]` `:307-327`; al `leave`/`onClose`, transfiere el control a otro
socket sin control (`clearConfigControlRequests` + `grantConfigControl`) `[CERT]` `:196-214,330-350`. La
lógica concreta vive en `BReflowSyncService` (R7). El flag por socket es `configControl` `[CERT]` `:159`.

## 140.9 — Quién registra comandos en el acceptor `[CERT]`

Registro observado vía grep de `addCommand`:

| Comando (`getName`/`getOwnerId`) | Registrado por | Cita |
|---|---|---|
| `licenseCommand` / `reflowService` (`ServiceCommand`) | `BReflowService.start` (dos ramas por versión N4) | `BReflowService.java:502,515,793-799` |
| `ConfigSyncCommand`, `RequestControlCommand` | `BReflowSyncService` | `sync/BReflowSyncService.java:129,132` |
| `ReflowOrdTreeFavoritesRead`, `…Write` | `BReflowSyncService` | `sync/BReflowSyncService.java:135-136` |

`ServiceCommand.method_0` responde `isLicensed` = `service.getLicenseStatus()` `[CERT]`
`BReflowService.java:804-818` — es la superficie WebSocket del subsistema de licensing de [Block 139]. Los
8 `BReflow*Commands` del paquete `commands/` (R10) son `BComponent` agentes montados `on ReflowService`
(`module.xml:22-34`) `[CERT]`, superficie distinta de estos `IReflowCommand` planos.

## 140.10 — `IReflowCommand` / `AsyncReflowCommand` `[CERT]`

`interface IReflowCommand { String getName(); String getOwnerId(); void method_0(ReflowWebSocket, JsonNode,
String ticket); }` `[CERT]` `SOCK/IReflowCommand.java:5-12` — contrato mínimo de un comando WS.

`abstract AsyncReflowCommand implements IReflowCommand` `[CERT]` `SOCK/AsyncReflowCommand.java:5`: su
`method_0` **lanza un `Thread` nuevo** (`"ReflowCommandTask"`) que invoca el `task()` sobreescribible
`[CERT]` `:9-33`. Es decir, un comando que herede de `AsyncReflowCommand` corre **fuera del hilo de Jetty**
(no bloquea el `onMessage`), pero **fuera** del `doPrivileged` de §140.7 — el `AccessController.doPrivileged`
envuelve el `method_0` que solo *arranca* el hilo, no el `task()` que corre después `[INFER]` `:262-267` +
`AsyncReflowCommand.java:11-15`. Detalle relevante para R10.

---

## Connections

- **[Block 138]** — este bloque **cierra el "servlet mount GAP"** de §138.4 (era `web.xml`, `/ws`) y detalla
  el sub-servicio `webSocketAcceptor` que 138 solo enumeró.
- **[Block 139]** — el `licenseCommand`/`ServiceCommand` es un `IReflowCommand` registrado aquí; su
  respuesta `isLicensed` viaja por este canal.
- **[Block 9]** — stack `web-rt`/servlets Niagara: base de path del módulo y autenticación del upgrade HTTP.
- **[Block 75]/[Block 113]** — `skipModuleValidation`/code-signing: precondición para que el
  `AccessController.doPrivileged` del dispatch tenga fuerza real (§140.7).
- Gaps que este bloque toca sin cerrar: **R7** (sync/config-control — §140.8), **R9** (canal `reflow-config`
  / config.json), **R10** (los `IReflowCommand` concretos), **R3** (parcialmente resuelto: el montaje ya no
  es GAP, queda solo el detalle de cómo Niagara `web-rt` compone la base `/module/<name>/`).

---

## Self-verify (METHODOLOGY §11)

- **Artefactos tocados**: escrito `niagara-mental-model-bloque140.md`. Leídos (read-only): `SOCK/{SocketServlet,
  BReflowWebSocketAcceptor, BReflowChannelService, IReflowCommand, AsyncReflowCommand, ReflowWsHttpSessionListener}.java`,
  `RT/BReflowService.java`, `WEB-INF/web.xml`, `META-INF/module.xml`.
- **Markers**: `[CERT]` ~58 (todos con `file:line` sobre el JAR embarcado build .75, grep-confirmados en
  §140.9) · `[CERT-doc]` 0 · `[INFER]` 8 (clientId global, base de path, herencia de auth, bug de broadcast
  `except`, muerte del WS con la sesión, nota de seguridad `doPrivileged`, `task()` fuera del privileged,
  precondición code-signing). Ratio `[INFER]/[CERT]` ≈ 0.14 — los `[INFER]` son análisis de comportamiento/
  seguridad derivado, no huecos de evidencia; cada uno ancla en líneas `[CERT]`.
- **Ground-truth**: `module.xml` confirma `vendorVersion="1.7.7.75"`, `runtimeProfile="rt"`, `moduleName="nmodsreflow"`.
- **Hallazgo colateral**: cerrado el GAP de montaje de B138 (web.xml `/ws`); R3 baja de "pending" a casi-cerrado.
