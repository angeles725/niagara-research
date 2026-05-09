# Exploration: mapping-reflow-clean-177

**Phase**: sdd-explore
**Date**: 2026-05-09
**Source (READ-ONLY)**: `/home/cristian/modules/Prototipos/Reflow-Clean-177/`
**Engram topic**: `sdd/mapping-reflow-clean-177/explore` (id #1209)

---

## Scale

### Backend — nmodsreflow Java (nmodsreflow-rt + nmodsreflow-ux)

| Profile | Java files | Notes |
|---------|-----------|-------|
| nmodsreflow-rt | 74 | All application code; 1 profile only in source |
| nmodsreflow-ux | 3 | BReflow, BReflowConfig, BReflowRedirect (BajaScript UX widgets) |
| **Total Java** | **77** | Decompiled with CFR 0.152; some dependency stubs missing |

**nmodsreflow-rt breakdown by package:**

| Package | Files | Contents |
|---------|-------|----------|
| root | 2 | BReflowService, BReflowScheme |
| alarms | 5 | ReflowAlarmSource, AlarmSourceCollection, AlarmData, QueryFilter, AlarmUuidArgs |
| backups | 1 | BackupManager |
| commands | 7 | BReflowAlarmCommands, BReflowHistoryCommands, BReflowNavCommands, BReflowBQLCommands, BReflowCSVCommands, BReflowFileCommands, BReflowUserCommands + implied BReflowLicenseCommands |
| history | 7 | HistoryData, HistoryList, HistoryGroups, HistoryGhostSubscriber, HistoryIO + 2 JSON serializers |
| history/json | 5 | HistoryObjectMapper, HistoryRecordSerializer, HistoryDeviceSerializer, HistoryFolderSerializer, HistoryRecordOptions, IHistorySerializer |
| http | 1 | BaseServlet |
| http/responses | 18 | 18 response handler classes |
| http/sockets | 5 | SocketServlet, BReflowWebSocketAcceptor, BReflowChannelService, IReflowCommand, AsyncReflowCommand, ReflowWsHttpSessionListener |
| http/util | 3 | Query, JsonBodies, CsrfGuard |
| sync | 4 | BReflowSyncService, ConfigIO, ReflowSyncResponse, ReflowSyncResponseSerializer + ReflowOrdTreeFavoritesRead/Write |
| util | 7 | RangeCalculator, CompareRangeCalculator, Json, NavNodeSerializer, PointHelper, StringUtils, CommandHelpers, BDateRangeEnum |

**nmodsreflow-ux breakdown:**

| File | Role |
|------|------|
| BReflow.java | BajaScript widget agent on ReflowService; implements BIJavaScript |
| BReflowConfig.java | Config-mode widget variant |
| BReflowRedirect.java | Force-redirects browser users to /nmodsreflow |
| niagara/reflow.js | JS loader for main SPA within Workbench iframe |
| niagara/reflow_config.js | JS loader for config mode |
| niagara/reflow_redirect.js | JS redirect logic |
| niagara/lib/widget.{hbs,css} | Handlebars template + CSS for UX widget |
| niagara/lib/{loader,resolver,hyperlink}.js | BajaScript helpers |

### Frontend — reflow-frontend (Vue 2.7 SPA)

| Category | Count |
|----------|-------|
| .vue files total | 378 |
| .js files (src/) | 81 |
| Views | ~13 |
| Store modules | 29 (14 persistent + 15 transient) |
| Router routes | 37 (20 top-level + 16 building-scoped + 1 catch-all) |
| API files | 5 (rest.js, websocket.js, bajascript.js, box.js, external.js, index.js) |
| Mixins | 17 |
| Plugins | 13 |
| Lib files | 8 (csrf, bajaHeartbeat, alarmCache, configMigration, configSerializer, deepMerge, eventBus, ord, utils, uuid) |
| CSS files in src/rc/ (compiled bundle) | 3 (app-readable, chunk-vendors, app) |

**Vue component breakdown by subdirectory:**

| Directory | Files |
|-----------|-------|
| components/alarms | ~22 |
| components/buildings | ~27 |
| components/browser | 1 |
| components/cards | ~18 |
| components/charts | ~11 |
| components/common | ~23 |
| components/config | ~22 |
| components/dashboard | ~12 |
| components/equipment | ~41 |
| components/floorplans | ~47 |
| components/histories | ~22 |
| components/layout | 5 |
| components/map | 1 |
| components/maps | 7 |
| components/navigation | ~17 |
| components/pages | ~11 |
| components/points | ~11 |
| components/profiles | ~11 |
| components/schedules | 4 |
| components/settings | ~13 |
| components/weather | 6 |
| components/websocket | 4 |
| components/wizard | 8 |
| views | ~13 |
| App.vue | 1 |

**LOC estimate:**
- Backend Java: ~8,000–12,000 LOC (74 classes; key classes: BReflowService 468L, HistoryData 580L, AlarmData 500L, BaseServlet ~300L, ConfigIO 249L, BackupManager 240L)
- Frontend source: ~40,000–60,000 LOC (378 .vue + 81 .js; original bundle was 123,237 lines minified)
- Original webpack bundle: 123,237 lines (5.8 MB readable JS at `nmodsreflow-rt/src/rc/js/app-readable.js`)
- Total source estimate: ~50,000–70,000 LOC

### Other artifacts

| Artifact | Location | Count |
|----------|----------|-------|
| openspec changes (source repo) | `/openspec/changes/` | 15+ archived + 2 active |
| openspec specs (source repo) | `/openspec/specs/` | 8 spec documents |
| Existing docs | `reflow-frontend/docs/` | 3 (REFLOW-ARCHITECTURE-ANALYSIS.md, GAP-ANALYSIS.md, NIAGARA-INTEGRATION.md) |
| Image library | `nmodsreflow-rt/src/image-library/` | 22 JPGs (AHUs, Boilers, Chillers, Cooling-Towers, FCUs, Misc, RTUs, VAVs) |
| Icons | `nmodsreflow-rt/src/icons/` | 6 PNGs |
| Build artifacts (ux profile) | `nmodsreflow-ux/build/` | .jar + .class + bajadoc files |

---

## Domains

### Backend Domains

| Domain | Key Classes | Role |
|--------|-------------|------|
| **Service Container** | BReflowService | Root BComponent; 26 slots; implements BIService + BIRestrictedComponent; lifecycle start/stop |
| **ORD Scheme** | BReflowScheme | Custom BOrdScheme for `reflow:` URLs |
| **HTTP REST** | BaseServlet + 18 Response handlers | 24 REST endpoints: config CRUD, history, alarms, schedules, backups, files, weather |
| **WebSocket** | SocketServlet, BReflowWebSocketAcceptor, BReflowChannelService, IReflowCommand | Pub/sub broadcast; commands: join/leave/who/broadcast/route; config-control locking |
| **BajaScript BOX** | 7 BReflowXxxCommands | RPC bridge: ~25 methods for alarms, histories, nav, BQL, CSV, files, users |
| **History** | HistoryData, HistoryList, HistoryGroups, HistoryIO | Read/write/cache station histories; JSON serialization with Jackson |
| **Alarms** | ReflowAlarmSource, AlarmData, AlarmSourceCollection, QueryFilter | BQL-based alarm querying; CSV export; UUID lookup; IAlarmSource |
| **Sync/Config** | BReflowSyncService, ConfigIO | Read/write `^/reflow/config.json`; multi-user locking; delta sync |
| **Backups** | BackupManager | Station backup CRUD |
| **Util** | RangeCalculator, CompareRangeCalculator, Json, NavNodeSerializer, PointHelper, StringUtils | Date ranges, JSON utils, ORD navigation |
| **UX Widgets** | BReflow, BReflowConfig, BReflowRedirect | Workbench BajaScript widgets; BIJavaScript; @AgentOn(types={"nmodsreflow:ReflowService"}) |

### Frontend Domains

| Domain | Components/Files | Role |
|--------|-----------------|------|
| **App Shell** | main.js, App.vue, router/index.js | Bootstrap; 13 plugins; 37 routes (hash mode) |
| **State** | store/index.js + 29 modules | Vuex: 14 persistent + 15 transient modules; LOAD_STATE/STATE_DELTA/REPLACE_STATE |
| **Dashboard/Home** | dashboard/ (12), views/Home.vue | Landing page; hero; card lists; buildings/equipment/histories summary |
| **Buildings** | buildings/ (27) | Multi-building hierarchy; map integration; RBAC-gated nav |
| **Equipment** | equipment/ (41) | Device cards, lists, types, CSV wizard, editor, point bindings |
| **Alarms** | alarms/ (22) | Console, table, priorities, sounds, ack, sources |
| **Histories** | histories/ (22) | Builder, chart (D3), sparklines, groups, picker |
| **Floorplans** | floorplans/ (47) | SVG canvas editor; element types: image, icon, label, button, polygon, arrow; props pane |
| **Config** | config/ (22) | ConfigView, ConfigMenu, ConfigCell + typed cell variants |
| **Navigation** | navigation/ (17) | Sidebar, mobile nav, dropdowns, subnav, styles |
| **Cards** | cards/ (18) | BaseCard, CircleCard, TableCard, FeaturedCard, Gauge, HX, etc. |
| **Charts** | charts/ (11) | D3chart (global), ChartToolBar, TimeRangePicker, Sparkline |
| **Points** | points/ (11) | PointList, NiagaraPoint (BajaScript subscriber), PointMap, PointCell |
| **Profiles/RBAC** | profiles/ (11), store/profiles.js | User roles; authorizeLink; isPathAvailable; page restrictions |
| **Schedules** | schedules/ (4), views/SchedulesHome | BQL-based schedule list/groups |
| **Pages** | pages/ (11), views/PageView | Custom page embedding (Niagara ORD, web URL) |
| **Weather** | weather/ (6) | Aeris weather API; map overlay; config |
| **Maps** | maps/ (7), map/ (1) | Mapbox GL via v-mapbox (stubs — needs npm install) |
| **Settings** | settings/ (13) | Global settings, backups, logo, background, color picker |
| **Websocket** | websocket/ (4), api/websocket.js | Multi-user config control; SocketAuth/Connect/Request modals |
| **API Layer** | api/{rest,websocket,bajascript,box,external,index}.js | REST (axios), WS (socket.io stub), BOX (BajaScript), external |
| **Mixins** | mixins/ (17) | subscriberMixin, equipmentMixin, historyListMixin, dynamicColorMixin, canvasDragResizeMixin, elementMixin, etc. |
| **Plugins** | plugins/ (13) | $baja, $niagara, $http, $time, $ord, $reflowLink, $gbo, $workbench, $cookies, labelForItem, configMode, colorUtils, utils |
| **Lib** | lib/ (10) | csrf, eventBus, deepMerge, configSerializer, configMigration, alarmCache, bajaHeartbeat, ord, utils, uuid |

---

## Entry Points

### Backend

| File | Role |
|------|------|
| `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowService.java` | Root service; 26 BComponent slots; lifecycle; spawns BReflowSyncService + BReflowWebSocketAcceptor + BReflowChannelService |
| `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/BaseServlet.java` | HTTP front controller; 24 endpoints mapped by URL path |
| `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/sockets/SocketServlet.java` | WebSocket upgrade endpoint at `/nmodsreflow/ws` |
| `nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowScheme.java` | Custom ORD scheme `reflow:` |
| `nmodsreflow-ux/src/com/niagaramods/nmodsreflow/ux/BReflow.java` | Workbench UX widget; @AgentOn ReflowService; BIJavaScript |
| `nmodsreflow-rt/module.palette` | Declares ReflowService for Niagara Workbench palette |
| `nmodsreflow/niagara-module.xml` | Module descriptor: `nmodsreflow`, profiles `rt,ux` |

### Frontend

| File | Role |
|------|------|
| `reflow-frontend/src/main.js` | App bootstrap: Vue 2.7, ViewUI, 13 plugins, D3chart global, CSRF bootstrap |
| `reflow-frontend/src/App.vue` | Root component; mounts layout |
| `reflow-frontend/src/router/index.js` | 37 routes (hash mode); 20 top-level + 16 building-scoped + catch-all |
| `reflow-frontend/src/store/index.js` | Vuex root: 29 modules; LOAD_STATE/STATE_DELTA/REPLACE_STATE; migrateState action |
| `reflow-frontend/src/plugins/baja.js` | $baja — BajaScript integration bridge |
| `reflow-frontend/src/plugins/niagara.js` | $niagara — alarm/subscriber/BQL/history RPC |
| `reflow-frontend/src/api/websocket.js` | WebSocket canal (stub; real implementation Phase 5+) |
| `reflow-frontend/src/api/rest.js` | REST axios wrapper |
| `reflow-frontend/src/lib/csrf.js` | CSRF token bootstrap; 403→refresh→retry |

---

## Mapping Dimensions

### Core Fields (universal — reusable for Analytics/MX60 and any other module)

| Field | Type | Description |
|-------|------|-------------|
| `path` | string | Absolute path within the repo (relative to repo root) |
| `kind` | enum | `java-class`, `vue-component`, `js-module`, `js-plugin`, `js-mixin`, `js-store`, `js-api`, `js-lib`, `js-router`, `config`, `resource` |
| `domain` | string | Logical domain (e.g., `history`, `alarm`, `equipment`, `navigation`, `store`, `util`) |
| `purpose` | string | One-sentence description of what this file does |
| `dependencies` | string[] | Key imports or usages (class names, module paths) |
| `loc` | integer | Lines of code (approximate) |
| `status` | enum | `source` (editable Java/Vue/JS), `compiled` (.class, .jar), `bundle` (minified dist) |

### Backend Extensions (nmodsreflow-specific)

| Field | Type | Description |
|-------|------|-------------|
| `profile` | enum | `rt`, `ux` |
| `package` | string | Java package (e.g., `com.niagaramods.nmodsreflow.history`) |
| `bcomponent_type` | string | `BComponent`, `BIService`, `BOrdScheme`, `BIJavaScript`, etc. (if applicable) |
| `slots` | integer | Number of BComponent Property/Action slots (if BComponent) |
| `actions` | string[] | Declared @NiagaraAction names |
| `rest_endpoints` | string[] | HTTP endpoints handled (for response handlers) |
| `box_methods` | string[] | BajaScript RPC method names (for command classes) |
| `decompiled` | boolean | True if source is CFR-decompiled output |

### Frontend Extensions (Vue SPA-specific)

| Field | Type | Description |
|-------|------|-------------|
| `component_dir` | string | Subdirectory within `components/` (e.g., `equipment`, `floorplans`) |
| `store_modules` | string[] | Vuex store modules this component accesses |
| `emits` | string[] | `$emit` events |
| `props` | string[] | Vue props (key ones) |
| `mixins` | string[] | Mixins applied |
| `plugins_used` | string[] | Vue.prototype plugins used ($baja, $niagara, etc.) |
| `persistent` | boolean | For store modules: whether included in config.json serialization |
| `route_name` | string | For views: router route name |
| `fidelity` | enum | `EXCELLENT`, `GOOD`, `FAIR`, `POOR` — from GAP-ANALYSIS.md (where known) |

### Schema Strategy

The mapping schema uses a **core + extension** model:
- `core` fields apply to every file in every module
- `backend` extension applies to Java files in Niagara modules
- `frontend_vue` extension applies to Vue 2.x SPA files
- This makes the schema reusable for Analytics/MX60 with different extension blocks

---

## Existing Artifacts That Can Be Leveraged

| Artifact | Path | Value |
|----------|------|-------|
| REFLOW-ARCHITECTURE-ANALYSIS.md | `reflow-frontend/docs/REFLOW-ARCHITECTURE-ANALYSIS.md` | Complete backend architecture (backend class map, slot table, REST endpoint list, BOX method list, LOC estimates) |
| GAP-ANALYSIS.md | `reflow-frontend/docs/GAP-ANALYSIS.md` | Per-domain fidelity ratings for all 378 .vue files and 29 store modules; covers Sessions 47-57 |
| NIAGARA-INTEGRATION.md | `reflow-frontend/docs/NIAGARA-INTEGRATION.md` | Build/deploy pipeline; servlet chain; entry point flow |
| openspec/changes/ (source repo) | `/openspec/changes/` | 15+ archived change specs with detailed design decisions |
| reflow-replica-baseline/exploration.md | `/openspec/changes/reflow-replica-baseline/exploration.md` | Original SPA architecture analysis (servlet chain, `injectBaja` flow, WebSocket protocol) |
| phase-b-survey-2026-04-15.md | `/openspec/phase-b-survey-2026-04-15.md` | Phase B survey |

**Key implication**: The mapping does NOT need to build file-purpose data from scratch. `REFLOW-ARCHITECTURE-ANALYSIS.md` already contains backend class purposes, LOC estimates, and slot counts. `GAP-ANALYSIS.md` already contains per-domain fidelity and known issues for all frontend files. The mapping artifact can import/synthesize these directly.

---

## Risks and Unknowns

| Risk | Severity | Notes |
|------|----------|-------|
| **Decompiled Java** | Medium | `BReflowScheme.java` and `nmodsreflow-ux` files have CFR decompile headers; missing dependency stubs (javax.baja.*) mean some class hierarchies are inferred, not verified |
| **nmodsreflow-ux build artifacts present** | Low-Medium | `nmodsreflow-ux/build/libs/nmodsreflow-ux.jar` exists as a binary — should be excluded from file mapping (mark as `status: compiled`) |
| **node_modules present** | Low | `reflow-frontend/node_modules/` is installed; must be excluded from the mapping (no source files, only dependencies) |
| **src/rc/ is compiled bundle** | Medium | `nmodsreflow-rt/src/rc/` contains the production webpack build (app.js, CSS, fonts, images), NOT source. Only `src/rc/js/app-readable.js` is useful for comparison. Map these as `status: bundle` |
| **image-library (binary assets)** | Low | 22 JPG files in `src/image-library/` should be catalogued but not analyzed for code |
| **Missing Gradle source** | Low | `gradle.properties` references a Windows path (`C:\Honeywell\`); build environment is assumed separate |
| **78% fidelity in floorplans** | Informational | Per GAP-ANALYSIS: floorplans domain has 47 components rated GOOD (78%) — some edge cases may not be source-of-truth |
| **FAIR/POOR stores** | Informational | GAP-ANALYSIS marks `buildings` store FAIR and `equipment` store POOR — mapping should note these with fidelity field |
| **WebSocket stubs** | Informational | api/websocket.js is Phase 5+ stub — all 11 exported functions are console.log only |

---

## Recommendation

**Build the mapping as a two-tier synthesized document:**

1. **Tier 1 — Directory Index** (`index.md` / `index.json`): One entry per file across the full codebase. Fields: path, kind, domain, purpose (one line), LOC, status. This is the foundation for any `jq` query.

2. **Tier 2 — Domain Sections** (separate markdown per domain): Detailed breakdown per domain with full extension fields (BComponent slots, REST endpoints, BOX methods for backend; props/emits/store usage for frontend). Pull directly from the three existing docs in `reflow-frontend/docs/`.

**Schema design**: Use the core + extension model described above. Do NOT try to capture every Vue prop in the index — reserve detailed props/emits for domain sections only.

**Leverage existing work aggressively**: `REFLOW-ARCHITECTURE-ANALYSIS.md` (2026-04-05) and `GAP-ANALYSIS.md` (2026-04-06) are high-quality, recent, and cover 90%+ of what the mapping needs. The propose phase should plan to ingest these docs and synthesize rather than re-read every file.

**Phasing**: Given the volume (377 .vue + 77 Java + 81 .js = 535 source files), the mapping artifact should be generated in domain batches: backend first (74 Java → easy index), then frontend by subdirectory group.

---

## Approaches for the Mapping Artifact

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **A. Full auto-scan per file** | Complete, authoritative | 535 files to read; context blowout risk | High |
| **B. Synthesize from existing docs + targeted sampling** | Leverages 3 existing analysis docs; fast; accurate for architecture | Some fine-grained per-file fields require inference | Low-Medium |
| **C. Domain-by-domain delegation** | Manageable chunks; can parallelize | Requires orchestration | Medium |

**Recommended**: Option B for the index.md/index.json skeleton, then Option C for domain section details. The three existing docs already cover backend class purposes and LOC, and the GAP-ANALYSIS covers frontend fidelity per area. A mapping team can produce a high-quality first version in 2-3 sessions.

---

## Ready for Proposal

Yes. The codebase is fully understood. A proposal can define the exact schema, file structure, and batching strategy for the mapping artifact.
