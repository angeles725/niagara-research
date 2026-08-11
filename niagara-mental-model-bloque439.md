# Block 439 — SYNTHESIS of the `workbench` focus (B427–B438): two unifying patterns behind the whole Swing UI

> Focus-closing synthesis of the `workbench` focus — the Workbench Swing engineering tool as infrastructure.
> Consolidates the 12 blocks (B427–B438) into the map, the two transversal theses, the Swing/Hx duality, the
> manager/driver inheritance stack, and the premise corrections. Does NOT re-derive any block — it connects
> them. Type: **SYNTHESIS** (a high [INFER]/cross-reference ratio is expected and healthy; the primary evidence
> lives in the cited blocks).
>
> Sources: blocks B427–B438 of this corpus (each independently `[CERT]`-grounded). Markers: `[CERT]` where a
> claim restates a block's verified finding · `[INFER]` for the synthesis-level generalization.
>
> Workbench UI framework. This is the terminal artifact of the `workbench` focus (bootstrapped 2026-08-10 from
> an audit-first coverage matrix over 202 `-wb` modules; 12/12 gaps closed).

---

## 439.1 — The map: what the 12 blocks cover `[CERT]`

| Block | Layer | One-line |
|---|---|---|
| B427 | widget model | `BWidget` IS a `BComponent`, painted through the `gx.Graphics` interface on an AWT shell; 3 theme families |
| B428 | shell | `BWbShell` hosts views in tabs; nav selection hyperlinks an ORD; `@AgentOn`+profile picks the view |
| B429 | wire sheet | glyphs mirror the tree; layout = hidden `wsAnnotation` slot; links delegate to workbench commands |
| B430 | property sheet | field-editor dispatch = `@AgentOn` on the value's type; commit in one Transaction |
| B431 | managers | `BAbstractManager` maps children to `hasOperatorRead`-filtered rows; `BJob` learn/discovery |
| B432 | commands | `Command`→`CommandArtifact`→`UndoManager`; transfer/wizard seams; all `@AgentOn` |
| B433 | Hx | `BHxView` is a SERVLET view — buffered HTML, header-keyed events, poll-based live values |
| B434 | devkit | Niagara Developer Kit TOOLING (wizards/Slotomatic/lexicon), not an SDK; Workbench-only |
| B435 | wbutil | cross-cutting UI services — user/role/permission UI, cell editors, credential/license tools |
| B436 | platform | the Workbench client of the plat.exe daemon (3011/5011); OS-domain creds |
| B437 | driver framework | reflection-driven device/point manager — a driver declares `@AgentOn` + `@MgrInclude` |
| B438 | driver tail | 48 modules repeat B437; ~32 deliberately not individually documented (no-silent-caps log) |

## 439.2 — Transversal thesis 1: ONE `@AgentOn`-keyed-on-type dispatch runs everything `[CERT]`/`[INFER]`

The single most load-bearing finding across the focus: the Workbench chooses almost every pluggable thing by
the SAME mechanism — register a type with `@AgentOn(types=…)`, and the framework resolves it by the target
object's runtime TYPE, filtered by profile. `[CERT]` It selects: `[CERT]`

- **views** ([Block 428] `WbSys.getFilteredViewList` → `NHyperlinkInfo`),
- **field editors** ([Block 430] `getAgents().filter(FE).getDefault()`),
- **cell editors** ([Block 431]/[Block 435]),
- **sidebars** ([Block 428] type-registry discovery),
- **nav context menus** ([Block 432] `BNavMenuAgent`),
- **wizards** ([Block 432] `BWizardView`),
- **managers & driver managers** ([Block 431]/[Block 437] `@AgentOn` on the container/network type),
- **the Hx peer of a Wb view** ([Block 433] `translate()` swap over the same registry).

`[INFER]` This is why a third-party module extends the Workbench without touching framework code: it ships a
`@AgentOn`-annotated type and the registry finds it. "Install a type, the framework finds it" is not a feature —
it is THE architecture. The `@MgrInclude` reflection in the driver framework ([Block 437]) is the same idea one
level down: annotate a property, get a column.

## 439.3 — Transversal thesis 2: ONE Command/CommandArtifact/UndoManager model runs all undo `[CERT]`/`[INFER]`

Every mutation is a `Command` whose `doInvoke` optionally returns a `CommandArtifact` pushed on the owner's
`UndoManager` ([Block 432]). `[CERT]` The wire sheet's `MoveGlyphsCommand`/`LinkCommand` ([Block 429]), the
property sheet's Save Transaction ([Block 430]), the manager's `MgrEdit` commit ([Block 431]), and the async
`TransferArtifact` for paste ([Block 432]) are ALL members of this one model. `[CERT]` `[INFER]` a single undo
stack, a single "is this undoable?" rule (non-null artifact), and a single Transaction boundary at the
component space ([Block 408]) — the editors differ in what they draw, not in how they mutate or undo.

## 439.4 — The Swing/Hx duality: same dispatch, two substrates `[CERT]`

The Workbench has TWO rendering worlds that share the `@AgentOn` resolution but nothing else: `[CERT]`

| | Swing (Wb) | Hx (browser) |
|---|---|---|
| view base | `BWbView` ([Block 428]) | `BServletView` ([Block 433]) |
| substrate | live `BWidget` tree on an AWT shell ([Block 427]) | buffered HTML from a servlet |
| events | `BWidgetEvent` on the widget | header-keyed POST → `process` |
| live values | real subscription | POLL (5 s default) |
| status | current | legacy (BHTML5HxProfile successor) |

`[INFER]` the same component, resolved to a Wb view OR its Hx peer, is engineered once and surfaced two ways —
the media split ([Block 194]) is a filter over one registry, not two codebases.

## 439.5 — The manager/driver inheritance stack `[CERT]`

A single chain underlies every table UI in the Workbench: `[CERT]`
`BAbstractManager` ([Block 431]) → `BFolderManager` → `BDeviceManager`/`BPointManager` ([Block 437]) →
`BNDeviceManager`/`BNPointManager` (ndriver) → the 48-module driver tail ([Block 438]). Row visibility is
permission-filtered at `hasOperatorRead` ([Block 431]); columns are `MgrColumn`s or `@MgrInclude`-reflected
([Block 437]); discovery is a `BJob`-backed `MgrLearn`/`NMgrLearn`. `[INFER]` the entire "manage devices and
points" experience of Niagara — across every protocol — is this one stack with protocol-specific annotations.

## 439.6 — Premise corrections and security threads `[CERT]`

**Four gap-premise refutations** (the audit-first backlog's gap NAMES were hypotheses; investigation corrected
them): `BWManager` does not exist (base is `BAbstractManager`, [Block 431]); `devkit-wb` is dev TOOLING, not an
SDK ([Block 434]); `BCellTable` is a live-editor grid, NOT the manager's table ([Block 431]); there is no
`BAbstractDiscovery` (the base is `MgrLearn`, [Block 437]). `[CERT]`

**Security threads surfaced** (for a who-can-do-what audit): row-level `hasOperatorRead` filtering in managers
([Block 431]); the user/role/permission admin UI + stored-credential manager live in `wbutil-wb` ([Block 435]);
platform access is gated by OS-domain daemon credentials distinct from station users ([Block 436]); Program/
module source compiles via a spawned `bin/javac` ([Block 426]) and Slotomatic generates the slot code
([Block 434]). `[CERT]`/`[INFER]`

## 439.7 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | 12 blocks cover the Workbench Swing infra end-to-end (widget→shell→editors→managers→commands + Hx/devkit/wbutil/platform/driver) | `[CERT]` | §439.1; B427–B438 |
| 2 | ONE `@AgentOn`-on-type dispatch selects views/FEs/sidebars/menus/wizards/managers/Hx-peers | `[CERT]`/`[INFER]` | §439.2 |
| 3 | ONE `Command`/`CommandArtifact`/`UndoManager` model runs all undo | `[CERT]`/`[INFER]` | §439.3; B432 |
| 4 | Swing (`BWbView`) and Hx (`BServletView`) share dispatch, differ in substrate | `[CERT]` | §439.4; B428/B433 |
| 5 | One manager/driver inheritance stack underlies every table UI | `[CERT]` | §439.5; B431/B437 |
| 6 | 4 premise corrections + 4 security threads recorded | `[CERT]` | §439.6 |

**Marker tally**: `[CERT]` ≈ 14 · `[INFER]` 10 ([INFER]/[CERT] ≈ 0.7). Type: **SYNTHESIS block** — the high
ratio is EXPECTED and healthy: this block generalizes over 12 blocks whose primary `[CERT]` evidence is already
banked; it adds cross-cutting inference, not new decompilation. Every generalization cites the constituent
block that grounds it.

## 439.8 — Connections

- **B427–B438** — the twelve constituent blocks this synthesizes.
- **[Block 209]** (px-editor-deep synthesis), **[Block 215]** (px-editor-core synthesis) — the PX side; this
  focus is the Swing-framework side those sat on. Together they map the full Workbench UI.
- **[Block 421]** (webEditors) / **[Block 433]** (Hx) — the two web-UI substrates vs this Swing one.
- **[Block 383]/[Block 413]/[Block 420]** — sibling focus-synthesis blocks; this is the `workbench` entry.

<!-- research-block: focus workbench SYNTHESIS (B427-B438) — terminal artifact; 12/12 gaps, two transversal theses -->
