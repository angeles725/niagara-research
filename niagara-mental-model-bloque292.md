# Block 292 — The one-button toggle IS solvable in pure PX: `ToggleButton` + `SetPointBinding` closes B189 §189.4

> **DOCUMENT-MODE block (METHODOLOGY §20)**, deployed and confirmed working on a live station.
>
> **Corrects [Block 189] §189.4**, which declared the single-button open/close toggle *"sin resolver en PX
> puro"* and offered only two workarounds: open-with-button/close-with-selection, or logic inside the
> station. Neither is needed. B189 has been edited in place with a pointer here.
>
> **Sources**: (a) Tridium original source — `javax.baja.ui.BToggleButton`, `com.tridium.kitpx.BSetPointBinding`,
> `BButtonGroupBinding`, `BMouseOverBinding`, `BPopupBinding`; (b) the LIVE station `PRUEBAS`
> (OptimizerSupervisor N4.14.0.162) where the pattern was deployed and reported working by the operator;
> (c) the station's own shipped `.px` files as the imitation baseline.
>
> Markers: `[CERT]` verbatim in local code · `[CERT-live]` empirical against the running station ·
> `[INFER]` derived. **Corpus language: ENGLISH.**
>
> PX layer (applied). Connects [Block 189] (the applied menu), [Block 185] (`PopupBinding`),
> [Block 186] (`ValueBinding`), [Block 184] (converter type-guard).

---

## 292.1 — Why B189 concluded it was unsolvable, and what it missed `[CERT]`

B189 §189.4's reasoning was sound as far as it went: PX has no scripting (B181/B22), so the open/closed
state must live in the station; and `BooleanWritable` has no native `toggle` action, so a single
`ActionBinding` cannot flip it. Both premises hold. `[CERT]` (by reference)

What the block missed is that the state does not have to be flipped by an **action** at all. A widget can
*carry* the boolean and a binding can *write* it. Two pieces already in the framework supply exactly that.

## 292.2 — `BToggleButton` carries the boolean `[CERT]`

```java
public class BToggleButton
  extends BAbstractButton
{
  public static final Property selected = newProperty(0, false, null);
```
`docSource/.../bajaui-wb/javax/baja/ui/BToggleButton.java:33-51` `[CERT]`

`selected` is an ordinary boolean property that the widget flips on click as part of its own button
semantics — no station round-trip needed to change it. `[INFER]`

## 292.3 — `BSetPointBinding` writes a widget property to a point `[CERT]`

```java
/**
 * BSetPointBinding is used to display the current value of a "setpoint"
 * and also to provide the ability to modify it.  A setpoint is typically
 * a StatusValue property such as fallback.  The SetPointBinding ord must
 * resolve down to the specific property being manipulated.  If bound to
 * a component or to a readonly property, then the binding attempts to use
 * a "set" action to save.
 */
```
`docSource/.../kitPx-wb/com/tridium/kitpx/BSetPointBinding.java:49-54` `[CERT]`

Its two relevant properties: `[CERT]`

| Property | Meaning (javadoc) |
|---|---|
| `widgetEvent` | "Slot name of widget action or topic to trigger the apply the set point" (`:95-101`) |
| `widgetProperty` | "This is the widget property used to track the setpoint being driven" (`:124-130`) |

The last sentence of the class javadoc is what closes B189's objection: bound to a component, **the binding
itself invokes a `set` action**. The absent `toggle` action was never required. `[INFER]`

## 292.4 — The composed pattern `[CERT-live]`

```xml
<ToggleButton layout="16.0,52.0,140.0,26.0" text="Menu" selected="false">
  <SetPointBinding ord="station:|slot:/Drivers/PRUEBAS/MenuOpen" widgetEvent="actionPerformed" widgetProperty="selected"/>
</ToggleButton>

<GridPane layout="16.0,78.0,140.0,130.0" columnCount="1" rowGap="1" background="#8a9099" columnAlign="fill" uniformColumnWidth="true" visible="false">
  <ValueBinding ord="station:|slot:/Drivers/PRUEBAS/MenuOpen" degradeBehavior="hide">
    <IBooleanToSimple name="visible"/>
  </ValueBinding>
  <!-- one Label or ImageButton per menu entry -->
</GridPane>
```

The circuit: click → `selected` flips → `SetPointBinding` writes it to the `BooleanWritable` → the panel's
`ValueBinding` reads that point → `IBooleanToSimple` maps it onto `visible`. One button, both directions.
Deployed on the live station and confirmed working by the operator. `[CERT-live]`

The read half is not new — it is the pattern the site already used, e.g. `Dashboard/Home.px:134-156` where
alarm polygons follow `station:|slot:/Drivers/CODIGOS/Alarma_Planta{1..4}` through
`<IBooleanToSimple name="visible"/>`. `[CERT-live]` `IBooleanToSimple` passes B184's type-guard because
`visible` is a `BBoolean` (B184 §184.4). `[CERT]` (by reference)

**Two operational requirements**, both learned the hard way: `[CERT-live]`

1. **Slot names are case-sensitive.** The point was created as `MenuOpen`; a binding written against
   `menuOpen` resolves to nothing and fails silently — no error, just a dead button.
2. **A fresh `BooleanWritable` reads `null`** (`null="true"` over obix until first written).
   `degradeBehavior="hide"` turns that into "panel hidden", which is the correct initial state, so this is
   benign — but set the point's `fallback` to `false` for a defined value.

## 292.5 — Why `PopupBinding` was the wrong tool, settled `[CERT]`

B189 §189.5 recommended Pattern A (`PopupBinding`) as the low-effort default. Field result: it produces a
floating **window**, in Workbench AND in the browser, never an anchored dropdown. The class says so:

```java
import com.tridium.workbench.shell.BNiagaraWbDialog;
/** This binding gets used for popping up new views in a window */
...
public static final Property title = newProperty(0, "Pop up", null);
```
`docSource/.../kitPx-wb/com/tridium/kitpx/BPopupBinding.java` (import `:33`, javadoc `:38`, `title` `:111`)
`[CERT]`

The default `title` value `"Pop up"` is literally the caption users see on the floating window.
`[CERT-live]` Under the browser profile it becomes a browser window with its own address bar. `[CERT-live]`

**Revised recommendation for [Block 189] §189.5**: for an anchored dropdown, Pattern B (this block) is the
default. `PopupBinding` remains correct for what it says it does — opening a *view in a window*. `[INFER]`

## 292.6 — The rest of the kitPx binding inventory `[CERT]`

Catalogued while solving this, since B189 assumed the toolbox was smaller than it is:

```
BActionBinding    BBoundLabelBinding  BButtonGroupBinding   BIncrementSetPointBinding
BMomentaryToggleBinding  BMouseOverBinding  BPopupBinding   BSetPointBinding
BSpectrumBinding  BSpectrumSetpointBinding
```
plus Hx (web-profile) counterparts: `BHxPxActionBinding`, `BHxPxButtonGroupBinding`, `BHxPxIncrementBinding`,
`BHxPxMomentaryToggleBinding`, `BHxPxMouseOverBinding`, `BHxPxPopupBinding`, `BHxPxSetPointBinding`. `[CERT]`

Two worth naming:

- **`BButtonGroupBinding`** — `style` defaults to `BButtonGroupStyle.radio` (`:71`), and it GENERATES its
  buttons from the bound point's range: `int[] ords = range.getOrdinals()` (`:209`), then
  `BToggleButton trueButton = makeButton(cmds[0]); w.add(null, trueButton, Flags.TRANSIENT)` (`:172-176`).
  `[CERT]` So mutually-exclusive "which menu item is active" state is a solved problem too — but the
  generated buttons carry no hyperlink of their own. `[INFER]`
- **`BMouseOverBinding`** — *"used to allow animating widgets on mouse enter / mouse exit events"*
  (`:29`), and it carries its own `hyperlink` property (`:144`). `[CERT]` Hover highlighting plus
  navigation in one binding.

Site usage confirms two dialects of the hover pattern: `Dashboard/Bien2.px` animates `stroke` on an
`ImageButton` that already declares a `background`, while `Dashboard/Home.px` animates `background` on one
that does not. `[CERT-live]` Whether that is causal was not established. `[INFER]`

## 292.7 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| B189 §189.4 declared the toggle unsolved in pure PX | B189 body | `[CERT]` |
| `BToggleButton` has a boolean `selected` property | `BToggleButton.java:33-51` quoted | `[CERT]` |
| `BSetPointBinding` writes a widget property to a point | class javadoc `:49-54` quoted | `[CERT]` |
| It falls back to invoking a `set` action | same javadoc, last sentence | `[CERT]` |
| `widgetEvent` / `widgetProperty` semantics | javadoc `:95-101`, `:124-130` | `[CERT]` |
| ⇒ no `toggle` action is required | derived | `[INFER]` |
| The composed pattern works on a live station | deployed; operator confirmed | `[CERT-live]` |
| The read half matches site usage | `Dashboard/Home.px:134-156` | `[CERT-live]` |
| `IBooleanToSimple` passes the type-guard for `visible` | B184 §184.4 by reference | `[CERT]` |
| Slot names are case-sensitive; `menuOpen` ≠ `MenuOpen` | observed silent failure, then fix | `[CERT-live]` |
| A fresh `BooleanWritable` is null until written | obix `null="true"` on the new point | `[CERT-live]` |
| `PopupBinding` imports `BNiagaraWbDialog`, javadoc says "in a window" | `BPopupBinding.java:33,38` | `[CERT]` |
| Its `title` defaults to the literal `"Pop up"` | `:111` | `[CERT]` |
| That caption appears on the floating window | screenshots, Workbench and browser | `[CERT-live]` |
| kitPx binding inventory (10 + 7 Hx variants) | class listing of `kitPx-wb` | `[CERT]` |
| `BButtonGroupBinding` generates radio buttons from a range | `:71`, `:172-176`, `:209` | `[CERT]` |
| Generated group buttons carry no hyperlink | absence in the quoted construction | `[INFER]` |
| `BMouseOverBinding` animates on enter/exit and has `hyperlink` | `:29`, `:144` | `[CERT]` |
| `stroke` vs `background` correlates with a declared background | two site files compared | `[CERT-live]` |
| That correlation is causal | NOT established | `[INFER]` |

Tally: **[CERT] 11 / [CERT-live] 6 / [INFER] 4.**

---

## 292.x — Connections and open gaps

- **[Block 189] §189.4/§189.5** — **CORRECTED by this block.** The friction it described is resolved and
  its Pattern A recommendation is revised for the anchored-dropdown case.
- **[Block 185]** — `PopupBinding` mechanics; §292.5 adds the field verdict and the `"Pop up"` caption.
- **[Block 186]** — `ValueBinding.getOnWidget` resolving `visible`, the read half of §292.4.
- **[Block 184] §184.4** — the converter type-guard `IBooleanToSimple` passes.
- **[Block 291]** — the alternative route for dropdowns that must overlap other widgets, which the PX
  canvas cannot do at all.

### Open gaps

| ID | Gap | Class |
|---|---|---|
| **B292-G1** | `BButtonGroupBinding` end-to-end: bind it to an `EnumWritable` and confirm whether the generated radio buttons can be given hyperlinks — the missing piece for a navbar with a persistent "active item" highlight. | DYNAMIC |
| **B292-G2** | Is the `stroke` vs `background` choice in `ActiveStateSimple` determined by whether the widget declares a `background` attribute, or by widget type? Two site files correlate; no code read. | STATIC |
| **B292-G3** | Whether `SetPointBinding` behaves the same under the Hx/web profile via `BHxPxSetPointBinding` — the live confirmation was obtained in Workbench. | DYNAMIC |
