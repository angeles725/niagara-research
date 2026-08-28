# RESEARCH-STATE — focus: hierarchy (ACTIVE)

> Multi-focus corpus (METHODOLOGY §16). SEEDED by an AUDIT-FIRST coverage sweep (§13) on 2026-08-28 (delegated
> sonnet, prior-coverage reconciliation first, verified inline).
>
> **Angle (§b2):** the Niagara CUSTOM-NAVIGATION engine (`hierarchy` module) — alternate nav trees built from
> station data via a sequence of LEVEL DEFINITIONS. Genuinely unopened: [B5 §5.3.3] is a one-page conceptual
> overview (names the types, gives a cost warning), [B565] is the RBAC role↔hierarchy SEAM only, [B387] is one
> license-gate row. This focus opens the ENGINE mechanics B5 never touched. Read-only, decompiled-Java + the rich
> official `guides-clean/Hierarchies/` section (32 files — first corpus use). Corpus language = **English**.
>
> **Scope:** the hierarchy engine: level-def model, cache build, scope/parallel-query, ORD scheme, on-demand tree
> gen, permission enforcement, transport. Does NOT re-derive [B5] (overview), [B565] (BRoleHierarchies seam), or
> the NEQL grammar ([B5 §5.3.2]/[B21]).

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 6
gaps_closed: 6
known_gaps: 7
investigable_open: 1
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: hierarchy
status: active (6/7; H1→B584 … H6→B589 DONE; NEXT H7 transport BOX+Fox)
seeded_from: AUDIT-FIRST coverage sweep 2026-08-28 (delegated sonnet; B5/B565/B387 verified inline)
seeded_on: 2026-08-28
gaps_total: 7 investigable (H1–H7)
gaps_closed: 0
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Surface (audit, scoped counts — verify inline)

`hierarchy-rt/vineflower/javax/baja/hierarchy/` ~19 (BHierarchy 417L extends BLevelDef, BHierarchyService 484L
singleton, BLevelDef 129L abstract, BGroupLevelDef/BListLevelDef/BQueryLevelDef/BRelationLevelDef,
BIGroupingLevelDef/BIEntityLevelDef markers, BNamedGroupDef, BLevelElem 511L, BHierarchyScheme 138L,
BHierarchyScope/ScopeContainer, BHierarchyTags, BHierarchySpace, BLevelSort, HierarchyQuery). `com/tridium/
hierarchy/` ~8 (HierarchyCacheBuilder 693L, BHierarchyCachingJob, BHierarchyCacheStatus, HierarchyUtil,
MakeElemUtil, QueryUtil ForkJoinPool, IHierarchyCacheBuilder). `com/tridium/hierarchy/fox/` 2 (BFoxHierarchyChannel,
BFoxHierarchySpace) + BHierarchyBoxChannel. `hierarchy-ux` ~8 + `hierarchy-wb` ~4 (mostly B565 seam / low value).

## REMITTANCE (cite, do NOT re-derive)

- BRoleHierarchies mixin (RBAC seam) → **[B565]**
- "hierarchy" license feature gate (BHierarchyService:129) → **[B387]**
- "hierarchy:" scheme existence (ORD table row) → **[B5 §5.1.2]**
- Conceptual overview (BHierarchy/level types/BLevelElem taxonomy) → **[B5 §5.3.3]** (shallow — NOT engine mechanics)
- NEQL syntax/semantics used by level defs → **[B5 §5.3.2]/[B21]** (foundation)

## Gap-backlog (prioritized) — the unopened engine

| Priority | Gap | Scope | Where (`organized/…` + `guides-clean/Hierarchies/`) | Status |
|---|---|---|---|---|
| high | ~~**H1 level-definition model**~~ | hierarchy = tree of BLevelDefs (root BHierarchy is one); getElements(parent,cx) per level; 2 axes GROUP (tag/list folders) vs ENTITY (query/relation leaves); doc-corroborated | — | **CLOSED → B584** |
| high | ~~**H2 caching architecture**~~ | optional job-built tree materialization; buildCache gathering→processEntityDef (2 strategies via includeGroupingQueries); BCategoryMask baked up ancestors; cacheStatus/cacheOnStationStarted + 2 kill-switch sysprops + SystemDb exclusion; no auto-invalidation | — | **CLOSED → B585** |
| high | ~~**H3 scope + parallel executor**~~ | scope=BOrd (local subtree or cross-station); license 2 flags local/system (neither→configFatal, unlicensed dropped); QueryUtil dedicated ForkJoinPool CPUs×8 default (2 sysprop knobs); resolveQueryOnScopes parallel per-scope+merge | — | **CLOSED → B586** |
| medium | ~~**H4 BHierarchyScheme ORD resolution**~~ | hierarchy: = BSpaceScheme, HierarchyQuery extends SlotPath; resolve walks segments (name[0]=hierarchy); leaf = grouping name→BLevelElem OR escaped station:| →REAL component (user-checked); local vs Fox dispatch | — | **CLOSED → B587** |
| medium | ~~**H5 on-demand tree gen + contextParams**~~ | STATELESS nav: BLevelElem carries contextParams (accumulated NEQL filter); expand = client echoes contextParams → cached walk (fw1300/1302) OR rebuild-parent + on-demand re-query; MakeElemUtil threads the filter | — | **CLOSED → B588** |
| medium | ~~**H6 permission enforcement in the tree**~~ | entity applied BCategoryMask (AC3) baked on BLevelElem + OR-propagated up to group ancestors (group visible if any child is); fw(1302) read filters per-user hasOperatorRead (super-user all); orthogonal to role-scoping (B565); group name can leak | — | **CLOSED → B589** |
| low | **H7 transport (BOX + Fox)** | BHierarchyBoxChannel load/resolve (web UX), BFoxHierarchyChannel getLevelElems circuit (remote station), BFoxHierarchySpace proxy | `com/tridium/hierarchy/BHierarchyBoxChannel.java`, `com/tridium/hierarchy/fox/{BFoxHierarchyChannel,BFoxHierarchySpace}.java` | **NEXT** |

## Proven-absent / notes

- No existing block on the hierarchy engine (B5/B565/B387 reconciled). `guides-clean/Hierarchies/` = 32 files,
  first corpus use — the primary doc source. `guide-search "BHierarchy"` → 2 files.
- `BHierarchyTags`, `hierarchy-ux` UI classes — unread, low value; classify on demand.
- `fw(501, "hierarchy.limit")` capacity-limit gate — opaque fw protocol, not opened.

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: 1 (H7). Focus ACTIVE.
- **Gaps closed**: 6 (H1→B584 … H6→B589).
- **Coverage metric**: 0 / 7.
