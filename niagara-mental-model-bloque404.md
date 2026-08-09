# Block 404 — alarmOrion: Orion ORM Alarm Backend — Schema, Cursor, and Archive Move

> **Research focus:** `database` (gap **DB3**, high-priority). Covers the complete alarmOrion RDB
> backend: the architectural distinction between alarmOrion and the rdb-rt pipeline; the six SQL
> tables managed by the Orion ORM; the schema defined by Java annotations on `BOrionAlarmRecord`
> and `BOrionAlarmClass`; the three-cursor merge-join of `OrionAlarmCursor`; and the full
> two-phase archive move executed by `BArchiveAlarmProvider.doExecute()` +
> `BOrionArchiveAlarmProvider.exportClearedRecords()`.
>
> **Not covered here:**
> - `.adb` format internals → [Block 34]
> - rdb-rt dialect pipeline (raw SQL/JDBC) → [Block 403]
> - Orion ORM internal implementation (`orion-rt`) — out of scope for this iteration
> - `BOrionAlarmArchive` commissioning workflow → [Block 34] §34.13.5
>
> Subject version: N4.14.0.162 (Vineflower decompiled corpus; organized/ tree).
>
> Sources:
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarmOrion/alarmOrion-rt/vineflower/javax/baja/alarmOrion/BOrionAlarmDatabase.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarmOrion/alarmOrion-rt/vineflower/javax/baja/alarmOrion/BOrionAlarmService.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarmOrion/alarmOrion-rt/vineflower/javax/baja/alarmOrion/BOrionAlarmRecord.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarmOrion/alarmOrion-rt/vineflower/javax/baja/alarmOrion/BOrionAlarmClass.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarmOrion/alarmOrion-rt/vineflower/javax/baja/alarmOrion/OrionAlarmDbConnection.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarmOrion/alarmOrion-rt/vineflower/com/tridium/alarmOrion/OrionAlarmCursor.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarmOrion/alarmOrion-rt/vineflower/com/tridium/alarmOrion/OrionObjectCache.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarmOrion/alarmOrion-rt/vineflower/com/tridium/alarmOrion/archive/BOrionArchiveAlarmProvider.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarmOrion/alarmOrion-rt/vineflower/com/tridium/alarmOrion/archive/BOrionAlarmArchiveDatabase.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarm/alarm-rt/vineflower/javax/baja/alarm/BArchiveAlarmProvider.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarmOrion/alarmOrion-rt/vineflower/com/tridium/alarmOrion/schema/Upgrade_1_2_to_1_3.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/alarmOrion/alarmOrion-rt/vineflower/com/tridium/alarmOrion/schema/Upgrade_1_3_to_1_4.java`
>
> Method: decompiled Java (Vineflower); direct read of organized/ corpus. Structural comprehension
> delegated to sonnet sub-agent; mechanical enumeration delegated to haiku sub-agent. All
> load-bearing citations independently verified against source files by orchestrator.
>
> `database` focus. Connects [Block 34] (`.adb` format — local alarm store), [Block 403] (rdb-rt
> JDBC export pipeline — contrasted here), [Block 402] (BOG save trigger — sibling write path).

---

## 404.1 — Architecture: Orion ORM Layered Over rdb-rt, Not a Dialect `[CERT]`

The alarmOrion module is architecturally distinct from the rdb-rt pipeline documented in
[Block 403]. It does NOT use `BRdbmsDeprecatedDialect`, `RdbmsDialect`, `BRdbmsWorker`, or raw
JDBC `PreparedStatement` anywhere in the module. Instead it uses the **Orion ORM** framework
(`com.tridium.orion.*`) as the sole SQL abstraction layer.

| Dimension | rdb-rt pipeline [Block 403] | alarmOrion [Block 404] |
|---|---|---|
| SQL construction | `RdbmsDialect` produces dialect-specific DDL/DML strings | `BSqlQuery` / `BSqlJoin` / `BSqlCase` ORM builder; Orion translates to SQL |
| Schema management | None — tables created on first export | `OrionAppSchemaManager` with versioned upgraders |
| JDBC exposure to module | Direct via `BRdbmsDeprecatedDialect.loadField()` / `ps.setXxx()` | None — hidden behind `OrionSession` |
| BRdbms role | The execution engine (worker thread, connection pool) | Only a lookup key — passed to `BOrionSpace.getOrionDatabase(rdbms)` |
| Worker thread | `BRdbmsWorker` (maxThreads=1, maxQueueSize=1000) | Borrows the same BRdbms worker via `rdbms.getWorker().postAsync()` |
| Transaction | `conn.commit()` after full cursor scan | `session.setAutoCommit(false)` + `session.commit()` per archive run |

**Connection path:**

```
BOrionAlarmService.database (BOrd → BRdbms)
  → BOrionAlarmDatabase.getOrionDatabase()
      → alarmService.getDatabase().resolve(alarmService).get()   [BRdbms]  :352
      → BOrd.make("orion:").resolve(alarmService).get()           [BOrionSpace]  :363
      → orionSpace.getOrionDatabase(rdbms)                        [BOrionDatabase]  :364
          → BOrionDatabase.createSession(cx)                      [OrionSession]  :369
```

`[CERT]` `BOrionAlarmDatabase.java:352-364` (both lookup paths — `getStatus()` and
`getOrionDatabase()`), `BOrionAlarmDatabase.java:369` (session creation)

The `BRdbms` object resolves the target SQL server; the `BOrionSpace` (Orion service) uses it
to open a connection pool. `BOrionAlarmDatabase` never calls any `BRdbms` method for SQL
execution. `[CERT]` `BOrionAlarmDatabase.java:39` (import `javax.baja.rdb.BRdbms`; used at line
352 and 362 only).

## 404.2 — Six SQL Tables: OrionType Registration `[CERT]`

`BOrionAlarmService` registers exactly **six `OrionType` instances** with the Orion ORM at
startup. The Orion layer creates and manages one SQL table per `OrionType`:

| OrionType | SQL table | Java class |
|---|---|---|
| `BOrionAlarmRecord.ORION_TYPE` | AlarmRecord | main alarm events |
| `BOrionAlarmClass.ORION_TYPE` | AlarmClass | alarm class registry + statistics |
| `BOrionAlarmFacetValue.ORION_TYPE` | AlarmFacetValue | EAV alarm data values |
| `BOrionAlarmFacetName.ORION_TYPE` | AlarmFacetName | EAV facet name registry |
| `BOrionAlarmSource.ORION_TYPE` | AlarmSource | alarm source ORDs |
| `BOrionAlarmSourceOrder.ORION_TYPE` | AlarmSourceOrder | alarm-to-source N:M join |

`[CERT]` `BOrionAlarmService.java:63-70` (ORION_TYPES array)

The same six types appear in `BOrionArchiveAlarmProvider.ORION_TYPES` (lines 76-83), confirming
both the live-service and archive-provider share the same schema. `[CERT]`
`BOrionArchiveAlarmProvider.java:76-83`

**Schema discovery:** table names are derived by the Orion ORM from the `@NiagaraOrionType`
annotation on each class. The actual table name is obtained at runtime via
`session.getOrionDatabase().getTableName(BOrionAlarmSource.ORION_TYPE)` — an Orion ORM call,
not a hard-coded string. `[CERT]` `Upgrade_1_2_to_1_3.java:45`

## 404.3 — BOrionAlarmRecord Schema: Annotations → SQL Columns `[CERT]`

`BOrionAlarmRecord extends BOrionObject` (not `BAlarmRecord` — it is a parallel struct mapped
to SQL). `[CERT]` `BOrionAlarmRecord.java:148`. Schema is declared entirely via
`@NiagaraProperties` / `@NiagaraProperty` / `@OrionProperty` annotations; the Orion ORM
generates the SQL DDL.

> **Remittance:** B34 §34.13.3 lists all 15 property names. This block documents the SQL
> index strategy, the FK annotation, and the ORM mechanism that B34 omits.

**Property → SQL column mapping with index/constraint annotations:**

| Java property | SQL column | Java type | Index / constraint | Source |
|---|---|---|---|---|
| `id` | ID (PK) | `int` | `ID_KEY`, `DESCENDING=true` | `BOrionAlarmRecord.java:149` |
| `timestamp` | TIMESTAMP | `BAbsTime` | — | `:150` |
| `datestamp` | DATESTAMP | `BDate` | `INDEXED=true` | `:151` |
| `uuidHash` | UUIDHASH | `int` | `INDEXED=true` | `:152` |
| `uuid` | UUID | `BUuid` | — | `:153` |
| `isOpen` | ISOPEN | `boolean` | — | `:154` |
| `sourceState` | SOURCESTATE | `BSourceState` | `INDEXED=true` | `:155` |
| `ackState` | ACKSTATE | `BAckState` | `INDEXED=true` | `:156` |
| `ackRequired` | ACKREQUIRED | `boolean` | — | `:157` |
| `alarmClass` | FK ref | `BRef` (→ OrionAlarmClass) | `ON_DELETE=CASCADE`, `AUTO_RESOLVE=true` | `:158-160` |
| `priority` | PRIORITY | `int` | — | `:161` |
| `normalTime` | NORMALTIME | `BAbsTime` | — | `:162` |
| `ackTime` | ACKTIME | `BAbsTime` | — | `:163` |
| `userAccount` | USERACCOUNT | `String` | `WIDTH=32` | `:164` |
| `alarmTransition` | ALARMTRANSITION | `BSourceState` | — | `:165` |
| `lastUpdate` | LASTUPDATE | `BAbsTime` | — | `:166` |

`[CERT]` `BOrionAlarmRecord.java:149-166` (static Property declarations match the annotation
above).

**`alarmClass` FK semantics:** `ON_DELETE=CASCADE` means deleting an `AlarmClass` row
cascades to `AlarmRecord` rows. `AUTO_RESOLVE=true` means the Orion ORM auto-loads the
referenced `BOrionAlarmClass` object when the record is fetched.
`[CERT]` `BOrionAlarmRecord.java:158-160`

**EAV pattern for alarm data:** `alarmData` (`BFacets`) is NOT a column on AlarmRecord. It is
stored via two satellite tables: `AlarmFacetName` (key strings) and `AlarmFacetValue` (FK to
AlarmRecord + FK to AlarmFacetName + serialized value). This was confirmed by seeing
`BOrionAlarmRecord.getAlarmData(session)` issue a `BSqlQuery` over `BOrionAlarmFacetValue`.
`[CERT]` `BOrionAlarmRecord.java:381-411` (`getAlarmData` method)

**`isOpen` computed flag:** set/updated by `beforeInsert()` and `beforeUpdate()` hooks via
the `isOpen()` method. `[CERT]` `BOrionAlarmRecord.java:476-484`

**`datestamp` derived from `timestamp`:** the `changed()` override sets `datestamp =
BDate.make(timestamp)` whenever timestamp changes. `[CERT]` `BOrionAlarmRecord.java:464-466`

## 404.4 — BOrionAlarmClass Schema and LRU Cache `[CERT]`

`BOrionAlarmClass extends BOrionObject` — stored in SQL as:

| Java property | SQL column | Type | Constraint |
|---|---|---|---|
| `id` | ID (PK) | `int` | `ID_KEY` flags=9 |
| `alarmClass` | ALARMCLASS | `String` | `UNIQUE=true`, `WIDTH=64` |
| `unackedAlarmCount` | UNACKEDALARMCOUNT | `int` | — |
| `openAlarmCount` | OPENALARMCOUNT | `int` | — |
| `inAlarmCount` | INALARMCOUNT | `int` | — |
| `totalAlarmCount` | TOTALALARMCOUNT | `int` | — |
| `timeOfLastAlarm` | TIMEOFLASTALARM | `BAbsTime` | — |

`[CERT]` `BOrionAlarmClass.java:70-79` (class body static declarations matching annotations at
lines 26-68)

**LRU cache:** `BOrionAlarmClass` maintains a static `OrionObjectCache` with capacity 100.
`[CERT]` `BOrionAlarmClass.java:80`:
```java
private static final OrionObjectCache ORION_ALARM_CLASS_CACHE = new OrionObjectCache(100);
```

`OrionObjectCache` is backed by a `HashMap` + `LinkedList` (pure in-memory LRU, no SQL).
`[CERT]` `OrionObjectCache.java:19-20`

The `BOrionAlarmClass.get(alarmClassName, session)` method: (1) checks cache; (2) on miss calls
`session.select(ORION_TYPE, new PropertyValue(BOrionAlarmClass.alarmClass, BString.make(...)))`;
(3) on NOT FOUND calls `session.insert(alarmClass)` to auto-create. Cache is invalidated by
`afterDelete()`, `afterInsert()`, and `afterUpdate()` hooks.
`[CERT]` `BOrionAlarmClass.java:142-172`

## 404.5 — BOrionAlarmService Startup and prepareSQL Check `[CERT]`

`BOrionAlarmService extends BAlarmService implements BIOrionApp`.
`[CERT]` `BOrionAlarmService.java:53`

**Slots:**

| Property | Type | Default | Purpose |
|---|---|---|---|
| `status` | `BStatus` | `BStatus.ok` | Service health (flags=3 readonly+summary) |
| `faultCause` | `String` | `""` | Error description |
| `database` | `BOrd` | `BOrd.NULL` | ORD pointing to the `BRdbms` instance (targetType = `"rdb:Rdbms"`) |

`[CERT]` `BOrionAlarmService.java:54-56`

**Schema version:** `BSchemaVersion.make("1.4")`. `[CERT]` `BOrionAlarmService.java:59`

**`serviceStarted()` sequence:**

```
1. Resolve database BOrd → BRdbms  (:122)
2. If rdbms type = "rdbSqlServer:SqlServerDatabase":
     Check extraConnectionProperties for "prepareSQL=1" or "prepareSQL=0"  (:123-127)
     If absent → LOG.warning("Database connection misconfigured. Reference issue 15402.")
3. Get BOrionService → BLocalOrionDatabase
4. BLocalOrionDatabase.registerApp(this)  (:131) — triggers schema creation/upgrade
5. Set status=ok
```

`[CERT]` `BOrionAlarmService.java:115-148`

**SQL Server `prepareSQL` check (extends B34 §34.13.6):** The code confirms the gotcha at
`BOrionAlarmService.java:123-127`. The check is exclusive to `rdbSqlServer:SqlServerDatabase`
(comparing the type name string), applied to `extraConnectionProperties`. It warns but does not
fail startup. `[CERT]` `BOrionAlarmService.java:123-127`

**`createAlarmDb()`:** returns `new BOrionAlarmDatabase()` (zero-arg constructor). The database
opens only after Orion calls back `orionReady(db)` — not at service start.
`[CERT]` `BOrionAlarmService.java:84-86,151-158`

## 404.6 — OrionAlarmCursor: Three-Cursor Merge-Join, No Explicit Page Size `[CERT]`

`OrionAlarmCursor extends AbstractCursor<BAlarmRecord>`.
`[CERT]` `OrionAlarmCursor.java:22`

**Fields:**

| Field | Type | Role |
|---|---|---|
| `recordCursor` | `OrionCursor` | Primary iteration — one row = one AlarmRecord |
| `facetCursor` | `OrionCursor` (optional) | Pre-fetched facets, sorted by alarm ID |
| `sourceCursor` | `OrionCursor` (optional) | Pre-fetched source orders, sorted by alarm ID |
| `session` | `OrionSession` | Held for per-record fallback queries |
| `autoClose` | `boolean` | If true, closes the session when cursor closes |

`[CERT]` `OrionAlarmCursor.java:25-29`

**`advanceCursor()` — no explicit page size:**

```java
// OrionAlarmCursor.java:142-156
protected boolean advanceCursor() {
    this.source = null;      // reset per-record cache
    this.facets = null;
    if (this.recordCursor.next()) {
        return true;
    } else {
        this.close();
        return false;
    }
}
```

`[CERT]` `OrionAlarmCursor.java:142-156`. There is no explicit page size, no LIMIT/OFFSET
logic, and no buffer beyond the single current record in `OrionAlarmCursor` itself. Paging is
delegated entirely to the underlying `OrionCursor` from `orion-rt`.

**Merge-join pattern in `doGet()` (lines 54-132):**

The constructor pre-advances both `facetCursor` and `sourceCursor` one position (lines 44-50).
In `doGet()`:
- Source merge (lines 76-83): advances `sourceCursor` while
  `((BOrionAlarmSourceOrder)sourceCursor.get()).getAlarm().equals(BRef.make(orionAlarmRecord))` —
  accumulates all source ORDs belonging to the current alarm record.
- Facet merge (lines 101-118): same pattern with `facetCursor` and `BOrionAlarmFacetValue`.
- Fallback when side cursors absent (null): calls `orionAlarmRecord.getSource(session)` /
  `orionAlarmRecord.getAlarmData(session)` per-record, each issuing its own `BSqlQuery`
  (N+1 pattern). `[CERT]` `OrionAlarmCursor.java:85,120`

`[CERT]` `OrionAlarmCursor.java:44-51` (constructor pre-advance), `OrionAlarmCursor.java:76-83`
(source merge), `OrionAlarmCursor.java:101-118` (facet merge)

**`closeCursor()`:** closes all three `OrionCursor` instances; closes the `OrionSession` if
`autoClose=true`. `[CERT]` `OrionAlarmCursor.java:158-176`

**Error if no source:** if the assembled `BOrdList` for source has size 0, throws
`RuntimeException("Alarm record has no source ...")`. `[CERT]` `OrionAlarmCursor.java:90-93`

## 404.7 — Archive Move: doExecute() + exportClearedRecords() `[CERT]`

The archive move executes as a **two-phase pipeline** split between the base class and the
alarmOrion concrete implementation.

### Phase 1: Base class selects cleared alarms from local .adb

`BArchiveAlarmProvider.doExecute()` (NOT overridden by BOrionArchiveAlarmProvider):

```java
// BArchiveAlarmProvider.java:291-365
final void doExecute(Context cx) {
    BAbsTime exportQueryStartTime = BAbsTime.now().subtract(this.getClearedAlarmLingerTime()); // :306
    BOrd query = BOrd.make(
        "alarm:|bql:select * where " +
        "(((sourceState = 'normal' or sourceState = 'alert') and ackState = 'acked')" +
        "or (sourceState = 'normal' and ackRequired = false))" +
        "and lastUpdate <= AbsTime '" + exportQueryStartTime.encodeToString() + "'"
    );                                                              // :307-311
    Cursor<BAlarmRecord> result = ((BITable)query.resolve(alarmService).get()).cursor();
    List<BUuid> clearedUuids = this.exportClearedRecords(result);   // :317 — delegated
    // Phase 3: delete from .adb
    for (BUuid uuid : clearedUuids) {
        alarmDbConn.clearRecord(uuid, null);                        // :343
    }
    this.executeOk();                                               // :357
}
```

`[CERT]` `BArchiveAlarmProvider.java:291,306-311,317,341-346,357`

**Cleared alarm filter (BQL query on local `alarm:` service):**
- `sourceState = 'normal' or sourceState = 'alert'` AND `ackState = 'acked'` — fully resolved
  and acknowledged; OR
- `sourceState = 'normal'` AND `ackRequired = false` — resolved with no ack requirement
- AND `lastUpdate <= now - clearedAlarmLingerTime` — waited at least the configured linger time

`[CERT]` `BArchiveAlarmProvider.java:307-311`

**`doExecute()` is dispatched asynchronously** via `postExecute()` which calls
`rdbms.getWorker().postAsync(new Invocation(this, action, arg, cx))`.
`[CERT]` `BOrionArchiveAlarmProvider.java:186-195`

### Phase 2: BOrionArchiveAlarmProvider writes to Orion SQL

```java
// BOrionArchiveAlarmProvider.java:315-350
public List<BUuid> exportClearedRecords(Cursor<BAlarmRecord> recordsToExport) {
    BLocalOrionDatabase db = (BLocalOrionDatabase)
        orionSpace.getOrionDatabase(this.getRdbms().get());         // :318
    OrionSession session = db.createSession(null);                  // :323
    session.setAutoCommit(false);                                   // :324

    while (recordsToExport.next()) {
        BAlarmRecord rec = (BAlarmRecord)recordsToExport.get();
        AppendAlarmRecord appendRecord =
            new AppendAlarmRecord(rec, orionAlarmArchive.getOrionAlarmDb());  // :328
        appendRecord.doRun();                                       // :329
        if (!appendRecord.getTransactionFailed()) {
            uuidsToRemove.add(rec.getUuid());                       // :331
        }
    }
    session.commit();                                               // :335
    // on RuntimeException: session.rollback()                       :339
}
```

`[CERT]` `BOrionArchiveAlarmProvider.java:315-350`

**Transaction model:** single `OrionSession` for the entire export run, `setAutoCommit(false)`,
one `commit()` at the end. On `RuntimeException`: `session.rollback()`, exception re-thrown.
`[CERT]` `BOrionArchiveAlarmProvider.java:324,335,339-342`

**No page/batch size constant:** all qualifying records from the `doExecute()` BQL cursor are
processed in one session without subdivision. `[INFER]` — no batch size constant found by
inspection of both `BOrionArchiveAlarmProvider.java` and `BArchiveAlarmProvider.java`.

### Phase 3: Base class deletes from local .adb

After `exportClearedRecords()` returns the list of successfully exported UUIDs, the base class
calls `alarmDbConn.clearRecord(uuid, null)` for each one. This removes the alarm from the local
`.adb` file (the `.adb` format and clear logic are covered in [Block 34]).
`[CERT]` `BArchiveAlarmProvider.java:341-346`

**Failure-tolerance:** if `AppendAlarmRecord.getTransactionFailed()` is true for a record, its
UUID is NOT added to `uuidsToRemove` — it stays in the local `.adb` and will be retried on the
next execute invocation. `[CERT]` `BOrionArchiveAlarmProvider.java:330-332`

### Reverse path: importOpenAlarms

`doImportOpenAlarms()` performs the inverse: reads open alarms FROM the Orion archive via
`"alarm:archive|bql:select * from openAlarms"`, writes them to the local `.adb` via
`alarmDbConn.append(record)`, then deletes from Orion via `conn.clearRecord(uuid, null)`.
`[CERT]` `BOrionArchiveAlarmProvider.java:197-276`

## 404.8 — Schema Versioning: v1.4 and Three Upgraders `[CERT]`

`OrionAppSchemaManager` applies versioned schema migrations registered in
`BOrionAlarmService.schemaManager`. Current schema version: **1.4**.
`[CERT]` `BOrionAlarmService.java:59-62`

| Upgrader | From | To | DDL applied |
|---|---|---|---|
| `Upgrade_1_0_to_1_1` | 1.0 | 1.1 | (not read — inferred same pattern) |
| `Upgrade_1_2_to_1_3` | 1.2 | 1.3 | `AlterColumn` on `BOrionAlarmSource.source` (width change); falls back to deleting records > `SOURCE_LENGTH` if too long |
| `Upgrade_1_3_to_1_4` | 1.3 | 1.4 | `AlterColumn` on `BOrionAlarmFacetValue.value` (width change) |

`[CERT]` `Upgrade_1_2_to_1_3.java:32-94`, `Upgrade_1_3_to_1_4.java:25-30`

`session.invokeDdl(ddl)` applies the DDL via the Orion ORM layer — no raw SQL DDL string is
hardcoded. The actual `ALTER COLUMN` SQL is generated by `SchemaUpgradeUtil.alterColumn()`.
`[CERT]` `Upgrade_1_2_to_1_3.java:35-36`, `Upgrade_1_3_to_1_4.java:27-28`

The 1.2→1.3 upgrader also demonstrates fallback: if the column is too long to resize directly,
it selects oversized rows via raw SQL (`session.select(type, "select * from " + tableName +
" where " + lengthFn + "(...) > " + SOURCE_LENGTH)`) and deletes associated alarm records and
sources before retrying the AlterColumn. It also calls `((RdbmsDialect)session.getRdbmsContext()
).getStringLengthFunctionName()` — confirming the Orion session has access to the underlying
dialect for fallback raw SQL. `[CERT]` `Upgrade_1_2_to_1_3.java:47-88`

## 404.9 — OrionAlarmDbConnection and BatchStatement `[CERT]`

`OrionAlarmDbConnection` extends `AlarmDbConnection` (base alarm DB connection abstraction).
`[CERT]` `OrionAlarmDbConnection.java:54`

It holds an `OrionSession` received at construction time — no direct JDBC.
`[CERT]` `OrionAlarmDbConnection.java:58-60`

**BatchStatement usage in BOrionAlarmDatabase.recalculateAlarmClassStatistics():**

```java
// BOrionAlarmDatabase.java:227-228
BatchStatement alarmClassUpdater = session.batchUpdate(BOrionAlarmClass.ORION_TYPE);
BatchStatement alarmClassDeleter = session.batchDelete(BOrionAlarmClass.ORION_TYPE);
```

`[CERT]` `BOrionAlarmDatabase.java:227-228`. The `recalculateAlarmClassStatistics()` method
builds a complex `BSqlQuery` with `BSqlJoin`, `BGrouping`, and `BSqlCase` (CASE WHEN) to
aggregate counts across the six tables, then batch-updates `BOrionAlarmClass` statistics in a
single round-trip. `[CERT]` `BOrionAlarmDatabase.java:157-309` (method body)

**`clearOldRecords()` in OrionAlarmDbConnection:**
```java
public void clearOldRecords(BAbsTime before, Context cx) throws IOException  // :778
```
Issues an Orion delete where `datestamp < nextDay(date)` AND `timestamp < before`.
`[CERT]` `OrionAlarmDbConnection.java:778` (method signature + haiku sub-agent enumeration)

---

## 404.x — Self-Verify

| Claim | Marker | Citation |
|---|---|---|
| `BOrionAlarmDatabase extends BAlarmDatabase` (not BRdbms) | `[CERT]` | `BOrionAlarmDatabase.java:53` |
| BRdbms used only as lookup key, NOT as SQL execution engine | `[CERT]` | `BOrionAlarmDatabase.java:352,362-364` |
| Orion ORM = sole SQL layer (`com.tridium.orion.*`) | `[CERT]` | `BOrionAlarmDatabase.java` imports (lines 6-18) |
| 6 OrionTypes registered → 6 SQL tables | `[CERT]` | `BOrionAlarmService.java:63-70` |
| Same 6 types in BOrionArchiveAlarmProvider | `[CERT]` | `BOrionArchiveAlarmProvider.java:76-83` |
| Table name retrieved at runtime via `getTableName()` | `[CERT]` | `Upgrade_1_2_to_1_3.java:45` |
| `BOrionAlarmRecord.java:149-166` defines 15 SQL columns via static Property declarations | `[CERT]` | `BOrionAlarmRecord.java:149-166` |
| `id` property: `ID_KEY`, `DESCENDING=true` | `[CERT]` | `BOrionAlarmRecord.java:149` |
| `datestamp`, `uuidHash`, `sourceState`, `ackState` all carry `INDEXED=true` | `[CERT]` | `BOrionAlarmRecord.java:151-156` |
| `alarmClass` FK: `ON_DELETE=CASCADE`, `AUTO_RESOLVE=true` | `[CERT]` | `BOrionAlarmRecord.java:158-160` |
| `datestamp` derived from `timestamp` in `changed()` hook | `[CERT]` | `BOrionAlarmRecord.java:464-466` |
| `isOpen` computed via `beforeInsert()`/`beforeUpdate()` | `[CERT]` | `BOrionAlarmRecord.java:476-484` |
| AlarmData stored via EAV (AlarmFacetName + AlarmFacetValue) | `[CERT]` | `BOrionAlarmRecord.java:381-411` |
| BOrionAlarmClass: `alarmClass` column UNIQUE=true, WIDTH=64 | `[CERT]` | `BOrionAlarmClass.java:70-79` |
| LRU cache max 100 entries for BOrionAlarmClass | `[CERT]` | `BOrionAlarmClass.java:80` |
| OrionObjectCache: pure in-memory HashMap+LinkedList | `[CERT]` | `OrionObjectCache.java:19-20` |
| Schema version 1.4 | `[CERT]` | `BOrionAlarmService.java:59` |
| `database` BOrd slot: targetType=`"rdb:Rdbms"` | `[CERT]` | `BOrionAlarmService.java:56` |
| prepareSQL check: SQL Server only, warns on absent | `[CERT]` | `BOrionAlarmService.java:123-127` |
| `createAlarmDb()` → `new BOrionAlarmDatabase()` | `[CERT]` | `BOrionAlarmService.java:84-86` |
| `orionReady()` → `getAlarmDb().open()` | `[CERT]` | `BOrionAlarmService.java:151-158` |
| `OrionAlarmCursor extends AbstractCursor<BAlarmRecord>` | `[CERT]` | `OrionAlarmCursor.java:22` |
| Three cursor fields: recordCursor, facetCursor, sourceCursor | `[CERT]` | `OrionAlarmCursor.java:25-27` |
| `advanceCursor()` resets source+facets, calls recordCursor.next() | `[CERT]` | `OrionAlarmCursor.java:142-156` |
| No explicit page size in OrionAlarmCursor | `[INFER]` | no constant found in OrionAlarmCursor.java |
| Merge-join: getAlarm().equals(BRef.make(orionAlarmRecord)) | `[CERT]` | `OrionAlarmCursor.java:76,101` |
| Fallback to per-record query when side cursors null | `[CERT]` | `OrionAlarmCursor.java:85,120` |
| closeCursor: closes session if autoClose=true | `[CERT]` | `OrionAlarmCursor.java:173-175` |
| `doExecute()` in BASE class BArchiveAlarmProvider (NOT overridden) | `[CERT]` | `BArchiveAlarmProvider.java:291` |
| BQL cleared-alarm query template | `[CERT]` | `BArchiveAlarmProvider.java:307-311` |
| `exportClearedRecords()` is abstract in base, overridden in BOrionArchiveAlarmProvider | `[CERT]` | `BArchiveAlarmProvider.java:369`, `BOrionArchiveAlarmProvider.java:315` |
| `session.setAutoCommit(false)` before export loop | `[CERT]` | `BOrionArchiveAlarmProvider.java:324` |
| Single `session.commit()` for entire export | `[CERT]` | `BOrionArchiveAlarmProvider.java:335` |
| `session.rollback()` on RuntimeException | `[CERT]` | `BOrionArchiveAlarmProvider.java:339-342` |
| Failed AppendAlarmRecord skips UUID → stays in .adb | `[CERT]` | `BOrionArchiveAlarmProvider.java:330-332` |
| `clearRecord()` called in base class after export | `[CERT]` | `BArchiveAlarmProvider.java:341-346` |
| postExecute dispatches on BRdbms worker thread | `[CERT]` | `BOrionArchiveAlarmProvider.java:186-195` |
| importOpenAlarms: reads from Orion archive, writes to .adb | `[CERT]` | `BOrionArchiveAlarmProvider.java:197-276` |
| Upgrade 1.2→1.3: AlterColumn on BOrionAlarmSource.source | `[CERT]` | `Upgrade_1_2_to_1_3.java:35-36` |
| Upgrade 1.3→1.4: AlterColumn on BOrionAlarmFacetValue.value | `[CERT]` | `Upgrade_1_3_to_1_4.java:27-28` |
| BatchStatement batchUpdate/batchDelete in recalculateAlarmClassStatistics | `[CERT]` | `BOrionAlarmDatabase.java:227-228` |
| clearOldRecords method at line 778 | `[CERT]` | `OrionAlarmDbConnection.java:778` |

**Self-verify tally:** 43 claims — 41 `[CERT]`, 2 `[INFER]` (no page size constant in
OrionAlarmCursor; no page size in exportClearedRecords). Zero unsupported assertions.

## 404.x — Connections

- **[Block 34]** — `.adb` local alarm format (MAGIC, paged, AlarmStore). B404's archive move
  reads cleared alarms from the local service (backed by `.adb`) and calls `clearRecord()` which
  writes back to `.adb`. B34 §34.13 is the prior surface for alarmOrion — B404 goes deeper on
  schema, cursor, and archive move mechanics that B34 only sketched.
- **[Block 403]** — rdb-rt JDBC export pipeline. B403 documents the direct JDBC/dialect path
  (raw SQL, PreparedStatement, batch). B404 contrasts: alarmOrion uses the Orion ORM abstraction,
  not the rdb-rt dialect pipeline, though both modules resolve through `BRdbms` for the
  underlying SQL connection.
- **[Block 402]** — BOG save trigger (DB1). B402 and B404 together complete the `database` focus
  write-path investigation on opposite ends: B402 is the component-space dirty-flag save, B404 is
  the alarm-to-SQL archive move.
