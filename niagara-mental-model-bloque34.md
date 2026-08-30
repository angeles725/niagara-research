# Niagara N4 — Mental Model · Bloque 34

**Tema**: Alarm framework end-to-end operacional — `BAlarmService` lifecycle + routing pipeline + `alarmQueue` Worker, `BAlarmClass` (ack semantics + 3 niveles escalation), `BAlarmRecipient` taxonomy (Console/Station/Email/LinePrinter/Printer/Recoverable) + `newUnackedAlarm` topic, `BAlarmSource`+`BAlarmSourceExt`+14 algoritmos (offnormal/fault), **3 state enums distintos** (`BSourceState` vs `BAlarmState` vs `BAckState`) — NO son el mismo, `BAlarmRecord.BStruct` serialization + 34 alarmData fields extensibles, **`alarm.db` formato binario paginado** (NO SQLite) MAGIC 0x6010ACCD, `BAlarmArchive`+`BArchiveAlarmProvider`+`BOrionAlarmArchive` (RDB backend paralelo al .adb local), `bacnetAlarmRouter` BACnet intrinsic→Niagara re-assign, Honeywell `BHonAlarmClass`+`BHonConsoleRecipient` (buffered delay con thread propio), `alarmOrion` como RDB alarm DB alternativo a file, Fox alarm channels (`alarmdb`+`AlarmConsoleChannel`), gotchas operacionales ack race / loss-of-comm / Nre:Engine thread / remote station recipient.

**Método**: Investigación READ-ONLY — extracción `alarm-rt.jar` (196 clases), `alarm-ux.jar`, `alarm-wb.jar`, `alarm-se.jar`, `alarmOrion-rt.jar`, `honAlarmExt-rt.jar`, `honAlarmConsole-rt.jar`, `honAlarmConsole-ux.jar`, `bacnetAlarmRouter-rt.jar`, `email-rt.jar` a `/tmp/b34/`; `javap -p` sobre ~35 clases clave (`BAlarmService`, `BAlarmClass`, `BAlarmRecord`, `BAlarmSource`, `BAlarmSourceExt`, `BAlarmRecipient`, `BConsoleRecipient`, `BStationRecipient`, `BEmailRecipient`, `BRecoverableRecipient`, `BAlarmDatabase`, `BFileAlarmDatabase`, `BFileAlarmDbConfig`, `AlarmStore`, `AlarmStoreHeader`, `Block`, `Page`, `BAlarmDbConfig`, `BSourceState`, `BAckState`, `BAlarmState`, `BAlarmTransitionBits`, `BAlarmPriorities`, `BNotifyType`, `BAlarmSchema`, `BAlarmInstructions`, `BAlarmSourceInfo`, `BAlarmTimestamps`, `BArchiveAlarmProvider`, `BAlarmArchive`, `AlarmSupport`, `BAlarmAcknowledger`, `BFoxAlarmDatabase`, `BAlarmConsoleChannel`, `BAlarmFilterSet`, `BBacnetAlarmClassReassigner`, `BCustomBacnetEventProcessor`, `BAlarmFilter`, `BEscalationFilter`, `BHonAlarmClass`, `BHonConsoleRecipient`, `BDelayFilterState`, `BHonAlarmConsoleRpc`, `BOrionAlarmService`, `BOrionAlarmRecord`, `BOrionAlarmDatabase`, `BOrionAlarmArchive`); `javap -c` sobre `AlarmStoreHeader` para extraer MAGIC (`1611526157` = `0x6010ACCD`) y `BFileAlarmDatabase` para literales thread-warning; grep sobre `niagara-help/docs-text/docAlarms.txt` (5 647 líneas) + `defaults/system.properties` (589 líneas); inventario `META-INF/module.xml` de cada JAR para enumerar BComponents registrados.

**Conecta con**:
- Bloque 8.2 (alarmExt básico — aquí se decompila end-to-end, corrige taxonomía de algoritmos)
- Bloque 8.3 (BAckState mencionado — aquí se verifica que son **3 enums distintos** a menudo confundidos: `BSourceState`/`BAckState`/`BAlarmState`)
- Bloque 15 (alarm class mencionado — aquí lifecycle + escalation + topics)
- Bloque 5.4 + 13.2 (keyring DPAPI `.km/.kr` NO `master.jceks` — usado por `BEmailRecipient`+`BEmailAccount` para credenciales SMTP/OAuth2; ver también `oauth2-rt` dep en email-rt module.xml)
- Bloque 13.1.7 + 19.13 (Supervisor bottleneck ~50 subordinados — **sí aplica a alarm aggregation**: cada `BStationRecipient` mantiene conexión Fox hacia subordinados, ver 34.6.3)
- Bloque 23.7 (BACnet intrinsic/algorithmic — upstream natural del `bacnetAlarmRouter`, 34.11)
- Bloque 24.14 (Schedule DST edge cases — aplica idénticamente a `BAlarmRecord.timestamp` / `ackTime` / `normalTime`)
- Bloque 31.4 (audit queue unbounded `LinkedBlockingQueue` → OOM — **hallazgo similar en `BAlarmService.alarmQueue`**: `javax.baja.util.Queue` NO acotada, ver 34.1.2)
- Bloque 31.3 (history archive 5-30 min — CORRECCIÓN cruzada: `.adb` es OTRO binario Tridium, NO SQLite, ver 34.10)
- Bloque 33.0 (`.hdb` binario Tridium — paralelo arquitectónico exacto al `.adb`, ambos paginados custom, MAGIC distinto)

---

## 34.0 Contexto — corrección crítica al Bloque 31 sobre `.adb`

**Bloque 31.3 sugería** que el alarm db usa SQLite con VACUUM blocking 5-30 min.

**Hallazgo empírico al decompilar `com.tridium.alarm.db.file.AlarmStoreHeader`**:

```
javap -c AlarmStoreHeader.class | grep ldc
  13: ldc  #15   // int 1611526157   → 0x6010ACCD (MAGIC)
  22: ldc  #35   // String "Invalid or corrupt alarm database."
  57: ldc  #36   // String "Unrecognized alarm db version:"
```

**NO** es SQLite. Es binario propietario Tridium con header fijo, page-based:

| Campo header | Offset | Tipo | Valor |
|---|---|---|---|
| MAGIC | 0 | int | `0x6010ACCD` (`1611526157` decimal) |
| version | 4 | int | schema version |
| recordVersion | 8 | int | `BAlarmRecord` serialization version |
| dataOffset | 12 | long | offset absoluto inicio de páginas |
| pageSize | 20 | int | `DEFAULT_PAGE_SIZE` (constante privada no literal en bytecode) |
| pagesPerBlock | 24 | int | `DEFAULT_PAGES_PER_BLOCK` |
| creationTime | 28 | long | millis al crear archivo |

Paralelo exacto al `.hdb` de Bloque 33.0 (history binario MAGIC `0xA0F61E5E`), **pero formatos distintos** — NO se lee un `.adb` con parser de `.hdb`.

**Consecuencia operacional**:
1. **No hay VACUUM** en `.adb`. Hay `trimToCapacity()` (`AlarmStore.trimToCapacity`) que descarta el registro más antiguo cuando se excede `capacity` (configurado en `BFileAlarmDbConfig.capacity`).
2. **`clearOldRecords(BAbsTime, Context)`** itera y borra registros con timestamp < cutoff — pero libera páginas al `FreePageMap`, **NO compacta el archivo** (el archivo puede mantener tamaño aunque registros vivos disminuyan, como `.hdb`).
3. La ventana "5-30 min" de Bloque 31 NO aplica al `.adb`. Aplica a `archive → Orion RDB` cuando `BArchiveAlarmProvider.execute()` dispara: ese job sí puede bloquear según tamaño batch hacia SQL server remoto.

SQLite **no aparece** en package `com.tridium.alarm.db.*`. Strings `jdbc:`/`sqlite` ausentes. El RDB path es exclusivamente `alarmOrion` + `rdb-rt` (Orion connector sobre jTDS/MS-SQL, confirmado en docAlarms.txt línea 1058 "jTDS driver").

---

## 34.1 `BAlarmService` — lifecycle, threads, queue

### 34.1.1 Inventario slots (decompilado `javax.baja.alarm.BAlarmService`)

`BAlarmService extends BAbstractService implements BIAlarmClassFolder, BIDataRecoverySourceService, BIRestrictedComponent`.

| Slot | Tipo | Rol |
|---|---|---|
| `alarmDbConfig` | `BAlarmDbConfig` | Config del alarm db (record type + schema + capacity) |
| `defaultAlarmClass` | `BAlarmClass` | Clase fallback si el alarmExt no define una |
| `masterAlarmInstructions` | `BAlarmInstructions` | Instrucciones globales concatenadas a la de cada alarmExt |
| `escalationTimeTrigger` | `BTimeTrigger` (control.trigger) | Cuándo evalúa escalation (cron-like) |
| `coalesceAlarms` | boolean | Si `true`, alarmas idénticas rápidas colapsan a una (ver 34.2.4) |
| **Actions**: | | |
| `routeAlarm(BAlarmRecord)` | void | Entrada programática para disparar alarma |
| `ackAlarm(BAlarmRecord)` | void | Ack por UUID |
| `enableToOffnormal / disableToOffnormal` | `BOrd(BVector)` | Bulk flag sobre puntos |
| `enableToFault / disableToFault` | `BOrd(BVector)` | Bulk flag |
| `auditForceClear(BAlarmRecord)` | void | Force-clear manual con audit trail |
| `escalateAlarms()` | void | Invocación manual a loop de escalation |
| **Topics**: | | |
| `alarm` | `Topic` | Fire general — cualquier alarma recibida |

### 34.1.2 Internal state — `alarmQueue` + Worker

Fields privados decompilados:
```java
private BAlarmDatabase alarmDb;                        // creado en initAlarmDb()
private final Queue alarmQueue;                        // javax.baja.util.Queue — NO acotado
private Worker alarmWorker;                            // worker dedicado "Alarm:ServiceWorker"
private final Station$SaveListener saveListener;       // callback pre-save del station
private final BAlarmService$PlatformServiceListener listener; // alarmas del platform service
```

Del `javap -c BAlarmService`:
```
132: ldc  #63   // String "Alarm:ServiceWorker"
134: invokevirtual Worker.start(Ljava/lang/String;)V
146: ldc  #67   // String "alarmdb"
148: invokevirtual BFoxChannelRegistry.get(...)
```

**Hallazgo clave**: El `alarmQueue` es `javax.baja.util.Queue` — la misma clase usada en `BAuditHistoryService` (Bloque 31.4). **No tiene `capacity` parameter** — es FIFO unbounded. Si el `alarmWorker` se bloquea (e.g., `BStationRecipient.handleAlarm` esperando a subordinado caído), la cola crece indefinidamente.

**Gotcha G1 (alarm queue OOM equivalente a audit queue)**:
- Supervisor con 50 subordinados (cap Bloque 13.1.7) + cada subordinado con `BStationRecipient` hacia Supervisor + un evento masivo (reinicio red, storm BACnet) → 50 × 500 alarms = 25 000 `BAlarmRecord` en cola por station recipient.
- `BAlarmRecord` pesa ~300-800 bytes serialized + `alarmData` BFacets adicional → 10-30 MB de heap por storm.
- En XS station (256 MB heap, ver Bloque 31.2 heap scale) → OOM.
- Análogo al audit queue (Bloque 31.4) pero agravado porque `BStationRecipient` NO extiende `BRecoverableRecipient` (34.6.4) — no hay persistencia a disco.

### 34.1.3 Routing pipeline — `doRouteAlarm` → `doRouteToRecipient` → `doRouteToSource`

Método clave (decompilado):
```java
public void doRouteAlarm(BAlarmRecord) throws Exception;       // llamado desde alarmWorker
public void doRouteToRecipient(BAlarmRecord);                  // enumera recipients del AlarmClass
public void doRouteToSource(BAlarmRecord) throws Exception;    // refleja ack/state back a BIAlarmSource
public void doRouteToSource(BAlarmRecord, BObject) throws Exception;
```

Pseudoflow (reconstruido desde bytecode + docAlarms.txt Ch.3):

```
           ┌───────────────────────────────────────────────────┐
           │  BControlPoint.setValue() ───► StatusValue change │
           └──────────────────────┬────────────────────────────┘
                                  ▼
           ┌───────────────────────────────────────────────────┐
           │  BAlarmSourceExt.changed(status, context)         │
           │  → BOffnormalAlgorithm.evaluate() (or fault)      │
           │  → si state-change: AlarmSupport.newOffnormalAlarm│
           └──────────────────────┬────────────────────────────┘
                                  ▼
           ┌───────────────────────────────────────────────────┐
           │  BAlarmService.fireAlarm(BAlarmRecord)            │
           │  → alarmQueue.put(record)   ◄── unbounded Queue   │
           └──────────────────────┬────────────────────────────┘
                                  ▼ (alarmWorker thread)
           ┌───────────────────────────────────────────────────┐
           │  doRouteAlarm(record)                             │
           │  1. alarmDb.append(record)  (BFileAlarmDatabase)  │
           │  2. lookupAlarmClass(record.getAlarmClass())      │
           │  3. BAlarmClass.doRouteAlarm(record)              │
           │     → fire topic 'alarm' to all linked recipients │
           │  4. if coalesceAlarms && isDuplicateRecent → skip │
           └──────────────────────┬────────────────────────────┘
                                  ▼
           ┌───────────────────────────────────────────────────┐
           │  BAlarmRecipient.handleAlarm(record)              │
           │  (Console / Station / Email / LinePrinter / ...)  │
           │  → fire topic 'newUnackedAlarm'                   │
           └──────────────────────┬────────────────────────────┘
                                  ▼
           ┌───────────────────────────────────────────────────┐
           │  Operator: alarm console → ack button             │
           │  → BIAlarmSource.ackAlarm(record) (source side)   │
           │  → BAlarmService.ackAlarm(record) (service side)  │
           │  → alarmDb.update(record with ackState=acked)     │
           │  → doRouteToSource(record) if recipient sent ack  │
           └───────────────────────────────────────────────────┘
```

### 34.1.4 License gate

Del `javap -c getLicenseFeature()`:
```
3: ldc #92  // "tridium"
5: ldc #93  // "alarm"
7: invokeinterface LicenseManager.getFeature(String,String)
32: ldc #99 // "Unlicensed for all alarm types. Alarms are disabled."
```

Si no hay feature `tridium:alarm` → `configFatal`. Esto también bloquea Orion/archive — el license check ocurre en `BAlarmService.getLicenseFeature()` y cascadea a `BArchiveAlarmProvider` (feature `UNLICENSED` constant string).

### 34.1.5 `atSteadyState()` y `serviceStarted()`

Override:
- `serviceStarted()` → registra el FoxChannel `"alarmdb"` en `BFoxChannelRegistry` → subordinados pueden hacer BQL contra el alarm db vía Fox.
- `atSteadyState()` → dispara data recovery restore (si hay evento `.drr` pendiente de crash anterior — ver 34.10.5).
- `serviceStopped()` → remove `"alarmdb"` channel + flush `alarmDb`.

---

## 34.2 `BAlarmClass` — clasificación, routing, escalation, coalesce

### 34.2.1 Slots inventariados

`BAlarmClass extends BComponent` (NO `BAbstractService`, NO `BFolder`).

| Slot | Tipo | Rol |
|---|---|---|
| `ackRequired` | `BAlarmTransitionBits` | Bits OR: `TO_OFFNORMAL`/`TO_FAULT`/`TO_NORMAL`/`TO_ALERT` |
| `priority` | `BAlarmPriorities` | 4-tuple int `(toOffnormal, toFault, toNormal, toAlert)` 0-255 |
| `totalAlarmCount` | int | Histórico acumulado |
| `openAlarmCount` | int | Actualmente sin clear |
| `inAlarmCount` | int | Actualmente en offnormal/fault (no normal) |
| `unackedAlarmCount` | int | Pendientes de ack |
| `timeOfLastAlarm` | `BAbsTime` | Último fire |
| `escalationLevel1Enabled` + `escalationLevel1Delay` | boolean + `BRelTime` | Level 1 |
| `escalationLevel2Enabled` + `escalationLevel2Delay` | boolean + `BRelTime` | Level 2 |
| `escalationLevel3Enabled` + `escalationLevel3Delay` | boolean + `BRelTime` | Level 3 |

### 34.2.2 Topics — 4 separados

```java
public static final Topic alarm;              // level 0 (inicial)
public static final Topic escalatedAlarm1;    // level 1
public static final Topic escalatedAlarm2;    // level 2
public static final Topic escalatedAlarm3;    // level 3
```

**String constants**:
```java
public static final String ESCALATED;  // prefix
public static final String LEVEL_1;
public static final String LEVEL_2;
public static final String LEVEL_3;
```

Estos strings definen los **subscription patterns** que los recipients usan para suscribirse a un nivel específico. `BAlarmRecipient.getSubscribedEscalatedAlarmClasses(int level)` retorna las clases a las que este recipient está suscrito en el nivel dado.

**Gotcha G2 (escalation wiring común mal entendido)**: Para "escalar a otro recipient al minuto 10" hay que:
1. Enable `escalationLevel1Enabled` en la `BAlarmClass`.
2. Setear `escalationLevel1Delay = 10m`.
3. **Linkear el topic `escalatedAlarm1` de la AlarmClass al slot `routeAlarm` de otro recipient** — NO reusa el recipient original. Cada nivel es independiente.
4. Cuando el timer se cumple y alarm sigue `unacked` → dispara `fireEscalatedAlarm1(record)`.

`BAlarmService.escalationTimeTrigger` es un `BTimeTrigger` que invoca `escalateAlarms()` periódicamente (típico: cada minuto). Este método recorre el alarm db buscando `isOpen && !isAcknowledged && age > delay` por level.

### 34.2.3 `BAlarmPriorities` — 4-tuple

```java
public final class BAlarmPriorities extends BSimple {
  private int toNormal;
  private int toOffnormal;
  private int toFault;
  private int toAlert;
  public static final int MAX_PRIORITY;     // 255 (estándar BACnet)
  public static final int MIN_PRIORITY;     // 0
  public int getPriority(BSourceState state);
}
```

Cada **transición** tiene prioridad distinta. Típico BACnet:
- `toOffnormal` = 100
- `toFault` = 50 (más urgente, menor número en BACnet)
- `toNormal` = 200 (low)
- `toAlert` = 128

Priority se escribe en el `BAlarmRecord.priority` property al momento de crear el record (no se recalcula). Los recipients pueden filtrar por priority threshold (BACnet `notificationClass.priority` mapping — ver 34.11).

### 34.2.4 Coalesce — dedup de alarmas rápidas

`BAlarmService.coalesceAlarms` (boolean, default **true** según docAlarms). Método: si una alarma nueva tiene `source + alarmClass + alarmState` idéntico a una alarma open <1s anterior → actualiza `lastUpdate` + `count` en `BAlarmTimestamps` pero NO crea nuevo record.

Implementación (inferida): `CoalesceUuidOnlyInvocation` class presente en package `com.tridium.alarm.*`. Se reusa el mismo UUID.

**Gotcha G3**: coalesce ES incompatible con `BHonAlarmClass.enableAlarmDelay` (34.12.1). Honeywell buffera todas las alarmas por `delayTime` y luego solo envía el "último estado" por source. Si además coalesce=true se pierde doble. Usar uno u otro, NO ambos.

---

## 34.3 Los **3 enums** que todo el mundo confunde

Esta sección aclara una confusión recurrente: hay **tres** state enums en alarm-rt, **todos distintos**, a menudo llamados genéricamente "alarm state" en la literatura.

### 34.3.1 `BSourceState` — estado del source

```java
public final class BSourceState extends BFrozenEnum {
  public static final int NORMAL    = 0;
  public static final int OFFNORMAL = 1;
  public static final int FAULT     = 2;
  public static final int ALERT     = 3;
  public BAlarmTransitionBits getAlarmTransitionBits();
}
```

- Refleja el **estado físico de la fuente** (point) al momento.
- Se usa en `BAlarmRecord.sourceState` y `BAlarmRecord.alarmTransition`.
- Método `getAlarmTransitionBits()` mapea al bit correspondiente en `BAlarmTransitionBits` para ack logic.

### 34.3.2 `BAckState` — estado del ack

```java
public final class BAckState extends BFrozenEnum {
  public static final int ACKED        = 0;
  public static final int UNACKED      = 1;
  public static final int ACK_PENDING  = 2;
}
```

- **3 valores**, NO 2. `ACK_PENDING` existe entre el momento en que un recipient remoto envió un ack y cuando el record en DB se actualiza (race window).
- Se usa en `BAlarmRecord.ackState`.
- `BAlarmRecord.isAckPending()` distingue `ACK_PENDING` de `UNACKED`.

### 34.3.3 `BAlarmState` — estado del alarmExt algorithm (NO del record)

```java
public final class BAlarmState extends BFrozenEnum {
  public static final int NORMAL     = 0;
  public static final int FAULT      = 1;
  public static final int OFFNORMAL  = 2;
  public static final int HIGH_LIMIT = 3;
  public static final int LOW_LIMIT  = 4;
  public boolean isInAlarm();
  public boolean isOffnormal();
}
```

- **5 valores** (agrega `HIGH_LIMIT`/`LOW_LIMIT` como subestados de offnormal).
- **Vive en `javax.baja.alarm.ext`**, NO en `javax.baja.alarm`.
- Se usa en `BAlarmSourceExt.alarmState` property — es el estado interno del **algoritmo** (`BOutOfRangeAlgorithm` necesita distinguir high vs low para text/sound distinto).
- **NO va al `BAlarmRecord`** directamente. El algoritmo mapea `highLimit/lowLimit` → `BSourceState.offnormal` al crear el record (información high/low se preserva en `alarmData` facets `HIGH_LIMIT`/`LOW_LIMIT` string).

### 34.3.4 Tabla de transiciones reales (de docAlarms.txt + bytecode)

| `alarmState` (ext) | `sourceState` (record) | `ackState` (record) | Open/Cleared |
|---|---|---|---|
| offnormal | OFFNORMAL | UNACKED | Open |
| offnormal | OFFNORMAL | ACKED | Open |
| normal | NORMAL | UNACKED | Open |
| normal | NORMAL | ACKED | **Cleared** (único estado cleared) |
| fault | FAULT | UNACKED | Open |
| fault | FAULT | ACKED | Open |
| highLimit | OFFNORMAL | UNACKED | Open (facet `HIGH_LIMIT` set) |
| lowLimit | OFFNORMAL | UNACKED | Open (facet `LOW_LIMIT` set) |

Clave: un alarm record solo se **clear** (desaparece del console) cuando `sourceState=NORMAL && ackState=ACKED` simultáneamente.

### 34.3.5 Gotcha G4 — `unack normal` edge case

Secuencia común:
```
t0: point goes offnormal → record created (OFFNORMAL, UNACKED)
t1: point returns normal BEFORE operator acks → record updated
       (sourceState=NORMAL, ackState=UNACKED)
       alarmTransition=NORMAL preserved en record
t2: operator acks → (NORMAL, ACKED) → cleared
```

El record **entre t1 y t2** es "normal pero unacked" → **sigue Open** (aparece en console). Esto es intencional: operador debe acknowledgear incluso alarmas que ya se auto-resolvieron, para confirmar awareness.

Si `BAlarmClass.ackRequired.isToNormal() == false` (bit TO_NORMAL desactivado), entonces al llegar a t1 el record se clear automáticamente sin pasar por t2. Default BACnet-compliant: `ackRequired = toOffnormal | toFault` (SIN toNormal).

---

## 34.4 `BAlarmTransitionBits` — bit-string de transiciones

```java
public final class BAlarmTransitionBits extends BBitString {
  public static final int TO_OFFNORMAL = 1;   // bit 0
  public static final int TO_FAULT     = 2;   // bit 1
  public static final int TO_NORMAL    = 4;   // bit 2
  public static final int TO_ALERT     = 8;   // bit 3
  public static final BAlarmTransitionBits EMPTY;  // 0
  public static final BAlarmTransitionBits ALL;    // 15
}
```

Usos:
1. **`BAlarmClass.ackRequired`**: qué transiciones requieren ack.
2. **`BAlarmRecipient.transitions`**: qué transiciones RECIBE el recipient (filter outbound).
3. **`BAlarmSourceExt.alarmEnable`**: qué transiciones están ENABLED en la fuente (supprimir toFault si device flaky).
4. **`BAlarmSourceExt.ackedTransitions`**: histórico de qué transiciones ya tuvieron ack.

**Gotcha G5**: confundir `transitions` del recipient con `ackRequired` del class es el error #1 al configurar alarms:
- `ackRequired=ALL` + `recipient.transitions=toOffnormal|toFault` → Email se envía solo al inicio; la vuelta a normal NO dispara email. Si el operator nunca entra a workbench y solo recibe emails → no sabe que la alarma se resolvió sola.
- Fix: setear `recipient.transitions=ALL` y `recipient.routeAcks=true` para también recibir "alarm acknowledged" emails.

---

## 34.5 `BAlarmSourceExt` + 14 algoritmos

`BAlarmSourceExt extends BPointExtension implements BIAlarmSource, BIAlarmMessages`.

### 34.5.1 Slots del alarmExt

| Slot | Tipo | Rol |
|---|---|---|
| `alarmInhibit` | `BStatusBoolean` | `true` → suprime eval temporal (por operador) |
| `inhibitTime` | `BRelTime` | Auto-clear inhibit después de X min |
| `alarmState` | `BAlarmState` | Estado interno (5-valor, ver 34.3.3) |
| `timeDelay` | `BRelTime` | Debe estar en offnormal X segundos antes de disparar (debouncing) |
| `timeDelayToNormal` | `BRelTime` | Debe estar normal X antes de clear |
| `alarmEnable` | `BAlarmTransitionBits` | Qué transiciones están enabled |
| `ackedTransitions` | `BAlarmTransitionBits` | Histórico acks |
| `toOffnormalTimes` | `BAlarmTimestamps` | Count + first/last times de offnormal |
| `toFaultTimes` | `BAlarmTimestamps` | ídem fault |
| `timeInCurrentState` | `BRelTime` | Duración del estado actual |
| `sourceName` | `BFormat` | Template para nombre legible (substituciones `%parent.name%`) |
| `toFaultText` / `toOffnormalText` / `toNormalText` | `BFormat` | Mensajes por transición |
| `hyperlinkOrd` | `BOrd` | URL/nav a abrir al click en alarma |
| `soundFile` | `BOrd` | WAV para trigger audio |
| `alarmIcon` | `BOrd` | Icono custom |
| `alarmInstructions` | `BAlarmInstructions` | Instrucciones operacionales |
| `faultAlgorithm` | `BFaultAlgorithm` | Algoritmo fault (4 subtipos, ver abajo) |
| `offnormalAlgorithm` | `BOffnormalAlgorithm` | Algoritmo offnormal (10 subtipos) |
| `alarmClass` | String | Nombre de la AlarmClass a usar |
| `metaData` | `BFacets` | Metadata custom (copy-to-record en `alarmData`) |
| `status` | `BStatus` | Forwarded del parent point |

### 34.5.2 `BAlarmTimestamps` — struct count + times

```java
public class BAlarmTimestamps extends BStruct {
  BAbsTime alarmTime;
  BAbsTime ackTime;
  BAbsTime normalTime;
  int count;    // cuántas veces disparó
}
```

Este struct se guarda en el alarmExt (NO en el record). Permite "¿cuántas veces offnormal en esta sesión?" sin query al alarm db. `count` nunca se decrementa — solo reset manual en workbench.

### 34.5.3 Algoritmos — enumeración completa (de `module.xml` alarm-rt)

**`BOffnormalAlgorithm` subclases (10)** — package `javax.baja.alarm.ext.offnormal`:
| Algoritmo | Uso | Property tipo |
|---|---|---|
| `BTwoStateAlgorithm` | Boolean 2-estado con normal configurable | boolean |
| `BOutOfRangeAlgorithm` | Numeric high/low con deadband + validation delay | double |
| `BFloatingLimitAlgorithm` | Numeric relativo a setpoint (±delta) | double |
| `BEnumChangeOfStateAlgorithm` | Enum con lista de valores offnormal | BEnum |
| `BBooleanChangeOfStateAlgorithm` | Boolean con valor específico offnormal | boolean |
| `BStringChangeOfStateAlgorithm` | String match valores offnormal | String |
| `BStringChangeOfStateFaultAlgorithm` | String match → fault (hybrid) | String |
| `BEnumCommandFailureAlgorithm` | Enum feedback vs command mismatch | BEnum |
| `BBooleanCommandFailureAlgorithm` | Boolean feedback vs command mismatch | boolean |
| `BStatusAlgorithm` | BStatus flags (down/disabled/alarm) → offnormal | cualquier status |
| `BNumericChangeOfStateAlgorithm` | Numeric exact match (step alarms) | double |

**`BFaultAlgorithm` subclases (4)** — package `javax.baja.alarm.ext.fault`:
| Algoritmo | Uso |
|---|---|
| `BOutOfRangeFaultAlgorithm` | Numeric con 2 límites externos (fault vs offnormal) |
| `BTwoStateFaultAlgorithm` | Boolean → fault |
| `BEnumFaultAlgorithm` | Enum → fault |
| `BStatusFaultAlgorithm` | BStatus.isDown() / isFault() → fault |

### 34.5.4 State machine interno — `BOutOfRangeAlgorithm` como ejemplo

Del `module.xml`:
```
BOutOfRangeAlgorithm$HighAlarmState
BOutOfRangeAlgorithm$LowAlarmState
BOutOfRangeAlgorithm$NormalState
BOutOfRangeAlgorithm$OutOfRangeState
BOutOfRangeAlgorithm$ValidateHighAlarmState
BOutOfRangeAlgorithm$ValidateLowAlarmState
BOutOfRangeAlgorithm$ValidateReturnFromHighState
BOutOfRangeAlgorithm$ValidateReturnFromLowState
BOutOfRangeAlgorithm$ValidationState
```

**9 inner states**. El flujo es:
1. Normal → valor sube > highLimit → `ValidateHighAlarmState` (timer `timeDelay`)
2. Si sigue > highLimit al vencer timer → `HighAlarmState` (fire alarm)
3. Si vuelve < highLimit-deadband antes del timer → vuelve a `NormalState` (NO dispara — debouncing)
4. Desde `HighAlarmState`, valor < highLimit-deadband → `ValidateReturnFromHighState` (timer `timeDelayToNormal`)
5. Al vencer → `NormalState` + fire to-normal

Esto implementa hysteresis completa + debouncing bidireccional. **Cada state es un `BFrozenEnum`** (confirmado en bytecode).

### 34.5.5 `AlarmSupport.ToNormalTransition`

Inner class privada:
```java
class AlarmSupport$ToNormalTransition implements Runnable {
  private final BAlarmRecord lastRecord;
  private final BAlarmClass alarmClass;
  private final boolean all;
  private final BFacets newFacets;
  private final BAbsTime toNormalTimestamp;
}
```

Se encola al `alarmWorker` para serializar transiciones to-normal. Si 100 puntos vuelven a normal simultáneamente, se procesan en orden, no en paralelo → previene race en `alarmDb.update`.

---

## 34.6 Recipients — taxonomía completa

Jerarquía:
```
BAlarmRecipient (abstract, alarm-rt)
├── BConsoleRecipient (com.tridium.alarm, alarm-rt)
│    └── BHonConsoleRecipient (honAlarmExt-rt) — extends, buffer thread
│    └── BPortalConsoleRecipient (alarm-wb) — portal web variant
├── BStationRecipient (com.tridium.alarm, alarm-rt)
├── BRecoverableRecipient (abstract, alarm-rt)
│    └── BEmailRecipient (com.tridium.email.alarm, email-rt)
│    └── BSmsRecipient (sms-rt — separate JAR)
├── BLinePrinterRecipient (com.tridium.alarm.print, alarm-se)
└── BPrinterRecipient (com.tridium.alarm.print, alarm-se)
```

### 34.6.1 `BAlarmRecipient` base — slots comunes

| Slot | Rol |
|---|---|
| `timeRange` | `BTimeRange` — solo recibir alarmas en horario X (e.g., 08:00-18:00) |
| `daysOfWeek` | `BDaysOfWeekBits` — solo ciertos días |
| `transitions` | `BAlarmTransitionBits` — filtro outbound (34.4) |
| `routeAcks` | boolean — si true, acks también se envían (útil para email) |
| **Action** `routeAlarm(BAlarmRecord)` | Entry point |
| **Action** `routeAlarmAck(BAlarmRecord)` | Ack entry point |
| **Topic** `newUnackedAlarm` | Fire cuando llega nuevo unacked (para subscribers UI) |

Template methods:
```java
public final void doRouteAlarm(BAlarmRecord);      // NO overridable — filter + dispatch
public abstract void handleAlarm(BAlarmRecord);    // subclass impl
public boolean accept(BAlarmRecord);                // filter: timeRange+days+transitions
```

`accept()` evalúa:
1. `timeRange.includes(now)` ?
2. `daysOfWeek.includes(today)` ?
3. `transitions.includes(record.alarmTransition)` ?
4. `getSubscribedAlarmClasses().contains(record.getAlarmClass())` ? (link-based)

Si **todo** es `true` → `handleAlarm(record)`. Si no → record discarded para este recipient.

### 34.6.2 `BConsoleRecipient` — default console en Workbench

```java
public class BConsoleRecipient extends BAlarmRecipient {
  BStatus status;
  String faultCause;
  BAlarmTransitionBits transitions;
  boolean routeAcks;
  BDynamicTimeRange defaultTimeRange;  // para la UI "show alarms from last 24h"
  Set<BiConsumer<BConsoleRecipient, BAlarmRecord>> alarmHandlers;  // suscriptores in-memory
}
```

El `alarmHandlers` set permite a otros componentes (e.g., `BTrayIcon`, `HonAlarmConsole`) registrar callbacks in-process para recibir alarmas sin pasar por Fox.

**handleAlarm flow**:
1. Add record al alarm db (via `BAlarmService.alarmDb`).
2. Notificar cada `alarmHandler` registrado (sync, en el worker thread).
3. Fire topic `newUnackedAlarm` si `record.ackState == UNACKED`.
4. Vía `BAlarmConsoleChannel` (Fox), notificar consoles remotas.

### 34.6.3 `BStationRecipient` — forward a otra station

```java
public class BStationRecipient extends BAlarmRecipient {
  String remoteStation;   // nombre station destino (no ORD completo)
  private NiagaraStation getStation() throws Exception;
  private BIRemoteAlarmRecipient getAlarmDeviceExt() throws Exception;
}
```

Implementación `handleAlarm`:
1. Resolver `NiagaraStation` desde `com.tridium.fox.sys.NiagaraStation` (registro runtime de stations conectadas).
2. Obtener `BIRemoteAlarmRecipient` de su `AlarmDeviceExt`.
3. Invocar `remoteRecipient.routeAlarm(record)` — **síncrono**, Fox RPC.

**Gotcha G6 (Supervisor bottleneck aplica aquí)**:
- `BStationRecipient` NO es `BRecoverableRecipient` → **no tiene queue de retry**.
- Si la station remota está offline → `getStation()` throws Exception → alarm **se pierde** (solo se escribe al alarm db local, no llega al destino).
- Para aggregation real desde subordinados hacia Supervisor, usar el patrón inverso: el subordinado tiene `BStationRecipient` apuntando al Supervisor, y Supervisor recibe via su `AlarmService`.
- Con 50 subordinados (cap Bloque 13.1.7) + storm de alarmas → cada `handleAlarm` es síncrono bloqueante. El `alarmWorker` del subordinado se bloquea. Solución: usar Email o hacer recipient `BRecoverable` custom.

### 34.6.4 `BRecoverableRecipient` — persistent queue para delivery garantizado

Abstract class. Slots:
| Slot | Rol |
|---|---|
| `status` | Operacional/fault |
| `lastSendTime` / `lastAckSendTime` | Tracking |
| `lastFailureTime` / `lastFailureCause` | Error diag |
| `retryInterval` | `BRelTime` — cuánto esperar entre reintentos |
| `queuedAlarmCount` | int — tamaño cola current |
| `persistent` | boolean — si true, cola se serializa a disco |
| **Action** `clearAlarmQueue` | Manual flush |

Internal:
```java
private RetryThread retryThread;
Queue q;                              // in-memory
BOrd persistenceDirectory;            // disk dir (si persistent=true)
```

Métodos privados:
- `dequeueMemory()` — process cola RAM
- `dequeueDisk()` — recover cola tras crash/restart

Template method:
```java
protected abstract boolean sendAlarm(BAlarmRecord) throws Exception;
```

Subclases deben implementar solo `sendAlarm` — retorna `true` si éxito, `false` para reenqueue.

**Persistence path**: ~~`${protected.station.home}/alarm/recipients/{recipientName}/`~~ (inferido de permissions `alarm/-` en module.xml). **§14 CORREGIDO por [Block 666] §666.2** (código leído): la ruta real es el ORD `file:^^alarm/<name>AlarmQueue` → `<stationHome>/alarm/<recipientName>AlarmQueue/` (sin segmento `recipients/`; basename `<name>AlarmQueue`, no el nombre pelado). El permiso `alarm/-` la cubre igual, por eso la inferencia acertó el árbol pero no la hoja. Cada alarma pendiente = 1 archivo `<uuid>.xml` vía `ValueDocEncoder.encodeDocument`. Al reiniciar station, `started()` → `dequeueDisk()` → reintenta en orden timestamp.

**Gotcha G7**: `persistent=true` es **crítico** para email/SMS — sin ello, si la station reinicia con 100 emails pendientes, se pierden. Con `persistent=true`, al reiniciar se releen de `${protected.station.home}/alarm/recipients/` y se reenvían (pueden llegar delayed pero NO se pierden).

### 34.6.5 `BEmailRecipient` — SMTP delivery

`com.tridium.email.alarm.BEmailRecipient extends BAlarmRecipient implements BIUserAlarmRecipient`.

| Slot | Rol |
|---|---|
| `to` / `cc` / `bcc` | `BEmailAddressList` |
| `language` | String — para lexicon multi-idioma |
| `emailAccount` | String — nombre `BEmailAccount` en `EmailService` (referencia por nombre, no ORD) |
| `subject` | `BFormat` — substituciones `%alarmData.msgText%`, `%source%`, etc. |
| `body` | `BFormat` — template HTML/text |

**handleAlarm flow**:
1. Lookup `BOutgoingAccount` por nombre en `BEmailService.accounts`.
2. `account` expone `BEmailClientAuthenticator` → credentials.
3. **Credentials**: el `BOutgoingAccount` tiene un `BPassword` property. Este es cifrado con el keyring del station (Bloque 5.4 + 13.2).
4. En Windows: `.km`/`.kr` DPAPI-wrapped (NO `master.jceks` como asumía Bloque 13.2 v1, CORREGIDO ya).
5. Conectar SMTP (o OAuth2 — dep `oauth2-rt` en module.xml email-rt confirma soporte).
6. Renderizar subject+body con `BFormat` substitutions del `BAlarmRecord`.
7. Send. Si fail → (aunque `BEmailRecipient` **NO extiende `BRecoverableRecipient`**) solo actualiza `status` fault. **Gotcha G8**: contrario a lo que uno esperaría, `BEmailRecipient` NO tiene retry queue propia. Si falla SMTP, el email se **pierde**. Workaround: poner `BEmailRecipient` dentro de un pattern wrapper con `BRecoverableRecipient` custom, o aceptar que emails son "best effort".
   - **Corrección cross-bloque**: el blog/docAlarms dice "email is recoverable" — NO lo es en clase base. Lo que es recoverable es el **SMTP transport queue** dentro de `BEmailAccount` (retry attempts configurables). Pero si la station crashea mid-send, la alarma queda como "sent" en memoria y no se reintenta.

### 34.6.6 `BLinePrinterRecipient` + `BPrinterRecipient` (alarm-se)

Solo disponibles en profile `se` (server edition — Supervisor típicamente). Permissions `RuntimePermission queuePrintJob` (module.xml).

- `BLinePrinterRecipient`: envía texto ASCII a impresora line (parallel/serial). Legacy.
- `BPrinterRecipient`: envía vía Java AWT `PrinterJob` a impresora OS.

**Gotcha G9**: en Supervisor headless Linux sin CUPS configurado, `BPrinterRecipient` throws `PrinterException` silencioso. Stations en JACE (profile `rt`, no `se`) NO tienen acceso a estas clases — el module.xml alarm-se restringe a profile `se`.

### 34.6.7 SMS y OnCall (no extraídos pero confirmados en listing)

`sms-rt.jar` + `onCall-rt.jar` presentes en modules. Referencias en docAlarms.txt:
- `BSmsRecipient` — probable subclass `BRecoverableRecipient` (persistent queue).
- `BSmsAlarmAcknowledger` — lee incoming SMS, matchea UUID en mensaje, hace ack.
- `BOnCallService` + `BOnCallList` — rotación de recipients por schedule (on-call shifts).

Investigación detallada out of scope de Bloque 34. HIPÓTESIS confirmable extrayendo `sms-rt.jar`.

---

## 34.7 `BAlarmRecord` — formato, serialization, alarmData fields

### 34.7.1 Slots principales

`BAlarmRecord extends BStruct` (NO `BComponent` — es value type, no live en space).

| Slot | Tipo | Rol |
|---|---|---|
| `timestamp` | `BAbsTime` | Creation time |
| `uuid` | `BUuid` | Unique ID — se usa para ack/update |
| `sourceState` | `BSourceState` | Estado al momento |
| `ackState` | `BAckState` | Ack status |
| `ackRequired` | boolean | Copia snapshot de `BAlarmClass.ackRequired` (por si class cambia después) |
| `source` | `BOrdList` | ORD al alarm source (point) |
| `alarmClass` | String | Nombre de la class (NO ref directa — lookup lazy) |
| `priority` | int | Copia de `BAlarmPriorities.getPriority(sourceState)` |
| `normalTime` | `BAbsTime` | Si ya volvió a normal, cuándo |
| `ackTime` | `BAbsTime` | Si ya ack, cuándo |
| `user` | String | Username del ack-er |
| `alarmData` | `BFacets` | 34 fields configurables (ver abajo) |
| `alarmTransition` | `BSourceState` | Transición que creó ESTE record (puede diferir de sourceState actual) |
| `lastUpdate` | `BAbsTime` | Última modificación (para coalesce, ack, etc.) |

### 34.7.2 `alarmData` — 34 campos string-keyed

Del javap, constantes public estáticas:
```java
MSG_TEXT, FROM_STATE, TO_STATE, NOTIFY_TYPE, STATUS,
NEW_VALUE, SETPT_VALUE, SETPT_NUMERIC, ERROR_LIMIT, DEADBAND,
COUNT, HIGH_LIMIT, LOW_LIMIT, HIGH_DIFF_LIMIT, LOW_DIFF_LIMIT,
ALARM_VALUE, OFFNORMAL_VALUE, FAULT_VALUE, PRESENT_VALUE,
NUMERIC_VALUE, FEEDBACK_VALUE, FEEDBACK_NUMERIC, CONTROLLED_VALUE,
HYPERLINK_ORD, SOUND_FILE, ICON, SOURCE_NAME, NOTES,
INSTRUCTIONS, TIME_ZONE, TIME_DELAY, TIME_DELAY_TO_NORMAL
```

**32 constantes literales** (el javap listing las enumera). Cada una es key del `BFacets alarmData`. No todos se usan en cada alarma — sólo los relevantes al algoritmo.

Ejemplo para `BOutOfRangeAlgorithm` high alarm:
```
alarmData = {
  MSG_TEXT: "Zone 5 temperature above high limit",
  FROM_STATE: "normal",
  TO_STATE: "offnormal",
  HIGH_LIMIT: "85.0",
  ALARM_VALUE: "87.3",
  DEADBAND: "1.0",
  UNITS: "°F",
  SOURCE_NAME: "Floor1_Zone5_Temp"
}
```

**Extensibilidad**: `addAlarmFacet(String key, BIDataValue value)` permite **agregar campos custom** al record. Esto es cómo Honeywell/BACnet inyectan metadata adicional (e.g., `BDelayFilterState.FILTER_STATE_FACET_KEY`).

### 34.7.3 Serialization — método `write(DataOutput, Context)`

Del javap:
```java
public void write(DataOutput) throws IOException;
public void write(DataOutput, Context) throws IOException;
public void read(DataInput) throws IOException;
public void read(DataInput, Context) throws IOException;
private void encodeAbsTime(DataOutput, Context, BAbsTime);   // tz-aware
private BAbsTime decodeAbsTime(DataInput, Context);
```

- **Tz-aware**: `encodeAbsTime` toma `Context` que puede portar `TimeZone` — permite escribir timestamps en UTC o local según el consumer.
- **Contexts especiales**:
  - `DATA_RECOVERY_CX` — usado cuando escribe `.drr` (data recovery file post-crash).
  - `ALARM_STORE_CX` — usado cuando escribe `.adb` (con posiciones fixed paginadas).

`serialVersionId` private int — permite forward-compat leyendo records con schema versions anteriores.

### 34.7.4 `isFixedSize()` — variable-size records

```java
public boolean isFixedSize();   // retorna false
public int getRecordSize();      // calcula real al momento
```

**Records son variable size** — no hay paginación slot-por-slot como en `.hdb` fixed-size history. El alarm store (34.10) usa paginación multi-page span per record si es grande.

---

## 34.8 Fox alarm channels — `"alarmdb"` + `BAlarmConsoleChannel`

### 34.8.1 `"alarmdb"` FoxChannel

Registrado en `BAlarmService.serviceStarted()`:
```java
BFoxChannelRegistry.add("alarmdb", alarmDb);
```

Permite a **clientes Fox** (otras stations, Workbench) ejecutar:
- BQL queries remotos contra el alarm db (`local:|station:|slot:/Services/AlarmService/alarmDb | bql:select ...`)
- `BFoxAlarmDatabase` actúa como proxy remoto (ver 34.8.3).
- `BAlarmArchiveChannel` (subclass) expone el archive provider sobre Fox.

### 34.8.2 `BAlarmConsoleChannel` — live console updates

`com.tridium.alarm.BAlarmConsoleChannel extends BFoxChannel`.

Internal:
```java
BlockingQueue<FoxMessage> newQueue;         // nuevas alarmas a push
BlockingQueue<BAlarmRecord> ackQueue;       // acks a reflect
Map<BConsoleRecipient, Integer> registeredConsoleRecipients;  // ref-count
volatile Worker worker;                      // push thread
public static final Object CHANNEL_MUTEX;
```

**Flow**:
1. Workbench conecta → `registerAlarmHandler(BConsoleRecipient)` — incrementa refcount.
2. `BConsoleRecipient.handleAlarm()` → `accept(recipient, record)` → `newQueue.put(FoxMessage)`.
3. Worker loop → `newQueue.take()` → push via Fox al Workbench conectado.
4. Workbench ack → llega por `ackQueue` → `BAlarmService.doAckAlarm(record)`.
5. Desconecta → `unregisterAlarmHandler` — decrementa. Si llega a 0 → stop worker.

**Gotcha G10**: si 20 Workbench conectan simultáneamente al mismo `BConsoleRecipient`, el worker single-threaded serializa los pushes. Con alarm storm (100 alarmas/s) + 20 clientes → 2 000 Fox messages/s en un solo thread. Latency perceivable.

### 34.8.3 `BFoxAlarmDatabase` — proxy remoto

```java
class BFoxAlarmDatabase extends BAlarmDatabase
                       implements BIFoxProxySpace, Queryable, RemoteQueryable, NavListener {
  BFoxSession session;
  BPermissions permissions;
}
```

Usado desde Workbench cuando abrís AlarmDbView de una station remota. Todas las queries se proxan sobre Fox — NO hay cache local. Cada paginación en el UI = 1 Fox roundtrip.

---

## 34.9 `BFileAlarmDatabase` + `AlarmStore` — formato `.adb`

### 34.9.1 Jerarquía

```
BAlarmDatabase (abstract, alarm-rt, javax.baja.alarm)
├── BFileAlarmDatabase (com.tridium.alarm.db.file)   ← default, local .adb
├── BFoxAlarmDatabase (com.tridium.alarm.fox)         ← remote proxy
├── BOrionAlarmDatabase (javax.baja.alarmOrion)       ← RDB backend (34.13)
└── BAlarmArchive (abstract)
     ├── BFoxAlarmArchive (fox proxy to archive)
     ├── BOrionAlarmArchive (RDB archive)
     └── BOrionAlarmArchiveDatabase
```

### 34.9.2 `BFileAlarmDatabase` — fields

```java
private int capacity;                     // cap records (trim más viejos al exceder)
private File dbDir;                       // ${protected.station.home}/alarm/
private File alarmFile;                   // alarmFile = new File(dbDir, "???")
private AlarmStore alarmStore;            // paginated storage
private BIDataRecoveryService dataRecoveryService;  // ref a data recovery
```

El filename exacto no es literal en bytecode (se construye lambda-based). Del path de permissions en module.xml + convention, es `${protected.station.home}/alarm/alarm.db` (hipótesis bien fundada — ver también archive path `alarm/-` en permissions).

### 34.9.3 Nre:Engine thread check

Del javap -c BFileAlarmDatabase.getDbConnection:
```
57: ldc #24   // String "Nre:Engine"
65: new javax/baja/alarm/AlarmException
69: ldc #26   // "BFileAlarmDatabase.getDbConnection called from Nre:Engine Thread"
82: ldc #27   // "Potential Performance Degradation"
```

**Si un caller invoca `getDbConnection()` desde el thread `Nre:Engine` → throw `AlarmException`** (o warn según versión). Esto previene que una action handler en el engine thread bloquee por I/O disk.

**Gotcha G11 (workflow)**: componentes custom que quieren query alarm db deben NO hacerlo en `changed()` o action handlers. Usar worker:
```java
// MAL — throws AlarmException
BAlarmDatabase db = BAlarmService.getService().getAlarmDb();
AlarmDbConnection conn = db.getDbConnection(ctx);

// BIEN — delegar a worker
BWorker.post(() -> {
  AlarmDbConnection conn = db.getDbConnection(ctx);
  // ... query
});
```

### 34.9.4 `AlarmStore` — paginated binary storage

Inner fields:
```java
private static final int BLOCK_CACHE_SIZE;         // private constant
private static final int MAX_SIZE;                 // cap total
private static final int MAX_SIZE_PER_SOURCE;      // cap per-source
private final File alarmFile;
private RandomAccessFile io;
private int capacity;
private AlarmStoreHeader header;
private FreePageMap freeMap;                       // bit-map de páginas libres
private BlockCache blockCache;                     // cache de Block en RAM
private Map<BUuid, IndexEntry> byUuid;             // index uuid → posición
private TimestampIndex timestampIndex;             // sorted tree para time queries
private AlarmSourceIndex sourceIndex;              // por source ORD
private OpenIndex openIndex;                       // solo open alarms
private AckPendingIndex ackPendingIndex;           // solo ACK_PENDING
private Map<BOrdList, AlarmCount> currentCounts;   // count por source
```

**4 indices separados** (`timestampIndex`, `sourceIndex`, `openIndex`, `ackPendingIndex`) — permiten queries O(log n) por cada uno. Los indices son in-memory (SkipList class presente), pero el data es on-disk.

### 34.9.5 Operations

```java
public synchronized void append(BAlarmRecord) throws IOException;
public synchronized void update(BAlarmRecord) throws IOException;
public synchronized int getRecordCount();
public synchronized int getOpenCount(BOrdList source);
public synchronized Cursor<BAlarmRecord> scan();
public synchronized Cursor<BAlarmRecord> timeQuery(BAbsTime from, BAbsTime to);
public synchronized Cursor<BAlarmRecord> getAlarmsForSource(BOrdList);
public synchronized Cursor<BAlarmRecord> getOpenAlarms();
public synchronized void trimToCapacity() throws IOException;
public synchronized void clearAllRecords(Context) throws IOException;
public synchronized void clearOldRecords(BAbsTime cutoff, Context) throws IOException;
public synchronized void clearRecord(BUuid, Context) throws IOException;
```

**TODOS los methods son `synchronized`** sobre `this`. Esto significa:
- Múltiples queries paralelas se serializan.
- Un `append` bloquea un `timeQuery` y viceversa.
- Un `trimToCapacity` bloquea todo durante la trim (puede ser lento si capacity shrink de 10k → 1k).

**Gotcha G12**: setting `capacity` desde 10 000 → 1 000 dispara `trimToCapacity()` — potentially 30+ seconds de lock en stations grandes. Hacer en ventana mantenimiento.

### 34.9.6 `hasDeletePermissions(Context)` — security check

En `clearRecord`, `clearAllRecords`, `clearOldRecords`, el store verifica que el context tenga permission `alarm:delete` ANTES de borrar. Esto previene delete accidental via BQL update queries.

### 34.9.7 `analyze(PrintStream)` — diagnostic

```java
public void analyze(java.io.PrintStream);
```

Método para dump del store — lista pages, record positions, free list, index counts. **Accesible vía Niagara Spy Pages** (ver Bloque 29 web tier + spy). URL típica: `http://station/ord?BFileAlarmDatabase/spy`.

---

## 34.10 `.adb` format — header + pages + blocks

### 34.10.1 Header layout (reconstruido)

```
Offset  Size  Field              Valor
0       4     MAGIC              0x6010ACCD (1611526157)
4       4     version            current (1?)
8       4     recordVersion      schema version BAlarmRecord
12      8     dataOffset         (offset absoluto donde comienzan pages)
20      4     pageSize           (DEFAULT_PAGE_SIZE — privado, típico 512/1024/4096)
24      4     pagesPerBlock      (DEFAULT_PAGES_PER_BLOCK — típico 16 o 32)
28      8     creationTime       millis
36+     ...   reserved/padding   hasta dataOffset
```

Si MAGIC != `0x6010ACCD` en read → throw `"Invalid or corrupt alarm database."` (literal confirmado).
Si version no reconocida → throw `"Unrecognized alarm db version: <n>"`.

### 34.10.2 Block + Page

```java
class Block {
  AlarmStore store;
  int index;
  byte[] data;                   // raw block bytes
  Page[] pages;
  boolean dirty;                 // needs flush
  Block prev, next;              // linked list (cache LRU)
}

class Page {
  public static final int HEADER_SIZE;    // private constant
  Block block;
  int pageInBlock;
  int size;
  int recPage;                    // índice de page al inicio de este record (para records multi-page)
  int recPages;                   // cuántas pages spans este record
  int nextPage;                   // para variable-size: next page en el record
}
```

- Un record puede ocupar múltiples pages (linked list via `nextPage`).
- `recPage` apunta al first page del record.
- `pagesPerBlock` agrupa pages en blocks para minimizar syscalls (read/write block = 1 syscall).

### 34.10.3 `FreePageMap` + `BlockCache`

- `FreePageMap` — bitmap persistido. `takePages(n)` busca run de `n` pages contiguas.
- `BlockCache` — LRU dentro de `BLOCK_CACHE_SIZE` entries. Eviction escribe dirty blocks.

### 34.10.4 Fragmentation

**Gotcha G13**: al trim con `clearOldRecords` + `clearRecord`, las páginas vuelven al `FreePageMap` pero el **archivo no shrinks**. Mismo comportamiento que `.hdb` (Bloque 33.0). Para reducir tamaño real:
1. Export records desired a nuevo .adb (via BQL + import).
2. Stop station.
3. Replace alarm.db con export.
4. Start station.
5. Re-build indices automático en `doOpen()`.

No hay un `VACUUM ALARM` builtin.

### 34.10.5 Data recovery

`BFileAlarmDatabase implements BIDataRecoverySource`:
```java
public void dataRecoveryRestoreComplete();
public boolean dataRecoveryRestore(IDataRecoveryRecord) throws Exception;
public void dataRecoverySpy(SpyWriter, Iterator<IDataRecoveryRecord>);
```

Paired `DataRecoveryAlarmEvent` inner classes:
- `Append` / `Update` / `ClearAll` / `ClearOld` / `ClearRecord` / `CapacityChange`

Flow:
1. Cada mutation al alarm store escribe un event al `.drr` (data recovery journal) ANTES de mutar el `.adb`.
2. Si station crashea mid-write → al restart, `BIDataRecoveryService` lee `.drr` pending events → replays vía `dataRecoveryRestore()`.
3. Evita alarm loss si power-cut durante un `append`.

**Gotcha G14**: si `.drr` file corrupt o disk full → station arranca pero `AlarmService` entra en fault. Log: `"DataRecoveryService in fault, could not append alarm event '<event>'. Stopped data recovery alarm recording."` (literal). Workaround: backup+remove `.drr`, restart. Alarms durante crash window se pierden.

---

## 34.11 BACnet alarm router — `bacnetAlarmRouter-rt`

### 34.11.1 Componentes

De module.xml:
| Tipo | Rol |
|---|---|
| `BBacnetAlarmClassReassigner` | Re-map BACnet NotificationClass → Niagara BAlarmClass |
| `BCustomBacnetEventProcessor` | Extiende `BBacnetEventProcessor` — intercepta alarm events antes de route |
| `BNCAlarmClassReassign` | NC-level reassignment (per NotificationClass) |
| `BAlarmFilter` | Filter con `divert` option — puede enrutar a otra class |
| `BEscalationFilter` | Filter per escalation level (solo deja pasar si allowedLevel match) |
| `BAbstractAlarmFilter` | Base abstract |
| `BRoundRobinCollector` | Unrelated — trend collector batching |
| `BCollectionWorker` + `BCollectorBatching` + `BCollectorMetrics` | Trend collection support |

### 34.11.2 `BBacnetAlarmClassReassigner` — properties clave

```java
boolean enabled;
String fallbackAlarmClass;
boolean useBacnetAlarmPriority;           // usar prio BACnet o override?
boolean setAlarmHyperlink;
boolean replaceSourcenameWithDisplayname;
boolean filterAlarmsFromUnknownObjects;   // drop si object ID no existe en cache
int numberDiscarded;
boolean filterAlarmsOnError;
boolean disableStrictObjectIdCheck;
BBacnetPointDeviceExt points;             // referencia al points device ext
BBacnetAlarmDeviceExt alarms;             // referencia al alarms device ext
```

**Flow**:
1. BACnet intrinsic alarm event llega (ConfirmedEventNotification / UnconfirmedEventNotification).
2. `BBacnetAlarmDeviceExt` (de bacnet-rt) crea `BAlarmRecord` con `alarmClass = "<BACnet default>"`.
3. Si `BCustomBacnetEventProcessor` está wired → intercepta → delega a `BBacnetAlarmClassReassigner.routeAlarm(record)`.
4. Reassigner lookup BACnet `NotificationClass` object → maps via algún dict/config a Niagara `BAlarmClass` name.
5. Modifica `record.alarmClass = newName`.
6. Si `filterAlarmsFromUnknownObjects=true` y el object ID no está en el `points` cache → `numberDiscarded++` y drop.
7. Si `setAlarmHyperlink` → inyecta `HYPERLINK_ORD` en `alarmData`.
8. `submitToAlarmService(record, class)` → `BAlarmService.fireAlarm(record)`.

### 34.11.3 Use cases

- Supervisor recibe alarmas de 50 subordinados, cada uno con su `AlarmClass "Default"`. Reassigner re-clasifica por subordinado: `subordinate1 → "SiteA_Critical"`, `subordinate2 → "SiteB_Normal"`.
- Priority re-mapping: BACnet priority 100 → Niagara priority 1 (invertir scale).

### 34.11.4 `BAlarmFilter` + `BEscalationFilter`

Composable con link:
```
AlarmClass → topic 'alarm' → AlarmFilter.routeAlarm
AlarmFilter → (if divert) → another_class
AlarmFilter → (else) → normal recipient
```

`BEscalationFilter`:
```java
int allowedLevel;   // 1,2,3
BString myLevel;    // calculado de record metadata
private static final String[] LEVELS;
```

Si `record.level != allowedLevel` → discard. Permite separar recipients por level más granular que el mecanismo nativo.

---

## 34.12 Honeywell alarm ext — `honAlarmExt-rt`

### 34.12.1 `BHonAlarmClass` — buffered delay

```java
public class BHonAlarmClass extends BAlarmClass {
  public static final Property enableAlarmDelay;
  public static final Property delayTime;
  public static final Property sendDelayBufferOnShutdown;
  public static final Action checkBuffer;

  private final LinkedList<AlarmRecordContainer> _buffer;
  private BHonAlarmClass$MyThread _myThread;
  private boolean _shutdown;
}
```

**Override de `doRouteAlarm`**:
1. Si `!enableAlarmDelay` → super.doRouteAlarm (comportamiento Niagara estándar).
2. Si `enableAlarmDelay`:
   a. Add record al `_buffer` (LinkedList).
   b. Start `_myThread` si no existe.
   c. Thread espera `delayTime`.
   d. Al wake up → invoca `doCheckBuffer()` → para cada record en buffer:
      - Si `removeFromBufferIfRecStateIsNormal()` → drop (nunca envió, auto-resolvió).
      - Else → `forwardRecord(record)` → super.doRouteAlarm(record).

**Use case Honeywell**: alarmas oscilantes (sensor flappeante). Si alarma + normal en <5s (delay) → NO se envía al operador. Reduce nuisance.

**Slot `sendDelayBufferOnShutdown`**: si `true`, al `stopped()` → flush buffer antes de matar thread (sino se pierden). Default depends on version — verificar por config.

**Gotcha G15 (shutdown race)**:
- Thread `MyThread` tiene flag `_shutdown` private.
- `stopped()` set `_shutdown=true`, thread exit loop, flush si flag set.
- Si station crasha hard → thread killed inline → buffer **se pierde** (NO hay persistence disk).
- Diferente de `BRecoverableRecipient` donde `persistent=true` escribe a disco.

**Gotcha G16 (incompatible con coalesce)**: ver G3 arriba.

### 34.12.2 `BHonConsoleRecipient`

Extiende `BConsoleRecipient`, **misma lógica de buffer + delay** que `BHonAlarmClass` pero a nivel recipient. Permite delay selectivo por destino (e.g., "retrasar notificaciones al console en off-shift").

### 34.12.3 `BDelayFilterState` — enum 4-valor

```java
public final class BDelayFilterState extends BFrozenEnum {
  public static final int UNKNOWN;
  public static final int DELAYED;
  public static final int SENT;
  public static final int IGNORED;
  public static final String FILTER_STATE_FACET_KEY;   // se inyecta en alarmData
}
```

Se agrega como `alarmData.filterState = "DELAYED"` (o `SENT`/`IGNORED`) durante el buffer. Operador puede ver en workbench si una alarma venía delayed. `IGNORED` = auto-resolved antes de delay.

### 34.12.4 `BHonAlarmConsoleRpc` (honAlarmConsole-rt)

```java
public class BHonAlarmConsoleRpc extends BObject {
  public static boolean checkFeatureLicense(Context);
  public static Feature checkHonAlarmConsoleFeature();
  public static boolean checkEdgeController(Context);
  public static String getBrandFromLicenseFile();
  public static JSONObject getMultiSourceSummary(Map, Context);
  public static JSONObject getSingleSourceSummary(Map, Context);
}
```

RPC endpoint para la UI web custom Honeywell. Retorna `JSONObject` (Jayway JsonPath / json-smart dependencies — ver Bloque 32 json stack). Consume `BAlarmRecord` list + `BAlarmRecipient` → serializa a BoxString JSON.

`checkEdgeController` — hay un gate: la console solo funciona si la station NO es Edge controller (feature licensing Honeywell-specific).

---

## 34.13 `alarmOrion-rt` — RDB alarm database

### 34.13.1 Contexto

Hay **dos backends** para alarm db:
1. **File** (default, Bloque 34.9): `.adb` binario paginado local.
2. **Orion**: RDB (MS-SQL via jTDS) remoto, compartido entre stations.

Se usa Orion cuando:
- Muchos sites, queremos consolidar alarmas en un DB central (BI, reporting).
- `BAlarmArchive` con Orion backend para "alarmas archivadas > N días".
- Hipótesis/compliance: alarmas deben vivir en DB corporativo (SOX, FDA 21 CFR 11).

### 34.13.2 `BOrionAlarmService extends BAlarmService`

```java
public class BOrionAlarmService extends BAlarmService implements BIOrionApp {
  BStatus status;
  String faultCause;
  BOrd database;                  // ORD al Orion DB (rdbmsNetwork)
  OrionAppSchemaManager schemaManager;
  BSchemaVersion VERSION;
  OrionType[] ORION_TYPES;
}
```

Override `createAlarmDb()` → retorna `BOrionAlarmDatabase` en vez de `BFileAlarmDatabase`. Resto del routing pipeline es idéntico — porque `BAlarmDatabase` es abstracto.

### 34.13.3 `BOrionAlarmRecord extends BOrionObject`

(NO extiende `BAlarmRecord` — es struct paralelo, mapped a tabla SQL).

| Property | Tipo |
|---|---|
| `id` | int (PK) |
| `timestamp` | `BAbsTime` |
| `datestamp` | `BDate` (para index by date en SQL) |
| `uuidHash` | int (hash index) |
| `uuid` | `BUuid` |
| `isOpen` | boolean (index) |
| `sourceState` | `BSourceState` |
| `ackState` | `BAckState` |
| `ackRequired` | boolean |
| `alarmClass` | `BRef` (FK a OrionAlarmClass) |
| `priority` | int |
| `normalTime` / `ackTime` | `BAbsTime` |
| `userAccount` | String |
| `alarmTransition` | `BSourceState` |
| `lastUpdate` | `BAbsTime` |

**No tiene `alarmData` (BFacets) expuesto como property** — Orion serializa en tabla secundaria via `BOrionAlarmFacetName` + `BOrionAlarmFacetValue` (EAV pattern).

### 34.13.4 Transaction stats

```java
class BOrionAlarmTransactionStatistics extends BComponent {
  int queueSize;
  double processRate;
  double peekProcessRate;
  double enqueueRate;
  double peekEnqueueRate;
  int alarmCount;
  Action update;
  Action transactionEnqueued;
  Action transactionProcessed;
  Action reset;
}
```

Visibilidad de throughput. `processRate` en alarms/s. `peekProcessRate` max.

### 34.13.5 `BOrionAlarmArchive` — hybrid setup

Workflow recomendado (docAlarms Ch.2.4):
- **Local .adb**: solo open alarms.
- **Remote Orion**: cleared alarms (historical/audit).
- `BArchiveAlarmProvider.executionTime` (cron) → mueve cleared alarms de local .adb → Orion.
- `clearedAlarmLingerTime` (BRelTime) → cuánto esperan en local antes de archive (típico 24h).

Flujo del archive provider:
```java
Action execute;       // trigger manual/cron
Action retry;         // si falló última
Action ackAlarm;      // ack de alarmas YA en archive
```

### 34.13.6 Gotcha G17 — jTDS + `extraConnectionProperties`

De docAlarms.txt:9 (linea 1060-1064):
> "The OrionAlarmService checks the SQL server database `prepareSQL` property at startup... Setting this property to 1 may result in increased performance. However, setting this property to 1 when using the jTDS driver may cause this warning."

Warning: `[alarm.database] Database connection misconfigured`. Ignorable — performance improvement vale la warning.

### 34.13.7 Migration Orion → File

Legacy: algunos setups tienen `OrionArchiveAlarmProvider` único (todas las alarmas a Orion, nada local). Recomendación N4.11+:
- Add local `BFileAlarmDatabase`.
- Import open alarms via `Actions > Import Open Alarms` en `OrionArchiveAlarmProvider`.
- Keep Orion solo para cleared.

---

## 34.14 BACnet intrinsic + algorithmic upstream (cross-ref Bloque 23.7)

Relación con Bloque 23 (BACnet driver):

| BACnet concept | Niagara equivalent |
|---|---|
| Intrinsic reporting (dentro del object) | `BBacnetPoint` con `BBacnetAlarmSourceExt` auto-wired |
| Algorithmic reporting (Event Enrollment obj) | `BBacnetEventEnrollment` — proxy object |
| NotificationClass obj | Mapped a `BAlarmClass` via name matching (NO auto) |
| ConfirmedEventNotification | Triggers `BBacnetEventProcessor.routeAlarm` |
| UnconfirmedEventNotification | Misma ruta — NO requiere ack BACnet, sí Niagara |
| COV subscription | Separate path — NO pasa por alarm system |
| GetEventInformation service | Polling fallback si comms flapping |
| AckAlarm service (BACnet) | Routed back via `BStationRecipient` + remote ack |

**Clave**: bacnet-rt crea `BAlarmRecord` a partir de BACnet notification, pero el recipient workflow es idéntico al de cualquier otra alarma Niagara. El `bacnetAlarmRouter-rt` interviene solo si está wired explícitamente (34.11).

---

## 34.15 Alarm Acknowledger — `BAlarmAcknowledger` abstract

`com.tridium.alarm.ack.BAlarmAcknowledger extends BComponent implements BIStatus`.

| Slot | Rol |
|---|---|
| `status` | `BStatus` |
| `enabled` | boolean |
| `ackAlarmsFromSameSource` | boolean — si se recibe un ack de UUID X, ackear también todas las otras alarms del mismo source |
| `lastAlarmAcked` | String (UUID) |
| `lastAlarmAckedTime` | `BAbsTime` |
| `lastAlarmAckedFailureTime` | `BAbsTime` |
| `lastAlarmAckedFailureCause` | String |
| `totalAlarmsAckedToday` | int |
| `totalAlarmAckedFailures` | int |
| `totalMessagesReceivedToday` | int |
| **Action** `resetTotals` | Reset daily counters |
| **Topic** `alarmAcked` | Fire al ack exitoso |

Constants:
```java
protected static final String UUID_TEXT;    // prefix para parse UUID de email/sms body
protected static final int UUID_LENGTH;     // UUID_LENGTH = 36 (uuid string)
private static Worker worker;               // shared worker across all acknowledgers
private static int compRefCount;            // ref counting
```

Subclases:
- `BEmailAlarmAcknowledger` — monitorea inbox, parsea `UUID: xxx-yyy-...` del body.
- `BSmsAlarmAcknowledger` — monitorea SMS inbox.
- `BHonAlarmAcknowledger` — posible custom, not confirmed in this JAR.

**Gotcha G18**: `ackAlarmsFromSameSource=true` puede ser peligroso — un operator que ack 1 alarma de un source con 100 alarmas abiertas las ack TODAS. Reconciliar con compliance (audit trail pierde granularity).

**`compRefCount` + shared Worker**: todos los acknowledgers comparten 1 worker thread static. Si 10 acknowledgers configurados, serializados. Throughput: ~1 ack/s máximo por `sendAlarm` call (tipicamente I/O bound por email IMAP/POP).

---

## 34.16 Gotchas operacionales — consolidado

| # | Gotcha | Contexto | Mitigación |
|---|---|---|---|
| **G1** | `alarmQueue` unbounded → OOM | `BAlarmService` + alarm storm | Monitor queue size via Spy; cap recipients inline; evitar `BStationRecipient` hacia destinos flaky |
| **G2** | Escalation require wiring explícito por level | `BAlarmClass.escalatedAlarmN` topics | Wire `escalatedAlarm1/2/3` a distintos recipients |
| **G3** | `coalesceAlarms` + `HonAlarmClass.enableAlarmDelay` = double-loss | Ambos filtran alarmas rápidas | Usar uno u otro |
| **G4** | "Unack normal" edge case — record open aunque ya normal | Transition to-normal antes de ack | Entrenar operators; o set `ackRequired.isToNormal=false` |
| **G5** | Confundir `ackRequired` del class con `transitions` del recipient | Slots con nombre similar | Revisar ambos; set `routeAcks=true` para recibir acks |
| **G6** | `BStationRecipient` NO es recoverable → alarm loss si remote offline | No extends `BRecoverableRecipient` | Usar email o recipient custom con persist |
| **G7** | `BRecoverableRecipient.persistent=false` → alarm loss on restart | Email/SMS config default | Set `persistent=true` SIEMPRE en prod |
| **G8** | `BEmailRecipient` NO tiene retry queue propia | Solo retry SMTP transport-level | Wrapper custom o aceptar best-effort |
| **G9** | `BPrinterRecipient` silent fail en headless Linux | Requires CUPS + AWT printable | Verificar infra; stations JACE (rt) NO tienen alarm-se |
| **G10** | `BAlarmConsoleChannel` worker single-threaded | 20 Workbench + storm = lag | Escalar con múltiples `BConsoleRecipient` |
| **G11** | `getDbConnection()` from Nre:Engine thread → throws | Action handlers inline | Delegar a `BWorker` |
| **G12** | `capacity` shrink dispara `trimToCapacity()` — 30s+ lock | Synchronized on AlarmStore | Cambiar en ventana mantenimiento |
| **G13** | `.adb` NO shrinks con delete | Pages marked free, file size stays | Export+replace para reducir |
| **G14** | `.drr` corrupt → AlarmService fault | Data recovery journal | Backup+remove `.drr`, restart |
| **G15** | `BHonAlarmClass` buffer NO persistent → loss on hard crash | In-memory LinkedList only | Set `sendDelayBufferOnShutdown=true` mitigates soft shutdowns |
| **G16** | `coalesce` + `HonAlarmDelay` — ver G3 | | Uno u otro |
| **G17** | jTDS `prepareSQL=1` warning (Orion) | "Database connection misconfigured" | Ignorable, perf improvement |
| **G18** | `ackAlarmsFromSameSource=true` = bulk ack | Audit trail loses granularity | Usar solo si compliance lo permite |
| **G19** | DST edge case — `ackTime`/`timestamp` TZ-aware | Station TZ vs UTC | Almacenar UTC, render local; ver Bloque 24.14 |
| **G20** | `BHonAlarmConsoleRpc.checkEdgeController` gate | License feature | Verificar license antes de desplegar en JACE edge |
| **G21** | Email credentials en keyring DPAPI `.km/.kr` NO `master.jceks` | Windows-specific encryption | Confirmado Bloque 13.2 — NO portar .km entre boxes |
| **G22** | Supervisor alarm aggregation bottleneck | ~50 subordinados (Bloque 13.1.7) | NO usar BStationRecipient desde Supervisor hacia subordinados; patrón inverso |
| **G23** | Shared `compRefCount` Worker en acknowledgers | Single thread, 1 ack/s max | Escalar con múltiples StationRecipients en vez de múltiples acknowledgers |
| **G24** | `AlarmStore.analyze()` accessible via Spy Pages | Security: info leak | Restringir spy pages a admin users (ver Bloque 29 web tier) |
| **G25** | `BArchiveAlarmProvider.execute` blocking — largos batches | Sync SQL insert contra Orion | Schedule fuera de peak hours |
| **G26** | `BAlarmSchema` immutable post-freeze | `addColumn` throws `IllegalStateException` | Diseñar schema upfront; Orion permite más flex via BAlarmFacetName |
| **G27** | `MAX_SIZE_PER_SOURCE` limit en AlarmStore | Private constant, no config | Un point "ruidoso" tope alarmas → nuevas descartadas; cleanup manual |

---

## 34.17 Flow diagram — end-to-end trace

Escenario: temperatura zona 5 sube por encima del highLimit.

```
T=0s  BNumericWritable.out = 87.3°F (prev 79°F, highLimit=85°F)
      ▼
T=0s  BControlPoint propaga status a children
      ▼
T=0s  BOutOfRangeAlarmExt.changed(status, ctx)
      ├── Algorithm: BOutOfRangeAlgorithm
      ├── currentState=NormalState
      ├── evaluate(87.3 > 85) → transition to ValidateHighAlarmState
      └── schedule Clock.schedule(timerExpired, timeDelay=10s)
      ▼
T=10s BOutOfRangeAlgorithm.timerExpired (timer fires)
      ├── re-evaluate: still 87.3 > 85 → transition to HighAlarmState
      ├── AlarmSupport.newOffnormalAlarm(facets) → BAlarmRecord created
      │    record.timestamp = T=10s
      │    record.sourceState = OFFNORMAL
      │    record.ackState = UNACKED
      │    record.alarmTransition = OFFNORMAL
      │    record.priority = alarmClass.priority.toOffnormal = 100
      │    record.alarmData.HIGH_LIMIT = "85.0"
      │    record.alarmData.ALARM_VALUE = "87.3"
      │    record.alarmData.MSG_TEXT = "Zone 5 high temp"
      │    record.uuid = BUuid.random()
      ├── BAlarmService.fireAlarm(record)
      └── alarmQueue.put(record) [unbounded]
      ▼
T=10s alarmWorker thread picks record
      ├── doRouteAlarm(record)
      ├── alarmDb.append(record) → AlarmStore.append
      │    ├── serializes record to writeBuf
      │    ├── takePages(N) from FreePageMap
      │    ├── writes pages via Block.write
      │    └── updates 4 indices (timestamp, source, open, byUuid)
      ├── .drr journal: Append event written (durability)
      ├── lookupAlarmClass("HVAC_Critical") → BAlarmClass instance
      └── BAlarmClass.doRouteAlarm(record)
      ▼
T=10s BAlarmClass fires topic 'alarm'
      ├── Recipient_1: BConsoleRecipient (linked)
      │    ├── accept(record) → yes (transitions, timeRange, etc.)
      │    ├── handleAlarm(record)
      │    ├── alarmHandlers callbacks (TrayIcon fires sound)
      │    ├── fire topic 'newUnackedAlarm'
      │    └── BAlarmConsoleChannel.newQueue.put → 20 WBs get push
      ├── Recipient_2: BEmailRecipient (linked)
      │    ├── accept(record) → yes
      │    ├── handleAlarm(record)
      │    ├── lookup emailAccount "smtp_main"
      │    ├── BOutgoingAccount.getPassword() → DPAPI unwrap .km
      │    ├── SMTP connect (port 587 STARTTLS)
      │    ├── subject+body BFormat substitute
      │    └── send() — success or log fault
      └── Recipient_3: BStationRecipient (linked, remote="SupervisorMain")
           ├── accept(record) → yes
           ├── handleAlarm(record)
           ├── resolve NiagaraStation("SupervisorMain")
           ├── getAlarmDeviceExt() → BIRemoteAlarmRecipient
           └── remoteRecipient.routeAlarm(record) — Fox sync RPC
                (if SupervisorMain offline → Exception → alarm DISCARDED G6)

--- (time passes, operator notices console) ---

T=45s Operator clicks "Ack" on alarm in Workbench
      ├── WB → AlarmConsoleChannel.ackQueue.put(record)
      ├── BAlarmService.ackAlarm(record)
      ├── record.ackState = ACKED
      ├── record.ackTime = T=45s
      ├── record.user = "operator1"
      ├── alarmDb.update(record)
      └── doRouteToSource(record) → source alarm ext receives ack echo

--- (time passes, temp drops) ---

T=180s  T = 82°F (below highLimit-deadband=84°F for 10s+ timeDelayToNormal)
      ├── algorithm transitions to NormalState
      ├── AlarmSupport.toNormal(ctx) → queue ToNormalTransition
      ├── alarmWorker processes ToNormalTransition.run()
      ├── BAlarmClass.doRouteAlarm(normalRecord) — NEW record
      │    normalRecord.sourceState = NORMAL
      │    normalRecord.ackState = UNACKED (unless ackRequired.toNormal=false)
      │    normalRecord.alarmTransition = NORMAL
      │    normalRecord.uuid = NEW
      └── recipients re-process (filtered by transitions.toNormal)

--- (operator acks the "to normal") ---

T=200s Operator acks normal → (NORMAL, ACKED) → CLEARED → disappears from console
       (record stays in alarm db until clearOldRecords or trim)

--- (eventually, archive executes) ---

T=24h  BArchiveAlarmProvider.execute (scheduled)
      ├── Query open alarms: still open? → skip
      ├── Query cleared alarms with age > clearedAlarmLingerTime → batch
      ├── For each: insert into BOrionAlarmDatabase (remote SQL)
      └── delete from local .adb via clearRecord(uuid)
```

---

## 34.18 Inventario consolidado BComponents

### 34.18.1 `alarm-rt` (rt profile)

Total ~50 types. Claves:

| Clase | Rol |
|---|---|
| `BAlarmService` | Service |
| `BAlarmClass` + `BAlarmClassFolder` | Classification |
| `BAlarmRecord` | Struct |
| `BAlarmRecipient` (abstract) | Recipient base |
| `BConsoleRecipient` / `BStationRecipient` | Recipients |
| `BRecoverableRecipient` | Recoverable base |
| `BAlarmSource` + `BIAlarmSource` | Source |
| `BAlarmSourceExt` + 14 algorithms | Point extension |
| `BAlarmSchema` + `BAlarmDbConfig` | Schema/config |
| `BFileAlarmDatabase` + `BFileAlarmDbConfig` | File backend |
| `BFoxAlarmDatabase` + channels | Remote proxy |
| `BAlarmArchive` + `BArchiveAlarmProvider` | Archive base |
| `BAlarmInstructions` + `BAlarmPriorities` + `BAlarmTransitionBits` | Simple types |
| `BSourceState` + `BAckState` (+`BAlarmState` en ext) | Enums |
| `BAlarmAcknowledger` (abstract) | Ack base |

### 34.18.2 `alarm-wb` (workbench profile)

| Clase | Rol |
|---|---|
| `BAlarmConsole` + `BAlarmUxConsole` + `BHxAlarmConsole` | Consolas (swing, bajaux, hx) |
| `BAlarmDbView` + `BAlarmDbMaintenance` | DB views |
| `BAlarmClassSummary` + `BAlarmInstructionsManager` | Management views |
| `BAlarmExtManager` | Bulk management alarmExt |
| `BAlarmDetailsDialog` + `BNotesDialog` + `BAlarmDialog` + `BAlarmReportDialog` | Dialogs |
| `BTrayIcon` | Tray notifier |
| `BAlarmConsoleOptions` + `BAlarmDataCols` | Options |
| `BAlarmClassDef` + `BAlarmClassMapping` | Mapping |
| `BPortalAlarmConsole` + `BAlarmPortal` + `BAlarmPortalTool` | Web portal |
| `BPdfAlarmDbView` + `BAlarmConsoleToPdf` | PDF export |

### 34.18.3 `alarm-ux` (ux profile)

| Clase | Rol |
|---|---|
| `BAlarmJsBuild` + `BAlarmCssResource` | JS/CSS resources |
| `BDatabaseView` + `BDatabaseMaintenance` | UX views |
| `BAlarmAlgorithmTypeExt` (+ subclasses per algorithm) | Algorithm editors |
| `BConsoleRecipientTypeExt` + `BAlarmInstructionsTypeExt` + ... | TypeExts UI |
| `BAlarmClassEditor` + `BAlarmInstructionsEditor` + `BAlarmPrioritiesEditor` + `BAlarmTransitionBitsEditor` | FE editors |
| `BAlarmUxConsole` | UX console (bajaux) |

### 34.18.4 `alarm-se` (server edition profile)

| Clase | Rol |
|---|---|
| `BLinePrinterRecipient` + `BPrinterRecipient` | Printer recipients |
| `BLinePrinterFE` + `BPrinterFE` | UI editors |

### 34.18.5 `alarmOrion-rt`

| Clase | Rol |
|---|---|
| `BOrionAlarmService` | Service override |
| `BOrionAlarmDatabase` | RDB DB |
| `BOrionAlarmRecord` + `BOrionAlarmFacetName`/`Value` + `BOrionAlarmSource`/`Order` + `BOrionAlarmClass` | Orion objects |
| `BOrionAlarmTransactionStatistics` | Perf stats |
| `BAlarmConversion` + `BAlarmConversionJob` (+ `RdbAlarmConversion*` + `FileAlarmConversion*`) | Migration |
| `BOrionAlarmNameFactory` (agent) | Name generation |
| `BOrionAlarmArchive` + `BOrionArchiveAlarmProvider` + `BOrionAlarmArchiveDatabase` | Archive |

### 34.18.6 `honAlarmExt-rt`

| Clase | Rol |
|---|---|
| `BHonAlarmClass` | AlarmClass con buffer delay |
| `BHonConsoleRecipient` | ConsoleRecipient con buffer delay |
| `BDelayFilterState` | Enum delay state (4 valores) |

### 34.18.7 `honAlarmConsole-rt` + `-ux`

| Clase | Rol |
|---|---|
| `BHonAlarmConsoleRpc` | Backend RPC |
| `BHonAlarmConsole` (ux) | UI bajaux |
| `BHonAlarmConsoleBuiltJS` (ux) | JS bundle |

### 34.18.8 `bacnetAlarmRouter-rt`

| Clase | Rol |
|---|---|
| `BBacnetAlarmClassReassigner` | Re-class BACnet→Niagara |
| `BCustomBacnetEventProcessor` | Event processor override |
| `BNCAlarmClassReassign` | NC-level |
| `BAlarmFilter` + `BEscalationFilter` + `BAbstractAlarmFilter` | Composable filters |
| `BRoundRobinCollector` + `BCollectionWorker` + `BCollectorBatching` + `BCollectorMetrics` | Trend collection (unrelated) |

### 34.18.9 `email-rt` (alarm-relevant subset)

| Clase | Rol |
|---|---|
| `BEmailService` | Service |
| `BEmailAccount` + `BIncomingAccount` + `BOutgoingAccount` | Accounts |
| `BEmailRecipient` | Alarm recipient |
| `BEmailAlarmAcknowledger` | Inbox → ack |
| `BEmailClientAuthenticator` + `BEmailAuthenticatorTypeConfig` | Auth (incl OAuth2 via `oauth2-rt` dep) |

---

## 34.19 `system.properties` flags relevantes

Del `defaults/system.properties`:

```properties
# Set the HxAlarmConsole auto-refresh interval in minutes, with a minimum
# of 1. When the value is invalid or commented, the HxAlarmConsole does
# not auto-refresh.
#hx.alarmConsole.autoRefresh.interval=5

# Allow niagara to cache open alarm sources when a file alarm db is used.
# If the property value is not set, by default, the flag will be true and
# open alarm sources will be cached.
#niagara.fileAlarm.enableCachedSources=true
```

**Solo 2 flags** específicas de alarms en el default. La mayoría de config es component-level (`BAlarmService` properties, per-recipient, per-class).

**`niagara.fileAlarm.enableCachedSources=true`** — cachea la lista de sources-con-open-alarms. Si `false`, cada query a `getOpenAlarmSources()` hace scan del store → lento en stations grandes. Default `true`. Desactivar solo si memoria muy constrained (stale data si records corruptos).

**`hx.alarmConsole.autoRefresh.interval=5`** — HxAlarmConsole (web console hx) auto-refresh en min. Default commented = no auto-refresh. Para web dashboard operacional 24/7, descommentar y setear 1-5 min.

**Otros flags inferidos** (vía permissions + paths, no literal):
- `niagara.alternative.database.path` — si seteado, `.adb` va a otra ubicación (fuera de station home). Útil para discos dedicados.
- `niagara.alternative.archive.path` — idem para archive.
- `niagara.alternative.archive.zip.path` — archive comprimido.

---

## 34.20 Checklist audit de una AlarmService en prod

**Pre-deploy**:
1. License feature `tridium:alarm` válida? (gate `configFatal`)
2. `coalesceAlarms` set según política (true reduce nuisance; false para compliance strict)
3. `defaultAlarmClass` existe y apunta a un `BAlarmClass` válido?
4. `masterAlarmInstructions` seteado con guidance global?
5. `escalationTimeTrigger` seteado (recommended: cron @ */1 min)?
6. `BAlarmDbConfig.capacity` dimensionado para peak storm:
   - storm peak/s × expected window (24h) × safety factor (3x)
   - típico site mediano: 5 000-20 000
7. Disk space `${protected.station.home}/alarm/` suficiente (capacity × avg record size ~500B)?
8. File path en `niagara.alternative.database.path` si disco dedicado?

**Recipients**:
1. Cada `BAlarmClass` tiene al menos 1 recipient linked?
2. Recipients con `persistent=true` donde aplique (Email, SMS)?
3. `BStationRecipient` SOLO donde destino es always-online (NO aggregation pattern)?
4. Credentials encrypted via BPassword + keyring (`.km/.kr`) verificadas?
5. `timeRange` + `daysOfWeek` coherent con on-call?
6. `routeAcks` set coherent?

**Escalation**:
1. Level 1/2/3 enabled donde necesario?
2. Delays razonables (level1=10m, level2=30m, level3=1h típico)?
3. Level distinct recipients? (no sentido escalar al mismo operator)

**Orion/archive (si aplica)**:
1. RDB connection jTDS healthy?
2. Schema versión match?
3. `executionTime` en horario off-peak?
4. `clearedAlarmLingerTime` coherente (24h típico)?
5. `alarmOnFailure=true` (receive alarm si archive falla)?

**Operational**:
1. Workbench console → check open alarms count stable?
2. Spy page `/ord/BAlarmService/spy` muestra queue size bajo?
3. `AlarmService.status` = `ok`?
4. Log: no `WARN` recurrentes de "DataRecoveryService in fault"?
5. `.drr` file < 10 MB (si crece → recovery loop)?
6. `.adb` file growing linearly? (sudden 10x = check storm o trim misconfig)

**Honeywell-specific**:
1. `BHonAlarmClass.enableAlarmDelay` coherent con `coalesceAlarms` (G3)?
2. `sendDelayBufferOnShutdown=true`?
3. `BHonAlarmConsoleRpc.checkEdgeController` passes (license)?

**Security**:
1. `BFileAlarmDatabase.canWrite` permissions granted solo a `alarm-admin` role?
2. Spy pages restringidas (G24)?
3. Email credentials rotated per policy?
4. `user` field en records matches authenticated session (no defaults)?

---

## 34.21 Conexión con bloques previos

### Correcciones

- **Bloque 31.3** (history/archive VACUUM SQLite): **CORREGIDO aquí (34.0)**. `.adb` ES binario custom Tridium MAGIC `0x6010ACCD`, **NO SQLite**. Paralelo arquitectónico a `.hdb` (MAGIC `0xA0F61E5E`, Bloque 33.0). No hay VACUUM. Hay `trimToCapacity` + `clearOldRecords`. Ventanas 5-30 min de Bloque 31 corresponden al archive job remoto (Orion SQL insert batch), NO al `.adb` local.
- **Bloque 13.2** (keyring `master.jceks`): ya corregido en ese bloque hacia DPAPI `.km/.kr`. Aquí **reconfirmado** — `BEmailRecipient.emailAccount` usa `BPassword` encrypted con ese keyring. Cross-linked G21.
- **Bloque 8.2** (alarmExt básico): **extendido** con enumeración completa de 14 algoritmos (offnormal 10, fault 4) y enum `BAlarmState` de 5 valores (no 3 como implícito en Bloque 8).
- **Bloque 8.3** (BAckState): **extendido** — son **3 enums distintos** (34.3). `BAckState.ACK_PENDING` es el tercer valor a menudo omitido.

### Extensiones

- **Bloque 13.1.7 + 19.13** (Supervisor bottleneck ~50 subordinados): aplica directamente a alarm aggregation pattern. `BStationRecipient` Supervisor→subordinate es anti-pattern — usar inverso (G22).
- **Bloque 23.7** (BACnet intrinsic/algorithmic): upstream natural de `bacnetAlarmRouter-rt`. Sección 34.11 + 34.14 mappea el flow.
- **Bloque 24.14** (Schedule DST edge cases): mismo problema en `BAlarmRecord.timestamp`/`ackTime`/`normalTime` — TZ-aware serialization vía `encodeAbsTime(Context)`.
- **Bloque 29** (web tier + spy): `/ord/BAlarmService/spy` + `AlarmStore.analyze()` expuesto. Securizar G24.
- **Bloque 31.4** (audit queue unbounded → OOM): mismo patrón en `BAlarmService.alarmQueue`. G1.
- **Bloque 33.0** (`.hdb` binario Tridium): **paralelo arquitectónico exacto** al `.adb` — ambos paginados custom (Block+Page), ambos con FreePageMap, ambos con indices in-memory + data on-disk, ambos con MAGIC distinto. Same design patterns, distinto schema.

### Nuevos gotchas transversales (G1-G27) consolidados arriba en 34.16.

---

**FIN Bloque 34**
