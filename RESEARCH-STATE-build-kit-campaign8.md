# RESEARCH-STATE — build-kit-campaign8

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 17
gaps_closed: 17
known_gaps: 17
investigable_open: 0
requires_execution_open: 6
blocked_open: 0
<!-- /research-state.v1 -->

focus: build-kit-campaign8
status: ALL 17 CLOSED (17/17). Campaign 8 (user reversed the C7 stop, 2026-09-05). Multi-session focus: investigador1 (Opus) + companero. B816 (write-path/overlap testing) added on a later user mandate. Open items are requires-execution / bounded gaps only (see below); both consolidated §18 fold retros filed (mine extended to D12).
seeded_from: lead directives 2026-09-05 + campaign8-research-candidates.md (AUDIT-FIRST ranking)
seeded_on: 2026-09-05
block_range: B800–B816 (shared-global numbering; next-free = B817)
owners: investigador1 = B801-B805, B809, B810, B816 + B775 §775.6 addendum · companero = B800, B806-B808, B811-B815
envelope_note: requires_execution_open counts the investigador1 lanes' station-required gaps (B802-G1, B803-G1, B803-G2, B809-G2, B810-G1); companero's B806-B815 track their own in-block gaps, not re-counted here.

## Coverage — all campaign-8 blocks (owner · topic · status)

| Block | Owner | Topic | Status |
|---|---|---|---|
| B800 | companero | Console-log census (triage-console.sh contract; TRIPLE attribution; EN+ES locale/mojibake) — closes B795-G1/#50 [CERT-live] | CLOSED [CERT] |
| B801 | investigador1 | Clock timer delay/period floor (`<=0` → IllegalArgumentException); [CERT-live] G1 (PANCCADIA 5×) | CLOSED [CERT-live] |
| B802 | investigador1 | Inter-module comms is module-agnostic within a station; only boundaries = Type dep + fox: hop | CLOSED [CERT] |
| B803 | investigador1 | Step-up/re-auth for critical writes; SAML can't re-auth mid-session; CSRF-token correction; design sketch | CLOSED [CERT] |
| B804 | investigador1 | History-extension authoring (BHistoryExt = point ext; Interval vs COV; capacity+fullPolicy; one ext/slot) | CLOSED [CERT] |
| B805 | investigador1 | RT control-logic exemplars (PID/anti-windup, deadband, D-latch, protection one-bit trace, NO ODE/matrix, flowchart) | CLOSED [CERT] |
| B806 | companero | Resource budget for a JACE/station — oversaturation + module-logic viability | CLOSED [CERT] |
| B807 | companero | N4 build pipeline + module versioning — task matrix, station-lock copy, reload code path | CLOSED [CERT] |
| B808 | companero | Who watches the logic — feedback surfaces traced from BEvaporatorUnit fault to operator | CLOSED [CERT] |
| B809 | investigador1 | Tridium -wb authoring conventions (5 checks: THREAD1/AGENT1/WB-LEX1/SCAFFOLD1/DEP1) + doctrine | CLOSED [CERT] |
| B810 | investigador1 | Driver-module authoring; write-to-DOWN-device is SILENTLY DROPPED; [CERT-live] PANCCADIA fallback gap (#49) | CLOSED [CERT/live] |
| B811 | companero | Station snapshot automation — copy a running station without mounting the filesystem | CLOSED [CERT] |
| B812 | companero | Heartbeat/liveness watchdog pattern — author-built independent monitor (builds on B775/B801/B805) | CLOSED [INFER/CERT] |
| B813 | companero | UX servlet authoring conventions — registration/routing/lifecycle, JSON+facet+CSRF (cites B796/B803/B762/B763) | CLOSED [CERT] |
| B814 | companero | Authoring a module tag dictionary — nav/search/hierarchy addressable components | CLOSED [CERT] |
| B815 | companero | Station-level / component-lifecycle test authoring (cites B805 §805.8 BTest); §815.10 logs the read-the-client-tree lesson (first pass read a same-named research exemplar) | CLOSED [CERT] (correction 85664208f) |

| B816 | investigador1 | Write-path & overlap testing — threading/link-override (dashboard write to a LINK-TARGET lands then is silently overwritten; set() serializes only the raw store); overlap cases incl. the live armTrigger Clock.schedule(0) crash; write-path test matrix + lints | CLOSED [CERT/live] |

Addendum (no new block): **B775 §775.6** — the BTimeTrigger timer self-heal exemplar (investigador1), folded for the timer defense-in-depth checklist.

## Fold status
- investigador1 lanes → consolidated §18 retro `retros/2026-09-05-…-campaign8-consolidated-fold-retro.md` (D1–D11), incl. B800 D7/D8 + the B795-G1 closure meta-delta.
- companero lanes → his consolidated §18 retro (B806/B807/B808/B811-B815 + the §800.3 Clock-not-java.util.concurrent doctrine), with pointers to the investigador1 retro for B800 D7/D8 + B795-G1. Clean split, no double-folds.

## Open gaps (requires-execution — investigador1 lanes)
- **B801-G1** — CLOSED [CERT-live] (PANCCADIA 5×).
- **B795-G1** — CLOSED [CERT-live] via B800 §800.8 (PANCCADIA boot failure = the schema-risk OUTAGE class; issue #50).
- **B802-G1** (open): fox connect-time auth / session liveness.
- **B803-G1/G2** (open): SAML mid-session re-auth block; gauth TOTP-as-password.
- **B809-G2** (open): chihuahua-wb EDT-freeze live confirm.
- **B810-G1** (open): live confirm write-drop-on-DOWN + writeOnUp recovery (pairs with the PANCCADIA fallback gap, #49).
- **B816-G1** (open): live confirm the servlet-set()↔engine-execute() callback interleave (per-slot store is [CERT] serialized; the callback interleave is [CERT] possible, effect on our slots wants a smoke test). CLIENT residue: fix armTrigger `Math.max(delayMs,1L)` + facet MIN≥1s; add matrix tests for the dashboard-writable slots.
- Bounded (not requires-execution): B804-G1, B805-G1/G2, B809-G1, B810-G2.
- companero's B806-B815 carry their own in-block gaps.

## Campaign-9 candidate (lead-flagged)
- A tested pure-Java "protection latch" seam scaffold (set/reset priority, first-out capture, trip-reason, operator reset, BAlarmSourceExt hookup) — B805 §805.3 established Tridium ships NO SR latch; the scaffold is the residue.
