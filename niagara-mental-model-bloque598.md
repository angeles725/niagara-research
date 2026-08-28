# Block 598 — File transfer strategies and the UNDO model: every transfer returns a `TransferResult` with an `undo()` — `CompTransferResult` knows to remove-what-it-added (copy) or move-it-back (move), which is what makes Workbench cut/copy/paste undoable; the file strategies mirror the component ones

**Session**: 2026-08-28
**Focus**: `sys-transfer` (gap ST4 — the file strategies + the result/undo model + the progress listener)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the file strategies + `TransferResult`/`CompTransferResult` +
`TransferListener`; the undo methods token-verified inline.
**Primary sources** `[CERT]`:
- `organized/baja/baja/vineflower/com/tridium/sys/transfer/{FileToFile,FileToComp,IntraFileSpaceMove,
  TransferResult,CompTransferResult,TransferListener}.java`.

**Scope**: the file-space transfers and the outcome/undo model shared by all strategies. Complements the
component strategies ([Block 597] ST3). Does NOT re-open the file space ([B5]) or Workbench undo ([B432]) —
connects.

---

## 598.1 The file strategies mirror the component ones [CERT]

- `FileToFile extends TransferStrategy` `[CERT] :18` holds `BIFile[] sourceFiles` `[CERT] :19`; `transfer()`
  `[CERT] :28` streams each source file (InputStream → OutputStream `[CERT] :4-5`) into the target file space — a
  file copy/move between file spaces.
- `IntraFileSpaceMove extends FileToFile` `[CERT] :8` overrides `transfer()` → `move()` `[CERT] :10-17` — a
  same-file-space move/rename (the file analog of `IntraCompSpaceMove`, [Block 597] §597.2).
- `FileToComp extends CompToComp` `[CERT] :11` — a file dropped onto a COMPONENT delegates to the component
  strategy (`super.transfer()` `[CERT] :13-15`): the file resolves to components (e.g. importing a `.bog`), so it
  is handled as a component transfer. It extends `CompToComp`, not `FileToFile`.

So the file/component split mirrors: cross-space file copy (`FileToFile`), same-space file move
(`IntraFileSpaceMove`), and file-into-component (`FileToComp` → component path).

## 598.2 Every transfer is an UNDO token [CERT]

The important finding is the result type. `TransferResult` `[CERT] :3` is abstract with one required method:
```java
public abstract void undo() throws Exception;
```
So EVERY `transfer()` returns a result that knows how to REVERSE itself. `CompTransferResult extends
TransferResult` `[CERT] :13` implements `undo()` `[CERT] :41` by dispatching to:
- `undoCopy()` `[CERT] :53-72` — remove the components that were ADDED (`target.remove(prop, tx)`), including any
  added `displayNames` `[CERT] :72`;
- `undoMove()` `[CERT] :82` — move the component BACK to its original parent.

So a copy is undone by deleting what it created, and a move is undone by re-parenting back. This is the mechanism
that makes Workbench cut/copy/paste/move **undoable** ([B432] Command/undo): the WB transfer command runs a
strategy, keeps the returned `TransferResult`, and calls `result.undo()` on Ctrl-Z. The transfer framework does
not just perform the operation — it hands back a reversal.

## 598.3 `TransferListener` — the progress hook [CERT]

`interface TransferListener { void updateStatus(String); }` `[CERT] :3-4` is the progress callback the strategies
call during a transfer (e.g. `DeployToComp`'s `updateStatus("deploy.transfer", …)`, [Block 596]). It is how a
long transfer (a deploy, a big paste) reports progress to the WB job bar / dialog.

## 598.4 Thesis [CERT-synthesis]

Two properties make the transfer framework a proper subsystem rather than a pile of copy loops: (1) a uniform
UNDO contract — every strategy returns a self-reversing `TransferResult`, so undoability is guaranteed at the
framework level, not re-implemented per caller; and (2) a uniform progress contract (`TransferListener`). Combined
with the factory ([Block 595]) and the semantic-correct strategies ([Block 597]), this is why Workbench's
clipboard operations are consistent, reversible, and progress-reporting across component and file spaces alike —
the caller (`TransferUtil`, ST5) just runs `make(...).transfer()`, stashes the result for undo, and passes a
listener.

## 598.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | FileToFile (BIFile[], stream copy between file spaces); IntraFileSpaceMove same-space move; FileToComp extends CompToComp (file→component path) | [CERT] | FileToFile.java:18-28; IntraFileSpaceMove.java:8-17; FileToComp.java:11-15 | token-checked ✓ |
| 2 | TransferResult abstract with undo() — every transfer returns a reversal | [CERT] | TransferResult.java:3,14 | token-checked ✓ |
| 3 | CompTransferResult.undo dispatches undoCopy (remove added comps + displayNames) or undoMove (re-parent back) | [CERT] | CompTransferResult.java:13,41-82 | token-checked ✓ |
| 4 | This is what makes WB cut/copy/paste/move undoable | [CERT-synthesis] | rows 2-3 + [B432] | reasoned ✓ |
| 5 | TransferListener.updateStatus(String) = progress hook the strategies call | [CERT] | TransferListener.java:3-4 | token-checked ✓ |

**Marker tally**: [CERT] ×4 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 4 of 5
rows token-verified inline.

## Connections

- **[Block 597]** (ST3) — the component strategies whose `CompTransferResult` this undoes.
- **[Block 432]** (workbench) — WB Command/undo; the transfer command stashes the `TransferResult` for Ctrl-Z.
- **[Block 596]** (ST2) — `DeployToComp` calls the `TransferListener` for progress.
- **[Block 5]** — the file space `FileToFile` moves between; the `.bog` a `FileToComp` imports.

## Open gaps (this block)

- Whether a `DeployToComp`/`ReplacingContext` replace is fully undoable (its result type) is not separately traced
  — likely a specialized result, low value. Focus continues at ST5 (remote transfer + the Workbench consumer),
  the final gap.
