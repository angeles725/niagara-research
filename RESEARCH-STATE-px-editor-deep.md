# RESEARCH-STATE — Focus `px-editor-deep` (STOPPED — 11/11 CERRADO)

> Focus **ACTIVO** (arrancado 2026-07-06). Captura lo que los focuses `px-menu` (B179-B190) y `px-editor`
> (B191-B196) NO cubrieron a fondo. Numeración global de bloques (desde B198). Corpus en Español (técnico EN).
> Engram topic key: `research/niagara/px-editor-deep/{gaps,progress}`.
> Continuar: `/research-sdd niagara-research px-editor-deep continue`.
<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 247
gaps_closed: 0
known_gaps: 0
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->


## Ángulo declarado (§b2)

Profundizar donde `px-editor` solo NOMBRÓ, y abrir los módulos PX-adyacentes SIN tocar. Dos naturalezas
(prefijos distintos): **D** = subsistemas internos de `pxEditor-wb`; **X** = módulos vecinos.
Prioridad del usuario: **D1 (`sidebars/`, el property/cell-sheet — corazón operativo)** primero, luego
**X1 (`webChart`) y X2 (templates)**.

## Cobertura

**11 / 11 gaps** (ratio 1.00) — **FOCUS CERRADO**. Read-only-investigable: 0 → STOP (§8). · requires-execution: 0 · blocked: 0.

## Historial de iteraciones

| # | Gap | Bloque | Hallazgo | Delegado? · tier |
|---|---|---|---|---|
| 1 | D1 | B198 | `sidebars/`: `BPxCellSheet` (property/binding editor) + 3-way `newCellEditor` + los CE del Workbench + commands→`javax.baja.ui.Command` (undo/redo) + árbol `PxTreeModel`/`PxTreeSelection` sync + propsheet/layersheet + binding/ ORD-rewriting. 36 CERT/3 INFER. | sí · sonnet |
| 2 | X1 | B199 | `webChart`: charting = **bajaux puro** (D3/JS), Java `ux` solo bridge `JsInfo` (confirma B194). Capa `rt` = servlets query(data/schedule)/file + `.chart` JSON + RPC. Motor JS: `seriesFactory` (4 series: Servlet/Schedule/Point/External), scales Time/Value D3, sampling auto 2500. Settings sobre `webEditors`. 27 CERT/9 CERT-doc/2 INFER. | sí · sonnet |
| 3 | X2 | B200 | `template`+`templateBulk`: sistema de templates (Application/Component/Station). `.ntpl`=zip(bog+manifest), `BTemplateConfig` modelo central, configs expuestos (`BConfigBinding`) + passwords (strip del secreto), deploy=`UpgradeUtil` save→remove→deploy→restore, bulk vía Excel (`templateBulk`=POI reflexivo opcional). Graphics tab = `BPxEditorPane` embebido. Descubre gap X6 (easyBinding). 24 CERT/11 CERT-doc/3 INFER. | sí · sonnet |
| 4 | D3 | B201 | `make/` wizard: `BMakeWidget`(BEdgePane) + selección **agent-based** (`BMwConfig` implements BIAgent). 8 estrategias `BMw*`: Chart/TimePlot (chart **clásico** `javax.baja.chart`, §14 vs webChart B199), BoundLabel (fallback), PxInclude, WorkbenchView, ActionBatch/PropertyBatch (grid). `MakeWidgetContext` reutiliza el cell-sheet (B198). `addBinding` deja la ord frozen. 19 CERT/1 INFER. | sí · sonnet |
| 5 | D5 | B202 | Field editors inline kitPx (4): par Wb-Swing (`BGenericFieldEditor`→`BWbFieldEditor.makeFor`, `BSetPointFieldEditor`) ↔ par Ux-web (shim `BSingleton`+`BIJavaScriptWidget`→JS). **Sin interfaz compartida**: paridad por type+agent (B192/B194). Semántica setpoint (dual-path write + gate) en el BINDING (`BSetPointBinding`), no en el FE. Hazard: Ux confía canWrite() client-side. 19 CERT/2 INFER. | sí · sonnet |
| 6 | X3 | B203 | Packs gráficos: `kitPxGraphics`/`kitPxHvac`/`kitPxN4svg` = **BOG palettes sin código** (símbolos pre-bound). Raster (BoundLabel+IBooleanToSimple On/Off, confirma B196) vs vector N4svg (`ui:Picture`+SVG+converters numeric/status ricos). `kitPxBuilding` la excepción con componentes Java tipados (BEquipment/BDamper/BKnob). 14 CERT/2 INFER. | no · inline (gap liviano) |
| S | — | B209 | **SÍNTESIS de cierre**: 4 hilos transversales (bajaux base unificadora type+2agentes; 2 sistemas chart/símbolos; undo=Workbench Command; reutilización sobre kitPx + OEM easyBinding). Remite B198-B208. | no · inline |
| 11 | X4 | B208 | `svgBatik` = **Apache Batik repackaged** (~604 clases) + 5 clases Tridium: `BSvgDecoder`(extends `BImageDecoder`, usa GVTBuilder→BufferedImage para rasterizar SVG) + puente ORD↔Batik-URL (registra protocolo `ord:` en Batik, `ImageTagRegistry`). Rasteriza los SVG de N4svg (B203). Integración de lib externa, no reimplementación. 12 CERT/2 INFER. | no · inline |
| 10 | X6 | B207 | `easyBinding`: módulo **OEM Honeywell** (`com.honeywell.easybinding`) de auto-binding sobre kitPx. Widget único bundlea value/alarm/override+`BBoundLabel`; converter auto por tipo (`BIEbConverter`). 2 familias binding (`BEasy*`/kitPx runtime vs `BEb*`/`BValueBinding` web-preview). Rebind virtual→real (link templates B200). Assets cifrados **AES por license-feature** (OEM branding). License-gated. PARCIALMENTE OBFUSCADO. 17 CERT/7 INFER. | sí · sonnet |
| 9 | D4 | B206 | `commands/` editor-level (14): patrón `Command`/`Artifact` por REMISIÓN a B205. Sustancia nueva: familia Insert(Dynamic/Frozen)/Delete/Rename/NewWidget + wrappers estructurales **AddResponsive**(`BResponsivePane`, responsive PX)/AddBorder(`BBorderPane`) + GotoOrd (navegación) + ApplyPxPropertiesToNewWidgets (aplica PX-props de B200 a widgets nuevos). **Cierra grupo D (D1-D5)**. 12 CERT/2 INFER. | no · inline |
| 8 | D2 | B205 | `studio/` (61 clases): sistema de dibujo del canvas. `BStudio`=5 role-interfaces+State pattern (mouse→tracker). Trackers=máquinas de estado (UnpressedTracker el router de hit-test). Painters=buffer-and-overlay (perf). Artisans=strategy per-shape; PathArtisan=gramática SVG path sobre gx `IPathGeom` (B183). Commands `javax.baja.ui.Command`+Artifact (cierra base D4). Gotchas: anchor estático, ConvertPointTracker stub vacío, Select sin undo. 17 CERT/2 INFER. | sí · sonnet |
| 7 | X5 | B204 | Framework **bajaux**: `Widget` lifecycle template-method (init/load/read/save/destroy sobre jQuery, NO ES6 class) + **spandrel** (virtual-DOM propio: diff shallow por circular refs de BajaScript Complex, focus-preservation "never wipe while typing", DiffQueue 5-buckets) + `WidgetManager`/RequireJS + puente rt→web (`BIJavaScriptWidget`/`JsInfo`/`WbWebWidgetServlet`/`NiagaraEnv` window.niagara.env + receta bundling js→…→webChart→kitPx). Confirma B194 (grep negativo PxMedia/Swing). 24 CERT/2 INFER. | sí · sonnet |

## Grupo D — Subsistemas internos de `pxEditor-wb` (nombrados en B191, no deep-dived)

| Gap | Descripción | Fuente (confirmada 2026-07-06) | Prioridad |
|---|---|---|---|
| ~~D1~~ | ✅ **CUBIERTO (B198)** — `sidebars/` (66 clases): cell-sheet, cell editors, commands undo/redo, árbol de widgets, propsheet/layersheet, binding/ ORD-rewriting. Fuentes preservadas en `sources/decompiled/pxEditor-wb/sidebars/`. | ~~`organized/.../editor/sidebars/`~~ | — |
| ~~D2~~ | ✅ **CUBIERTO (B205)** — `studio/` (61 clases, no 6): BStudio+trackers+painters+artisans+commands. Fuente en `sources/decompiled/pxEditor-wb/studio/`. | ~~`.../editor/studio/`~~ | — |
| ~~D3~~ | ✅ **CUBIERTO (B201)** — `make/` wizard: `BMakeWidget`+selección agent-based+8 estrategias `BMw*`. Fuente en `sources/decompiled/pxEditor-wb/make/`. Descubre: `javax.baja.chart` (chart clásico) como feed futuro. | ~~`.../editor/make/`~~ | — |
| ~~D4~~ | ✅ **CUBIERTO (B206)** — commands editor-level (14): patrón por remisión B205 + responsive/border/goto/apply-px-props. Fuente en `sources/decompiled/pxEditor-wb/commands/`. | ~~`.../editor/commands/`~~ | — |
| ~~D5~~ | ✅ **CUBIERTO (B202)** — 4 field editors inline kitPx (Wb-Swing ↔ Ux-web), paridad por type+agent, semántica en el binding. Fuente en `sources/decompiled/kitPx-fe/`. | ~~`kitPx-wb/.../*FieldEditor.java`~~ | — |

## Grupo X — Módulos PX-adyacentes SIN tocar

| Gap | Descripción | Fuente (confirmada 2026-07-06) | Prioridad |
|---|---|---|---|
| ~~X1~~ | ✅ **CUBIERTO (B199)** — `webChart`: charting bajaux/D3, servlets rt + motor JS + docs oficiales. Fuentes en `sources/decompiled/webChart-{rt,ux}/` + `sources/manuals/webChart-docs/`. | ~~`organized/webChart*`~~ | — |
| ~~X2~~ | ✅ **CUBIERTO (B200)** — `template`+`templateBulk`: sistema de templates (`.ntpl`, configs expuestos, deploy/upgrade, bulk Excel). Graphics tab = PxEditor embebido. Fuentes en `sources/decompiled/template-{rt,wb}/`+`templateBulk/`+`sources/manuals/template-docs/`. `easyBinding` separado → X6. | ~~`organized/{template,templateBulk}*`~~ | — |
| ~~X6~~ | ✅ **CUBIERTO (B207)** — `easyBinding`: OEM Honeywell, auto-binding sobre kitPx, license-gated, assets AES-cifrados. Fuente en `sources/decompiled/easyBinding/`. | ~~`organized/easyBinding/`~~ | — |
| ~~X3~~ | ✅ **CUBIERTO (B203)** — packs gráficos = BOG palettes de símbolos pre-bound (raster vs vector SVG); kitPxBuilding con componentes Java. Fuente en `sources/decompiled/kitPx-graphics-packs/`. | ~~`organized/kitPx{Graphics,Hvac,N4svg}*`~~ | — |
| ~~X4~~ | ✅ **CUBIERTO (B208)** — `svgBatik` = Apache Batik + 5 clases Tridium (BSvgDecoder + puente ORD). Fuente en `sources/decompiled/svgBatik/`. | ~~`organized/svgBatik*`~~ | — |
| ~~X5~~ | ✅ **CUBIERTO (B204)** — framework bajaux: `Widget`+spandrel(virtual-DOM)+bridge rt→web. Confirma B194. Fuente en `sources/decompiled/bajaux/`. | ~~`organized/bajaux*`~~ | — |

## Clasificación (§8)

- **read-only-investigable**: 0 → **STOP (§8, exhausted)**. **requires-execution**: 0. **blocked**: 0. Todos los 11 gaps cerrados.
- **Orden sugerido restante**: — (ninguno; focus cerrado).
- **PRÓXIMO gap**: — (focus STOPPED 11/11).

## Notas

- Este backlog salió de un **coverage-audit honesto** (§13) al cierre de sesión 2026-07-06: el usuario preguntó
  "¿investigaste TODO?" y la respuesta fue NO — el espinazo (7 capas, B179-B197) está, pero estas zonas
  quedaron. Cobertura estimada: misión ~85%, universo de clases PX ~35-40%.
- Reutilizar fuentes ya preservadas (`pxEditor-wb/`, `kitPx-*`, panes, gx) donde aplique; remisión a B179-B197.
- El bloque de síntesis cross-focus B197 mapea el subsistema en 7 capas — leerlo al retomar para orientar.
