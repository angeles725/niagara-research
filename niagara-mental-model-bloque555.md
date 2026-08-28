# Block 555 — Reference: a multi-user live dashboard on N4 — the recommended API stack, the read/write split, and the multi-user pitfalls (API11, applied guide)

**Session**: 2026-08-28
**Focus**: `apis` (gap API11 — multi-user live-dashboard architecture; operator-requested reference)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: SYNTHESIS/REFERENCE over the corpus's transport, RPC, control, and Reflow blocks. No new
decompilation — every recommendation traces to a cited evidence block.

**Scope**: an actionable architecture guide for the operator's use case — a browser dashboard where SEVERAL
users watch N4 data change in real time (and may command it). It consolidates the transports ([Block 553]),
the BOX wire ([Block 512]/[Block 554]), the writable-point arbitration ([Block 536]), the RPC surface
([Block 507]), and the Reflow precedent ([Blocks 59]/[217]/[221]) into one design. Block TYPE = DESIGN/APPLIED
reference — a high cross-reference density is expected and correct.

---

## 555.1 The recommended stack

For a browser, multi-user, live dashboard on a stock N4 station:

- **READ (live data) → BOX subscription over WebSocket** ([Block 512]/[Block 554]). The native substrate; each
  user's browser opens a BOX session, subscribes the dashboard's ORDs, and receives unsolicited change
  envelopes. No extra server code.
- **WRITE (a user commands a point) → `@NiagaraRpc`** ([Block 507]) or BOX `callssc`. Request/response with
  4-layer auth. The **16-level priority array** ([Block 536]) arbitrates when multiple users command the same
  point — no application-level data lock needed.
- **TRENDS → `BHistory`** (interval/COV, [Block 552]) via the BOX `history` channel or `webChart`.
- **Custom SPA?** → the Reflow WebSocket pattern ([Block 59]/[217]/[221]) if you need your own channel protocol
  + shared-editable state.

## 555.2 The READ path — BOX subscription fan-out [CERT-synthesis]

Per user: browser → `BBoxServlet /box` → a per-user server session (`ssession` channel, secure-random id,
[Block 512]). The session's `ComponentSpaceSessionHandler` takes `sub`/`unsub` of ORDs. Server-side, one
**`ProxyBroker`** attaches to the component space and receives every `SyncOp`; on change it wakes a
**`BrokerPoller`** that pushes to the subscribed sessions.

Two multi-user properties matter:
- **FAN-OUT**: one `sub` to a point serves ALL watchers of that point — the broker does not re-read per user, so
  N users watching the same dashboard is cheap on the station side ([Block 512]).
- **COALESCING**: the `BrokerPoller` rate-limits to **one push burst per 2 s** per session ([Block 512]); the
  MUX layer batches within `box.mux.minDelay`…`maxDelay` (0–200 ms) and caps an envelope at 1 MB
  ([Block 554]). So a dashboard gets a smooth ≤~0.5 Hz update, not a firehose — tune the poller/mux if you need
  faster.

Each live change arrives as an **`unsolicited` (`'u'`) BOX fragment** ([Block 554] §554.6) — the client demuxes
and applies it without polling.

## 555.3 The WRITE path — arbitration, not locking [CERT-synthesis]

When a user changes a value (setpoint, override), send `@NiagaraRpc` (`POST /rpc/{method}/{ord}`, [Block 507])
or a BOX `serverSideCall`. The station does NOT need an application lock for concurrent commanders: a
**writable control point's 16-level priority array** ([Block 536]) arbitrates — each source writes its level,
highest-valid wins, and an operator override goes to level 8 (manual). For the DASHBOARD's own shared editable
STATE (layout, widget config — not the live point), use a control token (Reflow's approach, §555.6), because
that state has no priority-array semantics.

## 555.4 The alternatives — when to use each [CERT-synthesis]

| API | Best for | Multi-user notes | Block |
|-----|----------|------------------|-------|
| **BOX / WebSocket** (recommended) | browser live data on stock N4 | per-user session; ProxyBroker fan-out; honors component RBAC; 2 s coalescing | [B512]/[B554] |
| **oBIX server Watch** | standards-based / third-party clients | per-user Watch (30 s lease, user-isolated) BUT **no per-object read ACL** — any authed user reads the whole tree; poll-oriented | [B509] |
| **Custom WebSocket (Reflow pattern)** | your own OEM SPA, shared editable state | proven multi-user: channel pub/sub + JSON-Patch broadcast + control token | [B59]/[B217]/[B221] |
| **Fox** | thick client / station↔station | typed channels, not a browser transport | [B513] |
| **@NiagaraRpc** | commands / writes (not the stream) | 4-layer auth; pair with BOX for reads | [B507] |
| **SSE** | — | **NOT available in N4** (proven-absent, [B553]) | — |

## 555.5 Multi-user considerations (the checklist) [CERT-synthesis]

1. **Auth per user** — each BOX/oBIX session authenticates via SCRAM ("N4 Digest", [Block 508]/[Block 457]).
2. **RBAC on reads** — BOX honors the component RBAC; **oBIX does NOT** have a per-object read ACL ([Block 509])
   — do not expose oBIX for a role-restricted dashboard.
3. **Concurrent commands to a point** — arbitrated by the 16-level priority array ([Block 536]); no lock. Decide
   which level the dashboard writes (typically a mid level, or level 8 for a manual override).
4. **Concurrent edits to dashboard CONFIG** — needs a control model; Reflow uses a **control token** (one editor
   at a time, others read + can request control, [Block 221]) with JSON-Patch delta broadcast.
5. **Update cadence** — the `BrokerPoller` 2 s burst + MUX 0–200 ms batching set the perceived refresh; tune via
   `box.mux.*` ([Block 554]) only if needed.
6. **Trends** — read `BHistory` (interval or COV, [Block 552]); don't stream every sample point-by-point.
7. **Session limits** — BOX sessions expire (~90 s idle, [Block 512]); the client must keep the session alive.

## 555.6 The Reflow reference implementation [CERT-synthesis]

Reflow ([Block 59]/[217]/[221]) is a WORKING multi-user live dashboard on N4 — the proof the pattern is viable:
- WebSocket **channel pub/sub** ([Block 140]).
- **JSON-Patch (RFC-6902) delta broadcast**: server applies the patch (`zjsonpatch`), rolls back on failure,
  then broadcasts `{type:"delta"}` to every client so all see the change ([Block 221] §221.2).
- **Control token** for concurrent editing: one socket holds control, others read + `control-request`
  ([Block 221]).
- **PITFALL to fix if you copy it**: Reflow's write path uses `doPrivileged` WITHOUT an auth check
  ([Block 221] §security) — add the permission check.

## 555.7 Pitfalls (from the security blocks) [CERT-synthesis]

- **oBIX exposes the whole tree** with no read allowlist ([Block 509]) — a real leak for a multi-tenant board.
- **Reflow `doPrivileged`-without-auth** on writes ([Block 221]) — don't inherit it.
- **1 MB BOX envelope cap** ([Block 554]) — a very large dashboard payload fragments; keep per-update deltas small.
- **2 s coalescing** ([Block 512]) — the dashboard is near-real-time (~0.5 Hz), not instantaneous; set
  expectations or tune.

## 555.8 Self-verify

| # | Claim | Marker | Basis | Verdict |
|---|-------|--------|-------|---------|
| 1 | Recommended: BOX/WebSocket read + @NiagaraRpc write + priority-array arbitration | [CERT-synthesis] | B512/B554/B507/B536 | cross-ref ✓ |
| 2 | ProxyBroker fan-out (one sub, many watchers) + BrokerPoller 2 s coalescing | [CERT-synthesis] | B512 | cross-ref ✓ |
| 3 | Concurrent point commands arbitrated by 16-level priority array, no lock | [CERT-synthesis] | B536 | cross-ref ✓ |
| 4 | oBIX has no per-object read ACL (multi-tenant caveat) | [CERT-synthesis] | B509 | cross-ref ✓ |
| 5 | Reflow = proven multi-user pattern (channel pub/sub + JSON-Patch + control token) | [CERT-synthesis] | B59/B217/B221 | cross-ref ✓ |
| 6 | SSE not available (use BOX/WebSocket) | [CERT-synthesis] | B553 | cross-ref ✓ |

**Marker tally**: [CERT-synthesis] ×6 · [INFER] ×0. Block TYPE = DESIGN/APPLIED reference — introduces no new
claims; each recommendation traces to a cited evidence block. High cross-reference density is correct here.

## Connections

- **[Block 512]/[Block 554]** — BOX subscription + fragment/mux (the read path).
- **[Block 507]** — `@NiagaraRpc` (the write path); **[Block 536]** — priority-array arbitration.
- **[Block 509]** — oBIX (the standards alternative + its read-ACL caveat).
- **[Block 59]/[217]/[221]** — the Reflow multi-user reference implementation.
- **[Block 552]** — `BHistory` for trends; **[Block 553]** — the transport map + SSE absence.

## Open gaps (this block)

- None investigable. This is an applied reference over covered subsystems. A concrete PoC (a working multi-user
  dashboard) would be a §19 build/PoC phase, not a read-only gap — recorded, not opened.
