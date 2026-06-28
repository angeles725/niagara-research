# Bloque 114 — Cifrado de configuración y de secretos: el **BOG encryption pipeline** (`EncryptionKeySource` none/external/**keyring**) + el **KeyRing** (almacén de secrets) — la capa de PROTECCIÓN DE DATOS EN REPOSO del arco [Bloque 75]

> Investigación de **protección de datos en reposo**, cuarto y último movimiento del arco de seguridad. El [Bloque 75] reconstruyó el **vector** (módulo no firmado abre 443 + borra audit). El [Bloque 112] aportó **detección/forense**. El [Bloque 113] endureció el **pipeline de firma** (tres palancas estructurales) y cerró apuntando explícitamente a este bloque: *"el **keyring/BOG** y la gestión de credenciales … quedan fuera de este bloque → [Bloque 114]"*. Este bloque cierra esa cola: **cómo se cifra la configuración (`.bog`) y dónde viven los secretos (KeyRing)** en una station N4.14.
>
> **Qué es** `[CERT]`: dos mecanismos reales de protección en reposo, uno por vector. (1) **El BOG encryption pipeline** — todo archivo `.bog` (la representación serializada de la config: stations, paletas, backups) puede cifrarse con **una de tres fuentes de clave** (`EncryptionKeySource`: `none` / `external` / `keyring`), orquestadas por `BBogFile`/`BBogSpace` y transcodificadas durante backup/restore por `BBackupService`. (2) **El KeyRing** — el almacén central de secretos de la plataforma (`com.tridium.nre.security.KeyRing`, **propietario, NO decompilado**), accedido siempre vía `SecurityInitializer.getInstance().getSecurityInfoProvider().getKeyRing()`, que custodia desde la clave de cifrado de los `.bog` hasta credenciales de integración (Azure SAS, AWS, OrientDB, RDB TLS, syslog).
>
> **El hallazgo de anclaje** `[CERT]`: el secreto raíz de la confidencialidad de la config **converge en el KeyRing**, y el KeyRing es la pieza que el corpus **NO** deja leer. Lo observable es el **plumbing** (qué se cifra, cuándo, con qué API, qué consume el keyring y bajo qué nombres de alias); lo **no observable** —y por eso marcado `[LAB]`— es **cómo** el KeyRing se cifra a sí mismo en disco (`.kr` + material `.km`), su esquema criptográfico, y la rotación de claves. La fidelidad de este bloque es **deliberadamente desigual y honesta**: alta en la máquina de estados del cifrado BOG, baja (por blocker propietario) en la implementación del almacén de secretos.
>
> **Refinamiento a [Bloque 113]** `[CERT]`: el B113 §113.2.2 enumeró en `BAxOfflineBackup.java` los archivos `file:!security/.km` (`:102`) y `file:!security/.kr` (`:103`) como genérico *"master key material"*. Este bloque los **identifica con precisión**: **`.kr` es el archivo del KeyRing** (store de secretos) y **`.km` es su material de clave** (master key que lo protege) — confirmado por su tratamiento explícito en `BBackupService` (`.kr` → `importKeyData/exportKeyData`, `:429,1044-1055`) y por el constructor `KeyRingFactory.getInstance(dir, ".awsUtils.kr", ".km")` (`AwsAuthUtils.java:114`).
>
> Fuentes READ-ONLY (vineflower): `file/file-rt/.../com/tridium/file/types/bog/{BBogFile,BBogSpace}.java`; `backup/backup-rt/.../javax/baja/backup/BBackupService.java`; `baja/.../javax/baja/security/PasswordEncodingContext.java`; consumidores del keyring: `azureUtils-rt/.../AzureIotSasTokenUtils.java`, `awsUtils-rt/.../auth/AwsAuthUtils.java`. Clases **propietarias confirmadas NO decompiladas** (`com.tridium.nre.security.*`): `KeyRing`, `SecurityInitializer`, `SecurityInfoProvider`, `EncryptionKeySource`, `BogPasswordObjectEncoder`, `BogTranscoderInputStream`, `PBEEncodingInfo`, `SimpleKeyRing`, `KeyRingFactory`.
> Método: **verificación directa file:line** de la máquina de estados del cifrado, del pipeline backup/restore y de los call-sites del keyring. `[CERT]` = leído verbatim en vineflower. `[INFER]` = deducción marcada. `[LAB]` = **NO observable en el corpus** — requiere validación contra station viva. Conteo honesto: **~6 clases vineflower decompiladas** + **9 clases propietarias referenciadas-no-leídas** (cuya existencia y superficie de API se infiere de los call-sites).
>
> Capa 21 (Test infrastructure + Security incident), **cierre del arco B75→B112→B113→B114**: B75 = vector + hardening de config; B112 = detección/forense; B113 = hardening del pipeline de firma; **B114 = protección de datos en reposo (config + secrets)**. Cross-ref fuerte: [Bloque 113] (ancla — `.kr`/`.km` en el backup-set, truststore), [Bloque 75] (vector + P0/P1/P2), [Bloque 30] (FIPS + key rotation), [Bloque 17] (security/ en disco), [Bloque 33] (history) y [Bloque 31] (backup/restore operativo).

---

## 114.0 — El reframe: firma protege la INTEGRIDAD del código; este bloque cubre la CONFIDENCIALIDAD de los datos `[CERT]`

Los tres bloques previos del arco atacan la **integridad/autenticidad** del código que corre (firma de módulos). Pero una station guarda dos clases de activo que la firma no protege:

1. **La configuración** (`*.bog`): la topología de la station, los componentes, y —crítico— las **contraseñas reversibles** embebidas en componentes (`BReversiblePasswordEncoder`, drivers que necesitan recuperar el plaintext para autenticarse contra un equipo).
2. **Los secretos de integración** (SAS tokens, claves AWS, passwords de DB): credenciales que la station debe poder **descifrar en runtime** para funcionar.

Ambas clases dependen de **cifrado simétrico reversible** con una clave que debe estar disponible para la station — y esa clave, en el caso fuerte, vive en el **KeyRing**. La tesis del bloque `[CERT]`: la postura de seguridad de los datos en reposo se reduce a **(a)** qué `EncryptionKeySource` usan los `.bog` y **(b)** cómo está protegido el KeyRing. El (a) es totalmente investigable; el (b) tiene un **blocker propietario** que se documenta como tal, no se inventa.

---

## 114.1 — Vector A: el **BOG encryption pipeline** — tres fuentes de clave `[CERT]`

### 114.1.1 — El enum `EncryptionKeySource`: tres valores confirmados por uso `[CERT, confirmado-por-uso]`

`EncryptionKeySource` (`com.tridium.nre.security.EncryptionKeySource`) es un **enum propietario NO decompilado** — no existe `EncryptionKeySource.java` en el corpus. Pero sus **tres constantes aparecen verbatim** en el código decompilado que las consume `[CERT]`:

| Constante | Significado (inferido del uso) | Evidencia verbatim |
|---|---|---|
| `EncryptionKeySource.none` | sin cifrado | `BBogFile.java:74`, `BBogSpace.java:115`, `PasswordEncodingContext` default |
| `EncryptionKeySource.external` | clave derivada de una **passphrase** (PBE) provista por el operador | `BBogFile.java` (`makeExternal`), `BBogSpace.java:113,122-123`, `BBackupService.java:464,1100` |
| `EncryptionKeySource.keyring` | clave custodiada por el **KeyRing** de la plataforma | `BBogFile.java:77`, `BBogSpace.java:112-113`, `BBackupService.java:476` |

> **Distinción de fidelidad** `[CERT/INFER]`: los **tres nombres** son `[CERT]` (leídos verbatim en los call-sites). El **orden ordinal, métodos y cualquier valor adicional del enum** son `[INFER]` — la declaración no es legible. No se asume que sean exactamente tres hasta confirmar contra el `.class` propietario, pero **solo tres aparecen** en todo el corpus de uso.

### 114.1.2 — La máquina de estados en `BBogFile`: passphrase vs keyring son mutuamente excluyentes `[CERT]`

`BBogFile` (`file/file-rt/vineflower/.../bog/BBogFile.java`) mantiene el encoder como campo (`:58`) y default a `makeNone()` en el space (`BBogSpace.java:49`) `[CERT]`:
```java
private BogPasswordObjectEncoder bogPasswordObjectEncoder;                    // BBogFile.java:58
// BBogSpace.java:49 →
private BogPasswordObjectEncoder bogPasswordObjectEncoder = BogPasswordObjectEncoder.makeNone();
```

`forceChangeReversibleEncryptionPassPhrase(BPassword value)` (`:71-111`) revela la **regla de exclusión** `[CERT]`:
```java
this.initFromHeader();
if (this.bogPasswordObjectEncoder.getKeySource().equals(EncryptionKeySource.none)) {        // :74
   this.setReversibleEncryptionPassPhrase(value);                                            // :75
   return Collections.emptyList();
} else if (this.bogPasswordObjectEncoder.getKeySource().equals(EncryptionKeySource.keyring)) {// :77
   throw new IllegalArgumentException("Bog file cannot use a pass phrase");                   // :78
} else {
   // ... external: re-cifra con la nueva passphrase ...
   this.bogPasswordObjectEncoder = BogPasswordObjectEncoder.makeExternal(valueChars);         // :85
   this.passPhraseValidator = BIPasswordValidator.fromPBEValidator(
                                 this.bogPasswordObjectEncoder.getPbeEncodingInfo());          // :86
   // ... y limpia los reversible passwords del árbol:
   List<SlotPath> result = PasswordUtil.forceClearReversiblePasswords(
                              this.getBogSpace().getRootComponent());                          // :107
```

Lectura `[CERT]`: un `.bog` en modo **`keyring` NO admite passphrase** (`:77-78`) — su clave la gobierna el KeyRing, no el operador. Solo el modo **`external`** acepta cambio de passphrase (rama `else`, `:85`). Es decir: **keyring y external son rutas alternativas, no combinables**.

### 114.1.3 — `BBogSpace.getEncodingContext()`: dónde se elige la clave en cada operación `[CERT]`

El punto donde se resuelve qué clave usar al leer/escribir el space es `getEncodingContext(Context)` (`BBogSpace.java:102-153`) `[CERT]`:
```java
if (!encoder.getKeySource().equals(EncryptionKeySource.external) && !passPhrase.isPresent()) {  // :109
   if (encoder.getKeySource().equals(EncryptionKeySource.keyring))
      pContext.setEncryptionAndDecryptionKey(EncryptionKeySource.keyring, Optional.empty());     // :112-113
   else if (encoder.getKeySource().equals(EncryptionKeySource.none))
      pContext.setEncryptionAndDecryptionKey(EncryptionKeySource.none, Optional.empty());         // :114-115
} else if (passPhrase.isPresent()) {
   SecretChars pp = this.reversibleEncryptionPassPhrase.get().getSecretChars();                   // :118
   pContext.setEncryptionAndDecryptionKey(
      EncryptionKeySource.external, Optional.of(encoder.passPhraseToKey(pp)));                     // :122-123
}
```
Tres hechos `[CERT]`:
1. En modo **keyring**, la clave se pasa como `Optional.empty()` (`:113`) — **el material de clave NO viaja en el context**; el primitivo (abajo) lo resuelve contra el KeyRing internamente.
2. En modo **external**, la passphrase se convierte a clave con `encoder.passPhraseToKey(pp)` (`:123`) — **PBE** (password-based encryption); el `getPbeEncodingInfo()` (`:86`) confirma parámetros PBE pero su contenido (salt, iteraciones, cipher) **no es legible** (encoder propietario) `[LAB-parcial]`.
3. Al guardar, un guard rehúsa persistir sin clave salvo en keyring (`BBogSpace.java:225`): `if (!passPhrase.isPresent() && !encoder.getKeySource().equals(keyring))` `[CERT]`.

### 114.1.4 — Los primitivos criptográficos: confirmados como **NO decompilados** `[CERT/LAB]`

El cifrado real (los bytes) lo hacen clases **propietarias** importadas desde `com.tridium.nre.*`, **ausentes del corpus** `[CERT — confirmado por `find` vacío]`:

| Clase propietaria | Superficie observada (de call-sites) | Estado |
|---|---|---|
| `BogPasswordObjectEncoder` | `makeNone()`, `makeExternal(chars)`, `getKeySource()`, `passPhraseToKey(chars)`, `getPbeEncodingInfo()` | **NO decompilada** |
| `BogTranscoderInputStream` | ctor `(keyRing, in, suppressLog, encodingKey, targetKeySource[, path])` | **NO decompilada** |
| `PBEEncodingInfo`, `SecretBytes`, `ISecretBytesSupplier`, `SecretChars` | tipos de material de clave | **NO decompiladas** |

> **Límite de fidelidad explícito** `[LAB]`: el **algoritmo** (cipher, modo, KDF/iteraciones del PBE, longitud de clave) con que se cifra un `.bog` **NO es observable**. Lo que sí está confirmado es **qué fuente de clave** se usa y **cuándo**. La afirmación "los `.bog` están cifrados con AES-X" sería **inventada** — no se hace.

---

## 114.2 — El pipeline backup/restore: transcodificación keyring ⇄ external `[CERT]`

El hallazgo más rico de Vector A: **un backup transcodifica el cifrado** para ser portable sin el KeyRing de origen. `BBackupService` (`backup/backup-rt/vineflower/javax/baja/backup/BBackupService.java`) `[CERT]`:

**Al hacer BACKUP** (`:1089-1100`) — un `.bog` cifrado con keyring se **re-envuelve a `external`** (PBE con la `encodingKey` del backup) `[CERT]`:
```java
} else if (f instanceof BBogFile && AccessController.<Boolean>doPrivileged(((BBogFile)f)::usesKeyRingEncryption)) {  // :1089
   in = AccessController.doPrivileged((PrivilegedExceptionAction<InputStream>)(() -> new BogTranscoderInputStream(
            SecurityInitializer.getInstance().getSecurityInfoProvider().getKeyRing(),   // :1096  fuente: keyring
            in, !log.isLoggable(Level.FINEST),
            localFileEncodingKey,                                                        // :1099  destino: PBE key del backup
            EncryptionKeySource.external,                                                // :1100  targetKeySource = external
            f.getFilePath().getBody())));
```

**Al hacer RESTORE** (`:464-476`) — el camino inverso: un `.bog` `external` del ZIP se **re-envuelve a `keyring`** de la station destino `[CERT]`:
```java
if (passwordObjectEncoder != null && EncryptionKeySource.external.equals(passwordObjectEncoder.getKeySource())) { // :464
   in = new BogTranscoderInputStream(
      AccessController.doPrivileged((PrivilegedExceptionAction<KeyRing>)(() ->
         SecurityInitializer.getInstance().getSecurityInfoProvider().getKeyRing())),    // :471  destino: keyring de la station
      op.zipFile.getInputStream(entry), !op.isLoggable(Level.FINEST),
      encodingKey, EncryptionKeySource.keyring);                                         // :476  targetKeySource = keyring
```

Y **el propio KeyRing** se incluye en el backup como el archivo `.kr` `[CERT]`:
```java
// RESTORE:  :426  } else if (...getName().equals(".kr")) {
//           :429    ...getKeyRing().importKeyData(in, (int)entry.getSize(), encodingKey);
// BACKUP:   :1044 if (f.getFilePath().getBody().equals("~security/.kr")) {
//           :1054-1055   ...getKeyRing().exportKeyData(localFileEncodingKey)
```

**Implicación de seguridad** `[CERT, crítica]`: la `encodingKey`/`localFileEncodingKey` del backup (una passphrase de operador, B31) es **la clave maestra de la confidencialidad de un backup completo**. Un backup contiene **(a)** el KeyRing entero (`.kr`, exportado vía `exportKeyData`, `:1054`) y **(b)** toda config keyring-cifrada re-envuelta a PBE bajo esa misma `encodingKey` (`:1099-1100`). Por lo tanto: **quien obtenga el ZIP de backup + la passphrase del backup obtiene todos los secretos de la station** — el KeyRing deja de ser un blocker. La fuerza del cifrado en reposo del backup = fuerza de **esa única passphrase**.

---

## 114.3 — Vector B: el **KeyRing** — el almacén de secretos (lo investigable + el BLOCKER) `[CERT/LAB]`

### 114.3.1 — Confirmado: KeyRing es PROPIETARIO / NO decompilado `[CERT — blocker documentado]`

`find /home/cristian/modules -iname 'KeyRing.java'` → **vacío**. La clase se importa desde `com.tridium.nre.security.KeyRing` y la variante de decompilado `decompiled/` lo marca explícito en su header `[CERT]`:
```
/* Could not load the following classes:
 *  com.tridium.nre.security.KeyRing
 *  com.tridium.nre.security.SecurityInitializer  */
```
Idéntico para `SecurityInitializer` y `SecurityInfoProvider`. **No se reconstruye su implementación.** Todo lo de abajo es la **superficie observable desde los consumidores**, no la lógica interna.

### 114.3.2 — Cómo se ACCEDE: el patrón singleton universal `[CERT]`

Un único patrón de acceso, verbatim en **todos** los call-sites `[CERT]`:
```java
SecurityInitializer.getInstance().getSecurityInfoProvider().getKeyRing()
```
Variante con almacén dedicado por módulo (no el de la plataforma) `[CERT]`:
```java
KeyRingFactory.getInstance(KEY_RING_DIR, ".awsUtils.kr", ".km").getKeyRing()   // AwsAuthUtils.java:114
// KEY_RING_DIR = <niagaraUserHome>/security/awsUtils/                          // AwsAuthUtils.java:22
```

**Superficie de API observada** (de los call-sites, no de la clase) `[CERT]`: `setKey(alias, bytes, boolean)`, `getKey(alias)→byte[]`, `removeKey(alias)`, `createKey(alias, boolean)`, `checkRollKeyMaterial(...)`, `importKeyData(in, size, encodingKey)`, `exportKeyData(encodingKey)`. Es un **mapa alias→secret** con import/export cifrado y una operación de **roll de material de clave** (`checkRollKeyMaterial` — pista de rotación, pero su mecánica es `[LAB]`).

### 114.3.3 — Qué CONSUME el KeyRing: inventario de secretos `[CERT]`

`grep getKeyRing|KeyRingFactory` (vineflower) → **10 módulos**. Lo que cada uno guarda (alias = `[CERT]` donde está verbatim, contenido = `[CERT/INFER]`):

| Módulo | Archivo (vineflower) | Secreto almacenado | Alias / evidencia |
|---|---|---|---|
| `azureUtils` | `AzureIotSasTokenUtils.java` | Azure IoT **SAS tokens** / connection strings | `setKey(alias,…)` `:103`, `getKey(alias)` `:113`, `removeKey` `:125`; alias = `{entryType}_{storeKey}` (`getKeyRingAlias`, `:129`) |
| `awsUtils` | `auth/AwsAuthUtils.java` | **AWS access keys** (secret key) | store dedicado `.awsUtils.kr`/`.km` en `security/awsUtils/` `:114,22` |
| `backup` | `BBackupService.java` | el **KeyRing entero** (import/export) | `:429,1054-1055` |
| `orientSystemDb` | `BOrientSystemDb.java` | passwords/clave de cifrado de **OrientDB** | `getKey`/`createKey` (consumidor confirmado en vineflower) |
| `rdb` | `BEncryptableTransportRdbms.java` | passwords de **truststore TLS** de bases relacionales | consumidor confirmado en vineflower |
| `platform` (syslog) | `BSyslogPlatformService.java` | password de cliente **syslog** | consumidor confirmado en vineflower |
| `abstractMqttDriver` | (delega en azureUtils) | SAS de **MQTT** | consumidor confirmado |
| `baja`, `workbench`, `docSource` | varios | material de clave base / utilidades | consumidores confirmados |

> **Expansión al scoping (no contradicción)** `[CERT]`: el encargo anticipaba el keyring como almacén de *"credenciales de integración, OAuth/SAS"*. El corpus confirma eso **y más**: también custodia **passwords de OrientDB, truststores TLS de RDB, y la clave de cifrado de los `.bog` keyring-mode**. Es el **secreto-raíz transversal** de la station, no solo de las integraciones cloud.

### 114.3.4 — Control de acceso: `KeyRingPermission` (modelo named) `[CERT]`

El acceso al KeyRing está gateado por `com.tridium.nre.security.KeyRingPermission`, declarado en **17 `module.xml`** (vineflower) `[CERT]`:
```xml
<java-permission class="com.tridium.nre.security.KeyRingPermission" name="*"/>   <!-- azureUtils/.../module.xml:15 -->
```
Es un **permiso Java named** (estilo `*` o alias específico): un módulo necesita la `KeyRingPermission` declarada en su `module.xml` para tocar el store. Conecta con el modelo de permisos firmados del [Bloque 18]/[Bloque 75]: **leer secretos requiere un módulo con la permission declarada** — y, por la cadena del B113, ese módulo debería estar **firmado**. *(Que `KeyRingPermission` requiera firma —vía `requiresSignature()`— es `[LAB]`: depende del `NiagaraPermissionGroup` al que mapea, no verificado en este corpus.)*

---

## 114.4 — `[LAB]` — Lo que REQUIERE station viva (blocker propietario, separado a propósito)

Esta sección es **explícitamente no investigable** en el corpus decompilado. Documenta **qué validar en lab** y **cómo**, sin inventar respuestas.

| # | Pregunta abierta | Por qué no es observable | Cómo validarlo en lab |
|---|---|---|---|
| L1 | **¿Cómo se cifra el `.kr` en disco?** (cipher, modo, longitud de clave) | `KeyRing`/`SimpleKeyRing` propietarias; los bytes los produce código no leído | Inspeccionar `<niagara_home>/security/.kr` en una station: header/magic, intentar `keytool`/`openssl`, correlacionar con `.km` |
| L2 | **¿Qué protege al material `.km`?** (la master key del keyring) | mismo blocker; `.km` solo aparece como nombre de archivo en backup-set | Dump del `.km`, ver si está atado a la máquina (TPM/host-binding) o derivado de passphrase de plataforma |
| L3 | **¿Cómo rota la clave?** (`checkRollKeyMaterial` observado, mecánica no) | método visto en call-site; cuerpo no decompilado | Forzar rotación en station de prueba, diff de `.kr`/`.km` antes/después; cruzar con [Bloque 30] key rotation |
| L4 | **¿Qué credenciales hay realmente en una station dada?** | el contenido del store es runtime, no código | Enumerar aliases vía spy/API en station viva; cruzar con la tabla 114.3.3 |
| L5 | **¿Los `.bog` están cifrados por default o en `none`?** | el default del encoder es `makeNone()` (`:49`) **en código**, pero la postura real de una station depende de su config | Auditar headers de `.bog` en la station: `EncryptionKeySource` efectivo por archivo |
| L6 | **Parámetros PBE del modo `external`** (salt, iteraciones) | `getPbeEncodingInfo()` propietario | Generar un `.bog` external de prueba, inspeccionar el header PBE |
| L7 | **¿`KeyRingPermission` exige módulo firmado?** | depende del mapping a `NiagaraPermissionGroup` (no en este corpus) | Verificar el permission group de `KeyRingPermission` en la station y su `requiresSignature()` |

> **Mini-guía de lab (orden sugerido)** `[LAB]`: (1) en una station de prueba, localizar `<niagara_home>/security/{.kr,.km}` y los `.bog`; (2) catalogar el `EncryptionKeySource` efectivo de cada `.bog` (L5) — esto solo no requiere romper nada; (3) enumerar aliases del KeyRing por API/spy (L4); (4) recién entonces, con station desechable, probar export de backup con una passphrase conocida y comparar contra el `.kr` on-disk (L1/L2); (5) forzar rotación y diff (L3). **Nunca sobre la station de producción del cliente.**

---

## 114.5 — Hardening checklist / mitigaciones accionables `[CERT/LAB]`

| # | Vector | Acción de hardening | Dónde se aplica | Evidencia file:line |
|---|---|---|---|---|
| H1 | BOG | **No dejar `.bog` sensibles en `EncryptionKeySource.none`**: el default del encoder es `makeNone()` — auditar y migrar a `keyring` (o `external` con passphrase fuerte) | headers de `.bog` en `security/`/stations | `BBogSpace.java:49`; auditar L5 |
| H2 | BOG | **Preferir modo `keyring` sobre `external`** para config viva: la clave no depende de una passphrase memorizada por el operador y no admite cambio ad-hoc (`:77-78`) | config de la station | `BBogFile.java:77-78` |
| H3 | Backup | **Tratar la passphrase del backup como secreto de máximo valor**: descifra el `.kr` + toda config re-envuelta a PBE — es la clave maestra del ZIP | proceso de backup (B31) | `BBackupService.java:1054-1055,1099-1100` |
| H4 | Backup | **Cifrar y custodiar los ZIP de backup** fuera de banda; rotar la passphrase de backup; no reusar la de la station | almacenamiento de backups | `BBackupService.java:429,1044` |
| H5 | KeyRing | **Minimizar módulos con `KeyRingPermission`**: revisar los 17 `module.xml` que la declaran con `name="*"` y restringir a alias específicos donde sea posible | `META-INF/module.xml` por módulo | `azureUtils/.../module.xml:15` (×17) |
| H6 | KeyRing | **Exigir que todo módulo con `KeyRingPermission` esté firmado** (encadena con B113): un módulo no firmado que lea el keyring sería catastrófico | gate de firma (B113 H1-H11) | `BBackupService.java:429`; B113 |
| H7 | KeyRing | **Proteger el filesystem de `<niagara_home>/security/` (`.kr`, `.km`)**: el at-rest del keyring solo es tan fuerte como el control de acceso al directorio (reusa B113 H2) | OS / permisos de archivo | `AwsAuthUtils.java:22`; B113 §113.2.2 |
| H8 | KeyRing | **Rotar el material de clave** (`checkRollKeyMaterial`) según [Bloque 30]; validar la mecánica en lab (L3) antes de producción | runtime / B30 | `[LAB]` L3 |
| H9 | Ambos | **Activar FIPS** (B113 H9 / [Bloque 30]): endurece los primitivos de cifrado disponibles también para el at-rest, no solo para la firma | `SecurityInitializer.isFips()` | B113 §113.3.2 |
| H10 | Lab | **Ejecutar la auditoría L1-L7** sobre una station representativa antes de firmar el informe de cliente | station viva | `[LAB]` 114.4 |

**Orden de prioridad** `[CERT]`: **H3-H4** primero (el backup es el punto de fuga más concentrado y el más fácil de exfiltrar), luego **H1-H2** (que la config no esté en claro), luego **H5-H7** (contener quién toca el keyring) — y **H8-H10** como cierre dependiente de lab. H6 es el **puente con B113**: la confidencialidad (este bloque) y la integridad (B113) se cruzan exactamente en *"el módulo que lee secretos debe estar firmado"*.

---

## 114.6 — Hallazgos, corrigenda y cierre del arco

**Hallazgos CERT (uno por vector)**:
1. **BOG**: el cifrado de config tiene **tres fuentes de clave** (`EncryptionKeySource.none/external/keyring`, confirmadas por uso), keyring y external **mutuamente excluyentes** (`BBogFile.java:74-78`), resueltas por operación en `getEncodingContext` (`BBogSpace.java:102-123`); el **algoritmo** es `[LAB]` (encoder propietario). El backup **transcodifica keyring→external** y de vuelta (`BBackupService.java:464-476,1089-1100`), volcando el KeyRing entero en `.kr` bajo la passphrase del backup.
2. **KeyRing**: almacén **propietario confirmado NO decompilado**, accedido por `SecurityInitializer…getKeyRing()`, con superficie `setKey/getKey/removeKey/createKey/importKeyData/exportKeyData/checkRollKeyMaterial`, consumido por **10 módulos** (Azure SAS, AWS, OrientDB, RDB TLS, syslog, MQTT, backup, baja…), gateado por `KeyRingPermission` (17 `module.xml`). Su **at-rest, master key y rotación** son `[LAB]`.

**Corrigenda / refinamientos** `[CERT]`:
- **`.kr` = KeyRing, `.km` = su material de clave**: precisa el genérico *"master key material"* de B113 §113.2.2 (`BAxOfflineBackup.java:102-103`).
- **El keyring es más que integraciones cloud**: también custodia la clave de los `.bog` keyring-mode, passwords de OrientDB y truststores RDB — expande (no contradice) el scoping.
- **El default de cifrado de un space es `none`** (`BBogSpace.java:49`) — un `.bog` no es confidencial por default; hay que elegir keyring/external explícitamente.

**Fidelidad lograda (honesta)**:
- **Vector A (BOG)**: ~**70%** logrado — la **máquina de estados** (qué/cuándo/dónde se cifra, las tres fuentes, el pipeline backup/restore) está **confirmada en código**; el **primitivo** (cómo se cifran los bytes, PBE params) es `[LAB]`.
- **Vector B (KeyRing)**: ~**40%** logrado — **acceso, consumidores, aliases y on-disk filenames** confirmados; **implementación, at-rest, rotación** = `[LAB]` (blocker propietario documentado, no inventado).

**Sin contradicción al encargo ni a B75/B112/B113**: este bloque **confirma** el blocker propietario anticipado, **honra** el hilo abierto en B113 §113.5 (keyring/BOG → B114), y **cierra** el arco de seguridad de la Capa 21 con la dimensión de **confidencialidad en reposo**, complementaria a la integridad de firma (B113), la detección (B112) y el vector original (B75).

---

**Bloque cerrado**: 2026-06-28. Investigación READ-ONLY sobre source decompilado (vineflower) — ~6 clases decompiladas + 9 propietarias referenciadas-no-leídas (`com.tridium.nre.security.*`). Capa de PROTECCIÓN DE DATOS EN REPOSO; cuarto y último movimiento del arco [Bloque 75] (vector) → [Bloque 112] (detección) → [Bloque 113] (firma) → **B114 (cifrado config + keyring)**. Capa 21. Blocker propietario (KeyRing) documentado como `[LAB]`, no reconstruido. Engram: `niagara/security/b114-config-secret-encryption`.
