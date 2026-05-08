# Bloque 60 — Sync engine deep dive + Response classes complejas + BackupManager business logic + 7 nuevos antipatterns AP-56..62 + 2 reglas template MX60 (18-19) + AP-60 HIGH favorites traversal

**Fecha**: 2026-05-08
**Método**: Audit deep-dive de la **capa sync + handlers async WebSocket + 4 Response classes complejas + BackupManager** del módulo Reflow. Cubre los handlers IReflowCommand identificados Bloque 59 (ConfigSyncCommand, RequestControlCommand, ReflowOrdTreeFavoritesRead, ReflowOrdTreeFavoritesWrite) + business logic completo de BReflowSyncService (599L) + BackupManager + 4 Response classes outliers en complejidad.

**Fuentes primarias**:
- `sync/BReflowSyncService.java` (599 líneas) — sync engine + ConfigSyncCommand + RequestControlCommand inner classes
- `sync/commands/ReflowOrdTreeFavoritesRead.java` (72 líneas)
- `sync/commands/ReflowOrdTreeFavoritesWrite.java` (47 líneas)
- `backups/BackupManager.java` (248 líneas)
- `http/responses/HistoryDataResponse.java` (265 líneas) — outlier 2x promedio
- `http/responses/ConfigResponse.java` (118 líneas)
- `http/responses/ConfigUpdateResponse.java` (128 líneas)
- `http/responses/WeatherMapResponse.java` (125 líneas)

**Versión analizada**: Reflow-Clean-177 (réplica clean-room, post-fix `harden-backup-csrf` para BaseServlet POST endpoints).

---

## 60.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Cierre del análisis **business logic** de Reflow. Bloque 59 cubrió la **plomería** WebSocket (acceptor + channel + servlet + lifecycle). Este bloque cubre los **handlers reales** que ejecutan comandos async + las Response classes complejas que sirven endpoints REST + el motor de sync RFC 6902 JSON Patch (referenciado en Bloque 51 pero nunca auditado en detalle) + BackupManager business logic completo.

### Qué corrige / valida

| Bloque previo | Hallazgo previo | Validación / corrección |
|---------------|-----------------|-------------------------|
| 51 (RFC 6902 JSON Patch) | "BReflowSyncService usa zjsonpatch 0.4.14 Flipkart con debounce 5000ms timer + 30s auto-grant" | ✅ **CONFIRMADO empíricamente** — patch via flipkart/zjsonpatch en `ConfigSyncTask.run()` L408-471, debounce vía `saveFileTimer.schedule(TimerTask, 5000)`, auto-grant vía `revokeConfigTimer` 30s. Hallazgo nuevo: `ConfigSyncTask` corre dentro `AccessController.doPrivileged()` con `thread.join()` blocking — sí propaga doPriv scope. |
| 55 (history domain N+1 prediction) | "12 AP-27 sites en history domain" | ✅ **HistoryDataResponse 3 sites adicionales** — `BOrd.make("history:")` en L44, L89, L145. Pero **NO N+1**: usa `Cursor` con `.timeQuery()` lazy. |
| 58 (BReflowSyncService 3 threads + 1 doPriv + 2 BOrd) | "noted Bloque 58 sin deep dive" | ✅ **REFINADO**: son **5 threads custom** — 3 ya identificados (LoadConfig, SaveConfig, ConfigSyncTask) + **2 Timers nuevos** (revokeConfigTimer 30s, saveFileTimer 5s). Total **5 thread spawns sin cx propagation explícita** (AP-49 cousin sistémico). |
| 59 sec 11.3 (predicción favorites filename injection) | "ReflowOrdTreeFavoritesRead/Write posiblemente vulnerable a traversal vía username" | ✅ **CONFIRMADO HIGH — AP-60 NUEVO**: `ReflowOrdTreeFavoritesRead.java:33` hace `"^reflow/favorites/" + user + ".json"` SIN sanitización. Si `socket.acceptCx.getUser().getUsername()` retorna `"../../../etc/passwd"` (LDAP injection / custom auth), traversal trivial. |
| 58 (AP-39 server↔client desync backups) | "POST harden-backup-csrf aplicado, 405 GET legacy" | ⚠️ **NUEVO HALLAZGO AP-61**: `BackupManager.apply()` (L175) hace `"^reflow/backups/" + this.filename + ".json"` SIN sanitización (vs `create()` que SÍ sanitiza con `replaceAll("[<>:\"/\\\\|?*#]", "")`). Inconsistencia: la mitad del código defiende, la otra mitad NO. |

### Pregunta unificadora

> ¿Cuál es la deuda técnica final del backend Reflow (sync + responses + backups), y qué reglas MX60 son obligatorias para no heredarla?

**Respuesta corta**:
- **Deuda sistémica confirmada**: cx propagation ausente en 5+ threads de SyncService, AP-27 expandido a 7 sites Sync + 6 sites Backup + 3 sites HistoryDataResponse = **~16 sites adicionales** post-Bloque 60. **Total AP-27 cross-bloques 53-60: ~66 sites**. Es una de las decisiones arquitectónicas MX60 #1.
- **AP-60 traversal favorites** = HIGH severity, equivalente a AP-33 (filesystem disclosure) pero en sync/commands path.
- **AP-57 credential exposure** (hostId pass-through a weather.niagaramodules.com) = MEDIUM, leak metadata station-level.
- **2 reglas nuevas obligatorias**: Regla 18 (Thread + cx propagation explícita), Regla 19 (filename sanitizer factory pattern).

---

## 60.1 BReflowSyncService.java — DEEP DIVE (599 líneas)

### 60.1.1 Estructura general

```
BReflowSyncService extends BAbstractService
├── Properties:
│   ├── volatile JsonNode config              // null hasta loadConfig
│   ├── volatile activeControlRequestMessage  // cross-thread visibility
│   ├── volatile activeControlRequester       // (RequestControlCommand)
│   ├── lastSync, lastWrite (timestamps)
│   ├── revokeConfigTimer (Timer 30s auto-grant)
│   ├── saveFileTimer (Timer 5s debounce)
│   └── requestTimeout = 30000ms (config)
│
├── Methods:
│   ├── reloadConfigurationFile(String, boolean) → spawn LoadConfigTask
│   ├── saveConfigurationFile()                  → spawn SaveConfigTask
│   ├── requestConfigControl(socket, msg)        → state machine
│   ├── grantConfigControl(socket)               → broadcast
│   ├── queueRevokeConfigControl()               → revokeTimer.schedule(30s)
│   ├── acceptControlRequest()                   → grantConfigControl
│   ├── rejectControlRequest()                   → broadcast denial
│   └── getConfigController()                    → ReflowWebSocket | null
│
├── Inner classes:
│   ├── LoadConfigurationFileTask extends Runnable    (L473-505)
│   ├── SaveConfigurationFileTask extends Runnable    (L561-598)
│   ├── ConfigSyncTask extends Runnable               (L408-471) — PRIVILEGED
│   ├── ConfigSyncCommand extends AsyncReflowCommand  (L371-406)
│   └── RequestControlCommand extends AsyncReflowCommand (L507-559)
│
└── Service registration:
    ├── addCommand(new ConfigSyncCommand())          (en lifecycle.start)
    └── addCommand(new RequestControlCommand())
```

### 60.1.2 Threading — 5 thread spawns custom

| # | Thread name | Spawned at | Purpose | cx propagation? |
|---|-------------|------------|---------|-----------------|
| 1 | `BReflowSyncService.loadConfigurationFile.Task` | L172 | Load config.json from BIFile | ❌ **NO** — `BOrd.make(fileLocation).get()` sin cx |
| 2 | `BReflowSyncService.saveConfigurationFile.Task` | L177 | Save config.json to BIFile | ❌ **NO** — idem |
| 3 | (anonymous, inside doPrivileged) | L355 | ConfigSyncTask apply patch + broadcast | ⚠️ Parcial — doPriv scope respected, but ObjectMapper.createObjectNode L451 sin cx |
| 4 | `revokeConfigTimer` Timer thread | L280-287 | Auto-grant config control after 30s | ❌ **NO** |
| 5 | `saveFileTimer` Timer thread | L331-339 | Debounce config write 5s | ❌ **NO** |

**Total cx propagation sites missing**: 5/5 = **100% sistémico**. Esta es la pieza más densa de AP-49 cousin del módulo.

### 60.1.3 ConfigSyncCommand (inner class L371-406)

**Purpose**: Aplica RFC 6902 JSON Patch al config in-memory. Notifica cliente success + broadcast a "reflow" channel.

```java
class ConfigSyncCommand extends AsyncReflowCommand {
    public String getName() { return "sync-delta"; }

    public void task(ReflowWebSocket socket, JsonNode message, String ticket) {
        if (service.config == null) {
            // sendFullState — primera sync
            socket.send(/* full config */);
            return;
        }
        // ConfigSyncTask runs inside doPrivileged + thread.join()
        AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
            ConfigSyncTask task = new ConfigSyncTask(socket, message, ticket);
            Thread thread = new Thread(task, "BReflowSyncService.configSync.Task");
            thread.start();
            thread.join();  // BLOCKING — waits for patch
            return null;
        });
    }
}
```

**Hallazgos**:
- ✅ doPrivileged scope respetado en thread.join()
- ❌ Si JsonPatch.apply() falla (malformed delta), exception swallowed por outer catch (AP-51 cousin)
- ❌ broadcast post-update SIN context — usa `service.channelService.broadcast("reflow", node)` con node creado sin cx tracking

### 60.1.4 RequestControlCommand (inner class L507-559)

**Purpose**: state machine para config control:
- `who` → query controller actual
- `request` → request control (auto-grant si nadie tiene control, queue revoke timer 30s si otro tiene)
- `accept`/`reject` → respuesta del controller actual al request pendiente

**Hallazgo crítico — AP-58 NUEVO MEDIUM**:
```java
// L532
service.getConfigController().makeClientInfo(null);  // NPE si controller race to null
```

**Race scenario**:
1. T0: Cliente A solicita control → `setActiveControlRequest(A)`
2. T0+100ms: Cliente B (controller actual) se desconecta → `getConfigController()` returns null
3. T0+150ms: Timer auto-grant fires → `acceptControlRequest()` → llama `getConfigController().makeClientInfo()` → NPE

**Severity**: MEDIUM (DoS via NPE crash en Timer thread, ¿propaga? Depende de Niagara handler).

**Fix**: null check post-getConfigController().

### 60.1.5 Sync engine flow

```
Client A: edits config (addPoint, removeEquipment, etc.)
    ↓ Vue local state changes
    ↓ JsonPatch generated client-side (zjsonpatch.diff(oldConfig, newConfig))
    ↓ socket.send({command: "sync-delta", delta: [...] })
    ↓
Server: ReflowWebSocket.onMessage()
    ↓ ConfigSyncCommand.task() → AccessController.doPrivileged
    ↓ ConfigSyncTask.run() (in new Thread, joined):
    │   ├─ patch = JsonPatch.apply(delta, service.config)
    │   ├─ service.config = patch  // in-place update
    │   ├─ saveFileTimer.cancel() + schedule(5s) // debounce write
    │   └─ channelService.broadcast("reflow", patchEvent)
    ↓
All other clients in "reflow" channel:
    ↓ socket.onMessage() → applyPatch → Vue store update → reactive UI
```

**Decisiones arquitectónicas (KEEP)**:
- RFC 6902 JSON Patch (industry standard, Niagara-agnostic) ✅
- 5s debounce write (avoid disk thrashing en multi-edit sessions) ✅
- Volatile flags para cross-thread state ✅
- Broadcast post-apply (vs broadcast pre-apply) — garantiza consistencia ✅

**Decisiones a IMPROVE en MX60**:
- Timer threads sin cx → Regla 18 obligatoria
- ConfigSyncTask ObjectMapper.createObjectNode sin cx propagation
- BroadcastEvent NO incluye sender clientId → can't filter own changes (waste of network)

### 60.1.6 AP-27 sites adicionales en BReflowSyncService

| Línea | Patrón | Thread | Severity |
|-------|--------|--------|----------|
| L491 | `BOrd.make(this.fileLocation).get()` | LoadConfigTask | HIGH (cx sin user) |
| L492 | `(BIFile) ord.get()` | LoadConfigTask | HIGH |
| L578 | `BOrd.make(this.fileLocation).get()` | SaveConfigTask | HIGH |
| L579 | `(BIFile) ord.get()` | SaveConfigTask | HIGH |
| L451 | `mapper.createObjectNode()` | ConfigSyncTask | LOW (no permission check) |

**Total nuevos AP-27 sites**: **5** (4 HIGH + 1 LOW).

---

## 60.2 ReflowOrdTreeFavoritesRead.java + Write.java — AP-60 NUEVO HIGH traversal

### 60.2.1 ReflowOrdTreeFavoritesRead (72L)

```java
public class ReflowOrdTreeFavoritesRead extends AsyncReflowCommand {
    public void task(BReflowWebSocketAcceptor.ReflowWebSocket socket, JsonNode data, String ticket) {
        try {
            String user = socket.acceptCx.getUser().getUsername();      // L31
            String location = ConfigIO.CONFIG_FAVORITES + "/" + user + ".json";  // L33 ← AP-60 SITE
            BDirectory directory = BFileSystem.INSTANCE.getStationHome();
            BIFile jsonFile = directory.getFileSpace().findFile(new FilePath(location));  // L35 ← AP-27
            // ... read + parse + send
        } catch (Exception ex) {
            // log only — AP-51 cousin
        }
    }
}
```

> **AP-60 NEW HIGH** — "Favorites filename traversal via unsanitized username"
>
> **Site**: `ReflowOrdTreeFavoritesRead.java:33` + `ReflowOrdTreeFavoritesWrite.java:30` (concat similar)
>
> **Descripción técnica**: `username` viene de `socket.acceptCx.getUser().getUsername()`. En Niagara N4 estándar, `BUser.getUsername()` retorna el `BUser.parent().name` (slot name) que ya está sanitizado por Niagara (BNameMap rules). **PERO**: si Reflow despliega con BUser custom (ej: LDAP-backed con DN como username, OAuth con email containing `..`), el username puede contener `..` o `/`. Concatenación directa → `^reflow/favorites/../../etc/passwd.json` → traversal.
>
> **Exploit scenario**:
> 1. Atacante crea cuenta LDAP con username `../../../etc/passwd`
> 2. Atacante autenticate via LDAP-backed BUser
> 3. Atacante abre WebSocket, envía `command: "favorites-read"`
> 4. Server: `location = "^reflow/favorites/../../../etc/passwd.json"` → BFileSystem.findFile() puede o no normalizar
> 5. Si findFile() acepta `..` literal → LFI confirmado.
>
> **Mitigación accidental**: Niagara BNameMap convencional restringe usernames. Pero Reflow es AGNÓSTICO al backend de auth — no garantiza sanitización.
>
> **Severity**: HIGH (LFI / disclosure / write-arbitrary-file en Write variant)
>
> **Fix recomendado** (Regla 19 nueva):
> ```java
> String safeUser = FilenameUtil.sanitize(user, FilenameUtil.SAFE_CHARS_ALPHANUMERIC_HYPHEN);
> if (safeUser.length() == 0 || safeUser.length() > 64) {
>     socket.send({ success: false, error: "INVALID_USERNAME" });
>     return;
> }
> String location = ConfigIO.CONFIG_FAVORITES + "/" + safeUser + ".json";
> // Plus canonicalize:
> Path canonical = Paths.get(BFileSystem.INSTANCE.getStationHome().toRealPath(), CONFIG_FAVORITES, safeUser + ".json").normalize();
> if (!canonical.startsWith(stationHome)) throw new SecurityException("path traversal");
> ```

### 60.2.2 ReflowOrdTreeFavoritesWrite (47L)

Mismo patrón — username sin sanitización. Adicionalmente:
- ConfigIO.writeFavorites(favorites, user + ".json") spawns `WriteFavoritesFileTask` SIN `task.join()` → async write sin garantía de durabilidad antes de response. Si servidor crashes entre dispatch y write completion, cliente recibe success pero data NO persisted.

> **AP-62 NEW LOW** — "Favorites write async sin durabilidad garantizada"
>
> **Site**: `ReflowOrdTreeFavoritesWrite.java` + `ConfigIO.writeFavorites()`
> **Descripción**: `task.start()` sin `task.join()`. Cliente recibe ACK success antes de fsync. Server crash entre dispatch + write completion = data loss silencioso.
> **Severity**: LOW (data loss en edge case raro)
> **Fix**: `task.join()` con timeout antes de send success ACK.

---

## 60.3 BackupManager.java — business logic completa (248L)

### 60.3.1 Métodos públicos

| Método | Thread? | Responsabilidad | Notas |
|--------|---------|-----------------|-------|
| `fileExists(filename)` | NO | check backup existencia | findFile() sin cx |
| `age(filename)` | NO | edad en ms desde lastModified | idem |
| `create(filename, overwrite)` | ✅ spawn | Crea backup vía CreateBackupTask | filename **sanitizado** L216 |
| `apply(filename)` | ✅ spawn | Restore config from backup | filename **NO sanitizado** L175 ← AP-61 |
| `destroy(filename)` | NO | BIFile.delete() | filename sin sanitización (caller-trusted) |
| `rename(oldName, newName)` | NO | rename + sanitize newName | `replaceAll("[<>:\"/\\\\|?*#]", "")` |
| `createDailyBackup()` | ✅ spawn | `create("Daily Backup", true)` | static literal, safe |
| `createIncrementalBackup()` | ✅ spawn | `create("Incremental Backup", true)` si `age() > 1h` | static literal, safe |
| `hasDefaultConfiguration()` | NO | parse config.json + check default | `mapper.readTree()` ← AP-NEW BIGJSON_DOS |

### 60.3.2 CreateBackupTask flow

```java
new Thread(new CreateBackupTask(filename, overwrite), "BackupManager.create.Task").start()
```

- L216: `filename.replaceAll("[<>:\"/\\\\|?*#]", "")` — sanitización OK
- L220-222: `BFileSystem.INSTANCE.getStationHome()` + `directory.getFileSpace().makeFile(...)` — **3 AP-27 sites sin cx**
- L227-233: poll loop esperando file size estable (up to 5s, 50 tries) — defensive pattern para asegurar BIFile flushed

### 60.3.3 ApplyBackupTask flow — AP-61 NUEVO HIGH

```java
public static void apply(String filename) throws Exception {
    Thread thread = new Thread(new ApplyBackupTask(filename), "BackupManager.apply.Task");
    thread.start();  // NO .join() — async
}

// Inside ApplyBackupTask.run():
BDirectory directory = BFileSystem.INSTANCE.getStationHome();
BIFile backupFile = directory.getFileSpace().findFile(new FilePath("^reflow/backups/" + this.filename + ".json"));
//                                                                                          ^^^^^^^^^^^^^^^^^^^^^
//                                                                                          AP-61: NO sanitización
```

> **AP-61 NEW HIGH** — "BackupManager.apply() filename traversal asimetría con create()"
>
> **Site**: `BackupManager.java:175` (ApplyBackupTask)
> **Descripción**: `create()` sanitiza filename (L216), pero `apply()` NO. Inconsistencia: mitad del código defiende contra `..`, otra mitad NO. Atacante hace POST `/station/backups/apply?filename=../config` → restore arbitrary file.
> **Exploit**:
> ```
> POST /nmodsreflow/station/backups/apply?filename=../../../sensitive
> → backupFile = findFile("^reflow/backups/../../../sensitive.json")
> → si findFile resolves traversal → arbitrary file restore como config
> ```
> **Severity**: HIGH (config replacement = RCE potencial vía malicious config injection)
> **Fix**: aplicar mismo `replaceAll` que create(). Regla 19 obligatoria.

### 60.3.4 hasDefaultConfiguration() — AP-NEW BIGJSON_DOS

```java
InputStream in = file.getInputStream();
ObjectMapper mapper = new ObjectMapper();
JsonNode json = mapper.readTree(in);  // L119 ← materializa JSON entero
return jsonIsDefaultConfiguration(json);
```

> **AP-NEW MEDIUM** — incluido en AP-61 family, ver Sección 60.7.

Si config.json es 100MB malicioso → readTree() materializa todo → OOM en JVM.

### 60.3.5 Race conditions multi-user backup

**Scenario problemático**:
- T0: User A POST `/station/backups/create?filename=test`
- T0+100ms: User B POST `/station/backups/destroy?filename=test`
- T0+200ms: User C POST `/station/backups/apply?filename=test`

**Sin lock**: 3 threads operando sobre mismo file. ApplyBackupTask puede leer archivo half-written, restoring corrupted config.

**Fix MX60**: ReentrantLock per-filename map en BackupManager.

---

## 60.4 HistoryDataResponse.java (265L) — outlier 2x promedio

### 60.4.1 Por qué es 2x más grande

| Sección | Líneas | Propósito |
|---------|--------|-----------|
| 3 overloads jsonForHistory | ~120L | (h, style, BDateRangeEnum), (h, style, BAbsTime, BAbsTime), (h, style, int) |
| arrayForHistoryCollection | ~40L | dual output format apex vs object |
| jsonArrayForRecord | ~25L | apex format `[timestamp, value]` |
| jsonObjectForRecord | ~25L | object format `{time, value, label}` |
| BQL helpers | ~30L | timeQuery, recordQuery, cursor iteration |
| AccessController + handlers | ~25L | doPrivileged wrap, CSV variant |

### 60.4.2 BQL queries — 3 AP-27 sites

```java
BHistoryDatabase historyDb = (BHistoryDatabase) BOrd.make("history:").resolve().get();  // L44, L89, L145
//                                                              ^^^^^^^^^^^^
//                                                              AP-27 site (3 calls separadas)
BHistory history = (BHistory) historyDb.getHistory(name);
BITable<BHistoryRecord> table = (BITable<BHistoryRecord>) history.timeQuery(start, stop);
Cursor<BHistoryRecord> cursor = table.cursor();
while (cursor.next() && (limit == 0 || count <= limit)) { ... }
```

**Hallazgos**:
- ✅ NO N+1 (predicción Bloque 55) — usa Cursor lazy iteration
- ❌ **3 BOrd.make("history:") repetidos** — cache singleton recomendado (Implication #130)
- ❌ AP-27 sites x3 sin cx
- ✅ `.resolve()` defense-in-depth (presente)

### 60.4.3 BUG NUEVO AP-56: redundant assignment

```java
// L254-256 jsonObjectForRecord
} else if (rec instanceof BEnumTrendRecord) {
    object.put("value", ((BEnumTrendRecord) rec).getValue());        // L254
    BEnumTrendRecord record = (BEnumTrendRecord) rec;
    object.put("value", record.getValue());                          // L256 ← OVERWRITES L254
    if (facets != null && facets.getFacet("range") != null && ...) {
        BEnumRange range = (BEnumRange) facets.getFacet("range");
        object.put("label", range.getDisplayTag(record.getValue().getOrdinal(), null));
    }
}
```

> **AP-56 NEW LOW** — "Redundant value assignment in HistoryDataResponse.jsonObjectForRecord"
>
> **Site**: `HistoryDataResponse.java:254-256`
> **Severity**: LOW (no functional impact, code smell)
> **Causa**: refactor incompleto — alguien agregó `BEnumTrendRecord record = (BEnumTrendRecord) rec` para reusar variable, pero no eliminó la línea original.
> **Fix**: eliminar L254 (o L256, una de las dos).

### 60.4.4 BUG NUEVO AP-59: limit unbounded

```java
while (cursor.next() && (limit == 0 || count <= limit)) {
    count++;
    BHistoryRecord rec = (BHistoryRecord) cursor.get();
    json.put(jsonForRecord(rec, style, facets));
}
```

> **AP-59 NEW MEDIUM** — "Limit unbounded en HistoryDataResponse"
>
> **Site**: `HistoryDataResponse.java:200`
> **Descripción**: si cliente envía `limit=999999999` (entero válido) o `limit=0` (zero = no limit), loop itera todos los records. Para histories de 10M records, materialización JSON entera → OOM + bandwidth blowup.
> **Severity**: MEDIUM (DoS via expensive query)
> **Fix**: `MAX_LIMIT = 10000`. Server-side guardrail. Regla 13 mention.

### 60.4.5 Patterns KEEP

- ✅ `BOrd.make("history:").resolve().get()` con `.resolve()` defense-in-depth
- ✅ Cursor lazy iteration (no materialize)
- ✅ try-with-resources cleanup
- ✅ Dual output format (apex vs object) — flexible API

---

## 60.5 ConfigResponse.java (118L) + ConfigUpdateResponse.java (128L)

### 60.5.1 ConfigResponse — read flow + AP-NEW gzip cache

```java
String location = CONFIG_ORD;  // "^reflow/config.json"
if (req.getQueryString() != null && req.getQueryString().length() > 0) {
    Map<String, String> query = Query.map(req.getQueryString());
    if (query.get("file") != null) {
        location = query.get("file");                          // ← USER-CONTROLLED
        if (location.startsWith("file:")) location = location.substring(5);
    }
}
BIFile jsonFile = directory.getFileSpace().findFile(new FilePath(location));  // ← AP-33 cousin
```

> **AP-NEW (covered by AP-33 expansion)** — `?file=` query param permite arbitrary location sin whitelist. Si `findFile` resolves `..` literal → traversal.
>
> Aplica Regla 12 (Bloque 57) — filesystem access whitelist.

### 60.5.2 Gzip cache pipeline

```java
if (service.getWebCache()) {
    boolean hasCache = ConfigIO.cacheExists(location);
    if (!hasCache) {
        Thread thread = ConfigIO.writeCache(location);
        thread.join();
        hasCache = true;
    }
    if (hasCache && acceptEncoding.contains("gzip")) {
        String cacheLocation = ConfigIO.cacheLocation(location);
        BIFile cacheFile = directory.getFileSpace().makeFile(new FilePath(cacheLocation));
        // stream cache to response
    }
}
```

**Patterns KEEP**:
- ✅ Gzip cache pipeline (bandwidth saver)
- ✅ Accept-Encoding check (graceful degradation)
- ✅ Thread.join() blocking — sync semantics

**IMPROVE**:
- ❌ `ConfigIO.cacheLocation(location)` — si `location` no whitelist, cache key puede contener `..`. Predicción para Bloque 61.

### 60.5.3 ConfigUpdateResponse — write flow

```java
InputStream in = req.getInputStream();
OutputStream out = tempConfig.getOutputStream();
byte[] buf = new byte[1024];
int count;
while ((count = in.read(buf)) >= 0) {
    out.write(buf, 0, count);
}
out.flush(); out.close(); in.close();

long fileWriteSize = tempConfig.getSize();
if (req.getContentLengthLong() != fileWriteSize) {
    tempConfig.delete();
    throw new Exception("Content Length mismatch...");
}
// ... swap temp → real config
```

**Patterns KEEP**:
- ✅ Content-Length validation pre-swap (truncation prevention)
- ✅ Atomic temp file swap (no corrupted config visible)
- ✅ Optional `createIncrementalBackup()` antes de write

**IMPROVE**:
- ❌ AP-51 site L103-105: catch + log + write error HTML (cliente puede no reconocer HTML como error en API call)

### 60.5.4 Broadcast post-update

```java
reflowService.getChannelService().broadcast("reflow", node);
```

Notifica todos los clientes del cambio — coordina multi-user updates ✅.

---

## 60.6 WeatherMapResponse.java (125L) — Mapbox proxy

### 60.6.1 Pattern principal

```java
private static byte[] fetchWeatherImage(String config) {
    try {
        String address = "http://weather.niagaramodules.com/maps" + config + "?host=" + getHostId();
        //                                                       ^^^^^^                ^^^^^^^^^^^^
        //                                                       USER-CTRL             AP-57 site
        URL url = new URL(address);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) { /* cache + return */ }
        return data;
    } catch (Exception var13) {
        LOGGER.log(Level.SEVERE, "weather map response error", var13);
        return null;
    }
}
```

### 60.6.2 AP-57 NUEVO MEDIUM — credential exposure

> **AP-57 NEW MEDIUM** — "Server hostId pass-through to external weather API"
>
> **Site**: `WeatherMapResponse.java:86`
> **Descripción**: `getHostId()` (likely Niagara station ID) is sent as query param to `weather.niagaramodules.com`. Si ese servicio externo se compromete o sus logs leakean, atacante obtiene mapeo `hostId → IP/customer/location` → infrastructure metadata leak.
> **Exploit**: comprometer weather.niagaramodules.com → DB de hostId + accesos → reconnaissance previo a ataque dirigido.
> **Severity**: MEDIUM (info disclosure, attacker-accessible metadata)
> **Fix recomendado** (Implication #129):
> - Reemplazar hostId con opaque session token rotated 24h
> - O eliminar el host param si el servicio externo no lo necesita
> - O proxy via Niagara station con cache local extendido (24h vs 1h)

### 60.6.3 AP-57b sub-finding — config injection

```java
String address = "http://weather.niagaramodules.com/maps" + config + "?host=" + getHostId();
```

Si `config` contiene `?` o `&`, atacante puede inyectar query params:
```
config = "/city/newyork&apikey=evil"
→ address = "http://weather.niagaramodules.com/maps/city/newyork&apikey=evil?host=..."
```

> **AP-57 sub-finding (incluido en MEDIUM)** — config sanitization missing.

### 60.6.4 Caching 1h

```java
BIFile image = directory.getFileSpace().findFile(new FilePath("^reflow/weather.png"));
if (image != null) {
    long diff = System.currentTimeMillis() - image.getLastModified().getMillis();
    if (diff < 3600000L) refreshFromAPI = false;
}
```

**Hallazgo**: cache key NO incluye `config` parameter. Si cliente A pidió "Buenos Aires" + cliente B pide "New York" en <1h, B recibe imagen de A.

> **AP-NEW (variant of AP-57)** — cache key sin config hash. Implication #135.

---

## 60.7 Antipatterns nuevos descubiertos — tabla resumen AP-56..62

| # | Severity | Título | Site | Categoría |
|---|----------|--------|------|-----------|
| AP-56 | LOW | Redundant value assignment in HistoryDataResponse | HistoryDataResponse:254-256 | Code smell |
| AP-57 | MEDIUM | Credential/metadata exposure to external weather API | WeatherMapResponse:86 | Info disclosure |
| AP-58 | MEDIUM | Race condition getConfigController NPE | BReflowSyncService:532 | NPE / robustness |
| AP-59 | MEDIUM | History limit unbounded query DoS | HistoryDataResponse:200 | DoS |
| AP-60 | **HIGH** | Favorites filename traversal via unsanitized username | ReflowOrdTreeFavoritesRead:33 + Write:30 | LFI / path traversal |
| AP-61 | **HIGH** | BackupManager.apply() filename traversal asimetría con create() | BackupManager:175 | Path traversal / config injection |
| AP-62 | LOW | Favorites write async sin durabilidad garantizada | ReflowOrdTreeFavoritesWrite + ConfigIO | Data loss edge case |

**Tally global cross-bloques 50, 51, 53-60**:

| Severity | Count | Ejemplos representativos |
|----------|-------|--------------------------|
| **CRITICAL** | 3 | AP-27 (sistémico ~66 sites), AP-43 (CSWSH), AP-49 (cx async) |
| **HIGH** | 7 | AP-10, AP-21, AP-33, AP-39, AP-42, AP-60, AP-61 |
| **MEDIUM** | 19 | AP-44, AP-47, AP-48, AP-50, AP-51, AP-52, AP-57, AP-58, AP-59, ... |
| **LOW** | 33 | AP-1..AP-9 misc + AP-46 + AP-53..AP-56 + AP-62 + ... |

**TOTAL AP-1..AP-62** = **62 antipatterns identificados** post-Bloque 60.

---

## 60.8 Patterns excelentes (KEEP literal MX60)

1. **RFC 6902 JSON Patch via flipkart/zjsonpatch** — industry standard, Niagara-agnostic, robust delta sync.
2. **Volatile flags para cross-thread visibility** (`volatile activeControlRequestMessage`) — standard Java pattern, simple, correct.
3. **Timer-based auto-grant (30s) + debounce write (5s)** — UX polish, reasonable defaults.
4. **Content-Length validation pre-swap** — truncation prevention en ConfigUpdateResponse.
5. **Atomic temp file swap** — no corrupted config visible.
6. **Try-with-resources cleanup** (HistoryDataResponse) — Java 7+ idiomatic.
7. **`.resolve()` defense-in-depth** en BOrd lookup (HistoryDataResponse) — handles malformed ords.
8. **Cursor lazy iteration** (HistoryDataResponse) — no full materialization, scales to large histories.
9. **Filename sanitization en BackupManager.create()** (`replaceAll("[<>:\"/\\\\|?*#]", "")`) — basic defense, must apply elsewhere.
10. **Optional `createIncrementalBackup()` antes de config write** — multi-user history preservation.
11. **Gzip cache pipeline con Accept-Encoding check** — bandwidth saver, graceful degradation.
12. **Broadcast post-apply (vs pre-apply)** — consistency garantizada.

---

## 60.9 MX60 implications — continuación desde #125

| # | Tag | Descripción |
|---|-----|-------------|
| 126 | IMPROVE | Todos los `BOrd.make()` en Threads MX60 deben capturar `cx` pre-spawn, propagar vía constructor. NO context propagation automática en `new Thread()`. Regla 18. |
| 127 | NEW | Sanitizar username ANTES de concatenación en filename (favorites + cualquier user-derived path). Whitelist `[a-zA-Z0-9_-]{1,64}` + canonicalize. Regla 19. |
| 128 | IMPROVE | Timer threads (revoke, save) carecen cx propagation. Migrar a ExecutorService MX60 con AsyncContext wrapper que captura cx en submit(). |
| 129 | NEW | NEVER pass hostId a untrusted external API. Use opaque session token rotated 24h, o eliminar pass-through (proxy con cache extendido server-side). |
| 130 | IMPROVE | HistoryDataResponse 3x `BOrd.make("history:")` — cache `BHistoryDatabase` singleton en BReflowService, reutilizar. Reduce ord lookup overhead. |
| 131 | IMPROVE | ConfigResponse `?file=` query param permite arbitrary location. Whitelist a `{^reflow/config.json, ^reflow/backups/*.json}`. Canonicalize FilePath. Regla 12 expand. |
| 132 | IMPROVE | BackupManager.apply() sin sanitización vs create() con sanitización. Asimetría inaceptable. Regla 19 obligatoria — sanitizer factory. |
| 133 | NEW | HistoryDataResponse limit unbounded — enforce `MAX_LIMIT = 10000` server-side guardrail. Regla 13 expand. |
| 134 | IMPROVE | ConfigIO.writeFavoritesFile() spawns Thread sin .join() — async sin durabilidad. Si crítico, return Future + barrier en handler. |
| 135 | IMPROVE | WeatherMapResponse caching key sin config hash. Stale cache bug en multi-region. Cache key = MD5(config + hostId). |
| 136 | IMPROVE | BackupManager multi-user race conditions — ReentrantLock per-filename map. Prevents corrupted config restore. |
| 137 | IMPROVE | BReflowSyncService broadcast post-apply NO incluye senderClientId — clientes filtran own changes con DOM diff (waste). Include senderClientId in broadcastEvent. |
| 138 | IMPROVE | AP-58 NPE post-timeout — null check post `getConfigController()`. Defensive coding. |
| 139 | IMPROVE | hasDefaultConfiguration() readTree() vulnerable BIGJSON_DOS — usar streaming JsonParser con depth/size limits. |

**Total MX60 implications post-Bloque 60**: **139 entries** (125 previos + 14 nuevos: 3 NEW + 11 IMPROVE).

---

## 60.10 Reglas template MX60 — 2 reglas nuevas (18-19)

### Regla 18 — Thread spawn debe capturar y propagar cx explícitamente

```
DEBE: capturar `Context cx` ANTES de spawn (en lifecycle del Jetty thread o handler)
DEBE: pasar cx al Runnable via constructor: `new Task(arg1, arg2, cx)`
DEBE: setear ThreadLocal cx al inicio de Runnable.run() — primer statement
NO HACER: BOrd.make().get() en thread spawn sin cx parameter

PATTERN:
class Task implements Runnable {
    private final Context cx;
    public Task(..., Context cx) { this.cx = cx; }
    public void run() {
        Sys.setContext(cx);  // o equivalente Niagara mechanism
        try {
            // BOrd.make(loc).get()  — ahora con cx implícito Y explícito
        } finally {
            Sys.clearContext();
        }
    }
}

EXTIENDE: a Timer threads (`Timer.schedule(TimerTask)`) — TimerTask debe heredar.
```

### Regla 19 — Filename sanitizer factory pattern + canonicalize

```
ANY user-derived filename component (username, route slug, custom name):
  1. Sanitize: FilenameUtil.sanitize(input, ALLOWED_CHARS)
     - DEFAULT ALLOWED_CHARS: [a-zA-Z0-9_-]
     - LENGTH: max 64, min 1
  2. Canonicalize: Path.normalize() + check startsWith(allowedRoot)
  3. Reject: si sanitization changed input length OR canonicalize != original

NO HACER: concatenación directa user-input + path
NO HACER: trust username from BUser.getUsername() ciegamente (Niagara permite custom auth)

APLICAR EN:
  - ReflowOrdTreeFavoritesRead/Write (AP-60)
  - BackupManager.apply (AP-61)
  - ConfigResponse `?file=` query param (AP-33 expand)
  - WeatherMapResponse config param (AP-57 sub-finding)
  - QUALQUIER filesystem access derivada de input cliente
```

**Total reglas template MX60 post-Bloque 60**: **19 reglas** (17 previas + 2 nuevas).

---

## 60.11 Predicciones / hipótesis a verificar después

1. **Bloque 61 frontend audit**: ¿Cliente sanitiza params antes de POST `/config?file=`? ¿O confía en server-side?
2. **Bloque 61**: `ConfigIO.cacheLocation()` implementation — si construye path con `location` sin whitelist → traversal en cache.
3. **Bloque 62 BaseServlet authenticate()**: ¿valida username post-auth para edge case LDAP/OAuth con caracteres especiales?
4. **Niagara N4 future**: ¿Timer/Thread automatically propagan cx en N4.20+? Si sí, Regla 18 simplifica.
5. **Multi-backup race**: empíricamente reproducible — 2 clientes simultáneos `create` + `apply` mismo filename → corrupted restore.
6. **WeatherMap config injection**: empíricamente reproducible — POST `/weather-map?config=/city&apikey=test` → ¿servicio externo loguea apikey query param?
7. **Favorites traversal LDAP exploit**: requiere Reflow + LDAP backend con username sin sanitización Niagara — verificar BUserManager nativo vs custom.

---

## 60.12 Cierre — gaps remanentes Capa 17 + reorganización post-feedback

### Capa 17 estado

| Path | Bloque | Estado |
|------|--------|--------|
| BajaScript canonical | 50-52 | ✅ COMPLETO |
| `yi`/serverSideCall RPC | 53 | ✅ COMPLETO |
| HTTP REST (BaseServlet + 34 Response classes) | 58 | ✅ COMPLETO (4 outliers refinados Bloque 60) |
| WebSocket trinity 3/3 | 59 | ✅ COMPLETO (handlers refinados Bloque 60) |
| Sync engine + handlers async + Response outliers + BackupManager | **60 (THIS)** | ✅ COMPLETO |

### Bloques pendientes — REORGANIZADOS por feedback usuario

| Bloque | Tema | Razón |
|--------|------|-------|
| **61** | **Librerías + APIs + tecnologías Reflow `-rt` y `-ux`** | Feedback usuario: catalogar deps + propósito + ubicación + uso + reemplazos modernos. Esto va ANTES del cierre. |
| **62** | **Alarmas Reflow — bloque dedicado** | Feedback usuario: profundizar Bloque 54 (Alarm domain audit). Cubre backend Java + frontend Vue + integración Niagara N4 Alarm Console + notifications + visualizations. |
| **63** | Frontend Reflow Vue 2.7 audit completo | Store, router, services, components principales — gaps no cubiertos en 50-58 |
| **64** | `-ux` modules Java (workbench views) + módulos `-rt` no auditados | Cierre técnico definitivo |
| **65** | Cierre Reflow + síntesis backlog MX60 final | Consolidación 62+ AP, 19+ reglas, 139+ implications, 21+ libs |
| **66+** | Pivote Analytics module | `/home/cristian/modules/Prototipos/modulos/organized/analytics` + `analytics-lib` |

---

**End of Bloque 60** — sync + responses + backups deep dive completo.

**Siguiente**: Bloque 61 (Librerías + APIs + tecnologías Reflow + reemplazos modernos MX60).
