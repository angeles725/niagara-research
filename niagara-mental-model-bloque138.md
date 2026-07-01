# Block 138 — nmodsreflow.77 (`-rt`): módulo, servicio y espina HTTP/WebSocket

> Research de **NiagaraMods Reflow v1.7.7 (build .75), módulo `nmodsreflow` runtime `-rt`**: el
> ESQUELETO backend — cómo el módulo se registra en Niagara, qué es `BReflowService` (el service
> central), cómo se resuelve por ORD, y cuáles son las dos superficies HTTP (`BaseServlet` REST +
> `SocketServlet` WebSocket). NO cubre la lógica interna de subsistemas (history/alarms/sync/backups/
> licensing) ni el contrato de datos frontend — esos son gaps propios del focus.
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Primer bloque del focus. Corpus language:
> Spanish (technical EN).
>
> Sources (primarias, JAR embarcado build .75 — bytecode que realmente corre):
> - `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow`
> - `META/` = `.../nmodsreflow77-rt/extracted/META-INF`
> - `UX/` = `.../nmodsreflow77-ux/vineflower/com/niagaramods/nmodsreflow/ux`
>
> Fuente relacionada (NO autoritativa, árbol de desarrollo divergente): `Reflow-Clean-177/` =
> `/home/cristian/modules/Prototipos/Reflow-Clean-177` — reconstrucción Gradle human-cleaned (nombres
> desofuscados); usada solo para resolver nombres ofuscados y como corroboración. Diverge del build 77
> (§138.6). DIFF forense 1.7.5↔1.7.7 preservado en `/home/cristian/modules/Prototipos/modulos/REFLOW-175-vs-177-DIFF.md`.
>
> Método: decompile Vineflower del JAR embarcado + lectura directa de `module.xml`/`MANIFEST.MF` +
> grep de símbolos. Markers:
> `[CERT]` fuente primaria local (`file:line`) ·
> `[CERT-doc]` documento oficial descargado ·
> `[CERT-web]` web oficial (URL+fecha) ·
> `[CERT-a]` fuente secundaria/foro ·
> `[INFER]` deducción.
>
> Capa 26 (OEM tercero NiagaraMods sobre N4). Connects [Block 50], [Block 51] (audit Reflow cross-stack
> previo), [Block 9] (UI stack / servlets Niagara), [Block 1] (estructura de módulo rt/ux).

---

## 138.1 — Identidad y registro del módulo `[CERT]`

El módulo se llama `nmodsreflow` (el nombre del focus del usuario, "nmdosreflow", es un typo). Datos del
descriptor embarcado (`META/module.xml` línea 1):

| Atributo | Valor | Cita |
|---|---|---|
| `moduleName` | `nmodsreflow` | `META/module.xml:1` |
| `name` (part rt) | `nmodsreflow-rt` | `META/module.xml:1` |
| `vendor` | `NiagaraMods` | `META/module.xml:1` |
| `vendorVersion` | `1.7.7.75` (= build 77 del focus) | `META/module.xml:1` |
| `preferredSymbol` | `nmflow` | `META/module.xml:1` |
| `runtimeProfile` | `rt` | `META/module.xml:1` |
| `buildMillis` | `1755192494644` | `META/module.xml:1` |
| `Implementation-Version` | `1.7.7.75`, `Sealed: true` | `META/MANIFEST.MF:2-4` |

El JAR está **firmado y sellado** — `MANIFEST.MF` lista digests SHA-256 por clase (`META/MANIFEST.MF`)
y `META/NMODS-C1.RSA`/`.SF` son la firma; el `Sealed: true` impide mezclar paquetes de otros JARs. El
JAR embebe dependencias shaded (Jackson, commons-lang3, commons-collections4) además del código propio
`com.niagaramods.nmodsreflow` `[CERT]` (los digests de `com/fasterxml/jackson/...` y `org/apache/commons/...`
en `META/MANIFEST.MF`).

**Dependencias Niagara** (12, todas `Tridium` `vendorVersion="4.6"` mínimo) `[CERT]` `META/module.xml:3-16`:
`bql-rt`, `bacnet-rt`, `schedule-rt`, `baja`, `driver-rt`, `alarm-rt`, `net-rt`, `platform-rt`, `web-rt`,
`history-rt`, `control-rt`, `box-rt`. Es decir: Reflow consume el modelo de datos (`baja`), drivers
(`driver-rt`/`bacnet-rt`), historia (`history-rt`), alarmas (`alarm-rt`), scheduling (`schedule-rt`),
consultas (`bql-rt`), web/servlets (`web-rt`) y subscription remota (`box-rt`) — el mismo stack que un
supervisor N4 estándar.

**Part `-ux`** declarada como módulo hermano `[CERT]` `META/module.xml` (`<modulePart name="nmodsreflow-ux" runtimeProfile="ux"/>`).

## 138.2 — Tipos registrados y permisos `[CERT]`

`META/module.xml` `<types>` registra 13 tipos exportados `[CERT]`:

| `name` (registry) | Clase | Rol |
|---|---|---|
| `ReflowService` | `BReflowService` | service central (§138.3) |
| `ReflowScheme` | `BReflowScheme` (`ordScheme="reflow"`) | resolución ORD (§138.4) |
| `DateRangeEnum` | `util.BDateRangeEnum` | enum rangos de fecha |
| `ReflowLicenseCommands` … `ReflowBQLCommands` | 8× `commands.BReflow*Commands` | agents `on nmodsreflow:ReflowService`, `requiredPermissions="r"` |
| `ReflowChannelService` | `http.sockets.BReflowChannelService` | pub/sub de canales WS |
| `ReflowWebSocketAcceptor` | `http.sockets.BReflowWebSocketAcceptor` | registro de sockets vivos |
| `ReflowSyncService` | `sync.BReflowSyncService` | sync de config multiusuario |

Los 8 `BReflow*Commands` (`License`, `File`, `Nav`, `CSV`, `History`, `Alarm`, `User`, `BQL`) son
**agents** montados sobre el `ReflowService` con permiso de lectura `[CERT]` `META/module.xml` (bloques
`<agent requiredPermissions="r"><on type="nmodsreflow:ReflowService"/></agent>`). Son la superficie de
comandos invocable vía el canal WebSocket/RPC, no vía el servlet REST.

**Permisos declarados** (`<permissions>`) `[CERT]` `META/module.xml`:
- `NETWORK_COMMUNICATION` con `hosts="*" ports="*" type="all"` — purposeKey: *"connects with NiagaraMods
  servers to authenticate and update licensing"*. Es el permiso que habilita el fetch de licencia a
  `api.niagaramodules.com` (subsistema licensing, gap del focus).
- `REFLECTION` — purposeKey: *"used in JSON object serialization to store configuration data"* (Jackson).

`module.palette` expone un solo objeto arrastrable: `ReflowService` bajo un `Folder` `[CERT]`
`META/../module.palette`.

## 138.3 — `BReflowService`: el service central `[CERT]`

`public class BReflowService extends BComponent implements BIService, BIRestrictedComponent` `[CERT]`
`RT/BReflowService.java:201`. Es el nodo raíz del módulo dentro de la station (un `BComponent` de
servicio, instalado bajo `Services`). `BIRestrictedComponent` significa que su ubicación en el árbol
está restringida — `checkParentForRestrictedComponent` delega en el contrato del framework `[CERT]`
`RT/BReflowService.java:781-783`.

**Superficie de slots**: ~30 properties + 6 actions declaradas vía `@NiagaraProperties`/`@NiagaraActions`
`[CERT]` `RT/BReflowService.java:44-200`. Agrupables en:

| Grupo | Properties (ejemplos) | Cita |
|---|---|---|
| Licensing/límites | `LicenseStatus`, `licenseType="trial"`, `buildingLimit=1`, `equipmentLimit=10`, `floorLimit=3`, `pageLimit=3`, `weatherMapsEnabled=false`, `niagaraLicensing`, `stationType`, `licenseStationType` | `:44-136` |
| Sub-servicios (frozen) | `webSocketAcceptor=new BReflowWebSocketAcceptor()`, `channelService=new BReflowChannelService()`, `syncService=new BReflowSyncService()` | `:137-152` |
| Caché | `webCache=true`, `historyCache=false`, `historyCacheTTL=3600`, `historyGroupCacheRefresh=true`, `historyGroupRefreshTime=00:00:00` | `:153-181` |
| Backups | `dailyBackups=false`, `incrementalBackups=true` | `:91-101` |
| Operación/seguridad | `crossOriginHosts=""`, `redirectReflowView`, `multiUserConfig`, `socketTimeout=10`, `hasModernSecurityPolicy` | `:102-184` |

Las 3 properties `webSocketAcceptor`/`channelService`/`syncService` **componen** los sub-servicios como
hijos frozen del service (flags=5) `[CERT]` `RT/BReflowService.java:220-222` — Reflow es un service con
sub-servicios embebidos, no servicios separados en el árbol.

**Ciclo de vida** (callbacks `BComponent`) `[CERT]`:
- `started()` / `stationStarted()` (idénticos): `doRefreshLicense()` → `startLicenseRefreshTimer()` →
  detecta política de seguridad moderna (`BModule.getClassVersion(BObject.class).minor() > 9`, es decir
  N4.10+) → registra `ServiceCommand` en el WebSocket acceptor → arranca el history-group cache si está
  habilitado. `RT/BReflowService.java:496-520`.
- `changed(Property, Context)`: reacciona a `multiUserConfig` (recarga syncService), `historyCache`,
  `historyGroupCacheRefresh`, `historyGroupRefreshTime` (re-arma timers). `RT/BReflowService.java:522-550`.
- Timer de licencia: `Clock.schedulePeriodically(..., BRelTime.make(86400000L), ticketExpired, null)` —
  refresh + backup diario cada **24 h** `[CERT]` `RT/BReflowService.java:562-568, 597-616`.

`getAgents(cx)` sube al tope las vistas `nmodsreflow:ReflowConfig` y `nmodsreflow:Reflow` `[CERT]`
`RT/BReflowService.java:750-755` — al abrir el service en Workbench/web, la vista por defecto es la UI
Reflow. Ícono `module://nmodsreflow/icons/reflow.png` `[CERT]` `RT/BReflowService.java:239`.

## 138.4 — Resolución ORD: `BReflowScheme` (`reflow:`) `[CERT]`

Reflow registra un **ORD scheme propio** `reflow` `[CERT]` `RT/BReflowScheme.java:17-20`
(`@NiagaraType(ordScheme="reflow")`, `extends BOrdScheme`, singleton `INSTANCE`). Su `resolve()` no
navega por handle: **itera todos los componentes del `BComponentSpace` y devuelve el primero cuyo tipo
`is(BReflowService.TYPE)`**, lanzando `UnresolvedException("Service not found")` si no hay `[CERT]`
`RT/BReflowScheme.java:32-54`. Efecto: un ORD `reflow:` resuelve al `ReflowService` sin conocer su ruta
exacta — desacopla el resto del módulo de dónde esté instalado el service en el árbol.

## 138.5 — Las dos superficies HTTP `[CERT]`

Reflow expone su UI/datos por dos servlets en el paquete `http`:

**(A) `BaseServlet` — REST/estático** `extends javax.servlet.http.HttpServlet` `[CERT]`
`RT/http/BaseServlet.java:37`. En build 77 implementa **solo `doGet` + `doPost`** `[CERT]`
`RT/http/BaseServlet.java:55, 270`. Routing por `req.getPathInfo()` (cadena de `equals/startsWith`):

| Método | Ruta | Response |
|---|---|---|
| GET | `/` → `/index.html`; `/demo/.*` → `/index.html` | SPA fallback |
| GET | `/config`, `/demos`, `/weather-map` | `ConfigResponse`/`DemoResponse`/`WeatherMapResponse` |
| GET | `/station/equipment-notes`, `/station/images`, `/station/files`, `/station/image-library`, `/station/schedules` | responses homónimos |
| GET | `/station/backups[/create|/apply|/destroy|/rename|/reset]` | `Backup*Response` |
| GET | `/station/histories[/<name>]`, `/station/history-data`, `/station/history-groups`, `/station/alarms/csv` | history/alarm responses |
| POST | `/config_update`, `/config_delta`, `/station/equipment-notes-update`, `/station/alarms/query` | update/query responses |
| — | no-match | `FileResponse.serve(path)` y si falla → `/index.html`, y si falla → 404 | `:242-264` |

El fallback final sirve `/index.html` ante cualquier ruta desconocida `[CERT]`
`RT/http/BaseServlet.java:249-263` — es el patrón **SPA deep-linking** (el frontend Vue enruta client-side).
`AlarmQueryResponse` está en `doPost` (no GET) `[CERT]` `RT/http/BaseServlet.java:285-287` — coincide con
el cambio 1.7.5→1.7.7 documentado en el DIFF forense (GET→POST) `[CERT-a]` `REFLOW-175-vs-177-DIFF.md §1`.

`setContentSecurityPolicy(resp)` se aplica en ambos métodos cuando `hasModernSecurityPolicy` `[CERT]`
`RT/http/BaseServlet.java:38-53`: CSP con `default-src 'self' ws: wss: blob: data:` + dominios
`niagaramodules.com`/`niagaramods.com`/`niagaramods.io`/`nmx.to`/`nmods.to` + `crossOriginHosts` +
`*.mapbox.com` + `'unsafe-inline' 'unsafe-eval'`, y `connect-src *`.

**(B) `SocketServlet` — WebSocket** `extends org.eclipse.jetty.websocket.servlet.WebSocketServlet` `[CERT]`
`RT/http/sockets/SocketServlet.java:12`. Configura la policy Jetty: `idleTimeout=60000ms`,
`maxTextMessageSize=262144`, `maxTextMessageBufferSize=65536`, `maxBinaryMessageSize=131072` `[CERT]`
`RT/http/sockets/SocketServlet.java:13-27`. Cada upgrade crea un `BReflowWebSocketAcceptor.ReflowWebSocket`
con id incremental (`AtomicLong`) `[CERT]` `RT/http/sockets/SocketServlet.java:29-52`. Los datos vivos y
comandos van por aquí (el `ServiceCommand` que responde `isLicensed` se registra en el acceptor `[CERT]`
`RT/BReflowService.java:502, 785-818`).

**Montaje del servlet en Jetty — GAP.** Ni `BaseServlet` ni `SocketServlet` se registran como tipo
Niagara en `module.xml`, ni extienden `BWebServlet`; el mecanismo por el que reciben un path bajo la
station (p.ej. `/reflow/...`) no está en el módulo `-rt` inspeccionado `[CERT]` (grep sin resultados de
`BWebServlet`/`addServlet`/`getServletName`). Hipótesis: se registran vía el `web-rt` framework o un
`BServletView` no visto todavía `[INFER]`. → registrado como gap R3.

## 138.6 — Loader `-ux` y divergencia de `Reflow-Clean-177` `[CERT]`

El part `-ux` son **3 clases loader**, todas `BSingleton implements BIJavaScript, BIFormFactorMax`,
agents `on nmodsreflow:ReflowService` `[CERT]`:

| Clase | JsInfo (recurso JS) | Permiso agent | Cita |
|---|---|---|---|
| `BReflow` | `module://nmodsreflow/niagara/reflow.js` | `r` | `UX/BReflow.java` |
| `BReflowConfig` | `module://nmodsreflow/niagara/reflow_config.js` | `rw` | `Reflow-Clean-177/.../ux/BReflowConfig.java` |
| `BReflowRedirect` | `module://nmodsreflow/niagara/reflow_redirect.js` | `r` | `Reflow-Clean-177/.../ux/BReflowRedirect.java` |

Cada una solo entrega un `JsInfo` con un recurso `.js` — **no hay lógica Java de vista**, la UI real es
el bundle JS `[CERT]`. Confirma el hallazgo previo del corpus: *"-ux es solo iframe/JS loader, servlets
reales en -rt"* [Block 50]/[Block 51].

**Divergencia [CERT]:** el árbol `Reflow-Clean-177` NO es idéntico al build 77 embarcado. Su
`BaseServlet` añade `doPut`, `doDelete`, `doOptions`, un `LOGGER` y un `http/util/CsrfGuard` con una rama
`/csrf-probe` marcada *"REMOVE before commit"* `[CERT]`
`Reflow-Clean-177/.../http/BaseServlet.java:281,297,313` + `.../http/util/CsrfGuard.java:27` — features
ausentes del JAR .75. Por eso las citas `[CERT]` de build 77 se anclan al decompile embarcado, y
`Reflow-Clean-177` se usa solo para nombres desofuscados (resuelve `method_311`→`put`, etc., ver DIFF
forense §"Ruido de Decompilador").

## 138.7 — Connections

- **[Block 50], [Block 51]** — audit Reflow cross-stack previo (v1.7.5): establecieron el par contractual
  `frontend ↔ -ux` y que los servlets reales viven en `-rt`. Este bloque abre el focus dedicado al
  build 77 y ancla el esqueleto `-rt` con citas `file:line` sobre el JAR embarcado.
- **[Block 9]** — UI stack Niagara (`BWebService`/`BWebServlet`/BajaScript): el gap R3 (montaje del
  servlet) se resolverá cruzando contra el mecanismo de servlets de `web-rt` documentado ahí.
- **[Block 1]** — estructura de módulo rt/ux + `module.xml`: `nmodsreflow` es una instancia OEM de tercero
  de ese patrón (part rt + part ux, tipos, permisos, ORD scheme propio).
- **[Block 2]** — licenciamiento Niagara: `BReflowService` implementa un licensing propio (RSA + host
  binding + `api.niagaramodules.com`) paralelo al de Tridium; es un gap dedicado del focus (R4).
