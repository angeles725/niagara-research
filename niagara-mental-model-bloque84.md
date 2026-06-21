# Bloque 84 — Auxiliares del stack cloud Honeywell: onboarding one-shot, backup cifrado y cert pinning — con hallazgo de credencial hardcodeada

> Investigación empírica de los tres módulos **auxiliares** de la familia cloud ([Bloque 83]): `honCloudEasyOnboard` (wizard de provisioning), `cloudBackup` (backup cifrado de la station al cloud) y `cloudConfig` (cert pinning). Completa la familia cloud y aporta un **hallazgo de seguridad verificado**: el onboarding crea un usuario administrador con **password hardcodeada en el JAR**.
>
> Fuentes: `organized/{honCloudEasyOnboard,cloudBackup,cloudConfig}/<m>-{rt,wb,ux}/vineflower/...` (+ `module.xml`, `web.xml`, `.lexicon`).
> Método: 2 sub-agentes + **verificación directa** de cada `extends` y de la credencial hardcodeada (`grep` verbatim). `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (flujo, endpoints, slots) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 83] (cierra la familia cloud). Conecta con [Bloque 75] (modelo de amenazas — credencial + superficie de backup), [Bloque 82] (instala el `BHonTagDictionary`), [Bloque 10] (Platform/system passphrase).

---

## 84.1 — Los tres auxiliares en conjunto `[CERT]`

| Módulo | Clase raíz verificada | Rol | Vendor/versión |
|--------|-----------------------|-----|----------------|
| `honCloudEasyOnboard` | `BEasyOnboard extends BAbstractService implements ICloudConfiguration` (:59) | wizard one-shot que arma toda la cadena cloud | Honeywell **4.6 / 2018.6** (~may 2020) |
| `cloudBackup` | `BCloudBackupService extends BBackupService implements BIAlarmSource` (:116) | backup cifrado de la station al "NC Backup Service" | Tridium |
| `cloudConfig` | `BCertObject extends BComponent implements BIAgent` (:24) | cert pinning (clave pública de Sentience) | Tridium 4.14 |

> **Nota de versión `[CERT-a]`**: `honCloudEasyOnboard` declara deps a `cloudConnector/nCloudDriver/...` en versión **2018.6** y `baja` 4.6 (build ~mayo 2020) — es **notablemente más viejo** que el stack cloud que orquesta (los conectores del [Bloque 83] eran 2023.14). El onboard puede estar desfasado respecto a los conectores actuales; verificar compatibilidad antes de usarlo en un site 4.14.

---

## 84.2 — honCloudEasyOnboard: provisioning one-shot (9 pasos) `[CERT]`

`BEasyOnboard extends BAbstractService implements ICloudConfiguration` (:59). Desde una sola acción WB (`easyOnBoard`) arma **toda la infraestructura cloud** que de otro modo serían 8+ componentes manuales en distintos servicios. Dos jobs `[CERT]`: `BOnboardJob extends BSimpleJob` (:16) y `BCleanCloudConfigJob extends BSimpleJob` (:16).

**`BOnboardJob` — 9 pasos idempotentes** `[CERT-a]` (cada uno hace BQL lookup antes de crear → re-ejecutable sin duplicar):
1. `BCloudConnector` en Services (+ `BSentienceConnectorImpl` con URLs Sentience + `BIotHubMessageClient`).
2. `BNiagaraCloudNetwork` en el primer DriverContainer.
3. `BCloudSentienceDevice` en la red (con `connector` linkado, commands enabled).
4. `BCloudAuthenticationScheme` en el AuthenticationService.
5. **Usuario local `cloudUser`** (roles `CloudManager` + `CloudOperator`).
6. `BCloudAlarmRecipient` en el AlarmService + link a **todas** las `BAlarmClass`.
7. `BHonTagDictionary` (palette `GenericNetworkDevicePointsDictionary`, [Bloque 82]) en el TagDictionaryService.
8. `BModelSyncService` (módulo Honeywell `SentienceModelSync`) con `BContextDiscoveryModelExtractor` (scope `station:|slot:/`, tags `hon:Gateway`/`hon:Id`/`hon:honType`, relaciones `honhvac:containsProperty;honcore:containsElement`).
9. (loop) `BUserMapping` por cada appId conocido (`appid_model`, `appid_alarm`) → mapea appId cloud → `cloudUser`.

**`BCleanCloudConfigJob`** `[CERT-a]`: orden inverso, **omite intencionalmente borrar el `BCloudConnector`** ("Intentionally Skipping, perform manually") — porque guarda las credenciales RPK.

**Entornos `[CERT-a]`**: `BEnvironment` (BFrozenEnum) solo `production` (default) y `qa` (no DAT). En QA agrega `BSentienceDevTestComponent`. URLs/appIds por entorno en `EnvironemntSettings` (typo en el nombre de la clase). PROD: auth `gaprodsystemauthentication.sentience.honeywell.com`, registro `gaprodregui.sentience.honeywell.com`, portal `papi.honeywellcloud.com/ccprodapp/#/`, systemType `honeywell-niagara-device`.

> **El onboarding NO invoca el registro remoto** `[CERT-a]`: solo coloca los componentes con las URLs correctas. El handshake RPK→IdentityJwt→SAS ([Bloque 83.4]) ocurre después, cuando el `cloudSentienceConnector` intenta conectar. Sin manejo de error parcial (`ActivityStatus` solo `SUCCSS`/`SKIPPED`, sin rollback — un fallo lanza excepción y corta).

### 84.2.1 — HALLAZGO DE SEGURIDAD: credencial administrador hardcodeada `[CERT]`

Verificado verbatim en `honCloudEasyOnboard-rt/.../CloudConnector.java`:
```java
private static final String CLOUD_USERNAME = "cloudUser";   // :28
...
user.addRole("CloudManager", null);                          // :98
user.addRole("CloudOperator", null);                         // :99
BPasswordAuthenticator authenticator = new BPasswordAuthenticator();
authenticator.setPassword(BPassword.make("Hon@123$1"));      // :101  ← password hardcodeada
```

El paso 5 del onboarding crea un usuario **`cloudUser` con roles `CloudManager` + `CloudOperator`** y password **`Hon@123$1` hardcodeada en el código fuente del JAR**. Implicaciones:
- Cualquiera con acceso al JAR (descargable/decompilable, como hicimos acá) conoce la contraseña del usuario cloud privilegiado de **toda station onboarded con esta herramienta**.
- Si el operador no rota la contraseña post-onboarding, es una **puerta trasera efectiva** con roles de gestión cloud.
- Combina con el [Bloque 83.5]: `cloudUser` es el sujeto al que se mapean los appIds cloud (alarm-ack, model-sync) — su compromiso afecta el canal de control remoto.
- **Recomendación operacional**: tras correr `easyOnBoard`, rotar inmediatamente la contraseña de `cloudUser` (o deshabilitar password-auth en favor del scheme cloud). Auditar stations existentes por `cloudUser` con la password default. Aporta al checklist del [Bloque 75].

---

## 84.3 — cloudBackup: backup cifrado de la station al "NC Backup Service" `[CERT]`

`BCloudBackupService extends BBackupService implements BIAlarmSource` (:116, paquete `com.tridium.cloud.client.backup`). Respalda la **station completa** (`.dist` → `.edist` cifrado) a un microservicio HTTP propio de Sentience/Forge (**NO** Azure IoT Hub ni Blob directo) `[CERT-a]`.

**Canal y auth `[CERT-a]`**: HTTPS REST directo a `backupUrl`, Bearer Token vía `BIBearerTokenProvider.getBearerToken("ncbackupservice")` (reutiliza el `cloudConnector` del [Bloque 83] solo para el token + `systemGuid`). Health: `GET {backupUrl}/ping`.

**Cifrado de sobre (envelope encryption) `[CERT-a]`** — patrón DEK/KEK serio:
1. `Station.saveSync()` local.
2. `POST /api/v1/systems/{guid}/backups` → recibe `backupId` + **KEK** (clave pública RSA del servidor) + `kekId`/`kekAlg`.
3. Passkey **AES-256** derivada de la **system passphrase** vía PBKDF2WithHmacSHA256 (4096 iter, sal 16 B random).
4. Backup cifrado **AES/CBC/PKCS5Padding**, subido en **bloques (1-4 MB)** con `PUT` → cada bloque devuelve `blockId`.
5. El DEK (passkey) se cifra con la KEK del servidor (**RSA/ECB/OAEPWithSHA-256AndMGF1Padding**), salvo `shareMode=private` (`dek="private"`).
6. `POST` commit con `alg="1-AES/CBC/PKCS5Padding-PBKDF2WithHmacSHA256-AES-256"`, `dek`, `kekId`, `blockIds[]`, retención.

**Scheduling `[CERT-a]`**: `BRandomizedTimeTrigger` — `manual`/`sevenDays`/`thirtyDays`/`ninetyDays`, disparo **aleatorio** dentro del período (`ThreadLocalRandom`) para evitar thundering herd entre stations.

**UX/WB `[CERT-a]`**: `BCloudBackupManager` (tabla de backups, RPC `runBackup`/`listBackups`/`delete`), `BEncryptedDistributionView` (descifra `.edist` local, `BIOffline`), `BCloudBackupDownloadNotificationHandler` (descarga cifrada vs descifrada con passphrase). Servlet en `/backups/*`. Permisos: `SYSTEM_PASSWORD`, `CLOUD_GET_CONNECTION_INFORMATION`, `GET_PLATFORM_PROVIDER`.

> **Gotcha de seguridad `[CERT-a]`**: lexicon `cloudBackupJob.defaultPublicKeyUsed = "Warning: Default Tridium Public Key used for passkey encryption!"` — si el servidor no entrega su KEK, cae a una clave pública Tridium por defecto (cifrado más débil/predecible). Y la passkey depende de la **system passphrase** del platform ([Bloque 10]) — si es débil, el PBKDF2 protege poco.

---

## 84.4 — cloudConfig: cert pinning de Sentience `[CERT]`

`BCertObject extends BComponent implements BIAgent` (:24, paquete `com.tridium.cloud.client`). Módulo de **una sola clase**; `module.xml`: "Library Dependencies for Niagara Cloud Driver certificate verification". Depende solo de `baja` (sin deps cloud, para evitar ciclos).

Embebe `Prod_Cert.pem` (cert de producción Sentience/Forge) y expone una action `extractPublicKey() → BString` `[CERT-a]` que carga el PEM del classloader, valida vigencia (`checkValidity()`) y devuelve la clave pública en Base64 DER. Es **agente sobre `cloudSentienceConnector:SentienceConnectorImpl`** — permite al conector validar la identidad del servidor (**cert pinning**) sin depender de la CA del SO.

> Cadena de custodia `[CERT-a]`: la clave pública que da `BCertObject` es el mismo tipo de **KEK** que el NC Backup Service entrega en el POST `/backups` (84.3) — `cloudConfig` ancla la confianza del stack cloud en un cert pinneado embebido en el JAR, no en la PKI del host.

---

## 84.5 — Síntesis: cierre de la familia cloud + postura de seguridad

Con los Bloques 83-84 la **familia cloud Honeywell queda destilada** (8 módulos): el transporte/identidad ([Bloque 83]: cloudConnector→IotHub→Sentience→nCloudDriver) + los auxiliares ([Bloque 84]: onboarding, backup, cert pinning).

**Postura de seguridad consolidada (aporta al [Bloque 75])**:
1. **Credencial hardcodeada `cloudUser` / `Hon@123$1`** con roles CloudManager+CloudOperator `[CERT]` — el hallazgo más serio: backdoor conocido en cualquier station onboarded; rotar post-onboard es obligatorio.
2. **Control remoto downlink** ([Bloque 83.5]): el `cloudUser` es el sujeto del mapeo appId→usuario para comandos cloud (write/invoke). Credencial débil + canal de control = riesgo compuesto.
3. **Backup**: envelope encryption correcto (AES-256 + KEK RSA + PBKDF2), pero (a) fallback a "Default Tridium Public Key" si el servidor no da KEK, (b) seguridad atada a la system passphrase del platform.
4. **Cert pinning** (`cloudConfig`) mitiga MITM contra Sentience pero está atado a un `Prod_Cert.pem` embebido — su rotación exige actualizar el módulo.
5. **`clean` no borra el CloudConnector** — credenciales RPK persisten tras un "limpiado"; un decommission real exige borrado manual.

**Para MX60 / Honeywell**: si se adopta el onboarding, tratar la contraseña de `cloudUser` como comprometida-por-diseño y rotarla en el runbook. El patrón de backup cifrado al cloud (envelope encryption + bloques + retención server-side) es una referencia sólida si MX60 necesita backup as-a-service.

**Pendiente conocido**: `BModelSyncService` (módulo `SentienceModelSync`, Honeywell) se referencia pero no se destiló — es el que sincroniza el modelo semántico `hon:` ([Bloque 82]) al cloud; candidato a bloque futuro para cerrar el ciclo tag→cloud. `cloudIotHubDep` (377 java, SDK qpid) sigue sin deep-dive por clase (identificado en [Bloque 83.3]).
