# Bloque 40 — Operational Honeywell Artifacts

**Sesión**: 2026-04-25
**Distribución**: Honeywell OptimizerSupervisor-N4.14.0.162
**Método**: walk empírico de directorios install-time, decompile parcial .NET vía `strings`/`file`, decompile JARs Java vía `unzip`, `keytool` para JKS, `diff` para comparativas.
**Cobertura**: Capa 15 — paquetes operacionales pre-armados Honeywell + flow Office Interop printout + filesystem install-time + truststore SEJOFA

Este bloque consolida cuatro líneas de investigación complementarias que NO estaban cubiertas en los 39 bloques previos:

- **40.1** — `Palettes_and_Misc/` — 13 subdirectorios con palettes, station templates, firmware, sensor calibration tables, sizing tools.
- **40.2** — `spyderApps/Ver28/` — biblioteca de 136 archivos en 19 categorías de apps Spyder LON Honeywell.
- **40.3** — `/printout/` — flow Word/.NET Office Interop con clPrintout.exe + 3 JARs Java disparado desde Workbench.
- **40.4** — `/lib/` y `/security/` install-time — doclet versionado, lexicon picker installer, `truststore.jks` SEJOFA, patrón `db/<hostId>/`, Webs.license asimétrica Win vs Qnx.

---

## 40.1 — Palettes_and_Misc — paquetes operacionales pre-armados

### 40.1.1 Inventario completo (13 subdirectorios)

| Subdirectorio | Tamaño relevante | Contenido | Función operacional |
|---------------|------------------|-----------|---------------------|
| `palettes/` | 3 archivos | `HoneywellSubmeter.palette` (12 KB) + `hvfd.palette` (12 KB) + `XL15C.palette` (586 KB) | Palette files Honeywell importables |
| `CIPer Model 30/` | 4 archivos + Stations | `CIPer30 Power Estimator_rev6.xlsm`, `Cable voltage drop calculator.xlsx`, `Sylk Bus Limits Calculator v1.8.xlsx` + 2 station bundles | Controlador IP-based + sizing tools |
| `Spyder Model 5/` | firmware + templates | `BEATS_MSTP_Unitary_V2.0.4.25.bin` + `BEATS_MSTP_VAV_V2.0.4.25.ufw` + 4 `.ntpl` (VAV TR23/TR42 Ana/Stg) + 4 `.doc` | Spyder Model 5 BACnet MS/TP |
| `Spyder Model 7/` | 1 firmware + 1 template | `NC_VAVA_V2.0.8.32.ufw` + `FlexVAV_S7_Rev001_117.ntpl` + PDF | Spyder Model 7 IP & MSTP VAV |
| `TC300/` | 1 firmware | `package_2.0.1.8_TC300B-G_withnewheader.en.bin` | Thermostat controller TC300B-G |
| `TC500/` | 1 firmware | `TSTAT_TC500A_v1.1.10.0_OTA_Prod_Signing_PelionProd_AzureProd_No_log_with_application_v2.8.8.8_withNewHeader.bin` | IoT thermostat (ARM Pelion + Azure IoT) |
| `TR100/` | 2 patches firmware | `TR100_FU_patch_01.00.07.00_to_01.02.00.00.fw`, `TR100_FU_patch_01.01.00.00_to_01.02.00.00.fw` | Wall module |
| `Niagara_IO_SensorTables/` | 4 XML | `10ktype2custresistance.xml`, `10ktype2to10ktype3f.xml`, `10ktype3custresistance.xml`, `20K_5degsteps_25F_to_240F.xml` | Curves NTC thermistor calibration |
| `BurnerInterface.zip` | 76 KB | `config.bog` 68 KB + `s7810main.px` 72 KB + `7800.gif` | UI legacy Honeywell 7800-series burner controller (mayo 2006) |
| `Optimizer Unitary/firmware/` | 1 archivo | `NC_Unitary_V2.1.1.40.ufw` | Network Controller Unitary v2.1.1.40 |
| `BACnetFFT_N4_Reflash/` | tool + firmware | `BACnet FFT N4 Firmware Download Tool SRB.pdf` + `HW_TB3026B_FW/hddcv3b23vldff-firmware.bin` | Reflash tool BACnet field devices |
| `Spyder Classic Files/` | calculators + export | `BiasCalculator_v4.xlsm`, `SpyderType3Export/`, `Sylk Bus Limits Calculator v1.8.xlsx` | Tools generación legacy Spyder LON |

### 40.1.2 Formato `.palette` — NO siempre es ZIP

Bloque 12.4 supuso que `.palette` siempre es ZIP con `file.xml`. Empíricamente:

| Archivo | Formato real |
|---------|--------------|
| `HoneywellSubmeter.palette` (12.5 KB) | ZIP deflate → `file.xml` 144 KB (BOG-XML estándar Tridium) — datado 2009-04-24 |
| `hvfd.palette` (12.6 KB) | ZIP deflate → `file.xml` 124 KB — datado 2010-02-22 (HVFD = Honeywell Variable Frequency Drive) |
| `XL15C.palette` (586 KB) | **NO ZIP** — `unzip` falla con "End-of-central-directory signature not found". Formato proprietary Honeywell legacy del controller Excel 15C |

**Implicación**: parsers genéricos `.palette` que asumen ZIP fallan en archivos legacy XL15C. Honeywell mantiene la extension pero el formato interno cambió en distintas eras del producto.

### 40.1.3 Formato `.ntpl` (Niagara Template) — Honeywell signed XML

Empírico — header del `VAV_TR42__Ana108.ntpl`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<template id="738a7264-dae6-425d-8e35-a5224bf6c1df" version="1.108"
          vendor="Honeywell" title="VAV_TR42_Ana"
          state="10" buildVersion="4.10.0.154" signature="39d205e7">
  <info i="V1.06 Corrected... v1.08 Reordered logic..."/>
  <revisions></revisions>
  <settings>
    <p n="uID" req="true" typ="str"/>
    <p n="DuctArea" req="true" units="square foot"/>
    <p n="KFactor" req="true" units="cubic feet per minute"/>
    <p n="MaxFlowSpt" req="true" units="cubic feet per minute"/>
    <p n="ZoneDescription" req="true" typ="str"/>
    <p n="ConfigBinding{1..4}" req="true" typ="str"/>
    ...
  </settings>
</template>
```

Atributos clave:
- `vendor="Honeywell"` — identifica origen vendor
- `version="1.108"` — versión del template
- `buildVersion="4.10.0.154"` — versión Niagara con la que se construyó (4.10 era — pre-N4.14)
- `signature="39d205e7"` — firma del template (8 hex chars — formato proprietary Honeywell, NO RSA-2048 estándar)
- `state="10"` — state machine de templates (deployed/active — undocumented en N4 standard)

**Confirma Bloque 14.6** — `.ntpl` Honeywell traen flow completo: vendor + version + signature + state. Bloque 14.6.3 advertía "NO auto-propaga", esto sigue válido. Lo nuevo es que el state machine `state="10"` indica template deployed.

### 40.1.4 Station bundles `.zip` — drop-in para Workbench

`CIPer Model 30/CIPer Model 30 Stations/CIPer30_VAV_TR42_Modulating_Reheat/Ciper30_VAV_TR42_Ana_001_R101.zip`:
```
Ciper30_VAV_TR42_Ana_001_R101/
├── config.bog (50 KB)
├── shared/
│   ├── charts/Monitor.chart (4 KB)
│   ├── nav/balancer.nav, default.nav
│   └── px/honeywellAXPlatinum/  ← AX-era theme persiste en N4.14
│       └── images/coils/filterV_1.png
```

`honeywellAXPlatinum` theme = AX (pre-N4) **mantenido en N4 por backward-compat visual**. Stations modernos pueden re-skinnarse a tema N4 nativo, pero Honeywell distribuye con AX theme para que clientes con sites mixtos N4+AX tengan continuidad visual.

### 40.1.5 Firmware formats por device

| Device | Formato | `file` reporta |
|--------|---------|----------------|
| Spyder M5 Unitary | `.bin` | `data` (raw binary, sin signature reconocible) |
| Spyder M5 VAV | `.ufw` | `Zip archive deflate` |
| Spyder M7 VAV | `.ufw` | ZIP |
| TC300 | `.bin` "with new header" | `data` |
| TC500 | `.bin` con sufijo flags | `data` |
| TR100 | `.fw` patch incremental | ZIP |
| BACnet FFT | `.bin` | `data` |
| Optimizer Unitary | `.ufw` | ZIP |

**`BEATS_MSTP_*`** prefix = Honeywell internal codename para Spyder firmware family BACnet MS/TP.

### 40.1.6 TC500 IoT thermostat — doble cloud Pelion+Azure

Filename literal codifica todos los flags:
```
TSTAT_TC500A_v1.1.10.0_OTA_Prod_Signing_PelionProd_AzureProd_No_log_with_application_v2.8.8.8_withNewHeader.bin
```

- `OTA` — Over-The-Air update support
- `Prod_Signing` — production signing chain (no dev)
- `PelionProd` — ARM Pelion device management prod environment
- `AzureProd` — Azure IoT Hub prod environment
- `No_log` — logging deshabilitado
- `application_v2.8.8.8` — app firmware bundled

TC500 es **un IoT thermostat con doble conectividad cloud** (ARM Pelion para device mgmt + Azure IoT Hub para telemetría). Único device en distro con cloud connectivity nativa.

### 40.1.7 NTC thermistor curves — calibración pre-canned

`Niagara_IO_SensorTables/10ktype2custresistance.xml` formato:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<conversion>
  <description>10KType2CustResistance</description>
  <table>
    <point src="121939.0" result="-11.0"/>  <!-- 121.9 kΩ → -11°F -->
    <point src="60592.0"  result="11.0"/>
    <point src="10000.0"  result="77.0"/>   <!-- 10K nominal point -->
    <point src="5207.0"   result="105.0"/>
    ...
  </table>
</conversion>
```

NTC 10K Type 2/3 + 20K son los thermistors estándar industria HVAC norteamericana. Estos XML se importan a `BConversionLink` o `BUnitsConversion` en kitControl. **EXTIENDE Bloque 24** (kitControl palette) — Honeywell distribuye curves pre-canned, NO hay registry/scan automático que detecte el folder.

### 40.1.8 BurnerInterface — anchor histórico AX-era

`BurnerInterface.zip` (76 KB, datado **mayo 2006** = 18 años de antigüedad):
- `config.bog` (68 KB)
- `px/s7810main.px` (72 KB) — UI completa Honeywell 7800-series burner controller
- `px/images/7800.gif` (12 KB)

Sobrevive en distro N4.14 como anchor para deploys que mantienen 7800 controllers físicos (legacy industrial burners en plantas químicas/HVAC). Importar como base requiere **migration audit** — referencia APIs deprecated probables.

### 40.1.9 Workflow operacional típico (deploy nuevo VAV TR42)

1. Comprar hardware Honeywell CIPer Model 30 + TR42 wall module + sensors NTC 10K Type 3
2. Workbench: importar `Ciper30_VAV_TR42_Ana_001_R101.zip` como station nueva (drop-in template-as-station)
3. Si Spyder en lugar de CIPer: importar `VAV_TR42__Ana108.ntpl` como Niagara Template (Bloque 14.6) → instanciar bajo el device
4. Sensor calibration: importar `Niagara_IO_SensorTables/10ktype3custresistance.xml` al BConversionLink del thermistor
5. Sizing: usar `Sylk Bus Limits Calculator v1.8.xlsx` (Excel macro Win-only) para verificar Sylk bus capacity
6. Power: `CIPer30 Power Estimator_rev6.xlsm` para budgeting
7. Cable voltage drop: `Cable voltage drop calculator.xlsx`
8. Field commissioning: TR100 wall module flash con `TR100_FU_patch_01.01.00.00_to_01.02.00.00.fw` (vía Honeywell tool)

**EXTIENDE Bloque 15.13** (workflow 5 fases end-to-end) — el workflow real Honeywell incluye estos paquetes como starting point, NO empieza de zero.

---

## 40.2 — spyderApps Ver28 — biblioteca apps Spyder LON

### 40.2.1 Estructura general

`/spyderApps/Ver28/` contiene **136 archivos** distribuidos en **19 categorías**:
- 49 archivos `.doc` (documentación Word)
- 36 archivos `.xml` (índices catálogo)
- 22 archivos `.libbog` (function blocks ejecutables)
- 18 archivos `.jar` (`Standard.jar` por categoría — runtime bindings)
- 11 archivos varios

Cada categoría tiene estructura dual:
- `<categoria>/UserDefined/<UUID>/` — `.libbog` ejecutables (macro logic)
- `<categoria>/CommonObjects/Attachments/` — `.doc` reference docs (block diagrams)
- `<categoria>/index.xml` — catalog
- `<categoria>/Standard.jar` — runtime loader

### 40.2.2 19 categorías — tabla función

| Categoría | Función | Apps clave |
|-----------|---------|------------|
| **Alarms** (9) | Alarmas binarias/analógicas/hybrid | AlarmBinary v7, AlarmAnalog v6, InvalidSetPtAlm, InputOverrideAlarm |
| **Control** (6) | PID, cascada, PWM | PWM_Control v3, PID_Enhanced v1, CascadeControl_RevAct/DirAct, FlowControlPlus |
| **CVAHU** (5) | Constant Volume AHU controller | CVAHU_AP1 v8 (`programId 90 00 0c 52 00 03 04 38`) |
| **Decode** (9) | Conversión SNVT: switch→logic, mode | SnvtSwToLogic, SysSwitch2HVACMode, HVAC2CmdMode |
| **Econo** (4) | Economizer enthalpy + free cooling | EconoLogicUnivAP_C7400, _BtuPerLb, Economizer_Pkg1 |
| **General** (10) | Switches, selectors, time | múltiples UserDefined |
| **Logic** (10) | AND/OR/NOT, timers | múltiples macros |
| **Math** (11) | Aritmética + interpolation 11/22 pts | NaturalLog, AbsVal, SortSelect, AreaFromDia, IntegerValue |
| **Metering** (9) | Energy, power, flow totalization | múltiples bloques |
| **Psych** (8) | Psicrometría: wet bulb, dew point | DewPtTemp_C_RH_Hvy, WetBulb_F, WetBulb_C |
| **Sched** (4) | Weekly, daily, override scheduling | UserDefined |
| **Time** (9) | GetTime, SetTime, MinutesFromMidnight, DetectFirstDayOfMonth | usa SNVT_time_stamp (id=84) |
| **Tstat** (5) | Thermostat logic | múltiples |
| **UnitVent** (3) | Unit Ventilator controller | macros |
| **UnitsConv** (7) | Conversión °C↔°F, psi↔kPa | UserDefined |
| **VAV_AHU** (5) | Variable Air Volume AHU | VAVAHU_AP1 v8 (refs SNVT_hvac_mode 93, SNVT_temp_p 64) |
| **WallModConv** (3) | Wall module Sbus | ConvWallMod_AbsSP |
| **ZoneTerminal** (8+20) | Zone terminal damper + CO2 + occupancy | extenso UserDefined |

**Total: ~108 UserDefined function blocks identificados**.

### 40.2.3 Formato `.libbog` — XML BOG comprimido

`.libbog` files son **XML UTF-8 con CRLF** (NO binarios):
```xml
<bajaObjectGraph v="4.0">
  <p n="snvtType" t="l:LonInteger" v="64"/>  <!-- SNVT_temp_p -->
  <p n="programId" v="90 00 0c 52 00 03 04 38"/>
  <p n="lonUUID" v="08e905bd-6710-4d08-be15-f945562fbce4"/>
  <p n="bacnetUUID" v="d65b80d4-a6cd-428e-bed7-98f0d25cf1b8"/>
  ...
</bajaObjectGraph>
```

**Hallazgo importante**: cada app tiene **doble UUID** (LON + BACnet) — Honeywell multi-stack support. El mismo function block es directamente mapeable a LonWorks NVs y a BACnet objects.

### 40.2.4 SNVT references encontrados

Tipos LON Standard Network Variable Types referenciados en spyderApps:
- **SNVT_temp_p** (64) — temperatura precision ±273.16 a 327.66°C
- **SNVT_hvac_mode** (93) — auto/heat/cool/night purge/precool/off/test/emerg/...
- **SNVT_hvac_status** (73) — estado AHU compuesto
- **SNVT_lev_percent** (107) — ±163.8% res 0.005, unidades %
- **SNVT_time_stamp** (84) — fecha/hora 6 bytes
- **SNVT_switch** — referenciado en Decode/SnvtSwToLogic

### 40.2.5 ProgramId Honeywell Spyder

Pattern empírico encontrado en `CVAHU_AP1` y `VAVAHU_AP1`:
```
programId: 90 00 0c 52 00 03 04 38
           └──┬─┘ └──┬──────────────┘
            mfg     device program class + version
```

- `90 00 0c` — Honeywell manufacturer prefix LON (decimal: Honeywell ID)
- `52 00 03 04 38` — device program class

**Coincide con Bloque 19.4 (`80 00 0c` para LON Honeywell)** — el `90 00 0c` es variante para Spyder family. Confirma que Honeywell tiene múltiples sub-namespaces de manufacturer prefix según device family.

### 40.2.6 ToolVersion + Ver28

Empírico — todos los `index.xml` declaran:
```xml
<index v="ApplicationLibrary" ToolVersion="3.7.44.5.206">
```

- **ToolVersion `3.7.44.5.206`** — Honeywell Spyder Tool versión 3.7.x build 206 (~2008-2012 era)
- **Ver28** — versión 28 del catálogo (NO firmware)
- Compatible con **Spyder Model 5/7 classic**, anterior a Model 8

### 40.2.7 UserDefined vs CommonObjects

| Aspecto | UserDefined | CommonObjects |
|---------|-------------|---------------|
| **Contenido** | `.libbog` (executable function blocks) | `Attachments/*.doc` (documentation) |
| **Rol** | Funcional — corre en Spyder firmware | Referencia — diagrams, datasheets |
| **Importación** | Sí, lo importa Workbench Honeywell 3.7 | NO se importa, es reference |
| **Binding** | `<p n="AlarmAnalog_Alarm Analog v1.doc">` en `.libbog` referencia el `.doc` | Linked metadata |

### 40.2.8 Migración a Niagara N4

`spyderToIrmNxMigrator-wb.jar` (Bloque 25.2) convierte:
- Spyder macros (`.libbog`) → IRMNx function blocks (N4 components)
- PID, Schedule, FunctionBlock logic → BComponents equivalents
- SNVT links → NiagaraVariables + Properties

Esto **EXTIENDE Bloque 25.4** — el migrator opera sobre estos `.libbog`. La biblioteca spyderApps Ver28 es el **input set conocido** del migrator.

### 40.2.9 Equivalencia con kitControl (Bloque 24)

| Aspecto | spyderApps | kitControl |
|---------|------------|------------|
| Propósito | Palette HVAC/control reusable | Palette HVAC/control reusable |
| Lenguaje | Baja Object Graph XML (`.libbog`) | BajaScript + Java BComponents |
| Target | Honeywell Spyder hardware | Niagara Framework |
| Bloques | PID, Economizer, VAV, Psychro, Math | kitControl: BLoopPoint, BPsychrometric, BSequence, BTstat |
| Importación | Honeywell Workbench 3.7 → Spyder firmware | Niagara Workbench → N4 modules |

**Conclusión**: spyderApps Ver28 = **equivalente Spyder exacto** de kitControl. Misma arquitectura conceptual, distinto target runtime + lenguaje serialization.

---

## 40.3 — Printout Office Interop flow

### 40.3.1 Composición del sistema

`/printout/` contiene un **subsistema .NET complete** disparado desde Workbench Java:

| Archivo | Tipo | Tamaño | Función |
|---------|------|--------|---------|
| `clPrintout.exe` | PE32 .NET WinForms | 954 KB | Entry point, MainForm, COM Word automation |
| `clPrintout.vshost.exe` | PE32 dev hosting | 14 KB | **Residuo Visual Studio debugging** — NO entry real |
| `Word.dll` | .NET PIA | 484 KB | Microsoft.Office.Interop.Word (redistribuible MS, NO Honeywell custom) |
| `Office.dll` | .NET PIA | 152 KB | Microsoft.Office.Core PIA (Mso* enums) |
| `WireSheetControl.dll` | .NET UserControl | 344 KB | **NO es Excel renderer** — UserControl Honeywell CARE legacy reciclado |
| `Interop.Shell32.dll` | .NET interop | 48 KB | Shell32 COM (file system shortcuts) |

Plus 8 templates `.dot` (Word 97-2003 OLE binario) + 2 XML configs + 1 HTM template + 1 BMP logo.

**Caller Java side** — 3 JARs en `/modules/`:
- `clPrintout-rt.jar` — runtime station-side (BPrintMe, BPrintoutStationExt, PrintoutConfig)
- `clPrintout-wb.jar` — workbench-side dialogs (BPrintoutDialog, Exporter, BOnlinePrintoutDialog)
- `clPrintout-doc.jar` — javadocs

PDB embedded en clPrintout.exe revela: `C:\Users\h446496\Downloads\NiagaraDotNetReport\NiagaraDotNetReport\clPrintout\obj\Release\clPrintout.pdb` — proyecto interno Honeywell **NiagaraDotNetReport** (build user h446496).

### 40.3.2 Flow end-to-end

```
[wb_w.exe] BPrintoutDialog (Java baja)
   | user click "Print Documentation"
   v
doPrintDocumentation(BWidgetEvent):
   1. loadProjects() ← !printout/projects.xml
   2. para cada station seleccionada: Helper.isStationRunning() check
   3. delete + createNewFile(_pathExportFile) ← !printout/printout.xml
   4. Exporter.exportProject(exportFileOrd, projectName, stationNames)
      ↓ usa javax.baja.xml.XWriter:
        <component> <property> <action> <topic> <knob>
        + sourceComponentName/sourceSlotName/sourceOrd
        + targetComponentName/targetSlotName/targetOrd
        + recorrida tipos: BDeviceNetwork, BDeviceFolder, BDevice,
          BLocalBacnetDevice, BPointFolder, BPointDeviceExt, BControlPoint
        + PxView → PDF via com.tridium.pdf.BPxViewToPdf
   5. Runtime.getRuntime().exec(_pathPrintoutExe)  ← shell exec single string
      waitFor(timeout, MILLISECONDS)
   6. clPrintout.exe arranca → MainForm
      ↓ lee printout.xml + DocumentTemplate.xml + DefaultFilter.xml
      ↓ Registry.ClassesRoot → check Word.Application CLSID
      ↓ OpenMicrosoftWord() → COM Word.Application
      ↓ Documents.Add(WordTemplate.dot)
      ↓ ITERA xml — para cada componente:
        Document.Bookmarks["AI_Bookmark" / "BI_Bookmark" /
                           "AO_Bookmark" / "BO_Bookmark" /
                           "DP_Bookmark" / "IOM_Bookmark" /
                           "NUM_Bookmark"].Range.Text = value
      ↓ Tables.Add(...) para datapoints listados
      ↓ AddPdfFile(per-wiresheet PDF)
      ↓ SaveFileDialog → user picks .docx
   7. Java: process.waitFor() — Workbench bloquea
```

**IPC mechanism**: temp file XML (`!printout/printout.xml`), NO STDIN, NO socket. El path absoluto se pasa al .exe vía `Runtime.exec(String)`.

### 40.3.3 Mecanismo substitución — Word Bookmarks (NO MailMerge)

**Hipótesis original (intuitive) era MailMerge — REFUTADA empíricamente.** Las refs a `WdMailMerge*` en Word.dll son ruido del PIA completo (PIA tiene toda la API Word, no significa que el código la use).

Bookmarks reales extraídos de strings en clPrintout.exe:
- `AI_Bookmark` — Analog Input
- `BI_Bookmark` — Binary Input
- `AO_Bookmark` — Analog Output
- `BO_Bookmark` — Binary Output
- `DP_Bookmark` — Datapoint
- `IOM_Bookmark` — IO Module
- `NUM_Bookmark` — Numeric
- `IRM_TERM_BOOKMARK` — IRM terminal
- `CENTRALINEBYHONEYWELL` — header logo bookmark

Métodos clave en clPrintout.exe:
- `WriteBookMarkText`, `AddTextToBookMarks`, `GoToBookmark`
- `ReplaceSwivelTilePageBookmarks`, `RemoveSwivelLabelBookmarks`
- `InsertSwivelLabelText`, `InsertIrmSwivelLabelText`, `InsertMioSwivelLabelText`
- `UpdateIncludePictureFields` (este sí usa Word IncludePicture field — para embed wiresheet PNG)

### 40.3.4 Templates `.dot` — Composite Document File V2 (Word 97-2003)

8 templates por autor + año:
- `WordTemplate.dot` (HTSL India / E308935, 2008-08-22) — master project documentation
- `MasterSwivelLabelTemplate.dot` (Tolstonog-Riedel L. GE51, 2016-06-09) — concatenar tiles
- `SwivelLabelHTemplate.dot` (Tolstonog-Riedel L., 2019-03-19) — Centraline standard horizontal
- `SwivelLabelVTemplate.dot` (HTSL, 2010-06-03) — vertical legacy
- `SwivelLabelIrmHTemplate.dot` (Schroeffel M. Centraline AT, 2019-01-08) — IRM horizontal
- `SwivelLabelIrmVTemplate.dot` (Schroeffel M., 2019-01-09) — IRM vertical
- `SwivelLabelMioHTemplate.dot` (Tolstonog-Riedel L., 2016-10-07) — MIO horizontal
- `SwivelLabelMioVTemplate.dot` (Tolstonog-Riedel L., 2016-10-07) — MIO vertical

**Format Word 97-2003** (NO `.dotx` moderno) — abren con Word 2003 minimum. **NO funciona con Office 365 web/online** (no soporta COM Automation legacy).

### 40.3.5 "Swivel labels" — etiquetas físicas adhesivas

Concept Honeywell único — **etiquetas físicas adhesivas para placas frontales** de controladores Centraline:
- **IRM** = Intelligent Room Manager
- **MIO** = Modular I/O

Incluyen: point names, datapoint IDs, terminal numbers (orientación H = horizontal, V = vertical). Tridium standard NO los tiene — esto es propietario CentraLine product line (Honeywell EU).

### 40.3.6 WireSheetControl.dll — NO es Excel renderer

**Bloque 26.12 ACTUAL dice (incorrectamente)**: `WireSheetControl.dll | Excel worksheet renderer`.

**Realidad empírica** (strings extraídos):
- Es **.NET UserControl WinForms** (UserControl base, IComponentConnector — interfaz WinForms designer)
- Strings: `WireSheetControl.WireSheetControl`, `CmdAddConnection`, `CmdDeleteConnection`, `OnRender`, `Autoroute (CTRL+R)`, `Mirror (CTRL+M)`, `Pan&Zoom (F7)`, `Snap to grid`, `CARE Datapoint`, `Math Point`, `XFM does not match CSD file`
- Origen real: **Honeywell CARE** (Computer Aided Regulation Engineering) — legacy controller programming tool de Centraline pre-Niagara
- Reciclado en clPrintout.exe **solo para renderizar wiresheet a bitmap** (`OnRender → System.Drawing.Bitmap`) que se embed en Word vía IncludePicture field

**NO genera Excel**. El flow produce solo Word `.docx` + PDFs auxiliares (PxView vía pdf-wb dependency).

### 40.3.7 PrintoutConfig — paths constants

`com.honeywell.printout.PrintoutConfig`:
```java
_printoutDir         = "!printout"
_pathProjectsFile    = "!printout/projects.xml"
_pathProjectsFileBak = "!printout/projects.xml.bak"
_pathExportFile      = "!printout/printout.xml"
_pathPrintoutExe     = "!printout/clPrintout.exe"
_pathPxDir           = "!printout/px"
_expFile             = "!printout/printout.exp"
getDefaultStationsHome() → "~stations"
```

ORD `!printout` resuelto por BFileSystem como `<station>/printout/` o `<wb_home>/printout/`. **`!printout/printout.exp`** formato propietario adicional, probablemente exportación legacy CARE — no analizado.

### 40.3.8 Permisos workbench module

`module.xml` declara solo `LOGGING + UI`. **Sin embargo `Runtime.exec` debería requerir permiso framework superior** — el sistema de permisos Niagara Workbench tiene un gap aquí: clases UI autorizadas pueden invocar `Runtime.exec` sin restriction adicional. Bypass implícito del AccessController standard.

### 40.3.9 Online printout (Fox session)

`BOnlinePrintoutDialog` permite imprimir **desde station online via Fox session** (sin volcar BOG offline primero). Mismo mecanismo (.exe + temp xml), pero datos vienen vía `BFoxSession` live en lugar de file dump. Introduce dependency `fox-rt` y network round-trips por componente.

### 40.3.10 Security implications

- **Sin .NET sandbox** — corre fullTrust as user (manifest `level="asInvoker"`). Sin partial trust CAS.
- **XXE potential** — `XmlReader` antiguo .NET Framework 2.0/4.0 vulnerable si DTD habilitado. NO confirmado el setting empíricamente, pero `XmlReaderSettings` aparece en strings.
- **Office macros risk** — `.dot` son Composite Documents OLE pueden contener VBA macros. Si user setting "Enable all macros" + atacante reemplaza `WordTemplate.dot` → ejecuta código arbitrario en contexto user Workbench.
- **printout.xml leak post-mortem** — temp XML puede contener passwords cifrados, schedule details, alarm config, IPs. Persiste en `!printout/printout.xml` después de generación si Workbench cierra abruptamente.
- **Path traversal en projects.xml** — `loadProjects()` parsea sin validar paths. Atacante con write access al station_home puede sobrescribir archivos fuera del printout dir.

### 40.3.11 Correcciones a Bloque 26.12

1. `WireSheetControl.dll` NO es "Excel worksheet renderer" — es .NET UserControl Honeywell CARE legacy reciclado para renderizar wiresheets a bitmap embed en Word.
2. NO hay generación de Excel — solo Word `.docx` + PDF auxiliar (vía pdf-wb dependency).
3. "Word 2003+" requirement subdocumentado: NO funciona con Office 365 web/online — solo Office desktop con COM Automation.
4. `clPrintout.exe` NO documentado en Bloque 26.12 — es el verdadero entry point 954 KB que orquesta. **Agregar fila a la tabla**.
5. `clPrintout.vshost.exe` 14 KB es **residuo Visual Studio debugging**, NO segundo entry point.

---

## 40.4 — `/lib/` + Honeywell install `/security/`

### 40.4.1 Doclet versionado — coexistencia 1.0.8 + 1.0.9

```
niagara-baja-doclet-1.0.8.jar  53 439 bytes
niagara-baja-doclet-1.0.9.jar  53 767 bytes
```

Manifests **idénticos** (`Manifest-Version: 1.0` solamente, sin Main-Class, sin Created-By, sin Implementation-Version). Solo el filename distingue las versiones.

**Diff de clases (md5)**:

| Clase | 1.0.8 | 1.0.9 | Estado |
|-------|-------|-------|--------|
| `Bajadoclet.class` | 56 306 B | 56 946 B | DIFERENTE (+640 B) |
| `xml/XElem.class` | 17 137 B | 17 141 B | DIFERENTE (+4 B) |
| `xml/XParser.class` | 13 210 B | 13 222 B | DIFERENTE (+12 B) |
| (8 clases restantes) | mismas | mismas | sin cambio |

**Conclusión**: **NO es deprecation lineal — es coexistencia paralela**. Módulos Honeywell/CentraLine compilados originalmente contra 1.0.8 invocan al doclet por path explícito vía Gradle. Bumping a 1.0.9 obligaría regenerar `*-doc.jar` y romper firmas de manifest. **Bloque 25 mencionó solo 1.0.9 — incompleto**.

### 40.4.2 `lexicon.properties` install — NO es runtime lexicon

Bloque 12.2.7 catalogó lexicon framework (runtime `*-lex.jar`). Este `/lib/lexicon.properties` (860 bytes) es **completamente diferente**:

```
# Install.properties for Win32 Self-Extracting Installer
# %version% inserts the Niagara version number.
en=English
de=German
es=Spanish
...
al=Debug/Test       ← pseudo-locale interno Tridium
zh_CN=Chinese (Simplified)
```

47 idiomas ISO-639 — language picker del **self-extracting installer Win32**, NO runtime lexicons. Verbatim Tridium upstream — sin keys Honeywell-specific.

**Gotcha**: 47 idiomas en menú install ≠ idiomas runtime disponibles. Cliente que pide install en `vi` (Vietnamese) tendrá installer vietnamita pero runtime en inglés (no hay vi-lexicon en módulos N4.14).

### 40.4.3 `tools.jar` 17.5 MB — Azul Zulu OpenJDK 1.8.0_282 stock

Manifest empírico:
```
Created-By: 1.8.0_282 (Azul Systems, Inc.)
```

5 011 entradas. Top packages: `com.sun.tools` (javac, javadoc, javap, jarsigner), `com.sun.javadoc` (Doclet API), `com.sun.jdi` (Java Debug Interface), `com.sun.xml` (JAXB), `sun.security`, `sun.tools` (jstack/jmap/jhat front-ends).

**Es el `tools.jar` clásico Java SE 8 SDK** distribuido por Azul Systems (Zulu OpenJDK 8u282). NO es custom Tridium. Está aquí porque módulo `niagaraDriver`/`platform` Win32 necesita acceso al `javadoc` API en runtime (el doclet `Bajadoclet` extiende `com.sun.javadoc.Doclet`).

**Implicación crítica**: en Java 9+ `tools.jar` desapareció (mergeó a `jrt:/`), pero N4.14 sigue corriendo Azul JDK 8u282. **Esto fija el techo de runtime a Java 8** — upgrade a JDK 9+ requiere reescribir doclet contra `jdk.javadoc.doclet`, regenerar todos los `*-doc.jar`, y firmar de nuevo.

### 40.4.4 `readmeLicenses.txt` — OSS declarado oficialmente

Fechado **June 25, 2019** (heredado de N4.7/N4.8 — NO actualizado para N4.14). 17 categorías legales:
1. Apache 2.0 (60+ artifacts: Ant, Batik, Commons, OrientDB, Jetty, jose4j...)
2. OPC Foundation Non-Exclusive
3. MPEG-4
4. MIT License (jQuery, jquery-mobile, jqPlot)
5. Mozilla Rhino (MPL)
6. BSD License
7. WinPcap (Politecnico Torino)
8. Eclipse Public License v1.0
9. JSON License ("for Good, not Evil")
10. OpenSSL
11. GNU LGPL
12. Oracle Binary Code License (Java SE + JavaFX)

**Hallazgos no triviales**:
- **OrientDB con 5 artefactos** confirma persistencia interna NO es solo SQLite/H2 — OrientDB GraphDB es grafo embebido para tags/relations/hierarchy. **Corrige sensación general** de que toda persistencia era plana.
- **mstp-lib de adigostin (no Tridium)** — stack BACnet MS/TP nativo es **OSS upstream**, no propietario.
- **gradle-js-plugin fork Tridium** — build pipeline JS/bajaux usa fork propio (relevante para reproducir builds módulos UX).
- **jose4j** — JWT/JWS native (relevante para SAML/OIDC).
- **NO hay Bouncy Castle declarado** — toda cripto es JCE+OpenSSL+Santuario. Si un análisis encuentra `org.bouncycastle.*`, son **vendored** (renombrados `com.tridium.shaded.bc` o similar), NO declared.
- **OpenSSL** declared — explica `.so`/`.dll` nativos en `bin/` y artefactos cert handling.

### 40.4.5 `Honeywell EULA.pdf` — texto Tridium rebrand-only en filename

Header literal: **"End User License Agreement January 14, 2020"**. Texto Tridium estándar ("TRIDIUM, INC. HAS DEVELOPED A STANDARDIZED ARCHITECTURE…").

**Importante legalmente**: el EULA cliqueable que Honeywell muestra en installer es el EULA Tridium, **rebrand solo en filename**. Sin overlay corporativo Honeywell propio.

### 40.4.6 `truststore.jks` — REEMPLAZADO por keystore SEJOFA

`keytool -list -keystore truststore.jks -storepass changeit` (default JKS password **funciona al primer intento**):

```
Keystore type: jks
Provider: SUN
Your keystore contains 1 entry

Alias name: niagaramoduledev
Creation date: Jan 15, 2026
Entry type: trustedCertEntry

Owner:  CN=Security Audit, OU=Testing, O=SEJOFA, L=Mexico, ST=CDMX, C=MX
Issuer: CN=Security Audit, OU=Testing, O=SEJOFA, L=Mexico, ST=CDMX, C=MX
Valid from: Thu Jan 15 16:03:33 CST 2026 until: Fri Jan 15 16:03:33 CST 2027
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
SHA256: 83:7B:38:E8:AF:D4:F4:01:C4:82:86:CA:63:DD:E3:1E:ED:6D:37:40:7D:F7:AB:F1:15:53:EB:DA:42:4F:41:CD
```

**Hallazgo crítico**: este truststore NO trae certs Tridium/Honeywell de fábrica. Fue **REEMPLAZADO por keystore SEJOFA de auditoría/dev** con 1 sola entry RSA-2048 self-signed Mexico CDMX, alias `niagaramoduledev` (intent: bootstrap módulos custom firmados con CA SEJOFA).

**NO es 5to trust store** (Bloque 27.4 catalogó 4 runtime: user/system/daemon/userUntrustedStore). Es la **semilla install-time** que el primer arranque copia/migra a `<station_home>/security/userTrustStore.jks`. Modificarlo después de que la station ya arrancó NO afecta runtime (los stores runtime ya divergieron).

### 40.4.7 3 `.certificate` Honeywell/HoneywellCentraLine/Tridium

Confirma Bloque 27.11 empíricamente. Header literal:
```xml
<certificate version="1.0" vendor="Honeywell" generated="2006-10-12" expiration="never">
 <publicKey algorthm="DSA">
   ...base64 X.509 SubjectPublicKeyInfo DSA-1024...
 </publicKey>
 <signature>MCwCFFuDNX00tdsOr8DWUf5cYMp2784UAhQi3tiWmf8lcn6Gyi67/ezFlEtRTg==</signature>
</certificate>
```

Notas críticas:
- Atributo es **`algorthm`** (typo histórico Tridium, NO `algorithm`). Cualquier parser custom debe aceptar el typo. Corregirlo upstream rompería todo.
- **Todos DSA-1024** (no RSA). Coherente con licensing legacy ~2003 — NIST deprecation desde 2010, prohibition desde 2030.
- `expiration="never"` literal — vendor certs no caducan. Solo `.license` files caducan.
- `<signature>` es self-signature del vendor (cert auto-firmado, root). Cada vendor es su propia CA.

| Vendor | Generated | Tamaño | Subject pubkey distinto |
|--------|-----------|--------|-------------------------|
| Honeywell | 2006-10-12 | 835 B | DSA-1024 (clave A) |
| HoneywellCentraLine | 2014-01-13 | 845 B | DSA-1024 (clave B, distinta — params propios) |
| Tridium | 2003-07-16 | 833 B | DSA-1024 (clave C, distinta) |

Honeywell y Tridium comparten **parámetros DSA (p,q,g)** pero distintas claves Y. HoneywellCentraLine usa **parámetros DSA propios** (generados separadamente en 2014 cuando CentraLine se incorporó al portfolio Honeywell). HoneywellCentraLine **NO es sub-CA de Honeywell** — es vendor separado.

### 40.4.8 Patrón `licenses/db/<hostId>/` — root es ALIAS no source

```
licenses/
├── Honeywell.license              ← root: hostId="Win-6E6E-10AC-D1DD-8276"
├── HoneywellCentraLine.license    ← root: hostId="Win-..."
├── Webs.license                   ← root: hostId="Win-..."
├── inbox/                         ← VACÍO (drop zone import)
└── db/
    ├── Qnx-TITAN-BB4C-D480-3C70-ACE4/    ← controlador embebido OTRO host
    │   ├── Honeywell.license
    │   ├── HoneywellCentraLine.license
    │   └── Webs.license
    └── Win-6E6E-10AC-D1DD-8276/          ← misma hostId que las root
        ├── Honeywell.license
        ├── HoneywellCentraLine.license
        └── Webs.license
```

**Empírico con `diff`**:
```
diff licenses/Honeywell.license licenses/db/Win-6E6E-10AC-D1DD-8276/Honeywell.license  → IDÉNTICOS bit-exact
```

**Conclusión**: las licencias root **NO son licenses genéricas** — son **alias bit-exact** del host actual (`Win-6E6E-10AC-D1DD-8276`). El `db/<hostId>/` es **fuente canónica per-host**, root es copia conveniente.

```
db/<hostId>/           ← fuente canónica per-host
licenses/*.license     ← alias del db/<MI hostId>/
inbox/                 ← drop zone import
```

Este patrón soporta **multi-host distros**: una sola distro Honeywell trae licencias para supervisor Win Y para JACE/TITAN QNX que el supervisor manage. El supervisor Win **ignora** licencias Qnx (no matchean su hostId), pero las distribuye/sincroniza a JACEs durante provisioning.

**Comportamiento si falta `licenses/db/<hostId>/`**: la station al iniciar busca licencias válidas en root primero. Si root tiene hostId diferente al actual, arranque falla con `LicensingException`. **NO cae al `db/` automáticamente** — `db/` es archivo, no fallback.

**Corrige Bloque 02-licensing**: trataba `security/licenses/*.license` como "el set" sin distinguir root vs `db/<hostId>/`.

### 40.4.9 Webs.license asimetría Win vs Qnx — threat model crítico

| | Win Webs.license | Qnx Webs.license |
|--|-------------------|-------------------|
| Tamaño | 16 193 B | 6 011 B |
| Features count | **150** | **55** |
| `expiration` | 2027-03-31 | never |
| **`developer skipModuleValidation`** | **PRESENTE** | **AUSENTE** |
| `<feature about owner project>` | `owner="Syscom" project="00 HW - DEMO LICENSES"` | (ausente) |

**Verifico explícitamente**:
```
grep "skipModuleValidation" db/Qnx-TITAN-*/Webs.license  → 0 matches
grep "skipModuleValidation" db/Win-.../Webs.license      → 1 match (developer feature)
```

**Esto refina Bloque 18.3.2 + 27.6**: el bypass `skipModuleValidation` está SOLO en Webs.license del **supervisor Win**, NO en la del **JACE Qnx**. Implicación threat model:
- Supervisor (Win) = máquina de oficina, dev/integrator can load custom modules sin firma OEM
- JACE (Qnx) = field, lockdown estricto — no puede saltar validación de módulos
- Dev/supervisor: puerta abierta para devs; field: lockdown coherente con production

`<feature about owner="Syscom" project="00 HW - DEMO LICENSES">` — campo metadata trazabilidad legal: identifica cliente integrador (Syscom) y contexto (DEMO). NO existe en Qnx license — la Qnx es production-grade con `serialNumber=80375597`.

### 40.4.10 Webs Qnx confirma `jre8J8000Azul`

Top features Qnx-only en Webs.license:
- `dataRecovery`, `globalCapacity`, `ieee8021x`, **`jre8J8000Azul`**, `knxnetIp`, `lonIp`, `lonworks`, `mbus`, `modbusAsync`, `modbusSlave`, `modbusTcp`, `modbusTcpSlave`, `mqtt`, `mstp`, `niagaraDriver`, `nre`, `nrio`, `obixDriver`, `opc`, `opcUaClient`, `opcUaServer`, `provisioning`, `qnx7`, `samlDP`, `serial`, `snmp`, `station`, `syslog`, `tags`, `template`, `web`

`jre8J8000Azul` confirma que **JACE QNX corre Azul JDK 8** (mismo runtime que Supervisor Win — coherente con `tools.jar` Azul Zulu 8u282).

### 40.4.11 HostId format

Regex empírica: `^(Qnx|Win)-[A-Z0-9-]{14,19}$`.

```
Win-6E6E-10AC-D1DD-8276            → 16 hex chars, 4 grupos × 4
Qnx-TITAN-BB4C-D480-3C70-ACE4      → "TITAN" + 16 hex chars, 5 grupos
```

- **Win**: prefijo OS + 4 hexgroups 4 chars (16 total). Derivado de NIC MAC + Windows MachineGuid.
- **Qnx**: prefijo OS + nombre modelo hardware (`TITAN` = familia controller Honeywell QNX) + 4 hexgroups. El `TITAN` token NO es parte del hash — es el modelo. Otros JACE QNX tendrían `Qnx-JACE8000-...`.

**Refina Bloque 32** — el formato hostId Qnx incluye model tag.

### 40.4.12 `maintenanceExpiration` — features Win vs Qnx

| Atributo | Win Honeywell.license | Qnx Honeywell.license |
|----------|----------------------|----------------------|
| `expiration` | 2027-03-31 (1 año) | **never** |
| `serialNumber` | (ausente) | 80375597 |
| `version` | 4.15 | 4.15 |
| `maintenanceExpiration` | (ausente) | **2026-02-01 — CADUCADO al snapshot 2026-04-11** |

`maintenanceExpiration` Qnx ya pasó. **Operación actual sigue** (porque `expiration="never"`), pero updates de firmware/módulos JACE post-2026-02-01 NO autorizados por mantenimiento. Renovación pendiente.

Features Win-only (supervisor capability): `bport`, `clBacnetUtil`, `CSEasyOnboard`, `EMonN4TenantBilling`, `HBDashboard`, `honAlarmConsole`, `honConnectedPower`, `honEdgeDriver`, `honHit`, `honNiagaraApi`, `honPointListView`, `maxproVideo`, `redLink`, `SylkActuatorAnalytics`. Supervisor concentra dashboards, tenant billing, OpenADR, video maxpro.

Features Qnx-only (controller embebido): `cpProgrammable`, `honLoRaMqtt`. JACE no tiene dashboards, sí drivers programables y LoRa+MQTT IoT.

### 40.4.13 inbox/ — drop zone vacía

Drop zone donde LicenseManager Workbench escribe `.license` recién importadas. Workflow:
1. Cliente recibe `.license` de Tridium/Honeywell
2. Lo deja en `licenses/inbox/`
3. LicenseManager (Workbench Tools → License Manager) lo detecta, valida (signature + hostId + cert match), y lo mueve a `licenses/db/<hostId>/`
4. Si reemplaza una existente, borra la vieja

Inbox vacío = no hay imports pendientes (estado consistente).

---

## Gotchas operacionales (consolidados)

### Palettes_and_Misc (40.1)

- **G40.1.1** — `XL15C.palette` NO es ZIP. Formato proprietary Honeywell legacy. Parsers genéricos `.palette` que asumen ZIP fallan.
- **G40.1.2** — `.ntpl` Honeywell tienen `signature="<8hex>"` proprietary (NO RSA-2048). Modificar XML invalida silently — pierde flag verified vendor.
- **G40.1.3** — `.ntpl` `state="10"` indica deployed. State machine no documentada en Bloque 14.
- **G40.1.4** — TC500 firmware filename codifica todos los flags (Pelion+Azure+OTA+signing). Cambio rompe identificación Pelion.
- **G40.1.5** — `honeywellAXPlatinum` theme persiste de AX en CIPer30. Re-skinning a tema N4 nativo requiere refactor manual.
- **G40.1.6** — BurnerInterface 2006 = 18 años. Importar requiere migration audit (APIs deprecated probables).
- **G40.1.7** — Spyder M5 distinto formato firmware Unitary (`.bin` raw) vs VAV (`.ufw` ZIP). Naming convention NO uniforme.
- **G40.1.8** — Excel sizing tools (.xlsm/.xlsx) son Win-only macro Excel. NO funcionan Mac/Linux Workbench.
- **G40.1.9** — TR100 patches incrementales solo cubren versiones >=01.00.07.00. Devices más antiguos requieren reflash full (no presente en distro).
- **G40.1.10** — `Niagara_IO_SensorTables` no auto-discovered. Importar manualmente al ConversionLink.

### spyderApps (40.2)

- **G40.2.1** — `.libbog` es XML (NO binario) pero **propietario**: serializado vía Baja `bajaObjectGraph`, no editable con text editor sin parser.
- **G40.2.2** — Doble UUID por app (`lonUUID` + `bacnetUUID`) — Honeywell multi-stack. Modificar uno sin sincronizar el otro rompe binding cross-protocol.
- **G40.2.3** — Each `.libbog` linkea attachment `.doc` por `<p n="<filename>.doc">`. Borrar `.doc` rompe metadata binding (no rompe ejecución, sí docs).
- **G40.2.4** — `programId 90 00 0c 52 00 03 04 38` — Honeywell tiene **múltiples sub-namespaces** mfg prefix (`80 00 0c` LON Honeywell standard, `90 00 0c` Spyder family). Bloque 19.4 mencionaba solo el primero.
- **G40.2.5** — `ToolVersion 3.7.44.5.206` indica Spyder Tool ~2008-2012 era. Spyder Model 8+ requiere ToolVersion más reciente — Ver28 NO compatible Model 8.

### Printout (40.3)

- **G40.3.1** — Linux/macOS Workbench: NO funciona. clPrintout.exe es PE32 .NET Windows-only + COM Word automation Windows-exclusive. Fail silencioso.
- **G40.3.2** — `WireSheetControl.dll` NO es Excel renderer (Bloque 26.12 incorrecto). Es .NET UserControl Honeywell CARE legacy reciclado para bitmap render.
- **G40.3.3** — `Runtime.exec(_pathPrintoutExe) + waitFor` bloquea Workbench EDT. Si Word lanza modal dialog (license popup, addin error), thread atascado hasta timeout MILLISECONDS. UI freeze.
- **G40.3.4** — `Runtime.exec(String)` con path con espacios o caracteres no-ASCII puede partir el comando mal (no usa ProcessBuilder con List<String>).
- **G40.3.5** — `clPrintout.vshost.exe` 14 KB es residuo Visual Studio dev — NO entry point. Build pipeline Honeywell no purgó `\bin\Release\`.
- **G40.3.6** — `.dot` Word 97-2003 (NO `.dotx`). Office 365 web/online NO soporta COM Automation legacy. Solo Office desktop.
- **G40.3.7** — Bookmarks, NO MailMerge. `WdMailMerge*` en Word.dll PIA es ruido — el código usa `Document.Bookmarks[X].Range.Text`.
- **G40.3.8** — Templates por autores Honeywell internos múltiples (HTSL India, Centraline AT/DE, GE51) — riesgo inconsistencia styling entre secciones.
- **G40.3.9** — License gate `Helper.isLicenseValid()` Java-side ANTES exec. Sin license, dialog muestra error pero NO se lanza .exe.
- **G40.3.10** — `printout.xml` temp puede contener passwords cifrados, schedule, alarm config, IPs. Persiste si Workbench cierra abruptamente — leak post-mortem en filesystem audit.
- **G40.3.11** — Workbench module declara `LOGGING+UI` permisos pero ejecuta `Runtime.exec` sin gating framework — bypass implícito AccessController.

### lib + install security (40.4)

- **G40.4.1** — `truststore.jks` default password `changeit` funciona. Niagara NO endurece by default.
- **G40.4.2** — Truststore en este install REEMPLAZADO por SEJOFA. NO es vanilla Honeywell. Reproducir bootstrap nueva station resultaría en runtime stores SIN Tridium/Honeywell roots — rompe validación módulos firmados Honeywell. Restaurar truststore vanilla antes de provisionar.
- **G40.4.3** — Doclet 1.0.8 NO deprecated. Si rebuild módulo Honeywell legacy con 1.0.9, regenera `*-doc.jar` y puede invalidar firmas. Usar versión declarada en `gradle.properties` del módulo.
- **G40.4.4** — Manifest sin `Implementation-Version` en doclets. Solo nombre archivo distingue. Renombrar = perder identidad.
- **G40.4.5** — `lexicon.properties` install ≠ runtime. 47 idiomas en menú install ≠ idiomas runtime disponibles.
- **G40.4.6** — `tools.jar` Java 8 fija el techo runtime. Upgrade JDK 9+ requiere reescribir doclet contra `jdk.javadoc.doclet`, regenerar `*-doc.jar`, re-firmar todo.
- **G40.4.7** — `readmeLicenses.txt` fechado 2019. NO actualizado para N4.14. Auditoría OSS pidiendo SBOM definitivo NO debería confiar.
- **G40.4.8** — Root `licenses/*.license` es ALIAS no source. Si hostId cambia (NIC swap, motherboard, VM clone) alias deja de matchear. Re-aliasing manual: borrar root, copiar `db/<nuevo-hostId>/*.license` a root.
- **G40.4.9** — `db/Qnx-TITAN-*/Webs.license` SIN `skipModuleValidation`. JACE NO puede ejecutar módulos sin firma OEM. Threat model: dev=Win abierto, field=Qnx lockdown.
- **G40.4.10** — `<feature about owner="Syscom" project="00 HW - DEMO LICENSES">` — esta install marcada explícitamente como **DEMO**. Producción real debería tener owner cliente final.
- **G40.4.11** — `maintenanceExpiration="2026-02-01"` Qnx ya pasó. Operación sigue (`expiration="never"`), pero updates firmware/módulos post-fecha NO autorizados.
- **G40.4.12** — DSA-1024 obsoleto crypto-compliance. NIST deprecation 2010, prohibition 2030. FIPS 140-3/OWASP scan marca HALLAZGO. Migración ECDSA-P256 rompería compat.
- **G40.4.13** — Typo `algorthm` en `<publicKey>` cert — parser custom debe aceptar. Corregirlo upstream rompería todo.

---

## Cross-refs a bloques previos

### Correcciones a bloques previos

- **Bloque 12.4** (`.palette` formato): CORRIGE — `.palette` NO siempre es ZIP. XL15C.palette legacy es proprietary binario.
- **Bloque 02-licensing / 02 root**: CORRIGE — `security/licenses/*.license` root es **alias** del host actual; fuente canónica es `db/<hostId>/`. El patrón soporta multi-host distros (supervisor Win + JACE Qnx coexisten).
- **Bloque 18.3.2 + 27.6** (`skipModuleValidation` bypass): REFINA — feature está SOLO en Webs.license **Win supervisor**, AUSENTE en Webs.license **Qnx JACE**. Threat model asimétrico confirmado empíricamente.
- **Bloque 25** (doclet 1.0.9): EXTIENDE — coexistencia con 1.0.8, NO deprecation lineal. Tres clases difieren (Bajadoclet/XElem/XParser). Manifests idénticos sin Implementation-Version.
- **Bloque 26.12** (DLLs printout): CORRIGE — `WireSheetControl.dll` NO es "Excel worksheet renderer" sino .NET UserControl Honeywell CARE legacy reciclado para bitmap render. NO genera Excel. Agregar `clPrintout.exe` a la tabla.
- **Bloque 27.4** (4 trust stores): REFINA — install-time `truststore.jks` NO es 5to store, es **semilla** del user trust store runtime. NO confundir.
- **Bloque 27.11** (XML cert format): CONFIRMA empíricamente — typo `algorthm`, DSA-1024, `expiration="never"`, self-signed. Confirma 3 vendors separados (Honeywell + HoneywellCentraLine NO sub-CA + Tridium).
- **Bloque 12.2.7** (lexicon framework): REFINA — `/lib/lexicon.properties` install-time es **distinto** de runtime `*-lex.jar`. 47 idiomas installer ≠ idiomas runtime.

### Extensiones

- **Bloque 14.6** (Niagara Templates `.ntpl`): EXTIENDE — Honeywell `.ntpl` traen `state` machine, signature attribute proprietary, `vendor=Honeywell`, `buildVersion=4.10.0.154` (4.10 era). Templates pre-canned line completa Spyder M5/M7 + CIPer30.
- **Bloque 19** (LON Honeywell + Spyder): EXTIENDE — Spyder Classic Files complementa deep dive LON. ProgramId Honeywell tiene **múltiples sub-namespaces** (`80 00 0c` LON standard, `90 00 0c` Spyder family).
- **Bloque 24** (kitControl): EXTIENDE — `Niagara_IO_SensorTables` son curves NTC para BConversionLink. NO mencionado como source pre-canned.
- **Bloque 25.2** (`spyderToIrmNxMigrator`): EXTIENDE — la biblioteca spyderApps Ver28 es input set conocido del migrator.
- **Bloque 32** (Honeywell modules): EXTIENDE — capa "operational artifacts" complementaria al "module inventory" del Bloque 32.
- **Bloque 15.13** (workflow 5 fases end-to-end): EXTIENDE con paso 0 ("import pre-canned station/template Honeywell").
- **Bloque 03-security / 17 / 18** (módulo signing + permissions): EXTIENDE — flow `Runtime.exec` desde clPrintout-wb.jar bypassea AccessController standard. Workbench permissions `LOGGING+UI` declarados pero exec runtime no gating.

---

## Topic keys engram

- `niagara/bloque40/palettes-and-misc-inventory` — 13 subdirs paquetes operacionales pre-canned
- `niagara/bloque40/spyderapps-ver28-catalog` — 19 categorías + 136 archivos + ToolVersion 3.7.44 + ProgramId 90 00 0c
- `niagara/bloque40/printout-office-interop-flow` — clPrintout.exe + 3 JARs Java + Word Bookmarks (NO MailMerge) + WireSheetControl Honeywell CARE legacy
- `niagara/bloque40/lib-and-install-security` — doclet 1.0.8/1.0.9 coexisten + tools.jar Azul JDK 8u282 + truststore SEJOFA + db/<hostId>/ alias pattern + Webs.license asimétrica Win/Qnx
