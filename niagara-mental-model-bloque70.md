# Bloque 70 — Audit empírico TIER-1: HistoryData split + Ack flow + historyCache shape

**Sesión**: 2026-05-09 (post-bloque #69)
**Source READ-ONLY**: `/home/cristian/modules/Prototipos/Reflow-Clean-177/`
**Disciplina**: audit empírico DIRECTO con file:line citations literales — NO síntesis desde mapping ni desde GAP-ANALYSIS.
**Continuación de**: bloque #68 (transplante blueprint) + bloque #69 (audit live-update + lección clean-room desconectado).

---

## §70.0 — Resumen ejecutivo (3 veredictos)

| Item | Verdadero (mapping/bloque #68 dijo) | Empírico (file:line) | Veredicto |
|------|--------------------------------------|----------------------|-----------|
| **A — HistoryData.java split** | 3 clases: HistoryQueryEngine ~300L + HistoryDataCache ~150L + HistoryJsonSerializer ~200L | 1 clase outer 100% static + 1 inner static `FromComponentTask` Runnable. **CERO state mutable**. **CERO cache** en backend (cache vive en frontend `historyCache.js`). 16 métodos + 1 inner class. | **SPLIT VIABLE pero INCORRECTO** — `HistoryDataCache` es ficticia: no hay state que cachear. Re-prescribir como **2 clases (Engine + Serializer) o 3 sin Cache** (Engine+Runner+Serializer). |
| **B — Ack flow end-to-end** | (no había prescripción explícita en bloque #68 — solo "preservar forma de AlarmsTable + AlarmCards") | **2 caminos asimétricos**. AlarmsHome bulk → AlarmAckConfirm modal → save. AlarmDetails row/card → directo (sin modal de confirmación). Backend ack = `$niagara.alarm.ackAlarmsByUuid` BajaScript nativo. **`canAcknowledgeAlarms` BOX existe pero NUNCA se llama desde frontend** — gate es `acknowledgmentEnabled` UI-side. `acknowledgmentRequiresNote` 100% frontend. | **PRESERVAR ASIMETRÍA** en MX60. Decidir: cablear `canAcknowledgeAlarms` BOX en MX60 o BORRAR método backend dead-code. |
| **C — historyCache shape post-S56** | xa cache + Sa service ~324L + Ia builder + minimal Vuex | xa cache **5 props** (histories/groups/devices/groupIndex/pagination) confirmado. Sa service **340 LOC reales** (target era ~324 — 9 métodos + 4 getters). Ia builder confirmado. Vuex minimal: `state.invalid` + `SET_INVALID` + `refresh` action. **2 bugs en clean-room**: `last24Hours` formula rota (devuelve 24 DAYS) + skip-if-cached condition NEVER fires (`state.groups` referenced sin existir). | **PINIA 1:1 VIABLE** — preservar separación: módulo-level reactive + service plain object + Pinia store mínimo. Corregir 2 bugs en MX60. |

**Tally TIER-1 cerrado**: las 3 piezas críticas pendientes de bloque #68 quedan auditadas empíricamente.

---

## §70.1 — Item A: `HistoryData.java` split verification + dependency graph

**File**: `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/history/HistoryData.java`
**LOC verified**: 663 (`wc -l` exacto)
**Estructura**: 1 clase outer `HistoryData` (todos los métodos static) + 1 inner static class `FromComponentTask implements Runnable`.

### §70.1.1 — Estado mutable: hay CERO

**Outer class fields** (L49-50):
```java
private static DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
private static DecimalFormat decimalFormat = new DecimalFormat("0.00");
```

Solo 2 fields static, ambos formateadores. **Gotcha thread-safety**: ni `SimpleDateFormat` ni `DecimalFormat` son thread-safe. Si el split se hace, en MX60 mover a `ThreadLocal<DateFormat>` o usar `java.time.format.DateTimeFormatter` (immutable). **Bug latente NO reportado en mapping** — flag `[OPEN-MX60]`.

**Inner class FromComponentTask fields** (L581-591): 10 fields finales por construcción + `private volatile JSONObject historyData` resultado. Encapsulado a 1 ejecución de Thread → no comparte mutación entre threads.

### §70.1.2 — Inventario de métodos (16 outer + 1 inner)

| # | Método | LOC | Líneas | Tipo |
|---|--------|-----|--------|------|
| 1 | `fromComponent(BComponent)` | 30 | L52-81 | **Entry point**: parsea slot props del BComponent, spawn Thread con AccessController.doPrivileged + FromComponentTask + thread.join. Retorna JSONObject. |
| 2 | `jsonForHistory(BIHistory, String, BDateRangeEnum, boolean, boolean)` | 4 | L83-86 | Wrapper COMPARE: `CompareRangeCalculator.make(range)` → delega a #4. |
| 3 | `jsonForHistory(BIHistory, String, BDateRangeEnum, boolean)` | 4 | L88-91 | Wrapper normal: `RangeCalculator.make(range)` → delega a #4. |
| 4 | `jsonForHistory(BIHistory, String, BAbsTime, BAbsTime, boolean, String)` | 102 | L93-194 | **Core load+serialize por rango**: subscribeToHistory si cross-station, abre HistorySpaceConnection, timeQuery, valueFacets ordinals/units, llama #9, agrega timezone metadata, cierra cursor+connection. |
| 5 | `jsonForHistory(BIHistory, String, int, boolean)` | 70 | L196-265 | **Core load+serialize por recordCount**: subscribeToHistory si cross-station, scan(history, true), llama #8 (que delega a #9), agrega timezone metadata. |
| 6 | `jsonForLastRecord(BIHistory)` | 32 | L267-298 | **Single-last-record**: `conn.getLastRecord(history)` → llama #13. |
| 7 | `arrayForHistoryCollection(Cursor, String, BHistoryConfig, boolean)` | 3 | L300-302 | Wrapper → #9 con limit=0. |
| 8 | `arrayForHistoryCollection(Cursor, String, BHistoryConfig, int, boolean)` | 3 | L304-306 | Wrapper → #9 con start=0,stop=0. |
| 9 | `arrayForHistoryCollection(Cursor, String, BHistoryConfig, int, long, long, boolean)` | 69 | L308-376 | **Core array serializer**: cursor loop + opcional pre/post BQL queries para contextual ranges (history:HistoryRecord BQL where timestamp.millis </> bounds order desc/asc), filtro `getBit(4)` hidden flag. Llama #10 (style=='d3') o #13 (default). |
| 10 | `jsonArrayForRecord(BHistoryRecord, BHistoryConfig)` | 101 | L378-478 | **Core array record→JSON [time, value, label?]**: 5 ramas BNumericTrendRecord/BNumeric64BitTrendRecord/BStringTrendRecord/BBooleanTrendRecord/BEnumTrendRecord. Aplica precision facet (BDouble/BInteger discriminado). Retorna `null` si NaN/Infinite. |
| 11 | `jsonObjectForRecord(BHistoryRecord, BHistoryConfig)` | 3 | L480-482 | Wrapper → #13 con status=false,millis=false. |
| 12 | `jsonObjectForRecord(BHistoryRecord, BHistoryConfig, boolean)` | 3 | L484-486 | Wrapper → #13 con millis=false. |
| 13 | `jsonObjectForRecord(BHistoryRecord, BHistoryConfig, boolean, boolean)` | 78 | L488-565 | **Core object record→JSON {time, value, status?, label?}**: 4 ramas (Numeric/String/Boolean/Enum + fallback BTrendRecord). Mismas reglas precision. Status opcional (`numericRec.getStatus().toString()`). |
| 14 | `subscribeToHistory(BIHistory)` | 3 | L567-569 | Wrapper → #15 con ctx=null. |
| 15 | `subscribeToHistory(BIHistory, Context)` | 8 | L571-578 | Cross-station workaround: crea `BasicContext(ctx, BFacets.make("asyncHistorySubscribe", true))` + `HistoryGhostSubscriber` + `bHistory.subscribe(subscriber, subContext)`. **Ya cross-referenciado a bloque #69 §69.5: subscriber one-shot auto-unsuscribe NO keepalive**. |
| 16 | `FromComponentTask` inner static class | 82 | L580-661 | Runnable con 10 fields + `getHistoryData()`. `run()`: split names CSV, itera + delega a #2/#3/#5 según limit/range/start/end + opcional `|compare|` key prefix. |

**Suma LOC métodos** = 30+4+4+102+70+32+3+3+69+101+3+3+78+3+8+82 = **595 LOC** + ~68 LOC overhead (imports L1-46, class header, blank lines) = **663 LOC** ✓

### §70.1.3 — Dependency graph interno

```
fromComponent (entry)
    └─> FromComponentTask.run()
            ├─> jsonForHistory(BDateRangeEnum, contextualRanges)            [#3]
            │       └─> jsonForHistory(BAbsTime, BAbsTime, ..., name)     [#4 core]
            │               ├─> subscribeToHistory                         [#14→#15]
            │               └─> arrayForHistoryCollection(start, stop)    [#9 core]
            │                       ├─> jsonArrayForRecord (style=='d3')  [#10]
            │                       └─> jsonObjectForRecord                [#13]
            ├─> jsonForHistory(int recordCount, contextualRanges)         [#5 core]
            │       └─> arrayForHistoryCollection(int)                    [#8→#9]
            └─> jsonForHistory(BDateRangeEnum, contextualRanges, true)    [#2 compare]
                    └─> jsonForHistory(BAbsTime, BAbsTime, ..., "COMPARE") [#4 core]

jsonForLastRecord (entry standalone)
    └─> jsonObjectForRecord(BHistoryRecord, BHistoryConfig, true, true)   [#13 core]
```

3 entry points externos: `fromComponent` (consumido por BReflowHistoryCommands BOX getData) + `jsonForHistory` overloads + `jsonForLastRecord` (consumido por otras clases del módulo via static import).

### §70.1.4 — Métodos "border" (ambiguos al hacer split)

- **`subscribeToHistory` (#14, #15)**: lógica de query (abrir subscription a history cross-station para que datos se hidraten antes del query). Más afín a Engine que a Serializer.
- **`arrayForHistoryCollection(main #9)`**: contiene cursor loop (Engine) + invoca jsonArrayForRecord (Serializer) + ejecuta BQL queries auxiliares para contextual pre/post records (Engine). Es **mixto**.
- **`fromComponent` (#1)** + **`FromComponentTask` (#16)**: orquestación + threading. Más afín a Engine pero acopla con BComponent slot-extraction (frontera UX→backend).

### §70.1.5 — Mapeo a 3 clases propuestas (bloque #68 §68.1.6)

| Clase propuesta bloque #68 | LOC target | Métodos asignables (empírico) | LOC reales | Veredicto |
|----------------------------|------------|------------------------------|------------|-----------|
| `HistoryQueryEngine` | ~300 | #1 fromComponent + #2/3/4 jsonForHistory(range) + #5 jsonForHistory(count) + #6 jsonForLastRecord + #14/15 subscribeToHistory + #16 FromComponentTask | **331** (30+4+4+102+70+32+3+8+82+~~6 wrappers~~) | ✓ alineado |
| `HistoryDataCache` | ~150 | **NINGUNO**. No hay state cacheable en HistoryData.java. Cache vive en frontend `historyCache.js` (ITEM C). | **0** | ✗ **CLASE FICTICIA** |
| `HistoryJsonSerializer` | ~200 | #7/8/9 arrayForHistoryCollection (3 overloads) + #10 jsonArrayForRecord + #11/12/13 jsonObjectForRecord (3 overloads) | **257** (3+3+69+101+3+3+78) | ✓ alineado |

### §70.1.6 — Veredicto

**Split viable pero requiere AJUSTE**. La clase `HistoryDataCache` es ficticia — no hay nada que cachear en backend. El cache real es frontend (`historyCache.js` xa module-level).

**Re-prescripción correcta para MX60**:

**Opción A (recomendada — 2 clases)**:
- `HistoryQueryEngine` (~331L): entry points + threading + subscription cross-station + load orchestration
- `HistoryJsonSerializer` (~257L): cursor→array, record→array, record→object (5 record-type branches each)

**Opción B (3 clases con boundary diferente)**:
- `HistoryQueryEngine` (~140L): #1 fromComponent + #16 FromComponentTask + #14/15 subscribeToHistory
- `HistoryQueryRunner` (~210L): #2/3/4/5 jsonForHistory overloads + #6 jsonForLastRecord (orchestration: connection lifecycle + cursor management)
- `HistoryJsonSerializer` (~315L): #7/8/9 array + #10 array record + #11/12/13 object record (puro mapeo)

**Polish OBLIGATORIO en MX60 además del split**:
1. Reemplazar `dateFormat`/`decimalFormat` static por `ThreadLocal<...>` o `DateTimeFormatter` immutable (thread-safety bug latente).
2. Reducir tres overloads de `arrayForHistoryCollection` y `jsonObjectForRecord` con default arguments (Kotlin/Java records) o método único + builder.

**[FIXED-70-A]** flag — bloque #68 §68.1.6 ya tiene re-prescripción correcta encima.

---

## §70.2 — Item B: Ack flow end-to-end + diagrama secuencial

**Files audited**:
- `reflow-frontend/src/components/alarms/AlarmAckConfirm.vue` (92L)
- `reflow-frontend/src/components/alarms/RequiredNoteModal.vue` (95L)
- `reflow-frontend/src/components/alarms/AlarmsTable.vue` (427L) — sección ack L20/L29/L59/L145/L306-311
- `reflow-frontend/src/components/alarms/AlarmCards.vue` (371L) — sección ack L95/L246
- `reflow-frontend/src/api/box.js` (331L) — sección alarm L154-241
- `reflow-frontend/src/views/AlarmsHome.vue` — sección ack L97-114, L425
- `reflow-frontend/src/views/AlarmDetails.vue` — sección ack L80-127, L361-389
- `reflow-frontend/src/store/modules/alarms.js` (239L) — alarmConsole shape + getConsoleById
- `reflow-frontend/src/plugins/niagara.js` — alarm namespace L96-122
- `nmodsreflow/.../commands/BReflowAlarmCommands.java` (113L)

### §70.2.1 — Trigger entry points UI (4 distintos)

| Site | Componente | Trigger | Payload emitido | Linea |
|------|------------|---------|-----------------|-------|
| 1 | `AlarmsTable.vue` | Per-row "Acknowledge" button (Poptip + alarm-card-actions) | `$emit('ack', { alarmConsole, uuid: alarm.uuid })` | L307-311 |
| 2 | `AlarmsTable.vue` | Bulk "Acknowledge Selected" button | `$emit('ack', { alarmConsole, uuid: uuids[] })` (vía batchAck) | L306 |
| 3 | `AlarmCards.vue` | Modal card "Acknowledge" button | `$emit('ack', { alarmConsole, uuid: record.uuid })` | L246 |
| 4 | `SourceGroupsTable.vue` | "Acknowledge All" / "Acknowledge Most Recent" group bulk | `$emit('ack-all', records)` / `$emit('ack-recent', group)` (eventos DISTINTOS) | L67-68/L166-167 (citados via rg) |

**Gating UI común**: todos los 4 sites guardean visibilidad con `v-if="alarmConsole && alarmConsole.acknowledgmentEnabled"` (rg confirma 8 sites usando este flag — lista en §70.2.5). El flag es CONFIG del console (alarms.js L10 default `true`), NO permission check del usuario.

### §70.2.2 — Validation flow

**`canAcknowledgeAlarms` BOX existe pero está MUERTO en frontend**:
- `BReflowAlarmCommands.java` L109-112: método existe, retorna `BBoolean.make(cx.getUser().getPermissionsFor(alarmService).hasOperatorWrite())` — server-side permission check válido
- `box.js` L222-230: stub `alarmCanAcknowledge()` con `console.log + Promise.resolve(false)`
- `rg "alarmCanAcknowledge|canAcknowledgeAlarms"` cross-frontend: **3 hits, ninguno consumidor** — solo definición JSDoc + stub + JSDoc cross-link en box.js. **CERO call sites en componentes/views**

**Conclusión**: la validación de permiso server-side EXISTE en backend pero el frontend NUNCA la consulta. El gate visual depende solo de:
1. `alarmConsole.acknowledgmentEnabled` (config UI por consola, default `true` — alarms.js L10)
2. `alarmConsole.acknowledgmentRequiresNote` (config UI per-console — controla si se pide nota antes de ack)

Si el usuario sin permisos clickea Acknowledge → la llamada `$niagara.alarm.ackAlarmsByUuid()` falla server-side y propaga como exception capturada en el `catch` de save/sendAck.

### §70.2.3 — Required note logic — 100% FRONTEND-side

**Decide el frontend, NO el backend**. Citas literales:

- **AlarmAckConfirm.vue L65-72** (camino bulk via AlarmsHome):
  ```js
  confirmAck() {
    if (this.alarmConsole && this.alarmConsole.acknowledgmentRequiresNote) {
      this.$refs.ackNoteModal.openModal(this.records);
    } else {
      this.save();
    }
  }
  ```
- **AlarmDetails.vue L369-377** (camino row/card via AlarmsTable+AlarmCards):
  ```js
  async ack(payload) {
    var console = payload.alarmConsole;
    var uuid = payload.uuid;
    if (console && console.acknowledgmentRequiresNote) {
      this.$refs.ackNoteModal.openModal(uuid);
    } else {
      this.sendAck(uuid);
    }
  }
  ```

**Backend no sabe nada**: en `BReflowAlarmCommands.java` no hay método `requiresNoteForAck` ni equivalente. El backend solo tiene `addNotes(uuid, note)` (vía $niagara.alarm.addNotes BajaScript) que acepta cualquier nota voluntaria. Si el frontend decide no pedir nota, el backend ack se ejecuta sin nota — sin enforcement.

**Implicación para MX60**: el `acknowledgmentRequiresNote` debe persistir en `useAlarmsStore.consoles[].acknowledgmentRequiresNote` (Pinia 1:1 con Vuex). Si MX60 quiere enforcement REAL, agregar validación server-side en BReflowAlarmCommands custom NUEVO (no existe en Reflow).

### §70.2.4 — Backend ack execution: NO es BOX, es BajaScript native

**Método llamado**: `$niagara.alarm.ackAlarmsByUuid(uuids)` — directamente al plugin baja del frontend, NO via `box.serverSideCall()`.

**Citas**:
- `AlarmAckConfirm.vue` L77-89 `save()`:
  ```js
  await this.$niagara.alarm.ackAlarmsByUuid(this.records);
  this.$store.dispatch('notify/success', 'Alarms acknowledged');
  this.$emit('load-alarms');
  ```
- `AlarmDetails.vue` L378-389 `sendAck()`:
  ```js
  await this.$niagara.alarm.ackAlarmsByUuid(uuids);
  this.$store.dispatch('notify/success', 'Alarm acknowledged successfully');
  this.loadAlarms();
  ```

**Plugin niagara.js (mock)** L105: `ackAlarmsByUuid: asyncNull('alarm.ackAlarmsByUuid')` — no-op en clean-room.

**Plugin niagara.js (mock)** L96-122 — alarm namespace tiene **20 métodos** mockeados, ninguno de subscribe (cierra otra vez bloque #69 §69.4): `getAlarmList`, `getUuidForSources`, `ackAlarm`, `ackAlarms`, **`ackAlarmsByUuid`**, `addNote`, **`addNotes`**, `getNotes`, `hyperAck`, `getAlarmClasses`, `getSourceGroupAlarms`, `getSourceList`, `getClassList`, `classList`, `getSoundFiles`, `getFilter`, `checkAlarmSounds`, `startAlarmSounds`, `stopAlarmSounds`, `playAlarmSound`.

**En producción real** (Reflow bundle 1.7.5): `ackAlarmsByUuid` resolve via `aQ.alarm` (Niagara framework `baja.js` global). Comment niagara.js L3: `In production, aQ object has: ... alarm, bql, history, subscriber, browser, file, time`.

**Note flow** (RequiredNoteModal):
- L80-87 `saveNote()`:
  ```js
  await this.$niagara.alarm.addNotes(this.alarmUuid, this.notes);
  ```
- Después emite `'ack'` que el parent captura → ejecuta save/sendAck (que ejecuta el ackAlarmsByUuid).
- **Orden crítico**: nota se guarda ANTES del ack. Si el ack falla server-side por permisos, la nota queda huérfana. NO hay rollback.

### §70.2.5 — UI refresh post-ack: PESSIMISTIC re-fetch

- `AlarmsHome.vue` L113: `<AlarmAckConfirm ... @load-alarms="getAlarmSources" />` — re-fetch completo de sources
- `AlarmDetails.vue` L107: `<AlarmAckConfirm ... @load-alarms="loadAlarms" />` — re-fetch completo de alarmList
- `AlarmDetails.vue` L384: `this.loadAlarms()` post-sendAck — re-fetch directo sin notificación
- **NO optimistic update** de `alarms[].ackState` — se espera el response del server
- **NO WebSocket push** — confirmado bloque #69: `BReflowChannelService` es genérico route+config-control, no emite alarmas
- **Polling también corre en paralelo** (bloque #69 §69.2): `consoleRefreshRate || 20s` setInterval en AlarmsHome/AlarmDetails — el ack puede ser sobrescrito por el siguiente poll si el ack server-side aún no confirmó

### §70.2.6 — Error paths

| Punto | Comportamiento empírico | File:Line |
|-------|--------------------------|-----------|
| AlarmAckConfirm.save catch | `console.error + notify/error 'Failed to acknowledge alarms'` + modal NO se cierra | AlarmAckConfirm.vue L85-88 |
| AlarmDetails.sendAck catch | `console.error + notify/error 'Error acknowledging alarm'` + lista NO se refresca | AlarmDetails.vue L385-388 |
| RequiredNoteModal.saveNote catch | `console.error + return false` → modal queda en estado loading=false (ack NO se emite, nota NO se guardó) | RequiredNoteModal.vue L84-87 |
| `$niagara.alarm` undefined (no plugin) | Guard `if (this.$niagara && this.$niagara.alarm)` evita crash, pero el flow NO ejecuta ack ni notifica error | AlarmAckConfirm L79, AlarmDetails L380, RequiredNoteModal L80 |

**Gotcha silente**: si `$niagara.alarm` es undefined, el flow "succeeds" (notify/success emite + emit('load-alarms')) sin haber ejecutado el ack. Mode mock-friendly pero **falsa victoria** en producción si baja.js no carga.

### §70.2.7 — Diagrama secuencial — 2 caminos asimétricos

**CAMINO 1: AlarmsHome bulk (via SourceGroupsTable)**
```
SourceGroupsTable.ackAll button
  └─ emit 'ack-all' → AlarmsHome.confirmAckAll(uuids) [L425]
        ├─ this.ackAllRecords = uuids
        ├─ this.ackAllModal = true
        └─ <AlarmAckConfirm v-model="ackAllModal" :records="ackAllRecords"
                           :console-id="consoleId" @load-alarms="getAlarmSources"/>
              └─ User clicks OK → @on-ok="confirmAck"
                    ├─ if alarmConsole.acknowledgmentRequiresNote:
                    │     └─ $refs.ackNoteModal.openModal(records)
                    │           └─ User types note + Acknowledge
                    │                 └─ ack() → saveNote()
                    │                       └─ $niagara.alarm.addNotes(uuids, notes) ← BAJA NATIVE
                    │                       └─ if success → emit 'ack' → AlarmAckConfirm.requiredNoteAck()
                    │                             └─ await save() ↓
                    │                             └─ closeModal()
                    └─ else:
                          └─ save() → $niagara.alarm.ackAlarmsByUuid(records) ← BAJA NATIVE
                                ├─ notify/success
                                ├─ emit 'load-alarms' → AlarmsHome.getAlarmSources() PESSIMISTIC RE-FETCH
                                └─ this.modal = false
```

**CAMINO 2: AlarmDetails row/card (via AlarmsTable + AlarmCards)**
```
AlarmsTable per-row "Acknowledge" OR AlarmCards modal "Acknowledge"
  └─ emit 'ack' { alarmConsole, uuid } → AlarmDetails.ack(payload) [L369]
        ├─ if console.acknowledgmentRequiresNote:
        │     └─ $refs.ackNoteModal.openModal(uuid)
        │           └─ User types + Acknowledge
        │                 └─ saveNote() → $niagara.alarm.addNotes(uuid, notes)
        │                 └─ emit 'ack' → AlarmDetails.requiredNoteAck(uuids)
        │                       └─ await sendAck(uuids)
        │                       └─ closeModal
        └─ else:
              └─ sendAck(uuid) → $niagara.alarm.ackAlarmsByUuid(uuid) ← BAJA NATIVE
                    ├─ notify/success
                    └─ this.loadAlarms() PESSIMISTIC RE-FETCH
                    [NO confirmation modal — ack es immediate sin AlarmAckConfirm]
```

**Asimetría DOCUMENTADA**: AlarmsHome bulk siempre pasa por AlarmAckConfirm modal de confirmación. AlarmDetails row/card NO. Es decisión UX intencional (bulk = destructivo masivo → confirmar; single = clic explícito ya es confirmación).

### §70.2.8 — Gotchas MX60 (lo que NO simplificar sin romper coordinación)

1. **NO unificar los 2 caminos en uno solo**. Operadores esperan UX distinta para bulk vs row.
2. **NO mover `acknowledgmentRequiresNote` decisión al backend** sin agregar también enforcement server-side (no existe hoy).
3. **NO cablear `canAcknowledgeAlarms` BOX** sin decidir qué hacer con el resultado (¿ocultar botón? ¿confirmar y mostrar error?). Mejor: **borrar método dead-code** o **wire-up coherente** en MX60. Decisión.
4. **NO reordenar saveNote/ack**: nota antes de ack es invariante. Reordenar = nota huérfana si ack falla.
5. **NO eliminar el `if ($niagara && $niagara.alarm)` guard** sin agregar real-error-on-missing-plugin — actualmente fallar silenciosamente.
6. **NO eliminar el polling paralelo** durante el ack — está cubriendo el caso ack-failed-but-not-detected. Si MX60 implementa optimistic update (decisión #69-MX60), polling sigue siendo defensivo.
7. **`consoleId: 'default-alarm-console'`** es el fallback default en 7+ sites — preservar literal.

### §70.2.9 — Decisión MX60 NUEVA sobre canAcknowledgeAlarms

`canAcknowledgeAlarms` BOX en backend está implementado (BReflowAlarmCommands.java L109-112) pero **dead-code en frontend**. 3 opciones para MX60:

- **(a) BORRAR método backend** — reduce superficie API. Riesgo: si futuro MX60 quiere enforcement server-side, hay que re-agregar.
- **(b) WIRE-UP en frontend** — agregar call al mounted() de AlarmsHome/AlarmDetails que setea un computed `canAck` que reemplaza/complementa `acknowledgmentEnabled`. Beneficio: respeto real de permisos N4. Costo: una llamada extra por mount + caching local.
- **(c) PRESERVAR como dead-code documentado** — heredar tal cual con comment `// TODO MX60: wire-up or remove`. Es lo que el bloque #68 implícitamente prescribió ("HEREDA 95%").

**Recomendación TIER-1**: opción (b) si MX60-alarms se ejecuta dentro de stations donde operadores pueden tener role `view-only` que no es bloqueado por consola config. Opción (a) si MX60-alarms asume todos los usuarios con acceso al frontend tienen ack-write. **Decidir en sprint-1 backend**.

---

## §70.3 — Item C: `historyCache.js` post-S56 shape + Pinia composable target

**File**: `reflow-frontend/src/store/modules/historyCache.js` (516 LOC verified)
**Comment header L1-5**: declara explícitamente "Module-level cache object (NOT Vuex state), service object with API methods, and a minimal Vuex store that only tracks invalidation. Components access historyData directly (import), NOT through Vuex getters."

### §70.3.1 — Shape real (4 piezas)

| Pieza | Bundle ref | Líneas reales | Tipo | Confirmación bloque #68 |
|-------|------------|---------------|------|--------------------------|
| **xa cache** (`historyData`) | bundle 13466-13845 | L85-91 | `export var historyData = { histories: [], groups: [], devices: [], groupIndex: {}, pagination: {} }` | ✓ EXACTO 5 props |
| **Sa service** (`historyService`) | bundle 13466-13845 | L122-462 = **340 LOC** | `export var historyService = { ...9 methods + 4 getters }` | ✓ shape correcto, pero LOC reales 340 vs target ~324 (diff +16L) |
| **Ia builder** (`groupsIndexBuilder`) | bundle ref | L98-114 | `function groupsIndexBuilder(node, index)` recursive (mutates `index` param) | ✓ exacto |
| **Ta Vuex store** (default export) | bundle ref | L468-516 | `{ namespaced: true, state: { invalid: true }, mutations: { SET_INVALID }, actions: { refresh } }` | ✓ minimal exacto |

### §70.3.2 — Métodos del service esperados (bloque #68 §68.0)

| Método esperado | Líneas | LOC | Cumplimiento |
|-----------------|--------|-----|--------------|
| `list(options)` | L137-148 | 12 | ✓ async wrapper sobre loadList |
| `loadList()` | L180-187 | 8 | ✓ GET /nmodsreflow/station/histories |
| `loadGroups()` | L170-177 | 8 | ✓ GET /nmodsreflow/station/history-groups |
| `loadDevices()` | L161-167 | 7 | ✓ STUB (mock returns []), comment: "yi.json(yi.spec.HISTORY, 'getDevices')" en producción |
| `loadDeviceTree()` | L151-158 | 8 | ✓ STUB, comment: "yi.json(yi.spec.HISTORY, 'getDeviceTree')" en producción |
| `generate()` | L190-194 | 5 | ✓ STUB ("BOX makeHistories not available outside N4") |
| `data(options)` | L197-248 | 52 | ✓ HTTP+BOX dual-path (en clean-room ambos branches caen a HTTP), normaliza `histories` array→csv, agrega style='d3' default, `compareHistories` join, opcional reverse de result.data si options.limit |
| `buildHistoryQueryString(params)` | L251-259 | 9 | ✓ excluye `fetchMethod` del qs |
| `d3Options(data, options)` | L263-461 | **199** | ✓ chart-data transformer (5 record-type branches: string/boolean/enum/number/raw + dashed pre-range marker + compare key prefix detection + color palette compArray injection) |

**Total service methods**: 9 + 4 getters (`$baja`, `$component`, `$color`, `compareKey`) = 13 entries.

### §70.3.3 — Divergencias con descripción GAP-ANALYSIS / bloque #68

1. **LOC service: 340 reales vs ~324 target** — diff +16L. No material; bundle estimate aproximado.
2. **Comment L1-5 declara explícitamente** la separación module-level vs Vuex. Sirve de defensa contra "lift-into-pinia" antipattern. Preservar comment en MX60.
3. **`compareKey` es getter, NO constante** (L132-134). Razón: permite override per-instance via Vue.prototype future. Trivial preservar.
4. **DOS BUGS LATENTES en clean-room** (no reportados en mapping ni en bloque #68):
   - **Bug `last24Hours`** L57: `subtractMs(t, 24 * DAY / 24 * 24)` se evalúa como `subtractMs(t, 86400000 * 24)` (operator precedence) = **24 días**, no 24 horas. Casi seguro typo de refactor (probable original: `24 * HOUR`). **`[OPEN-MX60] FIX OBLIGATORIO`** en MX60.
   - **Bug skip-cache condition** L489: `if (!state.invalid && state.groups && !invalidate && rootState.histories.localCacheEnabled)` — `state.groups` NUNCA está definido en `state` (state solo tiene `invalid`). El comment L487-488 reconoce esto: "NOTE: state.groups is always undefined (not in state) — this matches the bundle where the skip condition effectively never activates". **Significa**: el cache se refetchea TODA vez (refresh siempre llama loadList+loadGroups+loadDevices). En clean-room es bug-as-feature; en MX60 decidir: (a) preservar tal cual `[FIXED-S56-as-designed]`, (b) corregir para activar cache real con `historyData.groups` check.

### §70.3.4 — Mapeo a Pinia 1:1 (Vue 3 + Composition API)

**`useHistoryCacheStore.ts` — sketch literal**:

```ts
import { defineStore } from 'pinia';
import { ref, reactive } from 'vue';
import { historyService } from './historyService';      // service plain object exported separately
import { historyData } from './historyData';             // module-level reactive cache exported separately

export const useHistoryCacheStore = defineStore('historyCache', () => {
  const invalid = ref(true);

  function setInvalid(val: boolean) { invalid.value = val; }

  async function refresh(options?: { invalidate?: boolean }) {
    const opts = options ?? {};
    const inv = opts.invalidate ?? false;
    // Preservar bug-as-feature OR corregir — decisión pendiente §70.3.3.4(b)
    if (!invalid.value && /* historyData.groups.length > 0 && */ !inv && rootStore.histories.localCacheEnabled) {
      return;
    }
    const list = await historyService.list();
    historyData.histories = list?.list ?? list ?? [];
    historyData.pagination = list?.pagination ?? {};
    const groups = await historyService.loadGroups();
    historyData.groups = groups ?? [];
    const idx: Record<string, string[]> = {};
    for (const g of historyData.groups) groupsIndexBuilder(g, idx);
    historyData.groupIndex = idx;
    const devices = await historyService.loadDevices();
    historyData.devices = devices ?? [];
    setInvalid(false);
  }

  return { invalid, setInvalid, refresh };
});
```

**Files separados (3 módulos)**:
- `historyData.ts` — `export const historyData = reactive({ histories: [], groups: [], devices: [], groupIndex: {}, pagination: {} })` — module-level reactive, importado directo por componentes
- `historyService.ts` — plain object con 9 métodos + 4 getters; no necesita ser composable porque no tiene state propio
- `historyCacheStore.ts` — Pinia store con solo `invalid` + `setInvalid` + `refresh` action

**ANTI-PATTERN A EVITAR**: lift `historyData` o `historyService` adentro de Pinia state/getters. Bloque #68 §68.0 ya advirtió "historyCache shape POST-rewrite (cache module-level fuera Pinia + service object lógica pura + minimal store solo invalid flag) NO pre-rewrite massively over-built". Esta auditoría empírica CONFIRMA que la separación es intencional: el comment header L1-5 lo declara explícitamente y el código L85-91 + L122-462 + L468-516 lo implementa coherentemente.

### §70.3.5 — d3Options gotchas para Pinia/Vue 3 migration

`d3Options` (L263-461, 199 LOC) es el método más complejo del service. 5 ramas record-type + dashed pre-range marker + compare key prefix detection + color palette injection.

**Para MX60 transplant**:
- Mantener como función pura del `historyService` — NO mover a composable
- Acceso a `$color` (Vue 3 inject) → usar `inject('color')` al inicio del método o pasar como argumento explícito
- Tests obligatorios desde día 1 (sprint 6, decisión bloque #68 Vitest sprint-1): casos para cada record type + edge cases dashed/compare/empty data

---

## §70.4 — Decisiones MX60 actualizadas (vs bloque #68)

| Decisión bloque #68 | Status post-bloque 70 | Nuevo plan |
|----------------------|------------------------|-------------|
| **#231** split HistoryData.java en 3 clases (Engine+Cache+Serializer) | **AJUSTADA** — Cache es ficticia | Split en 2 (Engine+Serializer ~330L cada uno) o 3 alternativo (Engine+Runner+Serializer §70.1.6 Opción B). Decidir en sprint-1 backend. |
| **#238** historyCache shape post-S56 (module-level cache + service pure + minimal store) | **CONFIRMADA empíricamente** | Pinia 1:1 con sketch §70.3.4. Preservar comment header L1-5 como defensa anti lift-into-pinia. |
| **§68.4 ack flow** "preservar forma de ~60 componentes" | **EXTENDIDA** — asimetría 2-caminos documentada | Preservar AlarmsHome bulk (con AlarmAckConfirm) + AlarmDetails row/card (sin AlarmAckConfirm) literal. NO unificar. |
| (no había decisión bloque #68) | **NUEVA** §70.2.9 — `canAcknowledgeAlarms` BOX dead-code | Sprint-1 backend: decidir entre (a) borrar BReflowAlarmCommands.canAcknowledgeAlarms, (b) cablear en frontend (recomendado), o (c) heredar dead-code documentado. |
| (no había decisión bloque #68) | **NUEVA** §70.1.1 — thread-safety de SimpleDateFormat/DecimalFormat | Sprint-1 backend: reemplazar por `DateTimeFormatter` immutable o `ThreadLocal<...>` al hacer split HistoryData. |
| (no había decisión bloque #68) | **NUEVA** §70.3.3.4 — bug `last24Hours` returns 24 days | Sprint-3 frontend foundation: corregir literal en useHistoryCacheStore (`24 * HOUR`). |
| (no había decisión bloque #68) | **NUEVA** §70.3.3.4 — skip-cache condition NEVER fires | Sprint-3 frontend foundation: decidir preservar bug-as-feature OR corregir con `historyData.groups.length > 0` check. |

---

## §70.5 — Implications nuevas #254..#264 (11 implications)

- **#254** HistoryData.java es 100% static methods + 1 inner static Runnable — CERO state mutable outer (excepto 2 formatter fields no-thread-safe). Split viable sin tocar lifecycle.
- **#255** HistoryDataCache class prescrita por bloque #68 §68.1.6 es **ficticia** — no hay cache state en backend HistoryData.java. Cache real vive en frontend `historyCache.js` (xa module-level). Re-prescribir split en 2 clases (Engine+Serializer) o 3 alternativo (Engine+Runner+Serializer §70.1.6 Opción B).
- **#256** SimpleDateFormat + DecimalFormat static fields en HistoryData.java L49-50 son **NO thread-safe** — bug latente al hacer split o si algún consumidor llama desde múltiples threads. MX60 usar `DateTimeFormatter` immutable.
- **#257** Ack flow tiene **2 caminos asimétricos**: AlarmsHome bulk (via SourceGroupsTable → AlarmAckConfirm modal → save) y AlarmDetails row/card (via AlarmsTable+AlarmCards → directo sin AlarmAckConfirm). Preservar asimetría literal en MX60 — es decisión UX intencional (bulk-destructivo confirma; row-único es clic-confirma).
- **#258** Backend ack execution = **`$niagara.alarm.ackAlarmsByUuid` BajaScript NATIVE** (NO BOX/serverSideCall). Plugin niagara.js mock 20 métodos alarm namespace, ninguno de subscribe (re-confirma bloque #69 §69.4). En producción resuelve via baja.js global aQ.alarm.
- **#259** `canAcknowledgeAlarms` BOX en `BReflowAlarmCommands.java` L109-112 está **MUERTO en frontend** — definición + stub + JSDoc en box.js, CERO call sites en componentes/views. Decisión sprint-1 backend: borrar | wire-up | preservar dead-code documentado.
- **#260** `acknowledgmentRequiresNote` es **100% frontend gate** (alarmConsole config), backend no enforcea. Si MX60 quiere enforcement server-side, agregar validación NUEVA en BReflowAlarmCommands custom.
- **#261** UI refresh post-ack es **PESSIMISTIC re-fetch** (`getAlarmSources`/`loadAlarms`) — NO optimistic update, NO WebSocket push (re-confirma bloque #69 §69.5 alarmas-NO-WS-push). Polling paralelo `consoleRefreshRate||20s` cubre el ack-failed-but-not-detected.
- **#262** historyCache.js shape post-S56 **CONFIRMADO empíricamente** — comment header L1-5 declara explícitamente module-level cache + service pure + minimal Vuex. Pinia 1:1 sketch viable §70.3.4 sin lift-into-pinia. Service `Sa` real LOC = 340 (bloque #68 estimate ~324, diff +16L no material).
- **#263** Bug `last24Hours` L57 historyCache.js: `24 * DAY / 24 * 24` evalúa a 24 días por operator precedence, NO 24 horas. Bug NO reportado en mapping ni bloque #68. **`[OPEN-MX60]` fix obligatorio** en useHistoryCacheStore (`24 * HOUR`).
- **#264** Bug skip-cache condition L489 historyCache.js: `if (!state.invalid && state.groups && ...)` — `state.groups` NUNCA definido en state (solo `invalid`) → condición NEVER fires → cache se refetcheaa TODA vez. Comment L487-488 reconoce el bug-as-feature ("matches the bundle where the skip condition effectively never activates"). MX60 decidir: preservar tal cual O corregir con `historyData.groups.length > 0` check.

---

## §70.6 — Cross-refs

- **bloque #68 §68.1.6** split HistoryData prescription ← **AJUSTADA por #255** (Cache class ficticia) + **#256** (thread-safety bug)
- **bloque #68 §68.0 + §68.4** historyCache shape ← **CONFIRMADA por #262**
- **bloque #68 §68.4 ack patterns** ← **EXTENDIDA por #257** (asimetría 2 caminos documentada literal)
- **bloque #69 §69.4** alarmas NO WS push polling componentes ← **RE-CONFIRMA por #258 + #261**
- **bloque #69 §69.5** HistoryGhostSubscriber one-shot ← **RE-CITADO en §70.1 #15** (subscribeToHistory cross-station workaround)
- **bloque #69 §69.6** corrección mapping `domains/alarms.md` $niagara.alarmSubscribe NO existe ← **RE-CONFIRMA empírico** alarm namespace 20 métodos sin subscribe (#258)
- **bloque #62 §62.9.3** alarmas polling componentes ← **CONFIRMA al detectar polling paralelo durante ack** (#261)
- **engram #1236** methodology/mapping-vs-empirical-audit ← **CASO TIER-1 confirmado**: bloque #68 prescribió Cache class que NO existe (#255). Inferencia desde mapping fue WRONG.
- **engram #1238** methodology/clean-room-disconnected-asymmetry ← **APLICADA**: hallar 2 bugs latentes en clean-room (#263, #264) que mapping no reportó porque mapping sintetiza fuentes secundarias.
- **bloque #44** BAlarmService Niagara nativo ← **POTENCIAL** para MX60 si decide cablear `canAcknowledgeAlarms` en frontend (#259 opción b)
- **bloque #54** BReflowAlarmCommands audit (9 vs 7 métodos cross-check pendiente) ← **CERRADO TIER-1**: archivo tiene 9 métodos BOX exactos: getClasses, getAlarmByUuid, query, querySources, getUuidsForSources, getActiveAlarmCounts, getUnackedAlarmCounts, getAlarmsSinceTime, canAcknowledgeAlarms (L44-112).

---

## §70.7 — PARA EL YO 2027

Cuando MX60-alarms+history+charts arranque sprint-1 backend o sprint-4-6 frontend, **abrir bloque #70 PRIMERO antes de seguir bloque #68 al pie de la letra**. Bloque #68 sintetizó desde mapping y prescribió 2 cosas que el audit empírico TIER-1 corrigió:

1. **Split HistoryData en 3 clases incluyendo `HistoryDataCache`** → la cache class es FICTICIA. No existe state cacheable en backend. Split correcto = 2 clases (Engine+Serializer §70.1.6 Opción A) o 3 alternativo (Engine+Runner+Serializer §70.1.6 Opción B).
2. **(implícito) "ack flow es trivial, preservar forma"** → flow tiene 2 caminos asimétricos (#257) que MX60 DEBE preservar literal. Operadores tienen UX learned-helplessness: bulk siempre confirma, single nunca confirma.

Además **2 bugs latentes** en clean-room que NO fueron reportados ni en mapping ni en bloque #68:

- `last24Hours` returns 24 days (#263) — fix obligatorio
- skip-cache condition NEVER fires (#264) — preservar OR corregir, decidir

Y **1 dead-code para resolver**:

- `canAcknowledgeAlarms` BOX (#259) — borrar OR cablear OR documentar

Y **1 thread-safety bug latente**:

- `SimpleDateFormat`/`DecimalFormat` static fields (#256) — fix obligatorio en split

**Lección meta TIER-1 confirmada** (refuerza engram #1236): mapping `docs/mappings/reflow-clean-177/` es síntesis de fuentes secundarias. Para piezas críticas del transplante (split-decisions, flow-decisions, shape-decisions), **AUDITAR EMPÍRICAMENTE EL SOURCE ANTES de prescribir** — bloque #68 cayó en este error con HistoryDataCache (Cache prescrita sin verificar que no había state). Bloque #69 ya había detectado errores similares (HistoryGhostSubscriber NO keepalive, alarmas NO WS push). Bloque #70 cierra TIER-1 (las 3 piezas más críticas).

**Tally global post-Bloque 70**:
- 96 antipatterns AP-1..96
- 42 reglas template MX60 (1-42)
- **264 implications #1..#264**
- Capa 17 Reflow audit + Capa 18 Analytics + Capa 19 Transplante operacional EXTENDIDA con audit empírico TIER-1 cerrado
