# Block 213 — La infra transversal del PX Editor: `com.tridium.px.editor.util` (6) + `property` (1): SelectedWidgets, Reflector, LayerManager, MenuBuilder, Handle, PxPropertyUtil

> Research de la **INFRAESTRUCTURA de `pxEditor-wb`** — foco `px-editor-core`, gap **C4**. Documenta el
> plumbing transversal: `SelectedWidgets` (la impl concreta de `PxEditorSelection`), `Reflector` (bag de
> introspección/reflexión de widgets), `LayerManager` (CRUD de capas), `MenuBuilder` (factory de menús),
> `Handle` (POJO de handle de selección), `PxPropertyUtil` (CRUD de `PxProperty[]` con undo). `EventUtil` se
> remite a B210. **Incluye una corrección §14 a B211** (el `[INFER]` de disparo de `PxSelectionEvent`). NO
> cubre fieldeditors (C5).
>
> Sources (decompilado Vineflower, READ-ONLY):
> `/home/cristian/modules/Prototipos/modulos/organized/pxEditor/pxEditor-wb/vineflower/com/tridium/px/editor/util/`
> + `.../com/tridium/px/editor/property/` (citas `file:line` relativas: `SelectedWidgets.java:22` =
> `.../util/SelectedWidgets.java`; `PxPropertyUtil.java` está en `property/`).
> Method: lectura directa del decompilado (6 clases util + 1 property; `EventUtil` remitido a B210).
> Markers (canónico METHODOLOGY §3): `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción.
>
> Capa infraestructura pxEditor-wb (núcleo). Connects [Block 211] (`PxEditorSelection`/`PxProperty` — y §14
> corrección), [Block 210] (bus: `PxWidgetEvent`/`PxLayerEvent`), [Block 205] (studio/Artisan — geometría de
> handles), [Block 206] (commands — el idiom undo=Command).

---

## 213.1 — `SelectedWidgets`: la impl concreta de `PxEditorSelection` (+ corrección §14 a B211) `[CERT]`

`SelectedWidgets` **es** la implementación concreta que `BPxEditorPane.getSelectedWidgets()` devuelve —
confirma la pregunta abierta de B211:

```java
// SelectedWidgets.java:22
public class SelectedWidgets implements PxEditorSelection {
   // :27
   private List<BWidget> widgets = new ArrayList<>();   // lista, NO array
```

**Corrección §14 al `[INFER]` de [Block 211] §211.4.** B211 infirió que la impl concreta "llama
select/deselect/setWidgets y luego levanta un `PxSelectionEvent` por firePxEvent". Verificado en fuente: los
mutadores `select`/`deselect`/`setWidgets` (`SelectedWidgets.java:66-95`) mutan `widgets` y llaman
`sub.subscribe/unsubscribe`, **pero NO construyen ni disparan `PxSelectionEvent`** — no hay siquiera import de
`PxSelectionEvent` en el archivo. Lo que sí dispara es un `Subscriber` interno (`SelectedWidgets.java:32-57`)
adjunto por-widget-seleccionado, que reacciona a **cambios de PROPIEDAD de los widgets ya seleccionados**, y
emite un `PxWidgetEvent` (no un `PxSelectionEvent`):

```java
// SelectedWidgets.java:53-54
PxWidgetEvent pxEvent = new PxWidgetEvent(event.getId() == 1 ? 0 : 1, w, event.getSlotName(), w.get(event.getSlot().asProperty()));
editor.firePxEvent(pxEvent);
```

Conclusión de la corrección: el disparo de `PxSelectionEvent` en la mutación de selección **NO está aquí**; si
ocurre, vive en el caller `BPxEditor`/`BPxEditorPane` que invoca select/deselect (fuera de estas 7 clases). El
`[INFER]` de B211 §211.4 queda **acotado**: la parte "SelectedWidgets es la impl" se CONFIRMA; la parte
"dispara PxSelectionEvent al mutar" queda sin evidencia aquí y se remite al caller (marcada en B211).

Otros hallazgos de `SelectedWidgets` (el hub de selección):

| Elemento | Detalle | Cita |
|---|---|---|
| `resetHandles()` | **no-op vacío** en esta clase (`public void resetHandles() {}`) — la geometría real NO se recomputa aquí | `SelectedWidgets.java:148-149` |
| `getPointMap()` | motor real de geometría de handles: por cada widget seleccionado llama `Artisan.instance().addHandles(editorPane.getTrackerStudio(), widget, pointMap)` — delega hit-test/handles al **Artisan** de studio (B205), no a `Handle` | `SelectedWidgets.java:161-174` |
| `getHandle(x,y)` / `getHandlesHavingRole(role)` | leen del `PointMap`, casteando entradas a `Handle` (§213.5) filtradas por `role` | `SelectedWidgets.java:107-130` |
| `canSelect(BWidget)` | multi-select sólo entre widgets del mismo padre, y exige `Reflector.isFreeFormPane(parent)` más allá del primero — cruce a `Reflector` (§213.2) | `SelectedWidgets.java:132-146` |
| `envelope()` | copy/paste/drag: `TransferEnvelope` de clones (`editor.cloneWidget`) + `ApplyPxPropertiesToNewWidgets.storePxPropertyInfoOnNewWidget` | `SelectedWidgets.java:191-206` |

## 213.2 — `Reflector`: bag estático de introspección/clasificación/reflexión de widgets `[CERT]`

No es "instanciar por nombre de tipo" a secas — es una **bolsa de utilidades estáticas** de introspección y
reflexión usadas por freeform panes y converters (el "muy usado" de B198):

| Método | Qué hace | Cita |
|---|---|---|
| `isFreeFormPane(BWidget)` | clasifica pane de layout absoluto: hardcodea `BTabbedPane`/`BChartPane`/`report:ReportPane`/`mobile:IMobilePane`, y para el resto **instancia reflexivamente** `getDeclaredConstructor().newInstance()` y chequea `getChildWidgets().length == 0` | `Reflector.java:53-78` |
| `isDroppable` / `isLeaf` | clasificación de drop-target para inserción drag/drop | `Reflector.java:33-51` |
| `converter(String, BBinding[])` | escanea bindings por el primer `BValue` bajo `prop` que sea `instanceof BConverter` — cruce a converters | `Reflector.java:80-89` |
| `cloneFrozen(BComplex)` | clon reflexivo: `getClass().getDeclaredConstructor().newInstance()` + `copyFrom` | `Reflector.java:91-99` |
| `dynamicProperties(BComponent)` | recorre `SlotCursor<Property>` juntando `isDynamic()` | `Reflector.java:101-113` |
| `canvas(BWidget)` | sube por `getParentWidget()` hasta un `BCanvasPane` | `Reflector.java:115-128` |
| `displayName(...)` | arma el label del widget en el árbol/inspector (decora con ORD del binding, texto/imagen, ORD de `BPxInclude`) | `Reflector.java:136-172` |

## 213.3 — `LayerManager`: CRUD de `PxLayer` + `BLayerTag` dinámico + visibilidad + undo `[CERT]`

Gestiona las capas PX (cruce a `PxLayerEvent` de B210 y `pxLayers` de B211). Ctor
`LayerManager(BPxEditor editor, BPxEditorPane editorPane)` (`LayerManager.java:22-25`).

- `insert(int, PxLayer, CommandArtifact)` / `remove(BPxEditorPane, PxLayer)` (`:27-56`): mutan el array y
  llaman `editor.setPxLayers(...)` — **este es el call-site que (por B210) debería disparar `PxLayerEvent`**.
- `remove` devuelve un `RemoveTagsArtifact implements CommandArtifact` (`:274-294`) con undo/redo — mismo
  patrón Command que el resto del editor (hilo transversal undo=Command, B206/B209).
- Membresía de capa = **propiedad dinámica de tipo `BLayerTag`** (sin slot fijo): `getTag`/`getCommonTag`/
  `addTag`/`removeTag`/`stripMissingLayer` (`:74-233`) escanean `SlotCursor<Property>` por `instanceof BLayerTag`.
- Visibilidad: `setLayerStatus(PxLayer, BLayerStatus)` (`:197-207`) → `doAdjustVisibility` (`:235-246`)
  recursivo hace `widget.setVisible(...)` en cada widget tageado.
- `getLayerWidgets`/`doGetWidgets` (`:112-116,261-272`): walk recursivo juntando widgets cuyo
  `BLayerTag.getLayerName()` matchea. `renameLayerTag`/`doRenameLayerTag` (`:118-124,248-259`): rename recursivo.

## 213.4 — `MenuBuilder`: factory estático de `BMenu` desde bog + Command classes `[CERT]`

Factory pura (no guarda estado de menú); todos los comandos son subclases `Command` preexistentes de
`com.tridium.px.editor.{commands,studio.commands}`:

| Método | Arma | Cita |
|---|---|---|
| `newMenu(BPxEditorPane, BTransferWidget)` | el menú "New Widget" leyendo un **bog** `file:!defaults/workbench/newWidgets.bog|bog:|slot:/` y envolviendo recursivo cada hijo (`BFolder`→submenú, `BSeparator`→sep, `BWidget`→`new NewWidget(...)`); fallback hardcodeado Label/Picture/BorderPane si el bog no resuelve (`catch UnresolvedException`) | `MenuBuilder.java:30-79` (fallback `:41`) |
| `alignMenu`/`distributeMenu`/`reorgMenu` | menús de comandos `Align`/`Distribute`/`Reorg` de `studio.commands` | `MenuBuilder.java:81-109` |
| `borderMenu(..., SelectedWidgets)` / `responsiveMenu` | 2-items (`AddBorder`/`RemoveBorder`, `AddResponsive`/`RemoveResponsive`), gating `setEnabled` en `selected.size()>0` y `ClassUtil.all(arr, BBorderPane.class)` — consumidor directo de `SelectedWidgets` (§213.1) | `MenuBuilder.java:111-143` |

## 213.5 — `Handle`: POJO de 4 campos (NO un enum) `[CERT]`

`Handle` es un **POJO fino de 4 campos**, no un enum de posiciones:

```java
// Handle.java:7-18 (clase completa)
public class Handle {
   public Point pnt;          // posición pixel
   public BWidget widget;     // widget dueño
   public MouseCursor cursor; // cursor al hover
   public Object role;        // tag arbitrario
}
```

No hay constantes NW/N/NE/… en esta clase — `role` es un `Object` sin tipo. `[INFER]` La semántica de
dirección de resize la define quien construye los `Handle` — que es `Artisan.addHandles(...)`
(desde `SelectedWidgets.getPointMap()`, §213.1), fuera de estas 7 clases. `Handle` se guarda/filtra vía
`PointMap` keyed por pixel `(x,y)` y se recupera por su `role`.

## 213.6 — `PxPropertyUtil`: insert/remove de `PxProperty[]` con undo `[CERT]`

Espeja el patrón de `LayerManager` pero para propiedades px-level (`PxProperty`/`BWbPxView` de B211):

- `insert(BPxEditor, int, PxProperty, CommandArtifact)` (`PxPropertyUtil.java:16-27`): splice en
  `editor.getPxProperties()`, `editor.setPxProperties(...)`, con undo opcional de un artifact previo.
- `remove(BPxEditor, PxProperty, PxPropertyComponentArray)` (`:29-41`): quita del array y arma+`redo()` un
  `Artifact implements CommandArtifact` (`:43-101`) que además limpia la propiedad de cada
  `PxPropertyComponent`'s `TargetArray` (`ts.removeAll(t)`), descartando componentes sin targets — y puede
  `undo()` re-agregando ambos. Cruce a `com.tridium.ui.px.{PxPropertyComponent, PxPropertyComponentArray,
  Target, TargetArray}` (el lado target-binding de `PxProperty`).
- Mismo idiom `CommandArtifact` redo/undo que `LayerManager.RemoveTagsArtifact` — refuerza undo=Command.

## 213.7 — `EventUtil`: remisión a [Block 210] `[CERT]`

`EventUtil` (reclasificación `getEventType` de 13 constantes + factories `widgetsChanged`/`bindingsChanged` que
colapsan a compound + cruce `BConverter.TYPE`) ya está documentado end-to-end en **[Block 210] §210.6** — sin
sustancia nueva aquí (cierre por remisión de esa clase del gap).

## 213.x — Connections

- **[Block 211]** — §213.1 CONFIRMA que `SelectedWidgets implements PxEditorSelection` (la impl que
  `getSelection()` devuelve) y **corrige §14** su `[INFER]` §211.4: la selección NO dispara `PxSelectionEvent`
  desde aquí (dispara `PxWidgetEvent` en cambios de prop); el disparo, si existe, vive en el caller. También
  usa `PxProperty` (§213.6) y `cloneWidget` (§213.1 envelope).
- **[Block 210]** — `SelectedWidgets` emite `PxWidgetEvent` (§213.1), `LayerManager.setPxLayers` es el
  call-site de `PxLayerEvent` (§213.3), y `EventUtil` se remite a §210.6 (§213.7).
- **[Block 205]** (studio/Artisan) — la geometría de handles NO vive en `Handle` ni en `SelectedWidgets`
  (`resetHandles` es no-op); se delega a `Artisan.addHandles` de studio (§213.1/§213.5).
- **[Block 206]** (commands) — el idiom undo=Command (`CommandArtifact` redo/undo) aparece idéntico en
  `LayerManager.RemoveTagsArtifact` y `PxPropertyUtil.Artifact` (§213.3/§213.6).
- **[Block 198]** — `Reflector` es el helper "muy usado" que aquel bloque nombró; §213.2 lo abre
  (isFreeFormPane reflexivo, converter scan, displayName).
