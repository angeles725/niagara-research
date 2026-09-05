# B782 · Building a query / search / index surface — one uniform pattern (BQuery payload + a BIAgent provider → BITable) (MAE11, D10)

> **Scope**: how to AUTHOR a query-surface / station-index module — an area with 0 prior dedicated blocks. The key
> result: `query`, `queryTable`, `search`, and `systemIndex` are FOUR faces of ONE pattern — expose a typed
> `BQuery`/NEQL-ORD payload, register a `BIAgent` provider discovered by the agent registry, and bottom out in a
> `BITable` cursor. Execution/cursor internals are REMITTANCE (B5/B758 ORD/BQL cursor, B402-B413 BQL execution);
> this block is the AUTHOR-side SPI map. Focus: `module-authoring-exemplars` (MAE11 / D10). Kit destination:
> `types/logic.md`.
>
> **Sources**: FUENTE 3 decompiled — `query-rt` (`BQuery`, `BQueryEngine`, `BICompiledQuery`), `queryTable-wb`
> (`BQueryTable`, `BColumnsProvider`), `search-rt` (`BSearchService`, `BISearchProvider`, `BBqlSearchProvider`),
> `systemIndex-rt` (`BSystemIndexService`, `BIIndexQueryProvider`, `BSystemIndexer`), `niagaraSystemIndex-rt`;
> all present, verified this session at `organized/`. READ-ONLY. English (post-B115).

---

## 782.1 — The uniform pattern `[CERT]`
Every one of the four surfaces = **(payload) a typed `BQuery`/NEQL-ORD** + **(plug) a `BIAgent` provider** the
registry discovers (`@AgentOn` + `AgentFilter.is(TYPE)`) + **(result) a `javax.baja.collection.BITable` cursor**.
Learn one recipe, get all four. The provider interface changes per surface (execute / shape / search / index) but
the shape is identical.

## 782.2 — QUERY: `BQuery` fluent builder + `BQueryEngine` (BIAgent) → `BICompiledQuery.execute()` `[CERT]`
The parameterized query is a persistable `@NiagaraType` component built fluently: `BQuery.select(BProjection)`
(`query-rt/.../BQuery.java:62`), `select(BProjectionColumn)` (:70), plus `from(BExtent)`, `where(BPredicate|
BExpression)`, `groupBy`, `orderBy` — the typed AST form of NEQL/BQL (helper builders `Exprs`/`Predicates`/`Columns`).
Execution is an AGENT on the space: `BQueryEngine implements BIAgent`, resolved via `BQueryEngine.make(BSpace)` (an
agent filter), with `compile(BQuery, BOrd) → BICompiledQuery` and abstract `doCompile()`; `BICompiledQuery.execute()`
returns a `BITable` (`.../BICompiledQuery.java:26,28`). **Author move**: model the request as a `BQuery`; to make a
new space queryable, register a `BQueryEngine` subclass as a `BIAgent` on the space and implement `doCompile()`.

## 782.3 — QUERY-TABLE: `BQueryTable` wraps a `BQuery`, `BColumnsProvider` (BIAgent) shapes it `[CERT]`
`BQueryTable extends BComponent` (`queryTable-wb/.../BQueryTable.java:27`) wraps a single `BQuery` property — the
author surface for "expose a query as a table" is dropping a `BQueryTable` and configuring its `query` slot. Column/
row shape is a pluggable `BColumnsProvider implements BIAgent` (`.../BColumnsProvider.java`, `BIAgent`): override
`getDefaultColumns(BQuery)`, `makeColumn(TypeSpecPath, BQuery)`, `getDefaultSortColumnIndex`. `QueryUtil` turns a
`BQuery` into the table (rows = the result cursor, columns = `BProjCol` keyed by `TypeSpecPath`). **Author move**:
expose a `@NiagaraProperty` `BQuery`; for custom columns, register a `BColumnsProvider` subclass as a `BIAgent`.

## 782.4 — SEARCH: `BSearchService` + the `BISearchProvider` (BIAgent) SPI `[CERT]`
`BSearchService` (a `BAbstractService`, `search-rt/.../BSearchService.java:115`; `defaultScheme="neql"`,
`maxResultsPerSearch=500`) orchestrates searches; contributors implement `BISearchProvider extends BIAgent`
(`.../BISearchProvider.java:27-28`) — one method `search(BOrd query, BIObject scope, Context) → Stream<Entity>`.
**Discovery is the decisive part**: `findSearchProvider` intersects the agents on the SCOPE with the agents on the
ORD-SCHEME, both filtered `AgentFilter.is(BISearchProvider.TYPE)` — so a provider is registered by annotating it
`@NiagaraType(agent={@AgentOn(types={"<scope-type>","<ordScheme-type>"})})`. Exemplar `BBqlSearchProvider implements
BISearchProvider` with `@AgentOn(types={"baja:ComponentSpace","baja:Component","bql:BqlScheme"})` — its `search()`
resolves the ORD to a `BITable` and streams `cursor().stream()` into `BSearchResult`s. **Author move**: subclass
`BISearchProvider`, `@AgentOn` the scope×scheme types, return `BSearchResult`s; invoke via
`BSearchService.getService().search(params)`.

## 782.5 — INDEX: `BSystemIndexService` + `BIIndexQueryProvider` / `BSystemIndexer` `[CERT]`
`BSystemIndexService` (a `BAbstractService`, `systemIndex-rt/.../BSystemIndexService.java:103`) drives indexing. A
thing becomes indexable by implementing `BIIndexQueryProvider` — `BOrdList getOperationalIndexQueries()`
(`.../BIIndexQueryProvider.java:30`): **the index scope is declared as a set of NEQL/BQL ORD queries**. The author
base is `abstract class BSystemIndexer implements BIIndexQueryProvider` (`.../BSystemIndexer.java:136`) — override
`protected abstract void executeFullIndex(SystemIndexLog, Context)` (:244); slots merge `defaultIndexQueries` +
`customIndexQueries` (a `BOrdList`). Attach it under a `BSystemIndexSource`, or add a `…SystemIndexDeviceExt` on a
driver device — `niagaraSystemIndex-rt` is the concrete network exemplar (`BNiagaraNetworkSystemIndexer`,
`BNiagaraSystemIndexDeviceExt` — a DeviceExt opts a device into indexing). **Author move**: subclass `BSystemIndexer`
(or `BSystemIndexDescriptor` → `executeIndex`), implement the execute hook, supply the scope as a `BOrdList` of NEQL
queries. Index content is thus DECLARATIVE (NEQL) run by the same cursor engine as §782.2.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | The four surfaces share one pattern: BQuery/NEQL payload + a BIAgent provider (registry-discovered) → BITable | [CERT] | §782.2-782.5 spines below |
| 2 | `BQuery` is a fluent AST builder (select/from/where…); `BICompiledQuery.execute()` returns a `BITable` | [CERT] | BQuery.java:62,70; BICompiledQuery.java:26,28 |
| 3 | `BQueryTable` wraps a BQuery; `BColumnsProvider implements BIAgent` shapes columns | [CERT] | BQueryTable.java:27; BColumnsProvider (BIAgent) |
| 4 | Search: `BISearchProvider extends BIAgent` discovered by scope×ordScheme agent intersection (`@AgentOn`) | [CERT] | BISearchProvider.java:27-28; BSearchService.java:115; BBqlSearchProvider @AgentOn |
| 5 | Index: `BIIndexQueryProvider.getOperationalIndexQueries():BOrdList`; author subclasses `BSystemIndexer` + `executeFullIndex` | [CERT] | BIIndexQueryProvider.java:30; BSystemIndexer.java:136,244 |
| 6 | Index scope is declared as NEQL ORD queries (BOrdList), run by the same cursor engine as the query surface | [CERT/INFER] | BSystemIndexer customIndexQueries BOrdList; [INFER] shared cursor per B402-B413 |

**Tally**: 5 [CERT], 1 [CERT/INFER]. No unmarked claims. One spine per surface grep-verified inline this session at
`organized/`.

## Connections
- **B5/B758** (ORD/BQL cursor), **B402-B413** (BQL cursor / query execution) — the shared `BITable` cursor these
  surfaces bottom out in. **B778** (`@AgentOn`/agent-registry + BIAgent — the discovery mechanism all four providers
  use is the agent pattern; the `BQueryEngine`/`BISearchProvider` resolution is `Sys`-registry-agent lookup).
  **B780** (`@AgentOn` dual-surface — how each provider is registered).

## Open gaps
- **MAE11-G1** — the WB/UX config views (`BHxQueryTableView`, `BFxQuickSearch`, `search-ux`) are named but not walked;
  a bounded follow-up if a builder needs the search/table UI recipe (ties wb-ux-authoring B751-B753).

## Kit implication (→ `types/logic.md`)
Add ONE "query/search/index surface" recipe instead of four: **declare a typed `BQuery` (or a NEQL-ORD payload),
register the matching `BIAgent` provider for the surface you want — `BQueryEngine` (execute) / `BColumnsProvider`
(table columns) / `BISearchProvider` (station search, `@AgentOn` scope×scheme) / `BSystemIndexer` +
`BIIndexQueryProvider` (station index, scope = a `BOrdList` of NEQL queries) — and consume the resulting
`BITable` cursor.** All four are discovered by the agent registry (`@AgentOn` + `AgentFilter.is(TYPE)`), so the
uniform move is "declare the payload, plug the agent, read the table."
