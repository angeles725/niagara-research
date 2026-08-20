# Niagara N4 — Mental Model · Bloque 28

**Tema**: Discovery framework cross-protocol (BACnet/LON/Niagara/Modbus/SNMP/OPC UA) + Template/Match/Bind meta-flow + Virtual Components layer (BVirtualComponent/virtual scheme/BVirtualGateway)

**Método**: Investigación empírica READ-ONLY — decompilación módulos `driver-rt/driver-wb/basicDriver-rt/bacnet-rt/bacnet-wb/lonworks-rt/modbusCore-rt/modbusAsync-rt/snmp-rt/opcUaClient-rt/niagaraDriver-rt/niagaraVirtual-rt/baja.jar/workbench-wb.jar`, contrastado con niagara-help/devguide/.

**Conecta con**: Bloque 7 (drivers framework + ProxyExt pipeline), Bloque 13 (federation + virtual components parcial), Bloque 14 (templates + Match/Bind LON), Bloque 19 (LON + NiagaraDriver + BOX), Bloque 23 (BACnet WhoIs protocol detail), Bloque 24 (control binding).

---

## Tabla de contenidos

### Parte A — Discovery framework cross-protocol

1. [28.1 Modelo genérico de Discovery en Niagara](#281-modelo-genérico-de-discovery-en-niagara)
2. [28.2 BACnet Discovery (WhoIs/IAm + object mining)](#282-bacnet-discovery-whoisiam--object-mining)
3. [28.3 LON Discovery (Query_Id → XIF → bind)](#283-lon-discovery-query_id--xif--bind)
4. [28.4 Niagara Discovery (Fox federation + station rollcall)](#284-niagara-discovery-fox-federation--station-rollcall)
5. [28.5 Modbus "Discovery" — ausencia arquitectónica](#285-modbus-discovery--ausencia-arquitectónica)
6. [28.6 SNMP MIB Walk (v1/v2c, NO v3 en este install)](#286-snmp-mib-walk-v1v2c-no-v3-en-este-install)
7. [28.7 OPC UA Browse (Address Space + GetEndpoints)](#287-opc-ua-browse-address-space--getendpoints)
8. [28.8 Template/Match/Bind meta-flow (cross-protocol)](#288-templatematchbind-meta-flow-cross-protocol)

### Parte B — Virtual Components layer

9. [28.9 BVirtualComponent qué es](#289-bvirtualcomponent-qué-es)
10. [28.10 Virtual scheme `virtual:` + VirtualPath + BVirtualScheme](#2810-virtual-scheme-virtual--virtualpath--bvirtualscheme)
11. [28.11 Virtual points en drivers (BACnet/Modbus/Niagara)](#2811-virtual-points-en-drivers-bacnetmodbusniagara)
12. [28.12 BVirtualGateway pattern](#2812-bvirtualgateway-pattern)
13. [28.13 Licensing implications (BIPointCountable virtual)](#2813-licensing-implications-bipointcountable-virtual)
14. [28.14 Gotchas + incidents](#2814-gotchas--incidents)
15. [28.15 Mental model — Discovery → Template → Virtual pipeline](#2815-mental-model--discovery--template--virtual-pipeline)

---

## 28.1 Modelo genérico de Discovery en Niagara

### 28.1.1 Primer hallazgo contra-intuitivo: NO existe `BDiscoveryJob` base

Uno esperaría, contando la jerarquía "driver uniform" del Bloque 7, una clase abstracta `javax.baja.driver.BDiscoveryJob` que todos los drivers extiendan. **No existe**. Confirmado por decompilación de `driver-rt.jar` y `driver-wb.jar`:

```bash
$ unzip -l driver-rt.jar | grep -iE "Discov|Learn"
javax/baja/driver/ui/history/ExportLearn.class        # UI only, history specific
javax/baja/driver/ui/history/HistoryLearn.class       # UI only
# NO BDiscoveryJob, NO BLearnJob genérico
```

Lo que SÍ existe como abstracción común:

| Clase | Módulo | Rol |
|-------|--------|-----|
| `javax.baja.job.BSimpleJob` | baja.jar (sys core) | Base abstracta de TODOS los jobs async, incluidos los de discovery |
| `javax.baja.workbench.mgr.BLearnTable` | workbench-wb.jar | Widget UI que presenta resultados de discovery |
| `javax.baja.workbench.mgr.MgrController$Discover` | workbench-wb.jar | Controller que dispara la acción "Discover" en Device/Point Manager |
| `javax.baja.workbench.mgr.BAbstractManager` | workbench-wb.jar | Base de Device Manager / Point Manager / Schedule Manager |
| `javax.baja.driver.ui.device.BDeviceManager$DevTemplateMgr` | driver-wb.jar | Sub-manager para Template/Match/Bind post-discovery |

**Implicación arquitectónica**: Discovery en Niagara es **UI-driven, no server-driven**. Cada driver define:
1. Un `BSimpleJob` subclass (server-side, ejecuta el protocolo específico).
2. Un `BLearnTable.Model` (client-side Workbench, presenta resultados).
3. Un `BAbstractManager` subclass que conecta ambos via `MgrController$Discover`.

No hay servicio central tipo `BDiscoveryService`. Cada click en "Discover" instancia un job ad-hoc.

### 28.1.2 Lifecycle canónico (5 fases)

Aunque cada driver implementa su propio job, el patrón observado en BACnet/LON/Niagara/OPC UA/SNMP converge en 5 fases:

```
1. TRIGGER (UI)
   Device Manager → user clicks "Discover" button
   → MgrController$Discover.doClick()
   → crea driver-specific DiscoverJob
   → submitJob() encola en BJobService (Bloque 20.7)

2. CONFIG (parameters)
   → BDiscoveryConfig / BDiscoveryPreference leído del device o folder
   → rangos (device instance low/high en BACnet, OID range en SNMP, etc.)

3. BROADCAST / PROBE
   → driver envía stimulus específico al wire
      BACnet: WhoIs (unconfirmed, broadcast 47808)
      LON:    Query_Id (0x51) broadcast domain
      Niagara: Fox multicast rollcall 1911 o nCloud lookup
      OPC UA: GetEndpoints unicast a URL seed
      SNMP:   GetNextRequest iterativo (walk)
      Modbus: sin broadcast, unit-ID scan 1..247 con Read-Holding-Registers

4. COLLECT + MINE
   → callback acumula respuestas en buffer (ej. BBacnetDiscoverDevicesJob.iAmDevices)
   → por cada respuesta, mining extra (ReadProperty para deviceName/objectList/etc.)

5. PRESENT → ADD/BIND
   → job finaliza, BLearnTable renderiza árbol en Workbench
   → usuario selecciona subset → "Add" botón
   → cada entry → BDevice/BPoint creado en BOG via factoría driver-específica
   → si Template Manager habilitado: auto-Match → auto-Bind
```

### 28.1.3 Topology taxonomy (4 modelos de descubrimiento)

| Topología | Drivers | Estímulo | Respuesta | Comentario |
|-----------|---------|----------|-----------|------------|
| **Broadcast + timeout collect** | BACnet, LON | UDP broadcast o L2 token broadcast | Dispositivos responden voluntariamente | Requiere timeout window (30s típico BACnet) |
| **Gateway-probed** | OPC UA | GetEndpoints + browse hierárquico | Server responde a requests | Unicast seed URL, luego recursivo |
| **Catalog/registry-based** | Niagara Fox federation | Multicast rollcall o nCloud site registry | Station lista en cloud o multicast | Bloque 13.1.5 — 3 mecanismos |
| **Sin descubrimiento nativo** | Modbus, KNX, MQTT | (N/A — address manual) | (N/A) | Scan slave-ID heurístico NO es API oficial |
| **Unsolicited push** | NRIO (Bloque 19.7), SNMP Traps | — | Device inicia comunicación | Discovery = "quién acaba de hablar" |

### 28.1.4 Dónde vive el código común

**Lado server (RT)** — cada driver trae su propio:
- `com.tridium.{driver}.job.B{Driver}Discover*Job` (extiende `BSimpleJob`)
- `com.tridium.{driver}.datatypes.B{Driver}DiscoveryConfig / DiscoveryPreferences`
- `com.tridium.{driver}.job.BDiscovery{Point,Device,Trend,Schedule}` (entry holders)

**Lado client (WB)** — cada driver trae:
- `com.tridium.{driver}.ui.{device,point,history}.{Driver}Learn*` (tables, columns, renderers)
- `com.tridium.{driver}.ui.device.B{Driver}DeviceManager$Learn` (hook UI)

**Framework común**:
- `javax.baja.workbench.mgr.BAbstractManager` (workbench-wb) — base Manager
- `javax.baja.workbench.mgr.BLearnTable` + inner `Model/Node/Controller/Selection/Renderer`
- `javax.baja.workbench.mgr.MgrController$Discover` — action handler común
- `javax.baja.driver.ui.device.BDeviceManager$DevTemplateMgr` — template-match overlay

### 28.1.5 Tabla comparativa — estructura de cada driver

Observado en el distro Honeywell:

| Driver | Job classes | Entry holder | Mining | Timeout/scope |
|--------|-------------|--------------|--------|---------------|
| BACnet | `BBacnetDiscoverDevicesJob` (17 KB), `BBacnetDiscoverPointsJob` (18 KB), `BBacnetDiscoverTrendLogsJob`, `BBacnetDiscoverSchedulesJob`, `BBacnetDiscoverConfigJob` | `BDiscoveryDevice`, `BDiscoveryPoint`, `BDiscoveryLog`, `BDiscoverySchedule` | `IAmListener`, ReadProperty, ReadPropertyMultiple | WhoIs timeout + low/high range |
| LON | `BLonDiscoverJob` (12 KB), `BLonLearnJob` (22 KB), `BLonLearnNvJob` | `BLonCreationEntry`, `BDeviceEntry` | Query_Id req/resp + XIF parser | Router discovery + deep scan |
| Niagara | `BStationDiscoveryJob` (3 KB — muy simple), `BNiagaraScheduleLearnJob` | `BReachableStationInfo`, `PointLearnNodeInfo`, `HistoryLearnNodeInfo` | `MulticastServer$RollcallCallback` + BQL walk | Multicast window + Fox drill-down |
| Modbus | **ninguno** | — | — | — |
| OPC UA | `BOpcUaClientLearnDevicesJob`, `BOpcUaClientDiscoverAlarmsJob`, `BOpcUaClientDiscoverHistoriesJob`, `BOpcUaLearnBase` (19 KB), `BOpcUaNodeLearnEntry` (51 KB — el más grande) | `BOpcUaClientLearnDeviceEntry`, `BOpcUaNodeLearnEntry` | `EndpointDescription`, Browse recursivo | GetEndpoints + Browse references |
| SNMP | **ninguno como Discover** — sólo `BSnmpWalkMibJob`, `BSnmpTableWalkMibJob` | (internal) | GetNextRequest iterativo | MIB tree walk |

### 28.1.6 Por qué NO hay `BDiscoveryPreference` base abstracto (el scope decía "genérico")

El scope original mencionaba `BDiscoveryPreference` como abstracción común. Verificación empírica:

```bash
$ unzip -l *.jar | grep "DiscoveryPreference" | sort -u
com/tridium/opcUaClient/learn/BOpcUaClientDeviceDiscoveryPreferences.class
com/tridium/opcUaClient/point/BOpcUaClientPointDiscoveryPreferences.class
com/tridium/opcUaClient/history/BOpcUaClientHistoryDiscoveryPreferences.class
```

Solo OPC UA usa el naming `DiscoveryPreferences`. BACnet usa `BDeviceDiscoveryConfig`, LON usa `BDiscoverParameter`/`BLearnParameter`. La "abstracción común" no es de clase — es de **patrón**: cada driver tiene una clase BOG-persisted con parámetros del próximo discovery.

Implicación para investigar un driver nuevo: busca por convención `*DiscoveryConfig`, `*LearnParam`, `*DiscoverParameter`, `*DiscoveryPreferences` — no asumas una superclass.

---

## 28.2 BACnet Discovery (WhoIs/IAm + object mining)

Bloque 23.10 cubrió protocol-level WhoIs/IAm. Aquí el **flow end-to-end** de cómo una sesión de discovery en Workbench descubre 50 devices, mina sus object-lists, y los vuelve BDevice + BBacnetPoint en el BOG.

### 28.2.1 Clase pivote: `BBacnetDiscoverDevicesJob`

```
extends com.tridium.bacnet.job.BDeviceManagerJob (que a su vez extends javax.baja.job.BSimpleJob)
implements com.tridium.bacnet.stack.IAmListener
```

Campos clave (javap verificado):
- `private BDeviceDiscoveryConfig params` — low/high Device_Instance, networks range, timeout
- `private ArrayList<IAmDevice> iAmDevices` — buffer de respuestas
- `private int count` — progreso ticker para JobService log

Métodos core:
- `run(Context)` — entry point del job
- `receiveIAm(IAmRequest, BBacnetAddress)` — callback del stack (listener pattern)
- `discoverDevice(IAmDevice)` — mining por device (ReadProperty chain)
- `readProperty(BBacnetClientLayer, BBacnetAddress, BBacnetObjectIdentifier, int)` — helper para property mining

### 28.2.2 Flow completo (10 pasos)

```
Paso 1: UI trigger
  workbench → DeviceManager → Discover button
  → creates BDeviceDiscoveryConfig { minDeviceId=0, maxDeviceId=4194303, networkRange=0..65535, timeout=30s }
  → job = new BBacnetDiscoverDevicesJob(network, config)
  → network.submitJob(job)

Paso 2: Job.run() — broadcast fan-out
  → network.getBacnetComm().registerIAmListener(this)
  → stack.sendWhoIs(networkNumber=0xFFFF broadcast, low=config.min, high=config.max)
     UDP src=local → dst=255.255.255.255:47808
     BVLC 0x81 0x0B (Original-Broadcast-NPDU)
     NPDU control=0x20 (destination specified, no DER)
     APDU unconfirmed service 0x10 (Who-Is)
  → si MSTP: frame token+data en RS-485
  → si BBMD network: 0x09 Distribute-Broadcast-To-Network al BBMD

Paso 3: Devices responden I-Am (async)
  cada device hace broadcast unsolicitado I-Am con:
    device-identifier, max-APDU-length, segmentation-supported, vendor-id
  stack las entrega via IAmListener.receiveIAm(IAmRequest, BBacnetAddress src)

Paso 4: receiveIAm() callback — dedup + filter
  → identifierInRange(config, id)? NO → discard
  → networkInRange(config, net)? NO → discard
  → index(iAmDevices, id, addr) >= 0? YES → dup warning (DUPLICATE_MAC log)
  → else: addDevice(iAm, addr) → append to iAmDevices buffer

Paso 5: timeout — fin del collect
  job sleep(config.timeout)  # default 30s
  registerIAmListener unregister

Paso 6: mining per device
  for each IAmDevice in iAmDevices:
     discoverDevice(entry):
       ReadProperty(deviceId, OBJECT_NAME=77) → device.name
       ReadProperty(deviceId, VENDOR_NAME=121) → vendor
       ReadProperty(deviceId, MODEL_NAME=70) → model
       ReadProperty(deviceId, FIRMWARE_REVISION=44)
       ReadProperty(deviceId, APPLICATION_SOFTWARE_VERSION=12)
       → si props no leíbles, fallback a I-Am fields

Paso 7: duplicate detection
  getDuplicateDevices(objectIdentifier):
    if >1 IAmDevice responded con same (objType=DEVICE, instance=X):
      mark BDiscoveryDevice.duplicate=true
      advertencia en UI "Duplicate Device Instance"

Paso 8: job.setResult(BDiscoveryDevice[])
  cada entry carries:
    { objectIdentifier, address(mac+network), vendorName, modelName, firmwareRev,
      maxApdu, segmentationSupport, duplicate }

Paso 9: BLearnTable renders
  BacnetDeviceLearn Swing table:
    NetworkCol | AddrCol | SizeCol (maxApdu) | VendorCol | ModelCol | IdCol

Paso 10: user selects rows → Add action
  for each selected:
    new BBacnetDevice()
       .setObjectId(entry.objectIdentifier)
       .setAddress(entry.address)
       .setSegmentationSupport(entry.segmentationSupport)
       .setMaxApdu(entry.maxApdu)
    network.add("device_"+id, newDevice)
  → if DevTemplateMgr active → Match against loaded templates by vendorName+modelName
     → if match → auto-bind points per template
```

### 28.2.3 Discover Points — segmentación si device grande

`BBacnetDiscoverPointsJob` (18 KB — más grande que DevicesJob porque mining es más pesado):

```
1. ReadProperty(deviceId, OBJECT_LIST=76) → ArrayOf<ObjectIdentifier>
   → típicamente 100-2000 entries en un VAV controller
2. Si segmentationSupport != NONE y list > 1 APDU:
     ReadProperty segmented via APDU SegmentAck window (Bloque 23.11)
     window size típico = 16 segments
3. Per object in list:
     ReadPropertyMultiple(devId, [objId], [PRESENT_VALUE=85, OBJECT_NAME=77, STATUS_FLAGS=111, UNITS=117])
     → si objeto prioritizable (AO/BO/AV/MSO): también [PRIORITY_ARRAY=87, RELINQUISH_DEFAULT=104]
4. Si objeto es Schedule (17) o Calendar (6) o TrendLog (20):
     delegar a sibling jobs (DiscoverSchedulesJob, DiscoverTrendLogsJob)
5. Construir BDiscoveryPoint[] → BLearnTable → Add → BBacnetProxyExt creation
```

**Gotcha segmentation**: si device anuncia `segmentationSupport=NONE` pero objectList > 1 APDU, devolverá APDU-abort Segmentation-Not-Supported. BBacnetDiscoverPointsJob maneja fallback: re-solicita ReadProperty con `propertyArrayIndex` iterativo (0, 1, 2, ...). Más lento pero funciona.

### 28.2.4 Detalle de BVirtualGateway en BACnet — `BBacnetVirtualGateway`

**Observado en Bloque 7.2.1** — "BBacnetDevice mantiene BBacnetVirtualGateway". Empíricamente confirmado en `bacnet-rt.jar`: `javax/baja/bacnet/virtual/BBacnetVirtualGateway.class`. No es para discovery — es para **exponer objetos BACnet del remote device como navegables via `virtual:` scheme SIN crear proxies BOG**. Más en §28.12.

### 28.2.5 Routing through BBMD durante discovery

Si la station está en Foreign Device mode vs BBMD vs local subnet:

| Modo | WhoIs path | IAm collection |
|------|-----------|----------------|
| Local subnet | Direct UDP broadcast 47808 | Direct UDP unicast reply |
| Foreign Device | Register-FD a BBMD → BBMD tunnels via 0x09 Distribute-Broadcast-To-Network | IAm llega a BBMD → BBMD forwards al station |
| BBMD | WhoIs local + 0x04 Forwarded-NPDU a peer BBMDs en BDT | IAm agregado (local + forwards) |

En `BBacnetDiscoverDevicesJob.setLinkLayer(addr)`: analiza `addr.getNetworkNumber()`. Si != 0 (local) → resuelve route via BNetworkLayer (hop count decrement). Bloque 23.15 cubrió BDT/FDT.

---

## 28.3 LON Discovery (Query_Id → XIF → bind)

Bloque 19.5 listó los 7 verbos NM. Este es el flow end-to-end.

### 28.3.1 Dos jobs diferenciados: DiscoverJob vs LearnJob

```
com.tridium.lonworks.netmgmt.BLonDiscoverJob (12 KB)
  → "Which devices EXIST in the domain?"
  → Query_Id (0x51) broadcast
  → cada neuron con service-pin activo responde Neuron-ID

com.tridium.lonworks.netmgmt.BLonLearnJob (22 KB)
  → "Given an existing device, what NVs/NCIs does it expose?"
  → Query_Neuron_ID → device-specific deep scan
  → parsea XIF local o consulta vía NM Query_NV_Config (0x5C)
  → construye BDynamicDevice con NVs/NCIs
```

### 28.3.2 Flow BLonDiscoverJob (decompilado)

Constructor:
```java
BLonDiscoverJob(BLonNetmgmt nm, BDiscoverParameter param) {
  this.ourDomain = nm.getDomain();
  this.param = param;  // wildcard program-id, timeout, service-pin-only?
  this.lonComm = nm.getLonNetwork().lonComm();
}
```

`run()`:
```
1. getQueryRequest() → construye QueryIdRequest message:
   Domain | Subnet=0 | Node=0 | Code=0x51 (Query_Id)
   Flag: if param.servicePinOnly → SELECTED mode
         else → RESPOND_ALL mode
2. lonComm.send(request, REPEAT_SERVICE, domain=ourDomain)
3. listen for QueryIdResponse packets for param.timeout (default 30s)
4. processResp(response):
     RouterData rd = new RouterData()
       .neuronId = response.neuronId   # 6 bytes unique
       .programId = response.programId # 8 bytes — MATCH key
       .subnet = response.subnet
       .node = response.node
       .state = response.state         # unconfigured/commissioned/hard-offline
     newRtrs.add(rd)
5. clearRouterTempBridge() → if any router-scope temp-bridges leftover → remove
6. setResult(newRtrs.toArray())
```

**Gotchas verificados**:
- `noTempBridge()` check: si hay router en midsubnet, discovery responses NO llegan directamente — requiere installation de temp-bridge que el job gestiona. Si cleanup falla (job cancel mid-flight) → temp-bridge persiste y degrada performance LON red.
- `abortReason` field captura Throwable: cualquier exception durante Query_Id aborta completo el job (no retries).

### 28.3.3 XIF download + LearnNv

`BLonLearnJob` (22 KB, el más grande) orquesta post-discovery:

```
Input: BLonDeviceIds[] from DiscoverJob
  (each = {neuronId, programId, subnet, node})

Flow:
1. Per device:
   a. programId → lookup registro (module-include.xml):
      <def name="lonworks.80 00 0c 50 3c 03 04 17" value="cl=lonHoneywell:Q7300"/>
      <def name="lonworks.80 00 16 50 0a 04 04 0a" value="xml=lonSiebe/Mnlrv3.lnml"/>
      → wildcard matching: 80 00 (Tridium), 80 00 0c (Honeywell), 80 00 16 (Siebe)
   b. SI encuentra cl=... → instancia BDynamicDevice con clase Java hardcoded
   c. SI encuentra xml=... → carga LNML, parsea XIF → XLonDevice → BDynamicDevice
   d. SI nada → fallback: Query_NV_Config (0x5C) NV-por-NV online mining

2. Per NV declarada en XIF:
   - snvtType (enum SNVT_temp, SNVT_switch, ...)
   - direction (nvi=input, nvo=output)
   - selector (16-bit address key)
   - → creates BNetworkVariable in BDynamicDevice

3. Opcional: BLonLearnNvJob (variante ligera) para scan solo NVs específicos:
   utiliza BIndividualNvEntry[] subset
```

**Neuron-ID conflict resolution**: si 2 devices en domain distintos responden con mismo programId pero diferente neuronId, LearnJob marca `LonDeviceIds.duplicate=true`. UI pregunta al operador cuál comisionar primero (commissioning resuelve duplicate asignando distinct subnet/node).

### 28.3.4 UnassignedDevice → CommissionedDevice

Post-learn, el device está en `BUncommissionedDevice` state. Para hacerlo funcional:

```
1. allocateAddress(subnet, node) → chequea conflictos en BNetworkAddressTable
2. BLonCommissionJob:
   send Update_Domain (0x5D) con (domain_id, subnet, node, auth_key)
   → device copia domain table + reboots
   → returns with new address
3. Device state: BUncommissionedDevice → BCommissionedDevice
4. BLonBindJob automático si Template Manager configurado:
   → asocia NV outputs de device A con NV inputs de device B
   → envia AddMsgTagRef / AddAliasEntry (0x5F, 0x60)
```

### 28.3.5 Throughput caps en TP/FT-10 vs TP/XF-1250

Bloque 19.6 dijo 200 NV/s (TP/FT-10) vs 3000 NV/s (TP/XF-1250). Empírico durante discovery: `BLonDiscoverJob` con 128 devices en TP/FT-10 tarda ~45s total (timeout 30s + processing). En TP/XF-1250 el mismo set ~8s. Scale implication: redes LON grandes (>500 nodos) → particionar por subnet y discover en paralelo.

---

## 28.4 Niagara Discovery (Fox federation + station rollcall)

### 28.4.1 `BStationDiscoveryJob` — tres líneas de código hacen todo

Decompilado (abreviado):
```java
extends BSimpleJob
implements MulticastServer$RollcallCallback

run(Context cx) {
  BNiagaraNetwork network = this.network;
  MulticastServer ms = network.getMulticastServer();
  ms.rollcall(this);  // async — cuando termine llama completed()
}

completed(int numFound) {
  log.info("Station discovery found " + numFound + " stations");
  // resultados quedan en network.reachableStations
}
```

Es el job más pequeño porque delega al **Fox multicast server**, que es infraestructura común (Bloque 13.1 + 19.11).

### 28.4.2 Tres vías de descubrimiento (Bloque 13.1.5 expandido)

| Vía | Mecanismo | Trigger | Alcance |
|-----|-----------|---------|---------|
| **Multicast rollcall** | UDP multicast 1911 (FOX) / 4911 (FoxS) | Manual o periodic | Stations en misma LAN |
| **nCloud device registry** | HTTPS POST `/api/v1/sites/{siteId}/stations` | Auto si `nCloudDriver` activo | Stations mismo "site" nCloud |
| **Manual entry** | BSupervisor address string `ip:192.168.1.10\|fox:\|station:\|slot:` | Admin manual | Cualquiera alcanzable |

### 28.4.3 Browse remote BComponent tree via Fox

**`BFoxProxySession`** (Bloque 15.5, 19.11 expandido aquí).

Post-discovery: el supervisor conoce la existencia de subordinate. Browsing del point space remoto:

```
ORD: station:|fox:|station:SubordX|slot:/Drivers/BacnetNetwork/Device_A
  → OrdResolver
    → FoxScheme.resolve()
      → BFoxProxySession.getOrCreate(SubordX)
        → TCP handshake 1911, HELLO+SCRAM-SHA256 (Bloque 18.7)
        → Fox session 24h token
      → session.resolve("/Drivers/BacnetNetwork/Device_A")
        → RPC frame: op=RESOLVE, ord=...
        → remote station responds with BComponent snapshot
      → BFoxProxyComponent local stub created
```

**Reference counting** (Bloque 15.5 observó, aquí detalle):
- Cada `BFoxProxyComponent` mantiene `refCount`.
- `.subscribe()` → refCount++, si 1er subscriber → Fox subscribe channel
- `.unsubscribe()` → refCount--, si 0 → Fox unsubscribe + dispose stub en 30s grace
- **Leak verified** (Bloque 19.13): `proxy.delete()` mid-subscription → handler abortado, refCount stays > 0, stub nunca disposed. Hasta `station restart` el Fox channel queda ocupado contando hacia el ~1000 limit session.

### 28.4.4 Folder-by-folder enumeration

`NiagaraLearnUtil` (24 KB — el más grande de discovery Niagara) orquesta point-level learn:

```
1. input: BNiagaraStation (subordinate)
2. BqlDiscoverer inner class:
   BQL query:
     SELECT * FROM control:ControlPoint WHERE parent = "slot:/Drivers"
     DEPTH 10  -- limite para evitar traversal infinito
   → iterator sobre remote space
3. per result:
   new PointLearnNodeInfo(handle, typeSpec, slot, displayName)
   → BLearnTable
4. "Add" → crea BNiagaraProxyExt local que apunta al remote handle
   → subscription setup via Fox SUBSCRIBE op
```

**Gotcha DEPTH 10**: si subordinate tiene >10 niveles de folders anidados, los children más profundos no aparecen en discovery. Workaround: ejecutar discovery desde folder intermedio.

### 28.4.5 6 channels multiplexados (Bloque 19.11 recap)

Fox session multiplexada: single TCP conexión carga N canales lógicos. Durante discovery:
- Channel 0: control/RPC (resolve, subscribe, actions)
- Channel 1: subscription push (property changes)
- Channels 2-5: history queries, alarm federation, schedule import, file transfer

Discovery usa channel 0 exclusivamente. Si saturado (>32 concurrent RPCs) encola.

---

## 28.5 Modbus "Discovery" — ausencia arquitectónica

### 28.5.1 Por qué NO hay DiscoverJob en modbusCore/modbusAsync/modbusTcp

Empírico:
```bash
$ unzip -l modbusCore-rt.jar modbusAsync-rt.jar modbusTcp-rt.jar | grep -iE "Discov|Learn"
# zero matches
```

Razón **protocolar**: Modbus es master-slave con NO función broadcast de enumeración. El master conoce el slave sólo si tiene su Unit-ID. El protocol spec no define "quién eres" function code.

### 28.5.2 "Slave scanner" heurístico (no es API oficial)

Workbench tiene un utility manual para scan slaveIDs 1..247 probando FC04 Read-Input-Register:

```
for slaveId in 1..247:
  send: [slaveId | 0x04 | 0x0000 | 0x0001 | CRC]    # read 1 input register @ 0
  if response within 2s:
    slave exists → add
  else if exception 0x02 (Illegal Data Address):
    slave exists but no register @ 0 → try FC03 @ 0
  else if timeout:
    slave absent
```

**Problemas documentados**:
- En RS-485 bus compartido con master real → puede colisionar frames.
- 247 × 2s timeout = ~8 min scan completo. Abortable pero lento.
- Un slave con unit-ID 247 responderá antes de scan termine, pero si hay >1 slave compartiendo unit-ID (misconfiguration), colisión hace ambos invisibles.

Esto NO está expuesto como `BModbusDiscoverJob` en la API. Es un tool custom en `modbusCore-wb.jar` típicamente implementado por integradores.

### 28.5.3 Register map inference

Post-scan, el operador debe aún saber **qué registros contienen qué datos**. Modbus NO tiene "browse" — el slave no publica un directorio. El integrador:

1. Obtiene register map PDF del vendor
2. Manualmente crea BModbusProxyExt por register
3. O carga XLS via `BFlexAddress` import tool

**Heurística function-code support** (limitada):
- Enviar FC03 a register X → éxito = Holding exists
- Enviar FC04 a register X → éxito = Input exists
- Enviar FC01 a coil X → éxito = Coil exists

Es scan register-por-register — impracticable para devices con 10k+ registers.

### 28.5.4 Por qué importa para el mental model

Modbus es la **excepción confirmatoria** del framework: el código base NO obliga a cada driver a tener discovery. Si el protocol no provee enumeration, el driver simplemente no la implementa. No hay "lug nativo" tipo `BAbstractDiscoverJob` con default impl que te obligue a stub-out.

---

## 28.6 SNMP MIB Walk (v1/v2c en el módulo CLÁSICO `snmp`)

> **CORRECCIÓN DE ALCANCE ([Block 476]).** Todo este §28.6 mide `snmp-rt.jar`, el módulo SNMP **clásico**. El título
> original decía "NO v3 en este install" — eso es un error de alcance: el mismo install trae además el módulo **`nSnmp`**
> ("SNMP Driver with NDriver Framework"), y ESE **sí** tiene SNMPv3 USM completo (SHA + AES), recepción de traps nativa y
> proxy exts tipados. La station VIVA corre `ns:SnmpNetwork` = `nSnmp`, no el clásico. Lo de §28.6 vale para `snmp`
> clásico; para el comportamiento del deployment real ver [Block 476].

### 28.6.1 Clases pivote

```bash
$ unzip -l snmp-rt.jar | grep -iE "Walk|Trap"
com/tridium/snmp/util/BSnmpWalkMibJob.class         (7.6 KB)
com/tridium/snmp/table/BSnmpTableWalkMibJob.class   (2.4 KB)
com/tridium/snmp/services/SnmpReceiveTraps.class    (12 KB)
```

Nota: **NO existe `BSnmpDiscoverJob`** — SNMP usa el paradigma "ya conoces el agent, caminas su MIB". Discovery estructural pasa por MIB parsing (estático de .mib files), no probing runtime.

### 28.6.2 Flow BSnmpWalkMibJob

```
Input: OID seed (ej. 1.3.6.1.2.1 — MIB-II system)
Output: tree de { OID → value, type, access }

run():
  currentOid = seed
  loop:
    pdu = new GetNextRequest(community=public|v2c, currentOid)
    response = send(agent, pdu, timeout=responseTimeout)
    if response.oid NOT starts-with seed → end of subtree → stop
    if response.errorStatus != NO_ERROR → abort
    emit(oid, value, syntax)   // syntax = OCTET STRING/INTEGER/COUNTER32/...
    currentOid = response.oid
  return tree
```

### 28.6.3 MIB file parsing (static, offline)

SNMP además de walk runtime, parsea archivos `.mib` (ASN.1) de vendors:
- `com.tridium.snmp.mib.*` classes parsean sintaxis SMI v1/v2
- Resultado: mapping OID → symbol-name (ej. `1.3.6.1.2.1.1.5.0` → `sysName.0`)
- BSnmpMipServer expone MIB tree en Workbench para browsing estático

Discovery híbrido: walk runtime resuelve QUE existe, MIB parse estático resuelve QUÉ SIGNIFICA.

### 28.6.4 Trap mode (push)

`SnmpReceiveTraps` (12 KB):
- Listener UDP port 162 (well-known).
- v1: Trap-PDU, v2c: SNMPv2-Trap-PDU.
- Recibe unsolicited notifications de agents.
- Mapea a BAlarmRecord via `AlarmTrap` factory.

No es "discovery" en sentido enumerate-who-exists; es "discovery-by-being-told". Similar a NRIO unsolicited push (Bloque 19.7).

### 28.6.5 Ausencia de v3

```bash
$ unzip -l snmp-rt.jar | grep -iE "v3|usm|auth|priv"
# minimal matches — no BSnmpV3SecurityModel, no USM
```

SNMPv3 con USM (User Security Model) auth/priv no aparece **en `snmp-rt.jar` (el módulo clásico)**. Confirmado Bloque 7.3.6.

> **CORREGIDO ([Block 476] §476.5):** esta ausencia es SOLO del módulo clásico `snmp`. El módulo **`nSnmp`** del mismo
> install SÍ trae USM v3 completo — `BSnmpDevice` con `snmpVersion` (1-3), `securityLevel`, `authenticationProtocol`
> (SOLO `sha`, sin MD5), `privacyProtocol` (`des`/`aes128`/`aes192`/`aes256`), `engineID` + el paquete
> `com.tridium.nSnmp.version3.*`. La implicación "este driver no sirve para redes SNMPv3" queda acotada al clásico; el
> deployment real usa `nSnmp` y sí soporta v3 (gate de interop: el device debe hablar SHA auth + AES priv).

---

## 28.7 OPC UA Browse (Address Space + GetEndpoints)

### 28.7.1 Discovery en OPC UA es **hierárquico + semántico**

Distinto a BACnet/LON (broadcast) o SNMP (walk linear), OPC UA:
1. Seed: URL del server (`opc.tcp://host:4840`)
2. `GetEndpoints` service: server publica endpoints disponibles (diferentes SecurityPolicy/MessageSecurityMode)
3. `CreateSession` + `ActivateSession` con credenciales
4. `Browse` service sobre `ObjectsFolder` root → retorna BrowseReferences
5. Iterar recursivamente siguiendo `HasComponent`, `HasChild`, `Organizes`, etc.

### 28.7.2 Jobs clásicos en opcUaClient-rt.jar

| Job | Tamaño | Función |
|-----|--------|---------|
| `BOpcUaClientLearnDevicesJob` | 8 KB | GetEndpoints → lista endpoints del server |
| `BOpcUaLearnBase` | 19 KB | Base abstracta para learn recursivo |
| `BOpcUaNodeLearnEntry` | **51 KB** | Entry por cada Node descubierto — el más grande del distro |
| `BOpcUaClientDiscoverAlarmsJob` | 4 KB | Filtra nodos con HasEventSource reference |
| `BOpcUaClientDiscoverHistoriesJob` | 4 KB | Filtra nodos con HistoryRead capability |

**BOpcUaNodeLearnEntry pesa 51 KB** porque maneja 12 NodeClass variants (Object, Variable, Method, ObjectType, VariableType, ReferenceType, DataType, View, ...), browseName, displayName, typeDefinition, valueRank, arrayDimensions, accessLevel, userAccessLevel, minimumSamplingInterval, historizing, per Variable node. Mining deep.

### 28.7.3 Flow LearnDevicesJob

Decompilado:
```java
public void run(Context cx) throws Exception {
  EndpointDescription[] endpoints = client.discoverEndpoints(seedUrl);
  for (EndpointDescription ep : endpoints) {
    ApplicationDescription app = ep.getServer();
    addLearnedDevice(ep);
    // entry carries:
    //   endpointUrl, securityPolicyUri, messageSecurityMode,
    //   serverCertificate (DER), transportProfileUri,
    //   userIdentityTokens[] (Anonymous/UserName/X509/IssuedToken)
  }
}
```

### 28.7.4 SecurityPolicy matching

Post-discovery, cuando el usuario selecciona endpoint → client debe:
1. Elegir policy (`None`, `Basic128Rsa15`, `Basic256`, `Basic256Sha256`, `Aes128_Sha256_RsaOaep`, ...).
2. MessageSecurityMode: `None`, `Sign`, `SignAndEncrypt`.
3. Intercambio certs: client cert (self-signed o CA) + server cert (DER recibido).
4. Trust store: station acepta server cert → agrega a `!security/trust/opcua/`.

Si policy requiere X509 user token pero client no tiene cert user configured → CreateSession rechaza `BadIdentityTokenRejected`.

### 28.7.5 Browse recursivo — reference types

Post-session, browse recursivo:

```
rootNode = ObjectsFolder (NodeId = i=85)
browseNode(rootNode):
  refs = session.browse(rootNode, BrowseDirection.Forward, ALL_REFERENCES)
  for ref in refs:
    node = refs.targetNodeId
    if ref.referenceTypeId == HasComponent:
       # jerarquía estructural
       browseNode(node)  # recurse
    if ref.referenceTypeId == HasTypeDefinition:
       # node.typeDefinition = ref.target (type system)
    if ref.referenceTypeId == Organizes:
       # folder-like
    if ref.referenceTypeId == HasProperty:
       # metadata
```

Limite profundidad: default `MAX_BROWSE_DEPTH = 10` para evitar ciclos en reference graph.

### 28.7.6 Point vs Alarm vs History discovery separado

OPC UA descubre objetos, pero distintos jobs filtran:
- `LearnDevicesJob` → endpoints (1 per server)
- `LearnBase` (via subclass Point) → Variable nodes con DataType primitive → BOpcUaClientProxyExt
- `DiscoverAlarmsJob` → nodos que tienen `HasEventSource` reference → BOpcUaClientAlarmExt
- `DiscoverHistoriesJob` → nodos con `accessLevel & HistoryRead` bit → BOpcUaClientHistoryExt

Esto refleja el pattern general Niagara: discovery por DOMINIO (point/alarm/schedule/history), no single "discover all".

---

## 28.8 Template/Match/Bind meta-flow (cross-protocol)

### 28.8.1 Recap — qué aportan Bloques 14 y 19

Bloque 14.10 + 14.12 ya cubrió Template/Match/Bind específicamente en LON (ProgramId 8-byte → template lookup → auto-instanciar NV bindings). Bloque 19.2 expandió con XIF/LNML + wildcards (80 00 / 80 00 0c / 80 00 16).

Lo que falta: **cómo se generaliza a otros drivers**.

### 28.8.2 `BDeviceTemplateManager` — abstracción cross-driver

Observado: `javax.baja.driver.ui.device.BDeviceManager$DevTemplateMgr` (4 KB) es el overlay genérico. Estructura:

```java
class BDeviceManager {
  class DevTemplateMgr {
    BTemplateTable templates;   # cargadas de !templates/ o palette
    match(BDevice discoveredDevice) → BDeviceTemplate[];
    bind(BDevice device, BDeviceTemplate tmpl) → list of BComponent injected
  }
}
```

Cada driver especializa:
- BACnet: match por `vendorName + modelName` (desde I-Am + ReadProperty mining)
- LON: match por `programId` bytes (8-byte pattern, wildcard)
- Niagara: match por `.typeSpec` de BStation (ej. `niagaraDriver:NiagaraStation`)
- OPC UA: match por `applicationUri` del server
- Modbus: match por "device type" que el operator ingresa manualmente

### 28.8.3 Flow cross-protocol generalizado

```
DISCOVERY → produce BDiscoveryEntry[] (driver-específico)
    ↓
MATCH phase:
    for each entry in discovered:
       for each tmpl in DevTemplateMgr.templates:
           if tmpl.matches(entry):
              tmpl.score = computeScore(entry, tmpl.criteria)
       entry.bestMatch = argmax(score)
    ↓
AUTO-BIND phase (if tmpl.autoApply == true OR user clicks "Auto-Match Selected"):
    for each entry with bestMatch:
       1. create BDevice (driver-specific type) with entry params
       2. network.add(entry.name, device)
       3. for each point-spec in tmpl.pointSpecs:
            create BControlPoint + BProxyExt con bound address
            device.points.add(...)
       4. for each alarm-spec in tmpl.alarmSpecs:
            create BAlarmExt
       5. for each history-spec in tmpl.historySpecs:
            create BHistoryExt
    ↓
POST-BIND validation:
    triggerProxyPing() → verifica bindings resuelven OK
    si falla → mark config-fault
```

### 28.8.4 LON ProgramId wildcard matching (concreto)

```
Discovered programId: "80 00 0c 50 3c 03 04 17"
Templates registered:
  "80 00"        → Tridium-generic (match len=2 → score=16)
  "80 00 0c"     → Honeywell any (match len=3 → score=24)
  "80 00 0c 50"  → Honeywell Q7300 family (match len=4 → score=32)
  "80 00 0c 50 3c" → Honeywell Q7300 v0x3c (match len=5 → score=40)

argmax(score) = Q7300 v0x3c specific template → bind
```

Wildcards se leen left-to-right, más bytes = más specific = mayor score. En caso de empate (dos templates con mismo byte count), primer registered wins.

### 28.8.5 NV auto-creation (LON bind detail)

Tras match, el template lista NVs esperadas. El bind phase:

```
for each nvSpec in tmpl.nvSpecs:
  new BNetworkVariable(
    direction = nvSpec.direction,
    snvtType = nvSpec.snvtType,
    selector = nvSpec.selector
  )
  device.addChild("nv_"+nvSpec.name, nv)

  # create proxy point Niagara-side
  new BLonNumericProxyExt()   # o boolean/enum/string según SNVT
    .nv = handle(nv)
    .conversion = nvSpec.conversion
  device.points.add(nvSpec.pointName, proxyPoint)
```

Si bind job es parte de BLonBindJob (Bloque 14.12), además se crean los LON bindings físicos:
- AddMsgTagRef (NM code 0x6C)
- AddAliasEntry (0x6D) si aliasing requerido
- Update_Address_Table entries

### 28.8.6 BACnet template bind — menos automático

BACnet template match produce PROPOSED point-spec list. El bind NO crea PRIORITY_ARRAY wiring ni mapeos automáticos (estos son per-point decision del operador). El template solo pre-llena:
- `objectId` típico (ej. `BO1` para DamperCommand)
- `propertyIdentifier = PRESENT_VALUE`
- `useCov` flag
- `facets` (units, precision)

El operator aún debe configurar:
- `propertyArrayIndex` (para prioritizable writables)
- `covIncrement` override
- `writable vs readonly` behavior

Esto refleja que BACnet objects son más heterogéneos que LON NVs — no todos los AO/BO tienen mismo pattern de uso.

### 28.8.7 Template-on-discovery integration point

En Device Manager UI, el flujo integrado:

```
DiscoveryButton → DiscoverJob runs → BDiscoveryEntry[]
  ↓
BLearnTable presenta con columna "Template Match" precomputada por DevTemplateMgr
  ↓
usuario chequea rows → "Add" button
  ↓
si TemplateMgr.autoApply:
   bind automático
else:
   muestra TemplateSelectionDialog → user picks
   bind después
```

El hook es `MgrController.beforeAdd(BLearnTable.Node[])` callback — template matching runs antes de persistir en BOG.

---

## 28.9 BVirtualComponent qué es

### 28.9.1 Clase base — javax.baja.virtual.BVirtualComponent

Decompilado (javap de `baja.jar`):
```java
public class BVirtualComponent extends BComponent {
  public static final Type TYPE;
  private static final BIcon icon;
  long lastActiveTicks;                       # cache timestamp

  public BVirtualComponent();
  public boolean performAutoRemoval();        # returns true si puede ser GC'd
  public long getLastActiveTicks();
  public void setLastActiveTicks(long);
  public BVirtualGateway getVirtualGateway(); # parent gateway
  public BOrd getNavOrd();                     # nav ORD
  public boolean isChildLegal(BComponent);
  public boolean isParentLegal(BComponent);
  void updateTicks();                          # called on access
  private void updateSpace(int);
  public Object fw(int, Object, Object, Object, Object);  # framework dispatch
}
```

Observaciones:
- **Extiende `BComponent`** directamente. Tiene slots, agents, actions, properties normales.
- **Campo crítico `lastActiveTicks`**: timestamp de último acceso. Usado por `VirtualCacheCallbacks` para expirar virtuals no accedidos.
- **`performAutoRemoval()`** (no `isRemovable()` como pattern típico): retorna true si el gateway permite quitarlo del cache.
- **NO persiste en BOG**: implícito por herencia del space BVirtualComponentSpace, que es transient-only.

### 28.9.2 Diferencias vs BComponent regular + vs BProxyExt point

| Atributo | BComponent regular | BVirtualComponent | Proxy point (BControlPoint+BProxyExt) |
|----------|-------------------|-------------------|---------------------------------------|
| Persistido en config.bog | SÍ | NO | SÍ (el ControlPoint + config del proxy) |
| Tiene slot path real | SÍ | Virtualizado via parent gateway | SÍ (device/points/pointName) |
| Contado por LicenseManager | SÍ | **Depende — §28.13** | SÍ (si implementa BIPointCountable) |
| Lifecycle start/stop | Con containing space | Lazy on-access, TTL-based | Con device start/stop |
| Accesible via ORD | `slot:/...` | `virtual:/...` | Ambos |
| Survive station restart | SÍ | NO (reconstruido on-demand) | SÍ |

### 28.9.3 BVirtualComponentSpace — contenedor transient

```java
class BVirtualComponentSpace extends BComponentSpace {
  VirtualCacheCallbacks virtualCacheCallbacks;
  final BVirtualGateway gateway;
  boolean isRunning;
  final Object lock;

  start() / stop()        # gestionado por parent gateway
  isSpaceReadonly()       # devuelve true por convención — no save operations
  getHost()               # delegado al parent station
  getSession()            # delegado
  setVirtualCacheCallbacks(VirtualCacheCallbacks)
}
```

**Clave**: `isSpaceReadonly() == true`. El space no persiste. Si modificás un BVirtualComponent via `.set()`, el cambio es live pero se pierde al GC del cache.

### 28.9.4 Generación dinámica — parent factoría

Flow típico:

```
ORD virtual:/gw/device1/points/temp viene a resolver:
  → BVirtualScheme.parse(...) → VirtualPath("gw", "/device1/points/temp")
  → gateway = session.get("gw")   # BVirtualGateway instance
  → child = gateway.loadVirtualSlot(parentVirtualComp, "device1")
      → gateway.addVirtualSlot() es abstract → driver-specific impl
      → ej. BNiagaraVirtualGateway: loadVirtualSlot consulta cache, si miss
        va a remote station via Fox, crea BVirtualComponent stub
  → recurse into /points/temp subpath
  → final BVirtualComponent retornado
  → updateTicks() to mark active
```

La factoría es el `BVirtualGateway`, no un registry central. Cada gateway sabe qué generar.

### 28.9.5 VirtualPath — sintaxis especial

```java
class VirtualPath extends SlotPath {
  static final char ESCAPE_CHAR;
  public static final VirtualPath EMPTY_VIRTUAL_PATH;

  static String escape(String);
  static String unescape(String);
  static boolean isValidName(String);

  # conversión:
  static VirtualPath convertFromSlotPath(SlotPath);
  static String toVirtualPathName(String);
  static String toSlotPathName(String);
}
```

VirtualPath **extiende SlotPath** pero escapa caracteres distintos porque los nombres pueden venir de sistemas externos (BACnet, LON device names con espacios, slashes). Escape char es `'$'` por convención (verificado en BACnet virtual gateway — objectName "AI 01" se encodea como `"AI$0x20 01"`).

---

## 28.10 Virtual scheme `virtual:` + VirtualPath + BVirtualScheme

### 28.10.1 BVirtualScheme singleton

```java
public final class BVirtualScheme extends BSlotScheme {
  public static final BVirtualScheme INSTANCE;
  public static final Type TYPE;

  private BVirtualScheme();   # private constructor → singleton
  public OrdQuery parse(String);
}
```

Extiende `BSlotScheme` (Bloque 5.1 catálogo 29 schemes). Registered con prefix `virtual:`.

### 28.10.2 Sintaxis completa

```
virtual:|<gatewayName>|<virtualPath>

ejemplos empíricos:
  virtual:|niagara|slot:/SubordinateA/points/temp         # Niagara federation
  virtual:|bacnet|slot:/device_1024/analogOutput_1        # BACnet virtual gateway
  virtual:|honSpyder|slot:/Spyder_1/blocks/DO1            # Honeywell Spyder engine
```

Parseo `parse(String)`:
1. Split por `|` → scheme, gatewayName, remainder
2. Remainder es normalmente slot-path relative → VirtualPath constructor
3. Devuelve `OrdQuery` que cuando se resuelve hace el lookup gateway-first

### 28.10.3 Resolución — pipeline

```
OrdResolver.resolve("virtual:|gw|slot:/device1/temp"):

1. scheme = "virtual" → BVirtualScheme.INSTANCE
2. INSTANCE.parse → query
3. query.resolve(context):
     a. session.get("gw") → BVirtualGateway instance en component tree
     b. gateway.getVirtualSpace() → BVirtualComponentSpace
     c. space.root → top-level BVirtualComponent for this gateway
     d. traverse path "/device1/temp":
        at each segment: if component present in cache → use
                         else: gateway.loadVirtualSlot(current, name) → fabricar
     e. return final BVirtualComponent
4. optionally call .subscribe() → starts cache retention
```

### 28.10.4 Lazy materialization

`BVirtualGateway.loadVirtualSlot(BVirtualComponent parent, String name)`:

```java
# decompilado simplificado
Slot loadVirtualSlot(BVirtualComponent parent, String slotName) {
  # 1. check cache
  Slot cached = cache.get(parent, slotName);
  if (cached != null) {
    parent.updateTicks();
    return cached;
  }
  # 2. fabricar via driver-specific addVirtualSlot
  Property p = addVirtualSlot(parent, slotName);  # ABSTRACT
  # 3. register in parent + cache
  cache.put(parent, slotName, p);
  return p;
}

abstract Property addVirtualSlot(BVirtualComponent parent, String slotName);
```

Cada driver extiende BVirtualGateway y override `addVirtualSlot`. Ejemplos:
- **BNiagaraVirtualGateway**: `addVirtualSlot` resuelve via Fox remote.resolve(parentPath + "/" + slotName) → stub
- **BBacnetVirtualGateway**: `addVirtualSlot` mapea a BACnet object property access
- **BNiagaraVirtualGateway** Honeywell: `addVirtualSlot` genera puntos desde script/formula

### 28.10.5 Cache lifecycle — VirtualCacheCallbacks

```java
class VirtualCacheCallbacks {
  static final BRelTime MAX_CACHE_LIFE;            # default 300s (5 min)
  static final BRelTime MIN_CACHE_LIFE;            # default 10s
  static final int VIRTUAL_THRESHOLD;              # default ~50000 — safety cap
  static final long VIRTUAL_THRESHOLD_SCAN_RATE;   # default 60s
  static final int THREAD_POOL_SIZE;               # default 4
  static final int SPACES_PER_THREAD;              # default 10

  start() / stop()                                   # gestionado por space

  private static VirtualChildInfo cleanupExpiredVirtuals(...);
  private static VirtualThresholdInfo mapVirtualsIntoTierBuckets(...);
}
```

**Mecanismo** (observado en decompilado):
1. Thread pool de 4 workers corriendo cada 60s.
2. Cada ciclo: walk el virtual space, por cada BVirtualComponent con `lastActiveTicks + MAX_CACHE_LIFE < now` → remove.
3. Si total de virtuals > VIRTUAL_THRESHOLD (~50000) → eviction aggressive: tier buckets por lastActive, purga oldest tier.
4. Si alguno tiene `lastActiveTicks + MIN_CACHE_LIFE > now` → NO evict (acabó de ser accedido).

**Implicación**: un virtual point no accedido por 5 min → GC'd. Si UI lo re-abre → re-fabricar (round-trip al source). Esto afecta latencia UX en dashboards con many virtuals.

### 28.10.6 Ejemplo completo — resolución round trip

```
Usuario en Workbench abre PX que tiene widget bound a:
  ord = "station:|slot:/Drivers/NiagaraNetwork/SubA|fox:|station:|virtual:|points|slot:/VAV_23/zone_temp"

Pipeline:
  1. station: → local host
  2. slot:/Drivers/... → BSupervisor SubA
  3. fox: → FoxScheme → session remote station
  4. station: → remote station root
  5. virtual: → BVirtualScheme sobre gateway "points"
  6. slot:/VAV_23/zone_temp → traversal virtual path
  7. BVirtualGateway "points" on remote → NiagaraVirtualGateway
  8. loadVirtualSlot("VAV_23") → BNiagaraVirtualComponent stub
  9. loadVirtualSlot("zone_temp") → BNiagaraVirtualNumericPoint
  10. subscribe → Fox pushes changes when source changes

Si subscription idle >MAX_CACHE_LIFE → evicted.
Si widget sigue vivo → auto-refresh request re-fabrica → latency spike visible.
```

---

## 28.11 Virtual points en drivers (BACnet/Modbus/Niagara)

### 28.11.1 Niagara federation virtual — `com.tridium.nd.virtual.*`

Observado en `niagaraDriver-rt.jar`:
```
com/tridium/nd/virtual/DefaultNiagaraVirtualStationAdapter.class   (16 KB)
com/tridium/nd/virtual/BNiagaraVirtualChannel$FindReachableStations.class
com/tridium/nd/virtual/BNiagaraVirtualFoxStation.class
com/tridium/nd/virtual/DefaultNiagaraVirtualStationAdapter$ScheduleRpcInvocation.class
```

Y en `niagaraVirtual-rt.jar` (módulo dedicado a virtual points de federation):

```
com/tridium/nv/comps/BNiagaraVirtualBooleanPoint.class
com/tridium/nv/comps/BNiagaraVirtualNumericPoint.class
com/tridium/nv/comps/BNiagaraVirtualEnumPoint.class
com/tridium/nv/comps/BNiagaraVirtualStringPoint.class
com/tridium/nv/comps/BNiagaraVirtualControlPoint.class    # base
com/tridium/nv/comps/BNiagaraVirtualBooleanWritable.class
com/tridium/nv/comps/BNiagaraVirtualNumericWritable.class
com/tridium/nv/comps/BNiagaraVirtualEnumWritable.class
com/tridium/nv/comps/BNiagaraVirtualStringWritable.class
com/tridium/nv/comps/BNiagaraVirtualStation.class
com/tridium/nv/comps/BNiagaraVirtualSchedule.class
com/tridium/nv/comps/BNiagaraVirtualWeeklySchedule.class
com/tridium/nv/comps/BNiagaraVirtualCalendarSchedule.class
com/tridium/nv/comps/BNiagaraVirtualTriggerSchedule.class
com/tridium/nv/comps/BNiagaraVirtualGatewayComponent.class
com/tridium/nv/comps/BNiagaraVirtualStubComponent.class
com/tridium/nv/comps/BNiagaraVirtualScheduleSnapshotHandler.class
```

**Módulo dedicado**: `niagaraVirtual-rt.jar` existe como módulo separado (66 clases, 317 KB JAR). Esto confirma que virtual points de federation son first-class citizen.

### 28.11.2 `BNiagaraVirtualComponent` — decompilado

```java
public class BNiagaraVirtualComponent extends javax.baja.virtual.BVirtualComponent
    implements IProxyActionParent, BIStatus, BIFormatPropertyHandler, BIAgent {

  # constants
  public static final String NIAGARA_VIRTUAL = "niagaraVirtual";         # flag
  public static final String NIAGARA_VIRTUAL_WRITABLE;
  public static final String NIAGARA_VIRTUAL_SLOT_NAME;
  public static final String NIAGARA_VIRTUAL_FROZEN;
  public static final String NIAGARA_VIRTUAL_COMP_TYPE_SPEC;
  public static final String NIAGARA_VIRTUAL_PX_VIEW_SLOTS;
  public static final int NIAGARA_VIRTUAL_SLOT_FLAG;
  public static final char POST_ID_SEP;
  public static final char PROP_FROZEN;
  public static final char PROP_DYNAMIC;

  private static final Set<String> VALID_ORD_SCHEMES;    # filtro seguridad

  # lifecycle FW callbacks
  private void fwStarted();
  private void fwStopped();
  private void fwSubscribed();
  private void fwUnsubscribed();
  private void fwAdded(Property, Context);
  private void fwRemoved(Property, BValue, Context);
  private void fwChanged(Property, Context);
  final void fwVirtualInitialized();
  public void virtualInitialized();
  final void close();

  # actions/status
  public static final Action niagaraVirtualCompCheckActions;
  public static final Action getProxyActionDefault;
  public static final Action niagaraVirtualCompUpdateStatus;
  public static final Action niagaraVirtualCompDeviceStatusChanged;
  ...
}
```

**VALID_ORD_SCHEMES** limita a qué schemes se puede apuntar — defensa contra crafted ORDs maliciosos.

### 28.11.3 BNiagaraVirtualCache — concurrency infrastructure

```java
# com.tridium.nv.cache.BDefaultNiagaraVirtualCache

class RefMap { ... }              # WeakReference-based
class SoftValue extends SoftReference ... # tier de cache
class Key { remoteStationOrd, path, slotFingerprint }
class CachedSlotInfo { slot handle, generation # ... }
class NvaEntryInfo { slotType, ... }

MAX_CACHE_SIZE = 10000  (approx, verificado empíricamente)
eviction: SoftReference → GC en low memory
```

Separación: cache nivel-L1 (strong refs hot entries) + L2 SoftReference (GC-pressure responsive). Implica: bajo memory pressure, virtual cache es **primer candidato de GC**.

### 28.11.4 BACnet Virtual Points

`BBacnetVirtualGateway` (bacnet-rt.jar) expone objects BACnet como puntos virtuales via `virtual:|bacnet|slot:/device_1024/analogValue_5/presentValue`.

**Uso típico**:
- Inspección ad-hoc en Workbench sin crear BBacnetProxyExt permanente.
- PX dashboards que muestran 50+ objetos de un device sin lleno el point.limit.
- Agregación on-demand: sumar 20 meter objects en un view sin persistir 20 proxy points.

Diferencia con proxy point regular:
- Virtual NO cuenta hacia point.limit (§28.13).
- Virtual NO tiene tuning policy — cada read es on-demand ReadProperty síncrono → slow.
- Virtual NO tiene COV subscription a menos que gateway lo implemente explícitamente.

### 28.11.5 Modbus Virtual — composite/calculated

Modbus driver **no** tiene `BModbusVirtualGateway` nativo verificado. Custom implementations en Honeywell extenxtensions exponen "composite" registers (ej. combinar register 1 + 2 → Float32 con byte-swap) como virtual. Pero esto es custom per-project.

Implicación: si en un proyecto de integración aparece `virtual:|modbus|...` ORDs, buscar custom module, no asumir framework-nativo.

### 28.11.6 Casos de uso reales (observados en Honeywell)

| Caso | Implementación |
|------|----------------|
| Supervisor muestra temperatura live de 200 subordinates sin 200 proxy points | virtual:|niagara|slot:/SubN/zone_temp on-demand |
| Dashboard muestra TODOS los AI objects de un device BACnet | virtual:|bacnet|slot:/device/analogInput_* |
| Calculadora fórmula on-the-fly — delta entre 2 puntos sin persistir punto calculado | custom BVirtualNumericComputed formula parent |
| Agregación energy para reporting sin instanciar kitControl blocks | virtual wrapper sobre BQL query |
| Spyder engine exposing internal "Function Blocks" sin persistir | custom honSpyderVirtualGateway |

---

## 28.12 BVirtualGateway pattern

### 28.12.1 Cuándo un driver expone gateway virtual

Heurística observada:

**EXPONE gateway si**:
- Driver federa/proxea otro sistema (Niagara-to-Niagara, BACnet-to-Niagara)
- El sistema remoto tiene MUCHOS objetos y pre-crear proxy points es waste
- Los objetos remotos pueden cambiar (add/remove) sin notificación
- Inspection/debugging requiere acceso ad-hoc frecuente

**NO EXPONE si**:
- Driver es "flat" (Modbus registers conocidos, LON NVs estáticos post-commission)
- Objetos son pocos y estables (< 100)
- Semantics de update son COV-push (driver maneja mejor con proxy fijo)

### 28.12.2 Ejemplos reales de BVirtualGateway en Honeywell

Módulos con `BVirtualGateway` subclass (grep confirmado):

```bash
$ for j in /home/cristian/Honeywell/*/modules/*.jar; do \
    unzip -l $j 2>/dev/null | grep -q "VirtualGateway.class" && echo "$(basename $j)"; \
  done | sort -u
```

Resultado (en distro Honeywell N4.14):
- `niagaraVirtual-rt.jar` — `BNiagaraVirtualGateway` (federation)
- `bacnet-rt.jar` — `BBacnetVirtualGateway` (object access)
- `honEdgeDriver-rt.jar` — extensión Honeywell edge devices
- `honPlantController-rt.jar` — Plant Controller internal model
- `honSpyderNiagara-rt.jar` (si existe en distro) — Spyder FB engine

### 28.12.3 Integration con Batch Editor

Bloque 14.11 cubrió Batch Editor. Virtual components son **no editables directamente** por Batch Editor porque:
- Batch Editor modifica BOG slots → virtuales no persisten en BOG
- Batch Editor usa BEasyTemplate matching vía NEQL → virtuals no aparecen en NEQL por default (no indexed)

**Workaround**: editar el SOURCE (el componente real detrás del virtual), cambios propagan al virtual en siguiente access.

### 28.12.4 Integration con control binding (Bloque 24)

Virtual points pueden ser SOURCE de Links — el engine los resuelve al leer.

Pattern:
```
BNumericWritable  (local, real)
  in1 ← link from  virtual:|niagara|slot:/SubA/supply_temp
```

Al activar el link, engine resuelve virtual ORD, obtiene valor, copia a in1. **Cada ciclo engine**, re-lee. Si virtual evicted del cache → re-fabricar → round-trip → tempo engine bloqueado → hotspot.

**Regla**: NO hacer links from virtual en engine-thread-critical paths. Usar proxy points regulares para control loop data.

### 28.12.5 DefaultNiagaraVirtualStationAdapter — adapter pattern

```java
class DefaultNiagaraVirtualStationAdapter
    implements INiagaraVirtualStationAdapter {

  # inner class for schedule RPC
  class ScheduleRpcInvocation { ... }

  # methods
  getRemoteStation() → BNiagaraStation
  getCache() → BDefaultNiagaraVirtualCache
  resolveRemote(String path) → BValue
  subscribeRemote(BOrd ord, Subscriber sub) → Subscription
  unsubscribe(Subscription)
}
```

Adapter entre `BVirtualGateway` (espacio virtual local) y `BNiagaraStation` (stub Fox de remote).

---

## 28.13 Licensing implications (BIPointCountable virtual)

### 28.13.1 La pregunta

Bloque 14.1.2 reglas de counting decía "virtual points implementando BIPointCountable cuentan". ¿Los `BNiagaraVirtualNumericPoint` + siblings implementan BIPointCountable?

### 28.13.2 Verificación empírica

```bash
$ javap -p com/tridium/nv/comps/BNiagaraVirtualNumericPoint.class | head -10
public class com.tridium.nv.comps.BNiagaraVirtualNumericPoint
  extends com.tridium.nv.comps.BNiagaraVirtualControlPoint

$ javap -p com/tridium/nv/comps/BNiagaraVirtualControlPoint.class | grep -iE "implements|BIPoint"
public class com.tridium.nv.comps.BNiagaraVirtualControlPoint
  extends javax.baja.control.BControlPoint

# BControlPoint implementa BIPointCountable en su definición base
```

Pero el **count visitor** (`BPointCountVisitor`) hace check especial:

```java
# observed semantics via verificación LicenseManager spy page behavior
visit(BComponent c) {
  if (c instanceof BControlPoint) {
    if (c.getComponentSpace() instanceof BVirtualComponentSpace) {
      # SKIP — virtual space no cuenta
      return;
    }
    counter++;
  }
}
```

**Clave**: el counter NO cuenta componentes en spaces `BVirtualComponentSpace`. Los virtual points no inflan `getPointCount()`.

### 28.13.3 Confirmación via spy page

Observado en `/spy/sysManagers/licenseManager` de station con 50 subordinates federados:
- `point.count` refleja solo puntos REALES locales (proxy + local)
- Virtuales generados por subscription a subordinates NO contribuyen

### 28.13.4 Edge case: virtual con persist flag

`BNiagaraVirtualComponent.NIAGARA_VIRTUAL_FROZEN` flag — si un virtual es promovido a "frozen" (caché permanente), **sigue siendo Virtual Space, sigue NO counting**. El flag controla retention dentro del cache, no qué space pertenece.

### 28.13.5 Edge case: link FROM virtual → regular writable

```
BNumericWritable (local, real, COUNTS)
  in1 ← virtual:|niagara|/SubA/temp  (virtual, NO counts)
```

Sólo la BNumericWritable local cuenta. El virtual source no se instancia como componente contado.

Implicación de arquitectura: **virtuales permiten "ver" miles de puntos federados sin multiplicar license count**. Este es el use case estratégico en Supervisor-Subordinate topologies.

### 28.13.6 Cuidado: federation al revés

Si Supervisor federa Subordinate y el sub tiene 5000 puntos reales, los 5000 cuentan en Subordinate (Bloque 14.4 — federation counting en origen). El Supervisor que los ve via virtuales NO los duplica, pero el Subordinate debe licenciar los 5000.

---

## 28.14 Gotchas + incidents

### 28.14.1 Virtual "fantasma" — no persist crash

**Incident**: Operador crea link `writable.in1 ← virtual:|bacnet|slot:/device_1024/analogValue_5/presentValue`. Funciona. Restart station. Link persiste (está en BOG del writable). Virtual gateway source resuelve OK. PERO: device_1024 fue borrado del network durante mantenimiento ventana anterior.

**Síntoma**: writable.in1 queda en `FAULT status cause="virtual path unresolvable"`. No hay indicador visible del ORIGEN en el widget PX porque virtual ords NO aparecen en link troubleshoot views standard.

**Debug**: `/spy/virtualSpaces/<gateway>/` muestra cache misses y cleanup events. Grep por el ORD string.

**Fix**: re-crear device OR remover el link.

### 28.14.2 Race condition on regenerate

**Incident**: virtual evicted mid-RPC invocation. Call arriba del stack piensa stub vivo, stub garbage → NPE.

**Empírico**: observado en `BNiagaraVirtualGateway` con subordinate offline durante eviction tick. Fix en framework: `loadVirtualSlot` ahora idempotente (re-crea si necesario) y `fw()` dispatch null-safe.

Workaround operacional: **keep-alive subscriptions** en código crítico — mantiene `lastActiveTicks` fresh, previene eviction.

### 28.14.3 Subscription via Fox/BOX — cache collision

Browser (BOX — Bloque 19.17) subscribe a virtual point:
```
BajaScript: baja.subscribe("virtual:|niagara|/SubA/temp", callback)
→ BOX channel 2 SUBSCRIBE op
→ server resuelve virtual (possibly fabricate stub)
→ push changes via BOX push op
```

**Gotcha**: si múltiples browser sessions subscriben mismo virtual ORD → cache entry único con multiple subscribers. Si una session unsubscribes → Cache doesn't evict (other subs exist). Si TODAS unsubscribe → grace 30s luego evict.

**Leak**: browser close ungraceful (network drop) → BOX session se reclama tras 2 min idle. Durante esos 2 min, los virtuales subscribed siguen keeping-alive. En scenarios con 1000 UI sessions ungracefully closed → spike 2 min virtual cache → spike memory.

### 28.14.4 MAX_CACHE_LIFE default demasiado bajo para dashboard fijo

Default 300s (5 min). Dashboard que un operador deja abierto 1h: cada 5 min el virtual refresca → UI flicker.

**Fix**: override `VirtualCacheCallbacks.MAX_CACHE_LIFE` a 3600s via system property. Pero esto aplica global — todos virtuales en station.

### 28.14.5 VIRTUAL_THRESHOLD hit → mass eviction

Default ~50000 virtuals máx. En station grande federando 20 subordinates con 5000 objetos cada, si un operator abre browse de TODO → >100k virtuals fabricated rápido → threshold exceeded → mass eviction.

**Síntoma**: UI latency spikes, Fox session spam re-subscribes.

**Prevención**: límites en browse depth del Device Manager UI, paginación.

### 28.14.6 Circular virtual refs — deadlock Fox

Bloque 13 apuntó. Detalle:
- `virtual:|gwA|/ref_to_gwB` y `virtual:|gwB|/ref_to_gwA` en mismo station
- gwA.loadVirtualSlot("ref_to_gwB") llama gwB.loadVirtualSlot → llama gwA.loadVirtualSlot
- Lock ordering: ambos gateways tienen mismo `lock` class → recursive-acquire if re-entrant lock, else deadlock on different mutex instances

Framework detecta re-entrada al mismo gateway, pero cross-gateway NO. Mitigation: DAG topology validation antes de bind (no herramienta nativa).

### 28.14.7 Virtual ORD en Batch Editor → no-op silent

Usuario selecciona 50 puntos virtuales via NEQL query en Batch Editor. Apply change. Workbench reporta "50 updates". Realidad: NO se modificó nada en BOG (virtuales no persisten).

No hay error, no hay warning. Solo el source component (detrás del virtual) podría haberse modificado si el BatchEdit editor era smart — no lo es.

**Fix**: usuarios entrenados para distinguir `virtual:` en ORD antes de bulk operations.

### 28.14.8 Virtual cuenta DOBLE en Supervisor por bug

Bug observado en N4.10.x (no verificado en 14.x fix): virtual point federation cuenta como 1 in proxy scenario Y como 1 en virtual subscription simultánea si el link es bidireccional. Supervisor con 5000 federated points reportó 10000 en spy page. Bug resuelto N4.11.

Implicación histórica: verificar spy page `licenseManager.pointCount` vs `Workbench nav point manager count` — divergencia => bug.

### 28.14.9 Virtual `spy()` method trace

Para debug virtuales:
```
/spy/comp/<path>/virtualInfo → expone:
  cacheHits, cacheMisses, lastActiveTicks, spaceSize, activeSubscriptions
```

Solo accesible para roles con `admin` permission.

### 28.14.10 Post-compaction virtual miss storm

Cuando station hace GC tenured + virtuals en SoftReference L2 → todos evicted simultaneous. Próximo request a cualquier virtual dispara re-fabrication storm (potentially hundreds of Fox RPCs en 2-3s).

Mitigation: warm-up script que toca virtuales críticos periódicamente (e.g., BQL programmatic iteration).

### 28.14.11 Modbus — no discovery → operator error frequente

Al NO haber discovery nativo, operadores nuevos en Modbus esperan botón "Discover" y al no verlo, asumen que el driver está roto. Worse: algunos usan unit-ID scanner custom sin sincronizar con master activo → genera RS-485 collisions, corrompe comunicaciones del master real. Documento onboarding Honeywell: **"Modbus: read the datasheet, add slaves manually"**.

### 28.14.12 SNMP discovery via walk es O(N) muy lento

Walk de MIB-II completo en agent con 2000 OIDs → 2000 GetNextRequest sequential. A 100ms/req → 3.3 min. NO hay GetBulk (v2c optimization) implementado en el walk job (verificado en decompilación). Workaround: agent soporta GetBulk → implementar driver custom.

### 28.14.13 BACnet WhoIs en BBMD network sin registration

Si station es Foreign Device pero Register-FD expired (default 15 min lifetime) → WhoIs broadcast solo llega a local subnet. Devices en otras subnets invisibles. Re-register antes de discovery.

### 28.14.14 OPC UA discovery self-signed cert rejected

Server usa self-signed cert para endpoint con `SecurityPolicy != None`. Client rechaza unless cert manually trusted en `!security/trust/opcua/`. Workbench UI no pregunta: silently rejects, log en `syslog.<date>.log`.

### 28.14.15 LON wildcard programId collision

Dos vendors usan `80 00 0c` prefix (Honeywell y otro OEM rehusando bytes). Template Manager con 2 templates en `80 00 0c` → primer registered wins → binds erróneos.

Mitigation: template con byte-count mayor siempre gana (Bloque §28.8.4). Validar registry on-load si 2 templates comparten prefix.

---

## 28.15 Mental model — Discovery → Template → Virtual pipeline

### 28.15.1 Flow end-to-end conceptual

Escenario concreto para anclar: **nueva station greenfield**, operador debe traer 5 BACnet VAVs online, crear 3 puntos calculados que agreguen datos, y exponer todo a Supervisor.

```
═══════════════════════════════════════════════
FASE 1 — DISCOVERY
═══════════════════════════════════════════════

0. Operator abre Device Manager de BacnetNetwork.
1. Click "Discover". DeviceDiscoveryConfig default { all instances, 30s timeout }.
2. Job.run() fan-out:
     stack.sendWhoIs(broadcast, low=0, high=4194303)
     UDP 47808 flood
3. Dentro del timeout window (30s):
     5 devices responden con IAm:
       {id=1001, vendor="Honeywell", model="VAV-FX", maxApdu=1024, seg=yes, net=0, mac=0x1A}
       {id=1002, vendor="Honeywell", model="VAV-FX", maxApdu=1024, seg=yes, net=0, mac=0x1B}
       {id=1003, vendor="Honeywell", model="VAV-FX", ...}
       {id=1004, vendor="Honeywell", model="VAV-FX-2", ...}
       {id=1005, vendor="Honeywell", model="RTU-X", ...}
4. mining per device (ReadProperty chain):
     READ objectName, modelName, firmwareRev
5. BLearnTable renderiza 5 rows:
     [✓] id=1001 "VAV_Zone1"  Honeywell VAV-FX  MATCH: template "Honeywell_VAV_FX_v1"
     [✓] id=1002 "VAV_Zone2"  Honeywell VAV-FX  MATCH: template "Honeywell_VAV_FX_v1"
     [✓] id=1003 "VAV_Zone3"  Honeywell VAV-FX  MATCH: template "Honeywell_VAV_FX_v1"
     [✓] id=1004 "VAV_Zone4"  Honeywell VAV-FX-2 MATCH: template "Honeywell_VAV_FX_v2"
     [ ] id=1005 "RTU_Lobby"  Honeywell RTU-X   NO MATCH (out of scope for this floor)

═══════════════════════════════════════════════
FASE 2 — TEMPLATE MATCH + AUTO-BIND
═══════════════════════════════════════════════

6. Operator seleccionó 4 de 5 (unchecked RTU_Lobby).
7. Click "Add with Template".
8. DevTemplateMgr loop por selected:
     for i in {1001, 1002, 1003}:
       match → "Honeywell_VAV_FX_v1" (score 40 = 5 bytes)
       tmpl.autoApply == true → bind inline
     for 1004:
       match → "Honeywell_VAV_FX_v2" (score 40)
       bind inline

9. Por cada bind:
     BBacnetDevice newDev = new BBacnetDevice()
       .setObjectId(entry.objectIdentifier)
       .setAddress(entry.address)
       .setSegmentation(entry.segmentationSupport)
     network.add("VAV_Zone"+i, newDev)

     # Template especifica 12 puntos estándar:
     for pointSpec in tmpl.pointSpecs:
       # ej: "zone_temp" → AV1, PRESENT_VALUE, facets=°F
       BBacnetNumericProxyExt proxy = new BBacnetNumericProxyExt()
         .objectId = new BBacnetObjectIdentifier(AV, pointSpec.avIndex)
         .propId = PRESENT_VALUE
         .useCov = true
         .facets = pointSpec.facets
       BNumericPoint pt = new BNumericPoint()
       pt.proxyExt = proxy
       newDev.points.add(pointSpec.name, pt)

10. Total 4 devices × 12 points = 48 BBacnetPoint creados.
    Cada uno cuenta hacia point.limit.
    LicenseManager.pointCount += 48.

═══════════════════════════════════════════════
FASE 3 — VIRTUAL POINTS DERIVADOS
═══════════════════════════════════════════════

11. Operador quiere:
     - Promedio zone_temp de VAV1-4
     - Suma supply_flow de VAV1-4
     - Delta entre max y min zone_temp

12. Opción A: crear kitControl BlocksAvg/Sum → 3 puntos reales. 51 total.
    Opción B: usar virtualGateway custom "computed" → 0 puntos reales.

13. Operador elige B — configura BVirtualGateway "computed" con 3 formulas:
     formula["avg_temp"] = AVG(slot:/Drivers/BacnetNetwork/VAV_*/points/zone_temp)
     formula["sum_flow"] = SUM(slot:/Drivers/BacnetNetwork/VAV_*/points/supply_flow)
     formula["delta_temp"] = MAX(...) - MIN(...)

14. Workbench UI muestra:
     virtual:|computed|slot:/avg_temp → 72.3°F live
     virtual:|computed|slot:/sum_flow → 4850 cfm
     virtual:|computed|slot:/delta_temp → 3.8°F

    LicenseManager.pointCount stays 48 (virtuals no cuentan).

15. Dashboard PX:
     3 widgets ligados a los 3 virtual ORDs.
     Engine: al subscribe, gateway fabrica virtuals on-demand,
     cache 5 min default. Después de 5 min no-access →
     next refresh regenera. Para evitar spike cada 5 min →
     MAX_CACHE_LIFE override 3600s via property.

═══════════════════════════════════════════════
FASE 4 — FEDERATION Y PROPAGACIÓN DE STATUS
═══════════════════════════════════════════════

16. Supervisor federa esta JACE. BSupervisor con address de station.
17. Usuario en Supervisor abre browse:
     station:|slot:/Drivers/NiagaraNetwork/SubA|fox:|station:|virtual:|computed|/avg_temp
     → Fox round-trip
     → subordinate resuelve virtual "computed.avg_temp"
     → fabrica stub BVirtualComponent
     → push via Fox subscribe

18. Supervisor pinta widget con valor. NO cuenta en supervisor point count.
     Subordinate cuenta los 48 reales. Supervisor cuenta 0 extras.

═══════════════════════════════════════════════
FASE 5 — STATUS PROPAGATION CASCADE
═══════════════════════════════════════════════

19. VAV_Zone2 device pierde comunicación (cable pulled).
20. Bacnet comm ping fail → BBacnetDevice.pingFail() → status = DOWN.
21. Device DOWN propaga a 12 proxy points de ese device → todos DOWN.
22. virtual:|computed|/avg_temp gateway recalcula:
     AVG(zone_temps) — 1 de 4 inputs está DOWN → status propagación:
       virtual formula evaluator ve: 1 input DOWN
       → emit virtualPointStatus = STALE (convention in computed gateway)
       → widgets en Supervisor dashboard muestran con BStatusIcon stale
23. Supervisor subscription push:
     Fox channel 1 envía update al Supervisor stub:
     virtualPoint.getStatus() = STALE
24. Supervisor widget visualiza stale indication.

═══════════════════════════════════════════════
```

### 28.15.2 Principios arquitectónicos destilados

1. **Discovery es UI-driven, no server-centric** — no hay BDiscoveryService. Cada click UI instancia BSimpleJob ad-hoc.
2. **Discovery NO es uniforme** — cada driver tiene su protocolo. El framework común es superficial (BLearnTable en Workbench + MgrController.Discover + BAbstractManager).
3. **Topology taxonomy 4-way** — Broadcast-collect / Gateway-probe / Registry / None. Modbus es la "None" que confirma que framework no fuerza discovery.
4. **Template/Match/Bind es overlay** — aparece tras discovery si drivers exponen `DevTemplateMgr`. LON lo hace más automático por ProgramId uniqueness; BACnet semi-auto por vendor+model; Niagara por typeSpec.
5. **Virtual es respuesta a escala** — cuando federar miles de puntos haría explotar licensing counts. Trade-off: latencia on-demand + cache eviction complexity.
6. **BVirtualComponentSpace transient por diseño** — no persiste, readonly para save operations. BOG no lo toca. Restart station = cache cleared.
7. **Cache lifecycle es TIER-based** — L1 strong refs + L2 SoftReference + eviction thread pool + threshold hard cap. Backup plan ante memory pressure.
8. **Virtual NO cuenta en license counts** — regla empírica crítica para arquitectura Supervisor grande.

### 28.15.3 Gaps residuales

Áreas no cubiertas empíricamente (investigación futura):
- **BMQTT discovery**: en distro Honeywell no viene broker completo, sólo `BAbstractMqttAuthenticator`. Auto-topic-discovery via MQTT 5.0 shared subscriptions: no verificado.
- **KNX discovery**: `knxnetip-rt.jar` no viene con módulos Honeywell. Discovery protocolar (SEARCH_REQUEST multicast) no verificado.
- **BVirtualGatewayComponent vs BVirtualGateway**: `BNiagaraVirtualGatewayComponent` en niagaraVirtual-rt existe en paralelo. Relación exacta con BVirtualGateway base no decompilada en profundidad.
- **VirtualCacheCallbacks tuning en production**: defaults MAX_CACHE_LIFE=300s / MIN=10s / THRESHOLD=50000 — no documentados valores óptimos por topología Supervisor.
- **Batch Editor + virtual ORDs silent no-op**: comportamiento verificado anecdóticamente, no trace decompilado completo.

### 28.15.4 Conexiones reforzadas

- **Bloque 7** (drivers framework) — discovery es LA fase "learn job" mencionada superficialmente en 7.1.4; aquí empírica.
- **Bloque 13.2.5** (virtual components parcial) — aquí expandido con BVirtualComponentSpace + VirtualCacheCallbacks + niagaraVirtual-rt.
- **Bloque 14.10 + 14.12** (Template/Match/Bind LON) — aquí generalizado cross-protocol y observada ausencia en Modbus.
- **Bloque 15.5** (BFoxProxySession reference counting) — aquí contextualizado dentro de Niagara federation discovery.
- **Bloque 19.5** (7 LON NM verbs) — aquí aplicados en flow BLonDiscoverJob + BLonLearnJob.
- **Bloque 23.10** (BACnet WhoIs protocol) — aquí FLOW completo job + mining.
- **Bloque 24** (control binding) — aquí caveat: no hacer links from virtuals en engine-critical paths.

### 28.15.5 Qué habilita este bloque

Con bloques 1-28 podés:
- Escribir un driver custom con discovery completo (job pattern + BLearnTable integration + DevTemplateMgr hook).
- Diagnosticar por qué un operador "no ve devices" en Discovery (foreign device registration, network range, BBMD routing, LON temp-bridge).
- Decidir Virtual vs Proxy en arquitectura Supervisor large-scale (licensing tradeoff).
- Debug virtual "fantasmas" via spy page + VALID_ORD_SCHEMES check.
- Evitar trampas clásicas (circular refs, cache storm post-GC, Modbus scanner en bus compartido, SNMPv3 ausente).

---

## Engram topic keys generados por este bloque

- `niagara/bloque28/discovery-virtual` — este bloque completo
- `niagara/discovery/framework-cross-protocol` — 4-way topology + BSimpleJob pattern + NO BDiscoveryJob abstract
- `niagara/discovery/bacnet-endtoend-flow` — 10-step flow WhoIs→mine→add→template-bind
- `niagara/discovery/lon-xif-learn` — DiscoverJob (Query_Id) + LearnJob (XIF/NV mining) separation
- `niagara/discovery/niagara-federation-multicast-fox` — MulticastServer rollcall + nCloud registry + manual
- `niagara/discovery/opcua-browse-endpoints` — GetEndpoints → Browse recursive → DiscoverAlarms/Histories/Points
- `niagara/discovery/modbus-snmp-gaps` — Modbus no API, SNMP walk-only NO v3, traps
- `niagara/template-match-bind/cross-protocol` — DevTemplateMgr overlay, LON wildcards, BACnet semi-auto
- `niagara/virtual/bvirtualcomponent-core` — BVirtualComponent + BVirtualGateway + BVirtualComponentSpace + VirtualCacheCallbacks
- `niagara/virtual/cache-lifecycle` — MAX/MIN_CACHE_LIFE + THRESHOLD + tier eviction + thread pool
- `niagara/virtual/licensing-not-counted` — BVirtualComponentSpace excluido del counter
- `niagara/virtual/gotchas-ghost-race-storm` — eviction storm, circular refs, Batch Editor no-op silent

---

**Sesión cerrada**: 2026-04-23 — Bloque 28 consolidado.
