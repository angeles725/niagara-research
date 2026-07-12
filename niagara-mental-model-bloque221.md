# Block 221 — Reflow dashboard-builder (VI): el motor de update en vivo (JSON-Patch + control multiusuario)

> **Qué documenta.** El motor server-side que hace que "editar el dashboard se actualice en vivo": la aplicación
> de JSON-Patch RFC-6902 sobre el config en memoria, el modelo de CONTROL multiusuario (quién puede escribir), la
> persistencia debounced y el broadcast. Gap BG3 del focus `nmodsreflow-builder`. Profundiza el mecanismo que
> B217 §217.4-217.5 introdujo desde el cliente.
>
> **Alcance.** El lado servidor de la sincronización (`BReflowSyncService`). El diff/apply del CLIENTE
> (fast-json-patch) y la forma de la card son B217. La superficie de seguridad de esto (doPrivileged sin auth)
> está en B143/B150; aquí se documenta el MECANISMO de producto.
>
> **Fuente (primaria).** Java RT:
> `com/niagaramods/nmodsreflow/sync/BReflowSyncService.java` (585 líneas; leído directo, `file:line`).
>
> **Método / markers.** `[CERT]` = leído en la fuente primaria. `[INFER]` = deducción. Nota: los nombres
> `method_291/311/303/297/312` son de Jackson desofuscados por Vineflower (`get`/`put String`/`put long`/`set`/
> `put boolean` respectivamente) — se citan como aparecen y se explica el rol.

---

## 221.1 — Punto de entrada: `applyConfig()` corre el patch en un hilo privilegiado `[CERT]`

`applyConfig(username, clientId, data)` (`:336-355`) es lo que invoca `ConfigDeltaResponse` (B217) al recibir un
`config_delta`. Crea un `ConfigSyncTask` y lo corre bajo `AccessController.doPrivileged` en un `Thread` dedicado
que arranca y hace `join()` (`:339-346`) — es decir, la aplicación del patch corre a **permisos de sistema**,
sincrónicamente respecto del request. Si algo falla, devuelve `{patched:false, config-timestamp:lastSync, error}`
(`:348-353`). (El `doPrivileged` ancho es el hallazgo de seguridad de B143/B150; aquí es el mecanismo de ejecución.)

## 221.2 — El corazón: `ConfigSyncTask.run()` — apply, rollback, broadcast `[CERT]`

`ConfigSyncTask.run()` (`:412-455`) es el motor real:

1. Guard `service.config != null` (`:416`); guarda `from = lastSync` (`:417`) para poder revertir.
2. **Aplica el patch**: `JsonPatch.apply(data.get("delta"), service.config)` (`:420`, `com.flipkart.zjsonpatch`) —
   el motor RFC-6902 server-side. `setConfigNode(updatedState)` (`:421`), `lastSync = now` (`:422`), respuesta
   `{patched:true, config-timestamp}` (`:423-425`).
3. **Rollback ante error** (`:426-435`): si `JsonPatch.apply` lanza, `lastSync = from` (revierte el timestamp),
   respuesta `{patched:false, error}`, y **`return` — NO se hace broadcast** (los otros clientes no ven un delta
   fallido). Es un manejo transaccional del timestamp, aunque el `service.config` en sí no se restaura
   explícitamente `[INFER]` (apply produce un nodo nuevo; el viejo sólo se reemplaza en el happy-path `:421`).
4. **Broadcast del delta** (`:437-446`): arma `{type:"delta", timestamp, from, delta:<el mismo patch>,
   author:{username, clientId}}` y lo emite en el canal `"reflow"`. Los demás clientes aplican ESE patch a su
   Vuex (B217 §217.5) → merge en caliente sin reload.
5. **Persistencia debounced**: `service.queueSaveFile()` (`:447`, §221.5).

Diseño clave `[INFER]`: el delta se re-emite VERBATIM (el server no recalcula el diff), y lleva `from`+`timestamp`
para que los clientes detecten desincronización. Es una arquitectura de **event-sourcing ligero**: un patch
autoritativo aplicado en el server y reenviado a todos.

## 221.3 — El comando WS `sync-delta` y `sendFullState` `[CERT]`

Antes de mandar un delta, el cliente hace ping del comando WS `sync-delta` (`ConfigSyncCommand`, `:357-391`;
`NAME="sync-delta"`, `ownerId="reflowSyncService"`). Su `task()` (`:375-391`) responde
`{sendFullState: service.config == null}` (`:385`): si el server aún no tiene config en memoria, le pide al cliente
el estado COMPLETO (fallback a full-write, B217 §217.4b); si ya lo tiene, el cliente manda sólo el delta. Es el
handshake que decide delta-vs-full en cada save multiusuario.

## 221.4 — Control multiusuario: un solo escritor con "config control" `[CERT]`

Reflow serializa la edición concurrente con un **token de control** (no locking de datos):

- `getConfigController()` (`:176-185`): el socket WS cuyo flag `configControl==true` — hay **a lo sumo uno**.
- `grantConfigControl(socket)` (`:187-211`): revoca el control de todos (`revokeConfigControl`, `:213-220` pone
  `configControl=false` a cada socket), marca este socket, y hace broadcast `{type:"control-change",
  controller:{username, clientId}}`.
- `requestConfigControl(socket, message)` (`:242-…`): si no hay controller → lo otorga; si el pedidor YA es el
  controller → ok; si otro tiene control → registra un `activeControlRequest` y broadcast `{type:"control-request"}`
  (le pide al controller que ceda). `revokeConfigTimer` (`:265-281`) expira el control tras un timeout.

**Modelo de producto** `[INFER]`: el que tiene "config control" hace **full-writes** (`config_update`); los demás,
mientras tanto, propagan **deltas**. El control es cooperativo (pedir→ceder, con timeout de auto-revocación), no un
lock duro. Esto explica el modal "Reload Required" de B217 §217.5: cuando el controller hace un full-write, los
demás deben recargar porque su estado incremental ya no es confiable.

## 221.5 — Persistencia debounced y recarga `[CERT]`

`queueSaveFile()` (`:315-327`) NO escribe a disco en cada delta: cancela el timer previo y programa un `TimerTask`
(debounce) que persiste el config una sola vez tras la ráfaga de ediciones — evita I/O por cada patch. Simétrico al
debounce del cliente (B217 §217.4, 3s). `reloadConfigurationFile()` (`:153`) recarga el `config` en memoria desde
disco (lo usa `ConfigUpdateResponse` tras un full-write multiusuario, B217 §217.4a). `saveFileTimer`/
`revokeConfigTimer` son los dos temporizadores de estado del servicio (`:46-47`).

## 221.6 — Conexiones

- **[Block 217]** §217.4-217.5 — el lado CLIENTE (diff fast-json-patch, apply STATE_DELTA, modal Reload); §221
  es el lado SERVIDOR (apply zjsonpatch, control, broadcast, persist). Juntos cierran el ciclo edit→save→propagación.
- **[Block 216]** §216.1 — identificó `flipkart-zjsonpatch`; §221.2 lo muestra en `:420` como el apply autoritativo.
- **[Block 143]/[Block 150]** — el `doPrivileged` ancho (`:339`) y el config-write sin permission-check son el
  hallazgo de seguridad; §221 documenta el mismo código como el mecanismo de producto (dos lecturas del mismo `:336`).
- **Hacia adelante**: BG4 (cómo el editor GENERA las mutaciones que disparan estos saves), BG10 (síntesis de
  producto end-to-end), BG11 (chihuahua NO tiene control multiusuario ni JSON-Patch — usa POST directos con RBAC;
  comparar).
