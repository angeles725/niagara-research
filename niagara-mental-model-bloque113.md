# Bloque 113 — Hardening del **module signing**: las 3 palancas estructurales (`skipModuleValidation` · truststore/trust anchor *Angeles* · clave **RSA-1024**) — la capa de PREVENCIÓN profunda del [Bloque 75]

> Investigación de **prevención DEFENSIVA**, tercer movimiento del arco de seguridad. El [Bloque 75] reconstruyó el **vector** (módulo no firmado abre 443 + borra audit) y propuso un hardening de **configuración** (P0.1 `moduleVerificationMode=high`). El [Bloque 112] aportó la **detección/forense** (dashboard + `console.log` del daemon + PolicySpy). Este bloque baja una capa más: las **tres palancas estructurales del code-signing** que sobreviven *aunque* subas `moduleVerificationMode=high`, porque no son config de modo sino mecanismos del propio pipeline de firma. Si quedan abiertas, un atacante con acceso adecuado **neutraliza el gate de firma desde abajo** sin tocar el modo.
>
> **Qué es** `[CERT]`: tres mecanismos reales del runtime de firma, uno por sección. (1) **`skipModuleValidation`** — la palanca que **desactiva la validación de la cadena de certificados** del classloader; vive en un *holder* lazy y exige **sysprop + feature de licencia**. (2) **El truststore** (`ITrustStore`/`ICryptoManager`) — el **trust anchor** contra el que toda cadena de módulo se valida; hoy el cert activo es **Angeles** (la nomenclatura **SEJOFA está deprecada**), materializado en `cacerts.bks`/`ssl.tks` servidos por el platform daemon. (3) **La clave RSA-1024** — la opción de tamaño débil que el diálogo de generación de certs (incluidos los de *code-signing*) todavía ofrece con un simple click de warning.
>
> **El hallazgo de anclaje** `[CERT]`: el gate de firma de un módulo Niagara N4.14 se sostiene sobre **tres supuestos frágiles que no controla `moduleVerificationMode`**: (a) que `skipModuleValidation` esté **inactivo** —pero **no está en el blacklist de propiedades CLI por default** (`Nre.java:166,844`)—; (b) que el **trust anchor** del truststore sea exactamente el cert *Angeles* y no haya entradas espurias —el truststore es **mutable por alias** (`ITrustStore.setCertificateEntry/deleteEntry`)—; y (c) que la clave del cert de firma sea fuerte —pero el diálogo permite **RSA-1024** con un click (`BSelfSignedDialog.java:488-501`)—. Subir el modo a `high` cierra el camino *fail-open* del B75, pero **no** toca ninguna de estas tres. La prevención completa exige endurecer las tres palancas, no solo el modo.
>
> **Corrección de scoping incorporada** `[CERT]`: el “Vector 3 — DSA-1024” del encargo es, en el código real, **RSA-1024**: el diálogo de certs usa `new NRsaKeyPairGenerator(keySize)` (`BSelfSignedDialog.java:676`), **no DSA**. El `SignatureDSA` real (`samlEncryption`) es de **firma XML/SAML** (`URI dsa-sha1`), un dominio distinto del code-signing de módulos. Y el truststore en disco es **`.bks`/`.tks` (BouncyCastle)**, no `truststore.jks`. Ambos detalles se documentan abajo como refinamientos verificados.
>
> Fuentes READ-ONLY (vineflower): `baja/.../com/tridium/sys/module/{ModuleClassLoader,ModuleManager}.java` + `.../com/tridium/sys/Nre.java`; `baja/.../javax/baja/security/crypto/{ITrustStore,ICryptoManager,X509CertificateFactory}.java` + `.../javax/baja/security/{BX509Certificate,BCertificateAliasCredential}.java`; `platform/platform-rt/.../com/tridium/install/ModuleSignatureStatusEnum.java`; `platCrypto/platCrypto-rt/.../daemon/BPlatCryptoManager.java`; `platCrypto/platCrypto-wb/.../ui/BSelfSignedDialog.java`; `backup/backup-rt/.../BAxOfflineBackup.java`; `samlEncryption/.../algorithms/implementations/SignatureDSA.java`.
> Método: **verificación directa file:line** del holder lazy, del enforcement del blacklist, del flujo de validación de cadena, del backup-set de seguridad y del diálogo de generación de certs. `[CERT]` = leído verbatim; `[INFER]` = deducción marcada. Conteo honesto: **~11 clases vineflower** cross-módulo (baja, platform-rt, platCrypto-rt/-wb, backup-rt, samlEncryption).
>
> Capa 21 (Test infrastructure + Security incident), **arco B75→B112→B113**: B75 = vector + hardening de config; B112 = detección + forense; **B113 = hardening estructural del pipeline de firma**. Cross-ref fuerte: [Bloque 75] (ancla — vector + P0/P1/P2), [Bloque 112] (detección — dos gates de firma), [Bloque 18] (module signing + 25 permission groups), [Bloque 17] (truststore en disco), [Bloque 26] (standalone signing playbook), [Bloque 30] (FIPS + key rotation).

---

## 113.0 — El reframe: tres palancas que `moduleVerificationMode=high` NO cierra `[CERT]`

El B75 §75.4 entregó como P0.1 el quick-win **`moduleVerificationMode=high`** (`defaults/system.properties:442`, default `low` en este OEM). Es correcto y necesario: cierra el *fail-open* del gate de carga (`ModuleSignatureStatusEnum.isAcceptable`, abajo 113.2.3). Pero el gate de firma se apoya en tres mecanismos que **no son el modo** y que el atacante puede atacar por debajo:

| Palanca | Qué controla | ¿La cierra `moduleVerificationMode=high`? | Dónde vive |
|---|---|---|---|
| `skipModuleValidation` | Desactiva **toda** la validación de cadena del classloader | **NO** — es un short-circuit *previo* al modo | `ModuleClassLoader.java:93` |
| Truststore / trust anchor *Angeles* | Define **contra qué** se valida la cadena | **NO** — el modo decide *si* validar, no *contra quién* | `ICryptoManager`, `cacerts.bks` |
| Clave RSA-1024 del cert de firma | Fuerza criptográfica del propio anchor/firmante | **NO** — un cert 1024 válido encadena igual | `BSelfSignedDialog.java:676` |

La tesis del bloque `[CERT]`: **la prevención del B75 es necesaria pero no suficiente**. Cerrar el modo sin blindar estas tres deja tres maneras de volver al estado vulnerable: desactivar la validación entera (palanca 1), envenenar el anchor (palanca 2), o firmar con una clave rompible (palanca 3). Este bloque las documenta y da el blindaje accionable de cada una.

---

## 113.1 — Palanca 1: `skipModuleValidation` — el short-circuit total de la validación `[CERT]`

Ya nombrado en B75 §75.4 P0.3 (“blindar `skipModuleValidation` → blacklist”) y B112 §112.2.3 (una de las dos palancas de bypass). Acá se verifica **el código exacto**: cómo se activa, qué desactiva, y cómo blindarlo dado que **no se puede quitar de la licencia**.

### 113.1.1 — Doble gate de activación: **sysprop AND feature de licencia** `[CERT]`

`ModuleClassLoader.loadSkipModuleValidation()` (`ModuleClassLoader.java:543-570`) exige **dos condiciones simultáneas** `[CERT]`:

```java
boolean skipModuleValidationRequested = Boolean.getBoolean("niagara.classLoader.skipModuleValidation");  // :545-547
if (!skipModuleValidationRequested) { skipModuleValidation = false; }                                      // :548-549
else {
   Feature feature = Sys.getLicenseManager().checkFeature("tridium", "developer");                         // :552
   skipModuleValidation = feature.getb("skipModuleValidation", false);                                     // :553
   if (!skipModuleValidation)
      throw new FeatureNotLicensedException("feature 'developer' missing 'skipModuleValidation' attribute");// :555
}
```

**Refinamiento al scoping** `[CERT]`: NO es una *feature* `skipModuleValidation` standalone en la `.license`. Es un **atributo booleano `skipModuleValidation` dentro de la feature `developer`** (vendor `tridium`). La activación requiere **ambos**: (1) el sysprop `niagara.classLoader.skipModuleValidation=true` y (2) que la licencia tenga `developer{ skipModuleValidation=true }`. Si falta cualquiera → `false`. *(B75 §75.4 P0.3 localizó el feature en `Webs.license:40` en el install vivo; el corpus vineflower confirma el **mecanismo de lectura**, no el literal de la licencia — que vive en el install, no en el código.)*

Cuando se activa, el método **grita en el log** `[CERT]` (`:564-566`):
```
*********************************************
**** Module validation has been DISABLED ****
*********************************************
```
→ un **IOC ruidoso** (logger del classloader). Es la confirmación post-incidente de que esta palanca se usó.

### 113.1.2 — Qué desactiva exactamente, y el matiz “módulo activo vs. desactivo” `[CERT]`

El valor vive en un **holder lazy de inicialización única** (`ModuleClassLoader.java:696-698`):
```java
private static class SkipModuleValidationHolder {
   public static final Boolean SKIP_MODULE_VALIDATION = ModuleClassLoader.loadSkipModuleValidation();
}
```
Se evalúa **una sola vez** (al primer acceso, i.e. primera carga de módulo) y queda **cacheado para toda la vida de la JVM** `[CERT]`. No hay re-lectura: encender/apagar exige reiniciar la station.

El efecto puntual está en el constructor (`:88-96`) `[CERT]`:
```java
boolean requiresSignature = false;
if (module.hasRequestedPermissions())
   requiresSignature = module.getRequestedNiagaraPermissions().stream().anyMatch(NiagaraPermissionGroup::requiresSignature);  // :89-90
this.validateCertChain = requiresSignature && !SkipModuleValidationHolder.SKIP_MODULE_VALIDATION;  // :93
if (requiresSignature && !this.validateCertChain)
   log.warning("[" + module.getModulePartName() + "]: module validation is disabled");            // :94-95
```

El matiz **“módulo activo vs. desactivo”** se resuelve acá `[CERT]`: `skipModuleValidation` **solo importa para módulos que `requiresSignature`** — esto es, módulos que **declaran al menos un permission group cuyo `requiresSignature()==true`** (`:90`). Para un módulo *sin* permisos que exijan firma, `validateCertChain` ya es `false` por la izquierda del `&&` y la palanca es irrelevante. Es decir: la palanca **desactiva la validación de cadena precisamente para los módulos peligrosos** (los que piden permisos que normalmente forzarían firma) — exactamente la clase a la que pertenece el módulo malicioso del B75. Es la peor combinación posible: apaga el control donde más importa. Y emite por cada módulo afectado un `"[<modulo>]: module validation is disabled"` (`:95`) — IOC por-módulo.

### 113.1.3 — Blindaje: `niagara.commandLinePropertyBlacklist` (mecanismo real + el gap) `[CERT]`

Como la feature **no se puede quitar de la licencia** (B75 P0.3), el blindaje es **bloquear el sysprop por CLI**. El mecanismo está en `Nre.addToSystemProperties()` (`Nre.java:839-871`) `[CERT]`:

```java
boolean hasSystemPropsWriteAccess = hasWritePermissionsToSystemPropertiesFile(systemPropertiesFile);  // :840
if (!hasSystemPropsWriteAccess) {                                                                      // :841
   String blacklistProperty = props.getProperty("niagara.commandLinePropertyBlacklist",
        "niagara.moduleVerificationMode,program.requireSigning,...");                                   // :842-845
   for (String blacklistedProp : blacklistProperty.split(",")) {                                        // :847
      if (System.getProperty(p) != null || System.getProperty("cmdline::" + p) != null) {               // :851-852
         System.clearProperty(p); System.clearProperty("cmdline::" + p);                                // :853-854
         sysLog.warning("Ignoring command line property <" + p + ">. User does not have permissions..."); // :855
      }
   }
}
```

Tres hechos verificados, dos de ellos **gaps de seguridad** `[CERT]`:

1. **El nombre real de la propiedad es `niagara.commandLinePropertyBlacklist`** (`Nre.java:843`, default duplicado en la constante `DEFAULT_COMMAND_LINE_BLACKLIST`, `Nre.java:166`). No “commandLineBlacklist”.
2. **`skipModuleValidation` NO está en el blacklist por default** `[CERT, gap]`. El default contiene: `niagara.moduleVerificationMode`, `program.requireSigning`, `niagara.export.preventCSVInjection`, `niagara.webbrowser.disable`, `niagara.webbrowser.urlWhitelist`, `niagara.baja.formatBlacklist`, `niagara.baja.formatBlacklistExclusions`, `jdk.tls.rejectClientInitiatedRenegotiation` (`:166,844`). Nótese que **`niagara.moduleVerificationMode` SÍ está protegido** (por eso el P0.1 del B75, una vez fijado en `system.properties`, no se puede degradar por CLI) — pero **ni `niagara.classLoader.skipModuleValidation` ni `niagara.commissioning.ignoreVerificationMode`** (la palanca del install-gate, B112 §112.2.3) figuran. **Hay que agregarlos a mano.**
3. **El blacklist solo aplica si el usuario NO tiene permiso de escritura sobre `system.properties`** (`:841`, guard `if (!hasSystemPropsWriteAccess)`) `[CERT, caveat]`. Un atacante con **acceso de escritura al filesystem de `system.properties`** lo saltea: pondría la propiedad directamente en el archivo en vez de pasarla por CLI. El blacklist es defensa contra *override por línea de comandos*, no contra *control del filesystem de la station*.

**Timing del blindaje** `[CERT]`: el clear de `Nre` ocurre en el arranque del runtime; el `SkipModuleValidationHolder` lee el sysprop **lazy, en la primera carga de módulo** (posterior) → el clear es **efectivo**: cuando el holder consulta `Boolean.getBoolean(...)`, la propiedad ya fue borrada. El orden temporal favorece la defensa.

> **Mitigación 1** `[CERT]`: en `!system/system.properties`, **extender** `niagara.commandLinePropertyBlacklist` con `niagara.classLoader.skipModuleValidation` **y** `niagara.commissioning.ignoreVerificationMode` (cierra B75 P0.3 + B112 §112.2.3 en un solo lugar). Complementar con control del filesystem de `system.properties` (palanca CLI ≠ palanca filesystem). Y **alertar** ante el banner `**** Module validation has been DISABLED ****` o `module validation is disabled` en el log del classloader.

---

## 113.2 — Palanca 2: el **truststore** / trust anchor (*Angeles*) — contra qué se valida la cadena `[CERT]`

Subir el modo decide *si* se valida la cadena; el **truststore** decide *contra qué se valida*. Si el anchor es el incorrecto —o si el store admite entradas espurias— la firma “válida” deja de significar lo que el defensor cree.

### 113.2.1 — La interfaz: `ITrustStore` (mutable por alias) + `ICryptoManager` (tres stores) `[CERT]`

`ITrustStore` (`javax/baja/security/crypto/ITrustStore.java`) es un **almacén de certificados keyed-by-alias, mutable** `[CERT]`:

| Método | Línea | Rol |
|---|---|---|
| `getCertificate(alias)` / `getCertificateChain(alias)` | `:20`/`:24` | lectura del anchor por alias |
| `setCertificateEntry(alias, cert)` | `:32` | **alta de un trust anchor** |
| `deleteEntry(alias)` / `deleteEntries(String[])` | `:18`/`:57` | **baja de anchors** |
| `findCertificate(cert)` / `getCertificateAlias(cert)` | `:55`/`:22` | resolver alias desde el cert |
| `getCertificateEntries()` / `aliases()` / `size()` | `:38`/`:14`/`:34` | enumeración |
| `save()` | `:36` | persistir a disco |

`ICryptoManager` (`javax/baja/security/crypto/ICryptoManager.java`) expone **tres** stores `[CERT]`, no uno:
```java
ITrustStore getUserTrustStore();       // :9   ← anchors de confianza del operador
ITrustStore getUserUntrustedStore();   // :11  ← lista de EXPLÍCITAMENTE no confiables (revocación local)
ITrustStore getSystemTrustStore();     // :13  ← anchors del sistema (incluye el de code-signing)
IKeyStore   getKeyStore();             // :15  ← claves privadas (firmar, no validar)
```

> **Implicación defensiva** `[CERT]`: el `setCertificateEntry`/`deleteEntry` significan que **agregar un trust anchor es una operación de runtime**. Endurecer = controlar **quién** puede tocar el truststore (gate de plataforma, abajo) y **auditar** altas/bajas de anchor. El `getUserUntrustedStore()` (`:11`) es la pieza menos usada: una **blocklist explícita** donde plantar un cert comprometido para que toda cadena que lo incluya falle.

### 113.2.2 — Dónde vive en disco y quién lo sirve `[CERT]`

> **⚠ CORREGIDO por [B392 §392.3-4] (evidencia de disco, 2026-08-07):** en el install vivo el trust anchor
> de **code-signing de módulos** es `security/truststore.jks` (JKS estándar, password `changeit`), con una
> sola entrada **SEJOFA VIVA** (alias `niagaramoduledev`), **no** "Angeles"; **`cacerts.bks` no existe** —
> lo que aquí se llamó `.bks` es `jre/lib/security/cacerts.bcfks` (BC-FKS FIPS, dominio **TLS**, no módulos).
> "Angeles" y el ".bks de code-signing" fueron sobre-lectura del decompilado; el `ssl.tks`/`keystore.bks` de
> abajo son del dominio **TLS/keys**, distinto del code-signing (ver los 3 dominios en [B392 §392.1]).

**On-disk** (refinamiento al scoping: **`.bks`/`.tks`, NO `truststore.jks`**) `[CERT]`. El backup-set de seguridad lo enumera literal en `BAxOfflineBackup.java:96-110`:
```java
BOrd.make("file:!security/ssl.tks"),       // :98  SSL truststore (BouncyCastle)
BOrd.make("file:!security/keystore.bks"),  // :99  keystore (claves privadas)
BOrd.make("file:!security/cacerts.bks"),   // :100 CA certs / trust anchors  ← el code-signing anchor
BOrd.make("file:!security/exemptions.tes"),// :101
BOrd.make("file:!security/.km"),           // :102 master key material
BOrd.make("file:!security/.kr"),           // :103
// + el espejo de Workbench:
BOrd.make("file:!workbench/security/cacerts.bks"), // :105
```
`file:!security/…` resuelve bajo `<niagara_home>/security/` (system) y `<niagara_home>/workbench/security/` (WB) `[CERT]`. El trust anchor de **code-signing** vive en **`cacerts.bks`** (formato **BouncyCastle**, no JKS de Sun). Esto **refina** el caveat del B75 §75.4 P0.1 y el [Bloque 17] (que hablaban de “truststore en disco” genérico / `signing.properties`): el **runtime** valida contra `cacerts.bks`; el **toolchain de firma** (lado build) usa `signing.properties` para apuntar al anchor — son **dos artefactos distintos** que deben coincidir.

**Quién lo sirve** `[CERT]`: el truststore **lo gobierna el platform daemon**, no la JVM de la station. `BPlatCryptoManager` (`platCrypto-rt/.../daemon/BPlatCryptoManager.java`) `[CERT]`:
```java
public static final String USER_TRUST_STORE   = "userTrustStore";    // :37
public static final String SYSTEM_TRUST_STORE = "systemTrustStore";  // :39
public ITrustStore getUserTrustStore()   { return new BPlatTrustStore(this.getDaemonSession(), "userTrustStore"); }   // :70
public ITrustStore getSystemTrustStore() { return new BPlatTrustStore(this.getDaemonSession(), "systemTrustStore"); } // :78
```
Cada acceso es un `BPlatTrustStore` **sobre la sesión del daemon** (`getDaemonSession()`) `[CERT]`. Implicación: tocar el anchor pasa por el **platform daemon** (mismo plano que el deploy del B75) — controlar el acceso al daemon (B75 P1: TLS-only + firewall) **también** protege el truststore.

### 113.2.3 — Qué significa “encadenar al trust anchor” en código + el enum de estado `[CERT]`

Hay **dos invocaciones** del mismo concepto, una por gate (confirma B112 §112.2.3 “dos gates de firma”):

**(a) Gate de carga** (server-side, gobernado por `moduleVerificationMode`) — `ModuleManager.verifyModuleSignature()` (`ModuleManager.java:330-404`) `[CERT]`:
```java
JarSignatureRegistry registry = Nre.getJarSignatureRegistry();          // :332
CoreCryptoManager mgr = CoreCryptoManager.get(...);                     // :334
List<CodeSigner> signers = registry.getCodeSigners(m.getFile());        // :335
for (CodeSigner signer : signers)                                       // :351
   mgr.validateCertChain(signer, m.getCheckTpk());                      // :353  ← encadenar al anchor
```

**(b) Gate de install** (client-side, `InstallScenario`) — la clasificación de estado vive en `ModuleSignatureStatusEnum.getSignatureStatus(CodeSigner, CertificateChainValidator)` (`ModuleSignatureStatusEnum.java:96-141`) `[CERT]`:
```java
List<? extends Certificate> certs = signer.getSignerCertPath().getCertificates();      // :100
if (certs.size()==1 && CertUtils.checkDnEquality(subject, issuer)) status.add(SIGNER_SELF_SIGNED); // :102-104
// ... timestamp checks (:117-126) ...
validator.validateCertChain(signer);   // :128  ← encadenar al anchor (lanza ValidationException si falla)
if (status.isEmpty()) status.add(OK);  // :129-130
} catch (ValidationException e) {
   status.add(validator.getLaxValidation() ? CERT_PATH_VALIDATION_WARNING : CERT_PATH_VALIDATION_FAILURE); // :133-136
}
```

“Encadenar al trust anchor” = **`validateCertChain(signer)`** construye la ruta desde el cert hoja del firmante hasta una raíz presente en el truststore (`cacerts.bks`). Si **no** llega a un anchor confiable → `ValidationException` → `CERT_PATH_VALIDATION_FAILURE` (o `…WARNING` con *lax validation*) `[CERT]`.

El **gate de aceptación** por modo es el switch *fall-through* (`ModuleSignatureStatusEnum.isAcceptable`, `:26-43`) `[CERT]`:

| Estado | `low` acepta | `medium` acepta | `high` acepta |
|---|---|---|---|
| `OK` | sí | sí | sí |
| `NOT_TIMESTAMPED` / `CERT_PATH_VALIDATION_WARNING` | sí | sí | sí |
| `SIGNER_SELF_SIGNED` / `TIMESTAMP_SELF_SIGNED` | sí | sí | **NO** |
| `UNSIGNED` / `CERT_PATH_VALIDATION_FAILURE` / `UNKNOWN` | sí | **NO** | **NO** |
| `INVALID_SIGNATURE` | **NO** | **NO** | **NO** |

Lectura `[CERT]`: un fallo de cadena (`CERT_PATH_VALIDATION_FAILURE`) **pasa en `low`** (el *fail-open* del B75) y se **bloquea desde `medium`**. Pero `CERT_PATH_VALIDATION_WARNING` (lax validation) **pasa siempre** — un detalle a vigilar: si el validador corre en modo *lax* (`getLaxValidation()==true`, `:133`), un anchor problemático degrada a warning aceptable en cualquier modo.

### 113.2.4 — Política *Angeles* (actual) vs *SEJOFA* (deprecado) + el bypass `license.unreleasedSoftware` `[CERT/OPS]`

**Política del trust anchor** `[OPS, CERT-adyacente]`: el cert de code-signing en uso **hoy es Angeles**. **SEJOFA es nomenclatura vieja/deprecada** y no debe asumirse como el anchor activo. En el **código** decompilado **no hay literal `Angeles` ni `SEJOFA`** —los alias viven en el `cacerts.bks` del install y en `signing.properties` del toolchain, no en clases— por eso aparecen como `[OPS]`, no `[CERT]`. Lo que sí es `[CERT]`: el plumbing es **alias-based** (`ITrustStore.getCertificate(alias)`, `BCertificateAliasCredential.certificateAlias` `:56`/`getCertificateAlias()` `:74`) y existe un `CertUtils.LEGACY_CERT_ALIAS` referenciado en `web-rt`/`opc-rt` — coherente con un anchor **legacy** que se reemplaza. La acción de hardening: confirmar que el alias del anchor activo en `cacerts.bks` es **Angeles**, retirar cualquier entrada *SEJOFA* residual (`deleteEntry`, `:18`), y verificar que `signing.properties` (build) apunta al **mismo** Angeles que el runtime valida.

**Bypass adicional descubierto** `[CERT]`: `ModuleSignatureStatusEnum.getSignatureStatus(...)` (`:106-115`) lee la propiedad de licencia **`license.unreleasedSoftware`**:
```java
String allowUnreleasedSoftware = SubscriptionLicenseUtil.getLicenseProperties().getProperty("license.unreleasedSoftware", "false"); // :106
if (Boolean.parseBoolean(allowUnreleasedSoftware) && certificates.size() < 4) {                                                      // :107
   for (cert : certificates) if (checkDnEquality(subject, issuer)) { status.add(SIGNER_SELF_SIGNED); return status; }                // :108-114
}
```
Con `license.unreleasedSoftware=true` y cadena corta (<4), un cert **self-signed** se clasifica como `SIGNER_SELF_SIGNED` (aceptable hasta `medium`) en lugar de fallar la validación de cadena. Es una **tercera palanca de licencia** —junto a `developer{skipModuleValidation}` y `smDeveloperMode` (`Nre.java:973`, B75 `Webs.license:129`)— que relaja la firma. No bypassea `high` (que rechaza `SIGNER_SELF_SIGNED`), pero sí `low`/`medium`: otro motivo para **P0.1 = high**.

> **Mitigación 2** `[CERT]`: (a) confirmar el anchor **Angeles** en `cacerts.bks` y purgar SEJOFA residual; (b) restringir acceso al **platform daemon** (sirve el truststore — reusa B75 P1); (c) auditar altas/bajas vía `ITrustStore.setCertificateEntry/deleteEntry`; (d) verificar que **no** corra `lax validation` ni `license.unreleasedSoftware=true` en producción; (e) usar `getUserUntrustedStore()` para plantar explícitamente certs revocados.

---

## 113.3 — Palanca 3: la clave débil **RSA-1024** del diálogo de generación de certs `[CERT]`

Nuevo respecto de B75/B112. Foco: el diálogo de Workbench que genera certs self-signed —incluidos los de **code-signing**— y que **todavía ofrece clave de 1024 bits**.

### 113.3.1 — La opción 1024 + el warning click-through + (corrección) **es RSA, no DSA** `[CERT]`

`BSelfSignedDialog` (`platCrypto/platCrypto-wb/.../ui/BSelfSignedDialog.java`) tiene cuatro radios de tamaño (`:107-110`): `keySize1024/2048/3072/4096`, default **2048** (`:125`, seleccionado en `:253,261`). Cuando el usuario elige 1024, `updateKeySize()` (`:487-509`) abre un **diálogo de warning** y, si no se cancela, fija la clave en 1024 `[CERT]`:
```java
if (this.keySize1024 != null && this.keySize1024.getSelected()) {                       // :488
   if (BDialog.open(this, lex.getText("cert.generate.warning.weakKeySize"),
                    lex.getText("cert.generate.warning.weakKeySize.description"),
                    12, BDialog.WARNING_ICON, null) == 8) { return; }                    // :489-499 (cancela)
   this.keySize = 1024;                                                                  // :501
}
```
Es un **warning click-through**, no un bloqueo: la opción débil sigue disponible con una confirmación. Y el cert puede ser de **code-signing** — el mismo diálogo ofrece `codeSigningCert`→`KeyPurpose.CODE_SIGNING_CERT` (`:114,518-519`). Es decir: **se puede generar un cert de firma de módulos con clave de 1024 bits** aceptando un aviso.

**Corrección al scoping (Vector 3 = RSA, no DSA)** `[CERT]`: el material de clave lo genera **RSA**, no DSA. Imports (`:6-7`) `NKeyPairGenerator`/`NRsaKeyPairGenerator`, y la generación (`:676`) `[CERT]`:
```java
NKeyPairGenerator generator = new NRsaKeyPairGenerator(this.keySize);                    // :676
... mgr.generateSelfSignedCert(builder, generator, existingPasswordChars, pkPasswordChars); // :687
```
Por lo tanto la palanca débil es **RSA-1024**. **RSA-1024 es débil** por margen estrecho frente a factorización moderna (NIST lo retiró para uso general desde 2013; equivalente a <80 bits de seguridad simétrica). Para un cert de **code-signing** es especialmente grave: comprometer la clave permitiría **firmar módulos maliciosos que encadenan a un anchor confiable** — derrotando *toda* la cadena de los 113.2/113.1 de un solo golpe.

### 113.3.2 — La defensa ya existe en parte: **FIPS suprime el 1024** `[CERT]`

Hallazgo no trivial: la opción de 1024 **solo se construye fuera de modo FIPS** (`:248-251`) `[CERT]`:
```java
if (!SecurityInitializer.getInstance().isFips()) {                       // :248
   this.keySize1024 = new BRadioButton(group, lex.getText("cert.field.1024bits"), false);  // :249
   keyGrid.add(null, this.keySize1024);                                  // :250
}
```
En **modo FIPS** el radio de 1024 **ni se renderiza** → la palanca débil desaparece estructuralmente (no por warning, sino por ausencia). Conecta con el [Bloque 30] (FIPS): activar FIPS no es solo un requisito de compliance — **elimina la opción de clave débil** en la UI de generación de certs. (2048/3072/4096 permanecen.)

### 113.3.3 — Contexto: el `SignatureDSA` real es firma XML/SAML, no de módulos `[CERT]`

Para cerrar la confusión DSA: el único `DSA` del corpus de firma es `org.apache.xml.security.algorithms.implementations.SignatureDSA` (`samlEncryption-rt`), la implementación **Apache Santuario** de XML-DSig `[CERT]`:
```java
public class SignatureDSA extends SignatureAlgorithmSpi {                       // :22
   public static final String URI = "http://www.w3.org/2000/09/xmldsig#dsa-sha1"; // :23
```
Es **firma de documentos XML (SAML/SSO)**, dominio totalmente distinto del code-signing de módulos (que es RSA). DSA-SHA1 ahí es legacy del estándar XML-DSig, no una opción que afecte la firma de módulos. **No mezclar** los dos dominios: el hardening de módulos es 113.3.1-113.3.2 (RSA, FIPS); el XML-DSig es problema del stack SAML.

> **Mitigación 3** `[CERT]`: (a) **prohibir RSA-1024** para code-signing — operacionalmente, **activar FIPS** (`SecurityInitializer.isFips()`, suprime el radio 1024) o establecer convención/revisión de que todo cert de firma sea **≥2048** (idealmente 3072/4096); (b) auditar los certs existentes en `keystore.bks`/`cacerts.bks` por `keySize<2048` y rotarlos (reusa [Bloque 30] key rotation); (c) tratar la clave del anchor *Angeles* como el activo de mayor valor — su fuerza es el techo de seguridad de las palancas 1 y 2.

---

## 113.4 — Hardening checklist / mitigaciones accionables (los 3 vectores) `[CERT]`

| # | Palanca | Acción de hardening | Dónde se aplica | Evidencia file:line |
|---|---|---|---|---|
| H1 | `skipModuleValidation` | Agregar `niagara.classLoader.skipModuleValidation` **y** `niagara.commissioning.ignoreVerificationMode` a `niagara.commandLinePropertyBlacklist` | `!system/system.properties` | `Nre.java:166,843-855` |
| H2 | `skipModuleValidation` | Restringir escritura del filesystem de `system.properties` (el blacklist solo cubre el override CLI, no el FS) | OS / permisos de archivo | `Nre.java:841` |
| H3 | `skipModuleValidation` | Alertar ante `**** Module validation has been DISABLED ****` y `[<mod>]: module validation is disabled` | log del classloader | `ModuleClassLoader.java:95,564-566` |
| H4 | Truststore | Confirmar anchor **Angeles** en `cacerts.bks`; purgar entradas **SEJOFA** residuales (`deleteEntry`) | `<niagara_home>/security/cacerts.bks` | `BAxOfflineBackup.java:100`; `ITrustStore.java:18` |
| H5 | Truststore | Verificar que `signing.properties` (build) apunta al **mismo** Angeles que valida el runtime | toolchain de firma vs runtime | B75 §75.4 P0.1; B17 |
| H6 | Truststore | Restringir acceso al **platform daemon** (sirve el truststore por sesión) — reusar B75 P1 (TLS-only + firewall) | daemon | `BPlatCryptoManager.java:70,78` |
| H7 | Truststore | Auditar altas/bajas de anchor; plantar revocados en `getUserUntrustedStore()` | runtime crypto manager | `ICryptoManager.java:11`; `ITrustStore.java:32,57` |
| H8 | Truststore | Confirmar que NO corre *lax validation* ni `license.unreleasedSoftware=true` en producción | licencia / validador | `ModuleSignatureStatusEnum.java:106-115,133` |
| H9 | RSA-1024 | **Activar FIPS** → suprime el radio de 1024 en el diálogo de certs | `SecurityInitializer.isFips()` | `BSelfSignedDialog.java:248-251` |
| H10 | RSA-1024 | Auditar `keystore.bks`/`cacerts.bks` por claves `<2048` y rotarlas (especialmente el code-signing) | key stores | `BSelfSignedDialog.java:676`; B30 |
| H11 | Config base | Mantener **`moduleVerificationMode=high`** (B75 P0.1) — es la condición que hace que `CERT_PATH_VALIDATION_FAILURE`/`UNSIGNED`/`SIGNER_SELF_SIGNED` se bloqueen | `system.properties:442` | `ModuleSignatureStatusEnum.java:26-43` |

**Orden de prioridad** `[CERT]`: H11 (modo high, ya en B75 P0.1) es la base; sobre ella, **H1** (blacklist) cierra el bypass más directo (desactivar todo), **H4-H6** aseguran que “firmado” signifique “Angeles”, y **H9-H10** garantizan que el firmante no sea rompible. Las tres palancas son **AND**: dejar una abierta degrada las otras dos.

---

## 113.5 — Hallazgos CERT, corrigenda y cierre

**Hallazgos CERT (uno por palanca)**:
1. **`skipModuleValidation`** exige **sysprop + atributo `skipModuleValidation` de la feature `developer`** (`ModuleClassLoader.java:545-555`), vive en un **holder lazy de una sola evaluación** (`:696-698`), desactiva la validación **solo para módulos que requieren firma** (`:93`), y **NO está en el blacklist CLI por default** (`Nre.java:166,844`) — hay que agregarlo, y el blacklist solo cubre override CLI, no control del filesystem (`:841`).
2. **Truststore**: tres stores vía `ICryptoManager` (`getUserTrustStore`/`getUserUntrustedStore`/`getSystemTrustStore`, `:9-13`), **mutable por alias** (`ITrustStore.setCertificateEntry/deleteEntry`), servido por el **platform daemon** (`BPlatCryptoManager:70,78`), materializado en **`cacerts.bks`/`ssl.tks` (BouncyCastle)** bajo `<niagara_home>/security/` (`BAxOfflineBackup.java:98-105`). “Encadenar al anchor” = `validateCertChain(signer)` (`ModuleManager.java:353`, `ModuleSignatureStatusEnum.java:128`) → `CERT_PATH_VALIDATION_FAILURE` si no llega a Angeles.
3. **RSA-1024** (no DSA): el diálogo genera con `NRsaKeyPairGenerator` (`BSelfSignedDialog.java:676`), ofrece 1024 con **warning click-through** (`:488-501`) incluso para code-signing (`:518-519`), pero **FIPS lo suprime** (`:248-251`).

**Corrigenda / refinamientos al scoping y a bloques previos** `[CERT]`:
- **“Vector 3 = DSA-1024” → RSA-1024**: el diálogo de certs es RSA (`:676`); el `SignatureDSA` (`samlEncryption`) es XML-DSig SAML (`URI dsa-sha1`), otro dominio.
- **Truststore en disco = `.bks`/`.tks`, no `truststore.jks`**: refina el caveat genérico de B75 §75.4 P0.1 y del [Bloque 17]. El runtime valida contra `cacerts.bks`; `signing.properties` es el lado build.
- **`skipModuleValidation` no es feature standalone**: es atributo de la feature `developer`. Refina B75 §75.4 P0.3 (que lo trataba como feature en `Webs.license:40`).
- **Nombre real del blacklist**: `niagara.commandLinePropertyBlacklist` (no “commandLineBlacklist”).
- **Tercera palanca de licencia**: `license.unreleasedSoftware=true` relaja la validación de cadena a `SIGNER_SELF_SIGNED` (`ModuleSignatureStatusEnum.java:106-115`) — suma a `developer{skipModuleValidation}` y `smDeveloperMode`.

Sin contradicción a B75/B112: este bloque **confirma** los dos gates de firma (B112 §112.2.3) y **extiende** la prevención del B75 de la capa de *config* (modo) a la capa *estructural* (palancas del pipeline).

**Pendiente conocido**: los **literales de alias** (Angeles/SEJOFA), el contenido real de `cacerts.bks`/`signing.properties` y el tamaño de clave del anchor activo viven en el **install vivo**, no en el corpus decompilado — se confirman inspeccionando la station (recomendado para el informe). La validación empírica de que el blacklist extendido efectivamente bloquea `-Dniagara.classLoader.skipModuleValidation=true` queda como prueba de laboratorio antes de producción. El **keyring/BOG y la gestión de credenciales** (cómo se persisten las claves privadas de firma, rotación operativa) quedan **fuera** de este bloque → **[Bloque 114]** (BOG/keyring).

---

**Bloque cerrado**: 2026-06-28. Investigación READ-ONLY sobre source decompilado (vineflower) — ~11 clases cross-módulo (baja, platform-rt, platCrypto-rt/-wb, backup-rt, samlEncryption). Capa de PREVENCIÓN estructural del code-signing; tercer movimiento del arco [Bloque 75] (vector) → [Bloque 112] (detección) → **B113 (hardening)**. Capa 21. Engram: `niagara/security/b113-module-signing-hardening`.
