# MX60 Chihuahua — Module Mapping Index

**Module**: `mx60-chihuahua`  
**Schema version**: 1.0  
**Generated at**: 2026-05-10T02:13:16Z  
**Total entries**: 100  

## Kind distribution

| Kind | Count |
|------|------:|
| compiled-bundle | 5 |
| config | 2 |
| iife-app | 13 |
| iife-entry | 1 |
| iife-lib | 20 |
| iife-store | 7 |
| java-class | 17 |
| module-descriptor | 9 |
| resource | 7 |
| resource-image | 19 |

## Domain distribution

| Domain | Count |
|--------|------:|
| alarms-backend | 2 |
| alarms-frontend | 9 |
| app-shell | 4 |
| baja-integration | 3 |
| build-config | 2 |
| equipment-backend | 6 |
| equipment-detail | 7 |
| equipment-frontend | 4 |
| equipment-reader | 2 |
| history-backend | 1 |
| http-rest | 2 |
| module-descriptor | 9 |
| schedules-backend | 1 |
| schedules-frontend | 1 |
| service-container | 2 |
| static-resources | 32 |
| threshold-stores | 5 |
| ui-lib | 7 |
| util-backend | 1 |

## Entries (sorted by path)

| path | kind | domain | purpose | loc | status |
|------|------|--------|---------|----:|--------|
| chihuahua-rt/chihuahua-rt.gradle.kts | config | build-config | Gradle Kotlin build script for chihuahua-rt module; declares Niagara RT module dependencies, Java source sets, and slotomatic configuration. | 65 | source |
| chihuahua-rt/module-include.xml | module-descriptor | module-descriptor | RT sub-module include descriptor; declares module name, vendor, and dependency versions for chihuahua-rt Niagara module. | 10 | source |
| chihuahua-rt/module-permissions.xml | module-descriptor | module-descriptor | RT module permissions; declares Niagara permission categories for chihuahua-rt components. | 19 | source |
| chihuahua-rt/module.lexicon | module-descriptor | module-descriptor | RT module lexicon; provides i18n display names for chihuahua-rt BComponent types. | 3 | source |
| chihuahua-rt/module.palette | module-descriptor | module-descriptor | RT palette descriptor; declares BChiDashboardService and equipment BComponents for Niagara Workbench palette discovery. | 18 | source |
| chihuahua-rt/src/com/angeles/chihuahua/components/BChiCarcamo.java | java-class | equipment-backend | 8-slot BComponent for Cárcamo (sump/collection pit) equipment: label, position, planta, nivelCm, state, umbralAdvertencia, umbralCritico. | 309 | source |
| chihuahua-rt/src/com/angeles/chihuahua/components/BChiCarcamoMonitor.java | java-class | equipment-backend | Cárcamo equipment monitor BComponent; auto-creates BChiCarcamo children for parent BPlanta on started(); seeds from CARCAMO_DATA table. | 155 | source |
| chihuahua-rt/src/com/angeles/chihuahua/components/BChiDashboardService.java | java-class | service-container | Root service BComponent; ensures 6 BPlanta containers; schedules controlTick 10s for protection evaluation (inferred from mapping). | 685 | source |
| chihuahua-rt/src/com/angeles/chihuahua/components/BChiDatalogger.java | java-class | equipment-backend | 9-slot BComponent for Datalogger (pressure sensor): label, position, planta, pressurePsi, pressureBar, state, umbralAdvertencia, umbralCritico. | 338 | source |
| chihuahua-rt/src/com/angeles/chihuahua/components/BChiDataloggerMonitor.java | java-class | equipment-backend | Datalogger equipment monitor BComponent; auto-creates BChiDatalogger children for parent BPlanta on started(); seeds from datalogger table. | 154 | source |
| chihuahua-rt/src/com/angeles/chihuahua/components/BChiUp.java | java-class | equipment-backend | 37-slot BComponent for UP (Unidad Paquete) equipment: 1 label, 2 position, 15 numeric feedback, 7 boolean feedback, 5 writable commands, 7 thresholds. | 1941 | source |
| chihuahua-rt/src/com/angeles/chihuahua/components/BChiUpMonitor.java | java-class | equipment-backend | UP equipment monitor BComponent; auto-creates BChiUp children for parent BPlanta on started(); seeds from embedded table. | 257 | source |
| chihuahua-rt/src/com/angeles/chihuahua/components/BPlanta.java | java-class | service-container | Planta container BComponent (1..6); auto-creates UpMonitor, CarcamoMonitor, DataloggerMonitor on started(); filters by planta index. | 165 | source |
| chihuahua-ux/chihuahua-ux.gradle.kts | config | build-config | Gradle Kotlin build script for chihuahua-ux module; declares Niagara UX module dependencies, resource packaging, and slotomatic configuration. | 73 | source |
| chihuahua-ux/module-include.xml | module-descriptor | module-descriptor | UX sub-module include descriptor; declares module name, vendor, and dependency versions for chihuahua-ux Niagara module. | 3 | source |
| chihuahua-ux/module-permissions.xml | module-descriptor | module-descriptor | UX module permissions; declares Niagara permission categories for chihuahua-ux servlet access. | 19 | source |
| chihuahua-ux/module.lexicon | module-descriptor | module-descriptor | UX module lexicon; provides i18n display names for chihuahua-ux servlet and UX types. | 3 | source |
| chihuahua-ux/module.palette | module-descriptor | module-descriptor | UX palette descriptor; declares BChiServlet for Niagara Workbench palette discovery and servlet registration. | 9 | source |
| chihuahua-ux/src/com/angeles/chihuahua/ux/BChiServlet.java | java-class | http-rest | BWebServlet routing all /mx60/* requests; delegates to ChiServletDispatch.route(); serves 17 API endpoints + static assets under /mx60/. | 1743 | source |
| chihuahua-ux/src/com/angeles/chihuahua/ux/ChiAlarmHelper.java | java-class | alarms-backend | Queries BAlarmDatabase; handles alarm latch/unlatch/ack on BChiUp slots; serves /api/alarms, /api/alarmCounts, /api/alarms/notes. | 2041 | source |
| chihuahua-ux/src/com/angeles/chihuahua/ux/ChiAlarmQueryHelper.java | java-class | alarms-backend | Queries BAlarmDatabase grouped by source ORD; serves /api/alarms/sources and /api/alarms/source endpoints for Reflow alarms UX (ADR-2). | 379 | source |
| chihuahua-ux/src/com/angeles/chihuahua/ux/ChiEquipmentReader.java | java-class | equipment-reader | Walks BChiDashboardService station hierarchy; serializes UP, Cárcamo, Datalogger equipment slots to JSON for /api/equipment response. | 865 | source |
| chihuahua-ux/src/com/angeles/chihuahua/ux/ChiHistoryHelper.java | java-class | history-backend | Queries BHistoryDatabase for MX60 dashboard; ported from SnlsHistoryHelper.java (L21); serves /api/historyList and /api/historyData endpoints. | 619 | source |
| chihuahua-ux/src/com/angeles/chihuahua/ux/ChiJsonUtil.java | java-class | util-backend | Stateless JSON utility; escapeJson() for safe string embedding; date/number formatting helpers shared across all ux helper classes. | 270 | source |
| chihuahua-ux/src/com/angeles/chihuahua/ux/ChiScheduleHelper.java | java-class | schedules-backend | Enumerates BNumericSchedule instances from station via BOrd walk; serializes schedule data to JSON for /api/schedules response. | 254 | source |
| chihuahua-ux/src/com/angeles/chihuahua/ux/ChiServletDispatch.java | java-class | http-rest | Pure routing logic; package-private final class; all routing decisions in route() — pure-Java, WSL-testable without Niagara WebOp dependency. | 594 | source |
| chihuahua-ux/src/com/angeles/chihuahua/ux/ChiThresholdHelper.java | java-class | equipment-reader | Reads and writes threshold slots on BChiUp, BChiCarcamo, BChiDatalogger via BOrd walk; serves GET/POST /api/{type}/{ord}/threshold* endpoints. | 261 | source |
| chihuahua-ux/src/rc/css/components.css | resource | static-resources | Component-level CSS; equipment cards, alarm tables, modal dialogs, threshold forms, schedule UI styles for MX60 dashboard. | 5644 | source |
| chihuahua-ux/src/rc/css/main.css | resource | static-resources | Main application CSS; layout, header, nav-bar, page sections, footer styles for MX60 dashboard SPA. | 582 | source |
| chihuahua-ux/src/rc/ext/chartjs/chart.umd.min.js | compiled-bundle | static-resources | Chart.js v4 UMD precompiled bundle; must load before RequireJS to register window.Chart global without AMD conflict. | 14 | resource |
| chihuahua-ux/src/rc/ext/chartjs/chartjs-adapter-date-fns.bundle.min.js | compiled-bundle | static-resources | Chart.js date-fns adapter bundle; provides date/time scale support for Chart.js; must load before RequireJS. | 7 | resource |
| chihuahua-ux/src/rc/ext/threejs/addons/controls/OrbitControls.js | compiled-bundle | static-resources | Three.js OrbitControls addon; served via importmap as 'three/addons/controls/OrbitControls'; enables 3D orbit interaction in UpDetail. | 1417 | resource |
| chihuahua-ux/src/rc/ext/threejs/addons/environments/RoomEnvironment.js | compiled-bundle | static-resources | Three.js RoomEnvironment addon; provides indoor lighting environment for 3D equipment scenes in UpDetail. | 148 | resource |
| chihuahua-ux/src/rc/ext/threejs/three.module.js | compiled-bundle | static-resources | Three.js r160 ES module bundle; served via importmap as 'three'; loaded by UpDetail.js and CarcamoDetail.js for 3D equipment visualization. | 53044 | resource |
| chihuahua-ux/src/rc/fonts/inter-latin-ext.woff2 | resource | static-resources | Inter font — Latin Extended subset, woff2 format; used in MX60 dashboard UI typography. | 0 | resource |
| chihuahua-ux/src/rc/fonts/inter-latin.woff2 | resource | static-resources | Inter font — Latin subset, woff2 format; primary body and UI text font for MX60 dashboard. | 0 | resource |
| chihuahua-ux/src/rc/fonts/jetbrains-mono-400.woff2 | resource | static-resources | JetBrains Mono 400 weight — monospace font for data values, alarm codes, and JSON display in MX60 dashboard. | 0 | resource |
| chihuahua-ux/src/rc/fonts/jetbrains-mono-500.woff2 | resource | static-resources | JetBrains Mono 500 weight — medium weight monospace for emphasis in data display in MX60 dashboard. | 0 | resource |
| chihuahua-ux/src/rc/fonts/jetbrains-mono-600.woff2 | resource | static-resources | JetBrains Mono 600 weight — semibold monospace for headers and alarm severity labels in MX60 dashboard. | 0 | resource |
| chihuahua-ux/src/rc/img/MAQUILA_COMPLETA.jpeg | resource-image | static-resources | Full maquila site image; used as background or reference image in MX60 dashboard HomeMap view. | 0 | resource |
| chihuahua-ux/src/rc/img/cbre-logo.png | resource-image | static-resources | CBRE logo PNG; displayed in MX60 dashboard header or footer branding area. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P1-HP-NARANJA.jpg | resource-image | static-resources | Datalogger equipment image — DT-P1-HP state: warning (naranja/orange); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P1-HP-ROJO.jpg | resource-image | static-resources | Datalogger equipment image — DT-P1-HP state: alarm (rojo/red); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P1-HP-VERDE.jpg | resource-image | static-resources | Datalogger equipment image — DT-P1-HP state: normal (verde/green); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P1-NARANJA.jpg | resource-image | static-resources | Datalogger equipment image — DT-P1 state: warning (naranja/orange); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P1-ROJO.jpg | resource-image | static-resources | Datalogger equipment image — DT-P1 state: alarm (rojo/red); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P1-VERDE.jpg | resource-image | static-resources | Datalogger equipment image — DT-P1 state: normal (verde/green); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P2-NARANJA.jpg | resource-image | static-resources | Datalogger equipment image — DT-P2 state: warning (naranja/orange); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P2-ROJO.jpg | resource-image | static-resources | Datalogger equipment image — DT-P2 state: alarm (rojo/red); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P2-VERDE.jpg | resource-image | static-resources | Datalogger equipment image — DT-P2 state: normal (verde/green); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P3-NARANJA.jpg | resource-image | static-resources | Datalogger equipment image — DT-P3 state: warning (naranja/orange); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P3-ROJO.jpg | resource-image | static-resources | Datalogger equipment image — DT-P3 state: alarm (rojo/red); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P3-VERDE.jpg | resource-image | static-resources | Datalogger equipment image — DT-P3 state: normal (verde/green); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P5-NARANJA.jpg | resource-image | static-resources | Datalogger equipment image — DT-P5 state: warning (naranja/orange); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P5-ROJO.jpg | resource-image | static-resources | Datalogger equipment image — DT-P5 state: alarm (rojo/red); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/dataloggers/DT-P5-VERDE.jpg | resource-image | static-resources | Datalogger equipment image — DT-P5 state: normal (verde/green); used in equipment card UI. | 0 | resource |
| chihuahua-ux/src/rc/img/honeywell-h.png | resource-image | static-resources | Honeywell H icon PNG; used as browser favicon per index.html link rel=icon. | 0 | resource |
| chihuahua-ux/src/rc/img/honeywell-logo.png | resource-image | static-resources | Honeywell full logo PNG; displayed in dashboard header per index.html img src. | 0 | resource |
| chihuahua-ux/src/rc/index.html | iife-entry | static-resources | SPA entry point; loads IIFE scripts in dependency order; bootstraps MX60.DashboardApp.init() and ParticleAnimation; includes Three.js importmap. | 240 | source |
| chihuahua-ux/src/rc/js/app/AlarmCards.js | iife-lib | alarms-frontend | Full-screen alarm detail modal with 3 tabs (Summary, Table, Notes); footer actions: Acknowledge + Hyperlink + Close; §68.4 ANÁLOGO candidate. | 368 | source |
| chihuahua-ux/src/rc/js/app/AlarmDetailPage.js | iife-app | alarms-frontend | Dedicated alarm-source page (#alarms/<encodedOrd>): shows alarm drill-down table for a single source ORD; registered as page 'alarm-detail'. | 481 | source |
| chihuahua-ux/src/rc/js/app/AlarmDetailsTable.js | iife-lib | alarms-frontend | Alarm details drill-down table: row-action listeners, selection listeners, sortable columns; used as a tab inside AlarmCards. | 386 | source |
| chihuahua-ux/src/rc/js/app/AlarmLatchStore.js | iife-store | alarms-frontend | Client-side latch store per (equipId, alarmKey): sticky until reset, seeded from equipment fetch, write-through POST to /api/alarms/latch|unlatch. | 268 | source |
| chihuahua-ux/src/rc/js/app/AlarmModalActions.js | iife-lib | alarms-frontend | Poptip with 5 action buttons per alarm record (ack, notes, link, etc.); §68.4 ANÁLOGO candidate for ack flow transplant analysis. | 240 | source |
| chihuahua-ux/src/rc/js/app/AlarmNotesModal.js | iife-lib | alarms-frontend | Full-screen notes modal: GET + POST to /api/alarms/notes; §68.4 ANÁLOGO candidate for note-gate ack flow; open()/close() API. | 251 | source |
| chihuahua-ux/src/rc/js/app/AlarmsManager.js | iife-lib | alarms-frontend | In-memory alarm store + fetch layer: caches up to MAX_ALARMS entries, listener-pattern for data changes; ack mutations via /api/alarms. | 326 | source |
| chihuahua-ux/src/rc/js/app/AlarmsPage.js | iife-app | alarms-frontend | Alarms page (#alarms): planta-tabs + state-tabs toolbar, 12-card pagination grid, bulk select, CSV export; 20s polling interval via AlarmsManager. | 824 | source |
| chihuahua-ux/src/rc/js/app/BulkActionBar.js | iife-lib | alarms-frontend | Reusable sticky bulk action bar for selectable lists; displays count of selected items with bulk ack and export actions. | 110 | source |
| chihuahua-ux/src/rc/js/app/CarcamoDetail.js | iife-app | equipment-detail | Cárcamo 3D detail renderer: cutaway pit with animated water surface, level bar, sensor LED; color-threshold logic green/orange/red at 45/69%. | 1040 | source |
| chihuahua-ux/src/rc/js/app/CarcamoThresholdStore.js | iife-store | threshold-stores | Read-through cache + write-through for cárcamo level thresholds; Niagara slots are single source of truth; includes colorForReading() helper. | 216 | source |
| chihuahua-ux/src/rc/js/app/ConfigManager.js | iife-app | app-shell | Fetches and caches /api/config; deduplicated concurrent loads with callbacks; provides reload() and fallback MX60-specific config. | 141 | source |
| chihuahua-ux/src/rc/js/app/Configuracion.js | iife-app | equipment-detail | Configuración matrix page: all UPs with modoOperacion chips (MANUAL/SETPOINT/SCHEDULE); click-to-change with 5s undo toast; ModoOverrideStore-backed. | 535 | source |
| chihuahua-ux/src/rc/js/app/Confirm.js | iife-lib | ui-lib | Blocking confirmation dialog: single instance, keyboard Enter/Escape support, neutral/danger tone variants. | 116 | source |
| chihuahua-ux/src/rc/js/app/DashboardApp.js | iife-app | app-shell | Root orchestrator: boots ConfigManager and Router, manages page lifecycle (mount/destroy on nav), registers all page modules. | 309 | source |
| chihuahua-ux/src/rc/js/app/DataloggerDetail.js | iife-app | equipment-detail | Datalogger detail renderer: hero image by pressure state (VERDE/NARANJA/ROJO), reading panel, threshold form backed by DataloggerThresholdStore. | 700 | source |
| chihuahua-ux/src/rc/js/app/DataloggerThresholdStore.js | iife-store | threshold-stores | Read-through cache + write-through for datalogger pressure thresholds (inverted: lectura < umbral = degraded); Niagara slots single source of truth. | 205 | source |
| chihuahua-ux/src/rc/js/app/EquipmentCard.js | iife-app | equipment-frontend | Equipment page: planta-tab toolbar + 12-card pagination grid; groups by planta then type (UP → Cárcamo → Datalogger); destroy() cleans listeners. | 645 | source |
| chihuahua-ux/src/rc/js/app/EquipmentData.js | iife-lib | equipment-frontend | Single source of truth for equipment list: BajaScript subscription primary, REST 5s polling fallback; listener pattern for UI consumers. | 406 | source |
| chihuahua-ux/src/rc/js/app/EquipmentDetail.js | iife-app | equipment-detail | Dispatch-by-type orchestrator: reads route equipId from EquipmentData, routes to registered detail renderer; supports registerRenderer(type, fn) API. | 191 | source |
| chihuahua-ux/src/rc/js/app/EquipmentSnapshotStore.js | iife-store | equipment-frontend | Aggregator between EquipmentData and UI: coalesces BajaScript pushes into 1 throttled flush via rAF + 500ms cap; ring buffer per equipId. | 339 | source |
| chihuahua-ux/src/rc/js/app/HomeMap.js | iife-app | equipment-frontend | Home map page: clickable SVG plant zones over MAQUILA photo; sidebar with planta on/standby/alarm counts; popover with equipment list on zone click. | 889 | source |
| chihuahua-ux/src/rc/js/app/LiveHistoryBuffer.js | iife-lib | equipment-detail | In-memory ring buffer (120 pts/equipId) for live equipment snapshots; data layer only, no rendering; ported from SanLuis HistoryChart ring-buffer. | 212 | source |
| chihuahua-ux/src/rc/js/app/ModoOverrideStore.js | iife-store | threshold-stores | Optimistic override for modoOperacion per UP while writePoint propagates to Niagara; in-memory only; publish-subscribe pattern for cross-tab sync. | 65 | source |
| chihuahua-ux/src/rc/js/app/OutputOverrideStore.js | iife-store | threshold-stores | Optimistic override for device commands (fan, comp1, comp2) per UP in MANUAL mode; cleared on MANUAL exit; mirrors ModoOverrideStore pattern. | 93 | source |
| chihuahua-ux/src/rc/js/app/ParticleAnimation.js | iife-lib | app-shell | Header canvas particle animation: network-style RAF loop; exposes destroy() + pause()/resume() for lifecycle cleanup. | 169 | source |
| chihuahua-ux/src/rc/js/app/Router.js | iife-app | app-shell | Hash-based router: parses #page/params, dispatches onNavigate callback, manages hashchange listener lifecycle. | 170 | source |
| chihuahua-ux/src/rc/js/app/ScheduleView.js | iife-app | schedules-frontend | Schedules page (#schedules): auto-discovers BWeeklySchedule objects via /api/schedules; iframe-modal slot editor (inferred from mapping). | 550 | source |
| chihuahua-ux/src/rc/js/app/SharedEnv.js | iife-lib | baja-integration | Shared PMREM environment texture for all 3D detail scenes; off-screen renderer kept alive to share GPU texture across short-lived scene renderers. | 45 | source |
| chihuahua-ux/src/rc/js/app/StatusResolver.js | iife-lib | ui-lib | Single source of truth for effective equipment status: overrides underlying status with alarm when AlarmLatchStore.hasAnyLatched(id) is true. | 48 | source |
| chihuahua-ux/src/rc/js/app/SubscriptionPool.js | iife-lib | baja-integration | BajaScript subscription pool: registers point subscribers, watchdog reconnect on silence, teardown on cleanup() (inferred from mapping). | 587 | source |
| chihuahua-ux/src/rc/js/app/TimeRangePicker.js | iife-lib | equipment-detail | 15-preset time-range dropdown; selection persists to localStorage; uses MX60.util.Dropdown for iSMA 4.13.2 embedded-browser styling compat. | 161 | source |
| chihuahua-ux/src/rc/js/app/Toast.js | iife-lib | ui-lib | Bottom-right toast queue with optional undo window; Gmail-style commit/undo pattern; single active toast at a time. | 117 | source |
| chihuahua-ux/src/rc/js/app/UpDetail.js | iife-app | equipment-detail | MX60 UP detail page: 37-slot panel with MANUAL/SETPOINT/SCHEDULE modes, threshold UI, history chart, write logic, BajaScript subscription lifecycle. | 3841 | source |
| chihuahua-ux/src/rc/js/app/UpThresholdStore.js | iife-store | threshold-stores | Read-through cache + write-through for UP protection thresholds; Niagara slots are single source of truth; seedFromEquipment seeds from REST load. | 198 | source |
| chihuahua-ux/src/rc/js/lib/WritePoint.js | iife-lib | baja-integration | Canonical write helper: BajaScript p.set() primary path, REST POST /api/setpoint fallback; _bajaSetBroken dual-path selection (inferred from mapping). | 154 | source |
| chihuahua-ux/src/rc/js/util/CsvExport.js | iife-lib | ui-lib | RFC 4180 CSV download utility: columnsDef with getValue lambdas, pure browser Blob + URL.createObjectURL; no external dependencies. | 92 | source |
| chihuahua-ux/src/rc/js/util/Dropdown.js | iife-lib | ui-lib | Generic custom dropdown factory replacing native select for theming in iSMA 4.13.2 embedded browser; single-active enforcement. | 223 | source |
| chihuahua-ux/src/rc/js/util/Popover.js | iife-lib | ui-lib | Single shared body-level popover: click-outside, ESC, scroll to close; attach factory returns open()/close()/destroy() instance. | 151 | source |
| chihuahua-ux/src/rc/js/util/RelativeTime.js | iife-lib | ui-lib | Spanish relative-time formatter: buckets from seconds to weeks; format() for relative, absolute() for full locale date. No dependencies. | 73 | source |
| niagara-module.xml | module-descriptor | module-descriptor | Root Niagara module descriptor; declares module identity, vendor, and sub-module references for chihuahua-rt and chihuahua-ux. | 2 | source |

## Exclusions

| path | reason |
|------|--------|
| .gradle/ | Gradle cache directory — not project source. |
| .idea/ | IDE metadata directory — not project source. |
| chihuahua-rt/build/ | Compiled output directory — not editable source. |
| chihuahua-rt/srcTest/test/com/angeles/chihuahua/components/BChiCarcamoHistoryTest.java | Test files excluded per mapping convention. |
| chihuahua-rt/srcTest/test/com/angeles/chihuahua/components/BChiDashboardServiceTest.java | Test files excluded per mapping convention. |
| chihuahua-rt/srcTest/test/com/angeles/chihuahua/components/BChiDataLoggerHistoryTest.java | Test files excluded per mapping convention. |
| chihuahua-rt/srcTest/test/com/angeles/chihuahua/components/BChiUpProtectionSlotsTest.java | Test files excluded per mapping convention. |
| chihuahua-rt/srcTest/test/com/angeles/chihuahua/components/BChiUpSlotTest.java | Test files excluded per mapping convention. |
| chihuahua-rt/srcTest/test/com/angeles/chihuahua/components/BTestRunnerProbe.java | Test files excluded per mapping convention. |
| chihuahua-ux/build/ | Compiled output directory — not editable source. |
| chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/BChiServletIntegrationTest.java | Test files excluded per mapping convention. |
| chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/BChiServletTest.java | Test files excluded per mapping convention. |
| chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/BChiServletThresholdTest.java | Test files excluded per mapping convention. |
| chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/ChiAlarmHelperTest.java | Test files excluded per mapping convention. |
| chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/ChiAlarmQueryHelperTest.java | Test files excluded per mapping convention. |
| chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/ChiEquipmentReaderTest.java | Test files excluded per mapping convention. |
| chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/ChiHistoryHelperTest.java | Test files excluded per mapping convention. |
| chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/ChiJsonUtilTest.java | Test files excluded per mapping convention. |
| chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/ChiScheduleHelperTest.java | Test files excluded per mapping convention. |
| chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/ChiThresholdHelperTest.java | Test files excluded per mapping convention. |

_See [excluded.md](excluded.md) for full exclusion documentation._
