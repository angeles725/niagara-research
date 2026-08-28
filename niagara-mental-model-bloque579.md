# Block 579 — `UpgradeUtil`: the save→remove→deploy→restore upgrade transaction — it captures the template's internal AND external links/relations by HANDLE, does a `Mark`+`DeployToComp` physical bog-tree swap, then restores config + history names + rebuilds relations, excluding BRelation/BHistoryConfig/BPlatformService from the overwrite

**Session**: 2026-08-28
**Focus**: `template` (gap T5 — `UpgradeUtil` Mark/DeployToComp transfer internals; [Block 200 §200.7] excluded them)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `UpgradeUtil` (1546 lines) — the upgrade sequence, the save-data
capture, the transfer, and the restore/rebuild; token-verified inline.
**Primary sources** `[CERT]`:
- `organized/template/template-rt/vineflower/com/tridium/template/UpgradeUtil.java`.

**Scope**: how a template upgrade physically replaces a deployed subtree WITHOUT losing user config or severing
the wiresheet. [Block 200 §200.4] gave the 4-phase overview; §200.7 excluded the `Mark`/`DeployToComp` detail.
T5 opens it. Complements T2 ([Block 578], which uses the same `ReplacingContext`). Does NOT re-open the binding
contract ([Block 200 §200.3]) — connects.

---

## 579.1 The upgrade transaction — four phases [CERT]

`upgrade(deployedRoot, ntplFile, progress, relationSpecs)` `[CERT] :229-273` runs a save→remove→deploy→restore
transaction:
```java
TemplateSaveData templateSavedData = saveTemplateData(deployedRoot);        // 1. SAVE user state
collectRelationSpecs(deployedRoot, templateSavedData, relationSpecs);       //    capture cross-template wiring
Mark mark = new Mark(ntplFile, deployName);                                 // 2. mark the new source
rootParent.remove(deployedRootProperty, replacingContext);                  //    REMOVE old subtree
... deploy (params "exact"=TRUE) ...                                        // 3. DEPLOY new tree (DeployToComp)
BComponent newDeployRoot = rootParent.get(deployName).asComponent();
restoreTemplateSaveData(templateSavedData, newDeployRoot, ...);             // 4. RESTORE config + links
restoreHistoryExtensions(historyExtMap, newDeployRoot);                     //    RESTORE history names
```
The physical move is `javax.baja.space.Mark` + `com.tridium.sys.transfer.DeployToComp` `[CERT] :7,38,246` under a
`ReplacingContext` — the same transfer layer T2's installer uses. The old subtree is removed and the new one
deployed under the SAME `deployName` (`exact=TRUE`), so the deployed slot keeps its identity.

## 579.2 What is saved — internal AND external wiring, by handle [CERT]

`saveTemplateData(deployedRoot)` `[CERT] :299-307` captures far more than config values:
```java
inputLinks           = getInputLinks(deployedRoot, templateConfig);       // links INTO the template
outputKnobs          = getOutputKnobs(deployedRoot, templateConfig);      // knobs OUT of the template
externalLinks        = getExternalLinks(deployedRoot, handleMap);         // links crossing the boundary (by HANDLE)
externalKnobs        = getExternalKnobs(deployedRoot, handleMap);
externalOutputRelations = getExternalRelations(deployedRoot, handleMap);
externalInputRelations  = getExternalRelationKnobs(deployedRoot, handleMap);
```
The critical detail is the `handleMap`: EXTERNAL links/relations (wiresheet connections between the template and
the REST of the station) are captured **keyed by component handle**, not slot path. Because components keep their
handles across the `ReplacingContext` swap ([Block 578] §578.3), these external connections remain re-resolvable
after the tree is replaced. `collectRelationSpecs` `[CERT] :313-321` gathers all eight categories (input/output
links + knobs, internal + external relations) into `RelationSpec`s for later rebuild.

## 579.3 What survives untouched, and what is restored [CERT]

- **Excluded from the overwrite** `[CERT] :65`: `EXCLUDE_TYPES = {BRelation, BHistoryConfig, BPlatformService}`
  — relations, history config, and the platform service are NOT replaced by the template deploy, so they persist
  intact through the swap.
- **Preserved per-type** `[CERT] :66-68`: `SAVE_TYPE_PROPERTIES = {BHistoryExt → historyName}` — a history
  extension's `historyName` is saved and restored, so the upgraded template keeps logging to the SAME history id
  (no orphaned/duplicated history).
- **Restored** `[CERT] :270-271,477`: `restoreTemplateSaveData` re-applies the saved config + links onto
  `newDeployRoot`; `restoreHistoryExtensions` re-applies history names.
- **Rebuilt** `[CERT] :511-515`: `rebuildRelations(base, relationSpecs)` iterates the collected `RelationSpec`s
  and calls `rebuildLink` (for links) or the relation rebuild — reconnecting the cross-template wiring after the
  new tree is in place.

## 579.4 Subtemplates recurse first [CERT]

`upgradeSubtemplate` `[CERT] :131` recursively upgrades nested subtemplates within `upgradeTemplates`, and the
top-level `upgradeTemplates(configs, redeployValues, progress)` `[CERT] :72-97` calls `rebuildRelations(base,
relationSpecs)` ONCE at the end — so all subtemplate upgrades collect their relation specs, and the whole set is
rebuilt together after every subtree is deployed (T4 territory for the composition detail).

## 579.5 Thesis [CERT-synthesis]

A template upgrade is a **state-preserving physical swap**. The naive act (remove old subtree, deploy new) would
sever every wiresheet link and lose all runtime config. `UpgradeUtil` avoids that by (1) capturing internal +
external wiring by HANDLE before the swap, (2) excluding relations/history-config/platform from the overwrite,
(3) preserving history names per-type, and (4) restoring config + rebuilding relations after. The handle-keyed
external capture ([Block 578]'s handle preservation) is the linchpin — it is what lets an engineer re-issue a
template across a fleet without re-wiring each station. This is the mechanism [Block 200 §200.4] promised as
"save→remove→deploy→restore" and §200.7 left unopened.

## 579.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | upgrade = saveTemplateData → collectRelationSpecs → Mark+remove(ReplacingContext)+DeployToComp (exact) → restore+rebuild | [CERT] | UpgradeUtil.java:229-273 | token-checked ✓ |
| 2 | Physical transfer = javax.baja.space.Mark + com.tridium.sys.transfer.DeployToComp; new tree under same deployName | [CERT] | :7,38,246 | token-checked ✓ |
| 3 | saveTemplateData captures internal (inputLinks/outputKnobs) AND external (externalLinks/knobs/relations) wiring, external keyed by handleMap | [CERT] | :299-307 | token-checked ✓ |
| 4 | EXCLUDE_TYPES = {BRelation, BHistoryConfig, BPlatformService} not overwritten | [CERT] | :65 | token-checked ✓ |
| 5 | SAVE_TYPE_PROPERTIES preserves BHistoryExt.historyName across upgrade | [CERT] | :66-68 | token-checked ✓ |
| 6 | restoreTemplateSaveData + restoreHistoryExtensions + rebuildRelations reconnect after the swap | [CERT] | :270-271,477,511-515 | token-checked ✓ |
| 7 | Handle-keyed external capture is what preserves cross-template wiresheet links | [CERT-synthesis] | rows 3 + [B578 §578.3] | reasoned ✓ |

**Marker tally**: [CERT] ×6 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 6 of 7
rows token-verified inline.

## Connections

- **[Block 200 §200.4/§200.7]** — the make/install/upgrade overview; §200.7 excluded Mark/DeployToComp, opened here.
- **[Block 578]** (T2) — the installer uses the same `ReplacingContext`; handle preservation there is what makes
  §579.2's handle-keyed external capture resolvable.
- **[Block 6]** — the link/knob model (BLink) whose connections are saved/rebuilt here.
- **T3/T4** (this focus) — the manifest `optional`/`subtemplates` arrays that drive which components are kept.

## Open gaps (this block)

- The exact `rebuildLink`/relation-rebuild resolution (how a saved RelationSpec re-binds to the new tree's slots)
  is named, partially traced — deep-transfer detail, low marginal value. Focus continues at T3 (manifest XML grammar).
