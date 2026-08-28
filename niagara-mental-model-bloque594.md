# Block 594 — The template-wb tail: the Relation editor completes the Config/IO/Relation authoring trio, and a `.ntpl` is a first-class Workbench file — deployable from the browser (`BWbDeployableNtplFile`), with a Templates sidebar offering Find-Usages and Make-Module — closing the template-wb focus

**Session**: 2026-08-28
**Focus**: `template-wb` (gaps TW4 + TW5, collapsed — the Relation editor and the .ntpl WB file integration)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `BTemplateRelationEditor` + the `ui/file/` + `ui/sidebar/` classes.
Per the AUDIT-FIRST honest-depth assessment, TW4 and TW5 are thin enough to collapse into one tail block.
**Primary sources** `[CERT]`:
- `organized/template/template-wb/vineflower/com/tridium/template/ui/{BTemplateRelationEditor.java,
  file/{BWbDeployableNtplFile,BNtplFileMenuAgent,ExportApplicationCommand,ExportConfigsCommand}.java,
  sidebar/{BTemplateSideBar,BTemplateSideBarNavTree}.java}`.

**Scope**: the remaining template Workbench surface. Closes the `template-wb` focus. Does NOT re-open the rt
engine ([B577–B583]), the binding contract ([B200 §200.3]), or the relation model ([Block 579]) — UI over them.

---

## 594.1 TW4 — the Relation editor completes the authoring trio [CERT]

`BTemplateRelationEditor extends BEdgePane` `[CERT] :58` runs TWO `BTable`s — `relationTable` (a `RelateInfoModel`
over `BRelationInfo` `[CERT] :3,66,86-87`) and a `selectTable` (a selection model `[CERT] :67,93`). So it is the
third binding editor, alongside the Config and IO editors ([Block 591] TW1): where those author `BConfigBinding`
config/IO exposure, this authors `BRelationInfo` — WHICH relations the template exposes and preserves across
deploy/upgrade ([Block 579] §579.2 collects relations by handle). Config + IO + Relation is the complete set of
"what a template exposes" editors, one per binding kind ([Block 580] §580.1 manifest `<bindings>`/`<links>` +
relations).

## 594.2 TW5 — a `.ntpl` is a first-class Workbench deployable [CERT]

`BWbDeployableNtplFile extends BNtplFile implements BIDeployable` `[CERT] :90` — a template file in the WB browser
that IS deployable (`BIDeployable`), so an engineer can deploy a `.ntpl` straight from the file tree.
`BNtplFileMenuAgent extends BNavMenuAgent` `[CERT] :24` supplies its right-click menu (deploy/export). Two export
commands `[CERT]`: `ExportApplicationCommand` (export the application) and `ExportConfigsCommand` (export configs
to Excel — the export half of [Block 592] TW2's round-trip). So templates are managed as files: browse, deploy,
export.

## 594.3 TW5 — the Templates sidebar: Find-Usages and Make-Module [CERT]

`BTemplateSideBar extends BWbSideBar` `[CERT] :86` with a `BTemplateSideBarNavTree extends BNavTree`
`[CERT] :26` — the dedicated Templates sidebar. Beyond `Details` `[CERT] :383`, it carries two genuinely useful
commands `[CERT]`:
- **`FindUsages`** `[CERT] :399` — trace where a template is deployed/used across the station (the deployed
  `BTemplateConfig` instances of a given template).
- **`MakeModule`** `[CERT] :450` — package a template into a distributable Niagara **module**, turning a
  hand-built template into a shippable artifact.

These are WB-only capabilities with no rt-engine equivalent — the tooling that turns templating into a
development workflow (author → find usages → package as a module).

## 594.4 Focus close — template-wb, end to end [CERT-synthesis]

With the tail closed, `template-wb` is complete over four blocks. The Workbench template UI is: three binding
EDITORS (Config/IO/Relation, [Block 591]/here) that author what a template exposes as `BConfigBinding`/
`BRelationInfo` keyed by handle; a bulk-deploy IMPORT wizard ([Block 592]) that round-trips the parameter grid
through an (optionally encrypted) Excel workbook; a guided application-install wizard ([Block 593]) that wraps the
destructive rt swap in a default-on pre-install backup; and file-level integration (this block) making `.ntpl`s
deployable browser artifacts with Find-Usages and Make-Module tooling. It is a thin, standard-Workbench UI over
the rt engine ([B577–B583]) — no new engine logic, exactly as [B200 §200.6] implied, but with the substantive
editors and the safety/round-trip wrappers now documented.

## 594.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BTemplateRelationEditor (BEdgePane) = 2 BTables (RelateInfoModel over BRelationInfo + select) — the 3rd binding editor | [CERT] | BTemplateRelationEditor.java:58,66-93 | token-checked ✓ |
| 2 | BWbDeployableNtplFile extends BNtplFile implements BIDeployable (deploy from WB browser); BNtplFileMenuAgent = its menu | [CERT] | BWbDeployableNtplFile.java:90; BNtplFileMenuAgent.java:24 | token-checked ✓ |
| 3 | ExportApplicationCommand + ExportConfigsCommand (Excel export half of TW2 round-trip) | [CERT] | file/Export*Command.java:28,40 | token-checked ✓ |
| 4 | BTemplateSideBar (BWbSideBar) + nav tree; commands Details, FindUsages, MakeModule | [CERT] | sidebar/BTemplateSideBar.java:86,383,399,450 | token-checked ✓ |
| 5 | template-wb = thin standard-WB UI over the rt engine; no new engine logic | [CERT-synthesis] | rows 1-4 + [B591-593]/[B200 §200.6] | reasoned ✓ |

**Marker tally**: [CERT] ×4 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 4 of 5
rows token-verified inline.

## Connections

- **[Block 591]** (TW1) — Config/IO editors; this adds the Relation editor to complete the trio.
- **[Block 579]** (T5) — the `BRelationInfo` relations this editor authors, preserved by handle on upgrade.
- **[Block 592]** (TW2) — the ExportConfigs command here is that block's export counterpart.
- **[B200 §200.6]** — the overview; the template-wb focus (B591–B594) opened its four substantive areas.

## Open gaps (this block)

- Small dialogs (`BTemplateOptions`, `BTemplateHistoryDialog`, `BTemplateDeployProgressDialog`) and the
  `BTemplateManager` view internals (2705 L, mostly action dispatch already sketched in [B200 §200.6]) are
  low-value UI. **TW4+TW5 CLOSED; template-wb focus investigable=0 → STOP.**
