# Niagara N4 — Bloque 357: the `report` module (I) — the grid→bytes→file/email pipeline, and the two things it does NOT have

> **Focus**: `reports` (BOOTSTRAP, gap R1). First corpus coverage of the core Tridium **`report`** module —
> zero prior CATALOG rows. Driven by a client need: range-scoped reports + an analytics chart that marks alarm
> crossings. This block establishes the SPINE; later blocks attack ranges (R2), export (R3), history (R4),
> alarms (R5), the chart gap (R6), and the ux/wb layers (R7/R8).
>
> **Sources** (all `[CERT]`, decompiled CFR 0.152 unless noted; module
> `/home/cristian/modules/Prototipos/modulos/organized/report/`):
> - `report-rt/decompiled/javax/baja/report/BReportService.java`
> - `report-rt/decompiled/javax/baja/report/BReportSource.java`, `BReport.java`
> - `report-rt/decompiled/com/tridium/report/BExportSource.java`, `BFileRecipient.java`
> - `report-rt/decompiled/com/tridium/report/grid/BBqlGrid.java`
> - `report-rt/decompiled/com/tridium/report/exporters/BGridToCsv.java`
> - `report-rt/extracted/META-INF/module.xml` (dependency list)
>
> **Method**: audit sweep (sonnet) mapped the 49-class module (rt 23 · ux 9 · wb 17) and cross-referenced
> adjacent corpus; the driver then RE-READ every load-bearing class before writing. Block type: EVIDENCE.

---

## §357.1 — The spine in one sentence

A Niagara report is a **scheduled, queue-serialized transform of some station object into a named blob of bytes,
which is then routed to a file or an email**. Four component families implement it:

```
BReportService  (BIService singleton: one daemon thread, one FIFO queue)
      ▲ enqueue(Runnable)
      │
BReportSource   (abstract: schedule → generate → out)   ──fires──▶  out : BReport topic
      │  handleGenerate() : BReport                                   │
      └─ BExportSource (the only built-in concrete source)            │ (link)
                                                                      ▼
                                                          BReportRecipient.route(BReport)
                                                          ├─ BFileRecipient  (write bytes to a directory)
                                                          └─ BEmailRecipient (attach to SMTP)
```

The payload that flows through is **`BReport`** — a `BStruct` with exactly four fields:
`reportName`, `fileName`, `mimeType`, and `content` as a `byte[]`
[CERT] `BReport.java:21-24`. The report subsystem is therefore **format-agnostic**: it moves opaque bytes with a
MIME label. Whatever decides those bytes (a grid, an exporter) is upstream of this pipeline, not part of it — a
distinction that becomes decisive in §357.7.

## §357.2 — `BReportService`: one thread, one queue, super-user-gated

`BReportService extends BComponent implements BIService, BIRestrictedComponent`
[CERT] `BReportService.java:33-36`. On `serviceStarted()` it creates a `javax.baja.util.Queue` and starts a single
inner `QueueManager extends Thread` named `"ReportService:QueueManager"`
[CERT] `BReportService.java:52-56, 87-91`. The manager loop is `while(true) queue.todo(-1).run();`
[CERT] `BReportService.java:97-99` — `todo(-1)` blocks until a `Runnable` is available, then runs it **inline on
that one thread**. The only public producer is `enqueue(Runnable r)` [CERT] `BReportService.java:72-74`.

Two structural consequences, both `[INFER]` from the above:
- **Report generation is fully serialized.** No matter how many report sources fire at once, they run
  one-at-a-time on a single background thread. A slow report (a large history slurp — §357.5) blocks every other
  queued report behind it. [INFER]
- **The loop is crash-resilient but silent.** The `run()` outer `while` catches `Throwable`, prints the stack
  trace to stderr, and `continue`s the loop [CERT] `BReportService.java:103-106` — a failed report does not kill
  the service, but the only trace is a `printStackTrace()`, not a logged/alarmed event. [INFER]

`BIRestrictedComponent` + `checkParentForRestrictedComponent` calling `checkContextForSuperUser`
[CERT] `BReportService.java:67-70` means the **service itself is a super-user-only component** — installing/moving
the `ReportService` in the station tree requires super-user, consistent with other `BIService` singletons.

## §357.3 — `BReportSource`: schedule → generate → out (asynchronous by construction)

The abstract source declares three slots via annotations
[CERT] `BReportSource.java:49-57`:
- `schedule` : `BTimeTrigger` (a property) — WHEN the report runs.
- `generate` : an `Action` with `flags=16` (async) — the trigger.
- `out` : a `Topic` of `BReport` (`flags=9`) — the result event.

On `started()` it wires the trigger to the action:
`linkTo(getSchedule(), BTimeTrigger.fireTrigger, generate)` [CERT] `BReportSource.java:82-84`. So a
`BTimeTrigger` (daily / interval / manual) firing invokes `generate`.

The asynchrony is explicit in `post()`: when the action is `generate`, it looks up the `BReportService` and
`s.enqueue(new Invocation(this, action, arg, cx))` — returning `null` instead of running inline
[CERT] `BReportSource.java:99-106`. The queued `Invocation` later calls `doGenerate()`, which runs the abstract
`handleGenerate()` and `fireOut(event)` on success, or logs `SEVERE "Generate failed"` and rethrows as a
`BajaRuntimeException` on failure [CERT] `BReportSource.java:86-97`. `handleGenerate() : BReport` is the single
extension point [CERT] `BReportSource.java:86` — a custom report is a `BReportSource` subclass that builds a
`BReport`.

**This is the R2 hook**: the source's only time input is `schedule` (WHEN to run), a `BTimeTrigger`. There is **no
range property** here — nothing that says "cover the last 24 h" or "from date A to date B". Any date-range scoping
must live further downstream, in what `handleGenerate()` queries. (Confirmed for the BQL path in §357.5; R2 will
close the question.)

## §357.4 — `BExportSource`: the only built-in source is an "export anything with an exporter" adapter

`BExportSource extends BReportSource` [CERT] `BExportSource.java:48-49` is the sole concrete source shipped. It
holds one property `source : BExportSourceInfo` (an ORD + an `agentId` + an optional `BExporter`)
[CERT] `BExportSource.java:47-50`. Its `handleGenerate()` [CERT] `BExportSource.java:67-100`:
1. resolves the `source` ORD to an `OrdTarget` and `get()`s the target `BObject` (line 70-75);
2. finds the `BExporter` agent — either the one pinned in `BExportSourceInfo`, or the agent on the object whose
   `agentId` matches, filtered by `AgentFilter.is(BExporter.TYPE)` (line 52, 76-85);
3. builds a filename `rname + "." + exporter.getFileExtension()` and reads `exporter.getFileMimeType()`
   (line 92-93);
4. `exporter.export(ExportOp.make(source, byteArrayOut))` and wraps the bytes:
   `new BReport(rname, fname, mime, out.toByteArray())` (line 98-99).

So `BExportSource` is a **generic adapter**: point it at any station object that has a `BExporter` agent, and it
turns that object into report bytes. The **grid classes (§357.5) are exactly such objects**, and `BGridToCsv`
(§357.6) is exactly such an exporter. (Aside: line 96 logs `"exporting report to PDF"` unconditionally — a
copy-paste string, not evidence of a PDF path; the actual format is whatever `exporter` is. [CERT]
`BExportSource.java:95-96`.)

## §357.5 — The grid model: BQL in, an eagerly-slurped table out — and no time range

The tabular content of a report is a **grid**: `BIGrid` (interface, one method `resolve(base, cx) : GridModel`)
with `BGrid` the base component and two concrete grids — `BBqlGrid` (query-driven) and `BComponentGrid`
(template + explicit rows/columns). The load-bearing one is `BBqlGrid`.

`BBqlGrid` has one property `query : BOrd` edited by `bql:BqlQueryEditor`
[CERT] `BBqlGrid.java:68-72`. Its `resolve()` [CERT] `BBqlGrid.java:89-142`:
- parses the ORD; for each `bql` scheme part, **rejects `select *`** with
  `"You must explicitly specify columns for your query (do not use '*')."`
  [CERT] `BBqlGrid.java:102-104`, and **rewrites the projection to prepend `ordInSession`**:
  `body = "select ordInSession," + body.substring("select ".length())` [CERT] `BBqlGrid.java:105`;
- resolves the query against `base` and **`Tables.slurp(...)` the WHOLE table into a `BIRandomAccessTable`**
  [CERT] `BBqlGrid.java:112` — an EAGER, all-rows-in-memory load;
- keeps every column except the injected `ordInSession` as display columns, and `BatchResolve`s the
  `ordInSession` cell of each row back into an `OrdTarget[]` [CERT] `BBqlGrid.java:113-137`.

Two findings:
- **The eager `Tables.slurp` mirrors the classic-chart defect.** [B251]–[B259] found the classic chart loads its
  whole table up front (`Tables.slurp`) rather than paging; the report grid does the same [CERT]
  `BBqlGrid.java:112`. A report over a large history is a full in-memory materialization on the single report
  thread (§357.2). [INFER] — connect to [B251].
- **Still no time-range knob.** The only time input in the entire spine is the source's `schedule`
  (§357.3). `BBqlGrid` scopes data **only** through the BQL string in its `query` ORD. A date/time/month range is
  therefore something the integrator writes into the BQL (a `where`/time-filter clause) — it is not a first-class
  report parameter. Whether that range can be made DYNAMIC (user-chosen at generation time) is gap R2. [INFER]

## §357.6 — Exporters: CSV and text on the station; PDF only in Workbench; no xlsx

The exporter is a `BExporter` agent `@AgentOn(types={"report:IGrid"})`. On the station (`report-rt`) there are two:
- `BGridToCsv extends BITableToCsv` [CERT] `BGridToCsv.java:50-53`: `export()` resolves the grid to a
  `GridModel`, writes **UTF-8** with an optional **BOM** (`out.write(65279)`) and an optional header block
  (grid name, slot path, `BAbsTime.now()`), then `BGridToText.makeTable(model)` → `export(table)` as CSV
  [CERT] `BGridToCsv.java:62-84`.
- `BGridToText` — the sibling `.txt` exporter; `makeTable(GridModel, cx)` is the shared serialization helper both
  use (referenced from `BGridToCsv.java:82`).

The **PDF exporter `BGridToPdf` lives in `report-wb`, not `report-rt`** (audit finding; wb-profile, depends on
`pdf-wb`). That means a **station-side scheduled `BExportSource` cannot produce a PDF** — PDF is a Workbench-driven
export, not an autonomous one. [INFER] (to be pinned in R3.)

There is **no xlsx / Excel exporter anywhere in the module** — only CSV and text on the station. The client's
"like an Excel" table is served by **CSV** (which Excel opens natively), not a native `.xlsx` writer. Anything
requiring true `.xlsx` needs a custom exporter or an external library. [INFER] — R3 will confirm the absence by
enumeration.

## §357.7 — Recipients: bytes to a directory, or bytes to an email

`BFileRecipient extends BReportRecipient` [CERT] `BFileRecipient.java:54-55` has `ord` (a directory, default
`file:^`, `DirectoryOrdFE`) and `appendTimestamp` (default `false`)
[CERT] `BFileRecipient.java:52-57`. `handleRoute(BReport)` takes `report.getFileName()`, optionally splices a
`"-yyyyMMdd-HHmm"` stamp before the extension, resolves the directory, `makeFile`, and writes
`report.getContent()` bytes to it [CERT] `BFileRecipient.java:82-98`. `changed()` validates the ORD resolves to a
directory, else throws and reverts [CERT] `BFileRecipient.java:100-122`. `BEmailRecipient` (the SMTP sibling)
attaches the same bytes to an outgoing message via the `email` subsystem — already documented at the service level
in the `email` focus ([B324]–[B334]); the report side is just another `route` target.

The recipient consumes **only `BReport`** — name, mime, bytes. It neither knows nor cares whether the bytes are a
CSV, a PDF, or anything else. This confirms §357.1: the pipeline is a byte mover.

## §357.8 — What the module does NOT have (the client-relevant negatives)

`report-rt/extracted/META-INF/module.xml` declares dependencies on `alarm-rt`, `chart-rt`, `history-rt`,
`email-rt`, `bql-rt`, `schedule-rt`, and more [CERT] `module.xml` (dependency block). Yet:

1. **No chart renderer.** The audit sweep found **zero chart-producing classes** across all 49 report classes,
   despite the `chart-rt` dependency. The dependency exists for type resolution, not output — a report emits a
   grid/table, never a plotted chart. The PSI-vs-time chart the client wants (Y=PSI, X=date adapting to the range,
   with alarm-crossing markers) is **not something the report module can draw**; it belongs to `webChart` ([B199])
   or the classic chart ([B251]–[B259]). [INFER] — R6 will nail this.
2. **No time-range parameter.** The whole spine's only time input is the source's `schedule` (WHEN to run);
   data scoping is entirely BQL-string-carried (§357.5). A user-chosen dynamic range needs custom glue. [INFER] —
   R2.
3. **No xlsx and no station-side PDF** (§357.6). CSV/text on the station; PDF only via Workbench. [INFER] — R3.

**Client bottom line (preliminary, to be finalized in the R9 synthesis):** the `report` module alone gives the
client the **tabular Excel-openable extract (CSV)** on a schedule, delivered to a file or email — but the
**range-scoping**, the **chart**, and the **alarm-crossing markers** each live outside it and must be composed:
the range as a BQL/chart-range injection, the chart in the chart subsystem, the alarm timestamps from the alarm
db. The deliverable is a THREE-subsystem composition, not a single report feature.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BReport` is a `BStruct` with fields reportName/fileName/mimeType/content(byte[]) | [CERT] | `BReport.java:18-24` | ✅ read |
| 2 | `BReportService` is a `BIService`+`BIRestrictedComponent` with one daemon thread + one Queue | [CERT] | `BReportService.java:33-56,87-99` | ✅ read |
| 3 | Report generation is serialized on one thread; loop catches Throwable and continues | [CERT]/[INFER] | `BReportService.java:97-106` | ✅ read |
| 4 | Service is super-user-gated (checkContextForSuperUser) | [CERT] | `BReportService.java:67-70` | ✅ read |
| 5 | `BReportSource` = schedule(BTimeTrigger)→generate(async flags=16)→out(BReport) | [CERT] | `BReportSource.java:49-57,82-84` | ✅ read |
| 6 | `generate` is enqueued (async), not run inline; `handleGenerate` is the extension point | [CERT] | `BReportSource.java:86-106` | ✅ read |
| 7 | No range property on the source — only `schedule` (WHEN) | [INFER] | `BReportSource.java:49-57` (absence) | ✅ read |
| 8 | `BExportSource` resolves an ORD, finds a BExporter agent, exports to bytes, wraps in BReport | [CERT] | `BExportSource.java:67-100` | ✅ read |
| 9 | `BBqlGrid` bans `select *`, prepends `ordInSession`, eagerly `Tables.slurp`s the whole table | [CERT] | `BBqlGrid.java:102-112` | ✅ read |
| 10 | Grid scopes data only via the BQL string — no time-range knob | [INFER] | `BBqlGrid.java:89-142` (absence) | ✅ read |
| 11 | `BGridToCsv` `@AgentOn(report:IGrid)`, UTF-8 + optional BOM + header, delegates to makeTable | [CERT] | `BGridToCsv.java:50-84` | ✅ read |
| 12 | `BFileRecipient` writes report bytes to a directory, optional `-yyyyMMdd-HHmm` stamp | [CERT] | `BFileRecipient.java:82-98` | ✅ read |
| 13 | module.xml declares chart-rt/alarm-rt/history-rt deps | [CERT] | `report-rt/.../module.xml` | ✅ read |
| 14 | Zero chart-producing classes in the module (no chart renderer) | [INFER] | audit sweep over 49 classes; module.xml dep only | ⚠ sweep-derived, R6 to confirm by enumeration |

**Marker tally**: [CERT] ×11 · [INFER] ×5 (claims 3, 7, 10, 14 partial + the two structural consequences in
§357.2). [INFER]/[CERT] ratio ≈ 0.45. Block type = EVIDENCE; ratio is moderate, appropriate for a spine/overview
block that leans on structural inference between cited anchors. Load-bearing tokens checked against source: 13 of
14 read directly by the driver; claim 14 (chart-class absence) is the one sweep-derived negative, explicitly
deferred to R6 for enumeration-confirmation per RE-MEASURE-A-DRAMATIC-NEGATIVE.

## Connections

- [B251]–[B259] classic chart — the eager `Tables.slurp` here is the SAME pattern; and the chart the client wants
  is drawn there, not in `report`.
- [B199] webChart — the modern (D3/bajaux) chart path; R6's external composition target.
- [B73] history domain, [B237] Reflow history/CSV export — the data source a range-scoped report queries (R4).
- [B8], [B44], [B54], [B142], [B240] alarms — where the alarm-crossing timestamps come from (R5).
- [B324]–[B334] email subsystem — `BEmailRecipient` is a `route` target into that service.

## Gaps opened / queued

R2 (time-range scoping), R3 (export + xlsx gap), R4 (history-in-report feasibility), R5 (alarm records in a
report), R6 (chart-in-report / external composition), R7 (ux/web layer), R8 (wb builder), R9 (synthesis). 8
investigable gaps queued; R1 closed.
