# Block 545 — Composites at the code level: a promoted slot is a Knob-backed mirror of a child slot, confirming "each exposed slot is a link"

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC9 — composites as a reuse/programming construct, code model)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read (no sub-agent — narrow gap).
**Primary sources** `[CERT]`: `organized/baja/baja/vineflower/javax/baja/util/BCompositeAction.java`,
`BCompositeTopic.java`.

**Scope**: the CODE mechanism behind composites — how a slot promoted onto a parent glyph actually forwards to
the child slot. [Block 538] §538.5 covered the composite RULES from the official docs (expose child slots,
avoid folder composites, each exposed slot costs a link/resource, dynamic-link pins subscription) — REMITTANCE.
This block adds the implementation and confirms the "each exposed slot is a link" rule from code. It is a
COMPACT block: a narrow gap that did not warrant a full sweep.

---

## 545.1 A composite action = a Knob-backed mirror [CERT]

`BCompositeAction extends BAction` `[CERT] BCompositeAction.java:17`. When you composite a child action onto a
parent, the parent gets a `BCompositeAction` property; the ACTUAL link to the child is a **Knob** on that
property. `getMirror()` `[CERT] :25-56` resolves it:
```java
Knob[] knobs = parent.getKnobs(this.getPropertyInParent());   // the composite slot's knob(s)
Knob knob = knobs[0];
BOrd ord = knob.getTargetOrd();                                // child component
String slotName = knob.getTargetSlotName();                   // child slot
Slot slot = c.getSlot(slotName);
return (slot instanceof Action) ? new Mirror(knob, c, (Action)slot) : null;
```
The composite action delegates everything to the mirrored child: `getParameterType()`, `getParameterDefault()`,
and `getFacets()` all read from `mirror.action` on the child component `[CERT] :58-79`. Its own `invoke()`
returns `null` `[CERT] :82-84` — the composite slot holds NO behavior; invocation flows through the Knob (the
link) to the child. `BCompositeTopic extends BTopic` `[CERT] BCompositeTopic.java:17` is the parallel for topic
promotion.

## 545.2 This CONFIRMS the doc rule "each exposed slot is a link" from code [CERT]

[Block 538] R-C3 (`[CERT-doc] aCompositeIssues.txt:21`): "Each item exposed in a composite represents a link,
where each link consumes some small amount of station resources." §545.1 is the MECHANISM: the promoted slot is
literally backed by a `Knob` (the runtime face of a `BLink`, [Block 6] §6.2.3). That is why an over-composited
station loses capacity — every exposed slot carries a live knob — and why linking a dynamic value into a
composite pins the source subscribed ([Block 538] R-C4): the knob keeps the child leased. So composites reuse
the SAME link/knob machinery as ordinary wire-sheet links; they are not a separate aggregation primitive.

## 545.3 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BCompositeAction extends BAction; getMirror resolves child via the slot's Knob (targetOrd/slotName) | [CERT] | BCompositeAction.java:17,25-56 | token-checked ✓ |
| 2 | Composite action delegates paramType/default/facets to the mirrored child action | [CERT] | BCompositeAction.java:58-79 | token-checked ✓ |
| 3 | Composite slot holds no behavior — invoke() returns null; flow goes through the knob | [CERT] | BCompositeAction.java:82-84 | token-checked ✓ |
| 4 | BCompositeTopic extends BTopic (parallel topic promotion) | [CERT] | BCompositeTopic.java:17 | token-checked ✓ |
| 5 | Promoted slot IS a knob/link → confirms B538 R-C3 "each exposed slot is a link" | [CERT] | §545.2 + B538/aCompositeIssues | logic-checked |

**Marker tally**: [CERT] ×4 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation), COMPACT. 4 of 5 rows
token-verified inline. HONEST SCOPE NOTE: this covers ACTION/TOPIC composite promotion; a composited PROPERTY
uses the same knob-backed slot model (a `BComposite`-style property alias) — the property variant was not
separately decompiled here (the two util classes cover action/topic; property promotion is the wiresheet-wb
Composite Editor writing an equivalent knob-backed slot [INFER]).

## Connections

- **[Block 538]** §538.5 (R-C1..C4) — the composite RULES from docs; §545.2 confirms R-C3 at code level.
- **[Block 6]** §6.2.3 — the Knob (link mirror) that a composite slot reuses.
- **[Block 427]/workbench focus** — the wire-sheet Composite Editor UI (the tool that creates these) belongs to
  the workbench focus, not re-derived here.

## Open gaps (this block)

- Property composite (vs action/topic) implementation not separately decompiled — a workbench-focus child gap,
  not a kitControl one.
