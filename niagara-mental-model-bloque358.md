# Niagara N4 — Bloque 358: the `report` module (II) — where the time range actually lives (`?period=` in the BQL ORD), and what a user-chosen range costs

> **Focus**: `reports`, gap R2 (time-range scoping). Follows [B357] (the spine). Question: a report has no range
> property — so how does a report get scoped to a date/time/month range, and what does the client's
> **user-chosen dynamic range** require?
>
> **Sources**:
> - `report-rt/decompiled/com/tridium/report/grid/BComponentGrid.java`, `BSingleQueryRow.java` `[CERT]`
> - `report-rt/decompiled/com/tridium/report/grid/BBqlGrid.java` (re-cited from [B357]) `[CERT]`
> - Remittance `[CERT]`: [B45] (history ORD `?period=` + `BWebChartTimeRangeType`), [B73] (relative→absolute
>   range computation), [B357] (the spine)
> - `niagara-help` `guide-search`: "bql relative time", "history query time range", "report scheduling export
>   source" → **0 hits each** (real zeros, recorded so a later pass does not retry)
>
> **Method**: inline read of the two component-grid classes + the BQL grid, plus remittance to the corpus's
> existing history-range coverage. Block type: EVIDENCE + APPLIED (the dynamic-range requirement is derived).

---

## §358.1 — Restated and confirmed: the report module owns no range knob

[B357] §357.3/§357.5 established that `BReportSource` carries only `schedule` (a `BTimeTrigger` — WHEN to run),
and that `BBqlGrid` scopes data only through its `query` ORD. This block confirms the negative across **both**
grid types: the only data-shaping properties in the entire `com.tridium.report.grid` package are `BOrd`s:
- `BBqlGrid.query : BOrd` [CERT] `BBqlGrid.java:69,72`.
- `BComponentGrid.template : BOrd` + `BComponentGrid.query : BOrd` [CERT] `BComponentGrid.java:68,72-73`.

There is **no `BTimeRange`, `BRelTime`, `period`, `startTime`/`endTime`, or `BAbsTime` range property** anywhere
in the module (the only `BAbsTime` use is `BGridToCsv` stamping `now()` into the CSV header — [B357] §357.6). The
range is therefore not a report concept at all; it is **carried inside the ORD/BQL string** the grid resolves.

## §358.2 — Two grids = two report shapes (only one is time-series)

The client's need is a *time-series* table (PSI vs time over a range). Only one of the two grids serves it:

**`BBqlGrid` — one query, one table (the time-series vehicle).** It runs a single BQL query and turns each result
row into a report row [CERT] `BBqlGrid.java:89-142` ([B357] §357.5). A history/alarm query returns
timestamped records → exactly the "rows over a time range" shape the client wants.

**`BComponentGrid` — a live snapshot of N components (NOT time-series).** Its `resolve()` gathers the ORDs of a
set of rows and columns, builds the **cross-product** `sources × columns` of relative ORDs, `BatchResolve`s them,
and **leases the resolved components for 3 s** [CERT] `BComponentGrid.java:104-151` (`BComponent.lease(comps, 1,
3000L)` at line 150). The rows can be populated by a template BQL query whose results become `BSingleQueryRow`s —
a display row labeled from the `report` lexicon `compGridEditor.queryResultRow` [CERT] `BSingleQueryRow.java:24-35`.
The output is a grid of **current values** of many points (e.g. "every PSI sensor and its live reading/status"),
NOT a history over time. It has no timestamp axis and leases live values — a *state* report, not a *trend* report.

**Consequence for the client**: the range-scoped PSI-over-time extract is a `BBqlGrid` over a **history** query.
`BComponentGrid` is the wrong tool for it (it would give one snapshot column per point, not a time series). This
sharpens R4 (history-in-report feasibility): the whole deliverable rides on a `history:…|bql:…` query resolving
through `BBqlGrid`.

## §358.3 — Where the range lives: the `?period=` param on the history ORD

The corpus already reverse-engineered how a Niagara history ORD expresses a time window ([B45], `[CERT]`
decompiled — remittance here, not re-derived):

- A history ORD accepts a `?period=` query parameter, e.g. `history:$/Floor1_Temp?period=today`
  [CERT via B45 §, B45:263].
- `period=` takes one of **12 `BWebChartTimeRangeType` values**: `auto, timeRange, today, last24Hours, yesterday,
  weekToDate, lastWeek, last7Days, monthToDate, lastMonth, yearToDate, lastYear` [CERT via B45, B45:267,971].
- When `period != timeRange` (ordinal > 1), **the server computes the absolute start/end internally**, per the
  history's own timezone [CERT via B45, B45:130]. [B73] documents the same relative→absolute computation
  (`RangeCalculator`, 15 ranges lastHour..last12Months; native `BWebChartTimeRangeType.toAbsolute()`).
- BQL composes over that ORD: `bql:select timestamp, value from history:$/Floor1_Temp` [CERT via B45, B45:285].

So a report `query` ORD like `history:$/Plant/PSI_Sensor?period=monthToDate|bql:select timestamp,value` is the
mechanism: the `?period=` clause scopes the window, the `bql:` clause projects the columns for the grid. **The
range is a substring of the report's static `query` property.** [INFER] (composition of the two cited facts.)

## §358.4 — Relative range = self-updating; absolute range = frozen. Only relative fits a scheduled report.

The 12 `period=` values split into two kinds, and the distinction is the whole answer to "is the range dynamic?":

- **Relative periods** (`today`, `monthToDate`, `last7Days`, …): re-resolve to fresh absolute bounds **every time
  the report generates**, because the server recomputes start/end at resolve time ([B45:130]). A report scheduled
  daily with `?period=today` produces *today's* data each run, with **no human intervention** — the range is
  dynamic in the temporal sense, but chosen from a **fixed preset menu** baked into the ORD. [INFER]
- **Absolute / `timeRange`**: needs explicit start/end timestamps in the ORD. Frozen — the same window forever
  unless the ORD is edited. [INFER from B45's `period=timeRange` requiring client-supplied bounds, B45:130,273].

For a **scheduled** report (the report module's native mode — [B357] §357.3), a **relative preset period is the
natural and only zero-code fit**: pick `today`/`thisMonth`/`last7Days` in the `query` ORD and let each scheduled
run cover the rolling window. This already satisfies a large class of "monthly/weekly report" needs. [INFER]

## §358.5 — The client's "user picks any range in a UI" need is NOT zero-code

The client wants the **user to choose an arbitrary range at request time** (a date-picker: from A to B, any month,
any custom span) and get the extract for exactly that span. That collides with the report module's design:

1. The report `query` is a **static `BOrd` property** on a persisted component. It cannot take a runtime argument —
   there is no "generate(range)" action; `generate` takes no parameter ([B357] §357.3, `BReportSource.java:51`
   action has no arg slot). [CERT]/[INFER]
2. An **arbitrary** user range needs `period=timeRange` with explicit start/end — which means the ORD string must
   be rebuilt with the user's chosen bounds **before** the grid resolves. [INFER via B45]

Therefore a user-chosen arbitrary range requires **custom glue**, one of:
- **(a) Custom `BReportSource` subclass** whose `handleGenerate()` reads the user's chosen start/end (from a slot,
  a request param, or a linked input), builds `history:…?period=timeRange;start=…;end=…|bql:…`, resolves a
  `BBqlGrid`/exporter, and wraps the bytes — the sanctioned extension point ([B357] §357.3, `handleGenerate()` is
  abstract and overridable). [INFER]
- **(b) A driving UX/servlet layer** that sets the grid's `query` ORD (with the chosen bounds) then invokes
  `generate` — feasible but it mutates a shared persisted property, so it serializes poorly for concurrent users
  (the single report thread — [B357] §357.2). [INFER]
- **(c) Not the report module at all**: for an *interactive* "pick a range, see a table + chart now" experience,
  the **webChart / history web query** path ([B45] `WebChartQueryServlet`, NDJSON, `?period=`) already takes a
  runtime range and is built for on-demand UI. The report module is for **scheduled, pushed CSV/email artifacts**,
  not interactive range-picking. [INFER]

**R2 bottom line**: the range is a BQL/ORD substring, not a report feature. A **rolling preset** window
(`today`/`thisMonth`/…) is zero-code and is the report module's sweet spot. An **arbitrary user-chosen** window is
**custom development** — either a custom `BReportSource` (for a scheduled/pushed artifact) or, more naturally, the
interactive webChart/history-query path (for a live UI). This is the first concrete piece of the "three-subsystem
composition + glue" thesis from [B357] §357.8.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Only data-shaping grid props are BOrds (`query`, `template`) — no range/time property in the module | [CERT] | `BBqlGrid.java:69,72`; `BComponentGrid.java:68,72-73` | ✅ read |
| 2 | `BComponentGrid.resolve` builds a sources×columns cross-product, BatchResolves, leases 3 s (live snapshot) | [CERT] | `BComponentGrid.java:104-151` | ✅ read |
| 3 | `BSingleQueryRow` is a template-query result row labeled from the report lexicon | [CERT] | `BSingleQueryRow.java:24-35` | ✅ read |
| 4 | `BBqlGrid` (one query → table) is the time-series vehicle; `BComponentGrid` is not | [INFER] | §358.2 from claim 2 + [B357] §357.5 | ✅ reasoned |
| 5 | History ORD accepts `?period=` with 12 `BWebChartTimeRangeType` values; server computes start/end when relative | [CERT] (remittance) | [B45:130,263,267,971] | ✅ remittance |
| 6 | BQL composes over the history ORD (`bql:select timestamp,value from history:…`) | [CERT] (remittance) | [B45:285] | ✅ remittance |
| 7 | A relative `period=` self-updates each scheduled run; `timeRange` is frozen absolute | [INFER] | §358.4 from claim 5 + [B45:130,273] | ✅ reasoned |
| 8 | `generate` action takes no argument → static `query` can't take a runtime range | [CERT]/[INFER] | [B357] `BReportSource.java:51` | ✅ read (B357) |
| 9 | Arbitrary user-chosen range ⇒ custom `BReportSource` subclass or the interactive webChart path | [INFER] | §358.5 from claims 1,8 + [B45] | ✅ reasoned |

**Marker tally**: [CERT] ×3 (own reads) + [CERT] ×2 (remittance to B45) · [INFER] ×4. Ratio [INFER]/[CERT] ≈ 0.8.
Block type = EVIDENCE+APPLIED: the high ratio is expected (the dynamic-range *requirement* is a derivation over
cited facts, not a new decompilation) and does NOT signal focus exhaustion. `niagara-help` = 3 real zeros
(recorded). Load-bearing tokens checked: claims 1-3,8 read directly; claims 5-6 verified as already-`[CERT]` in
B45 (not re-derived — remittance).

## Connections

- [B357] §357.3/§357.5/§357.8 — the spine and the "no range knob" negative this block resolves.
- [B45] — the history ORD `?period=` mechanism + `BWebChartTimeRangeType` (the CHART/web-query layer where the
  range vocabulary actually lives); also the interactive alternative (c) for range-picking UIs.
- [B73] — relative→absolute range computation (`RangeCalculator`, 15 ranges) and the range-vocabulary divergence.
- Forward: R4 (history-in-report feasibility) is now the load-bearing next gap — the whole deliverable rides on a
  `history:…?period=…|bql:…` resolving through `BBqlGrid`.

## Gaps opened / queued

No new gaps. R2 closed. Remaining investigable: R3 (export/xlsx), R4 (history-in-report — now highest-value),
R5 (alarm records), R6 (chart-in-report), R7 (ux/web), R8 (wb builder), R9 (synthesis) → 7 open.
