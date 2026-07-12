# RESEARCH-STATE — Focus `px-editor-core` (STOPPED 5/5 — B210–B214 + síntesis B215)

> Focus **STOPPED 5/5** (2026-07-06, B210-B214 evidencia + B215 síntesis). Capturó la **infraestructura de `pxEditor-wb`** que los focuses previos
> (px-menu B179-B190, px-editor B191-B196, px-editor-deep B198-B209) NOMBRARON repetidamente pero nunca abrieron:
> el bus de eventos, la API pública base, la factory de widgets, y la infra util. Registrado 2026-07-06 al cerrar
> sesión. Numeración global de bloques (desde **B210**). Corpus en Español (técnico EN).
> Engram topic key: `research/niagara/px-editor-core/{gaps,progress}`.
> Arranque: `/research-sdd niagara-research px-editor-core new` (o `continue` una vez bootstrapeado).
<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 233
gaps_closed: 0
known_gaps: 0
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->


## Ángulo declarado (§b2)

Abrir las clases de INFRAESTRUCTURA de `pxEditor-wb` — las abstracciones base y el plumbing que todo el resto del
editor usa pero que ningún bloque documentó a fondo. Es un focus de "cerrar el núcleo": el flujo funcional ya está
(B198-B209); esto es el sistema nervioso y las bases. Prioridad del usuario: **C1 (event bus) y C2 (API base)** primero.

## Cobertura

**5 / 5 gaps** (C1→B210, C2→B211, C3→B212, C4→B213, C5→B214) — **FOCUS COMPLETO**. Síntesis en B215. ~39
clases (conteo real `find`+`wc`, un solo pipeline vineflower — lección retro 2026-07-06). B213 disparó
**corrección §14 a B211 §211.4** (SelectedWidgets NO dispara PxSelectionEvent).

## Backlog

| Gap | Descripción | Fuente (confirmada 2026-07-06) | Prioridad |
|---|---|---|---|
| ~~C1~~ | ✅ **CERRADO B210** — **`javax.baja.px.editor.event` — 12 clases**: el **PxEvent bus** documentado end-to-end: `PxEvent` (abstract, 9 categorías) + `PxListener` (SAM `pxEvent`) + taxonomía (2 jerarquías, `PxComponentEvent` intermedia) + compound (batch multi-widget) + dispatch en `BPxEditor` (registro/fire, no en el paquete `event`; sidebars oyentes+emisores) + `EventUtil` (reclasificación fina 13-const + factories compound; cruce `BConverter`). | `organized/pxEditor/pxEditor-wb/vineflower/javax/baja/px/editor/event/` | ~~ALTA~~ |
| ~~C2~~ | ✅ **CERRADO B211** — **`javax.baja.px.editor` (root) — 7 clases**: `BPxEditor` (extiende `BWbPxView`, agent sobre Component/PxFile; owns controller/selección-vía-pane/profile; clona por PxEncoder/Decoder; bus→remite B210), `BPxSideBar` (base sidebar = `BPane` abstracto, icono+desc+editor+layout template), `PxEditorController` (clase plana: cell-editor + registro 8 WidgetFactory/WidgetInserter + hooks transfer), `PxEditorSelection` (interfaz `BWidget[]` CRUD), `BDrawingTool` (`BFrozenEnum` 5 estados), `BPxProfile` (hook OEM `BIAgent`@WbProfile: getSideBars/getViewMenus/toolbar), `BIPxTransferWidget` (contrato popup). | `.../vineflower/javax/baja/px/editor/*.java` | ~~ALTA~~ |
| ~~C3~~ | ✅ **CERRADO B212** — **`javax.baja.px.editor.factory` — 10 clases**: `WidgetFactory` (base abstracta, `make(BObject[])→WidgetInserter` + `canConvert` por tipo, sin prioridad), `WidgetInserter` (**DTO-resultado**, NO insertador: widgets[]+auxCommand+columnCount; la colocación real vive en el controller), 8 factories: PxFile→BPxInclude(decode), JsFile→BWebWidget, ImageFile→BLabel(img), NavNode→abre wizard `BMakeWidget`+excluye paleta, Label/Picture→ImageCopying, WidgetCloning→`editor.cloneWidget`+ApplyPxProps (NO PxEncoder), ImageCopying→clona+localiza imágenes al ord space. Dispatch first-match en controller (B211). | `.../vineflower/javax/baja/px/editor/factory/` | ~~media~~ |
| ~~C4~~ | ✅ **CERRADO B213** — **`com.tridium.px.editor.util` (6) + `property` (1)**: `SelectedWidgets` (impl concreta de `PxEditorSelection`, List<BWidget>, dispara PxWidgetEvent NO PxSelectionEvent, handles vía Artisan, canSelect vía Reflector), `Reflector` (bag reflexivo: isFreeFormPane instancia-y-chequea, converter scan BConverter, cloneFrozen, displayName), `LayerManager` (CRUD PxLayer + BLayerTag dinámico + visibilidad + undo Artifact), `MenuBuilder` (factory estático de BMenu desde bog newWidgets + Command classes), `Handle` (POJO 4-campos, geometría vía Artisan), `PxPropertyUtil` (CRUD PxProperty[] + undo Artifact). `EventUtil`→remisión B210. | `.../vineflower/com/tridium/px/editor/{util,property}/` | ~~media~~ |
| ~~C5~~ | ✅ **CERRADO B214** — **`com.tridium.px.editor.fieldeditors` — 3 clases**: `BIEnumToSimpleFE`/`BINumericToSimpleFE`/`BIStatusToSimpleFE` — **clases CONCRETAS `BWbFieldEditor`** (el `BI...` NO es interfaz; refiere al converter-interface); registran por `@AgentOn(converters:I…Simple)`; overridan doLoadValue/doSaveValue; editan el PAYLOAD del converter (BEnumToSimpleMap/BNumericToSimpleMap/9 BSimple de status); delegan celdas a `BWbCellEditor.makeFor`. Capa distinta de BConverterCE (B198, elige tipo) y EventUtil (B210, clasifica eventos). | `.../vineflower/com/tridium/px/editor/fieldeditors/` | ~~baja~~ |

## Clasificación (§8)

- **read-only-investigable**: **0 → STOP** (los 5 gaps cerrados). **requires-execution**: 0. **blocked**: 0.
- No surgieron gaps NUEVOS fuera de C1-C5 durante todo el focus (la infra es autocontenida; sus dependencias
  ya estaban en el backlog o en focuses previos).

## Historia de iteración

| Iter | Bloque | Gap | Resultado | delegado? · modelo | Ratio [INFER]/[CERT] |
|---|---|---|---|---|---|
| 1 | B210 | C1 | cerrado por NUEVA investigación (12 clases event + EventUtil + dispatch BPxEditor) | sí · sonnet | 2/7 = 0.29 (evidencia, sano) |
| 2 | B211 | C2 | cerrado por NUEVA investigación (7 clases root; bus remitido a B210) | sí · sonnet | 2/8 = 0.25 (evidencia, sano) |
| 3 | B212 | C3 | cerrado por NUEVA investigación (10 clases factory; WidgetInserter=DTO, no insertador) | sí · sonnet | 2/6 = 0.33 (evidencia, sano) |
| 4 | B213 | C4 | cerrado por NUEVA investigación (6 util + 1 property; EventUtil remitido B210) + §14 corrige B211 | sí · sonnet | 6/8=0.75 **inflado** (5 de 6 [INFER] son meta-refs al marker de B211 en la corrección §14; deducción nueva real ≈1) |
| 5 | B214 | C5 | cerrado por NUEVA investigación (3 fieldeditors; concretos BWbFieldEditor no interfaces) | sí · sonnet | 3/5 = 0.60 (evidencia; gap chico, subsistema converter ya cubierto B198/202/210) |
| 6 | B215 | — | SÍNTESIS cierre focus (5 hilos transversales de la infra pxEditor-wb) | no · inline | (síntesis) |

## Notas

- Este backlog salió de un **coverage-check honesto** al cierre de sesión 2026-07-06 (el usuario preguntó "¿qué más
  queda del PX editor?"): mapeo real de los 22 paquetes de `pxEditor-wb` (203 clases) vs lo cubierto en B198-B206.
  Cubierto: studio/*, make/, sidebars/*, commands/, core (B191). SIN abrir: estas ~39 clases de infra.
- **NO son módulos vecinos** (eso fue el grupo X de px-editor-deep); es el NÚCLEO interno que quedó nombrado-no-abierto.
- **Feeds separados** (NO parte de este focus, serían otro): `javax.baja.chart` (chart clásico completo, B201 lo rozó) y
  `kitPxBuilding` (componentes de equipo tipados, B203). Esos son módulos distintos de `pxEditor-wb`.
- Reutilizar fuentes ya preservadas donde aplique; remisión a B198-B209. Leer B209 (síntesis) al retomar para orientar.
