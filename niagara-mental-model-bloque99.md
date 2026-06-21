# Bloque 99 — Driver IPC/CIPer Honeywell (controlador IPC 3036): `ipcCommBus` (N-driver embebido) + `ipcBaseDriver` (capa JNI `libciper.so`), deofuscado

> Investigación empírica del **driver del controlador Honeywell IPC 3036** (DDC de zona HVAC, VAV/unitary): el N-driver Niagara embebido `ipcCommBus` (269 java) que corre **dentro del propio controlador** y habla con su firmware por el protocolo propietario **PVID** vía JNI, sobre la capa de comunicación `ipcBaseDriver` (33 java, wrapper de `libciper.so`). **Primo arquitectónico directo del honPlantController ([Bloque 90])**: mismo N-driver, mismo managed switch RSTP, mismo `F1CommManager`, mismo OS embebido BSD/pfctl.
>
> 2 módulos: `ipcCommBus` (driver Niagara) + `ipcBaseDriver` (capa JNI). `IPC` = IP Controller; el `libciper.so` confirma el linaje **CIPer**.
>
> Fuentes: `organized/ipcCommBus/ipcCommBus-rt/vineflower/com/honeywell/ipccommbus/...` + `organized/ipcBaseDriver/.../com/honeywell/comm/...`.
> Método: 2 sub-agentes Explore + **verificación directa** de cada `extends`, el `loadLibrary`, el puerto del switch, el `RunShellCommand`/pfctl y los modelos. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (pipeline works, sensores, CWM, formato firmware) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 98]. Familia D del barrido. **`ipcCommBus` solo estaba mencionado de pasada (B81), no destilado** — 269 clases. Conecta [Bloque 90] (honPlantController, su primo de planta), [Bloque 88] (Sylk wall modules, que este aloja), [Bloque 100] (`ipcMigrator`, Spyder→IPC), [Bloque 75] (seguridad).

---

## 99.1 — Qué es: el controlador IPC 3036 + su capa JNI `[CERT]`

`ipcCommBus` es el **driver Niagara embebido del Honeywell IPC 3036** (controlador DDC de zona HVAC; modelos `ipc3036vav`/`ipc3036unitary`, verificado `BIPCDeviceModelEnum`/`LocalDeviceVAVModel3036.java`). El station Niagara **corre dentro del propio controlador** y se comunica con su firmware por el protocolo propietario **PVID** (Public Variable ID) vía **JNI** — no es BACnet/LON/Modbus.

La capa JNI es `ipcBaseDriver` (`com.honeywell.comm`, **Java puro, sin Baja** `[CERT]`): singleton `F1CommManager.INSTANCE` → `JNIRequest` (52+ native methods) → **`libciper.so`** (verificado: `loadLibrary` resuelve `"libciper.so".substring(3, len-3)` = `"ciper"`, truco de ofuscación de string). Solo opera en QNX/Linux; en otro OS queda offline `[CERT-a]`.

**Direccionamiento `[CERT-a]`**: PVID = entero 32-bit `[subsystem 8b][subrange 8b][index 16b]` (`PVIDUtils.makePVID`, verificado: platform PVIDs subsystem=0; I/O subsystem=1, subrange=3). TPID (Terminal Property ID) = otro packing 32-bit. Máx 16 dispositivos (addr 0-15, addr 0 = baseboard). El framing binario vive en el `.so` (no visible en Java); patrón Java `begin/add/end` que bloquea esperando respuesta.

---

## 99.2 — El driver N-driver `[CERT]`

Sobre el N-driver de Tridium (`com.tridium.ndriver`, como C-Bus/EnOcean del [Bloque 78]):

| Clase | `extends` verificado (archivo:línea) |
|-------|--------------------------------------|
| `BIPCNetwork` | `extends BNNetwork` (`BIPCNetwork.java:184`) |
| `BIPCDevice` | `extends BNDevice implements BISylkContainer, BIIOContainer, BIIPCPingable, IIOBoard` (`:338`) |
| `BIPCProxyExt` | `extends BNProxyExt implements BIIPCPoint` (`point/…:149`) |
| `BIPCPointDeviceExt` | `extends BNPointDeviceExt implements BINPollable` (`:75`) |
| `BApplicationFolder` | `extends BEventControlProgram extends BNPointFolder` (`:38`/`:39`) |

`BIPCDevice implements BISylkContainer` `[CERT]` → el controlador **aloja wall modules Sylk** (liga directa al [Bloque 88]). `BApplicationFolder implements IHoneywellExecutionBlock` → es el contenedor de la lógica DDC (un "DDC Engine Thread" ejecuta los function blocks `honfunctionblocks`).

---

## 99.3 — El pipeline PVID + el managed switch `[CERT]` + `[CERT-a]`

**network/works (35) `[CERT-a]`**: pipeline asíncrono de 3 etapas (`RequestMessageBuilderWorker` → `RequestHandlerWorker` → `HandleResponseWorker`), todos `IPCWork extends Runnable` con `buildRequestMessage/sendRequest/handleResponse`. Work items: batch/priority/transactional PVID read-write (JNI `(byte)15` read / `(byte)17` write / `(byte)19` writeLinear), device ping, download de config de punto, file/firmware, TPID write, descarga al wall module.

**network/switchport (24) — managed switch idéntico al [Bloque 90] `[CERT]`**: el IPC 3036 trae un switch Ethernet gestionado con RSTP. `BSwitchPortConfiguration`/`BRSTPConfiguration`/`BSwitchPortConfigDetails` (puertos físicos, velocidad, cable diagnostics, rol RSTP). `SwitchPortHandler` conecta a **`localhost:10000`** (verificado `SWITCH_PORT_SOCKET_PORT = 10000`) por NIO async TCP; protocolo header binario + body XML (parseado con XPath). Mismo patrón exacto que el honPlantController.

---

## 99.4 — Modelo de I/O + Conventional Wall Module `[CERT-a]`

**point/config (incl. modulatinginput, 22) `[CERT-a]`**: modelo de I/O del controlador. Base `BModulatingInputConfig extends BIOConfig` (sensorType, linearización, in/out low/high). Sensores Honeywell reales: C7400A (CO2), C7632A/B (temp/hum), H7655A (humedad), NTC20/Pt1000, custom resistivo/voltaje, pulse meter/totalizer/counter, linearización tabular. Otros: binary in/out, modulating output (4-20mA `BAnalogCurrentModulatingOutputConfig` / 0-10V), floating motor (3-point). Hardware base `DeviceModel3036`: 3 UI + 3 UIO + 6 DO + AO + flow sensors/actuators built-in; expansión vía `BExpansionIODeviceExt` (modelos expio3022h/expio9056h).

**builtin (12) `[CERT-a]`**: **no** son function blocks genéricos — es el driver del **Conventional Wall Module (CWM)**, un termostato Honeywell básico por bus SYLK (`BConventionalWallModule extends BComponent implements INotificationHandler`). Gestiona temp/humedad/ocupación/fan/HOA overrides; commissioning job + checksum propietario.

**Firmware OTA (de `ipcBaseDriver`) `[CERT-a]`**: archivos `.f1img` (`web-c3036-base`/`web-o9056-roc`/`web-o3022-roc`), sectores de 4096 B, **sector 0 enviado al final** (atomicidad anti-brick), CRC32 final, anti-downgrade configurable (30 días). `BSB` = Baseboard Switches (lee DIP switches del PVID `VID_DIP_SWITCHES`).

---

## 99.5 — Seguridad: RCE por JNI + control del firewall `[CERT]`

**[CRÍTICO CERT] Ejecución de shell vía JNI.** `F1CommManager.INSTANCE.RunShellCommand(...)` ejecuta comandos del OS directo desde Java (verificado `BNetworkPortSettings.java:308/315`: `pfctl_wrapper -d`/`-e`). Un usuario Niagara con escritura sobre el componente ejecuta comandos arbitrarios en el OS del controlador. `F1CommManager` también expone `ReadFromFile`/`WriteToFile` del filesystem por JNI sin sanitización Java.

**[ALTO CERT] Control del firewall desde N4.** `BNetworkPortSettings` escribe `/etc/pf.conf` + `/etc/niagara.pf.conf` (BSD Packet Filter) vía `F1CommManager.WriteToFile()` y recarga con `pfctl_wrapper` (interfaz `fec0` hardcodeada). Escritura al componente → abrir puertos arbitrarios o desactivar el firewall del controlador.

**[ALTO CERT-a] Sin autenticación en la frontera.** El socket `localhost:10000` (switch) no tiene handshake/credenciales/TLS. Las operaciones PVID son JNI directo sin auth por request: **la frontera de confianza es la JVM** — quien controla el station controla el firmware. Sin firma de firmware (CRC32, no criptográfica).

**[BAJO CERT-a] Licensing por marca.** `LicenseChecker` acepta `brandId` ∈ {Webs, WebsOpen, Trend} → activable con licencia Tridium si se manipula el brandId.

---

## 99.6 — Conexiones

- **[Bloque 90]** (honPlantController/BEATS ADV): **primo arquitectónico directo** — mismo N-driver, mismo switch RSTP `localhost:10000`, mismo `F1CommManager`/JNI, mismo BSD/pfctl. Diferencia: IPC 3036 = controller de **zona** (VAV/unitary); honPlantController = controller de **planta**.
- **[Bloque 88]** (Sylk wall modules): el IPC 3036 los aloja (`BIPCDevice implements BISylkContainer`; TR42/TR7X/TR75X + CWM convencional).
- Comparte librería (no código) con `honfunctionblocks` (DDC engine), `sylkdevice`, `versionmanager` — **no** importa honPlantController/clPanelBus/honIOBase.
- **[Bloque 100]** (`ipcMigrator`): puebla este stack desde Spyder. **[Bloque 75]** (seguridad): RCE por JNI + firewall.
