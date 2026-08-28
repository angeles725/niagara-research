# Block 595 — `TransferStrategy`: the strategy-pattern factory behind ALL component/file transfer — one `make(action, Mark, target)` call dispatches by target type then source-space×action to the right concrete strategy, which is why Workbench cut/copy/paste, drag-drop, and template deploy all funnel through one engine

**Session**: 2026-08-28
**Focus**: `sys-transfer` (gap ST1 — the strategy factory and its dispatch; the core of the transfer framework)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `TransferStrategy` (factory + dispatch) + `Mark`; the selection
tree and action constants token-verified inline.
**Primary sources** `[CERT]`:
- `organized/baja/baja/vineflower/com/tridium/sys/transfer/TransferStrategy.java`.
- `organized/baja/baja/vineflower/javax/baja/space/Mark.java`.

**Scope**: the general transfer engine, opened as a subsystem for the first time. [B578]/[B579]/[B583]/[B593]
CONSUMED `DeployToComp`/`ReplacingContext` from the template side; ST1 opens the framework they all funnel
through. Does NOT re-open the template deploy ([B578]/[B579]) or the component space ([B408]) — this is the layer
beneath them.

---

## 595.1 A transfer = (action, `Mark`, target) [CERT]

A transfer is defined by three things:
- **`Mark`** `[CERT] Mark.java:14-63` — the SOURCE marker: `BObject[] values` + `String[] names` (the set of
  components or files being transferred, each named). Constructors take one value, a value+name, or arrays. This
  is what a clipboard cut/copy or a drag holds.
- **action** — the operation: `ACTION_COPY = 16`, `ACTION_MOVE = 32` `[CERT] TransferStrategy.java:31-32`
  (a small int set; deploy rides these with a deployable source, §595.3).
- **target** — where it goes (a component, a file, a nav folder).

`TransferStrategy.make(int action, Mark mark, BObject target, BComponent params, Context cx)` `[CERT] :48` is the
single entry point. It first normalizes the target via `toActualTarget` `[CERT] :85-94` (a `BGateway` →
its gateway space, a `BComponentSpace` → its root component, a `FoxLibraryFileSpace`/`FoxFileSpace` → a file), so
the dispatch always sees a concrete component or file.

## 595.2 The dispatch tree: target type, then source×action [CERT]

`makeImpl(action, mark, target, cx)` `[CERT] :123-135` branches first on TARGET TYPE:
```java
if (target instanceof BComponent)            return toComp(action, mark, (BComponent) target, cx);
else if (target instanceof BIFile)           return toFile(action, mark, (BIFile) target, cx);
else if (target instanceof BIScopedFileSpace)return toFile(action, mark, ...findFile("") , cx);
else return (target instanceof BNavFolder || target instanceof BNavRoot) ? new ToNavFolder() : null;
```
Then `toComp(action, mark, target, cx)` `[CERT] :135-152` branches on the SOURCE SPACE and ACTION:
```java
BComponentSpace sourceSpace = ...;
if (sourceSpace == targetSpace) {                          // SAME space
   if (targetSpace instanceof RemoteTransferSpace) return new RemoteIntraSpace();
   if (action == 32 /*MOVE*/)                     return (TransferStrategy) sourceSpace.fw(113);  // IntraCompSpaceMove
}
if (sourceSpace == null)                          strategy = new CompToComp();     // no source space
else if (sourceSpace instanceof BComponentSpace)  strategy = new CompToComp();     // cross-space
// ...
```
So the concrete strategy is chosen from the (target-type × source-space × action) triple: same-space MOVE → an
intra-space move (`fw(113)`, `IntraCompSpaceMove`); same-space on a remote space → `RemoteIntraSpace`;
cross-space or no-source → `CompToComp`; a file target → the file strategies; a nav folder → `ToNavFolder`.

## 595.3 Deploy is a transfer of a `BIDeployable` [CERT]

The deploy path templates use ([B578]/[B579]) is just a special case in `toComp` `[CERT] :138`:
```java
if (!(mark.getValues()[0] instanceof BIDeployable) || (action != 32 && action != 16)) { ...normal path... }
// else → the DEPLOY strategy (DeployToComp), ST2
```
So when the Mark's source is a `BIDeployable` (a `.ntpl`, [B583]) and the action is copy/move, the factory selects
`DeployToComp` instead of a plain component copy. Deploy is not a separate API — it is `TransferStrategy.make`
with a deployable source (ST2 opens `DeployToComp` itself).

## 595.4 Why one engine [CERT-synthesis]

Every way a component or file moves in Niagara routes through `TransferStrategy.make`: Workbench cut/copy/paste
and drag-drop (`workbench-wb/transfer/TransferUtil`+`TransferArtifact`), the BOX web paste (`box/util/PasteRecord`),
cross-station transfer (`fox/…/TransferCodec`), and template deploy (`template-wb/…/TmplUtil`,
`BWbDeployableNtplFile`). Centralizing the source×target×action decision in one factory is why all of them share
the same semantics — a move preserves handles differently than a copy, a cross-space paste re-parents, a deploy
replaces — and why the template subsystem got a robust deploy "for free" by marking a `.ntpl` `BIDeployable` and
calling the same engine. The concrete strategies (ST2–ST5) are the leaves; `make` is the router.

## 595.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Mark = BObject[] values + String[] names (the source set); TransferStrategy.make is the single entry | [CERT] | Mark.java:14-63; TransferStrategy.java:48 | token-checked ✓ |
| 2 | Actions ACTION_COPY=16, ACTION_MOVE=32; toActualTarget normalizes gateway/space/foxfilespace | [CERT] | TransferStrategy.java:31-32,85-94 | token-checked ✓ |
| 3 | makeImpl dispatches by target type: BComponent→toComp, BIFile→toFile, BNavFolder→ToNavFolder | [CERT] | :123-135 | token-checked ✓ |
| 4 | toComp branches by source-space×action: same-space MOVE→fw(113) IntraCompSpaceMove; remote→RemoteIntraSpace; cross/no-source→CompToComp | [CERT] | :135-152 | token-checked ✓ |
| 5 | Deploy = a BIDeployable source with copy/move action → DeployToComp (not a separate API) | [CERT] | :138 | token-checked ✓ |
| 6 | All transfer paths (WB paste, BOX, Fox, template deploy) funnel through make | [CERT-synthesis] | consumer sweep + rows 1-5 | reasoned ✓ |

**Marker tally**: [CERT] ×5 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 6
rows token-verified inline.

## Connections

- **[Block 578]/[Block 579]** — template deploy/upgrade, a CONSUMER of this via `DeployToComp`/`ReplacingContext` (ST2).
- **[Block 408]** — the component space (`BComponentSpace`/rootComponent) transfers operate over.
- **[Block 432]** (workbench) — WB commands/undo/transfer; `TransferUtil` is the clipboard consumer of this engine.
- **ST2–ST5** (this focus) — the concrete strategies `make` selects.

## Open gaps (this block)

- The exact `fw(113)` intra-space-move hook and the full `action` bit set (beyond COPY/MOVE) are named, partially
  traced — ST3 territory. Focus continues at ST2 (DeployToComp + ReplacingContext).
