# Niagara N4 — Bloque 376: `webChart` (W9) — the `.chart` file is a JSON `BDataFile` holding the chart DEFINITION (series ORDs + settings) opened by `ChartWidget`; the time range is a 12-value frozen enum (default `today`, only `timeRange` absolute); and the tab machinery is a generic settings-editor container, not a multi-chart feature (closes the focus 9/9)

> **Focus**: `webChart`, gap **W9** (the `.chart` persisted format + time-range types + grid/tab). Closes the
> focus 9/9. **Remittance**: [B199] §199.2 (named `BChartFile`), [B45]/[B358] (the 12 time-range values),
> [B369] (the settings the chart holds).
>
> Subject: `webChart-rt` `BChartFile`, `BWebChartTimeRange(+Type)`; `webChart-ux` `rc/grid/GridEditor.js`, `rc/tab/*`.
> **Method**: delegated sweep + inline verify of the 3 load-bearing citations (the `.chart` file nature, the
> time-range enum, the tab-container semantics). Block type: **EVIDENCE**.

---

## §376.1 — `.chart` = a JSON `BDataFile`, the definition not the data `[CERT]`

`BChartFile` is `@NiagaraType(ext={@FileExt(name="chart")})`, `extends BDataFile implements BITextFile`, MIME
`application/json` `[CERT]` `BChartFile.java:29-32,47-49` — a JSON text file in the file space (addressable by ORD
as a file, not a station component), opened by the agents `webChart:ChartWidget` (toTop) + a mobile chart view
`:55-59`. The class declares no series/settings properties: it is the file wrapper; the JSON body is the chart
**definition** — series ORDs + `ChartSettings` ([B369]) serialized by `widget.makeJson` ([B371] §371.4) — **not**
sample data. `[CERT]`/[INFER] (the exact JSON schema lives in `ChartWidget.makeJson`, not this wrapper).

## §376.2 — The time range: a 12-value frozen enum, mostly relative presets `[CERT]`

`BWebChartTimeRangeType` is a frozen enum, **default `today`**, 12 values (ordinal 0-11) `[CERT]`
`BWebChartTimeRangeType.java:22,37-48`: `auto`, `timeRange`, `today`, `last24Hours`, `yesterday`, `weekToDate`,
`lastWeek`, `last7Days`, `monthToDate`, `lastMonth`, `yearToDate`, `lastYear`. Only **`timeRange`** carries fixed
absolute bounds; the rest are **relative presets recomputed each load** `[CERT]`/[INFER]. A concrete
`BWebChartTimeRange extends BAbsTimeRange` `[CERT]` `:34` carries the inherited absolute start/end PLUS a `period`
property (the preset tag, default `today`) and `startFixed`/`endFixed` flags saying whether each bound is pinned or
recomputed `[CERT]` `BWebChartTimeRange.java:33-41`. This is the model side of the `?period=` ORD substring
([B358]/[B45]): the preset rides as the `period` property. Ties the reports finding — a relative preset is
zero-code self-updating; an arbitrary user range is the absolute `timeRange` value.

## §376.3 — `grid/GridEditor` + `tab/*` = a generic settings-editor container, NOT multiple charts `[CERT]`

`GridEditor` is a "Widget that groups other Widgets and commands organized in tabs" `[CERT]` `GridEditor.js:12-13,
20-23` — a generic multi-pane editor scaffold holding `$tabs`, orchestrating initialize/read/save/load across child
editor widgets. `Tab` wraps exactly one `Widget` OR `Command` (not a chart/LineModel) `[CERT]` `Tab.js:66-78`;
`TabbedEditor extends GridEditor` draws a label per tab and shows one at a time, switching on label click `[CERT]`
`TabbedEditor.js:15,27,114-140`. **This is the Settings-dialog scaffold** ([B370]'s SimplePropertySheet groups
tile into it), **not** a "multiple charts in tabs" feature — nothing here references LineModels or chart instances.
So webChart has no native multi-chart tab set; a `.chart` file is a single chart definition. `[CERT]`/[INFER].

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `.chart` = JSON `BDataFile`/`BITextFile`, ext `chart`, mime `application/json`, opened by `ChartWidget` | [CERT] | `BChartFile.java:29-32,47-49,55-59` | ✅ sweep-cited |
| 2 | It persists the definition (series ORDs + settings via makeJson), not sample data | [CERT]/[INFER] | §376.1 + [B371] §371.4 | ✅ reasoned |
| 3 | Time-range = 12-value frozen enum, default `today`; only `timeRange` absolute, rest relative | [CERT] | `BWebChartTimeRangeType.java:22,37-48` | ✅ sweep-cited |
| 4 | Concrete range = `BAbsTimeRange` + `period` preset + startFixed/endFixed | [CERT] | `BWebChartTimeRange.java:33-41` | ✅ sweep-cited |
| 5 | grid/tab = generic settings-editor container (one Widget/Command per tab), NOT multiple charts | [CERT] | `GridEditor.js:12-23`; `Tab.js:66-78`; `TabbedEditor.js:15,114-140` | ✅ sweep-cited |

**Marker tally**: [CERT] ×4 · [CERT]/[INFER] ×2. Ratio ≈ 0.33. Block type = **EVIDENCE**. Load-bearing:
the `.chart` file nature (claim 1), the time-range enum (claim 3), the tab-container semantics (claim 5). No §14.
Focus **closed 9/9**.

## Connections

- [B199] §199.2 — named `BChartFile`; §376.1 gives its nature.
- [B358]/[B45] — the `?period=` ORD substring; §376.2 gives its model-side `period` property + the 12 presets.
- [B370] — the SimplePropertySheet groups that tile into the tab/grid settings container.
- [B371] — `ChartWidgetToChartFile` writes this file via `widget.makeJson`.

## Gaps opened / queued

**W9 closed. Focus `webChart` now 9/9** — all W1-W9 closed; one child gap `W7-G1` (requires-execution) open.
Terminal trigger: a cross-focus synthesis + the §18 self-retrospective.
