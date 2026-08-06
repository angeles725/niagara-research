# Niagara N4 — Bloque 362: the `report` module (VI, SYNTHESIS) — the client deliverable is a 4-piece composition where the stock module contributes only the schedule+delivery wrapper; the three data legs are all custom

> **Focus**: `reports`, gap R9 (synthesis — the client-need composition end-to-end). This block consolidates
> [B357]–[B361] into a single answer to the driving question: *can Niagara N4's `report` module deliver the
> client's range-scoped PSI report with an Excel-like table and an analytics chart that marks alarm-limit
> crossings — and if so, what is actually custom?* It also frames the cost shape that follows from the split.
>
> Subject version: Niagara N4.14 `report-{rt,ux,wb}` (decompiled corpus, release 2024-05-28).
>
> **Sources**: this is a SYNTHESIS block — its claims are **remittance** to already-`[CERT]` blocks, not new
> decompilation:
> - [B357] the spine (schedule→generate→BReport→recipient), CSV/text exporters, single serialized thread
> - [B358] the range lives in the `?period=`/BQL ORD; relative preset vs arbitrary range
> - [B359] history table blocked (`BBqlGrid` is a component viewer; `BHistoryRecord` is a `BStruct`)
> - [B360] alarm records queryable but same `ordInSession` wall; all crossing fields present
> - [B361] chart blocked at the rt/wb profile boundary; bands/markers custom in every path
> - Remittance to the wider corpus: [B45]/[B73] (webChart/history range), [B199]/[B251]–[B259] (chart engines)
>
> **Method**: inline synthesis over the verified focus blocks (no new sweep — the evidence is already cited and
> driver-verified in B357–B361). Block type: **SYNTHESIS/DESIGN** — a high `[INFER]` ratio is expected and
> healthy (this block reasons over cited facts; it does not re-derive them) and does NOT signal exhaustion.

---

## §362.1 — The one-sentence answer

**Yes, the deliverable is buildable — but almost none of it is the `report` module.** The stock module
contributes only a **schedule + file/email delivery wrapper** around a **BQL grid that can render components,
not records**. The client's three data legs — the range-scoped history table, the alarm-crossing markers, and
the banded PSI chart — are each blocked in stock and each require **custom rt-profile code**. The report module
is the *envelope*; the *contents* are custom. `[INFER]` (synthesis of B357–B361).

## §362.2 — What the stock module gives vs. what is custom

| Deliverable piece | Stock report module? | Verdict | Evidence |
|---|---|---|---|
| **Schedule** (run daily/weekly/monthly) | ✅ `BReportSource.schedule : BTimeTrigger` | **reusable as-is** | [B357] §357.3 |
| **Delivery** (write file / send email) | ✅ `BFileRecipient` / email recipient | **reusable as-is** | [B357] §357.3 |
| **Range scoping** | ⚠ only via `?period=` in the ORD | **reusable for relative presets; custom for an arbitrary user-picked range** | [B358] §358.4-5 |
| **Table of history samples over the range** | ❌ `BBqlGrid` renders components, not `BHistoryRecord` structs → NPE | **CUSTOM** (cursor `BIHistory` directly) | [B359] §359.5 |
| **Alarm-crossing rows + timestamps** | ❌ alarm DB is BQL-queryable but `BAlarmRecord` hits the same struct wall; report module has 0 alarm code | **CUSTOM** (cursor `BITable<BAlarmRecord>`) | [B360] §360.6 |
| **Chart (PSI-vs-time), scheduled/pushed** | ❌ blocked at the rt/wb profile boundary; station emits only CSV/text | **CUSTOM** (rt-profile headless chart renderer) | [B361] §361.6 |
| **Alarm bands `<12/>28` + crossing markers** | ❌ no band/threshold API in stock chart | **CUSTOM** (`doPaint` overlay) — regardless of path | [B361] §361.5 |
| **Excel `.xlsx` output** | ❌ only CSV/text runtime exporters (`.xlsx` unconfirmed but absent in rt) | **CUSTOM if true xlsx required** (R3 — pending) | [B357] §357.6 |
| **CSV output** | ✅ `BGridToCsv` | **reusable** (opens in Excel; not native xlsx) | [B357] §357.6 |

The pattern is uniform: **scheduling and delivery are free; every piece that touches the client's actual data or
its presentation is custom.** `[INFER]`.

## §362.3 — The recurring root cause: `BBqlGrid` is a component viewer, and the profile boundary

Two structural facts, established once and reused across all three legs, explain why so much is custom:

1. **`BBqlGrid` resolves components, not rows.** It force-prepends `select ordInSession,`, slurps the table, and
   `BatchResolve`s column 0 into navigable `BComponent`s — each report row is a *component* and each column a
   *slot* of it ([B359] §359.1). History samples and alarm records are `BStruct`s with no `ordInSession`, so
   both NPE ([B359] §359.2, [B360] §360.5). Any report over **records** (the client's whole need) must bypass
   `BBqlGrid` with a custom cursor. `[CERT]`-backed (remittance).
2. **The rt/wb profile split is a hard wall.** All chart/PDF/PX classes are `runtimeProfile="wb"` — absent from
   the station process that runs a scheduled report, which can only emit CSV/text ([B361] §361.2-3). An
   unattended charted PDF cannot exist without a custom `rt`-profile renderer. `[CERT]`-backed (remittance).

## §362.4 — The reference architecture (what to actually build)

For the client's **scheduled, unattended, pushed** report (the hard case), the composition is:

```
 [ stock BReportService ]  ──schedule (BTimeTrigger)──▶  [ CUSTOM BReportSource subclass ]
        (reusable)                                              handleGenerate():
                                                          1. resolve the range → ?period= (relative preset,
                                                             zero-code) OR rebuild ORD with user start/end (custom)
                                                          2. CURSOR BIHistory directly  ──▶  PSI-vs-time rows      [custom, B359]
                                                          3. QUERY alarm DB (alarm:|bql:from openAlarms)
                                                             ──▶ crossing rows + timestamps + high/lowLimit facet [custom, B360]
                                                          4. RENDER chart headless (JFreeChart/iText, rt-profile):
                                                             draw <12/>28 bands + mark crossings                   [custom, B361]
                                                          5. assemble bytes: CSV/xlsx table + chart image/PDF
                                                                    │
                                                                    ▼
                                                          [ BReport (name/mime/bytes) ]
                                                                    │  out topic
                                                                    ▼
                                              [ stock BFileRecipient / email recipient ]  (reusable)
```

Only the two end-caps (schedule, deliver) are stock; the middle five steps are the custom module. `[INFER]`.

**The interactive alternative** (if the client will accept "open a page and pick a range" instead of a pushed
file): the webChart/history-query path ([B45], [B358] §358.5c) already cursors histories with a runtime
`?period=` and renders a chart in the browser — no custom history cursor, no custom rt renderer. But it still
needs a **custom banded-chart widget** for the `<12/>28` overlay + crossing markers ([B361] §361.5), it does
**not** push a scheduled file/email, and it does not produce the Excel table. It is cheaper but answers a
*different* (on-demand) need. `[INFER]`.

## §362.5 — Cost shape (derived from the split, not a quote)

The effort concentrates exactly where the "custom" cells cluster in §362.2. Rough engineering shape (not a bid):

| Piece | Relative effort | Why |
|---|---|---|
| Schedule + delivery wiring | **low** (hours) | stock components, configuration ([B357]) |
| Range glue | **low** if relative preset; **medium** if arbitrary user range | preset is ORD config; arbitrary needs a custom source or a driving UI ([B358] §358.5) |
| History-table custom cursor | **medium** | bypass `BBqlGrid`, cursor `BIHistory`, page large ranges to avoid the eager-slurp OOM ([B359] §359.4) |
| Alarm-crossing custom query | **low–medium** | alarm DB is already BQL-queryable; pull typed cols + `alarmData` facets ([B360]) |
| **Banded PSI chart (rt headless renderer + bands + markers)** | **HIGH** — the dominant line item | no stock band/threshold API; a headless rt renderer drawing fills + crossing markers is net-new ([B361] §361.5-6) |
| xlsx (if required over CSV) | **medium** | no native xlsx in rt ([B357] §357.6); a POI-class lib compiled into the module |
| Test / commission / docs | **medium** | station deployment, license, range edge-cases |

The single most expensive item is the **banded chart renderer**, precisely because it is blocked twice — by the
profile boundary AND by the absence of a band API. Any conversation about reducing scope should start there
(e.g. accept the interactive webChart path, or accept plain lines without filled bands). `[INFER]`.

## §362.6 — Bottom line for the focus

The central thesis under test since [B357] §357.8 — *"the client deliverable COMPOSES three subsystems
(report + chart + alarm) plus range glue, it is not a report feature"* — is **confirmed and sharpened**: it is not
merely a composition, it is a composition in which the report module contributes **only the outermost wrapper**,
and all three data/presentation legs are custom `rt`-profile development. The module is well-designed for its
actual purpose — *scheduling a component/BQL grid to CSV/email* — and the client's need sits almost entirely
outside that purpose. `[INFER]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Stock module supplies schedule + file/email delivery, reusable as-is | [CERT] (remittance) | [B357] §357.3 | ✅ remittance |
| 2 | Range = `?period=`/BQL ORD; relative preset zero-code, arbitrary range custom | [CERT]/[INFER] (remittance) | [B358] §358.4-5 | ✅ remittance |
| 3 | History table requires a custom cursor (BBqlGrid can't render `BHistoryRecord` structs) | [CERT] (remittance) | [B359] §359.5 | ✅ remittance |
| 4 | Alarm crossings queryable but need a custom cursor; report module has 0 alarm code | [CERT] (remittance) | [B360] §360.1,§360.6 | ✅ remittance |
| 5 | Scheduled chart blocked at rt/wb profile boundary; bands/markers custom in every path | [CERT] (remittance) | [B361] §361.2-6 | ✅ remittance |
| 6 | Uniform pattern: schedule/deliver free, all data/presentation legs custom | [INFER] | §362.2 synthesis | ✅ reasoned |
| 7 | Reference architecture: 2 stock end-caps + 5 custom middle steps | [INFER] | §362.4 from claims 1-5 | ✅ reasoned |
| 8 | The banded chart renderer is the dominant cost (blocked twice) | [INFER] | §362.5 from [B361] §361.5-6 | ✅ reasoned |
| 9 | Interactive webChart path is cheaper but answers a different (on-demand, no-push, no-table) need | [INFER] | §362.4 from [B45]/[B358]§358.5c | ✅ reasoned |
| 10 | Thesis confirmed+sharpened: report contributes only the wrapper | [INFER] | §362.6 from all above | ✅ reasoned |

**Marker tally**: [CERT] (remittance) ×4 · [CERT]/[INFER] ×1 · [INFER] ×5. Ratio high by design. Block type =
**SYNTHESIS/DESIGN**: the high `[INFER]` ratio is EXPECTED and does NOT signal exhaustion — every `[CERT]` here is
already verified in B357–B361; this block only composes them. No new tokens to check (remittance-only). Not
delegated: inline synthesis (constraint: reasoning over already-verified corpus blocks, no new source material).

## Connections

- [B357]–[B361] — the five evidence blocks this synthesis composes (spine, range, history, alarm, chart).
- [B45]/[B73] — the webChart/history-query interactive alternative (§362.4).
- [B199]/[B251]–[B259] — the two chart engines and their band-API absence (§362.5's dominant cost).
- Client-need thesis originates at [B357] §357.8; confirmed here.

## Gaps opened / queued

No new gaps. R9 (client-composition synthesis) closed. The focus's LOAD-BEARING question — feasibility + what is
custom — is now fully answered (R1/R2/R4/R5/R6/R9). Remaining are SECONDARY detail gaps that do not change the
composition answer: **R3** (confirm no native xlsx exporter + PDF wb-only detail), **R7** (ux/web grid table —
`BUxReportPane`/`BHTML5BqlGridTable` pagination/sort), **R8** (wb builder — `BComponentGridEditor` workflow) → 3
open. Focus coverage 6/9.
