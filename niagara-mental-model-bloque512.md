# Block 512 — `apis` API4: the BOX protocol wire — N4's live-subscription substrate (`BBoxServlet` at `/box`, **JSON** frames not binary, a `ProxyBroker`-backed sub/event model, HTTP-poll v1 + WebSocket-push v2 that coexist, and the wire that `@NiagaraRpc`'s `box` transport and the WebSocket layer both ride on)

> **Focus:** `apis`, gap **API4** — the BOX wire BajaScript uses browser↔station, named-in-passing in
> [B19]/[B36]/[B42] but never documented as a protocol. READ-ONLY, decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/box/box-rt/decompiled/com/tridium/box/…` + `WEB-INF/web.xml`. FUENTE 1 —
> [B507] (`@NiagaraRpc` `box` transport rides this), [B59] (WebSocket, client PoV), [B36]/[B42] (BajaScript
> client), [B508] (web-tier auth), [B408] (component space / sync). Evidence delegated to a `sonnet` sweep; ALL
> load-bearing file:line RE-VERIFIED inline.

## §512.1 — Mount + service `[CERT]`

`BBoxServlet extends BWebServlet` with `servletName = "box"` (`BBoxServlet.java:48-51`) → mounts at **`/box`**;
its parent must be `BBoxService` — the top-level service owning the BOX infrastructure (servlet + WebSocket
acceptor + a Fox acceptor + the channels). A `BoxWebSocketServlet` maps `/*` in the box `web.xml` — **the same
BOX protocol over WebSocket**. `[CERT]` **BOX is NOT binary** — it is JSON (values use a BSON-like
`BsonEncoderPlugin`); the only byte-level framing is the fragment protocol (§512.2). This corrects the common
"BOX = binary wire" assumption the corpus never verified.

## §512.2 — Wire format: JSON frames + a fragment protocol `[CERT]`

`BBoxService.handleRequest()` branches on the POST body's first byte: `'F'` (0x46) → **fragment protocol**,
else → a **JSON frame**. JSON frame envelope (fields = `BBoxService` constants `:131-143`):

```
{ "p":"box", "v":"2.3", "n":<seq>, "c":<clientEnvType>,
  "m":[ { "t":"rt", "c":"<channel>", "k":"<key>", "r":<reqNum>, "b":<body> } ] }
```

`"t"`: `rt`=request, `rp`=response-OK, `e`=error, **`u`=unsolicited push**. **Fragment** wire
(`BoxEnvelope.toBoxFragmentBytes`, `:240`): `F;<version>;<serverSessionId>;<envelopeId>;<count>;<index>;<u|r>;<payload>`
— multiplexes large messages; reassembled by `BoxEnvelopeDemux`. `MAX_ENVELOPE_SIZE` default **1 MB**
(`BoxEnvelope.java:50`). Versions `1`/`2`/latest **`2.3`** (`BoxOp.java:42`).

## §512.3 — Channels + operations `[CERT]`

Frame `"c"` names a `BBoxChannel`; built-ins: `sys`, `reg`, `timeZone`, `unit`, `history`, `alarm`, **`ssession`**,
`ord`, `transfer`. The **`ssession`** channel (`BServerSessionChannel`) keys: `make` (create server session →
secure-random sessionId, `:71`), `del`, `pollchgs` (HTTP change-poll, `:79`), `makessc`/`removessc` (create/destroy
a session-component handler, `:83`), `callssc` (dispatch to a handler, `:91`). A **ComponentSpace session handler**
(`BComponentSpaceSessionHandler`) then serves the real work: **`sub`/`unsub`** (subscribe ORDs), `loadSlots`/
`loadSlotPath`/`loadRoot`, `invokeAction`, **`serverSideCall`** (= the `@NiagaraRpc` box route, [B507]), `syncTo`
(client mutations → `SyncBuffer.commit`), `makeLink`/`checkLink`, `navChildren`, `impliedTags`/`impliedRelations`,
`save`.

## §512.4 — Subscription & event model: `ProxyBroker` + `BrokerPoller` `[CERT]`

`sub` (`:217`) resolves each ORD to a `BComponent` and calls `broker.subscribeOp(comp, 0, isVirtual)` on a
**`javax.baja.sync.ProxyBroker`** (`:194`,`:317`) — the server-side engine that attaches to the component space and
receives every `SyncOp` change. On a change, `newSyncOp()` flags `hasNewEvent` and wakes a **`BrokerPoller`**
daemon: it debounces ~20 ms, then delivers, then sleeps `POLL_INTERVAL` (default **2000 ms**,
`BServerSession.java:109`) — server-side rate-limiting to one push burst per 2 s. Two transports:
- **HTTP v1 (poll):** client drives `ssession.pollchgs`; server returns the accumulated `SyncBuffer` (BSON-JSON).
- **WebSocket v2+ (push):** the poller pushes **unsolicited `"t":"u"`** frames (`ssession/evs`) over the WS — the
  same code path [B59] saw from the client side.

## §512.5 — Session lifecycle & auth `[CERT]`/`[INFER]`

Server sessions live in a `ConcurrentHashMap<sessionId, BServerSession>` (each also a live `BComponent`); expiry
default **90 s** (`serverSessionExpiryTime`, `:110`), renewed on every request. `[CERT]` **cross-user guard:**
`getServerSession()` verifies the requesting user's username matches the session's — cross-user access throws
`ServerSessionException`. **Auth is delegated to the N4 web tier** ([B508] SCRAM/Basic/cookie); `BBoxServlet` adds
none. Session `make` requires an HTTP/HTTPS (or Workbench) context. **WebSocket** auth: `BoxWebSocket` runs under
`doPrivileged`, pulls the `Subject` from the existing HTTP session via `SessionManager` ([B508]) and executes in it.
`[INFER]` **CSRF:** no BOX-level token — the WS path enforces **same-origin** (`isCrossOrigin` checks `Origin`
scheme/host/port), the HTTP path relies on the web-tier session cookie + servlet auth.

## §512.6 — Relation to the other APIs `[CERT]`

| Path | Transport | Subscription |
|---|---|---|
| BOX v1 | `POST /box` JSON | client polls (`pollchgs`) |
| BOX v2+ | WebSocket `/box/*` JSON | server push (`BrokerPoller`) |
| BOX Fox | `BFoxBoxAcceptor` TCP | same frames (Workbench) |
| `@NiagaraRpc` `box` ([B507]) | via BOX `serverSideCall`/`callssc` | — (RPC layered ON BOX) |
| oBIX ([B509]) | `/obix` REST | none |

`[INFER]` **BOX is the live-value substrate of N4's browser/Workbench clients** — `@NiagaraRpc`'s `box` transport
and the WebSocket layer both ride it; it is NOT legacy (v1-poll and v2-WS coexist in one `BBoxService`, WS is just
BOX's newer delivery). This is the wire under [B36]/[B42]'s BajaScript subscriber.

## §512.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | BBoxServlet=BWebServlet at /box; BBoxService owns servlet+WS+Fox acceptors; JSON not binary | `[CERT]` | `BBoxServlet.java:48-51` | PASS |
| 2 | JSON frame {p,v,n,c,m} t=rt/rp/e/u; fragment protocol first-byte F; MAX 1MB; version 2.3 | `[CERT]` | `BBoxService.java:131-143`; `BoxEnvelope.java:50,240`; `BoxOp.java:42` | PASS |
| 3 | ssession keys make/del/pollchgs/makessc/callssc; ComponentSpace handler sub/serverSideCall/syncTo | `[CERT]` | `BServerSessionChannel.java:71-91`; `BComponentSpaceSessionHandler.java:217` | PASS |
| 4 | subscribe via ProxyBroker.subscribeOp; BrokerPoller 2000ms; HTTP-poll v1 + WS-push v2 (unsolicited "u") | `[CERT]` | `BComponentSpaceSessionHandler.java:194,317`; `BServerSession.java:109` | PASS |
| 5 | session registry + 90s expiry + cross-user username guard; auth delegated to web tier; WS via SessionManager | `[CERT]`+`[INFER]` | `BServerSession.java:110`; SessionManager wiring | PASS |
| 6 | @NiagaraRpc box transport rides BOX (serverSideCall); WS is BOX push not separate; not legacy | `[CERT]` | `callssc`/`serverSideCall`; version constants | PASS |

**Tally:** 6 claims — all `[CERT]` load-bearing + `[INFER]` (CSRF-via-same-origin, BOX-is-substrate). Block TYPE =
**EVIDENCE**; API4 CLOSED. All load-bearing tokens re-verified inline; corrects the "BOX=binary" assumption.

## §512.8 — Connections & focus status

- **Names the wire under [B36]/[B42]** (BajaScript client) and **[B59]** (WebSocket) — B59 saw the WS transport
  client-side; this is the server BOX protocol it carries. **[B507]**'s `@NiagaraRpc` `box` transport is the RPC
  convention over BOX `serverSideCall`.
- Subscription engine is `javax.baja.sync.ProxyBroker` — the same component-space sync machinery [B408] documents;
  BOX is its network projection.
- Auth reuses [B508] (web tier + SessionManager); the cross-user session guard + same-origin WS check feed
  [B398]/[B490] SEC.
- **Focus status:** `apis` 6/8 (API1–API5, API7, API4 closed). NEXT = API6 (Fox client — source-locate), then API8.
