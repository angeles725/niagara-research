# Niagara N4 — Mental Model

**Proyecto**: `niagara-research`
**Sesión**: 2026-04-19/20
**Distribución analizada**: Honeywell OptimizerSupervisor N4.14.0.162 (`C:\Honeywell\OptimizerSupervisor-N4.14.0.162`)
**HostId**: `Win-6E6E-10AC-D1DD-8276`
**Corpus**: 926 JARs, 51,167 clases decompiladas (Vineflower 1.11.1), 126 carpetas de docs, 20 topic keys en engram (project `niagara-research`).

Este documento consolida el mental model del framework Niagara N4 en 3 ejes — Estructura, Licensing, Security — más una sección de evidencia empírica decompilada y las implicancias prácticas derivadas de la sesión httpapi previa.

Cada sección linkea a los **topic keys de engram** para el detalle completo. Buscá con `mem_search "topic-key-name"` y `mem_get_observation`.

---

## Índice

1. [Contexto y motivación](#1-contexto-y-motivación)
2. [Estructura del framework](#2-estructura-del-framework)
3. [Licensing](#3-licensing)
4. [Security](#4-security)
5. [Validaciones decompiladas](#5-validaciones-decompiladas)
6. [Implicancias prácticas para el caso httpapi](#6-implicancias-prácticas-para-el-caso-httpapi)
7. [Referencias rápidas](#7-referencias-rápidas)

---

## 1. Contexto y motivación

La sesión previa (2026-04-19, project `httpapi`) consumió 4 horas peleando contra un `AccessControlException: SocketPermission` al intentar POST desde un fork de `httpClient` Tridium en esta distribución Honeywell OEM. Al final de la sesión, 5 discoveries clave quedaron en engram (project `httpapi`):

- `niagara/honeywell-oem-signing-lockdown` (#302) — `signing.properties` hardcodea el único trust anchor (Honeywell CodeSign RSA CA)
- `niagara/honeywell-policy-integrity-check` (#303) — `java.policy` y `java.security` PKCS7-signed, modification breaks boot
- `niagara/httpclient-permission-model` (#301) — `<permissions>` declaration triggers strict cert check independent of `moduleVerificationMode=low`
- `niagara/cert-chain-validation-broken` (#300) — `angelesCA` wrongly configured as CA instead of code-signing leaf
- `session/2026-04-19-httpclient-saga-closure` (#305) — full rollback + 4 remaining options (B/C/D/E)

La conclusión de esa sesión fue pragmática: **no se puede granted `SocketPermission` a módulos de terceros con cert propio en esta distribución Honeywell OEM**. Antes de seguir intentando hacks, este workspace construye el mental model del framework para tomar decisiones informadas.

---

## 2. Estructura del framework

### 2.1 Programas y protocolos

Un sistema Niagara N4 se compone de **4 procesos**:

| Proceso | Executable | Rol |
|---------|-----------|-----|
| **Station** | `bin/station.exe` | JVM con la component application (`config.bog`) |
| **Workbench** | `bin/wb.exe` / `bin/wb_w.exe` | JVM con plugin-based UI desktop |
| **Daemon** (niagarad) | `bin/niagarad.exe` (Windows Service) | Boot de stations, platform config (IP), install/backup, license management |
| **Web Browser** | external | HTTP/HTTPS a la station |

Y **3 protocolos de red**:

- **Fox** (TCP **1911**) + **FoxS** (TLS **4911**): station↔station y workbench↔station. Propietario Tridium.
- **HTTP/HTTPS**: browser↔station.
- **Niagarad protocol**: workbench↔daemon.

Platforms:
- **JACE** (Java Application Control Engine): embedded QNX, station + daemon, sin workbench.
- **Supervisor**: station en server class, puede tener los 3 procesos.
- **Client**: desktop workbench o browser.

Detalle completo: `mem_search niagara/estructura/arquitectura-general`

### 2.2 Module system

Unit of deployment = JAR con `META-INF/module.xml`. Naming convention: `{module}-{profile}.jar` (e.g. `alarm-rt.jar`).

**5 runtime profiles** (no 3):

| Profile | JRE | Uso |
|---------|-----|-----|
| `rt` | Java 8 Compact 3 | Data model + comm (Fox, Web Servlets) |
| `ux` | Java 8 Compact 3 | BajaUX, HTML5, JS (web UI) |
| `wb` | Java 8 SE | Workbench Swing UI (legacy) |
| `se` | Java 8 SE | Direct Java SE deps (DB, Swing) |
| `doc` | N/A | Documentación pura |

**Dependency matrix**: `rt→rt` solo, `ux→rt,ux`, `wb→rt,ux,wb`, `se→todo`, `doc→nada`.

**Convención de packages**: `javax.baja.*` = API público, `com.tridium.*` = impl interna.

Detalle completo: `mem_search niagara/estructura/module-system`

### 2.3 `module.xml` root attributes

Required: `name, vendor, vendorVersion, description, preferredSymbol, runtimeProfile`.
Optional: `modulePartName, nre, autoload, installable, buildMillis, moduleName, releaseDate`.
Sub-elements: `moduleParts, dependencies, dirs, defs, types, lexicons, permissions, installation`.

### 2.4 Registry (`sys.registry`)

Database lightweight que indexa todos los types/agents/defs/lexicons sin cargar classes en memoria. Live at `{niagara.user.home}/registry/` — files `registry.chk` (checkpoint) + `registry.db`.

Entry point: `Sys.getRegistry()`.
Wrapper pattern: `ModuleInfo`↔`BModule`, `TypeInfo`↔`Type`.
Spy page runtime: `local:|spy:/sysManagers/registryManager`.

**Agents** son late-binding plugins registered via `module.xml`:
```xml
<type name="PropertySheet" class="com.tridium.workbench.propsheet.BPropertySheet">
  <agent requiredPermissions="r"><on type="baja:Component"/></agent>
</type>
```

Regeneration automática al detectar changes en `modules/`. Log: `"Loaded"` = parseó, `"up-to-date"` = cache hit.

Detalle completo: `mem_search niagara/estructura/registry`

### 2.5 Station bootstrap (6 fases)

1. **Load**: deserializa `config.bog` → `BStation`, mount en `local:|station:`
2. **Service Registration**: framework registra todos los services (`BIService`)
3. **Service Initialization**: callback `serviceStarted()` por service
4. **Component Start**: `BComponent.start()` → `started()` + `descendentsStarted()`
5. **Station Started**: callback `stationStarted()` — external comm debe esperar hasta acá
6. **Steady State**: timer (`niagara.steadystate=10000` ms default) + `atSteadyState()` callback

Shutdown: Save (bog) → Component Stop → Service Stop.

Station runtime en `C:\ProgramData\Niagara4.14\OptimizerSupervisor\stations\{name}\config.bog`. Este install tiene 2 stations: `HoneywellMX60`, `PRUEBAS`.

Detalle completo: `mem_search niagara/estructura/station-bootstrap`

### 2.6 Fox protocol

Multiplexed peer-to-peer sobre single TCP socket.

- **Ports**: 1911 TCP / 4911 FoxS TLS
- **Multicast discovery**: IPv4 `224.0.1.84`, IPv6 `FF02::137`, TTL 4 hops
- **Features**: digest auth, request/response + async eventing, streaming, channel multiplexing, text-based framing
- **Config**: 20+ props `niagara.fox.*` en `defaults/system.properties`
- **Implementation**: `modules/fox-rt.jar`, classes `com.tridium.fox.sys.*`
- **Ord schemes**: `fox:` / `foxs:`

**Módulo `fox-rt` NO declara `<permissions>`** — depende de `net-rt` que sí declara `NiagaraSocketPermission *:1-100000`. Los permissions NO se heredan por module dependency (cada ProtectionDomain es independiente por JAR), entonces modules de terceros con sockets propios necesitan declarar permissions explícitas.

Detalle completo: `mem_search niagara/estructura/fox-protocol`

---

## 3. Licensing

### 3.1 Modelo de 5 elementos

1. **HostId**: String único que identifica UNA máquina (`Win-6E6E-10AC-D1DD-8276` aquí). Check: `nre -version`.
2. **Certificate**: `.certificate` XML file mapeando `vendor id → public key`. En `{niagara.home}/security/certificates/`. DSA-signed, 2006 vintage, "never expire".
3. **License File**: `.license` XML file con features. HostId-bound. En `{niagara.home}/security/licenses/`.
4. **Feature**: `<feature name="X" prop1="Y">` elements. Key = (vendor, name).
5. **API**: `javax.baja.license` + `Sys.getLicenseManager()`.

**Validation flow** (5 checks al boot):
1. `hostId` match
2. `expiration` ≥ now
3. `generated` ≤ now
4. `vendor` tiene cert correspondiente
5. `signature` verify contra public key

Detalle completo: `mem_search niagara/licensing/model-overview`

### 3.2 Honeywell OEM layout — 3 vendors paralelos

Esta distribución tiene **3 license files paralelos** + **3 cert files**:

| License | Vendor | Features | Expiración |
|---------|--------|----------|------------|
| `Webs.license` (16 KB) | Tridium | 150+ core + drivers (brand="Webs") | 2027-03-31 |
| `Honeywell.license` (2.3 KB) | Honeywell | 27 OEM (spyder, honConnectedPower, redLink, etc.) | 2027-03-31 |
| `HoneywellCentraLine.license` (366 B) | HoneywellCentraLine | 1 (clCbus) | 2027-03-31 |

El `brand` feature en `Webs.license` con `brandId="Webs"` marca el install como brand "Webs" (internal name Honeywell, históricamente "Web-Enabled Building Service"). Accept lists `"*"` permiten comms con cualquier brand externo.

Detalle completo: `mem_search niagara/licensing/honeywell-oem-overlay`

### 3.3 License file format XML

```xml
<license version="4.15" vendor="Tridium"
         generated="2026-04-02" expiration="2027-03-31"
         hostId="Win-6E6E-10AC-D1DD-8276">
  <feature name="station" expiration="2027-03-31" station.limit="128" guestEnabled="true"/>
  <feature name="http" expiration="2027-03-31" sma.exempt="true" ...>
  ...
  <signature>MCwCFBa0lpgA...</signature>  <!-- DSA base64 -->
</license>
```

Feature properties son free-form key/value — el módulo que implementa el feature decide cómo interpretarlos. Common patterns:
- `*.limit="none"` = unlimited
- `camera.limit=32` = hard cap
- `sma.exempt="true"` = no requiere SMA activa
- `edgeLite1_*` = tiered JACE EdgeLite params

Detalle completo: `mem_search niagara/licensing/license-file-format`

### 3.4 SMA (Software Maintenance Agreement)

Tracked **per-feature** via attr `sma.exempt="true|false"`. En `Webs.license`:
- `http` (sma.exempt=true), `jsonToolkit` (sma.exempt=true) — **no requieren SMA**
- `cloudLink` (sma.exempt=false) — sí requiere SMA
- La mayoría sin el attr → default requires SMA

**Monitor class**: `com.tridium.httpClient.util.BSMAExpirationMonitor` (423 líneas en httpClient-rt). Es un `BComponent` con properties `exempt, mode, warnBelow=30days, checkInterval=1day, remaining`. Al start, lee `LicenseUtil.isSmaExempt()` y si TRUE → `reportOk()` sin validar fechas.

Runtime API:
```java
Sys.getLicenseManager().checkFeature("Tridium", "station");  // throws LicenseException si missing
Feature f = Sys.getLicenseManager().getFeature("Honeywell", "maxproVideo");
int cameras = Integer.parseInt(f.get("camera.limit"));
```

Detalle completo: `mem_search niagara/licensing/features-and-properties`

### 3.5 Cloud / subscription / signing service

Licenses cloud-related: `cloudBackupService, cloudIotHubConnector, cloudLink, cloudSentienceConnector, gcpGatewaySup, nCloudDriver`. Multi-cloud (Azure IoT Hub + GCP + Honeywell Sentience). Prop enable: `niagara.license.subscriptionLicenseAllowed=true` en `defaults/system.properties`.

Features `certSigningService` + `bulkCertSigner` permiten a la Supervisor actuar como CA interno para nodes edge.

Detalle completo: `mem_search niagara/licensing/subscription-and-cloud`

---

## 4. Security

### 4.1 Java Security Manager runtime model

Niagara 4 activó el Java Security Manager. **Por default NINGÚN módulo tiene permissions** — cada operación requiere grant explícito.

- En 4.0-4.1: static policy files en `{niagara.home}/security/policy`
- En 4.2+: policy determinada por `<permissions>` block en cada `module.xml` runtime

**Debug tools**:
- Logger `security.niagaraPolicy` con level `FINE` (failed checks), `FINER` (quien debería granted), `FINEST` (succeed + fail)
- JVM arg: `-Djava.security.debug=access,failure`
- Policy Spy: `Spy > securityInfo > Policy Spy`

**Disable requirement**: license feature `smDeveloperMode` (presente en tu Webs.license línea 129) + system prop `niagara.security.manager.disable` (o QNX `touch /etc/no-security-manager`).

Detalle completo: `mem_search niagara/security/java-security-manager-model`

### 4.2 Permission groups — 19+ total

Tabla en engram bajo `niagara/security/permission-groups-table`. Los que **requieren module signed** (módulo unsigned halts el station/workbench):
- `ACCESS_CLASS` (since 4.9)
- `HSM_SIGNING`
- `MBEAN_PERMISSION` (since 4.9)
- `REFLECTION`
- `THIRD_PARTY_PERMISSION` (since 4.9)

**NETWORK_COMMUNICATION** (el relevante para httpapi): parámetros `hosts, ports, type` (client/server/all), `proxySelector, SSLSockets, getNetworkInformation`. Expande a 5 permissions por cada host:port combo + opcional NetPermission + CryptoServicesPermission.

### 4.3 `module-permissions.xml` → `<permissions>` flow

**2 formatos distintos**:

**Source** (`module-permissions.xml`, high-level, en source project):
```xml
<permissions>
  <niagara-permission-groups type="station|workbench|all">
    <req-permission>
      <name>NETWORK_COMMUNICATION</name>
      <purposeKey>lexKey.purpose</purposeKey>
      <parameters>
        <parameter name="hosts" value="*"/>
        <parameter name="ports" value="443"/>
      </parameters>
    </req-permission>
  </niagara-permission-groups>
</permissions>
```

**Runtime** (`<permissions>` dentro de `META-INF/module.xml` del JAR, low-level):
```xml
<permissions>
  <java-permissions type="station|workbench|all">
    <java-permission class="com.tridium.nre.security.NiagaraSocketPermission"
                     name="host:port" action="connect,resolve"/>
    <java-permission class="java.net.URLPermission" name="https://..." action="*:*"/>
  </java-permissions>
</permissions>
```

Transformer: gradle plugin `niagara-signing` al build. Classes del mapping están en el módulo `baja` package `com.tridium.security`.

Detalle completo: `mem_search niagara/security/module-permissions-flow`

### 4.4 User/Role/Category model — permissions de USUARIO

Modelo separado del module JSM permissions. Aplica a BComponents, BIFiles, BIHistories.

- `BUser` + `BUserService` + `BAuthenticationService` (Fox/HTTP auth)
- `BCategoryMask` (variable-length bit string, hex display: `"a"` = bits 2+4, `"*"` = wildcard, `""` = null)
- `BPermissions` bitmask: 6 permissions (operator/admin × read/write/invoke)
- `BPermissionsMap` (per user role, per category → permissions)
- `SUPER_USER` = all perms
- Fox auto-filtra data SERVER-SIDE según user perms (sin permission nunca VE data sensible)
- Workbench views declaran `requiredPermissions="r"` en module manifest

Detalle completo: `mem_search niagara/security/user-category-model`

### 4.5 Signing profile flow

Code signing de modules via gradle plugin `niagara-signing`:

- **Default profile** (4.6+): `USER_HOME/.tridium/security/niagara.signing.{jks,xml}`, alias `Niagara4Modules`, self-signed
- **Custom profile**: `gradlew :createProfile --profile-path X.xml`
- **Profile types**: `RestrictedSigningProfile` (default), `JarSignerSigningProfile` (HSM since 4.14)
- **Timestamping**: `niagara.signing.standardtsa=http://timestamp.digicert.com` (RFC 3161, SHA-256)

**Establishing trust** — 3 opciones:
1. Cert firmado por CA comercial (auto-trusted, expensive)
2. Internal CA + install CA cert en **User Trust Store** de todas las installs
3. Self-signed + install en User Trust Store (**no allowed en `moduleVerificationMode=high`**)

**Honeywell OEM lockdown**: `bin/policy/signing.properties` hardcodea `Niagara4Modules Code Signing / Honeywell CodeSign RSA CA` como trust anchor EXCLUSIVO. Opciones 1-3 FALLAN para modules con `<permissions>` declared.

Detalle completo: `mem_search niagara/security/signing-profile-flow`

### 4.6 Keystore layout — 3 niveles

| Nivel | Path | Rol |
|-------|------|-----|
| **Install** | `{niagara.home}/security/` | `certificates/` (3 vendor certs DSA), `licenses/`, `truststore.jks` (= systemTrustStore) |
| **User Home** | `{niagara.user.home}/security/` | `keystore.jceks` (userKeyStore), `cacerts.jceks` (userTrustStore), `untrusted.jceks` (userUntrustedStore), `signing/signers` (303 KB binary AES-256) |
| **Station Home** | `{niagara.user.home}/security/` (ProgramData) | mismo schema, `signing/signers` 193 KB (menos trusted signers que el user) |

Todos los modules auto-granted `read` a `userTrustStore` + `systemTrustStore`. Otros access requiere `KEY_STORE` permission group.

El file `signing/signers` es **binary encrypted AES-256** — registry de trusted signers para module load time.

Detalle completo: `mem_search niagara/security/keystore-layout`

---

## 5. Validaciones decompiladas

Usando `module-navigator` (tool Python sobre 51,167 classes decompiladas con Vineflower 1.11.1) se confirmaron 3 mecanismos críticos en source code real:

### 5.1 Skip module validation — path legítimo

**`ModuleClassLoader.loadSkipModuleValidation()` (baja módulo, líneas 543-570)**:

Existe un bypass OFICIAL, DOCUMENTED EN CÓDIGO, license-gated. Requiere 2 condiciones AND:

1. **System property**: `niagara.classLoader.skipModuleValidation=true`
2. **License feature**: `tridium:developer` con attr `skipModuleValidation="true"`

```java
boolean skipModuleValidationRequested = Boolean.getBoolean("niagara.classLoader.skipModuleValidation");
if (skipModuleValidationRequested) {
   Feature feature = Sys.getLicenseManager().checkFeature("tridium", "developer");
   skipModuleValidation = feature.getb("skipModuleValidation", false);
}
if (skipModuleValidation) {
   log.warning("**** Module validation has been DISABLED ****");
}
```

**Tu `Webs.license` YA TIENE el feature** (línea 40): `<feature name="developer" ... skipModuleValidation="true"/>`. Solo falta agregar el system property en `etc/system.properties` de la station runtime.

**Log visible**: warning muy visible con asteriscos. No silencioso.

Detalle completo: `mem_search niagara/validated/skip-module-validation-path`

### 5.2 Cert chain validation triggers — 2 paths independientes

**Path 1 — `DefaultModulesFileManager.makeManagedFile` línea 211** (al descubrir el module file):
```java
if (result.hasJavaPermissions()) {
   mgr.validateCertChain(entry, true);
}
```
**Sólo se triggerea cuando `<permissions>` está declarado**.

**Path 2 — `ModuleClassLoader` constructor** (al load classes):
```java
boolean requiresSignature = module.getRequestedNiagaraPermissions().stream()
                                  .anyMatch(NiagaraPermissionGroup::requiresSignature);
this.validateCertChain = requiresSignature && !SKIP_MODULE_VALIDATION;
```

Solo los permission groups `REFLECTION, ACCESS_CLASS, MBEAN_PERMISSION, HSM_SIGNING, THIRD_PARTY_PERMISSION` require signature. `NETWORK_COMMUNICATION` NO lo requiere.

**Matrix** de cuándo `verificationRequired=TRUE`:
| mode | Has `<permissions>` sign-required | Entry en Tridium namespace | Result |
|------|-----------------------------------|---------------------------|--------|
| low | no | no | **FALSE** (warning only) |
| low | no | yes | TRUE |
| low | yes | any | TRUE |
| medium/high | any | any | TRUE |

Esto CONFIRMA por qué en httpapi:
- Sin `<permissions>` → módulo carga con warnings, no exception
- Con `<permissions>` → strict check dispara → `ValidationException: Could not validate certificate path`

Detalle completo: `mem_search niagara/validated/cert-chain-validation-trigger`

### 5.3 SMA bypass + NETWORK_COMMUNICATION expansion

**`BSMAExpirationMonitor.doCheckMaintenanceExpiration()`**:
```java
if (LicenseUtil.isSmaExempt()) {
   this.reportOk();
   return;
}
```
Short-circuit explícito en código. El `sma.exempt="true"` del license feature → `LicenseUtil.isSmaExempt()` TRUE → check bypass.

**`NetworkCommunicationPermissionGroup` (156 líneas, package `com.tridium.security`)**:

Un `<req-permission>NETWORK_COMMUNICATION hosts="X" ports="Y"</req-permission>` expande a:

Per `host:port` combo:
1. `new NiagaraSocketPermission(host+":"+port, actions)`
2-5. `new URLPermission("http|https://host:port[/-]", "*:*")` × 4

Type mapping:
- `"all"` → `"accept, connect, listen, resolve"`
- `"client"` → `"connect, listen"`
- `"server"` → `"accept, listen"`

Plus opcionales: `NetPermission("getProxySelector")`, `NetPermission("getNetworkInformation")`, `CryptoServicesPermission("exportPrivateKey")`.

Detalle completo: `mem_search niagara/validated/sma-and-permission-expansion`

---

## 6. Implicancias prácticas para el caso httpapi

Con el mental model consolidado, las opciones para el problema original (HTTP POST desde módulo custom) se reordenan:

### 6.1 Opción re-evaluada: `skipModuleValidation` via developer feature

**Status**: factible técnicamente, license-gated, **NO intentado antes**.

Setup:
1. Verificar que el feature `developer` esté activo (confirmed — `Webs.license` line 40)
2. Agregar a `C:\ProgramData\Niagara4.14\OptimizerSupervisor\etc\system.properties`:
   ```
   niagara.classLoader.skipModuleValidation=true
   ```
3. Deployar `httpClientGAngeles` con `<permissions>` declarado y cert self-signed
4. Rebootear la station
5. **Verificar en logs**: debe aparecer `"**** Module validation has been DISABLED ****"`
6. Si el module carga sin `ValidationException`, el bypass funcionó.

**Caveat importante**: la lógica en `DefaultModulesFileManager.makeManagedFile` línea 211 (el **primer** validation path) NO está confirmado que consulte `SKIP_MODULE_VALIDATION`. El skip flag aplicó en `ModuleClassLoader` (second path). Es posible que `DefaultModulesFileManager` todavía rechace el module al discover. Requiere test empírico o decompilación adicional de `CoreCryptoManager.validateCertChain`.

**Riesgos**: warning muy visible en logs. Auditing-unfriendly. Para dev work; no para production.

### 6.2 Opción C revisitada: file bridge + external poller

**Status**: sigue viable. Arquitectura:
- Módulo Niagara escribe JSON a file en station home (no requiere SocketPermission)
- External poller (Node/Python) lee files y hace HTTP POST real
- Station usa `FilePermission` (que sí se puede granted sin tocar cert chain)

Pros: sin modificar validation flow. Contras: latency, dos componentes coordinados.

### 6.3 Opción rechazada: `-Djava.security.policy==custom.policy` en nre.properties

Según doc oficial de securityManager.html, el JSM puede override via:
1. `-Djava.security.properties=<URL>` (append)
2. `-Djava.security.properties==<URL>` (2 equals, complete override)

Pero `nre.properties` en station home es modificable. El intento en sesión previa falló por line break issues en PowerShell — **no por el approach mismo**. Retestable via script `.ps1`.

Caveat: aunque grant `SocketPermission` via custom policy, el strict check de cert chain corre independiente. Solo sirve si el módulo NO declara `<permissions>` (lo cual contradice el approach).

### 6.4 Opción E rechazada: obtener cert firmado por Honeywell CodeSign RSA CA

No viable como end-user. Cert comercial issued by Honeywell ACS requiere contratos B2B.

### 6.5 Recomendación

**Para development/testing**: probar opción 6.1 (skipModuleValidation) primero. Si falla por el `DefaultModulesFileManager` path, pivotear a opción C.

**Para production**: opción C (file bridge) o usar el **Signing Service** (`certSigningService` feature presente) con `bulkCertSigner` para issue certs internos — pero NO resuelve el Honeywell trust anchor lockdown, solo simplifica PKI interna.

---

## 7. Referencias rápidas

### 7.1 Paths clave

| Path | Rol |
|------|-----|
| `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/` | Install root (Niagara N4 Honeywell OEM) |
| `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/policy/` | 3 policy files PKCS7-signed (java.policy, java.security, signing.properties) |
| `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/security/{certificates,licenses}/` | Install-level certs + licenses |
| `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/devguide/` | Developer guide oficial (HTML) |
| `/mnt/c/Users/equipo/Niagara4.14/OptimizerSupervisor/` | User home (workbench + dev gradle workspace) |
| `/mnt/c/Users/equipo/Niagara4.14/OptimizerSupervisor/security/` | User keystores (keystore.jceks, cacerts.jceks, untrusted.jceks, signing/signers) |
| `/mnt/c/Users/equipo/Niagara4.14/OptimizerSupervisor/certManagement/` | User cert management workspace (PEM exports) |
| `/mnt/c/ProgramData/Niagara4.14/OptimizerSupervisor/` | Station home (runtime data, station signers registry) |
| `/mnt/c/ProgramData/Niagara4.14/OptimizerSupervisor/stations/{HoneywellMX60,PRUEBAS}/` | Station configs (config.bog) |
| `/home/cristian/modules/Prototipos/modulos/organized/` | 667 modules decompiled workspace (Vineflower) |
| `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/module-navigator/` | Module Navigator tool (175+ CLI commands, 12 indexes, web dashboard) |
| `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/tools/niagara_help.py` | Help Navigator tool |

### 7.2 Classes clave para deep-dive futuro

| Class | Location | Rol |
|-------|----------|-----|
| `com.tridium.sys.Nre` | baja | Core runtime, holds all managers |
| `com.tridium.sys.module.ModuleClassLoader` | baja | Module class loading + validation |
| `com.tridium.sys.module.DefaultModulesFileManager` | baja | Module file discovery + validation |
| `com.tridium.sys.license.NLicenseManager` | baja | License manager runtime |
| `com.tridium.sys.registry.NRegistry` | baja | Registry implementation |
| `com.tridium.security.NetworkCommunicationPermissionGroup` | baja | NETWORK_COMMUNICATION mapping |
| `com.tridium.httpClient.util.BSMAExpirationMonitor` | httpClient-rt | SMA monitor component |
| `com.tridium.platform.license.BLicensePlatformService` | platform-rt | License platform service |
| `com.tridium.platform.fox.BLicenseChannel` | platform-rt | Fox channel for license queries |
| `javax.baja.license.Feature` + `LicenseManager` | baja | Public license API |

### 7.3 Topic keys en engram (project `niagara-research`)

**Estructura** (6):
- `niagara/estructura/arquitectura-general`
- `niagara/estructura/module-system`
- `niagara/estructura/permissions-format`
- `niagara/estructura/registry`
- `niagara/estructura/station-bootstrap`
- `niagara/estructura/fox-protocol`

**Security** (6):
- `niagara/security/java-security-manager-model`
- `niagara/security/permission-groups-table`
- `niagara/security/module-permissions-flow`
- `niagara/security/user-category-model`
- `niagara/security/signing-profile-flow`
- `niagara/security/keystore-layout`

**Licensing** (5):
- `niagara/licensing/model-overview`
- `niagara/licensing/license-file-format`
- `niagara/licensing/features-and-properties`
- `niagara/licensing/honeywell-oem-overlay`
- `niagara/licensing/subscription-and-cloud`

**Validated empirically from decompiled source** (3):
- `niagara/validated/skip-module-validation-path`
- `niagara/validated/cert-chain-validation-trigger`
- `niagara/validated/sma-and-permission-expansion`

### 7.4 Commands útiles para Module Navigator

```bash
cd /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/module-navigator

python3 tools/module_nav.py stats                              # corpus overview
python3 tools/module_nav.py search "BSMA*"                     # glob search
python3 tools/module_nav.py token "validateCertChain"          # full-text SQLite
python3 tools/module_nav.py source <ClassName>                 # view decompiled source
python3 tools/module_nav.py callers <method>                   # call graph
python3 tools/module_nav.py hierarchy <BaseClass>              # inheritance tree
python3 tools/module_nav.py unified <query>                    # cross-navigator search
python3 tools/module_nav.py feature-brief <domain>             # full investigation brief
python3 tools/module_nav.py repl                               # interactive REPL
python3 tools/module_nav_web.py --port 8042                    # web dashboard
```

### 7.5 Context de la sesión previa (project `httpapi`)

Para cross-reference histórico:

- Observation #300 — `niagara/cert-chain-validation-broken` (angelesCA wrongly configured as CA)
- Observation #301 — `niagara/httpclient-permission-model` (`<permissions>` triggers strict check)
- Observation #302 — `niagara/honeywell-oem-signing-lockdown` (signing.properties hardcoded trust anchor)
- Observation #303 — `niagara/honeywell-policy-integrity-check` (java.policy PKCS7-signed, not modifiable)
- Observation #305 — `session/2026-04-19-httpclient-saga-closure` (full session summary + 4 remaining options)

Search: `mem_search "topic-key-name" project=httpapi`.

---

## Changelog

- **2026-04-19**: Session previa en project `httpapi`. 5 discoveries, 4 horas, rollback completo.
- **2026-04-20**: Esta sesión. Init SDD `niagara-research`, 3 bloques deep-dive (Estructura, Security, Licensing), validación empírica con 51,167 classes decompiladas, 20 topic keys en engram.
