# Bloque 59 — WebSocket layer audit: SocketServlet + BReflowChannelService + BReflowWebSocketAcceptor + frontend Vue components + 14 nuevos antipatterns AP-42..55 + 5 reglas template MX60 (13-17)

**Fecha**: 2026-05-08
**Método**: Audit completo del directorio `/http/sockets/` (6 archivos Java, 926L total) + 4 Vue components frontend (`reflow-frontend/src/components/websocket/`) + cross-reference con sync layer (ConfigSyncCommand, RequestControlCommand) + análisis threading model + análisis wire format JSON cliente↔server.

**Fuentes primarias**:
- `BReflowWebSocketAcceptor.java` (505 líneas) — pieza central, acceptor + ReflowWebSocket inner class
- `BReflowChannelService.java` (281 líneas) — pub-sub channel manager
- `SocketServlet.java` (54 líneas) — Jetty WebSocketServlet entry point
- `ReflowWsHttpSessionListener.java` (40 líneas) — HTTP session lifecycle binding
- `AsyncReflowCommand.java` (34 líneas) — async command base class
- `IReflowCommand.java` (12 líneas) — command interface contract
- `SocketAuth.vue` (3795 bytes) — control request dialog
- `SocketConnect.vue` (3089 bytes) — connection lifecycle dialog
- `SocketRequest.vue` (5813 bytes) — control lock + request button
- `SocketResponseError.vue` (1009 bytes) — error envelope display
- Bloques 53-58 (HTTP layer + AP-1..41 + 12 reglas template + 101 MX60 implications)

**Versión analizada**: Reflow-Clean-177 (sin ID `harden-backup-csrf` aplicado a sockets — sockets no tienen fix similar, gap a verificar).

---

## 59.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Cierre de **trinity HTTP path 3 de 3** del módulo Reflow: la capa **WebSocket** (real-time bidireccional). Las otras dos paths fueron auditadas en bloques previos:
1. **BajaScript canonical** (Bloques 50-52): `injectBaja` + `subscriber.lease` + WebSocketConnection.js bundled
2. **`yi`/serverSideCall RPC** (Bloque 53): trinity wrapper sobre `$component.serverSideCall`, 7 typeSpecs Reflow
3. **WebSocket custom** (este bloque): `/nmodsreflow/ws` SocketServlet → ReflowWebSocket → IReflowCommand handlers

Este es el path para **multiuser awareness** (quién está conectado, en qué ruta), **config control exclusivity** (locking + request-grant flow), y **server push** (sync deltas, favorites broadcast, channel events).

### Qué corrige / valida

| Bloque | Sección | Hallazgo previo | Validación / corrección |
|--------|---------|-----------------|-------------------------|
| 53.5.16.5 (Reglas 11+12 template MX60) | Context propagation `cx` end-to-end OBLIGATORIA | AP-27 sistémico ~50 sites | ✅ **AP-49 NUEVO CRITICAL** confirma deuda cruzada en path WebSocket: `AccessController.doPrivileged()` envuelve `cmd.run()` PERO `AsyncReflowCommand.run()` spawnea `new Thread().start()` que NO hereda `acceptCx` → RBAC bypass en `task()` si usa `Sys.getContext()`. Regla 11 NO está observada en async path. |
| 50/52 CsrfGuard cliente Plan E | CSRF token via `window.top.document` | ✅ Plan E correcto | ⚠️ **AP-43 NUEVO CRITICAL**: WebSocket upgrade request NO valida Origin/Referer. CsrfGuard SÓLO se aplica a doPost en BaseServlet. Cross-origin malicious page puede abrir socket a station víctima. Plan E del Bloque 52 NO cubre WebSocket. |
| 58.1.3 (BaseServlet POST + CsrfGuard) | 9 POSTs todos con CsrfGuard | ✅ HTTP REST cubierto | ❌ **GAP CRÍTICO**: WebSocket es 4to vector que NO pasa por CsrfGuard. Si BaseServlet es 100% guarded, sockets son 0% guarded. Asimetría peligrosa. |
| 57 (AP-33 file disclosure) | Filesystem access sin RBAC | HIGH | ⚠️ **Hipótesis AP-33 cross-WebSocket**: `ReflowOrdTreeFavoritesRead/Write` lee `BFileSystem.getStationHome().FilePath(user + ".json")` — path inyectable si username contiene `..`. Necesita verificación BUserService canonical. Predicción Sección 11. |

### Pregunta unificadora

> ¿Cuál es el "shape" final del path WebSocket Reflow y qué patterns hereda MX60 vs reescribe vs descarta?

**Respuesta corta**:
- **Hereda**: BasicContext wrapping con facets, CopyOnWriteArrayList pub-sub, Jetty annotations, IReflowCommand polymorphism, WebSocketInfo metadata tracking.
- **Reescribe**: AsyncReflowCommand thread spawning (uncontrolled `new Thread()` → BoundedThreadPoolExecutor), context propagation (acceptCx → ThreadLocal o param explícito), error responses (silent → estructurados), CSRF/Origin validation (ausente → obligatorio).
- **Descarta**: `sendSync()` PipedStream pattern (deadlock-prone, unusual). MX60 usa `session.getRemote().sendStringByFuture()` directo.
- **Decisiones MX60 #3+#4**: Regla 13 (async pool + cx propagation), Regla 14 (CORS + connection limits), Regla 15 (error envelope estandarizado), Regla 16 (rate limiting), Regla 17 (heartbeat).

---

## 59.1 Arquitectura WebSocket layer — diagrama + flow end-to-end

### 59.1.1 Diagrama completo

```
┌─── VUE FRONTEND ──────────────────────────────────────────────┐
│                                                                 │
│  components/websocket/                                          │
│  ├── SocketConnect.vue      (dialog "Connecting to Reflow")    │
│  ├── SocketAuth.vue          (incoming control request)        │
│  ├── SocketRequest.vue       (control locked, request button)  │
│  └── SocketResponseError.vue (error envelope display)          │
│                                                                 │
│  Vuex store (inferido):                                        │
│    socketStatus: 'disconnected' | 'connecting' | 'connected'  │
│    hasControl: bool                                             │
│    activeControlRequest: { user, name, clientId }              │
│    controller: { username, lastActivity, ... }                 │
│    socketChannels: { reflow: [], reflow-config: [] }          │
└──────────────────────────────────────────────────────────────────┘
                             ↕ ws[s]://host/nmodsreflow/ws
┌─── JAVA BACKEND (Jetty WebSocket) ────────────────────────────┐
│                                                                 │
│  SocketServlet extends WebSocketServlet (entry point)          │
│  ├── configure(WebSocketServletFactory factory):               │
│  │   • idleTimeout = 60000ms (1 minuto)                       │
│  │   • maxTextMessageSize = 262144 (256 KB)                   │
│  │   • maxBinaryMessageSize = 131072 (128 KB)                 │
│  │   • factory.setCreator(ReflowWebSocketCreator)             │
│  │                                                             │
│  └── ReflowWebSocketCreator                                    │
│      └── createWebSocket(req, resp) → new ReflowWebSocket(id) │
│          (AtomicLong ID counter per connection)                │
│                                                                 │
│  ┌──── ReflowWebSocket (per-connection inner class) ──────┐  │
│  │                                                          │  │
│  │  Fields: httpSession, Session, service,                  │  │
│  │          context (cx, acceptCx), clientId, info          │  │
│  │                                                          │  │
│  │  @OnWebSocketConnect (Session sess):                    │  │
│  │  │  ├─ HttpSession from sess.getUpgradeRequest()        │  │
│  │  │  ├─ cx = (Context) attribute "niagara.context"      │  │
│  │  │  ├─ acceptCx = new BasicContext(cx, facets)          │  │
│  │  │  │   facets: remoteHost, remotePort                  │  │
│  │  │  ├─ ReflowWsHttpSessionListener.add(this)            │  │
│  │  │  ├─ webSockets.add(this) (synchronized Array)        │  │
│  │  │  └─ send greeting "client-info" JSON                 │  │
│  │  │                                                       │  │
│  │  @OnWebSocketMessage (String json):                      │  │
│  │  │  ├─ ObjectMapper.readTree(json) → JsonNode obj      │  │
│  │  │  ├─ String command = obj.get("command").asText()     │  │
│  │  │  ├─ String ticket = obj.get("ticket")?.asText()      │  │
│  │  │  ├─ Switch on builtin commands:                      │  │
│  │  │  │   ├─ "join" → channel.join(this) + broadcast     │  │
│  │  │  │   ├─ "leave" → channel.leave(this) + broadcast   │  │
│  │  │  │   ├─ "who" → reply with channel.who()            │  │
│  │  │  │   ├─ "broadcast" → channel.broadcast(payload)    │  │
│  │  │  │   ├─ "route" → info.route = obj.get("route")     │  │
│  │  │  │   ├─ "config-route" → info.configRoute = ...     │  │
│  │  │  │   ├─ "client-info" → reply with full info        │  │
│  │  │  │   └─ "ping" → noop (no action tracking)          │  │
│  │  │  │                                                   │  │
│  │  │  └─ For each registered IReflowCommand:              │  │
│  │  │      if cmd.getName().equals(command):                │  │
│  │  │        AccessController.doPrivileged(() -> {          │  │
│  │  │          cmd.run(this, obj, ticket);                  │  │
│  │  │          return null;                                 │  │
│  │  │        });                                            │  │
│  │  │                                                       │  │
│  │  @OnWebSocketClose (code, reason):                       │  │
│  │  │  ├─ ReflowWsHttpSessionListener.remove(this)         │  │
│  │  │  ├─ leaveAll() — exit all subscribed channels       │  │
│  │  │  └─ webSockets.remove(this)                          │  │
│  │  │                                                       │  │
│  │  @OnWebSocketError (Throwable t):                        │  │
│  │  │  └─ cleanup (deregister + log)                        │  │
│  │  │                                                       │  │
│  │  send(String): session.getRemote().sendStringByFuture()  │  │
│  │  sendSync(ReflowSyncResponse): PipedStream (2 threads)   │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  BReflowChannelService extends BAbstractService                │
│  ├── ConcurrentHashMap<String, ReflowChannel> channels         │
│  ├── ReflowChannel (inner class):                              │
│  │   ├─ CopyOnWriteArrayList<ReflowWebSocket> _subs           │
│  │   ├─ join(socket) → add + broadcast "join"                 │
│  │   ├─ leave(socket) → remove + broadcast "leave"            │
│  │   ├─ who() → ArrayNode [{user, clientId, lastAction, ...}] │
│  │   └─ broadcast(payload, except[]) → iterate _subs, send    │
│  │                                                             │
│  └── (channels: "reflow", "reflow-config")                     │
│                                                                 │
│  BReflowWebSocketAcceptor extends BAbstractService            │
│  ├── Array<ReflowWebSocket> webSockets (synchronized)          │
│  ├── ArrayList<IReflowCommand> commands (registered handlers) │
│  ├── addCommand(IReflowCommand) / removeCommand(...)           │
│  └── spy() — HTML table of open connections + metadata         │
│                                                                 │
│  Registered IReflowCommand impls (cross-module):               │
│  ├── ConfigSyncCommand (sync-delta) — async — BReflowSyncSvc  │
│  ├── RequestControlCommand (config-control) — async           │
│  ├── ReflowOrdTreeFavoritesRead (favorites-read) — async      │
│  └── ReflowOrdTreeFavoritesWrite (favorites-write) — async    │
│                                                                 │
└──────────────────────────────────────────────────────────────────┘
```

### 59.1.2 Wire format JSON

**Outgoing (cliente → server)**:
```json
{
  "command": "sync-delta|join|leave|config-control|favorites-read|...",
  "ticket": 123,
  "action": "ping|config|...",
  "channel": "reflow|reflow-config",
  "route": { "fullPath": "/Equipment", "path": "/Equipment" },
  "data": { /* command-specific payload */ }
}
```

**Incoming (server → cliente)** — NO estandarizado:
| Tipo | Schema |
|------|--------|
| Channel status (join/leave) | `{ type: "channel-status", ticket, command: "join", channel, success, who: [...] }` |
| Who reply | `{ type: "who", ticket, command: "who", channel, success, who: [...] }` |
| Client info | `{ type: "client-info", clientId, configControl, username, ticket }` |
| Sync delta | `{ type: "sync-delta", ticket, ord, patch, ... }` |
| Error | **NO ENVELOPE STANDARD** — silenciado en log, NO se manda al cliente (AP-51) |

### 59.1.3 Threading model

**Jetty workers** (default 8): onConnect/onMessage/onClose síncronos. Non-blocking design — Jetty pasa request, no espera.

**AsyncReflowCommand spawn** (AP-42 HIGH): cada mensaje async = `new Thread("ReflowCommandTask").start()`. SIN pool, SIN límite. Carga sostenida 1000 sockets × 10 msg/sec = 10K threads → OOM.

**`sendSync()` 2-thread pattern** (L441-490 BReflowWebSocketAcceptor): PipedInputStream + PipedOutputStream. `threadOut` escribe ReflowSyncResponse → buffer. `threadIn` lee → `session.getRemote().sendPartialString()`. `threadOut.join()` + `threadIn.join()` blocking. **Riesgo deadlock**: si buffer Piped lleno + threadIn slow → threadOut bloquea forever.

**Context propagation** (AP-49 CRITICAL — match AP-27 pattern):
- onConnect extrae `cx` de request attribute, guarda en `socket.acceptCx`.
- onMessage corre en Jetty worker → tiene acceso a `socket.acceptCx`.
- **PERO**: `AsyncReflowCommand.run()` hace `new Thread().start()` — el nuevo thread NO hereda contexto.
- TaskRunner.run() llama `task(socket, message, ticket)` — socket field tiene acceptCx, ok.
- **PERO**: si `task()` llama `Sys.getContext()` o `Sys.getUser()` (current thread context), recibe **system context** → permission check bypassed.
- Evidence: `ReflowOrdTreeFavoritesRead.java:32` usa `socket.acceptCx.getUser()` (correcto), pero NO hay garantía de que TODOS los handlers respeten esto.

---

## 59.2 Audit por archivo

### 59.2.1 IReflowCommand.java (12 líneas)

**Propósito**: Interface contract para comandos WebSocket.

**Estructura**:
```java
public interface IReflowCommand {
    String getName();
    String getOwnerId();
    void run(ReflowWebSocket socket, JsonNode message, String ticket);
}
```

**Patterns identificados (KEEP)**:
- Simple polymorphic interface — registración via ArrayList
- Ticket field para request-response correlation
- Owner ID para command namespacing (anti-collision)

**Antipatterns**: ninguno.

**Cross-references**: Implementado por `AsyncReflowCommand` + 4 commands concretos en sync module.

**MX60 implication**: **KEEP** — pattern exacto para MX60.

---

### 59.2.2 AsyncReflowCommand.java (34 líneas)

**Propósito**: Base class abstract para commands que ejecutan **async** (off Jetty worker).

**Estructura**:
```java
public abstract class AsyncReflowCommand implements IReflowCommand {
    public final void run(ReflowWebSocket socket, JsonNode message, String ticket) {
        new Thread(new TaskRunner(socket, message, ticket), "ReflowCommandTask").start();
    }
    protected abstract void task(ReflowWebSocket socket, JsonNode message, String ticket);

    private class TaskRunner implements Runnable {
        public void run() { task(socket, message, ticket); }
    }
}
```

**Patterns identificados (KEEP)**:
- Template method (run vs task)
- Thread naming "ReflowCommandTask" para debugging

**Antipatterns identificados**:

> **AP-42 NEW HIGH** — "Uncontrolled thread spawn in async commands"
>
> **Site**: AsyncReflowCommand.java:13
> **Descripción técnica**: SIN thread pool, SIN executor, SIN límite. Cada mensaje WebSocket async = `new Thread(...).start()`. Bajo carga sostenida 1000 sockets × 10 msg/sec = potencialmente 10K threads vivos en JVM.
> **Exploit scenario**: Cliente malicioso abre 100 sockets, manda 100 async commands rápidos. 100 threads spawneados simultáneos. Server responde lento → cliente repite → escala lineal hasta OOM.
> **Severity**: HIGH (DoS local + degradación de servicio)
> **Fix recomendado**: `BoundedThreadPoolExecutor(coreSize=N, maxSize=2N, queue=100)` — Niagara runtime ofrece BJobService o ForkJoinPool. Reglar 13 obligatoria.

**Cross-references**: 4 impls (ConfigSyncCommand, RequestControlCommand, ReflowOrdTreeFavoritesRead, ReflowOrdTreeFavoritesWrite).

**MX60 implication**: **IMPROVE** — reemplazar Thread.start() con executor. Regla 13 nueva.

---

### 59.2.3 SocketServlet.java (54 líneas)

**Propósito**: Jetty WebSocketServlet entry point. Upgrade HTTP → WS protocol, factory de creación.

**Estructura**:
```java
public class SocketServlet extends WebSocketServlet {
    public void configure(WebSocketServletFactory factory) {
        WebSocketPolicy p = factory.getPolicy();
        p.setIdleTimeout(60000);              // 1 minuto
        p.setMaxTextMessageSize(262144);      // 256 KB
        p.setMaxBinaryMessageSize(131072);    // 128 KB
        factory.setCreator(new ReflowWebSocketCreator());
    }
}
```

**Patterns identificados (KEEP)**:
- Policy config explícita y centralizada
- AtomicLong ID counter en ReflowWebSocketCreator → ID único por conexión

**Antipatterns identificados**:

> **AP-43 NEW CRITICAL** — "No CSRF/Origin validation on WebSocket upgrade"
>
> **Site**: SocketServlet.java:43 (createWebSocket factory call)
> **Descripción técnica**: HTTP upgrade request NO valida `Origin` ni `Referer`. Jetty NO enforce CORS por default. Cross-origin malicious page que abre `new WebSocket('wss://reflow-app/nmodsreflow/ws')` triggers upgrade exitoso si víctima logged-in (cookies se mandan).
> **Exploit scenario**: Atacante envía link a víctima logged-in en Reflow. Página atacante abre socket → ahora puede mandar commands en nombre del usuario. Cualquier comando favorites-write, config-control, broadcast queda accessible cross-origin.
> **Severity**: CRITICAL — CSWSH (Cross-Site WebSocket Hijacking) classic.
> **Fix recomendado**: validar `Origin` header en `ReflowWebSocketCreator.createWebSocket(req, resp)` contra whitelist; reject con HTTP 403 si mismatch. Regla 14 nueva.

> **AP-44 NEW MEDIUM** — "No max connections per IP"
>
> **Site**: SocketServlet.java:39-41 (factory configuration)
> **Descripción técnica**: SIN tracking de connections per source IP. Single IP puede agotar server sockets (default JVM 1024 FDs).
> **Severity**: MEDIUM (DoS vector)
> **Fix recomendado**: Map<String, AtomicInteger> en ReflowWebSocketCreator → reject si `connections[IP] > 10`. Regla 14.

**Cross-references**: registrado en Niagara web container; instancia ReflowWebSocket.

**MX60 implication**: **IMPROVE** — agregar CSRF + connection-per-IP. Regla 14 nueva.

---

### 59.2.4 ReflowWsHttpSessionListener.java (40 líneas)

**Propósito**: Listener para ciclo de vida de HTTP session. Cuando session destroyed (logout, timeout), cierra WebSocket asociado.

**Estructura**: HashSet<IHttpSessionDestroyListener> sincronizado static + add/remove static methods + sessionDestroyed() notify.

**Patterns identificados (KEEP)**:
- Observer pattern para session lifecycle coupling
- Synchronized HashSet anti-race

**Antipatterns identificados**:

> **AP-45 NEW MEDIUM** — "Listener not auto-deregistered on socket error"
>
> **Site**: ReflowWsHttpSessionListener (combinado con ReflowWebSocket.onError)
> **Descripción técnica**: `onError()` deregistra (L417 BReflowWebSocketAcceptor), pero si onError NO fire (TCP reset abrupto, kill -9 cliente, disconnect físico de red), socket queda registrado → memory leak en listener Set.
> **Severity**: MEDIUM (memory leak progresivo)
> **Fix recomendado**: WeakHashMap o cleanup explícito por timeout en BReflowWebSocketAcceptor.

**Cross-references**: registrado por `ReflowWebSocket.onConnect()`; deregistrado en `onClose()` + `onError()`.

**MX60 implication**: **IMPROVE** — cleanup en TODOS los exit paths.

---

### 59.2.5 BReflowChannelService.java (281 líneas)

**Propósito**: Pub-sub channel manager. Tracks subscriptions per channel ("reflow", "reflow-config"). Broadcasts mensajes a todos los subs (excepto sender opcional).

**Estructura**:
```java
public class BReflowChannelService extends BAbstractService {
    private ConcurrentHashMap<String, ReflowChannel> channels;  // L52

    public class ReflowChannel {
        private CopyOnWriteArrayList<ReflowWebSocket> _subs;     // L186

        public void join(ReflowWebSocket sock) { ... broadcast "join" ... }
        public void leave(ReflowWebSocket sock) { ... broadcast "leave" ... }
        public ArrayNode who() { ... [{user, clientId, lastAction, ...}] ... }  // L218
        public void broadcast(ObjectNode payload, List<ReflowWebSocket> except) { ... }
    }
}
```

**Patterns identificados (KEEP)**:
- **CopyOnWriteArrayList** excelente para pub-sub (snapshot iterators, sin synchronization en broadcast loop)
- **ConcurrentHashMap** sin synchronized blocks en call sites (low contention)
- **except list** previene echo (no manda back al sender)
- **Metadata tracking**: lastActionTime, lastRoute, lastMessageTime para idle detection en frontend

**Antipatterns identificados**:

> **AP-46 NEW LOW** — "ObjectMapper created per who() call"
>
> **Site**: BReflowChannelService.java:218
> **Descripción técnica**: Cada `who()` instancia ObjectMapper, ObjectNode, loop. SIN pooling. Frecuencia alta (frontend hace polling implícito por `channel-status` events) → GC pressure.
> **Severity**: LOW (perf only)
> **Fix recomendado**: cache static ObjectMapper, o usar ObjectWriter (thread-safe).

> **AP-47 NEW MEDIUM** — "Broadcast errors silently swallowed"
>
> **Site**: BReflowChannelService.java:249, 267
> **Descripción técnica**: dos `catch (Exception) {}` con bloque vacío. Si `socket.send()` falla (socket cerrado, network drop), nadie se entera.
> **Severity**: MEDIUM (silent failure operacional)
> **Fix recomendado**: log warn en catch, posiblemente eliminar socket de la lista si send falla repetidamente.

> **AP-48 NEW MEDIUM** — "Broadcast except list logic broken"
>
> **Site**: BReflowChannelService.java:244-245, 262-263
> **Descripción técnica**:
> ```java
> if (except != null && except.contains(sock)) {
>     return;  // BUG: exits LOOP entirely, not just this socket!
> }
> ```
> Debe ser `continue;`. Cuando primer socket de la lista except aparece, broadcast se detiene → recipients posteriores NO reciben mensaje.
> **Exploit scenario**: cliente A en except[0]. Sockets B, C, D suscritos al canal. Broadcast → loop entra socket A → `return` → B, C, D nunca reciben. **Channel broadcast roto en presencia de except list**.
> **Severity**: MEDIUM (correctness bug, no security)
> **Fix recomendado**: cambiar `return` a `continue`.

**Cross-references**: llamado por `BReflowWebSocketAcceptor.onMessage()` L305-307 para "channel-status" broadcast.

**MX60 implication**: **IMPROVE** — fix except loop bug, agregar logging, pool ObjectMapper.

---

### 59.2.6 BReflowWebSocketAcceptor.java (505 líneas) — PIEZA CENTRAL

**Propósito**: Acceptor principal. Listens client messages, rutea a commands, maneja connection lifecycle. **Pieza más grande del módulo**.

**Estructura**:
```java
public class BReflowWebSocketAcceptor extends BAbstractService {

    private Array<WebSocketInfo> webSockets;    // L101 — synchronized
    private ArrayList<IReflowCommand> commands; // L103

    // Per-connection inner class
    @WebSocket
    public class ReflowWebSocket {
        Session session;
        HttpSession httpSession;
        BReflowService service;
        Context cx, acceptCx;
        long clientId;
        WebSocketInfo info;

        @OnWebSocketConnect public void onConnect(Session) { ... }
        @OnWebSocketMessage public void onMessage(String json) { ... }   // L290+
        @OnWebSocketClose public void onClose(int code, String reason) { ... }
        @OnWebSocketError public void onError(Throwable t) { ... }

        public void send(String msg) { session.getRemote().sendStringByFuture(msg); }
        public void sendSync(ReflowSyncResponse r) { /* 2-thread Piped */ }
    }

    // WebSocketInfo (metadata per connection)
    static class WebSocketInfo {
        long acceptTime, lastActionTime, lastMessageTime;
        ObjectNode lastRoute, configRoute;
        ReflowWebSocket socket;
    }

    public void addCommand(IReflowCommand cmd) { commands.add(cmd); }
    public String spy() { /* HTML table of webSockets */ }
}
```

**Patterns identificados (KEEP)**:
- **`BasicContext(cx, facets)` wrapping** (L233): propaga user + agrega metadata (remoteHost, remotePort) al contexto.
- **Jetty annotations**: `@OnWebSocketConnect`, `@OnWebSocketMessage`, `@OnWebSocketClose`, `@OnWebSocketError` — declarativo, clean, sin state machine manual.
- **`AccessController.doPrivileged()`** (L311-314): corre command dentro del Niagara security context.
- **`Array<>` synchronized** para webSockets: evita ConcurrentModificationException durante iteración en spy() / cleanup.
- **WebSocketInfo metadata**: acceptTime, lastActionTime, lastRoute, lastMessageTime — habilita spy() table + idle detection cliente.

**Antipatterns identificados**:

> **AP-49 NEW CRITICAL** — "Context not propagated to async threads (match AP-27 pattern)"
>
> **Site**: BReflowWebSocketAcceptor.java:311-314 + AsyncReflowCommand.java:13
> **Descripción técnica**:
> ```java
> // BReflowWebSocketAcceptor onMessage
> AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
>     cmd.run(this, obj, ticket);   // ← cmd puede ser AsyncReflowCommand
>     return null;
> });
>
> // AsyncReflowCommand.run()
> new Thread(new TaskRunner(socket, message, ticket), "ReflowCommandTask").start();
> //  ^^^ NUEVO thread NO hereda doPrivileged scope NI ThreadLocal context
> ```
> El thread nuevo NO hereda `acceptCx` ni el privileged scope. Si `task()` llama `Sys.getContext()` o `Sys.getUser()` (que retornan ThreadLocal context del thread actual), recibe **system context** en lugar de user context → todos los RBAC checks que dependan de Sys.getContext() bypassed.
> **Mitigación accidental**: handlers actuales usan `socket.acceptCx.getUser()` (acceso explícito por field, NO por ThreadLocal). Pero NO hay enforcement.
> **Exploit scenario**: cualquier extensión futura del WebSocket que llame `Sys.getContext()` para chequeo de permisos correrá con system context. RBAC silenciosamente bypassed.
> **Severity**: CRITICAL (RBAC bypass latente, deuda equivalente a AP-27 sistémico)
> **Fix recomendado**: Pasar acceptCx explícitamente a task() como parámetro, O setear ThreadLocal context en TaskRunner.run() antes de invocar task(). Regla 13 obligatoria.

> **AP-50 NEW MEDIUM** — "Ticket parseInt without bounds check"
>
> **Site**: BReflowChannelService L82-84, BReflowWebSocketAcceptor L319, L410, L52, L37 (multiple)
> **Descripción técnica**: `Integer.parseInt(ticket)` sin validar. Ticket malformed string (e.g., "abc") → NumberFormatException uncaught → socket error → conexión cae.
> **Severity**: MEDIUM (DoS leve por mensaje malformado)
> **Fix recomendado**: validate ticket es numérico, bounds check, usar Long para safety.

> **AP-51 NEW MEDIUM** — "onMessage exception swallowed (no error to client)"
>
> **Site**: BReflowWebSocketAcceptor.java:398-400
> **Descripción técnica**:
> ```java
> catch (Exception ex) {
>     LOGGER.log(Level.SEVERE, "WebSocket acceptor error", ex);
> }
> ```
> Captura todas las excepciones en onMessage. Cliente nunca sabe que el comando falló — promise queda pendiente, UI freeze hasta timeout.
> **Severity**: MEDIUM (UX broken, debug impossible client-side)
> **Fix recomendado**: enviar error response: `{ success: false, error: ex.message, code: errorCode, ticket }`. Regla 15 nueva.

> **AP-52 NEW MEDIUM** — "No command validation before dispatch"
>
> **Site**: BReflowWebSocketAcceptor.java:290, 309
> **Descripción técnica**:
> ```java
> String command = obj.get("command").asText();  // NPE si "command" missing → asText() returns ""
> // Loop iter commands → no cmd matches "" → silent skip
> ```
> Si "command" missing del JSON, `asText()` returns empty string. Loop falla silenciosamente. Sin precedencia explícita custom vs builtin (race posible si command match ambos).
> **Severity**: MEDIUM (silent failure)
> **Fix recomendado**: validar command field present + non-empty; precedencia explícita (custom registered first, builtin second). Regla 15.

> **AP-53 NEW LOW** — "Ticket field not consistently echoed in responses"
>
> **Site**: varios — algunos responses chequean `if (ticket != null)`, otros no.
> **Descripción técnica**: inconsistencia en request-response correlation. Cliente puede recibir response sin ticket → no puede matchear con pending Promise.
> **Severity**: LOW (UX inconsistency)
> **Fix recomendado**: SIEMPRE echo ticket si fue provisto. Regla 15.

> **AP-54 NEW LOW** — "configRoute persists in info indefinitely"
>
> **Site**: BReflowWebSocketAcceptor.java:302
> **Descripción técnica**: `info.configRoute = obj.get("route")` guarda Object grande indefinidamente. Cliente malicioso manda JSON huge en field `route` → memory leak per WebSocketInfo.
> **Severity**: LOW (slow memory leak)
> **Fix recomendado**: limit field size; clear on lease/disconnect.

> **AP-55 NEW LOW** — "who() called per-join without memo"
>
> **Site**: BReflowChannelService:88, 133
> **Descripción técnica**: `chan.who()` crea ObjectNode cada vez. SIN caching entre llamadas consecutivas (broadcasts cascading).
> **Severity**: LOW (perf bajo carga)
> **Fix recomendado**: cache who() result, invalidate on join/leave events.

**Cross-references**: llamado por SocketServlet factory; llama a BReflowChannelService para pub-sub; registra IReflowCommand handlers (4 impls cross-module).

**MX60 implication**: **IMPROVE** — AP-49 (cx propagation) CRITICAL must fix; AP-50-55 IMPROVE via reglas 13+15.

---

### 59.2.7 Frontend Vue components (componentes WebSocket UI)

**SocketConnect.vue** (3795 bytes): dialog "Connecting to Reflow" con estados disconnected/connecting/connected. Watcher en `socketStatus` de Vuex store. Auto-reconnect con backoff implícito (10s timeout).

**SocketAuth.vue** (3795 bytes): dialog para incoming control request — cuando OTRO usuario solicita control y vos lo tenés. Botones "Allow" / "Deny". Si timeout sin respuesta → auto-grant (configurable, default 30s).

**SocketRequest.vue** (5813 bytes): dialog cuando vos NO tenés control — muestra controller actual (username, lastActivity), botón "Request Control". Manda mensaje WebSocket `command: "config-control", action: "request"`.

**SocketResponseError.vue** (1009 bytes): display de error envelope. **Pero AP-51 server NUNCA envía error envelope** → este componente NUNCA recibe data útil. UX broken.

**Patterns identificados**:
- ✅ Dialog-based UX para control flow (claro, audit-friendly)
- ✅ Vuex centralized state para socketStatus + hasControl + controller
- ❌ SocketResponseError.vue inútil sin server enviando errors (AP-51)

**Cross-references**: usados via `<SocketConnect />` en root layout; mounted by `App.vue` o equivalente.

**MX60 implication**: **KEEP** dialog UX pattern + Vuex centralized state. **IMPROVE** error envelope al implementar Regla 15 server.

---

## 59.3 Auth / RBAC / sessions

### 59.3.1 Authentication flow

1. HTTP request autenticado por **BaseServlet** (capa anterior, Bloques 53-58) — Niagara tiene cookie `JSESSIONID` o Auth header.
2. Niagara context (`cx`) almacenado en request attribute `"niagara.context"`.
3. WebSocket upgrade request **hereda** HTTP session + cx (Jetty propaga atributos del HttpServletRequest al UpgradeRequest).
4. `onConnect()` extrae `cx` del request, wraps en `acceptCx = new BasicContext(cx, facets)`.
5. `acceptCx.getUser()` retorna `BUser` autenticado.

**Conclusión**: WebSocket NO tiene autenticación propia — depende 100% de la HTTP session.

### 59.3.2 RBAC

| Comando | Check explícito | Vulnerable a AP-49? |
|---------|-----------------|---------------------|
| `join`, `leave`, `who`, `broadcast` | NINGUNO | N/A (síncrono, Jetty thread) |
| `route`, `config-route` | NINGUNO | N/A |
| `client-info` | NINGUNO | N/A |
| `sync-delta` (ConfigSyncCommand) | usa `socket.acceptCx.getUser()` | **SÍ** si llama Sys.getContext() internamente |
| `config-control` (RequestControlCommand) | usa `socket.acceptCx.getUser()` | **SÍ** if internal Niagara checks |
| `favorites-read` (ReflowOrdTreeFavoritesRead) | `socket.acceptCx.getUser().getUsername()` | **SÍ** + filename injection (predicción 11.3) |
| `favorites-write` (ReflowOrdTreeFavoritesWrite) | `socket.acceptCx.getUser().getUsername()` | **SÍ** + filename injection (predicción 11.3) |

**Hallazgo**: NO hay permission check explícito en commands. Si user con rol `r` (read-only) abre socket y manda `favorites-write`, NO hay enforcement frontend (AP-49 hipótesis), backend depende de que internal helpers chequeen — NO hay garantía sistémica.

### 59.3.3 Sessions

- HTTP session bound a WebSocket via `ReflowWsHttpSessionListener`.
- Session.destroy() (logout, timeout) → listener notify → `onHttpSessionDestroyed()` → close socket.
- **NO timeout explícito de socket más allá de Jetty idle 60s**. Idle 60s = server cierra conexión.

### 59.3.4 Rate limiting

**NINGUNO**. NO throttle per socket, per IP, per command type. AP-44 + AP-42 → DoS vector trivial.

### 59.3.5 CSRF / Origin / Referer validation

**NINGUNO** (AP-43 CRITICAL). Cross-Site WebSocket Hijacking trivial.

### 59.3.6 Heartbeat / Timeout

- Jetty idle 60000ms (SocketServlet L17).
- Frontend manda `action: "ping"` ocasionalmente — manejado en onMessage L292 pero **NO actualiza lastActionTime**.
- **NO server-side heartbeat enviado** al cliente para keep-alive.
- Cliente puede no detectar zombie connection hasta timeout local 10s.

---

## 59.4 Threading model — análisis profundo

### 59.4.1 Tres tipos de threads

1. **Jetty workers** (8 default): onConnect/onMessage/onClose síncronos. Non-blocking design — si onMessage hace I/O blocking, Jetty worker queda secuestrado → starvation.

2. **AsyncReflowCommand spawn**: `new Thread().start()` por mensaje async (AP-42).

3. **sendSync() PipedStream** (L441-490 BReflowWebSocketAcceptor):
```java
PipedInputStream in = new PipedInputStream();
PipedOutputStream out = new PipedOutputStream(in);

Thread threadOut = new Thread(() -> {
    objectMapper.writeValue(out, response);
    out.close();
});
Thread threadIn = new Thread(() -> {
    byte[] buf = new byte[4096];
    int n;
    while ((n = in.read(buf)) > 0) {
        session.getRemote().sendPartialString(new String(buf, 0, n), false);
    }
    session.getRemote().sendPartialString("", true);
});
threadOut.start();
threadIn.start();
threadOut.join();
threadIn.join();
```

**Riesgos**:
- Buffer Piped 1024 bytes default. Si threadOut produce >1024 antes que threadIn consume → threadOut bloquea write.
- Si threadIn falla (network drop) sin liberar buffer → threadOut bloqueado **forever** (no timeout).
- **Deadlock potential**: si Jetty session.getRemote() bloquea internally → threadIn no progresa → threadOut bloquea → threadOut.join() bloquea Jetty worker.

**MX60 implication**: **SKIP** — pattern unusual, deadlock-prone. MX60 debe usar `mapper.writeValueAsString(response)` + `session.getRemote().sendStringByFuture(json)` directo.

### 59.4.2 Context propagation — el problema real

```
HTTP Request
   ↓ Jetty Upgrade
ReflowWebSocket.onConnect (Jetty thread)
   ↓ extracts cx, wraps in acceptCx
   ↓ stores in this.acceptCx
   ↓ ... onMessage (Jetty thread, has socket.acceptCx) ...
   ↓ AccessController.doPrivileged( () -> cmd.run(this, obj, ticket) )
   ↓ cmd.run() = AsyncReflowCommand.run()
   ↓ new Thread(TaskRunner).start()
   ↓
   ↓ ════════ NEW THREAD ═══════════════
   ↓ TaskRunner.run() → task(socket, message, ticket)
   ↓
   ↓ task() implementations:
   ↓
   ↓   Option A: socket.acceptCx.getUser() → CORRECTO (acceso por field)
   ↓   Option B: Sys.getContext().getUser() → BUG! returns system context
   ↓   Option C: Sys.getUser() → BUG! returns system user
```

**Mitigación correcta** (3 opciones):

**A) Pass acceptCx explicit** (recomendado):
```java
protected abstract void task(ReflowWebSocket socket, Context cx, JsonNode message, String ticket);
```
Ventaja: explícito, no magic. Desventaja: cambia signature.

**B) ThreadLocal set in TaskRunner**:
```java
public void run() {
    Context.threadLocal.set(socket.acceptCx);
    try {
        task(socket, message, ticket);
    } finally {
        Context.threadLocal.remove();
    }
}
```
Ventaja: backward-compatible. Desventaja: Niagara Sys context internals son cajas negras.

**C) AccessController.doPrivileged inside thread**:
```java
public void run() {
    AccessController.doPrivileged( ... cmd internal logic ..., socket.acceptCx);
}
```
Ventaja: idiomático Niagara. Desventaja: requiere acceptCx explícito en doPrivileged signature.

**Regla 13 obligatoria** debe especificar Option A o Option B + B como fallback.

---

## 59.5 Wire format detail + correlation

### 59.5.1 Builtin commands (procesados por ReflowWebSocket directamente)

| Command | Payload | Response | Notas |
|---------|---------|----------|-------|
| `join` | `{ command, channel, ticket }` | `{ type: "channel-status", command: "join", channel, success, who: [...] }` broadcast a channel | Idempotente |
| `leave` | `{ command, channel, ticket }` | broadcast equivalente | Idempotente |
| `who` | `{ command, channel, ticket }` | `{ type: "who", channel, who: [...] }` solo al sender | Read-only |
| `broadcast` | `{ command, channel, ticket, payload, except }` | broadcast `payload` a channel except sender | **AP-48 except bug** |
| `route` | `{ command, ticket, route: {fullPath, path} }` | NO response | Stores in `info.lastRoute` |
| `config-route` | similar | NO response | Stores in `info.configRoute` (**AP-54 leak**) |
| `client-info` | `{ command, ticket }` | `{ type: "client-info", clientId, configControl, username, ticket }` | Greeting reply |
| `ping` | `{ command, ticket }` | NO response | Cliente uses para keep-alive — **server NO responde** |

### 59.5.2 Custom commands (registered via addCommand)

| Command | Owner | Async? | Response? |
|---------|-------|--------|-----------|
| `sync-delta` | BReflowSyncService | ✅ | sí (sync delta JSON, vía sendSync PipedStream) |
| `config-control` | BReflowSyncService | ✅ | sí (broadcast control state change) |
| `favorites-read` | (favorites module) | ✅ | sí (favorites JSON) |
| `favorites-write` | (favorites module) | ✅ | sí (success ACK) |

**Bloque 60** auditará el detalle de estos handlers (especialmente ConfigSyncCommand + RequestControlCommand de BReflowSyncService).

### 59.5.3 Error envelope — **NO ESTANDARIZADO**

- Sync failures: log only (AP-51).
- Parse failures (malformed JSON): caught + log, silent.
- Permission denied: depende de helper, generalmente exception → log + silent.
- Cliente NUNCA recibe error envelope → UI hangs hasta timeout cliente (10s).

**MX60 Regla 15 obligatoria** — error envelope estandarizado:
```json
{ "success": false, "error": "<message>", "code": "<error_code>", "ticket": <orig_ticket_or_null> }
```

---

## 59.6 Antipatterns nuevos descubiertos — tabla resumen AP-42..55

| # | Severity | Título | Site primario | Categoría |
|---|---------|--------|---------------|-----------|
| AP-42 | HIGH | Uncontrolled thread spawn in async commands | AsyncReflowCommand:13 | DoS / Resource |
| AP-43 | **CRITICAL** | No CSRF/Origin validation on WebSocket upgrade | SocketServlet:43 | Security / CSWSH |
| AP-44 | MEDIUM | No max connections per IP | SocketServlet:39-41 | DoS / Resource |
| AP-45 | MEDIUM | Listener not auto-deregistered on socket error | ReflowWsHttpSessionListener + onError | Memory leak |
| AP-46 | LOW | ObjectMapper created per who() call | BReflowChannelService:218 | Performance |
| AP-47 | MEDIUM | Broadcast errors silently swallowed | BReflowChannelService:249, 267 | Operability |
| AP-48 | MEDIUM | Broadcast except list logic broken (`return` vs `continue`) | BReflowChannelService:244-245, 262-263 | Correctness bug |
| AP-49 | **CRITICAL** | Context not propagated to async threads | BReflowWebSocketAcceptor:311-314 + AsyncReflowCommand:13 | RBAC bypass (match AP-27) |
| AP-50 | MEDIUM | Ticket parseInt without bounds check | múltiples sites | Robustness |
| AP-51 | MEDIUM | onMessage exception swallowed | BReflowWebSocketAcceptor:398-400 | UX / Operability |
| AP-52 | MEDIUM | No command validation before dispatch | BReflowWebSocketAcceptor:290, 309 | Robustness |
| AP-53 | LOW | Ticket field not consistently echoed | múltiples sites | Inconsistency |
| AP-54 | LOW | configRoute persists in info indefinitely | BReflowWebSocketAcceptor:302 | Memory leak (slow) |
| AP-55 | LOW | who() called per-join without memo | BReflowChannelService:88, 133 | Performance |

**Tally cross-Bloques 50, 51, 53-59**:
- **CRITICAL**: AP-27 (sistémico ~50 sites), AP-43 (CSWSH), AP-49 (RBAC bypass async)
- **HIGH**: AP-10 (backups GET), AP-21 (BQL injection), AP-33 (filesystem disclosure), AP-39 (server↔client desync), AP-42 (thread DoS)
- **MEDIUM**: 16 (incluyendo AP-44, AP-45, AP-47, AP-48, AP-50, AP-51, AP-52)
- **LOW**: 12

**TOTAL AP-1..AP-55** = **55 antipatterns identificados** post-Bloque 59.

---

## 59.7 Patterns excelentes (KEEP literal MX60)

1. **BasicContext wrapping con facets** (BReflowWebSocketAcceptor:233): `acceptCx = new BasicContext(cx, facets)` — propaga user + agrega metadata (remoteHost, remotePort). MX60 pattern exacto.

2. **CopyOnWriteArrayList para pub-sub** (BReflowChannelService:186): snapshot iterators sin synchronization → broadcast loop seguro durante join/leave concurrent. MX60 pattern exacto.

3. **ConcurrentHashMap channel map** (L52): thread-safe sin synchronized blocks. MX60 pattern exacto.

4. **Jetty annotation lifecycle** (`@OnWebSocketConnect`, `@OnWebSocketMessage`, etc.): clean, declarativo, sin state machine manual. MX60 pattern exacto (asumiendo Jetty o equivalente).

5. **IReflowCommand polymorphism** (interface 12L + addCommand registry): contracto simple, extensible. MX60 pattern exacto.

6. **WebSocketInfo metadata tracking** (acceptTime, lastActionTime, lastRoute, lastMessageTime): habilita observability + idle detection cliente. MX60 pattern exacto (ampliar con métricas adicionales: avg messages/min, bytes in/out).

7. **Synchronized Array<> snapshot iteration** (L147-149): `synchronized (webSockets) { ... }` antes de iterar para spy() — previene CMException. MX60 pattern exacto.

8. **Dialog-based UX** (SocketConnect/SocketAuth/SocketRequest Vue components): claro, accessibility-friendly, audit trail visual. MX60 pattern exacto.

---

## 59.8 MX60 implications — continuación desde #101

| # | Tag | Descripción |
|---|-----|-------------|
| 102 | **KEEP** | BasicContext wrapping con facets (remoteHost, remotePort). Pattern explícito MX60. |
| 103 | **KEEP** | CopyOnWriteArrayList para pub-sub broadcasts. Snapshot iterators previenen CMException. |
| 104 | **KEEP** | ConcurrentHashMap channel map. Sin synchronized blocks en call sites. |
| 105 | **KEEP** | Jetty `@OnWebSocketX` annotations. Lifecycle declarativo limpio. |
| 106 | **KEEP** | IReflowCommand polymorphism + ArrayList registry. Extensible, clean contract. |
| 107 | **KEEP** | WebSocketInfo metadata tracking exhaustivo. Observability + idle detection. |
| 108 | **KEEP** | Vuex centralized state para socketStatus + hasControl + controller (frontend). |
| 109 | **KEEP** | Dialog-based UX para control flow (Auth/Connect/Request dialogs). |
| 110 | **IMPROVE** | AsyncReflowCommand thread spawning → BoundedThreadPoolExecutor (Niagara BJobService). Regla 13. |
| 111 | **IMPROVE** | Context propagation a async tasks: pasar acceptCx explícito a task() O setear ThreadLocal. Regla 13 obligatoria. |
| 112 | **IMPROVE** | CSRF/Origin validation en WebSocket upgrade (`Origin` header check en createWebSocket). Regla 14. |
| 113 | **IMPROVE** | Connection limits per IP en factory. Regla 14. |
| 114 | **IMPROVE** | Error envelope estandarizado (`{success, error, code, ticket}`). Regla 15. |
| 115 | **IMPROVE** | Ticket validation (bounds + numeric check) antes de parseInt. Regla 13. |
| 116 | **IMPROVE** | Command precedence explícita (custom registered first, builtin second). Regla 15. |
| 117 | **IMPROVE** | Broadcast except list — fix `return` → `continue`. Critical para channel correctness. |
| 118 | **IMPROVE** | who() result caching — invalidate on join/leave. Reduce ObjectMapper churn. |
| 119 | **IMPROVE** | onMessage exception NO swallow — siempre enviar error response cliente. Regla 15. |
| 120 | **IMPROVE** | onError listener cleanup en TODOS los exit paths (incluyendo abrupt disconnects). Regla 13. |
| 121 | **SKIP** | sendSync() PipedStream pattern. Reemplazar por `mapper.writeValueAsString` + `sendStringByFuture` directo. |
| 122 | **NEW** | Rate limiting per socket/IP (messages/min, bytes/min). Regla 16. |
| 123 | **NEW** | Server heartbeat — server send `{ type: "ping" }` cada 30s. Regla 17. |
| 124 | **NEW** | Error code taxonomy estandarizada (`AUTH_FAIL`, `RATE_LIMIT`, `INVALID_COMMAND`, etc.). Regla 15. |
| 125 | **NEW** | Per-socket metrics (avg latency, msg count, bytes) — exponer via spy() + Niagara metrics. Regla 17. |

**Total MX60 implications post-Bloque 59**: **125 entries** (101 previos + 24 nuevos: 8 KEEP + 11 IMPROVE + 1 SKIP + 4 NEW).

---

## 59.9 Reglas template MX60 — 5 reglas nuevas (13-17)

### Regla 13 — Async command execution must use thread pool + propagate context

```
DEBE USAR: BoundedThreadPoolExecutor(coreSize=N, maxSize=2N, queue=100)
           o BJobService de Niagara para async commands
NO HACER: new Thread().start() en hot path

DEBE PROPAGAR: acceptCx (Niagara Context) al async task. Dos opciones:
  A) Parameter explícito: task(socket, cx, message, ticket)
  B) ThreadLocal set en TaskRunner.run() antes de invocar task()

DEBE LOGUEAR: Rejected tasks (queue full) at WARN level
DEBE EXPONER: métricas pool (queue depth, active threads, rejection count)
```

### Regla 14 — WebSocket upgrade CORS + connection limits

```
DEBE VALIDAR: Origin header matches whitelist en createWebSocket(req, resp)
DEBE RECHAZAR: mismatched origin con HTTP 403 + log WARN
DEBE TRACKEAR: connections per source IP (Map<String, AtomicInteger>)
DEBE RECHAZAR: si connections[IP] > maxPerIP (default 10)
DEBE LOGUEAR: rejected upgrades (Origin, IP, reason) at WARN level
```

### Regla 15 — Error responses estandarizados + command validation

```
TODO COMMAND HANDLER: catch (Exception ex) →
  socket.send({ success: false, error: ex.message, code: errorCode, ticket })

NO HACER: swallow exception, log only
CLIENTE: debe handle error envelope, set state para UI alerts

VALIDAR ANTES DE DISPATCH:
  - command field present + non-empty
  - ticket parseable si presente
  - payload JSON válido
PRECEDENCIA: custom registered commands first, builtin second
```

### Regla 16 — Rate limiting WebSocket messages

```
TRACK: messages per socket per minute
REJECT: si > threshold (default 1000/min) con error code RATE_LIMIT_PER_SOCKET
TRACK: messages per IP across all sockets
REJECT: si > aggregate threshold (default 10K/min) con error code RATE_LIMIT_PER_IP
BACKOFF: cliente debe implementar exponential retry con jitter
EXPONER: metrics rejected count, current rates
```

### Regla 17 — Server heartbeat + connection health metrics

```
SERVER: send { type: "ping", timestamp } cada 30s a cada socket conectado
CLIENT: si no recibe ping en 60s, close socket + reconnect
TIMEOUT: server cierra conexión si cliente no envía ANY msg en 90s
METRIC: heartbeat latency en WebSocketInfo (track time send-pong)
EXPONER: per-socket avg latency + jitter en spy()
```

**Total reglas template MX60 post-Bloque 59**: **17 reglas** (12 previas + 5 nuevas).

---

## 59.10 Predicciones / hipótesis a verificar después

1. **sendSync() deadlock empírico**: bajo carga sostenida, ¿buffer Piped (default 1024 bytes) genera bloqueo en threadOut.write()? Necesita load test con ReflowSyncResponse > 10KB. **Predicción**: deadlock bajo carga moderada (>50 sync-delta concurrent).

2. **onError() fire rate en disconnects abruptos**: ¿Jetty onError() siempre fire en abrupt TCP reset, o solo en protocol errors? Verificar Jetty docs + test con kill -9 en cliente. **Predicción**: NO fire en kill -9 → AP-45 confirmado.

3. **Favorites filename injection (cross-AP-33)**: `ReflowOrdTreeFavoritesRead.java:35` posiblemente usa `BFileSystem.getStationHome().FilePath(user + ".json")`. Si username contiene `..`, traversal posible. **Predicción**: BUserService canonical + valida username — pero si custom auth bypass username valid → traversal posible. Verificar BUserService.

4. **acceptCx.getUser() null safety**: onConnect extrae cx de request attribute. Si missing (malformed upgrade?), cx null → NPE en `new BasicContext(cx, ...)`. **Predicción**: Jetty rechaza upgrade antes de NPE — pero check defensive ausente.

5. **socket.service null safety**: onConnect L223 setea `this.service = Sys.getService(BReflowService.TYPE)`. Si service no running, getService returns null → NPE en L224 `.getWebSocketAcceptor()`. **Predicción**: error path, raro en producción pero crash en startup race.

6. **Ticket type overflow**: ticket sent integer (Integer.parseInt), JSON allows long. Edge case `ticket=9999999999999999999` (> Long.MAX_VALUE) → parsing fails. **Predicción**: AP-50 confirmado.

7. **onClose() vs onMessage() race**: Jetty pool worker thread ejecutando onMessage() mientras otro worker procesa onClose() del mismo socket. Synchronized block sobre webSockets array previene CMException, pero command handler in-flight con socket cerrado → send falla silently → AP-47.

8. **maxTextMessageSize overflow handling**: 256KB limit. Si cliente manda 256KB, Jetty buffer + onMessage receives full string. Si JSON falla parse (huge payload), exception swallowed (AP-51). **Predicción**: cliente recibe nada, espera timeout 10s.

---

## 59.11 Cierre Capa 17 — gaps remanentes

| Layer | Bloque | Estado | Gaps |
|-------|--------|--------|------|
| BajaScript canonical | 50-52 | ✅ COMPLETO | — |
| `yi`/serverSideCall RPC | 53 | ✅ COMPLETO | — |
| HTTP REST (BaseServlet + 34 Response classes) | 58 | ✅ COMPLETO | Response classes complejas (Bloque 60) |
| WebSocket trinity 3/3 | **59 (THIS)** | ✅ COMPLETO | Custom commands detail (Bloque 60) |
| Sync layer (ConfigSyncCommand, RequestControlCommand) | — | ⏸ Bloque 60 | Auditar handlers reales async path |
| BackupManager business logic | — | ⏸ Bloque 60 | Solo AP-27 sites cubiertos en 58 |
| Frontend Reflow Vue completo | — | ⏸ Bloque 61 | Store, router, services, components principales |
| `-ux` modules (workbench views) | — | ⏸ Bloque 62 | Cierre definitivo Reflow |

**Capa 17 cierre definitivo en Bloque 62**. Después de eso → pivote Analytics module (Bloque 63+).

---

## 59.12 Síntesis para el yo 2027

Si tenés que decidir arquitectura WebSocket de MX60 sin re-leer todo:

1. **Hereda literal**: BasicContext wrapping, CopyOnWriteArrayList, ConcurrentHashMap, Jetty annotations, IReflowCommand polymorphism, WebSocketInfo metadata, synchronized Array<> para snapshots, dialog UX components.

2. **Reescribe obligatorio (Reglas 13-17)**:
   - **Regla 13**: thread pool + cx propagation explícita (AP-42 + AP-49)
   - **Regla 14**: CSRF/Origin + connection limits (AP-43 + AP-44)
   - **Regla 15**: error envelope + command validation (AP-51 + AP-52)
   - **Regla 16**: rate limiting (mitigación AP-42 + AP-44)
   - **Regla 17**: heartbeat + per-socket metrics (mitigación AP-45)

3. **Descarta**: sendSync() PipedStream pattern. Usar `mapper.writeValueAsString` + `session.getRemote().sendStringByFuture(json)` directo.

4. **Pre-commit hooks anti-AP-49**: grep `new Thread\(.*Reflow|new Thread\(.*Async` → REJECT.

5. **Pre-commit hooks anti-AP-43**: grep `WebSocketServletFactory` sin `Origin` validation en factory → REJECT.

6. **Test harness obligatorio día 1**:
   - WebSocket integration test: connect + send command + assert response envelope
   - CSWSH test: cross-origin upgrade attempt → 403
   - Rate limit test: spam 10K msgs → reject + recovery
   - RBAC test: user `r` permission tries `favorites-write` → denied + audit log
   - Deadlock test: sync-delta con payload 100KB × 50 concurrent → no hang

**Nota crítica**: AP-49 es PEER de AP-27 (no descendiente) — ambos son context propagation issues pero AP-27 es síncrono (helpers Java), AP-49 es asíncrono (thread spawn). MX60 debe atacar AMBOS via Regla 11 (síncrono cx end-to-end) Y Regla 13 (asíncrono cx propagación a pool).

---

**End of Bloque 59** — WebSocket layer audit completo.

**Siguiente**: Bloque 60 (Response classes complejas + sync layer + BackupManager business logic).
