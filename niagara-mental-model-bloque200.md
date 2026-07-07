# Bloque 200 — El sistema de templates: `.ntpl` (zip+bog), configs expuestos, deploy/upgrade y bulk vía Excel

> Research del focus **`px-editor-deep`** (gap X2): el sistema de **templates** de Niagara (`template` +
> `templateBulk`). Scope-honesto: es el sistema de templates GENERAL (Application / Component-Device / Station);
> el gráfico PX es UNA faceta (el "Graphics tab"), no el todo. Documenta qué es un `.ntpl`, el modelo de config
> expuesto (inputs/outputs/relations + passwords), el proceso de deploy/upgrade, y el bulk-deploy por spreadsheet.
> `easyBinding` NO se abrió (gap nuevo X6). NO cubre `api/impl/*TemplateSource` ni el ApplicationTemplateInstaller.
>
> Sources (preservados §5): `sources/decompiled/template-rt/` (97 java) + `template-wb/` (66) + `templateBulk/` (20)
> + `sources/manuals/template-docs/` (8 HTML oficiales `[CERT-doc]`, en SOURCES.md). Barrido delegado (sonnet)
> 2026-07-06; 7 citas load-bearing token-checked literal. Method: decompilado + doc oficial.
> Markers (§3): `[CERT]` `file:line` · `[CERT-doc]` `sources/manuals/...html` · `[INFER]`. Tipo: EVIDENCE block.
> Citas relativas al paquete `com/tridium/template/` (rt) o `.../ui/` (wb).
>
> Capa PX (templates). Connects [Block 191] (BPxEditorPane), [Block 198] (sidebars), [Block 199] (webChart),
> [Block 197] (síntesis).

---

## 200.1 — Qué ES un template: el `.ntpl` es un zip con un bog + manifest `[CERT]`

Un template es un **snapshot bog de un árbol de componentes** + recursos, empaquetado como zip:

`BNtplFile extends BZipFile implements BINtplFile, TemplateConst` (`file/BNtplFile.java:48`) — **el `.ntpl` es
literalmente un zip** (abierto vía `BZipSpace`, `:178-192`) que contiene: un `BBogFile` (el bog del árbol), un
`template-manifest.xml`, archivos px (`/shared` o `/px`), imágenes (`/shared` o `/images`), un PNG de preview opcional,
y station files. `getBaseComponent()` (`:83`) abre el bog y devuelve el primer hijo de su root = el componente template
real. `BINtplFile` (`file/BINtplFile.java:14`, `extends BITemplate, AutoCloseable`) expone
`getTemplateManifest()`/`getPxFiles()`/`getStationFiles()`. `[CERT]`

**El modelo runtime — `BTemplateConfig`** (`BTemplateConfig.java:107`, `BComponent`): el nodo central. Lleva
`templateName`, `uID` (`BUuid`), `version`/`versionDate` (`BVersion`), `deployed`, `propagated`, y `pxEditBindings`
(un `BVector` de paths `slot:` que trackea qué propiedades se tocaron vía el editor Graphics/px, `:402-410`). Posee los
hijos `BConfigBinding[]` / `BPasswordBinding[]` / `BRelationInfo[]` y genera el `TemplateManifest` on-demand
(`generateManifest():730`). `TemplateManifest` (`manifest/TemplateManifest.java:10`) es un bean Java (no BObject) con
vendor/version/uID/`bogSignature` + settings/links/bindings/resources/subtemplates/dependencies, leído/escrito como el
XML `template-manifest.xml` dentro del zip. `[CERT]`

**Gotcha — el in-memory file space.** `BMemoryFileSpace` (`file/BMemoryFileSpace.java:29`) es un file space singleton
en memoria (scheme `memory:`, `file/BMemoryScheme.java:14,26`) respaldado por un `HashMap` estático, usado para stagear
los px/imágenes sacados del zip durante la edición SIN tocar disco; se limpian en `BNtplFile.close()`. `[CERT]`

**Firma/versión.** `BTemplateSignature` (`BTemplateSignature.java:15`, `BSimple`) envuelve `version + ":" + hex(sig)` —
detecta drift entre una instancia deployada y su fuente (§200.4). `TemplateConst` (`TemplateConst.java:8-51`) define el
dictionary de tags `ntpl:*` y las extensiones **`ntpl`** (component/device/station) vs **`napl`** (application). `[CERT]`

## 200.2 — Tres tipos: Component/Device · Application · Station `[CERT-doc]` + `[CERT]`

`ApplicationTemplateVs.ComponentDevi-B8475EE9.html` `[CERT-doc]` distingue los tipos; el código lo corrobora:

| | Component/Device | Application | Station |
|---|---|---|---|
| Deploy target | cualquier container (el usuario elige) | solo el Config root (reemplaza la station) | solo al CREAR una station nueva |
| Multiplicidad | N instancias/station | exactamente 1/station | n/a (una vez) |
| I/O | inputs/outputs/relations | ninguno | ninguno |
| Bulk deploy | sí (spreadsheet, N filas) | 1 fila (configura la única instancia) | no |
| Upgradeable | sí | sí | no (solo creación) `[INFER]` |

**`BApplicationService`** (`BApplicationService.java:32`, `BAbstractService`) vive bajo `Services` con la única propiedad
`BTemplateConfig configuration` + un `templateSignature`. `BTemplateConfig.createConfigForRoot(root, isApplication=true)`
(`:861-882`) instancia `BApplicationService`, lo agrega a `station.getServices()` y usa su slot `configuration` como el
`BTemplateConfig` — o sea el application template usa el MISMO modelo `BTemplateConfig`, solo anclado bajo el service en
vez de como hijo dinámico. `BApplicationInstallSpecs` (`application/BApplicationInstallSpecs.java:32`) es el struct de
params de `installApplication`: `upgrade`, `checkModules`, `fileOrd` (el `.ntpl`/`.napl`), `toBeRemoved` (`BOrdList` de
componentes a borrar antes de instalar). `[CERT]`

**Gotcha — dos generaciones co-existen.** El make delega a un fqcn `NiagaraTemplate` (`api/NiagaraTemplate.java:27`,
`AutoCloseable`) que envuelve un `TemplateSource` (strategy: NewStation/NewApplication/NewComponent/…Source). Este layer
`api`/`api.impl` (modelo `TemplateProperty`/`TemplateElement`/`OptionalComponent`) es una abstracción MÁS NUEVA encima del
`BTemplateConfig`/`TemplateManifest` viejo; ambos se usan lado a lado (ej. `BulkDeployUtil` usa `BTemplateConfig` para
component templates pero `NiagaraTemplate` para application). `[INFER]` (dos generaciones del mismo concepto)

## 200.3 — El contrato: inputs/outputs/relations/configs + passwords `[CERT]` + `[CERT-doc]`

`TemplateInputsOutputsRelations-89F2BFFF.html` + `ConfiguringExposedProperties-02D26B50.html` `[CERT-doc]`:

- **Inputs/Outputs** = slots del root flagueados por dirección. `BTemplateConfig.isInputSlot`/`isOutputSlot` (`:811-826`)
  testean el bit `4096` (input) vs `4097`/`COMPOSITE_READONLY` (output) en el root, salteando slots que sean `BLink`.
  `getInputSlotTags`/`getOutputSlotTags` (`:658-678`) leen del `BLink`/`BKnob` los tags "Bind Hints" (query NEQL) y
  "Target Slot Hints" — guardados como tags en el link, no en el slot. `[CERT]`
- **Relations** = `BRelationInfo` (`BRelationInfo.java:15`, `BSimple`): `inbound`, `relationId`, `relateHints` (NEQL),
  `userTip`, `slotPathScope`. `getRelationInfos()`/`getUnboundRelationInfos()` (`:696-728`) diffean las relaciones
  declaradas contra las que existen en el root deployado. `[CERT]`
- **Configs (propiedades expuestas)** = un `BConfigBinding` por propiedad expuesta (`BConfigBinding.java:33`, `BStruct`):
  `targetOrd` (ord al componente target), `sourceSlot` (la prop expuesta agregada dinámicamente en el propio
  `BTemplateConfig`), `targetSlot` (nombre del slot target **o** un pseudo-atributo `"#Name"`/`"#DisplayName"`,
  `BTemplateConfig.java:357-365` — convención no documentada para renombrar el componente deployado vía config).
  `propagateConfiguration()`/`changed()` (`:260-421`) implementan el sync bidireccional: editar la config empuja al
  target (`changeBoundPropertyOrAttribute`), y un `TargetSubscriber` (inner, `:967-1048`) trae cambios out-of-band del
  target de vuelta al default de la config. Doc: doble-click en una propiedad del pane izquierdo del tab Configuration la
  expone; el valor ingresado es el DEFAULT aplicado al deploy. `[CERT]`
- **Passwords — gotcha de secreto (el más importante).** `BPasswordBinding` (`BPasswordBinding.java:37`): `pswOrd`,
  `pswParentSlot`, `pswSlot`, `isDynamic`. `isPasswordDefault()` (`:105-124`) solo cuenta el binding si el valor live del
  target `((BPassword)bValue).isDefault()` — un template solo "declara" un slot de password expuesto si actualmente
  tiene el placeholder default, protegiendo contra shippear un secreto real horneado en el archivo. Y
  `BTemplateConfig.extractPassword`/`extractUsernameAndPassword` (`:334-355`) STRIPEAN activamente el password real de la
  copia config de vuelta a `BPassword.DEFAULT` cuando una prop de password bound cambia — **el secreto real solo vive en
  el target deployado, nunca se round-trippea por el slot de la config del template**. `[CERT]`

## 200.4 — Make / Install / Upgrade: el proceso de deploy `[CERT]` + `[CERT-doc]`

`AboutTheDeployProcessTemplates-9A2A91E8.html` + `UpgradingADeployedTemplateTemplates-9A17381D.html` `[CERT-doc]`.

- **Make**: `BMakeTemplateJob` (`job/BMakeTemplateJob.java:30`, abstract `BSimpleJob`); `BMakeStationTemplateJob`/
  `BMakeApplicationTemplateJob` llaman `NiagaraTemplate.createFromLocalStation()`/`createApplicationFromLocalStation()`
  y luego `saveTemplateToTemporaryFile()` (`:60-67`) que guarda a `^temp` y registra el ord del `.ntpl`. `[CERT]`
- **Install** (application): `BInstallApplicationTemplateJob` (`job/BInstallApplicationTemplateJob.java:83-108`) exige
  superuser, construye un `ApplicationTemplateInstaller` (fqcn) sobre el `.ntpl` + `componentsToBeRemoved`, chequea
  módulos, y llama `installer.upgrade(cx)` o `install(cx)` según `upgradeMode`. `[CERT]`
- **Upgrade/Redeploy** funnelean por **`UpgradeUtil.upgrade()`** (`UpgradeUtil.java:229-275`) — el doc "Deploy process"
  verbatim, 4 fases: **save** (`saveTemplateData()` — props del root, valores de slots px-edit-bound, passwords, configs,
  input links, output knobs, relations, tags no-`ntpl`, estado enabled de `BHistoryExt`) → **remove** (`rootParent.remove
  (deployedRootProperty)`) → **deploy** (`mark.copyTo(rootParent)` vía transfer `Mark`/`DeployToComp`) → **restore**
  (`restoreTemplateSaveData()`, espeja el save exacto, `:477-509`). `rebuildRelations()` (`:511-565`) junta las relaciones
  cross-template ANTES de cualquier remove y las reconstruye al final, para que borrar el template A no deje colgada una
  relación que B necesita. `[CERT]`
- **Signature/drift**: `BTemplateService.updateSignature()` (`:343-364`) compara el `bogSignature` del `.ntpl` contra un
  CRC del árbol live; `BTemplateInfo.getModifiedState()` (`BTemplateInfo.java:264-271`) devuelve `-1` unknown / `0` en
  sync / `1` drifteado — el estado "Up to Date"/"Out of Date" del Template Manager. `BTemplateChannel`
  (`BTemplateChannel.java:30`) es un canal Fox (registrado como "template") que expone `upgradeTemplate` como comando
  remoto client→station. `[CERT]`

## 200.5 — Bulk deploy vía Excel: `templateBulk` = Apache POI cargado reflexivamente `[CERT]` + `[CERT-doc]`

`TemplateBulkDeployment-7D0DBE85.html` + `AboutTheSpreadsheet-89F3D5D6.html` `[CERT-doc]`.

**`templateBulk` es Apache POI envuelto**, no un formato propio: `templateBulk/.../excel/impl/WorkbookImpl.java:13-14`
importa `org.apache.poi.ss.usermodel.*` / `XSSFWorkbook` directo, envolviendo cada tipo POI tras una interfaz Tridium. `[CERT]`

**Gotcha — la abstracción vive en `template`, la impl es un módulo OPCIONAL.** Las interfaces (`Workbook`/`Sheet`/`Cell`/
`Factory`…) viven en `template-rt/.../com/tridium/excel/`, NO en `templateBulk` (que solo trae el `impl`). `ExcelUtils`
(`com/tridium/excel/ExcelUtils.java:16-27,91`) lo carga reflexiva y lazy: `EXCEL_SUPPORT_MODULE="templateBulk"` →
`Sys.loadModule("templateBulk")` → `loadClass("com.tridium.excel.impl.FactoryImpl")`; si el módulo no está instalado,
lanza `UnsupportedOperationException("Install module templateBulk for Excel file support.")`. `BTemplateService.
serviceStarted()` setea `isBulkOperationSupported` desde eso — **bulk deploy es una feature opcional gateada por presencia
de módulo**, desacoplando `template` de una dependencia dura de POI. `[CERT]`

**UI**: `BulkDeploy` (`ui/BulkDeploy.java:57-153`) elige el `.xlsx`, lo carga vía `BulkDeployWorkbook` (que lee 3 named
ranges — `version`/`templateType`/`keepPrivate` — para identificar el archivo sin parsearlo entero, `BulkDeployWorkbook.java:18-195`),
maneja encriptación (prompt de password si `wb.isEncrypted()`), y corre `TemplateDeployWorker` (una worksheet por template,
una fila por instancia) o el wizard de application install. `BulkDeployUtil.exportTemplateToExcel()` (`ui/BulkDeployUtil.java:221-353`)
es el export: una `Sheet` por template, 6-7 filas de header (los bloques Input/Output/Relation/Config/Optional/Tag), locks
de protección de celda POI, encriptación opcional. **4.14 agregó una 3ª columna "Slot Path Scope"** por I/O/relation
(`EXCEL_HEADER_ROWS=6` vs `EXCEL_HEADER_ROWS_WITH_SLOT_PATH_SCOPE=7`, `:136-137`). Doc: el orden de tabs = orden de deploy;
no funciona offline; no cascadea a subtemplates. `[CERT]`

## 200.6 — UI (`template-wb`) + el Graphics tab ES el PxEditor `[CERT]`

`BTemplateManager` (`ui/BTemplateManager.java`) es el manager sobre los `BTemplateInfo` deployados (acciones upgrade/
downgrade/redeploy vía `BUpgradeTemplateJob`+`BJobBar`). `BTemplateBogEditor` (`ui/BTemplateBogEditor.java:60-79`) es el
tab de árbol: un `BEdgePane` combinando `BNavTree`+`BPropertySheet`+`BWireSheet`+`BSlotSheet` sobre el root del template —
la UI Workbench ordinaria repurposeada para editar la lógica interna. `[CERT]`

**El tie-back al focus px-editor**: `BTemplatePxEditor` (`ui/BTemplatePxEditor.java:91`, `BEdgePane implements PxListener`)
**embebe directamente `com.tridium.px.editor.BPxEditorPane`** (`import` en `:4`, campo `private BPxEditorPane editorPane`
en `:132`) y escucha los `PxEvent`/`PxComponentEvent`/`PxLayerEvent`/`PxPropertyEvent` — o sea **el Graphics tab de un
template ES la MISMA maquinaria PxEditor/PxEditorPane** documentada en el focus px-editor (B191/B198), solo envuelta con un
nav-tree + dropdown de `PxFile` (multi-px por template). Confirma `GraphicsTabTemplates-9DC53E6E.html`. `[CERT-doc]`

## 200.7 — Connections

- **[Block 191]** (el editor como herramienta): `BTemplatePxEditor` embebe el `BPxEditorPane` de B191 — un template con
  gráfico reutiliza el editor PX completo, no una versión reducida (§200.6).
- **[Block 198]** (sidebars): al editar el gráfico de un template, el cell-sheet/árbol/commands de B198 operan sobre el
  `.px` embebido en el `.ntpl` (stageado en el `memory:` file space, §200.1).
- **[Block 199]** (webChart): un `.chart`/`ChartWidget` puede vivir en el px de un template como cualquier widget; la data
  la sirve el servlet webChart, independiente del deploy del template.
- **[Block 197]** (síntesis 7 capas): X2 abre la capa "templates/reutilización" que la síntesis marcó como boundary.
- **Gap nuevo descubierto** → **X6 `easyBinding`** (119 clases rt/wb/ux): NO referenciado por `template`; subsistema de
  binding simplificado, distinto — queda en el backlog para su propia iteración.
- **Fuera de scope** (nombrados): `api/impl/*TemplateSource`, `ApplicationTemplateInstaller`, `Mark`/`DeployToComp`
  (transfer), `BZipFile`/`BZipSpace` (base zip), `org.apache.poi.*` (Excel real) — feeds de una futura profundización.
