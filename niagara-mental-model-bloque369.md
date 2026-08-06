# Niagara N4 — Bloque 369: `webChart` (W2) — the series/scale model: autoscale is a 3-tier facet precedence (`facetsLimitMode` off/inclusive/locked), and the DEFAULT `average` sampling AVERAGES OUT a limit-crossing spike (only its alarm-status color survives, not its height) — a global setting that cannot preserve `<12` dips and `>28` spikes at once

> **Focus**: `webChart`, gap **W2** (the series/scale/sampling model in depth). Continues [B368] (the render engine
> that consumed this model): [B368] saw the layer call `!isLocked()` and `model.scaleTicks()`; W2 is the MODEL side.
> [B199] §199.3 already surveyed the hierarchy (BaseModel→LineModel), the 4 series types, the `seriesFactory`
> chain + `webChart:IChartFactory` extension point, and named `samplingUtil.rollup` — all **REMITTANCE**, not
> re-derived. W2 adds the depth the survey skipped: the autoscale MODES and the sampling FIDELITY.
>
> Subject: Niagara N4.14 `webChart-ux` `rc/model/` + `rc/ChartSettings.js` (extracted bajaux JS).
>
> **Sources** (all `[CERT]`, read/verified this iteration):
> - `ChartSettings.js` (the enum declarations + docstrings), `model/ValueScale.js`, `model/BaseScale.js`,
>   `model/samplingUtil.js`, `model/BaseSeries.js`, `model/modelUtil.js`
> - Remittance `[CERT]`: [B199] §199.3 (model spine + series types + factory), [B368] (the render layers that read
>   `getMinMax`/`scaleTicks`/`isLocked`/`samplingPoints`), [B367]/[B360]/[B362] (the alarm-band thread this sharpens)
>
> **Method**: delegated `general-purpose` sweep of `model/` (16 tool-uses), then **inline driver re-resolution of
> the 7 load-bearing citations** (the two enum defaults, the averaging arithmetic, the status-OR, the per-series
> facet override, `samplingPoints`, the `focusPoints` non-average trim). Block type: **EVIDENCE**.

---

## §369.1 — Autoscale is a 3-tier facet precedence; the model side of [B368]'s `!isLocked()` `[CERT]`

The Y-domain policy is the enum **`facetsLimitMode`** — declared `["off", "inclusive", "locked"]`, **default
`"off"`** `[CERT]` `ChartSettings.js:107`. Its authoritative semantics (docstring `ChartSettings.js:676-687`)
`[CERT-doc-inline]`:

- **`off`** (default): ignore the `min`/`max` facets for the domain. *But `chartMin`/`chartMax` facets are STILL
  applied as inclusive bounds even when off.*
- **`inclusive`**: fold the `min`/`max` facets into the data-driven extent (domain = union of data ∪ facet bounds).
- **`locked`**: ignore the data entirely — force the domain to the facet `min`/`max` (or `chartMin`/`chartMax`).
- **Per-series override**: a series facet **`chartLimitMode` = `'inclusive'`|`'locked'`** overrides the global even
  when the global is `off` `[CERT]` `ValueScale.js:104`. `chartMin`/`chartMax` outrank `min`/`max` in every mode.

The domain decision is a **three-tier precedence** (highest wins):

1. **Options-set lock** (`BaseScale.getLockedMinMax` `:325-335`): the user's explicit per-axis lock —
   `settings.get("locked")` with finite `min<max`. `ValueScale.initialize` wires this into a hard `setLocked(true,…)`
   at construction `[CERT]` `ValueScale.js:32-44`, which is exactly what `isLocked()` reports to `YAxisLayer.js:47`
   ([B368] §368.4).
2. **Facet `locked` mode** (`ValueScale.js:104-113`): options beat facets; else `chartMin`/`chartMax` (preferred)
   or `min`/`max`.
3. **Data-driven + inclusive folding** (`ValueScale.getMinMax` `:129-196`): seed from `chartMin`/`chartMax` always,
   fold `min`/`max` only under `inclusive`, then scan point data to widen ("show any outliers", `:149`). Boolean →
   `[0,1]`; discrete → enum ordinal range.

**Relevance to the band thread** [INFER]: `inclusive`/`locked` with `chartMin=12`/`chartMax=28` can force the Y
axis to always *contain* the alarm limits (so the band region is on-axis) — but this is **axis domain only**; it
draws no shaded band. [B367]'s verdict stands: the band itself is still custom.

## §369.2 — The default `average` sampling AVERAGES OUT a limit-crossing spike `[CERT]` — the load-bearing finding

`samplingType` is declared `["average", "min", "max", "sum"]`, **default `"average"`** `[CERT]`
`ChartSettings.js:117`, and it is **GLOBAL — one setting per chart**, not per-series: `rollup` reads it once via
`model.samplingType()` `[CERT]` `samplingUtil.js:193`; there is no per-series `getSamplingType`.

In `rollup` each time-bucket aggregates by type `[CERT]` `samplingUtil.js:218-251`:

- **`average`/`sum`**: accumulate `newValueSum += point.y` for every sample (`:221`); `average` then divides
  `newPoint.y = newValueSum / newRecordCount` (`:243-244`). A single high sample in an otherwise-normal bucket is
  **arithmetically averaged away** — one emitted point per bucket, no extreme retained.
- **`max`/`min`**: keep the bucket extreme (`:222-225`) — these *preserve* a spike, but are **not** the default.
- No path retains min AND max together (no min/max envelope). `focusPoints` even confirms it: it trims edge padding
  ONLY when `samplingType !== "average"` (`BaseSeries.js:331`, comment: "ensure pre and post records don't skew data
  if sampling is on and not average"). `[CERT]`.

**Precise nuance — the alarm COLOR survives, the alarm HEIGHT does not** `[CERT]`: the rollup OR-combines status,
`newStatus |= point.status` `samplingUtil.js:232`. So the rolled-up point keeps the in-alarm status bit — the
per-sample status *coloring* of [B368] §368.3 will still tint it — but its **value** is the bucket average, so the
line does not visibly rise above the limit. **Plainly**: with stock settings, on a range wide enough to auto-sample
(threshold `getMaxSamplingSize()` = 2500, [B199] §199.5), a brief PSI spike above 28 is flattened out of the line;
you might see a tinted point, but not a curve that crosses the band.

## §369.3 — One global `samplingType` cannot preserve `<12` dips and `>28` spikes at once `[INFER]`

The client's requirement is a **dual** band: crossings **below 12** AND **above 28**. Preserving high spikes needs
`samplingType='max'`; preserving low dips needs `'min'`. Because `samplingType` is a **single global setting**
(§369.2), a sampled chart can honor only ONE of the two — `max` hides every low-limit dip, `min` hides every
high-limit spike. The only way to keep both is to **not sample** (raw points), but a wide range then hits the point
**capacity trim** (`$points` is capacity-trimmed, `BaseSeries.js:284`) and the 2500 auto-sample threshold. So on the
"cheaper interactive webChart path" [B362] named, faithfully showing *both* bands over a month-wide range is **not a
settings choice** — it is a structural limitation, reinforcing that the crossing-marking is custom work, not a
toggle. `[INFER]` (grounded on the verified single-global-setting mechanics).

## §369.4 — `getMinMax` and `scaleTicks`: the model calls [B368]'s axis layer delegates to `[CERT]`

- `ValueScale.getMinMax(useSamplingPoints, fallback)` `:129-196`: unions facet bounds (§369.1 tier 3) then scans
  every series on the scale (`seriesList()`, `:134`), choosing `samplingPoints()` vs `points()` per the flag
  (`:161`), over the scale's data property (`$dataProperty` default `"y"`). Infinities fall back to `fallback[]`.
- `scaleTicks()` `BaseScale.js:190-202`: asks `primarySeries().getTicks()`; an Array is used verbatim, a Number is
  a tick-count for `d3 scale().ticks(n)`; no primary series → `ticks(8)`. `BaseSeries.getTicks` `:703-738`:
  shade→`0`, boolean→`[0,1]`, discrete→rounded ordinals, else `8`. `[CERT]`.

## §369.5 — The point model and the two independent type-predicate sources `[CERT]`

- **A "point"** (typedef `BaseSeries.js:269-275`): `{ x:Date, y:Number, [skip], [status] }`. There is **no
  `trendFlags` property on the point** — the wire `r`=trendFlags of [B199] §199.2 is mapped into `skip` breaks by
  `modelUtil.getSkipInfo` (`:371`), a depth refinement of [B199] (the wire carries `r`; the client model transforms
  it to `skip`, not a point field). `[CERT]`/[INFER].
- **`points()` vs `samplingPoints()`**: `points()` `:284-293` = the full raw set (capacity-trimmed) + interpolated
  tail; `samplingPoints()` `:357-366` returns the rollup **only when** `model.isSampling()` (or the series
  `isBar()`) and `$samplingPoints` exists, else it degrades to `focusPoints()` (the zoom-window subset). `[CERT]`.
- **Type predicates from TWO distinct sources** `[CERT]`: `isLine`/`isDiscreteLine`/`isBar`/`isShade` `:124-157`
  read the **chartType string** from settings (user/facet-driven, mutable); `isDiscrete`/`isBoolean` `:568-581`
  read the **`$recordType` TypeSpec** (data-derived: `"baja:Boolean"`/`"baja:DynamicEnum"`), set by each subclass
  from the fetched record type (`ServletSeries.js:47`, `ScheduleSeries.js:75`, `PointSeries.js:63`,
  `ExternalSeries.js:55`). So *how it's drawn* (chartType) and *what it is* (recordType) are orthogonal.
- **`valueScale()`** `:404-406` returns the series' own fallback `ValueScale`; primary/secondary Y is decided by the
  model regrouping by unit ([B199], `BaseModel.js:1003-1055`) via `modelUtil.sortSeriesList` `:1044`. Color/facets
  come from settings/`$facets`. `[CERT]`.

## §369.6 — `modelUtil` is the 1289-line utility spine `[CERT]` (breadth, not deep-read)

`chunkData` (`:1199`, streamed AJAX history → `preparePoint`, progressive `REDRAW_REQUEST_EVENT`); `getFullOrd`
(`:127`, appends the `period=<tag>` sampling suffix, `:139`); `stretchDomain` (`:1243`, ±2% Y padding);
`getLines`/`getAreas`/`getBars` (`:519`/`:500`/`:554`, split seriesList by predicate for DOM layering); point prep
`prepareServletPoint`/`prepareLivePoint`/`getSkipInfo`; sampling-time math `getStartTime`/`getNiceTimeIncrement`;
`sortSeriesList` (`:1044`, primary-scale pick). One-line-each; not load-bearing. `[CERT]` (sweep-confirmed).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `facetsLimitMode` = off/inclusive/locked, default off; semantics per docstring | [CERT] | `ChartSettings.js:107,676-687` | ✅ read |
| 2 | Per-series `chartLimitMode` facet overrides the global even when off | [CERT] | `ValueScale.js:104` | ✅ read |
| 3 | Autoscale is 3-tier: options-lock > facet-lock > data+inclusive; wired via `initialize`→`setLocked` | [CERT] | `ValueScale.js:32-44,104-113,129-196`; `BaseScale.js:325-335` | ✅ read |
| 4 | `samplingType` default = `average`, GLOBAL per chart (not per-series) | [CERT] | `ChartSettings.js:117`; `samplingUtil.js:193` | ✅ read |
| 5 | `average` divides sum by count → a limit-crossing spike is averaged out; max/min preserve but aren't default | [CERT] | `samplingUtil.js:218-244` | ✅ read |
| 6 | Rollup OR-combines status → alarm COLOR survives, alarm HEIGHT does not | [CERT] | `samplingUtil.js:232,243-244` | ✅ read |
| 7 | `focusPoints` trims edges only when `samplingType !== "average"` (average is the lossy default path) | [CERT] | `BaseSeries.js:331` | ✅ read |
| 8 | One global `samplingType` can't preserve both `<12` dips (min) and `>28` spikes (max) | [INFER] | §369.3 from claims 4-6 + `BaseSeries.js:284` | ✅ reasoned |
| 9 | Point = {x,y,skip,status}; no trendFlags field (wire `r`→`skip` via getSkipInfo); points vs samplingPoints; predicates from chartType vs $recordType | [CERT]/[INFER] | `BaseSeries.js:269-275,284-293,357-366,124-157,568-581`; `modelUtil.js:371` | ✅ read |

**Marker tally**: [CERT] ×6 · [CERT]/[INFER] ×1 · [INFER] ×1 · [CERT-doc-inline] ×1. Ratio ≈ 0.25. Block type =
**EVIDENCE**. Load-bearing tokens re-resolved to disk this iteration: both enum defaults (`facetsLimitMode`,
`samplingType`), the averaging arithmetic + status-OR (the fidelity finding — the corpus's central new claim here),
the per-series `chartLimitMode` override, `samplingPoints`, and the non-average `focusPoints` trim — each read
directly, per the framework-semantic rule (not trusted from the delegated sweep). No §14 correction; one depth
refinement of [B199] (wire `r`=trendFlags → client `skip`, not a point field). Delegated sweep tier:
`general-purpose`; no claim rejected.

## Connections

- [B368] — the render layers that call `getMinMax`/`scaleTicks`/`isLocked`/`samplingPoints`; W2 is their model side.
- [B199] §199.3 — the model spine + series types + factory + `samplingUtil.rollup` naming (remittance; §369.5
  refines the wire-`r`→`skip` mapping).
- [B367]/[B362]/[B360] — the alarm-band/crossing thread: §369.2-3 add a NEW cost/risk to the "cheaper webChart
  path" — default `average` sampling hides crossings, and one global `samplingType` can't show both bands. The
  crossing-marking remains custom, now for a second structural reason (sampling), beyond the missing band renderer.
- Forward: **W3** (donut/gauge/boxTable chart types), **W8** (the `webChart:IChartFactory` extension, partly named
  in [B199]).

## Gaps opened / queued

**W2 closed.** No child gap. The sampling-fidelity finding (§369.2-3) is decision-relevant REMITTANCE back to the
reports deliverable — logged here, does not reopen reports (still 9/9). Focus `webChart` now **2/9**. NEXT = **W3**
(the chart-type catalog: donut, gauge, boxTable, and how a type is selected).
