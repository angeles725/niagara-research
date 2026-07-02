# Block 166 — chihuahua MX60 (`-ux/-rt`): subsistema de alarmas (BQL query, latch/unlatch, notes, ackAll, source grouping)

> **WHAT** — Subsistema de alarmas del módulo **chihuahua** MX60: cómo se consultan las alarmas contra `BAlarmDatabase` vía BQL (rango, ackState, paginación), la forma del *alarm-summary* (badge), el modelo latch/unlatch anclado en el slot `alarmLatches`, las notas por UUID, `ackAll` sobre toda la BD, el agrupamiento por fuente para la página de alarmas, y cómo el frontend consume todo. Se marca dónde el modelo es **baja-native** (`ackAlarms` vía `alarm:` ord + `@NiagaraAction resetAlarmas`) vs **servlet-mediated** (el resto).
>
> **Foco:** chihuahua. **Idioma:** Español.
> **Sources (aliases):**
> - `UX=` `chihuahua-ux/src/com/angeles/chihuahua/ux/`
> - `RT=` `chihuahua-rt/src/com/angeles/chihuahua/`
> - Archivos leídos: `UX=ChiAlarmHelper.java`, `UX=ChiAlarmQueryHelper.java`, `UX=BChiServlet.java`, `UX=rc/js/app/AlarmsManager.js`, `RT=components/BChiUp.java`.
> **Marcadores:** `[CERT]` = verificado con `file:line`; `[INFER]` = deducción razonada no atada a una línea.
> `.env.local` **NO** leído.
> **Capa 26.** Continúa [Block 163] / [Block 164].

---

## 166.1 — Panorama: dos serializadores BQL + un modelo de latch en slot

El subsistema de alarmas de chihuahua tiene **dos helpers BQL independientes** en la capa UX, ambos consultando `station:|alarm:` (el `BAlarmDatabase` de la estación):

| Helper | Responsabilidad | Entry points |
|--------|-----------------|--------------|
| `ChiAlarmHelper` | Consola de alarmas (lista paginada), *summary* del badge, ackAll, historia por fuente, latch/unlatch, notas | `queryAlarms`, `getAlarmSummary`, `ackAllUnacked`, `queryAlarmsBySource`, `latchAlarm`, `unlatchAlarm`, `addNote` |
| `ChiAlarmQueryHelper` | Página de alarmas estilo Reflow: agrupa `BAlarmRecord` por fuente y anota `source_type` (`"alarm"`/`"latch"`) | `queryAlarmSources` |

Ambos son clases `final` con constructor privado (utilidades stateless) `[CERT] UX=ChiAlarmHelper.java:50,56` / `[CERT] UX=ChiAlarmQueryHelper.java:32,39`.

El estado de latch **no vive en `BAlarmDatabase`**: vive en el slot `alarmLatches` (un `baja:String` JSON) de cada `BChiUp` `[CERT] RT=components/BChiUp.java:335-340,1459`. Las notas tampoco son nativas de Niagara: viven en un `ConcurrentHashMap` persistido a disco (§166.5).

---

## 166.2 — Consulta de alarmas: BQL, rango, ackState, paginación

### Construcción del BQL (`buildAlarmBql`)
`queryAlarms(range, ackState, page, out, context)` calcula `stop = now`, `start = stop − rangeToMillis(range)`, arma el BQL y ejecuta `(BITable) BOrd.make(bql).get(context, null)` sobre un `TableCursor` `[CERT] UX=ChiAlarmHelper.java:872-909`.

BQL generado `[CERT] UX=ChiAlarmHelper.java:1032-1054`:
```
station:|alarm:|bql:select * where timestamp.millis >= <start>
  and timestamp.millis <= <stop>
  [and ackState != 'acked'   // si ackState == "unacked"]
  [and ackState = 'acked'    // si ackState == "acked"]
  order by timestamp desc
```

- **ackState `"unacked"`** → filtra `ackState != 'acked'`, no `= 'unacked'`. Esto es deliberado: `ackPending` es un estado transitorio (sub-segundo); `!= 'acked'` cubre tanto `unacked` como `ackPending` (la condición persistente de "necesita ack") `[CERT] UX=ChiAlarmHelper.java:1040-1046`.
- **ackState `"acked"`** → `ackState = 'acked'` `[CERT] UX=ChiAlarmHelper.java:1047-1050`.
- Cualquier otro valor → sin filtro de ack (todas).

### Rango de tiempo (`rangeToMillis` / `rangeToWindow`)
`rangeToMillis` mapea 15 valores del picker (más aliases legacy `1h/8h/24h/7d` y camelCase `last8Hours`…) a un offset en ms; default 24h con WARNING para desconocidos `[CERT] UX=ChiAlarmHelper.java:1296-1341`. Los valores calendario (`today`/`yesterday`/`thisMonth`) NO los resuelve `rangeToMillis` (loguea WARN y cae a 24h); los resuelve `rangeToWindow`, que devuelve `long[2]{from,to}` usando `Calendar` en la TZ de la estación (`TimeZone.getDefault()`) `[CERT] UX=ChiAlarmHelper.java:1332-1337,1356-1417`.

### Paginación — **contador server-side, NO `BQL LIMIT`**
La paginación se hace recorriendo el cursor y saltando registros con un contador, **no** con `limit` en BQL (frágil entre versiones iSMA) `[CERT] UX=ChiAlarmHelper.java:884-902`:
- `skip = page * DEFAULT_PAGE_SIZE`, `end = skip + DEFAULT_PAGE_SIZE`.
- `DEFAULT_PAGE_SIZE = 200` (coincide con `config.alarms.maxStored`) `[CERT] UX=ChiAlarmHelper.java:53`.
- Se emite un array JSON `[...]` con `writeAlarmData` por registro dentro de `[skip, end)`.

Cada registro se convierte `BAlarmRecord → AlarmData` vía `fromBAlarmRecord` (extrae uuid, source ord, tipo/planta/label derivados del ord, prioridad por bucket, triggerSlot, mensaje, value/threshold desde `BFacets`, timestamp ISO-UTC, `ackState`) `[CERT] UX=ChiAlarmHelper.java:1656-1759`. Buckets de prioridad: `0–127 high`, `128–191 med`, `192–255 low` `[CERT] UX=ChiAlarmHelper.java:1262-1267`.

**No hay ventana de dedup**: la consola no deduplica; el agrupamiento por fuente es responsabilidad exclusiva de `ChiAlarmQueryHelper` (§166.6). `[INFER]`

---

## 166.3 — Forma del alarm-summary (badge)

`getAlarmSummary(out, context)` es el endpoint del badge (`GET /api/alarms/summary`, reemplaza al viejo `/api/alarmCounts`) `[CERT] UX=ChiAlarmHelper.java:980-1022`.

### BQL del summary (`buildSummaryBql`) — **sin filtro de tiempo**
```
station:|alarm:|bql:select * where
  (sourceState = 'offnormal' or sourceState = 'fault') or ackState != 'acked'
```
`[CERT] UX=ChiAlarmHelper.java:1073-1078`. Se removió el antiguo filtro de timestamp de 15 min: los registros unacked persisten indefinidamente en el badge (SDD `mx60-ackall-full-db-and-badge-no-filter`) `[CERT] UX=ChiAlarmHelper.java:988-990`.

### Cap de materialización
El walk del cursor se corta en `SUMMARY_MAX_RECORDS = 500` `[CERT] UX=ChiAlarmHelper.java:977,1002` — defensa contra materializar toda la BD en memoria cada 30 s cuando hay miles de unacked históricos (tech-debt `summary-bql-no-record-cap`, Judgment Day #1786) `[CERT] UX=ChiAlarmHelper.java:969-977`. Para el badge basta "¿hay algo?", no un total exacto más allá del cap.

### Shape del JSON (`summaryFromAlarmDataList`)
`[CERT] UX=ChiAlarmHelper.java:1158-1203`:
```json
{
  "unackedSources": N,   // # de sourceId distintos con ackState=unacked (Set)
  "ackedSources":   N,   // # de sourceId distintos con ackState=acked (Set)
  "latchedSources": N,   // inyectado por tallyLatchedSources (NO del cursor)
  "byPriority": { "high": N, "med": N, "low": N },
  "byType":     { "up": N, "carcamo": N, "datalogger": N },
  "total": N             // = high+med+low  → cuenta REGISTROS, no fuentes
}
```
- `unackedSources`/`ackedSources` cuentan **fuentes distintas** (`HashSet<sourceId>`), mientras `total` cuenta **registros** de alarma (`high+med+low`) — la diferencia es load-bearing (D-Q1) `[CERT] UX=ChiAlarmHelper.java:1154,1176-1183,1200`.
- `latchedSources` **no** proviene del cursor de alarmas: lo aporta `tallyLatchedSources(context)`, que recorre el árbol de componentes contando `BChiUp` con `alarmLatches` no vacío (`!= null && != "" && != "{}"`) — un solo walk del árbol por llamada, nunca el cursor de alarmas `[CERT] UX=ChiAlarmHelper.java:923-948,1008`.

---

## 166.4 — Modelo latch/unlatch y su relación con el slot `alarmLatches`

### El slot de servicio `alarmLatches`
Cada `BChiUp` tiene la propiedad `alarmLatches`: `baja:String`, `Flags.SUMMARY`, default `"{}"` `[CERT] RT=components/BChiUp.java:335-341,1459`. Es un **mapa JSON plano** `{ "<thresholdKey>": { "latched":true, "latchedAt":<epochMs>, "latchedBy":"<user>", "note":"<text>" } }` `[CERT] RT=components/BChiUp.java:330,335`. Política multi-usuario: last-write-wins v1 (sin lock distribuido) `[CERT] RT=components/BChiUp.java:331`.

### `latchAlarm` — read-modify-write idempotente
`latchAlarm(up, thresholdKey, username, note)` `[CERT] UX=ChiAlarmHelper.java:68-101`:
1. Parsea el mapa actual (`_parseLatchMap`, scanner de profundidad de llaves sin librería JSON externa) `[CERT] UX=ChiAlarmHelper.java:450-512`.
2. **No-op si la clave ya existe** — preserva `latchedAt/latchedBy/note` originales `[CERT] UX=ChiAlarmHelper.java:80`.
3. Construye la entrada con `now = System.currentTimeMillis()`, escapa con `ChiJsonUtil.escapeJson`, y re-serializa con `_serializeLatchMap` `[CERT] UX=ChiAlarmHelper.java:82-92,517-532`.

### `unlatchAlarm` — remoción idempotente
Quita una sola clave; no-op si la clave está ausente `[CERT] UX=ChiAlarmHelper.java:108-126`.

### Purga
`purgeOldLatches(up, maxAgeMs)` elimina entradas con `latchedAt < now − maxAgeMs`; si `latchedAt` falta o es 0 la entrada **se conserva** (fail-safe: `latchedAt > 0` guard) `[CERT] UX=ChiAlarmHelper.java:160-217,540-561`. El disparador productivo es `BChiDashboardService.controlTick()`, que purga entradas > 30 días `[CERT] RT=components/BChiUp.java:334`.

---

## 166.5 — Notas por `{uuid}`: store no-nativo persistido a disco

`BAlarmDatabase` en iSMA 4.13.2 **no tiene API nativa de notas de texto** `[CERT] UX=ChiAlarmHelper.java:220-246`. chihuahua lo resuelve con:

- **Store en memoria:** `ConcurrentHashMap<String uuid, List<NoteEntry>>` (lecturas lock-free) `[CERT] UX=ChiAlarmHelper.java:253-255`. `NoteEntry = {message, author, date}` `[CERT] UX=ChiAlarmHelper.java:422-433`.
- **Persistencia:** archivo `^chihuahua-alarm-notes.json` en el station home `[CERT] UX=ChiAlarmHelper.java:249-250`. Carga **lazy** (primer `getNotesByUuid`/`addNote` dispara `ensureNotesLoaded`, idempotente) para no romper los unit tests WSL sin clases Niagara `[CERT] UX=ChiAlarmHelper.java:268-306,312,347`.
- **Escritura atómica:** escribe a `.json.tmp`, borra el final existente y hace `FileSpace.move()` (rename POSIX / `MoveFileEx` NTFS) `[CERT] UX=ChiAlarmHelper.java:607-641`. Serializada por `PERSIST_LOCK` `[CERT] UX=ChiAlarmHelper.java:374-403`.
- **Modo de falla:** I/O falla → log SEVERE, nunca throw; la nota queda en memoria y el próximo `addNote` reintenta la escritura `[CERT] UX=ChiAlarmHelper.java:239-240,394-401`.

API pública: `getNotesByUuid(uuid)` devuelve un array JSON `[{message,author,date}]` (`"[]"` si vacío) `[CERT] UX=ChiAlarmHelper.java:309-333`; `addNote(uuids[], note, author)` adjunta timestamp ISO-UTC + author y persiste, apuntando la MISMA nota a cada UUID del array `[CERT] UX=ChiAlarmHelper.java:341-366`.

---

## 166.6 — Agrupamiento por fuente para la página de alarmas (`ChiAlarmQueryHelper`)

`queryAlarmSources(range, out, context)` alimenta la página de alarmas estilo Reflow `[CERT] UX=ChiAlarmQueryHelper.java:58-171`:

- **BQL** (`buildSourcesBql`): mismo patrón de rango que la consola, `order by timestamp desc`, **cap post-fetch** `MAX_RECORDS = 500` (no `BQL LIMIT`) `[CERT] UX=ChiAlarmQueryHelper.java:37,78,211-218`. Usa `rangeToWindow` (default `"last8h"`) para soportar rangos calendario `[CERT] UX=ChiAlarmQueryHelper.java:64`.
- **Agrupa por source ord** en un `LinkedHashMap` (preserva el orden time-desc del BQL) `[CERT] UX=ChiAlarmQueryHelper.java:73,85-125`. Por grupo cuenta `ackCount`/`unackCount` (`rec.getAckState() == BAckState.acked`) y mantiene el `lastUpdate` más reciente `[CERT] UX=ChiAlarmQueryHelper.java:131-147`.
- **`source_type` (`"alarm"` vs `"latch"`):** por cada fuente resuelve el `BChiUp` desde el ord (marcador `/ChiDashboardService/`), extrae el trigger-slot, mapea al latch key (`sobrecargaFan`, `sobrecargaCompresor1/2`, `proteccionFase`…) y consulta `computeSourceType(key, up.getAlarmLatches())` → `"latch"` si la clave está en el mapa, `"alarm"` si no `[CERT] UX=ChiAlarmQueryHelper.java:93,187-201,291-319,346-359`. Cualquier fallo degrada a `"alarm"` (safe default).
- **Shape `GroupRecord`** (`writeGroupRecord`): `source`, `priority`, `sourceType` + alias snake `source_type`, `ackCount`, `unackCount`, `lastUpdate` + `last_update`, `planta`, `message`, `uuid` (el más reciente), array `uuids[]` (todos), `alarmClass`, `alarmState`, `hyperlinkOrd` `[CERT] UX=ChiAlarmQueryHelper.java:230-262,269-285`. Emite camelCase + alias snake_case por compat de una release `[CERT] UX=ChiAlarmQueryHelper.java:238-244`.

---

## 166.7 — `ackAll`: colecta full-DB (servlet) + ack real (baja-native)

`ackAll` está **partido en dos responsabilidades** por una razón dura: `BAlarmService.ackAlarm(rec)` sobre un registro sacado del cursor es un **no-op silencioso** (smoke test: devolvía 413 pero el badge seguía en 413). El único path de ack probado es BajaScript `svc.ackAlarms()` `[CERT] UX=ChiAlarmHelper.java:1082-1090`.

1. **Colecta (servlet-mediated, COLLECT-ONLY):** `ackAllUnacked(context, maxUuids)` ejecuta `buildAckAllBql` = `station:|alarm:|bql:select * where ackState != 'acked'` (**toda la BD, sin filtro de tiempo**), materializa un `List<String>` de UUIDs (cap 2000), cuenta fuentes distintas y marca `truncated` si llega al cap. **No hace ack** `[CERT] UX=ChiAlarmHelper.java:1064-1067,1104-1148`. El handler llama con `maxUuids=2000` `[CERT] UX=BChiServlet.java:2086-2087`.
2. **Ack real (baja-native, client-side):** el frontend recibe `{uuids, sourceCount, truncated}` y hace `baja.Ord.make('alarm:').get().then(svc => svc.ackAlarms({ ids: uuids }))` `[CERT] UX=AlarmsManager.js:407-413` — el mismo path que `ackAlarms(uuids)` individual `[CERT] UX=AlarmsManager.js:271-322`.

---

## 166.8 — Handlers del servlet y superficie `/api/alarms*`

Ruteo en `doGet`/dispatch `[CERT] UX=BChiServlet.java:191-207,265-331` y catálogo de endpoints `[CERT] UX=BChiServlet.java:96-106`:

| Método + ruta | Handler | Delega en | RBAC |
|---------------|---------|-----------|------|
| `GET /api/alarms` | `handleAlarms` | `queryAlarms` | — (GET) |
| `GET /api/alarms/summary` | `handleAlarmSummary` | `getAlarmSummary` | — (GET) |
| `GET /api/alarms/sources` | `handleAlarmSources` | `queryAlarmSources` | — (GET) |
| `GET /api/alarms/source?ord&range&limit` | `handleAlarmsBySource` | `queryAlarmsBySource` | — (GET) |
| `GET /api/alarms/notes/{uuid}` | `handleAlarmNotesGet` | `getNotesByUuid` | — (GET) |
| `GET /api/alarms/hyperlink?ord` | `handleAlarmHyperlinkResolve` | `resolveHyperlinkSafe` | `remoteUser != null` → 403 |
| `POST /api/alarms/latch` | `handleAlarmLatch` | `latchAlarm` | `ChiRbacHelper.checkCanWrite` |
| `POST /api/alarms/unlatch` | `handleAlarmUnlatch` | `unlatchAlarm` | `ChiRbacHelper.checkCanWrite` |
| `POST /api/alarms/notes` | `handleAlarmNotesPost` | `addNote` | `ChiRbacHelper.checkCanWrite` |
| `POST /api/alarms/ackAll` | `handleAlarmAckAll` | `ackAllUnacked` | `ChiRbacHelper.checkCanWrite` |

- Los **4 POST mutantes** empiezan con `if (!ChiRbacHelper.checkCanWrite(req, resp)) return;` (guard que maneja 401 + 403) `[CERT] UX=BChiServlet.java:1321,1409,2014,2080`. Ver [Block 164].
- Latch/unlatch resuelven el `BChiUp` con `resolveChiUp(ord)` y responden 400 si el ord no resuelve o no es `BChiUp` `[CERT] UX=BChiServlet.java:1354-1361,1441-1448`.
- Cada mutación escribe un audit vía `resolveDashboardService().appendAudit(ChiAuditHelper.buildEntry(...))`; el fallo de audit se loguea WARN y se ignora (no rompe la respuesta) `[CERT] UX=BChiServlet.java:1371-1385,1458-1472,2035-2051,2107-2123`.

---

## 166.9 — `resetAlarmas`: la única acción de alarma baja-native del equipo

`resetAlarmas` es el **primer `@NiagaraAction` del codebase chihuahua** `[CERT] RT=components/BChiUp.java:357-359,2013`, declarado a nivel de clase (convención Niagara) con el par slotomatic `resetAlarmas` (constante) / `doResetAlarmas()` (implementación) `[CERT] RT=components/BChiUp.java:1510,1517,2022`.

`doResetAlarmas()` `[CERT] RT=components/BChiUp.java:2022-2076`:
1. Captura la identidad del operador desde `com.tridium.util.ContextThread.getContext().getUser()` (Principal); fallback `"unknown"` si no hay contexto autenticado `[CERT] RT=components/BChiUp.java:2034-2048`.
2. `setAlarmLatches("{}")` — reset de la única fuente de verdad de latches `[CERT] RT=components/BChiUp.java:2051`.
3. `syncProtectionSlots()` → pone todos los `protXActive` en false `[CERT] RT=components/BChiUp.java:2054`.
4. `BChiDashboardService.clearTripped(ord)` limpia los `trippedFlags` en memoria (0 si el dashboard no arrancó todavía — aceptable) `[CERT] RT=components/BChiUp.java:2060-2070`.
5. Un solo `LOG.info` de audit con ord/user/cleared `[CERT] RT=components/BChiUp.java:2073-2075`.

Es **baja-native**: se invoca como acción Niagara sobre el componente, **no** pasa por el servlet — a diferencia de latch/unlatch (§166.4), que sí son servlet-mediated. La protección queda "tripped" permanentemente hasta que el operador invoca `resetAlarmas()` explícitamente (T-B-5: auto-rearm eliminado) `[CERT] RT=components/BChiDashboardService.java:737,1090`.

### Baja-native vs servlet-mediated — resumen
| Operación | Camino | Cita |
|-----------|--------|------|
| Query / summary / sources / historia por fuente | servlet BQL (`ChiAlarm*Helper`) | §166.2, §166.3, §166.6 |
| Latch / unlatch | servlet → slot `alarmLatches` (RMW) | §166.4 |
| Notas | servlet → HashMap + archivo | §166.5 |
| `ackAll` colecta | servlet (COLLECT-ONLY) | §166.7 |
| **Ack real (`svc.ackAlarms` vía `alarm:` ord)** | **baja-native (BajaScript)** | `[CERT] UX=AlarmsManager.js:321-322,412-413` |
| **`resetAlarmas`** | **baja-native (`@NiagaraAction`)** | `[CERT] RT=components/BChiUp.java:357-359,2022` |

---

## 166.10 — Consumo del frontend

`AlarmsManager.js` (IIFE ES5) `[CERT] UX=AlarmsManager.js:295-322,350-443`:
- El feed `/api/alarms` es la consola *ackState-driven* y **NO** es la fuente de verdad de latches; los latches vienen de `BChiUp.alarmLatches` entregado por `/api/equipment`. Los latches manuales escritos vía `/api/alarms/latch` persisten en `alarmLatches` pero pueden no aparecer como `ackPending` en `/api/alarms` `[CERT] UX=AlarmsManager.js:138-148`.
- `ackAlarms(uuids, note, opts)` y `ackAllFullDb(opts)` bloquean la operación si `canWrite === false` (viewer) antes de tocar BajaScript `[CERT] UX=AlarmsManager.js:300-307,360`.
- `ackAllFullDb` hace `POST /mx60/api/alarms/ackAll` (colecta) y luego ackea los UUIDs devueltos vía `svc.ackAlarms({ids})` `[CERT] UX=AlarmsManager.js:350-413`.
- URLs configurables vía `cfg.api.*` con defaults `/mx60/api/alarms…` `[CERT] UX=AlarmsManager.js:48,367`.

---

## 166.x — Connections

- **[Block 164]** — Todas las escrituras de alarma (latch/unlatch/notes/ackAll) están *RBAC-gated* por `ChiRbacHelper.checkCanWrite` antes de cualquier mutación `[CERT] UX=BChiServlet.java:1321,1409,2014,2080`; el frontend replica el gate con `canWrite` `[CERT] UX=AlarmsManager.js:300-307`. El detalle del modelo RBAC + audit trail se documenta en [Block 164].
- **[Block 163]** — Continuación del recorrido UX/servlet de chihuahua (dispatch, endpoints, patrón de handlers). Este bloque profundiza el sub-dominio de alarmas del mismo servlet `BChiServlet`.
- **[Block 142]** — Contraparte de comparación: el subsistema de alarmas/BQL de **Reflow**. chihuahua adopta el mismo estilo de página "por fuente" (`ChiAlarmQueryHelper` está anotado como *Reflow-style* `[CERT] UX=ChiAlarmQueryHelper.java:19,24`), pero introduce dos divergencias propias: (1) el modelo **latch** anclado en un slot `alarmLatches` del equipo (Reflow no tiene equivalente en el equipo), y (2) el `ackAll` **partido** colecta-servlet + ack-BajaScript por el no-op de `BAlarmService.ackAlarm` en registros de cursor `[CERT] UX=ChiAlarmHelper.java:1082-1090`. Ver [Block 142] para el contraste de la implementación Reflow.
