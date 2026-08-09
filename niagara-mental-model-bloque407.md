# Block 407 — History Archive Provider Chain: BTimeTrigger Scheduling, `BRdbmsHistoryExport` Write Path, High-Watermark Idempotency, No-Retry Failure, and `BRdbArchiveHistoryProvider` Read-Back

> **Research focus:** `database` (gap **DB6**, medium-priority). Covers the complete
> archival chain that moves local `.hdb` history records into an external RDBMS — from
> the scheduling trigger through the INSERT batch loop and into the read-back provider
> that merges RDB records at query time. Answers: what triggers archival, how batching
> and failure/retry are handled, and how `BRdbmsHistoryExport` plugs into the
> `BArchiveHistoryProvider` chain.
>
> **Not covered here:**
> - `.hdb` binary format → [Block 33]
> - RDBMS JDBC write mechanics (dialect, DDL, column type map, batch INSERT internals) → [Block 403]
> - `BHistoryDatabase` in the MX60 context → [Block 174]
> - BQL grammar and AST → [Block 21]
>
> Subject version: N4.14.0.162 (Vineflower decompiled corpus; organized/ tree).
>
> Sources:
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/driver/driver-rt/vineflower/javax/baja/driver/util/BAbstractDescriptor.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/driver/driver-rt/vineflower/javax/baja/driver/util/BDescriptor.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/driver/driver-rt/vineflower/javax/baja/driver/history/BHistoryExport.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/driver/driver-rt/vineflower/javax/baja/driver/history/BArchiveDescriptor.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/javax/baja/rdb/history/BRdbmsHistoryExport.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/javax/baja/rdb/history/BRdbmsHistoryDeviceExt.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/com/tridium/rdb/history/BRdbArchiveHistoryProvider.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/history/history-rt/vineflower/javax/baja/history/db/BArchiveHistoryProvider.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/history/history-rt/vineflower/javax/baja/history/db/BArchiveHistoryProviders.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/history/history-rt/vineflower/javax/baja/history/db/BHistoryDatabase.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/history/history-rt/vineflower/javax/baja/history/db/BArchiveLimitNotificationBehavior.java`
>
> Method: decompiled Java (Vineflower); all cited file:line verified against
> source. Heavy sub-sweep (fork agent) confirmed findings independently.
> `database` focus. Connects [Block 403] (RDB write mechanics — dialect, DDL, batch
> INSERT), [Block 33] (`.hdb` format; §33.7 READ-plane query-merge; §33.7.4 system
> properties), [Block 174] (`BHistoryDatabase` in MX60 context).

---

## 407.1 — Two-Plane Architecture: Write Side vs. Read Side `[CERT]`

The "archive provider chain" has TWO distinct and independent planes. DB6 asked about
both; they live in different module trees and serve different purposes.

| Plane | Component | Lives under | Purpose |
|---|---|---|---|
| **WRITE** | `BRdbmsHistoryExport` | `BRdbms` (device) → `BRdbmsHistoryDeviceExt` | Reads `.hdb` records and INSERTs them into RDB on a schedule |
| **READ** | `BRdbArchiveHistoryProvider` | `BHistoryService.archiveHistoryProviders` | Queries the RDB at read-time to merge archived records with local cursor |

`[CERT]` `BRdbmsHistoryExport.java:79` (extends `BHistoryExport` — write side)
`[CERT]` `BRdbArchiveHistoryProvider.java:129` (extends `BArchiveHistoryProvider` — read side)

The `BArchiveHistoryProvider` abstract class carries an explicit API contract in its
docSource javadoc: *"archive data will only be accessed at query time (reads). The
archive provider will not be used for writing new history data."* `[INFER]` (docSource
javadoc; consistent with the two-plane decompiled structure).

The component topology is:

```
BHistoryService
  └── archiveHistoryProviders: BArchiveHistoryProviders   [READ plane container]
        └── BRdbArchiveHistoryProvider                    [READ — query-merge]

BRdbmsNetwork
  └── BRdbms                                              [WRITE plane — device]
        └── BRdbmsHistoryDeviceExt
              └── BRdbmsHistoryExport (one per history)   [WRITE — scheduled export]
```

---

## 407.2 — Archival Trigger: `BTimeTrigger` via Hidden Dynamic `BLink` `[CERT]`

The trigger is NOT a standalone cron daemon and is NOT fired by capacity thresholds.
It is a per-export `BTimeTrigger` property (cron-like wall-clock scheduler) wired to
the `execute` action via a hidden dynamic link installed at component start.

### 407.2.1 — The `executionTime` property

`BAbstractDescriptor` (top of the inheritance chain) declares:

```java
// BAbstractDescriptor.java:82
public static final Property executionTime =
    newProperty(0, new BTimeTrigger(BDailyTriggerMode.make()), null);
```

`[CERT]` `BAbstractDescriptor.java:48` (`@NiagaraProperty` annotation: `defaultValue = "new BTimeTrigger(BDailyTriggerMode.make())"`)
`[CERT]` `BAbstractDescriptor.java:82` (static field declaration)

Default value = `BTimeTrigger` with `BDailyTriggerMode` — fires once per day at
the configured time. Configurable in Workbench per-export descriptor.

### 407.2.2 — The hidden `triggerLink`

`fwStarted()` installs a hidden transient `BLink` that connects
`executionTime.fireTrigger` → `execute`:

```java
// BAbstractDescriptor.java:265-267
private void fwStarted() {
    this.updateStatus();
    this.add("triggerLink?",
        new BLink(this.getExecutionTime().getOrdInSession(), "fireTrigger", "execute", true),
        6);
}
```

`[CERT]` `BAbstractDescriptor.java:265-267`

When `executionTime` changes, the link is replaced (`fwChanged()` at line 282
reinstalls it with the new ORD). When the component stops, `fwStopped()` removes
the `triggerLink` property.

`[CERT]` `BAbstractDescriptor.java:271-275` (fwStopped removes triggerLink)
`[CERT]` `BAbstractDescriptor.java:282` (fwChanged reinstalls on executionTime change)

### 407.2.3 — Post gate: no concurrent execution, no queuing

`BAbstractDescriptor.post()` guards against concurrent or redundant dispatch:

```java
// BAbstractDescriptor.java:229-243
public IFuture post(Action action, BValue arg, Context cx) {
    if (action.equals(execute)) {
        if (this.isUnoperational()) { return null; }         // disabled/down → skip
        if (this.getState() != BDescriptorState.idle) { return null; } // in-flight → SKIP
        this.setLastAttempt(Clock.time());
        this.setState(BDescriptorState.pending);
        return this.postExecute(action, arg, cx);
    }
    return super.post(action, arg, cx);
}
```

`[CERT]` `BAbstractDescriptor.java:229-243`

Key: if `state != idle` (already pending or in-progress), the trigger fire is SILENTLY
DROPPED — no queue accumulation. This means at most one concurrent export per descriptor.

### 407.2.4 — Dispatch to `BRdbmsWorker`

`BRdbmsHistoryExport.postExecute()` overrides the abstract method to dispatch to the
RDBMS-specific thread pool:

```java
// BRdbmsHistoryExport.java:220-227
protected IFuture postExecute(Action action, BValue arg, Context cx) {
    BRdbms db = (BRdbms)this.getDevice();
    if (db != null) {
        db.getWorker().postAsync(new Invocation(this, action, arg, cx));
    }
    return null;
}
```

`[CERT]` `BRdbmsHistoryExport.java:220-227`

`BRdbmsWorker` is a `BThreadPoolWorker` (maxThreads=1, queue=1000) backed by a
`CoalesceQueue`. `[CERT]` B403 §403.1 (remit — write mechanics covered there).

### 407.2.5 — Station-restart recovery

If the station restarts with a descriptor in non-idle state (e.g. interrupted mid-export),
`fwStationStarted()` detects this and re-triggers immediately:

```java
// BAbstractDescriptor.java:287-292
private void fwStationStarted() {
    if (this.getState() != BDescriptorState.idle) {
        this.setState(BDescriptorState.idle);
        this.execute();    // re-trigger after restart
    }
}
```

`[CERT]` `BAbstractDescriptor.java:287-292`

---

## 407.3 — `BRdbmsHistoryExport.doExecute()`: Entry Point on the Worker Thread `[CERT]`

Execution on the `BRdbmsWorker` thread calls `doExecute()`:

```
BRdbmsHistoryExport.doExecute() [:138]
  → executeInProgress()                      [state = inProgress]
  → BRdbmsDeprecatedDialect.make(db)         [dialect selection]
  → BRdbms.getConnection(user, pass)         [DBCP JDBC pool]
  → conn.setAutoCommit(false)                [explicit transaction]
  → BHistoryService.getDatabase().getConnection(null)
      → .getHistory(historyId)               [resolve BIHistory from local .hdb]
  → exportRecords()                          [:229]
  → executeOk()                              [lastSuccess, state = idle]
  [on any Exception]:
  → executeFail(ex)                          [lastFailure, faultCause, state = idle]
  [finally]:
  → conn.close()
```

`[CERT]` `BRdbmsHistoryExport.java:138-218`

Guard: if `BRdbms.getEnabled() == false`, the export logs a trace, sets state=idle and
returns immediately without attempting a connection. `[CERT]` `BRdbmsHistoryExport.java:141-145`

---

## 407.4 — High-Watermark Idempotency: `lookupMaxTimestamp()` `[CERT]`

The starting point for each export run is computed from the RDBMS itself, not from a
separate state variable (unless `useLastTimestamp=true`).

```java
// BRdbmsHistoryExport.java:358-365
private BAbsTime insertRecords(BAbsTime lastTime, String tableName, boolean fallback...)
    throws SQLException {
    BAbsTime since = this.lookupMaxTimestamp(tableName);   // query RDB MAX(TIMESTAMP)
    if (since != null) {
        since = BAbsTime.make(since.getMillis() + this.dialect.getTimestampAccuracy());
    } else if (fallback...) {
        since = BAbsTime.make(lastTime.getMillis() + this.dialect.getTimestampAccuracy());
    }
    // ... open historyConn with EXCLUDE_ARCHIVE_DATA context
    // ... timeQuery(history, since, null).cursor()    [reads .hdb from since forward]
```

`[CERT]` `BRdbmsHistoryExport.java:358-372`

`lookupMaxTimestamp()` behaviour depends on `BRdbmsHistoryDeviceExt.useLastTimestamp`
(default=false):

| `useLastTimestamp` | Start point | Source |
|---|---|---|
| `false` (default) | `MAX(TIMESTAMP)` + dialect accuracy queried from RDB table | Idempotent; re-runs are safe |
| `true` | `lastTimestamp` property on the descriptor (persisted) | Faster; manual or `updateLastTimestamp()` action needed |

`[CERT]` `BRdbmsHistoryDeviceExt.java:33,61` (`useLastTimestamp`, default=false)
`[CERT]` `BRdbmsHistoryExport.java:519` (`lookupMaxTimestamp()` method header)

**Anti-circularity**: the `.hdb` query uses `HistoryQuery.makeExcludeArchiveDataContext(null)`
so the export cursor reads only from the LOCAL file store, never from archive providers.
This prevents circular reads when a `BRdbArchiveHistoryProvider` is also configured.

`[CERT]` `BRdbmsHistoryExport.java:368`

---

## 407.5 — Batch INSERT Loop: `insertRecords()` `[CERT]`

Records are batched in groups of `EXPORT_BATCH_SIZE` (default 1000); a single JDBC
`commit()` covers the full cursor scan.

```java
// BRdbmsHistoryExport.java:96-99
private static final int DEFAULT_EXPORT_BATCH_SIZE = 1000;
private static final Integer EXPORT_BATCH_SIZE = AccessController.doPrivileged(
    (PrivilegedAction<Integer>)(() ->
        Integer.getInteger("niagara.rdb.historyExport.batchSize", 1000))
);
```

`[CERT]` `BRdbmsHistoryExport.java:96-99`

Loop body:

```java
// BRdbmsHistoryExport.java:444-454
if (count >= EXPORT_BATCH_SIZE) {
    this.logTrace("sending batch at " + totalCount + " records");
    ps.executeBatch();
    ps.clearBatch();
    count = 0;
}
// ... after while loop:
if (count > 0) { ps.executeBatch(); }    // tail flush
```

`[CERT]` `BRdbmsHistoryExport.java:444-454`

Single commit after all batches: `[CERT]` `BRdbmsHistoryExport.java:457`

**Consequence**: if the RDBMS commit fails after 50 000 records were batched, the entire
transaction rolls back (JDBC autorollback on close). The next run re-reads from
`MAX(TIMESTAMP)` and re-inserts everything — no partial data survives. `[INFER]`

The `lastTimestamp` property on the descriptor is updated by `exportRecords()` only after
`insertRecords()` returns successfully:

```java
// BRdbmsHistoryExport.java:299-301
BAbsTime oldStamp = this.getLastTimestamp();
BAbsTime newStamp = this.insertRecords(oldStamp, tableName, ...);
this.setLastTimestamp(newStamp);           // updated ONLY on success
```

`[CERT]` `BRdbmsHistoryExport.java:299-301`

---

## 407.6 — Failure Semantics: No Automatic Retry `[CERT]`

`BAbstractDescriptor.executeFail()` records the failure and returns the descriptor to idle:

```java
// BAbstractDescriptor.java:169-183
public void executeFail(String reason) {
    if (reason == null) reason = "";
    this.setLastFailure(Clock.time());
    this.setFaultCause(reason);
    this.setState(BDescriptorState.idle);
    this.updateStatus();
}

public void executeFail(Throwable ex) {
    this.executeFail(getFailureReason(ex));  // dumps first line of stacktrace
}
```

`[CERT]` `BAbstractDescriptor.java:169-183`

There is no retry loop, no exponential backoff, and no failed-batch queue. The next
attempt is at the next `BTimeTrigger` fire (typically the following day). The `fault` bit
is set on the status property so the descriptor shows as faulted in Workbench.
`[CERT]` `BAbstractDescriptor.java:84-86` (status property)

`BRdbmsHistoryExport.doExecute()` catches `Exception` (line 202), calls `executeFail(ex)`,
and closes the connection in the `finally` block (line 206). No partial INSERT survives
because `setAutoCommit(false)` means the incomplete transaction is rolled back on close.
`[CERT]` `BRdbmsHistoryExport.java:202-213`

---

## 407.7 — `BRdbArchiveHistoryProvider`: Read-Back from RDB at Query Time `[CERT]`

`BRdbArchiveHistoryProvider` extends `BArchiveHistoryProvider` and is the concrete
subclass used by the rdb module to serve read-back queries from the archived RDB data.

### 407.7.1 — Properties

```java
// BRdbArchiveHistoryProvider.java:91-124 (annotations)
ordToRdbms:         BOrd   — ORD pointing to the target BRdbms instance
useDefaultFetchSize: boolean — default true (use BRdbms.resultSetFetchSize)
customFetchSize:    int    — used if useDefaultFetchSize=false
fetchSizeInUse:     int    — readonly, computed
```

`[CERT]` `BRdbArchiveHistoryProvider.java:91-140`

License: requires feature `"tridium"/"rdbHistoryArchive"` (checked by `checkProviderLicense()`).
`[CERT]` `BRdbArchiveHistoryProvider.java:563-574`

### 407.7.2 — The static `RDBMS_EXPORT_MAP`

A class-level cache associates each `BRdbms` instance with a map of
`BHistoryId → List<BRdbmsHistoryExport>`:

```java
// BRdbArchiveHistoryProvider.java:157
private static final Map<BRdbms, Map<BHistoryId, List<BRdbmsHistoryExport>>>
    RDBMS_EXPORT_MAP = Collections.synchronizedMap(new WeakHashMap<>());
```

`[CERT]` `BRdbArchiveHistoryProvider.java:157` (field declaration)
`[CERT]` `BRdbArchiveHistoryProvider.java:832` (static initializer: `Collections.synchronizedMap(new WeakHashMap<>())`)

`getIdToExportMap()` (line 719) populates the map lazily by walking the `BRdbms`
component's properties, collecting all `BRdbmsHistoryDeviceExt` children and their
`BRdbmsHistoryExport` descriptors. `[CERT]` `BRdbArchiveHistoryProvider.java:719-772`

`RdbmsTypeSubscriber` (inner class, line 1151) maintains the cache: it subscribes to
component events for `BRdbms`, `BRdbmsHistoryExport`, `BRdbmsHistoryDeviceExt`, and
`BArchiveFolder` and invalidates map entries on add/remove/change.
`[CERT]` `BRdbArchiveHistoryProvider.java:1151,452` (subscribe call in `stationStarted()`)

### 407.7.3 — `isLikelyToContainArchivedHistory()`

Used as a cheap filter before running a full SQL query:

```java
// BRdbArchiveHistoryProvider.java:204-212
public boolean isLikelyToContainArchivedHistory(BHistoryConfig historyConfig, Context context) {
    Optional<BRdbms> rdbmsOptional = this.getRdbms();
    if (rdbmsOptional.isPresent()) {
        BRdbms rdbms = rdbmsOptional.get();
        return findExportForHistory(rdbms, historyConfig) != null;  // checks EXPORT_MAP
    }
    return false;
}
```

`[CERT]` `BRdbArchiveHistoryProvider.java:204-212`

If a matching `BRdbmsHistoryExport` descriptor exists in `RDBMS_EXPORT_MAP` for the
requested `BHistoryId`, the provider is considered likely to have archived data.

### 407.7.4 — Inactive cursor cleanup

`stationStarted()` schedules a periodic cleanup of inactive open cursors:

```java
// BRdbArchiveHistoryProvider.java:447
this.inactiveCursorTicket = Clock.schedulePeriodically(this,
    BRelTime.make(INACTIVE_CURSOR_TIMEOUT), closeExpiredCursors, null);
```

`[CERT]` `BRdbArchiveHistoryProvider.java:447`

`INACTIVE_CURSOR_TIMEOUT` = `Long.getLong("niagara.rdbArchiveHistoryCursor.inactivityTimeout", 120000L)` = 120 000 ms (2 minutes) by default.
`[CERT]` `BRdbArchiveHistoryProvider.java:810-817` (static initializer)

`doCloseExpiredCursors()` (line 389) walks `openCursors` (a `ConcurrentHashMap`), compares
the cursor's advance counter against its previous value, and closes the cursor if the
counter has not advanced within `INACTIVE_CURSOR_TIMEOUT`.
`[CERT]` `BRdbArchiveHistoryProvider.java:389-421`

---

## 407.8 — `BArchiveHistoryProviders` Container `[CERT]`

`BArchiveHistoryProviders` (final class) is the folder that holds all read-side archive
providers. Topology constraint: must be a direct child of `BHistoryService`, exactly one
instance per station (enforced by `checkParentForRestrictedComponent()`).
`[CERT]` `BArchiveHistoryProviders.java:20,100-106`

Provider iteration is lazy-cached in two lists (all / operational):

```java
// BArchiveHistoryProviders.java:23-24
private List<BArchiveHistoryProvider> allArchiveHistoryProviders;
private List<BArchiveHistoryProvider> operationalArchiveHistoryProviders;
```

`[CERT]` `BArchiveHistoryProviders.java:23-24`

Both lists are invalidated and rebuilt on any component event that adds, removes, reorders,
or changes an `BArchiveHistoryProvider` child.
`[CERT]` `BArchiveHistoryProviders.java:83-98` (fw() method)

---

## 407.9 — `BArchiveHistoryProvider` Abstract: Key Properties and License Gate `[CERT]`

The abstract base class (implemented by `BRdbArchiveHistoryProvider`) declares:

| Property | Type | Default | Role |
|---|---|---|---|
| `enabled` | `boolean` | `false` | Provider inactive until explicitly enabled |
| `maxArchiveResultsPerQuery` | `int` | `50000` | Hard cap on rows returned by `doTimeQuery()` |
| `archiveLimitNotifications` | `BArchiveLimitNotificationBehavior` | `notifyOncePerQueryRangePerSession` | UI alert when limit hit |

`[CERT]` `BArchiveHistoryProvider.java:34-64` (annotations)
`[CERT]` `BArchiveHistoryProvider.java:66-70` (static field declarations with defaults)

Note: B33 §33.7.1 reported `enabled` default as `false` from `javap -p`; this block
confirms it from the vineflower body. B33 §33.7.1 also reported `maxArchiveResultsPerQuery`
default as 10 000; the vineflower source shows `50000`. Vineflower default wins
(full body read). `[CERT]` `BArchiveHistoryProvider.java:69`

`BArchiveLimitNotificationBehavior` enum ordinals (from vineflower):

| Ordinal | Name |
|---|---|
| 0 | `notifyOncePerQueryRangePerSession` (default) |
| 1 | `neverNotify` |
| 2 | `alwaysNotify` |

`[CERT]` `BArchiveLimitNotificationBehavior.java:11-22,27-29`

Base class license: requires feature `"tridium"/"historyArchive"` (checked in `fwStarted()`).
Subclasses add their own `checkProviderLicense()`.
`[CERT]` `BArchiveHistoryProvider.java:136-148`

---

## 407.10 — System Properties `[CERT]`

| Property | Default | Effect |
|---|---|---|
| `niagara.rdb.historyExport.batchSize` | `1000` | INSERT batch size in `insertRecords()` |
| `niagara.rdbArchiveHistoryCursor.inactivityTimeout` | `120000` (ms) | Inactive archive cursor cleanup interval |

`[CERT]` `BRdbmsHistoryExport.java:97-99` (batchSize)
`[CERT]` `BRdbArchiveHistoryProvider.java:810-817` (inactivityTimeout static init)

B33 §33.7.4 cited both properties from `defaults/system.properties`; this block confirms
their default values from the decompiled code. `[CERT]`

---

## 407.11 — Complete Call Chain `[CERT]` `[INFER]`

```
BTimeTrigger fires (daily by default)
  ↓ [hidden BLink: fireTrigger → execute]                          [CERT :267]
BAbstractDescriptor.post(execute, ...)
  → if state ≠ idle: DROP (silent)                                 [CERT :233-235]
  → setState(pending) + setLastAttempt()                           [CERT :238-239]
  → BRdbmsHistoryExport.postExecute()                              [CERT :220]
      → BRdbms.getWorker().postAsync(Invocation)                   [CERT :223]
          [BRdbmsWorker thread — maxThreads=1, CoalesceQueue(1000)]
BRdbmsHistoryExport.doExecute()                                    [CERT :138]
  → executeInProgress()               [state = inProgress]
  → BRdbmsDeprecatedDialect.make(db)
  → BRdbms.getConnection(user, pass)  [DBCP pool → JDBC Connection]
  → conn.setAutoCommit(false)                                       [CERT :157]
  → localDb.getConnection(null).getHistory(historyId)               [CERT :161-165]
  → exportRecords()                                                  [CERT :197]
      → dialect.tableExists() → createMetaTable() if absent
      → RdbmsHistoryUtil.isNewSchema() → schema detection
      → getMetaRecord() → resolve tableName                        [CERT :239-301]
      → insertRecords(oldStamp, tableName, ...)                     [CERT :300]
          → lookupMaxTimestamp(tableName)  → MAX(TIMESTAMP) from RDB [CERT :359]
          → since = maxTS + timestampAccuracy                        [CERT :361]
          → localDb.getConnection(EXCLUDE_ARCHIVE_DATA)             [CERT :368]
          → historyConn.timeQuery(history, since, null).cursor()    [CERT :372]
          → dialect.makeInsertSql(tableName, props, extraFields)
          → [per record] dialect.insertRecord(ps, rec, ...)
          → ps.addBatch()
          → [every 1000] ps.executeBatch() + ps.clearBatch()        [CERT :444-449]
          → [tail] ps.executeBatch() if count > 0                   [CERT :452-454]
          → conn.commit()                                            [CERT :457]
      → setLastTimestamp(newStamp)  [persisted on success only]     [CERT :301]
  → executeOk()  [lastSuccess, state = idle, fault cleared]
  [on Exception]:
  → executeFail(ex)  [lastFailure, faultCause, state = idle]        [CERT :184-193]
  → conn.close() (finally)  [JDBC autorollback — no partial data]   [CERT :206-212]
```

`[INFER]` — autorollback on close inferred from JDBC specification; no explicit
`rollback()` call visible in the source.

---

## 407.12 — Connections

- **[Block 403]** — documents the RDB WRITE pipeline (`BRdbmsWorker`, `doExecute()`,
  JDBC dialect, DDL, INSERT mechanics). Block 407 extends that by documenting WHO
  calls `doExecute()` (the `BTimeTrigger` chain) and the failure/retry semantics that
  B403 explicitly deferred to DB6.

- **[Block 33]** — documents `.hdb` binary format (§33.1-33.6) and the query-merge READ
  side in §33.7. Block 407 covers the WRITE side (archival trigger and INSERT path)
  that §33.7 does not address. Corrects B33 §33.7.1's `maxArchiveResultsPerQuery`
  default (B33 reported 10 000 from `javap -p`; vineflower body shows 50 000).

- **[Block 174]** — uses `BHistoryDatabase.getConnection()` in the MX60 context.
  Block 407 shows the same `getDatabase().getConnection()` call inside `doExecute()`
  at the export side.

- **[Block 21]** — BQL/NEQL grammar. Not directly related; `BIRelational` interface
  on `BHistoryDatabase` enables BQL table access to history configs, but that is
  the query path, not the archival path.
