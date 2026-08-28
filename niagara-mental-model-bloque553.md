# Block 553 — N4 real-time / push transports consolidated (API10): Fox, BOX (HTTP-poll v1 + WebSocket-push v2), WebSocket — and Server-Sent Events PROVEN-ABSENT

**Session**: 2026-08-28
**Focus**: `apis` (gap API10 — the real-time/push transport map; operator-requested)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: SYNTHESIS over existing transport blocks + a fresh PROVEN-ABSENCE measurement for SSE (RE-MEASURED
by two independent methods).
**Primary basis**: [Block 134]/[Block 513] (Fox), [Block 512] (BOX), [Block 59]/[Block 138]/[Block 140]
(WebSocket), plus a direct grep of `organized/` and `niagara-help/` for SSE.

**Scope**: the operator asked to investigate "APIs, WebSocket, Server-Sent-Events, etc." Most is already
covered by dedicated blocks (REMITTANCE — cited, not re-derived). This block CONSOLIDATES N4's real-time/push
transports into one map and adds one NEW finding: **N4 does not use Server-Sent Events at all**. It also serves
as the answer to "the database" (fully covered — §553.5).

---

## 553.1 The four real-time transports N4 actually uses [CERT / synthesis]

| Transport | Endpoint / substrate | Direction | Block |
|-----------|----------------------|-----------|-------|
| **Fox** | `BFoxProxySession` (:4911 native TCP) | station↔station / thick client, bidirectional push over named typed channels | [B134]/[B513] |
| **BOX** | `BBoxServlet` at `/box` (JSON frames) | browser↔station live subscription; **v1 = HTTP long-poll, v2 = WebSocket push** (coexist) | [B512] |
| **WebSocket** | `BoxWebSocketServlet` (BOX v2) + Reflow `SocketServlet`/`BReflowWebSocketAcceptor` | browser↔station full-duplex push | [B512]/[B59]/[B140] |
| **BACnet COV** | change-of-value subscriptions on field objects | device→station push | (BACnet blocks) |

The unifying substrate for BROWSER real-time is **BOX** ([B512]): a `ProxyBroker`-backed subscribe/event model,
JSON (not binary) frames, where the same wire carries `@NiagaraRpc`'s `box` transport and the WebSocket layer.
`BServerSession` emits **unsolicited** server-push envelopes (`BoxEnvelope.unsolicited(...)`) — that is the
push. WebSocket (BOX v2) is the upgrade of the HTTP-poll v1 path, not a separate stack.

## 553.2 Server-Sent Events — PROVEN ABSENT [CERT]

N4 does **not** implement HTTP Server-Sent Events (SSE). Measured two independent ways:
- **`text/event-stream`** (the definitive SSE response content-type): **0 occurrences** across all of
  `organized/` `[CERT]` (`rg -rc text/event-stream organized/` = 0).
- **Browser `EventSource`**: 0 real hits. The 98 grep matches for "EventSource" are ALL
  `javax.baja.event.BEventSource` `[CERT]` — the station's INTERNAL event-routing service
  (`BEventService`/`BEventSource`/`BIEventRoutable`, a `BComponent` event chain), which has nothing to do with
  HTTP SSE. No `new EventSource(...)` client exists in the ux/JS layer.
- **niagara-help**: "No guide text matches 'server-sent events'"; "No results for 'event-stream'" `[CERT-doc]`.

**Why**: N4 predates/sidesteps SSE by design — it already had Fox (native push since AX) and built BOX +
WebSocket for the browser. SSE (unidirectional server→client over a plain HTTP response) offers nothing BOX/
WebSocket don't, and N4's model is bidirectional (subscribe + command), which SSE cannot do alone. So the
absence is architectural, not an omission. **NAMING CAUTION**: `BEventService`/`BEventSource` in N4 is the
in-station event bus (alarm/event routing, [Block 552] alarm side) — do NOT mistake it for HTTP SSE.

## 553.3 How to choose (operator reference) [INFER — from the transport contracts]

- **Thick client / station-to-station** → Fox ([B513]): typed channels, `rpc()`, shared-connection interest.
- **Browser live data (BajaScript/bajaux, Workbench Web)** → BOX ([B512]): subscribe a component, receive
  unsolicited change envelopes; v2 upgrades to WebSocket automatically.
- **Custom browser real-time (an OEM SPA like Reflow)** → the WebSocket layer ([B59]/[B140]): `SocketServlet`
  + acceptor + channel pub/sub.
- **Field-device push** → the driver's native subscription (BACnet COV, etc.).
- **Server→browser one-way stream (SSE)** → NOT available; use BOX/WebSocket instead.

## 553.4 APIs — already fully covered (REMITTANCE map) [CERT]

The `apis` focus is STOPPED (9/9). For the operator's "investigate APIs":
- **API1 [B507]** `@NiagaraRpc` server-side RPC (`POST /rpc/{method}/{ord}`, JSON, web/box/fox transports,
  4-layer auth).
- **API2 [B508]** web-tier ORD-over-HTTP routing + SCRAM ("Digest") handshake.
- **API3 [B509]** oBIX server (`/obix`, whole-tree publish, per-user Watch).
- **API4 [B512]** BOX protocol wire.
- **API5 [B510]** `BAuthenticationScheme` SPI.
- **API6 [B513]** Fox CLIENT API.
- **API7 [B511]** `BJob`/`BJobService`.
- **API8 [B514]** BQL-over-HTTP.
- Plus Fox [B134], servlets/CSRF [B58], hx [B433], SCRAM login [B457], BajaScript/BOX [B36]/[B42], Baja SDK
  [B4], ORD [B5].

## 553.5 The DATABASE — already fully covered (REMITTANCE map) [CERT]

The `database` focus is STOPPED (11/11 + synthesis [B413]). For "investigate the database":
- **[B402]** BOG save trigger + dirty-flag (`BStationSaveJob`/`BBogSpace`).
- **[B408]** `BComponentSpace` lifecycle; **[B406]** BQL execution (unindexed DFS walk, TOP N, no SKIP).
- **[B405]** BOG version migration; **[B411]** boot-time crash recovery.
- **[B403]/[B407]** external RDBMS history export (`rdb-rt`, high-watermark idempotency); **[B404]** alarmOrion
  ORM backend; **[B409]** embedded HSQLDB; **[B410]** `.hdb` retention/rollover; **[B412]** `orion-rt` ORM.
- **[B413]** SYNTHESIS: two persistence worlds (BOG file-space vs external SQL), an unindexed query engine, a
  triple SQL stack, pervasive absence of integrity guarantees.

So "the database" needs no new investigation — it is a closed focus; deepen only a named sub-topic on request.

## 553.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | N4 push transports = Fox, BOX (poll v1 + WS v2), WebSocket, BACnet COV | [CERT] | B134/B513, B512, B59/B140 | cross-ref ✓ |
| 2 | `text/event-stream` = 0 in organized/ | [CERT] | rg -rc = 0 | token-checked ✓ |
| 3 | All "EventSource" hits = javax.baja.event.BEventSource (internal event bus), not HTTP SSE | [CERT] | grep of the 98 hits | token-checked ✓ |
| 4 | niagara-help has no SSE / event-stream content | [CERT-doc] | guide-search/find = 0 | token-checked ✓ |
| 5 | BOX emits unsolicited server-push envelopes (the push) | [CERT] | BServerSession BoxEnvelope.unsolicited | sweep-cited ([B512]) |
| 6 | apis focus 9/9 + database focus 11/11 already cover the operator's ask | [CERT] | RESEARCH-STATE-apis/database | token-checked ✓ |

**Marker tally**: [CERT] ×5 · [CERT-doc] ×1 · [INFER] ×1 (the choose-guide). Block TYPE = SYNTHESIS/REFERENCE +
one PROVEN-ABSENCE. The absence was RE-MEASURED by two independent methods (MIME type + client API) plus docs,
per the RE-MEASURE-A-DRAMATIC-NEGATIVE rule.

## Connections

- **[Block 512]** (BOX) / **[Block 513]** (Fox client) / **[Block 59]** (WebSocket) — the transports this maps.
- **[Block 552]** — `BEventService`/`BEventSource` is the INTERNAL event bus (alarm routing), the naming trap
  §553.2 warns about — NOT SSE.
- **`database` focus** [B402–B413] / **`apis` focus** [B507–B516] — the covered subsystems (§553.4–5).

## Open gaps (this block)

- None investigable. SSE is proven-absent. Database + APIs are closed focuses. A specific deepening (e.g. the
  BOX fragment/mux protocol detail, or a database sub-topic) can be opened on operator request.
