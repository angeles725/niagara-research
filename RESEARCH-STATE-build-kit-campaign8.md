# RESEARCH-STATE — build-kit-campaign8

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 4
gaps_closed: 4
known_gaps: 4
investigable_open: 0
requires_execution_open: 3
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
| STEP-UP — critical-write step-up/re-auth ("internal login") for -ux dashboards | **B803** | CLOSED [CERT] | Verdict: Niagara ships NO core step-up (CONFIRM_REQUIRED=UX-only); electronicSignature is the only true credential step-up (credential-in-action-arg + server verify). Clean servlet path = webOp.getUser()→re-verify via user's auth scheme; SAML users CANNOT be re-verified mid-session (SSO/IdP-redirect). CSRF correction: real token x-niagara-csrfToken. §803.6 = copy-ready design sketch. 2 requires-exec gaps (B803-G1 SAML block, B803-G2 gauth TOTP). One block (did not split). |
| B — history-extension authoring (BHistoryExt family, HistoryService, config + rollover) | **B804** | CLOSED [CERT] | Verdict: a BHistoryExt IS a point extension (extends BPointExtension); two modes Interval (timed, 15min/1s-min) vs COV (change-of-value); BHistoryConfig sets capacity (500 default) + fullPolicy (roll/stop, config-defaults roll); ONE ext per logged slot (chihuahua BChiDatalogger rule). Consumer = chihuahua (ColdRoomPan/DashboardPan don't log history; chihuahua does). 1 bounded gap (B804-G1 time-based capacity). |

**ALL 4 ASSIGNED LANES CLOSED** (C=B801, A=B802, STEP-UP=B803, B=B804) + timer-defense addendum (B775 §775.6). companero's resource-budget (B806/B807) + console census (B800) are separate campaign-8 lanes.

## Open gaps (requires-execution)
- **B801-G1 — CLOSED [CERT-live]**: the `<= 0` throw is reached at station runtime — PANCCADIA console shows it 5× from BDefrostController.armTrigger (console_backup_260903_1858.txt; B801 §801.4).
- **B802-G1** (open): fox connect-time AUTH + session-liveness (reconnect, dead-session detection) named, not traced. The in-station module-agnostic contract (B802 §802.1-3) is fully [CERT]; only the cross-station distributed residue is open.
- **B803-G1** (open): confirm the SAML mid-session re-auth BLOCK on a live station (does scheme.login throw / return no Subject for a SAML user?). Hierarchy says SSO-redirect [CERT]; the runtime block is [INFER].
- **B803-G2** (open): whether gauth's BPasswordCache.validate accepts a TOTP token as the mid-session "password" or needs the gauth login-module path — affects single-field vs TOTP step-up.

## Coverage / dedupe
- C extends [B775] (timer authoring) + feeds [B787] (timer lint); the decode/delegation is REMITTANCE to Clock.java/EngineManager.java code.
- ADDENDUM (lead directive, no new block): B775 §775.6 — the `BTimeTrigger` timer self-heal exemplar (one idempotent `init()` from all hooks + `clockChanged→init()` self-heal + exposed `nextTrigger`) for the kit's 8-layer timer defense-in-depth checklist; MAE4-G1 partially closed (independent dead-ticket monitor still find-zero). Kit cite `[ev: corpus B775]`.
- A extends [B778] (custom service + server-side same-space Subscriber); the cross-module + distributed hop is the new residue.
- 7 of 8 originally-briefed dimensions were already blocked (see campaign8-research-candidates.md); this focus pursues only the genuine gaps.
