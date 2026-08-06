# Niagara N4 — Bloque 371: `webChart` (W6) — the export pipeline is a `nmodule/export` transform provider offering CSV (of the SAMPLED on-screen points, so it inherits B369's averaging loss), client-side `window.print()` (no server PDF), and a `.chart`-definition save; no image/PNG/xlsx exporter exists

> **Focus**: `webChart`, gap **W6** (export pipeline). Decision-relevant vs the report module's export ([B363]
> CSV/BOM, [B361] Workbench-only PDF). **Remittance**: B199.5 named export; B363/B361 the report export.
>
> Subject: `webChart-ux` `rc/export/exportUtil.js`, `rc/transform/*`.
> **Method**: delegated sweep + inline verify of 4 load-bearing citations (provider gate, CSV=sampled, print=client,
> chartfile=definition). Block type: **EVIDENCE**.

---

## §371.1 — A `nmodule/export` transform provider; Print is browser-only `[CERT]`

`chartWidgetTransformOperationProvider` implements the webEditors/export `TransformOperationProvider`. Its
`getTransformOperations` always registers **`ChartWidgetToChartFile` + `ChartWidgetToCsv`**, and pushes
**`ChartWidgetToPrint` only when NOT Workbench** (`if (!isWb)`, `isWb = window.niagara.env.type==="wb"`) `[CERT]`
`chartWidgetTransformOperationProvider.js:27-35`. So the browser gets 3 export ops, Workbench 2.

## §371.2 — CSV exports the SAMPLED points — it inherits [B369]'s averaging loss `[CERT]`

`ChartWidgetToCsv` delegates to `exportUtil.exportToCsv`, which reads **`series.samplingPoints()`** `[CERT]`
`exportUtil.js:75` — the on-screen ROLLED-UP series, not the raw history records. Header = `timestamp` + one column
per **enabled** series (`name (unit)`); it builds a webEditors `TableModel` and delegates stringification to
webEditors' `TableModelToCsv` (baja types → strings), with UTF-8 **BOM default-on** (the "plays nice with Excel"
note) `[CERT]` `ChartWidgetToCsv.js:102-105`. **Decision-relevant** [INFER]: because it exports `samplingPoints()`,
a CSV of a wide range carries the SAME averaged-out limit-crossing loss as the chart ([B369] §369.2) — the spike is
gone from the numbers too, unless `samplingType` is `max`/`min`. Contrast [B363]: the report's `BGridToCsv` also
emits BOM CSV but via the report module's own path; webChart routes through webEditors.

## §371.3 — Print = client `window.print()`, no server PDF `[CERT]`

`ChartWidgetToPrint.transform` calls `window.print()` in a 100 ms `setTimeout` and resolves; it is not a supplier
(produces no file) `[CERT]` `ChartWidgetToPrint.js:73-78`. This is the browser's native print / print-to-PDF — no
server rendering. Distinct from [B361]'s Workbench-only `BPxInclude→PDF` report path: webChart's "PDF" is whatever
the browser's print dialog produces.

## §371.4 — `.chart` save = the definition, not the data; no image export `[CERT]`

`ChartWidgetToChartFile.transform` = `JSON.stringify(widget.makeJson(cx))` `[CERT]`
`ChartWidgetToChartFile.js:110-111` — persists series ORDs + settings (relativizable via an `ordType`
absolute/relative enum), NOT sample data ([B376] covers `BChartFile`). And `exportUtil.js` (418 lines) contains
**no** `canvas`/`toDataURL`/`serializeToString`/SVG→PNG — there is no image/PNG/SVG exporter `[CERT]` (measured
absence). So webChart's export surface = **sampled-CSV + client-print + .chart-definition**; no server render, no
image, no native xlsx.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Provider registers ChartFile+Csv always, Print only when `!isWb` (browser) | [CERT] | `chartWidgetTransformOperationProvider.js:27-35` | ✅ read |
| 2 | CSV exports `series.samplingPoints()` (sampled), via webEditors `TableModelToCsv`, BOM default-on | [CERT] | `exportUtil.js:75`; `ChartWidgetToCsv.js:102-105` | ✅ read |
| 3 | CSV inherits B369's averaged-out spike loss | [INFER] | §371.2 from claim 2 + [B369] | ✅ reasoned |
| 4 | Print = client `window.print()`, no server PDF, no file supplier | [CERT] | `ChartWidgetToPrint.js:73-78` | ✅ read |
| 5 | `.chart` save = `JSON.stringify(widget.makeJson)` (definition); no PNG/SVG exporter in exportUtil | [CERT] | `ChartWidgetToChartFile.js:110-111`; `exportUtil.js` (measured) | ✅ read |

**Marker tally**: [CERT] ×4 · [INFER] ×1. Ratio ≈ 0.2. Block type = **EVIDENCE**. Load-bearing re-resolved to disk:
the provider gate, the CSV `samplingPoints()` fidelity, the client `window.print()`, the chartfile `makeJson`.

## Connections

- [B369] — the sampling that the CSV export also carries (the averaged spike is lost in the numbers too).
- [B363]/[B361] — the report module's CSV/PDF; webChart's export is a parallel, webEditors-routed surface.
- [B376] — `BChartFile`, the `.chart` file this writes.

## Gaps opened / queued

**W6 closed.** No child gap. Focus `webChart` progressing.
