# Niagara N4 — Mental Model · Bloque 6: Control Engine (Execution + Links + kitControl)

**Sesión**: 2026-04-22
**Distribución**: Honeywell OptimizerSupervisor-N4.14.0.162
**Método**: Investigación empírica READ-ONLY (3 sub-agents Explore en paralelo)
**Fuentes primarias**:
- `niagara-help/devguide-clean/` — execution, links, control, componentModel, station, basicDriver
- `niagara-help/source/baja/javax/baja/sys/` — Clock, BComponent, BLink, BConversionLink, Knob, BAbstractService
- `niagara-help/source/baja/javax/baja/control/` — BControlPoint, BNumericWritable, BPointExtension
- Decompilado Vineflower: `kitControl-rt.jar` (100+ bloques), `NKnob`, `NRelationKnob`

Bloque 4 definió componentes. Bloque 5 cómo navegarlos/persistirlos/consultarlos. Este bloque explica **cómo ejecutan en runtime**: cuándo corre qué, cómo fluyen datos entre ellos, y qué bloques pre-fabricados trae el framework para construir lógica de control.

---

## Tabla de contenidos

1. [Execution Engine](#61-execution-engine)
2. [Link Model y Binding](#62-link-model-y-binding)
3. [Control Points y kitControl](#63-control-points-y-kitcontrol)
4. [Síntesis del bloque](#síntesis-del-bloque)

---

## 6.1 Execution Engine

### 6.1.1 Arquitectura general — event-driven, NO scan cycle

**Fact crítico a revertir si venís de BACnet/SCADA tradicional**: Niagara **NO tiene scan cycle explícito**. El modelo es **100% event-driven**. El `devguide-clean/execution.txt` es explícito en rechazar el modelo "polling de N milisegundos".

Los eventos que disparan ejecución:
1. Cambio en una Property (`.set()`) → `changed()` callback → knobs propagan a links.
2. Invocación de una Action (`.invoke()`) → `doActionName()` ejecuta → posibles links.
3. Disparo de Topic (`.fire()`) → knobs propagan.
4. Timer (`Clock.schedulePeriodically()`) → dispara callback en engine thread.
5. I/O completion (driver framework) → propaga vía `post()`.

### 6.1.2 EngineManager + engine thread

**`com.tridium.sys.engine.EngineManager`** (singleton, accesible vía `Nre.getEngineManager()`):
- **Queue de acciones async**: `enqueueAction(BComponent, Action, BValue, Context)`.
- **Scheduler de timers**: 4 métodos en `javax.baja.sys.Clock`:
  - `schedule(Runnable, BRelTime delay)` — one-shot relativo.
  - `scheduleAt(Runnable, BAbsTime when)` — one-shot absoluto.
  - `schedulePeriodically(Runnable, BRelTime delay, BRelTime period)` — periódico relativo.
  - `schedulePeriodicallyAt(Runnable, BAbsTime first, BRelTime period)` — periódico absoluto.
- **Un único engine thread** ejecuta TODO: callbacks de lifecycle, timers, actions async, propagación de knobs.

**Ticket API**: cada schedule retorna un `Ticket` con `.cancel()`. Útil para timers cancelables (common en `unmounted()` callback).

### 6.1.3 post() y postAsync() — encolar trabajo

`BComponent.post(Action, BValue)` (L1255-1259 BComponent.java) encola invocación async. El componente no ejecuta `doAction()` en el caller thread; encola en EngineManager. El engine thread lo procesa en el próximo tick de cola.

**Uso típico**:
- Callbacks de otros threads (network I/O, UI thread) que necesitan mutar estado de componentes → `post()` primero.
- Actions con flag `ASYNC` → auto-encoladas aun si invocadas sincrónicamente.

### 6.1.4 Coalescing — dedup de acciones

El EngineManager **deduplica acciones por `(component, action)` per ciclo**. Si el mismo `doSomething()` se postea 5 veces antes del next tick, se ejecuta **una sola vez**.

Esto previene:
- Storms de actions disparadas por loops de links rápidos.
- Re-ejecuciones redundantes de lógica costosa.

**Importante**: los **Property changes NO se coalescen** (cada `.set()` dispara `changed()`). Solo las actions. Si necesitás coalescing de propagación en loops, usar ASYNC flag en las actions destino.

### 6.1.5 Threading model y thread safety

**Regla dura**: toda mutación a estado de BComponent **debe correr en engine thread**.
- Callbacks (`started()`, `changed()`, `mounted()`, `clockChanged()`, timers, topics) → engine thread. Safe por default.
- Código invocado desde otros threads (FOX workers, driver I/O, UI thread) → usar `post()` / `postAsync()` para volver al engine thread antes de mutar.

**Consecuencia**: un callback lento bloquea al engine thread completo → todo el station "se congela" hasta que retorna. Anti-patrones:
- Llamadas de red sincrónicas dentro de `changed()`.
- `Thread.sleep()` en callbacks.
- Locks globales que podrían tardar.

**Solución estándar** (patrón `basicDriver-rt`):
- **Dispatcher** (sync, callback de `changed()`) — hace `enqueue(workItem)`.
- **Worker thread** (pool custom, no engine) — hace I/O, luego `post()` al componente cuando termina.
- **Write Worker** (async, coalesce múltiples writes al mismo device) — bufferea.

### 6.1.6 Topology sort para links — NO hay

Claim empírico confirmado: **Niagara NO computa topological sort automático de links**.
- Links directos se propagan FIFO en el callstack (depth-first).
- Links indirectos (via ORD) se cargan en orden de aparición en BOG (`LoadOp.loadKnobs()`).
- Ciclos A→B→A son responsabilidad del developer resolver, no del framework.

**Consecuencia práctica**: en una cadena A→B→C→D, si A cambia, la propagación es recursiva en el mismo thread stack. Si la cadena es larga O hay loops, **stack overflow** es un riesgo real.

**Protección**: usar `Flags.ASYNC` (0x10) en actions destino de links. Esto hace que la invocación del target se encole en EngineManager (thread pool) en vez de ejecutar en stack del source. Corta la recursión.

### 6.1.7 Clock service — ticks vs millis

**`Clock.ticks()`**: **monotónico**, inmune a cambios del reloj del sistema. Base para medir duraciones relativas ("hace cuánto que hice X"). Nunca salta.

**`Clock.millis()`**: wrapper sobre `System.currentTimeMillis()`. **Puede saltar** si el sysadmin cambia la hora o NTP sincroniza. Usar solo para fechas absolutas.

**Callback `clockChanged(BRelTime shift)`** en BComponent (L219 BComponent.java): invocado cuando el reloj salta. Componentes que cachean timestamps absolutos (`BAbsTime`) deben recompute sus cálculos basados en tiempo absoluto acá.

### 6.1.8 AppManager y servicios

**`javax.baja.sys.BAbstractService`**: base abstract para servicios de station (AlarmService, HistoryService, UserService, etc.).

Lifecycle específico de services:
- `BServiceContainer` mantiene ordered list.
- En `station.start()`, services se inician en orden de dependencia (resuelto por `@ServiceDependencies` annotations — si existen).
- `started()` callback particular del service puede instalar timers, hooks globales.

**Spy pages** (`/spy/sysManagers/`): cada AppManager típicamente expone estado runtime.

---

## 6.2 Link Model y binding

### 6.2.1 Taxonomía de links

BLink (javax.baja.sys.BLink, BStruct persistible) es el mecanismo de propagación. Existen 6 combinaciones source→target:

| Source | Target | Semántica |
|--------|--------|-----------|
| Property | Property | Data binding — target = copy de source value |
| Property | Action | Trigger — source cambia → invoca action con value como arg |
| Action | Action | Action chaining — invocación de source → invoca target |
| Action | Topic | Event forwarding — action invocada → dispara topic |
| Topic | Action | Event handler — topic fires → invoca action con event como arg |
| Topic | Topic | Topic chaining — topic dispara → dispara target |

**Subclases**:
- `BStdLink` — link estándar sin conversión (la mayoría).
- `BConversionLink` — aplica `BConverter.convert(source, targetDefault)` antes de copiar. Uses: °C→°F, escala 0-100 ↔ -40-120, multiplier/divisor, etc.

### 6.2.2 Binding semantics

**Dirección invariante**: source es pasivo (no sabe que está linked), target es activo (tiene el link como propiedad).

**Cuándo dispara**:
- Property→Property: cuando source cambia vía `.set()`. Propagate hook recorre knobs del source.
- Property→Action: mismo momento.
- Action→Action/Topic: cuando `.invoke()` del source se ejecuta.
- Topic→Action/Topic: cuando `.fire()` del source se dispara.

**Initial propagation**: al activar un link property→property, target recibe valor actual del source inmediatamente (no espera al próximo cambio). Setup-friendly.

**Coalescing**: property changes NO se coalescen. 10 `.set()` en 1ms → 10 propagaciones. Las actions destino SÍ (si tienen flag ASYNC).

### 6.2.3 Knobs — kernel runtime state

**BLink** (persistido) vs **Knob** (runtime):
- **BLink** es BStruct, vive en config.bog. Contiene `sourceOrd`, `sourceSlotName`, `targetSlotName`, `enabled`.
- **Knob** (interface `javax.baja.sys.Knob`, implementación `com.tridium.sys.engine.NKnob`) es el reflejo del link en el source. Nunca persistido.
  - Se instala en el source via `ComponentSlotMap.installKnob(link)` al activar.
  - Es el "mirror" del link — source tiene N knobs (uno por link inbound hacia él).
  - Cuando source dispara evento, cada knob llama `link.propagate()`.

Un link disabled/deactivated existe pero SIN knob. No propaga.

### 6.2.4 Lifecycle de link

**Creación**:
- **Directa**: `new BLink(BComponent source, Slot srcSlot, Slot tgtSlot)` — referencia Java directa. En constructores programáticos.
- **Indirecta**: `new BLink(BOrd sourceOrd, String srcSlotName, String tgtSlotName, enabled)` — ORD-based. Estándar en config persistido.

**Activación (`link.activate()`)**:
1. Valida parent != null.
2. Si indirecto: resuelve ORD.
3. Valida **unlinkability** — chequea flags `BIUnlinkableTarget`, `BIUnlinkableSource`. Slots como `BPassword`, `BPermissionsMap` no pueden linkearse; si falla, log warning + auto-remove.
4. Valida type access (coercion a nivel slot; coercion de value es del converter).
5. Instala knob en source.
6. Setea flag `Flags.LINK_TARGET` en target slot (hint UI).
7. Si property→property: propagate inicial.
8. Incrementa contador global de links (hay límite per station).

**Activación automática**:
- En constructor del parent: `target.linkTo(name, source, srcSlot, tgtSlot)`.
- En startup: `loadKnobs()` tras deserializar BOG.
- En agregación dinámica: auto-activa si se agrega BLink a running component.

**Deactivation**:
1. Si indirecto: descarta resolved source y slots.
2. Uninstall knob.
3. Notifica target (`deactivating()`) para cleanup.
4. Recomputa `LINK_TARGET` flag en target (limpia si no quedan más links).

**Removal automático**: si source de link indirecto unmounts, link se auto-deactiva y quita.

### 6.2.5 Type coercion y conversion

**Coercion a nivel slot** (`propagatePropertyToProperty()` L730-759 BLink.java):
- Switch sobre `Slot.getTypeAccess()`: BOOLEAN_TYPE, INT_TYPE, LONG_TYPE, FLOAT_TYPE, DOUBLE_TYPE, STRING_TYPE, BOBJECT_TYPE.
- Elevation automática: int→double, float→double via `t.setDouble(tProp, s.getInt(sProp), null)`.
- Mismatch incompatible (string→double): exception.

**BConverter** (BConversionLink):
- Interfaz: `abstract BValue convert(BValue source, BValue targetDefault)`.
- Subclases: `BNullConverter`, aritméticas (multiply, divide, offset), unidades (C↔F), escala.
- Aplicación: L170 BConversionLink.java (property→property), L152-154 (property→action).
- Registry: converters auto-sugeridos por Workbench al configurar conversion link.

**Status handling**: cuando source es BStatusValue (ej. BStatusNumeric), status bits (disabled, fault, down, alarm, stale, null) se copian junto con el valor. Propagación de "salud".

### 6.2.6 Priority arrays — 16-level BACnet-like

**Modelo**: `BControlWritable` (BNumericWritable, BBooleanWritable, BEnumWritable, BStringWritable) expone **16 slots de entrada** (in1..in16) más `fallback`.

**Cálculo del output**:
1. Escanear in1 → in16 en orden.
2. Primer level SIN status bits {disabled, fault, down, stale, null} = ganador.
3. `out.value = input[level].value`, `out.status = input[level].status | overridden(si level ∈ {1,8})`.
4. Si todos 16 invalid → `out = fallback` (que SÍ puede ser null).
5. Level ganador se expone en facet `activeLevel` del output.

**Niveles semánticos**:
- **Level 1 (emergency)**: override manual emergencia. Permanente hasta `emergencyAuto()`. Flags READONLY+TRANSIENT, PERO excepción: **level 1 SÍ persiste en BOG**.
- **Levels 2-7**: drivers, automatismos, señales externas.
- **Level 8 (manual override)**: override operador, timed o untimed. Flags READONLY+TRANSIENT, **sí persiste en BOG**.
- **Levels 9-16**: inputs de menor prioridad (drivers secundarios, defaults).
- **Fallback**: default si todos invalidan.

**Actions expuestas**:
- `emergencyOverride(value)` → set in1.
- `emergencyAuto()` → reset in1, pasa al siguiente valid.
- `override(value, ttl)` → set in8 con timeout opcional.
- `auto()` → reset in8.
- `set(value)` → setea fallback (o equivalent; drivers típicamente escriben in2).

**`overrideExpiration`** (BAbsTime): indica cuándo expira timed override en level 8. Framework verifica en cada execute(); auto-anula si pasó.

**Ventaja arquitectónica**: N sistemas (driver, loop, manual, safety) escriben a levels distintos sin conflicto. Más prioritario gana. No hay arbitration lógica necesaria.

### 6.2.7 Cycles, feedback, loop detection

**Detección de ciclos**: NO existe global. Cada link es instance independiente.

**Self-link detection**: BLink.propagate() (L665-669) detecta target==source ∧ targetSlot==sourceSlot → log warning, return (no propaga). Pero A→B→A NO se detecta.

**Riesgos de feedback loop**:
- Stack overflow si propagación sincrónica recursiva es profunda.
- CPU 100% si loop no tiene damping.

**Mitigaciones**:
- **`Flags.ASYNC`** en actions destino de links: invocación se encola, corta recursión.
- Usar `BConversionLink` con hysteresis/deadband.
- Diseñar con state machines explícitas en vez de feedback implícito.

### 6.2.8 BRelation vs BLink

**BLink** = binding operacional. Propaga cambios. Activa/desactiva. Knob en source. Semantics: "A es data source, B reacciona".

**BRelation** (entity model, N4.x) = relación semántica. NO propaga. Almacena `relationId` (ej. `hs:chilledWaterPlantRef`), `sourceOrd` (endpoint), `inbound` (dirección), `relationTags` (metadata). Sin knob; es estructura tag-like. Semantics: "A está relacionado con B de forma X". Consumido por NEQL traversals, no por execution engine.

**Persistencia**: ambas en BOG. LoadOp los maneja distinto: links en `loadKnobs()`, relations en `loadRelationKnobs()`.

**Coexisten**: mismo componente puede tener ambas.

---

## 6.3 Control Points y kitControl

### 6.3.1 Taxonomía de ControlPoint

`BControlPoint` (extends BComponent, implements BIStatusValue) es la raíz. Toda subclase tiene property **`out`** de tipo BStatusValue.

8 tipos normalizados: 4 tipos de datos × 2 modos (RO vs Writable):

| Tipo | Modo | Data type de out |
|------|------|------------------|
| `BBooleanPoint` | Readonly | BStatusBoolean |
| `BBooleanWritable` | Writable | BStatusBoolean |
| `BNumericPoint` | Readonly | BStatusNumeric |
| `BNumericWritable` | Writable | BStatusNumeric |
| `BEnumPoint` | Readonly | BStatusEnum |
| `BEnumWritable` | Writable | BStatusEnum |
| `BStringPoint` | Readonly | BStatusString |
| `BStringWritable` | Writable | BStatusString |

**Readonly**: solo **out** y **proxyExt**. Lectura desde device externo (driver) o cálculo.

**Writable**: 16 inputs + fallback (ver 6.2.6). Acepta comandos de múltiples fuentes arbitrados por prioridad.

### 6.3.2 ProxyExt — bridge a dispositivo externo

Todo ControlPoint tiene property `proxyExt` (BAbstractProxyExt o BNullProxyExt).

- Si `proxyExt == BNullProxyExt` → punto no es proxy (valor calculado o hardcoded).
- Si `proxyExt` es otra subclase → punto es proxy de device externo. El driver específico (bacnet, modbus, etc.) provee la subclase.

**Pipeline**: `proxyExt.onExecute()` es la **primera** extension ejecutada cada ciclo. Recibe el valor de escritura calculado (en writable) y devuelve el valor leído del device (o actualiza status).

Bloque 7 profundiza el driver framework y ProxyExt implementation.

### 6.3.3 kitControl — tabla de bloques por categoría

`kitControl-rt.jar` trae 100+ bloques pre-fabricados. Se componen via links (no código Java).

**Matemática** (~20): `BAdd` (hasta 4 inputs), `BSubtract` (A-B), `BMultiply`, `BDivide` (con /0 → fault), `BAbsValue`, `BMinimum`, `BMaximum`, `BAverage`, `BNegative`, `BSquareRoot`, `BPower`, `BModulus`, `BLogNatural`, `BLogBase10`, `BExponential`, `BSine`, `BArcSine`, `BCosine`, `BArcCosine`, `BTangent`, `BArcTangent`.

**Lógica booleana** (~12): `BAnd` (hasta 4), `BOr`, `BNot`, `BXor`, `BGreaterThan`, `BGreaterThanEqual`, `BLessThan`, `BLessThanEqual`, `BEqual`, `BNotEqual` (comparaciones numéricas devuelven booleano).

**Control** (~6): `BLoopPoint` (PID con proportional/integral/derivative constants, loopAction direct/reverse, disableAction), `BRaiseLower`, `BSequence`, `BSequenceBinary`, `BSequenceLinear` (rampa, útil HVAC), `BTstat` (termostato simple con occupancy/setpoint).

**Timing** (~8): `BNumericDelay` / `BBooleanDelay` (retraso configurable), `BOneShot` (pulso al edge), `BCounter` (cuenta pulsos con reset), `BCurrentTime`, `BTimeDifference` (entre dos BAbsTime), `BMultiVibrator` (oscilador a frecuencia fija).

**Conversión** (~20): `BNumericUnitConverter` (via BUnit catalog), `BBooleanToStatusBoolean`, `BStatusNumericToDouble` (null si status invalid), `BEnumToStatusEnum`, `BStatusEnumToStatusNumeric`, `BStatusStringToStatusNumeric`, `BIntToStatusNumeric`, `BFloatToStatusNumeric`, y más.

**Selección/Multiplexión** (~8): `BNumericSelect` (in1..in4 + index), `BNumericSwitch` (if/else), `BBooleanSelect`, `BBooleanSwitch`, `BEnumSelect`, `BNullValueOverrideSelect` (primary/fallback).

**HVAC especializado** (~8): `BOptimizedStartStop` (inicio optimizado), `BNightPurge`, `BOutsideAirOptimization` (damper control), `BPsychrometric` (enthalpy/density del aire), `BDegreeDays` (acumulador grados-día), `BElectricalDemandLimit` (peak shaving), `BSetpointOffset`.

**Energía** (~3): `BSlidingWindowDemandCalc` (kW avg en ventana), `BShedControl`, `BLeadLagCycles` (lead/lag rotation).

**Latch y constantes** (~10): `BNumericLatch` (retiene hasta trigger), `BBooleanLatch` (RS), `BNumericConst`, `BBooleanConst`, `BEnumConst`, `BStringConst`.

**Demultiplexión** (~5): `BNumericToBitsDemux` (int → bit0..bit15), `BStatusDemux` (separa value + status flags), `BDigitalInputDemux`.

**String** (~5): `BStringConcat` (hasta 4 inputs), `BStringLen`, `BStringSubstring`, `BStringIndexOf`, `BStringTrim`.

**Regla de null/invalid**: bloques con múltiples inputs (inA..inD) manejan elegantemente — si todos invalid → output null; si ≥1 valid → procesa esos.

### 6.3.4 Extensions — Alarm, History, Totalizer, Statistics

Todo ControlPoint puede tener múltiples **extensions** como propiedades dinámicas (subclases de `BPointExtension`). Se ejecutan en orden declarativo tras el punto principal.

**`BPointExtension`** (base):
- `onExecute(BStatusValue out, Context cx)` — invocado cada ciclo; puede modificar status.
- `pointSubscribed() / pointUnsubscribed()` — hooks lifecycle.
- `getParentPoint()` — accesor parent.

**Extensions en control-rt**:

| Extension | Rol | Props clave |
|-----------|-----|-------------|
| `BAbstractProxyExt` | Sincroniza con device externo (driver) | `onExecute()` devuelve valor leído |
| `BNumericTotalizerExt` | Integra valor sobre tiempo | `total`, `totalizationInterval` (minutely/hourly/daily), `resetTotal()` action |
| `BDiscreteTotalizerExt` | Cuenta cambios de estado booleano | `count`, `resetCount()` |

**Extensions en control-wb / ux** (cargadas condicionalmente):

| Extension | Rol |
|-----------|-----|
| `BAlarmSourceExt` | Monitorea condiciones, activa alarmas (linked a AlarmService) |
| `BIntervalHistoryExt` | Graba valores a intervalo fijo (`recordingInterval`, `historyId`) |
| `BCovHistoryExt` | Graba solo cambios (Change of Value) (`covThreshold`, `historyId`) |

**Composición**: extensions se procesan en orden declarado. Reorderables dinámicamente en Workbench.

### 6.3.5 Facets en control points

BFacets controla rendering UI y validación.

**Numéricos** (`BFacets.makeNumeric(unit, precision, min, max)`):
- `units` — "°F", "psi", "kW". BNumericUnitConverter los usa.
- `precision` — decimales mostrados.
- `min`/`max` — límites validación UI.

**Booleanos** (`BFacets.makeBoolean(trueText, falseText)`):
- `trueText` — "On", "Occupied", "Active".
- `falseText` — "Off", "Vacant", "Inactive".

**Genéricos**: `multiLine`, `editable`, `visible`.

Herencia: `BControlPoint.facets` → `out`. Overridable per input en `inputFacets` property de writables.

### 6.3.6 Override / manualAutoMode semantics

Ya cubierto en 6.2.6 (priority arrays). Resumen:

- Level 1 emergency: `emergencyOverride(v)` / `emergencyAuto()`. Persiste en BOG.
- Level 8 manual: `override(v, ttl)` / `auto()`. `overrideExpiration` auto-anula al expirar.
- Drivers escriben level 2 (default).
- Output tiene bit `overridden` en status si level ∈ {1,8}.
- ProxyExt recibe notificación `writablePointActionInvoked()` para forzar write inmediato al device cuando se invoca level 1 u 8.

### 6.3.7 Control Program — no hay "BProgram" explícito

Niagara NO expone un BProgram formal como lenguaje scripting built-in del control. El paradigma es **componencial**:
- Bloques kitControl se enlazan via Properties (links).
- Data flow es declarativo, no imperativo.
- Cambios propagan a través de cadena de links.

Para lógica más avanzada:
- Crear módulo custom con BComponent propios.
- Usar bloques generadores (BRamp, BRandom, BSineWave) + lógica kitControl.
- Componer árboles (BAdd(BMultiply(sensor, gain), BLoopPoint(...))).

Existe un `control-rt` package con clases de scripting adicional, pero no es el camino mainstream en N4.

---

## Síntesis del bloque

### Modelo mental consolidado

**Execution engine** es event-driven puro, single-threaded en engine thread, con coalescing de actions y timers via EngineManager. **NO hay scan cycle, NO hay topological sort** — es responsabilidad del developer diseñar cadenas sin ciclos y usar `Flags.ASYNC` donde haga falta.

**Link model** es el ADN del framework: BLinks persistidos configuran knobs (runtime state) que propagan cambios source→target. 6 tipos según source/target. Coercion automática simple (int→double), custom via BConverter. Priority arrays 16-level implementan arbitración de writes BACnet-like.

**Control points** son la API pública del control: 8 tipos normalizados (Boolean/Numeric/Enum/String × RO/Writable), con ProxyExt para integrarse con drivers externos. **kitControl** provee ~100 bloques de procesamiento pre-fabricados. Extensions (Totalizer, History, Alarm) agregan funcionalidad transversal sin recompilar.

### Conexiones con bloques anteriores

- **Bloque 4 (Baja Object Model)**: Slots (Property/Action/Topic), flags (ASYNC, LINK_TARGET), callbacks lifecycle son el substrato sobre el que corre toda la ejecución.
- **Bloque 5 (ORD + BOG)**: Links indirectos usan ORDs para referenciar sources. BOG persiste links + priority array values. LoadOp `loadKnobs()` reactiva knobs post-deserialización.
- **Bloque 3 (Security)**: permisos se chequean al invocar actions. Un link puede fallar activación si el user context no tiene permiso sobre source o target.

### Gotchas críticos

1. **Single engine thread** → un callback lento congela el station. Separar I/O en worker threads (patrón dispatcher→worker→write).
2. **Property changes NO coalescen** (solo actions). Alta frecuencia de source → alta propagación.
3. **Stack overflow en loops de links** sin ASYNC. Usar ASYNC o rediseñar.
4. **Clock.ticks() vs Clock.millis()** — nunca mezclar. Ticks = monotónico para duraciones; millis = wall clock para timestamps.
5. **ProxyExt es la primera extension**, sus effects preceden a History/Alarm/Totalizer extensions.
6. **Priority level 1 y 8 persisten en BOG** (única excepción al flag TRANSIENT de los 16 inputs).
7. **Writable con todos 16 invalid + fallback null** → output null con status invalid. Verificar fallback en diseño.
8. **kitControl blocks con múltiples inputs validan por ≥1 valid** — no todos requeridos. Comportamiento tolerante.

### Qué habilita

Con Bloques 1-6 podés:
- Leer código de un módulo kitControl y entender qué hace un BLoopPoint línea por línea.
- Diseñar control logic componiendo bloques sin escribir Java.
- Debuggear un loop de feedback que sobrecalienta CPU.
- Entender un wire sheet Workbench completamente (properties + links + priority).
- Reasonar sobre concurrency bugs en callbacks.

**Todavía no podés** con Bloques 1-6:
- Implementar un driver custom (Bloque 7).
- Entender el pipeline de alarmas end-to-end (Bloque 8).
- Escribir una Px view o BajaScript client (Bloque 9).

**Próximo**: Bloque 7 — Drivers Framework.

---

## Engram topic keys generados por este bloque

- `niagara/execution/engine-thread-model` — EngineManager, Clock, post/postAsync, coalescing, threading rules.
- `niagara/execution/link-model-binding` — BLink taxonomy, knobs, lifecycle, coercion, priority arrays, cycles.
- `niagara/control/kitcontrol-blocks` — ControlPoint taxonomy, ProxyExt, ~100 bloques kitControl, extensions, facets, override semantics.

---

**Sesión cerrada**: 2026-04-22 — Bloque 6 consolidado.
