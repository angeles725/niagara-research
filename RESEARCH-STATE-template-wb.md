# RESEARCH-STATE — focus: template-wb (ACTIVE)

> Multi-focus corpus (METHODOLOGY §16). SEEDED by an AUDIT-FIRST coverage sweep (§13) on 2026-08-28 (delegated
> sonnet, prior-coverage reconciliation first, verified inline).
>
> **Angle (§b2):** the WORKBENCH UI layer of the template subsystem (`com.tridium.template.ui`) — the DEEPENING
> of what **[Block 200 §200.6]** covered only as an overview paragraph. The rt engine is fully done ([Block
> 577–583], template focus). This is the tail: the substantive binding editors, the Excel IMPORT path, the
> application-template wizard, and the .ntpl Workbench file integration. Read-only, decompiled-Java. English.
>
> **Scope:** the template Workbench UI. Does NOT re-derive the rt engine ([B577–B583]), the PX editor embedded in
> the Graphics tab ([B191]/[B198]/[B210–214]), the BOG tab standard-WB widgets ([B15]), or the Tags API the tag
> chooser calls ([B260–B270]).

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 1
gaps_closed: 1
known_gaps: 5
investigable_open: 4
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: template-wb
status: active (1/5; TW1→B591 DONE; NEXT TW2 Excel import path)
seeded_from: AUDIT-FIRST coverage sweep 2026-08-28 (delegated sonnet; B200 §200.6 verified inline)
seeded_on: 2026-08-28
gaps_total: 5 investigable (TW1–TW5); honest ceiling ~4-6 blocks (TW4/TW5 may collapse into one tail synthesis)
gaps_closed: 0
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Surface (audit, scoped counts — verify inline)

`com/tridium/template/ui/` = ~39 direct `.java` + subdirs: `sidebar/` (2), `installapp/` (13 wizard),
`upgradeapp/` (1), `file/` (7), `tag/` (3) → ~65 total (B200 §9 said "66", rounding). Key views: BTemplateManager
(2705 L, main deployed-template view), BTemplateConfigEditor (1675 L), BTemplateIOEditor (1853 L),
BTemplateRelationEditor (~600 L), BTemplatePxEditor (embeds BPxEditorPane → REMITTANCE B191/B198), BTemplateBogEditor
(thin WB wrapper → REMITTANCE), BulkDeploy + BulkDeployWorkbook (Excel import, POI).

## REMITTANCE (cite, do NOT re-derive)

- BTemplatePxEditor (Graphics tab) = embeds BPxEditorPane → **[B191]/[B198]/[B210–B214]**
- BTemplateBogEditor (BOG tab) = BNavTree+BPropertySheet+BWireSheet+BSlotSheet thin wrapper → **[B15]**
- BTemplateManager overview + Excel EXPORT path → **[B200 §200.6]**
- All rt install/upgrade logic surfaced in wb → **[B577–B583]**
- UpgradeApplicationCommand (trivial wrapper over UpgradeUtil) → **[B579]** (no block)
- Tag chooser (ui/tag/, 3 thin classes calling tag API) → **[B260–B270]**
- Fleet/provisioning template deploy → **[B573]**

## Gap-backlog (prioritized)

| Priority | Gap | Scope | Where (`organized/…`) | Status |
|---|---|---|---|---|
| high | ~~**TW1 binding editors (Config + IO)**~~ | Config editor = BTable over BConfigBindings (rows=exposed params, save keyed by HANDLE, legacy composite-link migration); IO editor = 3 BTables (I/O bindings + source/io tag panes via TagSupport); UI over same BConfigBinding model as rt/manifest | — | **CLOSED → B591** |
| high | **TW2 Excel IMPORT path** | BulkDeploy + BulkDeployWorkbook: how a .xlsx is parsed row-by-row into binding values, password decryption, the deploy loop, the "Slot Path Scope" column — B200 §200.6 covered EXPORT only | `template-wb/…/ui/{BulkDeploy,BulkDeployWorkbook}.java` | **NEXT** |
| medium | **TW3 application-template wizard** | installapp/ (13-class WidgetUiHandler multi-step: backup → compatibility gate → optional-components chooser → install worker) — deploys a BApplicationTemplate with a backup-before-install step | `template-wb/…/ui/installapp/` (13) | open |
| medium | **TW4 Relation editor** | BTemplateRelationEditor: two-table model (BRelationInfo rows + relate-info pairs); B200 §200.6 does not mention it | `template-wb/…/ui/BTemplateRelationEditor.java` | open (collapsible w/ TW5) |
| low | **TW5 .ntpl WB file integration** | BWbDeployableNtplFile (how .ntpl shows as deployable in WB nav), BNtplFileMenuAgent (context menu), ExportApplication/ExportConfigsCommand, sidebar nav | `template-wb/…/ui/file/*`, `ui/sidebar/*` | open (collapsible w/ TW4) |

## Notes

- Shallow-not-worth-a-block (fold inline): UpgradeApplicationCommand, ui/tag/, small dialogs (BTemplateOptions,
  BTemplateHistoryDialog, BTemplateDeployProgressDialog), UpdateConfigs/UpdateUtil helpers.
- Honest ceiling: 4 substantive blocks (TW1 substantive alone; TW2/TW3 short concrete; TW4+TW5 collapsible into
  one tail synthesis).

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: 4 (TW2–TW5). Focus ACTIVE.
- **Gaps closed**: 1 (TW1→B591).
- **Coverage metric**: 0 / 5.
