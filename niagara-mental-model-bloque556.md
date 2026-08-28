# Block 556 — Cross-focus integration map: the end-to-end BMS-dashboard stack on N4 — how control programming, the database, history/alarms, the APIs, and the dashboard connect around the control-point spine

**Session**: 2026-08-28
**Focus**: `apis` (cross-focus integration reference; operator-requested navigable map)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: SYNTHESIS/REFERENCE across five focuses. No new decompilation — a navigable index tying the
corpus's subsystem blocks into one end-to-end architecture.

**Scope**: the operator asked how the database, APIs, control programming, history, and dashboard fit
TOGETHER. This is the compass: the layered data flow of a multi-user live BMS dashboard on N4, with the
control point as the organizing spine, every layer citing its evidence block. Block TYPE = DESIGN/APPLIED
cross-focus reference — high cross-reference density is correct and intended.

---

## 556.1 The organizing principle — the control point is the spine [CERT-synthesis]

Everything hangs off the **control point** ([Block 536]). Read this map as: the point is written BY control
logic (arbitrated by its priority array), it is recorded/alarmed BY its extensions, it is persisted IN the
database, it is published BY the APIs, and it is watched/commanded BY the dashboard. A user's command travels
back to the same point through the priority array — no separate lock. Hold that and every layer below has an
obvious place.

## 556.2 The five layers (read down = data flow to the user)

**Layer 0 — Field ↔ Point.** A driver `proxyExt` bridges a physical sensor/actuator to a control point
([Block 536] §536.8, proxyExt runs first); the write path to the device is [Block 544] (N4 priority level →
BACnet WriteProperty priority, or a single register for Modbus).

**Layer 1 — Control programming (writes the point).** Four independent ecosystems compute logic and write the
point via its 16-level priority array ([Block 549] four-ecosystem synthesis):
- kitControl (event-driven JVM) — the function-block catalog [Block 537], the RULES [Block 538], the PID
  [Block 539].
- clHVAC/Eagle (roster JVM) — HVAC sequences [Block 540]/[Block 551].
- honeywellFunctionBlocks (scan JVM) [Block 542]; honIrmControl (scan on IRM hardware) [Block 546].
- Custom **Java 8** logic — the `program` module (compiled `javac`, sandboxed) [Block 541].
- Fail-safe behavior of all this: [Block 543] (the 6 default-unsafe gaps + recommendations).

**Layer 2 — Persistence (stores the point + config).** The BOG component space saves on a dirty-flag
([Block 402]/[Block 408]); BQL queries it (unindexed DFS) [Block 406]; migration [Block 405]; crash recovery
[Block 411]. Synthesis: [Block 413] (two persistence worlds, no integrity guarantees).

**Layer 3 — History + Alarm (observes the point).** Point EXTENSIONS ([Block 552]): `BIntervalHistoryExt`
(timer, default 15 min) / `BCovHistoryExt` (change-of-value) record `out` to `BHistory`; `BAlarmSourceExt` +
the offnormal-algorithm family raise records to `AlarmService` (NOTIFICATION-ONLY — not an interlock). External
reporting: RDBMS history export `rdb-rt` [Block 403]/[Block 407], alarmOrion [Block 404], embedded HSQLDB
[Block 409].

**Layer 4 — API transport (publishes the point).** Real-time map [Block 553]: **BOX/WebSocket** (per-user
session, ProxyBroker fan-out, `unsolicited 'u'` push) [Block 512]/[Block 554]; **@NiagaraRpc** for commands
[Block 507]; **oBIX** for standards clients [Block 509]; **Fox** station↔station [Block 513]. **SSE is
proven-absent** [Block 553].

**Layer 5 — Dashboard (the user).** Multi-user browser SPA reference [Block 555]: BOX subscription for live
reads, @NiagaraRpc for writes, `BHistory` for trends, Reflow's control-token pattern for concurrent config
edits [Block 59]/[Block 217]/[Block 221].

## 556.3 The two directions [CERT-synthesis]

**READ (data → user)**: field → proxyExt → point `out` → history/alarm extensions observe it → ProxyBroker sees
the SyncOp → BOX pushes an `unsolicited` envelope → the browser demuxes ([Block 554]) and renders. Fan-out means
N dashboard users watching one point cost one subscription; the BrokerPoller coalesces to ~0.5 Hz.

**WRITE (user → field)**: dashboard → `@NiagaraRpc`/BOX `serverSideCall` → the writable point's priority array
arbitrates this command against all other sources ([Block 536]) → the winning level's value flows through
proxyExt → the driver write ([Block 544]) → the device. Concurrent commanders are arbitrated by PRIORITY, not
serialized by a lock.

## 556.4 The cross-cutting concerns [CERT-synthesis]

- **Security**: per-user SCRAM auth ([Block 508]/[Block 457]); RBAC honored by BOX, NOT by oBIX (whole-tree read
  leak, [Block 509]); module + program signing ([Block 541], `signing-pki` focus); the live security posture
  ([Block 398], `security-audit`).
- **Safety**: control fail-safe is CONFIGURATION-dependent — the 6 default-unsafe gaps ([Block 543]);
  interlocks are separate high-priority writes or embedded frost sub-functions ([Block 551]/[Block 552]), never
  the alarm extension.
- **Data integrity**: the persistence layer has "a pervasive absence of integrity guarantees" ([Block 413]) —
  design the reporting/history layer expecting that.

## 556.5 Coverage map — what to READ vs what to BUILD [CERT-synthesis]

Every layer above is RESEARCHED (read-only) and closed:

| Layer | Focus | State |
|-------|-------|-------|
| Control programming | `kitControl` | stopped (KC1–KC15 + synthesis B549) |
| Persistence | `database` | stopped (B402–B413) |
| History/Alarm | `kitControl` KC15 + subsystems | B552 + alarm/history focuses |
| APIs / transport | `apis` | stopped (B507–B516 + B553/B554/B555) |
| Dashboard architecture | `apis` | B555 (reference) |

What REMAINS is not read-only research — it is a **§19 BUILD/PoC** (assemble a working multi-user dashboard on
the live station: BOX read + RPC write + BHistory trend + a control point) which needs a live station + operator
authorization, exactly like `kitControl`'s KC13-G1. That is the next real advance beyond documentation.

## 556.6 Self-verify

| # | Claim | Marker | Basis | Verdict |
|---|-------|--------|-------|---------|
| 1 | The control point is the integration spine (written/observed/persisted/published/commanded) | [CERT-synthesis] | B536/B544/B552/B512/B555 | cross-ref ✓ |
| 2 | READ path: point→ProxyBroker→BOX unsolicited→browser (fan-out, coalesced) | [CERT-synthesis] | B512/B554/B553 | cross-ref ✓ |
| 3 | WRITE path: dashboard→RPC→priority-array arbitration→proxyExt→driver | [CERT-synthesis] | B507/B536/B544 | cross-ref ✓ |
| 4 | Five layers each map to a closed focus; SSE absent | [CERT-synthesis] | B549/B413/B552/B553 | cross-ref ✓ |
| 5 | Cross-cuts: security (RBAC BOX-yes/oBIX-no), safety (config-dependent), integrity (none) | [CERT-synthesis] | B509/B543/B413 | cross-ref ✓ |
| 6 | Remaining advance = §19 build/PoC (needs live station + auth), not more read-only research | [CERT-synthesis] | scope of covered focuses | reasoned ✓ |

**Marker tally**: [CERT-synthesis] ×6 · [INFER] ×0. Block TYPE = cross-focus DESIGN/APPLIED reference —
introduces no new primary claims; it is a navigable index over cited evidence blocks.

## Connections

- Spans and indexes: `kitControl` (B536–B552 + B549), `database` (B402–B413), `apis` (B507–B516 + B553–B555),
  and the alarm/history/signing-pki/security-audit focuses.
- Companion to **[Block 555]** (the dashboard architecture) — this is the wider system view around it.

## Open gaps (this block)

- None investigable (read-only). The named next advance is a §19 build/PoC of the dashboard on a live station,
  which requires operator authorization — recorded here, not opened as a read-only gap.
