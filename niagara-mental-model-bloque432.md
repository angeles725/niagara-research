# Block 432 — Commands, undo, transfer, and wizards: the Workbench extension seams and the undo model every editor plugs into

> Research of the Workbench **command + wizard + transfer** infrastructure (focus `workbench`, gap WB07) —
> the pluggable seams module developers hook into, and the `Command`/`CommandArtifact` undo model that
> [Block 429]-[Block 431]'s editors all use. Scope: the `Command` base + undo stack, how views contribute
> menus/toolbars, the workbench command set, the transfer (copy/paste/DnD) model, the wizard framework, and
> the four extension hooks. Closes the framework arc WB01-WB05.
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `bajaui-wb.jar`
> sha256 `391dfdd3d80d594e044654688f249182f181ea809139ff0c2e0b95b033b6dfa5` · `workbench-wb.jar`
> sha256 `17a84e2a26a6f6af0e1893738115ebb1ac7e002d3af8c11409fa4ee17f3d7c8c`.
>
> Sources: Tridium docSource (`sources/tridium-src/bajaui-wb/.../ui/Command.java`,
> `.../workbench/view/BWbView.java`) + Vineflower (`sources/decompiled/workbench-wb/.../{shell,transfer,wizard}/`).
> Method: docSource for contracts, Vineflower for impl, all lines re-verified live. Markers: `[CERT]`
> (`file:line`) · `[INFER]` deduction.
>
> Workbench UI framework. Connects [Block 427] (`UndoManager.Scope` on BPane), [Block 429]/[Block 431]
> (WsCommand / manager commands ARE these), [Block 430] (Save is a Command), [Block 428] (the shell chrome
> these contribute to).

---

## 432.1 — Command: a concrete class whose CommandArtifact IS the undo unit `[CERT]`

`javax.baja.ui.Command` is a CONCRETE class (`sources/tridium-src/bajaui-wb/javax/baja/ui/Command.java`). Its
`final invoke(CommandEvent)` calls the overridable `doInvoke()`, and if that returns a non-null
`CommandArtifact`, pushes it on the owner widget's `UndoManager` (`Command.java:275`,`:283`,`:287`): `[CERT]`

```java
CommandArtifact artifact = doInvoke(event);          // :283
if (artifact != null && owner != null) { um.addArtifact(artifact); }  // :287
```

`CommandArtifact` is a two-method interface — `undo()` / `redo()` (`CommandArtifact.java:15`,`:21`,`:26`).
`[CERT]` `UndoManager` keeps two stacks (undos/redos, default max 10); `addArtifact` clears the redo stack.
`[CERT]` `[INFER]` this is THE undo model of the whole Workbench: a command is undoable iff its `doInvoke`
returns an artifact — [Block 429]'s `MoveGlyphsCommand`, [Block 431]'s manager edits, and [Block 430]'s Save
all obey this one rule. A command returning `null` (e.g. `StationSaveCommand`) is simply non-undoable. `[CERT]`

## 432.2 — Contribution: a view overrides getViewMenus/getViewToolBar `[CERT]`

There is NO explicit `CommandSet`/`CommandGroup` type. A view contributes chrome by overriding three methods on
`BWbView`: `getViewMenus()` → `BIMenu[]`, `getViewToolBar()` → `BIToolBar`, `getViewStatusBarSupplement()`
(`sources/tridium-src/workbench-wb/javax/baja/workbench/view/BWbView.java:72`,`:86`). `[CERT]` On view
activation the shell's `WbCommands` aggregate calls `updateView(view)`, re-reading those methods to rebuild the
menu bar and toolbar. `[CERT]`/`[INFER]` so command contribution is per-active-view: the chrome is whatever the
focused view supplies, merged with the shell's standing commands.

## 432.3 — The Edit command set routes through the transfer widget `[CERT]`

Clipboard command IDs are constants on `BWbView` — `CUT=0, COPY=1, PASTE=2, … PASTE_SPECIAL=11`
(`BWbView.java:158`–`:169`). `[CERT]` The shell's `WbCommands` builds one `PluginCommand` per ID
(`cut/copy/paste/…`, `.../shell/WbCommands.java:112`–`:115`), and each `PluginCommand.doInvoke` routes to
`shell.getActiveView().invokeCommand(id)` (`BWbView.invokeCommand`, `BWbView.java:228`) — which delegates to the
view's installed `BTransferWidget`. `[CERT]` `[INFER]` Edit-menu Cut/Copy/Paste are not per-view reimplemented;
they are one shell command that dispatches to whatever transfer widget the active view installed.

## 432.4 — Transfer: a Mark on the clipboard, an async undoable TransferArtifact `[CERT]`

`BTransferWidget` (abstract, `bajaui.../transfer/BTransferWidget.java:18`) is the copy/paste/DnD seam — three
abstract methods a view implements: `getTransferData()` (what is selected), `insertTransferData(cx)` (paste/drop
target), `removeTransferData(cx)` (cut source). `[CERT]` The clipboard currency is a **`Mark`** — a bag of
`BObject[] values` + `String[] names` — wrapped in a `TransferEnvelope` at format `TransferFormat.mark` (the
DataFlavor analogue). `[CERT]`/`[INFER]` A paste/drop becomes a `TransferArtifact implements CommandArtifact`
(`.../transfer/TransferArtifact.java:22`) whose `redo()` runs a `TransferStrategy` on a Worker thread with a
delayed progress dialog, and whose `undo()` calls the `TransferResult.undo()` (`TransferArtifact.java:52`).
`[CERT]` `[INFER]` so cross-station paste is asynchronous AND undoable — it rides the same
`Command`/`UndoManager` model as everything else.

## 432.5 — Wizard: BWizardView + a WizardViewModel of steps `[CERT]`

There is no public `BWizard`/`WizardStep` type. The wizard seam is `BWizardView extends BWbComponentView`
(`.../wizard/BWizardView.java:29`) with four Command-backed buttons (back/next/finish/cancel) delegating to an
abstract inner `WizardViewModel` (`BWizardView.java:31`,`:56`). `[CERT]` `doLoadValue` auto-calls
`init(makeViewModel())` — a subclass overrides `makeViewModel()` (`:69`). `[CERT]` The step-based variant
`StepWizardViewModel` wraps a `StepModel` whose `next(view)`/`back(view)` swap the center content pane per page.
`[INFER]`

## 432.6 — The four extension seams `[CERT]`/`[INFER]`

A module developer extends the Workbench through exactly four hooks (§synthesis of the above): `[CERT]`/`[INFER]`

| Seam | How | Registration |
|---|---|---|
| **View chrome** | subclass `BWbView`, override `getViewMenus`/`getViewToolBar` | `@AgentOn` view binding on the target type ([Block 428]) |
| **Clipboard** | `BTransferWidget` (3 abstract methods) + `setTransferWidget()` | in the view; `setCommandEnabled(CUT,…)` toggles menu items |
| **Nav context menu** | subclass `BNavMenuAgent`, override `doMakeMenu` | `@AgentOn` on the target `BObject` type |
| **Wizard** | subclass `BWizardView`, override `makeViewModel` | standard view binding |

`[INFER]` the through-line: EVERY seam is the same Baja `@AgentOn` "install a type, the framework finds it"
pattern ([Block 428] views, [Block 430] field editors) — plus imperative `Command` instantiation for one-shot
actions (`new LinkCommand(...).invoke()`). There is no separate command/wizard manifest/descriptor. `[CERT]`

## 432.7 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | `Command.invoke`→`doInvoke`→non-null `CommandArtifact`→`UndoManager` (undo stack, max 10) | `[CERT]` | `Command.java:283`,`:287` |
| 2 | `CommandArtifact` = `undo()`/`redo()` interface — the universal undo unit | `[CERT]` | `CommandArtifact.java:15`,`:21` |
| 3 | Contribution = `BWbView.getViewMenus/getViewToolBar`; no CommandSet type; shell merges on activation | `[CERT]` | `BWbView.java:72`,`:86` |
| 4 | Edit IDs (CUT=0…PASTE_SPECIAL=11) → `PluginCommand`→`invokeCommand(id)`→`BTransferWidget` | `[CERT]` | `BWbView.java:158`,`:228`; `WbCommands.java:112` |
| 5 | Transfer seam = `BTransferWidget` (3 abstract); clipboard = `Mark` at `TransferFormat.mark` | `[CERT]` | `BTransferWidget.java:18`,`:145` |
| 6 | Paste = `TransferArtifact implements CommandArtifact`, async `TransferStrategy`, undoable | `[CERT]` | `TransferArtifact.java:22`,`:52` |
| 7 | Wizard = `BWizardView` + abstract `WizardViewModel` (back/next/finish/cancel); step variant wraps `StepModel` | `[CERT]` | `BWizardView.java:29`,`:56` |
| 8 | All four seams use the same `@AgentOn` view/agent binding | `[INFER]` | §432.6 |

**Marker tally**: `[CERT]` ≈ 26 · `[INFER]` 8 ([INFER]/[CERT] ≈ 0.31). Type: **EVIDENCE block** (model
overview) — ratio healthy. VERIFY-BEFORE-ACTING: every structural line re-verified live against the preserved
sources. The one mangling note from the sweep (`WbCommands$n` = `PluginCommand`) was confirmed as a benign
inner-class artifact; all cited bodies are clean. Tokens confirmed: `Command.invoke`/`addArtifact`,
`interface CommandArtifact`, `getViewMenus`/`getViewToolBar`, `CUT=0`/`invokeCommand`, `BTransferWidget`
abstract methods, `TransferArtifact implements CommandArtifact`, `BWizardView`/`WizardViewModel`.

## 432.8 — Connections

- **[Block 427]** — `BPane implements UndoManager.Scope`; this block is the `Command`/`CommandArtifact` model
  that scope hosts.
- **[Block 429]/[Block 431]** — `WsCommand` and the manager's `MgrCommand`/`LinkCommand` ARE members of this
  command framework; their undo is a `CommandArtifact`.
- **[Block 430]** — the property-sheet Save is a `Command`; its Transaction is what its `doInvoke` runs.
- **[Block 428]** — the menus/toolbars a view contributes here are merged into the shell chrome documented
  there; the `@AgentOn` binding is the same.
- **Framework arc WB01-WB07 (B427-B432)** — this closes the six-block Workbench Swing framework model:
  widget (427) → shell (428) → wire sheet (429) → property sheet (430) → managers (431) → commands (432).

<!-- research-block: focus workbench, gap WB07 (command + wizard + transfer) — CLOSED at body grade; completes the WB01-WB07 framework arc -->
