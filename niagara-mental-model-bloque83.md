# Bloque 83 — Stack de conectividad cloud Honeywell: Niagara N4 → Sentience/Forge vía Azure IoT Hub (AMQP) deofuscado

> Investigación empírica de la **familia cloud** del corpus: cómo una station Niagara N4 se conecta al backend cloud de Honeywell (**Sentience** = Cloud Building Platform / Forge) para telemetría, historiales, alarmas y **control remoto bidireccional**. Cierra la pregunta abierta del [Bloque 82] (qué es `cloudConnector:CloudConnector`).
>
> 4 conectores núcleo analizados: `cloudConnector` (broker base), `cloudIotHubConnector` (abstracción AMQP IoT Hub), `cloudSentienceConnector` (identidad/auth Honeywell), `nCloudDriver` (driver N-driver integrador). Más `cloudIotHubDep` (motor AMQP, identificado sin deep-dive) y auxiliares (`honCloudEasyOnboard`, `cloudBackup`, `cloudConfig`).
>
> Fuentes: `organized/{cloudConnector,cloudIotHubConnector,cloudIotHubDep,cloudSentienceConnector,nCloudDriver}/<m>-{rt,wb,ux}/vineflower/...` (+ `module.xml`, `module.palette`, `META-INF/maven/.../pom.properties`).
> Método: 4 sub-agentes en paralelo + **verificación directa** de toda la cadena de herencia (`grep ^public class` + línea `extends`) y del SDK Azure (pom.properties). `[CERT]` = verificado verbatim por mí; `[CERT-a]` = cita del sub-agente (protocolo, slots, endpoints) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 82]. **Conecta fuerte**: [Bloque 78] (N-driver framework `BNNetwork`/`BNDevice`/`BNProxyExt`), [Bloque 80] (Galileo/Sentience, `BSentienceAlarmRecordV2`), [Bloque 82] (tag `hon:CloudId`, `cloudConnector→honcore:ControlSegment`), [Bloque 75] (modelo de amenazas — esta es la superficie de ataque saliente real).

---

## 83.1 — El stack en conjunto: 4 capas en cadena estricta de dependencia `[CERT]`

Cadena de herencia y dependencia verificada de extremo a extremo:

```
BComponent
  └─ BConnectorImpl (abstract)                         [cloudConnector]      :17
       └─ BAbstractIotHubConnectorImpl (abstract)      [cloudIotHubConnector] :20
            ├─ BIotHubConnectorImpl (final)            [cloudIotHubConnector] :31  ← auth connection-string/SAS
            └─ BSentienceConnectorImpl (final)         [cloudSentienceConnector] :175 ← auth RPK challenge ECDSA

BAbstractService
  └─ BCloudConnector (final) implements BICloudConnector, BIAlarmSource, BIRestrictedComponent  [cloudConnector] :155
       (hospeda un connectorImpl: BConnectorImpl intercambiable; en producción = BSentienceConnectorImpl)

BNNetwork  ─ BNiagaraCloudNetwork                      [nCloudDriver] :61
BNDevice   ─ BCloudDevice ─ BCloudSentienceDevice      [nCloudDriver] :100 / :20
BNProxyExt ─ BCloudProxyExt implements BIPollable      [nCloudDriver] :118
```

| Módulo | Vendor | Rol | Clase raíz verificada |
|--------|--------|-----|-----------------------|
| `cloudConnector` | Tridium | Broker thin: lifecycle conexión + retry/backoff + identidad `id` + alarmas | `BCloudConnector extends BAbstractService` (:155) |
| `cloudIotHubConnector` | Tridium | Abstracción AMQP Azure IoT Hub + tipos de mensaje | `BAbstractIotHubConnectorImpl extends BConnectorImpl` (:20) |
| `cloudIotHubDep` | Tridium | **Motor AMQP** (qpid proton-j) — `BIotHubMessageClient` | (SDK, ver 83.3) |
| `cloudSentienceConnector` | Tridium | **Identidad/auth** Honeywell (RPK challenge → SAS) | `BSentienceConnectorImpl extends BAbstractIotHubConnectorImpl` (:175) |
| `nCloudDriver` | Tridium | **Driver** N-driver: puntos/historiales/alarmas/comandos bidireccional | `BNiagaraCloudNetwork extends BNNetwork` (:61) |

> Regla de orden `[CERT-a]`: `cloudConnector` es el ÚNICO que no depende de otro módulo cloud (solo `alarm`/`baja`/`net`). Todos dependen de él. `nCloudDriver` declara dependencia simultánea de los 4 → es el integrador final.

**Nomenclatura (importante para no confundirse) `[CERT-a]`**:
- **NiagaraCloud** = término Tridium (lexicon `cloudConnector`: "Connected to NiagaraCloud").
- **Sentience** = backend Honeywell, antes **CBP (Cloud Building Platform)**, dominio `forgescp.honeywell.com` / `sentience.honeywell.com` → ligado a **Forge** (BMS cloud Honeywell).
- System type registrado: `"n4-station"` (Tridium) o `"honeywell-niagara-device"` (Honeywell onboard).

---

## 83.2 — cloudConnector: el broker thin (Niagara Service) `[CERT]`

`BCloudConnector extends BAbstractService implements BICloudConnector, BIAlarmSource, BIRestrictedComponent` (:155). `BIRestrictedComponent` lo obliga a vivir solo en el `ServiceContainer`. Symbol `cc`.

Es **deliberadamente delgado** `[CERT-a]`: no conoce el protocolo cloud. Hospeda un `connectorImpl` (`BConnectorImpl extends BComponent`, abstract, :17) intercambiable (default `BNullConnectorImpl`, producción `BSentienceConnectorImpl`). Responsabilidades:
- **Identidad**: slot `id` (`BString`, readonly+hidden) = GUID/DeviceId Azure del site. Vacío hasta que `BSentienceConnectorImpl.setId()` lo llena post-registro. **Este `id` es el que el [Bloque 82] mapea a `honcore:ControlSegment`** — el CloudConnector ES el segment de control del building ante el cloud.
- **Lifecycle con retry/backoff** `[CERT-a]`: 24 reintentos normales (5 min) → modo backoff (30 × 24h) → auto-deshabilita. `connectionState` = `{disconnected, pendingConnect, connected}`.
- **Dispatch**: `sendMessage(byte[]/String, Map)` reenvía al impl (IOException si desconectado).
- **Alarmas Niagara**: `faultAlarm`/`toNormal` al cambiar estado (lexicon `connectFail = "Failed to contact NiagaraCloud."`).
- **Anti-tamper** `[CERT-a]`: `BConnectorImpl.fw()` marca fatal fault si el impl fue swapeado; la licencia se delega al impl (`getLicenseFeature()`).
- **RPC**: acción `reconnect` invocable vía `@NiagaraRpc` transport box.

Claves del mapa de conexión `[CERT-a]`: `HOST_NAME="hostName"`, `ID="id"`, `TOKEN="token"`.

---

## 83.3 — cloudIotHubConnector + cloudIotHubDep: transporte AMQP a Azure IoT Hub `[CERT]`

**El SDK NO es el oficial de Azure** `[CERT-a]` (hallazgo): `cloudIotHubDep` no contiene `com.microsoft.azure.sdk.iot.*`. Es un cliente AMQP **propio de Tridium** sobre librerías de bajo nivel (verificado en `pom.properties`):

| Capa | Artefacto | Versión | Paquete |
|------|-----------|---------|---------|
| AMQP 1.0 core | `org.apache.qpid:proton-j` | **0.33.8** | `org.apache.qpid.proton.*` |
| Ext WS/Proxy Azure | `com.microsoft.azure:qpid-proton-j-extensions` | **1.2.4** | `com.microsoft.azure.proton.transport.*` |
| Cliente Tridium | (código propio) | — | `com.tridium.cloud.client.iotdep.*` |

**Jerarquía** `[CERT]`: `BIotHubConnectorImpl extends BAbstractIotHubConnectorImpl extends BConnectorImpl`. El transporte real es un `BMessageClient` (abstract): `BNullMessageClient` (placeholder) vs `BIotHubMessageClient` (AMQP real, en `cloudIotHubDep`). `BMessageClientUpgrader` puede intercambiar el transporte en runtime sobre cualquier `BAbstractIotHubConnectorImpl` — **incluido `BSentienceConnectorImpl`** `[CERT-a]`.

**Protocolo** `[CERT-a]`: AMQP 1.0 en dos modos — `amqps_ws` (**default, puerto 443**, AMQP/WebSocket/TLS, path `/$iothub/websocket`, subprotocol `AMQPWSB10`) o `amqps` (5671, AMQP/TLS). **Auth**: SAS token vía **AMQP CBS** (`$cbs`, `put-token`, `type=servicebus.windows.net:sastoken`, `name={host}/devices/{deviceId}`) — NO X.509. TLS `VERIFY_PEER` con trust anchors DigiCert/Baltimore/D-Trust desde el `BCertManagerService` de Niagara. API version IoT Hub `2020-05-31-preview`.

**Links AMQP por device** `[CERT-a]`: sender D2C `/devices/{id}/messages/events`, receiver C2D `/devices/{id}/messages/devicebound`. **Sin device twin** (no reported/desired properties). Compresión GZip/Deflate si payload ≥150 B (`application/json+gzip`). Throttling: `pendingMessageLimit` (50), `messageTimeout` (300 s).

**5 tipos de mensaje Niagara** (`NiagaraMessageType`, 2 colas de prioridad): `alarm`, `hiPriPoint`, `cmdResp` (high) / `loPriPoint`, `history` (low).

---

## 83.4 — cloudSentienceConnector: identidad y autenticación (no datos) `[CERT]`

`BSentienceConnectorImpl extends BAbstractIotHubConnectorImpl implements BIBearerTokenProvider, MessageCallback` (:175). Es la **capa de identidad/auth**, no de datos: genera el túnel autenticado; la telemetría viaja por el AMQP de 83.3.

**Auth en 3 fases** `[CERT-a]`:
1. **RPK Challenge** (REST/HTTPS, criptografía **ECDSA P-256 / SHA256withECDSA**): `POST /api/authentication/rpkchallenge` con `{SystemId, SystemType}` → recibe `ChallengeString` (32 B base64). `POST /api/authentication/rpkchallengeresponse` con `ClientRandom` + firma ECDSA de `challenge[32]+clientRandom[8]` → recibe `IdentityJwt` (RS256).
2. **Credenciales IoT Hub**: `POST /api/system/connections` con `Authorization: Bearer <IdentityJwt>` → array con entrada `IoTHub2` (`Path`=host, `UserName`=DeviceId, `Password`=SAS token).
3. **Transporte**: connection string `HostName={path};DeviceId={id};SharedAccessSignature={sas};X509Cert=False` → `super.doConnect()` abre AMQP (83.3).

**Par de claves** `[CERT-a]`: ECDSA P-256 en keystore Niagara (`"Cloud_"+systemId`), cert auto-firmado `CN={systemId},O=Tridium,C=US` validez 20 años, soporte HSM (`NiagaraHsm`), **incompatible con FIPS**. Renovación SAS automática a `(exp-ahora)/2`. Heartbeat `doPing()` → `HeartbeatRequest`/`HeartbeatResponse` sobre IoT Hub.

**Identidad** `[CERT-a]`: `systemId = "N4:<stationName>:<hostId>"` (truncado: station 23 chars, JACE8000/TITAN 17) o `"GUID:<uuid>"`. `systemOwnershipCode` (24 B hex), `systemPublicKey` (EC base64), `gatewayId` (env `PELION_DEVICE_ID`).

**UX** `[CERT-a]`: `BDeviceRegistrationWidget extends BSingleton implements BIJavaScript, BIFormFactorMax` — agente sobre `cloudConnector:CloudConnector`; botones Register/Force-reconnect/Migrate; abre portal de registro con los datos de identidad como query params.

**Endpoints PROD** `[CERT-a]`: auth `systemauthentication.hbt.forgescp.honeywell.com` / `gaprodsystemauthentication.sentience.honeywell.com`; registro `regapi.hbt.forgescp.honeywell.com` / `gaprodregui.sentience.honeywell.com`; portal devreg `devreg.cloud.tridium.com`. Permiso de seguridad `CLOUD_GET_CONNECTION_INFORMATION`. Logger `cloud.connector.sentience`.

---

## 83.5 — nCloudDriver: el driver bidireccional (uplink + downlink + control remoto) `[CERT]`

`BNiagaraCloudNetwork extends BNNetwork` (:61), device `BCloudDevice extends BNDevice implements ConnectCallback, MessageCallback` (:100) → concreto `BCloudSentienceDevice extends BCloudDevice` (:20), proxy `BCloudProxyExt extends BNProxyExt implements BIPollable` (:118). Es el driver N-driver ([Bloque 78]) sobre el broker cloud. Descubre el connector por BQL `select * from cloudConnector:CloudConnector`. Transporte dual `BIotTransport = {sentience, iothub}`. License `tridium/nCloudDriver`.

**Uplink (Niagara → cloud)** `[CERT-a]`:
- **Telemetría batch** — 3 niveles (priority 30 s / standard 5 min / background 15 min), batch 500 (100-10000), encode `HistoryUpdateMessage` v1.
- **COV** — máx 20 puntos; el cloud lo activa por `CovActiveCommand`; cada cambio → mensaje individual lo/hiPriPoint.
- **Historiales** (`BCloudHistoryDeviceExt extends BHistoryDeviceExt`): batch 15 min, pide `GetLastHistoryTimestamp` y envía delta; backfill máx 1 h.
- **Alarmas**: `NewAlarmMessageV3`/`AlarmChangedMessageV3`/`AlarmRecoveryAfterDisconnectMessageV3` (resync post-desconexión); `CreatorType="Niagara4"`, `GeneratorType="NiagaraCloudConnector"`.

**`pointId`** `[CERT]`: por defecto `stationName!handle` o `!slotPath`; con `useDriverIdForPointId` usa el tag **`hon:CloudId`** (`Id.newId("hon:CloudId")`) — **liga directa con el [Bloque 82]** (los tag dicts pueblan la identidad cloud del punto). El proxy es `writeonly` upstream.

**Downlink (cloud → Niagara) = superficie de control remoto** `[CERT-a]`: comandos extensibles `SystemCommand` registrados en `BCloudCommandsDeviceExt`: `CloudPointReadCommand`, **`CloudPointWriteCommand`/`CloudMultiPointWriteCommand`** (escritura de puntos), `AlarmAckCommand`, `CovActive/Inactive`, **`InvokeCommand`** (invocar acciones), `RetrieveCloudPoints/Commands`. El JSON trae `CloudPlatformHeaders` con `Command`, `CommandId`, `Priority`, `Auth`.

**Autorización de comandos (JWT)** `[CERT-a]`: `CloudLoginModule` valida el Bearer del campo `Auth` con **jose4j** (RS256/ES256), claims `deviceid`/`cloudroles`/`sub`/`username`, JWKS endpoint configurable (`BJwksTrustMapping`), audience/issuer por `BCloudTrustManager`. Mapea **roles cloud → roles Niagara** (`BCloudRoleMappings`) o `appId → usuario local` (`BCloudUserMappings`). Scheme JAAS `"ncloud"`. Identidades cloud ephemeral (LRU 100).

**WB** `[CERT-a]`: solo managers estándar (`BCloudDeviceManager`/`BCloudPointManager`/`BCloudHistoryExportManager`), **sin wizard** — comisionamiento por el WB normal. Auxiliar `honCloudEasyOnboard` (Honeywell, system type `honeywell-niagara-device`) consume nCloudDriver + Sentience para el provisioning guiado.

---

## 83.6 — Síntesis: superficie de ataque + paralelos + MX60

**La arquitectura en una frase**: una station N4 se autentica ante Sentience con un par ECDSA P-256 (RPK challenge), obtiene un SAS token, abre un canal **AMQP 1.0 sobre WebSocket/443** a un **Azure IoT Hub**, y por ese canal sube telemetría/historiales/alarmas y **recibe comandos** (incluida escritura de puntos e invocación de acciones) autorizados por JWT.

**Relevancia de seguridad ([Bloque 75])** — esto es superficie de ataque **saliente y de control remoto**:
1. **Canal de control remoto real**: `CloudPointWriteCommand` + `InvokeCommand` permiten que el cloud escriba puntos e invoque acciones en la station. La defensa es el JWT (jose4j RS256/ES256) + role mapping. Comprometer el JWKS/issuer o el role mapping = control del edificio.
2. **Salida 443 AMQP/WS**: tráfico saliente a Azure IoT Hub indistinguible de HTTPS normal en el firewall — relevante para detección/egress filtering.
3. **Claves en keystore Niagara** (`Cloud_<systemId>`), cert auto-firmado 20 años, **incompatible con FIPS** — gap de cumplimiento si el site exige FIPS.
4. **Anti-tamper del connectorImpl** (`fw()` fatal fault) — defensa contra swap del transporte.

**Paralelos con lo ya estudiado**:
- vs **Galileo ([Bloque 80])**: Galileo sirve datos al browser (SignalR pull/push, in-station); nCloudDriver empuja al cloud (AMQP D2C/C2D). Ambos producen `BSentienceAlarmRecord` → Sentience es el destino común.
- vs **Reflow/MX60 ([Bloques 47-65])**: Reflow = SPA externa que consume Niagara por oBIX/BOX; nCloudDriver = la station empujando a un SaaS. Modelos opuestos de integración.
- usa **N-driver ([Bloque 78])** y el **tag `hon:CloudId` ([Bloque 82])** para identidad de punto.

**Para MX60 / Honeywell**: si MX60 necesita integrar con Forge/Sentience, este es el camino oficial (nCloudDriver + onboarding). El patrón de **comandos downlink autorizados por JWT con role mapping** es la referencia para cualquier control remoto seguro. Pero ojo: AMQP propio sobre qpid (no SDK Azure oficial) + sin device twin + SAS sobre CBS — es un stack legacy específico de Honeywell, no portable tal cual.

**Pendiente conocido**: `cloudBackup` (21 java) y `cloudConfig` (1 java) no analizados en profundidad (auxiliares de backup/config TLS); `cloudIotHubDep` (377 java) identificado como SDK qpid sin deep-dive de cada clase. Nombres internos no ofuscados (módulos Tridium limpios). `honCloudEasyOnboard` mencionado pero no destilado — candidato a bloque futuro si interesa el flujo de provisioning.
