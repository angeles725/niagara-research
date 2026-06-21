# Bloque 92 — `lonhoneywellAXWizards`: suite de wizards Workbench para dispositivos LON Honeywell Excel 10 / XL15C / T7350, sobre el driver `lonworks` estándar de Tridium, deofuscado

> Investigación empírica del módulo OEM Honeywell **`lonhoneywellAXWizards`** (304 java, Workbench-only): la **capa de configuración por wizards** para la familia de controladores LON Honeywell **Excel 10** (XL10) + el SBC **XL15C** + el termostato **T7350** + VFDs CX/NX. Aterriza el "qué configura los dispositivos LON Honeywell" que quedaba abierto sobre el driver Spyder.
>
> 1 módulo (`lonhoneywellAXWizards`, runtimeProfile `wb`). Paquetes: `com.honeywell.lonHoneywellXl15c` (datatypes 51 + enums 25), `com.honeywell.londevices.{axwizard.ui, xl10controller}`, `com.honeywell.framework` (wizard + dragdrop UI).
>
> Fuentes: `organized/lonhoneywellAXWizards/lonhoneywellAXWizards/.../vineflower/com/honeywell/...` + `META-INF/module.xml`.
> Método: 1 sub-agente Explore + **verificación directa** de la cadena `extends` del árbol de dispositivos y de vistas, del base `BLonData` de los datatypes, y de la ausencia de dependencia a `honeywellLonSpyder` (grep=0). `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (Program IDs, `.lnml`, datatypes individuales, hallazgos menores) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 91]. **CONTRASTA con [Bloque 77]** (`honeywellLonSpyder` = driver LON propio Honeywell): este módulo **NO usa Spyder** — se apoya 100% en el driver `lonworks` estándar de Tridium ([Bloque 7]). Conecta [Bloque 7] (LON framework), [Bloque 88] (cadena de campo Honeywell).

---

## 92.1 — Qué es: wizards de Workbench, no driver `[CERT]` + `[CERT-a]`

Módulo **exclusivo de Workbench** (`runtimeProfile='wb'` en `module.xml`, descripción literal *"Module for wizard based configuration of Honeywell devices"`) `[CERT-a]`. **No comunica con la red LON**: configura dispositivos ya descubiertos por el driver `lonworks` de Tridium. Lo verifica la dependencia masiva — **151 ficheros importan `javax.baja.lonworks.*`** `[CERT]` y **cero referencias a `honeywellLonSpyder`** `[CERT]`.

Lo que el módulo aporta `[CERT-a]`: lectura/escritura de Network Variables (NV/NCI) sobre el driver Tridium, carga de ficheros de aplicación `.lnml` (XIF en XML) en el dispositivo, y paneles de wiring que mapean I/O físico→NV. Trae **12 `.lnml` pre-compilados** (uno por modelo) — no genera XIF nuevos.

---

## 92.2 — El árbol de dispositivos `[CERT]`

Raíz verificada sobre el driver Tridium:
```
javax.baja.lonworks.BDynamicDevice                                   (Tridium lonworks)
 └─ BHoneywellDevice extends BDynamicDevice            (:13)         [CERT]
     ├─ BHoneywellInUseDevice extends BHoneywellDevice (abstract, :27)[CERT]
     │   ├─ BExcel10VAV extends BHoneywellInUseDevice  (:25)         [CERT]  (VAV)
     │   ├─ BExcel10CVAHU / BExcel10UnitVent / BExcel10CHC1          [CERT-a]
     │   ├─ BExcel10FCU2 / BExcel10HYD / BExcel10RIO                 [CERT-a]
     │   └─ BT7350  (termostato)                                     [CERT-a]
     ├─ BXl15c extends BHoneywellDevice  (:47)                       [CERT]  (SBC, NVs propietarios)
     ├─ BCXVariableFrequencyDrive / BNXVariableFrequencyConverter    [CERT-a]  (VFDs)
```

**11 Program IDs LON registrados** (`<defs>` en `module.xml`) `[CERT-a]`: Excel 10 VAV/CVAHU/UnitVent/CHC1/FCU2/HYD/RIO, T7350, CX/NX VFD, XL15C. Las aplicaciones HVAC (VAV, CVAHU, UV) son **plantillas precargadas** (NVs, I/O, PID, setpoints, economizador). El descubrimiento usa el job LON de Tridium (`com.tridium.lonworks.netmgmt.BLonNetmgmtJob` vía `BXl15cLearnJob`) `[CERT-a]`.

---

## 92.3 — El modelo de datos XL15C: `BLonData` + `BFrozenEnum` `[CERT]`

Los **51 datatypes** del XL15C extienden `javax.baja.lonworks.londata.BLonData` `[CERT]` (verificado `BAiConfig extends BLonData`, `:14`) — estructuras binarias LON mapeadas a NVs del SBC. No son SNVT LonMark estándar sino **tipos propietarios Honeywell** transportados como raw bytes en NVs genéricos `[INFER]`.

Familias de datatype `[CERT-a]`: config de I/O (`BAiConfig/BAoConfig/BDiConfig/BDoConfig`), control (`BSS*` start/stop, `BLL*` lead/lag, `BFt*` floating loop, `BFlexSetPoints`), alarmas (`BAlarm*`), lógica interna (`BMath*/BAndConfig/BOrConfig`), identificación (`BHwName/BPgmId`), y **`BDlcShed`** (demand limiting / load shedding). Los **25 enums** son `extends BFrozenEnum` `[CERT-a]` (`BAiKindEnum`, `BEngUnitsEnum`, `BLeadLagEnum`, `BMathOpEnum`…).

---

## 92.4 — El framework de wizard + UI `[CERT]`

**Wizard `[CERT]`**: `BWizardFrame extends BFrame` (abstract, `framework/…:29`) → `BTabbedWizardFrame extends BWizardFrame` (`:26`). Toma un `IConfigurationStepContainer` que devuelve un `BStep[]` (`extends BComponent`) — wizard multi-paso con tabs `[CERT-a]`.

**Vistas `[CERT]`**: `BAXView extends BWbComponentView` (`londevices/utilities/…:18`) → `BVavConfigView` / `BCvahuConfigView` / `BT7350ConfigView` / `BHYDConfigView` `[CERT-a]`. (`BUVConfigView` cuelga directo de `BWbComponentView` e implementa `IConfigurationStepContainer`.)

**dragdrop `[CERT-a]`**: NO es un editor de binding LON inter-dispositivo. Es una librería de widgets Baja con DnD (`BAXComponentBase extends BTransferWidget` → `BPaneBase` → text fields/labels/dropdowns "Ex"). Los `B*WiringDiagram extends BPaneBase` son **diagramas estáticos** del cableado físico sugerido por modelo, no editores activos (`dragOver/drop` sobrescritos devuelven null). `BHonLonNetExt extends BComponent implements BIMixIn` se registra `<on type='lonworks:LonNetwork'>` para añadir propiedades Honeywell de red.

---

## 92.5 — Relación con el transporte LON `[CERT]`

**Dependencia exclusiva del driver `lonworks` de Tridium** (`lonworks-rt`/`lonworks-wb`), confirmada por los 151 imports `javax.baja.lonworks.*` y las referencias a `com.tridium.lonworks.{util.NmUtil, netmgmt.BLonNetmgmtJob, ui.BDynamicDeviceMenuAgent}` `[CERT-a]`. Los `.lnml` bundled son XIF pre-generados por Honeywell, cargados por `LonXMLReader`/`XLonInterfaceFile` de Tridium. La comunicación LON real (mensajes, commissioning, addressing) la hace **íntegramente el driver Tridium** `[INFER]`.

**Diferencia con [Bloque 77]**: `honeywellLonSpyder` es un driver LON propio (`BLonSpyder extends BDynamicDevice`); este módulo es sólo UI de configuración sobre el driver genérico. Son dos enfoques distintos de Honeywell para LON: Spyder (driver propietario) vs Excel 10/XL15C (wizards sobre `lonworks`).

---

## 92.6 — Seguridad `[CERT-a]`

**Sin credenciales hardcodeadas** (scan de `password`/`secret`/`apikey`/claves = 0) `[CERT-a]`. Hallazgos menores: `System.out.println` de estado interno en producción; `workbenchLicenseType`/`stationLicenseType` como String legible vía API Baja; `FeatureNotLicensedException("AXWizards…")`/`("Action Binding")` exponen nombres de feature (fingerprinting); los 11 Program IDs en claro en `module.xml`; `buildHost='azu-hce-vbf-w20'` revela una VM de build Honeywell (OSINT). Ninguno crítico.

---

## 92.7 — Conexiones

- **CONTRASTA con [Bloque 77]** (`honeywellLonSpyder`): aquí Honeywell usa el driver `lonworks` estándar de Tridium + wizards, no un driver propio.
- **[Bloque 7]** (LON framework Tridium): es la base real de transporte.
- **[Bloque 88]** (cadena de campo Honeywell): los Excel 10 / T7350 son la capa de controlador de terminal en el lado LON (análogo a lo que Sylk/BEATS son en el lado BACnet).
