# Bloque 103 — `honeywellFunctionBlocks`: el motor de control DDC propietario de Honeywell (kitControl propietario) — `BFunctionBlock` + datatypes Negatable + 32 converters, deofuscado

> Investigación empírica del módulo OEM Honeywell **`honeywellFunctionBlocks`** (158 java en `com.honeywell.honfunctionblocks`): el **motor de Function Blocks DDC** de Honeywell, análogo propietario directo de `kitControl` de Tridium. Es la **base de la lógica de control** de los controladores Spyder e IPC 3036 dentro de Niagara — lo consumen kitCat ([Bloque 101]), ipcCommBus ([Bloque 99]) y el migrador Spyder→IPC ([Bloque 100]). Los FB se ejecutan **en el station**, no se descargan al hardware.
>
> 1 módulo (`honeywellFunctionBlocks`, `-rt`/`-ux`/`-wb`). Paquetes: `converters` (32), `fbs/{math,control,analog,zonecontrol,logic,datafunction,builtin}`, `datatypes` (10), `utils`.
>
> Fuentes: `organized/honeywellFunctionBlocks/honeywellFunctionBlocks-rt/vineflower/com/honeywell/honfunctionblocks/...`.
> Método: 1 sub-agente Explore + **verificación directa** de cada `extends`, las interfaces de ejecución, los datatypes y el license bypass. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (catálogo de FB, ciclo de ejecución, converters, seguridad RPC) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 102]. Destila una **dependencia compartida central** que el barrido reveló mencionada (B99) pero no destilada — 158 clases. Conecta [Bloque 99]/[Bloque 101] (IPC/kitCat que la ejecutan), [Bloque 100]/[Bloque 96] (migrador/Venom que la traducen), [Bloque 77] (Spyder, el origen del modelo de control), [Bloque 75] (seguridad).

---

## 103.1 — Qué es: el DDC engine que corre en Niagara `[CERT]`

Motor de bloques de control programable (DDC) propietario de Honeywell, análogo a `kitControl` pero con tipos y ejecución propios. **Los FB son `BComponent` vivos en el station space y se ejecutan en el runtime Niagara** (JACE/Station) — NO se compilan ni descargan al hardware `[CERT-a]`. Raíz verificada:

```
BFunctionBlock extends BComponent implements IHoneywellExecutionBlock, IHoneywellComponent  (abstract, fbs/…:77)  [CERT]
```

**Contrato de ejecución `[CERT]`** (interfaces verificadas):
- `IHoneywellComponent`: `executeHoneywellComponent(BExecutionParams)` + `initHoneywellComponent(...)` + `isOutputPropertiesOverridden()`.
- `IHoneywellExecutionBlock`: `updateBlockExecutionOrder(int)` (el engine ordena los FB antes del primer ciclo).
- `BExecutionParams extends BStruct` (verificado): transporta `iterationInterval` (100-65535 ms, def 1000) — los FB con estado (PID, integradores, Stager) lo usan para calcular ganancias por iteración.

**Ciclo `[CERT-a]`**: el engine DDC (vive en el módulo IPC) invoca `executeBlock(BExecutionParams)` → `doExecuteBlock()` → si no hay override, `executeHoneywellComponent()`. Propiedades base: `ExecutionOrder` (orden en el ciclo), `toolVersion` (`BHonVersion`), `OverrideExpiration` (`BAbsTime`). El gate de licencia se chequea en cada ejecución; sin licencia → outputs en `fault`.

---

## 103.2 — El catálogo de Function Blocks `[CERT]` + `[CERT-a]`

Patrón universal `[CERT-a]`: cada FB extiende `BFunctionBlock` (o un abstract intermedio), clasifica sus slots en `getInput/Output/ConfigPropertiesList()`, e implementa `executeHoneywellComponent()` con lógica pura. Los slots de salida usan field editors custom con botón de override (`StatusValueOutSlotFE` AX / `OutSlotWidget` UX) y flag `OPTIONAL` (no dan fault si no están enlazados).

Categorías (intermedios verificados `BArithmetic extends BFunctionBlock`, `BLogicBlock extends BFunctionBlock` `[CERT]`):

| Paquete | FBs representativos `[CERT-a]` |
|---------|--------------------------------|
| `fbs/math` (`BArithmetic`) | Add/Subtract/Multiply/Divide (8 entradas), SquareRoot/Log/Exponential, Ratio, Limit, DigitalFilter, **Enthalpy**, FlowVelocity (+ `doTailOperation`: abs/ceil/floor/round) |
| `fbs/control` | **`BPid`** (verificado, PID: tr/intgTime/dervTime/deadBand/bias, output %), **`BAia`** (Analog Integrating Actuator = floating control 3-cable), **`BStager`** (etapas, minOn/Off, hyst), Cycler, FlowControl, RateLimit, RuntimeStrategy/FOFOStrategy (lead-lag) |
| `fbs/analog` | Compare, DecisionBox, Select, Switch, Edge, HystereticRelay, AnalogLatch, Max/Min/Average, PrioritySelect, Encode |
| `fbs/zonecontrol` | **`BOccupancyArbitrator`** (schedule + WMOverride + NetworkOcc + sensor → EFF_OCC), TemperatureSetpointCalculator, GeneralSetpointCalculator, SetTemperatureMode |
| `fbs/logic` (`BLogicBlock`) | And/Or/Xor (6 entradas `BNegatableStatusBoolean`, trueDelay/falseDelay), OneShot |
| `fbs/datafunction` + `fbs/builtin` | Counter, Override, RuntimeAccumulate; **`BTuncos`** (Time Until Next Change Of State, minutos al próximo cambio de schedule) |

`BPassThru extends BFunctionBlock` (verificado, en `utils`): bridge in→OUT con `executeHoneywellComponent()` vacío (propaga reactivamente en `changed`); remueve la acción Override. Base del `BKitCatPassThru` ([Bloque 101]).

---

## 103.3 — Los datatypes propietarios: status + negate `[CERT]`

Jerarquía verificada (extienden los status types de Niagara):
```
BStatusNumeric → BHonStatusNumeric → BNegatableHonStatusNumeric (implements INegatableStatusValue)
BStatusBoolean → BHonStatusBoolean → { BNegatableStatusBoolean (implements INegatableStatusValue),
                                       BFiniteStatusBoolean → BNegatableFiniteStatusBoolean }
BStatusEnum    → BHonStatusEnum → { BStatusOccupancyStateEnum, BStatusGenSetpointCalcEnum }
```

Qué aportan sobre los estándar `[CERT-a]`:
- **`BHonStatusNumeric`** (verificado `extends BStatusNumeric`): `getValue()` aplica `BigDecimal.setScale(precision, HALF_UP)` (6 dígitos sig. por defecto) → evita drift de coma flotante en comparaciones.
- **`INegatableStatusValue`**: el flag `negate` se guarda en los **`BFacets` del slot** (no como propiedad separada) → permite invertir una entrada en el wire sin interponer un bloque NOT, **igual que en los PLCs Spyder**. Expuesto por RPC (`setNegateValueFromRPCCall`).
- **`BFiniteStatusBoolean`** (verificado `extends BStatusBoolean`): booleano con estados finitos conocidos (no null/indeterminate), para slots disable/override.
- Heredan el status Niagara completo (`ok/fault/override/null`) → degradación graceful si un input entra en fault.

---

## 103.4 — Los 32 converters `[CERT-a]`

Todos `extends BConverter implements BIAgent`, registrados como agente sobre `baja:ConversionLink` con `@Adapter(from,to)`. Tres grupos: (1) Niagara estándar → Honeywell (`BStatusNumericToHonStatusNumeric`…), (2) → tipos Negatable, (3) enum-to-enum cross-domain (`BHonStatusNumericToStatusOccupancyStateEnumConverter`, …SetTemperatureMode…). El volumen (32) refleja la necesidad de conectar FB Honeywell a puntos BACnet/Niagara estándar sin conversión manual. `utils`: `LimitCheckUtil` (valida rango + detecta NaN/Infinity), `LicenseHandler`, lexicon/logger.

---

## 103.5 — Seguridad `[CERT]` + `[CERT-a]`

**[ALTO CERT] License bypass por brand "Webs/WebsOpen".** `LicenseHandler.java:13`: `allowedBrands = {"Webs", "WebsOpen"}`; `isLicensed()` devuelve `true` si el JACE tiene ese brand (feature `Tridium/brand`) — sin necesidad de la licencia `Honeywell/IPVAV`. Afecta el gate de cada `executeHoneywellComponent()`. (Mismo patrón de brand que vimos en [Bloque 99]/[Bloque 101].)

**[MEDIO CERT-a] RPC que altera la lógica sin auditoría.** `setNegateValueFromRPCCall` (`@NiagaraRpc permissions="RWI"`, verificado que hay `@NiagaraRpc` en `BFunctionBlock`): cualquier usuario RWI invierte silenciosamente la lógica de un slot Negatable (disable, in1-6, overrideOff) en tiempo real; el cambio se guarda en `BFacets`, **no queda auditado** como cambio de valor. `getFacetsDataToRPCCall` expone facets (rangos/unidades) de cualquier slot por web RPC (reconocimiento).

**[MEDIO CERT-a] Override de salidas sin log.** `doOverride()` marca outputs `setStatusOverridden(true)` sin alarma ni auditoría → se puede silenciar un lazo PID/Stager/OccupancyArbitrator sin traza más allá del flag. `getFunctionBlockOverrideQueryOrd()` devuelve una BQL que lista todos los FB en override (info de estado del edificio si llega a UI no autenticada). Race latente en `overrideFBTimer` (no sincronizado).

---

## 103.6 — Conexiones

- **[Bloque 99]** (ipcCommBus): contiene el **DDC Engine Thread** que ejecuta estos FB; los expone vía PassThru F1.
- **[Bloque 101]** (kitCat) / **[Bloque 96]** (Venom): consumen los datatypes (`BHonStatusNumeric`, `BNegatableStatusBoolean`) y PassThru.
- **[Bloque 100]** (ipcMigrator) / **[Bloque 96]**: el `BFunctionBlockMigrator` traduce ~37 FB Spyder a esta red de `BFunctionBlock`.
- **[Bloque 77]** (Spyder): el modelo de control (negate en el wire, floating control Aia) replica el del PLC Spyder físico.
- **[Bloque 75]** (seguridad): suma license bypass por brand + RPC sin auditoría + override sin log.
