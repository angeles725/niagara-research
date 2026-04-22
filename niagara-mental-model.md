# Niagara N4 — Mental Model Consolidado

**Sesión**: 2026-04-20
**Distribución**: Honeywell OptimizerSupervisor-N4.14.0.162
**Corpus**: 926 JARs decompilados (51,167 clases) via module-navigator CLI
**Método**: Investigación empírica READ-ONLY (3 sub-agents Explore en paralelo)

---

## Tabla de contenidos

1. [Bloque 1 — Estructura del framework](#bloque-1--estructura-del-framework)
2. [Bloque 2 — Licenciamiento](#bloque-2--licenciamiento)
3. [Bloque 3 — Modelo de seguridad completo](#bloque-3--modelo-de-seguridad-completo)
4. [Síntesis final y decisiones estratégicas](#síntesis-final)

---

## BLOQUE 1 — Estructura del framework

### 1.1 Profiles rt / ux / wb

**Distribución empírica**: 500 rt, 103 ux, 197 wb (661 módulos únicos).

| Profile | Qué va | Dónde corre | Depende de |
|---------|--------|-------------|------------|
| `-rt` (runtime) | Lógica de negocio, drivers, servicios, alarmas, historiales. Sin UI. | Station JVM (NRE) | Otros `-rt`, `-doc`, infraestructura (`baja`, `crypto`) |
| `-ux` (UI eXtension) | Vistas lightweight para Workbench. Datos + frameworks UI. | Workbench JVM | `-rt` + `bajaui-ux`, `gx-ux` |
| `-wb` (Workbench plugin) | Plugin Swing completo. Vistas ricas. | Workbench JVM (desktop) | `-rt`, `-ux`, `bajaui-wb`, `gx-wb` |

**Regla dura**: `-rt` NUNCA depende de `-ux` o `-wb`. Es la fuente de verdad sin UI.

**Gotcha observado**: `alarm-rt` importa algunos `-wb` (devkit-wb, test-wb) SOLO para utilidades no-visuales (test helpers, config build). No instancia UI, reutiliza código. Caso borde, no la norma.

### 1.2 module.xml en META-INF/

- **Obligatorio**: si falta, `ModuleException: "Module missing META-INF/module.xml"`.
- **Generación**: gradle plugin Niagara automático. Manual solo fuera del framework (raro).
- **Case-insensitive fallback** (Windows compat): `meta-inf/module.xml`.
- **Parseado por**: `DefaultModulesFileManager`, `ModuleManager`, `NDependencyInfo`, `com.tridium.sys.registry.Builder`.

### 1.3 NRE / Station / Workbench — Procesos

| Proceso | Clase bootstrap | JVM | Rol |
|---------|----------------|-----|-----|
| **NRE** (Niagara Runtime Engine) | `com.tridium.sys.Nre` (1,495 líneas, ZKM obfuscado) | JVM Station | Ejecuta componentes reales (control loops, drivers) |
| **Station** | `com.tridium.sys.station.Station` (851 líneas) | Dentro del NRE | Instancia lógica (singleton). 1 por nre.exe típicamente |
| **Workbench** | Plugin loader `-wb` | JVM cliente distinta | Conecta a Stations via FOX |

**Arranque**: `nre.exe`, `station.exe`, `wb.exe` en `/bin/` (Windows; `.sh` en Unix).

**NO existe** `station.jar` ni `workbench.jar` monolítico — son colecciones de módulos indexados.

### 1.4 Registry de tipos (`sys.registry`)

**Clase central**: `com.tridium.sys.registry.NRegistry` (473 líneas, módulo `baja`).

**Flow**:
1. Boot NRE → inicializa `NRegistry`.
2. `rebuild()` o `syncModules()` → escanea módulos cargados.
3. `com.tridium.sys.registry.Builder` (1,369 líneas) parsea cada módulo:
   - Extrae `META-INF/module.xml`.
   - `ClassScanner` busca clases con `@NiagaraType`.
   - Indexa en `RegistryDatabase`.
4. 10,797 `@NiagaraType` en el corpus (4,326 con `@NiagaraProperty`, 1,499 con `@NiagaraAction`).

**Estados en log**:
- `Loaded` = tipo indexado, disponible para instanciar.
- `up-to-date` = registry sincronizado con JARs (checksum).
- Lazy-loaded: agregar módulo post-boot requiere `loadModule(name)` explícito o `syncModules()` full rebuild.

### 1.5 Fox protocol

- **Puertos**: TCP 1911 (plain) / TLS 4911 (FoxS).
- **Clases**: `BFoxService`, `BFoxSession`, `BFoxChannel` (68 clases `BFox*`).
- **Autenticación**: Digest, cert-based, Kerberos, Google Auth. Nunca plaintext.
- **Multiplexado**: 1 socket TCP → N canales (apps). No es 1 conexión = 1 channel.
- **Peer-to-peer**: Station y WB pueden ambos iniciar operaciones.

**Sobre "bypass SocketPermission"**: No hay bypass. Niagara usa **su propio** `NiagaraSocketPermission` (no `java.net.SocketPermission`). El SecurityManager intercepta en la capa FOX con la clase custom. Los módulos FOX requieren `NETWORK_COMMUNICATION` permission group igual que cualquier otro.

---

## BLOQUE 2 — Licenciamiento

### 2.1 Modelo SMA (Software Maintenance Agreement)

**Clave conceptual**: **SMA no es una feature, es un ATRIBUTO de features**.

```xml
<feature name="http" sma.exempt="true" ... />
<feature name="cloudLink" sma.exempt="false" ... />
```

- `sma.exempt="true"` → feature funciona sin SMA vigente.
- `sma.exempt="false"` → feature requiere SMA activo.

**Verificación en runtime**:
```java
Sys.getLicenseManager().checkFeature("tridium", featureName)
// throws FeatureNotLicensedException si invalid/expired
```

**`BSMAExpirationMonitor`** (httpClient-rt, 423 líneas): BComponent **pasivo**. NO bloquea operación. Solo expone estado vía slots:
- `mode` (warning/expired/disabled)
- `remaining` (BStatusNumeric, días restantes)
- `warnBelow` (int=30, threshold alarma)
- `exempt` (boolean, per-instance)
- `checkInterval` (BRelTime.DAY)

### 2.2 Archivos `.license`

**3 vendors en este install**:

```
/security/licenses/
├── Honeywell.license          (vendor="Honeywell", 27 features)
├── HoneywellCentraLine.license (vendor="HoneywellCentraLine", 1 feature)
└── Webs.license               (vendor="Tridium", 150 features, brand=Webs)

/security/licenses/db/{hostId}/  (cached copies per-station)
```

**Formato XML**:
```xml
<license vendor="Tridium" expiration="2027-03-31" hostId="Win-6E6E-10AC-D1DD-8276">
  <feature name="brand" brandId="Webs" accept.station.out="*" .../>
  <feature name="http" sma.exempt="true" history.limit="none" point.limit="none"/>
  <feature name="developer" moduleDev="true" skipModuleValidation="true"/>
  <signature>MCwCFBa0...</signature>
</license>
```

### 2.3 Station vs module licensing

**NO hay archivo separado para "el station"**. El station consume la feature `"station"` dentro de `Webs.license` (vendor=Tridium).

```java
Station.java:
Feature f = Sys.getLicenseManager().checkFeature("tridium",
    getStationLicenseFeature(NreLib.getHostId(...)));
```

→ La feature se calcula dinámicamente del hostId. Module licenses son features independientes (http, bacnet, mqtt) dentro del mismo archivo.

### 2.4 Top features y patrones

**95 hits de `checkFeature`** (58 resueltos, 37 ZKM-obfuscados).

Top 5:
1. `reflow` (12) — NiagaraMods addon
2. `fips140-2` (6) — FIPS crypto
3. `developer` (5) — module development / `skipModuleValidation`
4. `workbenchAzul` (5) — UI Azul
5. `brand` (4) — brandId "Webs"

### 2.5 Honeywell OEM overlay

**Arquitectura limpia**: NO parchea código Tridium. Solo archivos `.license` adicionales.

Features exclusivos Honeywell:
- `ascBAC`, `ascLON` — Advanced Services
- `honEdgeDriver`, `honConnectedPower`, `honEasyBinding`
- `maxproVideo` (16 cámaras), `redLink`, `SylkActuatorAnalytics`, `honLoRaMqtt`
- `IPVAV` — honeywellFunctionBlocks-rt

**Branding**: `<feature name="brand" brandId="Webs">` dentro de Webs.license (vendor=Tridium). Honeywell usa Niagara bajo marca Webs.

### 2.6 LicenseManager runtime API

```java
// Interfaz
javax.baja.license.LicenseManager {
  Feature checkFeature(String vendor, String feature)
    throws FeatureNotLicensedException, LicenseDatabaseException;
}

// Implementación
SubscriptionLicenseManager extends NLicenseManager

// Uso típico
Feature f = Sys.getLicenseManager().checkFeature("tridium", "developer");
String brandId = f.get("brandId");
boolean skip = f.getb("skipModuleValidation", false);
```

---

## BLOQUE 3 — Modelo de seguridad completo

### 3.1 Pipeline cert → trust store → signers → grants (end-to-end)

```
JarEntry (módulo cargándose)
    ↓
ModuleClassLoader.verifyJarEntrySignature(entry)
    ↓
[DECISION 1] verificationRequired = verificationMode != LOW
             || (shouldCheckTpk || validateCertChain) && canCheckTpk
    ↓
CodeSigner[] signers = entry.getCodeSigners()
    ↓
[LOOP] para cada CodeSigner:
    CoreCryptoManager.validateCertChain(signer, shouldCheckTpk)
        └─→ CertificateChainValidator (bytecode, no decompilado)
             ├─ self-signed check (issuer==subject?)
             │   └─ mode==HIGH → reject; mode<HIGH → warn+accept
             ├─ timestamp check (recommended, no mandatory)
             └─ extensions (KeyUsage, BasicConstraints)
    ↓
foundValid=true → define class
foundValid=false && verificationRequired → throw ValidationException
    ↓
[LATER] SecurityManager.checkPermission(...) cuando código se ejecuta
```

**Punto crítico**: fallo en cert chain es **FATAL ANTES de permission checks**. Esto explica la saga httpapi 2026-04-19 — nunca se llegaba a revisar permisos porque la firma rechazaba el load.

### 3.2 Los 3 archivos firmados de `bin/policy/`

| File | Size | Contenido | Integrity |
|------|------|-----------|-----------|
| `java.policy` | 21 KB | Grants por codeBase (nre.jar, niagarad.jar, npsdkTest.jar) | PKCS7 embebido en comentario `// -----BEGIN NIAGARA SIGNATURE-----` |
| `java.security` | 65 KB | Security providers JVM (BouncyCastle FIPS provider=2) | Firmado igual |
| `signing.properties` | 330 B | CA hardcoded: `CN=Honeywell CodeSign RSA CA, OU=ACS, O=Honeywell International Inc., C=US` | Firmado igual |

**PolicyIntegrityChecker** (no decompilado completo) valida PKCS7 al boot antes de aplicar grants. Modificar cualquiera de los 3 → invalida la firma → Niagara refusa arranque.

### 3.3 Transform `module-permissions.xml` → `module.xml` runtime

**SOURCE** (dev escribe):
```xml
<niagara-permission-groups type="station">
  <req-permission>
    <name>NETWORK_COMMUNICATION</name>
    <parameters>
      <parameter name="hosts" value="*"/>
      <parameter name="ports" value="1234"/>
    </parameters>
  </req-permission>
</niagara-permission-groups>
```

**RUNTIME** (gradle plugin `niagara-signing` genera):
```xml
<permissions>
  <java-permission class="com.tridium.nre.security.NiagaraSocketPermission">
    <parameter name="hosts" value="*"/>
    <parameter name="ports" value="1234"/>
  </java-permission>
</permissions>
```

Clases: `NiagaraPermissionGroupFactory` (212L), `BasePermissionGroup` (95L, abstract), 26 subclases.

### 3.4 Los 19 permission groups (tabla consolidada)

| Group | Java permissions | Severity | Requires signature | Top modules |
|-------|------------------|----------|--------------------|-----|
| NETWORK_COMMUNICATION | NiagaraSocketPermission(1091), URLPermission(34), NetPermission(24) | MODERATE | — | portalApi-rt, baja |
| MANAGE_EXECUTION | RuntimePermission(1159) | MODERATE | — | 559 mods |
| SYSTEM_PROPERTIES | PropertyPermission(609) | MILD | — | 549 mods |
| LOGGING | FilePermission(1075), LoggingPermission(541) | MODERATE | — | 542 mods |
| MODIFY_IO_STREAMS | RuntimePermission(542) | MODERATE | — | 541 mods |
| **ACCESS_CLASS** | RuntimePermission(575) | SEVERE | **YES** | 540 mods |
| GET_ENVIRONMENT_VARIABLES | RuntimePermission(553) | MILD | — | 547 mods |
| SHUTDOWN_HOOKS | RuntimePermission(546) | MILD | — | 543 mods |
| LOAD_LIBRARIES | RuntimePermission(39) | SEVERE | — | 23 mods |
| RUNTIME_EXECUTION | FilePermission(38) | SEVERE | — | 16 mods |
| KEY_STORE | KeyStorePermission(44), KeyRingPermission(18) | MODERATE | — | 47 mods |
| **REFLECTION** | ReflectPermission(14) | SEVERE | **YES** | 14 mods |
| **MBEAN_PERMISSION** | MBeanServerPermission(6), MBeanPermission(5) | SEVERE | **YES** | 4 mods |
| AUTHENTICATION | NiagaraBasicPermission(16), AuthPermission(9) | MILD | — | 13 mods |
| UI | AWTPermission(1083) | MILD | — | 540 mods |
| SIGNING | SigningPasswordPermission(5) | — | — | 5 mods |
| BACKUPS | FilePermission(4), NiagaraBasicPermission(2) | SEVERE | — | 3 mods |
| DIAGNOSTICS | ManagementPermission(3) | SEVERE | — | 3 mods |
| SET_SYSTEM_TIME | NiagaraBasicPermission(1) | MILD | — | 1 mod |

**3 grupos que requieren firma obligatoria**: ACCESS_CLASS, REFLECTION, MBEAN_PERMISSION. Esto aplica incluso en `verificationMode=LOW` via `requiresSignature()` en ModuleManager.

### 3.5 NiagaraSocketPermission (custom)

- Ubicación: `com.tridium.nre.security.NiagaraSocketPermission`
- vs `java.net.SocketPermission` estándar: permite **narrowing** por hosts/ports separados en parámetros del XML.
- Constructor: `NiagaraSocketPermission(String hostAndPort, String actions)`.
- `implies()`: host pattern matching + port range checking (bytecode compilado, no decompilado).

### 3.6 CertificateChainValidator (reglas)

Ubicación: `com.tridium.crypto.core.cert.CertificateChainValidator` (bytecode, no decompilado).

Reglas inferidas del caller (`ModuleClassLoader`):

1. **Self-signed** → solo OK si `verificationMode < HIGH`. En HIGH → `"Self signed signing certificate not permitted"`.
2. **Timestamp** → no mandatory pero si falta: `log.warning("signature will fail when cert expires")`.
3. **Extensions** (presumible): KeyUsage.digitalSignature, ExtendedKeyUsage.codeSigning, BasicConstraints.

### 3.7 Keystores (tabla)

| Nivel | Path | Rol |
|-------|------|-----|
| JDK | `$JAVA_HOME/lib/security/cacerts` | Root CAs estándar |
| Station | `/security/truststore.jks` | User trust anchors (Honeywell CA + intermediates) |
| Station | `/security/certificates/` | Filesystem cert storage (CRLs?) |
| Station | `/sw/signing/signers` (202KB binary) | JarSignatureRegistry cache |
| Session | runtime ModuleClassLoader per-module | Parsed certs from JAR |

### 3.8 Signing Profiles

```
BAbstractSigningProfile (abstract, 162L)
    ├─ BSimpleSigningProfile (422L) — local PKCS#12
    └─ BAwsSigningProfile (37L) — AWS KMS remote
```

Módulo: `signingService-{rt,wb,ux}.jar`.

### 3.9 nverify y Workbench Jar Signer

- `bin/nverify.exe` — standalone cert chain validator offline.
- Workbench Jar Signer: UI en `signingService-wb` (BSignatureDetailsPane en platDaemon-wb).
- Signing CLI: `gradle dist` + `niagara-signing` plugin orquesta `CoreCryptoManager.sign()`.

### 3.10 skipModuleValidation (bypass license-gated)

**Status en este install**: `LICENSE-GATED bypass AVAILABLE` — falta solo agregar system property.

```
Condition 1 — system property
  key:     niagara.classLoader.skipModuleValidation
  status:  INACTIVE

Condition 2 — license feature
  feature: developer with skipModuleValidation='true'
  match:   Webs.license (exp=2027-03-31, moduleDev=true, skipModuleValidation=true)
  status:  ACTIVE
```

**Implementación** (`ModuleClassLoader.loadSkipModuleValidation`, L543-570):
```java
if (!Boolean.getBoolean("niagara.classLoader.skipModuleValidation")) return false;
Feature f = Sys.getLicenseManager().checkFeature("tridium", "developer");
boolean skip = f.getb("skipModuleValidation", false);
if (skip) log.warning("**** Module validation has been DISABLED ****");
return skip;
```

**Crítico**: skip NO desactiva SecurityManager. Solo salta cert chain validation. Permisos siguen vigentes.

---

## Síntesis final

### Por qué pegamos 4 horas en httpClient fork (2026-04-19)

Stack real del fallo:
1. Build custom de httpClient-rt con `<permissions>` extendidos.
2. Firma con cert propio (no Honeywell).
3. Load en station → `ModuleClassLoader.verifyJarEntrySignature()`.
4. `CoreCryptoManager.validateCertChain()` compara issuerDN del cert contra `signing.properties` hardcoded: `CN=Honeywell CodeSign RSA CA`.
5. **Mismatch → ValidationException → class never loads → SecurityManager never even checked**.

El AccessControlException fue SÍNTOMA, la causa raíz era cert chain rejection.

### Caminos posibles (por orden de viabilidad)

1. **skipModuleValidation activo** — `niagara -Dniagara.classLoader.skipModuleValidation=true`. License feature `developer` ya está activa. Trade-off: abre ventana global, útil solo dev.
2. **File bridge** — módulo "oficial" Honeywell que lee/escribe archivos, proceso separado hace el trabajo pesado. Sin firma custom.
3. **Fox bridge** — servicio station que expone endpoints Fox para lógica externa. Sin parchear módulos OEM.
4. **Fork rebrand con cert Honeywell** — imposible sin acceso a CA Honeywell. Off the table.

### Conclusiones arquitectónicas

- **Honeywell lockdown es por diseño**, no bug. Política OEM explícita vía signing.properties hardcoded.
- **El framework está bien diseñado**: trust chain antes de permissions, policy files firmados, permission groups granulares, SMA ortogonal a features.
- **skipModuleValidation es la única puerta oficial**. Está license-gated. No es hack, es feature documentada.
- **Módulos que requieren firma (3)**: ACCESS_CLASS, REFLECTION, MBEAN_PERMISSION. Ningún workaround en modo LOW.

---

## Engram topic keys (detalle extendido)

- `niagara/estructura/profiles-rt-ux-wb`
- `niagara/estructura/registry-types`
- `niagara/estructura/fox-protocol`
- `niagara/licensing/sma-attribute-model`
- `niagara/licensing/honeywell-oem-overlay`
- `niagara/licensing/license-manager-api`
- `niagara/security/cert-chain-pipeline`
- `niagara/security/permission-groups-19-table`
- `niagara/security/skip-module-validation-bypass`
- `niagara/security/policy-files-triple-signed`

---

**Sesión cerrada**: 2026-04-20 — mental model consolidado y verificado empíricamente contra corpus decompilado.
