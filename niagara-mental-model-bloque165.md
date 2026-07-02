# Block 165 — chihuahua MX60 (`-ux`): superficie servlet HTTP completa (dispatch puro, guards, contrato de datos)

> **QUÉ documenta:** la superficie HTTP completa del servlet único del módulo **chihuahua** (MX60 Chihuahua BMS Dashboard): cada endpoint GET/POST, su método, su guard de autenticación/autorización, la forma de la respuesta, el diseño de *dispatch puro* (`RouteAction` + `route()` testeable off-station), los guards centrales (traversal→404, `/api/*` sin `X-Requested-With`→302, csrf-probe exento), el contrato de error JSON hecho a mano (try/catch→500 `{"error":...}`), los security headers y el `ReentrantLock` por-ord que produce **HTTP 423 Locked** en el setpoint.
> **Foco:** **chihuahua** (`-ux`), MX60 Honeywell.
> **Idioma del corpus:** Español.
> **Fuentes (rutas reales bajo `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/`):**
>   - alias **UX** = `chihuahua-ux/src/com/angeles/chihuahua/ux/`
>   - `UX/BChiServlet.java` (2166 líneas — `BWebServlet`, doGet/doPost, handlers, `setApiHeaders`, envelope de error, static serving, headers seguridad, 423 Locked)
>   - `UX/ChiServletDispatch.java` (685 líneas — jerarquía sellada `RouteAction`, `route()`, guards traversal/XHR/csrf-probe, `extractOrdSegment`, `getContentType`)
> **Marcadores:** `[CERT]` = leído en fuente primaria con cita `archivo:línea`. `[INFER]` = deducción a partir de la fuente.
> **Nota:** `.env.local` **NO** fue leído (fuera de alcance por instrucción explícita).
> **Capa 26.** Continúa [Block 163] / [Block 164].

---

## 165.1 — Arquitectura de dispatch puro (`RouteAction` + `route()` off-station)

El módulo usa **un solo servlet** (`BChiServlet extends BWebServlet`, `getServletName()` → `"mx60"`) que rutea todo lo que cae bajo el prefijo `/mx60/` `[CERT] UX/BChiServlet.java:51-52,142-146`. La decisión de ruteo NO vive en el servlet: está **totalmente delegada** a una función pura.

- `ChiServletDispatch` es `final`, constructor privado, y su única responsabilidad es decidir rutas `[CERT] UX/ChiServletDispatch.java:23-25`.
- `route(method, path, headerLookup, paramLookup)` es una **función pura** que recibe strings y dos `Function<String,String>` (adaptadores plain-Java para headers y query params), y devuelve una `RouteAction` `[CERT] UX/ChiServletDispatch.java:379-383`.
- El servlet construye esos adaptadores con method-references sobre el `HttpServletRequest` (`req::getHeader`, `req::getParameter`) y ejecuta la acción devuelta `[CERT] UX/BChiServlet.java:163-169, 180-184`.
- **Rationale explícito:** `WebOp` es `final` y `HttpServletRequest/Response` no se pueden subclasear sin una station; aislando el ruteo en inputs plain-Java, cada decisión de ruteo es **testeable desde WSL en unit-tests puros, cero dependencia Niagara** `[CERT] UX/ChiServletDispatch.java:12-17`.

`RouteAction` es una **jerarquía sellada** implementada con clases anidadas `static final` de constructor privado; `route()` devuelve una instancia y `BChiServlet` hace `instanceof`-check para ejecutar `[CERT] UX/ChiServletDispatch.java:35-37, 40-44`. Dos formas de acción:
- **Singletons sin parámetros** (`Config.INSTANCE`, `Equipment.INSTANCE`, `AlarmSummary.INSTANCE`, `SetpointWrite.INSTANCE`, `CsrfProbe.INSTANCE`, `MethodNotAllowed.INSTANCE`, `NotFound.INSTANCE`, …) `[CERT] UX/ChiServletDispatch.java:40-44, 99-110`.
- **Portadoras de datos** con campos `final` inmutables que llevan los query/path params ya parseados: `Alarms(range, ackState, page)`, `HistoryData(id, range, fullResolution)`, `AlarmsBySource(ord, range, limit)`, `UpThresholdsGet(ord)`, `Audit(user, range, action)`, `Redirect(url)`, `BadRequest(reason)`, `StaticResource(resourcePath)` `[CERT] UX/ChiServletDispatch.java:54-66, 128-140, 297-308, 351-363`.

`doGet` mapea la acción vía `executeAction()` (un `if/else if instanceof` largo) `[CERT] UX/BChiServlet.java:249-371`; `doPost` tiene su propio bloque `instanceof` inline dentro de `doPost` `[CERT] UX/BChiServlet.java:186-243`. `[INFER]` la asimetría (POST no pasa por `executeAction`) es porque los handlers de escritura necesitan el `req` además del `resp`.

---

## 165.2 — Mapa completo de endpoints GET

Todos los GET bajo `/api/*` exigen el guard XHR (ver §165.4), salvo `/api/csrf-probe`. Los paths que no empiezan por `/api/` caen al fallback de recurso estático.

| Endpoint | Método | Auth-guard | Response shape | Cita |
|---|---|---|---|---|
| `/` , `/index.html`, `/css/*`, `/js/*`, `/img/*`, `/fonts/*`, `/ext/*` | GET | traversal→404; sin XHR | archivo estático de `rc/` (Content-Type por extensión) | `[CERT] ChiServletDispatch.java:626-628; BChiServlet.java:1204-1251` |
| `/api/config` | GET | XHR | `200` JSON hardcodeado HEAD+monitorOrds+TAIL | `[CERT] BChiServlet.java:387-423` |
| `/api/equipment` | GET | XHR | `200` JSON de `ChiEquipmentReader`; error→`500 {"error":…}` | `[CERT] BChiServlet.java:429-446` |
| `/api/alarms` | GET | XHR | `200` stream paginado (`range="24h"`, `ackState="all"`, `page=0` por defecto) | `[CERT] BChiServlet.java:455-472; ChiServletDispatch.java:486-503` |
| `/api/alarms/summary` | GET | XHR | `200` conteos por prioridad/tipo | `[CERT] BChiServlet.java:475-491` |
| `/api/historyList` | GET | XHR | `200` lista de histories | `[CERT] BChiServlet.java:494-510` |
| `/api/historyData?id=&range=[&fullResolution=]` | GET | XHR; `id` vacío→**404** | `200` serie temporal; `range="lastHour"` default | `[CERT] BChiServlet.java:517-534; ChiServletDispatch.java:512-525` |
| `/api/equipment-histories` | GET | XHR | `200` mapa equip→{prop→histId}; servicio null→`{}` | `[CERT] BChiServlet.java:542-575` |
| `/api/schedules` | GET | XHR | `200` array de schedules; error→`500 []` | `[CERT] BChiServlet.java:635-651` |
| `/api/csrf-probe` | GET | **EXENTO de XHR** | `200 {"hasSession":bool,"sessionId":str\|null,"supportsCSRF":bool}` | `[CERT] BChiServlet.java:586-621; ChiServletDispatch.java:462-465` |
| `/api/alarms/sources?range=` | GET | XHR | `200` array; `range="last8Hours"` default; error→`[]` | `[CERT] BChiServlet.java:1926-1945; ChiServletDispatch.java:536-541` |
| `/api/alarms/notes/{uuid}` | GET | XHR | `200` array de notas | `[CERT] BChiServlet.java:1992-2000; ChiServletDispatch.java:544-551` |
| `/api/alarms/hyperlink?ord=` | GET | XHR **+ remoteUser≠null → 403** | `200 {"url":…\|null}` | `[CERT] BChiServlet.java:2140-2164; ChiServletDispatch.java:554-557` |
| `/api/alarms/source?ord=[&range=&limit=]` | GET | XHR; `ord` null/vacío→**400** | `200` array (limit clamp 1..200, def 200); `range="last8h"` | `[CERT] BChiServlet.java:1953-1973; ChiServletDispatch.java:581-603` |
| `/api/user/theme` | GET | XHR; anónimo→default | `200 {"theme":"dark"\|"light"}`; error→`200 {"theme":"dark"}` | `[CERT] BChiServlet.java:957-983; ChiServletDispatch.java:560-563` |
| `/api/user/capability` | GET | XHR **+ remoteUser≠null → 401** | `200 {"username":…,"role":…,"canWrite":bool}` | `[CERT] BChiServlet.java:1065-1092; ChiServletDispatch.java:566-569` |
| `/api/audit?user=&range=&action=` | GET | XHR **+ remoteUser≠null → 401** | `200 {"actions":[…],"logins":[…]}` | `[CERT] BChiServlet.java:1122-1167; ChiServletDispatch.java:572-578` |
| `/api/up/{ord}/thresholds` | GET | XHR **+ remoteUser≠null → 401** | `200` JSON; ord irresoluble→**400** | `[CERT] BChiServlet.java:1487-1520; ChiServletDispatch.java:606-610` |
| `/api/carcamo/{ord}/thresholds` | GET | XHR **+ 401** | `200` JSON; irresoluble→**400** | `[CERT] BChiServlet.java:1602-1635; ChiServletDispatch.java:611-615` |
| `/api/datalogger/{ord}/thresholds` | GET | XHR **+ 401** | `200` JSON; irresoluble→**400** | `[CERT] BChiServlet.java:1717-1750; ChiServletDispatch.java:616-620` |
| `/api/*` desconocido | GET | XHR | **404** `{"error":"Not found"}` | `[CERT] ChiServletDispatch.java:622-623` |

**Detalle de `/api/config`:** el bloque `monitorOrds` (18 entradas: ups/carcamos/dataloggers × plantas 1..6) se construye programáticamente con doble loop `for n=1..6 × t=0..2`, ensamblando `HEAD + "monitorOrds":{…} + TAIL`; el prefijo ORD es `station:|slot:/Services/ChiDashboardService` `[CERT] UX/BChiServlet.java:135-136, 393-421`.

---

## 165.3 — Mapa completo de endpoints POST

Todo POST bajo `/api/*` exige `X-Requested-With: XMLHttpRequest`; su ausencia → **302 Redirect** a `/mx60/#home` (mismo guard que GET, ver §165.4) `[CERT] UX/ChiServletDispatch.java:395-401`. Los handlers de escritura BMS pasan además por el guard RBAC `ChiRbacHelper.checkCanWrite(req,resp)` que ya emite 401/403 y hace `return` `[CERT] UX/BChiServlet.java:680, 1320-1321` (ver [Block 164]).

| Endpoint | Método | Auth-guard | Response shape | Cita |
|---|---|---|---|---|
| `/api/setpoint` | POST | XHR + **RBAC** + **423 lock** | `200 {"ok":true}`; falta ord/prop→400; **lock→423** `{"error":"locked","retryAfterMs":200}` | `[CERT] BChiServlet.java:675-830; ChiServletDispatch.java:402-405` |
| `/api/alarms/latch` | POST | XHR + **RBAC** | `200 {"ok":true}`; ord/key faltante→400; irresoluble→400 | `[CERT] BChiServlet.java:1317-1394; ChiServletDispatch.java:408-411` |
| `/api/alarms/unlatch` | POST | XHR + **RBAC** | `200 {"ok":true}`; validación→400 | `[CERT] BChiServlet.java:1405-1481; ChiServletDispatch.java:412-415` |
| `/api/alarms/notes` | POST | XHR + **RBAC** | `200 {"success":true,"count":N}`; uuids/note faltante→400 | `[CERT] BChiServlet.java:2009-2059; ChiServletDispatch.java:417-420` |
| `/api/alarms/ackAll` | POST | XHR + **RBAC** | `200 {"uuids":[…],"sourceCount":N,"truncated":bool}` (collect-only) | `[CERT] BChiServlet.java:2075-2132; ChiServletDispatch.java:422-425` |
| `/api/user/theme` | POST | XHR; **anónimo→401**; **EXENTO de RBAC** | `200 {"theme":…,"ok":true}`; theme inválido→400 | `[CERT] BChiServlet.java:993-1042; ChiServletDispatch.java:427-430` |
| `/api/up/{ord}/threshold` | POST | XHR + **RBAC** + **401** | `200 {"ok":true}`; name/value inválido→400 | `[CERT] BChiServlet.java:1522-1600; ChiServletDispatch.java:433-437` |
| `/api/carcamo/{ord}/threshold` | POST | XHR + **RBAC** + **401** | `200 {"ok":true}`; validación→400 | `[CERT] BChiServlet.java:1637-1715; ChiServletDispatch.java:438-442` |
| `/api/datalogger/{ord}/threshold` | POST | XHR + **RBAC** + **401** | `200 {"ok":true}`; validación→400 | `[CERT] BChiServlet.java:1752-1830; ChiServletDispatch.java:443-447` |
| `/api/*` desconocido / cualquier otro POST | POST | XHR | **405** `{"error":"Method Not Allowed"}` + `Allow: GET` | `[CERT] BChiServlet.java:239-242, 1296-1304; ChiServletDispatch.java:449` |

**Nota sobre `theme`:** es preferencia cosmética por-usuario, deliberadamente **exenta del check OPERATOR_WRITE de RBAC** pero NO de autenticación: autentica primero (anónimo→401) antes de parsear el body `[CERT] UX/BChiServlet.java:996-1012`. **Nota sobre `ackAll`:** es *collect-only* — el servlet solo colecta UUIDs vía BQL y el ack real ocurre client-side vía BajaScript, porque `BAlarmService.ackAlarm(rec)` sobre records de cursor es un no-op silencioso `[CERT] UX/BChiServlet.java:2070-2073`.

---

## 165.4 — Los tres guards centrales de `route()`

El orden de evaluación en `route()` es load-bearing y difiere entre GET y POST.

1. **Traversal → 404.** Cualquier path que contenga `".."`, `"\"` o `NUL` (`\0`) devuelve `NotFound.INSTANCE`. Se aplica **antes** del guard XHR para que `/api/../../../etc/passwd` caiga aquí primero, y aplica a **todos** los paths, no solo estáticos `[CERT] UX/ChiServletDispatch.java:452-457`. En POST el mismo check está al inicio del bloque POST `[CERT] UX/ChiServletDispatch.java:389-393`. Hay además una defensa **belt-and-suspenders** duplicada dentro de `serveStaticResource` antes de tocar el classloader `[CERT] UX/BChiServlet.java:1207-1213`.

2. **`X-Requested-With` (CSRF-lite) → 302.** Para todo path que empiece por `/api/`, si el header `X-Requested-With` es null o distinto de `"XMLHttpRequest"`, devuelve `Redirect("/mx60/#home")` — trata la navegación directa de browser como no-API `[CERT] UX/ChiServletDispatch.java:467-475` (GET) y `[CERT] UX/ChiServletDispatch.java:394-401` (POST, redirige antes de identificar el endpoint concreto).

3. **csrf-probe EXENTO.** `/api/csrf-probe` se evalúa **antes** del guard XHR (líneas 462-465 preceden a 467) precisamente para ser invocable antes de que el frontend sepa si XHR es viable; es read-only (usa `getSession(false)` — **no crea sesión**), por lo que su riesgo CSRF es cero `[CERT] UX/ChiServletDispatch.java:459-465; BChiServlet.java:593-597`.

**Fallback estático:** si el path no es `/api/*` y pasa el traversal, se sirve como recurso: `/` → `"index.html"`, cualquier otro → `path.substring(1)` `[CERT] UX/ChiServletDispatch.java:626-628`. `extractOrdSegment(path, prefix, suffix)` parsea `/api/{type}/{ord}/{suffix}` devolviendo el `{ord}` o null `[CERT] UX/ChiServletDispatch.java:643-651`.

---

## 165.5 — Contrato de error JSON (hecho a mano) + `try/catch → 500`

No hay librería JSON. Todo el JSON se ensambla con `PrintWriter.print` y `StringBuilder`; los strings se escapan con `ChiJsonUtil.escapeJson(...)` `[CERT] UX/BChiServlet.java:443, 604`. El patrón de error es **uniforme** en casi todos los handlers:

```
try { resp.setStatus(200); <delegar al helper> }
catch (Exception e) { LOG.warning(...); resp.setStatus(500);
                      out.print("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}"); }
finally/after: out.flush();
```
`[CERT] UX/BChiServlet.java:433-446, 460-472, 1085-1090`.

Envelopes fijos de error:
- **404:** `send404` → `{"error":"Not found"}`, Content-Type JSON `[CERT] UX/BChiServlet.java:1282-1289`.
- **405:** `send405` → `{"error":"Method Not Allowed"}` + header `Allow: GET` `[CERT] UX/BChiServlet.java:1296-1304`.
- **400:** `send400(reason,…)` → `{"error":"<reason escapado>"}` `[CERT] UX/BChiServlet.java:1979-1985`; y variantes inline `{"error":"missing ord"}`, `{"error":"invalid theme"}`, etc.
- **Degradación grácil:** algunos handlers no devuelven 500 sino un cuerpo neutro — `handleSchedules` error→`[]` `[CERT] UX/BChiServlet.java:646-649`; `handleAlarmSources` error→`[]` `[CERT] UX/BChiServlet.java:1938-1940`; `handleAlarmsBySource` error→`[{"error":…}]` `[CERT] UX/BChiServlet.java:1966-1967`.

Parsing de body: hand-rolled también. `ChiJsonUtil.extractJsonValue(json, key)` para campos escalares; `parseStringArray`/`extractJsonStringArray` con regex/scan manual para arrays; `unescapeJsonString` maneja `\" \\ \/ \n \r \t` `[CERT] UX/BChiServlet.java:885-943, 1877-1918`. `coerceValue` adapta el valor entrante al tipo del slot actual (BStatusNumeric/BDouble/BStatusBoolean/BBoolean/BStatusString), con fallback a BString `[CERT] UX/BChiServlet.java:842-874`.

---

## 165.6 — Security headers y Content-Type

- **`setApiHeaders`** (todos los `/api/*`): `Content-Type: application/json; charset=UTF-8` + `Cache-Control: no-cache, no-store, must-revalidate` `[CERT] UX/BChiServlet.java:1272-1276`.
- **`doGet`/`doPost`** fijan `resp.setCharacterEncoding("UTF-8")` de entrada `[CERT] UX/BChiServlet.java:158, 177`.
- **Recursos estáticos HTML** reciben además headers de seguridad: `Cache-Control: no-cache…` + **`X-Frame-Options: SAMEORIGIN`** + **`X-Content-Type-Options: nosniff`** `[CERT] UX/BChiServlet.java:1230-1236`.
- **Assets estáticos no-HTML:** `Cache-Control: public, max-age=3600` (una hora) `[CERT] UX/BChiServlet.java:1238-1241`.
- **MIME:** `getContentType` resuelve por extensión (html/css/js/json/png/jpg/svg/woff2/woff/ttf/ico), default `application/octet-stream`; portado de BSnlsServlet **menos glb/gltf** (chihuahua usa geometría procedural Three.js, sin modelos 3D) `[CERT] UX/ChiServletDispatch.java:660-682`. Los estáticos se leen del JAR bajo `rc/` vía classloader con copia por buffer de 4 KiB `[CERT] UX/BChiServlet.java:72, 1215-1262`.

`[INFER]` Los headers de seguridad `X-Frame-Options`/`X-Content-Type-Options` solo se aplican al HTML servido estáticamente, **no** a las respuestas `/api/*` JSON — éstas solo llevan Content-Type + Cache-Control.

---

## 165.7 — `ReentrantLock` por-ord → HTTP 423 Locked (solo setpoint)

`handleSetpointWrite` es el único handler con locking explícito. Tras resolver el `BChiDashboardService` vía `DRIVER_TREE_ORD`, intenta **adquirir un lock por-ord** con timeout de 100 ms:

```
acquiredLock = svc.acquireLock(parentOrd, 100);
if (acquiredLock == null) { resp.setStatus(423);
   out.print("{\"error\":\"locked\",\"retryAfterMs\":200}"); return; }
```
`[CERT] UX/BChiServlet.java:769-784`.

- El `423` sale cuando el `controlTick` del backend ya tiene el lock del mismo `parentOrd` — es decir, coordina la escritura HTTP contra el lazo de control interno `[CERT] UX/BChiServlet.java:755-756, 776-777`.
- La escritura real (`parent.set(prop, coerceValue(...), null)`) va en un `try/finally` que **siempre** libera el lock (`acquiredLock.unlock()`) `[CERT] UX/BChiServlet.java:786-821`.
- Tras el `200 {"ok":true}` se dispara un **audit fire-and-forget** (`appendAudit`) cuyo fallo NUNCA falla la respuesta de escritura `[CERT] UX/BChiServlet.java:801-816` (ver [Block 164]).
- Estrategia de resolución del setpoint: **sin `BOrdTarget`** (API N4.14 no verificada). Parte el último segmento del ORD para obtener el ORD padre, resuelve el padre con `BOrd.make(parentOrd).get(this,null)`, obtiene la property por nombre y la setea `[CERT] UX/BChiServlet.java:660-671, 706-742`.

`[INFER]` Ningún otro handler de escritura (latch/unlatch/thresholds/notes/ackAll) toma este lock; el `423` es exclusivo del setpoint numérico porque es el único que colisiona con el lazo de control periódico.

---

## 165.x — Connections

- **[Block 163]** — esqueleto/skeleton del módulo chihuahua MX60: este bloque profundiza en la capa servlet (`BChiServlet` + `ChiServletDispatch`) que el skeleton enumera. `handleConfig` expone el `MONITOR_ORD_PREFIX` (`Services/ChiDashboardService`) que ancla la topología Planta1..6 descrita en el skeleton.
- **[Block 164]** — RBAC gate sobre los handlers POST: el `ChiRbacHelper.checkCanWrite(req,resp)` que abre todos los handlers de escritura BMS (setpoint/latch/unlatch/thresholds/notes/ackAll) y el audit fire-and-forget (`appendAudit` / `ChiAuditHelper.buildEntry`) están documentados aquí solo como guard; el detalle de roles viewer/operador, el `423` vs `403`, y el trail de auditoría viven en [Block 164].
- **[Block 149]** — el `BaseServlet` router de Reflow: **contraparte de comparación eventual**. Chihuahua resuelve el problema "servlet Niagara no testeable" con una **función pura `route()` + jerarquía `RouteAction` sellada** ejecutada off-station en WSL; Reflow lo resuelve con un `BaseServlet` router. El contraste central: chihuahua **separa decisión (pura) de ejecución (impura)** en dos clases, mientras que un router clásico suele acoplar ambas. `[INFER]` la comparación fina Reflow↔chihuahua queda pendiente hasta cruzar este bloque con [Block 149].
