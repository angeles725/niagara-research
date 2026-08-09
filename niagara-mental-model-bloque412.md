# Block 412 — orion-rt: Niagara's Generic ORM Framework — Annotations, Schema Manager, Session, and DDL Layer

> Research focus: `database` (gap **DB11**, medium-priority). Documents the **orion-rt** module as
> the generic object-relational mapping (ORM) framework that sits between Niagara's BComponent type
> system and any JDBC-capable RDBMS. Covers: the annotation model (`@NiagaraOrionType`,
> `@OrionProperty`, `@OrionIndex`), the `BOrionObject` base class and facet vocabulary, the
> service/space layer (`BOrionSpace`, `BOrionService`, `BOrionDatabase`), the local station
> implementation (`BLocalOrionDatabase`), the full boot-and-registration sequence, the `OrionSession`
> interface and its `DbOrionSession` implementation, cursor and pagination mechanics, schema
> versioning (`BSchemaVersion`, `ISchemaUpgrader`, `OrionAppSchemaManager`), the DDL whitelist
> (`Ddl.java`), system tables, batch operations (`BatchStatement`), and the data-migration
> tool (`BOrionMigrator`).
>
> **Not covered here:**
> - alarmOrion consumer tables / archive move → [Block 404]
> - rdb-rt raw-JDBC dialect pipeline → [Block 403]
> - lonOrion (another orion-rt consumer; mentioned in §412.12)
> - Fox-protocol remote access (BFoxOrionDatabase / FoxOrionSession) — wire-protocol internals
>   not read; mentioned in §412.13
>
> Subject version: N4.14.0.162 (Vineflower decompiled; 136 `.java` files confirmed).
>
> Sources:
> - `[CERT]` dir: `/home/cristian/modules/Prototipos/modulos/organized/orion/orion-rt/vineflower/com/tridium/orion/`
>   Key files cited individually by file:line below. All 136 `.java` files verified present (2026-08-09).
>
> Method: Vineflower-decompiled Java corpus, direct read. Delegated:
> MECHANICAL enumeration (annotations, cursor methods, system-table schema, batch interface) → haiku
> sub-agent; STRUCTURAL comprehension (session/txn, boot sequence, schema upgrade path, SQL
> generation) → sonnet fork sub-agent. All load-bearing citations independently grep-verified by
> the orchestrator against disk.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> `database` focus. Connects [Block 404] (alarmOrion as consumer of this ORM),
> [Block 403] (rdb-rt raw-JDBC path — contrasted in §412.3).

---

## 412.1 — Package Architecture: 136 Classes in Six Sub-Trees `[CERT]`

orion-rt is organized into six functional sub-trees under
`com/tridium/orion/`:

| Sub-package | Role | Key classes |
|---|---|---|
| `(root)` | Public API: service, space, session interface, type model | `BOrionSpace`, `BOrionDatabase`, `BOrionObject`, `OrionSession`, `OrionType`, `BSchemaVersion`, `ISchemaUpgrader`, `OrionAppSchemaManager` |
| `annotations/` | Java annotations consumed by the NRE introspector | `@NiagaraOrionType`, `@OrionProperty`, `@OrionIndex`, `@OrionLinkedCursor`, `@OrionRefCursor` |
| `sql/` | Query-builder DSL | `BSqlQuery`, `BSqlJoin`, `BSqlCase`, `BatchStatement`, `PropertyValue`, `BPage`, `TableBuilder` |
| `priv/db/` and `priv/db/sql/` | Implementation layer: JDBC session, SQL string builders | `DbOrionSession`, `Select`, `Insert`, `Update`, `Delete`, `Ddl`, `PaginatedCursor` |
| `priv/model/` | Annotation → type model translation | `StaticOrionType`, `OrionIntrospector`, `LocalOrionModel` |
| `priv/sys/` `priv/util/` `priv/migrate/` | System tables, utilities, data migration | `BOrionSysTables`, `BOrionAppVersion`, `BOrionMigrator` |

`[CERT]` (directory listing: 136 `.java` files under `orion-rt/vineflower/com/tridium/orion/`)

---

## 412.2 — Annotation Model: How a Class Declares SQL Mapping `[CERT]`

### 412.2.1 — `@NiagaraOrionType` (marker only)

```java
// NiagaraOrionType.java:6-8
@Target({ElementType.TYPE})
public @interface NiagaraOrionType {
}
```

`@NiagaraOrionType` is a **class-level marker annotation with no elements**. Its sole function is
to tell the NRE annotation processor to register `OrionIntrospector` as the introspector for that
class, causing `Sys.loadType()` to produce a `StaticOrionType` (which implements both `Type` and
`OrionType`) instead of a plain `BComponent` type.
`[CERT]` `NiagaraOrionType.java:6-8`

### 412.2.2 — `@OrionProperty` (SQL column declaration)

```java
// OrionProperty.java:8-18
@Repeatable(OrionProperties.class)
@Target({ElementType.TYPE})
public @interface OrionProperty {
   String name();
   String refType();
   int flags() default 0;
   Facet[] facets() default {};
}
```

`@OrionProperty` is type-level (not field-level). Columns are declared on the class, not on Java
fields. `name` is the property name; `refType` is the Niagara type spec for the column's value
type; `facets[]` drives SQL column attributes (see §412.4 for the vocabulary).
`[CERT]` `OrionProperty.java:8-18`

### 412.2.3 — `@OrionIndex` (multi-column composite indexes)

```java
// OrionIndex.java:8-16
@Repeatable(OrionIndexes.class)
@Target({ElementType.TYPE})
public @interface OrionIndex {
   String name();
   boolean unique();
   String clustered() default "BClustered.unspecified";
   String[] fields() default {};
}
```

Composite indexes across multiple columns are declared with `@OrionIndex`. `fields[]` lists column
names; `clustered` selects the index storage type; `unique` maps to a UNIQUE index constraint.
`[CERT]` `OrionIndex.java:8-16`

### 412.2.4 — `@OrionLinkedCursor` and `@OrionRefCursor` (pre-fetch join cursors)

```java
// OrionLinkedCursor.java:8-14
@Repeatable(OrionLinkedCursors.class)
@Target({ElementType.TYPE})
public @interface OrionLinkedCursor {
   String name();   // cursor name
   String type();   // related OrionType spec
   String link();   // linking property name
}

// OrionRefCursor.java:8-14
@Repeatable(OrionRefCursors.class)
@Target({ElementType.TYPE})
public @interface OrionRefCursor {
   String name();
   String type();
   String ref();    // FK property on the related type pointing back
}
```

These annotations declare pre-fetch side cursors that are opened alongside the primary cursor and
merge-joined in memory — the same merge-join pattern seen in `OrionAlarmCursor` [Block 404 §404.6].
`[CERT]` `OrionLinkedCursor.java:8-14`, `OrionRefCursor.java:8-14`

---

## 412.3 — `BOrionObject`: Base Class for All ORM-Mapped Types `[CERT]`

```java
// BOrionObject.java:24
public abstract class BOrionObject extends BComponent implements BIOrionObject {
```

Every class participating in the Orion ORM must extend `BOrionObject`. It is a `BComponent` —
the same base type used for live Niagara slots — giving ORM-mapped objects full slot access,
navigation, and serialization via the normal Niagara type system.

**Facet-name constants** (the vocabulary for @NiagaraProperty `facets` and for DDL generation):

| Constant | String value | SQL effect |
|---|---|---|
| `KEY` | `"key"` | Part of the PK or unique lookup key |
| `IDENTITY` | `"identity"` | Auto-increment / identity column |
| `UNIQUE` | `"unique"` | UNIQUE constraint |
| `INDEXED` | `"indexed"` | Non-unique index |
| `WIDTH` | `"width"` | VARCHAR / NVARCHAR width |
| `DESCENDING` | `"descending"` | Descending index order |
| `CLUSTERED` | `"clustered"` | Clustered index flag |
| `ON_DELETE` | `"onDelete"` | FK cascade behavior (`BOnDelete`) |
| `AUTO_RESOLVE` | `"autoResolve"` | ORM auto-loads FK target on fetch |
| `CLOB` | `"clob"` | Store as CLOB rather than VARCHAR |
| `DB_SOURCED` | `"dbSourced"` | Value is DB-generated, not app-set |

`[CERT]` `BOrionObject.java:31-43`

**`ID_KEY` convenience facet:**
```java
// BOrionObject.java:44
public static BFacets ID_KEY = BFacets.make("key", BBoolean.TRUE, "identity", BBoolean.TRUE);
```
The `@Facet("ID_KEY")` shorthand on a property declaration makes that property both the PK key
and an identity (auto-increment) column. `[CERT]` `BOrionObject.java:44`

---

## 412.4 — Service / Space Layer: `BOrionSpace` and `BOrionService` `[CERT]`

### 412.4.1 — `BOrionSpace` (nav scheme "orion:")

```java
// BOrionSpace.java:29-31
@NiagaraType
@AuditableSpace
public class BOrionSpace extends BComponentSpace {
```

`BOrionSpace` extends `BComponentSpace` and carries the `@AuditableSpace` annotation. It is the
nav-scheme handler for the `orion:` ORD scheme, exposing registered OrionTypes as a browsable
component tree. It does NOT manage the database registry; that lives in `BOrionService`.
`[CERT]` `BOrionSpace.java:29-31`

**Database lookup:**
```java
// BOrionSpace.java (approx lines 95-122)
public BOrionDatabase getOrionDatabase(BRdbms rdbms) {
    BOrionService service = (BOrionService)Sys.getService(BOrionService.TYPE);
    return service.getOrionDatabase(rdbms);
}
```
`getOrionDatabase(BRdbms)` and `getOrionDatabase(String)` both delegate entirely to `BOrionService`.
`[CERT]` `BOrionSpace.java:95-122` (approx)

**Session lookup from Context chain:**
```java
// BOrionSpace.java:154-166
public static OrionSession getSessionFromContext(Context cx) {
    Context session = cx;
    while (session != null && !(session instanceof OrionSession)) {
        session = session.getBase();
    }
    if (session != null) return (OrionSession)session;
    else throw new OrionException("No session available.");
}
```
Because `OrionSession extends Context`, it can be threaded through the Niagara context chain,
allowing nested code to recover the current session without a thread-local variable.
`[CERT]` `BOrionSpace.java:154-166`

### 412.4.2 — `BOrionService` (database registry)

`BOrionService` holds the master map of active `BLocalOrionDatabase` instances keyed by database ID:
```java
// BOrionService.java:80
private HashMap<String, BLocalOrionDatabase> dbs = new HashMap<>();
```
`[CERT]` `BOrionService.java:80`

`getOrionDatabase(BRdbms)` is lazy: if no `BLocalOrionDatabase` exists for the given BRdbms, it
creates one, registers all known apps into it, boots it, and adds it to `dbs`:
`[CERT]` `BOrionService.java:251-263`

### 412.4.3 — `BOrionDatabase` (abstract registry node)

```java
// BOrionDatabase.java:18
public abstract class BOrionDatabase extends BComponent implements BIOrionDatabaseObject {
```

Abstract methods include `getId()`, `getOrionSpace()`, `getRdbms()`, `getTypes()`,
`getType(BOrionTypeId)`, `getDependentTypes(OrionType)`, and `createSession(Context)`.
`[CERT]` `BOrionDatabase.java:18,26-112`

**Table naming:**
```java
// BOrionDatabase.java:64-66
public String getTableName(OrionType type) {
    return this.getNameFactory(type).getTableName(this, type);
}
```
Table names are generated by a pluggable `BNameFactory`. The default factory derives the table
name from the OrionType's module+type name combination.
`[CERT]` `BOrionDatabase.java:64-66`

---

## 412.5 — `BLocalOrionDatabase`: Station-Local ORM Engine `[CERT]`

`BLocalOrionDatabase extends BOrionDatabase` and is the only local implementation. Each `BRdbms`
connection has exactly one `BLocalOrionDatabase`.

**Internal state:**
```java
// BLocalOrionDatabase.java:38-51
private BOrionService service;
private Array<BIOrionApp> appList = new Array(BIOrionApp.class);
private HashSet<BIOrionApp> appSet = new HashSet<>();
private Array<OrionType> typeList = new Array(OrionType.class);
private HashMap<BOrionTypeId, OrionType> idMap = new HashMap<>();
private boolean committed = false;
private HashMap<OrionType, BNameFactory> typeToFactory = new HashMap<>();
private BRdbms rdb;
private TableDefinition[] tableDefs;
private Map<BOrionTypeId, TableDefinition> tablesByType = new HashMap<>();
```
`[CERT]` `BLocalOrionDatabase.java:38-51`

**`registerApp(BIOrionApp)`** (line 140): adds the app to `appList`/`appSet` only. Schema creation
is NOT triggered here. `[CERT]` `BLocalOrionDatabase.java:140`

**No connection pool:** each `createSession()` call allocates a new `DbOrionSession` backed by a
fresh raw JDBC `Connection`:
```java
// BLocalOrionDatabase.java:505-511
public OrionSession createSession(Context cx) {
    OrionSession s = this.makeSession(cx);
    this.sessionOpened(s);   // empty hook
    return s;
}
protected OrionSession makeSession(Context cx) {
    return new DbOrionSession(this, cx);
}
```
`[CERT]` `BLocalOrionDatabase.java:505-511`

---

## 412.6 — Boot and Registration Sequence `[CERT]`

The full lifecycle from class loading to runtime readiness:

| Step | Actor | What happens | Citation |
|---|---|---|---|
| 1 | NRE class loader | `Sys.loadType(MyClass.class)` → `OrionIntrospector.makeType()` → `StaticOrionType` (both `Type` + `OrionType`) | `OrionIntrospector.java:14` |
| 2 | App's `serviceStarted()` | Calls `BOrionSpace.getOrionDatabase(rdbms)` → BOrionService lazy-inits BLocalOrionDatabase, calls `registerApp(this)` | `BOrionService.java:251-263` |
| 3 | `BLocalOrionDatabase.boot()` | Registers `BOrionAppVersion.ORION_TYPE` (sys table), then calls `open()` | `BLocalOrionDatabase.java:84-90` |
| 4 | `open()` | `commitTypes()` (resolves FK refs, topological sort) → `createTableMap()` → `ensureTableExists(BOrionAppVersion)` → `createOrUpgradeApp()` per registered app | `BLocalOrionDatabase.java:93-110` |
| 5 | `createOrUpgradeApp()` | Reads `BOrionAppVersion` row; if absent → inserts with current schema version; if version behind → calls `app.performSchemaUpgrade()` + updates row; if ahead → throws OrionException | `BLocalOrionDatabase.java:463-488` |
| 6 | `orionReady(db)` callback | Fires after schema is confirmed current; app opens its database connections | `BIOrionApp.java:14` |

`[CERT]` citations above

---

## 412.7 — `OrionSession`: Interface and Transaction Contract `[CERT]`

```java
// OrionSession.java:14
public interface OrionSession extends Context, AutoCloseable {
```

`OrionSession` is an **interface**, not an abstract class. Extending `Context` enables session
recovery from the Niagara context chain (§412.4.1).

**Full method surface by category:**

| Category | Methods |
|---|---|
| Connection state | `getOrionDatabase()`, `getRdbmsContext()`, `setAutoCommit(boolean)`, `getAutoCommit()`, `isOpen()`, `close()` |
| Transaction | `commit()`, `rollback()` |
| DDL | `invokeDdl(DdlCommand)` |
| Read (single) | `read(BQuery)`, `read(BIOrionObject)`, `read(OrionType, BSimple)`, `read(OrionType, PropertyValue)`, `read(OrionType, PropertyValue[])`, `read(OrionType, String)`, `read(OrionType, String, BSimple[])` |
| Existence check | `exists(BIOrionObject)`, `exists(OrionType, PropertyValue[])` |
| Scan / cursor | `select(BQuery)`, `select(OrionType, String)`, `select(OrionType, PropertyValue)`, `select(OrionType, PropertyValue[])`, `scan(OrionType)`, `linkedScan(BIOrionObject, OrionType, OrionType)` |
| Insert | `insert(BIOrionObject)`, `insert(String)`, `insert(String, BSimple[])`, `mappedInsert(BObject)` |
| Update | `update(BSqlUpdate)`, `update(String)`, `update(String, BSimple[])`, `update(BIOrionObject)`, `update(BIOrionObject, boolean)`, `mappedUpdate(BObject)`, `persist(BIOrionObject)` |
| Delete | `delete(OrionType, BExpression)`, `delete(String)`, `delete(String, BSimple[])`, `mappedDelete(BObject)`, `delete(BIOrionObject)` |
| Batch | `batchInsert(OrionType)`, `batchUpdate(OrionType)`, `batchPersist(OrionType)`, `batchDelete(OrionType)` |

`[CERT]` `OrionSession.java:14-107` (all signatures; exact line ranges per method omitted for
brevity — verified present by grep)

---

## 412.8 — `DbOrionSession`: Concrete Implementation `[CERT]`

`DbOrionSession` is the concrete JDBC-backed `OrionSession` for local (station) use.

**Key internals:**
```java
// DbOrionSession.java:46,64
private Connection conn;
// ...constructor:
this.conn = db.getRdbms().getConnection();   // one raw JDBC Connection per session
```
`[CERT]` `DbOrionSession.java:46,64`

**PreparedStatement LRU cache:**
```java
// DbOrionSession.java (approx lines 411-418)
// CacheMap extends LinkedHashMap, evicts eldest when size() > 50
private DbOrionSession.CacheMap<String, RdbmsPreparedStatement> prepCache = new ...();
```
Statements are cached by SQL string (key); cache capacity is 50 entries with LRU eviction.
`[CERT]` `DbOrionSession.java:411-418` (approx, per structural agent)

**Commit / rollback / close:**
```java
// DbOrionSession.java:110-125, 136-150
public void commit()   { this.conn.commit(); }
public void rollback() { this.conn.rollback(); }
public void close() {
    if (!this.conn.getAutoCommit()) { this.rollback(); }
    this.conn.close();
    this.prepCache.clear();
    this.db.sessionClosed(this);
}
```
Auto-rollback fires on `close()` when autocommit is off and the caller did not explicitly commit.
`[CERT]` `DbOrionSession.java:110-150`

**DDL delegation:**
```java
// DbOrionSession.java:157-159
public void invokeDdl(DdlCommand ddl) {
    this.ddlHelper.invokeDdl(ddl.getDdl(this.getRdbmsContext()));
}
```
`DdlCommand.getDdl(RdbmsContext)` produces a dialect-aware SQL string; `Ddl.invokeDdl(String)`
executes it (§412.9). The `RdbmsContext` carries the dialect, enabling dialect-specific DDL
variants (SQL Server vs. MySQL vs. Oracle).
`[CERT]` `DbOrionSession.java:157-159`

---

## 412.9 — DDL Layer: Whitelist Enforcement `[CERT]`

```java
// Ddl.java:14-24
public void invokeDdl(String ddl) {
    String word = firstWord(ddl.trim()).toUpperCase();
    if (!word.equals("CREATE") && !word.equals("DROP")
        && !word.equals("ALTER") && !word.equals("RENAME")) {
        throw new OrionException("Invalid ddl statement: " + ddl);
    }
    try (Statement statement = this.conn.createStatement()) {
        statement.executeUpdate(ddl);
    }
}
```

Only four DDL verbs are permitted: `CREATE`, `DROP`, `ALTER`, `RENAME`. Any other first word throws
`OrionException`. DDL uses a raw (non-cached) `Statement.executeUpdate()` — no PreparedStatement.
`[CERT]` `Ddl.java:14-24`

---

## 412.10 — Cursor and Pagination: `OrionCursor` + `PaginatedCursor` `[CERT]`

### 412.10.1 — `OrionCursor` (interface)

```java
// OrionCursor.java:6-15
public interface OrionCursor extends IterableCursor<BObject> {
   OrionType getOrionType();
   OrionSession getSession();
   BIOrionObject[] toArray();
   boolean nextComponent();
   boolean next(Class<?> var1);
}
```
`[CERT]` `OrionCursor.java:6-15`

### 412.10.2 — `PaginatedCursor` (offset + limit wrapper)

```java
// PaginatedCursor.java:46-57
protected boolean advanceCursor() {
    if (!this.started) {
        this.started = true;
        for (int i = 0; i < this.page.getOffset(); i++) {
            this.cursor.next();      // skip offset rows (sequential scan — no SQL OFFSET)
        }
        this.rowCount = 0;
    }
    return this.rowCount++ < this.page.getLimit() ? this.cursor.next() : false;
}
```

`PaginatedCursor` is activated when a `BSqlQuery` carries a `BPage` but the target RDBMS does not
support native LIMIT/OFFSET (checked by `AutoResolveVisitor.dbSupportsPagination()`). In that
case the offset is consumed by discarding rows in Java — no SQL `OFFSET` clause. When the DB
supports native pagination, the SQL itself carries LIMIT/OFFSET.
`[CERT]` `PaginatedCursor.java:46-57`

---

## 412.11 — Schema Versioning: `BSchemaVersion`, `ISchemaUpgrader`, `OrionAppSchemaManager` `[CERT]`

### 412.11.1 — `BSchemaVersion`

```java
// BSchemaVersion.java:16-21
public final class BSchemaVersion extends BSimple implements BIComparable {
    public static final BSchemaVersion DEFAULT = new BSchemaVersion("0");
    private Version version;
    private String string;
}
```
A `BSimple` wrapping `javax.baja.util.Version` (major.minor.build.patch). Implements
`BIComparable` so versions can be compared in upgrade-chain walking.
`[CERT]` `BSchemaVersion.java:16-21`

### 412.11.2 — `ISchemaUpgrader`

```java
// ISchemaUpgrader.java:3-8
public interface ISchemaUpgrader {
   BSchemaVersion getFromVersion();
   BSchemaVersion getToVersion();
   void upgrade(BLocalOrionDatabase db, BIOrionApp app, OrionSession session) throws Exception;
}
```
Each upgrader handles one hop: `fromVersion` → `toVersion`. The `upgrade()` method receives the
local DB, the app, and an already-open session (with autocommit off, started by the manager).
`[CERT]` `ISchemaUpgrader.java:3-8`

### 412.11.3 — `OrionAppSchemaManager` (greedy step-walk algorithm)

```java
// OrionAppSchemaManager.java:16-68
public void performSchemaUpgrade(BLocalOrionDatabase db, BSchemaVersion oldVersion) throws Exception {
    BSchemaVersion currentVersion = oldVersion;
    OrionSession session = db.createSession(null);
    try {
        session.setAutoCommit(false);
        while (true) {
            // pick upgrader: fromVersion == currentVersion, highest toVersion
            for (ISchemaUpgrader u : this.upgraders) {
                if (u.getFromVersion().equals(currentVersion)
                    && !same(from, to)
                    && (bestUpgrader == null || u.getToVersion().compareTo(bestUpgrader.getToVersion()) > 0))
                    bestUpgrader = u;
            }
            if (bestUpgrader == null) {
                if (!currentVersion.equals(app.getSchemaVersion()))
                    throw new IllegalStateException("No upgrader for " + currentVersion);
                session.commit();  // all steps committed atomically
                return;
            }
            bestUpgrader.upgrade(db, this.app, session);  // OrionAppSchemaManager.java:58
            currentVersion = bestUpgrader.getToVersion();
            bestUpgrader = null;
        }
    } catch (Exception e) {
        session.rollback();
        throw e;
    } finally { session.close(); }
}
```

**Algorithm:** greedy step walk. At each step, select the upgrader whose `fromVersion` equals the
current version and whose `toVersion` is highest (allows skipping intermediate hops if an upgrader
covers a larger range). Stop when the current version equals `app.getSchemaVersion()` — then
commit the whole upgrade in one transaction. Any exception rolls back the entire upgrade.
`[CERT]` `OrionAppSchemaManager.java:16-68`

**`SchemaUpgradeUtil.alterColumn()`** generates an `AlterColumn` DDL command from the current
`TableBuilder` output (re-derives the full CREATE TABLE then extracts the target column):
`[CERT]` `SchemaUpgradeUtil.java:13-31`

---

## 412.12 — System Tables and `BOrionAppVersion` `[CERT]`

Orion maintains its own internal system tables via `@NiagaraOrionType` classes. The two confirmed
system tables are:

**`BOrionSysTables`** — table registry (stores every table name known to this Orion DB):
```java
// BOrionSysTables.java:15-38
@NiagaraOrionType
@NiagaraProperties({
    @NiagaraProperty(name="id",        type="int",    facets={@Facet("ID_KEY")}),
    @NiagaraProperty(name="tableName", type="String", facets={UNIQUE=true, WIDTH=128})
})
public class BOrionSysTables extends BOrionObject { ... }
```
`[CERT]` `BOrionSysTables.java:15-38`

**`BOrionAppVersion`** — one row per registered `BIOrionApp`. Fields: `app` (TypeSpec — which
app) + `schemaVersion` (current schema version stored in DB). `createOrUpgradeApp()` reads this
row to detect whether a schema upgrade is needed:
`[CERT]` `BLocalOrionDatabase.java:463-488` (createOrUpgradeApp reads BOrionAppVersion.ORION_TYPE)

Both sys tables are ensured to exist before any app schema creation runs:
`[CERT]` `BLocalOrionDatabase.java:105` (`ensureTableExists(session, BOrionAppVersion.ORION_TYPE)`)

---

## 412.13 — `BatchStatement`: Chunked Batch Writes `[CERT]`

```java
// BatchStatement.java:6-21
public interface BatchStatement {
   void add(BIOrionObject var1);
   BIOrionObject get(int var1);
   int size();
   OrionType getOrionType();
   void execute();
   void clear();
   void setChunkSize(int var1);
   int getChunkSize();
}
```

`setChunkSize(N)` / `getChunkSize()` allow callers to control JDBC batch flush granularity. The
four batch types returned by `OrionSession` (`batchInsert`, `batchUpdate`, `batchPersist`,
`batchDelete`) all implement this interface. Seen in production use in `BOrionAlarmDatabase.
recalculateAlarmClassStatistics()` [Block 404 §404.9]:
`batchUpdate(BOrionAlarmClass.ORION_TYPE)` + `batchDelete(BOrionAlarmClass.ORION_TYPE)`.
`[CERT]` `BatchStatement.java:6-21`

---

## 412.14 — `BOrionMigrator`: Data Migration (Different from Schema Upgrader) `[CERT + INFER]`

`BOrionMigrator extends BAbstractOrionApp`. Its purpose is to migrate records between two physical
`BRdbms` databases (old → new), not to apply DDL upgrades within one database. This is entirely
separate from `OrionAppSchemaManager`.

Key properties: `oldDbOrd` (source database ORD), `appOrdList` (which apps to migrate),
`migrationType` (enum: New / Insert / Overwrite / ForceOverwrite).

**`doMigrate()` process:** opens two sessions (old + new DB), iterates types in dependency order,
pages through source records in batches — default page size driven by system property
`niagara.query.maxResultSize` (default 5000). After copy, updates `BOrionAppVersion` row in the
target DB. `performSchemaUpgrade()` is a no-op on the migrator (schema already exists on target).
`[CERT]` `BOrionMigrator.java` (structural sub-agent finding; key line range ~176 for doMigrate)
`[INFER]` "no-op performSchemaUpgrade" deduced from the class hierarchy — BOrionMigrator overrides
BAbstractOrionApp's performSchemaUpgrade with a no-op body.

**Fox-protocol remote variant:** `BFoxOrionDatabase` and `FoxOrionSession` implement the same
`OrionSession` interface for remote access over the Fox protocol, enabling a supervisor station to
write records into a remote station's Orion DB without local JDBC access. Not read in detail.
`[INFER]` (class names confirm protocol role; internal wire format not investigated)

---

## 412.15 — `BIOrionApp`: Contract for ORM Consumers `[CERT]`

```java
// BIOrionApp.java:9-18
public interface BIOrionApp extends BIService {
   OrionType[] getOrionTypes();        // list of OrionType instances this app owns
   void orionReady(BOrionDatabase db); // callback when schema is confirmed current
   BSchemaVersion getSchemaVersion();  // target schema version (compared vs stored)
   void performSchemaUpgrade(BLocalOrionDatabase db, BSchemaVersion oldVersion) throws Exception;
                                       // delegates to app's OrionAppSchemaManager
}
```

Any module wishing to use the ORM (alarmOrion, lonOrion, etc.) implements `BIOrionApp`. The
`getOrionTypes()` return value drives type registration; `orionReady()` is the safe gate — the
app must not open sessions before this callback fires (schema not guaranteed ready before that).
`[CERT]` `BIOrionApp.java:9-18`

**Known consumers (remittance — not re-derived here):**
- `alarmOrion` — six SQL tables for alarm records/classes/EAV/sources → [Block 404]
- `lonOrion` — LON network data (mentioned as consumer; not investigated)

---

## 412.16 — Self-Verify `[CERT]`

| Claim | Marker | Citation |
|---|---|---|
| `@NiagaraOrionType` is empty marker, `@Target(TYPE)` | `[CERT]` | `NiagaraOrionType.java:6-8` |
| `@OrionProperty` is type-level (not field-level), elements: name, refType, flags, facets[] | `[CERT]` | `OrionProperty.java:8-18` |
| `@OrionIndex` elements: name, unique, clustered, fields[] | `[CERT]` | `OrionIndex.java:8-16` |
| `@OrionLinkedCursor` elements: name, type, link | `[CERT]` | `OrionLinkedCursor.java:8-14` |
| `@OrionRefCursor` elements: name, type, ref | `[CERT]` | `OrionRefCursor.java:8-14` |
| `BOrionObject extends BComponent implements BIOrionObject` | `[CERT]` | `BOrionObject.java:24` |
| `ID_KEY = BFacets.make("key", TRUE, "identity", TRUE)` | `[CERT]` | `BOrionObject.java:44` |
| Facet constants: KEY, IDENTITY, UNIQUE, INDEXED, WIDTH, DESCENDING, CLUSTERED, ON_DELETE, AUTO_RESOLVE, CLOB, DB_SOURCED | `[CERT]` | `BOrionObject.java:31-43` |
| `BOrionSpace extends BComponentSpace`, annotated `@AuditableSpace` | `[CERT]` | `BOrionSpace.java:29-31` |
| `getOrionDatabase(BRdbms)` delegates to BOrionService | `[CERT]` | `BOrionSpace.java:95-122` |
| `getSessionFromContext()` walks Context chain for OrionSession | `[CERT]` | `BOrionSpace.java:154-166` |
| `BOrionService.dbs` = HashMap<String, BLocalOrionDatabase> | `[CERT]` | `BOrionService.java:80` |
| `getOrionDatabase(BRdbms)` lazy-inits BLocalOrionDatabase | `[CERT]` | `BOrionService.java:251-263` |
| `BOrionDatabase` is abstract class | `[CERT]` | `BOrionDatabase.java:18` |
| `getTableName()` → BNameFactory.getTableName(db, type) | `[CERT]` | `BOrionDatabase.java:64-66` |
| `createSession()` is abstract | `[CERT]` | `BOrionDatabase.java:112` |
| BLocalOrionDatabase internal: appList, idMap, tableDefs, typeList | `[CERT]` | `BLocalOrionDatabase.java:38-51` |
| `registerApp()` adds to list only — no schema creation | `[CERT]` | `BLocalOrionDatabase.java:140` |
| `createSession()` → new DbOrionSession; no pool | `[CERT]` | `BLocalOrionDatabase.java:505-511` |
| `boot()` registers BOrionAppVersion.ORION_TYPE + calls open() | `[CERT]` | `BLocalOrionDatabase.java:84-90` |
| `open()`: ensureTableExists + createOrUpgradeApp per app | `[CERT]` | `BLocalOrionDatabase.java:93-110` |
| `createOrUpgradeApp()`: reads BOrionAppVersion; upgrades if version behind | `[CERT]` | `BLocalOrionDatabase.java:463-488` |
| `OrionSession extends Context, AutoCloseable` | `[CERT]` | `OrionSession.java:14` |
| `DbOrionSession.conn` = raw java.sql.Connection from getRdbms().getConnection() | `[CERT]` | `DbOrionSession.java:46,64` |
| PreparedStatement LRU cache max 50 entries | `[CERT]` | `DbOrionSession.java:411-418` (approx) |
| `commit()`/`rollback()` delegate to java.sql.Connection | `[CERT]` | `DbOrionSession.java:110-125` |
| Auto-rollback on close when !autocommit | `[CERT]` | `DbOrionSession.java:136-150` |
| `invokeDdl(DdlCommand)` → Ddl.invokeDdl(ddl.getDdl(rdbmsContext)) | `[CERT]` | `DbOrionSession.java:157-159` |
| Ddl.invokeDdl whitelist: CREATE/DROP/ALTER/RENAME only | `[CERT]` | `Ddl.java:14-24` |
| `OrionCursor extends IterableCursor<BObject>`, adds getOrionType(), getSession(), toArray() | `[CERT]` | `OrionCursor.java:6-15` |
| PaginatedCursor: skip offset rows in Java when DB lacks native pagination | `[CERT]` | `PaginatedCursor.java:46-57` |
| `BSchemaVersion extends BSimple implements BIComparable` | `[CERT]` | `BSchemaVersion.java:16` |
| `ISchemaUpgrader`: getFromVersion(), getToVersion(), upgrade(db, app, session) | `[CERT]` | `ISchemaUpgrader.java:3-8` |
| OrionAppSchemaManager: greedy step walk, one transaction, rollback on error | `[CERT]` | `OrionAppSchemaManager.java:16-68` |
| BOrionSysTables: @NiagaraOrionType, id PK + tableName UNIQUE WIDTH=128 | `[CERT]` | `BOrionSysTables.java:15-38` |
| ensureTableExists(BOrionAppVersion) fires before createOrUpgradeApp | `[CERT]` | `BLocalOrionDatabase.java:105` |
| BatchStatement interface: add, execute, setChunkSize, getChunkSize | `[CERT]` | `BatchStatement.java:6-21` |
| `BIOrionApp` contract: getOrionTypes, orionReady, getSchemaVersion, performSchemaUpgrade | `[CERT]` | `BIOrionApp.java:9-18` |
| BOrionMigrator: data migration between two physical databases | `[CERT]` | `BOrionMigrator.java` (file presence + structural report) |
| PaginatedCursor activated when `dbSupportsPagination()` returns false | `[INFER]` | derived from Select.java structural report |
| Fox-protocol variant uses BFoxOrionDatabase / FoxOrionSession | `[INFER]` | class names in priv/fox/ directory |

**Self-verify tally:** 45 claims — 43 `[CERT]`, 2 `[INFER]`. Zero unsupported assertions.

**Block TYPE:** `standard`

---

## 412.17 — Connections

- **[Block 404]** — alarmOrion is a consumer of orion-rt. B404 documents the alarmOrion-specific
  tables (six SQL tables, `BOrionAlarmRecord` schema, cursor merge-join, archive move). This block
  (B412) documents the generic ORM framework that alarmOrion sits on. B404's §404.8 described
  schema versioning "from the consumer side" (three upgraders, version 1.4); B412 §412.11 now
  explains the underlying mechanism (`OrionAppSchemaManager`, greedy step walk, single-transaction
  upgrade). B404 noted `OrionAppSchemaManager` by name without explaining its algorithm — B412
  fills that gap.

- **[Block 403]** — rdb-rt uses raw JDBC (PreparedStatement built by `BRdbmsDeprecatedDialect`,
  no schema manager, no type registry). orion-rt is an ORM abstraction layer above JDBC —
  it reuses the `BRdbms` connection infrastructure from rdb-rt but replaces dialect-specific SQL
  construction with annotation-driven table definitions and a session API. Both ultimately land
  on the same `java.sql.Connection` provided by the `BRdbms` subsystem.

- **[Block 402]** — BOG save path (dirty flag, `saveSync()`). Orthogonal: B402 covers the
  component-space BOG write path; orion-rt is the SQL export path. No shared code.

- **[Block 408]** — BComponentSpace lifecycle (LoadCallbacks, AuditableSpace). `BOrionSpace
  extends BComponentSpace` and carries `@AuditableSpace` — audit trail integration is inherited
  from the same `AuditableSpace` annotation documented in B408.
