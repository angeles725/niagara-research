# RESEARCH-STATE — focus: webChart (ACTIVE)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPPED 2026-08-05** at the user's explicit request
> (option 3 after the reports charting-path thread closed): document the `webChart` framework as a SUBSYSTEM in
> BREADTH. The framework was only ever touched at survey depth in [B199] (spine across all layers) and probed
> for one feature in [B367] (no native alarm-limit band). The 59 authored JS classes of the render engine, the
> series/data model, the chart-type catalog, commands, field editors, and the export pipeline remain un-mapped.
>
> **Not a reports gap** — reports is CLOSED 9/9; this focus sits on the charting line ([B199], [B251]–[B259],
> [B367]). It does **not** re-derive [B199] (server servlets, 4 series types, gauge) or [B367] (the band verdict);
> both are REMITTANCE. Declared angle: the **bajaux client render engine + model** that [B199] surveyed but never
> mapped — how a webChart is composed, scaled, sampled, drawn, interacted with, extended, and exported.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 377
gaps_closed: 9
known_gaps: 9
investigable_open: 0
requires_execution_open: 1
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: webChart
status: stopped
closed_on: 2026-08-05 (9/9 W1-W9 in one session, B368-B376 + synthesis B377; one child gap W7-G1 requires-execution; §18 retro retros/2026-08-05-webChart.md, 3 kit deltas proposed WC-A/B/C)
bootstrapped_on: 2026-08-05
block_prefix: niagara-mental-model-bloqueN.md (global numbering; next free: B368)

## Pre-flight e2 — existence + MEASURED size

Root: `/home/cristian/modules/Prototipos/modulos/organized/webChart/`. Distinct classes:
- **webChart-rt**: 8 (1 `javax.baja.webChart.BIChartFactory` public API + 7 `com.tridium.webChart`: `BChartFile`,
  `BWebChartQueryRpc`, `BWebChartTimeRange`, `BWebChartTimeRangeType`, `WebChartFileServlet`,
  `WebChartQueryServlet`, `WebChartUtil`).
- **webChart-ux**: 4 Java (bridge/registration) + **59 authored JS** under `rc/` in 13 dirs: `line/` (11-layer
  render engine), `model/` (series + scales + sampling + factory), `gauge/`, `donut/`, `command/`, `fe/`
  (+`fe/color`, `fe/series`), `export/`, `transform/`, `grid/`, `menu/`, `tab/`.
- Total ≈ **71 artifacts** (12 Java + 59 JS). Original Tridium source with javadoc present for
  `javax.baja.webChart` under `docDeveloper/docDeveloper-doc/` — prefer for the public API.
- Source CONFIRMED reachable. All backlog gaps investigable from disk (bajaux JS is read directly).

## Prior coverage (REMITTANCE — do not re-derive)

- [B199] — the spine: bajaux-pure architecture (§199.1); rt servlet routes `/data` `{t,v,r,s}`, `/schedule`,
  `/boxTable` + `.chart` persistence (§199.2); 4 series types ServletSeries/ScheduleSeries/PointSeries/
  ExternalSeries + scales (§199.3); settings/field editors on webEditors (§199.4); zoom/time-range/export/
  sampling interactions (§199.5); `BCircularGaugeWidget` single-value gauge (§199.6).
- [B367] — the band verdict: no native alarm-limit band; layers are a hardcoded array in `line/Line.js:25` with no
  registry; feed carries no limit values. The band question is CLOSED — do not reopen.
- [B366] — Analytics rides webChart via `BIChartFactory` (the extension point W8 characterizes).
- [B45]/[B358] — the 12 `BWebChartTimeRangeType` values (remittance for W9).

## Coverage

| Gap | Question | Block | Status |
|---|---|---|---|
| W1 | The **line render engine** in depth — the 11 layers of `line/` (draw pipeline, redraw lifecycle, DataLayer geometry, LineStyle), beyond the composition [B367] named | B368 | closed |
| W2 | The **series/data model** in depth — `model/` (BaseModel, LineModel, BaseSeries, seriesFactory, ValueScale/BaseScale, samplingUtil, modelUtil): creation, autoscale (`chartLimitMode` locked/inclusive), resampling | B369 | closed |
| W3 | The **chart-type catalog** — line vs `donut/` vs `gauge/` vs boxTable; how a type is selected; the widget lifecycle | B372 | closed |
| W4 | The **command palette + interactions** — `command/` (addSeries, Settings, Stop, LockAxis, SetAxisValues), `menu/`, drag, zoom/pan wiring | B373 | closed |
| W5 | **Field editors + settings** — `fe/` (color, series, ChartTypeEditor, SamplingPeriodEditor, StartEndTimeRangeEditor) + `ChartSettings`; the webEditors integration in breadth | B370 | closed |
| W6 | The **export pipeline** — `export/exportUtil` + `transform/` (ExportCommand, chartWidgetTransformOperationProvider): formats (PDF/PNG/CSV/image), client- vs server-side | B371 | closed |
| W7 | The **rt server layer** in depth — WebChartQueryServlet 3 routes (schedule/boxTable beyond [B199]'s data), `BWebChartQueryRpc` (RPC vs servlet), `WebChartFileServlet`, `WebChartUtil`, permission gate | B374 | closed (+ child W7-G1) |
| W8 | The **`BIChartFactory` extension contract** — how a module registers a custom chart-type factory (agent on a type → JsInfo), the mechanism Analytics used [B366]; is charting license-gated? | B375 | closed |
| W9 | The **`.chart` file + `BWebChartTimeRange(+Type)`** — the persisted chart-definition format (`BChartFile`) + the 12 time-range types (remittance [B45]/[B358]) | B376 | closed |
| W7-G1 | **requires-execution** — does `/schedule` or `/boxTable`'s `sendError(404)`-without-`return` actually leak the response body to an unauthorized user? Container-commit-dependent ([B374] §374.2) | — | open (requires-execution) |

## Backlog (investigable)

| Priority | Gap | Notes | Status |
|---|---|---|---|
| high | W1 line render engine | CLOSED B368: hand-built D3-v3 layer engine. runLayers = duck-typed promise-chained reduce over hardcoded $layers (Line.js:25,55-65) across 4 phases (initialize/graphData/redraw/destroy); axes redraw BEFORE data. DataLayer = D3-v3 d3.svg.line() generator (:322), FULL-REDRAW (whole `d` regenerated each pass, no incrementality), 2 paths/line (normal+interpolated gaps/tail), circle.dot r=2.75/point, hardcoded branch tree by series type (isLine/isShade/isBar/isDiscrete/isBoolean). Status coloring via webChartUtil.statusToColor gated off by getStatusColoring() (:420) — the only alarm awareness (ties B367). Y autoscale ONLY under zoom & !isLocked() (:47), ticks delegated to model.scaleTicks; X domain first/last sample, ticks=floor(width/80). Zoom = d3.behavior.zoom IN-MEMORY rescale, NO servlet fetch (:145-165); alt=Y/shift=X, no brush. Engine CLOSED (no registry, no draw-primitive hook). §14: chartLimitMode NOT in axis layers (only isLocked) → it is a ValueScale/model concern = W2. [CERT]×5+2mixed+[INFER]×1, ratio 0.2 | closed |
| high | W2 series/data model | CLOSED B369. Autoscale = enum facetsLimitMode [off/inclusive/locked] default off (ChartSettings.js:107), per-series override via chartLimitMode facet (ValueScale.js:104); 3-tier precedence options-lock > facet-lock > data+inclusive; chartMin/chartMax outrank min/max. KEY FINDING: samplingType default='average' GLOBAL per chart (ChartSettings.js:117), and average DIVIDES sum/count → a limit-crossing spike is AVERAGED OUT of the line (samplingUtil.js:243); status OR-combined (:232) so alarm COLOR survives but HEIGHT doesn't; max/min preserve extreme but aren't default + one global setting can't preserve both <12 dips (min) AND >28 spikes (max). Decision-relevant remittance to reports/B362: a 2nd structural reason crossing-marking is custom on webChart. getMinMax unions facets+data across seriesList; scaleTicks delegates to primarySeries.getTicks. Point={x,y,skip,status} (wire r=trendFlags→skip via modelUtil.getSkipInfo, no point field); predicates from 2 sources (chartType string vs $recordType TypeSpec). 7 load-bearing citations driver-verified. [CERT]×6+1mixed+[INFER]×1, ratio 0.25 | closed |
| medium | W3 chart-type catalog | CLOSED B372: 2 top-level agents (line ChartWidget + single-value CircularGaugeWidget); donut = code-only BaseWidget; gauge NO limit zones. | closed |
| medium | W8 BIChartFactory extension | CLOSED B375: marker iface + agent-tag seam; NO license gate (charting free); seam adds series not draw primitive (band still needs DataLayer fork). | closed |
| medium | W7 rt server layer depth | CLOSED B374: canRead() gate; SECURITY defect (/schedule+/boxTable sendError-without-return → child W7-G1); no server sampling; RPC = cross-station metadata. | closed |
| low | W4 commands + interactions | CLOSED B373: event bus + 6 bajaux Commands + context menus + drag→seriesFactory. | closed |
| low | W5 field editors + settings | CLOSED B370: 5 setting groups; enums→FrozenEnumEditor; 7 FEs on webEditors; SimplePropertySheet surface. | closed |
| low | W6 export pipeline | CLOSED B371: CSV(sampled, inherits B369)+client-print+.chart-save; no image/xlsx. | closed |
| low | W9 .chart file + time ranges | CLOSED B376: .chart = JSON BDataFile definition; 12 time-range presets; tabs=editor panes not multi-chart. | closed |
| — | W7-G1 (requires-execution) | Does /schedule or /boxTable's sendError-without-return leak the body to an unauthorized user? Container-commit-dependent (B374 §374.2). Needs a live station + a read-denied user. | open (requires-execution) |

## Iteration history

| Block | Gap | Delegated? · model tier | Notes |
|---|---|---|---|
| (bootstrap) | — | no · inline (measured pre-flight + B199/B367 coverage read) | Focus seeded 2026-08-05. 71 artifacts measured (12 Java + 59 JS). 9 audit-first gaps W1-W9. NEXT = W1 (line render engine). |
| B370 | W5 | yes · general-purpose (fe/ + ChartSettings sweep) + inline verify (3 citations) | W5 field editors + settings. Settings = 5 baja.Component groups (chart/layers/sampling + per-series + per-scale); every enum = choiceUtil DynamicEnum bound to webEditors FrozenEnumEditor; FE catalog = 7 thin subclasses of webEditors base FEs; SimplePropertySheet (nested wb/PropertySheet) is the settings surface. [CERT]×5. |
| B371 | W6 | yes · general-purpose (export/transform sweep) + inline verify (4 citations) | W6 export. nmodule/export transform provider: ChartFile+Csv always, Print only !isWb. CSV = SAMPLED points (exportUtil.js:75) → inherits B369 averaging loss; via webEditors TableModelToCsv, BOM default-on. Print = client window.print() (no server PDF). ChartFile = JSON.stringify(makeJson) definition. No PNG/SVG/xlsx exporter. [CERT]×4+[INFER]×1. |
| B372 | W3 | yes · general-purpose (donut/gauge/module.xml sweep) + inline verify (3 citations) | W3 chart-type catalog. TWO top-level agents: ChartWidget (line, on histories/points/ChartFile/schedules) + CircularGaugeWidget (single-value); type = agent <on type> gating. Donut = code-only BaseWidget (D3 pie, static count). GAUGE HAS NO LIMIT ZONES — single status-driven fill (model.js:219 isAlarm), min/max only set scale range → nowhere in webChart is an alarm limit a colored region (reinforces B366/B367). bar/shade/discreteLine = per-series modes in the line engine. [CERT]×4+1mixed. |
| B373 | W4 | yes · general-purpose (command/menu/drag sweep) + inline verify (3 citations) | W4 commands/interactions. chartEvents.js = ~20 webchart:* bus constants. 6 bajaux Commands (addSeries/Settings/Stop/LockAxis/SetAxisValues/DialogWizard), none ToggleCommand. Context menus on series + axis (contextMenuUtil). Drag-drop ORD/point/.chart → model.addSeries → seriesFactory.make (BaseModel.js:749-752; ChartWidget.js:1429-1437). No keyboard shortcuts. [CERT]×5. |
| B374 | W7 | yes · general-purpose (webChart-rt server sweep) + inline driver-verify (security fall-through + no-server-sampling) | W7 rt server + SECURITY finding. Reads run as session user (niagara.context), gate = OrdTarget.canRead(). DEFECT: /data hard-throws PermissionException (:176) but /schedule (:138) + /boxTable (:152) call sendError(404) WITHOUT return → fall through to target.get() + encode body. [CERT] guard incomplete; [INFER] whether body leaks (sendError commit semantics, container-dependent) → child W7-G1 requires-execution. RPC (BWebChartQueryRpc) = cross-station metadata over Fox(box)+web, unrestricted invoke + internal hasOperatorRead. FileServlet = write-only doPost, hasOperatorWrite + traversal filter. NO server sampling (confirms B369); only boundary time-filter on boxTable. [CERT]×6+[INFER]×1. |
| B375 | W8 | yes · general-purpose (BIChartFactory/license sweep) + inline verify (marker iface + license grep + registry) | W8 extension + license. BIChartFactory = marker interface extends BIJavaScript (getJsInfo inherited); discovery by agent tagged webChart:IChartFactory. JS: seriesFactory registryFactory reg.resolveFirst(type,{tags:[webChart:IChartFactory]}) → factory.factory() → Promise<BaseSeries[]>. NO LICENSE GATE: 0 feature/license hits tree-wide, module.xml no <feature> → charting free with base station. NUANCE (§375.4): the seam adds a SERIES for a data type, NOT a draw primitive → a band still needs the closed DataLayer fork of B368 (open at factory layer, closed at draw layer). [CERT]×4+[INFER]×1. |
| B377 | SÍNTESIS | no · inline (reasoning over B368-B376 + B199 + reports thread) | Capstone. 5 threads: (1) hand-built engine on LEGACY D3 v3; (2) NO webChart surface draws an alarm limit (line/gauge/model); (3) open at factory layer / closed at draw layer — chart-factory unlicensed but band still a DataLayer fork; (4) default 'average' sampling erases crossings + CSV inherits it + one global setting can't show both bands; (5) charting free + read-gated with the /schedule+/boxTable servlet defect. Bottom line: webChart lowers but does not remove the custom cost of the client's banded crossing-marked chart. SYNTHESIS, [INFER]×5+1mixed (expected). §18 retro accompanies. |
| B376 | W9 | yes · general-purpose (BChartFile/timerange/tab sweep) + inline verify (3 citations) | W9 .chart + time range + tabs. FOCUS CLOSE 9/9. .chart = JSON BDataFile/BITextFile (ext chart, mime application/json), opened by ChartWidget, persists DEFINITION (series ORDs + settings via makeJson) not data. Time range = 12-value frozen enum (BWebChartTimeRangeType, default today): auto/timeRange(absolute)/today/last24Hours/yesterday/weekToDate/lastWeek/last7Days/monthToDate/lastMonth/yearToDate/lastYear — only timeRange absolute, rest relative presets; concrete range = BAbsTimeRange + period preset + startFixed/endFixed (model side of ?period= B358). grid/tab = generic settings-editor container (one Widget/Command per tab), NOT multiple charts. [CERT]×4+2mixed. |
| B369 | W2 | yes · general-purpose (model/ depth sweep, 16 tool-uses) + inline token-verify (7 load-bearing citations re-resolved) | W2 series/scale/sampling model = closed. Autoscale = 3-tier facet precedence (facetsLimitMode off/inclusive/locked default off; per-series chartLimitMode override; options-lock > facet-lock > data+inclusive). KEY: samplingType default 'average' GLOBAL → averages out a limit-crossing spike (samplingUtil.js:243, sum/count); status OR-combined (:232) so alarm color survives not height; one global setting can't preserve both <12 dips + >28 spikes → 2nd structural reason crossing-marking is custom (remittance to reports/B362). getMinMax/scaleTicks = the model calls B368's YAxisLayer delegates to. Point={x,y,skip,status}; predicates from chartType-string vs $recordType-TypeSpec (orthogonal). Depth refinement of B199 (wire r=trendFlags→client skip). [CERT]×6+1mixed+[INFER]×1+[CERT-doc-inline]×1, ratio 0.25 (EVIDENCE). |
| B368 | W1 | yes · general-purpose (11-file layer sweep) + inline token-verify (5 load-bearing citations re-resolved) + §14 | W1 line render engine = closed. Hand-built D3-v3 layer engine (not a lib, unlike Analytics c3). runLayers duck-typed promise-chain over hardcoded 9-layer array; DataLayer d3.svg.line full-redraw + series-type branch tree; status coloring gated (ties B367); Y autoscale only-under-zoom-&-unlocked, model-delegated ticks; zoom = in-memory rescale no servlet. Engine CLOSED (reinforces B367). §14: chartLimitMode is a ValueScale/model concern (W2), not the axis layer. 5 load-bearing citations driver-verified to disk. [CERT]×5+2mixed+[INFER]×1+§14, ratio 0.2 (EVIDENCE). |

## Dismissed file types

None yet — census pending at focus close. Vendored libraries (`ext/d3*`, `ext/c3*`, `*.min.js`) are EXCLUDED from
the authored count and will be dismissed explicitly at close.
