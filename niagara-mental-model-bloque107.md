# Bloque 107 — `ascCommon`/`ascBacnet`/`ascLon` (ASCOT): la herramienta de configuración del controlador **Stryker** (application-specific por wizard) — **corrigendum al Bloque 32**, deofuscada

> Investigación empírica de la familia OEM Honeywell **ASCOT** (`com.honeywell.ascot`): `ascCommon` (1741 clases, *"Honeywell Stryker controller common module"*, symbol `asc`), `ascBacnet` (147, *"Honeywell Stryker controller Bacnet modules"*, `ascb`) y `ascLon` (58). Vendor Honeywell `4.14.0.7.1.67`, **build 2024-10**, `runtimeProfile=wb` (herramienta Workbench, no corre en el JACE).
>
> **CORRIGENDUM `[CERT]` al [Bloque 32]**: ese bloque clasificó `ascCommon.jar` (5.3 MB) como *"ASC/CentraLine legacy AX pre-N4, code AX convertido, TODO"*. Es **INCORRECTO**: `buildMillis=1728966604433` = **8-oct-2024**, compilado en Azure (`buildHost=azu-hce-vbf-w14`), deps N4 4.0+. ASCOT es la herramienta **moderna** de configuración del controlador Stryker, no legacy. El tamaño grande no es legacy — es la cantidad de aplicaciones HVAC parametrizables.
>
> **Es el complemento del [Bloque 106]**: si ese destiló la herramienta del **Spyder** (controlador de programación libre), este destila la del **Stryker** (controlador de aplicación fija). Juntos cubren las dos líneas de controladores programables/configurables de Honeywell.
>
> Fuentes: `organized/{ascCommon,ascBacnet,ascLon}/<m>/vineflower/com/honeywell/ascot/...`. Decompilación vineflower **limpia** (0 fallos).
> Método: 1 sub-agente Explore profundo + **verificación directa** del cero-function-blocks, del uso de `genericUIFramework` (extends `AbstractAction`), del download objeto-por-objeto (`writeMultipleProperties`) y de la dependencia de `airFlowBalancer`. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (catálogo de panes, enums, jerarquías de validators, conteos) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 106]. **Conecta fuerte**: [Bloque 106] (Spyder — contraste programable vs application-specific), [Bloque 102] (`genericUIFramework` — su motor MVC), [Bloque 96] (`airFlowBalancer`/Venom — dependencia, vista de balancing), [Bloque 32] (corrigendum), [Bloque 75] (seguridad).

---

## 107.1 — Qué es: el wizard de configuración del Stryker `[CERT]`

ASCOT es un **wizard Workbench** (`runtimeProfile=wb`) que configura el controlador **Honeywell Stryker** (el hermano "application-specific" del Spyder; ya aparecía en [Bloque 96]/Venom como "Spyder/Stryker"). No es un driver ni corre en el JACE: es la herramienta N4 con la que el ingeniero parametriza una aplicación HVAC pre-construida y la descarga al dispositivo.

El Stryker soporta **2 familias de aplicación** (no hay FCU ni RTU) `[CERT-a]`:
- **CV-AHU** (Constant Volume Air Handling Unit): panes `BCvahu*Pane` (configuración, analog/digital inputs, outputs, economizer, equip control, HC settings, PID, zone options, misc). Equipos: `CONVENTIONAL`/`HEAT_PUMP`.
- **VAV** (Variable Air Volume): panes `BVav*Pane` (input, output, control parameters, temp/flow setpoints, custom wiring/sensor, PID). Cajas: `SINGLE_DUCT`.

Puntos de entrada/salida del wizard `[CERT]`: `LoadConfigAction` y `SaveConfigAction` (abstractas, `extends AbstractAction`) — cargan desde / escriben al dispositivo en cada paso.

---

## 107.2 — El hallazgo central: **Stryker = application-specific** (no programable) `[CERT]`

Lo más importante, y el contraste que cierra el panorama de controladores Honeywell. **ASCOT no tiene function blocks. Cero.** `grep` de `BFunctionBlock|honfunctionblocks|BNanoFunctionBlock|Kingfisher|honeywellSpyderTool` sobre las 1946 clases de la familia asc* = **0 resultados** `[CERT]`.

El Stryker es un controlador de **aplicación fija**: el firmware ya trae la lógica de control CV-AHU y VAV compilada y fija. ASCOT solo **parametriza** esa lógica — el usuario elige opciones (tipo de economizador, de calefacción, de damper, fuente de sensor…) y ASCOT las escribe como **objetos BACnet** o **NVs LON**.

### El contraste Spyder vs Stryker `[CERT]`

| | **Spyder ([Bloque 106])** | **Stryker (ASCOT, este)** |
|---|---|---|
| Naturaleza | **Programación libre** | **Aplicación fija (application-specific)** |
| Lógica de control | el ingeniero conecta function blocks en wiresheet | ya está en el firmware (CV-AHU / VAV) |
| Motor(es) FB | 3 (F1/B103, IRM Nano/B105, Kingfisher/B106) | **ninguno** |
| Rol de la herramienta | compilar el programa → binario → download | **parametrizar** opciones → objetos BACnet / NVs LON |
| Unidad de bajada | binario propietario monolítico (CRC-16) | **objeto por objeto** (`writeMultipleProperties`, lotes de 11) |

> **La conclusión `[INFER]`**: Honeywell ofrece dos filosofías de controlador HVAC. El **Spyder** para integradores que necesitan lógica custom (programable, más caro de configurar). El **Stryker** para aplicaciones estándar (CV-AHU/VAV de catálogo, configurado por wizard, más rápido de comisionar). ASCOT es el equivalente Stryker de los wizards de termostato (B97/B98), pero para un controlador de aplicación completa.

---

## 107.3 — Arquitectura MVC: ASCOT es el mayor cliente de `genericUIFramework` `[CERT]`

El patrón actions/validator/beans/panes **NO es motor propio** — es exactamente el `genericUIFramework` del [Bloque 102]. Verificado:

```
LoadConfigAction / SaveConfigAction        extends AbstractAction        [CERT, :35 / :53]
AscotValidator / IOAssignmentValidator     extends AbstractValidator     [CERT-a]
   ↑ import com.honeywell.generic.ui.framework.{AbstractAction, AbstractValidator,
                                                IUIFwRequest, IUIFwResponse, IUIFwSession}  [CERT, :24-27]
```

- **Sesión MVC** `[CERT]`: `req.getSession().getBean("DeviceConfigBean")` / `getBean("DeviceInfo")` — el estado del wizard vive en beans de la sesión del framework.
- **Beans (modelo)** `[CERT]`: `BAscotBean extends BComponent` (base propia, un BComponent N4 que actúa de modelo); concretos `BCvahuDeviceConfigBean`, `BVavDeviceConfigBean`…
- **Panes (vista)** `[CERT-a]`: `BVavInputPane extends BPaneBase extends BBorderPane`; contenedores `BCvahuContainer extends BAscContainer extends BUIFWContainer`; wizard `BWizardFrame extends BFrame` (con variantes tabbed/dynamic-tabbed).
- **Validators** `[CERT-a]`: jerarquía profunda `AscFieldRules → AscInputRules → reglas leaf por campo` (366 clases) — validación campo a campo del wizard.

ASCOT es el **cliente de aplicación más grande conocido** de `genericUIFramework` — aterriza ese framework (B102) sobre un caso de uso real masivo.

---

## 107.4 — Cómo escribe al controlador: objeto por objeto `[CERT]`

ASCOT mapea cada opción del wizard a un **parámetro de red** (`INetworkParam`, interfaz marcadora) `[CERT-a]`:
- BACnet: `BBacnetObjectReference implements INetworkParam` (un parámetro = un objeto BACnet: type + instance).
- LON: `BLonNetworkVariableField implements INetworkParam` (un parámetro = un campo de NV LON).

**Descarga BACnet** `[CERT]`: `BAscBacnetDownloadJob.download()` itera `getNumberOfNetworkParamsForDownload()`, agrupa en lotes de 11 objetos y llama `writeMultipleProperties()` (servicio BACnet WriteMultipleProperty). Para parámetros que requieren reinicio, primero pone el device offline (`setDeviceState(5)`). Soporta offline (escribe a la copia local en estación) y online (acción async al dispositivo). LON es análogo (`BAscotLonDevice`, NV por NV).

**Data sharing (BOAC)** `[CERT-a]`: para bindings peer-to-peer, `BBacnetFileWriter` serializa un `BVector` little-endian, calcula CRC con `BCRCGenerator.calculateCRCChecksumForBOAC()` (tabla de 256 entradas) y lo escribe vía BACnet FileWrite al **File Object 263**. Es el único binario+CRC del módulo.

**Factory dinámico** `[CERT-a]`: `FactoryManager` instancia en runtime `com.honeywell.ascot.factory.<Model>ControllerFactory` (`StandardOEMControllerFactory`, `SmartActuatorControllerFactory`, `AscLonCVAHUControllerFactory`) según el modelo de hardware descubierto.

---

## 107.5 — Capas BACnet/LON y los enums `[CERT-a]`

**`ascBacnet` (147)**: `BAscotBacnetDevice extends BBacnetDevice implements IAscotDevice, IBacnetBindable, IBacnetFailDetectSupport` → `BAscBACnetVAV`. Aporta: capa de objetos BACnet (25, `BBacnetFloatObject`/`BBacnetEnumeratedObject`/…), **data sharing BOAC** (15, con `BBacnetFileWriter`/`BCRCGenerator`), discovery (7), 40 enums BACnet-específicos.

**`ascLon` (58)**: `BAscotLonDevice extends BDynamicDevice implements IAscotDevice` → `BAscLonCVAHU` / `BAscLonVAV`. **Modelos de hardware físico** `[CERT-a]`: `PVL4022ASDevice`/`PVL4024NSDevice implements IDeviceModelInfo` (mapeo pin→NV de los VAV LON), templates de cableado (`BAscotLonVAVSingleDuctTemplate`).

**243 enums** (`ascot/enums/`) `[CERT-a]`: cada opción discreta del wizard HVAC es un `BFrozenEnum` (necesario para serialización BAJA + mapeo a valor numérico del firmware). Ejemplos: `BCvahuModulatingEconomizerEnum` (NONE/DIGITAL/ANALOG/FLOATING), `BCvahuCoolingTypeConventionalEnum` (NONE/STAGED_1..4/ANALOG/FLOATING), `BCvahuIAQControlTypeEnum` (SPACE_CO2/RETURN_AIR_CO2/…), `BDamperTypeEnum` (ANALOG/FLOATING/PWM), `BDeviceModeEnum` (21 modos), `BVAVDeviceModeEnum` (16 modos), `BControllerTypeEnum` (STANDARD_OEM/SMART_ACTUATOR).

**Integración con [Bloque 96]/Venom** `[CERT]`: `GenericDeviceDataModelFactory` añade `"airFlowBalancer:FlowBalancingView"` como agente del device model (:11,20) — el Stryker VAV expone directamente la vista de Air Balancing TAB de Venom.

---

## 107.6 — Calidad / seguridad `[CERT]` + `[CERT-a]`

- **Limpio `[CERT-a]`**: sin credenciales hardcodeadas, sin `Runtime.exec()`, sin `loadLibrary`/JNI, sin `MessageDigest`/crypto, sin EULA embebido. Solo 2 `System.out.println` residuales (`BPopUpHelpBinding`, `BacnetVavMonitoringInfo`).
- **Feature gating sin bypass `[CERT-a]`**: el data sharing BACnet (BOAC bindings) requiere **feature licenciada** en el servidor N4 — `checkForOnlineOperations()` devuelve status 7 con `"DataSharing.refreshjob.featurecheck - feature not licensed"` si falta. No hay bypass implementado (contrasta con el license-bypass "Webs" del motor F1 en [Bloque 103] y la EULA bypasseable de Venom/kitCat en [Bloque 96]/[Bloque 101]).
- **Integridad de config `[CERT-a]`**: el binario BOAC bindings se valida con CRC-16 tabla (no firma) — consistente con el patrón Honeywell, pero aquí solo aplica a los bindings de data sharing, no a la aplicación de control (que es fija en firmware).
- Módulo firmado (`SERVER1.RSA`), verificado por el NRE al cargar.

---

## 107.7 — Conexiones

- **[Bloque 106]** (Spyder): el **contraste arquitectónico** central. Spyder = programación libre (3 motores FB); Stryker = aplicación fija (cero FB, parametrización por wizard). Cero acoplamiento entre ambos. Las dos líneas de controlador de Honeywell.
- **[Bloque 102]** (`genericUIFramework`): su **motor MVC**. ASCOT es su mayor cliente — `AbstractAction`/`AbstractValidator`/`IUIFw*` por todas partes. Aterriza el framework B102 sobre un caso real masivo.
- **[Bloque 96]** (`airFlowBalancer`/Venom): dependencia declarada — el Stryker VAV expone la vista de Air Balancing TAB. Confirma el ecosistema VAV Honeywell.
- **[Bloque 32]** (**corrigendum**): invalida la etiqueta "legacy AX pre-N4" de `ascCommon`; es herramienta moderna build 2024.
- **[Bloque 75]** (seguridad): aporta un módulo limpio con feature-gating real (sin bypass) — el contrapunto positivo a los bypasses de B96/B101/B103.
