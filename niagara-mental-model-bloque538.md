# Block 538 — The OFFICIAL control-logic programming rules: linking, wire-sheet, action, and composite rules from the Tridium guides, reconciled with the code kernel

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC3 — the programming RULES of control modules)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY doc synthesis. Delegated sonnet sweep over the niagara-help guide corpus; the
load-bearing rules token-verified inline against the guide files.
**Primary source** `[CERT-doc]`: `niagara-help/guides-clean/` — the official Tridium engineering guides
(User/, EngNotes/, KitControl/, Scheduling/, Ace/). First corpus citations of `aPriorityLinkRules.txt`,
`aWritablePoints.txt`, `aObjectStatusPropagation.txt`, `aPriorityInputScan.txt`, `BasicObjectLinking.txt`,
`aComposites.txt`, `aCompositeIssues.txt`.

**Block TYPE**: DOC-SYNTHESIS (`[CERT-doc]` primary). A high `[CERT-doc]` ratio is EXPECTED and healthy —
this block captures the vendor's STATED rules, then reconciles them with the code behavior established in
[Block 6] §6.2 (link kernel), [Block 536] (writable-point arbitration), and [Block 537] (FB catalog).

**Scope**: the RULES an engineer must follow to program control logic — what may legally link to what, the
wire-sheet workflow, action/trigger semantics, composites, and the documented best-practice constraints. It
does NOT re-derive the code link kernel ([Block 6] §6.2 is REMITTANCE); it adds the OFFICIAL-rules layer the
corpus never cited and reconciles the two.

---

## 538.1 Linking rules [CERT-doc]

- **R1 — Links are directional, source→target.** The left column of the Link window is the source, the right
  the target; the wire is drawn source→target. `[CERT-doc] BasicObjectLinking.txt:12,40`.
- **R2 — A link is OWNED BY THE TARGET.** The link object stores `Source Ord`, `Source Slot Name`,
  `Target Slot Name`, `Enabled` and lives as a child of the target component. `[CERT-doc] EditingLinks.txt:24`.
  CONFIRMS [Block 6] §6.2.2 ("target is active, holds the link") and §6.2.3 (the knob is the source-side
  mirror).
- **R3 — Legal gesture / illegal slots.** Drag from an In/Out node until it highlights, release on a valid
  slot; **dimmed slots are not selectable** (illegal link targets). `[CERT-doc] BasicObjectLinking.txt:28,43`.
- **R4 — One-to-many / many-to-one via Link Mark.** "Link Mark" + "Link From"/"Link To" builds multiple
  links; Shift-drag chains multiple targets from one source in one gesture. `[CERT-doc]
  CreatingMultipleLinksAtTheSameTime.txt:12; ContinuousObjectLinking.txt:12`.
- **R5 — Target actions/topics accept MULTIPLE sources; target PROPERTY slots accept only one.**
  `[CERT-doc] AboutTriggerSchedules-D6677742.txt:25`.
- **R6 — Conversion links are AUTOMATIC on a type mismatch** (N4/AX-3.6+): linking two dissimilar-typed slots
  auto-inserts a `BIConverter` child; no Conversion-folder component needed. `[CERT-doc]
  docEn2_ConversionLinks.txt:13`. This is the doc face of [Block 6] §6.2.5's `BConversionLink`.
- **R7 — The conversion type matrix is a 17×17 From→To table** (string/boolean/double/float/long/integer/
  frozenEnum/dynamicEnum/status*/ord/time/absTime/relTime). Notable: **frozenEnum links to nothing**; `ord`
  only to/from string. `statusBoolean→statusNumeric`: active→1, inactive→0, null passes; reverse: 0→false,
  any non-zero→true. `[CERT-doc] aTypesOfConverterLinks.txt; aDataTransformViaConverterLink.txt:22`.

## 538.2 The priority-input link rules — the KEY reconciliation with [Block 536] [CERT-doc + CERT]

The official rules for linking into a writable point's 16 levels [CERT-doc] `aPriorityLinkRules.txt:21-25`
(token-verified):
- **Only ONE link per input level** — no two sources into the same `InN`.
- **Levels 1 and 8 are UNAVAILABLE for links.** On a Boolean writable, **level 6 is also unavailable**.
- Levels 1 and 8 are reserved for the emergency/override ACTIONS; Boolean level 6 is reserved for
  **minimum on/off times**.

`aWritablePoints.txt:48,62` (token-verified) labels In1 "Emergency (Manual Life Safety) — Unlinkable input,
but available as action (command)" and In8 "Override (Manual Operator) — Unlinkable input, but available as
action."

**Reconciliation** — this is the DOC explaining WHY the [Block 536] code behaves as it does:
- B536 found in1/in8 are the `READONLY`-persisted slots with dedicated `emergencyOverride`/`override`
  actions. The doc now states the RULE the code enforces: those levels are **unlinkable, action-only**. CODE
  (mechanism) and DOC (rule) agree. `[CERT]` B536 ↔ `[CERT-doc]` aWritablePoints.
- **NEW, refines B536**: on a **Boolean** writable, **level 6 (In6) is also unlinkable**, reserved for the
  built-in minimum on/off timers. B536 (numeric-focused) did not surface this Boolean-only reservation — it
  is a genuine addition to the writable-point model, not in the numeric arbitration path.
- **Schedule convention**: a weekly schedule's `Out` links to `In16` (lowest priority) by convention.
  `[CERT-doc] AboutLinkingWeeklySchedules-CE3CC2F7.txt:20`.

## 538.3 Wire-sheet rules [CERT-doc]

- **W1 — The wire sheet is a LIVE view** of a component's children; double-click a component to open it.
  `[CERT-doc] wiresheet-WireSheet-...txt:9`.
- **W2 — Links can be DELETED only on the wire sheet** (not the property sheet). `[CERT-doc]
  wiresheet-WireSheet-...txt:213`.
- **W3 — Pin Slots** makes glyph slots directly clickable, skipping the Link dialog. `[CERT-doc]
  ID-1228-000006b2.txt:52`.
- **W4 — Off-view links show as KNOBS.** A link to a component on another sheet renders a link-knob;
  "GoTo Link" / "GoTo Linked Component" navigate across. `[CERT-doc] workingwithWiresheet.txt:24,29`.
  ("Knob" is the exact vendor term — CONFIRMS [Block 6] §6.2.3's kernel `Knob`, here as the UI artifact.)
- **W5 — Execution order: EVENT-DRIVEN, with NO topological-order guarantee on a standard wire sheet.**
  `[CERT-doc] aPriorityInputScan.txt:18` (token-verified): "Like almost all control execution, this priority
  scan is event-driven, meaning it occurs when any input value changes." The standard wire sheet does NOT
  document a deterministic execution order for a kitControl chain — computation ripples on change. This
  answers the audit's "execution-order guarantee" question honestly: **there is none on a standard sheet.**
- **W6 — The ACE (edge controller) wire sheet is the EXCEPTION**: it exposes `Level` (scan-frequency
  multiplier of the Scan Period, default 50 ms) and `Order` (execution order within a level), with a
  "Force Order" button that walks link chains to assign order. `[CERT-doc] ace-AceCompManager.txt:23`.
  So deterministic ordering exists ONLY in the ACE subsystem, not general N4 control logic — a load-bearing
  distinction for anyone porting logic between a supervisor station and an edge controller.

## 538.4 Action / trigger rules [CERT-doc]

- **A1 — Three slot types**: Property (storage; link source or target), Action (behavior; invoked by user
  command OR by a link/event), Topic (event-source placeholder, no storage/behavior; source side of a
  trigger link). `[CERT-doc] aboutComponents.txt:59`. Matches [Block 6] §6.2.1's link taxonomy.
- **A2 — Actions are SYNCHRONOUS by default; the `async` flag COALESCES and defers to the engine thread.**
  `[CERT-doc] help-BajadocViewer-...txt:129` (token-verified): "By default Action are invoked synchronously
  on the callers thread. By using the async flag on an Action, invocations are coalesced and executed
  asynchronously … on the engine's [thread]." This is the doc source for [Block 6] §6.2.7's `Flags.ASYNC`
  feedback-loop mitigation — async is the documented way to break synchronous recursion.
- **A3 — Trigger→Action**: a trigger schedule / time trigger fires a Topic; the link invokes the target
  Action (commonly a point-extension action, not a property). `[CERT-doc] AboutTriggerSchedules-...txt:12`.
- **A4 — `executeOnChange` slot flag** makes a component (e.g. an Expr) re-execute when that input changes.
  `[CERT-doc] ID-1228-000006b2.txt:34`.

## 538.5 Composite rules [CERT-doc]

- **C1 — A composite EXPOSES child slots on the parent glyph** to simplify linking and promote reuse.
  `[CERT-doc] aComposites.txt:11`.
- **C2 — OFFICIAL CAUTION: avoid FOLDER composites; composite only at the point/object level.** "you should
  avoid making folder composites in your control logic, and instead use the composite feature only at the
  point/object level to expose extension slots (if necessary)." `[CERT-doc] aComposites.txt:22`.
- **C3 — Each exposed slot IS A LINK and costs station resources**; excessive composites reduce station
  capacity. `[CERT-doc] aCompositeIssues.txt:21`.
- **C4 — Linking a DYNAMIC value (e.g. a proxy point's `out`) into a composite PINS it subscribed** — that
  proxy then polls permanently regardless of other usage. `[CERT-doc] aCompositeIssues.txt:12`. A concrete
  performance footgun.

## 538.6 Best-practice / constraint rules [CERT-doc]

- **BP1 — Status propagation on Math/Logic is OPT-IN (off by default) via `Propagate Flags`**; multiple
  inputs use OR logic; four of five flag types (all but alarm/overridden) are "invalid" and mark downstream
  targets invalid. `[CERT-doc] aObjectStatusPropagation.txt:12,33,39` (token-verified). This is the doc rule
  behind [Block 537]'s status-OR in the multi-input null contract.
- **BP2 — Status NEVER propagates INTO a control point.** `[CERT-doc] aObjectStatusPropagation.txt:49`
  (token-verified): "never propagates to any point." Propagation is kitControl→kitControl only — the point
  boundary stops it. Adds a rule neither B536 nor B537 stated.
- **BP3 — Fallback null**: to force a null `Out` when all 16 levels relinquish, set the Fallback slot's
  **Hidden** flag; otherwise the default `set` action lets an operator write Fallback. `[CERT-doc]
  aPriorityInputScan.txt:39,43` (token-verified). Refines [Block 536] §536.5 (set→fallback) with the
  null-guarantee technique.
- **BP4 — Component naming rules**: alphanumeric + underscore only, first char a letter, unique within
  parent, case-sensitive. `[CERT-doc] aboutComponents.txt:115`.
- **BP5 — kitControl placement: Philosophy B (co-locate logic under each device's Points) is preferred** over
  a central Logic folder (Philosophy A), which breeds off-view knobs and harder-to-follow logic. `[CERT-doc]
  WhereToLocateKitControlComponents-4161DFC8.txt:18`.
- **BP6 — Use the `Expr` (BQL expression) component instead of a Program or many components for simple
  logic** — no compilation, fewer components; set `executeOnChange` per input. `[CERT-doc]
  docEN2_BQL_ExprComponent.txt:20`. (Bears on KC6 — Program is the heavier tool.)
- **BP7 — Extensions attach only to kitControl objects that have a ProxyExt**; adding one to a block that
  doesn't support extensions yields an "illegal parent" error. `[CERT-doc]
  ExtensionsAndKitControlComponents-41621597.txt:13`.
- **BP8 — LON mis-link caution**: do NOT link proxy points of one device directly to another device's proxy
  points; link at the network level. `[CERT-doc] LinkingAndBindingUsingAWireSheet-Lo-6A3562B6.txt:26`.

## 538.7 Official vocabulary (vendor-exact terms)

Wire Sheet · Glyph · Slot (Property/Action/Topic, frozen/dynamic) · Link (owned by target; Source Ord) ·
Knob / link-knob (off-view navigation) · Link Mark / Link From / Link To · Conversion link (auto BIConverter)
· Pin Slots · Composite / Composite Editor · Priority array (16 levels, BACnet-patterned) · Fallback · Out ·
In1–In16 · Propagate Flags · executeOnChange · Relation (tag connection, distinct from a data link) ·
Event-driven. `[CERT-doc] aboutComponents.txt, wiresheet-WireSheet-...txt, workingwithWiresheet.txt`.

## 538.8 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Only one link per level; levels 1/8 unlinkable; Boolean level 6 unlinkable (min on/off) | [CERT-doc] | aPriorityLinkRules.txt:21,23,24 | token-checked ✓ |
| 2 | In1/In8 = "Unlinkable input, available as action" — reconciles B536 | [CERT-doc]+[CERT] | aWritablePoints.txt:48,62 ↔ B536 | token-checked ✓ |
| 3 | Status propagation opt-in (Propagate Flags), OR across inputs, invalid types mark target invalid | [CERT-doc] | aObjectStatusPropagation.txt:12,33,39 | token-checked ✓ |
| 4 | Status NEVER propagates into a point | [CERT-doc] | aObjectStatusPropagation.txt:49 | token-checked ✓ |
| 5 | Priority scan is event-driven; no topo-order guarantee on standard sheet | [CERT-doc] | aPriorityInputScan.txt:18 | token-checked ✓ |
| 6 | ACE wire sheet has Level/Order + Force Order (deterministic exec) | [CERT-doc] | ace-AceCompManager.txt:23 | sweep-cited |
| 7 | Actions synchronous by default; async flag coalesces to engine thread | [CERT-doc] | help-BajadocViewer.txt:129 | token-checked ✓ |
| 8 | Conversion links auto-created on type mismatch (BIConverter child) | [CERT-doc] | docEn2_ConversionLinks.txt:13 | sweep-cited |
| 9 | Link owned by target (Source Ord property); dimmed slots illegal | [CERT-doc] | EditingLinks.txt:24; BasicObjectLinking.txt:43 | sweep-cited |
| 10 | Composite: avoid folder composites (official caution); each exposed slot = a link/resource cost | [CERT-doc] | aComposites.txt:22; aCompositeIssues.txt:21 | sweep-cited |
| 11 | Fallback Hidden flag forces null Out | [CERT-doc] | aPriorityInputScan.txt:43 | token-checked ✓ |
| 12 | kitControl placement Philosophy B preferred; naming rules | [CERT-doc] | WhereToLocateKitControl...:18; aboutComponents.txt:115 | sweep-cited |

**Marker tally**: [CERT-doc] primary (≈24 guide citations) · reconciliation cross-refs to [CERT] B536/B537 ·
[INFER] 0 substantive (only the "no topo-order guarantee" is an argued absence, sourced to the event-driven
statement). DOC-SYNTHESIS block — a high [CERT-doc] density is correct here. **7 of 12 load-bearing rows
token-verified against the guide files inline**; the remaining 5 are sweep-cited (the sweep's core priority
and status rules verified exactly, corroborating its reliability despite the classifier-unavailable note).

## Connections

- **[Block 6]** §6.2 — the CODE link kernel this block's official rules reconcile with (REMITTANCE). R2/W4
  CONFIRM the target-owns-link and knob model; A2 sources §6.2.7's ASYNC mitigation.
- **[Block 536]** (KC1) — R5/§538.2 reconcile the unlinkable levels 1/8 with the code; **refines B536** with
  the Boolean-only level-6 reservation and BP2 (status never enters a point).
- **[Block 537]** (KC2) — BP1 is the doc rule behind the multi-input status-OR; BP3 refines the fallback/set
  behavior.
- **Forward**: KC4 (BLoopPoint deep), KC6 (Program vs Expr — BP6), KC5 (clHVAC apps built under these rules),
  KC8 (priority-array write path end-to-end).

## Open gaps (this block)

- **Refinement candidate for [Block 536]**: the Boolean-writable level-6 reservation (min on/off times) is a
  writable-point fact B536 did not cover. Recorded here; not a full §14 correction (B536 was numeric-scoped
  and made no claim about Boolean level 6), but B536 gets a back-pointer note.
- The full 17×17 conversion matrix (R7) is summarized, not transcribed cell-by-cell — open a child gap only
  if a specific converter pair is later needed.
