# RESEARCH-STATE — build-kit-campaign7

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 2
gaps_closed: 2
known_gaps: 6
investigable_open: 0
requires_execution_open: 4
blocked_open: 0
<!-- /research-state.v1 -->

focus: build-kit-campaign7
status: STOPPED (investigable=0). Two targeted research deliverables for Campaign-7 kit work, both closed in one pass; §18 retros filed (pending). Remaining gaps are all requires-execution (need a live station / a built module pair).
seeded_from: lead directive 2026-09-05 (campaign-7 research = candidates 1+2 from campaign7-research-candidates.md, ranked by kit value)
seeded_on: 2026-09-05
gaps_total: 2 investigable (both closed) + 3 requires-execution children
blocks_written: B795 (MM3 decision table), B796 (-ux write-surface exemplar)
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Deliverables (both closed)

| Gap | Block | Verdict | Kit implication |
|---|---|---|---|
| C7-1 — mechanize B754's slot-change survival matrix into a machine-readable SAFE/LOSSY/OUTAGE classifier | **B795** | CLOSED [CERT via B754 + INFER fail-safe] | §795.4 CSV = the verbatim source for `schema-risk.sh` (MM3, niagara-tools #46); worst-cell verdict, unknown→OUTAGE fail-safe. Retro: `…-campaign7-mm3-decision-table-retro.md` |
| C7-2 — capture OUR proven `-ux` write-surface seam as the kit exemplar (no clean Tridium one exists, B791 THIN) | **B796** | CLOSED [CERT by file:line, §20 document-mode] | `types/dashboard.md` points at B796 (DashboardPan-ux) for the pure `route()→RouteAction` seam + the 5 write gates (scored 4/5). Retro: `…-campaign7-ux-write-surface-exemplar-retro.md` |

## Coverage
- **B795** mechanizes [B754] (module versioning + upgrade safety) — REMITTANCE for the decode mechanism; adds the
  machine-readable table + the fail-safe collapse + ext/package-move [INFER] rows. Version-floor half = [B784].
- **B796** consolidates [B762]/[B763] (the `module-ux-testing-and-write-surface` focus) into one citable exemplar;
  it EXTENDS that focus rather than reopening it. RBAC/CSRF framing = REMITTANCE [B752]/[B58]/[B507].

## Open gaps (all requires-execution)
- **B795-G1**: confirm `retype_complex` reverts-to-default vs orphans on a LIVE station with a seeded `.bog`.
- **B795-G2**: `package_move` with an unchanged registered type name — is it truly SAFE? Needs a built before/after pair.
- **B796-G1**: gate 4 (per-Ord lock + HTTP 423) unimplemented in DashboardPan-ux; exemplar becomes 5/5 after issue #49 lands.
  (Shares the same station backlog as B793-G1 / the requires-execution issue #50.)
- **B795-G3**: `swap_slot_kind` (name reused for a different slot kind, property→action) precise verdict — OUTAGE vs LOSSY.
  Surfaced by companero's B799 `unknown_kind` fixture (55d0519d2); added as an explicit OUTAGE-fail-safe row, station-verify pending.
