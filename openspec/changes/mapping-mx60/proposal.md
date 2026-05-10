# SDD Proposal — mapping-mx60

**Date**: 2026-05-09
**Status**: PROPOSED
**Phase**: sdd-propose
**Read**: explore.md (engram #1247 / openspec/changes/mapping-mx60/explore.md)
**Reference**: mapping-reflow-clean-177 (engram #1219 archive-report)

---

## 1. Intent

Produce a queryable, schema-validated mapping of the **MX60 Honeywell Chihuahua** module (`/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/`) following the same `core + extension` schema established by `reflow-clean-177` v1.0, plus a **delta-vs-reflow** artifact that captures HEREDADO/REESCRITO/FALTA/NUEVO/ANÁLOGO relationships against the reference mapping. This delta is the differentiating deliverable: it converts the bloque #68 transplant blueprint from prose-prescription into evidence-backed empirical validation, anchored by file:line citations on both sides. The mapping is needed NOW because (a) the MX60 transplant decisions in bloque #68 §68.1–§68.5 are currently theoretical and need ground-truth before MX60→reflow rewrite work proceeds; (b) the IIFE/BajaScript-classic stack divergence (vs reflow's Vue/Vuex) requires explicit catalog before sprint-1 patterns are committed; (c) the SDD-archived reflow mapping provides a 547-entry template proven viable and reusable.

---

## 2. Scope

### IN scope

- Catalog **all source files** (`.java`, `.js`, `.html`, `.css`, `.gradle.kts`, `.xml`, `.palette`, `.lexicon`, fonts, images, ext/ bundles) under `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/`
- Estimated **~100-130 source entries** + **~80-100 xref entries** (xref **in scope from start** — IIFE namespace dependency graph requires it; reflow deferred xref to a follow-up SDD)
- Schema reuse: **schema v1.0 core unchanged**; one new sibling schema doc with extended `kind` enum (D1) and a NEW `frontend_iife` extension block (D2)
- Domain deep-dives for **18 domains** identified in explore §3 (service-container, equipment-backend, http-rest, equipment-reader, alarms-backend, history-backend, schedules-backend, util-backend, app-shell, equipment-frontend, equipment-detail-up, equipment-detail-misc, alarms-frontend, schedules-frontend, history-frontend, baja-integration, ui-lib, threshold-stores)
- **Differentiating deliverable**: `delta-vs-reflow.md` + `delta.json` — dual-form table mapping every MX60 entry to its reflow analog with status + evidence + bloque #68 cross-ref
- README with rg/jq examples (MX60-specific queries: `MX60.X` namespace lookups, IIFE store inventory, BComponent slot enumeration)
- excluded.md catalog (test files, .git/, .gradle/, build/, .idea/, audit-2026-05-06)
- Spot-check validation (≥40 entries across strata) with `verified_at` timestamps written back to JSON

### OUT of scope

- **Empirical runtime validation** — per #1238 clean-room asymmetry rule: BajaScript subscription latency, `controlTick` timing, `_bajaSetBroken` fallback behavior remain marked **inferred from source, not verified at runtime**
- **Transplant execution** — this SDD produces the catalog + delta; bloque #68 transplant work happens in separate SDDs (mx60-transplant-historydata, mx60-transplant-iife-to-pinia, etc.)
- **Test files mapping** — 16 `.java` test files excluded per reflow convention (Niagara test discovery broken in this plugin version per HANDOFF.md 2026-05-05)
- **Re-mapping reflow** — reflow-clean-177 is frozen v1.0; this SDD reads it but does not modify it
- **UpDetail.js sub-section split** — see D3; deferred to follow-up SDD if needed
- **Binary asset machine-readable entries** — same W-2 deferral as reflow (catalogued in excluded.md, not in index.json)
- **Cross-references with implementation patterns (AP-1..96)** — handled by a separate `mapping-cross-references` style SDD after the core MX60 mapping lands

---

## 3. Deliverables

### Primary mapping artifacts (`docs/mappings/mx60-chihuahua/`)

| File | Purpose | Estimated size |
|------|---------|----------------|
| `index.json` | Machine-readable catalog: ~100-130 entries, schema v1.0 envelope, MX60 extended kind enum + `frontend_iife` extension block | ~140 KB |
| `index.md` | Human-readable master table: one row per entry, summary header | ~35 KB |
| `schema.md` | MX60 schema doc — declares schema_version 1.0, references reflow schema as parent, documents extended `kind` values + `frontend_iife` extension shape | ~12 KB |
| `README.md` | Usage guide: 5-7 rg examples + 6-10 jq examples; MX60-specific query recipes (IIFE namespace graph traversal, BComponent slot lookup, equipment-type filter) | ~9 KB |
| `excluded.md` | Excluded paths catalog: tests, .git, .gradle, build, .idea, audit-2026-05-06, with reasons | ~5 KB |
| `_validation.md` | Spot-check report: stratum sampling, fidelity %, JSON structural validity, coverage analysis | ~15 KB |
| `xref.json` | Machine-readable cross-references: ~80-100 edges, each `{from_id, to_id, usage_kind, evidence}`. Schema mirrors reflow xref schema v1.0 | ~25 KB |
| `xref.md` | Human-readable xref index | ~8 KB |
| `delta-vs-reflow.md` | **DIFFERENTIATING DELIVERABLE** — table comparing every MX60 entry vs reflow analog, with status + evidence + bloque #68 §x.y refs | ~25 KB |
| `delta.json` | Machine-readable form of delta table — enables `jq` queries like "find every NUEVO equipment-backend entry" | ~30 KB |

### Domain deep-dives (`docs/mappings/mx60-chihuahua/domains/`)

| File | Coverage |
|------|----------|
| `service-container.md` | BChiDashboardService, BPlanta (4-level v4 hierarchy, controlTick, lock pool) |
| `equipment-backend.md` | BChiUp, BChiCarcamo, BChiDatalogger + 3 monitors (NUEVO vs reflow) |
| `http-rest.md` | BChiServlet + ChiServletDispatch (single-servlet routing, no BaseServlet hierarchy) |
| `equipment-reader.md` | ChiEquipmentReader, ChiThresholdHelper (Layer-1/Layer-2 DTO, BOrd walk) |
| `alarms-backend.md` | ChiAlarmHelper, ChiAlarmQueryHelper (BAlarmDatabase grouping; no channel-service) |
| `history-backend.md` | ChiHistoryHelper (port of SnlsHistoryHelper) |
| `schedules-backend.md` | ChiScheduleHelper (BQL BNumericSchedule) |
| `util-backend.md` | ChiJsonUtil |
| `app-shell.md` | DashboardApp, Router (hash-based, NOT Vue Router), ConfigManager, SharedEnv |
| `equipment-frontend.md` | EquipmentData, EquipmentCard, EquipmentDetail, EquipmentSnapshotStore, HomeMap |
| `equipment-detail.md` | UpDetail (~2400 LOC), CarcamoDetail, DataloggerDetail (combines explore's two equipment-detail domains for reviewability) |
| `alarms-frontend.md` | AlarmsManager, AlarmsPage, AlarmCards, AlarmDetailsTable, AlarmDetailPage, AlarmLatchStore, AlarmModalActions, AlarmNotesModal, BulkActionBar |
| `schedules-frontend.md` | ScheduleView (BWeeklySchedule auto-discover) |
| `history-frontend.md` | LiveHistoryBuffer, TimeRangePicker (no historyCache.js analog) |
| `baja-integration.md` | SubscriptionPool, WritePoint (dual-path BajaScript + REST fallback) |
| `ui-lib.md` | Toast, Confirm, StatusResolver, Dropdown, Popover, RelativeTime, CsvExport |
| `threshold-stores.md` | ModoOverrideStore, OutputOverrideStore, UpThresholdStore, CarcamoThresholdStore, DataloggerThresholdStore (NUEVO vs reflow) |

**Total**: 17 domain files (the 18 domains from explore §3 are consolidated to 17 by merging `equipment-detail-up` + `equipment-detail-misc` into one `equipment-detail.md` for review ergonomics — a single page covers UP + Cárcamo + Datalogger detail flows since they share the same modal/page lifecycle pattern). `module-descriptor`, `build-config`, `static-resources` are documented in the root `README.md` + `index.md` rather than dedicated files (matches reflow pattern).

### Differentiating deliverable

- **`delta-vs-reflow.md`** + **`delta.json`** — dual-form per D4. Columns:

| Reflow component (id) | MX60 component (id) | Status | LOC reflow | LOC MX60 | LOC delta % | Evidence (file:line) | Bloque #68 §x.y |

---

## 4. Architectural decisions

### D1 — Schema `kind` enum strategy

**Decision**: **Option (a) — extend the kind enum with new IIFE values, treating MX60 schema.md as a v1.1 sibling.**

New kind values added:
- `iife-app` — IIFE module that bootstraps a page-level orchestrator (e.g., `DashboardApp.js`, `AlarmsPage.js`, `EquipmentDetail.js`)
- `iife-store` — in-memory IIFE store managing equipment/threshold/alarm state (e.g., `EquipmentSnapshotStore.js`, `ModoOverrideStore.js`, `AlarmLatchStore.js`)
- `iife-lib` — utility IIFE library reused across pages (e.g., `Toast.js`, `Confirm.js`, `StatusResolver.js`)
- `iife-util` — small utility IIFE co-located with a feature (e.g., `RelativeTime.js`, `CsvExport.js`)
- `iife-entry` — application entry point that wires the SPA shell (effectively `DashboardApp.js`'s bootstrap section; in MX60 `index.html` script-tag ordering supplies the entry semantics — likely 0-1 `iife-entry` instances)

**Rationale**:
1. **Queryability wins**: `jq '.entries[] | select(.kind=="iife-store")'` is a 1-token filter; encoding the same in a `frontend_iife.module_type` field requires `select(.frontend_iife?.module_type=="iife-store")` — slower, more error-prone, and breaks symmetry with how reflow uses `js-store` at the top level.
2. **Schema bump cost is contained**: MX60's `schema.md` is its OWN sibling document under `docs/mappings/mx60-chihuahua/` — extending its enum does NOT touch reflow's frozen `schema.md`. The shared schema_version `1.0` is preserved (additive enum extensions are documented as backward-compatible per the schema's own "additive extensions only" policy).
3. **Architectural truthfulness**: IIFE modules ARE a different `kind` from Vue/Vuex modules at the architectural level — claiming they are `js-lib` and burying the type in an extension misrepresents the catalog's primary classification axis.
4. **Reusability for Analytics**: when Analytics gets its own mapping, its (likely also non-Vue) module types can follow the same additive-enum precedent.

**Trade-off accepted**: any cross-mapping consumer that filters by `kind=="js-lib"` will miss MX60 IIFE libs. Mitigated by documenting in `schema.md` a "kind family" mapping (e.g., `iife-lib` is in family `lib`) and providing a jq recipe in README that selects all lib-family entries across modules.

### D2 — `frontend_iife` extension block shape

**Decision**: lock the shape NOW. Final field set:

```json
"frontend_iife": {
  "namespace": "MX60",
  "globals_written": ["MX60.AlarmsManager", "MX60.AlarmsManager.refresh"],
  "globals_read": ["MX60.ConfigManager", "MX60.Router", "MX60.SubscriptionPool"],
  "iife_pattern": "wrapped-window",
  "load_order_hint": 12,
  "subscriber_role": "consumer | producer | none"
}
```

Field semantics:

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `namespace` | string | yes | Top-level namespace this module attaches to. Always `"MX60"` for the chihuahua module; future-proofed for other namespaces. |
| `globals_written` | string[] | yes (may be `[]`) | Fully-qualified namespace symbols this file assigns to (e.g., `MX60.AlarmsManager`, plus public methods if exposed via prototype). Drives reverse-lookup xref. |
| `globals_read` | string[] | yes (may be `[]`) | Namespace symbols this file references. Source for the IIFE dependency graph. Excludes `MX60` root reads. |
| `iife_pattern` | enum | yes | `wrapped-window` (canonical `(function(window){...})(window)`), `wrapped-bare` (no window), `iife-no-args` (`(function(){...})()`), `not-iife` (the rare static script). Captures variant for the 4-5 outliers. |
| `load_order_hint` | integer or null | no | Position in `index.html` script tag ordering when discoverable (`null` if dynamically loaded or inferred). Validates ordering invariants. |
| `subscriber_role` | enum or null | no | `consumer` (subscribes to BajaScript via SubscriptionPool), `producer` (emits global events), `none` (pure compute). Drives the bloque #68 §68.5 SubscriptionPool→useSubscriber transplant validation. |

**Rationale**:
1. `iife_pattern` IS needed — the `_bajaSetBroken` fallback in WritePoint.js and the bootstrap logic in DashboardApp.js use different IIFE shapes, and the transplant phase needs to know which.
2. `load_order_hint` is needed because IIFE modules HAVE NO `import` — script-tag order in `index.html` IS the dependency declaration. Without it, the catalog cannot answer "what loads before X?". Marked optional to permit `null` for files where we can't determine ordering.
3. `subscriber_role` directly supports bloque #68 §68.5 (SubscriptionPool→useSubscriber decision). Already known from the explore's `baja-integration` analysis.
4. NOT included: `iife_export_keys` (redundant with `globals_written` last-segment), `vue_replacement_target` (belongs in `delta.json`, not the catalog itself).

### D3 — UpDetail.js (~2400 LOC) treatment

**Decision**: **Option (a) — single entry with compressed `purpose` ≤150 chars + xref edges only.**

`purpose` (149 chars):
> `MX60 UP detail page: 37-slot panel with MANUAL/SETPOINT/SCHEDULE modes, threshold UI, history chart, write logic, BajaScript subscription lifecycle.`

**Rationale**:
1. **Schema fidelity**: reflow's schema indexes by file boundary. `defined_at: {start, end}` would be a brand-new core-field extension and v1 of MX60 mapping should match reflow's grain.
2. **xref carries the functional load**: the transplant blueprint cares about WHICH modules UpDetail imports/produces — that lives perfectly in xref edges. Functional sub-sections (MANUAL mode, SETPOINT mode, etc.) are revealed by xref + the domain deep-dive (`equipment-detail.md`), not by splitting the file.
3. **Risk of spurious split**: defining "what is a logical section" of a 2400-LOC IIFE without a parser is subjective and would trigger churn during review. A purely structural split (line ranges) carries no semantic value beyond what `equipment-detail.md` already provides.
4. **Defer (b) explicitly**: if a downstream SDD (e.g., `mx60-transplant-updetail`) needs sub-section grain, it can ADD a `defined_at` extension at that point. Today's mapping does not need it.

**Mitigation for the xref high-cardinality risk** (R2 from explore): the xref shard plan will allocate UpDetail.js a dedicated review pass during S8 to ensure its outgoing edge list (likely 15-25 edges) is complete.

### D4 — `delta-vs-reflow.md` format

**Decision**: **Dual-form — markdown (`delta-vs-reflow.md`) + JSON (`delta.json`).**

`delta.json` schema (preliminary; locked in design phase):

```json
{
  "schema_version": "1.0",
  "module": "mx60-chihuahua",
  "compared_against": "reflow-clean-177",
  "generated_at": "<ISO 8601>",
  "deltas": [
    {
      "mx60_id": "chihuahua/chihuahua-ux/src/rc/js/app/SubscriptionPool.js",
      "reflow_id": "reflow-frontend/src/api/subscriber.js",
      "status": "REESCRITO",
      "loc_mx60": 280,
      "loc_reflow": 195,
      "loc_delta_pct": 43.6,
      "evidence": "ported from SanLuis port adapted for MX60 3-type equipment; see HANDOFF.md §port-history",
      "bloque68_section": "§68.5",
      "notes": "core lifecycle HEREDADO; wrapper REESCRITO"
    }
  ]
}
```

**Rationale**:
1. **Markdown alone fails for queries**: bloque #68 transplant work needs `jq` filters like "every NUEVO entry in domain=threshold-stores" to scope sprint-1. Markdown can't answer that without grep gymnastics.
2. **JSON alone fails for review**: PR reviewers need a rendered table. Generating MD from JSON via `jq -r '... | @tsv' | column` works but loses the curated narrative columns (e.g., the `notes` column needs human-friendly prose).
3. **Cost is small**: `delta.json` is ~30 KB, mechanically derivable by S7. The shard generates JSON first, MD as a rendered view (jq + sd template), aligning with engram #1231's "markdown >50 rows from JSON via jq" rule.
4. **Reusability**: when Analytics gets its delta, `delta.json` schema is reusable across modules.

---

## 5. Shard plan (preliminary — locked in design)

| Shard | Domains | Est. entries | Notes |
|-------|---------|-------------:|-------|
| S1: backend-rt | service-container, equipment-backend | 10 | BChiDashboardService, BPlanta, 3 monitors, 3 equipment types |
| S2: backend-ux | http-rest, equipment-reader, alarms-backend, history-backend, schedules-backend, util-backend | 9 | All `chihuahua-ux/src/com/...` Java |
| S3: frontend-core | app-shell, baja-integration, ui-lib | ~14 | DashboardApp, Router, ConfigManager, SharedEnv, SubscriptionPool, WritePoint, 7 ui-lib |
| S4: frontend-equipment | equipment-frontend, equipment-detail, threshold-stores | ~15 | EquipmentData/Card/Detail/SnapshotStore/HomeMap, UpDetail, CarcamoDetail, DataloggerDetail, LiveHistoryBuffer, TimeRangePicker, 5 threshold stores |
| S5: frontend-alarms-schedules | alarms-frontend, schedules-frontend, history-frontend | ~12 | 9 alarms + ScheduleView + 2 history-frontend (LiveHistoryBuffer/TimeRangePicker may move from S4 to S5 — locked in design) |
| S6: resources-config | module-descriptor, build-config, static-resources | ~25 | niagara-module.xml, palettes ×2, lexicons ×2, index.html, images, fonts, ext/ (5), gradle.kts ×3 |
| S7: delta-vs-reflow | cross-cutting | 1 markdown + 1 json | Reads completed S1-S6 outputs + reflow index.json |
| S8: xref | cross-cutting | ~80-100 edges | Reads completed S1-S6 outputs; produces xref.json + xref.md |

**Per-shard entry cap respected**: max ~25 entries (S6) under the 75 hard cap from #1231. Each shard receives ONE canonical JSON example literal in its sub-agent prompt + the prohibited fields list (`from`, `caller`, `file`, `callers`, `used_by`, `edges`, `source_path`, `name`).

**Order of execution**:
- S1, S2, S6 can run in parallel (independent domains, no cross-deps).
- S3, S4, S5 can run in parallel after S1-S2 complete (frontend xref needs backend Java FQNs available).
- S7 must run AFTER S1-S6 (reads all outputs).
- S8 must run AFTER S1-S6 (reads all outputs).
- S7 and S8 can run in parallel.

---

## 6. Bloque #68 transplant blueprint validation

The delta-vs-reflow artifact converts each bloque #68 prescription from prose into evidence-backed delta rows:

| Bloque #68 § | Decision (current state — prose) | Validation in `delta.json` |
|---|---|---|
| §68.1 — HistoryData split | Split `HistoryData.java` into `Engine` + `Serializer` (~331L + ~257L) | MX60's `ChiHistoryHelper.java` is ANÁLOGO to a slimmer `HistoryData` (~400 LOC, no servlet wrapping). Confirms the split is viable: ANÁLOGO row with `loc_delta_pct` quantifies. NOTE: the §68.1.6 `HistoryDataCache` class remains FICTICIA (verified empirically per #1236) — delta confirms it does not exist in MX60 either. |
| §68.2 — BChiServlet → BReflowService | Servlet routing pattern (single dispatch class vs BaseServlet hierarchy) | MX60 ships a working precedent: `BChiServlet` + `ChiServletDispatch` is REESCRITO from reflow's `BaseServlet` family. Delta row provides the LOC + line evidence. |
| §68.3 — IIFE store → Pinia | All IIFE stores are NUEVO with no Vuex analog, requiring fresh Pinia design | Delta marks `EquipmentSnapshotStore.js`, `UpThresholdStore.js`, `ModoOverrideStore.js`, `OutputOverrideStore.js`, `CarcamoThresholdStore.js`, `DataloggerThresholdStore.js`, `AlarmLatchStore.js` as NUEVO. Confirms greenfield Pinia design for these 7 stores. |
| §68.4 — ack flow asymmetry | MX60 inline ack vs reflow modal-based ack (AlarmAckConfirm) | Delta marks `AlarmModalActions.js` + `AlarmNotesModal.js` as ANÁLOGO (no AlarmAckConfirm modal in MX60 — inline confirm). Validates the §68.4 sprint-1 decision to preserve MX60 inline pattern literally. |
| §68.5 — SubscriptionPool → useSubscriber | Migrate IIFE SubscriptionPool to Vue 3 useSubscriber composable | Delta marks `SubscriptionPool.js` as REESCRITO from SanLuis port (HEREDADO core + REESCRITO wrapper). Validates the lift-and-rewrite path: core BajaScript lifecycle is reusable. |

Each delta row carries `bloque68_section` so a single `jq '.deltas[] | select(.bloque68_section!=null)'` query produces the transplant evidence index — directly consumable by the future MX60-transplant SDDs.

---

## 7. Risks and mitigations

| Risk | Severity | Mitigation |
|------|----------|------------|
| **R1 — Schema bump miscalibration** (D1 chosen Option (a) — 5 new kind values may misclassify edge cases like `index.html` script-tag inline JS or hbs templates) | MEDIUM | `schema.md` documents the decision tree: any new kind discovered during apply triggers a CR before adding. Spot-check phase explicitly samples 3 entries per new kind value. |
| **R2 — UpDetail.js xref completeness** (single entry with 15-25 outgoing edges; risk of missing internal `MX60.X` reads) | MEDIUM | S8 dedicates a review pass to UpDetail. `_validation.md` records xref coverage % for UpDetail specifically. Acceptance criterion: ≥90% of `MX60.X` reads in UpDetail.js source are reflected as xref edges. |
| **R3 — Runtime boundary drift** (per #1238: SubscriptionPool latency, controlTick timing, _bajaSetBroken fallback are NOT empirically verified) | MEDIUM | Every entry whose `purpose` describes runtime behavior (≈15-20 entries) gets a `**inferred from mapping, not verified empirically**` note in the domain deep-dive. The `purpose` field itself stays declarative (what the file IS, not what it does at runtime). |
| **R4 — delta-vs-reflow heuristic false positives** (the `Ported from Snls\|ported verbatim` grep may miss un-commented ports OR wrongly classify references in JavaDoc as ports) | MEDIUM | Two-pass: (pass 1) heuristic produces draft; (pass 2) human spot-check reviews all REESCRITO + ANÁLOGO classifications (~30-40 rows) before locking. Each delta row carries `evidence` field with the specific marker citation. |
| **R5 — Shard sub-agent context inflation** (S4 frontend-equipment hits ~15 entries including UpDetail's outsized purpose; risk of overflowing the per-shard prompt budget) | LOW | UpDetail's `purpose` is hard-capped at 150 chars (D3). S4 prompt size estimated at ~12K tokens — well within Sonnet's 200K budget. |
| **R6 — Test-files exclusion ambiguity** (16 test `.java` files; some readers will expect them documented at least minimally) | LOW | `excluded.md` documents the 16 test paths individually with reason "test discovery broken in plugin 7.3.40 per HANDOFF.md 2026-05-05". Mirrors reflow's exclusion pattern. |
| **R7 — Cross-module schema drift** (MX60 schema.md adds 5 kinds + new extension; future modules may diverge further, breaking the "shared schema_version 1.0" claim) | LOW | `schema.md` declares MX60 schema as "schema_version 1.0 + MX60 additive extensions" with explicit list of additions. Promotes to v1.1 only if a third module also needs the IIFE kinds. |

---

## 8. Estimates

- **Source entries**: 100-130
- **Xref edges**: 80-100
- **Delta rows**: ~80-100 (one per MX60 entry; ~70% map to a reflow analog, ~30% are NUEVO)
- **Total artifacts**: ~200-230 catalog rows + 17 domain files + 6 root files + 1 delta dual-form = ~240-260 artifacts
- **Wall-clock**: 3-4 SDD sessions
  - Session 1: spec + design (parallel) + tasks
  - Session 2: apply S1+S2+S6 (parallel)
  - Session 3: apply S3+S4+S5 (parallel) + S7+S8 (parallel)
  - Session 4: verify + archive
- **Token budget per shard**: ~15-25K input (source files + reflow xref subset for S7) + ~10-15K output (entries + domain prose). All shards comfortably under 200K Sonnet budget.
- **Sub-agent count**: 8 mapping/xref shards + 1 verify + 1 archive = 10 sub-agent invocations across 3-4 sessions.

---

## 9. Acceptance criteria (high-level — full criteria locked in spec)

- **AC-1**: every source file under MX60 source root has either an entry in `index.json` OR a row in `excluded.md` with a reason (coverage 100% post-exclusions, matching reflow's effective coverage)
- **AC-2**: every `kind: java-class` entry has a populated `backend` extension; every `kind: iife-*` entry has a populated `frontend_iife` extension; entries with neither use `null` explicitly (no missing extensions)
- **AC-3**: `delta.json` has one row per `index.json` entry (modulo entries marked excluded from delta), with status ∈ {HEREDADO, REESCRITO, FALTA, NUEVO, ANÁLOGO} and non-empty `evidence`
- **AC-4**: ≥40 entries (5 per stratum × ≥8 strata) have `verified_at` timestamps; spot-check fidelity ≥90%
- **AC-5**: every bloque #68 §68.1-§68.5 prescription is referenced by ≥1 row in `delta.json` via `bloque68_section` field

---

## 10. Recommended next phases

- **`sdd-spec`** — formalize 10-12 REQs (REQ-1..10 mapping reflow's REQs + REQ-11 frontend_iife extension contract + REQ-12 delta.json contract + bloque #68 traceability REQ)
- **`sdd-design`** — parallel with spec; lock the shard plan, finalize `delta.json` schema, define exact decision tree for `kind` classification edge cases (inline `<script>` in index.html, hbs templates absent, ext/ bundle status), commit per-shard sub-agent prompts
- **`sdd-tasks`** — after both spec and design land; mechanical breakdown into shard tasks with delivery_strategy `auto-chain`

---

## skill_resolution

- **injected** (compact rules from #1231 multi-shard sub-agent best practices, #309 skill-registry niagara conventions, #1238 clean-room runtime asymmetry — received from orchestrator and applied to D1-D4 reasoning + R3 mitigation)
