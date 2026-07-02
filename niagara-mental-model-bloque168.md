# Block 168 — chihuahua MX60 (`-rt/-ux`): protección/thresholds (slots BChiUp, control-tick 10s, allowlist de escritura)

> **WHAT:** la MÁQUINA DE ESTADOS de protección/thresholds del módulo de autoría propia `com.angeles.chihuahua`
> (dashboard Niagara N4 para el BMS **Honeywell MX60**). Documenta: qué thresholds existen por tipo de equipo
> (UP / Carcamo / Datalogger), el control-tick de 10 s que evalúa protecciones y hace LATCH permanente en la
> brecha, el `ReentrantLock` per-Ord que coordina el tick con las escrituras de setpoint/threshold (contención →
> 423 conceptual), y el allowlist + guards de `ChiThresholdHelper` que restringen qué slots puede escribir la API.
> Esta es profundidad de CONTROL DE DOMINIO que Reflow (dashboard genérico) no tiene.
>
> Focus: **chihuahua** (arquitectura del módulo MX60, fuente primaria — NO decompilado). Corpus language: Spanish.
>
> Sources (fuente primaria, base `…/Cliente/Honeywell/MX60/chihuahua/chihuahua/`):
> - `RT/` = `chihuahua-rt/src/com/angeles/chihuahua/`  ·  `UX/` = `chihuahua-ux/src/com/angeles/chihuahua/ux/`
>
> Markers: `[CERT]` = leído en la fuente primaria (`file:line`) · `[INFER]` = deducción. Capa 26 (módulo dashboard
> OEM de autoría propia). `.env.local` (IP JACE / credenciales) NO se leyó ni se cita.
>
> Continúa [Block 163] (esqueleto del focus: identidad, servlet HTTP, postura RBAC).

---

## 168.1 — Panorama: dos planos de "threshold" que NO se deben confundir

El módulo tiene **dos mecanismos de umbral con semántica distinta**, y sólo UNO es una máquina de control activa:

| Plano | Equipo | Slots | ¿Lo evalúa el control-tick? | Efecto |
|---|---|---|---|---|
| **Protección activa** (trip + latch) | `BChiUp` (unidad paquete HVAC) | 7 thresholds de `sobrecarga*` / `antifrezze*` | **SÍ** — `applyProtections()` cada 10 s + COV | Escribe `false` en `fanCmd`/`comp1Cmd`/`comp2Cmd` → apaga actuadores físicos, y LATCHea |
| **Umbral de estado** (advertencia/crítico) | `BChiCarcamo`, `BChiDatalogger` | `umbralAdvertencia`, `umbralCritico` | **NO** | Sólo se almacena/lee; la clasificación advertencia/crítico se resuelve fuera del tick `[INFER]` |

Es decir: los umbrales de Carcamo/Datalogger son **datos de comparación** (para colorear estado en la UI), NO
disparan comandos. Sólo los 7 thresholds del `BChiUp` mueven relés. Este es el corazón del bloque.

---

## 168.2 — Inventario de slots del `BChiUp` relevantes a protección

`BChiUp` es un contenedor de propiedades (sin lógica en la clase; la lógica vive en el service). Slots relevantes:

**Thresholds escribibles (7)** — `[CERT]` RT `components/BChiUp.java:224-265`:

| Slot | Tipo | Rol en la protección |
|---|---|---|
| `sobrecargaFan` | `StatusNumeric` | sobrecorriente ventilador → cascada full-stop |
| `sobrecargaCompresor1` | `StatusNumeric` | sobrecorriente compresor 1 |
| `sobrecargaCompresor2` | `StatusNumeric` | sobrecorriente compresor 2 |
| `sobrecargaAbanicos1` | `StatusNumeric` | sobrecorriente abanicos circuito 1 |
| `sobrecargaAbanicos2` | `StatusNumeric` | sobrecorriente abanicos circuito 2 |
| `antifrezzeSistema1` | `StatusNumeric` | anticongelamiento succión 1 (sic: "frezze") |
| `antifrezzeSistema2` | `StatusNumeric` | anticongelamiento succión 2 |

**Slots de salida de protección (8 `StatusBoolean`)** — `[CERT]` RT `BChiUp.java:280-327`:
`protFanActive`, `protCompresor1Active`, `protCompresor2Active`, `protAbanicosS1Active`, `protAbanicosS2Active`,
`protAntifrezzeS1Active`, `protAntifrezzeS2Active`, `protFaseActive`. Cada uno = `true` cuando su latch está
activo; se cablean en Workbench a canales de salida física. (Añadidos por el change `chihuahua-reflow-sanluis-replica`
C4 — `[CERT]` RT `BChiUp.java:275-278`.)

**Feedback booleano de protección (read-only)** — `[CERT]` RT `BChiUp.java:160-189`:
`protectorFase` (default `true`), `switchAlta1/2`, `switchBaja1/2` — estados de switches físicos de alta/baja
presión y protector de fase (entradas, no salidas).

**Persistencia del latch** — `alarmLatches` (`baja:String`, default `"{}"`) — `[CERT]` RT `BChiUp.java:335-340`.
Mapa JSON hand-rolled: `{"<thresholdKey>":{"latched":true,"latchedAt":<epochMs>,"latchedBy":"<user>","note":"<text>"}}`.
Política multi-usuario: last-write-wins v1. Es la **única fuente de verdad durable** del latch (sobrevive reinicio en `.bog`).

**Acción de reset** — `@NiagaraAction resetAlarmas` — `[CERT]` RT `BChiUp.java:358-360` (primera `@NiagaraAction`
del codebase chihuahua). Reset permanente y explícito del operador.

**Setpoint** — `effectiveSetpoint` (READONLY, default 18.0) `[CERT]` `BChiUp.java:268-273` y `setpointSchedule`
(READONLY mirror del `BNumericSchedule` hijo) `[CERT]` `BChiUp.java:347-352`. Estos son objeto del [Block 163+]
sobre setpoint/schedule; aquí importan sólo porque comparten el mismo lock per-Ord (§168.6).

---

## 168.3 — Los dos mapas inmutables que definen el ruteo latch→slot

`BChiUp` declara dos estructuras estáticas de sólo-lectura que gobiernan cómo un latch se refleja:

- **`LATCH_TO_PROT_SLOT`** — `[CERT]` RT `BChiUp.java:1549-1563`. `LinkedHashMap` de 8 entradas: cada
  `thresholdKey` → nombre de su slot `protXActive`. Incluye `proteccionFase → protFaseActive`. Orden de inserción
  preservado para serialización determinista.
- **`PROT_SLOTS`** — `[CERT]` RT `BChiUp.java:1575-1586`. `LinkedHashSet` de 5 nombres de slots de amperaje
  (`ampFan`, `ampCompresor1`, `ampCompresor2`, `ampAbanicos1`, `ampAbanicos2`) cuya escritura COV dispara
  evaluación instantánea (§168.5).

Nota de asimetría clave: el service tiene un TERCER mapa, **`LATCH_TO_TRIPPED_KEY`** (`[CERT]` RT
`BChiDashboardService.java:756-770`), de sólo **7** entradas — mapea `thresholdKey` → clave corta interna
(`"comp1"`, `"fan"`, `"antifrezze1"`, `"abanicos1"`, …). `proteccionFase` **NO** está en este mapa: el protector
de fase se refleja en `protFaseActive` (vía `LATCH_TO_PROT_SLOT`) pero **no lo gestiona `applyProtections()`** — es
una entrada externa, no un trip calculado.

---

## 168.4 — El control-tick de 10 s: cadencia y qué hace

**Cadencia** — `[CERT]` RT `BChiDashboardService.java:314-324`: en `started()`, un
`ScheduledExecutorService.scheduleAtFixedRate(controlTick, 10L, 10L, TimeUnit.SECONDS)` (thread daemon
`"chihuahua-controlTick"`). Se eligió `ScheduledExecutorService` en vez de `BRelTime` por portabilidad entre
versiones Niagara (iSMA 4.13.x / Honeywell 4.14.x) `[CERT]` `BChiDashboardService.java:299-302`. Se cancela en
`stopped()` `[CERT]` `:442-465`.

**Qué hace cada tick** — `[CERT]` RT `BChiDashboardService.java:788-833`, itera las 6 `Planta*` → `UpMonitor` →
cada `BChiUp`:

1. `evaluateUp(up, "TICK")` por cada UP `[CERT]` `:809`.
2. Cada 60º tick (~10 min): `_purgeAlarmLatches()` `[CERT]` `:815-819` — purga latches con `latchedAt` mayor a
   **30 días** (`ALARM_LATCH_MAX_AGE_MS` `[CERT]` `:783`; la purga per-UP vive en `BChiUp.purgeAlarmLatches()`
   `[CERT]` RT `BChiUp.java:1777-1880`).
3. `_recomputeScheduleSetpoints()` `[CERT]` `:824` — fallback de polling de setpoint SCHEDULE.
4. `_syncProtectionSlots()` `[CERT]` `:827` — sincroniza los 8 `protXActive` desde `alarmLatches` JSON
   (delega en `BChiUp.syncProtectionSlots()` `[CERT]` RT `BChiUp.java:1974-1996`).

**Reconciliación de arranque** — `[CERT]` RT `BChiDashboardService.java:352-439`: en `started()`, ANTES del primer
tick (que dispara a T+10 s), reconstruye `trippedFlags` en memoria parseando el `alarmLatches` durable de cada UP.
Sin esto habría una ventana ~10 s de enforcement perdido tras reinicio (los flags en RAM arrancan vacíos pero el
`.bog` conserva latches previos).

---

## 168.5 — `applyProtections()`: la evaluación y el LATCH PERMANENTE

`evaluateUp()` `[CERT]` RT `BChiDashboardService.java:1006-1044`:
- **Skip si `modoOperacion == "MANUAL"`** — el operador tiene override absoluto `[CERT]` `:1010-1012`.
- Adquiere el lock per-Ord `acquireLock(ord, 500)`; si timeout → salta este ciclo `[CERT]` `:1015-1022`.
- Bajo el lock: `applyProtections(up, ord, source)` `[CERT]` `:1025`.

`applyProtections()` `[CERT]` RT `BChiDashboardService.java:1047-1211`. Lee thresholds (0.0 = unset → **skip**,
defense-in-depth) y valores vivos, y aplica cada regla con guardia de flanco de subida `!tripped.contains(key)`:

| Protección | Condición de trip | Acción (comandos apagados) | Auto-latch | `[CERT]` |
|---|---|---|---|---|
| Antifreeze 1 | `tempSuccion1 < antifrezzeSistema1` | `fanCmd`, `comp1Cmd` = false | `antifrezzeSistema1` | `:1076-1091` |
| Antifreeze 2 | `tempSuccion2 < antifrezzeSistema2` | `fanCmd`, `comp2Cmd` = false | `antifrezzeSistema2` | `:1094-1108` |
| Sobrecarga comp 1 | `ampCompresor1 > sobrecargaCompresor1` | `comp1Cmd` = false | `sobrecargaCompresor1` | `:1111-1124` |
| Sobrecarga comp 2 | `ampCompresor2 > sobrecargaCompresor2` | `comp2Cmd` = false | `sobrecargaCompresor2` | `:1127-1140` |
| **Sobrecarga fan** | `ampFan > sobrecargaFan` | `fan`+`comp1`+`comp2` = false (**cascada full-stop**) | 3 keys (fan+comp1+comp2) | `:1145-1168` |
| Sobrecarga abanicos 1 | `ampAbanicos1 > sobrecargaAbanicos1` | `comp1Cmd` = false (**fan NO**, decisión operador 2026-05-17) | 2 keys (abanicos1+comp1) | `:1173-1189` |
| Sobrecarga abanicos 2 | `ampAbanicos2 > sobrecargaAbanicos2` | `comp2Cmd` = false (**fan NO**) | 2 keys (abanicos2+comp2) | `:1194-1210` |

**LATCH PERMANENTE (sin auto-rearme)** — `[CERT]` RT `BChiUp.java:1534-1537` + `BChiDashboardService.java:736-737`,
`:1090`,`:1107`,…: los bloques `else-if` de re-arme por histéresis fueron **borrados** (change T-B; `HYSTERESIS_FACTOR`
eliminado). Una protección permanece `tripped` hasta que el operador invoca `resetAlarmas()` explícitamente. Esto
convierte a las protecciones en una máquina de estados **monótona**: OFF → TRIPPED (por lectura) → OFF (sólo por reset humano).

**Auto-latch en flanco de subida** — `_autoLatchProtection()` `[CERT]` RT `BChiDashboardService.java:1387-1422`:
lee `alarmLatches`, si la key ya existe → **no-op** (preserva `latchedAt` original), si no, escribe entrada
`{"latched":true,"latchedAt":<epochMs>,"latchedBy":"system-cov","note":"auto-latch by <source>"}`. First-writer-wins:
en la cascada de fan, si comp1/comp2 ya estaban latcheados por su propia sobrecarga, se conservan.

**Trip instantáneo por COV** — además del tick de 10 s, `BChiUp.changed()` `[CERT]` RT `BChiUp.java:1607-1651`:
cuando se escribe cualquiera de los 5 slots de amperaje (`PROT_SLOTS`), despacha
`BChiDashboardService.scheduleCovEvaluate(this)` `[CERT]` `BChiDashboardService.java:967-1004`, que corre
`evaluateUp(up, "COV")` off-thread. Latencia de trip por sobrecorriente ≈ inmediata, no hasta 10 s.

**Reset del operador** — `BChiUp.doResetAlarmas()` `[CERT]` RT `BChiUp.java:2022-2076`: (1) `setAlarmLatches("{}")`,
(2) `syncProtectionSlots()` → todos los `protXActive` a false, (3) `svc.clearTripped(ord)` limpia `trippedFlags` en
RAM, (4) log INFO de auditoría con identidad del operador (via `ContextThread.getContext().getUser()`, fallback
`"unknown"`). `clearTripped()` toma el MISMO lock per-Ord `[CERT]` `BChiDashboardService.java:525-546`.

---

## 168.6 — El `ReentrantLock` per-Ord: coordinación tick ↔ escritura (423 Locked conceptual)

Cada `BChiUp` tiene su propio `ReentrantLock` en un `ConcurrentHashMap<String,ReentrantLock> ordLocks` indexado
por Ord `[CERT]` RT `BChiDashboardService.java:223-224`.

`acquireLock(ord, timeoutMs)` `[CERT]` `BChiDashboardService.java:475-498`:
- `ordLocks.computeIfAbsent(ord, new ReentrantLock())` → `lock.tryLock(timeoutMs, MILLISECONDS)`.
- En timeout: incrementa `lockContentionCounter` (`AtomicInteger`) y lo refleja en el slot Niagara
  `controlLockContentionCount` (SUMMARY + TRANSIENT, visible en Workbench, no persistido) `[CERT]` `:55-60`,`:489-490`.

Quién compite por el mismo lock de un UP:
- El **control-tick** (`evaluateUp` bajo lock, 500 ms) `[CERT]` `:1015`.
- El **path COV** (mismo `evaluateUp`) `[CERT]` `:986`.
- El **reset del operador** (`clearTripped`, 500 ms) `[CERT]` `:528`.

Cuando dos de estos coinciden sobre el MISMO UP, el segundo espera hasta 500 ms; si no lo obtiene, **abandona su
ciclo** (el tick loguea y salta; el reset devuelve `-1`) en vez de bloquear indefinidamente. Este es el equivalente
de dominio a una respuesta **423 Locked**: la escritura no se pierde silenciosamente, se reporta contención.
`[INFER]` La respuesta HTTP 423 que la API expone sobre escrituras de threshold/setpoint concurrentes (documentada
en el plano servlet/RBAC, [Block 164]) es la superficie externa de este mismo lock.

Nota de reentrancia — `[CERT]` RT `BChiUp.java:1963-1969`: `syncProtectionSlots()` es seguro de llamar bajo el lock
porque sólo hace lecturas de slot + escrituras locales de slot; NO re-adquiere `acquireLock` ni toca `trippedFlags`.

---

## 168.7 — `ChiThresholdHelper`: allowlist + guards que constriñen la API de escritura

La UX escribe thresholds únicamente vía `ChiThresholdHelper` (package-private, `final`, sin instanciar)
`[CERT]` UX `ChiThresholdHelper.java:21-25`. Es la **frontera de validación** entre la API HTTP y los slots.

**Allowlists por tipo de equipo** — `[CERT]` UX `ChiThresholdHelper.java:31-49`:

| Constante | Keys permitidas | `[CERT]` |
|---|---|---|
| `UP_THRESHOLD_KEYS` | los 7: `sobrecargaFan/Compresor1/2/Abanicos1/2`, `antifrezzeSistema1/2` | `:31-39` |
| `CARCAMO_THRESHOLD_KEYS` | `umbralAdvertencia`, `umbralCritico` | `:41-44` |
| `DATALOGGER_THRESHOLD_KEYS` | `umbralAdvertencia`, `umbralCritico` | `:46-49` |

**`writeThreshold(ord, name, value, allowlist, context)`** — `[CERT]` UX `ChiThresholdHelper.java:207-240`, aplica
en orden y devuelve `String` de error (o `null` en éxito):
1. `name` debe estar en el allowlist provisto → si no: `"invalid threshold name: …"` `[CERT]` `:210-213`.
2. `isValidThresholdValue(value)`: **`!NaN && !Infinite && value >= 0.0`** → si no: `"invalid threshold value…"`
   `[CERT]` `:70-73`,`:214-217`. (Rechaza negativos, NaN e infinitos — un threshold nunca es negativo.)
3. El Ord debe resolver a un `BComponent` `[CERT]` `:221-224`; y la propiedad `name` debe existir `[CERT]` `:227-230`.
4. Sólo entonces: `comp.set(prop, new BStatusNumeric(value), null)` `[CERT]` `:231`.

Doble candado: aunque el servlet pasa el allowlist correcto por tipo, `writeThreshold` re-valida `name ∈ allowlist`
Y existencia de la propiedad — un `name` fuera de allowlist nunca llega a un `set()`. Las lecturas
(`readUpThresholds`/`readCarcamoThresholds`/`readDataloggerThresholds`) también validan el tipo del componente
resuelto (`instanceof BChiUp/…`) antes de serializar JSON `[CERT]` `:94-186`.

---

## 168.8 — Thresholds de Carcamo y Datalogger (breve)

Ambos son contenedores de propiedades sin control-tick asociado:
- `BChiCarcamo` (cárcamo / nivel de agua): feedback `nivelCm`, `state`; thresholds `umbralAdvertencia`,
  `umbralCritico` `[CERT]` RT `components/BChiCarcamo.java:66-91`.
- `BChiDatalogger` (presión): feedback `pressurePsi`, `pressureBar`, `state`; thresholds `umbralAdvertencia`,
  `umbralCritico` `[CERT]` RT `components/BChiDatalogger.java:66-97`.

`[INFER]` A diferencia del UP, sus umbrales NO se comparan en `controlTick()` (que sólo itera `UpMonitor` →
`BChiUp`); la clasificación advertencia/crítico se resuelve en la capa de status/DTO o el frontend. Aquí el
threshold es dato de presentación, no disparador de comando físico.

---

## 168.x — Connections

- **[Block 164]** — las escrituras de threshold están **RBAC-gated y lock-coordinated**: `ChiThresholdHelper`
  (allowlist + guards, §168.7) es la validación de dominio, pero el gate de autorización (quién puede escribir) y
  la respuesta 423 Locked sobre contención (§168.6) viven en el plano servlet/RBAC/audit documentado en [Block 164].
  El `auditLog` ring buffer del service (`[CERT]` `BChiDashboardService.java:653-725`, MAX 500 entradas) registra
  cada escritura — es el mismo mecanismo que audita `resetAlarmas`.
- **[Block 163]** — este bloque profundiza el gap C6 (thresholds/protección) del esqueleto inaugural del focus:
  `BChiUp`/`BChiDashboardService` son los componentes cuyo servlet HTTP y postura RBAC describió el [Block 163].
- **Nota de profundidad vs. Reflow** — esto es **control de dominio HVAC de fuente primaria** (máquina de estados
  trip→latch permanente, cascadas de seguridad fan/abanicos, coordinación de lock per-equipo, auto-latch con
  identidad de operador) que **nmodsreflow NO tiene**: Reflow es un dashboard de visualización genérico. La
  contraparte de autoría propia (chihuahua) implementa la LÓGICA DE PROTECCIÓN que Reflow sólo mostraría como datos.
