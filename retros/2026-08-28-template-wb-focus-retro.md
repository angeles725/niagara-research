<!-- review-status: pending -->
<!-- kit-retro: include -->

# §18 Retrospective — template-wb focus (2026-08-28)

**Run**: niagara-research, focus `template-wb`, 2026-08-28
**Blocks written**: B591–B594 (5 gaps TW1–TW5; TW4+TW5 collapsed into B594)
**Coverage**: 5/5 investigable gaps closed in 4 blocks; 0 requires-execution
**Driver**: self-paced /research-sdd (Opus), AUDIT-FIRST seed delegated to sonnet, verified inline

---

## Summary

A short, honest tail focus that landed exactly where the sweep predicted. The most useful thing about this run
is that the AUDIT-FIRST sweep gave an explicit HONEST-DEPTH assessment ("TW1 substantive alone; TW2/TW3 short;
TW4+TW5 collapsible; 4 blocks is more honest than 6") — and following it produced 4 tight blocks instead of
padding to 5–6. One kit observation, confirming a pattern.

---

## Delta proposals

### D1 — an AUDIT-FIRST "honest depth ceiling" should be a first-class sweep output, and the driver should honor it (NEW, MEDIUM)

**What happened.** The sweep ended with: *"TW1 is the only block that could be substantive on its own. TW2 and
TW3 are worth short but concrete blocks. TW4 and TW5 are thin enough that they could be collapsed into one
'template-wb tail' synthesis block rather than individual blocks — 6 total blocks is likely the ceiling; 4 is
more honest."* The run followed it: TW1/TW2/TW3 became one block each, TW4+TW5 collapsed into B594. Result: 4
blocks, no padding, each with real content. Without that guidance the default 1-gap-1-block rule would have
produced two thin blocks (a ~120-line Relation-editor block and a ~100-line file-integration block) that add
little over a combined one.

**Proposed delta.** The AUDIT-FIRST sweep contract (§13) should REQUIRE an "honest depth" line per gap
(substantive / short-concrete / collapsible-thin) and a recommended block ceiling, and the loop (§8/PROMPT-LOOP)
should permit MERGING adjacent thin gaps into one synthesis block WITHOUT it counting as a skipped gap — the
gaps are still closed, just co-located. This prevents the 1-gap-1-block default from inflating tail focuses. The
`webChart` retro (2026-08-05, delta WC-A "fan-out paralelo de gaps independientes") is the inverse case (split
independent gaps); this is the merge case for dependent thin ones. Both should be explicit in §13.

---

## What went well (keep)

- **Prior-coverage reconciliation held again** (3rd focus running it): the sweep confirmed B200 §200.6 is an
  overview and correctly remitted the PxEditor (B191/198), BOG tab (B15), and tag chooser (B260–270) BEFORE any
  block — so the four blocks only covered genuinely-unopened UI.
- **Collapsing TW4+TW5 was announced in the state file and the block header**, not silent — the coverage metric
  reads "5/5, 4 blocks" with the collapse noted, so the count is honest and traceable.
- **The tail stayed a tail.** No temptation to open BTemplateManager's 2705 lines (mostly action dispatch already
  sketched in B200 §200.6) — it was named as low-value and left, which is the right call for a UI-over-engine
  focus.

---

## Child gaps surfaced (named, out of scope)

- `BTemplateManager` view internals (2705 L) — mostly action dispatch over the rt engine; low marginal value.
- Small dialogs (`BTemplateOptions`, `BTemplateHistoryDialog`, `BTemplateDeployProgressDialog`) — trivial UI.
- `MakeModule` command's packaging mechanics (template → distributable module) — a genuinely interesting seam
  into the module-build pipeline, but a `module`/build focus, not template-wb.
