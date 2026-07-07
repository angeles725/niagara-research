# Bloque 205 — `studio/`: el sistema de dibujo del canvas (Studio + trackers + painters + artisans)

> Research del focus **`px-editor-deep`** (gap D2): el paquete `studio/` (61 clases) — el sistema de dibujo y
> edición geométrica del canvas PX. Documenta la arquitectura Studio (role-interfaces + State pattern), los
> **trackers** (máquinas de estado de mouse), los **painters** (feedback buffer-and-overlay), los **artisans**
> (construcción de geometría, con el modelo de path estilo-SVG), y los **geometry commands** (que cierran D4 por
> remisión). El backlog subestimó el tamaño (decía 6; son 61). NO cubre los commands de nivel editor fuera de
> `studio/` (Delete/Insert/Rename — resto de D4).
>
> Sources (preservados §5): `sources/decompiled/pxEditor-wb/studio/` — 61 `.java` (Vineflower). Barrido delegado
> (sonnet, 61/61 leídas) 2026-07-06; 7 citas load-bearing token-checked literal. Method: lectura READ-ONLY del
> decompilado. Markers (§3): `[CERT]` `file:line` · `[INFER]`. Tipo: EVIDENCE block. Citas relativas a `studio/`.
>
> Capa PX (herramienta / dibujo). Connects [Block 191] (BPxEditorPane), [Block 198] (undo=Command), [Block 201]
> (make wizard/commands), [Block 183] (gx geometry IPathGeom), [Block 197] (síntesis).

---

## 205.1 — La arquitectura Studio: 5 role-interfaces + State pattern `[CERT]`

**No hay composición de sub-studios.** `BStudio` es una clase concreta única; `RootStudio`/`CommandStudio`/
`PainterStudio`/`TrackerStudio`/`TreeStudio` son **interfaces marker/rol** (Interface Segregation), y `BStudio` las
implementa TODAS:

```java
// studio/BStudio.java:65
public class BStudio extends BTransferWidget implements BIPxTransferWidget,
   TreeStudio, CommandStudio, TrackerStudio, RootStudio, PainterStudio {
```

Cada interfaz es un slice angosto de capacidad para un colaborador distinto: `TrackerStudio` expone solo hit-test/
coordenadas/cursor/swap-painter (lo que un `Tracker` necesita), `PainterStudio` solo los métodos de buffer/paint. Así
trackers/painters/commands NO ven un God-object del editor pane, solo su slice. `[CERT]` `[INFER: role-interface design]`

`BStudio` **ES el canvas** — un `BTransferWidget` embebido en `BPxEditorPane`, con la máquina de estado mutable
(`Artisan artisan`, `Painter painter`, `Tracker tracker`, `SelectedWidgets selected`). El flujo de mouse es **State
pattern**: cada callback reasigna `this.tracker` a lo que devuelve el handler del tracker actual:

```java
// studio/BStudio.java:133-144
public void mousePressed(BMouseEvent event)  { this.requestFocus(); this.tracker = this.tracker.mousePressed(event); }
public void mouseDragged(BMouseEvent event)  { this.tracker = this.tracker.mouseDragged(event); }
public void mouseReleased(BMouseEvent event) { this.tracker = this.tracker.mouseReleased(event); }
```

El paint delega igual (`paint(g)` → `this.painter.doPaint(g)`). Estado inicial: `tracker = new UnpressedTracker(...)`,
`painter = new DefaultPainter(this)`. `[CERT]`

## 205.2 — Trackers: máquinas de estado de mouse `[CERT]`

**Contrato base** (`trackers/Tracker.java`): abstract, cada handler (`mouse*`/`key*`) tiene un **default no-op que
devuelve `this`**. Un tracker "transiciona" devolviendo una instancia DISTINTA de Tracker desde un handler; `BStudio` la
instala. **Gotcha** — el anchor de drag-threshold es **estático** compartido entre TODOS los trackers
(`static double anchorX/anchorY`, `trackers/Tracker.java:15-16`; `moved()` = delta > 3px): safe porque solo una cadena de
tracker está viva a la vez (UI single-thread), pero filtra estado entre clases no relacionadas. `[CERT]`

**El router de hit-test vive en `UnpressedTracker`** (el estado idle), no en un dispatcher aparte. `mousePressed`
branchea por click-count/botón; `handleLeftClick` primero chequea `SelectedWidgets.getHandle(x,y)` (hit de handle de
resize → `Artisan.makeHandleTracker`), si no hace hit-test del stack de widgets y branchea por tipo:

| Widget bajo el click | Tracker resultante |
|---|---|
| `BCanvasPane` (área vacía) | `RubberBandTracker` (drag-select) |
| `BTabbedPane`/`BScrollBar`/`BSplitPane` | `PassThroughTracker` (pasa el mouse al widget) |
| widget ya seleccionado | `HitSelectedTracker` |
| widget no seleccionado | lo selecciona → `MoveTracker` (si está en un canvas) |

**Ciclo de vida (ej. `MoveTracker`)**: snapshot de anchor/bounds; en el primer `mouseDragged` pasado el umbral 3px
swappea el painter a `MovePainter`; en `mouseReleased`, si hubo drag, invoca `new MoveWidget(...).invoke()` (comando
undoable) y restaura `DefaultPainter`, devolviendo `new UnpressedTracker(...)` — todo tracker terminal resetea al idle. `[CERT]`

**Sub-jerarquía geométrica**: `GeometryTracker` (cursor crosshair) → `AddGeometryTracker` (acumula puntos de click,
Esc commitea vía `InsertDynamic`) → `AddPathTracker` (segmentos Bezier/quad/line de click+drag) y `AddPolygonTracker`.
`AddPointTracker`/`DeletePointTracker` mutan un `BPath`/`BPolygon` existente vía `MorphWidget`. Los handle-drag:
`HandleTracker` → `RectangularHandleTracker` (resize 8-puntos con aspect-lock + "friendly" sync multi-select vía
`CompoundCommand`) y `ShapeHandleTracker` → `Line`/`Path`/`PolygonHandleTracker` (edición por-vértice → `MorphWidget`). `[CERT]`

**Gotcha — código muerto**: `trackers/ConvertPointTracker.java` es un **stub vacío** decompilado
(`public class ConvertPointTracker {}`, sin campos/métodos/supertipo, `:3`); nada lo referencia — vestigial en este build. `[CERT]`

## 205.3 — Painters: feedback buffer-and-overlay `[CERT]`

Contrato base: `abstract void doPaint(Graphics)` (`painters/Painter.java`). Un painter activo a la vez, **swapped no
compuesto**, elegido por el tracker instalado vía `setPainter(...)`. El mecanismo no-obvio es **buffer-and-overlay**: todo
painter no-default llama `studio.buffer()` en su constructor, que snapshotea la página estática (widgets + handles) a un
`BImage` offscreen (`BStudio.buffer()`):

```java
// studio/painters/DefaultPainter.java (idle) — siempre un-buffers, pinta todo
public DefaultPainter(PainterStudio studio) { this.studio = studio; studio.unbuffer(); }
// studio/painters/MovePainter.java:23,28 — buffera una vez, por-frame solo blit + stroke dinámico
public MovePainter(...) { ...; studio.buffer(); }
public void doPaint(Graphics g) { this.studio.paintBuffer(g); /* ... stroke geoms del tracker ... */ }
```

Esto evita re-layout/re-paint de cada widget en cada mouse-move durante un drag — solo se blitea el bitmap bufferizado y
se stroke la geometría en movimiento encima: optimización de perf para páginas PX grandes. `GeomPainter` es la versión
genérica manejada por cualquier `GeomSupplier.geoms()` (RubberBand/HandleTrackers implementan `GeomSupplier`).
`AddGeometryPainter` dibuja las barras de control Bezier (fucsia) + handles ancla (lima) al trazar. `[CERT]` `[INFER: perf]`

## 205.4 — Artisans: construcción de geometría + path estilo-SVG `[CERT]`

**`Artisan`** (`artisans/Artisan.java`) es la strategy abstracta per-shape: `bounds`/`setGeom`/`move`/`zero`/`addHandles`/
`paintSelected`/`makeHandleTracker`. El dispatch NO es por registro sino un **router singleton con cadena `instanceof`/
`Type.is()`** hardcodeada:

```java
// studio/artisans/ArtisanRouter.java:71-83
public static Artisan artisan(BWidget widget) {
   if (widget.getType().is(BEllipse.TYPE)) return ellipse;
   else if (widget.getType().is(BRect.TYPE)) return rect;
   else if (widget.getType().is(BPolygon.TYPE)) return poly;
   else if (widget.getType().is(BLine.TYPE)) return line;
   else return widget.getType().is(BPath.TYPE) ? path : wid;  // fallback: WidgetArtisan genérico
}
```

`RectangularArtisan` (abstract) factoriza el resize rect de 8-handles compartido por `RectArtisan`/`EllipseArtisan`/
`WidgetArtisan` (el catch-all para cualquier `BWidget`, usa `BLayout` en vez de un gx Geom). `[CERT]`

**`PathArtisan` modela un path estilo-SVG** vía un router de segundo nivel: `SegmentArtisan artisan = new Router()`. El
`Router` (`artisans/path/Router.java`) despacha per-segmento por clase a un `SegmentArtisan` singleton por tipo de
segmento: `MoveTo`/`LineTo`/`HLineTo`/`VLineTo`/`CurveTo`/`SmoothCurveTo`/`QuadTo`/`SmoothQuadTo`/`ArcTo`/`ClosePath` —
espejo 1:1 de los `javax.baja.gx.IPathGeom.*` (**cross-ref B183 gx**: `IGeom`/`PathGeom`/`IPathGeom.Segment` son las mismas
clases gx). Es la gramática SVG path-data (`M/L/H/V/C/S/Q/T/A/Z`) reimplementada como objetos editables/pintables — por
eso `svgBatik` NO aparece acá: los tipos `IPathGeom` de gx son el modelo de path PROPIO de PX, aunque las letras/semántica
matcheen SVG 1:1. `[CERT]` `[INFER]`

Cada `SegmentArtisan` implementa `point`/`move`/`paintBars`/`paintHandles`/`addHandles`. **`Role`** (`artisans/path/
Role.java`) es el objeto de undo-bookkeeping por-handle: `Role.apply(dx,dy)` reconstruye el segmento en nueva posición —
ej. `CurveTo` registra **5 roles** por segmento (endpoint + 2 puntos de control + sus mirrored para la continuación smooth),
lo que deja arrastrar UN handle de control Bezier y que `PathHandleTracker.geoms()` reconstruya solo ese segmento. `[CERT]`

## 205.5 — Los geometry commands (cierran D4 por remisión) `[CERT]`

Todos extienden `javax.baja.ui.Command`; `doInvoke()` arma un `Artifact implements CommandArtifact` con `redo()`/`undo()`
— **confirma B198/B201**: el editor PX delega undo/redo enteramente al contrato Workbench, sin pila propia en `studio/`:

| Command | Qué hace | Undoable |
|---|---|---|
| `MoveWidget` | shift de N widgets por (dx,dy) vía `Artisan.move`; + helpers de geometría (min/max/center, clamp de flechas) | sí (redo +d/undo −d) |
| `MorphWidget` | resize/reshape — swap del `IGeom` vía `Artisan.setGeom`, guarda old/newGeom | sí |
| `Align` | alinea selección left/right/center o top/bottom/middle | sí |
| `Distribute` | espacia uniforme horizontal/vertical | sí |
| `Reorg` | z-order (front/back, canvas vs non-canvas invierten semántica) reordenando el `Property[]` del pane | sí |
| `PreferredSize` | resetea layout a `computePreferredSize()` | sí |
| `Select` | navegación z-stack (next above/below) — solo cambia selección | **NO** (`doInvoke` returns null, `commands/Select.java:38`) |

`Select` es el único sin `CommandArtifact` (navegación pura). Esto establece el patrón base (`Command` + inner `Artifact`)
con el que el gap **D4** (commands) cierra por REMISIÓN: los commands restantes del editor PX fuera de `studio/`
(Delete/Insert/Rename/etc., nombrados en B198 §198.7) siguen exactamente este mismo contrato — sin nueva sustancia. `[CERT]`

## 205.6 — Connections

- **[Block 198]** (undo=Workbench Command): CONFIRMADO en `studio/commands/` — Move/Morph/Align/Reorg/Distribute/
  PreferredSize son `javax.baja.ui.Command`+`Artifact`; el editor no tiene pila de undo propia.
- **[Block 191]** (BPxEditorPane): `BStudio` es el canvas (`BTransferWidget`) embebido en el `BPxEditorPane` de B191; el
  árbol de widgets (B198) y el canvas comparten el mismo `SelectedWidgets`.
- **[Block 183]** (gx geometry): los artisans operan sobre los `javax.baja.gx.IGeom`/`IPathGeom` de B183; el modelo de path
  de PX ES la geometría gx, no SVG DOM.
- **[Block 201]** (make wizard): el `AddGeometryTracker` commitea geometría nueva vía `InsertDynamic` — el mismo tipo de
  inserción de widgets que el wizard, en el contexto de dibujo a mano.
- **Cierra D4 por remisión** (§205.5): el patrón `Command`/`Artifact` cubre los commands de nivel editor restantes.
- **Contraste con X4 `svgBatik`** (feed): PX edita paths con su modelo gx propio (§205.4); `svgBatik` (aún sin abrir) es el
  render/import de SVG externo — dos cosas distintas.
