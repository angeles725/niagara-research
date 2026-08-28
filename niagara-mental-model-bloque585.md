# Block 585 — Hierarchy caching: an OPTIONAL job-built server-side materialization of the whole tree — `HierarchyCacheBuilder` walks grouping-then-entity defs (two query strategies), bakes each node's `BCategoryMask` up its ancestors, delivers the root via `fw(1304)`, and is governed by cacheStatus + two kill-switch sysprops + a SystemDb exclusion

**Session**: 2026-08-28
**Focus**: `hierarchy` (gap H2 — the caching architecture; why deep level sequences are affordable)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `HierarchyCacheBuilder` (693L) + `BHierarchy` cache section +
`BHierarchyCachingJob`; the build recursion, the two strategies, and the sysprops token-verified inline.
**Primary sources** `[CERT]`:
- `organized/hierarchy/hierarchy-rt/vineflower/com/tridium/hierarchy/{HierarchyCacheBuilder,BHierarchyCachingJob}.java`.
- `organized/hierarchy/hierarchy-rt/vineflower/javax/baja/hierarchy/BHierarchy.java`.
- `[CERT-doc]` `niagara-help/guides-clean/Hierarchies/CachingHierarchies-174DDD3A.txt`.

**Scope**: how the tree from [Block 584] is materialized so navigation is a read, not a per-level NEQL sweep.
The per-user READ-time permission FILTER (fw(1302)) is deferred to H6; H2 covers the BUILD (including baking
masks). Does NOT re-open the level-def model ([Block 584]) or BJob ([Block 511]) — uses both.

---

## 585.1 The cache is optional and job-built [CERT]

`BHierarchy` exposes cache controls `[CERT] :57-87`: `cacheStatus` (`BHierarchyCacheStatus`:
notCached/cached/notCachedOnStarted), `cacheOnStationStarted` (bool, default **false**), and the actions
`createCache` / `clearCache`. `createCache()` `[CERT] :160-161` submits a `BHierarchyCachingJob` (a `BSimpleJob`
run through `BJobService`, [Block 511]) that runs `HierarchyCacheBuilder`; on completion the finished cached root
is handed back through `fw(1304)` `[CERT]`. So caching is an explicit, asynchronous operation — a hierarchy runs
UN-cached by default (every expansion re-runs NEQL per level, [Block 584 §584.4]) until an operator or the
`cacheOnStationStarted` flag builds it.

## 585.2 The build algorithm: grouping-then-entity, two strategies [CERT]

`HierarchyCacheBuilder.buildCache(context)` `[CERT] :80-92`:
```java
int defIndex = this.gatherGroupingDefs(0, groupingDefs);          // collect leading GROUP/LIST defs (structure)
this.processEntityDef(nextDef, root, groupingDefs, defIndex+1, 0, context);  // expand entities under structure
this.estimateCacheSize();                                          // byte-accurate size estimate
```
`processEntityDef` `[CERT] :121-194` is the recursion. It re-gathers the grouping defs following the current
level, then branches on the entity level's `includeGroupingQueries` ([Block 584] `BQueryLevelDef`, default true):
- **`includeGroupingQueries = true`** `[CERT] :143-151`: the grouping predicates are folded INTO the entity NEQL
  — one combined query per group path (fewer, larger queries).
- **`includeGroupingQueries = false`** `[CERT] :163-176`: `appendAllGroupingElems(parent, groupingDefs,
  groupDefValues, queryContext)` pre-scans the distinct grouping values, builds the group nodes, then queries
  entities per group (more, smaller queries).

Each entity produces a leaf `BLevelElem`; grouping produces group nodes; the whole tree is materialized into
cached elements. `estimateCacheSize()` gives a byte estimate (so a huge hierarchy's cache cost is visible).

## 585.3 Permissions are baked in at build time [CERT]

For each entity, `setElemAndAncestorPermissions(entityElem, entity)` `[CERT] :194` propagates the entity's
`BCategoryMask` ([Block 561] AC3) **UP through its grouping ancestors**. So a cached GROUP node's mask is the
union of its descendants' categories — a group is potentially visible if ANY child is. The masks are stored in
the cached tree; at READ time `fw(1302)` (getServerCacheChildren) applies a per-user filter (`hasOperatorRead`) —
so the cache holds the FULL tree once, and each user sees a permission-filtered view of it (full detail in H6).

## 585.4 The governors: status, boot-build, kill-switches, SystemDb [CERT]

- `cacheOnStationStarted` `[CERT] :85,153` — build the cache automatically at station start.
- **Two kill-switch sysprops** `[CERT] :96-101`: `niagara.hierarchy.caching.disabled` (caching off entirely) and
  `niagara.hierarchy.caching.disableOnStationStarted` (skip the boot build) — read once via a `PrivilegedAction`
  `Boolean.getBoolean`. Each has a lexicon message shown when it suppresses caching `[CERT] :101-102,195,211`.
- **SystemDb exclusion** `[CERT]`: caching is disabled for the SystemDb scope (a dedicated
  `hierarchy.caching.disabled.systemdb.message` lexicon key `[CERT] :101`) — the system database is not cached.

So an operator has graduated control: per-hierarchy (createCache/clearCache/cacheOnStationStarted), station-wide
(the two sysprops), and an automatic SystemDb carve-out.

## 585.5 Thesis [CERT-synthesis]

The cache trades freshness for speed. Un-cached, a hierarchy is always current but pays a full NEQL/relation
sweep per expansion ([Block 584]); cached, navigation is an O(read) walk of a materialized, permission-baked
tree, but the tree is **stale until rebuilt** — there is no automatic invalidation on station data change (the
model is explicit `createCache`/`clearCache` or a boot build). The build is a proper `BJob` (cancellable,
progress-tracked, off the engine thread), it estimates its own size, and it bakes category permissions in so
read-time filtering is cheap. This is the standard tradeoff a large site makes: rebuild the hierarchy cache on a
schedule/after bulk changes, and navigate fast in between.

## 585.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Cache is optional: cacheStatus + createCache/clearCache actions + cacheOnStationStarted (default false); built by BHierarchyCachingJob (BSimpleJob), root via fw(1304) | [CERT] | BHierarchy.java:57-87,160; BHierarchyCachingJob.java | token-checked ✓ |
| 2 | buildCache = gatherGroupingDefs → processEntityDef recursion → estimateCacheSize | [CERT] | HierarchyCacheBuilder.java:80-92 | token-checked ✓ |
| 3 | Two strategies on includeGroupingQueries: inline combined query (true) vs pre-scan appendAllGroupingElems per group (false) | [CERT] | :143-176 | token-checked ✓ |
| 4 | setElemAndAncestorPermissions propagates entity BCategoryMask up grouping ancestors (baked at build) | [CERT] | :194 | token-checked ✓ |
| 5 | Two kill-switch sysprops (caching.disabled / caching.disableOnStationStarted) via PrivilegedAction; lexicon messages | [CERT] | BHierarchy.java:96-102,195,211 | token-checked ✓ |
| 6 | SystemDb scope excluded from caching (dedicated lexicon message) | [CERT] | BHierarchy.java:101 | token-checked ✓ |
| 7 | Cache trades freshness for speed; no auto-invalidation (explicit rebuild) | [CERT-synthesis] | rows 1-4 + [B584] | reasoned ✓ |

**Marker tally**: [CERT] ×6 · [CERT-doc] ×1 (CachingHierarchies) · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE =
EVIDENCE (decompilation + doc). 6 of 7 rows token-verified inline.

## Connections

- **[Block 584]** (H1) — the level-def tree the cache materializes; the per-level NEQL cost the cache avoids.
- **[Block 511]** — BJob; the cache build is a BSimpleJob.
- **[Block 561]** (AC3) — BCategoryMask, baked up the tree here; **H6** — the fw(1302) read-time per-user filter.
- **[Block 587?]** H3 — QueryUtil ForkJoinPool runs the per-scope NEQL the builder issues.

## Open gaps (this block)

- The exact `fw(1300–1303)` server-cache read protocol (isCachedOnServer / addServerCacheChild /
  getServerCacheChildren / getLevelDef) is named here but its read-side detail belongs to H5 (on-demand vs
  cached) and H6 (permission filter). Focus continues at H3 (scope + parallel executor).
