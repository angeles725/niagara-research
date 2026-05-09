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

---

## Entry Points and Key Findings

[... archived for brevity — see Engram #1209 for full exploration ...]

---

**Archived to**: openspec/archive/2026-05-09-mapping-reflow-clean-177/
**Status**: Ready for Proposal
