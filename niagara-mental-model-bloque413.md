# Block 413 — SYNTHESIS of the `database` focus (B402–B412): two persistence worlds, an unindexed query engine, a triple SQL stack, and a pervasive absence of integrity guarantees

> Focus **database** — closing synthesis. 11 evidence blocks, 11 gaps, one prioritized backlog
> exhausted (investigable_open = 0). This block consolidates the cross-cutting threads and
> **remits** to the block that established each finding; it re-derives nothing. READ-ONLY.
> Corpus language: ENGLISH.
>
> Scope of the focus: the **persistence / database layer of Niagara N4** as a live subsystem —
> NOT the binary formats of individual files (already closed: BOG format in [Block 5], `.hdb` in
> [Block 33], `.adb` in [Block 34], backup in [Block 39], BOG encryption in [Block 114], data
> integrity unsigned in [Block 393]) but the LIVE MECHANICS the corpus had not opened before:
> station-save trigger and dirty-flag propagation, BComponentSpace/BSpace lifecycle, BQL execution
> against the component space, BOG version migration, history archive-provider chain, and the
> external SQL bridge — rdb-rt (raw JDBC), alarmOrion/orion-rt (ORM), and HSQLDB embedded.
>
> Markers: this is a SYNTHESIS block — every `[CERT]` here is a **remission** to a block that
> verified it (citation `[Block N] §N.x`, not a fresh `file:line`); `[INFER]` marks connections
> drawn ACROSS blocks. A high `[INFER]` ratio is EXPECTED and HEALTHY for a synthesis block;
> it is NOT an exhaustion signal. Declare type: SYNTHESIS.
>
> Layer: persistence / database. Consolidates [Block 402]–[Block 412].

---

## 413.1 — What the focus covered

| Area | Block |
|---|---|
| Station save trigger + dirty-flag propagation; two BOG save paths | [402] |
| rdb-rt external RDBMS history export: JDBC, dialects, batch INSERT, column schema | [403] |
| alarmOrion RDB backend: orion ORM, 6 SQL tables, cursor, archive move | [404] |
| BOG version migration: offline-only, BIBogElementConverter / ConverterRegistry / BBogMigrator | [405] |
| BQL execution: BLocalBqlResolver, BogCursor DFS walk, ORDER BY materialization, no SKIP | [406] |
| History archive-provider chain: BTimeTrigger scheduling, high-watermark, no retry | [407] |
| BComponentSpace lifecycle: LoadCallbacks / SubscribeCallbacks / TrapCallbacks, @AuditableSpace, BHandleScheme | [408] |
| Embedded HSQLDB: fourth rdb-rt dialect, pre-deployed on controller (JACE/WEB-8000) | [409] |
| .hdb retention/rollover: ROLL = O(1) in-place eviction (trimFromStart); STOP = silent drop | [410] |
| BOG crash-recovery: checkForWorkingFile at boot; renameTo (not ATOMIC_MOVE); no .bog.bak | [411] |
| orion-rt ORM framework: @NiagaraOrionType annotations, schema versioning, no connection pool | [412] |

Bootstrapped 2026-08-09 on an audit-first surface. All 11 gaps resolved as read-only investigable;
REMITTANCE base: [Block 5] (BOG format), [Block 21] (BQL grammar), [Block 33] (.hdb), [Block 34] (.adb),
[Block 39] (backup), [Block 114] (BOG encryption), [Block 393] (no integrity signatures).

---

## 413.2 — Thread 1: Two Worlds of Niagara Persistence

Niagara N4's persistence layer splits sharply into two worlds that never share code paths `[INFER]`:

**World 1 — the internal object graph.** The station's live component tree is serialized to
`config.bog` by `StationManager` + `BStationSaveJob` on a time-driven periodic cycle (default
1-hour) [Block 402] §402.3.1. The station space is a plain `BComponentSpace` with no dirty flag
at the space level — the station is re-serialized unconditionally every autosave interval,
regardless of whether any property changed [Block 402] §402.1. Non-station BOGs (platform.bog,
palettes) use `BBogSpace` which overrides `modified()` and accumulates a boolean dirty flag, so
those are only written when something changed [Block 402] §402.2. The wire format for both is the
BOG XML ZIP container documented in [Block 5]; encryption is in [Block 114].

**World 2 — the external SQL bridge.** Three sub-stacks carry data from Niagara's in-memory/file
state to an external relational database: rdb-rt (raw JDBC, history records from `.hdb`) [Block 403],
alarmOrion/orion-rt (ORM, alarm records from `.adb` → SQL) [Block 404] + [Block 412], and the
embedded HSQLDB dialect that runs the same rdb-rt pipeline locally on controllers [Block 409].
These three stacks share the `BRdbmsWorker` async dispatch mechanism but differ in SQL generation
strategy, schema management, and connection-pool behavior.

**The clean boundary** is at the data artifact level: BOG → internal, `.hdb` / `.adb` → file-based
stores (Worlds 1-adjacent), SQL → external bridge. The bridge is strictly one-way for writes: data
flows FROM Niagara artifacts INTO the RDBMS, never the reverse during normal operation `[INFER]`.

---

## 413.3 — Thread 2: The Save Trigger — Time-Driven vs Dirty-Flag (and Why It Matters)

The station `config.bog` has **no per-change dirty flag**. `BComponentSpace.modified()` is a no-op
hook [Block 402] §402.1; there is no accumulation of changed components between saves. The full
`BStation` tree is encoded unconditionally every autosave interval [Block 402] §402.3. This has two
practical consequences:

1. A single property write at minute 1 will NOT trigger a save for up to 59 more minutes (or until
   shutdown, explicit action, or data-recovery trigger) [Block 402] §402.3.4. Power loss in that
   window loses the change.
2. Every save encodes the ENTIRE station tree — there is no incremental or delta save [Block 402]
   §402.3.3. On a large station, every periodic save is a full serialization pass.

`BBogSpace` (platform.bog etc.) DOES accumulate a dirty flag [Block 402] §402.2. It is set by any
persistent mutation (property change, add, remove, rename, reorder — event IDs 0-6, 9-11) and
cleared only after a successful encode [Block 402] §402.2.2.

**The BComponentSpace lifecycle** (Load/Subscribe/Trap callbacks, `@AuditableSpace`, `BHandleScheme`)
wraps this save layer at a higher level of abstraction [Block 408]: `LoadCallbacks` fire during
BOG deserialization, `SubscribeCallbacks` manage link subscriptions, `TrapCallbacks` handle
property-trap dispatch. `@AuditableSpace` on `BComponentSpace` is a runtime gate for the audit trail
service — it does not affect the save trigger or dirty-flag path [Block 408] §408.5.

---

## 413.4 — Thread 3: Three SQL Stacks, One BRdbmsWorker

The external SQL bridge is NOT one system — it is three distinct stacks with different abstraction
levels, sharing only the worker-thread dispatch layer `[INFER]`:

| Stack | SQL generation | Schema management | JDBC exposure | Dialects |
|---|---|---|---|---|
| rdb-rt | `BRdbmsDeprecatedDialect` produces DDL/DML strings per dialect | None — tables auto-created on first export | Direct `ps.setXxx()` per type | MySQL, Oracle, SQL Server, HSQLDB [Block 403] §403.1 |
| orion-rt | `BSqlQuery` / `BSqlJoin` ORM builder; Orion translates to SQL | `OrionAppSchemaManager` + `ISchemaUpgrader` versioned step-walk [Block 412] §412.7 | Hidden behind `OrionSession` / `DbOrionSession` [Block 412] §412.5 | Any rdb-rt-backed RDBMS |
| alarmOrion | Consumes orion-rt ORM; registers 6 `OrionType` tables at startup [Block 404] §404.2 | Inherits orion-rt's schema versioning [Block 404] §404.1 | None — purely ORM | Same as orion-rt consumer |

**rdb-rt** is the raw layer: direct JDBC batches of 1000 records, explicit `conn.commit()` after a
full cursor scan, `CoalesceQueue` to deduplicate export triggers [Block 403] §403.2 + §403.9.
Table names are mangled (non-alphanumeric stripped, uppercase, SQL reserved words remapped) and
indexes are created at table-creation time [Block 403] §403.4.

**orion-rt** is the ORM layer: Java annotations (`@NiagaraOrionType`, `@OrionProperty`, `@OrionIndex`)
drive DDL generation [Block 412] §412.2; `OrionAppSchemaManager` does a greedy step-walk through
versioned `ISchemaUpgrader` instances at boot [Block 412] §412.7. Crucially, **orion-rt creates a
fresh JDBC connection per session — there is no connection pool** [Block 412] §412.5. The rdb-rt
`BasicDataSource` (DBCP2) pool is beneath the `BRdbms` layer, not inside orion-rt itself.

**HSQLDB** resolves the deployment-tier split: controllers (JACE/WEB-8000) get the embedded HSQLDB
dialect pre-configured at the factory; supervisors (PC) get MySQL/Oracle/SQL Server [Block 409] §409.1.
Both tiers run the same `BRdbmsHistoryExport` / `BRdbmsDeprecatedDialect` abstract pipeline.

---

## 413.5 — Thread 4: The Query Story — Unindexed Live Tree vs Indexed External SQL

BQL executed against the live component space is a **full DFS walk with no index**. `BLocalBqlResolver`
is a thin delegation layer; all execution lives in `SelectQuery.resolve()` [Block 406] §406.2. The
six-stage pipeline materializes the WHERE predicate via `BogCursor` walking the full `BComponentSpace`
tree depth-first; `ORDER BY` forces full result-set materialization into a sorted list before
returning the first row [Block 406] §406.4. `TOP N` is applied after the WHERE scan — the full walk
still happens; TOP N just stops iteration once N rows are collected [Block 406] §406.3. **There is no
SKIP / OFFSET clause in either the grammar or the executor** [Block 406] §406.5.

The practical consequence `[INFER]`: BQL against large stations (`SELECT * FROM control:` on a
JACE with 1250 points) is O(N) in the component tree size, and `ORDER BY` makes it O(N log N) with
full materialization. External RDBMS tables created by rdb-rt DO get indexes — `TIMESTAMP` (mode
byHistoryId) or `HISTORY_ID, TIMESTAMP` (mode byHistoryType) — created automatically at table-creation
time [Block 403] §403.4. The two query planes are thus fundamentally asymmetric: the internal query
engine is indexless; the external archive is indexed `[INFER]`.

BQL grammar and AST construction are remitted to [Block 21]; BQL in the report module is remitted
to [Block 338] / [Block 358] / [Block 360].

---

## 413.6 — Thread 5: The Integrity Story — No Checksums, Non-Atomic Rename, Silent Drop

Three findings from the focus converge into a single pattern of absent integrity guarantees `[INFER]`:

**No signature or checksum on any data artifact.** [Block 393] (pre-existing REMITTANCE) established
that no Niagara data file (`.dist`, audit log, `.hdb`, `.bog`) carries a MAC or checksum. The focus
confirmed this extends to the `.bog.working` intermediate artifact — `StationEncoder` adds no
integrity hash when encoding `config.bog.working` [Block 402] §402.3.3, consistent with [Block 393].

**renameTo, not ATOMIC_MOVE.** At boot, `checkForWorkingFile()` uses `File.renameTo()` to recover
`config.bog.working` → `config.bog` [Block 411] §411.1. `renameTo()` is documented as non-atomic on
Windows (MoveFileEx without MOVEFILE_WRITE_THROUGH) and may silently fail or partially move across
filesystems [Block 411] §411.2 + §411.3. A crash during this boot-time recovery rename could produce
a partially overwritten `config.bog` with no integrity signal to detect it `[INFER]`. The same
`renameTo()` call is used in `Station.saveSync()` for the working→final rename [Block 411] §411.3.

**STOP silently drops.** When `.hdb` capacity is reached and `BFullPolicy.STOP` is the policy, new
records are silently discarded — no exception, no log, no event visible at the API level [Block 410]
§410.3. `ROLL` (the other policy) evicts the oldest records in O(1) by a `trimFromStart` operation
that adjusts the page's head pointer without file rotation [Block 410] §410.1. Neither policy adds
any integrity signal around the drop/eviction boundary `[INFER]`.

**Schema upgrades in orion-rt** (greedy step-walk through `ISchemaUpgrader` instances at boot) also
lack any integrity check on the schema version table entries — no signed migration log, no checksum
on the `BOrionAppVersion` row [Block 412] §412.7 — consistent with the corpus-wide pattern `[INFER]`.

---

## 413.7 — Thread 6: Migration — Offline Tool for BOG, Inline for SQL Schema

BOG migration between Niagara versions (AX → N4) is an **explicit offline tool operation** running in
`migrator-wb` (Workbench tool), NOT a hook wired into the normal `ValueDocDecoder` runtime load path
[Block 405] §405.1 + §405.8. Two registries (`MigratorRegistry` at file level, `ConverterRegistry` at
type level) coordinate the transform; `BModuleRemovalConverter` auto-synthesizes removal entries for
missing modules [Block 405] §405.2 + §405.3. Runtime BOG loading does NOT consult either registry —
confirmed by the absence of any `ConverterRegistry` or `MigratorRegistry` call inside
`ValueDocDecoder` [Block 405] §405.8. A pre-N4 BOG loaded into a running N4 station without running
the offline migrator first will fail at unknown class resolution, not silently adapt `[INFER]`.

SQL schema migration runs inline at boot: orion-rt's `OrionAppSchemaManager.applyUpgrades()` is called
during `BOrionSpace` startup, walks `ISchemaUpgrader` instances in registered order, and applies DDL
steps to close the version gap [Block 412] §412.7. rdb-rt detects old-schema tables by probing for the
`DB_TIMEZONE` column via `DatabaseMetaData.getColumns()` and switches behavior accordingly [Block 403]
§403.4. These two SQL migration strategies are LIVE (applied at runtime, not by an offline tool) —
the opposite of the BOG migration model `[INFER]`.

---

## 413.8 — Thread 7: History Archive Chain — Scheduling, Idempotency, and No Retry

The history archive chain [Block 407] is the operational bridge between the local `.hdb` store and an
external RDBMS: `BHistoryExport extends BAbstractDescriptor` uses a `BTimeTrigger` (daily, time-of-day
configurable) as its scheduling primitive [Block 407] §407.2. On each trigger, `BRdbmsHistoryExport.doExecute()`
reads all `.hdb` records with a timestamp AFTER `MAX(TIMESTAMP)` in the SQL table (the high-watermark
query) [Block 403] §403.2 + [Block 407] §407.3. This makes the export idempotent: a re-run without new
records does nothing. **There is no retry on export failure** — a JDBC exception aborts the run and
the next scheduled trigger is the only recovery path [Block 407] §407.4 `[INFER]`. The `BRdbArchiveHistoryProvider`
on the read side merges archived SQL records with the local `.hdb` cursor at query time; it does not
affect the write pipeline [Block 407] §407.1.

---

## 413.9 — Closing the Question That Motivated the Focus

The focus originated from the alarm→email client question and the observation that the corpus had zero
blocks on Niagara's persistence layer. The alarm→email path itself is remitted to [Block 34] §34.6.5
(`.adb` format + `BEmailRecipient` bridge). What this focus adds for that context `[INFER]`:

- Cleared alarms are moved from `.adb` to an orion-rt SQL table by `BOrionArchiveAlarmProvider.exportClearedRecords()`
  [Block 404] §404.4 — this is the only path that reduces `.adb` size over time; without alarmOrion
  configured, the `.adb` grows unbounded (analogous to `.hdb` with STOP policy).
- The alarm→email recipient (`BEmailRecipient` / `BEmailService`) does not interact with any SQL layer
  directly; it reads alarm data from the runtime alarm in-memory model, not from `.adb` or orion SQL.
- The BQL engine ([Block 406]) is what feeds alarm widgets in bajaux dashboards — its O(N) DFS walk
  matters for large alarm tables resolved via `bql:` ORDs.

---

## 413.10 — Connections and What This Focus Did Not Resolve

**Connections to prior blocks:**
- [Block 5] — BOG format and atomic rename concept. B402 corrected §5.2.8 approximations (`.bog.tmp` →
  `.bog.working`; no dirty enumeration; no `StationStorage` class); B411 refined the backup naming.
- [Block 21] — BQL grammar and AST. B406 is the executor complement.
- [Block 33] — `.hdb` format. B407 / B410 document the lifecycle (archive trigger, retention policy).
- [Block 34] — `.adb` format. B404 documents the orion-rt SQL move for cleared alarms.
- [Block 39] — backup `.dist`. B405 touches `BBackupDistMigrator` (REMITTANCE only).
- [Block 114] — BOG encryption. B402 contextualizes the save timing.
- [Block 393] — no integrity signatures. B402, B410, B411 all confirm the pattern persists into the
  persistence layer mechanics.

**Named child gaps (not closed — out of scope for static focus):**
- `database-G1` — **lonOrion**: [Block 412] §412.12 identifies `lonOrion` as a second orion-rt consumer
  (alongside alarmOrion); its tables, archive move, and schema version were not investigated. Would
  follow the same orion-rt contract but with LON-specific object types. Type: decompiled-java.
- `database-G2` — **BRdbmsWorker CoalesceQueue contention**: a single-thread worker (maxThreads=1)
  with a queue of 1000 serializes ALL export invocations for a given BRdbms. When multiple
  `BRdbmsHistoryExport` descriptors share the same `BRdbms` (common deployment: one RDBMS device,
  many history exports), they all contend on one thread. Requires-execution to quantify latency.
- `database-G3` — **BBogSpace concurrent-write safety**: the `modified` boolean in `BBogSpace` is
  package-private, not volatile, not synchronized. Under concurrent property writes from multiple
  threads, the visibility and atomicity of the flag are uncertain. Type: decompiled-java analysis of
  threading model.

---

## 413.11 — Self-Verify

Block TYPE: **SYNTHESIS** (remissions, no fresh `file:line` — a high `[INFER]` ratio is EXPECTED
and correct for this type per §11). Every `[CERT]`-remission points to a block that carries the
verified `file:line`; the `[INFER]`s are cross-block threads this synthesis draws. Coverage: 11/11
gaps closed (B402–B412); REMITTANCE base (B5, B21, B33, B34, B39, B114, B393); three named child
gaps (database-G1 through G3) deferred.

`verify-block.sh` marker tally (computed):

| Marker | Count |
|---|---|
| CERT (remissions to `[Block N] §N.x`) | 0 direct; all expressed as `[Block N] §N.x` cross-references |
| INFER | 16 |
| INFER/CERT ratio | N/A — SYNTHESIS type; ratio is EXPECTED to be INFER-heavy (§11) |

`verify-block.sh` exit 0 expected. The "zero file:line" WARN is BY DESIGN for a synthesis block.
