# Bloque 95 — Honeywell Smart Edge Devices: sensores IAQ serie TR50 (BACnet + Modbus) + wall modules TR100 BACnet, sobre `honIOBase`, deofuscado

> Investigación empírica de la familia OEM Honeywell **Smart Edge Devices**: drivers para integrar los **sensores de calidad de aire interior (IAQ) serie TR50** vía BACnet IP o Modbus RTU/TCP, y los **wall modules serie TR100** vía BACnet. Ambos extienden el driver estándar de su protocolo (no crean stack propio) y comparten la capa base `honIOBase`.
>
> 3 módulos: `honeywellBacnetSmartSensor` (28 java), `honeywellModbusSmartSensor` (25 java), `honeywellBacnetWallModule` (9 java). Capa base compartida: `honIOBase` (`com.honeywell.smartedgedevices.{sensors,smartsensorconfig,genericpoints,enums}`).
>
> Fuentes: `organized/honeywell{Bacnet,Modbus}SmartSensor/...`, `organized/honeywellBacnetWallModule/...`, `organized/honIOBase/...`.
> Método: 2 sub-agentes Explore (smart sensors, wall module) + **verificación directa** de cada `extends` de device/proxyExt y de la base `BSmartSensorPoint`/`ISmartSensorDevice` en `honIOBase`. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (modelos, magnitudes, registros, comandos, bug discovery) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 94]. Estos dispositivos son **consumidores del Device Manager** ([Bloque 94]: comisionamiento + firmware OTA). El wall module TR100 BACnet es la **variante de protocolo** del mismo hardware que los wall modules Sylk del [Bloque 88]. Conecta [Bloque 94], [Bloque 88], [Bloque 75].

---

## 95.1 — Qué son + clases raíz verificadas `[CERT]`

Drivers altamente especializados (no frameworks genéricos) para hardware Honeywell concreto, cada uno **extendiendo el driver estándar de su protocolo**:

| Device | `extends` verificado (archivo:línea) |
|--------|--------------------------------------|
| `BHonBacnetSmartSensorDevice` | `extends BBacnetDevice implements BIHonBacnetDevice, ISmartSensorDevice` (`bacnet/device/…:119`) |
| `BHonModbusSmartSensorDevice` | `extends BModbusAsyncDevice implements BIHonModbusDevice, ISmartSensorDevice` (`modbus/device/…:108`) |
| `BHonBacnetWallModuleDevice` | `abstract extends BBacnetDevice implements BIHonBacnetDevice` (`…/bacnet/device/…:42`) |
| modelos sensor | `BHonBacnetTR503N extends BHonBacnetSmartSensorDevice`; `BHonModbusTR503N extends BHonModbusSmartSensorDevice` (`:21`) |
| modelos wall | `BHonBacnetTR100TH extends BHonBacnetWallModuleDevice` (`:11`) |

`BBacnetDevice` y `BModbusAsyncDevice` son las clases estándar de Tridium `[CERT]`. Las interfaces `BIHonBacnetDevice`/`BIHonModbusDevice` vienen del Device Manager ([Bloque 94]).

---

## 95.2 — Sensores IAQ TR50 (BACnet + Modbus) `[CERT]` + `[CERT-a]`

**Modelos `[CERT-a]`**: `TR50-{3,5}{D,N}` (3/5 = nº de magnitudes; D = con LCD, N = sin display). Magnitudes (8 clases base de sensor en `honIOBase`): temperatura, humedad, CO2, AQI (común a 3x y 5x); +PM2.5, TVOC, PM1, PM10 (solo 5x). Sólo temperatura (°C/°F) y TVOC (ppb/µg/m³) tienen unidad configurable. Sin sensor de ocupación.

**Jerarquía de punto (en `honIOBase`) `[CERT]`**: `BSmartSensorPoint extends BHonAnalogInput` (`honIOBase/…/sensors/…:29`, abstract) → 8 sensores base (`BTemperatureSensor`, `BCo2Sensor`, `BPM25Sensor`…). Cada driver los extiende con su variante (`BHonBacnetTemperatureSensor`/`BHonModbusTemperatureSensor extends BTemperatureSensor`) `[CERT-a]`.

**ProxyExt con override `[CERT]`**: `BHonBacnetNumericProxyExt extends BBacnetNumericProxyExt` (`:11`), `BHonModbusClientNumericProxyExt extends BModbusClientNumericProxyExt` (`:11`). Ambos sobrescriben `readOk/readFail` para usar `getOverrideValue()` si está seteado `[CERT-a]`.

**Direccionamiento `[CERT-a]`**:
- BACnet: objeto AI por sensor (Temp=instancia 1, Hum=2, CO2=3, PM2.5=4, TVOC=5, AQI=6, PM1=7, PM10=8). Escritura de config por `writeProperty` (presentValue propId 104 escritura / 85 lectura / 117 units), con `WritePropertyMultiple` en lotes de 3.
- Modbus: dirección decimal (Temp=1…), `BLinearConversion(0.1, 0)` para Temp/Hum (device envía ×10). Escritura FC6 (holding register) o FC5 (coil) para units.

**Sync de config `[CERT-a]`**: `configVersion == 65536` → "nunca sincronizado, fullSync"; otro valor → deltaSync. Registros Modbus de identificación: 1000 model, 1030 serial, 96 fw, 98 BLE fw, 210 config version.

---

## 95.3 — La capa base compartida: `honIOBase` `[CERT]` + `[CERT-a]`

`honIOBase` es el módulo base de TODA la familia Smart Edge (no sólo I/O). Contiene `com.honeywell.smartedgedevices.{sensors, smartsensorconfig, genericpoints, enums, displayconfig, util}` `[CERT-a]`: la interfaz `ISmartSensorDevice`, `BSmartSensorPoint` (verificado `extends BHonAnalogInput` `[CERT]`), las 8 clases base de sensor, las clases de config (`BSmartSensorConfig` + por-magnitud + `BSmartSensorDeviceConfig3D/5D`), y `HonSmartEdgeHelper`. Los slot-names de punto son idénticos entre protocolos (`"SS_AI_SensorReading_Temp"`…) `[CERT-a]`. El executor de comandos (`BConfigurationDownload/UploadExecutor implements BICommandExecutor`, anotados `@CommandMgr`) existe sólo en BACnet; Modbus delega a un `BSimpleJob` directo.

> **`honIOBase` merece su propio bloque** — es la base de Smart Edge + I/O Honeywell y aquí solo se cubre lo que aflora desde los drivers. Pendiente para una pasada dedicada.

---

## 95.4 — Wall modules TR100 BACnet: variante del hardware Sylk del B88 `[CERT]` + `[CERT-a]`

`honeywellBacnetWallModule` registra los wall modules **serie TR100** (sensor de sala con display/setpoint) como dispositivos BACnet de primera clase. Modelos `[CERT-a]`: `TR100-T-G` (temp), `TR100-TH-G` (+humedad), `TR100-THC-G` (+CO2). Módulo **delgado a propósito** `[CERT-a]`: sólo tipifica el dispositivo (`extends BBacnetDevice`); los puntos de proceso se descubren dinámicamente por el learn BACnet estándar, no son propiedades Java estáticas. `getDeviceType() = BHonProductType.f` (FIELD). Depende de `honeywellBacnetDeviceManager` + `honeywellDeviceManager` + `honIOBase` ([Bloque 94]).

**Relación con [Bloque 88] (Sylk) `[CERT-a]`**: son **dos variantes de protocolo del mismo hardware físico TR100/TR120** (ambos usan el icono `TR120.png`). Sylk → sub-dispositivo colgado de un controlador (`BTR100SylkDeviceForTR42Emulation`, bus RS-485 propietario, con gestión de potencia 74/1 mA); BACnet → dispositivo autónomo con su propio instance ID (sin gestión de potencia). El sufijo "-G" marca la variante de firmware con stack BACnet embebido `[INFER]`.

---

## 95.5 — Seguridad `[CERT-a]`

**[ALTO CERT-a] Sin cifrado de transporte** — ni BACnet/SC ni TLS Modbus (cero imports de seguridad); valores y config van en texto plano.

**[CRÍTICO CERT-a] Busy-wait `while(!sensor.isMounted()){}`** en `initializeSensors()` (ambos drivers) → satura un thread del servidor al 100% si el montaje demora o falla.

**[MEDIO CERT-a] Lectura por reflexión con fallo silencioso** (`HonModbusUtil.getRegisterValue` retorna 0 si falla la reflexión de versión Niagara → toda la config leída queda en 0 sin alarma). Config Modbus parámetro-a-parámetro sin rollback (estado parcial si falla a mitad). Reset de `configVersion=65536` fuerza fullSync que sobrescribe el sensor físico sin confirmación.

**[BAJO CERT-a] Bug de encoding en el discovery BACnet**: `dpUnits.append(dpUnits + ";" + unit)` (concatena el acumulador consigo mismo → `supportedUnits` mal formado); la variante Modbus lo corrigió. Sin validación de rangos en valores leídos del device (un device spoofeado puede corromper config). Para el wall module: `download/uploadConfiguration()` son stubs que retornan `null`; `atomicWriteFile` de firmware sin validación (igual que [Bloque 94]).

---

## 95.6 — Conexiones

- **[Bloque 94]** (Device Manager): estos devices son sus consumidores — comisionamiento por pool de instance IDs (BACnet) / discovery Modbus, firmware OTA por `BIFileHandler`.
- **[Bloque 88]** (Sylk wall modules): el TR100 BACnet es la variante de protocolo del mismo hardware.
- **`honIOBase`**: capa base compartida, candidata a bloque propio.
- **[Bloque 75]** (seguridad): suma transporte sin cifrar + busy-wait + lecturas sin validación de rango.
