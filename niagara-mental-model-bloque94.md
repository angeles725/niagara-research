# Bloque 94 — Familia Honeywell Device Manager (core agnóstico + BACnet + Modbus): firmware OTA a dispositivos de campo Honeywell, deofuscado

> Investigación empírica de la familia OEM Honeywell **Device Manager**: la capa que gestiona el **ciclo de vida de firmware** (OTA update) + comandos de configuración de los dispositivos de campo Honeywell (controladores unitary/plant, sensores, I/O boards). Arquitectura **agnóstica de protocolo**: un core común (`honeywellDeviceManager`) + dos implementaciones de transporte (`honeywellBacnetDeviceManager`, `honeywellModbusDeviceManager`).
>
> 3 módulos: `honeywellDeviceManager` (80 java, core), `honeywellBacnetDeviceManager` (15 java, BACnet), `honeywellModbusDeviceManager` (14 java, Modbus).
>
> Fuentes: `organized/honeywell{,Bacnet,Modbus}DeviceManager/<m>-rt/vineflower/com/honeywell/devicemanager/...`.
> Método: 2 sub-agentes Explore (core+Modbus, BACnet) + **verificación directa** de cada `extends`, el path-traversal del firmware, la construcción del File object BACnet (tipo 10 + atomicWriteFile), la `ModbusWriteFileRequest` (FC21) y la prop 372 de serial. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (formato de header firmware, flujo lock/unlock, registros, pool de IDs) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 93]. Hallado en el **barrido de verificación del corpus** (18 módulos OEM sin destilar; familia B "Smart Edge + Device Manager"). Conecta [Bloque 95] (Smart Edge devices, consumidores de este firmware), [Bloque 90] (honPlantController también gestiona firmware vía su propio canal), [Bloque 75] (seguridad).

---

## 94.1 — Arquitectura: core agnóstico + 2 transportes `[CERT]`

El core `honeywellDeviceManager` **no es un `BService`** sino un `BComponent` (`BHonDeviceConfig`) que se instala dentro de una `BDeviceNetwork`. Define la interfaz `BIFileHandler` (`writeFile`/`readFwFileDownloadStatus`/`sendFwFileHandCmd`) que abstrae todo el I/O con el dispositivo; cada protocolo la implementa. Clases raíz verificadas:

| Módulo | Clase | `extends` verificado (archivo:línea) |
|--------|-------|--------------------------------------|
| core | `BHonDeviceConfig` | `extends BComponent` (`commission/…:69`) |
| core | `BHonBaseJob` | `abstract extends BSimpleJob` (`job/…:15`) |
| core | `BFirmwareDownloadJob` / `BCommandJob` | `extends BHonBaseJob` (`job/…:46` / `:44`) |
| BACnet | `BHonBacnetDeviceConfig` | `extends BHonDeviceConfig` (`bacnet/commission/…:82`) |
| BACnet | `BHonBacnetFileHandler` | `extends BComponent implements BIFileHandler` (`bacnet/firmware/…:59`) |
| BACnet | `BHoneywellBacnetDeviceManager` | `extends BBacnetDeviceManager implements BIHonDeviceManager` (`bacnet/ui/…:44`) |
| Modbus | `BHonModbusDeviceConfig` | `extends BHonDeviceConfig` (`modbus/commission/…:49`) |

La dirección de memoria de cada operación se describe con `BFwMemoryStruct` = `BFwProtocolEnum` (bac/mod/slk) + `BMemoryLocationTypeEnum` + `address`. Un único `BMemoryLocationTypeEnum` mezcla valores BACnet (file/av/ai/bv…), Modbus (reg/coil) y SYLK (pv) `[CERT-a]` — por eso el mismo `BFirmwareDetails` describe ubicaciones en cualquier protocolo.

**Tipos de producto `[CERT-a]`** (`BHonProductType`): `UNITARY`(u, soporta firmware directo Y a periféricos vía el controlador como proxy), `PLANT`(p), `FIELD`(f, solo directo). Vendor BACnet `VENDOR_ID_HON = 17`.

---

## 94.2 — El flujo de firmware OTA `[CERT-a]` + `[CERT]`

1. Workbench envía `BFirmwareDetails` (binario en **Base64** dentro de un `BString`) a la acción `initiateFirmwareDownload` `[CERT-a]`.
2. `BFirmwareDownloadHandler` crea `{stationHome}/firmware/`, decodifica el Base64 y escribe en chunks de 5000 B (`Files.write APPEND`) `[CERT]`.
3. `FirmwareDownloadToEachDeviceTask` lee el archivo (`Files.readAllBytes`) y lo pasa al `BIFileHandler` del protocolo `[CERT-a]`.
4. Secuencia de comando al dispositivo `[CERT-a]`: `unlockFile`(0) → transferir binario → `lockFile`(1) → `update`(2, reboot/apply) → polling de status hasta salir de `fwFileUpdateInProgress`(3).

**Formato de header del firmware `[CERT-a]`** (`FirmwareHeaderParser`): headerLength, version, **CRC (4 B)**, fileSequence, fwUpdateRequired, firmwareFileLength, productTypeIdentifier ('U'/'P'/'F'), `deviceTag` (8 B), `mcuIdentifier` (4 B), `firmwareVersion` (11 B), + campos de identificación con prefijo de longitud. Alineado con familias TR/TC/CIPer.

**Transferencia por protocolo:**
- **BACnet `[CERT]`**: `BHonBacnetFileHandler.writeFile()` construye `BBacnetObjectIdentifier.make(10, address)` (tipo **10 = File**, `:219`), verifica `isServiceSupported("atomicWriteFile")` (`:223`) y transfiere por **`atomicWriteFileStream(address, fileObj, start, data)`** (`:363`) en chunks de `min(maxAPDU local, remoto) − 30` B `[CERT-a]`. Pre-paso: escribe la propiedad vendor 42 a 0 `[CERT-a]`.
- **Modbus `[CERT]`**: `BHonModbusFileHandler` usa **`ModbusWriteFileRequest`** (Function Code **21 = Write File Record**, `:312`) en chunks de hasta 220 B, gestionando `fileNumber`/`startRecNum` (límite 9900 registros/archivo) `[CERT-a]`.

---

## 94.3 — Comisionamiento BACnet: pool de instance IDs + I-Am `[CERT]` + `[CERT-a]`

`BHonBacnetDeviceConfig` gestiona un **pool de instance IDs BACnet libres** (rango def 5–9999, máx 0x3FFFFE) con anti-colisión de 3 capas `[CERT-a]`: IDs ya en la station + IDs del último WhoIs + consulta a hijos `BIHonBacnetConfig.isIdTaken()`. Pool `MAX 500`, recarga bajo `MIN 100` en background; si se agota, `getNextFreeInstanceId()` devuelve `null` con log INFO (sin error en UI).

**Lectura de serial number `[CERT]`**: `HonIAmListener` escucha I-Am (service 26) y por cada uno lee la **propiedad vendor 372** del device (`BAC_PROP_ID_SERIALNO = 372`, `readProperty(addr, objId, 372)`, `:93`/`:331`). Match en 2 modos `[CERT-a]`: `DATABASE` (reusa el ID del device existente) u `ONLINE` (respeta el ID del WhoIs). Discovery delega en `BDiscoveryDevice`/`BacnetDeviceLearn` de Tridium; Honeywell solo enriquece el post-proceso (`BHonDiscoveryDeviceEx` con serial). Modbus discovery: `BHonModbusDiscoverDevicesJob extends BSimpleJob` (scan dir 1..247, 250 ms/dispositivo; registros 1000=model, 1030=serial, 96=fwVersion, 114=fwStatus) `[CERT-a]`.

---

## 94.4 — Seguridad: firmware OTA sin garantías de integridad `[CERT]` + `[CERT-a]`

**[CRÍTICO CERT/CERT-a] Firmware sin validación de integridad ni firma.** El header trae un campo CRC (4 B) que se **lee y loguea pero nunca se valida** contra el payload; no hay hash, HMAC ni firma asimétrica en ningún punto del flujo (verificado por ausencia en core, BACnet y Modbus). Quien pueda inyectar un `BFirmwareDetails` flashea **binario arbitrario** a cualquier dispositivo Honeywell de la red.

**[ALTO CERT] Path traversal en la escritura de firmware.** `BFirmwareDownloadHandler.java:88-90`: `firmwarePath.resolve(fileName)` + `Files.write(..., APPEND)` con `fileName` tomado de `BFirmwareDetails.getFileName()` **sin sanitizar** → un `fileName` con `../` o ruta absoluta escribe fuera de `firmware/`.

**[ALTO CERT-a] `atomicWriteFile` / FC21 sin auth de capa.** La única precondición es `isServiceSupported`. Sobre BACnet/IP o Modbus sin BACnet/SC ni TLS, un atacante con acceso L3 envía AtomicWriteFile/WriteFileRecord directo al dispositivo, sin pasar por Niagara.

**[MEDIO CERT-a] Identidad de dispositivo auto-reportada.** `isHoneywellDevice()` confía sólo en `vendorId=17` + model name (no verificados criptográficamente en BACnet/IP). I-Am spoofing puede inyectar serials falsos (`getSerialNumber`) y agotar el pool de IDs. El serial (prop 372) se usa en `SlotPath` sin validación de longitud/caracteres → riesgo de injection en el namespace.

**[MEDIO CERT] Estado compartido frágil.** `BHonDeviceConfig.executor` es `static` y `initializeExecutorForGivenSize()` hace `shutdownNow()` si cambia el pool size → una red puede cancelar los threads en vuelo de otra. `BHonModbusDiscoverDevicesJob.modbusDiscoveryDevices` es `static` → discoveries concurrentes mezclan resultados.

**[BAJO CERT-a] Borrado destructivo en `finally`.** `BFirmwareDownloadJob` borra todo `{stationHome}/firmware/` en el `finally`, incluso si el job falla → impide reintentos sin re-enviar el binario y borra evidencia. `BHonBacnetFwHandler`/`BHonModbusFwHandler` son stubs `@Deprecated` vacíos presentes en el JAR.

---

## 94.5 — Conexiones

- **[Bloque 95]** (Smart Edge devices TR50 + Wall Module TR100): son **consumidores** de este firmware OTA; sus `BIHonBacnetDevice`/`BIHonModbusDevice` cuelgan de este device manager.
- **[Bloque 90]** (honPlantController): gestiona firmware por su propio canal (BTP/JNI); este es el camino genérico BACnet/Modbus.
- **[Bloque 75]** (seguridad): suma el caso "firmware OTA sin firma + path traversal + escritura BACnet/Modbus sin auth de capa".
