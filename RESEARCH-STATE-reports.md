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
covered_blocks: 356
gaps_closed: 4
known_gaps: 9
investigable_open: 5
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: reports
status: paused
bootstrapped_on: 2026-08-05
paused_on: 2026-08-05 (session end after R1/R2; NEXT = R4 history-in-report, the load-bearing client-feasibility gap; R3 export/xlsx also pending)
resumed_on: 2026-08-05 (R4+R5 closed → B359/B360; both data legs need custom cursor code, stock BBqlGrid blocked; NEXT = R6 chart-in-report, then R3/R7/R8/R9)
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
| R2 | Time-range scoping — is there a built-in range param, or is the range carried inside the BQL? What does a user-chosen dynamic range require? | B358 | closed |
| R3 | Table export pipeline (grid → CSV/text) + Excel(xlsx) gap + file/email delivery | — | open |
| R4 | History in a report — does a `history:...|bql:` query resolve through BBqlGrid's BatchResolve (records vs navigable ORDs)? | B359 | closed (NO — structural) |
| R5 | Alarm records in a report — BQL over the alarm db, available AlarmRecord columns, alarm-specific source/grid | B360 | closed (CONDITIONAL — YES w/ custom cursor) |
| R6 | Chart-in-report — no chart renderer in-module; PDF is wb-only; how the PSI-vs-time chart with bands composes externally | — | open |
| R7 | ux/web layer — BUxReportPane, BHTML5BqlGridTable, pagination/sort, browser view | — | open |
| R8 | wb builder — BComponentGridEditor, BReportPxMedia, the engineering workflow | — | open |
| R9 | SYNTHESIS — the client-need composition: report(table) + chart(plot+bands) + alarm(markers) + range glue | — | open |

## Backlog (investigable)

| Priority | Gap | Notes | Status |
|---|---|---|---|
| high | R2 time-range scoping | CLOSED B358: no range property in module; range is a `?period=`/BQL ORD substring (12 BWebChartTimeRangeType values, remittance B45); relative preset = zero-code self-updating, arbitrary user range = custom BReportSource or the interactive webChart path | closed |
| high | R3 table export + xlsx gap | grid→BGridToCsv/BGridToText→BFileRecipient; confirm no xlsx exporter, PDF wb-only | pending |
| high | R4 history-in-report feasibility | CLOSED B359: NO (structural). BBqlGrid force-prepends `select ordInSession,` + slurps + BatchResolves col-0 to component targets — it is a COMPONENT viewer. BHistoryRecord extends BStruct (no ordInSession) → null col-0 → NPE → resolveError. 0 history grids in module. Feasible only via custom BExportSource cursoring BIHistory directly, or the webChart/history path | closed |
| medium | R5 alarm records in a report | CLOSED B360: CONDITIONAL (YES w/ custom code). Alarm DB is a first-class BQL source (`alarm:\|bql:...from openAlarms`, BAlarmDatabase Queryable, operator-read gate). All fields available: timestamp/source/sourceState/normalTime typed columns; highLimit/lowLimit/presentValue in the alarmData facet bag (getAlarmFacet, not BQL columns). BUT BAlarmRecord extends BStruct → same ordInSession wall as R4 → BBqlGrid NPEs. Feasible via custom cursor over BITable<BAlarmRecord> (BExportSource/AlarmDbTableModel pattern). Report module itself has 0 alarm code | closed |
| medium | R6 chart-in-report gap | Module has no chart renderer (chart-rt dep but 0 chart classes); PDF wb-only; compose externally — remittance B199/B251-B259 | pending |
| medium | R7 ux/web grid table | BUxReportPane + BHTML5BqlGridTable + pagination/sort | pending |
| low | R8 wb builder | BComponentGridEditor template-query workflow + BReportPxMedia | pending |
| low | R9 synthesis | Focus-closing composition answering the client deliverable end-to-end | pending |

## Iteration history

| Block | Gap | Delegated? · model tier | Notes |
|---|---|---|---|
| B357 | R1 | yes · sonnet (audit sweep) + inline verify | Foundation. Sweep mapped the 49-class module + cross-referenced adjacent corpus; driver re-verified every load-bearing citation against decompiled source (BReportService/BReportSource/BReport/BExportSource/BBqlGrid/BFileRecipient/BGridToCsv + module.xml). Spine: schedule(BTimeTrigger)→generate(async action)→single-thread serialized queue→handleGenerate→BReport(name/mime/bytes)→out topic→recipient.route. Grid=BQL, eager Tables.slurp, bans `select *`, no time-range knob. [CERT]-heavy evidence block. |
| B358 | R2 | no · inline (constraint: narrow scoping gap, 2 grid classes + corpus remittance) | Range lives in the ORD/BQL, not the module. Two grids: BBqlGrid (query→table, time-series vehicle) vs BComponentGrid (sources×cols cross-product, 3s-lease live snapshot, NOT time-series). Range = `?period=` on the history ORD (12 BWebChartTimeRangeType values, server computes start/end when relative — remittance B45/B73). Relative preset (`today`/`monthToDate`) = zero-code self-updating each scheduled run; arbitrary user-chosen range = custom BReportSource subclass (static `query` BOrd can't take a runtime arg) OR the interactive webChart/history-query path. niagara-help 3 real zeros. [CERT]×3 own + ×2 remittance, [INFER]×4, ratio 0.8 (EVIDENCE+APPLIED). |
| B359 | R4 | yes · sonnet (structural sweep, 51 tool-uses) + inline token-verify (10 citations re-resolved) | R4 = NO (structural). BBqlGrid is a COMPONENT viewer: bans `select *`, force-prepends `select ordInSession,`, eager-slurps, extracts col-0 as ORD, BatchResolves to component targets, renders each cell against `targets[row]`. BHistoryRecord extends BStruct → no `ordInSession` → BQL field null → NPE → `Report.gridTable.bql.resolveError`. BatchResolve fast-path only `slot:/virtual:/h:`. 0 history grids in module (re-measured). Dramatic negative re-measured 2 independent ways. Feasible path = custom BExportSource cursoring BIHistory directly, or webChart/history. [CERT]×9, [INFER]×2, ratio 0.50 (EVIDENCE). |
| B360 | R5 | yes · sonnet (structural sweep, 57 tool-uses) + inline token-verify (proven-absence + columns re-resolved) | R5 = CONDITIONAL (YES w/ custom cursor). Report module has 0 alarm code (proven absent). Alarm DB IS a first-class BQL source: BAlarmDatabase implements Queryable, ordInSession `alarm:`, bqlQuery→BAlarmDbQueryResult, relations openAlarms/ackPendingAlarms, ORD form `alarm:\|bql:...`; throws if run on engine thread (report runs off-engine, OK); gate hasOperatorRead (not super-user). All client fields present: timestamp/source(BOrdList)/sourceState/normalTime typed columns; highLimit/lowLimit/presentValue in alarmData facet bag via getAlarmFacet (not BQL cols). BUT BAlarmRecord extends BStruct → same ordInSession NPE wall as R4. Feasible via custom cursor (BExportSource / AlarmDbTableModel pattern, the latter is wb-side). [CERT]×7, [INFER]×1 + 2 mixed, ratio 0.50 (EVIDENCE). |

## Dismissed file types

None yet — census pending at focus close (report module is small and homogeneous: decompiled Java + module.xml
resources + ux JS/Handlebars). To be enumerated in the R9 synthesis.
