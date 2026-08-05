# Niagara N4 — Bloque 360: the `report` module (IV) — alarm-records-in-a-report: the DB is fully BQL-queryable and carries every limit-crossing field, but the SAME `ordInSession` wall blocks `BBqlGrid`

> **Focus**: `reports`, gap R5 (alarm records in a report). Twin of [B359] (R4): it tests whether the client's
> **alarm-marker** need — mark where PSI crossed a limit, with a timestamp per crossing — can be served by
> querying the alarm database through the report module's grid. Follows [B357]/[B358]/[B359].
>
> Subject version: Niagara N4.14 `report-rt` + `alarm-rt` (decompiled corpus, unversioned jars).
>
> **Sources** (all `[CERT]`, read directly this iteration):
> - `alarm-rt/decompiled/javax/baja/alarm/BAlarmDatabase.java` — the alarm space's BQL entry point + RBAC gate
> - `alarm-rt/decompiled/javax/baja/alarm/BAlarmRecord.java` — the alarm record type + its columns + the
>   `alarmData` facet bag (the limit-crossing values)
> - `report-rt/decompiled/…` — grepped for alarm code (proven absent)
> - Remittance `[CERT]`: [B359] §359.1-2 (the `BBqlGrid` component-viewer wall + `BStruct`/`ordInSession`),
>   [B357] §357.3/§357.6 (BExportSource + exporters)
> - The corpus already covers the alarm subsystem itself ([B8],[B44],[B54],[B142],[B240]) — remittance, this
>   block only touches the alarm DB's *queryability from a report*.
>
> **Method**: inline driver read + token-verify of a delegated `sonnet` structural sweep (57 tool-uses). Proven
> absence (no alarm code in report) and every load-bearing column/gate re-resolved against disk. Block type:
> **EVIDENCE** (decompilation).

---

## §360.1 — The report module has ZERO alarm-specific code (proven absent) `[CERT]`

`rg -il 'alarm' report/report-rt/decompiled -g '*.java'` returns **0 files** `[CERT]` (driver-re-verified, not
trusted from the sweep). There is no alarm source, no alarm grid, no `BAlarmRecord` import anywhere in the report
module. Any alarm-in-a-report capability is therefore **entirely remittance** to (a) the `alarm` module's BQL
surface and (b) the generic `BBqlGrid` — exactly the composition thesis of [B357] §357.8. The report module
brings only the schedule/deliver pipeline; the alarm data and its query surface live elsewhere.

## §360.2 — The alarm database IS a first-class BQL source `[CERT]`

Unlike a history collection, the alarm DB is built to be queried:

| Fact | Evidence | Citation |
|---|---|---|
| `BAlarmDatabase` implements `Queryable`, `BIRelational` | `public abstract class BAlarmDatabase extends BSpace implements Queryable, BIRelational, …` | `BAlarmDatabase.java:86-91` |
| Its session ORD is `alarm:` | `private static BOrd ordInSession = BOrd.make("alarm:")` | `BAlarmDatabase.java:94` |
| It answers BQL directly | `public BObject bqlQuery(OrdTarget base, OrdQuery query){ return new BAlarmDbQueryResult(this, (BqlQuery)query); }` | `BAlarmDatabase.java:246` |
| Two built-in relations to query | `if (id.equals("openAlarms")) …; if (id.equals("ackPendingAlarms")) …` | `BAlarmDatabase.java:259` |
| The framework's own ORD form | `new StringBuilder("alarm:\|bql:select * from openAlarms where …")` | `BAlarmDatabase.java:192` |

So the query pattern is real and runtime-proven: **`alarm:|bql:select timestamp, source, priority, normalTime,
sourceState from openAlarms where …`** (archive via `alarm:archive|bql:…`). This resolves to a
`BITable<BAlarmRecord>`. `[CERT]` `BAlarmDatabase.java:86-91,94,192,246,259`.

**One runtime nuance**: `bqlQuery` throws if invoked from the engine thread —
`throw new AlarmException("BAlarmDatabase.bqlQuery called from Nre;Engine Thread")` `[CERT]`
`BAlarmDatabase.java:249`. A report generates on the report service's own serialized worker thread ([B357]
§357.2), NOT the engine thread, so a report-driven alarm query satisfies this — but any glue that tried to run it
inline on the engine thread would fault. `[INFER]` from [B357] §357.2 + the cited guard.

## §360.3 — Every field the client needs is a queryable column `[CERT]`

`BAlarmRecord` exposes 14 typed `@NiagaraProperty` slots, all BQL-addressable (BQL wraps each property as a
column). The client's alarm-marker need maps cleanly onto them:

| Client need | Column | Type | Citation |
|---|---|---|---|
| **timestamp of the crossing** | `timestamp` | `BAbsTime` | `BAlarmRecord.java:74` |
| which point crossed | `source` | `BOrdList` (source component slot path) | `BAlarmRecord.java:79` |
| the transition (offnormal/fault/normal) | `sourceState` | `BSourceState` | `BAlarmRecord.java:76` |
| when it returned to normal | `normalTime` | `BAbsTime` | `BAlarmRecord.java:82` |
| priority / class | `priority` (int), `alarmClass` (String) | — | `BAlarmRecord.java:80-81` |
| ack state / who / when | `ackState`, `user`, `ackTime` | — | `BAlarmRecord.java:77,83-84` |

`BAlarmRecord extends BStruct` `[CERT]` `BAlarmRecord.java:72-73` — hold that fact for §360.5.

## §360.4 — The actual crossing VALUES (highLimit / lowLimit / presentValue) live in an untyped facet bag `[CERT]`

The client's bands are **<12 / >28 critical, 15–25 normal** — so the report must show the limit that was crossed
and the value that crossed it. Those are NOT typed columns; they live inside the `alarmData : BFacets` bag
(`BAlarmRecord.java:85`) as named string keys:

- `HIGH_LIMIT = "highLimit"` `BAlarmRecord.java:130`, `LOW_LIMIT = "lowLimit"` `:131`,
  `PRESENT_VALUE = "presentValue"` `:137`, `MSG_TEXT = "msgText"` `:119` — retrieved via
  `getAlarmFacet(String key)` `:351`. The full 30-key vocabulary (including `ALARM_VALUE`, `DEADBAND`,
  `SETPT_VALUE`, `TO_STATE`/`FROM_STATE`, …) is at `BAlarmRecord.java:494`. `[CERT]`.
- **Consequence**: `highLimit`/`lowLimit`/`presentValue` are reachable per-record, but only through
  `getAlarmFacet()` / `getFormattedAlarmDataValue()`, **not** as plain BQL `select` columns. Extracting them
  into report columns needs custom rendering code that pulls each facet key by name. `[INFER]` from the cited
  API (facet bag accessor vs typed property). This is a second reason the stock grid can't just emit the client's
  table even setting aside §360.5.

## §360.5 — Same wall as history: `BAlarmRecord` is a `BStruct`, so `BBqlGrid` NPEs `[CERT]`/`[INFER]`

R5's verdict rides on the exact mechanism [B359] established for R4. `BBqlGrid` force-prepends
`select ordInSession,` and resolves column 0 of every row as a navigable component ORD ([B359] §359.1,
`BBqlGrid.java:105,126,128`). But:

- `BAlarmRecord extends BStruct` `[CERT]` `BAlarmRecord.java:72-73` — a value record, not a `BComponent`; it has
  **no `getOrdInSession()`**.
- BQL field lookup for `ordInSession` on the struct returns null ([B359] §359.2, `ComplexScriptFields.java:26-31`)
  → col-0 cell null → `.toString()` NPE → `Report.gridTable.bql.resolveError` ([B359] §359.1,
  `BBqlGrid.java:139`). `[INFER]` (same deduced null→NPE chain as B359, same cited code).

So although the alarm DB is a *better* BQL source than a history (it is `Queryable` by design, §360.2), feeding it
through the report module's stock grid hits the **identical structural wall** — the grid wants components, the DB
yields struct records.

## §360.6 — R5 verdict: CONDITIONAL — data fully available, stock grid blocked, custom cursor required `[INFER]`

**R5 = CONDITIONAL (YES with custom code).** The alarm database exposes everything the client's marker need
requires — a queryable `openAlarms`/archive relation, a per-crossing `timestamp`, the `source` point, the
`sourceState` transition, and the `highLimit`/`lowLimit`/`presentValue` facets — behind an `operator-read` RBAC
gate (`canRead → getPermissionsForTarget().hasOperatorRead()` `[CERT]` `BAlarmDatabase.java:319`, not super-user).
What is blocked is only the *stock delivery path*: `BBqlGrid` cannot render `BStruct` records (§360.5).

The feasible path mirrors R4's ([B359] §359.5): **custom code that reads the `BITable<BAlarmRecord>` cursor
directly**, bypassing `BBqlGrid` — either a custom grid modeled on the Workbench `AlarmDbTableModel` /
`BAlarmDbView` pattern (`alarm-wb`, which already cursors the alarm DB row-by-row — note it is a *Workbench* view,
so a station-side report needs its own equivalent cursor, not a reuse), or a custom `BExportSource` /
`handleGenerate()` ([B357] §357.3) that runs the `alarm:|bql:…` query on the report worker thread (§360.2), pulls
the typed columns + the needed `alarmData` facets, and hands rows to `BGridToCsv`/`BGridToText` ([B357] §357.6).
`[INFER]`.

Combined with [B359], both data legs of the client deliverable — the history table AND the alarm markers —
require the same kind of custom cursor code; neither is served by the stock report grids.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Report module has 0 alarm-specific code (proven absent, driver-re-verified) | [CERT] | `rg -il alarm report/report-rt/decompiled` = 0 | ✅ measured |
| 2 | `BAlarmDatabase implements Queryable, BIRelational`; session ORD `alarm:` | [CERT] | `BAlarmDatabase.java:86-91,94` | ✅ read |
| 3 | `bqlQuery()` answers BQL → `BAlarmDbQueryResult`; relations `openAlarms`/`ackPendingAlarms` | [CERT] | `BAlarmDatabase.java:246,259` | ✅ read |
| 4 | Framework's own ORD form is `alarm:\|bql:select … from openAlarms where …` | [CERT] | `BAlarmDatabase.java:192` | ✅ read |
| 5 | `bqlQuery` throws if called on the engine thread; a report runs off-engine | [CERT]/[INFER] | `BAlarmDatabase.java:249` + [B357] §357.2 | ✅ read+reasoned |
| 6 | `timestamp`(BAbsTime), `source`(BOrdList), `sourceState`, `normalTime`, `priority`, `alarmClass`, `ackState/user/ackTime` are typed columns | [CERT] | `BAlarmRecord.java:74,76,77,79,80,81,82,83,84` | ✅ read |
| 7 | `highLimit`/`lowLimit`/`presentValue`/`msgText` live in the `alarmData` facet bag, via `getAlarmFacet`, not as typed columns | [CERT] | `BAlarmRecord.java:85,119,130,131,137,351,494` | ✅ read |
| 8 | `BAlarmRecord extends BStruct` → no `ordInSession` → `BBqlGrid` NPE (same wall as R4) | [CERT]/[INFER] | `BAlarmRecord.java:72-73` + [B359] §359.1-2 | ✅ read+reasoned |
| 9 | Access gate is operator-read, not super-user | [CERT] | `BAlarmDatabase.java:319` | ✅ read |
| 10 | Feasible path = custom cursor over `BITable<BAlarmRecord>` (BExportSource / AlarmDbTableModel pattern), not the stock grid | [INFER] | §360.6 from claims 1,7,8 + [B357]§357.3 | ✅ reasoned |

**Marker tally**: [CERT] ×7 · [CERT]/[INFER] ×2 · [INFER] ×1. Ratio [INFER]/[CERT] ≈ 0.11 (counting the two
mixed as CERT). Block type = **EVIDENCE**: low ratio, code-grounded. Load-bearing tokens re-resolved against disk
this iteration: claims 1-4,6,7,9 (driver-verified, not trusted from the sweep); claims 5,8 reuse [B359]'s
already-verified `BBqlGrid`/`BStruct` mechanism. Delegated sweep tier: `sonnet` (structural), driver-verified.

## Connections

- [B359] §359.1-2 — the `BBqlGrid` component-viewer wall and the `BStruct`/`ordInSession` NPE this block reuses.
- [B357] §357.2/§357.3/§357.6 — the report worker thread (satisfies the engine-thread guard), `handleGenerate()`
  extension point, and the CSV/text exporters a custom alarm source would feed.
- [B358] §358.5 — the custom-`BReportSource` and interactive-webChart escape hatches, now confirmed for alarms too.
- Alarm subsystem proper: [B8],[B44],[B54],[B142],[B240] (remittance — this block only covers query-from-report).
- Forward: R6 (chart-in-report — the third data leg: plotting PSI-vs-time with the <12/>28 bands and marking the
  §360 crossings), R9 (synthesis tying report+history+alarm+chart into the client composition).

## Gaps opened / queued

No new gaps. R5 closed (CONDITIONAL — YES with custom cursor code). Remaining investigable: R3 (export/xlsx),
R6 (chart-in-report), R7 (ux/web), R8 (wb builder), R9 (synthesis) → 5 open.
