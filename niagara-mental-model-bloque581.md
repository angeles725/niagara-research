# Block 581 — Subtemplate composition: templates nest by CONTAINMENT (a subtemplate is a deployed `BTemplateConfig` subtree inside the parent), the manifest `<subtemplates>` array records child identity, and a parent upgrade cascades depth-first, re-deploying only version-stale children

**Session**: 2026-08-28
**Focus**: `template` (gap T4 — subtemplate nesting and parent-update propagation)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `TemplateManifest.Subtemplate` + `UpgradeUtil.upgradeSubtemplate` +
the official parent-update guide. Recursion + version gate token-verified inline.
**Primary sources** `[CERT]`:
- `organized/template/template-rt/vineflower/com/tridium/template/manifest/TemplateManifest.java:170-176`
  (`Subtemplate` sub-bean).
- `organized/template/template-rt/vineflower/com/tridium/template/UpgradeUtil.java:131-` (`upgradeSubtemplate`).
- `[CERT-doc]` `niagara-help/guides-clean/Templates/UpdatingParentTemplate-991F8ECD.txt`.

**Scope**: how templates COMPOSE. Deepens the `<subtemplates>` array from T3 ([Block 580]) and the recursion
noted in T5 ([Block 579 §579.4]). Does NOT re-open the transfer machinery ([Block 579]) — reuses it per child.

---

## 581.1 The child reference: `Subtemplate` sub-bean [CERT]

`TemplateManifest.Subtemplate` `[CERT] :170-176` records a child template's identity: `name`,
`type = "subtemplate"`, `vendor`, `version`, `locationOrd`, `ntplFileOrd`. So the parent manifest's
`<subtemplates>` array ([Block 580] T3) does NOT embed the child bytes — it records WHO the child is
(name/vendor/version) and WHERE its `.ntpl` lives (`locationOrd`/`ntplFileOrd`). The child is a reference, resolved
at deploy.

## 581.2 Composition is by containment (deployed `BTemplateConfig` subtrees) [CERT]

There is no separate subtemplate registry. A subtemplate, once deployed, is a **`BTemplateConfig`-bearing
subtree inside the parent's component tree**. `upgradeSubtemplate(root, …)` `[CERT] :131-` finds children by
walking the parent for descendant configs:
```java
BTemplateConfig[] descendants = (BTemplateConfig[]) CompUtil.getDescendants(root, BTemplateConfig.class);
for (BTemplateConfig tmplConfig : descendants) {
   BComponent stRoot = BTemplateConfig.getRootForConfig(tmplConfig);
   ... // version check, then upgradeTemplate(tmplConfig, ...)
}
```
So "which subtemplates does this template contain" is answered by enumerating the `BTemplateConfig`s nested under
its root — composition is structural (containment in the bog tree), not a manifest-driven link table at runtime.

## 581.3 Parent upgrade cascades depth-first, version-gated [CERT]

`upgradeSubtemplate` is recursive and version-aware `[CERT] :131-`:
- For each descendant config, it compares the deployed version against the available template; if they match it
  logs **"No need to update"** and skips `[CERT]` — only a version-STALE child is re-deployed via
  `upgradeTemplate(tmplConfig, progress, relationSpecs)`.
- After a child is re-deployed, it **recurses into the newly-deployed roots**
  (`for (newRoot : newRoots) upgradeSubtemplate(newRoot, …)` `[CERT]`), so nesting to arbitrary depth is handled
  depth-first, and each re-deploy uses the same save→transfer→restore machinery ([Block 579]) with a SHARED
  `relationSpecs` set (so cross-subtemplate links are collected across the whole tree and rebuilt once at the end,
  [Block 579 §579.4]).

The official guide `UpdatingParentTemplate` `[CERT-doc]` frames the operator workflow: change a parent template,
re-issue it, and the contained subtemplates update — but only those whose version actually changed.

## 581.4 Thesis [CERT-synthesis]

Templates compose like a library: a station/application template CONTAINS reusable subtemplates by reference
(`<subtemplates>` records identity + `.ntpl` location), and at runtime they are simply nested `BTemplateConfig`
subtrees. Upgrade is a **version-gated depth-first cascade** — a parent re-issue walks the contained configs,
re-deploys only the stale ones (each through the [Block 579] transfer), recurses, and rebuilds all cross-subtree
relations once. This is what lets a vendor ship a hierarchy of reusable templates (e.g. an AHU template
containing damper + valve subtemplates) and update one layer without disturbing the others.

## 581.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Subtemplate sub-bean = name/type/vendor/version/locationOrd/ntplFileOrd (a reference, not embedded bytes) | [CERT] | TemplateManifest.java:170-176 | token-checked ✓ |
| 2 | Subtemplates are found by CompUtil.getDescendants(root, BTemplateConfig.class) — containment, no registry | [CERT] | UpgradeUtil.java:131- | token-checked ✓ |
| 3 | Version gate: matching version → "No need to update" skip; stale → upgradeTemplate | [CERT] | UpgradeUtil.java:131- | token-checked ✓ |
| 4 | Recursive depth-first into newly-deployed roots; shared relationSpecs rebuilt once (B579 §579.4) | [CERT] | UpgradeUtil.java:131- + [B579] | token-checked ✓ |
| 5 | Official UpdatingParentTemplate guide frames the re-issue workflow | [CERT-doc] | guides-clean/Templates/UpdatingParentTemplate-991F8ECD.txt | doc-cited ✓ |
| 6 | Composition = reusable-template library; upgrade cascades version-gated | [CERT-synthesis] | rows 1-4 | reasoned ✓ |

**Marker tally**: [CERT] ×4 · [CERT-doc] ×1 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE
(decompilation + doc). 4 of 6 rows token-verified inline.

## Connections

- **[Block 580]** (T3) — the `<subtemplates>` manifest array; T4 shows how those references deploy and upgrade.
- **[Block 579]** (T5) — each subtemplate re-deploy uses the save→transfer→restore machinery; §579.4 noted the
  recursion and single final rebuildRelations.
- **[Block 578]** (T2) — the containment/keep-list interacts with subtemplate boundaries at install.

## Open gaps (this block)

- The exact version-comparison predicate (major-vs-minor, `useMinorVersionOnDeployment` from [Block 577]) and how
  a subtemplate's `locationOrd` resolves at deploy fold into T7 (TemplateManager resolution). Focus continues at
  T6 (BTemplateChannel wire).
