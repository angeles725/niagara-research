# Bloque 108 — `clCBus`: el driver del **CentraLine Controller Bus** (controladores XL vía adaptador BNA por TCP, protocolo SUSI + XCnap) — **2º corrigendum al Bloque 32**, deofuscado

> Investigación empírica del driver OEM Honeywell/CentraLine **`clCBus`** (`com.honeywell.cbus`, 360 clases top-level / 1252 java). `module.xml`: *"CentraLine clCBus Utilities"*, symbol `clCBus`, vendor Honeywell `4.14.0.162.208`, build **2024-08**. Driver Niagara para los **controladores de automatización de edificios CentraLine serie XL**, conectados a la estación vía un **adaptador IP "BNA" (Bus Network Adapter)** por TCP puerto **2499**.
>
> **CORRIGENDUM `[CERT]` al [Bloque 32]** (el segundo de esta tanda; el 1º fue `ascCommon` en [Bloque 107]): ese bloque clasificó `clCBus` como *"Clipsal CBus driver, lighting control Australia"*. Es **INCORRECTO**. No existe ninguna referencia a Clipsal, SAL, ni Group Address en el código. "C-Bus" aquí = **CentraLine Controller Bus** (bus propietario Honeywell de los controladores XL), no el Clipsal C-Bus australiano. La nomenclatura real del protocolo es **SUSI** (para hablar con la BNA) + **XCnap** (eXtended Controller Network Application Protocol, comandos al controlador).
>
> Fuentes: `organized/clCBus/clCBus-{rt,wb,ux}/vineflower/com/honeywell/cbus/...`. Decompilación vineflower **limpia** (1252 java, 0 fallos).
> Método: 1 sub-agente Explore profundo + **verificación directa** de la naturaleza CentraLine/BNA (no Clipsal), del puerto 2499, de los extends ndriver (`BNNetwork`/`BNDevice`/`BProxyExt`), del handshake SUSI/`SusiMsgLogin`, y del license-check doble. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (capas del protocolo, comandos, modelo de schedule, conteos) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 107]. **Conecta fuerte**: [Bloque 7] (Drivers Framework — `BProxyExt`), [Bloque 19] (drivers/`ndriver`/BOX), [Bloque 78]/[Bloque 86]/[Bloque 87] (familia CentraLine), [Bloque 32] (corrigendum), [Bloque 75] (seguridad).

---

## 108.1 — Qué es: driver del CentraLine Controller Bus vía BNA `[CERT]`

`clCBus` es el driver Niagara para los **controladores CentraLine serie XL** (línea legacy de automatización de edificios de Honeywell/CentraLine). El JACE **no habla el bus físico directamente**: se conecta por **TCP/IP a una BNA (Bus Network Adapter)** — un gateway hardware que traduce IP ↔ bus físico de controladores. Verificado: la red declara `bnaName` + `bnaIpAddress` con default `new BIpAddress("", 2499)` `[CERT, BCBusNetwork.java:115]`; el log del listener dice literalmente *"Received incoming msg from BNA"* `[CERT, comm/CBusListener.java:28]`.

La BNA soporta **2 canales C-Bus físicos** (`cbusChannel1Config`/`cbusChannel2Config`), cada uno con enable/baudrate/controllerNumber `[CERT, BCBusNetwork.java:115]`. `module.xml` declara `NETWORK_COMMUNICATION` (ports/hosts `*`) con propósito explícito "port 2499".

> **Por qué importa el corrigendum**: confundir esto con Clipsal C-Bus (un protocolo de iluminación residencial australiano sin relación) llevaría a conclusiones erróneas sobre topología, seguridad y casos de uso. Es un driver de **controladores HVAC/BMS comerciales**, no de iluminación.

---

## 108.2 — Arquitectura del driver: sobre `ndriver`, no el driver clásico `[CERT]`

El driver NO extiende el framework clásico `BDeviceNetwork`/`BDevice` del [Bloque 7] directamente — usa **`ndriver`** (la abstracción Tridium para drivers con TCP link layer nativo, [Bloque 19]):

```
BCBusNetwork    extends BNNetwork        (com.tridium.ndriver.BNNetwork)   [CERT, :118]
                implements BINDiscoveryHost
BCBusDevice     extends BNDevice         (com.tridium.ndriver.BNDevice)    [CERT, :222]
                implements ICommandEventListener, IPointRefreshListener, BINPollable
BCBusProxyExt   extends BProxyExt        (javax.baja.driver.point.BProxyExt) [CERT, :124]  ← enlace con B7
BCBusScheduleDeviceExt extends BDeviceExt                                   [CERT-a]
```
`BNNetwork`/`BNDevice` son subclases de `BDeviceNetwork`/`BDevice` dentro de ndriver, así que el driver llega al framework del [Bloque 7] **por herencia a través de ndriver-rt** `[INFER]`. El `BProxyExt` sí es el del framework clásico.

**Tipos de punto Niagara** (3 writables) `[CERT-a]`: `BCBusNumericWritable extends BNumericWritable`, `BCBusBooleanWritable extends BBooleanWritable`, `BCBusEnumWritable extends BEnumWritable` — cada uno lleva un `BCBusProxyExt`. **Deps** (`-rt`): `baja, control-rt, alarm-rt, driver-rt, ndriver-rt, net-rt, serial-rt, schedule-rt, gx-rt, bql-rt`. (`serial-rt` declarado pero **sin uso real** — el transporte es solo TCP `[CERT-a]`.)

---

## 108.3 — El stack de protocolo: 3 capas (TCP → SUSI → XCnap) `[CERT]`+`[CERT-a]`

El núcleo técnico del driver. Tres capas superpuestas:

**Capa 1 — SUSI** (System/Subsystem Interface): el protocolo para hablar con la **BNA**. Mensajes `CBusMessage extends NMessage`; frame = pre-header 4 bytes + header 12 bytes; sequence 0–65535; little-endian `[CERT-a]`. **Handshake** (`SusiProtocol.onStart()`) `[CERT-a]`: `SetBoardMode(config)` → `GetLoginKey` (clave aleatoria 4 bytes) → `Login(keyConvert(key))` → `SBOC(5,30)` (outstanding commands) → `SBT(now)` (sync tiempo) → `SBUC(channel,baud,addr)` (config canal) → `SetBoardMode(running)` → `UPA(deviceList)` (suscribir). Mensajes uplink: `SusiMsgSE/RPA/UPA/PE/DE/Trans`.

**Capa 2 — XCnap** (eXtended Controller Network Application Protocol): los comandos al controlador viajan **tunelizados como mensajes transparentes dentro de SUSI** (`SusiMsgTrans`) `[CERT-a]`. `XCnapMessage`: header 7 bytes (`0x80|destNode`, srcNode, command/response, length, function code…). Function codes: PR/PDR/Broadcast/GwPR/GwBrd… `[CERT-a]`.

**Capa 3 — Commands** (`CommandBase implements ICommand`, identificados por {channel, controller, ip}) `[CERT-a]`:
- Datapoints: `CmdDpPointAttributesRead/Write`, `CmdDpMultiPointAttributesRead`, `CmdDpGlobalRefreshSetupWrite`.
- Metadatos: `CmdDpCharacteristicsRead`, `CmdDpEnggUnitsRead`, `CmdDpStateTextsRead`, `CmdDpAlarmTextsRead`, `CmdReadAPPTT` (Application Point Type Table = tabla maestra de puntos).
- Sistema: `CmdReadControllerInfo`, `CmdReadCentralAlarm`, `CmdReadFile/WriteFile` (ficheros de aplicación), **`CmdFlash`** (flasheo de firmware del controlador), `CmdReadTime/WriteTime`, `CmdLogOnOff`, `CmdAcknowledgeAlarm`.

**Parser de aplicación** `[CERT-a]`: `ApplicationFile` lee ficheros de aplicación del controlador (secciones con header 7 bytes, decompresión, **checksum 16-bit** suma `& 0xFFFF == 0`).

---

## 108.4 — Modelo C-Bus → Niagara y schedules `[CERT-a]`

**Direccionamiento** `[CERT-a]`: un punto Niagara = un datapoint del controlador, identificado por **{canal, controllerNumber, tipo de punto, pointIndex}** (NO Group Address). `BCBusProxyExt` guarda `pointName`/`pointType`/`pointIndex`. **15 tipos de punto** (`BCBusPointType`): AnalogInput(1)/DigitalInput(2)/DigitalOutput(3)/AnalogOutput(4)/CounterInput(5)/AnalogValue(7)/DigitalValue(8)/AnalogRemote(9)/DigitalRemote(10)/XRegister(11)/YRegister(12)/FlexiblePoint(13)/CounterValue(14)/WildCard(15). El subtipo writable (Numeric/Boolean/Enum) se deriva del tipo C-Bus `[INFER]`. Organización: `BCBusNetwork → BCBusDeviceFolder → BCBusDevice → BCBusPointDeviceExt → puntos`. También hay `BCBusParameterDeviceExt` (parámetros de configuración del controlador).

**Schedules = time programs del controlador, sincronizados bidireccionalmente** `[CERT-a]`: NO son schedules Niagara. `BCBusScheduleDeviceExt extends BDeviceExt` gestiona los Time Programs (firmware mínimo `3.8.38.52`). Modelo: `BCBusTimeProgram → BCBusWeeklySchedule → 7× BCBusDailySchedule → N× BCBusSwitchPoint`, + `BCBusYearlySchedule`/`HolidaySchedule`/`TodayOverrideSchedule`. Comandos (`protocol/commands/schedule/`): `CmdTpTimeProgramsRead`, `CmdTpDailyScheduleCreate/Delete`, `CmdTpSwitchPointCreate/Delete`, `CmdTpWeeklyScheduleWrite`, `CmdTpYearlyScheduleWrite`, `CmdTpHolidayScheduleRead/Write`, `CmdTpTempOverrideWrite`. Jobs `BCBusRead/WriteTimeProgramsJob`. La gran cantidad de clases de schedule (18 + 26 UI + 15 comandos) es porque el driver **reimplementa un editor de programación horaria completo** sobre el modelo del controlador XL.

**UI** `[CERT-a]`: **NO usa `genericUIFramework`** ([Bloque 102]). UX = `BClTimeProgramView extends BSingleton implements BIJavaScript, BIFormFactorMax` (bajaux) + `BClServerSideCallHandler implements BIServerSideCallHandler` (box-rt, RPC desde browser) + BajaScript (`clCBus.built.min.js`, widgets `clWeeklySchedule.js`/`clHolidaySchedule.js`…). Workbench = Swing clásico (`BCBusPointManager`, `BCBusDeviceManager`).

---

## 108.5 — Calidad / seguridad `[CERT]`

- **License-check doble + brand-gating `[CERT]`**: `Sys.getLicenseManager().checkFeature("Honeywell","honCbus")` y si falla `checkFeature("HoneywellCentraLine","clCbus")` (BCBusNetwork.java:335-407). Valida además `brandId` contra una whitelist {HoneywellBMS, CentraLine, ComfortPoint, Webs, ComfortAndEnergy, WebsOpen, SBC, Trend, HoneywellMVC}; si no coincide → `FeatureNotLicensedException` `[CERT-a]`. **Point-limit** por licencia (campo `point.limit`, `"none"`=MAX_INT), consultado por BQL cada 10 s. Hay UI de mensaje de licencia (`clLicenseMsgView.js`). No se observó bypass.
- **Login BNA challenge-response `[CERT-a]`**: `SusiMsgLogin.keyConvert()` transforma la clave del servidor con un **LCG** (`m=22695477, n=1` — constantes Knuth/Numerical Recipes) + XOR. **Sin contraseña en texto claro, sin credenciales hardcodeadas.** Es autenticación débil (LCG no es cripto), pero no hay secreto embebido.
- **Limpio `[CERT-a]`**: sin `Runtime.exec()`, sin `loadLibrary`/JNI, sin `MessageDigest`/`Cipher`. Un solo `System.out.println` residual (`CBusAlarmSupport.java:432`).
- **Integridad** `[CERT-a]`: checksum 16-bit de los ficheros de aplicación (`ApplicationFile`); enum de respuesta con `CRC_CHECK(84)`/`ApplicationChecksumError(99)`. **Sin firma criptográfica** del fichero de aplicación — consistente con el patrón Honeywell del barrido.
- **`CmdFlash`** `[CERT-a]`: el driver puede **flashear firmware** del controlador XL desde Niagara — superficie sensible (un atacante con acceso a la estación puede reprogramar controladores), aunque protegida por el login BNA y la licencia.

---

## 108.6 — Conexiones

- **[Bloque 7]** (Drivers Framework): `BCBusProxyExt extends BProxyExt`; la red/dispositivo llegan al framework vía ndriver.
- **[Bloque 19]** (drivers / `ndriver` / BOX): `clCBus` es un caso real de driver sobre **ndriver** con TCP link layer + `BIServerSideCallHandler` (box-rt) para la UI.
- **[Bloque 78]/[Bloque 86]/[Bloque 87]** (familia CentraLine): otro miembro CentraLine — drivers de campo (PanelBus/OnboardIO B86) y librerías de control (clHVAC B87); este es el driver de los controladores XL legacy vía BNA.
- **[Bloque 32]** (**corrigendum**): invalida la etiqueta "Clipsal CBus lighting Australia"; es el CentraLine Controller Bus (SUSI+XCnap sobre BNA TCP 2499). Segundo error de B32 corregido (tras `ascCommon` en [Bloque 107]).
- **[Bloque 75]** (seguridad): aporta login BNA con LCG débil (no cripto), fichero de aplicación sin firma (solo checksum), y `CmdFlash` como superficie de reprogramación de firmware.
