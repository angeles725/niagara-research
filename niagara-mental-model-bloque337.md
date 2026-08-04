# Block 337 — The generation trigger: a COV event regenerates the whole schema SYNCHRONOUSLY on the engine thread, debounced by a single-shot `minWriteTime` timer, and writes the JSON string into the `output` slot

> Focus **jsonToolkit** — evidence block J3. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the OUTBOUND generation PIPELINE — what turns the static schema tree [Block 336] into an emitted JSON
> string: the `generateJson` action, the `cov` vs `onDemandOnly` update strategy, the COV subscription wiring +
> filters, and the `minWrite`/`maxWrite` coalescing. Stops at `setOutput()` (the `output` slot); how the JSON
> then LEAVES the station is J5.
>
> Sources (primary, decompiled N4.14.0.162 + docs), sweep sonnet + driver re-verification of the load-bearing
> claims: `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/outbound/schema/`
> (`BJsonSchema.java`, `subscription/*`, `config/BJsonSchemaUpdateStrategy.java`,
> `config/BJsonSchemaTuningPolicy.java`) and `docJsonToolkit` (`SchemaTuningPolicy-json.html`,
> `SubscriptionExamples-Json-71502433.html`, `jsonToolkit-SubscriptionSlotBlacklist.html`).
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[CERT-doc]` official doc · `[INFER]` deduction.
> Block TYPE: **evidence**.

---

## 337.1 — `generateJson`: license → queries → walk → `output`

The `generateJson` action (`newAction(2064, null)`) `[CERT]` (`BJsonSchema.java:126`) routes to
`generateAndOutputJson(context)`, whose body is the whole outbound act `[CERT]` (`:467-494`):

1. `LicenseLimit.checkExportLicensed()` — the per-op export gate [Block 335] §335.2 (an unlicensed or
   SMA-expired station throws HERE, at generation time) `[CERT]` (`:469`).
2. `processQueries(context)` — runs the schema's queries synchronously (J4) `[CERT]` (`:473`).
3. `JSONStringer json = new JSONStringer()` → `this.process(json, false)` — the top-down tree walk [Block 336]
   §336.2 `[CERT]` (`:474`, `:477`).
4. `setOutput(json.toString())` → `setLastUpdated(now())` `[CERT]` (`:490-492`).

So the PRODUCT is the `output` String slot [Block 336] §336.1; generation is "walk the tree into a `JSONStringer`,
store the string." On a generation exception it appends the partial `json.toString()` to the message and resets
the stringer `[CERT]` (`:483-489`) — partial output is not published `[INFER]`.

## 337.2 — Update strategy: `cov` (default) vs `onDemandOnly`

`BJsonSchemaUpdateStrategy` has exactly two values: `cov` (ordinal 0, **DEFAULT**) and `onDemandOnly` (1) `[CERT]`
(`BJsonSchemaUpdateStrategy.java:12`, `:17-19`). The gate is `requiresMemberSubscriptions()` =
`updateStrategy == cov && !isRelative()` `[CERT]` (`BJsonSchema.java:407-409`):
- **cov** → subscriptions are created (schema fires `BSchemaEvent.subscriptionsEnabled`, each bound member calls
  `startSubscriptions()`), and COV events auto-trigger regeneration `[CERT]` (§337.3). A query-execution timer
  also runs `[CERT]` (`:360-369`).
- **onDemandOnly** → no subscriptions; regeneration happens only on the explicit action or the query-interval
  timer `[CERT]` (`:379-384`). Changing `updateStrategy` at runtime wires/unwires subscriptions live via
  `changed()` `[CERT]` (`:274-276`).

## 337.3 — The trigger runs SYNCHRONOUSLY on the engine thread (the load-bearing finding)

A subscribed slot change lands in `SchemaBoundMemberSubscriber` (Niagara's `Subscriber` COV API, event mask
`{0,1,2,3,4,13,20}`) `[CERT]` (`SchemaBoundMemberSubscriber.java:25`, `:39-49`), passes the filter chain
(§337.5), and calls `handleSubscriptionEvent`, which — for a normal change — does `[CERT]`
(`BJsonSchemaBoundMember.java:303-309`):

```java
this.updateCacheWithLiveValue(subscription, event);
this.getSchema().requestGenerateJson(null);   // DIRECT call — no enqueue
```

This is a **direct synchronous call on the Niagara engine thread** — there is no work queue between the COV event
and the regeneration `[CERT]`. So the ENTIRE schema (all bound resolutions, all queries §337.1) re-runs inline on
the engine thread each time a subscribed value changes (subject to the §337.4 debounce) `[INFER]`. A large schema
or a slow query [Block J4] therefore blocks the engine thread — the concrete engine-thread risk of this module
`[INFER]`. (Events 13/20 = unsubscribe/removed-from-tree just unsubscribe `[CERT]` `:305`.)

## 337.4 — Coalescing: a single-shot debounce, not a queue

`requestGenerateJson` gates on `minWriteTime` `[CERT]` (`BJsonSchema.java:411-429`):
- `minWrite == DEFAULT` (unset) OR `maxWriteTimeExceeded()` → `generateAndOutputJson` immediately.
- else if the `minWriteTicket` is not yet expired → set `requestDuringMinWritePeriod.set(true)` and RETURN
  (deferred) `[CERT]` (`:422`).
- else generate, then `updateMinWriteTimer()` arms a one-shot `Clock.schedule(this, minWrite, minWriteExpired)`
  `[CERT]` (`:398-405`).

When the timer fires, `minWriteExpired` regenerates ONCE iff `requestDuringMinWritePeriod` is set `[CERT]`
(`:457-465`). So **any number of COV events inside the window collapse to at most ONE deferred regeneration** —
the coalescer is a single `AtomicBoolean`, NOT a growable queue `[CERT]`. This bounds memory (no backlog) but
means intermediate values are skipped `[INFER]`. `maxWriteTimeExceeded()` forces a write through the window if
`lastUpdated` is older than `maxWrite` `[CERT]` (`:440-443`); `writeOnStart` fires one generation at
`atSteadyState()` `[CERT]` (`:243-247`); and `forceGenerateJson` bypasses ALL of this `[CERT]` (`:205`, `:453`).

## 337.5 — The five filters: which slot changes actually fire

`Subscription.test()` runs the filters in insertion order, first non-CONTINUE wins (PROPAGATE→generate,
IGNORE→skip) `[CERT]` (`Subscription.java:57-68`):

| Filter | Decides | Cite |
|---|---|---|
| `BSubscriptionSlotBlacklist` (the service's `globalCovSlotFilter`) | IGNORE if slot ∈ blacklist (default `wsAnnotation`, `jsonExportMarker`, `exportMarker`) | `BSubscriptionSlotBlacklist.java:38-40` |
| `SlotWhiteListFilter` | CONTINUE only if the changed slot is in `getPropertiesToIncludeInJson()` [Block 336] §336.4 | `SlotWhiteListFilter.java:19-31` |
| `BindingSlotFilter` | CONTINUE only for the exact bound slot | `BindingSlotFilter.java:16-18` |
| `ChildSlotRenamedFilter` | for RENAMED, PROPAGATE only if the node is a bound OBJECT (name→key) | `ChildSlotRenamedFilter.java:17-21` |
| `COMPONENT_CHANGED_FILTER` | PROPAGATE lifecycle 13/20, else CONTINUE | `SubscriptionFactory.java:27-29` |

This is where [Block 335] §335.5's `globalCovSlotFilter` finally acts: it suppresses regeneration on
UI-annotation / marker slot changes `[CERT]`, distinct from J2's generation-time slot SELECTION (§336.4) `[INFER]`.

## 337.6 — The official tuning model `[CERT-doc]`

> "There is a built-in **Min Write Time** to ensure that hundreds of concurrent CoV changes over a short time do
> not result in a deluge of JSON messages … schema generation defers … However, if this … exceeds the **Max
> Write Time** setting, the system forces schema generation." — `SchemaTuningPolicy-json.html` §"Tuning policy"
> `[CERT-doc]`.
> "**Update Strategy** determines when JSON string generation occurs: at change-of-value or on demand." — same
> doc `[CERT-doc]`. "A **Force Generate Json** action overrides all tuning policy settings." — same `[CERT-doc]`
> (confirms §337.4).
> "This filter denotes which slots to ignore when subscribed to bound values … changes to a component's
> `wsAnnotation` property … should generally be excluded." — `jsonToolkit-SubscriptionSlotBlacklist.html`
> §"Global Cov Slot Filter" `[CERT-doc]` (confirms §337.5).

## 337.7 — What this block does NOT resolve

- `processQueries` / `BJsonSchemaQueryFolder` — the query dialect + timeout on the engine thread → **J4**.
- Where `output` GOES (HTTP servlet / file / fox push) → **J5**.
- Whether high-COV load can still overwhelm the engine thread despite the debounce → related to **J12**
  (engine-cycle queues) and the §337.3 risk.

## 337.8 — Connections

- [Block 336] §336.2 — the tree walk this trigger invokes; §336.4 slot selection vs §337.5 COV filtering.
- [Block 335] §335.2/§335.5 — the export license check (§337.1) and `globalCovSlotFilter` (§337.5) realized here.
- J4 (queries) / J5 (transport) — the two ends this block deliberately leaves open.

## 337.9 — Self-verify

Block TYPE: **evidence** (code + `[CERT-doc]`). Delegated sweep **sonnet**; driver re-verified verbatim the
load-bearing claims: the synchronous `requestGenerateJson(null)` call (`BJsonSchemaBoundMember.java:303-309`), the
`minWrite` debounce + `requestDuringMinWritePeriod` AtomicBoolean (`BJsonSchema.java:411-429`), the
`generateAndOutputJson` body (`:467-494`, checkExport→queries→JSONStringer→setOutput), and the update-strategy
enum (`BJsonSchemaUpdateStrategy.java:12,17-19`). `[CERT-doc]` quotes token-checked against the three doc files
(registered + preserved under `sources/manuals/jsonToolkit-docs/`).

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 28 |
| CERT-doc | 8 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 6 |
| INFER/CERT ratio | 0.17 |

`verify-block.sh` exit 0; `verify-sources.sh` no FABRICATED-CITE for B337.

Evidence block: `[INFER]`s are the engine-thread-blocking risk, the skipped-intermediate-values reading, and the
two-filters-two-stages distinction — each anchored to a cited `[CERT]`/`[CERT-doc]`.
