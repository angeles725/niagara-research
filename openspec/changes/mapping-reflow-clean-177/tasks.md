# Tasks: mapping-reflow-clean-177

**Change**: mapping-reflow-clean-177
**Phase**: sdd-tasks
**Date**: 2026-05-09
**Delivery strategy**: auto-chain
**Artifact store**: hybrid

---

## Dependency order

```
Batch A (sequential)
    └── Batch B (sequential, after A)
    └── Batch C (5 parallel, after A)
            └── Batch D (sequential, after B + all C complete)
```

---

## Batch A — Skeleton (1 sub-agent, sequential)

All 5 tasks run in a single sub-agent invocation. The index and support files must
exist before any downstream batch can operate.

**Inputs**: 3 synthesis docs (`REFLOW-ARCHITECTURE-ANALYSIS.md`, `GAP-ANALYSIS.md`,
`NIAGARA-INTEGRATION.md`) + `fd` scan of source repo.
**Output dir**: `docs/mappings/reflow-clean-177/`

- [ ] **T-A1** — Generate `index.md`
  - Output: `docs/mappings/reflow-clean-177/index.md`
  - Content: Flat human-readable Markdown table — one row per in-scope file, sorted
    ascending by `path`. Columns: `path`, `kind`, `domain`, `purpose`, `loc`, `status`.
    Header must include total entry count.
  - Satisfies: REQ-1 (core fields), REQ-4 (dual-form index), REQ-7 (≥95% coverage)
  - Design ref: §2 (file layout), §3 Batch A
  - Constraint: No row may be missing any of the 6 core fields. Table must render
    without broken syntax in a standard Markdown viewer.

- [ ] **T-A2** — Generate `index.json`
  - Output: `docs/mappings/reflow-clean-177/index.json`
  - Content: Valid JSON object with top-level shape:
    `{schema_version, module, source_repo, generated_at, generator, entries[], exclusions[]}`.
    Every entry must carry all core fields from schema §1.1. Extension blocks may be
    minimal stubs at this stage (Batch B/C will enrich them); stubs MUST use correct
    field names so jq queries don't fail.
  - Satisfies: REQ-1, REQ-4, REQ-7, REQ-10 (exclusions[] top-level)
  - Design ref: §1.1, §3 Batch A
  - Constraint: `jq -e .` must exit 0. Entry count ≥530.

- [ ] **T-A3** — Generate `schema.md`
  - Output: `docs/mappings/reflow-clean-177/schema.md`
  - Content: Full schema reference — `schema_version: v1` at top; all core fields
    (id, path, kind, domain, purpose, dependencies, loc, status, source_doc,
    verified_at) with name, type, constraints, and enum values; backend extension
    block (`§1.2`); frontend_vue extension block (`§1.3`); frontend_js extension
    block (`§1.4`); forward-compat rules (`§1.5`); analytics extension example
    (algorithm_type, dag_role, aon_encoded, execution_order, verdict); Excluded Paths
    section with all 5 required entries from REQ-10.
  - Satisfies: REQ-5 (schema documented), REQ-10 (excluded paths)
  - Design ref: §1.1–1.7, §3 Batch A
  - Constraint: Must contain exactly the 5 Excluded Paths rows from REQ-10. Analytics
    example must be clearly marked as a forward-compat extension (not yet live).

- [ ] **T-A4** — Generate `README.md`
  - Output: `docs/mappings/reflow-clean-177/README.md`
  - Content: ≥3 `rg` query examples (find REST endpoints, find decompiled classes,
    find files by domain); ≥3 `jq` query examples (filter by domain, count by kind,
    list persistent store modules); step-by-step extension instructions (add extension
    block, register in schema.md, add entries); pointer/link to `schema.md`.
  - Satisfies: REQ-9 (README usage examples)
  - Design ref: §2, §3 Batch A, risk "Bloques 63-67 no migran"
  - Constraint: All jq examples must be syntactically valid and runnable against the
    real `index.json`. All rg examples must use correct field/pattern syntax.

- [ ] **T-A5** — Generate `excluded.md`
  - Output: `docs/mappings/reflow-clean-177/excluded.md`
  - Content: Dedicated file listing every excluded path with: path pattern, reason
    for exclusion, file count (approximate), and disposition (e.g., catalogued as
    `status: resource` vs fully omitted). Must include the 5 required exclusions from
    REQ-10 plus any additional exclusions discovered during `fd` scan.
  - Satisfies: REQ-10 (excluded paths documented)
  - Design ref: §3 Batch A
  - Constraint: Binary assets (image-library 22 JPG, icons 6 PNG) must note they ARE
    catalogued in index.json with `status: resource` — not fully omitted.

---

## Batch B — Backend deep dive (1 sub-agent, sequential — after Batch A)

Single sub-agent. Reads `index.json` produced by Batch A, enriches all Java entries
with full backend extension blocks, writes `domains/backend.md`.

**Inputs**: `index.json` (Batch A), synthesis docs, source repo Java files (READ-ONLY).
**Output**: `docs/mappings/reflow-clean-177/domains/backend.md` + enriched backend
entries saved as a JSON patch file for Batch D to merge into `index.json`.

- [ ] **T-B1** — Generate `domains/backend.md`
  - Output: `docs/mappings/reflow-clean-177/domains/backend.md`
  - Content: 5-section template (§REQ-6):
    1. **Overview**: 1 paragraph — nmodsreflow-rt (77 Java files), role as service
       container and ORD provider.
    2. **Entry points**: Table with `BReflowService.java` (service container, 26 slots,
       26 actions), `BaseServlet.java` (HTTP REST anchor), `BOrdScheme.java`,
       `BIJavaScript.java` (BajaScript BOX anchor).
    3. **Components / classes**: Full inventory of all 77 Java files grouped by
       sub-domain:
       - service-container (BReflowService, BIReflowService, BReflowConfig…)
       - ord-scheme (BOrdScheme…)
       - http-rest (BaseServlet + all http/responses/ classes — include rest_endpoints)
       - http-websocket (WebSocket handler classes)
       - bajascript-box (commands/ classes — include box_methods)
       - history (Java-side history handlers)
       - alarms (BReflowAlarmCommands + alarm Java classes)
       - sync-config (sync handlers)
       - backups (backup service classes)
       - util (utility/helper classes)
       - ux-widget (nmodsreflow-ux classes — decompiled=true, profile=ux)
       Each class entry: path, bcomponent_type, slots|null, actions[], rest_endpoints[]
       (if http), box_methods[] (if commands), decompiled bool, profile (rt|ux).
    4. **Cross-references**: Links to frontend domains that call backend REST endpoints
       or WebSocket channels (alarms, history, equipment, app-shell).
    5. **Notes & gotchas**: Decompiled classes (BReflowScheme + 3 ux classes) — fidelity
       caveat, GAP-ANALYSIS reference. Any backend entries where source_doc differs
       from REFLOW-ARCHITECTURE-ANALYSIS.
  - Also produces: `domains/backend-patch.json` — a JSON array of `{path, backend_ext}`
    objects for every Java entry (77 entries), to be merged by T-D5.
  - Satisfies: REQ-2 (backend ext mandatory), REQ-6 (5-section template), REQ-8
    (source_doc cross-reference)
  - Design ref: §1.2, §2, §3 Batch B
  - Constraint: All 4 decompiled classes must have `decompiled: true`. All http/responses
    entries must have `rest_endpoints` non-empty. All commands/ entries must have
    `box_methods` non-empty.

---

## Batch C — Frontend deep dives (5 parallel sub-agents — after Batch A)

5 sub-agents run in parallel. Each owns a disjoint path prefix and writes one or more
domain docs. None may edit `index.json` directly — they each produce a patch file for
Batch D to merge.

**Inputs**: `index.json` (Batch A), synthesis docs, source repo Vue/JS files (READ-ONLY).

### C1 — Frontend overview + small domains

- [ ] **T-C1** — Generate `domains/frontend.md`
  - Output: `docs/mappings/reflow-clean-177/domains/frontend.md`
  - Content: 5-section template covering the app shell and all small/misc frontend
    domains that do not warrant their own dedicated file:
    - App Shell (main.js, App.vue, router/index.js)
    - State (Vuex store root + 29 modules — 14 persistent / 15 transient)
    - Cards, Charts, Common, Layout, Navigation, Settings, Pages, Schedules, Weather,
      Maps, Profiles/RBAC, Wizard, Browser, Map, Websocket-UI, Mixins, Plugins, Lib,
      API layer, Views
    - Buildings & config domain entries (buildings/, config/) — grouped here per
      design decision §2; if buildings or config exceed 30 components each after
      inventory, note it in Notes & gotchas and recommend a future split.
    - Entry points table: `main.js`, `App.vue`, `router/index.js`.
    - Cross-references: equipment, floorplans, alarms, history (Batch C siblings).
    - Notes: persistent vs transient store split; Vue 2.7 constraint; mixin count.
  - Also produces: `domains/frontend-patch.json` — frontend_vue/frontend_js ext patch
    for all covered components.
  - Path scope: `reflow-frontend/src/` EXCLUDING `components/equipment/`,
    `components/floorplans/`, `components/alarms/`, `components/history/`.
  - Satisfies: REQ-3 (frontend ext mandatory), REQ-6 (5-section template)
  - Design ref: §1.3, §1.4, §2, §3 Batch C-C1

### C2 — Equipment domain

- [ ] **T-C2** — Generate `domains/equipment.md`
  - Output: `docs/mappings/reflow-clean-177/domains/equipment.md`
  - Content: 5-section template — deep dive into the 41 equipment components.
    - Overview: role of equipment domain (real-time point display, control widgets).
    - Entry points: main equipment route component and parent container.
    - Components / classes: all 41 Vue components with component_dir=equipment,
      props (top 3-5), emits (top 3-5), store_modules, plugins_used, fidelity rating.
    - Cross-references: backend http-rest (REST endpoints consumed), history domain
      (trend widgets), alarms domain (equipment alarm indicators).
    - Notes & gotchas: fidelity ratings from GAP-ANALYSIS; any POOR-rated components.
  - Also produces: `domains/equipment-patch.json`
  - Path scope: `reflow-frontend/src/components/equipment/`
  - Satisfies: REQ-3, REQ-6
  - Design ref: §1.3, §2, §3 Batch C-C2

### C3 — Floorplans domain

- [ ] **T-C3** — Generate `domains/floorplans.md`
  - Output: `docs/mappings/reflow-clean-177/domains/floorplans.md`
  - Content: 5-section template — deep dive into the 47 SVG canvas editor components.
    - Overview: SVG floorplan editor role, canvas architecture.
    - Entry points: root canvas component, toolbar, event bus.
    - Components / classes: all 47 Vue components with frontend_vue ext fields.
    - Cross-references: backend ORD scheme, equipment (asset overlays on floorplan),
      websocket-UI (live updates on canvas).
    - Notes & gotchas: SVG rendering caveats, any components with POOR fidelity.
  - Also produces: `domains/floorplans-patch.json`
  - Path scope: `reflow-frontend/src/components/floorplans/`
  - Satisfies: REQ-3, REQ-6
  - Design ref: §1.3, §2, §3 Batch C-C3

### C4 — Alarms domain

- [ ] **T-C4** — Generate `domains/alarms.md`
  - Output: `docs/mappings/reflow-clean-177/domains/alarms.md`
  - Content: 5-section template — cross-stack deep dive (5 Java + 22 Vue components).
    - Overview: alarm console + source architecture; both backend and frontend
      participation.
    - Entry points: `BReflowAlarmCommands.java` (backend), alarm root Vue component
      (frontend).
    - Components / classes: 5 Java entries (with backend ext, box_methods where
      applicable) + 22 Vue entries (with frontend_vue ext, fidelity).
    - Cross-references: backend bajascript-box (commands/), history domain (alarm
      history), sync-config (alarm config sync).
    - Notes & gotchas: GAP-ANALYSIS fidelity for alarm area; known cross-stack coupling
      points; any decompiled Java alarm classes.
  - Also produces: `domains/alarms-patch.json` (frontend_vue ext for 22 Vue components
    only; Java entries covered by T-B1 patch)
  - Path scope (frontend): `reflow-frontend/src/components/alarms/`
  - Satisfies: REQ-2 (for Java entries referenced), REQ-3, REQ-6, REQ-8
  - Design ref: §1.2, §1.3, §2, §3 Batch C-C4

### C5 — History domain + buildings-config

- [ ] **T-C5** — Generate `domains/history.md`
  - Output: `docs/mappings/reflow-clean-177/domains/history.md`
  - Content: 5-section template — history builder, chart, groups, picker (22 Vue
    components + Java-side history handlers from backend).
    - Overview: history query builder architecture, chart rendering pipeline.
    - Entry points: history root component, history Vuex store module.
    - Components / classes: 22 Vue components with frontend_vue ext; cross-reference
      Java history handlers covered in T-B1.
    - Cross-references: backend history sub-domain (REST endpoints), alarms (alarm
      history), equipment (trend overlays on equipment).
    - Notes & gotchas: fidelity ratings; picker UX complexity notes.
  - Also produces: `domains/history-patch.json`
  - Path scope: `reflow-frontend/src/components/history/`
  - Satisfies: REQ-3, REQ-6
  - Design ref: §1.3, §2, §3 Batch C-C5

  > **Note on buildings-config**: The design (§3 Batch C) groups buildings and config
  > under C5 alongside history. T-C5 sub-agent MUST inventory
  > `reflow-frontend/src/components/buildings/` and `reflow-frontend/src/components/config/`
  > (if they exist as separate directories). If combined file count exceeds 30, produce
  > separate `domains/buildings-config.md`; if under 30, fold the entries into
  > `domains/history.md` under a dedicated sub-section and note the decision in Notes &
  > gotchas. Either way, include these entries in `history-patch.json` (or a separate
  > `buildings-config-patch.json` if the standalone file is produced).

---

## Batch D — Validation + deferred merge (1 sub-agent, sequential — after B + all C)

Runs after Batch B and all 5 Batch C sub-agents are complete. Reads all patch files,
merges them into `index.json`, then runs all validation checks.

**Inputs**: `index.json` (Batch A base), all `*-patch.json` files from B and C,
synthesis docs.

- [ ] **T-D1** — Spot-check 40 stratified entries (fidelity ≥90%)
  - Output: verification notes appended to `docs/mappings/reflow-clean-177/index.json`
    (`verified_at` field on each of the 40 checked entries) + inline report in T-D4.
  - Method: Select 5 entries per domain × 8 domains (backend, frontend, equipment,
    floorplans, alarms, history, buildings-config, util/misc). For each entry: open
    the actual source file (READ-ONLY), compare the `purpose` field against the file's
    primary responsibility. Score PASS (purpose correct) or FAIL (purpose incorrect or
    contradictory). Set `verified_at` ISO-8601 on each inspected entry.
  - Threshold: ≥36/40 (90%) must be PASS. If any domain falls below 90%, flag it for
    re-processing and record which entries failed and why.
  - Satisfies: REQ-7 (spot-check fidelity ≥90%), REQ-8 (verified_at field)
  - Design ref: §4 validation, §3 Batch D

- [ ] **T-D2** — Validate `index.json` structure (11 schema checks)
  - Output: Pass/fail report for each check, written to stdout and embedded in T-D4
    coverage report.
  - Checks:
    1. `jq -e .` exits 0 (parse validity)
    2. Top-level keys present: schema_version, module, source_repo, generated_at,
       generator, entries, exclusions
    3. `entries | length` ≥530
    4. 0 entries with path containing "node_modules"
    5. Every entry has all 6 mandatory core fields non-null non-empty
    6. All `kind` values are in the allowed enum
    7. All `status` values are in the allowed enum
    8. All java-class entries have `backend` ext with `profile` and `decompiled` fields
    9. `decompiled: true` entries = exactly the 4 expected classes
       (BReflowScheme + 3 ux classes)
    10. History domain entries: ≥34 total (≥12 java-class + ≥22 vue-component)
    11. Alarms domain entries: ≥27 total
  - Satisfies: REQ-1, REQ-2, REQ-4, REQ-7
  - Design ref: §4 validation checks

- [ ] **T-D3** — Validate domain doc template compliance (5-section check)
  - Output: Compliance table (domain × section presence) embedded in T-D4 report.
  - Method: For each `domains/*.md` file, run:
    `rg -c '^## (Overview|Entry points|Components|Cross-references|Notes)' <file>`
    Count must be ≥5. If any file is missing a section, flag it as non-conformant.
  - Domains to check: backend, frontend, equipment, floorplans, alarms, history,
    and buildings-config (if produced as standalone).
  - Satisfies: REQ-6 (5-section template compliance)
  - Design ref: §4 validation, §3 Batch D

- [ ] **T-D4** — Compute coverage % and write coverage report
  - Output: `docs/mappings/reflow-clean-177/coverage-report.md`
  - Method: Run `fd --exclude node_modules --exclude src/rc --exclude build`
    on source repo to get total file count. Compare to `entries | length` in
    `index.json`. Compute ratio. Report must include:
    - Total source files scanned
    - Total entries in index.json
    - Coverage % (must be ≥95% to pass)
    - Breakdown by domain (entry count per domain)
    - Spot-check results summary (from T-D1)
    - Schema validation results (from T-D2)
    - Template compliance table (from T-D3)
    - Any domains flagged for re-processing
    - Final verdict: PASS (all checks green) or FAIL (list of failing checks)
  - Satisfies: REQ-7 (coverage ≥95%), REQ-6 (template compliance documented)
  - Design ref: §4 validation, §3 Batch D

- [ ] **T-D5** — Merge deferred patch files into `index.json`
  - Output: Final enriched `docs/mappings/reflow-clean-177/index.json` (in-place update)
  - Method: Read base `index.json` (from Batch A). For each patch file
    (`backend-patch.json`, `frontend-patch.json`, `equipment-patch.json`,
    `floorplans-patch.json`, `alarms-patch.json`, `history-patch.json`, and
    `buildings-config-patch.json` if it exists): merge extension block fields into
    matching entries by `path` key. Write final merged JSON back to `index.json`.
    Run `jq -e .` as final sanity check.
  - Order: T-D5 MUST run AFTER T-D1 (so verified_at stamps are already in memory)
    and AFTER T-D2/T-D3 checks have been run against the pre-merge state (to isolate
    merge from schema errors). Re-run T-D2 check #1 and #3 post-merge to confirm
    nothing was corrupted.
  - Satisfies: REQ-1 (fully populated entries), REQ-2 (backend ext complete), REQ-3
    (frontend ext complete)
  - Design ref: §3 Batch D deferred merge, ADR-7

---

## Review Workload Forecast

| Batch | Deliverable files | Est. lines written | 400-line budget risk | Notes |
|-------|-------------------|--------------------|----------------------|-------|
| A | index.md, index.json, schema.md, README.md, excluded.md | ~2,000–3,000 | N/A — docs only | Not code; 400-line rule doesn't apply to mapping artifacts |
| B | domains/backend.md + backend-patch.json | ~800–1,200 | N/A — docs only | 77 Java entries × ~10 lines each |
| C (all 5) | 5 domain .md + 5 patch .json | ~3,000–4,500 | N/A — docs only | Parallel; each sub-agent ~600–900 lines |
| D | coverage-report.md + merged index.json | ~500–800 | N/A — docs only | Merge in-place; no code changes |

**Total estimated lines**: ~6,300–9,500 (documentation artifacts only).

**400-line budget risk**: LOW — this change produces only documentation and JSON
mapping artifacts, not source code changes. The 400-line PR review budget applies to
code diffs; these mapping artifacts are exempt from that constraint.

**Chained PRs recommended**: No — all output is documentation. A single PR with all
mapping artifacts is appropriate.

**Decision needed before apply**: No — delivery_strategy `auto-chain` is cached and
the 400-line guard does not trigger for documentation-only changes.

---

## Task dependency summary

```
T-A1 → T-A2 → T-A3 → T-A4 → T-A5   (sequential, same sub-agent)
                    │
          ┌─────────┴────────────────────────────┐
          ▼                                       ▼
        T-B1 (sequential)          T-C1, T-C2, T-C3, T-C4, T-C5 (parallel)
          │                                       │
          └─────────────────┬─────────────────────┘
                            ▼
              T-D5 → T-D1 → T-D2 → T-D3 → T-D4   (sequential, same sub-agent)
```

- Batch B and Batch C have no dependency on each other — they run in parallel.
- Batch D requires ALL of B and C to be complete before starting.
- Within Batch D, T-D5 (merge) runs first; T-D1/D2/D3 validate the merged index;
  T-D4 aggregates results from D1/D2/D3 into the final coverage report.

---

## Risks

1. **T-C5 scope ambiguity (buildings-config)**: The design groups buildings and config
   under C5 but does not give a firm file count. T-C5 sub-agent must inventory those
   directories first and decide standalone vs. folded. If standalone file is created,
   T-D3 must add it to the compliance check list. Risk: LOW.

2. **T-B1 decompiled class identification**: 4 classes must have `decompiled: true`.
   The design names BReflowScheme + "3 ux classes" without listing them explicitly.
   T-B1 sub-agent must identify them from `nmodsreflow-ux/` by inspecting
   REFLOW-ARCHITECTURE-ANALYSIS.md. If fewer than 4 are identifiable, flag in
   Notes & gotchas. Risk: LOW-MEDIUM.

3. **T-D5 merge correctness**: Patch files use `path` as the join key. If any
   sub-agent (C1–C5) writes a path with different casing or normalization than the
   base index.json, the merge will silently miss those entries. T-B1 and C1–C5 MUST
   use paths exactly as they appear in index.json (copied from `fd` output, not
   re-derived). Risk: MEDIUM — mitigation is to validate post-merge entry count ≥530.

4. **T-D1 spot-check failure threshold**: If any domain scores below 90% fidelity, the
   domain doc must be re-processed. This could block the final coverage report. With
   `auto-chain` delivery, the orchestrator should re-launch the failing domain's Batch C
   sub-agent and re-run Batch D. Risk: LOW (synthesis docs are high-quality source).

5. **index.json size**: With 530+ entries each potentially carrying 15+ fields,
   `index.json` will be approximately 400–600 KB. Sub-agents reading the full file may
   hit context limits. T-D5 should process the merge using `jq` scripts rather than
   loading the entire JSON into the prompt. Risk: MEDIUM — mitigate by using `jq` CLI
   for merges, not in-prompt JSON editing.
