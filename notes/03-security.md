# BLOQUE 3 — Security model completo de Niagara N4

Fecha: 2026-04-20
Fuente primaria: `niagara-help/devguide-clean/security/{security,securityManager,requestingPermissions,codeSigning}.txt`
Fuente empírica: `bin/policy/{java.policy,java.security,signing.properties}`, `com.tridium.sys.module.ModuleClassLoader`, 927 module.xml scanned
Validado contra: corpus Honeywell OptimizerSupervisor N4.14.0.162

---

## 3.1 Dos modelos de "permissions" que NO hay que confundir

Niagara usa el término "permissions" en dos sistemas distintos. Confundirlos es el error #1 del dev novato.

| Modelo | Qué protege | A quién chequea | Clases clave |
|--------|-------------|-----------------|--------------|
| **Java Security Manager + module permissions** | Código ejecutable (qué puede hacer un módulo: sockets, files, reflection, etc.) | `CodeSource` del class loader (el JAR firmado) | `NiagaraPolicy`, `NiagaraSocketPermission`, `java.lang.RuntimePermission` |
| **User/Role/Category permissions** | Datos runtime (qué puede hacer un usuario sobre BComponents/Files/Histories) | `BUser` context en invocaciones de slots | `BPermissions`, `BPermissionsMap`, `BCategoryMask`, `BIProtected` |

**Este bloque cubre el primero** (code-level) — es el que causó la saga httpapi. El segundo se cubre brevemente al final para cerrar el mental model.

---

## 3.2 El Java Security Manager en Niagara 4

### Estado por default
Niagara 4 **activó el Java Security Manager (JSM)**. Citado directo del devguide:

> By default, no one has any permissions. Any code that requires a permission check will fail, with an AccessControlException. Each permission must be granted explicitly, using a policy file.

Esto invierte el default de Java (que tradicionalmente daba todos los permisos si no había SecurityManager). En Niagara 4+, **arrancás de cero** — tu módulo no puede hacer NADA sensible hasta que sus `<permissions>` sean validadas y granteadas.

### Síntomas de issue
- `AccessControlException: access denied (<required permission>)`
- `access denied (<required permission>)`

### Debug
- Log `security.niagaraPolicy` con level FINE/FINER/FINEST
- JVM arg `-Djava.security.debug=access,failure` (produce MUCHO output)

### Disable (workaround, NO fix)
Requiere la feature **`Tridium:smDeveloperMode`** en la license. Después:
- Command line: `station <name> -Dniagara.security.manager.disable`
- System property: `niagara.security.manager.disable=true` en `etc/system.properties`
- QNX: `touch /etc/no-security-manager`

Al activar disable, los AccessControlException ahora se **loggean a archivo** (`developerSecurityManagerLog-STATION-{date}-{time}.txt` en `niagara_user_home`) pero la aplicación sigue corriendo. Útil para enumerar todos los issues en un go.

**En la distribución Honeywell OptimizerSupervisor**: la feature `smDeveloperMode` está en `Webs.license` (confirmado grep). Por lo tanto, `niagara.security.manager.disable` es una opción legítimamente activable, aunque NO es lo que la sesión httpapi necesitaba (la saga quería PERMISOS, no disable del SM).

---

## 3.3 module-permissions.xml — source format (dev edita)

Ubicación: al lado de `module-include.xml` en el dev dir del módulo. Ejemplo oficial:

```xml
<permissions>
  <niagara-permission-groups type="station">  <!-- station | workbench | all -->
    <req-permission>                           <!-- required, sin esto el módulo no arranca -->
      <name>NETWORK_COMMUNICATION</name>
      <purposeKey>moduleComm.purpose</purposeKey>  <!-- lexicon key o texto literal -->
      <parameters>
        <parameter name="hosts" value="*"/>
        <parameter name="ports" value="1234"/>
      </parameters>
    </req-permission>
    <opt-permission>                           <!-- optional, puede fallar gracefully -->
      <name>SET_SYSTEM_TIME</name>
      <purposeKey>setTime.purpose</purposeKey>
    </opt-permission>
  </niagara-permission-groups>
</permissions>
```

### 3 tipos de scope
- **station**: permission aplica a cuando el módulo corre dentro de una station JVM
- **workbench**: aplica cuando corre dentro del Workbench JVM
- **all**: aplica a ambos

Podés tener múltiples `<niagara-permission-groups>` con scopes distintos en el mismo archivo.

### Required vs Optional
En Niagara 4.2+ **TODOS los permissions requested son granteados automáticamente** — el dev dice si son required u optional, pero el effect actual no cambia. En versiones futuras (anunciadas) se plantea darle al usuario final opción de aceptar/rechazar optionales en install time.

### Qué hace el dev en código
Para required: asumir que el permission está, y manejarlo con try/catch genérico por si falla.
Para optional: wrap el call sensible en try/catch de `AccessControlException` y tener fallback:

```java
try {
    myField = System.getenv("my_env_variable");  // needs GET_ENVIRONMENT_VARIABLES
} catch (AccessControlException e) {
    myField = System.getProperty("my.system.property");  // fallback
}
```

---

## 3.4 Los 19 Niagara permission groups (catálogo completo)

Lista compilada desde `requestingPermissions.txt`. Cada grupo → mapping a Java permissions concretas + severity + parameters aceptados.

### Tabla resumen (severity y requisito de firma)

| Group | Severity | Requires signed module? |
|-------|----------|-------------------------|
| ACCESS_CLASS (>=4.9) | SEVERE | **Sí** |
| AUTHENTICATION | MILD | No |
| BACKUPS (>=4.4) | SEVERE | No |
| DIAGNOSTICS | SEVERE | No |
| GET_ENVIRONMENT_VARIABLES | MILD | No |
| KEY_STORE | MODERATE | No |
| LOAD_LIBRARIES | SEVERE | No |
| LOGGING | MODERATE | No |
| MANAGE_EXECUTION | MODERATE | No |
| MBEAN_PERMISSION (>=4.9) | SEVERE | **Sí** |
| MODIFY_IO_STREAMS | MODERATE | No |
| NETWORK_COMMUNICATION | MODERATE | No |
| REFLECTION | SEVERE | **Sí** (hard halt si no signed) |
| RUNTIME_EXECUTION | SEVERE | No |
| SET_SYSTEM_TIME | MILD | No |
| SHUTDOWN_HOOKS | MILD | No |
| SIGNING | (depende) | No |
| SYSTEM_PROPERTIES | MILD | No |
| THIRD_PARTY_PERMISSION (>=4.9) | SEVERE | **Sí** |
| UI | MILD | No |
| HSM_SIGNING | — | **Sí** (hard halt si no signed) |

### Los 4 más usados en el corpus (por frecuencia)

**NETWORK_COMMUNICATION** (el que pedía httpapi):
- `com.tridium.nre.security.NiagaraSocketPermission "hosts:ports" "action"`
- `java.net.NetPermission "getProxySelector"` (opcional, since 4.3)
- `java.net.NetPermission "getNetworkInformation"` (opcional, since 4.3)
- `org.bouncycastle.crypto.CryptoServicesPermission "exportPrivateKey"` (opcional, since 4.6)
- `java.net.URLPermission "http://host:port"` (opcional, since 4.9)
- `java.net.URLPermission "https://host:port"` (opcional, since 4.9)

Parameters: `hosts`, `ports`, `type` (client/server/all), `proxySelector`, `getNetworkInformation`, `SSLSockets`.

**SYSTEM_PROPERTIES**: `java.util.PropertyPermission "<name>", "read,write"`. Params: `properties`, `actions`.

**MANAGE_EXECUTION**: thread handling (`modifyThread`, `modifyThreadGroup`, `setContextClassLoader`, `enableContextClassLoaderOverride`, `getClassLoader`). Sin params.

**KEY_STORE**: `java.util.PropertyPermission "keystore_name", "read,write"`. Params: `keystores` (userTrustStore, userUntrustedStore, systemTrustStore, userKeyStore o `*`), `actions`.

### Gotcha: algunos permission groups hoy son "grant-signer-only"
**REFLECTION** y **HSM_SIGNING** hacen **halt del Station/Workbench** si el módulo no está válidamente firmado. Las otras que requieren signing (ACCESS_CLASS, MBEAN_PERMISSION, THIRD_PARTY_PERMISSION) — docs dicen "requires signed" pero no aclara si halt o warn.

### Aggregate empírico del corpus
**927 module.xml scanned. 563 tienen `<permissions>` (60.7%). 4,806 java-permission entries totales.**

Cada módulo Tridium que hace networking declara literalmente:
```
com.tridium.nre.security.NiagaraSocketPermission "*:1-100000" "accept,connect,listen,resolve"
```

Esta **mismísima entry** es la que necesitaba el módulo `httpClientGAngeles` de la saga httpapi.

---

## 3.5 `<permissions>` en module.xml — runtime format (gradle produce)

El plugin gradle `niagara-module` traduce `module-permissions.xml` source → `<permissions>` block embeddable en `module.xml`. Cambia el formato: en vez de `<niagara-permission-groups>` con `<req-permission>`/`<opt-permission>` + `<name>` + `<purposeKey>`, el runtime usa `<java-permissions>` con `<java-permission class="..." name="..." action="..."/>` directamente.

### Ejemplo runtime (httpClient-rt, ya leído)
```xml
<permissions>
  <java-permissions type="station">
    <java-permission class="java.lang.RuntimePermission" name="modifyThread"/>
    <java-permission class="java.lang.RuntimePermission" name="modifyThreadGroup"/>
    <java-permission class="java.lang.RuntimePermission" name="setContextClassLoader"/>
    <java-permission class="java.lang.RuntimePermission" name="enableContextClassLoaderOverride"/>
    <java-permission class="java.lang.RuntimePermission" name="getClassLoader"/>
    <java-permission class="java.lang.RuntimePermission" name="shutdownHooks"/>
    <java-permission action="connect,resolve"
                     class="com.tridium.nre.security.NiagaraSocketPermission"
                     name="*:1-100000"/>
    <java-permission action="*:*"
                     class="java.net.URLPermission"
                     name="http:*"/>
    <java-permission action="*:*"
                     class="java.net.URLPermission"
                     name="https:*"/>
    <java-permission class="java.net.NetPermission" name="getProxySelector"/>
  </java-permissions>
</permissions>
```

### Qué hace el transformador
- Cada `<req-permission>`/`<opt-permission>` con `<name>NETWORK_COMMUNICATION</name>` → expande a los 4-6 `<java-permission>` concretos que ese group mapea.
- `<parameters>` (hosts, ports, actions) → se inyectan como valores de los `name` y `action` de cada java-permission.
- **El `<purposeKey>` NO se transmite al runtime** — es solo metadata para el usuario final antes de install.
- El block runtime es **literal, denso, sin abstracción** — el security manager chequea contra esas entries exactas.

### Por qué esto importa
El runtime format es **lo que el framework ve**. Cuando el security manager valida una operación, compara contra los `<java-permission>` concretos. El source format `<req-permission><name>NETWORK_COMMUNICATION</name>` es una convenience para devs — el runtime no lo usa.

**Si dos módulos declaran NETWORK_COMMUNICATION con los mismos params, terminan con `<java-permission>` blocks IDÉNTICOS**. El framework los trata igual a nivel semántico — lo único que los distingue es el cert que firmó el JAR.

---

## 3.6 Los 3 archivos firmados de `bin/policy/`

La distribución Honeywell tiene 3 archivos en `bin/policy/` que el framework carga al boot:

### `java.policy` (271 líneas, firmado)
Policy file estándar de Java, DEFINE grants por `codeBase`. Primeros grants:

```
grant codeBase "file:${niagara.home}/bin/ext/nre.jar" {
  permission java.util.PropertyPermission "java.home", "read";
  permission java.util.PropertyPermission "os.*", "read";
  permission java.util.PropertyPermission "niagara.*", "read";
  ...
  permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
  permission com.tridium.nre.security.NiagaraBasicPermission "GET_PLATFORM_PROVIDER";
  permission java.lang.RuntimePermission "loadLibrary.nre";
};

grant codeBase "file:${niagara.home}/bin/ext/niagarad.jar" {
  permission java.util.PropertyPermission "*", "read,write";
  ...
};
```

Estos grants son **para los binarios core de Niagara** (nre.jar, niagarad.jar). Le dan permissions fijos al framework mismo, independiente de `<permissions>` en module.xml.

**La firma al final**:
```
// -----BEGIN NIAGARA SIGNATURE-----
// [base64 PKCS7 blob, ~10 líneas]
// -----END NIAGARA SIGNATURE-----
```

Está en las últimas ~10 líneas, como comentario Java-style (`//`). Niagara la lee y verifica con message-digest + PKCS7/CMS al boot. **Modificar cualquier byte del archivo antes de la firma = invalidate total** → boot falla.

### `java.security` (1323 líneas, firmado)
El **master security properties file** estándar de Java — registra Cryptography Package Providers (Bouncy Castle FIPS, SunJCE, etc.), config de SecureRandom, TLS protocols default, algoritmos disabled por weak. También firmado con el mismo método. También verificado al boot.

### `signing.properties` (7 líneas, hardcoded trust anchor)
Este es el archivo central del lockdown OEM Honeywell. Contenido real:
```properties
#THIS FILE IS AUTO GENERATED... DO NOT MODIFY!
#Fri Jun 14 10:56:01 UTC 2024
issuerDN=CN=Honeywell CodeSign RSA CA, OU=ACS, O=Honeywell International Inc., C=US
notAfter=253370764800000
notBefore=1694029865000
serialNumber=1415098852177779243
subjectDN=C=US, O=Honeywell International Inc., CN=Niagara4Modules Code Signing
```

**Este es EL cert aceptado como trust anchor para módulos con `<permissions>` críticos**. Cualquier módulo que declare permissions y no esté firmado por un cert que encadene a este → rechazo.

Analizado:
- **issuerDN** = CA que emitió el cert: "Honeywell CodeSign RSA CA" (la root CA interna de Honeywell para code signing).
- **subjectDN** = cert subject: "Niagara4Modules Code Signing" (el cert end-entity que Honeywell usa para firmar sus módulos).
- **notAfter** = `253370764800000` millis = **year 9999** = efectivamente nunca expira.
- **notBefore** = `1694029865000` millis = **2023-09-06 22:31:05 UTC** = desde entonces.
- **serialNumber** = identifier único del cert.

**Consecuencia**: el módulo httpClientGAngeles de la saga anterior estaba firmado por un cert auto-generado (`Niagara4Modules` alias con issuer `angelesCA` self-signed). Ese cert NO encadena a `Honeywell CodeSign RSA CA`. Por lo tanto → rechazo.

### Cómo Niagara verifica integridad de los 3 archivos
El boot:
1. Lee los 3 archivos.
2. Extrae la firma (PKCS7/CMS block al final).
3. Compone el "content-to-sign" (todo excepto la firma).
4. Verifica la firma con la public key embebida (probablemente hardcoded en nre.jar, o desde un keystore de system).
5. Si una sola falla → **boot aborta**.

**Otra implicación**: si Honeywell quisiera re-emitir estos archivos, Honeywell tendría que tener la private key que los firmó (probablemente en su HSM corporativo). Modificar local = romper la firma = no boot.

---

## 3.7 CertificateChainValidator — reglas de validación de cert chain

Clase: `com.tridium.crypto.core.cert.CertificateChainValidator` (decompilable en corpus).

Cuando un módulo tiene `<permissions>` que requieren signing (todos los permission groups marcados "Requires signed" + cualquier con severity SEVERE), el framework extrae la cadena de certs del JAR signature y la valida contra:

1. **Chain integrity**: `cert[i].issuerDN == cert[i+1].subjectDN` para cada par adyacente.
2. **Signature verification**: cada cert firmado correctamente por su issuer (usando la public key del issuer).
3. **Validity window**: `notBefore <= now <= notAfter` en todos.
4. **KeyUsage extension**: debe tener `digitalSignature` + `nonRepudiation` (no solo esos — depende del contexto).
5. **ExtendedKeyUsage extension**: debe tener **`1.3.6.1.5.5.7.3.3` (codeSigning)** explícitamente. Otros OIDs (serverAuth, clientAuth) NO aplican.
6. **BasicConstraints**: end-entity cert NO debe ser CA (`CA=FALSE`); CAs en la chain SÍ deben ser `CA=TRUE`.
7. **Anchor trust**: el último cert de la chain (root) debe matchear el `subjectDN` del `signing.properties` (o estar en el trust store). Eso es el hook del lockdown.

**Hallazgo crítico de la sesión httpapi** (confirmado en engram #304): el cert `angelesCA` generado durante ese troubleshooting falló PKIX validation para codeSigning por 3 razones concretas:
- `BasicConstraints: CA=TRUE, critical` → declarado como CA, no end-entity
- `KeyUsage: keyCertSign+cRLSign` → permissions de CA, no de signer
- No tenía OID `1.3.6.1.5.5.7.3.3` en ExtendedKeyUsage

Incluso si el cert `angelesCA` se hubiera incluido en el trust store, el código de validación habría rechazado por role mismatch (CA root ≠ code signer leaf).

---

## 3.8 NiagaraSocketPermission — implementación custom

Clase: `com.tridium.nre.security.NiagaraSocketPermission` (decompilable).

**Por qué existe**: el `java.net.SocketPermission` estándar tiene problemas conocidos con DNS lookups al hacer `implies()`, que en Niagara (many-connection, high-rate) causa latencia. Tridium reemplaza con un equivalente optimizado.

### Formato de params
- **name**: `"host:ports"` — host puede ser `*`, `*.domain`, IP, CIDR. Ports puede ser `N`, `N-M`, `*`.
- **action**: CSV de `accept`, `connect`, `listen`, `resolve`.

### API semantics
`implies(permission)` returns `true` si:
- El permission pedido es un `NiagaraSocketPermission` con subset de actions
- El host del pedido está en el host pattern del grant
- Los ports del pedido están en el range del grant

### Tuning knob
System property `niagara.socketPermission.noDns=true` → disable DNS lookups en el `implies()` call. Rápido pero puede rechazar valid connections si confiás en reverse DNS. **Default=false** (lookup enabled). Este tuning viene de la doc de `NETWORK_COMMUNICATION` en requestingPermissions.

### Convención universal en el corpus
**Todos los módulos Tridium** (verificado en 560+ module.xml files) declaran exactamente:
```xml
<java-permission action="accept,connect,listen,resolve"
                 class="com.tridium.nre.security.NiagaraSocketPermission"
                 name="*:1-100000"/>
```

"`*:1-100000`" = cualquier host en puertos 1-100,000. Es efectivamente "wildcard network access". Funciona porque los módulos Tridium están firmados por el cert Tridium root — el framework les confía. Un módulo custom con la MISMA entry + firmado con otro cert = rechazo binario.

---

## 3.9 Keystore layout en Niagara 4

3 niveles de keystores. Cada uno con role específico.

| Nivel | Path | Role |
|-------|------|------|
| **System / Install** | `bin/policy/signing.properties` + certs hardcoded en `nre.jar` | Trust anchor para módulos con `<permissions>` (el hardcoded cert Honeywell) |
| **User Home** | `USER_HOME/.tridium/security/niagara.signing.{jks,xml}` | Default signing profile del dev (auto-gen cert `Niagara4Modules`) |
| **Station Home** | `STATION_HOME/security/` (y `ProgramData/Niagara4.14/.../security/`) | Trust store + key store de la station runtime |

### Archivos de Station Home típicos
- `cacerts.jceks` — system trust store (CAs trusted universalmente)
- `userKeyStore.p12` — keys privadas del usuario de la station
- `userTrustStore.p12` — certs trusted por el usuario (para clients HTTP, Fox, etc.)
- `userUntrustedStore.p12` — certs EXPLÍCITAMENTE unconfiables (blacklist)

### API para leer
Módulos que necesitan acceder a keystores piden permission group **`KEY_STORE`** con parámetros `keystores=<name>`, `actions=<rw>`. El framework tiene APIs dedicadas (`com.tridium.crypto.core.*`) que actúan como proxies.

**Default**: todos los módulos tienen `read` sobre `userTrustStore` y `systemTrustStore` sin tener que pedirlo. Write requiere la KEY_STORE permission explícita.

---

## 3.10 Signing profiles y herramientas de firma

### LocalSigningProfile (default)
Desde Niagara 4.6, el build environment **auto-firma modules** con cert self-signed. Ubicación: `USER_HOME/.tridium/security/`:
- `niagara.signing.jks` — keystore con cert auto-generado (alias `Niagara4Modules`)
- `niagara.signing.xml` — settings XML

Gradle task: `gradlew :createProfile --profile-path path/to/profile.xml`. El perfil declara profileType, validity, storetype, dname, keysize, etc.

### RestrictedSigningProfile
Para builds release con cert productivo (idealmente CA-signed o HSM-backed).

### JarSignerSigningProfile (since 4.14, HSM support)
Properties file (no XML) que configura `jarsigner` con args custom. Soporta HSM via PKCS11. Para enterprise code signing compliant con req 2023+ que exigen HSM.

Ejemplo:
```properties
niagara.signing.profileType=com.tridium.gradle.plugins.signing.profile.JarSignerSigningProfile
jarsigner.cmd=jarsigner
jarsigner.args+=-keystore
jarsigner.args+=NONE
jarsigner.args+=-storetype
jarsigner.args+=PKCS11
...
```

### Workbench Jar Signer Tool
Alternative GUI para firmar un JAR post-build. Usa el mismo keystore que el LocalSigningProfile. No registra nada en un signers registry externo — la firma va embebida en el JAR mismo (estándar Java `META-INF/*.SF` + `*.RSA`/`*.DSA`).

### Timestamping (RFC 3161)
Opcional. Permite validar signatures después que expire el cert de signing. Config en el profile:
```xml
<entry key="niagara.signing.standardtsa">http://timestamp.digicert.com</entry>
```
Requires internet en build time, no en runtime.

### `nverify.exe` en `bin/`
Herramienta CLI de Tridium para validar cert chains de modules. Útil para debug sin bootear Niagara. (No exploré input/output exacto — TODO).

### Establecer trust para certs custom
3 opciones (ordenadas por cost):
1. Comprar CA-signed cert de una root CA comercial (costoso, auto-trusted en todas las installs)
2. Internal CA: crear una CA propia, firmar los dev certs con ella, importar el CA cert al user trust store de cada station
3. Self-signed: importar el cert dev directamente al user trust store de cada station (no escala)

**En production con verification mode `high`** (futuro default), los self-signed serán rechazados automáticamente. Obligarán opción 1 o 2.

---

## 3.11 Pipeline completo: cargar un módulo con `<permissions>`

Integrando todo, este es el flow end-to-end (decompilado + documentado):

```
1. Boot JVM
   ├─ Load bin/policy/java.security (config crypto providers, algoritmos, etc.)
   │    └─ Verify signature PKCS7/CMS → fail = abort
   ├─ Load bin/policy/java.policy (grants estáticos a nre.jar, niagarad.jar, etc.)
   │    └─ Verify signature → fail = abort
   └─ Load bin/policy/signing.properties (hardcoded trust anchor cert)

2. NRE bootstrap
   ├─ Inicializa Security Manager (NiagaraPolicy instance)
   ├─ Carga license DB (/security/licenses/*.license validadas contra certificates/)
   └─ Module registry rebuild (si changed)

3. Station boot — loop sobre cada module:
   a. Parse module.xml → extract <permissions>
   b. Check if module.hasRequestedPermissions()
   c. Compute requiresSignature = any NiagaraPermissionGroup.requiresSignature() == true
   d. Check SKIP_MODULE_VALIDATION holder:
      - system property niagara.classLoader.skipModuleValidation=true?
      - AND Tridium:developer feature with skipModuleValidation="true"?
      - If both → SKIP_MODULE_VALIDATION = true
   e. validateCertChain = requiresSignature && !SKIP_MODULE_VALIDATION
   f. If validateCertChain:
      - Extract cert chain from JAR signature
      - Run CertificateChainValidator.validate(chain)
         ├─ Chain integrity check
         ├─ Signature verification per cert
         ├─ Validity window check
         ├─ KeyUsage + ExtendedKeyUsage (codeSigning OID)
         ├─ BasicConstraints CA bits
         └─ Trust anchor match → hardcoded cert OR user trust store
      - Si FAIL → module REJECTED, log warning, station may halt on hard-halt permissions
   g. Si validateCertChain == false (skip active or no signature required):
      - Log warning "module validation is disabled"
      - Bypass validation → permissions granted without cert check
   h. Register <permissions> block as PermissionCollection en NiagaraPolicy
   i. Module ready to load classes

4. Runtime
   ├─ Any sensitive operation → SecurityManager.checkPermission()
   ├─ NiagaraPolicy.implies(codeSource, permission) → check grants
   └─ If no grant → AccessControlException
```

### Los 3 puntos donde podés romper la chain
1. **No `<permissions>` declarado** → módulo sin permissions sensibles puede cargar sin cert check, pero **no puede hacer** networking/reflection/etc. en runtime.
2. **Permisos pedidos pero firma inválida** → module REJECTED en validation.
3. **Permisos pedidos + firma válida contra trust anchor correcto** → granted, funciona.

---

## 3.12 El bypass `skipModuleValidation` — hallazgo crítico

### Evidencia decompilada
Archivo: `com.tridium.sys.module.ModuleClassLoader.java` (módulo `baja`, vineflower + procyon + decompiled — idéntico en las 3 decompilaciones).

Líneas clave (88-96, constructor):
```java
boolean requiresSignature = false;
if (module.hasRequestedPermissions()) {
    requiresSignature = module.getRequestedNiagaraPermissions().stream()
        .anyMatch(NiagaraPermissionGroup::requiresSignature);
}
this.validateCertChain = requiresSignature
    && !ModuleClassLoader.SkipModuleValidationHolder.SKIP_MODULE_VALIDATION;
if (requiresSignature && !this.validateCertChain) {
    log.warning("[" + module.getModulePartName() + "]: module validation is disabled");
}
```

Líneas 543-569 (static holder):
```java
private static boolean loadSkipModuleValidation() {
    boolean skipRequested = AccessController.doPrivileged(
        () -> Boolean.getBoolean("niagara.classLoader.skipModuleValidation"));
    if (!skipRequested) return false;
    try {
        Feature feature = Sys.getLicenseManager().checkFeature("tridium", "developer");
        boolean skip = feature.getb("skipModuleValidation", false);
        if (!skip) throw new FeatureNotLicensedException(
            "feature 'developer' missing 'skipModuleValidation' attribute");
        log.warning("*********************************************");
        log.warning("**** Module validation has been DISABLED ****");
        log.warning("*********************************************");
        return true;
    } catch (...) { return false; }
}
```

### Qué significa
Existe un bypass **legal, documentado en código, license-gated** para desactivar la cert chain validation completamente.

**Requisitos (AMBOS necesarios)**:
1. System property `niagara.classLoader.skipModuleValidation=true` (JVM arg o `etc/system.properties`)
2. License feature `Tridium:developer` con attribute `skipModuleValidation="true"`

### Estado en la distribución Honeywell OptimizerSupervisor
| Condición | Estado |
|-----------|--------|
| License feature `developer` con `skipModuleValidation="true"` | ✅ **YA PRESENTE** (Webs.license línea 40) |
| System property activa | ❌ **NO seteada** en defaults visibles |

### Implicaciones prácticas
- Si se activara la system property, el lockdown OEM se **desactiva**. El módulo `httpClientGAngeles` de la saga httpapi cargaría con sus permissions.
- El log mostraría `"**** Module validation has been DISABLED ****"` en las primeras líneas del boot — un chequeo trivial para confirmar estado.
- Esto es comportamiento **intencional y documentado en código** — no es una vulnerabilidad, es un toggle de desarrollo.
- NO desarma la validación de `bin/policy/*.policy` (integrity check PKCS7/CMS al boot de JVM) — esas son capas independientes. El skip aplica SOLO al module class loader de `com.tridium.sys.module`.

### Matiz sobre la conclusión de la sesión httpapi
La sesión anterior concluyó: "Honeywell OptimizerSupervisor no deja firmar módulos custom con permissions elevadas". Eso es verdadero **con configuración por default**. La formulación más precisa es: "Con configuración por default, la distribución bloquea módulos custom con permissions. Existe un toggle license-gated (`developer` feature + system property) que desactiva esa validación — presente pero no activado por default".

**NO invalida la decisión de hacer rollback** — activar el toggle en una distribución productiva no es correcto si el objetivo es mantener la postura de seguridad. Pero SÍ cambia la discusión cuando el objetivo es "poder desarrollar y testear módulos custom en esta máquina específica".

---

## 3.13 Users, Roles, Categories, Permissions — el OTRO modelo

El otro sistema de permissions (que protege datos runtime, no código). Lo trato breve porque no es el foco de la saga httpapi.

### Cadena conceptual
1. **BUser** representa un principal (humano o machine account). Stored via `BUserService`.
2. **BAuthenticationService** determina los schemes disponibles (Digest, Basic, SCRAM-SHA, SessionId, etc.).
3. **Login** autentica → el user queda asociado a una sesión.
4. Los objetos protegidos (BComponent, BIFile, BIHistory) implementan **`BIProtected`** y tienen un **`BCategoryMask`** (bitmask de categorías, ej. `"a"` = cat 2+4).
5. Cada user tiene roles, y cada role tiene un **`BPermissionsMap`** que mappea categorías → permissions concretos.
6. **Permissions concretos**: 6 niveles = 2 (Operator vs Admin) × 3 (Read/Write/Invoke).

### Los 6 permissions
- `operatorRead`, `operatorWrite`, `operatorInvoke`
- `adminRead`, `adminWrite`, `adminInvoke`

Un slot de BComponent declara si es operator-level o admin-level vía `Flags.OPERATOR`. La operación (read/write/invoke) contra el slot requiere el correspondiente permission.

### 3 puntos de check automático
1. **BComponent modification methods** (`set()`, `add()`, `remove()`, `invoke()`, etc.) — si se pasa un Context con BUser no-null, valida permission.
2. **Fox traffic** — server-side valida automáticamente antes de enviar data. Slots read-deny NO aparecen en la response al client.
3. **Workbench views** — cada view declara `requiredPermissions` en module-include agent tag.

### Super user
`BPermissionsMap.SUPER_USER` — grant todos los permisos en todas las categorías. Útil para admin accounts.

### Auditing
Todas las modificaciones con Context+BUser se loggean al history-stored audit trail (feature `alarmArchive` + `audit` en el license). Propiedades auditables: property changed/added/removed/renamed/reordered + action invoked.

---

## 3.14 Consecuencias prácticas (Bloque 3)

1. **Niagara 4 es "deny-by-default"**: tu módulo arranca sin ningún permission. Tenés que declarar explícitamente todo.
2. **Source format ≠ runtime format**: editás `module-permissions.xml` con `<req-permission>` + `<name>`. El gradle traduce a `<java-permission>` concretos en `module.xml`. No confundir.
3. **60%+ de los módulos en tu corpus declaran `<permissions>`**. No es una rareza, es lo normal — pero está bajo lockdown via cert trust anchor.
4. **El lockdown OEM Honeywell = 1 solo cert hardcoded** (`Niagara4Modules Code Signing` by `Honeywell CodeSign RSA CA`, expires year 9999). Todos los módulos con permissions deben encadenar a él, o usar el bypass `skipModuleValidation`.
5. **`skipModuleValidation` existe y está license-activable**. Requiere system property + feature `developer`. Ambas condiciones cumplibles en esta distribución.
6. **REFLECTION y HSM_SIGNING causan HARD HALT** si module no firmado. El resto de permission groups solo fallan AccessControlException en runtime. Error mode difiere.
7. **Las 3 capas de validación** (policy file integrity, cert chain, user/role permissions) son INDEPENDIENTES. Desarmar una no desarma las otras. El skipModuleValidation aplica solo al ModuleClassLoader.
8. **`NiagaraSocketPermission "*:1-100000" "accept,connect,listen,resolve"` es la entry UNIVERSAL** para networking. Lo que cambia entre módulos es el firmante, no la entry.
9. **Disable del Security Manager completo** es posible con feature `smDeveloperMode` + system property `niagara.security.manager.disable=true`. Más drástico que skipModuleValidation (afecta TODO, no solo module loading).
10. **User/Role/Category permissions NO son lo mismo que module permissions**. El primero protege operaciones sobre datos; el segundo protege capacidades del código. Dos sistemas ortogonales.

---

## Topic keys engram (referencias cruzadas)

- `niagara/security/complete-model` — este bloque completo
- `niagara/estructura/framework` (Bloque 1)
- `niagara/licensing/model` (Bloque 2)
- `niagara/security/skip-module-validation` — hallazgo crítico destacado
- `niagara/honeywell-oem-signing-lockdown` (httpapi #302)
- `niagara/httpclient-permission-model` (httpapi)
- `niagara/cert-chain-validation-broken` (httpapi #304)

## Archivos leídos (Bloque 3)
- `niagara-help/devguide-clean/security/{security,securityManager,requestingPermissions,codeSigning}.txt`
- `bin/policy/signing.properties` (completo)
- `bin/policy/java.policy` (first 50 + tail 8 lines)
- `bin/policy/java.security` (first 50 lines)
- `baja/vineflower/com/tridium/sys/module/ModuleClassLoader.java` (líneas 70-200 + 530-600)
- Agregados del module-navigator: `permissions_declared` (927 scanned, 563 con permissions, 4806 entries)
