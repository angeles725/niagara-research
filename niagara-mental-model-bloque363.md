# Niagara N4 — Bloque 363: the `report` module (VII) — the table export is CSV/text only, every cell stringified to `BString`, with a UTF-8 BOM for Excel; native `.xlsx` is PROVEN ABSENT (custom if required)

> **Focus**: `reports`, gap R3 (table export pipeline + xlsx gap + delivery). Detail gap closing the export leg:
> exactly how the grid becomes bytes, and whether "Excel-like table" means native `.xlsx` or CSV. Follows
> [B357] (the spine + exporters) and [B361] (PDF is wb-only).
>
> Subject version: Niagara N4.14 `report-rt` (decompiled corpus, release 2024-05-28).
>
> **Sources** (all `[CERT]`, read directly this iteration):
> - `report-rt/decompiled/com/tridium/report/exporters/BGridToCsv.java`
> - `report-rt/decompiled/com/tridium/report/exporters/BGridToText.java`
> - measured absence: `rg -ril 'xlsx|excel|poi|workbook' report -g '*.java'` = 0
> - Remittance `[CERT]`: [B357] §357.6 (exporters + BReport bytes), [B361] §361.2-3 (PDF/chart wb-only)
>
> **Method**: inline driver read (narrow — 2 exporter classes + one measured absence). Block type: **EVIDENCE**.

---

## §363.1 — Both exporters build the SAME table; every cell is stringified to `BString` `[CERT]`

The station ships exactly two runtime exporters, and they share one table-builder:

- `BGridToText.makeTable(GridModel, Context)` walks the grid model — `colCount = model.getColumnCount()`,
  `rowCount = model.getRowCount()` — and builds a `BDataTable`, adding **every column as `BString.TYPE`**:
  `t.addColumn(name, displayName, BString.TYPE, 0, BFacets.NULL)` `[CERT]` `BGridToText.java:86-93`.
- `BGridToCsv.export()` calls `BGridToText.makeTable(model, op)` then serializes it `[CERT]`
  `BGridToCsv.java:82-83`.

**Consequence**: the export has **no typed columns** — PSI values, timestamps, everything is coerced to a string
before serialization. There is no numeric/date type carried into the output; a downstream consumer (Excel) re-parses
the strings. This is inherent to the CSV/text nature of the pipeline and is a second reason the output is not a
typed spreadsheet. `[CERT]`/`[INFER]`.

## §363.2 — CSV writes a UTF-8 BOM (the "opens cleanly in Excel" mechanism); text delegates to a generic writer `[CERT]`

- `BGridToCsv.export()` wraps the output in a UTF-8 writer — `new OutputStreamWriter(op.getOutputStream(),
  "UTF-8")` `[CERT]` `BGridToCsv.java:66` — and writes a **byte-order mark** first: `out.write(65279)` (0xFEFF)
  then `flush()` `[CERT]` `BGridToCsv.java:69-70`. The BOM is precisely what makes Excel open the CSV in the
  correct (UTF-8) encoding instead of mangling non-ASCII. So "opens in Excel" is a real, deliberate feature — but
  it is a **CSV**, not a native workbook. `[CERT]`/`[INFER]`.
- `BGridToCsv` also carries an `encodeToString : boolean` property (default false) `[CERT]`
  `BGridToCsv.java:55` — an alternate mode that yields the export as a string (e.g. for inline/email embedding)
  rather than a raw byte stream.
- `BGridToText.export()` builds the same `makeTable` result and hands it to a generic `BITableToText().export(sub)`
  `[CERT]` `BGridToText.java:81-83` — i.e. the CSV/text split is just a delimiter/writer choice over one shared
  table model.

## §363.3 — Native `.xlsx` is PROVEN ABSENT `[CERT]`

`rg -ril 'xlsx|excel|poi|spreadsheet|workbook' report -g '*.java'` (excluding duplicate decompiler pipelines) =
**0 files** `[CERT]` (driver-measured). There is no Apache-POI dependency, no `.xlsx` writer, no workbook class
anywhere in the `report` module — rt, ux, or wb. The client phrase "Excel-like table" is satisfiable **only** as
**CSV** (which Excel opens) out of the box. A **true `.xlsx`** (multiple sheets, typed cells, formatting) is
**custom development** — a POI-class library compiled into a module and a new `BExporter`. `[CERT]`/[INFER].

## §363.4 — The runtime delivery-format set: CSV + text; PDF is Workbench-only `[CERT]`

Combining this block with [B361]: on the station (rt profile), a scheduled report can emit **only** the bytes
these two exporters produce — **CSV or plain text**. `BGridToPdf` is `report-wb` (wb profile), not loaded on the
station ([B361] §361.2-3), so **PDF is not a runtime delivery format** for a pushed report; it is a manual
Workbench export. The `BReport` struct then carries `name/fileName/mime/bytes` to a file or email recipient
([B357] §357.3/§357.6 — remittance). `[CERT]`.

## §363.5 — R3 verdict `[INFER]`

**R3 = the table export is CSV/text only, all cells stringified, UTF-8 BOM for Excel; no native xlsx (custom if
required); PDF is Workbench-only.** For the client's "Excel-like table": a **BOM-prefixed CSV** is the zero-code
answer and opens correctly in Excel; a **genuine `.xlsx`** artifact is a custom exporter (+POI-class lib). This
adds the xlsx line-item to the composition cost ([B362] §362.5) but does not change the architecture: the export
leg, like the others, is stock only in its simplest (CSV) form. `[INFER]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Both exporters build one shared `BDataTable` via `makeTable`; every column added as `BString.TYPE` | [CERT] | `BGridToText.java:86-93`; `BGridToCsv.java:82-83` | ✅ read |
| 2 | `BGridToCsv` writes UTF-8 (`OutputStreamWriter … "UTF-8"`) with a leading BOM `out.write(65279)` | [CERT] | `BGridToCsv.java:66,69-70` | ✅ read |
| 3 | `encodeToString` property offers a string-encoded output mode (default false) | [CERT] | `BGridToCsv.java:55` | ✅ read |
| 4 | `BGridToText` delegates to a generic `BITableToText` writer over the same table | [CERT] | `BGridToText.java:81-83` | ✅ read |
| 5 | No xlsx/excel/poi/workbook anywhere in the report module (proven absent) | [CERT] | `rg -ril xlsx\|excel\|poi\|workbook report` = 0 | ✅ measured |
| 6 | Runtime delivery formats = CSV + text; PDF is wb-only (not station-schedulable) | [CERT] | §363.4 + [B361] §361.2-3 | ✅ read (remittance) |
| 7 | "Excel-like table" = BOM CSV zero-code; native .xlsx = custom exporter | [INFER] | §363.5 from claims 2,5 | ✅ reasoned |

**Marker tally**: [CERT] ×5 · [CERT]/[INFER] ×1 · [INFER] ×1. Ratio ≈ 0.14. Block type = **EVIDENCE** (low ratio,
code+measure grounded). Load-bearing tokens re-resolved against disk: claims 1-5 (all read/measured inline this
iteration). Not delegated: inline (constraint: narrow — 2 exporter classes + one measured absence).

## Connections

- [B357] §357.6 — the exporter agents and the `BReport` bytes/mime/recipient delivery this block details.
- [B361] §361.2-3 — the rt/wb profile boundary that makes PDF Workbench-only (so runtime = CSV/text).
- [B362] §362.5 — the cost shape; this block fixes the xlsx line-item (custom) and the CSV zero-code baseline.

## Gaps opened / queued

No new gaps. R3 closed. Remaining investigable: R7 (ux/web layer), R8 (wb builder) → 2 open. Focus 7/9.
