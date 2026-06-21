# Bloque 89 — Stack MQTT/IoT: framework MQTT multi-cloud de Tridium (AWS/GCP/Azure/genérico) + driver Netvox LoRaWAN de Honeywell deofuscados

> Investigación empírica del **stack de mensajería MQTT/IoT**: el framework genérico `abstractMqttDriver` (Tridium) que abstrae **cuatro backends cloud** y el driver concreto `honMqttDriver` (Honeywell) que integra **sensores Netvox LoRaWAN**. Aterriza la mención "honMqttDriver Netvox" del [Bloque 32].
>
> 2 módulos: `abstractMqttDriver` (framework MQTT sobre N-driver, 1978 java mayormente SDKs) y `honMqttDriver` (driver LoRaWAN, 96 java).
> Decompilados Tridium/Honeywell limpios.
>
> Fuentes: `organized/{abstractMqttDriver,honMqttDriver}/<m>-rt/vineflower/{com.tridium,javax.baja,com.honeywell}.mqtt*/...` (+ `META-INF/maven/.../pom.properties`).
> Método: 2 sub-agentes + **verificación directa** de cada `extends`, las versiones de SDK (pom.properties) y si el AWS Secrets Manager se invoca (grep). `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (protocolo, payloads, flujo) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 88]. **Contrasta con [Bloque 83]** (nCloudDriver = stack Honeywell propio AMQP→Azure IoT Hub): aquí MQTT genérico multi-cloud. Conecta [Bloque 78] (N-driver), [Bloque 32] (honMqttDriver Netvox), [Bloque 75] (seguridad).

---

## 89.1 — Los dos módulos + el hallazgo multi-cloud `[CERT]`

| Módulo | Vendor | Clase raíz verificada | Rol |
|--------|--------|-----------------------|-----|
| `abstractMqttDriver` | Tridium | `BAbstractMqttDriverNetwork extends BNNetwork` (:61) | framework MQTT pluggable |
| `honMqttDriver` | Honeywell | `BHonMqttNetwork extends BAbstractMqttDriverNetwork` (:36) | driver Netvox LoRaWAN |

**HALLAZGO `[CERT]` (corrige la sospecha inicial)**: `abstractMqttDriver` NO está atado a AWS. Es un **framework MQTT multi-cloud** con cuatro autenticadores pluggables, todos `extends BAbstractMqttAuthenticator` (`extends BComponent`, abstract, :45):

| Backend | Autenticador | Auth |
|---------|--------------|------|
| **AWS IoT Core** | `BAwsMqttAuthenticator` (:38) + `BAwsJitpMqttAuthenticator` (JITP) | **mTLS X.509** (cert en CertManager Niagara, TLSv1.2) |
| **GCP IoT Core** | `BGcpAuthenticator` (:96) | **JWT RS256** (clave RSA local, renovación auto) |
| **Azure IoT Hub** | `BAzureMqttSasAuthenticator` (:111) | **SAS token** HMAC-SHA256 (clave en keyring) |
| **Broker genérico** | `BGenericMqttAuthenticator` (:65) | anonymous / user+pass / TLS client cert |

> **AWS Secrets Manager `[CERT]`**: el SDK `aws-java-sdk-secretsmanager` 1.12.698 está empaquetado en el JAR, pero **NO lo invoca ninguna clase** del driver (grep sobre `com.tridium`/`javax` = **0 resultados**). Es una dependencia transitiva del `aws-java-sdk-core` (fat-jar), no funcionalidad. Mi suposición preliminar de "este camino va por AWS Secrets Manager" queda **refutada**.

**Contraste con [Bloque 83]**: Honeywell tiene DOS caminos IoT distintos — `nCloudDriver` (stack propio Honeywell, AMQP→Azure IoT Hub, atado a Sentience/Forge) y este `abstractMqttDriver` (framework Tridium genérico, MQTT, cualquiera de 4 clouds). Son arquitecturas independientes.

---

## 89.2 — abstractMqttDriver: el framework MQTT multi-cloud `[CERT]`

Sobre el N-driver ([Bloque 78]): `BAbstractMqttDriverNetwork extends BNNetwork` (:61), devices `BAbstractMqttDevice` / `BAbstractMqttDriverDevice extends BNDevice implements IBaseMqttDevice` (:138/:160; `IBaseMqttDevice` solo declara `doSubscribeAll`/`doUnsubscribeAll`). ProxyExt base `BMqttClientAbstractProxyExt extends BProxyExt` (:49). License `tridium/mqtt`.

**Cliente MQTT `[CERT-a]`**: **Eclipse Paho MQTTv3 1.2.5** (`MqttAsyncClient`) `[CERT]` para genérico/Azure/GCP; **AWS IoT Device SDK Java 1.3.11** (`AWSIotMqttClient`) `[CERT]` para AWS. Transporte `tcp://` o `ssl://` según `BMqttConnectionType` (Anonymous/AnonymousOverSSL/UserLoginOverSSL). **QoS** 0/1/2 (`BMqttQualityOfService`, default 0), clean session default false, keep-alive 60 s, **LWT** (last will) completo (`MqttConnectOptions.setWill`).

**Autenticación por backend `[CERT-a]`**:
- **AWS**: mTLS X.509 — alias de cert del `BCertManagerService` (`ClientTlsParameters("tlsv1.2", alias)`, TLSv1.2 hardcoded). JITP: onboarding automático del cert vía firma Fox PKI (`BFoxSigningRequester`), clientID = CN del cert, puerto 8883.
- **GCP**: JWT RS256 firmado con clave RSA 2048 local (username literal `"unused"`, password = JWT, validez 20 min, renovación auto vía Clock). ClientID = `projects/{p}/locations/{r}/registries/{reg}/devices/{d}`. Clave pública en `<stationHome>/JwtKeys/<net>/<device>.pem`.
- **Azure**: SAS HMAC-SHA256 desde connection string, clave en keyring, renovación auto. Usa el módulo separado `com.tridium.azureUtils`.

**Modelo punto→topic `[CERT-a]`**: 8 ProxyExt concretos (publishers + subscribers para numeric/boolean/enum/string). Por punto: `topic` (String **libre, sin template ni sustitución**), `qoS`, `localPoint` (BOrd al punto fuente/destino). **Payload texto plano, NO JSON** (numeric=`Double.toString`, boolean=`"true"`/`"false"`, sin envelope/timestamp/schema). Publishers: `retained=true` + `publishMessageOnChange=true` por defecto, enlazados por `BLink` `publisherLink`. Subscribers: mapa `topic→puntos`, bulk subscribe en lotes de 500.

---

## 89.3 — honMqttDriver: integración de sensores Netvox LoRaWAN `[CERT]`

`BHonMqttNetwork extends BAbstractMqttDriverNetwork` (:36). **Aterriza el [Bloque 32]**: el módulo existe para integrar **sensores Netvox LoRaWAN** vía MQTT. NO configura endpoint/auth propio — delega 100% al `abstractMqttDriver` (cualquiera de los 4 backends). Lo que aporta es la **decodificación de payload LoRaWAN** + licencia `honLoRaMqtt` (vendor Honeywell).

**Jerarquía de sensores `[CERT]`**: `BHonLoraSensor extends BMqttClientDriverPointFolder` (:77, abstract) → 7 modelos Netvox `extends BHonLoraSensor` (`BR718ALoraSensor` :35 verificado):

| Modelo | Magnitudes `[CERT-a]` |
|--------|-----------------------|
| RA715 | temp, humedad, CO2, NH3, ruido, viento |
| RA716 | temp, humedad, PM2.5 |
| R718A | temp, humedad |
| R718CT | temperatura |
| R718G | luz (lux) |
| R718PE | distancia ultrasónica, nivel de llenado |
| R720E | TVOC, temp, humedad |

(todos + `battery`). Cada sensor es un `PointFolder` con `BNumericWritable` hijos por magnitud, `deviceEui`, `staleTime` (15-60 min).

**Decodificación de payload `[CERT-a]`** (`BHonLoraStringPoint`): el mensaje MQTT es JSON `{deveui, data}`; `data` es **Base64** → bytes → hex. El `reportType` está en el byte 2 (hex pos 4-6) y selecciona el parser. Decodificación **posicional fija por modelo** (ej. R718A reportType=1: byte[3]=battery×0.1, byte[4-5]=temp×0.01, byte[6-7]=hum×0.01). RA715 tiene 2 reportTypes (7=CO2/NH3/ruido, 12=temp/hum/viento). Topic `lora/{device-eui-con-guiones}/up`. Parser con Gson.

> **Nota de seguridad `[CERT-a]`** (aporta [Bloque 75]): `module://honMqttDriver/res/SensorDetails.json` (lookup `deviceEui→applicationEui+applicationKey` para activación OTAA) trae el **AppKey LoRaWAN de prueba conocido** `5a6967426565416c6c69616e63653039` = ASCII `"ZigBeeAlliance09"`. Es la clave de ejemplo pública de la spec LoRaWAN — si se usa en producción, las sesiones OTAA son triviales de comprometer. Verificar que cada sensor tenga su AppKey real, no el default.

**Conexión `[CERT-a]`**: al arrancar, `BHonMqttNetwork` lanza un `ConnectionMonitor` (espera ≤5 min a que los devices conecten) y luego `subscribeAll()`. Topología típica: `Sensor Netvox → Gateway LoRa → LNS → broker MQTT → Niagara`.

---

## 89.4 — Síntesis: dos paradigmas IoT en Niagara

**El stack MQTT en una frase**: Tridium provee un framework MQTT **genérico y multi-cloud** (`abstractMqttDriver`, Paho + 4 autenticadores pluggables AWS/GCP/Azure/genérico, payload texto plano por topic libre); Honeywell lo especializa en `honMqttDriver` para **ingerir sensores Netvox LoRaWAN** decodificando el payload binario por modelo.

**Dos paradigmas IoT contrastados (clave del mental model)**:

| | **abstractMqttDriver/honMqtt ([Bloque 89])** | **nCloudDriver/Sentience ([Bloque 83])** |
|---|---|---|
| Protocolo | **MQTT** (Paho/AWS SDK) | **AMQP 1.0** (qpid propio) |
| Cloud | **multi**: AWS/GCP/Azure/genérico | Azure IoT Hub (atado a Sentience) |
| Quién | Tridium (framework) + Honeywell (LoRa) | Honeywell propio |
| Payload | texto plano por topic | mensajes tipados (alarm/point/history) |
| Caso | ingesta de sensores genéricos / LoRaWAN | BMS completo bidireccional + control remoto |
| Modelo | NO sube el modelo semántico | model sync JSON-LD ([Bloque 85]) |

→ Niagara/Honeywell ofrece **dos rutas IoT independientes**: una genérica multi-broker (MQTT) y una integrada propietaria (AMQP/Sentience). Elegir según el destino cloud y si se necesita control remoto + modelo semántico.

**Seguridad ([Bloque 75])**:
- **AppKey LoRaWAN default** (`ZigBeeAlliance09`) en `SensorDetails.json` — riesgo si no se reemplaza.
- **TLSv1.2 hardcoded** en el path AWS — bueno (no degradable) pero sin TLS1.3.
- **SDKs viejos empaquetados**: AWS SDK 1.12.698, Paho 1.2.5, jackson 2.16.1, joda-time 2.8.1, **AWS Secrets Manager bundled sin uso** — superficie de CVEs de dependencias (mismo patrón que [Bloques 32.3, 85]). El fat-jar arrastra el SDK AWS completo aunque solo se use el cliente IoT.
- Credenciales por backend en lugares distintos: CertManager (AWS X.509), keyring (Azure SAS), `BPassword` ofuscado + PEM en disco (GCP) — auditar los tres.

**Para MX60 / Honeywell**: si MX60 necesita publicar/suscribir a un broker MQTT (cualquier cloud), `abstractMqttDriver` es la vía nativa multi-cloud — payload texto plano por topic, sin schema. Para LoRaWAN, `honMqttDriver` es la referencia de decodificación Netvox. Pero el payload sin JSON/timestamp/schema limita interoperabilidad: para integraciones ricas, conviene un publisher string con JSON propio.

**Pendiente conocido**: el detalle de cada uno de los 4 clientes (Paho/AWS/GCP/Azure) se citó vía sub-agente `[CERT-a]`; no se decompiló cada `connect()` línea a línea. Los SDKs empaquetados (~1900 java) no se analizaron clase por clase (son libs estándar). Otros OEM Honeywell pendientes: `honPlantControllerHMI` (liga [Bloque 32]), `lonhoneywellAXWizards`, `knxnetIp`.
