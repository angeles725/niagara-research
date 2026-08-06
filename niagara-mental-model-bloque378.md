# Niagara N4 — Bloque 378: alarm escalation & filtering is FOUR mechanisms across THREE modules — core AlarmClass level-timers (B34) + a core BQL *query* filter for display, and two OEM add-ons: bacnetAlarmRouter's wiresheet *routing* filters (a level-gate + an A/B divert switch, one carrying an index bug) and honAlarmExt's DELAYED/SENT/IGNORED hold — so the declarative "route escalated alarms to this recipient" gate is not core

> **Topic**: alarm escalation + filtering — the channel-agnostic layer that makes any alert (email included) tiered
> and gated. Chosen after the `email` focus ([B324]-[B334]) and [B34] §34.6.5 covered the alarm→email recipient
> itself. [B34] §34.2 documented the core **AlarmClass escalation** (level1/2/3 timers + `escalatedAlarmN` topics)
> and its javap inventory LISTED `BAlarmFilterSet`/`BEscalationFilter`/`BAlarmFilter`/`BDelayFilterState` — but
> never opened them. This block opens them. **Remittance**: [B34] §34.2 (AlarmClass escalation timers), §34.6.5
> (`BEmailRecipient`).
>
> Subject: N4.14 `alarm-rt` (`com.tridium.alarm`), `bacnetAlarmRouter-rt`
> (`com.tridiumemea.addons.bacnet.alarmServiceFilter`, a **TridiumEMEA** add-on), `honAlarmExt-rt`
> (`com.honeywell.honAlarmExt`, a **Honeywell** add-on).
> **Method**: READ-ONLY, all filter classes read in full to disk (~350 lines total; small enough to read inline,
> not delegated). Block type: **EVIDENCE**.

---

## §378.1 — "Filter" is overloaded: a QUERY filter and ROUTING filters are different things `[CERT]`

Two unrelated mechanisms share the word:

- **`BAlarmFilterSet`** (core, `com.tridium.alarm`) **extends `BFilterSet`** (`com.tridium.bql.filter`) — a **BQL
  query filter** that decides which alarms are **DISPLAYED** in a view/console, not how they are routed `[CERT]`
  `BAlarmFilterSet.java:17`. `accept(BComplex)` ANDs each active `BFilterEntry` against the record `:24-46`;
  `getPredicate()` emits a BQL where-clause `:48-81`. Special case: for the `source` field it matches `source` OR
  `alarmData.sourceName` (`(pred) OR (pred with source→alarmData.sourceName)`) `:34-37,59-68` — so a source filter
  catches both the ORD and the denormalized source name. `getQueryPredicate()` **throws
  `UnsupportedOperationException`** `:83-85` (BQL-string path only). This is the console/query layer.
- **The `alarmServiceFilter` package** (bacnetAlarmRouter add-on) — **routing filters** on the wiresheet (§378.2).

## §378.2 — The routing-filter pattern: a wiresheet action-in / topic-out gate `[CERT]`

`BAbstractAlarmFilter` (abstract `BComponent`) is the base for pipeline filters `[CERT]`
`BAbstractAlarmFilter.java:35`: an `enabled` boolean, an **input Action `routeAlarm(alarm:AlarmRecord)`**, an
**output Topic `alarm`**, and an abstract `doRouteAlarm(BAlarmRecord)` that subclasses implement to decide whether
to `fireAlarm(record)` (re-emit on the topic) `:36-62`. So the wiring is: *alarm source output → `filter.routeAlarm`
(link) → [doRouteAlarm decides] → `filter.alarm` topic → recipient.routeAlarm*. It is a **link-based gate on the
wiresheet**, channel-agnostic (the downstream recipient can be email, console, station, BACnet). `[CERT]`.

Two concrete filters:

- **`BAlarmFilter`** = a static **A/B divert switch** `[CERT]` `BAlarmFilter.java:47-55`: if `enabled`, fire on
  the `alarm1` topic when `divert==true`, else on the `alarm` topic. `divert` is a **config property, not
  per-record** — so it routes ALL passing alarms to output A or output B by configuration, not by content. A
  two-way manual splitter, not a content filter. `[CERT]`.
- **`BEscalationFilter`** = a **level-gate** (§378.3).

## §378.3 — `BEscalationFilter`: a level-gate keyed on the `escalated` alarm-data field — with an index bug `[CERT]`

`BEscalationFilter` has one property `allowedLevel` (int, facets min 0 / max 3) `[CERT]` `BEscalationFilter.java:31`.
`doRouteAlarm` `:50-69`:

- Reads `record.getAlarmData().get("escalated")` — a `BString` stamped by the core AlarmClass escalation ([B34]
  §34.2, the `escalatedAlarmN` path).
- If `escalated` is set/non-empty: fire ONLY if it equals `myLevel` (where `myLevel = LEVELS[allowedLevel-1]`,
  `LEVELS = {"level1","level2","level3"}`).
- If `escalated` is empty/null: fire ONLY if `allowedLevel == 0`.

So it routes a **specific escalation level** to a specific downstream recipient: `allowedLevel=0` passes only
original (non-escalated) alarms; `1/2/3` passes only alarms escalated to that level. This is the declarative
"level → recipient" gate that pairs with [B34] §34.2's timers. `[CERT]`.

**Latent bug** `[CERT]`: `resolveAlarmFilter()` computes `LEVELS[getAllowedLevel() - 1]` `:72`, and is called
unconditionally from `started()` `:79-81` and from `changed()` when `allowedLevel` is set `:83-86`. With the
**default `allowedLevel = 0`**, this is `LEVELS[-1]` → **`ArrayIndexOutOfBoundsException`** at component start.
`myLevel` stays null. Functionally, level-0 routing still works (its `doRouteAlarm` branch uses the `escalated`-empty
test, not `myLevel`), so the defect is **benign-but-real**: an exception + null `myLevel` on every level-0 filter at
start. `[CERT]` (the index) + `[INFER]` (severity: log noise / fragile init, not a routing failure for level 0).

## §378.4 — Honeywell's DELAY filter: a DELAYED/SENT/IGNORED hold, tracked on the record `[CERT]`

`honAlarmExt` (Honeywell) adds a **time-based delay/hold** filter — the nuisance-suppression / buffered-delay
recipient [B34] mentioned. Its state is `BDelayFilterState`, a frozen enum `{unknown, delayed, sent, ignored}`
`[CERT]` `BDelayFilterState.java:11-19`, stamped onto the record as an **alarm-data facet** `"DelayFilterState"`
(`setRecordFilterState`/`getFilterFacetValue`, `:39-48`) and consumed by `BHonConsoleRecipient` + `BHonAlarmClass`.
So an alarm can be **held (delayed)**, then **sent** or **ignored** — a debounce/dedup gate distinct from both the
core query filter and the bacnetAlarmRouter routing gate. `[CERT]` (enum + facet plumbing; the hold-timer logic
lives in `BHonConsoleRecipient`, not re-read here — noted as the honeywell delay recipient).

## §378.5 — Synthesis: four mechanisms, three modules; the declarative gates are OEM `[INFER]`

| Mechanism | Module | What it does | Where documented |
|---|---|---|---|
| AlarmClass level timers | core `alarm` | Stamp `escalated=levelN` + fire `escalatedAlarmN` after `escalationLevelNDelay` if still unacked | [B34] §34.2 (remittance) |
| `BAlarmFilterSet` (BQL query) | core `alarm` | Choose which alarms DISPLAY in a view/console | §378.1 |
| `BAlarmFilter` / `BEscalationFilter` (routing) | **bacnetAlarmRouter** (TridiumEMEA OEM) | Wiresheet gates: A/B divert; route a given escalation level to a recipient | §378.2-3 |
| Delay/hold (`BDelayFilterState`) | **honAlarmExt** (Honeywell OEM) | Hold→send/ignore debounce | §378.4 |

The load-bearing framing for the user's alert question: **core Niagara provides the escalation TIMERS (AlarmClass)
and a DISPLAY filter (FilterSet), but the declarative "route escalated alarms to THIS recipient" gate and the
delay/dedup hold are OEM add-ons** (bacnetAlarmRouter / honAlarmExt). To build "if not acked in 10 min, email the
next person" on a stock station you wire it by hand ([B34] §34.2 G2: enable `escalationLevel1`, set the delay, link
`escalatedAlarm1` → the second `BEmailRecipient.routeAlarm`) — the `BEscalationFilter` gate only makes that routing
declarative and is not present without the BACnet add-on. `[INFER]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BAlarmFilterSet` is a BQL QUERY filter (display), extends `BFilterSet`; source↔sourceName OR; getQueryPredicate throws | [CERT] | `BAlarmFilterSet.java:17,34-37,59-68,83-85` | ✅ read |
| 2 | `BAbstractAlarmFilter` = wiresheet gate: `enabled` + `routeAlarm` action-in + `alarm` topic-out + abstract `doRouteAlarm` | [CERT] | `BAbstractAlarmFilter.java:35-62` | ✅ read |
| 3 | `BAlarmFilter` = static A/B divert switch (config, not per-record) | [CERT] | `BAlarmFilter.java:47-55` | ✅ read |
| 4 | `BEscalationFilter` = level-gate keyed on the `escalated` alarm-data field; allowedLevel 0-3 | [CERT] | `BEscalationFilter.java:31,50-69` | ✅ read |
| 5 | Bug: `LEVELS[allowedLevel-1]` with default 0 → `LEVELS[-1]` AIOOBE in `started()`; level-0 still works (myLevel unused there) | [CERT]/[INFER] | `BEscalationFilter.java:34,72,79-86` | ✅ read |
| 6 | Honeywell delay filter = `BDelayFilterState{unknown,delayed,sent,ignored}` stamped as `DelayFilterState` facet | [CERT] | `BDelayFilterState.java:11-19,39-48` | ✅ read |
| 7 | Escalation ROUTING gate + delay hold are OEM; core has only the timers (B34) + the query filter | [INFER] | §378.5 from claims 1-6 + [B34] §34.2 | ✅ reasoned |

**Marker tally**: [CERT] ×5 · [CERT]/[INFER] ×1 · [INFER] ×1. Ratio ≈ 0.29. Block type = **EVIDENCE**. All filter
classes read directly to disk (small module surface). The bug (claim 5) and the query-vs-routing framing (claim 1)
are the load-bearing findings. No §14 correction — extends [B34]'s inventory into behavior.

## Connections

- [B34] §34.2 — the core AlarmClass escalation timers/topics that STAMP `escalated`; §378.3's filter GATES that stamp.
- [B34] §34.6.5 — `BEmailRecipient`, the downstream this routing feeds for email alerts.
- [B345] — the JSON alarm recipient (another downstream a filter could route to).
- [B242]-[B250] `oem-honeywell-tail` — honAlarmExt is Honeywell OEM; the delay filter (§378.4) belongs to that line.

## Gaps opened / queued

No focus bootstrapped (bounded topic, single block). Threads NOT pursued here (candidates if the alerting角 is
deepened): the honeywell **delay-timer logic** in `BHonConsoleRecipient` (the hold→send/ignore timing, only its
state enum was read); the core **AlarmClass `escalateAlarms()` loop** internals ([B34] §34.2 documented the wiring,
not the loop body); and whether any core (non-OEM) routing filter exists (none found — the routing gates are all in
bacnetAlarmRouter).
