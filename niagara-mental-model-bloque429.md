# Block 429 — The wire sheet editor: glyphs mirror the component tree, layout persists as a hidden slot, links delegate to workbench commands

> Research of the **Workbench wire sheet editor** (`wiresheet-wb`, focus `workbench`, gap WB03) — the canvas
> where BComponents are wired together. Scope: the view + canvas, the glyph model, link routing, drag/drop +
> creation, undo, and where layout persists. Builds on [Block 427] (widget/pane) and [Block 428] (the shell
> that opens this view). Does NOT cover the link/relation semantics themselves (`BLink` runtime — framework).
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `wiresheet-wb.jar`
> sha256 `c2bb9d121bc3fe96a03cb050bcccaf10751601e242c813da8ebf6c414786b2f6`.
>
> Sources: Tridium docSource (`sources/tridium-src/wiresheet-wb/`, public `BWireSheet`) + Vineflower impl
> (`sources/decompiled/wiresheet-wb/`, `com.tridium.wiresheet.*` — clean, no mangling; 68 classes). Method:
> docSource for the view contract, Vineflower for impl, all load-bearing lines re-verified live. Markers:
> `[CERT]` (`file:line`) · `[INFER]` deduction.
>
> Workbench UI framework. Connects [Block 427] (glyphs are `BWidget`s; the canvas is a `BTransferWidget`, NOT a
> `BCanvasPane`), [Block 428] (the shell opens `BWireSheet` via the `@AgentOn` ORD→view pipeline).

---

## 429.1 — The view and canvas `[CERT]`

`BWireSheet` is a `BWbComponentView` registered `@AgentOn(types="baja:Component", requiredPermissions="W")`
(`sources/tridium-src/wiresheet-wb/javax/baja/wiresheet/BWireSheet.java:31`) — so ANY component node gets a
wire sheet view, gated on WRITE permission. `[CERT]` It wraps a `BWireSheetPane` (`extends BEdgePane`,
`.../BWireSheetPane.java:31`) whose center is a `BScrollPane` over a `BWsCanvas`
(`new BScrollPane(new BWsCanvas())`, `:85`). `[CERT]` The canvas `BWsCanvas` extends **`BTransferWidget`**, not
`BCanvasPane` (`.../BWsCanvas.java:29`) — it forwards all mouse/key/drag events to a `WsController` state
machine. `[CERT]` `[INFER]` the drawing surface is a transfer-target widget (drag/drop aware) rather than a
generic canvas pane — the wire sheet is fundamentally a drop target.

## 429.2 — Glyphs mirror the component tree in two layers `[CERT]`

The canvas holds a `RootGlyph` with two child layers painted link-under-component
(`.../RootGlyph.java:9`,`:10`): `[CERT]`

| Layer | Type | Holds |
|---|---|---|
| `componentLayer` | `LayerGlyph` | one `ComponentGlyph` per child `BComponent` |
| `linkLayer` | `LinkLayerGlyph` | one `LinkGlyph` per `BLink` |

Each `ComponentGlyph` holds the LIVE `BComponent` plus its `handle` as identity key
(`.../ComponentGlyph.java`), so the glyph layer is a live projection of the container's children. `[CERT]`
`StdComponentGlyph` builds the box as a `TitleBarGlyph` + one `SlotBarGlyph` per visible slot + a footer
(`.../StdComponentGlyph.java`); **the `SlotBarGlyph`'s left/right hotspot is the wire terminal**. `[CERT]`
`[INFER]` a component's box on the sheet is literally its slot list; wiring is slot-to-slot.

## 429.3 — Links: orthogonal wixel-grid routing, drawn but not owned `[CERT]`

Wires render as `LinkSnakeGlyph` on a **wixel grid** with an orthogonal routing cascade — straight →
L-shape → detour → pathfind → stubs (`.../LinkSnakeGlyph.java`). `[CERT]` Crossings (a horizontal wire over a
vertical one in the same cell) are detected on the canvas and drawn as bridges. `[INFER]` But the sheet does
NOT own links (see §429.4).

## 429.4 — Creation is delegated to workbench Commands, not written by the sheet `[CERT]`

This is the load-bearing architectural fact. When the user drags between two slot terminals, `LinkState`
(the controller state entered on a slot hotspot) constructs and invokes a **workbench** command — it imports
`javax.baja.workbench.commands.LinkCommand` and `RelateCommand` and calls
`new RelateCommand(shell, s, t, sId).invoke()` / the link equivalent
(`sources/decompiled/wiresheet-wb/com/tridium/wiresheet/states/LinkState.java:20`,`:101`). `[CERT]` The
`BLink` is added to the component tree by that framework command, NOT by wiresheet code. `[CERT]` Likewise a
palette drop routes through `BWsCanvas.drop → WsController.insertTransferData` which uses `TransferUtil` to add
the `BComponent`. `[CERT]` `[INFER]` the wire sheet is a VIEW that issues standard workbench commands; the
model mutations (add component, add link) all go through the same undoable command layer any editor uses.

## 429.5 — Persistence: layout lives as a HIDDEN slot on the component `[CERT]`

Glyph position is NOT a separate sheet file. `WsController.saveAnnotation` writes a `BWsAnnotation` (holding
wixel `p, q, width`) onto the component itself as a HIDDEN slot named `wsAnnotation`
(`comp.add("wsAnnotation", anno, 1, tx)` where flag `1` = hidden, inside a `Transaction`;
`.../WsController.java:190`,`:192`). `[CERT]` If no annotation exists, glyphs auto-place diagonally
(`nextAutoAnnotation` steps by 4). `[CERT]` `[INFER]` because layout is a slot on the `BComponent`, the wire
sheet's visual arrangement is stored IN THE BOG and travels with the station save/backup — there is no
side-car layout file. Height is not stored (recomputed from slot count). `[CERT]`/`[INFER]`

## 429.6 — Undo: sheet edits are framework Commands `[CERT]`

Every sheet edit is a `WsCommand extends javax.baja.ui.Command`
(`.../commands/WsCommand.java:7`) — so it slots into the standard undo stack ([Block 427]: `BPane implements
UndoManager.Scope`). `[CERT]` `MoveGlyphsCommand` implements `undo` by inverting the delta and replaying
`redo`, which calls `saveAnnotations` to persist (`.../commands/MoveGlyphsCommand.java:29`,`:65`). `[CERT]`
The command family: `ArrangeCommand`, `DeleteLinksCommand`, `EditLinkCommand`, `PinSlotsCommand`,
`ResizeCommand`, … all extend `WsCommand`. `[CERT]`

## 429.7 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | `BWireSheet` `@AgentOn(baja:Component, requiredPermissions=W)` | `[CERT]` | `.../wiresheet/BWireSheet.java:31` |
| 2 | Canvas `BWsCanvas extends BTransferWidget` (not BCanvasPane); pane is BEdgePane→BScrollPane | `[CERT]` | `BWsCanvas.java:29`; `BWireSheetPane.java:85` |
| 3 | `RootGlyph` two layers (componentLayer/linkLayer); glyph holds live BComponent + handle | `[CERT]` | `RootGlyph.java:9`,`:10` |
| 4 | Link/component creation DELEGATED to `javax.baja.workbench.commands.LinkCommand`/`RelateCommand` | `[CERT]` | `states/LinkState.java:20`,`:101` |
| 5 | Layout persists as HIDDEN slot `wsAnnotation` (BWsAnnotation p/q/w) on the component, in a Transaction | `[CERT]` | `WsController.java:190`,`:192` |
| 6 | Sheet edits are `WsCommand extends Command`; MoveGlyphs inverts delta | `[CERT]` | `commands/WsCommand.java:7`; `MoveGlyphsCommand.java:29` |

**Marker tally**: `[CERT]` ≈ 20 · `[INFER]` 7 ([INFER]/[CERT] ≈ 0.35). Type: **EVIDENCE block** (model
overview) — ratio healthy. VERIFY-BEFORE-ACTING: the sweep's `LinkState.java:101 new LinkCommand(...)` was at
the wrong path — the real class is `states/LinkState.java`, and the raw decompile rendered `LinkCommand` as the
token `ln`; the PRESERVED copy resolves the clean import `javax.baja.workbench.commands.LinkCommand`, so the
delegation claim was re-confirmed, not taken on trust. No mangled classes in this module. Tokens verified:
`@AgentOn`, `extends BTransferWidget`, `wsAnnotation`+`comp.add(...,1,tx)`, `WsCommand extends Command`,
`componentLayer`/`linkLayer`, the `LinkCommand`/`RelateCommand` imports.

## 429.8 — Connections

- **[Block 427]** — glyphs are `BWidget`s; notably the canvas is a `BTransferWidget`, and the layout uses the
  same `UndoManager.Scope` from `BPane`.
- **[Block 428]** — the shell opens `BWireSheet` through the `@AgentOn` ORD→view pipeline; `baja:Component`
  match is why every component has a wire sheet.
- **WB04 (queued)** — the property sheet is the OTHER default view on the same `baja:Component`; both are
  `@AgentOn` views competing in `WbSys.getFilteredViewList`.
- **[Block 402]-[Block 413]** (database focus) — because `wsAnnotation` is a hidden slot, wire-sheet layout is
  part of the BOG this focus documented; it is saved/backed-up with the station, not separately.

<!-- research-block: focus workbench, gap WB03 (wire sheet editor) — CLOSED at body grade -->
