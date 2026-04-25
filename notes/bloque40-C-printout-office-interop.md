# Bloque 40 — Printout Office Interop flow

Fuentes empíricas (Linux, sin decompilers .NET disponibles — análisis vía `file`, `strings -n 6/8`, `unzip -p`, `grep` sobre PE32 .NET assemblies y `*.class`):
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/printout/` (binarios + templates)
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/clPrintout-{rt,wb,doc}.jar`

PDB embedded path empírico: `C:\Users\h446496\Downloads\NiagaraDotNetReport\NiagaraDotNetReport\clPrintout\obj\Release\clPrintout.pdb` — confirma proyecto Honeywell **NiagaraDotNetReport** (build máquina h446496).

---

## Resumen ejecutivo

`clPrintout.exe` NO es invocado por NRE ni por el daemon: es invocado **desde Workbench (proceso `wb_w.exe`)** vía `Runtime.getRuntime().exec(String)` en `BPrintoutDialog.doPrintDocumentation()`. El módulo `clPrintout-wb.jar` (puro Java/baja) recorre la station, vuelca componentes a `!printout/printout.xml` (vía `Exporter` con `XWriter`), y luego shell-exec del .exe .NET que abre Word (COM Automation) usando `WordTemplate.dot` o `*SwivelLabel*.dot` como base, hace `bookmark replace` (NO MailMerge — usa `Document.Bookmarks` API), y produce `.docx`/PDF.

Tres outputs distintos:
1. **Project documentation** (`WordTemplate.dot`) — listado completo de Services/Drivers/Datapoints/Wiresheets en Word.
2. **Swivel labels** (`SwivelLabel{H,V,IrmH,IrmV,MioH,MioV}Template.dot` + `MasterSwivelLabelTemplate.dot`) — etiquetas físicas adhesivas para placas frontales de controladores Centraline IRM/MIO.
3. **Online printout** (`BOnlinePrintoutDialog`) — variante que imprime directo desde estación corriendo (sin volcar BOG offline primero).

Hipótesis ORIGINAL del usuario (Workbench → XML → exec clPrintout.exe → Word + MailMerge → docx) **CONFIRMADA salvo en un punto**: NO usa MailMerge sino **Bookmarks** (`Document.Bookmarks["AI_Bookmark"].Range.Text = ...`). Las clases `WdMailMergeXxx` están presentes en `Word.dll` solo porque es el Word PIA completo, no porque el código las use.

---

## Inventario binarios (.NET vs nativo + función)

| Archivo | Tipo | Función verificada |
|---|---|---|
| `clPrintout.exe` 954 KB | PE32 GUI .NET (Mono/.Net assembly) | Entry point. WinForms (`MainForm`, `SetOptionsDlg`). Refs `MainForm_Load`, `SaveFileDialog`, `Word.Application`, `OpenMicrosoftWord`, `IsWordInstalled`, `OpenRuntime`, `WordAutomation` |
| `clPrintout.vshost.exe` 14 KB | PE32 GUI .NET | **Visual Studio debugging hosting process**. NO se usa en producción — es residuo del build. Manifest dice `MyApplication.app asInvoker uiAccess=false`. Misleading: NO es el real exec |
| `clPrintout.vshost.exe.manifest` 490 B | XML asm v1 | Manifest VS hosting — sólo trustInfo asInvoker |
| `Word.dll` 484 KB | PE32 DLL .NET | **Microsoft Word PIA** (`Microsoft.Office.Interop.Word`). Tiene TODA la API (WdMailMerge*, Bookmarks, Tables, Range, Documents, _Document). Es **redistribuible**, no Honeywell custom |
| `Office.dll` 152 KB | PE32 DLL .NET | **Microsoft Office Core PIA** (`Microsoft.Office.Core` — Mso* enums: MsoLineDashStyle, MsoFillType, MsoTriState, IAccessible, _IMsoDispObj, MsoBalloonType etc.). Redistribuible Microsoft |
| `Interop.Shell32.dll` 48 KB | PE32 DLL .NET | **Shell32 COM interop** (Windows shell — file system shortcuts, special folders) |
| `WireSheetControl.dll` 344 KB | PE32 DLL .NET | **NO es PIA Microsoft**. Es UserControl Honeywell con UI completa de wiresheet edition: Autoroute, Math Editor, Macros, Mirror, Pan&Zoom, XFM (Honeywell CARE format), CARE Datapoints, Constants 0/1, Multi-State I/O, IRM terminals. Compartido del producto **Honeywell CARE**. Aquí se usa solo para **renderizar wiresheet en bitmap embebido en Word** (no como editor) |

**.NET runtime usado**: System.Drawing, System.Windows.Forms, System.Xml, System.Configuration, System.Runtime.InteropServices, System.Diagnostics, System.CodeDom.Compiler, Microsoft.Win32 (Registry). Es **WinForms .NET Framework** (no WPF — aunque hay refs a System.Windows.Media).

---

## Flow end-to-end (Workbench → Word docx)

```
[wb_w.exe] BPrintoutDialog (Java baja)
   | user click "Print Documentation"
   v
doPrintDocumentation(BWidgetEvent):
   1. loadProjects() ← !printout/projects.xml
   2. para cada station seleccionada:
        Helper.isStationRunning() check
   3. delete + createNewFile(_pathExportFile)   ← !printout/printout.xml
   4. Exporter.exportProject(exportFileOrd, projectName, stationNames)
      ↓
      [Exporter.java] usa javax.baja.xml.XWriter:
        <component> <property> <action> <topic> <knob>
        + sourceComponentName/sourceSlotName/sourceOrd
        + targetComponentName/targetSlotName/targetOrd
        + recorrida tipos: BDeviceNetwork, BDeviceFolder, BDevice,
          BLocalBacnetDevice, BPointFolder, BPointDeviceExt, BControlPoint
        + PxView → PDF via com.tridium.pdf.BPxViewToPdf
          (defaultViewWidth/Height, output a !printout/px/)
   5. Runtime.getRuntime().exec(_pathPrintoutExe)
      ↓ shell-exec una sola string (NO ProcessBuilder — NO args list)
      ↓ waitFor(timeout, MILLISECONDS)
   6. clPrintout.exe arranca → MainForm
      ↓ lee printout.xml (LoadXmlFile, XmlDocument)
      ↓ lee DocumentTemplate.xml (HTML→Word convert config)
      ↓ lee DefaultFilter.xml (qué printear)
      ↓ Registry.ClassesRoot → check Word.Application CLSID
      ↓ OpenMicrosoftWord() → COM Automation Word.Application
      ↓ Documents.Add(WordTemplate.dot)
      ↓ ITERA xml → para cada componente:
         Document.Bookmarks["AI_Bookmark" / "BI_Bookmark" /
                            "AO_Bookmark" / "BO_Bookmark" /
                            "DP_Bookmark" / "IOM_Bookmark" /
                            "NUM_Bookmark"].Range.Text = value
      ↓ Tables.Add(...) para datapoints listados
      ↓ InsertFile(StandardSegmentTemplate.htm) si helper segments
      ↓ AddPdfFile(per-wiresheet PDF de !printout/px/)
      ↓ SaveAsXmlFile / SaveFileDialog → user picks .docx
   7. Java: process.waitFor() — Workbench bloquea
```

**Mecanismo IPC concreto**: **temp file XML** (no STDIN, no socket). El XML está en `!printout/printout.xml` (resuelto por BFileSystem como `<station>/printout/` o `<wb_home>/printout/`). El .exe lo lee del disco por path absoluto pasado vía Java `pathToLocalFile(_pathExportFile).getAbsolutePath()`.

**Concurrencia**: `Runtime.exec(String)` con `waitFor` bloquea el thread Workbench. Si Word cuelga (modal dialog Office, license popup, addin error), Workbench thread atascado hasta el timeout MILLISECONDS — **gotcha operacional confirmado** (`process.waitFor(N, TimeUnit.MILLISECONDS)`). Ver gotchas §G3.

---

## Templates .dot — análisis

Todos los `.dot` son **Composite Document File V2** (formato OLE binario MS Word 97-2003). NO son `.dotx` (XML moderno). Esto significa que abren con **Word 2003 minimum**, ideal Word 2007+.

| Template | Autor | Última edición | Función |
|---|---|---|---|
| `WordTemplate.dot` | E308935 / HTSL | 2008-08-22 | Master template **project documentation** completa (services/drivers/datapoints/wiresheets) |
| `MasterSwivelLabelTemplate.dot` | Tolstonog-Riedel L. (GE51) | 2016-06-09 | Página master para concatenar tiles de swivel labels |
| `SwivelLabelHTemplate.dot` | Tolstonog-Riedel L. | 2019-03-19 | Swivel label **horizontal** (Centraline standard) |
| `SwivelLabelVTemplate.dot` | HTSL | 2010-06-03 | Swivel label **vertical** (legacy `vfv.dot` template parent) |
| `SwivelLabelIrmHTemplate.dot` | Schroeffel M. | 2019-01-08 | Swivel label H para **Centraline IRM** (Intelligent Room Manager) controllers |
| `SwivelLabelIrmVTemplate.dot` | Schroeffel M. | 2019-01-09 | Swivel label V para Centraline IRM |
| `SwivelLabelMioHTemplate.dot` | Tolstonog-Riedel L. | 2016-10-07 | Swivel label H para **Centraline MIO** (Modular Input/Output) modules |
| `SwivelLabelMioVTemplate.dot` | Tolstonog-Riedel L. | 2016-10-07 | Swivel label V para Centraline MIO |

**Mecanismo de sustitución**: Bookmarks Word, NO MailMerge. Bookmarks empíricos extraídos de strings: `AI_Bookmark`, `BI_Bookmark`, `IOM_Bookmark`, `NUM_Bookmark`, `AO_Bookmark`, `BO_Bookmark`, `DP_Bookmark`, `IRM_TERM_BOOKMARK`. Métodos `clPrintout.exe`: `WriteBookMarkText`, `AddTextToBookMarks`, `GoToBookmark`, `ReplaceSwivelTilePageBookmarks`, `RemoveSwivelLabelBookmarks`, `InsertSwivelLabelText`, `InsertIrmSwivelLabelText`, `InsertMioSwivelLabelText`, `UpdateIncludePictureFields` (este último sí usa fields Word — IncludePicture para embedear el wiresheet PNG).

---

## XML configs (DocumentTemplate, DefaultFilter)

### `DocumentTemplate.xml` (33 KB) — formato Word XML

NO es XML genérico. Es **HTML+Word XML híbrido** con namespaces:
- `xmlns:o="urn:schemas-microsoft-com:office:office"`
- `xmlns:w="urn:schemas-microsoft-com:office:word"`
- `xmlns="http://www.w3.org/TR/REC-html40"`

Define `<w:WordDocument>` settings (View=Normal, AttachedTemplate=msword-template.dot, UpdateStylesOnOpen, DoNotOptimizeForBrowser), `<o:DocumentProperties>` (Author=Alok, Company=HTSL — Honeywell Technology Solutions Lab India) y un `<style>` CSS extenso con `.TabHead`, `.liTab`, `font-family Arial 10pt`. Sirve como **shell HTML** que se inyecta en Word para crear secciones intermedias del docx con styling consistente. Generado por proyecto `xml2word` (DCIdentifier `http://www.xmlw.ie/xml2word/xml2word.xml` — librería externa de Alok Singh, Irlanda, antigua).

### `DefaultFilter.xml` (16 KB) — filter de qué imprimir

Estructura:
```xml
<PrintFilter name="Default Filter">
  <Print n="Services">y</Print>
  <Print n="Drivers">y</Print>
  <Print n="Datapoints">y</Print>
  <Print n="Parameters">y</Print>
  <Print n="IOAssignment">y</Print>
  <Print n="Calendars">y</Print>
  <Print n="Schedules">y</Print>
  <Print n="Wiresheets">y</Print>
  <Print n="PlantGraphics">y</Print>
  <Print n="PrintFunctionDescriptions">y</Print>
  <Services>
    <Service type="UserService" name="UserService" enable="y">
      <PropertyGroup path="..." enable="y/n"/>
    </Service>
    ...
  </Services>
</PrintFilter>
```

**Granularidad fina**: por servicio, qué PropertyGroups se muestran (e.g. excluye `admin`, `guest`, `User Prototypes\Default Prototype` para evitar leakear configuración interna de seguridad en docs operacionales). Es un filter **per-bog**, modificable por usuario (probablemente vía `BPrintoutDialog → Edit Project`).

### `StandardSegmentTemplate.htm` (2 KB)

XHTML 1.0 Strict con CSS inline (Arial 10pt, `.oddrow #faffef`, `.evenrow #ffffff`, `.mmirow #99ff00`, `.TabHead #ccff66`). **Plantilla de tabla genérica** insertada como segmento HTML cuando se documentan tablas dinámicas (e.g. point lists). Insertada vía Word `Range.InsertFile` o `Selection.PasteHTML`.

### `centraline.bmp` (332 KB)

**24-bit BMP 512x221 px** — logo "CENTRALINE BY HONEYWELL" insertado como header visual en docs (bookmark `CENTRALINEBYHONEYWELL` en `clPrintout.exe`). Alta resolución (7874 px/m) para impresión.

---

## WireSheetControl.dll — ActiveX/COM/OCX

NO es ActiveX/OCX. Es **.NET UserControl** WinForms (`UserControl` base, `IComponentConnector` — interfaz WinForms designer). Strings empíricos:
- `WireSheetControl.WireSheetControl`
- `CmdAddConnection`, `CmdDeleteConnection`, `OnRender`
- `Autoroute (CTRL+R)`, `Mirror (CTRL+M)`, `Pan&Zoom (F7)`, `Snap to grid`
- `CARE Datapoint`, `Math Point`, `Macro Input`, `Macro Output`, `XFM does not match CSD file`
- Tipos puntos: `Analog Input`, `Binary Input`, `Multi-State Input`, `Flag Analog`, `Flag Digital`, `Flexible Points DO Feedback DI`, `Function Register`, `Global Parameters`, `Constant 0`, `Constant 1`

**Origen real**: Es el control de **Honeywell CARE** (Computer Aided Regulation Engineering) reciclado. CARE es el legacy controller programming tool de Centraline (pre-Niagara). El control fue adoptado en `clPrintout.exe` solo para **renderizar el wiresheet a bitmap** (`OnRender` → `System.Drawing.Bitmap`) que luego se embed en Word vía Word `InlineShape.AddPicture` o IncludePicture field. NO se usa como editor en este flow.

**Confirmación referencias .NET sólo (no COM)**: no hay registración OCX, no aparece `CoCreateInstance`, sólo `UserControl` y métodos paint/render normales WinForms.

---

## Caller Java side (qué jars referencian printout)

Tres JARs Niagara N4 firmados por Honeywell (`vendor=Honeywell`, `vendorVersion=4.14.0.4.0.6`, `buildHost=azu-hce-vbf-w13`, `buildMillis=1739850280358` = 2025-02-18):

### `clPrintout-rt.jar` (RT — runtime, station-side)
- `com.honeywell.printout.BPrintMe` — BComponent flag-marker en wiresheet (icono `print.png`) que indica "incluir esto en printout"
- `com.honeywell.printout.BPrintoutStationExt` — extension agregada a `BStation` para hosting estructuras de printout
- `com.honeywell.printout.BStatementExt` — extension para añadir descripciones textuales a componentes
- `com.honeywell.printout.PrintoutConfig` — **paths constantes**:
  - `_printoutDir = "!printout"` (ord absoluto, resuelto vs `niagara_home`)
  - `_pathProjectsFile = "!printout/projects.xml"`
  - `_pathProjectsFileBak = "!printout/projects.xml.bak"`
  - `_pathExportFile = "!printout/printout.xml"`
  - `_pathPrintoutExe = "!printout/clPrintout.exe"`
  - `_pathPxDir = "!printout/px"`
  - `_expFile = "!printout/printout.exp"` (formato propietario adicional, probablemente exportación legacy CARE)
  - `getDefaultStationsHome() → "~stations"` (ord-shortcut a stations dir)
- `com.honeywell.printout.util.Helper` — `isStationRunning()`, lock files (`pathLockFile`), `isLicenseValid()` (gating!)
- `com.honeywell.printout.util.BCreatePointLabelOptionsEnum`, `BPointLabelLargeCoverEnum`, `BPointLabelSmallCoverEnum` — enums Niagara `BFrozenEnum` para opciones de etiquetas
- `com.honeywell.printout.util.BPrintStationNotification`, `BAddStatementExtsNotification` — notification BNotificationManager (cross-station messaging)

### `clPrintout-wb.jar` (WB — workbench-side, dialogs UI)
- `com.honeywell.printout.export.Exporter` — **el writer XML real** (XWriter wrapping)
- `com.honeywell.printout.ui.BPrintoutTool` — entry tool, registrado en módulo
- `com.honeywell.printout.ui.BPrintoutDialog` — **dialog principal** que dispara el flow (descrita arriba)
- `com.honeywell.printout.ui.BPrintoutProjectDialog` — editor de project filter
- `com.honeywell.printout.ui.BOnlinePrintoutDialog` — variante online (via Fox session live)
- `com.honeywell.printout.ui.BAddStatementExtDialog`, `BLargeCoverHelpDialog`, `BSmallCoverHelpDialog`
- `com.honeywell.printout.ui.ExporterProgressDialogWorker` — progress UI BProgressDialog$Worker
- `com.honeywell.printout.util.ui.BPrintStationNotificationHandler`, `BAddStatementExtsNotificationHandler` — handlers para notifications RT-side

### `clPrintout-doc.jar` (DOC — javadocs)
Documentación API en HTML para developers de extensión.

**Dependencias módulo** (de `module.xml`): `file-rt`, `workbench-wb`, `bajaui-wb`, `control-rt`, `fox-rt`, `clPrintout-rt`, `history-wb`, `pdf-wb`, `driver-rt`, `gx-rt`, `bacnet-rt`, `baja`. Confirma uso de **pdf-wb** (clase `com.tridium.pdf.BPxViewToPdf`) y **bacnet-rt** (export BLocalBacnetDevice) y **fox-rt** (online printout vía Fox session a station remota).

**Permisos requeridos** (workbench permissions group): `LOGGING` (acceso al printout log), `UI` (clipboard).

---

## Gotchas operacionales

**G1. Linux/macOS: NO funciona**. clPrintout.exe es PE32 .NET Windows-only + COM Automation requiere MS Word instalado. En Workbench Linux/Mac el botón "Print Documentation" lanzará exec → fail silencioso → `Error Executing Printout!`. Es **Windows-only feature**, no documentado como tal.

**G2. NO Excel**. El nombre `WireSheetControl.dll` 344 KB sugiere Excel pero strings empíricos confirman que **sólo se genera Word .docx** (no `.xlsx`). El control es un editor de wiresheet (canvas custom), no un workbook driver. La hipótesis original "renderizar wiresheet en Word/Excel" es half-true: solo Word.

**G3. waitFor timeout bloquea Workbench**. `Runtime.exec(_pathPrintoutExe)` + `process.waitFor(N, MILLISECONDS)` en EDT → si Word lanza diálogo modal (license, addin macro warning, COM exception), el thread Workbench se bloquea hasta el timeout. UI freeze durante la generación.

**G4. Path tied to niagara_home**. PrintoutConfig hardcodea `!printout/clPrintout.exe`. Si Workbench se instala en path con espacios o caracteres no-ASCII, `Runtime.exec(String)` puede partir el path mal (no usa ProcessBuilder con List<String>). En Niagara-Workbench standard install path es OK; en custom paths puede fallar.

**G5. .vshost.exe es residuo dev**. El binario `clPrintout.vshost.exe` (14 KB) es un debugging hosting process de Visual Studio — NO se ejecuta en producción. Está incluido por error del build pipeline Honeywell (no se purgó `\bin\Release\` apropiadamente). Ocupa 14 KB extra en cada install pero es inerte.

**G6. Word version dependency**. `WordTemplate.dot` formato Word 97-2003 (.dot, no .dotx) con `Code page: 1252`. Word 2007+ abre con compat warning. Office 365 / Word para web NO soporta COM Automation legacy → flow quiebra completamente en hosts sin Office desktop.

**G7. Templates con autores Honeywell internos**. `Schroeffel Martin` (Centraline Austria), `Tolstonog-Riedel Lew (GE51)` (Honeywell GE51 dept), `HTSL` (Honeywell Tech Solutions Lab India), `E308935`, `h446496` (build user). Esto significa templates fueron diseñados manualmente por equipos distintos en distintas épocas (2008-2019) — riesgo de **inconsistencia de styling** entre secciones de un mismo docx generado.

**G8. License gate**. `Helper.isLicenseValid(BWidget, Object)` se invoca antes de exec. License feature flag específica para printout. Si license no incluye el feature, el dialog muestra error pero NO el .exe en ejecución (la barrera es Java-side antes de exec).

**G9. SOURCE_ORD/TARGET_ORD parsing**. En el XML exportado, links wiresheet aparecen como `<knob>` con `sourceOrd`/`targetOrd` (ord absoluto Niagara). El .exe los parsea sin validar — un componente con ord muy largo o caracteres exóticos podría causar overflow en buffer .NET (no verificado, pero superficie potencial).

**G10. printout.exp y projects.xml.bak**. Archivos secundarios en `!printout/`. `.exp` formato propietario CARE (sospecha — nunca abrió, sólo path constante en código). `.bak` es backup automático del filter file. Ambos persisten en el station_home y aparecen en backups completos.

---

## Security implications (.NET sandbox? Office macros?)

**S1. Sin .NET sandbox**. clPrintout.exe corre **fullTrust as user** (manifest level=`asInvoker`). Sin `partial trust CAS` ni AppDomain isolation. Si XML printout.xml es manipulado (path traversal en sourceOrd, XXE injection), el .exe procesa sin validación → posible **XXE attack** (System.Xml `XmlReader` antiguo en .NET Framework 2.0/4.0 era vulnerable por defecto si DTD habilitado). NO confirmado el setting empíricamente, pero `XmlReaderSettings` aparece en strings — si alguien lo configura con `DtdProcessing=Parse`, exploitable.

**S2. Office macros**. Los `.dot` son Composite Documents OLE — pueden contener **VBA macros**. No verificado si los templates tienen macros, pero la posibilidad existe. Word abre el `.dot` con setting de seguridad del usuario — si baja a "Enable all macros", un atacante que reemplace `WordTemplate.dot` en `!printout/` ejecuta código arbitrario en contexto del usuario Workbench con privs Workbench (UI, file system).

**S3. COM elevation**. `OpenMicrosoftWord` invoca COM. Si el usuario está como Admin y Word es lanzado con prompt UAC accidentalmente, ventana Word puede heredar elevación y acceder a archivos restringidos.

**S4. Registry read**. `RegistryKey` + `Microsoft.Win32` en strings. clPrintout lee `HKEY_CLASSES_ROOT` para resolver Word.Application CLSID (`get_RootMacro`, `ClassesRoot`). Lectura, no escritura — riesgo bajo.

**S5. printout.xml leak**. El XML temp puede contener **passwords cifrados, schedule details, schedule expressions, alarm config, IP addresses** de la station entera (todo con `<Print enable="y">`). Persiste en `!printout/printout.xml` después de la generación si Workbench se cierra abruptamente — **leak post-mortem en filesystem audit**. DefaultFilter.xml por defecto excluye PropertyGroups sensibles (`admin`, `guest`, password configs) pero un usuario con custom filter puede activarlos.

**S6. Path traversal en projects.xml**. `loadProjects()` parsea `!printout/projects.xml` con `XParser`. Si un atacante con write access al station_home modifica el XML para que `pathExportFile` apunte a un path arbitrario (`../../../etc/...`), el Exporter podría sobrescribir archivos fuera del printout dir. NO verificado defensa — superficie potencial.

**S7. Workbench permissions sólo `LOGGING` + `UI`**. El módulo declara permisos minimal — no `FILE_SYSTEM` ni `EXEC`. Sin embargo la operación `Runtime.exec` debería requerir un permiso framework superior. Bypass del sistema de permisos Niagara Workbench? Sospecha — `Runtime.exec` evade el AccessController standard al ser una llamada nativa Java unrestricted desde clase autorizada UI.

---

## CORRIGE/EXTIENDE Bloque 26.12

**Bloque 26.12 ACTUAL** (líneas 478-491) dice:

> | Word.dll | 484 KB | Word 2003 Primary Interop Assembly |
> | Office.dll | 152 KB | Office library |
> | WireSheetControl.dll | 344 KB | **Excel worksheet renderer** ← **INCORRECTO** |
> | Interop.Shell32.dll | 48 KB | Shell/COM API |
>
> **Requisito**: MS Office instalado en host (Word 2003+).
> **Uso**: Excel/Word export en Workbench reports.

**CORRECCIONES**:

C1. `WireSheetControl.dll` NO es "Excel worksheet renderer". Es **.NET UserControl WinForms del legacy Honeywell CARE tool**, reutilizado para renderizar wiresheets a bitmap embebido en Word. NO genera Excel.

C2. **NO hay generación de Excel** en este flow. Solo Word .docx + PDF (vía pdf-wb dependency, NO vía la .exe). La frase "Excel/Word export" debe ser solo "Word export + PDF (PxView)".

C3. **El requisito "Word 2003+"** es correcto pero subdocumentado: **NO funciona con Office 365 web/online**. Solo Office desktop con COM Automation registrado.

C4. El binario `clPrintout.exe` NO está documentado en 26.12 como tal — solo se mencionan los DLLs PIA. El .exe es el verdadero entry point de 954 KB que hostea WinForms y orquesta la conversión. **Agregar fila a la tabla**.

C5. `clPrintout.vshost.exe` (14 KB) es **residuo de Visual Studio debugging**, NO un segundo entry point. Mencionar como gotcha de packaging.

**EXTENSIONES nuevas para Bloque 26.12 (o crear 26.12 ampliado)**:

E1. Caller Java side: módulos `clPrintout-{rt,wb,doc}.jar` en `/modules/`. Vendor=Honeywell. Disparado desde `BPrintoutDialog.doPrintDocumentation` vía `Runtime.getRuntime().exec(String)`.

E2. IPC mechanism: **temp file** (`!printout/printout.xml`), no STDIN. PathConstants en `PrintoutConfig.java`.

E3. Output formats: Word `.docx` (project documentation), Word `.docx` con swivel labels (etiquetas físicas Centraline IRM/MIO), PDFs auxiliares de PxView en `!printout/px/`.

E4. Templates inventariados (8 `.dot` + 2 XML + 1 HTM + 1 BMP). Mecanismo de variable substitution: **Word Bookmarks** (NO MailMerge — los WdMailMerge* del Word.dll PIA son ruido).

E5. PIAs Microsoft (Word.dll 484 KB, Office.dll 152 KB) son **redistribuibles MS** — no Honeywell custom. WireSheetControl.dll es Honeywell. Interop.Shell32.dll es interop wrapper estándar.

E6. Build provenance: PDB path `C:\Users\h446496\Downloads\NiagaraDotNetReport\NiagaraDotNetReport\clPrintout\obj\Release\clPrintout.pdb`. Proyecto `NiagaraDotNetReport` (build user h446496). Templates por autores múltiples 2008-2019 (HTSL India, Centraline Austria/Germany).

E7. Concept "Swivel labels" = **etiquetas físicas adhesivas para placas frontales** de controladores Centraline IRM (Intelligent Room Manager) y MIO (Modular I/O). Incluyen point names, datapoint IDs, terminal numbers para campo (orientación H = horizontal, V = vertical). Único en productos Honeywell — Tridium standard NO los tiene.

E8. License gate: `Helper.isLicenseValid()` Java-side antes de exec. Feature licensable.

E9. Linux/macOS Workbench → flow falla silente (Windows-only).

---

## Confirmación hipótesis original

> "el flow es Workbench → genera XML wiresheet → llama clPrintout.exe via shell exec → clPrintout abre Word con templates → MailMerge XML data → user obtiene .docx"

**CONFIRMADO con 1 corrección**:
- Workbench → XML: ✅ (`Exporter.java` con XWriter a `!printout/printout.xml`)
- shell exec: ✅ (`Runtime.getRuntime().exec(String)` blocking con waitFor)
- abre Word con templates: ✅ (COM `Word.Application.Documents.Add(WordTemplate.dot)`)
- **MailMerge ❌ → es Bookmarks**. Word `Document.Bookmarks["AI_Bookmark"].Range.Text = ...`. Razón: bookmarks permiten posicionamiento exacto en plantilla compleja (swivel labels = grid de tiles), mientras MailMerge es row-oriented. Honeywell eligió bookmarks por flexibilidad de layout.
- user obtiene .docx: ✅ (`SaveFileDialog` user picks output)

**Hallazgo extra no en hipótesis**: `BOnlinePrintoutDialog` permite imprintear desde station **online via Fox session** (sin volcar BOG offline). Mismo mecanismo (exec .exe + temp xml), pero datos vienen vía `BFoxSession` live en lugar de file dump. Esto introduce dependency adicional `fox-rt` y network round-trips para cada componente leído.
