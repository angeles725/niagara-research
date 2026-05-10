# SDD Tasks — mapping-mx60

**Date**: 2026-05-09
**Phase**: sdd-tasks
**Reads**: spec.md (engram #1249) + design.md (engram #1250)
**Delivery strategy**: auto-chain
**Mode**: auto

---

## Dependency graph

```
A1 ──► A2 ──┬──► B1 ──┐
             ├──► B2 ──┤
             ├──► B6 ──┤
             │          ├──► (batch-1 complete) ──► B3 ──┐
             │                                      B4 ──┤
             │                                      B5 ──┘
             │                                           │
             └───────────────────────────────────────────►──► C1 ──┬──► C2
                                                                    ├──► C3
                                                                    ├──► C4
                                                                    ├──► D1..D17 (parallel)
                                                                    ├──► B7 (delta)
                                                                    └──► B8 (xref)
                                                                                │
                                                                                └──► E1 ──► E2 ──► E3
```

**Linearized execution order (auto-chain slices)**

| Slice | Tasks | Sequential / Parallel |
|-------|-------|-----------------------|
| 1 | A1, A2 | sequential |
| 2 | B1, B2, B6 | parallel (after A2) |
| 3 | B3, B4, B5 | parallel (after B1+B2 complete) |
| 4 | C1 | sequential (after B1..B6 complete) |
| 5 | C2, C3, C4, D1..D17, B7, B8 | parallel (after C1) |
| 6 | E1, E2, E3 | sequential (after slice 5 complete) |

---

## Phase A — Setup

### [x] T-A1: Create output directory tree for mx60-chihuahua mapping

- **Depends on**: nothing
- **Owner**: sdd-apply (file-ops)
- **Inputs**: none (directory creation only)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/` (root dir)
  - `docs/mappings/mx60-chihuahua/domains/` (subdir)
  - `docs/mappings/mx60-chihuahua/scripts/` (subdir for jq/bash scripts)
- **Acceptance**:
  - `fd . docs/mappings/mx60-chihuahua/ --type d` lists 3 directories (root + domains + scripts)
  - Directory does NOT yet contain any `.json` or `.md` files (clean slate)
- **Estimated entries / LOC**: 0 entries; directory stubs only
- **Notes**: Create `scripts/` now so later tasks (C1, C2, B7, B8) can write scripts in-place. Do not create any placeholder files — empty dirs only.

---

### [x] T-A2: Author `schema.md` for mx60-chihuahua

- **Depends on**: T-A1
- **Owner**: sdd-apply (content-write)
- **Inputs**:
  - `docs/mappings/reflow-clean-177/schema.md` (template reference)
  - Design §A (locked schema decisions)
  - Spec REQ-5 (5 new kind values)
  - Spec REQ-4 (frontend_iife block shape)
  - Spec REQ-3 (backend block — inherited unchanged)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/schema.md`
- **Acceptance** (REQ-5):
  - `rg '^| \`iife-app\`' docs/mappings/mx60-chihuahua/schema.md` returns a match — all 5 IIFE kind values documented
  - `rg 'frontend_iife' docs/mappings/mx60-chihuahua/schema.md` returns ≥6 lines (block declaration + all 6 fields)
  - `rg 'frontend_vue' docs/mappings/mx60-chihuahua/schema.md` returns 0 matches (forbidden, §A.3)
- **Estimated entries / LOC**: ~200 LOC (schema.md ~12 KB analogous to reflow)
- **Notes**:
  - Declare `schema_version: "1.0"` + `extensions: ["backend","frontend_iife"]` in preamble
  - List inherited core kind values from reflow v1.0 as "inherited"
  - Add 5 new kinds: `iife-app`, `iife-store`, `iife-lib`, `iife-util`, `iife-entry` with definitions matching design §A.1
  - Document `iife_pattern` enum: `wrapped-window | wrapped-bare | iife-no-args | not-iife` (design §A.2 uses `iife-window|iife-self|iife-named|iife-other` — use the design §A.2 enum values exactly)
  - Document `subscriber_role` enum: `consumer | producer | none`
  - Include 9-rule kind decision tree (design §E item 4)
  - Explicitly prohibit: `frontend_vue` (forbidden), `frontend_js` when kind is `iife-*` (mutually exclusive)
  - Note: `decompiled: false` for all MX60 entries (source available, no CFR decompilation)

---

## Phase B — Per-shard mapping

### [x] T-B1: Map S1 backend-rt — service-container + equipment-backend domains

- **Depends on**: T-A2
- **Owner**: sdd-apply (mapping, model: sonnet)
- **Inputs**:
  - Source root: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-rt/src/`
  - `docs/mappings/mx60-chihuahua/schema.md`
  - `scripts/validate-shard.jq` (T-A2 does not create it — B1 must inline validate or defer to C1; see Notes)
  - Design §D shard plan S1, §E apply template
  - Explore §3 domain table rows: service-container, equipment-backend
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/shards/s1-backend-rt.json` (intermediate shard JSON, ≤75 entries)
- **Acceptance** (REQ-1, REQ-3, REQ-13):
  - `jq '.entries | length' shards/s1-backend-rt.json` is between 8 and 12 (estimated 10)
  - `jq '[.entries[] | select(.kind == "java-class") | select(.backend == null)] | length' shards/s1-backend-rt.json` returns `0`
  - `jq '[.entries[] | select(.id == null or .path == null or .kind == null or .domain == null or .purpose == null or .loc == null or .status == null)] | length' shards/s1-backend-rt.json` returns `0`
- **Estimated entries / LOC**: 10 entries; ~150 LOC of JSON
- **Notes**:
  - Files to map: `BChiDashboardService.java`, `BPlanta.java`, `BChiUp.java`, `BChiCarcamo.java`, `BChiDatalogger.java`, `BChiUpMonitor.java`, `BChiCarcamoMonitor.java`, `BChiDataloggerMonitor.java` (8 source java) + 2 NRE-generated stubs if present
  - `BChiDashboardService.java`: `profile: "rt"`, `bcomponent_type: "BAbstractService"`, `slots` from `@NiagaraProperty` count (empirical read), `decompiled: false`
  - `BChiUp.java`: 37 slots (design §D S1 note), `bcomponent_type: "BComponent"`, `actions: []`, `rest_endpoints: []`
  - Any claim about `controlTick` 10-second interval behavioral outcome MUST carry `"(inferred from mapping)"` annotation — REQ-14
  - Shard output uses same envelope as index.json (schema_version, module, entries[]) to allow jq merge in C1
  - Create `docs/mappings/mx60-chihuahua/shards/` subdirectory if it does not exist

---

### [x] T-B2: Map S2 backend-ux — http-rest + equipment-reader + alarms-backend + history-backend + schedules-backend + util-backend domains

- **Depends on**: T-A2
- **Owner**: sdd-apply (mapping, model: sonnet)
- **Inputs**:
  - Source root: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-ux/src/` (`.java` files only)
  - `docs/mappings/mx60-chihuahua/schema.md`
  - Design §D shard plan S2, §E apply template
  - Explore §3 domain table rows: http-rest, equipment-reader, alarms-backend, history-backend, schedules-backend, util-backend
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/shards/s2-backend-ux.json` (intermediate shard JSON, ≤75 entries)
- **Acceptance** (REQ-1, REQ-3, REQ-13):
  - `jq '.entries | length' shards/s2-backend-ux.json` is between 7 and 11 (estimated 9)
  - `jq '[.entries[] | select(.kind == "java-class") | select(.backend == null)] | length' shards/s2-backend-ux.json` returns `0`
  - Entry for `BChiServlet.java`: `backend.rest_endpoints` is a non-empty array with ≥8 endpoints (GET `/api/equipment`, `/api/alarms`, etc. per explore §2)
  - Entry for `ChiEquipmentReader.java`: `backend.bcomponent_type` is `null`, `backend.slots` is `null`
- **Estimated entries / LOC**: 9 entries; ~140 LOC of JSON
- **Notes**:
  - Files to map: `BChiServlet.java`, `ChiServletDispatch.java`, `ChiEquipmentReader.java`, `ChiThresholdHelper.java`, `ChiAlarmHelper.java`, `ChiAlarmQueryHelper.java`, `ChiHistoryHelper.java`, `ChiScheduleHelper.java`, `ChiJsonUtil.java`
  - `BChiServlet.java`: `profile: "ux"`, list all REST endpoints from explore §2 (GET+POST routes under `/mx60/`)
  - `ChiHistoryHelper.java`: the §68.1 split candidate — note in `purpose` that it is analogous to SnlsHistoryHelper; claims about Engine+Serializer split are `"(inferred from mapping)"` per REQ-14
  - `ChiJsonUtil.java`: `bcomponent_type: null`, `slots: null`, `actions: []`, `box_methods: []` (pure-Java stateless util)

---

### [x] T-B3: Map S3 frontend-core — app-shell + baja-integration + ui-lib domains

- **Depends on**: T-B1, T-B2 (backend shards must be complete so domain taxonomy is confirmed before frontend mapping begins)
- **Owner**: sdd-apply (mapping, model: sonnet)
- **Inputs**:
  - Source root: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-ux/src/rc/js/`
  - `docs/mappings/mx60-chihuahua/schema.md`
  - Design §D shard plan S3, §E apply template (canonical IIFE JSON example for AlarmsManager.js)
  - Explore §3 domain rows: app-shell, baja-integration, ui-lib
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/shards/s3-frontend-core.json` (intermediate shard JSON, ≤75 entries)
- **Acceptance** (REQ-1, REQ-4, REQ-5, REQ-13):
  - `jq '.entries | length' shards/s3-frontend-core.json` is between 12 and 16 (estimated ~14)
  - `jq '[.entries[] | select(.kind | test("^iife-")) | select(.frontend_iife == null)] | length' shards/s3-frontend-core.json` returns `0`
  - Entry for `SubscriptionPool.js`: `frontend_iife.subscriber_role` is `"producer"`, `globals_written` contains `"MX60.SubscriptionPool"`
  - `jq '[.entries[].kind] | unique | map(select(test("^vue-|^js-store|^js-mixin|^js-plugin"))) | length' shards/s3-frontend-core.json` returns `0` (no reflow-only kinds used)
- **Estimated entries / LOC**: ~14 entries; ~220 LOC of JSON
- **Notes**:
  - Files: `DashboardApp.js` (iife-app), `Router.js` (iife-app), `ConfigManager.js` (iife-app), `SharedEnv.js` (iife-lib), `SubscriptionPool.js` (iife-lib), `WritePoint.js` (iife-lib), `Toast.js` (iife-lib), `Confirm.js` (iife-lib), `StatusResolver.js` (iife-lib), `Dropdown.js` (iife-lib), `Popover.js` (iife-lib), `RelativeTime.js` (iife-lib), `CsvExport.js` (iife-lib) + any `iife-entry` bootstrap file
  - `SubscriptionPool.js`: BajaScript lifecycle claims (`baja.Ord.make().get()` latency, subscriber teardown on `destroy()`) MUST carry `"(inferred from mapping)"` — REQ-14
  - `WritePoint.js`: `_bajaSetBroken` flag behavior (dual-path fallback) is station-specific — annotate as inferred
  - `iife_pattern` for all: `"wrapped-window"` (pattern confirmed at explore §2: `(function(window) { 'use strict'; var MX60 = window.MX60 || {}; ...`)
  - `load_order_hint`: DashboardApp last (highest), SubscriptionPool early (low number), Router mid-range — derive from namespace dependency order

---

### [x] T-B4: Map S4 frontend-equipment — equipment-frontend + equipment-detail + threshold-stores domains

- **Depends on**: T-B1, T-B2
- **Owner**: sdd-apply (mapping, model: sonnet)
- **Inputs**:
  - Source root: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-ux/src/rc/js/`
  - `docs/mappings/mx60-chihuahua/schema.md`
  - Design §D shard plan S4, §E apply template
  - Explore §3 domain rows: equipment-frontend, equipment-detail-up, equipment-detail-misc, threshold-stores
  - Design §D: UpDetail purpose hard cap ≤150 chars (HARD constraint)
  - Proposal §D3: UpDetail purpose text (149 chars literal)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/shards/s4-frontend-equipment.json` (intermediate shard JSON, ≤75 entries)
- **Acceptance** (REQ-1, REQ-4, REQ-5, REQ-13):
  - `jq '.entries | length' shards/s4-frontend-equipment.json` is between 13 and 18 (estimated ~15)
  - `jq '[.entries[] | select(.id | test("UpDetail")) | .purpose | length <= 150] | all' shards/s4-frontend-equipment.json` returns `true`
  - `jq '[.entries[] | select(.kind == "iife-store")] | length' shards/s4-frontend-equipment.json` is ≥5 (threshold stores: EquipmentSnapshotStore + 4 threshold stores)
  - `jq '[.entries[] | select(.kind | test("^iife-")) | select(.frontend_iife == null)] | length' shards/s4-frontend-equipment.json` returns `0`
- **Estimated entries / LOC**: ~15 entries; ~240 LOC of JSON
- **Notes**:
  - Files: `EquipmentData.js` (iife-lib), `EquipmentCard.js` (iife-app), `EquipmentDetail.js` (iife-app), `EquipmentSnapshotStore.js` (iife-store), `HomeMap.js` (iife-app), `UpDetail.js` (iife-app, ~2400 LOC), `CarcamoDetail.js` (iife-app), `DataloggerDetail.js` (iife-app), `LiveHistoryBuffer.js` (iife-lib), `TimeRangePicker.js` (iife-lib), `ModoOverrideStore.js` (iife-store), `OutputOverrideStore.js` (iife-store), `UpThresholdStore.js` (iife-store), `CarcamoThresholdStore.js` (iife-store), `DataloggerThresholdStore.js` (iife-store)
  - `UpDetail.js` is domain `equipment-detail`; `CarcamoDetail.js` + `DataloggerDetail.js` are also `equipment-detail`
  - `UpDetail.js` purpose: use the 149-char literal from proposal §D3: `MX60 UP detail page: 37-slot panel with MANUAL/SETPOINT/SCHEDULE modes, threshold UI, history chart, write logic, BajaScript subscription lifecycle.`
  - IIFE stores: `globals_written` contains the store namespace (e.g. `"MX60.UpThresholdStore"`), `subscriber_role: "none"` (stores are not subscribers — they are read/written by app modules)
  - `LiveHistoryBuffer.js`: ring-buffer pattern — no BajaScript subscription, purely REST-driven; `subscriber_role: "none"`

---

### [x] T-B5: Map S5 frontend-alarms-schedules — alarms-frontend + schedules-frontend + history-frontend domains

- **Depends on**: T-B1, T-B2
- **Owner**: sdd-apply (mapping, model: sonnet)
- **Inputs**:
  - Source root: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-ux/src/rc/js/`
  - `docs/mappings/mx60-chihuahua/schema.md`
  - Design §D shard plan S5, §E apply template
  - Explore §3 domain rows: alarms-frontend, schedules-frontend, history-frontend
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/shards/s5-frontend-alarms-schedules.json` (intermediate shard JSON, ≤75 entries)
- **Acceptance** (REQ-1, REQ-4, REQ-5, REQ-13):
  - `jq '.entries | length' shards/s5-frontend-alarms-schedules.json` is between 10 and 14 (estimated ~12)
  - `jq '[.entries[] | select(.kind | test("^iife-")) | select(.frontend_iife == null)] | length' shards/s5-frontend-alarms-schedules.json` returns `0`
  - Entry for `AlarmLatchStore.js`: `kind` is `"iife-store"`, `frontend_iife.subscriber_role` is `"none"`
  - Entry for `AlarmsManager.js`: `frontend_iife.subscriber_role` is `"consumer"` or `"producer"` (not `"none"` — it drives alarm polling)
- **Estimated entries / LOC**: ~12 entries; ~190 LOC of JSON
- **Notes**:
  - Files: `AlarmsManager.js` (iife-app), `AlarmsPage.js` (iife-app), `AlarmCards.js` (iife-lib), `AlarmDetailsTable.js` (iife-lib), `AlarmDetailPage.js` (iife-app), `AlarmLatchStore.js` (iife-store), `AlarmModalActions.js` (iife-lib), `AlarmNotesModal.js` (iife-lib), `BulkActionBar.js` (iife-lib), `ScheduleView.js` (iife-app)
  - History-frontend files from S4 (`LiveHistoryBuffer.js`, `TimeRangePicker.js`) belong to S4 per design §D — S5 does NOT duplicate them; if explore §3 listed them under history-frontend, note this as domain `history-frontend` on those entries in S4
  - `ScheduleView.js` domain: `schedules-frontend`; BWeeklySchedule auto-discover behavior is `"(inferred from mapping)"` — REQ-14
  - `AlarmModalActions.js` + `AlarmNotesModal.js` are §68.4 ANÁLOGO candidates — note `source_doc: {file: "openspec/changes/mapping-mx60/proposal.md", section: "6. Bloque #68"}` for traceability (REQ-9)

---

### [x] T-B6: Map S6 resources-config — module-descriptor + build-config + static-resources domains

- **Depends on**: T-A2
- **Owner**: sdd-apply (mapping, model: sonnet)
- **Inputs**:
  - Source root: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/`
  - `docs/mappings/mx60-chihuahua/schema.md`
  - Design §D shard plan S6
  - Explore §1 static resource counts (~30 items: 16 JPGs + 3 logos + 1 MAQUILA + 5 fonts + CSS + ext/ bundles)
  - Spec REQ-10 (exclusions must be documented)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/shards/s6-resources-config.json` (intermediate shard JSON, ≤75 entries)
- **Acceptance** (REQ-1, REQ-5, REQ-10, REQ-13):
  - `jq '.entries | length' shards/s6-resources-config.json` is between 20 and 32 (estimated ~25)
  - `jq '[.entries[] | select(.status == "resource") | select(.loc != 0)] | length' shards/s6-resources-config.json` returns `0` (binary assets have loc=0)
  - `jq '[.entries[] | select(.kind | IN("iife-app","iife-store","iife-lib","iife-util","iife-entry"))] | length' shards/s6-resources-config.json` returns `0` (no IIFE kinds in this shard — only config/resource/module-descriptor)
  - Exclusions array present with ≥3 groups (test-rt, test-ux, .idea/.gradle/build)
- **Estimated entries / LOC**: ~25 entries + exclusions; ~300 LOC of JSON
- **Notes**:
  - Files to map: `niagara-module.xml` (×2), `module.palette` (×2), `module-permissions.xml` (×2), `module.lexicon` (×2) → `kind: module-descriptor`; `chihuahua-rt.gradle.kts`, `chihuahua-ux.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` → `kind: config`; `index.html` → `kind: config`; `rc/img/` files → `kind: resource-image`, `loc: 0`; `rc/fonts/` files → `kind: resource-font` (if declared in schema) or `kind: resource`, `loc: 0`; `rc/css/` → `kind: resource-css`, `loc: 0` or actual LOC if text; `ext/chartjs/*.js` (×2) + `ext/threejs/*.js` (×3) → `kind: compiled-bundle` or `kind: resource`, `status: resource`, `loc: 0`
  - Exclusions section: 16 test java files (list individually with reason), `.idea/` dir, `.gradle/` dir, `build/` dir, outer Gradle wrapper files — populate `exclusions[]` array per REQ-10
  - This shard is the SOLE producer of `exclusions[]` — C1 must merge them into the envelope

---

### [x] T-B7: Build delta-vs-reflow dual-form artifact (delta.json + delta-vs-reflow.md)

- **Depends on**: T-C1 (full index.json must exist)
- **Owner**: sdd-apply (content-write + jq scripting)
- **Inputs**:
  - `docs/mappings/mx60-chihuahua/index.json` (full merged index)
  - `docs/mappings/reflow-clean-177/index.json` (reflow reference, READ-ONLY per #309)
  - Design §F (delta methodology and heuristics)
  - Spec REQ-11 (delta schema, status enum, bloque68_section)
  - Proposal §6 (bloque #68 validation table with expected status per §68.x)
  - Explore §5 (rg detection queries for HEREDADO/NUEVO candidates)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/scripts/build-delta.sh`
  - `docs/mappings/mx60-chihuahua/delta.json`
  - `docs/mappings/mx60-chihuahua/delta-vs-reflow.md`
- **Acceptance** (REQ-11):
  - `jq '{schema_version, module, compared_against}' delta.json` → `schema_version: "1.0"`, `module: "mx60-chihuahua"`, `compared_against: "reflow-clean-177"`
  - `jq '[.deltas[] | select(.evidence == null or .evidence == "")] | length' delta.json` returns `0`
  - `jq '[.deltas[] | select(.status | IN("HEREDADO","REESCRITO","FALTA","NUEVO","ANÁLOGO") | not)] | length' delta.json` returns `0`
  - `jq '[.deltas[] | select(.bloque68_section != null)] | map(.bloque68_section) | unique' delta.json` contains `"§68.1"`, `"§68.2"`, `"§68.3"`, `"§68.4"`, `"§68.5"` (all 5 sections)
  - `delta-vs-reflow.md` contains a Markdown table with all required columns
- **Estimated entries / LOC**: ~80-100 delta rows; `delta.json` ~30 KB; `delta-vs-reflow.md` ~25 KB; `build-delta.sh` ~80 LOC
- **Notes**:
  - Heuristics from design §F: HEREDADO (port-marker hit AND |LOC delta| ≤15%), REESCRITO (port-marker AND >30% OR name-match AND >30%), ANÁLOGO (name-match AND 15-30% AND no marker), NUEVO (no name analog in reflow), FALTA (reflow has it, MX60 doesn't)
  - Run: `rg "Ported|ported|Port of" /home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua --include="*.java" --include="*.js"` for HEREDADO candidates
  - Expected classifications per proposal §6: `ChiHistoryHelper` → ANÁLOGO (§68.1), `BChiServlet` → REESCRITO (§68.2), 7 threshold stores → NUEVO (§68.3), `AlarmModalActions+AlarmNotesModal` → ANÁLOGO (§68.4), `SubscriptionPool` → HEREDADO core + REESCRITO wrapper (§68.5)
  - Human spot-check of ≥10 REESCRITO rows required before committing delta.json
  - `evidence` MUST be `"file:line"` format (e.g. `"ChiHistoryHelper.java:1"`)

---

### [x] T-B8: Build xref layer (xref.json + xref.md)

- **Depends on**: T-C1 (full index.json must exist for id validation)
- **Owner**: sdd-apply (jq + rg scripting)
- **Inputs**:
  - `docs/mappings/mx60-chihuahua/index.json` (for id validation)
  - Source root: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-ux/src/rc/js/` (all `.js` files)
  - Source root Java: `chihuahua-rt/src/` + `chihuahua-ux/src/` (for FQN Java refs)
  - Design §C (C5 build-xref.sh pipeline stages)
  - Spec REQ-12 (≥80 edges, valid schema, no orphans)
  - `docs/mappings/reflow-clean-177/xref-schema.md` (schema reference)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/scripts/build-xref.sh`
  - `docs/mappings/mx60-chihuahua/xref.json`
  - `docs/mappings/mx60-chihuahua/xref.md`
- **Acceptance** (REQ-12):
  - `jq '. | length' xref.json` ≥ 80 (or `jq '.entries | length'` if wrapped in envelope)
  - `jq '[.[] | select(.from_id == null or .to_id == null or .usage_kind == null or .evidence == null)] | length' xref.json` returns `0`
  - `jq '[.[] | select(.from_id | IN(["the","actual","id","list"]))] | length'` — all `from_id` and `to_id` values appear in `jq '[.entries[].id]' index.json` (no orphan endpoints)
  - Count of xref edges where `from_id` matches `UpDetail.js` path is ≥10 (R2 mitigation per spec REQ-12)
- **Estimated entries / LOC**: ~80-100 xref edges; `xref.json` ~25 KB; `xref.md` ~8 KB; `build-xref.sh` ~100 LOC
- **Notes**:
  - Pipeline stages per design §C5: Stage 1 namespace writes (`rg 'MX60\.\w+ =' --type js`), Stage 2 namespace reads (`rg 'MX60\.\w+' --type js`), Stage 3 subtract defines from reads (net xref graph), Stage 4 Java FQN refs (`rg 'BChi\w+' --type java`), Stage 5 envelope assembly
  - `usage_kind` enum: `defines | reads-global | invokes-java`
  - `evidence` format: `"filename.js:lineN"`
  - UpDetail.js has the highest expected outgoing edge count (~37 slot writes + reads from SubscriptionPool/WritePoint/ConfigManager/Router)
  - Use `rg --type js` NOT `grep` per tool rules (#309)

---

## Phase C — Aggregation

### [x] T-C1: Merge shard JSONs into final index.json with full envelope

- **Depends on**: T-B1, T-B2, T-B3, T-B4, T-B5, T-B6 (ALL 6 mapping shards must be complete)
- **Owner**: sdd-apply (jq scripting)
- **Inputs**:
  - `docs/mappings/mx60-chihuahua/shards/s1-backend-rt.json`
  - `docs/mappings/mx60-chihuahua/shards/s2-backend-ux.json`
  - `docs/mappings/mx60-chihuahua/shards/s3-frontend-core.json`
  - `docs/mappings/mx60-chihuahua/shards/s4-frontend-equipment.json`
  - `docs/mappings/mx60-chihuahua/shards/s5-frontend-alarms-schedules.json`
  - `docs/mappings/mx60-chihuahua/shards/s6-resources-config.json` (also contributes exclusions[])
  - Design §B (envelope shape), Spec REQ-2 (envelope fields)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/index.json`
  - `docs/mappings/mx60-chihuahua/scripts/merge-shards.sh`
- **Acceptance** (REQ-2, REQ-13):
  - `jq '{schema_version, module, source_repo, generated_at}' index.json` → `schema_version: "1.0"`, `module: "mx60-chihuahua"`, `source_repo` non-empty, `generated_at` ISO 8601
  - `jq '.entries | type' index.json` returns `"array"`
  - `jq '.exclusions | type' index.json` returns `"array"` with ≥3 exclusion objects
  - `jq '.entries | length' index.json` is between 100 and 140 (REQ-13 target range)
  - `jq '.' index.json > /dev/null` exits with code 0 (valid JSON)
  - `jq '[.entries[] | select(.id == null or .path == null or .kind == null or .domain == null or .purpose == null or .loc == null or .status == null)] | length' index.json` returns `0`
- **Estimated entries / LOC**: merge script ~30 LOC; index.json ~140 KB total
- **Notes**:
  - Merge jq: `jq -s '{schema_version: "1.0", module: "mx60-chihuahua", extensions: ["backend","frontend_iife"], source_repo: "...", generated_at: (now | todate), generator: "sdd-apply mapping-mx60 v1", entries: [.[].entries] | flatten, exclusions: [.[].exclusions // []] | flatten | unique}' shards/*.json`
  - Deduplicate entries by `id` (in case any path appears in two shards by mistake): `group_by(.id) | map(.[0])`
  - Validate id uniqueness: `jq '[.entries | group_by(.id) | .[] | select(length > 1) | .[0].id] | length' index.json` must return `0`
  - Run prohibited field scan: `jq '[.entries[] | keys | map(select(IN("from","caller","file","callers","used_by","edges","source_path","name")))] | flatten | length' index.json` must return `0`
  - This task UNBLOCKS T-B7, T-B8, T-C2, T-C3, T-C4, T-D1..T-D17

---

### [x] T-C2: Generate index.md from index.json

- **Depends on**: T-C1
- **Owner**: sdd-apply (bash + jq)
- **Inputs**:
  - `docs/mappings/mx60-chihuahua/index.json`
  - Design §C (C2 build-index-md.sh)
  - `docs/mappings/reflow-clean-177/index.md` (format reference)
  - Spec REQ-6 (dual-form, path-sorted, consistent row count)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/index.md`
  - `docs/mappings/mx60-chihuahua/scripts/build-index-md.sh`
- **Acceptance** (REQ-6):
  - `jq '.entries | length' index.json` matches `rg '^\|' index.md | rg -v '^\|[-|]+\|$' | wc -l` minus 1 (header row) — counts match
  - `jq '.' index.json > /dev/null` exits 0 (index.json still valid — not modified by this task)
  - `index.md` rows are sorted ascending by the `path` column
  - Every row has the same number of `|` delimiters as the header row
- **Estimated entries / LOC**: `index.md` ~35 KB; `build-index-md.sh` ~60 LOC
- **Notes**:
  - Script: bash heredoc + `jq -r` producing Markdown table. Include summary block at top (kind distribution, domain distribution) as in reflow index.md pattern
  - Sort: `jq '.entries | sort_by(.path)[]'` before rendering
  - Columns (minimum): `path | kind | domain | purpose | loc | status`

---

### [x] T-C3: Generate excluded.md from envelope exclusions

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**:
  - `docs/mappings/mx60-chihuahua/index.json` (`.exclusions[]` array)
  - `docs/mappings/reflow-clean-177/excluded.md` (format reference)
  - Spec REQ-10 (16 test files named individually, reason strings)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/excluded.md`
- **Acceptance** (REQ-10):
  - `rg 'srcTest' docs/mappings/mx60-chihuahua/excluded.md` returns ≥16 matches (one per test file)
  - `rg '\.idea|\.gradle|build/' docs/mappings/mx60-chihuahua/excluded.md` returns ≥3 matches
  - `jq '.exclusions | length' index.json` ≥ 3
  - No srcTest path appears in `jq '[.entries[].path]' index.json` (exclusions are NOT in entries)
- **Estimated entries / LOC**: `excluded.md` ~5 KB; ~60 LOC
- **Notes**:
  - List the 16 test java files individually with reason: `"Test files excluded per mapping convention"`
  - Note that Niagara test discovery is broken for plugin 7.3.40 (reference HANDOFF.md)
  - Outer Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`) also documented

---

### [x] T-C4: Author README.md with rg/jq usage examples

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**:
  - `docs/mappings/mx60-chihuahua/index.json` (for example queries that must actually work)
  - `docs/mappings/reflow-clean-177/README.md` (structure reference)
  - Spec REQ-6 (dual-form mentioned), REQ-12 (xref queries)
  - Design §D shard descriptions (for module overview)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/README.md`
- **Acceptance**:
  - `rg '^### ' docs/mappings/mx60-chihuahua/README.md | wc -l` ≥ 5 (at least 5 query examples)
  - Every `jq` example in README uses `index.json` path or `xref.json` path that actually exists
  - `rg 'iife-app|iife-store|iife-lib' README.md` returns ≥3 matches (IIFE-specific examples present)
  - README includes a "kind family mapping" table explaining reflow `js-lib` → MX60 `iife-lib` cross-mapping (per design §D1 rationale)
- **Estimated entries / LOC**: ~9 KB; ~120 LOC
- **Notes**:
  - Include ≥5 rg + ≥6 jq examples per proposal §3
  - Must include: IIFE dependency query (`jq '.entries[] | select(.frontend_iife.globals_read | contains(["MX60.SubscriptionPool"]))'`), delta status filter, xref edge count for a specific node, backend rest_endpoints filter, coverage summary
  - Include kind-family mapping table: reflow kind → MX60 equivalent + notes

---

## Phase D — Domain documents

All T-D tasks depend on T-C1 (index.json must exist). All T-D tasks are parallel to each other and to T-B7, T-B8. Each domain doc follows the 5-section template from design §G: Overview, Entry points, Components / classes, Data Flow / Integration Points, Notes & Cross-References.

**Template gate**: `rg '^## ' domains/<name>.md` must output exactly: `Overview`, `Entry points`, `Components / classes`, `Data Flow / Integration Points`, `Notes & Cross-References` — in that order.

---

### [x] T-D1: Author `domains/service-container.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S1 shard entries for domain `service-container`; explore §3 service-container row; proposal §6 §68.1 row
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/service-container.md`
- **Acceptance** (REQ-7): 5 sections present in order; `BChiDashboardService.java` in Entry points table; `controlTick` timing claim carries `"(inferred from mapping)"` annotation (REQ-14)
- **Estimated entries / LOC**: ~120 LOC
- **Notes**: v4 4-level hierarchy diagram in Data Flow section (BChiDashboardService → BPlanta → [BChiUpMonitor, BChiCarcamoMonitor, BChiDataloggerMonitor]); reference §68.2 (BChiServlet → BReflowService) in Notes

---

### [x] T-D2: Author `domains/equipment-backend.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S1 shard entries for domain `equipment-backend`; explore §3 equipment-backend row
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/equipment-backend.md`
- **Acceptance** (REQ-7): 5 sections; all 3 equipment BComponent types (BChiUp, BChiCarcamo, BChiDatalogger) + 3 monitors in Components table; domain listed as `NUEVO` vs reflow (no reflow analog)
- **Estimated entries / LOC**: ~110 LOC
- **Notes**: `BChiUp` 37-slot schema is the source for UpDetail.js §68.3 xref; note that `@NiagaraProperty` slot declarations drive xref cardinality

---

### [x] T-D3: Author `domains/http-rest.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S2 shard entries for domain `http-rest`; explore §2 REST endpoint list; proposal §6 §68.2
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/http-rest.md`
- **Acceptance** (REQ-7): 5 sections; REST endpoint table lists all ≥13 endpoints from explore §2; `BChiServlet` vs `BReflowService` comparison in Notes (§68.2); `ChiServletDispatch` pure-Java routing noted
- **Estimated entries / LOC**: ~130 LOC
- **Notes**: Data Flow section shows request routing: BChiServlet → ChiServletDispatch → domain helpers (ChiEquipmentReader, ChiAlarmHelper, etc.)

---

### [x] T-D4: Author `domains/equipment-reader.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S2 shard entries for domain `equipment-reader`; explore §3 equipment-reader row
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/equipment-reader.md`
- **Acceptance** (REQ-7): 5 sections; Layer-1/Layer-2 DTO pattern described in Data Flow; `ChiThresholdHelper` linked to frontend `UpThresholdStore`
- **Estimated entries / LOC**: ~100 LOC
- **Notes**: BOrd walk pattern for 4-level v4 hierarchy; note LOC estimates (~900 total for reader+helper)

---

### [x] T-D5: Author `domains/alarms-backend.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S2 shard entries for domain `alarms-backend`; explore §3 alarms-backend row; proposal §6 §68.4
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/alarms-backend.md`
- **Acceptance** (REQ-7): 5 sections; NO BReflowChannelService analog noted explicitly (FALTA vs reflow); §68.4 ack flow referenced
- **Estimated entries / LOC**: ~110 LOC
- **Notes**: `ChiAlarmHelper` + `ChiAlarmQueryHelper`; BAlarmDatabase query pattern; NO WebSocket push (explore §3 confirmed)

---

### [x] T-D6: Author `domains/history-backend.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S2 shard entries for domain `history-backend`; explore §3 history-backend row; proposal §6 §68.1
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/history-backend.md`
- **Acceptance** (REQ-7): 5 sections; `ChiHistoryHelper` as §68.1 ANÁLOGO candidate noted; Engine+Serializer split rationale referenced
- **Estimated entries / LOC**: ~100 LOC
- **Notes**: ~400 LOC file; "Port of SnlsHistoryHelper" if the port-marker is present — cite actual file:line

---

### [x] T-D7: Author `domains/schedules-backend.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S2 shard entries for domain `schedules-backend`; explore §3 schedules-backend row
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/schedules-backend.md`
- **Acceptance** (REQ-7): 5 sections; BQL BNumericSchedule query pattern in Data Flow; `ChiScheduleHelper` ~350 LOC noted
- **Estimated entries / LOC**: ~90 LOC

---

### [x] T-D8: Author `domains/util-backend.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S2 shard entries for domain `util-backend`; explore §3 util-backend row
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/util-backend.md`
- **Acceptance** (REQ-7): 5 sections; `ChiJsonUtil` stateless pattern noted; `dependencies: []` confirmed (no BComponent deps)
- **Estimated entries / LOC**: ~80 LOC

---

### [x] T-D9: Author `domains/app-shell.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S3 shard entries for domain `app-shell`; explore §3 app-shell row
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/app-shell.md`
- **Acceptance** (REQ-7): 5 sections; hash-based routing (NOT Vue Router) explicitly noted; `DashboardApp.js` as bootstrap entry point; `ConfigManager.js` global config source
- **Estimated entries / LOC**: ~110 LOC
- **Notes**: IIFE namespace bootstrap sequence in Data Flow (SharedEnv → ConfigManager → Router → DashboardApp)

---

### [x] T-D10: Author `domains/baja-integration.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S3 shard entries for domain `baja-integration`; explore §3 baja-integration row; proposal §6 §68.5; REQ-14
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/baja-integration.md`
- **Acceptance** (REQ-7, REQ-14): 5 sections; `SubscriptionPool.js` BajaScript lifecycle claims carry `"(inferred from mapping)"` annotations; `WritePoint.js` `_bajaSetBroken` dual-path fallback annotated as inferred; §68.5 (SubscriptionPool → useSubscriber) referenced in Notes
- **Estimated entries / LOC**: ~120 LOC
- **Notes**: This is the highest-risk domain for REQ-14 compliance — review all behavioral claims carefully

---

### [x] T-D11: Author `domains/ui-lib.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S3 shard entries for domain `ui-lib`; explore §3 ui-lib row
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/ui-lib.md`
- **Acceptance** (REQ-7): 5 sections; all 7 utility modules listed (Toast, Confirm, StatusResolver, Dropdown, Popover, RelativeTime, CsvExport)
- **Estimated entries / LOC**: ~95 LOC

---

### [x] T-D12: Author `domains/equipment-frontend.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S4 shard entries for domain `equipment-frontend`; explore §3 equipment-frontend row
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/equipment-frontend.md`
- **Acceptance** (REQ-7): 5 sections; 3 equipment types (UP/Carcamo/Datalogger) referenced in Components; `EquipmentSnapshotStore` as the central data cache noted
- **Estimated entries / LOC**: ~120 LOC

---

### [x] T-D13: Author `domains/equipment-detail.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S4 shard entries for domain `equipment-detail`; explore §3 equipment-detail-up + equipment-detail-misc rows; Proposal §D3 (UpDetail single-entry decision); REQ-7 (UpDetail.js as primary entry point)
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/equipment-detail.md`
- **Acceptance** (REQ-7): 5 sections; `UpDetail.js` explicitly in Entry points as primary; purpose ≤150 chars confirmed in Components table; MANUAL/SETPOINT/SCHEDULE modes listed
- **Estimated entries / LOC**: ~130 LOC
- **Notes**: Merged domain (equipment-detail-up + equipment-detail-misc per proposal §3); UpDetail's 37-slot panel, threshold UI, history chart as sub-responsibilities in Overview

---

### [x] T-D14: Author `domains/alarms-frontend.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S5 shard entries for domain `alarms-frontend`; explore §3 alarms-frontend row; proposal §6 §68.4
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/alarms-frontend.md`
- **Acceptance** (REQ-7): 5 sections; inline ack (no AlarmAckConfirm modal) explicitly noted; `AlarmModalActions + AlarmNotesModal` as §68.4 ANÁLOGO noted; `AlarmLatchStore` as dedicated latch-state store
- **Estimated entries / LOC**: ~130 LOC
- **Notes**: ~3000 LOC total for this domain — the largest frontend domain; bulk-ack via `BulkActionBar` + individual ack via `AlarmDetailPage` are the two ack paths

---

### [x] T-D15: Author `domains/schedules-frontend.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S5 shard entries for domain `schedules-frontend`; explore §3 schedules-frontend row
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/schedules-frontend.md`
- **Acceptance** (REQ-7): 5 sections; `ScheduleView.js` in Entry points; iframe modal for Niagara native schedule editor noted; BWeeklySchedule auto-discover annotated as `"(inferred from mapping)"`
- **Estimated entries / LOC**: ~90 LOC

---

### [x] T-D16: Author `domains/history-frontend.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S4 shard entries for domain `history-frontend` (`LiveHistoryBuffer.js`, `TimeRangePicker.js`); explore §3 history-frontend row
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/history-frontend.md`
- **Acceptance** (REQ-7): 5 sections; ring-buffer pattern described; no historyCache.js analog in MX60 noted (FALTA vs reflow); REST-driven (not BajaScript subscriber)
- **Estimated entries / LOC**: ~90 LOC

---

### [x] T-D17: Author `domains/threshold-stores.md`

- **Depends on**: T-C1
- **Owner**: sdd-apply (content-write)
- **Inputs**: S4 shard entries for domain `threshold-stores`; explore §3 threshold-stores row; proposal §6 §68.3
- **Outputs**: `docs/mappings/mx60-chihuahua/domains/threshold-stores.md`
- **Acceptance** (REQ-7): 5 sections; all 5 threshold stores listed; `NUEVO` vs reflow (no reflow analog) explicitly stated; §68.3 (IIFE store → Pinia) referenced
- **Estimated entries / LOC**: ~100 LOC
- **Notes**: `ModoOverrideStore` + `OutputOverrideStore` are per-equipment override stores; in-memory only (no serialization); `persistent: false` confirmed for all

---

## Phase E — Validation

### [x] T-E1: Run validate-shard.jq across full index.json + run all per-shard acceptance tests

- **Depends on**: T-C1, T-B7, T-B8 (all primary artifacts must exist)
- **Owner**: sdd-apply (jq scripting + execution)
- **Inputs**:
  - `docs/mappings/mx60-chihuahua/index.json`
  - `docs/mappings/mx60-chihuahua/delta.json`
  - `docs/mappings/mx60-chihuahua/xref.json`
  - `docs/mappings/mx60-chihuahua/scripts/validate-shard.jq` (must be authored in this task if not done in shard tasks)
  - Design §C (C1 validate-shard.jq spec), Design §H (4 validation tiers)
  - Spec REQ-1, REQ-2, REQ-3, REQ-4, REQ-5, REQ-10, REQ-11, REQ-12, REQ-13
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/scripts/validate-shard.jq`
  - `docs/mappings/mx60-chihuahua/_validation.md` (partial — structural checks section)
- **Acceptance** (REQ-1, REQ-2, REQ-3, REQ-4, REQ-5, REQ-10, REQ-11, REQ-12, REQ-13):
  - `jq -f scripts/validate-shard.jq index.json` outputs empty string (no violations)
  - `jq '[.entries[] | select(.id == null or .path == null or .kind == null or .domain == null or .purpose == null or .loc == null or .status == null)] | length' index.json` returns `0`
  - `jq '[.entries[] | select(.kind == "java-class") | select(.backend == null)] | length' index.json` returns `0`
  - `jq '[.entries[] | select(.kind | test("^iife-")) | select(.frontend_iife == null)] | length' index.json` returns `0`
  - `jq '[.entries[] | select(.frontend_iife != null) | select(.kind | test("^iife-") | not)] | length' index.json` returns `0` (no non-iife entry has frontend_iife block)
  - `jq '[.entries[].kind] | unique | map(select(IN("vue-component","vue-view","js-store","js-mixin","js-plugin","js-router"))) | length' index.json` returns `0` (no reflow-only kinds)
  - `jq '[.entries[] | .purpose | length > 150] | any' index.json` returns `false`
  - Prohibited fields scan returns `0`
  - Id uniqueness check returns `0`
  - Per-shard cap: each shard domain grouping ≤75 entries
- **Estimated entries / LOC**: `validate-shard.jq` ~80 LOC; `_validation.md` partial ~5 KB
- **Notes**:
  - `validate-shard.jq` must check ALL constraints from design §C1: core fields, kind/status enums, purpose ≤150 chars, id==path, prohibited fields, frontend_vue forbidden, frontend_js+iife-* mutual exclusion, frontend_iife required for iife-*, backend required for java-class, iife_pattern enum, subscriber_role enum
  - Record each check result in `_validation.md` tier table

---

### [x] T-E2: Compute coverage % and fidelity sample + complete `_validation.md`

- **Depends on**: T-E1
- **Owner**: sdd-apply (bash + rg/fd)
- **Inputs**:
  - `docs/mappings/mx60-chihuahua/index.json`
  - Source root: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/`
  - `docs/mappings/mx60-chihuahua/scripts/coverage.sh`
  - Spec REQ-8 (≥95% coverage), REQ-9 (verified_at ≥40)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/scripts/coverage.sh`
  - `docs/mappings/mx60-chihuahua/_validation.md` (complete)
- **Acceptance** (REQ-8, REQ-9):
  - `jq '[.entries[] | select(.status == "source")] | length' index.json` / fd source count ≥ 0.95
  - `jq '[.entries[] | select(.verified_at != null)] | length' index.json` ≥ 40
  - `jq '[.entries[] | select(.source_doc != null) | select(.source_doc.file == null or .source_doc.section == null)] | length' index.json` returns `0` (no malformed source_doc)
  - `_validation.md` contains a completed table with all 4 tiers from design §H
- **Estimated entries / LOC**: `coverage.sh` ~40 LOC; `_validation.md` ~15 KB
- **Notes**:
  - Coverage formula: `jq '[.entries[] | select(.status == "source")] | length' index.json` divided by `fd '\.(java|js)$' /home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/ --exclude srcTest --exclude .git --exclude build | wc -l`
  - If coverage < 95%, identify which files are missing and re-process the relevant shard
  - `_validation.md` must document: (1) structural check results from E1, (2) coverage %, (3) verified_at count, (4) kind distribution table, (5) domain distribution table, (6) shard cap compliance table

---

### [x] T-E3: Spot-check ≥40 entries against source files; set `verified_at` + document fidelity

- **Depends on**: T-E2
- **Owner**: sdd-apply (read + write)
- **Inputs**:
  - `docs/mappings/mx60-chihuahua/index.json`
  - Source files under `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/` (READ-ONLY per #309)
  - Spec REQ-8 (≥90% fidelity), REQ-9 (verified_at format)
- **Outputs**:
  - `docs/mappings/mx60-chihuahua/index.json` (updated `verified_at` on ≥40 entries)
  - `docs/mappings/mx60-chihuahua/_validation.md` (spot-check results appended)
- **Acceptance** (REQ-8, REQ-9):
  - `jq '[.entries[] | select(.verified_at != null)] | length' index.json` ≥ 40
  - Fidelity score ≥ 90% documented in `_validation.md` (≥36 of 40 entries have correct `purpose`)
  - `jq '[.entries[] | select(.verified_at != null) | .verified_at] | map(test("^[0-9]{4}-[0-9]{2}-[0-9]{2}T"))| all' index.json` returns `true` (ISO 8601 format)
  - `rg 'inferred from mapping' docs/mappings/mx60-chihuahua/ -r | wc -l` ≥ 10 (REQ-14 floor)
- **Estimated entries / LOC**: ~2 LOC changes per verified entry in index.json; ~3 KB addition to `_validation.md`
- **Notes**:
  - Sample strategy: ≥2 entries per domain × 17 domains = 34 + 6 extras from high-risk domains (baja-integration, equipment-detail, alarms-frontend) = 40 minimum
  - Verify: open source file, check that `purpose` text correctly describes primary responsibility without contradiction
  - Set `verified_at: "2026-05-09T00:00:00Z"` (use session date) on each verified entry
  - If any domain fails fidelity (<90% within that domain's sample), re-process that shard before final commit
  - Final check: `rg 'inferred from mapping' docs/mappings/mx60-chihuahua/ -r | wc -l` ≥ 10 for REQ-14

---

## Review Workload Forecast

| Metric | Value |
|--------|-------|
| Estimated total entries (index.json) | 100–130 source + ~25 config/resource = 125–155 total |
| Estimated total xref entries | 80–100 edges |
| Estimated total delta rows | 80–100 rows |
| Estimated changed files (new) | 29 files: 1 schema.md + 1 README.md + 1 excluded.md + 1 index.json + 1 index.md + 17 domain docs + 1 delta.json + 1 delta-vs-reflow.md + 1 xref.json + 1 xref.md + 1 _validation.md + 5 scripts (merge-shards.sh, build-index-md.sh, build-delta.sh, build-xref.sh, coverage.sh, validate-shard.jq) + 6 intermediate shard JSONs under shards/ |
| Estimated changed files (modified) | 0 (no existing artifacts touched) |
| Estimated changed lines (LOC) | ~2,800–3,500 LOC total: index.json ~1,200 LOC, domain docs ~17×110 = 1,870 LOC, delta.json ~400 LOC, xref.json ~250 LOC, scripts ~380 LOC, schema+README+excluded+index.md ~460 LOC, _validation.md ~150 LOC |
| Number of PRs recommended | 3 chained PRs (see slice breakdown below) |
| 400-line budget risk | High (total ~3,200 LOC far exceeds single-PR budget) |
| Chained PRs recommended | Yes |
| Decision needed before apply | No (delivery_strategy: auto-chain — orchestrator proceeds automatically) |

**Recommended chained PR slices for auto-chain**:

| PR | Slice | Tasks | Approx LOC |
|----|-------|-------|------------|
| PR-1 (Schema + Backend) | Slices 1–2 | A1, A2, B1, B2, B6 | ~700 LOC |
| PR-2 (Frontend + Aggregation) | Slices 3–4 | B3, B4, B5, C1, C2, C3, C4 | ~1,200 LOC |
| PR-3 (Domains + Delta + Xref + Validation) | Slices 5–6 | D1–D17, B7, B8, E1, E2, E3 | ~1,500 LOC |

---

## Acceptance summary — REQ traceability

| REQ | Description | Enforcing tasks |
|-----|-------------|----------------|
| REQ-1 | Core fields mandatory on every entry | T-B1..T-B6 (per shard), T-C1 (merge validation), T-E1 (full scan) |
| REQ-2 | Top-level JSON envelope fields | T-C1 (merge + envelope), T-E1 (validation) |
| REQ-3 | `backend` block for every java-class entry | T-B1 (S1), T-B2 (S2), T-E1 (full scan) |
| REQ-4 | `frontend_iife` block for every iife-* entry | T-B3 (S3), T-B4 (S4), T-B5 (S5), T-E1 (full scan) |
| REQ-5 | 5 new IIFE kind values in schema.md + no unknown kinds in index.json | T-A2 (schema), T-E1 (kind enum scan) |
| REQ-6 | Dual-form index (MD + JSON), path-sorted, consistent count | T-C1 (JSON), T-C2 (MD), T-E1 (count match) |
| REQ-7 | 17 domain deep-dive docs with 5-section template | T-D1..T-D17, T-E1 (template gate) |
| REQ-8 | Coverage ≥95%, fidelity ≥90% | T-E2 (coverage), T-E3 (spot-check) |
| REQ-9 | verified_at ≥40 entries; source_doc non-null where applicable | T-E3 (spot-check + verified_at), T-E1 (source_doc scan) |
| REQ-10 | Excluded paths in excluded.md + exclusions[] array | T-B6 (exclusions in shard), T-C1 (merge exclusions), T-C3 (excluded.md) |
| REQ-11 | Delta dual-form with required schema, status enum, bloque68_section | T-B7 (delta.json + delta-vs-reflow.md) |
| REQ-12 | Xref ≥80 edges, valid schema, no orphans | T-B8 (xref.json + xref.md), T-E1 (orphan check) |
| REQ-13 | No shard exceeds 75 entries; total 100–140 | T-B1..T-B6 (per-shard caps), T-C1 (total count), T-E1 (shard grouping check) |
| REQ-14 | Runtime claims annotated as inferred | T-B3 (SubscriptionPool/WritePoint), T-B4 (UpDetail), T-D10 (baja-integration domain), T-E3 (rg count ≥10) |

---

## skill_resolution
- injected (#1231 multi-shard cap + canonical JSON example rule applied to B1..B6 acceptance criteria; #309 read-only on modulos_niagara_n4 + rg/fd/bat/jq tooling + Rioplatense convention applied throughout; #1238 inferred-from-mapping annotation applied to REQ-14 tasks B3, B4, B5, D10, D15, E3)
