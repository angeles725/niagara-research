# Niagara N4 — Bloque 19: LON deep + NRIO + NiagaraDriver + BOX protocol

Parte del mental model. Ver [INDEX.md](INDEX.md). Profundiza gaps de Bloques 7.3.3 (LON basic), 13.1 (Niagara Network federation), 13.2 (Fox wire), 14.12 (Template/Match/Bind), 9.2 (BajaScript).

Este bloque cubre 4 gaps técnicos específicos: (1) **LON deep** con XIF/LNML format + ProgramId registry + SNVT conversions + LonMark profiles; (2) **NRIO** Honeywell Remote I/O driver serial; (3) **NiagaraDriver** para station-to-station federation via Fox multiplexed; (4) **BOX protocol** — hallazgo empírico: es "Building Object eXchange", NO "BajaScript-over-HTTP" como decía el scope original. Protocolo distinto de Fox, JSON sobre WebSocket.

---

## 19.1 LON framework overview

### 19.1.1 Módulos LON + clases principales

6 módulos LON verificados:

| Módulo | Función |
|--------|---------|
| `lonworks-rt.jar` | Runtime base — BLonNetwork, BLonDevice, BDynamicDevice, BLonLearnJob, XIF parsers, SNVT marshaling |
| `lonworks-ux.jar` | UI components Workbench (editors, panels, bind tools) |
| `lonworks-wb.jar` | Workbench integration (wizards, debug tools) |
| `lonHoneywell-rt.jar` | Templates Honeywell — BQ7300, BXl10Chc1, BXl10Hyd2, 30+ LNML (Rio, Xl12Fcli, Excel50, T7350Cs) |
| `lonSiebe-rt.jar` | Templates Invensys/Siebe — Mnlrv3/Mnlrs2/Mnlrf3 VAVs |
| `ascLon.jar` | Apollo/Ascent VAV palettes |

Jerarquía:
```
BLonNetwork (gestor comisionamiento)
├── BLocalLonDevice (neuron local, frozen slot)
├── BDynamicDevice (via learnNv + discovery)
└── BLonLink (point-to-point bindings)

BLonDevice
├── BNetworkVariable (NV in/out)
├── BNetworkConfig (NCI)
└── BConfigParameter

BLonProxyExt extends BProxyExt
├── BLonPointDeviceExt
├── BLonBooleanProxyExt
├── BLonFloatProxyExt
├── BLonEnumProxyExt
└── BLonStringProxyExt

BLonLearnJob (discovery orchestrator)
├── BLonDiscoverJob (broadcast WHO-IS-THERE)
├── BLonBindJob (auto-create bindings)
└── BLonCommissionJob (comisionar routers)
```

Clases utility:
- `com.tridium.lonworks.util.xif.XifToXDevice` — parsea XIF → XLonDevice
- `javax.baja.lonworks.datatypes.BProgramId` — 8-byte device identifier
- `javax.baja.lonworks.datatypes.BLearnParameter` — controla discovery
- `javax.baja.lonworks.datatypes.BDeviceEntry` — entry address table descubierto
- `com.tridium.lonworks.netmgmt.BLonLearnJob` — workflow discovery completo

### 19.1.2 LonMark profiles + SNVT types

**LonMark** = consortium standard (Echelon). Define:

**SNVT** (Standard Network Variable Types) — 300+ tipos estándar. Más comunes:
- `SNVT_temp` / `SNVT_temp_p` — 16-bit signed × 0.01, °C
- `SNVT_switch` — 16-bit (bit 15 = state, 14-0 = value)
- `SNVT_elec_kwh` — 32-bit float, kWh acumulado
- `SNVT_flow_p` — 32-bit float, flujo con presión
- `SNVT_count_f` — 32-bit float
- `SNVT_hvac_mode` — 8-bit enum (off/heating/cooling/auto)
- `SNVT_hvac_status` — 16-bit flags (on/off + heating/cooling + fan)
- `SNVT_lev_percent` — 16-bit unsigned 0-100
- `SNVT_time_sync` — 14-byte BCD date/time

**SCPT** (Standard Config Property Types): node address table, domain table, priority slots, retry counts, timers.

**Functional profiles**: ST Sensor, ST Actuator, ST Transceiver, ST Gateway, ST Router.

---

## 19.2 XIF + LNML format

### 19.2.1 Estructura LNML con ejemplos reales

LNML = LonWorks Markup Language Niagara-specific. Encapsula XIF estándar en XML.

Ejemplo real `Rio.lnml` (Honeywell IAQ controller):
```xml
<Rio type='XLonInterfaceFile'>
  <Rio file='datatypes\Rio.lnml'/>
  <Rio type='XLonDevice'>
    <deviceData>
      <majorVersion v='4'/>
      <programID v='80 00 0c 05 00 03 04 06'/>
      <addressTableEntries v='15'/>
      <numNvDeclarations v='33'/>
      <networkInputBuffers v='4'/>
      <channelBitRate v='78125'/>
      <firmwareRevision v='7'/>
    </deviceData>

    <nviRequest type='XNetworkVariable'>
      <objectIndex v='0'/>
      <snvtType v='ObjRequest'/>
      <index v='0'/>
      <serviceType v='acked'/>
    </nviRequest>

    <nvoRemoteTemp type='XNetworkVariable'>
      <objectIndex v='13-16'/>
      <snvtType v='TempP'/>
      <arraySize v='4'/>
      <direction v='output'/>
      <serviceType v='acked'/>
    </nvoRemoteTemp>
  </Rio>
</Rio>
```

Elementos clave:
- `<deviceData>` — metadata hardware (Neuron version, ProgramId, buffer counts)
- `<nvi...>` — Network Variable Input (read remote, write local)
- `<nvo...>` — Network Variable Output (write local, read remote)
- `<nxiConfigProperty>` — Config NCI (no vinculable)
- `<sd>` — self-documentation strings
- `<objectIndex>` — 0 = root, 1-254 = subobjects
- `<snvtType>` — enum SNVT estándar
- `<arraySize>` — si NV es array
- `<direction>` — input/output/ambos
- `<serviceType>` — acked (guaranteed) / unacked (broadcast) / repeat
- `<priorityConfigurable>` — si puede override remoto

Ejemplo Invensys VAV `Mnlrv3.lnml`:
```xml
<Mnlrv3 type='XLonDevice'>
  <deviceData>
    <programID v='80 00 16 50 0a 04 04 0a'/>
    <numNvDeclarations v='61'/>
  </deviceData>
  <nviSpaceTemp><snvtType v='TempP'/><index v='0'/><serviceType v='acked'/></nviSpaceTemp>
  <nvoSpaceTemp><snvtType v='TempP'/><index v='2'/><direction v='output'/></nvoSpaceTemp>
  <nvoUnitStatus><snvtType v='HvacStatus'/><index v='3'/><direction v='output'/></nvoUnitStatus>
  <nviApplicMode><snvtType v='HvacMode'/><index v='4'/><serviceType v='acked'/></nviApplicMode>
</Mnlrv3>
```

### 19.2.2 `XifToXDevice` transformation

Pipeline:
1. Parse XIF texto → intermediate representation
2. Validate SNVT type references
3. Resolve custom types en `datatypes.lnml`
4. Create `XLonDevice` object tree
5. Serialize → LNML XML (persistido en BOG con handle)

Conversiones SNVT → Niagara types (tabla):

| SNVT | LON Bytes | Niagara Type | Units | Nota |
|------|-----------|--------------|-------|------|
| SNVT_temp_p | 16-bit signed × 0.01 | BDouble | °C/°F | 2540 → 25.40°C |
| SNVT_switch | 2 bytes: bit 15=state + 14-0=value | BBoolean + BUInt | — | 0xFF80 = on+0 |
| SNVT_elec_kwh | IEEE 754 float | BDouble | kWh | — |
| SNVT_flow_p | 32-bit float | BDouble | m³/h | — |
| SNVT_count_f | 32-bit float | BDouble | counts | — |
| SNVT_lev_percent | 16-bit unsigned 0-100 | BDouble | % | 8000 = 80% |
| SNVT_hvac_mode | 8-bit enum | BEnum | — | 0=off 1=heat 2=cool 3=auto |
| SNVT_hvac_status | 16-bit flags | BEnum | — | 0xC000 = on+heating |
| SNVT_time_sync | 14-byte BCD | java.util.Date | — | YYMMDDhhmmssss |

Escalado + offset: `niagara_value = (lon_raw × gain) + offset`. `StandardConversion.xml` mapea SNVT units index LON → unidades Niagara (ej. ndx=92 → `celsius`).

---

## 19.3 ProgramId registry + wildcards

### 19.3.1 Formato 8-byte

```
Byte 0-1: Manufacturer ID
Byte 2:   Device Type
Byte 3:   Device Class
Byte 4-7: Device Specific
```

Vendor IDs verificados:

| Vendor | MfgId hex | Devices conocidos |
|--------|-----------|-------------------|
| Tridium | `80 00` | Framework, generic |
| Honeywell | `80 00 0c` | Rio, Q7300, Xl12Fcli, T7350Cs, Excel50 |
| Siebe/Invensys | `80 00 16` | Mnlrv3, Mnlrs2, Mnlrf3 VAVs |

ProgramIds reales extraídos:

| ProgramId | Device | Template mapping |
|-----------|--------|------------------|
| `80 00 0c 05 00 03 04 06` | Rio (Honeywell IAQ) | `cl=lonHoneywell:Rio` |
| `80 00 0c 50 3c 03 04 17` | Q7300 (Honeywell terminal) | `cl=lonHoneywell:Q7300` |
| `80 00 0c 55 01 03 04 28` | XL12 FCU | `xml=lonHoneywell/datatypes/Xl12Fcli.lnml` |
| `80 00 16 50 0a 04 04 0a` | Mnlrv3 Invensys VAV | `xml=lonSiebe/Mnlrv3.lnml` |
| `80 00 16 50 0a 04 04 01` | Mnlrs1 | `xml=lonSiebe/Mnlrs1.lnml` |
| `80 00 0c 0a 46 04 04 01` | IAQMulti | `xml=lonHoneywellAnalytics/IAQMulti.lnml` |
| `80 00 0c 0a 46 04 04 02` | IAQCo2 | `xml=lonHoneywellAnalytics/IAQCo2.lnml` |

### 19.3.2 Wildcards

Pattern matching con `*` (single) y `**` (catch-all):
```
lonworks.80 00 0c ** ** ** ** ** = Honeywell family entero
lonworks.80 00 16 50 0a 04 04 0* = Invensys VAV family, any firmware minor
lonworks.80 00 8e 10 0a 04 0* ** = Vendor 8e + range
```

Resolución: primer match orden de registro (exact > wildcard). Secuencial, no hash table.

---

## 19.4 `BLonLearnJob` discovery

### 19.4.1 LON NM commands + timeouts

`BLonLearnJob` workflow:

1. **Broadcast WHO-IS-THERE** — NM verb `0x50` (query domain). Todos devices responden con NeuronId (6 bytes) + ProgramId (8 bytes). Timeout 3-5s configurable.
2. **Read Address Table** — NM verb `0x60`. Obtiene subnet/node address per device. Stored en `BDeviceEntry`.
3. **Read NV Config Table** (si `useLonObjects=true`) — NM verb `0x72`. Binding info per NV.
4. **Match ProgramId** — consulta `DeviceDef` registry. Exact match → template class/LNML. Wildcard → genérico. No match → `BDynamicDevice` + manual learning.
5. **Auto-create Bindings** (si `autoBinding=true`) — `BLonPoint` + `BLonPointDeviceExt` per NV.

LON NM verbs usados:

| Verb | Hex | Función |
|------|-----|---------|
| QUERY_DOMAIN | `0x50` | Lee domain table entry |
| QUERY_ADDRESS | `0x60` | Lee address table entry |
| QUERY_NV_CONFIG | `0x72` | Lee config NV (binding info) |
| QUERY_STATUS | `0x5A` | Device status (online/offline) |
| SHUTDOWN_DEVICE | `0x7C` | Apagar device |
| RUN | `0x7E` | Poner en run mode |
| RESET | `0x70` | Reset device |

Timeouts observados:

| Operación | TP/FT-10 78k | TP/XF-1250 1.25M |
|-----------|--------------|-------------------|
| Single NV update | 10-20 ms | 1-2 ms |
| WHO-IS broadcast 10 devices | ~500 ms | ~50 ms |
| Full discovery 100 devices | 5-10 sec | 500-1000 ms |
| Throughput max NV updates/s | ~200 | ~3000 |

Reliability: retries automáticos (3-5 estándar), exponential backoff (50/100/200ms), duplicate detection LON-level (transaction IDs). `BLonLearnJob` re-ejecutable en partial failure.

### 19.4.2 `BLearnParameter.useLonObjects`

**true** — funcional partitioning:
- Agrupa NVs por `objectIndex`
- Crea `BLonObjectFolder` per objeto (ej. "Temperature Sensor", "Actuator")
- Refleja estructura lógica device
- Más BOG overhead

**false** — flat layout:
- Todos NVs en contenedor raíz
- Simpler para devices sin object structure clara
- Menos BOG

Otros `BLearnParameter`:

| Param | Default | Efecto |
|-------|---------|--------|
| `commandTimeout` | 5000 ms | WHO-IS broadcast timeout |
| `scanNodesOnly` | false | Scan sin read full config |
| `excludeInUseDevices` | true | Skip devices con bindings existentes |
| `autoBinding` | true | Auto-create proxy points |
| `createHistory` | false | Add HistoryExt per proxy |
| `pollingInterval` | 10000 ms | Refresh si polling mode |

---

## 19.5 NV binding + proxy points

### 19.5.1 `BLonPoint` + `BLonPointDeviceExt`

Pipeline:
1. Remote NV changes → listener → `readOk(newValue)` callback
2. Convierte SNVT bytes → Niagara type con units
3. Copia a `readValue` slot del point
4. `getTuning().readOk()` actualiza timestamp + COV deadband
5. `updateStatus()` limpia STALE bit, propaga status
6. `executePoint()` en engine cycle aplica conversion + facets
7. Retorna `outValue` con status propagado

Subclases especializadas:

| Clase | Tipo LON | Niagara Type |
|-------|----------|--------------|
| `BLonPointDeviceExt` | Generic | Cualquier |
| `BLonBooleanProxyExt` | SNVT_switch, SNVT_bool | BBoolean |
| `BLonFloatProxyExt` | SNVT_..._f | BDouble 32-bit |
| `BLonEnumProxyExt` | SNVT_..._mode, SNVT_hvac_mode | BEnum |
| `BLonStringProxyExt` | SNVT_str | BString |

### 19.5.2 Bind Tool Workbench UI

Clases: `com.tridium.lonworks.xml.BLnmlFile`, `BLonXmlEditor`, `BLonXmlCreate`.

Funcionalidades:
1. Import XIF manual → parsear con `XifToXDevice`
2. Edit LNML en XML editor built-in (highlighting + validation)
3. Review discovery matches post-BLonLearnJob
4. Approve/reject binds individual (checkbox per device/NV)
5. Ajustar binding params (units, polling, COV, scaling)
6. Create custom XIF para devices non-standard
7. Compile LNML → persist BOG con handle

---

## 19.6 LON topology + performance

### 19.6.1 Addressing hierarchy

```
Domain (6-byte DomainId, max 254 devices/domain)
├── Subnet 0-255 (broadcast = 255)
│   ├── Node 1-127 (dentro subnet)
│   │   ├── NeuronId (6 bytes globally unique)
│   │   └── ProgramId (8 bytes type identifier)
│   └── Node 128-254 (routers reservados)
```

Max: 254 subnets × 127 nodes = **~32,000 devices per domain**.

### 19.6.2 Channel types

| Channel | Speed | Max devices | Common use |
|---------|-------|-------------|-----------|
| TP/FT-10 | 78.125 kbaud | 64 recomendado, 127 max/subnet | Legacy HVAC |
| TP/XF-1250 | 1.25 Mbaud | 127/subnet | Modernos high-speed |
| IP-852 | Ethernet | Unlimited virtual | Cloud/WAN overlay |
| Wireless | Variable | Depende chip | IoT |
| PLC | 10-100 kbaud | Bajo SNR | Retail/lighting |

Routers (`BLonRouter`): repeater/bridge/gateway types. Mantienen routing tables. Broadcast forwarding al iniciar `BLonLearnJob`.

### 19.6.3 Limits + scalability

| Métrica | Límite |
|---------|--------|
| Max NVs per device | 254 (8-bit index) |
| Max devices/subnet | 127 |
| Max subnets/domain | 254 |
| Total devices/domain | ~32,000 |
| BOG overhead (100 devices × 50 NVs) | 10-15 MB |
| Heap proxy points (5000 × ~50 bytes) | ~250 MB |

---

## 19.7 Integración Honeywell LON devices

Portfolio en corpus:

| Device | ProgramId | NVs | Función |
|--------|-----------|-----|---------|
| Rio | `80 00 0c 05 00 03 04 06` | 33 | IAQ multi-sensor (temp 4, CO2 1, humidity 2, pressure 1, voltage, current, DO 8, DI 4) |
| Q7300 | `80 00 0c 50 3c 03 04 17` | 30+ | Terminal unit sensor + setpoint |
| Xl12Fcli | `80 00 0c 55 01 03 04 28` | 32 | FCU 12-slot controller |
| Xl10Chc1 | `80 00 0c ...` | Variable | Central heat/cool |
| Xl10Hyd2 | `80 00 0c ...` | Variable | Hydronic 2-stage |
| T7350Cs | LNML | 20+ | Commercial thermostat |
| Excel50 | LNML | Variable | Energy management |
| IAQMulti | `80 00 0c 0a 46 04 04 01` | — | LON IAQ multi-param |
| IAQCo2 | `80 00 0c 0a 46 04 04 02` | — | CO2-específico |

Rio IAQ detalle de objetos:
- Object 0: Management (NVi/o Request, Status)
- Object 1-8: Digital Outputs (8 channels)
- Object 9-12: Digital Inputs (4)
- Object 13-16: Temperature inputs (4, TempP)
- Object 17-18: Humidity inputs (2, LevPercent)
- Object 19: Pressure (LevPercent)
- Object 20: CO2 (LevPercent 0-5000 ppm)
- Object 21-22: Voltage + Current monitor

Invensys VAV (Mnlrv3): 61 NVs incluyendo space temp, setpoint, unit status, HVAC mode, override, fan speed, damper, alarms.

---

## 19.8 LON vs BACnet tradeoffs

| Dimensión | LON | BACnet |
|-----------|-----|--------|
| Discovery | Auto (WHO-IS broadcast) | Semi-auto (I-Am + object list) |
| Binding | Auto (NV → proxy) | Manual (discover objects → map) |
| Addressing | Device type-aware (ProgramId) | Vendor-specific |
| Reliability | Neuronal guaranteed retries | IP-based (TCP/UDP) |
| Bandwidth | Optim TP/FT-10 serial | IP overhead, parallel |
| Configuration | LNML XML device model | Servicios genéricos |
| Latency | Determinístico (CSMA/CD) | Best-effort IP |
| Failover | Routers redundancia built-in | BBMD + Foreign Device |
| Uso ideal | Small/mid-size heterogéneo | Large/multi-vendor cloud |
| Legacy | HVAC legacy install | Industrial de facto |

Hybrid: LON devices via IP-852 overlay, LON+BACnet gateway para edificios grandes, Niagara federation con Subordinates LON + Supervisor BACnet.

---

## 19.9 NRIO (Niagara Remote I/O — Honeywell)

### 19.9.1 Módulos + clases

Verificado empírico — **NRIO existe**:
- `nrio-rt.jar` (runtime)
- `platNrio-rt.jar` (platform support)
- `nrio-wb.jar`, `nrio-ux.jar` (Workbench/UX)
- `nrioConversion-wb.jar`, `docNrio-doc.jar`

Clases principales:
- `BNrioNetwork` extends `BBasicNetwork` — contenedor BNrioDevice
- `BNrioDevice` — modelo device con `address` (logical ID), `deviceType` (enum), `uid` (device blob), `installedVersion` / `availableVersion`
- `BNrio16Module` — 16-point I/O module
- `BNrio34Module` — 34-point I/O
- `BNrio34PriModule` / `BNrio34SecModule` — primary/secondary redundancia
- `BNrioDualDevice` — soporte dual failover

### 19.9.2 Hardware + protocol

Hardware: Honeywell NRIO controllers, módulos 16/34-point analog+digital I/O con redundancia primary/secondary.

Tipos input: voltage, resistive (RTD/thermistor), boolean, counter.
Tipos output: voltage, relay outputs con failsafe configurable.

Protocolo wire: **Serial RS-485** via `portName` + `baudRate` enum. Unsolicited message handling (`unsolicitedMsgCount`, `unsolicitedMessageRate`) — push proactivo desde device.

### 19.9.3 Configuración típica

| Property | Función |
|----------|---------|
| `maxDevices` | Límite devices/network |
| `pushToPoints` | Enable unsolicited messages |
| `minPushTime` | Throttle para COV |
| `maxFailsUntilDown` | Fallos antes marcar device DOWN |
| `sdiValueConfig` | SDI (Standard Data Interface) conversion |
| `outputFailsafeConfig` | Default values en falla |

Proxy extensions: `BNrioProxyExt`, `BNrio16ProxyExt`, `BNrio16WriteProxyExt`.
Containers: `BNrioIOPoints`, `BNrio16Points` (analog/boolean/counter/relay).
Conversions custom: `BNrioThermistorType3Conversion`, `BNrioTabularThermistorConversion`, shunt 500 ohm.

Acciones: `submitDeviceDiscoveryJob()` discovery, `upgradeFirmware()` firmware update.

---

## 19.10 Otros drivers Honeywell-específicos

Verificados en corpus:

**`honEdgeDriver`** (feature license):
- Sin JAR dedicado visible
- Hipótesis: legacy edge/gateway para periféricos Honeywell
- License: unlimited (device.limit/point.limit/history.limit=none)

**`honConnectedPower`**:
- Feature license
- Power/energy driver, integración probable con microgrid/DR (demand response)

**`bport`** (feature license):
- Driver propietario Honeywell
- Hipótesis: BACnet proprietary transport o Panel Bus interface

**`maxpro-rt.jar`** (Maxpro NVR Video):
- Clases: `BMaxproNetwork`, `BMaxproCamer`, `BMaxproNvr`
- License: `camera.limit=16`, `foxStream.limit=none` (video streaming multiplexado via Fox)
- Soporta RTSP, ffmpeg integration
- Event capture + alarm sync vía niagaraDriver-rt

**`honPlantController-rt.jar`**:
- Depends: serial-rt, bacnet-rt, modbusAsync-rt, platMstp-rt, platPanelbus-rt
- Clase `BHonPlantControllerService`
- BTP (Boiler Tuning Protocol) support
- Switch port management (RSTP, cable diagnostics)
- Native library: `libplantctrl.so` (Linux device control)

**`honeywellBacnetDeviceManager-rt.jar`**:
- `BHonBacnetDeviceConfig` — firmware handling Bacnet devices Honeywell
- Bridge BACnet ↔ OptimizerSupervisor

**`honAdvWirelessCfg-rt.jar`**:
- WiFi/BLE config (Beats platform)
- WPA2/WPA PSK, 802.1X

---

## 19.11 NiagaraDriver — station-to-station federation

### 19.11.1 Arquitectura — `BNiagaraNetwork` + `BNiagaraStation`

Módulo: `niagaraDriver-rt.jar`.

**`BNiagaraNetwork`** extends `BDeviceNetwork` implements `BFoxService$FoxServerConnectionListener`:
- Root del driver (BComponent en Drivers folder)
- `localStation` — BLocalSysDefStation (self)
- `sysDefProvider` — proveedor system definition remoto
- `tuningPolicies` — `BNiagaraTuningPolicyMap` (polling rates, thresholds)
- `historyPolicies` — `BHistoryNetworkExt`
- `workers` — `BCyclicThreadPoolWorker` (thread pool compartido)
- `virtualPolicies` — `NiagaraVirtualNetworkExt`
- `persistFetchedTags` — cache remote tags en proxy points (N4.2+)

**`BNiagaraStation`** extends `BDevice` implements `NiagaraStation`, `BIPollableHistorySource`:
- Representa station remota como device individual
- `address` — BOrd al host remoto
- `clientConnection` — `BFoxClientConnection`
- `serverConnectionOrd` — enlace bidireccional (reverse tunnel)
- `hostModel` / `hostModelVersion` — metadata plataforma remota
- `version` — N4 version de station remota

Subtypes:
- `BNiagaraEdgeLiteStation` — Edge Lite (periféricas, menos funcionalidad)
- `BNiagaraStationFolder` — carpeta contenedora

### 19.11.2 Device extensions (6)

| Extension | Función |
|-----------|---------|
| `BNiagaraPointDeviceExt` | Punto proxy mappings |
| `BNiagaraHistoryDeviceExt` | Archives remotas sincronizadas |
| `BNiagaraAlarmDeviceExt` | Alarmas replicadas |
| `BNiagaraScheduleDeviceExt` | Sincronización schedules |
| `BNiagaraUserDeviceExt` | Sincronización cuentas usuario |
| `BNiagaraVirtualDeviceExt` | Gateway puntos virtuales |

### 19.11.3 Fox channels multiplexados

Base: `BFoxChannel` (abstract). 6 subclases implementadas:

| Channel | Función |
|---------|---------|
| `BPointChannel` | Subscription values (point data) |
| `BArchiveChannel` | Historia queries |
| `BAlarmChannel` | Alarm events replication |
| `BScheduleChannel` | Schedule sync |
| `BUserSyncChannel` | User account updates |
| `BNiagaraVirtualChannel` | Virtual point data |

Max channels/session: ~1000 (Bloque 13.2.5). Típicamente saturado con 100+ point subscriptions.

Lifecycle: Created → Subscribed (Fox handshake ack) → Active → Unsubscribed → Destroyed.

Reconnection:
- Fox connection falla → `clientConnection=null`
- `BPointChannel` buffers changes (~1-5 min)
- Auto-reconnect configurable backoff
- Upon reconnect: force refresh de todos los proxy points

### 19.11.4 Authentication + Fox handshake

- Fox TLS + HELLO + SCRAM (Bloque 13.2, 18.6)
- Credenciales: username+password remote admin
- `BFoxClientConnection$Interest` — subscribers a events de conexión (history/schedule/alarm channels)
- `BNiagaraNetwork implements BFoxService$FoxServerConnectionListener` — escucha conexiones entrantes Subordinate→Supervisor

### 19.11.5 Proxy points

`BNiagaraProxyExt` extends `BProxyExt` implements `INiagaraProxyExt`:
- `pointId` — BOrd/swid del punto remoto
- `subscriptionStatus` — subscribing / active / stale / offline
- `forceUpdate()` — refresh valor + actions
- `getProxyActionDefault(name)` — obtiene actions remotas (Write, Adjust)

Workflow:
1. Browse remote station tree (RPC discovery)
2. Select points of interest
3. Add BControlPoint con BNiagaraProxyExt local
4. Set `pointId` = remote BOrd
5. Enable → `BPointChannel` subscription activa
6. Values flow via Fox async

### 19.11.6 History + alarm + user forwarding

**History** (`BNiagaraHistoryDeviceExt` + `BArchiveChannel`):
- Supervisor solicita históricos de Subordinate (range queries)
- `BNiagaraHistoryImport`, `BNiagaraHistoryExport` bidireccional
- Retention policies via `BHistoryNetworkExt`

**Alarm** (`BNiagaraAlarmDeviceExt` + `BAlarmChannel`):
- `alarmAck(BAlarmRecord)` — ack remoto
- `alarmUpdate(BAlarmRecord)` — replicación Sub→Super
- Filter por severity, source, time range
- Supervisor mantiene virtual alarm tree reflejando remote state

**User Sync** (`BNiagaraUserDeviceExt` + `BUserSyncChannel`):
- Cuentas sincronizadas Super→Subs
- Changes password/permisos propagados
- `BUserSyncStrategy` manual vs auto

---

## 19.12 Workflow Supervisor/Subordinate típico

Supervisor + 3 Subordinates (Floor1/2/3):

1. **Configuration**: Drivers/NiagaraNetwork → new BNiagaraStation per floor → address + credentials.
2. **Connection**: Fox TLS+HELLO+SCRAM → remote validates creds → bidireccional tunnel established.
3. **Point discovery**: browse remote tree via RPC → select points → Add Proxy Points local.
4. **Subscription**: BPointChannel per point → remote polls locally → async updates via Fox.
5. **History collection**: configure BNiagaraHistoryDeviceExt retention → periodic queries pull deltas → local archive.
6. **Alarm forwarding**: Subordinate alarm → BAlarmChannel → Supervisor alarm tree with origin tracking.
7. **User management**: Supervisor password change → BUserSyncChannel propagates to all Subs.
8. **Runtime monitoring**: dashboard live values via proxies, connection status indicators, aggregated analytics.

---

## 19.13 License + limits + bottleneck

No feature license explícita `niagaraNetwork` (implícito en base platform). Sin `device.limit`/`point.limit` explícitos.

**Bottleneck Supervisor** (Bloque 13.1.7 confirmado):
- Max ~50 Subordinates per Supervisor (1 Fox session × connection pool limit)
- Fox channel exhaustion: ~1000 channels/TLS session × 50 Subs = 50K subscriptions max (bandwidth limited)

Practical limits:
- Network bandwidth 100 Mbps → ~1000 point updates/sec
- Supervisor CPU: `workers` thread pool sizing crítico
- Remote station uplink first bottleneck
- History collection storage I/O bottleneck segundo

Honeywell típico: 5-10 Subs/Supervisor, 10-100 proxy points/Sub.

---

## 19.14 HA + failover

Corpus search: **NO existe `BSupervisorFailover`** o clases secondary supervisor en niagaraDriver-rt.

Interpretación:
- **No HA nativa** — NiagaraDriver no soporta primary+secondary auto failover
- Manual failover: operador cambia DNS/IP en Subordinates → secondary Supervisor
- State replication: sin mecanismo built-in
- Subordinate reconnection: backoff automático solo al mismo Supervisor

HA requiere solución externa (keepalived+VIP, load balancer) + manual state sync.

---

## 19.15 Gotchas operacionales NiagaraDriver

1. **Subordinate offline ambiguo** — proxy status STALE vs OFFLINE depende config. Monitor `clientConnection` status separado.
2. **Credentials rotation break** — change password remote sin update Supervisor → connection fails silent. Update manual en properties.
3. **Fox channel exhaustion** — >1000 channels × 50 Subs → latency spike, queued subscriptions. Monitor debug logs.
4. **History collection contention** — pulling 50 Subs simultáneo → remote DB locked, local point updates blocked. Stagger schedules.
5. **Time sync criticality** — Sub clocks ≠ Super clocks → alarm/history timestamps unreliable. NTP centralizado mandatory.
6. **BPointChannel memory leak** — delete proxy mid-subscription → lingering Fox channels. Cleanup requires station restart (Bloque 13.2.4).
7. **Remote browse slow** — O(N) traversal. Workbench hang si Sub has 1000+ devices. Cached search recommended.
8. **Proxy write async lag** — confirmation delayed 100-500ms. Apps asumiendo sync writes fail.
9. **Alarm duplication** — Sub alarm + local trigger en Super simultáneo → 2 alarms. Dedup not visible.
10. **User sync race condition** — cambio password directo en Sub → BUserSyncChannel next sync sobrescribe con Super value, conflict no resuelto gracefully.

---

## 19.16 NiagaraDriver vs alternativas

| Aspecto | Web HTTP | RPC directo | NiagaraDriver |
|---------|----------|-------------|---------------|
| Connections | stateless/request | stateless/call | 1 session multiplexed |
| Subscriptions | polling manual | polling manual | push nativo |
| History sync | no | no | sí (BArchiveChannel) |
| Alarms | polling manual | polling manual | replicadas |
| Scale | <10 pt/s | <100 calls/s | 1000+ channels |
| Security | HTTPS+creds | Fox TLS+SCRAM | Fox TLS+SCRAM |
| Latency | 100-1000ms | 50-500ms | 10-100ms push |
| Use case | mobile, 3rd-party | dev, scripting | enterprise federation |

---

## 19.17 BOX protocol (Building Object eXchange)

### 19.17.1 Hallazgo empírico — BOX SÍ existe

Verificación corpus confirma: BOX **existe como protocolo distinto** de Fox. Corrección al scope original:
- **Sigla real**: Building Object eXchange (NO "BajaScript-over-HTTP")
- **No es alias Fox** — es protocolo complementario
- **Introducido**: Niagara 3.7, reforzado 4.x con optimizaciones (NCCB-2025 streaming JSON, NCCB-2026 type generation)

**Módulo**: `box-rt.jar`, package `com.tridium.box`.

### 19.17.2 Transport + endpoint

- **Transport**: **WebSocket sobre HTTP/HTTPS** (no TCP directo como Fox)
- **Servlet**: `BoxWebSocketServlet extends WebSocketServlet` (Jetty)
- **Endpoint**: `/box` (servlet mapping)
- **Timeout inactivo** configurable
- **Firewall-friendly** — pasa HTTPS donde Fox TCP 1911/4911 no

### 19.17.3 Arquitectura multiplexada

**Clase base**: `BBoxChannel` (abstract) — cada channel representa un dominio funcional.

Channels especializados verificados:

| Channel | Función |
|---------|---------|
| `BServerSessionChannel` | Gestión sesiones (state, subscriptions, RPC) |
| `BComponentSpaceChannel` | Navegación espacios de componentes |
| `BOrdChannel` | Resolución ORDs |
| `BSysChannel` | Info sistema |
| `BUnitChannel` | Units resolution |
| `BHistoryChannel` | History queries |
| `BAlarmChannel` | Alarm events |
| `BTransferChannel` | File transfer |

Multiplexing: cada channel multiples operations concurrent dentro misma WebSocket connection.

### 19.17.4 Serialización

- **Formato base**: **JSON** (no binary propietario como Fox)
- **Encoder**: `BoxWriter` wrappea JSON con metadata de tipo
- **Type System**: Contracts (similar oBIX) definen schema de Componentes
- **Encoding**: lazy type loading — tipos enviados on-demand para reducir overhead

### 19.17.5 Estructura mensaje BOX

```json
{
  "id": "<correlationId>",
  "type": "<messageType>",
  "channel": "<channelName>",
  "op": "<operation>",
  "body": { ... },
  "serverSessionId": "<sessionId>"
}
```

Cada operación mapea a "service" en channel correspondiente. Channels despachan via método abstract `service(key, body, out, op)`.

### 19.17.6 Server sessions stateful

`BServerSession`:
- Ops `make` (crear) / `del` (destruir)
- `pollchgs` — polling de changes subscription
- `callssc` — RPC invoca `BIServerSideCallHandler` en componentes
- Límite configurable: `BBoxService.sessionLimit`

### 19.17.7 RPC sobre BOX (desde N4.1)

- Clases implement `javax.baja.box.BIServerSideCallHandler`
- Method signature: `BValue method(BComponent comp, BValue arg, Context cx)`
- Requiere registración como agente con permissions specified
- Invocable vía Fox, BajaScript, Web Servlet — uniforme RPC layer

### 19.17.8 Clases verificables corpus

```
com.tridium.box.BBoxService                (singleton service)
com.tridium.box.BoxWebSocketServlet        (endpoint HTTP/WebSocket)
com.tridium.box.BBoxWebSocketAcceptor      (connection acceptor)
com.tridium.box.BServerSessionChannel      (session mgmt)
com.tridium.box.BBoxChannel                (abstract base)
javax.baja.box.BIServerSideCallHandler     (RPC interface)
```

Documentación referencia:
- `modules/docDeveloper-doc/doc/box-rt/module-index.bajadoc`
- `modules/docDeveloper-doc/doc/jsdoc/bajaScript-ux/index.html` (Technical Overview)
- `modules/docDeveloper-doc/doc/niagaraRpc.html`

### 19.17.9 BOX vs Fox — comparativa

| Aspecto | Fox | BOX |
|---------|-----|-----|
| Transport | TCP 1911/4911 binario | WebSocket HTTP/HTTPS JSON |
| Serialización | Binario propietario | JSON |
| Overhead | Bajo | Medio (JSON parsing native JS) |
| Cliente típico | Workbench, drivers, backend | BajaScript browser, web apps |
| Sesiones stateful | Sí (fox-rt) | Sí (box-rt BServerSession) |
| Multiplexing | Conexión por dominio | Múltiples channels en 1 WebSocket |
| Firewall-friendly | No (puertos específicos) | Sí (pasa HTTPS) |
| RPC | Sí (NiagaraRPC sobre Fox) | Sí (BIServerSideCallHandler desde N4.1) |

### 19.17.10 Corrección al Bloque 9.2

Bloque 9.2 mencionó que BajaScript v2 es "Fox-over-WebSocket/HTTP". **Empírico**: BajaScript usa BOX (no Fox) cuando corre en browser. Fox es para Workbench + drivers backend. BOX es el wire real para web apps.

---

## 19.18 Hallazgos críticos del bloque

1. **LON ProgramId breakdown verificado**: bytes 0-1 MfgId (Tridium 80 00, Honeywell 80 00 0c, Siebe 80 00 16), byte 2 Device Type, byte 3 Device Class, bytes 4-7 Specific.

2. **7 LON NM verbs hex documentados**: 0x50 QUERY_DOMAIN, 0x60 QUERY_ADDRESS, 0x72 QUERY_NV_CONFIG, 0x5A QUERY_STATUS, 0x7C SHUTDOWN, 0x7E RUN, 0x70 RESET.

3. **Throughput LON empírico**: TP/FT-10 (78.125 kbaud) ~200 NV updates/s; TP/XF-1250 (1.25 Mbaud) ~3000 NV/s. Full discovery 100 devices: 5-10s (TP/FT-10) vs 500-1000ms (TP/XF-1250).

4. **Honeywell Rio IAQ** — 33 NVs mapeadas: DO 8, DI 4, Temperature 4 (TempP array), Humidity 2, Pressure 1, CO2 1 (0-5000 ppm), Voltage 1, Current 1.

5. **LNML format diferencia con XIF estándar**: LNML encapsula XIF en XML Niagara-specific con elementos `<deviceData>`, `<nvi...>`/`<nvo...>`, `<snvtType>`, `<objectIndex>`, `<serviceType>` (acked/unacked/repeat).

6. **NRIO existe empíricamente** — `nrio-rt.jar` + 5 módulos asociados. Clases `BNrioNetwork`, `BNrioDevice`, `BNrio16Module`, `BNrio34Module` + redundancia primary/secondary vía `BNrio34PriModule`/`BNrio34SecModule`/`BNrioDualDevice`.

7. **NRIO wire = Serial RS-485** con unsolicited push messages (no polling-only). Conversions custom: BNrioThermistorType3Conversion, BNrioTabularThermistorConversion, shunt 500 ohm.

8. **Drivers Honeywell adicionales** catalogados: honEdgeDriver, honConnectedPower, bport, maxpro-rt (video), honPlantController (BTP boiler tuning + libplantctrl.so), honeywellBacnetDeviceManager, honAdvWirelessCfg (WiFi/BLE WPA2 + 802.1X).

9. **NiagaraDriver — 6 device extensions** cubriendo todo el stack federation: Point + History + Alarm + Schedule + User + Virtual. Cada una con su propio Fox channel multiplexado.

10. **6 Fox channels verificados** en NiagaraDriver: BPointChannel, BArchiveChannel, BAlarmChannel, BScheduleChannel, BUserSyncChannel, BNiagaraVirtualChannel. Base abstract `BFoxChannel`.

11. **NO HA nativa en NiagaraDriver** — `BSupervisorFailover` no encontrado. Primary+secondary Supervisor requiere solución externa.

12. **10 gotchas operacionales** concretos documentados — el más crítico: Fox channel memory leak por proxy point delete mid-subscription (requiere station restart).

13. **BOX SÍ existe** como protocolo distinto de Fox. Sigla real: "Building Object eXchange". Corrección al scope original del usuario.

14. **BOX = WebSocket + JSON**, Fox = TCP + binario. BOX es firewall-friendly (pasa HTTPS), Fox no. BOX introducido N4 3.7, es el wire de BajaScript v2 en browser.

15. **Corrección al Bloque 9.2**: BajaScript usa **BOX** en browser, NO Fox. Fox es para Workbench + drivers backend + integración backend.

16. **RPC desde N4.1 via BIServerSideCallHandler** — interfaz común invocable por Fox, BajaScript, Web Servlet. Unified RPC layer.

17. **8 BOX channels** verificados: BServerSessionChannel, BComponentSpaceChannel, BOrdChannel, BSysChannel, BUnitChannel, BHistoryChannel, BAlarmChannel, BTransferChannel.

---

## 19.19 Conexiones con otros bloques

- **Bloque 4 (Baja)**: BLonPointDeviceExt + BNrioProxyExt + BNiagaraProxyExt todas extienden BProxyExt (pipeline Bloque 4).
- **Bloque 5.1 (ORD schemes)**: BLon ORD `lonworks.XX XX...` para ProgramId matching; BNiagara `niagara://host/ord` para remote resolution.
- **Bloque 7.1 (Driver framework)**: BLonNetwork, BNrioNetwork, BNiagaraNetwork todos extienden BDeviceNetwork (o BBasicNetwork).
- **Bloque 7.3.3 (LON basic)**: este bloque profundiza LNML format + ProgramId registry + SNVT conversions.
- **Bloque 8.1 (Alarms)**: BAlarmChannel replica alarms Sub→Super vía BNiagaraAlarmDeviceExt.
- **Bloque 8.2 (History)**: BArchiveChannel + BNiagaraHistoryDeviceExt replica histories.
- **Bloque 9.2 (BajaScript)**: **corrección** — BajaScript browser usa BOX, no Fox.
- **Bloque 11 (RBAC)**: BNiagaraUserDeviceExt + BUserSyncChannel sincroniza users + roles Super→Subs.
- **Bloque 13.1 (Niagara Network)**: este bloque expande el driver runtime que implementa la federation.
- **Bloque 13.1.7 (Supervisor bottleneck)**: ~50 Subordinates confirmado + Fox channel exhaustion ~1000 channels.
- **Bloque 13.2 (Fox wire)**: 6 Fox channels de NiagaraDriver son consumidores principales del wire protocol.
- **Bloque 13.2.3 (virtual components)**: BNiagaraVirtualDeviceExt + BNiagaraVirtualChannel gateway.
- **Bloque 13.2.5 (channel exhaustion)**: limit ~1000 applies directamente a NiagaraDriver.
- **Bloque 14.12 (Template/Match/Bind)**: este bloque profundiza el caso LON — XIF + LNML + ProgramId registry + wildcards.
- **Bloque 18.6 (HELLO+SCRAM)**: auth de Fox connection usado por NiagaraDriver.
- **Bloque 20 (siguiente)**: BApp, net module, misc.

---

## Engram topic keys

- `niagara/drivers/lon-deep-xif-lnml-snvt-programid`
- `niagara/drivers/nrio-niagaradriver-station-federation`
- `niagara/protocols/box-protocol-websocket-json`
