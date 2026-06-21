# Bloque 109 — `honImporter` (CARE → N4 vía formato **NIDEX**) + `honProjectExport` (N4 → Honeywell Cloud, JSON): los dos puentes de ingeniería de la station, deofuscados

> Investigación empírica de dos módulos OEM Honeywell de **intercambio de datos de ingeniería**, ambos build **feb-2025**: **`honImporter`** (`com.honeywell.honImporter`, 102 java; `module.xml`: *"Provide support to import data from CARE project"*, symbol `hi`, vendor honeywell `4.14.0.4.4.3`) y **`honProjectExport`** (`com.honeywell.honprojectexport`, 107 java; *"Modules to export the Niagara project data to Cloud"*, symbol `hpe`, vendor Honeywell `4.14.0.1.0.8`).
>
> **Matiz al [Bloque 32]**: ese bloque los agrupó como *"Import/export legacy Honeywell systems"*, sugiriendo un par simétrico. **No lo son** `[CERT]`: `honImporter` importa proyectos **CARE legacy** (vía el formato NIDEX); `honProjectExport` exporta la station **al cloud de Honeywell** (JSON). Distinto origen, distinto destino, **cero código compartido, no hay round-trip**.
>
> Fuentes: `organized/{honImporter,honProjectExport}/<m>-{rt,wb}/vineflower/com/honeywell/...` + `ConfigFiles/{FBSlotMap,LonImportTemplates}.xml`. Decompilación vineflower **limpia** (0 fallos).
> Método: 1 sub-agente Explore profundo + **verificación directa** del root NIDEX (`NiagaraExchangeFile` + "Care 10.08.07"), del `BCloudExportService`/`projectID`, del `FBSlotMap.xml`, y de los license-checks. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (estructura XML/JSON detallada, mapeo de entidades, conteos) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 108]. **Conecta fuerte**: [Bloque 25] (Migration Framework), [Bloque 92] (wizards Excel 10 LON) y [Bloque 100] (ipcMigrator) — otros caminos de migración; [Bloque 108]/[Bloque 86]/[Bloque 87] (los drivers/control destino del import); [Bloque 83]/[Bloque 85] (el cloud destino del export); [Bloque 32] (matiz); [Bloque 75] (seguridad).

---

## 109.1 — Qué son: dos puentes opuestos del ciclo de vida `[CERT]`

Ambos mueven la "ingeniería" de una station, pero en direcciones y formatos distintos:

| | **`honImporter`** | **`honProjectExport`** |
|---|---|---|
| Dirección | **CARE legacy → N4** (entrada) | **N4 → Honeywell Cloud** (salida) |
| Formato | **NIDEX** (`.nidex`, XML `<NiagaraExchangeFile>`) | **JSON** propietario (comprimido) |
| Propósito | migrar proyectos del software legacy CARE a una station N4 | comisionar / subir el modelo de la station al cloud |
| Deps | **40 módulos** (drivers + control + I/O OEM) | **solo `baja`** (cloud-native, minimal) |
| Entry | `BHonImportService extends BComponent implements BIService` `[CERT]` | `BCloudExportService` (BIService) + `BPrepareProjectDataJob extends BHonCloudJob` `[CERT]` |

> **CARE** = *Computer Aided Regulation Engineering*, el software legacy de Honeywell para programar los controladores **Excel 5000 / Excel Web / IRC** (línea pre-Niagara). `honImporter` es el camino oficial CARE→N4. **El export NO es para CARE** — es para la plataforma cloud de Honeywell (Sentience/Forge, [Bloque 83]/[Bloque 85]).

---

## 109.2 — `honImporter`: el formato NIDEX y el mapeo CARE → Niagara `[CERT]`

**NIDEX = Niagara Data Exchange.** No se parsea el formato binario de CARE directamente: **CARE 10.08.07+ exporta un archivo `.nidex`** (XML) y `honImporter` lo consume. Verificado: el importador valida `importFile.name().equals("NiagaraExchangeFile")` (root) `[CERT, BImportStation.java:50, NiagaraDataExchangeImporter.java:26]` y exige versión ≥ 2.6 con el mensaje *"Please use Care 10.08.07 or higher for generating import file."* `[CERT, BImportStation.java:75]`.

**Estructura NIDEX** `[CERT-a]`: `<NiagaraExchangeFile><Station><Drivers><Network NetworkType="BACnet|CBus|LonChannel">…</Network></Drivers><PanelBus>…</PanelBus></Station>`. Contiene puntos, schedules, control loops PID, macros HVAC (`<macro opcode>` + `<conn>` wiring), notification classes, NVs LON, módulos/terminales PanelBus.

**Mapeo de entidades** (motor `importer/`, 32 clases) `[CERT-a]`:
```
NIDEX                              → Niagara N4
<Network BACnet>                  → BBacnetNetwork / BBacnetAws/OwsNetwork
<Network CBus>                    → BCBusNetwork          ([Bloque 108])
<Network LonChannel>              → BLonNetwork / BLonIpNetwork
<PanelBus>                        → BPanelbusNetwork      ([Bloque 86])
<Point> analog/digital/multistate → BNumericWritable / BBooleanWritable / BEnumWritable (kitControl)
<Schedule>                        → BWeeklySchedule / BSpecialEventSchedule
<NotificationClass>              → BHonBacnetNotificationClass (honBACnetUtilities)
<macro opcode> + FBSlotMap.xml    → programa de function blocks HVAC
<NV>                              → componentes LON (LonImportTemplates.xml)
```

**`FBSlotMap.xml`** `[CERT]`: mapea los **opcodes de macro de CARE → clases de function block Niagara** por número de tipo: `type="1"→BAnd`, `type="2"→BOr`, `type="5"→BAddition`… (31 tipos, con sus slots in/out nombrados). Es la tabla que reconstruye la lógica de control de CARE como wiresheet Niagara. `LonImportTemplates.xml` hace lo análogo para los NVs por tipo de device LON (ej. `Xl10Hyd2`).

**El paquete `wrapper/`** (27 clases, todas `BImportWrapper extends BComponent`) `[CERT-a]`: es un **árbol de preview en Workbench** que muestra el diff (add/update/delete/keep) entre el NIDEX (source) y la station existente (target) **antes** de importar, con selección por nodo. Cada nodo lleva `objectUID` (UID del elemento CARE), `actionType` y `handleRef`. El mapa UID→handle se persiste en la station (slot `UIDHandleMap`).

**2 modos** `[CERT-a]`: `importProject` (supervisor — toda la station) y *controller replacement* (`setControllerReplacement(true)` — un solo controlador por nombre, para reemplazo de hardware).

> **Por qué depende de 40 módulos** `[CERT]`: `honImporter` es el **integrador de migración** — para reconstruir un proyecto CARE necesita poder instanciar todos los tipos destino: `clCBus` ([Bloque 108]), `clPanelBus`/`clOnboardIO` ([Bloque 86]), `clHVAC` ([Bloque 87]), `lonHoneywell`, `honBACnetUtilities`, `honIOBase` ([Bloque 104]), `bacnetAws/Ows`. Es el módulo que **ata toda la familia OEM destilada en esta capa**.

---

## 109.3 — `honProjectExport`: la station → JSON al cloud `[CERT]`

NO exporta a CARE ni a NIDEX. Produce un **JSON propietario para el cloud de Honeywell** `[CERT]`:
```json
[{ "brandID":1, "projectID":"<stationGuid>", "projectName":"<station>",
   "addedObjects":[ {"EntityType":"Element",...}, {"EntityType":"Terminal",...} ] }]
```
`BPrepareProjectDataJob extends BHonCloudJob` `[CERT, :48]`; `projectID = exportService.getStationGuid()` `[CERT, :80]`. Escribe `<stationHome>/ProjectData/<station>.json` y lo comprime (CompressToGo nivel 5, o `Deflater` zlib) a un `BBlob` `[CERT-a]`.

**Modelo exportado** `[CERT-a]`: **Element** (device: CONTROLLER=1/IO_BOARD=2/SENSOR=3, con modelId/serial/timezone/parentElementId/customAttributes) + **Terminal** (UIO=1/UI=2/UO=3/AI=4/AO=5/DI=6/DO=7/CO=8, con signalType Analog/Digital/Multistate/Totalizer y dirección). Hay variante específica para PanelBus (`version=3`).

**Selección por interfaz** `[CERT]`: solo exporta componentes que implementen `BIHonProjectExport` — `BQL select * from honProjectExport:IHonProjectExport` sobre `slot:/Drivers`. Cada device exportable provee `getDeviceData()` / `isCloudExportSupported()` / `resetDeviceGuid()`. Es el contrato que los drivers OEM implementan para ser "cloud-exportables". `BResetProjectGuidJob` regenera los GUIDs (re-onboarding). Dependencia **mínima** (`baja@4.14`) → es código cloud-native moderno, desacoplado de los drivers.

---

## 109.4 — Calidad / seguridad `[CERT]` + `[CERT-a]`

- **License gating (honImporter) `[CERT]`**: feature `Honeywell/hbsEurMirrorExtension` ("Mirror Points", graceful — devuelve false sin licencia, `BHonImportService.java:1141`) + feature `Tridium/brand` (`Helper.java:3857`, lanza `FeatureNotLicensedException` si falta). Carga `BMirrorExt` por reflexión desde `hbsEurMirrorExtension` si está licenciado.
- **Path traversal (bajo) `[CERT-a]`**: `honImporter` crea el fichero local con `FilePath("^"+sFileName)` (prefijo `^`=station home, pero `sFileName` viene de un arg de action sin sanitizar `../`). `honProjectExport` escribe `<station>.json` usando `getStationDisplayName()` sin sanitizar. Riesgo real bajo (inputs de administrador), pero defecto de calidad. El borrado del temp file sí valida que el parent sea exactamente `^` (correcto).
- **XML parsing `[CERT-a]`**: usa `javax.baja.xml.XParser` (parser interno de Niagara), no `DocumentBuilderFactory`/`SAXParser` — sin configuración XXE visible en el código Honeywell; el endurecimiento depende de la implementación de XParser (framework, no decompilable aquí).
- **Limpio en lo grave `[CERT-a]`**: sin `Runtime.exec()`, sin `loadLibrary`/JNI, sin `MessageDigest`/crypto, sin credenciales hardcodeadas, sin `ZipFile`. `honProjectExport` empaqueta Gson y una lib propietaria `CompressToGo` (solo serializa datos propios). Ambos firmados (`KEY_6888.RSA` honImporter / `SERVER1.RSA` honProjectExport).
- **Permiso de red honImporter `[CERT-a]`**: declara `NETWORK_COMMUNICATION *:*` con comentario "communicate with Micros Server" — sin socket directo visible en el código; probablemente para notificación de progreso (`BHonImportNotification`).

---

## 109.5 — Conexiones

- **[Bloque 25]** (Migration Framework) / **[Bloque 92]** (wizards Excel 10 LON) / **[Bloque 100]** (ipcMigrator Spyder XL10→IPC): los **otros caminos de migración**. `honImporter` es el camino **CARE legacy → N4** vía NIDEX; complementa (no solapa) a ipcMigrator (Spyder→IPC) y a los wizards LON.
- **[Bloque 108]** (clCBus) / **[Bloque 86]** (PanelBus/OnboardIO) / **[Bloque 87]** (clHVAC) / **[Bloque 104]** (honIOBase): los **tipos destino** que `honImporter` instancia al reconstruir el proyecto — por eso depende de toda la familia OEM. `FBSlotMap.xml` reconstruye los macros HVAC de CARE como function blocks.
- **[Bloque 83]/[Bloque 85]** (cloud Honeywell / model sync): el **destino del export**. `honProjectExport` (N4→Cloud JSON) es un canal de subida hermano del model-sync ([Bloque 85], hon:→JSON-LD→Azure) — ambos suben "la station" al cloud, pero con esquemas distintos (este = Element/Terminal de comisionado; B85 = grafo semántico hon:).
- **[Bloque 32]** (matiz): no son un par import/export simétrico — import CARE legacy vs export cloud.
- **[Bloque 75]** (seguridad): aporta los license-gates, el path-traversal de bajo riesgo y la dependencia de XParser para XXE.
