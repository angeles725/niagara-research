# Block 597 — The component transfer strategies: copy-vs-move is a flag on `CompToComp`, a same-space move is specialized to an identity-preserving re-parent (`IntraCompSpaceMove`), `CompToBog` exports to a fresh in-memory bog, `ToNavFolder` organizes bookmarks, and `DeleteOp` deletes with a link-cleanup facet

**Session**: 2026-08-28
**Focus**: `sys-transfer` (gap ST3 — the component-side concrete strategies)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the five component strategies; copy/move/reparent/export/delete
paths token-verified inline.
**Primary sources** `[CERT]`:
- `organized/baja/baja/vineflower/com/tridium/sys/transfer/{CompToComp,IntraCompSpaceMove,CompToBog,ToNavFolder,
  DeleteOp}.java`.

**Scope**: what actually happens for a component copy/move/reparent/export/delete. These are the leaves the
factory ([Block 595] ST1) selects for component targets. Does NOT re-open the factory or the component space
([B408]).

---

## 597.1 `CompToComp` — copy, or move by flag [CERT]

`CompToComp extends TransferStrategy` `[CERT] :32` is the base component transfer. It carries a `copySource`
boolean `[CERT] :37` read from a parameter (`getParameter("copySource", true)` `[CERT] :103`, default COPY).
`transfer()` `[CERT] :52-89` adds the source to the target parent and, when NOT copying (i.e. a MOVE),
`remove()`s it from the source `[CERT] :89`. So COPY vs MOVE is a single flag on one strategy — a move is a copy
followed by a source removal, for a CROSS-space transfer.

## 597.2 `IntraCompSpaceMove` — a same-space move is a re-parent, not copy+delete [CERT]

`IntraCompSpaceMove extends CompToComp` `[CERT] :18` overrides `transfer()` `[CERT] :20-26`:
```java
this.copySource = false;
this.move();          // re-parent within the same space
```
`move()` `[CERT] :31-33` re-parents the component from `oldParent` (`sourceParent`) to `newParent`
(`target.asComponent()`) WITHIN the same component space. This is the `fw(113)` strategy the factory picks for a
same-space MOVE ([Block 595] §595.2). The distinction matters: a same-space move is an identity-preserving
**re-parent** (the component object and its handle stay the same), NOT a copy-then-delete — so links and handles
survive automatically, without the `ReplacingContext` machinery ([Block 596]) a cross-space replace needs.

## 597.3 `CompToBog` — export to a fresh in-memory bog [CERT]

`CompToBog extends CompToComp` `[CERT] :11` overrides `getTargetSpace()` to return a **new**
`BComponentSpace(null, null, null)` `[CERT] :24-25` — a fresh in-memory component space — and adds the marked
components into its root (`root.add(insertNames[i], insertValues[i])` `[CERT] :41`). So "copy a component to a
bog" is copying it into a standalone in-memory space, which is then serialized (a `.bog` file, a clipboard
payload, [B5]/[B408]). This is the export path behind saving a subtree as a `.bog` and behind the clipboard
serialization ([Block 595] consumers).

## 597.4 `ToNavFolder` and `DeleteOp` [CERT]

- `ToNavFolder extends TransferStrategy` `[CERT] :11` — `move()` a `BINavNode` into a `BNavFolder`
  `[CERT] :56-64`, recursing for nested folders (`moveFolder`), guarding duplicate names
  (`ToNavFolder.error.duplicateName` `[CERT] :50`). This organizes NAV-TREE bookmarks/folders, not component-space
  data.
- `DeleteOp` `[CERT] :38` (NOT a `TransferStrategy` subclass — a standalone op) removes each marked component from
  its parent `[CERT] :79`. Its notable facet: `noRemoveLinks = BFacets.make("niagaraRemoveLinks", BBoolean.FALSE)`
  `[CERT] :42` — deletion can be told NOT to remove the links pointing at the deleted component (leaving them to
  fault) vs cleaning them up. `make(space, cx, in)` `[CERT] :59` supports a batch delete from a serialized input.

## 597.5 Thesis [CERT-synthesis]

The component strategies encode the SEMANTIC differences a naive "copy the object" would miss: a same-space move
must preserve identity (re-parent, §597.2), a cross-space move is a copy+remove (§597.1), an export goes through
a throwaway bog space (§597.3), a nav-folder move touches the nav tree not the data (§597.4), and a delete has a
choice about dangling links (§597.4). Centralizing these in the transfer framework ([Block 595]) is why Workbench
cut/copy/paste behaves correctly across all these cases without each caller re-implementing the rules — the
factory picks the strategy that has the right semantics for the (source, target, action) triple.

## 597.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | CompToComp: copySource flag (default true=copy); transfer adds to target, remove() on move | [CERT] | CompToComp.java:32,37,52-89,103 | token-checked ✓ |
| 2 | IntraCompSpaceMove: same-space MOVE = re-parent oldParent→newParent (identity-preserving, not copy+delete) | [CERT] | IntraCompSpaceMove.java:18-33 | token-checked ✓ |
| 3 | CompToBog: target space = new in-memory BComponentSpace; root.add the components (export path) | [CERT] | CompToBog.java:11,24-41 | token-checked ✓ |
| 4 | ToNavFolder: move BINavNode into BNavFolder, recurse, guard duplicate names (nav tree, not data) | [CERT] | ToNavFolder.java:11,50-64 | token-checked ✓ |
| 5 | DeleteOp (standalone): removes components; niagaraRemoveLinks=FALSE facet controls link cleanup; batch from stream | [CERT] | DeleteOp.java:38,42,59,79 | token-checked ✓ |
| 6 | Strategies encode semantic differences (identity vs copy vs export vs nav vs link-cleanup) | [CERT-synthesis] | rows 1-5 | reasoned ✓ |

**Marker tally**: [CERT] ×5 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 6
rows token-verified inline.

## Connections

- **[Block 595]** (ST1) — the factory selecting these by (source, target, action).
- **[Block 596]** (ST2) — the cross-space REPLACE path (`ReplacingContext`); §597.2 shows why a same-space move
  does NOT need it.
- **[Block 408]** — `BComponentSpace`; **[Block 5]** — the `.bog` export `CompToBog` produces.
- **[Block 432]** (workbench) — WB cut/copy/paste/delete consume these.

## Open gaps (this block)

- `DeleteOp`'s exact link-removal traversal and `ToNavFolder`'s nav-space persistence are named, low value. Focus
  continues at ST4 (file strategies + transfer results/listener).
