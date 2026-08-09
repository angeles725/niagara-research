# Block 403 — rdb-rt: External RDBMS History Export Pipeline (Write Path)

> **Research focus:** `database` (gap **DB2**, high-priority). Covers the complete write pipeline
> for exporting Niagara N4 `.hdb` history records to an external relational database — from the
> asynchronous dispatch through `BRdbmsWorker`, through the JDBC connection setup in `doExecute()`,
> the two export-mode table strategies, the per-dialect SQL type mapping in
> `BRdbmsDeprecatedDialect.loadField()`, the JDBC batch INSERT loop, and the concrete per-DBMS
> implementations (MySQL, Oracle, SQL Server) including JDBC URL construction and driver loading.
>
> **Not covered here:**
> - `.hdb` format internals → [Block 33]
> - `.adb` alarm format → [Block 34]
> - `BArchiveHistoryProvider` trigger/batching (DB6) — touched only as a scope boundary in §403.12
> - HSQLDB embedded adapter (DB8)
> - alarmOrion RDBMS backend (DB3)
>
> Subject version: N4.14.0.162 (Vineflower decompiled corpus; organized/ tree).
>
> Sources:
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/javax/baja/rdb/history/BRdbmsHistoryExport.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/com/tridium/rdb/BRdbmsDeprecatedDialect.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/com/tridium/rdb/jdbc/RdbmsDialect.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/javax/baja/rdb/BRdbms.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/javax/baja/rdb/BRdbmsWorker.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/javax/baja/rdb/BRdbmsTimestampStorage.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/javax/baja/rdb/history/BRdbmsHistoryExportMode.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/com/tridium/rdb/BEncryptableTransportRdbms.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/com/tridium/rdb/aes/BRdbSecuritySettings.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdb/rdb-rt/vineflower/com/tridium/rdb/history/RdbmsHistoryUtil.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdbMySQL/rdbMySQL-rt/vineflower/com/tridium/rdb/mysql/BMySQLDatabase.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdbMySQL/rdbMySQL-rt/vineflower/com/tridium/rdb/mysql/BMySQLConnectionPool.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdbOracle/rdbOracle-rt/vineflower/com/tridium/rdb/oracle/BOracleDatabase.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdbOracle/rdbOracle-rt/vineflower/com/tridium/rdb/oracle/BOracleConnectionPool.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdbSqlServer/rdbSqlServer-rt/vineflower/com/tridium/rdb/sqlserver/BSqlServerDatabase.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdbSqlServer/rdbSqlServer-rt/vineflower/com/tridium/rdb/sqlserver/BConnectionPool.java`
>
> Method: decompiled Java (Vineflower); direct read of organized/ corpus. Supplemental structural
> analysis delegated to sonnet sub-agent; mechanical enumeration delegated to haiku sub-agent.
> All load-bearing citations independently verified against source files.
>
> `database` focus. Connects [Block 33] (`.hdb` format from which records are read), [Block 8]
> (BHistoryExport / BHistoryImport abstractions), [Block 34] (`.adb` format — parallel alarm backend).

---

## 403.1 — Module Architecture and Class Roles `[CERT]`

The rdb-rt pipeline spans three Niagara modules. `rdb-rt` provides the abstract framework;
concrete dialect modules each bundle their own JDBC driver.

| Class | Module | Role |
|---|---|---|
| `BRdbmsNetwork` | rdb-rt | `BDeviceNetwork` shell; registers the `sql:` Fox scheme channel. No connection parameters. |
| `BRdbms` | rdb-rt | Abstract device base. Holds connection config, export mode, worker, dialect, security settings. |
| `BRdbmsWorker` | rdb-rt | `BThreadPoolWorker` (maxThreads=1, maxQueueSize=1000); serializes async export invocations via `CoalesceQueue`. |
| `BRdbmsHistoryExport` | rdb-rt | Abstract. The core export orchestrator; subclassed once per dialect module. |
| `BRdbmsDeprecatedDialect` | rdb-rt | Abstract; dialect-agnostic SQL generation: `makeCreateTableSql`, `makeInsertSql`, `insertRecord`, `loadField`. |
| `RdbmsDialect` | rdb-rt | Interface. Per-DBMS SQL type methods, capability flags, identity/sequence strategy constants. |
| `BEncryptableTransportRdbms` | rdb-rt | Abstract subclass of `BRdbms`; adds TLS slots (`tlsMinProtocol`, `verifySubjectInCertificate`) and per-instance keystores. |
| `BMySQLDatabase` | rdbMySQL-rt | Concrete `BEncryptableTransportRdbms` for MySQL 8+. Bundles MySQL Connector/J. |
| `BOracleDatabase` | rdbOracle-rt | Concrete `BEncryptableTransportRdbms` for Oracle. Bundles Oracle OJDBC. |
| `BSqlServerDatabase` | rdbSqlServer-rt | Concrete `BEncryptableTransportRdbms` for SQL Server. Bundles `com.microsoft.sqlserver.jdbc.*`. |
| `BRdbArchiveHistoryProvider` | rdb-rt | Read-back path: queries exported SQL table for archive queries. **Not the write path.** (Scope boundary → §403.12) |

`[CERT]` `BRdbmsNetwork.java:15-64`, `BRdbms.java:36-98`, `BRdbmsWorker.java:34`, `BRdbmsHistoryExport.java:79`, `BRdbmsDeprecatedDialect.java` (class header), `RdbmsDialect.java:9-136`

## 403.2 — Export Entry Point: doExecute() Call Chain `[CERT]`

`BRdbmsHistoryExport.doExecute()` is the write entry point, called by the Niagara driver
scheduler on the `BRdbmsWorker` thread pool. It opens a JDBC connection, opens a `.hdb`
cursor, and drives the INSERT loop.

```
BRdbmsWorker.postAsync(Invocation)              [dispatcher — CoalesceQueue]
  → BRdbmsHistoryExport.doExecute()             [:138 — thread-pool thread]
      → BRdbmsDeprecatedDialect.make(db)        [instantiate dialect]
      → BRdbms.getConnection(user, password)    [DBCP pool → JDBC Connection]
      → conn.setAutoCommit(false)               [explicit transaction]
      → BHistoryDatabase.getConnection()
          .getHistory(historyId)                [resolve .hdb BIHistory object]
      → exportRecords()                         [:197 / :229]
          → dialect.tableExists() → createMetaTable()  if absent
          → RdbmsHistoryUtil.isNewSchema()
          → createTable(tableName, tableId)     if data table absent [:270]
              → dialect.makeCreateTableSql()    [DDL string]
              → stmt.executeUpdate(sql)         [CREATE TABLE sent to RDBMS]
              → RdbmsHistoryUtil.createIndex()  [CREATE INDEX sent]
              → conn.commit()
          → insertRecords(oldStamp, tableName)  [:300 / :358]
              → historyConn.timeQuery(history, since, null).cursor()  [.hdb cursor]
              → dialect.makeInsertSql(...)      [build INSERT template once]
              → conn.prepareStatement(sql)
              → [per record] dialect.insertRecord(ps, rec, template, extras, facets)
                    → loadField(...) → ps.setXxx()  [type dispatch]
                    → ps.addBatch()
              → [every EXPORT_BATCH_SIZE] ps.executeBatch() + ps.clearBatch()
              → [tail flush] ps.executeBatch()  if residual > 0
          → conn.commit()                       [single commit for full cursor scan]
```

`[CERT]` `BRdbmsHistoryExport.java:138` (doExecute entry), `BRdbmsHistoryExport.java:153-157`
(dialect + connection + autoCommit), `BRdbmsHistoryExport.java:220-226` (postExecute async
dispatch), `BRdbmsHistoryExport.java:229` (exportRecords), `BRdbmsHistoryExport.java:358`
(insertRecords). Structural sub-agent (sonnet) validated call chain against source.

**Batch size:**

```java
// BRdbmsHistoryExport.java:96-99
private static final int DEFAULT_EXPORT_BATCH_SIZE = 1000;
private static final Integer EXPORT_BATCH_SIZE = AccessController.doPrivileged(
    (PrivilegedAction<Integer>)(() -> Integer.getInteger("niagara.rdb.historyExport.batchSize", 1000))
);
```

Default 1000 records per JDBC batch; configurable via JVM system property
`niagara.rdb.historyExport.batchSize`. `[CERT]` `BRdbmsHistoryExport.java:96-99`

**Async dispatch:** `postExecute()` wraps the invocation in an `Invocation` object and enqueues
it on the `BRdbmsWorker` `CoalesceQueue`. All export executions are asynchronous relative to
the caller. `[CERT]` `BRdbmsHistoryExport.java:220-226`, `BRdbmsWorker.java:60-67`

## 403.3 — Export Modes `[CERT]`

`BRdbms.exportMode` (`BRdbmsHistoryExportMode`) controls table layout:

| Mode | Ordinal | Default | Data table naming | Meta table | Extra columns |
|---|---|---|---|---|---|
| `byHistoryId` | 0 | YES | `DEVICENAME_HISTORYNAME` (truncated to `maxTableName`) | `HISTORY_CONFIG` | None |
| `byHistoryType` | 1 | No | Record-type class name (mangled, truncated) | `HISTORY_TYPE_MAP` | `HISTORY_ID VARCHAR(500)` |

`[CERT]` `BRdbmsHistoryExportMode.java:11-18` (enum ordinals), `BRdbmsHistoryExport.java:591-600`
(getMetaTableName), `BRdbmsHistoryExport.java:668-701` (inventTableName), `BRdbmsHistoryExport.java:100`
(`HISTORY_ID` constant)

**Table name mangling (`inventTableName`):**
- Mode 0: `DEVICENAME_HISTORYNAME`, uppercased, SQL-identifier-mangled, truncated to `dialect.getMaxTableName()`.
- Mode 1: record-type simple class name, same mangling + numeric suffix if collision in meta table.
- `BRdbmsDeprecatedDialect.mangleIdentifier()`: strips non-alphanumeric, uppercases, remaps SQL reserved
  words: `"id"→"ID_"`, `"schema"→"SCHEMA_"`, `"interval"→"INTERVAL_"`. `[CERT]` `BRdbmsDeprecatedDialect.java:659-664`

**Meta table extra fields:** both `HISTORY_CONFIG` and `HISTORY_TYPE_MAP` include `TABLE_NAME`
and `DB_TIMEZONE` columns (both `BString`). `[CERT]` `BRdbmsHistoryExport.java:606-607`

## 403.4 — SQL Column Schema `[CERT]`

`BRdbmsDeprecatedDialect.makeCreateTableSql()` builds `CREATE TABLE {name} (ID {PK}, col1, col2, ...)`.
The column list is derived from the `BHistoryRecord` property template. Extra columns are appended
based on mode and record type.

| Column | Type | Condition | Source |
|---|---|---|---|
| `ID` | per-dialect PK (SEQUENCE / IDENTITY / AUTO_INCREMENT) | always | `RdbmsDialect.getInsertionMode()` |
| All `BHistoryRecord` property fields | per-dialect type (see §403.5) | always | property template |
| `HISTORY_ID` | `VARCHAR(500)` | mode = `byHistoryType` | `BRdbmsHistoryExport.java:339` |
| `TRENDFLAGS_TAG` | `VARCHAR(500)` | record is `BTrendRecord` | `BRdbmsHistoryExport.java:316-318` |
| `STATUS_TAG` | `VARCHAR(500)` | record is `BTrendRecord` | `BRdbmsHistoryExport.java:316-318` |

Default VARCHAR width: 500 characters. `[CERT]` `BRdbmsDeprecatedDialect.java:52`

**Indexes created at table-creation time:**

| Mode | Index name pattern | Indexed columns |
|---|---|---|
| `byHistoryId` | `ID_{tableId}_TS` | `TIMESTAMP` |
| `byHistoryType` | `IDX_{tableId}_ID_TS` | `HISTORY_ID, TIMESTAMP` |

`[CERT]` `BRdbmsHistoryExport.java:276,278` (index patterns), `RdbmsHistoryUtil.java` (createIndex DDL)

**Auto-creation:** if the data table does not exist, `exportRecords()` calls `createTable()`
automatically (no pre-existing schema required). `[CERT]` `BRdbmsHistoryExport.java:268-270`

**Schema-version detection:** `RdbmsHistoryUtil.isNewSchema()` checks for the presence of the
`DB_TIMEZONE` column via `DatabaseMetaData.getColumns()` to distinguish old-schema tables from
new ones. `[CERT]` `RdbmsHistoryUtil.java:182`

## 403.5 — Java-to-SQL Type Mapping: BRdbmsDeprecatedDialect.loadField() `[CERT]`

`loadField()` maps each `BHistoryRecord` property value to a JDBC parameter:

| Niagara type | JDBC setter | Edge-case handling |
|---|---|---|
| `BString` | `ps.setString()` | Truncated to `getVarcharSize(facets)` (default 500) |
| `BInteger` | `ps.setInt()` | — |
| `BLong` | `ps.setLong()` | — |
| `BFloat` | `ps.setFloat()` | NaN → `setNull(REAL)`; ±∞ → clamped to ±`MAX_VALUE` |
| `BDouble` | `ps.setDouble()` | NaN → `setNull(DOUBLE)`; ±∞ → clamped |
| `BAbsTime` | `ps.setTimestamp(idx, ts, cal)` | Calendar via `getCalendarForTimestamps()` (see §403.8); falls back to `ps.setLong(millis)` if `!supportsTimestamp()` |
| `BRelTime` | `ps.setLong(millis)` | — |
| `BBoolean` | `ps.setBoolean()` | — |
| `BStatus` | `ps.setInt(bits)` | Status bitfield stored as integer |
| `BTrendFlags` | `ps.setInt(bits)` | TrendFlags bitfield stored as integer |
| `BTimeZone` | `ps.setString(toString())` | Truncated to varcharSize |
| `BSimple` | `ps.setString(encodeToString())` | Encoded string representation |
| fallback | `ps.setString(toString())` | — |

`[CERT]` `BRdbmsDeprecatedDialect.java:518-579` (loadField), `BRdbmsDeprecatedDialect.java:597-610`
(insertRecord calling ps.addBatch)

**Per-dialect SQL DDL types** (used in CREATE TABLE):

| Niagara type | MySQL | Oracle | SQL Server |
|---|---|---|---|
| `BInteger` | `INTEGER` | `INT` | `INTEGER` |
| `BLong` | `BIGINT` | `INT` ¹ | `BIGINT` |
| `BFloat` | `FLOAT` | `REAL` | `REAL` |
| `BDouble` | `DOUBLE` | `FLOAT` | `DOUBLE PRECISION` |
| `BBoolean` | `TINYINT` | — ² | `BIT` |
| `BAbsTime` | `DATETIME` | `TIMESTAMP` | `DATETIME` |
| `BString` / VARCHAR | `VARCHAR` / `NVARCHAR` | `VARCHAR2` / `NVARCHAR2` | `VARCHAR` / `NVARCHAR` |
| UUID | — | `RAW(16)` | `UNIQUEIDENTIFIER` |
| BLOB | `MEDIUMBLOB` | `BLOB` | `IMAGE` |
| CLOB | `TEXT` | `CLOB` | `TEXT` / `NTEXT` |

¹ Oracle maps `BLong` to `INT` (not BIGINT) — possible precision loss for very large values.
`[INFER]` This is a known Oracle mapping quirk; no override was found in the source.

² Oracle dialect uses `getBooleanType()` but `supportsBooleanType()` determines whether it
emits a boolean column or falls back. Full Oracle boolean handling not traced here.

`[CERT]` `BMySQLDatabase.java` (inline dialect), `BOracleDatabase.java` (inline dialect),
`BSqlServerDatabase.java:191-229` (SQL Server type methods)

## 403.6 — JDBC Driver Loading and URL Construction `[CERT]`

Each dialect module bundles its own JDBC driver. Driver selection is implicit: instantiating
`BMySQLDatabase` (vs `BOracleDatabase` vs `BSqlServerDatabase`) loads the bundled driver.
There is no explicit `Class.forName()` at the rdb-rt level. All pools use **Apache Commons
DBCP2 `BasicDataSource`**.

### MySQL (`rdbMySQL-rt`)

```
Driver class : com.mysql.cj.jdbc.Driver   [CERT BMySQLConnectionPool.java:78]
Default port : 3306                        [CERT BMySQLDatabase.java]
URL pattern  : jdbc:mysql://{host}:{port}/{dbName}
               ?serverTimezone=UTC
               [&useUnicode=true&characterEncoding=UTF-8]  (if useUnicodeEncodingScheme)
               &useSSL={bool}&requireSSL={bool}
               [&enabledTLSProtocols={list}]               (if TLS enabled)
               [&verifyServerCertificate=true
                &trustCertificateKeyStoreUrl=...
                &trustCertificateKeyStorePassword=...]      (if verifySubjectInCertificate)
```
`[CERT]` `BMySQLConnectionPool.java:78,88-134`

**MySQL dialect traits:** `getInsertionMode()=INSERT_VIA_IDENTITY` (AUTO_INCREMENT);
`supportsMillisecondTimestamp()=false`; `useUtcTimestamps()=false` (timestamps stored in
local time by default). `[CERT]` `BMySQLDatabase.java` (inline dialect)

### Oracle (`rdbOracle-rt`)

```
Default port : 1521                        [CERT BOracleDatabase.java]
URL pattern  : jdbc:oracle:thin:@(DESCRIPTION=
                 (ADDRESS=(PROTOCOL={tcp|tcps})(HOST={host})(PORT={port}))
                 (CONNECT_DATA=(SERVICE_NAME={serviceName}))
                 {securityOptions})
Optional     : customJdbcUrl slot overrides the generated URL entirely
```
`[CERT]` `BOracleConnectionPool.java:147` (URL format string), `BOracleConnectionPool.java:27`
(CUSTOM_JDBC_URL slot)

**Oracle dialect traits:** `getInsertionMode()=INSERT_VIA_SEQUENCE` (uses sequences named
`{tableName}_Q`); `supportsMillisecondTimestamp()=true` (unless utcMillis mode);
`useUtcTimestamps()=true` when `timestampStorage=utcTimestamp`; `ORACLE_FETCH_SIZE=1000`.
`[CERT]` `BOracleDatabase.java`

### SQL Server (`rdbSqlServer-rt`)

```
Driver class : com.microsoft.sqlserver.jdbc.SQLServerDriver  [CERT BConnectionPool.java:115]
Default port : 1433                                          [CERT BSqlServerDatabase.java:76]
URL pattern  : jdbc:sqlserver://{host}:{port}
               [;databaseName={instance}]
               [;...extraConnectionProperties...]
               ;encrypt={bool}
               ;trustServerCertificate=false
               ;hostNameInCertificate={subject}
               ;trustManagerClass=SqlServerClientTrustManager
               ;trustManagerConstructorArg={absOrd}
               [;sendStringParametersAsUnicode=false]  (if not unicode mode and not already set)
```
`[CERT]` `BConnectionPool.java:115,125-187`

**SQL Server dialect traits:** `getInsertionMode()=INSERT_VIA_IDENTITY` (IDENTITY column);
`supportsBatchInsert()=false` ³; `supportsMillisecondTimestamp()=false`;
`useUtcTimestamps()=timestampStorage.equals(utcTimestamp)`; `getMaxTableName()=128`.
`[CERT]` `BSqlServerDatabase.java:105-106,117-118,153-154,235-241`

³ `supportsBatchInsert()=false` for SQL Server at the dialect-interface level. However,
history export calls `ps.addBatch()` / `ps.executeBatch()` unconditionally in
`BRdbmsDeprecatedDialect.insertRecord()`. `[INFER]` The `supportsBatchInsert()` flag is
consumed by other rdb-rt operations (DDL migration, alarm export) but not by the history
export batch loop — the Microsoft JDBC driver supports JDBC batch natively. Full resolution
would require tracing all callers of `supportsBatchInsert()`.

## 403.7 — Timestamp Handling `[CERT]`

`BRdbmsTimestampStorage` (enum on `BRdbms`) controls how `BAbsTime` values are stored:

| Ordinal | Tag | Behavior |
|---|---|---|
| 0 | `dialectDefault` | Let the dialect decide (`useUtcTimestamps()`) |
| 1 | `localTimestamp` | `ps.setTimestamp()` with JVM-local-timezone Calendar |
| 2 | `utcTimestamp` | `ps.setTimestamp()` with UTC Calendar |
| 3 | `utcMillis` | `ps.setLong(millis)` — stored as `BIGINT` epoch milliseconds |

`[CERT]` `BRdbmsTimestampStorage.java` (enum), `BRdbmsDeprecatedDialect` (getCalendarForTimestamps)

`BRdbmsDeprecatedDialect.getCalendarForTimestamps()` selects the `Calendar` instance:
`useUtcTimestamps()` on the dialect → UTC calendar; otherwise `historyConfigCalendar` (from the
`.hdb` history configuration) or local calendar. `[CERT]` `BRdbmsDeprecatedDialect.java:697-699`

The `lookupMaxTimestamp()` query (`SELECT MAX(TIMESTAMP) AS MAX_TIMESTAMP FROM {table}
[WHERE HISTORY_ID=?]`) reads back the latest exported timestamp to resume from on the next
export run, using the same calendar selection. `[CERT]` `BRdbmsHistoryExport.java:519-568`

## 403.8 — Primary Key Generation Strategy `[CERT]`

`RdbmsDialect` defines three insertion-mode constants:

| Constant | Value | Meaning |
|---|---|---|
| `INSERT_VIA_SEQUENCE` | 0 | Oracle sequences (`tableName_Q.NEXTVAL` in INSERT VALUES) |
| `INSERT_VIA_IDENTITY` | 1 | DB-generated identity; `ps.setNull()` for ID param or omit |
| `INSERT_VIA_IDENTITY_LOOKUP` | 2 | IDENTITY + post-INSERT lookup (not used by current dialects) |

`[CERT]` `RdbmsDialect.java:10-12`

Oracle creates a sequence per table: `CREATE SEQUENCE {tableName}_Q ...` at `createTable()`
time. The INSERT SQL includes `{tableName}_Q.NEXTVAL` as the ID value. MySQL uses
`AUTO_INCREMENT` (sequence omitted). SQL Server uses `IDENTITY`.
`[CERT]` `BOracleDatabase.java` (getInsertionMode=0, getSequenceName), `BSqlServerDatabase.java:117-118` (getInsertionMode=1)

## 403.9 — Asynchronous Dispatch: BRdbmsWorker `[CERT]`

`BRdbmsWorker` extends `BThreadPoolWorker`. Default configuration:

| Slot | Default | Constraint |
|---|---|---|
| `maxThreads` | 1 | min 1 |
| `maxQueueSize` | 1000 | — |

Uses a `CoalesceQueue` internally. Items with the same coalesce key are merged, so redundant
export triggers do not stack up. Thread is named `"RdbmsWorker:{parentName}"`.

`postAsync(Runnable r)` enqueues the `Invocation` wrapping `doExecute` into the queue. All
export work is fully asynchronous from the caller's perspective; `postExecute()` returns `null`
immediately. `[CERT]` `BRdbmsWorker.java:34,60-67`

## 403.10 — Security: TLS Transport and AES Field Encryption `[CERT]`

### TLS (BEncryptableTransportRdbms)

All three concrete dialect classes extend `BEncryptableTransportRdbms` which adds:

| Slot | Default | Purpose |
|---|---|---|
| `tlsMinProtocol` | `BSslTlsEnum.tlsv1_2` (TLSv1.2) | Minimum TLS version for encrypted connections |
| `verifySubjectInCertificate` | `true` | Validates server certificate subject |

Each dialect manages a per-instance truststore (`KeyStore`) stored in
`stationHome/rdb{Dialect}/`. The truststore password is stored in the NRE KeyRing.
`[CERT]` `BEncryptableTransportRdbms.java:32-42,74,101`

TLS parameters flow into the JDBC URL (MySQL/SQL Server) or into the Oracle TNS protocol
field (`PROTOCOL=tcps`) and OJDBC connection properties. `[CERT]` `BMySQLConnectionPool.java:113-131`,
`BOracleConnectionPool.java:147,153-172`, `BConnectionPool.java:176-180`

### AES field-level encryption (BRdbSecuritySettings)

`BRdbSecuritySettings` provides AES-256 encryption for password-type columns stored
in the external SQL table (not for JDBC credentials):

- Key derivation: PBKDF2WithHmacSHA256, 65536 iterations, 256-bit key
- Auto-generates random passkey and salt on station start if not configured
- `enterPassKey` action; `changeOfPassKey` topic

`[CERT]` `BRdbSecuritySettings.java` (class body, PBKDF2 constants and action defs)

## 403.11 — Connection Pool Management `[CERT]`

All three dialect modules use Apache Commons DBCP2 `BasicDataSource`. The pool is
initialized lazily on first `getConnection()` call and re-initialized if the JDBC URL,
username, or password changes. Pool parameters (`maxActive`, `maxIdle`, `maxWait`) are
inherited from `BAbstractConnectionPool`.

`BRdbmsSession` (the `sql:` URL-scheme navigation session) holds its own separate JDBC
`Connection` used only for interactive SQL browsing in Workbench. It does not participate
in the history export write path. `[CERT]` `BRdbmsSession.java:21,31`

## 403.12 — Scope Boundary: BRdbArchiveHistoryProvider (DB6) `[CERT]`

`BRdbArchiveHistoryProvider extends BArchiveHistoryProvider` is the **read-back path**: it
serves archived history data FROM the external RDBMS TO Niagara history queries
(`doTimeQuery()`). It is the inverse direction of `BRdbmsHistoryExport`.

It maintains a static `WeakHashMap<BRdbms, Map<BHistoryId, List<BRdbmsHistoryExport>>>` cache
(`RDBMS_EXPORT_MAP`, line 157) to locate the matching export descriptor and resolve the SQL
table name via `HISTORY_CONFIG` / `HISTORY_TYPE_MAP`. It does not call `doExecute()` and does
not participate in the INSERT pipeline. `[CERT]` `BRdbArchiveHistoryProvider.java:129,157`
(structural sub-agent read, not independently re-verified by orchestrator)

The trigger and batching logic of `BArchiveHistoryProvider` — what schedules the archival,
cron/capacity triggers, fail/retry — is gap **DB6**, deferred to a future block.

---

## 403.x — Self-Verify

| Claim | Marker | Citation |
|---|---|---|
| DEFAULT_EXPORT_BATCH_SIZE=1000, sysprop `niagara.rdb.historyExport.batchSize` | `[CERT]` | `BRdbmsHistoryExport.java:96-99` |
| `doExecute()` is the write entry point | `[CERT]` | `BRdbmsHistoryExport.java:138` |
| `autoCommit=false` on JDBC connection | `[CERT]` | `BRdbmsHistoryExport.java:153-157` |
| `postExecute()` dispatches asynchronously via `BRdbmsWorker.postAsync()` | `[CERT]` | `BRdbmsHistoryExport.java:220-226` |
| Two export modes: byHistoryId (0, default) / byHistoryType (1) | `[CERT]` | `BRdbmsHistoryExportMode.java:11-18` |
| byHistoryId → meta table `HISTORY_CONFIG`; byHistoryType → `HISTORY_TYPE_MAP` | `[CERT]` | `BRdbmsHistoryExport.java:591-600` |
| HISTORY_ID VARCHAR(500) extra column added for byHistoryType | `[CERT]` | `BRdbmsHistoryExport.java:339`, `BRdbmsDeprecatedDialect.java:52` |
| TRENDFLAGS_TAG + STATUS_TAG added for BTrendRecord | `[CERT]` | `BRdbmsHistoryExport.java:316-318` |
| byHistoryId index: `ID_{tableId}_TS` on TIMESTAMP | `[CERT]` | `BRdbmsHistoryExport.java:276` |
| byHistoryType index: `IDX_{tableId}_ID_TS` on `HISTORY_ID, TIMESTAMP` | `[CERT]` | `BRdbmsHistoryExport.java:278` |
| Default VARCHAR width = 500 | `[CERT]` | `BRdbmsDeprecatedDialect.java:52` |
| NaN float → setNull; ±Inf → clamped to MAX_VALUE | `[CERT]` | `BRdbmsDeprecatedDialect.java:518-579` |
| BStatus / BTrendFlags stored as int bits | `[CERT]` | `BRdbmsDeprecatedDialect.java:518-579` |
| Table auto-created if absent | `[CERT]` | `BRdbmsHistoryExport.java:268-270` |
| Identifier mangling: strip non-alphanum, uppercase, reserved words remapped | `[CERT]` | `BRdbmsDeprecatedDialect.java:659-664` |
| Oracle: INSERT_VIA_SEQUENCE (sequences `tableName_Q`) | `[CERT]` | `BOracleDatabase.java` (getInsertionMode=0) |
| Oracle maps BLong → INT (not BIGINT) | `[CERT]` | `BOracleDatabase.java` (getLongType) |
| MySQL driver: `com.mysql.cj.jdbc.Driver` | `[CERT]` | `BMySQLConnectionPool.java:78` |
| MySQL URL: `jdbc:mysql://{host}:{port}/{db}?serverTimezone=UTC&...` | `[CERT]` | `BMySQLConnectionPool.java:88-134` |
| MySQL: supportsMillisecondTimestamp=false | `[CERT]` | `BMySQLDatabase.java` (dialect) |
| Oracle URL: TNS-style `jdbc:oracle:thin:@(DESCRIPTION=...)` | `[CERT]` | `BOracleConnectionPool.java:147` |
| Oracle: customJdbcUrl slot overrides generated URL | `[CERT]` | `BOracleConnectionPool.java:27` |
| SQL Server driver: `com.microsoft.sqlserver.jdbc.SQLServerDriver` | `[CERT]` | `BConnectionPool.java:115` |
| SQL Server URL: `jdbc:sqlserver://{host}:{port}[;databaseName=...][;encrypt=...][;...]` | `[CERT]` | `BConnectionPool.java:133-180` |
| SQL Server: INSERT_VIA_IDENTITY (IDENTITY column) | `[CERT]` | `BSqlServerDatabase.java:117-118` |
| SQL Server: supportsBatchInsert=false (dialect flag; INSERT path uses batch anyway) | `[CERT]` / `[INFER]` | `BSqlServerDatabase.java:105-106` + deduction |
| SQL Server: DATETIME, BIT, INTEGER, BIGINT, REAL, DOUBLE PRECISION types | `[CERT]` | `BSqlServerDatabase.java:191-229` |
| SQL Server maxTableName=128 | `[CERT]` | `BSqlServerDatabase.java:153-154` |
| BRdbmsWorker: maxThreads=1, maxQueueSize=1000 | `[CERT]` | `BRdbmsWorker.java` (slots) |
| TLS min protocol default TLSv1.2 | `[CERT]` | `BEncryptableTransportRdbms.java:33-36` |
| AES-256 / PBKDF2WithHmacSHA256 / 65536 iterations for field encryption | `[CERT]` | `BRdbSecuritySettings.java` |
| RdbmsDialect.INSERT_VIA_SEQUENCE=0, INSERT_VIA_IDENTITY=1 | `[CERT]` | `RdbmsDialect.java:10-11` |
| BRdbArchiveHistoryProvider is NOT the write path; RDBMS_EXPORT_MAP at line 157 | `[CERT]` | `BRdbArchiveHistoryProvider.java:129,157` (sub-agent) |

**Self-verify tally:** 36 claims — 34 `[CERT]`, 1 `[CERT]`/`[INFER]` mixed (SQL Server batch flag),
1 `[INFER]` (Oracle BIGINT quirk consequence). Zero unsupported assertions.

## 403.x — Connections

- **[Block 33]** — `.hdb` format (MAGIC, paged, two versions). B403 reads from `.hdb` via
  `historyConn.timeQuery()` cursor; the format internals are in B33.
- **[Block 8]** — `BHistoryExport` / `BHistoryImport` abstraction mentioned in B8 §8.2.6.
  B403 documents the concrete RDBMS export subclass.
- **[Block 34]** — `.adb` alarm format. Parallel persistence layer; the alarm RDBMS backend
  (`alarmOrion`) is gap DB3, a separate investigation.
- **[Block 402]** — DB1: station save trigger (BOG). B402 and B403 together close the two
  high-priority `database` gaps; both are write-path pipelines to different backends.
- **Gap DB6** — `BArchiveHistoryProvider` trigger/batching: what schedules archival, cron vs
  capacity triggers, fail/retry. B403 touches `BRdbArchiveHistoryProvider` only as a boundary
  (§403.12); DB6 is the full investigation.
- **Gap DB8** — HSQLDB embedded adapter (`rdbHsqlDb-rt` / `nHsqlDb-rt`): role as embedded
  SQL backend vs optional feature. Not covered here.
