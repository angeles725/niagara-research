# Block 431 — The manager framework: BAbstractManager maps a container's children to permission-filtered rows, with a BJob-driven learn/discovery pane

> Research of the Workbench **manager / table framework** (focus `workbench`, gap WB05) — the
> `BAbstractManager` machinery every device/point/folder manager extends. Scope: the manager base + layout,
> the `MgrModel`/`MgrColumn` model, how children become table rows, the `MgrLearn` discovery hook, the
> New/Edit/Add flow, and how `BCellTable`/`cellmini` relate (they DON'T — a premise correction). Does NOT
> cover a specific driver's manager (bacnet/modbus device managers are their own modules).
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `workbench-wb.jar`
> sha256 `17a84e2a26a6f6af0e1893738115ebb1ac7e002d3af8c11409fa4ee17f3d7c8c`.
>
> Sources: Tridium docSource (`sources/tridium-src/workbench-wb/.../mgr/` — the `mgr` package is docSource-only,
> no vineflower counterpart) + Vineflower impl (`sources/decompiled/workbench-wb/com/tridium/workbench/{celltable,cellmini}/`).
> Method: docSource for the manager contract, Vineflower for celltable, all lines re-verified live. Markers:
> `[CERT]` (`file:line`) · `[INFER]` deduction.
>
> Workbench UI framework. Connects [Block 428] (managers are `@AgentOn` views the shell opens), [Block 430]
> (managers reuse `MgrColumn` field editors), [Block 406] (deep managers query via BQL), [Block 316]/security
> thread (row-level `hasOperatorRead` filter).

---

## 431.1 — Two premise corrections (§14) `[CERT]`

The gap WB05 was seeded with two wrong premises; the source refutes both: `[CERT]`

1. **There is no `BWManager`.** The framework root is `BAbstractManager` (`fd` for `BWManager.java` → 0 hits).
   `[CERT]`
2. **`BCellTable`/`cellmini` is NOT the manager's main table.** The manager db table is `BMgrTable`; `BCellTable`
   is a SEPARATE live-editor grid widget (§431.5). The gap conflated them. `[CERT]`

## 431.2 — The manager base and its layout `[CERT]`

`BAbstractManager extends BWbComponentView implements BIExportableTableView`
(`sources/tridium-src/workbench-wb/javax/baja/workbench/mgr/BAbstractManager.java:53`–`:55`). `[CERT]` It is
abstract with NO `@AgentOn` — concrete driver managers subclass it and add the `@AgentOn(types=...)` to bind
themselves as the view for a specific container type. `[CERT]` `doLoadValue(target)` receives the selected
`BComponent`; `init()` builds an `BEdgePane` whose center is a `BSplitPane`: the lower half is always the
`tablePane` (the db table), the upper half is optionally `learnPane` / `tagDictionaryPane` / `templatePane`
(`learn = makeLearn()` at `BAbstractManager.java:154`). `[CERT]` `[INFER]` this is the familiar two-pane
manager: discovered items on top, the live database below.

## 431.3 — Children → rows: SlotCursor (or BQL), filtered by type AND operator-read `[CERT]`

`BMgrTable.reload()` (`.../mgr/BMgrTable.java:139`) is where a container's children become rows. It takes the
model's include-types (`:147`), walks the target's properties via a `SlotCursor<Property>` (`:187`), and
accepts a child only if BOTH the model accepts it AND the caller may read it: `[CERT]`

```java
if (model.accept(rows[i]) && rows[i].getPermissions(manager.getCurrentContext()).hasOperatorRead())  // :159
```

So the manager is **permission-filtered at the row level** — a user without operator-read on a child never
sees that row. `[CERT]` `[INFER]` this is the same `hasOperatorRead` gate the corpus security thread flagged
(local syntax like `permissions="unrestricted"` still passes through this call). Deep managers
(`FolderModel.isAllDescendants`) instead fire a BQL `select slotPath from <types>` and batch-resolve ORDs
([Block 406]). `[CERT]`/`[INFER]`

## 431.4 — Columns: MgrModel.makeColumns → MgrColumn (display + edit + editor) `[CERT]`

`MgrModel extends MgrSupport`; its `makeColumns()` (subclass override) returns a `MgrColumn[]` — the default is
`{ Name, Type }`. `[CERT]` `MgrColumn` is abstract (`.../mgr/MgrColumn.java:53`); each column implements
`get(row)` (display), `load`/`save(MgrEditRow, ...)` (edit serialization), and `toEditor`/`fromEditor` (the
field-editor factory — reusing [Block 430]'s `BWbEditor`). `[CERT]` Flags: `EDITABLE` (in the edit dialog),
`UNSEEN` (hidden by default), `READONLY` (`MgrColumn.java:179`,`:206`). `[CERT]` Built-in columns: `Name`,
`Type`, `Prop`, `PropPath`, `PropString`, `MixIn`, `TagColumn`, … `[CERT]`

## 431.5 — BCellTable / cellmini: a separate live-editor grid `[CERT]`

`BCellTable extends BAbstractCellTable` (itself a `BTable`), holding `Array<BWbCellEditor[]> rows`
(`sources/decompiled/workbench-wb/com/tridium/workbench/celltable/BCellTable.java:12`,`:14`). `[CERT]` Its
distinguishing trait: **cells are REAL child widgets** (`BWbCellEditor[]`), added as components and positioned
by pixel bounds in `doLayout`, NOT painted by a cell renderer. `[CERT]` The `cellmini` package
(`BMiniTextField`, `BMiniListDropDown`, …) are border-stripped, no-preferred-size input widgets embedded inside
those cell editors so the table's layout fully controls placement. `[CERT]` `[INFER]` this machinery backs
configurable grids where each cell needs a LIVE editor (e.g. the BQL expression grid), and is distinct from
`BMgrTable` — the manager's main table uses `MgrColumn` renderers, not `BCellTable`.

## 431.6 — Learn/discovery: a BJob the driver submits `[CERT]`

`MgrLearn extends MgrSupport` (`.../mgr/MgrLearn.java:29`), abstract, and `BAbstractManager.makeLearn()`
returns `null` by default — driver managers override it. `[CERT]` Its abstract contract is `toTypes(discovery)`
+ `toRow(discovery, row)` (map a discovered thing to types + a row) and it owns a `BJob` (`MgrLearn.java:8`
imports `javax.baja.job.BJob`; a `BJobBar` shows progress, `:165`). `[CERT]` Flow: the `Discover` command sets
learn-mode on and the subclass submits a `BJob` via an Action on the container; on `jobComplete` the manager
calls `updateRoots()` to fill the learn table. `[INFER]` so discovery is asynchronous and job-based — the
framework provides the pane, progress bar, and existing-row matching; the driver provides the protocol scan.

## 431.7 — New/Edit/Add: command → batch dialog → one commit `[CERT]`

All manager mutations are `MgrCommand`s. **New** prompts for a type (`getNewTypes()`) + count, builds a
`MgrEdit` of default instances (`MgrModel.newInstance`). **Add** (from learn) calls `toTypes`/`toRow` per
discovered item into a `MgrEdit`. Both then `MgrEdit.invoke(cx)` (`.../mgr/MgrEdit.java:468`) which opens
`BMgrEditDialog` (a batch table + per-row `ColumnInput` editors) unless a `quickContext` sentinel bypasses the
prompt (QuickAdd). `[CERT]` OK → `commit()` (`:477`) calls each column's `save`, then `MgrModel.addInstances`
→ `Mark.moveTo(container, …)` to add the components. `[CERT]` `[INFER]` the manager edits in BATCH and commits
through the same slot-add machinery — one dialog can create/edit many children at once.

## 431.8 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | No `BWManager`; base is `BAbstractManager extends BWbComponentView` (abstract, no @AgentOn) | `[CERT]` | `BAbstractManager.java:53`; fd 0 hits |
| 2 | Two-pane: BSplitPane, lower=table, upper=learn/tagDict/template; `learn=makeLearn()` | `[CERT]` | `BAbstractManager.java:154` |
| 3 | `BMgrTable.reload` maps children via SlotCursor, filtered by `model.accept` AND `hasOperatorRead` | `[CERT]` | `BMgrTable.java:159`,`:187` |
| 4 | Columns = `MgrColumn` (abstract) with display/edit/editor + EDITABLE/UNSEEN/READONLY flags | `[CERT]` | `MgrColumn.java:53`,`:179` |
| 5 | `BCellTable extends BAbstractCellTable`, cells are real `BWbCellEditor[]` widgets — SEPARATE from BMgrTable | `[CERT]` | `celltable/BCellTable.java:12`,`:14` |
| 6 | `MgrLearn` (default null) owns a `BJob`; discovery is async job-based | `[CERT]` | `MgrLearn.java:29`,`:8` |
| 7 | New/Add → `MgrEdit.invoke`→`BMgrEditDialog`→`commit`→`addInstances`→`Mark.moveTo` | `[CERT]` | `MgrEdit.java:468`,`:477` |

**Marker tally**: `[CERT]` ≈ 24 · `[INFER]` 7 ([INFER]/[CERT] ≈ 0.29). Type: **EVIDENCE block** (model
overview) — ratio healthy. VERIFY-BEFORE-ACTING: both gap premises (`BWManager`, celltable-is-the-manager-table)
were REFUTED by direct check before writing; every structural line re-verified live. The `mgr` package has NO
vineflower output — docSource is the sole (clean) source and is authoritative. Tokens confirmed:
`BAbstractManager extends BWbComponentView`, `hasOperatorRead`, `MgrColumn`+`EDITABLE`, `BCellTable extends
BAbstractCellTable`, `MgrLearn`+`BJob`, `MgrEdit.invoke`/`commit`.

## 431.9 — Connections

- **[Block 428]** — a manager is an `@AgentOn` view the shell opens; §431.2 is what runs after the ORD→view
  resolution lands on a manager.
- **[Block 430]** — `MgrColumn.toEditor` reuses the same `BWbEditor` field editors; the manager's edit dialog
  is a batch property editor.
- **[Block 406]** — deep managers resolve rows via a BQL `select slotPath from <types>` query.
- **security thread** — the row-level `hasOperatorRead` filter (§431.3) is where a manager enforces read
  permission per child; a finding for who-sees-what audits.

<!-- research-block: focus workbench, gap WB05 (manager/table framework) — CLOSED at body grade; 2 premise corrections (BWManager, celltable) -->
