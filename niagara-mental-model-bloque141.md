# Block 141 — nmodsreflow.77 (`-rt`): subsistema history (cache GZIP en disco, threading privilegiado, ghost-subscribe, grouping)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), paquete `history/` del runtime `-rt`**: cómo Reflow
> lista, consulta y cachea el historial de Niagara. Cubre `HistoryIO` (cache), `HistoryData` (motor de
> query + threading privilegiado), `HistoryGhostSubscriber` (subscribe efímero para montar histories
> remotas), `HistoryGroups` (árbol de grupos/dispositivos) y `HistoryList` (listado/paginación), más el
> gancho de la capa HTTP `http/responses/History*Response` que consume la cache. NO cubre el contrato JSON
> completo frontend↔-rt (R12) ni el `BReflowHistoryCommands` como superficie de comandos (R10).
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Cierra el gap **R5**. Corpus language: Spanish
> (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow`
> `HIST/` = `RT/history`. Capa HTTP: `RT/http/responses/`.
>
> Método: decompile Vineflower del JAR embarcado + lectura directa + grep de callers/tokens. Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción anclada a líneas `[CERT]`.
> Nota de decompilado: Vineflower dejó nombres ofuscados en algunas llamadas Jackson/JSON
> (`method_301`=`put`, `field_284`=el `Context` de la subscripción); se citan tal cual aparecen.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 138] (service central `BReflowService`: la cache y el
> TTL salen de su config), [Block 139] (licensing: el `doPrivileged` de este subsistema reabre la cuestión
> de firma/validación), [Block 140] (mismo patrón `AccessController.doPrivileged` que el dispatch del canal
> WS), [Block 75]/[Block 113] (skipModuleValidation / code-signing).

---

## 141.1 — Mapa del subsistema `[CERT]`

| Clase | Rol | Estado | Cita |
|---|---|---|---|
| `HistoryIO` | Cache layer (lee/escribe blobs gzip en disco) | static-util, sin estado salvo 2 ORD | `HIST/HistoryIO.java:16-18` |
| `HistoryData` | Motor de query (timeQuery/scan/getLastRecord) + serialización | static-util | `HIST/HistoryData.java:53,128,217,276` |
| `HistoryGhostSubscriber` | Subscribe efímero "fire-once" para montar histories no-locales | `extends javax.baja.sys.Subscriber` | `HIST/HistoryGhostSubscriber.java:9` |
| `HistoryGroups` | Árbol de grupos/dispositivos (nav del history space) | static-util, sin estado | `HIST/HistoryGroups.java:81,103` |
| `HistoryList` | Listado + filtros + paginación | clase con estado (`dirty`, `list[]`) | `HIST/HistoryList.java:24-30` |

Punto de anclaje común: **todo cuelga de `BOrd.make("history:").resolve().get()`** → `BHistoryDatabase`
`[CERT]` `HistoryData.java:53,97,199,268`. No hay lookup por `BHistoryId` cacheado; la resolución es ORD
en caliente en cada request.

## 141.2 — HistoryIO: la cache es DISCO gzip, no memoria `[CERT]`

Contra lo que el nombre "cache" sugiere, **no hay ningún mapa en memoria** (ni `ConcurrentHashMap`, ni
`byte[]` cache, ni keying por history-id/ORD). La cache son **dos archivos gzip** bajo el station home,
keyed sólo por nombre fijo `[CERT]` `HistoryIO.java:16-18`:

- `HISTORY_CACHE = "^reflow/cache/history.cache"` — un único blob para TODA la lista de histories.
- `GROUP_CACHE  = "^reflow/cache/history-groups.cache"` — un único blob para el árbol de grupos.

Se escriben vía el file space de Baja (`makeFile`/`findFile` sobre `FilePath`) `[CERT]`
`HistoryIO.java:44-49,73`, comprimidos con `java.util.zip.GZIPOutputStream` `[CERT]`
`HistoryIO.java:9,35-38,75`. **No hay `GZIPInputStream`** `[CERT]` (grep negativo): la descompresión se
delega al browser — la respuesta sirve los bytes gzip crudos con `Content-Encoding: gzip` (§141.6).

**Validez = TTL wall-clock** desde la config del service `[CERT]` `HistoryIO.java:26-27`:

```
int ttl = service.getHistoryCacheTTL() * 1000;
return cacheFile.getLastModified().getMillis() + ttl > System.currentTimeMillis();
```

El TTL sale de `BReflowService` (B138) → la cache es global-por-blob, no por history. Invalidación manual =
borrar ambos archivos (`clearCache()`) `[CERT]` `HistoryIO.java:41-57`.

**Threading:** `refreshHistoryGroupCache()` lanza un **`Thread` crudo sin pool** por llamada `[CERT]`
`HistoryIO.java:59-63` (task `WriteHistoryGroupCacheTask`, `HistoryIO.java:89`): sin executor, sin
coalescing/debounce, sin guard de dedup. `[INFER]` bajo carga concurrente esto crea threads sin cota.

## 141.3 — HistoryData: query + threading privilegiado `[CERT]`

`HistoryData` resuelve la DB por ORD, abre `HistorySpaceConnection` y corre `timeQuery`/`scan`/
`getLastRecord` con cursor + try/finally `[CERT]` `HistoryData.java:128,217,276`. El punto clave es cómo
`fromComponent()` envuelve el trabajo: un `AccessController.doPrivileged` que **spawnea un Thread y lo
joinea** — corre la query sincrónica pero bajo privilegio elevado `[CERT]` `HistoryData.java:69-76`:

```
AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(new PrivilegedExceptionAction() {
   public run() {
      Thread thread = new Thread(task, "BReflowHistoryData.FromComponentTask.Task");
      thread.start();
      thread.join();
      return null;
   }
}));
```

`[INFER]` **Alcance de privilegio muy ancho**: el bloque privilegiado no envuelve una sola llamada API
sensible, sino todo el ciclo del thread y —vía ese thread— la query + BQL + serialización completa. Los
parámetros (`histories`, `compareHistories`, `range`, `limit`, fechas) llegan crudos del query string HTTP
(`HistoryChartDataResponse`), de modo que ORDs/BQL influenciados por el cliente se ejecutan bajo privilegio
elevado. Es el **mismo patrón** que el dispatch del canal WS (B140 §140), no un caso aislado.

**BQL por concatenación de strings** `[CERT]` `HistoryData.java:319,357`:

```
"...|bql:select * from history:HistoryRecord where timestamp.millis < " + start + " order by ..."
```

`start`/`stop` son `long` (riesgo de inyección bajo), pero `config.getId()` se concatena sin escapar en el
mismo ORD `[INFER]`.

**Modelo de record:** sin agregación min/max/range; los records se aplanan por tipo a JSON. El flag "oculto"
del trend es `getBit(4)` — los records ocultos se saltan `[CERT]` `HistoryData.java:330,342,368`.

## 141.4 — HistoryGhostSubscriber: el "ghost" (subscribe fire-once) `[CERT]`

Cuando la history-id no contiene el nombre de la station local, `HistoryData` llama `subscribeToHistory(h)`
`[CERT]` `HistoryData.java:102,204,567`. Ese subscribe usa un `Context` con facet asíncrono y un subscriber
efímero `[CERT]` `HistoryData.java:573`:

```
Context subContext = new BasicContext(ctx, BFacets.make("asyncHistorySubscribe", true));
```

El `HistoryGhostSubscriber` (`extends Subscriber`) **se auto-desuscribe en el primer evento** `[CERT]`
`HistoryGhostSubscriber.java:9,19-21`:

```
public void event(BComponentEvent bComponentEvent) {
   this.history.unsubscribe(this, this.field_284);
}
```

`[INFER]` Es un "ghost" = subscriber descartable para forzar el montaje/carga async de una history remota o
no-montada, que se termina solo al primer evento. **Riesgo:** si el evento nunca dispara, la subscripción
se filtra (nunca desuscribe); se crea un ghost por cada history no-local por request.

## 141.5 — HistoryGroups y HistoryList `[CERT]`

**HistoryGroups** NO son favoritos de usuario: reflejan el **nav tree del propio history space** de Niagara
`[CERT]` `HistoryGroups.java:81,99-103`. Nombres de grupo vía `BHistoryService.getHistoryGroupNames(space)`
`[CERT]` `:81`; grupos = `BHistoryFolder` en el nav, **excluyendo explícitamente `"Default (All)"`** `[CERT]`
`:103`; dispositivos vía `getNavChildren()` `[CERT]` `:26,32`. No los persiste esta clase — la única forma
persistida es el blob gzip de `GROUP_CACHE` (§141.2).

**HistoryList** tira TODAS las histories de la DB y filtra en memoria, luego rebana para paginar (sin paging
en DB) `[CERT]` `HistoryList.java:24-30`. **Bug de paginación** `[CERT]` `HistoryList.java:203,225,280`:

```
total_pages = (int)Math.ceil(this.total / this.limit);
```

`total` y `limit` son `int` → la división entera ocurre ANTES de `ceil`, así que el techo nunca redondea
hacia arriba (p.ej. 150/100 → 1 página). Presente en los tres builders JSON.

## 141.6 — Cómo la capa HTTP consume la cache `[CERT]`

`HistoryListResponse` está gated por `service.getHistoryCache()` `[CERT]` `HistoryListResponse.java:30`. Si
existe un archivo de cache fresco y el cliente mandó `Accept-Encoding: gzip`, streamea los bytes gzip crudos
con `Content-Encoding: gzip` `[CERT]` `HistoryListResponse.java:26,38`. En el cold path usa un
**`TeeOutputStream`** (Apache Commons IO) para escribir cache y respuesta simultáneamente `[CERT]`
`HistoryListResponse.java:20,66`. `HistoryGroupsResponse` replica el patrón sobre `GROUP_CACHE`.

`[INFER]` **Gotcha:** si `getHistoryCache()` está ON pero el cliente NO acepta gzip mientras existe archivo
de cache, `writeCache` queda false y sirve datos en vivo — la cache sólo se puebla en un primer miss con
cliente gzip-capable.

## 141.7 — APIs Niagara usadas (cross-cutting) `[CERT]`

De `javax.baja.history.*`: `BHistoryDatabase`, `HistorySpaceConnection`, `BIHistory`, `BHistoryConfig`,
`BHistoryId`, `BHistoryRecord`, `BHistoryService`, `BHistorySpace`, `BHistoryDevice`, records
`BNumeric/Boolean/String/Enum/TrendRecord`, `BTrendFlags`, `BEnumRange` `[CERT]`
`HistoryData.java:22-33`, `HistoryGroups.java:11-13`. De `com.tridium.*`: `com.tridium.history.BHistory`
(impl concreta, para el ghost-subscribe y el check `is(BHistory.TYPE)`) `[CERT]` `HistoryData.java:6,572`,
`com.tridium.history.BNumeric64BitTrendRecord` (rama numérica 64-bit) `[CERT]` `HistoryData.java:7`,
`BHistoryFolder`/`BRootHistoryFolder` `[CERT]` `HistoryGroups.java:9-10`, y `com.tridium.json.JSONObject/
JSONArray`.

## 141.8 — Fallas y riesgos (line-anchored) `[INFER]`

Cada ítem ancla a líneas `[CERT]` verificadas arriba:

- **Privilegio ancho sobre input atacante-controlado**: `doPrivileged` envuelve query params del query
  string HTTP — `HistoryData.java:69-76`.
- **BQL por concat**: `HistoryData.java:319,357` (`config.getId()` sin escapar).
- **Race de escritura de cache sin lock**: requests concurrentes en cold cache abren cada uno su
  `GZIPOutputStream` sobre el MISMO archivo — `HistoryIO.java:37-38` vía `HistoryListResponse.java:66`.
- **Threads crudos sin cota**: `HistoryIO.java:62` (por refresh) y `HistoryData.java:71` (por chart request).
- **`SimpleDateFormat`/`DecimalFormat` estáticos no-thread-safe usados desde threads spawneados**:
  `HistoryData.java:49-50`.
- **NPE con `valueFacets` ausente**: `facets.getFacet(...)` sin guard — `HistoryData.java:397,504,525`.
- **Subscription leak** si el evento del ghost nunca dispara: `HistoryGhostSubscriber.java:19-21`.
- **Page count roto** (int-divide antes de ceil): `HistoryList.java:203,225,280`.

**Hallazgos negativos (ausencias verificadas por grep):** sin cache en memoria/`ConcurrentHashMap`/`byte[]`
en todo el subsistema; sin `GZIPInputStream` (el browser infla); sin `Executor`/pool/`Clock.schedule` — sólo
`new Thread` crudo; sin BQL en `HistoryList`/`HistoryGroups`.

## 141.9 — Connections

- **[Block 138]** — el TTL y el on/off de la cache (`getHistoryCacheTTL()`, `getHistoryCache()`) son props
  de `BReflowService`; este subsistema es un consumidor de esa config central.
- **[Block 140]** — mismo patrón `AccessController.doPrivileged` que el dispatch del canal WebSocket. En
  ambos casos Reflow eleva privilegio alrededor de trabajo alimentado por input del cliente (WS command
  dispatch allá, history query acá). Es un patrón de módulo, no un caso aislado.
- **[Block 139]** — licensing. El `doPrivileged` de este bloque corre bajo la garantía de que el módulo está
  firmado/validado por la plataforma; ver la nota de seguridad cross-focus abajo.
- **[Block 75]/[Block 113]** — `skipModuleValidation` / code-signing.

**Nota de seguridad pendiente (cross-focus, para síntesis):** el subsistema history (y el dispatch WS de
B140) apoyan su `AccessController.doPrivileged` en el supuesto de que el JAR OEM está firmado y validado por
la plataforma Niagara. Pero B139 documentó que el licensing de Reflow tiene un **bypass** (validación RSA que
puede saltarse). Eso es ortogonal a la validación de MÓDULO (firma del `.jar`), que B75/B113 mostraron que
puede desactivarse vía **`skipModuleValidation`**. `[INFER]` La combinación —un módulo que corre bloques
privilegiados anchos + una plataforma donde la validación de módulo puede apagarse + un licensing con
bypass— merece un bloque de síntesis cross-focus (nmodsreflow × platform-security) que evalúe la superficie
de ataque agregada. Queda ANOTADA aquí; no se resuelve en R5 (read-only, y cruza focuses).
