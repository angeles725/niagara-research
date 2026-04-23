# Bloque 24 — Schedule Framework Niagara-native + driverSchedule cross-driver + Control palette (kitControl)

Fecha: 2026-04-23
Fuentes empíricas: decompilados `schedule-rt/ux/wb`, `control-rt`, `kitControl-rt/ux/wb`, `docKitControl-doc`, `driver-rt`, `ndriver-rt`, `niagaraDriver-rt`, + drivers específicos (bacnet, lonworks, modbusTcp/Async, opcUa) + `devguide-clean/{schedule,driverSchedule,control}.txt`.

Cubre el **control layer** completo: schedules Niagara puros (no BACnet — ver Bloque 23), extension cross-driver para sincronizar schedules con devices remotos, palette kitControl con 152 componentes, binding operacional schedule→writable→ProxyExt→device.

---

## 24.1 BAbstractSchedule jerarquía (Niagara-native)

### Clase base

```java
BAbstractSchedule extends BComponent (abstract)
  // API core:
  boolean           isEffective(BAbsTime)
  BAbsTime          nextEvent(BAbsTime)
  BStatusValue      getEffectiveValue()
  BStatusValue      getOutput(BAbsTime)
  BAbstractSchedule getOutputSource(BAbsTime)

  // Properties:
  effectiveValue    BStatusValue  (dinámico)
  alwaysEffective   boolean
  EFFECTIVE_VALUE   constante marca

  // Subscribers:
  references        SimpleSortedSet  (tracks BScheduleReference)
```

Métodos críticos:
- `isEffective(time)` — estado lógico ahora
- `nextEvent(time)` — próximo cambio de estado (punto donde isEffective cambia)
- `getOutput(time)` — valor salida (default pasa a primer hijo effective)
- `getOutputSource(time)` — CUÁL hijo composite es responsable de la salida

### Subclases atómicas

| Clase | Función |
|---|---|
| BBooleanSchedule | Weekly boolean (ON/OFF) |
| BNumericSchedule | Weekly numeric (setpoints) |
| BEnumSchedule | Weekly enum (modo HVAC) |
| BStringSchedule | Weekly string |
| BTimeSchedule | Rango HH:MM:SS - HH:MM:SS |
| BDateSchedule | Selector día/mes/año/weekday |
| BDateRangeSchedule | Rango entre fechas |
| BDayOfMonthSchedule | Días específicos (1-31) |
| BWeekdaySchedule | Mon-Sun selector |
| BMonthSchedule | Jan-Dec selector |
| BYearSchedule | Año específico |
| BCustomSchedule | Criterio user-defined |

### Composite

```java
BCompositeSchedule extends BAbstractSchedule
  children  BAbstractSchedule[]
  union     boolean          true = OR; false = AND
  status    BStatus
  faultCause String
  // cache internal: cache[], hint → evita full scan
```

`nextEvent(time)` recursivo: explora children, retorna MIN(próximas transiciones).

---

## 24.2 BWeeklySchedule + BDailySchedule (núcleo control continuo)

### BWeeklySchedule estructura

```
BWeeklySchedule extends BControlSchedule (abstract)
├ effective            BDateRangeSchedule
│                       (período validez: 1-ago hasta 30-nov, ej.)
├ schedule             BCompositeSchedule (interna, auto-creada)
│  └ 7 children        BDailySchedule × BWeekday (Mon-Sun)
│    union=false (AND) — día correcto + time correcto
├ Week                 BWeekSchedule
│  ├ sunday..saturday  BDailySchedule
│  └ c/u: lista BTimeSchedule
├ specialEvents        BCompositeSchedule
│  └ BDailySchedule[] nombrados ("Christmas", "LaborDay")
├ addSpecialEvent(name, BDailySchedule)
└ get(BWeekday)        BDaySchedule acceso directo
```

### Prioridad de salida (BWeeklySchedule)

```
Priority  Source
 1        In slot (override exógeno — linked y non-null)
 2        Special Event activo (fecha/hora coincide)
 3        Regular Weekly event (BWeekday + BTime coinciden)
 4        defaultOutput (configurado)
```

### BDailySchedule estructura

```java
BDailySchedule extends BCompositeSchedule
  day    BDaySchedule  container de BTimeSchedule[]
                       cada: [HH:MM start, HH:MM finish, BStatusValue output]
  days   BAbstractSchedule  selector (BDateSchedule/Range/etc)
  union  boolean       true = cualquier BTimeSchedule activo → effective
                       false = todos activos (raro)
```

### API programática

```java
BWeeklySchedule weekSched = new BBooleanSchedule();

// Weekly regular: Lunes 08:00-17:00 → true
BDaySchedule monday = weekSched.get(BWeekday.monday);
monday.add(BTime.make(8,0,0), BTime.make(17,0,0), BStatusBoolean.make(true));

// Special event: 5 mayo 11:00-12:00
BDailySchedule cincoMayo = new BDailySchedule(
  new BDateSchedule(5, BMonth.may, -1),
  BTime.make(11,0,0), BTime.make(12,0,0),
  BStatusBoolean.make(true)
);
weekSched.addSpecialEvent("cincoDiMayo", cincoMayo);

// Iterar eventos especiales
BDailySchedule[] events = weekSched.getSpecialEventsChildren();
```

---

## 24.3 BCompositeSchedule — motor composición (union/intersection)

### Algoritmo isEffective

```
isEffective(time):
  if union=true:
    return ANY child.isEffective(time)      // OR
  else:
    return ALL children.isEffective(time)   // AND
  fault detectado si cualquier child.status = error
```

### getOutputSource (primera salida effective)

```
getOutputSource(time):
  for each child (orden adición):
    if child.isEffective(time):
      // busca propiedad "effectiveValue" (10 niveles deep)
      if child.effectiveValue != null:
        return child
      else if child es composite:
        RECURSE child.getOutputSource()
  return this  // usar defaultOutput
```

**ORDEN de adición** determina prioridad. Primer effective con valor non-null gana.

### Ejemplo composite

```
myCompositeSchedule (union=false, AND)
 ├[0] SpecialEventSchedule    prioridad 1  → 22°C si active
 ├[1] WeeklySchedule          prioridad 2  → 20°C weekday / 18°C weekend
 └[2] DefaultSchedule         prioridad 3  → 16°C (always effective)

Eval @14:00 martes normal:
  0 SpecialEvent false
  1 Weekly true → output 20°C (source = Weekly)

Eval @14:00 25-dic:
  0 SpecialEvent true → output 22°C (source = SpecialEvent)
```

---

## 24.4 BCalendarSchedule

```java
BCalendarSchedule extends BControlSchedule
  in         BStatusBoolean (override external)
  out        BStatusBoolean (computed)
  nextTime   BAbsTime
  nextValue  BStatusBoolean
  events     BCompositeSchedule (interna)
    children BAbstractSchedule[]
  add(String eventName, BAbstractSchedule dateSchedule)
```

Tipos de eventos legales:
- BDateSchedule (fecha específica)
- BDateRangeSchedule (rango fechas)
- BWeekAndDaySchedule (3er viernes julio)
- BCustomSchedule (user-defined)
- BScheduleReference (ref a otro calendar — mantiene subscriber)

### Ejemplo Holiday Calendar

```java
BCalendarSchedule holidays = new BCalendarSchedule();
holidays.setDefaultOutput(BStatusBoolean.make(false));

// Thanksgiving: 4to jueves noviembre
BWeekAndDaySchedule tg = new BWeekAndDaySchedule(
  BMonth.november, 4, BWeekday.thursday);
holidays.add("Thanksgiving", tg);

// Navidad: 24-dic a 1-ene
BDateRangeSchedule xmas = new BDateRangeSchedule();
xmas.setStart(new BDateSchedule(24, BMonth.december, -1));
xmas.setEnd(new BDateSchedule(1, BMonth.january, -1));
holidays.add("Christmas", xmas);
```

---

## 24.5 BTriggerSchedule (event-based, NO continuo)

```java
BTriggerSchedule extends BCompositeSchedule
                implements BIStatus, IMetricResource
  dates                    BCalendarSchedule  (qué fechas aplica)
  times                    BDaySchedule       (qué horas del día)
  nextTriggerSearchLimit   BRelTime (default 90 días)

  // Output:
  trigger                  Topic (fires en nextTrigger)
  triggerMissed            Topic (fires si se perdió en startup)
  execute                  Action

  // Tracking:
  lastTrigger              BAbsTime
  nextTrigger              BAbsTime (computed)
  enabled                  boolean
  status                   BStatus

  // API:
  addDates(name, BAbstractSchedule)
  addTime(hour, minute)
  nextTrigger(start, searchLimit) BAbsTime
  fireTrigger(BValue) / fireTriggerMissed(BAbsTime)
```

### Diferencia con BWeeklySchedule

| Aspecto | BWeeklySchedule | BTriggerSchedule |
|---|---|---|
| Output | Continuo (StatusValue) | Discreto (Topic/Action fired) |
| Cambio | Transiciones → nextValue | fire/miss eventos |
| Uso | Control PN (setpoints) | Disparadores (alarmas, auditoría) |
| Trigger perdido | Recupera en próx. eval | Un solo triggerMissed fire |

Ejemplo audit daily:
```java
BTriggerSchedule audit = new BTriggerSchedule();
audit.getTimes().addTrigger(23, 59);  // 23:59:00
audit.subscribe(audit.trigger, event -> { /* log */ });
```

---

## 24.6 BScheduleSelector — multiplexado

```java
BAbstractScheduleSelector extends BComponent (abstract)
  container              BOrd (parent con schedules)
  schedule               BDynamicEnum (poblado dinámicamente)
  updateScheduleList     Action

// Subclases:
BBooleanScheduleSelector / BNumericScheduleSelector
BStringScheduleSelector / BEnumScheduleSelector
```

Mecanismo de link dinámico:
```java
doUpdateScheduleList():
  for each BAbstractSchedule found:
    enum.add(schedule.name)

changed(schedule) selector cambia:
  removeCurrentLink()
  createLink(selectedSchedule.In, currentSchedule.Out)
```

Uso típico: Ocupancy sensor → selector → picks occupied vs unoccupied schedule.

---

## 24.7 BControlSchedule (base Weekly + Calendar)

```java
BControlSchedule extends BCompositeSchedule (abstract)
  defaultOutput             BStatusValue (fallback)
  cleanupExpiredEvents      boolean
  scanLimit                 BRelTime (default 90 días, búsqueda nextEvent)
  facets                    BFacets (trueText/falseText display)
  lastModified              BAbsTime
  // Actions:
  cleanup                   Action   (fuerza limpieza)
  execute                   Action   (trigger manual)
  // Clock subscription:
  clockChanged(BRelTime)    notificado por Clock service
  // Status:
  status                    BStatus
  IMetricResource impl
```

Ciclo:
```
started() → subscribes Clock + primer execute()
clockChanged(BRelTime) → if nextCov() reached: SET out + fire nextCov change
stopped() → unsubscribes Clock
```

---

## 24.8 Motor de evaluación — nextTime + nextValue

### Algoritmo nextEvent

```
nextEvent(BAbsTime current):
  if atomic (BTimeSchedule):
    return primer t donde start ≤ t < finish cambia
  if composite:
    times = []
    for each child:
      times.append(child.nextEvent(current))
    return MIN(times)
  // Hint caching:
  cache[hint] = last effective child → probar primero
```

Complejidad: O(N).

### nextTime/nextValue (BWeeklySchedule, leídos cada segundo por Clock)

```
nextTime    BAbsTime    = schedule.nextEvent(now)
                          Si > 1 año away, null

nextValue   BStatusBoolean = schedule.getOutput(nextTime)
                              valor que tendrá en nextTime

outSource   String (read-only)
            = "Week: monday" | "Special Event: Christmas" | "Override"
```

### Overrides temporales (In slot)

```
In linked AND value != null:
  use In.value directamente
  outSource = "Override"
else:
  prosigue con schedule logic normal
```

Duración: override persiste mientras In linked + valor non-null. Deslinka → retorna a schedule.

---

## 24.9 Persistencia BOG (BScheduleSnapshotHandler)

```java
public final class BScheduleSnapshotHandler extends BSingleton
                                            implements BIScheduleSnapshotHandler {
  BAbstractSchedule getSnapshot(BComponent, Context);
  BAbstractSchedule saveSnapshot(BComponent, BAbstractSchedule, Context);
  BOrd getRefBaseOrd(BComponent);
  List<BDate> getHighlightedDates(BComponent, BAbstractSchedule, startDate, endDate, Context);
  List<Map<String,String>> getSummary(BComponent, BAbstractSchedule, startDate, endDate, Context);
}
```

Ciclo Workbench:
```
User edits schedule → doSaveValue() en BAbstractScheduleView
 → BScheduleSnapshotHandler.saveSnapshot()
 → BOG persistence (entityIo-rt)
 → XML/Binary storage
```

---

## 24.10 Views Workbench

### BAbstractScheduleView (base)

```java
BAbstractScheduleView extends BWbComponentView
  enableSave      Action
  save            Action
  refresh         Action
  btnSave, btnRefresh UI buttons
  doLoadValue(BObject, Context)
  doSaveValue(BObject, Context)
  doRefresh()
```

### Vistas específicas (schedule-wb)

- `BHxSpecialEventsView` — calendar grid con special events (add/edit/delete, date picker)
- `BHxTriggerScheduleView` — config trigger times + dates
- `ViewUtil` — helpers AllDay/CopyDay/PasteDay/ClearDay

---

## 24.11 Time Zones + DST

```
BAbsTime.make(year, month, day, hour, minute, second, BTimeZone tz)

Durante DST spring forward (02:00 → 03:00):
  Schedule @ 02:30 se SALTA
  triggerMissed fires en próx startup

Durante DST fall back (02:00 → 01:00):
  Schedule @ 01:30 puede disparar 2 veces (depende impl BAbsTime)
```

**GOTCHA**: Schedules UTC sin problemas DST. Schedules timezone local dependen de `BTimeZone.getTimeZone()`.

---

## 24.12 driverSchedule Framework cross-driver

### BScheduleDeviceExt (contract base)

```java
BScheduleDeviceExt extends BDescriptorDeviceExt (abstract)
  // roles: Supervisor (master) / Subordinate
  abstract BScheduleExport makeExport(String supervisorId);
  abstract BScheduleImportExt makeImportExt();

  void subscribe();  // suscripción manual subordinados
  BAbstractSchedule processImport(String supervisorId, BAbsTime subordinateVersion);
  BAbsTime           processExport(String supervisorId, BAbstractSchedule supervisor);
  static BAbsTime    getVersionOf(BAbstractSchedule sch);

  subscribeWindow   BRelTime (default 1 día)
  retryTrigger      Property (intervalo retry en fault)
```

Ciclo startup:
```
1. Tiempo aleatorio post-startup dentro subscribeWindow
2. doSubscribe() invoca execute() en cada BScheduleImportExt sin com previa
3. Drivers que no persisten (Modbus/LON) → subscribeWindow chico
4. Si retryTrigger habilitado → retry periódico exports/imports en fault
```

### BScheduleExport (Supervisor → Subordinate, push)

```java
BScheduleExport extends BDescriptor (abstract)
  supervisorId        String  (ORD supervisor)
  subordinateVersion  BAbsTime (última sync exitosa)
  executionTime       Property (trigger default OFF)

  abstract void doExecute();            // codificar + transmitir
  abstract IFuture postExecute(Action, BValue, Context);  // async enqueue

  BAbstractSchedule getExportableSchedule();   // CRÍTICO: inline refs
  BAbstractSchedule getSupervisor();
  BAbsTime getSubordinateVersion();
  void setSubordinateVersion(BAbsTime);
```

Semántica ejecución:
- Solo si `getVersionOf(supervisor) > getSubordinateVersion()`
- `getExportableSchedule()` INLINE references antes serializar (normalización)
- Actualiza subordinateVersion tras éxito

### BScheduleImportExt (Subordinate ← Supervisor, pull)

```java
BScheduleImportExt extends BDescriptor (abstract)
  supervisorId    String
  executionTime   Property (trigger default OFF — prefer supervisor push)

  abstract void doExecute();
  abstract IFuture postExecute(Action, BValue, Context);

  void importSupervisor(BAbstractSchedule supervisor);
  protected static void copyOver(BAbstractSchedule supervisor, BAbstractSchedule subordinate);
  BAbstractSchedule getSubordinate();
  String getSupervisorId();
```

---

## 24.13 Implementación por driver

### BACnet (rich support)

```
BBacnetDevice
 └ schedules: BBacnetScheduleDeviceExt implements BFoxClientConnection$Interest, BISubLicenseable

BBacnetScheduleExport extends BScheduleExport
  Props: supervisorOrd, objectId, dataType, priorityForWriting, skipWrites, writeEnumAs, outOfService
  Actions: readFromDevice(), readChangeTypeParams(), changeType(BBacnetChangeTypeParm)

BBacnetScheduleImportExt extends BScheduleImportExt
  Props: objectId, priorityForWriting (readonly)
```

Export flow:
```
Supervisor BWeeklySchedule cambia
 → getVersionOf > subordinateVersion
 → BBacnetScheduleExport.execute() enqueue async
 → doExecute(): getExportableSchedule → inline refs → ASN.1 encoding (ScheduleSupport0/4/16)
 → WriteProperty confirmed (o CreateObject si no existe, DeleteObject+CreateObject si cambio tipo)
 → Device aplica
 → setSubordinateVersion(BAbsTime.now())
 → Fault: status=FAULT, faultCause; retry en retryTrigger
```

### Niagara-Niagara (NiagaraDriver)

```
BNiagaraStation
 └ schedules: BNiagaraScheduleDeviceExt implements BFoxClientConnection$Interest, BINiagaraDeviceExt
      action submitDiscoveryJob()   // learn schedules

BNiagaraScheduleExport extends BScheduleExport
  implements BFoxClientConnection$Interest, BISubLicenseable

BNiagaraScheduleImportExt extends BScheduleImportExt
  implements BFoxClientConnection$Interest, BISubLicenseable
```

Transporte: Fox binary + compression. Station→station native serialization sin pérdida. Reconexión auto si connection cae.

### LON (limited)

No schedule nativo. Workaround: custom NV + Trend Log.
```
ScheduleExport:
  convert BAbstractSchedule → array (time-of-day + day-of-week values)
  write como LON NV via WriteProperty-equivalent
  device local aplica lógica

Limitaciones: no BCompositeSchedule, max weekly + simple special events,
              SNVT custom requerido.
```

### Modbus (simulado)

No schedule nativo. Holding registers como "time points":
```
regs 1-5:  [hh1, mm1, hh2, mm2, hh3]
reg 10:    day_mask (bitmap, 0x3F = Mon-Fri)
regs 20-30: output_values[] per time point

Export: map BDailySchedule.events[] → regs + day_mask
Import: leer regs, parsear → BDailySchedule
```

Limitaciones: weekly simple only, no atomicidad (local device puede sobrescribir), no composite.

### OPC UA (variable)

Depende del server OPC UA: algunos exponen Schedule type, otros no.
- Si Schedule type: map directo Niagara ↔ OPC UA struct
- Si no: flatten a array + metadata en custom variables

### Comparativa

| Dimensión | BACnet | Niagara-N | LON | Modbus | OPC UA |
|---|---|---|---|---|---|
| Schedule nativo | Sí (Schedule Object) | Sí (Baja) | No (NV simulado) | No (regs simulado) | Depende server |
| Richness | Alta (composite, special events) | Very High | Baja (weekly only) | Very Low (daily only) | Variable |
| Dirección sync | Bidireccional | Bidireccional nativo | Unidireccional (read) | Unidireccional | Bidireccional si server |
| Cambio detection | BAbsTime timestamp | Subscription Fox | Polling + timestamp | Polling + timestamp | OPC UA subscription |
| Atomicidad | No (por evento) | Transaccional | No | No | Depende |
| Versionado | subordinateVersion:BAbsTime | idem | Timestamp last read | Timestamp last sync | OPC UA timestamp |
| Complejidad impl | Media (ASN.1) | Baja (Baja serial) | Alta (SNVT custom) | Baja (regs) | Media |
| Pérdida info | Sí (truncation por priority) | No | Posible (overflow) | Sí (solo weekly) | Depende |

---

## 24.14 Versionado clock-based

### getVersionOf (universal BAbsTime)

```java
static BAbsTime getVersionOf(BAbstractSchedule sch);
```

Lógica execute:
```java
supervisor_time = BScheduleDeviceExt.getVersionOf(getSupervisor());
if (supervisor_time > getSubordinateVersion()) {
  execute();
  setSubordinateVersion(supervisor_time);  // tras éxito
}
```

**GOTCHA clock drift**:
- Subordinado atrasa vs supervisor → siempre sync (OK, redundante pero consistente)
- Subordinado adelanta → NUNCA sync (timestamp futuro)
- **Remedio obligatorio**: NTP sincronización entre stations

---

## 24.15 Control framework base (BControlPoint)

```java
BControlPoint extends BComponent (abstract)
  abstract BStatusValue getOutStatusValue();
  void onExecute(BStatusValue, Context);
  Property getOutProperty();
  final BPointExtension[] getExtensions();
  final void doExecute();
  abstract void onExecute(BStatusValue, Context);

  // Props:
  facets     BFacets (units, min, max, precision, enum choices)
  proxyExt   BAbstractProxyExt (binding a device)
```

Pipeline: `execute() → doExecute() → onExecute(BStatusValue, Context)`.
- Context permite suprimir ejecución (noExecuteContext)
- Extensions hook: `executeExtensions(BStatusValue, Context)` — alarm, history, control

### Subclases por tipo

```
BNumericPoint implements BINumeric
  out BStatusNumeric (read-only)
  double getNumeric(), BFacets getNumericFacets()

BBooleanPoint implements BIBoolean
  out BStatusBoolean, boolean getBoolean()

BEnumPoint
  out BStatusEnum, BEnum getEnum()

BStringPoint
  out BStatusString
```

kitControl extiende con:
```
BKitNumericPoint (hereda BNumericPoint + propagateFlags)
BKitBooleanPoint
BKitEnumPoint
```

---

## 24.16 Writable points — priority array 16 + fallback

### BNumericWritable / BBooleanWritable

Extienden BNumericPoint/BBooleanPoint + `BIWritablePoint`.

```java
Property in1, in2, ..., in16    BStatusNumeric/BStatusBoolean
Property fallback                valor si ningún nivel activo
Property overrideExpiration     BAbsTime (temporary override)
```

### BPriorityLevel enum

```
NONE                  default, no assignment
LEVEL_1 .. LEVEL_16   niveles BACnet-like
FALLBACK              después de LEVEL_16
```

Significancia típica:
```
LEVEL_1          máxima (schedule, automation crítica)
LEVEL_6-8        zona manual (operador)
LEVEL_16         mínima (default/setpoint)
FALLBACK         estático si nada en rango
```

### Actions

```java
// BNumericWritable
emergencyOverride(BDouble)      // LEVEL_2, expira con overrideExpiration
emergencyAuto()                 // restaura auto
override(BNumericOverride)      // struct con duration + value
auto()                          // clear manual override
set(BDouble)

// BBooleanWritable
active() / inactive()
emergencyActive() / emergencyInactive()
auto()
set()
```

### WritableSupport

```
getActiveLevel()     retorna BPriorityLevel ganador
getInStatusValue(BPriorityLevel)  consulta nivel específico
Selección: primer non-null/valid desde LEVEL_1 → LEVEL_16 → FALLBACK
```

### BOverride struct

```java
BOverride
  duration              BRelTime
  maxOverrideDuration   BRelTime

BNumericOverride extends BOverride
  value double
```

---

## 24.17 kitControl palette — 152 componentes

### Math (operaciones aritméticas/funcionales)

**Binaria/Quad (BQuadMath con inA, inB, inC, inD):**
- `BAdd`, `BSubtract`, `BMultiply`, `BDivide` (NaN en divide-by-zero)
- `BAverage`, `BMaximum`, `BMinimum`, `BModulus`
- `BPower` (inA^inB), `BReset` (linear reset)

**Unaria (BUnaryMath):**
- `BAbsValue`, `BNegative`, `BSquareRoot`, `BExponential`
- `BLogNatural`, `BLogBase10`, `BFactorial`
- `BSine`, `BCosine`, `BTangent`, `BArcSine`, `BArcCosine`, `BArcTangent`

**Bit operations:**
- `BNumericBitAnd`, `BNumericBitOr`, `BNumericBitXor`

**Aggregate:**
- `BMinMaxAvg` — 10 inputs (inA..inJ) → min/max/avg simultáneos (extiende BDecaInputNumeric)

### Logic (booleana)

**Quad logic (BQuadLogic con inA..inD):**
- `BAnd`, `BOr`, `BNot` (unaria), `BXor`

**Comparación (BComparison):**
- `BGreaterThan`, `BLessThan`, `BEqual`, `BGreaterThanEqual`, `BLessThanEqual`, `BNotEqual`

**Property especial en BLogic:**
- `nullOnInactive` — output null si logic inactive
- `propagateFlags` — BStatus bitmask propagación

### Timer (temporización)

- `BBooleanDelay` — delay on/off en transiciones (onDelay/offDelay BRelTime; active flags; onTimerExpired/offTimerExpired actions; Clock$Ticket scheduling)
- `BNumericDelay` — delay en cambios numéricos
- `BOneShot` — one-shot pulse en flanco
- `BCurrentTime` — output BAbsTime actual
- `BTimeDifference` — diff entre timestamps

### Select (multiplexer 10 inputs)

- `BBooleanSelect` / `BNumericSelect` / `BEnumSelect` / `BStringSelect`
- Base BMuxSwitch con `selector` (integer 0-9 o enum)

### Conversion

**Status ↔ primitive:**
- BooleanToStatusBoolean, StatusBooleanToBoolean
- Double/Float/Int/Long ToStatusNumeric y reverse
- StatusNumericToStatusEnum y reverse
- StatusEnumToStatusBoolean, EnumToStatusEnum
- StatusEnumToInt (ordinal)
- StringToStatusString, StatusStringToStatusNumeric
- StatusValueToValue (generic)

**Unit conversion:**
- `BNumericUnitConverter` — acceleration/temperature/etc

### Constants

- `BNumericConst`, `BBooleanConst`, `BEnumConst`, `BStringConst`
- `BNullValueOverrideSelect` — maneja nulls en override

### Latch (state memory)

- `BBooleanLatch` — set/reset actions, retiene entre power cycles
- `BNumericLatch` — set(BDouble)/reset()
- `BEnumLatch`, `BStringLatch`

### HVAC

- `BLoopPoint` (PID): loopEnable, controllerVariable, setpoint, executeTime, proportional/integral/derivative constants, bias, resetIntegral action. **Time-driven** via executeTime.
- `BSequence` — secuenciador (raise/lower con BLoopAction Direct/Reverse)
- `BSequenceBinary` — sequencia binarias discretas
- `BLeadLagRuntime`, `BLeadLagCycles` — compensación lead-lag
- `BRaiseLower` — control direccional
- `BMultiVibrator` — generador oscilaciones
- `BInterstartDelayControl` + `BInterstartDelayMaster` — interlock multi-device
- `BRamp`, `BRampWaveform` — rate-limiting
- `BRandom` — valores aleatorios (debug/test)
- `BTstat` — termostato con hysteresis

### Energy Management

- `BOutsideAirOptimization` — minimiza OA vs IAQ
- `BOptimizedStartStop` — anticipated start con demanda térmica
- `BDegreeDays` — heating/cooling degree days accumulator
- `BNightPurge` — cooling nocturno
- `BSetpointOffset` — incremento bajo carga
- `BSetpointLoadShed` — reducción en peak
- `BShedControl` — demand limiting
- `BElectricalDemandLimit` — limitación instantánea
- `BSlidingWindowDemandCalc` — demanda ventana móvil
- `BPsychrometric` — humedad relativa, entalpia

### Alarm Algorithms

- `BLoopAlarmAlgorithm` — detección falla loop PID
- `BDiscreteTotalizerAlarmAlgorithm` — totalización anómala
- `BChangeOfStateCountAlarmAlgorithm` — transiciones excesivas
- `BElapsedActiveTimeAlarmAlgorithm` — tiempo activo excedido

### String / Utility

- `BStringConcat`, `BStringSubstring`, `BStringLen`, `BStringIndexOf`, `BStringTest` (regex), `BStringTrim`
- `BBqlExprComponent` — expresiones BQL custom, slots dinámicos, múltiples outputs
- `BStatusDemux` — desmembra BStatusValue
- `BDigitalInputDemux` — inputs discretos
- `BNumericToBitsDemux` — divide numeric en bits

---

## 24.18 Execution framework

### Change-driven (default)

```java
public void changed(Property p, Context cx) {
  if (p == inA || p == inB) {
    calculate();  // o doExecute()
  }
}
```

Types: Math, Logic, Select, Timer-con-transición, Latch. Reactivo a inputs, no consume ciclos si estables.

### Time-driven (scheduled)

```java
// BLoopPoint.started():
scheduler = Clock.schedule(executeTime, new Runnable() {
  execute() { onExecute(status, cx); }
});
```

Types: BLoopPoint (PID ejecuta cada executeTime BRelTime). Gotcha: executeTime muy corto = overhead.

### Status propagation

BStatus flags:
```
UNACKED_ALARM    0x0001
ALARM            0x0002
FAULT            0x0004
DOWN             0x0008
STALE            0x0010
OVERRIDDEN       0x0020
NOT_CERTIFIED    0x0040
```

En math/logic con `propagateFlags = true`:
```java
BStatus inStatus = propagate(inA.getStatus());
output.setStatus(inStatus | calculatedStatus);
```

### ProxyExt binding → device

```java
BAbstractProxyExt
  void onExecute(BStatusValue, Context);
  void pointSubscribed() / pointUnsubscribed();
  void writablePointActionInvoked();
  abstract void checkStatusValueTypes();
```

Pipeline:
```
BNumericWritable.out (BStatusNumeric)
 → ProxyExt → getWriteValue(out)
 → BACnet/Modbus/etc write
 → Device (VAV, chiller, etc)
```

### Tuning policy (rate limit + deadband)

Pseudo-code write:
```java
if (newValue == lastWriteValue) return;     // dedup

timeSince = now() - lastWriteTime;
if (timeSince < minWritePeriod) { queue(newValue); }
else if (abs(newValue - lastWriteValue) < deadband) return;
else { writeToDevice(newValue); lastWriteTime = now(); }
```

Propiedades:
- `minWritePeriod` (mínimo intervalo writes, ej 1s)
- `deadband` (ej 0.5°)
- `maxWritePeriod` (máximo sin confirmar write)

**GOTCHA**: tuning agresiva + change-driven satura device bus → usar executeTime para throttle.

---

## 24.19 Binding operacional (schedule→writable→PID→device)

### Ejemplo VAV Box Temperature Control

```
┌─────────────────┐     ┌─────────────────┐
│  ProxyPoint     │     │   Schedule      │
│  TempSensor     │     │ 7am-6pm: 21°C  │
│  (read VAV)     │     │ 6pm-7am: 18°C  │
└────────┬────────┘     └────────┬────────┘
         │ 21.3°C                │ 21°C
         │                       │
         ▼                       ▼
    ┌─────────────────────────────────────┐
    │   BNumericWritable (Setpoint)       │
    │   in8: 21°C (LEVEL_8 schedule)      │
    │   in10: null (LEVEL_10 manual)      │
    │   fallback: 20°C                    │
    │   getActiveLevel() → LEVEL_8        │
    │   out: 21°C, status OK              │
    └──────────────┬──────────────────────┘
                   │
                   ▼
    ┌─────────────────────────────────────┐
    │   BLoopPoint PID                    │
    │   setpoint: 21°C                    │
    │   controllerVariable: 21.3°C        │
    │   executeTime: 0.5s (time-driven)   │
    │   error = -0.3°C                    │
    │   out: 45% valve command            │
    └──────────────┬──────────────────────┘
                   │
                   ▼
    ┌─────────────────────────────────────┐
    │   Ramp (rate limit)                 │
    │   rampTime: 2s                      │
    │   out: 45% (ramped)                 │
    └──────────────┬──────────────────────┘
                   │
                   ▼
    ┌─────────────────────────────────────┐
    │   ProxyExt (Modbus Write)           │
    │   minPeriod 1s, deadband 2%        │
    │   writes 45% → VAV damper          │
    └─────────────────────────────────────┘
```

### Timeline

```
t=0s:  Schedule 20°C → 21°C
       → WritablePoint.changed(in8)
       → onExecute() change-driven
       → getActiveLevel() = LEVEL_8, out = 21°C

t=0.5s: BLoopPoint.executeTime fires (time-driven)
       → error = -0.3°C, out = 45%
       → Ramp.changed(in)

t=0.5s+: Ramp.changed → onExecute → out = 45%
         → ProxyExt subscription → onExecute
         → tuning: timeSince > minPeriod → write to device
```

### Priority resolution (ejemplo)

```java
for (level = LEVEL_1; level <= LEVEL_16; level++) {
  value = getInStatusValue(level);
  if (value != null && value.isValid()) return value;
}
return fallback;
```

### Override workflow

```java
BNumericOverride ovr = new BNumericOverride(
  BRelTime.make("1h"),  // duration
  45.0                   // value
);
writable.override(ovr);

// Internamente:
// - WritableSupport inserta en level apropiado
// - Status OVERRIDDEN flag
// - Clock scheduler expira tras 1h
```

---

## 24.20 Execution order + cycle detection

Niagara framework durante `started()`:
1. traverse dependency graph
2. detecta ciclos → log warning o exception
3. ordena para que inputs estén listos antes `onExecute()`

Protection against recursion:
- BBooleanDelay, BLoopPoint usan Clock.schedule() para **desacoplar**, evitando stack overflow.

**Bad pattern**:
```java
public void changed(Property p, Context cx) {
  setInput(...);  // RECURSION - stack overflow
}
```

---

## 24.21 Gotchas cross-bloque

### Schedule framework

1. **BScheduleReference stale** — si calendar movido/deleted, ref apunta a location inválida. BScheduleReference mantiene CalendarSubscriber para resubscribir auto si movido. getSchedule() retorna null si permanentemente lost.
2. **Overlapping events** en BDailySchedule con union=true (OR) — primero en array gana si ambos effective (08-12 true + 10-15 false @ 11:00 → true).
3. **Workbench vs RPC** — Workbench save sincrónico (doSaveValue → saveSnapshot → modified → nextCov recalculado). RPC cambia property, cada change puede recalcular nextCov (performance hit en múltiples).
4. **Clock service dependency** — si Clock no corre, clockChanged() no llamado, triggers NUNCA disparan. Recovery automático tras restart + triggerMissed fires si within search window.
5. **scanLimit** (default 90 días) — nextEvent() puede retornar null si no hay cambio en ventana. Lower scanLimit (ej 14 días) si requiere predictability corto plazo.
6. **Union vs intersection ambiguity** — BCompositeSchedule.union=false (AND) con child alwaysEffective=true → composite SIEMPRE effective, impossible deshabilitar.

### driverSchedule

7. **Clock drift entre stations** — supervisor/subordinate clocks no sync → versionado broken. NTP obligatorio. Manual: forzar execute() action para resincronizar.
8. **Schedule update durante polling activo** — supervisor modifica mid-flight, subordinate recibe "intermedia". Próximo poll corrige. Usar `getExportableSchedule()` (captura inmutable) obligatorio.
9. **Device offline durante sync** — timeout (BACnet/Modbus/Niagara), subordinateVersion NO actualizado, status=FAULT. Retry via retryTrigger. Si device nunca regresa → schedule permanente out-of-sync.
10. **Partial state multi-event** — schedule con 10 eventos, 5 escritos antes timeout = estado inconsistente. NO rollback transaccional. Driver debe escribir como unidad atómica si posible (BACnet CreateObject) o en orden (viejos primero).
11. **Conflict resolution bidireccional** — BScheduleDeviceExt NO implementa CvC. Last-write-wins. Arquitectura clara: UN supervisor, resto subordinates.
12. **Reference inlining obligatorio** — `getExportableSchedule()` DEBE llamarse antes de transmit (inlines BScheduleReference). Omitir = subordinate recibe ords irresolubles.
13. **executionTime default**: Export ON (push changes), Import OFF (prefer push). Retry trigger global compartido.
14. **subscribeWindow tuning**: BACnet/Niagara large OK (persiste info); LON/Modbus small (hours) porque no persisten.

### Control framework

15. **execute() recursion** — A→B→A stack overflow dentro de 100 iterations. Mitigar con Clock.schedule() para desacoplar.
16. **Status propagation overhead** — `propagateFlags = ALL_FLAGS` en cada math congela output si input alarma. Set selectivo (ej solo STALE).
17. **Tuning policy flapping** — minWritePeriod=0.1s + deadband=0 + change-driven = device flood → reset device. Aumentar deadband + throttle executeTime.
18. **Priority array ambiguity** — múltiples levels activos: LEVEL_1 SIEMPRE gana (menor # = mayor prioridad). Document level assignment en commissioning.
19. **BQlExprComponent type check** — no valida loading, runtime exception. No type checking → category mismatch = null output.
20. **NullProxyExt vs null** — `BNullProxyExt.isNull()=true` evita writes (útil testing). proxyExt=null causa NullPointerException en onExecute(). SIEMPRE usar BNullProxyExt en lugar de null.
21. **BControlSchedule.scanLimit vs BTriggerSchedule.nextTriggerSearchLimit** — NO confundir. Primero para cambios continuos de output; segundo para búsqueda próximo trigger (default ambos 90 días pero semántica distinta).

### Best practices operacionales

- **Distributed logic (Philosophy B)**: kitControl components bajo device Points container (más portable que central Logic folder).
- **Explicit subscription**: `subscribe(Property.In1)` en `started()` (previene missed changes si subscriptor tarde).
- **Documentar execution semantics**: wireSheet con anotaciones level assignments.
- **Rate-limit via executeTime**: no rely solo en deadband+minWritePeriod, usar BRelTime throttle.
- **NTP crítico** en todas stations con driverSchedule activo.

---

## 24.22 Tabla resumen — clases Schedule/Control

| Clase | Extiende | Responsabilidad |
|---|---|---|
| BAbstractSchedule | BComponent | API base (isEffective/nextEvent/getOutput) |
| BTimeSchedule | BAbstractSchedule | Rango horario simple |
| BDateSchedule | BCompositeSchedule | Selector día/mes/año/weekday |
| BDateRangeSchedule | BAbstractSchedule | Rango entre dates |
| BDaySchedule | BCompositeSchedule | Container BTimeSchedule[] |
| BDailySchedule | BCompositeSchedule | Time blocks + day selector |
| BWeekSchedule | BCompositeSchedule | 7 días (Sun-Sat) BDailySchedule |
| BWeeklySchedule | BControlSchedule | Full weekly + special events + effective range |
| BCalendarSchedule | BControlSchedule | Calendar eventos especiales |
| BTriggerSchedule | BCompositeSchedule | Trigger acciones/topics |
| BCompositeSchedule | BAbstractSchedule | Combinador union/intersection |
| BControlSchedule | BCompositeSchedule | Base Weekly/Calendar (Clock sub, Workbench) |
| BAbstractScheduleSelector | BComponent | Multiplexor de schedule choice |
| BScheduleReference | BAbstractSchedule | Ref externa (inlining capable) |
| BScheduleDeviceExt | BDescriptorDeviceExt | Container export/import drivers remotos |
| BScheduleExport | BDescriptor | Mapeo supervisor→remote |
| BScheduleImportExt | BDescriptor | Mapeo remote→local |
| BScheduleSnapshotHandler | BSingleton | Persistencia BOG |
| BControlPoint | BComponent | Base control point (abstract) |
| BNumericWritable | BNumericPoint | Writable + 16 priority levels + fallback |
| BLoopPoint | BComponent | PID controller (time-driven) |

---

## Fuentes primarias leídas

1. `modules/schedule-rt.jar` — BAbstractSchedule, BWeeklySchedule, BCalendarSchedule, BTriggerSchedule, BControlSchedule, BCompositeSchedule, BScheduleSnapshotHandler
2. `modules/schedule-ux.jar` + `schedule-wb.jar` — Views (BAbstractScheduleView, BHxSpecialEventsView, BHxTriggerScheduleView)
3. `modules/control-rt.jar` — BControlPoint, BNumericPoint/Writable, BBooleanPoint/Writable, BEnumPoint, BStringPoint, BPriorityLevel, BAbstractProxyExt, WritableSupport, BOverride/BNumericOverride
4. `modules/kitControl-rt.jar` — 152 componentes: BQuadMath/UnaryMath (math), BQuadLogic/BComparison (logic), BMuxSwitch (select), BBooleanDelay/BOneShot (timer), BLoopPoint (HVAC), BOutsideAirOptimization/BOptimizedStartStop/BDemand (energy), BLoopAlarmAlgorithm/ElapsedTimeAlarm (alarm), BStringConcat/BBqlExprComponent (string)
5. `modules/kitControl-ux.jar` + `kitControl-wb.jar` — UI adapters + Workbench views
6. `modules/docKitControl-doc.jar` — documentación
7. `modules/driver-rt.jar` + `ndriver-rt.jar` — BScheduleDeviceExt, BScheduleExport, BScheduleImportExt
8. `modules/niagaraDriver-rt.jar` — BNiagaraScheduleDeviceExt/Export/ImportExt + BFoxClientConnection$Interest
9. `modules/bacnet-rt.jar` — BBacnetScheduleDeviceExt/Export/ImportExt (integración con Bloque 23)
10. `niagara-help/devguide-clean/{schedule,driverSchedule,control,kitControl}.txt`
11. `niagara-help/guides-clean/Scheduling/`

Total: ≈2000 clases decompiladas, 152 kitControl components enumerados, 3 execution models documentados (change/time-driven, priority resolution), comparativa 5 drivers (BACnet/Niagara/LON/Modbus/OPC UA) para schedule sync.
