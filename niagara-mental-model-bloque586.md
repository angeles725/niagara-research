# Block 586 — Hierarchy scopes and the parallel executor: a hierarchy queries a set of scope ORDs (local subtree or cross-station), the `hierarchy` license gates scope KINDS by two flags (local/system), and `QueryUtil` fans each scope's NEQL across a dedicated ForkJoinPool sized CPUs × 8 by default

**Session**: 2026-08-28
**Focus**: `hierarchy` (gap H3 — the scope model + the parallel query executor)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `BHierarchyScope` + the `BHierarchyService` license gate +
`QueryUtil`; scope licensing and the ForkJoinPool sizing token-verified inline.
**Primary sources** `[CERT]`:
- `organized/hierarchy/hierarchy-rt/vineflower/javax/baja/hierarchy/{BHierarchyScope,BHierarchyScopeContainer,
  BHierarchyService}.java`.
- `organized/hierarchy/hierarchy-rt/vineflower/com/tridium/hierarchy/QueryUtil.java`.
- `[CERT-doc]` `niagara-help/guides-clean/Hierarchies/HierarchyScopes-101F0812.txt`.

**Scope**: WHERE a hierarchy's level queries run and HOW they are parallelized. Connects the level-def model
([Block 584]) to cross-station queries ([Block 414–420]) and the NEQL foundation ([Block 21]). Does NOT re-open
NEQL syntax or the supervisor join.

---

## 586.1 A scope is an ORD; a hierarchy has a set of them [CERT]

`BHierarchyScope extends BComponent` `[CERT] :17` holds one property: `scopeOrd` (`BOrd`, default `NULL`)
`[CERT] :12-18`. A scope names WHERE the hierarchy's NEQL/relation queries run — a local subtree, or a
cross-station target (`station:`/`sys:` ORD, [Block 343]/[Block 414–420]). `BHierarchyScopeContainer` holds a
`BHierarchyScope[]` (it is the parent-legal container for scopes, [Block 584] `BHierarchy.isChildLegal` admits
`BHierarchyScopeContainer`). So a single hierarchy definition can aggregate MANY scopes — e.g. a supervisor
hierarchy spanning every subordinate station.

## 586.2 The license gates scope KINDS: local vs system [CERT]

The `hierarchy` license feature carries two booleans `[CERT] BHierarchyService.java:130-133`:
```java
allowLocalHierarchy  = feature.getb("local",  false);
allowSystemHierarchy = feature.getb("system", false);
if (!allowLocalHierarchy && !allowSystemHierarchy) configFatal("Unlicensed for all scopes. No hierarchies are allowed.");
```
`getLicensedScopes(scopes)` `[CERT] :139-148` then DROPS any scope the license doesn't permit:
```java
if ((allowLocalHierarchy  || !isLocalScope(scope)) &&
    (allowSystemHierarchy || !isSystemScope(scope)))  scopesList.add(scope);
```
So **LOCAL** scopes (this station) and **SYSTEM** scopes (the SystemDb / cross-station system space) are
independently licensed. A station licensed only for `local` silently drops system scopes; a station licensed for
neither faults the service outright. This is the licensing lever behind "can this station build a
supervisor-wide hierarchy".

## 586.3 The executor: a dedicated ForkJoinPool, CPUs × 8 [CERT]

`QueryUtil` `[CERT] :41` owns a static `ForkJoinPool queryExecutor` `[CERT] :53`. Its parallelism `[CERT]
:44-79`:
```java
// niagara.hierarchy.threadsPerCPU default 8 ; niagara.hierarchy.threads default 0 (absolute override)
int parallelism = THREAD_COUNT_OVERRIDE > 0 ? THREAD_COUNT_OVERRIDE
                                            : Runtime.getRuntime().availableProcessors() * multiplier;   // multiplier=8
queryExecutor = new ForkJoinPool(parallelism, NreForkJoinWorkerThreadFactory.DEFAULT_INSTANCE,
                                 new UncaughtHierarchyExceptionHandler(), true /* asyncMode */);
```
So by default the pool is **`availableProcessors() × 8`** worker threads (an 8-core box → 64 hierarchy query
threads), tunable two ways: `niagara.hierarchy.threadsPerCPU` (the multiplier) or `niagara.hierarchy.threads`
(an absolute override that wins when > 0). `resolveQueryOnScopes(def, query, traverseBaseOrd, timeout, cx)`
`[CERT] :95` runs the level's NEQL **against each licensed scope in parallel** on this pool and returns a merged
`CloseableIterator<Entity>` (with a timeout). `spyExecutor(SpyWriter)` `[CERT] :58` exposes the pool on `/spy`
for diagnostics.

## 586.4 Thesis [CERT-synthesis]

A hierarchy is inherently a MULTI-SCOPE, parallel query workload. The level-def tree ([Block 584]) defines WHAT
to compute; the scope set defines WHERE (which subtrees/stations); the license decides which scope KINDS are
allowed; and `QueryUtil`'s CPUs×8 ForkJoinPool is what makes fanning a level's NEQL across many scopes (and,
across a supervisor, many stations) affordable at interactive speed — feeding the cache builder ([Block 585]) or
an on-demand expansion. The high default multiplier (8) reflects that hierarchy queries are I/O-bound
(cross-station Fox round-trips), so oversubscribing CPU threads keeps the pool busy while scopes wait on the
network. The knobs let a large supervisor tune the fan-out.

## 586.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BHierarchyScope = one scopeOrd (BOrd); BHierarchyScopeContainer holds the set | [CERT] | BHierarchyScope.java:12-18; BHierarchyScopeContainer.java | token-checked ✓ |
| 2 | License = two flags local/system; neither → configFatal | [CERT] | BHierarchyService.java:130-133 | token-checked ✓ |
| 3 | getLicensedScopes drops scopes not permitted by local/system flags | [CERT] | :139-148 | token-checked ✓ |
| 4 | QueryUtil static ForkJoinPool; parallelism = CPUs × threadsPerCPU(default 8) or niagara.hierarchy.threads override | [CERT] | QueryUtil.java:44-80 | token-checked ✓ |
| 5 | resolveQueryOnScopes runs NEQL per licensed scope in parallel, merged CloseableIterator + timeout; spyExecutor for /spy | [CERT] | :58,95 | token-checked ✓ |
| 6 | Multi-scope parallel workload; high multiplier reflects I/O-bound cross-station queries | [CERT-synthesis] | rows 1-5 + [B414-420] | reasoned ✓ |

**Marker tally**: [CERT] ×5 · [CERT-doc] ×1 (HierarchyScopes) · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE =
EVIDENCE (decompilation + doc). 5 of 6 rows token-verified inline.

## Connections

- **[Block 584]** (H1) — the level defs whose NEQL these scopes run; **[Block 585]** (H2) — the cache builder
  consumes `resolveQueryOnScopes`.
- **[Block 343]/[Block 414–420]** — cross-station `sys:` queries; a SYSTEM scope spans the supervisor's fleet.
- **[Block 21]/[Block 260–270]** — NEQL/tags foundation the scope queries evaluate.
- **[Block 561]** (AC3) — category permissions applied to the scope results (H6).

## Open gaps (this block)

- `isLocalScope`/`isSystemScope` exact predicates (how a scope ORD is classified) and the merge/dedup of
  overlapping scope results are named, low value. Focus continues at H4 (BHierarchyScheme ORD resolution).
