# Bloque 40 — spyderApps Ver28 Catalog (Honeywell Spyder Application Library)

## Resumen Ejecutivo

- **Descubrimiento**: Ver28/ contiene 19 categorías de apps Spyder, **136 archivos totales** (49 .doc + 36 .xml + 22 .libbog + 18 .jar + 6 otros)
- **Formato real**: **.libbog = XML comprimido** (`bajaObjectGraph v4.0`), no binario BOG-like; .xml = índices de catálogo; .doc = documentación Word (meta)
- **Estructura dual**: UserDefined/ (instancias/macros específicas v3-v8) + CommonObjects/ (attachments = documentos reference)
- **LON integration**: Referencias a `SNVT_*` (tipos estándar LON: temp_p=64, hvac_mode=93, hvac_status=73), `programId` patterns (`90 00 0c 52...`), lonUUID + bacnetUUID mappings
- **Versión Spyder**: `ToolVersion="3.7.44.5.206"` (compatible con Spyder Model 5/7); Ver28 = versión firmware/app lib 28
- **Equivalencia kitControl**: spyderApps SON los building blocks reusables Spyder (análogo a blocks HVAC en Niagara)
- **Uso**: Palette de aplicaciones importables via Niagara Workbench → assembly en Logic diagram programador

## Formato Real (.libbog vs .xml vs .jar)

| Extensión | Tipo | Contenido | Ejemplo |
|-----------|------|----------|---------|
| **.libbog** | XML UTF-8 CRLF | `<bajaObjectGraph v4.0>` serialización Baja completa macros/function blocks; no binario | `/Alarms/UserDefined/.../AlarmAnalog.libbog` = Macro Spyder Alarms implementación |
| **.xml** (index) | XML catalog | Índice por categoría: `<index v="ApplicationLibrary">` + entradas `<UUID n="AppName" v="#" type="UserDefined">` | `/Alarms/index.xml` = lista 5 alarmas registradas (AlarmBinary v7, AlarmAnalog v6, etc.) |
| **.jar** | Java archive | Standard.jar por categoría (probablemente compiled loader/runtime binding) | `/Alarms/Standard.jar` |
| **.doc** | MS Word 97-2003 | Documentación gráfica: "Spyder Macro v2.dot" template, block diagrams, datasheets | `Decode/CommonObjects/Attachments/SnvtSwToLogic_SnvtSwToLogic.doc` (8 rev, 2011) |

---

## 19 Categorías — Tabla Resumen

| Categoría | #Archivos | Función | Key Apps |
|-----------|-----------|---------|----------|
| **Alarms** | 9 | Alarmas: binarias, analógicas, hybrid | AlarmBinary (v7), AlarmAnalog (v6), InvalidSetPtAlm, InputOverrideAlarm |
| **CVAHU** | 5 | Constant Volume AHU controller | CVAHU_AP1 (v8, `programId: 90 00 0c 52 00 03 04 38`) |
| **Control** | 6 | PID, cascada, PWM | PWM_Control (v3), PID_Enhanced (v1), CascadeControl_RevAct/DirAct, FlowControlPlus |
| **Decode** | 9 | Conversión SNVT: switch→logic, mode decoding | SnvtSwToLogic, SysSwitch2HVACMode, HVAC2CmdMode |
| **Econo** | 4 | Economizer logic (enthalpy + free cooling) | EconoLogicUnivAP_C7400, EconoLogicUnivAP_BtuPerLb, Economizer_Pkg1 |
| **General** | 10 | Bloques genéricos: switches, selectors, tiempo | +8 más en UserDefined |
| **Logic** | 10 | Lógica booleana: AND/OR/NOT, timers | +3 macros UserDefined |
| **Math** | 11 | Aritmética: interpolación 11/22 pts, NaturalLog, AbsVal, SortSelect, AreaFromDia | IntegerValue, AreaFromDia, Interplation_22Pts |
| **Metering** | 9 | Medición: energy, power, flow totalization | +10 bloques |
| **Psych** | 8 | Psicromética: wet bulb, dew point cálculos (humedad) | DewPtTemp_C_RH_Hvy, WetBulb_F, WetBulb_C |
| **Sched** | 4 | Scheduling: weekly, daily, override | +1 macro UserDefined |
| **Time** | 9 | Time/Date: GetTime, SetTime, MinutesFromMidnight, DetectFirstDayOfMonth | SNVT_time_stamp (id=84) |
| **Tstat** | 5 | Thermostat logic | +3 bloques |
| **UnitVent** | 3 | Unit Ventilator controller | +1 macro |
| **UnitsConv** | 7 | Conversión de unidades (°C↔°F, psi↔kPa) | +2 macros UserDefined |
| **VAV_AHU** | 5 | Variable Air Volume AHU (damper + fan) | VAVAHU_AP1 (v8, `programId: 90 00 0c 52...`), refs SNVT_hvac_mode, SNVT_temp_p |
| **WallModConv** | 3 | Wall module conversion (Sbus wall mod) | ConvWallMod_AbsSP |
| **ZoneTerminal** | 8 | Zone terminal units (damper control, CO2, occupancy) | +20 UserDefined bloques |

**Total: 136 archivos, 108 UserDefined function blocks identificados**

---

## Detalle Técnico por Categoría

### Alarms
```xml
<index v="ApplicationLibrary" ToolVersion="3.7.44.5.206">
 <8ce208de-0473-4be9-8f7f-e67605048d6a n="InvalidSetPtAlm" version="3" type="UserDefined" spyderBrand="true"/>
 <0f572a1a-e9d9-4c46-b1cc-beba6337ddd6 n="AlarmBinary" version="7" type="UserDefined" spyderBrand="true"/>
 <ad468502-dedb-4c03-8c4d-89a6b0f66b38 n="AlarmAnalog" version="6" type="UserDefined" spyderBrand="true"/>
```
- **AlarmAnalog.libbog**: `<bajaObjectGraph>` contiene input pins (Value, HighLimit, LowLimit, Disable, PresetTimeDelay, PostTimeDelay), output (ALARM_STATUS), nested `<honst:Alarm>` block, links.
- ResourceManager: memory flash/NvRAM allocation (startPVID=33536, maxVars=40)

### VAV_AHU / CVAHU
```
<VAVAHU_AP1 programId="90 00 0c 52 00 03 04 38">
  <snvtType>93</snvtType> <!-- SNVT_hvac_mode -->
  <snvtType>73</snvtType> <!-- SNVT_hvac_status -->
  <snvtType>74</snvtType> 
  <snvtType>90</snvtType>
  <snvtType>92</snvtType>
  <snvtType>107</snvtType> <!-- SNVT_lev_percent -->
```
- **programId signature**: LON identifier `90 00 0c 52 00 03 04 38` = Honeywell Spyder HVAC program class
- Inputs: HVAC mode, supply/return temps, flow, occupancy → Outputs: damper%, fan%, setpoint

### Math
- **Interpolation_22Pts / 11Pts**: lookup tables para characterize curves (fan law, valve authority)
- **NaturalLog, AbsVal, SortSelect**: 1st-order primitives

### Psych (Psychrometric)
- **WetBulb_F / WetBulb_C**: calcula wet-bulb temperature desde dry-bulb + RH
- **DewPtTemp_C_RH_Hvy**: dew point = enthalpy-based

### Control
- **PID_Enhanced, PIDwithLimit**: standard PID (Kp, Ki, Kd, setpoint, feedback, output %)
- **CascadeControl_RevAct/DirAct**: nested PID, reverse/direct acting
- **PWM_Control**: pulse-width modulation para proportional valves

---

## UserDefined vs CommonObjects

| AspectFold | UserDefined | CommonObjects |
|------------|------------|----------------|
| **Contenido** | .libbog files = function block implementations | Attachments/ = .doc (documentation only) |
| **Estructura** | UUID-named subdirs c/ versión; macro logic XML serializado | No código, solo references/datasheets |
| **Rol** | **Funcional**: macro logic ejecutable en Spyder runtime | **Referencia**: block diagrams, parameter docs |
| **Importación** | Los que importa Workbench via palette | Metadata/context |

Ejemplo:
- `/Alarms/UserDefined/ad468502-dedb-4c03-8c4d-89a6b0f66b38/AlarmAnalog.libbog` = **executable**
- `/Alarms/CommonObjects/Attachments/AlarmAnalog_Alarm Analog v1.doc` = **diagrama visual** del mismo bloque

---

## Conexión LON Honeywell (Bloque 19)

### SNVT References
Tipos estándar LON encontrados (Standard Network Variable Types):
- **SNVT_temp_p** (64): temperatura presición, ±273.16 a 327.66°C, res=0.01, unidades celsius/kelvin
- **SNVT_hvac_mode** (93): -1=null, 0=auto, 1=heat, 2=warmup, 3=cool, 4=night purge, 5=precool, 6=off, 7=test, 8=emerg heat, ... 20=noload
- **SNVT_hvac_status** (73): compuesto, estado del AHU
- **SNVT_lev_percent** (107): ±163.8%, res=0.005, unidades %
- **SNVT_time_stamp** (84): fecha/hora, 6 bytes

```
<p n="snvtName" v="SNVT_time_stamp"/>
<p n="nsnvtType" t="l:LonInteger" v="84"/>
```

### programId Pattern
`programId: 90 00 0c 52 00 03 04 38` aparece en CVAHU_AP1 y VAVAHU_AP1:
- **90 00 0c** = Honeywell manufacturer prefix (0x900000C en LON)
- **52 00 03 04 38** = device program class + version

Coincide con patrones Bloque 19.4: "ProgramId LON patterns Honeywell 80 00 0c"

### UUIDs Dual
Cada app tiene:
- `lonUUID` = identificador LON/XIF (cross-compatible)
- `bacnetUUID` = identificador BACnet (multi-protocol)

Ejemplo:
```xml
lonUUID="08e905bd-6710-4d08-be15-f945562fbce4"
bacnetUUID="d65b80d4-a6cd-428e-bed7-98f0d25cf1b8"
```

---

## Versión Spyder

- **ToolVersion**: `3.7.44.5.206` (constant en todos index.xml)
- **Ver28**: "versión 28" del catálogo de aplicaciones (no firmware)
- **Compatible**: Spyder Model 5, 7 (classic); anterior a Model 8
- **Relevancia**: En Niagara N4.14, spyderApps Ver28 es biblioteca legacy heredada de OptimizerSupervisor
- **Estado**: Production (ToolVersion stamps release build de Honeywell Spyder Tooling 3.7.x, ~2008-2012)

---

## Relación con kitControl (Bloque 24)

**Conclusión**: spyderApps Ver28 = **equivalente Spyder exacto** de HVAC blocks en kitControl Niagara.

| Aspecto | spyderApps | kitControl |
|---------|------------|-----------|
| **Propósito** | Palette reusable HVAC/control apps | Palette reusable HVAC/control blocks |
| **Lenguaje** | Baja Object Graph XML (.libbog) | BajaScript + Java Components |
| **Target runtime** | Honeywell Spyder hardware controllers | Niagara Framework (Tridium) |
| **Blockos** | PID, Economizer, VAV, Psychro, Math | kitControl: PID, Valve, Fan, Sensor blocks |
| **Importación** | Honeywell Tool v3.7 → Spyder firmware | Niagara Workbench → N4 modules |

---

## Workflow: Importación/Exportación

### Flujo: Estación Real Spyder

1. **Workbench Honeywell (3.7.44+)**: File → Import Library → select category (e.g., /Alarms/)
2. **Parser**: reads index.xml → discovers UserDefined/ macros
3. **Compilation**: .libbog → binary Spyder microcode (proprietary, NOT XML runtime)
4. **Download**: Connect → Target device (Spyder N4, N7) → load program
5. **Execution**: runs native Spyder OS (embedded, real-time HVAC logic)

### Migración a Niagara N4

- `spyderToIrmNxMigrator-wb.jar` (Bloque 25.2) convierte:
  - Spyder macros (.libbog) → IRMNx function blocks (N4 components)
  - PID, Schedule, FunctionBlock logic → BComponents equivalents
  - SNVT links → NiagaraVariables + Properties

---

## Gotchas & Notas Técnicas

1. **.libbog = XML pero propietario**: serializado via Baja framework (`bajaObjectGraph`), no editable con text editor standard sin parser
2. **Memory tracking**: ResourceManager embebido traks flash/NVRAM allocation por programId
3. **Version drift**: cada .libbog tiene `versionNumber=5.206.0` (build timestamp?)
4. **Dual protocol**: simultáneamente LonWorks + BACnet via UUIDs (Honeywell multi-stack)
5. **No source**: .doc files SON la única documentación; .libbog compilados desde original source desconocido
6. **Attachment semantics**: CommonObjects/Attachments/ linked via `<p n="AlarmAnalog_Alarm Analog v1.doc">` en UserDefined .libbog (metadata binding)

---

## Cross-refs a Bloques Existentes

| Bloque | Relación | Notas |
|--------|----------|-------|
| **Bloque 19** (lonHoneywell-rt.jar) | spyderApps → LON network mappings | SNVT types, programId patterns, UUIDs |
| **Bloque 25** (Migration Framework) | spyderToIrmNxMigrator-wb.jar | convierte spyderApps a IRMNx N4 |
| **Bloque 24** (kitControl) | **equivalente Niagara** | analogue HVAC block palette |
| **Bloque 15** (Workbench GUI) | import/export UI | Honeywell Workbench 3.7 x spyderApps |

---

## Conclusión

spyderApps Ver28 es un **catálogo completo de HVAC/control building blocks** para Honeywell Spyder controllers:
- **136 archivos** (22 .libbog XML compilados + 49 docs + 36 índices + 18 runtimes)
- **19 categorías** (alarmas, PID, psicromética, scheduling, unidades de zona, etc.)
- **Heredado en N4.14**: empaquetado en OptimizerSupervisor distribution (legacy compatibility)
- **Migrable**: spyderToIrmNxMigrator convierte a IRMNx blocks N4 (Bloque 25)
- **Not documented**: mental model existente ignoraba este repositorio; especificación LON + Baja serialization clarificado.

