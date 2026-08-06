# Niagara N4 — Bloque 370: `webChart` (W5) — settings are five `baja.Component` groups (chart/layers/sampling + per-series + per-scale) rendered by a `SimplePropertySheet` over webEditors; every enum is a `choiceUtil` DynamicEnum wired to `FrozenEnumEditor`, and the field-editor catalog is 7 thin subclasses of webEditors base FEs

> **Focus**: `webChart`, gap **W5** (field editors + settings structure). [B199] §199.4 established the FEs mount on
> `webEditors`; [B369] read two enums (`facetsLimitMode`/`samplingType`) — this maps the REST of the settings model
> and the FE catalog. **Remittance**, not re-derived: those two enums, the webEditors mount fact.
>
> Subject: `webChart-ux` `rc/ChartSettings.js`, `rc/choiceUtil.js`, `rc/fe/**`.
> **Method**: delegated `general-purpose` sweep (10 tool-uses) + inline driver verify of the 3 load-bearing
> citations (the 5-group structure, the `addEnumChoice→FrozenEnumEditor` wiring, the ChartTypeEditor toggles).
> Block type: **EVIDENCE**.

---

## §370.1 — Settings = five `baja.Component` groups `[CERT]`

`ChartSettings` builds five setting containers `[CERT]` `ChartSettings.js:60-64`: `$chartSettings` (global),
`$layersSettings` (overlays), `$samplingSettings`, `$seriesListSettings` (one child comp per series), and
`$valueScaleListSettings` (one child comp per scale). Populated via `choiceUtil.add*Choice` `:93-126`:

- **global** (`:102-112`): `yAxisOrient`, `dataZoom` (primary/all), `showGrid`, `chartCursor`, `facetsLimitMode`
  ([B369]), `showStartTrendGaps`/`showDataGaps` (DATA_GAP_TAGS), `showInterpolateTail`, `delta`.
- **layers** (`:95-100`): `dataPopup`, `dataMouseover`, `statusColoring` (the [B368]/[B369] status-tint toggle).
- **sampling** (`:114-125`): `autoSampling`, `sampling`, `samplingType` ([B369]), `desiredSamplingPeriod`
  (RelTime), `sampleSize`.
- **per-series** (`:841-898`, keyed by escaped ORD): `color`, `chartType`, `enabled`.
- **per-scale** (`:931-955`, keyed by uniqueName): `displayName`, `locked`, `max`/`min` (hidden if discrete).

Persistence: `saveToJson`/`loadFromJson` write/read `settings.{chart,layers,sampling,scales}`, filtering
`OBSOLETE_PROPERTIES=['backgroundAreaColor']` `[CERT]` `:15,184-236`.

## §370.2 — `choiceUtil`: every enum is a DynamicEnum bound to webEditors' `FrozenEnumEditor` `[CERT]`

`addEnumChoice(comp, prop, tags, selected)` makes a DynamicEnum and attaches the facet
`uxFieldEditor:'nmodule/webEditors/rc/fe/baja/FrozenEnumEditor'` before `addChoice` `[CERT]`
`choiceUtil.js:70-77`. `getChoice` returns an Enum's `.getTag()` (the string) or a Boolean's value `:18-36`. So
every enum setting renders as a webEditors dropdown — the settings UI is **declarative facets over webEditors**, no
custom widgets.

## §370.3 — The FE catalog: 7 thin subclasses of webEditors base FEs `[CERT]`

All extend a `nmodule/webEditors` base and are wired by `uxFieldEditor` facets:

| Editor | Extends (webEditors) | Edits |
|---|---|---|
| `ChartTypeEditor` | `BaseEditor` | series `chartType` — 4 exclusive toggles line/discreteLine/shade/bar `[CERT]` `:51-54` |
| `SeriesEditor` | `ComplexCompositeEditor` | a series comp (slots color+chartType) |
| `ColorEditor` (**@deprecated 4.12**) | `BaseEditor` | `gx:Color`, opens `colorChooser` |
| `colorChooser` | (dialog, not FE) | 8×8 fixed swatch palette via `dialogs.showOkCancel` |
| `SamplingPeriodEditor` | `OverrideRelTimeEditor` | `desiredSamplingPeriod` (bestFit/1m/…/custom) |
| `StartEndTimeRangeEditor` | `ComplexCompositeEditor` | the time range; self-registers for `webChart:WebChartTimeRange` |
| `SimplePropertySheet` | `wb/PropertySheet` (nested, no header/controls) | the settings surface itself |

`[CERT]` (each `:10-30`). **`SimplePropertySheet` is the settings surface** `:21-27`: each group Component renders
as a nested property sheet whose rows delegate to the per-slot `uxFieldEditor`s; the only true modal is
`colorChooser`. Confirms [B199] §199.4 in breadth.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Settings = 5 baja.Component groups (chart/layers/sampling/series-list/scale-list) | [CERT] | `ChartSettings.js:60-64` | ✅ read |
| 2 | Global/layers/sampling setting keys as listed; per-series color/chartType/enabled; per-scale locked/min/max | [CERT] | `ChartSettings.js:95-125,841-955` | ✅ sweep + spot-read |
| 3 | `addEnumChoice` binds every enum to webEditors `FrozenEnumEditor` | [CERT] | `choiceUtil.js:70-77` | ✅ read |
| 4 | 7 FEs subclass webEditors base FEs; ChartTypeEditor = 4 exclusive toggles line/discreteLine/shade/bar | [CERT] | `fe/series/ChartTypeEditor.js:20-54` | ✅ read |
| 5 | SimplePropertySheet (nested wb/PropertySheet) is the settings surface | [CERT] | `fe/SimplePropertySheet.js:21-27` | ✅ sweep-cited |

**Marker tally**: [CERT] ×5. Ratio ≈ 0.0 (pure evidence). Block type = **EVIDENCE**. Load-bearing re-resolved to
disk: the 5-group structure (claim 1), the enum→FrozenEnumEditor wiring (claim 3), the ChartTypeEditor toggles
(claim 4). No §14. Delegated sweep tier: `general-purpose`.

## Connections

- [B369] — the `facetsLimitMode`/`samplingType` enums live in these groups; W5 maps the rest.
- [B368] — `statusColoring` (layers group) is the toggle for the per-sample tint the render engine applies.
- [B199] §199.4 — the webEditors mount, now mapped in breadth.
- Forward: W4 (the SettingsCommand that opens this sheet), W3 (chartType values the ChartTypeEditor sets).

## Gaps opened / queued

**W5 closed.** No child gap. Focus `webChart` progressing. NEXT among remaining W3/W4/W6/W7/W8/W9.
