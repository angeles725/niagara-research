# Campaign 8 — research candidates (AUDIT-FIRST, ranked by kit value × read-only tractability)

Author: investigador1 (Opus). Draft seed for campaign-8 explore. Same shape as campaign7-research-candidates.md.
**Read the coverage table FIRST** — most of the dimensions in the campaign-8 brief already have a block from the
campaign-6 census (B772–B791); campaign 8 should NOT re-open them. Only the genuinely uncovered dimensions are ranked.

## AUDIT-FIRST coverage of the briefed dimensions (do NOT re-derive)

| Briefed dimension | Status | Block (cite, don't re-derive) |
|---|---|---|
| watchdog timers | COVERED | B775 (BAbstractMonitor + Clock.schedulePeriodically + Ticket) — MAE4 |
| function / action protection | COVERED | B776 (@NiagaraAction OPERATOR gating, canInvoke, doPrivileged AP-27) — MAE5 |
| palette | COVERED | B780 (module.palette conventions, copy-ready) — MAE9 |
| lists / child containers | COVERED | B779 (frozen vs dynamic vs BFolder, reorder, NO BComponentList, legality) — MAE8 |
| labels (lexicon) | COVERED | B780 + B788 (lexicon prefixing, dup-key hazard, coverage%) |
| groups / relations | COVERED | B781 (categories = no scaffold; relations = relationId not subclass; hierarchy = level-def) — MAE10 |
| scopes | COVERED | B781 (categories/ORD-prefix) + access-control B558–B566 (RBAC scope model) |

→ 7 of the 8 briefed dimensions are already blocked. Campaign 8 cites them; it does not re-author them.

## Genuine campaign-8 candidates (ranked)

1. **[HIGH] Inter-module communication patterns** — the ONE briefed dimension with no block. How a module talks to
   another at runtime: cross-module `BLink`, service discovery (`Sys.getService`/`lookupService` + the SPI registry),
   event/subscription across a module boundary (extends B778's same-space `Subscriber`), and the distributed hop
   (fox/box station-to-station). Tractable read-only from `organized/` (fox, niagaraDriver, the service registry).
   High kit value — "how do my two modules cooperate" is unanswered. [ev: gap vs B778 (same-space only); fox/box corpus]

2. **[MED] History-extension authoring** — how a module collects/exposes history (`BHistoryExt`/`B*HistoryExt`,
   `HistoryService`, config + rollover). A common real authoring need (our ColdRoomPan/DashboardPan would use it) that
   the census (points/analytics/jobs) never covered. Tractable read-only (history module + docSource). [ev: not in B772–B791]

3. **[MED] Tag-dictionary authoring** — defining a module's own semantic tags (`tagdictionary` module, `BTagDictionary`,
   dict registration) so its components are nav/hierarchy/search-addressable. Ties to B781 (hierarchy) + B782 (search)
   but the AUTHOR-side "ship a dictionary" surface is unblocked. Tractable read-only. [ev: not in B772–B791]

4. **[LOW] Heartbeat / liveness watchdog** — B775 covers THRESHOLD monitors; the liveness/heartbeat pattern (detect a
   stalled producer, not a bad value) has NO exemplar in our corpus (named gap). Mostly requires-execution to prove, but
   the framework PATTERN (BAbstractMonitor + Clock heartbeat + stale-timestamp detection) is documentable from primitives.
   Low read-only tractability; defer unless a client needs it. [ev: B775 gap; campaign6-close-research-lessons "no heartbeat exemplar"]

5. **[LOW] Electronic-signature authoring** — the `electronicsignature` module's author surface (gate an action behind a
   signed confirmation). Specialized; only if a client requires e-sign. [ev: not in B772–B791]

Recommendation: Campaign 8 research = candidate 1 (inter-module comms — the real gap the brief points at) + candidate 2
(history-extension authoring — highest-value uncovered generic dimension). Candidates 3–5 are on-need. The rest of the
brief is CITE-not-derive (B775/B776/B779/B780/B781).
