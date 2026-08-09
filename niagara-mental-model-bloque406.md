# Block 406 — BQL Execution Path: BLocalBqlResolver, SelectQuery Pipeline, BogCursor DFS Walk, TOP N, ORDER BY, and the Absence of SKIP M

> **Research focus:** `database` (gap **DB5**, medium-priority). Covers the complete
> BQL query execution path from `BLocalBqlResolver.resolve()` through `SelectQuery.resolve()`
> six-stage pipeline, `BBqlExtent` / `BBogCollection`, `BogCursor` DFS walk of the component
> space, `BTopTable` (TOP N), `Ordering` (ORDER BY full-materialization), and a
> code-verified absence of SKIP M / OFFSET in the grammar and executor.
>
> **Not covered here:**
> - BQL/NEQL grammar, tokenizer, parser, AST construction → [Block 21]
> - BQL usage in the report module (ordInSession wall) → [Block 338], [Block 358], [Block 360]
> - `BFilteredTable`, `BAggregateTable`, `BDistinctTable` internals beyond what their
>   wrapper role requires in the pipeline
> - NEQL execution path (separate engine in `neql-rt`)
>
> Subject version: N4.14.0.162 (Vineflower decompiled corpus; organized/ tree).
>
> Sources:
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/BLocalBqlResolver.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/BIBqlResolver.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/SelectQuery.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/BBqlExtent.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/collection/BBogCollection.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/cursor/BogCursor.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/BTop.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/collection/BTopTable.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/Ordering.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/IndexUtil.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/RangeUtil.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/BSelect.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/compiler/Constants.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/compiler/SelectParser.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/fox/BFoxBqlResolver.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/bql/bql-rt/vineflower/com/tridium/bql/util/BqlUtil.java`
>
> Method: decompiled Java (Vineflower); all cited line ranges read inline by orchestrator.
> Block 21 (`[CERT]` niagara-mental-model-bloque21.md) remitted for grammar / AST level.
>
> `database` focus. Connects [Block 21] (BQL grammar and AST; `BogCursor` summary),
> [Block 338] / [Block 358] / [Block 360] (BQL in the report module),
> [Block 5] (component space / BOG format).

---

## 406.1 — Resolver Architecture: Two Implementations of BIBqlResolver `[CERT]`

`BIBqlResolver extends BInterface, BIAgent` — a typed Niagara interface that the agent dispatch
mechanism routes to based on the session type.

`[CERT]` `BIBqlResolver.java:13-17` (interface declaration, `resolve(BISession, OrdTarget, BqlQuery)` signature)

| Resolver | Session type | What it does |
|---|---|---|
| `BLocalBqlResolver` | `baja:LocalHost` | Delegates directly to `query.resolve(base)` — no preprocessing, no index lookup |
| `BFoxBqlResolver` | `fox:FoxSession` | Proxies the ORD through Fox's data channel; execution happens on the remote station |

**`BLocalBqlResolver` is a thin delegation layer and nothing more.**

```java
// BLocalBqlResolver.java:31-33
public OrdTarget resolve(BISession session, OrdTarget base, BqlQuery query) {
    return query.resolve(base);
}
```

`[CERT]` `BLocalBqlResolver.java:31-33`

It is a `@NiagaraSingleton` (`INSTANCE = new BLocalBqlResolver()`) registered as agent for `baja:LocalHost`.
`[CERT]` `BLocalBqlResolver.java:13-20`

**`BFoxBqlResolver`** walks the `OrdTarget` chain looking for a `RemoteQueryable` or `BSpace`
implementing `RemoteQueryable`; if found, calls `bqlQuery(remoteOrd, base)` on it. Otherwise,
delegates to `BFoxQueryHandler.getLookAheadOrd()` and resolves via the Fox data channel.
`[CERT]` `BFoxBqlResolver.java:40-84`

---

## 406.2 — SelectQuery.resolve(): The Six-Stage Execution Pipeline `[CERT]`

All BQL SELECT execution lives in `SelectQuery.resolve(OrdTarget base)`. The stages execute in
strict order; each stage wraps the prior result in a new table decorator:

```
Stage 1: Extent acquisition
    ↓  BITable<? extends BIObject>
Stage 2: Predicate filter   (if WHERE present)
    ↓  BFilteredTable
Stage 3: Aggregates          (if aggregate functions present)
    ↓  BAggregateTable       (forces full scan — sets skipQuantifier=true)
Stage 4: DISTINCT            (if DISTINCT and no aggregates)
    ↓  BDistinctTable
Stage 5: ORDER BY            (if ordering present)
    ↓  BObjectTable          (backed by sorted BListTable — FULL MATERIALIZATION)
Stage 6: TOP N               (if TOP present)
    ↓  BTopTable             (lazy: stops after N rows from the sorted table)
Stage 7: Projection          (if SELECT columns present)
    ↓  BProjectionTable / AllProjection
```

`[CERT]` `SelectQuery.java:54-87` (complete `resolve()` body)

Key ordering consequences:

- **ORDER BY precedes TOP N**: all rows are sorted into memory before the TOP limit is applied.
  A query `SELECT TOP 5 … ORDER BY name` materializes the FULL result set, sorts it, then
  truncates at 5. There is no early-termination optimization.
- **Aggregates bypass DISTINCT**: when `skipQuantifier = true` (aggregates detected),
  `BDistinctTable` is not applied even if `DISTINCT` was in the query.
  `[CERT]` `SelectQuery.java:66-73`
- **Predicate is lazy**: `BFilteredTable` wraps `BooleanFilter.accept(o, cx)`, which evaluates
  the WHERE expression per-row on demand without any index lookup.
  `[CERT]` `SelectQuery.java:62-64`

The `extentTarget` (the base OrdTarget pointing to the unwrapped extent table) is captured
before any filter/ordering is applied and is used as the outer `OrdTarget` in the result.
`[CERT]` `SelectQuery.java:61`, `SelectQuery.java:87`

---

## 406.3 — Extent Acquisition: BBqlExtent → BBogCollection `[CERT]`

`BBqlExtent.getExtent(OrdTarget base, BqlQuery query)` selects the extent table:

| Condition on `base.get()` | Action |
|---|---|
| `BDual` (type in `id`) | Returns a single-element `BListTable` wrapping `new BDual()` (the "no FROM" case for scalar queries) |
| `BIRelational` | Calls `getRelation(id, base)` — used by history / alarm stores that expose named relations |
| `Queryable` (but not BIRelational) | Calls `bqlQuery(base, query)` on the target — the target decides how to iterate |
| `BComplex` (component space) | Creates `new BBogCollection(base, depth, returnTypes, stop)` — **the standard station tree walk** |
| Other | Throws `InvalidOrdBaseException` |

`[CERT]` `BBqlExtent.java:93-137` (`getExtent` full body)

For `SELECT … FROM control:ControlPoint` against a station ORD, the base is a `BComponent`
(which `is` `BComplex`), so the standard path is `BBogCollection`.

**Type resolution:** `id` is a comma-separated list of type specs (`"module:TypeName,…"`);
an empty `id` defaults to `BComponent.TYPE`. `BBqlExtent.toType(id)` calls
`BTypeSpec.make(moduleName, typeName).getResolvedType()`.
`[CERT]` `BBqlExtent.java:119-135`, `BBqlExtent.java:139-152`

**`getExtentFromBase(OrdTarget base)` — implicit extent from the base object:**
When the query has no explicit `FROM` clause or uses the base extent, `SelectQuery` calls
`getExtentFromBase()`:
1. If `target instanceof Queryable` → `bqlQuery(base, this)`
2. If `target instanceof BComplex` → `BBqlExtent.getExtent(base, this)`
3. If `target instanceof BITable` → return the table directly
4. Else → `InvalidOrdBaseException`

`[CERT]` `SelectQuery.java:90-104`

---

## 406.4 — BogCursor: Pure DFS Linear Walk — No Index `[CERT]`

`BBogCollection.cursor()` instantiates a `BogCursor` (wrapped in `AccessSlotCursor` when
a user session is present for permission checking).
`[CERT]` `BBogCollection.java:57-73`

**`BogCursor` is a depth-first, stack-based iterator over the live component tree. There is
no index structure — it enumerates properties sequentially.**

```java
// BogCursor.java:34-47  (constructor)
public BogCursor(BComplex root, int depth, Type[] returnTypes, boolean stop, Context context) {
    this.root = root;
    this.depth = depth;
    this.returnTypes = returnTypes != null && returnTypes.length != 0
        ? returnTypes : new Type[]{BComponent.TYPE};
    this.stop = stop;
    this.context = context;
    this.componentOnly = true;
    for (int i = 0; i < this.returnTypes.length; i++) {
        if (!this.returnTypes[i].is(BComponent.TYPE)) {
            this.componentOnly = false; break;
        }
    }
}
```

`[CERT]` `BogCursor.java:34-47`

**`nextImpl()` — the walk algorithm:**

```
State:
  current   = SlotCursor<Property> at the current node level
  nodeStack = Stack<SlotCursor>   for DFS backtracking
  currentDepth = integer

1. If depth == 0 → return false (depth limit reached)
2. If current == null (first call):
     current = root.asComplex().getProperties()
     advance: componentOnly ? current.nextComponent() : current.next()
3. Else:
     If (not stopped) AND (currentDepth+1 < depth) AND current node is BComponent/BComplex:
         newRoot = current.get()
         newRootProps = newRoot.getProperties()
         If newRootProps.next[Component]():
             push current onto nodeStack; current = newRootProps; currentDepth++; return true
     Else:
         advance current: nextComponent()/next()
         If current exhausted → pop from nodeStack; currentDepth--; try again
         If nodeStack empty and current exhausted → return false
```

`[CERT]` `BogCursor.java:112-162` (`nextImpl()` full body)

The `stop` flag: when the current node matches the return type AND `stop == true`, sets
`stopped = true`; on the next `nextImpl()` call, `stopped` prevents descending into that
node's children (`if (!this.stopped && …)`). This is the `STOP` keyword from BQL's `FROM`
clause.
`[CERT]` `BogCursor.java:101-105` (`stop` branch in `next(Type[])`), `BogCursor.java:126` (stop guard)

**Type filter:** `isMatch(BObject o, Type[] types)` — iterates `types[]` and returns `true`
if `o.getType().is(types[i])` for any element. This is the Niagara type-hierarchy check
(`is` = instance-of in the type registry).
`[CERT]` `BogCursor.java:164-171`

**Security exclusion:** `BqlUtil.excludeFromResults(o)` filters out `BPassword`,
`BPasswordHistory`, `BICredentials`, and `BAbstractAuthenticator` regardless of type filter.
`[CERT]` `BogCursor.java:100`, `BqlUtil.java:65-70`

**Answer to the gap question — index or linear walk?** There is no index. The walk calls
`root.asComplex().getProperties()` at each level and iterates every property slot in
order. For a large station, this is an O(N) scan of all components in the subtree bounded
by `depth`. The only optimization available is `depth` limiting and `stop` pruning.

---

## 406.5 — TOP N at the Cursor Level: BTopTable `[CERT]`

`BTop` holds a single `limit` property of type `long` (default 0).
`[CERT]` `BTop.java:16-17`

`BTopTable<T>.TopCursor.advanceCursor()`:

```java
// BTopTable.java:57-62
public boolean advanceCursor() {
    if (this.row < BTopTable.this.limit) {
        this.row++;
    }
    return this.row < BTopTable.this.limit && this.innerCursor.next();
}
```

`[CERT]` `BTopTable.java:57-62`

- `row` starts at -1 (`TopCursor` constructor sets `this.row = -1`); first call increments to 0.
- When `row >= limit`, the cursor stops advancing the inner cursor — lazy early termination.
- `[CERT]` `BTopTable.java:47-63`

**Critical interaction with ORDER BY:** `SelectQuery.resolve()` applies `Ordering.order()`
BEFORE wrapping in `BTopTable`. `Ordering.order()` fully materializes ALL rows into an
in-memory array and sorts them. Only after the complete sorted array is in a `BListTable`
is `BTopTable` applied. A query `SELECT TOP 5 … ORDER BY x` scans and materializes the
entire result set, then limits to 5 rows at read time.
`[CERT]` `SelectQuery.java:75-80` (ORDER BY then TOP in resolve() body)

Without ORDER BY, `BTopTable` wraps the `BFilteredTable` / `BBogCollection` directly.
In this case, the DFS walk IS short-circuited after N matches — `BogCursor.nextImpl()`
is not called beyond the Nth row because `advanceCursor()` stops calling `innerCursor.next()`.

---

## 406.6 — ORDER BY: Full In-Memory Materialization `[CERT]`

`Ordering.order(BITable table, Context cx)`:

1. Opens a `TableCursor` on the input table.
2. Materializes ALL rows into `Array<OrderByRecord>` (each record holds the `BObject` and
   pre-evaluated sort-key values).
3. Calls `SortUtil.sort(recordsx, recordsx, new OrderByComparator())`.
4. Builds a sorted `BObject[]` array and wraps it in `BListTable → BObjectTable`.
5. Returns the sorted `BObjectTable`.

`[CERT]` `Ordering.java:41-99`

**Sort key evaluation:** each `OrderByColumn.getColumnExpression()` is evaluated via
`ExprEngine.evaluate(expr, o, cx)` for every row during materialization. If the order-by
column is a positional reference (`BSimpleExpression` with an integer), it resolves to the
corresponding projection column expression.
`[CERT]` `Ordering.java:134-147` (`resolveColumns()`)

**ASC/DESC:** `OrderByComparator.compare()` uses `SortUtil.compare()` for natural ordering;
if `!fields[i].isAscending()`, the result is negated.
`[CERT]` `Ordering.java:153-167`

**Edge case — non-component BComplex:** if a row object `!o.isComponent() && o.isComplex()`
and has no parent component, `newCopy(true)` is called to detach it before storing. This
prevents the sorted list from holding a reference to a transient component with an
unstable parent.
`[CERT]` `Ordering.java:53-56`

**Conclusion:** ORDER BY in BQL is never streamed. It always reads and holds the entire
result set in JVM heap before returning any row to the caller.

---

## 406.7 — SKIP M / OFFSET: Proven Absent from Grammar and Executor `[CERT]`

Three independent proofs confirm that SKIP M (pagination offset) does not exist in BQL:

**Proof 1 — Token list.** `Constants.java` defines 42 integer token constants and their
string labels. The complete token list contains: SELECT, FROM, WHERE, ORDER, BY, ASC, DESC,
HAVING, TOP, DISTINCT, ALL, DEPTH, STOP, AS — and NO `SKIP`, `OFFSET`, or equivalent.
`[CERT]` `Constants.java:6-91` (full token constant list)

**Proof 2 — Parser.** `SelectParser.parse()` calls `top()`, `quantifier()`, `projection()`,
`extent()`, `predicate()`, `having()`, `ordering()` — no `skip()` call exists.
`SelectParser.top()` reads only `TOP <NUMBER>` (token type 42).
`[CERT]` `SelectParser.java:36-54` (`parse()` and `top()`)

**Proof 3 — `BTop` data model.** `BTop` has exactly one property: `limit` (a `long`).
No `skip`, `offset`, or `start` field exists.
`[CERT]` `BTop.java:11-42`

**Conclusion:** There is no SKIP M / OFFSET / pagination facility in BQL at any layer —
grammar, AST, or executor. Pagination must be implemented by the caller (by materializing
results and slicing externally, or by using a different mechanism).

---

## 406.8 — IndexUtil and RangeUtil: Expression Field Extractors, Not a Data Index `[CERT]`

The name `IndexUtil` is misleading. It does NOT maintain an in-memory index of component
property values.

**`IndexUtil.indexes(BExpression expr)`** extracts all `BPath` field references from a
predicate expression by walking the AST (`BBinaryExpression` → lhs/rhs, `BUnaryExpression`
→ operand, `BPath` → add to `HashSet`). Returns the set of field path strings referenced
in the expression.
`[CERT]` `IndexUtil.java:13-31`

**`SelectQuery.getRange(String index, Type requiredType, boolean equalityOnly)`:**
Calls `RangeUtil(predicate, engine, index, requiredType).solveRange(equalityOnly)` to
derive a `RangeSet` — an interval set over a specific field. For example, a predicate
`x >= 10 AND x < 20` yields `RangeSet([10, 20))` for field `"x"`. Falls back to
`Range.ALL` when the expression cannot be solved (LIKE, complex logic).
`[CERT]` `SelectQuery.java:39-43`, `RangeUtil.java:35-46`

**Who uses `getRange()`?** Any class implementing `Queryable.bqlQuery(OrdTarget, BqlQuery)`
can call `((SelectQuery)query).getRange("fieldName", fieldType, false)` to obtain a range
hint and apply its own index. History providers and table-backed stores use this to avoid
full scans against their storage. `BBogCollection` / `BogCursor` do NOT call `getRange()` —
they perform the full DFS walk and rely on `BFilteredTable` to apply the WHERE predicate
lazily.
`[INFER]` (no direct `getRange()` call in BBogCollection confirmed by reading source; the
interface is defined on `SelectQuery` for external implementors of `Queryable`)

**Summary:** The component space (station tree) has no query index. `IndexUtil` and
`RangeUtil` exist to provide range hints to storage backends that already maintain their
own indexes (e.g., a time-series store indexed by timestamp).

---

## 406.x — Self-Verify

| Claim | Marker | Citation |
|---|---|---|
| `BIBqlResolver.resolve(BISession, OrdTarget, BqlQuery)` is the interface contract | `[CERT]` | `BIBqlResolver.java:13-17` |
| `BLocalBqlResolver` is `@NiagaraSingleton`, agent for `baja:LocalHost` | `[CERT]` | `BLocalBqlResolver.java:13-20` |
| `BLocalBqlResolver.resolve()` = `query.resolve(base)` — no other logic | `[CERT]` | `BLocalBqlResolver.java:31-33` |
| `BFoxBqlResolver` walks OrdTarget chain for `RemoteQueryable`, else uses Fox data channel | `[CERT]` | `BFoxBqlResolver.java:40-84` |
| `SelectQuery.resolve()` applies: extent → predicate → aggregate → distinct → order → top → projection | `[CERT]` | `SelectQuery.java:54-87` |
| `skipQuantifier = true` when aggregates detected → DISTINCT is skipped | `[CERT]` | `SelectQuery.java:66-73` |
| Predicate applied via `BFilteredTable(BooleanFilter)` — lazy, per-row | `[CERT]` | `SelectQuery.java:62-64` |
| ORDER BY before TOP N in resolve() | `[CERT]` | `SelectQuery.java:75-80` |
| `extentTarget` captured before filter/order/top wrapping | `[CERT]` | `SelectQuery.java:61`, `SelectQuery.java:87` |
| `BBqlExtent.getExtent()`: BDual / BIRelational / Queryable / BComplex → BBogCollection branches | `[CERT]` | `BBqlExtent.java:93-137` |
| Empty `id` → `returnTypes = new Type[]{BComponent.TYPE}` | `[CERT]` | `BBqlExtent.java:119-120` |
| `SelectQuery.getExtentFromBase()`: Queryable / BComplex / BITable branches | `[CERT]` | `SelectQuery.java:90-104` |
| `BBogCollection.cursor()` creates `BogCursor`; wraps in `AccessSlotCursor` when user present | `[CERT]` | `BBogCollection.java:57-73` |
| `BogCursor` constructor: sets `componentOnly=true` unless any returnType is not `is(BComponent)` | `[CERT]` | `BogCursor.java:34-47` |
| `BogCursor.nextImpl()`: first call → `root.asComplex().getProperties()`; descend via stack push | `[CERT]` | `BogCursor.java:112-162` |
| `stop` flag: prevents descent into a matched node on next `nextImpl()` call | `[CERT]` | `BogCursor.java:101-105`, `BogCursor.java:126` |
| `isMatch()`: `o.getType().is(types[i])` — type hierarchy check | `[CERT]` | `BogCursor.java:164-171` |
| `BqlUtil.excludeFromResults()` filters BPassword / BICredentials / BAbstractAuthenticator | `[CERT]` | `BogCursor.java:100`; `BqlUtil.java:65-70` |
| No index structure in BBogCollection or BogCursor — pure DFS | `[CERT]` | `BBogCollection.java:56-83` (no index field); `BogCursor.java:17-28` (no index field) |
| `BTop` has one property: `limit` (long, default 0) | `[CERT]` | `BTop.java:16-17` |
| `BTopTable.TopCursor.row` starts at -1; incremented before each check | `[CERT]` | `BTopTable.java:47-62` |
| `advanceCursor()`: returns false when `row >= limit` without advancing inner cursor | `[CERT]` | `BTopTable.java:57-62` |
| `Ordering.order()` materializes ALL rows into `Array<OrderByRecord>` before sorting | `[CERT]` | `Ordering.java:41-99` |
| `OrderByComparator.compare()`: `SortUtil.compare()`; negate for DESC | `[CERT]` | `Ordering.java:153-167` |
| Positional ORDER BY resolved via `resolveColumns()` → `getProjectionColumns()[projCol-1]` | `[CERT]` | `Ordering.java:134-147` |
| `Constants.java`: 42 tokens defined; no SKIP, OFFSET, or similar token | `[CERT]` | `Constants.java:6-91` |
| `SelectParser.parse()` calls: top, quantifier, projection, extent, predicate, having, ordering — no skip | `[CERT]` | `SelectParser.java:36-54` |
| `SelectParser.top()` reads only `TOP <NUMBER>` (token type 42) | `[CERT]` | `SelectParser.java:50-54` |
| `IndexUtil.indexes()`: extracts BPath field names from expression AST — not a data index | `[CERT]` | `IndexUtil.java:13-31` |
| `SelectQuery.getRange()` → `RangeUtil.solveRange()` returns Range.ALL on unsolvable predicate | `[CERT]` | `SelectQuery.java:39-43`; `RangeUtil.java:35-46` |

**Self-verify tally:** 30 claims — 29 `[CERT]`, 1 `[INFER]` (absence of getRange() call in
BBogCollection — confirmed by reading both files; no call site found).
Zero `[CERT-a]` assertions. Block Type: **standard**.

---

## 406.x — Connections

- **[Block 21]** — BQL grammar (tokenizer → parser → AST, `BSelect`, aggregates, TOP/SKIP
  at syntax level). Block 21 documents at summary level that `BogCursor` does "DFS sobre
  object graph" and that `BAggregateTable` / `BDistinctTable` / `BTopTable` are in-memory
  wrappers. B406 goes deep into the actual call chain, the stack-based DFS mechanics,
  the proven absence of SKIP M, and the order-before-top materialization consequence.
  B406 corrects nothing in B21 — the B21 summary is accurate; this block provides the
  execution-path detail B21 deliberately left at summary level.

- **[Block 338]** / **[Block 358]** / **[Block 360]** — BQL usage in the report module
  (ordInSession wall, `BReportQuery`, BQL result table). Those blocks cover how BQL
  queries are constructed and executed from the report engine. The executor path documented
  here (SelectQuery → BogCursor) is the same executor they reach; B338/358/360 cover
  the caller side, B406 covers the executor side.

- **[Block 5]** — BOG format and component space serialization. The component tree that
  `BogCursor` walks at runtime is the same tree that `ValueDocDecoder` loads from the BOG
  file at startup. B5 covers the load path; B406 covers how BQL queries traverse the loaded
  tree.

- **[Block 402]** — BOG save trigger and dirty-flag propagation. B402 covers how property
  writes propagate to trigger saves. B406 covers how BQL reads traverse that same component
  space without any index.
