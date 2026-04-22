# Niagara N4 — Bloque 18: Module signing standalone + module-permissions + CSRF + Header Auth

Parte del mental model. Ver [INDEX.md](INDEX.md) para el mapa completo. Relacionado directamente con Bloques 3 (sandbox JVM), 11 (auth runtime), 17 (install filesystem + `moduleVerificationMode=low` default).

Este bloque cubre la capa **operacional de dev/ops** sobre el modelo de seguridad: cómo firmar módulos sin Workbench, qué formato tiene `module-permissions.xml` realmente, cómo funciona la protección CSRF de la web tier, el handshake HELLO + SCRAM del header auth, las annotations `@RequiresPermission` a runtime y el mecanismo de bypass explícito `exemptions.tes`.

---

## 18.1 Module signing overview

### 18.1.1 Gradle plugin `com.tridium:niagara-signing-plugin`

**Única vía standalone CLI**. No existe `nsign.exe` binario ni equivalent. Plugin version en este install: **1.0.10** (verificado en `etc/gradle/public_libraries.gradle`: `signing: "com.tridium:niagara-signing-plugin:1.0.10"`).

DSL Kotlin DSL en `build.gradle.kts`:
```kotlin
plugins {
  id("com.tridium.niagara-signing")
}
niagaraSigning {
  aliases.set(listOf("mykey"))
  signingProfileFile.set(file("profile.jceks"))
}
```

Tasks expuestas:
- `:createProfile` — genera keystore profile JCEKS
- `:generateCertificate` — genera self-signed cert dentro del profile
- `:sign` / `:signMods` — firma todos los JARs de módulos producidos
- `:verifySignatures` — valida localmente (equivalente a `nverify.exe`)

Integración automática: el plugin `com.tridium.niagara` (core) auto-wire el task `sign` como dependiente de `jar`. Build de un módulo = JAR + `.sig` sidecar generado en `build/libs/`.

### 18.1.2 Keystore formats

Supported: **JKS**, **JCEKS** (default, preferred), **PKCS12** (`.p12`/`.pfx`). El plugin usa JCEKS como default por defecto porque soporta `SecretKeyEntry` además de `PrivateKeyEntry` (útil para master keys Niagara).

Verificable en User Home real: `/home/cristian/Niagara4.14/OptimizerSupervisor/security/keystore.jceks` (14 K) es JCEKS.

Keystore password + key password por default son **distintos** en JCEKS. Niagara expone ambos via `niagaraSigning` DSL (`keystorePassword`, `keyPassword`).

### 18.1.3 `.sig` sidecar format — raw RSA-2048, NO PKCS#7

Verificación empírica sobre `bin/ext/jetty-all-compact3-9.4.54.v20240208.jar.sig`:
- Tamaño: **256 bytes exactos**
- Primer byte hexdump: `79 28 c5 22 b9 c8 d6 a5 ...` — **NO es ASN.1 SEQUENCE** (que sería `30 82` o `30 45`)
- Sin estructura PKCS#7/CMS, sin timestamp token, sin cert chain embebido

Interpretación: **raw RSA-2048 signature** (2048 bits = 256 bytes) sobre el SHA-256 del contenido del JAR. El verificador (`nverify.exe`) aplica RSA public key del cert Honeywell (obtenido de `bin/policy/signing.properties`) al `.sig` → debe matchear el hash computado del JAR.

Implicación: estándar `jarsigner` (JDK) **no sirve** — produce manifest signature + PKCS#7 block, no raw RSA sidecar. Por eso el plugin `niagara-signing` es obligatorio: implementa el formato custom.

### 18.1.4 CLI tools

| Tool | Path | Uso |
|------|------|-----|
| `nverify.exe` | `bin/nverify.exe` (517 K) | Valida un JAR + `.sig` contra el cert Honeywell. CLI standalone. |
| `keytool` | `jre/bin/keytool.exe` | Generate keypair, keystore ops. NO firma módulos (formato estándar). |
| Gradle tasks | `./gradlew sign`, `:signMods`, `:verifySignatures` | Build-integrated signing. |

**NO existe `nsign.exe`**. Todo signing corre via Gradle plugin.

### 18.1.5 `signing.properties` — cert Honeywell hardcoded

Archivo `bin/policy/signing.properties` (330 bytes, verificado):
```
#THIS FILE IS AUTO GENERATED... DO NOT MODIFY!
#Fri Jun 14 10:56:01 UTC 2024
issuerDN=CN\=Honeywell CodeSign RSA CA, OU\=ACS, O\=Honeywell International Inc., C\=US
notAfter=253370764800000
notBefore=1694029865000
serialNumber=1415098852177779243
subjectDN=C\=US, O\=Honeywell International Inc., CN\=Niagara4Modules Code Signing
```

- `notBefore` = 1694029865000 ms → 2023-09-06 UTC
- `notAfter` = 253370764800000 ms → **año 9999** (efectivamente eternal)
- `serialNumber` = 1415098852177779243 — específico de este cert Honeywell

**Integridad**: el archivo es parte del trust root. Modificarlo invalida el hash `NIAGARA4.SF` en `bin/META-INF/` → validator rechaza boot de cualquier módulo contra la nueva CA. Esto es el muro que encontró la saga httpapi 2026-04-19.

---

## 18.2 Self-signed workflow completo

Opción viable para dev local + lab, no para producción contra este install.

### 18.2.1 Generate keypair + cert

```bash
keytool -genkeypair \
  -alias mykey -keyalg RSA -keysize 2048 \
  -dname "CN=MyDev, O=LabLocal, C=AR" \
  -validity 365 \
  -keystore ~/niagaralab.jceks \
  -storetype JCEKS \
  -storepass LabPass123 -keypass LabPass123

keytool -exportcert \
  -alias mykey -keystore ~/niagaralab.jceks \
  -storetype JCEKS -storepass LabPass123 \
  -file ~/mycert.cer
```

O via Gradle plugin (more Niagara-native):
```bash
./gradlew :createProfile -PprofileName=mylab
./gradlew :generateCertificate -PprofileName=mylab -PcertCN=MyDev
```

### 18.2.2 Import cert a truststores Niagara

**Opción A (safe, recomendado)** — user-level cacerts:
```bash
keytool -importcert -alias mydev \
  -file ~/mycert.cer \
  -keystore ~/Niagara4.14/OptimizerSupervisor/security/cacerts.jceks \
  -storetype JCEKS -storepass PASSWORD_DEL_USER
```
Afecta solo al Workbench de este user.

**Opción B (rompe integridad install)** — install-level:
```bash
keytool -importcert -alias mydev -file ~/mycert.cer \
  -keystore /home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/security/truststore.jks
```
**ROMPE** `NIAGARA4.SF` en `bin/META-INF/`. Install ya no bootea hasta regenerar firma (que requiere cert Honeywell). No hacer.

### 18.2.3 Build config en user project

`build.gradle.kts` del módulo:
```kotlin
plugins {
  id("com.tridium.niagara")
  id("com.tridium.niagara-signing")
}
niagaraSigning {
  aliases.set(listOf("mykey"))
  signingProfileFile.set(file("niagaralab.jceks"))
  keystorePassword.set("LabPass123")
  keyPassword.set("LabPass123")
}
```

### 18.2.4 Verify + deploy

```bash
./gradlew :myModule:sign
./gradlew :myModule:verifySignatures   # self-validation
nverify.exe myModule-rt.jar             # cross-check external
```

Deploy via Workbench (Platform → Software Manager) o drop en `modules/` del install target. Validación corre en ModuleClassLoader en load (ver 18.3.6).

### 18.2.5 Registrar signer en user/security/signing

El user home tiene `security/signing/signers` (verificado empírico) — registro binario de signers que el Workbench ha visto. Primer load de módulo self-signed agrega entry aquí.

---

## 18.3 Bypass opciones cert Honeywell hardcoded

Matriz comparativa de opciones cuando necesitás correr módulos no-firmados-por-Honeywell:

### 18.3.1 `verificationMode=low` (default YA)

**Confirmado empírico del Bloque 17**: `defaults/system.properties` tiene `niagara.moduleVerificationMode=low` hardcoded. Este install ya arranca en modo LOW.

Comportamiento por modo (desde `ModuleClassLoader.verifyJarEntrySignature()`):

| Mode | Módulo unsigned | Self-signed cert | Cert Honeywell válido |
|------|-----------------|------------------|------------------------|
| LOW  | Warning, carga (excepto groups obligatorios) | Warning, carga | Carga sin warning |
| MEDIUM | Warning, carga con restricciones | Warning | Carga |
| HIGH | Rechaza | Rechaza | Carga |

**Pero**: si el módulo **solicita** ACCESS_CLASS, REFLECTION o MBEAN_PERMISSION, la firma es **obligatoria incluso en LOW** (ver 18.3.6 — `requiresSignature()=true` en 3 grupos). Cargo parcial.

### 18.3.2 `skipModuleValidation` license feature

Verificación empírica de las licenses del install:

```
security/licenses/Webs.license:
  <feature name="developer" expiration="2027-03-31"
           moduleDev="true" skipModuleValidation="true"/>

security/licenses/Honeywell.license:
  (NO contiene feature 'developer' — solo features operacionales Honeywell)
```

Hallazgo: el feature **sí existe en el license `Webs.license`** con `skipModuleValidation="true"`. Si el station carga la license `Webs.license` (además de `Honeywell.license`), el bypass está disponible. El check es `Feature.getb("skipModuleValidation", false)` en `ModuleClassLoader` (Bloque 3.10 match).

**Cómo activarlo**:
1. Asegurarse que `Webs.license` está instalado y válido (expires 2027-03-31).
2. Set system property: `-Dniagara.classLoader.skipModuleValidation=true` en JVM options.
3. El check runtime: `SKIP_MODULE_VALIDATION = Boolean.getBoolean("niagara.classLoader.skipModuleValidation") && Feature.getb("skipModuleValidation", false)` — **AND**. Ambos requeridos.

### 18.3.3 System property standalone `-Dniagara.classLoader.skipModuleValidation=true`

Por sí solo **NO funciona** sin la license feature (ver 18.3.2). El flag es AND con la feature, no OR.

Confirmación inversa en `module-navigator/tools/module_nav_lib/feasibility.py` (249 líneas, contiene matriz de bypass): la lógica documentada explica que `skipModuleValidation` es license-gated.

### 18.3.4 Re-firmar `bin/policy/*` files

Imposible sin el cert root Honeywell. Los 3 archivos de `bin/policy/` (`java.policy`, `java.security`, `signing.properties`) están listados en `NIAGARA4.SF` con su SHA-256. Re-generar NIAGARA4.SF requiere firma con el cert Honeywell Code Signing RSA CA — no disponible al usuario.

**Workaround teórico**: desactivar completamente el check de `NIAGARA4.SF` del launcher. Requiere modificar `niagarad.exe` nativo o deshabilitar via flag JVM no documentado. Nunca probado exitosamente.

### 18.3.5 Inyectar cert propio en truststore install

Técnicamente: `keytool -importcert` al `install/security/truststore.jks`. Pero el archivo está en el listing `NIAGARA4.SF` → invalida firma del install → launcher rechaza boot. Misma restricción que 18.3.4.

**Seguro sólo**: `user/security/cacerts.jceks` del Workbench user. Funciona para compile-time signature validation en Workbench, NO para station runtime (el daemon usa `install/security/truststore.jks`).

### 18.3.6 Matriz comparativa final

| Opción | Req. license `developer` | Efecto | Riesgo | Uso recomendado |
|--------|-------------------------|--------|--------|------------------|
| LOW verification (default) | No | Módulos unsigned cargan con warnings (excepto AC/REFL/MBean) | Bajo | Dev local sin permissions sensitivos |
| `skipModuleValidation` flag + `developer` feature | Sí (Webs.license) | Salta cert chain check completo | Medio — runtime exposed | Dev con permissions ACCESS_CLASS etc. |
| Flag solo sin license | No | **No funciona** (AND con feature) | — | — |
| Re-sign policy files | No | Imposible sin cert Honeywell | Alto — install unusable | Nunca |
| Import cert install/security | No | Install cert inconsistency | Alto — boot fail | Nunca |
| Import cert user/security | No | Workbench local only | Bajo | Dev workflow user-level |

**Para la saga httpapi**: probable escenario — el módulo declaraba ACCESS_CLASS o REFLECTION, y aunque `Webs.license` tiene el feature, el daemon target no la cargaba, o el flag system property no estaba set. La ruta correcta era: asegurar Webs.license activa + set flag en JVM options del daemon target.

---

## 18.4 `module-permissions.xml` — formato source vs runtime

### 18.4.1 Source format (dev escribe)

El dev escribe `module-permissions.xml` en `src/` del módulo. Template en `gradle/includes/niagara/module-permissions.xml.vm` (verificado en `devkit-wb.jar`):

```xml
<permissions>
  <niagara-permission-groups type="all">
    <!-- Global permissions -->
  </niagara-permission-groups>
  <niagara-permission-groups type="workbench">
    <!-- Workbench-only -->
  </niagara-permission-groups>
  <niagara-permission-groups type="station">
    <req-permission>
      <name>NETWORK_COMMUNICATION</name>
      <purposeKey>Outside access for Driver</purposeKey>
      <parameters>
        <parameter name="hosts" value="127.0.0.1"/>
        <parameter name="ports" value="*"/>
        <parameter name="type" value="all"/>
      </parameters>
    </req-permission>
  </niagara-permission-groups>
</permissions>
```

**Alto-nivel**: agrupado por `type` (all/workbench/station), cada permission usa un **group name** semántico (`NETWORK_COMMUNICATION`, `ACCESS_CLASS`, etc.), `purposeKey` explicativo, y `parameters` clave/valor.

### 18.4.2 Runtime format (inside META-INF/module.xml del JAR firmado)

**Hallazgo empírico crítico** — extraído de `modules/bacnet-rt.jar` en `META-INF/module.xml`:

```xml
<permissions>
  <java-permissions type="station">
    <java-permission action="read" class="java.io.FilePermission" name="${niagara.home}${/}defaults"/>
    <java-permission action="read,write" class="java.io.FilePermission" name="${niagara.home}${/}defaults${/}bacnetObjectTypes.xml"/>
    <java-permission class="com.tridium.nre.security.NiagaraBasicPermission" name="GET_PLATFORM_PROVIDER"/>
    <java-permission class="com.tridium.nre.security.NiagaraBasicPermission" name="RESTORE_BACKUP"/>
    <java-permission class="com.tridium.nre.security.NiagaraBasicPermission" name="SET_TIME"/>
    <java-permission class="com.tridium.nre.security.NiagaraBasicPermission" name="MANAGE_SERVER_TRUST_ANCHORS"/>
    <java-permission class="com.tridium.nre.security.KeyStorePermission" name="userKeyStore" action="read"/>
    <java-permission class="com.tridium.nre.security.NiagaraSocketPermission" name="*:1-100000" action="accept,connect,listen,resolve"/>
    <java-permission class="java.net.NetPermission" name="getNetworkInformation"/>
    <java-permission class="java.lang.RuntimePermission" name="accessDeclaredMembers"/>
    <java-permission class="java.lang.reflect.ReflectPermission" name="suppressAccessChecks"/>
    <java-permission class="java.lang.RuntimePermission" name="setContextClassLoader"/>
    <java-permission class="java.lang.RuntimePermission" name="setIO"/>
    <java-permission class="java.lang.RuntimePermission" name="modifyThread"/>
    <java-permission class="java.lang.RuntimePermission" name="modifyThreadGroup"/>
    <java-permission class="java.lang.RuntimePermission" name="shutdownHooks"/>
    <java-permission class="java.lang.RuntimePermission" name="getenv.JETTY_AVAILABLE_PROCESSORS"/>
  </java-permissions>
  <java-permissions type="all">
    <java-permission action="read" class="java.util.PropertyPermission" name="*"/>
    <java-permission class="java.util.logging.LoggingPermission" name="control"/>
  </java-permissions>
  <java-permissions type="workbench">
    <java-permission class="java.lang.RuntimePermission" name="getenv.*"/>
    <java-permission class="java.lang.RuntimePermission" name="exitVM.*"/>
    <java-permission class="java.lang.RuntimePermission" name="accessClassInPackage.sun.util.logging.resources"/>
    <java-permission action="connect,resolve" class="com.tridium.nre.security.NiagaraSocketPermission" name="*:1-100000"/>
    <java-permission class="java.awt.AWTPermission" name="accessClipboard"/>
    <java-permission class="java.awt.AWTPermission" name="showWindowWithoutWarningBanner"/>
  </java-permissions>
</permissions>
```

**Bajo-nivel**: clases Java directas (`FilePermission`, `RuntimePermission`, etc.) con action + target/name. Sin groups, sin purposeKey. Esto es lo que el `SecurityManager.checkPermission()` consume.

### 18.4.3 Transformación Source → Runtime

La transformación **SÍ cambia el formato** (confirmado empírico, sub-agent previo reportó erróneamente que "persiste sin cambios"). Ocurre en:

**Gradle task**: `slotomatic` + signing plugin. La clase `NiagaraPermissionGroupFactory` en `com.tridium.gradle.plugins.niagara-signing` toma cada `<req-permission>` del source y expande a los `<java-permission>` correspondientes vía metadata de la clase del group.

**Ejemplo de expansión** `NETWORK_COMMUNICATION` con `hosts=*, ports=80,443`:
- → `<java-permission class="com.tridium.nre.security.NiagaraSocketPermission" name="*:80,443" action="connect,resolve"/>`
- → `<java-permission class="java.net.URLPermission" name="http://*:80/-" action="*:*"/>`

Los groups actúan como **macros**: el dev escribe lo intent-high-level, el build expande a los Java permissions necesarios. Esto es el "runtime transformation" del Bloque 3.3.

### 18.4.4 Permission groups registrados

**Corrección al Bloque 3**: el Bloque 3.4 enumeró 19 groups, pero `NiagaraPermissionGroupFactory` registra **~25 groups** en N4.14. Los nombres reales verificados desde la factory:

| Group | Requiere firma | Parámetros clave |
|-------|---------------|------------------|
| NETWORK_COMMUNICATION | No | hosts, ports, type, proxySelector, SSLSockets |
| MANAGE_EXECUTION | No | — |
| SYSTEM_PROPERTIES | No | properties, actions |
| LOGGING | No | — |
| MODIFY_IO_STREAMS | No | — |
| **ACCESS_CLASS** | **Sí (siempre)** | packages |
| GET_ENVIRONMENT_VARIABLES | No | — |
| SHUTDOWN_HOOKS | No | — |
| LOAD_LIBRARIES | No | libraries |
| RUNTIME_EXECUTION | No | files |
| KEY_STORE | No | — |
| **REFLECTION** | **Sí (siempre)** | — |
| **MBEAN_PERMISSION** | **Sí (siempre)** | type, actions, className |
| AUTHENTICATION | No | — |
| UI | No | — |
| SIGNING | No | — |
| BACKUPS | No | actions |
| DIAGNOSTICS | No | — |
| SET_SYSTEM_TIME | No | — |
| THIRD_PARTY_PERMISSION | No | class, name, actions |
| MANAGE_SERVER_TRUST_ANCHORS | No | — |
| CLOUD_BEARER_TOKEN | No | — |
| PROTECTION_DOMAIN | No | — |
| (y otros adicionales per-version) | | |

**Los 3 que siempre requieren firma** (`requiresSignature()=true`, incluso en LOW mode):
1. **ACCESS_CLASS** — expone clases internas JDK (`sun.misc`, `sun.reflect`) vía reflection
2. **REFLECTION** — `ReflectPermission("suppressAccessChecks")`, bypass de access modifiers
3. **MBEAN_PERMISSION** — JMX operations con poder de modificar runtime

**Corrección al Bloque 3.4**: los nombres "BINDING" y "PRIVILEGE" del Bloque 3.4 NO existen como groups en N4.14. Eran referencias erróneas. Los groups que sí siempre requieren firma son los 3 listados arriba.

### 18.4.5 Validation en ClassLoader load

`com.tridium.nre.module.ModuleClassLoader.verifyJarEntrySignature()` (método principal de validation, L374-430 según decompilado corpus):

Flow por JarEntry (`.class` o resource):
1. Si es `META-INF/` → skip (directorio control)
2. Determinar modo: `Nre.getModuleVerificationMode()` → LOW/MEDIUM/HIGH
3. `shouldCheckTpk = name.startsWith("com/tridium/") || module.getCheckTpk()` (TPK = Tridium Public Key)
4. `verificationRequired = (mode != LOW) || (shouldCheckTpk && canCheckTpk)`
5. `entry.getCodeSigners()` empty + required → `ValidationException`
6. Para cada CodeSigner → `CoreCryptoManager.validateCertChain(signer, shouldCheckTpk)`
7. Chain termina en cert Honeywell (según `signing.properties`) → OK
8. Otherwise → `ValidationException`, class no carga
9. Define class en `ProtectionDomain` con codeSource + certs

**Signature requirement check separate** (L88-96):
```java
boolean requiresSignature = false;
if (module.hasRequestedPermissions()) {
  requiresSignature = module.getRequestedNiagaraPermissions()
    .stream().anyMatch(NiagaraPermissionGroup::requiresSignature);
}
this.validateCertChain = requiresSignature && !SKIP_MODULE_VALIDATION;
```

Donde `SKIP_MODULE_VALIDATION = Boolean.getBoolean("niagara.classLoader.skipModuleValidation") && Feature.getb("skipModuleValidation", false)` — **AND** (flag + license feature).

### 18.4.6 Interacción con `bin/policy/java.policy`

Dos capas independientes:

**Policy file** (`bin/policy/java.policy`) — granular por `codeBase`:
```
grant codeBase "file:${niagara.home}/bin/ext/nre.jar" {
  permission java.util.PropertyPermission "java.home", "read";
  permission java.lang.RuntimePermission "getProtectionDomain";
  ...
};
```

Aplica a: executables de `bin/` + JARs del sistema core (nre, niagarad). NO a módulos en `modules/`.

**module.xml `<java-permissions>`** — per-module ProtectionDomain. Aplica a los módulos en `modules/`.

**Intersection, NO union**: SecurityManager evalúa ambos. Permiso denegado en cualquiera de los dos → operación rechazada.

### 18.4.7 Profile scoping (`type=station|workbench|all`)

Crítico. Cada permission es válido solo en el perfil declarado:
- `type="station"` — solo NRE/daemon
- `type="workbench"` — solo Workbench
- `type="all"` — ambos

Implicación: un driver declarado con `AWTPermission` en `type="all"` fallaría en station (no hay desktop). Por eso `bacnet-rt` declara `AWTPermission` solo en `type="workbench"` (para tools de diagnóstico UI, no para el driver mismo).

---

## 18.5 CSRF protection

### 18.5.1 Token lifecycle

**Clase responsable**: `javax.baja.web.CsrfUtil` + filter `javax.baja.web.filters.CsrfProtectedFilter` en `web-rt.jar`.

**Token name**: default `csrfToken` (configurable). Header HTTP: `x-niagara-csrfToken`.

**Generación**: session-scoped, probablemente random bytes base64url-encoded de ~128-256 bits (no verificable sin decompilar bytecode nativo — solo signatures en javap). El token se crea al establecer la session HTTP (primer login) y vive junto al `JSESSIONID`.

**Entrega al cliente**: embedded en HTML de login como `<input id="csrfToken" value="...">`. El client extrae vía regex:
```
<input [^<>]*id=['"]csrfToken['"][^<>]*>   → match
value=['"]([^"']*)['"]                      → captura
```

**Re-entrega en request**:
- Header: `x-niagara-csrfToken: <token>` (method preferred)
- Query param: `?csrfToken=<token>` (GET logout)
- Form field: `csrfToken=<token>` (POST form)

### 18.5.2 Validation filter

`CsrfProtectedFilter` intercepta requests ante de servlet. Lanza `CsrfException` con key `csrf.token.verify.error` si falla. Typical responses:
- Token missing → 403 Forbidden
- Token mismatch → 403 Forbidden
- Session expirada → 401 Unauthorized

**Bypass endpoints** (excluidos de CSRF check):
- Login endpoints (primer handshake — no hay session aún)
- Endpoints marcados `@AllowUnauthenticated`
- GET idempotentes (configuración típica — no confirmado empírico)

**No bypass system property conocido**. No existe `niagara.web.csrf.disabled` en el corpus. Solo via config de filter en web.xml del módulo `web-rt` — modificarlo rompe NIAGARA4.SF.

### 18.5.3 Ejemplo HTTP real

```http
POST /ord/station:%7Cslot:/devices/HVAC1/setPoint HTTP/1.1
Host: station.local:443
Cookie: JSESSIONID=ABC123
x-niagara-csrfToken: dGVzdHRva2VuYmFzZTY0

action=setValue&value=72.5
```

Server valida cookie session → lookup CSRF token stored → compare con header → if match ejecuta; if mismatch 403.

---

## 18.6 Header-based authentication

### 18.6.1 `BHttpHeaderCallbackHandler` HELLO + SCRAM flow

Clase: `javax.baja.web.authn.BHttpHeaderCallbackHandler` (abstract) en `web-rt.jar`. Implementación concreta: `com.tridium.web.authn.BHttpCallbackHandler`.

Flow multi-step **HELLO + SCRAM-SHA-256** (RFC 5802 style):

**Paso 1 — HELLO (client initiates)**:
```http
GET /login HTTP/1.1
Authorization: HELLO username=dXNlcg==      (base64url del username)
```

**Paso 2 — Server challenges**:
```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: SCRAM hash=SHA-256, handshakeToken=abc123
```

**Paso 3 — Client first message (SCRAM)**:
```http
Authorization: SCRAM handshakeToken=abc123, data=<base64url(clientFirstMessage)>
```
`clientFirstMessage = "n,,n=<user>,r=<client-nonce>"` (RFC 5802)

**Paso 4 — Server first message**:
```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: SCRAM handshakeToken=abc123, data=<base64url(serverFirstMessage)>
```
`serverFirstMessage = "r=<client-nonce+server-nonce>,s=<salt>,i=<iterations>"`

**Paso 5 — Client final message**:
```http
Authorization: SCRAM handshakeToken=abc123, data=<base64url(clientFinalMessage)>
```
`clientFinalMessage = "c=biws,r=<full-nonce>,p=<client-proof>"`

**Paso 6 — Server success + authToken**:
```http
HTTP/1.1 200 OK
Authentication-Info: authToken=SESSION_ID, hash=SHA-256, data=<server-proof>
```

**Paso 7 — Subsequent requests con BEARER**:
```http
Authorization: BEARER authToken=SESSION_ID
```

**Stateful**: tras handshake, el `authToken` es session id. Server caches y valida en cada request.

### 18.6.2 Use cases

1. **Reverse proxy pre-auth**: proxy hace HELLO/SCRAM una vez → cache `authToken` → inject en `Authorization: BEARER` para requests downstream
2. **API keys**: subclass custom que parsea `X-API-Key` → mapea a Niagara user
3. **SAML/OAuth SSO**: federation flow previo → token bridging a BHttpHeaderCallbackHandler
4. **M2M IoT**: device establishes session una vez → BEARER en todos los requests durante runtime

---

## 18.7 Runtime permission annotations

### 18.7.1 Annotation base

Niagara soporta annotations complementarias a `module-permissions.xml`:

- `@NiagaraType` — declara BComponent (Bloque 4)
- `@NiagaraAction(permissions="...")` — declara action method + permissions at call-site
- `@RequiresPermission("...")` — explicit permission check
- `@AllowUnauthenticated` — marca endpoint que NO requiere auth (CSRF bypass etc.)
- `@RequiresLicense("featureName")` — gated por feature en license

Ejemplo uso inferido del patrón:
```java
@NiagaraAction(permissions="NETWORK_COMMUNICATION:hosts=*,ports=8080")
public void connectToDevice() { ... }

@RequiresPermission("FILE_ACCESS")
public void readConfigFile(String path) { ... }
```

### 18.7.2 Processing

**Compile time**: annotation processors registrados en `META-INF/services`, ejecutados por `slotomatic` durante Gradle build. Generan metadata embebida en el módulo (típicamente adentro de slotomatic-generated code).

**Runtime**: SecurityManager intercept via:
- **Bytecode-generated checks**: slotomatic inyecta `AccessController.checkPermission()` antes del método annotated
- **Proxy classes**: para BComponent actions, proxy wrappa + checks

**Exception flow**: `java.security.AccessControlException` → wrapper `com.tridium.nre.security.NiagaraSecurityException` → HTTP 403 en web tier, dialog en Workbench.

### 18.7.3 Diferencia con `module-permissions.xml`

| Aspecto | module-permissions.xml | @Annotations |
|---------|------------------------|--------------|
| Scope | Module-wide ProtectionDomain | Call-site specific |
| Granularity | Coarse (groups) | Fine (per-method) |
| Evaluation | Load time (ClassLoader) | Runtime (call) |
| Override | No (hardcoded in JAR) | Possible via AOP |
| Use case | "This module needs FILE access" | "This specific method needs FILE read on /tmp/*" |

Complementarios: module-permissions.xml provee la capacidad base; annotations refuerzan en callsites sensitivos.

---

## 18.8 Credential storage — SCRAM-SHA256

### 18.8.1 Storage en `config.bog`

El componente `BUser` (Bloque 11.1) serializa en `config.bog` de la station. Password **nunca plaintext**. Campos persistidos (SCRAM-SHA-256 derivation per RFC 5802):

- `salt` — 16 bytes aleatorio (base64 en BOG)
- `iterationCount` — típicamente 4096 (PBKDF2)
- `storedKey = SHA-256(clientKey)`
- `serverKey = HMAC-SHA-256(saltedPassword, "Server Key")`

Donde `saltedPassword = PBKDF2WithHmacSHA256(password, salt, iterationCount)` y `clientKey = HMAC-SHA-256(saltedPassword, "Client Key")`.

Ventaja: el server **nunca** ve el password plaintext, ni siquiera en primer login. El client-side SCRAM verifica conocimiento sin transmitir.

### 18.8.2 Change flow

1. Workbench UI → user modifica password
2. RPC: `BUser.setPassword(newPassword)` llamada remota (Bloque 13.3.1)
3. Server (signed RPC): genera nuevo salt aleatorio, re-computa PBKDF2 + stored/server keys
4. Persist en `config.bog`, save atomic (Bloque 5.2)
5. Next login: SCRAM handshake usa nuevos salt + iterations

### 18.8.3 Password complexity

**Bloque 11.3.5 confirmado**: NO hay enforcement nativo (length, chars, history). Override via subclass `BAuthenticationScheme` custom — hook API existe pero undocumented en user-facing docs.

LDAP/AD hereda complexity del directorio. Kerberos idem.

---

## 18.9 `exemptions.tes` — TES bypass explícito

**Path**: `user/security/exemptions.tes` (16 K verificado en User Home real).

**Formato**: binario (NO plaintext XML), probable Java serialization o custom TLV. Nombre TES = "Trustability Exemption Service".

**Qué exime**:
- Módulos específicos por name (bypass signature validation)
- Classes FQN específicas (bypass reflection permission)
- Cert fingerprints (bypass revocation check)

**Workflow creación**:
1. User instala módulo unsigned / cert no-trusted
2. Workbench detecta, prompts dialog: "Module XYZ is not validly signed. Trust permanently?"
3. User accepta → entry agregado a `exemptions.tes`, persisted
4. Next startup: módulo carga sin validation check

**Security implication**: es la **puerta trasera oficial**. Permite bypass de code signing sin modificar install. Mitigaciones:
- Requiere autenticación admin para modificar
- Audit log entry cada vez que se agrega exemption
- User-specific (no afecta daemon) — user-level exemptions solo afectan al Workbench de este user

Para la saga httpapi: si hubiera existido workflow admin en Workbench, bastaba con agregar el módulo custom a `exemptions.tes` del user deployer. Alternativa menos invasiva que los bypass JVM flags.

---

## 18.10 Hallazgos críticos del bloque

1. **`signing.properties` hardcoded Honeywell CA** (CN=Honeywell CodeSign RSA CA + serial 1415098852177779243, valid hasta año 9999). Modificarlo invalida `NIAGARA4.SF` → install no bootea. Es el muro de la saga httpapi 2026-04-19.

2. **`.sig` = 256 bytes raw RSA-2048**, NO PKCS#7. Primer byte `79 28 c5 22` ≠ ASN.1 SEQUENCE. `jarsigner` estándar JDK NO sirve — formato custom solo producible con `com.tridium:niagara-signing-plugin:1.0.10`.

3. **Plugin Gradle `com.tridium:niagara-signing-plugin` versión 1.0.10** — única vía standalone de firma. No existe `nsign.exe`. `nverify.exe` (517 K) es la única CLI de validación externa.

4. **`Webs.license` SÍ contiene `<feature name="developer" skipModuleValidation="true">`** (expiration 2027-03-31). `Honeywell.license` NO. Si Webs.license está cargada en el station target, el bypass `skipModuleValidation` está disponible (flag + feature = AND).

5. **Source format vs Runtime format difieren** — el dev escribe `<niagara-permission-groups>/<req-permission>/<name>` high-level; el build transforma a `<java-permissions>/<java-permission class="..." name="..." action="...">` low-level con clases Java concretas (FilePermission, RuntimePermission, NiagaraSocketPermission, NiagaraBasicPermission, KeyStorePermission).

6. **3 permission groups siempre requieren firma** (incluso en `verificationMode=LOW`): ACCESS_CLASS, REFLECTION, MBEAN_PERMISSION. Corrige al Bloque 3.4 que listaba "BINDING" y "PRIVILEGE" — esos nombres no existen en N4.14.

7. **Permission type scoping** (`type="station"` / `workbench"` / `all"`) — per profile. Un módulo puede pedir AWTPermission solo en Workbench (diagnostic UI) sin pedirlo en station.

8. **`NiagaraBasicPermission` custom** cubre actions Niagara-específicas: `GET_PLATFORM_PROVIDER`, `RESTORE_BACKUP`, `SET_TIME`, `MANAGE_SERVER_TRUST_ANCHORS`. `NiagaraSocketPermission` extiende SocketPermission con `accept,connect,listen,resolve` en rangos `host:port-port`.

9. **CSRF** — token `x-niagara-csrfToken` embedded en HTML login + session-scoped. `CsrfProtectedFilter` intercepta POST/PUT/DELETE. No system property bypass.

10. **HELLO + SCRAM-SHA-256** — 6-step handshake (RFC 5802 style) via headers `Authorization: HELLO` → `SCRAM` → `BEARER authToken=...`. Server nunca ve password plaintext. `BHttpHeaderCallbackHandler` orquesta.

11. **`exemptions.tes`** binary file en user security — puerta trasera oficial user-level. Bypass module validation via Workbench dialog → persisted → subsequent loads skip validation. Menos invasivo que JVM flags para dev local.

12. **`ModuleClassLoader.verifyJarEntrySignature()`** flow completo documentado (L374-430 corpus). `SKIP_MODULE_VALIDATION = flag AND feature` — AND, no OR. Ambos requeridos para bypass.

---

## 18.11 Conexiones con otros bloques

- **Bloque 3 (Security sandbox)**: Este bloque corrige 3.4 (no existen "BINDING" / "PRIVILEGE" groups) y expande 3.3 (formato real runtime de permissions.xml en module.xml). Confirma 3.2 (PKCS7-signed policy files inmutables). Actualiza 3.9 (`nverify.exe` uso), 3.10 (bypass flag + feature AND).
- **Bloque 9.3.6 (CSRF)**: profundiza el header `x-niagara-csrfToken` mencionado + validation filter clase concreta.
- **Bloque 11.3.2 (BHttpHeaderCallbackHandler)**: documenta el HELLO + SCRAM flow completo.
- **Bloque 11.3.5 (password complexity)**: confirma no enforcement nativo + SCRAM-SHA-256 storage.
- **Bloque 12.1 (Gradle)**: expande `com.tridium.niagara-signing` plugin version 1.0.10 + tasks.
- **Bloque 17.6 (`moduleVerificationMode=low` default)**: este bloque explica qué hace LOW vs HIGH y los 3 groups que lo sobreescriben.

---

## Engram topic keys

- `niagara/security/module-signing-standalone-gradle-plugin`
- `niagara/security/module-permissions-xml-source-runtime`
- `niagara/security/csrf-header-auth-annotations-exemptions`
