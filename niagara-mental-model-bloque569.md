# Block 569 — The software-distribution engine: a supervisor inventory mirrored from the local installable registry, plus software steps that COMBINE unconditionally (N module installs → one install transaction per station) and a passphrase-gated encrypted-dist install

**Session**: 2026-08-28
**Focus**: `provisioning` (gap PV3 — the software distribution subsystem: the installable inventory + the
combine-into-one-install optimization + encrypted-dist handling)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the `software` package (18 classes); the combine rule and the PBE
passphrase test token-verified inline. SECRETS DISCIPLINE: cite the encryption STRUCTURE, never a value.
**Primary sources** `[CERT]`:
- `organized/provisioningNiagara/provisioningNiagara-wb/vineflower/com/tridium/provisioningNiagara/software/
  {BSoftwareContainer,BInstallableSummary,BAbstractSoftwareStep,BInstallCombinedBySpecStep,
  BInstallDistWithPassPhraseStep,BInstallStep,BInstallBySpecStep}.java`.

**Scope**: how the supervisor knows what software exists and how it batches installs across a fleet. Locates the
adjacent-step combining [Block 39 §39.2.1] named and [Block 567] traced. Does NOT re-open the generic batch
engine ([Block 567]) or the at-rest dist encryption ([Block 466]) — connects to both.

---

## 569.1 The inventory: `BSoftwareContainer` mirrors the local installable registry [CERT]

`BSoftwareContainer extends BComponent implements LocalInstallableRegistry.RegistryListener` `[CERT] :37`. It
subscribes to the supervisor's `LocalInstallableRegistry` and, on `installableRegistered(inst)` `[CERT] :55-64`,
adds or updates a `BInstallableSummary` child keyed by the installable name (`SlotPath.escape(name)`), calling
`summary.addInstallable(inst)` when the name already exists. So it is the supervisor's live **inventory of
available software** — one `BInstallableSummary` per installable name, each aggregating its multiple versions
(`BInstallable`s). `DIST_DIR = "!cleanDist"` `[CERT] :41` is the clean-dist location. This is what the PV2
channel's `getInstallables`/`findInstallable` queries read.

## 569.2 Software steps combine UNCONDITIONALLY [CERT]

`BAbstractSoftwareStep extends BDeviceJobStep` `[CERT] :95` — every software operation is a per-device step
([Block 567] PV1). Its combining hooks `[CERT] :110-115`:
```java
public boolean canCombine(BDeviceJobStep step) { return step instanceof BAbstractSoftwareStep; }
public void    combine(BDeviceJobStep step)     { this.combinedSteps.add((BAbstractSoftwareStep) step); }
```
The rule is **permissive**: ANY two software steps targeting the same device combine — there is no per-module
compatibility test at this layer. Combined children are fanned to on completion
(`deviceNetworkJobComplete`/`deviceJobStepComplete` loop over `combinedSteps` `[CERT] :121-130`). This is the
concrete site of the [Block 39 §39.2.1] "adjacent software steps are combined" optimization that [Block 567]
traced to `BForEachDeviceStage.getCombinedSteps`: the *decision* lives here (`canCombine`), the *execution* in
the batch engine.

## 569.3 The merged install: `BInstallCombinedBySpecStep` [CERT]

`BInstallCombinedBySpecStep extends BAbstractSoftwareStep implements BIEncodable` `[CERT] :48` carries a
`toInstallList` (`BVector` of `BInstallableSpec`) `[CERT] :44-59`, constructed from a `List<BInstallableSpec>`
`[CERT] :70`. So when the engine combines, say, 10 module-install steps for one station, they collapse into a
**single** `BInstallCombinedBySpecStep` whose `toInstallList` holds all 10 specs — one install transaction to
the station's daemon instead of 10 round-trips. This is why upgrading a fleet with many module changes is one
install pass per subordinate.

## 569.4 Encrypted-dist install is passphrase-gated (PBE) [CERT]

`BInstallDistWithPassPhraseStep extends BAbstractSoftwareStep` `[CERT] :53` holds a `distPassphrase` property of
type `BPassword` (flags=5 = readonly+hidden) `[CERT] :43-64`. When the target is a `BDistribution`, it reads the
dist manifest's **PBE (password-based-encryption) info** and tests the passphrase before installing `[CERT]
:134-147`:
```java
PBEEncodingInfo distEncodingInfo = dist.getManifest().getPBEEncodingInfo();
if (distEncodingInfo != null) { ...
   if (!distEncodingInfo.test(AccessController.doPrivileged(this.getDistPassphrase()::getSecretChars))) { /* fail */ }
}
```
So an **encrypted `.dist`** (the passphrase-protected distribution domain from [Block 466]) can only be
installed with the correct passphrase, which the step carries as a reversible `BPassword` ([Block 562] AC4:
reversible because the installer must present the secret to `PBEEncodingInfo.test`). SECRETS DISCIPLINE: the
mechanism is PBE-tested-before-install; no passphrase value is exposed here. This is the fleet-side counterpart
to [Block 466]'s finding that the portable `.dist` is passphrase-encrypted.

## 569.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BSoftwareContainer is a RegistryListener mirroring LocalInstallableRegistry into BInstallableSummary children (per name, aggregating versions) | [CERT] | BSoftwareContainer.java:37,55-64 | token-checked ✓ |
| 2 | BAbstractSoftwareStep extends BDeviceJobStep; canCombine = any BAbstractSoftwareStep (unconditional) | [CERT] | BAbstractSoftwareStep.java:95,110-115 | token-checked ✓ |
| 3 | Combined children fanned on deviceNetworkJobComplete/deviceJobStepComplete | [CERT] | :121-130 | token-checked ✓ |
| 4 | BInstallCombinedBySpecStep carries toInstallList (BVector of BInstallableSpec) → one install txn per station | [CERT] | BInstallCombinedBySpecStep.java:44-70 | token-checked ✓ |
| 5 | BInstallDistWithPassPhraseStep: distPassphrase BPassword; PBE test via getPBEEncodingInfo().test(passphrase) pre-install | [CERT] | BInstallDistWithPassPhraseStep.java:43-64,134-147 | token-checked ✓ |
| 6 | Locates B39 §39.2.1 combining decision (canCombine) that B567 traced to the engine | [CERT] | rows 2 + [B39]/[B567] | cross-ref ✓ |

**Marker tally**: [CERT] ×6 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 6 rows token-verified
inline.

## Connections

- **[Block 567]** (PV1) — the batch engine; combining is DECIDED here (`canCombine`), EXECUTED there
  (`getCombinedSteps`/`safeExecuteParallel`).
- **[Block 39 §39.2.1]** — named adjacent-step combining; located to `BAbstractSoftwareStep.canCombine`.
- **[Block 466]** — the passphrase-encrypted portable `.dist`; PV3 is the install-side that tests that passphrase.
- **[Block 562]** (AC4) — the `distPassphrase` is a reversible `BPassword` (must present the secret to PBE.test).
- **[Block 568]** (PV2) — the channel's `getInstallables`/`findInstallable` queries read this inventory.

## Open gaps (this block)

- The full 18-class step catalog (BUpgradeOutOfDateStep decision logic, BStationInstallablesStep,
  BRebootJobStep sequencing) is sampled, not exhaustively enumerated — low value; [Block 39 §39.3] has the
  Niagara-step catalog. Focus continues at PV4 (bootstrap/discovery).
