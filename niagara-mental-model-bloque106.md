# Bloque 106 — `honeywellSpyderTool` / `XL10NextGen`: la herramienta de programación del Spyder (3er motor de Function Blocks Honeywell — modelo **compilar→binario→descargar**), deofuscada

> Investigación empírica del módulo OEM Honeywell **`honeywellSpyderTool`** (= paquete `com.honeywell.honeywellXL10NextGen`, ~1464 clases top-level / 1619 java — el módulo Honeywell **más grande** del corpus). `module.xml`: *"Library of control components to program Honeywell Spyder device"*, symbol `honst`, vendor Honeywell `4.14.0.10.5.64`, build **2024-10**, **`runtimeProfile=wb`** (Workbench-only — es una herramienta de ingeniería, no corre en el JACE).
>
> **Completa el [Bloque 77]**: ese bloque destiló los **drivers de comunicación** del Spyder (`BBacnetSpyder`/`BLonSpyder`, interfaces `ISpyder*`, ciclo compilar→descargar a alto nivel). Este destila **la herramienta de programación en sí**: el motor de function blocks, el **compilador a binario propietario**, el **simulador** y el sistema de tipos.
>
> **Cierra el panorama de los TRES motores de Function Blocks de Honeywell** (todos independientes entre sí): F1 / DDC ([Bloque 103]), IRM Nano ([Bloque 105]), y Kingfisher/XL10NextGen (este).
>
> 1 módulo Workbench-only. Paquetes: `functionalBlocks/{blocks/{logic,math,analog,control,builtIn/kingfisher,...},ui}`, `xl10Controller/{compilation,datatypes,device,...}`, `deviceModes/{common,simulation}`, `library`, `sylk/fw`, `io`, `points`, `network`.
>
> Fuentes: `organized/honeywellSpyderTool/honeywellSpyderTool/vineflower/com/honeywell/honeywellXL10NextGen/...`. Decompilación vineflower **limpia**.
> Método: 1 sub-agente Explore profundo + **verificación directa** del extends raíz (`BHoneywellComponent`/`BAnd`/`BMath`/`BSBusWallModule`/`BSpyderIICompilation`), del **cero acoplamiento** con `honfunctionblocks`, del CRC-16, y del **algoritmo PID real en el simulador**. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (catálogo, conteos, jerarquía del compilador, límites, modos) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 105]. **Conecta fuerte**: [Bloque 77] (drivers Spyder — la otra mitad), [Bloque 103] y [Bloque 105] (los otros dos motores FB, contraste arquitectónico), [Bloque 88] (Sylk/wall modules TR — división tool-side vs runtime), [Bloque 102] (`genericUIFramework`, dependencia), [Bloque 101] (`airFlowBalancer`, dependencia), [Bloque 75] (seguridad).

---

## 106.1 — Qué es: la herramienta de ingeniería del Spyder `[CERT]`

`honeywellSpyderTool` es la **herramienta Workbench** con la que el ingeniero programa los controladores **Honeywell Spyder** (Model 4–7, BACnet y LON) y sus periféricos. No es un driver ni corre en el JACE: `runtimeProfile=wb` `[CERT]`. Contiene la paleta de function blocks, el editor wiresheet, el compilador, el simulador y el wizard de aplicaciones.

`module.xml` `[CERT]`: symbol `honst`, build 2024-10. **Dependencias**: Tridium (`baja`, `control-rt`, `driver-rt`, `bajaui-wb`, `wiresheet-wb`, `workbench-wb`, `chart-wb`, `kitPx-wb`, `fox-rt`, `web-rt`, `batchJob-rt`…) + Honeywell **`genericUIFramework`** ([Bloque 102]) + **`airFlowBalancer`** ([Bloque 101]). **NO depende de `honeywellFunctionBlocks`, `honIrmControl` ni `honeywellSylkDevice`** `[CERT]`. Módulo firmado (`META-INF/SERVER1.RSA`).

> **Nomenclatura `[CERT-a]`**: el código usa dos nombres en clave internos:
> - **"XL10NextGen"** = el controlador Spyder en sí (`xl10Controller/`).
> - **"Kingfisher"** = el subsistema de los **Smart Room Controllers T-Series** (TR70/TR71/TR75/TR40/TR42) que cuelgan del Spyder por **S-Bus (Sylk)**. NO es un chip; es el subsistema tool-side que configura y compila esos wall modules. Verificado: `BSpyderIICompilation.compileKingfisherInputs()/compileKingfisherOutputs()`.
> - **"Piranha"** = el subsistema de **device modes** (`BPiranhaMode`, `BPiranhaModeEnum`).

---

## 106.2 — El hallazgo central: **el TERCER motor de FB Honeywell**, y es **tool-side** `[CERT]`

Verificado de mi mano. La librería de function blocks del Spyder cuelga de una raíz propia, **sin ningún acoplamiento** con los otros dos motores:

```
BAnd  (functionalBlocks/blocks/logic/BAnd.java:40)        BAdd → BMath (math/BMath.java:21)
  extends BHoneywellComponent                                 extends BHoneywellComponent
            │
            └─ BHoneywellComponent  (functionalBlocks/BHoneywellComponent.java:90)   [CERT]
                 extends BComponent
                 implements BIHoneywellComponent, BIXL10VirtualChild
```

`grep -rl "honfunctionblocks|BFunctionBlock|IHoneywellExecutionBlock"` sobre todo el módulo = **0 resultados** `[CERT]`. Los FB del Spyder NO comparten una sola línea con el motor F1 ([Bloque 103]) ni con el IRM Nano ([Bloque 105]).

### Los TRES motores de Function Blocks de Honeywell `[CERT]`

| | **F1 / DDC — B103** | **IRM Nano — B105** | **Kingfisher / XL10NextGen — este (B106)** |
|---|---|---|---|
| Raíz del FB | `BFunctionBlock implements IHoneywellExecutionBlock` | `BNanoFunctionBlock implements BINanoControl` | `BHoneywellComponent implements BIHoneywellComponent` |
| Módulo | `honeywellFunctionBlocks` | `honIrmControl` + `honIrmConfig` | `honeywellSpyderTool` |
| Target | Spyder/IPC/kitCat | PUC BEATS | Spyder Model 4–7 + wall modules TR |
| **Dónde corre la lógica** | **EN el station** (engine DDC, `executeBlock()` por ciclo) | **EN el firmware del BEATS** (Java = descriptor, sync vía protocolo Nano) | **EN el firmware del Spyder** (Java = descriptor, **compilado a binario** y descargado) |
| Modelo de bajada | corre server-side | sincronización Nano sobre BACnet (B88) | **compilar → binario propietario (CRC-16) → download** |

> **La conclusión arquitectónica `[CERT]`**: Honeywell mantiene **tres** librerías de control paralelas, una por familia de hardware, **sin reutilización de código entre ellas**. La diferencia no es solo el código sino *dónde y cómo se ejecuta la lógica*: B103 server-side; B105 sincroniza descriptores al firmware; B106 **compila** descriptores a un binario y lo descarga. Este (B106) es el modelo "compilar→descargar" clásico de PLC.

---

## 106.3 — El compilador: descriptores → binario propietario `[CERT]`

El núcleo de valor de la herramienta. Dos jerarquías de compilador:

**Controlador Spyder** (`xl10Controller/compilation/`) `[CERT]`+`[CERT-a]`:
```
BCompilation extends BComponent implements ICompiler        [CERT, :181]
  └─ BSpyderIICompilation extends BCompilation               [CERT, :112]   (Model4-6, BACnet1-3)
       └─ BCompilationCC1 extends BSpyderIICompilation
            └─ BSpyderRelayCompilation extends BCompilationCC1               (Model7, BACnet4)
```
**Wall modules Kingfisher** (`kingfisher/compilation/`) `[CERT-a]`: `BBasicKFCompiler implements IKFCompiler` → `BZioEnhCompiler` → `BZioplusCompiler`.

**Salida** `[CERT-a]`: binarios estructurados en **secciones** (`FileSection0..5`), serializados big-endian a `BBlob` (Niagara binary blob) vía stores (`BKFFileSectionNStore`, `BTr4xFileNStore`). `BCompilationJob extends BSimpleJob` orquesta el flujo: `verifyApplication()` → `BSpyderValidateUtility.validate()` → `setCompileStatusFlag(3)` → `loadControlProgram()` → `clearAllBinaryInfo()` → `beginCompile()`. El `compileStatusFlag` es el slot frozen del [Bloque 77]. `getBinaryModified()` (BitSet) permite **descarga incremental** de solo las secciones cambiadas.

### Integridad: solo CRC-16, **sin firma criptográfica** `[CERT]`
El binario compilado se protege con CRC, **no con firma**:
- **1's complement** (`BChecksumGenerator`): suma de bytes + `~n`.
- **CRC-16/CCITT** (`BCRCGenerator`): `POLYNOMIAL=0x1021`, `INITIAL=0xFFFF`, bit-a-bit `[CERT, verificado :23,24,44]`.
- **CRC-16 tabla** (`calculateCRCChecksumForBOAC`): tabla de 256 entradas, variante para protocolo BOAC.

> **[SEGURIDAD CERT]** El controlador Spyder acepta cualquier binario con CRC-16 válido. **No hay verificación criptográfica del programa descargado** — un atacante que pueda hablar el protocolo de descarga puede inyectar lógica de control arbitraria recalculando el CRC. Es el **mismo patrón transversal** del barrido: firmware/binarios Honeywell sin firma (Device Manager B94, TC wizard B98, IPC B99). Aquí aplica al *programa de control* del Spyder, no solo al firmware.

---

## 106.4 — El simulador: el algoritmo de control vive en Java **solo para simular** `[CERT]`

La herramienta **simula la lógica del controlador en el PC** antes de descargar `[CERT-a]`. Subsistema "Piranha":
- `BSimulationMode extends BPiranhaMode`; `BSimulationThread extends BComponent implements Runnable` invoca los bloques por reflexión.
- Interfaz `BISimulation { void execute(BComponent, BSimulationStruct); }` `[CERT-a]`.
- **44 simulation blocks** (`deviceModes/simulation/simulationblocks/`), uno por FB: Add, Pid, Aia, Stager, OccArb, Schedule, Hystrel, Enthalpy, FlowControl…

> **El contraste que cierra los tres motores `[CERT]`**: `BSimulationPid` contiene el **algoritmo PID completo en Java** — estado persistente `PidLoopStatic_intglerr`/`old_err`, integración `intglerr += err·Ki` (:136), anti-windup con `limitInput(...)` clamp 0–100 (:137), término derivativo sobre `old_err` (:112-113). **Pero ese algoritmo solo existe para la simulación**: el `BAnd`/`BPid` "real" de la paleta es un descriptor sin lógica (igual que en B105). Es decir:
> - En **B103** la matemática del FB corre en Java en producción (server-side).
> - En **B105** y **B106** la matemática NO está en el FB de producción (corre en firmware); en B106 además existe una **copia del algoritmo en el simulador** para previsualizar en el PC.

`BKFStateMachine` (~1800 líneas) simula además la **máquina de estados del LCD** de los wall modules TR (display, segmentos, timers) `[CERT-a]`.

---

## 106.5 — Sistema de tipos, modos, librería, Sylk `[CERT-a]`

**Sistema de tipos** (`xl10Controller/datatypes/`, 75 + `kingfisher/datatypes/`, 62): descriptores Java de las estructuras del firmware. `IDefinitions` fija los **límites del Spyder** `[CERT-a]`: `MAX_CONTROL_LOOP=100`, `MAX_ANALOG_INPUT=8`, `MAX_DIGITAL_OUTPUT=8`, `MAX_NUMBER_OF_NVS=62`, `BYTES_IN_CONTROL_LOOP=1322`. Tipos: `AnalogInputConfiguration`, `ControlLoop(Store)`, `NvConfigurationDescriptor`, `WallModuleConfiguration`, `HolidaySchedule`… Kingfisher añade `BParameter` (PVID), `BPVIDGroupTable/SendTable` (mapeo S-Bus), `BKFLabel` (strings LCD), `BSensorCalibration`.

**Modos "Piranha"** (`deviceModes/`, 125 clases) `[CERT-a]`: `BPiranhaModeEnum` con 4 modos — **Engineering** (programación offline), **OnlineDebugging** (monitoreo live conectado), **Monitoring** (online read-only), **Simulation** (PC). Base `BPiranhaMode extends BComponent`.

**Librería** (`library/`, 63): `BAppLibrary` (índice XML + `UserDefined/<UUID>`), `BLibBogFile extends BBogFile` (guardar/importar sub-aplicaciones como BOG). Los **modelos asistidos por wizard** `[CERT-a]`: `TASOWizModel1`=CV/AHU, `Model2/3`=VAV, `BacnetModel1/2`=VAV BACnet, `Model4`=LCBS — templates HVAC.

**Sylk / S-Bus tool-side** (`sylk/fw/`, 39 + `kingfisher/tr4x/`, 101) `[CERT-a]`: `BAbstractSylkDeviceFB extends BHoneywellComponent implements BISylkDevice`; `BSBusWallModule extends BAbstractSylkDeviceFB implements BILibItem` `[CERT]`. Interfaz `BISylkDevice`: `compile(SylkLinkTable, address, ISpyderDevice)`, `fullDecompile()`, `quickDecompile()`. `SylkLinkTable`/`SylkLink` = mapeo PVID Spyder ↔ PVID dispositivo Sylk. `BZioTIDevice` calcula bandwidth S-Bus (TR4X vs TR7X).
> **División con [Bloque 88] `[CERT-a]`**: `sylk/fw/` aquí = **compilación/decompilación tool-side** del binario Sylk; `honeywellSylkDevice` (B88) = **driver runtime** de la comunicación física S-Bus.

---

## 106.6 — Calidad / seguridad `[CERT]` + `[CERT-a]`

- **[SEGURIDAD CERT]** Binario de control **sin firma criptográfica** — solo CRC-16 (§106.3). Es el hallazgo de seguridad principal: el programa del Spyder es falsificable si se conoce el protocolo de descarga.
- **Limpio en ejecución `[CERT-a]`**: sin `Runtime.exec()`, sin `loadLibrary`/JNI, sin `MessageDigest`/`Cipher`/crypto. Los únicos `Runtime` son `gc()`/`freeMemory()` (monitoreo). Sin credenciales hardcodeadas (el `passwordScreen` del TR4X es campo de usuario U16 0-9999).
- **Debug en producción `[CERT-a]`**: **561 `System.out.println`** sin remover (concentrados en `ZioResourceCounter`: dumpea tamaños de sección de archivo). Fuga de detalles internos de compilación a stdout.
- **EULA hardcodeado** `[CERT-a]`: `BEULADialog.java:194` embebe el texto completo del EULA como literal — artifact de packaging, no vulnerabilidad.

---

## 106.7 — Conexiones

- **[Bloque 77]** (drivers Spyder): la **otra mitad**. Los compiladores de aquí producen el payload que `ISpyderDownload`/`ISpyderCompile` (B77) envían; `BCompilationJob.compile()` manipula el `compileStatusFlag` (slot frozen de B77). B77 = cómo se comunica; B106 = cómo se programa y compila.
- **[Bloque 103]** (`honeywellFunctionBlocks`) y **[Bloque 105]** (`honIrmControl`): los otros dos motores FB. **Cero acoplamiento** verificado entre los tres (§106.2). Tres librerías paralelas, una por familia de hardware.
- **[Bloque 88]** (`honeywellSylkDevice`): división tool-side (compilación binario, aquí) vs runtime (driver físico S-Bus, B88).
- **[Bloque 102]** (`genericUIFramework`) y **[Bloque 101]** (`airFlowBalancer`): dependencias declaradas — la UI de wizards y el balancing de flujo se reutilizan dentro de la herramienta.
- **[Bloque 75]** (seguridad): aporta el binario de control sin firma (solo CRC-16) y el debug-to-stdout en producción.

---

## 106.8 — Pendiente del módulo (no destilado aquí)

`honeywellSpyderTool` son ~1464 clases; este bloque cubrió el **eje de programación** (motor FB, compilador, simulador, tipos, modos, Sylk tool-side). Quedan ángulos secundarios para un bloque futuro si interesa: la capa **UI completa** (`functionalBlocks/ui`, `kingfisher/ui` con wizardgen — ~250 clases), el detalle de **I/O** (`io/`, 87: asignación de terminales, linearización), y el **catálogo exhaustivo** de los ~60 function blocks builtIn por categoría. También sigue pendiente de la cola del item #2: **`ascCommon`** (1741 clases, legacy ASC/CentraLine, marcado TODO en [Bloque 32]).
