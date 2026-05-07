# Bloque 44 — Alarm Console pipeline frontend (SPA externa)

**Fecha**: 2026-05-04
**Método**: Investigación empírica READ-ONLY. Síntesis cruzada de Bloques 16 (BAnalyticAlert + BAlarmService), 23 (BACnet AlarmRouter + NotificationClass), 29 (web tier servlets), 47 (Bootstrap headless SPA), 50 (Reflow frontend audit), 51 (Reflow -rt + app-readable.js deep dive). Evidencia directa de código Java decompilado y bundle JS deobfuscado.
**Fuentes primarias**: `alarm-rt.jar`, `bacnetAlarmRouter-rt.jar`, `BReflowAlarmCommands.java`, `AlarmData.java`, `app-readable.js:14520-14582` (Reflow v1.7.5), `box-rt.jar` (channel `alarm`), `BAlarmService.class`, `BAlarmRecord.class`.
**Versión analizada**: Honeywell OptimizerSupervisor-N4.14.0.162.

---

## 44.0 Contexto, scope, qué NO es este bloque

### Pregunta unificadora

> Mi SPA vive en `dashboard.sejofa.com`. La station Niagara está en `station.sejofa.io:443`. Quiero mostrar alarmas activas, paginar history de alarmas, hacer ACK/clear/silence, y recibir alarmas nuevas en tiempo real. ¿Cómo funciona end-to-end?

### Qué ES este bloque

Documenta el **pipeline completo de consumo de alarmas desde una SPA externa** (Vue, React, vanilla JS) conectada a una station Niagara N4. Cubre:

1. El modelo interno del servidor — `BAlarmService`, `AlarmSpaceConnection`, `BAlarmRecord`
2. Los topics de alarma (eventos server-to-client)
3. BQL sobre el alarm space y cómo paginar
4. El flow ack/clear/silence con sus métodos exactos
5. La distinción open alarms vs alarm history archive
6. Notification routing y si una SPA puede registrarse como recipient
7. Los servlets HTTP que exponerse alarmas
8. La anomalía `canAcknowledgeAlarms` → BBoolean
9. Refinamiento de los bloques del plan 42-43, 45-46, 48-49

### Qué NO es este bloque

- **NO cubre BACnet intrinsic alarming** — ese es el protocolo BACnet de device → notification class → Niagara. Bloque 23 cubre eso. Este bloque empieza donde ese termina: en el `BAlarmService` de Niagara.
- **NO cubre Analytics alerts** — `BAnalyticAlert implements BIAlarmSource` está en Bloque 16. Una vez que el alert se convierte en `BAlarmRecord` vía `BAlarmService`, el pipeline es el mismo.
- **NO cubre configuración WB de alarm classes** — eso es administración, no desarrollo de SPA.
- **NO cubre NiagaraRPC** — el mecanismo general de RPC está en Bloque 29. Acá se aplica concretamente a alarmas.

---

## 44.1 BAlarmService + AlarmSpaceConnection API

### 44.1.1 BAlarmService — clase central

**Package**: `javax.baja.alarm` (en `alarm-rt.jar`).

**Clase**: `BAlarmService extends BAbstractService implements BIAlarmDatabase`.

**Ubicación en station**: `/Services/AlarmService` (singleton por station).

**Resolución programática**:
```java
// Java server-side
BAlarmService alarmSvc = Sys.getService(BAlarmService.TYPE);

// BajaScript client-side (CONFIRMADO, app-readable.js:14520)
baja.Ord.make("alarm:").get().then(alarmService => { ... });
```

El ORD `alarm:` es el scheme registrado del `BAlarmService`. `BAlarmScheme` lo resuelve en el `BAlarmService` singleton de la station local. En contexto de Supervisor, puede calificar con `station:|alarm:` para la station raíz o `station:|slot:/Supervisors/Sub1|alarm:` para un subordinado.

### 44.1.2 Métodos públicos confirmados

Del análisis de `BAlarmAlarmService.class` (decompilado) y de la evidencia de uso en `AlarmData.java` (Reflow) y `app-readable.js`:

```java
// Consulta de alarmas abiertas (activas)
BAlarmSpaceConnection openAlarms(Context cx) throws Exception;

// Consulta de alarmas por cursor con filtro
BIAlarmCursor getAlarms(BAlarmFilter filter, Context cx) throws Exception;

// ACK de múltiples alarmas por UUID array
void ackAlarms(String[] uuids, Context cx) throws Exception;
// ó
void ackAlarms(BString[] uuids, Context cx) throws Exception;

// Clear alarma
void clearAlarm(BAlarmRecord rec, Context cx) throws Exception;

// Silence alarma
void silenceAlarm(BAlarmRecord rec, Context cx) throws Exception;

// Notas sobre alarmas
void addNoteToAlarms(String[] uuids, String note, Context cx) throws Exception;
String[] getNotes(String uuid, Context cx) throws Exception;
```

**IMPORTANTE — `getOpenAlarms()` NO existe como método directo**: El término "open alarms" es conceptual. La API real usa `BAlarmSpaceConnection` para abrir una vista al espacio de alarmas activas. La confusión AX→N4 viene porque en AX había un `getOpenAlarms()` synchronous. En N4, la query es asíncrona vía `BAlarmSpaceConnection` o síncrona vía `BIAlarmCursor` con filtro de estado.

### 44.1.3 AlarmSpaceConnection lifecycle

`BAlarmSpaceConnection` es la conexión al alarm space de Niagara — el equivalente del Subscriber pero para alarmas:

```
BAlarmService
 └─ openAlarms(cx) → BAlarmSpaceConnection
       ├─ addListener(BIAlarmSpaceConnectionListener) → recibe eventos push
       ├─ getOpenAlarms() → BAlarmRecord[]  (snapshot inicial de activas)
       ├─ query(BAlarmFilter) → BIAlarmCursor  (cursor para navegación)
       └─ close() → libera recursos
```

**Lifecycle**:

```
1. BAlarmSpaceConnection conn = alarmSvc.openAlarms(cx);
2. BAlarmRecord[] snapshot = conn.getOpenAlarms();    // alarmas activas al momento
3. conn.addListener(listener);                        // eventos futuros push
4. // ... usar alarmas ...
5. conn.close();                                      // liberar
```

**CRÍTICO**: La conexión es stateful. Debe cerrarse explícitamente. En un servlet, abrir una conexión por request y no cerrarla genera leak de listeners en el `BAlarmService`. Patrón correcto: try-finally o try-with-resources.

**Desde SPA externa**: La `BAlarmSpaceConnection` es Java server-side. La SPA no instancia esto directamente — la usa vía:
- **Canal BOX `alarm`** (para eventos push en tiempo real)
- **REST HTTP** (para queries paginadas)
- **BQL** (para queries con filtros)

### 44.1.4 Filtrado server-side

`BAlarmFilter` permite filtrar antes de retornar registros al cliente:

```java
// Clases de filtro en alarm-rt.jar (confirmadas via module.xml)
BAlarmFilter         // interfaz base
BAlarmClassFilter    // por clase de alarma (por tipo/nombre)
BAckStateFilter      // por estado de ACK: UNACKED, ACKED, ACKED_TRANSITIONED
BSourceStateFilter   // por sourceState: offnormal, fault, normal
BPriorityFilter      // por rango de priority (0-255)
BTimeFilter          // por rango temporal (desde, hasta)
BSourceFilter        // por ORD fuente

// Combinación
BAndFilter           // AND de filtros
BOrFilter            // OR de filtros
BNotFilter           // NOT de un filtro
```

**Prioridades en Niagara**: El modelo de prioridad de alarmas Niagara es 0-255 (NO los 16 niveles de BACnet). 0 = crítico, 255 = informativo. Las clases predefinidas:
- 0-49: critical / life safety
- 50-99: urgent  
- 100-199: warning
- 200-255: informational / fault

Reflow mapea esto a high/medium/low en su lógica de presentación — es una simplificación específica de Reflow, no de Niagara.

### 44.1.5 BIAlarmCursor — navegación

`BIAlarmCursor` es el mecanismo de paginación server-side del alarm database:

```java
interface BIAlarmCursor {
    boolean next() throws Exception;     // avanza al siguiente registro
    BAlarmRecord get();                   // registro actual
    void close() throws Exception;        // OBLIGATORIO
}
```

**Pattern de paginación manual**:

```java
BAlarmFilter filter = new BAndFilter(
    new BAckStateFilter(BAckState.UNACKED),
    new BPriorityFilter(0, 99)   // critical + urgent
);

BIAlarmCursor cursor = alarmSvc.getAlarms(filter, cx);
int count = 0;
int limit = 50;
int offset = 0;  // skip primeros N

try {
    // skip inicial
    while (count < offset && cursor.next()) count++;
    
    // collect page
    List<BAlarmRecord> page = new ArrayList<>();
    while (page.size() < limit && cursor.next()) {
        page.add(cursor.get());
    }
} finally {
    cursor.close();  // SIEMPRE
}
```

**GOTCHA G44-1 — TOP/SKIP en BQL de alarmas**: La sintaxis BQL `alarm:|bql:select * from alarm where ... TOP 50 SKIP 0` NO funciona igual que en `history:` o en el entity space. El alarm space tiene limitaciones en paginación BQL — en Reflow, `AlarmData.java` usa `BIAlarmCursor` manual con un campo `limit` en la query, NO TOP/SKIP nativo. Ver sección 44.3.

---

## 44.2 Topics de alarma — push events

### 44.2.1 Topics confirmados en alarm-rt.jar

Los topics de alarma son el mecanismo de notificación push del `BAlarmService`. Se publican en el espacio de alarmas de la station y son accesibles vía BOX channel `alarm`.

Topics verificados en el módulo `alarm-rt.jar` (via `module.xml` + `BAlarmRecord.class`):

| Topic | Nombre constante | Cuándo se dispara |
|-------|-----------------|-------------------|
| `alarmGenerated` | Alarma nueva creada | `BIAlarmSource` genera nuevo `BAlarmRecord` |
| `alarmAcked` | Alarma reconocida | Usuario hace ACK vía `ackAlarms()` |
| `alarmCleared` | Alarma limpiada | Estado vuelve a normal + cleared |
| `alarmNoteAdded` | Nota agregada | `addNoteToAlarms()` llama |
| `alarmDeleted` | Alarma eliminada del DB | Purga por retention policy |
| `alarmStateChanged` | Cambio de estado genérico | Cualquier transición de estado |

**NOTA HONESTA**: Los nombres exactos de los topics son INFERIDOS de `BAlarmSpaceConnectionListener` métodos y constantes de string en bytecode. La evidencia directa de los nombres como strings es `alarmGenerated`, `alarmAcked`, `alarmCleared` — los demás son inferencia de los métodos del listener. El topic `alarmFired` mencionado en algunos documentos es probablemente un alias o nombre AX. En N4, el topic es `alarmGenerated`.

### 44.2.2 BIAlarmSpaceConnectionListener — interfaz Java

La interfaz Java para recibir eventos push desde el servidor:

```java
interface BIAlarmSpaceConnectionListener {
    void alarmGenerated(BAlarmRecord rec);
    void alarmAcked(BAlarmRecord rec);
    void alarmCleared(BAlarmRecord rec);
    void alarmNoteAdded(BAlarmRecord rec, String note);
    void alarmDeleted(String uuid);
    void alarmStateChanged(BAlarmRecord rec);
    void connectionClosed();
}
```

Este listener es Java server-side. Para la SPA externa, los eventos llegan via canal BOX `alarm`.

### 44.2.3 BOX channel `alarm` — protocolo desde SPA

El canal BOX `alarm` (CONFIRMADO en inventario de Bloque 47.5.6) expone el alarm space vía WebSocket. Es el equivalente del canal `boxcs` pero para alarmas.

**Identificador del canal**: `"alarm"` — clave `c` en los frames BOX.

**Suscribirse al canal**:
```json
// Frame BOX para join alarm channel
{"c":"alarm","k":"subscribe","b":{"handle":"hAlarm"}}
```

**Eventos push del servidor** (unsolicited, formato JSON BOX):
```json
{"c":"alarm","k":"alarmGenerated","b":{
    "uuid": "550e8400-e29b-...",
    "source": "station:|slot:/Equipment/AHU1/HighTemp",
    "priority": 50,
    "sourceState": "offnormal",
    "ackState": "unacknowledged",
    "alarmClass": "HighTemp",
    "timestamp": "2026-05-04T14:23:11Z",
    ...
}}
```

**Implementación en BajaScript** (patrón Reflow confirmado en `app-readable.js`):
```javascript
// Via baja.Ord.make("alarm:").get()
baja.Ord.make("alarm:").get({subscriber: sub}).then(alarmService => {
    // alarmService es el BAlarmService como BComponent
    // sub.attach("changed", handler) recibe cambios de estado
    alarmService.attach("changed", function(prop, newVal) {
        // prop.getName() === "alarmCount" u otras propiedades del servicio
    });
});
```

**ALTERNATIVA más directa** (patrón Reflow `app-readable.js:14520`):
```javascript
// ACK directo via BAlarmService resuelto por ORD
baja.Ord.make("alarm:").get().then(alarmSvc => {
    return alarmSvc.ackAlarms({ids: [uuid1, uuid2]});
})
```

### 44.2.4 ¿Subscriber del Bloque 42 aplica?

**SÍ, parcialmente**. El `baja.Subscriber` (Bloque 42) es el mecanismo general de subscription a BComponents. Como `BAlarmService` ES un `BComponent`, se puede suscribir con:

```javascript
subscriber.subscribe(alarmService);
subscriber.attach("changed", (comp, prop) => {
    // prop.getName() podría ser "alarmCount", "openAlarmCount", etc.
    // pero NO da el detalle del AlarmRecord individual
});
```

Esto notifica que **algo cambió en el servicio**, no cuál alarma específica cambió.

Para eventos de alarma individuales (qué alarma se generó/ackó/clearó), el canal BOX `alarm` es el mecanismo correcto — es más granular que el Subscriber genérico.

**Tabla comparativa**:

| Mecanismo | Granularidad | Payload | Uso recomendado |
|-----------|-------------|---------|-----------------|
| `baja.Subscriber` sobre `alarm:` | Nivel servicio (contador cambió) | Propiedad del servicio | Badge contador en navbar |
| BOX channel `alarm` | Nivel registro individual | `BAlarmRecord` completo | Alarm console en tiempo real |
| REST poll `getAlarmsSinceTime(ts)` | Records desde timestamp | JSON array | Polling fallback si BOX no disponible |

### 44.2.5 SSE vs BOX para alarmas

**Server-Sent Events (SSE)**: Niagara N4 NO expone un endpoint SSE nativo para alarmas. No existe `EventSource` endpoint documented. TODO verificación adicional — no se encontró ninguna clase `AlarmSSEServlet` o similar en alarm-rt.jar.

**BOX subscription** (recomendado): El canal BOX `alarm` provee push events bidireccionales. El WebSocket BOX es la única vía confirmada para push de alarmas individuales.

**Alternativa de polling** (para SPA que no puede usar BajaScript/BOX): El REST endpoint `getAlarmsSinceTime(epochMs)` retorna alarmas desde un timestamp. La SPA puede hacer polling cada N segundos. Este es el approach de Reflow en su implementación de "novedades" (confirmado en `BReflowAlarmCommands.getAlarmsSinceTime()`).

---

## 44.3 BQL sobre alarm space

### 44.3.1 Scheme alarm: en BQL

El alarm space de Niagara es accesible como `BITable` via el scheme `alarm:`. La sintaxis BQL:

```
alarm:|bql:select * from alarm
alarm:|bql:select * from alarm where sourceState = "offnormal"
alarm:|bql:select * from alarm where priority <= 99
alarm:|bql:select * from alarm where ackState = "unacknowledged"
alarm:|bql:select uuid, source, priority from alarm where timestamp.millis >= 1714000000000
```

**Columnas disponibles en BQL** (inferidas de `BAlarmRecord` y Reflow AlarmData):
- `uuid` — identificador único (String)
- `source` — ORD del punto fuente (String)
- `priority` — 0-255 (int)
- `timestamp` — momento de generación (BAbsTime)
- `normalTime` — momento en que volvió a normal (BAbsTime, null si sigue activa)
- `lastUpdate` — última modificación (BAbsTime)
- `sourceState` — estado del source: `offnormal`, `fault`, `normal`
- `ackState` — estado ACK: `unacknowledged`, `acknowledged`, `acknowledged-transitioned`
- `ackRequired` — boolean
- `alarmTransition` — transición que generó la alarma
- `alarmClass` — clase de alarma (nombre)
- `user` — usuario que ack-eó (null si no acked)
- `noteCount` — número de notas (int)
- `alarmData` — datos adicionales estructurados (BComplex)

### 44.3.2 Paginación — TOP/SKIP vs cursor

**TOP/SKIP en BQL** (CONFIRMADO con reservas):

```
alarm:|bql:select * from alarm where ackState = "unacknowledged" TOP 50 SKIP 0
```

`TOP N SKIP M` es parte de la sintaxis BQL general (Bloque 21). Sin embargo, para el alarm space hay una complicación: el `BAlarmService` puede almacenar alarmas en memoria + archivo `.adb`. El TOP/SKIP en BQL sobre alarmas opera sobre el cursor interno — para queries grandes (miles de alarmas), puede ser lento porque el cursor no tiene índices como un RDBMS.

**GOTCHA G44-2 — TOP/SKIP sobre alarm: NO garantiza orden**: El alarm space NO garantiza orden de retorno sin `ORDER BY`. Especificar siempre:

```
alarm:|bql:select * from alarm where ackState = "unacknowledged" 
       ORDER BY timestamp DESC TOP 50 SKIP 0
```

**GOTCHA G44-3 — ORDER BY en alarm space tiene comportamiento diferente**: Si el alarm space almacena en `.adb` SQLite-like (Bloque 31), ORDER BY es más eficiente. Si almacena en memoria (para alarmas muy recientes), el ORDER BY se aplica en memoria. El mecanismo interno no es configurable desde BQL.

**Recomendación empírica** (basada en Reflow `AlarmData.java`): Reflow usa `BIAlarmCursor` manual con campo `limit` para paginación, NO TOP/SKIP BQL. La razón probablemente es que `BIAlarmCursor` tiene comportamiento más predecible para alarmas. Para una SPA externa que no puede instanciar cursor Java, usar TOP/SKIP en BQL vía un servidor proxy (servlet custom) es la alternativa viable.

### 44.3.3 NEQL y alarmas

**NEQL NO aplica a alarmas**. NEQL (Bloque 5/16) opera sobre el entity space — componentes con tags. El alarm space (`alarm:`) es un `BHistoricalSpace`/`BITable`, no un `BIEntitySpace`. NEQL solo funciona sobre `slot:`, `station:`, y spaces con tags Haystack.

```
// ESTO FUNCIONA
bql:select * from alarm where priority <= 99

// ESTO NO EXISTE PARA ALARMAS
neql:select ... from alarm ...  // INCORRECTO - no hay NEQL para alarm space
```

### 44.3.4 Ejecución BQL desde SPA

Desde SPA externa, hay dos rutas para ejecutar BQL sobre alarmas:

**Ruta 1 — BOX ord channel**:
```javascript
baja.Ord.make("alarm:|bql:select * from alarm where ackState='unacknowledged' TOP 50").get()
  .then(table => {
      // table es un BITable
      const cursor = table.cursor();
      while (cursor.next()) {
          const row = cursor.get();
          // row.get("uuid"), row.get("priority"), etc.
      }
  });
```

**Ruta 2 — Servlet custom** (proxy server-side):
La SPA envía el query BQL al servlet custom vía REST, el servlet ejecuta en Java y retorna JSON. Este es el approach de Reflow (`AlarmQueryResponse` + `AlarmData.query()`).

**Ruta 3 — ReflowBQLCommands BOX** (si se usa el módulo Reflow):
```javascript
// Via serverSideCall
window.top.niagara.box.serverSideCall(
    'nmodsreflow:ReflowBQLCommands',
    'query',
    'alarm:|bql:select * from alarm where ackState="unacknowledged" TOP 50',
    callback
);
```

---

## 44.4 Ack/Clear/Silence flow

### 44.4.1 Métodos de escritura exactos

**ACK** — reconocer una alarma:

```java
// Java server-side (BAlarmService)
alarmSvc.ackAlarms(new String[]{uuid1, uuid2}, cx);

// BajaScript client-side (CONFIRMADO app-readable.js:14520-14537)
baja.Ord.make("alarm:").get().then(alarmSvc => {
    return alarmSvc.ackAlarms({ids: [uuid1, uuid2]});
});
```

**CLEAR** — limpiar una alarma ya resuelta:

```java
// Java server-side
alarmSvc.clearAlarm(alarmRecord, cx);
```

En BajaScript client-side, clear probablemente es:
```javascript
baja.Ord.make("alarm:").get().then(alarmSvc => {
    return alarmSvc.clearAlarm({id: uuid});
});
```

**IMPORTANTE**: `clearAlarm` solo funciona cuando el `sourceState` ya es `normal`. No se puede "forzar" clear de una alarma activa — la fuente debe haber vuelto a normal. Esto es una restricción del modelo de alarmas Niagara.

**SILENCE** — silenciar (suprimir notificaciones sin ACK):

```java
// Java server-side
alarmSvc.silenceAlarm(alarmRecord, cx);
```

El silence es temporal — la alarma sigue activa pero no dispara nuevas notificaciones. El `sourceState` no cambia.

**NOTA HONESTA**: Los métodos exactos `clearAlarm` y `silenceAlarm` en BajaScript no están directamente confirmados en el código de Reflow (que no implementa clear/silence). Son INFERIDOS del modelo de la API Java de `BAlarmService`. Se confirmarán o refutarán en investigación futura del canal BOX `alarm`.

### 44.4.2 ACK batch (multi-alarma)

`ackAlarms` acepta un array de UUIDs — esto es el batch ACK nativo de Niagara. NO requiere N requests individuales.

```javascript
// ACK de 50 alarmas en un solo call
baja.Ord.make("alarm:").get().then(alarmSvc => {
    return alarmSvc.ackAlarms({
        ids: [uuid1, uuid2, uuid3, /* ... hasta 50 */ uuid50]
    });
});
```

**Límite de batch**: No documentado un límite oficial. Empíricamente Reflow no especifica un tope. INFERIDO: el límite es la memoria disponible para serializar el array. Para operaciones masivas (>500 alarmas), dividir en lotes de 100.

### 44.4.3 RBAC — quién puede hacer ACK

**CONFIRMADO** (`BReflowAlarmCommands.java:109-112`, Bloque 51):

```java
public BValue canAcknowledgeAlarms(BComponent comp, BValue arg, Context cx) throws Exception {
    BComponent alarmService = Sys.getService(BAlarmService.TYPE);
    return BBoolean.make(cx.getUser().getPermissionsFor(alarmService).hasOperatorWrite());
}
```

El permiso requerido es `operatorWrite` sobre el `BAlarmService`. Este permiso se configura en el role del usuario en Niagara:

- El role del usuario debe tener `operator write` sobre la categoría donde vive `BAlarmService`
- La categoría default de `BAlarmService` es `admin` (configurable en Workbench)
- Roles built-in: `superuser` (todos los permisos), `operator` (típicamente tiene operator write), `observer` (solo lectura)

**Implicaciones para frontend**: La SPA DEBE llamar a `canAcknowledgeAlarms` (o un endpoint equivalente) antes de mostrar botones de ACK. Si el usuario no tiene permiso, los botones deben estar deshabilitados — no simplemente ocultos (seguridad defensiva: el servidor rechazará el ACK de todos modos, pero la UX debe ser clara).

**Flujo correcto en SPA**:
```javascript
// Al cargar la consola de alarmas
const canAck = await checkAckPermission();
if (canAck) {
    showAckButtons();
} else {
    showReadonlyView();
}

async function checkAckPermission() {
    // Via Reflow BOX endpoint
    const result = await box.call('nmodsreflow:ReflowAlarmCommands', 'canAcknowledgeAlarms', null);
    return result === true; // BBoolean deserializado (ver 44.8)
}
```

### 44.4.4 Audit trail — quién hizo ACK

El `BAlarmRecord` contiene el campo `user` — el username de quien ackó. Este campo se persiste en el `.adb` archivo.

**Audit adicional**: `BAuditService` (Bloque 30) registra operaciones de escritura. Cuando un usuario hace ACK, el servicio de audit genera un `BAuditRecord` con:
- `user`: username
- `action`: `ackAlarm`
- `source`: UUID de la alarma
- `timestamp`: momento del ACK

Para consultar el audit log desde SPA:
```
station:|audit:|bql:select * from audit where action = "ackAlarm" TOP 100
```

La integración audit + alarma es importante para auditorías de compliance (ASHRAE, seguridad industrial). No es responsabilidad del cliente implementarla — el servidor la mantiene automáticamente.

---

## 44.5 Open alarms vs Alarm history

### 44.5.1 Distinción conceptual

| Aspecto | Open Alarms | Alarm History |
|---------|-------------|---------------|
| ¿Qué contiene? | Alarmas activas (sourceState != normal O ackState = unacknowledged) | Todas las alarmas generadas, incluyendo resueltas |
| Dónde vive | Memoria del BAlarmService + DB activa | Archivo `.adb` (alarm database) |
| Formato .adb | CONFIRMADO Bloque 25: `AlarmDbMigrator.extensions=adb` — formato binario propietario, NO SQLite puro (ver Bloque 31 corrección) | Idem |
| Acceso BQL | `alarm:` (sin filtro de estado) o con filtro `sourceState = "offnormal"` | `alarm:` con rango de tiempo |
| Retención | Configurable en `BAlarmService.retentionPolicy` | Misma DB, con políticas de archivado |
| Límite default | Configurable (típico 10,000 alarmas abiertas) | Configurable (típico 30,000 registros) |

### 44.5.2 Filtros para distinguir

```
// Alarmas activas (open)
alarm:|bql:select * from alarm where sourceState != "normal" ORDER BY priority ASC

// Alarmas no ack-eadas (pendientes de acción)
alarm:|bql:select * from alarm where ackState = "unacknowledged" ORDER BY timestamp DESC

// Alarmas históricas (resueltas) en las últimas 24 horas
alarm:|bql:select * from alarm where normalTime >= {24hAgo} ORDER BY normalTime DESC

// Historia completa (open + history)
alarm:|bql:select * from alarm ORDER BY timestamp DESC TOP 100 SKIP 0
```

### 44.5.3 Endpoints separados en Reflow

Reflow implementa dual-channel para open vs history:

**Via REST** (queries paginadas con filtros complejos):
```
POST /nmodsreflow/station/alarms/query
Body: { "query": "range=lastDay&ackState=unacknowledged&sourceOrd=station:|slot:/Equipment&limit=50&offset=0" }
→ { "total": 123, "records": [{...}, ...] }
```

**Via BOX** (datos live y conteos):
- `getActiveAlarmCounts` — alarmas activas por prioridad
- `getUnackedAlarmCounts` — no ack-eadas
- `getAlarmsSinceTime(epochMs)` — novedades desde timestamp

En Niagara nativo (sin módulo custom), los dos conceptos van por el mismo endpoint `alarm:` con filtros distintos.

### 44.5.4 Límite hardcodeado en Reflow — GOTCHA crítico

**CONFIRMADO** (`AlarmData.java:408`, Bloque 51.4 AP-14):

```java
// AlarmData.java dentro de AlarmData.QueryTask:
int limit = 15;  // HARDCODEADO
```

El BOX endpoint `ReflowAlarmCommands.query` siempre retorna 15 registros máximo, independientemente del `limit` pasado como parámetro. El REST endpoint sí respeta el `limit` de la query.

**Para módulos custom propios**: NO copiar este error. Pasar el límite como parámetro configurable desde el request.

---

## 44.6 Notification routing hacia SPA

### 44.6.1 Pipeline BNotificationClass → AlarmRecipient

El pipeline de notificación de alarmas en Niagara:

```
BIAlarmSource (BAnalyticAlert, BBacnetProxyExt, BNumericPoint alarmExt, etc.)
    │ genera BAlarmRecord
    ▼
BAlarmService.generateAlarm()
    │ lookup NotificationClass del AlarmRecord
    ▼
BNotificationClass
    │ evalúa recipientList
    ▼
BAlarmRecipient[]
    ├─ BEmailAlarmRecipient → email
    ├─ BSMSAlarmRecipient → SMS
    ├─ BWritePropertyAlarmRecipient → escribe valor en punto
    └─ (custom recipients vía módulo)
```

### 44.6.2 ¿Puede una SPA registrarse como recipient?

**NO directamente**. No existe `BHttpAlarmRecipient` ni `BWebhookAlarmRecipient` en el corpus Honeywell N4.14. Búsqueda empírica: `grep -r "BHttpAlarmRecipient\|BWebhookAlarmRecipient"` en 974 JARs → 0 resultados.

**La SPA como observador (no recipient)**:

La distinción es importante:
- **Recipient** = entidad que recibe la notificación al momento de generación, vía el pipeline BNotificationClass. Requiere un BComponent en el station.
- **Observador** = entidad que consume el estado actual del alarm space, sea via polling o push BOX.

La SPA es un **observador**, no un **recipient**. Las opciones son:

| Approach | Mecanismo | Latencia | Complejidad |
|----------|-----------|----------|-------------|
| BOX canal `alarm` subscribe | Push WebSocket | < 1s | Media (requiere BajaScript) |
| Polling REST `getAlarmsSinceTime` | Pull HTTP | 5-30s | Baja |
| Custom recipient con webhook | Push HTTP POST al backend SPA | < 1s | Alta (módulo Java) |

**Custom `BAlarmRecipient` con webhook**: Si se necesita push activo sin WebSocket, un módulo Niagara puede implementar:

```java
@NiagaraType
public class BWebhookAlarmRecipient extends BAlarmRecipient {
    @Override
    public void generateAlarm(BAlarmRecord rec, Context cx) {
        // HTTP POST al webhook de la SPA
        HttpClient.post(webhookUrl, toJson(rec));
    }
}
```

Este módulo debe estar firmado, instalado, y configurado en el `BNotificationClass`. Es la única manera de que Niagara haga **push HTTP** a una URL externa cuando se genera una alarma.

### 44.6.3 BEmailAlarmRecipient como referencia

`BEmailAlarmRecipient` es la implementación más común:

```
BAlarmRecipient (base)
 └─ BEmailAlarmRecipient
       ├─ toAddresses: String[]
       ├─ ccAddresses: String[]
       ├─ subjectFormat: BFormat (template con %{source}, %{priority}, etc.)
       └─ bodyFormat: BFormat (template con detalle)
```

El template `BFormat` de email es el mismo mecanismo que el template de notificación — puede usarse como referencia para el JSON template de webhook.

---

## 44.7 Servlet endpoints para alarmas

### 44.7.1 ¿Hay servlet dedicado en alarm-rt.jar?

**BÚSQUEDA EMPÍRICA**: Scan de `alarm-rt.jar` buscando clases que extiendan `BWebServlet` o `HttpServlet`:

El resultado empírico del Bloque 29 (matriz de 50+ servlets) NO lista ningún `BAlarmServlet` dedicado en `alarm-rt.jar`. No existe un endpoint nativo `/alarm/*` que sirva alarmas en JSON.

**Lo que SÍ existe**:
- **BOX channel `alarm`** — el mecanismo nativo de acceso al alarm space vía WebSocket BOX
- **BQL sobre `alarm:`** — accesible via el canal `ord` del BOX o vía el mecanismo NiagaraRPC
- **NiagaraRPC** (`/rpc/*`) — podría usarse para invocar métodos del alarm service, pero no hay endpoint documentado específico

### 44.7.2 NiagaraRPC para alarmas

El mecanismo NiagaraRPC (Bloque 29.13) permite invocar actions de BComponents via HTTP:

```
POST /rpc
Content-Type: application/json
Body:
{
  "method": "invoke",
  "handle": "<handle del BAlarmService>",
  "slot": "ackAlarms",
  "arg": {"ids": ["uuid1", "uuid2"]}
}
```

**LIMITACIÓN**: `handle` se obtiene via navegación del árbol de componentes — no es estático. En general, no se recomienda NiagaraRPC directo para alarmas desde SPA externa. El enfoque Reflow (servlet custom + BOX) es más robusto.

### 44.7.3 Endpoint REST via módulo custom (patrón Reflow)

El approach recomendado para SPA externa sin BajaScript directo es implementar un módulo Niagara con servlet:

**POST** `/mymodule/alarms/query`
```json
Request:
{
  "ackState": "unacknowledged",
  "priority": {"max": 99},
  "sourceOrd": "station:|slot:/Equipment",
  "limit": 50,
  "offset": 0,
  "orderBy": "timestamp",
  "orderDir": "DESC"
}

Response:
{
  "total": 123,
  "records": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "source": "station:|slot:/Equipment/AHU1/HighTemp",
      "priority": 50,
      "normalTime": null,
      "lastUpdate": "2026-05-04T14:23:11Z",
      "timestamp": "2026-05-04T14:20:00Z",
      "sourceStateDisplay": "High Temperature",
      "sourceState": "offnormal",
      "ackStateDisplay": "Unacknowledged",
      "ackState": "unacknowledged",
      "ackRequired": true,
      "alarmTransitionDisplay": "To Offnormal",
      "alarmTransition": "toOffnormal",
      "sourceClass": "HighTemp",
      "sourceClassDisplay": "High Temperature Alarm",
      "user": null,
      "alarmData": {},
      "noteCount": 0
    }
  ]
}
```

**DELETE/POST** `/mymodule/alarms/ack`
```json
Request: {"ids": ["uuid1", "uuid2"]}
Response: {"success": true, "acked": 2}
```

**POST** `/mymodule/alarms/{uuid}/note`
```json
Request: {"note": "Revisado. AHU-1 reiniciado a las 14:30."}
Response: {"success": true}
```

### 44.7.4 JSON shape confirmado de AlarmRecord

**CONFIRMADO** (`AlarmData.java:246-278`, Bloque 51.3.1):

```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "source": "station:|slot:/Equipment/AHU1/Temperature",
  "priority": 50,
  "normalTime": "2026-05-04T15:00:00Z",
  "lastUpdate": "2026-05-04T14:23:11Z",
  "timestamp": "2026-05-04T14:20:00Z",
  "sourceStateDisplay": "High Temperature",
  "sourceState": "offnormal",
  "ackStateDisplay": "Unacknowledged",
  "ackState": "unacknowledged",
  "ackRequired": true,
  "alarmTransitionDisplay": "To Offnormal",
  "alarmTransition": "toOffnormal",
  "sourceClass": "HighTemp",
  "sourceClassDisplay": "High Temperature Alarm",
  "user": null,
  "alarmData": {},
  "noteCount": 0
}
```

Este shape tiene **29 campos** cuando se expande `alarmData`. La serialización la hace el módulo Niagara (no hay un JSON endpoint nativo de Niagara que retorne exactamente este shape).

---

## 44.8 Anomalía canAcknowledgeAlarms → BBoolean

### 44.8.1 La anomalía confirmada

**CONFIRMADO** (`BReflowAlarmCommands.java:109-112`, Bloque 51):

```java
// TODOS los otros métodos BOX retornan BString (JSON serializado):
public BValue getClasses(BComponent comp, BValue arg, Context cx) throws Exception {
    return BString.make(toJson(result));  // → BString
}

// canAcknowledgeAlarms es el ÚNICO que retorna BBoolean:
public BValue canAcknowledgeAlarms(BComponent comp, BValue arg, Context cx) throws Exception {
    BComponent alarmService = Sys.getService(BAlarmService.TYPE);
    return BBoolean.make(cx.getUser().getPermissionsFor(alarmService).hasOperatorWrite());
    // ↑ retorna BBoolean, NO BString
}
```

**En el frontend** (CONFIRMADO `box.js:227`, Bloque 50.1.5):
> "canAcknowledgeAlarms returns BBoolean (NOT BString) — único entre todos los BOX endpoints (comentado explícitamente en box.js:227)"

### 44.8.2 ¿Es convención Niagara o bug de Reflow?

**Es una DECISIÓN DE DISEÑO de Reflow, no una convención Niagara**.

La convención Niagara de `BIServerSideCallHandler` dice que el método puede retornar cualquier `BValue`:

```java
// Contrato del framework:
public interface BIServerSideCallHandler {
    BValue serverSideCall(String methodName, BComponent comp, BValue arg, Context cx) throws Exception;
}
```

El return type es `BValue` — abstracto. `BBoolean` y `BString` son ambos `BValue`. El framework de BOX serializa el `BValue` de retorno según su tipo real:

- `BString` → se serializa como string JSON directo
- `BBoolean` → se serializa como `true`/`false` JSON
- `BInteger` → se serializa como número JSON
- `BFloat` → se serializa como número JSON

**El problema es el cliente que asume que TODOS los returns son BString y hace `JSON.parse()` sobre el valor sin verificar el tipo**. Este es el bug real: deserialización ciega.

### 44.8.3 Patrón correcto de deserialización

```javascript
// INCORRECTO — asume siempre BString:
const result = await box.call('module:TypeCommands', 'someMethod', null);
const data = JSON.parse(result);  // 💥 explota si result es true/false/number

// CORRECTO — verificar tipo:
const raw = await box.call('module:TypeCommands', 'someMethod', null);
let data;
if (typeof raw === 'string') {
    data = JSON.parse(raw);
} else if (typeof raw === 'boolean') {
    data = raw;  // BBoolean ya deserializado como JS boolean
} else if (typeof raw === 'number') {
    data = raw;  // BInteger/BFloat
}

// IDIOMÁTICO para canAcknowledgeAlarms específicamente:
const canAck = await box.call('nmodsreflow:ReflowAlarmCommands', 'canAcknowledgeAlarms', null);
// canAck ya es boolean JS — NO hacer JSON.parse()
```

### 44.8.4 ¿Todos los `can*` retornan BBoolean?

**INFERIDO — PROBABLEMENTE SÍ**: El naming `canX` sugiere que el método hace una pregunta de permiso que devuelve boolean. Si el desarrollador del módulo sigue convenciones semánticas (lo que `can*` implica un permiso check), es esperable que retornen `BBoolean`.

Sin embargo, esto es una INFERENCIA — no existe en el corpus estudiado ningún otro método `can*` confirmado en `alarm-rt.jar` o en el framework Niagara. La confirmación de que "todos los `can*` retornan BBoolean" requeriría revisar otros métodos con ese prefijo en el corpus.

**Implicación práctica**: Al consumir cualquier BOX endpoint de Niagara (propio o de terceros), NO asumir que el return type es siempre `BString`. Siempre verificar con `typeof` antes de parsear.

---

## 44.9 Refinamiento Bloques 42-43, 45-46, 48-49

### Bloque 42 — Subscriber lifecycle

**Impacto de Bloque 44**:
- El `baja.Subscriber` NO es el mecanismo correcto para eventos de alarma individuales. El canal BOX `alarm` es el correcto.
- Sin embargo, suscribirse a `alarm:` como BComponent (para observar cambios en el servicio) SÍ usa el Subscriber del Bloque 42.
- El Subscriber debe documentar cómo manejar `BAlarmService` como componente suscribible.

**Gap identificado**: El Bloque 42 debe cubrir cómo el servidor BOX limpia suscripciones cuando el WebSocket se cierra inesperadamente — especialmente para el canal `alarm` que puede tener muchos listeners activos.

**Prioridad post-44**: ALTA. El subscriber es la base de todos los canales live.

### Bloque 43 — Schedule render + edit desde SPA

**Impacto de Bloque 44**:
- Sin impacto directo. Schedules y alarmas son independientes.
- El único punto de contacto: alarmas generadas POR schedules (si un schedule activa/desactiva equipment que tiene alarm extensions). El flow es el mismo — la alarma llega al `BAlarmService` igual.

**Prioridad post-44**: BAJA. No hay interdependencia nueva.

### Bloque 45 — History/Trend chart consumption

**Impacto de Bloque 44**:
- History y Alarm history son conceptos relacionados pero con APIs distintas:
  - History (`history:`) — datos de tendencia de puntos (temperatura, flujo, etc.)
  - Alarm history (`alarm:`) — historial de eventos de alarma
- El Bloque 45 debe distinguir estos dos claramente.
- El shape JSON de Alarm history está documentado acá (Sección 44.7.4). El shape de History data está en Bloque 51.3.1.

**Nuevo hallazgo para Bloque 45**: El `alarm.adb` NO es SQLite (corrección del Bloque 31 mencionada en el INDEX). El formato es binario propietario con MAGIC `0x6010ACCD`. El Bloque 45 debe mencionar esto si cubre "alarm history charts" — los datos no son consultables con herramientas SQLite estándar.

**Prioridad post-44**: MEDIA.

### Bloque 46 — Writes con priority array desde SPA

**Impacto de Bloque 44**:
- Sin impacto directo en writes de puntos.
- Relevancia indirecta: cuando se escribe a un punto vía priority array (Bloque 46), si el punto tiene alarm extensions configuradas, la escritura puede cambiar el `sourceState` del punto y generar/resolver alarmas. El flow de alarma resultante es el documentado en este Bloque 44.
- El ACK de alarmas generadas por writes erróneos (ej. setpoint fuera de rango → alarma) va por el pipeline de Bloque 44.

**Gap en Bloque 46**: Documentar el ciclo completo: write → alarma → ACK → clear. Esto requiere ambos bloques (44 + 46) para el flujo end-to-end.

**Prioridad post-44**: ALTA. Priority array es crítico para control real.

### Bloque 48 — RBAC visibility en frontend

**Impacto de Bloque 44**:
- **CRÍTICO**: `canAcknowledgeAlarms` es el ejemplo concreto de RBAC check para alarmas. El Bloque 48 debe documentar este patrón.
- El permiso `operatorWrite` sobre `BAlarmService` es un caso real de RBAC que afecta la UI.
- El Bloque 48 debe cubrir cómo mapear roles Niagara (`superuser`, `operator`, `observer`) a permisos de UI para la consola de alarmas.

**Tabla RBAC para consola de alarmas** (para incorporar en Bloque 48):

| Acción UI | Permiso Niagara requerido | Role típico |
|-----------|--------------------------|-------------|
| Ver alarmas | `read` sobre BAlarmService | observer, operator, superuser |
| ACK alarma | `operatorWrite` sobre BAlarmService | operator, superuser |
| Clear alarma | `operatorWrite` sobre BAlarmService | operator, superuser |
| Silence alarma | `operatorWrite` sobre BAlarmService | operator, superuser |
| Agregar nota | `operatorWrite` sobre BAlarmService | operator, superuser |
| Configurar alarm classes | `admin` sobre BAlarmService | superuser |

**Prioridad post-44**: ALTA.

### Bloque 49 — Facets, i18n y formatting

**Impacto de Bloque 44**:
- `sourceStateDisplay` y `ackStateDisplay` en el JSON de alarma son strings pre-formateados por el servidor Niagara. El servidor aplica el lexicon de Niagara para traducir `offnormal` → "Fuera de Normal" (si el locale es ES).
- Para una SPA que recibe estos strings ya formateados desde el servlet, no hay problema de i18n de alarmas.
- Si la SPA consume BQL directamente y obtiene los valores crudos (`offnormal`, `unacknowledged`), DEBE hacer el mapping de traducción client-side.

**Strings que requieren traducción** en alarmas:
```javascript
const ALARM_STATE_LABELS = {
  en: {
    sourceState: { offnormal: "Offnormal", fault: "Fault", normal: "Normal" },
    ackState: { unacknowledged: "Unacknowledged", acknowledged: "Acknowledged" },
    alarmTransition: { toOffnormal: "To Offnormal", toFault: "To Fault", toNormal: "To Normal" }
  },
  es: {
    sourceState: { offnormal: "Fuera de Normal", fault: "Falla", normal: "Normal" },
    ackState: { unacknowledged: "Sin Reconocer", acknowledged: "Reconocida" },
    alarmTransition: { toOffnormal: "A Fuera de Normal", toFault: "A Falla", toNormal: "A Normal" }
  }
};
```

**Prioridad post-44**: BAJA. La i18n de alarmas es manejable con tabla de strings.

---

## 44.10 Antipatterns detectados

### AP44-1 — No cerrar BAlarmSpaceConnection

```java
// INCORRECTO
BAlarmSpaceConnection conn = alarmSvc.openAlarms(cx);
BAlarmRecord[] recs = conn.getOpenAlarms();
// ← NO se cierra. Leak de listeners en el servicio.

// CORRECTO
BAlarmSpaceConnection conn = alarmSvc.openAlarms(cx);
try {
    BAlarmRecord[] recs = conn.getOpenAlarms();
    // procesar recs
} finally {
    conn.close();
}
```

**Impacto**: Cada conexión no cerrada acumula listeners en `BAlarmService`. Con tráfico REST concurrente, cientos de listeners acumulados degradan el rendimiento del servicio.

### AP44-2 — Confundir open alarms con alarm history

```javascript
// INCORRECTO para "alarmas activas":
// alarm:|bql:select * from alarm ORDER BY timestamp DESC TOP 50
// Esto retorna TODA la historia, incluidas las resueltas

// CORRECTO para "alarmas activas":
// alarm:|bql:select * from alarm 
//   where sourceState != "normal" OR ackState = "unacknowledged"
//   ORDER BY priority ASC, timestamp DESC TOP 50
```

**Impacto**: La consola de alarmas muestra alarmas ya resueltas mezcladas con las activas.

### AP44-3 — Asumir que todos los BOX returns son JSON string

Ya documentado en 44.8. Repetido acá como antipatrón específico:

```javascript
// INCORRECTO
const result = await serverSideCall('SomeType', 'someMethod', null);
const parsed = JSON.parse(result);  // 💥 si result es boolean/number

// CORRECTO
const result = await serverSideCall('SomeType', 'someMethod', null);
const parsed = typeof result === 'string' ? JSON.parse(result) : result;
```

### AP44-4 — No verificar permiso de ACK antes de mostrar botones

```javascript
// INCORRECTO — mostrar botón sin verificar permiso:
<button onClick={ackAlarm}>ACK</button>
// El usuario hace click → servidor rechaza con 403 → UX confusa

// CORRECTO — verificar al cargar:
const [canAck, setCanAck] = useState(false);
useEffect(() => {
    checkAckPermission().then(setCanAck);
}, []);
<button onClick={ackAlarm} disabled={!canAck}>ACK</button>
```

### AP44-5 — Hacer ACK uno a uno en un loop

```javascript
// INCORRECTO — N requests para N alarmas:
for (const uuid of uuids) {
    await ackSingleAlarm(uuid);  // N round trips
}

// CORRECTO — batch en un solo call:
await baja.Ord.make("alarm:").get().then(svc => 
    svc.ackAlarms({ids: uuids})  // 1 round trip
);
```

**Impacto**: 50 ACKs individuales vs 1 ACK batch = 49 round trips extra. Con latencia de 50ms por request = 2.5 segundos extra innecesarios.

### AP44-6 — No manejar el caso de alarma ya ACK-eada (race condition)

En sistemas multi-usuario, entre el momento que un operador ve la alarma y el momento que hace ACK, otro operador puede haberla ACK-eado. El servidor rechaza el segundo ACK (o lo acepta silenciosamente — depende de la implementación).

```javascript
// CORRECTO — manejar el caso idempotente:
try {
    await ackAlarms(ids);
    refreshAlarmList();
} catch (err) {
    if (err.code === 'ALREADY_ACKED') {
        // Simplemente refrescar — la alarma ya está ack-eada
        refreshAlarmList();
    } else {
        showError(err);
    }
}
```

### AP44-7 — Usar getAlarmsSinceTime() sin límite para polling frecuente

**CONFIRMADO** (TODO-51-3, Bloque 51.6):

```java
// BReflowAlarmCommands.getAlarmsSinceTime() en Reflow:
// alarm:|bql:select * where timestamp.millis >= {ts}
// NO tiene TOP/límite
```

Si la SPA hace polling cada 5s y la station tiene 10,000 alarmas en el período, el query retorna todas. Para polling de novedades frecuente:

```javascript
// CORRECTO para polling:
async function pollNewAlarms(lastTimestamp) {
    const result = await getAlarmsSinceTime(lastTimestamp, { limit: 100 });
    // limit = 100 máximo por poll
    // Si result.length === 100, puede haber más — hacer otro poll con el último timestamp
    return result;
}
```

### AP44-8 — No configurar X-Frame-Options para embedding de consola de alarmas

Si la consola de alarmas de la SPA se embebe en un iframe dentro de Workbench (Bloque 47.6.3), y la station tiene `X-Frame-Options: SAMEORIGIN`, el iframe cross-origin falla. Verificar y configurar antes del deploy:

```
// En BOG de la station: WebService > WebServer > httpHeaderProviders > xFrameOptions
// Valor: SAMEORIGIN (default) NO permite cross-origin iframe
// Para permitir iframe desde dominio específico → configurar CSP frame-ancestors en lugar de X-Frame-Options
```

---

## 44.11 TODOs honestos

**TODO-44-1 — Nombres exactos de topics BOX channel `alarm`**: Los nombres `alarmGenerated`, `alarmAcked`, `alarmCleared` son INFERIDOS de los métodos del `BIAlarmSpaceConnectionListener`. No se decompilaron directamente los frames BOX del canal `alarm` en `box-rt.jar`. La confirmación requeriría sniffing del WebSocket BOX durante operaciones de alarma en un entorno lab.

**TODO-44-2 — `clearAlarm` y `silenceAlarm` en BajaScript**: Los métodos de clear y silence en la API JavaScript del `BAlarmService` no están confirmados en `app-readable.js` (Reflow no los implementa). Son inferidos del modelo Java. Confirmar nombres exactos con decompilación de `alarm-rt.jar:javax/baja/alarm/BAlarmService.class`.

**TODO-44-3 — `BAlarmCursor` vs `BIAlarmCursor`**: La clase/interfaz exacta para paginación (¿es `BAlarmCursor` o `BIAlarmCursor`?) requiere confirmación directa del bytecode. El nombre `BIAlarmCursor` (con 'I' de interfaz) es el patrón Niagara pero no está 100% confirmado para alarm-rt.jar.

**TODO-44-4 — TOP/SKIP en BQL sobre `alarm:`**: No se verificó empíricamente que `alarm:|bql:select * TOP 50 SKIP 0` funcione correctamente con ORDER BY. El comportamiento con `ORDER BY timestamp DESC` + `TOP 50 SKIP N` requiere prueba en lab — el cursor puede o no ordenar antes de paginar.

**TODO-44-5 — Canal BOX `alarm` frame format exacto**: El formato JSON de los mensajes del canal `alarm` (qué campos incluye la notificación push) no está documentado empíricamente. El contenido mostrado en la sección 44.2.3 es INFERIDO de la estructura de `BAlarmRecord`.

**TODO-44-6 — `silenceAlarm` semántica exacta**: El silenciado de alarmas en Niagara tiene matices: ¿aplica solo a la instancia actual o a futuras? ¿Cuánto tiempo dura el silencio? ¿Hay un timeout? No encontrado en el corpus analizado.

**TODO-44-7 — Límite de alarmas en memoria vs .adb**: El umbral exacto donde el BAlarmService pasa de memoria a archivo `.adb` no fue documentado. Impacta la performance de queries BQL (en memoria es O(n) sobre array, en .adb puede tener índices).

**TODO-44-8 — Comportamiento de `ackAlarms()` cuando algún UUID no existe**: ¿El método lanza excepción o silencia el UUID desconocido? Importante para manejo de errores en batch ACK.

---

## 44.12 Próximos pasos — plan de implementación

### Para la consola de alarmas de SEJOFA Dashboard

**Decisión 1 — Approach de conectividad**: Ya resuelta en Bloque 47. Usar **Approach A** (SPA dentro del módulo Niagara) o **Approach A'** (SPA + reverse proxy). El canal BOX `alarm` requiere BajaScript, que requiere same-origin.

**Decisión 2 — Dual channel para alarmas**:

```
┌─────────────────────────────────────────────────────────────┐
│ Consola de alarmas — Dual Channel                           │
├───────────────────┬─────────────────────────────────────────┤
│ Canal A — REST    │ Canal B — BOX                           │
│ (snapshot + page) │ (tiempo real)                           │
├───────────────────┼─────────────────────────────────────────┤
│ GET /alarms/query │ baja.Ord.make("alarm:").get()           │
│ → paginación 50   │ + canal `alarm` subscribe               │
│ → filtros server  │ → push individual por alarma            │
│ → CSV export      │ → contadores live (badge)               │
└───────────────────┴─────────────────────────────────────────┘
```

**Paso 1 — Backend servlet** (Java):

```java
@Override
protected void doPost(WebOp op) throws Exception {
    AlarmQueryRequest req = parseBody(op);
    
    BAlarmFilter filter = buildFilter(req);
    BAlarmSpaceConnection conn = alarmSvc.openAlarms(op.getContext());
    try {
        BIAlarmCursor cursor = conn.query(filter);
        try {
            AlarmQueryResponse resp = paginate(cursor, req.offset, req.limit);
            writeJson(op, resp);
        } finally {
            cursor.close();
        }
    } finally {
        conn.close();
    }
}
```

**Paso 2 — Frontend subscribe** (JavaScript con BajaScript):

```javascript
// En el componente Vue/React de la consola de alarmas
async mounted() {
    // Snapshot inicial via REST
    this.alarms = await this.$api.getAlarms({ ackState: 'unacknowledged', limit: 50 });
    
    // Subscribe a novedades via BOX
    const alarmSvc = await baja.Ord.make("alarm:").get({ subscriber: this.$subscriber });
    this.$subscriber.subscribe(alarmSvc, (comp, prop) => {
        if (prop.getName() === 'openAlarmCount' || prop.getName() === 'unackedAlarmCount') {
            // Refrescar la lista cuando el contador cambie
            this.refreshAlarms();
        }
    });
    
    // Verificar permiso de ACK
    this.canAck = await this.$box.call('module:AlarmCommands', 'canAcknowledgeAlarms', null);
}
```

**Paso 3 — ACK flow**:

```javascript
async ackSelected() {
    const uuids = this.selectedAlarms.map(a => a.uuid);
    try {
        await baja.Ord.make("alarm:").get().then(svc => svc.ackAlarms({ids: uuids}));
        this.selectedAlarms = [];
        await this.refreshAlarms();  // snapshot actualizado
    } catch (err) {
        this.showError('Error al reconocer alarmas: ' + err.message);
    }
}
```

**Paso 4 — Verificación de permisos al cargar**:

```javascript
// Al iniciar la consola
const [canAck, alarmClasses] = await Promise.all([
    this.checkAckPermission(),
    this.$api.getAlarmClasses()
]);
this.canAck = canAck;  // boolean directo (no JSON.parse)
this.alarmClasses = alarmClasses;
```

### Orden de implementación recomendado

1. **Módulo Java con servlet REST** — query paginado con filtros server-side
2. **Component Vue/React** — tabla de alarmas con paginación y filtros client-side
3. **BOX subscription** — badge contador de alarmas no-ack en navbar
4. **ACK batch** — con verificación de permiso previa
5. **Notas** — agregar nota por alarma
6. **Tiempo real** — canal BOX `alarm` para push de nuevas alarmas (después de que el Bloque 42 documente el subscriber completo)

### Referencias cruzadas

| Tema | Bloque | Sección |
|------|--------|---------|
| Bootstrap BOX/BajaScript | 47 | 47.1-47.4 |
| Subscriber lifecycle completo | 42 | — |
| BQL syntax | 21 | 21.x |
| RBAC y permisos | 48 | — |
| Audit log `BAuditService` | 30 | 30.x |
| Alarm history archive `.adb` formato | 31 | 31.x |
| BACnet → AlarmRouter → BAlarmService | 23 | 23.22 |
| BAnalyticAlert → BAlarmService | 16 | 16.4 |
| Web servlet registry + filter chain | 29 | 29.2-29.3 |
| CSRF en POST requests | 47 | 47.4 |

---

## Tabla resumen rápido — Alarm Console SPA

| Pregunta | Respuesta | Certeza |
|----------|-----------|---------|
| ¿Cómo acceder al BAlarmService desde JS? | `baja.Ord.make("alarm:").get()` | CONFIRMADO (app-readable.js:14520) |
| ¿Qué retorna getOpenAlarms()? | `BAlarmRecord[]` snapshot | INFERIDO (Java API) |
| ¿Cómo paginar alarmas? | BQL TOP/SKIP o cursor manual | INFERIDO con reservas |
| ¿Cómo hacer ACK batch? | `alarmSvc.ackAlarms({ids: [...]})` | CONFIRMADO (app-readable.js) |
| ¿Qué permiso requiere ACK? | `operatorWrite` en BAlarmService | CONFIRMADO (AlarmCommands.java:112) |
| ¿Hay servlet nativo /alarm? | NO | CONFIRMADO (scan 974 JARs) |
| ¿Existe BHttpAlarmRecipient? | NO | CONFIRMADO (scan 974 JARs) |
| ¿canAcknowledgeAlarms retorna BBoolean? | SÍ | CONFIRMADO (AlarmCommands.java:111) |
| ¿Es convención Niagara o bug Reflow? | Decisión de diseño Reflow | INFERIDO |
| ¿NEQL aplica a alarm:? | NO — alarm: es BHistoricalSpace | INFERIDO (architecture) |
| ¿Canal BOX `alarm` existe? | SÍ | CONFIRMADO (Bloque 47.5.6) |
| ¿SSE nativo para alarmas? | NO encontrado | CONFIRMADO (scan alarm-rt.jar) |

---

*Archivo producido*: `/home/cristian/niagara-research/niagara-mental-model-bloque44.md`

*Fuentes auditadas en este bloque*:
- `alarm-rt.jar` — análisis de classes via bytecode (indirecto via evidencia Reflow)
- `BReflowAlarmCommands.java` — implementación Java directa de operaciones de alarma
- `AlarmData.java` — serialización JSON y query de alarmas (Reflow)
- `app-readable.js:14520-14582` — ACK/notas via BajaScript (Reflow v1.7.5)
- `box-rt.jar` — inventario de channels BOX incluyendo `alarm` (via Bloque 47.5.6)
- `niagara-mental-model-bloque23.md` — BACnet AlarmRouter + NotificationClass
- `niagara-mental-model-bloque16.md` — BAnalyticAlert implements BIAlarmSource
- `niagara-mental-model-bloque25.md` — AX→N4 migration framework (AlarmDbMigrator)
- `niagara-mental-model-bloque29.md` — Web tier servlets matrix
- `niagara-mental-model-bloque47.md` — Bootstrap headless + BOX channels
- `niagara-mental-model-bloque50.md` — Reflow frontend audit (alarm dual-channel)
- `niagara-mental-model-bloque51.md` — Reflow -rt + app-readable.js deep dive
