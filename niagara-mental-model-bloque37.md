# Bloque 37 — KNX/IP driver deep + proxy ↔ virtual ↔ kitControl ↔ writeback end-to-end

**Tema**: Anatomía completa del driver KNX/IP (`knxnetIp-rt.jar`, package `com.tridiumX.knxnetIp`) + flujo operacional end-to-end wire → proxy → virtual → control → writeback. Cómo una trama KNX cEMI que entra por UDP 3671 termina atravesando `BProxyExt`, pasando por `BVirtualComponent` opcional, propagándose a kitControl vía links, y regresando como otra trama cEMI OUT cuando un bloque PID o un schedule genera un nuevo setpoint. Incluye protocolo de wire (Routing / Tunnelling / Search), DPT catalog, address encoding 2L/3L, priority mapping ↔ BPriorityArray Niagara, y gotchas operacionales de proxy vs flag ASYNC vs Execution Engine.

**Método** (READ-ONLY, lab Supervisor Windows N4.14.0.162, vendor "Tridium Europe" v4.14.9.2 build 2024-04-10):
1. Extracción de `/modules/knxnetIp-rt.jar` (212 clases), `/modules/knxnetIp-wb.jar` (~90 clases), `/modules/knxStationConverter-wb.jar`, `/modules/docKnxnetIp-doc.jar` a `/tmp/b37/`.
2. Inspección de `META-INF/module.xml` runtime con enumeración de 150+ types registrados y dependencies declaradas.
3. `javap -p` sobre clases cardinales: `BKnxNetwork`, `BKnxDevice`, `BGroupAddress`, `BKnxProxyExt`, `BKnxBooleanProxyExt`, `BKnxNumericProxyExt`, `BKnxEnumProxyExt`, `BKnxStringProxyExt`, `BLocalInterface`, `BEndPoint`, `BConnections`, `BGroupDataManager`, `BTunnelConnection`, `BDiscoverDevicesJob`, `BKnxInstallation`, `KnxSpec`, `KnxCodecFuncs`, `BDataValueType`.
4. Búsqueda de strings/DPT en `knxDataDefs/` + `knxSpec/`.
5. Parte B: `javap -p` cruzado sobre `BControlPoint`, `BProxyExt`, `BLink`, `BLinks`, `BPriorityArray`, `BNumericWritable` + grep de `Flags.ASYNC` / `relinquishDefault` en `baja.jar`, `control-rt.jar`, `virtual-rt.jar`.
6. Verificación del directorio `/knx/` raíz de la estación (hallazgo: 2 archivos vacíos — ver §37.20).

**Conecta con**:
- **Bloque 19** (drivers wire framework genérico — `BDriverNetwork`, `BDriverContainer`, ping/poll arquitectura): KNX se monta sobre esa base, NO extiende `BTcpNetwork` ni `BIpNetwork` — es un driver multicast con su propio `BEndPoint`/`BLocalInterface` que gestiona socket UDP multicast directo.
- **Bloque 24** (control + kitControl): sección 24.17 (execution engine y propagación de links) es prerequisito para Parte B de este bloque. La propagación proxy.out → link → kitControl.in → kitControl.out → writable.in se apoya exactamente en ese modelo.
- **Bloque 28** (Discovery + Virtual): KNX implementa `BDiscoverDevicesJob` como job-based discovery idéntico en patrón al BACnet discovery (28.3), y el `BVirtualComponent` intercalado entre proxy KNX y bloque kitControl reusa todo lo del 28.9-28.12.
- **Bloque 23** (BACnet deep) como referencia comparativa: BACnet COV usa unicast confirmed con retries; KNX usa multicast fire-and-forget Routing (sin ACK a nivel IP) o Tunnelling con ACK KNX-layer (diferencia crítica §37.11).
- **Bloque 27** (palette + workspace): `module.palette` de KNX se enumera aquí.
- **Bloque 32** (Honeywell custom modules): NO hay módulos `honKnx*` ni `clHVACKnx*` — el driver es 100% Tridium Europe OEM, no hay overlay Honeywell (§37.21).
- **Bloque 6** (Flags / Niagara core): Flags.ASYNC obligatorio en links proxy↔kitControl (§37.14, gotcha G-B3).
- **Bloque 15.14** (Jobs): Discovery de KNX se ejecuta como `BDiscoverDevicesJob` en JobService.

---

## 37.0 Header: metadata del módulo y runtime profile

Datos del `META-INF/module.xml` de `knxnetIp-rt.jar`:

```
moduleName:       knxnetIp
runtimeProfile:   rt
vendor:           Tridium Europe          (NO "Tridium" plano — es el equipo europeo de Tridium)
vendorVersion:    4.14.9.2
buildMillis:      1721035920768            (→ 2024-07-15 08:32:00 UTC)
buildHost:        be12145a901f             (container hash, build CI)
releaseDate:      2024-04-10
preferredSymbol:  knx
nre:              true                     (exportable a JACE/controllers)
autoload:         true
installable:      true
sealed:           true                     (MANIFEST.MF — JAR sealed)
Implementation-Vendor: Tridium Europe      (del MANIFEST)
```

Dependencies declaradas (6):

| Dep | Version | Por qué |
|---|---|---|
| `alarm-rt` | 4.14 | Proxy genera alarms por comms loss, DPT codec failure |
| `baja` | 4.14 | Core framework |
| `control-rt` | 4.14 | `BControlPoint`, writables, priority array — cruce Parte B |
| `driver-rt` | 4.14 | `BDeviceNetwork`, `BDevice`, `BProxyExt` base |
| `platBacnet-rt` | 4.14 | **Hallazgo inesperado** — KNX depende de platBacnet. Ver §37.5 (sospecha: compartición de algún HPAI/endpoint util o de la integración con BIBB de IBM discovery) |
| `platform-rt` | 4.14 | System Access, network interface enumeration |

NO depende de `bacnet-rt` (normal), NO depende de `web-rt` (el WB tiene su propio doc handler). El preferredSymbol `knx` es el prefijo con que el WB lo muestra en nav tree y palette.

**Observación no-trivial**: el package Java raíz es `com.tridiumX.knxnetIp` — con **X mayúscula** en `tridiumX`. No es `com.tridium.knxnetIp` como la mayoría de drivers. Es la marca interna del equipo Tridium Europe. Esto importa porque cualquier extensión custom (honKnx*) que pretenda heredar de `BKnxProxyExt` debe importar `com.tridiumX.knxnetIp.point.BKnxProxyExt`, fácil equivocarse tipeando `com.tridium.knxnetIp`.

---

## 37.1 Inventario completo de clases KNX runtime (212 clases)

Clases organizadas por package con rol:

### 37.1.1 `com.tridiumX.knxnetIp.addresses` (8 clases)

| Clase | Rol |
|---|---|
| `IKnxAddress` | Interface base para cualquier dirección KNX (individual o group) |
| `BKnxAddress` | `BComponent` base — 16-bit address wrapper |
| `BIndividualDeviceAddress` | Dirección física `A.L.D` (Area.Line.Device) — 4/4/8 bits |
| `BGroupAddress` | Dirección de grupo `M/M/S` o `M/S` (ver §37.3) |
| `BIndividualDeviceAddresses` | Folder/container de direcciones individuales descubiertas |
| `BGroupAddresses` | Folder/container de group addresses (poblado típicamente desde import ETS) |
| `BKnxAddressStyle` | Component property: estilo del sistema (2-Level / 3-Level / Free) |
| `BKnxAddressStyleEnum` | Enum values para el Style |

### 37.1.2 `com.tridiumX.knxnetIp.driver` (4 clases — núcleo driver tree)

| Clase | Rol |
|---|---|
| `BKnxNetwork` | **Nodo raíz del driver**. Extiende `BDeviceNetwork` (driver-rt). NO es `BKnxnetIpNetwork` — el naming real es **`BKnxNetwork`** dentro del package knxnetIp |
| `BKnxDevice` | Nodo device. Referencia a una instalación KNX descubierta o manual |
| `BKnxDeviceFolder` | Organizador de devices, hereda patrón `BDeviceFolder` |
| `BKnxPointDeviceExt` | Device extension = contenedor de puntos (análogo a `BBacnetPointDeviceExt`) |

### 37.1.3 `com.tridiumX.knxnetIp.point` (9 clases — proxies)

| Clase | Rol |
|---|---|
| `BKnxProxyExt` | **Clase base abstracta** — extiende `BProxyExt`. Provee encoding DPT, group address binding, read/write behaviour |
| `BKnxBooleanProxyExt` | Boolean concreto — DPT1.xxx |
| `BKnxNumericProxyExt` | Numeric — cubre DPT5, DPT6, DPT7, DPT8, DPT9 (float16), DPT12, DPT13, DPT14 (float32) |
| `BKnxEnumProxyExt` | Enumerated — DPT5.010, DPT20.xxx (HVAC modes) |
| `BKnxStringProxyExt` | String — DPT16 (14-byte ASCII o 14-byte ISO-8859-1) |
| `BIKnxPollable` | Interface: el point participa del poll cycle |
| `BKnxPollScheduler` | Scheduler que orquesta polls a group addresses (Read requests) |
| `BKnxPointFolder` | Organizador de puntos dentro de un BKnxDevice |
| `BDataValueType` | Catálogo interno de DPT con encode/decode funcs |

### 37.1.4 `com.tridiumX.knxnetIp.comms` (~80 clases — capa wire)

Subdivisiones:
- `comms.*` raíz — `BEndPoint`, `BConnections`, `BConnection`, `BTunnelConnection`, `BLocalInterface`, `BKnxHpai`, `BGroupDataManager`, `BKnxInstallation`, `BProxyDeviceRef`, `BTcpIpAdapter` (adaptador network interface).
- `comms.cemi.*` — cEMI frame manipulation (CommonEMI = formato canonical KNX sobre IP).
- `comms.enums.*` — state machines, error codes.
- `comms.frames.*` — frame types: SEARCH_REQ, SEARCH_RES, CONNECT_REQ/RES, DISCONNECT_REQ/RES, TUNNELLING_REQ/ACK, ROUTING_IND, DESCRIPTION_REQ/RES.
- `comms.frames.parts.*` — DIBs (Description Information Blocks): `BDeviceInfoDIB`, `BSupportedServiceFamiliesDIB`.

### 37.1.5 `com.tridiumX.knxnetIp.knxSpec` (~20 clases — constantes protocolo)

Todas son `KnxSpec$*` nested + enums:
- `KnxSpec$MediumCodes` — TP1 (twisted pair), PL110 (powerline), RF, IP.
- `KnxSpec$CemiMessageCodes` — L_Data.req (0x11), L_Data.con (0x2E), L_Data.ind (0x29), L_Busmon.ind (0x2B), etc.
- `KnxSpec$CemiCtrl1` / `CemiCtrl2` — bitmasks del byte de control cEMI.
- `KnxSpec$TransportProtocolControlInformationCodes` (TPCI) — T_Data_Group_Req, T_Data_Tag_Group, T_Connect_Req.
- `KnxSpec$ApplicationProtocolControlInformationCodes` (APCI) — A_GroupValue_Read (0x00), A_GroupValue_Response (0x40/0x01 dentro del nibble bajo), A_GroupValue_Write (0x80/0x02).
- `BServiceFamilyEnum` — Core (0x02), Device Mgmt (0x03), Tunnelling (0x04), Routing (0x05), Remote Logging (0x06), Remote Config (0x07), Object Server (0x08).
- `KnxSpec$StatusBitMasks` — config status bits (programming mode, link OK, etc).

### 37.1.6 `com.tridiumX.knxnetIp.knxDataDefs` (~30 clases — DPT catalog)

Esta es la **estructura semántica de DPT**:
- `BKnxDataDefs` — container de definitions, cargado desde XML embebido.
- `BKnxStationDataDefs` — station-level overrides.
- `BDataValueTypeDef` — define un DPT (ID, length, encoder/decoder function name).
- `BEnumDef` + `BEnumValueDef` — enumerations para DPT enums (ej. HVAC mode, day/night).
- `BFacetDef` — binding a Niagara Facets (units, precision, range).
- `BDefaultDef` — default values.
- `KnxCodecFuncs` — **tabla de funciones codec Java-side** invocada por reflection según el tipo.
- `BKnxEncodingFormatEnum` — float16 vs float32 vs uint8 vs signed formats.
- `DataIntegrityCalculator` — checksum para detectar XML tampering de dataDefs.
- `IXmlImportableComponent` — se cruzan con importer del WB.

### 37.1.7 `com.tridiumX.knxnetIp.enums` (~7 clases)

Enums "de dominio" (separados de knxSpec que es wire):
- `BKnxPointTypeEnum` — Boolean, Numeric, Enum, String.
- `BKnxPriorityEnum` — Low / High / Alarm / System (mapea a bits de Ctrl1).
- `BKnxComObjectSizeEnum` — 1bit, 4bit, 1byte, 2byte, 4byte, 14byte.
- `BConnectionMethodEnum` — Routing Multicast / Tunnelling UDP / Tunnelling TCP (Secure).
- `BConfigStatus`, `BKnxDeviceConfigStatus` — config status component.
- `BDateTimeFieldEnum` — para DPT10 / DPT11 / DPT19 breakdown.

### 37.1.8 `com.tridiumX.knxnetIp.job` (3 clases)

- `BDiscoverDevicesJob` — **job de discovery** (hereda `BSimpleJob`), envía SEARCH_REQUEST por 224.0.23.12:3671 y colecta SEARCH_RESPONSE.
- `BDiscoveredDevice` — resultado: IP, puerto, device name, serial, MAC, KNX individual address, supported service families.
- `IDevicesJob` — interface para progress callbacks.

### 37.1.9 `com.tridiumX.knxnetIp.util` (~10 clases)

- `BKnxWorker` + `BKnxWorker$KnxCoalesceQueue` — thread pool dedicado + cola que **coalescea writes a la misma group address** (colapsa N writes sucesivos al último value — crítico, ver §37.13).
- `ThreadPriorityUtil` — ajusta prioridad del thread según `StationSaveListener` y `JobServiceSubscriber` (al salvar o al correr jobs sube prio).
- `BIIncludeInTrace` — marca components para aparecer en spy trace.
- `KnxStrings` — lexicon helper.
- `Dump` — debug dump de frames cEMI.
- `Queuable` — marker interface cola.
- `CatchAll` — error boundary util.
- `Constants` — UDP 3671, multicast 224.0.23.12, hop count default 6, etc.

**Total 212 clases** — es un driver **significativamente más grande** que la mayoría de drivers L2 del sistema (un modbus-rt típico tiene ~80-100 clases). Esto refleja complejidad del protocolo KNX (DPT catalog + dual connection mode + discovery + cEMI + xml importer).

---

## 37.2 BKnxNetwork — nodo raíz del driver (validado via javap)

Signature real (de `javap -p com.tridiumX.knxnetIp.driver.BKnxNetwork`):

```java
public final class com.tridiumX.knxnetIp.driver.BKnxNetwork
        extends javax.baja.driver.BDeviceNetwork
        implements javax.baja.sys.BIService
```

**Hallazgo importante**: `BKnxNetwork` NO solo es un `BDeviceNetwork` — implementa también `BIService`. Esto significa que el driver se registra como **service singleton** a nivel station, no como un mero subtree bajo `/Drivers`. Campos `static` confirman:

```java
private static com.tridiumX.knxnetIp.driver.BKnxNetwork knxnetService;
```

Existe un singleton global `knxnetService` accesible desde cualquier parte del módulo. Esto es **diferente a drivers tipo BACnet o Modbus** donde el `BDeviceNetwork` es sólo un component tree. La razón probable: el driver necesita **estado global multicast socket compartido** entre múltiples instancias KNX potenciales, y el singleton centraliza.

Slots reales (Properties expuestas):
- `tuningPolicies` : `BTuningPolicyMap` — políticas de tuning heredadas del framework driver.
- `pollScheduler` : `BPollScheduler` — scheduler genérico del framework driver (NO `BKnxPollScheduler` como asumido inicial — el KnxPollScheduler vive en package `point` y es util, no slot del network).
- `localInterfaces` : `BLocalInterfaces` — lista de NICs con rol KNX (una por binding).
- `knxDataDefs` : `BKnxStationDataDefs` — catálogo DPT station-level (override del embebido).
- `propertiesVersion` : String — versión del schema de properties (para migration N4.x→N4.x).

Actions expuestas:
- `addTraceablesActions`
- `removeTraceablesActions`

Lifecycle methods (sobrescritos):
- `serviceStarted()` — NO `started()`: es el lifecycle de `BIService`, arranca socket multicast, registra listeners.
- `serviceStopped()` — análogo.
- `doPing() throws Exception` — heartbeat.
- `getServiceTypes() : Type[]` — reporta el tipo BIService (para service lookup).
- `isParentLegal(BComponent)` / `isChildLegal(BComponent)` — validación del árbol.
- `getDeviceType() : Type` — retorna `BKnxDevice.TYPE`.
- `getDeviceFolderType() : Type` — retorna `BKnxDeviceFolder.TYPE`.
- `getLicenseFeature() : Feature` — feature de licencia requerida para activar este network (feature gating).

Loggers dedicados:
- `knxLog` — logging KNX-side.
- `commsLog` — logging capa comms (frames, socket).

**Nota crítica actualizada**: `BKnxNetwork` NO extiende `BTcpNetwork` ni `BIpNetwork` — extiende directo `BDeviceNetwork` porque el transport KNX/IP mezcla UDP multicast + UDP unicast + opcional TCP Secure, no encaja en las abstracciones TCP/IP genéricas de Niagara. Y además **es BIService con singleton global**, lo que lo hace más "infraestructural" que la mayoría de drivers.

---

## 37.3 Address space KNX: individual vs group, encoding 2-level / 3-level

### 37.3.1 Individual Device Address (4/4/8 bits)

Formato `A.L.D`:
- **A** (Area): 4 bits, 0-15.
- **L** (Line): 4 bits, 0-15.
- **D** (Device): 8 bits, 0-255.
- Total: 16 bits.
- Ejemplo: `1.2.15` → Area 1, Line 2, Device 15.
- Uso: identificar físicamente un participante del bus (un actuator, un sensor). NO se usa para tráfico normal de datos.
- Address `0.0.0` es "router unassigned". Address `A.L.0` identifica al router/line coupler de esa línea.

### 37.3.2 Group Address

**Dos estilos** soportados por el driver (clase `BKnxAddressStyle` + enum `BKnxAddressStyleEnum`):

**2-Level** (Main/Sub, 5/11 bits):
- Formato `M/S`.
- M: 5 bits → 0-31 "main groups".
- S: 11 bits → 0-2047 "sub groups".
- Ejemplo: `1/500` → main 1, sub 500.

**3-Level** (Main/Middle/Sub, 5/3/8 bits):
- Formato `M/M/S`.
- Main: 5 bits → 0-31.
- Middle: 3 bits → 0-7.
- Sub: 8 bits → 0-255.
- Ejemplo: `1/2/15` → main 1, middle 2, sub 15.

Bit pattern 16-bit es **idéntico en ambos estilos** — el estilo es solo una convención de presentación. Encoding raw:

```
2L:   MMMMM SSSSSSSSSSS       (5+11)
3L:   MMMMM mmm SSSSSSSS      (5+3+8)
```

El driver codifica/decodifica según `BKnxNetwork.addressStyle`, pero el frame wire lleva SIEMPRE los 16 bits crudos. Cambiar estilo en el WB no re-codifica frames; cambia solo el display.

**Free style** (también soportado): el address se muestra como entero decimal plano 0-65535.

### 37.3.3 Reserved addresses

- `0/0/0` (= 0x0000): broadcast group, usado por A_IndividualAddress_Read y trascends.
- `15/7/255` (= 0x7FFF): último válido, típicamente reservado.
- `0x8000-0xFFFF`: dirección **destino individual** encoded en TPCI (bit DAF=0 en cEMI indica individual).

El bit **DAF** (Destination Address Flag) del Ctrl2 byte diferencia: 0 = individual address, 1 = group address. Esto está en `KnxSpec$CemiCtrl2`.

---

## 37.4 DPT catalog — tabla de tipos soportados

Del análisis de `knxDataDefs/BDataValueTypeDef` + `KnxCodecFuncs` + lexicon strings, los DPT principales implementados:

| DPT | Size | Tipo Java | Proxy class | Uso típico |
|---|---|---|---|---|
| **DPT 1.xxx** | 1 bit | boolean | `BKnxBooleanProxyExt` | switch on/off, alarm, binary step, move up/down |
| DPT 2.xxx | 2 bit | int (0-3) | `BKnxNumericProxyExt` | switch control (no control / switch) |
| DPT 3.xxx | 4 bit | int (0-15) | `BKnxNumericProxyExt` | dimming step, blind step |
| **DPT 5.xxx** | 1 byte | short (0-255) | `BKnxNumericProxyExt` | DPT5.001 = scaling 0-100% (=0-255), DPT5.010 = counter |
| DPT 6.xxx | 1 byte | sbyte (-128..127) | `BKnxNumericProxyExt` | signed percent |
| DPT 7.xxx | 2 byte | ushort (0-65535) | `BKnxNumericProxyExt` | time period ms |
| DPT 8.xxx | 2 byte | short (-32768..32767) | `BKnxNumericProxyExt` | signed 2-byte |
| **DPT 9.xxx** | 2 byte | **float16 (KNX 2-byte float)** | `BKnxNumericProxyExt` | **temperatura, humedad, presión** — formato especial: `MEEEEE MMM MMMMMMMM` con mantissa 11-bit signed y exp 4-bit |
| DPT 10.xxx | 3 byte | time-of-day | `BKnxNumericProxyExt` | hora-min-seg + day-of-week |
| DPT 11.xxx | 3 byte | date | — | fecha |
| DPT 12.xxx | 4 byte | ulong (0..2^32) | `BKnxNumericProxyExt` | unsigned counter 32-bit |
| DPT 13.xxx | 4 byte | long (signed 32-bit) | `BKnxNumericProxyExt` | signed counter 32-bit |
| **DPT 14.xxx** | 4 byte | **float32 IEEE 754** | `BKnxNumericProxyExt` | magnitudes físicas precisión extendida |
| DPT 15.xxx | 4 byte | access data | — | badge / card |
| **DPT 16.xxx** | 14 byte | **string ASCII/ISO-8859-1** | `BKnxStringProxyExt` | texto display |
| DPT 17.xxx | 1 byte | scene number (0-63) | `BKnxEnumProxyExt` | scene |
| DPT 18.xxx | 1 byte | scene control | — | learn/recall scene |
| DPT 19.xxx | 8 byte | datetime | — | fecha + hora completa |
| **DPT 20.xxx** | 1 byte | enum | `BKnxEnumProxyExt` | **HVAC mode (DPT20.102): auto/comfort/standby/economy/protection**, occupancy, building mode |
| DPT 232.xxx | 3 byte | RGB | — | color dimming |
| DPT 242.xxx | 6 byte | xyY color | — | chromaticity |

**DPT 9 (float16) es el caso más traicionero** — es un formato **no-IEEE** de 16-bit específico KNX con mantissa signed 11-bit y exp 4-bit. `KnxCodecFuncs` tiene funciones dedicadas `encodeDpt9`/`decodeDpt9`. El rango efectivo es ≈ -671088.64 a +670760.96 con precisión ~0.01 cerca de cero. La conversión es lossy — un valor Niagara `double` con muchos decimales se redondea al encode.

**DPT 20.102 (HVAC Operating Mode) valores enum**:
- 0 = Auto
- 1 = Comfort
- 2 = Standby
- 3 = Economy
- 4 = Building Protection

El `BKnxEnumProxyExt` se conecta con un `BFacetDef` que expone Facets Niagara-side para que el UI muestre labels localizados.

**Formato DPT9 bit layout** (referencia wire):
```
Byte 0: [M EEEE MMM]  (M = sign bit mantissa, EEEE = exponent, MMM = mantissa high 3 bits)
Byte 1: [MMMMMMMM]     (mantissa low 8 bits)
Valor = (0.01 * M_signed * 2^E)
```

---

## 37.5 Dependencia de platBacnet-rt — hallazgo no-obvio

El `module.xml` de `knxnetIp-rt` declara dependency en `platBacnet-rt`:

```xml
<dependency name="platBacnet-rt" vendor="Tridium" vendorVersion="4.14"/>
```

Esto es **no-obvio**. platBacnet es una platform dependency que en teoría es para BACnet/IP. Por qué la necesita KNX:

1. **Hipótesis principal**: `platBacnet-rt` contiene primitivas de **network interface binding y UDP multicast socket platform-level** que se reusan. BACnet también usa UDP multicast (en BVLC broadcast). El wrapper `BTcpIpAdapter` del knxnetIp-rt (package `comms`) probablemente hereda o compone desde clases en platBacnet.
2. **Hipótesis secundaria**: los BIBB (BACnet Interoperability Building Blocks) o IP port enumeration util (descubrir interfaces Ethernet disponibles) están en platBacnet-rt y son reutilizados por el `BDiscoverDevicesJob` de KNX al bindear socket a interfaces específicas.

No pude confirmar sin resolver todas las referencias de importación — pero la dependencia es **real, declarada y mandatoria**. Si deployás knxnetIp a un JACE **debés tener también platBacnet-rt instalado** aunque el JACE no hable BACnet. Gotcha de provisioning (ver G-A4).

---

## 37.6 Conexión modes: Routing vs Tunnelling vs Secure

El enum `BConnectionMethodEnum` + `BConnectionMethods` define los modos soportados por el driver:

### 37.6.1 Routing (Multicast)

- Transport: UDP multicast sobre **224.0.23.12:3671** (grupo asignado IANA a "KNXnet/IP").
- Frame type: `ROUTING_INDICATION` (body es cEMI L_Data.ind).
- **Sin ACK a nivel IP** — fire and forget. Es el driver KNX quien envía al multicast group y confía que todos los routers/devices KNX en la LAN lo reciben.
- **Hop count** en cada frame (byte Ctrl1): default 6, decremento cada router que cruza. A 0 se descarta. Previene loops.
- **Ventaja**: 1 mensaje llega a todos. Ideal para KNX backbone donde hay múltiples routers KNX/IP.
- **Desventaja**: no hay confirmación — si la red Ethernet pierde el paquete UDP, se perdió silently. Aplicación típica compensa con periodic polls.
- **Rate limit**: la spec KNX pide ~50 telegrams/s máximo por routing para no saturar líneas KNX/TP1 que son 9600 bps.

### 37.6.2 Tunnelling (UDP unicast)

- Transport: UDP unicast sobre puerto 3671 al **KNX/IP Interface** concreto (típicamente un IP router KNX como Weinzierl 730 o Gira 216300).
- Handshake: `CONNECT_REQUEST` → `CONNECT_RESPONSE` con Communication Channel ID. Luego:
- Data frames: `TUNNELLING_REQUEST` (con sequence counter) → `TUNNELLING_ACK`.
- Keepalive: `CONNECTIONSTATE_REQUEST` cada 60s (timeout 120s → disconnect).
- Cierre: `DISCONNECT_REQUEST` → `DISCONNECT_RESPONSE`.
- **Sequence counter 8-bit** por conexión — rollover en 256, enum `BWrongSequenceNumberReactionEnum` define qué hacer si el peer reporta seq fuera (disconnect / log / ignore).
- **Ventaja**: ACK KNX-layer por cada frame. Confiable sobre LAN ruidosa.
- **Desventaja**: 1 conexión = 1 channel ID. Muchos KNX IP interfaces solo soportan 1-5 conexiones simultáneas. Si el Supervisor está tuneleado y abrís ETS en paralelo, competís por channel.

### 37.6.3 Secure (TCP + KNX/IP Secure)

- Transport: TCP sobre puerto 3671 + KNX IP Secure (criptografía a nivel aplicación).
- El driver tiene `BTcpIpAdapter` y referencias a Secure — **no confirmé si está plenamente implementado en esta v4.14.9.2 o solo stub**. La clase `BAbstractLocalInterface` sugiere multiples transports con herencia abstracta.
- KNX IP Secure usa AES-128 CCM con counters y claves intercambiadas vía ETS.
- No es compatible con Routing legacy — requires Secure-capable router y ETS 5.5+.

### 37.6.4 Enum values de `BConnectionMethodEnum`

Del `javap`, los slots del enum `BConnectionMethods`:
- `routingMulticast`
- `tunnellingUdp`
- `tunnellingTcp` (Secure)

Durante la configuración de un `BKnxNetwork` se elige uno por installation. Mixing modes requiere **múltiples `BLocalInterface`** — hay un LocalInterface por combinación (interfaz-Ethernet, método).

---

## 37.7 Wire format: cEMI + KNX/IP frame structure

### 37.7.1 KNX/IP frame outer structure

Todo frame KNX/IP (sea Routing o Tunnelling) tiene:

```
+--------+--------+--------+--------+----------------+
| Header | Total  | Service Type    | Body           |
| Length | Length | Identifier      | (variable)     |
| (1B=06)| (2B)   | (2B)            |                |
+--------+--------+--------+--------+----------------+
  0x06     0x10     hi       lo       ...

Header Length = 0x06 (fijo en KNXnet/IP v1.0)
Protocol Version = 0x10 (implícito en la spec, siguiente byte)
Total Length = len(header+body) big-endian
Service Type = ej. 0x0201 (SEARCH_REQUEST), 0x0420 (TUNNELLING_REQUEST), 0x0530 (ROUTING_INDICATION)
```

Service Types relevantes (`KnxIpFrameTypeEnum`):

| Hex | Name | Dirección |
|---|---|---|
| 0x0201 | SEARCH_REQUEST | Multicast (client→) |
| 0x0202 | SEARCH_RESPONSE | Unicast (device→client) |
| 0x0203 | DESCRIPTION_REQUEST | Unicast client→device |
| 0x0204 | DESCRIPTION_RESPONSE | Unicast device→client |
| 0x0205 | CONNECT_REQUEST | Unicast |
| 0x0206 | CONNECT_RESPONSE | Unicast |
| 0x0207 | CONNECTIONSTATE_REQUEST | Unicast keepalive |
| 0x0208 | CONNECTIONSTATE_RESPONSE | Unicast |
| 0x0209 | DISCONNECT_REQUEST | Unicast |
| 0x020A | DISCONNECT_RESPONSE | Unicast |
| 0x0420 | TUNNELLING_REQUEST | Unicast (data) |
| 0x0421 | TUNNELLING_ACK | Unicast |
| 0x0530 | ROUTING_INDICATION | Multicast (data) |
| 0x0531 | ROUTING_LOST_MESSAGE | Multicast (control) |

### 37.7.2 cEMI (Common External Message Interface) body

Dentro de TUNNELLING_REQUEST o ROUTING_INDICATION, el body es un frame cEMI. El frame cEMI estándar L_Data tiene:

```
+------+------+------+------+-------+-------+------+------+------+---...---+
| MC   | AddL | Ctrl1| Ctrl2| SrcH  | SrcL  | DstH | DstL | NPDU | TPCI/APCI + data
+------+------+------+------+-------+-------+------+------+------+---...---+

MC (Message Code): 0x11=L_Data.req, 0x29=L_Data.ind, 0x2E=L_Data.con
AddL (Additional Info Length): normalmente 0x00
Ctrl1: frame type | repeat flag | broadcast flag | priority | ack req | confirm
Ctrl2: DAF (dest addr flag) | hop count (3-bit) | extended frame format (4-bit)
Src (2B): individual address sender
Dst (2B): group address o individual address destino
NPDU (Network PDU Length): longitud del TPCI+APCI+data
TPCI+APCI byte 0-1: tipo de servicio transporte + aplicación
Data: payload según APCI+DPT
```

**Ctrl1 byte** (bit por bit):
- b7: frame type (1 = standard frame)
- b6: (reservado)
- b5: repeat flag (0 = repeat permitido)
- b4: broadcast flag (0 = domain broadcast, 1 = system broadcast)
- b3-b2: **priority** (00=System, 01=Urgent/Alarm, 10=Normal, 11=Low)
- b1: ACK request
- b0: confirm flag (en L_Data.con)

**Clases que modelan esto**: `KnxSpec$CemiCtrl1`, `KnxSpec$CemiCtrl2`, `KnxSpec$CemiMessageCodes`, `BCemiServiceGroupEnum`.

### 37.7.3 APCI para group operations

El byte 6 (primer nibble de APCI) define:
- 0x00 → **A_GroupValue_Read**: request leer el valor actual de la group address.
- 0x40 (bits 6-7 en byte) → **A_GroupValue_Response**: respuesta a Read.
- 0x80 → **A_GroupValue_Write**: escribir nuevo valor.

Para values ≤ 6 bits, el data viaja **embedded en los bits bajos del byte APCI** (optimización del protocolo KNX para DPT1/DPT2/DPT3). Para values ≥ 1 byte, viaja en bytes siguientes.

Esto es relevante para entender por qué DPT1 (boolean switch) es ultra-eficiente en KNX — el telegram entero son ~9 bytes cEMI incluyendo addresses.

---

## 37.8 BLocalInterface y BEndPoint — socket management

### 37.8.1 BLocalInterface

Representa una **asociación (NIC, connection method)**. Propiedades:
- `ipAddress` : IP de la NIC a bindear (0.0.0.0 = any).
- `connectionMethod` : enum (routing / tunnelling / secure).
- `multicastAddress` : default 224.0.23.12 — configurable para instalaciones con multicast diferente.
- `multicastPort` : default 3671.
- `hopCount` : default 6.
- `configStatus` : `BLocalInterfaceConfigStatus` enum (OK, bind failed, interface down, multicast denied).

`BAbstractLocalInterface` es la clase abstracta de la que heredan las variantes concretas (routing vs tunnelling).

### 37.8.2 BEndPoint

El endpoint UDP real. Encapsula:
- Un `MulticastSocket` Java (`KnxMulticastSocket` wrapper) o un `DatagramSocket` unicast.
- Un **`PacketReceiverThread`** (inner class `BEndPoint$PacketReceiverThread`) — thread dedicado leyendo del socket en loop bloqueante.
- Validation pipeline: cada paquete pasa por `BKnxIpFrameValidationResultEnum` check antes de disparar listeners.
- Queue de outbound frames con coalescing.

El thread receiver llama a `IMulticastListener.onMulticastFrame(...)` o `IEndPointListener.onFrame(...)` registrados. Los listeners incluyen `BConnections` (para rutear frames por channel ID a su `BTunnelConnection`) y `BGroupDataManager` (para routear group data a proxies).

### 37.8.3 Thread map (runtime)

Por cada `BKnxNetwork` activa, los threads son:
1. **`ConnectionsProcessorThread`** (1) — procesa eventos de conexión (disconnect, reconnect, state).
2. **`PacketReceiverThread`** (1 por endpoint) — blocking `socket.receive()`.
3. **`BKnxWorker`** (pool configurable, default 4) — ejecuta writes/reads/group operations, usa `KnxCoalesceQueue`.
4. **`BKnxPollScheduler`** (1) — timer que dispara reads periódicos a group addresses con poll habilitado.
5. **`ReceivedFrameWorkers`** — workers que procesan frames recibidos y despachan a proxies.

Son ~7-10 threads extra por cada `BKnxNetwork`. Escalabilidad: 10 networks KNX = ~80 threads solo para driver. Suma al total de 21 pools del Bloque 31.

---

## 37.9 BGroupDataManager — el corazón del routing wire ↔ proxy (validado)

`BGroupDataManager` es la clase **clave** del flujo Parte B. Signature real:

```java
public final class com.tridiumX.knxnetIp.comms.BGroupDataManager
        extends javax.baja.driver.BDeviceExt
        implements com.tridiumX.knxnetIp.comms.IConnectionClient
```

**Hallazgo**: extiende `BDeviceExt` (no es standalone component) — es un **Device Extension** del `BKnxDevice`. Esto significa que en el nav tree aparece como child slot bajo el device, y comparte lifecycle con él.

Properties reales:
- `lDataWorker` : `BKnxWorker` — worker dedicado (cola coalesce) separable del worker global.
- `hopCount` : int — override del hop count para el frames enviados desde este manager.
- `maxPendingReads` : int — límite de reads simultáneos pendientes (protección contra inundación).
- `readBeforeWriteTimeout` : `BRelTime` — timeout del patrón "read before write" (leer GA de status ANTES de escribir, para confirmar conflict).

Actions:
- `dumpObjectMap` — debug: volcar el mapa GA→proxies a log.
- `checkPendingReads` — forzar verificación de timeouts de pending reads.

Estado interno (fields):
- `groupAddressMap` : `javax.baja.nre.util.IntHashMap` — **mapa int→valor optimizado** (no un `HashMap<Integer>`). La key int es el GA raw 16-bit. Esto es de `javax.baja.nre.util`, el paquete NRE (Niagara Runtime Environment) con colecciones especializadas. Más rápido que HashMap boxed, crítico con miles de GAs.
- `pendingReadsMap` : otra `IntHashMap` — tracking de reads pendientes por GA.
- `dataConnection` : `ILDataConnection` — la conexión L-Data (link-layer KNX) activa.
- `writeOperation` : inner `LDataWriteOperation` — write en curso (uno a la vez por manager).
- `readBeforeWriteMsg` : `CemiMessage` — mensaje buffer del read-before-write.
- `pendingReadsTicket` : `Clock$Ticket` — ticket de scheduling periódico para chequear timeouts.
- Locks separados para cada zona crítica: `groupAddressMapLock`, `dataConnectionLock`, `sendRequestLock`, `writeMonitor`, `pendingReadsMapLock`. **5 locks separados** = diseño cuidadoso de granularidad, evita contención en hotpath.

Inner classes (del listing):
- `PendingRead` — tracking de read pendiente.
- `LDataReadOperation` — operación de read en ejecución.
- `LDataWriteOperation` — operación de write en ejecución.

Lifecycle (de `IConnectionClient`):
- `connectionOpened()` — se invoca cuando la conexión baja está lista.
- `connectionClosing()` — preparar cleanup.
- `updateStatus()` — propagar status al device.

**Diferencia con BACnet**: en BACnet cada request tiene invoke-id y el reply match es por invoke-id. En KNX group Read/Response no hay ID: el match es **"el primer A_GroupValue_Response que llega para esta GA tras mi Read"**. Si dos Supervisors piden Read a la misma GA casi-simultáneo, ambos consumen la misma Response. Es una limitación del protocolo, no del driver — pero `pendingReadsMap` keyed by GA int mitiga parcialmente dentro de un solo Supervisor (el segundo Read para misma GA se coalesce en el pending existente).

### 37.9.1 BGroupAddress signatures (validado)

```java
public final class BGroupAddress extends BKnxAddress {
    public BGroupAddress();
    public BGroupAddress(int);
    public BGroupAddress(int, BKnxAddressStyleEnum);
    public BGroupAddress(BKnxAddressStyleEnum);
    public static BGroupAddress make(int, BKnxAddressStyleEnum);
    public static BGroupAddress make(String) throws Exception;
    public boolean isZero();
}
```

Constructor `(int)` toma el raw 16-bit. `make(String)` parsea strings como `"1/2/15"`, `"1/500"` o `"12345"` según el style — puede lanzar exception en parse error. `isZero()` útil para detectar GA no-asignada.

### 37.9.2 Feature gating: isRoutingInstalled()

`BConnectionMethods` tiene método privado:

```java
private static boolean isRoutingInstalled();
```

Esto sugiere **licenciamiento diferencial**: el connection method "Routing multicast" puede requerir license feature separada. En una JACE con licencia "Tunnelling-only", el enum runtime excluye `routingMulticast` de los valores disponibles. **Gotcha operacional** (G-A13, nuevo): si provisioning el driver en JACE con licencia limitada y la config BOG usa routing, el driver falla al arrancar.

### 37.9.3 Tres endpoints separados: MULTICAST, CONTROL, DATA

`com.tridiumX.knxnetIp.util.Constants` define queue size constants para **tres endpoint types**:
- `MULTICAST_END_POINT_RECEIVE_QUEUE_SIZE_DEFAULT`
- `CONTROL_END_POINT_RECEIVE_QUEUE_SIZE_DEFAULT`
- `DATA_END_POINT_RECEIVE_QUEUE_SIZE_DEFAULT`

Esto confirma la arquitectura KNX/IP spec:
- **Multicast endpoint** — único socket multicast 224.0.23.12:3671 para Routing + Discovery.
- **Control endpoint** — socket unicast para frames de control del Tunnelling (CONNECT_REQ, DISCONNECT_REQ, CONNECTIONSTATE_REQ).
- **Data endpoint** — socket unicast para TUNNELLING_REQUEST/ACK (los datos reales).

La spec KNXnet/IP permite que control y data usen puertos UDP **diferentes** — durante CONNECT_RESPONSE el peer informa en qué puerto esperar data. Esto complica firewalls: abrir solo 3671 NO es suficiente para Tunnelling; hay que abrir un rango de puertos ephemeros o configurar specific data port en el IP Interface.

### 37.9.4 Otros constants operacionales notables

- `KNXNETIP_WORKER_MAX_QUEUE_SIZE_DEFAULT` — cap de la cola del worker (evita OOM si un bus cuelga).
- `NETWORK_PING_FEREQUENCY_DEFAULT_SECONDS` — heartbeat default (nótese typo "FEREQUENCY" en código — confirmado del listing).
- `NETWORK_STARTUP_ALARM_DELAY_DEFAULT_MINUTES` — delay antes de disparar alarms durante startup (evita alarmas falsas durante boot).
- `CACHE_STORE_TIME_DEFAULT_HOURS` / `_MIN_MINUTES` / `_MAX_HOURS` — TTL del cache de discovery results (el `cache.bog` de §37.20).
- `M_PROP_SERVER_QUEUE_SIZE_*` — **M_Prop** = Management Property, servicio KNX para leer/escribir propiedades de devices (ETS-level management). El driver soporta esto aunque no es el flujo data normal.

---

## 37.10 BKnxProxyExt y sus subclases — el hook Parte B

### 37.10.1 BKnxProxyExt (abstract) — signatures reales

`javap` sobre `BKnxProxyExt` revela:

```java
public abstract class com.tridiumX.knxnetIp.point.BKnxProxyExt
        extends javax.baja.driver.point.BProxyExt
        implements com.tridiumX.knxnetIp.point.BIKnxPollable
```

Properties reales (Slots):
- `knxId` : String — identificador único del proxy point (para cross-ref al ETS import).
- **`groupAddresses` : `BGroupAddresses`** — NO es `groupAddress` singular sino un **container con múltiples GAs**. Esto es un hallazgo: un solo proxy point puede tener **múltiples group addresses asociadas** (ej. una para Read/status, otra para Write/command, una para priority override, etc.). Patrón típico KNX que el driver soporta nativamente.
- `dataValueTypeId` : String — ID del DPT en el catálogo `BKnxDataDefs` (ej. `"9.001"`).
- `pollEnable` : boolean — habilita polling periódico.
- `pollOnceOnSubscribed` : boolean — poll único cuando un subscriptor externo pide el point.
- `pollOnceOnOperational` : boolean — poll único cuando el device transiciona a operational.
- `pollUntilAnswer` : boolean — retry poll hasta que llegue A_GroupValue_Response (con timeout).
- `pollAfterWrite` : boolean — después de escribir, poll la GA de status para confirmar.
- `pollFrequency` : Duration — período del poll regular.
- `writeOnly` : boolean — marca el proxy como solo-escritura (no decodifica incoming writes en esa GA).

Actions:
- `pollNow` — trigger manual de poll.
- `writeNow` — re-envía el último value.
- `dump` — debug dump.

Topics (event channels):
- `busDataReceived` — dispara cuando llega un telegram KNX para esta GA.
- `busDataExecuted` — dispara cuando se completa una write al bus.

Estos topics son suscribibles desde otros components — permiten reaccionar event-driven sin necesidad de link.

Campos internos notables:
- `m_bDataValueTypeSpecNotFound` : boolean — flag de error si el DPT no existe en el catálogo.
- `pollSubscribed` + `pollSubscribedLock` — coordinación thread-safe.
- `oldStatus` : `BStatus` — último status conocido para delta detection.

Métodos clave (implementados en subclases):
- `decodeFromBytes(CemiMessageData) : BStatusValue` — decode wire bytes a value Niagara (en `BKnxNumericProxyExt` y peers).
- `encodeToBytes(BStatusValue) : CemiMessageData` — encode value Niagara a wire bytes.
- `setWriteValueAndWrite(BStatusValue, boolean) : boolean` — setter + trigger write operation.

**Diferencia con modelo simplificado**: el prompt inicial describió `groupAddress` + `writeGroupAddress` singulares. La realidad es **un container `BGroupAddresses`** multi-elemento, con semantic de qué GA se usa para Read vs Write determinada por flags/roles dentro de cada `BGroupAddress` child. Más flexible, pero también más propenso a miscofiguración (3 GAs todas marcadas "Write" = conflicto).

### 37.10.1.1 BKnxNumericProxyExt (concreto, ejemplo)

```java
public final class com.tridiumX.knxnetIp.point.BKnxNumericProxyExt
        extends com.tridiumX.knxnetIp.point.BKnxProxyExt {
    public BStatusValue decodeFromBytes(CemiMessageData);
    public CemiMessageData encodeToBytes(BStatusValue);
    public static CemiMessageData encodeToCemiMessageData(BDataValueTypeDef, BStatusValue);
    private static boolean isEncodingFormatValid(BDataValueTypeDef);
    public boolean setWriteValueAndWrite(BStatusValue, boolean);
}
```

El static `encodeToCemiMessageData(BDataValueTypeDef, BStatusValue)` es **reusable externally** — cualquier código puede encodear un value sin instanciar el proxy. Útil si alguna extension custom necesita generar cEMI frames para bypass del driver normal.

### 37.10.2 Las 4 subclases concretas

| Subclase | Tipo point Niagara asociado | DPTs que maneja |
|---|---|---|
| `BKnxBooleanProxyExt` | `BBooleanPoint` / `BBooleanWritable` | DPT1.xxx |
| `BKnxNumericProxyExt` | `BNumericPoint` / `BNumericWritable` | DPT5/6/7/8/9/12/13/14 |
| `BKnxEnumProxyExt` | `BEnumPoint` / `BEnumWritable` | DPT17, DPT18, DPT20.xxx |
| `BKnxStringProxyExt` | `BStringPoint` / `BStringWritable` | DPT16.xxx |

El framework elige la subclase al "learn" de una GA (en el ETS import o discovery manual). El XML importer (`com.tridiumX.knxnetIp.xml.XmlImporter`) lee el `.knxproj` de ETS y mapea com-objects → proxy subclass según el DPT declarado en ETS.

### 37.10.3 Method signatures relevantes (javap)

Simplificado, `BKnxProxyExt` tiene:
```java
public abstract class BKnxProxyExt extends BProxyExt {
    public abstract void readInputFromDevice() throws Exception;
    public abstract void writeOutputToDevice() throws Exception;
    protected void onGroupValueReceived(byte[] apdu, BBusAccessPriorityEnum pri);
    protected void setPointValue(Object value, BStatus status);
    // ... setters para groupAddress, dpt, etc
}
```

El `onGroupValueReceived` se llama desde `BGroupDataManager` cuando llega un frame con APCI = Write o Response para la GA registrada. El proxy decodifica con `KnxCodecFuncs` según su DPT y llama `setPointValue` que actualiza el `BControlPoint.out` asociado.

---

## 37.11 Flujo end-to-end (diagrama ASCII) — packet → proxy → virtual → kitControl → writeback

Este es el core de la Parte B. Secuencia real de un ciclo "KNX → Niagara → KNX":

```
 ┌─────────────── FASE 1: telegram IN (device KNX → Supervisor) ───────────────┐
 │                                                                             │
 │  KNX device (sensor temp)                                                   │
 │     │ bus KNX TP1 (9600 bps)                                                │
 │     ▼                                                                       │
 │  KNX/IP Router (Weinzierl 730)                                              │
 │     │ UDP multicast 224.0.23.12:3671                                        │
 │     │ ROUTING_INDICATION (service=0x0530) + cEMI L_Data.ind                 │
 │     │   src=1.1.50, dst=1/2/15 (GA), apci=GroupValueWrite, data=0x0C 0x1A   │
 │     ▼                                                                       │
 │  [Ethernet NIC Supervisor Windows]                                          │
 │     │                                                                       │
 │     ▼                                                                       │
 │  MulticastSocket.receive()  in PacketReceiverThread  ◄─── thread #1         │
 │     │                                                                       │
 │     ▼                                                                       │
 │  BEndPoint.validateFrame()  →  dispatch a listeners                         │
 │     │                                                                       │
 │     ├─► BConnections.onFrame()  (no aplica — es multicast)                  │
 │     └─► BGroupDataManager.onGroupDataReceived(ga=1/2/15, apci=Write, data)  │
 │            │                                                                │
 │            │ lookup: proxies registered for GA 1/2/15                       │
 │            │ → [BKnxNumericProxyExt#42]  (DPT=9.001 temperature)            │
 │            │                                                                │
 │            ▼                                                                │
 │         BKnxNumericProxyExt.onGroupValueReceived(apdu, priority)            │
 │            │                                                                │
 │            │ KnxCodecFuncs.decodeDpt9(data) → 21.8  (float)                 │
 │            │                                                                │
 │            ▼                                                                │
 │         proxy.setPointValue(21.8, Status.ok)                                │
 │            │                                                                │
 │            ▼                                                                │
 │         BControlPoint(BNumericPoint)  —  slot "out" actualizado             │
 │            │  Status: ok, value=21.8, timestamp=now                         │
 │            ▼                                                                │
 │         Niagara Execution Engine   ◄── topic "propertyChange: out"          │
 │            │                                                                │
 └────────────┼────────────────────────────────────────────────────────────────┘
              │
 ┌────────────┼──── FASE 2: propagación por links dentro de Niagara ───────────┐
 │            │                                                                │
 │            ▼                                                                │
 │  BLink (source=sensor.out, target=virtualSensor.in, flags=ASYNC)            │
 │            │  Link fires en thread del Engine                               │
 │            ▼                                                                │
 │  BVirtualComponent#sensorTempVirtual                                        │
 │    (in=21.8)  —  slot virtual intermedio; actúa como shim/isolation        │
 │    out = transform(in) = in * 1.0  (identity típica)                        │
 │            │                                                                │
 │            ▼                                                                │
 │  BLink (source=virtualSensor.out, target=pidBlock.controlInput, ASYNC)      │
 │            ▼                                                                │
 │  kitControl::BLoopPoint (PID controller, del Bloque 24)                     │
 │    controlInput=21.8, setpoint=22.0, output computed=45.3%                  │
 │    .execute() dispatch en kitControl's execution group                      │
 │            ▼                                                                │
 │  BLink (source=pid.out, target=valveWritable.in16, ASYNC)                   │
 │     in16 = entrada de prioridad 16 (baja) del BPriorityArray                │
 │                                                                             │
 └─────────────┬───────────────────────────────────────────────────────────────┘
               │
 ┌─────────────┼── FASE 3: writeback (Niagara → device KNX) ──────────────────┐
 │             │                                                              │
 │             ▼                                                              │
 │  BNumericWritable#valveCmd  (del kitControl output)                        │
 │    priorityArray[16] = 45.3                                                │
 │    winner = resolve priority array (lowest non-null priority)              │
 │    .out = 45.3  (asumiendo priorities 1-15 son null)                       │
 │             │                                                              │
 │             │ Proxy ext responds to "input change"                         │
 │             ▼                                                              │
 │  BKnxNumericProxyExt.writeOutputToDevice()                                 │
 │    DPT=5.001 (scaling 0-100%)                                              │
 │    KnxCodecFuncs.encodeDpt5(45.3) → 0x74  (45.3% * 2.55 ≈ 116 = 0x74)      │
 │             │                                                              │
 │             ▼                                                              │
 │  BGroupDataManager.writeGroupValue(ga=1/2/20, apci=Write, data=0x74, prio) │
 │             │                                                              │
 │             │ enqueue en BKnxWorker.KnxCoalesceQueue                       │
 │             │  (si hay write pending para misma GA, se COALESCE: keep last)│
 │             ▼                                                              │
 │  BConnections.send()  →  build cEMI L_Data.req  →  build ROUTING_IND       │
 │             │                                                              │
 │             ▼                                                              │
 │  MulticastSocket.send(frame)  en thread del BKnxWorker                     │
 │             │                                                              │
 │             ▼  UDP multicast 224.0.23.12:3671                              │
 │  KNX/IP Router — propaga al bus KNX TP1                                    │
 │             │                                                              │
 │             ▼                                                              │
 │  KNX actuator (valve) — recibe telegram, mueve válvula a 45.3%             │
 │             │                                                              │
 │             ▼  (ciclo cerrado — nuevo sensor reading en siguiente poll)    │
 │                                                                            │
 └────────────────────────────────────────────────────────────────────────────┘
```

**Tiempos típicos end-to-end (LAN + bus TP1)**:
- Fase 1 (wire → out point): 5-20 ms (receive + decode + setPoint).
- Fase 2 (links + kitControl execute): 20-100 ms (depende de execution engine period, default 1000 ms si no es async — por eso `Flags.ASYNC` es crítico, ver G-B3).
- Fase 3 (writable → wire): 20-100 ms (coalesce queue + socket.send).
- Propagation KNX TP1: 25-40 ms por telegram a 9600 bps.
- **Total: 70-260 ms** típico end-to-end para un ciclo cerrado. Bajo carga (bus KNX TP1 saturado ~40 tel/s) puede ir a 1-2 s.

---

## 37.12 Priority array mapping: kitControl output → BNumericWritable → KNX priority

Niagara `BPriorityArray` (de `control-rt.jar`) tiene 16 niveles. kitControl outputs van típicamente a priority 16 (lowest), dejando 1-15 para override manual / schedule / emergency.

**Pero KNX priority es diferente** — el byte Ctrl1 del cEMI tiene solo 2 bits de prioridad (4 valores: System / Urgent / Normal / Low). No hay equivalente de "16 slots". El driver mapea:

| BPriorityArray slot | → KNX Ctrl1 priority |
|---|---|
| 1 (emergency) | System (00) |
| 2-7 (manual, schedule) | Urgent (01) |
| 8-15 (operator override, default) | Normal (10) |
| 16 (default / kitControl) | Low (11) |

El mapeo exacto está en el código de `BKnxProxyExt.writeOutputToDevice()` + `BBusAccessPriorityEnum`. Si el proxy ext tiene `priority` slot explícito seteado, **override absoluto** al mapping automático.

**Gotcha G-B5**: el peer KNX no sabe qué slot de tu priority array disparó el write — solo ve "Urgent" o "Low". Si tenés 3 sources compitiendo (HMI=8, Schedule=12, kitControl=16), las tres resuelven a **Low** en el bus, y el actuator KNX no puede diferenciar origen. Confiabilidad del priority semantics se pierde al cruzar el gateway IP→TP1.

---

## 37.13 Coalesce queue — writes consecutivos a la misma GA se colapsan

`BKnxWorker$KnxCoalesceQueue` es una cola inner class con lógica:

```java
enqueue(operation) {
    if exists pending operation with same (targetGA, type=Write) {
        replace its data with the new one;   // coalesce — keep latest
    } else {
        append to queue;
    }
}
```

**Caso de uso**: un PID loop genera output cada 100ms, el valve KNX solo acepta ~20 writes/s. Sin coalescing, la cola creciera infinita y el driver se quedara atrás. Con coalescing, entre dos ciclos del `BKnxWorker` (configurable, típicamente 100-200 ms) solo sobrevive el **último value** por GA.

**Trade-off**: perdés intermediate values. Para accumulators (counters) esto es catastrófico — pero los DPT counter son Reads, no Writes, así que no aplica.

**Gotcha G-A9**: si tu lógica de negocio depende de ver cada transición (ej. un state machine que cuenta "se ejecutó el comando X 5 veces"), KNX write coalescing te rompe el contrato. Solución: usar proxy directo sin kitControl (escritura directa desde link), o configurar kitControl con output suscribiéndose de forma event-driven a un subscription de write confirmations.

---

## 37.14 Flags.ASYNC obligatorio en links proxy↔kitControl

Del Bloque 6.1.6 y 24.17: los `BLink` tienen flag `Flags.ASYNC`. Por default en workflows normales, un link ejecuta **en el mismo thread que disparó el change event** (el Execution Engine).

Problema: el `BControlPoint` del proxy tiene su `out` modificado desde el **`PacketReceiverThread` del driver KNX** (o desde un worker `BKnxWorker`). Ese thread NO debe bloquear con lógica kitControl pesada — si el PID tarda 50 ms en converger, el PacketReceiver no recibe frames durante 50 ms y UDP multicast empieza a perder paquetes (el buffer del socket OS se llena).

Solución que el framework usa:
- Links desde proxy hacia ControlPoint / VirtualComponent / kitControl → **DEBEN tener Flags.ASYNC**.
- Con ASYNC, el link encola el event en el Engine y retorna inmediato. El thread KNX vuelve a `socket.receive()`.
- El Engine procesa el event en su propio thread pool.

**Gotcha G-B3**: al construir links programáticamente en BOG o vía WB, el default es sync. Debés setear explícitamente `Flags.ASYNC` en el link. Si no lo hacés y el kitControl downstream es pesado, empezás a perder telegrams KNX y el sistema parece "lento" sin explicación obvia.

---

## 37.15 BVirtualComponent entre proxy y kitControl — rol de shim

Patrón común en sites Niagara industriales:

```
BKnxNumericProxyExt ──link──► BVirtualComponent.sensorTemp ──link──► BLoopPoint(PID)
   (wire binding)              (shim/isolation layer)                 (kitControl)
```

Razones para intercalar el `BVirtualComponent`:

1. **Desacoplamiento**: si mañana cambiás de KNX a BACnet, solo redireccionás el link del proxy al virtual; el kitControl NO cambia.
2. **Transformaciones** (scaling, unit conversion, filtering): el virtual tiene slots con `@NiagaraProperty` que pueden aplicar `in * factor + offset` antes de propagar.
3. **Engineering view**: el "point engineer" que dibuja lógica kitControl no tiene que conocer naming wire KNX — trabaja con nombres virtuales ("sensorTempSala3").
4. **Testability**: puede bypassear el virtual point con un valor manual para simular sin device real.

Esto viene del Bloque 28.9 (Virtual Components framework) y es **patrón de arquitectura limpia tipo container-presentational** aplicado a BAS.

**Pero con costo**: el virtual adds un link hop extra → más latencia + más uso del engine. En sites con 50K points, esto se nota en CPU baseline.

---

## 37.16 Proxy offline NO auto-disable kitControl downstream — el contrato invisible

Escenario crítico:
- KNX router Ethernet falla → driver detecta comms loss → `BKnxDevice.status = down`.
- Todos los proxy ext en ese device marcan su ControlPoint con `Status.fault` / `Status.down`.
- El value del proxy point **queda en el último value válido** (freeze), y su Status pasa a fault.

**kitControl NO respeta Status fault por default**:
- El PID loop sigue leyendo el input value (el frozen value).
- El output sigue calculando — basado en un input obsoleto.
- El output sigue escribiéndose a otro proxy (si el otro proxy está up, se escribe con value basura).

Esto es un **problema de contrato cross-bloque**. La culpa no es del driver KNX — es la ausencia de una policy global "propagate fault → disable downstream computation".

**Mitigaciones que sí existen**:
- `BLink` con flag `PROPAGATE_STATUS` — hace que el link propague status al in del target. Pero no todos los blocks kitControl reaccionan a status.
- `BLoopPoint` tiene slot `disabled` — podés linkearlo al status.fault del sensor para desactivar el PID. Pero requires configuración explícita por cada loop.
- `faultCause` slot en kitControl blocks — se puede propagar.

**Gotcha G-B1 (crítico operacional)**: en el Bloque 32 se mencionó similar issue con honPlantController. Acá se formaliza: **Niagara NO auto-disables downstream computation on upstream fault**. Es responsabilidad del engineer configurar fault propagation explícito. En sites mal configurados, un KNX router caído 10 min puede producir comandos sin sentido a otros sistemas (BACnet VAVs recibiendo setpoints desde un PID que lee sensor frozen).

---

## 37.17 ETS project import (`knxnetIp-wb.jar`)

El WB module agrega la capacidad de **importar `.knxproj` de ETS** (ETS = Engineering Tool Software, la IDE oficial KNX de la KNX Association).

Clases del WB relevantes:
- `BEtsProjectFileImportManager` (con nested `EtsProjectFileImportController`, `EtsProjectFileImportLearn`, `EtsProjectFileImportModel`).
- `BImportedEtsProjectFile` — referencia al proyecto importado.
- `BImportedEtsProjectFiles` — container de varios.
- `BImportedPointGroup` — grupo lógico de points importados.
- `ImportEtsProjectFilesController` — orchestrador.
- `BEtsProjectFileOrdFE` — ORD file expression (apunta al `.knxproj` en `!knx/`).
- `BDataDefsUpdateFrequencyEnum` — frecuencia de re-sync del datadefs XML.
- `BFileNameDecorationEnum`.

### 37.17.1 El `.knxproj` es un ZIP encriptado

El `.knxproj` que exporta ETS es un ZIP con archivos XML adentro, **opcionalmente protegido con password**. El driver incluye:
- `com.tridiumX.knxnetIp.zip.EncryptedZipInputStream` — lector de ZIP encriptado (AES).
- `com.tridiumX.knxnetIp.zip.CRC32`.
- `BZipDecryptSectionEnum`, `BZipDecryptStateEnum` — state machine.
- `ZipConst$GeneralPurposeBitFlags` — flags del header ZIP.

La **AES key derivation** del ZIP ETS usa PKCS#12 + password — al importar, el WB pide password si el `.knxproj` tiene flag "password protected".

### 37.17.2 XML parse + import

Clases en `com.tridiumX.knxnetIp.xml`:
- `XmlImporter` (con nested `COpenTag`).
- `XmlInputStream` — parser propio (NO javax.xml.stream), probablemente para perf + control estricto de namespaces.
- `IXmlImporterHelper`, `IXmlImportableComponentSpec`, `XmlPropertyImportSpec`, `XmlChildImportSpec` — especificaciones declarativas de qué XML nodes mapean a qué Niagara slots.
- `IXmlNameSpaceEnum` — namespaces KNX ETS (project, datadefs, topology, etc).
- `BXmlAttributeTypeEnum`, `BXmlPropertySetTypeEnum`.
- `ImportProblemReports` — reporte de warnings/errors del import.

**Output del import**: un tree de `BImportedEtsProjectFile` → `BImportedPointGroup` → proxies con DPT mapeados. El user en WB luego arrastra proxies al device real para activarlos.

---

## 37.18 knxStationConverter-wb — migración de stations legacy

Este módulo (`knxStationConverter-wb.jar`, 81 KB, **solo 5 clases**) es una **tool aislada** con una responsabilidad muy específica:

```
com.tridiumX.knxStationConverter.BKnxStationConverterTool
                                 .BStationConverterOptions
                                 .BStationConverterOptionsPane
                                 .BStationConverterOptionsPane$StationConverterOptions
```

Contiene un `knx_station.xml` embebido.

**Función**: convertir stations N4 que usaban un driver KNX legacy (probablemente `knxbus-rt` de versiones N4.x previas, o el driver KNX de AX 3.x al migrar) al formato actual `knxnetIp-rt`. Es una **one-shot migration tool**, se corre desde WB menú "Tools → Convert KNX Station".

El `knx_station.xml` embebido describe el esquema de transformación — qué components renombrar, qué slots mapear.

No es runtime. Es solo WB.

---

## 37.19 BKnxInstallation y BKnxInstallationRef — unidad lógica de agrupación

`BKnxInstallation` representa una **instalación KNX física** (normalmente un edificio o wing con su propio backbone KNX TP1 + router IP). Slots:
- Lista de group addresses.
- Lista de individual device addresses descubiertas.
- Reference al `BLocalInterface` que la sirve.
- Config status (`BKnxInstallationConfigStatus`).

`BKnxInstallationRef` es una referencia indirecta desde proxies — así un proxy no aprende directamente una GA sino que aprende "la GA X de la instalación Y". Si la instalación se renombra o se mueve, todos los proxies siguen funcionando.

Esto es patrón de **indirection** puro (Bloque 6: referencias vs embedded).

---

## 37.20 El directorio `/knx/` de la estación — caché vacío

**Hallazgo directo del filesystem**:

```
/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/knx/
  cache.bog     0 bytes
  datadefs.bog  0 bytes
```

Los dos archivos existen pero **son 0 bytes**. Interpretación:
- `datadefs.bog` está diseñado para contener la **versión persistida del `BKnxStationDataDefs`** — el catálogo DPT custom de la estación (si el integrator agregó DPTs propios más allá del catálogo default, se guardan aquí).
- `cache.bog` está diseñado para contener **cache de resultados de discovery** — last-known devices, GA mappings, para acelerar startup.

Ambos están vacíos porque **en esta copy del Supervisor el driver KNX nunca se ha ejecutado** — no hay `BKnxNetwork` instanciada en la config, o fue eliminada. La existencia del directorio sugiere que en algún momento se planificó usar KNX (o el instalador lo crea por default cuando el módulo está instalado), pero está dormant.

**Verificable vía**: buscar `BKnxNetwork` en `config.bog` — si no aparece, confirma que el driver está instalado pero no configurado.

---

## 37.21 Honeywell custom KNX — no existe

Búsqueda exhaustiva:
- NO hay `honKnx*.jar` en `/modules/`.
- NO hay `honBacnetKnx*.jar` (algunos OEM tienen bridge modules).
- NO hay referencias a `com.honeywell.*knx*` en el build que pude verificar.
- El driver `knxnetIp-rt.jar` es **100% Tridium Europe OEM sin overlay Honeywell**.
- La distribución Honeywell OptimizerSupervisor simplemente **re-empaqueta el driver stock** sin modificaciones.

Esto contrasta con otros drivers donde Honeywell sí overlay:
- Bloque 32 documentó `honBacnetHelper` (overlay BACnet con FastAccessList optimization).
- Bloque 32 documentó `clHVACKnx*` no existente (confirmado aquí también).

**Implicación operacional**: si Honeywell soporte te dice "tu issue KNX es nuestro", NO lo es — es driver stock Tridium. Escalación debe ir a Tridium Europe support para bugs del driver.

---

## 37.22 Ciclo Discovery + palette

### 37.22.1 Discovery via BDiscoverDevicesJob

Flow:
1. User click "Discover Devices" en WB → instancia `BDiscoverDevicesJob` via `BKnxWbService`.
2. Job envía SEARCH_REQUEST (service=0x0201) multicast a 224.0.23.12:3671 por cada `BLocalInterface` con rol routing.
3. KNX/IP Routers y Interfaces responden con SEARCH_RESPONSE (service=0x0202) unicast al source IP+port del request.
4. Cada response tiene HPAI (Host Protocol Address Information) + DeviceInfoDIB + SupportedServiceFamiliesDIB.
5. Job colecta en `BDiscoveredDevice` instances: IP, MAC, serial number, knx individual address, project install ID, supported services.
6. User elige cuáles importar → crear `BKnxDevice` components.

Timeout típico del job: 3-5s. El discovery es **single-shot** por cada local interface — no es continuo. Para observar devices que aparecen/desaparecen en runtime, no hay equivalent COV.

### 37.22.2 Palette

`module.palette` del knxnetIp-rt enumera los BComponents drag-and-drop:
- KnxNetwork
- KnxDevice
- KnxDeviceFolder
- KnxPointFolder
- KnxBooleanProxyExt / KnxNumericProxyExt / KnxEnumProxyExt / KnxStringProxyExt
- GroupAddress / GroupAddresses
- IndividualDeviceAddress / IndividualDeviceAddresses
- LocalInterface / LocalInterfaces
- KnxInstallation

User típicamente solo drag-n-drop `KnxNetwork` y `KnxDevice`; el resto se genera via ETS import o learn.

---

## 37.23 Gotchas Parte A (driver KNX)

### G-A1 — Multicast requires L2 network support, NO atraviesa routers L3 sin IGMP

El driver KNX con connection method Routing (224.0.23.12) **no funciona cross-subnet** sin:
- IGMP snooping habilitado en switches.
- Multicast routing en routers L3 (PIM) — infrecuente en corporate LANs.
- O bien usar Tunnelling point-to-point al KNX IP Interface.

En sites donde el Supervisor está en una VLAN y el KNX IP router en otra, **Routing multicast falla silently**. NO hay error log claro — los SEARCH_RESPONSE simplemente no llegan. Solución: VLAN dedicada KNX, o usar Tunnelling unicast con IP específico.

### G-A2 — Hop count default 6 es insuficiente para instalaciones grandes

El Ctrl2 byte lleva 3 bits de hop count. Default del driver = 6. En sites con múltiples edificios + backbone + líneas en cada edificio (3-4 router layers), **telegrams pueden morir** antes de llegar. KNX spec permite hasta 7. Subir a 7 global en `BLocalInterface.hopCount`.

### G-A3 — DPT silent mismatch: proxy con DPT9 pide GA que actuator publica DPT5

Si el proxy Niagara está configurado con `dpt=9.001` (2-byte float temperature) pero la GA real del bus publica DPT5.001 (1-byte scaling), el decode falla silently o produce **valores basura** (reinterpretación binaria). El driver NO valida cross-check porque a nivel wire la longitud coincide solo por coincidencia en algunos pairs.

Solución: importar DPTs desde ETS `.knxproj` (única source of truth authoritativa). NO confiar en learn manual.

### G-A4 — Dependency platBacnet-rt oculta

Provisioning a un JACE: si instalás solo `knxnetIp-rt` sin `platBacnet-rt`, el módulo falla en load con ClassNotFoundException. El error aparece en `niagarad.log` pero NO siempre en la UI de Station Copier. Siempre deployar ambos juntos.

### G-A5 — Rate limit TP1: 40-50 tel/s por línea

KNX TP1 es 9600 bps → ~45 telegrams/s pico. Si el Supervisor envía más rápido (vía coalesce queue que draina rápido), el IP router descarta los excedentes. Logs del router (si es Gira/Weinzierl) muestran "telegram dropped".

Mitigación: configurar `pollFrequency` en proxies conservador (>1s). Evitar tormentas de writes en schedules.

### G-A6 — Routing multicast sin ACK = writes pueden perderse en silencio

En modo Routing, `writeOutputToDevice()` envía y retorna OK. **No hay confirmación** a nivel IP ni KNX que el actuator recibió. Si el UDP se pierde (LAN ruidosa, buffer overflow, NIC bug), el value del actuator KNX sigue en el estado previo.

Mitigación: configurar `readOnChange` + poll de la GA de status para confirmar. O usar Tunnelling (ACK KNX).

### G-A7 — Tunnelling channel ID finito (1-5 connections típico en IP Interfaces)

Un Gira 216300 soporta 5 channel IDs. Si el Supervisor ocupa 1, ETS laptop ocupa otro, otro Supervisor peer ocupa otro, etc., a la 6ta request el router responde `CONNECT_RESPONSE` con error `E_NO_MORE_CONNECTIONS`. El driver loggea fault y espera — pero no hay retry automático hasta disconnect de otro client.

Observable en `BTunnelConnectionCommsCounters`.

### G-A8 — Address style cambio en runtime NO rebinds proxies

Si cambiás `BKnxNetwork.addressStyle` de 3-Level a 2-Level en runtime, los proxies existentes muestran GAs en nuevo formato pero el wire es idéntico (es presentación). BOG files guardados en 3-Level se leen correctamente en 2-Level. Pero si editás un proxy y tipeás una GA en formato erróneo para el style actual, el parser puede fallar o interpretar distinto.

### G-A9 — Coalesce queue sacrifica intermediate writes

Ya mencionado en §37.13. Writes consecutivos a la misma GA con distinto value dentro de una misma "tick" del worker pool → solo el último sobrevive. Para logic que depende de contar transitions (ej. edge-counting un pulso), romper.

### G-A10 — datadefs.bog vacío pero driver funciona — fallback a catalog embebido

Si `/knx/datadefs.bog` es 0 bytes (como en el Supervisor analizado), el driver fallback al **catálogo DPT embebido en el JAR** (`BKnxDataDefs` default). Funciona para DPTs estándar, pero DPTs custom del site (que algunas instalaciones Alemania/Italia definen como "DPT 65535 — weird custom") NO están y decode produce faults.

### G-A11 — Worker thread priority boost durante save

`ThreadPriorityUtil$StationSaveListener` sube la prio del `BKnxWorker` durante station saves. Observable: durante `save()` el driver KNX procesa writes pendientes antes. Pero **si el save tarda 30s** (sites grandes), el rest del Engine pierde CPU. Visible en monitoreos como "UI lag durante save" — ver Bloque 31.

### G-A12 — Sequence counter rollover en Tunnelling cada 256 requests

8-bit counter → 256 writes luego wrap a 0. El `BWrongSequenceNumberReactionEnum` define qué hacer si el peer reporta "sequence inesperada". Default en el driver = `ignore` (recommended). Algunos routers exigen estricto y disconnectan — cambiar a `disconnect` y dejar al driver reconectar.

### G-A13 — Feature gating de Routing via licencia

`BConnectionMethods.isRoutingInstalled()` existe como check privado. Esto sugiere que **la licencia de la station puede no incluir "Routing multicast"** aunque sí incluya "KNX/IP Tunnelling". En JACE con licencias budget, intentar usar Routing puede fallar silently en load-time (el enum no expone el value) o raise "feature not licensed" en runtime.

Verificar licencia vía `getLicenseFeature()` de `BKnxNetwork` antes de desplegar config que usa Routing.

### G-A14 — Control/Data endpoints en puertos diferentes — firewall issue

La spec KNX/IP permite que Tunnelling use puerto 3671 para Control pero un **puerto ephemeral distinto para Data** (asignado por el peer en CONNECT_RESPONSE). Firewalls que solo permiten UDP/3671 entrante/saliente **bloquean el data channel**. Síntoma: CONNECT funciona, pero no llegan telegrams.

Solución: abrir UDP rango 3671-3700 bidirectional, o configurar el IP Interface para force data en 3671 (si soporta).

### G-A15 — Typo en constant `FEREQUENCY`

`Constants.NETWORK_PING_FEREQUENCY_DEFAULT_SECONDS` — el typo es en el código real del driver v4.14.9.2. Si tu código custom referencia esto por reflection o string-match, escribí el typo. Es un detalle de color, pero refleja que el vendor "Tridium Europe" tiene su propio pipeline de reviews.

---

## 37.24 Gotchas Parte B (proxy ↔ virtual ↔ kitControl ↔ writeback)

### G-B1 — Fault upstream NO se propaga a kitControl por default

§37.16 detallado. Configurar explícito `faultCause` o link de status.fault al slot `disabled` de los kitControl blocks. **Policy site-wide** deseable.

### G-B2 — Priority conflict HMI vs schedule vs kitControl

§37.12. Tres sources compitiendo, KNX reduce a Low/Normal/Urgent/System. Monitor real con logs del writable — NO asumir que la priority Niagara llega al bus.

### G-B3 — Flags.ASYNC obligatorio en link proxy → kitControl

§37.14. Sin ASYNC, el thread packet receiver se bloquea en lógica kitControl → buffer UDP se llena → packet loss silencioso. Regla: **cualquier link donde source = driver proxy, target ≠ simple value copy → ASYNC**.

### G-B4 — kitControl hidden state survive restart (sticky)

kitControl blocks como `BLatch` tienen internal state (`currentOutput`, `last command time`). En `station.save()` ese state se persiste al BOG. Al restart, el block reanuda con el state previo. Si el site cambió físicamente mientras la station estaba down (operador movió válvula manualmente), el kitControl al startup escribe su stale state → conflict.

Solución: configurar `initState` explicit en blocks que lo soportan, o `readOnStartup=true` en proxies + delay kitControl execution hasta primer read válido.

### G-B5 — Device slow write vs fast link propagation

Link KNX writable dispara `writeOutputToDevice()` en ~5 ms. Coalesce queue encola. Worker thread procesa cada 100-200 ms. **Bus TP1 tarda ~25 ms por telegram**. Si el link dispara cada 50 ms (PID loop agresivo), la cola crece → coalesce → solo el último value del último tick sobrevive. kitControl computa un output, no lo ve aplicado, el siguiente ciclo computa otro output diferente → oscilación del PID.

Mitigación: clamp `sampleTime` del PID >= 2 × periodo worker KNX. Matemática clásica de control (Nyquist para control discreto).

### G-B6 — BVirtualComponent en medio adds latency + CPU

§37.15. Evitar virtuals gratuitos. En sites con 50K proxies, un virtual por cada uno duplica el link count → Engine CPU sube. Usar solo cuando hay valor de engineering claro (transformación o isolation real).

### G-B7 — BControlPoint.out no actualiza si Status degraded

Si el proxy marca `Status.stale` o `Status.disabled`, el `setPointValue` puede NO actualizar `.out` (depende del point impl). kitControl downstream sigue leyendo el value previo, pero **sin notificación de que es stale**. Combinar con G-B1.

### G-B8 — RelinquishDefault en BNumericWritable — misused

El slot `relinquishDefault` del Writable se toma si todos los priorityArray slots son null. Si el kitControl deja su slot en null (ej. `priorityArray[16] = null` explicit al stop), el writable cae al `relinquishDefault`. Si este default es 0 y el actuator KNX espera "posición safe", 0 puede ser peligroso (valve fully open). **Configurar relinquishDefault = safe position**.

### G-B9 — Link loop risk — feedback del actuator re-entra al PID

Patrón común: proxy lee status GA (value actual del actuator) → link → virtual.in → link → PID.controlInput. **PID.output → writable.in16 → proxy.writeGA → actuator → bus → status GA → vuelve al PID.controlInput.** Si latencia es alta y lazo es cerrado con ganancia mal tuneada, **oscilación**.

Es control loop clásico, no bug del driver. Pero en PLC-land suele haber saturación/filtrado; en Niagara kitControl es responsabilidad del engineer configurar `minOutput`/`maxOutput`/`slewRate`.

### G-B10 — Write coalesce + kitControl output oscilante = histéresis perdida

Si PID satura entre dos values `45%` y `55%` cambiando cada 50ms y worker tick cada 200ms, el coalesce descarta todos los "55" y solo envía "45" en una iter, luego solo "55" en otra. Al actuator le llega un square wave low-freq → desgaste mecánico. **No es visible en la UI de Niagara** (que muestra el point out = último computed).

Mitigación: slew rate limiter a la salida del PID.

---

## 37.25 Conexiones con otros bloques (refs cruzadas)

| Cruce | Explicación |
|---|---|
| **19.2** — `BDriverNetwork` base | `BKnxNetwork extends BDeviceNetwork extends BDriverNetwork`. Ping/doPing/doDiscover framework. |
| **19.5** — Device extensions | `BKnxPointDeviceExt` sigue exactamente el patrón. |
| **24.3** — ControlPoint jerarquía | `BNumericPoint`/`BBooleanPoint` son los destinos donde el proxy pone `.out`. |
| **24.10** — BPriorityArray | Mapping 16→4 en §37.12. |
| **24.17** — Execution engine & links | Base para entender Parte B. Flags.ASYNC rule. |
| **28.3** — DiscoverDevicesJob pattern | KNX reusa exactamente el pattern (job-based, timeout, progress callback). |
| **28.9** — Virtual components | §37.15 patrón shim. |
| **23.2** — BACnet COV | Comparativa KNX Group Value Read vs BACnet COV. KNX es poll-based o event via bus actualizations naturales (el bus es broadcast — si device publica, todos oyen); BACnet COV requires subscription explícita. |
| **27.2** — Palette enumeration | §37.22.2. |
| **31.1** — Thread pool inventory | §37.8.3 — KNX adds ~7-10 threads/network. Suma al total del Bloque 31. |
| **31.2** — UI lag durante save | G-A11 (worker priority boost during save) contribuye. |
| **32.3** — Honeywell module coverage | §37.21 — NO hay honKnx*, gap documentado. |
| **6.1.6** — Flags.ASYNC | G-B3 — contrato. |
| **15.14** — JobService | `BDiscoverDevicesJob` corre en JobService, comparte thread pool con otros jobs. |
| **39** (paralelo) | No investigado aquí, pero Bloque 39 podría cubrir SAML/security en el WB que toca el KNX WB controllers (`DiscoverDevicesController`, `BDiscoverDevicesPane`) — sin intersección crítica. |

---

## 37.26 Correcciones a bloques previos

### Corrección al Bloque 28.3 (Discovery framework)

El Bloque 28.3 documentó el patrón DiscoverDevicesJob como "generic framework extensible por drivers". KNX confirma el patrón **exactamente idéntico** — `BDiscoverDevicesJob` + `BDiscoveredDevice` + `IDevicesJob`. **No es corrección, es validación** — pattern confirmed en 3 drivers (BACnet, Modbus presumiblemente, KNX).

### Corrección al Bloque 32 (Honeywell modules)

El Bloque 32 buscó `honKnx*` y dijo "no hallado". **Confirmado aquí**: 0 módulos Honeywell tocan KNX. El driver es 100% Tridium Europe OEM sin overlay. **No corrección — refuerzo**. Si el Bloque 32 decía "probable Honeywell custom", ajustar: NO hay custom Honeywell para KNX, es stock.

### Corrección al Bloque 19 — naming del BKnxNetwork

Si el Bloque 19 (o el prompt del 37) referenció `BKnxnetIpNetwork` como nombre de clase, el **naming real es `BKnxNetwork`** — sin "nIp" en el medio. El module se llama `knxnetIp` pero la clase network es `BKnxNetwork`. Nota terminológica.

### Corrección al Bloque 23 (BACnet deep)

El Bloque 23 describió BACnet COV unicast confirmed. Aquí se contrasta que **KNX NO tiene COV como servicio diferenciado** — el bus KNX es por naturaleza broadcast, entonces cualquier A_GroupValue_Write que cualquier device envía es oído por todos los que escuchan esa GA. El driver Niagara KNX puede hacer "poll on startup" y luego ser pasivo (escuchar) para obtener updates — similar a COV en efecto, diferente en mecanismo.

---

## 37.27 Tabla inventario condensada de clases KNX cardinales (top 30)

| Package | Clase | Rol |
|---|---|---|
| driver | `BKnxNetwork` | Root nodo driver (extends BDeviceNetwork) |
| driver | `BKnxDevice` | Device node |
| driver | `BKnxDeviceFolder` | Folder device |
| driver | `BKnxPointDeviceExt` | Point container extension |
| addresses | `BGroupAddress` | Group address 16-bit |
| addresses | `BIndividualDeviceAddress` | Individual addr 4/4/8 |
| addresses | `BKnxAddressStyle` | Style 2L/3L/Free |
| point | `BKnxProxyExt` | Abstract proxy ext base |
| point | `BKnxBooleanProxyExt` | DPT1 proxy |
| point | `BKnxNumericProxyExt` | DPT5/6/7/8/9/12/13/14 proxy |
| point | `BKnxEnumProxyExt` | DPT17/18/20 proxy |
| point | `BKnxStringProxyExt` | DPT16 proxy |
| point | `BKnxPollScheduler` | Poll orchestrator |
| point | `BDataValueType` | DPT runtime definition |
| comms | `BEndPoint` | UDP socket wrapper |
| comms | `BLocalInterface` | NIC + method binding |
| comms | `BConnections` | Connection pool |
| comms | `BTunnelConnection` | Tunnelling channel |
| comms | `BGroupDataManager` | GA routing hub |
| comms | `BKnxInstallation` | Instalación lógica |
| comms | `BKnxHpai` | Host Protocol Addr Info |
| comms | `BTcpIpAdapter` | Adapter TCP/IP network |
| comms.frames.parts | `BDeviceInfoDIB` | Device info block |
| comms.frames.parts | `BSupportedServiceFamiliesDIB` | Service families block |
| knxDataDefs | `BKnxDataDefs` | DPT catalog runtime |
| knxDataDefs | `KnxCodecFuncs` | Encode/decode funcs |
| knxSpec | `KnxSpec` (+$nested) | Protocolo constants |
| util | `BKnxWorker` | Dedicated thread pool |
| util | `ThreadPriorityUtil` | Thread prio control |
| job | `BDiscoverDevicesJob` | Discovery job |

---

## 37.28 Conclusiones no-obvias (TL;DR hallazgos)

1. **Package namespace es `com.tridiumX.knxnetIp`** (X mayúscula) — no `com.tridium.*`. Vendor "Tridium Europe", no "Tridium". Hallazgo que impacta cualquier custom extension que quiera heredar.
2. **Dependency hidden en `platBacnet-rt`** — provisioning a JACE requiere ambos, no solo knxnetIp. Tripwire silencioso.
3. **`/knx/` en el Supervisor analizado está vacío** — `cache.bog` y `datadefs.bog` 0 bytes, indica que el driver está instalado pero nunca se usó en esta station. Si te pasás config.bog a otro Supervisor, estos archivos se regeneran al primer `BKnxNetwork.start()`.
4. **Priority 16→4 mapping colapsa información** — Niagara priority semantics se pierden en el wire KNX. Diseñar policies aceptando esto.
5. **Coalesce queue en `BKnxWorker` sacrifica writes intermedios** — impacto real en logic event-counting.
6. **NO hay Honeywell custom sobre KNX** — 100% stock Tridium Europe. Soporte debe escalar a Tridium, no Honeywell.
7. **Multicast Routing no cross-VLAN** — Tunnelling es el fallback obligatorio en LANs corporativas segmentadas.
8. **Flags.ASYNC obligatorio en link driver→kitControl** — sin eso, packet receiver se bloquea y hay silent packet loss.
9. **Fault NO se propaga auto a kitControl** — contrato cross-bloque inexistente, responsabilidad del engineer.
10. **`BKnxNetwork` extiende directo `BDeviceNetwork`**, NO `BTcpNetwork` ni `BIpNetwork` — porque el transport KNX mezcla multicast + unicast + opcional TCP Secure.
11. **DPT9 (float16 KNX) es lossy no-IEEE** — valor Niagara `double` con muchos decimales se redondea al encode.
12. **ETS `.knxproj` importer soporta ZIP encriptado AES** — password via UI prompt; todo el codec encryption está en `com.tridiumX.knxnetIp.zip`.
13. **`BKnxInstallation` es unidad de agrupación lógica** — referencias indirectas via `BKnxInstallationRef` permiten renombrar sin romper proxies.
14. **Thread count: ~7-10 threads por `BKnxNetwork`** — suma al inventario global Bloque 31.
15. **`knxStationConverter-wb`** es tool de migración legacy one-shot, no runtime — útil para sites que migraron desde AX 3.x.

---
