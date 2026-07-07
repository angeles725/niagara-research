# RESEARCH-STATE — Focus `px-editor-core` (ACTIVO — B210–)

> Focus **ACTIVO** (arrancado 2026-07-06, B210). Captura la **infraestructura de `pxEditor-wb`** que los focuses previos
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

**2 / 5 gaps** (C1→B210, C2→B211). ~39 clases (conteo real `find`+`wc`, un solo pipeline vineflower —
lección retro 2026-07-06).

## Backlog

| Gap | Descripción | Fuente (confirmada 2026-07-06) | Prioridad |
|---|---|---|---|
| ~~C1~~ | ✅ **CERRADO B210** — **`javax.baja.px.editor.event` — 12 clases**: el **PxEvent bus** documentado end-to-end: `PxEvent` (abstract, 9 categorías) + `PxListener` (SAM `pxEvent`) + taxonomía (2 jerarquías, `PxComponentEvent` intermedia) + compound (batch multi-widget) + dispatch en `BPxEditor` (registro/fire, no en el paquete `event`; sidebars oyentes+emisores) + `EventUtil` (reclasificación fina 13-const + factories compound; cruce `BConverter`). | `organized/pxEditor/pxEditor-wb/vineflower/javax/baja/px/editor/event/` | ~~ALTA~~ |
| ~~C2~~ | ✅ **CERRADO B211** — **`javax.baja.px.editor` (root) — 7 clases**: `BPxEditor` (extiende `BWbPxView`, agent sobre Component/PxFile; owns controller/selección-vía-pane/profile; clona por PxEncoder/Decoder; bus→remite B210), `BPxSideBar` (base sidebar = `BPane` abstracto, icono+desc+editor+layout template), `PxEditorController` (clase plana: cell-editor + registro 8 WidgetFactory/WidgetInserter + hooks transfer), `PxEditorSelection` (interfaz `BWidget[]` CRUD), `BDrawingTool` (`BFrozenEnum` 5 estados), `BPxProfile` (hook OEM `BIAgent`@WbProfile: getSideBars/getViewMenus/toolbar), `BIPxTransferWidget` (contrato popup). | `.../vineflower/javax/baja/px/editor/*.java` | ~~ALTA~~ |
| C3 | **`javax.baja.px.editor.factory` — 10 clases**: la creación/inserción de widgets — `WidgetInserter` (materializa el resultado del wizard, B201 lo dejó fuera de scope), `WidgetFactory`, `WidgetCloningFactory`, `ImageCopyingWidgetFactory`, `LabelFactory`/`PictureFactory`/`ImageFileFactory`/`JsFileFactory`/`PxFileFactory`/`NavNodeFactory`. | `.../vineflower/javax/baja/px/editor/factory/` | media |
| C4 | **`com.tridium.px.editor.util` (6) + `property` (1)**: infra transversal — `EventUtil` (crea los PxEvents, visto en B198), `Reflector` (freeform panes/converters, muy usado B198), `SelectedWidgets` (el modelo de selección compartido canvas/árbol), `LayerManager`, `MenuBuilder`, `Handle`, `PxPropertyUtil`. | `.../vineflower/com/tridium/px/editor/{util,property}/` | media |
| C5 | **`com.tridium.px.editor.fieldeditors` — 3 clases**: editores de CONVERTERS (`BIEnumToSimpleFE`, `BINumericToSimpleFE`, `BIStatusToSimpleFE`) — distintos de los kitPx-fe de B202; cómo se edita un converter en la celda (relaciona con `BConverterCE` de B198). | `.../vineflower/com/tridium/px/editor/fieldeditors/` | baja |

## Clasificación (§8)

- **read-only-investigable**: 3 (C3-C5; todos con fuente confirmada alcanzable — chequeo 2026-07-06).
- **requires-execution**: 0. **blocked**: 0.
- **Orden sugerido restante**: C3 (factory) → C4 (util) → C5 (fieldeditors).
- **Pre-respuestas parciales** (cerrarán en parte por remisión): C3 (factory) ya visto parcial en B211
  (`PxEditorController` registra 8 `WidgetFactory` + `getWidgetInserter`, `PxEditorController.java:79-115`);
  C4 verá `EventUtil` ya abierto en B210 (reclasif + factories); C5 verá el cruce `EventUtil`↔`BConverter.TYPE`.
  No surgieron gaps NUEVOS fuera de C1-C5.

## Historia de iteración

| Iter | Bloque | Gap | Resultado | delegado? · modelo | Ratio [INFER]/[CERT] |
|---|---|---|---|---|---|
| 1 | B210 | C1 | cerrado por NUEVA investigación (12 clases event + EventUtil + dispatch BPxEditor) | sí · sonnet | 2/7 = 0.29 (evidencia, sano) |
| 2 | B211 | C2 | cerrado por NUEVA investigación (7 clases root; bus remitido a B210) | sí · sonnet | 2/8 = 0.25 (evidencia, sano) |

## Notas

- Este backlog salió de un **coverage-check honesto** al cierre de sesión 2026-07-06 (el usuario preguntó "¿qué más
  queda del PX editor?"): mapeo real de los 22 paquetes de `pxEditor-wb` (203 clases) vs lo cubierto en B198-B206.
  Cubierto: studio/*, make/, sidebars/*, commands/, core (B191). SIN abrir: estas ~39 clases de infra.
- **NO son módulos vecinos** (eso fue el grupo X de px-editor-deep); es el NÚCLEO interno que quedó nombrado-no-abierto.
- **Feeds separados** (NO parte de este focus, serían otro): `javax.baja.chart` (chart clásico completo, B201 lo rozó) y
  `kitPxBuilding` (componentes de equipo tipados, B203). Esos son módulos distintos de `pxEditor-wb`.
- Reutilizar fuentes ya preservadas donde aplique; remisión a B198-B209. Leer B209 (síntesis) al retomar para orientar.
