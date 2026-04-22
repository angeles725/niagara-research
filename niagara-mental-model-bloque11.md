# Niagara N4 — Mental Model · Bloque 11: Auth / RBAC runtime

**Sesión**: 2026-04-22
**Fuentes**: devguide + guides-clean, source `javax.baja.user.*`, `javax.baja.role.*`, `javax.baja.category.*`, `javax.baja.security.*`, `javax.baja.authentication.*`.

**Nota crítica**: este bloque es distinto del Bloque 3. Bloque 3 cubre el **sandbox JVM** (cert chain validation, Java permissions, 19 permission groups). Este bloque cubre el **RBAC del sistema BAS** — usuarios finales, roles, categorías del árbol, permisos a nivel de BComponent.

---

## 11.1 BUser + BRole + BCategory + BPermissions

### 11.1.1 BUser

Componente que representa usuarios (humanos y máquina-a-máquina).

**Props principales**:
- `name`, `fullName`, `email`, `language`.
- `enabled` (bool).
- `expiration` (BAbsTime): fecha de expiración del user.
- `lockOut` (readonly): lockout status post fallos.
- `roles[]`: array de ORDs a `BRole` components.
- `authenticator`: pluggable (default `BPasswordAuthenticator`).
- `permissions`: transient cache calculado de roles (BPermissionsMap).
- `autoLogoffPeriod`: 2-240 minutos, default 15.

`Authenticator` (abstract `BAbstractAuthenticator`): decide cómo se valida al user. `BPasswordAuthenticator` soporta expiración y force reset.

### 11.1.2 BRole + Auth scheme

`BRole`: componente que agrupa un `BPermissionsMap`. Un user puede tener N roles. Los permissions se combinan con OR.

Cada user tiene su propio `authenticator` (default password, pero puede ser cert, LDAP, Kerberos, SAML — ver 11.2).

**Propagación**: cuando `BRole.permissions` cambia, `BRoleService` propaga a todos los users que lo tienen.

### 11.1.3 BCategory + BCategoryService

**BCategory**: partición lógica del árbol de componentes. Un BComponent puede pertenecer a 0 o N categorías.

- `index` (int): posición en el CategoryMask (bitmap 64-bit).
- `mode`: union (OR) o intersection (AND) para combinar permisos de múltiples categorías.

**BCategoryService**: mapea `BCategoryMask` (64-bit) a índices [1..N]. Máximo 64 categorías per station.

**Ejemplo**: un componente "Floor1/Chiller1" puede estar en categorías "HVAC" (index=1), "Floor1" (index=2), "CriticalEquipment" (index=3). Su categoryMask = bits (1 | 2 | 3).

### 11.1.4 BPermissions — granularidad

6 bits:
- **rwi** (lowercase): operator read / write / invoke.
- **RWI** (uppercase): admin read / write / invoke.

Encoding string: `"rwi"` (operator completo), `"RWI"` (admin completo), `"r"` (solo read operator), `"RW"` (admin read+write sin invoke).

**Normalización automática**: `ADMIN_WRITE` → fuerza `OPERATOR_READ + OPERATOR_WRITE + ADMIN_READ`. No podés dar admin write sin los inferiores. Evita configuraciones inconsistentes.

### 11.1.5 BPermissionsMap

Array `BPermissions[]` indexado por `categoryIndex`. Cada slot del array = permisos sobre componentes en esa categoría.

**Especiales**:
- `BPermissionsMap.SUPER_USER`: todos los permisos sobre todas las categorías.
- `BPermissionsMap.DEFAULT`: vacío (ningún permiso).

**Operaciones**:
- `.or(otherMap)`: OR bit-a-bit — combinación de múltiples roles.
- `.and(otherMap)`: AND bit-a-bit — intersección (raro).

Cuando user tiene roles R1, R2, R3 → user.permissions = R1.permissions OR R2.permissions OR R3.permissions.

### 11.1.6 BUserService

Gestiona users del station.

**Props de política**:
- `lockOutEnabled` (bool).
- `maxBadLoginsBeforeLockOut` (1-10).
- `lockOutWindow` (default 30s): ventana en que contar fallos.
- `lockOutPeriod` (default 10s): tiempo de bloqueo tras alcanzar max.
- `defaultAutoLogoffPeriod` (default 15 min, rango 2m-4h).

**User prototypes**: plantillas que definen roles/auth scheme para users creados dinámicamente (ej. desde LDAP sync).

### 11.1.7 BRoleService

Gestiona roles.

- `BRole`: concreto, configurable.
- `BAdminRole`: super-user hardcoded, otorga `BPermissionsMap.SUPER_USER`.

### 11.1.8 Evaluación de permisos

Flujo cuando un user invoca una acción X sobre componente Y:

1. **Extrae BUser** del contexto (sesión actual).
2. **Identifica categorías** del componente Y:
   - Y implementa `BICategorizable` → `Y.getCategoryMask()` retorna BCategoryMask 64-bit.
3. **Map categorías a índices**: `BCategoryService.resolveIndices(mask)` → lista de índices.
4. **Busca permisos**: para cada índice, consulta `user.permissions[index]` → BPermissions.
5. **Combina** si múltiples categorías (union=OR, intersection=AND según `BCategory.mode`).
6. **Verifica** bits requeridos (read=r/R, write=w/W, invoke=i/I).
7. **Si falta permiso** → `PermissionException`.

**Caso múltiples roles del user**: `user.permissions = role1 OR role2 OR ... OR roleN`.

**Caso componente sin categorías**: cae a permisos default/global (configurado en BUserService).

**Caso componente con categoría que user no cubre**: denegado implícitamente.

---

## 11.2 Authentication schemes

### 11.2.1 BAuthenticationService orquestador

Enrutador central. Contenedor de uno o más `BAuthenticationScheme` en carpeta `AuthenticationSchemes`. Cada user tiene asignado un scheme.

**Soporta múltiples esquemas simultáneamente** (ej. Digest para humanos, Certificate para dispositivos automatizados).

**Flujo**:
1. Cliente inicia conexión.
2. AuthenticationService busca user en UserService.
3. Obtiene scheme del user (`user.getAuthenticator()`).
4. Ejecuta JAAS LoginContext con módulo del scheme.
5. LoginException si falla, acceso si éxito.

### 11.2.2 Digest (Niagara 4 default)

**`BDigestAuthenticationScheme`** (palette baja). Usa **SCRAM-SHA256** (RFC 5802) sobre TLS.

Nunca transmite password plaintext. Cliente envía prueba criptográfica.

**Flujo 4 mensajes**:
1. Cliente → Server: nombre user.
2. Server → Client: nonce (desafío aleatorio).
3. Client → Server: `hash(password + salt + nonce)`.
4. Server → Client: verificación.

**Uso**: Workbench↔Station (FoxService), Browser↔Station (WebService), Station↔Station.

**Config global password**: minimum length, require uppercase, require digits, password history.

**Seguridad**: 3 (con TLS) / 4 (sin TLS).

### 11.2.3 AX Digest (legacy)

**`BLegacyDigestAuthenticationScheme`**. Backward compat con NiagaraAX 3.5u4+. Mismo SCRAM-SHA256 pero diferente orden de ops para ajustarse al protocolo AX.

Deprecado. N4 puro debería usar Digest.

### 11.2.4 Certificate (mTLS / X.509)

**`BClientCertAuthScheme`** (palette clientCertAuth). Cliente presenta X.509 cert con private key. Server verifica contra `User.ClientCertAuthenticator` stored public cert.

**Ventajas**: sin transmisión de credenciales, M2M/kiosk mode.
**Desventajas**: gestión PKI compleja (CSR, signing, distribución, rotación).

### 11.2.5 Kerberos / Active Directory

**`BKerberosScheme`** (palette ldap). Kerberos V5 ticket-based + mutual auth.

**Config**:
- KDC host (usualmente AD).
- Realm.
- SPN (Service Principal Name).
- Cache Expiration (fallback si KDC down).

**Sincronización de tiempo crítica** — skew = fallo.

### 11.2.6 SAML 2.0

**`BSAMLAuthenticationScheme`** (palette saml, extends BSSOAuthenticationScheme).

Federated SSO token-based. IdP (Okta, ADFS, OpenAM) autentica → firma assertion → SP (station Niagara) valida firma y crea sesión.

**Config station**:
- IdP Host URL + port + login path.
- IdP Cert (validar firma).
- SAML Server Cert (firmar propias assertions si IdP requiere signed requests).
- Time Skew (3 min default).
- Requested Authentication Type (Password/Kerberos/SmartCard/Unspecified).
- Requested Auth Comparison (Exact/Minimum/Better/Maximum).

**Metadata URL**: `https://station/saml/samlrp/metadata?scheme=MyScheme` auto-genera XML para registrar SP en IdP.

**Usuarios pullados dinámicamente** de IdP, roles mapeados desde claims.

### 11.2.7 HTTP Basic (legacy)

**`BHttpBasicAuthenticationScheme`**. Header `Authorization: Basic base64(user:pass)`. Password en base64 (NO cifrado) — requiere TLS obligatorio.

Casos de uso: APIs simples, scripts curl/wget, clients legacy.

**NUNCA usar sin TLS**. Seguridad 5 (ninguna sin TLS).

### 11.2.8 Google Authenticator (2FA / TOTP)

**`BGoogleAuthenticationScheme`** (palette gauth). Password + TOTP (6-digit, 30s refresh).

Usuario instala Google Authenticator móvil, station provisiona QR único (seed compartido). Login = username + password + token.

**Requiere sync de tiempo**. Seguridad 1 (mejor).

### 11.2.9 LDAP

**`BLdapScheme`** (palette ldap). Usuarios en servidor LDAP/OpenLDAP (no necesariamente AD).

**Configs**:
- AD Config: AD 2008+, sAMAccountName, LDAP over TLS.
- LDAP V2: legacy, simple bind + password.
- LDAP V3: moderno con SASL (CRAM-MD5, DIGEST-MD5, Simple).

**Common props**:
- Connection URL: `ldap://host:389` o `ldaps://host:636`.
- SSL: true (TLS obligatorio).
- User Login Attr: `sAMAccountName` (AD) / `uid` (OpenLDAP).
- User Base: `DC=domain,DC=com`.
- Attr mapping: email, fullName, memberOf → roles.
- Cache Expiration: fallback si LDAP down.
- Auth Mechanism V3: None (requires TLS), CRAM-MD5, DIGEST-MD5.

**Flujo**: user ingresa username → station busca en LDAP (anon o connection user) → obtiene DN → `LDAP bind(DN, password)` → extrae atributos → crea/actualiza user local.

### 11.2.10 Tabla comparativa

| Scheme | Tipo | Password Envío | TLS | Users | SSO | 2FA | Seguridad |
|--------|------|----------------|-----|-------|-----|-----|-----------|
| Digest | Local | Prueba SCRAM-SHA256 | Recomend | Locales | No | con Google | 3 TLS / 4 no |
| AX Digest | Local | SCRAM-SHA256 orden AX | Recomend | Locales | No | No | 3 TLS / 4 no |
| HTTP Basic | Local | base64 (no cifrado) | **OBLIGATORIO** | Locales | No | No | 5 sin TLS |
| Kerberos | Remoto AD | Ticket KDC | No (propio canal) | AD | **Sí** | No | 2 |
| LDAP V2/V3 | Remoto | Simple/CRAM/DIGEST | **OBLIGATORIO** | LDAP | No | No | 4 TLS / 5 no |
| SAML 2.0 | Federated | Assertion IdP | TLS típico | IdP | **Sí** | IdP decide | 1 + IdP |
| Certificate | Cripto X.509 | Private key | Sí | Locales | Sí (kiosk) | No | 3 |
| Google Auth | Local 2FA | Password + TOTP | Recomend | Locales | No | **Sí (TOTP)** | 1 |

---

## 11.3 Session management + AutoLogoff + Enterprise Security

### 11.3.1 Session lifecycle

Login → create session → activity monitoring → idle/timeout → logout.

**Dos contextos distintos** (Bloque 10):
1. **Platform Session (5011)**: daemon ↔ Workbench. OS-level credentials.
2. **Fox Session (1911/4911)**: Station ↔ remotes. BUser + auth scheme.

Ambos usan `BAuthenticationService` centralizado, pero scopes de permisos independientes.

**Session holders**: `BFoxProxySession` (Workbench), `HttpSession` (web). Property `sessionId` identifica para logs/audit.

### 11.3.2 Session ID + cookies

**Web session ID** (Jetty):
- Cookie typical: `JSESSIONID`.
- Attributes (de BWebService, ver Bloque 9.3.5):
  - `httpOnly=true` (previene XSS theft).
  - `secure=true` (solo HTTPS si `requireHttpsForPasswords=true`).
  - `sameSite=Strict|Lax|None` configurable.

**Fox session ID** (FOX protocol): generado durante handshake FOX, parte del protocolo binario. No es HTTP cookie. Transportado en mensajes encriptados si FoxS/TLS.

**User resolution**:
- Web: `HttpServletRequest.getUserPrincipal()` → BUser.
- Fox: session lookup → BUser (Tridium proprietary).

### 11.3.3 AutoLogoff (Workbench + Web)

**Mecanismo**: activity monitoring. Click/keystroke/mouse-move reset del timer. Expira sin activity → session close.

**Workbench**:
- `ActivityListener` interface. `BWbShell` emite `activity()` callback.
- Custom views no attached a station requieren manual registration + `session.userActivity()` calls.
- **Grace period**: 30s antes de timeout. `NotifyListener.onNotify()` dispara. Dialog "Continuar sesión" via `BWbShell.notifyTimeout()`.
- `pauseActivityMonitor()` / `resumeActivityMonitor()` para long-running ops sin interacción (exports).

**Web**:
- JS `activityMonitor.js` (auto-include en Hx, Velocity, Mobile profiles).
- Mixin pattern: `activityMonitor.mixinKeepAlive(widget)` — auto-pausa on create, auto-resume on destroy.
- Manual API: `activityMonitor.keepAlive()` returns Promise con token, `activityMonitor.release(token)`.
- Grace period: 30s.
- **Dashboard pattern**: `keepAlive()` sin release mantiene sesión indefinida (live monitors).
- **`UserActivityFilter`** (servlet filter en web.xml): marca cada request como activity — alternativa a JS monitor si no controlás front-end.

**Timeout default**: configurable en BWebService y BFoxService. Típico 30-60 min.

### 11.3.4 Concurrent sessions

**Sin límite hardcoded** per user en core framework.

Realidad:
- User `admin` puede tener múltiples Workbench sessions (PCs distintos).
- Múltiples web sessions (tabs/browsers distintos).
- Mixed Workbench + web.

**Implicación audit**: 5 sessions del mismo user → 5 cambios loggeados contra el mismo BUser. Session ID en logs ayuda correlar.

**Escalabilidad**: Jetty con connection pooling maneja 1000s. Fox menos common (típicamente <100 stations).

### 11.3.5 Login screen + lockout

**Customization**: CSS + logo + favicon (ver loginScreen.txt). Module custom con resources, setea ORDs en WebService slots `logo`, `loginCss`, `favicon`.

**Branding solo visual** — HTML form es boilerplate. No CAPTCHA native. Custom requiere módulo con servlet ante-login.

**Failed login lockout** (en BUserService 11.1.6):
- `lockOutEnabled` bool.
- `maxBadLoginsBeforeLockOut` (1-10).
- `lockOutWindow` (default 30s): ventana contar fallos.
- `lockOutPeriod` (default 10s): tiempo bloqueo.

**Password policy core**: complexity enforcement NO built-in. LDAP/AD hereda política del directorio. Custom policy requiere module custom con validation regex.

### 11.3.6 Audit log

**Scope**: login/logout, privilege escalation, data modification.

**Mecanismo**: audit trail integrado con `BIHistory` + alarm/history archive (features `alarmArchive` + `audit` en license).

**Logged automáticamente**:
- BComponent property changes (set/add/remove/rename/reorder) invoked con Context+BUser.
- Method invokes (`@NiagaraRpc` o slot con `Flags.INVOKE`).
- Role assignment changes.

**NOT logged automatic**:
- Raw `BUser.login()/.logout()` — sin callback nativo.
- Failed password attempts — requiere custom servlet.

**Location**: `STATION_HOME/histories/`, rolling buffers rotated daily.

**Compliance**: export UI (date range → CSV/JSON). Documentado en SecurityBestPractices.txt — recommend regular export to Supervisor.

### 11.3.7 Enterprise Security — approach modular

**No existe módulo llamado "enterpriseSecurity"**. Niagara usa approach modular — features enterprise via add-ons.

**Módulos typically bundled en instalaciones enterprise**:

| Feature | Module | Capability |
|---------|--------|-----------|
| LDAP/AD sync | `ldap` | User mgmt centralizado, password sync TLS, cache fallback |
| Client Cert Auth | `clientCertAuth-rt/ux/wb` | Kiosk mode PKI, sin password |
| SAML SSO | Parte de ldap/security guides | IdP integration, attribute mapping |
| OAuth2 | `gauth-rt` | Federación (Google, GitHub) |
| Kerberos | `ldap` | Windows domain SSO |
| Session audit per ID | Core BUser | Auditoría per sesión |

**Password complexity**:
- Core: NO enforcement.
- LDAP/AD sync: hereda política del directorio.
- Custom: módulo custom con regex validation.

**FIPS / PCI compliance**:
- `java.security` (bin/policy, Bloque 3.2): configurable, puede desactivar weak ciphers (DES, RC4).
- TLS versioning: FoxService/WebService soportan TLS 1.2+.
- Cert pinning: NO built-in — custom module con `CertificateChainValidator` override.

**Conclusión**: Niagara N4 core = foundation segura (deny-by-default permissions, module signing del Bloque 3, activity monitoring). Enterprise features via modular ecosystem — no monolítico addon.

---

## Síntesis del bloque

### Dos sistemas de seguridad, no uno

Niagara tiene **dos sistemas de seguridad ortogonales**:

1. **Sandbox JVM** (Bloque 3): cert chain validation, Java permissions, 19 permission groups. Opera a nivel de **carga de clases** y **syscalls JVM**. Protege el station de código malicioso.

2. **RBAC BAS** (Bloque 11): BUser, BRole, BCategory, BPermissions. Opera a nivel de **BComponent + operación (read/write/invoke)**. Protege los datos de users no autorizados.

Un módulo firmado correctamente (Bloque 3) puede cargar y tener todos los Java permissions, PERO si un user sin permisos BAS intenta modificar un slot, falla con `PermissionException`.

### Modelo mental

- **User ← Role ← Permissions ← Category** es la cadena.
- **Category particiona el árbol**; user tiene permisos different per categoría.
- **Scheme decide CÓMO autenticar** (Digest/SAML/LDAP/etc.); es ortogonal a QUÉ puede hacer (roles).
- **Session lifetime** es ortogonal a autenticación — logout forzado por auto-logoff, no por scheme.
- **Audit** captura cambios automáticamente pero NO logins raw — necesitás policy explícita.

### Conexiones

- **Bloque 3 (Security JVM)**: complementario pero distinto. Bloque 3 decide si un módulo puede correr; Bloque 11 decide si un user puede usar esa funcionalidad expuesta.
- **Bloque 4 (BComponent)**: slots tienen flag `OPERATOR` (0x100) — si presente, requiere operator level, no admin. Conecta BPermissions con Flags.
- **Bloque 9 (Web)**: BWebCallbackHandler usa auth schemes para procesar HttpServletRequest. BAuthenticationService es el punto de entrada.
- **Bloque 10 (Platform)**: dos sets de credenciales (platform vs station), dos puertos (5011 vs 1911/4911).
- **Bloque 8 (Alarms)**: audit events pueden generar alarms si se configura `AlarmExt` sobre el event stream.

### Gotchas críticos

1. **Platform credentials ≠ Station credentials** — son cuentas separadas. SSO no automático entre las dos sesiones.
2. **BPermissions normalización**: ADMIN_WRITE sin ADMIN_READ es inválido. Framework lo normaliza; NO confiés en setear solo un bit.
3. **64 categorías max** per station (BCategoryMask 64-bit). Diseño cuidadoso si tenés gran árbol.
4. **Múltiples roles OR-combine**: no hay revocación fine-grained per role. Si user tiene admin role + operator role, tiene admin en todas las categorías que admin role cubre.
5. **Failed login lockout NO incluye lockout count window distinto per IP** — el lockout es per-user. Distributed attacks (múltiples IPs probando un user) NO se previenen por defecto.
6. **Password complexity NO enforced nativo** — integrar LDAP o custom si compliance requiere.
7. **SAML time skew default 3 min** — en redes con NTP mal configurado, assertions fallarán validation.
8. **Sin audit nativo de login events** — necesitás custom servlet o usar auth scheme que loggee (LDAP typically lo hace).
9. **`sameSite=None` requiere `secure=true`** (HTTPS) en browsers modernos. Si None sin HTTPS, cookie rechazada.
10. **Auto-logoff Workbench** requiere `ActivityListener` explícito en custom views no station-attached — fácil olvido.

### Qué habilita

Con Bloques 1-11 podés:
- Diseñar RBAC complejo: departamentos (categorías) × roles × users.
- Integrar SSO corporativo (SAML + AD via LDAP).
- Configurar 2FA (Google Auth) para admin accounts.
- Debuggear "por qué user X no puede hacer Y" siguiendo la cadena user→roles→permissions[categoryIdx]→bits.
- Cumplir compliance PCI/FIPS deshabilitando weak ciphers + password policy AD.

**Próximo**: Bloque 12 — Build system + module dev lifecycle.

---

## Engram topic keys

- `niagara/auth/user-role-category-permission` — BUser/BRole/BCategory/BPermissions model + evaluation flow.
- `niagara/auth/authentication-schemes` — Digest/AXDigest/Basic/Kerberos/LDAP/SAML/Cert/GoogleTOTP + comparativa.
- `niagara/auth/session-autologoff-enterprise` — lifecycle session, AutoLogoff WB+Web, concurrent sessions, lockout, audit, modular enterprise.

---

**Sesión cerrada**: 2026-04-22 — Bloque 11 consolidado.
