# Bloque 98 — `honeywellTCThermostatWizard`: wizard de termostatos serie TC300/TC500 (config BACnet + firmware OTA vía Device Manager + matriz de features + frontend BajaScript), deofuscado

> Investigación empírica del módulo OEM Honeywell **`honeywellTCThermostatWizard`** (197 java): wizard de configuración y **firmware OTA** de los termostatos comerciales serie **TC300/TC500** vía BACnet. Más complejo que el TB3026B ([Bloque 97]): se apoya en el **Device Manager** ([Bloque 94]) para el firmware, tiene una **matriz de features modelo×firmware**, frontend **BajaScript** (`-ux`), y licensing para TC500.
>
> 1 módulo con 3 perfiles (`-rt`/`-ux`/`-wb`). Paquetes: `enums` (57+`tc300` 41+`v11` 11), `jobs` (18), `tc500` (15), `tc300` (11), `modelfeature` (7), `firmware` (6), `ux` (5), `common`.
>
> Fuentes: `organized/honeywellTCThermostatWizard/honeywellTCThermostatWizard-rt/vineflower/com/honeywell/honeywellTCThermostatWizard/...`.
> Método: 1 sub-agente Explore + **verificación directa** de cada `extends`, la liga al Device Manager (`BHonBaseJob`/`BIHonBacnetConfig`/`BHonBacnetFileHandler`), y dos bugs (`Pattern.quote`, licensing `"xxx"`). `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (modelos, tc300 vs tc500, matriz de features, enums, RPC) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 97]. **Liga fuerte a [Bloque 94]** (Device Manager: firmware OTA, file handler, config singleton). Conecta [Bloque 97] (el wizard hermano TB3026B), [Bloque 75] (seguridad).

---

## 98.1 — Qué es + jerarquía verificada `[CERT]`

Los TC300/TC500 son termostatos comerciales Honeywell (HVAC commercial grade) `[CERT-a]`. Modelos (`DeviceModelEnum`, device model numbers BACnet 74-93) `[CERT-a]`: TC300{B,C}-G, TC320{B,C}-{G,N}; TC500{A,B,C}-{N,W}. Configuración vía BACnet (prop 85). Jerarquía verificada:

```
BBacnetDevice
 └─ BThermostatBacnetDevice extends BBacnetDevice implements BIHonBacnetDevice  (abstract, :96)  [CERT]
     ├─ BTC300 extends BThermostatBacnetDevice                          (:87)  [CERT]
     └─ BTC500 extends BThermostatBacnetDevice implements ITC500Constants (:112) [CERT]
```

`BIHonBacnetDevice` es la interfaz del **Device Manager** ([Bloque 94]) → estos termostatos son devices gestionables por él. Ambos modelos replican el esquema de 4 contenedores de config (`appConfig`/`editValueConfig`/`alarmViewConfig`/`commonConfig`) + 3 acciones (`downloadConfig`/`uploadConfig`/`downloadFirmware`) `[CERT-a]`.

---

## 98.2 — TC300 vs TC500: arquitecturas paralelas `[CERT-a]`

| Aspecto | TC300 | TC500 |
|---------|-------|-------|
| Equipo HVAC | FanCoil/Conventional/HeatPump | 15 app templates + 16 tipos |
| Schedules BACnet | OccSchedule (obj 17 inst 2) | + PurgeSchedule (inst 3) |
| File OID firmware | `(10, 1)` | `(10, 65536)` |
| Salidas digitales | DO1-DO3 | DO1-DO8 |
| Economizador / SYLK bus | No | Sí |
| Licencia runtime | No | `TC500Licensing.licenseOK()` |
| Enums | nombres semánticos | **ofuscados `Ordinal1..16`** |

**Nota de ofuscación `[CERT-a]`**: los enums TC500 quedaron con valores `Ordinal1..N` (ofuscación post-compilación ZKM no revertida del todo); los TC300 y el subpaquete `v11` conservan nombres semánticos. Esto explica que los `enums/` (57+41+11) sean el grueso del módulo: el catálogo exhaustivo de opciones (system mode, equipment/fan type, heating/cooling stages hasta 16, changeover TC300, economizer/enthalpy TC500, alarmas, SYLK).

---

## 98.3 — Matriz de features modelo×firmware `[CERT-a]`

Subsistema `modelfeature`: matriz **modelo × versión de firmware → features habilitados**. `BModelFeaturesEnum`: `basic`/`customSensorName`/`release2Configuration`/`release11Configuration`. `ModelFeatureCompatibilityXMLParser` lee `res/config/ModelFeatureCompatibility.xml` (XPath) y construye un `DeviceModelContainer` de 3 dimensiones (ModelGroups, FirmwareGroups con herencia acumulativa, EnumGroups de ordinals válidos por feature). En cada upload/download, `ThermostatConfigurationTask.execute()` consulta la matriz para **saltar propiedades no soportadas** por el firmware instalado y filtrar ordinals inválidos. El subpaquete `enums/tc300/v11` son los tipos habilitados cuando `release11Configuration` está activo (correspondencia `v11` ↔ release11) `[INFER]`.

---

## 98.4 — Firmware OTA: delega en el Device Manager `[CERT]` + `[CERT-a]`

A diferencia del TB3026B, este wizard **sí actualiza firmware**, y lo hace a través de la infraestructura del **[Bloque 94]** (verificado `[CERT]`):
- `BThermostatFileHandler extends BHonBacnetFileHandler` (`firmware/…:10`) — el file handler BACnet del Device Manager.
- `BTCThermostatConfig extends BComponent implements BIHonBacnetConfig` (`firmware/…:62`) — el config singleton (par del `BHonBacnetDeviceConfig`).
- `BThermostatConfigurationJob extends BHonBaseJob` (`jobs/…:21`) — el job base del Device Manager.

Flujo `[CERT-a]`: el frontend selecciona un `.bin` → `FirmwareFileDownloadHandler` lee versión (offset 20, 11 B) y model name (offset 32, 32 B), valida que empiece por "tc300"/"tc500" → `BTCThermostatConfig.copyBinaryString()` escribe chunks Base64 en `$STATION_HOME/firmware/` → `doSubmitFirmwareDownloadJob()` lanza el **`BFirmwareDownloadJob` del [Bloque 94]** que hace el `atomicWriteFile` BACnet real. **Hereda el mismo problema de integridad del B94** (sin firma/hash).

---

## 98.5 — Frontend BajaScript (`-ux`) `[CERT-a]`

`BConfigurationWizard extends BSingleton implements BIJavaScript` (agente preferido sobre nodos TC300/TC500) entrega `TCWidget.js` como vista HTML5/BajaScript (patrón Hx5, no PX). Dos clases RPC servidor: `BThermostatWizardRPC` (TC500) y `BTC300WizardRPC` (TC300), con `buildProxyPoint` (crea/actualiza proxy points BACnet + conversión °F/°C). Jobs (18): orquestador `BThermostatConfigurationJob` + tasks download/upload (`CommUtil` WritePropertyMultiple por lotes, con la matriz de features), + jobs TC500 específicos (diagnostic view, calibration status, common) + alarmas (commonAlarm, mstpHeaderCrc/DataCrc) + executors `BICommandExecutor` (entrada desde el manager del Device Manager).

---

## 98.6 — Seguridad y bugs `[CERT]` + `[CERT-a]`

**[ALTO CERT-a] Firmware sin verificación de integridad.** `FirmwareFileDownloadHandler.validateIfModelMatched()` solo comprueba que el model name del header empiece por "tc500"/"tc300" — sin hash/CRC/firma. Cualquier binario con esos bytes pasa y se envía al dispositivo (vía el OTA del [Bloque 94]).

**[MEDIO CERT] Bug que deshabilita la validación de versión de firmware.** `FirmwareFileDownloadHandler.java:149/153`: `firmwareVersion.matches(Pattern.quote("(\\d+).(\\d+)..."))` — `Pattern.quote` convierte el regex en literal `\Q...\E`, así que **nunca matchea**; la validación de formato de versión queda inerte.

**[MEDIO CERT] RPC `unrestricted`.** `BThermostatWizardRPC.buildProxyPoint` declara `permissions = "unrestricted"` → cualquier usuario autenticado (sin importar rol) puede crear proxy points BACnet escribibles en la station.

**[MEDIO CERT-a] Path traversal en escritura de firmware.** `BTCThermostatConfig.doCopyBinaryString()` arma la ruta con `resolve("firmware").resolve(fileName)` sin sanitizar `fileName` (mismo patrón que el [Bloque 94]).

**[BAJO CERT] Licensing TC500 frágil.** `TC500Licensing.licenseOK()`: `checkFeature("honeywell", "xxx")` (feature key `"xxx"` = placeholder ofuscado) y el fallback de marca usa `BRANDS_ALLOWED = new String[0]` (array **vacío** → el fallback siempre falla). La licencia depende exclusivamente del check con la key placeholder. Clases legacy BEATS/WEBS `@Deprecated` con `getModelName()` que retorna `null` (riesgo NPE).

---

## 98.7 — Conexiones

- **[Bloque 94]** (Device Manager): liga estructural — `BThermostatFileHandler extends BHonBacnetFileHandler`, `BThermostatConfigurationJob extends BHonBaseJob`, `BTCThermostatConfig implements BIHonBacnetConfig`, y el firmware OTA real lo hace `BFirmwareDownloadJob` del B94.
- **[Bloque 97]** (TB3026B): wizard hermano, pero standalone y sin firmware. Contraste de arquitectura dentro de la misma familia C.
- **[Bloque 75]** (seguridad): suma firmware sin firma + validación de versión rota + RPC unrestricted + licensing placeholder.
