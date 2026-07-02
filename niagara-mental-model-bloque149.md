# Block 149 — nmodsreflow.77 (`-rt`): contrato de datos HTTP (router `BaseServlet`, shapes JSON, y nuevos sinks que extienden la tesis)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), el contrato de datos HTTP del runtime `-rt`**: cómo
> `BaseServlet` rutea, qué shape JSON emite cada `*Response` que el bundle `-ux` consume, y los serializers
> que definen esos shapes. Cubre `BaseServlet` + las 9 Response no documentadas antes (EquipmentNote(+Update),
> File, FileTree, ImageLibrary, ImageList, SchedulesData, WeatherMap, Demo) + los 4 serializers. Las Response
> ya cubiertas (History/Alarm/Backup/Config, B141-B145) sólo se referencian. Cierra el gap **R12**.
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Cierra el gap **R12** (último de superficie).
> Corpus language: Spanish (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow`
> `RESP/` = `RT/http/responses`.
>
> Método: barrido citado (delegado) + verificación directa de router/sinks. Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción anclada a líneas `[CERT]`.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 50]/[Block 51] (bundle `-ux` que consume estos shapes),
> [Block 138] (el servlet base y el service central), [Block 141]-[Block 145] (Response ya documentadas),
> [Block 144]/[Block 145]/[Block 147] (los nuevos sinks extienden el patrón traversal), [Block 139] (HostID
> del licensing, ahora fugado por WeatherMap).

---

## 149.1 — El router `BaseServlet`: sin envelope común, sin gate de auth `[CERT]`

`BaseServlet` despacha con un **ladder `if/else if` sobre `req.getPathInfo()`** repartido entre `doGet`
(`RT/http/BaseServlet.java:57`) y `doPost` (`:272`); cada rama matchea un path exacto y llama a
`*Response.serve(req,resp)`. `[INFER]` **No hay envelope común** (`{status,data,error}`): cada Response
escribe su propio body crudo; el content-type se setea por-Response. Fall-through GET → `FileResponse` →
`/index.html` → 404.

**Gate de auth — hallazgo negativo decisivo:** grep de `getPermissionsFor`/`requiredPermissions`/
`checkPermission` sobre todo `BaseServlet` → **cero** `[CERT]` (count 0). `[INFER]` El router **no agrega**
ningún gate de permiso que las Response no tuvieran — la tesis zero-auth-at-REST de B143/B144/B145 no se
revisa, se **refuerza a nivel router**. (Contrasta con la capa de comandos B146, que sí gatea a `"r"`; el REST
no tiene ni eso.)

**CSP muy permisiva** `[CERT]` `BaseServlet.java:41-50` (sólo si `getHasModernSecurityPolicy()`): incluye
`'unsafe-inline' 'unsafe-eval'` `:48` y `connect-src *` `:50`. `[INFER]` habilita XSS/exfil si algún reflejo
de input llega al DOM (ver §149.4). Errores mayormente `text/html` que **reflejan el `path`** sin escapar.

## 149.2 — Shapes JSON de las Response no cubiertas `[CERT]`

| Response | Método | Path | Shape top-level | Cita |
|---|---|---|---|---|
| EquipmentNote | GET | `/station/equipment-notes` | `[ <note>, … ]` (array opaco; `[]` si vacío) | `EquipmentNoteResponse.java:24` |
| EquipmentNoteUpdate | POST | `/station/equipment-notes-update` | sin body (200/500) | `EquipmentNoteUpdateResponse.java:24` |
| FileResponse | GET | fall-through | bytes crudos (mimeType) | `FileResponse.java:49,54` |
| FileTree | GET | `/station/files` | `[ {ord,name,icon,type,ext?,children?} ]` recursivo | `FileTreeResponse.java:43-54` |
| ImageLibrary | GET | `/station/image-library` | igual tree (`ord`=filePath) | `ImageLibraryResponse.java:45-57` |
| ImageList | GET | `/station/images` | `[ {ord,name} ]` flat | `ImageListResponse.java:50-53` |
| SchedulesData | GET | `/station/schedules` | `[ <Json.component(schedule)> ]` (BQL) | `SchedulesDataResponse.java:17,22` |
| WeatherMap | GET | `/weather-map` | bytes `image/png` | `WeatherMapResponse.java:44,64` |
| Demo | GET | `/demos` | passthrough `demo.json`; `{status:404}` si ausente | `DemoResponse.java:17-18,37` |

`[INFER]` Patrón: los shapes son arrays JSON planos o árboles recursivos `{ord,name,type,...}` para navegación
de archivos/imágenes/schedules; `Json.component` (B148) provee la serialización opaca por-componente.

## 149.3 — Serializers = definición de shapes `[CERT]`

- **HistoryDeviceSerializer**: `{fullPath:str, title:str, devices?:[Device], children?:[Folder], histories?:[str]}`
  (opcionales sólo si no vacíos) `[CERT]` `history/json/HistoryDeviceSerializer.java:28-29,52-60`.
- **HistoryFolderSerializer**: `{fullPath:str, title:str, children?:[Folder], histories?:[str]}` `[CERT]`
  `HistoryFolderSerializer.java:27-28,46-50`.
- **HistoryRecordSerializer**: `{time:num|str, value:str|bool, status?:str, label?:str}` — `time` millis o ISO
  según `options.millis` `[CERT]` `HistoryRecordSerializer.java:42-44`.
- **ReflowSyncResponseSerializer**: `{...dynamicFields, ticket:str|null, data:obj|null}` `[CERT]`
  `sync/ReflowSyncResponseSerializer.java:23-38` — es lo más parecido a un envelope, pero **sólo** lo usa el
  subsistema sync (B143), NO `BaseServlet`.

## 149.4 — Nuevos sinks que EXTIENDEN la tesis de seguridad `[CERT]`

R12 es contrato de datos, pero el barrido destapó sinks nuevos que suman a la cadena de B144/B145/B147:

- **EquipmentNote: traversal por header, lectura Y escritura** `[CERT]`. Read: `Equipment-Id` header →
  `NOTE_FOLDER + fileName + ".json"` → `findFile(new FilePath(location))` `EquipmentNoteResponse.java:20-24`.
  Write (POST, **primitiva de escritura**): mismo header → `makeFile(new FilePath(path))` con el body del
  cliente `EquipmentNoteUpdateResponse.java:20,24`, **sin `requiredPermissions`** `[INFER]`. Un `Equipment-Id`
  con `../` da traversal de lectura/escritura de `.json` arbitrario — extiende B144/B145 con un vector por
  **header** (no query param).
- **WeatherMap: sink outbound SSRF-flavored + fuga de HostID** `[CERT]` `WeatherMapResponse.java:24,82`: el
  query param `config` del cliente se concatena crudo en la URL upstream
  `"http://weather.niagaramodules.com/maps" + config + "?host=" + getHostId()`, y `getHostId()` es el HostID de
  la plataforma `[CERT]` `:117-119`. `[INFER]` control parcial de la URL saliente + exfil del HostID de la
  station off-box (el mismo HostID que ancla el licensing de B139).
- **FileResponse: traversal read module-scoped** `[CERT]` `FileResponse.java:49`:
  `BOrd.make("module://nmodsreflow/rc" + path)` — más angosto (scoped al namespace `rc` del módulo vía BOrd),
  no al FileSpace de la station.
- **FileTree: exposición del árbol completo de la station sin auth** `[CERT]` `FileTreeResponse.java:43-54`
  (root `^`, recursivo) — disclosure del layout completo de archivos.
- **Input reflejado + CSP permisiva** (§149.1): errores HTML que reflejan `path` + CSP con `unsafe-inline`
  `[INFER]` → superficie XSS.

## 149.5 — Connections y cierre de superficie

- **[Block 50]/[Block 51]** — el bundle `-ux` consume estos shapes (arrays/árboles `{ord,name,type}`); B51 ya
  había mapeado parcialmente el config.json y los responses de history/alarms.
- **[Block 138]** — `BaseServlet` es el servlet base montado por el service central (B138 §138.4, mount vía
  `web.xml` `/*`, cerrado en B140).
- **[Block 141]-[Block 145]** — Response ya documentadas; acá se completa el resto del contrato.
- **[Block 144]/[Block 145]/[Block 147]** — los sinks de §149.4 extienden el traversal (ahora también por
  header y outbound) que B147 mostró sin mitigación aguas arriba.
- **[Block 139]** — el HostID que WeatherMap fuga es el mismo que ancla el binding del licensing.

**Nota de seguridad cross-focus (R12 SUMA sinks, no cambia el veredicto):** el router `BaseServlet` **no
agrega** gate de auth (grep-negativo) — refuerza la tesis a nivel router. Los sinks nuevos (EquipmentNote
read/write por header, WeatherMap SSRF+HostID leak, FileTree disclosure, CSP `unsafe-*`) se agregan a la cadena
ya cerrada end-to-end en B147 §147.4. `[INFER]` Con R12 el mapeo de superficie del focus está **completo**: el
único gap residual es R3 (base `/module/<name>/`, casi-cerrado). El **NEXT-ACTION** es el bloque de síntesis
cross-focus de seguridad (nmodsreflow × platform-security) como bloque TERMINAL, consolidando B139/B141/B142/
B143/B144/B145/B146/B147/B149 contra `skipModuleValidation` (B75/B113) y el licensing bypass (B139).
