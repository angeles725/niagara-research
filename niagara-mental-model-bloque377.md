# Niagara N4 — Bloque 377: `webChart` (SÍNTESIS) — a capable but LEGACY-D3, hand-built chart engine that is open at the series/factory layer and closed at the draw layer; free of license, read-gated (with one servlet defect), and — like every Niagara charting surface — blind to alarm limits, so the client's banded PSI chart stays custom on the cheapest path too

> **Focus**: `webChart`, **SYNTHESIS** (capstone). Weaves the nine evidence blocks [B368]–[B376] (gaps W1-W9) with
> the survey [B199] and the reports thread ([B362]/[B366]/[B367]) that spawned the focus. Closes `webChart` 9/9.
>
> **Method**: reasoning over already-verified blocks — no new sweep. Block type: **SYNTHESIS/DESIGN** (INFER-heavy
> by design; every factual anchor is remittance to a cited evidence block).

---

## §377.1 — Why the focus exists

The `reports` deliverable ([B362]) priced its dominant cost as a **banded PSI-vs-time chart** (Y=PSI, shaded
`<12`/`>28` bands, per-crossing markers). [B366] proved buying Analytics doesn't supply it; [B367] proved the
`webChart` framework — [B362]'s named "cheaper interactive path" — doesn't render it natively either. The user then
chose to document `webChart` in full breadth. Nine gaps later, the picture is complete. `[INFER]`.

## §377.2 — Thread 1: a hand-built engine on LEGACY D3

`webChart` is **not** a wrapper over a charting library (the contrast with Analytics' c3, [B366]/[B67]). Its line
chart is a hand-built layer system ([B368]), its gauge and donut are hand-drawn ([B372]) — all on **D3 v3**:
`d3.svg.line`/`d3.behavior.zoom` ([B368]), `d3.svg.arc`/`d3.svg.line` (gauge), `d3.layout.pie` (donut) ([B372]).
D3 removed all of these in v4 (current is v7). So the whole client stack is a **modernization liability**: capable
and self-contained, but pinned to an API generation that upstream deleted a decade ago. `[INFER]` (from [B368]/[B372]).

## §377.3 — Thread 2: nothing in webChart draws an alarm limit

Across every surface the focus measured, no alarm limit is drawn as a line, band, or zone:

- **Line chart** ([B367]/[B368]): no `LimitLayer`, the `{t,v,r,s}` feed carries no limit values; the only alarm
  awareness is a per-sample **status tint** (`getStatusColoring`), which can be switched off.
- **Gauge** ([B372]): the one place a colored limit zone is idiomatic — and it has none; a single status-driven
  fill, with `min`/`max` only setting the scale range.
- **Model** ([B369]): `facetsLimitMode` can force the axis to *contain* `chartMin=12`/`chartMax=28` (limits on-axis),
  but that is axis-domain only — not a shaded band.

This is the same verdict [B366]/[B367] reached platform-wide, now confirmed inside webChart at three sites. The
`<12`/`>28` band is **intrinsically custom** in N4.14. `[INFER]` (from [B367]/[B369]/[B372]).

## §377.4 — Thread 3: open at the factory layer, closed at the draw layer

The two facts that look contradictory are true at different layers ([B375] §375.4 reconciling [B368]):

- **Open (series/factory)**: a module can register a `BIChartFactory` (marker interface + agent tagged
  `webChart:IChartFactory`) whose JS `BaseChartFactory.factory()` returns custom series — the seam Analytics uses
  for `box:BoxTable` ([B366]). **No license** is required ([B375] §375.3).
- **Closed (draw)**: those series are drawn by the hardcoded `DataLayer` branch tree (line/bar/shade/discrete,
  [B368]) over a fixed 9-layer array with no registry. You can add a **data source**, not a **draw primitive**.

**Consequence for the band**: the factory buys you data ingestion and the whole on-screen engine (axes, zoom,
resampling, time-range) for free and unlicensed — but the shaded band itself is a new draw primitive, so it still
requires the `DataLayer` fork of [B368] §368.6. Cost ordering unchanged from [B367]: **webChart-fork <
BChart-subclass < headless-from-scratch**. `[INFER]`.

## §377.5 — Thread 4: default sampling erases the very crossings the client wants marked

Even ignoring the band, faithfully *plotting* the crossings is compromised ([B369]): the default `samplingType` is
**`average`**, global per chart, and on a range wide enough to auto-sample (>2500 pts) it divides sum/count — a
brief PSI spike above 28 is **averaged out of the line** (its alarm-status *color* survives via OR-combined status,
its *height* does not). `max`/`min` preserve one extreme but not both, and there is one global setting — so a chart
cannot faithfully show `<12` dips **and** `>28` spikes at once. The CSV export inherits this: it serializes
`samplingPoints()`, not raw records ([B371]). So the "cheaper interactive path" loses crossing fidelity by default —
a second structural reason (beyond the missing band renderer) the crossing-marking is custom. `[INFER]` (from
[B369]/[B371]).

## §377.6 — Thread 5: free, read-gated, with one servlet defect

Charting is **not license-gated** ([B375]) — free with the base station. Reads run as the session user and are
gated by `OrdTarget.canRead()` ([B374]). But the gate is **incomplete**: `/data` (the bulk history feed)
hard-throws `PermissionException`, while `/schedule` and `/boxTable` call `sendError(404)` **without `return`** and
fall through to encode the body ([B374] §374.2). Whether the body actually leaks is container-commit-dependent →
**W7-G1 (requires-execution)**, the focus's one open gap. Blast radius is bounded (schedule transitions +
box/analytics tables, not the history feed). `[CERT]`-anchored (the code omission) + `[INFER]` (exploitability).

## §377.7 — The bottom line for the client deliverable

`webChart` is the **cheapest** route to the client's chart and the analysis holds ([B362]): you inherit a free,
capable, interactive engine and extend it. But three costs are now precisely characterized, none of them a config
toggle: **(a)** the band is a `DataLayer` fork (§377.4); **(b)** default sampling erases crossings, and one global
`samplingType` can't cover both bands (§377.5); **(c)** the engine is legacy-D3, so any nontrivial customization
inherits a dead API generation (§377.2). The recommendation from [B362] is unchanged and sharpened: the banded,
crossing-marked PSI chart is **custom work on every path**, and webChart lowers — but does not remove — that cost.
`[INFER]`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | webChart is hand-built on legacy D3 v3 across line/gauge/donut | [INFER] | [B368], [B372] | ✅ remittance |
| 2 | No webChart surface draws an alarm limit (line/gauge/model) | [INFER] | [B367], [B368], [B369], [B372] | ✅ remittance |
| 3 | Open at factory (unlicensed BIChartFactory) / closed at draw (DataLayer) — band still a fork | [INFER] | [B375] §375.4, [B368] | ✅ remittance |
| 4 | Default `average` sampling erases crossings; one global setting can't show both bands; CSV inherits it | [INFER] | [B369], [B371] | ✅ remittance |
| 5 | Charting free + read-gated, with the `/schedule`+`/boxTable` sendError-without-return defect (W7-G1) | [CERT]/[INFER] | [B375], [B374] §374.2 | ✅ remittance |
| 6 | For the client: webChart lowers but does not remove the custom cost of the banded, crossing-marked chart | [INFER] | §377.7 from threads 1-5 + [B362] | ✅ reasoned |

**Marker tally**: [INFER] ×5 · [CERT]/[INFER] ×1. Ratio ≈ high (expected for SYNTHESIS). Every anchor is remittance
to a driver-verified evidence block ([B368]-[B376]); this block introduces no new raw claim, it composes. No §14.

## Connections

- [B368]–[B376] — the nine evidence blocks this synthesizes (W1-W9).
- [B199] — the survey that gave the spine; this focus filled its breadth.
- [B362]/[B366]/[B367] — the reports thread; §377.7 returns the sharpened verdict to it.
- [B251]–[B259] — the classic charting focus; the platform-wide no-band pattern spans both engines.

## Gaps opened / queued

Focus `webChart` **CLOSED 9/9**. One child gap open: **W7-G1** (requires-execution — the servlet fall-through
leak). Optional future: a full-breadth focus was NOT bootstrapped for the parts that stayed remittance (the
`webEditors` base FEs the settings mount on — that is the `px-tail` P1 target). §18 retro accompanies this close.
