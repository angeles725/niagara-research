# Niagara N4 — Bloque 359: the `report` module (III) — history-in-a-report is STRUCTURALLY blocked: `BBqlGrid` is a component viewer, and history samples are `BStruct` records with no `ordInSession`

> **Focus**: `reports`, gap R4 (history-in-report feasibility). Follows [B358] (where the range lives) and
> [B357] (the spine). This is the load-bearing feasibility gap: [B358] §358.2 concluded the whole client
> deliverable rides on a `history:…?period=…|bql:…` query resolving through `BBqlGrid` into a table. R4 asks
> whether it actually does.
>
> Subject version: Niagara N4.14 `report-rt` (decompiled corpus, unversioned jar).
>
> **Sources** (all `[CERT]`, read directly this iteration):
> - `report-rt/decompiled/com/tridium/report/grid/BBqlGrid.java` — the query→table→resolve path
> - `baja/baja/decompiled/javax/baja/naming/BatchResolve.java` — the ORD batch-resolver's scheme fast-path
> - `history-rt/decompiled/javax/baja/history/BHistoryRecord.java` — the history sample record type
> - `bql-rt/decompiled/com/tridium/script/ComplexScriptFields.java` — BQL field lookup on a `BStruct`
> - Remittance `[CERT]`: [B357] §357.5 (BBqlGrid spine), [B358] §358.2/§358.3 (range lives in the ORD)
> - `niagara-help` not re-queried — [B358] already recorded 3 real zeros on "history query time range /
>   bql relative time / report scheduling export source"; the gap here is a code-structural one, not a doc one.
>
> **Method**: inline driver read + token-verify of a delegated `sonnet` structural sweep (51 tool-uses). Every
> load-bearing citation re-resolved against disk before writing. Block type: **EVIDENCE** (decompilation) —
> the verdict is a structural NO derived from cited code, not an inference.

---

## §359.1 — What `BBqlGrid` actually is: a component viewer, not a record viewer `[CERT]`

[B357] §357.5 described `BBqlGrid` as "one query → one table". That is true but underspecifies the mechanism,
and the mechanism is the whole answer to R4. `BBqlGrid.resolve()` runs a **two-phase, component-oriented**
pipeline — it does NOT render the query result rows directly:

| Phase | What the code does | Citation |
|---|---|---|
| Ban `select *` | rejects a star query outright | `BBqlGrid.java:102` (`if (body.startsWith("select *"))`) |
| Force `ordInSession` col 0 | rewrites the body to `"select ordInSession," + body.substring("select ".length())` | `BBqlGrid.java:105` |
| Eager slurp | `Tables.slurp((BITable)query.resolve(base).get())` — materializes the ENTIRE result into a heap `BInMemoryTable` | `BBqlGrid.java:112` |
| Extract col-0 as ORDs | `ords[i] = BOrd.make(table.get(i).cell(cols.get(0)).toString())` — column 0 of every row is treated as a navigable ORD string | `BBqlGrid.java:126` |
| Batch-resolve to targets | `BatchResolve br = new BatchResolve(ords); model.targets = br.resolve(base, cx).getTargets()` | `BBqlGrid.java:128` |
| Render per cell | `Model.getObjectAt(row,col)` returns `targets[row]` — the **same resolved component** for every column; the column "value" is `BFormat.make("%colName%")` applied to that component | `BBqlGrid.java:190-196` |
| Failure | any resolve error → `throw new LocalizableRuntimeException("report","Report.gridTable.bql.resolveError")` | `BBqlGrid.java:139` |

The consequence is decisive: **each report row is a resolved `BComponent`, and each column is a property/slot of
that component**. `BBqlGrid` is not a viewer of arbitrary BQL result rows — it is a viewer of the *components
named by* the result rows' column 0. The BQL query is used as a way to ENUMERATE COMPONENTS, and their live slots
supply the columns. `[CERT]` `BBqlGrid.java:102,105,112,126,128,190-196`.

## §359.2 — Why a history query breaks it: `BHistoryRecord` is a `BStruct`, not a `BComponent` `[CERT]`

A history/trend query does not return components — it returns **records**:

- `public abstract class BHistoryRecord extends BStruct` `[CERT]` `BHistoryRecord.java:51-52`. A `BStruct` is a
  value type; it is **not** a `BComponent`, it does not live in the component space, and it has **no
  `getOrdInSession()`**. (`BHistory` itself — the collection — does have `ordInSession = "history:"+id`, but that
  is the ORD of the *whole history*, not of an individual sample row.)
- When `BBqlGrid` forces `select ordInSession,` (§359.1) over a history query, the projection is evaluated per
  record. BQL field lookup on a struct goes through `ComplexScriptFields.getField()`: `Property prop =
  complex.getProperty(name)` for `"ordInSession"` returns **null**, then it falls to introspected getters —
  and a `BStruct` has no `getOrdInSession()` method either. `[CERT]` `ComplexScriptFields.java:26-31`.
- So col-0 for a history row resolves to **null**. `BBqlGrid.java:126` then calls
  `table.get(i).cell(cols.get(0)).toString()` on that null cell → **NPE** → caught and rethrown as the
  `Report.gridTable.bql.resolveError` at `BBqlGrid.java:139`. `[INFER]` (the null→NPE step is deduced from the
  two cited facts: the field resolves null, and the code unconditionally `.toString()`s it).

The virtual-space sibling `BNiagaraVirtualBqlGrid` does not rescue it: it uses `"slotPath"` / a `slot:` prefix
instead of `ordInSession` — still a component-space navigable key that a history record does not have.
`[CERT]` sweep-confirmed at `BNiagaraVirtualBqlGrid.java:94` (component-only).

## §359.3 — Corroborating structural facts (independent re-measure of the dramatic negative) `[CERT]`

A verdict this decisive (the module cannot do the client's core table) is a DRAMATIC NEGATIVE — re-measured by
two independent methods before entering this block (METHODOLOGY hard rule):

1. **`BatchResolve` has no history fast-path.** Its component-space resolver only accepts ORDs containing
   `slot:`, `virtual:`, or `h:`; anything else is skipped: `if (ord.toString().indexOf("slot:") < 0 &&
   ord.toString().indexOf("virtual:") < 0 && ord.toString().indexOf("h:") < 0 ... continue)` `[CERT]`
   `BatchResolve.java:125`. Even if a history record *did* somehow yield an ORD, the `history:` scheme falls
   outside the batch path. (`h:` is the ord-shorthand for handles, not the `history:` scheme.)
2. **The report module contains zero history-grid code.** `fd -e java . report | rg -i history` (excluding the
   duplicate decompiler pipelines) returns **0 files** — there is no `BHistoryGrid`, no history cursor, no
   `BHistoryRecord` import anywhere in `report-rt`. `[CERT]` (measured, 0 hits). The module has exactly three
   grids — `BBqlGrid`, `BComponentGrid` ([B358] §358.2), `BNiagaraVirtualBqlGrid` — and all three are
   component-oriented.

Both methods agree with the code-path reading in §359.1-2: the negative is a genuine structural property, not a
counting artifact.

## §359.4 — What that leaves: eager-slurp memory risk + the one thing that DOES work `[CERT]`/`[INFER]`

- **Eager slurp is a second, independent hazard.** `Tables.slurp()` "will typically result in the entire table
  being read into memory" (docstring, `Tables.java`), and `BBqlGrid.java:112` slurps *before* any resolve. A
  wide date range of trend samples (tens of thousands of rows) is fully materialized in heap first — so even a
  hypothetical history-aware grid built on this path carries an OOM risk on a large range. `[CERT]`/`[INFER]`.
- **What DOES resolve through `BBqlGrid`:** a *component-space* query — e.g.
  `station:|slot:/Plant|bql:select ordInSession, out, status from control:ControlPoint` — gives one row per
  point with its **current** `out`/`status`. That is a live snapshot of many points, NOT a time series (same
  shape `BComponentGrid` gives — [B358] §358.2). The client's PSI-vs-time-over-a-range table is precisely the
  shape this path cannot produce. `[INFER]` from §359.1 + [B358] §358.2.

## §359.5 — R4 verdict and the feasibility consequence `[INFER]`

**R4 = NO.** The stock `report` module cannot produce a range-scoped history-sample table through its BQL grids.
The blocker is structural, not configuration: `BBqlGrid` renders **components named by column 0**, and history
samples are `BStruct` records with no navigable `ordInSession`. There is no `BHistoryGrid`.

The sanctioned path is therefore **custom code that bypasses `BBqlGrid` entirely**: a custom `BExportSource` /
`handleGenerate()` ([B357] §357.3, [B358] §358.5 option (a)) that cursors the `BIHistory`/`BITable<BHistoryRecord>`
directly (the `?period=` window from [B358] §358.3 applied to the history ORD), reads `timestamp`/`value` off each
record, and hands rows to an exporter (`BGridToCsv`/`BGridToText`, [B357] §357.6) — or, for an interactive UI, the
webChart/history-query path ([B45], [B358] §358.5 option (c)) that already cursors histories server-side. This
promotes the "three-subsystem composition + glue" thesis ([B357] §357.8) from *likely* to **required**: even the
TABLE — the part that looked native — needs custom development. `[INFER]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BBqlGrid` bans `select *` and force-prepends `select ordInSession,` | [CERT] | `BBqlGrid.java:102,105` | ✅ read |
| 2 | It eager-slurps the whole result via `Tables.slurp` before resolving | [CERT] | `BBqlGrid.java:112` | ✅ read |
| 3 | It reads col-0 of each row as an ORD string and `BatchResolve`s them to component targets | [CERT] | `BBqlGrid.java:126,128` | ✅ read |
| 4 | Each cell renders against `targets[row]` — the grid is a component viewer, not a row viewer | [CERT] | `BBqlGrid.java:190-196` | ✅ read |
| 5 | A resolve failure throws `Report.gridTable.bql.resolveError` | [CERT] | `BBqlGrid.java:139` | ✅ read |
| 6 | `BHistoryRecord extends BStruct` (not a `BComponent`; no `getOrdInSession`) | [CERT] | `BHistoryRecord.java:51-52` | ✅ read |
| 7 | BQL field lookup for `ordInSession` on a struct returns null (property null → introspection null) | [CERT] | `ComplexScriptFields.java:26-31` | ✅ read |
| 8 | Null col-0 cell → NPE → resolveError for a history query | [INFER] | §359.2 from claims 5,7 | ✅ reasoned |
| 9 | `BatchResolve` fast-path accepts only `slot:`/`virtual:`/`h:`; `history:` is skipped | [CERT] | `BatchResolve.java:125` | ✅ read |
| 10 | Report module has 0 history-grid classes (independent re-measure) | [CERT] | `fd -e java report \| rg -i history` = 0 | ✅ measured |
| 11 | Eager slurp is an OOM risk on a large range independent of the ordInSession blocker | [CERT]/[INFER] | `Tables.java` slurp docstring + `BBqlGrid.java:112` | ✅ read+reasoned |
| 12 | Feasible path = custom BExportSource cursoring BIHistory directly, or the webChart/history path | [INFER] | §359.5 from [B357]§357.3, [B358]§358.5 | ✅ reasoned |

**Marker tally**: [CERT] ×9 (own reads/measure) · [CERT]/[INFER] ×1 · [INFER] ×2. Ratio [INFER]/[CERT] ≈ 0.25.
Block type = **EVIDENCE**: the low ratio is healthy and the verdict is code-grounded. Load-bearing tokens
checked against disk this iteration: claims 1-7,9-10 (10 citations re-resolved by the driver, not trusted from
the sweep); the dramatic negative was re-measured by two independent methods (§359.3). Delegated sweep tier:
`sonnet` (structural comprehension), driver-verified.

## Connections

- [B357] §357.5/§357.6/§357.8 — the BBqlGrid spine, the CSV/text exporters, and the composition thesis this
  block promotes to *required*.
- [B358] §358.2/§358.3/§358.5 — `BComponentGrid` vs `BBqlGrid` (both component-oriented), the `?period=` range
  in the ORD, and the custom-`BReportSource` / webChart escape hatches now confirmed as the only paths.
- [B45] — the webChart/history-query server path that DOES cursor histories with a runtime `?period=` range.
- Forward: **R5** (alarm records in a report) tests the SAME `ordInSession` wall for `BAlarmRecord` → [B360].

## Gaps opened / queued

No new gaps. R4 closed (verdict NO — structural). Remaining investigable: R3 (export/xlsx), R5 (alarm records —
next), R6 (chart-in-report), R7 (ux/web), R8 (wb builder), R9 (synthesis) → 6 open.
