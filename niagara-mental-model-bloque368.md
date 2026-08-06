# Niagara N4 — Bloque 368: `webChart` (W1) — the line render engine is a hand-built D3-v3 layer system: a duck-typed promise-chained `runLayers` dispatcher over a hardcoded 9-layer array, full-redraw (no incrementality), a series-type branch tree in `DataLayer`, in-memory zoom with no servlet round-trip; extensibility closed (reinforces B367)

> **Focus**: `webChart`, gap **W1** (the line render engine in depth). The framework's core: how a webChart is
> composed, scaled, drawn, and zoomed on the client. [B199] surveyed the spine (bajaux-pure, 4 series types,
> gauge); [B367] opened only `line/Line.js:25` (the layer array) to settle the alarm-band question. W1 maps the
> 11 `line/` layers' own draw/scale/redraw logic. First evidence block of the focus.
>
> Subject: Niagara N4.14 `webChart-ux` `rc/line/` (extracted bajaux JS). Rendering stack: **D3 v3** (`d3.svg.line`,
> `d3.behavior.zoom` — the legacy pre-v4 API).
>
> **Sources** (all `[CERT]`, read/verified this iteration):
> - `line/Line.js`, `line/DataLayer.js`, `line/YAxisLayer.js`, `line/XAxisLayer.js`, `line/ZoomLayer.js`
>   (+ chrome: `LegendLayer`/`LabelsLayer`/`TitleLayer`/`TipLayer`/`DataPopupLayer`)
> - Remittance `[CERT]`: [B199] (spine + servlet feed), [B367] (band verdict + `Line.js:25` array)
>
> **Method**: delegated `general-purpose` sweep of all 11 layer files (11 tool-uses), then **inline driver
> re-resolution of the 5 load-bearing citations to disk** (the lifecycle dispatcher, the D3 line generator, the Y
> autoscale gate, the X domain, the zoom handler). One sweep claim CORRECTED on re-read (§368.4, `chartLimitMode`).
> Block type: **EVIDENCE**.

---

## §368.1 — Lifecycle: a duck-typed, promise-chained dispatcher over a hardcoded layer array `[CERT]`

`runLayers(graph, name, ...args)` reduces over `graph.$layers` and calls `layer[name](...)` **only if
`typeof layer[name] === "function"`**, chaining each through `prom.then(...)` so a layer returning a Promise blocks
the next `[CERT]` `Line.js:55-65`. Four phase names are dispatched: **`initialize`** (`:128`), **`graphData`**
(with the data, `:138`), **`redraw`** (`:165`), **`destroy`** (`:301`). The layer set is the hardcoded array of
[B367] `[CERT]` `Line.js:25`: `[XAxis, YAxis, Labels, Zoom, Data, Title, Legend, DataPopup, Tip]`.

The order matters: **axes redraw before `DataLayer`**, so the value/time scales are current when the series paths
are generated. A layer opts into a phase merely by implementing the method (e.g. `YAxisLayer.graphData` is an empty
stub; `TipLayer` implements only `redraw`). `[CERT]`/[INFER].

## §368.2 — `DataLayer`: a D3-v3 line generator, full-redraw, branch-tree by series type `[CERT]`

**Draw call**: the solid line's `d` attribute is produced by
`d3.svg.line().interpolate(series.getLineInterpolation()).x(d => graph.getScaleX()(d.x)).y(d => scale(d.y))
.defined(d => isDefinedForNormalGraph(d, widget))(points)` `[CERT]` `DataLayer.js:322-328`, assigned via
`normalPathSelection.attr('d', ...)` `[CERT]` `:295`. This is **D3 v3** (`d3.svg.line`, not v4's `d3.line`).

- **Curve is per-series config**, not fixed: `series.getLineInterpolation()` `[CERT]` `:322`. Areas/shades force
  `d3.svg.area().interpolate("step-after")` `[CERT]` `:276,305`.
- **Two paths per line**: a `.normal` (real samples) and a `.interpolated` (dashed gap-fills / tail), split by
  `isDefinedForNormalGraph` vs `isDefinedForInterpolatedGraph`, keyed off `getShowDataGaps`/`isInterpolateTail`
  settings `[CERT]` `DataLayer.js:124-125,578-633`.
- **Sample markers**: one `<circle class="dot" r=2.75>` per point `[CERT]` `:513-537`; bars draw `rect.bar` per
  point `:334-416`.
- **Draw dispatch is a hardcoded branch tree on series type**: `isLine`/`isShade`/`isBar`/`isDiscrete`/`isBoolean`
  `[CERT]` `:206,232,261,273,334`. No seam to register a new draw primitive (§368.6).
- **Full-redraw, not incremental**: every `redraw` regenerates each path's entire `d` string and re-binds
  `.data(points)` for dots/bars/status-lines `[CERT]` `:295,322,336,515`. There is no append-only update path — a
  new sample repaints the whole series.

## §368.3 — Per-sample status coloring is the only alarm awareness — and it is switchable off `[CERT]`

A sample is colored by its `BStatus` via `webChartUtil.statusToColor(d.status)` — dots `[CERT]` `:526-527`, shade
status-lines `:446-447`, bar status-lines `:501-502`; presence tested by `hasStatusColor(d.status)` `:433,466`.
This is **gated by `settings.getStatusColoring()`**: when `'off'` (or when a shade coexists with a line), the point
array is emptied so no status marks draw `[CERT]` `:420-422`. This is exactly the "alarm awareness" of [B367]
§367.1 seen from the draw side: the engine tints a *sample* by its in-alarm status; it never shades the
value-limit **region** — and even the tint is optional. `[CERT]`/[INFER].

## §368.4 — Axes: autoscale fires only under zoom, ticks delegated to the model `[CERT]`

**Y (`YAxisLayer.rescale` `:37-54`)**: for each value scale, pixel `range([height,0])` `[CERT]` `:45`; **autoscale
runs only when `(widget.isDataZoom() || widget.isTimeZoom()) && !valueScale.isLocked()`** `[CERT]` `:47`, pulling
`valueScale.getMinMax(true, [0,10])` `:49` through `checkDomain` (fallback / equal-bounds `±4` / `stretchDomain`)
`:315-332`. A **locked** scale is never autoscaled `[CERT]` `:47`. Multiple Y scales are supported —
`model.mapValueScales(fn)` `:44`, secondary drawn in its own `g.ticks2` `:121-139`. Y ticks are **delegated to the
model** (`valueScale.scaleTicks()` `:116,128`), not generated by D3.

**§14 correction of the sweep**: the strings `chartLimitMode` / `inclusive` do **NOT** appear in the axis layers —
the only autoscale-locking primitive there is `isLocked()` `[CERT]` (re-grep). The `chartLimitMode`/facets-limit
mode lives in `model/ValueScale.js` (the MODEL), which is gap **W2**, not the render layer. The sweep's phrasing
implied the mode was in the axis; corrected here.

**X (`XAxisLayer.domain` `:136-200`)**: min/max from `points[0].x` / `points[last].x` across series `[CERT]`
`:154-164`, with `fixedTime`/period fallbacks and data-zoom clamping to the configured `timeRange`. X ticks =
`scale.ticks(tickCount)` `[CERT]` `:346` with `tickCount = floor(width/80)` `:484`, then timezone-corrected;
`checkDomain` rejects year <1900/>2200 and spans <5 s `:209-261`.

## §368.5 — Zoom: `d3.behavior.zoom`, in-memory rescale, NO servlet round-trip `[CERT]`

`ZoomLayer.redraw` `:24` builds a `d3.behavior.zoom()` `:30` on the `.line-container-overlay` rect. Modifier keys
select the axis: **alt = Y-only** `:71,84-86`, **shift/ctrl = X-only** `:99`, plain drag = both; a locked value
scale forces X-only `:79-82`. **No brush** exists (no `d3.svg.brush` anywhere). The zoom handler reads
`d3.event.scale`/`translate`, applies them to each `valueScale.zoom` `[CERT]` `:145-152`, clamps X via
`XAxisLayer.checkDomain` `:154-158`, then calls `graph.widget().manualZoom()` `[CERT]` `:165` to drive a redraw.

**No servlet fetch is issued from zoom** `[CERT]`/[INFER]: re-sampling to the new window is client-side over
already-loaded points (`XAxisLayer.redraw` → `model().startSampling()` `XAxisLayer.js:426` / `addExtraPoints()`
`:476`, over `series.samplingPoints()`). Live re-subscription exists but lives in `Line.initialize`
(`Line.js:110-127`), driven by live-mode events, not by zoom. So panning/zooming a historical range is a pure
in-memory rescale of the data the servlet already delivered ([B199]'s `{t,v,r,s}` feed).

## §368.6 — Verdict: a closed, hand-built D3-v3 engine `[INFER]`

The line engine is **not** a wrapper over a charting library (unlike Analytics' c3, [B366]/[B67]) — it is a
hand-built layer system directly on **D3 v3**. Its only structural seam is `runLayers`' duck-typing (`Line.js:60`),
but the `$layers` array is hardcoded (`:25`) with no registry, and `DataLayer`'s draw is a hardcoded type branch
(`:206…`) with no primitive-registration hook. The only parameterized behaviors are **per-series model config**
read at draw time (interpolation, color, scale-lock, status-coloring on/off). New visual behavior — a limit band,
a new mark — requires editing `DataLayer`'s branch tree or adding a layer to the `Line.js:25` array. This **closes
W1** and reinforces [B367] from the inside: the engine is capable and interactive, but **closed**. Two design
consequences carry into cost estimates: (a) the **full-redraw** model (§368.2) bounds live-update performance, and
(b) the **legacy D3-v3** stack (§368.2/5) is a modernization liability (D3 is at v7; `d3.svg.line`/`d3.behavior.zoom`
were removed in v4). `[INFER]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `runLayers` is a duck-typed, promise-chained reduce over the hardcoded `$layers`; 4 phases dispatched | [CERT] | `Line.js:25,55-65,128,138,165,301` | ✅ read |
| 2 | The series line is a D3-v3 `d3.svg.line()` generator; curve is per-series config | [CERT] | `DataLayer.js:295,322-328` | ✅ read |
| 3 | Draw is full-redraw (whole `d` regenerated each pass) with a hardcoded series-type branch tree; dots r=2.75 | [CERT] | `DataLayer.js:206,232,261,295,336,513-537` | ✅ sweep + spot-read |
| 4 | Per-sample status coloring via `statusToColor`, gated off by `getStatusColoring()` | [CERT]/[INFER] | `DataLayer.js:420-422,526-527` + [B367] §367.1 | ✅ sweep-cited |
| 5 | Y autoscale only under zoom & `!isLocked()`; ticks delegated to model; multi-scale | [CERT] | `YAxisLayer.js:47,49,116,128` | ✅ read |
| 6 | §14: `chartLimitMode`/`inclusive` NOT in axis layers (only `isLocked()`); the mode is in `model/ValueScale.js` = W2 | [CERT] | re-grep of axis layers; `ValueScale.js` (earlier) | ✅ re-grep |
| 7 | X domain from first/last sample; ticks = `scale.ticks(floor(width/80))` | [CERT] | `XAxisLayer.js:136,154-164,346,484` | ✅ read |
| 8 | Zoom = `d3.behavior.zoom` in-memory rescale, no servlet fetch; alt=Y/shift=X; no brush | [CERT]/[INFER] | `ZoomLayer.js:145-165` + `XAxisLayer.js:426,476` | ✅ read |
| 9 | Engine is closed (no layer registry, no draw-primitive hook); D3-v3 legacy stack | [INFER] | §368.6 from claims 1-3 + [B367] | ✅ reasoned |

**Marker tally**: [CERT] ×5 · [CERT]/[INFER] ×2 · [INFER] ×1 · +1 §14. Ratio ≈ 0.2. Block type = **EVIDENCE**.
Load-bearing tokens re-resolved to disk this iteration: the lifecycle dispatcher (claim 1), the D3 line generator
(claim 2), the Y autoscale gate (claim 5), the X domain + ticks (claim 7), and the zoom handler (claim 8) — the
five the sweep flagged, each read directly, per the framework-semantic rule (not trusted from the delegated sweep).
§14 recorded (claim 6): the sweep implied `chartLimitMode` lived in the axis; re-grep shows it does not — it is a
`ValueScale` (model) concern, deferred to W2. Delegated sweep tier: `general-purpose`; no claim rejected, one
scoped correction.

## Connections

- [B367] — the layer array + band verdict; §368.6 confirms the engine is closed from the inside.
- [B199] — the spine + the `{t,v,r,s}` servlet feed that §368.5 rescales in memory on zoom.
- [B366]/[B67] — Analytics wraps c3; §368.6 contrasts: webChart's native engine is hand-built D3-v3, not a lib.
- Forward: **W2** (`model/` — `ValueScale.getMinMax`/`scaleTicks`/`isLocked` + `chartLimitMode`, the model half of
  the autoscale this block saw the layer side of); **W3** (donut/gauge/boxTable draw, the other chart types).

## Gaps opened / queued

**W1 closed.** No child gap. Threads handed forward (already in the W2/W3 backlog): the `ValueScale` model
(autoscale modes, `scaleTicks`, sampling) is W2; the non-line chart types (donut, gauge, boxTable) are W3. Focus
`webChart` now **1/9**. NEXT = **W2** (the series/data model in depth).
