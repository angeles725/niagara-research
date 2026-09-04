# B747 · Niagara's Wire Sheet IS a flow-programming surface — a feature-by-feature comparison with Node-RED, and the three visual-FB engines already in this install

> **Scope**: the operator likes how Node-RED blocks are built and wants OUR module ecosystem to feel
> interactive, discoverable, and not overwhelming. This block answers the prior question: *what is Niagara's
> equivalent of a Node-RED flow, and how do they compare?* It reconstructs the Wire Sheet AS a flow editor
> (blocks, pins, wires, palette, layout, deploy model) from the code-grounded blocks, sets it side-by-side
> with Node-RED's model, and names the THREE visual function-block engines already present in this install
> (Niagara Wire Sheet · kitControl · Honeywell Spyder). Foco: **interactive-composition** (IC1 + IC4), gap
> bootstrap. The recommendations live in B748.
>
> **Sources**: FUENTE 1 — B429 (wire-sheet editor internals, `[CERT]` `file:line`), B735 (SUMMARY pins /
> slot curation), B737/B744 (composition), B746/B634 (palette), B738 (icons), B24 (kitControl palette, 152
> blocks / BLoopPoint), B116/B106/B115 (Honeywell Spyder FB tool), B5 (ORD/BQL, obix scheme). Node-RED facts
> are general public knowledge of the open-source project → marked `[CERT-web]` (model-level, no install
> under study). No re-derivation: every Niagara fact cites a code-grounded block.

---

## 747.1 — The claim: the Wire Sheet is a live flow editor, not a diagram `[CERT, B429]`

`BWireSheet` is registered `@AgentOn(types="baja:Component", requiredPermissions="W")` — so **every component
in the station gets a wire-sheet view**, gated on WRITE permission (`BWireSheet.java:31`, B429 §429.1). It is
not a picture of the design; it is an EDITOR whose glyphs are a **live projection of the container's
children**: each `ComponentGlyph` holds the live `BComponent` + its handle as identity (B429 §429.2). Open a
folder → you are looking at, and editing, the real component tree. That is the same promise as a Node-RED
flow tab: the canvas *is* the running program, not a drawing of it.

## 747.2 — Term-by-term: Niagara Wire Sheet ↔ Node-RED `[CERT, B429/B735 · CERT-web Node-RED]`

| Node-RED concept | Niagara Wire Sheet equivalent | Evidence / note |
|---|---|---|
| **Flow tab** (canvas) | The wire-sheet view of a container `BComponent` | `@AgentOn(baja:Component)` — B429 §429.1 |
| **Node** (a block) | `ComponentGlyph` / `StdComponentGlyph` = a `BComponent` drawn as a `TitleBarGlyph` + one `SlotBarGlyph` per visible slot + footer | B429 §429.2 |
| **Input/output ports** | The `SlotBarGlyph`'s left/right **hotspot = the wire terminal**; wiring is slot-to-slot | B429 §429.2 |
| **Which slots become ports** | **`Flags.SUMMARY` and ONLY summary**: `SlotBarGlyph` visibility is `Flags.isSummary(component, slot)` — so a pin row shows iff the slot is summary. "Pin a slot" (`PinSlotsCommand`) literally **sets the summary flag**. `HIDDEN` never shows | `SlotBarGlyph.java:56`; `PinSlotsCommand.java:38`; `Flags.java:12` (SUMMARY=8); closes B735-G1 |
| **Wire** (a connection) | A `BLink` (Property→Property "dataLink", or Topic→Action event link), drawn as a `LinkSnakeGlyph` on a wixel grid with orthogonal routing (straight→L→detour→pathfind) and bridge glyphs over crossings | B429 §429.3; B735 §735.4 |
| **Message on a wire** (`msg` object, discrete) | **A continuous data-link**: the target slot is kept equal to the source slot; propagation is push-on-change on the single engine thread, not a discrete message queue | B737 §A.2 (engine thread); B735 §735.4 (dataLink) |
| **Palette** (left drawer of node types) | The **module palette** (`module.palette` BOG XML) surfaced as a browsable component space; drag an item onto the sheet → `WsController.insertTransferData` → `TransferUtil.add` | B746; B429 §429.4 |
| **Deploy button** | **None — edits are live.** Adding a link/component is an undoable workbench Command applied immediately to the running station | B429 §429.4/§429.6 |
| **Node icon** | `getIcon()` → a PNG/SVG module resource painted on the glyph | B738 |
| **Node status line** (text under a node) | **Richer**: `PropertyBarGlyph.updateValueString()` paints each pin's LIVE value formatted with its facets (units/precision), and if the value is `BIStatus` the pin row is **tinted by its `BStatus`** (fault/down/stale/override colors, `getShowStatusColors()`). Refresh is push-on-change: `handleComponentEvent → WsController.handleComponentEvent → glyph.changed(slot) → updateValueString → repaint` | `PropertyBarGlyph.java:35-46`; `WsController.java:544-574`; `BWireSheet.java:79-81` |
| **Flow layout (stored in the flow JSON)** | Glyph position persists as a **HIDDEN `wsAnnotation` slot** (`p, q, width`) on the component itself → travels IN THE BOG with the station backup; no side-car file | B429 §429.5 |
| **Undo/redo** | Every sheet edit is a `WsCommand extends Command` on the standard undo stack (`MoveGlyphsCommand`, `DeleteLinksCommand`, …) | B429 §429.6 |
| **Subflow** (reusable grouped flow) | **A child `BComponent`** — composition nests a whole sub-tree behind one glyph; drill in for its own wire sheet | B737 §B; B744 §744.6 |
| **Import/export flow as JSON** | Copy/paste or a **palette assembly template** (a `<p>` with nested children + overrides) serializes the sub-tree as BOG XML | B746 §746.3 |
| **Function node** (inline JS) | `kitControl` blocks (math/logic/PID) or a custom `BComponent`; there is also a Program object for BajaScript — logic is a typed block, not free-text on the canvas | B24; B744 |

**Reading**: Niagara ALREADY has the Node-RED primitives — draggable typed blocks, slot-to-slot ports,
routed wires, a palette, persistent layout, undo, and nesting-as-subflow. The two real model differences are
(a) **data-links are continuous state mirrors, not discrete messages**, and (b) **there is no deploy step —
the sheet edits the live station**. Both are consequences of the single engine-thread execution model
(B737 §A.2), not missing features.

## 747.3 — Where each MODEL wins on cognitive load `[INFER, grounded above · CERT-web Node-RED]`

- **Node-RED wins on**: a browser-native, zero-install editor; a live **debug sidebar** (wire a debug node,
  watch messages) that makes discrete data flow *visible*; a massive community palette installed from a
  registry; and flows that are plain JSON to diff/share. Its message model makes "what fired and with what
  payload" legible at a glance.
- **Niagara Wire Sheet wins on** (some of these Node-RED lacks): it edits the REAL running control program
  with typed, unit-aware values; **it already paints each pin's live value tinted by its status color**
  (`PropertyBarGlyph`, §747.2) — a live "what is this block doing right now" readout Node-RED only approximates
  with a manual debug node; links are **type-checked before they connect** (`checkLink`, B735 §735.4) so you
  cannot wire an incompatible pair; layout and logic are one persisted artifact (the BOG, B429 §429.5); and
  every edit is transactional + undoable on a hardened single-threaded engine (B737). It is a control engine
  with a flow editor built in, not a flow runtime.
- **Where Niagara feels "cansino" (the operator's word)** and Node-RED doesn't: the Swing desktop weight (no
  browser-native editor for the wire sheet — the ux variant exists but isn't the authoring surface); a flat
  wall of slots when a component wasn't composed into children (B737 §B.2 — our 25-slot `BEvaporatorUnit`);
  every non-SUMMARY slot cluttering the property sheet; a palette of **bare** components that must be nested
  and wired by hand (B746 §746.2); and no packaged "watch the flow" debug affordance for operators. Note the
  live-value readout is NOT missing — it exists but requires SUMMARY-curated pins and facets to be legible.
  Nearly all of this is an AUTHORING choice on OUR side, addressable without leaving Niagara — that is B748.

## 747.4 — Three visual function-block engines already in THIS install `[CERT]`

The install already contains three distinct visual-FB systems — a useful spread of design points for "what a
good interactive block model looks like":

1. **Niagara Wire Sheet + kitControl** `[CERT, B24/B429]` — the native surface. `kitControl` ships a
   **152-component palette** (math, logic, latches, timers, `BLoopPoint` PID that executes every
   `executeTime`, B24 §24.17/§24-PID). Live-edit, continuous data-links, no compile. This is the closest
   analog to a Node-RED running flow.
2. **Honeywell Spyder FB tool (XL10NextGen)** `[CERT, B116/B106]` — an OEM tool INSIDE the same Workbench,
   with its own `WireSheet`, `WiringDiagram`, and `PiranhaSplitWireSheetView` views
   (`honeywellSpyderTool-WireSheet.html`, B116 §116.3). Its palette has **7 categories** (Analog, BuiltIn,
   Control, DataFunction, Logic, Math, ZoneArbitration; 43 documented algorithms, B116 §116.4). Crucially it
   uses the **compile→binary→download** model (B106/B116 §116.5) — the Node-RED "Deploy" step made explicit,
   because the target is a separate physical controller, not the running station.
3. **kitControl vs Spyder contrast** `[INFER]` — same visual grammar (blocks + wires + palette), opposite
   execution: kitControl runs live in the station engine (edit = effect); Spyder compiles an application and
   pushes a binary to a field controller (edit → compile → download → run). Node-RED sits with kitControl
   (live) but borrows Spyder's explicit "deploy" affordance. The lesson for us: **live-edit is powerful but
   dangerous on a control engine** (a bad link is immediate, B737 §A.2) — an interactive layer for operators
   may WANT a Spyder-style stage/commit boundary rather than kitControl's edit-is-effect.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Every component has a wire-sheet view; glyphs are a live projection of the child components | [CERT] | B429 §429.1/§429.2 (`BWireSheet.java:31`) |
| 2 | Block = ComponentGlyph (TitleBar + SlotBar per visible slot); SlotBar hotspot = wire terminal | [CERT] | B429 §429.2 |
| 3 | Wire-sheet pin row is visible IFF the slot is SUMMARY; pinning a slot = setting its summary flag; HIDDEN removes from all UI | [CERT] | `SlotBarGlyph.java:56`; `PinSlotsCommand.java:38`; `Flags.java:12`; closes B735-G1 |
| 4 | Wires = BLink drawn via a Knob (source/target ords+slots) as LinkKnobGlyph/LinkGlyph, orthogonal wixel routing + bridges | [CERT] | `Knob.java:5-22`; `LinkKnobGlyph.java:16-31`; `LinkGlyph.java:141-160`; B429 §429.3 |
| 5 | Data-links are continuous state mirrors on the single engine thread, not discrete messages | [CERT] | B737 §A.2; B735 §735.4 |
| 6 | Palette drop → BWsCanvas.drop → WsController.insertTransferData → container.add; layout persists as HIDDEN wsAnnotation (BWsAnnotation p/q/wixelWidth) in the BOG; edits live+undoable | [CERT] | `BWsCanvas.java:291-296`; `WsController.java:189-194,245-282`; `BWsAnnotation.java:230`; B429 |
| 7 | The glyph paints each pin's LIVE value with facets and tints the row by BStatus; refresh is push-on-change | [CERT] | `PropertyBarGlyph.java:35-46`; `WsController.java:544-574` |
| 8 | Subflow ≈ child BComponent (composition nests a sub-tree behind one glyph) | [CERT] | B737 §B; B744 §744.6 |
| 9 | Three FB engines: Niagara WireSheet+kitControl (152 blocks, live), Honeywell Spyder (7 cats, compile→download) | [CERT] | B24 §24.17; B116 §116.3/§116.4/§116.5 |
| 10 | Node-RED model facts (flows, msg objects, palette, deploy, debug sidebar) | [CERT-web] | public Node-RED project knowledge |
| 11 | Cognitive-load deltas (Swing weight, flat slots, bare palette, no packaged operator debug) are our authoring choices | [INFER] | grounded in B737 §B.2 / B746 §746.2 |

**Tally**: 9 [CERT], 1 [CERT-web], 1 [INFER]. No unmarked claims. (Live-value + pin-flag claims re-verified `file:line` by a fresh wiresheet-wb code sweep this iteration.)

## Connections
- **B429** (wire-sheet editor internals), **B735** (SUMMARY pins / curation), **B737**/**B744** (composition),
  **B746**/**B634** (palette), **B738** (icons), **B24** (kitControl 152 blocks), **B116**/**B106**/**B115**
  (Spyder FB tool), **B5** (ORD/BQL/obix scheme). Forward: **B748** (the recommendations that act on this).

## Open gaps
- **B747-G1**: CLOSED this iteration — live values ARE painted with facets and status-tinted
  (`PropertyBarGlyph.java:35-46`), refreshed push-on-change (`WsController.java:544-574`). §747.2 row upgraded
  to [CERT].
- **B747-G2 / B735-G1**: CLOSED — the wire sheet shows ONLY SUMMARY slots (`SlotBarGlyph.java:56
  Flags.isSummary`), and "pin a slot" sets the summary flag (`PinSlotsCommand.java:38`). B429's "per visible
  slot" = per SUMMARY slot; reconciled.
- **B747-G3** (new, low): the `BPalette` widget class itself (the side-bar palette source) lives in the
  `workbench` module and was not opened — only the drop→add path is [CERT]. Cosmetic; the drag mechanics are
  proven.
