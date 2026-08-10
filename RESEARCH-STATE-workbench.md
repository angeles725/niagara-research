# RESEARCH-STATE — focus: workbench (BOOTSTRAPPING)

> Focus **BOOTSTRAPEADO** el 2026-08-10 a pedido del usuario tras confirmar que el Workbench está lejos de
> cubierto: de **202 módulos `-wb`** en el install, solo 2 subsistemas están a fondo (edición PX y charting);
> 120 módulos no se mencionan ni una vez. §16 multi-focus: focus nuevo sobre target maduro.
>
> Corpus en **Español (bloques en EN, convención desde B115)**. Numeración global; próximo libre: **B427**.
> Engram topic key: `research/niagara/workbench/{gaps,progress}`.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 422
gaps_closed: 9
known_gaps: 12
investigable_open: 3
requires_execution_open: 0
blocked_open: 0
block_scope: shared-global
<!-- /research-state.v1 -->

focus: workbench
status: active
planned_on: 2026-08-10
started_on:

## Ángulo declarado (§b2)

La infraestructura del **Workbench Swing** como herramienta de ingeniería — el core UI (`bajaui`/`gx`/
`workbench-wb`), el framework de **managers/views/tablas**, el **wire sheet**, el **property sheet**, el
**nav tree**, y el framework de **field editors** (no-PX). **EXCLUYE por REMITTANCE** lo ya profundo:
edición PX (`px-*`, `pxEditor`, `kitPx*`, `webEditors`, `galileoKitPx`) y charting (`chart*`, `webChart`).
No es re-derivar lo documentado — es abrir la infraestructura sobre la que esos focuses se apoyaron sin
nunca abrirla.

## Coverage matrix (§13 audit-first, sembrado por sweep delegado 2026-08-10)

202 módulos `-wb` en 15 clusters. Verificado por el driver: dirs vineflower presentes (gx-wb 1075 `.java`,
bajaui-wb 536, workbench-wb 505, wiresheet-wb 68, hx-wb 122, devkit-wb 683, wbutil-wb 84).

| Subsistema | # | Profundidad actual | Veredicto |
|---|---|---|---|
| PX editing + charting | 9 | **deep** B179-215/251-259/289-293/421-423 | REMITTANCE |
| Core UI toolkit (gx/bajaui) | 4 | **partial** (B22 runtime, B38 forms, B183/190 gx-PX) — modelo BWidget/BPane Swing SIN abrir | **GAP WB01** |
| Workbench shell/framework | 3 | **partial** (B9/B15 conceptual) — shell, nav tree, sidebars, commands SIN abrir | **GAP WB02/04/05/07** |
| Wiresheet editor | 1 | **named-only** (B15 título) | **GAP WB03** |
| Hx/mobile/browser | 4 | **named-only** (B9/B194 mención) | **GAP WB06** |
| Platform admin UIs | 15 | partial (rt-level B10/B129/B392-395) | WB10 (low) |
| Core app domain UIs | 11 | partial (rt-level B8/B24/B34) | capa tool -wb no documentada (low) |
| Manager/table/grid | 8 | partial (B267/B365) — celltable/BWManager SIN abrir | **GAP WB05** |
| Dev & test tools | 5 | **none** (devkit-wb sin cubrir) | **GAP WB08** |
| Security/auth/cert UIs | 7 | eSignature **deep** B350-356; resto **none** | low |
| Provisioning/backup/cloud | 9 | partial (B39) | low |
| Content/data/report/misc | 20 | report B357-365/email B331/json B347 deep; resto none | low |
| Protocol driver UI long tail | 51 | modbus-wb deep B304; bacnet/lon/knx partial; resto none | **GAP WB11 (framework) + WB12 (bucket)** |
| OEM UIs (Hon/Centraline/Galileo) | 46 | mayormente deep B77-122/241-250 | residuo bajo valor |
| Video/surveillance UIs | 9 | **none** | fuera de scope del ángulo |

## Gap-backlog (priorizado)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | WB01 gx-wb + bajaui-wb — modelo BWidget/BPane/BComponent-UI + layout + event dispatch + theming Palladium | decompiled-java | closed (B427) |
| high | WB02 workbench-wb — shell + nav tree (BNavTree) + sidebars + console | decompiled-java | closed (B428) |
| high | WB03 wiresheet-wb — canvas BSheet + BLink + drag/drop palette + routing + undo | decompiled-java | closed (B429) |
| high | WB04 workbench-wb propsheet + slotsheet + dispatch de field editors Swing (no-PX) | decompiled-java | closed (B430) |
| medium | WB05 workbench-wb celltable/cellmini + patrón BWManager (device/point managers) | decompiled-java | closed (B431) |
| medium | WB06 hx-wb — framework de render Hx por servlet (BHxView, pipeline HTML/JS) | decompiled-java | closed (B433) |
| medium | WB07 workbench-wb — command + wizard + transfer (seams de extensión) | decompiled-java | closed (B432) |
| medium | WB08 devkit-wb — dev tooling (NO SDK): wizards New Module/Driver + Slotomatic + lexicon tools | decompiled-java | closed (B434) |
| low | WB09 wbutil-wb — capa de servicios UI transversales (user/role/perm UI + cell editors + credential/license tools) | decompiled-java | closed (B435) |
| low | WB10 platform-wb + platDaemon-wb — UI de administración de plataforma | decompiled-java | pending |
| low | WB11 driver-wb + ndriver-wb — framework genérico de UI de driver (BAbstractDiscovery, BDeviceManager) | decompiled-java | pending |
| low | WB12 cola larga de UI de drivers (51 módulos, bucket — tras WB11) | decompiled-java | pending |

Orden recomendado: WB01 → WB02 → WB03 → WB04 → WB05 → WB07 (modelo completo del framework Swing);
WB08 desbloquea los seams de extensión; WB06 (Hx) es ortogonal.

## Blocked gaps

- none

## Clasificación (§8)

- **read-only-investigable**: **3** abiertos (9 cerrados: WB01-WB09). **requires-execution**: 0. **blocked**: 0.
- **Coverage metric**: 9 / 12. + wbutil (B435).
- **Próximo libre**: B436. NEXT = WB10 (platform-wb UI). Siguen WB10, WB11, WB12.

## Historia de iteración

| It | Fecha | Gap | Bloque | Hallazgo | Delegado? · tier |
|---|---|---|---|---|---|
| 0 | 2026-08-10 | (bootstrap) audit-first coverage matrix | — | 202 módulos `-wb`; matrix + backlog sembrados por sweep delegado (sonnet) | yes · sonnet (audit) |
| 1 | 2026-08-10 | WB01 gx/bajaui widget model | B427 | BWidget extends BComponent (widgets viven en el slot tree); layout deferido relayout→doLayout→setBounds baked per-BPane; gx = interface Graphics + value types (impl AWT en gx-wb); eventos BWidgetEvent+Topic; theming 3 familias (Palladium/Curium/Custom) × ~40 widget themes device-selected; AWT bridge (AwtShellManager=Panel, BSwingWidget=JRootPane) mangleado→INFER. VERIFY atrapó citas limpias del sweep sobre bodies mangleados → downgrade a INFER. | yes · sonnet |
| 2 | 2026-08-10 | WB02 shell + nav tree + sidebars + console | B428 | BWbShell(abstract)→BNiagaraWbShell; root=BWbPane (views BViewTabbedPane + sideBar + console + splits); active view=tab. BNavTree extends BTree/NavListener, ordMap O(1), expansión lazy; selección→shell.hyperlink(node.getNavOrd()) (hyperlinkea un ORD, no una view). Sidebars auto-descubiertos por type registry (sin registro explícito). BConsole extends BEdgePane (panel persistente, el canal de compile de B426). ORD→view: NHyperlinkInfo resolve→getViewAgentsList→getInstance; @AgentOn(types) + filtro por perfil (WbSys.getFilteredViewList). NavMonitor poll 20s. Sweep anidó sub-agente→pedí Q1-Q5 consolidados vía SendMessage; drift de línea WbSys 111→88 corregido en verify. | yes · sonnet |
| 3 | 2026-08-10 | WB03 wire sheet editor | B429 | BWireSheet @AgentOn(baja:Component,W)→BWireSheetPane(BEdgePane)→BScrollPane→BWsCanvas (BTransferWidget, NO BCanvasPane). RootGlyph 2 capas (componentLayer/linkLayer); ComponentGlyph=live BComponent+handle; StdComponentGlyph=titleBar+SlotBarGlyph/slot (terminals)+footer. Links=LinkSnakeGlyph ruteo ortogonal wixel-grid. CLAVE: creación de link/component DELEGADA a javax.baja.workbench.commands.LinkCommand/RelateCommand (el sheet NO escribe el BLink). Layout persiste como slot HIDDEN wsAnnotation (BWsAnnotation p/q/w) en el componente mismo (flag 1, en Transaction) → vive en el BOG, no side-car. Undo=WsCommand extends Command. VERIFY: cita LinkCommand estaba en states/ y token mangleado ln→resuelto en copia preservada. | yes · sonnet |
| 4 | 2026-08-10 | WB04 property sheet + field editor dispatch | B430 | BPropertySheet @AgentOn(baja:Component,r)=Property rows curados (filtra hidden/action/topic/wsAnnotation); BSlotSheet @AgentOn(W)=getSlotsArray raw schema. FE base BWbFieldEditor extends BWbEditor (doLoadValue/doSaveValue). DISPATCH 2 niveles: TIER1 facet "fieldEditor" override → TIER2 kid.getAgents().filter(FE).getDefault() keyed en el TIPO DEL VALOR (mismo @AgentOn que views B428/web FE B421/PX FE B214). Concretos: baja:Boolean→BBooleanFE, baja:Complex→BPropertySheetFE (recursivo). Commit: dirty→shell Save→1 Transaction→complex.set(prop, saveValue(), tx). | yes · sonnet |
| 5 | 2026-08-10 | WB05 manager/table framework | B431 | 2 PREMISAS REFUTADAS: no existe BWManager (base=BAbstractManager extends BWbComponentView); BCellTable/cellmini NO es la tabla del manager (es grid de edición live separado, la tabla es BMgrTable). Manager 2-pane (BSplitPane: learn arriba/table abajo). BMgrTable.reload mapea children vía SlotCursor (o BQL deep, B406) filtrado por model.accept AND hasOperatorRead (seguridad row-level). MgrColumn abstract (display/edit/editor, flags EDITABLE/UNSEEN/READONLY). MgrLearn (default null, override por driver) = discovery async por BJob. New/Add→MgrEdit.invoke→BMgrEditDialog batch→commit→addInstances→Mark.moveTo. | yes · sonnet |
| 6 | 2026-08-10 | WB07 command + wizard + transfer | B432 | CAPSTONE del framework. Command (concreto) doInvoke→CommandArtifact→UndoManager (undo stack max 10) = el modelo de undo de TODO el Workbench (WsCommand B429, MgrCommand B431, Save B430 lo obedecen). Contribución: BWbView.getViewMenus/getViewToolBar (sin CommandSet type); shell merge en activación. Edit IDs (CUT=0..PASTE_SPECIAL=11)→PluginCommand→invokeCommand(id)→BTransferWidget. Transfer: BTransferWidget (3 abstract), clipboard=Mark (BObject[]+names) @TransferFormat.mark; TransferArtifact implements CommandArtifact async+undoable. Wizard: BWizardView+WizardViewModel (step via StepModel). 4 seams de extensión, todos @AgentOn. Cierra arc WB01-WB07 (B427-B432). | yes · sonnet |
| 7 | 2026-08-10 | WB06 Hx render framework | B433 | BHxView extends BServletView (NO BWbView) — Hx=profile de servlet/HTML, no Swing. Sin BHxServlet: la view ES el handler; /ord?target→BServletView agent→doGet→profile.writeDocument→view.write(HxOp)→HtmlWriter buffer→page shell (DOCTYPE/head/form + CSRF). Eventos: registerEvent server-side + dispatch header-keyed (EVENT_PATH/EVENT_ID)→process→event.handle; Command extends Event (evento web, NO el Command de undo). Hx-vs-Wb: HxFilter agent filter + translate() swap al peer Hx del registry. State: stateless por request; live values POLL-based (hx.poll.freq 5000ms), sin push/HxSession. Legacy: @Deprecated 4.13/5.0, BHTML5HxProfile=sucesor. | yes · sonnet |
| 8 | 2026-08-10 | WB08 devkit-wb | B434 | PREMISA "SDK" REFUTADA: devkit-wb = módulo de HERRAMIENTAS de desarrollo (Niagara Developers Kit), NO un SDK ni ejemplos. De 683 clases, 506 son JavaParser embebido; ~177 Tridium. runtimeProfile="wb" (NUNCA en station), autoload, permisos <<ALL FILES>>+exitVM. 4 subsistemas: New Module Wizard (Velocity/Gradle scaffolding), New Driver Wizard (NDriver/Video .vm templates), Slotomatic (slot code-gen del // AUTO-GENERATED, el mismo del Gradle plugin, tie a B426), Lexicon tools (BLexiconTool extends BWbNavNodeTool, tie a B428) + PaletteGenerator. No hookea el build lifecycle de la station. | yes · sonnet |
| 9 | 2026-08-10 | WB09 wbutil-wb | B435 | NO es librería pasiva: capa de servicios UI transversales que registra @AgentOn views en ~15 tipos core. 84 clases/8 dominios. Aquí vive la UI de user/role/permission: BUserManager (@AgentOn baja:UserService, extends BAbstractManager), BPermissionsBrowser (ACL RoleService+UserService). Cell editors (12 primitivos), field editors (gx color/brush/ORD), BColorChooser HSV. Security-adjacent: BManageCredentialsTool (creds remotas via AuthUtil), BRequestLicenseTool (reflection a portalApi). Password FE mangleados (token n, parent BWbFieldEditor real). Pull Fox+authn = módulo required. | yes · sonnet |
