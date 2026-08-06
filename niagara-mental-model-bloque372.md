# Niagara N4 — Bloque 372: `webChart` (W3) — the chart-type catalog is TWO top-level agents (line `ChartWidget` + single-value `CircularGaugeWidget`) plus a code-only donut; the gauge has NO alarm-limit zones (single status-driven fill), so nowhere in webChart is an alarm limit drawn as a colored region

> **Focus**: `webChart`, gap **W3** (the chart-type catalog beyond the line engine). Continues [B368] (line render)
> and [B372's sibling] W-gaps. **Remittance**: [B199] §199.6 (CircularGauge as single-value gauge), [B368] (the
> line DataLayer branch tree), [B366]/[B367] (the platform-wide no-native-alarm-band pattern this extends).
>
> Subject: `webChart-ux` `rc/donut/`, `rc/gauge/`, `module.xml`.
> **Method**: delegated sweep + inline verify (the module.xml agents, the gauge no-zones grep, the donut D3 pie).
> Block type: **EVIDENCE**.

---

## §372.1 — Two top-level chart agents; the type is chosen by the target's baja type `[CERT]`

`module.xml` registers exactly **two** chart Widgets as agents: `ChartWidget` (name `ChartWidget`) on history
spaces/folders/queries + Numeric/Enum/Boolean points + `webChart:ChartFile` + schedules `[CERT]`
`webChart-ux/module.xml:39-58`; and `CircularGaugeWidget` on single-value sources only (control points, schedules,
kitControl numerics, virtual points) `[CERT]` `:60-73`. Both extend `bajaux/Widget` directly. The chart TYPE is
thus selected by **which agent the user opens on a component**, gated by the component's baja type via the
`<on type>` lists — not an ORD "type" nor a setting. `[CERT]`/[INFER].

## §372.2 — Donut is a code-only `BaseWidget`, a static count summary `[CERT]`

`DonutChartWidget` is **not** an agent and **not** a view — it extends `webEditors/rc/fe/BaseWidget` and is only
instantiated programmatically via `fe.buildFor(...)` `[CERT]` `DonutChartWidget.js:10,88`; it appears nowhere in
`module.xml`. It renders with D3 v3 `d3.layout.pie()` + `d3.svg.arc()` over a plain `{type, value}` array, center =
the summed count `[CERT]` `:99,129,132-138` — a static, subscription-less count donut (healthy/warning/critical
color map), unrelated to the line engine. `[CERT]`.

## §372.3 — The gauge has NO alarm-limit zones — a single status-driven fill `[CERT]`

`CircularGaugeWidget` gets a **live-subscribed** point value (`subscriberMixIn`, re-render on "changed";
`model.js:54` resolves `widget.value()`) `[CERT]` and renders a D3 arc + animated needle over −125°→+125°
`[CERT]` `CircularGaugeWidget.js:144-150,211-235`. **Crucially, there are no colored limit zones**: a grep of
`gauge/` for `zone|band|threshold|hiLimit|loLimit` returns **0**; the only alarm reference is `status.isAlarm()`
`[CERT]` `gauge/model.js:219`, which picks ONE whole-gauge fill color by point status
(disabled/fault/down/alarm/stale/overridden). `min`/`max` come from facets and only set the scale range — no
hi/lo alarm-limit arcs. `[CERT]`. This is decision-relevant: the gauge is the one widget where a colored
limit zone would be idiomatic, and webChart draws none — extending the platform-wide finding ([B366]/[B367]) that
no Niagara charting surface renders alarm limits.

## §372.4 — No other chart-type widgets; bar/shade/discrete are per-series modes `[CERT]`

There is no bar/scatter/area/heatmap/pie top-level widget. What looks like "more types" is the per-SERIES
`chartType` ∈ line/discreteLine/bar/shade inside the ONE line engine (`BaseSeries.js:115-156`; drawn by the
[B368] `DataLayer` branch tree, e.g. `bar` = `rect.bar` at `DataLayer.js:345`) `[CERT]`. So the catalog is: **line
chart (with 4 per-series render modes) + single-value gauge + a code-only donut** — three renderers, none with a
native limit zone.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Two chart agents only: ChartWidget (line) + CircularGaugeWidget (single-value); type = agent `<on type>` gating | [CERT] | `module.xml:39-73` | ✅ read |
| 2 | Donut = code-only BaseWidget (not agent/view), D3 pie over `{type,value}`, static count summary | [CERT] | `DonutChartWidget.js:10,88,99,129` | ✅ read |
| 3 | Gauge = live-subscribed value, D3 arc+needle | [CERT] | `CircularGaugeWidget.js:144-150,211-235`; `gauge/model.js:54` | ✅ sweep-cited |
| 4 | Gauge has NO limit zones — single status-driven fill; grep zone/band/threshold = 0, only `isAlarm` | [CERT] | `gauge/model.js:219` + measured grep | ✅ re-grep |
| 5 | No other top-level chart types; bar/shade/discreteLine are per-series modes in the line engine | [CERT] | `BaseSeries.js:115-156`; `DataLayer.js:345` | ✅ sweep-cited |

**Marker tally**: [CERT] ×4 · [CERT]/[INFER] ×1. Ratio ≈ 0.2. Block type = **EVIDENCE**. Load-bearing re-resolved to
disk: the two `module.xml` agents (claim 1), the gauge no-zones grep + `isAlarm` (claim 4), the donut D3 pie
(claim 2). No §14.

## Connections

- [B368] — the line render engine the bar/shade/discrete series modes draw through.
- [B366]/[B367] — the platform-wide no-native-alarm-band finding; §372.3 adds the gauge as another negative site.
- [B199] §199.6 — CircularGauge as single-value gauge, now confirmed to lack limit zones.

## Gaps opened / queued

**W3 closed.** No child gap.
