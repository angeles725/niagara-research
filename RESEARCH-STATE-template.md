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
covered_blocks: 1
gaps_closed: 1
known_gaps: 7
investigable_open: 6
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: template
status: active (1/7; T1→B577 DONE; NEXT T2 ApplicationTemplateInstaller)
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
| high | **T2 ApplicationTemplateInstaller** | station-side install/upgrade: reads the .ntpl, applies to the component tree via ReplacingContext, handles componentsToBeRemoved (NameTree) — the install path B200 excluded | `template-rt/…/application/{ApplicationTemplateInstaller,ApplicationTemplateUtil,BApplicationInstallSpecs}.java` | **NEXT** |
| high | **T5 UpgradeUtil transfer internals** | UpgradeUtil.upgrade Mark/DeployToComp transfer layer (physical bog-tree move) + rebuildRelations cross-template dependency preservation + save-data contract for bound slots — B200 §200.7 excluded Mark/DeployToComp | `template-rt/…/UpgradeUtil.java` | open |
| medium | **T3 manifest XML grammar** | full template-manifest.xml grammar: root attrs + child elements (settings/links/bindings/resources/subtemplates/tags/dependencies/revisions/optionals) + Value attrs; deepens B200 §200.3 binding contract | `template-rt/…/manifest/{ManifestXMLReader,ManifestXMLWriter,TemplateManifest,TemplateFileSpec}.java` + guide `guides-clean/Templates/ExampleTemplate.manifest*` | open |
| medium | **T4 subtemplate composition** | subtemplate nesting: TemplateManifest.Subtemplate[], how a parent tracks children + propagation when a subtemplate changes | `template-rt/…/manifest/TemplateManifest.java` + guide `guides-clean/Templates/UpdatingParentTemplate*` | open |
| medium | **T6 BTemplateChannel wire** | the upgradeTemplate Fox circuit wire (request deployedSlotPath, streamed running→complete/failed/canceled job events); deepens B200 §200.4 existence-only | `template-rt/…/BTemplateChannel.java` | open |
| low | **T7 TemplateManager resolution + memory scheme** | 3-dir resolution (templateDir/modDir/applicationDir), `file:~templates/` ORD rewrite, BMemoryScheme (`memory:` singleton) + BNewNtplFromTemporary lifecycle | `template-rt/…/file/{TemplateManager,BMemoryScheme,BMemoryFileSpace}.java` | open |

## Proven-absent / notes

- `guide-search "NiagaraTemplate api"` / `"TemplateSource"` → 0: the api/ programmatic façade is CODE-ONLY (no
  official docs). `guide-search "template manifest"` → 1 (example XML only). `docTemplates` doc tree is empty;
  docs live in `niagara-help/guides-clean/Templates/`.
- No docSource javadoc for the template module (decompiled only).
- Sweep count self-corrections: api = 10 (not 27; the 27 was api+impl), wb ui = 44 (not 65).

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: 6 (T2–T7). Focus ACTIVE.
- **Gaps closed**: 1 (T1→B577).
- **requires-execution / blocked**: 0.
- **Coverage metric**: 0 / 7.
