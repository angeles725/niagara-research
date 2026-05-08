# Bloque 54 — Alarm domain audit cliente↔server + corrigendum HTTP REST endpoints + AP-27 RBAC bypass crítico

**Fecha**: 2026-05-07
**Método**: Cross-reference triple (`$niagara.alarm`/`Na` cliente + `BReflowAlarmCommands.java` Commands + `AlarmData.java` business logic) sobre Reflow producción 1.7.5 bundle + Reflow-Clean-177 src. Triangulación grep multiline-aware con counts empíricos antes de declarar findings.
**Fuentes primarias**:
- `/home/cristian/modules/Prototipos/Reflow/decompiled-rt/app-readable.js` — bundle producción (`Na` namespace líneas 14545-14935+)
- `/home/cristian/modules/Prototipos/Reflow-Clean-177/nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowAlarmCommands.java` — 113 líneas, 9 métodos
- `/home/cristian/modules/Prototipos/Reflow-Clean-177/nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/alarms/AlarmData.java` — 440 líneas, business logic
- Bloque 48 (canAcknowledgeAlarms RBAC pattern, ya cubierto)
- Bloque 50 AP-10 (backups GET destructivos, expandido en este bloque)
- Bloque 53 (template MX60 Java Commands — corregido aquí por AP-27)

**Versión analizada**: Reflow producción 1.7.5 RC1 build 43 (Jul 2024) + Reflow-Clean-177 commit actual.

---

## 54.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Audit completo del **dominio Alarms** de Reflow — la pieza más compleja del módulo dado que cubre 3 capas (cliente Vue, Commands Java thin wrapper, AlarmData business logic) + tiene REST endpoints custom (no solo serverSideCall) + revela un bug crítico de RBAC server-side.

Este bloque tiene **tres misiones**:
1. **Mapear el dominio Alarms** completo client↔server.
2. **Corrigendum al Bloque 53** — corrige dos errores: (a) la lista de mitos sobre HTTP no era completa, Reflow SÍ usa axios via wrapper minimizado `m.a`, y (b) el template MX60 Java Commands no incluía la regla anti-`BOrd.make(...).get(null)`.
3. **Documentar AP-27** — RBAC bypass crítico en `AlarmData.java` que hereda casi todos los métodos de query.

### Qué corrige

| Bloque | Sección | Afirmación previa | Corrección |
|--------|---------|-------------------|------------|
| 53 | 53.2.2 (AP-3 reformulado) | "0 axios.get/post + 0 fetch + 0 XMLHttpRequest = TODA HTTP via BajaScript canónico" | El bundle usa **`m.a`** (axios minimizado por webpack mangler), 28 callsites. Reflow tiene **3 paths HTTP**: (a) BajaScript canonical via Ord.make/cursor, (b) serverSideCall via `yi`, (c) **REST custom** via 20 endpoints HTTP servidos por BaseServlet `/*`. Lista canónica en sección 54.6. |
| 53 | 53.5.16.5 (template MX60 Java) | "TODO ord lookup en Commands DEBE usar el `cx` recibido" — regla declarada pero ejemplos no la verificaban end-to-end | Confirmado **CRÍTICAMENTE** por audit AlarmData.java: si el método delega a una helper class que usa `BOrd.make(...).get(null)` Y la helper corre en thread propio (sin ThreadLocal Context), el RBAC se rompe. **Regla extendida**: la disciplina debe propagar a TODA la cadena de llamadas, no solo al método público. |
| 50 | 50.0 AP-10 | "5 GET destructivos backup operations" | Expandido a **6**: agregar `/nmodsreflow/station/equipment-notes-update` que también es write-via-GET. Sección 54.6 enumera. |

### ¿Qué NO es este bloque?

- **NO** redefine el modelo de alarms Niagara (BAlarmRecord, BAckState, etc) — Bloque 31 cubre.
- **NO** documenta el cliente WebSocket de notificación de alarms (separado del REST de query).
- **NO** cubre cada uno de los métodos de `Na` exhaustivamente — solo los patrones arquitectónicos.

### Pregunta unificadora

> Si MX60 va a tener un sistema de alarms tan completo como Reflow, ¿qué patrones copio, qué bugs evito, y cómo organizo las 3 capas (Vue / Java Commands / Java business logic)?

**Respuesta corta**: copiá el wrapper namespace pattern (`Na`), la separación Commands→BusinessLogic class, el UUID validation con `UUID.fromString` antes de BQL, el `MAX_QUERY_LIMIT` cap. Evitá `BOrd.make(...).get(null)` (AP-27) en TODA la cadena, evitá GET destructivos (AP-10), evitá threads custom sin Context propagation, evitá BString.make(jsonString) si tenés más de un campo.

---

## 54.1 Triangulación metodológica

### Recuentos empíricos

| Marker | Count | Comentario |
|--------|------:|-----------|
| `$niagara.alarm` callsites | 0 | Wrapper Na **no se usa via** `$niagara.alarm` namespace path — todo callsite directo a `Na.method()`. |
| `Na.<method>(` callsites | (varios) | Direct internal usage pattern |
| `yi.spec.ALARM` callsites | 8 | querySources, getUuidsForSources(×2), getAlarmByUuid, getActiveAlarmCounts, getUnackedAlarmCounts, getAlarmsSinceTime, getClasses |
| `Na` definición | línea 14551 | namespace local |
| `m.a.get/post/put/delete` callsites | **28** | **axios mangled** — confirmado uso REAL de HTTP REST en bundle |
| `"axios"` literal | 0 | Webpack tree-shake / mangle borró el nombre symbol — pero el cliente está bundled. Confirmado por inspección de patterns (interceptors, tokens, cancelTokens vistos en sed regions) |
| Endpoints REST únicos `/nmodsreflow/...` | **20** | Lista en sección 54.6 |

### Lección metodológica añadida

> **Counts de symbols con grep son LITERALES — pueden mentir cuando hay minification/mangling.**

El Bloque 53.2.2 hizo `grep -c "axios\.get"` y obtuvo 0, declarando "Reflow no usa axios". **Falso**. Webpack production build minifica los symbols a 1-2 chars (`axios` → `m.a`). El grep literal NO captura el uso. **Solución**: buscar también por **patrones de uso** (paths URL, interceptor patterns, error patterns típicos de axios) Y por imports en `package.json` del proyecto fuente (Bloque 50/51 vio `axios` en deps).

**Regla nueva metodológica**:

> Para audits sobre bundles minified, NUNCA confiar solo en `grep` de symbols. Verificar con patrones de uso (URL strings, error handlers, response shapes) Y cross-reference con `package.json` o import statements del source pre-bundle.

---

## 54.2 Cliente — `Na` namespace ($niagara.alarm) en líneas 14545-15050+

### 54.2.1 Estructura interna

```js
// Variables de módulo (líneas 14545-14550)
var Ma = "true",
    Ra = {                              // alarm sounds cache
        lastFetchedTime: null,
        fileCache: null,
        nextAlarmSound: null
    };

Na = {
    // Estado interno
    sourceClassCache: {},

    // Helpers
    get $baja() { return Vue.prototype.$baja; },
    performanceTime() { /* high-precision timestamp */ },
    perfLog(msg, opts) { /* styled console.log */ },

    // BajaScript-direct path (NO yi)
    ackAlarmsByUuid(uuids) { /* Ord.make("alarm:").ackAlarms({ids}) */ },
    addNotes(uuids, text)  { /* Ord.make("alarm:").addNoteToAlarms({ids,notes}) */ },
    getNotes(uuid)         { /* Ord.make("alarm:").getNotes({uuid}) + parse "## date - author\n\nmsg" */ },

    // serverSideCall path (yi)
    classList(filter)      { /* yi.json(yi.spec.ALARM, "getClasses", filter) */ },
    querySources(time, opts) { /* yi.json(yi.spec.ALARM, "querySources", {timeRange, ...}) */ },
    getUuidForSources(start, end, sources) { /* yi.json(yi.spec.ALARM, "getUuidsForSources", {...}) */ },
    getUuidForSourcesByRange(range, sources) { /* yi.json variant */ },

    // REST custom path (axios m.a)
    query(timeRange, opts) {
        // ⚠️ USA REST, NO yi:
        // m.a.get("/nmodsreflow/station/alarms/query?<built-string>")
    },

    // Hybrid paths (other methods omitted)
    // ...
};
```

### 54.2.2 Hallazgo crítico — Na usa LOS TRES paths HTTP

| Path | Métodos `Na` |
|------|--------------|
| **BajaScript-direct** (`$baja.Ord.make("alarm:").get()`) | `ackAlarmsByUuid`, `addNotes`, `getNotes` |
| **serverSideCall via `yi`** (`yi.json(yi.spec.ALARM, ...)`) | `classList`, `querySources`, `getUuidForSources`, `getUuidForSourcesByRange`, `getAlarmByUuid`, `getActiveAlarmCounts`, `getUnackedAlarmCounts`, `getAlarmsSinceTime` |
| **REST custom via `m.a`** (axios minimizado) | `query` (heavy timeRange-based search) |

**Esto es DECISIÓN ARQUITECTÓNICA CONSCIENTE de Reflow**: usar el path más adecuado según operación:
- Mutaciones (ack, addNotes) → BajaScript directo (canónico).
- Queries simples / counts → serverSideCall (cached service component).
- Queries pesadas con paginación + streaming → REST custom (probable razón: streaming response no soportado por serverSideCall).

**MX60 implication**: el patrón "mix de 3 paths según uso" es válido y trasladable. NO unificar artificialmente — cada path tiene su use case.

### 54.2.3 Pattern de note parsing

`Na.getNotes` parsea response del Niagara con format específico:

```
## 2024-01-15 10:30:00 - admin
First note message body

## 2024-01-15 11:45:00 - operator1
Second note message
```

Parser lógico (líneas ~14490):
```js
var notes = response.notes.split("\n\n");
notes.forEach(note => {
    if (note.length > 1) {
        var stripped = note.replace(/##/g, "");
        var [header, message] = stripped.split("\n", 2);
        var [date, author] = header.split(" - ").map(s => s.trim());
        result.push({ message, date, author });
    }
});
```

**Convención del format**: `## YYYY-MM-DD HH:MM:SS - <author>\n<message>\n\n`. Doble newline separa notas. Triple split (`\n\n` → `## ` → `\n` → ` - `).

**MX60 implication**:
- **IMPROVE**: format frágil (depende de no tener `\n\n` en el message body — si user escribe doble newline, parsing rompe).
- MX60 → JSON array de notes estructurado, cada note con `{date, author, message}` typed slots.

---

## 54.3 Server-side — `BReflowAlarmCommands.java` (113 líneas, 9 métodos)

### 54.3.1 Header (idéntico a BQL/User Commands)

```java
@NiagaraType(agent={
    @AgentOn(types={"nmodsreflow:ReflowService"}, requiredPermissions="r")
})
public class BReflowAlarmCommands extends BComponent implements BIServerSideCallHandler {
    // 9 métodos públicos
}
```

### 54.3.2 Patrones identificados — variaciones del template

**Strict typing** (`query`, `querySources`, `getUuidsForSources`):
```java
public BValue query(BComponent comp, BValue arg, Context cx) throws Exception {
    if (arg.getType().equals(BComponent.TYPE)) {
        BComponent filters = (BComponent) arg;
        QueryFilter queryFilter = QueryFilter.make(filters);
        return BString.make(AlarmData.query(queryFilter).toString());
    } else {
        return null;  // ⚠️ silent failure
    }
}
```

✅ KEEP: strict arg typing pattern (regla 2 del template MX60).
⚠️ IMPROVE: el `else { return null; }` confunde "wrong arg type" con "no permission" en el cliente.

**Magic toString fallback** (`getClasses`, `getAlarmByUuid`, `getAlarmsSinceTime`):
```java
public BValue getClasses(BComponent comp, BValue arg, Context cx) throws Exception {
    List<String> classList = (arg != null)
        ? Arrays.asList(arg.toString().split(","))
        : new ArrayList<>();
    return BString.make(AlarmData.getAlarmClasses(classList).toString());
}
```

⚠️ AP-26 confirmed pattern (defeated typing, ya documentado en Bloque 53.5.16.4).

**Self-introspection** (`canAcknowledgeAlarms` — del Bloque 48):
```java
public BValue canAcknowledgeAlarms(BComponent comp, BValue arg, Context cx) throws Exception {
    BComponent alarmService = Sys.getService(BAlarmService.TYPE);
    return BBoolean.make(cx.getUser().getPermissionsFor(alarmService).hasOperatorWrite());
}
```

✅ KEEP: pattern self-introspection idéntico a `getRoles` del Bloque 53.5.17.

**No-arg methods** (`getActiveAlarmCounts`, `getUnackedAlarmCounts`):
```java
public BValue getActiveAlarmCounts(BComponent comp, BValue arg, Context cx) throws Exception {
    return BString.make(AlarmData.getActiveAlarmCounts().toString());
}
```

⚠️ **`cx` NO se usa**. Delega a `AlarmData.<>()` que tampoco lo recibe. **Esto es donde AP-27 vive.**

### 54.3.3 GAP MASIVO — `cx` NO se propaga a AlarmData

**Hallazgo crítico**: 8 de los 9 métodos NO pasan `cx` a la helper class `AlarmData`. Solo `canAcknowledgeAlarms` usa `cx` correctamente.

```java
// Patrón uniforme problemático:
public BValue someMethod(BComponent comp, BValue arg, Context cx) throws Exception {
    // ... extract args ...
    return BString.make(AlarmData.someStaticMethod(args).toString());
                       // ↑ cx perdido
}
```

**Esto significa**: todo lo que `AlarmData` haga internamente con `BOrd.make(...).get(...)` puede o no respetar el RBAC del usuario invocante, dependiendo de si propaga el ThreadLocal Context o no. Sigue en sección 54.4.

---

## 54.4 Server-side — `AlarmData.java` (440 líneas) y AP-27 crítico

### 54.4.1 AP-27 (NUEVO) — RBAC bypass via `null` Context + thread custom

**Patrón problemático uniforme**:

```java
// Líneas 66, 98, 112, 121, 134, 163, 186, 216, 423 — 9 sites
BITable table = (BITable) BOrd.make("station:|...").get(null);
                                                     //  ↑ NULL CONTEXT
```

**El `null` Context tiene comportamiento documentado en Bloque 38.314**:
- Si hay ThreadLocal Context (set por el caller path), cae a ese.
- Si NO hay ThreadLocal (e.g., thread custom), cae a **system Context** → bypass total de RBAC del usuario.

**Línea 145-160 — El bug catastrófico de `query()`**:

```java
public static JSONObject query(QueryFilter queryFilters) throws Exception {
    QueryTask task = new QueryTask(queryFilters);
    AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
        Thread thread = new Thread((Runnable) task, "BReflowAlarmData.QueryCommand.Task");
        thread.start();      // ← Thread CUSTOM, sin ThreadLocal del caller
        thread.join();
        return null;
    });
    return task.getAlarmData();
}
```

**Análisis**:
1. `AccessController.doPrivileged(...)` — **eleva permisos a system level** (Java SecurityManager). Cualquier ord lookup adentro corre con permisos de system, ignorando el caller's user.
2. **Thread custom** (`new Thread(task)`) — no hereda ThreadLocal Context del caller. El ThreadLocal del usuario invocante NO está disponible adentro.
3. `QueryTask.run()` (línea 423) hace `BOrd.make(...).get(null)` — el null cae a **system Context** porque (a) no hay ThreadLocal y (b) el doPrivileged ya elevó.
4. **Resultado**: TODAS las alarms de la station se devuelven, **independiente del RBAC del user invocante**.

**Severidad**: **HIGH — info disclosure de toda la base de datos de alarms**.

**Blast radius**:
- Cualquier user con `r` permission al ReflowService puede invocar `Na.query()` → `BReflowAlarmCommands.query()` → `AlarmData.query()` y obtener:
  - UUIDs de TODAS las alarms (incluso de Components ACL-restricted)
  - Source paths completos (revela jerarquía organizacional)
  - User names de quién ack qué (línea 274 expone `alarm.getUser()`)
  - Timestamps, priorities, alarm transitions
  - Notes (mensajes potencialmente sensibles escritos por operadores)

### 54.4.2 AP-27 — sites afectados

Tabla por método:

| Método | Línea `BOrd.make().get(null)` | Thread custom? | doPrivileged? | RBAC bypass? |
|--------|------------------------------|----------------|---------------|--------------|
| `getAlarmClasses` | 66 | No | No | **DEPENDE de ThreadLocal** (probable OK si llamado vía serverSideCall) |
| `getAlarmsSinceTime` | 98 | No | No | DEPENDE |
| `getAlarmByUuid` | 112 | No | No | DEPENDE |
| `getActiveAlarmCounts` | 121 | No | No | DEPENDE |
| `getUnackedAlarmCounts` | 134 | No | No | DEPENDE |
| `streamAlarmsCSV` | 163 | No | No | DEPENDE |
| `querySources` | 186 | No | No | DEPENDE |
| `streamSourcesCSV` | 216 | No | No | DEPENDE |
| `query` (vía QueryTask) | **423** | ✅ **SÍ** | ✅ **SÍ** | 🚨 **CONFIRMADO BYPASS** |

**Nota crítica sobre los "DEPENDE"**: aunque ThreadLocal CUBRE el caso normal (invocación via `yi` → serverSideCall → método Java en thread del request), no es un seguro. Cualquier refactor futuro que introduzca thread pooling o async execution rompe el RBAC silenciosamente. **Un solo sitio con `BOrd.make().get(null)` es bug latente**.

### 54.4.3 Mitigación correcta (regla nueva del template MX60 Java)

**Regla 11 (NUEVA del template MX60)**:

> **TODA la cadena de llamadas server-side propaga `Context cx` end-to-end.** Helper classes (como `AlarmData`) reciben `cx` como parámetro explícito. NUNCA `BOrd.make(...).get(null)`. NUNCA threads custom sin propagación de Context. NUNCA `AccessController.doPrivileged` para operaciones que el user invocante no debería poder hacer.

Patrón correcto:

```java
// Commands (capa pública)
public BValue query(BComponent comp, BValue arg, Context cx) throws Exception {
    QueryFilter filter = QueryFilter.make((BComponent) arg);
    return BString.make(AlarmData.query(filter, cx).toString());
                                              // ↑ propagación explícita
}

// Helper class (business logic)
public class AlarmData {
    public static JSONObject query(QueryFilter filter, Context cx) throws Exception {
        // ... NO threads custom
        BITable table = (BITable) BOrd.make("station:|...").get(cx);
                                                              // ↑ usa cx
        // ... iterate y return
    }
}
```

Si MX60 necesita async (e.g., para BQL caro que no puede correr en request thread):
- Usar Niagara's `BJobService` que propaga Context correctamente, NO threads raw.
- O ejecutar el job dentro del request thread con timeout (limit BQL severamente).

### 54.4.4 Otros hallazgos en AlarmData.java

✅ **GOOD pattern — UUID validation antes de BQL** (líneas 107-117):

```java
public static JSONObject getAlarmByUuid(String uuid) {
    // Validate + canonicalize UUID before BQL interpolation to prevent injection.
    String validated = java.util.UUID.fromString(uuid).toString();
    BITable table = (BITable) BOrd.make("station:|alarm:|bql:select * where uuid = '" + validated + "'").get(null);
    // ...
}
```

**Análisis**: `UUID.fromString` throws `IllegalArgumentException` si el input no es UUID válido. `toString()` canonicaliza. Después interpola en BQL — **seguro porque validated es siempre formato UUID, nunca user-controlled BQL fragment**.

**MX60 implication — NEW PATTERN**: para CUALQUIER user input que va a BQL, usar **validación + canonicalización** primero (UUID, Long, Boolean, paths estáticos). Si el input es text libre, **BqlBuilder con escape obligatorio** (sección 53.5.16.5).

✅ **GOOD pattern — BQL builder server-side** (`buildBQLQuery` líneas 340-356):

```java
public static String buildBQLQuery(BAbsTime start, BAbsTime end, Boolean active) {
    StringBuilder str = new StringBuilder();
    str.append("station:|alarm:|bql:select *");
    if (!Boolean.TRUE.equals(active)) {
        str.append(" where timestamp.millis >= ").append(start.getMillis());
        str.append(" and timestamp.millis <= ").append(end.getMillis());
    } else {
        str.append(" where sourceState != 'normal'");
    }
    str.append(" order by timestamp DESC");
    return str.toString();
}
```

**Análisis**: BQL armada server-side, los parámetros son `long` (millis) y `Boolean` (validados). NO concatena user strings directamente. **Patrón seguro**.

✅ **GOOD pattern — `MAX_QUERY_LIMIT` cap server-side** (línea 62 + uso línea 98):

```java
private static final int MAX_QUERY_LIMIT = 1000;
// ...
"select top " + MAX_QUERY_LIMIT + " * where ..."
```

**Análisis**: BQL incluye `select top 1000` para evitar dump completo. Pattern correcto. **Pero**: solo aplica en `getAlarmsSinceTime`. Otros métodos no lo usan.

⚠️ **AP-22 confirmado server-side también** (`QueryTask.run()` línea 425-431):

```java
TableCursor c = table.cursor();
while (c.next()) {
    BAlarmRecord alarm = (BAlarmRecord) c.get();
    if (... filters ... || ++total <= skip || recordCount >= limit ...) continue;
    // ... add to records ...
}
```

Mismo patrón skip+take in-memory que `BReflowBQLCommands.query()`. AP-22 (originalmente client-side `sa.query`) también vive aquí server-side.

⚠️ **Info disclosure menor** — `getAlarmRecord` línea 274:

```java
record.put("user", (Object) alarm.getUser());
```

Expone qué user reconoció/ack la alarm. En BAS multi-tenant esto puede ser sensible (operador A ve nombres de operador B). **MX60 implication**: configurable — opt-in para incluir user en records si el contexto lo permite.

⚠️ **Note count via division-by-3** (línea 279):

```java
noteCount = StringUtils.countOccurrences(notes, '\n') / 3;
```

Cuenta `\n` y divide entre 3 para contar notas (asume cada nota tiene exactamente 3 newlines: header, body, separator). **Frágil** — si el formato cambia, count rompe.

---

## 54.5 Síntesis Alarms vs Template MX60 — qué heredar

### 54.5.1 KEEP literal

| Patrón | Razón |
|--------|-------|
| `Na` namespace cliente con mix de paths (BajaScript / yi / REST) | Decisión arquitectónica válida — cada path tiene su use case |
| Separation Commands (thin) → BusinessLogic class | Single responsibility, testeable |
| `UUID.fromString(uuid).toString()` validation antes de BQL | Defense-in-depth contra injection |
| `MAX_QUERY_LIMIT = 1000` cap server-side | Defense-in-depth contra DoS |
| Self-introspection pattern (`canAcknowledgeAlarms`) | Idéntico a `getRoles`, KEEP |
| `Sys.getService(BAlarmService.TYPE)` | Pattern Niagara estándar |
| BajaScript directo para mutaciones (`Ord.make("alarm:").ackAlarms`) | Path canónico para writes |
| BQL builder server-side con tipos primitivos validados (long millis, Boolean) | Patrón seguro |

### 54.5.2 IMPROVE / FIX

| Patrón Reflow | MX60 mejor |
|---------------|-----------|
| **AP-27**: `BOrd.make(...).get(null)` en helpers | `cx` propagado end-to-end |
| **AP-27**: thread custom + `AccessController.doPrivileged` | `BJobService` o async dentro de request thread |
| **AP-22**: skip+take in-memory pagination | Cursor offset/limit nativos |
| **AP-26**: magic `toString()` fallback (en getClasses, getAlarmByUuid, getAlarmsSinceTime) | Strict arg typing |
| **AP-23**: `return null` silencioso en branch wrong-type | Throw IllegalArgumentException |
| `BString.make(jsonString)` doble overhead | BComponent estructurada |
| Note format `## date - author\n\nmessage\n\n` parseo frágil | JSON array con notes typed |
| Note count `\n` / 3 | Counter slot explícito |
| `alarm.getUser()` expuesto siempre | Configurable per-tenant |

### 54.5.3 NEW (regla 11 del template MX60)

**Regla 11 — Context propagation end-to-end**:

> Helper classes server-side (BusinessLogic) reciben `Context cx` como parámetro obligatorio. Threads custom heredan Context vía `BJobService` o execute en request thread. `AccessController.doPrivileged` SOLO para operaciones que NO requieren RBAC del user (ej: lectura de config interna que el user explícitamente puede invocar).

---

## 54.6 Apéndice — REST endpoints canónicos del bundle Reflow producción

### 54.6.1 Lista exhaustiva (20 endpoints + 1 WebSocket)

```
=== Backups (7) — algunos AP-10 GET destructivos ===
GET    /nmodsreflow/station/backups               (list)
GET    /nmodsreflow/station/backups/create        ⚠️ AP-10 destructive via GET
GET    /nmodsreflow/station/backups/rename        ⚠️ AP-10 destructive via GET
GET    /nmodsreflow/station/backups/apply         ⚠️ AP-10 destructive via GET
GET    /nmodsreflow/station/backups/destroy       ⚠️ AP-10 destructive via GET
GET    /nmodsreflow/station/backups/reset         ⚠️ AP-10 destructive via GET

=== Alarms (2 REST + 8 serverSideCall) ===
GET    /nmodsreflow/station/alarms/query          (Na.query — heavy timeRange-based)
GET    /nmodsreflow/station/alarms/csv            (CSV download — streamAlarmsCSV)

=== History (3) ===
GET    /nmodsreflow/station/histories             (Na.list)
GET    /nmodsreflow/station/history-data          (Na.getData — paginated)
GET    /nmodsreflow/station/history-groups        (Na.getDeviceTree)

=== Config (3) ===
GET    /nmodsreflow/config                        (read JSON config)
GET    /nmodsreflow/config_delta                  (read deltas)
POST   /nmodsreflow/config_update                 (write — proper POST!)

=== Equipment notes (2) — un AP-10 ===
GET    /nmodsreflow/station/equipment-notes
GET    /nmodsreflow/station/equipment-notes-update ⚠️ AP-10 expandido — write via GET

=== Static / metadata (3) ===
GET    /nmodsreflow/point-matrix.json
GET    /nmodsreflow/icon-categories
GET    /nmodsreflow/icon-search
GET    /nmodsreflow/demos

=== WebSocket (1) ===
WS     /nmodsreflow/ws                            (SocketServlet — Bloque 51)
```

### 54.6.2 AP-10 expandido (Bloque 50.0 corrigendum)

**Bloque 50.0 enumeraba 5 backup endpoints destructivos via GET**. Audit completo del bundle revela **6**:

```
1. GET /nmodsreflow/station/backups/create        — crear (file write)
2. GET /nmodsreflow/station/backups/rename        — modify file
3. GET /nmodsreflow/station/backups/apply         — restaurar (write to disk)
4. GET /nmodsreflow/station/backups/destroy       — borrar file
5. GET /nmodsreflow/station/backups/reset         — borrar todos
6. GET /nmodsreflow/station/equipment-notes-update— update notes ⚠️ NUEVO descubierto
```

Todos estos están en **AP-10** clase OWASP-A01 (CSRF amplification — un `<img src>` o prefetch malicioso dispara la operación).

### 54.6.3 BaseServlet `/*` confirmado como handler

**Bloque 51 confirmó "solo 2 servlets reales" SocketServlet `/ws` + BaseServlet `/*`**. Este audit confirma que **BaseServlet sirve TODOS los 20 endpoints REST** (porque su match pattern `/*` cubre cualquier path bajo `/nmodsreflow/`).

**Implicación operacional**: BaseServlet es donde viven las routes — un solo .java handler con probable switch/case sobre `req.getPathInfo()`. Audit pendiente: leer BaseServlet.java para confirmar y mapear path→method.

### 54.6.4 MX60 implications de los REST endpoints

**KEEP**: el modelo "REST custom para operaciones especializadas + serverSideCall para CRUD genérico + BajaScript para mutaciones" es válido.

**IMPROVE / NUEVO MX60**:
1. **Convertir TODOS los GET destructivos a POST/DELETE**: backups + equipment-notes-update. CSRF token obligatorio (Bloque 47/52).
2. **Documentación OpenAPI/Swagger** del REST custom. Reflow no la tiene. MX60 → spec auto-generada.
3. **Versionado** de endpoints: `/v1/...` o header `Api-Version`. Reflow no versiona.
4. **Rate limiting** por user / IP en BaseServlet. Reflow no tiene.
5. **Streaming CSV** (alarms/csv) — KEEP el pattern, IMPROVE con compression (gzip).

---

## 54.7 Antipatterns nuevos catalogados en este bloque

| AP # | Patrón | Severidad | Sección |
|------|--------|-----------|---------|
| **AP-27** | `BOrd.make(...).get(null)` en helper classes server-side + thread custom + `AccessController.doPrivileged` = RBAC bypass via system Context | **HIGH** (info disclosure de toda la station) | 54.4.1 — `AlarmData.query/QueryTask` |
| **AP-28** | `m.a` axios usage hidden by mangling — invisibility a grep estándar | metodológico (no bug) | 54.1 lección methodology |
| **AP-29** | Note format `## date - author\n\nmsg\n\n` parsing en cliente Vue — frágil ante user input con `\n\n` en body | **LOW** | 54.2.3 |
| **AP-30** | `noteCount = countOccurrences('\n') / 3` — heurística frágil que rompe si format cambia | **LOW** | 54.4.4 |
| **AP-31** | `equipment-notes-update` write-via-GET (extiende AP-10 backups) | **MEDIUM** (CSRF) | 54.6.2 |
| **AP-32** | `alarm.getUser()` expuesto siempre en alarm records — info disclosure menor en multi-tenant | **LOW** | 54.4.4 |

---

## 54.8 MX60 implications resumen — tabla incremental

Continúa la tabla 53.8 (57 entries) con nuevos descubrimientos del Alarms domain:

| # | Patrón | Tag | Razón |
|---|--------|-----|-------|
| 58 | `Na` mix de 3 paths HTTP (BajaScript / yi / REST) según operación | **KEEP** | Decisión arquitectónica válida — cada path tiene su use case ideal |
| 59 | Separation Commands (thin wrapper) → BusinessLogic class (AlarmData) | **KEEP** | Single responsibility, testeable |
| 60 | `UUID.fromString(uuid).toString()` validation antes de BQL interpolation | **KEEP** | Defense-in-depth canónica — patrón obligatorio para cualquier user input que va a BQL |
| 61 | `MAX_QUERY_LIMIT` cap server-side por método | **KEEP** | Defense-in-depth contra DoS — generalizar a TODOS los métodos query |
| 62 | BajaScript directo para mutaciones (`Ord.make("alarm:").ackAlarms`) | **KEEP** | Path canónico Niagara |
| 63 | BQL builder server-side con tipos primitivos validados (long millis, Boolean) | **KEEP** | Patrón seguro — extender a queries dinámicas con BqlBuilder builder pattern |
| 64 | `Context cx` propagation end-to-end (Commands → BusinessLogic → ord lookups) | **REGLA 11 OBLIGATORIA** | Sin esto, RBAC se rompe (AP-27 confirmado). Disciplina más crítica del template MX60 |
| 65 | `BOrd.make(...).get(null)` en helpers + thread custom + doPrivileged | **NUNCA** (AP-27) | RBAC bypass crítico. Si MX60 necesita async, usar `BJobService` que propaga Context |
| 66 | Note format frágil `## date - author\n\nmsg\n\n` | **IMPROVE** | JSON typed array |
| 67 | `noteCount = '\n'/3` heurística | **IMPROVE** | Counter slot explícito |
| 68 | `alarm.getUser()` expuesto siempre | **IMPROVE** | Configurable per-tenant |
| 69 | GET destructivos (`equipment-notes-update`, backups) | **NUNCA** (AP-10 / AP-31) | POST/DELETE + CSRF |
| 70 | OpenAPI/Swagger spec ausente | **NEW** | MX60 obligatorio para REST endpoints |
| 71 | Versionado de endpoints (`/v1/...`) | **NEW** | MX60 desde día 1 |
| 72 | Rate limiting por user/IP en BaseServlet | **NEW** | MX60 — DoS defense |
| 73 | Audit logging de write operations (config_update, backups/*, notes-update) | **NEW** | MX60 obligatorio (gap también de Bloque 53.5.17.9) |

### Resumen agregado actualizado

- **31 KEEP** (+5 del Bloque 54): Na mix paths, Commands→BusinessLogic separation, UUID validation, MAX_LIMIT, BQL builder primitivos
- **25 IMPROVE** (+3): note format, noteCount heuristic, alarm.getUser() exposure (+ existing AP-26/22/23 confirmados también server-side)
- **8 NEW** (+4): cx propagation regla 11, OpenAPI spec, versioning, rate limiting, audit logging para writes
- **5 SKIP** (sin cambios)

**73 entries totales** — backlog cubre client + server + Java decorations + RBAC patterns + REST endpoints + audit logging + versioning.

---

## 54.9 Estado de TODOs cross-bloque

| TODO | Estado pre-Bloque 54 | Estado post-Bloque 54 |
|------|----------------------|----------------------|
| Bloque 50.0 AP-10 enumerar destructive GETs | 5 documentados | ✅ **EXPANDIDO** a 6 (54.6.2 — equipment-notes-update agregado) |
| Bloque 53.2.2 "TODA HTTP via BajaScript" | declarado | ⚠️ **CORREGIDO** — 3 paths (BajaScript / yi / REST custom 20 endpoints) |
| Bloque 53.5.16 "cx propagation discipline" | declarado | ✅ **CRÍTICAMENTE VERIFICADO** — debe extenderse a TODA cadena helper, no solo Commands. Regla 11. |
| Bloque 48 canAcknowledgeAlarms RBAC | confirmado | ✅ confirmado uniformemente con audit completo de AlarmCommands |
| TODO 48-3 bajaScript browser API enumerate | pendiente | (no afectado) |
| TODO 48-4 BCategoryService.resolveIndices | pendiente | (no afectado) |

---

## 54.10 Próximos hilos — sub-libs `$niagara` restantes

Tabla actualizada de prioridades con context post-alarm:

| Sub-lib | Java backend | Insights anticipados | Prioridad |
|---------|-------------|---------------------|-----------|
| `$niagara.history` (Sa) | `BReflowHistoryCommands` + helper classes | Probable mismo patrón AP-27 (helpers con `null` Context). Múltiples REST endpoints (`/history-data`, `/histories`, `/history-groups`). | **HIGH — Bloque 55** |
| `$niagara.points` (Ti) | (probable BajaScript directo, no Commands) | Hot path. Probable sin Java backend dedicado (delega todo a BajaScript). | **HIGH — Bloque 56** |
| `$niagara.schedule` (la) | (sin Commands Java visto, solo BQL via sa) | Validar si tiene Commands o solo BQL. | **MEDIUM — Bloque 57 (combinado)** |
| `$niagara.nav` (Ci) | `BReflowNavCommands` (1 método visto: getNavChildren) | Validar Commands + posible AP-27. | **MEDIUM — Bloque 57 (combinado)** |
| `BReflowFileCommands` | (sin sub-namespace cliente directo) | listFiles + others. Validar AP-27. | **MEDIUM — Bloque 57 (combinado)** |
| `$niagara.backups` (Se) | (probable wrapper sobre REST endpoints, ya cubierto en AP-10) | Cliente del REST destructive endpoints. | **LOW — Bloque 58 closeout** |
| `$niagara.matrix` (vi) | Reflow-specific | Evaluar relevancia para MX60. | **LOW — Bloque 58 closeout** |
| `$niagara.util.timerange/.facets` | — | Helpers de timestamp + facets parsing. | **LOW — Bloque 58 closeout** |
| `BReflowCSVCommands` | — | CSV streaming export. Posible duplicado de `streamAlarmsCSV`. | **LOW — Bloque 58 closeout** |
| `BaseServlet.java` (apart) | — | El handler que sirve los 20 REST endpoints. **CRÍTICO para MX60** — patrón router. | **HIGH PRIORITY ESPECIAL — incluir en Bloque 57 o 58** |

**Decisión metodológica para próximos bloques**: dado que el alarm domain reveló AP-27 (RBAC bypass via helpers con `null` cx), los próximos audits Java van a **buscar específicamente este patrón en cada helper class** (HistoryData, NavData, etc). Si se confirma uniformemente, es **deuda técnica sistémica de Reflow** que MX60 debe abordar como diseño obligatorio.
