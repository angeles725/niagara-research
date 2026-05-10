# Bloque 71 — Equipment domain TIER-1 audit triple-source: Reflow + MX60 + Bloques 1-70 guidance

**Sesión**: 2026-05-09 (post-mapping-mx60 + post-bloque #70)
**Source READ-ONLY (Reflow)**: `/home/cristian/modules/Prototipos/Reflow-Clean-177/`
**Source READ-ONLY (MX60)**: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/`
**Disciplina**: triple-source per engram #1259 — auditar Reflow actual (lo que funciona en producción con bugs), MX60 actual (lo que se decidió hasta ahora), bloques guidance (lo aprendido en research). NO sesgar. Recomendación final con justificación técnica de los 3 sources.
**Continuación de**: bloque #68 (transplante blueprint, §68.2 frontend Vue 3 + Pinia REFUTED por engram #1257) + bloque #69 (audit live-update patterns) + bloque #70 (TIER-1 HistoryData/Ack/historyCache).

---

## §71.0 — Resumen ejecutivo (3 veredictos)

| Item | Reflow approach | MX60 actual | Bloques guidance | Veredicto MX60 final |
|------|-----------------|-------------|------------------|----------------------|
| **A — Backend Equipment** | MINIMAL: solo 2 file-I/O endpoints para notas (`EquipmentNoteResponse.java:16` + `EquipmentNoteUpdateResponse.java:19`). NO BComponent, NO BOX commands. Toda la lógica vive en frontend Vuex store 1130 LOC. | ENRIQUECIDO: BChiUp 50 slots + lógica real (`started/changed/recompute/sync/purge`) + BChiCarcamo 8 slots + BChiDatalogger 9 slots + 3 monitors con seed-data idempotente + ChiEquipmentReader 866 LOC walk + ChiThresholdHelper 262 LOC allowlists. | Bloque #65 Decisión #7 BComponent service container pattern (BReflowService blueprint) + Regla 13 BJobService + Regla 11 cx propagation END-TO-END. | **PRESERVAR backend MX60 enriquecido** — descartar Reflow approach (file-I/O insuficiente para 6 plantas × 25 UPs). Mantener slot-based BComponent + monitors + reader. CORREGIR slotomatic regen (#284 BChiUp.java AWAITING) antes sprint-1 deploy. |
| **B — Frontend stack** | Vue 2.7 + Vuex + Vite + 44 components + mixins Options API. Subscriber via `$niagara.subscriber.subscribe(_uid, comp, cb)` patrón canónico (7 components). 0 setInterval. CRUD direct via Vuex commits 15+ sites. Cross-domain coupling alto (15+ external consumers). | ES module hybrid: 3 archivos ES module (UpDetail.js 3841 LOC + CarcamoDetail.js 1040 + SharedEnv.js) + 8 IIFE clásico restantes. UpDetail.js Three.js 3D scene + Chart.js 7 instances. Subscriber via `EquipmentSnapshotStore.subscribe()` + REST fallback 5s en `EquipmentData.js`. 0 setInterval directo en components. | Engram #1257: §68.2 Vue 3 + Pinia REFUTED — MX60 NO es Vue. Bloque #69 #246: subscriber canónico mounted→subscribe+heartbeatAdd / beforeDestroy→unsubscribe. Bloque #69 #249: `bajaHeartbeat.start(baja)` sprint-1 OBLIGATORIO. AP-82 subscriber leak. Regla 29 useSubscriber composable. | **PRESERVAR ES module hybrid + IIFE actual** — descartar Vue 3 migration short-term (3-6 sprints adicionales sin valor functional inmediato). Adoptar useSubscriber-equivalent IIFE wrapper (no composable Vue, sino IIFE function que encapsule lifecycle pair) en sprint-1. **OBLIGATORIO**: bajaHeartbeat.start sprint-1 (heredado #249). DEFERRED: migración Vue 3 a bloque #80+ con decisión producto/timeline. |
| **C — Threshold stores arquitectura** | Inline en items state (`equipment.js:51-98` 30+ state fields). Persistencia via JSON Patch sync config.json (PERSISTENT module). Sin write-through dedicated — toda mutación pasa por Vuex commits que luego se serializan al config. STUBS críticos: `pointsForTemplate`/`pointMapForTemplate` (`equipment.js:1041,1052`) sin implementar — `setTypePointsFromTemplate` y `duplicateType` actions retornan vacío en clean-room. | 5 threshold-stores dedicados: 3 con REST write-through + re-fetch (Up/Carcamo/Datalogger), 2 in-memory only (Modo/Output overrides optimistic). DataloggerThresholdStore SIN DEFAULTS (CarcamoThresholdStore tiene 45/69). Sincronización entre tabs ausente. Backend enforcement en BChiCarcamo/BChiDatalogger slots + ChiThresholdHelper allowlists per type. | Bloque #46 (writes priority array Niagara) — referenciado pero NO localizado en search. Bloque #65 Regla 11 cx propagation. Bloque #69 #248 BReflowChannelService genérico NO alarmas. AP-86 JSON Patch RFC 6902 SUBSET (Reflow bug). | **PRESERVAR MX60 dedicated stores** — descartar Reflow inline approach (acopla persistence con state, complica testing). MX60 ya tiene write-through correcto. **CORREGIR sprint-1**: (1) DataloggerThresholdStore agregar DEFAULTS (consistencia con Carcamo); (2) ModoOverrideStore + OutputOverrideStore agregar BroadcastChannel para sync tabs; (3) cx propagation Regla 11 en POST writes (ChiThresholdHelper write line 207 — verificar Context propagation). |

**Tally TIER-1 Equipment cerrado**: 3 veredictos arquitectónicos sintetizados de los 3 sources. Equipment domain transplant readiness para sprint-1 backend MX60 = **READY** con 5 fixes precisos (bug-free deploy).

---

## §71.1 — Bloques relevantes Equipment (Stage 0 mem_search)

| # | Topic | Guidance Equipment | Status |
|---|-------|---------------------|--------|
| #46 | writes priority array Niagara (citado por user prompt) | NO localizado en mem_search. Probable: priority array param en `$niagara.points.set(point, value, priority)` per engram #878 G50-2. Validación TIER-1 pendiente si transplant frontend writes en sprint-4+. | **partial** (búsqueda no encontró bloque dedicado — flag para investigar si existe bloque #46 o si guidance vive en bloque #44/#45/#65) |
| #50 | Reflow-177 Par A audit cross-stack (engram #878) | 5 canales comm: REST 28 endpoints + WS propio + BOX 21 methods + BajaScript directo + External APIs. AP-10 backup destructivo via GET. AP-12 `window.top.niagara.box` sin fallback. | **vigente** |
| #61 | Catálogo librerías + APIs Reflow + stack MX60 (engram #1128) | Stack Reflow: Vue 2.7 + Vite 5.4 + d3 7.9. Decisión MX60 greenfield: Vue 3 + Vuetify 3 + Vite 5 + Pinia + TS strict. | **REFUTED** post-#1257 stack pivot — MX60 actual es BajaScript IIFE + ES module hybrid, NO Vue 3 |
| #63 | Frontend Vue 2.7 audit completo 29 Vuex (engram #1133) | Equipment store rated POOR (sin file:line cita disponible — `docs/GAP-ANALYSIS.md` no localizado en repo Reflow-Clean-177). AP-82 subscriber leak. Regla 29 useSubscriber composable. AP-86 JSON Patch SUBSET. | **partial** — POOR rating no verificable empíricamente; AP-82 + Regla 29 vigentes pero adapt to IIFE no composable |
| #65 | Síntesis backlog MX60 + 7 decisiones arquitectónicas (engram #1136) | Regla 11 cx propagation END-TO-END (AP-27 sistémico ~66 sites). Regla 13 BJobService obligatorio (AP-42+49+70). Decisión #7 BComponent service container pattern. | **partial** — Regla 11+13+Decisión #7 vigentes; Decisión #6 frontend Pinia 1:1 REFUTED por #1257 |
| #68 | Transplant blueprint Reflow → MX60 alarms+history+charts (engram #1235) | §68.1 Backend HEREDA 95% — vigente. §68.2 Frontend Vue 3 + Pinia — REFUTED por #1257. §68.1.5 HistoryGhostSubscriber keepalive — REFUTED por #69. §68.1.6 HistoryDataCache class — REFUTED por #70. | **REFUTED parcial** — backend §68.1 vigente; frontend §68.2 REFUTED; §68.1.5 + §68.1.6 corregidos |
| #69 | Audit empírico patrones live-update Reflow (engram #1237) | 3 patrones: setInterval polling alarms+charts+weather / BajaScript subscriber points+cards+schedules / on-demand pull history-data. #246 patrón canónico subscriber lifecycle. #249 bajaHeartbeat.start MUST sprint-1. #248 BReflowChannelService genérico NO alarmas. | **vigente** |
| #70 | TIER-1 audit HistoryData split + Ack flow + historyCache (engram #1242) | Methodology TIER-1: audit empírico pre-transplant. Bug detection patterns: operator precedence (`last24Hours`), NEVER fires conditions (`state.groups`), thread-safety formatters (SimpleDateFormat). | **vigente** — methodology re-usable Equipment |
| #1257 | mx60/stack-pivot-iife-not-vue (engram methodology) | MX60 chihuahua es BajaScript IIFE classic (refinado a IIFE + ES module hybrid post-#71). 3 caminos frontend: (a) migración Vue 3 (b) preservar IIFE (c) hybrid. | **vigente** + refinado por #71 (es ES module hybrid no IIFE puro) |
| #1258 | framing/reflow-mx60-bilateral-critique | Reflow = referencia funcional con bugs. MX60 = nuevo, decisiones cuestionables. NO sesgar — sintetizar ambos + bloques. | **vigente** |
| #1259 | methodology/triple-source-reflow-mx60-bloques | Stage 0 mem_search obligatorio. 3 sources: Reflow + MX60 + bloques. Recommendations con justificación técnica. | **vigente** — aplicada en este bloque |
| #1238 | clean-room-disconnected-asymmetry | Reflow-Clean-177 backend mockeado. Para runtime real consultar bundle 1.7.5 o lab. Estructura estática autoritativa, runtime NO. | **vigente** — confirmado en #71 (`api/rest.js:243,255` STUBS Phase 5+) |

---

## §71.2 — Stage 1: Audit Reflow Equipment empírico

### §71.2.1 — Backend MINIMAL

| File:line | Class | Profile | REST endpoint | Purpose |
|-----------|-------|---------|---------------|---------|
| `nmodsreflow-rt/.../EquipmentNoteResponse.java:16` | EquipmentNoteResponse | Static handler | GET `/nmodsreflow/station/equipment-notes` | Lee `^reflow/notes/<Equipment-Id>.json`; retorna `[]` si no existe |
| `nmodsreflow-rt/.../EquipmentNoteUpdateResponse.java:19` | EquipmentNoteUpdateResponse | Static handler | POST `/nmodsreflow/station/equipment-notes-update` | Escribe body a temp file, valida content-length, copia, borra temp con Timer 2s |

NO existe `BReflowEquipmentCommands.java`, NO BOX, NO BComponent. Toda la lógica de tipos/items/puntos/badges/grupos vive enteramente en el frontend.

### §71.2.2 — Frontend Equipment store (1130 LOC)

`reflow-frontend/src/store/modules/equipment.js`:
- **State** (L51-98): 30+ propiedades (items, badges, types 10 tipos default AHU/Boiler/Chiller/CoolingTower/FCU/MUA/RTU/VAV/VFD/WHP, baseTypes copia inmutable, equipmentTick reactive trigger)
- **Getters** (L404-1057): 25+ getters incluyendo `getDevicesBetter` (cache TTL 2s), `getDeviceBuildingMap` (cross-rootGetter buildings), `getDeviceFloors` (cross-rootGetter floorplans)
- **Mutations** (L101-334): 40+ mutations
- **Actions** (L1059-1122): 3 — `setTypePointsFromTemplate`, `duplicateType`, `deleteType`
- **Persistence**: PERSISTENT (NO en EXCLUDED_KEYS) — sync completo a config.json via JSON Patch
- **Module-level cache** (L337-351): `_pointsCache`, `_indexCache`, `_typeTemplateCache`, `_deviceFilterCache` con TTL 2s + `equipmentTick` invalidation

### §71.2.3 — Frontend components (44 .vue)

Top 10 componentes por LOC:

| File | LOC | Subscriber? | Notas |
|------|-----|-------------|-------|
| PickerModal.vue | 582 | No | Listado paginado/filtrado in-memory, sin live-update |
| EquipmentItemList.vue | 559 | No | CRUD pura via Vuex commits |
| DeviceCard.vue | 483 | **Sí** (par mounted:164/beforeDestroy:171) | `$niagara.subscriber.subscribe(_uid, ...)` para featured + status point |
| EquipmentTypeSummaryEditor.vue | 414 | No | Editor de tipo con templates |
| EquipmentEditor.vue | 379 | No | CRUD complejo |
| DeviceTitle.vue | 358 | **Sí** (mounted:203/beforeDestroy:207) | Subscriber para room slot |
| GroupCard.vue | 346 | **Zombie** (unsub sin sub) | mounted:166 vacío + beforeDestroy:169 con unsubscribe (bug L165-171) |
| EquipmentTypeForm.vue | 344 | No | — |
| EquipmentItemRemap.vue | 335 | No | $niagara.backups.* |
| DeviceRow.vue | 314 | **Sí** (par mounted:167/beforeDestroy:149) | Same pattern as DeviceCard |

### §71.2.4 — Patrones identificados

| Patrón | Frecuencia | Notas |
|--------|------------|-------|
| BajaScript subscriber via `$niagara.subscriber` | 7 components | Mock en clean-room — runtime real per #1238 |
| Unsubscribe en beforeDestroy paired | 10 components | 2 zombies (GroupCard + GroupRow llaman unsubscribe sin sub) |
| BOX command Equipment-específico | **0** | api/box.js existe pero NO métodos Equipment — todos STUB |
| REST endpoints Equipment | 2 (notes only) | Ambos STUB en clean-room (`api/rest.js:243,255` retornan `Promise.resolve()`) |
| CRUD direct via Vuex commit | 15+ sites | NO pasa por actions — components commitean mutations |
| setInterval polling | **0** | Equipment es event-driven Vuex reactivity + subscriber |
| Module-level cache TTL 2s | 1 store | `_deviceFilterCache` invalidado por `equipmentTick` |

### §71.2.5 — Bugs documentados Reflow Equipment

1. `equipment.js:1041,1052` — `pointsForTemplate`/`pointMapForTemplate` STUB Phase C — `setTypePointsFromTemplate` y `duplicateType` retornan vacío en clean-room. **inferred from clean-room mock per #1238**
2. `equipment.js:149` — `SET_SUMMARY_STATUS_LABELS` setea `summaryStatusBadges` (mismatch nombre mutation vs propiedad)
3. `GroupCard.vue:171` + `GroupRow.vue:120` — zombie unsubscribes (sin sub). Cross-ref AP-82 categoría
4. `equipment.js:962-964` — LIVE BUG documentado: getter `getGroupDeviceTypes` ausente causaba `EquipmentGroupOrder.vue:62` retornar `[]` silenciosamente
5. `DeviceCard.vue:241-243` — catch de status point swallowed silently (sin error visible)
6. `api/rest.js:243,255` — Equipment notes endpoints STUB. Funcionalidad `enableNotes` nunca ejecuta backend real. **inferred from clean-room mock**
7. `equipment.js:421-422` + `:489-491` — DRIFT CORRECTIONS documentadas: divergencia replica vs bundle 1.7.5 en `getItemsWithPoints` y `getGroups`

### §71.2.6 — Cross-domain coupling

15+ stores/components fuera de equipment consumen el store:
- `floorEditor.js:414,420,1080`
- `pointMapData.js:514-515`
- `dashboard/LandingEquipment.vue:50-51`
- `floorplans/{Floor,Canvas,FloorEquipment}.vue`
- `buildings/{BuildingLayout,BuildingCard}.vue`
- `points/{Point,Group,ClassicPoint}*.vue`
- `views/{DeviceDetailsView,EquipmentGroupView}.vue`
- `mixins/{equipment,equipmentList}Mixin.js`

**Coupling alto** vs MX60 (low): consecuencia de Vuex global state + mapState everywhere.

---

## §71.3 — Stage 2: Audit MX60 Equipment empírico

### §71.3.1 — Backend BChiUp.java (50 slots — NO 37 del mapping)

`chihuahua-rt/.../BChiUp.java` LOC 1941, slotomatic hash `1785712449`. **50 slots @NiagaraProperty** distribuidos en 9 grupos:

| Grupo | Cant | Líneas | Default |
|-------|------|--------|---------|
| Identity (label) | 1 | L33 | `""` |
| Position (X, Y) | 2 | L41, L47 | `0.0` StatusNumeric |
| Read-only numeric feedback (planta, tempZona, tempAbasto, tempRetorno, tempExterior, humedadZona, tempSuccion1/2, ampCompresor1/2, ampAbanicos1/2, ampFan, deltaAbastoRetorno) | 15 | L55-138 | `0.0` |
| Read-only boolean feedback (fanOn, compressor1On/2On, protectorFase, switchAlta1/2, switchBaja1/2) | 8 | L141-188 | `false` (excepto protectorFase=`true`) |
| Writable commands (setpoint, modoOperacion, fanCmd, comp1Cmd, comp2Cmd) | 5 | L191-220 | varies |
| Writable thresholds (sobrecargaFan, sobrecargaCompresor1/2, sobrecargaAbanicos1/2, antifrezzeSistema1/2) | 7 | L223-264 | `0.0` |
| Setpoint control (manualSetpoint, effectiveSetpoint) | 2 | L267-278 | `22.0` |
| Protection output (protFanActive, protCompresor1Active/2Active, protAbanicosS1/2Active, protAntifrezzeS1/2Active, protFaseActive) | 8 | L285-332 | `false` |
| Alarm latch + Schedule mirror (alarmLatches, setpointSchedule) | 2 | L340, L352 | `"{}"`, `0.0` |

**Discrepancia mapping vs realidad**: mapping decía 37 — los 13 extra vienen de 3 batches post-mapping (chihuahua-protection-batch +10 + chihuahua-charts-alarms-schedules-batch +1 + chihuahua-bugs-and-persistence-batch +1, más corrección boolean group +1).

**Methods business logic**:
- `started()` L1566 — `ensureSchedule()` + `recomputeEffectiveSetpoint()`
- `changed(Property, Context)` L1585 — recompute en cambios de modoOperacion / manualSetpoint / schedule
- `recomputeEffectiveSetpoint()` L1612 — switch MANUAL/SCHEDULE/SETPOINT, epsilon `<= 0.001`
- `purgeAlarmLatches(long maxAgeMs)` L1718 — hand-rolled JSON brace-depth scanner (evita dep externa per L1711-1716 comment)
- `parseAlarmLatchesKeys(String json)` L1855 — Set\<String\> de keys activos
- `syncProtectionSlots()` L1904 — itera `LATCH_TO_PROT_SLOT` LinkedHashMap

**Constants**: `HYSTERESIS_FACTOR = 0.95` (L1537), `LATCH_TO_PROT_SLOT` (L1549) 8 entries inmutable.

### §71.3.2 — BChiCarcamo (8 slots) + BChiDatalogger (9 slots) + 3 monitors

**BChiCarcamo** L309 / 8 slots / pure container. Slots: label, posX, posY, planta, nivelCm, state, umbralAdvertencia, umbralCritico.

**BChiDatalogger** L338 / 9 slots / pure container. Slots: label, posX, posY, planta, pressurePsi, pressureBar, state, umbralAdvertencia, umbralCritico.

**Monitors** (UpMonitor / CarcamoMonitor / DataloggerMonitor):
- Pattern: seed-data → `started()` → `readParentPlantaIndex()` → `ensureX(plantaIdx)` idempotente
- UpMonitor L257 / 57 UPs en 5 plantas + 8 special (CARRIER\_1..5, LABORATORIO, MAQUINARIA). Wire Sheet layout WS_COLS=8.
- CarcamoMonitor L155 / 6 cárcamos (C5_P2, C6_P2, C7_P5, C8_P5, C9_P4, C10_P4) con positionX/Y reales (porcentaje).
- DataloggerMonitor L154 / 5 dataloggers (DT_P1, DT_P1_HP, DT_P2, DT_P3, DT_P5) con positionX/Y reales.

### §71.3.3 — ChiEquipmentReader (866 LOC) + ChiThresholdHelper (262 LOC)

**ChiEquipmentReader.java** — utility class, ORD raíz `station:|slot:/Services/ChiDashboardService` (L68 — cambio v2 desde `/Drivers/MX60/Chihuahua`). Walk pattern:
```
buildEquipmentResponse(BComponent context)
  → BOrd.make(DRIVER_TREE_ORD).get(context, null)
  → readAllFromService(service)
      → for n=1..6: getChildComponent(service, "Planta"+n)
          → readFromMonitor(planta, "UpMonitor", "up", n)
          → readFromMonitor(planta, "CarcamoMonitor", "carcamo", n)
          → readFromMonitor(planta, "DataloggerMonitor", "datalogger", n)
              → monitor.getPropertiesArray()
              → readSlotsFromUp/Carcamo/Datalogger(equip, slotName)
  → buildEquipmentResponseFromDataList(list)
```
ID: `slotName.toLowerCase().replace('_', '-')` (L312). Nullable readers (L539-582): `readNumericNullable`, `readBoolNullable` retornan Java `null` en `isFault()/isDisabled()/isNull()`. `nullIfZero(double v)` (L492) epsilon `0.001`.

**ChiThresholdHelper.java** — package-private final class. Allowlists per type:
- `UP_THRESHOLD_KEYS` L31 — 7 keys
- `CARCAMO_THRESHOLD_KEYS` L41 — 2 keys
- `DATALOGGER_THRESHOLD_KEYS` L46 — 2 keys

Read endpoints: GET `/mx60/api/{up|carcamo|datalogger}/{ord}/thresholds`. Write L207: `comp.set(prop, new BStatusNumeric(value), null)` directo BOrd-resolved component. Validación L210 (allowlist) + L216 (`isFinite() && >= 0`).

### §71.3.4 — Frontend UpDetail.js (3841 LOC) — ES module con Three.js

`chihuahua-ux/src/rc/js/app/UpDetail.js` 3841 LOC verified.

**ES module** (NO IIFE clásico). Líneas 12-14:
```javascript
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { getEnvTexture } from './SharedEnv.js';
```
Línea 1 comment: `"ES module — uses the importmap declared in index.html"`. L16: `const MX60 = window.MX60 = window.MX60 || {};`

**Globals**:
- Written: `MX60.HistoryIndex` (L408 IIFE auto-invocado), `MX60.HistoryListCache` (L472 IIFE auto-invocado), `MX60.UpDetail = { mount: mount }` (L3830)
- Read top 8: `MX60.StatusResolver`, `MX60.AlarmLatchStore`, `MX60.EquipmentData`, `MX60.EquipmentSnapshotStore`, `MX60.UpThresholdStore`, `MX60.OutputOverrideStore`, `MX60.ModoOverrideStore`, `MX60.ParticleAnimation`

**Subscribers BajaScript directo**: 0. UpDetail NO habla BajaScript directamente. Depende de:
- `MX60.EquipmentSnapshotStore.subscribe(equip.id, onFlush)` L3555 — path principal (RAF + 500ms throttle)
- Fallback: `MX60.EquipmentData.addListener(onDataChange)` L3565

**Polling**: `setInterval` count 0. NO polling propio.

**Sub-clusters internos** (~17):
| Cluster | Líneas |
|---------|--------|
| Module-level helpers (escHtml, fmtNum) | 1-151 |
| Const config (READINGS, UP_THRESHOLDS, ALARM_LOCKDOWN) | 90-131 |
| Const analytics (COMFORT, TEMP_GAUGES, RANGES) | 157-176 |
| Analytics helpers (_arcGaugeSvg, _computeTrend) | 188-400 |
| MX60.HistoryIndex IIFE | 408-470 |
| MX60.HistoryListCache IIFE | 472-537 |
| _fetchSlotHistory + _mergeSeriesByTimestamp | 540-700 |
| HTML builders (_cockpitHtml, _analyticsHtml, _thresholdsSectionHtml) | 700-1145 |
| mount() Three.js scene setup | 1151-1900 |
| mount() animation loop (animate, applyState) | 1900-2195 |
| mount() side panel DOM refs + updateSidePanel | 2193-2300 |
| mount() setpoint write (_applySetpoint, onSpInput/Blur/Key) | 2302-2400 |
| mount() device toggle write (FAN/COMP1/COMP2 MANUAL) | 2400-2500 |
| mount() threshold UI subscribe + renderThresholdDots | 2500-2870 |
| mount() alarm latch UI (latching, notes, reset) | 2870-2960 |
| mount() modoOperacion buttons | 2960-3050 |
| mount() Chart.js infra (createChart, rebuildChart, _appendLiveSample) | 3050-3400 |
| mount() range tabs + baseline | 3395-3490 |
| mount() schedule link (Niagara WebScheduler) | 3473-3484 |
| mount() EquipmentData listener + onFlush | 3486-3566 |
| mount() destroy() | 3568-3682 |
| mount() history load staggered | 3682-3825 |

### §71.3.5 — CarcamoDetail (1040 LOC) + DataloggerDetail (700 LOC) + 5 threshold-stores

**CarcamoDetail.js** ES module + Three.js (water surface animation).
**DataloggerDetail.js** IIFE clásico (sin Three.js — usa imágenes pre-renderizadas VERDE/NARANJA/ROJO). Threshold comparison **INVERTIDA** (`lectura < umbral` = degradado, presión baja = problema).

**Threshold stores**:
| Store | LOC | Pattern | Persistence | Defaults | Sync tabs |
|-------|-----|---------|-------------|----------|-----------|
| ModoOverrideStore | 65 | IIFE | In-memory only | N/A | **NO** |
| OutputOverrideStore | 93 | IIFE | In-memory only | N/A | **NO** |
| UpThresholdStore | 198 | IIFE | REST RT + WT + re-fetch | N/A | NO |
| CarcamoThresholdStore | 216 | IIFE | REST RT + WT + re-fetch | umbralAdvertencia=45, umbralCritico=69 | NO |
| DataloggerThresholdStore | 205 | IIFE | REST RT + WT + re-fetch | **AUSENTES** | NO |

### §71.3.6 — Patrones identificados MX60

| Patrón | Frecuencia | Notas |
|--------|------------|-------|
| ES module hybrid (import + window.MX60) | 3 archivos | UpDetail / CarcamoDetail / SharedEnv (Three.js dependency) |
| IIFE clásico `(function(window){'use strict';...})(window)` | 8 archivos | Mayoría — sin Three.js |
| BComponent pure property container | 2 (BChiCarcamo, BChiDatalogger) | BChiUp es excepción con lógica |
| Seed-monitor → ensureX() idempotente | 3 monitors | Pattern v4 con plantaIdx filter |
| Nullable readers fault discrimination | 2 métodos | ChiEquipmentReader L539-582 |
| nullIfZero umbrales | 1 método | epsilon 0.001 ChiEquipmentReader L492 |
| REST write-through + re-fetch | 3 threshold stores | POST → 200 → init() llamado de nuevo |
| `_evalThresholdColor()` duplicada | 2 archivos (HomeMap, EquipmentCard) | Sin shared helper |
| RAF + hard cap throttle | 1 (EquipmentSnapshotStore) | 500ms default configurable |
| Two-phase render READS-then-WRITES | 1 (UpDetail onFlush) | Anti-pattern fix layout thrashing |
| Staggered chart rebuild via RAF | 1 (UpDetail repaintAllCharts) | 7 Chart.js evita layout thrashing sync |
| Brace-depth JSON scanner sin lib | 2 métodos BChiUp | Evita dep externa RT classpath |

### §71.3.7 — Bugs latentes MX60 Equipment

1. **HISTORY_MINUTES cap aritmético confuso** (`UpDetail.js:396`) — `24 * 60 = 1440` correcto, pero `> HISTORY_MINUTES + 600 = 2040` cap. Bajo REST fallback (5s poll) un turno de 8h = 5760 entries. Trim O(N) sobre array grande.
2. **ModoOverrideStore + OutputOverrideStore tabs sync ausente** — operador con 2 tabs ve estado inconsistente. NO localStorage / NO BroadcastChannel / NO WS.
3. **DataloggerThresholdStore SIN DEFAULTS** — inconsistente con CarcamoThresholdStore (45/69). `get()` retorna null sin trigger.
4. **`protectorFase` null-safety bug** (`UpDetail.js:2272`) — `s.protectorFase === false ? 'alarm' : 'ok'`. Si sensor faulted y `protectorFase = null`, `null === false` es `false` → muestra 'ok'. LED verde con sensor desconectado.
5. **`slotNameToId()` no valida non-standard slots** (`ChiEquipmentReader.java:312`) — `String.replace(char, char)` reemplaza todas. No validación para slots con múltiples underscores no estándar.
6. **BChiUp slots AWAITING SLOTOMATIC REGEN** (`BChiUp.java:284, 351`) — 8 protection slots + setpointSchedule marcados pendiente. Hash comment `3236798429` vs slotomatic real `1785712449`. Posible desincronización `.bog` binary si slotomatic no re-ejecutado.

### §71.3.8 — Decisiones implícitas no documentadas en mapping

- **5 threshold-stores en lugar de 1 genérico**: cada tipo tiene semántica de comparación diferente (UP=7 keys con direction over/under, Carcamo=2 keys lectura > umbral con DEFAULTS, Datalogger=2 keys lectura < umbral sin DEFAULTS).
- **ES modules en 3 archivos**: solo los que importan Three.js (UpDetail, CarcamoDetail, SharedEnv). DataloggerDetail usa imágenes pre-renderizadas, queda IIFE.
- **No sync tabs para overrides**: arquitectura asume operador único o aceptación de inconsistencia transitoria.
- **`HISTORY_MINUTES = 24 * 60` literal**: preserva intención semántica (días × horas-por-día) para futuros cambios.
- **`purgeAlarmLatches` parser propio**: evita dep JSON externa en RT classpath controlado.

---

## §71.4 — Triple comparison por sub-tema (Stage 3)

7 sub-temas críticos:

### Sub-tema 1: Backend Equipment scope

| | Reflow | MX60 | Bloques guidance |
|---|--------|------|------------------|
| Approach | MINIMAL: 2 file-I/O endpoints (notas) | ENRIQUECIDO: 50+ slots + lógica + monitors + reader + helper | #65 Decisión #7 BComponent service container; #1257 §68.1 backend HEREDA 95% |
| LOC | ~150 LOC (notas) | ~5500 LOC (RT + UX backend) | — |
| Tensión | Reflow no tiene contract Niagara — pierde licensing visibility, alarm coupling, history coupling | MX60 acoplado a Niagara nativo — gana visibility pero requiere 6 plantas BChiPlanta hierarchy | bloques afirman BComponent pattern superior |

### Sub-tema 2: Live-update mechanism

| | Reflow | MX60 | Bloques guidance |
|---|--------|------|------------------|
| Approach | Subscriber `$niagara.subscriber.subscribe(_uid, comp, cb)` 7 components, 0 setInterval, 0 fallback | Subscriber via SubscriptionPool → EquipmentSnapshotStore.subscribe + REST fallback 5s en EquipmentData | #69 #246 subscriber canónico mounted+heartbeatAdd / beforeDestroy+heartbeatRemove+unsubscribe; #249 bajaHeartbeat.start sprint-1 OBLIGATORIO |
| Lifecycle | Paired correcto en 7 components, 2 zombies (GroupCard, GroupRow) — AP-82 risk no realizado | EquipmentSnapshotStore central — single subscribe per equipId + RAF flush; UpDetail destroy() L3568 explicit | bloques afirman lifecycle pair MUST + heartbeat MUST |
| Tensión | Reflow direct subscriber per component (correcto pero replicado N veces); MX60 store-mediated (single subscribe + N listeners). Trade-off: simplicity vs central choke point. | bloques no diferencian: #246 prescribe lifecycle pair, no menciona store-mediated |

### Sub-tema 3: Threshold persistence pattern

| | Reflow | MX60 | Bloques guidance |
|---|--------|------|------------------|
| Approach | Inline en items state (config.json sync via JSON Patch). Sin write-through dedicated. | 5 dedicated stores con REST write-through + re-fetch (3 con REST + 2 in-memory optimistic) | #65 Regla 11 cx propagation; #65 Regla 13 BJobService async (no aplica directo); AP-86 JSON Patch SUBSET (Reflow bug RFC 6902) |
| Tensión | Reflow simpler (sin REST extra layer) pero acopla persistence con state — testing complica + JSON Patch SUBSET bug latente. MX60 más complejo (5 stores) pero separation of concerns + write-through optimistic. | bloques no prescriben — Regla 11 sí aplica (cx propagation en POST writes) |

### Sub-tema 4: Override pattern (modo / output commands)

| | Reflow | MX60 | Bloques guidance |
|---|--------|------|------------------|
| Approach | N/A — Reflow NO tiene pattern equivalente a MX60 ModoOverrideStore/OutputOverrideStore | Stores in-memory optimistic per-tab. Bug: sin sync entre tabs | bloques sin guidance directa |
| Tensión | Reflow sin pattern (todo via Vuex commits + backend writes); MX60 capa optimista que se sincroniza eventualmente vía próximo poll/subscriber | — |

### Sub-tema 5: 3D rendering & ES modules

| | Reflow | MX60 | Bloques guidance |
|---|--------|------|------------------|
| Approach | N/A — Reflow NO usa Three.js. SVG via D3 | UpDetail + CarcamoDetail Three.js 3D scene + OrbitControls + textures (SharedEnv). DataloggerDetail static images (sin Three.js) | #1257 stack pivot — MX60 ES module hybrid post-#71 refinamiento |
| Tensión | Reflow más portable (SVG funciona en cualquier browser). MX60 más rich UX (3D rotation/zoom) pero requiere importmap config + ES module loader compat | bloques no prescriben — engram #1257 documenta el pivot |

### Sub-tema 6: Cross-domain coupling

| | Reflow | MX60 | Bloques guidance |
|---|--------|------|------------------|
| Approach | HEAVY — 15+ external consumers del store equipment (floorplans, buildings, dashboard, points, views, mixins) | LOW — threshold stores leídos por Detail components + EquipmentCard + HomeMap (3-4 consumers each) | #65 Regla 11 cx propagation END-TO-END (66 sites en Reflow per AP-27) |
| Tensión | Reflow Vuex global state facilita coupling. MX60 IIFE namespace impone friction natural — coupling más visible. | bloques afirman cx propagation crítica |

### Sub-tema 7: Bug latente patterns

| | Reflow | MX60 | Bloques guidance |
|---|--------|------|------------------|
| Approach | STUBs documentados (`pointsForTemplate`, equipment-notes), zombie unsubscribes, name mismatch SET_SUMMARY_STATUS_LABELS, AD3 crash risk fallback | 6 latent bugs identificados: HISTORY_MINUTES cap, tabs sync absent, DataloggerThresholdStore sin DEFAULTS, protectorFase null-safety, slotNameToId no valida, slotomatic AWAITING REGEN | #70 methodology TIER-1 (operator precedence, NEVER fires conditions, thread-safety formatters); engram #1238 clean-room asymmetry; engram #1236 mapping-vs-empirical-audit |
| Tensión | Reflow bugs frecuentes pero documentados (AD3, DRIFT). MX60 bugs latentes no documentados — descubiertos en este audit. | bloques afirman audit empírico TIER-1 obligatorio pre-transplant |

---

## §71.5 — Recommendations finales con justificación técnica (Stage 4)

### R1 — Backend Equipment: PRESERVAR MX60 enriquecido, descartar Reflow approach

**Recommendation**: mantener BChiUp 50 slots + lógica + 3 monitors + ChiEquipmentReader + ChiThresholdHelper. Heredar de Reflow: nada (Reflow es file-I/O insuficiente para escala MX60 6 plantas × 25 UPs × thresholds + alarms).

**Justificación**:
- (a) MX60 ya tiene contract Niagara nativo (BIControlPoint inheritance + alarm coupling + history coupling) — ganancia visibility no triviales
- (b) Reflow approach (file-I/O) requeriría 1500+ LOC adicionales en MX60 para replicar lo que BComponent + slotomatic ya da (history extensions, alarm extensions, schedule extensions)
- (c) Bloque #65 Decisión #7 BComponent service container pattern es la dirección bloques-aligned
- (d) Engram #1257 §68.1 backend HEREDA 95% del transplante de alarms+history+charts — el patrón backend-rich aplica también a Equipment

**Caveat**: corregir 1 issue antes sprint-1 deploy:
- **C1**: BChiUp slots AWAITING SLOTOMATIC REGEN (L284, L351) — re-ejecutar slotomatic para asegurar `.bog` binary sincronizado con código fuente. Verificar hash post-regen.

### R2 — Live-update mechanism: PRESERVAR MX60 store-mediated + agregar bajaHeartbeat sprint-1

**Recommendation**: mantener EquipmentSnapshotStore central (single subscribe + N listeners + RAF throttle 500ms) + EquipmentData REST fallback 5s. **OBLIGATORIO sprint-1**: `bajaHeartbeat.start(baja)` (heredar de bloque #69 #249).

**Justificación**:
- (a) Store-mediated subscribe es superior a per-component subscribe (Reflow approach) en escala — single choke point para 6 plantas × 25 UPs = 150 BComponents posibles vs 150 subscribers individuales
- (b) RAF throttle 500ms previene layout thrashing en UpDetail (7 Chart.js + Three.js + DOM updates) — anti-pattern fix documentado L3495-3541
- (c) bloque #69 #249 bajaHeartbeat.start sprint-1 OBLIGATORIO — sino tabs backgrounded pierden subscriptions silenciosamente. Aplica idéntico a Equipment como a points/cards/schedules
- (d) AP-82 subscriber leak no realizado en MX60 (single choke point en store), pero protección heartbeat sigue siendo necesaria contra browser tab eviction

**Caveat**: NO migrar MX60 a per-component subscriber pattern de Reflow — eso sería regresión.

### R3 — Threshold persistence: PRESERVAR MX60 dedicated stores + 3 fixes sprint-1

**Recommendation**: mantener 5 threshold-stores con REST write-through + re-fetch. Heredar de Reflow: nada (inline state + JSON Patch SUBSET es bug latente AP-86).

**Justificación**:
- (a) Separation of concerns: stores no acoplan persistence con item state — testable individualmente
- (b) REST write-through + re-fetch es robust pattern: optimistic update + backend confirmation + cache refresh
- (c) Bloque #65 AP-86 Reflow JSON Patch SUBSET (RFC 6902 partial) es bug latente multi-user — MX60 evita por arquitectura
- (d) Bloque #65 Regla 11 cx propagation END-TO-END aplica al POST write (`ChiThresholdHelper.java:207` `comp.set(prop, value, null)` — verificar Context propagation, ese `null` es flag)

**Caveats sprint-1 obligatorios**:
- **C2**: DataloggerThresholdStore agregar DEFAULTS (consistencia con CarcamoThresholdStore 45/69). Operador sin config no debe ver 'verde' silencioso.
- **C3**: ModoOverrideStore + OutputOverrideStore agregar BroadcastChannel API para sync entre tabs. Sin esto, operador con 2 tabs ve estado inconsistente.
- **C4**: cx propagation Regla 11 — verificar `ChiThresholdHelper.java:207` propaga Context (no `null`). Sino aplicar fix.

### R4 — Override pattern (modo/output): MX60 INVENTA — preservar con C3 fix

**Recommendation**: mantener ModoOverrideStore + OutputOverrideStore como capa optimista + agregar BroadcastChannel sync (C3 arriba).

**Justificación**: Reflow no tiene equivalente — MX60 inventa este pattern. La capa optimista cubre el gap entre user click y backend write confirmation (REST + BajaScript propagation). Sin ella, UI parece "trabada" mientras llega la próxima update del store. Bloques no prescriben — pattern válido.

### R5 — 3D rendering: PRESERVAR ES module hybrid

**Recommendation**: mantener UpDetail + CarcamoDetail + SharedEnv como ES modules (Three.js dependency). DataloggerDetail puede quedar IIFE (sin Three.js).

**Justificación**:
- (a) Three.js 0.150+ es ES module library — `import` requerido
- (b) importmap declarado en index.html ya soluciona compat — no hay friction
- (c) Engram #1257 documenta el hybrid como decisión empírica válida — refinada en este bloque
- (d) Migración a Vue 3 + bundler (Vite) eliminaría hybrid pero costaría 3-6 sprints sin valor functional inmediato — DEFERRED a bloque #80+ con decisión producto/timeline

### R6 — Cross-domain coupling: MX60 LOW es superior

**Recommendation**: mantener MX60 IIFE namespace coupling LOW. NO migrar a Vuex/Pinia global state pattern de Reflow.

**Justificación**:
- (a) Reflow 15+ external consumers del store equipment es deuda — bloque #63 AP cross-domain coupling
- (b) Bloque #65 Regla 11 cx propagation END-TO-END es la regla — global state hace cx propagation harder de auditar
- (c) MX60 IIFE namespace impone friction natural — coupling más visible y reviewable
- (d) Si futuro requiere coupling extra (ej. dashboard widgets que lean equipment), agregar via subscribe pattern al store específico — NO via global state

### R7 — Bug detection: aplicar #70 methodology TIER-1 a 6 bugs latentes MX60

**Recommendation**: documentar los 6 bugs latentes Equipment como `[OPEN-MX60]` y resolver pre-sprint-1 deploy.

**Justificación**: Bloque #70 estableció methodology TIER-1 (audit empírico pre-transplant). Aplicarla a Equipment domain antes de cerrar sprint-1 backend evita propagación a sprint-2-6 frontend.

**Bugs a resolver**:
- **B1**: HISTORY_MINUTES cap REST fallback — bajar cap a 1440 estricto + trim aggressive O(1) usando ring buffer
- **B2**: ModoOverrideStore + OutputOverrideStore tabs sync (cubierto en C3)
- **B3**: DataloggerThresholdStore DEFAULTS (cubierto en C2)
- **B4**: protectorFase null-safety — fix `s.protectorFase === true ? 'ok' : (s.protectorFase === false ? 'alarm' : 'unknown')`
- **B5**: slotNameToId validar non-standard slots — agregar regex check `/^[a-zA-Z][a-zA-Z0-9_-]*$/` previo a replace
- **B6**: BChiUp slotomatic regen (cubierto en C1)

---

## §71.6 — Decisiones MX60 sprint-1 Equipment domain

### Heredar (de Reflow)
- **NADA** del backend (Reflow es MINIMAL, MX60 ya superior)
- **NADA** del frontend stack (Reflow Vue 2.7 != MX60 IIFE+ES module hybrid post-#1257)
- **Patrón conceptual subscriber lifecycle pair** (de bloque #69 #246) — adaptar a IIFE no composable

### Descartar (de Reflow)
- File-I/O backend approach (`EquipmentNote*Response.java`)
- Vuex inline threshold state pattern (acoplamiento + AP-86 JSON Patch SUBSET bug)
- Per-component subscriber pattern (escala mal a 150+ BComponents)
- 15+ cross-domain consumers pattern (Vuex global state)
- STUBs heredados (`pointsForTemplate`, `pointMapForTemplate`)

### Preservar (de MX60 actual)
- BChiUp 50 slots + lógica `started/changed/recompute/sync/purge`
- BChiCarcamo + BChiDatalogger pure containers
- 3 monitors con seed-data idempotente
- ChiEquipmentReader walk pattern + nullable readers
- ChiThresholdHelper allowlists per type
- 5 threshold-stores dedicados con REST write-through + re-fetch
- ES module hybrid (UpDetail + CarcamoDetail + SharedEnv) + IIFE classic resto
- EquipmentSnapshotStore single-subscribe central + RAF flush
- ModoOverrideStore + OutputOverrideStore optimistic layer

### Inventar (NUEVO en MX60 sprint-1)
- **`bajaHeartbeat.start(baja)` IIFE wrapper** — invocar al boot post `MX60.SubscriptionPool.isReady()`. Heredar regla #249.
- **BroadcastChannel sync layer** para ModoOverrideStore + OutputOverrideStore (C3 fix).
- **DataloggerThresholdStore DEFAULTS** (C2 fix) — alinear con CarcamoThresholdStore.
- **cx propagation Regla 11 audit** sobre `ChiThresholdHelper.java:207` — verificar Context no `null`.

---

## §71.7 — Implications nuevas #265..#284

#265 Backend Equipment MX60 enriquecido SUPERIOR a Reflow MINIMAL — preservar BChiUp 50 slots + lógica + monitors + reader (no heredar Reflow file-I/O approach)
#266 Reflow Equipment backend = MINIMAL (2 file-I/O endpoints solamente) — confirmado empíricamente, NO BReflowEquipmentCommands NO BOX NO BComponent
#267 BChiUp 50 slots reales (NO 37 mapping) — discrepancia explicada por 3 batches post-mapping (chihuahua-protection-batch +10 + chihuahua-charts-alarms-schedules-batch +1 + chihuahua-bugs-and-persistence-batch +1)
#268 BChiUp slots AWAITING SLOTOMATIC REGEN bug latente — re-ejecutar slotomatic obligatorio pre-sprint-1 deploy (C1)
#269 ES module hybrid MX60 confirmado empírico — refinamiento engram #1257: NO IIFE puro, sino IIFE classic (8 archivos) + ES module con Three.js (3 archivos UpDetail/CarcamoDetail/SharedEnv)
#270 Reflow Equipment frontend 0 setInterval — Vuex reactivity + subscriber + module-level cache TTL 2s suficientes; REST polling NO usado
#271 MX60 EquipmentSnapshotStore single-subscribe + RAF 500ms throttle SUPERIOR a per-component subscriber Reflow approach (escala 150+ BComponents)
#272 bajaHeartbeat.start(baja) sprint-1 OBLIGATORIO Equipment domain (heredar #249) — sin esto tabs backgrounded pierden subscriptions
#273 5 threshold-stores MX60 con REST write-through + re-fetch pattern SUPERIOR a Reflow inline state (sin JSON Patch SUBSET bug AP-86)
#274 DataloggerThresholdStore SIN DEFAULTS bug latente — agregar consistencia con CarcamoThresholdStore (45/69) sprint-1 (C2)
#275 ModoOverrideStore + OutputOverrideStore tabs sync ausente bug latente — agregar BroadcastChannel sprint-1 (C3)
#276 cx propagation Regla 11 audit obligatorio en ChiThresholdHelper.java:207 `comp.set(prop, value, null)` — verificar Context no `null` (C4)
#277 protectorFase null-safety bug latente UpDetail.js:2272 — fix `=== true ? 'ok' : (=== false ? 'alarm' : 'unknown')` para sensor faulted
#278 slotNameToId no valida non-standard slots ChiEquipmentReader.java:312 — agregar regex check pre-replace
#279 HISTORY_MINUTES cap UpDetail.js:396 confuso bajo REST fallback — bajar a 1440 estricto + trim O(1) ring buffer
#280 BChiUp lógica real (started/changed/recomputeEffectiveSetpoint/syncProtectionSlots/purgeAlarmLatches) NO replicada en BChiCarcamo/BChiDatalogger (pure containers) — design intentional NO refactor a inheritance
#281 Cross-domain coupling MX60 LOW (Detail + Card + HomeMap) SUPERIOR a Reflow HIGH (15+ external consumers) — preservar IIFE namespace friction natural NO migrar a Vuex/Pinia global state
#282 ModoOverrideStore + OutputOverrideStore inventan pattern NUEVO no presente en Reflow — capa optimista válida cubrir gap user-click vs backend-confirmation
#283 ChiEquipmentReader walk pattern (BOrd + readAllFromService + plantas 1..6 + monitors UpMonitor/CarcamoMonitor/DataloggerMonitor + readSlotsFromX) reusable para Analytics module y futuras extensiones
#284 Reflow Equipment STUBs (`pointsForTemplate`/`pointMapForTemplate` equipment.js:1041,1052) NO heredar a MX60 — `setTypePointsFromTemplate` y `duplicateType` actions retornan vacío en clean-room (engram #1238 clean-room-disconnected-asymmetry confirma)

**Tally global post-Bloque 71**: 96 antipatterns AP-1..96 + 42 reglas template MX60 (1-42) + **284 implications #1..#284** + Capa 19 Transplante operacional EXTENDIDA con audit empírico TIER-1 Equipment triple-source.

---

## §71.8 — Cross-refs

- **#46** writes priority array Niagara — referenciado en prompt user pero NO localizado por mem_search; flag para investigar (probable: guidance vive en bloque #44/#45/#65)
- **#50** Reflow-177 Par A audit cross-stack — 5 canales comm (engram #878), AP-10 backup destructivo, AP-12 fallback `window.top.niagara.box`
- **#61** Catálogo librerías + APIs Reflow + stack MX60 (engram #1128) — REFUTED post-#1257
- **#63** Frontend Vue 2.7 audit 29 Vuex (engram #1133) — Equipment store rated POOR (sin file:line empírico), AP-82 subscriber leak, Regla 29 useSubscriber
- **#65** Síntesis backlog MX60 (engram #1136) — 7 decisiones arquitectónicas, Regla 11 cx propagation, Regla 13 BJobService, Decisión #7 BComponent service container
- **#68** Transplant blueprint (engram #1235) — §68.1 backend vigente, §68.2 frontend REFUTED por #1257, §68.1.5 + §68.1.6 corregidos por #69 + #70
- **#69** Audit live-update patterns (engram #1237) — #246 subscriber canónico, #248 BReflowChannelService NO alarmas, #249 bajaHeartbeat.start MUST sprint-1
- **#70** TIER-1 audit methodology (engram #1242) — operator precedence + NEVER fires + thread-safety formatters bug detection
- **mapping-mx60** archive (engram #1254) — Equipment domain entries en index.json + delta.json + xref.json + 4 domain docs (equipment-backend, equipment-detail, equipment-frontend, threshold-stores)
- **#1257** mx60/stack-pivot-iife-not-vue — confirmado empírico + refinado a IIFE+ES module hybrid en este bloque
- **#1258** framing/reflow-mx60-bilateral-critique — aplicada en Stage 4 recommendations
- **#1259** methodology/triple-source-reflow-mx60-bloques — aplicada en este bloque end-to-end
- **#1238** clean-room-disconnected-asymmetry — confirmado empírico (Reflow `api/rest.js:243,255` STUBS Phase 5+)
- **#1236** mapping-vs-empirical-audit — RE-CONFIRMADO TIER-1: mapping decía 37 slots BChiUp, realidad 50 slots (diferencia 13 = 3 batches post-mapping no documentados en mapping)

---

## §71.9 — PARA EL YO 2027

Cuando arranque MX60 sprint-1 backend Equipment domain, abrir bloque #71 PRIMERO. Las decisiones arquitectónicas están sintetizadas — NO re-discutir backend vs frontend stack ni inventory threshold-store pattern. Las 4 cosas a hacer en sprint-1:

1. **Re-ejecutar slotomatic** sobre BChiUp.java (C1 bug #268). Verificar hash `1785712449` actualiza si los 8 protection slots + setpointSchedule fueron agregados post-último regen. `.bog` binary necesita estar en sync con Java source.
2. **DataloggerThresholdStore agregar DEFAULTS** (C2 bug #274). Alinear con CarcamoThresholdStore 45/69. Sin esto operador sin config ve 'verde' silencioso (engineer-paranoia: pressure abnormal sin trigger).
3. **ModoOverrideStore + OutputOverrideStore agregar BroadcastChannel** (C3 bug #275). Operador con 2 tabs hoy ve estado inconsistente — fix obligatorio antes de demo a cliente.
4. **ChiThresholdHelper.java:207 cx propagation audit** (C4 #276). El `null` en `comp.set(prop, value, null)` es Context flag — verificar Regla 11 cumplida o aplicar fix.

Para sprint-2-6 frontend Equipment domain: bloque #71 §71.5 R5 (preservar ES module hybrid) + R6 (preservar IIFE coupling LOW). NO migrar a Vue 3 a menos que producto/timeline lo requiera (decisión bloque #80+).

Si surge nuevo dominio (ej. Analytics Equipment), reusar ChiEquipmentReader walk pattern (#283). El BOrd traversal con plantas 1..6 + monitor delegation es reusable.

Si el mapping-mx60 cita 37 slots BChiUp, NO confiar — el audit empírico §71.3.1 confirmó 50. Mapping fue antes de 3 batches.

Si Reflow Equipment domain se referencia como guidance, ojo: Reflow Equipment es **MINIMAL backend + heavy frontend**. NO replicar — MX60 es contraria (heavy backend + ES module frontend). El triple-source dice MX60 path is correct.

Bloque #71 cerró TIER-1 Equipment empírico. Próximo: bloque #72 (próxima sesión, prompt corto en `NEXT_SESSION_PROMPT_BLOQUE_72.md`).
