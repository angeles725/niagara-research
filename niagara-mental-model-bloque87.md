# Bloque 87 — Librería de control HVAC Centraline ("Eagle control primitives"): function blocks propios + know-how HVAC codificado (clHVAC + dominios) deofuscados

> Investigación empírica de la **librería de lógica de control HVAC de Centraline/Honeywell** — un framework de function blocks **propio, independiente de kitControl**, más una librería de aplicaciones HVAC pre-armadas (calefacción, AHU, chillers, energía, salas) que codifica el know-how de ingeniería HVAC de Honeywell en bloques reutilizables.
>
> Núcleo: `clHVAC` ("Eagle control primitives", 103 function blocks) + `clHVACGeneral` (10 macros). Dominios: `clHVACHeating` (171), `clHVACAirConditioning` (135), `clHVACNordicAirCondition` (86), `clHVACChiller` (32), `clHVACEnergyManagement` (11), `clHVACRoomControl` (3), `clHVACNordicGeneral` (7).
> Decompilados Centraline limpios; nombres de parámetros HVAC en inglés en el lexicon.
>
> Fuentes: `organized/clHVAC*/<m>-rt/vineflower/cl/hvac/...` (+ `module.xml`, `*.lexicon`).
> Método: 2 sub-agentes + **verificación directa** de la clase base, el service de ejecución y una muestra de bloques de dominio (`grep ^public class`). `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (algoritmos, parámetros, lexicon) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 86]. **Contrasta con [Bloques 14-15]** (kitControl/control framework Tridium) — es la alternativa Centraline. Conecta [Bloque 7] y [Bloque 86] (los puntos de I/O sobre los que opera esta lógica).
>
> **Actualizado por [Bloque 540]** (focus kitControl, KC5): §87.3/§87.4 (las secuencias de control HVAC) estaban en `[CERT-a]` (cita de sub-agente, no re-verificada). B540 las DECOMPILA y las eleva a `[CERT]` — curva de calefacción `BCmVTB_HtgCirc` (interpolación lineal 2-puntos OAT→Tsupply), damper de mezcla/economizer `BCmDMB_MixingDamper`, secuenciación lead-lag de 12 chillers `BCmSQA_ChillerSeq`. Aclara los conteos: "264 vf" = rt+wb; "103 function blocks" = las primitivas `BControlFunctionSupport` de `-rt` (250 clases); 83 bloques `BCm*` de dominio en los sub-módulos. Base class y engine (§87.2) CONFIRMADOS byte a byte.

---

## 87.1 — La librería en conjunto: un kitControl paralelo de Honeywell `[CERT]`

`clHVAC` se describe en su `module.xml` como **"Eagle control primitives"** (v4.20.0.4.1.5) — "Eagle" es el nombre interno del proyecto Centraline. Es una **librería de function blocks completa y autónoma**, NO una extensión de kitControl:

| Componente | Clase verificada | Rol |
|------------|------------------|-----|
| Base de todo bloque | `BControlFunctionSupport extends BComponent implements IEnvironment` (:37) | slots/ejecución/status comunes |
| Motor de ejecución | `BControlProgramService extends BAbstractService` (:30) | clock central que ejecuta los bloques |
| Bloque ejemplo | `BSwitchingLogic extends BControlFunctionSupport` (:61), `BZeroEnergyBand extends BControlFunctionSupport` (:16) | function blocks |
| Macro de dominio | `BCmVTB_HtgCirc extends BControlFunctionSupport implements IEnvironment` (:40) | aplicaciones HVAC |

**HALLAZGO `[CERT]`**: NO hereda nada de kitControl. `BControlFunctionSupport extends BComponent` directamente e implementa la interfaz **propia** `cl.hvac.base.IEnvironment`. La única dependencia de `control-rt` es para `BIWritablePoint`/`BPriorityLevel` (manipular priority arrays BACnet de puntos enlazados) — no usa `BControlPoint` ni el framework de kitControl. Es un **segundo ecosistema de control** que convive con el de Tridium ([Bloques 14-15]).

**Por qué importa**: esta librería es **el know-how de ingeniería HVAC de Honeywell/Centraline codificado en software**. Cada `BCm*` de dominio encapsula una secuencia de control real (curva de calefacción, economizer, secuenciación de chillers, optimum start) con decenas de parámetros nombrados — es la "biblioteca de aplicaciones" que un ingeniero de Centraline ensambla en el wiresheet.

---

## 87.2 — El motor: BControlFunctionSupport + ejecución + sub-frameworks `[CERT]`

**`IEnvironment`** (interfaz base) `[CERT-a]` provee a cada bloque: `getCycleTime()` (segundos desde última ejecución), vector de parámetros `getParameter(i)/setParameter(i,v)` (los `BCm*` guardan su config interna en un `float[]`, no en slots), acceso a priority arrays BACnet (`getHighestPriority`/`getPriorityValue`), `getTuncosInfo()` (próximo cambio de schedule) y logging.

**`BControlFunctionSupport`** `[CERT-a]` aporta: la `Action execute` + `doExecute()` (que cada bloque sobreescribe), alta/baja en el service según `NeedsPeriodicExecution()`, manipulación de priority arrays vía links, i18n de slots desde el lexicon `clHVAC`, slots dinámicos (`addInputSlot`), e icono `HoneywellLibrary.png`.

**Ejecución — `BControlProgramService`** `[CERT-a]` (clave, distinto de kitControl):
- Un **clock central** (`Clock.schedulePeriodically`, ciclo default **2 s**, rango 1-10 s) recorre una `functionList` y llama `doExecute()` en cada bloque registrado (yield cada 100 bloques). Mide `execTime`; error si supera el ciclo.
- **Dos modos por bloque**:
  - **Periódico** (`NeedsPeriodicExecution()=true`): se registra en la lista, corre cada 2 s. Usado por bloques con tiempo (Timer, Integral, Differential, DutyCycle, HeatingCurve, Eoh/Eov).
  - **Event-driven** (`=false`, la mayoría): NO se registra; corre una vez al arranque y luego por el callback `changed()` cuando cambia un input. Bloques puros (math, lógica, ZeroEnergyBand, Economy).
- **NO es link-based como kitControl** (no hay subscribe/propagate propio): clock central para temporales + event-callbacks de Niagara para el resto. La propagación entre bloques usa los links nativos del wiresheet.

**Catálogo de 103 function blocks `[CERT-a]`** (categorías): aritméticos (Add/Multiply/XRootY/Truncate/Factorial...), trigonométricos (Sin/Cos/Tan/Arc*), estadísticos/multi-entrada (Max/Min/Average/NOfM/Parity), lógicos (And/Or/Xor/Compare), flip-flops (RS/JK/AnyChange), bits (BitAnd/Or/Test/Set), ventana/histéresis/límite, temporales (Timer/Counter/Monoflop/Delay/Cycle), rampas (Ramp/ValueRamp), **PID** (Pid/PidPlus), integral/diferencial, switches/mux (Split/Merge/Idt), conversión/curvas (Linear/Polynomial/**HeatingCurve**/HcWithAdaption), **psicométricos** (Enthalpy/Dewpoint/Economy/Eoh/Eov), HVAC complejos (**ZeroEnergyBand**, **NightPurge**), registros globales, prioridades BACnet, schedule/Tuncos, evaluador de ecuaciones (`BMat` con parser propio).

**Sub-frameworks notables `[CERT-a]`**:
- **GlobalRegister** `[CERT]`: `ControlFunction.globalRegister = new float[1024]` (estático) — variables globales compartidas entre bloques vía `BWrGlobalRegister`/`BRdGlobalRegister` (índice + modo Write/Add). Permite comunicación fuera del wiresheet.
- **SwitchingLogic**: tabla de verdad **N filas × M columnas** con condiciones **relacionales** (no solo bool: <, <=, >=, >, ==, !=) + histéresis + delays independientes por fila/columna/salida; cada columna AND-combina filas (TRUE/FALSE/DONT_CARE), OR de columnas. Editor WB propio (`BSwitchingLogicEditor extends BWbComponentView`). Más potente que una truth-table binaria.
- **Tuncos**: cache del próximo cambio de schedule (`TuncosInformation`: nextChangeTime + nextValue) que permite a bloques anticipar el estado futuro (clave para optimum start).

**`clHVACGeneral`** `[CERT-a]`: 10 macros `BCm*` (PID extendido, control de bombas gemelas/única, filtro de OAT, sensores T7460) compuestos internamente de primitivos `Cf*`; llevan `macroId` único + `versionInfo` ("51.xx ... 2015-2019") — son macros del proyecto Centraline original portados.

---

## 87.3 — Los dominios HVAC: know-how de ingeniería codificado `[CERT-a]`

Todos los `BCm*` de dominio son `extends BControlFunctionSupport implements IEnvironment` (verificado en muestra) y dependen solo de `clHVAC` + `baja`. Se comunican entre módulos por señales en el wiresheet, no por imports. Cada uno encapsula una secuencia de control real con parámetros nombrados tipo `HCA-01`, `SAT-05`, `CYC-06`:

**`clHVACHeating` (171) — calefacción hidráulica**:
- **Curva de calefacción con compensación climática** (`BCmVTB_HtgCirc`/`Optim`): el corazón — calcula el setpoint de agua mezclada desde la OAT filtrada vía conversión lineal, parámetros `HCA-01` curvatura / `HCA-02` pendiente / `HCA-04` adaptación. La variante `Optim` añade **modelo de edificio adaptativo** (optimum start con aprendizaje, `BMOD-01..05`) y optimum stop usando `TuncosInformation`.
- Calderas: `BCmBOA_StagedBoiler` (secuenciación por etapas, rotación cíclica, anti-condensación), `BCmBOB_ModBoiler` (modulante con rampa máx).
- ACS con **anti-legionela** (`BCmBOD_HeatExch_DHW`: setpoint/tiempo/duración), buffers térmicos (estratificados 4 capas), control solar (diferencial panel/buffer compensado por OAT).

**`clHVACAirConditioning` (135) — unidades de tratamiento de aire (AHU)**:
- **Control en cascada** (`BCmCSA/CSB_CascContr`): bucle exterior sala→SAT, bucle interior SAT→actuadores en secuencia calor→ERC→frío (`SAT-05..12`), con **economizer** (`SYS-01`), compensación invierno/verano y override por **CO2**.
- **Recuperadores de energía** (4 tipos: Wheel/Plate/Glycol/Water) con protección antihelada.
- Ventiladores (1/2 etapas + modulante), dampers de mezcla, baterías (pre/re-heater con anti-frost), humidificación spray/steam con zona muerta.
- **`BCmPSA_PlantModeOptim`**: optimum start/stop calor y frío, pre-heat/pre-cool adaptativos (`EOV-*`), duty cycling, night setback/purge.

**`clHVACChiller` (32) — plantas de frío**:
- **Secuenciación con rotación lead/lag** (`BCmSQA_ChillerSeq`, hasta 12 unidades): rotación por horas de funcionamiento (`CYC-06: max runtime difference` fuerza changeover para igualar desgaste), changeover por fallo (`ALM-05`).
- Cálculo de carga (PID o directo), **free cooling** por grupo (con bulbo húmedo), control individual de chiller (secuencia condensador→bomba→válvula→chiller), **cálculo de COP** y capacidad real (calorimétrico).

**`clHVACEnergyManagement` (11) — medición (NO control)**:
- Grados-día HDD/CDD, contadores de pulsos con sub-metering, estadísticas día/semana/mes/año, balance de energía solar con **ahorro de CO2**.
- Nota `[CERT-a]`: el nombre es impreciso — NO implementa demand limiting/optimum start (esos están en PlantMode y HtgCircOptim); es solo instrumentación/reporte.

**`clHVACRoomControl` (3) — terminales de zona** (fan coil/ventiloconvector, 2/4 tubos):
- Mini-curva de calefacción por terminal + control de enfriamiento con **gestión de punto de rocío** (anti-condensación superficial). Interopera con controladores Honeywell **XL10/IRM** (salidas `ApplicModeXL10`/`ApplicModeIRM`).

---

## 87.4 — La variante Nordic: HVAC para clima frío `[CERT-a]`

`clHVACNordicAirCondition` (86) + `clHVACNordicGeneral` (7) son la adaptación para mercados escandinavos. Diferencias clave verificadas:
- **Modo estacional explícito**: `BCmSWM_SummerWinterMode` (clHVACNordicGeneral) genera `HVAC_Mode` (por fecha, OAT o señal externa) que TODOS los bloques Nordic consumen como input directo — en vez de inferir verano/invierno de la OAT como el módulo base.
- **Control de caudal volumétrico** (`BCmSPB_AirFlowControlNordic`): único sin equivalente base; calcula caudal real desde presión diferencial con **compensación por densidad del aire** (relevante en frío), y fuerza velocidad mínima si la OAT es muy baja (`FSLIM-01`).
- **Eficiencia del recuperador** (`BCmERC_Efficiency`): calcula eficiencia real con las 4 temperaturas del proceso + alarma.
- **Bloqueo de etapas DX por OAT** (`BCmSCA_DXCoolingNordic`): no arranca frío mecánico bajo cierto umbral.
- Protección antihelada mejorada en baterías y ventiladores (fuerzan velocidad mínima en vez de apagar, para no congelar la batería).
- Utilidad `BCmLIN_LinearChar_5points` (curva lineal a trozos de 5 puntos) para calibración de sensores/válvulas.

---

## 87.5 — Síntesis: el segundo ecosistema de control + valor

**clHVAC es el "kitControl de Honeywell"** — un framework de function blocks completo, paralelo y técnicamente distinto al de Tridium:

| | **clHVAC (Centraline/Eagle)** | **kitControl (Tridium, [Bloques 14-15])** |
|---|---|---|
| Base de bloque | `BControlFunctionSupport extends BComponent` | `BComponent`/`BControlPoint` |
| Ejecución | clock central 2 s (`BControlProgramService`) + event-driven | link-based (subscribe/propagate) |
| Config de macros | `float[] parameterList` interno | slots Niagara |
| Comunicación global | `globalRegister[1024]` estático | links explícitos |
| Aplicaciones HVAC | librería pre-armada (heating/AHU/chiller...) | el integrador las arma |
| Licencia | `honHit` (CentraLine/ComfortPoint/etc.) | incluida en Niagara |

**El valor real (para MX60 / Honeywell)**: esta librería es **know-how de ingeniería HVAC empaquetado** — décadas de secuencias de control (curvas de calefacción, economizers, secuenciación de chillers con igualación de desgaste, optimum start con modelo de edificio, anti-legionela, free cooling) codificadas con parámetros nombrados y probadas en campo. Para un proyecto Honeywell es la vía rápida a control HVAC correcto sin reinventarlo en kitControl. Para entender una station Centraline existente, los bloques `BCm*` + sus parámetros (`HCA-*`, `SAT-*`, `CYC-*`) son el mapa de qué hace el control.

**Atención operacional**:
- **Licencia `honHit` obligatoria** (igual que [Bloque 86]) — sin ella el `BControlProgramService` va a fault y la lógica no corre.
- El **clock central de 2 s** es un punto único: si hay demasiados bloques periódicos y `execTime` supera el ciclo, se degrada todo el control (a diferencia de kitControl distribuido). Vigilar `execTime` en stations grandes.
- `globalRegister[1024]` es estado **global compartido sin namespacing** — colisiones de índice entre programas distintos son un riesgo de diseño.

**Pendiente conocido**: el detalle interno de cada algoritmo (la matemática exacta de la curva de calefacción adaptativa, el modelo de edificio `BMOD`, el PID) se citó por parámetros del lexicon vía sub-agente `[CERT-a]`; no se decompiló la fórmula línea a línea. Los ~750 bloques de dominio se cubrieron por familia/representante, no exhaustivamente. Con esto, la familia Centraline (drivers [Bloque 78], I/O [Bloque 86], control [Bloque 87]) queda mapeada de campo a aplicación.
