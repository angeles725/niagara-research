# Bloque 47 — Bootstrap headless: SPA externa 100% custom ↔ Niagara N4 station

**Fecha**: 2026-05-04
**Método**: Investigación empírica READ-ONLY. Decompilación de `web-rt.jar`, `bajaScript-ux.jar`, source JS legible en `rc/` de los módulos, bytecode JVM, `defaults/system.properties`. Sin ejecución de código.
**Fuentes primarias**: `WebSocketConnection.js` (líneas 87-113), `BrowserCommsManager.js`, `browser.js`, `bs.built.min.js` (360 KB), `BWebService.class`, `BHttpHeaderProviders.class`, `BXFrameOptionsEnum.class`, `CsrfProtectedFilter.class`, `NiagaraAuthenticator.class`, `BBoxServlet.class`, `BoxWebSocketServlet.class`, decompilados de `web-rt.jar`.
**Versión analizada**: Honeywell OptimizerSupervisor-N4.14.0.162.

---

## 47.0 Contexto, scope, qué NO es este bloque

### ¿Qué ES este bloque?

Este bloque documenta el PATRÓN GENERAL de cómo una SPA externa (Vue, React, o vanilla JS) que vive en un origen distinto (`dashboard.sejofa.com`) se conecta a una station Niagara N4 (`station.public.io:443`). Cubre las 6 decisiones técnicas críticas: bootstrap del runtime JS, CORS, auth headless, CSRF, WebSocket BOX, y hosting de assets.

### ¿Qué NO es este bloque?

- **NO es Reflow-specific**: Reflow es UN approach entre varios. Se cita como evidencia empírica, no como referencia.
- **NO cubre BajaUI (Workbench)**: ese universo es Java Swing + JxBrowser embebido, irrelevante para SPA.
- **NO cubre Fox protocol**: Fox es para comunicación inter-station y Workbench↔Station. SPA usa BOX sobre HTTP/S.
- **NO cubre BQL/NEQL en profundidad**: esos mecanismos están en Bloques 21 y 16. Acá solo se los referencia como payloads.

### Pregunta unificadora

> Mi SPA vive en `dashboard.sejofa.com`. La station Niagara está en `station.sejofa.io:443`. ¿Cómo me conecto, autentico y recibo datos en tiempo real?

La respuesta honesta: **hay un muro arquitectónico**. El runtime BajaScript (`bs.built.min.js`) asume que corre co-locado con la station (mismo origen). Cruzar ese muro requiere una de tres estrategias bien definidas. Este bloque las documenta.

---

## 47.1 Bootstrap del runtime baja sin Workbench

### 47.1.1 Qué es `bs.built.min.js`

**CONFIRMADO** (`bajaScript-ux.jar` → `rc/bs.built.min.js`, 360 KB, verificado empíricamente en Bloque 22 y presente en `bajaScript-ux.jar:rc/bs.built.min.js`).

`bs.built.min.js` es el bundle RequireJS que contiene el runtime completo de BajaScript:
- `baja` namespace: `alarm.js`, `boxcs.js`, `coll.js`, `comm.js`, `comp.js`, `ctypes.js`, `file.js`, `hist.js`, `nav.js`, `obj.js`, `ord.js`, `sys.js`, `tag.js`, `transfer.js`, `virt.js`
- `bajaScript/env/browser.js` — inicialización browser: mixin de Promises a Callbacks, `BrowserCommsManager`, `ConnectionManager`
- `bajaScript/env/WebSocketConnection.js` — gestión del WebSocket a `/wsbox`
- `bajaBrowserEnvUtil` — helpers DOM (jQuery-lite)

El archivo se sirve desde `/module/bajaScript/rc/bs.built.min.js` vía el `DefaultServlet` de Niagara (Bloque 29.11.3). En producción Niagara, la URL es:
```
https://station.sejofa.io/module/bajaScript/rc/bs.built.min.js
```

### 47.1.2 Cómo se sirve y el RequireJS config

El bootstrap estándar en una página Niagara (PX, HX, or custom) inyecta dos scripts:
```html
<script src="/requirejs/config.js"></script>
<script src="/module/js/com/tridium/js/ext/require/require.js"></script>
```

`/requirejs/config.js` lo sirve `RequireJsConfigServlet` (`com.tridium.web.servlets.RequireJsConfigServlet`). CONFIRMADO en Bloque 29.4.1. Es un JavaScript generado dinámicamente por `RequireJsUtil` (`com.tridium.web.RequireJsUtil`) que define:
- `paths`: mapeo de módulos AMD → URLs reales de la station
- `basePath`: siempre raíz de la station
- `config.baja.*`: configuración del runtime (ver 47.1.3)

**La clave**: `RequireJsConfigServlet` genera rutas RELATIVAS al host de la station. Si la SPA está en origen distinto, necesita o reescribir el config o usar el de la station vía CORS (no nativo — ver 47.2).

### 47.1.3 Inicialización mínima requerida

El flujo de bootstrap real (CONFIRMADO de `WebSocketConnection.js` + `browser.js`):

```
1. Cargar require.js (AMD loader)
2. Cargar /requirejs/config.js (mapa de módulos de la station)
3. require(["baja!"]) → carga bajaScript asíncronamente
4. baja.comm.start() [llamado internamente por browser.js]
   └─ makeServerSession() → HTTP POST /box
      └─ respuesta: { wsEndpoint: "/wsbox", sessionToken: "...", csrfToken: "..." }
5. new WebSocket("wss://" + location.host + "/wsbox")
6. Handshake BOX estándar
7. baja está listo para suscripciones
```

**CRÍTICO — HALLAZGO EMPÍRICO**: En `WebSocketConnection.js:87-113` (fuente legible, `bajaScript-ux.jar:rc/env/WebSocketConnection.js`):

```javascript
WebSocketConnection.prototype.$createSocket = function () {
    var protocol = this.isSecure() ? 'wss' : 'ws',
        uri = protocol + '://' + location.host + '/wsbox';
    try {
      return new WebSocket(uri);
    } catch (e) { ... }
};
```

**`location.host` es HARDCODED**. El WebSocket se abre SIEMPRE contra el mismo host donde está cargada la página. No hay forma de configurar un host externo sin parchear el bundle o sin que la página esté co-locada con la station.

### 47.1.4 Qué configura `niagara.env`

`niagara.env` es un objeto JavaScript inyectado en el HTML por el servidor Niagara antes de cargar la SPA. En Workbench (JxBrowser), lo inyecta el launcher Java. En HX/bajaux (browser), lo inyecta el servlet de la página.

Claves documentadas (INFERIDO de Bloque 22 + Bloque 51 evidencia `injectBaja`):
- `niagara.env.hyperlink` — URL base de la station
- `niagara.env.toHyperlink(ord)` — convierte ORD a URL HTTP
- `niagara.env.guid` — identificador de sesión Workbench (nulo en browser puro)

En modo "redirect" de Reflow (`window.onload` cuando `niagara.env == null`), la SPA llama `injectBaja()` sin ese objeto — lo que CONFIRMA que es posible un bootstrap headless sin `niagara.env`, pero requiere que el RequireJS config ya esté cargado en la página. CONFIRMADO (`app-readable.js:121489-121490`, Bloque 51.2.2).

### 47.1.5 ¿Se puede usar baja sin bs.built.min.js?

**INFERIDO — TODO honesto**: No existe evidencia de un subset mínimo oficial. `bs.built.min.js` es el bundle único. Hacerlo funcionar con menos requeriría:
- Conocer exactamente qué AMD modules son necesarios para el caso de uso específico
- Usar el source sin minificar (los `.js` individuales en `rc/baja/`)
- Asumir que cada versión de Niagara puede cambiar esas dependencias internas

Para producción, **usar `bs.built.min.js` completo (360 KB) es la única ruta respaldada empíricamente**.

---

## 47.2 CORS cross-origin

### 47.2.1 ¿Tiene Niagara soporte CORS nativo?

**CONFIRMADO como AUSENTE**: Búsqueda empírica sobre `web-rt.jar` — todas las clases en `/tmp/web-rt-extract/`:
- **NO existe** ninguna clase `CorsFilter`, `CorsHandler`, o `BCorsSetting`
- **NO existe** ningún header `Access-Control-Allow-Origin` emitido por código Niagara
- **NO existe** ninguna propiedad `niagara.cors.allowed-origins` en `defaults/system.properties`

El framework `BHttpHeaderProviders` (`javax.baja.web.http.BHttpHeaderProviders`) incluye:
- `contentSecurityPolicy` → `BCspHeaderProvider`
- `crossOriginOpenerPolicy` → `BGenericHttpHeaderProvider` (valor hardcoded `"same-origin"`)
- `xContentTypeOptions` → `BXContentTypeOptionsHeaderProvider`
- `xFrameOptions` → `BXFrameOptionsHeaderProvider` (valores: `DENY` o `SAMEORIGIN`)
- `xXssProtection` → `BXXssProtectionHeaderProvider`

**NO hay** `Access-Control-Allow-Origin` en ninguno de estos providers. Niagara N4.14 no tiene soporte CORS built-in configurable.

### 47.2.2 Qué SÍ tiene Niagara respecto a headers de seguridad

CONFIRMADO (decompilado `web-rt.jar`):

| Header | Implementación | Configurable? |
|--------|---------------|---------------|
| `X-Frame-Options` | `BXFrameOptionsEnum` — valores `DENY` o `SAMEORIGIN` | SÍ, via BOG |
| `Cross-Origin-Opener-Policy` | Hardcoded `same-origin` en `BGenericHttpHeaderProvider` | Parcialmente |
| `Content-Security-Policy` | `BCspHeaderProvider` | SÍ, via BOG |
| `X-Content-Type-Options` | `BXContentTypeOptionsHeaderProvider` | SÍ, via BOG |
| `X-XSS-Protection` | `BXXssProtectionHeaderProvider` | SÍ, via BOG |
| `Strict-Transport-Security` | Emitido si `httpsOnly=true` | Implícito |
| `Access-Control-Allow-Origin` | **AUSENTE** | **NO** |

El `X-Frame-Options: SAMEORIGIN` es especialmente crítico para el approach iframe (ver 47.6.3).

### 47.2.3 Implicaciones concretas para SPA cross-origin

Si la SPA está en `dashboard.sejofa.com` y la station en `station.sejofa.io`:

**Problema 1 — `/requirejs/config.js`**: La SPA no puede cargar el config RequireJS de la station. `fetch("https://station.sejofa.io/requirejs/config.js")` falla con CORS error porque la station no emite `Access-Control-Allow-Origin`.

**Problema 2 — JSESSIONID cookie**: Cookies de sesión Niagara tienen `SameSite=Lax` por defecto (Bloque 29.5.3). Con origenes distintos, el browser NO envía el cookie en requests cross-origin. SCRAM handshake puede funcionar (auth headers), pero la cookie de sesión resultante no es enviable cross-origin sin `SameSite=None; Secure`.

**Problema 3 — WebSocket `/wsbox`**: `WebSocketConnection.js` usa `location.host` hardcoded. Aunque se configura `Access-Control-Allow-Origin` vía nginx, el código BajaScript abrirá el WS contra el host ACTUAL de la página, no contra la station.

**Problema 4 — CSP `frame-ancestors`**: Si la station tiene CSP con `frame-ancestors 'self'` o `X-Frame-Options: SAMEORIGIN`, no puede ser embebida en iframe desde `dashboard.sejofa.com`.

### 47.2.4 Workaround A — Reverse proxy co-locado (RECOMENDADO)

```
dashboard.sejofa.com/
├── / → SPA estática (S3, CDN, nginx)
└── /niagara/ → proxy_pass https://station.sejofa.io/
```

Con nginx:
```nginx
server {
    server_name dashboard.sejofa.com;
    
    location / {
        root /srv/spa;
        try_files $uri $uri/ /index.html;
    }
    
    location /niagara/ {
        proxy_pass https://station.sejofa.io/;
        proxy_set_header Host station.sejofa.io;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto https;
        
        # WebSocket upgrade
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # Idle timeout mayor al BOX keepalive
        proxy_read_timeout 600s;
        proxy_send_timeout 600s;
    }
}
```

La SPA carga `/niagara/requirejs/config.js` y `/niagara/module/bajaScript/rc/bs.built.min.js` — mismo origen, sin CORS. El WebSocket se abre a `wss://dashboard.sejofa.com/niagara/wsbox`. La station ve el proxy como el cliente; cookies funcionan con `SameSite=Lax` porque son same-origin para el browser.

**TRADEOFF**: Requiere nginx con TLS pass-through o re-emisión de cert. `X-Forwarded-Proto: https` obliga a configurar Niagara con `ForwardedRequestCustomizer` (no disponible por defecto — ver Bloque 29.12.2). Workaround: Niagara en HTTPS en el connector local + proxy sin re-emisión TLS (`proxy_ssl_verify off` para cert Niagara self-signed).

**GOTCHA G47-1**: Niagara emite cookies con el flag `Secure` basado en `req.isSecure()`. Si el proxy hace TLS termination y Niagara escucha HTTP interno, `req.isSecure()=false` → cookies sin `Secure` → browsers modernos rechazan. Fix: usar `proxy_pass https://` al conector HTTPS de Niagara, aunque sea con cert self-signed (`proxy_ssl_verify off`).

### 47.2.5 Workaround B — Custom CORS filter (requiere módulo Niagara)

Desarrollar un módulo Niagara con un `BProfileFilterFactory` que inyecte un filtro CORS:

```java
@NiagaraType
public class BCorsFilterFactory extends BProfileFilterFactory {
    @Override
    public void configureFilters(FilterHolder[] filters, ...) {
        // Inyectar un javax.servlet.Filter que emita Access-Control-*
        FilterHolder cors = new FilterHolder(new CorsFilter("dashboard.sejofa.com"));
        // ... registrar con Jetty
    }
}
```

Luego montar este componente bajo `/Services/WebService/WebServer`. INFERIDO — no existe empíricamente en ningún módulo de la distribución Honeywell. Es técnicamente posible pero requiere un módulo firmado (Bloque 18).

**TRADEOFF**: Requiere build + firma + deploy del módulo. El módulo debe declarar la permission `NETWORK_COMMUNICATION` o equivalente. Cada actualización de Niagara puede romper el API del `BProfileFilterFactory` (semi-privado).

### 47.2.6 WebSocket cross-origin: `Origin` header

Para WebSocket cross-origin, el browser envía `Origin: https://dashboard.sejofa.com` en el handshake. Niagara (`BoxWebSocketServlet`) NO valida el `Origin` header empíricamente — no se encontró ningún `getHeader("Origin")` check en el decompilado de `box-rt.jar`. INFERIDO: el servidor confía en la autenticación de session (JSESSIONID cookie), no en el `Origin`. Con el reverse proxy approach (Workaround A), este problema no existe.

---

## 47.3 Auth headless desde SPA externa

### 47.3.1 Los 9 schemes y su compatibilidad headless

(Basado en Bloques 29.6, 18.5-18.6, 30)

| Scheme | Headless JS? | Notas |
|--------|-------------|-------|
| **SCRAM-SHA256 via HELLO** | SÍ | Flow 6-step via `Authorization: HELLO` headers. Completamente manejable desde fetch/XHR. El único que la documentación Tridium soporta para M2M. |
| HTTP Basic | SÍ (pero inseguro) | `Authorization: Basic base64(user:pass)`. Contraseña viaja sobre TLS en cada request. NUNCA usar en producción. |
| HTTP Digest (MD5) | SÍ (deprecado) | RFC 2617 two-step. Deprecado en N4, no recomendado. |
| Header SSO (`X-Remote-User`) | SÍ (si proxy confiable) | El proxy hace la auth y pone el header. Niagara requiere CIDR whitelist del proxy. Exige `BHeaderAuthenticationScheme` instalado. Bloque 27 confirmó que **NO está incluido por defecto** — requiere módulo. |
| SAML 2.0 | NO headless JS directo | Requiere redirect browser a IdP. Posible solo con iframe hidden + postMessage hack. Extremadamente complejo. |
| Kerberos SPNEGO | NO en browser externo | Funciona solo si el browser tiene ticket Kerberos y el domain está configurado (`krb5.conf`). Impráctico para SPA externa. |
| LDAP Bind | NO directo | Niagara hace el bind internamente. La SPA aún necesita hacer SCRAM o Basic contra el `/login` endpoint. LDAP es el backend, no el protocolo frontend. |
| Google TOTP | NO headless | Requiere segundo factor interactivo. |
| mTLS client cert | SÍ si cert en browser | El browser puede presentar cert cliente en TLS handshake. Requiere cert distribution a todos los usuarios. |

**Conclusión**: Para SPA headless, **SCRAM-SHA256** es el único approach robusto nativamente soportado.

### 47.3.2 Flow SCRAM-SHA256 completo desde JavaScript

(Basado en Bloque 18.6.1 — CONFIRMADO empíricamente)

```javascript
// Paso 1: HELLO — anunciar usuario
const helloResp = await fetch('https://station.sejofa.io/login', {
    method: 'GET',
    headers: {
        'Authorization': 'HELLO username=' + btoa(username)
    },
    credentials: 'include'  // para que cookie JSESSIONID sea enviada
});
// Respuesta: 401 con WWW-Authenticate: SCRAM hash=SHA-256,handshakeToken=abc123

// Paso 2-5: SCRAM dance (requiere crypto.subtle o librería)
const serverChallenge = parseWwwAuthenticate(helloResp.headers.get('WWW-Authenticate'));
const handshakeToken = serverChallenge.handshakeToken;
const clientFirstMsg = buildClientFirstMessage(username);

const scramResp = await fetch('https://station.sejofa.io/login', {
    method: 'GET',
    headers: {
        'Authorization': `SCRAM handshakeToken=${handshakeToken},data=${btoa(clientFirstMsg)}`
    },
    credentials: 'include'
});
// Respuesta: 401 con server first message

// ... (pasos 4-5) ...

// Paso 6: Servidor retorna 200 con Authentication-Info que incluye authToken (BEARER)
// Cookie JSESSIONID es seteada automáticamente si credentials: 'include'
```

**GOTCHA G47-2**: La librería SCRAM-SHA-256 para browser es CUSTOM — no existe npm package que haga el SCRAM exactamente como Niagara lo espera (ver Bloque 18.6 para el protocolo exacto). Hay que implementar:
1. `PBKDF2WithHmacSHA256` para derivar `saltedPassword`
2. `HMAC-SHA-256` para `clientKey` y `serverKey`  
3. SHA-256 para `storedKey`
4. `crypto.subtle` API (disponible en browsers modernos bajo HTTPS)

La implementación en puro `crypto.subtle` es ~80 líneas. No es trivial pero es viable.

**GOTCHA G47-3**: El endpoint de auth headless es `/login` con `Authorization: HELLO` header — NO un endpoint REST dedicado. El servidor distingue entre request de browser (sin header HELLO → sirve HTML form) y request de API (con header HELLO → hace challenge-response). El selector es el header `Authorization`.

### 47.3.3 Alternativa: Header SSO con reverse proxy

Si el reverse proxy ya autentica al usuario (e.g. con OAuth2 Proxy, Cloudflare Access, o autenticación corporativa), puede inyectar un header confiable:

```nginx
# En nginx, después de auth_request:
location /niagara/ {
    auth_request /auth-endpoint;
    auth_request_set $auth_user $upstream_http_x_auth_user;
    proxy_set_header X-Remote-User $auth_user;
    proxy_pass https://station.sejofa.io/;
}
```

Niagara (`BHeaderAuthenticationScheme`) mapea `X-Remote-User` → `BUser`. PERO: en la distribución Honeywell analizada, Bloque 27 documentó que `BHeaderAuthenticationScheme` **no viene instalado por defecto**. Se necesita instalar el módulo correspondiente. Y requiere CIDR whitelist del proxy.

### 47.3.4 Token-based auth: ¿Bearer tokens?

**CONFIRMADO** (Bloque 18.6.1): Niagara SÍ emite `authToken` vía el paso 6 del SCRAM handshake, enviado como `Authentication-Info: authToken=SESSION_ID`. Este `authToken` puede usarse en subsequent requests como:
```
Authorization: BEARER authToken=SESSION_ID
```

**IMPORTANTE**: Este `authToken` es el `JSESSIONID` de Jetty — NO es un JWT ni un token de larga duración. Tiene el mismo lifetime que la sesión web (15 min idle timeout default, Bloque 29.5.5). No hay refresh automático — si expira, hay que re-autenticar.

No existe un "Bearer token" OAuth2 separado. `oauth2-rt` es un cliente M2M para conectar NIAGARA A otros sistemas, NO para que sistemas externos usen Niagara como resource server (CONFIRMADO Bloque 30).

### 47.3.5 Session lifetime y logout headless

**Lifetime**: Idle timeout 15 min default (`BAutoLogoffSettings`). No hay cap absoluto de vida configurado por defecto. Re-enviar requests antes de 15 min = renovación automática (la sesión Jetty hace reset del timer en cada request).

**Logout headless**: `GET /logout` (no requiere body). Invalida la sesión + borra JSESSIONID cookie. Si se gestiona via `credentials: 'include'`, el browser descarta la cookie automáticamente.

**NSuperSession**: Bloque 30.13 documenta que Niagara agrega HTTP + Fox + BOX bajo un mismo `BUser` vía `NSuperSession`. El invariant "1 concurrent session per user" aplica — si la SPA inicia sesión nueva, ¿invalida la anterior? INFERIDO: NO por defecto (Bloque 29.5.8 dice que no hay límite configurable per user). El `NSuperSession` agrega bajo el mismo usuario, no reemplaza.

---

## 47.4 CSRF token lifecycle desde SPA

### 47.4.1 Cómo obtener el primer token

**CORRIGENDUM 2026-05-06 — ver Bloque 52 para el patrón completo**: La afirmación original de esta sección ("se extrae del HTML del `/login` post-SCRAM") **es válida solo en el flujo de first-login** (usuario aún no autenticado, server muestra el form). Post-autenticación, `GET /login` retorna **302 redirect con body vacío** y el token NO es extraíble por ese path. Para SPAs embebidas en iframe del Workbench shell (caso típico de módulos custom), el token se obtiene del DOM del frame ancestor: `window.top.document.querySelector('input#csrfToken').value`. El patrón canónico es **Plan E (ancestor frame DOM) primario, Plan A (`/login` fetch) fallback**. Documentado completo en Bloque 52.

**CONFIRMADO** (Bloque 18.5.1 + `CsrfProtectedFilter.class`):

El token CSRF se crea al establecer la sesión HTTP. Niagara lo inyecta como `<input id="csrfToken" value="...">` en CADA HTML que renderiza (no solo en `/login`). Hay tres formas de obtenerlo desde la SPA, en orden de preferencia:

```javascript
// Plan E (PRIMARIO, post-auth) — leer del DOM ancestor del iframe
function readFromAncestor(ancestor) {
  try {
    var input = ancestor.document.querySelector('input#csrfToken, input[name="csrfToken"]');
    return input && input.value ? input.value : null;
  } catch (err) { return null; /* cross-origin block */ }
}
var token = readFromAncestor(window.parent) || readFromAncestor(window.top);

// Plan A (FALLBACK — solo first-login o non-iframe contexts)
const loginResp = await fetch('/login', { credentials: 'include' });
const html = await loginResp.text();
const csrfToken = html.match(/id=["']csrfToken["'][^>]*value=["']([^"']+)["']/)?.[1];
// HALLAZGO EMPÍRICO 2026-05-06: post-auth, /login redirige 302, body vacío, regex retorna undefined
```

Alternativa NO CONFIRMADA: el endpoint `/clientEnv` (`BClientEnvServlet`, Bloque 29.4.1) podría retornar el token en su JSON response — INFERIDO, no confirmado empíricamente que incluya el CSRF token. TODO-52-2 en Bloque 52.

### 47.4.2 ¿Se rota el token?

**INFERIDO**: El token es **session-scoped** — una por sesión HTTP, no por request. Vive junto al `JSESSIONID`. Si la sesión expira y se re-autentica, el nuevo `JSESSIONID` tiene un nuevo token CSRF.

No hay evidencia de rotación per-request (Double Submit Cookie pattern). Niagara usa el server-side storage pattern: token guardado en `NiagaraWebSession` attribute, comparado contra el header.

### 47.4.3 Qué requests requieren CSRF

**CONFIRMADO** (`CsrfProtectedFilter.class`): Los métodos `POST`, `PUT`, `DELETE`, `PATCH` requieren CSRF. GET es EXENTO.

**GOTCHA G47-4 — GETs destructivos en módulos custom**: Como CONFIRMADO en Bloque 51.3.3 (análisis de Reflow), algunos módulos diseñan endpoints destructivos como GETs (e.g. `/nmodsreflow/station/backups/destroy`). Estos GETs NO están protegidos por `CsrfProtectedFilter`. Niagara no fuerza CSRF en GETs. Es una decisión de diseño del módulo.

**Paths exentos hardcoded** (CONFIRMADO `CsrfProtectedFilter.class`, Bloque 29.3.3): `/logout`, `/logoutConfirm`.

### 47.4.4 Cómo enviar el token desde SPA

```javascript
// Interceptor axios (recomendado)
axios.interceptors.request.use(config => {
    if (['post', 'put', 'delete', 'patch'].includes(config.method)) {
        config.headers['x-niagara-csrfToken'] = csrfTokenStore.get();
    }
    return config;
});

// O en fetch manual:
const response = await fetch(url, {
    method: 'POST',
    headers: {
        'x-niagara-csrfToken': csrfToken,
        'Content-Type': 'application/json'
    },
    body: JSON.stringify(data),
    credentials: 'include'
});
```

El header se llama `x-niagara-csrfToken` (CONFIRMADO en bytecode `CsrfProtectedFilter.class`, Bloque 29.3.3 + 18.5.1).

### 47.4.5 Recuperación si el token vence

Flujo si el CSRF falla (sesión expirada → nuevo `JSESSIONID` → token viejo inválido):

1. Server retorna 403 con body lexicon-ized (`csrf.token.missing.error` o `csrf.token.invalid.error`)
2. SPA detecta 403 en el interceptor
3. SPA llama a un `refreshAuth()`: re-hace SCRAM handshake → obtiene nuevo JSESSIONID + nuevo csrfToken
4. SPA reintenta el request original con el nuevo token

```javascript
// Interceptor de respuesta
axios.interceptors.response.use(
    res => res,
    async err => {
        if (err.response?.status === 403 && err.response?.data?.includes('csrf')) {
            const newToken = await refreshAuth();
            err.config.headers['x-niagara-csrfToken'] = newToken;
            return axios.request(err.config);
        }
        return Promise.reject(err);
    }
);
```

---

## 47.5 Connection lifecycle: WebSocket BOX desde SPA externa

### 47.5.1 Handshake completo

(CONFIRMADO Bloques 22.12, 29.10, 36.6)

```
Paso 1: POST /box
        Body: {"version":"1.0","features":[...]}
        Headers: Cookie: JSESSIONID=..., x-niagara-csrfToken: ...
        
        Response: 200 OK
        Body: {"wsEndpoint":"/wsbox","sessionToken":"..","csrfToken":".."}
        
Paso 2: GET /wsbox (HTTP Upgrade)
        Headers: Upgrade: websocket
                 Sec-WebSocket-Version: 13
                 Sec-WebSocket-Key: <random16b>
                 Cookie: JSESSIONID=...
                 Origin: https://[same host]
                 
        Response: 101 Switching Protocols
                  Sec-WebSocket-Accept: ...

Paso 3: BOX messages (JSON sobre text frames)
        {"p":"box","v":2,"n":1,"m":[{"r":1,"t":"rt","c":"sys","k":"getTypes","b":{}}]}
```

**CRÍTICO para SPA externa**: El `/box` POST requiere CSRF token. El `/wsbox` upgrade NO requiere CSRF (validado en el POST — ver Bloque 29.10.4). La validación de session es vía cookie en ambos.

### 47.5.2 El problema de `location.host` hardcoded

Ya documentado en 47.1.3. El código fuente CONFIRMADO:

```javascript
// WebSocketConnection.js:87-113 (bajaScript-ux.jar:rc/env/WebSocketConnection.js)
var uri = protocol + '://' + location.host + '/wsbox';
return new WebSocket(uri);
```

**Implicación directa**: Para que el BajaScript runtime conecte al `wsbox` de la station desde una SPA en otro origen, la página HTML donde corre BajaScript DEBE estar servida desde el mismo host que la station (o desde un reverse proxy que exponga la station bajo el mismo origen).

No hay forma de configurar un host alternativo sin parchear el bundle o sin uno de los approaches de 47.6.

### 47.5.3 Subprotocols, frame format, fragmentation

**Frame format** (CONFIRMADO `BoxFrame.js`, Bloque 36.6):
```json
{
  "p": "box",      // protocol — siempre "box"
  "d": null,       // destination — null = station local
  "v": 2,          // version — 2 en N4.14
  "n": 3,          // frame ID incremental
  "m": [...]       // array de BoxMessages
}
```

**Fragmentation**: Frames >64 KB usan WebSocket continuation frames (Jetty automático). Config: `box.ws.maxTextMessageSize=65536` (default, configurable en `system.properties`).

**Encoding**: JSON sobre text frames por defecto. BSON-lite sobre binary frames como alternativa (config: `box.ws.maxBinaryMessageSize`).

**Batching implicit** (desde N4.10): múltiples messages en `m[]` se emiten juntos con un debounce ~10ms (CONFIRMADO Bloque 36.6, `Batch.js`).

### 47.5.4 Heartbeat, lease 10s hardcoded, y load balancer conflict

**GOTCHA G47-5 — Lease 10s hardcoded**: El lease default de `baja.Component.lease()` es **10 segundos hardcoded** en `Component.js:1795`:
```javascript
time = bajaDef(obj.time, 10000)  // 10000 ms = 10 segundos
```
(CONFIRMADO Bloque 36.5). El cliente DEBE renovar el lease antes de que expire. Si el WebSocket se corta o el JavaScript se pausa (tab en background), el servidor hace unsubscribe automático al vencer el lease.

**Workaround lease**: La API de lease puede recibir un `time` custom:
```javascript
baja.Component.lease({ comps: [comp], time: 60000 })  // 60 segundos
```
Sin embargo, hay un límite máximo server-side no documentado. INFERIDO: configurable vía `LeaseManager` (Bloque 20), pero no expuesto en propiedades BOG estándar.

**GOTCHA G47-6 — LB idle vs WS keepalive**: Bloque 29.16.7 documenta que el default ping/pong de Jetty WebSocket es 30s. Si el load balancer (AWS ALB, nginx) tiene idle timeout < 30s, el WS se corta. Fix: `box.ws.idleTimeout=600000` en `system.properties` + configurar LB idle > 10 min.

**GOTCHA G47-7 — Reconnect manual obligatorio**: El BajaScript runtime NO tiene reconnect automático out-of-the-box (CONFIRMADO en `ConnectionManager.js` — sólo detecta cierre, no reintenta). El código de Reflow tampoco lo implementa de forma robusta (Bloque 51.2.4: "NO hay reconexión automática"). La SPA debe implementar su propio reconnect loop:

```javascript
function connectWithRetry(delay = 1000, maxDelay = 30000) {
    baja.comm.start().catch(() => {
        setTimeout(() => connectWithRetry(Math.min(delay * 2, maxDelay)), delay);
    });
}
```

Tras reconectar, **las subscripciones activas NO se re-registran automáticamente** (CONFIRMADO Bloque 36.5). La SPA debe re-invocar `lease()` en el evento `reconnected`.

### 47.5.5 Límite concurrent sessions per user

**CONFIRMADO** Bloque 30.13: `NSuperSession` agrega HTTP + Fox + BOX bajo un mismo usuario. El invariant es 1 concurrent session per user — pero empíricamente Bloque 29.5.8 dice que no hay límite built-in. ACLARACIÓN: el límite de 1 sessión es para la sesión Niagara como concepto lógico (NSuperSession), no el número de WebSockets. Un mismo usuario puede tener múltiples tabs/browsers activos con WebSocket BOX simultáneamente — todos usan el mismo NiagaraHttpSession vía `superId`.

### 47.5.6 BOX channels disponibles para SPA externa

(Inventario CONFIRMADO de Bloque 36.6, basado en `box-rt.jar`)

| Channel | Clave `c` | Operaciones disponibles | Uso para SPA |
|---------|-----------|------------------------|--------------|
| `sys` | `sys` | `getTypes`, `resolve`, `clock`, `registry` | Resolver ORDs, obtener tipos |
| `boxcs` | `boxcs` | `addKnob`, `removeKnob`, `sync` (unsolicited) | **Principal** — suscripción a componentes |
| `ord` | `ord` | `resolve`, `get`, `put`, `delete` | CRUD sobre ORDs |
| `hist` | `hist` | `query`, `stream` | Datos históricos |
| `alarm` | `alarm` | Live alarm console | Alarmas en tiempo real |
| `nav` | `nav` | Nav tree lazy-loading | Navegación árbol |
| `foxbox` | `foxbox` | Tunnel Fox over BOX | Acceso a subordinados |

La invocación de actions y serverSideCalls sigue el formato:
```json
{"c":"sys","k":"invoke","b":{"handle":"hXX","slot":"actionName","arg":{...}}}
```

---

## 47.6 Recursos estáticos: station vs externo vs hybrid

### 47.6.1 Approach A — SPA dentro del módulo Niagara (same-origin)

**Patrón**: El bundle Vite/webpack de la SPA se incluye en el JAR del módulo `-rt` bajo `rc/`. Niagara lo sirve vía `DefaultServlet` (o `FileServlet`) en `/module/<modName>/rc/`.

**Evidencia empírica**: CONFIRMADO en Reflow (Bloque 51.1.3):
```kotlin
// nmodsreflow-rt.gradle.kts:64-80
tasks.named<Jar>("jar") {
    from("src/rc") { include("**/*"); into("rc") }
}
```
El HTML de la SPA, los JS bundles, CSS, y assets viven en el JAR. El servlet sirve `/nmodsreflow/` (context path en `jetty-web.xml`).

**Ventajas**:
- Same-origin completo: sin CORS, sin problemas de cookies, `location.host` correcto
- `baja.comm.start()` funciona out-of-the-box
- CSRF manejado automáticamente (same-origin)
- RequireJS config accesible sin configuración adicional

**Desventajas**:
- Cada deploy de UI requiere rebuild del módulo Niagara + reinstall vía Platform Daemon (5011)
- Módulo requiere firma (si `moduleVerificationMode` no es `low`) o `skipModuleValidation`
- Ciclo de deploy más lento (build Gradle → sign → upload → install)
- Múltiples stations = mismo bundle a todas → upgrade coordinado

**Mejor para**: módulos de producto (como Reflow) donde el bundle es estable y los deploys son infrecuentes.

### 47.6.2 Approach B — SPA externa (CDN/S3/nginx)

**Patrón**: El bundle vive en un servidor estático separado. La SPA hace fetch a la station via CORS o reverse proxy.

**Ventajas**:
- Deploy UI independiente de Niagara (CI/CD rápido, sin Gradle/signing)
- CDN edge caching para assets
- Múltiples stations → mismo bundle para todas, sin sincronización

**Desventajas**:
- CORS es un muro total (ver 47.2.1 — NO existe CORS nativo en Niagara)
- `WebSocketConnection.js` hardcodea `location.host` — el BajaScript runtime NO conecta a station remota
- Cookies cross-origin requieren `SameSite=None; Secure` — configurable en Niagara pero frágil
- **CONCLUSIÓN**: **Approach B puro es INVIABLE sin reverse proxy** dado el hardcode de `location.host`

**Si se usa reverse proxy** (convertido en Approach A+B hybrid): ver 47.2.4. El reverse proxy hace que desde el punto de vista del browser, todo es mismo origen.

### 47.6.3 Approach C — Reflow hybrid: iframe injection desde módulo trivial

**Patrón** (CONFIRMADO empíricamente en Bloque 50):
1. Un módulo Niagara trivial (`-ux.jar`) instala un `BIJavaScript` loader o un BWidget que inyecta un `<iframe>` apuntando a la SPA externa.
2. La SPA externa carga desde CDN/S3/nginx.
3. Dentro del iframe (mismo origen de la station, si el iframe apunta a `/module/mySpa/rc/index.html` dentro de la station), el BajaScript puede correr.
4. La comunicación entre la SPA en el iframe y el frame padre se hace via `window.postMessage`.

**Topología Reflow** (CONFIRMADO Bloque 50):
```
Browser principal (HX)
  └─ BReflow widget (-ux) → monta <iframe> → src="/nmodsreflow/"
       └─ SPA Vue dentro del iframe (mismo origen: station.sejofa.io)
            ├─ window.injectBaja() → require(["baja!"]) → BOX /wsbox
            └─ HTTP REST → /nmodsreflow/*
```

El iframe apunta a `/nmodsreflow/` — un path dentro del servidor Niagara. Same-origin. BajaScript funciona.

**Para la variante "CDN con iframe"**: El iframe `src` puede apuntar a `https://dashboard.sejofa.com/app.html`, pero entonces ese HTML cargado en el iframe es cross-origin con la station → BajaScript dentro del iframe NO puede conectar a la station (origin mismatch). El approach de Reflow FUNCIONA porque el iframe apunta a un path dentro de la station.

**GOTCHA G47-8 — X-Frame-Options SAMEORIGIN**: Si la station tiene `X-Frame-Options: SAMEORIGIN` configurado (Bloque 27 — configurable en `BXFrameOptionsEnum`), un iframe desde `dashboard.sejofa.com` es RECHAZADO por el browser. Para el approach Reflow que monta el iframe desde la propia station (HX frame que ya es same-origin), no hay problema. Para el approach "SPA externa que embebe iframe de la station", sí hay problema.

**Ventajas approach C**:
- Deploy de la SPA independiente de Niagara (si el index.html que contiene el iframe es estático)
- El módulo Niagara instalar es mínimo (solo el loader del iframe, ~3 clases Java)
- BajaScript corre same-origin dentro del iframe

**Desventajas**:
- El bundle JS (incluido `bs.built.min.js`) DEBE servirse desde la station o via reverse proxy
- Comunicación entre iframe y página padre requiere `window.postMessage` (extra complejidad)
- El iframe HTML (`index.html`) que el browser carga DEBE estar en el origen de la station para que BajaScript funcione

### 47.6.4 Decision matrix para SEJOFA

Consideraciones del contexto SEJOFA (múltiples stations, Supervisor central):

| Criterio | Approach A (in-module) | Approach B (CDN puro) | Approach C (iframe hybrid) |
|----------|----------------------|----------------------|--------------------------|
| Deploy UI independiente | NO | SÍ | PARCIAL |
| Requiere Gradle + sign | SÍ | NO | Solo módulo loader |
| Same-origin BajaScript | SÍ | **NO VIABLE** | SÍ (iframe) |
| Múltiples stations | Un módulo por station (idéntico) | SÍ (un CDN) | SÍ (módulo loader idéntico) |
| CORS setup | NO necesario | BLOQUEANTE | NO necesario |
| Reverse proxy obligatorio | NO | SÍ | NO |
| Complejidad operacional | BAJA | MEDIA-ALTA | MEDIA |

**Recomendación para SEJOFA**: Si el deploy de UI necesita ser ágil (múltiples deploys por semana), usar **Approach A** con el bundle dentro del módulo + pipeline CI/CD que automatice el build Gradle + install via API Platform (5011). Si el equipo NO quiere tocar Niagara, usar **Approach A + reverse proxy** (nginx frente a la station, SPA servida por nginx, proxy transparente a Niagara bajo `/niagara/`).

---

## 47.7 Tabla resumen: tres approaches deployment + tradeoffs

| # | Approach | Bundle vive en | BajaScript funciona | CORS | Deploy UI | Complejidad |
|---|---------|--------------|-------------------|------|-----------|-------------|
| A | In-module | JAR Niagara `-rt` | SÍ (same-origin) | NO necesario | Rebuild módulo + install | BAJA |
| A' | In-module + reverse proxy | JAR Niagara `-rt` | SÍ | NO necesario | Rebuild módulo + install | MEDIA |
| B | CDN/S3 puro | CDN externo | **NO** (`location.host` hardcode) | BLOQUEANTE | Push a CDN | ALTA (inviable) |
| B' | CDN + reverse proxy | CDN externo + nginx | SÍ (via proxy) | Resuelto por proxy | Push a CDN | MEDIA-ALTA |
| C | Iframe hybrid | CDN (el loader en módulo Niagara, bundle en CDN) | SÍ (iframe same-origin) | NO necesario | Push a CDN + módulo loader | MEDIA |
| C' | Iframe full-station | JAR `-rt` | SÍ | NO necesario | Rebuild módulo | BAJA-MEDIA |

**Los approaches viables en producción**: A, A', B', C, C'.
**Approach B puro**: INVIABLE sin cambios al BajaScript runtime.

---

## 47.8 Antipatterns típicos detectados

**G47-1 ANTIPATTERN — TLS termination sin ForwardedRequestCustomizer**:
Reverse proxy termina TLS, Niagara en HTTP → `req.isSecure()=false` → cookies sin `Secure` → `SameSite=None` no funciona → loop de login infinito. Fix: documentado en Bloque 29.12.3.

**G47-2 ANTIPATTERN — Exponer la station directamente en internet sin hardening**:
Puerto 443 abierto + no HSTS + no CSP + `showStackTrace=true` = superficie de ataque muy grande. Niagara N4.14 usa Jetty 9.4.54 EOL (Bloque 29.16.13). Usar siempre reverse proxy con WAF.

**G47-3 ANTIPATTERN — Usar SCRAM sin `credentials: 'include'`**:
`fetch()` sin `credentials: 'include'` NO envía ni recibe cookies. La sesión se establece pero el browser descarta el `JSESSIONID`. El siguiente request falla como no autenticado.

**G47-4 ANTIPATTERN — Guardar csrfToken en localStorage**:
`localStorage` es accesible desde JavaScript en cualquier página del mismo origen. Si hay XSS, el atacante puede leer el token. Almacenarlo en memoria de módulo JS (variable de closure) es más seguro.

**G47-5 ANTIPATTERN — No implementar reconnect para BOX WebSocket**:
El BajaScript runtime no tiene reconnect automático. Sin it, cualquier blip de red deja el dashboard "congelado" sin datos. El usuario debe refrescar.

**G47-6 ANTIPATTERN — No renovar lease → suscripciones silenciosamente perdidas**:
Lease default = 10 segundos. Si el tab está en background más de 10s sin renovación, las suscripciones se pierden. El dashboard muestra datos desactualizados sin error visible. Implementar un heartbeat que llame `lease()` periódicamente.

**G47-7 ANTIPATTERN — Widget destroy sin `sub.detach()`**:
Si un widget Vue/React destruye un componente sin llamar `baja.Subscriber.detach()` (Bloque 36.5), la suscripción permanece activa en la station. Con SPA navegando entre views, esto acumula suscripciones "fantasma" que consumen recursos del servidor.

**G47-8 ANTIPATTERN — Copiar `bs.built.min.js` a CDN externo**:
Aunque se puede hostear el bundle externamente, `WebSocketConnection.js` dentro de él usará `location.host` del CDN, no de la station. Las llamadas WS irán al CDN → fallo. El bundle DEBE correr en un contexto donde `location.host` sea la station o el proxy de la station.

**G47-9 ANTIPATTERN — No configurar `box.ws.idleTimeout`**:
Default `box.ws.idleTimeout=300000` (5 min, Bloque 29.10.3). Con un reverse proxy con idle timeout < 5 min, el WebSocket se corta antes del timeout Niagara, pero Niagara no lo sabe. Alinear: `box.ws.idleTimeout > LB idle timeout`.

**G47-10 ANTIPATTERN — No manejar `BOnMissingType` en subscripciones**:
Si la station tiene tipos custom (módulos propios) y el browser intenta suscribirse a un componente de eso tipo, BajaScript puede fallar en el decode si `ctypes.js` no tiene la definición. Solución: pre-cargar los tipos vía `require(["baja!", "baja!<typeSpec>"])` antes de suscribirse (patrón de Reflow en `app-readable.js:121341`, Bloque 51.2.2).

---

## 47.9 Refinamiento Bloques 42-49 post-47

### Impacto en Bloque 42 (Subscriber lifecycle)

**Hallazgo de 47 que afecta 42**:
- Lease 10s hardcoded — Bloque 42 debe documentar la estrategia de renovación
- Reconexión NO automática — Bloque 42 debe incluir reconnect pattern con exponential backoff
- `sub.detach()` obligatorio en destroy — Bloque 42 debe documentar cleanup en el lifecycle de componente SPA
- `me.resolveBatched()` con debounce 100ms — Bloque 42 puede referir el patrón de Reflow como implementación de referencia

**Prioridad Bloque 42**: ALTA. Es el contrato operacional del pipeline real.

### Impacto en Bloque 44 (NiagaraRPC / REST API)

**Hallazgo de 47 que afecta 44**:
- CSRF requerido en POST/PUT/DELETE — Bloque 44 debe incluir el interceptor de axios/fetch
- Bearer token via SCRAM (authToken) — Bloque 44 debe documentar su uso y expiración
- `requestHeaderSize=8192` (Bloque 29.1.3) — ORDs largos en headers fallan. Usar body
- `Content-Type: text/plain` en `/na` (Bloque 29.16.10) — no asumir JSON en todos los endpoints

**Prioridad Bloque 44**: ALTA. REST es el canal principal para queries paginadas.

### Impacto en Bloque 45 (History / Analytics REST)

**Hallazgo de 47 que afecta 45**:
- `BNaServlet` (`/na/*`) requiere rol `NA_API` — la SPA necesita un usuario con ese rol
- `BHistoryChannel` sobre BOX disponible para streaming — alternativa al REST paginado
- `HistoryData.jsonForHistory()` shape confirmada en Bloque 51.3.1 — Bloque 45 puede usarla como contrato

**Prioridad Bloque 45**: MEDIA. Depende de si el dashboard necesita históricos en tiempo real o solo paginados.

### Impacto en Bloque 46 (Alarm console real-time)

**Hallazgo de 47 que afecta 46**:
- `BAlarmChannel` sobre BOX disponible (inventario de channels en 47.5.6) — alarmas en tiempo real via BOX
- `baja.Ord.make("alarm:").get()` para BAlarmService (CONFIRMADO Bloque 51.2.6) — acceso directo
- `canAcknowledgeAlarms` verifica permiso `operatorWrite` en `BAlarmService` — la SPA debe chequearlo

**Prioridad Bloque 46**: ALTA. Las alarmas son el core de BAS.

### Impacto en Bloque 48 (Escribir puntos desde SPA)

**Hallazgo de 47 que afecta 48**:
- Writes van via BajaScript directo: `component.set(value, priority)` — NO hay endpoint REST de write en Niagara nativo
- Priority array 16 niveles — la SPA debe pasar nivel de prioridad (CONFIRMADO que Reflow no lo implementa aún, Bloque 50.1.4)
- `BOverride` con duration — para overrides temporales, la API es diferente
- CSRF NO necesario para BOX (validado en el POST `/box`) pero SÍ para REST
- `Flags.ASYNC` obligatorio en writes para evitar blocking del engine thread (patrón de KNX en Bloque 37 aplica a cualquier write)

**Prioridad Bloque 48**: ALTA. Escribir puntos es funcionalidad crítica de BAS.

### Impacto en Bloque 49 (i18n / lexicon)

**Hallazgo de 47 que afecta 49**:
- `lex!` AMD loader disponible en RequireJS config — si la SPA usa BajaScript, tiene acceso a lexicons de Niagara
- Locale detectado por `LocaleFilter` (Bloque 29.3.7) — si la SPA envía `Accept-Language`, Niagara lo usa
- `niagara_locale` cookie — la SPA puede setear esta cookie para forzar locale
- `bs.built.min.js` es OBLIGATORIO (47.1.5) — no hay subset mínimo sin i18n de Niagara

**Prioridad Bloque 49**: BAJA-MEDIA. i18n es una preocupación de segundo orden para el MVP del dashboard.

### Priorización ajustada post-47

| Bloque | Tema | Prioridad anterior | Prioridad post-47 | Razón del cambio |
|--------|------|--------------------|-------------------|-----------------|
| 42 | Subscriber lifecycle | ALTA | **ALTA** | Lease renewal + reconnect son críticos para dashboard live |
| 44 | REST API / RPC | ALTA | **ALTA** | CSRF interceptor y authToken son prerequisito |
| 45 | History REST | MEDIA | **MEDIA** | Sin cambio significativo |
| 46 | Alarm real-time | ALTA | **ALTA** | BAlarmChannel disponible — acelera implementación |
| 48 | Write points | ALTA | **ALTA** | Priority array y ASYNC flag son detalles que hay que documentar bien |
| 49 | i18n | BAJA | **BAJA** | bs.built.min.js obligatorio confirma que lexicons Niagara son accessibles if needed, pero no es bloqueante |

---

## 47.10 TODOs honestos

**TODO-1 — SCRAM JS implementation testing**: No se verificó empíricamente un SCRAM-SHA256 handshake completo desde JavaScript puro. El flow de 6 pasos es documentado (Bloque 18.6.1) pero no hay implementación de referencia open source verificada contra N4.14. La implementación con `crypto.subtle` requiere testing real.

**TODO-2 — ForwardedRequestCustomizer availability**: Bloque 29.12.1 menciona que Niagara NO expone `ForwardedRequestCustomizer` de Jetty en las propiedades de `BJettyWebServer` directamente. No se verificó si hay un mecanismo alternativo (BOG edit manual, system property) para activarlo. INFERIDO: requiere módulo custom o edit directo del `.bog`.

**TODO-3 — BHttpHeaderProviders y CORS via custom provider**: `BHttpHeaderProviders` tiene slots genéricos. Existe `BGenericHttpHeaderProvider` que permite headers custom. Teóricamente se podría usar para emitir `Access-Control-Allow-Origin` desde la UI BOG, sin módulo custom. No se verificó empíricamente que esté expuesto en la UI de Workbench y qué restricciones tiene. Es un TODO de investigación que podría simplificar el setup cross-origin.

**TODO-4 — Límite máximo de lease time server-side**: `Component.js` acepta `time` custom en `lease()`, pero no se verificó si el servidor tiene un límite máximo. `LeaseManager` (Bloque 20) gestiona los leases — podría rechazar un tiempo excessivo. Necesita prueba en lab.

**TODO-5 — Origin validation en BoxWebSocketServlet**: Se afirmó (47.2.6) que el server NO valida el header `Origin` en el WS handshake, basado en ausencia de evidencia empírica. No se decompilaron las clases internas de `BoxWebSocketServlet` para confirmar. Es un TODO de seguridad importante para deployment en internet.

**TODO-6 — Session sharing entre múltiples WebSockets**: Si la SPA abre múltiples instancias BOX (ej. múltiples widgets independientes que hacen `baja.comm.start()`), ¿comparten el mismo `JSESSIONID` y `superId`? El flag `baja.disableConnectionReuse` en Reflow sugiere que SÍ hay connection reuse por defecto (CONFIRMADO Bloque 51.2.2). Pero el comportamiento con múltiples llamadas a `baja.comm.start()` desde diferentes scripts no está verificado.

**TODO-7 — Límite de BOX sessions concurrentes por usuario**: `NSuperSession` agrega, pero ¿hay un techo? El Supervisor con 50+ subordinados tiene su propio throttle (Bloque 39). Para el caso de un usuario con 10 tabs abiertas — ¿cada tab crea un BOX session separado? ¿Impacta performance de la station?

---

## 47.11 Próximos pasos

### Para implementar SEJOFA Dashboard (orden recomendado)

1. **Definir topología de red**: ¿Reverse proxy frente a station? ¿Multi-station? Esta decisión gobierna todo lo demás.

2. **Setup reverse proxy** (si se elige Approach B' o como complemento de A'):
   - nginx con `proxy_pass https://station.sejofa.io/`
   - Configurar WebSocket upgrade headers
   - `proxy_read_timeout 600s` para evitar idle cut
   - Verificar cookies `Secure` flag funcionan (HTTPS end-to-end o `proxy_ssl_verify off`)

3. **Implementar SCRAM-SHA256 client** (si no se usa SSO):
   - `crypto.subtle` implementation del protocolo (ver 47.3.2)
   - Almacenar `csrfToken` en closure de módulo JS
   - Axios interceptors para CSRF y refresh de auth

4. **Bootstrap BajaScript**:
   - Cargar `/requirejs/config.js` de la station
   - `require(["baja!"])` para iniciar el runtime
   - Implementar reconnect loop con exponential backoff

5. **Implementar lease renewal**:
   - Heartbeat cada 8 segundos para mantener lease activo (margen sobre los 10s hardcode)
   - Event handler para `reconnected` que re-registre suscripciones

6. **Implementar cleanup**:
   - `sub.detach()` en `beforeDestroy` / `onUnmounted` de cada componente que tenga suscripciones
   - `baja.comm.stop()` en `window.beforeunload`

### Para investigación futura

- Verificar `BGenericHttpHeaderProvider` para emisión de `Access-Control-*` sin módulo custom (TODO-3)
- Implementar y testear SCRAM-SHA256 headless en N4.14 (TODO-1)
- Verificar `Origin` validation en `BoxWebSocketServlet` (TODO-5)

---

## Fuentes y referencias cruzadas

| Afirmación | Fuente empírica | Bloque ref |
|------------|----------------|------------|
| `location.host` hardcoded en WebSocket | `bajaScript-ux.jar:rc/env/WebSocketConnection.js:87-113` | — |
| No CORS nativo Niagara | Scan de `web-rt.jar` — ausencia `Access-Control-Allow-Origin` | — |
| `BHttpHeaderProviders` sin CORS | `/tmp/web-rt-extract/javax/baja/web/http/BHttpHeaderProviders.class` | — |
| `X-Frame-Options: SAMEORIGIN\|DENY` | `BXFrameOptionsEnum.class` — campos `SAMEORIGIN`, `DENY` | — |
| `Cross-Origin-Opener-Policy: same-origin` | String literal en `BHttpHeaderProviders.class` | — |
| Lease 10s hardcoded | `Component.js:1795` — `bajaDef(obj.time, 10000)` | Bloque 36.5 |
| Reconnect NO automático | `WebSocketConnection.js` — no hay reconnect loop | Bloque 36.5 |
| CSRF en POST/PUT/DELETE, no GET | `CsrfProtectedFilter.class` — `init-param httpMethod` | Bloque 29.3.3 |
| CSRF token via header `x-niagara-csrfToken` | `CsrfProtectedFilter.class` | Bloque 18.5.1 |
| `/box` POST requiere CSRF, `/wsbox` no | `CsrfProtectedFilter` vs BOX handshake flow | Bloque 29.10.4 |
| `/login` post-auth retorna 302 redirect, body vacío | Live N4 station test 2026-05-06 — corrigendum 47.4.1 | **Bloque 52** |
| Plan E (ancestor frame DOM) para CSRF en SPA-in-iframe | `reflow-frontend/src/lib/csrf.js` commit `d9de398` | **Bloque 52** |
| SCRAM flow 6 pasos | `BHttpHeaderCallbackHandler` API | Bloque 18.6.1 |
| `injectBaja()` bootstrap real | `app-readable.js:121342-121421` | Bloque 51.2.2 |
| Reflow bundle en JAR `-rt` | `nmodsreflow-rt.gradle.kts:64-80` | Bloque 51.1.3 |
| `authToken` = BEARER via SCRAM | `Authentication-Info` response header SCRAM paso 6 | Bloque 18.6.1 |
| 15 min idle timeout | `BAutoLogoffSettings` default | Bloque 29.5.5 |
| `box.ws.idleTimeout=300000` default | `defaults/system.properties` + Bloque 29.10.3 | Bloque 29.10.3 |
| `SameSite=Lax` default | `BSameSiteEnum` default + Bloque 29.5.3 | Bloque 29.5.3 |
| `bs.built.min.js` 360 KB | `bajaScript-ux.jar:rc/bs.built.min.js` tamaño verificado | Bloque 22 |
| Require config via `/requirejs/config.js` | `RequireJsConfigServlet` (Bloque 29.4.1) | Bloque 29.4.1 |
| `BHeaderAuthenticationScheme` NO en distribución | Bloque 27 — gap documentado | Bloque 27 |
| oauth2-rt es cliente M2M, no auth scheme | Bloque 30 | Bloque 30.3 |
