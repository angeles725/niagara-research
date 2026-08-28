# Block 596 — `DeployToComp` + `ReplacingContext`: deploy is polymorphic (the `BIDeployable` supplies its own steps; the strategy just drives them), and `ReplacingContext` IS the handle-preservation primitive — it captures the old subtree's handles and restores them onto the replacement, the mechanism B578/B579 relied on

**Session**: 2026-08-28
**Focus**: `sys-transfer` (gap ST2 — the deploy/replace strategy the template subsystem consumes)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `DeployToComp` + the full `ReplacingContext`; the BIDeployable
delegation and the handle methods token-verified inline.
**Primary sources** `[CERT]`:
- `organized/baja/baja/vineflower/com/tridium/sys/transfer/{DeployToComp,ReplacingContext}.java`.

**Scope**: the two primitives the template deploy/upgrade ([B578]/[B579]) consumed as opaque. ST2 opens them and
LOCATES the handle-preservation mechanism those blocks relied on. Does NOT re-open the template side — this is the
engine beneath it. Builds on the factory ([Block 595] ST1).

---

## 596.1 `DeployToComp` delegates to `BIDeployable` [CERT]

`DeployToComp extends TransferStrategy` `[CERT] :19` holds a `BIDeployable deployable` + `BIDeployable.Step[] steps`
`[CERT] :24-25`. Setup `[CERT] :31-50`:
```java
this.deployable = (BIDeployable) this.mark.getValue(0);
if (this.deployable.getFileName().endsWith(".ntpl")) { this.params.add/set("exact", BBoolean.TRUE); }   // exact-name deploy
if (!this.deployable.isDeployable(this.target.asComponent())) { ...reject... }
BIDeployable.Step[] steps = this.deployable.getSteps(owner, this.target.asComponent(), this.mark.getNames()[0]);
```
The key design: **the deployable supplies its own deploy steps** (`getSteps`). `DeployToComp` does not know how a
`.ntpl` unpacks — it asks the `BIDeployable` for a `Step[]` and DRIVES them. `transfer()` `[CERT] :65-82` iterates
the steps, updating progress (`deploy.transfer`, count/totalFiles). So deploy is POLYMORPHIC over the
`javax.baja.file.BIDeployable` interface — anything that implements it (a `.ntpl` via `BNtplFile`, [B583];
`BWbDeployableNtplFile`, [B594]) plugs into the transfer engine ([Block 595] ST1 selects `DeployToComp` when the
Mark's source is a `BIDeployable`). The `exact=TRUE` flag on `.ntpl` deploys to the EXACT target name (no
auto-rename to avoid collision), which is why a template upgrade lands under the same deployed name ([B579]).

## 596.2 `ReplacingContext` IS the handle-preservation primitive [CERT]

`ReplacingContext extends BasicContext` `[CERT] :16` — a Context subtype (constructible with a base + additional
`BFacets`, e.g. `niagaraAutoStart=false`, [B578] §578.3). But its substance is a HANDLE MAP `[CERT] :30-50`:
```java
public boolean handlesEmpty();
public void    addAllHandles(BComponent component);        // capture the subtree's handles BEFORE replace
public boolean hasHandle(BOrd ord);
public Object  removeHandle(BOrd ord);
public void    restoreHandles(SlotPath parentPath, String name, BComponent component);   // apply to the NEW tree
```
This is the exact mechanism [B578 §578.3] ("preserves handles so relations survive") and [B579 §579.2]
("handle-keyed external capture") relied on but treated as opaque. NOW LOCATED: when a transfer runs under a
`ReplacingContext`, `addAllHandles` snapshots every component handle in the subtree about to be replaced; as the
new components are added, `restoreHandles` re-assigns the OLD handle to the matching NEW component (looked up by
ORD via `hasHandle`/`removeHandle`). So a replaced component keeps its handle — and every link/relation that
addressed it BY HANDLE ([B6]/[B579]) still resolves. The `ReplacingContext` is the carrier of that handle map for
the duration of the replace.

## 596.3 The two-line story of template deploy, from below [CERT-synthesis]

Reading the template thread from the primitive up: a `.ntpl` is a `BIDeployable` ([B583]); `TransferStrategy.make`
with that deployable source selects `DeployToComp` ([Block 595] ST1); `DeployToComp` asks the deployable for its
`Step[]` and drives them under a `ReplacingContext`; the `ReplacingContext` snapshots handles before the old
subtree is removed and restores them onto the new one — which is why the template upgrade ([B579]) could capture
external links by handle and rebuild them, and why the installer ([B578]) preserves relations across the swap. The
template subsystem invented NO deploy machinery of its own; it implemented `BIDeployable` and reused this engine.
This is a clean example of the corpus's recurring pattern (also [B567] batchJob, [B573] template) — a subsystem is
a thin specialization over a generic base the corpus had not opened.

## 596.4 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | DeployToComp holds a BIDeployable + Step[]; asks deployable.getSteps() and drives them (polymorphic deploy) | [CERT] | DeployToComp.java:19-50,65 | token-checked ✓ |
| 2 | .ntpl deploy sets params "exact"=TRUE (exact-name, no auto-rename); isDeployable gate | [CERT] | :31-46 | token-checked ✓ |
| 3 | ReplacingContext extends BasicContext (base + additional BFacets) | [CERT] | ReplacingContext.java:16-27 | token-checked ✓ |
| 4 | ReplacingContext carries a handle map: addAllHandles (capture) / hasHandle / removeHandle / restoreHandles (apply to new tree) | [CERT] | :30-50 | token-checked ✓ |
| 5 | This is the handle-preservation mechanism B578 §578.3 / B579 §579.2 relied on | [CERT-synthesis] | rows 3-4 + [B578]/[B579] | reasoned ✓ |
| 6 | Template deploy = implement BIDeployable + reuse this engine (no own deploy machinery) | [CERT-synthesis] | rows 1-4 + [B583] | reasoned ✓ |

**Marker tally**: [CERT] ×4 · [CERT-synthesis] ×2 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 4 of 6
rows token-verified inline.

## Connections

- **[Block 595]** (ST1) — the factory that selects `DeployToComp` for a `BIDeployable` source.
- **[Block 578]/[Block 579]** — the template installer/upgrade that CONSUMED these; §578.3/§579.2's handle
  preservation is LOCATED here (`ReplacingContext`).
- **[Block 583]** — `BNtplFile` is the `BIDeployable`; **[Block 594]** — `BWbDeployableNtplFile`.
- **[Block 6]** — links addressed by handle, which survive because `restoreHandles` re-assigns old handles.

## Open gaps (this block)

- `BIDeployable.Step` internals (what a single deploy step does) live on the DEPLOYABLE side (`BNtplFile`, [B583])
  not here — REMITTANCE. The exact `restoreHandles` matching (ORD vs name) is named, low value. Focus continues at
  ST3 (component strategies: CompToComp / IntraCompSpaceMove / CompToBog / ToNavFolder / DeleteOp).
