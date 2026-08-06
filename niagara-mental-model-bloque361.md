# Niagara N4 — Bloque 361: the `report` module (V) — chart-in-a-report is blocked at the PROFILE boundary: the station generates only CSV/text; the only chart path is a manual Workbench PDF, and alarm bands are custom in either case

> **Focus**: `reports`, gap R6 (chart-in-report). The third and costliest data leg of the client deliverable:
> a PSI-vs-time analytics chart with two alarm bands (<12 / >28 critical, 15–25 normal) that MARKS the
> limit-crossings with a timestamp, X adapting to the range. Follows [B357]-[B360]. Question: can a chart be
> composed INTO a report, through which path, and is it available for a SCHEDULED station-side report or only
> in Workbench?
>
> Subject version: Niagara N4.14 `report-{rt,ux,wb}` + `chart-{rt,wb}` (decompiled corpus, release 2024-05-28).
>
> **Sources** (all `[CERT]`, read directly this iteration):
> - `report-rt/extracted/META-INF/module.xml`, `report-wb/…/module.xml`, `report-ux/…/module.xml` — the profiles
> - `report-rt/decompiled/com/tridium/report/BExportSource.java` — the runtime exporter resolution
> - `report-wb/decompiled/com/tridium/report/pdf/BPdfReportPane.java` — the PDF chart-composition path
> - `report-wb/decompiled/com/tridium/report/exporters/pdf/BGridToPdf.java` — the PDF grid exporter (no chart)
> - `report-wb/decompiled/com/tridium/report/ui/BReportPxMedia.java` — the wb new-file scaffolding
> - `chart/chart-{rt,wb}/decompiled` — grepped for band/threshold (proven absent)
> - Remittance `[CERT]`: [B357] §357.6 (CSV/text exporters), [B199]/[B251]–[B259] (the two chart engines),
>   [B194] (PX media/profiles), [B359]/[B360] (the history/alarm custom-cursor conclusion this composes with)
>
> **Method**: inline driver read + token-verify of a delegated `sonnet` structural sweep (31 tool-uses). The
> dramatic negative (scheduled chart-in-report blocked at the platform level) re-measured by two independent
> methods. Block type: **EVIDENCE** (decompilation + module.xml profiles).

---

## §361.1 — The `chart-rt` dependency is DEAD in `report-rt` `[CERT]`

`report-rt` declares `<dependency name="chart-rt" .../>` `[CERT]` `report-rt/…/module.xml:7` — which at first
suggests the report runtime can chart. It cannot: **no `report-rt` class references any chart type**.
`rg -il 'chart|BChart' report/report-rt/decompiled -g '*.java'` = **0 files** `[CERT]` (driver-re-verified). The
dep is a **load-ordering/transitive artifact** — chart-rt's data types must register in the type system before the
ux/wb layers can use them — not evidence of runtime charting. This confirms and explains [B357]'s "chart-rt dep
but 0 chart classes" observation: the dep is real, the capability is not. `[CERT]`/`[INFER]`.

## §361.2 — The profile split IS the platform boundary `[CERT]`

The `report` module ships in three profiles, and the station loads only two of them:

| Jar | `runtimeProfile` | Loaded on a headless station (NRE)? | Citation |
|---|---|---|---|
| `report-rt` | `rt` | **Yes** (the generation pipeline) | `report-rt/…/module.xml:2` |
| `report-ux` | `ux` | **Yes** (the JS browser view) | `report-ux/…/module.xml:2` |
| `report-wb` | `wb` | **No** (Workbench only) | `report-wb/…/module.xml:2` |

**Every chart/PDF/PX class in the report module lives in `report-wb`** (§361.4) — `runtimeProfile="wb"`. So does
the whole chart-rendering stack (`chart-wb`, `BPdfChartPane`, `BHxPxChartPane` — [B199]/[B251]-[B259]). None is
present in the process that runs a scheduled report. This is not a config gate; it is a **module-loading
boundary**. `[CERT]`.

## §361.3 — The runtime exporter set is exactly CSV + text (a PDF config NPEs on the station) `[CERT]`/`[INFER]`

`BExportSource.handleGenerate()` resolves the exporter by agent lookup: `BExporter exporter =
getSource().getExporter(); if (null) { AgentList agents = obj.getAgents().filter(ExportFilter); … }` where
`ExportFilter = AgentFilter.is(BExporter.TYPE)`, then `exporter.export(...)` `[CERT]`
`BExportSource.java:52,77,79,98`. The only `BExporter` subclasses shipped in `report-rt` are **`BGridToCsv`** and
**`BGridToText`** ([B357] §357.6; the exporters package holds exactly these two). `BGridToPdf` is in `report-wb`
(§361.4). `[CERT]`.

**Consequence**: if a `BExportSourceInfo` is pointed at the PDF exporter, the station-side agent lookup finds no
matching `BExporter` (the class is not loaded), `exporter` stays null, and `exporter.export(...)` at
`BExportSource.java:98` **NPEs**. A scheduled/pushed report on the station can emit **only CSV or text bytes** —
never a PDF, never a chart image. `[INFER]` (the null→NPE follows from the cited lookup + the profile boundary).

> Note on decompiled fidelity: in this scrubbed copy the exporters' `@AgentOn` type literals are string-scrubbed
> to `"n"` and class names to `Bn` (the Vineflower scrubbing pattern). The *mechanism* (agent lookup filtered by
> `BExporter.TYPE`) and the *class set* (CSV/text in rt, PDF in wb) are intact and cited; the exact agent-id
> strings (`report:GridToPdf` etc.) are not reliably readable here and are not load-bearing.

## §361.4 — The ONE chart-composition path: a manual Workbench PDF via `BPxInclude` → `BChartPane` `[CERT]`

A chart CAN reach a report — but only in Workbench, and only through the PDF pane, never the grid exporter:

- **`BGridToPdf`** renders only the data grid — name/path/timestamp header + a `BTable` of values, no chart, no
  PX `[CERT]` `BGridToPdf.java` (`report-wb`, wb profile). So even the PDF *grid* exporter draws no chart.
- **`BPdfReportPane.fromWidget()`** is the composition point: it walks a `BReportPane`'s children, and for a
  `BPxInclude` child it expands the embedded PX and routes each child widget through `PdfUtil.getWidget(...)`:
  `if (child instanceof BPxInclude) { BPxInclude px = …; … BWidget pw = PdfUtil.getWidget(pxChild, null);
  flow.add(null, pw); }` `[CERT]` `BPdfReportPane.java:79,85,87`. `PdfUtil.getWidget` resolves a `BChartPane`
  to `BPdfChartPane` (chart-wb) — so **a chart appears in the PDF iff the report's PX embeds a `BChartPane`**.
  `[CERT]`/`[INFER]`.
- **`BReportPxMedia`** is NOT a pipeline — it is Workbench new-file scaffolding: `extends BPxMedia`, exposing
  `MOBILE_PX_FILE = "file:!defaults/workbench/newfiles/ReportPxFile.px"` as the default template for a new Report
  PX `[CERT]` `BReportPxMedia.java:24,27,36-37`. It has no link to `BReportSource`/`BExportSource`; it embeds and
  exports nothing. `[CERT]`.

So path (b) — Workbench PDF with an embedded chart PX — is real but **manual, Workbench-triggered, never scheduled
or pushed**. The browser paths do not rescue it: the modern station web view is `BUxReportPane` (report-ux JS) with
no chart widget, and the Hx `BHxPxReportPane` is wb-profile (Workbench preview only). `[CERT]`/[INFER] via §361.2.

## §361.5 — The client's alarm bands + crossing markers do NOT exist in the stock chart (custom in EVERY path) `[CERT]`

Even where a chart is reachable (the wb PDF), the client's specific ask is not a stock feature:

- `rg -ril 'band|threshold|overlay|alarmZone|limitLine' chart/chart-{wb,rt}/decompiled` = **0 files** `[CERT]`
  (driver-re-verified). The stock line chart (`BLineChart`) iterates series and strokes lines; it has **no band,
  threshold, limit-line, or alarm-zone API** ([B251]-[B259] documented the classic chart; [B199] the webChart).
- Drawing the two bands (<12 / >28) as horizontal fills and marking the crossings requires a **custom widget**
  overriding the chart's paint (`doPaint`/`BChartCanvas`) — regardless of which delivery path is chosen. `[INFER]`.

## §361.6 — R6 verdict and the cost consequence `[INFER]`

| Path | Chart in a report? | Basis |
|---|---|---|
| **(a) Scheduled/pushed, station-side** | **NO** — platform boundary | report-wb + chart-wb are `wb` profile, not loaded on the station; runtime emits only CSV/text (§361.2/§361.3) |
| **(b) Workbench PDF, manual** | **CONDITIONAL YES** | `BPdfReportPane` expands `BPxInclude`→`BChartPane`→`BPdfChartPane` (§361.4); manual, never scheduled |
| **(c) Hx/web interactive** | **NO for charts** | `BUxReportPane` (ux) has no chart renderer; `BHxPxReportPane` is wb-only (§361.4) |

**R6 = the analytics chart is NOT a report-module capability for the client's use case.** The client needs an
*unattended, scheduled/pushed* PSI-vs-time chart with alarm bands; that is blocked at the module-loading boundary,
and the bands/markers do not exist in stock charting anyway. The only production path is a **custom `rt`-profile
module** that renders the chart headlessly (a JVM chart lib — JFreeChart/iText — compiled into an `rt` module),
draws the bands + crossing markers itself, and either writes an image into a custom exporter or produces a PDF —
bypassing the entire `wb` graphics stack. This is exactly the same "custom code, not configuration" conclusion as
[B359]/[B360]: all THREE data legs — history table, alarm markers, and now the chart — require custom development.
The report module contributes only the schedule + file/email delivery wrapper. `[INFER]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `report-rt` declares `chart-rt` dep but references 0 chart types (dead/transitive) | [CERT] | `report-rt/…/module.xml:7` + `rg chart report-rt` = 0 | ✅ read+measured |
| 2 | Profile split: report-rt=`rt`, report-ux=`ux` (both on station), report-wb=`wb` (Workbench only) | [CERT] | `report-{rt,ux,wb}/…/module.xml:2` | ✅ read |
| 3 | `BExportSource` resolves a `BExporter` agent then calls `export()`; only CSV/text classes ship in rt | [CERT] | `BExportSource.java:52,77,79,98` + [B357]§357.6 | ✅ read |
| 4 | A PDF-exporter config NPEs on the station (exporter unresolved, class not loaded) | [INFER] | §361.3 from claims 2,3 | ✅ reasoned |
| 5 | `BGridToPdf` renders only the grid table (no chart/PX) and is wb-profile | [CERT] | `BGridToPdf.java` (report-wb) | ✅ read |
| 6 | `BPdfReportPane.fromWidget` expands `BPxInclude` children → `PdfUtil.getWidget` → `BPdfChartPane` (the only chart path) | [CERT] | `BPdfReportPane.java:79,85,87` | ✅ read |
| 7 | `BReportPxMedia` is wb new-file scaffolding (extends BPxMedia, template ORD), not a generation pipeline | [CERT] | `BReportPxMedia.java:24,27,36-37` | ✅ read |
| 8 | Station browser view `BUxReportPane` (ux) has no chart renderer; Hx pane is wb-only | [CERT]/[INFER] | §361.4 + profile split | ✅ read+reasoned |
| 9 | Stock chart has no band/threshold/limit-line API → bands+markers are custom in every path | [CERT] | `rg band\|threshold chart-{rt,wb}` = 0 + [B251]-[B259] | ✅ measured |
| 10 | Only production path = custom rt-profile module with a headless chart renderer drawing bands+markers | [INFER] | §361.6 from claims 1-9 | ✅ reasoned |

**Marker tally**: [CERT] ×7 · [CERT]/[INFER] ×2 · [INFER] ×1. Ratio ≈ 0.2 (counting mixed as CERT). Block type =
**EVIDENCE**: code+module.xml grounded. Dramatic negative (scheduled chart-in-report blocked) re-measured two
ways: (i) the module.xml profile of every chart/PDF/PX class = `wb`; (ii) the rt exporter agent set = CSV/text
only. Load-bearing tokens re-resolved against disk this iteration: claims 1-3,5-7,9 (driver-verified, not trusted
from the sweep). Delegated sweep tier: `sonnet` (structural), driver-verified.

## Connections

- [B357] §357.2/§357.6 — the report worker thread + the CSV/text runtime exporters this block bounds.
- [B359]/[B360] — the history-table and alarm-marker legs; R6 completes the "all three legs need custom code" set.
- [B199] (webChart) / [B251]–[B259] (classic chart) — the two chart engines, both `wb`/browser, neither
  station-side-schedulable into a report; and the confirmed absence of a band/threshold API.
- [B194] — PX media and the Wb/Hx/ux profile model this block's boundary rests on.
- Forward: R9 (synthesis) — assemble report(schedule/deliver) + custom history cursor + custom alarm cursor +
  custom rt chart renderer into the end-to-end client composition and its cost shape. R3 (export/xlsx), R7 (ux),
  R8 (wb builder) still open.

## Gaps opened / queued

No new gaps. R6 closed (chart-in-report = NO for the scheduled use case; only a manual Workbench PDF, bands always
custom). Remaining investigable: R3 (export/xlsx), R7 (ux/web), R8 (wb builder), R9 (synthesis) → 4 open.
