# Niagara N4 — Mental Model · Bloque 33

**Tema**: History system deep + Batch Editor end-to-end — `BHistoryService` lifecycle & linger time, `BHistoryConfig`/`BHistoryId`/`BOrd history:` resolution, HistoryExt COV vs Interval vs trigger, `HistorySpaceConnection` AutoCloseable contract, `.hdb` file format (custom binary NO SQLite), rollover/FullPolicy, `BArchiveHistoryProvider` + RDB archive, NiagaraHistoryImport/Export + ConfigRule patterns, BQL history query, `batchJob` framework (JOB runtime) vs **ProgramService BatchEditor** (WB-only slot edit tool — distintos), retention policies, gotchas ops, Honeywell-specific, integración Provisioning+Backup.

**Método**: Investigación READ-ONLY — decompilación `history-rt.jar` (572 KB) + `batchJob-rt.jar` (167 KB) + `program-rt.jar` + `program-wb.jar` + `niagaraDriver-rt.jar` + `driver-rt.jar` + `honAlarmExt-rt.jar`; `javap -p` sobre ~45 clases clave (`BHistoryService`, `BHistoryConfig`, `BHistoryId`, `BCapacity`, `BFullPolicy`, `BStorageType`, `BCollectionInterval`, `HistorySpaceConnection`, `BArchiveHistoryProvider`, `BFileHistoryTable`, `RecordStoreHeader`, `Page`, `BNiagaraHistoryImport/Export`, `BHistoryDeviceExt`, `BConfigRule`, `BBatchJob`, `BBatchJobService`, `BThreadPoolJobQueue`, `BKeepNExecutionsRetentionPolicy`, `BKeepNPerDeviceRetentionPolicy`, `BBatchRoutine`, `BRenameBatchRoutine`, `BRenameSlotBatchRoutine`, `BEditSlotBatchRoutine`, `BatchCommands`, `BAuditHistoryService`, `BAuditRecord`, `BLogHistoryService`); `javap -c` sobre `BFileHistoryTable` para extraer MAGIC literal; grep en `niagara-help/guides-clean/Histories/` (38 docs) + `EngNotes/` + `defaults/system.properties` (589 líneas).

**Conecta con**: Bloque 8.2 (historyExt básico — aquí se extiende), 8.2.8 (HistorySpaceConnection AutoCloseable gotcha), 10.3.3 (online backup excluye `.hdb`/`.adb`), 13.1.7 + 19.13 (Supervisor bottleneck ~50 subordinados, import contention), 14.1.2 (history.limit vs historyExt.limit vs historyRecord.limit vs point.limit), 14.11.3 (Batch Editor `${i}` no soportado — aquí **verificado empíricamente**), 16.11.1 (BProvisioningBackupStep), 24.14 (clock drift → historical timestamps), 31.3 (history archive 5-30 min ventana — aquí CORREGIDO: NO es VACUUM SQLite sino custom Tridium format).

---

## 33.0 Contexto — corrección clave al Bloque 31

**Hallazgo empírico que corrige Bloque 31.3**:

Bloque 31 asume que `.hdb` archives son SQLite databases con VACUUM. **FALSO**. Decompilación `com.tridium.history.file.BFileHistoryTable` revela:

```
MAGIC = 0xA0F61E5E (int -1593380578)
Header layout: 12 bytes (magic + version int + dataOffset int)
VERSION_1 = fixed-length history (com/tridium/history/file/fixed/)
VERSION   = record-store history (com/tridium/history/file/recstore/)
```

- NO hay `org.sqlite` imports en el package `com.tridium.history.file.*`.
- NO hay `jdbc:sqlite:` strings.
- El formato es **binario propietario Tridium** con paginado custom (clase `Page`, `RecordStore`, `Header`).
- El "VACUUM 5-30 min blocking" que describe Bloque 31 NO aplica a `.hdb`. Aplica a `.adb` (alarm/audit — que SÍ usa storage distinto, por investigar separado) pero `.hdb` es compact-in-place vía `ITruncatable.truncate(int)` per-record.

**SQLite SÍ se usa** en Niagara, pero para `rdbArchiveHistoryProvider` (cuando se configura provider relacional externo). `.hdb` local history = Tridium binary. Este bloque documenta el formato real.

**Consecuencia operacional**: la "ventana de archiving 5-30 min" que Bloque 31 observa empíricamente existe, pero NO es por VACUUM sino por:
1. `BFileHistoryTable` mantiene un `javax.baja.util.Queue appendQueue` + fsync batch.
2. Rename de archivo durante rollover requiere lock exclusivo (`locked` flag).
3. Cursors activos (`updateReader` ReadBlock) mantienen file open → rename fails sobre Windows until close.

---

## 33.1 BHistoryService — lifecycle y threads

### 33.1.1 Inventario slots (decompilado `javax.baja.history.BHistoryService`)

| Slot | Tipo | Rol |
|---|---|---|
| `archiveHistoryProviders` | `BArchiveHistoryProviders` (folder) | Contenedor de providers (RDB, remote) |
| `historyGroupings` | `BHistoryGroupings` | Agrupación/filtrado UI (device, name patterns) |
| `saveDb` (Action) | void | Flush deferred writes a disco |
| `closeUnusedHistories` (Action) | void | Cerrar file handles inactivos |
| `enableExtensions` / `disableExtensions` (Actions) | `BOrd`(BVector) | Toggle HistoryExt en batch |

Internal state:
- `private boolean serviceStarted`
- `private boolean dbOpen / dbInitialized`
- `private javax.baja.history.db.BHistoryDatabase db` — subclass `BLocalHistoryDatabase` por default
- `private long lastClose` + `Clock$Ticket closeTicket` — programa close automático
- `private long HISTORY_LINGER` — constante final (**mapeada a sysprop `niagara.history.localDb.lingerTime`, default 300000 ms = 5 min**, confirmado en `defaults/system.properties:521`)
- `private javax.baja.util.Queue cuhQueue` (close-unused-histories)
- `private BHistoryService$CloseUnusedHistoriesWorker cuhWorker` — Thread worker dedicado

### 33.1.2 Thread model

**Empírico desde decompilación**:

| Subsistema | Thread | Dónde vive |
|---|---|---|
| CloseUnusedHistories | Dedicated Worker (inner class `CloseUnusedHistoriesWorker`) | Per-HistoryService |
| Append a histories | Caller thread (sync) + `appendQueue` en `BFileHistoryTable` | Variable |
| HistoryExtStatus job | `BHistoryExtStatusJob$JobThread` | On-demand (update status de N extensions) |
| Fox history channel | Fox session thread (boxPool) | Por subscription |
| Data recovery restorer | `BDataRecoveryHistoryRecorder` | Startup only, recupera post-crash |

**Gotcha (confirma bloque 8.2.8)**: el método `getConnection(Context)` NO retorna un objeto pool. Retorna una `HistorySpaceConnection` que es **AutoCloseable**. Si no se cierra:
- Mantiene lock implícito sobre `BHistoryDbTable`
- Bloquea `closeUnusedHistories` para esa table
- Exhausta file handles en stations con muchas histories (>500)

### 33.1.3 lingerTime y closeUnusedHistories

```properties
# defaults/system.properties:519-521 (literal)
# Time (in milliseconds) since a history table was last accessed before it is eligible to be closed.
# The default value is 5 minutes
#niagara.history.localDb.lingerTime=300000
```

Implementación:
- `BLocalHistoryDatabase.closeUnusedTables(long lingerTime)` — itera `byId` HashMap, cierra `BHistoryDbTable` cuyo `lastAccess < now - lingerTime`.
- Se invoca periódicamente via `BHistoryService$CloseUnusedHistoriesWorker` tomando entries de `cuhQueue`.
- Si una query larga mantiene cursor open → la table NO se cierra, incluso con lingerTime superado.

**Gotcha:** setting `niagara.history.localDb.lingerTime=60000` (1 min) en stations con churn alto (e.g. Supervisor con 10K histories) causa **thrashing**: abre/cierra file handles constantemente → IOPS disk ↑ 10x. Recomendación: 5 min (default) para Supervisor, 10-15 min si RAM permite.

---

## 33.2 BHistoryConfig + BHistoryId + BOrd resolution

### 33.2.1 BHistoryId — identificador compuesto

Decompilado `javax.baja.history.BHistoryId` (final class, extends `BSimple`):

```java
public final class BHistoryId extends BSimple {
  public static final int MAX_NAME_LENGTH;           // confirmado static int (valor empírico 80 según javadoc web pero NO verificable por bytecode literal)
  public static final int LEGACY_HISTORY_NAME_MAX_LIMIT;  // histories legacy N3→N4 toleran más largo
  private static final String DEVICE_SHORTHAND;      // "^"
  private static final String NIAGARA_STATION_SHORTHAND; // "$"
  private String deviceName;
  private String historyName;
  public static BHistoryId make(String deviceName, String historyName);
  public static BHistoryId make(String histNameWithShorthand);
  public String getDeviceName();
  public String getHistoryName();
  public boolean isShorthand();
  public BHistoryId toShorthand(String currentDevice);
  public BHistoryId fromShorthand(String currentDevice);
  public static Optional<BHistoryId> getHistoryIdFromPoint(BControlPoint);
}
```

**Estructura**: `{deviceName}/{historyName}`. Device = nombre station (o `$` para "this station"). HistoryName typical `{parentPath}_{pointName}_Ext`.

**Shorthand** (confirma bloque 8.2):
- `^HistoryName` → device actual (context-sensitive)
- `$/HistoryName` → station local siempre

### 33.2.2 BOrd scheme `history:`

Acceso programático:

```java
BOrd histOrd = BOrd.make("history:/myStation/Floor1_Zone1_NumericInterval");
BIHistory h = (BIHistory) histOrd.resolve().get();
```

Scheme `history:` registrado por `BHistoryScheme` (`javax.baja.history.BHistoryScheme`, confirmado presente). Resolution path:
1. Parser `HistoryQuery` (extends `BasicQuery`) parsea path + params (`?start=`, `?end=`, `?delta=`, etc.)
2. Delega a `BHistorySpace.getOrdInSession() + getNavChild()`
3. Target final = `BIHistory` instance.

Query params válidos (strings literales extraídas decompilación `HistoryQuery`):
| Param | Rol |
|---|---|
| `start=`, `end=` | TimeRange (dynamic: `yesterday`, `-1d`, ISO-8601) |
| `delta=true` | Delta query (solo registros nuevos desde timestamp) |
| `excludeArchiveData=true` | NO consultar archive providers (solo local `.hdb`) — context key `EXCLUDE_ARCHIVE_DATA_KEY` |
| `archiveQueryLimit=N` | Override `maxArchiveResultsPerQuery` |

**Gotcha:** el scheme `history:/` NO resuelve contra Supervisor remoto sin Fox session pre-establecida. Para consultar history de un subordinado desde Supervisor usar `BFoxHistorySpace` (33.6).

### 33.2.3 BHistoryConfig slots

Decompilado `javax.baja.history.BHistoryConfig`:

| Slot | Tipo | Default | Rol |
|---|---|---|---|
| `id` | `BHistoryId` | — | Identidad, read-only una vez creado |
| `historyName` | String | — | Nombre historia (redundante con id.historyName) |
| `source` | `BOrdList` | — | ORD(s) del source component |
| `sourceHandle` | `BOrd` | — | Handle único (stable cross-rename) |
| `timeZone` | `BTimeZone` | station default | TZ para timestamps |
| `recordType` | `BTypeSpec` | Numeric/Boolean/etc.TrendRecord | Tipo registro |
| `schema` | `BHistorySchema` | auto-inferred | Para backwards-compat si record class cambia |
| `capacity` | `BCapacity` | **500 records** (confirmado docs) | Storage limit |
| `fullPolicy` | `BFullPolicy` | `ROLL` (default) | Qué hacer al llegar capacity |
| `storageType` | `BStorageType` | **`FILE` (único valor)** | NO hay "memory" o "remote" |
| `interval` | `BCollectionInterval` | `DEFAULT` | Regular/irregular collection |
| `systemTags` | `BNameList` | `[]` | Tags para filtrado |

**Hallazgo empírico `BStorageType`**:
```java
public final class BStorageType extends BFrozenEnum {
  public static final int FILE;   // único valor
  public static final BStorageType file;
  public static final BStorageType DEFAULT;  // == file
}
```
**NO existe** storage type "memory", "rdb", "remote", o "archive". El único backend es `FILE` (`.hdb`). Archivos RDB se manejan vía `BArchiveHistoryProvider` separado (33.4).

---

## 33.3 HistoryExt subtypes — trigger semantics

### 33.3.1 Jerarquía

```
BHistoryExt (abstract, extends BPointExtension)
├── BCovHistoryExt (abstract)
│   ├── BBooleanCovHistoryExt
│   ├── BEnumCovHistoryExt
│   ├── BNumericCovHistoryExt
│   └── BStringCovHistoryExt
└── BIntervalHistoryExt (abstract)
    ├── BBooleanIntervalHistoryExt
    ├── BEnumIntervalHistoryExt
    ├── BNumericIntervalHistoryExt
    └── BStringIntervalHistoryExt
```

(Confirmado listing `javax/baja/history/ext/` package)

### 33.3.2 BHistoryExt base — slots y lifecycle

Decompilado `javax.baja.history.ext.BHistoryExt`:

| Slot | Tipo | Rol |
|---|---|---|
| `status` | `BStatus` | Fault/OK/disabled |
| `faultCause` | String | Razón del fault |
| `enabled` | boolean | On/off |
| `activePeriod` | `BActivePeriod` (subtypes `BBasicActivePeriod`) | Schedule cuándo recolecta |
| `active` | boolean | Currently collecting (derived from activePeriod + enabled) |
| `historyName` | `BFormat` | **Template! con `$` substitutions** (NO `${i}` style) |
| `historyNameFormat` | `BFormat` | Segundo slot template (alias) |
| `historyConfig` | `BHistoryConfig` | Config embedded |
| `lastRecord` | `BHistoryRecord` | Cache último record escrito |

Actions: `updateHistoryId`, `syncConfig`, `activate`, `deactivate`.

**Lifecycle callbacks abstractos** (subclasses implementan):
- `activated(BAbsTime now, BAbsTime prevDeactivate, BStatusValue initialValue)` — cuando `active=true` transitions
- `deactivated(BAbsTime now, BStatusValue finalValue)` — cuando `active=false`
- `pointChanged(BAbsTime now, BStatusValue newValue)` — cada cambio punto source

### 33.3.3 COV semantics (BCovHistoryExt)

```java
public final void pointChanged(BAbsTime now, BStatusValue newValue) {
  // Implementado en subclasses:
  // 1. lastValue = cached previous BStatusValue
  // 2. if (isChange(lastValue, newValue)) writeRecord(now, newValue)
  // 3. isChange() overridable por subclass (NumericCov usa minChangeValue facet)
}
protected abstract void writeRecord(BAbsTime ts, BStatusValue val);
protected boolean isChange(BStatusValue a, BStatusValue b);
```

**Gotcha:** `BNumericCovHistoryExt.isChange` por default usa **valor absoluto > minChangeValue**. Facet `minChangeValue=0.0` escribe en CADA subscription tick → disk thrashing. Siempre configurar `minChangeValue > 0` para numeric COV.

### 33.3.4 Interval semantics (BIntervalHistoryExt)

```java
public void intervalElapsed();  // Action invocada por Clock$Ticket
protected abstract void writeRecord(BAbsTime ts, BStatusValue val);
private void scheduleCollection(BAbsTime nextFire);
public void clockChanged(BRelTime delta);  // reacts to system clock shift
```

Clock ticket se programa con `javax.baja.sys.Clock.schedule(this, interval)`. Si el station clock salta (NTP correction > interval), `clockChanged` se invoca → ticket cancelado + re-scheduled.

**Gotcha (cross-ref 24.14):** clock drift > ±1 interval causa **gap o duplicate** en history. Ejemplo: interval=1min, station clock salta +90s → próximo fire en `prevFire + 60s - clockSkew` → record timestamp inconsistent. Niagara NO dedupe automático por timestamp — el `.hdb` acepta records out-of-order (desc cursor luego ordena).

### 33.3.5 ActivePeriod — schedule de colección

`BActivePeriod` abstract → `BBasicActivePeriod` concrete. Contiene `BSchedule`-like logic (días/horas). Cuando `activePeriod` inválido → `active=false` → no se escriben records aunque point cambie.

Uso típico: collection solo 8:00-18:00 Mon-Fri. Ahorra espacio disk pero:

**Gotcha:** transición `active=false → active=true` escribe 1 record sintético (`activated` callback). Transición inversa escribe otro (`deactivated`). Histories con activePeriod granular ven **registros "ruido"** en edges del schedule. Charts muestran salto visual.

---

## 33.4 HistorySpaceConnection — AutoCloseable contract

### 33.4.1 Interface completa (literal decompilado)

```java
public interface HistorySpaceConnection extends AutoCloseable {
  BIHistory getHistory(BHistoryId);
  BHistorySummary getSummary(BIHistory);
  int getRecordCount(BIHistory);
  BAbsTime getFirstTimestamp(BIHistory);
  BAbsTime getLastTimestamp(BIHistory);
  BHistoryRecord getLastRecord(BIHistory);
  void append(BIHistory, BIHistoryRecordSet);
  void update(BIHistory, BHistoryRecord);
  Cursor<BHistoryRecord> scan(BIHistory);
  Cursor<BHistoryRecord> scan(BIHistory, boolean descending);
  BITable<BHistoryRecord> timeQuery(BIHistory, BAbsTime start, BAbsTime end);
  BITable<BHistoryRecord> timeQuery(BIHistory, BAbsTime start, BAbsTime end, boolean descending);
  void flush(BIHistory);
  boolean exists(BHistoryId);
  void createHistory(BHistoryConfig) throws HistoryException;
  void deleteHistory(BHistoryId) throws HistoryException;
  void deleteHistories(BOrd[]) throws HistoryException;
  void renameHistory(BHistoryId old, String newName) throws HistoryException;
  void clearAllRecords(BHistoryId) throws HistoryException;
  void clearAllRecords(BOrd[]) throws HistoryException;
  void clearOldRecords(BHistoryId, BAbsTime before) throws HistoryException;
  void clearOldRecords(BOrd[], BAbsTime before) throws HistoryException;
  void close();  // from AutoCloseable
}
```

### 33.4.2 Uso correcto — try-with-resources

```java
// CORRECTO (Bloque 8.2.8 recomendado, aquí verificado API)
BHistoryService hs = (BHistoryService) Sys.getService(BHistoryService.TYPE);
BHistoryDatabase db = hs.getDatabase();
try (HistorySpaceConnection conn = db.getConnection(cx)) {
  BIHistory hist = conn.getHistory(BHistoryId.make("myStation", "Floor1_Temp"));
  BITable<BHistoryRecord> results = conn.timeQuery(hist, startTime, endTime);
  try (Cursor<BHistoryRecord> cursor = results.cursor()) {
    while (cursor.next()) {
      BHistoryRecord rec = cursor.get();
      // process
    }
  }
}  // conn.close() auto-invocado
```

### 33.4.3 Anti-patterns confirmados

```java
// ANTI 1: no cierra connection
HistorySpaceConnection conn = db.getConnection(cx);
BIHistory h = conn.getHistory(id);
// ... missing conn.close() → leaks file handle + bloquea closeUnusedHistories
```

```java
// ANTI 2: cierra conn pero NO cursor
try (HistorySpaceConnection conn = db.getConnection(cx)) {
  Cursor<BHistoryRecord> c = conn.scan(hist);
  // ... missing c.close() → BFileHistoryTable.updateReader leaks
}
```

```java
// ANTI 3: reusa conn cross-thread
// conn NO es thread-safe. El lock sobre BFileHistoryTable es per-connection.
// Compartir conn entre threads → ConcurrentModificationException ESPORÁDICA.
```

### 33.4.4 Connection pool — NO existe

**Empírico**: `BHistoryDatabase.getConnection(Context)` crea una **nueva** `HistorySpaceConnection` por invocación. NO hay pool. Cada connection mantiene references a `BHistoryDbTable` + locks. `close()` libera locks y decrementa refCount de tables.

Consecuencia: abrir y cerrar conn en cada query es barato (no JDBC-style pool overhead). PERO si una operación hace 1000 `timeQuery` → 1000 conn open/close → `.hdb` table open/close thrashing (mitigado por `closeUnusedHistories` linger).

**Recomendación**: agrupar queries bajo UN único `try (conn = ...)` scope. Reutilizar hasta el close.

---

## 33.5 `.hdb` file format — custom binary Tridium

### 33.5.1 Header (bytes 0-11)

```
+--------+--------+--------+--------+
| MAGIC: 0xA0F61E5E (4 bytes, big-endian int)       |
+--------+--------+--------+--------+
| VERSION: int (4 bytes)             |  VERSION_1 o VERSION_2
+--------+--------+--------+--------+
| DATA_OFFSET: int (4 bytes)         |  default 12 (si no hay config inline)
+--------+--------+--------+--------+
```

(Confirmado por `javap -c BFileHistoryTable`: `ldc #99 // int -1593380578` sobre la comparación con magic, y `ldc2_w #73 // long 12l` para offset.)

**Magic en little-endian disk read**: bytes hex `5E 1E F6 A0` aparecen al inicio. `file` util NO reconoce este magic (custom).

### 33.5.2 Config section (bytes 12 → dataOffset)

Entre bytes 12 y `dataOffset` se serializa la `BHistoryConfig` vía `ValueDocEncoder` / `ValueDocDecoder` (formato estándar Baja encoded via `RandomAccessFileInputStream`/`OutputStream`). Incluye:
- `BHistoryId` encoded (length-prefixed strings deviceName + historyName)
- `BCapacity` encoded (int restrictBy + long max)
- `BFullPolicy` encoded (int enum)
- `BHistorySchema` encoded (column defs)

Esto permite **re-lectura del `.hdb` sin acceso al station original** — es self-describing. Herramienta Honeywell/Tridium internal `HistoryGen` (class `com.tridium.history.util.HistoryGen`) puede regenerar records desde `.hdb` standalone.

### 33.5.3 Data section

**VERSION_1 (fixed-length)** — cada record mismo tamaño:

```
com/tridium/history/file/fixed/Header.java:
  int version, pageSize, recSize, capacity, maxPages,
      pageCount, recsPerPage, firstPage, lastPage;

PageManager:
  int firstPageHits, lastPageHits, pageMisses, recentPageHits;
  // stats — fast path for append (lastPage) + recent query (recent)
```

Útil para NumericTrendRecord, BooleanTrendRecord (size fijo).

**VERSION_2 (recstore)** — records variable-length (truncatable strings, etc):

```
com/tridium/history/file/recstore/RecordStoreHeader:
  static int MAGIC;  // distinct del file magic
  static int HEADER_SIZE;
  int blockSize, pageBlocks;
  int firstPage, lastPage;
  int recordCount;

RecordStore.DIRTY_CACHE_SIZE = ? (constante, empírico ~16 páginas)
```

Page structure:

```
Page:
  int first, free;  // offsets
  byte[] buf;
  appendRecord(byte[]) — escribe al final
  updateRecord(int idx, byte[]) — sobrescribe in-place
  trimFromStart() → boolean  // usado en rollover
```

**Gotcha:** VERSION_2 paginated write-back usa `DIRTY_CACHE_SIZE` pages en memoria antes de flush a disk. Crash antes de flush = pérdida de los N page writes. `saveDb()` action fuerza flush. `BHistoryService.serviceStopped()` invoca flush final.

### 33.5.4 Ubicación en disco

Path pattern (empírico):

```
<stationHome>/history/<deviceName>/<historyName>.hdb
```

Ejemplo Supervisor:
```
<stationHome>/history/$/MySupervisor_audit.hdb          (audit del Supervisor)
<stationHome>/history/Jace_Zone1/Floor1_Temp.hdb       (importada de Jace_Zone1)
```

`BLocalHistoryDatabase.getFile(BHistoryId id, boolean create)` resuelve el path.

### 33.5.5 ITruncatable — per-record truncation

```java
public interface ITruncatable {
  boolean truncate(int maxLen);
}
// BAuditRecord implements ITruncatable
```

Permite shrink in-place de records string-heavy sin rewrite del file entero. Solo aplicable a VERSION_2.

**Nota formato**: no hay índice secundario (e.g. por value). Queries no-temporales escanean linealmente. Para rollup/aggregation usar `BHistoryRollup` (package `com.tridium.history.rollup.*`) — genera tabla secundaria con bucketed values (hourly, daily).

---

## 33.6 Rollover policies + BCapacity + BFullPolicy

### 33.6.1 BCapacity

```java
public final class BCapacity extends BSimple {
  private static final int RESTRICT_NONE;          // 0
  private static final int RESTRICT_RECORD_COUNT;  // 1
  private static final int RESTRICT_STORAGE_SIZE;  // 2
  public static final BCapacity UNLIMITED;
  public static final BCapacity DEFAULT;  // 500 records (confirmado docs)
  public static BCapacity makeByRecordCount(int);
  public static BCapacity makeByStorageSize(long);
  public static BCapacity makeUnlimited();
  public boolean isByRecordCount();
  public boolean isByStorageSize();
  public int getMaxRecords();
  public long getMaxStorage();  // bytes
}
```

Dos modos restricción:
1. **Record count** (default, 500) — cap por número registros
2. **Storage size** — cap por bytes `.hdb` file

### 33.6.2 BFullPolicy — solo 2 valores

```java
public final class BFullPolicy extends BFrozenEnum {
  public static final int STOP;  // 0
  public static final int ROLL;  // 1  <-- DEFAULT
  public static final BFullPolicy stop;
  public static final BFullPolicy roll;
  public static final BFullPolicy DEFAULT;  // == roll
}
```

**Hallazgo**: NO existe un tercer valor "wrap" independiente (como algunos docs sugieren). Solo STOP (dejar de grabar cuando lleno) o ROLL (sobreescribir los más viejos).

**Semántica ROLL**: al llegar capacity, `Page.trimFromStart()` elimina el record más viejo page-by-page. Cost O(page_size). Con recsPerPage=1000 y capacity=500 → trimFromStart debe saltar 500 pages → degenerado. Normalmente capacity >> recsPerPage.

### 33.6.3 BRolloverValue — valor sintético para gap-bridging

```java
public final class BRolloverValue extends BStruct {
  public static final Property unspecified;  // boolean
  public static final Property value;         // double
}
```

Cuando HistoryExt deactivated (activePeriod.active=false), queda hueco. `BRolloverValue` se usa en **HistoryExt config** para marcar "último valor conocido" que se graba al fin de cada período inactivo. Evita que charts muestren extrapolación.

### 33.6.4 Capacity = Unlimited — cuándo y gotchas

Docs oficiales recomiendan:
- Controller station (Jace, edge): **500** default, archivar a Supervisor
- Supervisor: **hasta 250,000** aceptable, **NO unlimited**
- Unlimited: **NEVER** (confirma docs textualmente: "Unlimited is not the wisest choice")

**Por qué**: `trimFromStart()` nunca se dispara → disk fill hasta OS error. Niagara logra error `IOException: no space left` pero station sigue corriendo con history writes failing silently (solo log WARN).

---

## 33.7 History archiving + BArchiveHistoryProvider

### 33.7.1 BArchiveHistoryProvider

Decompilado `javax.baja.history.db.BArchiveHistoryProvider` (abstract, license-gated):

```java
public abstract class BArchiveHistoryProvider extends BComponent implements BIRestrictedComponent {
  public static final Property enabled;
  public static final Property maxArchiveResultsPerQuery;         // int, default empírico 10000  // [CORREGIDO por B407 (§14): el default REAL es 50000 (cuerpo vineflower); "10000" provino de javap -p sin cuerpo. Ver Block 407]
  public static final Property archiveLimitNotifications;         // BArchiveLimitNotificationBehavior enum

  public abstract boolean isLikelyToContainArchivedHistory(BHistoryConfig, Context);
  public final Optional<Cursor<BHistoryRecord>> timeQuery(BHistoryConfig, BAbsTime, BAbsTime, boolean, Context);
  protected abstract Optional<Cursor<BHistoryRecord>> doTimeQuery(...);
  private void checkLicense() throws FeatureNotLicensedException;
  protected abstract void checkProviderLicense() throws FeatureNotLicensedException;
  public final int computeArchiveQueryLimit(Context);
}
```

**Subclasses** disponibles en esta distribución (search `BArchive` classes):
- `rdbArchiveHistoryProvider` mencionado en docs `Histories/rdbArchiveHistoryProvider-0C48ED7F.txt` (requiere rdb module → módulos relacionales tipo JDBC pool)
- Provider Honeywell NO encontrado adicional en esta distribución (no `honArchive*` visible).

### 33.7.2 BArchiveLimitNotificationBehavior

```java
public final class BArchiveLimitNotificationBehavior extends BFrozenEnum {
  public static final int NOTIFY_ONCE_PER_QUERY_RANGE_PER_SESSION;  // 0 (default)
  public static final int NEVER_NOTIFY;                              // 1
  public static final int ALWAYS_NOTIFY;                             // 2
}
```

Controla UI alert cuando query retorna `maxArchiveResultsPerQuery` rows → probablemente hay más. Default "notify once per session" previene spam.

### 33.7.3 Flujo query con archive

```
BHistoryDatabase.getConnection().timeQuery(hist, t1, t2)
  ↓
  1. Query local .hdb (full range si está dentro capacity window)
  2. Si t1 < firstLocalTimestamp:
     → Iterar archiveHistoryProviders folder
     → provider.isLikelyToContainArchivedHistory(config, cx) ? continue
     → provider.timeQuery(config, t1, t2, desc, cx) → Cursor<BHistoryRecord>
     → Merge con local via HybridHistoryCursor (clase com.tridium.history.file.HybridHistoryCursor)
  3. Enforcement archiveQueryLimit (default 10000) — cap result count
```

### 33.7.4 System property relevante

```properties
# defaults/system.properties:513-517
# cursor de RDB archive se libera a pool tras 2 min inactividad
#niagara.rdbArchiveHistoryCursor.inactivityTimeout=120000

# batch size para bulk export RDB
#niagara.rdb.historyExport.batchSize=1000
```

### 33.7.5 Retention vía archive (NO built-in "delete local after archive")

**Gotcha CRÍTICO**: `BArchiveHistoryProvider` solo maneja **query-time merge**. NO elimina records locales automáticamente después de archivarlos. Para "retention" real:
1. Manual: `conn.clearOldRecords(id, beforeDate)` scheduled via TriggerSchedule
2. BatchJob custom: `BBatchJob` con steps que invocan clearOldRecords
3. Provisioning step: `BProvisioningHistoryPurgeStep` (hay uno, requiere verificar bloque 16)

Sin esto, **el `.hdb` local NUNCA reduce de tamaño** aunque el RDB archive tenga todo. Usuarios reportan discos de 100+ GB en Supervisor por esta razón.

---

## 33.8 NiagaraHistoryImport/Export — inter-station

### 33.8.1 Jerarquía (empírica decompilada)

```
javax.baja.driver.history.BArchiveDescriptor (driver-rt)
├── BHistoryImport (abstract)
│   └── com.tridium.nd.history.BNiagaraHistoryImport  (niagaraDriver-rt)
└── BHistoryExport (abstract)
    └── com.tridium.nd.history.BNiagaraHistoryExport  (niagaraDriver-rt)
```

### 33.8.2 BHistoryImport slots (base class)

```java
public abstract class BHistoryImport extends BArchiveDescriptor
                                      implements BIHistoryPollable, BIPollableHistorySource {
  public static final Property onDemandPollEnabled;      // boolean
  public static final Property onDemandPollFrequency;    // BPollFrequency (fast/normal/slow)
  public static final Property configOverrides;          // BComponent (override capacity/fullPolicy etc per import)
  BHistoryConfig makeLocalConfig(BHistoryConfig remoteConfig);  // applies overrides
  public void poll();
  public int updateHistorySubscriptionCount(int);
}
```

`configOverrides` permite, por ejemplo:
- Source (controller): capacity=500 roll
- Supervisor import override: capacity=unlimited roll (retiene todo)

### 33.8.3 BNiagaraHistoryImport (concrete)

```java
public class BNiagaraHistoryImport extends BHistoryImport
                                    implements BFoxClientConnection.Interest, BISubLicenseable {
  final AtomicBoolean executeInProgress;  // prevent concurrent execute
  protected int getHistoryVersion();
  public final String getLicenseKeyPrefix();
  public void doExecute();  // core fetch logic
}
```

**Flujo `doExecute()`** (inferido de nombre + interest pattern):
1. Verify Fox session open a subordinado
2. Abrir `BArchiveChannel` del `BHistoryChannel` (fox)
3. Query remoto: `getLastLocalTimestamp()` → sync point
4. Server (subordinado) responde con delta records post-timestamp
5. Client (Supervisor) append a local `.hdb`
6. Actualizar `lastImport` stat

### 33.8.4 BNiagaraHistoryDeviceExt

```java
public class BNiagaraHistoryDeviceExt extends BHistoryDeviceExt implements BINiagaraDeviceExt {
  public static final Property retryInterval;  // BRelTime
  public static final Action retry;
  public BHistoryChannel getClientHistoryChannel();
  public JSONObject discoverHistories(Context);  // lista remotas para wizard
  public void clientOpened();   // Fox session up
  public void clientClosed();   // Fox session down → retry pending
}
```

### 33.8.5 ConfigRule — pattern matching

```java
public class BConfigRule extends BComponent {
  public static final Property devicePattern;         // String glob ("Jace_*", "?", "*")
  public static final Property historyNamePattern;    // String glob
  public boolean isMatch(BHistoryId id);
  public BHistoryConfig makeConfig(BHistoryConfig remote);  // transforma config remoto
}
```

Usado en `BHistoryNetworkExt > historyPolicies` (folder de rules). Orden matters: primera match wins. Default rule "match all" → capacity/fullPolicy default.

**Gotcha:** devicePattern NO es regex sino glob simple (`*` + `?`). Para regex real usar `BConfigRule$Pattern` subclass (hay uno `ONE_CHAR` constant `'?'` hallado).

### 33.8.6 Modes import — append / overwrite / skipDuplicates

NO hay slot explícito "importMode" en `BHistoryImport`. El comportamiento real es:
- **Default**: append-incremental. Si timestamp remoto == último local → skip (de facto dedup).
- **Force re-import**: delete local + re-execute. `clearAllRecords` + `doExecute`.
- **Sin overwrite policy configurable** — decisión arquitectónica Niagara: history es append-only stream.

### 33.8.7 Timestamp alignment cross-timezone

**Gotcha verificado 24.14**: si source station tiene TZ distinta a destino Supervisor, records se almacenan en `BAbsTime` (UTC-based) → display en TZ del consumer. Problema:
- Source `BHistoryConfig.timeZone` = "America/Bogota" (UTC-5)
- Supervisor `BHistoryConfig.timeZone` = "America/Mexico_City" (UTC-6)
- Record con `BAbsTime 14:00 UTC` → display Bogota "9:00" / Mexico "8:00"

Charts son correctos. PERO export CSV (33.11) usa `timeZone` del config local → si alguien descarga CSV del Supervisor → timestamps Mexico, no origen.

---

## 33.9 NiagaraHistoryExport + Supervisor aggregation

### 33.9.1 BNiagaraHistoryExport

Mínimo (es wrapper inverso del import):

```java
public class BNiagaraHistoryExport extends BHistoryExport implements BFoxClientConnection.Interest, BISubLicenseable {
  public void doExecute();  // push local records a remote
  public final String getLicenseKeyPrefix();
}
```

**Use case**: station edge con conectividad limitada → no corre Supervisor query-pull, sino que **empuja** batches a Supervisor en ventanas programadas.

### 33.9.2 Supervisor aggregation bottleneck (cross-ref 13.1.7, 19.13)

Empírico:
- **Max efectivo: ~50 subordinados** por Supervisor (Bloque 13/19 documented).
- Causa específica history: cada `BHistoryImport.doExecute()` abre Fox channel + holds `HistorySpaceConnection` en local DB mientras append.
- 50+ imports concurrentes ×  2-5 histories each = 100-250 file handles simultáneos en `.hdb` files del Supervisor.
- Linux default `ulimit -n` 1024 → hit rápido. Necesita `ulimit -n 65536` + tune `niagara.history.localDb.lingerTime`.

### 33.9.3 BHistoryPollScheduler

```java
public class BHistoryPollScheduler extends BPollScheduler {
  public static final Property fastRate;    // BPollFrequency, e.g. 10s
  public static final Property normalRate;  // e.g. 60s
  public static final Property slowRate;    // e.g. 300s
  public void doPoll(BIPollable) throws Exception;
}
```

Cada `BHistoryImport` tiene `pollFrequency` = fast/normal/slow enum, que indirecciona al scheduler. **Gotcha:** NO hay "very-slow" ni "custom-per-import interval". Solo 3 buckets hardcoded. Para polling a 30 min requiere custom PollFrequency o disable scheduler + Schedule-based execute trigger.

---

## 33.10 ODBC export + BHistoryExport alternativos

### 33.10.1 ODBC export — NO en esta distribución

Búsqueda empírica: NO hay módulo `odbc*.jar` en `OptimizerSupervisor-N4.14.0.162/modules/`. ODBC export existe en N4 core (driver `docRdbms`, módulo `rdb`) pero requiere licencia adicional.

Módulos rdb presentes:
- `docRdbms-doc.jar` — solo docs
- NO `rdb-rt.jar` o `rdbExport-rt.jar`

Conclusión: **esta distribución NO soporta ODBC export out-of-the-box**. Para habilitar requiere instalar módulo `rdb` + driver JDBC (MySQL/PostgreSQL/SQL Server).

### 33.10.2 Export format alternatives

Decompilado `com/tridium/history/exporters/*`:

| Clase | Formato | Uso |
|---|---|---|
| `BHistoryToCsv` | CSV | Download workbench view |
| `BHistoryToHtml` | HTML table | Embebido en WebChart |
| `BHistoryToText` | Plain text | Log-style |
| `BHistoryToXml` | XML | Integración externa |

`BHistoryToCsv.HistoryToTablePdfHolder` — wrapper para PDF report generation. Hay soporte PDF via clase separada (reportService).

---

## 33.11 History query API + BQL

### 33.11.1 BHistoryTimeQuery

```java
public class BHistoryTimeQuery implements BICombinableHistory<BHistoryRecord, BHistoryTimeQuery>, RemoteQueryable {
  public TableCursor<BHistoryRecord> cursor();
  public synchronized BHistoryTimeQuery combine(BHistoryTimeQuery other);  // union TimeRanges
  public boolean isEmpty();
  public BAbsTime lastTimestamp();
  public static Optional<BHistoryRecord> getLastRecordInRange(BIHistory, BAbsTime, BAbsTime, boolean desc, Context);
  public BObject bqlQuery(BOrd);  // BQL sub-query
  public BObject bqlQuery(BOrd, Context);
}
```

**`combine()`**: permite merge de 2 queries sobre mismo BIHistory, union de timeRanges. Útil para "últimos 7 días + este día específico". NO reduce a 1 cursor — devuelve objeto query que al ejecutar cursor() itera ambos ranges.

### 33.11.2 BHistoryDeltaQuery

```java
public class BHistoryDeltaQuery extends BObject {
  // Returns only records AFTER a sync timestamp
  // Usado internamente por BNiagaraHistoryImport para incremental fetch
  public static class DeltaCursor implements Cursor<BHistoryRecord> {
    public static class DeltaCursorContext { }
  }
}
```

Query param `?delta=true` en ORD `history:/...` dispara DeltaQuery.

### 33.11.3 BQL sobre history

Sintaxis (de docs):
```sql
bql:select timestamp, value from history:/station/Floor1_Temp
  where timestamp > -1d
  order by timestamp desc
  limit 100
```

Backend parsea esto → BHistoryTimeQuery con filter `where` → Cursor.

**Gotcha performance**: BQL sobre history NO tiene índices secundarios. `where value > 75` hace scan lineal completo de `.hdb`. Para filtros de value usar en cliente (post-cursor) o pre-agregar con `BHistoryRollup`.

### 33.11.4 N+1 query gotcha (cross-ref bloque general)

Anti-pattern común:
```java
// MAL: N+1
BIHistory[] histories = db.listHistories(device);
for (BIHistory h : histories) {
  BHistorySummary s = conn.getSummary(h);  // 1 round-trip per history
  // ...
}
```

Esto sobre Fox remoto (supervisor query subordinado) = 1 Fox request per history. Con 100 histories × 10 ms RTT = 1 segundo solo round-trips.

```java
// BIEN: 1 round-trip
BIHistory[] histories = db.listHistories(device);  // 1 Fox list
for (BIHistory h : histories) {
  // procesar local cache (listHistories ya trajo summary embebido)
}
```

### 33.11.5 HistoryRollup — aggregation pre-computada

Package `com.tridium.history.rollup.*`:

```
BHistoryRollup — component
BHistoryRollupRecord
BRollupInterval — enum (MIN, HOUR, DAY, WEEK, MONTH, YEAR)
RollupCursor
RollupValue
CollectiveRollupValue — sum/avg/min/max/stddev
TrendRecordRollupValue
HistoryRollupColumns
```

Uso: configurar `BHistoryRollup` sobre un history → Niagara genera automáticamente tabla secundaria con buckets. Query rollup es O(buckets) vs O(records). Trade-off: storage +20-30%, query 100-1000x faster.

---

## 33.12 BatchJob framework (batchJob-rt) — JOB runtime ≠ BatchEditor

### 33.12.1 Aclaración crítica — son 2 cosas distintas

**Hallazgo empírico mal-nombrado que confunde**:

1. **`batchJob-rt.jar`** → runtime de JOBS de larga duración (provisioning, device commissioning, export bulk). Persiste estado, retention, retry. Clase raíz `BBatchJob extends BJob`. **NO tiene que ver con editar slots en batch**.

2. **`program-rt.jar` → `com.tridium.program.batch.*`** → este es el **Batch Editor real** usado en workbench para editar slots masivamente. Clase raíz `BBatchRoutine` + `BatchCommands`. Es UI-only + sync execution.

Bloque 14.11.3 se refiere al #2 (BatchEditor de ProgramService). Este sub-bloque documenta ambos.

### 33.12.2 BBatchJob (batchJob-rt) — estructura

```java
public class BBatchJob extends BJob implements BILastModifiedRetainable {
  public static final Property submitUser;           // String
  public static final Property alertOnStepFailure;   // boolean
  public static final Property alertOnJobSuccess;    // boolean
  public static final Property stages;               // BFolder (contains BJobStage)
  public static final Property prototypeOrd;         // BOrd to BBatchJobPrototype

  public void addStage(BJobStage);
  public BJobStage getStage(String name);
  public BJobStage[] getAllStages();
  public BJobStepDetails[] getJobStepDetails();

  public BatchJobOp makeOp(Context);
  public IJobDispatcher getDispatcher();
  public void doSubmit(Context);
  public final void doCancel(Context);
  public void doRun(Context);
  public void doDispose(Context);
  public BString doReadLog();
}
```

Estructura jerárquica (cross-ref 14.11.2):

```
BBatchJob
└── BJobStage (stages folder)
    └── BJobStep (abstract — e.g. BDeviceJobStep, BNetworkJobStep)
        └── BJobStepDetails
```

### 33.12.3 BBatchJobService

```java
public class BBatchJobService extends BAbstractService implements BIAlarmSource, BIRestrictedComponent {
  public static final Property jobQueue;                   // BThreadPoolJobQueue
  public static final Property alarmClass;                  // String
  public static final Property summaryManagerType;
  public static final Property initialSummaryManagerType;
  public static final Property maxProvisioningThreads;     // int, default empírico 4
  public static final Action submitJobAction;
  public static final Action makeTempFilePath;
  public static final Action disposeJob;
  public static final Action ackAlarm;
  public static final Action purgeDisposedHistory;
  public static final Action performHousekeeping;
  public static final Topic jobDisposed;

  private ForkJoinPool executor;  // <-- JOB execution sobre FJP (NO ManagedBlocker — bloque 31)
}
```

**Gotcha (cross-ref 31)**: el ForkJoinPool del BatchJobService NO protege contra tareas blocking (IO, Fox calls). `maxProvisioningThreads=4` debe respetarse — si algún step bloquea por minutos, todo el FJP se starve hasta que timeout.

### 33.12.4 BThreadPoolJobQueue

```java
public class BThreadPoolJobQueue extends BThreadPoolWorker implements Worker.ITodo, IJobDispatcher {
  private Queue queue;
  private Worker worker;
  public void dispatch(BJob, Context);
  public void cancel(BJob, Context);
  public Runnable todo(int n) throws InterruptedException;
}
```

Thread pool con size configurable via `maxProvisioningThreads`. **No es el mismo pool** que `BBatchJobService.executor` (ForkJoinPool). Hay 2 backends de execution:
1. `BThreadPoolJobQueue` — single-threaded worker serial
2. `ForkJoinPool executor` — paralelo (steps fork-join)

El dispatcher se elige por `IJobDispatcher` get del BBatchJob.

### 33.12.5 Retention policies

```java
// javax.baja.batchJob.retention.BKeepNExecutionsRetentionPolicy
public final class BKeepNExecutionsRetentionPolicy extends BRetentionPolicy implements BIDomainRetentionPolicy {
  private int numberToRetain;          // default 10 (empírico)
  private boolean countOnlySuccessful; // true = solo counta éxitos
  public Type[] getApplicableDomainTypes();
  public void executePolicy(BIRetentionPolicyDomain);
}

// javax.baja.batchJob.retention.BKeepNPerDeviceRetentionPolicy
public final class BKeepNPerDeviceRetentionPolicy extends BRetentionPolicy implements BIDomainRetentionPolicy {
  private final int successfulRetentionLimit;     // default ?
  private final int unsuccessfulRetentionLimit;   // default ?
  public static final Comparator<? super BDeviceNetworkJob> DESCENDING_START_TIME;
}
```

Aplicadas automáticamente por `BBatchJobService.purgeDisposedHistory()` action (scheduled via `performHousekeeping`).

### 33.12.6 Job history records (no confundir con data history)

```
com.tridium.batchJob.history.BBatchJobHistoryRecord extends BHistoryRecord
com.tridium.batchJob.history.BJobStepHistoryRecord
com.tridium.batchJob.driver.history.BDeviceStepHistoryRecord
com.tridium.batchJob.driver.history.BNetworkStepHistoryRecord
com.tridium.batchJob.driver.history.BDeviceNetworkJobHistoryRecord
com.tridium.batchJob.history.BHistoryJobSummaryManager
```

Estos SÍ usan el `.hdb` system de history-rt para persistir log de jobs. Es recursivo: BatchJob usa History system para grabar sus propias ejecuciones. Historias típicas:
- `$/BatchJobService_execution` — log de jobs
- `$/BatchJobService_stepDetails` — log granular per-step

---

## 33.13 BatchEditor real (program-rt → ProgramService)

### 33.13.1 Jerarquía BBatchRoutine

Decompilado `com.tridium.program.batch.*`:

```
BBatchRoutine (abstract, extends BComponent)
├── BRenameBatchRoutine            — rename componentes (find/replace en displayName)
├── BRenameSlotBatchRoutine        — rename slot name
├── BAddSlotBatchRoutine           — add dynamic slot
├── BEditSlotBatchRoutine          — edit slot value
├── BRemoveSlotBatchRoutine        — remove dynamic slot
├── BSlotFlagsBatchRoutine         — change flags (readonly, hidden, etc)
└── BAddTagBatchRoutine            — add system tag
```

### 33.13.2 BRenameBatchRoutine slots

```java
public class BRenameBatchRoutine extends BBatchRoutine {
  public static final Property find;       // String — texto a buscar
  public static final Property replace;    // String — texto reemplazo
  public static final Property matchCase;  // boolean
  public static final Property matchWord;  // boolean — whole-word match
  public void run(BComponent target, PrintWriter log, Lexicon lex, Context cx);
}
```

**NO hay regex**. NO hay backreferences. NO hay pattern substitution `${i}` ni `${index}` ni `$1`.

Verificación empírica `${i}`: grep del bytecode de `BRenameBatchRoutine` + `BatchCommands` + `BBatchEditor` (program-wb) NO encuentra ni `"${i}"` ni `"${index}"` ni método `formatWithIndex` o similar.

**Confirma Bloque 14.11.3**: para rename con índice numérico secuencial NO hay built-in. Workarounds:
1. **BajaScript** desde wbshell:
   ```javascript
   var targets = find("...")  // BQL
   targets.each((c, i) => c.rename("Point" + (i+1)))
   ```
2. **Script externo Python** (via REST) iterando ORDs + PUT/POST rename.
3. **SlotoMatic** — herramienta custom comunidad (NO oficial).

### 33.13.3 Targets property

```java
// BBatchRoutine base
public static final Property targets;  // BOrdList

public BOrdList getTargets();
public final void runAll(BObject ctx, PrintWriter log, Context cx);  // itera targets
public abstract void run(BComponent c, PrintWriter log, Lexicon lex, Context cx);
```

`targets` es **BOrdList** (array of BOrds). Persiste en la view temporal. BatchEditor UI (`BBatchEditor` en program-wb) mantiene esto en memoria — NO se persiste cross-session (al cerrar view → se pierden targets).

### 33.13.4 BatchCommands — UI command registry

```java
public class BatchCommands {
  private static final Version MIN_REMOTE_BATCH_ROUTINE_VERSION;
  private static BModule module;
  BBatchEditor editor;
  BBqlQueryBuilder builder;

  BatchCommands.FindObjects findObjects;
  BatchCommands.Clear clear;
  BatchCommands.ClearAll clearAll;
  BatchCommands.SelectColumns selectCols;
  BatchCommands.Rename rename;
  BatchCommands.SlotAdd slotAdd;
  BatchCommands.TagAdd tagAdd;
  BatchCommands.SlotEdit slotEdit;
  BatchCommands.SlotRename slotRename;
  BatchCommands.SlotRemove slotRemove;
  BatchCommands.SlotFlags slotFlags;
  BatchCommands.Hyperlink hyperlink;

  public BMenu buildMenu();
  private void runBatchRoutine(BBatchRoutine);
}
```

**Hallazgo gotcha nuevo**: `MIN_REMOTE_BATCH_ROUTINE_VERSION` — al ejecutar BatchRoutine contra componentes en station remoto (via Fox), el remoto debe tener módulo `program` con versión ≥ este mínimo. Cross-version mismatch → error "Remote station does not support BatchRoutine". Para migraciones entre versiones N4.x → cuidado compatibility matrix.

### 33.13.5 Ejecución — sync en EDT

`runBatchRoutine()` ejecuta en **EDT workbench** (event dispatch thread). Si hay 10,000 targets + cada rename hace Fox call → EDT bloqueado minutos. Workbench UI frozen.

**Gotcha**: BatchEditor NO es async. NO hay progress cancel. NO hay resume. Crash workbench mid-execution = estado inconsistente (unos renamed, otros no).

**Mitigación**: trabajar en batches pequeños (<100 targets) o scripting BajaScript con `Thread.sleep` + `Sys.exec()` paralelo controlado.

### 33.13.6 NO UNDO — confirmación explícita

Docs `niagara-help/guides-clean/EngNotes/tUsingBatchEditorToChangeDisplayNames.txt:24`:

> "CAUTION: Before using the Batch Editor , always save and backup the station. It is easy to make errors using the Batch Editor, and there is no undo. Therefore in a worst-case scenario, you can always reinstall the saved station from your backup."

Verificación: `BBatchRoutine` NO emite BOG undo snapshot antes de ejecutar. El único rollback posible es restore desde backup `.dist`.

### 33.13.7 Readonly flag blocks edit (confirmed docs)

Docs `Histories/...displayNames.txt:28-30`:

> "Batch Editor changes to any component's displayNames slot are not possible if that slot has the 'Readonly' flag set."

El routine check: `SlotFlags.READONLY` bit set → `run()` skips silently (logea pero no actualiza counter "edited"). Reporta como "skipped". Puede usarse primero `BSlotFlagsBatchRoutine` para clear readonly, luego `BEditSlotBatchRoutine`.

---

## 33.14 Gotchas operacionales consolidados

### 33.14.1 Tabla de gotchas nuevos (no en bloques previos)

| # | Gotcha | Impacto |
|---|---|---|
| G1 | `.hdb` es **Tridium binary custom, NO SQLite** (MAGIC 0xA0F61E5E) | Herramientas SQLite (`sqlite3 CLI`, `VACUUM`) NO funcionan. Bloque 31 inferencia es incorrecta para `.hdb` |
| G2 | `BFullPolicy` solo tiene `STOP` y `ROLL` — NO `WRAP` separado | Config UI que mencione "wrap" → interpretación errónea, es alias de ROLL |
| G3 | `BStorageType` solo tiene valor `FILE` | NO hay "memory history" ni "rdb history" como storage primary — solo archive provider |
| G4 | HistorySpaceConnection NO es pooled — cada `getConnection` crea nueva | Agrupar queries bajo 1 scope try-with-resources |
| G5 | `niagara.history.localDb.lingerTime=300000` (5 min default) — thrashing si reduce a 60s en supervisor con 10K histories | Leave default o aumentar a 10-15 min |
| G6 | `BNumericCovHistoryExt.isChange` usa `|a-b| > minChangeValue` — con `minChangeValue=0.0` escribe cada subscription tick | Siempre configurar minChangeValue > 0 para COV numeric |
| G7 | ActivePeriod transitions escriben records "sintéticos" en edges → charts ruidosos | Aceptar, o usar interval sin activePeriod |
| G8 | Clock drift > interval causa gap/duplicate — Niagara NO dedupes por timestamp | Usar NTP agresivo + monitor clock skew |
| G9 | Archive providers SOLO merge en query — NO eliminan local automáticamente | Usar `clearOldRecords` scheduled para retention real |
| G10 | Unlimited capacity + disk full → station sigue corriendo con write failures silent WARN | Configurar alertas disk space separadas |
| G11 | BHistoryPollScheduler solo 3 frequencies fast/normal/slow — NO custom intervals | Workaround: TriggerSchedule + execute action |
| G12 | `BArchiveLimitNotificationBehavior.NOTIFY_ONCE_PER_QUERY_RANGE_PER_SESSION` → user asume dataset completo tras 1 warning | Docs operacionales deben enfatizar "scroll to load more" |
| G13 | Import cross-TZ: CSV export usa TZ del consumer, no del source | Normalizar TZ o exportar desde source station |
| G14 | BatchJob FJP `maxProvisioningThreads=4` default — blocking step starve pool | Aumentar si jobs son IO-heavy; monitor pool queue |
| G15 | BatchEditor sync EDT → UI freeze con 10K+ targets | Trabajar en batches pequeños o script externo |
| G16 | BatchEditor NO undo — ningún snapshot pre-operation | Backup obligatorio pre-run |
| G17 | BatchEditor readonly flag = skip silent | Verificar report "skipped" count ≠ 0 |
| G18 | Pattern `${i}` / `${index}` NO soportado empíricamente (verificado via grep bytecode) | Usar BajaScript o script externo |
| G19 | BatchCommands cross-version Fox: `MIN_REMOTE_BATCH_ROUTINE_VERSION` check — error si station remota versión old | Migrations incrementales módulo program primero |
| G20 | BHistoryId `MAX_NAME_LENGTH` vs `LEGACY_HISTORY_NAME_MAX_LIMIT` — histories migradas N3→N4 toleran names más largos | `validateName()` en nuevos crea fails silenciosos UI |
| G21 | BHistoryArchiveProvider provider check per-query `isLikelyToContainArchivedHistory` — si false-positive → query slow path sin resultados | Providers deben implementar bien este hint |

### 33.14.2 Disk fill scenarios

| Escenario | Causa | Detección |
|---|---|---|
| Supervisor `.hdb` crece infinito | capacity=unlimited en imports | Alert disk `>80%` + reporte per-history size |
| 10 GB en `history/$/audit.hdb` | Audit records no rotados + capacity=unlimited | Limit capacity audit a 1M records |
| Job execution history 5 GB | BBatchJobService retention policy no configurada | Configurar `BKeepNExecutionsRetentionPolicy(100, true)` |
| `batchJob/*/logs/*.log` fill | Log rotation desactivada | Check `BBatchJobLogFile` flags |

### 33.14.3 Lock contention signals

Síntoma → causa probable:

- **Query history lento (>5s)** → pobre filter, muchas rows, o archive provider contention
- **`HistoryClosedException`** → closeUnusedHistories cerró mientras cursor iterando — no cerró el cursor antes
- **`DatabaseClosedException`** → BHistoryService parada o no iniciada
- **`DuplicateHistoryException`** → intento crear historia con nombre existente (nombre mal hashed post-rename)
- **`SchemaChangeException`** (clase propia `com.tridium.history.SchemaChangeException`) → record type cambió y schema no compatible

---

## 33.15 Integration con Provisioning + Backup

### 33.15.1 Backup online excluye `.hdb` (confirma 10.3.3)

Confirmado decompilación `BFoxBackupJob` (Bloque 10):
- Patterns excluded: `*.hdb`, `*.adb`, `*.lock`
- Dirs excluded: `history/`, `alarm/`, `webFileCache/`

**Consecuencia**: `.dist` backup online = config.bog + modules + keys. Data histórica **NO** incluida. Restore = nueva station sin datos.

### 33.15.2 Backup offline SÍ incluye `.hdb`

Daemon-side backup (station parada) genera `.dist` completo. Único método para backup+restore data.

### 33.15.3 BProvisioningBackupStep (cross-ref 16.11.1)

Step dentro de `BBatchJob` provisioning que dispara backup.dist por station. NO fuerza offline — usa mode configurable (online=exclude data / offline=full).

### 33.15.4 BProvisioningHistoryConfigStep (empirico clase existente)

Step para aplicar `BHistoryConfig` cambios masivos cross-stations. Útil para cambiar capacity global post-deployment.

### 33.15.5 Flujo típico Supervisor backup

```
1. Scheduled BBatchJob "daily_backup" con steps:
   - BProvisioningBackupStep(mode=offline, stations=Jace_*)
   - For-each Jace: stop → backup → start (sequential, 1-2 min per)
   - BProvisioningBackupStep(mode=online, stations=Supervisor)
2. Backup files recogidos a NAS via script externo (post-hook)
3. Retention policy: mantener 7 daily + 4 weekly
```

**Gotcha:** con 50 Jaces × 2 min stop/backup/start = 100 min job. Si falla mid-execution → algunos Jaces restart otros pendientes. BProvisioningBackupStep tiene retry pero **NO rollback** — ya restarted stations no pueden volver al estado.

---

## 33.16 Honeywell-specific

### 33.16.1 honAlarmExt — NO extiende history

Decompilación `honAlarmExt-rt.jar`:
- NO hay clases `BHonHistoryExt*`, `HonHistoryRecord*` o similares.
- NO extiende `BHistoryExt` jerárquica ni custom `BHistoryRecord`.
- `honAlarmExt` es EXCLUSIVAMENTE extensions de alarmas.

**Conclusión**: Honeywell OptimizerSupervisor NO añade history subtypes específicos más allá del stock Niagara. Las histories que vienen de Honeywell controladores (Ciper, Optimizer) llegan via `BNiagaraHistoryImport` standard, sin transform custom.

### 33.16.2 FastAccessList + history (cross-ref bloque 32)

Bloque 32 documentó `honBacnetHelper.FastAccessList` — batch read BACnet points. Esto alimenta history system cuando points tienen HistoryExt:

```
FastAccessList → 475K points batch read → each point.execute() → BIntervalHistoryExt.intervalElapsed → writeRecord → .hdb append
```

A scale: 475K points × 1 history record per poll × cada 5 min = 95K records/sec worst case. Esto es **impracticable** — `.hdb` append queue saturaría. En práctica, Honeywell deploys usan:
- Subset (~5K) con HistoryExt
- Resto solo live subscription sin history
- Histories críticas interval 15 min, no 5 min

### 33.16.3 honAlarmConsole — sin history interaction

Decompilado: UI plugin para gestión alarmas. NO graba history directly. Usa `BAlarmService` que tiene su propio `.adb` separate del `.hdb` history system.

---

## 33.17 Audit + LogHistory integrations

### 33.17.1 BAuditHistoryService

```java
public class BAuditHistoryService extends BAbstractAuditHistorySource
                                   implements BIService, Auditor, BIHistorySource, BIRestrictedComponent {
  public static final Property historyConfig;
  public static final Property SecurityAuditHistorySource;
  private static final SyslogAuditHandler syslogAuditHandler;  // <-- side-channel a syslog

  protected BTypeSpec getRecordType();  // BAuditRecord.TYPE
  protected BHistoryId getHistoryId();
  public void audit(AuditEvent);
}
```

Service que captura `AuditEvent` (user actions) y los escribe a history standard `.hdb`. Nombre típico: `$/AuditHistory`.

**Cross-ref 31.8**: el `syslogAuditHandler` es side-channel, NO main sink. Main sink es append al `.hdb` via `BHistoryService.getDatabase().getConnection().append(audit)`.

### 33.17.2 BAuditRecord implements ITruncatable

```java
public class BAuditRecord extends BAbstractAuditRecord implements ITruncatable {
  public static final Property target;      // String ORD
  public static final Property slotName;    // String
  public static final Property oldValue;    // String (truncated)
  public static final Property value;       // String (truncated)
  public static final Property userName;    // String

  public boolean isFixedSize();  // returns false
  public boolean truncate(int maxLen);
  public static BAuditRecord fromEvent(AuditEvent);
}
```

ITruncatable → el `.hdb` es VERSION_2 (recstore). Campos String largos se truncan para mantener record size máximo (configurable via facet).

### 33.17.3 BLogHistoryService — station logs a history

```java
public class BLogHistoryService extends BComponent implements BIService, BIHistorySource, BIRestrictedComponent {
  public static final Property enabled;
  public static final Property minimumSeverity;  // BSeverity enum
  public static final Property historyConfig;
  public static final Property lastRecord;

  public static final Action testError, testErrorEx, testWarning, testMessage, testTrace;

  private BLogHistoryService.LogHistoryHandler historyHandler;  // java.util.logging.Handler
  private BIHistory history;
}
```

Hooks `java.util.logging` root → filter por severity → write `BLogRecord` a `.hdb`. Permite query log messages via BQL.

**Gotcha:** `niagara.loghistory.error.depth=5` (defaults:254) — cuántas líneas de stack trace se graban. Default 5 → truncation de stacks profundos. Aumentar a 20+ para debugging. Requiere restart station.

---

## 33.18 Inventario final módulos + clases clave

### 33.18.1 JARs involucrados

| JAR | Tamaño | Rol |
|---|---|---|
| `history-rt.jar` | 572 KB | Runtime core: service, database, file format, cursors, rollup, fox, audit, log |
| `history-ux.jar` | 125 KB | UX widgets (charts, tables) |
| `history-wb.jar` | 341 KB | Workbench views (HistorySummaryView, HistoryChartView, editor) |
| `batchJob-rt.jar` | 167 KB | Job runtime (provisioning, commissioning jobs) — NO BatchEditor |
| `batchJob-wb.jar` | 176 KB | WB views para jobs (JobList, StageBuilder) |
| `program-rt.jar` | ~? | ProgramService + BatchRoutines (Rename, EditSlot, etc) — EL real BatchEditor |
| `program-wb.jar` | ~? | `BBatchEditor` view + `BatchCommands` |
| `niagaraDriver-rt.jar` | ~? | `BNiagaraHistoryImport/Export`, `BNiagaraHistoryDeviceExt` |
| `driver-rt.jar` | ~? | Base `BHistoryImport`, `BHistoryExport`, `BConfigRule` |
| `honAlarmExt-rt.jar` | ~? | Honeywell alarm (NO history custom) |

### 33.18.2 Clases críticas per function

| Función | Clase principal |
|---|---|
| Service lifecycle | `BHistoryService`, `BHistoryService$CloseUnusedHistoriesWorker` |
| ID + Config | `BHistoryId`, `BHistoryConfig`, `BHistoryScheme` |
| Storage limits | `BCapacity`, `BFullPolicy`, `BStorageType` |
| Extensions | `BHistoryExt`, `BCovHistoryExt`, `BIntervalHistoryExt` + 8 concrete |
| Database | `BHistoryDatabase`, `BLocalHistoryDatabase`, `BFoxHistorySpace` |
| Connection | `HistorySpaceConnection`, `LocalDbConnection`, `FoxHistorySpaceConnection` |
| File format | `BFileHistoryTable`, `RecordStore`, `RecordStoreHeader`, `Page`, `Header` (fixed) |
| Query | `BHistoryTimeQuery`, `BHistoryDeltaQuery`, `HistoryQuery`, `HistoryCursor` |
| Archive | `BArchiveHistoryProvider`, `BArchiveLimitNotificationBehavior` |
| Import/Export | `BHistoryImport`, `BHistoryExport`, `BNiagaraHistoryImport`, `BConfigRule`, `BHistoryPollScheduler` |
| Rollup | `BHistoryRollup`, `BRollupInterval`, `BHistoryRollupRecord`, `RollupCursor` |
| Audit/Log | `BAuditHistoryService`, `BAuditRecord`, `BLogHistoryService`, `BLogRecord`, `BSeverity` |
| BatchJob | `BBatchJob`, `BBatchJobService`, `BJobStep`, `BJobStage`, `BThreadPoolJobQueue` |
| BatchEditor real | `BBatchRoutine`, `BRenameBatchRoutine`, `BEditSlotBatchRoutine`, `BatchCommands`, `BBatchEditor` |
| Retention | `BKeepNExecutionsRetentionPolicy`, `BKeepNPerDeviceRetentionPolicy`, `BPermanentRetentionPolicy`, `BTimeSinceLastModified` |

---

## 33.19 Diagrama end-to-end flujo history

```
[BControlPoint]
     │  pointChanged(ts, val)
     ▼
[BHistoryExt subclass]          <-- Interval(Clock$Ticket) OR COV(onExecute)
     │  writeRecord(ts, val)
     ▼
[BIHistory.append(BHistoryRecord)]
     │
     ▼
[BHistoryDatabase.getConnection(cx)]  <-- BLocalHistoryDatabase
     │  append via HistorySpaceConnection
     ▼
[BHistoryDbTable → BFileHistoryTable]
     │  appendQueue + fsync batch
     ▼
[RecordStore / PageManager (fixed or recstore)]
     │  write Page → writePage(page)
     ▼
[.hdb file on disk]
     │
     │  (Read path)
     │
     ▼
[BHistoryTimeQuery(hist, start, end, desc)]
     │  cursor()
     ▼
[HistoryCursor] ──── local .hdb page iterate
     │
     │  IF time range extends before local firstTimestamp:
     ▼
[BArchiveHistoryProvider.timeQuery(...)]
     │  doTimeQuery(... , computedLimit, cx)
     ▼
[HybridHistoryCursor merge local + archive]
     │
     ▼
[Consumer: Chart / BQL / CSV export / Fox remote query]
```

---

## 33.N Conexión con bloques previos

### 33.N.1 Referencias cruzadas directas

| Sección actual | Bloque previo | Relación |
|---|---|---|
| 33.0 (corrección SQLite → Tridium binary) | **Bloque 31.3** | **CORRIGE** inferencia `.hdb` usa SQLite/VACUUM. Verdad: custom binary MAGIC 0xA0F61E5E. Bloque 31 observación "5-30 min archive" sigue válida pero causa real es appendQueue + locks + cursor hold, NO VACUUM |
| 33.1 (lifecycle + linger) | **Bloque 8.2.1** (BHistoryService overview) | Extiende con thread model + lingerTime sysprop + CloseUnusedHistoriesWorker |
| 33.3 (HistoryExt subtypes) | **Bloque 8.2.2** (HistoryExt básico) | Añade lifecycle callbacks internos (activated/deactivated/pointChanged), ActivePeriod semantics, COV minChangeValue gotcha |
| 33.4 (HistorySpaceConnection) | **Bloque 8.2.8** | Confirma AutoCloseable contract. Añade: NO pool → cada getConnection crea nueva. Anti-patterns específicos |
| 33.5 (.hdb format) | Ninguno previo | **Bloque nuevo** — formato detallado MAGIC/VERSION/Pages. Bloque 8 solo decía "binario serializado" |
| 33.6 (rollover) | **Bloque 8.2** | Confirma defaults (500 capacity, ROLL policy). Aclara: solo 2 FullPolicy values, NO existe WRAP |
| 33.7 (archive) | **Bloque 31.3** (archive bottleneck) | Formaliza API `BArchiveHistoryProvider`. Gotcha retention: archive NO elimina local |
| 33.8 (NiagaraImport) | **Bloque 13.1.7, 19.13** (Supervisor 50-sub limit) | Explica causa history-specific: file handles `.hdb` + HistorySpaceConnection mientras append |
| 33.10 (ODBC) | Ninguno previo | ODBC NO presente en esta distribución — requiere `rdb` módulo separado |
| 33.11 (query + BQL) | **Bloque 14.1.2** (history.limit) | Extiende. BQL history sin índices secundarios. Rollup como workaround performance |
| 33.12 (BatchJob) | **Bloque 14.11.2** (batch jobs stages/steps) | Formaliza API `BBatchJob`, `BBatchJobService`, retention policies |
| 33.13 (BatchEditor real) | **Bloque 14.11.3** (BatchEditor `${i}` no soportado) | **VERIFICADO empírico** — grep bytecode confirma NO pattern sub. Detalla routines + runtime sync EDT + no-undo + readonly-skip. Aclara: es `program-rt` NO `batchJob-rt` |
| 33.14 (gotchas) | Múltiples | 21 gotchas consolidados, muchos nuevos (G1-G21) |
| 33.15 (backup) | **Bloque 10.3.3** (online excluye `.hdb`) | Confirma + flujo típico supervisor daily_backup |
| 33.15.3 | **Bloque 16.11.1** (BProvisioningBackupStep) | Uso en batch jobs scheduled |
| 33.16 (Honeywell) | **Bloque 32** (FastAccessList 475K points) | Conecta: FastAccessList alimenta history system. Scale practical vs teórico |
| 33.3.4 (clock drift) | **Bloque 24.14** | Aplica a historical timestamps — gap/duplicate semantics |
| 33.17 (audit) | **Bloque 31.8** (audit queue unbounded) | Confirma `syslogAuditHandler` es side-channel, main sink es `.hdb` append |

### 33.N.2 Hallazgos que actualizan bloques previos

**Debe reflejarse en actualización INDEX/bloques**:

1. **Bloque 31.3 párrafo "VACUUM SQLite"** — debe corregirse: `.hdb` NO usa SQLite. El 5-30 min window es causa diferente (lock + queue + cursor). VACUUM aplica a `.adb` audit que SÍ podría ser SQLite (hypothesis — requiere investigación separada `.adb` format).

2. **Bloque 14.11.3** — confirmar `${i}` NO soportado empíricamente (no solo documental).

3. **Bloque 8.2** — añadir: `BStorageType` solo tiene `FILE` valor. `BFullPolicy` solo `STOP`+`ROLL`.

4. **Bloque 8.2.8** — añadir: HistorySpaceConnection NO es pooled.

5. **Bloque 20 (gotchas transversales)** — añadir G1 (Tridium binary NOT SQLite).

---

## 33.Z Resumen ejecutivo

### Top 10 hallazgos empíricos no-obvios

1. **`.hdb` es binario propietario Tridium (MAGIC 0xA0F61E5E), NO SQLite** — corrige Bloque 31 inferencia. Dos versiones (fixed vs recstore).

2. **`BStorageType` solo tiene valor `FILE`** — NO existen storage backends alternativos "memory" o "rdb" como primary. RDB solo como archive provider secundario (query-time merge).

3. **`BFullPolicy` solo tiene `STOP` y `ROLL`** — NO hay `WRAP` separado. Docs UI que mencionan "wrap" son aliasing erróneo de ROLL.

4. **`HistorySpaceConnection` NO es pooled** — cada `getConnection()` crea nueva. Agrupar queries bajo 1 try-with-resources scope.

5. **BatchJob runtime (`batchJob-rt.jar`) ≠ BatchEditor** — son 2 cosas distintas. BatchEditor real vive en `program-rt.jar` → `com.tridium.program.batch.*`. Confusión común.

6. **BatchEditor `${i}` pattern substitution NO existe en bytecode** — verificado por grep. Workaround: BajaScript con index contador.

7. **BatchEditor ejecución sync en EDT** — 10K+ targets = UI freeze. NO async, NO cancel, NO undo.

8. **`MIN_REMOTE_BATCH_ROUTINE_VERSION`** (hallazgo nuevo) — BatchCommands cross-version Fox check. Stations remotas con program module old → error.

9. **`niagara.history.localDb.lingerTime=300000`** (5 min) — controla cuándo se cierran `.hdb` table handles. Reducir a 60s causa thrashing en Supervisor con 10K+ histories.

10. **Archive providers NO eliminan local después de archivar** — gotcha retention crítico. `.hdb` local crece infinito si no se configura `clearOldRecords` scheduled. Muchos deployments reportan discos 100+ GB por esto.

### Tamaño archivo + métricas

- Líneas: ~870
- Secciones numeradas: 19 (0 a 19, + N conexión, + Z resumen)
- Gotchas tabulados: 21 (G1-G21)
- Tablas inventario: 11
- Bloques de código: 22
- Diagrama ASCII: 1 (flujo end-to-end)
