# Bloque 58 — HTTP infrastructure deep dive: BaseServlet routing + 34 Response classes + CsrfGuard server-side + sync/backup AP-27 sites + 8 nuevos antipatterns AP-34..41

**Fecha**: 2026-05-07
**Método**: Audit del directorio `/http/` completo (BaseServlet 367L + 34 response classes + util) + sync helpers (BReflowSyncService 599L, ConfigIO 252L, BackupManager) con foco en AP-27 sistémico final + descubrimiento de 8 antipatterns nuevos en BaseServlet pattern.
**Fuentes primarias**:
- `BaseServlet.java` (367 líneas) — el HTTP router que sirve los 20 REST endpoints
- `CsrfGuard.java` (143 líneas) — server-side CSRF validation
- `BReflowSyncService.java` (599 líneas) + `ConfigIO.java` (252 líneas) + `BackupManager.java` — sync engine
- 34 Response classes en `/http/responses/` — pattern por endpoint
- Bloques 50, 51, 52 (CSRF cliente), 54 (REST endpoints catalogados), 55-57 (AP-27 sistémico)

**Versión analizada**: Reflow-Clean-177 (post-fix `harden-backup-csrf`) vs bundle producción Reflow 1.7.5 (Jul 2024) — **DESINCRONIZADOS**.

---

## 58.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Cierre de la auditoría arquitectónica de Reflow desde el lado HTTP infrastructure. Cubre:
1. **BaseServlet routing** — el código que sirve los 20 REST endpoints
2. **CsrfGuard server-side** — la pieza que valida los tokens CSRF (cliente cubierto en Bloques 50/52)
3. **Response class architecture** — 34 clases con pattern uniforme
4. **sync/backup helpers** — donde vive el sync engine + AP-27 final tally
5. **Bundle producción vs Reflow-Clean-177 desincronización** — finding crítico operacional

### Qué corrige / valida

| Bloque | Sección | Hallazgo previo | Validación / corrección |
|--------|---------|-----------------|-------------------------|
| 50.0 + 54.6.2 | AP-10 backups GET destructivos | "5+1 endpoints destructivos via GET" | ⚠️ **MATIZADO**: Reflow-Clean-177 server YA tiene fix (POST + CsrfGuard, 405 en GET legacy), pero bundle producción 1.7.5 sigue mandando GET. **DESINCRONIZACIÓN cliente↔server**. |
| 52 | CsrfGuard pattern cliente Plan E | "token via window.top.document" | ✅ **CONFIRMA server-side**: CsrfGuard espera el token en header `x-niagara-csrfToken` con constant-time compare contra session attribute `"csrfToken"`. Plan E del cliente coincide perfectamente. |
| 53.5.16.5 (template MX60 Java Reglas 1-12) | Template establecido para Commands | Confirmado uniformemente | ✅ **AP-27 final tally**: 40+ sites en helpers Reflow. Deuda técnica masiva confirmada definitivamente. |

### Pregunta unificadora

> ¿Cuál es el "shape" final del backend Reflow y qué patterns hereda MX60 vs reescribe?

**Respuesta corta**: BaseServlet manual routing es **anti-pattern** (heredado de Niagara HttpServlet limitations) — MX60 debe usar framework moderno con annotation routing. Response class architecture (34 classes) **se hereda 100%** (excelente separation). CsrfGuard pattern **se hereda 100%** (constant-time compare + structured response). Sync engine con threads custom hereda con regla 11 estricta (BJobService, no raw threads).

---

## 58.1 BaseServlet routing — anti-patterns + KEEP pattern

### 58.1.1 Estructura general

```
BaseServlet extends HttpServlet
├── doGet(req, resp)   — 165 líneas, 13 endpoints
├── doPost(req, resp)  — 137 líneas, 9 endpoints (5 backups + 4 misc)
└── setContentSecurityPolicy(resp) — CSP header injection
```

**Routing**: switch/case manual con `req.getPathInfo().equals(...)` o `.startsWith(...)`.

### 58.1.2 GET endpoints servidos (13)

| Path | Response class | CSRF guard | Comments |
|------|----------------|------------|----------|
| `/` | redirect to `/index.html` | — | SPA shell |
| `/index.html` | FileResponse.serve | — | SPA index |
| `/config` | ConfigResponse | — | Read JSON config |
| `/demos` | DemoResponse | — | Demo listing |
| `/demo/.*` | redirect to `/index.html` | — | SPA route |
| `/weather-map` | WeatherMapResponse | — | Mapbox proxy |
| `/station/equipment-notes` | EquipmentNoteResponse | — | Read notes |
| `/station/backups` | BackupListResponse | — | List (read-only) |
| `/station/backups/<other>` | **405 Method Not Allowed** | — | Hardenizado: GET legacy bloqueado |
| `/station/images` | ImageListResponse | — | Image catalog |
| `/station/files` | FileTreeResponse | — | Filesystem (AP-33 risk) |
| `/station/image-library` | ImageLibraryResponse | — | Reflow image lib |
| `/station/schedules` | SchedulesDataResponse | — | Schedule listing |
| `/station/histories` o `/station/histories/<name>` | HistoryListResponse / HistoryDataResponse | — | History data |
| `/station/history-data` | HistoryChartDataResponse | — | Chart data |
| `/station/history-groups` | HistoryGroupsResponse | — | Groups tree |
| `/station/alarms/csv` | AlarmCSVResponse | — | CSV export |
| `<other>` | FileResponse fallback | — | SPA fallback |

### 58.1.3 POST endpoints servidos (9, todos con CsrfGuard)

| Path | Response class | Action |
|------|----------------|--------|
| `/station/backups/create` | BackupCreateResponse | Crear backup |
| `/station/backups/apply` | BackupApplyResponse | Restaurar backup |
| `/station/backups/destroy` | BackupDestroyResponse | Borrar backup |
| `/station/backups/rename` | BackupRenameResponse | Renombrar |
| `/station/backups/reset` | BackupResetResponse | Borrar todos |
| `/config_update` | ConfigUpdateResponse | Update config JSON |
| `/config_delta` | ConfigDeltaResponse | Apply JSON Patch |
| `/station/equipment-notes-update` | EquipmentNoteUpdateResponse | Update notes |
| `/station/alarms/query` | AlarmQueryResponse | Query alarms |

**Nota**: alarms/query es **read-only pero POST + CSRF** — defense-in-depth aplicado consistentemente con backups (comment línea 318-320).

### 58.1.4 Antipatterns descubiertos en BaseServlet

**AP-34 (NUEVO MEDIUM)** — Manual routing O(n) match-by-match:

```java
if      (path.equals("/config"))         { ... }
else if (path.equals("/demos"))           { ... }
else if (path.equals("/weather-map"))     { ... }
// ... 18 endpoints más en cascade
```

Cada request se compara secuencialmente contra TODAS las paths. Para 23 endpoints es manejable, pero NO escala. MX60 → JAX-RS, Spring annotation routing, o table-driven dispatch (Map<path, handler>).

**AP-35 (NUEVO LOW)** — Code duplication masiva en error handling:

Cada endpoint repite el mismo bloque ~10 líneas:
```java
try {
    XYResponse.serve(req, resp);
} catch (Exception varN) {
    resp.setStatus(500);
    resp.setContentType("text/html");
    PrintWriter out = resp.getWriter();
    out.write("<h1>ERROR 500: Internal Error</h1>" + path);  // ⚠️ XSS
    LOGGER.log(Level.SEVERE, "BaseServlet error", varN);
}
```

22+ veces. MX60 → middleware/filter de error handling centralizado.

**AP-36 (NUEVO MEDIUM)** — Reflected XSS en error responses:

```java
out.write("<h1>ERROR 500: Internal Error</h1>" + path);
```

`path` viene de `req.getPathInfo()` — atacante manda request a `/<script>alert(1)</script>` → response contiene el script raw → XSS reflejado. **Mitigado parcialmente por CSP `default-src 'self'` y Content-Type fijo**, pero igual fragile. Dependencia indebida en CSP.

MX60 → HTML escape obligatorio en cualquier user data inyectado en response.

**AP-37 (NUEVO LOW)** — Log messages copy-paste incorrectos:

```java
} else if (path.equals("/station/backups/destroy")) {
    // ...
    LOGGER.log(Level.SEVERE, "BaseServlet doPut error", e);   // ⚠️ es doPost, no doPut
} else if (path.equals("/station/backups/rename")) {
    LOGGER.log(Level.SEVERE, "BaseServlet doDelete error", e); // ⚠️ es doPost
} else if (path.equals("/station/backups/reset")) {
    LOGGER.log(Level.SEVERE, "BaseServlet doOptions error", e); // ⚠️ es doPost
```

Logs confusos en diagnóstico — refleja refactor incompleto donde method-specific dispatchers se colapsaron a doPost pero los strings no se actualizaron.

**AP-38 (NUEVO LOW)** — HTTP status code inconsistente:

```java
} catch (Exception var7) {
    resp.setStatus(200);                     // ⚠️ 200 EN ERROR CASE
    resp.setContentType("application/json");
    PrintWriter out = resp.getWriter();
    out.write("{ \"status\": 500 }");        // ⚠️ payload dice 500 pero status code 200
    // ...
}
```

`/config` endpoint en error case retorna HTTP 200 con payload `status: 500`. Cliente no detecta error en HTTP layer, debe parsear JSON. Anti-RFC. Otros endpoints retornan 500. Inconsistente.

**AP-39 (NUEVO HIGH OPERACIONAL)** — Cliente↔server desincronizado:

- Reflow-Clean-177 server: backups via POST + CsrfGuard, GET legacy retorna 405
- Bundle producción 1.7.5 (cliente actual deployed): sigue mandando GET a `/station/backups/create?file=...`

Si cliente bundle producción se conecta a server hardenizado → todas las operaciones backup fallan con 405 (visible en networking tab).

**Si el server productivo NO se ha redeployado con clean-177** (probable, dado que bundle es Jul 2024): backups siguen funcionando vía GET y AP-10 sigue activo.

**MX60 implication**: rebuild + redeploy obligatorio sincronizado cliente+server. CI pipeline debe gating versions.

**AP-40 (NUEVO MEDIUM)** — CSP `connect-src '*'` too permissive:

```java
"... ; connect-src * ws: wss:"
```

`connect-src '*'` permite XHR/fetch/WebSocket a CUALQUIER host. Mitiga la utilidad de la CSP. MX60 → whitelist explícita (`connect-src 'self' wss://reflow.example.com`).

**AP-41 (NUEVO metodológico)** — Variable naming sospechoso (`var7`, `var8`, ..., `var20`):

```java
} catch (Exception var7) { ... }
} catch (Exception var8) { ... }
```

Esto es decompiled output preservado en source. Indica que la fuente original está perdida — Reflow-Clean-177 source es decompilation de bytecode, no source real. Trabajamos con un step removed.

**Implicación operacional**: cualquier comment, formatting, o non-essential code structure en estos archivos NO refleja la fuente original. Audits de code style/quality sobre Reflow-Clean-177 deben ser **agnósticos a presentation**.

---

## 58.2 KEEP pattern — Response class architecture

### 58.2.1 Lista canónica de 34 Response classes

```
http/responses/
├── AlarmCSVResponse.java          (38 LOC)
├── AlarmQueryResponse.java        (44 LOC)
├── BackupApplyResponse.java       (57 LOC)
├── BackupCreateResponse.java      (39 LOC)
├── BackupDestroyResponse.java     (38 LOC)
├── BackupListResponse.java        (43 LOC)
├── BackupRenameResponse.java      (47 LOC)
├── BackupResetResponse.java       (58 LOC)
├── ConfigDeltaResponse.java       (55 LOC)
├── ConfigResponse.java            (118 LOC)
├── ConfigUpdateResponse.java      (128 LOC)
├── DemoResponse.java              (48 LOC)
├── EquipmentNoteResponse.java     (58 LOC)
├── EquipmentNoteUpdateResponse.java (87 LOC)
├── FileResponse.java              (84 LOC)
├── FileTreeResponse.java          (66 LOC)
├── HistoryChartDataResponse.java  (74 LOC)
├── HistoryDataResponse.java       (265 LOC)  ← más grande
├── HistoryGroupsResponse.java     (83 LOC)
├── HistoryListResponse.java       (84 LOC)
├── ImageLibraryResponse.java      (68 LOC)
├── ImageListResponse.java         (64 LOC)
├── SchedulesDataResponse.java     (33 LOC)   ← más chica
├── WeatherMapResponse.java        (125 LOC)
└── ... 10 más no enumeradas en grep total 34 archivos
```

### 58.2.2 Pattern uniforme

Cada Response class tiene método estático `serve(...)`:

```java
public class XYResponse {
    public static void serve(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // 1. Parse params del request
        // 2. Llamar a helper de business logic (HistoryData, AlarmData, etc)
        // 3. Serializar response (Jackson JSON)
        // 4. Set headers + content-type
        // 5. Write a resp.getWriter()
    }
}
```

**KEEP — patrón excelente para MX60**:
- Single responsibility por endpoint
- Testeable individualmente (mock req/resp)
- Tamaño manejable (38-128 líneas mayoría)
- Naming claro (paths → class names obvious)
- HistoryDataResponse 265 LOC es outlier — probablemente candidate to split

### 58.2.3 MX60 implication arquitectural

```
mx60-rt/.../http/
├── routes/                     ← annotation-based routing
│   ├── BackupRoutes.java       (@GET /backups, @POST /backups/create, ...)
│   ├── HistoryRoutes.java
│   └── ...
├── responses/                  ← KEEP de Reflow
│   ├── BackupCreateResponse.java
│   └── ...
├── handlers/
│   ├── ErrorHandler.java       ← centraliza error handling (anti-AP-35)
│   ├── CsrfGuardFilter.java    ← KEEP de Reflow
│   └── ...
└── util/
    ├── JsonBodies.java         ← KEEP de Reflow
    └── ...
```

---

## 58.3 CsrfGuard.java — KEEP pattern excelente

### 58.3.1 Análisis

143 líneas, código limpio (probable post-decompilation reformatting o source preservada). El comment header es **valioso** — documenta:
- Token source-of-truth: session attribute `"csrfToken"`
- Header name: `x-niagara-csrfToken`
- ASSUMPTION T1: la session attribute key
- Plan B fallback: endpoint `/csrf-token` con ConcurrentHashMap por JSESSIONID

### 58.3.2 Algoritmo `validate(req)`

```java
public static boolean validate(HttpServletRequest req) {
    // 1. Read header
    String header = req.getHeader("x-niagara-csrfToken");
    if (header == null || header.isEmpty()) {
        logRejection(req, "missing header");
        return false;
    }

    // 2. Read session WITHOUT creating new (false flag)
    HttpSession session = req.getSession(false);
    if (session == null) {
        logRejection(req, "no active session");
        return false;
    }

    // 3. Read session attribute (ASSUMPTION T1)
    Object expected = session.getAttribute("csrfToken");
    if (expected == null) {
        logRejection(req, "session attribute 'csrfToken' not found");
        return false;
    }

    // 4. CONSTANT-TIME COMPARE (timing-attack defense)
    byte[] headerBytes = header.getBytes(UTF_8);
    byte[] expectedBytes = expected.toString().getBytes(UTF_8);
    if (!MessageDigest.isEqual(headerBytes, expectedBytes)) {
        logRejection(req, "token mismatch");
        return false;
    }

    return true;
}
```

### 58.3.3 KEEP patterns identificados

✅ **Constant-time compare via `MessageDigest.isEqual`** — defensa contra timing attacks. Patrón obligatorio para cualquier secret comparison. MX60 KEEP literal.

✅ **`req.getSession(false)`** — NO crea nueva sesión. Si no hay sesión = no token = reject. Patrón correcto.

✅ **Estructured 403 response**: `respondForbidden(resp)` retorna `{"error":"csrf_token_invalid"}` con `application/json` Content-Type. Cliente parseea estructura, no string.

✅ **Logging detallado con IP + path + method**: `[CsrfGuard] CSRF rejection — <reason> | ip=X | path=Y | method=Z`. Útil para detectar attack patterns.

✅ **Documented assumptions**: Plan A y Plan B documentados en JavaDoc class-level. Explicita la dependencia en N4 internal API.

### 58.3.4 MX60 implication

CsrfGuard se hereda **TAL CUAL** con cambios mínimos:
- Header name: `x-mx60-csrfToken` (o mantener `x-niagara-csrfToken` por compat)
- Session attribute key: confirmar en N4.14 viva (Plan A verification del comment)
- Plan B `/csrf-token` endpoint: implementar como fallback desde día 1, no como future work

---

## 58.4 sync helpers — AP-27 sites finales

### 58.4.1 BReflowSyncService.java (599 líneas)

**Sites confirmados**:
- Línea 173: `Thread thread = new Thread(task, "BReflowSyncService.loadConfigurationFile.Task")`
- Línea 179: `Thread thread = new Thread(task, "BReflowSyncService.saveConfigurationFile.Task")`
- Línea 353: `AccessController.doPrivileged(...)` envolviendo:
- Línea 355: `Thread thread = new Thread(task, "BReflowSyncService.ConfigSyncCommand.Task")`
- Línea 491: `BOrd ord = BOrd.make(this.fileLocation)` (sin .get(cx) visible inmediatamente)
- Línea 578: `BOrd ord = BOrd.make(this.fileLocation)` (mismo)

**Total**: 3 threads custom + 1 doPrivileged + 2 BOrd.make. AP-27 confirmado.

**Implicación específica**: BReflowSyncService es donde vive el sync engine RFC 6902 JSON Patch (Bloque 51 AP-17). Las operaciones de sync potencialmente bypass RBAC del user invocante.

### 58.4.2 ConfigIO.java (252 líneas)

```java
Línea 28: Thread thread = new Thread(task, "ConfigIO.writeFavorites.Task")
Línea 51: Thread thread = new Thread(task, "ConfigIO.write.Task")
Línea 57: Thread thread = new Thread(task, "ConfigIO.write.Task")  ← duplicado naming
Línea 91: Thread thread = new Thread(task, "ConfigIO.writeCache.Task")
```

**4 threads custom**. Cada operación de write a config file usa thread separado. AP-27 amplificado.

### 58.4.3 BackupManager.java (parcial)

```java
Línea 50: Thread thread = new Thread(task, "BackupManager.create.Task")
Línea 56: Thread thread = new Thread(task, "BackupManager.apply.Task")
```

**2 threads custom**. AP-27.

### 58.4.4 Tally final AP-27 cross-bloques 53-58

| Bloque | Source | Sites |
|--------|--------|------:|
| 53.5.16 | BReflowBQLCommands | 1 (línea 92) |
| 54.4.2 | AlarmData.java | 9 BOrd + 1 thread + 1 doPriv |
| 54.3.3 | BReflowAlarmCommands.java | 8 (cx no propagado a helpers) |
| 55.3.1 | HistoryData.java | 6 BOrd + 1 thread + 1 doPriv |
| 55.3.1 | HistoryGroups.java | 4 BOrd |
| 55.3.1 | HistoryList.java | 2 BOrd |
| 55.3.1 | HistoryIO.java | 1 thread |
| 57.2.2 | BReflowNavCommands | 2 BOrd |
| 57.3 | BReflowFileCommands | 1 BOrd + AP-33 |
| 57.3.4 | BReflowCSVCommands | 1 BOrd + AP-33 |
| 58.4.1 | BReflowSyncService | 3 thread + 1 doPriv + 2 BOrd |
| 58.4.2 | ConfigIO | 4 thread |
| 58.4.3 | BackupManager | 2 thread |
| **TOTAL** | | **~50 sites** |

**~50 sites de RBAC bypass potencial en helpers Reflow**. Es deuda técnica MASIVA SISTÉMICA. Cualquier refactor que rompa la presunción "ThreadLocal cubre" causa info disclosure cascade en TODO el módulo.

**MX60 implication final**:

> La regla 11 (Context propagation end-to-end) es la **DECISIÓN ARQUITECTÓNICA #1** del template MX60 Java. Cualquier helper que la viole genera deuda inmediata. Pre-commit hook obligatorio que rechace `BOrd.make(...).get(null)`, `.get()` sin args, `new Thread(task)` raw, y `AccessController.doPrivileged` (excepto en clase whitelisted con justificación documented).

---

## 58.5 Antipatterns nuevos — AP-34..41

| AP # | Patrón | Severidad | Sección |
|------|--------|-----------|---------|
| **AP-34** | Manual routing O(n) match-by-match en BaseServlet | MEDIUM | 58.1.4 |
| **AP-35** | Code duplication masiva en error handling | LOW | 58.1.4 |
| **AP-36** | Reflected XSS en error responses (path en HTML sin escape) | MEDIUM | 58.1.4 |
| **AP-37** | Log messages copy-paste incorrectos (doPut/doDelete/doOptions cuando es doPost) | LOW | 58.1.4 |
| **AP-38** | HTTP status 200 en error case `/config` (anti-RFC) | LOW | 58.1.4 |
| **AP-39** | Cliente↔server desincronizado: bundle 1.7.5 sigue GET, server hardenizado solo POST | HIGH OPERACIONAL | 58.1.4 |
| **AP-40** | CSP `connect-src '*'` too permissive | MEDIUM | 58.1.4 |
| **AP-41** | Variable naming `var7..var20` indica decompiled source preservado | metodológico | 58.1.4 |

**Total antipatterns Bloque 58: 8 nuevos**.
**Total antipatterns acumulado Bloques 50-58**: AP-1..AP-41 = **41 antipatterns** identificados en Reflow.

---

## 58.6 Síntesis MX60 implications — finales

| # | Patrón | Tag | Razón |
|---|--------|-----|-------|
| 96 | Manual routing switch/case en HttpServlet | **SKIP** (AP-34) | MX60 → annotation-based (JAX-RS / Spring) o table-driven |
| 97 | Response class architecture (1 class por endpoint) | **KEEP literal** | 34 classes pattern excelente. Single responsibility, testable, claro. |
| 98 | CsrfGuard pattern (constant-time compare + structured 403) | **KEEP literal** | Pattern excelente. Migrar tal cual a MX60. |
| 99 | Session attribute `"csrfToken"` para token storage | **KEEP** (Plan B fallback documented) | N4 convention. MX60 → mismo pattern + endpoint /csrf-token fallback desde día 1. |
| 100 | Logging detallado en CSRF rejection (IP + path + method + reason) | **KEEP** | Útil para detectar attack patterns. MX60 → mismo + structured log (JSON). |
| 101 | Defense-in-depth POST + CSRF para alarms/query (read-only pero protegido) | **KEEP** | Política consistente. MX60 → POST + CSRF para CUALQUIER mutación ambigua. |
| 102 | CSP header injection en cada response | **KEEP** (mejorar) | Pattern correcto. MX60 → `connect-src` whitelist explícito (anti-AP-40). |
| 103 | Reflected XSS via `path` in error response | **NUNCA** (AP-36) | MX60 → HTML escape obligatorio o template engine |
| 104 | HTTP status code consistente (no 200 en error) | **REGLA OBLIGATORIA** | MX60 → status code matchea payload "status" siempre |
| 105 | Code duplication en error try/catch | **IMPROVE** (AP-35) | Middleware/filter centralizado |
| 106 | Sync engine con threads custom raw | **NUNCA** (AP-27 amplified) | MX60 → BJobService obligatorio |
| 107 | Cliente↔server version sync via CI gating | **NEW** (anti-AP-39) | MX60 → CI pipeline rechaza deploy si cliente bundle es older que server |

### Resumen agregado FINAL Capa 17

- **49 KEEP** (+10 Bloque 58): Response class architecture (34 classes), CsrfGuard literal pattern, session attribute, detailed logging, defense-in-depth POST+CSRF, CSP header injection, MAX_LIMIT, BqlBuilder, audit logging writes, Sys.getService
- **35 IMPROVE** (+4): error handling middleware, CSP whitelist, error status code, sync threads → BJobService
- **11 NEW** (+2): cliente↔server CI sync, error response sin XSS
- **6 SKIP** (+1 Bloque 58): manual routing
- **41 antipatterns** (AP-1..41) — backlog completo de "no replicar"

**Tabla MX60 acumulada FINAL: 101 entries** — backlog de diseño Cubierto: client + server + Java decorations + RBAC patterns + REST endpoints + audit logging + versioning + HTTP infrastructure + CSRF + CSP + sync engine.

---

## 58.7 Cierre del bloque + estado de la auditoría completa

### 58.7.1 Bloques publicados — sesión 2026-05-07

| Bloque | Tema | Líneas |
|--------|------|-------:|
| **53** | `app-readable.js` audit + MX60 implications + 7 sub-libs $niagara | ~1500 |
| **54** | Alarm domain client↔server + AP-27 + REST corrigendum | ~600 |
| **55** | History domain + AP-27 sistémico + fetchMethod switch | ~430 |
| **56** | Points domain (sin Java backend = decisión positiva) | ~250 |
| **57** | Schedule + Nav + File + CSV + AP-33 file disclosure | ~370 |
| **58** | HTTP infrastructure deep dive + 8 nuevos antipatterns | ~500 |
| **TOTAL** | | **~3,650 líneas** |

### 58.7.2 Antipatterns acumulados (bloques 50-58)

41 antipatterns identificados:
- AP-1..12 (Bloque 50)
- AP-13..20 (Bloque 51)
- AP-21..23 (Bloque 53 cliente)
- AP-24..26 (Bloque 53 BQL Java)
- AP-27..32 (Bloque 54 Alarm Java)
- AP-33 (Bloque 57 file disclosure)
- AP-34..41 (Bloque 58 HTTP infrastructure)

### 58.7.3 MX60 design backlog — 101 entries

- **49 KEEP** — heredar literal o con migración Vue 3 / N4.14
- **35 IMPROVE** — heredar el qué, mejorar el cómo
- **11 NEW** — patterns nuevos no presentes en Reflow
- **6 SKIP** — no replicar
- **41 antipatterns** — explicit "DO NOT inherit" list

### 58.7.4 Restantes / próximos hilos

**LOW priority cleanup** (Bloque 59 si necesario):
- Audit `BReflowChannelService.java` (281L) + `BReflowWebSocketAcceptor.java` (505L) — el WebSocket layer real-time (Bloque 51 cliente cubierto)
- `/sync/commands/` (ReflowOrdTreeFavoritesRead/Write)
- Restantes Response classes detalle (algunas con lógica compleja: HistoryDataResponse 265L, ConfigUpdateResponse 128L, ConfigResponse 118L, WeatherMapResponse 125L)

**HIGH priority cuando llegue MX60 implementation**:
- Validar Plan A/B de CsrfGuard contra N4.14 viva (test real con station)
- Implementar template Java Commands con 12 reglas obligatorias
- Pre-commit hooks anti-AP-27 + anti-AP-33

**Investigación pendiente futura**:
- BaseServlet routing pattern alternative (Niagara 4.14 puede tener annotation routing built-in?)
- BJobService API completa (alternative a threads raw para sync engine)
- Integration testing pattern para Commands con cx propagation verificable

---

## 58.8 Conclusión meta del audit completo

### 58.8.1 Lecciones metodológicas capitalizadas

1. **Counts grep con paréntesis** para callsites de funciones (Bloque 53.10)
2. **Triangulación orchestrator + sub-agent** con counts independientes (Bloque 53.10)
3. **Counts en bundles minified mienten** — verificar con patterns de uso (Bloque 54 lección)
4. **Severidades iniciales son hipótesis** — verificar con audit completo antes de declarar HIGH (AP-21 LOW-MEDIUM final)
5. **Predicciones explícitas** post-audit (Bloque 54.10 → 55.3.3 confirmed) refinan el modelo

### 58.8.2 Para el yo del 2027 que vuelve a leer estos bloques

> Si volvés acá tras meses de programar MX60 y querés acordarte de qué hacer:
> - **Regla 11 + 12** (Java Commands cx propagation + filesystem RBAC) son las DECISIONES ARQUITECTÓNICAS #1 y #2 del backend.
> - **34 Response classes** del `/http/responses/` se heredan literal (ajustando frameworks).
> - **CsrfGuard** se hereda literal (ajustando session attribute key si difiere en N4.14).
> - **NO** intentes wrappear Niagara en cada dominio — abstracciones bajas por default (Bloque 56).
> - **41 antipatterns** son la checklist de "qué NO heredar". Si un component MX60 hace algo de la lista, es bug pendiente.

### 58.8.3 Cómo MX60 evita las 3 deudas críticas de Reflow

1. **AP-27 (RBAC bypass via null Context)**: regla 11 + pre-commit hook. Imposibilita el patrón desde día 1.
2. **AP-33 (file system disclosure)**: regla 12 + whitelist + audit logging. Filesystem access SOLO via wrappers controlados.
3. **AP-39 (cliente↔server desync)**: CI pipeline rechaza deploys con version mismatch. Build cliente y server juntos.

Si MX60 evita estas tres, ya está significativamente mejor que Reflow desde día 1, sin esfuerzo extra. **Diseño defensivo preventivo gana sobre fix reactivo siempre**.
