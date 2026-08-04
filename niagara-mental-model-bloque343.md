# Block 343 — The relative schema: same tree, but the fixed station base is swapped for a stream of query rows run one-at-a-time on a 24 h periodic clock — cross-station aggregation via `station:`/`sys:` ORDs, no explicit Fox

> Focus **jsonToolkit** — evidence block J9. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: `BRelativeJsonSchema` and its `relative/` package — the supervisor-style variant that generates one JSON
> per row of a BASE QUERY (e.g. all BACnet points, or subordinate-station points) instead of one JSON for the
> station. Extends the model [Block 336] J2; its trigger differs from the normal cov/onDemand path [Block 337] J3.
>
> Sources (primary, decompiled N4.14.0.162 + docs), sweep sonnet + driver re-verification (vineflower obfuscated
> the SubscriptionTable class + the base-query property to `n` — reported by role):
> `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/outbound/schema/relative/`
> (`BRelativeJsonSchema.java`, `BBaseQuery.java`, `SubscriptionTable.java`, `BBaseAndOutputPair.java`) and
> `docJsonToolkit` (`RelativeSchemaConstruction-94DD3616.html`, `Supervisors-Json-6DC9E60A.html`,
> `jsonToolkit-RelativeJsonSchema.html`).
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[CERT-doc]` official doc · `[INFER]` deduction.
> Block TYPE: **evidence**.

---

## 343.1 — A relative schema is a `BJsonSchema` with a swapped base

`public class BRelativeJsonSchema extends BJsonSchema` `[CERT]` (`BRelativeJsonSchema.java:64`). The ONE structural
change is the base: it overrides `getBaseObject()` to return `currentBaseItem` (a `BComplex`), NOT
`Sys.getStation()` [Block 336] §336.1 `[CERT]` (`:256-257`). `currentBaseItem` is dequeued from a
`baseItemQueue` (`baseItemQueue.take()`) `[CERT]` (`:276`). So the SAME member tree [Block 336] §336.2 runs
repeatedly, each time rooted at a different base — one JSON generation per base `[INFER]`.

## 343.2 — The base query: a BQL/NEQL ORD → a table of bases, iterated one at a time

The base is chosen by a `baseQuery` `BOrd` (field-editor facet `bql:BqlScheme`, hidden flags 8) `[CERT]`
(`BBaseQuery.java:70`). `resolveBaseQuery()` does `baseQuery.get(Sys.getStation())` and requires a
`BITable<BComplex>` `[CERT]` (`:165-174`) — the same "anything returning a BITable" contract as a normal query
[Block 338] §338.1, but the rows are the BASE COMPONENTS. `enqueueBaseItems()` walks the table cursor and enqueues
each row `[CERT]` (`:188`), and `processBase()` takes one at a time, sets `currentBaseItem`, and runs
`generateAndOutputJson` — sequential, one full tree walk per base `[CERT]` (`:273-289`). Doc: "A base query feeds
base components to the schema, which the query resolves against the schema one at a time … for example, all
BACnet points in a station" — `RelativeSchemaConstruction-94DD3616.html` `[CERT-doc]`.

## 343.3 — The trigger is PERIODIC, not member-COV

Where a normal schema subscribes members and debounces on COV [Block 337], the relative schema runs on a clock:
`Clock.schedulePeriodically(this, getBaseQuery().getPublishInterval(), generateJson)` `[CERT]`
(`BRelativeJsonSchema.java:181`). `publishInterval` defaults to **24 hours** `[CERT]` (`BBaseQuery.java:71`) — so a
supervisor schema republishes everything daily by default `[INFER]`. `isRelative()` returns `true` `[CERT]`,
which trips the `!isRelative()` gate in `requiresMemberSubscriptions()` [Block 337] §337.2 — a relative schema
does NOT create per-member COV subscriptions during the tree walk `[CERT]`. Doc: "The base query's Publish
Interval causes the base query to be re-executed periodically and triggers a complete publish output … at the
interval selected." — `RelativeSchemaConstruction-94DD3616.html` `[CERT-doc]`.

## 343.4 — `SubscriptionTable`: COV fanout across bases

COV IS still available, but through a different mechanism — the `SubscriptionTable` (`n`) `[CERT]`
(`SubscriptionTable.java:24-25`): `baseMap` maps a subscribed slot path → the SET of `BComplex` bases that
referenced it, and `subscribers` holds one `SchemaBoundMemberSubscriber` per unique slot. `register()` is called
from `getOrdTarget()` per resolved member, associating the slot with the current base `[CERT]` (`:33-46`,
`BRelativeJsonSchema.java:218`). On a COV event it looks up which bases depend on the changed slot and re-enqueues
each affected base `[CERT]` (`SubscriptionTable.java:54-79`). So it is a **dedup + fanout table**: one physical
subscription per slot even when many bases share it, re-generating only the bases that reference a change `[INFER]`.

## 343.5 — Cross-station: `station:`/`sys:` ORDs, resolved over Fox transparently — no explicit Fox

There is **no explicit Fox API** in the relative package — grep for `fox:`/`FoxSession`/`BFox`/fox imports across
all four files returned zero `[CERT]`. Cross-station reach is IMPLICIT: the base query is resolved with
`get(Sys.getStation())` `[CERT]` (`BBaseQuery.java:172`), and a `station:`/`sys:` ORD scheme is routed to the
remote subordinate by Niagara's standard ORD framework (which uses Fox under the hood) `[INFER]` (cross-corpus:
Fox/niagara-network). Doc: "Use the system database to index subordinate controllers and `sys:` ords for queries
within a Supervisor schema … Example base query: `station:|sys:|neql:n:point|bql:select *` … Subscription to the
remote points works so that change of value is available." — `Supervisors-Json-6DC9E60A.html` `[CERT-doc]`. So a
supervisor aggregates its subordinates' points into one JSON by pointing the base query at the system database
`[INFER]`.

## 343.6 — What this block does NOT resolve

- `BRelativeHistoryQuery` (`%baseHistoryOrd%` token substitution) — the history variant, mechanics in
  [Block 338] J4; its relative binding is the same base-swap as here.
- The `sys:`/system-database internals + the Fox channel security — corpus `niagara-network-supervisor` focus
  (planned), not re-opened here.
- `BBaseAndOutputPair` / the `currentBaseAndOutput` topic — a per-base output pairing consumed by relative-topic
  builders (a UI/consumer concern) `[INFER]`.

## 343.7 — Connections

- [Block 336] §336.1 — the fixed `Sys.getStation()` base this block swaps for `currentBaseItem`.
- [Block 337] §337.2 — the `!isRelative()` gate that routes relative schemas off the COV path onto the periodic
  clock + SubscriptionTable.
- [Block 338] §338.1 — the BITable query contract the base query reuses.
- `niagara-network-supervisor` focus (planned) — the supervisor↔subordinate Fox join this block consumes via
  `sys:` ORDs.

## 343.8 — Self-verify

Block TYPE: **evidence** (code + `[CERT-doc]`). Delegated sweep **sonnet**; driver re-verified verbatim: `extends
BJsonSchema` (`BRelativeJsonSchema.java:64`), `getBaseObject()`→`currentBaseItem` (`:256-257`),
`baseItemQueue.take()` (`:276`), `publishInterval`=24 h (`BBaseQuery.java:71`), `Clock.schedulePeriodically`
(`BRelativeJsonSchema.java:181`), `baseQuery` BOrd+BqlScheme (`BBaseQuery.java:70`), and grep-confirmed ZERO
explicit Fox references across the package. `[CERT-doc]` token-checked; docs registered + preserved under
`sources/manuals/jsonToolkit-docs/`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 17 |
| CERT-doc | 6 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 7 |
| INFER/CERT ratio | 0.30 |

`verify-block.sh` exit 0; `verify-sources.sh` no FABRICATED-CITE for B343.

Evidence block: `[INFER]`s are the one-JSON-per-base reading, the daily-republish default consequence, and the
Fox-implicit-via-ORD deduction — each anchored to a cited `[CERT]`/`[CERT-doc]`.
