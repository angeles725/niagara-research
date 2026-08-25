# Block 513 — `apis` API6: the Fox CLIENT programmatic API — `javax.baja.fox.BFoxProxySession` (+ `BFoxSession`/`BFoxClientConnection`/`BFoxChannelRegistry`), the public Java surface a module uses to open a Fox session to a remote station, its shared-connection interest model, named typed channels, and `rpc()` — the Fox-transport face of the `niagaraRpc` verb

> **Focus:** `apis`, gap **API6** — the module-author Fox CLIENT API. READ-ONLY, decompiled; no run. Markers §3.
> **REMITTANCE-checked GENUINE:** distinct from [B134] (Fox WIRE: frames/opcodes/SCRAM), [B414]–[B420] (the
> niagaraNetwork focus — `BFoxClientConnection` appears there only as an *internal detail* of `BNiagaraStation`;
> B420 explicitly defers wire to B134), and [B471]–[B473] (a hand-rolled Python client). None documents the
> public `BFoxProxySession` API.
> **Sources:** FUENTE 3 — `organized/fox/fox-rt/decompiled/{javax/baja/fox, com/tridium/fox/sys}`. FUENTE 1 —
> [B134] (wire), [B510] (SCRAM/auth), [B507]/[B512] (the `niagaraRpc` verb's other transport). Evidence delegated
> to a `sonnet` locate+REMITTANCE sweep; ALL load-bearing file:line RE-VERIFIED inline.

## §513.1 — The public entry point + classes `[CERT]`

`javax.baja.fox.BFoxProxySession` is the public factory/facade a module author calls — NOT locked behind
`BNiagaraStation`:

```
BFoxProxySession.java:56  static BFoxProxySession make(BHost host, int port, boolean useFoxs, BIUserCredentials credentials)
```
(overloads at `:48`/`:52` take username/password.) The concrete impl is `com.tridium.fox.sys.BFoxSession`
(`make()` → creates a `BFoxClientConnection`; lifecycle `connect()`/`disconnect()`/`close()`).

`BFoxClientConnection` (`com/tridium/fox/sys/BFoxClientConnection.java:130`) is the persistent connection component:
`port` default **1911** (`:135`, `BFoxScheme.DEFAULT_PORT`), `useFoxs` default **false** (`:136`, → TLS Foxs when
true), `retryPeriod` 5 min, `credentialStore` (`BClientCredentials`). `connect()` (`:350`) opens the TCP socket and
runs the Fox handshake. **Shared-connection interest model:** `engageNoRetry(Interest)`/`engageRetry(Interest)` —
multiple consumers share ONE connection with linger management (so N device-exts to the same station reuse a
single Fox pipe).

## §513.2 — Channels: the client-side registry `[CERT]`

`session.getConnection().getChannels()` → `BFoxChannelRegistry` (`:35`) with typed accessors: `getSysChannel()`
→ `BSysChannel` (`:46`), `getFileChannel()` → `BFileChannel` (`:50`), `getUserChannel()` → `BUserChannel` (`:54`),
and generic `get(String name, Type channelType)` (`:58`) — the call [B415] saw from the consumer side
(niagaraDriver getting `"point"`/`"archive"`/`"history"` channels). A custom channel subclasses `BFoxChannel`
(`:134`) and registers. Two send patterns on a channel: **`makeRequest(command) → FoxRequest`** (`:253`,
one-shot request/response) and **`openCircuit(command, metadata) → FoxCircuit`** (`:267`, bidirectional streaming
— e.g. subscription/file transfer).

## §513.3 — Remote ops: `rpc()` = the Fox face of `niagaraRpc` `[CERT]`

```
BFoxProxySession.java:90-91
  <R> Optional<R> rpc(BOrd ord, String methodName, Object... args)
      → getConnection().getChannels().getSysChannel().niagaraRpc(ord, methodName, args)
```

`[CERT]` **This is the Fox-transport client face of the same `niagaraRpc` verb** whose server side is
`@NiagaraRpc` ([B507]) and whose BOX-transport client face is `serverSideCall` ([B512]) — it routes through the
**"sys" channel**, not BOX. So the `TransportType.fox`/`TransportType.box` an `@NiagaraRpc` method declares
([B507]) map exactly to: fox → `BFoxProxySession.rpc` (this block) / box → BOX `serverSideCall` ([B512]).
`[INFER]`: there is no `get(BOrd)`/`set(BOrd,val)` on `BFoxProxySession` — remote slot read/write flows through
`BOrd.resolve(session)` (the ORD system, [B5]) which uses the sys channel internally; the exact `BSysChannel`
read/write commands are a recorded sub-item (not blocking).

## §513.4 — Auth `[CERT]`

Credentials (`BIUserCredentials`/`BClientCredentials`) are set on the session/connection; `connect()` runs the
Fox login handshake, delegating the SCRAM challenge/response to the shared `AuthenticationClient` — the same SCRAM
primitive as [B134] (Fox wire) and the scheme framework [B510]. `useFoxs=true` wraps the socket in TLS (Foxs).

## §513.5 — Who uses it `[CERT]`

The public API is NOT driver-locked. Consumers beyond `BNiagaraStation` ([B414]-[B420], the primary internal
user): `maxpro-rt` `Helper` casts the session to `BFoxProxySession` and calls `.rpc(nvr.getNavOrd(), RPCMethodCall)`
(`Helper.java:31,35`) — an OEM driver using the public RPC API; `exportTags` `BSupervisorJoinJob`/`BSubordinateJoinJob`
use `BFoxClientConnection` directly for the join handshake (`BSupervisorJoinJob.java:181`); `aaphp` UI uses
`BOrd.toSession()` to obtain a `BFoxSession`. `[INFER]`: any module can open a Fox client session — it is a
first-class SDK surface, not an implementation detail of the Niagara driver.

## §513.6 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | public BFoxProxySession.make(host,port,useFoxs,creds); impl BFoxSession; not driver-locked | `[CERT]` | `BFoxProxySession.java:56` | PASS |
| 2 | BFoxClientConnection: port 1911, useFoxs=false, engageNoRetry/engageRetry shared-connection interest, connect() | `[CERT]` | `BFoxClientConnection.java:135,136,350` | PASS |
| 3 | BFoxChannelRegistry typed getSysChannel/getFileChannel/getUserChannel + get(name,type); channel makeRequest/openCircuit | `[CERT]` | `BFoxChannelRegistry.java:46-58`; `BFoxChannel.java:253,267` | PASS |
| 4 | rpc(ord,method,args) → sysChannel.niagaraRpc = the Fox face of the niagaraRpc verb (box face = B512) | `[CERT]` | `BFoxProxySession.java:90-91` | PASS |
| 5 | auth via credentials → connect → SCRAM (AuthenticationClient); useFoxs=TLS | `[CERT]` | `BFoxClientConnection` connect; credentialStore | PASS |
| 6 | users beyond BNiagaraStation: maxpro Helper.rpc, exportTags join jobs (BFoxClientConnection) | `[CERT]` | `Helper.java:31,35`; `BSupervisorJoinJob.java:181` | PASS |

**Tally:** 6 claims — all `[CERT]` load-bearing + 2 `[INFER]` (ORD-resolve read/write path, first-class-SDK-surface).
Block TYPE = **EVIDENCE**; API6 CLOSED. REMITTANCE-checked genuine vs B134/B414-420/B471. All load-bearing tokens
re-verified inline.

## §513.7 — Connections & focus status

- **Rides [B134]'s wire** (this is the Java client that speaks it) and is the public API `BNiagaraStation`
  ([B414]-[B420]) uses internally — but reachable directly by any module.
- **Unifies the RPC picture:** the `niagaraRpc` verb has a server side (`@NiagaraRpc`, [B507]) and TWO client
  transports — **Fox `BFoxProxySession.rpc`** (this block) and **BOX `serverSideCall`** ([B512]) — exactly the
  `TransportType.fox`/`box` enum values of [B507]. `web` (the third) is the HTTP `/rpc` servlet [B507].
- **Recorded-not-seeded sub-item:** `BSysChannel` remote read/write/invoke command internals (reachable from this
  API; a natural depth follow-up, not blocking).
- **Focus status:** `apis` 7/8 (API1–API7 closed; API6=this). NEXT = API8 (BQL/NEQL call + over-HTTP) — the last.
