# Bloque 73 — History + Data domain TIER-1 audit triple-source: Reflow + MX60 + Bloques 1-71 guidance

**Fecha**: 2026-05-10
**Methodology**: Triple-source (engram #1259) + bilateral-critique (engram #1258)
**Scope**: dominio History + Data (charts + time range + fetch + sampling + cache + live update). Independiente del flow alarms (bloque #72 separado).
**Goal específico**: Reflow puede graficar para CUALQUIER lapso (incluyendo histories largas con 3000+ records). MX60 actualmente "solo grafica para 1 hora". Entender CÓMO Reflow lo maneja para integrar el mecanismo de time range a MX60.
**Source READ-ONLY**: `/home/cristian/modules/Prototipos/Reflow-Clean-177/` + `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/`

---

## §73.0 — Resumen ejecutivo (3 veredictos)

### (A) **Time range vocabulary disjoint — root cause del "1-hour effective limit" MX60**

**Empírico confirmado** — NO es BD retention, NO es Chart.js limitation, NO es backend cap fundamental. Es un bug de **string mismatch frontend↔backend** silencioso:

- MX60 frontend `RANGES` (UpDetail.js:171-176): IDs `'1h', '8h', '24h', '7d'`
- MX60 backend `ChiHistoryHelper.computeRange` (ChiHistoryHelper.java:257-318): keys aceptados `lastHour, last8Hours, today, last24Hours, yesterday, last7Days, last30Days, monthToDate`
- Frontend pasa `range='7d'` → backend hace `if/else if` chain comparando con `"last8Hours"`, `"today"`, etc. — NINGUNO matchea `'7d'` → cae al `else` final (L312-313): `cal.add(Calendar.HOUR_OF_DAY, -1)` = **lastHour silencioso**.
- Síntoma exacto del usuario: cada tab '1h/8h/24h/7d' devuelve la misma 1h de data.

→ **Veredicto**: el primer fix es **mapping vocabulary**. NO hay limitación arquitectónica. Sprint-1 OBLIGATORIO.

### (B) **Sampling/downsampling — Reflow NO sampling, MX60 SÍ sampling client-side, ambos truncan**

- **Reflow backend** (`HistoryDataResponse.java:196-211` arrayForHistoryCollection): `cursor.next()` loop completo con `limit==0 → no cap`. Para `range=lastYear` con 1pt/min: 525,600 records → todos van al JSON response.
- **Reflow frontend** (historyCache.js → d3Options): map sin sampling. Pasa los N records a D3chart engine global.
- **D3chart** (3114L, ya audited bloque #45): bucketing temporal client-side simple (NOT LTTB). webChart nativo Niagara: `autoSamplingSize=2500` default, `maxSeriesCapacity=250000` cap.
- **MX60 backend** (`ChiHistoryHelper.java:194`): `maxPoints = 2000` HARDCODED **truncate** (descarta records nuevos). NO downsampling — toma primeros 2000 por cursor order.
- **MX60 frontend** (`UpDetail.js:894-907` filterHistoryByRange): downsampling client-side stride-based: `stride = max(1, floor(filtered.length / r.points))` con `r.points` ∈ {60, 96, 96, 168}. SÍ hace sampling — diferencia clave vs Reflow.

→ **Veredicto**: heredar **MX60 client-side stride sampling** (es superior al Reflow no-sampling para HVAC dashboards). Pero **subir el backend cap** de 2000 a un valor más alto (ej. 50000 conforme `niagara.webChart.maxSamplingSize`) y **agregar opción styles** ('apex' tuple vs object) para reducir payload size en lapsos largos.

### (C) **Chart engine — Chart.js (MX60) vs D3 (Reflow) — ambos viables, MX60 más simple**

- **Reflow**: D3chart.vue **3114 LOC** engine global (registrado app-level en main.js Webpack, requiere fix Vite/Vue 3 explícito per bloque #68 §68 #234). 11 charts components, 2 NO history (GraphicReflow + GraphicSelect). SVG d3 — bucketing client + zoom/pan/tooltip/compare/export/delta/contextMenu. Custom developer time alto.
- **MX60**: Chart.js v4 `type: 'line'` (UpDetail.js:3273). 7 charts por UP. `MAX_CHART_POINTS = 200` for live append (L3327). Plugins propios `comfortBandPlugin` + `htmlLegendPlugin`. `_appendLiveSample` O(1) per notify (L3329-3377). Stagger rebuild via requestAnimationFrame (L3692-3709) — fix concreto a "Forced reflow took 300-500ms" violations.

→ **Veredicto**: **PRESERVAR Chart.js MX60**. NO migrar a D3 — D3chart Reflow es over-engineered para casos sub-day HVAC. Para lapsos largos (`>24h, 7d, 30d`) Chart.js con line chart simple + sampling stride es suficiente para el requirement del usuario.

---

## §73.1 — Bloques relevantes History + Data (Stage 0)

| # | Topic | Guidance history+data | Status vs hallazgos #73 |
|---|-------|------------------------|--------------------------|
| **45** | History/Trend chart consumption SPA externa (engram #892) | WebChartQueryServlet GET NDJSON chunked + 12 BWebChartTimeRangeType nativo + `autoSamplingSize=2500` default + `maxSamplingSize=50000` + `maxSeriesCapacity=250000` client cap + `interpolateRefreshInterval=10000ms` tail + boxcs real-time tail subscription. SVG d3 NO Canvas2D/WebGL. Bucketing temporal client NO LTTB. SeriesTransform server-side rollup. 9 antipatterns G45-1..G45-9. | **VIGENTE** — Reflow respeta vocabulary nativo (12 ranges) parcialmente (15 customs); MX60 ignora completamente — recomendamos heredar `autoSamplingSize=2500` como inspiración para nuevo cap MX60 backend. |
| **46** | Writes priority array oBIX REST + BOX BajaScript + NiagaraRPC | NO aplica directo a history reads — pero **Transaction NO ACID** confirmado triple (Bloques 32+41+46) lección reusable. | **VIGENTE NO-APLICABLE** — history es read-only, sin priority array. |
| **62** | Alarmas backend audit + AP-72..78 + reglas 20-22 + #62 §62.9.3 polling componentes | **NO history**, pero §62.9.3 patrón polling componentes (vs Vuex centralizado) confirmado bloque #69 — paralelo conceptual a "history charts polling guard". | **VIGENTE** — confirma patrón polling distribuido. |
| **68** | Transplante alarmas+history+charts blueprint Reflow → MX60 (#228..#243) | §68.1 backend HEREDA 95% (~1500L copiable, 6 helpers alarms + 5 helpers history + RangeCalculator/CompareRangeCalculator + 7 responses + WS BReflowChannelService). §68.0 historyCache shape post-S56. **§68.1.6 SPLIT HistoryData en 3 clases REFUTED por bloque #70** (Cache class ficticia → 2 clases o 3 alternativo). #234 D3chart global app.component() obligatorio Vite/Vue 3. | **§68.1 vigente con corrigendum #70** — HistoryData split AJUSTADO. **§68.2 frontend Vue 3 REFUTED** post engram #1257 (MX60 IIFE+ES module hybrid no Vue). Para #73 importa el inventario backend (bloque #68 §68.1 sigue siendo el mapa de código copiable). |
| **69** | Audit empírico patrones live-update Reflow-Clean-177 + cierre flag #241 (#244..#253) | #245 **history charts polling guard ≥30s default 300s = 5min** (HistoryChart.vue:304-309). #252 HistoryGhostSubscriber **one-shot detection NO keepalive** (corrige bloque #68 §68.1.5). #249 bajaHeartbeat DORMANT en clean-room — MX60 sprint-1 OBLIGATORIO start(baja). | **VIGENTE** — confirmado en este bloque: HistoryChart.vue:304-309 polling 300s default + Reflow no usa boxcs tail por design (mock backend). |
| **70** | Audit empírico TIER-1 HistoryData + Ack flow + historyCache (#254..#264) | #254 HistoryData 100% static + 1 inner Runnable + cero state outer. #255 `HistoryDataCache` class FICTICIA (cache real vive en frontend xa). #256 SimpleDateFormat+DecimalFormat **NO thread-safe** static fields. #262 historyCache shape post-S56 CONFIRMADO empírico. #263 bug `last24Hours` operator precedence (24 días vs 24 horas) historyCache.js:57. #264 bug skip-cache `state.groups` undefined NEVER fires historyCache.js:489. | **VIGENTE + CONFIRMADO empírico aquí** — `last24Hours` bug L57 confirmé visualmente: `subtractMs(t, 24 * DAY / 24 * 24)` = `t - 24*86400000/24*24` = `t - 86400000*24` (24 días). #256 thread-safety idéntico bug en `HistoryDataResponse.java:39-40` (SimpleDateFormat + DecimalFormat static — replica del bloque #70 #256 en otro archivo). |
| **71** | Equipment domain TIER-1 audit triple-source (#265..#284) | UpDetail.js 3841 LOC + ES module hybrid + IIFE classic + EquipmentSnapshotStore single-subscribe RAF 500ms + REST fallback 5s. **#279 HISTORY_MINUTES cap UpDetail.js:396 confuso bajo REST fallback**. | **VIGENTE + EXTENDIDO** — #279 confirmado L388-396 (cap reducido 7d→24h pero comentario Spanish menciona REST fallback 5s = 5760 entries per 8h shift > 1440 cap). |

**Bloques NO aplicables**: #1-43 (foundations), #44 (alarm console — flow paralelo), #47-49 (frontend SPA bootstrap+CSRF+i18n — backend independent), #50 (Reflow Par A audit — equipment-focused), #61 (libraries catalog — REFUTED post #1257), #63 (Vue 2.7 audit — REFUTED), #65 (síntesis backlog MX60 — REFUTED partial), #66-67 (Analytics — separate scope), #72 (alarms triple-source — independent flow per user note).

---

## §73.2 — Audit Reflow History+Data empírico

### §73.2.1 — Backend inventario

**Archivos audited file:line** (READ-ONLY):

| File | LOC | Rol |
|------|-----|-----|
| `commands/BReflowHistoryCommands.java` | 113 | 7 BOX endpoints — getList, getQuickList, getData, getGroupNames, getGroupTree, getDeviceTree, getDevices |
| `util/RangeCalculator.java` | 300 | **15 ranges** mapping ordinal→`BAbsTime[start,stop]`: lastHour(0), last8Hours(1), last12Hours(2), today(3,DEFAULT), last24Hours(4), yesterday(5), weekToDate(6), lastWeek(7), last7days(8), last30days(9), monthToDate(10), lastMonth(11), yearToDate(12), lastYear(13), last12Months(14) |
| `util/CompareRangeCalculator.java` | 316 | Espejo shifted — overlay del período anterior. lastHour compare = -2..-1h, last24Hours compare = -48..-24h, lastYear compare = ~2 años atrás |
| `history/HistoryData.java` | 663 | (per bloque #70 #254) 100% static + 1 inner Runnable, cero state outer. 16 outer methods + entry points fromComponent / jsonForHistory(3 overloads) / jsonForLastRecord. SimpleDateFormat+DecimalFormat static fields L49-50 NO thread-safe (#256). |
| `history/HistoryList.java` | 355 | List enumeration + 2 outputs (full JSON + simplified node) |
| `history/HistoryGroups.java` | 112 | Group/device tree builders |
| `history/HistoryGhostSubscriber.java` | 26 | (per bloque #69 #252) one-shot detection auto-unsuscribe — NO keepalive |
| `history/json/` (6 serializers) | ~600 | HistoryObjectMapper registry + 4 serializers + IHistorySeralizer typo |
| `http/responses/HistoryListResponse.java` | 84 | REST GET — disk cache GZIP via HistoryIO + TeeOutputStream |
| `http/responses/HistoryGroupsResponse.java` | 83 | Mismo patrón con HistoryIO.GROUP_CACHE |
| `http/responses/HistoryDataResponse.java` | 265 | REST GET single-history (style + range + limit) — **NO downsampling, NO chunked** — `cursor.next()` loop full + JSONArray complete |
| `http/responses/HistoryChartDataResponse.java` | 74 | REST GET chart data (multi-series + compare + start/end + contextualRanges) — wraps HistoryData.fromComponent |

### §73.2.2 — Time range mechanism Reflow

```
[User TimeRangePicker.vue:38] DropdownItem name="lastHour"
    ↓ @on-click
[selectItem L132-138]
    ↓ this.$emit('input', t)
[parent — HistoryForm/HistoryBuilder/AlarmsHome] v-model
    ↓ stored in card.config.range
[HistoryChart.vue:17] :range="rangeOverride || card.config.range || 'lastHour'"
    ↓ prop
[Chart.vue:80-83] range: { type: String, default: 'lastHour' }
    ↓ watch range L186-191
[Chart.vue updateHistoryData()]
    ↓
[historyService.data L197-247]
    ↓ buildHistoryQueryString
[axios.get('/nmodsreflow/station/history-data?histories=A,B&range=lastHour&style=d3')]
    ↓ HTTP GET
[HistoryDataResponse.serve L42-81]
    ↓ Query.map(req.getQueryString())
    ↓ historyName.split(",") multi-history
[per history → BDateRangeEnum.make(range) → RangeCalculator.make(enum)]
    ↓ BAbsTime[start, stop]
[BHistoryDatabase.timeQuery(history, start, stop)]
    ↓ Cursor<BHistoryRecord>
[arrayForHistoryCollection L196-211]
    ↓ NO downsampling, NO sampling — todos los records
[JSONArray response]
```

**Time range types disponibles 3 vocabularios divergentes en Reflow**:

| Vocabulario | Source | # | Keys |
|-------------|--------|---|------|
| **Backend** RangeCalculator | RangeCalculator.java:8-56 | 15 | lastHour, last8Hours, last12Hours, today, last24Hours, yesterday, weekToDate, lastWeek, last7days, last30days, monthToDate, lastMonth, yearToDate, lastYear, last12Months |
| **Frontend ChartToolBar** dropdown | ChartToolBar.vue:38-180 | 13 | lastHour, last8Hours, today, last24Hours, yesterday, weekToDate, lastWeek, last7Days(C), monthToDate, lastMonth, yearToDate, lastYear, last12Months — falta last12Hours, last30days |
| **Frontend TimeRangePicker** fallback | TimeRangePicker.vue:62-66 | 15 | last15, lastHour, last4Hours, last8Hours, last12Hours, today, yesterday, last3Days, lastWeek, last2Weeks, lastMonth, last3Months, last6Months, lastYear, last12Months — incluye **6 keys NO existentes en backend**: last15, last4Hours, last3Days, last2Weeks, last3Months, last6Months |

**Implication**: 3 vocabularies divergentes en Reflow producción. Click "last3Months" en TimeRangePicker → backend `BDateRangeEnum.make("last3Months")` (probable null/exception) → `BDateRangeEnum.DEFAULT.getRange().isTag(range)` check L66 → fallback a `BDateRangeEnum.today` L69. Bug latente — el TimeRangePicker fallback es un bug pero salvado por backend default.

Cuando `$niagara.util.timerange` está disponible (Workbench host), el plugin Niagara provee `display(value)` + `ranges` overrideando el hardcoded fallback. Probable que producción usa el plugin (nativo 12 BWebChartTimeRangeType + custom), no el fallback.

### §73.2.3 — Data fetch para lapsos largos Reflow

- **Backend NO downsampling**: `arrayForHistoryCollection` L196-211 hace `while (cursor.next() && (limit == 0 || count <= limit))` — devuelve todos los records O hasta limit (solo si frontend pasa `limit` explícito).
- **NO chunked / NO NDJSON**: response es un único `JSONObject.toString()` write completo. Reflow custom endpoints difieren de WebChartQueryServlet nativo (que SÍ es NDJSON chunked per bloque #45).
- **Multi-history**: comma-separated en path: `historyName.split(",") L48-52` — UN response con dictionary `{historyName1: [...], historyName2: [...]}`.
- **2 estilos output**: `style=apex` → array `[ts, value]` minified. Default → object `{time: "MM/dd/yyyy HH:mm:ss", value: ...}` rico (con string format). Trade-off payload size.
- **NO timezone awareness**: usa `dateFormat.format(new Date(rec.getTimestamp().getMillis()))` con SimpleDateFormat static `Locale.US` — siempre browser-equivalent UTC, NO BTimeZone.
- **Resolves history INEFICIENTE**: `BOrd.make("history:")` + `getHistories()` loop comparando `getDisplayName()` per request — O(N) per request, escala mal con 1000+ histories station.
- **Disk cache GZIP**: solo aplica a HistoryListResponse + HistoryGroupsResponse (LIST/GROUPS, NO DATA). Cache hit = stream from disk (FAST). Cache miss = full traversal + cache write.

**Para 3000+ records (típica BD HVAC station 1pt/min × 50h)**:
- Reflow backend devuelve los 3000 records en JSONArray `[[ts1, val1], [ts2, val2], ...]` style=apex
- ~3000 × 30 bytes ≈ 90KB raw JSON sin gzip (si endpoint soporta gzip, ~10-15KB)
- Frontend D3chart recibe vía `historyService.data` → `d3Options` map → series object → D3chart engine
- D3chart con 3000 puntos OK SVG d3 (per bloque #45 max 50000 antes degradación; LTTB no implementado pero bucketing simple alivia)

### §73.2.4 — Live update Reflow

- **HistoryChart.vue:304-309**: `setInterval` con guard `>= 30s`, default `300s` (5 min) — confirmado bloque #69 #245.
- **NO boxcs tail subscription** en clean-room (mock backend). Per bloque #45, en producción Niagara nativo charts usan `boxcs` subscription al `BControlPoint.out` con `PointSeries.subscribe()` — Reflow custom NO replica esto, hace solo polling.
- **`interpolated tail`** (bloque #45): client-side virtual point con y=lastPoint.y, x=Date.now() refresh `interpolateRefreshInterval=10000ms` — Reflow custom NO replica (mock).
- **Refresh handler**: `refreshData(true)` → `chart.updateNowAndHistoryData(keepIdentifiers)` (Chart.vue ref method) — keepIdentifiers preserva uid de series para evitar recreate gradients/animations.

### §73.2.5 — Chart rendering Reflow

- **D3chart.vue 3114 LOC** (NO leído en este bloque por scope, info via bloque #45 + #68): SVG d3 engine global registrado app-level. `app.component('D3chart', D3chart)` Vite/Vue 3 obligatorio (#234).
- **Compare mode**: data prefijada con `'|compare|'` separator (`compareKey` historyCache.js:132). Series con `key.indexOf(compareKey) !== -1` → `getCompareRange(now, range)` overlay.
- **Bucketing temporal client-side**: simple stride (NO LTTB). Per bloque #45 G45-7: para 100K+ puntos requeriría LTTB propio o WebGL — Reflow NO lo implementa.

### §73.2.6 — historyCache flow Reflow (post-S56 confirmed)

- **Module-level cache `xa`** (historyCache.js:85-91): 5 props `histories/groups/devices/groupIndex/pagination`. NO en Vuex state. Components import directly.
- **Service `Sa`** (historyCache.js:122-462): 9 métodos público — list / loadList / loadGroups / loadDevices / loadDeviceTree / generate / data / buildHistoryQueryString / d3Options.
- **Builder recursivo `Ia`** (historyCache.js:98-114): groupsIndexBuilder mapping `{historyId: [groupFullPath, ...]}`.
- **Vuex minimal `Ta`** (historyCache.js:468-516): solo `state.invalid + SET_INVALID + refresh action`. Action `refresh` carga list+groups+devices y commit invalid=false.
- **2 BUGS LATENTES confirmados visualmente** (per bloque #70 #263 y #264):
  1. **`last24Hours` L57**: `subtractMs(t, 24 * DAY / 24 * 24)` — operator precedence left-to-right: `(24*86400000)/24*24` = `86400000*24` = **24 días** NO 24 horas. Fix: `subtractMs(t, 24 * HOUR)`.
  2. **skip-cache L489**: `if (!state.invalid && state.groups && !invalidate && rootState.histories.localCacheEnabled)` — `state.groups` NUNCA definido en state (L470-472 solo tiene `invalid: true`) → condición NEVER fires → cache SE REFETCHEA siempre. Comment L487-488 reconoce bug-as-feature ("matches the bundle"). Fix: `historyData.groups.length > 0`.

---

## §73.3 — Audit MX60 History+Data empírico

### §73.3.1 — Backend inventario

| File | LOC | Rol |
|------|-----|-----|
| `chihuahua-ux/src/com/angeles/chihuahua/ux/ChiHistoryHelper.java` | 619 | **Único helper history** — combina list + query + auto-detect |
| `chihuahua-rt/srcTest/test/.../BChiCarcamoHistoryTest.java` | (test) | Unit tests carcamo histories |
| `chihuahua-rt/srcTest/test/.../BChiDataLoggerHistoryTest.java` | (test) | Unit tests datalogger histories |
| `chihuahua-ux/srcTest/test/.../ChiHistoryHelperTest.java` | (test) | Unit tests del helper |

**Endpoints REST MX60** (3 distintos, NO BOX):
1. `GET /mx60/api/historyList` → `listHistories(out, context)` L74-126 — enumera todos los histories station
2. `GET /mx60/api/historyData?id={historyId}&range={rangeName}` → `queryHistoryData` L132-229 — single-history time query
3. `GET /mx60/api/equipment-histories` → `detectEquipmentHistories(service, out, context)` L338-428 — auto-resolution mapping `{equipId: {slotName: historyId}}`

**HISTORY_PROPERTIES split por equipment type** L43-66:
- `UP_HISTORY_PROPERTIES` (11): tempZona, tempAbasto, tempRetorno, tempSuccion1, tempSuccion2, setpoint, ampCompresor1, ampCompresor2, ampAbanicos1, ampAbanicos2, ampFan
- `CARCAMO_HISTORY_PROPERTIES` (1): nivelCm
- `DT_HISTORY_PROPERTIES` (2): pressurePsi, pressureBar (REQ-G10-1 separación)

**Auto-resolution via link graph** (NO en Reflow):
- `detectEquipmentHistories` walks 6 plantas × 3 monitors × N equipment
- `resolveHistoryViaLink` L438-462: follow BLink → source point → BHistoryExt → BHistoryId
- `resolveHistoryViaName` L538-564 fallback: lowercase name matching `{label|slotName}_{propName}`
- `findHistoryExtIdRecursive` L481-528: MAX_HISTORY_SEARCH_DEPTH = 3 — busca BHistoryExt directamente o bajo `extensions/` o folders custom (kitControl Ramp pattern)

### §73.3.2 — Time range mechanism MX60

```
[User clicks .up-range-tab data-range="7d"] (UpDetail.js:327 button rendered from RANGES)
    ↓ click event
[onRangeTabClick L3399-3432]
    ↓ setRange(trendId, '7d') → trendState[trendId].range = '7d'
    ↓ rebuildChart(trendId) [client-side filterHistoryByRange]
    ↓ if range !== _currentHistoryRange: _loadRealHistory(equip, '7d')
[_loadRealHistory L690+ → MX60.HistoryIndex.load + per slot _fetchSlotHistory]
    ↓ MX60.HistoryIndex.load() — cached `/mx60/api/equipment-histories` per session
    ↓ per slot in histMap → _fetchSlotHistory(histId, '7d')
[_fetchSlotHistory L544-587]
    ↓ url = base + '?id=' + encodeURIComponent(histId) + '&range=' + encodeURIComponent('7d')
    ↓ XHR GET '/mx60/api/historyData?id=...&range=7d'
[ChiHistoryHelper.queryHistoryData L132-229]
    ↓ computeRange('7d') L257-318
       if "last8Hours".equals('7d')   → false
       if "today".equals('7d')        → false
       if "last24Hours".equals('7d')  → false
       if "yesterday".equals('7d')    → false
       if "last7Days".equals('7d')    → false  ← '7d' !== 'last7Days'
       if "last30Days".equals('7d')   → false
       if "monthToDate".equals('7d')  → false
       else → cal.add(Calendar.HOUR_OF_DAY, -1) ← lastHour silencioso
    ↓ BAbsTime[start = now - 1h, stop = now]
    ↓ conn.timeQuery(history, start, stop)
    ↓ cursor.next() loop con maxPoints=2000 truncate
[JSON {hId, title, units, range:'7d', data: [[ts, val], ...]}]
    ↓ frontend recibe — 60 puntos típicos (1h × 1pt/min)
[_mergeSeriesByTimestamp L601-644 — TOLERANCE_MS 5min]
    ↓ fullHistory.push.apply (replace o accumulate)
[filterHistoryByRange L894-907 — cutoff = now - 7*24*3600000 (7 días)]
    ↓ stride = max(1, floor(60/168)) = 1 (no sampling needed, ya es < points)
[Chart.js render — solo 60 puntos en ventana de 7 días = línea de 1h al final del eje]
```

**3 vocabularios MX60 internos disjoint**:
- Frontend RANGES ids (UpDetail.js:171-176): `'1h', '8h', '24h', '7d'`
- Frontend `_rangeToMs` (L652-660): mismas 4 keys (compatible con RANGES)
- Backend `ChiHistoryHelper.computeRange` (L257-318): 8 keys camelCase `lastHour/last8Hours/today/last24Hours/yesterday/last7Days/last30Days/monthToDate`

→ **Frontend nunca matchea backend → backend cae al `else` default lastHour para todos los tabs**.

### §73.3.3 — Data fetch MX60

- **Backend `maxPoints = 2000` HARDCODED** (ChiHistoryHelper.java:194). Cursor truncate (NO downsampling — discard records cuando count == 2000).
- **NO multi-history endpoint**: 1 XHR per slot. Equipment con 11 slots → 11 XHRs paralelos por chart load (mitigado por idle defer + RAF stagger rebuild).
- **Single-history JSON shape**: `{hId, title, units, range, data: [[ts, val], ...]}` — formato simple `[ts, val]` siempre (NO style param).
- **NO compare mode backend**.
- **NO gzip cache**.
- **NO timezone info** en response (vs Reflow que sí incluye timezoneOffset/Name/ShortName en d3Options).
- **Thread-safe**: NO usa SimpleDateFormat ni DecimalFormat estáticos. `extractValue` formatea inline por record (`String.format("%02d", ...)` per call). MEJOR que Reflow + HistoryDataResponse.

### §73.3.4 — Live update MX60

- **EquipmentSnapshotStore single-subscribe RAF 500ms throttle + REST fallback 5s** (per bloque #71 #270/#271). NO setInterval polling per chart.
- **`_appendLiveSample(trendId)`** (UpDetail.js:3329-3377): O(1) push label + value into existing dataset; trim to MAX_CHART_POINTS=200; chart.update('none'). 4 SAFETY NETS: baseline rebuild fallback, empty history no-op, atomic trim, try/catch fallback rebuild.
- **`HISTORY_MINUTES = 24*60 = 1440`** L388-396 (per bloque #71 #279) — cap fullHistory in-memory. Bajo REST fallback 5s × 8h shift = 5760 entries → over cap → Array.shift() O(N) per shift (frequent but smaller array).

### §73.3.5 — Chart rendering MX60

- **Chart.js v4** `type: 'line'` (UpDetail.js:3273). 7 charts por UP detail page.
- **Plugins propios**:
  - `comfortBandPlugin` — pinta zona de confort (HVAC ranges 14-22°C abasto, 20-24°C zona)
  - `htmlLegendPlugin` — custom HTML legend con grid 2-column cuando 4 datasets (live + baseline)
- **Stagger rebuild** L3692-3709: `requestAnimationFrame` chain (~16ms × 7 charts = ~112ms total) en lugar de forEach síncrono. Fix concreto a "Forced reflow took 300-500ms" violations DevTools.
- **Pre-cache canvas dimensions** L3264-3272: setea `canvas.width/height/style` ANTES de Chart.js init para evitar 7 forced reflows por chart (49 total para 7 charts).
- **In-place dataset update** L3292-3301: cuando count matches existing — preserva animation state, evita flicker.
- **Initial history load DEFERRED** L3780-3806: `requestIdleCallback` con timeout 300ms para que paint de 3D scene + shell se renderice primero.

### §73.3.6 — Cache layer MX60

- **`MX60.HistoryIndex` IIFE** (UpDetail.js:408-457): cached singleton per session — `/mx60/api/equipment-histories` mapping `{equipId: {slot: histId}}`. Drains waiter queue después del primer fetch. Subsequent calls síncronos (cache).
- **`MX60.HistoryListCache` IIFE** (UpDetail.js:472-533): fallback fetch ALL station histories — `/mx60/api/historyList` array. `findByNameContaining(needle)` para discovery cuando backend BLink-resolution falla.
- **NO chart data cache** (vs Reflow tampoco — cada chart fetch es fresh).
- **`LiveHistoryBuffer` integration** (referenced UpDetail.js:694, archivo separado): cuando `histMap` está vacío para un equip → `bufferOnly` mode → frontend filtra buffer series por range. Permite UI "Recolectando datos en vivo..." overlay (L3733-3755).

---

## §73.4 — Triple comparison por sub-tema (Stage 3)

| # | Sub-tema | Reflow approach | MX60 approach | Bloques guidance | Tensión |
|---|----------|-----------------|---------------|-------------------|---------|
| 1 | **Time range UI** | `TimeRangePicker.vue` 190L Dropdown 15 fallback ranges + `$niagara.util.timerange.ranges` override Workbench. `ChartToolBar.vue` 357L 13 ranges hardcoded. Cross-domain (HistoryBuilder/AlarmsHome/AlarmDetails) | `_rangeTabsHtml` L322-331 4 button tabs `data-range`. Per-trend (7 charts × 4 tabs = 28 tabs por UP detail). NO dropdown. | #45 12 BWebChartTimeRangeType nativo Niagara. #69 #245 polling guard ≥30s. | **Reflow más ranges + 2 vocabularios** vs **MX60 menos ranges + tabs inline + simpler UX**. MX60 limited a HVAC sub-day. |
| 2 | **Time range computation** (relative→absolute) | `RangeCalculator.java:7-300` 15 ranges (lastHour..last12Months) + `CompareRangeCalculator.java` 316L espejo shifted | `ChiHistoryHelper.computeRange` L257-318 8 ranges (`lastHour/last8Hours/today/last24Hours/yesterday/last7Days/last30Days/monthToDate`) | #45 Niagara nativo BWebChartTimeRangeType.toAbsolute() | **MX60 backend usa keys distintos al frontend** (`'7d'` vs `'last7Days'`) — bug silencioso |
| 3 | **Backend fetch mechanism** | REST GET `/nmodsreflow/station/history-data?histories=A,B,C&range=...&style=...` MULTI-history comma-sep + BOX `getData` paralelo + Niagara native (no usado clean-room) | REST GET `/mx60/api/historyData?id={histId}&range={range}` SINGLE-history. NO BOX. NO Niagara native via servlet aux. | #45 WebChartQueryServlet `GET /webChart/data/{ORD-escaped}` con NDJSON chunked + Accept header obligatorio | **Reflow multi-history (1 round-trip) vs MX60 single-history (N round-trips)** — para UP con 11 slots = 11 XHRs paralelos. Mitigado por HTTP/2 multiplexing pero ineficiente. |
| 4 | **Sampling / downsampling** | **NO sampling** ni backend (cursor.next loop) ni frontend (d3Options map). D3chart bucketing simple. webChart nativo `autoSamplingSize=2500`. | **Backend truncate `maxPoints=2000`** L194 + **frontend stride downsampling** filterHistoryByRange L894-907: `stride = max(1, floor(filtered.length / r.points))`. r.points ∈ {60, 96, 96, 168}. | #45 G45-7 LTTB NO implementado, simple bucketing | **MX60 sampling client-side superior** (Reflow no tiene). PERO MX60 backend truncate (NO downsample) — primeros 2000 silenciosos sin tail records. Hybrid issue. |
| 5 | **Streaming vs full-load** | **Full-load** JSONArray complete `out.write(json.toString())` HistoryDataResponse.java:79. NO chunked. NO NDJSON. webChart nativo SÍ NDJSON (no usado custom). | **Full-load** PrintWriter print loop (one record at a time pero respuesta single body). NO streaming. | #45 NDJSON chunked nativo Niagara | **Ambos full-load**. Para histories largas (50K+ records) ambos fallan. Reflow puede gzip via reverse proxy/Niagara filter — MX60 no tiene. |
| 6 | **Live update / tail** | **setInterval polling** `interval >= 30s, default 300s` HistoryChart.vue:304-309. NO boxcs tail (mock backend clean-room). Producción Niagara nativo usa boxcs. | **EquipmentSnapshotStore single-subscribe** RAF 500ms throttle + REST fallback 5s (per #71 #271). `_appendLiveSample` O(1) per notify. | #45 boxcs `BControlPoint.out` PointSeries.subscribe + `interpolated tail` virtual point. #69 #246 patrón subscriber lifecycle. | **MX60 modelo unificado superior** vs Reflow polling per-chart. MX60 single subscription drives N charts. |
| 7 | **Chart engine** (performance, scalability) | **D3chart 3114L** SVG d3 engine global. 11 charts components. Vite/Vue 3 requiere `app.component('D3chart', D3chart)` explícito (#68 #234). Bundle peso significativo. | **Chart.js v4** `type: line` 7 charts/UP. `MAX_CHART_POINTS=200` live append cap. Plugins custom comfortBand + htmlLegend. RAF stagger + canvas pre-cache. | #45 SVG d3 NO Canvas2D/WebGL. Para 100K+ puntos requeriría LTTB o WebGL. | **Para HVAC sub-day ambos OK**. D3chart over-engineered, Chart.js suficiente. **Preservar Chart.js MX60**. |
| 8 | **Cache layer** | Module-level `xa` cache 5 props (histories/groups/devices/groupIndex/pagination) + Sa service 340L + minimal Vuex `Ta` `state.invalid` + 2 BUGS LATENTES (#263, #264). Backend disk cache GZIP via HistoryIO (LIST + GROUPS only, NO DATA). | `MX60.HistoryIndex` IIFE cached singleton equipment-histories mapping. `MX60.HistoryListCache` IIFE fallback ALL histories. NO chart-data cache. NO disk cache. | #68 #238 historyCache shape post-S56 confirmed empírico (#70 #262) | **Reflow cache más rico (LIST/GROUPS/DEVICES + Vuex invalidation) pero 2 bugs latentes**. **MX60 IIFE simpler + thread-safe (frontend single-thread anyway) sin bugs documentados**. |
| 9 | **Compare mode** (overlay 2 ranges) | `CompareRangeCalculator.java` espejo shifted + `compareKey = '|compare|'` separator + `getCompareRange(now, range)` historyCache.js:74-78 | **NO compare mode** | #45 SeriesTransform IntervalSeriesCursor server-side alignment | **Reflow tiene, MX60 no — feature gap**. Para HVAC baseline "ayer" MX60 usa `baselineParallel` UpDetail.js:912-935 (binary-search 24h shift) — más specific use case. |
| 10 | **Export** (CSV, PNG, SVG) | ChartToolBar.vue:202-206 dropdown SVG/PNG/CSV via D3chart `exportSelected` event | **NO export en MX60** (UpDetail.js no incluye export) | — | **Reflow tiene, MX60 no — feature gap**. Sprint-2+ candidate. |
| 11 | **Multi-series alignment** | client-side cada serie mantiene timestamps + d3Options map. server-side opción via SeriesTransform IntervalSeriesCursor (no usado custom). | `_mergeSeriesByTimestamp` UpDetail.js:601-644 union timestamps + closest-in-time TOLERANCE_MS=5min binary search | #45 IntervalSeriesCursor + BQuantizationTable rollup server-side | **MX60 client-side merge tolerance 5min** apropiado HVAC 1pt/min. **Reflow client mantiene timestamps separados** (D3chart maneja gaps). Ambos válidos. |
| 12 | **Bucketing / aggregation** | NO bucketing custom (D3chart engine bucketing simple per #45). | NO bucketing real — solo stride sampling. NO time-window aggregation. | #45 BHistoryRollup pre-computado + SeriesTransform runtime rollup | **Ambos NO bucketing nativo**. Para 7d con 1pt/min = 10080 puntos mostrar bien requiere bucketing/rollup. Sprint-2+ candidate. |

---

## §73.5 — Recommendations TIER 1/2/3 con justificación 3-source

### **TIER 1 — Sprint-1 OBLIGATORIO** (sin estos, MX60 no resuelve el goal del usuario)

#### **R1.1 — Mapping vocabulary frontend↔backend MX60** (root cause del 1-hour limit)

**Problema** (§73.6): frontend RANGES ids (`'1h', '8h', '24h', '7d'`) NO matchean backend keys (`lastHour, last8Hours, ..., last7Days`). Backend cae al default `lastHour` para todos los tabs.

**Opciones**:
- **(a) Renombrar frontend RANGES ids** → camelCase backend-compatible: `'lastHour', 'last8Hours', 'last24Hours', 'last7Days'`. Pro: 1 línea cambio + literal. Con: UI labels son '1h/8h/24h/7d' (compactos para tabs estrechas) — separar `id` (backend key) de `label` (UI display).
- **(b) Mapping function frontend** → `RANGE_TO_BACKEND = {'1h': 'lastHour', '8h': 'last8Hours', '24h': 'last24Hours', '7d': 'last7Days'}` aplicar en `_fetchSlotHistory` antes de XHR. Pro: preserva UI ids actuales. Con: una función más por mantener.
- **(c) Renombrar backend keys** → adoptar frontend ids. Pro: simpler UI. Con: backend ChiHistoryHelper compartido con SnlsHistoryHelper (heredado SanLuis) — divergir vocabulary entre módulos.

**Recommendation**: **Opción (b) mapping function**. Justificación:
- Reflow vocabulary backend (15 ranges camelCase) es el estándar implícito en Niagara nativo.
- MX60 backend ya usa el camelCase Niagara — preservarlo.
- Frontend ids cortos `'1h/8h/24h/7d'` son más legibles en HTML tabs/CSS — preservarlos.
- Mapping function es 1 const + 2 LOC en `_fetchSlotHistory`.

```js
// UpDetail.js — agregar en top-level after RANGES const
const RANGE_TO_BACKEND = {
  '1h': 'lastHour',
  '8h': 'last8Hours',
  '24h': 'last24Hours',
  '7d': 'last7Days'
};

// _fetchSlotHistory L552 modificar:
const backendRange = RANGE_TO_BACKEND[range] || 'lastHour';
const url = base + '?id=' + encodeURIComponent(histId) + '&range=' + encodeURIComponent(backendRange);
```

**Cross-validation**: Reflow no tiene este bug porque su frontend ChartToolBar/TimeRangePicker usan keys camelCase que SÍ matchean backend RangeCalculator (al menos los 13 que coinciden — los 2 missing del ChartToolBar caen al backend default which is `today`, no `lastHour`).

#### **R1.2 — Subir backend `maxPoints` cap MX60 + agregar style param**

**Problema**: ChiHistoryHelper.java:194 `maxPoints = 2000` HARDCODED truncate. Para `last7Days × 1pt/min = 10080 records` → backend devuelve solo primeros 2000 (records OLD — los más recientes se descartan). Frontend filtra cutoff = now - 7*24h → 0 puntos en ventana → chart vacío.

**Opciones**:
- **(a) Aumentar cap a 50000** alineado con webChart `niagara.webChart.maxSamplingSize` (per bloque #45). Para 7d × 1pt/min = 10080 fits. Para 30d × 1pt/min = 43200 fits. Para 1 año × 1pt/min = 525600 NO fits.
- **(b) Cap dinámico por range** → 1h:2000, 8h:5000, 24h:10000, 7d:50000, 30d:100000.
- **(c) Backend downsampling stride** análogo al frontend (en lugar de truncate, devolver `floor(N/r.points) * r.points` records distribuidos uniformemente).

**Recommendation**: **(c) backend downsampling stride** + **(a) raise cap a 50000 final cap**. Justificación:
- Truncate primer-2000 actual descarta records recientes (los que importan más para "qué está pasando ahora") — DEFECTO grave latente.
- Stride downsampling preserva temporal distribution.
- Cap final 50000 protege contra OOM en histories anómalas (30+ días al 1pt/sec).

```java
// ChiHistoryHelper.java:194 reemplazar:
int maxPoints = 50000;  // hard cap protect OOM

// Después del cursor count, hacer 2-pass o usar deque para keep-tail
// Alternativa simple: agregar ?points=N query param y muestrear stride = totalRecords / points
```

**Cross-validation**: Reflow NO tiene cap (cursor full) → para histories largas el problema es payload size, no truncation. WebChart nativo Niagara `autoSamplingSize=2500` con `maxSamplingSize=50000` — alineamos a esto.

#### **R1.3 — Agregar ranges que faltan al frontend MX60**

**Problema**: MX60 frontend tiene 4 tabs (1h/8h/24h/7d). Goal usuario "cualquier lapso de tiempo". Backend ya soporta 8 ranges. Faltan 4 tabs frontend: `today`, `yesterday`, `last30Days`, `monthToDate`.

**Recommendation**: agregar al `RANGES` array UpDetail.js:171-176 para feature parity backend:

```js
const RANGES = [
  { id: '1h',   label: '1h',  hours: 1,    points: 60,  step: 60 * 1000 },
  { id: '8h',   label: '8h',  hours: 8,    points: 96,  step: 5 * 60 * 1000 },
  { id: '24h',  label: '24h', hours: 24,   points: 96,  step: 15 * 60 * 1000 },
  { id: '7d',   label: '7d',  hours: 168,  points: 168, step: 60 * 60 * 1000 },
  // NUEVOS
  { id: 'today',       label: 'Hoy',  hoursDynamic: 'today',       points: 96,  step: 15 * 60 * 1000 },
  { id: 'yesterday',   label: 'Ayer', hoursDynamic: 'yesterday',   points: 96,  step: 15 * 60 * 1000 },
  { id: '30d',         label: '30d',  hours: 720,                   points: 240, step: 3 * 3600 * 1000 },
  { id: 'monthToDate', label: 'MTD',  hoursDynamic: 'monthToDate', points: 240, step: 3 * 3600 * 1000 }
];
```

**Cross-validation**: Reflow ChartToolBar tiene 13 ranges similares. Para HVAC 24/7 monitoring, "Hoy" + "Ayer" + "MTD" son use cases comunes (turnos diarios, reportes diarios, summaries fin de mes).

**Caveat**: `today/yesterday/monthToDate` requieren update a `filterHistoryByRange` L894-907 — `r.hours` no aplica directamente (es ventana fija dinámica). Necesita 2-line cambio (compute cutoff por id especial).

### **TIER 2 — Sprint-2** (mejoras que valen, no urgentes)

#### **R2.1 — Multi-history endpoint MX60** (1 round-trip por chart load)

Actualmente `_loadRealHistory` lanza N XHRs paralelos (uno por slot). Para UP con 11 slots = 11 round-trips. HTTP/2 mitiga pero ineficiente.

**Opción**: agregar al backend `GET /mx60/api/historyData` soporte multi-history via `?ids=histId1,histId2,...&range=...` (igual que Reflow `histories=A,B,C` comma-sep). Response: `{histId1: {...}, histId2: {...}}`.

Frontend `_loadRealHistory` colapsar `Promise.all(slots.map(_fetchSlotHistory))` a 1 request `_fetchAllHistories(histIds, range)`.

**Cross-validation**: Reflow lo hace en `HistoryDataResponse.java:48-52`. Patrón canónico.

#### **R2.2 — Live tail + boxcs subscription** (vs polling fallback REST 5s)

MX60 actualmente: EquipmentSnapshotStore single-subscribe (#71 #271) drives `_appendLiveSample`. Pero esto subscribe a equipment slots, NO a `BControlPoint.out` directamente — así que las muestras live son las del subscribe equipment poll, no del control point trend.

**Opción**: implementar BajaScript subscriber al control point ord (vía `findHistoryExtId` link source) con `PointSeries.subscribe()` callback `changed` → `_appendLiveSample`. Cross-ref bloque #45 (boxcs pattern) + bloque #69 #246 (subscriberMixin).

**Caveat**: requiere `bajaHeartbeat.start(baja)` activo (bloque #69 #249, #71 #272). Sprint-1 OBLIGATORIO según engram #1260.

#### **R2.3 — Compare mode (overlay período anterior)**

Reflow tiene CompareRangeCalculator. Útil HVAC para "comparar performance hoy vs ayer en mismo horario".

**Opción**: portar `CompareRangeCalculator.java:7-316` a `ChiHistoryHelper.computeCompareRange(name)` + frontend toggle `data-compare` button + segunda fetch `_fetchSlotHistory(histId, range, true)` con `compare=true` query param.

**Cross-validation**: MX60 ya tiene `baselineParallel` UpDetail.js:912-935 (binary-search 24h shift) — fully client-side. Compare mode es generalización (cualquier range previo, no solo 24h).

#### **R2.4 — Export CSV/PNG**

Sprint-2+. Reflow lo hace via D3chart export. Chart.js v4 tiene `toBase64Image()` para PNG nativo. CSV requiere helper que serialize `fullHistory[range]` → CSV string + blob download.

### **TIER 3 — DEFER (sprint-3+ o nunca)**

#### **R3.1 — D3 migration** — NO migrar. Chart.js suficiente HVAC sub-day. D3chart 3114L over-engineered.

#### **R3.2 — NDJSON streaming** — defer. Reflow custom no lo hace (mock + sin chunked). webChart nativo sí — para MX60 implementar requiere refactor servlet a `Transfer-Encoding: chunked` + JSON parser frontend incremental. Para 50K+ records sí valdría — para HVAC sub-day usual no.

#### **R3.3 — Bucketing/rollup server-side** (BHistoryRollup pre-computado) — defer hasta que aparezca un dashboard requiriendo 1+ año vista.

#### **R3.4 — Disk cache GZIP** Reflow — defer. MX60 station tiene N pequeño histories (250 max estimated), cache miss tolerable. Si crece, considerar.

---

## §73.6 — Root cause MX60 1-hour limit (con cita file:line)

### Causa principal: vocabulary mismatch frontend↔backend

**Frontend** (UpDetail.js:171-176):
```js
const RANGES = [
  { id: '1h',  label: '1h',  hours: 1,   points: 60,  step: 60 * 1000 },
  { id: '8h',  label: '8h',  hours: 8,   points: 96,  step: 5 * 60 * 1000 },
  { id: '24h', label: '24h', hours: 24,  points: 96,  step: 15 * 60 * 1000 },
  { id: '7d',  label: '7d',  hours: 168, points: 168, step: 60 * 60 * 1000 }
];
```

**Frontend pasa range** (UpDetail.js:552):
```js
const url = base + '?id=' + encodeURIComponent(histId) + '&range=' + encodeURIComponent(range);
```

**Backend recibe range** (ChiHistoryHelper.java:257-318):
```java
public static BAbsTime[] computeRange(String name) {
  if (name == null) name = "lastHour";
  Calendar cal = Calendar.getInstance();
  Date end = cal.getTime();

  if ("last8Hours".equals(name))   { cal.add(Calendar.HOUR_OF_DAY, -8); }       // '8h'  !== 'last8Hours'   ✗
  else if ("today".equals(name))   { cal.set(Calendar.HOUR_OF_DAY, 0); ... }    // '24h' !== 'today'        ✗
  else if ("last24Hours".equals(name))  { cal.add(Calendar.HOUR_OF_DAY, -24); } // '24h' !== 'last24Hours'  ✗
  else if ("yesterday".equals(name))    { ... }
  else if ("last7Days".equals(name))    { cal.add(Calendar.DAY_OF_MONTH, -7); } // '7d'  !== 'last7Days'    ✗
  else if ("last30Days".equals(name))   { ... }
  else if ("monthToDate".equals(name))  { ... }
  else {                                                                         // ← TODOS los tabs caen aquí
    cal.add(Calendar.HOUR_OF_DAY, -1);  // ← lastHour silencioso siempre
  }
  ...
}
```

**Resultado para cada tab**:
- Click `1h` → frontend pasa `range='1h'` → backend `else default lastHour` → **1h data** ✓ (acierta por coincidencia)
- Click `8h` → frontend pasa `range='8h'` → backend `else default lastHour` → **1h data** ✗ (parece 8h pero solo muestra 1h)
- Click `24h` → frontend pasa `range='24h'` → backend `else default lastHour` → **1h data** ✗
- Click `7d` → frontend pasa `range='7d'` → backend `else default lastHour` → **1h data** ✗

Frontend después aplica `filterHistoryByRange(fullHistory, '7d')` con `cutoff = now - 7*24*3600000` → solo los 60 puntos del 1h backend están en la ventana → chart visual ve 1h de data en eje extendido a 7 días → línea corta al final del eje.

### Causa secundaria: backend cap maxPoints=2000 truncate (no downsample)

**ChiHistoryHelper.java:194-212**:
```java
int maxPoints = 2000;

while (cursor.next() && count < maxPoints) {
  BHistoryRecord rec = (BHistoryRecord) cursor.get();
  long ts = rec.getTimestamp().getMillis();
  String val = extractValue(rec);
  ...
  count++;
}
```

Para `last7Days × 1pt/min` (asumiendo BD HVAC típica): 7 × 24 × 60 = **10080 records**. Backend devuelve **primeros 2000** (records más OLD según cursor order Niagara — típicamente cronológico ascendente). Los 8080 records nuevos se descartan.

Aún si R1.1 (vocabulary fix) se aplica solo, el efecto sería:
- Click `7d` con vocabulary fix → backend recibe `last7Days` → cursor abre ventana 7d → cursor.next() retorna primeros 2000 → response devuelve los 2000 más OLD del 7d ago.
- Frontend cutoff = now - 7*24h → todos en ventana → chart muestra 7d con 2000 puntos sampleados a 168 client-side.
- **Pero gap visible al final** (records de las últimas N horas faltan porque cap se llenó antes).

R1.2 (raise cap + downsampling stride) elimina esto.

### Causa terciaria (latente): HISTORY_MINUTES cap fullHistory frontend

**UpDetail.js:388-396**:
```js
// P5 fix: cap reduced from 7d (10080 entries × ~80 bytes = ~854 KB/UP) to 24h
// (1440 entries = ~115 KB/UP). The 7d range tab fetches fresh data from the
// history backend on demand instead of accumulating in client memory.
const HISTORY_MINUTES = 24 * 60;
```

Cuando user clickea `7d` y backend devuelve correctamente 7d data (post R1.1 + R1.2), frontend hace `fullHistory.length = 0; Array.prototype.push.apply(fullHistory, result.rows);` (UpDetail.js:3771-3773). Si rows.length > 1440, se reemplaza ese array sin cap inicialmente. Pero next live notify tick → push → eventually exceeds 1440 → Array.shift() loop empieza.

Comment menciona: "REST fallback (5s poll), an 8-hour shift accumulates 5760 entries — well over the new cap, so Array.shift() runs more frequently but with a smaller array (less expensive O(N) per shift)". Pero para 7d range tab con 10080 records, post-load Array.shift() se ejecutará repetidamente trim records OLD primero (8640 shifts en sucesión).

**Sprint-1 candidate fix**: cap dinámico HISTORY_MINUTES por range activo:
```js
function _maxHistoryEntries() {
  const r = RANGES.find(x => x.id === _currentHistoryRange) || RANGES[0];
  return Math.max(1440, r.points * 1.5);  // headroom 50% para baseline + live append
}
```

---

## §73.7 — Decisiones MX60 sprint-1 history domain

### **HEREDAR de Reflow** (qué tomar literal)

- **Backend ranges vocabulary camelCase**: `lastHour, last8Hours, last24Hours, last7Days` etc. — adoptar como contract frontend↔backend (ya lo tiene MX60 backend). Mapping function frontend (R1.1).
- **Multi-history endpoint comma-separated** para sprint-2 R2.1 — patrón `histories=A,B,C` from `HistoryDataResponse.java:48-52`.
- **`style=apex` tuple format `[ts, val]`** ya es el default MX60 — confirmar que es el diseño correcto (rico format object es overkill HVAC).
- **CompareRangeCalculator** algoritmo (sprint-2 R2.3) — código portable directo a `ChiHistoryHelper.computeCompareRange(name)`.

### **DESCARTAR de Reflow** (qué NO heredar)

- **D3chart engine 3114L** — Chart.js MX60 es suficiente. Sprint-3 review si crece feature gap.
- **2 BUGS LATENTES historyCache.js** (#263 last24Hours operator precedence + #264 skip-cache NEVER fires) — NO heredar al MX60 (frontend MX60 no tiene Vuex de history; usa IIFEs). Lección general: si MX60 alguna vez agrega Vuex/Pinia de history cache, evitar replicar estos bugs.
- **TimeRangePicker fallback con 6 keys NO existentes en backend** (last15, last4Hours, last3Days, last2Weeks, last3Months, last6Months) — NO replicar. Si MX60 agrega ranges, sincronizar backend+frontend atómico.
- **SimpleDateFormat+DecimalFormat static** (HistoryDataResponse.java:39-40 igual a HistoryData.java:49-50 #256) — NO heredar al copiar el patrón. Usar `String.format` per-call (como hace MX60 ChiHistoryHelper extractValue) o `DateTimeFormatter` immutable.
- **NO downsampling Reflow backend** — MX60 ya tiene ventaja con stride client-side, agregar también stride backend (R1.2).

### **PRESERVAR de MX60** (qué mantener intacto)

- **Chart.js v4 line type** — engine elegido apropiado HVAC.
- **`MX60.HistoryIndex` + `MX60.HistoryListCache` IIFE** — patrón cache singleton apropiado.
- **`detectEquipmentHistories` auto-resolution via link graph** + name fallback — superior a Reflow manual `HistoryGroups` (Reflow obliga al usuario configurar grupos en station; MX60 auto-detecta).
- **`_appendLiveSample` O(1) per notify** + 4 SAFETY NETS — patrón superior al Reflow polling per-chart.
- **RAF stagger rebuild** + canvas pre-cache — fix concreto Forced reflow violations.
- **`requestIdleCallback` initial history defer** — UX superior (3D scene paint primero).
- **`comfortBandPlugin` + `htmlLegendPlugin`** — domain-specific HVAC plugins, no en Reflow.

### **INVENTAR nuevo** (no presente en ninguno)

- **`RANGE_TO_BACKEND` mapping function** (R1.1) — único concepto nuevo necesario.
- **Backend cap dinámico + stride downsampling** (R1.2) — no presente Reflow ni MX60 actual.
- **Cap dinámico HISTORY_MINUTES por range activo** (§73.6 causa terciaria) — fix latente.

### **4 cosas sprint-1 OBLIGATORIAS** (pre-deploy)

1. **R1.1 vocabulary mapping** — sin esto, los 4 tabs MX60 son fake (todos devuelven 1h). 1 const + 2 LOC change.
2. **R1.2 backend cap + downsampling stride** — sin esto, last7Days+ devuelve gap final (truncate primeros 2000 = los más OLD).
3. **R1.3 agregar 4 ranges nuevos** (today/yesterday/30d/monthToDate) — feature parity backend MX60 ya disponible, frontend solo expone 4 de 8.
4. **§73.6 causa terciaria HISTORY_MINUTES cap dinámico** — sin esto, post-vocabulary-fix los 7d data trigga Array.shift() loop massive.

---

## §73.8 — Implications NUEVAS #299..#312

| # | Implication | Source empírico |
|---|-------------|-----------------|
| #299 | **MX60 frontend RANGES ids `'1h/8h/24h/7d'` NO matchean backend `lastHour/last8Hours/last24Hours/last7Days`** — backend cae al `else default lastHour` para los 4 tabs. ROOT CAUSE 1-hour effective limit. | UpDetail.js:171-176 + ChiHistoryHelper.java:257-318 + UpDetail.js:552 |
| #300 | **MX60 backend `maxPoints = 2000` HARDCODED** truncate (NO downsampling) — descarta records nuevos cuando count == 2000. Para last7Days × 1pt/min = 10080 records → response devuelve solo primeros 2000 (los más OLD). | ChiHistoryHelper.java:194 |
| #301 | **MX60 Reflow comparten anti-pattern thread-safety**: SimpleDateFormat + DecimalFormat static fields (HistoryDataResponse.java:39-40 = HistoryData.java:49-50 = bloque #70 #256). MX60 ChiHistoryHelper L235-251 SIN este bug — patrón correcto inline `String.format`. | HistoryDataResponse.java:39-40 + ChiHistoryHelper.java:235-251 |
| #302 | **Reflow tiene 3 vocabularios divergentes time range** internos: backend RangeCalculator (15) ≠ ChartToolBar dropdown (13) ≠ TimeRangePicker fallback (15 con 6 keys NO existentes en backend). Salvado por `BDateRangeEnum.DEFAULT.getRange().isTag(range)` check fallback → today. | RangeCalculator.java:8-56 + ChartToolBar.vue:38-180 + TimeRangePicker.vue:62-66 + HistoryDataResponse.java:66-70 |
| #303 | **Reflow ChartDataResponse + DataResponse ambos full-load JSONArray (NO chunked, NO streaming)** — para 50K+ records puede causar OOM. webChart nativo Niagara SÍ es NDJSON chunked (bloque #45) — Reflow custom NO replica. | HistoryDataResponse.java:79 + HistoryChartDataResponse.java:23 |
| #304 | **MX60 NO multi-history endpoint** — single-history per XHR. UP con 11 slots = 11 XHRs paralelos. Mitigado HTTP/2 multiplexing. Reflow tiene multi-history `histories=A,B,C` comma-sep — patrón canónico. | ChiHistoryHelper.java:132-229 + HistoryDataResponse.java:48-52 |
| #305 | **MX60 client-side stride downsampling SUPERIOR a Reflow** — `filterHistoryByRange` UpDetail.js:894-907 `stride = max(1, floor(filtered.length / r.points))`. Reflow no tiene. Es el patrón que MX60 debe preservar. | UpDetail.js:894-907 |
| #306 | **MX60 EquipmentSnapshotStore RAF 500ms throttle drives `_appendLiveSample` SUPERIOR a Reflow polling per-chart 300s** — single subscription drives N charts. O(1) per notify. | UpDetail.js:3329-3377 + HistoryChart.vue:304-309 |
| #307 | **MX60 RAF stagger rebuild + canvas pre-cache** elimina 5+ Forced reflow violations DevTools (~2s layout thrashing) — patrón concreto que Reflow no implementa por usar D3 (diferente engine). | UpDetail.js:3264-3272 + UpDetail.js:3692-3709 |
| #308 | **Reflow Compare mode = `CompareRangeCalculator` espejo shifted + `'|compare|'` separator + `getCompareRange(now, range)`** — código portable directo a MX60 sprint-2. | CompareRangeCalculator.java:7-316 + historyCache.js:74-78 + historyCache.js:132 |
| #309 | **MX60 detectEquipmentHistories auto-resolution via link graph + name fallback** SUPERIOR a Reflow `HistoryGroups` manual. `findHistoryExtIdRecursive` MAX_HISTORY_SEARCH_DEPTH = 3 cubre 3 patrones (direct + extensions/ + custom folder kitControl Ramp). | ChiHistoryHelper.java:338-528 + HistoryGroups.java |
| #310 | **MX60 `requestIdleCallback` initial history defer 300ms** UX superior — 3D scene + shell paint primero, history fetch en idle background. Reflow no tiene equivalente. | UpDetail.js:3780-3806 |
| #311 | **MX60 HISTORY_MINUTES = 24*60 cap fullHistory** in-memory + Array.shift() loop bajo REST fallback 5s × 8h = 5760 entries (per #71 #279). Cap dinámico por range activo es sprint-1 candidate fix. | UpDetail.js:388-396 + UpDetail.js:3771-3773 |
| #312 | **MX60 frontend solo expone 4 de los 8 ranges del backend** (1h/8h/24h/7d frontend vs lastHour/last8Hours/today/last24Hours/yesterday/last7Days/last30Days/monthToDate backend). Faltan today/yesterday/30d/monthToDate frontend — feature gap sprint-1. | UpDetail.js:171-176 + ChiHistoryHelper.java:257-318 |

**TALLY GLOBAL post-Bloque 73**:
- 96 antipatterns AP-1..96 (sin nuevos en este bloque — los hallazgos son implications + bugs concretos, no patterns)
- 42 reglas template MX60 (1-42)
- **312 implications #1..#312**
- Capa 19 EXTENDIDA con audit empírico TIER-1 History+Data triple-source

---

## §73.9 — Cross-refs

### Bloques referenciados

- **#45** (engram #892) — **VIGENTE**: WebChart nativo NDJSON + 12 BWebChartTimeRangeType + autoSamplingSize 2500 + maxSamplingSize 50000 + bucketing simple. Inspiración para R1.2 cap MX60 backend.
- **#62** §62.9.3 polling componentes — confirma patrón polling distribuido (paralelo HistoryChart.vue:304-309).
- **#68** (engram #?) — **§68.1 backend HEREDA 95% vigente con corrigendum #70**: HistoryData split 2 clases (Engine+Serializer) o 3 alternativo. **§68.2 frontend Vue 3 REFUTED** post engram #1257. **#234 D3chart global app.component()** — preserva si Reflow se hereda a Vue 3 sprint futuro.
- **#69** (engram #?) — **VIGENTE**: #245 history charts polling guard ≥30s default 300s (HistoryChart.vue:304-309 confirmé). #249 bajaHeartbeat DORMANT — sprint-1 OBLIGATORIO start(baja). #252 HistoryGhostSubscriber one-shot detection NO keepalive.
- **#70** (engram #1242) — **VIGENTE + RECONFIRMADO empíricamente aquí**: #254 HistoryData 100% static + 1 inner Runnable. #256 SimpleDateFormat+DecimalFormat thread-safety bug confirmado en HistoryDataResponse.java:39-40 también. #262 historyCache shape post-S56 confirmado. #263 `last24Hours` operator precedence (24 días) confirmado L57. #264 skip-cache NEVER fires confirmado L489.
- **#71** (engram #1260) — **VIGENTE**: Equipment domain triple-source methodology aplicada idéntica aquí. #279 HISTORY_MINUTES cap UpDetail.js:396 confuso bajo REST fallback 5s = 5760 entries — sprint-1 candidate fix dynamic cap por range activo.

### Engrams methodology

- **#1259** triple-source-reflow-mx60-bloques — APLICADO Stage 0+1+2+3+4 framework.
- **#1258** reflow-mx60-bilateral-critique — APLICADO veredictos no sesgados.
- **#1238** clean-room-disconnected-asymmetry — APLICADO confirmar Reflow polling NO boxcs (clean-room mock backend).
- **#1236** mapping-vs-empirical-audit — RECONFIRMADO: lo que el mapping decía sobre time ranges NO match con lo empírico (3 vocabularios divergentes Reflow + 1 mismatch frontend↔backend MX60). Audit empírico siempre gana.
- **#1257** mx60-stack-pivot-iife — RECONFIRMADO: UpDetail.js es ES module hybrid (3841 LOC) + IIFE classic resto.

### NO referenciados (out of scope)

- #44 alarms console — flow paralelo, bloque #72 alarms triple-source independiente.
- #46 writes priority array — read-only domain history aquí.
- #47-49 SPA bootstrap+CSRF+i18n — backend independent.

---

## §73.10 — PARA EL YO 2027

Cuando arranques **MX60 history sprint-1 backend Equipment domain charts**, abrí este bloque #73 **PRIMERO** antes de tocar ChiHistoryHelper.java o UpDetail.js. Las cosas críticas:

### Las 4 cosas sprint-1 OBLIGATORIAS (pre-deploy)

1. **R1.1 vocabulary mapping** — `RANGE_TO_BACKEND` const + 2 LOC change `_fetchSlotHistory`. **SIN ESTO, los 4 tabs son fake** (#299).
2. **R1.2 backend cap dinámico + stride downsampling** — reemplazar `maxPoints=2000 HARDCODED` por mecanismo stride. **SIN ESTO, 7d devuelve gap final** (#300).
3. **R1.3 agregar 4 ranges frontend** (today/yesterday/30d/monthToDate) — feature parity backend (#312).
4. **§73.6 causa terciaria HISTORY_MINUTES cap dinámico** — sin esto, post-vocabulary-fix 7d trigger Array.shift() loop massive.

### Las 3 trampas a evitar

- **NO heredar SimpleDateFormat+DecimalFormat static de Reflow** (#301) — usar inline `String.format` per-call (como ya hace ChiHistoryHelper.extractValue).
- **NO agregar Vuex/Pinia historyCache replicando Reflow** sin auditar bugs latentes #263 y #264 (operator precedence + skip-cache NEVER fires) — los IIFEs MX60 son simpler y thread-safe (frontend single-thread, pero estructura clean).
- **NO migrar Chart.js → D3** — D3chart 3114L es over-engineered HVAC. Sprint-3 review feature gaps si aparecen.

### Las 4 ventajas MX60 a preservar

- `MX60.HistoryIndex` + `MX60.HistoryListCache` IIFE cache singleton (mejor que Reflow Vuex con bugs).
- `detectEquipmentHistories` auto-resolution link graph (mejor que Reflow manual HistoryGroups).
- `_appendLiveSample` O(1) per notify + 4 SAFETY NETS (mejor que Reflow polling per-chart 300s).
- RAF stagger rebuild + canvas pre-cache + requestIdleCallback initial defer (mejor que cualquier Reflow performance pattern).

### Sprint-2 candidates si crece scope

- **R2.1** multi-history endpoint (1 round-trip vs 11 paralelos) — código en HistoryDataResponse.java:48-52 portable.
- **R2.2** boxcs subscription tail BControlPoint.out — requiere bajaHeartbeat.start(baja) sprint-1 (engram #1260 #272).
- **R2.3** Compare mode — port CompareRangeCalculator.java directo + frontend toggle.
- **R2.4** Export CSV/PNG — Chart.js v4 `toBase64Image()` PNG nativo, CSV helper trivial.

### Si el usuario reporta "no veo data en lapsos largos"

1. Validar R1.1 aplicado (vocabulary mapping) — `range='7d'` debería convertirse a `'last7Days'` antes del XHR.
2. Validar R1.2 aplicado (cap raised) — `maxPoints` fijo en 50000 o stride sampling activo.
3. Verificar history Niagara station tiene records para el range pedido (BD retention policy).
4. Verificar `BHistoryExt` enabled en el control point (findHistoryExtIdRecursive checa `histExt.getEnabled()`).
5. Si UI muestra "Recolectando datos en vivo..." overlay → backend NO encontró histMap para ese equip → revisar `detectEquipmentHistories` link graph + name fallback (UP_HISTORY_PROPERTIES list cubre el slot pedido?).

---

**Bloque 73 cerrado** — audit empírico TIER-1 History+Data triple-source completo. Root cause MX60 1-hour limit identificado (#299). 14 implications nuevas (#299..#312). 4 acciones sprint-1 OBLIGATORIAS (R1.1+R1.2+R1.3+§73.6 causa terciaria). 4 acciones sprint-2 candidate (R2.1..R2.4). Capa 19 EXTENDIDA. 312 implications acumuladas.
