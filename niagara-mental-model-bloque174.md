# Block 174 — chihuahua MX60 (`-ux`): subsistema history (BHistoryDatabase, stride downsampling, equipment-histories)

> **WHAT** — documenta el SUBSISTEMA HISTORY del dashboard chihuahua MX60 (gap C12): cómo se
> enumeran las histories de la estación, cómo se ejecuta la consulta por rango de tiempo contra
> `BHistoryDatabase`, el algoritmo de **stride downsampling** que auto-escala el número de puntos
> según la densidad real de captura, el vocabulario de rangos soportado, y cómo se detectan/emparejan
> los history IDs por equipo (link-graph + fallback por nombre). Fuente primaria de autoría propia
> (`com.angeles.chihuahua.ux`), portado desde `SnlsHistoryHelper`.
>
> Focus: **chihuahua** (subsistema history del módulo MX60, fuente primaria). Corpus language: Spanish.
>
> Sources (fuente primaria, base `…/Cliente/Honeywell/MX60/chihuahua/chihuahua/`):
> - `UX/` = `chihuahua-ux/src/com/angeles/chihuahua/ux/`
> - Ficheros leídos: `UX/ChiHistoryHelper.java` (helper del subsistema) · `UX/BChiServlet.java`
>   (handlers HTTP `/api/historyList`, `/api/historyData`, `/api/equipment-histories`)
>
> **Sensibilidad:** despliegue de cliente real → `.env.local` (IP JACE/credenciales) NO se leyó ni se
> cita. El código es citable.
>
> Markers: `[CERT]` = leído en la fuente primaria (marker FUERA de la cita, `file:line`) · `[INFER]` =
> deducción a partir del código. Capa 26 (módulo dashboard OEM, autoría propia).
>
> Continúa [Block 163].

---

## 174.1 — Los tres puntos de entrada HTTP y su cableado al helper

El servlet expone el subsistema history vía tres endpoints, publicados en el bloque de config del
frontend como `historyList`, `historyData` y `equipmentHistories` `[CERT]` `UX/BChiServlet.java:98-100`.
El dispatch los mapea a tres `RouteAction` (`HistoryList`, `HistoryData`, `EquipmentHistories`) en
`executeAction` `[CERT]` `UX/BChiServlet.java:271-284`.

Toda la lógica real vive en `ChiHistoryHelper` (clase `final`, constructor privado, sólo estáticos)
`[CERT]` `UX/ChiHistoryHelper.java:38-69`; los handlers del servlet son delgados: fijan headers, status
200, delegan y capturan excepciones devolviendo `{"error":...}`:

- `handleHistoryList` → `ChiHistoryHelper.listHistories(out, this)` `[CERT]` `UX/BChiServlet.java:493-510`.
- `handleHistoryData(id, range, fullResolution)` → `ChiHistoryHelper.queryHistoryData(id, range, fullResolution, out, this)` `[CERT]` `UX/BChiServlet.java:517-534`.
- `handleEquipmentHistories` → resuelve primero el `ChiDashboardService` por `BOrd` (`DRIVER_TREE_ORD`) y,
  si existe, llama `ChiHistoryHelper.detectEquipmentHistories(service, out, this)`; si el service no se
  resuelve emite `{}` `[CERT]` `UX/BChiServlet.java:542-566`.

El `BComponent context` que reciben todos los métodos del helper es el propio servlet (`this`), usado
como ancla para resolver `BOrd.make("history:")` `[INFER]` `UX/BChiServlet.java:501,525,565`.

## 174.2 — listHistories: enumeración de todas las histories de la estación

`listHistories` resuelve la base de datos de histories con `BOrd.make("history:").get(context, null)`
casteado a `BHistoryDatabase`, y obtiene el arreglo completo con `db.getHistories()` `[CERT]`
`UX/ChiHistoryHelper.java:79-80`. Por cada `BIHistory` extrae su `BHistoryConfig` y de ahí el
`BHistoryId` `[CERT]` `UX/ChiHistoryHelper.java:86-87`, y serializa a mano un objeto JSON
`{"histories":[{id,name,units}, ...]}`.

Resolución del **name** con degradación en cascada `[CERT]` `UX/ChiHistoryHelper.java:92-100`:
1. `cfg.getDisplayName(null)`;
2. si es null/vacío/`"null"` → `hid.toString()`;
3. si sigue null/`"null"` → `hid.encodeToString()`.

El **id** emitido es siempre `hid.encodeToString()` (forma canónica para round-trip) `[CERT]`
`UX/ChiHistoryHelper.java:112-113`. Las **units** se leen del facet `units` dentro del `BFacets`
`valueFacets` del config, envuelto en try/catch que ignora fallos y deja units `""` `[CERT]`
`UX/ChiHistoryHelper.java:101-110`. Cualquier excepción global produce `{"error":...}` `[CERT]`
`UX/ChiHistoryHelper.java:122-126`.

## 174.3 — queryHistoryData: la consulta por rango de tiempo

Firma pública con overload de compatibilidad: `queryHistoryData(id, range, out, context)` delega a la
variante con `fullResolution=false` `[CERT]` `UX/ChiHistoryHelper.java:150-153`. La variante completa
`queryHistoryData(id, range, fullResolution, out, context)` es el núcleo `[CERT]`
`UX/ChiHistoryHelper.java:155`.

Flujo:
1. `computeRange(rangeName)` → par `[start, stop]` de `BAbsTime` (§174.5) `[CERT]` `UX/ChiHistoryHelper.java:160-162`.
2. Resuelve `BHistoryDatabase` y abre una `HistorySpaceConnection` con `db.getConnection(null)` `[CERT]` `UX/ChiHistoryHelper.java:164-165`.
3. **Resolución del history ID** en dos vías `[CERT]` `UX/ChiHistoryHelper.java:167-181`:
   - si el `historyId` empieza con `/` → es un ID canónico directo: `BHistoryId.make(historyId)`;
   - en otro caso → se trata como un ORD de componente/slot: resuelve el `BComponent` por `BOrd` y busca
     su `BHistoryExt` vía `findHistoryExtId(slotComp)` (§174.6). Este segundo camino permite consultar
     por ORD de punto en vez de por ID de history.
4. Si no se resolvió ningún `hid` → emite un objeto vacío con `"data":[]` (contrato no-crash) `[CERT]`
   `UX/ChiHistoryHelper.java:182-187`.
5. Obtiene la history de la conexión (`conn.getHistory(hid)`), lee `units` igual que §174.2, y ejecuta
   la consulta temporal: `conn.timeQuery(history, start, stop)` → `BITable` → `TableCursor` `[CERT]`
   `UX/ChiHistoryHelper.java:188-203`.
6. Serializa cabecera `{"hId","title","units","range","data":[...]}` donde `title = hid.toString()`
   `[CERT]` `UX/ChiHistoryHelper.java:205-213`.
7. La conexión se cierra siempre en `finally` `[CERT]` `UX/ChiHistoryHelper.java:279-285`.

**Extracción del valor** por registro (`extractValue`) `[CERT]` `UX/ChiHistoryHelper.java:292-308`:
- `BNumericTrendRecord`: NaN/Inf → `"null"`; enteros exactos → `long`; resto → redondeo a 2 decimales
  formateado manualmente (`(r/100)."%02d"`).
- `BBooleanTrendRecord`: `true→"1"`, `false→"0"`.
- otro tipo de registro → `null` (se descarta en el bucle).

## 174.4 — El algoritmo de stride downsampling (auto-escala por densidad real)

Este es el corazón del subsistema y su historia de bug. Dos constantes de tope:
- `MAX_POINTS_HARD_CEILING = 5000` — cota dura de registros emitidos, incluso con `fullResolution=true`
  `[CERT]` `UX/ChiHistoryHelper.java:139`.
- `MAX_BUFFERED_RECORDS = 100_000` — guarda anti-OOM sobre los registros bufferizados antes de
  submuestrear `[CERT]` `UX/ChiHistoryHelper.java:147`.

**Diseño REVISADO (fix de densidad horaria del cárcamo).** El comentario documenta el bug previo: la ruta
anterior estimaba el stride asumiendo ~1 muestra/min, lo que descartaba silenciosamente ~98% de las
muestras del cárcamo (capturadas por hora, 60× más dispersas) — una gráfica de 30d mostraba ~7 de 720
puntos reales `[CERT]` `UX/ChiHistoryHelper.java:215-224`.

El algoritmo actual tiene tres fases `[CERT]` `UX/ChiHistoryHelper.java:228-270`:

1. **Target por rango**: `targetPoints = computeTargetPoints(rangeName)` (tabla, §174.5b) `[CERT]`
   `UX/ChiHistoryHelper.java:228`.
2. **Buffer primero, densidad después**: recorre TODO el cursor bufferizando `(timestamp, value)` de los
   registros válidos en dos `ArrayList` paralelos (`tsBuf`, `valBuf`), saltándose los `val==null`. Si el
   buffer alcanza `MAX_BUFFERED_RECORDS` emite un WARNING y trunca la cola `[CERT]`
   `UX/ChiHistoryHelper.java:232-248`. El stride se deriva del conteo REAL `n = tsBuf.size()`, no de una
   densidad asumida.
3. **Stride y emisión**: `stride = fullResolution ? 1 : computeStrideFromCount(n, targetPoints)` `[CERT]`
   `UX/ChiHistoryHelper.java:250-251`. El bucle emite el registro `i` cuando `i % stride == 0` **o**
   `i == n-1` (garantiza SIEMPRE el último punto, para que la línea termine en "ahora" y no deje el
   hueco visual al final), acotado por `emitted < MAX_POINTS_HARD_CEILING` `[CERT]`
   `UX/ChiHistoryHelper.java:255-270`.

**`computeStrideFromCount(actualCount, targetPoints)`** — la fórmula self-scaling `[CERT]`
`UX/ChiHistoryHelper.java:460-466`:
- `targetPoints < 1` → se clampa a 1;
- `actualCount <= targetPoints` → **stride 1** (histories dispersas se emiten completas);
- `actualCount > targetPoints` → **ceil division** `(actualCount + targetPoints - 1) / targetPoints`,
  de modo que los puntos emitidos nunca exceden `targetPoints` (+1 por la garantía del último punto);
- resultado siempre `>= 1` (`Math.max(1, stride)`).

Consecuencia `[INFER]` (documentada en el javadoc `UX/ChiHistoryHelper.java:435-459`): una history densa
(UP a 1 muestra/min) se adelgaza a ~`targetPoints`; una dispersa (cárcamo a 1 muestra/hora) sale
íntegra; `fullResolution=true` fuerza stride 1 y sólo el hard ceiling de 5000 la recorta.

## 174.5 — computeRange: el vocabulario de rangos

`computeRange(name)` devuelve `BAbsTime[]{start, stop}` a partir de un `Calendar.getInstance()` cuyo
`stop` por defecto es "ahora" `[CERT]` `UX/ChiHistoryHelper.java:314-319`. `name==null` se normaliza a
`"lastHour"` `[CERT]` `UX/ChiHistoryHelper.java:316`.

Vocabulario soportado (8 claves, case-sensitive) `[CERT]` `UX/ChiHistoryHelper.java:321-379`:

| clave | cómputo del start |
|---|---|
| `lastHour` | −1 hora (rama por defecto) `UX/ChiHistoryHelper.java:378` |
| `last8Hours` | −8 horas `:321-323` |
| `today` | medianoche de hoy (H/M/S/ms = 0) `:325-331` |
| `last24Hours` | −24 horas `:332-335` |
| `yesterday` | medianoche ayer → 23:59:59.999 ayer (retorno anticipado con start Y stop explícitos) `:336-350` |
| `last7Days` | −7 días `:351-354` |
| `last30Days` | −30 días `:355-358` |
| `monthToDate` | día 1 del mes, medianoche `:359-366` |

**Contrato defensivo ante clave desconocida**: cae a la rama por defecto (`lastHour`, −1 hora) pero emite
`LOG.warning("computeRange unknown range: ...")` sólo si la clave no era literalmente `"lastHour"`. El
comentario documenta que el fallback silencioso original fue la causa raíz de "bloque #73" `[CERT]`
`UX/ChiHistoryHelper.java:367-379`.

### 174.5b — computeTargetPoints (tabla paralela de puntos objetivo)

Tabla explícita (design Decision 2: lookup, no fórmula de densidad) que devuelve los puntos objetivo por
rango `[CERT]` `UX/ChiHistoryHelper.java:412-429`: `lastHour`=60, `last8Hours`=96, `last24Hours`=96,
`last7Days`=168, `today`=144, `yesterday`=96, `last30Days`=360, `monthToDate`=360. `null` → 60 + WARNING;
clave desconocida → 60 + WARNING (mismo contrato defensivo que `computeRange`) `[CERT]`
`UX/ChiHistoryHelper.java:414-428`. Las 4 claves originales espejan intencionalmente el `RANGES[i].points`
del frontend, para que el stride cliente sea no-op cuando el backend ya devolvió exactamente
`targetPoints` `[CERT]` `UX/ChiHistoryHelper.java:396-401`.

## 174.6 — detectEquipmentHistories: history IDs por equipo (link-graph + nombre)

`/api/equipment-histories` produce un mapa `{ equipId: { propName: historyId, ... }, ... }`. Las
propiedades de history se definen por tipo de equipo en tres arreglos estáticos `[CERT]`
`UX/ChiHistoryHelper.java:44-67`:
- `UP_HISTORY_PROPERTIES` (11: tempZona/Abasto/Retorno/Succion1/2, setpoint, ampCompresor1/2, ampAbanicos1/2, ampFan),
- `CARCAMO_HISTORY_PROPERTIES` (1: nivelCm),
- `DT_HISTORY_PROPERTIES` (2: pressurePsi, pressureBar — nota C4/REQ-G10-1: PSI y bar son series
  independientes, sin dataset compartido).

`detectEquipmentHistories(service, out, context)` recorre 4 niveles bajo el service (v4:
service → `Planta1..6` → monitor → equipo) `[CERT]` `UX/ChiHistoryHelper.java:486-567`:
1. Pre-carga `buildHistoryNameMap` (mapa `displayName.toLowerCase() → encodeToString()` de TODAS las
   histories) `[CERT]` `UX/ChiHistoryHelper.java:490,718-737`.
2. Obtiene los slots de monitor con `ChiEquipmentReader.getMonitorSlotNames()` y su arreglo de props
   correlativo `propsByMonitor = {UP, CARCAMO, DT}` `[CERT]` `UX/ChiHistoryHelper.java:495-500`.
3. Por cada Planta1..6 → cada monitor → cada propiedad-equipo que sea `BComponent` `[CERT]`
   `UX/ChiHistoryHelper.java:503-521`. La clave JSON del equipo es `ChiEquipmentReader.slotNameToId(equipSlot)`
   (empareja con los IDs de equipo del frontend); el `label` se lee como clave secundaria `[CERT]`
   `UX/ChiHistoryHelper.java:522-528`.
4. Por cada `propName` del tipo de equipo intenta emparejar un history en **dos vías con fallback**
   `[CERT]` `UX/ChiHistoryHelper.java:537-557`:
   - **Primaria (link-graph)**: `resolveHistoryViaLink(links, propName)`;
   - **Fallback (nombre)**: si la primaria da null → `resolveHistoryViaName(nameToId, label, slot, propName)`.
   Sólo se emiten equipos con al menos un match `[CERT]` `UX/ChiHistoryHelper.java:559-564`. Excepción
   global → `{}` `[CERT]` `UX/ChiHistoryHelper.java:571-575`.

### 174.6a — resolveHistoryViaLink (seguir el grafo de enlaces)

Recorre los `BLink[]` entrantes al componente-equipo; para el link cuyo `getTargetSlotName()` coincide con
`propName`, toma el `getSourceComponent()` (el punto de control fuente) y busca en él un `BHistoryExt` vía
`findHistoryExtId` `[CERT]` `UX/ChiHistoryHelper.java:586-610`. Cadena documentada: `Equipment property
<-- BLink <-- source control point --> BHistoryExt --> BHistoryId` `[CERT]`
`UX/ChiHistoryHelper.java:583-585`.

### 174.6b — findHistoryExtId (búsqueda recursiva del BHistoryExt)

Búsqueda recursiva de profundidad máxima `MAX_HISTORY_SEARCH_DEPTH = 3` `[CERT]`
`UX/ChiHistoryHelper.java:622-676`. Por nivel: **Pass 1** busca hijos que sean `BHistoryExt` directos,
habilitados (`getEnabled()`), con `getHistoryConfig().getId()` no-null → devuelve `histId.encodeToString()`
`[CERT]` `UX/ChiHistoryHelper.java:637-652`. **Pass 2** recursa a hijos `BComponent` que no sean
`BHistoryExt`, saltándose los slots `parent`/`source`/`target` para no subir por el grafo de equipo
`[CERT]` `UX/ChiHistoryHelper.java:654-673`. El comentario documenta que esto cubre `BHistoryExt` directo
(depth 1), bajo carpeta `extensions` (depth 2), y bajo carpetas arbitrarias tipo kitControl Ramp
(depth 2-3) `[CERT]` `UX/ChiHistoryHelper.java:612-621`.

### 174.6c — resolveHistoryViaName (fallback por coincidencia de nombre)

Construye dos candidatos en minúsculas: `label + "_" + propName` y `slot + "_" + propName` (omitiendo los
vacíos), y busca el primer `displayName` del `nameToId` que los CONTENGA como substring, devolviendo su
`encodeToString()` `[CERT]` `UX/ChiHistoryHelper.java:686-712`. Es la red de seguridad cuando no existe un
link explícito equipo→punto pero el operador nombró la history con la convención `<equipo>_<prop>`
`[INFER]` `UX/ChiHistoryHelper.java:686-694`.

## 174.7 — Notas de portabilidad y robustez

- `ChiHistoryHelper` fue **portado de `SnlsHistoryHelper`** con adaptaciones chihuahua: paquete,
  `HISTORY_PROPERTIES` partido por tipo de equipo, `detectEquipmentHistories` recorriendo los 3 monitores
  vía `ChiEquipmentReader`, y `escJson` delegando en `ChiJsonUtil.escapeJson` `[CERT]`
  `UX/ChiHistoryHelper.java:22-36,763-766`.
- Postura no-crash consistente: cada endpoint envuelve todo en try/catch y devuelve un JSON de error o un
  objeto vacío en lugar de propagar la excepción; los getters tipados (`getChildComponent`) devuelven
  `null` en fallo `[CERT]` `UX/ChiHistoryHelper.java:743-757`.
- `escJson` es la única serialización de strings; el JSON se arma a mano por concatenación en el
  `PrintWriter` (no hay librería JSON) `[INFER]` `UX/ChiHistoryHelper.java:112-118,763-766`.

## 174.8 — Connections

- **[Block 165]** — documenta los endpoints `/api/history*` del servlet desde el lado del routing/dispatch;
  este bloque profundiza en la implementación del helper detrás de esos endpoints.
- **[Block 170]** — `LiveHistoryBuffer` del frontend: el consumidor JS de `/api/historyData`, donde el
  stride cliente (`filterHistoryByRange`) es no-op cuando `targetPoints` ya coincide (§174.5b).
- **[Block 141]** — subsistema history de **Reflow (nmodsreflow)**: contraparte de comparación. chihuahua
  es autoría propia de fuente primaria; Reflow es el módulo decompilado. Comparar sus algoritmos de
  consulta/submuestreo de history es un eje de contraste entre ambos focus.
