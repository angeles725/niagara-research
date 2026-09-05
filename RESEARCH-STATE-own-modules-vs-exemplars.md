# RESEARCH-STATE — focus: own-modules-vs-exemplars (BOOTSTRAPPING)

> Multi-focus corpus (METHODOLOGY §16). CONFORMANCE AUDIT of our three real modules against the exemplar
> authoring idioms documented in the `module-authoring-exemplars` census (B772–B785) + the -ux write-surface
> (B763) + the point-extension/child-tree/service SPIs. Reference exemplar = chihuahua (the only production
> module). NEW focus (the `own-modules-audit` one-module-per-block manifest shape does not fit a
> dimension-clustered conformance audit that cites the idiom blocks). Angle: exemplar conformance 2026-09.
>
> **Subjects (READ-ONLY source):** ColdRoomPan (`Cliente/Leon-Guanjuato/Paccadia/ColdRoomPan`), CompPan
> (`.../Compresores/CompPan`), DashboardPan (`.../Dashboard/DashboardPan`); reference chihuahua
> (`Cliente/Honeywell/MX60/chihuahua`). One block per DIMENSION cluster where the audit finds something.
>
> **Output per finding:** `[CERT]` file:line in the module · the exemplar block it violates/satisfies · severity ·
> TWO routings — (1) client punch-list (module change, out of kit scope), (2) kit implication, especially a
> candidate BITING CHECK for `verify-module.sh` / a new lint that QA can RED-first. Cite the audit corpus
> (own-modules-audit, B760 punch-list) — do not re-derive. Blocks from B786; investigador1 sole writer.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 0
gaps_closed: 1
known_gaps: 7
investigable_open: 6
requires_execution_open: 0
blocked_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: own-modules-vs-exemplars
status: active (1/7 audited: OMV1 conforms/no-block) — angle: exemplar conformance 2026-09; investigador1 sole writer
seeded_from: the module-authoring-exemplars census (B772–B785) + B763 + B760 punch-list, one gap per dimension cluster
seeded_on: 2026-09-05
gaps_total: 7 (OMV1–OMV7, one per dimension; a block is written only where the audit FINDS something)
blocks_written: none yet; next = B786
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Gap-backlog (prioritized by biting-check value + safety)

| Priority | Gap | Exemplar idiom | Subjects | Status |
|---|---|---|---|---|
| high | **OMV1 actions/protection** — which write/state-changing `@NiagaraAction` lack `flags=Flags.OPERATOR` (admin-only by accident) or, worse, which config/dangerous actions ARE operator-invokable; any `doPrivileged` (AP-27) | B776 | ColdRoomPan/CompPan/DashboardPan-rt vs chihuahua | **CONFORMS — NO BLOCK** (audited, clean, no padding). All our `@NiagaraAction`s are `Flags.HIDDEN` engine callbacks (ColdRoomPan 8, CompPan 2); DashboardPan has ZERO actions (writes via the -ux servlet, B763); write surfaces = OPERATOR-flagged properties. No doPrivileged/AP-27 anywhere. chihuahua baseline: 3 actions all correctly admin. NEGATIVE biting-check finding for retro: a "non-HIDDEN @NiagaraAction lacking Flags.OPERATOR → FAIL" lint is TOO NOISY (would false-positive on every legit admin action incl. chihuahua's 3) — the write/command-vs-read distinction is not statically decidable; recommend an ADVISORY review-line only, not a hard fail. |
| high | **OMV2 timers/watchdogs** — `Clock.schedule`/`schedulePeriodically` without a kept `Ticket` (can't cancel/re-arm); no re-arm on `changed`; a configurable interval not honored; any threshold monitor | B775, B729/B730 | all 3 rt vs chihuahua | pending → B787 |
| high | **OMV4 palette/lexicon** — empty/scaffold palette, missing lexicon keys, DUP bare keys (no prefix); run `slot-coverage.sh` dup-keys once PR5b merges | B780, B759 | all 3 vs chihuahua | pending → B788 |
| med | **OMV3 extensions/children** — container-by-cardinality misuse, missing legality vetoes, retype hazards | B772/B779 | all 3 rt | pending → B789 (if found) |
| med | **OMV7 write-surface** — DashboardPan-ux servlet vs the B763 5 gates (OPERATOR_WRITE, CSRF, SERVICE_ORD pin, per-Ord lock/423, audit) | B763 | DashboardPan-ux (+chihuahua-ux ref) | pending → B790 (may cite B763 §763.6, mostly done) |
| low | **OMV5 services/ORD/subscription** — service registration correctness; any polling that should be a server-side Subscriber | B778 | all 3 | pending → B791 (if found) |
| low | **OMV6 background-work** — any long op on the engine thread that should be a BSimpleJob | B774 | all 3 | pending → B792 (if found) |

## Stop control (METHODOLOGY §8)
- STOP when each dimension is audited; a block is written ONLY where a real finding exists (a clean dimension = a
  one-line "conforms, no block" note, not a padded block). §18 retro at STOP with the consolidated client punch-list
  + the kit biting-check proposals for QA to RED-first.

## Iteration history

| Iter | Gap | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|
| seed | idiom-conformance backlog from B772–B785 + B763 + B760 | — | inline (investigador1) | OMV1–OMV7 seeded, one per dimension |
| 1 | OMV1 actions/protection audit (vs B776) — ALL 3 modules CLEAN (HIDDEN callbacks only / zero actions; write via OPERATOR properties + -ux servlet); no doPrivileged. NO BLOCK (no-padding). Negative biting-check finding: no-OPERATOR→FAIL lint too noisy → advisory only | — (no block) | yes · Explore audit + inline grep-verify | none; retro carries the advisory-lint note |
