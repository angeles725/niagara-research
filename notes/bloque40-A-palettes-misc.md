# Bloque 40 — Palettes_and_Misc inventory (empírico)

## Resumen ejecutivo

- **13 subdirectorios** con paquetes pre-armados Honeywell para deployments reales en sites HVAC.
- **palettes/**: 3 palettes Tridium-format (`.palette` = ZIP con `file.xml` BOG-XML) — `HoneywellSubmeter` (2009, 144KB XML), `hvfd` (HVFD = Variable Frequency Drive, 2010, 124KB XML), **`XL15C` (586 KB) NO es ZIP** — formato custom binario del Honeywell Excel 15C controller.
- **CIPer Model 30 Stations**: 2 station templates pre-canned como `.zip` (config.bog + shared/charts/nav/px theme `honeywellAXPlatinum`) — VAV TR42 Modulating Reheat (Ana) + Staged Reheat (Stg). Datados enero 2020.
- **Spyder Model 5/7**: firmware (`.bin` Unitary + `.ufw` VAV — ZIP) + templates `.ntpl` (Niagara Template XML signed, `vendor="Honeywell" buildVersion="4.10.0.154"` con signature `39d205e7`). VAV TR23/TR42 Ana/Stg.
- **TC300/TC500/TR100**: firmware thermostat controllers + wall module — `.bin`, `.ufw` (ZIP), `.fw` (ZIP patches incrementales). TC500 prod-signed (Pelion+Azure cloud).
- **Niagara_IO_SensorTables**: 4 XML resistance-curve tables (NTC 10K-Type2/Type3 + 20K) para calibración termistores — formato `<conversion><table><point src result/></table></conversion>`.
- **BurnerInterface.zip** (76 KB): UI legacy Honeywell 7800-series burner controller (mayo 2006) — config.bog + s7810main.px + 7800.gif. Anchor histórico AX-era preservado.
- **Optimizer Unitary**: firmware NC_Unitary_V2.1.1.40.ufw (Network Controller Unitary).
- **BACnetFFT_N4_Reflash**: tool reflash + firmware HW_TB3026B (hddcv3b23vldff-firmware.bin).

---

## 1. `palettes/` — palette files importables a Workbench

| Archivo | Tamaño | Formato | Datado | Función |
|---------|--------|---------|--------|---------|
| `HoneywellSubmeter.palette` | 12.5 KB ZIP → 144 KB `file.xml` | Tridium standard ZIP+XML BOG | 2009-04-24 | Submeters (energía/agua/gas) |
| `hvfd.palette` | 12.6 KB ZIP → 124 KB `file.xml` | Tridium standard | 2010-02-22 | Honeywell Variable Frequency Drive |
| `XL15C.palette` | **586 KB — NO ZIP** | Formato proprietary custom | n/a | Excel 15C controller (legacy) |

### XL15C anomaly

`unzip -l XL15C.palette` falla con "End-of-central-directory signature not found". El header NO es ZIP standard — es un formato proprietary Honeywell, posiblemente BOG-binario serializado o un wrapper custom. Esto **CONTRADICE el supuesto del Bloque 12.4 de que `.palette` siempre es ZIP+file.xml**. Hay variantes legacy Honeywell que NO siguen la convención.

### Estructura típica `.palette` (ZIP):
- `file.xml` único — BOG XML con typeSpecs Honeywell (`bacnetCore:Device`, custom Honeywell components con vendor metadata)

---

## 2. `CIPer Model 30/` — IP controller + sizing tools

```
CIPer Model 30/
├── CIPer30 Power Estimator_rev6.xlsm        # Excel macro tool
├── Cable voltage drop calculator.xlsx        # Sizing Excel
├── Sylk Bus Limits Calculator v1.8.xlsx      # Sylk wire limits
└── CIPer Model 30 Stations/
    ├── CIPer30_VAV_TR42_Modulating_Reheat/
    │   ├── Ciper30_VAV_TR42_Ana_001_R101.zip  # 50KB station bundle
    │   └── CIPer30 VAV TR42 Ana_rev1.01.doc   # Documentation
    └── CIPer30_VAV_TR42_Staged_Reheat/
        ├── CIPer30_VAV_TR42_Stg_001_R101.zip
        └── CIPer30 VAV TR42 Staged Rev 1.01.doc
```

### Station ZIP contents (`Ciper30_VAV_TR42_Ana_001_R101.zip`)
- `config.bog` (50 KB) — pre-built station config
- `shared/charts/Monitor.chart` (4 KB)
- `shared/nav/` — `balancer.nav`, `default.nav`
- `shared/px/honeywellAXPlatinum/images/coils/` — pre-rendered PX assets (filterV_1.png 8 KB)
- `honeywellAXPlatinum` theme = AX-era platinum package, **mantenido en N4** para backward-compat visual.

CIPer Model 30 = controlador IP-based VAV de Honeywell. Las stations pre-armadas son drop-in para integradores: importás ZIP en Workbench, conectás al device físico, ajustás setpoints, deploy.

---

## 3. `Spyder Model 5/` y `Spyder Model 7/`

### Spyder Model 5
```
Spyder Model 5/
├── Sylk Bus Limits Calculator v1.8.xlsx
├── firmware/
│   ├── Spyder Model 5 Unitary v2.0.4.25/
│   │   └── BEATS_MSTP_Unitary_V2.0.4.25.bin    # raw binary firmware
│   └── Spyder Model 5 VAV v2.0.4.25/
│       └── BEATS_MSTP_VAV_V2.0.4.25.ufw         # ZIP firmware package
└── templates/
    └── Spyder Model 5 VAV Templates 108/
        ├── VAV_TR42__Ana108.ntpl                # Niagara Template XML signed
        ├── VAV_TR42__Stg108.ntpl
        ├── VAV_TR23_Ana108.ntpl
        ├── VAV_TR23_Stg108.ntpl
        └── *.doc (4 documentation files)
```

### Spyder Model 7
- Firmware: `Spyder Model 7 IP & MSTP VAV v2.0.8.32/NC_VAVA_V2.0.8.32.ufw`
- Template: `Spyder 7 VAV application Rev 01_117/FlexVAV_S7_Rev001_117.ntpl` + PDF doc

### `.ntpl` = Niagara Template (Bloque 14 confirmation)

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
    ...
  </settings>
</template>
```

**CONFIRMA Bloque 14.6** — `.ntpl` es XML, Honeywell vendor, signed `signature` attribute, `buildVersion="4.10.0.154"` exacto. Bloque 14.6.3 advirtió "NO auto-propaga" — los `.ntpl` Honeywell traen `state="10"` que indica deployed.

### Firmware formats
- `.bin` (Unitary) = raw binary, `file` reporta "data" sin signature reconocible
- `.ufw` (VAV) = ZIP archive deflate
- `.fw` (TR100 patches) = ZIP archive

`BEATS_MSTP_*` prefix = Honeywell internal codename para Spyder firmware family BACnet MS/TP.

---

## 4. `TC300/`, `TC500/`, `TR100/` — thermostat + wall module firmware

### TC300
- `firmware/package_2.0.1.8_TC300B-G_withnewheader.en.bin` — TC300B-G v2.0.1.8 (binary "data")
- "with new header" sufijo indica formato actualizado con metadata header

### TC500 (IoT thermostat con cloud)
- `firmware/TSTAT_TC500A_v1.1.10.0_OTA_Prod_Signing_PelionProd_AzureProd_No_log_with_application_v2.8.8.8_withNewHeader.bin`
- Sufijos clave:
  - `OTA` = Over-The-Air update support
  - `Prod_Signing` = production signing chain
  - `PelionProd` = ARM Pelion (IoT device management) prod environment
  - `AzureProd` = Azure IoT Hub prod environment
  - `No_log` = logging deshabilitado en build
  - `application_v2.8.8.8` = app firmware bundled
- TC500 es un **IoT thermostat con doble conectividad cloud**: ARM Pelion + Azure IoT Hub.

### TR100 (Wall Module)
- `TR100_FU_patch_01.00.07.00_to_01.02.00.00.fw` (ZIP)
- `TR100_FU_patch_01.01.00.00_to_01.02.00.00.fw` (ZIP)
- "FU patch" = Firmware Upgrade patch incremental (delta entre versiones)
- TR100 = wall-mounted room sensor con setpoint + display

---

## 5. `Spyder Classic Files/`
- `BiasCalculator_v4.xlsm`
- `SpyderType3Export/` (subdir)
- `Sylk Bus Limits Calculator v1.8.xlsx`

Spyder Classic = generación legacy (LON-based). El Bloque 19 cubrió LON deep + ascLon-rt — Spyder Classic es el equipment line correspondiente.

---

## 6. `Niagara_IO_SensorTables/` — sensor calibration curves

4 XMLs:
- `10ktype2custresistance.xml`
- `10ktype2to10ktype3f.xml` (cross-conversion 10K Type 2 → 10K Type 3)
- `10ktype3custresistance.xml`
- `20K_5degsteps_25F_to_240F.xml`

### Formato (verificado)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<conversion>
  <description>10KType2CustResistance</description>
  <table>
    <point src="121939.0" result="-11.0"/>  <!-- ohms → °F -->
    <point src="88090.0" result="-1.0"/>
    <point src="60592.0" result="11.0"/>
    ...
    <point src="10000.0" result="77.0"/>   <!-- 10K @ 77°F = nominal point -->
    ...
  </table>
</conversion>
```

**Uso**: estos XML se importan a kitControl `BConversionLink` o equivalent thermistor block. Bloque 6.2 (Control Engine) y Bloque 24 (kitControl palette) cubren los blocks pero NO mencionan estos archivos pre-canned como source de curves NTC. **EXTIENDE Bloque 24.7** (Status conversion blocks).

NTC 10K Type 2/Type 3 son los thermistors estándar de la industria HVAC norteamericana — esto es plug-and-play para sensor mapping.

---

## 7. `BurnerInterface.zip` (76 KB, mayo 2006)

```
BurnerInterface/
├── config.bog (68 KB)
├── px/
│   ├── images/7800.gif (12 KB)
│   └── s7810main.px (72 KB)
└── readme.txt (384 bytes)
```

**Honeywell 7800 Series** burner controller — UI/control panel UI legacy. Datado **mayo 2006** = AX-era (pre-N4). Sobrevive en distro N4.14 como anchor histórico para deploys que mantienen 7800 controllers físicos.

`s7810main.px` (72 KB) es un PX sustancial — UI completa para operar el burner. La conexión driver-side es probablemente Modbus o serial directo (no presente en el zip — se asume station con driver instalado por separado).

---

## 8. `Optimizer Unitary/firmware/Optimizer Unitary v2.1.1.40/`
- `NC_Unitary_V2.1.1.40.ufw`
- "NC" = Network Controller Unitary (rooftop unit / packaged equipment)

Naming "Optimizer Unitary" coincide con el branding del producto Honeywell **Optimizer Supervisor** — son equipment line del mismo producto.

---

## 9. `BACnetFFT_N4_Reflash/`
- `BACnet FFT N4 Firmware Download Tool SRB.pdf` (Service Release Bulletin)
- `HW_TB3026B_FW/hddcv3b23vldff-firmware.bin` (binary data)

**FFT** (en este contexto Honeywell) = Field Field Tool / Forced Function Test, herramienta para reflash de field controllers vía BACnet. **TB3026B** es modelo de field device. El proceso: instalar tool → conectar BACnet → reflash con `hddcv3b23vldff-firmware.bin`.

---

## 10. Workflow operacional — cómo se usan estos paquetes

**Caso típico — deploy nuevo VAV TR42 con CIPer30:**

1. Comprar hardware Honeywell CIPer Model 30 + TR42 wall module + sensors
2. En Workbench: importar `Ciper30_VAV_TR42_Ana_001_R101.zip` como station nueva (NO restore — es template-as-station)
3. Si es Spyder en lugar de CIPer: importar `VAV_TR42__Ana108.ntpl` como Niagara Template (Bloque 14.6) → instanciar bajo el device
4. Sensor calibration: importar `Niagara_IO_SensorTables/10ktype3custresistance.xml` al BConversionLink del thermistor
5. Sizing: usar `Sylk Bus Limits Calculator v1.8.xlsx` para verificar que la cantidad de devices Sylk en el bus no excede limits
6. Power sizing: `CIPer30 Power Estimator_rev6.xlsm` para budgeting
7. Cable run: `Cable voltage drop calculator.xlsx` para verificar drop
8. Field commissioning: TR100 wall module se flasea con el patch `.fw` correspondiente vía Honeywell tool
9. Si Centraline burner 7800 está en el sitio: importar `BurnerInterface.zip` como anchor UI

Esto **EXTIENDE Bloque 15.13** (workflow 5 fases end-to-end) — el workflow real Honeywell incluye estos paquetes como starting point, NO empieza de zero.

---

## Gotchas operacionales

- **G40A.1 — XL15C.palette NO es ZIP**: el `unzip` falla con "End-of-central-directory signature not found". Formato proprietary Honeywell custom (legacy XL15C controller). **CONTRADICE supuesto Bloque 12.4** de que `.palette` siempre es ZIP.
- **G40A.2 — `.ntpl` Honeywell signed**: signature attribute `39d205e7` en VAV_TR42_Ana108. **Modificar el XML invalida la firma silently** — el template puede importar pero perder el flag "verified vendor". (Bloque 14.6 dijo signed pero no especificó comportamiento).
- **G40A.3 — `state="10"` en `.ntpl` Honeywell**: indica template deployed/active. State machine de templates Honeywell no documentada en Bloque 14.
- **G40A.4 — TC500 firmware filename como spec**: el filename literalmente codifica todos los flags (`OTA_Prod_Signing_PelionProd_AzureProd_No_log_with_application_v2.8.8.8_withNewHeader`). Cambio de filename rompe identificación del Pelion device manager.
- **G40A.5 — `honeywellAXPlatinum` theme en CIPer30 stations**: persiste de AX. Workbench N4.14 lo soporta por backward-compat pero los assets son AX-era. Re-skinning a tema N4 nativo requiere refactor manual del station.
- **G40A.6 — BurnerInterface 2006**: 18 años de antigüedad (al 2026). Puede tener referencias a APIs deprecated, drivers que ya no existen, ORDs huérfanos. Importar como base require migration audit.
- **G40A.7 — Spyder Model 5 Unitary `.bin` vs VAV `.ufw`**: distinto formato para mismo Spyder Model 5 según application. **Naming convention NO uniforme** — integrators must know which format to use per app.
- **G40A.8 — Excel sizing tools (xlsx/xlsm)**: tools macro Excel — Win-only, NO funciona Mac/Linux Workbench. Calcular Sylk bus limits manualmente requiere comprender formula propietary Honeywell.
- **G40A.9 — TR100 patches incrementales**: ambos patches van a 01.02.00.00 desde 01.00.07.00 o 01.01.00.00 — **NO existe patch directo desde versiones más antiguas**. Devices con firmware <01.00.07.00 requieren reflash full (no presente en distro).
- **G40A.10 — `Niagara_IO_SensorTables` no auto-discovered**: estos XML deben importarse manualmente al ConversionLink. NO hay registry/scan automático que detecte el folder.

---

## Cross-refs a bloques 1-39

- **Bloque 12.4 (`.palette` formato)**: CORRIGE — `.palette` NO siempre es ZIP. XL15C.palette es proprietary binario.
- **Bloque 14.6 (Niagara Templates `.ntpl`)**: EXTIENDE — Honeywell `.ntpl` traen `state` machine, signature attribute, `vendor=Honeywell`, `buildVersion=4.10.0.154` (4.10 era — anteriores a 4.14). Templates pre-canned line completa Spyder M5/M7/CIPer30.
- **Bloque 19 (LON Honeywell + Spyder)**: EXTIENDE — Spyder Classic Files complementa el deep dive LON. ProgramId 8-byte (`80 00 0c ...`) Honeywell aplicable a Spyder Classic.
- **Bloque 24 (kitControl + control)**: EXTIENDE — `Niagara_IO_SensorTables` son curves NTC para BConversionLink/conversion blocks. NO mencionado como source pre-canned.
- **Bloque 25 (Migration)**: cross-ref con `spyderToIrmNxMigrator-wb.jar` mencionado en Bloque 25.4 — los Spyder Classic files son inputs al migrator.
- **Bloque 32 (Honeywell modules)**: EXTIENDE — esta es la capa "operational artifacts" complementaria al "module inventory" del Bloque 32.
- **Bloque 15 (Workbench editing)**: EXTIENDE workflow 15.13 con paso 0 ("import pre-canned station/template Honeywell" como starting point real).
