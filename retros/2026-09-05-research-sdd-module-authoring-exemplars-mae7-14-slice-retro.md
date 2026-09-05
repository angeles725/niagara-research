<!-- review-status: applied 2026-09-05 · subsumed by FOCUS-CLOSE · kit c4788a7 -->
<!-- Marker lifecycle: maintainer flips 'pending' → 'applied <date> · kit <sha>' (or 'dismissed') once folded; sweep-retros.sh reads this (METHODOLOGY §18). -->
# Retro — niagara-research · research-sdd · 2026-09-05 · focus `module-authoring-exemplars`, slice MAE7–MAE14 (B778–B785) — kit deltas for /build-n4-module

> Run reviewed: the investigador1 slice of the two-lane census (companero owns MAE1–MAE6/B772–B777). Eight blocks,
> one per dimension, each = a delegated decompile sweep + INLINE grep-verify of every load-bearing cite, ending with
> a "Kit implication" section naming the destination file. READ-ONLY on the build kit — PROPOSES only (§18); I do
> NOT edit `$KIT`. Per-block deltas were also messaged to the lead as they landed; this consolidates them + the one
> research-tools proposal the lead routed here.

## Proposed kit deltas (for `/build-n4-module`)

| # | Proposed change | Target (file) | Evidence (block · key cite) | Priority |
|---|---|---|---|---|
| MAE7Δ | Author-side SPIs section: custom SERVICE (`extends BAbstractService` + `getServiceTypes(){return new Type[]{TYPE};}` = registration-by-placement under /Services, hook `serviceStarted()`); new ORD SCHEME (`extends BOrdScheme` BSingleton + `@NiagaraType(ordScheme="id")` + `resolve()`); SERVER-side subscription (`extends Subscriber` + `event(BComponentEvent)` + `subscribe(c,depth,cx)`) | `types/logic.md` | B778 · BSystemMonitorService:134 / BHandleScheme:55,66 / UserMonitor:147 | MED |
| MAE8Δ | Child-tree containers "pick by cardinality": frozen `@NiagaraProperty` (fixed) / runtime `add(name,BValue)` + `reorder(Property[])` (data-driven) / typed `BFolder` (growable); NO `BComponentList`; typed-tree legality via `isChildLegal`/`isParentLegal` (default true) `instanceof` vetoes | `types/logic.md` | B779 · BControlPoint:34 / BComponent:1118,1369,1381 | MED |
| MAE9Δa | `module.palette` convention (bare-Type-minus-B names, plural folders, `m="alias=module"` once, nested `<p>` pre-seed) + lexicon PREFIXING rule (flat/module-global → `parent.child`/`Type.slot` to dodge B759) | `types/dashboard.md` | B780 · control-rt/module.palette; alarm-rt.lexicon:27 | MED |
| MAE9Δb | Dual-surface `@AgentOn` registration (write `@NiagaraType(agent={@AgentOn(types={"mod:Type"},requiredPermissions="r")})`; Slot-o-Matic emits `<type><agent><on/></agent></type>`) | `types/wb-widgets.md` | B780 · BAlarmDbMaintenance:76; alarm-wb module.xml:52 | MED |
| MAE10Δ | Grouping/relating declaration postures (THREE distinct): categories = NO author scaffold (runtime-only, BICategorizable); relations = never subclass `BRelation` (concrete carrier), define a type via `relationId`+`RelationInfo`/`BCustomRelation`; hierarchy = compose `BHierarchy`+`BLevelDef` variants under `BHierarchyService` | `types/logic.md` | B781 · BComponent:84 / BRelation:44 / BLevelDef:46 | MED |
| MAE11Δ | ONE "query/search/index surface" recipe (not four): declare a typed `BQuery`/NEQL payload + plug the matching `BIAgent` provider (BQueryEngine/BColumnsProvider/BISearchProvider/BSystemIndexer) discovered by the agent registry → read a `BITable` | `types/logic.md` | B782 · BQuery:62 / BISearchProvider:27 / BSystemIndexer:244 | MED |
| MAE12Δ | Template author path = ARTIFACT PRODUCTION, not type registration: a "template type" is an `.ntpl` ZIP (bog + `template-manifest.xml`) made by a job from a `BTemplateConfig`-marked subtree — explicitly DO NOT scaffold a `BTemplate` subclass (no such SPI) | `types/logic.md` | B783 · TemplateType:8 / BTemplateConfig:107 / NiagaraTemplate:57 | LOW |
| MAE13Δ | module.xml conventions: profile split `-rt`/`-ux`/`-wb`/`-se`(server), `-doc` is a SEPARATE `runtimeProfile="doc"` module (never a part); `<dependency>` `vendorVersion` = 3-part Tridium FLOOR (`4.14.0`) vs the module's own 4-part build stamp (`4.14.0.162`) | `METHODOLOGY.md` / `corpus-index.md` | B784 · alarm-rt/module.xml:2,9; docMicros-doc:8 | MED |
| MAE14Δ | "Extend a framework via a Device + a self-describing SPI object" pattern (rdb dialect exemplar): `B<X>Database extends BRdbms`/`BEncryptableTransportRdbms` + 3 abstract methods; `getRdbmsContext()`→60-method `RdbmsDialect`; register `<type>`; no central registry | `types/logic.md` | B785 · BRdbms:290,308,326 / RdbmsDialect:9 | LOW |

### Cross-cutting META-delta (the highest-value synthesis)
**MAE7 + MAE11 + MAE14 reveal ONE recurring Niagara extension idiom**: *subclass a framework base + register a
`<type>`/agent in module.xml + hand back a self-describing SPI object* — services (`getServiceTypes`), ORD schemes
(`resolve`), query/search/index providers (a `BIAgent`), and rdb dialects (`RdbmsDialect`) all follow it; there is
never a central switch/registry the author edits. → PROPOSED `types/logic.md` opening note: teach this idiom ONCE, then
the per-surface blocks are instances of it. HIGH value (it compresses ~5 mechanisms into one mental model).

## Research-tools lane proposal (NOT a build-kit delta — routed here by the lead)
- **`module_nav palette-lexicon-agents <module>`** — a read-only module-navigator subcommand dumping (i) every `<p n=
  t= m=>` from `module.palette`, (ii) all `key=value` from `*.lexicon` grouped by prefix WITH a duplicate-bare-key
  collision report (operationalizes B759 — a check no human should do by eye), (iii) all `<type>/<agent>/<on>` from
  module.xml cross-checked against `@AgentOn` in source. Also: teach `module_nav resources` the
  `organized/<mod>/<sub>/extracted/` + `/vineflower/` layout (it currently expects `modules/<name>.jar`). Evidence:
  B780. → for the niagara-research `tools/` lane. (The lexicon dup-key WARN separately goes to build-kit
  `slot-coverage.sh` per the lead.)

## Already covered (dedupe — proof the retro read the kit + corpus first)
- ORD/BQL cursor, agent registry, BComponent slot machinery, module.xml MECHANISM, template deploy/consume, category/
  relation/hierarchy RUNTIME models, rdb READ-side dialects → B5/B758, B20/B757, B4/B33, B12/B629-B636/B754, B573,
  B11/B48/B584-B586/B758, B402-B413. Every MAE block CITES these and adds only the AUTHOR-side residue.

## What went well (keep)
- INLINE grep-verify caught THREE delegated-sweep errors before they shipped as [CERT]: BComponent line numbers
  (B779), @AgentOn line numbers (B780), and the audit's "`reorder` unused" / "`BComponentList` exists" hypotheses
  (B779, both refuted/confirmed by grep). Reinforces: a delegated sweep's file:line is a HYPOTHESIS until grep-verified.
- Two high-value NEGATIVE findings (categories have no author scaffold B781; template has no subclass SPI B783) —
  a research lane telling the kit what NOT to generate is as valuable as what to generate.
- The lane discipline (edit only my rows, pull --rebase before each push, hand-recompute the envelope) kept a
  two-writer shared corpus consistent across 8 concurrent-lane blocks with zero clobbers.
