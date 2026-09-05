# B773 · Authoring an analytics compute node + data source — the `BOutputBlock` SPI (MAE2, D7)

> **Scope**: the AUTHOR side of the analytics pipeline — how to write a NEW compute node (algorithm / filter /
> rollup / in-graph data source) and how to feed external series in. B66/B67 are read-only CATALOGS of the shipped
> blocks; this is the WRITE surface. Focus: `module-authoring-exemplars` (MAE2 / dimension D7). Kit destination:
> `types/logic.md`.
>
> **Sources**: FUENTE 3 decompiled `analytics-rt` — `javax.bajax.analytics.algorithm.{BAlgorithmBlock,BOutputBlock,
> BFunctionBlock,BBlockPin,BAlgorithm}` + `com.tridiumx.analytics.algorithm.{BRollupBlock,BDataSourceBlock,
> BDeadbandFilterBlock}` + `com.tridiumx.analytics.data.AnalyticDataSource`; verified this session at `organized/`.
> FUENTE 1: B66/B67 (analytics catalogs, REMITTANCE). READ-ONLY. English (post-B115).
>
> **Premise correction (seed candidate list):** `BAlgorithmBlock` is NOT the node base to override — it is the block
> ROOT; `BAnalyticAlgorithm`/`BINodeAlgorithm` DO NOT EXIST. The real compute base is `BOutputBlock`.

---

## 773.1 — The node base: `BOutputBlock` with abstract `getValue`/`getTrend` `[CERT]`
The compute contract lives on `public abstract class BOutputBlock` (`analytics-rt/…/javax/bajax/analytics/algorithm/
BOutputBlock.java:32`, `extends BAlgorithmBlock`): two abstract methods an author implements —
`abstract AnalyticTrend getTrend(AnalyticContext)` (:74) and `abstract AnalyticValue getValue(AnalyticContext)` (:77).
That IS the inputs→output contract: `getValue` = the scalar over the time range, `getTrend` = the streaming series.
`BAlgorithmBlock extends BComponent implements AlgorithmBlock` (`BAlgorithmBlock.java:40`) is the shared base (input
enumeration + edge resolution, §773.2); `BAlgorithm` is the CONTAINER (one algorithm = a wiresheet of blocks rooted
in a `BResultBlock`), not a node to subclass.

## 773.2 — Inputs/outputs → DAG edges: `BBlockPin` slots + `BLink` `[CERT]`
- **Inputs/outputs are `@NiagaraProperty` slots of type `BBlockPin`** (a status-bearing marker `BStruct`, not the data
  itself) — `in`/`out` pins.
- **Edges are standard `BLink`s between pins**: `BAlgorithmBlock.getInput(...)` resolves the upstream block live via
  `links[0].getSourceComponent()` (`BAlgorithmBlock.java:84`). A wire from an upstream `out` pin to this block's input
  pin IS the DAG edge.
- **The DAG is discovered by slot reflection**: input enumeration walks every property whose value `instanceof
  BBlockPin` and is not read-only (`BAlgorithmBlock.java:114`), rebuilt on knob/link changes. [INFER] evaluation is
  PULL-based (no scheduler): the sink (`BResultBlock`→`BAlgorithm.getValue/getTrend`) recursively calls each input's
  `getValue(cx)` up the DAG.

## 773.3 — The single-input convenience base: `BFunctionBlock.apply(...)` `[CERT]`
For a pointwise algorithm, `public abstract class BFunctionBlock extends BOutputBlock implements
Function<AnalyticValue,AnalyticValue>` (`BFunctionBlock.java:31`) — override just `AnalyticValue apply(AnalyticValue)`
(:51) and inherit `getValue`/`getTrend` for free (a `FunctionTrend` wraps the single `in` pin). This is the
low-effort author surface; use `BOutputBlock` directly only for multi-input nodes.

## 773.4 — Filters / rollups / in-graph data sources are all `BOutputBlock` subclasses; external feeds use a separate SPI `[CERT]`
There is NO separate `BFilter`/`BRollup`/`BIDataSource` base — every in-graph node kind is a `BOutputBlock` subclass:
`BRollupBlock extends BOutputBlock` (drains the input trend into a `Combiner`), `BDataSourceBlock extends BOutputBlock`
(resolves a nav-node series + applies rollup/aggregation/filter), and filters like `BDeadbandFilterBlock`/
`BRangeFilterBlock`/`BTimeFilterBlock`. The ONE genuinely separate SPI is the READ-side external feed:
`interface AnalyticDataSource` (`data/AnalyticDataSource.java:15`) with nested `interface Provider {
Collection<AnalyticDataSource> getDataSources(Id) }` (:22-23) — DUCK-TYPED on the nav node (`if (node instanceof
AnalyticDataSource.Provider)`), not registered.

## 773.5 — Registration: a plain `module.xml <type>`, no agent, no service `[CERT]`
An algorithm node is registered purely by its `@NiagaraType` → a `module.xml <type>` line — e.g.
`<type class="com.tridiumx.analytics.algorithm.BFunctionBlock" name="FunctionBlock"/>` (`analytics-rt/…/module.xml:58`;
DataSourceBlock :49, RollupBlock etc.). There is NO `@AgentOn` and NO per-block service (grep of analytics-rt module.xml
for `@AgentOn`/`<agent>` = 0). The lone `BAnalyticService` is the runtime request/context host, not a block registry.
Blocks surface in the wiresheet palette via `analytics-wb/module.palette` once built (slotomatic emits the `<type>`).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Node base = `BOutputBlock` (abstract `getValue`/`getTrend`); `BAlgorithmBlock` is the shared base, `BAlgorithm` the container; `BAnalyticAlgorithm` absent | [CERT] | BOutputBlock.java:32,74,77; BAlgorithmBlock.java:40 |
| 2 | Inputs/outputs = `BBlockPin` `@NiagaraProperty` slots; edges = `BLink` resolved via `getSourceComponent()` | [CERT] | BAlgorithmBlock.java:84,114 |
| 3 | `BFunctionBlock extends BOutputBlock`; override `apply(AnalyticValue)` for a pointwise node | [CERT] | BFunctionBlock.java:31,51 |
| 4 | Filters/rollups/in-graph sources are all `BOutputBlock` subclasses; external feed = `AnalyticDataSource.Provider` (duck-typed) | [CERT] | BRollupBlock/BDataSourceBlock (extends BOutputBlock); AnalyticDataSource.java:15,22-23 |
| 5 | Registration = `module.xml <type>` only; no `@AgentOn`, no per-block service | [CERT] | analytics-rt module.xml:49,58; @AgentOn count = 0 |

**Tally**: 5 [CERT], 0 [INFER on claims] (1 INFER note on pull-based evaluation). Spine grep-verified inline this
session at `organized/`.

## Connections
- **B66/B67** (analytics algorithm/filter CATALOGS — this block is their author-side complement). **B782** (query/
  search/index surface — analytics is a DAG-of-blocks variant of "declare a payload + read a result"; but note the
  CONTRAST: analytics registers purely by `<type>`, NOT by a `BIAgent` provider — a useful exception to B782's idiom).
  **B779** (BBlockPin inputs are dynamic-ish slots; DAG edges use the same `BLink`/`getSourceComponent` machinery).

## Open gaps
- **MAE2-G1** — the `Combiner`/`BCombination` rollup math and the `FunctionTrend` streaming wrapper are named but not
  walked; a bounded follow-up if a builder writes a custom streaming rollup.

## Kit implication (→ `types/logic.md`)
Add an "authoring an analytics node" recipe: a custom node is a `@NiagaraType` subclass of
`javax.bajax.analytics.algorithm.BOutputBlock` implementing `getValue(AnalyticContext)`/`getTrend(AnalyticContext)`
(or `BFunctionBlock.apply(AnalyticValue)` for a single-input pointwise node); inputs are `BBlockPin`
`@NiagaraProperty` slots wired by `BLink` edges (resolved via `getLinks().getSourceComponent()`), evaluated pull-based
up the DAG; registered by a plain `module.xml <type>` (NO `@AgentOn`, NO service). Filters/rollups/in-graph sources are
the SAME shape (a `BOutputBlock` subclass); external data feeds use the separate duck-typed `AnalyticDataSource(.Provider)`
interface. NOTE the contrast with the B782 query/search/index idiom: analytics registers by type, not by a `BIAgent`.
