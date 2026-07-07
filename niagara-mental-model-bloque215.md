# Block 215 — SÍNTESIS del focus `px-editor-core`: 5 hilos transversales de la infraestructura de `pxEditor-wb`

> **Bloque de síntesis** (terminal del focus `px-editor-core`, §8). Consolida los 5 bloques de evidencia
> B210-B214 (la infraestructura interna de `pxEditor-wb` que los focuses previos nombraron-no-abrieron) en 5
> hilos transversales, y los cruza con los focuses `px-editor`/`px-editor-deep` (B191-B209). NO agrega
> evidencia nueva — es un mapa de relaciones sobre lo ya citado; cada hilo referencia los `[CERT]` de origen.
>
> Sources: bloques B210-B214 de este focus (que a su vez citan `pxEditor-wb/vineflower/...`); cruces a
> B191/B198/B202/B204/B205/B206/B209.
> Method: síntesis sobre corpus propio. Markers: `[INFER]` (síntesis/relación) salvo donde se re-cita un
> `[CERT]` de origen. Un ratio alto de `[INFER]` es ESPERADO y sano en un bloque de síntesis (METHODOLOGY §11).
>
> Capa infraestructura pxEditor-wb (cierre). Connects [Block 209] (síntesis px-editor-deep — el nivel de
> arriba), [Block 210]-[Block 214] (los 5 gaps de este focus).

---

## 215.1 — Hilo A: `BPxEditor` es el hub central por donde pasa TODO `[INFER]`

Los 5 gaps convergen en una sola clase: `BPxEditor` no es "una vista más", es el **nodo central** del editor:

- **aloja el bus de eventos** (`listeners`/`firePxEvent`, [Block 210] §210.5);
- **es dueño del controller, la selección (vía pane) y el profile** ([Block 211] §211.1);
- **la creación de widgets** pasa por su controller (`getWidgetInserter`, [Block 212] §212.5) y por su
  `cloneWidget` ([Block 212] §212.4);
- **la util transversal** lo toma como arg (LayerManager/PxPropertyUtil llaman `editor.setPxLayers`/
  `setPxProperties`, [Block 213] §213.3/§213.6);
- **los fieldeditors** sólo lo tocan para lexicon ([Block 214] §214.4).

`[INFER]` El diseño es un **hub-and-spoke**: cada subsistema recibe una referencia a `BPxEditor` en su
constructor y colabora a través de él, en vez de conocerse entre sí. Esto explica por qué los focuses previos
lo nombraban constantemente sin poder "cerrarlo": era el centro no documentado del grafo.

## 215.2 — Hilo B: el modelo de selección es el nexo, y su eventing se aclaró por §14 `[INFER]`

La selección atraviesa tres capas y fue la fuente de la única corrección §14 del focus:

- **contrato**: `PxEditorSelection` (interfaz `BWidget[]` CRUD, [Block 211] §211.4);
- **impl**: `SelectedWidgets` (`implements PxEditorSelection`, [Block 213] §213.1);
- **geometría**: los handles NO viven en la selección (`resetHandles` no-op) sino en `Artisan` de studio
  ([Block 213] §213.1, cruce [Block 205]).

**Corrección §14** (B213→B211): la selección NO dispara `PxSelectionEvent` al mutar — dispara `PxWidgetEvent`
en cambios de propiedad de widgets ya seleccionados; el `PxSelectionEvent`, si se emite, es en el caller. `[INFER]`
El nexo canvas↔sidebars↔árbol que B198/B209 describían se apoya en este objeto compartido, no en el eventing
directo de selección.

## 215.3 — Hilo C: `@AgentOn` es EL mecanismo de extensión de todo el editor `[INFER]`

El patrón agent de Niagara aparece como el punto de plug-in uniforme en 3 lugares del focus:

| Uso | `@AgentOn(type)` | Qué enchufa | Cita |
|---|---|---|---|
| `BPxEditor` | `baja:Component`, `file:PxFile` | la vista-editor sobre componentes/`.px` | [Block 211] §211.1 |
| `BPxProfile` | `workbench:WbProfile` | el **hook OEM/media** (Wb/Hx/Mobile de B194) que customiza sidebars/menús/toolbar | [Block 211] §211.6 |
| 3 fieldeditors | `converters:IEnumToSimple`/`INumericToSimple`/`IStatusToSimple` | el FE correcto por tipo de converter | [Block 214] §214.2 |

`[INFER]` La extensibilidad del editor (OEM, perfiles, nuevos converters) NO usa un registry ad-hoc: reusa el
`@AgentOn` de la plataforma. Esto conecta con el hilo "alto nivel sobre kitPx/plataforma" de [Block 209]: el
editor se apoya en mecanismos de la plataforma en vez de inventar los suyos.

## 215.4 — Hilo D: `undo=Command` confirmado en la capa de infra `[INFER]`

El hilo transversal undo=Command (identificado en [Block 206]/[Block 209]) se confirma en la infra:

- `CommandArtifact` con `redo()`/`undo()` idéntico en `LayerManager.RemoveTagsArtifact` y
  `PxPropertyUtil.Artifact` ([Block 213] §213.3/§213.6);
- `WidgetInserter` transporta un `auxCommand` (`ApplyPxPropertiesToNewWidgets`) que el controller ejecuta
  post-inserción ([Block 212] §212.2/§212.4);
- `MenuBuilder` arma menús enteramente de subclases `Command` preexistentes ([Block 213] §213.4).

`[INFER]` Toda mutación estructural del editor (mover a capa, cambiar propiedad px, insertar widget) se modela
como Command reversible — no como edición imperativa directa. Es el mismo patrón que B206 vio en los commands
editor-level, ahora visto desde el lado de la infra que los produce.

## 215.5 — Hilo E: reutilización de bases bajaux genéricas (no reinvención) `[INFER]`

El editor extiende bases bajaux/Workbench en vez de crear las suyas — confirma el hilo "bajaux base
unificadora" de [Block 204]/[Block 209]:

| Clase del editor | Base genérica reutilizada | Cita |
|---|---|---|
| `BPxEditor` | `BWbPxView` → `BWbView` (parseo PX en la superclase) | [Block 211] §211.1 |
| `BPxSideBar` | `BPane` | [Block 211] §211.2 |
| 3 fieldeditors | `BWbFieldEditor` (misma base que kitPx-fe de B202) | [Block 214] §214.1 |
| celdas de FE | `BWbCellEditor.makeFor` (dispatch estándar por BSimple) | [Block 213]/[Block 214] |

`[INFER]` La infra de `pxEditor-wb` es **delgada sobre bajaux**: aporta el pegamento específico del editor
(bus PxEvent, selección, factories, capas) pero delega vista/pane/field-editor/cell-editor a la plataforma.
Esto cierra el arco que B209 abrió: el subsistema PX completo (menú B179-190, editor B191-196, deep B198-209,
core B210-214) es una capa de dominio-específico montada sobre el framework bajaux genérico.

## 215.x — Connections

- **[Block 209]** (síntesis px-editor-deep) — B215 es el nivel de abajo de aquella síntesis: donde B209
  consolidó la capa herramienta/render, B215 consolida el sistema nervioso interno. Los hilos "bajaux
  unificador", "undo=Command" y "alto nivel sobre plataforma" de B209 se re-confirman aquí desde la infra.
- **[Block 210]-[Block 214]** — los 5 bloques de evidencia que esta síntesis mapea (event bus, API base,
  factory, util, fieldeditors).
- **[Block 197]** (síntesis cross-focus px del 2026-07-06) — con B215, el subsistema PX de Niagara N4 queda
  documentado end-to-end: 4 focuses cerrados (px-menu, px-editor, px-editor-deep, px-editor-core), 2 síntesis
  de focus (B209, B215) + 1 cross-focus (B197).
