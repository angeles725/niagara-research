# Block 588 — Stateless hierarchy navigation: every `BLevelElem` carries a `contextParams` BFacets that ACCUMULATES the NEQL filter down the tree, so expanding a node re-runs its query on-demand (or walks the cache) with no server session state — `MakeElemUtil` is the factory that threads the filter

**Session**: 2026-08-28
**Focus**: `hierarchy` (gap H5 — on-demand tree generation and the contextParams filter-state thread)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `MakeElemUtil` + `BHierarchyService.getChildElems` + `BLevelElem`
contextParams; the accumulation and the cached-vs-on-demand branch token-verified inline.
**Primary sources** `[CERT]`:
- `organized/hierarchy/hierarchy-rt/vineflower/com/tridium/hierarchy/MakeElemUtil.java`.
- `organized/hierarchy/hierarchy-rt/vineflower/javax/baja/hierarchy/{BHierarchyService,BLevelElem}.java`.
- `[CERT-doc]` `niagara-help/guides-clean/Hierarchies/ContextParameters-10210055.txt`.

**Scope**: how a node is expanded WITHOUT a cache, and how the filter state travels. Complements H2 ([Block 585],
the cache) — this is the on-demand path and the state model that makes both work. Does NOT re-open the level-def
model ([Block 584]).

---

## 588.1 Every element carries accumulated filter state [CERT]

`BLevelElem` has a `contextParams` property (`BFacets`, default `BFacets.DEFAULT`) `[CERT] :51-64` plus
`elemTags` (`BFacets`) `[CERT] :57-65`. `MakeElemUtil.makeElem(...)` `[CERT] :55-64` computes each element's
contextParams by RESOLVING the parent's against the new level's:
```java
BFacets contextParams = getResolvedContextParams(
   levelDef, entity, SlotPath.escape(name), childPredicate,
   parent != null ? parent.getContextParams() : BFacets.DEFAULT, newContextParams);
return new BLevelElem(levelDef, parent, name, icon, contextParams, tags.toFacets());
```
So contextParams **accumulates down the tree** — a node's contextParams is its parent's plus this level's
contribution. It is the filter-state thread [B5 §5.3.3]'s cost warning hinted at.

## 588.2 The accumulation is a growing NEQL predicate [CERT]

The factory shows what each level contributes (`childPredicate` = the accumulated NEQL "groupingBase") `[CERT]`:
- `makeGroupElem(groupDef, parent, groupingBase, groupName, groupTagValue, …)` `[CERT] :73-89`:
  `childPredicate = groupingBase`, and it stores `BFacets.make(SlotPath.escape(groupBy), groupTagValue)` — so a
  GROUP node records "the `groupBy` tag equals THIS value", the filter that node represents. Its children inherit
  it.
- `makeListElem(listDef, namedGroupDef, parent, …, groupingBase, …)` `[CERT] :92-103`:
  `childPredicate = groupingBase + '(' + namedGroupDef.getQuery() + ')'` — the named group's NEQL AND-appended to
  the accumulated base (logged as the effective `neql:`).
- `makeHierarchyElem` `[CERT] :67` seeds the root; `makeEntityElem` `[CERT] :106` makes a real-component leaf.

So descending the tree builds a progressively-narrower NEQL (`Floor==1` → `Floor==1 AND equipType==AHU` → …),
and each element carries the exact predicate that produced it.

## 588.3 Expansion: cached walk OR on-demand re-query [CERT]

`BHierarchyService.getChildElems(defPath, contextParams, tags, context)` `[CERT] :305-346` branches on the cache:
```java
if ((Boolean) hierarchyRoots[0].fw(1300)) {                 // isCachedOnServer
   BLevelElem parent = getCachedParent(hierarchyRoots[0], contextParams);   // locate in the materialized tree
   return this.getChildElems(parent, context);              // return cached children (fw(1302))
} else {
   BLevelElem parent = new BLevelElem(levelDef, null, "parent", BIcon.DEFAULT, contextParams, tags);  // rebuild
   return this.getChildElems(parent, context);              // ON-DEMAND: re-run the query from contextParams
}
```
The key move: when NOT cached, the service **reconstructs the parent element purely from the incoming
`contextParams`** (the accumulated filter) and re-runs the level query. It needs NO server-side session state
between expansions — the client hands back the node's contextParams, and that fully determines what to compute.
When cached, `getCachedParent` locates the same node in the materialized tree and returns its stored children
(the H2/H6 `fw(1302)` path).

## 588.4 Why this design [CERT-synthesis]

Carrying the filter IN the element (contextParams) rather than in a server session is what lets the SAME engine
serve a stateless web client. A browser navigating a hierarchy over BOX (H7) never holds a server cursor: it
expands a node by echoing that node's contextParams, and the station either walks its cache or re-runs the NEQL —
identical result either way. It also means the cache is an OPTIMIZATION, not a correctness requirement: an
un-cached hierarchy is fully navigable (just slower, one NEQL per expansion, [Block 584 §584.4]), and a cached
one is the same tree read from memory. The accumulated-NEQL model is also why a deep hierarchy's leaf query can
be large — every ancestor level's predicate is ANDed in.

## 588.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BLevelElem carries contextParams (BFacets) + elemTags; MakeElemUtil.makeElem resolves parent's contextParams into the child's | [CERT] | BLevelElem.java:51-65; MakeElemUtil.java:55-64 | token-checked ✓ |
| 2 | makeGroupElem: childPredicate=groupingBase + stores groupBy=value facet (the node's filter) | [CERT] | MakeElemUtil.java:73-89 | token-checked ✓ |
| 3 | makeListElem: childPredicate = groupingBase + '(' + namedGroupDef.query + ')' (NEQL AND-append) | [CERT] | MakeElemUtil.java:92-103 | token-checked ✓ |
| 4 | getChildElems branches: fw(1300) cached → getCachedParent → fw(1302) children; else rebuild parent from contextParams → on-demand re-query | [CERT] | BHierarchyService.java:305-346 | token-checked ✓ |
| 5 | On-demand path is stateless: parent reconstructed purely from incoming contextParams | [CERT] | :336 | token-checked ✓ |
| 6 | Design lets a stateless web client navigate; cache is optimization not correctness | [CERT-synthesis] | rows 4-5 + [B585] | reasoned ✓ |

**Marker tally**: [CERT] ×5 · [CERT-doc] ×1 (ContextParameters) · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE =
EVIDENCE (decompilation + doc). 5 of 6 rows token-verified inline.

## Connections

- **[Block 584]** (H1) — the level defs whose predicates accumulate here; **[Block 585]** (H2) — the cached path
  (fw(1300)/fw(1302)) this branches to.
- **[Block 586]** (H3) — the accumulated NEQL is what `resolveQueryOnScopes` runs on the ForkJoinPool.
- **[Block 587]** (H4) — the `hierarchy:` scheme resolves intermediate segments to these BLevelElems.
- **H7** (this focus) — BOX/Fox transports carry contextParams to/from a stateless client.

## Open gaps (this block)

- `getResolvedContextParams` exact facet-merge rules and the `childPredicate`/`groupingBase` string grammar are
  named, partially traced (the accumulation is clear; the exact facet keys — levelDefPath/childPredicate/
  entityOrd/hierarchyOrd — are per-[ContextParameters] doc). Focus continues at H6 (permission enforcement).
