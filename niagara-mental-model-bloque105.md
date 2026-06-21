# Bloque 105 — `honIrmControl`: la librería de control (kitControl) del controlador BEATS/IRM — ~163 function blocks sobre el motor **IRM Nano**, deofuscada

> Investigación empírica del módulo OEM Honeywell **`honIrmControl`** (203 java en `-rt`, `module.xml`: *"Library of IRM Control Components"*, symbol `irmn`, vendor Honeywell `4.14.0.3.2.0.6`, build **2025-01-10**). Es el **kitControl del PUC BEATS**: ~163 function blocks (aritmética, lógica, comparación, selección, timers, control de lazo, VAV, salidas, I/O físico, objetos BACnet, Modbus, Sylk, schedule, GUI) que el ingeniero compone en el programa del controlador.
>
> **Cierra la familia BEATS/IRM**: [Bloque 88] destiló la herramienta de config (`honIrmConfig` — protocolo Nano sobre BACnet) y el modelo de wall modules (`honeywellSylkDevice`); este bloque destila la **librería de control** que corre dentro de ese controlador. Análogo a lo que `clHVAC*` ([Bloque 87]) es para Centraline.
>
> 1 módulo (`honIrmControl`, `-rt`/`-ux`/`-wb`). Paquetes `-rt`: `irm/{arithmetic,logic,comparison,bitfunctions,selectswitch,timer,datetime,controlLoop,vav,vav/schedule,outputs,light,blind,physicalpoints,bacnetobjects,modbus,sylk,wallmodule,util,conversion,parameter,fbOne}`; `-ux`: `irm/{honirmfunctionblocks,modbus/serversidecall}`; `-wb`: field editors.
>
> Fuentes: `organized/honIrmControl/honIrmControl-{rt,ux,wb}/vineflower/com/honeywell/irm/...` + el motor en `organized/honIrmConfig/honIrmConfig-rt/vineflower/com/honeywell/irmnano/fbfactory/...`. Decompilación vineflower **limpia** (203 java = 203 .class top-level, 0 fallos).
> Método: 1 sub-agente Explore + **verificación directa** del extends raíz (`BAdd`/`BNanoFunctionBlock`), de la ubicación del motor, de `BFbFactory`, y de la ausencia de algoritmo PID en `BPid`. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (catálogo por grupo, conteos, propiedades de bloques) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 104]. **Conecta fuerte**: [Bloque 88] (config Nano + motor `BNanoFunctionBlock` + wall modules — la otra mitad de la familia), [Bloque 103] (el OTRO motor de FB Honeywell, contraste arquitectónico clave), [Bloque 99] (driver IPC/CIPer), [Bloque 87] (kitControl Centraline), [Bloque 75] (seguridad).

---

## 105.1 — Qué es: el kitControl del BEATS `[CERT]`

`honIrmControl` es una **librería de componentes de control** (no un driver, no un servicio): un catálogo de ~163 function blocks que el ingeniero arrastra al programa del controlador físico **BEATS** (Building Edge Automation Trusted System; "IRM" = apodo interno del equipo de software, [Bloque 88]). Es a Honeywell-BEATS lo que `kitControl` es a Tridium y `clHVAC*` ([Bloque 87]) a Centraline.

`module.xml` `[CERT]`: symbol `irmn`, `description="Library of IRM Control Components"`, `autoload=true`, `nre=true`. **Dependencias (11)** `[CERT]`: `baja`, `control-rt`, `bacnet-rt`, `driver-rt`, `kitIo-rt`, `schedule-rt` (Tridium); `honeywellSylkDevice` (B88), **`honIrmConfig` (B88, donde vive el motor)**, `honeywellVersionManager`, `airFlowBalancer` (B101), `honeywellDeviceManager` (B94).

> **El catálogo se publica como `<types>` en `module.xml`** `[CERT]`: ~150 tipos registrados (`Add`, `Pid`, `BinarySelectPrio`, `OnboardIoRS4`, `BacnetNumericWritable`, …). Cada uno es un FB instanciable desde la paleta Workbench.

---

## 105.2 — El hallazgo central: **DOS motores de Function Blocks Honeywell distintos** `[CERT]`

Lo más importante de este bloque. `honIrmControl` **NO usa el motor del [Bloque 103]**. En `module.xml` **no hay dependencia sobre `honeywellFunctionBlocks`** `[CERT]`. Su motor es otro:

```
BAdd  (honIrmControl-rt/arithmetic/BAdd.java:117)
  extends BNanoFunctionBlock                          [CERT, import :4]
           │
           └─ BNanoFunctionBlock  (honIrmConfig-rt/irmnano/fbfactory/…:165)   ← vive en B88, NO aquí
                extends BComponent
                implements BINanoControl, BIIRMSupportedComponent             [CERT]
```

Verificado idéntico para `logic/BAnd`, `comparison/BGreaterThan`, `timer/BMultifunctionTimer` `[CERT-a]`. Es decir, **toda la librería cuelga de `BNanoFunctionBlock`, que está definido en `honIrmConfig` (B88), no en este módulo**.

### Contraste arquitectónico con el [Bloque 103]

| | **Motor F1 — `honeywellFunctionBlocks` (B103)** | **Motor IRM Nano — `honIrmControl` + `honIrmConfig` (B88/este)** |
|---|---|---|
| Clase base FB | `BFunctionBlock extends BComponent implements IHoneywellExecutionBlock` | `BNanoFunctionBlock extends BComponent implements BINanoControl` |
| **Dónde ejecuta la lógica** | **EN el station** (engine DDC invoca `executeBlock()` por ciclo) | **EN el firmware del controlador BEATS** — el Java es solo descriptor que se **sincroniza** vía protocolo Nano |
| Targets | Spyder, IPC/CIPer (B99), kitCat (B101) | PUC BEATS — RS3N/RS4N/RL/VA423/VA75xx/TC300… |
| Protocolo de bajada | function blocks corren server-side | **Nano sobre BACnet** (B88): GET/SET_VALUES, CREATE_CHILD… |

> **Implicación `[CERT]`**: en IRM Nano el Java N4 es un **espejo de configuración**, no un ejecutor. `BPid` declara `ProportionalBand`/`IntegralTime`/`DerivativeTime` pero **no contiene NINGUNA operación aritmética** (verificado: cero `*`, `/`, integral, derivative, prevError en el cuerpo) — el lazo PID corre en el silicio del controlador; N4 solo serializa parámetros. Esto es lo opuesto al B103, donde la matemática del FB SÍ vive en Java.

### `fbOne` — la fábrica del motor `[CERT]`

`com.honeywell.irm.fbOne.BFbFactory extends BObject implements BINanoFactory` es el registro del motor:
- `FAMILY_ID = 200`, `FAMILY_NAME = "IrmControl"`, `FB_FAMILY_VERSION = "3.0.0.0"` `[CERT]`.
- `createNanoFunctionBlock(int fbType)`: switch gigante (fbType 1–277) que instancia cada FB concreto — `case 77 → new BPid()` `[CERT]`. El `fbType` de cada bloque se codifica `FAMILY_ID << 16 | n` (BPid = `200<<16|77` = 13107277) `[CERT-a]`.
- **`[CALIDAD CERT-a]`**: los fbType no reconocidos caen al `default` dejando `el = null` y el método **retorna `null` sin excepción** → NPE potencial si el caller no chequea. El paquete `fbOne` también trae FBs sueltos (`BFan`, `BHystereticRelay`, `BUpDownSlatAngle`) y `BIrmControlVersion` (solo versión, `extends BComponent`, no es FB).

---

## 105.3 — Catálogo de function blocks por grupo `[CERT-a]`

Todos `extends BNanoFunctionBlock` salvo lo indicado. ~163 FB reales + 15 enums frozen + 8 helpers + 4 componentes no-FB.

- **arithmetic (12)**: `BAdd`/`BSubtract`/`BMultiply`/`BDivide`/`BNegative`/`BExponential`, `BLimit`, `BLinearGraph`, `BMathOperation`, `BAggregation`, `BReset`, **`BPsychrometric`** (entalpía/humedad HVAC). `BDivide` tiene `Divisor` default 0.0 **sin guardia de división por cero en Java** — protección delegada al firmware `[CERT-a]`.
- **logic (7) + comparison (6) + bitfunctions (3)**: `BAnd`/`BOr`/`BXor`/`BNot`, `BRsFlipFlop`/`BSrFlipFlop`, `BTrigger`; `BCompare`/`BGreaterThan(Equal)`/`BLessThan(Equal)`/`BEqualNull`; `BBitAnd`/`BBitOr`/`BNumericToBit`. Lógica booleana/bit estilo PLC.
- **selectswitch (11)**: selección/switch con arbitraje por prioridad — `BBinarySelect[Multi][Prio]`, `BNumericSelect`/`BNumericSwitch`, `BMaxSelectMulti`/`BMinSelectMulti`, `BValidSelect[Multi]Prio`, `BChangeSelect`. Las variantes "Prio" emulan el priority array BACnet.
- **timer (5) + datetime (7)**: `BOneShot`, `BTimeDelay`, `BTimeRamp`, `BRateLimit`, `BMultifunctionTimer`; `BCurrentDateTime`, `BDate`/`BTime`/`BTimeOfDay`, `BDateTimeOperation`/`BDateAndTimeOperation`, `BTimeDifference`. Lógica temporal (monoestable, rampas, retardos en segundos).
- **controlLoop (3)**: `BPid`, `BPidA` (sin propiedad `Operation` — dirección fija/firmware), `BAia` (Air-side Integral Action, floating). **Los 3 son descriptores puros, sin algoritmo en Java** (ver §105.2).
- **vav (14) + vav/schedule (6)**: control de cajas VAV y staging — `BFlowControl`, `BFlowVelocity`, `BDigitalFilter`, `BGeneralSetpointCalculator`, `BTemperatureSetpointCalculator`, `BSetTemperatureMode`, `BOccupancyArbitrator`, `BStager`/`BStageDriver`/`BCycler`, `BRunTimeAccumulate` (acumula RuntimeSec/Hours/Days), `BAlarm`, `BCounter`, `BEncode`. Schedule: `BCalendar`/`BEnumSchedule` (FB), `BIrmCalender extends BCalendarSchedule`, `BIrmEnumSchedule extends javax.baja.schedule.BEnumSchedule`, `BScheduleStatesEnum`.
- **outputs (7) + light (2) + blind (1)**: `BFloating` (actuador 3-wire con power-up delay + auto-sync de posición), `BPwm`, `BStg123Outp` (1-2-3 etapas); `BLightA`/`BOnOffDimming` (DALI/0-10 V); `BBlindA` (persianas Up/Down/Stop/SlatAngle).
- **util (14)**: constantes `BConst{1,2,5}{Boolean,Numeric}`, `BPassThru`, `BPrevValue`, `BError`, `BText A`, `BSystemA` (diagnóstico), **`BSavePermanent`** (persiste al storage no-volátil del controlador), `BEvaluateBacnetStatusFlags`.
- **conversion (1)**: `BBinaryToNumeric`. **parameter (1)**: `BTypeCharacteristicPar extends BComponent` (no-FB; caracteriza terminales UIO).

---

## 105.4 — I/O físico, objetos BACnet, Modbus, Sylk `[CERT-a]`

### physicalpoints (50) — el hardware de cada modelo BEATS
Dos jerarquías paralelas:
- **Terminales**: `BAoTerminal/BBoTerminal/BBiTerminal/BUiTerminal/BUioTerminal/BCoTerminal/BServicePinTerminal` → `extends BIOTerminalWithPin` (de honIrmConfig, que `extends BNanoFunctionBlock`) `[CERT-a]`.
- **Onboard I/O por modelo**: `BOnboardIoRS3/RS4/RS5`, `…VA423B24N/VA75xx/VA00xx`, `…RL16xx/RS08xx`, `…FCU24V/230V`, `…SMB1/IO424`, `…TC300/TC320/TC321/TC322/TC323` (todas las variantes BG/CG/DG/CN) → `extends BOnboardIo` (de honIrmConfig, `extends BComponent`) `[CERT-a]`. Es el lineup de hardware del [Bloque 88] aterrizado en clases I/O.
- **Sensores virtuales**: `BTemperatureSensor`/`BCO2Sensor`/`BHumiditySensor`/`BLightSensor`/`BProximitySensor`/`BVOCSensor`/`BFlowSensor`/`BActuatorPositionFeedback` (FB directos).

### bacnetobjects (27) — los objetos que el BEATS expone en su red BACnet
`BBacnetNumericBase extends BNanoFunctionBlock` (package-private) ← `BBacnetNumeric{Input,Output,Value,Writable}` `[CERT-a]`. Booleanos/enum (`BBacnetBoolean*`, `BBacnetEnum*`), `BBacnetNotificationClass`, `BRefIn`/`BRefOut`, `BNotificationClassPackage extends BDefaultNotificationClasses` + 8 enums de fail-detect/fallback.
> **Relación con B88 `[CERT-a]`**: estos objetos se DEFINEN aquí y `honIrmConfig` (B88) los descubre/gestiona por string de tipo — `BNanoOfflineDiscoveryJob` referencia `"honIrmControl:BacnetNumericWritable"`. Aterriza los `BBacnetNumericWritable`/`BNotificationClass` que el B88 mencionaba como objetos propietarios tipo 512.

### modbus (4 rt + 5 ux) — el BEATS como master Modbus RTU
`BModbusDevice`/`BModbusReadPoint`/`BModbusWritePoint` (FB) + `BTriggerWriteEnum`; el `-ux` trae `BModbusServerSideCallHandler` (RPC de la UI para validar/persistir config) + DTOs.

### sylk (4) + wallmodule (3) — periféricos de sala
`BSylkDeviceFunctionBlock` + `BSylkIn/Out/InOutParam` (`extends BSylkParamFunctionBlock` de honIrmConfig). `BConventionalWallModule`, `BSylkWallModTr42` (termostato de pared TR42 vía Sylk), `BWmConfigHvacA implements IWallModuleConfig` (gestiona proxy files de config por modelo, `MODEL_ID_2_PROXY_FILE`). Conecta con el modelo Sylk del [Bloque 88].

### gui (9) — HMI embebido del controlador
`BGui{Boolean,Enum,Numeric}{Input,Output,InputOutput}` (FB): puntos de interfaz del display local del BEATS.

---

## 105.5 — Calidad / seguridad `[CERT]` + `[CERT-a]`

**Limpio en credenciales y ejecución `[CERT]`** (grep exhaustivo sobre `-rt`/`-ux` vineflower de `password|exec|Runtime|loadLibrary|MessageDigest|System.out|cipher|secret`):
- Sin credenciales hardcodeadas, sin `Runtime.exec()`/`Process`, sin `System.loadLibrary` (JNI), sin `MessageDigest`, sin `System.out` en producción.
- Contrasta con primos de la familia: a diferencia del IPC/CIPer ([Bloque 99], que SÍ tiene `RunShellCommand` JNI y `libciper.so`) y del motor F1 ([Bloque 103], con license-bypass "Webs/WebsOpen"), `honIrmControl` es **librería pura de modelo de datos** — sin superficie de ejecución propia. El riesgo real está aguas arriba: en el protocolo Nano de bajada ([Bloque 88]) y en el firmware que ejecuta estos descriptores.

**Defectos de calidad (bajo impacto) `[CERT-a]`**:
- `BFbFactory.createNanoFunctionBlock()` retorna `null` para fbType no reconocido sin excepción → NPE potencial en el caller (§105.2).
- `BDivide` sin guardia de división por cero en Java (default Divisor 0.0) — falla silenciosa si el firmware tampoco la maneja.

---

## 105.6 — Conexiones

- **[Bloque 88]** (`honIrmConfig` + `honeywellSylkDevice`): la **otra mitad de la familia BEATS/IRM**. Ahí vive el motor `BNanoFunctionBlock`, el protocolo Nano de bajada y el modelo Sylk; aquí viven los ~163 function blocks que se componen y se sincronizan a través de ese protocolo. Juntos = config + control del PUC BEATS.
- **[Bloque 103]** (`honeywellFunctionBlocks`): el **otro motor de FB Honeywell**. Contraste arquitectónico clave (§105.2): F1 ejecuta en el station; IRM Nano ejecuta en el firmware del controlador. Honeywell mantiene DOS librerías de control paralelas según dónde corre la lógica.
- **[Bloque 99]** (IPC/CIPer) y **[Bloque 101]** (kitCat): primos que usan el motor F1 (B103), no éste — el IPC ejecuta FB server-side; el BEATS los ejecuta en hardware.
- **[Bloque 87]** (`clHVAC*` Centraline): el kitControl paralelo de la otra marca del grupo — mismo rol, distinto ecosistema.
- **[Bloque 75]** (seguridad): aporta que el módulo es limpio en sí; el riesgo está en el canal Nano (B88) y el firmware.
