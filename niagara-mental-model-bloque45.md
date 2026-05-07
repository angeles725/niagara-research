# Niagara N4 — Mental Model · Bloque 45

**Tema**: History/Trend chart consumption desde SPA externa — WebChart 3 servlets + SeriesTransform pipeline + query params real + streaming chunked + real-time tail + multi-series sync + render performance + system.properties webChart

**Fecha**: 2026-05-04
**Método**: Investigación empírica READ-ONLY. Decompilación de `webChart-rt.jar` + `webChart-ux.jar` + `seriesTransform-rt.jar` + `seriesTransform-ux.jar` con `javap -p -c`. Lectura fuente JS legible en `rc/model/` (NO minificado — los `.js` individuales en `webChart-ux.jar:rc/` son fuente completa con comentarios JSDoc). Contrastado con `niagara-help/devguide-clean/history.txt`, `seriesTransforms/*.txt`, `defaults/system.properties`. Bloques referenciados: 29 (WebChart 3 mencionados sin profundizar), 31 (history archive blocking), 33 (`.hdb` format real + HistorySpaceConnection), 21 (BQL), 47 (bootstrap headless restrictions).

**JARs primarios**: `webChart-rt.jar` (8 clases + WEB-INF/web.xml), `webChart-ux.jar` (65 JS + 3 classes), `seriesTransform-rt.jar` (39 clases), `seriesTransform-ux.jar` (3 JS). Todos CONFIRMADOS presentes en `OptimizerSupervisor-N4.14.0.162/modules/`.

**Conecta con**: Bloque 8.2 (BHistoryExt basics), Bloque 21 (BQL sobre history), Bloque 29.4.3 (tabla servlets mencionó WebChart 3 sin profundizar — ESTE BLOQUE profundiza), Bloque 31.7 (history archive blocking), Bloque 33 (`.hdb` binary + HistorySpaceConnection + rollup + BHistoryConfig), Bloque 36 (BOX lifecycle + lease 10s), Bloque 42 (Subscriber lifecycle — tail via PointSeries), Bloque 47 (bootstrap headless + CORS + WebSocket hardcode).

---

## 45.0 Contexto — por qué este bloque

Bloque 29.4.3 inventarió los 3 WebChart servlets + SeriesTransform en la tabla de 53 servlets, sin profundizar en sus endpoints concretos ni en cómo una SPA externa los consume. Bloque 33 documentó el `.hdb` format y las APIs Java. Este bloque cierra el gap SPA-side:

**Pregunta unificadora**: Mi SPA custom (React/Vue) en `dashboard.sejofa.com` necesita renderizar charts de tendencia con datos de 100K+ puntos, en tiempo real. ¿Cuáles son los endpoints concretos, el wire format, el patrón real-time tail, los límites del sistema, y cómo resuelvo multi-series sync?

**Prerequisito**: La SPA ya estableció sesión (Bloque 47) y tiene `JSESSIONID` + `csrfToken` válidos. Todas las llamadas aquí incluyen esos headers. CORS no es problema porque la SPA usa reverse proxy co-locado (Bloque 47.2.4) o vive en el módulo Niagara (Bloque 47.6.1).

---

## 45.1 Storage formats — `.hdb` y configuración por history

### 45.1.1 `.hdb` — binario propietario Tridium (CONFIRMADO Bloque 33)

```
Header 12 bytes:
  MAGIC:       0xA0F61E5E (bytes en disco: 5E 1E F6 A0 big-endian)
  VERSION:     int (VERSION_1 = fixed-length | VERSION_2 = recstore paginated)
  DATA_OFFSET: int (default 12)
```

**NO es SQLite.** La confusión del Bloque 31 vino de asumir VACUUM = SQLite. El blocking de 5-30 min en compaction es por rename exclusivo del archivo (Windows file lock) cuando cursores activos mantienen el `.hdb` open.

Path en disco:
```
<stationHome>/history/<deviceName>/<historyName>.hdb
```

Ejemplo supervisor:
```
/Niagara4.14/OptimizerSupervisor/stations/<station>/history/$/audit.hdb
/Niagara4.14/OptimizerSupervisor/stations/<station>/history/Jace1/Floor1_Temp.hdb
```

El archivo es self-describing: la `BHistoryConfig` está embebida en bytes 12..dataOffset vía `ValueDocEncoder`. Se puede leer sin acceso al station original.

### 45.1.2 BHistoryConfig slots relevantes para SPA

| Slot | Tipo | Valor | Relevancia para chart |
|------|------|-------|----------------------|
| `timeZone` | `BTimeZone` | Defecto: TZ de la station | **CRÍTICO** — timestamps en `.hdb` son `BAbsTime` (UTC-based), display se convierte a esta TZ. Si Supervisor tiene TZ distinta al origin, charts muestran timestamp correcto pero export CSV usa TZ local del Supervisor |
| `capacity` | `BCapacity` | Default: 500 records | Determina ventana disponible localmente antes de ir al archive provider |
| `fullPolicy` | `BFullPolicy` | `ROLL` (default) o `STOP` | Con ROLL: oldest records se borran. El chart puede ver "corte" histórico en el punto de rollover |
| `storageType` | `BStorageType` | `FILE` (único valor existente) | NO hay "memory" ni "remote". Siempre `.hdb` |

**Gotcha TZ (cross-ref 33.8.7)**: Si el origin station (JACE, UTC-5) y el Supervisor (UTC-6) tienen TZ distintas, los registros se almacenan en `BAbsTime` UTC. El chart los muestra correctamente en la TZ configurada en la query o del consumer. El CSV export del Supervisor usa la TZ del historyConfig LOCAL del Supervisor — puede no coincidir con el origin. Para una SPA, siempre especificar TZ explícita en la query o normalizar a UTC en el cliente.

### 45.1.3 Capacity vs Archive — cuántos datos están disponibles

```
query.start < firstLocalTimestamp?
  ↓ YES: ir a BArchiveHistoryProvider (si configurado)
         → maxArchiveResultsPerQuery cap (default 10,000 records)
         → HybridHistoryCursor merge local + archive
  ↓ NO:  solo local .hdb
         → hasta capacity records disponibles sin cap adicional
         → si capacity=500 roll → solo últimos 500 registros locales
```

**Gotcha para SPA chart**: una historia con capacity=500 ROLL en un JACE → al Supervisor llegan (via import) registros más recientes. El Supervisor tiene su propia capacity. Si el Supervisor tiene capacity=250,000, el chart puede ver 250K registros del Supervisor. Si el Supervisor no tiene archive provider, queries más viejas de esa ventana → vacías. **No es un error visible** — `timeQuery` devuelve empty cursor para timestamps fuera de la ventana retenida.

---

## 45.2 WebChart 3 servlets — endpoints reales (PROFUNDIZACIÓN Bloque 29)

### 45.2.1 Inventario confirmado

Los 3 servlets del módulo `webChart-rt.jar` (8 clases totales):

| Servlet | URL Path | Método HTTP | Auth | Content-Type respuesta |
|---------|----------|-------------|------|------------------------|
| `WebChartQueryServlet` | `/webChart/data/{ord-escaped}` | GET | Sí | `application/json` (o `text/plain` para schedule) |
| `WebChartFileServlet` | `/webChartFile/*` | POST (save) / GET (load) | Sí | JSON para load, vacío para save |
| SeriesTransform servlet | `/seriesTransform/data/{ord-escaped}` | GET | Sí | `application/json` |

**Hallazgo crítico**: el Bloque 29 listó `/webChart/*` como POST. **INCORRECTO**. `WebChartQueryServlet` expone `doGet(HttpServletRequest, HttpServletResponse)` — es **GET**. Confirmado por decompilación `javap -p com/tridium/webChart/WebChartQueryServlet.class`:
```java
protected void doGet(HttpServletRequest, HttpServletResponse) throws ...
```
`WebChartFileServlet` sí expone `doPost` (para save) pero el GET de datos es via `WebChartQueryServlet`.

### 45.2.2 WebChartQueryServlet — URL patterns internos

URL patterns hardcoded (CONFIRMADO `javap -c` bytecodes `ldc` strings):

```java
private static final Pattern dataPattern     = Pattern.compile("/data/.+");
private static final Pattern schedulePattern = Pattern.compile("/schedule/.+");
private static final Pattern boxTablePattern = Pattern.compile("/boxTable/.+");
```

Y desde el JS en `ServletSeries.js` (fuente legible, confirmado):
```javascript
var dataUri = "/webChart/query/data/" + baja.SlotPath.escape(fullOrd);
```

Combinando: la URL real del endpoint de datos de history es:
```
GET /webChart/data/{ORD-slot-escaped}
```

El ORD incluye los parámetros de query como parte del ORD string (no como query params HTTP separados). Ejemplo ORD completo que llega en la URL:
```
/webChart/data/history:%24%2FFloor1_Temp?start=13,2024-01-01T00:00:00Z;end=13,2024-01-02T00:00:00Z
```

Donde `%24` = `$` (shorthand de la station local), `?` es el delimiter que separa el ORD base de los query params del ORD Niagara (NO query string HTTP). Los parámetros se parsean en `HistoryQuery`.

### 45.2.3 Parámetros del ORD history

Desde `HistoryQuery` (decompilado Bloque 33.2.2) y `modelUtil.getFullOrd()` (JS fuente legible):

| Param ORD | Tipo | Ejemplo | Rol |
|-----------|------|---------|-----|
| `start=` | `BAbsTime` encoded | `13,2024-01-01T00:00:00Z` | Inicio rango. El `13,` es el type ordinal de BAbsTime |
| `end=` | `BAbsTime` encoded | `13,2024-01-31T23:59:59Z` | Fin rango |
| `period=` | String enum tag | `today`, `yesterday`, `last7Days`, `last24Hours`, `monthToDate`, `lastMonth`, `yearToDate`, `lastYear`, `weekToDate`, `lastWeek`, `auto`, `timeRange` | Si period != `timeRange` (ordinal > 1), server calcula start/end internamente según TZ de la historia |
| `delta=true` | Boolean | `delta=true` | Solo registros nuevos desde último sync timestamp (DeltaQuery — usado por import incremental) |
| `excludeArchiveData=true` | Boolean | `excludeArchiveData=true` | NO consultar archive providers, solo local `.hdb` |
| `archiveQueryLimit=N` | Int | `archiveQueryLimit=5000` | Override `maxArchiveResultsPerQuery` (default 10,000) |

Separadores en el ORD: `?` inicia la query section, `;` separa múltiples params:
```
history:$/Floor1_Temp?start=13,2024-01-01T00:00:00Z;end=13,2024-01-31T23:59:59Z
```

**Gotcha Accept header**: `WebChartQueryServlet.doGet()` verifica `RestUtil.acceptJson()` y además chequea que el `Accept` header custom sea `webChart` version `1`. CONFIRMADO en bytecode:
```java
ldc #16 // String webChart
// ... check Accept: application/json; protocol=webChart; version=1
```
Sin el Accept header correcto → 406. El JS de webChart-ux lo envía automáticamente. Si llamas desde SPA custom, debes enviarlo:
```
Accept: application/json; protocol=webChart; version=1
```

### 45.2.4 Response format — NDJSON chunked

Response: `Content-Type: application/json`, `Transfer-Encoding: chunked`, `Cache-Control: no-cache`.

Formato: **NDJSON** (Newline-Delimited JSON) — un JSON object por línea. NO es un array JSON. CONFIRMADO en `chunkUtil.ajax()`:
```javascript
data.split("\n").forEach(chunk => {
  const obj = JSON.parse(chunk);  // cada línea es un JSON object
  series.preparePoint(obj);
});
```

Cada record JSON tiene campos comprimidos (minified field names). CONFIRMADO en `WebChartUtil.encodeMinifiedHistoryRecord()` y `modelUtil.prepareServletPoint()`:

```json
{ "t": "13,2024-01-15T10:00:00Z", "v": 72.5, "s": 0, "r": 0 }
```

| Campo | Nombre largo | Tipo | Notas |
|-------|-------------|------|-------|
| `t` | timestamp | `BAbsTime` encoded string | Siempre presente |
| `v` | value | Number/boolean/String | El valor del punto |
| `s` | status | int (BStatus bits) | 0 = ok, otros bits = fault/alarm/etc |
| `r` | trendFlags | int (BTrendFlags bits) | 0 = normal, otros = startTrend/outOfOrder/hidden/modified/interpolated |
| `w` | warning | String | Si presente (key `w`), es un warning del server, no un datapoint |

**Gotcha**: si el campo `w` está presente en el chunk, el objeto es un warning y debe ignorarse como data point. El cliente JS lo maneja con:
```javascript
if (chunk.hasOwnProperty('w')) {
  series.addWarning(chunk.w);
} else {
  series.preparePoint(chunk);
}
```

**Gotcha**: el campo `v` puede ser `null` para records con status fault/disabled. Parsear sin null check → NaN en el chart. Los `r` (trendFlags) bits determinan si el punto debe ser "skipped" (gap visual en el chart):
- `startTrend` → primer record tras un gap → insert skip point antes
- `outOfOrder` → timestamp < timestamp previo (ojo: `.hdb` acepta out-of-order)

### 45.2.5 WebChartFileServlet — chart file management

`POST /webChartFile/save?chartName={name}` — guarda configuración de chart (serie ORDs, colores, rangos) como archivo `.chart` en `!station/config/charts/`:
```
<stationHome>/config/charts/^{name}.chart  (el ^ = shorthand de station local)
```

Requiere `operatorWrite` permission en el filesystem (CONFIRMADO bytecode `hasOperatorWrite()` check → 403 si falla).

El `.chart` file es JSON serializado vía `QuickJSONWriter` de Tridium. Los ORDs dentro se almacenan relativos a la station.

`GET /webChartFile/load?chartName={name}` — carga un `.chart` file. Retorna JSON con la config.

Pattern anti-traversal: `chartNameFilter = Pattern.compile("[|]|([.][.])") ` — bloquea `|` y `..` en el name para prevenir path traversal. CONFIRMADO bytecode `ldc "[|]|([.][.])"`.

**Relevancia para SPA**: El `.chart` file system es útil para persistir configuraciones de dashboard. Una SPA puede listar charts disponibles y cargarlos. NO es una API estable documentada — es uso interno del WebChart widget.

### 45.2.6 SeriesTransformWebChartQueryServlet — transform pipeline

URL pattern: `/seriesTransform/data/{ORD-escaped}` (CONFIRMADO decompilado, pattern `Pattern.compile("/data/.+")`).

Response: mismo formato NDJSON chunked que WebChartQueryServlet. Campos idénticos `{t, v, s}`.

El ORD apunta a un `BTransformGraph` component, NO directamente a un history. El `BTransformGraph` es un nodo compositor que encadena:

```
BHistoryInput → [Cleanser → Scale → Filter → Aggregate/Rollup → Terminal] → output series
```

Clases confirmadas en `seriesTransform-rt.jar`:

| Clase | Rol |
|-------|-----|
| `BTransformGraph` | Raíz del grafo. Resuelve el pipeline a `BSeriesTransformTable[]` |
| `BCleanserNode` | Limpieza: detectores (`BNanDetector`) + reemplazadores (`BReplaceWithZero`, `BReplaceWithLastGood`, `BReplaceWithNextGood`, `BReplaceWithInterpolation`) |
| `BQuantizationTable` | **Rollup on-the-fly**: bucketing temporal configurable |
| `BBqlFilterNode` | Filtro BQL sobre los records |
| `BScaleNode` / `BScaleTable` | Scaling numérico (factor, offset) |
| `BTerminalNode` | Nodo final que expone el schema output |
| `IntervalSeriesCursor` | Cursor que genera records a intervalo fijo (alineación temporal) |

**BTransformGraph.doResolve()** signature (CONFIRMADO):
```java
BSeriesTransformTable[] doResolve(
  BSeriesTransformTable[] inputTables,
  GraphNodeParams params,
  BOrd graphOrd,
  Context cx
) throws TransformException
```

El `encodeSeriesTransformData()` del servlet acepta opcionalmente `|transform:...` en el ORD para pasar parámetros de transformación (CONFIRMADO bytecode `ldc "|transform:"`).

---

## 45.3 Time-range queries — sintaxis real

### 45.3.1 Parámetros de tiempo: modos

**Modo 1 — Rango absoluto** (más común para SPA):
```javascript
// modelUtil.getFullOrd() — CONFIRMADO fuente JS legible
const fullOrd = "history:$/Floor1_Temp" +
  "?start=" + start.encodeToString() +
  ";end=" + end.encodeToString();
const url = "/webChart/data/" + encodeURIComponent(fullOrd);
```

`BAbsTime.encodeToString()` produce `13,{ISO-8601}`. Ejemplo: `13,2024-01-15T10:00:00Z`.
El `13` es el type ordinal de `BAbsTime` — SIEMPRE necesario, NO solo el ISO-8601 string.

**Modo 2 — Período relativo** (cuando `period.getOrdinal() > 1`):
```javascript
// Cuando se usa un período relativo:
const fullOrd = "history:$/Floor1_Temp?period=today";
// Server calcula start/end según la TZ del BHistoryConfig
```

Períodos disponibles (`BWebChartTimeRangeType` — CONFIRMADO decompilado):
```
auto, timeRange, today, last24Hours, yesterday, weekToDate, lastWeek,
last7Days, monthToDate, lastMonth, yearToDate, lastYear
```

**Gotcha período `auto`**: Cuando `period=auto` (ordinal 0), el modelo JS usa la ventana visible del chart para calcular start/end dinámicamente. Para una SPA custom que no usa el WebChart widget, usar siempre rango absoluto.

**Modo 3 — Delta** (para tailing incremental):
```
history:$/Floor1_Temp?start={lastTimestamp};delta=true
```
Retorna solo records DESPUÉS del `start` timestamp. Usado internamente por `BNiagaraHistoryImport.doExecute()` para sync incremental.

### 45.3.2 BQL sobre history

Sintaxis BQL para history (cross-ref Bloque 21 + 33.11.3):
```sql
bql:select timestamp, value from history:$/Floor1_Temp
  where timestamp > -1d
  order by timestamp desc
  limit 100
```

**IMPORTANTE**: BQL sobre history NO tiene índices secundarios. `where value > 75.0` = scan lineal completo del `.hdb`. Solo `where timestamp` usa el cursor temporal eficiente.

**Gotcha**: el BQL endpoint para queries no-chart es `QueryServlet` (`/query/*`), no `WebChartQueryServlet`. La respuesta de `/query/` es formato diferente (tabla Baja). Para chart data, usar siempre `/webChart/data/`.

### 45.3.3 Límites server-side

| Límite | Fuente | Default | Configurable |
|--------|--------|---------|--------------|
| `maxArchiveResultsPerQuery` | `BArchiveHistoryProvider.maxArchiveResultsPerQuery` | 10,000 records | Sí, por BOG property del provider |
| `niagara.webChart.maxSeriesCapacity` | `system.properties` / webChartUtil.js | **250,000 records** | Sí, `system.properties` |
| `niagara.webChart.maxSamplingSize` | webChartUtil.js | **50,000 puntos** (max de autoSamplingSize o este) | Sí |
| `niagara.webChart.autoSamplingSize` | webChartUtil.js | **2,500 puntos** (default de auto-downsampling) | Sí |
| `rdbArchiveHistoryCursor.inactivityTimeout` | `system.properties:513-517` | 120,000 ms (2 min) | `niagara.rdbArchiveHistoryCursor.inactivityTimeout` |
| No hay server timeout explícito para la query | — | — | El GC corta si cliente desconecta (chunked streaming detecta connection close) |

**Gotcha maxSeriesCapacity**: 250K records es el hard cap del CLIENT-SIDE. El servidor envía todos los records que tiene (hasta `maxArchiveResultsPerQuery` para archive, sin cap para local). El cliente descarta los más viejos al superar `maxSeriesCapacity`. Para 100K+ puntos en un rango largo, el servidor los envía todos pero el cliente solo retiene 250K.

**Gotcha timeout implícito**: la respuesta es chunked streaming HTTP. No hay timeout en el servidor para el query en sí — el cliente puede desconectarse y el servidor detecta el Connection Abort (log `"Connection Aborted"` — CONFIRMADO string en bytecode). Una query sobre una historia con 10M records puede tardar minutos.

---

## 45.4 Rollup / downsampling

### 45.4.1 BHistoryRollup (server-side, pre-computado)

Package `com.tridium.history.rollup.*` (CONFIRMADO Bloque 33.11.5):

```
BHistoryRollup       — component (se agrega como child del BIHistory)
BHistoryRollupRecord
BRollupInterval      — enum: MIN, HOUR, DAY, WEEK, MONTH, YEAR
RollupCursor
CollectiveRollupValue — contiene: sum, avg, min, max, stddev, count
TrendRecordRollupValue
HistoryRollupColumns
```

El `BHistoryRollup` genera una tabla secundaria pre-computada con buckets. Query sobre el rollup es O(N_buckets) vs O(N_records). Trade-off: storage +20-30%, query speed 100-1000x más rápido para aggregates.

**Cómo accederlo desde SPA**: el ORD del rollup tiene el ORD del history como base + la path del `BHistoryRollup` component como slot path. Para consultar el rollup de una historia desde `/webChart/data/`, el ORD apunta al `BHistoryRollup` component, no a la historia directamente.

**Gotcha**: `BHistoryRollup` debe ser configurado y habilitado en el station. NO existe por defecto. Una historia sin rollup configurado → la SPA recibe todos los records sin pre-aggregation.

### 45.4.2 SeriesTransform — rollup on-the-fly (server-side, runtime)

`BQuantizationTable` + `IntervalSeriesCursor` en el pipeline de SeriesTransform:

- Input: cursor raw de la historia
- Output: cursor con records a intervalos fijos (1min, 5min, 1h, etc)
- Función de aggregación: media, mínimo, máximo, suma, conteo (extensible via `BTransformFunction`)

**Cómo expone functions custom** (devguide confirmado):
```java
public abstract class BTransformFunction extends BComponent {
  public abstract void applyFunction(
    Map<String, List<BComplex>> series,
    String[] srcProps,
    Property destProp,
    BComplex resultRecord
  ) throws TransformException;
}
```

Registradas automáticamente — Aggregate/Rollup graph nodes las descubren en el classpath.

**Cuándo usar SeriesTransform vs BHistoryRollup**:
- `BHistoryRollup`: una historia, un intervalo fijo, pre-computado → **más rápido para dashboards de producción**
- `SeriesTransform BQuantizationTable`: multi-historia, intervalo configurable en runtime, calculo on-the-fly → **más flexible, más lento**

### 45.4.3 Client-side downsampling (webChart-ux)

`samplingUtil.calculateSeriesSamplingStats()` (CONFIRMADO fuente JS legible en `rc/model/samplingUtil.js`):

El chart calcula un `bestSlice` (ms per bucket) basado en:
- `sampleSize` del modelo (configurable, default 2,500 — `niagara.webChart.autoSamplingSize`)
- Duración del rango visible (zoom)
- Cantidad de puntos en `focusPoints` (puntos dentro del viewport)

El downsampling client-side es un **bucketing temporal simple** (NOT LTTB — no se encontró Largest-Triangle-Three-Buckets en el código). El algoritmo toma el promedio (o el valor representativo) de cada bucket de `bestSlice` ms:
- Si `focusPoints.length > sampleSize` → `maxSlice` needed → agrupa por bucket
- Si `focusPoints.length <= sampleSize` → `minSlice` → muestra todos los puntos disponibles

**Gotcha no LTTB**: Niagara webChart usa bucketing temporal, no LTTB. Esto puede perder picos y valles en series muy irregulares cuando el downsampling es agresivo. Para series con spikes cortos críticos (alarmas de temperatura), el downsampling puede ocultarlos visualmente.

**Gotcha `d3` dependency**: `DataLayer.js` usa `d3` para render SVG lines/bars. NO es Canvas2D ni WebGL. SVG con 250K nodos → DOM muy pesado. Para 100K+ puntos, el downsampling client-side a ~2,500 puntos visible es OBLIGATORIO para performance.

---

## 45.5 Real-time tail — patrón confirmado

### 45.5.1 El patrón dual: query histórica + subscription live

Niagara webChart NO tiene un endpoint "tail" dedicado. El patrón real-time está implementado via **2 mecanismos simultáneos**:

**Mecanismo 1 — Carga inicial histórica** (para el rango de tiempo configurado):
```
GET /webChart/data/{historyOrd}?start=...&end=...
→ NDJSON chunked: todos los records del rango
→ modelUtil.chunkData() los procesa progresivamente mientras llegan
```

**Mecanismo 2 — Live tail via `PointSeries`** (CONFIRMADO `rc/model/PointSeries.js`):

`PointSeries` implementa tail real-time **suscribiéndose al punto de control** (no al history):
```javascript
// PointSeries.subscribe() — CONFIRMADO fuente JS
subscriber.attach("changed", function(prop, cx) {
  if (prop.getName() === "out") {
    that.$update(cx.timestamp);  // agrega nuevo punto cuando el out cambia
  }
});
subscriber.subscribe(value);  // value = BControlPoint BOX subscription
```

Esta es la clave arquitectónica: **el real-time tail NO es via polling a `/webChart/data/`**. Es via el BOX `boxcs` channel subscribiendo al `BControlPoint.out` via el `Subscriber` de BajaScript (Bloque 42). El nuevo valor live se procesa con `$updateWithTime()` → `prepareLivePoint()` → append al array de points del chart.

**Para SPA sin BajaScript runtime** (no usa el webChart widget): el patrón equivalente es:
1. Carga histórica inicial: `GET /webChart/data/{historyOrd}?start={T_inicio}&end={now}`
2. Suscripción al punto via BOX `boxcs` channel (Bloque 42)
3. En el callback `changed` → `prepareLivePoint()` → append al array → re-render

NO hay un endpoint HTTP de "tail streaming" tipo Server-Sent Events (SSE). El live data llega via WebSocket BOX.

### 45.5.2 BOX `hist` channel — alternativa nativa

`BOX hist` channel (CONFIRMADO Bloque 47.5.6, `box-rt.jar`):

```json
{"c":"hist","k":"query","b":{"ord":"history:$/Floor1_Temp","start":"...","end":"..."}}
```

Operaciones: `query` (obtener registros históricos) y `stream` (streaming de nuevos registros).

**Gotcha**: el `hist` channel está confirmado en la tabla de channels pero su contrato detallado de `stream` NO fue decompilado en profundidad. La evidencia de `BHistoryChannel` en `baja.jar` (class `com.tridium.nd.history.BNiagaraHistoryImport implements BFoxClientConnection.Interest, BISubLicenseable`) sugiere que el channel existe principalmente para la comunicación Fox (subordinado→supervisor) y su uso desde BajaScript browser puede tener restricciones no documentadas.

**Recomendación**: Para SPA, usar el patrón dual `GET /webChart/data/` + BOX `boxcs` subscription al punto. El `hist` channel para streaming requiere más investigación empírica.

### 45.5.3 Latency del live tail

| Mecanismo | Latency | Notas |
|-----------|---------|-------|
| BOX subscription `boxcs` (`out` del punto) | ~10 ms debounce BOX + engine callback latency | El `BoxMessageRelay` tiene debounce 10ms (Bloque 22). La actualización llega cuando el engine propaga el cambio del `out` slot |
| `PointSeries` HistoryInterval (si el punto tiene `BIntervalHistoryExt`) | Interval configurable (1s, 5s, 60s, etc) | El punto live llega entre intervalos. Si interval=60s, la "tail" tiene latencia de hasta 60s |
| Polling manual a `/webChart/data/?delta=true` | Polling interval SPA-side | Evitar — peor enfoque. Cada request re-abre el cursor del `.hdb`. Causa file handle churn |

La configuración de `niagara.history.localDb.lingerTime=300000` (5 min, Bloque 33.1.3) afecta la latencia de cierre de file handles, NO la latency del live data.

---

## 45.6 Multi-series sync — N series en un chart

### 45.6.1 El problema de alineación temporal

Niagara history es **sample-per-trigger** (COV o interval). Dos puntos en el mismo chart:
- Point A (1 min interval): muestrea en 10:00:00, 10:01:00, 10:02:00...
- Point B (COV): muestrea en 10:00:03, 10:00:47, 10:02:15...

Los timestamps NO están alineados. El chart necesita decidir qué hacer en el eje X compartido.

### 45.6.2 Enfoque webChart — client-side sin interpolación forzada

El chart webChart-ux usa **client-side merge basado en `focusPoints`**:
1. Cada series mantiene su propio array de points independiente
2. Al render, `DataLayer` mapea cada serie en el mismo scale de tiempo X (`d3.scaleTime`)
3. Los puntos se dibujan en sus timestamps exactos — NO hay interpolación forzada

Esto significa:
- Si A tiene valor en 10:00:00 y B no tiene dato hasta 10:00:03 → el chart muestra B con un "gap" o con el último valor conocido según la configuración de `showDataGaps`
- La línea de B se mantiene plana (last value) hasta el próximo sample, a menos que `skip=true` fuerce una discontinuidad

**Configuración `showDataGaps`** (CONFIRMADO en `modelUtil.prepareLivePoint()`):
```javascript
if (model && model.settings().getShowDataGaps() === "yes") {
  if (points.length) {
    // marcar el último punto como skip para crear gap visual
  }
}
```

### 45.6.3 Enfoque SeriesTransform — server-side alignment

`IntervalSeriesCursor` + `BQuantizationTable` en el pipeline alinea múltiples historias al mismo grid temporal ANTES de retornar los datos. Esto es el **server-side merge recomendado** para multi-series con diferentes sample rates:

1. Configura un `BTransformGraph` con múltiples `BHistoryInput` nodes
2. `BQuantizationTable` cuantiza todos al mismo intervalo (e.g. 5 min)
3. `IntervalSeriesCursor` genera records alineados: todos los inputs tienen valor en cada bucket de 5 min
4. El servlet retorna los records ya alineados → el chart los dibuja sin gaps por desalineación

**Gotcha**: el `IntervalSeriesCursor` usa el último valor conocido para llenar buckets vacíos (step-interpolation), NO interpolación lineal. Para temperatura esto es correcto (last-value-carry-forward). Para energía acumulada podría ser incorrecto.

### 45.6.4 Timestamp deduplication

`modelUtil.prepareServletPoint()` (CONFIRMADO):
```javascript
if (lastPoint && lastPoint.x.getTime() === point.x.getTime()) {
  // Mismo timestamp → pop el punto anterior y reemplaza con el nuevo
  // Evita duplicados por polling remoto (NCCB-54925)
  points.pop();
}
```

Esto cubre el caso de deduplication por polling. Para out-of-order records (flag `r` con `outOfOrder` bit), el `prepareServletPoint` los incluye pero el `skip` se activa:
```javascript
if (webChartUtil.traceOn) {
  if (lastPoint && point.x.getTime() < lastPoint.x.getTime()) {
    // log: "backwards:N:t1<t2"
  }
}
```

---

## 45.7 Render performance 100K+ puntos

### 45.7.1 Constraints del browser

El webChart-ux usa **SVG via d3** (confirmado `DataLayer.js` imports `d3`). NO usa Canvas2D ni WebGL.

SVG constraint típico:
- 1,000 nodos SVG `<path>` → OK, 60 fps
- 10,000 nodos SVG → lento, 10-20 fps
- 100,000 nodos SVG → inutilizable, browser freeze

**Por eso el sistema tiene downsampling obligatorio**: con `autoSamplingSize=2,500` default, incluso si el servidor envía 250K records, el chart client-side hace rendering de ~2,500 puntos visible en el viewport.

### 45.7.2 Pipeline de performance end-to-end

Para 100K+ puntos en un rango de 1 año:

```
[SERVER] history .hdb lectura
  → hasta 250K records locales (sin archive)
  → NDJSON streaming chunked (transfer-encoding: chunked)
  → ~10-50 MB de data raw (depende del tipo de record)

[RED] HTTPS streaming chunked
  → chunkUtil.ajax() procesa progresivamente en XHR onprogress
  → cada chunk completo (delimitado por \n) → JSON.parse → preparePoint()

[CLIENT] BaseSeries.trimToCapacity()
  → cap = maxSeriesCapacity (default 250K)
  → si points.length > 250K → slice() drop oldest

[CLIENT] samplingUtil.calculateSeriesSamplingStats()
  → bestSlice = duración_visible / min(sampleSize, focusPoints.length)
  → default autoSamplingSize = 2,500 points

[RENDER] DataLayer (d3 SVG)
  → ~2,500 SVG path nodes → 60fps OK
  → zoom-in → más puntos en viewport → re-sample → re-render
```

### 45.7.3 system.properties para tuning webChart

| Propiedad | Default | Tuning recomendado |
|-----------|---------|-------------------|
| `niagara.webChart.maxSeriesCapacity` | 250,000 | Reducir a 50,000 si el station tiene poca RAM |
| `niagara.webChart.autoSamplingSize` | 2,500 | Aumentar a 5,000-10,000 para displays de alta resolución (4K) |
| `niagara.webChart.maxSamplingSize` | 50,000 | Cap del sampling size máximo |
| `niagara.webChart.indentChartFile` | — | Boolean: indent JSON en `.chart` files (debug only) |

**Gotcha**: estas propiedades son leídas via `getIntSystemProperty()` del JS runtime (CONFIRMADO en `webChartUtil.getMaxSeriesCapacity()`) — el servidor Niagara las expone como JavaScript-accessible via `ClientEnvServlet`. Deben estar en `system.properties` del station para que el cliente las vea.

### 45.7.4 Estrategias de performance para SPA custom (no usando webChart widget)

Si la SPA implementa su propio chart (Highcharts, Chart.js, D3, ECharts):

**Estrategia 1 — BHistoryRollup server-side** (RECOMENDADA):
- Configurar `BHistoryRollup` con intervalo apropiado (1h para dashboard diario, 1d para dashboard anual)
- Query siempre al rollup ORD, no al history raw
- Resultado: ~365 puntos para un año, 100x menos data transfer

**Estrategia 2 — SeriesTransform BQuantizationTable**:
- Configurar `BTransformGraph` con `BQuantizationTable` para el intervalo deseado
- Query via `/seriesTransform/data/{transformGraphOrd}?start=...&end=...`
- On-the-fly rollup → más flexible pero más lento que pre-computado

**Estrategia 3 — Downsampling LTTB client-side**:
- Recibir todos los points del servidor (hasta maxSeriesCapacity)
- Aplicar LTTB (Largest-Triangle-Three-Buckets) → preserva picos/valles mejor que bucketing uniforme
- Librerías disponibles: `d3-downsample`, `simplify-js`, `@symptomatic/lttb`
- NO implementado en webChart-ux nativo → implementar en SPA custom

**Estrategia 4 — Lazy viewport loading**:
- Cargar solo el rango temporal visible en el viewport
- Al zoom/pan → nueva query con el rango visible
- Pattern: mantener un "buffer" de 2x el viewport visible para scroll fluido
- Usar `delta=true` para el "buffer" adelante del viewport actual

### 45.7.5 Canvas2D vs WebGL en SPAs custom

Niagara webChart usa SVG (d3). Para SPAs con 100K+ puntos SIN downsampling:
- **Canvas2D**: ~100K puntos a 60fps con paths manuales. Límite ~1M antes de degradarse.
- **WebGL**: escala a millones de puntos. Requiere librerías como `echarts` (WebGL mode), `Plotly` (WebGL scatter), `deck.gl`.
- **Recomendación SPA custom**: Canvas2D con LTTB (resultado ~1,000-5,000 puntos) para casi todos los casos. WebGL solo si se necesita >100K puntos en pantalla simultáneamente.

---

## 45.8 Refinamiento Bloques 42-44, 46, 48-49

### 45.8.1 Refinamiento Bloque 42 — Subscriber lifecycle + history tail

El patrón `PointSeries.subscribe()` en el chart usa exactamente el `Subscriber` de BajaScript documentado en Bloque 42. La diferencia vs una subscription de alarma o propiedad:

- El `Subscriber` se adjunta al `BControlPoint.out` (not al history)
- `$changed` callback recibe `prop.getName() === "out"` + `cx.timestamp` del engine
- El timestamp del `cx` es el timestamp del evento de engine, que aproxima el momento del cambio
- Para historias COV: coincide con el timestamp que se graba en el `.hdb`
- Para historias interval: el timestamp del event es el tiempo del `intervalElapsed` callback, que puede diferir del timestamp grabado en el `.hdb` por latencia del engine

**Corrección al Bloque 42 implícita**: el lifecycle de subscription para chart tail es el mismo Subscriber API, pero el evento de interés es `changed` con `prop="out"`, NO `topicFired`. El historyExt escribe al `.hdb` internamente — la SPA no necesita suscribirse al historyExt, solo al punto.

### 45.8.2 Refinamiento Bloque 43 — Schedule series

`WebChartQueryServlet` expone también datos de schedule (CONFIRMADO `schedulePattern = Pattern.compile("/schedule/.+")`):

```
GET /webChart/schedule/{scheduleOrd}?start=...&end=...
```

Response format: `text/plain` (NOT `application/json`). Cada línea es un entry de schedule:
```
encodeMinifiedEntry(jsonWriter, timestamp, value, trendFlags, status, cx)
```

La firma `encodeScheduleEntry(JSONWriter, BAbsTime, BValue, Context)` confirma que el schedule se serializa igual que los history records (minified JSON). Esto es lo que `ScheduleSeries.js` en `webChart-ux` consume.

**Para SPA de Bloque 43**: la SPA de schedule render puede reusar este endpoint para cargar la historia del schedule (qué valor estuvo activo cuándo), complementando la UI de edición.

### 45.8.3 Refinamiento Bloque 44 — Alarm Console + history cruce

El alarm console (Bloque 44) y el history chart pueden compartir el mismo timeline. Pattern:
1. Chart muestra trend de temperatura (via `/webChart/data/`)
2. Alarm events se superponen como annotations (marcadores verticales)
3. Los alarm events vienen de BOX `alarm` channel o de query BQL sobre el `.adb`

La sincronización temporal es via `BAbsTime` UTC → mismo eje X. No hay API de "align alarms to history" server-side — el client merge es responsabilidad de la SPA.

**Gotcha**: el `.adb` (alarm database) tiene formato diferente al `.hdb`. Las queries BQL sobre alarms usan el ORD `alarm:` scheme, NO `history:`. No se puede combinar en una sola query WebChart.

### 45.8.4 Refinamiento Bloque 46 — Writes con priority array

Luego de hacer write con priority array (Bloque 46), el chart live tail recibe el cambio via `PointSeries.subscribe()` callback en `out`. Si el write afecta el level activo → el `out` value cambia → el chart append el nuevo punto. Si el write es en un level no-activo (override silencioso) → el `out` NO cambia → el chart NO actualiza.

**Gotcha para SPA**: un write exitoso (HTTP 200) no garantiza un update en el chart. Solo si el write cambia el `out` del punto el chart actualiza. Esto es correcto por diseño del priority array — la SPA debe entender qué level está activo antes de asumir que un write será visible.

### 45.8.5 Refinamiento Bloque 48 — RBAC visibility

El `WebChartQueryServlet.encodeHistoryData()` corre bajo el `Context` del usuario autenticado. Si el usuario NO tiene permiso de lectura sobre la historia → `BOrd.resolve()` lanzará exception de seguridad → response 500 o 403.

**Gotcha**: NO hay un mecanismo de "filter out unauthorized histories" en la query. Si una query BQL lista histrorias y el usuario no tiene acceso a alguna → la query falla completa (no retorna las accesibles). Para SPA multi-tenant, verificar permisos por historia individualmente antes de mostrar en el selector.

### 45.8.6 Refinamiento Bloque 49 — Facets + units en series

`ServletSeries.loadInfo()` hace un RPC a `WebChartQueryRpc.getInfo([ordString])`:
```javascript
webChartUtil.rpc("type:webChart:WebChartQueryRpc", "getInfo", [String(ord)])
  .then(response => {
    that.$recordType = response.recordTypes[0];
    that.$displayPath = response.displayPaths[0];
    that.$facets = await baja.Facets.DEFAULT.decodeAsync(response.valueFacets[0]);
    that.$units = that.$facets.get("units", baja.Unit.DEFAULT);
  });
```

Los `valueFacets` vienen del `BControlPoint.facets` del punto source. Las units (e.g. `°C`, `kWh`, `Pa`) se usan para:
- Eje Y label del chart
- Tooltip display con conversión de unidades
- `numberUtils.convertUnitTo()` para conversión client-side

Para una SPA custom, la ruta más directa es:
1. Resolver el ORD del history via BOX `sys` channel → `getTypes`
2. Obtener los facets del punto source directamente via `BControlPoint.facets` subscription
3. Extraer `units` facet para formatear los valores

O simplemente consumir los facets raw del `getInfo` RPC si se usa el stack WebChart.

---

## 45.9 Antipatterns

### G45-1 — Polling a `/webChart/data/` para live data
**Antipattern**: hacer GET cada 30 segundos a `/webChart/data/{ord}?start={now-30s}&end={now}` para simular live.

**Por qué es malo**: cada request re-abre el cursor del `.hdb` (Bloque 33.4.4 — NO hay pool de connections). Con 20 histories × polling 30s = 40 file opens/min → iops elevado. Además, si el cursor activo impide `closeUnusedHistories`, se acumulan file handles hasta el `ulimit`.

**Correcto**: Usar BOX `boxcs` subscription al `BControlPoint.out` para live data. Solo hacer query histórica una vez al cargar.

### G45-2 — No cerrar HistorySpaceConnection
**Antipattern**: código server-side (módulo custom) que abre `db.getConnection(cx)` sin try-with-resources.

**Por qué es malo**: Bloque 33.4 — cada connection mantiene lock implícito sobre `BHistoryDbTable`. El `closeUnusedHistories` worker no puede cerrar la table → file handles leaked. Con >500 histories concurrentes → `Too many open files`.

**Correcto**: siempre `try (HistorySpaceConnection conn = db.getConnection(cx)) { ... }`.

### G45-3 — Usar `capacity=unlimited` en historias consultadas frecuentemente
**Antipattern**: history con `BCapacity.UNLIMITED` y la SPA la consulta con rango "todos los datos".

**Por qué es malo**: el `.hdb` crece indefinidamente → la query puede enviar gigabytes de NDJSON → OOM en el cliente, timeout en red. Docs dicen textualmente "Unlimited is not the wisest choice".

**Correcto**: siempre capacity explícita + rollup o archive para datos históricos largos.

### G45-4 — Ignorar el `w` field en el response NDJSON
**Antipattern**: parsear todos los chunks de `/webChart/data/` como data points sin verificar `chunk.w`.

**Por qué es malo**: warnings del servidor (e.g. `archiveLimitNotificationBehavior` alerta) son lines NDJSON con campo `w`, no `t/v/s/r`. `JSON.parse(chunk).v` → `undefined` → NaN en el chart → línea rota o spike.

**Correcto**:
```javascript
const obj = JSON.parse(chunk);
if ('w' in obj) { handleWarning(obj.w); } else { addPoint(obj); }
```

### G45-5 — Asumir que un write exitoso actualiza inmediatamente el chart
**Antipattern**: escribir via priority array → asumir que el chart "ya tiene" el nuevo valor → NO re-subscribir.

**Por qué es malo**: si el write no cambia el `out` (otro level tiene mayor prioridad), el `changed` callback no llega. El chart muestra el valor anterior.

**Correcto**: tras write, verificar el `out` actual del punto antes de asumir el chart actualizó.

### G45-6 — Usar `period=auto` desde SPA custom sin el webChart widget
**Antipattern**: pasar `period=auto` al ORD esperando que el servidor determine el rango.

**Por qué es malo**: `period=auto` requiere que el cliente envíe el rango actual del viewport (via JS del modelo LineModel). Sin el widget, el servidor no tiene contexto del viewport. El comportamiento con `auto` sin start/end → server calcula `today` como fallback.

**Correcto**: desde SPA custom, siempre usar `start=` y `end=` explícitos.

### G45-7 — SVG con 100K+ puntos sin downsampling
**Antipattern**: recibir 100K records del servidor y renderizarlos todos en un chart SVG.

**Por qué es malo**: SVG DOM de 100K nodos → browser freeze 5-30 segundos. Tab crash probable en dispositivos con poca RAM.

**Correcto**: aplicar LTTB o bucketing para reducir a <5,000 puntos renderizados. Usar `autoSamplingSize` como referencia del sistema (default 2,500).

### G45-8 — Queries sin Accept header correcto
**Antipattern**: `GET /webChart/data/{ord}` sin header `Accept: application/json; protocol=webChart; version=1`.

**Por qué es malo**: `WebChartQueryServlet.doGet()` verifica `RestUtil.acceptJson()` + custom protocol/version. Sin el Accept correcto → **406 Not Acceptable**.

**Correcto**: siempre incluir el header Accept con el protocol custom.

### G45-9 — HistoryRollup sin retención de la historia base
**Antipattern**: configurar `BHistoryRollup` sobre una historia con `capacity=500 ROLL` asumiendo que el rollup compensa.

**Por qué es malo**: el rollup secundario también tiene capacidad configurable. Si la historia base tiene 500 records y el rollup es 1-día → solo 500/1440 ≈ 0.3 días de rollup disponibles. El rollup no "extiende" la retención — está limitado por los registros disponibles en la historia base al momento de computar.

**Correcto**: para retención larga, combinar: archivo a Supervisor (aumenta ventana) + rollup en Supervisor.

---

## 45.9b Feature especial: Interpolated tail

### 45.9b.1 Qué es el interpolated tail

El "interpolated tail" es una feature UI del webChart que dibuja una línea punteada (CSS class `interpolated`) desde el último data point real hasta "ahora" (timestamp actual del browser). Simula visualmente la extrapolación del último valor conocido.

**Implementación CONFIRMADA** (`rc/model/BaseSeries.js` fuente legible):

```javascript
// pointsWithInterpolatedTail() — agrega un punto virtual al array
BaseSeries.prototype.updateInterpolatedTail = function () {
  this.$interpolatedPointDate = new Date();  // timestamp actual browser
  var lastPoint = points[points.length - 1];
  this.$tailPoints = [{
    x: this.$interpolatedPointDate,
    y: lastPoint.y,           // MISMO valor que el último punto real
    interpolated: true,       // flag para CSS/render diferenciado
    status: lastPoint.status
  }];
};
```

Activación: `ChartSettings.isShowInterpolateTail()`. El comando `toggleInterpolateTailCommand` en la toolbar del chart activa/desactiva.

Refresh interval: `niagara.history.interpolateRefreshInterval` (default 10,000 ms = 10 segundos). CONFIRMADO en `webChartUtil.getInterpolateTailRefreshInterval()`.

### 45.9b.2 Implicaciones para SPA custom

El interpolated tail es puramente client-side. Para una SPA custom que implementa su propio chart:

1. Mantener el `lastDataPoint` de cada serie
2. Agregar un "virtual point" con `x = Date.now()` e `y = lastDataPoint.y`
3. Re-renderizar cada N segundos (recomendado: 10s, igual que `interpolateRefreshInterval`)
4. Diferenciar visualmente el punto virtual (CSS dashed line, lower opacity)

**Gotcha**: el interpolated tail solo tiene sentido si el chart está en modo "live" (activo, tab visible). Cuando el tab es backgrounded y el JavaScript se pausa, el tail deja de actualizarse hasta que el tab vuelve al frente.

### 45.9b.3 Interacción con samplingUtil

`samplingUtil.calculateSeriesSamplingStats()` filtra explícitamente los puntos interpolados del cálculo:
```javascript
focusPoints = seriesList[i].focusPoints(true).filter(function (point) {
  return !point.interpolated;  // el tail NO participa en el cálculo de bestSlice
});
```

Esto evita que el tail virtual distorsione el sampling — solo los puntos reales definen el zoom y density del chart.

---

## 45.9c Wire format completo — ejemplo de request/response

### 45.9c.1 Request completo desde SPA

```http
GET /webChart/data/history%3A%24%2FFloor1_Temp%3Fstart%3D13%2C2024-01-15T00%3A00%3A00Z%3Bend%3D13%2C2024-01-15T23%3A59%3A59Z HTTP/1.1
Host: station.sejofa.io
Accept: application/json; protocol=webChart; version=1
Cookie: JSESSIONID=abc123; niagara_userid=operator
x-niagara-csrfToken: def456
Connection: keep-alive
```

El ORD en el path está URL-encoded. Decodificado:
```
/webChart/data/history:$/Floor1_Temp?start=13,2024-01-15T00:00:00Z;end=13,2024-01-15T23:59:59Z
```

El `$` = shorthand de la station local (resuelve a su nombre en runtime).

### 45.9c.2 Response completo

```http
HTTP/1.1 200 OK
Content-Type: application/json
Transfer-Encoding: chunked
Cache-Control: no-cache
```

Body (NDJSON — una línea por record):
```ndjson
{"t":"13,2024-01-15T00:00:00Z","v":21.5,"s":0,"r":0}
{"t":"13,2024-01-15T00:01:00Z","v":21.7,"s":0,"r":0}
{"t":"13,2024-01-15T06:00:00Z","v":null,"s":8,"r":0}
{"w":"Archive limit reached: 10000 records"}
{"t":"13,2024-01-15T06:01:00Z","v":22.1,"s":0,"r":2}
{"t":"13,2024-01-15T23:59:00Z","v":23.0,"s":0,"r":0}
```

Decodificación:
- Línea 3: `v=null, s=8` → valor null + status con algún bit set (fault/overridden)
- Línea 4: `w=...` → warning (archive limit), NO es datapoint → ignorar como data
- Línea 5: `r=2` → BTrendFlags bit 2 = `startTrend` → gap ANTES de este punto (activePeriod reinició)

### 45.9c.3 BAbsTime encoding

El formato `13,2024-01-15T00:00:00Z`:
- `13` es el type ordinal de `BAbsTime` en el schema de Niagara (constante del sistema)
- `2024-01-15T00:00:00Z` es ISO-8601 con zona UTC siempre
- Decodificación cliente: `new Date("2024-01-15T00:00:00Z")` funciona directamente en JS

Para construir el ORD con start/end desde JS:
```javascript
function absTimeEncode(date) {
  return "13," + date.toISOString().replace(/\.\d{3}Z$/, "Z");
  // Niagara no requiere ms en el timestamp
}
const start = absTimeEncode(new Date("2024-01-15T00:00:00Z"));
// resultado: "13,2024-01-15T00:00:00Z"
```

---

## 45.9d Rollup + downsampling — tabla de decisión

| Escenario | Datos disponibles | Recomendación | Throughput |
|-----------|------------------|---------------|------------|
| Dashboard diario (24h), 1-min interval | ~1,440 records | Sin rollup, directo `/webChart/data/` | ~40 KB NDJSON |
| Dashboard semanal (7d), 1-min interval | ~10,080 records | Client-side sampling 2,500 OR BHistoryRollup 1h | ~280 KB raw |
| Dashboard mensual (30d), 5-min interval | ~8,640 records | BHistoryRollup 1d (30 buckets) | ~50 KB raw |
| Dashboard anual (365d), 1-min interval | ~525,600 records | BHistoryRollup 1d (365 buckets) OR SeriesTransform BQuantizationTable | ~15 MB raw SIN rollup → 10 KB con rollup 1d |
| Multi-series 10 puntos, 1 semana, COV irregular | Variable por punto | SeriesTransform IntervalSeriesCursor 1h → todos alineados | Variable |
| Picos críticos (alarmas, spikes) | Cualquier volumen | LTTB client-side (no built-in) + capacidad máxima | Sin pérdida de picos |

---

## 45.9e Historia en Supervisor vs en subordinado — diferencias de query

### 45.9e.1 Historia local vs importada

En el Supervisor, las historias pueden ser:

**A) Locales** (creadas en el Supervisor mismo via historyExt de puntos locales):
- ORD: `history:$/Supervisor_AuditLog`
- Query directa: `HistorySpaceConnection.timeQuery()` → local `.hdb`
- Sin round-trip a subordinado

**B) Importadas** (via `BNiagaraHistoryImport` — Bloque 33.8):
- ORD en el Supervisor: `history:{SubStationName}/Floor1_Temp`
- `{SubStationName}` = nombre del device (station subordinada)
- La data está en el `.hdb` LOCAL del Supervisor (ya importada)
- Query igual de rápida que local — la import ya copió los datos

**C) Query remota directa** (vía `BFoxHistorySpace`):
- Cuando el Supervisor consulta una historia que NO fue importada
- Abre Fox channel al subordinado + query remota
- Más lento (latencia Fox + rate del subordinado)
- Gotcha: `history:/StationName/HistName` con ORD que incluye el stationName hace la resolución via Fox session. La SPA nunca usa esto directamente — lo hace el servidor al resolver el ORD.

### 45.9e.2 Implicación para SPA

Desde la SPA, el ORD siempre apunta al Supervisor. El Supervisor resuelve si la historia está local o necesita Fox remoto. La SPA NO necesita saber si la historia es local o importada.

**Gotcha de latencia**: si el chart muestra 50 histories de 50 JACEs distintos y ninguna fue importada → 50 Fox calls al mismo tiempo durante el render del chart → potencial bottleneck Supervisor. Con histories importadas: 1 query local × 50.

---

## 45.10 TODOs honestos

1. **`hist` BOX channel stream operation** — la operación `stream` del channel `hist` está listada en la tabla de channels (Bloque 47.5.6) pero no fue decompilada en profundidad. Se desconoce el contrato exacto (¿requiere cursor abierto? ¿es SSE-like sobre WebSocket?). PENDIENTE decompilación de `hist-rt.jar`.

2. **`BWebChartQueryRpc` RPC interface** — el RPC `type:webChart:WebChartQueryRpc` (usado en `ServletSeries.loadInfo()`) fue identificado como clase `BWebChartQueryRpc` en `webChart-rt.jar`. Sus operaciones disponibles (`getInfo`, `getDisplayPath`) no fueron decompiladas completamente. PENDIENTE `javap -p BWebChartQueryRpc.class`.

3. **Límites server-side explícitos para query local** — el servidor NO tiene un hard cap de registros para query sobre `.hdb` local (solo `maxArchiveResultsPerQuery` para el archive). El GC controla si el cursor termina. No se encontró un server-side timeout configurable para queries largas. PENDIENTE verificar si `niagara.history.maxLocalQueryResults` existe en versiones posteriores.

4. **Rollup pre-computado — frecuencia de actualización** — `BHistoryRollup` genera los buckets cuando la historia recibe nuevos records. No se confirmó si el rollup se actualiza en tiempo real o con algún delay. PENDIENTE: decompilación `RollupCursor` para entender el mecanismo de invalidación.

5. **`webChart.built.min.js`** — el bundle minificado en `webChart-ux.jar:rc/webChart.built.min.js` puede contener lógica adicional no visible en los `.js` individuales (si el build concatena más fuentes). Los `.js` individuales en `rc/` son fuente pre-bundle, suficiente para el análisis conceptual. PENDIENTE confirmar si el bundle incluye módulos adicionales.

6. **Limits con 50+ historias en el mismo chart** — el comportamiento del chart con N series (50+) en el mismo TimeScale no fue testado. `samplingUtil.calculateSeriesSamplingStats()` itera todas las series para calcular el `bestSlice`. Con 50 series → 50x más computo. INFERIDO: degradación cuadrática probable, no lineal.

---

## 45.11 Próximos bloques

| Bloque | Tema | Dependencia de 45 |
|--------|------|-------------------|
| **46** | Writes con priority array desde SPA | Usa la misma sesión/auth de 47; el resultado del write afecta el live tail documentado en 45.5 |
| **48** | RBAC visibility en frontend | Complementa 45.8.5 — cómo verificar permisos de lectura ANTES de hacer query history |
| **49** | Facets, i18n y formatting en cliente | Complementa 45.8.6 — cómo usar `valueFacets` del `getInfo` RPC para formatear unidades |

---

## 45.12 Grafo de conexiones

```
history:.hdb (FORMAT: Bloque 33)
  ↓ HistorySpaceConnection.timeQuery()
  ↓
WebChartQueryServlet (webChart-rt.jar)
  ↓ encodeHistoryData() → WebChartUtil.encodeMinifiedHistoryRecord()
  ↓ NDJSON streaming chunked
  ↓
chunkUtil.ajax() (webChart-ux.jar:rc/chunkUtil.js)
  ↓ split("\n") → JSON.parse()
  ↓
ServletSeries.preparePoint() → modelUtil.prepareServletPoint()
  ↓ {x: timestamp, y: value, s: statusBits, r: trendFlags}
  ↓
BaseSeries.$points[] → trimToCapacity(250K)
  ↓ samplingUtil.calculateSeriesSamplingStats() → bestSlice
  ↓ focusPoints (viewport subset)
  ↓
DataLayer (d3 SVG) → render ~2,500 nodes

LIVE TAIL (paralelo):
BOX /wsbox → boxcs channel
  ↓ subscriber.subscribe(BControlPoint)
  ↓ "changed" event en out slot
  ↓
PointSeries.$updateWithTime(timestamp)
  ↓ prepareLivePoint() → append to $points[]
  ↓
REDRAW_REQUEST_EVENT → DataLayer re-render
```

---

## Resumen ejecutivo

**WebChart 3 endpoints concretos**:
1. `GET /webChart/data/{history-ORD-escaped}` — query time-series, NDJSON chunked, Accept: `application/json; protocol=webChart; version=1`
2. `GET /webChart/schedule/{schedule-ORD-escaped}` — datos de schedule, text/plain NDJSON
3. `POST /webChartFile/save?chartName={name}` / `GET /webChartFile/load?chartName={name}` — persistencia de configuración de chart
4. `GET /seriesTransform/data/{transformGraph-ORD-escaped}` — pipeline de transformación (rollup/scale/filter/cleanser)

**Top 7 hallazgos**:
1. `WebChartQueryServlet` es **GET** (no POST como sugería Bloque 29). El ORD completo con params viaja en el path.
2. Response es **NDJSON chunked** (`\n`-delimited JSON objects), NOT JSON array. Campos minificados: `{t, v, s, r}` + `{w}` para warnings.
3. Real-time tail: **NO hay endpoint "tail" HTTP**. El live data llega via BOX `boxcs` subscription al `BControlPoint.out`. El patrón es: query histórica inicial (HTTP GET) + subscription live (WebSocket BOX).
4. `BWebChartTimeRangeType` tiene **12 valores** confirmados: `auto, timeRange, today, last24Hours, yesterday, weekToDate, lastWeek, last7Days, monthToDate, lastMonth, yearToDate, lastYear`.
5. `maxSeriesCapacity` = 250,000 records (client-side hard cap, configurable via `niagara.webChart.maxSeriesCapacity`). `autoSamplingSize` = 2,500 puntos (default de downsampling visible, configurable via `niagara.webChart.autoSamplingSize`).
6. webChart usa **SVG d3** (NOT Canvas2D/WebGL). Downsampling obligatorio: client-side bucketing temporal a ~2,500 puntos. NO LTTB nativo — implementar en SPA custom para mejor preservación de picos/valles.
7. SeriesTransform (`/seriesTransform/data/`) ofrece rollup on-the-fly + multi-series alignment server-side (via `IntervalSeriesCursor` + `BQuantizationTable`). Distinción clave: `BHistoryRollup` = pre-computado (más rápido), SeriesTransform = on-the-fly (más flexible).
