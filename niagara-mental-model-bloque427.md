# Block 427 — The Workbench Swing widget model: BWidget is a BComponent, painted through gx, hosted on an AWT shell

> Research of the **gx + bajaui widget foundation** every Workbench UI surface sits on (focus `workbench`,
> gap WB01). Scope: the `BWidget` base contract, the `BPane`/layout model, the `gx` abstract 2D layer, event
> dispatch, the Palladium/Curium/Custom theming system, and the AWT/Swing bridge. Does NOT cover the PX web
> widget layer (`bajaux` — [Block 204], a different toolkit) nor specific editors (wire sheet = WB03,
> property sheet = WB04).
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `bajaui-wb.jar`
> sha256 `391dfdd3d80d594e044654688f249182f181ea809139ff0c2e0b95b033b6dfa5` · `gx-wb.jar`
> sha256 `05a8889fac6ab878affef4b7429f2a8bbb8de8b32e591e03ae57599d0199bbd9`.
>
> Sources: ORIGINAL Tridium javadoc source (highest fidelity) preserved under `sources/tridium-src/bajaui-wb/`
> + `sources/tridium-src/gx-rt/`; private impl from Vineflower decompilation of `bajaui-wb`
> (`com.tridium.ui.*`). Method: read the clean docSource for the public contract, Vineflower for impl
> existence. CAVEAT: the private AWT shell (`com.tridium.ui.awt.AwtShellManager`) decompiles MANGLED
> (`abstract ln ln`) — its body-level mechanics are `[INFER]` from the public contract + class names, not
> `[CERT]`. Markers: `[CERT]` clean source (`file:line`) · `[INFER]` deduction.
>
> Workbench UI framework. Connects [Block 22] (BajaUI runtime — this opens its widget model), [Block 204]
> (bajaux = the WEB widget twin, contrast), [Block 183]/[Block 190] (gx used for PX serialization — same gx,
> different consumer).

---

## 427.1 — BWidget IS a BComponent: widgets live in the slot tree `[CERT]`

The base of every Workbench UI element is `BWidget`, and it extends `BComponent`: `[CERT]`

```java
public class BWidget
  extends BComponent implements IStylable        // sources/tridium-src/bajaui-wb/javax/baja/ui/BWidget.java:121
```

This is the load-bearing architectural fact: a widget is a Baja **component** — it lives in the slot/component
tree, has dynamic slots, fires Topics, and participates in the same subscription/link machinery as any
`BComponent`. `[CERT]` Mounting is tree-recursive: `initShell(ShellManager)` walks the child widgets and wires
each into the shell (`BWidget.java:572`,`:587`). `[CERT]` `[INFER]` so a Workbench UI is not a separate object
graph bolted onto Baja — it IS a Baja component subtree whose paint/layout/event behavior the framework adds.

## 427.2 — Geometry and layout: deferred relayout → doLayout → setBounds `[CERT]`

Geometry is plain fields set through `setBounds(x,y,w,h)` / `setSize` / `setPreferredSize`
(`BWidget.java:961`,`:947`,`:970`), NOT serialized slots. `[CERT]` Layout is a deferred, escalating cycle:
`relayout()` marks the widget dirty and bubbles up (`BWidget.java:998`); the actual pass runs later via
`doLayout(BWidget[] children)` (`:1099`), a hook each container overrides to position children, with
`computePreferredSize()` (`:1109`) supplying the sizing input. `[CERT]`

The container is `BPane` (`abstract class BPane extends BWidget implements UndoManager.Scope`,
`sources/tridium-src/bajaui-wb/javax/baja/ui/pane/BPane.java:25`). `[CERT]` **Layout strategy is baked into
each concrete pane's `doLayout`**, not a pluggable LayoutManager object. Example — `BEdgePane` (BorderLayout
analogue) exposes typed `top/left/center/right/bottom` slots (`BEdgePane.java:99`,`:159`,`:218`) and its
`doLayout` gives edges their preferred size and lets `center` fill the remainder (`:277`). `[CERT]` The pane
family (border/grid/flow/canvas/split/tabbed/scroll/expandable/…) is ~18 concrete subclasses. `[INFER]`

## 427.3 — gx: the abstract 2D layer bajaui paints through `[CERT]`

`gx` (`javax.baja.gx`, from `gx-rt`) is a pure **interface** plus value types — the rendering contract, with
no AWT in it: `[CERT]`

```java
public interface Graphics {                  // sources/tridium-src/gx-rt/javax/baja/gx/Graphics.java:14
  void setBrush(BBrush b);   // :59      void setPen(BPen p);   // :74
  void setFont(BFont font);  // :84       void translate(double x, double y);  // :99  ...
}
```

Value types: `BBrush` (fill), `BPen` (stroke), `BFont`, `BColor`, `BImage`, `BTransform`, and geometry
(`IGeom`/`RectGeom`/`BPathGeom`/…). `[CERT]` (roster from `gx-rt` docSource.) `BWidget.paint(Graphics g)`
(`BWidget.java:1173`) draws exclusively against this interface — never against `java.awt` directly. `[CERT]`
`gx-wb` (the 1075-class wb module) is the AWT **backend**: it provides the concrete `Graphics` that wraps
`java.awt.Graphics2D`. `[INFER]` (the wb module holds the impl; the interface is in rt.)

## 427.4 — Event dispatch: fire*Event on the widget, then a Baja Topic `[CERT]`

Input events are Baja structs: `BWidgetEvent` (`BWidgetEvent.java:19`) with subtypes `BMouseEvent`
(`BMouseEvent.java:26`, e.g. `MOUSE_PRESSED = 501` at `:49`), `BKeyEvent`, `BFocusEvent`. `[CERT]` `BWidget`
carries CUSTOM `fireMouseEvent`/`fireKeyEvent`/`fireFocusEvent` implementations (annotated `@NoSlotomatic`,
`BWidget.java:120`) that dispatch to per-type handler methods AND fire a Baja Topic so links/subscriptions
see the event. `[CERT]` `[INFER]` events originate at the AWT shell (hit-test → coordinate translate →
construct `BMouseEvent` → `widget.fireMouseEvent`), so the AWT event queue feeds the widget tree but widgets
themselves speak only in `BWidgetEvent`.

## 427.5 — Theming: a 3-family × ~40-widget theme matrix, selected by device `[CERT]`

Look-and-feel is a static `Theme` registry (`com.tridium.ui.theme.Theme`, Vineflower — cleanly decompiled).
`installDefaultTheme()` (`Theme.java:437`) picks by device: **Curium** for a touchscreen, **Palladium** for
desktop. `[CERT]` On disk there are THREE complete theme families, each a full set of ~40 per-widget-type
theme classes: `[CERT]`

| Family | Package | When | Evidence |
|---|---|---|---|
| **Palladium** | `com/tridium/ui/theme/palladium/` | desktop default | `PalladiumWidgetTheme`, `…BorderPaneTheme`, `…WiresheetTheme`, … (~40 classes) |
| **Curium** | `com/tridium/ui/theme/curium/` | touchscreen | `CuriumWidgetTheme`, … (~40 classes) |
| **Custom** | `com/tridium/ui/theme/custom/` | NSS-driven override | `CustomWidgetTheme`, … (~40 classes) |

Each widget type has a matching `*Theme` (`WidgetTheme`, `PaneTheme`, `TableTheme`, `TreeTheme`,
`WiresheetTheme`, …) and pulls its brushes/fonts from the installed family — e.g. `BPane.paint` calls
`Theme.pane().paintBackground(g, this)`. `[CERT]`/`[INFER]` The `Custom` family + `BWidget`'s `IStylable`
(`styleClasses`/`styleId`) is the NSS (CSS-like) override path. `[INFER]`

## 427.6 — The AWT/Swing bridge `[CERT]`/`[INFER]`

Rendering, repaint, and relayout all route through a `ShellManager` abstraction (`BWidget.repaint()` →
`shellManager.repaint(...)`, `BWidget.java:1154`), never through AWT directly from widget code. `[CERT]` The
concrete shell is `com.tridium.ui.awt.AwtShellManager` and the Swing-embedding widget is
`com.tridium.ui.swing.BSwingWidget` — both present in `bajaui-wb`. `[CERT]` (existence). Their bodies decompile
MANGLED (`AwtShellManager` = `abstract ln ln`), so the mechanics are `[INFER]`: `AwtShellManager` is the
`java.awt.Panel` that is the single real host surface — it double-buffers and calls the root `BWidget.paint`
with a gx `Graphics` wrapping `Graphics2D`; `BSwingWidget` is the reverse bridge, a `BWidget` that hosts a
`JRootPane` so real Swing `JComponent`s can live inside the bajaui tree. `[INFER]` (I do not cite line-level
behavior for the mangled classes.)

## 427.7 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | `BWidget extends BComponent implements IStylable` — widgets are Baja components | `[CERT]` | `sources/tridium-src/bajaui-wb/javax/baja/ui/BWidget.java:121` |
| 2 | Layout = deferred `relayout`→`doLayout(children)`→`setBounds`; strategy baked per-pane | `[CERT]` | `BWidget.java:998`,`:1099`; `BEdgePane.java:277` |
| 3 | `BPane extends BWidget implements UndoManager.Scope` | `[CERT]` | `sources/tridium-src/bajaui-wb/javax/baja/ui/pane/BPane.java:25` |
| 4 | gx = interface `Graphics` + value types; widget paints only through it | `[CERT]` | `sources/tridium-src/gx-rt/javax/baja/gx/Graphics.java:14` |
| 5 | Events are `BWidgetEvent`/`BMouseEvent` (`MOUSE_PRESSED=501`), fired on the widget + a Topic | `[CERT]` | `BMouseEvent.java:49`; `BWidget.java:120` |
| 6 | Theme = 3 families (Palladium desktop / Curium touch / Custom NSS) × ~40 widget themes; device-selected | `[CERT]` | `Theme.java:437` + on-disk roster |
| 7 | AWT shell (`AwtShellManager` = a `Panel`) + `BSwingWidget` host JRootPane — bodies mangled | `[CERT]`/`[INFER]` | vineflower existence; §427.6 |

**Marker tally**: `[CERT]` ≈ 22 · `[INFER]` 8 ([INFER]/[CERT] ≈ 0.36). Type: **EVIDENCE block** (model
overview) — ratio healthy. VERIFY-BEFORE-ACTING applied: the sweep's clean-name citations were token-checked
against the PRESERVED docSource (public model confirmed at the exact cited lines) and the mangled AWT-impl
claims were DOWNGRADED to `[INFER]` when the Vineflower body was found unreadable — the sweep had cited them
as clean bodies. Load-bearing tokens re-verified: `BWidget extends BComponent`, `relayout`/`doLayout`,
`interface Graphics`, `MOUSE_PRESSED = 501`, `installDefaultTheme`, the 3-family theme roster — all present.

## 427.8 — Connections

- **[Block 22]** — BajaUI runtime overview; this block opens its widget model at body grade.
- **[Block 204]** — `bajaux` is the WEB widget toolkit (JS/HTML, `@AgentOn`); `bajaui` here is its Swing twin.
  Same conceptual role (Widget lifecycle), completely different substrate.
- **[Block 183]/[Block 190]** — gx used to serialize/parse PX; SAME `javax.baja.gx` layer, consumed by the
  PX pipeline instead of by an interactive shell.
- **WB02–WB05, WB07** (queued) — the shell, wire sheet, property sheet, managers, and commands all build on
  THIS BWidget/BPane/gx/Theme foundation.

<!-- research-block: focus workbench, gap WB01 (gx/bajaui widget model) — CLOSED at body grade (public model CERT; private AWT impl INFER due to mangled decompile) -->
