# Block 346 — The engine-cycle queues bound the whole module: one item drained per engine cycle, capacity 1000, overflow REJECTS (throws) — plus `toJsonType`, where a null Baja value becomes `""`, never JSON null

> Focus **jsonToolkit** — evidence block J12. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the `util/` package — the `BEngineCycleQueue` family (the coalescing/backpressure that bounds the
> per-cycle work J3/J4/J6/J11 feed) and the `toJsonType` Baja→JSON primitive mapping, plus the minor utils.
> `LicenseLimit` is [Block 335] J1 — remitted, not re-covered.
>
> Sources (primary, decompiled N4.14.0.162), sweep sonnet + driver re-verification:
> `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/util/` (`BEngineCycleQueue.java`,
> `BEngineCycleMessageQueue.java`, `BEngineCycleAlarmQueue.java`, `BEngineCyclePairQueue.java`, + minor) and
> `.../outbound/schema/support/JsonSchemaUtil.java` (`toJsonType`); the wrapped platform class is
> `organized/baja/baja/vineflower/com/tridium/util/EngineCycleQueue.java`.
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[INFER]` deduction. Block TYPE: **evidence**.

---

## 346.1 — The engine-cycle model: one item per cycle advance

`BEngineCycleQueue<T> extends BComponent` wraps `com.tridium.util.EngineCycleQueue<T>` `[CERT]`
(`BEngineCycleQueue.java:3`, `:49`, `:60`), constructed as `new EngineCycleQueue(slotPath, this::consumeMessage,
rate, maxSize)` `[CERT]` (`:84`). The platform class holds a `LinkedBlockingQueue` drained by a dedicated
"emptier" thread that dequeues **one item only when the NRE engine-cycle counter has advanced**
(`currentCycle > lastCycle`) `[CERT]` (`EngineCycleQueue.java` emptier loop). So bursts of enqueues are COALESCED
to the engine's execution cadence — at most one item is dispatched per engine cycle, regardless of how fast they
arrive `[INFER]`. Each dequeued item is written to the queue's `out` property (the downstream consumer link)
`[CERT]`.

## 346.2 — Backpressure: bounded 1000, overflow REJECTS (throws)

The capacity is `maxSize`, default **1000**, min 0 `[CERT]` (`BEngineCycleQueue.java:58`). Enqueue goes
`enqueueMessage → engineCycleQueue.performEnqueue` `[CERT]` (`:118-119`), and `performEnqueue` does a
non-blocking `queue.offer(msg)`; if it returns false (queue full) it **throws `QueueFullException`** `[CERT]`
(`EngineCycleQueue.java` `performEnqueue`). So the backpressure policy is **bounded + REJECT**: not drop-oldest,
not block-the-producer — a synchronous throw to the caller `[CERT]`. This is exactly the exception [Block 340]
§340.3 caught in the array-forEach selector (which re-threw it as `RoutingFailedException`) — corroborated from
the consumer side `[INFER]`. Consequence: memory is hard-bounded at 1000 items per queue, and an overflow
surfaces as a fault, never silent loss or an engine stall `[INFER]`.

This closes the engine-thread-risk thread: the COV-driven synchronous regenerate [Block 337] §337.3 and the
30 s query wait [Block 338] §338.2 are the LATENCY bound; the engine-cycle queue is the THROUGHPUT + MEMORY
bound (one/cycle, cap 1000, reject) `[INFER]`.

## 346.3 — The three concrete queues

All extend `BEngineCycleQueue` and differ only in payload `[CERT]`:

| Queue | Payload | Fed by |
|---|---|---|
| `BEngineCycleMessageQueue` | `String` | the subscription→generate path [Block 337] J3, array-forEach [Block 340] J6 |
| `BEngineCycleAlarmQueue` | `BAlarmRecord` | the JSON alarm recipient [Block 345] J11 |
| `BEngineCyclePairQueue` | `BBaseAndOutputPair` | the relative-schema per-base path [Block 343] J9 |

`[CERT]` (`BEngineCycleMessageQueue.java:47-53`, `BEngineCycleAlarmQueue.java:47-53`, `BEngineCyclePairQueue.java:47-53`).

## 346.4 — `toJsonType`: the Baja→JSON primitive boundary (null becomes `""`)

`JsonSchemaUtil.toJsonType(value, config)` is where every leaf value crosses into JSON `[CERT]`
(`JsonSchemaUtil.java:164`). The dispatch, all `[CERT]` (`:167-218`):

| Baja input | JSON output |
|---|---|
| `null` | **`""`** (empty string — NOT JSON `null`) `[CERT]` (`:167-172`) |
| `BInteger` / `BLong` | int / long |
| `BNumber` (BDouble/BFloat) | double, rounded to `config.numericPrecision` (NaN→0.0, ±Inf→MIN/MAX) |
| `BBoolean` | boolean |
| `BControlPoint` / `BStatusValue` | RECURSE on the unwrapped value (`getStatusValue()`/`getValueValue()`) — status stripped `[CERT]` (`:206-211`) |
| `BAbsTime` | `config.formatTime(v)` (formatted string) |
| everything else (`BString`, `BFrozenEnum`, …) | `v.toString()` — an enum emits its TAG `[CERT]` (`:218`) |

The load-bearing quirk: **a null/unresolved value serializes as `""`, never JSON `null`** `[CERT]` (`:169`) — so
a consumer cannot distinguish "empty string" from "absent/unresolved" in the payload `[INFER]`. And status
wrappers are unwrapped to their bare value (status/priority lost unless emitted as a separate property) `[INFER]`.

## 346.5 — Minor utils (one line each)

- `JsonKeyExtractUtil` — mixin that parses a JSON string (`JSONTokener`) and pulls named keys; throws
  `MissingJsonValueException` on a required-but-absent key `[CERT]` (`JsonKeyExtractUtil.java:17`).
- `JsonStringUtil` — holds the module `Lexicon.make("jsonToolkit")` + a min-length-2 viability guard `[CERT]`
  (`JsonStringUtil.java:5-16`).
- `BListOf<T>` — a generic `BComponent` list over `BVector` with `itemAdded`/`itemRemoved` topics `[CERT]`
  (`BListOf.java:67`).
- `BBFormatErrorSubstituteValue` — frozen enum `ignore`/`keyOnly`/`blank` (default `blank`) controlling what
  `FormatResolveUtil` [Block 345] J11 emits on an unresolvable format token `[CERT]`
  (`BBFormatErrorSubstituteValue.java:15`).
- `MissingJsonValueException` / `ParentLegal` — a `BajaRuntimeException` for absent required keys; and a
  parent-type validator for `isChildLegal` (dev-only `disableChecks` downgrades to a WARNING) `[CERT]`
  (`MissingJsonValueException.java:5`, `ParentLegal.java:11`).

## 346.6 — What this block does NOT resolve

- `LicenseLimit` — [Block 335] J1 (feature + import/export attributes + SMA), remitted.
- `FormatResolveUtil` — [Block 345] J11 (the `BFormat` resolver `BBFormatErrorSubstituteValue` feeds).
- The platform `com.tridium.util.EngineCycleQueue` internals beyond the enqueue/overflow contract — a baja util,
  not the jsonToolkit focus.

## 346.7 — Connections

- [Block 337] §337.3 / [Block 338] §338.2 — the latency bounds; §346.2 is the throughput/memory bound that
  completes the engine-thread-safety picture.
- [Block 340] §340.3 — the array-forEach `QueueFullException` catch that corroborates §346.2's reject policy.
- [Block 345] §345.2 / [Block 343] — the alarm and relative paths that use the Alarm/Pair queues.
- [Block 336] §336.2 / [Block 342] — the property leaves whose values pass through `toJsonType` (§346.4).

## 346.8 — Self-verify

Block TYPE: **evidence**. Delegated sweep **sonnet**; driver re-verified verbatim: `maxSize`=1000
(`BEngineCycleQueue.java:58`), the `EngineCycleQueue` wrap + `performEnqueue` (`:3`, `:84`, `:118-119`), the
overflow `QueueFullException` throw (platform `EngineCycleQueue`, corroborated by [Block 340] §340.3's catch),
and `toJsonType`'s null→`""` (`JsonSchemaUtil.java:167-172`) + status-unwrap (`:206-211`) + enum/string toString
(`:218`). No official doc for the util package (internal).

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 22 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 7 |
| INFER/CERT ratio | 0.32 |

`verify-block.sh` exit 0. No official doc for the util package (internal).

Evidence block: `[INFER]`s are the coalescing/throughput reading and the null-vs-empty-string consumer
consequence — each anchored to a cited `[CERT]`.
