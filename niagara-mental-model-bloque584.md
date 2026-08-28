# Block 584 — The hierarchy level-definition model: a hierarchy is a TREE of `BLevelDef`s (root `BHierarchy` is one) along two axes — GROUP levels (tag-grouping / named-list, structure folders) vs ENTITY levels (NEQL query / relation-follow, real components) — each level's `getElements(parent,cx)` producing that level's children

**Session**: 2026-08-28
**Focus**: `hierarchy` (gap H1 — the level-definition model; the core of the custom-nav engine, unopened by B5)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `BLevelDef` + the 4 concrete types + the 2 interfaces + the root;
model corroborated with the official `AboutLevelDefinitions` guide (`[CERT-doc]`).
**Primary sources** `[CERT]`:
- `organized/hierarchy/hierarchy-rt/vineflower/javax/baja/hierarchy/{BLevelDef,BHierarchy,BGroupLevelDef,
  BListLevelDef,BQueryLevelDef,BRelationLevelDef,BIGroupingLevelDef,BIEntityLevelDef}.java`.
- `[CERT-doc]` `niagara-help/guides-clean/Hierarchies/AboutLevelDefinitions-101B1B58.txt`.

**Scope**: what defines a custom hierarchy and how its tree shape is determined. [B5 §5.3.3] named the types in a
table; H1 opens the model + the `getElements` contract. Does NOT re-derive NEQL ([B5 §5.3.2]/[B21]) or the cache
(H2) — connects.

---

## 584.1 A hierarchy is a tree of `BLevelDef`s [CERT] + [CERT-doc]

`abstract class BLevelDef extends BComponent` `[CERT] :25`. The root of a hierarchy is itself a level def:
`final class BHierarchy extends BLevelDef` `[CERT] :78`. The official guide confirms the model `[CERT-doc]`:
*"Each hierarchy is defined as a tree of LevelDefs where there is a unique LevelDef for each node of the tree."*
So a hierarchy definition is a `BHierarchy` root with child `BLevelDef`s; the levels are an ORDERED SEQUENCE
walked by `getNext()`/`getPrevious()` `[CERT] :70-89` (via `CompUtil.getDescendants(root, BLevelDef.class)` —
the next level def in document order). **The tree shape = the level sequence.**

## 584.2 The contract: `getElements(parent, cx)` [CERT]

The single abstract method is `BLevelElem[] getElements(BLevelElem parent, Context cx)` `[CERT] :58`. Given a
parent element and a context (carrying the accumulated filter state, H5), a level def produces the child elements
AT THAT LEVEL. Tree generation is therefore level-by-level: the root's `getElements` yields the first level, each
result's `getNext().getElements(elem, cx)` yields the next, down to an entity level whose elements are real
components. `getResolvedContextParams` `[CERT] :117-121` threads the filter state (`BFacets`) down each step.

## 584.3 Two axes: GROUP levels vs ENTITY levels [CERT] + [CERT-doc]

Two marker interfaces split the four types `[CERT]`:

**GROUPING levels** (`BIGroupingLevelDef` `[CERT]`) — "structure/placeholder folders" that create GROUP nodes
(`[CERT-doc]`: *"Group and list level definitions, basically placeholder folders, set up the structure"*):
- `BGroupLevelDef implements BIGroupingLevelDef` `[CERT] :45` — `groupBy` (a tag Id) + `tags` (`BHierarchyTags`)
  `[CERT] :46-49`. `getElements` runs a NEQL grouping on the `groupBy` tag, collecting **distinct tag values**
  into a `LinkedHashMap` (order-preserving), one group node per value. Doc: *"a node based on distinct tag values
  … Marker tags should not be used in a GroupLevelDef"* `[CERT-doc]`.
- `BListLevelDef implements BIGroupingLevelDef` `[CERT] :26` — **static named groups**: one or more
  `BNamedGroupDef` children, each carrying a NEQL query (marker + value tags allowed). Sort defaults to `none`
  (declaration order preserved). Doc: *"a node based on one or more NamedGroupDefs … require one or more"*
  `[CERT-doc]`.

**ENTITY levels** (`BIEntityLevelDef` `[CERT]`) — create LEAF nodes that are REAL station components:
- `BQueryLevelDef implements BIEntityLevelDef` `[CERT] :38` — `query` (NEQL String), `includeGroupingQueries`
  (bool, default **true**), `sort` (default `ascending`) `[CERT] :26-41`. `getElements` runs the NEQL query to
  produce entity nodes.
- `BRelationLevelDef implements BIEntityLevelDef` `[CERT] :69` — `inboundRelationIds` / `outboundRelationIds`
  (comma-separated), `filterExpression`, `repeatRelation` (bool), `cachingRepeatLimit` (int, MIN-faceted), `sort`
  `[CERT] :29-72` (+ deprecated `relationId`/`inbound`). It follows relations from the parent entity to produce
  child entities; `repeatRelation` follows the SAME relation recursively (bounded by `cachingRepeatLimit`) — e.g.
  an equip-contains-equip chain.

## 584.4 Why this matters [CERT-synthesis]

The four types are a small, composable grammar for reshaping a station. An engineer builds a "By Floor → By
Equipment Type → Points" hierarchy as `[GroupLevelDef groupBy=floor] → [GroupLevelDef groupBy=equipType] →
[QueryLevelDef query=points]`, and the SAME station renders under a completely different tree WITHOUT moving a
component. Grouping levels add structure (tag-driven or hand-named); entity levels bind to real components (by
query or by relation-following). Because each level is a NEQL/relation evaluation over the whole station, a deep
hierarchy is query-expensive — which is exactly why the caching layer (H2) exists ([B5 §5.3.3]'s cost warning).

## 584.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BLevelDef abstract (BComponent); BHierarchy extends BLevelDef (root is a level def); hierarchy = tree of LevelDefs | [CERT]+[CERT-doc] | BLevelDef.java:25; BHierarchy.java:78; AboutLevelDefinitions | token+doc ✓ |
| 2 | Single contract getElements(parent,cx)→BLevelElem[]; getNext/getPrevious walk the ordered level sequence | [CERT] | BLevelDef.java:58,70-89 | token-checked ✓ |
| 3 | Two interfaces: BIGroupingLevelDef (Group/List) vs BIEntityLevelDef (Query/Relation) | [CERT] | BI*LevelDef.java + the 4 impls | token-checked ✓ |
| 4 | BGroupLevelDef groupBy tag → distinct values (LinkedHashMap); marker tags not used | [CERT]+[CERT-doc] | BGroupLevelDef.java:46-49; AboutLevelDefinitions | token+doc ✓ |
| 5 | BListLevelDef = static NamedGroupDefs (NEQL each); BQueryLevelDef query+includeGroupingQueries(true)+sort(asc) | [CERT] | BListLevelDef.java:26; BQueryLevelDef.java:26-41 | token-checked ✓ |
| 6 | BRelationLevelDef inbound/outboundRelationIds + filterExpression + repeatRelation + cachingRepeatLimit | [CERT] | BRelationLevelDef.java:29-72 | token-checked ✓ |
| 7 | Four types = composable grammar; deep hierarchy is query-expensive → motivates cache (H2) | [CERT-synthesis] | rows 1-6 + [B5 §5.3.3] | reasoned ✓ |

**Marker tally**: [CERT] ×5 · [CERT-doc] ×2 (shared rows) · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE =
EVIDENCE (decompilation + official doc). 6 of 7 rows token-verified inline.

## Connections

- **[B5 §5.3.3]** — the shallow overview; H1 opens the model + getElements contract.
- **[B21]/[B260–B270]** — tags/NEQL: grouping levels group by tags, query levels run NEQL (foundation).
- **[B565]** (AC7) — BRoleHierarchies scopes WHICH hierarchies a role sees; this is what a hierarchy IS.
- **H2** (this focus) — the cache that makes deep level sequences affordable; **H5** — contextParams thread.

## Open gaps (this block)

- The exact NEQL string a `BGroupLevelDef`/`BQueryLevelDef` builds (the `neql:` composition) and `BNamedGroupDef`
  filter detail fold into H5 (on-demand generation) — the getElements bodies are read at the model level here.
  Focus continues at H2 (caching architecture).
