# Bloque 78 — Drivers de bus de campo Centraline: C-Bus (Clipsal) + EnOcean (wireless), deofuscados

> Investigación empírica de dos drivers de campo **Centraline** (prefijo `cl`) tras deofuscar su ZKM: **`clCBus`** (bus Clipsal C-Bus — iluminación/automatización determinista) y **`clEnoceanNetwork`** (EnOcean — wireless energy-harvesting).
> Strings descifradas (`deobfuscator-patched.jar` rev2 + CFR); nombres de clases internas siguen `a`/`b`/`c` (ZKM name-mangling, irreversible). Las clases public Network/Device/ProxyExt, slots `@NiagaraProperty`, enums y lexicon keys SÍ son legibles.
>
> Fuentes:
> - `organized/clCBus/clCBus-rt/vineflower/com/honeywell/cbus/`
> - `organized/clEnoceanNetwork/clEnoceanNetwork-rt/vineflower/cl/enocean/network/`
>
> Método: sub-agente + **verificación directa** de declaraciones de clase, constantes y strings (`[CERT]` = verbatim verificado esta sesión; `[CERT-a]` = cita del sub-agente con archivo:línea no re-verificada; `[INFER]` = deducción).
>
> Continúa la **Capa 22** (drivers OEM deofuscados) iniciada en [Bloque 77](niagara-mental-model-bloque77.md). ATERRIZA [Bloque 7 — Drivers Framework](niagara-mental-model-bloque7.md), capa **N-driver** (`com.tridium.ndriver`).

---

## 78.1 — Hallazgo común: ambos sobre el N-driver framework `[CERT]`

A diferencia del Spyder ([Bloque 77]: `BBacnetDevice`/`BDynamicDevice` directos), C-Bus y EnOcean se construyen sobre el **N-driver framework** de Tridium (`BNNetwork`/`BNDevice`/`BProxyExt`) — el andamiaje genérico para escribir drivers custom. Declaraciones reales verificadas:

```java
// C-Bus (com/honeywell/cbus/)
public class BCBusNetwork  extends BNNetwork  implements BINDiscoveryHost {        // BCBusNetwork.java:117
public class BCBusDevice   extends BNDevice   implements ICommandEventListener,
                                                         IPointRefreshListener, BINPollable {  // device/BCBusDevice.java:221
public class BCBusProxyExt extends BProxyExt  implements BINPollable,
                                                         ICommandEventListener, BIAlarmSource { // point/BCBusProxyExt.java:123

// EnOcean (cl/enocean/network/)
public class BCentralineEnoceanNetwork extends BNNetwork implements ICommListener { // BCentralineEnoceanNetwork.java:86
public class BEnoceanGateway           extends BNDevice  implements BINPollable {   // BEnoceanGateway.java:127
```

> Implicación: la topología en el station tree es la canónica **Network → Device → Points (ProxyExt)** del [Bloque 7]. Quien entiende un driver N-driver entiende ambos. El protocolo concreto vive en los paquetes `comm/`, `message/`, `protocol/` (C-Bus) y `commands/`, `bindings/` (EnOcean).

---

## 78.2 — C-Bus: transporte, addressing SUSI y tipos de punto `[CERT]`

**Transporte TCP** `[CERT]`: el C-Bus se habla por **TCP al puerto 2499** vía una pasarela BNA (Centraline). Default verificado en `BCBusNetwork.java`:
```java
new BIpAddress("", 2499)
```
Config: `BCBusTcpCommConfig extends BTcpCommConfig`; factory `CBusLinkMessage(2000)` (buffer 2000 bytes) `[CERT-a]` `comm/BCBusTcpCommConfig.java:31`. El stack usa mensajes **XCNAP** (`XCnapMessage`) sobre `TcpLinkLayer` `[CERT-a]`.

**Addressing SUSI** `[CERT-a]` (`message/SusiAddress.java`): direcciones jerárquicas `bus.device.datapointType.datapointIndex` (separadores `:` y `.`). El enum `BSusiDestTypeEnum` define los destinos: HOST(1), BUS(2), DEVICE(3), ROOM(4), ANALOG_INPUT(5), ANALOG_OUTPUT(6), DIGITAL_INPUT(7), DIGITAL_OUTPUT(8), ANALOG/DIGITAL_PARAMETER(9/10), Z_REGISTER_*(11/12), TOTALIZER(13), PSEUDO_*(16-18), FLEXIBLE_POINT(19), REMOTE_*(20/21), PROGRAM_POINT(22).

**Punto C-Bus** (`BCBusProxyExt` slots, `point/BCBusProxyExt.java:121+`) `[CERT-a]`: `pollFrequency`, `pointName`, `pointType` (`BCBusPointType`), `pointIndex`, `pointValue` (double), `engineeringUnit` (BDynamicEnum), `pointFlags`, `config`, `duplicatePoint`. Tipos (`BCBusPointType`): UNIVERSAL(0), ANALOG/DIGITAL_INPUT, DIGITAL/ANALOG_OUTPUT, COUNTER_INPUT, ANALOG/DIGITAL_VALUE, *_REMOTE, X/YREGISTER, FLEXIBLE_POINT, COUNTER_VALUE, WILD_CARD.

---

## 78.3 — C-Bus: discovery y diagnóstico `[CERT]`

**Discovery jobs** `[CERT-a]` (`extends BNDiscoveryJob`):
- `BCBusDiscoverDevicesJob` — espera init de red, timeout **120 s** (`MAX_BUFFER_AFTER_INIT=120000`).
- `BCBusDiscoverPointsJob` — exige `communicationStatus==idle`; lanza `CmdDpUserAddrByNameRead` con wildcard `"*"` para leer **todos** los puntos.
- Acciones de red: `submitDiscoveryJob`, `sendTimeSynchronization`, `readAllAlarms`, `enableAllDuplicatePoints` `[CERT-a]`.

**Lexicon keys de error** (verbatim, `BCBusNetwork.java`) `[CERT]` — lo que ve el integrador:

| Síntoma | Key | Causa probable |
|---------|-----|----------------|
| Sin comunicación con pasarela | `CBusNetwork.PingFail` | "Unable To establish communication with BNA!" — IP/puerto/cableado |
| Licencia | `CBusNetwork.NotLicensed` | "License expired or not valid!" |
| Canales deshabilitados | `CBusNetwork.ChannelDisabled` | ambos canales de la BNA off |
| Config inválida | `CBusNetwork.InvalidConfig` | configuración BNA inválida |
| Red caída | `CBusNetwork.NetworkDown` | red C-Bus down |
| Device offline | `CBusDevice.DeviceOffline` | "Device reported offline!" |
| Punto inexistente / mismatch | `CBusDevice.PointIDMismatch`, `.PointNameFault`, `.PointTypeIndexNameMissmatch` `[CERT-a]` | el punto descubierto no coincide |
| Sin puntos | `CBusDiscoverPointsJob.NoPointsFound` `[CERT-a]` | discovery vacío |

`communicationStatus` (`BCBusDeviceCommunicationStatus`) expone el progreso de carga: IDLE → LOADING_POINT_DESCRIPTORS → ...ENG_UNITS → ...ALARM_TEXTS → ...DATA_POINTS → ...SCHEDULES `[CERT-a]`.

---

## 78.4 — EnOcean: gateway USB, telegramas y teach-in `[CERT]`

**Gateway USB (USB300)** `[CERT]`: `BEnoceanGateway extends BNDevice` representa el dongle EnOcean. Límite verificado: **32 devices por red** (`BEnoceanGateway.java:142`):
```java
public static int MAX_NUMBER_DEVICES = 32;
// throw "Can't add: Maximum number of devices reached!"  (:281)
```
Slots del gateway `[CERT-a]`: `deviceAddress` (long 32-bit, radix 16), `antennaMode`, `filterTable`, `points`, `retryMax` (1-3), `baseAddressOffsets`, `rssiThreshold` (0-100 dB), `firmwareVersion`.

**Transporte ESP3** `[INFER/CERT-a]`: serial **EnOcean Serial Protocol 3** (`BClEnoceanSerialCommConfig`); la red implementa `ICommListener`.

**Telegramas** `[CERT]`: el 4BS (4-Byte Status, sensores analógicos) está modelado en `BClEnoceanTelegram4Bs extends BComponent` con `MAX_MSC_DATA_LENGTH = 7` (máx 7 bytes de datos) `[CERT]`, slots `eoSenderAddress` (EURID 32-bit), `orgByte`/`functionByte`/`typeByte`/`statusByte`. También RPS (switches) y VLD (variable length) `[CERT-a]`.

**EEP (EnOcean Equipment Profile)** `[CERT-a]` (`bindings/BClEep.java`): codifica el perfil del dispositivo en 3 bytes — `BClEep(Long)` decodifica `orgByte=(l>>16)&0xFF`, `functionByte=(l>>8)&0xFF`, `typeByte=l&0xFF`. El EEP determina cómo interpretar el payload (temperatura, switch, dimmer…).

**Teach-in / discovery** `[CERT-a]`: `BClEnoceanLearnGatewaysJob extends BSimpleJob` hace `doPingBroadcast()` durante `discoverPeriod` (10 s) y acumula en `learnedDevices`. Acciones del gateway: `ping`, `setMode`, `read/write/deleteFilterTableEntry`, `readMailBox`, `discoverRemoteDevices`/`cancel`, `buzzerOn`/`Off` (aviso audible al encontrar device). A nivel red: `pingBroadcast()`, `submitDiscoveryJob(BClEnoceanDiscoveryPreferences)`.

---

## 78.5 — EnOcean: status flags y diagnóstico `[CERT-a]`

`BStatusFlags` (`utils/BStatusFlags.java`) — bitmask mapeado a `BStatus` vía `flagsToStatus()`:

| Flag | Valor | Significado |
|------|------:|-------------|
| STATUS_OK | 0 | normal |
| LEARN_MODE_ACTIVE | 1 | device en aprendizaje |
| ADDRESSING_INCOMPLETE | 2 | dirección EnOcean no configurada → device disabled |
| CHANNEL_NUMBER_INVALID | 4 | canal inválido para el EEP |
| TIMEOUT | 8 | sin telegramas en la ventana esperada |
| INVALID_COMMAND | 16 | comando no reconocido por gateway |
| LAST_TELEGRAM_INVALID | 32 | telegrama corrupto/malformado |
| AUTO_DETECT_RUNTIMES_ACTIVE | 64 | gateway midiendo tiempos |

Config crítica `[CERT-a]`: `retryMax=3`, `pingLoopDelay=1000ms`, `discoverPeriod=10000ms`, `highlightPeriod=20000ms`, `rssiThreshold` configurable. La `filterTable` por source-ID descarta telegramas irrelevantes (reduce carga). `ClEnoceanNetworkUtils.isValidDeviceId()` deshabilita devices con EURID inválido.

---

## 78.6 — Síntesis comparativa

| | **C-Bus (Clipsal)** | **EnOcean** |
|---|---|---|
| Framework | N-driver (`BNNetwork`/`BNDevice`/`BProxyExt`) | idéntico |
| Transporte | **TCP puerto 2499** vía pasarela BNA | **serial ESP3** vía dongle USB (USB300) |
| Addressing | SUSI `bus.device.type.index` (22 dest types) | EURID 32-bit + EEP (3 bytes) + canal |
| Naturaleza | determinista cableado (iluminación/clima) | wireless energy-harvesting (sensores/switches) |
| Discovery | jobs automáticos (`Discover Devices/Points`, wildcard `*`, timeout 120 s) | **teach-in** (`Learn`, ping-broadcast 10 s, buzzer) |
| Límite | sin límite duro observado | **32 devices/red** |
| Telegramas/comandos | XCNAP/SUSI sobre TCP | RPS / 4BS (≤7 bytes) / VLD |

**Para el integrador:**
1. **Ambos son N-driver**: la topología, ProxyExt y el modelo de polling son los del [Bloque 7]. El "protocolo" es solo la capa `comm/`.
2. **C-Bus offline** → `CBusNetwork.PingFail` apunta a la **pasarela BNA** (IP/puerto 2499/canales), no al device. `communicationStatus` muestra en qué fase de carga está.
3. **EnOcean** es teach-in puro: sin learn, el device queda `ADDRESSING_INCOMPLETE` y disabled. Vigilar `rssiThreshold` (señal) y `LAST_TELEGRAM_INVALID`. Máx 32 por gateway.
4. **EEP es la clave semántica** EnOcean: sin el perfil correcto, el payload del telegrama no se interpreta.

**Pendiente conocido**: nombres de clases internas ofuscados (`a`/`b`/`c`) — irreversible (ZKM). Las clases public, slots y enums dan el contrato completo.
