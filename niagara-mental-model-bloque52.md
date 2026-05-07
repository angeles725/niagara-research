# Bloque 52 — CSRF cross-frame token injection en SPA-in-iframe Niagara

**Fecha**: 2026-05-06
**Método**: Hallazgo empírico durante validación de fix `harden-backup-csrf` contra station N4 en vivo (Honeywell OptimizerSupervisor-N4.14). Inspección directa del DOM del Workbench shell que hosta el iframe `nmods_iframe`, intento de Plan A (POST `/login` parsing) y observación del comportamiento real del server. Análisis estático del bundle de producción Reflow 1.7.5 (`app.af9bb4c1.js`).
**Fuentes primarias**:
- `reflow-frontend/src/lib/csrf.js` (commit `d9de398` en `Reflow-Clean-177`, módulo `csrf`)
- Live N4 station response a `GET /login` autenticado (302 redirect, body vacío)
- `window.top.document` y `window.parent.document` del frame `nmods_iframe` en sesión activa
- Bundle producción Reflow 1.7.5 (`app.af9bb4c1.js`, ausencia total de tokens "csrf"/"csrfToken")
- `CsrfProtectedFilter.class` decompilado (`web-rt.jar`) — header `x-niagara-csrfToken`
- Bloque 47.4 (CSRF token lifecycle desde SPA — corregido por este bloque)

**Versión analizada**: Honeywell OptimizerSupervisor-N4.14.0.162 + Reflow producción 1.7.5 + Reflow-Clean-177 réplica al commit `d9de398`.

---

## 52.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Este bloque documenta el patrón empírico — y NO documentado en el universo Tridium oficial — de **cómo Niagara entrega el CSRF token a una SPA custom embebida en un iframe del Workbench shell**. El token NO se obtiene del flujo `/login` que asume el resto de la documentación (incluido el propio Bloque 47.4). Vive en el `<input id="csrfToken">` que Niagara inyecta en el HTML del shell que hosta el iframe, y se accede vía `window.top.document.querySelector('input#csrfToken').value` desde el código del iframe.

### Qué corrige

| Bloque | Sección | Afirmación previa | Corrección |
|--------|---------|-------------------|------------|
| 47 | 47.4.1 | "El token CSRF se entrega embebido en HTML del `/login`" | Cierto SOLO en el flujo de primer login. Tras autenticación, `/login` redirige (302). En SPA-in-iframe el token vive en el shell ancestor — Plan E es primario, Plan A es fallback. |
| 18 | 18.5 | (no documentaba el delivery cross-frame) | Agregar nota: en deployments SPA-in-iframe, el token también está accesible vía DOM ancestor (same-origin). |
| 29 | 29.3.3 | (lista `CsrfProtectedFilter` exemptions) | Agregar gotcha sobre cross-frame token injection — es feature, no bug. |

### ¿Qué NO es este bloque?

- **NO redefine el header** — sigue siendo `x-niagara-csrfToken` (Bloque 18.5.1).
- **NO redefine cuándo se valida** — sigue siendo POST/PUT/DELETE/PATCH (Bloque 29.3.3).
- **NO cubre flows headless puros** (cliente sin iframe) — esos siguen el Plan A clásico de Bloque 47.4.
- **NO cubre la creación server-side del token** — eso vive en `NiagaraWebSession` y está documentado en Bloques 18 y 29.

### Pregunta unificadora

> Tengo una SPA custom (Vue, React, vanilla) embebida en un `<iframe>` del Workbench. Quiero hacer `POST /mymodule/api/destroy` y necesito el CSRF token. ¿Dónde lo agarro?

**Respuesta corta**: `window.top.document.querySelector('input#csrfToken').value`. Funciona porque el iframe es same-origin con el shell que lo hosta. Si te quedaste con el Plan A del Bloque 47.4 (`fetch('/login') + parse HTML`), te vas a comer un 302 redirect con body vacío y vas a arrancar sin token.

---

## 52.1 El Plan A refutado: por qué `fetch('/login')` no funciona post-auth

### 52.1.1 La hipótesis original (Bloque 47.4.1)

El Bloque 47.4 propuso este flujo:

```javascript
// Plan A — refutado como path PRIMARIO
const loginResp = await fetch('/login', { credentials: 'include' });
const html = await loginResp.text();
const csrfToken = html.match(/id=["']csrfToken["'][^>]*value=["']([^"']+)["']/)?.[1];
```

La hipótesis: el server inyecta `<input id="csrfToken">` en el HTML del login, y la SPA lo extrae después de autenticarse. **Es cierto** — el HTML del primer login efectivamente contiene ese input. Pero hay un detalle empírico que rompe el flujo en el caso real.

### 52.1.2 Lo que hace el server cuando ya estás autenticado

**HALLAZGO EMPÍRICO** (validado contra Honeywell OptimizerSupervisor-N4.14.0.162 vivo, sesión 2026-05-06):

Cuando un cliente hace `GET /login` con `JSESSIONID` válido (es decir, ya autenticado), el server responde:

```
HTTP/1.1 302 Found
Location: /
Set-Cookie: niagara_session=...
Content-Length: 0
```

El body está vacío. La SPA puede o no seguir el redirect (depende de `redirect: 'follow'|'manual'|'error'` en `fetch`). Pero en cualquiera de los tres casos:

| Modo `redirect` | Status visto por SPA | Body | Token recuperable |
|-----------------|----------------------|------|-------------------|
| `'follow'` (default) | 200 (de `/`, página dashboard) | HTML de `/`, distinto del login | NO — `/` no inyecta `<input id="csrfToken">` con la misma estructura |
| `'manual'` | 0 (opaque redirect) | string vacío | NO |
| `'error'` | rechaza con TypeError | n/a | NO |

**Consecuencia directa**: el regex `html.match(/id=["']csrfToken["']/)` retorna `undefined` en los tres casos. La SPA arranca sin token. El primer POST recibe 403. El usuario ve la app rota.

### 52.1.3 ¿Y por qué pareció funcionar al principio?

La hipótesis del Bloque 47.4.1 vino del análisis del HTML que entrega `/login` en el flujo de **first-login** (cuando el usuario aún no se autenticó y el server muestra el form). En ese flujo, el HTML SÍ contiene el `<input id="csrfToken">` y el regex matchea. El error fue extrapolar ese caso al post-auth.

**Lección de inferencia**: documentar un flujo empírico con UN solo escenario observado (first-login) llevó a una afirmación que falla en el escenario realmente común (autenticado, navegando la SPA). El test de fuego — deploy real contra una station — fue el que reveló el gap.

---

## 52.2 El Plan E: el shell expone el token en su DOM

### 52.2.1 Cómo descubrirlo

**HALLAZGO EMPÍRICO** (sesión 2026-05-06, Reflow-Clean-177):

Mientras se debuggeaba el Plan A roto, se inspeccionó el DOM del frame padre desde la consola del iframe `nmods_iframe`:

```javascript
// Ejecutado en consola del iframe nmods_iframe (Workbench shell hosting the SPA)
window.top.document.querySelector('input#csrfToken')
// → <input id="csrfToken" name="csrfToken" type="hidden" value="abc123def456...">

window.top.document.querySelector('input#csrfToken').value
// → "abc123def456..."

// Same token presente en window.parent (en single-iframe deployment, parent === top)
window.parent.document.querySelector('input#csrfToken').value
// → "abc123def456..." (mismo valor)
```

El token vive en el HTML del Workbench shell — la página padre que renderiza el iframe del módulo custom. Niagara inyecta ese `<input>` server-side cuando construye la página del shell, mismo mecanismo que usa para servir el `/login` HTML cuando no estás autenticado.

### 52.2.2 ¿Qué shell exactamente lo inyecta?

**INFERIDO + EVIDENCIA INDIRECTA**: En deployments donde el módulo custom se accede vía la URL típica `https://station/<module>/<view>` (ej. `/nmodsreflow/station`), el HTML servido por el módulo es la página padre. Cuando ese HTML contiene un `<iframe>` que apunta al SPA bundle, el `<input id="csrfToken">` está en el HTML padre, y el iframe (la SPA) lo lee cross-frame.

**CONFIRMADO** (`reflow-frontend/src/lib/csrf.js:8-10`):

> "Niagara injects `<input id="csrfToken" value="<token>">` into the Workbench shell HTML that hosts the Reflow iframe (`nmods_iframe`). Same token is present in `window.parent.document` AND `window.top.document` — accessible cross-frame because same-origin (localhost / station host)."

En el caso Reflow-Clean-177 el shell es la página servida por el `BReflowServlet` del módulo `-rt`, y el iframe `nmods_iframe` carga el bundle Vue.

### 52.2.3 Por qué el cross-frame access funciona (same-origin)

El acceso `window.top.document` desde un iframe está gobernado por la **Same-Origin Policy** del browser:

- El shell vive en `https://station.host:443` (origin del Niagara)
- El iframe carga el bundle desde la misma URL base — `https://station.host:443/<module>/<bundle>`
- Same scheme + host + port → same origin → cross-frame DOM access permitido sin restricción

No hay CORS preflight, no hay header `Access-Control-Allow-Origin` involucrado, no hay `postMessage` ceremony. Es una lectura directa del DOM como si fuera el mismo documento.

**GOTCHA G52-1 — Si la SPA se sirve desde un origen distinto (CDN, dominio separado), el cross-frame access se rompe**: el iframe sería cross-origin con el shell, `window.top.document` lanzaría `SecurityError: Blocked a frame with origin "X" from accessing a cross-origin frame`. Es la misma restricción que evita que `evil.com` lea el DOM de `bank.com` cuando lo embebe. En esos casos hay que volver al Plan A para el primer token (asumiendo que el server emite los headers CORS necesarios — que Niagara nativo NO emite, ver Bloque 47.2).

### 52.2.4 ¿`window.parent` o `window.top`? Ambos, en este orden

**CONFIRMADO** (`csrf.js:67-81`):

```javascript
var ancestors = [];
if (typeof window !== 'undefined') {
  if (window.parent && window.parent !== window) ancestors.push(window.parent);
  if (window.top && window.top !== window && window.top !== window.parent) ancestors.push(window.top);
}
for (var i = 0; i < ancestors.length; i++) {
  var token = readFromAncestor(ancestors[i]);
  if (token) {
    _token = token;
    return Promise.resolve(_token);
  }
}
```

En deployment **single-iframe** (el caso típico de Reflow), `window.parent === window.top` — son el mismo frame y el código solo agrega uno. En deployment **nested-iframe** (un módulo dentro de otro módulo dentro del Workbench), `window.parent` es el iframe intermedio y `window.top` es el shell raíz. Probar primero `parent` minimiza la cadena de propiedades cruzadas; si el padre inmediato no tiene el token (puede ser un wrapper sin shell HTML), se fallback a `top`.

### 52.2.5 El método de lectura defensivo

**CONFIRMADO** (`csrf.js:37-48`):

```javascript
function readFromAncestor(ancestor) {
  if (!ancestor) return null;
  try {
    var input = ancestor.document.querySelector(
      'input#csrfToken, input[name="csrfToken"]'
    );
    return input && input.value ? input.value : null;
  } catch (err) {
    // Cross-origin block — caller will try the next ancestor or fallback.
    return null;
  }
}
```

Tres defensas:

1. **Selector dual**: `input#csrfToken` (por id) Y `input[name="csrfToken"]` (por name). Niagara inyecta ambos atributos, pero algunos templates custom de site-deployments solo agregan uno. Cubre los dos casos sin asumir cuál.
2. **try/catch**: si `ancestor.document` lanza por cross-origin, no rompe el flujo — devuelve `null` y el caller pasa al siguiente ancestor o al fallback Plan A.
3. **Validación de valor**: `input && input.value ? input.value : null` — un `<input id="csrfToken">` sin `value` (caso edge: shell mal-construido) cuenta como ausente.

---

## 52.3 La implementación canónica: bootstrap + cache + refresh

### 52.3.1 Anatomía del módulo `lib/csrf.js`

Tres responsabilidades:

| Función | Qué hace | Cuándo se llama |
|---------|----------|-----------------|
| `bootstrap()` | Plan E primero, Plan A fallback. Setea `_token` y `_bootstrapped` | Al inicio de la app, antes de cualquier POST |
| `getToken()` | Devuelve `_token` síncrono | Por el axios interceptor en cada request |
| `refresh()` | Resetea cache y re-llama `bootstrap()` | Por el interceptor de respuesta cuando hay 403 con `csrf` en el body |

### 52.3.2 El flow Plan E → Plan A en `bootstrap()`

**CONFIRMADO** (`csrf.js:67-111`):

```javascript
export function bootstrap() {
  // Plan E — ancestor frame DOM (the normal Reflow case)
  var ancestors = [];
  if (typeof window !== 'undefined') {
    if (window.parent && window.parent !== window) ancestors.push(window.parent);
    if (window.top && window.top !== window && window.top !== window.parent) ancestors.push(window.top);
  }
  for (var i = 0; i < ancestors.length; i++) {
    var token = readFromAncestor(ancestors[i]);
    if (token) {
      _token = token;
      _bootstrapped = true;
      return Promise.resolve(_token);  // SYNC success wrapped in promise
    }
  }

  // Plan A fallback — /login fetch (works only if not yet authenticated, or no iframe)
  return fetch(FALLBACK_URL, { credentials: 'include' })
    .then(function (response) { return response.text(); })
    .then(function (html) {
      var parser = new DOMParser();
      var doc = parser.parseFromString(html, 'text/html');
      var input = doc.getElementById('csrfToken');
      if (!input || !input.value) {
        console.warn('[csrf] bootstrap: token not found in any source ...');
        _token = null;
        _bootstrapped = true;
        return null;
      }
      _token = input.value;
      _bootstrapped = true;
      return _token;
    })
    .catch(function (err) {
      console.warn('[csrf] bootstrap: ancestors empty AND fallback fetch failed — '
        + (err && err.message));
      _token = null;
      _bootstrapped = true;
      return null;
    });
}
```

**Observaciones clave**:

1. **Soft-fail design**: `bootstrap()` NUNCA rejecta. Falla silenciosa en `_token = null`, `_bootstrapped = true`. El interceptor de 403 invocará `refresh()` después.
2. **Sync success en Plan E**: cuando ancestor hits, no hay round-trip de red — el token está disponible inmediatamente en el `Promise.resolve`. Una micro-optimización con efecto real: en deploys donde Plan E funciona, el primer POST puede salir antes que cualquier otro request. La latencia visible es 0.
3. **Sigue retornando Promise**: aunque Plan E sea sync, la API expone Promise para preservar uniformidad. Importante para callers que ya hacían `await bootstrap()`.
4. **Plan A solo se ejecuta si Plan E fallaba**: NO en paralelo. El fallback es defensivo, no concurrente.

### 52.3.3 El cache `_token` + `_bootstrapped`

**CONFIRMADO** (`csrf.js:27-28, 119-142`):

Dos variables a nivel módulo (singleton de JS, no Vuex):

```javascript
var _token = null;
var _bootstrapped = false;

export function getToken() { return _token; }
export function refresh() {
  _token = null;
  _bootstrapped = false;
  return bootstrap();
}
export function isBootstrapped() { return _bootstrapped; }
```

Decisiones de diseño explícitas (`csrf.js:3-4`):

> "Design: module singleton (NOT Vuex). CSRF is infrastructure, not app state. No UI binds to the token — it flows silently through the axios interceptor."

**Por qué NO Vuex**:

- El token no se renderiza. Ningún componente lo muestra. No necesita reactividad.
- Vuex agrega ceremonia (`mutations`, `actions`, `getters`) sin beneficio.
- El cache vive en closure del módulo — sigue siendo testeable mockeando el import.
- Aislamiento: si Vuex se borra/migra, el CSRF sigue funcionando.

### 52.3.4 El refresh loop (axios interceptor 403)

El interceptor de respuesta de axios (en `plugins/http.js`) maneja el caso "token vencido":

```javascript
axios.interceptors.response.use(
  res => res,
  async err => {
    if (err.response?.status === 403 && /csrf/i.test(err.response.data?.error || '')) {
      const newToken = await csrf.refresh();
      if (newToken) {
        err.config.headers['x-niagara-csrfToken'] = newToken;
        return axios.request(err.config);  // retry once
      }
    }
    return Promise.reject(err);
  }
);
```

**GOTCHA G52-2 — `refresh()` después de re-auth completo**: si el `JSESSIONID` también expiró (no solo el CSRF), el refresh va a leer el shell DOM otra vez. El shell pudo haber re-renderizado con un token nuevo después de un re-login implícito, o pudo seguir teniendo el token viejo si el shell no se recargó. En el segundo caso `refresh()` lee el mismo token roto y el retry vuelve a fallar. La SPA debe diferenciar 403-csrf (recuperable con refresh) de 401-session (forza re-login).

---

## 52.4 Por qué Niagara entrega el token así (modelo de seguridad)

### 52.4.1 El modelo CSRF clásico (server-side storage)

**CONFIRMADO** (Bloques 18.5.1 + 29.3.3):

Niagara implementa CSRF **server-side storage**:
- Token generado al crear `NiagaraWebSession`
- Almacenado en atributo de sesión server-side
- Comparado contra el header `x-niagara-csrfToken` en cada POST/PUT/DELETE/PATCH

NO es Double Submit Cookie (donde el token está en cookie + header). NO es per-request rotation (Synchronizer Token Pattern estricto). Es una sola token por sesión, válida hasta que la sesión muere.

### 52.4.2 ¿Cómo entregar ese token al cliente?

**Las opciones que considera Niagara**:

| Mecanismo | Nativo en N4 | Pros | Contras |
|-----------|--------------|------|---------|
| HTML inline `<input id="csrfToken">` en cada página | SÍ | Cliente lo extrae sin request extra. Funciona en JS desactivado para forms tradicionales | Cada navegación HTML lo trae. SPAs deben extraerlo del DOM |
| Cookie con `Secure` + `HttpOnly:false` | NO | Sería accesible vía `document.cookie` | Mezclar `Secure` + readable es una superficie de XSS extra |
| Custom header en respuesta | NO | Limpio para clients HTTP | No funciona en navegación full-page |
| JSON endpoint dedicado | NO (no oficial) | Cliente API-friendly | Round-trip extra, requiere su propio CSRF (chicken-egg) |

**Niagara eligió HTML inline**. Es la opción que mejor sirve a su modelo principal: aplicaciones server-rendered (PX, HX) que pintan páginas completas y dejan que el browser tradicional siga forms. Las SPAs son ciudadanos de segunda clase en este modelo — no hay endpoint JSON oficial para obtener el token.

### 52.4.3 ¿Por qué el cross-frame funciona "sin querer"?

El cross-frame DOM access es UNA CONSECUENCIA del modelo, no un feature buscado. Niagara inyecta el `<input>` en CADA HTML que renderiza. Cuando ese HTML contiene un iframe a otro módulo, el módulo hijo (si es same-origin) hereda visibilidad por el modelo del browser, no por una decisión de Niagara.

**Implicación de seguridad**: Si una aplicación maliciosa LOGRARA cargar el shell Niagara dentro de un iframe propio (con `Origin` permitido por CSP — que NO es el caso por defecto, ver `X-Frame-Options: SAMEORIGIN` del Bloque 47), esa app maliciosa podría leer el token y hacer POSTs en nombre del usuario. El defensor primario contra esto es `X-Frame-Options`/`Content-Security-Policy: frame-ancestors`, NO el modelo CSRF en sí. El CSRF protege contra forms maliciosos hosteados en otro sitio que apuntan al server Niagara — NO contra robo de token vía iframe embedding.

**GOTCHA G52-3 — Si tu deploy desactiva `X-Frame-Options` o lo cambia a `ALLOW-FROM`, el cross-frame token leak deja de ser hipotético**: cualquier sitio que el usuario visite mientras tiene la sesión Niagara abierta podría embeber el shell, leer el token via `iframe.contentDocument.querySelector('input#csrfToken')` (siempre que sea same-origin, que es el ataque puntual a defender), y hacer POSTs. Verificar que el `BHttpHeaderProviders` mantenga `SAMEORIGIN` o `DENY` en producción.

---

## 52.5 Plan A vs Plan E: tabla de comparación

| Dimensión | Plan A (`fetch /login`) | Plan E (ancestor DOM) |
|-----------|-------------------------|------------------------|
| **Cuándo funciona** | Solo si user NO autenticado todavía, o si `/login` no redirige | Si la SPA está en un iframe same-origin del shell Niagara |
| **Cuándo falla** | User autenticado: 302 redirect, body vacío, NO recuperable | SPA fuera de iframe; shell sin `<input id="csrfToken">`; cross-origin |
| **Latencia** | 1 round-trip HTTP (~50-200 ms en LAN) | 0 ms — lectura síncrona del DOM |
| **Falla silenciosa** | Sí (regex no matchea, devuelve null) | Sí (try/catch atrapa SecurityError, devuelve null) |
| **Requiere CORS** | NO (same-origin fetch) | NO (same-origin DOM) |
| **Permisos del browser** | Standard `fetch` | Cross-frame `document` access — needs same-origin |
| **Debuggable en consola** | `await fetch('/login')` | `window.top.document.querySelector('input#csrfToken')` |
| **Riesgo de token desactualizado** | Bajo (server siempre da el actual) | Medio (si el shell no se recargó tras re-auth, queda viejo) |
| **Test sin server** | Difícil (necesita mock de fetch) | Fácil (montar un DOM con el input) |

**Recomendación**: Plan E primario, Plan A fallback. Es el patrón canonizado en `csrf.js` post-d9de398.

### 52.5.1 ¿Hay un Plan B/C/D?

Sí, fueron consideradas (durante la sesión 2026-05-06) y descartadas:

- **Plan B — `/clientEnv` JSON**: el endpoint `BClientEnvServlet` (Bloque 29.4.1) podría retornar el token en su JSON. INFERIDO en Bloque 47.4.1 pero no confirmado empíricamente. Validación pendiente — si lo retornara, sería una opción más limpia que Plan A. Pero requiere que `BClientEnvServlet` esté habilitado y exponiendo el campo (no garantizado en todos los deployments).

- **Plan C — Cookie reading**: Niagara NO setea el CSRF token en una cookie accesible. La cookie `niagara_session` contiene el `JSESSIONID` no el CSRF token (CONFIRMADO Bloque 18.5.1). Plan C inviable.

- **Plan D — `postMessage` desde el shell**: requeriría que el shell HTML emita `window.postMessage(token, '*')` o equivalente. Niagara nativo NO hace esto. Sería una modificación al shell.

Plan E ganó por: (1) funciona inmediatamente sin patches al server, (2) zero latency, (3) ya estaba "ahí" — solo había que descubrir que el browser lo permite.

---

## 52.6 Edge cases y deployments donde NO funciona Plan E

### 52.6.1 SPA cargada full-page (sin iframe)

Si el usuario navega directamente a la URL de la SPA sin pasar por el shell Niagara — ej. abre `https://station/<module>/spa-bundle.html` directamente — entonces `window.parent === window` y `window.top === window`. No hay ancestor del cual leer. El `for` loop no agrega nada y se cae al Plan A.

**Comportamiento esperado**: Plan A intenta `/login`. Si el usuario YA está autenticado (cookie viva), `/login` redirige y Plan A falla también. La SPA arranca sin token, primer POST = 403, interceptor llama `refresh()`, refresh vuelve a fallar (mismo flujo). **La app queda rota** hasta que el user fuerce re-login (logout + login).

**Mitigación**: si el deploy soporta SPA full-page, considerar que el módulo `-rt` exponga un endpoint `/api/csrf-token` que devuelva el token en JSON (custom servlet). Eso requiere que el módulo lo implemente — no es nativo Niagara.

### 52.6.2 Iframe cross-origin (SPA en CDN externo)

Si la SPA se sirve desde `https://cdn.dashboard.com/spa.html` y el shell Niagara está en `https://station.host`, el iframe es cross-origin. `window.top.document` lanza `SecurityError`. Plan E falla en el `try/catch`. Plan A intenta `/login` — pero ese fetch es CROSS-ORIGIN, requiere CORS, y Niagara nativo NO emite headers CORS (Bloque 47.2). Plan A también falla.

**Mitigación**: usar uno de los approaches del Bloque 47.6 (reverse proxy que pone todo bajo el mismo origen, o módulo CORS custom). Es un problema de arquitectura, no de CSRF.

### 52.6.3 First-login (usuario aún no autenticado)

En el primer login, el shell no se ha cargado todavía — el usuario está mirando el form `/login`. La SPA no se está ejecutando aún. Cuando el form de login se submite y la SPA se carga después en el iframe, el shell padre ya tiene el token (post-auth) y Plan E funciona.

Caso edge: si la SPA decide hacer un POST ANTES de que el shell se cargue (raro pero posible si hay pre-fetch agresivo), Plan E fallaría — el ancestor todavía es el form de login, NO el shell. Plan A funcionaría aquí (porque el HTML del form sí tiene el `<input>`).

**Conclusión**: la combinación Plan E + Plan A cubre los casos correctamente. Plan E gana en post-auth (caso común), Plan A cubre pre-auth (caso edge).

### 52.6.4 Iframe sandbox sin `allow-same-origin`

Si el iframe se monta con `sandbox="allow-scripts"` (sin `allow-same-origin`), el browser fuerza al iframe a tratarse como cross-origin con su propio shell. `window.top.document` lanza SecurityError aunque el HTML venga del mismo host. Plan E falla.

Esto NO ocurre en deployments Niagara estándar (el iframe del Workbench shell NO usa sandbox restrictivo). Es relevante solo si un módulo custom decide aplicar sandbox por su cuenta — lo cual sería un anti-pattern para este flow.

### 52.6.5 Multi-iframe nested (módulo dentro de módulo)

Si hay nested iframes — Workbench shell → wrapper iframe → SPA iframe — el `window.parent` del SPA es el wrapper, y `window.top` es el shell. Si el wrapper NO tiene el `<input id="csrfToken">` (es solo un container vacío), `readFromAncestor(window.parent)` devuelve `null` y el for loop pasa a `window.top`. Si el shell raíz lo tiene, Plan E gana.

Por eso el código (`csrf.js:71-72`) probó AMBOS y en ese orden: cubre nested deployments sin cambios.

---

## 52.7 Reflow producción 1.7.5: análisis del bundle

**HALLAZGO EMPÍRICO** (sesión 2026-05-06):

Análisis del bundle de producción `app.af9bb4c1.js` (Reflow 1.7.5 desplegado, source minificado y obfuscado):

```bash
$ grep -c -i "csrf" app.af9bb4c1.js
0
$ grep -c "x-niagara-csrf" app.af9bb4c1.js
0
$ grep -c "csrfToken" app.af9bb4c1.js
0
```

**Conclusión cruda**: Reflow producción 1.7.5 **NO maneja CSRF en absoluto**. Sus POSTs van sin el header `x-niagara-csrfToken`.

### 52.7.1 ¿Cómo funciona si nunca manda CSRF?

**INFERIDO + EVIDENCIA INDIRECTA**: Si el `CsrfProtectedFilter` estuviera realmente activo en producción, todos los POSTs de Reflow recibirían 403. La app no funcionaría. Como funciona, una de estas opciones es cierta:

1. **El filter no está aplicado a los paths del módulo**: el `web.xml` de `nmodsreflow-rt` excluye sus paths del filter. Esto sería una decisión consciente — pero implica que esos endpoints son CSRF-vulnerables (Bloque 51.3.3 ya identificó esto en backups).

2. **El filter está deshabilitado a nivel station**: alguna config global apaga `CsrfProtectedFilter`. Si esto es cierto, TODA la station es CSRF-vulnerable, no solo Reflow.

3. **El BOX bypass cuenta**: el handshake `/box` requiere CSRF (Bloque 47.5.1) pero los POSTs subsiguientes vía BOX van sobre WebSocket — NO pasan por filtros HTTP estándar. Si TODO Reflow comunica vía BOX (no REST tradicional), el CSRF nunca aplica. Esto es probablemente la verdad — Reflow es BajaScript-heavy y la mayoría de la lógica vive sobre BOX.

**Verificación pendiente** (TODO-52-1): hacer `grep` de patterns `BWebServlet`/POST endpoints en `nmodsreflow-rt` decompilado. Si los pocos POSTs REST existentes van a paths excluidos, queda confirmada opción 1. Si no, queda activa la dimensión "BOX bypass" como respuesta.

### 52.7.2 ¿Por qué Reflow-Clean-177 SÍ implementa CSRF entonces?

Decisión arquitectónica explícita en Reflow-Clean-177 (commit `5ffd40b`, change `harden-backup-csrf`): los nuevos POST endpoints de `BaseServlet` (incluyendo `/backups/destroy` movido de GET a POST) validan CSRF inline. Esto es **endurecimiento sobre producción** — Reflow producción nunca lo hizo, pero la réplica decide hacerlo bien. El fix `d9de398` (Plan E) fue necesario porque el bootstrap original se rompía contra la station real.

**Lección estratégica**: Reflow producción es "good enough by accident" en CSRF — funciona porque no cruza el filter, no porque maneje el token. Una réplica que se propone hacerlo bien necesita resolver este problema explícitamente.

---

## 52.8 Validación empírica: cómo testearlo en la consola

### 52.8.1 Test rápido en sesión activa

Abrir el iframe del módulo, ir a la consola (DevTools → Console → frame `nmods_iframe` o equivalente):

```javascript
// 1. ¿Soy iframe?
console.log('Has parent:', window.parent !== window);
console.log('Has top:', window.top !== window);

// 2. ¿Puedo leer el ancestor?
try {
  console.log('Parent doc accessible:', !!window.parent.document);
  console.log('Top doc accessible:', !!window.top.document);
} catch(e) {
  console.error('Cross-origin block:', e.message);
}

// 3. ¿Está el input ahí?
var input = window.top.document.querySelector('input#csrfToken, input[name="csrfToken"]');
console.log('Token input:', input);
console.log('Token value:', input?.value);
```

**Resultado esperado** (deploy Niagara estándar SPA-in-iframe):
```
Has parent: true
Has top: true
Parent doc accessible: true
Top doc accessible: true
Token input: <input id="csrfToken" name="csrfToken" type="hidden" value="...">
Token value: "abc123def456..."
```

### 52.8.2 Test del bootstrap completo

```javascript
// Simular el bootstrap del módulo csrf
function simulateBootstrap() {
  var ancestors = [];
  if (window.parent && window.parent !== window) ancestors.push(window.parent);
  if (window.top && window.top !== window && window.top !== window.parent) ancestors.push(window.top);

  for (var i = 0; i < ancestors.length; i++) {
    try {
      var input = ancestors[i].document.querySelector('input#csrfToken, input[name="csrfToken"]');
      if (input && input.value) return { source: 'ancestor[' + i + ']', token: input.value };
    } catch(e) {}
  }
  return { source: 'plan-a-fallback', token: null };
}
console.log(simulateBootstrap());
```

**Output esperado en frame `nmods_iframe` Reflow producción**:
```
{ source: 'ancestor[0]', token: 'abc123...' }
```

### 52.8.3 Test del Plan A (verificar la falla esperada)

```javascript
// Plan A debería fallar post-auth
fetch('/login', { credentials: 'include' })
  .then(r => { console.log('Status:', r.status, 'Type:', r.type); return r.text(); })
  .then(html => {
    console.log('Body length:', html.length);
    var m = html.match(/id=["']csrfToken["'][^>]*value=["']([^"']+)["']/);
    console.log('Token from /login:', m?.[1] ?? null);
  });
```

**Output esperado autenticado**:
```
Status: 200 Type: basic    // (después de seguir el redirect a /)
Body length: 4523          // HTML de la página dashboard, NO del login
Token from /login: null    // el regex no matchea — el HTML de / tiene otra estructura
```

O con `redirect: 'manual'`:
```
Status: 0 Type: opaqueredirect
Body length: 0
Token from /login: null
```

Esto demuestra empíricamente por qué Plan A es inviable como path primario post-auth.

---

## 52.9 Cross-bloque corrigendum

### 52.9.1 Bloque 47.4.1 — corrección puntual

**Texto previo** (Bloque 47.4.1):
> "El token CSRF se crea al establecer la sesión HTTP. Se entrega al cliente embebido en HTML del `/login` como `<input id="csrfToken" value="...">`. La SPA lo extrae del DOM o de la respuesta del login..."

**Texto corregido** (a aplicar al Bloque 47.4.1):
> "El token CSRF se crea al establecer la sesión HTTP. Se entrega al cliente embebido en HTML del `/login` (en el flujo de **first-login**, antes de autenticación) y también en CADA HTML servido por la station mientras la sesión está viva (en cualquier página renderizada por el shell). Para SPAs embebidas en iframe del Workbench shell, el token NO se obtiene de `/login` post-autenticación (que redirige 302 con body vacío) sino del DOM del frame ancestor: `window.top.document.querySelector('input#csrfToken').value`. Ver Bloque 52 para el patrón completo Plan E + Plan A fallback."

### 52.9.2 Bloque 18.5 — adición

**Adición a aplicar al Bloque 18.5** (al final de la sección):
> "**Nota cross-frame delivery (CONFIRMADO sesión 2026-05-06)**: Niagara inyecta el `<input id="csrfToken">` en CADA HTML que renderiza, no solo en `/login`. Para SPAs custom en iframe same-origin del Workbench shell, esto significa que el token es accesible vía `window.top.document` desde el código del iframe. Ver Bloque 52 para el patrón."

### 52.9.3 Bloque 29.3.3 — gotcha agregado

**Gotcha a agregar al Bloque 29.3.3** (lista de exemptions/gotchas del `CsrfProtectedFilter`):
> "**G29-X — Cross-frame token reading (NO es un bug, es una consecuencia del modelo)**: El token CSRF es legible cross-frame si el iframe es same-origin con el shell ancestor. Esto es esperado y deseable (permite SPAs custom en iframe leer el token sin endpoint dedicado). El defensor contra abuso es `X-Frame-Options: SAMEORIGIN` (Bloque 47.2.5), NO el modelo CSRF en sí. Si el deploy desactiva XFO, queda expuesto a token leak vía iframe embedding. Ver Bloque 52."

---

## 52.10 Gotchas transversales

**G52-1 — Cross-origin SPA pierde Plan E**: Si la SPA se sirve desde dominio distinto al de la station, `window.top.document` lanza SecurityError. Plan E falla. Plan A también falla (sin CORS headers). Solución: reverse proxy que ponga todo bajo el mismo origen (Bloque 47.6).

**G52-2 — Token desactualizado tras re-auth implícito**: Si el server hace re-auth y emite nuevo `JSESSIONID` sin que el shell se recargue, `refresh()` lee el mismo token viejo del DOM ancestor. La SPA debe diferenciar 403-csrf (recuperable) de 401-session (forza re-login + reload).

**G52-3 — Sin XFO, leak vía iframe embedding**: si el deploy desactiva `X-Frame-Options` o lo cambia a `ALLOW-FROM`, cualquier sitio que embeba el shell same-origin podría leer el token. Verificar `BHttpHeaderProviders` mantenga `SAMEORIGIN` en producción.

**G52-4 — `iframe sandbox` rompe el modelo**: si un módulo custom decide montar el iframe con `sandbox="allow-scripts"` (sin `allow-same-origin`), Plan E falla aunque el HTML venga del mismo host. Anti-pattern para este flow.

**G52-5 — Plan A es trampa documental**: Confiar en `fetch('/login') + parse HTML` lleva a deploy roto. La validación contra station autenticada es OBLIGATORIA antes de declarar el bootstrap CSRF como funcional. Lo que parece funcionar en local-dev (donde uno está usando first-login frecuentemente) explota en producción.

**G52-6 — `BClientEnvServlet` como Plan B sin confirmar**: el endpoint `/clientEnv` MENCIONADO como alternativa en Bloque 47.4.1 NO está confirmado empíricamente que retorne el CSRF token. Si tu fix depende de eso, validalo primero — no lo asumas.

**G52-7 — Module singleton pattern para infraestructura, no Vuex**: lecciones de `csrf.js` aplicables a otros estados de infraestructura (auth tokens, feature flags resueltos al arranque, lease IDs): viven en closure de módulo. Vuex es para estado que se renderiza.

**G52-8 — `Promise.resolve` para preservar API uniforme**: aunque Plan E sea sync, devolver `Promise.resolve(token)` permite que callers existentes que hacen `await bootstrap()` sigan funcionando. Cambiar la API a sync rompe call sites — no vale la pena por ese ms.

**G52-9 — Reflow producción no maneja CSRF en absoluto**: 0 refs de "csrf" en `app.af9bb4c1.js`. Funciona porque sus POSTs probablemente van sobre BOX (que bypassea filters HTTP) o sus REST endpoints están excluidos del `CsrfProtectedFilter` en `web.xml`. NO copiar este patrón en módulos nuevos — replica el endurecimiento de Reflow-Clean-177.

---

## 52.11 TODOs y validaciones pendientes

**TODO-52-1 — Confirmar BOX bypass del CsrfProtectedFilter**: validar empíricamente que mensajes BOX sobre WebSocket NO pasan por `CsrfProtectedFilter`. Implicación: APIs RPC sobre BOX están exentas de CSRF — el modelo de seguridad relevante es validación de session vía cookie + lease scoping. Verificación: agregar logging al filter (en una build local), monitorear si requests BOX activan el filter o no.

**TODO-52-2 — Verificar `/clientEnv` content**: hacer GET a `/clientEnv` en una sesión Niagara real, parsear la respuesta JSON, ver si contiene `csrfToken` o equivalente. Si lo contiene, sería Plan B confirmado y más limpio que Plan A.

**TODO-52-3 — Audit `web.xml` de Reflow producción**: confirmar opción (1) de 52.7.1 — los paths del módulo están explícitamente excluidos del `CsrfProtectedFilter`. Bajar el JAR `nmodsreflow-rt-1.7.5.jar` y leer `web.xml`.

**TODO-52-4 — Test multi-iframe nested**: validar empíricamente el patrón con un wrapper iframe vacío entre el shell y la SPA. Confirmar que el for loop pasa al `window.top` correctamente cuando `window.parent` no tiene el input.

**TODO-52-5 — Validar el comportamiento con iframe `sandbox`**: forzar el iframe Reflow con `sandbox="allow-scripts"` (modificando el HTML del shell), confirmar que Plan E falla con SecurityError, confirmar que Plan A es el único path posible (y que también falla post-auth — la SPA queda completamente rota).

**TODO-52-6 — Plan D (postMessage) como future-proof**: explorar si vale la pena que un módulo custom `-rt` agregue un script al shell que emita `postMessage(token, '*')` al iframe hijo. Sería resilient a cross-origin (siempre que el shell hijo verifique el `event.origin`). Cost-benefit: requiere modificar el shell, pero independiza la SPA del DOM-snooping del padre.

---

## 52.12 Próximos pasos

### Para deployments SPA-in-iframe Niagara (orden recomendado)

1. **Implementar `lib/csrf.js`** con el patrón Plan E + Plan A fallback (referencia: `Reflow-Clean-177/reflow-frontend/src/lib/csrf.js`).
2. **Wire al axios interceptor**: request interceptor agrega `x-niagara-csrfToken` header en POST/PUT/DELETE/PATCH; response interceptor maneja 403-csrf con `refresh()`.
3. **Llamar `bootstrap()` en el `main.js`** antes de cualquier mount de Vue/React. Awaitear el promise — Plan E es sync así que el await es nop, Plan A da una sola promesa de fetch.
4. **Validar empíricamente contra station real** — NO confiar en local-dev. Plan A pasa local porque first-login es frecuente; producción explota.
5. **Audit del módulo `-rt`**: confirmar que TODOS los POST/PUT/DELETE/PATCH endpoints validan CSRF inline (`CsrfGuard.validate(req)` o equivalente). Plan E del cliente sin validación server es seguridad teatro.
6. **Verificar `X-Frame-Options`** en producción (`SAMEORIGIN` o `DENY`). Sin esto, el cross-frame token leak deja de ser hipotético.

### Para investigación futura

- Confirmar BOX bypass empíricamente (TODO-52-1)
- Verificar `/clientEnv` (TODO-52-2)
- Auditar `web.xml` de Reflow producción (TODO-52-3)
- Explorar Plan D `postMessage` (TODO-52-6)

---

## Fuentes y referencias cruzadas

| Afirmación | Fuente empírica | Bloque ref |
|------------|-----------------|------------|
| `/login` retorna 302 redirect post-auth | Live N4 station test 2026-05-06 (sesión Reflow-Clean-177) | — |
| Token vive en `window.top.document.querySelector('input#csrfToken')` | DOM inspection iframe `nmods_iframe` 2026-05-06 | — |
| Same token en `window.parent` (single-iframe deploy) | DOM inspection idem | — |
| Cross-frame DOM access permitido same-origin | Same-Origin Policy spec; verificación empírica `try { window.top.document } catch` | — |
| Plan E + Plan A fallback canonizado | `reflow-frontend/src/lib/csrf.js` commit `d9de398` | — |
| Niagara NO emite CORS headers | Bloque 47.2 — scan de `web-rt.jar` | Bloque 47.2 |
| `X-Frame-Options: SAMEORIGIN\|DENY` | `BXFrameOptionsEnum.class` | Bloque 47.2.5 |
| Header CSRF: `x-niagara-csrfToken` | `CsrfProtectedFilter.class` | Bloque 18.5.1 + 29.3.3 |
| CSRF aplicado a POST/PUT/DELETE/PATCH, no GET | `CsrfProtectedFilter.class` `init-param httpMethod` | Bloque 29.3.3 |
| `/box` POST requiere CSRF, `/wsbox` upgrade no | `CsrfProtectedFilter` vs BOX handshake | Bloque 29.10.4 + 47.5.1 |
| Server-side storage pattern (no Double Submit) | Bloque 18.5 + `NiagaraWebSession` attribute pattern | Bloque 18.5 |
| Bundle Reflow producción 1.7.5 — 0 refs csrf | `grep -c "csrf" app.af9bb4c1.js` 2026-05-06 | — |
| BOX bypass de filters HTTP | INFERIDO desde arquitectura WebSocket vs Servlet pipeline | Bloque 36.6 + 47.5 |
| `BClientEnvServlet` posible Plan B | INFERIDO Bloque 47.4.1 — NO confirmado empíricamente | Bloque 29.4.1 |
| Module singleton vs Vuex para infra-state | `csrf.js:3-4` design comment | — |
| `harden-backup-csrf` SDD change | Reflow-Clean-177 commit `5ffd40b` + archive | — |
| Plan E refute commit | Reflow-Clean-177 commit `d9de398` "fix(csrf): bootstrap from ancestor frame DOM" | — |
| Reflow-Clean-177 audit prior a este | Bloque 51 (Reflow audit) — re-audit 2026-05-05 | Bloque 51 |
