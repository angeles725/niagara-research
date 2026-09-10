# Block 852 — HARBOR GreenMAX lighting UI (Px) rework: existing structure, approved design, point spec, and preview method (advances B851-G2)

> **Focus:** harbor-greenmax-lighting. **Scope:** the DASHBOARD/Px side of the Route B redesign — how the
> current GreenMAX screens are built, the client-approved new design for the menu and the tablero interruptor,
> the backing points it needs, and the offline preview method used to validate it. Advances B851-G2.
>
> **Evidence:**
> - `[CERT]` = verbatim from the real `.px` files under
>   `clients/distech-merida-harbor/HM_BMS_backup_08_09_2026/shared/px/`, and from decompiled converter/control
>   classes in `organized/`.
> - `[CERT-live]` = the client's stated UI requirements confirmed this session while reviewing rendered previews.
> - `[INFER]` = the campaign application plan / effort.
>
> Previews (offline, no station touched) rendered with `tools/px-render.py`; client approved both.

---

## 1. How the current GreenMAX screens are built `[CERT]`

Three levels, all under `shared/px/`:
- **Menu** = `GreenMAX Iluminacion.px` (CanvasPane `viewSize=1370x780`). 16 `ImageButton`s, each an **absolute**
  hyperlink `station:|slot:/Drivers/NiagaraNetwork/HM_Central/points/GMxxilum`. Columns at x=210 (Sót2),
  420 (Sót1), 610 (PB), 810 (Nivel1), 1010 (Nivel2). No schedule/horario element; free space on the right.
- **Tableros** = `GreenMAX02 TabAL6.px` (+ 15 more). Per circuit k (20 in GM02), ~7 flat sibling widgets, all
  **relative** ORDs (resolved against the device the view is attached to):
  - On/Off pill: `Picture` + `ValueBinding ord="slot:Relay$5b{k}$5d$2eBO"` + `IBooleanToSimple` (On.png/Off.png).
  - Status LED: `Picture` + same ord + `IStatusToSimple` (ok=green, fault=orange, down=yellow, overridden=purple, alarm=red).
  - Auto button: `ImageButton` + `ActionBinding ord="slot:Relay[k].BO/auto"`.
  - Invisible ON/OFF taps: `ImageButton` + `ActionBinding ord=".../active"` / `".../inactive"` + `<Override name="actionArg"/>`.
  - Schedule icon: `Picture` + `PopupBinding ord="slot:R{k}"` (opens the per-circuit schedule `Rk`).
  - Descriptions: two `Label` + `ValueBinding ord="slot:tabR{k}"` / `"slot:labelR{k}"` (editable StringWritables).
- **No `PxInclude` in any GreenMAX/plantilla file** (only `HVAC Sot02.px` uses one anywhere). `plantilla
  greenmax.px` is a **starting-copy template** (bare relative `Relay[k].BO` grid), NOT a runtime include.
  Reuse works only because every ORD is relative, so an identical hand-copied grid resolves per device.

## 2. Converters available for the LED `[CERT]`

`organized/converters`: `INumericToSimple` (BINumericToSimple), `IEnumToSimple`, `IStatusToSimple`,
`IBooleanToSimple`, plus `BNumericToSimpleMap`/`BEnumToSimpleMap`. So a numeric/enum value can map to an image
set. LED SVGs: `shared/px/kitPxN4svg/.../LEDs/` has PURPLE/GREEN/ORANGE/YELLOW/GRAY/BROWN; **RED is NOT in
shared** — it lives in the module (`organized/kitPxN4svg/.../MISC_UI_LED_RED.svg`, referenced `module://`).

## 3. Client-approved design `[CERT-live]`

**Menu — 5 horario rows on the right** (x≈1160, fit within the 1370 canvas). Each row:
- editable name (StringWritable `Horario{n}_Nombre`), a **LED**, three buttons **Auto / ON / OFF**, and a
  schedule popup icon (to `Horario{n}`).
- **LED colors (client rule, revised): green = on, red = off — two colors only.** The LED reflects the
  horario's effective value, with no mode/override coloring, so it binds DIRECTLY (no helper point, §4).

**Tablero interruptor — simplified.** Per circuit: **REMOVE** the On/Off pill, the Auto button, the invisible
active/inactive taps, and the schedule icon (5 widgets/circuit = 100 removed in GM02); **KEEP** the status
LED; **ADD** a horario selector bound to `SelR{k}`. Manual control is gone from the circuit (it moved to the
horario level). Description labels stay.

## 4. The LED — no helper (green/red only) `[CERT]`/`[INFER]`

The client dropped the purple/override coloring: the menu horario LED is just **green = on, red = off**. So it
binds directly to the effective value, no computed point:
```
<ValueBinding ord="slot:Hor{n}_HOA">
  <IBooleanToSimple name="image">
    <Image name="trueValue"  value="module://kitPxN4svg/Misc/Misc_UI_Elements/LEDs/MISC_UI_LED_GREEN.svg"/>
    <Image name="falseValue" value="module://kitPxN4svg/Misc/Misc_UI_Elements/LEDs/MISC_UI_LED_RED.svg"/>
  </IBooleanToSimple>
</ValueBinding>
```
`Hor{n}_HOA.out` is the effective horario value (post Auto/ON/OFF). GREEN is in shared; RED only in the module
(`module://`). This removed the earlier `Hor{n}_LED` helper entirely. `Hor{n}_Mode` (1/2/3) is still used —
only by the three Auto/ON/OFF buttons — not by the LED.

## 5. Backing points to create `[INFER]`

- Per horario (×5, once): `Hor{n}_Mode` (**NumericWritable** 1/2/3, written by the 3 buttons via `set`+actionArg),
  `Horario{n}_Nombre` (**StringWritable**). Reuse pilot's `Horario{n}` + `Hor{n}_HOA` (the LED binds its `.out`
  directly, §4). **Replace** the pilot's NumericConst `Hor{n}_Mode` with a NumericWritable. No `Hor{n}_LED` helper.
- Per circuit (×288 in scope): `SelR{k}` (**NumericWritable** 1..5, written by the tablero selector). Replace
  the pilot's NumericConst `SelR1` with a writable.

## 6. Application plan `[INFER]`

- **Menu:** paste the 5-row Horarios block with **ABSOLUTE** ORDs (`station:|slot:/…/Iluminacion/Horarios/…`)
  — the menu is not device-scoped.
- **Tablero:** per circuit remove pill/schedule/manual-buttons, keep the LED, add the selector (**relative**
  `slot:SelR{k}`).
- **Reuse:** convert the circuit grid to a real **PxInclude** so one edit propagates to all 13 tableros
  (today it is hand-copied). Build GM02 as the template, verify live, replicate.
- Order: finish GM02 control points → apply GM02 Px (menu + tablero) → verify live → time it → replicate 12.

## 7. Preview method `[CERT]`

`tools/px-render.py <px> --shared <backup shared> [--organized organized] --out <html>` renders a Px to
self-contained HTML (embeds `file:^` assets; `--organized` resolves `module://`, needed for the RED LED).
Two previews generated and client-approved: the menu with the 5 horario rows, and the GM02 tablero with the
manual widgets removed + the selector added. Bound values render as demo/placeholder (no live station).

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | Menu = 16 absolute-hyperlink ImageButtons, 1370x750 canvas, no horario element, free right space | [CERT] | GreenMAX Iluminacion.px |
| 2 | Tablero circuit = pill+LED+Auto+taps+schedule+2 desc, all relative `slot:` ORDs | [CERT] | GreenMAX02 TabAL6.px lines 57–96 |
| 3 | No PxInclude in GreenMAX/plantilla files; plantilla is a starting copy, reuse via relative ORDs | [CERT] | px dir grep; plantilla greenmax.px |
| 4 | INumericToSimple/IEnumToSimple/IStatusToSimple exist; RED LED svg only in module, not shared | [CERT] | organized/converters; shared LED folder listing |
| 5 | Client LED rule (revised): green=on, red=off only — LED binds IBooleanToSimple on Hor{n}_HOA.out, no helper | [CERT-live] | client instruction this session |
| 6 | Menu row = name(StringWritable)+LED(Hor{n}_LED)+3 buttons(Hor{n}_Mode)+schedule popup | [CERT-live]/[INFER] | approved design |
| 7 | Tablero interruptor: remove pill/Auto/taps/schedule (100 in GM02), keep LED, add SelR{k} selector | [CERT-live]/[INFER] | approved design; px-render transform removed 100 widgets |
| 8 | Backing points: SelR{k}/Hor{n}_Mode NumericWritable, Hor{n}_LED helper, Horario{n}_Nombre StringWritable | [INFER] | design; pilot consts become writables |
| 9 | Previews rendered offline with px-render.py and approved | [CERT] | menu_horarios_v2.html, tablero_gm02_nuevo.html |

**Tally:** 9 claims — 4 [CERT] (px + code facts), 1 [CERT-live] (client LED rule), 2 mixed, 2 [INFER]
(point spec + plan). The existing structure and the converters are verified; the new design is client-approved
and its logic rests on verified primitives.

## Connections
- **B851** — the control-chain pilot; this block is its UI half (advances B851-G2).
- **B845** — dashboard feasibility (Px vs SPA); this block executes the Px route.
- **B846** — the `tabR{k}`/`labelR{k}` description points reused as editable-label precedent.
- **B293** — PxInclude as the official Tridium reuse pattern (the recommended §6 conversion).

## Open gaps (RESEARCH-STATE-harbor-greenmax-lighting)
- **B851-G2** — still open: apply the Px live (menu + GM02 tablero as PxInclude), create the backing points, verify, replicate 12.
- **B852-G1** — confirm the exact write action/priority for `Hor{n}_Mode/set` and `SelR{k}` from the buttons/selector, live in Workbench (same way the pilot gate was confirmed).
