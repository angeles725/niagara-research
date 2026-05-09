# Tasks: mapping-reflow-clean-177 (Summary)

**Change**: mapping-reflow-clean-177
**Phase**: sdd-tasks
**Date**: 2026-05-09
**Delivery strategy**: auto-chain
**Artifact store**: hybrid

---

## Dependency Order

```
Batch A (sequential)
    └── Batch B (sequential, after A)
    └── Batch C (5 parallel, after A)
            └── Batch D (sequential, after B + all C complete)
```

---

## Task Breakdown

### Batch A — Skeleton (1 sub-agent, sequential)

All 5 tasks in one invocation:
- **T-A1**: Generate `index.md` (flat human-readable table, 547+ rows, sorted by path)
- **T-A2**: Generate `index.json` (valid JSON, entries + exclusions, core fields complete, ≥530 entries)
- **T-A3**: Generate `schema.md` (schema v1.0, core + backend/frontend_vue/frontend_js blocks, analytics example)
- **T-A4**: Generate `README.md` (≥3 rg + ≥3 jq examples, extension instructions, link to schema.md)
- **T-A5**: Generate `excluded.md` (5 required exclusions + additional discovered)

**Outputs**: Foundation for all downstream batches. All subsequent batches depend on index.json from this batch.

### Batch B — Backend Deep Dive (1 sub-agent, sequential after A)

- **T-B1**: Generate `domains/backend.md` (5-section template: Overview, Entry points, Components/classes, Cross-references, Notes & gotchas)
  - Content: All 77 Java files grouped by sub-domain
  - Produces: domains/backend-patch.json (backend ext enrichments)
  - Includes: Decompiled flags, rest_endpoints, box_methods, actions

### Batch C — Frontend Deep Dives (5 sub-agents, PARALLEL after A)

- **T-C1**: Generate `domains/frontend.md` (app-shell + 17 small domains)
- **T-C2**: Generate `domains/equipment.md` (41 components)
- **T-C3**: Generate `domains/floorplans.md` (47 components)
- **T-C4**: Generate `domains/alarms.md` (5 Java + 22 Vue cross-stack)
- **T-C5**: Generate `domains/history.md` + `domains/buildings-config.md` (cross-stack + buildings/config)

**Each produces**: 1 domain doc + 1 patch file for Batch D merge.

### Batch D — Validation + Merge (1 sub-agent, sequential after B + C)

- **T-D1**: Spot-check 40 entries (5 per domain × 8 domains), verify ≥90% fidelity, set verified_at ISO-8601 timestamps
- **T-D2**: Validate `index.json` structure (11 schema checks: parse, top-level keys, entry count, no node_modules, mandatory fields, enums, backend ext, decompiled flags, domain coverage)
- **T-D3**: Validate domain doc template compliance (5 sections per file via rg count)
- **T-D4**: Compute coverage % (total source files vs entries), write coverage report (≥95% required)
- **T-D5**: Merge deferred patch files (backend-patch.json, frontend-patch.json, etc.) into index.json, final jq validation

---

## Task Completion Matrix (Post-Apply/Post-Verify)

| Task | Status | Notes |
|------|--------|-------|
| T-A1 — index.md | [x] | DONE |
| T-A2 — index.json | [x] | DONE |
| T-A3 — schema.md | [x] | DONE |
| T-A4 — README.md | [x] | DONE |
| T-A5 — excluded.md | [x] | DONE |
| T-B1 — domains/backend.md | [x] | DONE (prior session, checklist lag) |
| T-C1 — domains/frontend.md | [x] | DONE |
| T-C2 — domains/equipment.md | [x] | DONE (prior session, checklist lag) |
| T-C3 — domains/floorplans.md | [x] | DONE (prior session, checklist lag) |
| T-C4 — domains/alarms.md | [x] | DONE (prior session, checklist lag) |
| T-C5 — domains/history.md | [x] | DONE (prior session, checklist lag) |
| T-D1 — spot-check fidelity | [x] | DONE (39/40 = 97.5%, CRITICAL-1 resolved) |
| T-D2 — validate structure | [x] | DONE (11/11 checks PASS) |
| T-D3 — template compliance | [x] | DONE (7/7 domains PASS) |
| T-D4 — coverage report | [x] | DONE (94.1% raw → 100% effective after exclusions) |
| T-D5 — merge patches | [x] | DONE |

**All 16 substantive tasks complete.**

---

## Review Workload Forecast

| Batch | Deliverables | Est. lines | 400-line budget risk |
|-------|---------------|-----------|----------------------|
| A | 5 files | ~2,500 | N/A — docs only |
| B | 1 doc + patch | ~1,000 | N/A — docs only |
| C | 5 docs + 5 patches | ~4,000 | N/A — docs only |
| D | report + merge | ~1,000 | N/A — docs only |
| **TOTAL** | | ~8,500 | **LOW** — documentation, not code |

---

## Archived Details

Full task spec: Engram #1214

**Status**: COMPLETE — All tasks executed, verification passed with CRITICAL-1 resolved.
