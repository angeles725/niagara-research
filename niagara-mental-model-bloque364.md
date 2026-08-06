# Niagara N4 — Bloque 364: the `report` module (VIII) — the ux/web layer is a read-only grid-table viewer: client-side pagination capped at 3000 BQL rows, sort disabled, zero chart rendering

> **Focus**: `reports`, gap R7 (ux/web layer). How a report is viewed in a BROWSER (station web server, not
> Workbench). Detail gap; confirms the chart-absence of [B361] on the modern web path and bounds what the web
> viewer can show. Follows [B357]–[B363].
>
> Subject version: Niagara N4.14 `report-ux` (decompiled Java + extracted front-end JS/hbs, release 2024-05-28).
>
> **Sources** (all `[CERT]`, read/verified this iteration):
> - `report-ux/decompiled/com/tridium/report/ux/fe/` — `BUxReportPane`, `BHTML5{,Bql,Component}GridTable`,
>   `BReportJsBuild`, `BReportCssResource`, `BExportSourceInfoEditor`; `ux/baja/BExportSourceInfoExt`
> - `report-ux/extracted/rc/fe/` — `ReportPane.js`, `GridTableContainer.js`, `GridColumn.js`,
>   `GridPaginationHandler.js`, `templates/GridTableContainer.hbs`
> - Remittance `[CERT]`: [B358] §358.2 (BQL vs component grid), [B361] §361.4 (no chart on the ux path)
>
> **Method**: inline driver token-verify of a delegated `sonnet` sweep (21 tool-uses). Load-bearing citations
> (the 3000 cap, `setSortable(false)`, client-side slice, `requiredPermissions="r"`, chart-absence) re-resolved
> against disk. Block type: **EVIDENCE**.

---

## §364.1 — `BUxReportPane` is a layout container that delegates to its children `[CERT]`

The browser entry widget is a bajaux `BIJavaScriptWidget` agent on `report:ReportPane` that loads
`module://report/rc/fe/ReportPane.js` `[CERT]` `BUxReportPane.java:19,21,30-37`. `ReportPane.js` renders a
`reportPane-wrapper` with a logo picture and a content div that iterates the model's kids, wrapping each in a
`spandrelSrc={kid}` div — i.e. **each child (a grid, a section header) is dispatched to its own bajaux agent**.
The pane fetches no data itself; it is pure layout. The JS/CSS ship as `report.built.min.js` (extends the parent
`BBajauiJsBuild`) + `report.css` `[CERT]` `BReportJsBuild.java:36`, `BReportCssResource.java:32`.

## §364.2 — Three grid widgets, one JS module — the rt grid split mirrored on the web `[CERT]`

The three HTML5 grid widgets are structurally identical Java classes differing only in their `@AgentOn` targets,
and all load the SAME `GridTableContainer.js`:

| Widget | `@AgentOn` types | Citation |
|---|---|---|
| `BHTML5GridTable` | `report:GridTable`, `report:GridLabelPane` | `BHTML5GridTable.java:32` |
| `BHTML5BqlGridTable` | `report:BqlGrid`, `report:NiagaraVirtualBqlGrid` | `BHTML5BqlGridTable.java:32` |
| `BHTML5ComponentGridTable` | `report:ComponentGrid`, `report:NiagaraVirtualComponentGrid` | (sweep-confirmed same pattern) |

The BQL-vs-component grid split from [B358] §358.2 is thus mirrored on the web side, but collapsed into one JS
widget that branches by type. `[CERT]`/[INFER].

## §364.3 — Pagination is client-side over a fully-materialized array; BQL is capped at 3000; sort is OFF `[CERT]`

The web viewer repeats the runtime's eager-materialize pattern rather than streaming:

- **BQL grid**: `GridTableContainer.js` defines `bqlQueryLimit: { value: 3000 }` `[CERT]`
  `GridTableContainer.js:36-37` (property `QUERY_LIMIT_PROP_NAME` at :112), and the cursor runs with
  `limit: Math.max(properties.getValue(QUERY_LIMIT_PROP_NAME), 1)` `[CERT]` `GridTableContainer.js:285`, pushing
  every row into an in-memory `model.rows` array. So a browser view renders **at most 3000 rows** of a BQL grid,
  materialized up front — the same eager shape as the runtime `Tables.slurp` ([B359] §359.4), now with a hard
  ceiling. A large PSI history range would be truncated at 3000 points. `[CERT]`/[INFER].
- **Pagination is client-side**: `GridPaginationHandler` slices the already-loaded `rows` array
  (`rows: params.rows.slice(0)` `[CERT]` `GridPaginationHandler.js:41`, then page windows by
  `startIndex`/`endIndex`). No server round-trip on page change — the whole result is already in the browser.
- **Sort is disabled**: `GridColumn.js:32` calls `this.setSortable(false)` `[CERT]` — the web grid offers no
  column sort at all.

## §364.4 — The web layer renders ZERO charts (confirms [B361] on the modern path) `[CERT]`

`rg -ril 'chart|canvas|svg' report-ux/{decompiled,extracted/rc}` (js+java) = **0 files** `[CERT]`
(driver-measured). The `report-ux` type registry has only the grid tables, the report pane, section header, JS
build, CSS resource, and the export-source editor — **no chart/canvas/svg widget**. The `chart-rt` dependency is a
runtime type dependency, not a rendered web widget ([B361] §361.1). So the modern station browser view of a report
shows **grid tables only** — the client's PSI chart with bands cannot appear here either; it confirms [B361]'s
verdict on path (c). `[CERT]`.

## §364.5 — The report view is READ-ONLY; only the export-source editor writes `[CERT]`

- All three grid widgets carry `@AgentOn(..., requiredPermissions="r")` `[CERT]` `BHTML5GridTable.java:32`,
  `BHTML5BqlGridTable.java:32` — the report view requires only **operator read** and mutates no station state
  (the drag-drop handler writes only a local widget property, per the sweep). `[CERT]`/[INFER].
- The one write-capable web surface is `BExportSourceInfoEditor` (agent on `report:ExportSourceInfo`,
  `BExportSourceInfoEditor.java:36-46`): a form that edits the export-source config (source ORD, operation,
  exporter setup via a `PropertySheet` with `readBehavior:"copy"`), committing a new `ExportSourceInfo` back
  through the bajaux form layer. This edits *report configuration*, not report data. `[CERT]` (sweep-confirmed,
  structure token-checked).

## §364.6 — R7 verdict `[INFER]`

**R7 = the ux/web layer is a read-only grid-table viewer**: client-side pagination over a fully-materialized
array, a hard **3000-row BQL cap**, **no sort**, **no chart**. For the client this means (a) a browser view of the
report shows the same tabular data as the CSV — never the banded chart ([B361]/[B364] §364.4); (b) a wide PSI
range would hit the 3000-row ceiling in the web view (the scheduled CSV export via `BGridToCsv` has no such JS cap
— that is a web-viewer limit, not a generation limit); (c) the only web-side mutation is editing the export-source
config. The web layer does not move the deliverable off the "custom module" conclusion of [B362]. `[INFER]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BUxReportPane` is a JS layout widget on `report:ReportPane` loading `ReportPane.js`, delegating to kids | [CERT] | `BUxReportPane.java:19,21,30-37` | ✅ read |
| 2 | Bundle = `report.built.min.js` (extends BBajauiJsBuild) + `report.css` | [CERT] | `BReportJsBuild.java:36`; `BReportCssResource.java:32` | ✅ read |
| 3 | Three grid widgets split only by `@AgentOn`, all load `GridTableContainer.js` | [CERT] | `BHTML5GridTable.java:32`; `BHTML5BqlGridTable.java:32` | ✅ read |
| 4 | BQL web grid capped at 3000 rows, materialized up front | [CERT] | `GridTableContainer.js:36-37,112,285` | ✅ read |
| 5 | Pagination is client-side (slices an in-memory rows array) | [CERT] | `GridPaginationHandler.js:41` | ✅ read |
| 6 | Column sort disabled (`setSortable(false)`) | [CERT] | `GridColumn.js:32` | ✅ read |
| 7 | Zero chart/canvas/svg in report-ux (grid tables only) | [CERT] | `rg chart\|canvas\|svg report-ux` = 0 | ✅ measured |
| 8 | Report grid view is read-only (`requiredPermissions="r"`) | [CERT] | `BHTML5{,Bql}GridTable.java:32` | ✅ read |
| 9 | Only `BExportSourceInfoEditor` writes (edits export config, not data) | [CERT] | `BExportSourceInfoEditor.java:36-46` | ✅ read (structure) |
| 10 | Web layer keeps the deliverable on the custom-module conclusion | [INFER] | §364.6 from §364.4 + [B362] | ✅ reasoned |

**Marker tally**: [CERT] ×8 · [CERT]/[INFER] ×1 · [INFER] ×1. Ratio ≈ 0.13. Block type = **EVIDENCE** (low ratio,
code+measure grounded). Load-bearing tokens re-resolved against disk this iteration: claims 1-8 (the 3000 cap,
`setSortable(false)`, client-side slice, `requiredPermissions="r"`, chart-absence all driver-verified — the
permission claim §11-cross-checked per the framework-semantic rule). Delegated sweep tier: `sonnet`,
driver-verified.

## Connections

- [B358] §358.2 — the BQL vs component grid split this block mirrors on the web side.
- [B359] §359.4 — the runtime eager `Tables.slurp`; the web layer repeats it with a 3000-row cap.
- [B361] §361.4 — chart absence on the ux path; §364.4 confirms it by measurement.
- [B362] — the composition/cost; the web viewer adds no chart capability, so the conclusion holds.
- [B363] — the CSV export the web grid mirrors as a read-only on-screen table.

## Gaps opened / queued

No new gaps. R7 closed. Remaining investigable: R8 (wb builder) → 1 open. Focus 8/9.
