# RESEARCH-STATE — focus: template-wb (STOPPED)

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
covered_blocks: 4
gaps_closed: 5
known_gaps: 5
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: template-wb
status: stopped (5/5, investigable=0; 4 blocks B591-B594; TW4+TW5 collapsed into B594). §18 retro pending.
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
| high | ~~**TW2 Excel IMPORT path**~~ | BulkDeployWorkbook (Closeable POI reader, optionally password-encrypted, doPrivileged) parses per-sheet Input/Output/Config/Relation/Optional/Tag rows→values; BulkDeploy = wizard reusing installapp flow (backup→compat→confirm→install) per sheet; closes export round-trip | — | **CLOSED → B592** |
| medium | ~~**TW3 application-template wizard**~~ | guided StepWizardModel: select template→optional-components→compatibility→BACKUP(default on)→confirm; InstallingApplicationWorker runs backup-THEN-install (BBackupManager before the destructive B578 tree-swap); reused by bulk deploy | — | **CLOSED → B593** |
| medium | ~~**TW4 Relation editor**~~ | BTemplateRelationEditor (BEdgePane, 2 BTables over BRelationInfo) = 3rd binding editor completing Config/IO/Relation trio | — | **CLOSED → B594** |
| low | ~~**TW5 .ntpl WB file integration**~~ | BWbDeployableNtplFile (BIDeployable, deploy from browser), BNtplFileMenuAgent, Export commands, BTemplateSideBar w/ FindUsages + MakeModule | — | **CLOSED → B594** |

## Notes

- Shallow-not-worth-a-block (fold inline): UpgradeApplicationCommand, ui/tag/, small dialogs (BTemplateOptions,
  BTemplateHistoryDialog, BTemplateDeployProgressDialog), UpdateConfigs/UpdateUtil helpers.
- Honest ceiling: 4 substantive blocks (TW1 substantive alone; TW2/TW3 short concrete; TW4+TW5 collapsible into
  one tail synthesis).

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: **0** — ALL 5 closed (TW1–TW5; TW4+TW5 collapsed). Focus STOPPED (§8).
- **Gaps closed**: 5 (TW1→B591, TW2→B592, TW3→B593, TW4+TW5→B594).
- **Coverage metric**: 5 / 5 (100%), 4 blocks. Honest ceiling hit exactly (TW4+TW5 collapsed as predicted).
- **Unopened tail**: BTemplateManager view internals (2705 L, mostly action dispatch in B200 §200.6), small dialogs — low value.
