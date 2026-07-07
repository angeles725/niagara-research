# RESEARCH-STATE — Focus `px-editor-core` (PLANIFICADO — arranca próxima sesión)

> Focus **PLANIFICADO** (no arrancado). Captura la **infraestructura de `pxEditor-wb`** que los focuses previos
> (px-menu B179-B190, px-editor B191-B196, px-editor-deep B198-B209) NOMBRARON repetidamente pero nunca abrieron:
> el bus de eventos, la API pública base, la factory de widgets, y la infra util. Registrado 2026-07-06 al cerrar
> sesión. Numeración global de bloques (desde **B210**). Corpus en Español (técnico EN).
> Engram topic key: `research/niagara/px-editor-core/{gaps,progress}`.
> Arranque: `/research-sdd niagara-research px-editor-core new` (o `continue` una vez bootstrapeado).

## Ángulo declarado (§b2)

Abrir las clases de INFRAESTRUCTURA de `pxEditor-wb` — las abstracciones base y el plumbing que todo el resto del
editor usa pero que ningún bloque documentó a fondo. Es un focus de "cerrar el núcleo": el flujo funcional ya está
(B198-B209); esto es el sistema nervioso y las bases. Prioridad del usuario: **C1 (event bus) y C2 (API base)** primero.

## Cobertura

**0 / 5 gaps** (PLANIFICADO, sin arrancar). ~39 clases (conteo real `find`+`wc`, un solo pipeline vineflower —
lección retro 2026-07-06).

## Backlog

| Gap | Descripción | Fuente (confirmada 2026-07-06) | Prioridad |
|---|---|---|---|
| C1 | **`javax.baja.px.editor.event` — 12 clases**: el **PxEvent bus** — `PxEvent`+`PxListener` + subtipos (`PxWidgetEvent`, `PxPropertyEvent`, `PxBindingEvent`, `PxLayerEvent`, `PxSelectionEvent`, `PxComponentEvent`, `PxCompoundBindingEvent`, `PxCompoundWidgetEvent`, `PxEditorEvent`, `PxUserDefinedEvent`). El pub/sub que sincroniza sidebars↔canvas↔árbol; citado en B198/B201/B205/B206 sin abrir. | `organized/pxEditor/pxEditor-wb/vineflower/javax/baja/px/editor/event/` | **ALTA** |
| C2 | **`javax.baja.px.editor` (root) — 7 clases**: la API pública base — `BPxEditor`, `BPxSideBar` (base de TODO sidebar), `PxEditorController`, `PxEditorSelection`, `BDrawingTool`, `BPxProfile`, `BIPxTransferWidget`. Las abstracciones que todo extiende. | `.../vineflower/javax/baja/px/editor/*.java` | **ALTA** |
| C3 | **`javax.baja.px.editor.factory` — 10 clases**: la creación/inserción de widgets — `WidgetInserter` (materializa el resultado del wizard, B201 lo dejó fuera de scope), `WidgetFactory`, `WidgetCloningFactory`, `ImageCopyingWidgetFactory`, `LabelFactory`/`PictureFactory`/`ImageFileFactory`/`JsFileFactory`/`PxFileFactory`/`NavNodeFactory`. | `.../vineflower/javax/baja/px/editor/factory/` | media |
| C4 | **`com.tridium.px.editor.util` (6) + `property` (1)**: infra transversal — `EventUtil` (crea los PxEvents, visto en B198), `Reflector` (freeform panes/converters, muy usado B198), `SelectedWidgets` (el modelo de selección compartido canvas/árbol), `LayerManager`, `MenuBuilder`, `Handle`, `PxPropertyUtil`. | `.../vineflower/com/tridium/px/editor/{util,property}/` | media |
| C5 | **`com.tridium.px.editor.fieldeditors` — 3 clases**: editores de CONVERTERS (`BIEnumToSimpleFE`, `BINumericToSimpleFE`, `BIStatusToSimpleFE`) — distintos de los kitPx-fe de B202; cómo se edita un converter en la celda (relaciona con `BConverterCE` de B198). | `.../vineflower/com/tridium/px/editor/fieldeditors/` | baja |

## Clasificación (§8)

- **read-only-investigable**: 5 (todos con fuente confirmada alcanzable — chequeo de existencia 2026-07-06).
- **requires-execution**: 0. **blocked**: 0.
- **Orden sugerido**: C1 (event bus) → C2 (API base) → C3 (factory) → C4 (util) → C5 (fieldeditors).

## Notas

- Este backlog salió de un **coverage-check honesto** al cierre de sesión 2026-07-06 (el usuario preguntó "¿qué más
  queda del PX editor?"): mapeo real de los 22 paquetes de `pxEditor-wb` (203 clases) vs lo cubierto en B198-B206.
  Cubierto: studio/*, make/, sidebars/*, commands/, core (B191). SIN abrir: estas ~39 clases de infra.
- **NO son módulos vecinos** (eso fue el grupo X de px-editor-deep); es el NÚCLEO interno que quedó nombrado-no-abierto.
- **Feeds separados** (NO parte de este focus, serían otro): `javax.baja.chart` (chart clásico completo, B201 lo rozó) y
  `kitPxBuilding` (componentes de equipo tipados, B203). Esos son módulos distintos de `pxEditor-wb`.
- Reutilizar fuentes ya preservadas donde aplique; remisión a B198-B209. Leer B209 (síntesis) al retomar para orientar.
