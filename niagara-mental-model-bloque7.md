# Niagara N4 — Mental Model · Bloque 7: Drivers Framework

**Sesión**: 2026-04-22
**Método**: Investigación empírica READ-ONLY (3 sub-agents Explore en paralelo)
**Fuentes**: devguide (driverFramework, basicDriver, arch-proxyExt, bacnet, lonworks), source `javax.baja.driver.*`, decompilado bacnet-rt, modbusAsync-rt, obixDriver-rt, lonworks-rt, snmp-rt, abstractMqttDriver-rt.

Bloque 6.3.2 introdujo ProxyExt como primera extension del ControlPoint. Este bloque profundiza: framework genérico, BACnet end-to-end, otros drivers (Modbus, MQTT, LON, KNX, OBIX, SNMP).

---

## Tabla de contenidos

1. [Driver framework genérico](#71-driver-framework-genérico)
2. [BACnet driver](#72-bacnet-driver)
3. [Otros drivers](#73-otros-drivers)
4. [Síntesis](#síntesis-del-bloque)

---

## 7.1 Driver framework genérico

### 7.1.1 Jerarquía canónica (4 niveles)

**`BDriverContainer`** (root, singleton por convención):
- Organizador. Aloja todas las `BDeviceNetwork` instances.
- Sin lógica de negocio propia.

**`BDeviceNetwork`** (protocolo/canal):
- Abstract. Modela un protocolo (BACnet, Modbus, OBiX, LON, RDBMS).
- Props: `status` (BStatus), `enabled`, `faultCause`, `health` (BPingHealth).
- **Cascada automática**: deshabilitar/DOWN propaga a todos los devices y points hijos.
- Contiene `BPingMonitor` para health check periódico.
- `tuningPolicies` (BTuningPolicyMap) mapea nombres → `BTuningPolicy`.
- Fault management: `configOk()`, `configFail(cause)`, `configFatal(cause)` (último no se recupera sin restart).

**`BDevice`** (dispositivo remoto):
- Desciende de BDeviceNetwork (directo o via BDeviceFolder).
- Mismo patrón de status/enabled/fault.
- **Type checking automático**: valida que sea compatible con tipo de network padre; si no, `configFatal()`.
- **Licensing**: verifica límite de devices por tipo via `BISubLicenseable.getLicenseKey()`.
- `postPing()` abstract que drivers implementan.

**`BDeviceExt`** (extensión por dominio de datos):
- Subclases: `BPointDeviceExt` (puntos/proxy), `BHistoryDeviceExt`, `BAlarmDeviceExt`, `BScheduleDeviceExt`.
- Frozen slot del device.
- BPointDeviceExt organiza árbol de ControlPoints con proxies.

**`BControlPoint` + `BProxyExt`**:
- BControlPoint = API pública (Bloque 6.3).
- BProxyExt subclase driver-específica (BBacnetProxyExt, BModbusProxyExt, etc.) enlaza point lógico ↔ device remoto.
- Props internas: `readValue` (último leído), `writeValue` (pending write en unidades device).

**Invariante**: toda ruta parent→child valida tipos. BBacnetDevice bajo BLonNetwork → `configFatal()`.

### 7.1.2 ProxyExt pipeline

**Fase 1 — Lectura desde device**:
- Worker thread invoca `readOk(newValue)` o `readFail(cause)`.
- `readOk`: copia → `readValue`, limpia fault, `getTuning().readOk()` (actualiza timestamp, re-evalúa stale), `updateStatus()`, `executePoint()`.
- `readFail`: setea `readFault`, `faultCause`, `getTuning().readFail()`, propaga FAULT bit.

**Fase 2 — `onExecute(working, Context)`** (cada engine cycle):
```
1. Si writable y working cambió:
   - convertProxyToDevice(working → writeValue en unidades device)
   - getTuning().writeDesired() → engine evalúa timing del write
2. Status computation:
   - outStatus = inStatus (priority array) | readStatus | myStatus (proxyExt)
3. Conversion y facets propagation (units, precision, etc.)
4. Retorna working con status propagado y value
```

**Fase 3 — Escritura**:
- `getTuning().writeDesired()` → engine llama `write(Context cx)` (callback abstract driver).
- Driver encola mensaje en worker thread. Retorna bool "encolado".
- Al completar: worker invoca `writeOk(value)` o `writeFail(cause)`.

**Fase 4 — Conversion bidireccional**:
- Property `conversion` (BProxyConversion).
- `convertDeviceToProxy()`, `convertProxyToDevice()`.
- Excepción en conversion → FAULT status, no bloquea pipeline.

**Fase 5 — Status propagation**:
- `point.status = point.status | device.status | network.status`.
- Device DOWN → point DOWN → tuning detiene writes.

### 7.1.3 Comm model (poll vs COV vs event-driven)

**1. Poll**: driver registra points que implementan `BIPollable` en `BPollScheduler`. Scheduler mantiene N buckets por tiempo (10s, 1m, etc.). Tic → `poll(Context)` en subscribers.

**2. Subscribe/COV**: driver se suscribe a device remoto "notifica si cambia X". Device notifica → `readOk(newValue)`. Ejemplos: BACnet COV, LON subscriptions. Reduce tráfico; latencia depende de timer del server.

**3. Event-driven genérico**: `readSubscribed(Context)` callback decide modelo basado en `tuningPolicyName`. TuningPolicy incluye `writeOnStart`, `writeOnUp`, `writeOnEnabled` para disparar writes en transiciones.

**Threading model (patrón basicDriver)**:
- **Engine thread**: onExecute, callbacks de property change, invoca write().
- **Dispatcher queue** (sync): serializa acceso al comm stack.
- **Worker thread** (async): I/O encolado, postea via readOk/writeOk.
- **Write worker** (coalescing): bufferea múltiples writes al mismo device, batch.

**NO hay scan cycle explícito** — reactivo puro, consistente con Bloque 6.1.

### 7.1.4 Discovery / Learn job

**`BILoadable`** + `BLoadable`:
- Actions `upload(BUploadParameters)` y `download(BDownloadParameters)` (ASYNC).
- `upload()` = leer del device, descubrir estructura, crear BDevice/BPoint automáticamente.
- `download()` = escribir config de la station al device.

**BLoadableNetwork / BLoadableDevice**:
- Subclases con props upload/download params.

**UI Manager** (driver-ui): wizard invoca upload(), presenta árbol discoverable, usuario selecciona, genera BOG.

**No hay learn job persistible** formal — es acción ephemeral + UI. BACnet tiene variante propia (objetos learn job).

### 7.1.5 Tuning policies

**`BTuningPolicy`** props clave:
- `minWriteTime` (BRelTime): throttle mínimo entre writes. 10 cambios/s con minWrite=5s → 1 write cada 5s (último gana).
- `maxWriteTime` (BRelTime): keep-alive. Rewrite cada N aunque no cambie.
- `writeOnStart`: escribir al steady state.
- `writeOnUp`: escribir en DOWN→UP.
- `writeOnEnabled`: escribir en DISABLED→ENABLED.
- `staleTime`: tiempo sin readOk para marcar STALE (último value con flag untrustworthy).

**`Tuning` class** (runtime state machine per ProxyExt):
- Estados: STOP, READ, WRITE_PENDING, etc.
- Callback `transition()` reevalúa: ¿running? ¿subscribed? ¿enabled? → decide read/write.
- Schedulers internos manejan min/maxWriteTime, staleTime.

**`BPollScheduler`** (utility en basicDriver):
- Manages `BIPollable` en N buckets temporales.
- `scheduler.subscribe(point, bucketName)`, tic → `poll(Context)`.

### 7.1.6 Device status propagation

**Status bits (BStatus flags)**:

| Bit | Significado | Origen |
|-----|-------------|--------|
| DISABLED | Usuario seteó enabled=false | Property |
| FAULT | Config error, read/write fail, type mismatch | Driver/framework |
| DOWN | Comunicación perdida | pingOk/pingFail |
| STALE | Lectura vieja (sin readOk en staleTime) | Tuning |
| NULL | Valor no disponible | Conversion/status |

**Cascada automática**:
1. Network status cambia → propaga a todos los BDevices (recursivo).
2. Device status cambia → propaga a points vía ProxyExt.
3. ProxyExt.updateStatus(): `out = myStatus | device.status | network.status`.

**Triggers**: configFail/Ok/Fatal, readOk/readFail/setStale, network enable/disable.

**BPingMonitor** (`network.monitor`):
- Servicio periódico; invoca `ping()` action async en cada device.
- Driver implementa `postPing()` → encola async → `pingOk()` o `pingFail(cause)`.
- `pingFail()` setea DOWN; `pingOk()` lo limpia.

### 7.1.7 Retries, timeouts, backoff

**No hay retry/backoff automático centralizado** en el framework base. Responsabilidad del driver.

**Patrón estándar (basicDriver)**:
1. **Timeout**: map `pendingRequests: commTag → Runnable`. Worker chequea age > timeout → cancel, `readFail("Timeout")`.
2. **Exponential backoff** (opcional): delay = 2^retry × base; after max retries → `readFail("Max retries exceeded")`.
3. **Stale timer**: `staleTime` violado → Tuning invoca `setStale(true)`. No reintenta automáticamente; driver decide.
4. **Config fail auto-disable**: `configFail(cause)` → FAULT; Tuning NO reescribe hasta `configOk()` limpie.

**Transient vs fatal**:
- Transient: `readFail()` clears, espera siguiente readOk.
- Fatal: `configFatal()` no se recupera sin station restart. Ejemplos: address inválido permanente, device bajo network wrong type, licencia agotada.

---

## 7.2 BACnet driver

### 7.2.1 Jerarquía BACnet

- **`BBacnetNetwork`**: contenedor raíz. Stack de comunicaciones BACnet, BLocalBacnetDevice, colas de trabajo, políticas de history. Máximo 1 por station.
- **`BBacnetDevice`**: representa device BACnet remoto. Extensiones especializadas (BBacnetPointDeviceExt, BBacnetAlarmDeviceExt, BBacnetScheduleDeviceExt, BBacnetHistoryDeviceExt, BBacnetConfigDeviceExt). Mantiene BBacnetVirtualGateway y lista de enumeraciones extensibles.
- **`BBacnetPoint`**: mapea BACnet object properties a ControlPoints via `BBacnetProxyExt` (subclases: BBacnetNumericProxyExt, BBacnetBooleanProxyExt, BBacnetEnumProxyExt, BBacnetStringProxyExt).

Dos capas de operación:
- **Cliente**: lee/controla devices remotos.
- **Servidor**: la station es device BACnet exportable (BBacnetPointDescriptor para exports).

### 7.2.2 Object types y mapping Niagara

| BACnet object | ID | Mapeo Niagara | Key props |
|---------------|-----|---------------|-----------|
| Analog Input (AI) | 0 | BNumericPoint | presentValue, statusFlags |
| Analog Output (AO) | 1 | BNumericWritable | presentValue, priorityArray, relinquishDefault |
| Analog Value (AV) | 2 | BNumericPoint/Writable | presentValue, priorityArray (si prioritizado) |
| Binary Input (BI) | 3 | BBooleanPoint | presentValue, statusFlags |
| Binary Output (BO) | 4 | BBooleanWritable | presentValue, priorityArray, relinquishDefault |
| Binary Value (BV) | 5 | BBooleanPoint/Writable | presentValue, priorityArray |
| MultiState Input (MSI) | 13 | BEnumPoint | presentValue, statusFlags |
| MultiState Output (MSO) | 14 | BEnumWritable | presentValue, priorityArray |
| MultiState Value (MSV) | 19 | BEnumPoint/Writable | presentValue, priorityArray |

**Status mapping**: BACnet statusFlags (4 bits: in-alarm, fault, overridden, out-of-service) ↔ Niagara BStatus. Máscara BACNET_SBITS_MASK = 0x2B. DOWN se limpia cuando llega presentValue válido.

### 7.2.3 Properties clave

**presentValue**: primaria. ASN.1 typed (REAL, UNSIGNED, BOOLEAN, CHARACTER_STRING). `BBacnetNumericProxyExt.fromEncodedValue()` decodifica → BStatusNumeric/Boolean/Enum.

**statusFlags**: 4 bits:
- Bit 0: in-alarm
- Bit 1: fault (evaluated por inputs)
- Bit 2: overridden (write BACnet en progreso)
- Bit 3: out-of-service

**priorityArray**: 16 niveles (1-16, NULL=sin comando). Solo en objetos prioritizables (AO, BO, AV/BV prioritizados, MSO, MSV). Escribir presentValue sin index → escribe al nivel activo del punto. Escribir NULL a un índice → libera ese nivel.

### 7.2.4 Services BACnet

- **ReadProperty**: lee una propiedad. `doForceRead()`. Args: Object_Identifier, Property_Identifier, [Property_Array_Index].
- **WriteProperty**: escribe una propiedad. `doForceWrite()`. Prioritarios: si propertyArrayIndex>0 → escribe a ese nivel; si NOT_USED=-1 y punto prioritario → escribe al nivel activo del punto Niagara.
- **ReadPropertyMultiple**: múltiples props en un APDU. Usado por scan wizards.
- **SubscribeCOV** (confirmed opcional): subscriber_process_id, monitored_object_id, issue_confirmed_notifications, lifetime.
- **SubscribeCOVProperty**: property-específico, incluye COV_Increment.
- **Who-Is / I-Am**: unconfirmed discovery. Broadcast Who-Is; devices responden I-Am. Station envía I-Am al iniciar.
- **Confirmed/Unconfirmed EventNotification**: alarmas. `BBacnetAlarmDeviceExt` procesa, mapea a BAlarmRecord.

### 7.2.5 Transports

**BACnet/IP (Annex J)**: `BBacnetIpLinkLayer`. UDP puerto 47808 (0xBAC0). Props: IP Adapter, IP Address/Port, IP Device Type (Standard / Foreign Device / BBMD).

**BACnet/MSTP**: `BBacnetMstpLinkLayer`. Serial port (COM2, COM3). Props: MSTP Address (0-127), baud rate (9600 default, 19200, 38400, 57600, 76800), Max Master, Max Info Frames (default 1, max 50), Support Extended Frames (>128 bytes).

**BACnet/PTP**: enlace serial simple, sin token-passing.

**Startup sequence**: `BacnetNetwork.started()` → BacnetStack → BacnetClientLayer → BacnetServerLayer → BacnetTransportLayer → BacnetNetworkLayer → NetworkPort → BacnetIpLinkLayer/MstpLinkLayer.

### 7.2.6 BBMD y network addressing

- **Network Number**: identificador de subnet BACnet. Devices en subnets distintas → BBMD o router BACnet.
- **Device Instance Number**: único per red (0-4194303, 22 bits). Combinado con Network Number = dirección completa.
- **`BBacnetAddress`**: codifica (network-number, MAC-address) o (IP:port). Ej: `"C0A80101:BAC0"` (192.168.1.1:47808), `"NETWORK-123:01"` (MSTP).
- **BBMD Registration**: Foreign Device envía Register-Foreign-Device a BBMD; broadcasts se tunelean via Distribute-Broadcast-To-Network. Registration Lifetime (default 15 min).

### 7.2.7 COV subscriptions

- **BBacnetCovSubscription**: recipient (address + process_id), monitored_property_reference, issue_confirmed, subscription_end_time, cov_increment.
- **COV Increment**: notificación solo si |ΔV| ≥ increment (ej. 0.5°C).
- **Lifetime**: default 1-2h. Auto-cancela al vencer. Cliente puede renovar re-subscribing.
- **`useCov` flag** en BBacnetDevice: habilita cliente a usar COV en vez de polling.

### 7.2.8 Alarming BACnet → Niagara

- **`BBacnetAlarmDeviceExt`** recibe EventNotification → mapea a BAlarmRecord.
- **Event Mapping**: event_object_identifier → objeto; event_type → BAlarmTransitionBits; event_values → presentValue, referenced_value, fault_values; event_time → timestamp.
- **Notification Class**: objeto BACnet que define tipos reportados, destinatarios, timing. `BBacnetNotificationClassDescriptor` exporta BAlarmClass Niagara como Notification Class.
- **niagaraProcessId**: prop que configura Process_Identifier para CoV/EventNotification.
- **Ack**: `doAckAlarm(BAlarmRecord)` envía ConfirmedEventNotification con acknowledge_time.

### 7.2.9 Priority array mapping

- **Proxy prioritizado**: `propertyArrayIndex` define qué nivel del array expone. Index=8 → readonly reflejo de priorityArray[8].
- **Write prioritario**: `doForceWrite()` examina:
  - Si propertyArrayIndex>0: WriteProperty con Property_Array_Index = idx. Escribe a ese nivel.
  - Si propertyArrayIndex=NOT_USED=-1 y punto prioritario: WriteProperty sin array index, mapea al nivel activo del punto Niagara.
- **Relinquish**: escribir NULL libera nivel. BACnet recalcula presentValue desde priorityArray[1..16] (nivel más alto no-NULL gana).
- **READ_PRIORITY_ARRAY="priorityArray"**: referencias indirectas via BBacnetVirtualComponent (ej `analogOutput_10/priorityArray/8`).

### 7.2.10 Discovery

- **Who-Is broadcast**: `BBacnetNetwork.submitDeviceDiscoveryJob()` con rango opcional (low, high device instance). Devices responden I-Am.
- **Device Scan Wizard**: `BBacnetDiscoverDevicesJob` emite Who-Is, recolecta I-Am en timeout (~30s), obtiene Device_Object_Identifier, objectName, maxAPDUSize, segmentationSupported, crea BBacnetDevice entries.
- **Point Discovery**: `BBacnetDiscoverPointsJob` lee Object_List, Object_Identifier, Object_Type, Present_Value vía ReadPropertyMultiple → crea BBacnetProxyExt candidatos.
- **Object Scan (Config view)**: `BBacnetConfigDeviceExt` navega objetos BACnet nativos (BBacnetObject subclasses) sin crear proxies. Útil para comisionamiento, inspección props opcionales.

---

## 7.3 Otros drivers

### 7.3.1 Modbus (Async RTU, TCP)

- **`BModbusNetwork`** (abstract) → `BModbusAsyncNetwork` (serial), `BModbusTcpNetwork` (Ethernet).
- **`BModbusDevice`** con `modbusConfig` (BModbusConfig) override opcional de settings de red.
- **Proxy points**: `BModbusProxyExt` (abstract) + subclases por tipo (Boolean/Numeric/String/Enum).

**Register types & function codes**:
- Holding Registers (rw, 16-bit): FC03 read, FC06/FC16 write.
- Input Registers (ro, 16-bit): FC04 read.
- Coils (rw, bit): FC01 read, FC05/FC15 write.
- Discrete Inputs (ro, bit): FC02 read.

**Addressing (`BFlexAddress`)**:
- `addressFormat` enum: "40001" vs "Holding 1".
- `address`: string específico.
- Byte order per multi-register: `floatByteOrder`, `longByteOrder`, `double64BitByteOrder`, `long64BitByteOrder`.

**Transport**:
- **RTU**: `serialPortConfig` (BSerialHelper: puerto, baud, bits, paridad), `interMessageDelay`, `maxRxInterCharacterDelay`, `snifferMode`.
- **TCP**: `socketOptionTimeout`.

**Polling/resiliencia**:
- `maxFailsUntilDeviceDown` (default 0 = primer fallo).
- `usePresetMultipleRegister`, `useForceMultipleCoil` para FC16/FC15.
- Actions: `forceRead()`, `forceWrite()`.

**Gotchas**: Slave address (Unit ID) obligatorio en RTU; TCP lo encapsula en MBAP header. Register offset: Modbus usa 0-65535, pero docs a menudo 1-indexed (40001 = holding 1).

### 7.3.2 MQTT

Módulo disponible: `abstractMqttDriver-rt`. Solo API de autenticación (`BAbstractMqttAuthenticator`) visible — no hay implementación de client completamente documentada en este install.

**Modelo esperado** (patrón IoT estándar):
- Pub/Sub via topics (no request/reply).
- QoS 0/1/2 en transport.
- Payload parsing: JSON, binary, delimitado.
- Network = broker connection; devices = topic subscriptions o action triggers.

**Investigar en profundidad**: si se usa MQTT en un proyecto, decompilar JAR específico del vendor (Honeywell no lo trae completo por default).

### 7.3.3 LonWorks

**Estructura**:
- **`BLonNetwork`**: top-level, managers de comisionamiento/binding/troubleshooting.
- **`BLonDevice`**: base.
  - `BLocalLonDevice`: neuron local (frozen slot en network).
  - `BDynamicDevice`: construido dinámicamente via `learnNv` (auto-discovery) o `importXLon` (archivo XML).

**Data model**:
- `BNetworkVariable` (NV): input/output, binding entre devices.
- `BNetworkConfig` (NCI): configuración de red.
- `BConfigParameter`: parámetros específicos del device.
- `BMessageTag`: solo linking, sin comportamiento en station.

**Tipos**:
- Primitivos: `BLonBoolean`, `BLonFloat`, `BLonInteger`, `BLonEnum`, `BLonString`, `BLonByteArray`.
- Complejos: `BLonSimple` (implementa `BILonNetworkSimple`) serialización custom.
- SNVT + TypeDef en LONML.

**Proxy points**: `BLonProxyExt` + subclases. `LonPointManager` view sobre points. Address via network address, subnet/node, NV index binding.

**Discovery**: `DeviceManager` (auto-discovery, address mgmt), `RouterManager` (topología), `LinkManager` (NV bindings).

**LONML (Lon Markup Language)**: XML define `XLonDevice` con deviceData, NVs, NCIs, ConfigProperties. Mapping programId (8 bytes) en `module-include.xml`:
```xml
<def name="lonworks.80 00 0c 50 3c 03 04 17" value="cl=lonHoneywell:Q7300"/>
<def name="lonworks.80 00 16 50 0a 04 04 0a" value="xml=lonSiebe/Mnlrv3.lnml"/>
```

**LonComm API**: `BLonNetwork.lonComm()`. Service types: unacked, acked, repeat, request/response. Listeners para mensajes no solicitados.

### 7.3.4 KNX

Módulos `knx/` con `cache.bog`, `datadefs.bog`. Detalles de API no están en devguide-clean. Modelo estándar:
- **Network topology**: Line, Area, Individual Address (3-1-2 format).
- **Group addressing**: DPT (Datapoint Types) — ej. DPT 1.001 = on/off, DPT 5.001 = 0-255 uint.
- **Modes**: Tunneling (P2P TCP), Routing (multicast LAN).
- **Proxy points** por tipo DPT (Boolean, Numeric, String).

**No hay detalles empíricos profundos** — decompilación del JAR KNX necesaria para investigar más.

### 7.3.5 OBIX (Open Building Information Exchange)

**Arquitectura**:
- **`BObixNetwork`**: threadPool, server, exports, tuning/history policies.
- **`BObixClient`**: conecta a server OBIX remoto. Props: `lobby` (URI raíz), `authUser`/`authPass`, `pollScheduler` (fallback si watches fail), `debugRequests`/`debugResponses`.

**Protocolo**:
- HTTP/REST con payloads XML.
- **Watches**: mecanismo preferido de push (vs polling).
- **Invocables**: actions remotas vía OBIX contracts.
- Encoders/Decoders: `ObixEncoder`, `ObixDecoder`. Interfaces: `BIObixWatchable`, `BIObixInvocable`, `BIObixEncodable`, `BIObixWritable`, `BIObixAgent`.

**Modelo de puntos**: proxy mapea objetos OBIX → points Niagara. State machine en client (property `state`). Thread pool configurable.

**Relación con Niagara**:
- Station puede ser cliente OBIX (consume server remoto) o servidor (exporta árbol vía `/exports`).
- `BObixServer`: exporta para otros clientes.
- Licensing: feature key `tridium, obixDriver`.

### 7.3.6 SNMP

- **`BSnmpNetwork`**: manager (consulta agents) y agent (responde requests).
- **`BSnmpDevice`**: agent SNMP remoto.
- **`BSnmpMipServer`**: soporte MIB definitions.
- **`BSnmpAgent`**: agent en la station para managers externos.

**Data model**:
- Proxy: `BSnmpNumericProxyExt`, `BSnmpBooleanProxyExt`, `BSnmpStringProxyExt`, `BSnmpEnumProxyExt`.
- OID notation dotted (ej. 1.3.6.1.2.1.25.3.2.1.5.1).
- MIB trees.

**Operaciones**:
- `BSnmpPollScheduler`: cadencia de lectura OIDs.
- `retryCount`, `responseTimeout` per device.
- Trap reception: `BSnmpRecipient` para notificaciones v1/v2c.
- Versions: v1, v2c. (v3 no visible en este install).

**Config**: ipAddress, port, snmpVersion, community strings (v1/v2c), enterprise/contact/description.

### 7.3.7 Tabla comparativa

| Aspecto | Modbus | MQTT | LonWorks | KNX | OBIX | SNMP |
|---------|--------|------|----------|-----|------|------|
| Modelo | Register-based | Pub/Sub | NVs (SNVT) | Group addr (DPT) | RESTful objects | OID tree |
| Transport | Serial RTU, TCP, UDP | TCP (broker) | TP/IP, wireless | TP/IP | HTTP/REST | UDP |
| Req/Reply | Sí (master-slave) | No (async) | Sí (LonTalk) | Sí | Sí (HTTP) | Sí (get/set) |
| Discovery | Scan direcciones | Manual subscription | deviceManager, learnNV | Manual | lobby tree walk | MIB walk |
| Addressing | BFlexAddress | Topic + parser | NV index + binding | Group addr + DPT | HTTP path | OID |
| Auth | No estándar | User/pass MQTT 5.0 | NeuronID | KNX auth | HTTP basic/digest | Community v1/v2 |
| Licensing | No | No explícito | `lonworks` feature | No data | `obixDriver` | `snmp` feature |
| Multicast | No | No | Sí (neuronal) | Sí (KNX MC) | No | Sí (traps) |
| Persist | Config BOG | Topic subs BOG | NV bindings + LONML | Group assignments | Client state BOG | OID snapshots |

---

## Síntesis del bloque

### Modelo mental

Todos los drivers siguen el **mismo esqueleto**: `BDeviceNetwork → BDevice → BDeviceExt → BControlPoint + BProxyExt`. La varianza vive en transport, addressing y semantics:

| Dimensión | Eje de variación |
|-----------|------------------|
| Transport | Serial (Modbus RTU, LON TP/FT), Ethernet/IP (BACnet/IP, Modbus TCP, OBIX, SNMP, KNX), wireless (LON RF, MQTT-over-WiFi) |
| Model | Register (Modbus), Object-property (BACnet), NV (LON), OID (SNMP), Group-DPT (KNX), Object tree (OBIX) |
| Semantics | Req/reply (la mayoría) vs Pub/Sub (MQTT) vs Subscription (BACnet COV, LON, OBIX watches) |

**ProxyExt** es el punto canónico de extensión. Cada driver provee su subclase que sabe cómo hacer read/write con el protocolo específico. El framework hace el resto: lifecycle, status propagation, tuning, pipeline con el ControlPoint, links.

### Conexiones con bloques anteriores

- **Bloque 6.3.2** (ProxyExt como primera extension) es el gancho concreto que este bloque expande.
- **Bloque 6.2.6** (priority arrays 16-level) se conecta 1:1 con BACnet priorityArray — mapping explícito en 7.2.9.
- **Bloque 6.1** (engine thread único) justifica el patrón basicDriver dispatcher→worker→writeWorker. I/O nunca en engine thread.
- **Bloque 5** (ORD+BOG): devices se direccionan con ORDs; device/network config se persiste en BOG con handles.

### Gotchas críticos

1. **Type invariant parent-child** — BBacnetDevice bajo BLonNetwork → `configFatal()` no recuperable sin restart.
2. **No retry/backoff automático** — cada driver implementa el suyo. Default: `maxFailsUntilDeviceDown=0` en Modbus = primer fallo marca DOWN.
3. **`configFatal()` vs `configFail()`** — el primero es permanente (restart), el segundo transient (auto-recover cuando `configOk()`).
4. **COV vs polling** — useCov reduce tráfico pero añade dependencia del timer del server (lifetime expira → hay que renovar).
5. **BACnet priorityArray mapping**: `propertyArrayIndex=-1` (NOT_USED) + punto prioritario = escribe al nivel activo del punto Niagara. Si propertyArrayIndex es specific → lock a ese nivel BACnet.
6. **Modbus byte order**: para floats y longs multi-register, hay 4+ configuraciones. Device docs vs implementation pueden divergir.
7. **Stale bit no reintenta automáticamente** — driver debe decidir. Tuning solo marca el flag.
8. **BBMD Registration lifetime** default 15 min — si Foreign Device no renueva, broadcasts se pierden silenciosamente.

### Qué habilita

Con Bloques 1-7 podés:
- Escribir un driver custom desde cero (esqueleto: Network/Device/Point/ProxyExt + tuning).
- Debuggear un BACnet device que no responde (ping → who-is → COV → priority array).
- Entender por qué un writable point escrib a un driver está siendo sobrescrito por otro source.
- Optimizar polling vs COV en función del tráfico de red.

**Próximo**: Bloque 8 — Alarm + History + Schedule (los 3 subsistemas que consumen masivamente puntos de drivers).

---

## Engram topic keys generados por este bloque

- `niagara/drivers/framework-generico` — 4 niveles (Container/Network/Device/Point), ProxyExt pipeline, comm models, tuning policies, status cascada.
- `niagara/drivers/bacnet-detalle` — object types, services, transports (IP/MSTP/PTP), BBMD, COV, alarming, priority array mapping, discovery.
- `niagara/drivers/otros-modbus-lon-obix-snmp` — Modbus (RTU/TCP, registers, FC), LON (NVs, LONML), OBIX (REST, watches), SNMP (OIDs, MIBs, traps), comparativa.

---

**Sesión cerrada**: 2026-04-22 — Bloque 7 consolidado.
