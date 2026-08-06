# Niagara N4 — Bloque 373: `webChart` (W4) — the command/interaction layer: a `webchart:*` event bus, six `bajaux/commands/Command`s (addSeries/Settings/Stop/LockAxis/SetAxisValues/DialogWizard), context menus on series and axes, and a drag-drop path where a dropped ORD/point/`.chart` becomes a series via `seriesFactory.make`

> **Focus**: `webChart`, gap **W4** (command palette + interactions). **Remittance**: [B199] §199.5 surveyed
> zoom/time-range/export/sampling; [B368] the ZoomLayer. W4 maps the commands, menus, drag, and the event bus.
>
> Subject: `webChart-ux` `rc/command/*`, `rc/menu/*`, `rc/dragUtil.js`, `rc/chartEvents.js`.
> **Method**: delegated sweep + inline verify of 3 load-bearing citations (event bus, addSeries→seriesFactory,
> drag→series). Block type: **EVIDENCE**.

---

## §373.1 — `chartEvents.js`: the internal `webchart:*` event bus `[CERT]`

A single exports object of ~20 string constants `[CERT]` `chartEvents.js:13-86`:
`REDRAW_REQUEST_EVENT`='webchart:redrawrequest', `SETTINGS_CHANGED`='webchart:settings:changed',
`FILE_MODIFY_EVENT`='webchart:file:modify' (the "chart dirtied" signal), plus series enable/focus/add, model
live/load/stopped, `VALUE_SCALE_CHANGED`, `TIME_RANGE_CHANGED`, `DISPLAY_ERROR`, `MODEL_OVER_MAX_CAPACITY`. This is
the decoupling bus between model, layers, and commands.

## §373.2 — Six `bajaux` commands; none is a ToggleCommand `[CERT]`

All extend `bajaux/commands/Command` (the widget's own status/sampling/delta/live toggles are separate) `[CERT]`:

| Command | Action / target | Mutates |
|---|---|---|
| `addSeries` | OkCancel NavTree dialog → `model.addSeries` → `model.load` | model (adds series) |
| `Settings` | tabbed Series/Axis/Layers/Sampling dialog; OK → `tabbedEditor.save` + `reloadAll` + `SETTINGS_CHANGED` | settings/.chart |
| `Stop` | toggles `model.stop()` ↔ `model.reloadAll()` | model load state |
| `LockAxis` | flips `setLocked(!isLocked())` + re-stretch; fires `VALUE_SCALE_CHANGED` | a BaseScale ([B369]) |
| `SetAxisValues` | (extends `DialogWizardCommand`) per-scale min/max/name editor → `stretchDomain`/`manualZoom` | scale settings |
| `DialogWizard` | abstract wizard engine (`resolveTab→dialogCycle`); parent of SetAxisValues | — |

`[CERT]` (`addSeriesCommand.js:132-153`, `SettingsCommand.js:22,53-58`, `StopCommand.js:30-34`,
`LockAxisCommand.js:27-40`, `SetAxisValuesCommand.js:68-94`, `DialogWizardCommand.js:45-75`).

## §373.3 — Context menus on series and axes `[CERT]`

`contextMenuUtil.registerContextMenu` wires the trigger (d3 `contextmenu`, JavaFX mouse-button emulation,
long-press) `[CERT]` `contextMenuUtil.js:239-260`. Right-click a **series** → `seriesContextMenu` (toggle-enable,
SetAxisValues, removeFromChart, removeOthers, setAsPrimary) `:94-190`; right-click an **axis/unit label** →
`scaleContextMenu` (SetAxisValues, LockAxis, setAsPrimary, remove-all-on-scale) `:25-85`. `MenuWidget` is a
`GridEditor` closed by an outside-click capturing listener `:59-79`. `[CERT]`.

## §373.4 — Drag-drop → series via `seriesFactory.make` `[CERT]`

`dragUtil.armDrop` binds drop/dragover, racing `getNavNodes` (clipboard `niagara/navnodes`) and
`getChartFileSeries` (a dropped `.chart`'s `model.series`), normalizing to an ORD array via `getOrdsFromDrag`
`[CERT]` `dragUtil.js:104-188`. `ChartWidget.drop` then calls `model.addSeries(subscriber, ords)` → `model.load`
`[CERT]` `ChartWidget.js:1429-1437`, and `BaseModel.addSeries` calls **`seriesFactory.make(that, subscriber,
seriesParams)`** `[CERT]` `BaseModel.js:749-752`. So dropping an ORD / point / `.chart` file becomes a series
through the same chain-of-responsibility factory ([B199] §199.3). It is add-only — reordering is via the
context-menu `setAsPrimary`, not drag. No keyboard shortcuts exist (measured absence); commands register through the
widget-bar/grid, not a keymap.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `chartEvents.js` defines ~20 `webchart:*` bus constants (redraw/settings/file-modify/series/model/scale) | [CERT] | `chartEvents.js:13-86` | ✅ read |
| 2 | Six commands extend `bajaux/commands/Command`; roles/mutations as tabled | [CERT] | the six `command/*.js` cited | ✅ sweep + spot-read |
| 3 | Context menus on series + axis via `contextMenuUtil.registerContextMenu` | [CERT] | `contextMenuUtil.js:25-85,94-190,239-260` | ✅ sweep-cited |
| 4 | Drag-drop ORD/point/.chart → `model.addSeries` → `seriesFactory.make` | [CERT] | `dragUtil.js:104-188`; `ChartWidget.js:1429-1437`; `BaseModel.js:749-752` | ✅ read |
| 5 | No keyboard shortcuts; commands via widget-bar/grid | [CERT] | measured absence + `ChartWidget.js:268` | ✅ sweep-cited |

**Marker tally**: [CERT] ×5. Ratio ≈ 0.0. Block type = **EVIDENCE**. Load-bearing re-resolved to disk: the event
bus (claim 1), the addSeries→seriesFactory chain (claim 4 via BaseModel.js:749-752), the drag→drop path
(ChartWidget.js:1429-1437). No §14.

## Connections

- [B199] §199.3 — the `seriesFactory` chain the drag/add commands feed.
- [B369] — the BaseScale the LockAxis/SetAxisValues commands mutate.
- [B370] — the SettingsCommand opens the SimplePropertySheet settings surface.

## Gaps opened / queued

**W4 closed.** No child gap.
