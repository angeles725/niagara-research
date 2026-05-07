# Niagara N4 — Mental Model · Bloque 29

**Tema**: Web tier completo — Jetty embedding + BWebServlet registry + filter chain (CSRF/Auth/Session) + REST endpoints matrix + WebSocket upgrade (BOX/Fox) + session lifecycle + authentication 9 schemes + authorization + MIME negotiation + NiagaraRPC JSON-RPC + gotchas producción

**Método**: Investigación empírica READ-ONLY — decompilación `web-rt.jar`/`jetty-rt.jar`/`box-rt.jar`/`bajaux-rt.jar`/`baja.jar` + scan de 974 module JARs buscando servlet subclasses + grep de string constants en bytecode (`javap -c`) para rutas, header names, filter orders + contrastado con `niagara-help/devguide/` y `defaults/system.properties`.

**Conecta con**: Bloque 9 (UI stack + BWebService overview), Bloque 11 (auth + session), Bloque 13.3 (NiagaraRPC multi-transport), Bloque 16 (BNaServlet `/na` ejemplo Analytics), Bloque 18 (CSRF + SCRAM), Bloque 19.17 (BOX protocol), Bloque 22 (BajaScript + BOX muxing + BSubscriber), Bloque 20 (monitors JMX).

---

## 29.0 Contexto — qué añade este bloque vs Bloque 9

Bloque 9.3 cubrió overview genérico del stack web. Bloque 29 profundiza en:

- **Versiones exactas**: Jetty `9.4.54.v20240208` (confirmado en `bin/ext/jetty-all-compact3-9.4.54.v20240208.jar`), Servlet API `javax.servlet-api-3.1.0.jar`.
- **Inventario COMPLETO de 50+ servlets** descubiertos via scan `find . -name "*Servlet.class"` en 974 JARs.
- **Filter chain real** del `BJettyWebServer.configureNiagaraWebApp()` — 7 filtros built-in + DoS/QoS opcionales + `BProfileFilterFactory` extensible.
- **Auth 9 schemes** con tabla comparativa (endpoint → scheme → protocolo).
- **Session lifecycle** con Jetty `DefaultSessionCache` + `NiagaraSessionCache` override + `super_session_id` para federación + cookies `niagara_userid`/`niagara_essential_session_support`/`niagara_current_sso_scheme`.
- **REST matrix** con headers exactos (`x-niagara-csrfToken`, `Authorization: HELLO`), MIME response, auth gating.
- **WebSocket upgrade** específico BOX (`/box` → HTTP POST handshake → WS upgrade `/wsbox`).
- **Gotchas productivos** — filter order invariants + same-site + request body limits + panic state.

Bloque 18 detalló CSRF y SCRAM. Acá los ubicamos dentro del pipeline filter-chain real.

---

## 29.1 Jetty embedded architecture

### 29.1.1 Versión exacta

**JAR**: `bin/ext/jetty-all-compact3-9.4.54.v20240208.jar` (135 KB → aggregate compact3; Niagara usa compact3 subset sin JSP/JSTL/Taglib).

MANIFEST confirmado:
- `Bundle-SymbolicName: org.eclipse.jetty.aggregate.all.compact3`
- `Bundle-RequiredExecutionEnvironment: JavaSE-1.8`
- `Bnd-LastModified: 1709645472669` → 2024-03-05 UTC.
- `Import-Package` lista `javax.servlet;version="[3.1.0,5)"` → Servlet 3.1 mandatory.

Jetty CVE post-9.4.54 (CVE-2024-xxxx async timeout, etc.) NO parcheados en N4.14.0.162 — el JAR fue construido 1 semana antes del release Niagara (2024-05-28). Supervisor expuesto a internet debe anteponer reverse proxy con WAF.

### 29.1.2 BJettyWebServer — wrapper principal

Clase: `com.tridium.jetty.BJettyWebServer` extends `javax.baja.web.BWebServer`. **Singleton** del servicio — montado como child de `BWebService` bajo `/Services/WebService/WebServer`.

Campos internos relevantes (de `javap -p`):

```java
private volatile org.eclipse.jetty.server.Server jetty;                      // Server real
private volatile ContextHandlerCollection contextHandlers;                    // múltiples webapps
private volatile PrivilegedQueuedThreadPool threadPool;                       // thread pool custom
private volatile ServerConnector httpConnector;                               // puerto 80/custom
private volatile ServerConnector httpsConnector;                              // puerto 443/custom
private ContextSessionData contextSessionData;                                // session state
private volatile NCSARequestLog ncsaRequestLog;                               // access log
private volatile Authenticator authenticator;                                 // NiagaraAuthenticator
private final Map<String, BINiagaraWebServlet> byServletName;                 // registry
private final Map<BINiagaraWebServlet, Handler> byHandler;                    // reverse map
```

Connector names constantes: `niagaraHttp` y `niagaraHttps`.

Path constants hardcoded:
```java
private static final String preloginPath = "/prelogin";
private static final String loginPath = "/login";
private static final String logoutPath = "/logout";
private static final String webstartPath = "/webstart";
private static final String defaultRealmName = "niagara";
```

### 29.1.3 Thread pool + connectors

`BJettyWebServer` expone como properties:

| Property | Default empírico | Rango | Efecto |
|---|---|---|---|
| `minThreads` | 20 | 10–5000 | Threads base en pool |
| `maxThreads` | 200 | 50–5000 | Techo bajo carga |
| `threadIdleTimeout` | 60 s (BRelTime) | 5–3600 s | Idle antes de reaper |
| `acceptorPriorityDelta` | 0 | -5..+5 | Prioridad threads aceptor |

`PrivilegedQueuedThreadPool` subclase con `setContextClassLoader` permission wrapping — por eso el módulo pide `java.lang.RuntimePermission "setContextClassLoader"` en `module.xml`.

**Connector settings** (`BJettyServerConnector` struct):

```
acceptorThreads: default 1 (QNX) / cores/2 (Win/Linux)
selectorThreads: cores (Jetty default)
outputBufferSize: 32768  (bytes)
outputAggregationSize: 8192
headerCacheSize: 1024
requestHeaderSize: 8192   ← límite URL+headers combinado
responseHeaderSize: 8192
```

**Gotcha**: request header size = 8 KB. URLs largos con `ord:/station:|slot:/...` + filtros BQL superan fácil. Si pegás 413/414, subí `requestHeaderSize`.

### 29.1.4 Protection stack (opcional, off-by-default)

Config `BJettyWebServer` incluye 6 capas de protección configurables (struct children):

1. **denialOfServiceSettings** (`BJettyDoSFilter`): wraps Jetty `DoSFilter`. Props: `maxRequestsPerSec`, `delayMs`, `maxWaitMs`, `throttledRequests`, `throttleMs`, `maxRequestMs`, `maxIdleTrackerMs`, `insertHeaders`, `trackSessions`, `remotePort`, `ipWhitelist`, `tooManyCode` (default 429). **Enabled=false** por defecto.
2. **qualityOfServiceSettings** (`BJettyQoSFilter`): wraps `QoSFilter`. Props: `maxRequests`, `maxPriority`, `waitMs`, `suspendMs`. Controla concurrencia total. Off default.
3. **connectionLimit** (`BJettyConnectionLimit`): Jetty `ConnectionLimit`. Off default.
4. **acceptRateLimit** (`BJettyAcceptRateLimit`): Jetty `AcceptRateLimit` — limit accept rate to prevent SYN floods. Off default.
5. **inetAccessHandler** (`BJettyInetAccessHandler`): allow/block list por IP/CIDR. Props: `allowLoopback`, `allowLocalAdapters`, `allowedList`, `blockedList`. Off default.
6. **sizeLimitHandler** (`BJettySizeLimitHandler`): request/response max size.

Mensaje warning cuando uno de los 6 se activa:
> "Jetty DoSFilter, QoSFilter, AcceptRateLimit, ConnectionLimit, InetAccessHandler, or SizeLimitHandler protection is enabled and may prevent or limit valid web connections to this host."

Crítico: activar estos en un Supervisor con >50 subordinados sin tuning puede cortar conexiones Fox/BOX válidas.

### 29.1.5 Archivo configuración efectivo

No existe `jetty.xml` externo editable. TODO lo configura `BJettyWebServer.doStartWebServer()` vía properties BOG persistidas dentro del `.bog` principal de la station bajo `/Services/WebService/WebServer`.

Logging config: `logback.xml` en `bin/ext/` (confirmado presente). Niagara usa SLF4J `logback-classic` para Jetty internals. Properties toggleables por log level:
- `web.jetty` — server lifecycle.
- `org.eclipse.jetty.*` — Jetty internals.

Para bajar ruido: set `web.jetty` a WARNING (recomendado en warning stdout cuando DoSFilter activo).

---

## 29.2 BWebServlet registry + dispatch

### 29.2.1 Jerarquía de types

```
javax.baja.sys.BAbstractService
  └── javax.baja.web.BWebServlet (abstract, implements BINiagaraWebServlet)
        ├── com.tridium.box.BBoxServlet
        ├── com.tridiumx.analytics.ws.BNaServlet
        ├── sejofa/Honeywell-custom subclases (~35 encontradas)
        └── ... 50+ descubiertos
```

`BWebServlet.servletName` es una `baja:String` Property con default = type name lowercased; override en subclases. La URL path = `/<servletName>/*`.

### 29.2.2 Ciclo registro (runtime, no descriptor-based)

En `BWebServlet`:

```java
public void changed(Property p, Context cx) { ... }  // start() / stop() logic
private void register() { ... BJettyWebServer.register(this); }
private void unregister() { ... BJettyWebServer.unregister(this); }
```

Cuando un `BWebServlet` se monta bajo un `BComponent` vivo y su `serviceStarted()` corre → llama `register(this)` en `BWebServer` (la base abstract) → `BJettyWebServer.doRegister()` construye un `ServletContextHandler` + `ServletHolder` + lo agrega al `ContextHandlerCollection` en runtime.

Esto es lo opuesto a una webapp Servlet estándar (`web.xml` o `@WebServlet`). Niagara **no usa** anotaciones `@WebServlet` ni descriptor XML — **la registry es dinámica en runtime via BComponent tree**. Mover/eliminar el `BWebServlet` en el BOG → `serviceStopped()` → `unregister()` → Jetty lo remueve en vivo (sin restart).

**Por qué importa**: `module.xml` NO contiene `<servlet>` tags. Un search `grep -r "@WebServlet\|servletMapping" modules/` devuelve vacío para tipos Niagara (solo aparece en `javax/servlet/annotation/WebServlet.class` de `electronicSignature-wb.jar` que incluye API standalone).

### 29.2.3 BINiagaraWebServlet — contrato

```java
public interface javax.baja.web.BINiagaraWebServlet {
    String getServletName();
    javax.servlet.http.HttpServlet getHttpServlet();   // devuelve wrapper interno
    ...
}
```

El wrapper interno es `BWebServlet.Servlet` (inner class) que delega a `doService(HttpServletRequest, HttpServletResponse)` → luego a `doGet/doPost/doPut/doDelete(WebOp)` con el wrapper `WebOp` ya poblado.

### 29.2.4 `WebOp` — context per request

`javax.baja.web.WebOp extends javax.baja.file.ExportOp implements com.tridium.util.SecurableContext`.

Campos:
```java
BWebService service;
IWebEnv webEnv;
BWebProfileConfig hxProfileConfig;
BWebProfileConfig profileConfig;
HttpServletRequest request;
HttpServletResponse response;
UserAgent userAgent;
String lang;
BWebServlet servlet;
```

Métodos clave:
- `getPathInfo()` — everything past servletName.
- `isSecure()` — true si HTTPS.
- `setContentType(String)` — shortcut.
- `getHtmlWriter()` — retorna `HtmlWriter` (OutputStreamWriter UTF-8).
- `toUri(BOrd)` — encodea ORD en URL.
- `getLanguage()` — locale del request.

El `WebOp` es el puente formalizado Niagara-style para acceder a request/response — la mayoría de subclases override `doGet(WebOp)` en vez de `doGet(HttpServletRequest, HttpServletResponse)`.

### 29.2.5 Tabla: URL routing + wildcard matching

Jetty matching order (estándar Servlet 3.1, confirmado en `addWebServlet`):

| Orden | Pattern | Ejemplo |
|---|---|---|
| 1 | Exact | `/login`, `/logout`, `/prelogin` |
| 2 | Longest prefix | `/module/*`, `/baja/*`, `/ord/*` |
| 3 | Extension | `*.jnlp`, `*.css` |
| 4 | Default | `/` (DefaultServlet) |

`BWebServlet` genera mapping `/<servletName>/*` automáticamente — prefix match. Si dos servlets colisionan (duplicate `servletName`) → `BJettyWebServer` loggea `web.duplicateServlet` y registra solo el primero. Si servletName es inválido (caracteres /, ?, #) → `web.badServletName`.

---

## 29.3 Filter chain completo

### 29.3.1 Orden real del pipeline (extraído de `BJettyWebServer.configureNiagaraWebApp()`)

Strings en bytecode ordenados de forma que corresponde al orden de `addFilter()`:

```
1. BJettyDoSFilter          (EnumSet: REQUEST, optional)            — /*
2. BJettyQoSFilter          (EnumSet: REQUEST, optional)            — /*
3. DiagnosticBenchmarkFilter (diagnosticsEnabled=true)              — /*
4. LocaleFilter              com.tridium.web.filters                — /*
5. ContextFilter             com.tridium.web.filters                — /*  (sets niagara.context)
6. WebStartServletFilter     com.tridium.web.filters                — /webstart/*
7. TridiumSecurityFilter     com.tridium.web.filters                — /*  (SecurityManager boundary)
8. AddSubjectFilter          com.tridium.web.filters                — /*  (JAAS Subject on thread)
9. BProfileFilterFactory-instances (per-module extension)           — variable
```

Luego, **ANTES de llegar a los servlets**, Jetty aplica su chain propio:

```
10. ConstraintSecurityHandler + NiagaraAuthenticator  (auth)
11. NiagaraSecurityHandler                            (authz + login flow)
12. SessionHandler (NiagaraSessionCache)              (session resolution)
13. ServletHandler                                    → dispatch al servlet
```

Y finalmente, el `BWebServlet.doService()` del servlet aplica a NIVEL APP:

```
14. CsrfProtectedFilter (javax.baja.web.filters) — para POST/PUT/DELETE request methods
15. UserActivityFilter   — marca last-activity timestamp para auto-logoff
```

### 29.3.2 Gotcha clave — orden Auth vs CSRF

**Auth (10) corre ANTES que CSRF (14)**. Razón: CSRF requiere session válida (CsrfProtectedFilter lee `NiagaraWebSession` y compara token server-side contra `x-niagara-csrfToken` header o form field `csrfToken`). Si la auth no pasó → no hay session → CSRF devuelve `"Unauthorized - Niagara session is empty"`.

String constante confirmado en `CsrfProtectedFilter.class`:
> `Unauthorized - Niagara session is empty`
> `Problem decoding csrf token from request`
> `csrf.token.error`

Invariant: **NUNCA reordenar — si CSRF corre antes, todo request anónimo al `/login` fallaría con token missing paradójicamente**.

### 29.3.3 CsrfProtectedFilter internals

```java
public class CsrfProtectedFilter implements Filter {
    private String[] methods;  // init-param "httpMethod" → "POST,PUT,DELETE,PATCH" default

    public void doFilter(req, resp, chain) throws IOException, ServletException {
        if (métodosAplicables.contains(req.getMethod())) {
            if (!CsrfUtil.verifyCsrfToken(req)) {
                throw new CsrfException("csrf.token.verify.error");
            }
        }
        chain.doFilter(req, resp);
    }
}
```

`CsrfUtil.verifyCsrfToken` chequea primero header `x-niagara-csrfToken`, luego form field `csrfToken`. Token se genera en login y persiste en `NiagaraWebSession` attribute. Codificación: base64 + UTF-8.

Strings de error (`csrf.token.missing.error`, `csrf.token.invalid.error`, `csrf.token.verify.error`) son lexicon keys → message user-facing customizable.

**Exempt paths**: `/logout` y `/logoutConfirm` NO validan CSRF (strings confirmados en bytecode).

**GOTCHA G29-CSRF-CROSS-FRAME (NUEVO 2026-05-06, ver Bloque 52)**: El token CSRF es legible cross-frame si un iframe es same-origin con el shell ancestor que lo hosta. Niagara inyecta `<input id="csrfToken">` en CADA HTML renderizado, incluyendo el shell que monta iframes de módulos custom. Esto NO es un bug — es feature implícita del modelo (server-side storage + delivery por HTML inline) que permite SPAs custom obtener el token sin endpoint dedicado vía `window.top.document.querySelector('input#csrfToken').value`. El defensor contra abuso es `X-Frame-Options: SAMEORIGIN`/`Content-Security-Policy: frame-ancestors`, NO el modelo CSRF en sí. Si el deploy desactiva XFO o lo cambia a `ALLOW-FROM`, el token queda expuesto a robo vía iframe embedding.

**Empírico 2026-05-06**: `GET /login` con `JSESSIONID` válido retorna **302 redirect, body vacío** — la extracción regex del token desde `/login` (asumida en Bloque 18.5.1 + Bloque 47.4.1) NO funciona post-auth. Esto canonizó el patrón **Plan E (DOM ancestor) primario, Plan A (`/login` fetch) fallback** documentado en Bloque 52.

### 29.3.4 TridiumSecurityFilter — boundary SecurityManager

`com.tridium.web.filters.TridiumSecurityFilter` — activa contexto de `SecurityManager` (Java 2 policy) para el thread actual antes de invocar el chain. Wraps `ServletResponse` en `TridiumSecurityServletResponse` que intercepta `sendError`/`sendRedirect` para aplicar policy.

Esto es lo que permite que el policy `bin/policy/niagara.policy` firme grants a código Jetty vs código Niagara vs código de módulos específicos. Sin este filter el SecurityManager no sabría qué codeBase aplicar.

### 29.3.5 AddSubjectFilter — JAAS binding

Pone el `javax.security.auth.Subject` autenticado en el thread context via `Subject.doAs()` (usa `AuthPermission "doAs"` declarado en `web-rt module.xml`). Subsecuentes `Subject.getSubject(AccessController.getContext())` devuelven el usuario vigente — consumido por `BUserService.getAuthenticatedUser()`.

### 29.3.6 ContextFilter — sets BComponent context

Attach `niagara.context` + `niagara.target` como request attributes → consumibles por servlets via `req.getAttribute("niagara.context")` y por el wrapper `WebOp`. Esto evita resolver ORD repetidamente por filter.

### 29.3.7 LocaleFilter — lexicon + charset

Wraps request en `LocaleServletRequest` con `Locale` derivada de:
1. cookie `niagara_locale` si existe.
2. `Accept-Language` header primer valor.
3. Locale de la `BUser` si autenticado.
4. Server locale default.

Setea `req.setCharacterEncoding("UTF-8")` si no venía. Implica que casi todo lo que llega al servlet ya es UTF-8.

---

## 29.4 REST endpoints matrix completo

Compilación empírica de 50+ servlets discovered via `find modules/ -name "*Servlet.class"` + inspección de `servletName` static field + análisis de `register()` calls.

### 29.4.1 Core Niagara servlets (web-rt.jar)

| URL path | Handler class | Methods | Auth | MIME req | MIME resp | Propósito |
|---|---|---|---|---|---|---|
| `/ord/*` | `com.tridium.web.servlets.OrdServlet` | GET/POST/PUT/DELETE/TRACE/OPTIONS | yes | varies | text/html, `module://theme` | **ORD resolution universal** — resuelve ORD, busca `BServletView` agent, delega. Core del framework (Bloque 9.3.4). |
| `/login` | `com.tridium.web.servlets.LoginServlet` | GET/POST | NO (punto de entrada) | application/x-www-form-urlencoded | text/html | Form login + SSO dispatch via `BAuthenticationService`. Strings: `j_username`, `niagara_current_sso_scheme`, `niagara_essential_session_support`. |
| `/logout` | `com.tridium.web.servlets.LogoutServlet` | GET/POST | yes | — | 302 redirect a `/prelogin` | Session invalidate + cookie clear. |
| `/logoutConfirm` | `com.tridium.web.servlets.LogoutConfirmServlet` | GET | yes | — | text/html | Confirma logout en HTML (dialog "Are you sure?"). |
| `/prelogin` | `com.tridium.web.servlets.PreloginServlet` | GET | NO | — | text/html | Landing pre-auth — lista schemes disponibles, branding. |
| `/file/*` | `com.tridium.web.servlets.FileServlet` | GET/PUT/DELETE | yes + file perms | varies (text/plain, image/*) | varies | Serve `!file/` ORDs — imágenes, .bog, CSS. Cache 30 días (`max-age=2592000`) para `png|svg|gif|jpg|jpeg`, private NO cache para .bog. Path traversal filter: `[|]\|([.][.])`. |
| `/webstart/*` | `com.tridium.web.servlets.WebStartServlet` | GET | varies | — | application/x-java-jnlp-file | JNLP para Workbench remote launch. |
| `/require/config.js` | `com.tridium.web.servlets.RequireJsConfigServlet` | GET | yes | — | application/javascript | RequireJS config per-station (paths, shim). |
| `/session/timeout` | `com.tridium.web.servlets.SessionTimeoutServlet` | GET | yes | — | text/html | Timeout warning page + auto-refresh script. |
| `/speedtest` | `com.tridium.web.servlets.SpeedTestServlet` | GET/POST | admin | application/octet-stream | application/octet-stream | Bandwidth test endpoint — Workbench usage. |
| `/wb/*` | `com.tridium.web.servlets.WbServlet` | GET | yes + licenseWb | — | varies | WbApplet bootstrap + JxBrowser resources (`/wb/bin/ext/jxbrowser`, `/wb/wbapplet`). |
| `/viewAllOrds` | `com.tridium.web.servlets.ViewAllOrdServlet` | GET | debug admin | — | text/html | Debug list de ORDs registered. |
| `/spy/*` | `com.tridium.web.servlets.BSpyServlet` | GET | admin (spy perm) | — | text/html | **SpyPages runtime debugging** (Bloque 10.5 + 20.5). Root `/spy/stationJetty` + `/spy/engineManager` + `/spy/hogs` + per-component spies. |
| `/cspReport` | `com.tridium.web.servlets.CspReportServlet` | POST | NO | application/csp-report | 204 | CSP violation reports from browser. |
| `/login-file/*` | `com.tridium.web.servlets.LoginFileServlet` | GET | NO | — | varies | Serve anonymous assets on login page (logo, CSS). |
| `/clientEnv` | `com.tridium.web.BClientEnvServlet` | GET | yes | — | application/json | Client environment detection (UA parse, form factor). |
| `/default/*` | `com.tridium.web.servlets.DefaultServlet` | GET | — | — | — | Jetty DefaultServlet for `/` catch-all. |
| `/rpc/*` | `javax.baja.web.servlets.NiagaraRpcServlet` | POST | yes | application/json | application/json; charset=utf-8 | **JSON-RPC** (Bloque 13.3). Path regex `/([^/]+)/(.+)` → `/<rpcName>/<methodName>`. Headers: `Cache-Control: no-store`. |
| `/securityCheck` | `javax.baja.web.servlets.SecurityCheckServlet` | POST | yes | form | — | `/j_security_check` form POST target (Servlet 3.1 login). |
| `/unauth/*` | `javax.baja.web.servlets.UnauthenticatedServlet` | GET/POST | NO | — | varies | Endpoint para stuff público (license feature `UNAUTHENTICATED_SERVLET` gated). |

### 29.4.2 BOX + WebSocket + BajaScript (box-rt, bajaux-rt)

| URL path | Handler | Methods | Auth | MIME resp | Propósito |
|---|---|---|---|---|---|
| `/box` | `com.tridium.box.BBoxServlet` | POST | yes | application/json; charset=utf-8 | Handshake inicial BOX (Bloque 19.17 + 22.12). Devuelve session params + WS endpoint. |
| `/wsbox/*` | `com.tridium.box.BoxWebSocketServlet` (extends `org.eclipse.jetty.websocket.servlet.WebSocketServlet`) | GET Upgrade | yes (cookie + CSRF one-time) | binary | WebSocket BOX channel real. Idle timeout + max text/binary buffers configurables via system props `box.ws.idleTimeout`, `box.ws.maxTextMessageBufferSize`, etc. |
| `/query/*` | `com.tridium.box.QueryServlet` | GET/POST | yes | application/json | Query BQL-like via BOX endpoint — regex `DATA_PATTERN`, `SOURCE_PATTERN`, `LAST_TIME_PATTERN`. |
| `/wbWidget/*` | `com.tridium.ux.WbWebWidgetServlet` | GET | yes | text/html | Bajaux widget bootstrap — genera HTML container con `niagara.*` injections para embedding bajaux widgets. Keys: `themeName`, `useLocalWbRc`, `attachAfterInit`. |

### 29.4.3 Analytics + BQL + Hierarchy + WebChart

| URL path | Handler | Methods | Auth | MIME resp | Propósito |
|---|---|---|---|---|---|
| `/na/*` | `com.tridiumx.analytics.ws.BNaServlet` | GET/POST | yes + `NA_API` role | `text/plain` | Bloque 16.5 — Analytics API. |
| `/naQuery/*` | `com.tridiumx.analytics.util.AnalyticQueryServlet` | POST | yes + `NA_API` | application/json | Analytics query engine. |
| `/naChart/*` | `com.tridiumx.analytics.chart.AnalyticsChartFileServlet` | GET | yes | image/svg+xml | Analytics chart images. |
| `/hierarchy/*` | `com.tridium.hierarchy.HierarchyServlet` | GET | yes | application/json | Hierarchy service (Bloque 5.3.5). |
| `/webChart/*` | `com.tridium.webChart.WebChartQueryServlet` | POST | yes | application/json | WebChart time-series query. |
| `/webChartFile/*` | `com.tridium.webChart.WebChartFileServlet` | GET | yes | image/* | WebChart image export. |
| `/seriesTransform/*` | `com.tridium.seriestransform.servlets.SeriesTransformWebChartQueryServlet` | POST | yes | application/json | History transform pipeline. |

### 29.4.4 Auth + SAML + ClientCert

| URL path | Handler | Methods | Auth | Propósito |
|---|---|---|---|---|
| `/clientCertAuth` | `com.tridium.clientCertAuth.web.ClientCertAuthServlet` | GET | mTLS (client cert required in TLS handshake) | Extrae cert del SSLContext, mapea a BUser via `CN=` o extension. |
| `/saml/idpRequest` | `com.tridium.saml.idp.SAMLIdPAuthnRequestServlet` | POST | — | Niagara as SAML Identity Provider — genera AuthnRequest. |
| `/saml/idpLogin` | `com.tridium.saml.idp.SAMLIdPProcessLoginServlet` | POST | — | Procesa login SAML server-side cuando Niagara ES el IdP. |
| `/saml/rp` | `com.tridium.saml.rp.servlet.SAMLRPServlet` | GET/POST | — | Niagara as Relying Party — redirect a IdP externo. |
| `/saml/consumer` | `com.tridium.saml.rp.servlet.SAMLConsumerServlet` | POST | — | SAMLResponse consumer (ACS endpoint). |

### 29.4.5 File download + backup + icons + velocity

| URL path | Handler | Propósito |
|---|---|---|
| `/backupDownload` | `com.tridium.cloud.client.backup.BackupDownloadServlet` | Descarga `.dist` backup a cloud. |
| `/velocity/*` | `com.tridium.velocity.BVelocityServlet` | Render `.pxvm` Velocity templates (requires `axvelocity` license). |
| `/uxBuilder/*` | `com.tridium.uxBuilder.servlet.UxBuilderServlet` | UI builder en workbench (wb profile). |
| `/paletteEditor/*` | `com.tridium.webeditors.ux.servlets.PaletteServlet` | Palette editor web-based. |
| `/stationOutput/*` | `com.tridium.platform.hx.BStationOutputServlet` | Platform daemon station stdout/stderr streaming. |
| `/securityDashboard/*` | `com.tridium.nss.dashboard.SecurityDashboardServlet` | Security dashboard (NSS module). |
| `/bacnetAws/*` | `com.tridium.bacnetAws.servlets.BacnetAwsServlet` | BACnet AWS bridging. |
| `/bacnet/sc/*` | `com.tridium.bacnet.stack.link.sc.connection.jetty.BJettyScWebSocketAcceptor$JettyScWebSocketServlet` | **BACnet/SC WebSocket** (Bloque 23 BVLC over TLS 1.3 port 49152). |
| `/httpClient/*` | `com.tridium.httpClient.servlet.BStringServlet` | HTTP client string endpoint. |
| `/silk/*` | `com.tridium.silk.BSoapServlet` | SOAP for SYLK devices. |
| `/galileo/signals/*` | `com.honeywell.signals.transport.SignalRServlet` + forever-frame + long-poll + SSE (4 transports) | Honeywell SignalR — real-time push. |
| `/galileo/points/*` | `com.honeywell.galileo.point.servlets.BPointListServlet` | Honeywell Galileo point list. |

### 29.4.6 Customer-specific (sejofa, Honeywell vertical)

~35 custom servlets en módulos `sejofa/*-ux.jar` descubiertos. Ejemplos:
- `/alsuper/*` → `com.sejofa.Alsuper.ux.BAlsuperServlet`
- `/dtcr/*` → `com.sejofa.datacenter.ux.BDtcrServlet`
- `/sdash/*` + `/sdashws/*` → SdashServlet + SdashSocketServlet
- `/mcp/*` → `com.sejofa.mcpbridge.BMcpServlet` (Model Context Protocol bridge)

Todos siguen el mismo pattern: extends `BWebServlet`, servletName hardcoded, cuelgan del `/Services/WebService/` o del BComponent que los monta.

### 29.4.7 Test + deprecated

- `/bsh/*` — BeanShell servlet en `test-wb.jar` (solo profile wb, NO producción).
- HSQLDB `org.hsqldb.server.Servlet` — en `rdbHsqlDb-rt.jar`, solo si RDB HSQL habilitado.
- `javax.servlet.GenericServlet/HttpServlet` redundantemente embebidos en `electronicSignature-wb.jar` (mal packaging — duplicate Servlet API).

### 29.4.8 Total

Conteo: **~53 servlets activos** en production Supervisor N4.14.0.162 (sin contar `*-wb.jar` profile-gated). De estos, 20 son core Tridium + 10 verticales Honeywell + 35 custom sejofa + SAML/BACnet/Analytics específicos.

---

## 29.5 Session lifecycle deep

### 29.5.1 Classes involucradas

```
com.tridium.jetty.NiagaraLoginService         extends DefaultIdentityService
com.tridium.jetty.NiagaraAuthenticator        extends LoginAuthenticator
com.tridium.jetty.NiagaraHttpSession          implements NiagaraWebSession
com.tridium.jetty.NiagaraSessionCache         extends DefaultSessionCache
com.tridium.jetty.NiagaraSessionDataStoreFactory
com.tridium.jetty.NiagaraSessionIdManager
com.tridium.jetty.SessionIdChangeHandler
com.tridium.jetty.SessionInvalidationHandler
com.tridium.jetty.ContextSessionData
com.tridium.jetty.HttpSessionLogger
com.tridium.web.session.NiagaraWebSession     (interface app-level)
com.tridium.web.session.WebSessionUtil        (helpers)
```

### 29.5.2 NiagaraHttpSession — estructura

```java
public class NiagaraHttpSession implements NiagaraWebSession {
    private Collection<HttpSession> httpSessions;     // multiple Jetty sessions aggregadas
    private ConcurrentMap<String,Object> attributes;  // thread-safe map
    private String superId;                            // "super session" — spans logins across contexts
    private String remoteHost;
    private long creationTime;
    private String id;                                 // Jetty session ID
    private boolean valid;
    private boolean createdWithSuperSessionId;
}
```

**Key insight**: una `NiagaraHttpSession` agrupa MÚLTIPLES `HttpSession` Jetty — una por `ServletContext` (cada `BWebServlet` registra su propio context). Por eso `httpSessions` es una `Collection`. Esto evita re-login cuando el user cambia entre `/ord` → `/na` → `/box` — todos comparten la misma NiagaraHttpSession via el `superId`.

### 29.5.3 Cookies emitidas

Strings empíricos en `NiagaraAuthenticator` + `LoginServlet`:

| Cookie | Propósito | Attributes |
|---|---|---|
| `niagara_userid` | User name remember (opcional, si `rememberUserIdCookie=true`) | HttpOnly no (populated a form field) |
| `niagara_essential_session_support` | Feature detection — did browser accept cookies? | HttpOnly, SameSite según config |
| `niagara_current_sso_scheme` | Tracking del scheme SSO actual | HttpOnly |
| `niagara_origin_uri` | Dónde redirigir post-login | HttpOnly, Secure si HTTPS |
| `niagara_failure_cause` | Razón último fallo (`BAD_PWD`, `LOCKED`, etc.) | short-lived |
| `niagara_locale` | Locale preference | persistent |
| `JSESSIONID` (Jetty default) | Session ID real | HttpOnly, Secure condicional |

Cookie attributes central:
- **Secure**: Set cuando HTTPS. Forzado si `BWebService.httpsOnly=true`.
- **HttpOnly**: Always para JSESSIONID.
- **SameSite**: Controlado por `BSameSiteEnum` (`none` / `lax` / `strict`). Default = `lax`. Property on `BWebService`.

### 29.5.4 SameSite gotcha (Bloque 9.3.5 expandido)

String constante en Jetty bytecode: `org.eclipse.jetty.cookie.sameSiteDefault`.

Flow:
1. `BWebService.getSameSite()` → pasa a `configureSameSite(ContextHandler$Context)` → setea attribute `org.eclipse.jetty.cookie.sameSiteDefault`.
2. Jetty aplica a TODAS las cookies emitidas via `Response.addCookie()`.
3. **Browsers modernos requieren `SameSite=None; Secure`** (Chrome 80+, Firefox 96+). Si seteás `SameSite=None` pero HTTPS está off o cert autoinstaladoruto sin confiar → browser ignora silentemente la cookie → session no persiste → loop infinito de login.
4. Workaround: o `Secure=true + HTTPS real + cert chain trustable`, o fallback a `SameSite=Lax`.
5. Producción detrás de reverse proxy terminando TLS: necesitás que Niagara detecte HTTPS aunque connector sea HTTP. Verificar `X-Forwarded-Proto: https` con Jetty `ForwardedRequestCustomizer` — esto NO viene habilitado por defecto en Niagara. Solución: configurar reverse proxy para inyectar `X-Forwarded-Proto` + habilitar el customizer vía connector config.

### 29.5.5 Timeouts

- **maxInactive (idle)**: `updateToAuthenticatedMaxInactiveInterval()` en `NiagaraAuthenticator` — reseteable post-auth. Default 15 min (Bloque 20.8.5, confirmado). Configurable en `BAutoLogoffSettings` dentro de `BWebService`.
- **maxLife absoluto**: NO hay cap absoluto built-in en `NiagaraHttpSession`. El `BAutoLogoffSettings` permite configurarlo si la organización lo necesita.
- **Session reap**: Jetty scavenger corre cada N segundos (Jetty default 10 min, no modificado en Niagara). Sesiones idle > maxInactive se invalidan + se emite `niagara_failure_cause=IDLE_TIMEOUT` cookie.

### 29.5.6 Session fixation protection

En `NiagaraAuthenticator.validateRequest()` + `recreateSession()`:

```java
private static NiagaraWebSession recreateSession(HttpServletRequest req, String superId) {
    // Invalidate old session, create new one, copy superId
}
```

Post-login exitoso → el session ID se regenera (Jetty `changeSessionId()` via `SessionIdChangeHandler`). El attacker que plantó un JSESSIONID antes de login no lo puede reusar. `superId` se preserva para correlación cross-context.

Esto cumple OWASP A1 session fixation.

### 29.5.7 Logout

`LogoutServlet`:
1. Llama `req.getSession().invalidate()` → dispara `SessionInvalidationHandler`.
2. Jetty `SessionHandler` emite `Set-Cookie: JSESSIONID=; Max-Age=0`.
3. `HttpSessionLogger` agrega log entry (audit trail).
4. Redirect 302 a `/prelogin` (o `loginUri` config).

**Gotcha**: si hay múltiples `HttpSession` aggregados en el mismo `NiagaraHttpSession.httpSessions`, debe invalidar TODOS. El método `NiagaraHttpSession.invalidate()` itera la colección.

### 29.5.8 Concurrent sessions

NO hay limit configurable por user built-in. Un user puede tener N sesiones activas (multiple browsers, multiple devices). Algunas organizaciones implementan esto con un custom `BAuthenticationScheme` que hace housekeeping. El `BUserService` expone `getAllSessions(BUser)` pero sin enforcement.

### 29.5.9 Storage — memoria vs persisted

`NiagaraSessionCache extends DefaultSessionCache` → in-memory `ConcurrentHashMap`. Sessions NO persistidas a disco por default. En restart de station → todas las sessions pierden state, users deben re-login.

Strings en bytecode: `retainSessions`, `cacheSessionsAndRestart` (action). Método: `cacheSessionsAndRestart()` serializa sessions a `ContextSessionData`, hace restart del WebServer, las restaura. Esto se usa cuando cambiás `httpPort`/`httpsPort`/`cipherSuiteGroup` — propiedad isRestartRequired()=true pero queremos conservar sesiones.

---

## 29.6 Authentication variants deep — 9 schemes

### 29.6.1 Tabla comparativa

| Scheme | Tipo | Endpoint trigger | Handshake | Keystore/config | Bloque ref |
|---|---|---|---|---|---|
| **Digest SCRAM-SHA256** | Challenge-response | `/login` + `Authorization: HELLO` | 6-step SCRAM | BUser hashed password + salt | Bloque 18.5 |
| **HTTP Basic** | Clear (legacy) | `/login` + `Authorization: Basic` | 1-step | BUser clear/hashed | Bloque 11.3.1 |
| **Digest HTTP** | MD5 challenge (deprecated) | `/login` + `Authorization: Digest` | 2-step | BUser + realm=`niagara` | Bloque 11.3.1 |
| **Header-based (SSO)** | Trusted proxy header | Cualquiera con `niagara_current_sso_scheme=header` | Inmediato | Lista headers confiables + CIDR whitelist | nuevo bloque |
| **SAML 2.0 RP** | Redirect + consume SAMLResponse | `/saml/rp` → IdP → `/saml/consumer` | 3-step | IdP metadata.xml + cert | Bloque 11.3.1 |
| **SAML 2.0 IdP** | Niagara as IdP | `/saml/idpRequest` | — | Niagara signing cert | — |
| **Kerberos SPNEGO** | GSS-API | `/login-kerb` + `Authorization: Negotiate` | Kerberos ticket | `krb5.conf` + keytab + AD | Bloque 11.3.1 |
| **LDAP Bind** | External dir | `/login` form | Bind + search | LDAP URL + service account + search base | Bloque 11.3.1 |
| **Google TOTP (2FA)** | Second factor | Post-primary auth | TOTP code 6 digits | Per-user seed | Bloque 11.3.1 |
| **mTLS client cert** | TLS handshake | `/clientCertAuth` | TLS mutual | Server trust store + cert→user mapping | — |

### 29.6.2 NiagaraAuthenticator — dispatch logic

Extracto clave:

```java
public Authentication validateRequest(req, resp, mandatory) {
    String authHeader = req.getHeader("Authorization");
    // Scheme: "HELLO <base64-payload>" → SCRAM
    // Scheme: "Basic <base64>" → HTTP Basic
    // Scheme: "Digest ..." → Digest
    // Scheme: "Negotiate ..." → Kerberos
    // Otherwise: session-based (form POST /login already handled)

    if (authHeader contains "HELLO") return authenticateServlet(...);  // SCRAM
    if (authHeader contains "Basic") return authenticateServlet(...);
    if (authHeader contains "Digest") return authenticateServlet(...);
    if (authHeader contains "Negotiate") return authenticateKerberos(...);
    // Fallback: header-based SSO
    return authenticateHeader(...);
}
```

### 29.6.3 Header-based SSO

`authenticateHeader()` inspecciona headers custom configurables (comunmente `X-Remote-User`, `X-Authenticated-User`) — el scheme `BHeaderAuthenticationScheme` (cuando instalado vía módulo `*AuthScheme`). **Crítico**: debe estar whitelisted a nivel network (reverse proxy confiable, IP filter en `BJettyInetAccessHandler`). Si un cliente arbitrario pega `X-Remote-User: admin` → compromiso total. Niagara exige CIDR restriction.

### 29.6.4 Error codes

Strings de `NiagaraAuthenticator`:
- `"Authentication failed."` — generic fallback.
- `"User <%s> attempting web login with unsupported authentication scheme <%s>"` — scheme mismatch.
- `"Unable to Reset Password"` — password expired + reset failed.
- `"Security Exception"` — SecurityManager denial.
- `"Invalid authorization header, colon delimiter not found."` — malformed Basic.
- `"Invalid authorization header, not valid base64."`
- `"Invalid authorization header, scheme not supported: <scheme>"`
- `"Attempting servlet authentication but associated authentication scheme does not support web logins"`

### 29.6.5 Password expiration + forceReset flow

`NiagaraAuthenticator` supports `forceReset`:
1. Login exitoso pero `password.expired=true`.
2. Attribute `passwordExpires` añadido al request.
3. Jetty emite cookie `forceReset=yes-reset` y redirige a `/changeUserPassword` endpoint.
4. User cambia password → `BChangeUserPasswordRpc` (en `com.tridium.web.rpc`) procesa.
5. Post-success → cookie `forceReset=no-reset` + acceso normal.

### 29.6.6 WWW-Authenticate header challenges

Cuando auth falla en un endpoint protegido → `NiagaraAuthenticator.validateRequest` setea `WWW-Authenticate` con el scheme. Ej: `WWW-Authenticate: HELLO realm="niagara"` — indicando SCRAM espera.

Para clientes no-browser (scripts, Workbench) esto dispara el flow correcto. El browser ignora los schemes no standard (HELLO) y cae a form login.

---

## 29.7 Authorization + RBAC en web

### 29.7.1 Per-servlet permission gating

`BWebServlet.getPermissions(Context)` — override en subclases. Default delegates a `BPermissions.ADMIN` o similar. El `NiagaraSecurityHandler` (subclase de Jetty `ConstraintSecurityHandler`) corre este check post-auth.

Ejemplo `BBoxServlet`:
```java
public BPermissions getPermissions(Context cx) {
    return BPermissions.make(BPermissions.OP_READ);  // requires read on /
}
```

### 29.7.2 Agent-based view permissions

Cuando un `BWebServlet` sirve un **view** sobre un `BComponent` target (patrón típico `/ord/station:|...|view:X`):

1. `OrdServlet` resuelve ORD → target `BComponent`.
2. Busca `BServletView` agents registered en `module.xml` con `requiredPermissions="r"` o `"rw"`.
3. Compara contra permisos efectivos del user sobre el component (derived de `BUser.roles` × `BCategory` del target).
4. Si falla → 403 + mensaje `"User has no permissions to access view: '<name>'"` (string confirmed in OrdServlet).

Ejemplo en `web-rt module.xml`:
```xml
<type class="com.tridium.web.servlets.BFileDownloadView" name="FileDownloadView">
  <agent requiredPermissions="r">
    <on type="baja:IDataFile"/>
  </agent>
</type>
<type class="com.tridium.web.servlets.BFileUploadView" name="FileUploadView">
  <agent requiredPermissions="w">
    <on type="file:ITextFile"/>
  </agent>
</type>
```

### 29.7.3 Return codes

| Code | Condición |
|---|---|
| 200 | OK |
| 204 | No content (DELETE, CSP report) |
| 301/302 | Redirect (login flow, logout) |
| 400 | Bad request (malformed ORD, BQL syntax) |
| 401 | No auth — Jetty challenge emitido |
| 403 | Auth OK pero no tiene permiso sobre el recurso |
| 404 | Path no matched → DefaultServlet fallback |
| 405 | Method not allowed (ej. PUT a un servlet read-only) |
| 409 | Conflict (concurrent edit en `WbServlet`) |
| 413 | Payload too large (si `sizeLimitHandler` enabled) |
| 414 | URI too long (excede `requestHeaderSize`) |
| 429 | Too Many Requests (DoSFilter tooManyCode) |
| 500 | Server error — log + (showStackTrace ? stack : generic) |
| 503 | Service unavailable (panic state, Bloque 29.14) |

### 29.7.4 Licensing gating

`OrdServlet` checkea features `ui` (HTML5) y `ui.wb` (Workbench). Strings:
- `"web ui not licensed"`
- `"web ui.wb not licensed"`

Si feature ausente → 403. Es el gate entre Supervisor (ui) y Station (ui o ui.wb según license).

---

## 29.8 MIME type handling + content negotiation

### 29.8.1 Jetty default MIME map

Jetty trae ~50 types estándar (text/html, application/json, image/png, etc.). Strings confirmados en BJettyWebServer: `text/css`, `text/html`, `text/javascript`, `text/plain`, `application/javascript`, `application/json`, `application/rss+xml`, `application/x-javascript`, `application/xhtml+xml`, `application/xml`.

### 29.8.2 Compression gzip

Jetty `GzipHandler` activado si `BWebService.gzipEnabled=true` (default true). Whitelist MIME types para compress:

```
text/html
text/plain
text/xml
text/css
application/javascript
application/json
application/xml
application/xhtml+xml
application/rss+xml
text/javascript
application/x-javascript
```

(strings `GZIP_MIME_TYPES` en BJettyWebServer bytecode).

Threshold: default 150 bytes (Jetty default, no modificado). Headers: `Content-Encoding: gzip` emitido condicional a `Accept-Encoding: gzip` del cliente.

### 29.8.3 Custom MIME types Niagara

- `application/x-java-jnlp-file` — `/webstart/*` WebStartServlet.
- `application/java-archive` — module JARs via `/module/*`.
- `application/octet-stream` — `/speedtest`, BOX binary frames, BACnet/SC.
- `application/csp-report` — CSP violation POST bodies.

### 29.8.4 Content-Type peculiarities

**`/na` Analytics usa `text/plain`** (Bloque 16.5.1) — NO `application/json` — por historia legacy. Browsers no interpretan el body como script, por ende evita XSS autoejecutado. Workaround del browser: parsear manually como JSON.

**`/rpc` usa `application/json; charset=utf-8`** (standard).

**`/ord` devuelve el content-type del view agent** — variable.

**`/file` adivina MIME via extensión**:
- `.png|.svg|.gif|.jpg|.jpeg` → image/* + cache 30 días public.
- `.bog|.bog.gz` → application/octet-stream + NO cache.
- `.css` → text/css.
- `.html` → text/html.

### 29.8.5 Charset defaults

`LocaleFilter` fuerza `UTF-8` charset en request. `BWebServlet` default response charset = UTF-8. Strings confirmados en múltiples servlets: `text/plain; charset=UTF-8`, `text/html; charset=utf-8`.

### 29.8.6 Accept header negotiation

Niagara NO implementa verdadera content negotiation per-servlet. La mayoría de servlets Niagara ignoran `Accept` y devuelven el type fijo. Excepciones:
- `OrdServlet` — delega a view agent, que puede switchear (JSON/XML/HTML según params `niagara.viewInfo`).
- `BBqlServlet` (parte de query-rt / bql-rt, no en web-rt) — interpreta param `format=csv|json|xml`.

`RestUtil.Accept` inner class (en `com.tridium.web.RestUtil`) — utilitario para servlets que SÍ quieran parsear Accept, no universalmente usado.

---

## 29.9 NiagaraRPC deep (JSON-RPC)

### 29.9.1 Envelope JSON-RPC 2.0

Request:
```json
{
  "jsonrpc": "2.0",
  "method": "changeUserPassword",
  "params": {"username": "bob", "oldPassword": "...", "newPassword": "..."},
  "id": 42
}
```

Response exitoso:
```json
{
  "jsonrpc": "2.0",
  "result": {"success": true},
  "id": 42
}
```

Error:
```json
{
  "jsonrpc": "2.0",
  "error": {"code": -32000, "message": "Password policy violated", "data": {...}},
  "id": 42
}
```

### 29.9.2 Path format

Regex confirmada en `NiagaraRpcServlet.class`:
```java
private static final Pattern rpcPathPattern = Pattern.compile("/([^/]+)/(.+)");
```

→ `POST /rpc/<rpcName>/<methodName>` — ejemplo `POST /rpc/password/changeUserPassword` → busca `BPasswordRpc` clase con método `changeUserPassword`.

### 29.9.3 Handler registration

RPC handlers se registran vía el module.xml + `BI*Rpc` marker. En `web-rt module.xml`:

```xml
<type class="com.tridium.web.rpc.BChangeUserPasswordRpc" name="ChangeUserPasswordRpc"/>
<type class="com.tridium.web.rpc.BFileRpc"                name="FileRpc"/>
<type class="com.tridium.web.rpc.BLexiconRpc"             name="LexiconRpc"/>
<type class="com.tridium.web.rpc.BLogRpc"                 name="LogRpc"/>
<type class="com.tridium.web.rpc.BPasswordRpc"            name="PasswordRpc"/>
<type class="com.tridium.web.rpc.BRegistryRpc"            name="RegistryRpc"/>
```

El servlet descubre handlers via `BNiagaraRpcDispatcher` scanning registry.

### 29.9.4 CSRF en body o header

Confirmado: `CsrfProtectedFilter` lee de header `x-niagara-csrfToken` O form field `csrfToken`. Para JSON RPC los clientes usan header (typical patrón AJAX).

### 29.9.5 Error codes

- `-32700` parse error (body no es JSON válido).
- `-32600` invalid request (no tiene `method`).
- `-32601` method not found.
- `-32602` invalid params.
- `-32603` internal error.
- `-32000..-32099` application-defined (`niagaraRpc.securityMessage`, etc.).

### 29.9.6 Batch requests

JSON-RPC 2.0 spec permite batch `[{...},{...}]`. El servlet iterates + devuelve array de responses. Strings `"Error invoking multi RPC"`, `"Error invoking RPC:"` confirman soporte.

### 29.9.7 Multi-transport

Bloque 13.3 detalló que RPC calls también pueden ir sobre Fox y BOX. Sobre HTTP (este servlet) es el path JSON-RPC estándar. Sobre Fox: frame tipo RPC con payload baja-encoded. Sobre BOX: channel dedicado.

---

## 29.10 WebSocket upgrade (BOX)

### 29.10.1 Handshake

1. Cliente: `POST /box` con body JSON `{"version":"1.0","features":[...]}`.
2. Servidor (`BBoxServlet.doPost`): valida auth, emite respuesta `{"wsEndpoint":"/wsbox","sessionToken":"...","csrfToken":"..."}`.
3. Cliente: `GET /wsbox HTTP/1.1` + `Upgrade: websocket` + `Sec-WebSocket-Key: <random16b>` + `Sec-WebSocket-Version: 13` + Cookie (JSESSIONID) + header `Origin`.
4. `BoxWebSocketServlet` (extends Jetty `WebSocketServlet`) → `configure(WebSocketServletFactory)` setea idle/size limits → hands off a Jetty handshake → response 101 Switching Protocols + `Sec-WebSocket-Accept`.

### 29.10.2 Frame handling

Después del upgrade, Jetty WebSocket framework maneja:
- Text frames (BSON/JSON serializado BOX messages).
- Binary frames (BOX binary payload).
- Continuation frames para messages >64 KB.
- Ping/Pong every N seconds (Jetty default 30 s).
- Close frames graceful.

### 29.10.3 Parámetros configurables (system.properties)

```
box.ws.idleTimeout              default 300000 ms (5 min)
box.ws.maxTextMessageBufferSize default 32768
box.ws.maxTextMessageSize       default 65536
box.ws.maxBinaryMessageBufferSize default 32768
box.ws.maxBinaryMessageSize     default 65536
```

**Gotcha**: `maxTextMessageSize` = 64 KB. Un PX muy grande con include chain profundo (Bloque 22.4) puede exceder → fragmentation debería kickear, pero si el encoder no fragmenta → drop silencioso de la subscription.

### 29.10.4 Auth persistence

El WS inherits cookies del initial `/box` POST. La NiagaraHttpSession persiste durante la vida del WS. Si el user hace logout en otra pestaña → `SessionInvalidationHandler` notifica y el WS recibe close frame `1008 Policy Violation`.

CSRF: validado UNA vez en el `/box` POST. El WS no valida CSRF por frame (performance). Pero un attacker cross-origin no puede abrir WS sin pasar primero por `/box`, y `/box` exige CSRF → invariant preservado.

### 29.10.5 Fox over HTTP tunneling

Niagara soporta también `/fox` como canal HTTP para cuando Fox puerto (4911/5011) está blocked. El `BFoxBoxAcceptor` en box-rt muxea Fox sobre BOX WS. Menos performant que Fox nativo pero atraviesa firewalls corporativos.

---

## 29.11 Static resource serving

### 29.11.1 Module resources

Path pattern: `/module/<moduleName>/rc/<resource>` — servido por `DefaultServlet` mapeado a un custom resource base que resuelve contra `NModule.getResource()`. Cache headers:

- `Cache-Control: private, must-revalidate, max-age=2592000` para images (30 días).
- `Cache-Control: private, must-revalidate, max-age=0` para otros (revalidate every request via ETag).

### 29.11.2 ETag + If-Modified-Since

Jetty `DefaultServlet` emite `Last-Modified` + `ETag` based on resource's lastModified + length. Clientes mandan `If-Modified-Since` / `If-None-Match` → 304 Not Modified si match.

### 29.11.3 BajaScript bundles

Bloque 22.11 detalló `bs.built.min.js` 360 KB servido via `/baja/*` path. Cache-Control agresivo (`max-age=2592000`) para production. Dev builds (webdev mode) emit `no-cache`.

### 29.11.4 Theme resources

String `module://theme` en `FileServlet` → alias para `/Services/WebService/... theme resource`. Themes se aplican per-user via `BWebProfileConfig` attached al `BUser`.

### 29.11.5 Signing vs HTML injection

NO hay sub-resource integrity (SRI) built-in. Las JS bundles se sirven sin `integrity="sha256-..."` attribute. Un MITM que atraviese TLS podría alterar `bs.built.min.js`. Mitigation: TLS propio + HSTS (string `HSTS_MAX_AGE` confirmado en bytecode).

Niagara emite `Strict-Transport-Security` header cuando `BWebService.httpsOnly=true`. HSTS max-age hardcoded al constante (probablemente 1 año = 31536000 seconds según convención Niagara — no verificado empíricamente).

---

## 29.12 Reverse proxy + load balancer considerations

### 29.12.1 X-Forwarded-* handling

Por default Niagara **NO confía** en `X-Forwarded-For` / `X-Forwarded-Proto`. Para activar hay que setear:

- Jetty `ForwardedRequestCustomizer` vía system property o custom connector config — NO expuesto en propiedades BWebService directamente. Workaround: editar el `.bog` manualmente en Workbench para agregar via `BJettyServerConnector` customizers? — no disponible standard, requiere custom module.

**Gotcha práctica**: si pones Niagara detrás de nginx/HAProxy, los access logs (`BNCSARequestLog`) loguean el IP del proxy (127.0.0.1) en vez del cliente real. Para audit trail confiable necesitás custom filter que lea `X-Real-IP`.

### 29.12.2 TLS termination patterns

**Pattern A — TLS passthrough**: balanceador solo forwarda TCP 443, Niagara termina TLS. Ventaja: certs manejados por Niagara (`BAdditionalHttpsCerts` supports SNI). Desventaja: no hay inspección L7 en el balancer.

**Pattern B — TLS termination upstream**: balanceador termina TLS, pasa HTTP plain a Niagara. Ventaja: offload crypto, WAF L7. Desventaja: `req.isSecure()` retorna false en Niagara → `requireHttpsForPasswords=true` bloquea login paradójicamente. **Fix**: setear `httpsEnabled=false + httpEnabled=true` solo en localhost, filter upstream confiable por IP whitelist (CIDR del balancer).

### 29.12.3 SameSite=None requires Secure

Documentado en Bloque 9.3.5. Si el balancer termina TLS y Niagara está en HTTP, las cookies sin `Secure` attribute viajan HTTP entre balancer↔Niagara (local), pero hacia el cliente el cookie ya llevó el attr (seteado por Niagara antes). El problema real: browsers verifican que `SameSite=None` venga con `Secure` EN EL COOKIE EMITIDO, y Niagara pone `Secure` basado en `req.isSecure()` — que es false si HTTP upstream. Resultado: cookies quedan sin `Secure` → browser las rechaza → loop login.

**Fix empírico**: custom Jetty connector customizer que force `req.setScheme("https")` cuando vea `X-Forwarded-Proto: https`.

### 29.12.4 BHttpProxyService CIDR exclusions

Bloque 20.2 — outbound HTTP del Supervisor usa `BHttpProxyService`. Exclusion CIDR list permite rutear tráfico interno directo sin proxy. Esto es OUTBOUND (Supervisor → remote), NO inbound.

---

## 29.13 Virtual hosts + multi-tenancy

### 29.13.1 Single Jetty, multiple contexts

`BJettyWebServer.contextHandlers` es `ContextHandlerCollection` — soporta MÚLTIPLES `ServletContextHandler` bajo una Jetty única. Niagara típicamente usa 1 context (`/`), pero otros módulos pueden agregar contexts adicionales (ej. `/workbench/`).

### 29.13.2 BVirtualHost

NO existe `BVirtualHost` type como tal. Niagara no soporta vhosts Apache-style (múltiples dominios en mismo puerto). Todo va al mismo servlet tree.

### 29.13.3 Multi-tenancy workarounds

Para exposure dual (internal + external) con políticas distintas: corré DOS stations en mismo host, cada una con su `web.service` en puertos distintos. O usá reverse proxy que route `domain1.com` → `:8081` y `domain2.com` → `:8082`. Niagara no tiene multi-tenant nativo.

---

## 29.14 Error handling + custom error pages

### 29.14.1 WebErrorHandler

`com.tridium.jetty.WebErrorHandler` — override de Jetty `ErrorHandler`. Emite HTML con branding Niagara para 4xx/5xx.

### 29.14.2 showStackTrace property

`BWebService.showStackTrace` (boolean, default **true** en dev, **false** en production) — si true, 500 errors incluyen full stack trace HTML-escaped en la response. **SECURITY**: dejarlo true en production expone classpath, versions, paths → useful for attacker fingerprinting.

### 29.14.3 Panic state

String constante en BJettyWebServer: `"panic state"`. Si Jetty falla al arrancar (port bind collision, cert error, SSL context failure) → `BWebServer.ServerState` transiciona a `FAILED`. HTTP requests devuelven 503. Se puede resetear via action `restart`.

Logs característicos:
- `"Failed to bind to http port [%d]"`
- `"Failed to bind to https port [%d]"`
- `"Failed to bind to https port [%d], http port [%d], or both"`
- `"Could not initialize SslContextFactory"`

### 29.14.4 NCSA access log

`com.tridium.jetty.BNCSARequestLog` wraps Jetty `NCSARequestLog`. Format hardcoded `yyyy_mm_dd.request.log`. Path: `${niagara.user.home}/logging/` (confirmed in module.xml `<java-permission name="${niagara.user.home}${/}logging">`).

Formato: extended NCSA (includes referer + user-agent). Rotation diaria. No auto-retention nativo (Bloque 20.8.5 — sin policy por default).

---

## 29.15 Debug + observability

### 29.15.1 SpyPages

`/spy/*` via `BSpyServlet` — admin-gated. Sub-paths descubribles:

- `/spy/stationJetty` → JettySpy (strings `Station Jetty Spy`, `spy:/stationJetty`) — exposed config + runtime stats Jetty.
- `/spy/engineManager` → EngineManager$HogsPage (Bloque 20.5.1).
- `/spy/hogs` → callback hogs.
- `/spy/leaseManager` → lease usage.
- `/spy/sessionManager` → active sessions con superId + remoteHost.
- `/spy/fox` → Fox sessions.
- `/spy/box` → BOX channels.
- `/spy/jxbrowser` → embedded browser state.
- `/spy/sysmem` → JVM heap.

### 29.15.2 JettyStatisticsSpy + JettyStructPropertySpy

Inner classes de `BJettyWebServer`. Expone:
- `JettyStatisticsSpy` → `bytes sent/received`, `requests handled`, `response 2xx/3xx/4xx/5xx counts`, `stat period start`.
- `JettyStructPropertySpy` → serializa los struct properties (DoSFilter, QoSFilter, connectors) como HTML.

### 29.15.3 DiagnosticBenchmarkFilter

Activable via `diagnosticsEnabled=true` en `BJettyWebServer`. Mide latencia per-request en `diagnosticLog` (separate logger `web.jetty.diagnostic`). Histograms no directos, pero se puede computar post-hoc desde el log.

### 29.15.4 JMX

Port 9010 (JMX, Bloque 20). Jetty MBeans accesibles si `managedAttr=true` en DoS/QoSFilter. Jetty `ServerConnector` se registra como MBean `org.eclipse.jetty.server:type=serverconnector,id=0`.

### 29.15.5 Thread dump

NO hay endpoint HTTP para thread dump. Platform tool `nstat` en shell (Bloque 10) O JMX `Threading.dumpAllThreads`.

---

## 29.16 Gotchas + production incidents

### 29.16.1 Jetty worker thread ≠ engine thread

Confirmado (Bloque 9.3.2). Workaround concreto:

```java
public void doGet(WebOp op) throws Exception {
    // MAL: directo
    BComponent target = (BComponent) BOrd.make("station:|slot:/.../MyComp").resolve().get();
    target.invoke(action, value, null);   // corre en Jetty thread → NO thread-confined

    // BIEN: dispatch al engine
    ResponseFuture future = new ResponseFuture();
    Sys.getService(BEngineService.class).post(() -> {
        try {
            BComponent target = ...;
            Object result = target.invoke(action, value, null);
            future.complete(result);
        } catch (Exception e) { future.completeExceptionally(e); }
    });
    Object result = future.get(30, SECONDS);
    // Escribí response con result
}
```

Si NO haces esto → race condition intermitente (99% works, 1% crash con `IllegalStateException: Not in engine thread`).

### 29.16.2 CSRF token ausente → 403 silent

En N4.14 el default `CsrfProtectedFilter` aplicado a `/rpc`, `/na`, `/ord` POST. Si el cliente custom (curl, Postman) NO incluye `x-niagara-csrfToken` → 403 con mensaje lexicon-ized `csrf.token.missing.error`. Desde browser nunca pasa (bajaScript inyecta automáticamente). Troubleshooting: inspeccionar `niagara_essential_session_support` cookie y obtener token de `/clientEnv` response.

### 29.16.3 Session fixation si no regenera ID

Si desarrollás un scheme custom `BAuthenticationScheme` y olvidás invocar `req.changeSessionId()` post-auth → session fixation exploitable. El `NiagaraAuthenticator` lo hace para los 9 schemes built-in. **Verificar siempre** en custom schemes.

### 29.16.4 SameSite=None requires Secure

Ver 29.5.4 + 29.12.3 — causa #1 de login infinito detrás de reverse proxy con TLS termination.

### 29.16.5 Max request body size default

Jetty default `outputBufferSize=32768`, pero no hay hard cap en request body size A MENOS que `BJettySizeLimitHandler` esté enabled. Un attacker puede POSTear 1 GB → DoS memoria. **Recomendado**: enable `sizeLimitHandler` con limit 10 MB para endpoints de upload, unlimited para BOX/Fox.

### 29.16.6 Multipart upload gotcha

Niagara NO usa servlet 3.0 `@MultipartConfig` con `maxFileSize`. Para uploads (`/ord/.../fileUploadView`) el servlet parsea manualmente el stream. Si no cierras el stream o usás buffers grandes → OOM.

### 29.16.7 WS ping/pong timeout

Default Jetty 30 s. Si el reverse proxy tiene idle timeout < 30 s (e.g. nginx default 60 s OK, AWS ALB default 60 s OK, pero algunos F5 default 30 s) → WS se corta. Síntoma: user ve "reconnecting" cada 30 s en bajaScript console.

Fix: aumentar `box.ws.idleTimeout=600000` (10 min) + configurar proxy idle > 10 min.

### 29.16.8 Keepalive idle vs LB idle

Misma raíz que 29.16.7 pero para HTTP keepalive. Si Jetty keepalive > LB idle → LB cierra socket, Niagara cree conexión abierta, próximo request falla con `IOException: connection reset`. Solución: alinear Niagara `threadIdleTimeout` con LB idle.

### 29.16.9 Filter order invariants

`AuthenticationFilter` DEBE correr ANTES que `CsrfProtectedFilter`. Si invertís → session no existe aún, CSRF rechaza todo POST incluyendo `/login` → loop. Niagara lo hace bien por default — solo tenés que cuidarlo en custom extensions vía `BProfileFilterFactory`.

### 29.16.10 BNaServlet Content-Type text/plain peculiaridad

Bloque 16.5.1. `/na` devuelve `text/plain`. ¿Otros servlets igual? Confirmado: solo BNaServlet. `/rpc` usa `application/json`, `/box` usa `application/json`, `/ord` usa HTML. La rareza es solo Analytics.

### 29.16.11 Encoding de URLs con ORD

`/ord/station:|slot:/Drivers` — el `|` es válido en Jetty (RFC 3986 sub-delim). Pero `requestHeaderSize=8192` límite total. ORDs muy largos (filter BQL inline con múltiples where clauses) pegan 414. Solución: pasar la query en body POST en vez de URL.

### 29.16.12 `requireHttpsForPasswords=true` + TLS upstream

Si el balancer termina TLS y Niagara escucha HTTP: `req.isSecure()=false` → login con password rechazado con `"Password login requires HTTPS"`. Fix: 29.12.3 ForwardedRequestCustomizer + `X-Forwarded-Proto`.

### 29.16.13 Jetty 9.4.x EOL concern

Jetty 9.4 entra EOL (end of community support) en 2025. Niagara 4.14 se quedó en 9.4.54. Futuras CVEs no serán parcheadas hasta upgrade Jetty 12. Migitations: WAF upstream, IP whitelist estricto, `DoSFilter` habilitado.

### 29.16.14 WebSocket auth inheritance

El WS `/wsbox` hereda cookies del initial POST `/box`. Si user logout en otra tab → el WS recibe close `1008`. Pero algunos proxies (older AWS ELB classic) NO propagan close frames correctamente → WS queda "half-dead" server-side hasta idle timeout. Workaround: forzar client ping cada 10 s para detectar.

### 29.16.15 LogFileDirectory ORD

`BWebService.logFileDirectory` es un `BOrd` — default `file:^logging` → station `${protected.station.home}/logging/`. Si se cambia a path externo (S3 via driver) → NCSA log NO funciona (driver asincrónico, Jetty espera sincro). **Keep local**.

### 29.16.16 cacheSessionsAndRestart action

Action `cacheSessionsAndRestart` NO es idempotente: si disparás 2 veces en rápida sucesión → race con `restartTicket` — segunda invocación puede pegar NPE. Workaround: throttle UI + `@NiagaraAction` con `confirmRequired=true`.

### 29.16.17 Duplicate servletName

Si dos módulos registran `servletName="foo"` → el segundo falla con `web.duplicateServlet` en station log, pero NO impide startup. Parece funcionar — solo que el segundo NO se llega a montar. Diagnóstico: spy `/spy/stationJetty` lista servlets activos.

---

## 29.17 Mental model — request flow end-to-end

Tracemos `GET /bql/station:|slot:/Drivers|bql:select%20name,out%20from%20control:ControlPoint HTTP/1.1` desde un browser autenticado con session válida:

### 29.17.1 TLS + accept

```
1. Browser resuelve DNS → IP del Supervisor.
2. Connect TCP 443.
3. TLS 1.2/1.3 handshake:
   - ClientHello + SNI=supervisor.example.com
   - ServerHello (selecciona cert según BAdditionalHttpsCerts SNI mapping)
   - Cert chain + KeyExchange
   - Finished
4. Jetty httpsConnector (NiagaraCustomConnector, acceptor thread) accept.
```

### 29.17.2 HTTP parse + protection layers

```
5. BJettyAcceptRateLimit check — allowed (bajo maxRate).
6. BJettyConnectionLimit check — allowed.
7. Jetty HttpParser: request line + headers.
8. BJettyInetAccessHandler: IP dentro allow list → proceed.
9. BJettySizeLimitHandler: content-length 0 (GET) → proceed.
10. BNCSARequestLog: registra entry al finalizar.
```

### 29.17.3 Filter chain Niagara

```
11. BJettyDoSFilter — chequea rate per-IP (si enabled). Proceed.
12. BJettyQoSFilter — chequea concurrencia. Proceed.
13. LocaleFilter — setea UTF-8, Locale en-US desde Accept-Language.
14. ContextFilter — setea req attrs niagara.context, niagara.target (vacíos hasta ORD resolve).
15. WebStartServletFilter — no match (path != /webstart).
16. TridiumSecurityFilter — instala SecurityManager context.
17. AddSubjectFilter — pending hasta post-auth, passthrough.
```

### 29.17.4 Auth

```
18. ConstraintSecurityHandler pregunta a NiagaraAuthenticator.
19. NiagaraAuthenticator.validateRequest():
    - No "Authorization" header → session-based check.
    - req.getSession() → Jetty devuelve HttpSession existente (JSESSIONID cookie).
    - WebSessionUtil.getSession(req) → NiagaraHttpSession.
    - superId != null, valid=true → Authentication.User con BUser bob.
20. AddSubjectFilter (re-entry) — Subject.doAs(subject, () -> chain.doFilter(...)).
```

### 29.17.5 Authz

```
21. NiagaraSecurityHandler check permissions sobre /bql/* path.
    BBqlServlet.getPermissions(cx) = BPermissions(OP_READ).
    bob tiene read sobre Drivers (category match) → proceed.
```

### 29.17.6 Servlet dispatch

```
22. Jetty ServletHandler match path /bql/* → BBqlServlet (BWebServlet wrapper).
23. BWebServlet.doService() → doGet(WebOp op).
24. BBqlServlet parsea path info:
    ord = "station:|slot:/Drivers"
    bqlExpr = "select name,out from control:ControlPoint"
25. BOrd.make(ord).resolveAsync() → BDrivers component.
```

### 29.17.7 BQL execution

```
26. BqlQuery.parse(bqlExpr) → AST.
27. Crea BqlCursor.
28. post() del query al engine thread (evitar deadlock).
29. Engine thread itera Drivers tree, evalúa `control:ControlPoint` type match,
    genera rows con columns [name, out].
30. Cursor result devuelto via CompletableFuture al Jetty thread.
```

### 29.17.8 Response encoding

```
31. BBqlServlet setea Content-Type (default text/csv, o application/json si ?format=json).
32. Escribe rows al OutputStream.
33. GzipHandler wraps si Accept-Encoding: gzip → añade Content-Encoding: gzip.
34. Jetty flushes buffered 32 KB + chunked transfer encoding.
```

### 29.17.9 Post-filter

```
35. CsrfProtectedFilter — no aplica (GET method, skip).
36. UserActivityFilter — actualiza NiagaraHttpSession.lastActivity = now.
37. BNCSARequestLog entry: 192.168.1.5 - bob [23/Apr/2026:14:23:01 +0000] "GET /bql/..." 200 2048 "-" "Mozilla/5.0"
38. Connection keepalive → espera next request.
```

### 29.17.10 Total latency breakdown (estimado)

| Fase | Latencia típica |
|---|---|
| TLS handshake (primera request) | 40–100 ms |
| HTTP parse | 0.1 ms |
| Protection stack | 0.5 ms |
| Filter chain Niagara | 1–2 ms |
| Auth session lookup | 0.5 ms |
| Authz permission check | 1 ms |
| BOrd.resolve | 5–20 ms (depends on depth + cache) |
| BQL execution | 10–500 ms (depends rows) |
| Response encoding + gzip | 2 ms |
| Network write | variable |
| **Total sin TLS** | 20–525 ms |

En production un Supervisor saturado (~50 subordinados + 20 Workbench concurrentes) los auth + permission lookups pueden dominar si BUserService no cachea — ver Bloque 11.

---

## 29.18 Resumen + puntos de salida

### Qué cubre este bloque (vs Bloque 9)

- **50+ servlets** inventariados (tabla en 29.4) — Bloque 9 mencionaba solo 5-6.
- **Filter chain 15 capas** (DoS → QoS → Diag → Locale → Context → WebStart → Security → Subject → ProfileFactory → Auth → Authz → Session → ServletDispatch → CSRF → Activity) con orden exacto.
- **Jetty 9.4.54 internals** — ServerConnector, ThreadPool, 6 protection struct properties, 4 cookies Niagara, HSTS, SameSite enum.
- **Session lifecycle completo** — NiagaraHttpSession.httpSessions collection + superId + regeneration post-auth.
- **9 auth schemes** tabla comparativa por endpoint.
- **RPC JSON protocol** request/response envelopes + 6 handlers built-in + error codes.
- **BOX WebSocket** handshake + 5 system properties + frame limits.
- **16 gotchas productivos** con workarounds empíricos.
- **End-to-end request trace** con latency breakdown.

### Conecta forward

- Bloque 30 candidato (no cubierto aún): performance tuning — threadpool sizing por # subordinados + DoSFilter tuning real-world + benchmark Jetty vs otros embeds (Tomcat, Undertow).
- Bloque 31 candidato: Observability stack — integración con Prometheus/Grafana via JMX exporter, structured logging vía logback patterns, distributed tracing con OpenTelemetry (NO soportado nativo).

### Gotchas críticos a memorizar

1. Filter order: Auth ANTES CSRF. NO tocar.
2. SameSite=None needs Secure — problema #1 detrás de reverse proxy.
3. BNaServlet usa text/plain (solo ese).
4. `requireHttpsForPasswords` + TLS upstream termination = fallar silente.
5. Jetty worker thread ≠ engine thread — usar `EngineService.post()`.
6. WS idle timeout debe > LB idle timeout.
7. `showStackTrace=true` en production = leak.
8. Duplicate servletName falla silente (solo log).
9. Jetty 9.4 EOL 2025 — WAF upstream obligatorio a mediano plazo.
10. requestHeaderSize=8 KB — URLs con filtros BQL largos → 414.

---

**Fin Bloque 29.** Próximo candidato: Bloque 30 — performance tuning + benchmark empírico o Bloque 31 — observability stack.
