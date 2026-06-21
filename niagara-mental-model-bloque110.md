# Bloque 110 — `honRemoteConfig` + `honRemoteConfigBacnet`: el **receptor C2D** de comandos remotos del cloud Honeywell — cierra el arco abierto por los Bloques 83/84/85, deofuscado

> Investigación empírica de los dos módulos OEM Honeywell que implementan el **lado receptor** del canal **cloud-to-device (C2D)**: `honRemoteConfig` (`com.honeywell.remoteconfig.*`, symbol `rc`, vendor **Honeywell `4.6.1.1.0`**, build **2019-04-04**, descripción *"Honeywell Remote Configuration Tool"*) y su extensión `honRemoteConfigBacnet` (symbol `rcb`, mismo vendor/versión, build **2019-03-20**, *"Honeywell Remote Configuration Tool extension for Bacnet"*).
>
> **El hallazgo central** `[CERT]`: esto **NO es un motor de comandos propio**. `BAbstractSystemCommand extends BCloudCustomCommand` (`com.tridium.nc.cmds.BCloudCustomCommand`) — la **misma clase base** que el [Bloque 85] identificó para `BUpdateModelSyncStatusCommand`/`BUpdateModelIdCommand`. `honRemoteConfig` **registra sus comandos dentro del `BCloudCommands` del `BCloudSentienceDevice`** del `nCloudDriver` ([Bloque 83.5]). Es decir: **el transporte, la recepción AMQP C2D y la autorización JWT son del `nCloudDriver` (Bloque 83); `honRemoteConfig` solo aporta comandos concretos nuevos** que se enchufan al despachador por nombre. Cierra literalmente la pregunta abierta de los Bloques 83-85 ("¿quién recibe y qué comandos hay además de write/invoke?").
>
> Fuentes: `organized/honRemoteConfig/honRemoteConfig-rt/vineflower/com/honeywell/remoteconfig/` (49 java) + `organized/honRemoteConfigBacnet/honRemoteConfigBacnet-rt/vineflower/...` (12 java) + ambos `META-INF/module.xml`. Decompilación vineflower **limpia** (nombres Honeywell **no ofuscados** — código legible).
> Método: **verificación directa por mí** de cada `extends`/`implements`, del `module.xml` (deps + tipos registrados), de los 7 `COMMAND_NAME` verbatim, del `RemoteCommandRegistry` (cómo registra en el device cloud), del `RemoteCommandProcessor` (flujo recepción/respuesta), del `BModelExporter` (puente a [Bloque 85]) y de los comandos BACnet. `[CERT]` = verificado verbatim por mí; `[CERT-a]` = cita de sub-agente; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 109]. **Cierra el arco cloud Honeywell** (Bloques 83/84/85): [Bloque 83] (nCloudDriver, transporte AMQP IoT Hub + `BCloudCommands` + auth JWT) es el **receptor físico**; [Bloque 84] (onboarding) **monta** el stack; [Bloque 85] (model sync) es lo que **dispara** el comando `post.core.publishmodel`. Conecta con [Bloque 82] (tags `hon:`/identidad), [Bloque 75] (superficie de control remoto).

---

## 110.1 — Qué es: la capa de "comandos extra" sobre el despachador C2D del nCloudDriver `[CERT]`

El [Bloque 83.5] dejó el cuadro casi cerrado: el `nCloudDriver` recibe mensajes **C2D por IoT Hub AMQP** (link `/devices/{id}/messages/devicebound`), los autoriza con JWT (`CloudLoginModule`, jose4j RS256/ES256) y los despacha por nombre a comandos `SystemCommand` registrados en `BCloudCommandsDeviceExt`. Mencionó comandos nativos del driver: `CloudPointReadCommand`, `CloudPointWriteCommand`, `InvokeCommand`, etc.

`honRemoteConfig` es **un módulo que añade comandos C2D nuevos a ese mismo despachador**, sin tocar el transporte:

| | **`honRemoteConfig`** | **`honRemoteConfigBacnet`** |
|---|---|---|
| Symbol / vendor | `rc` / Honeywell `4.6.1.1.0` (2019-04) | `rcb` / Honeywell `4.6.1.1.0` (2019-03) |
| Rol | servicio + 3 comandos C2D "core" | extensión: 1 provider + 4 comandos C2D BACnet |
| Entry | `BRemoteConfigurationService extends BAbstractService implements IAppConfiguration` `[CERT, :51]` | `BBacnetCommandProvider extends BAbstractCommandProvider` `[CERT, :12]` |
| Base de comando | `BAbstractSystemCommand extends BCloudCustomCommand` `[CERT]` | (hereda la misma base vía dep `honRemoteConfig-rt`) |
| Deps clave | `nCloudDriver`, `cloudConnector`, `cloudSentienceConnector`, `cloudIotHubConnector` (todas `4.4`) `[CERT, module.xml]` | `honRemoteConfig-rt`, `nCloudDriver`, `bacnet-rt` `[CERT, module.xml]` |

> **Caveat de versión (consistente con [Bloque 84]/[Bloque 85])** `[CERT]`: ambos módulos son **Honeywell 4.6.1.1.0, build 2019** — **mucho más viejos** que el `nCloudDriver` 2023.14 que el [Bloque 83] destiló. Declaran deps `4.4`. Es el mismo patrón "módulo Honeywell viejo montado sobre el stack cloud Tridium moderno": funciona porque dependen del contrato `BCloudCustomCommand`/`BCloudCommands`, estable desde 4.4.

---

## 110.2 — `BRemoteConfigurationService`: el servicio que registra los comandos en el device cloud `[CERT]`

`BRemoteConfigurationService extends BAbstractService implements IAppConfiguration` (`:51`), icon `remoteMgmt.png`. **4 slots** `[CERT]`:

| Slot | Tipo | Función |
|------|------|---------|
| `cloudDevice` | `baja:Ord` (targetType `nCloudDriver:CloudSentienceDevice`) | apunta al `BCloudSentienceDevice` del [Bloque 83.5] sobre el que se registran los comandos |
| `commandProviders` | `BCommandProviderExt` (hidden) | contenedor de los `ICommandProvider` (core + extensiones) |
| `modelExporter` | `BModelExporter` | puente al `BModelSyncService` del [Bloque 85] (ver 110.5) |
| `lastHistoryIndex` | `int` (readonly) | cursor de historiales (infra del `CloudHistoryManager`) |

**El ciclo de arranque** `[CERT]`: en `atSteadyState()`/`descendantsStarted()` el servicio crea un `RemoteConfigurationApp` (vía `RemoteConfigurationAppFactory`), resuelve el `BCloudSentienceDevice` (`resolveCloudDevice()`) y arranca la app. **El registro de comandos es el corazón** (`RemoteConfigurationApp.startApp()` → `RemoteCommandRegistry`):

```java
// RemoteCommandRegistry.registerCommands(...)  [CERT, verbatim]
BCloudCommands cloudCommands = this.device.getCommands().getCloudCommands();
for (BAbstractSystemCommand command : systemCommands) {
   if (cloudCommands.get(SlotPath.escape(command.getCommandName())) == null)
      cloudCommands.add(SlotPath.escape(command.getCommandName()), command);   // ← se inserta como hijo del BCloudCommands del nCloudDriver
}
```

**Esto ES el cierre del arco**: el comando Honeywell queda como **hijo del `BCloudCommands` del device del `nCloudDriver`**, indexado por su `commandName`. Cuando llega un C2D por IoT Hub, el despachador del `nCloudDriver` ([Bloque 83.5]) busca por nombre en ese contenedor y encuentra el comando Honeywell. `honRemoteConfig` no abre sockets, no escucha AMQP, no maneja JWT — **reutiliza todo el receptor del nCloudDriver**.

**Modelo de proveedores pluggables** `[CERT]`: `BCommandProviderExt implements IRemoteCommandRepository` agrega los comandos de **todos sus hijos `ICommandProvider`** (`getChildren(ICommandProvider.class)`). El `BDefaultCommandProvider` (`PROVIDER_NAME = "core"`) trae los 3 comandos core; `honRemoteConfigBacnet` aporta un `BBacnetCommandProvider` (`"bacnet"`) que, dropeado bajo el ext, suma sus 4 comandos. La extensibilidad es por **composición de BComponents**, no por reflexión.

---

## 110.3 — El flujo receptor: de C2D AMQP a respuesta (sync + async event uplink) `[CERT]`

`BAbstractSystemCommand extends BCloudCustomCommand` define `doRun(JSONObject receivedJSON, Map responseProperties, Context)` — **el método que el despachador del nCloudDriver invoca** al llegar el C2D. El flujo (`RemoteCommandProcessor.processCommand`):

```
C2D AMQP (IoT Hub, Bloque 83.3)  →  nCloudDriver despacha por commandName (Bloque 83.5)
   →  BAbstractSystemCommand.doRun(receivedJSON, responseProperties, ctx)
        1. new CommandContext(...)  → extrae "CommandId" de responseProperties
        2. createRequestObj():  parsea receivedJSON["commandParams"] → { query, headers, body }   (JSON Tridium)
        3. handleCommandRequest(req, resp)  → lógica concreta del comando
        4. setResponseParameters(responseProperties, resp.asJson())  → respuesta SÍNCRONA
```

**Códigos de estado propios** (`Constants.java`) `[CERT]`: `200` OK, `500` error (HTTP-like síncrono) + **asíncronos**: `220` async-OK, `221` async-OK-multipart, `520` async-error. Los comandos largos (discovery, model sync) heredan de `BAbstractAsynchronousSystemCommand`: lanzan un `BSimpleJob` (`submitJob`), un `AsynchronousSystemCommandSubscriber` escucha `BJob.jobState`, y al completar llaman `processAsyncJob(job)`.

**La respuesta asíncrona viaja UPLINK como evento** (`RemoteCommandProcessor.sendAsyncResponse`) `[CERT]`: arma un `EventData(uuid, time, connectorId, commandId, commandName, payload, ..., "DeviceConfiguration")`, lo encoda con `getCloudDevice().getFactory().createNewEventRequestMsg()` y lo empuja por `connector.sendMessage(...)` — o sea **por el mismo canal D2C del nCloudDriver**. El cloud correlaciona por `CommandId`. Es exactamente el patrón "comando largo → ack inmediato + evento de resultado después" sobre IoT Hub.

---

## 110.4 — Catálogo de comandos C2D concretos (7 comandos) `[CERT]`

**Todos los nombres verificados verbatim** en las constantes `COMMAND_NAME`/`module.xml`:

| Comando (C2D name) | Clase | Tipo | Qué hace `[CERT]` |
|---|---|---|---|
| `get.core.gateway` | `BGetCoreGateway` | async | Devuelve la **identidad del gateway**: `systemGuid` (`connector.getId()`), `systemId`/`systemType`/`ownershipCode` (del `BSentienceConnectorImpl`, [Bloque 83.4]), `station`, `hostID`, `hostName` + **licencia `tridium/nCloudDriver`** (featureName/expiration/isExpired) |
| `get.core.networks` | `BGetCoreNetworks` | sync | BQL `select * from driver:DeviceNetwork` → lista todas las redes de driver con `name`/`displayName`/`type`/`status`(flag/enable/faultCause) + `_links` (self/parent ORD) |
| `post.core.publishmodel` | `BPostCorePublishModel` | async | **Dispara el model sync del [Bloque 85]** vía `BModelExporter.export()`; al completar emite evento `220` con `modelId`+`statusUpdateTime`, o `520` si falla (ver 110.5) |
| `get.bacnet.devices` | `BGetBacnetDevices` | async | **Discovery "offline"**: BQL `select * from bacnet:BacnetDevice` (lee lo ya modelado en la station, no escanea el wire) → lista de devices BACnet |
| `get.bacnet.points` | `BGetBacnetPoints` | async | Enumera los puntos bajo los devices BACnet (recorre los `BBacnetDevice`) |
| `post.bacnet.cloudhistory` | `BPostBacnetCloudHistory` | async | **Crea cloud history exports** para puntos BACnet (job `BBacnetPublishCloudHistoryJob`) → publica historiales al cloud ([Bloque 83.5] `BCloudHistoryExport`) |
| `delete.bacnet.cloudhistory` | `BDeleteBacnetCloudHistory` | async | Quita esos exports (job `BBacnetUnpublishCloudHistoryJob`) |

**Caracterización del surface** `[CERT]`: es un canal **read-mostly + trigger + provisioning de historiales**. Lectura: identidad del gateway, inventario de redes/devices/puntos. Trigger: republicar el modelo semántico. Provisioning: alta/baja de cloud history exports BACnet. **NO incluye escritura de puntos ni invocación de acciones** — eso lo aporta el `nCloudDriver` nativo (`CloudPointWriteCommand`/`InvokeCommand`, [Bloque 83.5]). `honRemoteConfig` es el complemento de **descubrimiento/comisionado remoto**, no de control de proceso.

> **"Offline discovery"** `[CERT]`: los comandos BACnet **no hacen un escaneo BACnet vivo** — leen el `bacnet:BacnetDevice` que ya está en la base de la station (vía BQL) dentro de un `BSimpleJob`. "Offline" = sin tráfico de red BACnet, solo consulta del modelo persistido. Sirve para que el cloud sepa qué hay modelado, no para detectar hardware nuevo.

---

## 110.5 — La bisagra con el [Bloque 85]: `post.core.publishmodel` → `syncModel` `[CERT]`

El [Bloque 85] dejó el model sync con un trigger `syncModel` **manual** (acción WB) y un downlink de **status** (`BUpdateModelSyncStatusCommand`/`BUpdateModelIdCommand`). El `BModelExporter` de este bloque aporta el **trigger remoto** que faltaba:

```java
// BModelExporter.export(...)  [CERT, verbatim]
this.modelSyncService = resolveModelExporter();                     // Ord → BModelSyncService (Bloque 85)
this.modelSyncService.invoke(modelSyncService.getAction("syncModel"), null);  // ← dispara el sync de B85
this.subscriber = new ObjectSubscriber(listener);
this.subscriber.subscribe(this.modelSyncService);                   // escucha el slot "modelSyncStatus"
```

`BModelExporter` resuelve un Ord `sentienceModelSyncService` y **invoca la acción `syncModel`** del `BModelSyncService` del módulo `SentienceModelSync` ([Bloque 85.3]) — mismos slots `modelId`/`modelSyncStatus`/`statusUpdatedTime` verificados allá. Suscribe el slot `modelSyncStatus`; cuando pasa a `COMPLETED`, `BPostCorePublishModel.statusChanged()` empuja el evento `220` con `modelId` uplink. `FAILED` → `520`.

> **Refinamiento al [Bloque 85]** `[CERT]`: hay **dos puertas C2D distintas al model sync**, no una. (a) `BUpdateModelSyncStatusCommand`/`BUpdateModelIdCommand` (módulo `SentienceModelSync`) — el cloud **escribe** status/modelId de vuelta en el service. (b) `post.core.publishmodel` (este módulo) — el cloud **dispara** un sync nuevo y recibe el `modelId` por evento async. La primera es push de estado; la segunda es comando de re-publicación. Ambas sobre `BCloudCustomCommand`/IoT Hub C2D.

**Infra latente: `CloudHistoryManager`** `[CERT]`: gestiona altas de `nCloudDriver:CloudHistoryExport` (tags `rc:cloudhist`, prefijos `RCC_`/`RCH_`, idempotente vía BQL). En `honRemoteConfig` **no hay un comando core que lo invoque** (el `CoreCommandFactory` solo registra 3); es la base que **`honRemoteConfigBacnet` usa** para `post.bacnet.cloudhistory`. Queda como capacidad de "remote history provisioning" que el split base/BACnet refleja.

---

## 110.6 — `honRemoteConfigBacnet`: la extensión por protocolo `[CERT]`

12 java, dep de `honRemoteConfig-rt` + `bacnet-rt`. Aporta un **segundo `ICommandProvider`** (`BBacnetCommandProvider`, `PROVIDER_NAME = "bacnet"`) que, dropeado bajo el `BCommandProviderExt` del servicio, suma sus 4 comandos al registro del device cloud (110.4). Estructura `[CERT, module.xml]`:

- **4 comandos**: `BGetBacnetDevices`, `BGetBacnetPoints`, `BPostBacnetCloudHistory`, `BDeleteBacnetCloudHistory` (todos `extends BAbstractAsynchronousSystemCommand`).
- **4 jobs** (`BSimpleJob`): `BBacnetOfflineDeviceDiscoveryJob`, `BBacnetOfflinePointDiscoveryJob`, `BBacnetPublishCloudHistoryJob`, `BBacnetUnpublishCloudHistoryJob`.
- **Factory** `BacnetCommandFactory` (mismo patrón estático que el core).

> Es el **patrón de extensibilidad por protocolo** que el [Bloque 85.4] ya mostró para los discoverers/cloud-writers de `fcModelSync` (BACnet/Niagara): el core define el contrato genérico (`get/post` sobre redes/puntos) y cada driver aporta un provider con la implementación BACnet-específica (BQL sobre `bacnet:BacnetDevice`, cloud history de puntos BACnet). **No hay variante LON/Modbus análoga en el corpus** — solo se shippeó la extensión BACnet, coherente con que el grueso del parque Honeywell-Sentience es BACnet ([Bloque 77]).

---

## 110.7 — Hallazgos, seguridad y cierre del arco cloud `[CERT]`

**Hallazgos CERT**:
1. **`honRemoteConfig` NO es un motor C2D propio** — `BAbstractSystemCommand extends BCloudCustomCommand` y los comandos se registran como hijos del `BCloudCommands` del `BCloudSentienceDevice` del `nCloudDriver`. Transporte/recepción/auth = [Bloque 83]. Esto **cierra el arco** Bloques 83-85.
2. **Catálogo de 7 comandos C2D** con nombres dotted (`get.core.gateway`, `get.core.networks`, `post.core.publishmodel`, `get.bacnet.devices`, `get.bacnet.points`, `post.bacnet.cloudhistory`, `delete.bacnet.cloudhistory`).
3. **`post.core.publishmodel` es el trigger remoto del model sync** del [Bloque 85] — segunda puerta C2D al model sync (refinamiento a B85).
4. **Patrón sync/async** con códigos propios (200/500 sync; 220/221/520 async) y respuesta async como **evento D2C** (`EventData "DeviceConfiguration"`) correlacionado por `CommandId`.

**Seguridad (aporta al [Bloque 75])**:
- **Autorización heredada, no propia** `[CERT]`: ningún comando de `honRemoteConfig` valida identidad/rol. Toda la defensa es el JWT del `CloudLoginModule` del `nCloudDriver` ([Bloque 83.5]: jose4j RS256/ES256, role mapping cloud→Niagara). Comprometer el JWKS/issuer del nCloudDriver = poder ejecutar también estos 7 comandos. **El surface de control remoto del [Bloque 83] crece** con descubrimiento de inventario (gateway/redes/devices/puntos) y republicación de modelo.
- **Fuga de reconocimiento** `[CERT]`: `get.core.gateway` expone `hostID`/`hostName`/`systemOwnershipCode`/licencia; `get.core.networks`+`get.bacnet.devices/points` exponen la **topología completa de drivers y BACnet** de la station. Para un atacante con el canal C2D, es reconocimiento de building servido por la propia station (complementa la exposición del modelo JSON-LD del [Bloque 85.5]).
- **ORD/BQL injection (bajo)** `[CERT]`: `BGetBacnetDevices` concatena el `src` del request del comando en un ORD: `BOrd.make("station:|" + src + "|bql:select * from bacnet:BacnetDevice")`. El `src` viene del `commandParams` del C2D (autenticado). Riesgo real bajo (input de comando autorizado), pero es un defecto de calidad — un `src` malicioso podría redirigir el BQL/ORD. Mismo perfil que el path-traversal de bajo riesgo del [Bloque 109.4].
- **Limpio en lo grave** `[CERT]`: comandos en factory **estática** (sin carga reflexiva por nombre desde el payload), sin `Runtime.exec`, sin crypto propia, sin credenciales hardcodeadas. La superficie es de **datos y trigger**, no de ejecución arbitraria.

**El arco cloud Honeywell, ahora completo (Bloques 82→85→110)**:
```
[Bloque 82] tags hon: → identidad semántica del building
[Bloque 84] onboarding → monta CloudConnector + nCloudDriver + ModelSync + tags
[Bloque 83] nCloudDriver → transporte AMQP IoT Hub (D2C datos / C2D comandos) + auth JWT + BCloudCommands
[Bloque 85] model sync → sube el grafo JSON-LD a Azure Blob (side-channel) + status por C2D
[Bloque 110] honRemoteConfig → AÑADE 7 comandos C2D al despachador del nCloudDriver:
              inventario remoto (gateway/redes/BACnet) + trigger de model sync + cloud-history provisioning
```

**Para MX60 / Honeywell**: el patrón a replicar es **extender un despachador de comandos existente por composición** (un `CommandProvider` pluggable que registra `CustomCommand`s en el contenedor del driver) en vez de abrir un canal propio — reutiliza transporte y auth, y mantiene la autorización en un solo lugar. La contracara: el surface de control remoto **se amplía sin auditoría adicional propia** del módulo, y el inventario remoto (gateway/topología) es información sensible que conviene gatear más allá del JWT genérico del driver.

**Pendiente conocido**: el detalle interno de `BCloudCommands`/`BCloudCommandsDeviceExt` (clases `com.tridium.nc.cmds.*` del `nCloudDriver`) se referencia pero pertenece al [Bloque 83] (Tridium, no Honeywell). `CloudHistoryManager`/`HistoryManager` (infra de batch de historiales) leídos pero no destilados clase-por-clase — son auxiliares del `post.bacnet.cloudhistory`.
