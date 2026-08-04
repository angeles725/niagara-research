# Block 340 — The inbound core: a `route(BString)` action feeds JSON to selectors (JSONPath via jayway) and routers that map JSON keys to slot names by literal match, with an unguarded array-forEach and opt-in slot auto-creation

> Focus **jsonToolkit** — evidence block J6. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the INBOUND core — `BJsonInbound` (the `route(BString)` entry), the selector model (`BJsonSelector`,
> `BJsonPath` and the ~12 selectors), and the routers (`BJsonMessageRouter`, `BJsonDemuxRouter`,
> `BJsonArrayRouter`). The HANDLERS that actually write points / ack alarms / register export markers — and their
> authorization under `runAsUser` [Block 335] §335.4 — are J7.
>
> Sources (primary, decompiled N4.14.0.162 + docs), sweep sonnet + driver re-verification of the load-bearing +
> absence claims: `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/inbound/`
> (`BJsonInbound.java`, `selector/*`, `routing/*`) and `docJsonToolkit` (`InboundComponents-Json-71508AE4.html`,
> `AboutTheJsonPathSelector-json-A5F6317D.html`, `RedirectingMessages-Json-70340FD3.html`,
> `jsonToolkit-JsonPath.html`).
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[CERT-doc]` official doc · `[INFER]` deduction.
> Layer (integration/JSON) + Layer 22 (security surface). Block TYPE: **evidence**.

---

## 340.1 — `BJsonInbound`: the `route(BString)` entry

`public abstract class BJsonInbound extends BComponent` `[CERT]` (`BJsonInbound.java:87`). JSON enters through a
`@NiagaraAction route(BString)` (default `""`, flags 2072) `[CERT]` (`:75-80`, `:94`) → `doRoute` → `process` →
the abstract `routeValue(BString, Context)` implemented per concrete component `[CERT]` (`:178`, `:190`, `:204`).
So an inbound component is DRIVEN by invoking `route` with a raw JSON string — consistent with [Block 339] §339.4
(inbound RECEIVES via an action, it does not listen on a socket) `[INFER]`.

Parsing is DEFERRED and per-component: JSONPath selectors parse with **jayway** —
`DocumentContext ctx = JsonPath.parse(message); ctx.read(query, type)` `[CERT]` (`:253-257`) — while the array
components parse with Tridium's own `JSONTokener` `[CERT]` `[INFER]`. There is no single up-front parse.

## 340.2 — Selectors: extract a value to an `out` slot

`BJsonSelector` (abstract, extends `BJsonInbound`) `[CERT]` (`BJsonSelector.java:12`): every selector writes its
extracted value to an `out` property. `BJsonPath` is the JSONPath one: a `path` String property (`$.a.b[0]`-style)
`[CERT]` (`BJsonPath.java:35`) fed to `runJsonPathQuery(...)` → jayway `read()` `[CERT]` (`:73`). The ~12 selectors
group into `[CERT]` (class headers):
- **Value-extractors**: `BJsonPath`, `BJsonAtArrayIndex`, `BJsonContainsKey` (boolean), `BJsonIndexOfKeySelector`
  (numeric).
- **Structural/aggregate**: `BJsonLengthSelector` (`$..key.length()`), `BJsonFindAllSelector` (`$..key` deep scan).
- **Iteration**: `BJsonArrayForEachSelector` (§340.3).

Doc: "Selectors … take an inbound JSON message, apply some selection criteria, and set the result [in] an out
slot." — `jsonToolkit-JsonPath.html` `[CERT-doc]`; the `$.data.values.[0]` path notation is described in
`AboutTheJsonPathSelector-json-A5F6317D.html` `[CERT-doc]`.

## 340.3 — `BJsonArrayForEachSelector`: no pre-loop size guard (a resource surface)

It parses the message and loops EVERY element into an internal `BEngineCycleMessageQueue`, firing one per engine
cycle `[CERT]` (`BJsonArrayForEachSelector.java:117-125`):

```java
for (int i = 0; i < array.length(); i++) {
   ...
   this.enqueueArrayItem(JSONUtil.getString(array, i));   // no length check before the loop
}
```

I grep-confirmed there is **no `maxSlots`/size guard** in the class (0 hits) — the only backstop is a mid-loop
`QueueFullException` re-thrown as `RoutingFailedException` when the engine-cycle queue fills `[CERT]` (`:134-137`).
So a large inbound array enqueues elements until the queue is exhausted — a reactive, not preventive, cap: a
resource-exhaustion surface bounded only by the queue capacity `[INFER]`. This CONTRASTS with `BJsonArrayRouter`,
which has an explicit `maxSlots` cap (§340.4) `[CERT]`.

## 340.4 — Routers: JSON keys → slot names by LITERAL match

`BJsonRouter` (base) writes a value to a named slot via `setNewValueOnSlot()`: `set(slotName, value)`, and on
`NoSuchSlotException` either `add(slotName, ...)` if `learnMode` (§340.5) or throws `RoutingFailedException`
`[CERT]` (`BJsonRouter.java:71-85`). The three routers, all `[CERT]`:

| Router | Slot selection | Cap |
|---|---|---|
| `BJsonMessageRouter` | the VALUE of a configured JSON field (`key`, default `"messageType"`) becomes the slot name; the WHOLE message is written there | — (`:28`, `:55-71`) |
| `BJsonDemuxRouter` | each JSON key → a slot of the SAME literal name (`SlotPath.escape(jsonKey)`); missing keys reset to default if `defaultMissing` | — (`:66`, `:90`) |
| `BJsonArrayRouter` (+`ArraySortRouter`) | array element `i` → slot `"index"+i`, for `i < maxSlots` (default **50**) | 50 (`:69`, `:145-151`) |

Crucially, slot selection is by **literal JSON key/field name = slot name**, NOT a JSONPath expression `[CERT]`
(this answers the audit's demux-key question). Beyond `maxSlots`, `BJsonArrayRouter` logs FINE and IGNORES extra
elements `[CERT]` (`:151`). Doc: "A JsonMessageRouter … directs a whole incoming message … to a new slot … then
on to connected handlers" — `RedirectingMessages-Json-70340FD3.html` `[CERT-doc]`.

## 340.5 — `learnMode`: incoming JSON can CREATE slots (opt-in on the base, on-by-default on the array router)

`setNewValueOnSlot` calls `this.add(slotName, value, DEFAULT_ROUTER_SLOT_FLAGS)` when `learnMode` is true `[CERT]`
(`BJsonRouter.java:80`). Defaults differ `[CERT]`: the base `BJsonRouter.learnMode` is **false** (`:33`) — a
message/demux router must be explicitly opted in before incoming JSON can auto-create slots — but
`BJsonArrayRouter.learnMode` is **true** (`:75`), so an array router auto-creates its `index0..indexN` slots
(bounded by `maxSlots`=50). Security note: with `learnMode` ENABLED on a message/demux router, crafted incoming
JSON keys create arbitrary station slots — an inbound attack surface, mitigated only by the default-off `[INFER]`.
This pairs with J7 (what those slots then WRITE) for the full inbound-trust picture.

## 340.6 — Errors: `RoutingFailedException` carries the payload → fault

`RoutingFailedException` is a checked exception carrying the offending JSON `payload` String `[CERT]`
(`RoutingFailedException.java:3`). It is thrown across the selectors/routers (bad JSON, non-array, empty array,
missing key, unsupported conversion, queue full, slot-not-found-without-learn) and CAUGHT in `doRoute`, which
sets `BStatus.fault`, logs, and optionally clears outputs `[CERT]` (`BJsonInbound.java:182-188`). So a malformed
inbound message faults the component, it does not crash the engine `[INFER]`.

## 340.7 — What this block does NOT resolve

- The HANDLERS the routed slots feed — `BJsonSetPointHandler` (point write + priority + `runAsUser` auth),
  `BAlarmUuidAckHandler`, and the `exportMarker` registration handlers → **J7** (the inbound security block).
- jayway-jsonpath internals (bundled OSS, DISMISSED) — only its USE (`JsonPath.parse().read()`) is documented.

## 340.8 — Connections

- [Block 339] §339.4 — inbound receives via an action, not a socket; this block is that action (`route`).
- [Block 335] §335.4 — `runAsUser`, the identity the J7 handlers write as; the routers here just place values in slots.
- [Block 327] (email) — the email alarm-ack's `alarm.<uuid>` reply pattern is conceptually mirrored by the
  inbound alarm-ack handler (J7) `[INFER]`.
- J7 (handlers) / J12 (the engine-cycle queue §340.3 fills).

## 340.9 — Self-verify

Block TYPE: **evidence** (code + `[CERT-doc]`, with a driver-verified absence in §340.3). Delegated sweep
**sonnet**; driver re-verified verbatim: the `route(BString)` action (`BJsonInbound.java:75-80`), the
arrayForEach unguarded loop + grep-confirmed NO size guard (`BJsonArrayForEachSelector.java:117-137`, 0 guard
hits), the demux literal-key match (`BJsonDemuxRouter.java:66`, `:90`), `maxSlots`=50 (`BJsonArrayRouter.java:69`),
and `learnMode` defaults (base false `BJsonRouter.java:33`, array true `:75`, `add()` at `:80`). `[CERT-doc]`
token-checked; docs registered + preserved under `sources/manuals/jsonToolkit-docs/`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 21 |
| CERT-doc | 6 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 7 |
| INFER/CERT ratio | 0.26 |

`verify-block.sh` exit 0; `verify-sources.sh` no FABRICATED-CITE for B340.

Evidence block: `[INFER]`s are the two security-surface readings (unguarded forEach, learnMode auto-create) and
the receive-via-action link, each anchored to a cited `[CERT]`/`[CERT-doc]`.
