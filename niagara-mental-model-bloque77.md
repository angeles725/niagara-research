# Bloque 77 — Drivers Honeywell Spyder (BACnet + LON): arquitectura interna deofuscada

> Investigación empírica de los **drivers Honeywell Spyder** (`honeywellBacnetSpyder`, `honeywellLonSpyder`, `honeywellSpyderTool`/`honeywellXL10NextGen`) a partir del código **deofuscado** en esta línea de trabajo.
> Estos módulos venían ofuscados con **ZKM (Zelix KlassMaster)**: string encryption + name-mangling. Las **strings fueron descifradas** (`deobfuscator-patched.jar` rev2 + CFR, ~53k strings en los 3 módulos); los **nombres de clases internas siguen ofuscados** (`a`/`b`/`c`) — ZKM destruye los nombres y son irrecuperables. Las clases device, slots, interfaces y mensajes operacionales SÍ son legibles.
>
> Relevancia: soporte directo al comisionamiento de controladores **Spyder Model 5/7** (BACnet MS/TP sobre RS-485) a stations N4 en JACE.
>
> Fuentes (decompilado deofuscado):
> - `/home/cristian/modules/Prototipos/modulos/organized/honeywellBacnetSpyder/honeywellBacnetSpyder/vineflower/`
> - `/home/cristian/modules/Prototipos/modulos/organized/honeywellLonSpyder/honeywellLonSpyder/vineflower/`
> - `/home/cristian/modules/Prototipos/modulos/organized/honeywellSpyderTool/honeywellSpyderTool/vineflower/`
>
> Método: sub-agente de extracción + **verificación directa** de los hechos estructurales (declaraciones de clase, slots, strings) contra el código. `[CERT]` = verificado verbatim en esta sesión; `[INFER]` = deducción de flujo no confirmada por ejecución.
>
> Conexión: este bloque ATERRIZA el [Bloque 7 — Drivers Framework](niagara-mental-model-bloque7.md) sobre dos drivers OEM reales. Complementa engram `architecture/spyder-bacnet-driver-hereda-address-de-pila-tridium`.

---

## 77.1 — Jerarquía device: dos drivers, dos bases distintas `[CERT]`

Verificado en la declaración de clase real (no en el javadoc del header):

**BACnet Spyder** — `bacnetSpyder/xl10Controller/device/BBacnetSpyder.java:415`
```java
public class BBacnetSpyder
extends BBacnetDevice                       // javax.baja.bacnet.BBacnetDevice
implements IDeviceMode, ISpyderDevice, ISpyderDownload, ISpyderLicense,
           ISpyderUpload, IOnlineNetworkInterfaceHandler, ISpyderCompile,
           ISpyderBoac, ISpyderValidate, IBacnetBindable {
```

**LON Spyder** — `lonSpyder/xl10Controller/device/BLonSpyder.java:365`
```java
public class BLonSpyder
extends BDynamicDevice                      // javax.baja.lonworks.BDynamicDevice (NO BLonDevice)
implements IDeviceMode, ISpyderDevice, IOnlineNetworkInterfaceHandler,
           ISpyderDownload, ISpyderUpload, ISpyderCompile, ISpyderLicense, ISpyderValidate {
```

**Hallazgos clave:**
1. El device BACnet es un **`BBacnetDevice` estándar de Tridium** — por eso hereda toda la pila BACnet nativa (address MAC MS/TP + Device Instance independientes; ver engram Spyder). El módulo NO reimplementa el transporte BACnet, lo recibe del framework.
2. El device LON extiende **`BDynamicDevice`** (slots dinámicos), no `BLonDevice` directo — modela el controlador como un device de NVs construidas dinámicamente.
3. La lógica común Spyder vive en un set de **interfaces `ISpyder*`** que ambos comparten: `IDeviceMode`, `ISpyderDevice`, `ISpyderDownload`, `ISpyderUpload`, `ISpyderCompile`, `ISpyderLicense`, `ISpyderValidate`, `IOnlineNetworkInterfaceHandler`. El BACnet añade `ISpyderBoac` + `IBacnetBindable`. **Esto es el contrato real del controlador Spyder**, agnóstico del bus.

**Slots frozen comunes** (verificados, ambas clases): `revisionFlag`, `controlNvRamFlag`, `niChangeFlag`, `compileStatusFlag`; LON añade `appModifiedFlag`, `controllerModel` (`BLonSpyder.java:415+`). Son banderas de estado del ciclo compilar→descargar.

---

## 77.2 — Modelo BACnet: 7 tipos de objeto + binding por proxy `[CERT]`

Tipos de objeto BACnet que el driver expone/valida (string set verbatim, `bacnetSpyder/.../BacnetBindableUtil.java`):
```
"AnalogInput"  "AnalogOutput"  "AnalogValue"
"BinaryInput"  "BinaryOutput"  "BinaryValue"
"MultistateValue"
```

- El binding de un punto se hace por **`ObjectName + presentValue`** o **`ObjectName + priority[N]`** (binding a un nivel del priority array). `[CERT-agente]` `BacnetBindableUtil.java:176-181`.
- Proxy extensions: `BBacnetProxyExt` / `BBacnetNumericProxyExt` (mapeo punto Niagara ↔ objeto del controlador). `[INFER]` consistente con [Bloque 7] ProxyExt pipeline.
- Datasharing: jobs `BBacnetDataSharingJob`, `BBacnetLearnJob` + configs `BPollMapConfiguration` / `BPushMapConfiguration` `[CERT-agente]` `BBacnetSpyder.java:186-223` — el driver soporta **poll-map y push-map** (dos modelos de transferencia de datos entre controlador y station).

> Para el comisionador: los puntos del Spyder aparecen como objetos BACnet nativos (AV/BV/MSV/AI/BI/AO/BO). El priority array de 16 niveles aplica igual que cualquier device BACnet (ver [Bloque 6] priority array + [Bloque 7] mapping).

---

## 77.3 — Modelo LON: SNVT + NVI/NCI/NVO + COV opcional `[CERT]`

El LON Spyder modela los puntos como **Network Variables** tipadas con SNVT. Tipos SNVT presentes verbatim (`lonSpyder/.../vineflower`, muestra):
```
SNVT_area  SNVT_btu_f  SNVT_btu_kilo  SNVT_btu_mega  SNVT_count  SNVT_count_f
SNVT_count_inc_f  SNVT_density  SNVT_elec_kwh  SNVT_enthalpy  ...
```

- Clases de variable: `BXL10NextGenNvi` (entrada), `BXL10NextGenNci` (config), `BXL10NextGenNvo` (salida) `[CERT-agente]`.
- **COV (Change of Value) es opcional y configurable** — store dedicado `SylkCOV` con métodos `getSylkCOV()`/`writeSylkCOV(boolean)` (`BLonSpyderFileOffsetWriter.java:103-116`). `[CERT-agente]`
- Polling granular: contadores de recurso "Many To One NVIs", "GPU Refresh NVOs", "Unpolled NVOs", "COV" (`BLonResourceCounters.java`). `[CERT-agente]`
- Generación de **XIF** (XML Interface File) vía `BXIFGenerator` para la definición de NVs. `[INFER]`

---

## 77.4 — Comisionamiento: ciclo compilar → escribir archivo → checksum `[CERT]/[INFER]`

El flujo de bajada de lógica al controlador se deduce de los writers y flags:

1. **Compilar** `[INFER]`: `BBacnetSpyderCompilation` + `BCompilationJob` / `BSpyderCompileUtility` (`BBacnetSpyder.java:216,268`). Produce binario + checksum.
2. **Escribir archivo de proxy** `[CERT-agente]`:
   - BACnet: `BBacnetKFFileWriter.writeProxyFile(...)` con log `"KF Write - FullDownload flag - "` (`BBacnetKFFileWriter.java:67,99`); `BBacnetFileWriter.writeProperty(objectType, objectId, propertyId, byte[])` (`:212`).
   - LON: `BLonSpyderFileOffsetWriter.writeSylkCOV(boolean)` con log `"Writing Sylk COV store..."`.
3. **Retry logic** `[CERT-agente]`: `BSpyderDownloadUtility.addRetryDownloadProp()` (`BBacnetFileUtil.java:96,132`) — reintentos de descarga gestionados como propiedad.
4. **Restore** `[CERT-agente]`: `BBacnetDeviceRestorer extends BDeviceRestorer` con checking de versión (`this.compVer.compareTo(new Version("5.113"))`) — compatibilidad por versión de firmware.

**Bandera crítica `FB_NV_MODIFIED`** `[CERT]` — verificado verbatim:
```
"Setting the CompileStatus Flag to FB_NV_MODIFIED, since sDelta value of the NVO is reset to '0'"
"Setting ShortStackFlag to FB_NV_MODIFIED, since sDelta value of the NVO is reset to '0'"
```
> Lectura operacional: cuando una NVO se modifica (sDelta vuelve a 0), el `compileStatusFlag` pasa a `FB_NV_MODIFIED` → **hay que recompilar y volver a descargar** antes de que el cambio surta efecto. Es la causa típica de "cambié algo y no aplica".

---

## 77.5 — Errores y diagnóstico operacional (para el comisionador) `[CERT]`

Strings/lexicon keys reales que un comisionador verá (verificadas en `BBacnetSpyder` y vistas LON):

| Síntoma | String / key | Qué chequear |
|---------|--------------|--------------|
| Device caído | `"BacnetSpyder.DeviceOffline"` | Bus MS/TP (cableado RS-485, baud, MAC), Device Instance |
| Ping no operacional | `"BacnetDevice.ping.nonOperational"`, `".ping.nullMac"` | MAC MS/TP nula o device sin responder I-Am |
| Escritura de instance falló | `"BacnetSpyder.WriteDeviceInstanceFailed"` | Conflicto/permiso al fijar Device Instance |
| Error de descarga | `"BacnetSpyder.DownloadError"`, `"Download Failed"` | Reintentar; revisar checksum / proxy data |
| Comm error en alarma | `"AV_WMCommErrorDeviceA"`, `"AV_WMCommErrorFileID"` | Rango de ObjectId / file id inválido |
| Config inválida | `"BXL10NextGen.ConfigurationError"`, `".NvSize31ByteNotSupported"` | Tamaño NV / configuración no soportada |
| Auth | `"BXL10NextGen.DeviceAuth"`, `"Authentication mismatch error"` | Credencial/firma del device |
| Aprender enlaces | `"BacnetSpyder.LearnLinksErr"` | Re-ejecutar Learn (datasharing) |

LON-específico `[CERT-agente]`: `"NVO is in Polled state"`, conversión de modo `BNVManager.handleNviToNciConversion` / `handleNciToNviConversion` (cambiar el tipo de una NV dispara conversiones internas).

---

## 77.6 — Sylk / modelo de aplicación (XL10NextGen) `[CERT]/[INFER]`

`honeywellSpyderTool` (paquete `com.honeywell.honeywellXL10NextGen`) contiene el **modelo de aplicación** portable del controlador:

- **`BSylkDeviceFile`** (`sylk/fw/BSylkDeviceFile.java:22`): archivo portable del device — `descriptor` (BProxyFileDescriptor) + `fileData` (BBlob). `[CERT-agente]`
- **`BSylkVersionInfo`** (`:21`): `deviceType`, `osModelNumber`, `majorRevision`, `minorRevision`, `bugRevision`, `wmAddress` — usado para validar **compatibilidad de firmware** antes de descargar. `[CERT-agente]`
- **Sylk addressing** `[CERT]`: `getSylkAddress()`, `SylkUtility.getSylkTiInfoNameByIOComp(...)` — los puntos del bus Sylk se direccionan por componente IO.
- **Modelo de lógica** `[INFER]`: `BApplicationLogic` (raíz) → `BApplication` → `BMacro` (segmentos), navegables por `BIApplication`/`BIMacro.getListOfFunctionBlocks()`. La carpeta `BSylkTIFolder` agrupa la información técnica (TI) del device; slot `logicLoaded` indica si la lógica está cargada.
- **Kingfisher Tr4x** `[CERT-agente]`: tablas `BTr4xSensorsConfigTable`, `BTr4xLabelsTable`, `BTr4xUnitSetsTable`, `BTr4xFailDetectTable`, etc. (todas `extends BSylkDeviceFileSection`) — configuración de sensores/etiquetas/unidades de los controladores Tr4x.

> **Sylk** = bus propietario Honeywell (2-wire, polarity-insensitive) para sensores/actuadores del Spyder. El "Spyder Tool" modela ese mundo (devices Sylk, función blocks, TI) por encima del controlador físico BACnet/LON.

---

## 77.7 — Síntesis accionable

1. **El Spyder NO es un device exótico**: BACnet Spyder = `BBacnetDevice` estándar; sus puntos son objetos BACnet nativos (7 tipos) con priority array de 16 niveles. Todo lo del [Bloque 7] aplica.
2. **Ciclo de cambio = compilar + descargar**: cualquier modificación de NV/lógica setea `compileStatusFlag=FB_NV_MODIFIED`; sin recompilar+descargar, el cambio no aplica. Es el gotcha #1.
3. **Offline → bus físico**: `DeviceOffline`/`ping.nullMac` apuntan a MS/TP (RS-485, MAC, baud) o LON link, no al driver.
4. **Firmware version gating**: el restore compara versión (`Version("5.113")`); mismatch de firmware bloquea/altera la restauración.
5. **Sylk es otra capa**: el bus Sylk (sensores/actuadores Honeywell) vive bajo el controlador; su modelo (`BSylkDeviceFile` + función blocks Tr4x) es portable y versionado.

**Pendiente conocido**: nombres de clases internas ofuscados (`a`/`b`/`c`) — irreversible (ZKM). Las interfaces `ISpyder*`, slots y strings dan el contrato; la lógica fina de clases mangled requiere lectura caso por caso.
