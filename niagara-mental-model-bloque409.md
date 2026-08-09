# Block 409 — Embedded HSQLDB in Niagara N4: BHsqlDatabase as the Controller-Resident rdb-rt Dialect

> **Research focus:** `database` (gap **DB8**, low-priority). Answers whether embedded HSQLDB
> (`rdbHsqlDb-rt`) is a backend for the rdb-rt driver, a standalone embedded SQL server, or only
> for optional features. Scope: the Niagara adapter classes (`com.tridium.rdb.hsqldb.*`) and the
> `module.xml` dependency graph. Does NOT document the `org.hsqldb.*` 3rd-party library internals
> bundled in the same JAR.
>
> Subject version: N4.14.0.162 (Vineflower decompiled corpus + module.xml + official help guides).
>
> Sources:
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdbHsqlDb/rdbHsqlDb-rt/vineflower/com/tridium/rdb/hsqldb/BHsqlDatabase.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdbHsqlDb/rdbHsqlDb-rt/vineflower/com/tridium/rdb/hsqldb/HsqlConnection.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/rdbHsqlDb/rdbHsqlDb-rt/vineflower/META-INF/module.xml`
> - `[CERT-doc]` `niagara-help/guides-clean/Rdbms/DatabaseRequirement-Rdbms-7C2CBFED.txt` (N4.14.0.162; sha256 `c3a0713c6714ee17…`)
> - `[CERT-doc]` `niagara-help/guides-clean/Rdbms/RDBMSModules-7C17FB3B.txt` (N4.14.0.162; sha256 `3f392284a23714e6…`)
> - `[CERT-doc]` `niagara-help/guides-clean/Rdbms/SecurityBestPractices-CC28C36C.txt` (N4.14.0.162; sha256 `fb0dbaae93b85390…`)
>
> Short path alias used in citations: `BHsqlDatabase.java` = above full path + file.
>
> Method: Vineflower decompile (Niagara adapter); module.xml; niagara-help official guides (corroboration).
> Markers:
> `[CERT]` local primary source (`file:line`) ·
> `[CERT-doc]` official downloaded document (guide:line) ·
> `[INFER]` deduction.
>
> `database` focus. Connects [Block 403] (rdb-rt pipeline — HSQLDB is its 4th dialect, contrasted
> with MySQL/Oracle/SQL Server covered there). [Block 29] (§29.4.7 noted `org.hsqldb.server.Servlet`
> in `rdbHsqlDb-rt.jar`).

---

## 409.1 — Role Answer: Fourth rdb-rt Dialect, Controller-Resident `[CERT]` `[CERT-doc]`

The gap question ("backend for rdb-rt, standalone embedded SQL server, or only for optional features")
resolves to: **HSQLDB is the fourth dialect of the rdb-rt driver, pre-deployed in remote controller
stations (JACE/WEB-8000).** It is NOT a standalone embedded SQL server and NOT optional in the sense
of being for peripheral features — it is the standard RDBMS path on controllers.

| Deployment tier | RDBMS module | Physical location |
|---|---|---|
| Remote controller (JACE/WEB-8000) | `rdbHsqlDb` — `BHsqlDatabase` | Embedded in station JVM, files under `${station.home}/hsqldb/` |
| Supervisor station (PC) | `rdbMySQL`, `rdbOracle`, `rdbSqlServer` | External server process, network JDBC |

`[CERT-doc]` `DatabaseRequirement-Rdbms-7C2CBFED.txt:22` — "An HsqlDbDatabase is configured at
the factory to run in each remote controller."

`[CERT-doc]` `RDBMSModules-7C17FB3B.txt:55-59` — "rdbHsqlDb (optional) … This database resides
in a remote controller station." vs. `RDBMSModules-7C17FB3B.txt:67` — "This database [MySQLDatabase]
resides in a Supervisor station."

`[CERT-doc]` `SecurityBestPractices-CC28C36C.txt:83` — "HSQLDB is used internally only as a file
system DB."

`[CERT-doc]` `DatabaseRequirement-Rdbms-7C2CBFED.txt:14-15` — "The RDBMS driver supports four
third-party databases: MySQL, OracleDatabase or SqlServerDatabase, and HsqlDbDatabase."

**Contrast with [Block 403] MySQL/Oracle/SQL Server:** All four share the same rdb-rt abstract
pipeline (`BRdbmsHistoryExport`, `BRdbmsWorker`, `BRdbmsDeprecatedDialect`). HSQLDB is the
controller-local flavor; the other three are Supervisor-side network databases. `[INFER]` from
`module.xml` dependency on `rdb-rt` + class hierarchy (§409.2).

---

## 409.2 — Adapter Class Structure: BHsqlDatabase `[CERT]`

The Niagara adapter has exactly **three classes** in `com.tridium.rdb.hsqldb`:

| Class | Role |
|---|---|
| `BHsqlDatabase` | Main adapter — `@NiagaraType`, extends `BRdbms`, implements `RdbmsDialect` inline (anonymous) |
| `HsqlConnection` | Privilege-escalating wrapper over `java.sql.Connection` |
| `HsqlStatement` | Privilege-escalating wrapper over `java.sql.Statement` |

`BHsqlDatabase` extends `BRdbms` directly — **not** `BEncryptableTransportRdbms` (which MySQL/Oracle/
SQL Server use for TLS slots). HSQLDB therefore has no TLS slots. `[CERT]` `BHsqlDatabase.java:121`

The entire `RdbmsDialect` contract is implemented as a single anonymous inner class field:
`[CERT]` `BHsqlDatabase.java:137` (`private final RdbmsDialect dialect = new RdbmsDialect() { … }`)

The module exports exactly one Niagara type:
`[CERT]` `module.xml` — `<type class="com.tridium.rdb.hsqldb.BHsqlDatabase" name="HsqlDatabase"/>`

The module depends on `rdb-rt` (confirming dialect role) and a broad set of base modules:
`[CERT]` `module.xml` — `<dependency name="rdb-rt" vendor="Tridium" vendorVersion="4.14.0"/>`

License gate: `getLicenseFeature()` checks `"tridium", "rdbHsqlDb"` — a separate license feature
distinct from the base `rdb` feature. `[CERT]` `BHsqlDatabase.java:606-608`

---

## 409.3 — Connection Mechanics: Embedded File-Backed, No Pool `[CERT]`

The JDBC URL is always the **embedded file mode**:
```
"jdbc:hsqldb:file:" + home + File.separator + databaseName
```
`[CERT]` `BHsqlDatabase.java:672`

`home` is derived from the `baseDirectory` property, which defaults to `file:^^hsqldb`
(station home → `hsqldb/` subdirectory). `[CERT]` `BHsqlDatabase.java:77`

Connection is established by **directly instantiating HSQLDB's internal `JDBCConnection`**
(bypassing the DBCP pool used by MySQL/Oracle/SQL Server):
```java
Connection conn = AccessController.doPrivileged(
    (PrivilegedExceptionAction<JDBCConnection>)(() -> new JDBCConnection(hprops)));
return new HsqlConnection(conn);   // → privilege-elevating wrapper
```
`[CERT]` `BHsqlDatabase.java:695-696`

Every JDBC operation in `HsqlConnection` and `HsqlStatement` is wrapped in
`AccessController.doPrivileged()`. This is necessary because HSQLDB reads/writes files directly
and the Niagara station security manager would otherwise block raw file I/O from station-level code.
`[CERT]` `HsqlConnection.java:33-40` (example: `createStatement()` dispatch)

**Consequence:** HSQLDB runs **in-process** inside the station JVM — there is no separate database
server process, no network socket for the connection. The "embedded" label is literal. `[INFER]`
from `jdbc:hsqldb:file:` URL (contrast with `jdbc:hsqldb:hsql://` for server mode, mentioned in
`SecurityBestPractices-CC28C36C.txt:83` as "future use case").

---

## 409.4 — Dialect Properties: No Batch Insert, Checkpoint, SHUTDOWN `[CERT]`

Key dialect differences vs. MySQL/Oracle/SQL Server (all from the anonymous `RdbmsDialect` impl):

| Property | HSQLDB value | MySQL/Oracle/SQL Server |
|---|---|---|
| `supportsBatchInsert()` | `false` | `true` |
| `supportsBatchUpdate()` | `false` | `true` |
| `supportsBatchDelete()` | `true` | `true` |
| `getInsertionMode()` | `2` (insert-per-row) | `0` (batch) |
| TLS | none (no `BEncryptableTransportRdbms`) | optional |
| Identity | `GENERATED BY DEFAULT AS IDENTITY` + `CALL IDENTITY()` | sequence-based |

`[CERT]` `BHsqlDatabase.java:166-180` (batch flags and insertion mode)

**Lifecycle events:**

- **On start (`rdbmsStarted()`):** adds a scheduled periodic link (`defragAndSavePeriodicScheduleLink`)
  that fires a `CHECKPOINT` or `CHECKPOINT DEFRAG` SQL at the configured interval (default: every
  30 days). `[CERT]` `BHsqlDatabase.java:468-475`
- **On stop (`stopped()`):** executes `SHUTDOWN` SQL to close the embedded DB cleanly before the
  station JVM exits. `[CERT]` `BHsqlDatabase.java:503-517` (`statement.execute("SHUTDOWN")` at `:509`)

The first connection also checks for a HSQLDB schema version upgrade (pre-2.5 → 2.x) by reading
the `.properties` file and patching the `.script` file if needed. `[CERT]` `BHsqlDatabase.java:711-769`

---

## 409.5 — nHsqlDb-rt: Proven Absent `[CERT]`

The gap specification mentioned a possible separate adapter module named `nHsqlDb-rt`. No such module
exists in the corpus.

- Filesystem search of `organized/` found no directory or JAR matching `nHsqlDb*`. `[CERT]` — search
  result: empty (0 hits).
- Module-navigator full-corpus grep for the string `"nHsqlDb"` across 50,798 files returned
  zero matches. `[CERT]` — module-navigator grep output: "No matches for /nHsqlDb/ (50,798 files
  searched)."

**Conclusion:** `nHsqlDb-rt` is a false name; the only HSQLDB adapter is `rdbHsqlDb-rt`. The term
may stem from a Niagara AX naming convention (`nHsqlDb`) that was not carried forward to N4.
`[INFER]`

---

## 409.6 — Connections

- **[Block 403]** — documents the rdb-rt RDBMS export pipeline (MySQL, Oracle, SQL Server). Block
  409 adds HSQLDB as the fourth dialect of that same pipeline — sharing `BRdbmsHistoryExport`,
  `BRdbmsWorker`, and `BRdbmsDeprecatedDialect` — while being the controller-specific flavor (no
  network, embedded file-backed). B403 §403.1 listed `BHsqlDatabase (DB8)` as out of scope; B409
  closes that scope boundary.

- **[Block 29]** — §29.4.7 noted `org.hsqldb.server.Servlet` inside `rdbHsqlDb-rt.jar`. That
  class is from the bundled HSQLDB 3rd-party library and would be used only if HSQLDB were
  configured in server mode (`jdbc:hsqldb:hsql://`) — which the official security guide marks as
  "future use case." In normal controller deployments, the embedded file mode is used and that
  servlet is dormant.
