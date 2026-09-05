# RESEARCH-STATE — build-kit-campaign8

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 2
gaps_closed: 2
known_gaps: 4
investigable_open: 2
requires_execution_open: 1
blocked_open: 0
<!-- /research-state.v1 -->

focus: build-kit-campaign8
status: ACTIVE. Lead directive 2026-09-05 (user reversed the C7 stop). Plan = campaign8-research-candidates.md candidates 1+2 + one urgent kit-doctrine cite. C DONE; A (inter-module comms) NEXT; B (history-extension) after.
seeded_from: lead directive 2026-09-05 + campaign8-research-candidates.md (AUDIT-FIRST ranking)
seeded_on: 2026-09-05
gaps_total: 3 (C cite · A inter-module comms · B history-extension authoring)
blocks_written: B801 (Clock delay/period floor)
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering); range reserved B801–B805
block_scope_note: B800=companero (console-log census); B801–B805 reserved for this focus

## Gaps

| Gap | Block | Status | Note |
|---|---|---|---|
| C — [CERT] cite: Clock timer delay/period floor (`<= 0` → IllegalArgumentException) for a new non-positive-delay lint | **B801** | CLOSED [CERT] | Clock.java:72-85 delegate → EngineManager.java:327/346/366/388. Corrected the ":497" hypothesis. Extends B775 (pointer added). Kit cite `[ev: corpus B801]`. |
| A — inter-module communication patterns (cross-module BLink, Sys.getService + SPI registry, cross-module Subscriber, fox/box station hop) | **B802** | CLOSED [CERT] | Verdict: comms is MODULE-AGNOSTIC within a station (BLink by ORD, Subscriber by ComponentSlotMap/BComponentSpace, service discovery by type-spec string); only real boundaries = compile-time Type dep + fox: remote hop. Extends B778. 1 requires-exec gap (B802-G1 fox auth/liveness). |
| STEP-UP — critical-write step-up/re-auth ("internal login") for -ux dashboards (electronicsignature surface, servlet re-auth path, fresh short-TTL token bound to session+user+target ORD) | B803+ | OPEN (investigable) — NEXT | lead directive (user priority), before B. AUDIT-FIRST: NOT greenfield — dedupe vs B350-356 (e-sign), B558-566 (RBAC), B763 (canWrite), B776 (action protection), B791 (web). Residue = the step-up token pattern. |
| B — history-extension authoring (BHistoryExt family, HistoryService, config + rollover) with ColdRoomPan/DashboardPan as consumers | B804+ | OPEN (investigable) | MED candidate; not covered by B772-B791. Now after STEP-UP. |

## Open gaps (requires-execution)
- **B801-G1 — CLOSED [CERT-live]**: the `<= 0` throw is reached at station runtime — PANCCADIA console shows it 5× from BDefrostController.armTrigger (console_backup_260903_1858.txt; B801 §801.4).
- **B802-G1** (open): fox connect-time AUTH + session-liveness (reconnect, dead-session detection) named, not traced. The in-station module-agnostic contract (B802 §802.1-3) is fully [CERT]; only the cross-station distributed residue is open.

## Coverage / dedupe
- C extends [B775] (timer authoring) + feeds [B787] (timer lint); the decode/delegation is REMITTANCE to Clock.java/EngineManager.java code.
- ADDENDUM (lead directive, no new block): B775 §775.6 — the `BTimeTrigger` timer self-heal exemplar (one idempotent `init()` from all hooks + `clockChanged→init()` self-heal + exposed `nextTrigger`) for the kit's 8-layer timer defense-in-depth checklist; MAE4-G1 partially closed (independent dead-ticket monitor still find-zero). Kit cite `[ev: corpus B775]`.
- A extends [B778] (custom service + server-side same-space Subscriber); the cross-module + distributed hop is the new residue.
- 7 of 8 originally-briefed dimensions were already blocked (see campaign8-research-candidates.md); this focus pursues only the genuine gaps.
