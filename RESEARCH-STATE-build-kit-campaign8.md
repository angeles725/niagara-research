# RESEARCH-STATE — build-kit-campaign8

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 1
gaps_closed: 1
known_gaps: 3
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
| A — inter-module communication patterns (cross-module BLink, Sys.getService/lookupService + SPI registry, cross-module Subscriber, fox/box station hop) | B802+ | OPEN (investigable) — NEXT | HIGH candidate; tractable read-only from organized/ (fox, service registry). Extends B778 (same-space subscription only). |
| B — history-extension authoring (BHistoryExt family, HistoryService, config + rollover) with ColdRoomPan/DashboardPan as consumers | B803+ | OPEN (investigable) | MED candidate; not covered by B772-B791. |

## Open gaps (requires-execution)
- **B801-G1**: confirm the `<= 0` throw is reached at station runtime (fires only for a running component) — live smoke-test on a seeded `BRelTime.make(0)` schedule. Static contract is [CERT].

## Coverage / dedupe
- C extends [B775] (timer authoring) + feeds [B787] (timer lint); the decode/delegation is REMITTANCE to Clock.java/EngineManager.java code.
- A extends [B778] (custom service + server-side same-space Subscriber); the cross-module + distributed hop is the new residue.
- 7 of 8 originally-briefed dimensions were already blocked (see campaign8-research-candidates.md); this focus pursues only the genuine gaps.
