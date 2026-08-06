# Niagara N4 — Bloque 367: the modern `webChart` framework does NOT render alarm-limit bands natively either — no threshold/limit-line layer, the data feed carries no limit values, and the render layers are a hardcoded array with no extension point; so the client's `<12`/`>28` band is custom on EVERY charting path (closes the thread B366 opened)

> **Focus**: charting line ([B199], [B251]–[B259]) — the "webChart and the bands" thread opened by [B366] §Gaps.
> [B362] named the **interactive `webChart` path** as the *cheaper* alternative to the blocked scheduled-report
> chart; [B366] proved Niagara Analytics rides webChart and adds no band. The open question: **does the `webChart`
> framework itself draw alarm-limit bands / threshold lines bound to a point's alarm extension natively — making
> the cheaper path also near-free for the band — or is the band custom here too?** This block answers it.
>
> Subject: Niagara N4.14 `webChart` (`webChart-rt` + `webChart-ux`, decompiled + extracted `rc/` bajaux JS).
>
> **Sources**:
> - `[CERT]` `webChart-ux/extracted/rc/line/` — the 11-layer render engine (`Line.js`, `DataLayer.js`,
>   `YAxisLayer.js`, `XAxisLayer.js`, `ZoomLayer.js`, `LegendLayer.js`, `LabelsLayer.js`, `TipLayer.js`,
>   `TitleLayer.js`, `DataPopupLayer.js`)
> - `[CERT]` measured-absence sweep over the whole `webChart` module (rt+ux)
> - `[CERT]` `webChart-ux/extracted/rc/webChartUtil.js`, `gauge/model.js` — the only `alarm` references (status
>   coloring, not limit bands)
> - `[CERT-doc]` `docDeveloper` → `webChart-rt` / `webChart-ux` developer guide (silent on limit lines)
> - niagara-help `guide-search` + `devguide-search` (real zeros, tool-verified)
> - Remittance `[CERT]`: [B199] (the `{t,v,r,s}` history feed servlet), [B361] (classic `BChart` has no band API),
>   [B366] (Analytics rides webChart), [B362] (the cheaper-path claim under test)
>
> **Method**: bounded question → measured absence triangulated three ways + one extensibility read (the layer
> composition). Positive control: the same sweep returns 34 `limit` / 4 `alarm` hits (tool works; the threshold /
> limit-line zeros are REAL). Block type: **EVIDENCE / DECISION**.

---

## §367.1 — The only `alarm` awareness is per-sample status coloring, not a limit band `[CERT]`

`webChart-ux` references `alarm` in exactly one role: **coloring a sample by its `BStatus`**. `webChartUtil.js`
builds status brush arrays keyed on the lexicon (`Status.alarm.fg`/`Status.alarm.bg`, `webChartUtil.js:575,579`)
and tests `status.isAlarm()` to pick a sample's color (`:540,591`; likewise `gauge/model.js:219`). `[CERT]`. This
turns a **point** the alarm color *when that sample's status is in-alarm* — it does not shade the `<12`/`>28`
**value region** across the time span, and it knows nothing of the alarm extension's `highLimit`/`lowLimit`. It is
the same status-coloring the classic side does; it is not a threshold band.

## §367.2 — No threshold/limit/reference-line layer exists; the layer set is a hardcoded array `[CERT]`

The line chart is composed of a **fixed set of 11 layers** (`line/` dir). The composition is a literal array in
`Line.js`:

```js
that.$layers = [that.$x, that.$y, new LabelsLayer(that), new ZoomLayer(that),
                new DataLayer(that), new TitleLayer(that), new LegendLayer(that),
                new DataPopupLayer(that), new TipLayer(that)];   // Line.js:25
```

`[CERT]` `line/Line.js:25`. `runLayers(graph, name)` simply reduces over `graph.$layers`, calling a named lifecycle
method (`initialize`/`graphData`/`redraw`) on each if present `[CERT]` `line/Line.js:55-61`. There is **no
`LimitLayer` / `ThresholdLayer` / `ReferenceLayer` / `BandLayer`** in the directory, and **no registry / `addLayer`
/ plugin hook** — layers are not registered, they are hand-listed. `[CERT]` (dir listing + `Line.js`).

**Extensibility consequence**: adding a band overlay is **not** "register a layer" — it requires **forking the
render composition** (`Line.js` or the widget), because the layer array is closed. `[CERT]`/[INFER].

## §367.3 — Measured absence across the whole module (second name) `[CERT]`

A whole-module sweep (rt+ux) for every limit-line spelling returns **zero**:
`limitLine|plotLine|markLine|referenceLine|thresholdLine|alarmLimit|hiLimit|loLimit|highLimit|lowLimit|drawBand|
shadeRegion|limitBand` → **0 hits**. `[CERT]`. And the render-engine terms `threshold`, `region`, `gridLine`,
`hilimit`, `lolimit` → **0** in the authored `rc/**`. The non-zero counts that looked promising are false
positives on inspection:

| Term | Raw count | What it actually is |
|---|---|---|
| `band` | 1–2 | `"aba**band**on the current sampling"` (`BaseModel.js:161`) — not a chart band |
| `reference` | 8 | "function **reference**", "give pre**ference** to", "d3 API **reference**" |
| `limit` | 34 | series limits (`maxSeriesListLength`), axis-scale mode (`chartLimitMode` = locked/inclusive), "limit reached" errors — never an alarm limit |

`[CERT]` (each hit read). The tool works (34+4 positive hits); the threshold/limit-line zeros are real.

## §367.4 — Structural clincher: the data feed carries no limit values `[CERT]` (remittance [B199])

Even if a band layer were written, it would have nothing to draw: the history feed is
`{t, v, r, s}` — time / value / trendFlags / status — served by `WebChartQueryServlet` ([B199], `:133-203`). A
sweep of the servlet for `highLimit|lowLimit|hiLimit|loLimit|limit` returns **0** `[CERT]`. The point's alarm
**extension** limits are never transported to the client, so the band's Y positions (`12`, `28`) are not even in
the wire model. Drawing them requires **injecting** the limits from elsewhere — i.e., custom. `[CERT]`/[INFER].

## §367.5 — Doc + niagara-help: silent, real zeros `[CERT-doc]`

The `docDeveloper` webChart developer guide (`webChart-rt` + `webChart-ux`) has **0 files** mentioning `limit line
/ threshold / reference line / alarm limit / plot-line / band` `[CERT-doc]`. niagara-help `guide-search 'chart
alarm limit line'` and `devguide-search 'webChart limit line reference'` both return *no match* — real zeros, not
tool failures (the CLI answered; other queries in this session returned content). `[CERT-doc]` (tool-verified
negative). All three sources agree: the feature does not exist and is not documented because there is nothing to
document.

## §367.6 — Verdict: the band is custom on EVERY path; webChart is still the cheaper one, but not because bands are native `[INFER]`

Consolidating the four charting paths the corpus has now measured for the client's `<12`/`>28` band:

| Path | Native alarm-limit band? | Evidence |
|---|---|---|
| Scheduled report chart (station) | ❌ blocked at rt/wb profile boundary; no band API | [B361] |
| Classic `BChart` (Workbench/PDF) | ❌ no band/threshold API (0 hits) | [B251]–[B259], [B361] |
| Niagara Analytics chart suite | ❌ 7 analytical types, 0 alarm code, rides webChart | [B366] |
| **Interactive `webChart`** | ❌ **no limit layer, feed carries no limits, layers not extensible** | **§367.1–5** |

So **neither** of Niagara N4.14's two charting engines draws an alarm-limit band, and the Analytics product adds
none: the `<12`/`>28` band is **intrinsically custom** in this platform. [B362]'s "the banded chart is the
dominant cost" is now confirmed a fifth way.

**But [B362]'s other claim also holds, now with the precise reason**: the interactive `webChart` path *is* the
cheaper route — not because the band is native (it is not), but because you **inherit the whole engine** (history
feed, resampling, axes, zoom/pan, time-range) and add only the band. The catch measured here: that addition is a
**fork of the layer composition** (§367.2), not a clean plugin — so it is *cheaper than a from-scratch headless
renderer* (the report path's JFreeChart/iText, [B362] §362.5), yet not *trivial*. The honest cost ordering for the
band: **webChart-fork < classic-BChart-subclass < headless-from-scratch**; all three are custom, none is a config
toggle. `[INFER]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | webChart's only `alarm` use is per-sample status coloring (isAlarm → color), not a limit band | [CERT] | `webChartUtil.js:540,575,579,591`; `gauge/model.js:219` | ✅ read |
| 2 | The line chart is a hardcoded 11-layer array; `runLayers` iterates it; no LimitLayer/registry | [CERT] | `line/Line.js:25,55-61` + dir listing | ✅ read |
| 3 | Adding a band = fork the layer composition (no addLayer/plugin hook) | [CERT]/[INFER] | §367.2 from claim 2 | ✅ reasoned |
| 4 | Whole-module sweep: 0 limitLine/plotLine/markLine/referenceLine/alarmLimit/highLimit/lowLimit/drawBand/shadeRegion | [CERT] | measured (§367.3) | ✅ re-run |
| 5 | band/reference/limit non-zero counts are false positives (abandon / function reference / series+axis limits) | [CERT] | `BaseModel.js:161`; grep of the 34 `limit` hits | ✅ read |
| 6 | The history feed is `{t,v,r,s}`; the servlet emits 0 limit fields → limits not transported to client | [CERT] (remittance) | `WebChartQueryServlet.java:133-203` ([B199]); 0-hit sweep | ✅ re-run |
| 7 | docDeveloper webChart guide + niagara-help: 0 limit-line mentions (real zeros) | [CERT-doc] | docDeveloper webChart-{rt,ux}; guide+devguide-search | ✅ tool-verified |
| 8 | Band is custom on all 4 charting paths; webChart still cheapest (inherit engine, fork a layer) | [INFER] | §367.6 from claims 1-7 + [B361]/[B366]/[B362] | ✅ reasoned |

**Marker tally**: [CERT] ×4 · [CERT]/[INFER] ×1 · [CERT] remittance ×1 · [CERT-doc] ×1 · [INFER] ×1. Ratio ≈ 0.14.
Block type = **EVIDENCE / DECISION**. Load-bearing tokens re-resolved to disk: the layer array (`Line.js:25`, the
extensibility fact), the measured absences (claims 4-6, re-run so the negatives are not tool artifacts), and the
alarm-coloring role (claim 1, to distinguish it from a band). Positive control satisfied inline (34 `limit` + 4
`alarm` hits prove the sweep works). No §14 correction — corroborates [B361]/[B366]/[B362], contradicts no block.

## Connections

- [B366] — opened this thread; Analytics rides webChart, so its band absence and webChart's are the same ceiling.
- [B362] — the cheaper-interactive-path claim; §367.6 confirms it AND supplies the precise reason (inherit engine,
  fork a layer) + the cost ordering.
- [B199] — the webChart history feed servlet (`{t,v,r,s}`) whose missing limit fields are §367.4's clincher.
- [B361] / [B251]–[B259] — the classic charting paths that also lack a native band; the platform-wide pattern.

## Gaps opened / queued

No child gap for `reports` (stays 9/9). The charting-path question raised by [B362]/[B366] is now **fully closed**:
the alarm-limit band is intrinsically custom across all Niagara N4.14 charting engines, and the cheapest custom
route (webChart-fork) is characterized. **Optional future focus** (not queued): a full `webChart` subsystem focus
(the framework was only ever touched in [B199]); the layer engine, series model, and bajaux integration remain
un-mapped in breadth — but nothing there changes the band verdict.
