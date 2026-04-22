# Niagara N4 — Mental Model · Bloque 8: Alarm + History + Schedule

**Sesión**: 2026-04-22
**Método**: Investigación empírica READ-ONLY (3 sub-agents Explore en paralelo)
**Fuentes**: devguide (alarm, driverAlarm, history, driverHistory, schedule, driverSchedule), source `javax.baja.alarm.*`, `javax.baja.history.*`, `javax.baja.schedule.*`, decompilado alarm-rt, history-rt, schedule-rt.

Tres subsistemas core del nivel BAS que consumen masivamente puntos de drivers. Son "productos dentro del framework" — cada uno con pipeline y persistencia propios.

---

## Tabla de contenidos

1. [Alarm subsystem](#81-alarm-subsystem)
2. [History service](#82-history-service)
3. [Schedule subsystem](#83-schedule-subsystem)
4. [Síntesis](#síntesis-del-bloque)

---

## 8.1 Alarm subsystem

### 8.1.1 Pipeline general (source → class → recipient)

Pipeline de 3 capas:

1. **Source** (`BIAlarmSource` + `BAlarmSourceExt`): cualquier objeto que genere alarmas. La mayoría son extensiones de BControlPoint. Decide cuándo su punto padre entra en estado alarma.
2. **Service** (`BAlarmService`): singleton que orquesta. Recibe BAlarmRecord desde sources, routea via `BAlarmClass`, gestiona acks de vuelta. Mantiene `BAlarmDatabase` y opcionalmente `BAlarmArchive` (Niagara 4.11+).
3. **Recipient** (`BAlarmRecipient` subclases): consumidores. BConsoleRecipient (UI console), BStationRecipient (envío a stations remotos), BEmailRecipient, BLinePrinterRecipient/BPrinterRecipient.

**Flujo nueva alarma**: source → AlarmService → AlarmClass (setea priority, ackRequired, datos) → N recipients via topic link.
**Flujo ack**: recipient → AlarmService → AlarmClass → source (si ackRequired).
**Flujo cleared**: acked + toNormal → archivada si BArchiveAlarmProvider configurado.

### 8.1.2 BAlarmService y organización

BAlarmService es un BSpace que mantiene:
- `BAlarmDatabase` (implementa `BIAlarmSpace`): API refactorizada desde Niagara AX. Soporta RDBMS y ODBMS. Métodos en `AlarmSpaceConnection`: `append()`, `update()`, `getRecord(uuid)`, `getOpenAlarmSources()`, `getOpenAlarms()`, `getAckPendingAlarms()`, `scan()`, `timeQuery()`.
- `AlarmDbConnection` (extends AlarmSpaceConnection, `AutoCloseable`): adds `clearAllRecords()`, `clearOldRecords()`, `clearRecord()`.
- `BAlarmDbConfig`: config modular. `BFileAlarmDbConfig` (default) tiene prop `capacity`.
- Slots de `BAlarmClass`: todas las clases registradas.
- Opcional `BAlarmArchive` (Niagara 4.11+) para archival de cleared.

Patrón uso:
```java
try (AlarmDbConnection conn = service.getAlarmDb().getDbConnection(null)) {
  conn.getOpenAlarms();
  conn.clearOldRecords(before, cx);
}
```

### 8.1.3 BAlarmClass

Agrupa alarmas con características de routing/manejo similares.

**Propiedades clave**:
- `ackRequired` (bool): si requiere acknowledgement explícito.
- `Priority[transition]` (1-255, default 255): orden notificación en OnCallService. Bajos = alta prioridad.
- `Escalation Level(n) Enable/Delay` (n=1,2,3): reescalamiento si no acked en delay (mín 1 min).
- `Total/Open/InAlarm/Unacked Alarm Count` (readonly): métricas.
- `Time Of Last Alarm` (readonly).

AlarmClass maneja persistencia via alarm database (insert, update). Topic para linking a recipients. Cada source mapea a una clase via `BAlarmSourceExt.alarmClass` property.

### 8.1.4 AlarmSourceExt y transitions

**Subclases especializadas**:
- `BooleanChangeOfStateAlarmExt` — cambios en boolean points.
- `OutOfRangeAlarmExt` — rangos numéricos (high/low limits).
- `StatusAlarmExt` — alarmas de status.
- `EnumChangeOfStateAlarmExt`, `StringChangeOfValueAlarmExt`, `EnumCommandFailureAlarmExt`.

**Transitions (`BAlarmTransitionBits`)**:
1. **toOffnormal**: source setea `BStatus.ALARM` + `BStatus.UNACKED_ALARM` (si ackRequired).
2. **toFault**: `BStatus.ALARM` + `BStatus.UNACKED_ALARM` + `BStatus.FAULT`. Sin delay para faults.
3. **toNormal**: limpia `BStatus.ALARM` + `BStatus.FAULT`.
4. **toAlert**: alarma sin estado normal (solo fired y acked).
5. **ackAlarm()**: si es último pendiente, limpia `BStatus.UNACKED_ALARM`.
6. **toDisabled**: via `BAlarmInhibit` + `BAlarmInhibitTime`. Inhibit previene generación de toOffnormal/toNormal.

**Config en extension**:
- `Alarm Enable` (toOffnormal o toFault).
- `Time Delay` (h:m:s): condición persistente antes de fire (no aplica a faults).
- `Time Delay to Normal`: tiempo normal requerido antes de clear.
- `Offnormal/Fault Algorithm`: containers con condiciones específicas.
- `Alarm Class`, `Inhibit`, `Inhibit Time`, `Source Name`, `To Offnormal/Fault/Normal Text`, `Hyperlink`, `Sound File`, `Icon`, `Instructions`.

### 8.1.5 BAlarmRecord

Entidad única per evento. **Lifecycle**:
- NewAlarm → generada, routeada.
- AcknowledgedAlarm → recipient.ackAlarm() → source. ackState=acked, ackTime, ackUser.
- NormalAlarm → source transiciona a normal.
- AcknowledgedNormalAlarm → cleared (final).

**4 estados**: newAlarm, acknowledgedAlarm, normalAlarm, acknowledgedNormalAlarm. Alert no tiene normal (newAlert, acknowledgedAlert solamente).

**Campos principales**:
- `uuid`: BUuid único.
- `source`: BOrdList al BIAlarmSource.
- `timestamp`.
- `sourceState`: estado del source al crear (Normal, LowLimit, HighLimit, Fault).
- `transition`: toOffnormal/toFault/toNormal/toAlert (inmutable).
- `ackState`: acked/unacked.
- `ackTime`, `ackUser`.
- `normalTime`: cuándo transicionó a normal.
- `messageText` (BFormat customizable).
- `priority` (1-255).
- `alarmClass`: ref a la clase que routeó.
- `count`: # eventos offnormal en el source.
- `alarmData`: dict custom.
- `lastUpdate`.

### 8.1.6 Recipients

**BConsoleRecipient**: gestor alarm history ↔ UI console.
- Config: Time Range (start/end hours, default 24h), Days of Week, Transitions (qué ve el console vs qué queda en history), Route Acks (bool), Default Time Range.
- Vista default: AX Alarm Console (tabla per-point, último alarm por row).
- Comandos: Acknowledge, Hyperlink, Add Notes, Silence (audio), Filter, Live Update, Alarm Details, Refresh.

**BStationRecipient**: transfiere alarms a station remota.
- Config: Remote Station (dropdown), Time Range, Days of Week, Transitions, Retry Interval (h:m), Status, Last Send/Failure Time, Failure Cause, Queued Alarm Count.

**BEmailRecipient**: parte del email package. Requiere SMTP config.

**BLinePrinterRecipient**, **BPrinterRecipient**: solo Windows. Impresoras ink-jet/laser.

**Patrón genérico**: linked desde topic del AlarmClass. `handleAlarm(BAlarmRecord)` recibe routeAlarm. Filtros por time of day, day of week, transition type.

### 8.1.7 Ack workflow

1. Source genera offnormal/fault → `postAlarm(BAlarmRecord)` a AlarmService.
2. AlarmService.doRouteAlarm() → busca AlarmClass → routeAlarm() en todos los recipients.
3. Recipient.handleAlarm() → procesa (console muestra, email envía).
4. User/sistema en recipient llama ackAlarm(uuid, user).
5. Recipient enruta ACK → AlarmService.
6. AlarmService.doRouteAlarmAck() → si ackRequired, routea BAlarmSourceExt.ackAlarm(uuid).
7. Source limpia `UNACKED_ALARM` bit si es último pendiente.
8. Opcional: BAlarmAcknowledger para escalation/retry de ACK.

**Constraints**:
- Cleared solo si acked AND source en normal.
- Escalation: si no acked en Escalation Level Delay, re-routea via OnCallService + priority order.
- Si ack no required: cleared cuando sourceState → normal.

### 8.1.8 Persistence, retention, purge

**Storage**:
- BAlarmDatabase (extends BSpace) en `station/alarm/` folder.
- Default `BFileAlarmDbConfig` (file-based, capacity = max records).
- Connection-oriented vía `AlarmDbConnection` (AutoCloseable).

**Open vs Archive**:
- **Open Alarms**: no cleared. Vía `getOpenAlarms()`.
- **Cleared**: acked + normal. Candidatos a archival.
- **BAlarmArchive** (4.11+): RDBMS secundaria. `BOrionArchiveAlarmProvider` ejecuta action periódica. Requires `tridium:alarmArchive` license.

**Queries BQL**:
- `alarm:|select * from openAlarms`.
- `alarm:archive|select * where (sourceState='normal' or 'alert') and ackState='acked'`.

**Maintenance view**:
- Clear Old Records (before datetime).
- Clear All Before Selected Record.
- Clear All Records.
- Run Maintenance button.

**updateConfig(BAlarmDbConfig, Property)**: permite reconfiguración dinámica sin restart.

---

## 8.2 History service

### 8.2.1 Arquitectura general

**`BHistoryService`**: singleton. Implementa BIService. Contiene:
- `BHistoryDatabase` (implementa `BHistorySpace`): maneja archivos y acceso eficiente. Persiste en disco binario serializado.
- `BHistorySpace`: interface abstracta (create/delete/rename histories).
- `HistorySpaceConnection`: AutoCloseable. Todas lecturas/escrituras/deletes via esta conexión (patrón connection-oriented de Niagara 4, reemplazó acceso directo AX).

**Acceso vía ORD scheme `history:`** (BHistoryScheme). Ej: `history:/demo/TestLog` accede historia "TestLog" del device "demo". Historias se organizan bajo `BHistoryDevice`.

### 8.2.2 HistoryExt subtypes

**BHistoryExt** (abstract, implementa BIHistorySource). Proporciona config (BHistoryConfig) y permite a points generar histories automáticamente.

**BIntervalHistoryExt** — periódica:
- `interval` (BRelTime, default 15 min).
- Action `intervalElapsed()` disparada por scheduler.
- Subtypes: NumericIntervalHistoryExt, BooleanIntervalHistoryExt, EnumIntervalHistoryExt, StringIntervalHistoryExt.
- Algoritmo: IntervalAlgorithm muestrea a intervalos.

**BCovHistoryExt** — cambio de valor:
- Sin intervalo configurable (IRREGULAR).
- `pointChanged(timestamp, value)` comparado con lastValue.
- `isChange(oldValue, newValue)` via `equivalent()`.
- Subtypes: NumericCovHistoryExt, BooleanCovHistoryExt, EnumCovHistoryExt, StringCovHistoryExt.

**BBooleanChangeOfStateHistoryExt**: especialización para booleanos.

**Props comunes**:
- `enabled`.
- `activePeriod` (BActivePeriod: days semana + horario).
- `active` (readonly transient).
- `historyName` (BFormat, default `%parent.name%`).
- `historyConfig` (BHistoryConfig).

### 8.2.3 TrendRecord types

Todos extienden `BHistoryRecord` (clave timestamp).

**BTrendRecord** (abstract) añade:
- `trendFlags` (BTrendFlags): STARTING, OUT_OF_ORDER, HIDDEN, MODIFIED, INTERPOLATED.
- `status` (BStatus).

Concretos:
- `BNumericTrendRecord`: value double, implementa BINumeric.
- `BBooleanTrendRecord`: value boolean.
- `BEnumTrendRecord`: value enum.
- `BStringTrendRecord`: value String.

Cada tipo serializable binario. Formato optimizado N4 (NCCB-8646): status + trend empaquetados en 2 bytes en vez de 4.

### 8.2.4 Storage layout

`BStorageType.file` (default).

```
/{station}/history/
  /{historyDevice}/
    {historyName}.history
    {historyName}_cfg0.history (si split por cambio incompatible)
```

**Serialización per record**: timestamp (BAbsTime), status (byte), trendFlags (byte), valor tipado (double/boolean/String/enum). Versioned via `getHistoryVersion()`.

**Split policy**: si BHistoryExt cambia incompatiblemente (tipo record o intervalo), la historia vieja conserva nombre, nueva con sufijo `_cfg#`.

### 8.2.5 Rollups

Documentación menciona "History Policies" pero no detalla rollups como agregación temporal en el source analizado. Controles principales:
- `BCapacity`: recordCount, storageSize, o unlimited.
- `BFullPolicy`: ROLL (sobrescribe viejos) o STOP (detiene grabación).

Supervisor puede recolectar histories de subordinados y aplicar config rules (Default/Custom) en `NiagaraNetwork > History Policies` para transformación al importar.

### 8.2.6 Retention y archive

**Retención local**:
- `BCapacity.makeByRecordCount(N)`.
- `BCapacity.makeByStorageSize(bytes)`.
- `BCapacity.makeUnlimited()` (recommended Supervisor con disk grande).

**Archive/Export**:
- `BHistoryExport` / `BHistoryImport` (push/pull entre stations via NiagaraNetwork + Fox).
- `BFoxHistorySpace`: acceso a histories remotas via Fox.

**Maintenance**:
- `clearOldRecords(BHistoryId, beforeDate, Context)`.
- `clearAllRecords(BHistoryId)`.
- `deleteHistory(BHistoryId)`.

### 8.2.7 HistoryId y HistoryConfig

**BHistoryId**: identificador compuesto `{deviceName}/{historyName}` (max 200 chars historyName). Shorthand `^historyName` para referencia local.

**BHistoryConfig**:
- `id` (readonly).
- `source` (BOrdList, path recorrido via archiving).
- `sourceHandle` (BOrd).
- `timeZone` (BTimeZone).
- `recordType` (BTypeSpec).
- `schema` (BHistorySchema, permite leer si clase original cambió/desaparece).
- `capacity`, `fullPolicy`, `storageType`.
- `interval` (BCollectionInterval: IRREGULAR para COV, fixed para Interval).
- `systemTags`: metadata para import/export selectivo (semicolon-delimited, ej. `"NorthAmerica;Region1"`).

Cambios incompatibles disparan split.

### 8.2.8 Queries

```java
try (HistorySpaceConnection conn = db.getConnection(cx)) {
  BIHistory hist = conn.getHistory(historyId);
  try (Cursor<BHistoryRecord> cursor = conn.scan(hist, descending)) {
    while (cursor.next()) {
      BHistoryRecord rec = cursor.get();
    }
  }
  BITable<BHistoryRecord> results = conn.timeQuery(hist, startTime, endTime);
}
```

**HistoryQuery (BQuery)**:
- Sintaxis `history:<path>[?params]`.
- Path absoluto `/device/name`, relativo `name`, local `^name`.
- Params: `period=lastWeek`, `start=...`, `end=...`, `delta=true`.

**Summary**: `conn.getSummary(hist)` → BHistorySummary (id, recordCount, firstTimestamp, lastTimestamp).

### 8.2.9 Remote history / Supervisor collection

Arquitectura Supervisor-Subordinate:
- Subordinate (controller) genera histories locales.
- Supervisor recolecta vía NiagaraNetwork + Fox.
- `BFoxHistorySpace` provee acceso remoto transparente.

**Export (push)**: en source, crear BHistoryExport descriptor, config target (station Supervisor) + schedule. Export descriptor vive en `NiagaraNetwork > Histories` del target.

**Import (pull)**: en Supervisor, crear BHistoryImport descriptor + on-demand poll. `History Policies > Default Rule` establece capacity/fullPolicy para importadas.

### 8.2.10 Export (CSV, PDF)

`BHistoryExport` con formatos alternativos:
- Integración con History Export Manager (UI Workbench).
- Filter por rango + station/history name.
- CSV, PDF.

Database Maintenance view permite seleccionar histories, "Run Maintenance". NiagaraNetwork History Export Manager permite "Discover" de historias disponibles.

Metadatos incluyen `systemTags` para marcado selectivo.

---

## 8.3 Schedule subsystem

### 8.3.1 Jerarquía de tipos

```
BAbstractSchedule (contrato isEffective + nextEvent)
  ├─ BCompositeSchedule (union OR / intersection AND)
  │    ├─ 6 subclases concretas
  │    └─ BCustomSchedule (AND de daysOfMonth + months + weekdays + weeksOfMonth + year)
  │
  ├─ BWeeklySchedule (grid 7-días × N time-slots + excepciones)
  │    ├─ BBooleanSchedule
  │    ├─ BNumericSchedule
  │    ├─ BEnumSchedule
  │    └─ BStringSchedule
  │
  ├─ BControlSchedule (adds output continuous, defaultOutput, cleanup)
  │
  ├─ BDateSchedule / BDaySchedule / BTimeSchedule / BDateRangeSchedule
  │
  └─ BCalendarSchedule (eventos Boolean reutilizables)
      BTriggerSchedule (discrete event triggers, lastTrigger, nextTrigger)
      BScheduleReference (referencia a schedule externo)
```

### 8.3.2 BAbstractSchedule contract

- **`isEffective(BAbsTime at)`**: retorna bool si el schedule está activo en ese instante. **Stateless**.
- **`nextEvent(BAbsTime after)`**: próximo cambio de efectividad desde `after`. Return null si `alwaysEffective=true`.

**BControlSchedule** añade:
- `defaultOutput`: valor cuando no hay ningún slot efectivo.
- `effectiveValue` (readonly): valor actual calculado.
- `lastModified` (BAbsTime readonly): tracking vía `trackModifications()`.
- `cleanupExpiredEvents` (boolean): purga automática de eventos vencidos.
- 4 tipos de output según BWeeklySchedule concreta.

### 8.3.3 Evaluación — isEffective, nextEvent, getOutput

**isEffective**:
- BCompositeSchedule: OR → early-exit true; AND → acumulativo.
- BWeeklySchedule: (1) extrae day-of-week, (2) chequea `specialEvents` primero, (3) sino BDaySchedule de ese weekday, (4) luego BTimeSchedule children.
- Time-slots son `[start, finish)` — finish exclusivo.

**Caching**: BCompositeSchedule invalida cache en ADDED/REMOVED/CHANGED vía `fw()` hook.

**nextEvent** (forward-scanning):
- BWeeklySchedule: min(schedule.nextEvent, effective.nextEvent, specialEvents.nextEvent).
- BDaySchedule: itera slots temporales.
- Búsqueda acotada por `scanLimit` (default 90 días).

**getOutput(at)**: DFS pre-order de descendientes efectivos. **Primer hijo con effectiveValue no-null gana** (prioridad = orden de inserción). Si ninguno efectivo → null → `defaultOutput` aplicado en nivel superior.

### 8.3.4 Excepciones y calendarios

- **specialEvents**: `Map<String, BDailySchedule>` en BWeeklySchedule. Ej: `"Christmas" → BDailySchedule`. Si fecha coincide con special event, se evalúa el BDailySchedule en vez del weekday.
- **BCalendarSchedule**: centraliza eventos (output Boolean). Reusable por múltiples schedules via BScheduleReference.
- **No hay holiday DB built-in** — configuración manual o via BCustomSchedule.

### 8.3.5 Composición multi-nivel

- Nested BCompositeSchedule permitida. Ej: `Season AND DayOfWeek`.
- **BCustomSchedule**: composite con hijos `daysOfMonth`, `months`, `weekdays`, `weeksOfMonth`, `year` (AND).
- **BDateSchedule**: intersección de 4 dimensiones temporales (year AND month AND day AND weekday).

### 8.3.6 Prioridad entre slots

A diferencia de Controls (priority array 16-level), Schedules usan **orden de hijos DFS pre-order**. El primer slot efectivo con value no-null gana. Para forzar prioridad, ordenar slots manualmente (más específicos primero).

### 8.3.7 Zonas horarias y DST

- `BAbsTime`: millis UTC internamente. Construcción con BTimeZone + DstRule.
- DST spring-forward/fall-back manejadas por calendar math de BAbsTime.
- **Ambigüedad** (ej. 2:30am en fall-back): Niagara usa hora estándar (winter offset).
- `nextEvent()` respeta transiciones automáticamente.

### 8.3.8 Sincronización remota

- **BScheduleDeviceExt**: manager export/import entre stations.
- **BScheduleExport**: Supervisor → Subordinate. `doExecute()` sube config. Inlining via `getExportableSchedule()`.
- **BScheduleImportExt**: Subordinate → Supervisor. `subscribeWindow` randomiza suscripción post-startup para distribuir carga.
- Properties con flag **`USER_DEFINED_1`** se replican automáticamente.
- Retry vía `retryTrigger` en faults.

### 8.3.9 Historia y auditoría

- `BControlSchedule.lastModified` — tracking modifications.
- `BTriggerSchedule.lastTrigger`, `nextTrigger` — readonly transient.
- **NO hay journal built-in** de output changes. Para trazar, subscribirse a topic `onOutputChange`.
- `cleanupExpiredEvents` automatiza purga.

### 8.3.10 Diferencias clave Controls (Bloque 6) vs Schedules

| Aspecto | Controls | Schedules |
|---------|----------|-----------|
| Output | punto-en-tiempo | function(BAbsTime) |
| Prioridad | priority array 16-level | hijo-order DFS |
| Composición | simple array | árbol composite ilimitado |
| Overrides | 2 inputs | 1 input (in property) |
| Sync remota | proxyExt | deviceExt + ref inlining |
| Time-awareness | event-driven | forward-scan nextEvent |

---

## Síntesis del bloque

### Modelo mental

**Alarm, History, Schedule** son los 3 "productos operacionales" del framework. Cada uno:
1. Tiene pipeline/arquitectura propia (source → class → recipient; point → historyExt → database; schedule → output).
2. Persiste con formato propio (alarm DB con BAlarmRecord; history binario .history; schedules serializados en BOG).
3. Tiene acceso remoto via FOX (BStationRecipient, BFoxHistorySpace, BScheduleImportExt).
4. Expone BQL/NEQL sobre su espacio (alarm:, history:, no schedule-specific).

### Conexiones con bloques anteriores

- **Bloque 6.3.4**: `BAlarmSourceExt`, `BIntervalHistoryExt`, `BCovHistoryExt` son extensions del ControlPoint. Este bloque expande cada una end-to-end.
- **Bloque 4.3**: los flags de slot aparecen en schedule — `USER_DEFINED_1` = property replicable en Schedule sync.
- **Bloque 5.2**: BAlarmRecord, BHistoryConfig, Schedules se serializan en BOG siguiendo el formato general.
- **Bloque 5.3**: BQL sobre `alarm:` y `history:` extents son casos canónicos.
- **Bloque 6.1**: History collection, alarm routing, schedule evaluation corren en engine thread. Long-running queries deben moverse a workers.

### Gotchas críticos

1. **Alarm ack cleared requires BOTH acked AND source normal**. Si ackRequired=false, cleared solo con normal. Si ackRequired=true, cleared con ambos.
2. **Escalation** trigger por falta de ack dentro de delay — configurable per AlarmClass, no per source.
3. **History split por cambio incompatible** genera `{name}_cfg#` files. Upgrades de tipo de record o interval rompen continuidad.
4. **History COV NO tiene interval** — es IRREGULAR. Queries deben usar timestamps no asume spacing uniforme.
5. **Schedule child-order como prioridad** — no hay mecanismo 16-level como Controls. Ordenar slots específicos primero.
6. **Schedule DST ambiguity 2:30am fall-back**: Niagara usa winter offset. Documentá esto en design.
7. **systemTags en HistoryConfig**: semicolon-delimited (no coma). Error común.
8. **BAlarmArchive requiere license separada** (`tridium:alarmArchive`) y solo está disponible N4.11+.
9. **HistorySpaceConnection AutoCloseable** — usar try-with-resources siempre. Leaks = DB locked.
10. **BTriggerSchedule.nextTrigger readonly transient** — no persiste entre reboots. Post-restart primer scan computa nuevamente.

### Qué habilita

Con Bloques 1-8 podés:
- Construir un pipeline completo: BAC device → point → alarm extension → recipient → email.
- Diseñar estrategia de historización con mix Interval + COV y calcular disk usage.
- Implementar un schedule complejo con holidays, overrides y sync Supervisor→Subordinate.
- Debuggear por qué una alarma no llega a console (pipeline tracing).
- Calcular impact de retention en disk y configurar archival.

**Próximo**: Bloque 9 — UI Stack (Workbench, Px, BajaScript, hx, servlets).

---

## Engram topic keys

- `niagara/subsystems/alarm-pipeline` — source→class→recipient, transitions, ack workflow, persistence, archive.
- `niagara/subsystems/history-service` — HistoryExt types, TrendRecord, storage layout, HistoryConfig, queries, remote collection.
- `niagara/subsystems/schedule-subsystem` — tipos, evaluation isEffective/nextEvent/getOutput, excepciones, composición, sync, DST.

---

**Sesión cerrada**: 2026-04-22 — Bloque 8 consolidado.
