# Block 143 — nmodsreflow.77 (`-rt`): subsistema sync (colaboración config multiusuario JSON-Patch, favoritos por-usuario, sin locking)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), paquete `sync/` del runtime `-rt`**: cómo Reflow
> sincroniza en tiempo real un `config.json` compartido entre clientes WebSocket vía deltas JSON-Patch, y
> cómo persiste favoritos ORD-tree por usuario. Cubre `BReflowSyncService` (+ sus tasks/commands internos),
> `ConfigIO` (capa de persistencia read/write), `ReflowSyncResponse`(+serializer) y los comandos
> `ReflowOrdTreeFavoritesRead`/`Write`. NO cubre los `Config*Response` REST (eso es R9) ni el `QueryFilter`
> taint source (R13).
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Cierra el gap **R7**. Corpus language: Spanish
> (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow`
> `SYNC/` = `RT/sync`. Comandos favoritos: `SYNC/commands`.
>
> Método: decompile Vineflower del JAR embarcado + lectura directa + grep de tokens/callers. Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción anclada a líneas `[CERT]`.
> Nota de decompilado: Vineflower dejó ofuscados algunos nombres Jackson (`method_291`=`get(String)`,
> `method_297`=`set`/`putPOJO`); se citan tal cual.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 138] (`BReflowSyncService` sólo monta bajo
> `BReflowService`; registra sus commands en el WS acceptor), [Block 140] (los sync-commands son
> `IReflowCommand`/`AsyncReflowCommand` del canal WS), [Block 141]/[Block 142] (mismo patrón `doPrivileged`
> ancho + threads crudos), [Block 75]/[Block 113] (skipModuleValidation), [Block 139] (licensing bypass).

---

## 143.1 — Qué es "sync" y cómo monta `[CERT]`

`BReflowSyncService` **extends `BAbstractService`** (no es un BComponent plano) `[CERT]`
`SYNC/BReflowSyncService.java:35`. Sólo es legal montarlo bajo el service central de B138 `[CERT]`
`:65-66`:

```
public boolean isParentLegal(BComponent parent) { return parent instanceof BReflowService; }
```

`[INFER]` "Sync" NO es replicación station-to-station ni config por-usuario: es **colaboración realtime
multiusuario sobre UN `config.json` compartido**, dirigida por deltas JSON-Patch. En `startup()` elige uno de
dos modos mutuamente excluyentes según `service.getMultiUserConfig()` `[CERT]` `:128`:

- **multiusuario**: registra `ConfigSyncCommand` (canal `"sync-delta"`) + carga el archivo de config.
- **single-user**: registra `RequestControlCommand` (`"config-control"`) — un protocolo cooperativo de
  single-writer lock (grant/revoke/request/accept/reject). `[CERT]` bloque `:187-313`.

Los comandos de favoritos Read/Write se registran en **ambos** modos `[CERT]` `:135-136`.

**Sin slots frozen:** pese al `@NiagaraType`, no hay `@NiagaraProperty`/`@NiagaraAction`; todo el estado son
campos Java planos (`config`, `lastSync`, `lastWrite`, `requestTimeout=30000`, `saveTimeout=5000`) `[CERT]`
`:39-48`. `[INFER]` el service no expone estado sincronizable por el ORD framework — la sincronización es
ad-hoc sobre el archivo.

**Scheduling con `java.util.Timer`, NO `Clock.schedule`:** `[CERT]` `:21-22,46-48`. `queueSaveFile`
debouncea la persistencia 5 s tras la última edición; el revoke de config-control es un one-shot a
`requestTimeout` `[CERT]` `:266-267`.

## 143.2 — El hallazgo central: config-write bajo doPrivileged ancho SIN permission check `[CERT]`

`applyConfig` (comando `sync-delta`) envuelve el spawn+join de un thread en `AccessController.doPrivileged`
`[CERT]` `BReflowSyncService.java:339-341`, y dentro `ConfigSyncTask.run` aplica un **delta JSON-Patch provisto
por el cliente** al config compartido y lo re-broadcastea al canal `"reflow"` para que los demás converjan
`[CERT]` `:420,441`:

```
JsonNode updatedState = JsonPatch.apply(this.data.method_291("delta"), service.config);   // :420
...
node.method_297("delta", this.data.method_291("delta"));                                   // :441 (rebroadcast)
```

El delta luego se persiste a `^reflow/config.json` vía `queueSaveFile`→`SaveConfigurationFileTask` `[CERT]`
`:164`. **Autorización — ausente:** grep confirma que **ningún** comando/response bajo `SYNC/` declara
`requiredPermissions` `[CERT]` (grep negativo sobre `SYNC/`). `[INFER]` Es decir: cualquier usuario que pueda
abrir el WebSocket de Reflow puede mutar el `config.json` compartido de TODOS los usuarios, y ese patch corre
bajo privilegio elevado (el `doPrivileged` **remueve** la restricción de privilegio del caller). Es el
**cuarto** subsistema con el patrón `doPrivileged` ancho (tras B140 dispatch WS, B141 history, B142 alarms) y
el **único que además escribe estado persistente** desde input del cliente.

`[INFER]` La librería es `com.flipkart.zjsonpatch.JsonPatch` `[CERT]` `:7` — un JSON-Patch RFC6902 de
tercero embarcado (coincide con lo que B51 detectó del lado frontend).

## 143.3 — ConfigIO: persistencia sin locking `[CERT]`

Persiste en el station home con paths `^` hard-coded `[CERT]` `SYNC/ConfigIO.java:18-20`:

```
CONFIG_FILE      = "^reflow/config.json"
CONFIG_CACHE     = "^reflow/cache"
CONFIG_FAVORITES = "^reflow/favorites"
```

Escribe JSON vía Jackson por `makeFile(new FilePath(...))` `[CERT]` `:96`, con copias de cache GZIP `[CERT]`
`:10`. **Locking — ausente:** cada write spawnea un `Thread` crudo sin sincronización, lock ni cola `[CERT]`
`:25,48,54,88`. `[INFER]` Dos `favorites-write`/`config` concurrentes corren sobre el mismo archivo sin
exclusión mutua → last-flush-wins / corrupción por interleaving. **Excepciones tragadas:** los `catch`
sólo hacen `println`/`printStackTrace` y siguen — un write fallido "tiene éxito" desde la vista del socket
`[INFER]` (patrón repetido en toda la clase).

**Path-traversal — latente pero NO alcanzable vía sync:** `getOutputStream(location,...)` concatena el
`filename` del caller sin sanitizar en un `FilePath` `[CERT]` `:31,114`. `[INFER]` Sería explotable si un
caller pasara `location` no confiable, pero (§143.4) el único componente de path variable es el username
server-side; los demás son constantes. Verdicto: latente en `ConfigIO`, no explotable por los comandos sync
tal como están escritos.

## 143.4 — Favoritos ORD-tree por usuario (multiusuario) `[CERT]`

Ambos comandos extienden `AsyncReflowCommand`, `ownerId="reflowSyncService"`. Clave de seguridad: **el
username sale del contexto autenticado del socket, no del input del cliente** `[CERT]`:

- **Read**: `user = socket.acceptCx.getUser().getUsername()`; `location = CONFIG_FAVORITES + "/" + user + ".json"`
  `[CERT]` `ReflowOrdTreeFavoritesRead.java:31-32`. Devuelve `{items: <favoritos>}` del propio usuario. `[INFER]`
  no honra ningún parámetro de usuario → un usuario no puede pedir el archivo de otro.
- **Write**: `user = socket.acceptCx.getUser().getUsername()`; sólo si `favorites.isArray()` llama
  `ConfigIO.writeFavorites(favorites, user + ".json")` `[CERT]` `ReflowOrdTreeFavoritesWrite.java:26-27`.
  `[INFER]` filename derivado de la sesión autenticada, NO del cliente → **usuario A no puede escribir el
  archivo de B**; no hay componente de path atacante-controlado → sin path traversal en esta ruta. El
  contenido sólo se valida por shape (`isArray()`), no por contenido — ORDs arbitrarios se guardan verbatim,
  pero confinados al propio archivo del caller.

**Autorización — sólo autenticación WS:** ninguno de los dos declara `requiredPermissions` ni hace check de
permiso `[CERT]` (grep negativo). `[INFER]` la autorización descansa únicamente en que el socket esté
autenticado; cualquier usuario autenticado lee/escribe SUS favoritos. Si eso es vulnerable depende del
channel-level auth del acceptor (B140), fuera de este barrido.

**Info-leak menor:** el error de favorites-read echa el mensaje de excepción crudo al socket `[CERT]`
`ReflowOrdTreeFavoritesRead.java:63`: `... "message": "" + var13.getLocalizedMessage() + "" ...` — sin
escapar, un `"` en el mensaje rompe el JSON de la propia respuesta `[INFER]`.

## 143.5 — ReflowSyncResponse (contrato de respuesta) `[CERT]`

POJO con tres campos opcionales `ticket`/`fields`/`node` `[CERT]` `ReflowSyncResponse.java:6-9`; el
`StdSerializer` emite un objeto plano: inline de `fields`, siempre clave `"ticket"` (string o null) y siempre
`"data"` (el `node` o null) `[CERT]` `ReflowSyncResponseSerializer.java:20-40`. `[INFER]` `ticket` correlaciona
respuesta↔request; `data` lleva el payload. Nota: la mayoría de rutas de comando en vivo bypassean este
wrapper y construyen `ObjectNode`/`JSONObject` con `socket.send(...)` directo (§143.2/143.4).

## 143.6 — Cross-cutting vs. history/alarms `[CERT]`

| Patrón | history (B141) | alarms (B142) | sync (B143) | Cita |
|---|---|---|---|---|
| `doPrivileged` ancho sobre input HTTP/WS | sí | sí | **sí — y escribe estado persistente** | `BReflowSyncService.java:339` |
| BQL por concat | sí | sí (uuid) | **ausente** | grep negativo |
| Thread crudo sin pool | sí | sí | sí (pervasivo) | `ConfigIO.java:25,48,54,88` |
| Cache gzip en disco | sí | ausente | sí (copias de cache) | `ConfigIO.java:10` |
| Locking en escritura | n/a | n/a | **ausente** | `ConfigIO.java:25` |
| `requiredPermissions` en la superficie de escritura | — | `="r"` (read) | **ninguno** | grep negativo `SYNC/` |

## 143.7 — Connections

- **[Block 138]** — `BReflowSyncService` sólo monta bajo `BReflowService` (`isParentLegal`) y registra sus
  commands en el WS acceptor de B138.
- **[Block 140]** — los sync-commands (`sync-delta`, `config-control`, favoritos) son `AsyncReflowCommand`
  del canal WS; el broadcast de convergencia usa el canal `"reflow"` de B140.
- **[Block 141]/[Block 142]** — mismo patrón `doPrivileged` ancho + threads crudos; sync lo agrava porque el
  bloque privilegiado **escribe** `config.json` desde un delta del cliente, no sólo lee.
- **[Block 75]/[Block 113]** — `skipModuleValidation` / code-signing.
- **[Block 139]** — licensing bypass.

**Nota de seguridad cross-focus (REFORZADA desde B142 §142.8):** R7 añade la superficie de **config-write**
que faltaba. Cuadro agregado actualizado: (1) **cuatro** subsistemas de Reflow corren bloques `doPrivileged`
anchos sobre input del cliente (B140/B141/B142/B143); (2) B143 es el más grave del lado escritura — el comando
`sync-delta` aplica y **persiste** un JSON-Patch provisto por el cliente a `^reflow/config.json` **sin ningún
`requiredPermissions`**, bajo privilegio elevado, y **sin locking** (race entre writers); (3) B142 aporta la
BQL injection concreta (`uuid`) a read-level; (4) todo ese privilegio descansa en que el módulo esté
firmado/validado, pero B75/B113 mostraron que la validación de módulo puede apagarse vía `skipModuleValidation`
y B139 documentó el bypass del licensing RSA. `[INFER]` La combinación —config-write no autorizado bajo
doPrivileged + BQL injection read-level + múltiples bloques privilegiados anchos + validación de módulo
desactivable + licensing con bypass— consolida la necesidad de un **bloque de síntesis cross-focus**
(nmodsreflow × platform-security). Queda anotada; no se resuelve en R7 (read-only, cruza focuses).
