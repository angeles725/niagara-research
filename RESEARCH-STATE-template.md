# RESEARCH-STATE — focus: template (ACTIVE)

> Multi-focus corpus (METHODOLOGY §16). SEEDED by an AUDIT-FIRST coverage sweep (§13) on 2026-08-28 (delegated
> sonnet, verified inline).
>
> **Angle (§b2):** the generic Niagara TEMPLATE engine (`com.tridium.template`, module `template`) as a dedicated
> focus — specifically the DEEPENING of the internals **[Block 200] named out-of-scope**. B200 is the breadth
> block (.ntpl=zip+bog+manifest, BTemplateConfig, binding contract, 3 template types, make/install/upgrade
> overview, signature, channel existence, templateBulk=POI, wb UI). This focus opens what B200's header
> explicitly excluded: `api/impl/*TemplateSource`, `ApplicationTemplateInstaller`, `Mark`/`DeployToComp`
> transfer internals, the full manifest XML grammar, subtemplate composition, and TemplateManager resolution.
> Read-only, decompiled-Java (no docSource for template) + official `guides-clean/Templates/`. Corpus language =
> **English**.
>
> **Scope:** the template ENGINE internals. Does NOT re-derive [Block 200] (breadth), the provisioning fleet
> wrapper ([Block 573]), or `easyBinding` ([Block 36]/[Block 81]; B200 X6 = its own subsystem).

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 4
gaps_closed: 4
known_gaps: 7
investigable_open: 3
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: template
status: active (4/7; T1→B577, T2→B578, T5→B579, T3→B580 DONE; NEXT T4 subtemplate composition)
seeded_from: AUDIT-FIRST coverage sweep 2026-08-28 (delegated sonnet; B200 coverage verified inline)
seeded_on: 2026-08-28
gaps_total: 7 investigable (T1–T7)
gaps_closed: 0
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Surface (audit, scoped counts — verify inline before promoting to [CERT])

`template-rt` = `organized/template/template-rt/vineflower/com/tridium/template/`: root pkg ~13
(BTemplateService, BTemplateChannel, BTemplateConfig, BConfigBinding, BPasswordBinding, BRelationInfo,
BTemplateSignature, UpgradeUtil), `api/` ~10 (NiagaraTemplate façade + TemplateProperty/Element/Value/Scope/Type
enums), `api/impl/` ~17 (TemplateSource strategy hierarchy), `file/` ~16 (BNtplFile extends BZipFile,
TemplateManager, BMemoryScheme), `manifest/` ~4 (TemplateManifest bean + ManifestXML Reader/Writer +
TemplateFileSpec), `application/` ~3 (ApplicationTemplateInstaller + Util + BApplicationInstallSpecs), `job/` ~8
(BMake*/BInstall*/BUpgrade* jobs). `template-wb ui/` ~44 (Workbench UI, low priority). `templateBulk` = Apache
POI wrapper (`com/tridium/excel/impl/`, ~20) loaded reflectively — NO template logic (B200 §200.5).

## REMITTANCE — [Block 200] breadth (cite, do NOT re-derive)

- `.ntpl` = zip + bog + manifest structure → **[B200 §200.1]**
- 3 template types (Component/Device · Application · Station) → **[B200 §200.2]**
- Binding CONTRACT (BConfigBinding/BPasswordBinding/BRelationInfo + password-strip gotcha) → **[B200 §200.3]**
- Make/Install/Upgrade 4-phase OVERVIEW → **[B200 §200.4]**
- BTemplateSignature / drift detection → **[B200 §200.4]**
- BTemplateChannel Fox registration + "upgradeTemplate" circuit EXISTENCE → **[B200 §200.4]**
- templateBulk = Apache POI + Excel export → **[B200 §200.5]**
- template-wb UI overview (Graphics tab = PxEditor) → **[B200 §200.6]**
- Provisioning fleet wrapper (ProvisionTemplateManager, deploy/upgrade steps) → **[B573]** (PV7)
- EasyTemplates / easyBinding → **[B36]/[B81]** (B200 X6 = separate subsystem)

## Gap-backlog (prioritized) — B200-named-out-of-scope internals

| Priority | Gap | Scope | Where (`organized/…`) | Status |
|---|---|---|---|---|
| high | ~~**T1 api/impl TemplateSource strategy**~~ | NiagaraTemplate AutoCloseable façade + fail-loud TemplateSource strategy base (17 in api/impl); factory type-dispatch CREATE vs OPEN(Combined); 5-step .ntpl save pipeline | — | **CLOSED → B577** |
| high | ~~**T2 ApplicationTemplateInstaller**~~ | module-compat gate (missing=hard fail), ReplacingContext swap (auto-start off) clear+install preserving handles (relations survive); install vs upgrade (config-save + optional diff); passwordNote | — | **CLOSED → B578** |
| high | ~~**T5 UpgradeUtil transfer internals**~~ | save→remove→deploy→restore txn; captures internal+external wiring by HANDLE; Mark+DeployToComp physical swap; EXCLUDE_TYPES {Relation,HistoryConfig,PlatformService}; restore config+historyName+rebuildRelations | — | **CLOSED → B579** |
| medium | ~~**T3 manifest XML grammar**~~ | `<template>` root metadata + 10 child arrays; settings/links/bindings = typed Value (num/bool/str/cfg/in/out/px) with req+slotPath+min/max/units (the parameter schema); manifest is authoritative grammar (format code-only) | — | **CLOSED → B580** |
| medium | **T4 subtemplate composition** | subtemplate nesting: TemplateManifest.Subtemplate[], how a parent tracks children + propagation when a subtemplate changes | `template-rt/…/manifest/TemplateManifest.java` + guide `guides-clean/Templates/UpdatingParentTemplate*` | **NEXT** |
| medium | **T6 BTemplateChannel wire** | the upgradeTemplate Fox circuit wire (request deployedSlotPath, streamed running→complete/failed/canceled job events); deepens B200 §200.4 existence-only | `template-rt/…/BTemplateChannel.java` | open |
| low | **T7 TemplateManager resolution + memory scheme** | 3-dir resolution (templateDir/modDir/applicationDir), `file:~templates/` ORD rewrite, BMemoryScheme (`memory:` singleton) + BNewNtplFromTemporary lifecycle | `template-rt/…/file/{TemplateManager,BMemoryScheme,BMemoryFileSpace}.java` | open |

## Proven-absent / notes

- `guide-search "NiagaraTemplate api"` / `"TemplateSource"` → 0: the api/ programmatic façade is CODE-ONLY (no
  official docs). `guide-search "template manifest"` → 1 (example XML only). `docTemplates` doc tree is empty;
  docs live in `niagara-help/guides-clean/Templates/`.
- No docSource javadoc for the template module (decompiled only).
- Sweep count self-corrections: api = 10 (not 27; the 27 was api+impl), wb ui = 44 (not 65).

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: 3 (T4,T6,T7). Focus ACTIVE.
- **Gaps closed**: 4 (T1→B577, T2→B578, T5→B579, T3→B580).
- **requires-execution / blocked**: 0.
- **Coverage metric**: 0 / 7.
