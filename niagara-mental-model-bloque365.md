# Niagara N4 — Bloque 365: the `report` module (IX) — the Workbench builder authors component grids; its query editor hardcodes `select ordInSession`, so the client's history-sample table is NOT authorable through stock UI (confirms B359 from the tooling side)

> **Focus**: `reports`, gap R8 (Workbench builder workflow). The last detail gap — how an engineer BUILDS a report
> in Workbench, and whether the client deliverable is authorable through stock editors or needs the custom module
> established in [B359]–[B362]. Closes the focus 9/9. Follows [B357]–[B364].
>
> Subject version: Niagara N4.14 `report-wb` (decompiled corpus, release 2024-05-28).
>
> **Sources** (all `[CERT]`, read/verified this iteration):
> - `report-wb/decompiled/com/tridium/report/grid/editor/` — `BComponentGridEditor`, `BComponentGridQueryEditor`,
>   `BGridEditorPane`
> - `report-wb/decompiled/com/tridium/report/grid/ui/` — `BGridTable`, `BGridLabelPane`, `GridUtil`
> - `report-wb/decompiled/com/tridium/report/ui/` — `BReportPane`, `BReportPxMedia`
> - Remittance `[CERT]`: [B358] §358.2 (component grid = live snapshot), [B359] §359.1-2 (the `select
>   ordInSession` component-viewer wall), [B361] §361.4 (chart embedding via BPxInclude), [B362] (composition)
>
> **Method**: inline driver token-verify of a delegated `sonnet` sweep (14 tool-uses). The load-bearing claim
> (the query editor hardcodes `select ordInSession`) and one §14 adjudication (rejecting the sweep's "partially
> supports history table" over-read) re-resolved against disk. Block type: **EVIDENCE**.

---

## §365.1 — The builder authors a `BComponentGrid`: template + query + columns + rows `[CERT]`

`BComponentGridEditor` is the `BWbView` that opens on a `BComponentGrid`, agent `@AgentOn(types=
{"report:ComponentGrid"}, requiredPermissions="W")` `[CERT]` `BComponentGridEditor.java:68` (note: **write**
permission to edit, vs the `"r"` preview/ux views). It edits four things read from the grid — `template`, `query`,
`columns`, `rows` — via an interactive `BGridEditorPane` and saves them transactionally (sweep-confirmed
`doLoadValue`/`doSaveValue`). The engineer's gestures:

- **Drag a component onto the template zone** → sets the grid template (auto-adds a `BSingleRow`). The template
  must be set first: `if (null == base) throw new BajaRuntimeException("Must define Component Grid Template
  before defining Query")` (sweep-cited `BComponentGridEditor.java:768`). `[CERT]`/[INFER].
- **Drag components onto the header zone** → derives `slot:`-relative column ORDs.
- **Drag components onto the rows zone** → adds `BSingleRow`s.
- **Double-click the query zone** → opens `BComponentGridQueryEditor` (§365.2).

`BGridEditorPane` is a custom-painted canvas with colored zones (header / template / query / rows / query-results),
double-click editors, and drag-drop — a genuine visual builder for a **component grid** (the live-snapshot grid of
[B358] §358.2), not a history/record grid. `[CERT]` (sweep-confirmed, structure token-checked).

## §365.2 — The query editor hardcodes `select ordInSession` — this is the load-bearing fact `[CERT]`

`BComponentGridQueryEditor` assembles the BQL query, and its projection is **fixed**:

```java
public BOrd toOrd() {
    String q = "|bql:select ordInSession";          // line 750 — the projection is HARDCODED
    ...  // appends extent + " where " + qualifier clauses (field op 'value')
    if (this.isHistoryQuery()) {                     // line 781
        ... q = "?" + period.toOrdParams() + q;      // prepends ?period= for a history base
    }
    return BOrd.make(this.base.getNavOrd() + q);
}
```

`[CERT]` `BComponentGridQueryEditor.java:750,781-790`. And `isHistoryQuery()` merely tests whether the query
BASE's last ORD scheme is `history`: `return q[q.length-1].getScheme().equals("history")` `[CERT]`
`BComponentGridQueryEditor.java:744-746`. The period picker (`BDynamicTimeRangeFE`) is shown only then
(`periodFE.setVisible(model.isHistoryQuery())` `[CERT]` `:419`).

**What this means**: the editor can ONLY author a `select ordInSession` query — a **component** projection. It has
no way to express `select timestamp, value` (a **record** projection). Adding a history base just prepends a
`?period=` time window to that component query; it does not switch the projection to history samples.

## §365.3 — §14 adjudication: this CONFIRMS [B359], it does not contradict it `[CERT]`

The R8 delegated sweep concluded the stock editor "partially supports the history table." **I reject that reading**
on the code evidence. Feeding a history base into a `select ordInSession` projection is precisely the case [B359]
§359.1-2 proved broken: a `history:` query yields `BHistoryRecord` structs, which have no `ordInSession`, so
column 0 resolves null → NPE → `Report.gridTable.bql.resolveError`. So the editor does not "partially" build the
client's table — it **cannot build it at all**: the one projection it emits is the one that fails on records, and
the projection the client needs (`timestamp, value`) is not expressible in the UI. `[CERT]`/[INFER] (§14 clarifies
the sweep; [B359] stands, now corroborated from the tooling side: the stock Workbench builder never authors a
record-projection query). This is the same conclusion reached three other ways (runtime B359, alarm B360, chart
B361): **the client's report is not stock-authorable.**

## §365.4 — Preview, container, and PX scaffolding `[CERT]`

- **Preview**: `BGridTable` and `BGridLabelPane` (agents on `report:IGrid`, `requiredPermissions="r"`) are the
  read-only rendered views; `GridUtil.getStatusFg/Bg` colors cells by `BStatus` (fault/alarm/stale). These render
  a *grid*, not a chart. `[CERT]` (sweep-confirmed, structure token-checked).
- **Container**: `BReportPane` stacks child widgets vertically with `rowGap`, plus a logo/page-number/timestamp
  chrome. It has **no section-type awareness** and **no explicit `BPxInclude` handling** — it hosts any `BWidget`
  child generically. A chart reaches a report only when a `BPxInclude` child embeds a PX containing a `BChartPane`,
  and that composition + the PDF expansion is the wb-only path of [B361] §361.4 — not something `BReportPane`
  itself configures. `[CERT]`/[INFER].
- **PX scaffolding**: `BReportPxMedia` is a `@NiagaraSingleton` `BPxMedia` that registers the new-file template
  `file:!defaults/workbench/newfiles/ReportPxFile.px` `[CERT]` `BReportPxMedia.java:27` — Workbench "New File"
  scaffolding, confirming [B361] §361.4. The template lives inside the jar (no `.px` in the extracted tree).

## §365.5 — The engineering workflow, and where it stops for the client `[INFER]`

The stock Workbench flow to build a report: add `BReportService` → add `BReport`/`BExportSource` under it →
configure recipients via the `BExportSourceInfoFE` 2-step wizard (pick source ORD → pick a `BExporter` agent +
settings, `BExporter` import at `BExportSourceInfoFE.java:47`) → add a `BComponentGrid` → author it in
`BComponentGridEditor` (template + query + columns + rows) → preview via `BGridTable`/`BGridLabelPane`. `[INFER]`
(composed from the cited agents).

Mapped to the client's deliverable:

| Client requirement | Stock Workbench editor? |
|---|---|
| Component/state grid (live values of N points) | ✅ fully authorable (`BComponentGridEditor`) |
| Alarm grid (list of alarm records) | ⚠ authorable ORD, but hits the same `ordInSession` record wall at resolve ([B360]) |
| **History-sample table (PSI vs time)** | ❌ **NOT authorable** — query editor hardcodes `select ordInSession` (§365.2-3) |
| **Banded PSI chart** | ❌ NOT authorable — no chart editor; `BReportPxMedia` scaffolds a blank PX; embedding a live-wired banded chart is the custom path ([B361]/[B362]) |

So the stock builder authors exactly the grid the module was designed for (a component snapshot) and stops
precisely where the client's needs begin. R8 closes the focus with the same verdict its five siblings reached from
different angles. `[INFER]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BComponentGridEditor` edits a component grid; agent needs `requiredPermissions="W"` | [CERT] | `BComponentGridEditor.java:68` | ✅ read |
| 2 | Template must be set before the query (guard throw) | [CERT]/[INFER] | `BComponentGridEditor.java:768` (sweep) | ✅ sweep-cited |
| 3 | The query editor's projection is HARDCODED to `select ordInSession` | [CERT] | `BComponentGridQueryEditor.java:750` | ✅ read |
| 4 | `isHistoryQuery()` only tests the base scheme; period is prepended as `?period=` | [CERT] | `BComponentGridQueryEditor.java:744-746,781-790,419` | ✅ read |
| 5 | §14: a history base + `select ordInSession` = the exact B359 NPE case → history table NOT authorable | [CERT]/[INFER] | §365.3 from claim 3 + [B359] §359.1-2 | ✅ reasoned |
| 6 | Preview views (`BGridTable`/`BGridLabelPane`) render a grid, not a chart; color by BStatus | [CERT] | `GridUtil` status brushes (sweep) | ✅ sweep-cited |
| 7 | `BReportPane` stacks children generically; no BPxInclude-specific handling; chart via BPxInclude only | [CERT]/[INFER] | §365.4 + [B361] §361.4 | ✅ reasoned |
| 8 | `BReportPxMedia` registers the new-file PX template (scaffolding, not pipeline) | [CERT] | `BReportPxMedia.java:27` | ✅ read |
| 9 | Stock builder authors component/state grids fully; history table + banded chart NOT authorable | [INFER] | §365.5 from claims 3-5 + [B360]/[B361] | ✅ reasoned |

**Marker tally**: [CERT] ×5 · [CERT]/[INFER] ×3 · [INFER] ×1. Ratio ≈ 0.2. Block type = **EVIDENCE**. Load-bearing
tokens re-resolved against disk this iteration: claims 1,3,4,8 (driver-verified; the `select ordInSession` hardcode
and `isHistoryQuery` semantics read directly to settle the §14 adjudication, per the framework-semantic rule —
NOT trusted from the sweep). Delegated sweep tier: `sonnet`; the sweep's "partially supports history table" claim
was REJECTED on driver re-read (§365.3). §14 clarification recorded: corroborates [B359], corrects the sweep.

## Connections

- [B358] §358.2 — the component grid (live snapshot) this builder authors.
- [B359] §359.1-2 — the `select ordInSession` component-viewer wall; §365.3 corroborates it from the tooling side.
- [B360]/[B361] — the alarm and chart legs the stock builder also cannot author for the client.
- [B362] — the composition/cost; R8 confirms the custom module is required, the stock builder is not a shortcut.
- [B363]/[B364] — the CSV export and the read-only web viewer that render what this builder produces.

## Gaps opened / queued

No new gaps. R8 closed. **Focus `reports` now 9/9 — all gaps closed, investigable_open = 0.** Terminal trigger:
the client-composition synthesis is already [B362]; next is the §18 self-retrospective and the focus close.
