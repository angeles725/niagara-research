# Niagara N4 — Bloque 366: build-vs-buy for the client report — Niagara Analytics does NOT ship the banded PSI-vs-time chart out of the box; its chart suite is 7 analytical/aggregation views built ON webChart, and its whole authored frontend has ZERO alarm-limit awareness (confirms B362's dominant-cost finding)

> **Focus**: `reports` — **ADDENDUM** to the closed 9/9 focus. Not an R-gap: a build-vs-buy follow-up raised by the
> [B362] synthesis (the deliverable's dominant cost is the *banded chart renderer*, blocked twice). The question a
> buyer asks before writing custom code: **would licensing Niagara Analytics — a Tridium product that already
> extends `BChart` and ships a chart suite — deliver the client's range-scoped PSI-vs-time chart with alarm-limit
> bands, so the custom renderer is avoided?** This block answers it. It does **not** reopen charts ([B199],
> [B251]–[B259]) or Analytics-the-framework ([B16], [B66], [B67]); it re-reads them through the deliverable lens.
>
> Subject: Niagara N4.14 `analytics` (TridiumPS add-on, decompiled corpus) + `docAnalyticsGuide` official guide.
>
> **Sources**:
> - `[CERT]` `analytics-ux/decompiled/com/tridiumx/analytics/ux/BAnalyticsChartFactory.java` — the chart-factory
>   singleton (agent on `box:BoxTable`, implements `javax.baja.webChart.BIChartFactory`)
> - `[CERT]` `analytics-ux/extracted/rc/AnalyticsChartFactory.js` — the JS factory (extends
>   `nmodule/webChart/rc/model/BaseChartFactory`, yields `AnalyticsBoxTableSeries`)
> - `[CERT]` measured-absence sweep over `analytics-ux/extracted/rc/**` (authored frontend, excluding vendored
>   `ext/c3/c3.js` + `analytics.built.min.js`)
> - `[CERT-doc]` `docAnalyticsGuide` → `doc/AlgorithmsAndAlerts.html` (algorithm outputs framing)
> - Remittance `[CERT]`: [B66] §66.3.3 (the 7 chart types), [B67] §67 (Spectrum = load histogram, not FFT; c3.js
>   frontend; chart-rt + webChart-rt deps), [B251]–[B259] (classic `BChart` has no native band API),
>   [B362] (the composition + dominant-cost thesis this addendum tests)
>
> **Method**: bounded question → targeted driver reads (the factory chain read to disk) + one measured-absence
> re-run under a clean glob. Block type: **EVIDENCE / DECISION** (build-vs-buy adjudication).

---

## §366.1 — The build-vs-buy question, stated precisely `[INFER]`

[B362] fixed the deliverable's cost on one item: a **banded PSI-vs-time chart** — Y=PSI, X=date over a chosen
range, with the `<12` / `>28` critical bands shaded and each limit-crossing marked with a timestamp — because the
stock `report` module can render no chart on the station and the classic `BChart` exposes no band/threshold API
([B361]). Before building that renderer, a rational buyer checks the shelf: **Niagara Analytics** is a licensed
Tridium product that [B259] already noted *extends `BChart`*, and [B66]/[B67] documented as a large charting +
algorithm framework. If Analytics already draws a trend with alarm-limit bands, the "buy" path deletes the most
expensive line item. This block tests exactly that — nothing about Analytics' algorithm value in general, only:
**does its chart suite deliver the banded trend?**

## §366.2 — Analytics is a webChart *consumer*, not a second charting engine `[CERT]`

`BAnalyticsChartFactory` is `@NiagaraSingleton`, agents on `box:BoxTable`, and **implements
`javax.baja.webChart.BIChartFactory`** `[CERT]` `BAnalyticsChartFactory.java:30,34`. It carries no drawing logic —
it hands off to JS: `JsInfo.make("module://analytics/rc/AnalyticsChartFactory.js")` `[CERT]` `:37`. That JS
**`define(['...', 'nmodule/webChart/rc/model/BaseChartFactory', ...])`** and its factory produces an
`AnalyticsBoxTableSeries` per capable `box:BoxTable` series `[CERT]` `AnalyticsChartFactory.js:11,42-43`.

So Analytics does not own a charting stack; it **plugs into the `webChart` framework** ([B199]) and feeds it series
derived from analytics *result box tables*. Consequence for build-vs-buy: whatever native alarm-band capability
exists is a **`webChart` capability**, not an Analytics one — Analytics inherits webChart's ceiling. (This is the
same inheritance [B259] flagged for the classic side; here it is the *modern* side.) `[CERT]`/[INFER].

## §366.3 — The chart catalog is 7 analytical views — none is a limit-band trend `[CERT]` (remittance [B66] §66.3.3)

Analytics renders **7 chart types** (remittance [B66] §66.3.3, factory dispatch): **Spectrum, RelativeContribution,
LoadDuration, AverageProfile, Ranking, Aggregation, EquipmentOperation**. Every one is an *analytical/aggregation*
answer, not a point-value time series with thresholds:

- **Spectrum** = an *hourly load histogram*, explicitly **not** an FFT ([B67] §67 correction).
- **RelativeContribution** = stacked bar with a color-range editor ([B66] §66.4 `BRelativeContributionChart`).
- **LoadDuration / AverageProfile / Ranking / Aggregation / EquipmentOperation** = duration curves, profile
  averages, rankings, roll-ups.

None of the seven is "plot the raw signal against time and shade the alarm band." The closest primitive is the c3
**axis chart** (`chart/ux/type/c3/BC3AxisChartType`), which draws a time series — but it draws the *analytics
series*, with no limit-band parameter (§366.4). `[CERT]`/[INFER].

## §366.4 — Measured absence: the authored Analytics frontend has ZERO alarm-limit awareness `[CERT]`

Clean sweep over the **authored** frontend `analytics-ux/extracted/rc/**` (excluding the vendored `ext/c3/c3.js`
library and the `analytics.built.min.js` bundle):

| Term | Files matched |
|---|---|
| `alarm` | **0** |
| `setpoint` | **0** |
| `alarm limit` | **0** |
| `limit line` | **0** |
| `region` (c3 shaded band) | **0** |

`[CERT]` (literal query, re-run under `-g '!analytics.built.min.js' -g '!**/c3/**'`). The non-zero `threshold`/
`limit` hits elsewhere are unrelated: `d3.scale.threshold()` (a scale, `AnalyticC3BaseChart.js:28`) and
`$textLengthLimit = 20` (label truncation, `:32`). The 30 `baseline` hits are the Analytics **baseline-comparison**
concept (this-period vs a reference period), **not** an alarm band. Even the c3 library *has* `regions`/`grid.y.lines`
(§ the library supports shaded Y-bands and threshold grid-lines), but **Analytics never calls them** — the wrapper
binds no limit to a region. The band the client needs is not merely un-exposed in the UI; it is **absent from the
code**. `[CERT]`.

## §366.5 — The official guide frames alarm and chart as PARALLEL outputs, never overlaid `[CERT-doc]`

`docAnalyticsGuide` states an algorithm's contract: *"An algorithm performs a calculation on real-time or
historical (trend) data to generate a result. The result can **trigger an alert or an alarm**, **be displayed on a
chart**, or can become an input to another calculation."* `[CERT-doc]` `AlgorithmsAndAlerts.html`. The three
outputs are **coordinate alternatives** joined by *or* — the crossing becomes an **alarm record** (viewed in the
Alarm Console, `ViewingAnAlertInTheAlarmConsole.html`) *or* the trend is **shown on a chart**; the guide never
describes the alarm limit being **drawn onto** the chart as a band. This is the doc-level confirmation of the
code-level absence in §366.4: Analytics' model is *detect-and-record the crossing*, not *shade-the-band*.

## §366.6 — Verdict: buying Analytics does NOT collapse the dominant cost item `[INFER]`

Mapping Niagara Analytics onto the four-piece deliverable of [B362]:

| Deliverable leg | Does licensing Analytics deliver it? |
|---|---|
| Detect + record the limit crossing (`<12` / `>28`) | ✅ **Yes** — an algorithm block (threshold/range filter) fires an **alarm record** with a timestamp ([B67] blocks; guide §366.5). This is real "buy" value for the *marker* data. |
| Range aggregation / analytics over the period | ✅ **Yes** — the 22 trend wrappers ([B67] §67.5) + aggregation charts cover range roll-ups. |
| **Banded PSI-vs-time chart** (shaded `<12`/`>28` + crossing markers) | ❌ **No** — 7 analytical chart types, none is a banded trend (§366.3); zero alarm/limit-band code in the frontend (§366.4); charts on `webChart`, inheriting its ceiling (§366.2). The band renderer stays **custom**. |
| Excel-like table over a chosen range | ❌ Not from Analytics — this is the `report`/history leg of [B359]/[B363], unchanged. |

**Bottom line**: Analytics is a *buy* for the crossing-detection and analytics legs, but **not** for the item
[B362] priced as dominant — the banded chart. That renderer is custom in **both** the build path and the buy path,
so licensing Analytics does not delete the most expensive line; it trims the cheaper legs (detection, aggregation).
The reports verdict [B362] stands **and is strengthened**: the banded chart is intrinsic-custom regardless of
build-vs-buy. `[INFER]`.

An honest caveat for the recommendation: whether the band could be *near-free* on the **interactive** path depends
on what the **`webChart`** framework itself supports (regions / y-grid-lines bound to a point's alarm extension) —
Analytics rides webChart, so that ceiling is webChart's, not Analytics'. That is the next open thread (§ Gaps).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BAnalyticsChartFactory` implements `webChart.BIChartFactory`, agents on `box:BoxTable`, delegates to JS | [CERT] | `BAnalyticsChartFactory.java:30,34,37` | ✅ read |
| 2 | The JS factory extends webChart `BaseChartFactory` and yields `AnalyticsBoxTableSeries` — Analytics is a webChart consumer | [CERT] | `AnalyticsChartFactory.js:11,42-43` | ✅ read |
| 3 | Analytics renders 7 analytical chart types; none is a limit-band trend | [CERT] (remittance) | [B66] §66.3.3; Spectrum=histogram [B67] | ✅ remittance + reasoned |
| 4 | Authored frontend `rc/**` has 0 `alarm`, 0 `setpoint`, 0 `alarm limit`, 0 `limit line`, 0 `region` (clean glob) | [CERT] | measured sweep (query in §366.4) | ✅ re-run |
| 5 | `threshold`/`limit`/`baseline` hits are unrelated (d3 scale, text-length, baseline-comparison) | [CERT] | `AnalyticC3BaseChart.js:28,32` | ✅ read |
| 6 | Official guide frames alert/alarm vs chart as parallel algorithm outputs, never overlaid as a band | [CERT-doc] | `AlgorithmsAndAlerts.html` | ✅ read |
| 7 | Classic `BChart` (which Analytics also depends on via chart-rt) has no native band API | [CERT] (remittance) | [B361] R6; [B251]–[B259] | ✅ remittance |
| 8 | Verdict: buying Analytics reduces detection + aggregation legs but NOT the banded chart → dominant cost stays custom | [INFER] | §366.6 from claims 1-7 + [B362] | ✅ reasoned |

**Marker tally**: [CERT] ×4 · [CERT] remittance ×2 · [CERT-doc] ×1 · [INFER] ×1. Ratio ≈ 0.14. Block type =
**EVIDENCE / DECISION**. Load-bearing tokens re-resolved to disk this iteration: the factory chain (claims 1-2,
the `BIChartFactory`/`BaseChartFactory` inheritance that makes Analytics a webChart consumer) and the measured
absence (claim 4, re-run under a clean glob so the negative is not polluted by the vendored c3 library or the
minified bundle). No §14 correction — this addendum **corroborates** [B362]/[B259], contradicts no prior block.

## Connections

- [B362] — the composition + dominant-cost thesis this addendum tests; verdict strengthened, not changed.
- [B361] — classic `BChart` has no band/threshold API; §366 shows the *modern* Analytics side inherits the same gap.
- [B199] — the `webChart` framework Analytics plugs into; the open band question (§Gaps) belongs to webChart.
- [B66] §66.3.3 / [B67] — the 7 chart types + Spectrum=histogram + chart-rt/webChart-rt deps, cited as remittance.
- [B251]–[B259] — the classic charting focus; [B259] first noted Analytics extends `BChart`.

## Gaps opened / queued

No child gap for `reports` (focus stays stopped 9/9; this is a decision addendum). **One thread raised, not a
reports gap**: does the **`webChart`** framework itself render alarm-limit **regions / y-grid-lines bound to a
point's alarm extension** natively? That is the "webChart and the bands" angle — the cheaper *interactive* path
[B362] named — and the natural next block. It sits under the PX/web charting line ([B199]), not under `reports`.
