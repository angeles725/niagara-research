# Bloque 104 — `honIOBase`: la base común de I/O Honeywell (puntos + config + 56 tipos AI) y de los Smart Edge Devices (sensores TR50), deofuscada

> Investigación empírica del módulo OEM Honeywell **`honIOBase`** (76 java): la **capa base de I/O reutilizable across-protocols** de Honeywell (`module.xml`: *"Common module for IO communication used across protocols"`, `autoload`, **cero dependencias de protocolo**). Dos capas independientes: (A) `com.honeywell.iobase.*` — tipos de punto + config de I/O genéricos; (B) `com.honeywell.smartedgedevices.*` — la base de los sensores IAQ TR50 (aterriza el [Bloque 95]).
>
> 1 módulo (`honIOBase`, `-rt`/`-wb`). Paquetes: `iobase/{point,config,enums,util}`, `smartedgedevices/{sensors,smartsensorconfig,enums,displayconfig,genericpoints}`.
>
> Fuentes: `organized/honIOBase/honIOBase-rt/vineflower/com/honeywell/...`.
> Método: 1 sub-agente Explore + **verificación directa** de cada `extends` de las dos jerarquías y del bug de conversión de unidad. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (tipos AI, defaults de config, enums, display, acoplamientos) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 103]. **Aterriza el [Bloque 95]** (que trató esta base de pasada como dependencia de Smart Edge) y completa la capa `iobase.*` no cubierta. Conecta [Bloque 95] (Smart Edge BACnet/Modbus), [Bloque 86] (PanelBus/Snapon I/O), [Bloque 75] (seguridad).

---

## 104.1 — Qué es: base común de I/O, dos capas `[CERT]`

Módulo base que NO comunica con ningún protocolo (sin deps BACnet/Modbus/PanelBus, `autoload=true`); define los tipos abstractos que los módulos de protocolo concretan. Dos capas:
- **(A) `iobase.*`**: base de I/O genérica — tipos de punto abstractos, config de I/O (56 tipos AI, actuadores con carrera), enums de campo (HOA, safety position).
- **(B) `smartedgedevices.*`**: base exclusiva de los sensores Smart Edge (TR50/IAQ) — jerarquía de 8 sensores, configs por magnitud, config de display LED/LCD.

---

## 104.2 — Capa A: `iobase` (puntos + config) `[CERT]`

**Puntos abstractos** (envuelven los ControlPoint de Niagara, no `BProxyExt` directo):
```
BNumericPoint    → BIONumericPoint  (abstract, implements BIAlarmSource)   [CERT, point/…:64]
BBooleanPoint    → BIOBooleanPoint  (abstract)                              [CERT, point/…:37]
BNumericWritable → BIONumericWritable (abstract)                            [CERT-a]
BBooleanWritable → BIOBooleanWritable (abstract)                            [CERT-a]
```
Los 4 son abstractos; el protocolo concreta las subclases. Interfaz `IIOProxyExt` (`getParentPoint()` + `isReadonlyProxyExt()`), marcadoras `ISnaponIOPoint`/`ISnaponIOProxyExt` `[CERT-a]`.

**Config** verificada:
```
BComponent → BIOPointConfig (abstract)  [CERT, config/…:12]
              ├─ BAnalogInputConfig  (abstract, [CERT] config/…:74)  → BSnaponIOAnalogInputConfig
              ├─ BDigitalInputConfig (abstract)                       → BSnaponIODigitalInputConfig
              ├─ BOutputConfig       (abstract)                       → BSnaponIOOutputConfig
              └─ BNullPointConfig    (abstract)                       → BSnaponIONullPointConfig
```
`BIOPointConfig` vive como hijo del ProxyExt (`getProxyExt() = (IIOProxyExt)getParent()`). `BAnalogInputConfig` tiene **~56 constantes de tipo AI** (0x00-0x59) con clasificadores `isThermistorType()/isVoltageType()/isBooleanType()` + conversión C↔F + slot dinámico `invalidValue` si `sensorFail` `[CERT-a]`. `BOutputConfig` modela actuadores con `open/closeRuntime` (def 90 s, 10-1000), valve exercising, sync 24h — analógico o digital `[CERT-a]`.

---

## 104.3 — Capa B: Smart Edge (sensores TR50) `[CERT]`

**Jerarquía de sensor verificada** (aterriza el [Bloque 95]):
```
BNumericPoint → BHonAnalogInput        ([CERT] genericpoints/…:33)
                  → BSmartSensorPoint  (abstract, [CERT] sensors/…:29)
                      ├─ BTemperatureSensor / BCo2Sensor / BHumiditySensor / BTVOCSensor
                      ├─ BAQISensor / BPM25Sensor / BPM1Sensor
                      └─ BPM10Sensor extends BPM1Sensor  ([CERT] sensors/…:10 — NO de BSmartSensorPoint directo)
```
Puntos BACnet-style genéricos `[CERT-a]`: `BHonAnalogInput`(AI)/`BHonAnalogValue extends BNumericWritable`(AV)/`BHonBooleanValue`(BV)/`BHonMultistateValue extends BEnumWritable`(MSV).

**Config de sensor `[CERT]`+`[CERT-a]`**: `BSmartSensorConfig extends BComponent` (verificado) → 7 configs por magnitud (`BTemp/CO2/Hum/TVOC/AQI/PM25/PM1SmartSensorConfig`; `BPM10SmartSensorConfig extends BPM1SmartSensorConfig`), cada una con defaults (ej. CO2: deadband 100, low 1000, high 1400 ppm). `BAlarmLimitsConfig extends BComponent` (alarm enable/low/high/deadband/timeDelay con propIds 1-5). Las propIds mapean a las propiedades BACnet/Modbus del sensor (offset propId=6, unit=7) — el mismo modelo que vimos en [Bloque 95].

**Config de display `[CERT-a]`**: cadena `BDeviceConfiguration → BSmartSensorDeviceConfig → …Display → Config3D/5D` (LED ring color/brightness, LCD cycle/dim/backlight). `displayconfig`: interfaces escalonadas `ISmartSensorLed → LedLcd → D3/D5LedLcd`; `BParameterDisplayConfig` empaca los parámetros visibles como **bitmap** (3D = 4 params AQI/Temp/Hum/CO2; 5D = +PM2.5/TVOC). Enums (9): `BHonSyncStateEnum`, `BConfigSyncNeededStatus` (full/delta/outOfDate sync), `BLedColorEnum`, `BParameterSwitchEnum3D/5D`.

---

## 104.4 — Base compartida y acoplamientos `[CERT-a]`

`honIOBase` exporta ~76 tipos y es la base de varios módulos de protocolo (los que implementan `ISmartSensorDevice` o concretan los puntos/config de `iobase`): Smart Edge BACnet/Modbus ([Bloque 95]), `honBacnetIO`, PanelBus. **Acoplamientos no declarados** `[CERT-a]`: `BIOPointConfig` usa un ícono `module://honBacnetIO/...` y `BIONumericPoint` hardcodea lexicon de `clPanelBus` (`%lexicon(clPanelBus:point.alarm)%`) — en instalaciones sin esos módulos, ícono/textos fallan. El método abstracto `isParentLegal()` lo provee cada protocolo (hook de topología).

---

## 104.5 — Calidad / seguridad `[CERT]` + `[CERT-a]`

**[BUG CERT] Check de convertibilidad de unidad contra sí misma.** `BAlarmLimitsConfig.java:139`: `if (previousUnit.isConvertible(previousUnit))` — compara la unidad consigo misma → **siempre true**; el chequeo real de convertibilidad entre unidades nunca se ejecuta correctamente al convertir límites de alarma.

**[BAJO CERT-a] Otros defectos:** `BTVOCSensor.changed()` llama `super.changed()` **después** de su conversión (vs `BTemperatureSensor` que lo llama antes) → estado transitoriamente inconsistente si el padre revierte la unidad. `HonIOToolkit` imprime a `System.out` si el `Log` no fue inyectado (errores de I/O perdidos). Typos en mensajes de UI (`"Limt"`). Métodos `getValueConversionForPPB/Microgram` mal nombrados (en realidad SET valores). Ninguno es crítico — es un módulo base sin protocolo, sin credenciales.

---

## 104.6 — Conexiones

- **[Bloque 95]** (Smart Edge BACnet/Modbus): aterriza su capa base aquí — `BSmartSensorDevice` implementa `ISmartSensorDevice`, sus puntos extienden `BSmartSensorPoint`, sus drivers concretan `BHonBacnet/ModbusSmartSensorDevice`.
- **[Bloque 86]** (PanelBus/Snapon I/O): los `BSnaponIO*Config` son la config base que PanelBus concreta; el lexicon `clPanelBus` está acoplado aquí.
- **[Bloque 75]** (seguridad): aporta el bug de conversión de unidad + los acoplamientos cross-module no declarados.
