# Bloque 86 — Drivers de I/O de campo Centraline/Honeywell: PanelBus (RS-485 CLIO/Snap-on) + OnboardIO (Eagle Hawk JNI) + IOcreation (framework UI) deofuscados

> Investigación empírica de los **tres módulos de I/O físico** de Centraline/Honeywell — cómo un controlador conecta entradas/salidas de campo (sensores, actuadores) al modelo Niagara. Extiende el [Bloque 78] (drivers Centraline C-Bus/EnOcean) con la capa de I/O eléctrico real.
>
> 3 módulos: `clPanelBus` (driver del bus RS-485 de módulos I/O CLIO + Snap-on), `clOnboardIO` (I/O integrado del controlador Eagle Hawk vía JNI) y `clIOcreation` (framework Workbench común que crea los puntos).
> Decompilados Centraline limpios (sin ZKM relevante; nombres claros).
>
> Fuentes: `organized/{clPanelBus,clOnboardIO,clIOcreation}/<m>-{rt,wb}/vineflower/com/honeywell/...` (+ lexicon, configFiles/*.xml).
> Método: 2 sub-agentes + **verificación directa** de cada `extends` con grep. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (protocolo, modelos HW, configs) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 78]. Conecta [Bloque 7] (drivers/proxy ext), [Bloque 10] (Platform — el bus RS-485 lo expone `platPanelbus`), [Bloque 83] (export BTP/BEATS al cloud).

---

## 86.1 — Los tres módulos + hallazgo de DOS frameworks de driver `[CERT]`

| Módulo | Clase raíz verificada | Framework | Qué conecta |
|--------|-----------------------|-----------|-------------|
| `clPanelBus` | `BPanelbusNetwork extends BBasicNetwork` (:240) | **basicdriver** (`com.tridium.basicdriver`) | módulos I/O externos en bus RS-485 |
| `clOnboardIO` | `BOnboardIONetwork extends BNNetwork implements BIClNPollable` (:28) | **N-driver** (`com.tridium.ndriver`) | I/O integrado del controlador (JNI) |
| `clIOcreation` | `BEnhancedWireSheet extends BWireSheet` (:59) | Workbench UI | (no es driver — crea los puntos) |

**HALLAZGO `[CERT]`**: los dos drivers de I/O Centraline usan **frameworks de driver distintos**:
- `clPanelBus` sobre **`BBasicNetwork`/`BBasicDevice`/`BBasicProxyExt`** (el "basic driver" de Tridium) — distinto del N-driver de [Bloque 78] (C-Bus/EnOcean) y distinto del BACnet de [Bloque 77] (Spyder). Refina la observación del [Bloque 78]: Centraline NO usa un framework único; elige por driver.
- `clOnboardIO` sobre **`BNNetwork`/`BNDevice`** (N-driver), igual que C-Bus/EnOcean.

`clIOcreation` es el **nexo común** (registry-discovered) que abstrae ambos en el Workbench vía la interfaz `BIIOCreationHelper` — un módulo no depende del otro en compilación; se descubren en runtime.

> Distinción conceptual: **OnboardIO** = el I/O que trae el propio controlador en su placa (canales físicos del hardware). **PanelBus** = módulos de I/O externos que se enchufan a un bus RS-485 para ampliar la capacidad. Complementarios.

---

## 86.2 — clPanelBus: driver del bus RS-485 (CLIO legacy + Snap-on IO) `[CERT]`

`BPanelbusNetwork extends BBasicNetwork implements BIPanelbusHelperParent, PanelbusAnalyzer, BINetworkDetails` (:240); `BPanelbusDevice extends BBasicDevice implements PanelbusMessageConst, BIHonProjectExport` (:213); `BPanelbusProxyExt extends BBasicProxyExt implements ..., IIOProxyExt` (:89, abstract). Cada `BPanelbusDevice` = un módulo de I/O del bus.

**Hardware `[CERT-a]`** — dos generaciones:
- **CLIO legacy** (DIN-rail, switch hex 0x0-0xF): `CLIOP821` (8 AI), `822` (8 AO), `823` (12 DI), `824` (6 DO), `825R` (3 floating/modulating), `830/831` (mixto: 8AI+8AO+12DI+6DO). Variantes `R` con override manual.
- **Snap-on IO** (nueva gen, display HOA en variantes `IOD-`): `IO-xUIO-S` (universal I/O 4/8/16), `IO-xDI-S`, `IO-xDOR-S` (relé), `IO-4DORE-S` (relés enhanced), `IO-8AO-S`, `IO-16UI-S`.

**Protocolo `[CERT-a]`**: bus **RS-485** (puertos `RS485_R`/`RS485_1..6`, hasta 7 por JACE; baud lo maneja el módulo de plataforma `platPanelbus`, no el driver). Frame binario de 8 campos `[msgType][devType][address][dataLen][command][status][data][checksum]`, **checksum = complemento a dos** de la suma (`~sum+1`). Protocol v1 rev51. Master-slave síncrono, inter-msg 10 ms, timeout 2 s, 1 retry. 9 tipos de mensaje incluyendo **star/ring token** (topología star y ring). Comandos: `READ_INPUT 0x51`, `WRITE_OUTPUT 0x91`, `READ/WRITE_POINT_CONFIG`, `DEVICE_PING_REQ 0x06`, `WRITE_CHALLENGE_DATA 0x20`. Máx 16 dispositivos/bus por tipo; addr 0xFF = broadcast interfaz, 0x80 = broadcast Snap-on.

**Modelo de puntos `[CERT-a]`**: 17 `BPanelbusPointType` con proxy exts específicos por hardware (AI/AO/DI/DO/floating/universal). Config por punto muy rica: **>60 tipos de sensor** (NTC 10k/20k, PT1000/PT100/PT500, NI1000, Balco500, voltaje 0-10/2-10V, corriente 4-20mA, custom con linearización tabular `BTabularConversion`), offset, contactMode, sendOnDelta, safety position. Poll groups dedicados (numeric/boolean read/write/alarm; normal 10s, slow 60s). **Safety position** ante timeout de comunicación (`controllerToIoCommunicationTimeout` ≥60s).

**Discovery `[CERT-a]`**: `BPanelbusDeviceDiscoveryJob extends BSimpleJob` (:19) hace ping por modelo×dirección (timeout 250 ms) → crea entries con firmware/serial. `BPanelbusPointDiscoveryJob` infiere los puntos de la definición del modelo (no pollea el bus). `syncConfiguration()` escribe la config al módulo físico (rastreado por `configId`/`remoteConfigId`).

**WB `[CERT-a]`**: `BPanelbusDeviceManager`/`BPanelbusPointManager`, `BPanelbusAnalyzerView` (sniffer de frames en vivo, 500 registros), generador de template Excel de etiquetas. **Licencia `honHit`** (Honeywell Integration Tool) requerida si un punto tiene slot `uid`.

---

## 86.3 — clOnboardIO: el I/O integrado del Eagle Hawk (JNI) `[CERT]`

`BOnboardIONetwork extends BNNetwork implements BIClNPollable` (:28); `BOnboardIODevice extends BNDevice` (:41). **Singleton estricto** (1 network + 1 device por station) `[CERT-a]`.

**Hardware `[CERT-a]`**: el I/O físico onboard de los controladores **CentraLine CLAXEH (Eagle Hawk) Series 14 y 26**. Driver vía **JNI** sobre `libOnboardIO-npsdk.so` — **solo corre en QNX/Linux** (el SO del controlador); en Windows/offline `isValidPlatform()=false`. La definición de canales se lee de `module://clOnboardIO/configFiles/<model>.xml`.

**Canales `[CERT-a]`**: 4 tipos hardware — `universalInput` (configurable: AI 0-10V/0-20mA, DI, contador, NTC10k/20k), `analogOutput`, `binaryInput`, `binaryOutput`. La `characteristic` del canal es un `BDynamicEnum` cuyos valores válidos cambian según el canal. Props por punto: offset, contactMode (NO/NC), direct/reverse, safety position, sensor-fail detection.

**Watchdog `[CERT-a]`**: la network envía `notifyAlive()` al hardware cada `safetyPositionTimeoutInSec/2`; si N4 deja de latir, el hardware va a posición de seguridad. La interfaz `BIClNPollable extends BIPollable` añade `doPoll(pollSchedulerId)` para multiplexar schedulers (id 2 = watchdog, id 1 = alarmas). Licencia `honHit`.

---

## 86.4 — clIOcreation: el framework UI común de creación de puntos `[CERT]`

`BEnhancedWireSheet extends BWireSheet` (:59); `BReferencePoint extends BComponent` (:25, abstract) con subtipos `BRef{Numeric,Boolean,Enum}{Point,Writable}` y especializaciones `BOnboardIORef*`/`BPanelbusRef*`. NO gestiona hardware — es Workbench puro.

**Qué hace `[CERT-a]`**: reemplaza el wiresheet estándar; al agregar un punto de I/O nativo crea automáticamente un `BReferencePoint` (proxy en el wiresheet linkado por `BLink` al punto real). **Abstrae OnboardIO y PanelBus** vía la interfaz `BIIOCreationHelper extends BInterface`: descubre en el Niagara registry qué helpers hay (`OnboardIOHelper`/`PanelbusHelper`) y enriquece el menú "New" en consecuencia. Persiste defaults (safety positions, LED) en `!iocreation/config.bog` (`BIOCreationConfig` con `onboardIOConfig`/`clioConfig`/`snaponIOConfig`).

> Gotcha verificado `[CERT-a]`: `createPanelbusPointsIfOnbaordIOPointLimitIsReached` — si el OnboardIO llega a su límite de puntos, clIOcreation **desvía la creación a PanelBus** automáticamente. El framework trata onboard + bus como un pool unificado de I/O.

---

## 86.5 — Síntesis: la capa de I/O eléctrico Centraline + export a cloud

**El stack de I/O Centraline en una frase**: el controlador Eagle Hawk expone su I/O onboard por `clOnboardIO` (JNI/QNX) y amplía capacidad con módulos RS-485 por `clPanelBus` (CLIO/Snap-on); `clIOcreation` unifica ambos en el Workbench creando ReferencePoints sobre el wiresheet, tratándolos como un pool único de I/O físico.

**Por qué importa (cierra el modelo de campo)**:
- Los [Bloques 77-78] cubrieron drivers de **buses de dispositivos** (Spyder BACnet/LON, C-Bus, EnOcean). Este bloque cubre el **I/O eléctrico crudo** (los terminales donde se cablean sensores/actuadores) — el nivel más bajo del edificio.
- Confirma que Centraline **mezcla frameworks de driver** según el caso: BACnet (Spyder), N-driver (C-Bus/EnOcean/OnboardIO), basicdriver (PanelBus). No hay un único patrón — al portar/depurar, identificar primero el framework base.

**Integración cloud (liga [Bloques 82-85])**: `BPanelbusDevice implements BIHonProjectExport` y `BIBTPPanelBusHandler`/`HonTerminalBEATSAdv` formatean los terminales para **BTP/BEATS** (Building Technology Platform / Beats) — el mismo destino Honeywell Forge/Sentience de la familia cloud. El I/O físico se etiqueta y exporta como `HonDevice`/`HonTerminal` al modelo cloud (conecta con el model sync del [Bloque 85] y los tags `hon:` del [Bloque 82]).

**Para MX60 / Honeywell**: si un site usa Eagle Hawk + PanelBus, este bloque es la referencia para entender de dónde vienen los puntos físicos. El `BPanelbusAnalyzerView` (sniffer de frames) es la herramienta de diagnóstico de bus. Atención operacional: **licencia `honHit`** obligatoria (sin ella el device se deshabilita), y el I/O onboard **solo funciona en el hardware QNX/Linux** (no se puede probar en un Supervisor Windows).

**Pendiente conocido**: la familia de **aplicaciones HVAC Centraline** (`clHVAC*`, ~750 java en 9 módulos: heating/AC/chiller/energy/room control) es un artefacto distinto (lógica de control pre-armada, no drivers) — candidato a bloque futuro. El protocolo binario PanelBus se citó por frames/comandos vía sub-agente `[CERT-a]`; el detalle de cada `BPanelbusPointConfig` por sensor no se enumeró exhaustivamente.
