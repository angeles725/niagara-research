# Bloque 97 — `honeywellTB3026BWizard`: wizard Workbench de configuración BACnet del termostato comercial TB3026B (19 aplicaciones HVAC), deofuscado

> Investigación empírica del módulo OEM Honeywell **`honeywellTB3026BWizard`** (115 java): un wizard de Workbench que configura el termostato comercial **TB3026B** (BACnet nativo, vendor 17) — selección de aplicación HVAC, setpoints, schedules, fan, I/O, economizador, PINs de servicio. **Solo gestiona configuración** (NO firmware).
>
> Familia C del barrido del corpus (wizards de termostato). El módulo hermano TC300/TC500 va en [Bloque 98].
>
> Fuentes: `organized/honeywellTB3026BWizard/honeywellTB3026BWizard/.../vineflower/com/honeywell/tb3026b/...`.
> Método: 1 sub-agente Explore + **verificación directa** de cada `extends`, los OIDs de los PINs de servicio y el flag de autenticación BACnet. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (19 apps, 8 tabs, flujo de jobs, enums) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 96]. Conecta [Bloque 95] (otros devices BACnet Honeywell), [Bloque 98] (el otro wizard de termostato, que SÍ usa el Device Manager), [Bloque 75] (seguridad).

---

## 97.1 — Qué es + clases raíz verificadas `[CERT]`

El TB3026B es un termostato comercial multiaplicación Honeywell con BACnet nativo (vendor 17) `[CERT-a]`. El wizard configura **exclusivamente vía BACnet** (`ReadPropertyMultiple`/`WritePropertyMultiple` sobre prop 85). Clases raíz verificadas:

| Clase | `extends` verificado (archivo:línea) |
|-------|--------------------------------------|
| `BTB3026B` | `extends BBacnetDevice implements ITB3026BConstants` (`device/…:43`) |
| `BTB3026BConfigProps` | `extends BComponent` (`device/…:22`, ~70 props de config) |
| `BTB3026BBacnetConfigObjects` | `extends BStruct` (`device/…:9`, mapeo prop→OID BACnet) |
| `BTB3026BUploadDownloadJobCommon` | `abstract extends BSimpleJob` (`job/…:16`) |
| `BTB3026BConfigurationView` | `extends BWbComponentView` (`ui/configView/…:50`) |

El modelo: `BTB3026B` (device) contiene `appConfig` (`BTB3026BConfigProps`, ~70 propiedades tipadas) que se mapea 1:1 a OIDs BACnet vía `BTB3026BBacnetConfigObjects extends BStruct` (p.ej. `appSelection→AV:49`, `fanConfig→AV:17`). Estado visual: rojo `downloadStatus==1` (pendiente) / verde `==2` (sincronizado) `[CERT-a]`.

---

## 97.2 — Los jobs de configuración `[CERT]` + `[CERT-a]`

Todos `extends BSimpleJob` (no usan el Device Manager, a diferencia del [Bloque 98]):
- `BTB3026BDownloadConfigJob` / `BTB3026BUploadConfigJob extends BTB3026BUploadDownloadJobCommon` (template method) `[CERT]`.
- **Download** `[CERT-a]`: model check (vendor 17, model "TB3026B") → `preTransfer` (deshabilita outputs, escribe defaults comunes + por-aplicación en lotes de 20) → `transfer` (escribe las props mapeadas) → `postTransfer` (re-habilita outputs, `setOutputsToAuto()`).
- **Upload** `[CERT-a]`: lee prop 85 de cada objeto y actualiza `BTB3026BConfigProps`.
- `BTB3026BBuildProxyPointsJob extends BSimpleJob`: crea proxy points (`BNumeric/Boolean/EnumWritable` con `BBacnetProxyExt`) desde un `.bog` offline según la aplicación.
- `BTB3026BWriteDeviceInstanceJob`: lee el instance real por broadcast (`make(8, 4194303)`, prop 75), valida vendor/modelo y escribe el instance de la station.
- `BFFTBatchOpJob extends BBatchJob extends BSimpleJob`: orquesta dl/ul/setAuto sobre listas de devices.

`DefaultValuesFactory` `[CERT-a]`: singleton con 19 mapas de defaults BACnet (uno común + uno por aplicación), usados en el preTransfer.

---

## 97.3 — La UI: wizard de 8 pestañas `[CERT-a]`

`BTB3026BConfigurationView extends BWbComponentView` `[CERT]` organiza un `BTabbedPane` de 8 pestañas: Application (selección HVAC + sub-panel dinámico), General/Display, Schedule Options, Zone Setpoints, Heat/Cool (PID), Fan/Dehum, Economizer (visible solo en apps que lo soportan), I/O Config. Sub-paneles por familia de aplicación (`BTB3026BAppSubPane{2PFCU,4PFCU,ACUnit,ASHP,WSHP}`) y "options" reutilizables (sensores aux, acceso a schedule/sistema). `doSaveValue` copia a la copia de trabajo y marca `setModified(true)` — **NO** dispara descarga (la descarga es una Action separada) `[CERT-a]`.

**Modelo de configuración (enums) `[CERT-a]`**: `enums/` son `BFrozenEnum` que mapean 1:1 a propiedades BACnet Enumerated — `BTB3026BApplicationTypeEnum` (verificado `extends BFrozenEnum` `[CERT]`, 19 apps: ASHP/WSHP/ACUnit/2PFCU/4PFCU…), fan config, keypad lock, modos de válvula 2P/4P, DST. `ui/enums/` son POJOs (`extends TB3026BUIOptionEnum`) solo para poblar dropdowns, no serializados. `beans/` son DTOs (`BObjectBean extends BComponent` para read/write genérico de objetos; `DiagnosticsBean` POJO para la vista de diagnóstico).

---

## 97.4 — Seguridad `[CERT]` + `[CERT-a]`

**[ALTO CERT] PINs de servicio en texto plano BACnet.** `fieldServicePin` (`AV:132`) e `ISUServicePin` (`AV:133`) son enteros 0-9999 escritos por `WritePropertyMultiple` sin cifrado (verificado `BTB3026BBacnetConfigObjects.java:60-61`). Sniffing o write-access BACnet los lee/modifica; el field editor solo valida que sean dígitos.

**[ALTO CERT-a] Sin autenticación BACnet.** `DeviceConfigObjectSettings` declara el peer Niagara con el bit `Authenticate` en falso en `protocolServicesSupported` (`AUTHENTICATE_NOT_SUPPORTED = false`, verificado `:63`/`:179`) → cualquier device en la red escribe al termostato sin autenticar. (El servicio Authenticate BACnet es obsoleto; el matiz es que no hay capa de auth.)

**[MEDIO CERT-a] Sin autorización por acción.** `downloadConfiguration`/`writeObjectValue`/`writeMultiplePropertyValues` tienen flag de permiso 4 (lectura) pero no verifican rol antes de escribir al device; la protección depende del control de acceso de la station.

**[BAJO CERT-a] Ventana de DoS en download.** El `preTransfer` deshabilita las salidas físicas antes de escribir; si el job falla/cancela después, el HVAC queda sin operación hasta intervención manual. Los `.bog` de offline discovery (`TB3026BFF_{US,SI}.bog`) van en el JAR sin hash/firma. **No hay firmware update** en este módulo.

---

## 97.5 — Conexiones

- **[Bloque 98]** (`honeywellTCThermostatWizard`): el otro wizard de termostato Honeywell. **Diferencia clave**: el TB3026B es standalone (jobs `BSimpleJob` propios, sin firmware); el TC300/TC500 se apoya en el Device Manager ([Bloque 94]) y sí hace firmware OTA.
- **[Bloque 95]** (devices BACnet Honeywell): comparten el patrón `BBacnetDevice` + config por OIDs.
- **[Bloque 75]** (seguridad): suma PINs en claro + escritura BACnet sin auth.
