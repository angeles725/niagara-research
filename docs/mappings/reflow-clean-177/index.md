# Reflow-Clean-177 — Master Index

**Schema version**: 1.0  
**Source repo**: `/home/cristian/modules/Prototipos/Reflow-Clean-177`  
**Total entries**: 547  
**Generated**: 2026-05-09T12:46:39Z  
**Spot-checked entries**: 547 (verified_at populated)

**Companions**: `index.json` (canonical), `schema.md`, `README.md`, `excluded.md`, `domains/`

## Summary by Kind

| Kind | Count |
|------|------:|
| config | 11 |
| java-class | 77 |
| js-api | 6 |
| js-lib | 10 |
| js-mixin | 18 |
| js-module | 3 |
| js-plugin | 13 |
| js-router | 1 |
| js-store | 30 |
| vue-component | 378 |

## Summary by Domain

| Domain | Count |
|--------|------:|
| alarms | 33 |
| api | 6 |
| app-shell | 4 |
| backups | 1 |
| bajascript-box | 5 |
| browser | 1 |
| buildings | 27 |
| cards | 18 |
| charts | 11 |
| common | 26 |
| config | 24 |
| dashboard | 14 |
| equipment | 44 |
| floorplans | 52 |
| history | 34 |
| http-rest | 28 |
| layout | 5 |
| lib | 10 |
| map | 1 |
| maps | 7 |
| mixins | 18 |
| module-config | 11 |
| navigation | 16 |
| ord-scheme | 1 |
| pages | 11 |
| plugins | 13 |
| points | 12 |
| profiles | 12 |
| router | 1 |
| schedules | 4 |
| service-container | 1 |
| settings | 13 |
| store | 30 |
| sync-config | 6 |
| util | 8 |
| ux-widgets | 3 |
| views | 12 |
| weather | 6 |
| websocket | 10 |
| wizard | 8 |

## Master Index

Sorted by path. Entries with `verified_at` populated have been spot-checked against source. For per-file details see `domains/<name>.md` or `jq` queries against `index.json`.

| Path | Kind | Domain | Purpose | LOC | Status | Verified |
|------|------|--------|---------|----:|--------|:--------:|
| `build.gradle.kts` | config | module-config | Root Gradle Kotlin build script declaring multi-module project structure and shared dependency versions. | 45 | source | Y |
| `nmodsreflow/niagara-module.xml` | config | module-config | Top-level Niagara module descriptor declaring module name, vendor, version, and submodule list. | 2 | source | Y |
| `nmodsreflow/nmodsreflow-rt/module-include.xml` | config | module-config | Niagara module-include descriptor for -rt submodule listing Java class registrations and service contributions. | 47 | source | Y |
| `nmodsreflow/nmodsreflow-rt/module-permissions.xml` | config | module-config | Niagara permission descriptor granting -rt submodule access to station services and security categories. | 10 | source | Y |
| `nmodsreflow/nmodsreflow-rt/module.palette` | config | module-config | Niagara palette descriptor for the -rt (runtime) submodule, declaring component types available in Workbench. | 6 | source | Y |
| `nmodsreflow/nmodsreflow-rt/nmodsreflow-rt.gradle.kts` | config | module-config | Gradle Kotlin build script for the -rt submodule declaring Java dependencies and Niagara SDK compilation settings. | 97 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowScheme.java` | java-class | ord-scheme | BOrdScheme registration for the nmodsreflow module — maps reflow: ORD scheme to servlet handlers. | 78 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowService.java` | java-class | service-container | Main BComponent service container (26 slots) — orchestrates license, HTTP, WebSocket, sync sub-services and BOX dispatch. | 468 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/alarms/AlarmData.java` | java-class | alarms | Alarm querying/filtering via BQL, CSV export and UUID lookup for the alarm REST/BOX layer. | 439 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/alarms/AlarmSourceCollection.java` | java-class | alarms | Container for multiple alarm source references used during alarm query aggregation. | 83 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/alarms/AlarmUuidArgs.java` | java-class | alarms | Value object carrying alarm UUID arguments for acknowledge and query BOX command calls. | 74 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/alarms/QueryFilter.java` | java-class | alarms | Encapsulates alarm query filter parameters (time range, severity, source) for BQL alarm queries. | 158 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/alarms/ReflowAlarmSource.java` | java-class | alarms | Wrapper for a Niagara alarm source ORD reference used when building multi-source alarm queries. | 25 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/backups/BackupManager.java` | java-class | backups | Station backup CRUD operations — create, list, apply, rename, destroy and reset station backups. | 248 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowAlarmCommands.java` | java-class | alarms | BajaScript BOX command class exposing alarm RPC methods: query, getClasses, getActiveAlarmCounts and acknowledge helpers. | 113 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowBQLCommands.java` | java-class | bajascript-box | BajaScript BOX command class providing a generic BQL query method callable from the browser. | 120 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowCSVCommands.java` | java-class | bajascript-box | BajaScript BOX command class for loading point map CSV files from the station file system. | 121 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowFileCommands.java` | java-class | bajascript-box | BajaScript BOX command class for listing station files via BOX RPC from the browser. | 149 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowHistoryCommands.java` | java-class | history | BajaScript BOX command class exposing history RPC: getList, getData, getGroupNames, getGroupTree, getDeviceTree. | 112 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowNavCommands.java` | java-class | bajascript-box | BajaScript BOX command class for navigation tree RPC: bformat and getNavChildren for the browser nav panel. | 127 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowUserCommands.java` | java-class | bajascript-box | BajaScript BOX command class for user/role queries: getRoles and getAllRoles for RBAC in the browser. | 62 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/HistoryData.java` | java-class | history | History record retrieval with Builder pattern and Jackson JSON serialization for chart and table data. | 663 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/HistoryGhostSubscriber.java` | java-class | history | Ghost subscriber that keeps a history session alive on the station to avoid timeout during long queries. | 26 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/HistoryGroups.java` | java-class | history | History grouping and categorization — organizes history records by folder/device hierarchy for UI display. | 112 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/HistoryIO.java` | java-class | history | I/O utilities for history data serialization — reads/writes history records to/from JSON streams. | 103 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/HistoryList.java` | java-class | history | History enumeration and caching — lists available histories in the station with optional cache for performance. | 355 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/json/HistoryDeviceSerializer.java` | java-class | history | Jackson serializer for device-level history nodes, producing device JSON objects for the history tree response. | 65 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/json/HistoryFolderSerializer.java` | java-class | history | Jackson serializer for folder-level history nodes, producing folder JSON objects for the history tree response. | 55 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/json/HistoryObjectMapper.java` | java-class | history | Configures a Jackson ObjectMapper with all history serializers registered for consistent history JSON output. | 20 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/json/HistoryRecordOptions.java` | java-class | history | Value object specifying serialization options (format, timezone, precision) for history record JSON output. | 29 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/json/HistoryRecordSerializer.java` | java-class | history | Jackson serializer for individual history records, converting BHistoryRecord to timestamped value JSON. | 122 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/json/IHistorySeralizer.java` | java-class | history | Interface contract for history serializers — defines serialize() method implemented by all history JSON serializers. | 80 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/BaseServlet.java` | java-class | http-rest | HTTP front controller (300 lines) — routes all REST requests to 24 response handler classes by path prefix. | 367 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/AlarmCSVResponse.java` | java-class | http-rest | REST response handler for alarm CSV export — queries AlarmData and writes CSV to HTTP response stream. | 38 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/AlarmQueryResponse.java` | java-class | http-rest | REST response handler for alarm JSON queries — delegates to AlarmData.query() and writes JSON response. | 44 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/BackupApplyResponse.java` | java-class | http-rest | REST response handler for applying (restoring) a named station backup via POST request. | 57 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/BackupCreateResponse.java` | java-class | http-rest | REST response handler for creating a new station backup with optional name parameter. | 39 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/BackupDestroyResponse.java` | java-class | http-rest | REST response handler for destroying (deleting) a named station backup via POST. | 38 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/BackupListResponse.java` | java-class | http-rest | REST response handler for listing available station backups with metadata as JSON array. | 43 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/BackupRenameResponse.java` | java-class | http-rest | REST response handler for renaming an existing station backup to a new name via POST. | 47 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/BackupResetResponse.java` | java-class | http-rest | REST response handler for resetting backup settings/state to defaults via POST. | 58 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/ConfigDeltaResponse.java` | java-class | http-rest | REST response handler for config delta — returns only changed config sections since a given version token. | 55 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/ConfigResponse.java` | java-class | http-rest | REST response handler for full config GET — reads and returns the 66KB config JSON to the browser. | 118 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/ConfigUpdateResponse.java` | java-class | http-rest | REST response handler for config POST updates — validates and persists modified config sections from browser. | 128 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/DemoResponse.java` | java-class | http-rest | REST response handler for demo mode — serves sample data payloads when operating without a live station. | 48 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/EquipmentNoteResponse.java` | java-class | http-rest | REST response handler for reading equipment notes — GET returns note text for a specific equipment point ORD. | 58 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/EquipmentNoteUpdateResponse.java` | java-class | http-rest | REST response handler for writing/updating equipment notes via POST with note text payload. | 87 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/FileResponse.java` | java-class | http-rest | REST response handler for serving a single station file's content by path parameter. | 84 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/FileTreeResponse.java` | java-class | http-rest | REST response handler for station file tree — returns recursive directory listing as JSON tree structure. | 66 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/HistoryChartDataResponse.java` | java-class | http-rest | REST response handler for chart-optimized history data — returns time-series arrays for frontend chart rendering. | 74 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/HistoryDataResponse.java` | java-class | http-rest | REST response handler for history data retrieval — returns paginated history records for table display. | 265 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/HistoryGroupsResponse.java` | java-class | http-rest | REST response handler for history group tree — returns hierarchical history folder/device structure as JSON. | 83 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/HistoryListResponse.java` | java-class | http-rest | REST response handler for history list — returns flat list of all available history records with metadata. | 84 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/ImageLibraryResponse.java` | java-class | http-rest | REST response handler for image library — returns metadata for all images in the station image library. | 68 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/ImageListResponse.java` | java-class | http-rest | REST response handler for listing available image assets in the station file system as JSON array. | 64 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/SchedulesDataResponse.java` | java-class | http-rest | REST response handler for schedule data — queries Niagara schedule objects and returns JSON schedule info. | 33 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/responses/WeatherMapResponse.java` | java-class | http-rest | REST response handler for weather map data — proxies external weather API and returns cached weather JSON. | 125 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/sockets/AsyncReflowCommand.java` | java-class | websocket | Asynchronous WebSocket command wrapper — defers IReflowCommand execution off the WebSocket thread. | 34 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/sockets/BReflowChannelService.java` | java-class | websocket | BComponent pub/sub channel service — manages named channel join/leave/broadcast for real-time browser updates. | 281 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/sockets/BReflowWebSocketAcceptor.java` | java-class | websocket | BComponent WebSocket lifecycle manager — accepts connections, dispatches IReflowCommand, handles config sync. | 505 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/sockets/IReflowCommand.java` | java-class | websocket | Interface for WebSocket command handlers — defines execute() contract for all WebSocket message processors. | 12 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/sockets/ReflowWsHttpSessionListener.java` | java-class | websocket | HTTP session listener for WebSocket connections — cleans up per-session state on Niagara session expiry. | 40 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/sockets/SocketServlet.java` | java-class | websocket | Servlet entry point for WebSocket upgrade handshake — bridges Niagara HTTP servlet with WebSocket acceptor. | 54 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/util/CsrfGuard.java` | java-class | http-rest | CSRF protection utility — validates double-submit tokens on POST requests to guard against cross-site attacks. | 143 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/util/JsonBodies.java` | java-class | http-rest | HTTP utility for reading and writing JSON request/response bodies — wraps Jackson for servlet handler use. | 86 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/util/Query.java` | java-class | http-rest | HTTP query parameter parsing utility (hotspot: 10 calls) — extracts and type-converts URL query params for handlers. | 45 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/sync/BReflowSyncService.java` | java-class | sync-config | BComponent config sync service — manages multi-user config locking, grantConfigControl and WebSocket broadcast on save. | 599 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/sync/ConfigIO.java` | java-class | sync-config | JSON config file read/write with caching — persists the 66KB config JSON to station file system with cache layer. | 252 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/sync/ReflowSyncResponse.java` | java-class | sync-config | Value object representing a sync operation response payload — carries status, version token and changed sections. | 26 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/sync/ReflowSyncResponseSerializer.java` | java-class | sync-config | Jackson serializer for ReflowSyncResponse — converts sync response value object to JSON for WebSocket broadcast. | 42 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/sync/commands/ReflowOrdTreeFavoritesRead.java` | java-class | sync-config | Sync command for reading user ORD-tree favorites — reads persisted favorite ORDs from station config store. | 71 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/sync/commands/ReflowOrdTreeFavoritesWrite.java` | java-class | sync-config | Sync command for writing user ORD-tree favorites — persists updated favorite ORDs to station config store. | 46 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/util/BDateRangeEnum.java` | java-class | util | BComponent enum of named date range presets (Today, LastWeek, etc.) used by history and alarm queries. | 122 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/util/CommandHelpers.java` | java-class | util | Static helper methods shared across BOX command classes — argument parsing, auth checks and error formatting. | 23 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/util/CompareRangeCalculator.java` | java-class | util | Period-over-period comparison date range computation — derives prior period range from a current range for trend charts. | 316 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/util/Json.java` | java-class | util | JSON utility façade — wraps Jackson ObjectMapper with convenience methods for safe parse/stringify operations. | 97 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/util/NavNodeSerializer.java` | java-class | util | Jackson serializer for Niagara navigation tree nodes — converts BNavNode to JSON for BReflowNavCommands responses. | 58 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/util/PointHelper.java` | java-class | util | Utility for resolving Niagara point ORDs and reading current values for equipment and dashboard widgets. | 50 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/util/RangeCalculator.java` | java-class | util | Date range computation for history queries — translates named range presets into absolute AbsTime start/end pairs. | 300 | source | Y |
| `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/util/StringUtils.java` | java-class | util | String utility helpers — null-safe operations, trimming, and encoding utilities used across the module. | 15 | source | Y |
| `nmodsreflow/nmodsreflow-ux/module-include.xml` | config | module-config | Niagara module-include descriptor for -ux submodule listing BPX web servlet and module-web registrations. | 17 | source | Y |
| `nmodsreflow/nmodsreflow-ux/module-permissions.xml` | config | module-config | Niagara permission descriptor granting -ux submodule web-servlet and user-authentication access. | 5 | source | Y |
| `nmodsreflow/nmodsreflow-ux/module.palette` | config | module-config | Niagara palette descriptor for the -ux (user experience) submodule, declaring BPX web component types. | 5 | source | Y |
| `nmodsreflow/nmodsreflow-ux/nmodsreflow-ux.gradle.kts` | config | module-config | Gradle Kotlin build script for the -ux submodule configuring the frontend webpack build integration. | 44 | source | Y |
| `nmodsreflow/nmodsreflow-ux/src/com/niagaramods/nmodsreflow/ux/BReflow.java` | java-class | ux-widgets | UX BComponent entry point for the Reflow workbench plugin — registers the Reflow view in Niagara Workbench. | 59 | source | Y |
| `nmodsreflow/nmodsreflow-ux/src/com/niagaramods/nmodsreflow/ux/BReflowConfig.java` | java-class | ux-widgets | UX BComponent for Reflow Workbench configuration panel — exposes editable config properties to Workbench editors. | 59 | source | Y |
| `nmodsreflow/nmodsreflow-ux/src/com/niagaramods/nmodsreflow/ux/BReflowRedirect.java` | java-class | ux-widgets | UX BComponent that implements browser redirect from legacy PX views to the Reflow HTML5 URL. | 59 | source | Y |
| `reflow-frontend/src/App.vue` | vue-component | app-shell | Root Vue application component mounting the authenticated Layout shell and WebSocket providers | 118 | source | Y |
| `reflow-frontend/src/api/bajascript.js` | js-api | api | BajaScript API wrapper: subscribe/unsubscribe helpers for ORD point-value streams using the Niagara bajaux client. | 72 | source | Y |
| `reflow-frontend/src/api/box.js` | js-api | api | BajaScript Box API helpers for invoking Niagara actions and reading component property maps. | 331 | source | Y |
| `reflow-frontend/src/api/external.js` | js-api | api | External HTTP API wrapper for third-party integrations (weather, tiles) not routed through Niagara. | 63 | source | Y |
| `reflow-frontend/src/api/index.js` | js-api | api | API barrel export re-exporting all api/* modules for unified import. | 11 | source | Y |
| `reflow-frontend/src/api/rest.js` | js-api | api | Niagara REST API client: config CRUD (GET /nmodsreflow/config, POST /config_update, POST /config_delta) and auth endpoints. | 353 | source | Y |
| `reflow-frontend/src/api/websocket.js` | js-api | api | Niagara WebSocket client handling channel subscriptions for config-delta sync and multi-user control. | 155 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmAckConfirm.vue` | vue-component | alarms | Confirmation dialog for acknowledging one or more alarms with optional note validation. | 92 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmCards.vue` | vue-component | alarms | Card-based alarm record viewer with pagination, acknowledgement, and notes actions. | 371 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmClassList.vue` | vue-component | alarms | Displays and manages the list of alarm classes for console configuration. | 160 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmConsoleForm.vue` | vue-component | alarms | Form for creating or editing an alarm console configuration. | 287 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmConsoleList.vue` | vue-component | alarms | Draggable list of alarm console configurations; triggers dive navigation. | 93 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmDisplay.vue` | vue-component | alarms | Main alarm display view orchestrating table and card sub-views for a console. | 295 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmIconsForm.vue` | vue-component | alarms | Form for configuring per-priority icons used in alarm display. | 130 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmList.vue` | vue-component | alarms | Thin wrapper that renders a list of alarm records for a given console. | 16 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmNotes.vue` | vue-component | alarms | Inline notes editor for a single alarm record with save/error feedback. | 124 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmNotesModal.vue` | vue-component | alarms | Modal dialog wrapping the notes editor for detailed alarm annotation. | 64 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmPrioritiesForm.vue` | vue-component | alarms | Form for mapping alarm priorities to labels and display order in a console. | 81 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmPriorityPicker.vue` | vue-component | alarms | Dropdown picker for selecting an alarm priority level. | 160 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmPriorityType.vue` | vue-component | alarms | Visual type indicator badge for a given alarm priority (color + icon). | 279 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmRowStyleForm.vue` | vue-component | alarms | Form for configuring row colors and fonts for each alarm state in console display. | 333 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmSoundsForm.vue` | vue-component | alarms | Form for assigning audio alerts to alarm priority levels. | 252 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmSoundsPicker.vue` | vue-component | alarms | Picker for selecting a sound clip to associate with an alarm priority. | 88 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmStatusPicker.vue` | vue-component | alarms | Filter control for selecting acknowledged/unacknowledged alarm status in console view. | 158 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmSummaryForm.vue` | vue-component | alarms | Form for configuring the summary card display of alarms per building. | 90 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmsTable.vue` | vue-component | alarms | Paginated table view of alarm records with multi-select, ack, notes, and details actions. | 427 | source | Y |
| `reflow-frontend/src/components/alarms/AlarmsTableForm.vue` | vue-component | alarms | Form for customizing the column layout and display options of the alarms table. | 149 | source | Y |
| `reflow-frontend/src/components/alarms/BuildingAlarmSummary.vue` | vue-component | alarms | Per-building alarm count summary card with priority breakdown and console dive link. | 114 | source | Y |
| `reflow-frontend/src/components/alarms/ConsoleRefreshRateForm.vue` | vue-component | alarms | Form for setting the auto-refresh interval of an alarm console. | 47 | source | Y |
| `reflow-frontend/src/components/alarms/PriorityColorsForm.vue` | vue-component | alarms | Form for assigning custom colors to each alarm priority level. | 108 | source | Y |
| `reflow-frontend/src/components/alarms/RequiredNoteModal.vue` | vue-component | alarms | Modal requiring the user to enter a note before acknowledging an alarm. | 95 | source | Y |
| `reflow-frontend/src/components/alarms/SourceGroupsTable.vue` | vue-component | alarms | Paginated table of alarm source groups with multi-select ack and notes actions. | 656 | source | Y |
| `reflow-frontend/src/components/alarms/SourcesTableForm.vue` | vue-component | alarms | Form for configuring the source filter table used in an alarm console. | 150 | source | Y |
| `reflow-frontend/src/components/alarms/Total.vue` | vue-component | alarms | Badge component showing total active alarm count across all consoles. | 128 | source | Y |
| `reflow-frontend/src/components/browser/ImageBrowser.vue` | vue-component | browser | Full-screen image browser dialog for selecting background images from the Niagara image library | 343 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingAlarms.vue` | vue-component | buildings | Lists active alarms scoped to a specific building | 165 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingCard.vue` | vue-component | buildings | Card widget displaying building summary (name, status, metrics) | 384 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingDashboard.vue` | vue-component | buildings | Dashboard overview page for a single building with KPI widgets | 70 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingFloors.vue` | vue-component | buildings | Lists floors belonging to a building with navigation to each floor plan | 129 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingForm.vue` | vue-component | buildings | Create/edit form for building metadata (name, address, timezone) | 189 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingGrid.vue` | vue-component | buildings | Grid layout for displaying multiple building cards | 351 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingGroupForm.vue` | vue-component | buildings | Form for creating or editing a building group (portfolio) | 97 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingGroupMarker.vue` | vue-component | buildings | Map marker representing a building group cluster on a map view | 154 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingHeroContent.vue` | vue-component | buildings | Hero section content (headline metrics, image) for the building detail page | 97 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingHistories.vue` | vue-component | buildings | Historical data panel for building-level trend charts | 97 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingIndex.vue` | vue-component | buildings | Root index view for the buildings section combining list, map and counts | 116 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingIndexCounts.vue` | vue-component | buildings | Summary count badges (total buildings, active alarms) shown on buildings index | 117 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingIndexMarker.vue` | vue-component | buildings | Individual map marker for a building on the buildings index map | 160 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingIndexMarkerList.vue` | vue-component | buildings | Scrollable list of map markers rendered inside a popover on the buildings index map | 106 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingIndexPopovers.vue` | vue-component | buildings | Manages popover overlays attached to building markers on the index map | 72 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingLayout.vue` | vue-component | buildings | Shell layout for building detail pages providing nav and slot regions | 195 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingList.vue` | vue-component | buildings | Scrollable list of buildings with search and filter controls | 103 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingListConfigItem.vue` | vue-component | buildings | Configuration row item for customising column visibility in the building list | 123 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingListItem.vue` | vue-component | buildings | Single row in the building list displaying name, status and quick actions | 95 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingLocation.vue` | vue-component | buildings | Displays and edits the geographic location (lat/lng) for a building | 71 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingMap.vue` | vue-component | buildings | Interactive map showing building location using a mapping library | 84 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingMapMarker.vue` | vue-component | buildings | Custom map marker component for a single building pin | 89 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingPicker.vue` | vue-component | buildings | Searchable dropdown for selecting a building from the full list | 93 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingSchedules.vue` | vue-component | buildings | Lists and manages operational schedules assigned to a building | 142 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingStatusDisplay.vue` | vue-component | buildings | Badge or indicator displaying current operational status of a building | 153 | source | Y |
| `reflow-frontend/src/components/buildings/BuildingSubNav.vue` | vue-component | buildings | Secondary navigation tabs for building detail sections (Dashboard, Floors, Alarms…) | 211 | source | Y |
| `reflow-frontend/src/components/buildings/StatusWrap.vue` | vue-component | buildings | Generic wrapper that applies status-based styling to its slotted content | 76 | source | Y |
| `reflow-frontend/src/components/cards/BaseCard.vue` | vue-component | cards | Base card wrapper component providing shared card layout, drag handles, and edit-mode controls | 125 | source | Y |
| `reflow-frontend/src/components/cards/BaseFlipCard.vue` | vue-component | cards | Flip-animation card base that toggles between front (display) and back (config) faces | 323 | source | Y |
| `reflow-frontend/src/components/cards/CardList.vue` | vue-component | cards | Draggable grid of dashboard cards with add/remove controls and layout serialization | 397 | source | Y |
| `reflow-frontend/src/components/cards/CircleCard.vue` | vue-component | cards | Circular gauge/value card showing a Niagara point value with configurable color thresholds | 284 | source | Y |
| `reflow-frontend/src/components/cards/DividerCard.vue` | vue-component | cards | Visual divider card used to separate groups of cards on the dashboard | 72 | source | Y |
| `reflow-frontend/src/components/cards/FeaturedCard.vue` | vue-component | cards | Featured/hero card with large text and optional background image for key dashboard metrics | 72 | source | Y |
| `reflow-frontend/src/components/cards/Gauge.vue` | vue-component | cards | SVG gauge card rendering a semi-circular dial for numeric Niagara point values | 328 | source | Y |
| `reflow-frontend/src/components/cards/GroupCard.vue` | vue-component | cards | Card group container that renders nested CardList for hierarchical dashboard layouts | 239 | source | Y |
| `reflow-frontend/src/components/cards/GroupCardForm.vue` | vue-component | cards | Configuration form for GroupCard — sets group label and layout options | 52 | source | Y |
| `reflow-frontend/src/components/cards/HX.vue` | vue-component | cards | Heat exchanger status card displaying inlet/outlet temperatures and efficiency metrics | 112 | source | Y |
| `reflow-frontend/src/components/cards/Hyperlink.vue` | vue-component | cards | Hyperlink card that renders a clickable tile navigating to an internal route or external URL | 270 | source | Y |
| `reflow-frontend/src/components/cards/NoteCard.vue` | vue-component | cards | Freeform text/note card with rich-text editing capability for dashboard annotations | 267 | source | Y |
| `reflow-frontend/src/components/cards/NoteGrid.vue` | vue-component | cards | Grid layout wrapper for multiple NoteCard instances on a single dashboard page | 139 | source | Y |
| `reflow-frontend/src/components/cards/TableCard.vue` | vue-component | cards | Tabular data card showing multiple Niagara points in a configurable column layout | 325 | source | Y |
| `reflow-frontend/src/components/cards/ToggleCard.vue` | vue-component | cards | Boolean toggle card that reads and writes a Niagara BooleanWritable point via WebSocket | 376 | source | Y |
| `reflow-frontend/src/components/cards/URLCard.vue` | vue-component | cards | URL embed card that renders an iframe with a configurable external URL inside the dashboard | 79 | source | Y |
| `reflow-frontend/src/components/cards/table/Cell.vue` | vue-component | cards | Individual table cell for TableCard — renders value, unit, and alarm state for a single point | 110 | source | Y |
| `reflow-frontend/src/components/cards/table/MobileRowCard.vue` | vue-component | cards | Mobile-optimized row card for TableCard that stacks point data vertically on small screens | 88 | source | Y |
| `reflow-frontend/src/components/charts/Chart.vue` | vue-component | charts | Main chart orchestrator that composes D3chart, toolbar, and type picker for history visualization | 469 | source | Y |
| `reflow-frontend/src/components/charts/ChartExportPicker.vue` | vue-component | charts | Dropdown picker for exporting chart data as CSV or PNG image | 102 | source | Y |
| `reflow-frontend/src/components/charts/ChartToolBar.vue` | vue-component | charts | Toolbar for chart controls: time range selector, zoom reset, export, and type switching | 356 | source | Y |
| `reflow-frontend/src/components/charts/ChartTypePicker.vue` | vue-component | charts | Picker component for selecting chart render type: line, bar, area, or scatter | 134 | source | Y |
| `reflow-frontend/src/components/charts/ContextMenu.vue` | vue-component | charts | Right-click context menu overlay for chart interactions: add trend, copy value, zoom to selection | 127 | source | Y |
| `reflow-frontend/src/components/charts/D3chart.vue` | vue-component | charts | Core D3.js chart rendering engine (3114 LOC) handling multi-series time-series, zoom, brush, and tooltips | 3114 | source | Y |
| `reflow-frontend/src/components/charts/DeltaSymbol.vue` | vue-component | charts | Small symbol component displaying delta/change value between two chart data points | 41 | source | Y |
| `reflow-frontend/src/components/charts/GraphicReflow.vue` | vue-component | charts | SVG-based graphic/schematic viewer that overlays live Niagara point values on a floor plan graphic | 534 | source | Y |
| `reflow-frontend/src/components/charts/GraphicSelect.vue` | vue-component | charts | Picker for selecting a graphic/schematic file from the Niagara ORD tree for use in GraphicReflow | 306 | source | Y |
| `reflow-frontend/src/components/charts/Sparkline.vue` | vue-component | charts | Inline mini sparkline chart for compact trend visualization within cards and tables | 431 | source | Y |
| `reflow-frontend/src/components/charts/TimeRangePicker.vue` | vue-component | charts | Date/time range picker component for selecting history query start and end timestamps | 190 | source | Y |
| `reflow-frontend/src/components/common/ActionForm.vue` | vue-component | common | Generic form for invoking Niagara BComponent actions with typed parameter inputs | 205 | source | Y |
| `reflow-frontend/src/components/common/ActiveColorPicker.vue` | vue-component | common | Color picker bound to the active theme color slot; saves selection to config store | 106 | source | Y |
| `reflow-frontend/src/components/common/BoundLabel.vue` | vue-component | common | Live-updating label that subscribes to a Niagara point ORD and displays its current value and status | 432 | source | Y |
| `reflow-frontend/src/components/common/DraggableTracker.vue` | vue-component | common | Minimal renderless component tracking drag state for use in drag-and-drop card layouts | 14 | source | Y |
| `reflow-frontend/src/components/common/ExpandTransition.vue` | vue-component | common | Reusable CSS height-expand/collapse transition wrapper for collapsible UI sections | 38 | source | Y |
| `reflow-frontend/src/components/common/IconBrowser.vue` | vue-component | common | Searchable icon browser dialog showing all available Reflow icon glyphs for picker selection | 389 | source | Y |
| `reflow-frontend/src/components/common/IconMarker.vue` | vue-component | common | Map marker icon component rendering a themed Reflow glyph at a geographic coordinate | 79 | source | Y |
| `reflow-frontend/src/components/common/IconPicker.vue` | vue-component | common | Inline icon picker input that opens IconBrowser and emits the selected icon name | 144 | source | Y |
| `reflow-frontend/src/components/common/IconTip.vue` | vue-component | common | Tooltip-wrapped icon button providing contextual help hints in form fields | 27 | source | Y |
| `reflow-frontend/src/components/common/ImageSelect.vue` | vue-component | common | Image selection input that opens ImageBrowser and emits the chosen image ORD path | 353 | source | Y |
| `reflow-frontend/src/components/common/LinkPicker.vue` | vue-component | common | URL/route picker allowing selection of an internal view or external URL for hyperlink cards | 37 | source | Y |
| `reflow-frontend/src/components/common/LoadingScreen.vue` | vue-component | common | Full-viewport loading spinner overlay displayed during initial WebSocket authentication | 102 | source | Y |
| `reflow-frontend/src/components/common/LocationInput.vue` | vue-component | common | Geographic coordinate input with map preview for setting building or point location | 106 | source | Y |
| `reflow-frontend/src/components/common/LockedColorPicker.vue` | vue-component | common | Color picker variant that enforces a locked palette preventing out-of-theme color selection | 145 | source | Y |
| `reflow-frontend/src/components/common/OrdEmbed.vue` | vue-component | common | Iframe-based ORD path embedder that renders Niagara Px/Hx views within the Reflow UI | 264 | source | Y |
| `reflow-frontend/src/components/common/OrdTree.vue` | vue-component | common | Full Niagara ORD tree browser (3242 LOC) with lazy-load, search, and multi-select for point picking | 3242 | source | Y |
| `reflow-frontend/src/components/common/OrdTreeItem.vue` | vue-component | common | Recursive tree node component for OrdTree rendering a single Niagara component with children | 926 | source | Y |
| `reflow-frontend/src/components/common/PreferredColorPicker.vue` | vue-component | common | Color picker saving selection to user-preferred color history for quick re-use | 236 | source | Y |
| `reflow-frontend/src/components/common/TrialBanner.vue` | vue-component | common | Top-of-page banner displayed during trial period showing days remaining and upgrade CTA | 83 | source | Y |
| `reflow-frontend/src/components/common/TrialModal.vue` | vue-component | common | Modal dialog shown when trial expires prompting license activation or contact sales | 67 | source | Y |
| `reflow-frontend/src/components/common/URLEmbed.vue` | vue-component | common | Iframe wrapper that safely embeds external URLs with sandbox attributes inside the dashboard | 105 | source | Y |
| `reflow-frontend/src/components/common/URLInput.vue` | vue-component | common | Validated URL text input with http/https scheme enforcement for URL card configuration | 54 | source | Y |
| `reflow-frontend/src/components/common/Underline.vue` | vue-component | common | Decorative underline accent component used in section headings across the UI | 29 | source | Y |
| `reflow-frontend/src/components/common/WhoDot.vue` | vue-component | common | Presence indicator dot showing online/offline status of a user in collaborative editing mode | 116 | source | Y |
| `reflow-frontend/src/components/common/WhoList.vue` | vue-component | common | List of active users currently editing the same dashboard page with WhoDot indicators | 103 | source | Y |
| `reflow-frontend/src/components/common/WhoUser.vue` | vue-component | common | Single user avatar/chip component used in WhoList for collaborative presence display | 93 | source | Y |
| `reflow-frontend/src/components/config/ConfigBackupControl.vue` | vue-component | config | Control panel for triggering and managing configuration backups. | 19 | source | Y |
| `reflow-frontend/src/components/config/ConfigBackupItem.vue` | vue-component | config | Single backup entry row showing timestamp, size, and restore action. | 18 | source | Y |
| `reflow-frontend/src/components/config/ConfigButton.vue` | vue-component | config | Reusable action button for config panels with loading and disabled states. | 86 | source | Y |
| `reflow-frontend/src/components/config/ConfigCell.vue` | vue-component | config | Core config table cell that routes to the correct sub-type (text, number, color, etc.). | 391 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellButton.vue` | vue-component | config | Config cell variant that renders a clickable action button. | 45 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellColor.vue` | vue-component | config | Config cell variant for picking a hex color value. | 45 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellColorPreset.vue` | vue-component | config | Config cell variant with a preset color palette swatch picker. | 94 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellDelete.vue` | vue-component | config | Config cell variant that renders a delete action with confirm step. | 124 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellEmitButton.vue` | vue-component | config | Config cell variant that emits a custom event name defined by the cell schema. | 157 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellIcon.vue` | vue-component | config | Config cell variant for selecting an icon from the icon library. | 48 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellImage.vue` | vue-component | config | Config cell variant for uploading or selecting an image asset. | 41 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellInfo.vue` | vue-component | config | Config cell variant that displays read-only informational text. | 46 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellNumber.vue` | vue-component | config | Config cell variant for numeric input with min/max/step constraints. | 71 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellPreferredColor.vue` | vue-component | config | Config cell variant for selecting a preferred theme color from defined palette. | 39 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellSave.vue` | vue-component | config | Config cell variant that triggers save for the current row with loading state. | 123 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellSelect.vue` | vue-component | config | Config cell variant for selecting from a predefined options list. | 80 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellSwitch.vue` | vue-component | config | Config cell variant for boolean toggle switch. | 44 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellText.vue` | vue-component | config | Config cell variant for free-text string input. | 47 | source | Y |
| `reflow-frontend/src/components/config/ConfigCellTitle.vue` | vue-component | config | Config cell variant that renders a section title/header row in the config table. | 19 | source | Y |
| `reflow-frontend/src/components/config/ConfigMenu.vue` | vue-component | config | Navigation menu tree for the config panel, driven by menuTree data. | 234 | source | Y |
| `reflow-frontend/src/components/config/ConfigOptions.vue` | vue-component | config | Panel for global application options accessible from the config mode. | 15 | source | Y |
| `reflow-frontend/src/components/config/ConfigReset.vue` | vue-component | config | Button + confirmation dialog for resetting config section to defaults. | 113 | source | Y |
| `reflow-frontend/src/components/config/ConfigView.vue` | vue-component | config | Root view for config mode rendering the menu + active config section panel. | 394 | source | Y |
| `reflow-frontend/src/components/config/MenuNode.vue` | vue-component | config | Recursive tree node component for rendering config menu items with children. | 317 | source | Y |
| `reflow-frontend/src/components/config/menuTree.js` | js-module | app-shell | Static menu-tree definition object used to populate the config panel navigation tree. | 169 | source | Y |
| `reflow-frontend/src/components/dashboard/DashboardCard.vue` | vue-component | dashboard | Configurable widget card rendered on a dashboard page | 195 | source | Y |
| `reflow-frontend/src/components/dashboard/DashboardLayout.vue` | vue-component | dashboard | Grid layout engine for arranging dashboard cards on a page | 343 | source | Y |
| `reflow-frontend/src/components/dashboard/DashboardPointListPoint.vue` | vue-component | dashboard | Single data point row inside a dashboard point-list widget | 161 | source | Y |
| `reflow-frontend/src/components/dashboard/DashboardTableColumnList.vue` | vue-component | dashboard | Configures the column list for a dashboard table widget | 134 | source | Y |
| `reflow-frontend/src/components/dashboard/DashboardTableRowList.vue` | vue-component | dashboard | Renders the row list inside a dashboard table widget | 112 | source | Y |
| `reflow-frontend/src/components/dashboard/DynamicColorForm.vue` | vue-component | dashboard | Form for defining dynamic color rules based on point value thresholds | 548 | source | Y |
| `reflow-frontend/src/components/dashboard/HeroContent.vue` | vue-component | dashboard | Content slot component inside a hero banner section of the landing page | 79 | source | Y |
| `reflow-frontend/src/components/dashboard/HomeHero.vue` | vue-component | dashboard | Hero banner for the home/dashboard landing page with branding and CTA | 80 | source | Y |
| `reflow-frontend/src/components/dashboard/LandingBuildings.vue` | vue-component | dashboard | Landing page section listing buildings as navigable cards | 59 | source | Y |
| `reflow-frontend/src/components/dashboard/LandingCardList.vue` | vue-component | dashboard | Generic card list section on the landing page for multiple entity types | 62 | source | Y |
| `reflow-frontend/src/components/dashboard/LandingEquipment.vue` | vue-component | dashboard | Landing page section surfacing recently active or favourite equipment | 105 | source | Y |
| `reflow-frontend/src/components/dashboard/LandingHistories.vue` | vue-component | dashboard | Landing page section showing recent history trends as a preview list | 137 | source | Y |
| `reflow-frontend/src/components/dashboard/LandingPage.vue` | vue-component | dashboard | Root landing page view composing hero, buildings, equipment, and histories sections | 71 | source | Y |
| `reflow-frontend/src/components/dashboard/RowForm.vue` | vue-component | dashboard | Complex form for configuring a dashboard table row with dynamic color and point bindings | 989 | source | Y |
| `reflow-frontend/src/components/equipment/CSVWizard.vue` | vue-component | equipment | Multi-step wizard for CSV bulk-import of equipment points | 15 | source | Y |
| `reflow-frontend/src/components/equipment/CompactGroups.vue` | vue-component | equipment | Renders a compact list of equipment groups for dense layouts | 123 | source | Y |
| `reflow-frontend/src/components/equipment/CompactPointGroup.vue` | vue-component | equipment | Renders a single compact group of data points for an equipment item | 177 | source | Y |
| `reflow-frontend/src/components/equipment/DeviceCard.vue` | vue-component | equipment | Card tile displaying device summary with live point values | 483 | source | Y |
| `reflow-frontend/src/components/equipment/DeviceForm.vue` | vue-component | equipment | Form for creating or editing a device/equipment record | 199 | source | Y |
| `reflow-frontend/src/components/equipment/DeviceGrid.vue` | vue-component | equipment | Grid layout rendering multiple DeviceCard components | 239 | source | Y |
| `reflow-frontend/src/components/equipment/DevicePicker.vue` | vue-component | equipment | Modal/inline picker for selecting a device from the equipment list | 133 | source | Y |
| `reflow-frontend/src/components/equipment/DeviceRow.vue` | vue-component | equipment | Table row rendering device data with inline actions | 314 | source | Y |
| `reflow-frontend/src/components/equipment/DeviceSelect.vue` | vue-component | equipment | Dropdown select for choosing a single device/equipment item | 38 | source | Y |
| `reflow-frontend/src/components/equipment/DeviceTable.vue` | vue-component | equipment | Table container orchestrating DeviceRow list with sorting/filtering | 28 | source | Y |
| `reflow-frontend/src/components/equipment/DeviceTip.vue` | vue-component | equipment | Tooltip/popover showing device point details on hover | 81 | source | Y |
| `reflow-frontend/src/components/equipment/DeviceTitle.vue` | vue-component | equipment | Editable title header for a device with icon and name display | 358 | source | Y |
| `reflow-frontend/src/components/equipment/DisplayStyle.vue` | vue-component | equipment | Control for toggling equipment display style (card vs table vs list) | 14 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentAdd.vue` | vue-component | equipment | Dialog/panel for adding a new equipment item to the system | 290 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentBadgeForm.vue` | vue-component | equipment | Form for creating or editing a badge/label on an equipment item | 244 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentBadgeList.vue` | vue-component | equipment | List of badges/labels associated with an equipment item | 273 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentEditor.vue` | vue-component | equipment | Main tabbed editor for an equipment item (points, attachments, graphics, relations) | 379 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentEditorAddAttachments.vue` | vue-component | equipment | Sub-panel for uploading and linking attachments to an equipment record | 254 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentEditorAttachments.vue` | vue-component | equipment | Tab pane listing existing attachments for an equipment item | 228 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentEditorGraphic.vue` | vue-component | equipment | Tab pane for assigning or editing the graphic/floorplan asset of an equipment item | 282 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentEditorPoints.vue` | vue-component | equipment | Tab pane for managing data-point bindings of an equipment item | 213 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentEditorServedBy.vue` | vue-component | equipment | Tab pane listing upstream equipment items that serve this device | 96 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentEditorServes.vue` | vue-component | equipment | Tab pane listing downstream equipment items served by this device | 116 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentGrid.vue` | vue-component | equipment | Grid view of equipment items using card or row sub-components | 307 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentGroupOrder.vue` | vue-component | equipment | Drag-and-drop interface for reordering equipment groups | 123 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentIndex.vue` | vue-component | equipment | Top-level container view for the equipment module with search and view-mode controls | 299 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentItemList.vue` | vue-component | equipment | Paginated list of equipment items with filtering and selection support | 559 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentItemRemap.vue` | vue-component | equipment | UI for remapping equipment items to different types or groups | 335 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentList.vue` | vue-component | equipment | Simple list layout for equipment items, wraps EquipmentItemList | 272 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentType.vue` | vue-component | equipment | View for a single equipment type showing its items and configuration | 235 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentTypeForm.vue` | vue-component | equipment | Form for creating or editing an equipment type definition | 344 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentTypeSettings.vue` | vue-component | equipment | Settings panel for configuring an equipment type (units, display, thresholds) | 101 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentTypeSummary.vue` | vue-component | equipment | Read-only summary card for an equipment type used in overview pages | 170 | source | Y |
| `reflow-frontend/src/components/equipment/EquipmentTypeSummaryEditor.vue` | vue-component | equipment | Editable version of EquipmentTypeSummary with inline save/cancel controls | 414 | source | Y |
| `reflow-frontend/src/components/equipment/GroupCard.vue` | vue-component | equipment | Card tile representing an equipment group with item count and summary | 346 | source | Y |
| `reflow-frontend/src/components/equipment/GroupRow.vue` | vue-component | equipment | Table row representing an equipment group with expandable item list | 189 | source | Y |
| `reflow-frontend/src/components/equipment/GroupedDevicesDisplay.vue` | vue-component | equipment | Renders devices grouped by type or category with collapsible sections | 149 | source | Y |
| `reflow-frontend/src/components/equipment/IconSelect.vue` | vue-component | equipment | Icon picker for assigning a visual icon to an equipment type or item | 82 | source | Y |
| `reflow-frontend/src/components/equipment/ItemEditStyle.vue` | vue-component | equipment | Inline style editor for customizing an equipment item's visual appearance | 14 | source | Y |
| `reflow-frontend/src/components/equipment/PickerModal.vue` | vue-component | equipment | Generic modal wrapper used by device and type pickers in the equipment module | 582 | source | Y |
| `reflow-frontend/src/components/equipment/TableOptionsMenu.vue` | vue-component | equipment | Dropdown menu for table-level actions (export, column visibility, etc.) | 14 | source | Y |
| `reflow-frontend/src/components/equipment/TypeSelect.vue` | vue-component | equipment | Dropdown for selecting an equipment type from the defined type list | 14 | source | Y |
| `reflow-frontend/src/components/equipment/TypesPicker.vue` | vue-component | equipment | Multi-select picker for choosing multiple equipment types at once | 15 | source | Y |
| `reflow-frontend/src/components/equipment/ViewsMenu.vue` | vue-component | equipment | Menu for switching between saved equipment views (grid, list, table) | 15 | source | Y |
| `reflow-frontend/src/components/floorplans/ActionPoptipStub.vue` | vue-component | floorplans | Stub placeholder for action poptip overlay in floorplan editor | 28 | source | Y |
| `reflow-frontend/src/components/floorplans/ActionsTab.vue` | vue-component | floorplans | Tab panel listing available actions for a selected floorplan element | 70 | source | Y |
| `reflow-frontend/src/components/floorplans/ArrowProperties.vue` | vue-component | floorplans | Properties panel for configuring arrow element attributes on a floorplan | 104 | source | Y |
| `reflow-frontend/src/components/floorplans/ArrowShape.vue` | vue-component | floorplans | Renders the visual shape of an arrow SVG element on the floorplan canvas | 92 | source | Y |
| `reflow-frontend/src/components/floorplans/BasePane.vue` | vue-component | floorplans | Abstract base pane providing shared layout and behaviour for editor side-panels | 240 | source | Y |
| `reflow-frontend/src/components/floorplans/ButtonProperties.vue` | vue-component | floorplans | Properties panel for button element configuration on a floorplan | 74 | source | Y |
| `reflow-frontend/src/components/floorplans/ButtonStyle.vue` | vue-component | floorplans | Style editor for button element appearance on a floorplan | 114 | source | Y |
| `reflow-frontend/src/components/floorplans/CanvasForm.vue` | vue-component | floorplans | Form for editing canvas-level properties such as background and dimensions | 417 | source | Y |
| `reflow-frontend/src/components/floorplans/CanvasPane.vue` | vue-component | floorplans | Main interactive canvas pane rendering all floorplan elements and handling drag/drop | 646 | source | Y |
| `reflow-frontend/src/components/floorplans/ClipboardToolbar.vue` | vue-component | floorplans | Toolbar buttons for clipboard operations (cut, copy, paste) on floorplan elements | 92 | source | Y |
| `reflow-frontend/src/components/floorplans/DynamicColorTab.vue` | vue-component | floorplans | Tab for configuring dynamic colour bindings driven by point values | 93 | source | Y |
| `reflow-frontend/src/components/floorplans/ElementButton.vue` | vue-component | floorplans | Renders a clickable button element placed on the floorplan canvas | 131 | source | Y |
| `reflow-frontend/src/components/floorplans/ElementIcon.vue` | vue-component | floorplans | Renders an icon element on the floorplan canvas | 126 | source | Y |
| `reflow-frontend/src/components/floorplans/ElementImage.vue` | vue-component | floorplans | Renders an image element placed on the floorplan canvas | 119 | source | Y |
| `reflow-frontend/src/components/floorplans/ElementLabel.vue` | vue-component | floorplans | Renders a data-bound label element displaying a live point value on the floorplan | 130 | source | Y |
| `reflow-frontend/src/components/floorplans/ElementSVGArrow.vue` | vue-component | floorplans | Renders a resizable SVG arrow shape element on the floorplan canvas | 543 | source | Y |
| `reflow-frontend/src/components/floorplans/ElementSVGPolygon.vue` | vue-component | floorplans | Renders a resizable SVG polygon/zone shape element on the floorplan canvas | 866 | source | Y |
| `reflow-frontend/src/components/floorplans/ElementText.vue` | vue-component | floorplans | Renders a static or editable text element on the floorplan canvas | 118 | source | Y |
| `reflow-frontend/src/components/floorplans/ElementsItem.vue` | vue-component | floorplans | Single row in the elements list panel representing one floorplan element | 265 | source | Y |
| `reflow-frontend/src/components/floorplans/ElementsPane.vue` | vue-component | floorplans | Side panel listing all elements on the floorplan with selection and visibility controls | 287 | source | Y |
| `reflow-frontend/src/components/floorplans/FloorAddMultiple.vue` | vue-component | floorplans | Dialog for bulk-adding multiple floor levels to a building at once | 120 | source | Y |
| `reflow-frontend/src/components/floorplans/FloorEquipment.vue` | vue-component | floorplans | Displays equipment list associated with a specific floor | 73 | source | Y |
| `reflow-frontend/src/components/floorplans/FloorForm.vue` | vue-component | floorplans | Create/edit form for floor metadata (name, level, image upload) | 288 | source | Y |
| `reflow-frontend/src/components/floorplans/FloorGrid.vue` | vue-component | floorplans | Grid view of floors in a building showing thumbnail previews | 249 | source | Y |
| `reflow-frontend/src/components/floorplans/FloorPlan.vue` | vue-component | floorplans | Root component orchestrating the read-only floorplan view for a floor | 60 | source | Y |
| `reflow-frontend/src/components/floorplans/FloorPlanCanvas.vue` | vue-component | floorplans | Canvas container rendering all elements in read-only floorplan view with live data | 883 | source | Y |
| `reflow-frontend/src/components/floorplans/FloorPlanEditor.vue` | vue-component | floorplans | Full floorplan editor wrapping canvas, toolbars and side panels in edit mode | 535 | source | Y |
| `reflow-frontend/src/components/floorplans/FloorPlanPx.vue` | vue-component | floorplans | Pixel-unit wrapper providing coordinate conversion helpers for floorplan elements | 84 | source | Y |
| `reflow-frontend/src/components/floorplans/FloorPlans.vue` | vue-component | floorplans | Index page listing all floor plans for a building with navigation | 57 | source | Y |
| `reflow-frontend/src/components/floorplans/FloorsCompact.vue` | vue-component | floorplans | Compact list view of floors within the building sidebar or widget | 419 | source | Y |
| `reflow-frontend/src/components/floorplans/FloorsFull.vue` | vue-component | floorplans | Full-page view of all floors for a building with expanded detail | 456 | source | Y |
| `reflow-frontend/src/components/floorplans/GroupProperties.vue` | vue-component | floorplans | Properties panel for a group of selected floorplan elements | 39 | source | Y |
| `reflow-frontend/src/components/floorplans/IconProperties.vue` | vue-component | floorplans | Properties panel for icon element configuration including icon picker | 81 | source | Y |
| `reflow-frontend/src/components/floorplans/IconStyle.vue` | vue-component | floorplans | Style editor for icon element colour and sizing on a floorplan | 146 | source | Y |
| `reflow-frontend/src/components/floorplans/ImageDisplay.vue` | vue-component | floorplans | Displays a floorplan background image with tint and opacity controls | 114 | source | Y |
| `reflow-frontend/src/components/floorplans/ImageProperties.vue` | vue-component | floorplans | Properties panel for image element (source URL, sizing, alt text) | 96 | source | Y |
| `reflow-frontend/src/components/floorplans/ImageTint.vue` | vue-component | floorplans | Applies a colour tint overlay to the floorplan background image | 75 | source | Y |
| `reflow-frontend/src/components/floorplans/LabelBindings.vue` | vue-component | floorplans | Editor for binding a label element to a live data point or expression | 74 | source | Y |
| `reflow-frontend/src/components/floorplans/LabelProperties.vue` | vue-component | floorplans | Properties panel for label element (format, precision, units) | 121 | source | Y |
| `reflow-frontend/src/components/floorplans/LabelStyle.vue` | vue-component | floorplans | Style editor for label element font, colour and background | 146 | source | Y |
| `reflow-frontend/src/components/floorplans/PositionSize.vue` | vue-component | floorplans | Numeric inputs for precise x/y position and width/height of a floorplan element | 111 | source | Y |
| `reflow-frontend/src/components/floorplans/PropsPane.vue` | vue-component | floorplans | Right-side properties pane showing contextual options for the selected element | 211 | source | Y |
| `reflow-frontend/src/components/floorplans/Resizer.vue` | vue-component | floorplans | Drag handles for resizing a selected element on the floorplan canvas | 111 | source | Y |
| `reflow-frontend/src/components/floorplans/StatesItem.vue` | vue-component | floorplans | Single state row in a dynamic-colour or state-based binding editor | 244 | source | Y |
| `reflow-frontend/src/components/floorplans/TabView.vue` | vue-component | floorplans | Tab switcher for Properties / Style / Actions / States panels in the editor | 86 | source | Y |
| `reflow-frontend/src/components/floorplans/TextProperties.vue` | vue-component | floorplans | Properties panel for text element content and alignment configuration | 99 | source | Y |
| `reflow-frontend/src/components/floorplans/TextStyle.vue` | vue-component | floorplans | Style editor for text element font family, size and colour | 105 | source | Y |
| `reflow-frontend/src/components/floorplans/ToolbarRight.vue` | vue-component | floorplans | Right-side toolbar with zoom controls and layer toggle actions in the editor | 277 | source | Y |
| `reflow-frontend/src/components/floorplans/ToolbarTop.vue` | vue-component | floorplans | Top toolbar with element insert tools, undo/redo, save and export actions | 369 | source | Y |
| `reflow-frontend/src/components/floorplans/ZoneProperties.vue` | vue-component | floorplans | Properties panel for zone/polygon element (name, linked equipment) | 120 | source | Y |
| `reflow-frontend/src/components/floorplans/ZoneStyle.vue` | vue-component | floorplans | Style editor for zone/polygon fill, stroke and opacity on a floorplan | 96 | source | Y |
| `reflow-frontend/src/components/floorplans/ZoomLevelPicker.vue` | vue-component | floorplans | Dropdown or slider for selecting zoom level on the floorplan canvas | 134 | source | Y |
| `reflow-frontend/src/components/histories/AuditHistory.vue` | vue-component | history | Displays the audit trail history log for a station or device. | 15 | source | Y |
| `reflow-frontend/src/components/histories/FeaturedHistoryList.vue` | vue-component | history | List of featured/pinned histories for quick access from the home screen. | 113 | source | Y |
| `reflow-frontend/src/components/histories/HistoriesCompact.vue` | vue-component | history | Compact list view of histories for embedding in dashboard cards. | 139 | source | Y |
| `reflow-frontend/src/components/histories/HistoriesFull.vue` | vue-component | history | Full-page history browser with all filter/sort controls. | 129 | source | Y |
| `reflow-frontend/src/components/histories/HistoriesHome.vue` | vue-component | history | Home view for the histories section combining list, chart, and filter panels. | 655 | source | Y |
| `reflow-frontend/src/components/histories/HistoriesMenu.vue` | vue-component | history | Side navigation menu for the histories section (groups, devices, favorites). | 295 | source | Y |
| `reflow-frontend/src/components/histories/HistoryBuilder.vue` | vue-component | history | Wizard for building a new history query by selecting device, type, and time range. | 384 | source | Y |
| `reflow-frontend/src/components/histories/HistoryBuildingPicker.vue` | vue-component | history | Dropdown for filtering histories by building. | 134 | source | Y |
| `reflow-frontend/src/components/histories/HistoryCard.vue` | vue-component | history | Card tile for a single history entry with selection, star, and summary info. | 232 | source | Y |
| `reflow-frontend/src/components/histories/HistoryChart.vue` | vue-component | history | Full-featured time-series chart for history data with toolbar, delta mode, and export. | 408 | source | Y |
| `reflow-frontend/src/components/histories/HistoryDevicePicker.vue` | vue-component | history | Picker for selecting a Niagara device to browse its available histories. | 123 | source | Y |
| `reflow-frontend/src/components/histories/HistoryForm.vue` | vue-component | history | Form for editing history metadata: name, group, building, and display options. | 311 | source | Y |
| `reflow-frontend/src/components/histories/HistoryGroupPicker.vue` | vue-component | history | Picker for selecting or creating a history group. | 166 | source | Y |
| `reflow-frontend/src/components/histories/HistoryOrderPicker.vue` | vue-component | history | Picker for selecting the sort order of history list (name, date, type). | 120 | source | Y |
| `reflow-frontend/src/components/histories/HistoryPicker.vue` | vue-component | history | Generic history picker with search and multi-select for adding histories to a card. | 140 | source | Y |
| `reflow-frontend/src/components/histories/HistoryRefresh.vue` | vue-component | history | Control for triggering a manual refresh of history data in the current view. | 75 | source | Y |
| `reflow-frontend/src/components/histories/HistoryRow.vue` | vue-component | history | Table row representation of a single history entry with inline actions. | 172 | source | Y |
| `reflow-frontend/src/components/histories/HistorySpark.vue` | vue-component | history | Sparkline chart card for a single history point on the dashboard. | 395 | source | Y |
| `reflow-frontend/src/components/histories/HistoryStationCache.vue` | vue-component | history | Renderless component that pre-warms the station history cache on mount. | 97 | source | Y |
| `reflow-frontend/src/components/histories/HistoryTable.vue` | vue-component | history | Tabular view of time-series history data with sortable columns and CSV export. | 307 | source | Y |
| `reflow-frontend/src/components/histories/HistoryTypePicker.vue` | vue-component | history | Picker for filtering histories by type (numeric, boolean, enum, string). | 139 | source | Y |
| `reflow-frontend/src/components/histories/LogHistory.vue` | vue-component | history | Displays log-type history records (string/event) in a chronological list. | 15 | source | Y |
| `reflow-frontend/src/components/layout/Content.vue` | vue-component | layout | Main content area wrapper providing router-view slot and responsive padding for page content | 30 | source | Y |
| `reflow-frontend/src/components/layout/Footer.vue` | vue-component | layout | Application footer displaying version info, copyright, and optional support link | 59 | source | Y |
| `reflow-frontend/src/components/layout/Header.vue` | vue-component | layout | Top application bar with logo, building selector, user menu, and edit-mode toggle | 184 | source | Y |
| `reflow-frontend/src/components/layout/Layout.vue` | vue-component | layout | Root authenticated layout shell composing Header, Navigation sidebar, Content area, and Footer | 328 | source | Y |
| `reflow-frontend/src/components/layout/Styles.vue` | vue-component | layout | Global CSS variable injector applying theme colors and font settings from config store | 44 | source | Y |
| `reflow-frontend/src/components/map/Map.vue` | vue-component | map | Wrapper component integrating Mapbox GL via MglMap for building location display | 38 | source | Y |
| `reflow-frontend/src/components/maps/MglAttributionControl.vue` | vue-component | maps | Mapbox GL attribution control Vue wrapper rendering the map data attribution UI element | 16 | source | Y |
| `reflow-frontend/src/components/maps/MglGeocoderControl.vue` | vue-component | maps | Mapbox Geocoder search control Vue wrapper enabling address search on the map | 21 | source | Y |
| `reflow-frontend/src/components/maps/MglMap.vue` | vue-component | maps | Core Mapbox GL map container Vue component providing the map instance and slot API | 43 | source | Y |
| `reflow-frontend/src/components/maps/MglMarker.vue` | vue-component | maps | Mapbox GL marker Vue wrapper placing a draggable marker at a lat/lng position | 21 | source | Y |
| `reflow-frontend/src/components/maps/MglNavigationControl.vue` | vue-component | maps | Mapbox GL navigation control Vue wrapper providing zoom and compass UI controls | 17 | source | Y |
| `reflow-frontend/src/components/maps/MglPopup.vue` | vue-component | maps | Mapbox GL popup Vue wrapper displaying building info tooltip on marker click | 22 | source | Y |
| `reflow-frontend/src/components/maps/MglRasterLayer.vue` | vue-component | maps | Mapbox GL raster layer Vue wrapper for overlaying tile-based imagery on the map | 18 | source | Y |
| `reflow-frontend/src/components/navigation/LinkHome.vue` | vue-component | navigation | Navigation link component routing to the home dashboard view | 31 | source | Y |
| `reflow-frontend/src/components/navigation/LinkLogout.vue` | vue-component | navigation | Navigation link that triggers Niagara session logout via WebSocket disconnect | 27 | source | Y |
| `reflow-frontend/src/components/navigation/NavDivider.vue` | vue-component | navigation | Visual divider separator between navigation menu sections | 48 | source | Y |
| `reflow-frontend/src/components/navigation/NavDropdown.vue` | vue-component | navigation | Expandable navigation dropdown rendering a group of nav items with animated open/close | 396 | source | Y |
| `reflow-frontend/src/components/navigation/NavDropdownBuilding.vue` | vue-component | navigation | Building-scoped navigation dropdown scoping all nav items under a selected building context | 396 | source | Y |
| `reflow-frontend/src/components/navigation/NavLabel.vue` | vue-component | navigation | Non-clickable label item in the navigation sidebar used as a section heading | 95 | source | Y |
| `reflow-frontend/src/components/navigation/NavLink.vue` | vue-component | navigation | Standard router-link navigation item with icon and label for the sidebar menu | 28 | source | Y |
| `reflow-frontend/src/components/navigation/Navigation.vue` | vue-component | navigation | Main sidebar navigation container composing NavLink, NavDropdown, and SubnavList items | 235 | source | Y |
| `reflow-frontend/src/components/navigation/NavigationDropdownList.vue` | vue-component | navigation | Scrollable list within a NavDropdown showing nested navigation items | 178 | source | Y |
| `reflow-frontend/src/components/navigation/NavigationForm.vue` | vue-component | navigation | Configuration form for editing navigation items: label, icon, link target, and ordering | 280 | source | Y |
| `reflow-frontend/src/components/navigation/NavigationList.vue` | vue-component | navigation | Draggable ordered list of all navigation items used in the navigation config editor | 355 | source | Y |
| `reflow-frontend/src/components/navigation/NavigationMobile.vue` | vue-component | navigation | Bottom-sheet mobile navigation drawer replacing the sidebar on small-screen devices | 187 | source | Y |
| `reflow-frontend/src/components/navigation/NavigationStyles.vue` | vue-component | navigation | Scoped style component injecting theme CSS variables for navigation color customization | 35 | source | Y |
| `reflow-frontend/src/components/navigation/SubnavColors.vue` | vue-component | navigation | Sub-navigation panel for selecting accent colors in the navigation bar configuration | 119 | source | Y |
| `reflow-frontend/src/components/navigation/SubnavForm.vue` | vue-component | navigation | Form for creating or editing a sub-navigation item under an existing NavDropdown group | 169 | source | Y |
| `reflow-frontend/src/components/navigation/SubnavList.vue` | vue-component | navigation | Draggable list of sub-navigation items within a dropdown group for reordering | 142 | source | Y |
| `reflow-frontend/src/components/pages/AssociatedPages.vue` | vue-component | pages | Displays pages associated with a building or equipment item, linking to PageView routes | 101 | source | Y |
| `reflow-frontend/src/components/pages/PageControl.vue` | vue-component | pages | Toolbar control bar for a dashboard page with edit, save, delete, and share actions | 70 | source | Y |
| `reflow-frontend/src/components/pages/PageForm.vue` | vue-component | pages | Create/edit form for a dashboard page: name, layout, background, and access profile settings | 380 | source | Y |
| `reflow-frontend/src/components/pages/PageFormLayout.vue` | vue-component | pages | Layout picker sub-form within PageForm for selecting grid vs. freeform page layout modes | 242 | source | Y |
| `reflow-frontend/src/components/pages/PageGroupForm.vue` | vue-component | pages | Form for creating a page group that organizes multiple dashboard pages under one label | 74 | source | Y |
| `reflow-frontend/src/components/pages/PageList.vue` | vue-component | pages | Sidebar or panel listing all dashboard pages with add/reorder/delete controls | 115 | source | Y |
| `reflow-frontend/src/components/pages/PageListConfigItem.vue` | vue-component | pages | Individual page row in the config-mode PageList with edit, duplicate, and delete actions | 177 | source | Y |
| `reflow-frontend/src/components/pages/PageListItem.vue` | vue-component | pages | Read-only page list item chip used in view-mode navigation to switch between pages | 88 | source | Y |
| `reflow-frontend/src/components/pages/PageNiagara.vue` | vue-component | pages | Page type rendering a Niagara Px/Hx view via OrdEmbed within the dashboard page frame | 95 | source | Y |
| `reflow-frontend/src/components/pages/PageProfiles.vue` | vue-component | pages | Page access-control panel for assigning user profiles that can view or edit a given page | 139 | source | Y |
| `reflow-frontend/src/components/pages/PageWeb.vue` | vue-component | pages | Page type rendering an external URL via URLEmbed within the dashboard page frame | 85 | source | Y |
| `reflow-frontend/src/components/points/ClassicPointGroup.vue` | vue-component | points | Legacy-style point group display rendering multiple Niagara points in a table format | 201 | source | Y |
| `reflow-frontend/src/components/points/GroupCell.vue` | vue-component | points | Cell component for a point group row showing value, status, alarm state, and write controls | 792 | source | Y |
| `reflow-frontend/src/components/points/NiagaraPoint.vue` | vue-component | points | Polymorphic point renderer selecting the appropriate cell type based on Niagara point facets | 457 | source | Y |
| `reflow-frontend/src/components/points/PointCard.vue` | vue-component | points | Dashboard card rendering a single Niagara point with value, unit, sparkline, and write capability | 816 | source | Y |
| `reflow-frontend/src/components/points/PointCell.vue` | vue-component | points | Core point cell (1107 LOC) rendering value, units, enum display, write dialog, and alarm badge | 1107 | source | Y |
| `reflow-frontend/src/components/points/PointDelete.vue` | vue-component | points | Confirmation dialog for removing a point binding from a card or group | 73 | source | Y |
| `reflow-frontend/src/components/points/PointEdit.vue` | vue-component | points | Form for editing point binding configuration: ORD, label override, unit, and display options | 167 | source | Y |
| `reflow-frontend/src/components/points/PointGroup.vue` | vue-component | points | Container grouping multiple NiagaraPoint cells under a shared label with expand/collapse | 99 | source | Y |
| `reflow-frontend/src/components/points/PointInfoBadge.vue` | vue-component | points | Info badge overlay showing full point metadata on hover: ORD, type, last-write time, status | 160 | source | Y |
| `reflow-frontend/src/components/points/PointList.vue` | vue-component | points | Scrollable list of NiagaraPoint cells for equipment detail and group views | 263 | source | Y |
| `reflow-frontend/src/components/points/PointMap.vue` | vue-component | points | Map-based point locator (899 LOC) showing Niagara points as geographic markers on a Mapbox map | 899 | source | Y |
| `reflow-frontend/src/components/points/PointPicker.vue` | vue-component | points | ORD-based point picker dialog using OrdTree for selecting a Niagara point to bind to a card | 162 | source | Y |
| `reflow-frontend/src/components/profiles/ConfigViewUsers.vue` | vue-component | profiles | Admin config view listing all Niagara users and their Reflow profile assignments | 217 | source | Y |
| `reflow-frontend/src/components/profiles/PageProfiles.vue` | vue-component | profiles | Page-level RBAC panel assigning view/edit access profiles to a specific dashboard page | 86 | source | Y |
| `reflow-frontend/src/components/profiles/ProfileBanner.vue` | vue-component | profiles | Banner shown to users with restricted profile indicating limited navigation access | 71 | source | Y |
| `reflow-frontend/src/components/profiles/UserProfile.vue` | vue-component | profiles | Full profile editor for a single Reflow user: roles, start page, access permissions, and users list | 194 | source | Y |
| `reflow-frontend/src/components/profiles/UserProfileAccess.vue` | vue-component | profiles | Access control tab within UserProfile defining which pages and features the profile can reach | 147 | source | Y |
| `reflow-frontend/src/components/profiles/UserProfileList.vue` | vue-component | profiles | List of all Reflow profiles with create/delete controls and active-profile indicator | 170 | source | Y |
| `reflow-frontend/src/components/profiles/UserProfileNav.vue` | vue-component | profiles | Tab navigation bar within the profile editor switching between roles, access, users, and start-page tabs | 115 | source | Y |
| `reflow-frontend/src/components/profiles/UserProfileRoles.vue` | vue-component | profiles | Roles tab within UserProfile for assigning Niagara-side role memberships to a profile | 104 | source | Y |
| `reflow-frontend/src/components/profiles/UserProfileStartPage.vue` | vue-component | profiles | Start-page tab within UserProfile for configuring the default landing page after login | 81 | source | Y |
| `reflow-frontend/src/components/profiles/UserProfileUsers.vue` | vue-component | profiles | Users tab within UserProfile listing Niagara users assigned to this profile with add/remove controls | 213 | source | Y |
| `reflow-frontend/src/components/profiles/UserProfiles.vue` | vue-component | profiles | Root profiles management page composing UserProfileList and UserProfile editor panels | 18 | source | Y |
| `reflow-frontend/src/components/profiles/UserProfilesControl.vue` | vue-component | profiles | Toolbar control bar for the profiles page with save and discard changes actions | 19 | source | Y |
| `reflow-frontend/src/components/profiles/profileMixin.js` | js-mixin | mixins | Vue mixin for profile-scoped component access control and active-profile switching. | 56 | source | Y |
| `reflow-frontend/src/components/schedules/ScheduleGroupForm.vue` | vue-component | schedules | Form for creating or editing a schedule group that organizes Niagara schedules by label | 151 | source | Y |
| `reflow-frontend/src/components/schedules/ScheduleList.vue` | vue-component | schedules | List of Niagara schedules with group headers, active state indicators, and navigation to ORD embed | 147 | source | Y |
| `reflow-frontend/src/components/schedules/ScheduleListConfigItem.vue` | vue-component | schedules | Config-mode schedule row with edit form, drag handle, and delete action for schedule management | 348 | source | Y |
| `reflow-frontend/src/components/schedules/ScheduleListItem.vue` | vue-component | schedules | Read-only schedule list row showing schedule name, next-run time, and enabled status | 115 | source | Y |
| `reflow-frontend/src/components/settings/BackgroundSettings.vue` | vue-component | settings | Settings panel for configuring the dashboard background image or color | 14 | source | Y |
| `reflow-frontend/src/components/settings/ColorPickerSettings.vue` | vue-component | settings | Settings panel for choosing the primary and accent theme colors for the Reflow UI | 81 | source | Y |
| `reflow-frontend/src/components/settings/GlobalSettings.vue` | vue-component | settings | Top-level settings page composing all settings sub-panels: colors, logo, background, and advanced | 71 | source | Y |
| `reflow-frontend/src/components/settings/LogoSettings.vue` | vue-component | settings | Settings panel for uploading or selecting the organization logo shown in the header | 14 | source | Y |
| `reflow-frontend/src/components/settings/OpenWelcomeWizard.vue` | vue-component | settings | Button/trigger component in settings that opens the WelcomeWizard onboarding flow | 13 | source | Y |
| `reflow-frontend/src/components/settings/OptimizeConfig.vue` | vue-component | settings | Advanced settings panel for WebSocket subscription optimization and batch size configuration | 149 | source | Y |
| `reflow-frontend/src/components/settings/ResetConfigControl.vue` | vue-component | settings | Danger-zone button in settings for resetting all Reflow config to factory defaults | 19 | source | Y |
| `reflow-frontend/src/components/settings/RestrictConfig.vue` | vue-component | settings | Toggle in settings that locks the config UI preventing non-admin users from editing | 20 | source | Y |
| `reflow-frontend/src/components/settings/SettingsAutomatedBackups.vue` | vue-component | settings | Settings panel for configuring automated backup schedule: frequency, retention, and destination | 77 | source | Y |
| `reflow-frontend/src/components/settings/SettingsBackups.vue` | vue-component | settings | Backups management page listing backup history with download, restore, and delete actions | 421 | source | Y |
| `reflow-frontend/src/components/settings/SettingsRedirectReflowView.vue` | vue-component | settings | Settings control for configuring the Niagara redirect ORD that opens Reflow on login | 53 | source | Y |
| `reflow-frontend/src/components/settings/SettingsWebCache.vue` | vue-component | settings | Settings panel for clearing or managing the browser-side web cache for the Reflow SPA | 74 | source | Y |
| `reflow-frontend/src/components/settings/SoftwareUpdates.vue` | vue-component | settings | Settings panel showing available Reflow module updates with changelog and install trigger | 88 | source | Y |
| `reflow-frontend/src/components/weather/AerisWeather.vue` | vue-component | weather | Aeris Weather API integration component fetching and caching current conditions and forecasts | 150 | source | Y |
| `reflow-frontend/src/components/weather/Weather.vue` | vue-component | weather | Main weather widget (620 LOC) composing AerisWeather data with WeatherDisplay and WeatherMap | 620 | source | Y |
| `reflow-frontend/src/components/weather/WeatherConfig.vue` | vue-component | weather | Configuration form for the weather widget: API key, location, units (C/F), and refresh interval | 169 | source | Y |
| `reflow-frontend/src/components/weather/WeatherDisplay.vue` | vue-component | weather | Display panel rendering current temperature, conditions icon, humidity, and wind speed | 234 | source | Y |
| `reflow-frontend/src/components/weather/WeatherMap.vue` | vue-component | weather | Mapbox-based weather radar/satellite map layer overlay for the weather widget | 115 | source | Y |
| `reflow-frontend/src/components/weather/WeatherSettings.vue` | vue-component | weather | Minimal settings toggle stub for enabling or disabling the weather widget on the dashboard | 15 | source | Y |
| `reflow-frontend/src/components/weather/weatherIcons.js` | js-module | app-shell | Weather icon mapping table converting weather condition codes to SVG icon identifiers. | 251 | source | Y |
| `reflow-frontend/src/components/websocket/SocketAuth.vue` | vue-component | websocket | WebSocket authentication flow component managing login credentials exchange with Niagara WS server | 173 | source | Y |
| `reflow-frontend/src/components/websocket/SocketConnect.vue` | vue-component | websocket | WebSocket connection manager handling connect/disconnect lifecycle and reconnect backoff | 139 | source | Y |
| `reflow-frontend/src/components/websocket/SocketRequest.vue` | vue-component | websocket | WebSocket subscription request component batching point ORD subscriptions to Niagara | 237 | source | Y |
| `reflow-frontend/src/components/websocket/SocketResponseError.vue` | vue-component | websocket | Error display component shown when WebSocket receives an error response from Niagara | 33 | source | Y |
| `reflow-frontend/src/components/wizard/StepBuilding.vue` | vue-component | wizard | Wizard step for configuring the first building: name, location, and Niagara ORD connection | 88 | source | Y |
| `reflow-frontend/src/components/wizard/StepColors.vue` | vue-component | wizard | Wizard step for selecting primary and accent colors for the initial theme configuration | 87 | source | Y |
| `reflow-frontend/src/components/wizard/StepFinish.vue` | vue-component | wizard | Final wizard step showing completion summary and redirecting to the home dashboard | 97 | source | Y |
| `reflow-frontend/src/components/wizard/StepLogoHero.vue` | vue-component | wizard | Wizard step for uploading the organization logo and hero image during initial setup | 88 | source | Y |
| `reflow-frontend/src/components/wizard/StepTitle.vue` | vue-component | wizard | Wizard step for entering the site/organization title shown in the header and browser tab | 52 | source | Y |
| `reflow-frontend/src/components/wizard/StepWeather.vue` | vue-component | wizard | Wizard step for configuring weather widget API key and location during onboarding | 231 | source | Y |
| `reflow-frontend/src/components/wizard/WelcomeWizard.vue` | vue-component | wizard | Multi-step onboarding wizard (754 LOC) orchestrating all Step* components for first-run setup | 754 | source | Y |
| `reflow-frontend/src/components/wizard/WizardTitle.vue` | vue-component | wizard | Branded title header component displayed at the top of each WelcomeWizard step | 41 | source | Y |
| `reflow-frontend/src/lib/alarmCache.js` | js-lib | lib | In-memory alarm record cache with TTL eviction to reduce repeated Niagara alarm queries. | 61 | source | Y |
| `reflow-frontend/src/lib/bajaHeartbeat.js` | js-lib | lib | BajaScript session heartbeat manager that periodically pings the station to prevent session timeout. | 151 | source | Y |
| `reflow-frontend/src/lib/configMigration.js` | js-lib | lib | Version migration runner applying ordered migration steps to upgrade persisted config.json from older schema versions. | 653 | source | Y |
| `reflow-frontend/src/lib/configSerializer.js` | js-lib | lib | Vuex state serializer that strips transient/excluded keys before persisting config to Niagara. | 26 | source | Y |
| `reflow-frontend/src/lib/csrf.js` | js-lib | lib | CSRF token extraction from Niagara-injected meta tags and Axios request interceptor setup. | 142 | source | Y |
| `reflow-frontend/src/lib/deepMerge.js` | js-lib | lib | Recursive deep-merge utility used by LOAD_STATE to blend loaded config onto existing Vuex state. | 21 | source | Y |
| `reflow-frontend/src/lib/eventBus.js` | js-lib | lib | Global Vue event bus instance for cross-component communication (e.g., delta-sync, reflow-state-load). | 5 | source | Y |
| `reflow-frontend/src/lib/ord.js` | js-lib | lib | ORD (Object Resolution Descriptor) string parser and builder for Niagara component addressing. | 90 | source | Y |
| `reflow-frontend/src/lib/utils.js` | js-lib | lib | General-purpose utility functions: debounce, clamp, slugify, array helpers. | 22 | source | Y |
| `reflow-frontend/src/lib/uuid.js` | js-lib | lib | UUID v4 generator used for assigning stable IDs to dashboard elements, cards, and pages. | 24 | source | Y |
| `reflow-frontend/src/main.js` | js-module | app-shell | Vue 2.7 application entry point: bootstraps Vue, registers global plugins, mounts root App component. | 123 | source | Y |
| `reflow-frontend/src/mixins/buildingConfigMixin.js` | js-mixin | mixins | Vue mixin providing building-configuration form helpers and store-dispatch wrappers. | 43 | source | Y |
| `reflow-frontend/src/mixins/canvasDragResizeMixin.js` | js-mixin | mixins | Vue mixin implementing pointer-event drag and resize handlers for floor-plan canvas elements. | 237 | source | Y |
| `reflow-frontend/src/mixins/checkedItemsMixin.js` | js-mixin | mixins | Vue mixin for multi-select checkbox list management with select-all and bulk action support. | 48 | source | Y |
| `reflow-frontend/src/mixins/clipboardMixin.js` | js-mixin | mixins | Vue mixin providing clipboard copy/paste operations for dashboard elements and ORD strings. | 121 | source | Y |
| `reflow-frontend/src/mixins/dynamicColorMixin.js` | js-mixin | mixins | Vue mixin resolving dynamic color bindings from the colors store for element theming. | 296 | source | Y |
| `reflow-frontend/src/mixins/editorPaneMixin.js` | js-mixin | mixins | Vue mixin providing shared editor pane lifecycle and panel show/hide behaviour. | 91 | source | Y |
| `reflow-frontend/src/mixins/elementMixin.js` | js-mixin | mixins | Vue mixin encapsulating floor-plan element CRUD operations, ORD linkage, and label resolution. | 374 | source | Y |
| `reflow-frontend/src/mixins/equipmentListMixin.js` | js-mixin | mixins | Vue mixin providing paginated equipment-list data access, filtering, and sort helpers. | 155 | source | Y |
| `reflow-frontend/src/mixins/equipmentMixin.js` | js-mixin | mixins | Vue mixin for single-equipment detail view: subscribes BajaScript points and maps live values. | 159 | source | Y |
| `reflow-frontend/src/mixins/historyListMixin.js` | js-mixin | mixins | Vue mixin providing history query dispatch, cache-aware data loading, and chart data formatting. | 361 | source | Y |
| `reflow-frontend/src/mixins/navItemMixin.js` | js-mixin | mixins | Vue mixin for navigation tree item components: active state, expand/collapse, and routing. | 104 | source | Y |
| `reflow-frontend/src/mixins/navigationMixin.js` | js-mixin | mixins | Vue mixin managing sidebar navigation state, breadcrumb computation, and deep-link resolution. | 198 | source | Y |
| `reflow-frontend/src/mixins/propertiesMixin.js` | js-mixin | mixins | Vue mixin providing properties-panel form binding and validation for configurable UI entities. | 181 | source | Y |
| `reflow-frontend/src/mixins/stateBaseMixin.js` | js-mixin | mixins | Vue mixin base class for store-connected components with loading/error state lifecycle helpers. | 79 | source | Y |
| `reflow-frontend/src/mixins/subscriberMixin.js` | js-mixin | mixins | BajaScript subscriber lifecycle mixin: subscribes to ORD points on mount and unsubscribes on destroy. | 131 | source | Y |
| `reflow-frontend/src/mixins/summaryViewMixin.js` | js-mixin | mixins | Vue mixin for summary dashboard views: aggregates live point values and computes KPI metrics. | 92 | source | Y |
| `reflow-frontend/src/mixins/tweenMixin.js` | js-mixin | mixins | Vue mixin providing GSAP/requestAnimationFrame tween helpers for animated value transitions. | 61 | source | Y |
| `reflow-frontend/src/plugins/baja.js` | js-plugin | plugins | Vue plugin wrapping the BajaScript client library, exposing it globally as Vue.prototype.$baja. | 48 | source | Y |
| `reflow-frontend/src/plugins/colorUtils.js` | js-plugin | plugins | Vue plugin providing global color manipulation utilities (hex/RGB/HSL conversion, contrast) as Vue.prototype.$colorUtils. | 163 | source | Y |
| `reflow-frontend/src/plugins/configMode.js` | js-plugin | plugins | Vue plugin exposing a reactive config-mode flag (view vs edit) as Vue.prototype.$configMode. | 11 | source | Y |
| `reflow-frontend/src/plugins/cookies.js` | js-plugin | plugins | Vue plugin wrapping browser cookie read/write for session persistence as Vue.prototype.$cookies. | 8 | source | Y |
| `reflow-frontend/src/plugins/gbo.js` | js-plugin | plugins | Vue plugin exposing the Niagara GBO (Generic BajaScript Object) helper as Vue.prototype.$gbo. | 72 | source | Y |
| `reflow-frontend/src/plugins/http.js` | js-plugin | plugins | Vue plugin configuring Axios with CSRF headers and base URL for Niagara REST API calls. | 79 | source | Y |
| `reflow-frontend/src/plugins/labelForItem.js` | js-plugin | plugins | Vue plugin providing a global label-resolution helper for BajaScript display names as Vue.prototype.$labelForItem. | 36 | source | Y |
| `reflow-frontend/src/plugins/niagara.js` | js-plugin | plugins | Vue plugin bootstrapping the full Niagara client context (requires, baja init) and attaching it as Vue.prototype.$niagara. | 269 | source | Y |
| `reflow-frontend/src/plugins/ord.js` | js-plugin | plugins | Vue plugin exposing ORD parsing and resolution utilities globally as Vue.prototype.$ord. | 42 | source | Y |
| `reflow-frontend/src/plugins/reflowLink.js` | js-plugin | plugins | Vue plugin providing cross-page Reflow deep-link URL generation as Vue.prototype.$reflowLink. | 99 | source | Y |
| `reflow-frontend/src/plugins/timePlugin.js` | js-plugin | plugins | Vue plugin providing Niagara-aware time formatting and timezone helpers as Vue.prototype.$time. | 89 | source | Y |
| `reflow-frontend/src/plugins/utils.js` | js-plugin | plugins | Vue plugin exposing general-purpose utility functions (debounce, clamp, slugify) as Vue.prototype.$utils. | 69 | source | Y |
| `reflow-frontend/src/plugins/workbench.js` | js-plugin | plugins | Vue plugin bridging the Niagara Workbench JS API for embedded workbench context detection. | 25 | source | Y |
| `reflow-frontend/src/router/index.js` | js-router | router | Vue Router 3 configuration with 32 route definitions covering views, lazy-loaded pages, and navigation guards. | 274 | source | Y |
| `reflow-frontend/src/store/index.js` | js-store | store | Root Vuex store with 28 root state properties, LOAD_STATE/STATE_DELTA/REPLACE_STATE mutations, and 14 persistent + 15 transient namespaced modules. | 386 | source | Y |
| `reflow-frontend/src/store/modules/alarmData.js` | js-store | store | Transient Vuex module caching live alarm records fetched at runtime from the Niagara backend. | 155 | source | Y |
| `reflow-frontend/src/store/modules/alarms.js` | js-store | store | Persistent Vuex module for alarm configuration: alarm rules, display settings, and filter preferences. | 238 | source | Y |
| `reflow-frontend/src/store/modules/buildings.js` | js-store | store | Persistent Vuex module for building/site hierarchy configuration including floor plans and zones. | 783 | source | Y |
| `reflow-frontend/src/store/modules/colors.js` | js-store | store | Persistent Vuex module storing custom color palette definitions used across dashboard components. | 267 | source | Y |
| `reflow-frontend/src/store/modules/dashboardCards.js` | js-store | store | Persistent Vuex module for dashboard card definitions, positions, and display configuration. | 169 | source | Y |
| `reflow-frontend/src/store/modules/demo.js` | js-store | store | Transient Vuex module providing demo/mock data mode flag and sample data fixtures. | 77 | source | Y |
| `reflow-frontend/src/store/modules/documentData.js` | js-store | store | Transient Vuex module holding runtime document/attachment records fetched on demand. | 78 | source | Y |
| `reflow-frontend/src/store/modules/equipment.js` | js-store | store | Persistent Vuex module for equipment/asset definitions, ORD bindings, and point-map configuration. | 1130 | source | Y |
| `reflow-frontend/src/store/modules/equipmentData.js` | js-store | store | Transient Vuex module holding runtime equipment point values subscribed via BajaScript. | 27 | source | Y |
| `reflow-frontend/src/store/modules/floorEditor.js` | js-store | store | Transient Vuex module managing floor-plan editor state: drag-resize positions, selection, and undo history. | 1327 | source | Y |
| `reflow-frontend/src/store/modules/floorplans.js` | js-store | store | Persistent Vuex module storing floor plan metadata, layer visibility settings, and SVG asset references. | 268 | source | Y |
| `reflow-frontend/src/store/modules/histories.js` | js-store | store | Persistent Vuex module for history query configurations and time-range chart presets. | 139 | source | Y |
| `reflow-frontend/src/store/modules/historyCache.js` | js-store | store | Transient Vuex module caching Niagara history query results to avoid redundant network fetches. | 516 | source | Y |
| `reflow-frontend/src/store/modules/landing.js` | js-store | store | Persistent Vuex module managing landing page layout and default view configuration. | 61 | source | Y |
| `reflow-frontend/src/store/modules/license.js` | js-store | store | Transient Vuex module caching Niagara license data and feature-flag availability. | 209 | source | Y |
| `reflow-frontend/src/store/modules/menu.js` | js-store | store | Transient Vuex module for runtime context-menu and dropdown state management. | 19 | source | Y |
| `reflow-frontend/src/store/modules/mouseData.js` | js-store | store | Transient Vuex module tracking mouse position and drag state for canvas editor interactions. | 96 | source | Y |
| `reflow-frontend/src/store/modules/navigation.js` | js-store | store | Persistent Vuex module for navigation menu structure, sidebar items, and nav-tree configuration. | 365 | source | Y |
| `reflow-frontend/src/store/modules/notify.js` | js-store | store | Transient Vuex module managing toast/snackbar notification queue. | 58 | source | Y |
| `reflow-frontend/src/store/modules/pages.js` | js-store | store | Persistent Vuex module managing custom dashboard page definitions, layouts, and widget placements. | 164 | source | Y |
| `reflow-frontend/src/store/modules/pointMapData.js` | js-store | store | Transient Vuex module maintaining runtime point-map subscription data and live BajaScript readings. | 592 | source | Y |
| `reflow-frontend/src/store/modules/profiles.js` | js-store | store | Persistent Vuex module managing user profile definitions and role-based view configurations. | 1026 | source | Y |
| `reflow-frontend/src/store/modules/scheduleData.js` | js-store | store | Transient Vuex module caching schedule objects retrieved from the Niagara scheduler service. | 253 | source | Y |
| `reflow-frontend/src/store/modules/schedules.js` | js-store | store | Persistent Vuex module for schedule display configuration and time-range presets. | 294 | source | Y |
| `reflow-frontend/src/store/modules/theme.js` | js-store | store | Persistent Vuex module managing UI theme settings (color scheme, font sizes, layout preferences). | 69 | source | Y |
| `reflow-frontend/src/store/modules/updates.js` | js-store | store | Transient Vuex module tracking available Reflow module version updates from the Niagara station. | 62 | source | Y |
| `reflow-frontend/src/store/modules/user.js` | js-store | store | Transient Vuex module holding the currently authenticated Niagara user and role information. | 100 | source | Y |
| `reflow-frontend/src/store/modules/weather.js` | js-store | store | Persistent Vuex module for weather widget configuration including location and display units. | 101 | source | Y |
| `reflow-frontend/src/store/modules/weatherData.js` | js-store | store | Transient Vuex module caching live weather API responses and forecast data. | 228 | source | Y |
| `reflow-frontend/src/views/AlarmDetails.vue` | vue-component | views | View rendering alarm detail console for a specific alarm instance with acknowledge and notes | 559 | source | Y |
| `reflow-frontend/src/views/AlarmsHome.vue` | vue-component | views | Alarms dashboard view listing active, acknowledged, and cleared alarms with filter controls | 636 | source | Y |
| `reflow-frontend/src/views/BuildingFloorDetailView.vue` | vue-component | views | View rendering a specific floor plan detail with floorplan SVG overlay and point markers | 52 | source | Y |
| `reflow-frontend/src/views/BuildingFloorsView.vue` | vue-component | views | View listing all floors of a building with thumbnails and navigation to floor detail | 122 | source | Y |
| `reflow-frontend/src/views/DebugColorView.vue` | vue-component | views | Developer debug view rendering all theme color swatches and CSS variables for QA | 28 | source | Y |
| `reflow-frontend/src/views/DeviceDetailsView.vue` | vue-component | views | Equipment device detail view showing device metadata, point list, and history charts | 350 | source | Y |
| `reflow-frontend/src/views/EmbedView.vue` | vue-component | views | Full-page ORD embed view rendering any Niagara Px/Hx view at the given ORD path via OrdEmbed | 110 | source | Y |
| `reflow-frontend/src/views/EquipmentGroupView.vue` | vue-component | views | Equipment group detail view listing all devices in a group with summary cards and filters | 346 | source | Y |
| `reflow-frontend/src/views/FloorsRedirectView.vue` | vue-component | views | Redirect view that resolves /floors to the correct building floor route based on current building context | 26 | source | Y |
| `reflow-frontend/src/views/Home.vue` | vue-component | views | Home dashboard view rendering the active page CardList with page selector and edit controls | 108 | source | Y |
| `reflow-frontend/src/views/PageView.vue` | vue-component | views | Generic page view rendering a named dashboard page by ID with CardList and page controls | 216 | source | Y |
| `reflow-frontend/src/views/SchedulesHome.vue` | vue-component | views | Schedules view composing ScheduleList with group navigation and ORD embed for schedule editing | 405 | source | Y |
| `settings.gradle.kts` | config | module-config | Gradle settings script enumerating all subprojects included in the Reflow module multi-project build. | 122 | source | Y |
