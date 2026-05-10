# SDD Explore — mapping-mx60

**Date**: 2026-05-09
**Source**: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/`
**Output target**: `/home/cristian/niagara-research/docs/mappings/mx60-chihuahua/` (does not exist yet)
**Template reference**: `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/`
**Engram artifact**: `sdd/mapping-mx60/explore` (obs-id 1247)

---

## 1. Source structure inventory

### Nesting resolution

The outer `chihuahua/` is the **Git + Gradle wrapper repo root** — contains `.git/`, `gradle/wrapper/`, `build.gradle.kts`, `settings.gradle.kts`. The inner `chihuahua/chihuahua/` is the **Gradle project root** (confirmed by `niagara-module.xml` at `chihuahua/chihuahua/niagara-module.xml:2`: `<niagara-module moduleName="chihuahua" preferredSymbol="chihua" runtimeProfiles="rt,ux"/>`). The double-nesting is the standard Tridium multi-project layout — NOT a bug or artifact of copying.

**Effective source root**: `chihuahua/chihuahua/` within the repo.

### Sub-module breakdown

| Sub-module | Gradle profile | Java package | Role |
|------------|---------------|--------------|------|
| `chihuahua-rt` | rt | `com.angeles.chihuahua.components` | BComponent service tree + equipment BComponents + control tick |
| `chihuahua-ux` | ux | `com.angeles.chihuahua.ux` | Servlet + REST helpers + BajaScript-classic IIFE SPA frontend |

### Effective source file counts (verified against filesystem)

| Extension | Count | Where |
|-----------|-------|-------|
| `.java` (source) | 17 | rt/src: 8, ux/src: 9 |
| `.java` (test, excluded) | 16 | rt/srcTest: 6, ux/srcTest: 10 |
| `.js` (app IIFE) | 36 | ux/src/rc/js/app/ |
| `.js` (lib IIFE) | 1 | ux/src/rc/js/lib/ (WritePoint.js) |
| `.js` (util IIFE) | 3 | ux/src/rc/js/util/ |
| `.js` (ext — bundled 3rd party) | 5 | rc/ext/chartjs/ (2), rc/ext/threejs/ (3) |
| `.html` | 1 | rc/index.html |
| `.css` | 1+ | rc/css/ |
| Images + fonts | ~30 | rc/img/, rc/fonts/ |
| Gradle/config | ~8 | gradle.kts, gradle.properties, niagara-module.xml, palettes, lexicons |

**CONFIRMED: 0 `.vue` files.** This is not a gap — it is the design. MX60 is architecturally IIFE-based, not Vue.

### Build system

Gradle Kotlin DSL, Niagara plugin 7.3.40 (build) / 7.3.0 (settings). NRE annotation processor (`@NiagaraType`, `@NiagaraProperty`) drives Slot-o-Matic codegen.

---

## 2. Stack confirmation (empirical, with evidence)

### Frontend: BajaScript-classic IIFE SPA

This is the most important finding for anyone coming from the reflow mental model. MX60 has **no Vue, no Vuex, no module bundler, no npm**. Every JS file is an IIFE (`(function(window) { 'use strict'; ... })(window)`) writing to a global `window.MX60` namespace.

Evidence:
- `chihuahua-ux/src/rc/js/app/DashboardApp.js:18` — `(function(window) { 'use strict'; var MX60 = window.MX60 || {};`
- `chihuahua-ux/src/rc/js/app/AlarmsManager.js:30` — same IIFE pattern
- `chihuahua-ux/src/rc/js/app/SubscriptionPool.js:28` — same, uses real `baja.Ord.make(ord).get({subscriber: ...})` calls
- All 40 JS files: ES5 strict — `var` declarations, no arrow functions, no template literals, no `import`/`export`

BajaScript is used for **live point subscriptions** (`SubscriptionPool.js`) and **point writes** (`WritePoint.js:38` — `baja.Ord.make(ord).get().then(p => p.set(value))`). REST polling is the fallback path.

### Backend: Niagara N4 RT + UX module

- `BChiDashboardService.java:55` — `extends BAbstractService` (RT profile)
- `BChiServlet.java:51` — `extends BWebServlet` (UX profile)
- `BChiUp.java` — `@NiagaraType`, 37-slot BComponent, pure property container (no logic)
- `chihuahua-ux.gradle.kts:46-54` — depends on `:web-rt`, `:alarm-rt`, `:bql-rt`, `:history-rt`, `:control-rt`, `:schedule-rt`, `:bajaScript-ux`
- REST endpoints under `/mx60/`: config, equipment, alarms, alarmCounts, historyList, historyData, equipment-histories, schedules, csrf-probe, setpoint, alarms/ack, alarms/ack-all, thresholds

### Architecture v4 component hierarchy (current state)

```
ChiDashboardService (BAbstractService, RT)
  Planta1..Planta6 (BPlanta)
    UpMonitor (BChiUpMonitor) → BChiUp ×0..25
    CarcamoMonitor (BChiCarcamoMonitor) → BChiCarcamo ×0..2
    DataloggerMonitor (BChiDataloggerMonitor) → BChiDatalogger ×0..2
mx60 (BChiServlet, UX, URL prefix "/mx60/")
  └─ routes via ChiServletDispatch (pure-Java, no Niagara dependency)
```

No ORD scheme, no WebSocket server, no `BReflowScheme` equivalent. Navigation is hash-based client-side routing in `Router.js`.

---

## 3. Domain decomposition

| Domain | Reflow analog | MX60 source files (count) | LOC estimate | Shard count | Status |
|--------|--------------|--------------------------|--------------|-------------|--------|
| `service-container` | service-container | BChiDashboardService.java, BPlanta.java | ~760 | 1 | ANÁLOGO (different hierarchy depth: 4-level vs reflow 2-level) |
| `equipment-backend` | n/a | BChiUp.java, BChiCarcamo.java, BChiDatalogger.java, BChiUpMonitor.java, BChiCarcamoMonitor.java, BChiDataloggerMonitor.java | ~900 | 1 | NUEVO (MX60-specific 3 equipment types) |
| `http-rest` | http-rest | BChiServlet.java, ChiServletDispatch.java | ~1900 | 1 | ANÁLOGO (single servlet vs reflow BaseServlet hierarchy) |
| `equipment-reader` | n/a | ChiEquipmentReader.java, ChiThresholdHelper.java | ~900 | 1 | ANÁLOGO (no reflow equivalent; ported from SanLuis) |
| `alarms-backend` | alarms-backend | ChiAlarmHelper.java, ChiAlarmQueryHelper.java | ~700 | 1 | HEREDADO base, REESCRITO query layer |
| `history-backend` | history-backend | ChiHistoryHelper.java | ~400 | 1 | HEREDADO (port of SnlsHistoryHelper) |
| `schedules-backend` | n/a (reflow: frontend-only) | ChiScheduleHelper.java | ~350 | 1 | NUEVO (no reflow backend helper) |
| `util-backend` | util-backend | ChiJsonUtil.java | ~300 | 1 | ANÁLOGO |
| `app-shell` | app-shell | DashboardApp.js, Router.js, ConfigManager.js, SharedEnv.js | ~800 | 1 | ANÁLOGO (IIFE namespace orchestrator vs Vue+Vuex) |
| `equipment-frontend` | equipment (Vue) | EquipmentData.js, EquipmentCard.js, EquipmentDetail.js, EquipmentSnapshotStore.js, HomeMap.js | ~2000 | 1 | ANÁLOGO (same functional role, completely different stack) |
| `equipment-detail-up` | n/a (merged in reflow) | UpDetail.js (~2400 LOC) | ~2400 | 1 | NUEVO (no equivalent; largest single file) |
| `equipment-detail-misc` | n/a | CarcamoDetail.js, DataloggerDetail.js | ~700 | 1 | NUEVO |
| `alarms-frontend` | alarms-frontend | AlarmsManager.js, AlarmsPage.js, AlarmCards.js, AlarmDetailsTable.js, AlarmDetailPage.js, AlarmLatchStore.js, AlarmModalActions.js, AlarmNotesModal.js, BulkActionBar.js | ~3000 | 1 | ANÁLOGO (alarm lifecycle same, no Vue component hierarchy) |
| `schedules-frontend` | schedules-frontend | ScheduleView.js | ~400 | 1 | HEREDADO (ported verbatim from SanLuis) |
| `history-frontend` | histories-frontend | LiveHistoryBuffer.js, TimeRangePicker.js | ~500 | 1 | ANÁLOGO (ring buffer + REST; no historyCache.js equivalent) |
| `baja-integration` | api-layer (partial) | SubscriptionPool.js, WritePoint.js | ~600 | 1 | ANÁLOGO (explicit layer vs embedded in Vue plugins in reflow) |
| `ui-lib` | lib | Toast.js, Confirm.js, StatusResolver.js, Dropdown.js, Popover.js, RelativeTime.js, CsvExport.js | ~700 | 1 | ANÁLOGO |
| `threshold-stores` | n/a | ModoOverrideStore.js, OutputOverrideStore.js, UpThresholdStore.js, CarcamoThresholdStore.js, DataloggerThresholdStore.js | ~490 | 1 | NUEVO (no Vuex equivalent in reflow) |
| `module-descriptor` | module-descriptor | niagara-module.xml, ×2 palette, ×2 permissions.xml, ×2 lexicon | ~10 items | 1 | ANÁLOGO |
| `build-config` | build-config | ×3 gradle.kts, gradle.properties | ~4 files | 1 | ANÁLOGO |
| `static-resources` | image-library + bundle-output | index.html, ×17 JPGs, ×5 fonts, ext/ (5 bundled JS) | ~30 items | 1 | ANÁLOGO |

**Domains FALTA** (in reflow, absent in MX60):
- `floorplans` — reflow has PxView SVG floor system; MX60 has HomeMap.js (zone polygon overlay on static JPEG — belongs to `equipment-frontend`, not a separate domain)
- `buildings` — reflow multi-building config; MX60 has plantas (physical floors of a single facility)
- `websocket-ui` — no WebSocket server in MX60; BajaScript direct subscription is the live-data layer
- `state` (Vuex root) — no Vuex; IIFE per-concern stores
- `profiles-rbac` — no RBAC module; Niagara handles auth at servlet level
- `weather` — no weather module in MX60

---

## 4. Schema applicability check

Schema v1.0 core fields all apply. Three extension blocks need attention:

| Extension block | Applies? | Action |
|-----------------|----------|--------|
| `backend` | YES — unchanged | All java-class entries get `profile` (rt\|ux), `package`, `bcomponent_type`, `slots`, `actions`, `rest_endpoints`, `box_methods`, `decompiled: false` |
| `frontend_vue` | NO | Skip entirely — no Vue files |
| `frontend_js` | PARTIAL | `module_type` enum needs extension: add `iife-app`, `iife-store`, `iife-lib`, `iife-util`, `iife-entry`. `persistent: false` for all. `exports` maps to `MX60.XxxModule` assignments |
| `frontend_iife` (NEW) | YES — needed | Proposed: `{ "module_type": "iife-app|iife-store|iife-lib|iife-util", "namespace": "MX60", "globals_written": [...], "globals_read": [...] }` — captures the runtime namespace dependency graph (replaces static `import` statements that don't exist) |

Three fields from schema.md verified applicable to MX60:
1. `kind: "java-class"` — applies to all 17 source `.java` files (evidence: `BChiDashboardService.java:55`)
2. `backend.rest_endpoints` — applies to `BChiServlet.java` entries (evidence: routes declared at `BChiServlet.java:26-38`)
3. `source_doc` — applies; points to HANDOFF.md and openspec change docs

One field that doesn't apply cleanly: `kind: "js-store"` — implies Vuex. For MX60 IIFE stores, the schema `kind` must be extended (MINOR bump → v1.1) or handled via the `frontend_iife` extension block with `module_type: "iife-store"`.

---

## 5. Delta-vs-reflow approach

### Columns

| Column | Source |
|--------|--------|
| Reflow component | reflow index.json `id` field |
| MX60 component | MX60 mapping `id` field |
| Status | HEREDADO \| REESCRITO \| FALTA \| NUEVO \| ANÁLOGO |
| Evidence | cite file:line for classification |
| Bloque #68 ref | which §68.x decision this validates |

### Classification heuristics

- **HEREDADO**: `rg "Ported|ported verbatim|Port of Snls"` in MX60 → hit + LOC delta ≤15%
- **REESCRITO**: name/function match + LOC delta >30% OR API signature changed
- **FALTA**: reflow has it, `rg "<className>"` in MX60 src → 0 hits
- **NUEVO**: MX60 has it, `rg "<className>"` in reflow index.json → 0 hits
- **ANÁLOGO**: functionally equivalent, different name/tech stack

### Bloque #68 validation targets (5 components)

1. **§68.1 HistoryData split** → `ChiHistoryHelper.java` is HEREDADO from SanLuis (confirmed: file header says "Ported from SnlsHistoryHelper"). Validates that Engine+Serializer split is viable.
2. **§68.2 BChiServlet dispatch** → `ChiServletDispatch.java` is NUEVO (no equivalent in reflow's BaseServlet hierarchy). Validates the pure-Java routing layer pattern.
3. **§68.3 IIFE store → Pinia** → `ModoOverrideStore.js`, `OutputOverrideStore.js`, `UpThresholdStore.js` are all NUEVO. Validates that there are 5 store migration targets absent from Vuex.
4. **§68.4 ack flow** → `AlarmModalActions.js` + `ChiAlarmHelper.java` — ANÁLOGO to reflow's ack path but without the RequiredNoteModal. Validates asymmetric ack (inline vs modal).
5. **§68.5 SubscriptionPool → useSubscriber** → `SubscriptionPool.js` is ANÁLOGO to reflow's `subscriberMixin` (cited in header: "Ported from SanLuis SubscriptionPool.js"). Validates the subscription lifecycle transplant.

---

## 6. Shard plan

Total estimated mapping entries: **~100-130** (comfortably below 75 per shard with natural splits).

| Shard | Domain(s) | Est. entries | Notes |
|-------|-----------|--------------|-------|
| S1: backend-rt | service-container, equipment-backend | ~10 | 2+6 java-class |
| S2: backend-ux | http-rest, equipment-reader, alarms-backend, history-backend, schedules-backend, util-backend | ~9 | 9 java-class |
| S3: frontend-core | app-shell, baja-integration, ui-lib | ~15 | IIFE app/lib modules |
| S4: frontend-equipment | equipment-frontend, equipment-detail-up, equipment-detail-misc, threshold-stores | ~16 | incl. UpDetail.js monolith |
| S5: frontend-alarms-schedules | alarms-frontend, schedules-frontend, history-frontend | ~13 | alarm lifecycle + schedule + history |
| S6: resources-config | module-descriptor, build-config, static-resources | ~30 | descriptors + images + ext/ |
| S7: xref | cross-cutting | ~80-100 xref entries | MX60 namespace graph (MX60.X → MX60.Y + java FQN) |

All shards fit within the 75-entry hard cap. S6 is highest at ~30 entries. No splitting needed beyond the 6+1 plan.

---

## 7. Risks and unknowns

**R1 — CRITICAL: `kind` enum gap for IIFE modules**
Reflow schema `kind` has `vue-component`, `vue-view`, `js-store` (Vuex) — none apply to MX60. The design phase must decide: (a) add new `kind` values (MINOR bump → v1.1), or (b) use existing `js-lib` / `js-util` as closest analogs and encode IIFE type in the `frontend_iife` extension block. Option (b) is cleaner for schema stability.

**R2 — MEDIUM: UpDetail.js is a 2400-line monolith**
Maps to a single index.json entry. `purpose` field (≤150 chars) must be brutally compressed. The xref `used_at` edges from this file will be the highest-cardinality in the xref layer.

**R3 — MEDIUM: runtime behavior boundary (#1238)**
All BajaScript subscription lifecycle claims (SubscriptionPool), controlTick timing, and WritePoint dual-path fallback are flagged as **inferred from mapping, not verified empirically** — pending station audit if needed for transplant.

**R4 — LOW: test files excluded**
16 test Java files present in srcTest/ (HANDOFF.md confirms tests don't run due to Niagara N4.14 plugin 7.6.17 discovery bug). Document in excluded.md; don't map.

**R5 — LOW: ext/ bundled JS**
5 vendor bundles in rc/ext/ (Chart.js, three.js). Map with `kind: compiled-bundle`, `status: resource`, `loc: 0`. Not excluded — useful for transplant blueprint (tells the team which charting library is in use).

---

## 8. Recommendation

**Proceed to sdd-propose**: YES

**xref in scope from start**: YES. The IIFE global namespace makes xref MORE critical than in reflow. With no static imports, the only way to trace dependencies is via the `MX60.X` namespace usage graph. Xref answers "who reads `MX60.EquipmentData`?" — essential for the transplant blueprint.

**Estimated total**: 100-130 index.json entries + 80-100 xref.json entries. Smaller than reflow-clean-177 (547+615). Wall-clock: 3-4 SDD sessions.

**Key design decisions for propose phase**:
1. Confirm `kind` enum extension strategy (new values vs extension block encoding)
2. Confirm `frontend_iife` extension block shape
3. Confirm delta-vs-reflow.md format and column set
4. Decide whether UpDetail.js gets split into logical sections in the mapping or stays as one entry

---

## skill_resolution

- injected (compact rules from #1231 multi-shard sub-agent, #309 skill-registry, #1238 clean-room asymmetry received from orchestrator)
