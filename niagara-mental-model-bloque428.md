# Block 428 — The Workbench shell: BWbShell hosts views in a tabbed pane, nav-tree selection hyperlinks an ORD to a @AgentOn view

> Research of the **Workbench shell + nav tree + sidebars + console** (focus `workbench`, gap WB02) — the
> container that hosts every editor/view. Scope: the shell class + its root layout, the nav-tree model and
> its selection→navigation seam, the sidebar registration mechanism, the console, and the ORD→view resolution
> that decides which editor opens. Builds on the widget foundation [Block 427]. Does NOT cover the property
> sheet / field editors (WB04), managers/tables (WB05), or commands/wizards (WB07).
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `workbench-wb.jar`
> sha256 `17a84e2a26a6f6af0e1893738115ebb1ac7e002d3af8c11409fa4ee17f3d7c8c`.
>
> Sources: ORIGINAL Tridium javadoc source preserved under `sources/tridium-src/workbench-wb/`
> (`javax.baja.workbench.*` public API — CAVEAT: mixed fidelity, some classes render the class-name token as
> `n` while parent types/members stay real); impl preserved under `sources/decompiled/workbench-wb/`
> (`com.tridium.workbench.*`, Vineflower — clean, no mangling in shell/nav/console). Method: docSource for the
> contract, Vineflower for impl, all load-bearing lines re-verified live. Markers: `[CERT]` (`file:line`) ·
> `[INFER]` deduction.
>
> Workbench UI framework. Connects [Block 427] (BWidget/BPane foundation this shell is built from),
> [Block 9] (UI-stack overview — this opens the shell at body grade), [Block 426] (BConsole seen there as the
> Program-compile output channel — §428.4 is what it is).

---

## 428.1 — The shell: BWbShell → BNiagaraWbShell, root layout = BWbPane `[CERT]`

The top-level Workbench window is `BWbShell` (`abstract`, `implements BIHyperlinkShell + BIActiveOrdShell`,
`sources/tridium-src/workbench-wb/javax/baja/workbench/BWbShell.java:31`); the concrete impl is
`BNiagaraWbShell`, whose content widget is a `BWbPane` root layout
(`sources/decompiled/workbench-wb/com/tridium/workbench/shell/BNiagaraWbShell.java:102`). `[CERT]` `BWbPane`
owns the four regions of the Workbench window: `[CERT]`

| Field | Type | Role | Citation |
|---|---|---|---|
| `views` | `BViewTabbedPane` | the tabbed area where editors/views open | `.../shell/BWbPane.java:49` |
| `sideBar` | `BSideBarPane` | the left sidebar host | `BWbPane.java:50` |
| `console` | `BConsole` | the output console panel | `BWbPane.java:52` |
| (splits) | `BSplitPane` | h/v splits arranging the three | `BWbPane.java` |

The active view is simply the selected tab: a view is shown by selecting its `BViewTab` in `views`, and
`BWbPane.update(tab)` (`tab.view` at `BWbPane.java:177`) syncs the menu/toolbar/locator/statusbar to it.
`[CERT]` `[INFER]` so "which view is active" is tab-owned state, not a separate active-view service.

## 428.2 — Nav tree: a BTree over ORD nodes, selection hyperlinks the node's ORD `[CERT]`

`BNavTree` extends `BTree implements NavListener` (`.../nav/tree/BNavTree.java` — parent types real; class-name
token is a decompiler artifact). `[CERT]` Its `NavTreeModel` keeps a `HashMap<BOrd,NavTreeNode> ordMap` for
O(1) event routing, and expansion is LAZY: `NavTreeNode.hasChildren()` returns a sentinel
(`navNode.hasNavChildren()`) without loading, and children are built only on first access. `[CERT]`/`[INFER]`

The load-bearing seam is selection → navigation: `NavTreeController.doSelectAction` takes the selected node's
ORD (`navNode.getNavOrd()`) and calls `((BWbShell)shell).hyperlink(new HyperlinkInfo(ord, DEFAULT))`
(`.../nav/tree/NavTreeController.java`). `[CERT]` So clicking a nav node does not "open a view" directly — it
hyperlinks the node's **ORD**, and the shell's ORD→view pipeline (§428.5) decides the editor.

## 428.3 — Sidebars: auto-discovered by type registry, no explicit registration `[CERT]`

A sidebar is a `BWbSideBar` (`abstract extends BWbPlugin`,
`sources/tridium-src/workbench-wb/javax/baja/workbench/sidebar/BWbSideBar.java:40`). `[CERT]` There is NO
registration call — sidebars are DISCOVERED from the type registry: `BWbSideBar.getInstalled()` queries
`Sys.getRegistry().getTypes(...)` for all concrete subtypes and sorts by display name. `[CERT]` They are
hosted by `BWbSideBarManager` (`extends BToolPane`); the default open sidebar is declared as a pickle string
`"workbench:NavSideBar;|toolpane=10000"` (`.../shell/BSideBarPane.java:24`). `[CERT]` `[INFER]` this is the
same "install a type, the framework finds it" agent pattern used across Niagara — a module contributes a
sidebar just by shipping a `BWbSideBar` subtype.

## 428.4 — Console: a persistent BEdgePane panel, not a view `[CERT]`

`BConsole` extends `BEdgePane` (`.../console/BConsole.java:29`) — a persistent panel below the view area, NOT
a `BWbView`. `[CERT]` It wraps a `ConsoleShell` writing through a `ConsoleWriter` `PrintStream`, buffers lines
in a `BConsoleBuffer` (a bounded ring), and exposes `appendLine(String)` (`BConsole.java:81`); a
`BConsoleEntry` text field runs Enter=exec / Up-Down=history. `[CERT]` This is exactly the channel
[Block 426] used: the Program compiler's `BConsole.exec(cmd, callback)` runs `javac` and streams output here.

## 428.5 — The hosting contract: ORD → filtered view agents → instance `[CERT]`

The heart of the shell is `NHyperlinkInfo` (`.../shell/NHyperlinkInfo.java`), the pipeline that turns a
hyperlinked ORD into a mounted view: `[CERT]`

```
this.target     = this.resolve();            // :121  ORD → OrdTarget (Fox auth if remote)
this.viewAgents = this.getViewAgentsList();  // :136 → :415  the candidate views for this target
this.agent      = this.getAgent();           // ViewQuery in the ORD, else the default agent
this.view       = (BWbView) agent.getInstance();   // instantiate the registered view
```

The candidate list comes from `WbSys.getFilteredViewList(...)`
(`sources/tridium-src/workbench-wb/javax/baja/workbench/WbSys.java:88`), which filters registered agents to
`BWbView | BAbstractPxView | BIFormFactorMax` (¬WebOnly) AND gates by profile (`hasView`) and app-name.
`[CERT]` Views register declaratively with `@AgentOn(types={...})` on the view class — e.g. the default
nav-container table view `BNavContainerView` is `@AgentOn(types={"baja:NavContainer"})`
(`.../nav/BNavContainerView.java:26`). `[CERT]` A trailing `ViewQuery` in the ORD overrides the default view;
PX views get wrapped in a `BWbPxView`. `[INFER]` So the shell never hard-codes editors — the ORD's TYPE plus
`@AgentOn` registrations, filtered by profile, choose the view. This is the same agent mechanism [Block 427]'s
theming and [Block 210-215]'s field editors use.

## 428.6 — Two supplementary mechanisms `[CERT]`

- **NavMonitor** (`.../nav/NavMonitor.java:27`) — a background thread polling every `SCAN_RATE` (default
  `20000` ms, `niagara.nav.touch.scanRate`) that collects visible `BISpaceNode`s across all open nav trees and
  `touchNodes(...)` them, keeping remote station node state fresh without user action. `[CERT]`
- **BWorkbenchScheme / BToolScheme** — `BOrdScheme`s that provide the `workbench:` and `tool:` ORD namespaces
  (`workbench:/help`, `workbench:/tools/<name>`, `workbench:/licenses/...`). `[CERT]`/`[INFER]` these are how
  the shell's own pseudo-locations resolve as ORDs through the same §428.5 pipeline.

## 428.7 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | Shell = `BWbShell` (abstract) → `BNiagaraWbShell`; content = `BWbPane` (views/sidebar/console/splits) | `[CERT]` | `BWbShell.java:31`; `BWbPane.java:49`–`:52` |
| 2 | Active view = selected `BViewTab` (`tab.view`), tab-owned | `[CERT]` | `BWbPane.java:177` |
| 3 | `BNavTree extends BTree/NavListener`; lazy expansion; `ordMap` O(1) | `[CERT]` | `NavTreeController.java`; §428.2 |
| 4 | Nav selection → `shell.hyperlink(node.getNavOrd())` — hyperlinks an ORD, not a view | `[CERT]` | `NavTreeController.java` |
| 5 | Sidebars auto-discovered via `Sys.getRegistry().getTypes` — no registration; default NavSideBar pickle | `[CERT]` | `BWbSideBar.java:40`; `BSideBarPane.java:24` |
| 6 | `BConsole extends BEdgePane` (persistent panel), `appendLine`; the B426 compile channel | `[CERT]` | `BConsole.java:29`,`:81` |
| 7 | ORD→view = `NHyperlinkInfo` resolve→getViewAgentsList→getInstance; `@AgentOn(types)`; profile-filtered | `[CERT]` | `NHyperlinkInfo.java:121`,`:136`; `WbSys.java:88` |
| 8 | NavMonitor polls every 20 s (`niagara.nav.touch.scanRate`) to keep remote nodes fresh | `[CERT]` | `NavMonitor.java:27` |

**Marker tally**: `[CERT]` ≈ 24 · `[INFER]` 7 ([INFER]/[CERT] ≈ 0.29). Type: **EVIDENCE block** (model
overview) — ratio healthy. VERIFY-BEFORE-ACTING: the first sweep returned only a tail (it nested a sub-agent);
the consolidated Q1–Q5 were re-requested and then EVERY load-bearing line re-verified live against the
preserved sources (WbSys line drifted 111→**88** in the sweep — the verified line is cited, not the sweep's).
No mangling in shell/nav/console packages. Tokens confirmed: `BWbShell`, `BViewTabbedPane`/`BSideBarPane`/
`BConsole` fields, `getFilteredViewList`, `defaultPickle`, `@AgentOn baja:NavContainer`, `SCAN_RATE 20000`.

## 428.8 — Connections

- **[Block 427]** — the shell, panes, tree, and console are all `BWidget`/`BPane` subclasses; this block is
  the concrete Workbench window built from that foundation.
- **[Block 426]** — `BConsole` here is the output channel the Program compiler streams `javac` into.
- **[Block 210]–[Block 215]** — the `@AgentOn` view-agent mechanism (§428.5) is the same one the PX editor's
  field editors use; the shell chooses editors the same way.
- **WB04 (queued)** — when the selected view IS a property sheet, §428.5 lands on `BPropertySheet`; that view's
  internals are the next gap.

<!-- research-block: focus workbench, gap WB02 (shell + nav tree + sidebars + console) — CLOSED at body grade -->
