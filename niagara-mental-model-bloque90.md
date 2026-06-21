# Bloque 90 — Familia honPlantController (núcleo): BTP IPC local + managed switch RSTP/switchport + JNI `plantctrl` + migradores EagleHawk→PanelBus, deofuscados

> Investigación empírica del **núcleo del módulo OEM Honeywell `honPlantController`**: el runtime de comunicación del controlador **BEATS ADV** (Honeywell Advanced Controller sobre Ubuntu Core / snap). Cubre el protocolo propietario **BTP** (Building Technology Protocol) como **IPC local**, la gestión del **switch Ethernet interno** (RSTP + switchport), el **puente JNI `plantctrl`**, y los dos **migradores** que actualizan stations EagleHawk legacy al stack moderno.
>
> 4 sub-módulos: `honPlantController` (rt/ux/wb, 142 java — 28 son Gson bundled), `honPlantControllerEHMigrator` (8 java, adaptador EagleHawk), `honPlantControllerMigrator` (68 java, framework de migración genérico). Gson empaquetado en el JAR = SDK, ignorado.
>
> Fuentes: `organized/honPlantController*/<m>-rt/vineflower/com/honeywell/honplantcontroller/...` + `.../honPlantControllerMigrator/...`.
> Método: 3 sub-agentes Explore + **verificación directa** de cada `extends`, puertos, preamble del frame BTP, bind del `ServerSocket`, `loadLibrary`, y `ITERATION_COUNT`. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (estructura de protocolo, reqIds, flujo de migración) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 89]. **ATERRIZA y CORRIGE el [Bloque 32]** (que listó el inventario de `honPlantController-rt` e infirió "BTP = IP remoto al CIPer sin JNI"). Conecta [Bloque 86] (I/O campo PanelBus/OnboardIO Centraline — destino del migrador), [Bloque 88] (BEATS PUC + cadena de campo), [Bloque 75] (seguridad). El HMI de esta familia (`platHMI`) va aparte en [Bloque 91].

---

## 90.1 — La familia + clases raíz verificadas `[CERT]`

| Sub-módulo | Clase raíz verificada (archivo:línea) | Rol |
|------------|----------------------------------------|-----|
| `honPlantController` | `BHonPlantControllerService extends BAbstractService implements BIBTPPanelBusHandler` (`service/BHonPlantControllerService.java:95`) | Servicio de estación: agrega RSTP + switchport + BTP listener + device |
| `honPlantControllerMigrator` | `BHonPlantControllerMigratorService extends BAbstractService` (`service/…:97`) | Servicio que lanza el job de migración |
| `honPlantControllerMigrator` | `BOnlineMigrationJob extends BSimpleJob` (`job/…:37`) | Job de upgrade in-situ de la station |
| `honPlantControllerMigrator` | `BCustomIOMigrator extends BObject` (abstract, `utilities/interfaces/…:17`) | Interfaz polimórfica de migración de I/O |
| `honPlantControllerEHMigrator` | `BOnboardIOMigrator extends BCustomIOMigrator` (`eaglehawk/…:67`) | Adaptador EagleHawk: OnboardIO→PanelBus |

**HALLAZGO `[CERT]` — el host es un controlador BEATS ADV con Linux, no el Supervisor Windows.** El servicio sólo opera "on Beats Adv" validando `System.getProperty("os.name")` (gate en `BHonPlantControllerService.java:329` + `PlantCtrlCommon.java:50`). En Windows/desarrollo entra en modo offline y omite el JNI. Esto **corrige la lectura del [Bloque 32]**, que asumía un Supervisor x86_64 invocando `btp/*`+`rstp/*` por IP hacia un CIPer remoto.

Propiedades del servicio raíz (`@NiagaraProperty`) `[CERT-a]`: `RSTPConfiguration` (`BRSTPConfiguration`), `SwitchPortConfiguration` (`BSwitchPortConfiguration`), `HonPlantControllerHMI` (hidden), `BTPConnectionListener` (hidden), `BTPDevice` (`BBTPDeviceObject`, hidden), `overridePriority` (Station/Network).

---

## 90.2 — BTP (Building Technology Protocol): IPC **local**, no IP remoto `[CERT]` + `[CERT-a]`

**CORRIGENDUM al [Bloque 32]:** BTP **no** es una conexión TCP saliente al PLC remoto. Es **IPC loopback-intencionado** entre la station Niagara y el daemon BLE/wireless del propio hardware. Verificado `[CERT]` en `PlantCtrlCommon.java:13-14`:
- `SWITCH_PORT_SOCKET_PORT = 10000`
- `BTP_CONNECTION_SOCKET_PORT = 11000`

El `BTPServer` actúa como **servidor** (`ServerSocket.accept()`, poll con `Thread.sleep(100)`, fail-detect 3000 ms); el cliente que se conecta es el daemon de la app móvil BLE en el mismo host `[CERT-a]`.

### Frame binario sobre TCP `[CERT]` (preamble verificado)
`SocketPayloadMessageStructcure.java:12`: `PREAMBLE_DATA = new byte[]{85, -1}` → **`0x55 0xFF`**. Estructura `[CERT-a]`:
```
[PREAMBLE 2B = 0x55 0xFF][HEADER 4B = len payload BE][CMD_ID 1B][PAYLOAD NB = JSON UTF-8][CRC 2B = CRC-16]
```
`CMD_ID`: `0x80` request · `0x81` heartbeat · `0x82` error. Header fijo hasta payload = 7 B; mínimo sin payload = 9 B; tope de respuesta = 10240 B. El payload JSON puede venir comprimido (`{"compression":"true","data":"<base64-gzip>"}`).

### Tipos de request `[CERT-a]` (discriminados por `reqId`)
| `reqId` | Request | `respId` |
|---------|---------|----------|
| 6 | FileDataRead | 774 |
| 14 | ReadProperty | 782 |
| 16 | WriteProperty | 784 |
| 18 | Query (mini-SQL) | 786 |
| 264 | DeviceDiscovery | 256 |

No hay Subscribe ni discovery por rango funcional (devuelve sólo el device propio).

### Modelo de objetos BTP (calcado de BACnet) `[CERT-a]`
Tipos: Device(8), File(10), **IO Device Data(136)**, **Terminal(137)** (los 2 últimos propietarios). `objectId` = bits[31:22] tipo + bits[21:0] instancia (`0x3FFFFF`). Propiedades 0–371 = BACnet estándar; **propietarias desde 5000** (`5001 CHANNEL_ADDRESS`, `5003 IO_MODULE_SERIAL_NUMBER`, `5005 IO_TERMINAL_LIST`, `5008 SIGNAL_DIRECTION`, `5012 TERMINAL_OVERRIDE_TIMER`, `5053 STATUS`…).

**WriteProperty restringido `[CERT-a]`**: sólo objetos tipo **137 (Terminal)** son escribibles (resto → `WRITE_ACCESS_DENIED`); pId gestionados: `85 PRESENT_VALUE`, `81 OUT_OF_SERVICE`, `5012 TERMINAL_OVERRIDE_TIMER`.

**Query `[CERT-a]`**: mini-parser `select [*|pids] from objectId [where (pid:N op value) [and|or …]]`, operadores `eq/le/lt/gt/ne/ge`, soporte `distinct`.

El árbol BTP se puebla descubriendo PanelBus vía BQL `[CERT-a]`: `BOrd.make("slot:/Drivers|bql:select * from clPanelBus:PanelbusNetwork")` — liga directo al [Bloque 86].

---

## 90.3 — El managed switch interno: RSTP + switchport `[CERT]`

El controlador BEATS ADV trae un **switch Ethernet gestionado de 3 puertos** que Niagara configura por socket local (10000) y persiste a ficheros del snap vía JNI.

**`BRSTPConfiguration extends BComponent`** (`network/rstp/…:168`) `[CERT]` — topología en anillo redundante (Rapid Spanning Tree):
- `bridgePriority` (`BBridgePriorityEnum`, 0–61440 paso 4096, def 49152), `port{1,2,3}Priority` (0–240 paso 16, def 128) `[CERT-a]`
- `helloTime` 1–10 s (def 2), `forwardDelayTime` 4–30 s (def 15), `maximumAgingTime` 6–40 s (def 20) `[CERT-a]`
- Roles `unknown/disabled/root/designated/alternate/backup/master`; estados `discarding/learning/forwarding` `[CERT-a]`
- Topología **`daisychain` hardcodeada** en `getConfig()` (no editable en UI) `[CERT-a]`
- Config persistida a `$SNAP_DATA/managed-switch/etc/link.cfg` por JNI `[CERT-a]`
- `overridePlatformConfig` (`always/onlyOnce/never`) decide si Niagara pushea su config o lee la del hardware `[CERT-a]`

**`BSwitchPortConfiguration extends BComponent`** (`network/switchport/…:81`) `[CERT]` — 3 puertos físicos (`BSwitchPortConfigDetails extends BComponent`), por puerto `[CERT-a]`: velocidad (`ten/hundred/thousand/Disconnected`), modo (`half/full/disconnected`), **MAC whitelist** (`macAddressFilter` + `allowedMACAddress` máx 16), `cableDiagnostics`, `status`. Comandos del switch (`getPortLinkStatus/enablePort/portCableDiagnostics/getMACAddressList/enableAcessControl/setAccessControlList/getRSTPStatus`); mensajería `request/response/notify`. Socket `localhost:10000` (`SwitchPortHandler`), config a `$SNAP_DATA/managed-switch/etc/switch.cfg`.

---

## 90.4 — Puente JNI `plantctrl` + modelo de despliegue `[CERT]`

`comm/JNIRequest.java:20`: `System.loadLibrary("plantctrl")` `[CERT]` → `libplantctrl.so` (Linux/QNX). **Activo en runtime, no legacy.** Métodos nativos `[CERT-a]`: `init()`, `jniSetLibraryDebugLevel(int)`, `jniReadFromFile(byte[],int)`, `jniWriteToFile(byte[],int,byte[],int)`. `CommManager` (singleton) sólo se inicializa si `os.name ∈ {LINUX, QNX}`; `isJNIAvailable()` es el gate previo a toda op de switch. RSTP y switchport leen/escriben sus `.cfg` a través de él.

**Cadena de despliegue real `[INFER]` + `[CERT-a]`:**
```
[App móvil] --BLE--> [daemon wireless Linux] --TCP localhost:11000 (BTP)--> [BTPServer en station Niagara]
        --handlers reqId--> [árbol Niagara: BBTPDeviceObject→IODeviceData→Terminal]
        --JNI plantctrl.so--> [SNAP_DATA / hardware BEATS ADV]
[SwitchPortHandler] --TCP localhost:10000--> [managed switch de 3 puertos]
```

---

## 90.5 — Los migradores: upgrade in-situ EagleHawk→PlantController `[CERT]` + `[CERT-a]`

No migran desde un formato propietario externo: **operan sobre la station Niagara viva** (`Sys.getStation()`, online) o sobre **`config.bog`** en disco (offline, `BOG_FILE = "config.bog"` verificado `[CERT]` en `model/Const.java:59`). El `.bog` es el Baja Object Graph nativo de N4.

**Patrón de extensión `[CERT]`**: el módulo genérico define las interfaces abstractas (`BCustomIOMigrator`, `BFALHomeMigrator`, `BStationInfoAccessor`, todas `extends BObject`); el EHMigrator registra implementaciones en el registry Niagara; el genérico las descubre por `Sys.getRegistry()` y despacha por `StationType` `[CERT-a]`.

**`StationType` (4 valores) `[CERT]`** (`enums/StationType.java:4-7`): `EAGLEHAWK` ("EHN4/Ciper50/CP-NX"), `BEATS_ADVANCED`, `BEATS_ADVANCED_WITH_HMI_PRIVATE`, `GENERIC`.

**`BOnlineMigrationJob extends BSimpleJob` `[CERT]` — secuencia de `run()` `[CERT-a]`:** backup `.bog` → `migrateOnboardIo()` (despacho polimórfico `BCustomIOMigrator`) → elimina network legacy → migra HMI (devices, alarmas, descriptors, FALs, schedules, HMINetwork) → `removeLegacyServices()` (EagleHawk HMI service + authenticators + LED recipients) → `addHonPlantControllerServices()`. Offline: `OfflineMigrationWorker extends Worker` (Workbench progress dialog).

**Migración física de I/O `[CERT-a]`** (`BOnboardIOMigrator`): convierte `clOnboardIO:OnboardIODevice` → `clPanelBus:PanelbusNetwork` con módulos Snap-on IO. `OnboardIODeviceType`: `DEVICE_14IO`/`DEVICE_26IO`. `OnboardIOMigrationOptions` mapea a combos de hardware comercial (`IOD-8DOR-S`, `IOD-16UIO-S`, `IOD-4UIO-S`). **Tabla de remapeo de direcciones hardcodeada** (switch/case): p.ej. `binaryOutput @1..4 → sio_address_do_05..08`, `analogOutput @1..2 → sio_address_uio_09..10`, `binaryInput @1..4 → sio_address_uio_01..04`. Transfiere campo a campo (`safetyPosition`, `characteristic` ntc10k/20k/0-10v/4-20mA, `conversion`, `pollFrequency`…), reemplaza `BOnboardIORef*`→`BPanelbusRef*` preservando el ORD, re-enlaza links externos.

---

## 90.6 — Seguridad `[CERT]` + `[CERT-a]`

**[SEC-1 CERT] El socket BTP (11000) bindea a `0.0.0.0`, no a loopback.** `BTPServer.java:176`: `this.socket = new ServerSocket(serverAddr.getPort())` — construye el `ServerSocket` **sólo con el puerto**, descartando la `InetAddress 127.0.0.1` de `serverAddr`. En Java eso bindea a la wildcard (todas las interfaces). El puerto pretende ser IPC local pero **queda expuesto a la red del controlador**.

**[SEC-2 CERT-a] BTP sin autenticación.** El envelope JSON no tiene campo de auth; el `BTPServer` sólo valida CRC-16 (integridad, no identidad). Combinado con SEC-1 y con `WriteProperty` escribiendo puntos Terminal sin verificar credenciales → **cualquier nodo con acceso de red puede leer/forzar I/O del controlador**. Sin TLS (texto plano).

**[SEC-3 CERT-a] Detección de plataforma sólo por `os.name == LINUX`** → cualquier Niagara en Linux se considera "controlador válido".

**[SEC-4 INFER] Path de config por env var sin sanitizar** (`$SNAP_DATA/...`) → posible traversal en la capa JNI si `SNAP_DATA` es manipulable.

**[SEC-5 CERT-a] Descompresión de payload BTP sin límite de ratio** → vector zip-bomb. Mensajes de error revelan arquitectura interna ("…supported by the BLE snap").

**Migradores `[CERT-a]`:** BQL armado por concatenación de string desde la UI (`select * from clPanelBus:PanelbusNetwork where displayName = '<input>'`) → BQL injection potencial. `GlobalPageModel` singleton sin sincronizar (race entre jobs concurrentes). `LicenseException` lanzada tras crear el `BPanelbusNetwork` puede dejar la station en estado parcial (el backup permite recuperación manual). El migrador elimina los authenticators EagleHawk antes de instalar el nuevo stack: si falla a mitad, el HMI queda sin autenticación.

---

## 90.7 — Conexiones

- **ATERRIZA + CORRIGE [Bloque 32]**: BTP = IPC local (11000), no IP remoto al CIPer; JNI `plantctrl` activo; host = BEATS ADV Linux/snap, no Supervisor Windows.
- **[Bloque 86]** (PanelBus/OnboardIO Centraline): es el **destino** del migrador y la fuente que BTP descubre por BQL.
- **[Bloque 88]** (BEATS PUC + cadena de campo Honeywell) y **[Bloque 91]** (`platHMI`, el panel de esta misma familia).
- **[Bloque 75]** (seguridad): suma SEC-1/2 (BTP sin auth, bind 0.0.0.0) al inventario.
