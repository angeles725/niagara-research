# Block 516 — `apis` API9: the Fox `BSysChannel` command set — what the "sys" channel actually carries (summary / space enumeration / nav-event push / `stationCall` / `niagaraRpc`), and the key structural fact that it holds **no slot read/write primitives** — those bootstrap a separate `BBrokerChannel` (the ProxyBroker/BOG sync of [B512], over Fox)

> **Focus:** `apis`, gap **API9** — the recorded-not-seeded sub-item from [B513 §513.7] (the Fox client API noted
> `BSysChannel`'s command internals were unopened). READ-ONLY, decompiled; no run. Markers §3.
> **REMITTANCE-checked GENUINE:** [B134] is the Fox WIRE (frame opcodes); [B513] the client facade; the sys-channel
> COMMAND SET is uncovered.
> **Sources:** FUENTE 3 — `organized/fox/fox-rt/decompiled/com/tridium/fox/sys/BSysChannel.java`. FUENTE 1 —
> [B513] (Fox client), [B507] (@NiagaraRpc), [B512] (BOX ProxyBroker sync), [B134] (wire). Evidence delegated to a
> `sonnet` sweep; ALL load-bearing file:line RE-VERIFIED inline.

## §516.1 — The command set `[CERT]`

`BSysChannel.process()` dispatches on interned String constants (`:151-175`). The complete "sys" command set:

| Command | Direction | Purpose |
|---|---|---|
| `summary` (`:154`) | client→server sync | post-connect station metadata |
| `listLocalSpaces` (`:163`) | client→server sync | enumerate accessible nav spaces |
| `makeBrokerChannel` (`:166`) | client→server sync | **bootstrap a slot-sync channel** (§516.2) |
| `subNavEvents`/`unsubNavEvents` (`:169`) + `navEvent` (`:151`) | sub + server push | nav-tree change subscription |
| `stationCall` (`:157`) | client→server sync | generic opaque infra RPC (byte[] payload) |
| `stationEvent` (`:160`) | server push | station fault / type-mixin broadcast |
| `niagaraRpc` (`:175`) | client→server sync | `@NiagaraRpc` invocation over Fox (§516.3) |

Only `stationCall` may be **routed to an intermediate station hop** (`allowRoutingRequestToReachableStation()`
`:136`); the rest are peer-local.

## §516.2 — No slot ops on sys → a separate `BBrokerChannel` `[CERT]`

`[CERT negative]` **The sys channel carries NO `readSlot`/`writeSlot`/`invokeAction`/`subscribe` command** (grep = 0).
This corrects the natural assumption that Fox remote reads/writes ride the sys channel. Instead:
`makeBrokerChannel(space)` (`:334`,`:348`) takes a component-space ORD, and the server registers a **`BBrokerChannel`**
(`com.tridium.fox.sys.broker`, `:343`) on the channel registry — named `station` (the station space, `:321`),
`virt_<handle>` (virtual, `:354`), or `gw_<handle>` (gateway, `:356`). **From then on all slot reads, writes,
subscriptions, and action invokes travel on the BBrokerChannel** via the ProxyBroker/BOG sync protocol — the SAME
`javax.baja.sync.ProxyBroker` machinery [B512] documented for BOX, here over Fox. `[INFER]`: so `BOrd.resolve(session)`
([B513]) lands on a BBrokerChannel, not on sys; the sys channel is a control/metadata plane, the broker channel is
the data plane. Fox and BOX share one sync engine over two transports.

## §516.3 — `niagaraRpc` over Fox `[CERT]`

`niagaraRpc(ord, method, args)` (`:526+`) is the sys channel's only "call a remote method" primitive. Client:
version-gates remote ≥ 4.1; checks `isWhitelistedLegacyRpc` (`:536`); modern path encodes each arg via
`NiagaraRpcUtil.convertFromCollection` into a JSON array (`:542`); sends `ord` (relativized,
`ord.relativizeToSession()` `:546`), `methodName`, `args` (JSON string — double-encoded), `legacyRpc` flag.
**Server** (`:563+`): `NiagaraRpcUtil.rpc(TransportType.fox, isSecure, remoteAddr, ord, methodName, args,
sessionContext)` (`:578`) — **the exact same dispatch [B507] documents**, Fox just supplies `TransportType.fox`.
Legacy RPCs substitute `ValueDoc` for the value portion; the envelope stays JSON. Server exceptions serialize to
`{m, st:[{cn,mn,fn,ln}]}`. `[INFER]`: this closes the RPC picture — `niagaraRpc` reaches its [B507] handler over all
three transports (web `/rpc`, BOX `serverSideCall`, Fox `BSysChannel.niagaraRpc`), the [B515] unification made
concrete on the Fox leg.

## §516.4 — The other sys services `[CERT]`

- **`summary`** (`:187+`): the post-connect metadata exchange — `stationName` (`:207`), `host`/`hostModel`,
  **`hostId`** (`Nre.getHostId()`, `:212`), **`niagaraVersion`** (`:213`), java/os version, locale, and
  **`currentTime`** (`BAbsTime.make()`, `:217`). Station time is here, not a dedicated frame.
- **`listLocalSpaces`** (`:288+`): enumerates `BLocalHost.getNavChildren()` filtered to `BSpace`, **permission-checked**
  (operator read per space) → drives which proxy spaces the client instantiates.
- **`stationCall`** (`:221+`): generic infra RPC `Station.remoteCall(id, payload)` with a byte[] blob — used for e.g.
  `module.version` queries, NOT slot access.
- **nav events**: `subNavEvents` registers a `BNavRoot` listener; `navEvent` pushes relative-ORD + type code
  (a=added/v=removed/r=renamed/o=reordered/p=replaced/c=recategorized).
- **`stationEvent`**: server push for `stationFault` and type `mixIns`. `[CERT]` Session keepalive is on
  `BUserChannel.resetSessionTimeout()`, NOT the sys channel.

## §516.5 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | sys command set: summary/listLocalSpaces/makeBrokerChannel/nav-events/stationCall/stationEvent/niagaraRpc | `[CERT]` | `BSysChannel.java:151-175` | PASS |
| 2 | NO slot read/write/invoke on sys; slot ops via makeBrokerChannel → separate BBrokerChannel (station/virt_/gw_) | `[CERT]`/`[CERT neg]` | grep=0; `:334,343,348,354,356` | PASS |
| 3 | BBrokerChannel runs ProxyBroker/BOG sync (=B512 engine) over Fox | `[CERT]`+`[INFER]` | `broker.BBrokerChannel` import :58 | PASS |
| 4 | niagaraRpc over Fox → NiagaraRpcUtil.rpc(TransportType.fox,...) = same B507 handler; JSON args, ord relativized | `[CERT]` | `BSysChannel.java:536,542,546,578` | PASS |
| 5 | summary carries hostId/niagaraVersion/currentTime; keepalive on BUserChannel not sys | `[CERT]` | `:207,212,213,217` | PASS |

**Tally:** 5 claims — all `[CERT]`/`[CERT negative]` load-bearing + 2 `[INFER]` (control-plane vs data-plane,
RPC-picture-closure). Block TYPE = **EVIDENCE**; API9 CLOSED. REMITTANCE-checked genuine vs [B134]/[B513]. All
load-bearing tokens re-verified inline.

## §516.6 — Connections & focus status

- **Closes [B513]'s recorded sub-item.** The Fox client picture is now complete: `BFoxProxySession` ([B513]) →
  sys channel (this block) for control/RPC + a bootstrapped `BBrokerChannel` for slot data.
- **Unifies the sync engine:** Fox slot sync = `ProxyBroker`/BOG = the same engine as BOX ([B512]) — two transports,
  one component-space sync protocol ([B408]).
- **Completes the RPC verb** ([B515] §515.3): the Fox leg of `niagaraRpc` is `BSysChannel.niagaraRpc` → the [B507]
  handler with `TransportType.fox`.
- **Focus status:** `apis` reopened for one recorded sub-item, now 9 gaps closed (API1–API9). Remaining candidate:
  the data-access-cookbook REMITTANCE verdict (API10, sweep running) — likely REMITTANCE. `basicDriver` (FD11) is a
  separate framework-drivers candidate under investigation.
