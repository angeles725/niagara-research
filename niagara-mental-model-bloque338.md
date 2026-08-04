# Block 338 — Outbound queries: any `BITable`-returning ORD (BQL / NEQL / history / transform) run in parallel on a thread pool, the engine thread blocked up to a 30 s timeout, results cached in memory and rendered later

> Focus **jsonToolkit** — evidence block J4. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the OUTBOUND QUERY subsystem — `BJsonSchemaQuery` (the query definition), `QueryRunner` (async execution
> + the engine-thread timeout guard), `BJsonSchemaBoundQueryResult` (rows→JSON), `QueryResultHolder`,
> `QueryFailReasons`. This is what feeds array/object content into a schema [Block 336]/[Block 337] §337.1
> (`processQueries`). Query STYLE/formatters are J8; relative/cross-station history is J9.
>
> Sources (primary, decompiled N4.14.0.162 + docs), sweep sonnet + driver re-verification of the load-bearing
> claims: `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/outbound/schema/query/`
> (`QueryRunner.java`, `BJsonSchemaQuery.java`, `BJsonSchemaQueryFolder.java`,
> `BJsonSchemaBoundQueryResult.java`, `QueryResultHolder.java`, `QueryFailReasons.java`) and `docJsonToolkit`
> (`Queries-Json-9A07510A.html`, `jsonToolkit-JsonSchemaQuery.html`, `SettingUpQueriesJson-9A09D8D2.html`).
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[CERT-doc]` official doc · `[INFER]` deduction.
> Block TYPE: **evidence**.

---

## 338.1 — A query is any ORD that resolves to a `BITable`

`BJsonSchemaQuery` holds a `queryOrd` (`BOrd`, field-editor facet `targetType = "bql:BqlScheme"`) `[CERT]`
(`BJsonSchemaQuery.java:34`) and `execute()` is `[CERT]` (`:75-81`):

```java
public BITable<?> execute(BComplex base, Context context) {
   if (!getQueryOrd().isNull() && !getQueryOrd().toString().isEmpty())
      return (BITable<?>) getQueryOrd().resolve(base, context).get();
   else throw new UnresolvedException("Query empty");
}
```

So the query is **language-agnostic at runtime**: the ORD is resolved against the schema base and whatever it
returns — as long as it is a `BITable` — is the result `[CERT]`. The facet nudges Workbench toward BQL, but BQL,
NEQL, a history ORD, or a transform ORD all work `[INFER]`. This matches the doc exactly: "A query can be any
valid **transform, neql or bql** statement, which returns a **BITable**" — `jsonToolkit-JsonSchemaQuery.html`
`[CERT-doc]`; "bql on the history space or neql on the component space … anything you can feed to the
ReportService" — `SettingUpQueriesJson-9A09D8D2.html` `[CERT-doc]`. There is no `resultLimit` property — the
cursor is fully drained (§338.4) `[CERT]`.

## 338.2 — `QueryRunner`: parallel on a thread pool, engine thread bounded by a 30 s timeout

Queries do NOT run inline. `QueryRunner` submits each to `ModuleThreadPool` and collects a `Future` `[CERT]`
(`QueryRunner.java:110`):

```java
Future<?> future = ModuleThreadPool.getInstance(query.getType()).submit(() -> executeQuery(query, base, context));
this.executingQueries.add(future);
```

So all a schema's queries run in PARALLEL on worker threads `[CERT]`. But `executeQueries()` — called on the
engine thread inside `generateAndOutputJson` [Block 337] §337.1 — then BLOCKS on those futures with a hard
deadline `[CERT]` (`QueryRunner.java:49-59`):

```java
BAbsTime timeout = now().add(schema.getQueries().getQueriesMaxExecutionTime());
while (!executingQueries.isEmpty()) {
   if (now().isAfter(timeout)) { executingQueries.forEach(f -> f.cancel(true)); break; }
   waitForQueriesToComplete(timeout.getMillis() - now().getMillis());   // future.get(ms, TimeUnit.MILLISECONDS)
}
```

`queriesMaxExecutionTime` is a **hidden** `BRelTime` property, default **30 seconds**, min 1 ms `[CERT]`
(`BJsonSchemaQueryFolder.java:48`). So the engine-thread block that [Block 337] §337.3 flagged is BOUNDED: a
COV-triggered regenerate can stall the engine thread up to 30 s on slow queries, then the futures are cancelled
and the cycle aborts `[INFER]` — protection, but 30 s is a long block for a real-time engine thread `[INFER]`.

## 338.3 — Three failure modes

`QueryFailReasons` has exactly three values `[CERT]` (`QueryFailReasons.java:4-6`):

| Reason | Trigger | Effect |
|---|---|---|
| **TIMEOUT** | `future.get()` throws `TimeoutException` past the deadline | metric + **`throw QueryFailException`** — hard, aborts the whole regeneration `[CERT]` (`:90-92`) |
| **OVERLAP** | `executeQueries()` called while a prior batch's futures are still pending | metric + **`return`** (skip this cycle, no exception) `[CERT]` (`:37-43`) |
| **RUNTIME** | worker throws `ExecutionException`/`InterruptedException` (e.g. `UnresolvedException` from a bad ORD) | logged SEVERE, message recorded; **no exception** — partial results survive `[CERT]` (`:75-89`) |

The OVERLAP guard is a concurrency protection: if the previous query cycle has not finished, the new one is
dropped rather than run concurrently `[CERT]` — so query cycles never stack `[INFER]`. RUNTIME is a SOFT failure:
one bad query does not kill the others' output `[INFER]`.

## 338.4 — Results are cached in memory, rendered later

Execution and rendering are DECOUPLED. `QueryResultHolder` eagerly drains the resolved `BITable` cursor into a
`List<Map<String, BIObject>>` at cache time — ALL rows in memory `[CERT]` (`QueryResultHolder.java:28-59`). Then,
at JSON-render time, `BJsonSchemaBoundQueryResult.process()` reads the CACHED holder (it does NOT re-run the
query) and hands it to the configured `BQueryResultWriter` style `[CERT]` (`BJsonSchemaBoundQueryResult.java:120-129`):

```java
QueryResultHolder result = query.getLastResult();               // cached
BQueryResultWriter writer = (BQueryResultWriter) getOutputStyle().getInstance();  // J8
writer.appendJson(json, result);                                 // one JSON object per row
```

If a query has no cached result yet, a WARNING is logged and nothing is written `[CERT]`. So in `cov` mode queries
run on subscription/refresh, cache, and the frequent JSON regenerations just re-serialize the cache — the doc
confirms: "Queries do not execute each time a schema generates in change-of-value mode … a BoundQueryResult
caches the results" — `Queries-Json-9A07510A.html` `[CERT-doc]`. Consequence: a huge query result is fully
materialized in memory for the life of the cache `[INFER]` (a memory concern, related to J12).

## 338.5 — The official query model `[CERT-doc]`

> "If multiple queries exist, the station **runs each query in parallel** each time the schema executes." —
> `Queries-Json-9A07510A.html` `[CERT-doc]` (confirms §338.2 parallel dispatch).
> "You may use the hidden query folder property `queriesMaxExecutionTime` to increase the time granted to
> complete all the queries during each cycle. **Failure to complete in this time causes schema generation to
> fail.**" — same doc `[CERT-doc]` (confirms §338.2/§338.3 TIMEOUT).

## 338.6 — What this block does NOT resolve

- The `BQueryResultWriter` STYLES (objects-array / row-array / key-value) that turn cached rows into a specific
  JSON shape → **J8**.
- `BRelativeHistoryQuery` cross-station/history aggregation (it extends `BJsonSchemaQuery`, substituting a
  `%baseHistoryOrd%` token, then the same `resolve→BITable` path) → **J9**.
- The `ModuleThreadPool` sizing / saturation → **J12**.

## 338.7 — Connections

- [Block 337] §337.1/§337.3 — `processQueries` runs here; this block BOUNDS the engine-thread block it flagged to
  `queriesMaxExecutionTime` (30 s default).
- [Block 336] §336.2 — `BJsonSchemaBoundQueryResult` is a bound member that renders into the tree walk.
- [Block 335] — the schema base (`Sys.getStation()`) the query ORD resolves against.
- J8 (styles) / J9 (relative history) / J12 (thread pool) — the three ends left open.

## 338.8 — Self-verify

Block TYPE: **evidence** (code + `[CERT-doc]`). Delegated sweep **sonnet**; driver re-verified verbatim: the
timeout loop + `future.cancel(true)` (`QueryRunner.java:49-59`), the async `ModuleThreadPool.submit` (`:110`), the
`TimeoutException`→`QueryFailException` (`:90-92`), the OVERLAP guard (`:37-43`), `execute()`=`queryOrd.resolve().get()`
(`BJsonSchemaQuery.java:75-77`), `queriesMaxExecutionTime` 30 s hidden (`BJsonSchemaQueryFolder.java:48`), and the
three `QueryFailReasons` (`:4-6`). `[CERT-doc]` quotes token-checked; docs registered + preserved under
`sources/manuals/jsonToolkit-docs/`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 17 |
| CERT-doc | 9 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 7 |
| INFER/CERT ratio | 0.27 |

`verify-block.sh` exit 0; `verify-sources.sh` no FABRICATED-CITE for B338.

Evidence block: `[INFER]`s are the 30 s-engine-block reading, the memory-materialization concern, and the
soft-vs-hard failure distinction — each anchored to a cited `[CERT]`/`[CERT-doc]`.
