# Niagara N4 — Bloque 375: `webChart` (W8) — the chart-factory extension is a two-sided contract (Java marker `BIChartFactory extends BIJavaScript` registered as an agent tagged `webChart:IChartFactory`, JS `BaseChartFactory.factory()` returning a Promise of series) and charting is NOT license-gated; but the seam adds a SERIES for a data type, not a new DRAW primitive — a band still needs the closed DataLayer of B368

> **Focus**: `webChart`, gap **W8** (the `BIChartFactory` extension contract + license gate). Decision-relevant to
> build-vs-buy ([B362]/[B366]): can a custom banded-chart type be registered, and does it cost a license?
> **Remittance**: [B199] §199.3 (the `seriesFactory` chain + `webChart:IChartFactory` tag), [B366] (Analytics'
> `BAnalyticsChartFactory`), [B368] (the render engine is closed).
>
> Subject: `webChart-rt` `BIChartFactory.java` + `module.xml`; `webChart-ux` `seriesFactory.js`, `BaseChartFactory.js`.
> **Method**: delegated sweep + inline verify of the marker interface, the tree-wide license grep, the JS registry
> lookup. Block type: **EVIDENCE / DECISION**.

---

## §375.1 — `BIChartFactory` is a marker interface; discovery is by tagged agent `[CERT]`

`BIChartFactory` declares **no own methods** — it is `public interface BIChartFactory extends BIJavaScript` with
only a `TYPE` field `[CERT]` `BIChartFactory.java:17-21`. The method contract (`getJsInfo()` → the backing JS
module) is inherited from `javax.baja.web.js.BIJavaScript`. So the Java side is a **typed tag**: "this component
has a JS chart-factory." Discovery is by **agent registration** — an implementer registers as an `<agent>` on a
target type, resolvable under the exported type `webChart:IChartFactory` (`webChart-rt/module.xml:30`). This is the
exact seam [B366] found Analytics using (`BAnalyticsChartFactory implements BIChartFactory` agent on
`box:BoxTable`). `[CERT]`.

## §375.2 — JS side: the registry lookup returns a Promise of series `[CERT]`

The `seriesFactory` chain-of-responsibility terminates in `registryFactory` `[CERT]` `seriesFactory.js:729-755`:
it takes the dropped value's `type`, and calls
**`StationRegistry.getInstance().resolveFirst(type, { tags: ['webChart:IChartFactory'] })`** `[CERT]` `:44,739-740`
— resolving the first agent on that type carrying the tag. On a hit it does `new ChartFactoryConstructor()` then
`factory.factory(model, subscriber, seriesParams, params)` `[CERT]` `:743-744`. A custom JS factory therefore
returns **a Promise resolving to an array of `BaseSeries`** (`BaseChartFactory.js:36-37,41-43`) — the series to add
to the chart.

## §375.3 — Charting is NOT license-gated `[CERT]`

A grep of the **entire** `webChart` tree (rt + ux, Java + JS + both `module.xml`) for
`getFeature|<feature|feature=|LicenseException|checkLicense` returns **zero** `[CERT]` (measured). `module.xml`
declares only `vendor="Tridium"` and `<permissions>` — **no `<feature>` element** `[CERT]`
`webChart-rt/META-INF/module.xml:2`. The module is `autoload="true" installable="true"`. **Plainly: drawing a
webChart / adding a chart series requires no license feature — charting is free with the base station.** The only
gates are the runtime Java permissions and the `canRead`/`canWrite` data-model checks of [B374]. `[CERT]`.

## §375.4 — The seam adds a SERIES, not a DRAW primitive — reconciling with [B368] `[INFER]`

To register a custom chart type a developer authors: **(Java)** a `BIChartFactory` impl with `getJsInfo()` →
its JS module, registered as an `<agent>` on the target data type; **(JS)** a `BaseChartFactory` subclass
overriding `factory()` to return the series. No license step. `[CERT]` (recipe composed from §375.1-3).

But the crucial nuance for the client's **banded PSI chart** [INFER]: this seam lets you feed a custom
**series for a new data TYPE** (what Analytics does for `box:BoxTable`) — the series is then drawn by the
**existing, closed** render engine ([B368]) as one of line/bar/shade/discrete. It does **not** let you register a
new **draw primitive** (a shaded threshold band). So the extension point and [B368]'s "render engine is closed" are
BOTH true, at different layers: **open at the series/factory layer, closed at the draw layer.** A banded chart
therefore still requires the `DataLayer` fork of [B368] §368.6 — the factory buys you data ingestion, not the band.
The one good news for cost: no license, and the on-screen chart engine (axes/zoom/feed) is inherited. `[INFER]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BIChartFactory` is a marker interface `extends BIJavaScript` (no own methods; `getJsInfo` inherited) | [CERT] | `BIChartFactory.java:17-21` | ✅ read |
| 2 | Discovery = agent tagged `webChart:IChartFactory` (the seam Analytics used) | [CERT] | `webChart-rt/module.xml:30`; [B366] | ✅ sweep + remittance |
| 3 | JS `registryFactory` does `reg.resolveFirst(type, {tags:['webChart:IChartFactory']})` → `factory.factory()` → Promise<BaseSeries[]> | [CERT] | `seriesFactory.js:44,729-755`; `BaseChartFactory.js:36-43` | ✅ sweep-cited |
| 4 | NO license gate: 0 feature/license hits tree-wide; module.xml has no `<feature>` | [CERT] | measured grep; `webChart-rt/META-INF/module.xml:2` | ✅ re-grep |
| 5 | The seam adds a series for a data type, NOT a draw primitive; a band still needs the [B368] DataLayer fork | [INFER] | §375.4 from §375.1-3 + [B368] | ✅ reasoned |

**Marker tally**: [CERT] ×4 · [INFER] ×1. Ratio ≈ 0.2. Block type = **EVIDENCE / DECISION**. Load-bearing
re-resolved to disk: the marker interface (claim 1), the tree-wide no-license grep (claim 4, re-run — a
decision-relevant negative), the JS registry lookup (claim 3). No §14 — reconciles [B368] and this seam at
different layers (§375.4).

## Connections

- [B366] — Analytics is the worked example of this seam (BIChartFactory on box:BoxTable).
- [B368] — the render engine is closed at the DRAW layer; §375.4 reconciles: open at the factory/series layer.
- [B362]/[B367] — the banded chart cost: no license (good) but the band is still a DataLayer fork (§375.4).
- [B199] §199.3 — the seriesFactory chain this terminates.

## Gaps opened / queued

**W8 closed.** No child gap.
