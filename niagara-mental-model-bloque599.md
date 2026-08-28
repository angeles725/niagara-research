# Block 599 — The two ends of the transfer engine: cross-station transfer (`RemoteIntraSpace` → `RemoteTransferSpace`, serialized by the Fox `TransferCodec`) and the Workbench consumer (`TransferUtil` wraps a `Mark`, runs the strategy, returns an undoable `CommandArtifact`) — closing the sys-transfer focus

**Session**: 2026-08-28
**Focus**: `sys-transfer` (gap ST5 — remote transfer + the Workbench clipboard/drag-drop consumer; the final gap)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `RemoteIntraSpace`/`RemoteTransferSpace` + the Fox `TransferCodec` +
`TransferUtil`; the delegation, the wire serialization, and the WB entry token-verified inline.
**Primary sources** `[CERT]`:
- `organized/baja/baja/vineflower/com/tridium/sys/transfer/{RemoteIntraSpace,RemoteTransferSpace}.java`.
- `organized/fox/fox-rt/vineflower/com/tridium/fox/sys/broker/TransferCodec.java`.
- `organized/workbench/workbench-wb/vineflower/com/tridium/workbench/transfer/TransferUtil.java`.

**Scope**: how a transfer crosses stations and how Workbench drives the engine. Closes ST5 and the `sys-transfer`
focus. Applies the factory ([Block 595]) and the results ([Block 598]). Does NOT re-open Fox ([B134]) or WB
commands ([B432]) — connects.

---

## 599.1 Cross-station transfer: delegate to the remote space, serialize the Mark [CERT]

`RemoteIntraSpace extends TransferStrategy` `[CERT] :6` does not perform the transfer locally — it DELEGATES to
the target's space `[CERT] :8-10`:
```java
public TransferResult transfer() { return ((RemoteTransferSpace) space).transfer(this); }
```
`RemoteTransferSpace` `[CERT] :4` is the interface `{ TransferResult transfer(TransferStrategy); }` a
Fox-proxied space implements. The serialization is the Fox `TransferCodec` `[CERT] :22`:
`transferToMessage(FoxRequest, TransferStrategy)` `[CERT] :29-31` pulls the strategy's `Mark`, encodes its
`values` + `names` + `params` into the Fox message; the decode side rebuilds `Mark mark = new Mark(values, names)`
`[CERT] :59` and the `params` `[CERT] :61`, and the `TransferResult`/`CompTransferResult` are codec'd back
`[CERT] :9-10`. So a cross-station cut/paste works by shipping the Mark (the component/file set) + action over
Fox, running the transfer on the TARGET station, and returning the result — e.g. copy a subtree from the
supervisor onto a subordinate.

## 599.2 The Workbench consumer: `TransferUtil` [CERT]

`TransferUtil implements TransferConst` `[CERT] :54` is the Workbench glue between the clipboard/drag-drop UI and
the engine:
- **Wrap**: `TransferEnvelope.make(new Mark(object))` `[CERT] :60` turns a selected object into a transferable
  `Mark` in an envelope (the clipboard/drag payload); the drop reads it back
  (`cx.getEnvelope().getData(TransferFormat.mark)` `[CERT] :112`).
- **Classify**: `isComplex` / `isComponent` / `isFile` / `isHost(Mark)` `[CERT] :63-99` — what is being moved.
- **Run**: `insert(BWidget owner, int action, Mark mark, BObject target, BComponent params, Context cx)`
  `[CERT] :116` is the main entry — it runs `TransferStrategy.make(action, mark, target, params, cx).transfer()`
  ([Block 595]) and returns a **`CommandArtifact`** — so a paste/move is an **undoable Workbench command**
  ([B432], using the `TransferResult.undo()` of [Block 598]). `action` is the `ACTION_COPY`/`ACTION_MOVE` of
  [Block 595].
- **Prompt**: `promptForName` `[CERT] :133-140` asks for a name on insert (paste) unless the general option
  `getPromptForNameOnInsert()` is off and it is not a file.

So Workbench cut/copy/paste/drag-drop is: select → `Mark` in an envelope → `TransferUtil.insert(action, …)` →
`TransferStrategy.make().transfer()` → an undoable `CommandArtifact`.

## 599.3 Focus close — the transfer engine, end to end [CERT-synthesis]

With ST5 the `sys-transfer` focus is complete. The engine, across five blocks: a transfer is `(action, Mark,
target)`, and `TransferStrategy.make` ([Block 595]) routes it by target-type × source-space × action to a
concrete strategy — component copy/move/reparent/export/delete ([Block 597]), file copy/move ([Block 598]),
deploy-of-a-`BIDeployable` under a handle-preserving `ReplacingContext` ([Block 596]), or a remote delegation
serialized over Fox (this block). Every transfer returns a self-reversing `TransferResult` ([Block 598]), which
is what makes it undoable. Workbench's clipboard/drag-drop (`TransferUtil`, this block) is one consumer; the
template subsystem ([B578]/[B579]) is another (a `.ntpl` is a `BIDeployable`); BOX web paste and cross-station
transfer are two more. **This is a foundational primitive the corpus had used from five consumer blocks without
ever opening** — now it is the base beneath the template thread, and the reason cut/copy/paste, drag-drop,
`.bog` export, cross-station paste, and template deploy all share one correct, reversible semantics.

## 599.4 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | RemoteIntraSpace.transfer delegates to RemoteTransferSpace.transfer(strategy) | [CERT] | RemoteIntraSpace.java:8-10; RemoteTransferSpace.java:4 | token-checked ✓ |
| 2 | Fox TransferCodec serializes Mark (values+names)+params to/from a FoxRequest; result codec'd back | [CERT] | TransferCodec.java:22,29-61,9-10 | token-checked ✓ |
| 3 | TransferUtil wraps object as Mark in a TransferEnvelope; classifies isComponent/isFile/isHost | [CERT] | TransferUtil.java:54-99,112 | token-checked ✓ |
| 4 | insert(action, mark, target, params) runs TransferStrategy.make().transfer() → CommandArtifact (undoable) | [CERT] | TransferUtil.java:116 | token-checked ✓ |
| 5 | promptForName on insert unless option off and not a file | [CERT] | :133-140 | token-checked ✓ |
| 6 | Focus close: one engine behind WB clipboard, BOX paste, cross-station, template deploy — foundational primitive | [CERT-synthesis] | B595-B598 + rows 1-4 | reasoned ✓ |

**Marker tally**: [CERT] ×5 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 6
rows token-verified inline.

## Connections

- **[Block 595]** (ST1) — `TransferStrategy.make`, which `TransferUtil.insert` calls and `RemoteIntraSpace` is
  selected by.
- **[Block 598]** (ST4) — the `TransferResult.undo()` that makes `TransferUtil`'s `CommandArtifact` undoable.
- **[Block 432]** (workbench) — WB Command/undo; `TransferUtil` returns a `CommandArtifact`.
- **[Block 134]** — Fox; `TransferCodec` rides it for cross-station transfer.
- **[Block 578]/[Block 579]** — the template consumer of the same engine.

## Open gaps (this block)

- `TransferCodec`'s exact value-encoding (how a component subtree is serialized to the Fox message) overlaps the
  BOG/BOX encoders ([B5]/[B512]) — named, low value. **ST5 CLOSED; sys-transfer focus investigable=0 → STOP.**
