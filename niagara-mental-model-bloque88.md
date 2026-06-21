# Bloque 88 — Capa de dispositivos de sala/terminal Honeywell: wall modules Sylk + config tool de PUC "BEATS" (protocolo Nano sobre BACnet) deofuscados

> Investigación empírica de la **capa de dispositivos de sala y controlador terminal** de Honeywell: los **wall modules del bus Sylk** (sensores/displays de habitación serie TR) y la **herramienta de ingeniería del controlador unitario programable (PUC) "BEATS"** que los hospeda y se programa con el protocolo propietario **Nano** sobre BACnet.
>
> 2 módulos: `honeywellSylkDevice` (modelo de 28 wall modules Sylk) y `honIrmConfig` (config tool del PUC BEATS — descubrimiento, comisionamiento, programación offline, Teach/Learn, simulación).
> Decompilados Honeywell limpios.
>
> Fuentes: `organized/{honeywellSylkDevice,honIrmConfig}/<m>-{rt,ux}/vineflower/com/honeywell/...` (+ `module.xml`, `*.lexicon`).
> Método: 2 sub-agentes + **verificación directa** de cada `extends`/`interface` con grep. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (protocolo Nano, PVIDs, flujo) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 87]. **Conecta fuerte**: [Bloque 77] (Sylk mencionado con Spyder; aquí el modelo completo), [Bloque 79] (BEATS = el mismo controlador "Beats" cuyo radio WiFi/BLE configura `honAdvWirelessCfg`), [Bloque 7] (BACnet device), [Bloque 87] (clHVACRoomControl interopera con controladores de sala).

---

## 88.1 — Los dos módulos + cómo se relacionan `[CERT]`

| Módulo | Clase raíz verificada | Rol |
|--------|-----------------------|-----|
| `honeywellSylkDevice` | `BSylkDevice extends BDeviceExt implements BISylkDevice` (:133, abstract) | modelo de wall modules del bus Sylk |
| `honIrmConfig` | `BIrmBacnetDevice extends BBacnetDevice implements BIHonBacnetDevice` (:591) | config tool del PUC BEATS |

**La relación clave `[CERT]`**: `honeywellSylkDevice` NO es un driver — los `BSylkDevice` viven como hijos de un **host** que implementa `BISylkContainer extends BIService` (:19). El **controlador BEATS es ese host**: `BIrmProgram extends BIrmFolder implements BIIrmProgram, BIAbstractSylkContainer` (:328) — es decir, `honIrmConfig` **consume** `honeywellSylkDevice` (lo declara como dependencia) y hospeda los wall modules Sylk dentro del programa del controlador. El bus Sylk físico corre en el **controlador**, no en el JACE; el JACE alcanza al controlador por BACnet y éste gestiona el bus Sylk autónomamente.

> Arquitectura de campo Honeywell completa (uniendo bloques): JACE/Supervisor → (BACnet) → **controlador BEATS** ([Bloque 88]) → (bus Sylk) → **wall modules TR** ([Bloque 88]) + (I/O onboard) → sensores/actuadores. El radio WiFi/BLE de ese BEATS lo configura `honAdvWirelessCfg` ([Bloque 79]).

---

## 88.2 — honeywellSylkDevice: el modelo de wall modules Sylk `[CERT]`

`BSylkDevice extends BDeviceExt implements BISylkDevice` (:133). Es la **librería de modelo de dispositivos** del bus Sylk (bus propietario de 2 hilos de Honeywell para periféricos de sala). Modela **28 variantes de hardware** `[CERT-a]` en familias:

- **TR40** (`BTR40SylkDevice extends BTR4XSylkDevice`): wall module básico — TR23H/TR40/TR40H/TR40CO2/TR40HCO2 (sufijo H=humedad, CO2=sensor CO2).
- **TR42** (`BTR42SylkDevice extends BTR4XSylkDevice`): mid-range con display, idioma, password, override de ocupación.
- **TR7X/TR75** (`BTR7XSylkDevice` → `BTR71XSylkDevice` → `BTR75XSylkDevice`): premium con display gráfico, home screen configurable; TR75 añade **schedule local embebido** (`BScheduleConfig`).
- **TR50** (`extends BTR40SylkDevice`): smart sensor con PM1/PM2.5/PM10/TVOC/AQI (calidad de aire), alimentación Sylk o 24 V externo.
- **C7400** (`BC7400SylkDevice extends BSylkDevice`): sensor CO2 standalone, direcciones 8-15.
- **Actuador** (`BSylkActuatorDevice extends BSylkDevice`): posición/ciclos/override/power report.
- **Emulación**: TR120/TR100 (hardware físico ejecutando firmware TR75/TR42).

**Protocolo de aplicación (PVID) `[CERT-a]`**: cada punto tiene `SYLK_PVID` (fijo por tipo, lado wall module) + `HOST_PVID` (lado controlador). PVIDs fijos: temperatura TR4X=512/TR7X=8192, humedad 513/8193, CO2 514/8194, PM2.5=516, TVOC=517, AQI=520. El `ProxyFileBuilder` compila todos los `BSylkDevice` a un **archivo binario** (CRC + send/group/fail-detect tables) que el controlador descarga al bus. Polling configurable (múltiplos de 5 s o COV) con `senDelta`.

**Modelo de datos `[CERT-a]`**: `BSylkParam` con jerarquía por dirección — sensores (solo salida wall→controlador), bidireccionales (`BNetworkSetpointParam`/`BFanCommand`/`BSystemCommand`: heatOnly/coolOnly/autoChangeover/heatPump), entrada (`BOccupancyStatus`), salida (`BOccupancyOverrideCommand`). Direccionamiento: TR4X 1-15, TR7X 1-10, C7400 8-15.

---

## 88.3 — honIrmConfig: config tool del PUC BEATS + protocolo Nano `[CERT]`

`BIrmBacnetDevice extends BBacnetDevice implements BIHonBacnetDevice` (:591). Symbol `irmn`, desc "Programmable Unitary Controller", build ene-2025.

### Corrigendum: NO es el XL10/IRM legacy `[CERT-a]`

A pesar del nombre "Irm", este módulo **NO gestiona los controladores XL10/IRM legacy** de Honeywell (los que `clHVACRoomControl` del [Bloque 87] menciona vía `ApplicModeXL10`/`ApplicModeIRM` son otra cosa). "IRM" es el **apodo interno del equipo de software** para la línea moderna de PUC **BEATS** (Building Edge Automation Trusted System) — modelos físicos **RS3N/RS4N/RL4N-RL8N** (FCU), **VA423/VA75xx** (VAV), **TC/SMB-IO**, con marcas hbs/trend/sbc/alerton/centraline/webs/smb (`BBrandEnum`). Verificado: interfaz `IBEATSDeviceModels`, imágenes `BEATS_*` en el `-ux`. **BEATS = el mismo controlador "Beats" del [Bloque 79]** (cuyo WiFi/BLE configura `honAdvWirelessCfg`).

### El protocolo Nano `[CERT-a]`

"Nano" es un **protocolo binario propietario de Honeywell tunelizado sobre BACnet**. El controlador expone objetos BACnet propietarios (tipo 512): un `interactivePipe` (inst. 512) y un `backgroundPipe` (inst. 513). La herramienta:
1. Escribe el comando Nano serializado (little-endian) en la propiedad 1024 del pipe vía `BACnetComm.writeProperty`.
2. Lee la respuesta en la propiedad 85 (present-value) en polling.

Header del comando: `[commandVersion | commandId | transactionId | responseCode]` + payload. Comandos: ECHO, GET_CHILDREN(_DETAILS), GET/SET_VALUES, CREATE_CHILD(REN), DELETE_CHILD, SET_LINK, SET_PROPERTIES, WRITE_FILE, SET_CONTROLLER_PASSWORD, etc.

**Tres protocol services `[CERT]`** (todos `implements BINanoProtocolService`):
- `BBacnetProtocolService extends BComponent implements BacnetConst, BIBacnetPollable, BIStatus, BINanoProtocolService` (:102) — habla con el **hardware real** (reintentos 3×10, buffer ≤1750 B).
- `BSimulationProtocolService extends BComponent implements BIStatus, BINanoProtocolService, Runnable` (:50) — **modo simulación sin hardware**: socket TCP a `localhost:47616`, framing magic `@Nano` + length. Permite el flujo completo contra un simulador software.
- Mutuamente excluyentes (`protocolServiceChanged`); el device crea uno solo al `started()`.

`BServicePinDevice extends BComponent` (:52) = data-holder de un controlador descubierto por Service Pin.

### Modelo de aplicación y flujo `[CERT-a]`

El programa de control es un **árbol de function blocks** propio (`BIrmProgram` → `BIrmFolder` [PERIODIC_DDC/EVENT_DDC/TERMINALS/ALARMS] → `BNanoFunctionBlock` con `BIrmParameter`/`BIOTerminalWithPin`/links). **Modelo de control totalmente propio — NO usa clHVAC ([Bloque 87]) ni Spyder/honBACnetUtilities.**

Ciclo de vida: (1) **Descubrimiento** por BACnet Private Transfer (vendorId=17 Honeywell, service=130 service-pin) → `BServicePinDevice`. (2) **Comisionamiento**: asigna BACnet ID (YouAre, service=124) + MAC/IP vía Nano. (3) **Programación offline** (sin hardware). (4) **Teach** (descarga): incremental (CREATE_CHILD/SET_LINK diffs) o **Full Application Teach** (serializa todo comprimido con CompressToGo → `BBacnetFile` tipo 10). (5) **Learn** (inverso: reconstruye el proyecto desde el controlador). (6) Sync continuo por CRC (`masterCrc`/`ioConfigDDCCRC`), swap-in/out para reemplazo de hardware.

**Seguridad `[CERT-a]`**: cifrado **AES** (`AesSymmetricCryptographer`) de la password del controlador (`apw`), usando el `serialNo` como vector, cuando el firmware lo soporta. Aporta al [Bloque 75]: el canal de programación (Teach/SET_CONTROLLER_PASSWORD) es la superficie de control del controlador de campo.

---

## 88.4 — Síntesis: la cadena de campo Honeywell completa

Con este bloque, la **cadena de control de campo Honeywell** queda mapeada de extremo a extremo, uniendo la Capa 22:

```
Supervisor/JACE
   │ BACnet (+ Nano protocol tunneled)
   ▼
Controlador BEATS (PUC)  ──[honIrmConfig, B88]── programación function blocks + Teach/Learn
   │  ├─ radio WiFi/BLE ──[honAdvWirelessCfg, B79]
   │  ├─ I/O onboard / PanelBus ──[clOnboardIO/clPanelBus, B86]
   │  └─ bus Sylk
   ▼
Wall modules TR (sensores/display de sala) ──[honeywellSylkDevice, B88]
```

Y en paralelo, el **otro linaje** de controlador de campo Honeywell: **Spyder** (BACnet/LON, [Bloque 77]) — que también hospeda Sylk (`BISylkContainer`). Así, los wall modules Sylk de este bloque sirven a **dos familias de controlador** (Spyder legacy + BEATS moderno).

**Lecturas clave**:
- **Sylk es modelo, no driver**: el bus vive en el controlador; N4 solo compila la config y la descarga. Para depurar un wall module, el problema puede estar en el controlador host, no en N4.
- **Nano sobre BACnet**: un protocolo propietario completo (CRUD de árbol de control) tunelizado en objetos BACnet tipo 512 — invisible a un analizador BACnet estándar salvo como writes a propiedades opacas.
- **Modo simulación** (`BSimulationProtocolService`, TCP localhost) permite ingeniería y pruebas sin hardware — útil y también un vector a considerar (un simulador local hablando el protocolo real).
- **BEATS unifica** el [Bloque 79] (radio), [Bloque 86] (I/O) y [Bloque 88] (programación + Sylk) en un solo controlador.

**Para MX60 / Honeywell**: si el site usa controladores BEATS, `honIrmConfig` es la herramienta de comisionamiento; el modelo de programación es propio (function blocks Nano), independiente de kitControl y clHVAC. Para sensores de sala, la serie TR (Sylk) es el catálogo; el TR75 con schedule embebido y los TR50 con calidad de aire (PM/TVOC/AQI) son los más capaces.

**Pendiente conocido**: el detalle binario exacto del protocolo Nano y de las tablas del proxy file Sylk se citó vía sub-agente `[CERT-a]` (PVIDs, comandos, framing), no se decompiló byte a byte. honIrmConfig tiene 541 java; se cubrió la arquitectura (device, protocol services, programa, flujo), no cada function block. Otros OEM Honeywell pendientes en el corpus: `honPlantControllerHMI` (liga [Bloque 32]), `lonhoneywellAXWizards` (wizards LON), `honPlantController`/`abstractMqttDriver`.
