# RESEARCH-STATE — Focus `px-editor-deep` (ACTIVO — en curso)

> Focus **ACTIVO** (arrancado 2026-07-06). Captura lo que los focuses `px-menu` (B179-B190) y `px-editor`
> (B191-B196) NO cubrieron a fondo. Numeración global de bloques (desde B198). Corpus en Español (técnico EN).
> Engram topic key: `research/niagara/px-editor-deep/{gaps,progress}`.
> Continuar: `/research-sdd niagara-research px-editor-deep continue`.

## Ángulo declarado (§b2)

Profundizar donde `px-editor` solo NOMBRÓ, y abrir los módulos PX-adyacentes SIN tocar. Dos naturalezas
(prefijos distintos): **D** = subsistemas internos de `pxEditor-wb`; **X** = módulos vecinos.
Prioridad del usuario: **D1 (`sidebars/`, el property/cell-sheet — corazón operativo)** primero, luego
**X1 (`webChart`) y X2 (templates)**.

## Cobertura

**2 / 10 gaps** (ratio 0.20). Read-only-investigable: 8 restantes · requires-execution: 0 · blocked: 0.

## Historial de iteraciones

| # | Gap | Bloque | Hallazgo | Delegado? · tier |
|---|---|---|---|---|
| 1 | D1 | B198 | `sidebars/`: `BPxCellSheet` (property/binding editor) + 3-way `newCellEditor` + los CE del Workbench + commands→`javax.baja.ui.Command` (undo/redo) + árbol `PxTreeModel`/`PxTreeSelection` sync + propsheet/layersheet + binding/ ORD-rewriting. 36 CERT/3 INFER. | sí · sonnet |
| 2 | X1 | B199 | `webChart`: charting = **bajaux puro** (D3/JS), Java `ux` solo bridge `JsInfo` (confirma B194). Capa `rt` = servlets query(data/schedule)/file + `.chart` JSON + RPC. Motor JS: `seriesFactory` (4 series: Servlet/Schedule/Point/External), scales Time/Value D3, sampling auto 2500. Settings sobre `webEditors`. 27 CERT/9 CERT-doc/2 INFER. | sí · sonnet |

## Grupo D — Subsistemas internos de `pxEditor-wb` (nombrados en B191, no deep-dived)

| Gap | Descripción | Fuente (confirmada 2026-07-06) | Prioridad |
|---|---|---|---|
| ~~D1~~ | ✅ **CUBIERTO (B198)** — `sidebars/` (66 clases): cell-sheet, cell editors, commands undo/redo, árbol de widgets, propsheet/layersheet, binding/ ORD-rewriting. Fuentes preservadas en `sources/decompiled/pxEditor-wb/sidebars/`. | ~~`organized/.../editor/sidebars/`~~ | — |
| D2 | **`studio/` — 6**: `BStudio` + trackers/painters, las herramientas de dibujo (`NormalTool`/`GeometryTool`), edición de geometría, dragOver/drop. | `.../com/tridium/px/editor/studio/` | media |
| D3 | **`make/` — 16 estrategias del wizard**: `BMwBoundLabel`, `BMwChart`, `BMwTimePlot`, `BMwWorkbenchView`, `BMwPxInclude`… (solo `BMwFromPalette` se documentó en B191). | `.../com/tridium/px/editor/make/` | media |
| D4 | **`commands/` — 36**: `MoveWidget`/`MorphWidget`/`Align`/`Reorg`/`Delete` + el undo/redo delegado al `Command`/`CommandArtifact` del Workbench. | `.../com/tridium/px/editor/studio/commands/` + `commands/` | baja |
| D5 | **Field editors**: `BGenericFieldEditor`, `BSetPointFieldEditor` + variantes Ux (`BUxGenericFieldEditor`/`BUxSetPointFieldEditor`) — cómo funcionan los editores inline. | `organized/kitPx/kitPx-wb/vineflower/.../*FieldEditor.java` | media |

## Grupo X — Módulos PX-adyacentes SIN tocar

| Gap | Descripción | Fuente (confirmada 2026-07-06) | Prioridad |
|---|---|---|---|
| ~~X1~~ | ✅ **CUBIERTO (B199)** — `webChart`: charting bajaux/D3, servlets rt + motor JS + docs oficiales. Fuentes en `sources/decompiled/webChart-{rt,ux}/` + `sources/manuals/webChart-docs/`. | ~~`organized/webChart*`~~ | — |
| X2 | **`template`/`templateBulk`/`easyBinding`**: el sistema de templates PX (bloque36 lo rozó). | `organized/{template,templateBulk,easyBinding}*` | **ALTA** |
| X3 | **`kitPxGraphics`/`kitPxHvac`/`kitPxN4svg`**: packs de widgets gráficos (HVAC, SVG). | `organized/kitPx{Graphics,Hvac,N4svg}*` | media |
| X4 | **`svgBatik`**: el motor SVG (B196 lo mencionó sin abrir) — cómo se renderiza un SVG animado en PX. | `organized/svgBatik*` | baja |
| X5 | **`bajaux`**: el render web moderno a fondo — el pipeline JS/HTML5 (`BUx*` codegen) que B194 dijo "no usa PxMedia" pero NO documentó. | `organized/bajaux*` | media |

## Clasificación (§8)

- **read-only-investigable**: 8 (D2, D3, D4, D5, X2-X5 — fuentes confirmadas 2026-07-06). **requires-execution**: 0. **blocked**: 0.
- **Orden sugerido restante**: **X2 (templates)** → D3/D5 → X3/X5 → D2/D4 → X4.
- **PRÓXIMO gap**: **X2 — `template`/`templateBulk`/`easyBinding`** (el sistema de templates PX, prioridad ALTA; B36 lo rozó).

## Notas

- Este backlog salió de un **coverage-audit honesto** (§13) al cierre de sesión 2026-07-06: el usuario preguntó
  "¿investigaste TODO?" y la respuesta fue NO — el espinazo (7 capas, B179-B197) está, pero estas zonas
  quedaron. Cobertura estimada: misión ~85%, universo de clases PX ~35-40%.
- Reutilizar fuentes ya preservadas (`pxEditor-wb/`, `kitPx-*`, panes, gx) donde aplique; remisión a B179-B197.
- El bloque de síntesis cross-focus B197 mapea el subsistema en 7 capas — leerlo al retomar para orientar.
