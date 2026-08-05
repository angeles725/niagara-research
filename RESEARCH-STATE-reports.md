# RESEARCH-STATE — focus: reports (ACTIVE)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPPED 2026-08-05** at the user's explicit request
> (`/research-sdd reportes generados con rangos, analiticas con graficas y muestreo de alarmas`), driven by a
> real client need: reports over a user-chosen date/time/month RANGE that extract data as an Excel-like table,
> plus an analytics CHART (Y=PSI, X=date, X adapts to the range) that MARKS the points where the signal crossed
> alarm limits (two bands: below 12 / above 28 critical; 15–25 normal), with a timestamp per alarm.
>
> **NOT documented before** — audit-first. Precise search: ZERO CATALOG rows mention the `report` module; it has
> never been a focus. Adjacent coverage exists and is treated as REMITTANCE, not re-investigation: charts
> ([B199] webChart, [B251]–[B259] classic chart, [B224] Reflow chart render), history/CSV ([B73], [B237]),
> alarms ([B8], [B44], [B54], [B142], [B240]), analytics ([B16], [B66], [B67]).
>
> **Declared angle (§b2)**: the core Tridium `report` module (`report-rt` / `report-ux` / `report-wb`,
> namespaces `javax.baja.report.*` public API + `com.tridium.report.*` impl) as a SUBSYSTEM: a scheduled,
> queue-serialized **grid → bytes → file/email** pipeline. Central thesis under test: the module produces
> **tabular exports (CSV/text), not charts, and carries no time-range knob** — the client deliverable therefore
> COMPOSES three subsystems (report for the table, chart for the plot, alarm for the markers) plus custom glue
> that injects the user's chosen range into the BQL query and the chart range.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 353
gaps_closed: 1
known_gaps: 9
investigable_open: 8
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: reports
status: active
bootstrapped_on: 2026-08-05
block_prefix: niagara-mental-model-bloqueN.md (global numbering; next free after B357: B358)

## Pre-flight e2 — existence + MEASURED size

Root: `/home/cristian/modules/Prototipos/modulos/organized/report/`. Distinct classes (decompiled/, excluding
duplicate procyon/vineflower pipelines): **rt 23 · ux 9 · wb 17 = 49**. Public API `javax.baja.report.*` also
present as ORIGINAL Tridium source (with javadoc) under `report-rt/extracted/` and
`docSource/docSource-doc/vineflower/report-rt/javax/baja/report/` — highest fidelity, prefer for the API.
Source CONFIRMED reachable. All backlog gaps investigable from disk; R4 carries a needs-live child for
resolution confirmation.

## Coverage

| Gap | Question | Block | Status |
|---|---|---|---|
| R1 | Architecture/wiring spine: service, source, report DTO, recipients, grid model, exporters | B357 | closed |
| R2 | Time-range scoping — is there a built-in range param, or is the range carried inside the BQL? What does a user-chosen dynamic range require? | — | open |
| R3 | Table export pipeline (grid → CSV/text) + Excel(xlsx) gap + file/email delivery | — | open |
| R4 | History in a report — does a `history:...|bql:` query resolve through BBqlGrid's BatchResolve (records vs navigable ORDs)? | — | open |
| R5 | Alarm records in a report — BQL over the alarm db, available AlarmRecord columns, alarm-specific source/grid | — | open |
| R6 | Chart-in-report — no chart renderer in-module; PDF is wb-only; how the PSI-vs-time chart with bands composes externally | — | open |
| R7 | ux/web layer — BUxReportPane, BHTML5BqlGridTable, pagination/sort, browser view | — | open |
| R8 | wb builder — BComponentGridEditor, BReportPxMedia, the engineering workflow | — | open |
| R9 | SYNTHESIS — the client-need composition: report(table) + chart(plot+bands) + alarm(markers) + range glue | — | open |

## Backlog (investigable)

| Priority | Gap | Notes | Status |
|---|---|---|---|
| high | R2 time-range scoping | No built-in range param seen in B357 spine; range appears BQL-carried — confirm + derive the dynamic-range requirement | pending |
| high | R3 table export + xlsx gap | grid→BGridToCsv/BGridToText→BFileRecipient; confirm no xlsx exporter, PDF wb-only | pending |
| high | R4 history-in-report feasibility | BBqlGrid slurps a table + BatchResolve on ordInSession — do history records produce resolvable ORDs? Needs-live child for final confirmation | pending |
| medium | R5 alarm records in a report | BQL over alarm db through BBqlGrid; map AlarmRecord columns (timestamp, sourcePath, normalTime) | pending |
| medium | R6 chart-in-report gap | Module has no chart renderer (chart-rt dep but 0 chart classes); PDF wb-only; compose externally — remittance B199/B251-B259 | pending |
| medium | R7 ux/web grid table | BUxReportPane + BHTML5BqlGridTable + pagination/sort | pending |
| low | R8 wb builder | BComponentGridEditor template-query workflow + BReportPxMedia | pending |
| low | R9 synthesis | Focus-closing composition answering the client deliverable end-to-end | pending |

## Iteration history

| Block | Gap | Delegated? · model tier | Notes |
|---|---|---|---|
| B357 | R1 | yes · sonnet (audit sweep) + inline verify | Foundation. Sweep mapped the 49-class module + cross-referenced adjacent corpus; driver re-verified every load-bearing citation against decompiled source (BReportService/BReportSource/BReport/BExportSource/BBqlGrid/BFileRecipient/BGridToCsv + module.xml). Spine: schedule(BTimeTrigger)→generate(async action)→single-thread serialized queue→handleGenerate→BReport(name/mime/bytes)→out topic→recipient.route. Grid=BQL, eager Tables.slurp, bans `select *`, no time-range knob. [CERT]-heavy evidence block. |

## Dismissed file types

None yet — census pending at focus close (report module is small and homogeneous: decompiled Java + module.xml
resources + ux JS/Handlebars). To be enumerated in the R9 synthesis.
