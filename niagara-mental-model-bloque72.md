# Bloque 72 — Alarms domain TIER-1 audit triple-source (Reflow + MX60 + bloques)

> Fecha: 2026-05-10 · Capa 19 (Transplante operacional) · Audit empírico TIER-1
> Methodology: triple-source (engram #1259) + bilateral-critique framing (engram #1258)
> Output: ~700 LOC · 11 secciones §72.0-§72.10
> Cross-refs: #44, #54, #62, #68 §68.1+§68.4, #69, #70, #71

---

## §72.0 — Resumen ejecutivo: 3 veredictos

**Hallazgo central (refuta el framing inicial del prompt)**: MX60 NO está vacío en alarms. Tiene **dos sistemas coexistentes** con responsabilidades distintas:

- **Sistema 1 — Latch system** (BChiUp.alarmLatches JSON property + 8 slots typed protXActive + LATCH_TO_PROT_SLOT mapping + UpDetail.js latch UI + AlarmLatchStore client-store + REST /api/alarms/latch|unlatch). Modelo TRIP/RESET binario para protección activa de equipment, persistido en Niagara property + mirrored a slots wirables en Workbench.
- **Sistema 2 — Niagara native ack flow** (ChiAlarmHelper 2041 LOC + BChiServlet HTTP endpoints /api/alarms/ack|notes|sources|hyperlink + AlarmsPage 824 LOC equipment-style + AlarmsManager fetch+retry + AlarmNotesModal + AlarmModalActions Poptip). Modelo Reflow-style con BAlarmDb native + ack canónico via reflection probe A-007.

**3 veredictos arquitectónicos**:

### (A) Ack flow capability
**MX60 YA TIENE ack flow funcional end-to-end**, NO es gap como sugería el prompt. Evidencia file:line: `ChiAlarmHelper.ackAlarms()` L353 + `BChiServlet.handleAlarmAck` L1158 + `MX60.AlarmsManager.ackAlarms` AlarmsManager.js:261 (POST /mx60/api/alarms/ack + 3x retry exponential backoff 100/200/400ms) + `AlarmsPage._handleBulkAck` L549-611 (MX60.Confirm + bulk ack). El gap real NO es "ack as concept" — es **paridad de feature richness con Reflow** (sounds / priority / multi-console / notes obligatorias / CSV server-side).

### (B) Capabilities gap real (Reflow → MX60)
**6 features Reflow tiene y MX60 NO**:
1. Priority management DUAL MODE (class-based + range-based con custom labels/colors)
2. Sound notifications (11 MP3 lib + checkAlarmSounds + autoplay graceful fallback)
3. Multi-console configuration (N consoles JSON Patch independientes)
4. Alarm class management UI (filter + listado classes)
5. `acknowledgmentRequiresNote` gate (notes obligatorias antes de ack)
6. CSV export **server-side** streaming (MX60 hace client-side capped at MAX_ALARMS=200)

### (C) Capabilities exclusivas MX60 (gap inverso, NO presente en Reflow)
**5 features que MX60 tiene y Reflow NO**:
1. Manual latch trigger (operador fuerza trip apagando device — UpDetail.js:2636 `_confirmManualLatch`)
2. Reset latch desde UpDetail (limpia sin ir a página alarms — UpDetail.js:2663 `_confirmReset`)
3. LATCH_TO_PROT_SLOT mirror → 8 slots typed `protXActive` wirables en Workbench (BChiUp.java:1549-1562)
4. `alarmLatches` como Niagara property (persistido + ORD-exposed para Wire)
5. Reflection probe A-007 BAlarmService.ackAlarm (compat iSMA 4.13.2 vs N4.14, 4 strategies cached at class-load)

**Conclusión ejecutiva**: el SDD pending mx60-alarms-latch-mode-change debe **mantenerse focused** en el latch system + reset behavior. La paridad con Reflow (priority/sounds/multi-console) es **scope separado** que vale TIER-2 sprint posterior, NO TIER-1 pre-sprint-1. Detalle escenarios en §72.7.

---

## §72.1 — Bloques relevantes Alarms (Stage 0)

| # | Topic | Guidance Alarms | Status vigente |
|---|---|---|---|
| #44 | Alarm Console pipeline Niagara nativo (BIAlarmCursor) | BAlarmDb canonical patterns + BAlarmRecord.acknowledge() jamás reimplementar | ✅ vigente — confirmado por ChiAlarmHelper L353 reflection probe |
| #54 | BReflowAlarmCommands audit cerrado TIER-1 | 9 BOX methods exactos + UUID canonicalization | ✅ vigente |
| #62 | Alarmas Reflow dedicated audit | 78 antipatterns AP-72..78 + 22 reglas + 175 implications #161-175 + decisión arquitectónica #5 (WebSocket push P0 obligatorio) | ⚠️ **partial** — decisión #5 (WS push) sigue siendo gap GLOBAL, ambos Reflow+MX60 usan polling 20s |
| #68 §68.1 | Backend transplant blueprint Java | KEEP UUID canon + defensive copy + LinkedHashMap pagination + BQL safety + thread timeout | ✅ vigente — refuted §68.1.5/6 (HistoryGhostSubscriber + HistoryDataCache) ya corregidos #69+#70 |
| #68 §68.4 | Ack flow (extendida en #70) | $niagara.alarm.ackAlarmsByUuid BajaScript NATIVE + 2 caminos asimétricos | ✅ vigente Reflow / **N/A literal MX60** (MX60 usa REST POST /api/alarms/ack porque iSMA 4.13.2 no expone BAlarmDb directamente — reflection probe necesaria) |
| #69 | Live-update polling 20s + BReflowChannelService genérico | alarmas SIEMPRE polling, NO WS push (#244 #248) + bajaHeartbeat.start sprint-1 obligatorio (#249) | ✅ vigente — MX60 confirmá: AlarmsPage.js:19 `REFRESH_INTERVAL=20000` + AlarmsManager 5s polling badge |
| #70 | TIER-1 ack flow asimétrico 2 caminos + canAcknowledgeAlarms BOX dead-code + acknowledgmentRequiresNote 100% frontend gate | 11 implications #254-264 | ✅ vigente Reflow / **MX60 simplifica**: 1 camino (AlarmsPage bulk con MX60.Confirm), NO replica asymmetry |
| #71 | Equipment audit + LATCH_TO_PROT_SLOT pattern | BChiUp 50 slots + ES module hybrid + IIFE classic + EquipmentSnapshotStore | ✅ vigente — base estructural para alarmLatches este bloque |
| engram #1236 | mapping-vs-empirical-audit | Mapping síntesis ≠ ground truth | ✅ aplicado — confirmado este bloque (prompt asumía MX60 sin ack; empírico revela ack completo) |
| engram #1238 | clean-room-disconnected-asymmetry | Clean-room 177 NO conectado a backend | ✅ aplicado — Reflow alarmCache.js es STUB L1 (61L) |
| engram #1257 | mx60-stack-pivot IIFE no Vue | Frontend MX60 IIFE classic + ES module hybrid | ✅ vigente — MX60 alarms UI confirma IIFE classic (`(function(window) { 'use strict'; ... })(window);`) |
| engram #1258 | reflow-mx60 bilateral-critique framing | Reflow=production reference + MX60=actual ground truth + bloques=research guidance | ✅ vigente |
| engram #1259 | Triple-source methodology | Stage 0 obligatorio + audit empírico + recommendation con evidencia 3-source | ✅ aplicado bloque 72 |
| engram #1244 | Pending post-bloque-70 corrections | Si aplica: chore inline corrections | ⚠️ N/A — no encontrada por mem_search en este audit; se asumió no-bloqueante |

**Tally bloques aplicables al dominio alarms**: 11 bloques + 5 engram methodology entries. NINGUNO refuted — todos vigentes para guiar recomendaciones.

---

## §72.2 — Audit Reflow Alarms empírico (Stage 1)

### §72.2.1 — Backend Java

Source: `/home/cristian/modules/Prototipos/Reflow-Clean-177/nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/`

| Archivo | LOC | Rol | Methods clave |
|---|---|---|---|
| `commands/BReflowAlarmCommands.java` | 113 | 9 BOX methods (`BIServerSideCallHandler`) | `getClasses` L44, `getAlarmByUuid` L56, `query` L65, `querySources` L75, `getUuidsForSources` L85, `getActiveAlarmCounts` L95, `getUnackedAlarmCounts` L99, `getAlarmsSinceTime` L103, `canAcknowledgeAlarms` L109 ⚠️ DEAD-CODE (per #70) |
| `alarms/AlarmData.java` | 439 | 13 static methods + thread + AccessController privileged | `getAlarmClasses` L64, `getAlarmsSinceTime` L96, `getAlarmByUuid` L107 (UUID validation BQL injection prevention L111), `getActiveAlarmCounts` L119, `getUnackedAlarmCounts` L132, `query` L145 (QueryTask thread + AccessController.doPrivileged + thread.join), `streamAlarmsCSV` L162 (server-side CSV streaming), `querySources` L177, `streamSourcesCSV` L212, `getUuidsForSource` L238 (uses `BAlarmService.getAlarmDb().getDbConnection`), `buildBQLQuery` L340 |
| `alarms/AlarmSourceCollection.java` | 82 | LinkedHashMap pagination (#62 KEEP) | `add` + `getSortedSources` |
| `alarms/AlarmUuidArgs.java` | 73 | Robust exception fallbacks (#62 KEEP) | constructor parses BComponent args |
| `alarms/QueryFilter.java` | — | Filter spec | `make(BComponent)` |
| `alarms/ReflowAlarmSource.java` | 25 | Defensive copy (#62 KEEP) | `lastRecord` + `totalCount` + `ackCount` |
| `http/responses/AlarmCSVResponse.java` | — | REST handler para CSV | streams via AlarmData.streamAlarmsCSV |
| `http/responses/AlarmQueryResponse.java` | — | REST handler para query JSON | wraps AlarmData.query/querySources |

**Hallazgos backend**:
- **NO hay código de ack en backend**. Ack delegado 100% a `$niagara.alarm.ackAlarmsByUuid` BajaScript native → `BAlarmRecord.acknowledge()` canónico (per engram #1129 #62).
- `canAcknowledgeAlarms` L109-112 es **DEAD-CODE confirmed** (cero call sites frontend per #70 §70.2.4 — solo definición + stub + JSDoc).
- UUID validation regex previene BQL injection en `getAlarmByUuid` L111.
- Thread + AccessController.doPrivileged en `query` L145-153 — patrón potencialmente problemático (AP-74 thread.join() sin timeout per #62).
- CSV export es **server-side streaming** (`streamAlarmsCSV` + `streamSourcesCSV`) sin cap explícito — limita por `MAX_QUERY_LIMIT=1000` L62 en BQL `select top` clause.

### §72.2.2 — Frontend Vue (27 components + 2 views + 3 stores)

Source: `/home/cristian/modules/Prototipos/Reflow-Clean-177/reflow-frontend/src/`

| Archivo | LOC | Rol |
|---|---|---|
| `views/AlarmsHome.vue` | ~498 | Home page — SourceGroupsTable + AlarmAckConfirm + bulk flow |
| `views/AlarmDetails.vue` | ~498 | Detail page — AlarmsTable + AlarmCards + RequiredNoteModal + per-row flow |
| `components/alarms/AlarmDisplay.vue` | 295 | Navbar count polling AlarmDisplay.vue:168 setInterval |
| `components/alarms/AlarmsTable.vue` | 427 | Tabular display + batch + emit('ack', payload) L306,310 |
| `components/alarms/AlarmCards.vue` | 371 | Card display + emit('ack', payload) L246 |
| `components/alarms/SourceGroupsTable.vue` | 656 | Groups + emit @ack-all + 4 call sites a `getUuidForSources` (L350,360,376,386) |
| `components/alarms/AlarmAckConfirm.vue` | 92 | Modal "Are you sure" + branch `acknowledgmentRequiresNote` (L67) → save() o RequiredNoteModal |
| `components/alarms/RequiredNoteModal.vue` | 95 | Gate textarea required + saveNote → addNotes → emit ack |
| `components/alarms/AlarmNotes.vue` | 124 | Inline notes display |
| `components/alarms/AlarmNotesModal.vue` | — | Full modal notes history |
| `components/alarms/AlarmConsoleForm.vue` + `AlarmConsoleList.vue` | — | Multi-console config CRUD |
| `components/alarms/AlarmPriorityPicker.vue` + `AlarmPriorityType.vue` + `AlarmPrioritiesForm.vue` + `PriorityColorsForm.vue` | — | Priority management DUAL MODE (class-based + range-based) |
| `components/alarms/AlarmStatusPicker.vue` | — | Status filter (active/ackState) |
| `components/alarms/AlarmSoundsForm.vue` + `AlarmSoundsPicker.vue` | — | 11 MP3 sounds library config |
| `components/alarms/AlarmIconsForm.vue` + `AlarmRowStyleForm.vue` + `AlarmSummaryForm.vue` | — | Visual customization |
| `components/alarms/AlarmClassList.vue` | — | Alarm class management UI |
| `components/alarms/ConsoleRefreshRateForm.vue` | — | Polling rate config |
| `components/alarms/AlarmsTableForm.vue` + `SourcesTableForm.vue` | — | Column visibility config |
| `components/alarms/BuildingAlarmSummary.vue` | — | Cross-domain consumer (per-building summary) |
| `components/alarms/Total.vue` | 128 | Aggregate count badge |
| `store/modules/alarms.js` | 238 | Vuex module — `consoles[]` persisted via JSON Patch (defaultConsole 70+ props L5-69) — `consoleRefreshRate=20` L24 + `acknowledgmentRequiresNote` flag (frontend gate per #70 §70.2.5) + DUAL priority `classPriorities[]` L21 + `rangePriorities {low:100, high:200}` L22 + `styles{}` L23 |
| `store/modules/alarmData.js` | 155 | Vuex transient — priorityForRecord dual mode + inAlarmCount filtered |
| `lib/alarmCache.js` | 61 | **STUB** L1 "Original: bundle Fa lines 15058-15177 ... Full implementation requires active Niagara alarm API — stub for now" (engram #1238 confirmed) |

**Total Vue components/alarms/**: 4947 LOC en 27 archivos.

### §72.2.3 — Flow ack end-to-end (re-confirmando #70)

**Path A — AlarmsHome bulk** (con AlarmAckConfirm modal):
```
SourceGroupsTable @ack-all=confirmAckAll (Reflow flow:)
└→ AlarmsHome.vue:98 @ack-all="confirmAckAll"
   └→ AlarmsHome.vue:428 confirmAckAll(records) → ackAllRecords + ackAllModal=true
      └→ AlarmAckConfirm.vue:9 modal "Are you sure" @on-ok=confirmAck
         └→ AlarmAckConfirm.vue:67 if (acknowledgmentRequiresNote) ackNoteModal.openModal(records)
            ├→ ELSE branch: save()
            │   └→ AlarmAckConfirm.vue:80 await $niagara.alarm.ackAlarmsByUuid(records)
            │       └→ AlarmAckConfirm.vue:83 emit('load-alarms') 
            │           └→ AlarmsHome.vue:113 @load-alarms=getAlarmSources (PESSIMISTIC re-fetch)
            └→ NOTE branch: RequiredNoteModal opened → saveNote → addNotes → emit ack → save()
```

**Path B — AlarmDetails row/card** (sin AlarmAckConfirm):
```
AlarmsTable.vue:306,310 / AlarmCards.vue:246 emit('ack', payload)
└→ AlarmDetails.vue:88 @ack="ack" (table) / :119 @ack="ack" (cards)
   └→ AlarmDetails.vue:369 ack(payload) → if (acknowledgmentRequiresNote) ackNoteModal.openModal(uuid)
      ├→ ELSE: sendAck(uuid) directo (sin "Are you sure")
      └→ NOTE: requiredNoteAck → sendAck → ackNoteModal.closeModal
         └→ AlarmDetails.vue:378 sendAck(uuids) → ackAlarmsByUuid + loadAlarms (PESSIMISTIC re-fetch)
```

**Backend ack mechanism**: `$niagara.alarm.ackAlarmsByUuid(uuids)` BajaScript NATIVE (NO BOX RPC) → `BAlarmRecord.acknowledge()` canónico (engram #1129 KEEP-5). `canAcknowledgeAlarms` BOX dead-code (#70 §70.2.4 + impl #259).

**Live update parallel**: AlarmsHome.vue:355-369 setInterval(refreshRate=20s) ejecuta paralelo durante ack: `checkAlarmSounds` + `getAlarmSources` + `getAlarmClasses` + `playAlarmSound`. AlarmDisplay.vue:168 polling separado para navbar count.

### §72.2.4 — Bugs conocidos Reflow alarms

- ✅ **FIXED-S50** V-P0-8 AlarmsTable + V-P0-9 AlarmCards (engram session 2026-05 confirmed). Cierre con bullet `[FIXED-S50/S54/S55]` (per commit `d99c6ea` de la sesión actual).
- ⚠️ **canAcknowledgeAlarms BOX dead-code** (BReflowAlarmCommands.java L109-112) — sigue presente. Decisión sprint-1 backend por bloque #70: (a) borrar (b) wire-up frontend (c) preservar dead-code documentado.
- ⚠️ **alarmCache.js stub** (lib/alarmCache.js L1) — clean-room-177 disconnected (engram #1238). NO consumido en componentes (#251).
- ⚠️ **AP-72..78 (#62)**: autoplay sin graceful fallback / polling no push / thread.join sin timeout / CSV close sin flush / missing RLS BAlarmRecord / no frontend pagination strict / concurrent note adds race.

---

## §72.3 — Audit MX60 Alarms empírico (Stage 2)

Source: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/`

### §72.3.1 — Sistema 1: Latch system (BChiUp.alarmLatches)

| Pieza | Archivo:Linea | Rol |
|---|---|---|
| Property `alarmLatches` | `BChiUp.java:340-345` (annotation) + L1471-1495 (slot accessors) | JSON-encoded map persisted en Niagara property: `{"<thresholdKey>":{"latched":true,"latchedAt":<epochMs>,"latchedBy":"<user>","note":"<text>"}}`. Default `"{}"`. Multi-user policy: last-write-wins v1. Max practical ~5KB (10 active latches con notes). Purge >30d. |
| 8 protection slots | `BChiUp.java:280-332` | `protFanActive` / `protCompresor1Active` / `protCompresor2Active` / `protAbanicosS1Active` / `protAbanicosS2Active` / `protAntifrezzeS1Active` / `protAntifrezzeS2Active` / `protFaseActive` — todos `BStatusBoolean` SUMMARY wirables en Workbench. AWAITING SLOTOMATIC REGEN L284 (bug #71 C1). |
| `LATCH_TO_PROT_SLOT` mapping | `BChiUp.java:1549-1562` | Immutable `LinkedHashMap<String,String>` 8 entries: latchKey → slot name (deterministic serialization). |
| `purgeAlarmLatches(maxAgeMs)` | `BChiUp.java:1718-1821` | Purga entries >30d. Hand-rolled brace-depth JSON parser (NO external lib). Llamado por `BChiDashboardService.controlTick` cada 60th tick (~10 min). |
| `parseAlarmLatchesKeys` | `BChiUp.java:~1855` (mentioned mem #1242) | Parser keys del JSON map |
| `syncProtectionSlots()` | `BChiUp.java:1904+` | Itera `LATCH_TO_PROT_SLOT`, parsea alarmLatches JSON, set protXActive true/false. Llamado cada controlTick. |
| `_extractLatchedAtLocal` | `BChiUp.java:1824-` | Helper extract latchedAt para purge. |
| `latchAlarm(BChiUp, key, user, note)` | `ChiAlarmHelper.java:189` | Set latch en BChiUp.alarmLatches. |
| `unlatchAlarm(BChiUp, key)` | `ChiAlarmHelper.java:229` | Clear latch. |
| `getLatches(BChiUp)` | `ChiAlarmHelper.java:254` | Return `Map<String,String>` parsed. |
| `purgeOldLatches(BChiUp, maxAgeMs)` | `ChiAlarmHelper.java:281` | Wrapper que llama `BChiUp.purgeAlarmLatches`. |
| `BChiServlet.handleAlarmLatch` | `BChiServlet.java:1021-1079` | POST `/api/alarms/latch` — body `{ord, thresholdKey, note}`. Audit log INFO. |
| `BChiServlet.handleAlarmUnlatch` | `BChiServlet.java:1090-1145` | POST `/api/alarms/unlatch` — body `{ord, thresholdKey}`. |
| `MX60.AlarmLatchStore` | `AlarmLatchStore.js` 268L | Client store + `seedFromEquipment(equipArr)` reset+populate from REST + `latch/reset/setNote/isLatched/hasAnyLatched/latchedKeys/getAll`. ordMap[equipId]→ord para write-through. |
| `UpDetail.js` latch UI | L2555-2823 (~270 LOC) | `_confirmManualLatch(key)` L2636 (forzar trip + apaga devices + nota), `_confirmReset(key)` L2663 (single reset con warning re-evaluación), `_confirmResetAll()` L2680 (reset all latches del up), `_commitAlarmNote(key)` L2698 (nota inline). Integrado con `MX60.Confirm` modal genérico. |

**Modelo conceptual latch**: TRIP/RESET binario sticky. Una lectura que viola threshold dispara el latch via backend (BChiUp recompute). El operador puede:
1. Resetear manualmente desde UpDetail → si lectura sigue violando, vuelve a alarmar (warning explícito UpDetail.js:2672).
2. Forzar latch manual desde UI → apaga el device asociado (`ALARM_LOCKDOWN[thresholdKey]`).
3. Editar nota inline mientras está latched (last-write-wins).

NO hay concepto Niagara-native ack en este sistema. Latch ≠ alarm record en BAlarmDb.

### §72.3.2 — Sistema 2: Niagara-style ack flow (BAlarmDb + ChiAlarmHelper)

| Pieza | Archivo:Linea | Rol |
|---|---|---|
| `ChiAlarmHelper` | `ChiAlarmHelper.java` 2041 LOC | Stateless utility (final + private constructor). Patrón ported de `SnlsAlarmHelper.java` (BQL + cursor + ack pattern). |
| Reflection probe A-007 | `ChiAlarmHelper.java:60-330` (header L60-77 + static init) | Class-load resolves `BAlarmService.ackAlarm(BOrd[], String)` via reflection. iSMA 4.13.2 ENUMERATION 2026-05-08: canonical N4 method name es `ackAlarm` (NO `acknowledge` ni `acknowledgeAlarm`). 4 strategies cached: 0=BAlarmService.ackAlarm(BAlarmRecord) preferred / 1=BAlarmRecord.ackAlarm(String user) / 2=BAlarmRecord.ackAlarm() / 3=BAlarmService.doAckAlarm(BAlarmRecord) last resort. -1=none → stub mode. |
| `ackAlarms(uuids[], username, context)` | `ChiAlarmHelper.java:353-484` | Returns `AckResult{ackedCount, failedCount, errors[]}`. UUID validation regex `[0-9a-fA-F-]+` L404. BQL select por uuid L415. Per-uuid try/catch + cursor.close finally. ackStrategy switch L429-454. |
| Notes infra | `ChiAlarmHelper.java:486-572` | NOTES_MAP `ConcurrentHashMap<String,List<NoteEntry>>` + PERSIST_LOCK + lazy bootstrap `ensureNotesLoaded` L535 + persistence file `^chihuahua-alarm-notes.json` con atomic move (POSIX rename / Windows MoveFileEx via BFileSystem). NO native BAlarmDatabase note API en iSMA 4.13.2 — workaround. |
| `getNotesByUuid(uuid)` | `ChiAlarmHelper.java:576` | Returns JSON array string. |
| `addNote(uuids[], note, author)` | `ChiAlarmHelper.java:608` | Append note + persist. Failure mode: I/O error logs SEVERE pero NO throws (in-memory map ya tiene la nota). |
| `resolveHyperlinkSafe(ord)` | `ChiAlarmHelper.java:677` | Null-guard hyperlink ords. |
| `queryAlarms(range, ackState, page, ...)` | `ChiAlarmHelper.java:1169` | BQL execution. Page size DEFAULT_PAGE_SIZE=200 L54. |
| `getAlarmCounts(out, context)` | `ChiAlarmHelper.java:1220` | Counts dump. |
| Time windows | `ChiAlarmHelper.java:1492-1539` | `_todayWindow` / `_yesterdayWindow` / `_thisMonthWindow` — pure functions testables WSL. |
| `queryAlarmsBySource(sourceOrd, range, limit, ...)` | `ChiAlarmHelper.java:1891` | Per-source query. |
| `BChiServlet.handleAlarmAck` | `BChiServlet.java:1158-1203` | POST `/api/alarms/ack`. Body: `{uuids:[],note}`. Response 200: `{ackedCount, failedCount, errors[]}`. extractJsonStringArray hand-rolled. setRequestHeader audit. remoteUser fallback "unknown". |
| `ChiServletDispatch` routing | `ChiServletDispatch.java:184, 357` | `/api/alarms/ack` dispatch. Otros endpoints visible: `/api/alarms`, `/api/alarms/sources` (L240 mentions ChiAlarmQueryHelper.queryAlarmSources), `/api/alarms/notes`, `/api/alarms/notes/{uuid}`, `/api/alarms/hyperlink`, `/api/alarms/latch`, `/api/alarms/unlatch`. |
| `MX60.AlarmsManager` | `AlarmsManager.js` 326 LOC | In-memory cache + fetch layer + listener pattern. MAX_ALARMS=200 L35 (Fix B15 cap). 5s polling badge H1 L75-80 (cadencia leída de config api.alarmCounts.pollMs). `loadAll(cb)` con _loading guard B6 + _pendingCbs queue. `getFiltered({priority,type})` AND filter. `getCount() → {high, med, low, total, byType:{up,carcamo,datalogger}}`. `ackAlarms(uuids, note, opts)` L261 → POST `/mx60/api/alarms/ack` con 3x retry exponential backoff [100,200,400ms] + reload alarm list on success. |
| `MX60.AlarmsPage` | `AlarmsPage.js` 824 LOC | Registered as `MX60.DashboardApp.registerPage('alarms', MX60.AlarmsPage)`. SDD-A equipment-style toolbar+grid+pagination + planta-tabs + state-tabs + dedicated source page. SDD-B bulk select + bulk action bar + CSV export. REFRESH_INTERVAL=20000 L19. PAGE_SIZE=12 L20. Sources URL fallback `/mx60/api/alarms/sources?range=today` L66-71. `_handleBulkAck` L549-611 → MX60.Confirm.show + dispatch ackAlarms + clear selection + setTimeout 600ms re-fetch. |
| `MX60.AlarmModalActions` | `AlarmModalActions.js` 240 LOC | Poptip placement="left" 5 buttons (REQ-G5-poptip): AcknowledgeAll, AcknowledgeSingle, Hyperlink (navigate or disabled), Notes (opens AlarmNotesModal), ViewAlarms (navigates to alarms page). `_postAck` XHR helper L50-63 ad-hoc (NO usa AlarmsManager.ackAlarms — duplicated path). |
| `MX60.AlarmNotesModal` | `AlarmNotesModal.js` 251 LOC | Full-screen modal. GET `/api/alarms/notes/{uuid}` on open + POST `/api/alarms/notes` on save. Author from `MX60.SharedEnv.getUser()` fallback "Operador". History list newest-first L93. |
| `MX60.AlarmCards` | `AlarmCards.js` 368 LOC | Card display alarms |
| `MX60.AlarmDetailPage` | `AlarmDetailPage.js` 481 LOC | Per-alarm detail page |
| `MX60.AlarmDetailsTable` | `AlarmDetailsTable.js` 386 LOC | Tabular detail view |

**Total MX60 alarms UI**: 3144 LOC en 8 archivos IIFE classic ES5 strict. Backend: 2041 LOC ChiAlarmHelper + 379 LOC ChiAlarmQueryHelper + handlers en BChiServlet (`handleAlarmLatch` L1021, `handleAlarmUnlatch` L1090, `handleAlarmAck` L1158, + others not enumerated this audit).

### §72.3.3 — Flow ack MX60 end-to-end

**Path único — AlarmsPage bulk** (NO replica asymmetry Reflow):
```
AlarmsPage planta-tab + bulk checkboxes 
└→ AlarmsPage._handleBulkAck L549 → collect allUuids[] from selected source rows
   └→ MX60.Confirm.show {title:'Reconocer alarmas', tone:'danger', ...} L581
      └→ onConfirm: MX60.AlarmsManager.ackAlarms(allUuids, '', {onSuccess, onError}) L590
         └→ AlarmsManager._ackWithRetry L273 → POST /mx60/api/alarms/ack
            ├→ XHR retry 3x exponential backoff 100/200/400ms
            ├→ onSuccess: loadAll(null) PESSIMISTIC re-fetch L291
            └→ onError final: MX60.Toast.error L298
   └→ AlarmsPage L597-606 clear _selected + uncheck checkboxes + setTimeout 600ms _fetch (defensive re-render)
```

**Backend processing**:
```
BChiServlet.handleAlarmAck L1158
└→ extractJsonStringArray "uuids" L1170
└→ remoteUser = req.getRemoteUser() L1179 (fallback "unknown")
└→ ChiAlarmHelper.ackAlarms(uuids, remoteUser, this) L1188
   └→ if (!hasAckSupport) → stub failedCount=n L361-369
   └→ resolve BAlarmService via Sys.getService reflection L383-387
   └→ for each uuid: UUID validate regex L404 + BQL select L415 + ackStrategy switch L429-454
   └→ AckResult{ackedCount, failedCount, errors[]}
└→ resp 200 result.toJson() L1194
```

**Path adicional — AlarmModalActions Poptip** (single ack from card/row):
```
AlarmModalActions Poptip "AcknowledgeSingle" 
└→ _postAck([uuid], callback) AlarmModalActions.js:50 → XHR ad-hoc POST /api/alarms/ack
   ⚠️ DUPLICATED PATH: NO usa AlarmsManager.ackAlarms (no retry, no AlarmsManager refresh)
```

**Live update parallel**: AlarmsManager 5s polling badge `_pollTimer` (init L80) + AlarmsPage REFRESH_INTERVAL=20s.

### §72.3.4 — Capabilities NO presentes en MX60 (gaps vs Reflow)

| Gap | Reflow ref | MX60 status |
|---|---|---|
| Priority management DUAL MODE | Vuex `classPriorities[]` + `rangePriorities {low,high}` + AlarmPriorityType class\|number + AlarmPriorityPicker | ❌ NO existe — alarms tienen `priority` pero NO custom mapping ni labels per console |
| Sound notifications 11 MP3 | `checkAlarmSounds` + `playAlarmSound` + `startAlarmSounds` + 11 MP3 ~448KB + AlarmSoundsForm + AlarmSoundsPicker | ❌ NO existe |
| Multi-console configuration | Vuex `consoles[]` JSON Patch persisted (defaultConsole L5-69 ~70 props) + AlarmConsoleForm + AlarmConsoleList | ❌ NO existe — single page hardcoded |
| Alarm class management UI | `AlarmClassList.vue` + `BReflowAlarmCommands.getClasses` BOX | ❌ NO existe — alarm classes Niagara existen pero sin UI dedicada |
| `acknowledgmentRequiresNote` gate | Frontend gate L67 AlarmAckConfirm + RequiredNoteModal (per #70 §70.2.5 frontend-only no enforcement backend) | ❌ NO existe — note opcional |
| CSV export server-side | `streamAlarmsCSV` + `streamSourcesCSV` AlarmData L162,L212 + REST handlers AlarmCSVResponse | ❌ MX60 hace **client-side** (AlarmsPage._handleCsvExport L616 + util.CsvExport.download) — limita por MAX_ALARMS=200 client cache |
| AlarmAckConfirm dedicated modal "Are you sure" | AlarmAckConfirm.vue 92L + dedicated `<RequiredNoteModal>` slot | ❌ MX60 usa `MX60.Confirm` genérico — funcional pero menos polished + no dedicated note flow para required-note |
| 2-paths asymmetric flow (#70) | AlarmsHome bulk con confirm vs AlarmDetails row sin confirm | ❌ MX60 1-path con confirm |
| Source-grouped bulk ack via expand | SourceGroupsTable 4 call sites `getUuidForSources(start,end,sources)` L350,360,376,386 expand source→uuids | ⚠️ MX60 hace bulk SOLO en sources directamente seleccionadas — sin expansión backend explícita |
| Hyperlink config per priority | `extHyperlinkEnabled` console flag | ⚠️ MX60 tiene endpoint `/api/alarms/hyperlink` + AlarmModalActions Hyperlink button — pero NO config per console |
| Inline notes count badge | `noteCount` campo en AlarmRecord (AlarmData L281) | ⚠️ MX60 nota count NO surfaced en alarm record (notes en archivo separado, no en BAlarmDatabase) |

### §72.3.5 — Capabilities EXCLUSIVAS MX60 (gap inverso, NO presente en Reflow)

| Capability MX60 | Archivo:Linea | Justificación domain-specific |
|---|---|---|
| Manual latch trigger ("alarmar manualmente") | UpDetail.js:2636 `_confirmManualLatch` | Operador puede forzar trip → apaga device via `ALARM_LOCKDOWN[thresholdKey]` (ej. fan trips → also stops compresor1+compresor2). Reflow NO tiene equipment lockdown. |
| Reset latch desde equipment detail | UpDetail.js:2663 `_confirmReset` + `_confirmResetAll` | Resetear sin ir a página alarms separate. Warning explícito "si la lectura sigue violando, volverá a alarmarse en el siguiente ciclo. La nota se perderá" L2672. |
| LATCH_TO_PROT_SLOT mirror | BChiUp.java:1549-1562 + syncProtectionSlots() L1904 | 8 slots `protXActive` typed BStatusBoolean wirables en Workbench → wire-to-output channels en grupo de actions. Refleja el latch state en typed slot, NO solo en JSON property. |
| `alarmLatches` como Niagara property | BChiUp.java:340-345 (annotation) + L1471-1495 (slots) | Persisted across station restart + ORD-exposed para Wire/REST/Fox. Reflow alarm state vive en BAlarmDb (no exposed como slot). |
| Reflection probe A-007 | ChiAlarmHelper.java:60-330 | Compat iSMA 4.13.2 (canonical method `ackAlarm` no `acknowledge`) vs N4.14. 4 strategies cached at class-load. Reflow asume N4 native API disponible — fallaría en iSMA 4.13.2 sin probe. |
| Hand-rolled brace-depth JSON parser | BChiUp.java:1748-1793 (purge) + ChiAlarmHelper._parseLatchMap | NO external lib dependency. Reflow usa `com.tridium.json.JSONObject` directo. |
| Notes persistence en JSON file station home | ChiAlarmHelper.java:494-572 + `^chihuahua-alarm-notes.json` con atomic move | Workaround porque iSMA 4.13.2 NO tiene native BAlarmDatabase note API. Reflow sí (notes en `alarm.getAlarmData().gets("notes", "")` per AlarmData L276). |
| ALARM_LOCKDOWN device chains | UpDetail.js (mentioned L2641) | Map thresholdKey → array of devices to power-off when latched. Equipment-aware safety semantics. |

---

## §72.4 — Triple comparison por sub-tema (Stage 3)

10 sub-temas críticos comparados Reflow vs MX60 vs bloques guidance.

### Sub-tema 1: Acknowledge as concept

| Sub-tema | Reflow approach | MX60 approach | Bloques guidance | Tensión |
|---|---|---|---|---|
| ACK as concept | `$niagara.alarm.ackAlarmsByUuid(uuids)` BajaScript NATIVE → BAlarmRecord.acknowledge() canónico (#70 §70.2.3, #62 KEEP-5) | Sistema 1 (latch): trip/reset binario, NO ack-state. Sistema 2 (Niagara): `ChiAlarmHelper.ackAlarms` reflection probe + REST POST `/api/alarms/ack` (4 strategies fallback iSMA 4.13.2 compat) | #44 BAlarmRecord.acknowledge() canónico — JAMÁS reimplementar | ⚠️ MX60 stack DIFFERS: iSMA 4.13.2 NO expone BAlarmDb directamente al frontend (no BajaScript subscriber-style ack), requiere REST + reflection. Reflow asume N4.14. |

### Sub-tema 2: Backend ack mechanism

| Sub-tema | Reflow | MX60 | Bloques | Tensión |
|---|---|---|---|---|
| Backend ack | NO Java code — delegated 100% BajaScript native (#70 §70.2.3 / #1242 implications #258) | `ChiAlarmHelper.ackAlarms` 130L Java (UUID validation + BQL select + 4 strategies switch + AckResult DTO) | #44 native API canónico + #54 `canAcknowledgeAlarms` BOX dead-code en Reflow | ⚠️ Tradeoff: MX60 más Java code → más testable WSL pure-unit + more compat layers, pero MORE attack surface (2041 LOC ChiAlarmHelper). Reflow más thin pero N4.14-only. |

### Sub-tema 3: Bulk ack multi-source

| Sub-tema | Reflow | MX60 | Bloques | Tensión |
|---|---|---|---|---|
| Bulk ack | SourceGroupsTable expand source→uuids via `getUuidForSources(start,end,sources)` L350,360,376,386 + AlarmAckConfirm.vue bulk modal | AlarmsPage._handleBulkAck L549 collect from selected rows + MX60.Confirm + AlarmsManager.ackAlarms | #62 KEEP UUID canonicalization universal (#175) | ⚠️ Reflow expands server-side source→uuids antes de bulk ack (correcto cuando alarms grow >MAX_QUERY_LIMIT). MX60 bulk solo de uuids ya cargados client-side (cap MAX_ALARMS=200 — bulk fail si > 200 alarms en source). |

### Sub-tema 4: Notes obligatorias / opcionales

| Sub-tema | Reflow | MX60 | Bloques | Tensión |
|---|---|---|---|---|
| Notes flow | `acknowledgmentRequiresNote` console flag (frontend gate per #70 §70.2.5) → RequiredNoteModal antes de ack | NO existe gate. Notes opcionales via AlarmModalActions "Notes" → AlarmNotesModal POST /api/alarms/notes (separado de ack) | #70 implication #260 100% frontend gate sin enforcement backend | ⚠️ Reflow gate frontend-only es bug-as-feature (operador puede saltarse gate via REST direct). MX60 NO tiene gate — operador puede ack sin nota. Para auditoría regulada (industrial sites), MX60 necesita gate. |

### Sub-tema 5: Priority management

| Sub-tema | Reflow | MX60 | Bloques | Tensión |
|---|---|---|---|---|
| Priority | DUAL MODE: `classPriorities[]` (mapeo per sourceClass) + `rangePriorities {low:100, high:200}` (en [0,255]). AlarmPriorityType `class\|number` selector. Custom labels (long/short) + colors per priority. AlarmPrioritiesForm + PriorityColorsForm + AlarmPriorityPicker (#62 implications #166 KEEP DUAL) | Backend emite `priority: "high|med|low"` (AlarmsManager getCount byType up/carcamo/datalogger). NO custom mapping ni labels per console | #62 #166 KEEP DUAL priority mapping universal | ⚠️ Para multi-tenant con custom industries, DUAL MODE Reflow es feature deal-breaker. Para single-tenant industrial Honeywell MX60, hardcoded high/med/low es suficiente. |

### Sub-tema 6: Filter / search UI

| Sub-tema | Reflow | MX60 | Bloques | Tensión |
|---|---|---|---|---|
| Filter | priority + status (active/ackState) + timeRange + source + class. AlarmStatusPicker + AlarmPriorityPicker + TimeRangePicker. Server-side filter QueryFilter + 5 testFn (testActive/testAckState/testByClass/testBySource/testByThresholds) | planta tabs (1-6 + Oficinas) + state tabs + search + timeRange (today/yesterday/thisMonth via _todayWindow/_yesterdayWindow). Server-side filter via ChiAlarmQueryHelper | #62 KEEP filter set | ✅ Equivalent en capability — lenguaje distinto (Reflow más generic, MX60 más equipment-specific con planta concept). |

### Sub-tema 7: CSV export server-side

| Sub-tema | Reflow | MX60 | Bloques | Tensión |
|---|---|---|---|---|
| CSV | Server-side streaming `streamAlarmsCSV` + `streamSourcesCSV` AlarmData.java:162,212 — sin cap memoria explícito (limita por BQL `select top MAX_QUERY_LIMIT=1000`) | Client-side `MX60.util.CsvExport.download(filtered, cols, filename)` AlarmsPage:616-637 — capa MAX_ALARMS=200 | #62 AP-75 CSV close sin flush MEDIUM + #167 KEEP CSV export limit | ⚠️ MX60 limit 200 inadequate para industrial site con histórico mensual (1000+ alarms/month por site). Server-side streaming Reflow correcto. CSV server-side es SDD pending P1. |

### Sub-tema 8: Sound notifications

| Sub-tema | Reflow | MX60 | Bloques | Tensión |
|---|---|---|---|---|
| Sounds | 11 MP3 ~448KB lib + checkAlarmSounds + playAlarmSound + startAlarmSounds + stopAlarmSounds + AlarmSoundsForm + AlarmSoundsPicker. Autoplay con `soundsEnabled` console flag. Browser autoplay-policy graceful fallback (#62 AP-72 MEDIUM) | NO existe | #62 AP-72 + Regla 22 sound playback try-catch | ⚠️ Industrial site control room: sounds críticos para attention-grab. Pero browser autoplay-policy + multi-user jitter (#62 AP-72) son problemas reales. P2. |

### Sub-tema 9: Multi-console configuration

| Sub-tema | Reflow | MX60 | Bloques | Tensión |
|---|---|---|---|---|
| Multi-console | Vuex `consoles[]` JSON Patch persisted (defaultConsole 70+ props). Per-console: refreshRate / priorityType / classPriorities / rangePriorities / styles / time defaults / column visibility / page title. AlarmConsoleForm + AlarmConsoleList | NO existe — single page hardcoded `MX60.AlarmsPage` | #62 #169 KEEP multi-console for multi-tenant | ⚠️ MX60 single-tenant Honeywell — multi-console N/A en short-term. P3. |

### Sub-tema 10: Live-update mechanism

| Sub-tema | Reflow | MX60 | Bloques | Tensión |
|---|---|---|---|---|
| Live update | AlarmsHome.vue:359 setInterval(consoleRefreshRate=20s) + AlarmDisplay.vue:168 navbar polling (#244 #1237). NO WebSocket (#62 AP-73 MEDIUM gap principal + decisión arquitectónica #5 P0 obligatorio sin implementar) | AlarmsPage REFRESH_INTERVAL=20000 L19 + AlarmsManager 5s polling badge `_pollTimer` (Fix H1 reduced 20s→5s para badge live) + EquipmentData seedFromEquipment latch refresh on each fetch (separate cadence) | #62 #161 WebSocket push P0 obligatorio + #69 #248 BReflowChannelService genérico NO emite alarmas + #69 #249 bajaHeartbeat.start sprint-1 obligatorio | ⚠️ Ambos sistemas son polling-based. WS push #5 sigue gap GLOBAL no resuelto en ningún sistema. MX60 5s badge mejor que Reflow 20s para "live feel". |

---

## §72.5 — Recommendations integración a MX60 (Stage 4)

Por sub-tema, recommendation con evidencia 3-source. Priorizado TIER 1/2/3.

### TIER 1 — Must-have sprint-1 (cierre gaps P0/P1 críticos)

**T1.1 — Ack flow paridad: ya existe, NO requiere transplant.**
- Evidencia: ChiAlarmHelper.ackAlarms L353 + BChiServlet handleAlarmAck L1158 + AlarmsManager.ackAlarms L261 + AlarmsPage._handleBulkAck L549 — todos audited empíricamente vivos.
- Justificación: bloque #70 §70.2.3 ack canónico vía BajaScript native = N/A en iSMA 4.13.2 (probe A-007 confirma). MX60 reflection-based approach es **estructuralmente correcto** dado constraint stack (engram #1257).
- Acción sprint-1: **ninguna** — keep as-is + SDD pending mx60-alarms-latch-mode-change debe NO tocar este flow.

**T1.2 — Source-grouped bulk ack expand backend (FIX limitation MAX_ALARMS=200).**
- Evidencia: Reflow SourceGroupsTable.vue 4 call sites `getUuidForSources(start,end,sources)` L350-386 → backend AlarmData.getUuidsForSource L238 (uses BAlarmDb timeQuery sin limit). MX60 actual collect-from-cache fail si source >200 alarms.
- Justificación: industrial sites pueden tener >200 alarms/source en históricos largos. Bug latente when bulk-ack usado en time-range largo.
- Acción sprint-1: agregar endpoint MX60 `GET /api/alarms/uuids-by-source?start=&end=&sources=` que llama `ChiAlarmHelper.queryAlarmsBySource` con limit=null → return uuid array. Conectar AlarmsPage._handleBulkAck para expand server-side antes de ack.

**T1.3 — AlarmModalActions._postAck duplicated path → consolidar via AlarmsManager.**
- Evidencia: `AlarmModalActions.js:50 _postAck` ad-hoc XHR sin retry sin loadAll-on-success. Duplicates `AlarmsManager.ackAlarms` L261.
- Justificación: dos paths divergentes pueden divergir por bug. AlarmsManager tiene retry + refresh; postAck no.
- Acción sprint-1: AlarmModalActions.AcknowledgeSingle/All → llamar `MX60.AlarmsManager.ackAlarms([uuid], '', opts)` en lugar de _postAck.

**T1.4 — `acknowledgmentRequiresNote` gate frontend (P1 si site requiere auditoría).**
- Evidencia: Reflow AlarmAckConfirm.vue:67 + RequiredNoteModal + #70 §70.2.5 (impl #260 100% frontend gate). MX60 NO tiene.
- Justificación: industrial sites con normativa requieren note antes de ack (audit trail). Honeywell sites probablemente requieren ISO 9001.
- Acción sprint-1 (CONDICIONAL si producto requiere): agregar config flag `MX60.ConfigManager` `alarms.acknowledgmentRequiresNote` + pre-ack flow MX60.AlarmsPage check flag → si true, abrir AlarmNotesModal en mode-required antes de Confirm.

### TIER 2 — Nice-to-have sprint-2

**T2.1 — CSV export server-side streaming.**
- Evidencia: Reflow `streamAlarmsCSV` AlarmData.java:162 + `streamSourcesCSV` L212. MX60 client-side cap 200.
- Justificación: histórico mensual industrial puede tener 1000+ alarms (excede cap). Server-side streaming es requirement para large datasets.
- Acción sprint-2: agregar `ChiAlarmHelper.streamAlarmsCSV(filter, OutputStream)` + handler `BChiServlet.handleAlarmsCsvExport` GET `/api/alarms/csv?range=&...` con Content-Disposition: attachment + thread + Niagara `OutputStream.flush()` defensive (corregir Reflow AP-75).

**T2.2 — Notes count badge en alarm record.**
- Evidencia: Reflow AlarmData L276-281 `noteCount = StringUtils.countOccurrences(notes, '\n') / 3`. MX60 notes en archivo separado, count NO surface en alarm record.
- Justificación: UX — badge "(3 notas)" en grid antes de abrir AlarmNotesModal.
- Acción sprint-2: ChiAlarmHelper notes API agregar `getNoteCount(uuid)` + incluir en queryAlarms response per-row.

**T2.3 — Source-grouping en AlarmsPage (Reflow SourceGroupsTable parity).**
- Evidencia: Reflow SourceGroupsTable.vue 656 LOC con bulk @ack-all + ack-recent + grouping per source. MX60 AlarmsPage actualmente flat grid.
- Justificación: alarmas por source ayuda operador a actuar (ej. todas las alarms de UP-3 ackear de una vez).
- Acción sprint-2: nueva vista `MX60.AlarmsByPlantaView` + grouping client-side por planta+up.

### TIER 3 — Defer (P3, prio baja)

**T3.1 — Priority management DUAL MODE (class-based + range-based).**
- Justificación defer: MX60 single-tenant Honeywell, hardcoded high/med/low suficiente. Si Honeywell adopta multi-tenant en V2, agregar.

**T3.2 — Sound notifications 11 MP3.**
- Justificación defer: industrial control room likely tiene physical alarm sirens dedicados. Browser sound es nice-to-have, NO crítico. Post engram #62 AP-72 (autoplay graceful fallback) requiere cuidado.

**T3.3 — Multi-console configuration.**
- Justificación defer: single-tenant. N/A short-term.

**T3.4 — Alarm class management UI.**
- Justificación defer: MX60 ya tiene equipment-class concept (UP/Carcamo/Datalogger en getCount.byType) + planta concept. Class management Niagara-native UI = redundante para single-site.

**T3.5 — WebSocket push live update (decisión arquitectónica #5 P0 sigue gap GLOBAL).**
- Justificación defer (paradoja): #5 marca P0 obligatorio pero NO se ha implementado en NINGÚN sistema. Si el polling 20s + 5s badge es "good enough" para sites Honeywell actuales, mantener polling. Si NO, requiere extender BReflowChannelService o implementar BIAlarmCursor (#44) — scope grande, separate change.

---

## §72.6 — Decisiones MX60 sprint-1 alarms (qué heredar concreto)

### Decisiones HEREDAR de Reflow → MX60 sprint-1

1. ✅ **Source-grouped bulk ack expand backend** (T1.2 — endpoint `/api/alarms/uuids-by-source`)
2. ✅ **Consolidar AlarmModalActions duplicated path → AlarmsManager.ackAlarms** (T1.3 — fix bug latente)
3. ⚠️ **`acknowledgmentRequiresNote` gate** (T1.4 — CONDICIONAL si producto requiere auditoría regulada Honeywell)

### Decisiones DESCARTAR de Reflow

1. ❌ Sound notifications 11 MP3 (TIER-3 defer)
2. ❌ Multi-console configuration (TIER-3 defer)
3. ❌ Priority DUAL MODE (TIER-3 defer)
4. ❌ Alarm class management UI (TIER-3 defer)
5. ❌ AlarmAckConfirm "Are you sure" dedicated modal (MX60 ya usa MX60.Confirm genérico — funcional)
6. ❌ 2-paths asymmetric flow (#70) — MX60 1-path simplificado es PREFERABLE en realidad (#70 §70.2.7 prescribió "preservar literal" pero esa prescripción tiene sentido SOLO para Reflow → migration). MX60 nuevo desarrollo: 1-path consistent es mejor UX.

### Decisiones PRESERVAR de MX60 actual

1. ✅ **Sistema 1 latch (BChiUp.alarmLatches + 8 protXActive + LATCH_TO_PROT_SLOT + UpDetail.js latch UI)** — patrón único MX60 critical para industrial protection control. JAMÁS reemplazar con Reflow style.
2. ✅ **Reflection probe A-007** ChiAlarmHelper class-load (compat iSMA 4.13.2 vs N4.14) — sin esto, ack falla en iSMA stations.
3. ✅ **AlarmsManager.ackAlarms 3x retry exponential backoff** (Fix D-001/D-002 — más robusto que Reflow no-retry pattern).
4. ✅ **Hand-rolled brace-depth JSON parser** — no external lib dependency. Útil porque iSMA puede no tener `com.tridium.json` consistent.
5. ✅ **Notes persistence en `^chihuahua-alarm-notes.json` con atomic move** — workaround correcto para iSMA 4.13.2 sin native BAlarmDatabase note API.

### Decisiones INVENTAR sprint-1 (si aplica T1.4 audit-required)

1. 🆕 `MX60.ConfigManager.alarms.acknowledgmentRequiresNote` config flag + pre-ack flow check.

### Bugs latentes audited este bloque

1. 🐛 **AlarmModalActions._postAck duplicates AlarmsManager.ackAlarms path** — fix obligatorio T1.3.
2. 🐛 **AlarmsPage._handleBulkAck cap MAX_ALARMS=200 puede fail bulk source-large** — fix obligatorio T1.2 (endpoint expand).
3. 🐛 **`acknowledgmentRequiresNote` gate ausente** — fix opcional T1.4 según audit requirement.
4. 🐛 **CSV export client-side cap 200** — fix sprint-2 T2.1.
5. 🐛 **AWAITING SLOTOMATIC REGEN** L284 BChiUp.java — flag heredado de #71 (C1 bug #268), aplica también a este bloque (sigue en código).

---

## §72.7 — DECISIÓN PARA EL USUARIO sobre scope SDD pending

**Contexto**: SDD pending `mx60-alarms-latch-mode-change` originalmente focused en latch-mode + reset. Este bloque audita gaps reales encontrados.

### Resumen 1-página de gaps encontrados

**Gap principal del prompt original (REFUTADO empíricamente)**: el prompt asumía "MX60 no tiene ack" → FALSO. ChiAlarmHelper.ackAlarms L353 + BChiServlet POST /api/alarms/ack L1158 + AlarmsManager.ackAlarms + AlarmsPage bulk flow YA EXISTEN y son funcionalmente completos.

**Gaps reales identificados** (en orden criticidad):
1. **CRÍTICO bug latente**: AlarmModalActions._postAck duplicated path vs AlarmsManager.ackAlarms (single-line fix sprint-1)
2. **CRÍTICO bug latente**: AlarmsPage bulk ack fail si source >MAX_ALARMS=200 client cache (necesita endpoint expand server-side)
3. **MEDIO**: acknowledgmentRequiresNote gate ausente (CONDICIONAL si Honeywell requiere auditoría regulada)
4. **MEDIO**: Notes count badge en alarm record (UX)
5. **BAJO**: CSV export server-side streaming (cap 200 client → P1)
6. **BAJO**: Source-grouping AlarmsByPlantaView (UX nice-to-have)
7. **DEFER**: Priority DUAL MODE / sounds / multi-console / class-mgmt-UI / WebSocket push (TIER-3)

### 3 Escenarios SDD scope

**(a) Mantener SDD focused en latch-mode + reset**
- Scope: solo Sistema 1 (BChiUp.alarmLatches latch behavior + UpDetail.js reset semantics).
- Cuándo aplica: gaps Sistema 2 (bugs T1.2/T1.3) son fix-able en chores inline POST-SDD sin necesidad de spec/design separados. Bug T1.2 endpoint expand es TIER-1 pero scope chico (1 endpoint + 1 frontend hookup ~80 LOC).
- Sprint-1 followups: 2 chores inline (consolidate _postAck + uuids-by-source endpoint) DESPUÉS del SDD.
- **Pro**: SDD tight, ship-able rápido. Gaps T1.2/T1.3 son chores small.
- **Con**: si SDD verifica empíricamente todo el sistema 1, no aprovecha hallazgos sistema 2 audit.

**(b) Expandir SDD a latch-mode + reset + ack-as-concept**  
- Scope: Sistema 1 + ack flow Sistema 2 verification + bugs T1.2/T1.3 fix (incluidos en SDD).
- Cuándo aplica: el SDD vale 1 sola pasada porque Sistema 1 reset y Sistema 2 ack están conceptualmente relacionados (ambos "limpian" estado de alarma desde UI distintas) — vale documentar contract claro entre los dos.
- **Pro**: 1 pasada cubre ack-flow paridad + latch-reset semantics + 2 bugs latentes.
- **Con**: SDD scope crece ~50%. Verify report más largo.

**(c) Expandir SDD a latch-mode + reset + ack + bulk + filter**
- Scope: Sistema 1 + Sistema 2 ack + T1.2 bulk endpoint + T1.4 acknowledgmentRequiresNote gate (si Honeywell regulado) + T2.1 CSV server-side + T2.3 source-grouping view.
- Cuándo aplica: si el equipo decide "consolidate alarms domain in 1 SDD para no fragmentar".
- **Pro**: bundling correcto si todos los items son de mismo dominio.
- **Con**: SDD scope crece ~150%. Verify largo. Riesgo de scope creep + tasks list grande con dependencias internas.

### Recomendación con justificación técnica

**RECOMIENDO ESCENARIO (b) — "latch-mode + reset + ack-as-concept"**.

**Por qué (b) y no (a)**:
- T1.2 (uuids-by-source endpoint expand) y T1.3 (consolidate _postAck) son **bugs latentes silenciosos** — fácil que se olviden si quedan como chores POST-SDD. Mejor cubrirlos en spec del SDD.
- El SDD pending YA toca el latch system (Sistema 1). Documentar el contract con Sistema 2 (cuándo el operador resetea latch UpDetail.js vs cuándo hace ack en AlarmsPage) es **decisión arquitectónica** que vale formalizar — sino, el comportamiento es ambiguo para futuro audit.

**Por qué (b) y no (c)**:
- T2.1 CSV server-side y T2.3 source-grouping son **features**, NO bugs. SDD debe cerrar bugs/contracts antes que features. Features grandes pueden ser SDD separado (ej. `mx60-alarms-csv-server-side` + `mx60-alarms-by-planta-view`).
- T1.4 `acknowledgmentRequiresNote` gate es **CONDICIONAL** según requerimiento Honeywell. Si requirement existe, SDD separado dedicado. Si NO, no incluir en este SDD.

**Concreto: scope sugerido SDD (b)**:
- §spec REQ-latch-001..005 (lo ya planeado en `mx60-alarms-latch-mode-change`)
- §spec REQ-ack-001 — paridad funcional ack flow validar empíricamente (test integration: ack 1 alarm → BAlarmDb state acked + AlarmsPage refresh shows acked). 
- §spec REQ-ack-002 — uuids-by-source endpoint server-side expand (T1.2 — fix bulk ack >200)
- §spec REQ-ack-003 — AlarmModalActions consolidate _postAck → AlarmsManager.ackAlarms (T1.3 — fix duplicated path)
- §spec REQ-contract-001 — documentar **diferencia conceptual** latch-reset vs alarm-ack en design.md (latch=protección equipment, ack=ack BAlarmDb state)

Esta recomendación NO es invariante — el usuario puede preferir (a) o (c) según prioridades de timeline. La justificación es solo técnica.

---

## §72.8 — Implications nuevas #285..#298

Tally previo (post bloque #71): 96 antipatterns + 42 reglas + 284 implications.

| # | Implication | Ref | Sprint priority |
|---|---|---|---|
| #285 | MX60 SÍ tiene ack flow funcional end-to-end (NO es gap como prompt sugería) — ChiAlarmHelper.ackAlarms L353 + BChiServlet POST /api/alarms/ack L1158 + AlarmsManager + AlarmsPage UI | §72.0(A) §72.3.2 | INFO |
| #286 | Reflection probe A-007 BAlarmService.ackAlarm 4-strategy fallback es PATRÓN OBLIGATORIO para iSMA 4.13.2 stations (canonical name `ackAlarm`, NO `acknowledge`) | ChiAlarmHelper.java:60-330 | sprint-1 KEEP |
| #287 | MX60 dual sistema: latch (Sistema 1 BChiUp.alarmLatches + 8 protXActive + UpDetail.js) vs Niagara-native ack (Sistema 2 BAlarmDb + ChiAlarmHelper). NO MEZCLAR conceptos | §72.0 §72.3.1 §72.3.2 | INFO arquitectónico |
| #288 | LATCH_TO_PROT_SLOT pattern es exclusivo MX60 (8 typed slots wirables Workbench) — Reflow NO tiene equivalente; NO aplicable a Reflow port | BChiUp.java:1549-1562 | KEEP MX60 |
| #289 | AlarmModalActions._postAck duplicates AlarmsManager.ackAlarms path — bug latente fix obligatorio T1.3 sprint-1 | AlarmModalActions.js:50 | sprint-1 FIX |
| #290 | AlarmsPage bulk ack fail si source >MAX_ALARMS=200 client cache — endpoint expand `/api/alarms/uuids-by-source` requerido T1.2 sprint-1 | AlarmsManager.js:35 + AlarmsPage.js:549 | sprint-1 FIX |
| #291 | acknowledgmentRequiresNote gate ausente MX60 — CONDICIONAL T1.4 según audit requirement Honeywell | §72.5 T1.4 | sprint-1 condicional |
| #292 | Notes en MX60 persistidas a `^chihuahua-alarm-notes.json` station home con atomic move + lazy bootstrap (workaround iSMA 4.13.2 sin native API) | ChiAlarmHelper.java:486-572 | KEEP MX60 |
| #293 | CSV export MX60 client-side cap 200 inadequate para industrial site — server-side streaming sprint-2 T2.1 | AlarmsPage.js:616 + AlarmsManager.js:35 | sprint-2 FIX |
| #294 | MX60 single-path ack flow (AlarmsPage bulk) PREFERABLE vs Reflow 2-path asymmetric (#70) para nuevo desarrollo single-tenant | §72.6 §72.4 sub-tema 9 | INFO |
| #295 | Reflow `getUuidForSources` 4 call sites SourceGroupsTable expand server-side correcto pattern para bulk over time-range | SourceGroupsTable.vue:350-386 + AlarmData.java:238 | sprint-1 IMPORT |
| #296 | MX60 5s polling badge AlarmsManager + 20s page refresh es paridad acceptable vs Reflow 20s — WS push #5 sigue gap GLOBAL no resuelto | AlarmsManager.js:80 + AlarmsPage.js:19 + #62 #161 | DEFER WS |
| #297 | TIER-3 defer alarms para MX60 single-tenant Honeywell: priority DUAL + sounds + multi-console + class-mgmt-UI — N/A short-term | §72.5 TIER-3 | DEFER |
| #298 | El prompt-driven framing puede asumir gaps que NO existen empíricamente — Stage 0 obligatorio + Stage 2 audit DIRECTO refuta o confirma. RE-CONFIRMA engram #1236 #1259 a NIVEL prompt-task — no solo bloques | §72.0 §72.7 + engram #1236 + #1259 | learning meta |

**Tally post-Bloque 72**: 96 antipatterns + 42 reglas template MX60 + **298 implications #1..#298** + Capa 19 EXTENDIDA con audit empírico TIER-1 alarms.

---

## §72.9 — Cross-refs

### Bloques referenciados

- **#44** Alarm Console pipeline Niagara nativo — confirmado vigente ChiAlarmHelper reflection probe usa BAlarmService.ackAlarm canónico per #44 KEEP
- **#54** BReflowAlarmCommands TIER-1 cerrado — `canAcknowledgeAlarms` BOX dead-code Reflow (impl #259) sigue presente, decisión sprint-1 backend Reflow pending
- **#62** Alarmas Reflow dedicated audit — todos los 78 antipatterns + 22 reglas + 175 implications aplicables. Decisión arquitectónica #5 (WS push P0) sigue gap GLOBAL ambos sistemas (#296)
- **#68 §68.1** Backend transplant blueprint — vigente; §68.4 ack flow preserved literal NO aplicable a MX60 stack iSMA 4.13.2
- **#69** Live-update polling 20s — confirmado ambos Reflow + MX60 polling-based (#244 #248 #249 vigentes)
- **#70** TIER-1 ack flow asymmetric Reflow — vigente Reflow; MX60 simplifica a 1-path (#294 nueva implication)
- **#71** Equipment audit + LATCH_TO_PROT_SLOT pattern — base estructural alarmLatches este bloque + AWAITING SLOTOMATIC REGEN (#268) sigue aplicable

### Engram methodology referenciado

- **engram #1129** (#62 mem) Reflow alarmas dedicated complete capabilities — used Stage 1 audit
- **engram #1236** mapping-vs-empirical-audit — RE-CONFIRMADO en este bloque (prompt asumía MX60 sin ack; empírico revela ack completo). Patrón ahora confirmed a NIVEL **prompt-task framing**, NO solo bloques (impl #298)
- **engram #1237** (#69 mem) live-update patrones empíricos — confirmed MX60 polling 20s + 5s badge
- **engram #1238** clean-room-disconnected-asymmetry — Reflow alarmCache.js stub L1 confirmed (61L)
- **engram #1242** (#70 mem) TIER-1 ack flow + 11 implications — extendido este bloque con MX60 actual implementation
- **engram #1257** mx60-stack-pivot IIFE no Vue — confirmed MX60 alarm UI 8 archivos IIFE classic ES5 strict
- **engram #1258** reflow-mx60 bilateral-critique framing — aplicado §72.4 + §72.5
- **engram #1259** triple-source methodology — aplicado integral este bloque
- **engram #1260** (#71 mem) equipment TIER-1 audit triple-source — formato referencia este bloque

### Pending engram saves (post-bloque)

- `bloque/72/alarms-tier1-triple-source` — discovery type — síntesis 3 veredictos + 14 implications + decisiones MX60 sprint-1 + recommendation §72.7 escenario (b)

---

## §72.10 — PARA EL YO 2027

Si en 2027 estás auditando el dominio alarms de un nuevo módulo Niagara y querés saber qué aprendimos en 2026:

1. **El framing del prompt puede tener errores empíricos**. Este bloque empezó con prompt asumiendo "MX60 no tiene ack". El audit reveló que SÍ tiene ack flow completo. **Stage 0 obligatorio + Stage 2 audit DIRECTO antes de implementar/proponer gaps**.

2. **iSMA 4.13.2 ≠ N4.14**. El canonical method es `ackAlarm` (sin `acknowledge`). Reflection probe A-007 con 4 strategies fallback es OBLIGATORIO si tu deployment puede tocar iSMA. Sin probe, ack falla silente.

3. **Latch system y ack system son CONCEPTOS DISTINTOS**. Latch = protección equipment-level (BChiUp.alarmLatches + 8 typed slots). Ack = state transition en BAlarmDb. NO mezclar. UI debería tener ambos: reset desde equipment detail (latch) + ack desde alarms page (BAlarmDb).

4. **JSON property persistence + atomic move es workaround válido cuando native API no existe**. ChiAlarmHelper notes en `^chihuahua-alarm-notes.json` con BFileSystem.move() es POSIX rename atomic. Pattern reusable para cualquier dato estructurado que necesite persistir sin native Niagara API.

5. **Patrón triple-source methodology funciona**. Reflow=production reference (qué se hizo) + MX60=ground truth actual (qué se decidió) + bloques=research guidance (qué se debe hacer). Recommendation final NO es bias hacia ninguno — es síntesis con justificación técnica de los 3 lados.

6. **Hand-rolled JSON parser brace-depth scanning es válido en niagara modules**. NO siempre `com.tridium.json.JSONObject` está disponible. BChiUp.purgeAlarmLatches L1748-1793 + ChiAlarmHelper._parseLatchMap son references.

7. **Bug latente más común: paths duplicados frontend ad-hoc XHR vs centralized manager.** AlarmModalActions._postAck duplica AlarmsManager.ackAlarms sin retry sin refresh. Always centralize via Manager namespace.

8. **CSV export server-side es better-than client-side para datasets industriales**. Cap 200 client (MX60) inadecuado para histórico mensual. Reflow `streamAlarmsCSV` AlarmData.java:162 reference impl. Pero AP-75 close-without-flush bug (#62) — fix obligatorio defensive.

9. **Polling 20s + 5s badge es acceptable. WebSocket push sigue gap GLOBAL**. Decisión arquitectónica #5 (#62 P0 obligatorio) NO se ha implementado en NINGÚN sistema todavía (2026-05-10). Si el operator tolerance es ~5s para "live feel", polling-only es ship-able.

10. **El SDD scope decision (a/b/c §72.7) es siempre tradeoff TIME vs COVERAGE**. (a) ship rápido + chores POST. (b) bundling correcto bug+contract documentation. (c) feature creep risk. Default "(b)" salvo que time pressure dicte (a).

---

**FIN BLOQUE 72** · 2026-05-10 · Capa 19 · audit triple-source dominio alarms cerrado
