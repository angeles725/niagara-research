# Block 853 — HARBOR GreenMAX Px in the BROWSER: the Hx web profile (EC-Net 4.3.58.18 ≈ N4.4) — ActionBinding/set limits, Paste Special, and the working stock-Px HOA/selector recipe

> **Focus:** harbor-greenmax-lighting. **Scope:** everything learned making the redesigned GreenMAX Px work
> **in the client's web browser**, which is NOT the same engine as Workbench. The station is **Distech EC-Net
> 4.3.58.18 (≈ Niagara 4.4, build 2017-05-10)** and its browser Px renders through the **Hx profile**
> (`kitPx-wb` has the `hx/*` classes; there is **no `kitPx-ux.jar`** / no bajaux HTML5 in this install).
> This block is the deployment reality that B851 (control) and B852 (UI design) did not cover.
>
> **Evidence:**
> - `[CERT-live]` = observed in the operator's Chrome against station HM_BMS (192.168.1.150) this session.
> - `[CERT]` = verbatim from the client's REAL module `kitPx-wb.jar` v4.3.58.18 (pulled to
>   `organized/_client-ecnet-4.3.58.18-n4.4/`) + `hx-wb`/`control-rt` in `organized/` (4.14 corpus, same logic).
> - `[INFER]` = design/UX judgment.

---

## 1. The browser uses the Hx profile, and it is NOT Workbench `[CERT]`
`kitPx-wb.jar` (vendorVersion `4.3.58.18`) contains `com.tridium.kitpx.hx.BHxPxActionBinding` (+ `ActionDialog`,
`ActionConfirmation`, `InvokeActionCommand`) and **no `rc/` JS / no `kitPx-ux`**. So a Px opened in the browser
is rendered/handled by the **Hx** engine, whose binding behavior differs from the Workbench Px engine. A binding
that works in Workbench can behave differently (or silently fail) in the browser. **Always test in the browser.**

## 2. Copy-paste of wired components — `Paste Special → Keep all links` `[CERT-live]`
Plain Copy→Paste **drops links whose source is OUTSIDE the copied selection** (internal links, both ends
selected, are kept and repointed to the copy). To replicate a per-circuit block that reads shared sources
(the 5 horarios), use **Edit → Paste Special** with **"Number of copies"** (e.g. 19) and **"Keep all links"** —
external links (to the shared `Hor{n}_*`) are preserved on every copy. This made the 20-circuit build trivial.
(Corrects the mid-session claim, based on a bog-nav false-negative, that external links survive plain paste.)

## 3. Added Px widgets need their module imported `[CERT-live]`
The stock menu Px imported only `baja/bajaui/gx/kitPx`. New bindings failed to load until the module that
DEFINES each element was imported: **`ObjectToString` and `IBooleanToSimple` → `converters`**; **`Override`
→ `control`** (`javax.baja.control.util.BOverride`). Symptom: `PxDecoder … Unknown type <X> [line N]`.

## 4. The core limitation: `set(value)` with a FIXED arg fails in Hx `[CERT]`/`[CERT-live]`
`BNumericWritable.set` takes a **`BDouble`** parameter (`@NiagaraAction name="set" parameterType="BDouble"`).
A Px `ActionBinding` stores `actionArg` as a **`BString`** property. In **Workbench** the binding DECODES the
string to the param type (`decodeFromString`) → `set("2.0")` works. In **Hx**, `BHxPxValueBinding.handleAction`
does `component.invoke(action, getDefaultActionArgument(...))` where `getDefaultActionArgument` returns the raw
`actionArg` **BString** — it is **NOT decoded to `BDouble`** → type mismatch → the invoke **fails silently**.
So a button that writes a fixed number to a NumericWritable works in Workbench but does nothing in the browser.

## 5. What DOES work in Hx — no-arg actions, `set`-dialog, and the empty Override `[CERT-live]`
- **No-argument actions invoke directly.** `BooleanWritable` `active`/`inactive`/`auto` (all `flags=256=OPERATOR`)
  take no parameter → `invoke(action, null)` → instant, no dialog, no decode needed. This is why the existing
  tablero On/Off/Auto worked in the browser all along.
- **`active`/`inactive` DO carry an "Override Duration" parameter**, so a bare `ActionBinding` to them pops a
  duration dialog. Adding **`<Override name="actionArg"/>` (empty)** makes Hx invoke with the action's DEFAULT
  (Permanent) and skip the dialog — exactly what the stock tablero buttons do.
- **To ENTER a numeric value in the browser**, bind an `ActionBinding` to the point's **`/set`** action WITHOUT
  `actionArg`. Hx opens the **Set dialog** (user types the value → decoded correctly via the dialog) → works.
  Binding to a specific `.../set` shows ONLY that action; invoking the point generally showed the FULL action
  menu (Emergency Override/Auto/Override/Auto/Set).
- Property-sheet / MultiSheet writes and right-click-edit of writables also work in the browser.

## 6. The working UI recipe (stock Px, EC-Net 4.3, browser) `[CERT-live]`/`[INFER]`
**Menu horario row** (5×): editable name → `ValueBinding Horario{n}_Nombre` + `ObjectToString name="text"`;
green/red LED → `IBooleanToSimple` on `Hor{n}_Eff.out` (GREEN in shared, **RED via `module://kitPxN4svg`**);
Auto/ON/OFF → `ActionBinding Hor{n}_Eff/{auto|active|inactive}` **each with `<Override name="actionArg"/>`**;
schedule edit → `PopupBinding Horario{n}`. All **absolute** ords `station:|slot:/Iluminacion/Horarios/…`.
**Tablero interruptor** (per circuit, relative ords, view scoped to `GM02ilum`): **pill** = `IBooleanToSimple`
on `Relay[k].BO` (physical relay on/off, read-only); **LED** = `IStatusToSimple` on **`HorR{k}`** (status of the
command point we drive); **selector** = `Label` (`ValueBinding SelR{k}` + `ObjectToString name="text"`) with an
overlay `ImageButton` → `ActionBinding SelR{k}/set` (no actionArg → Set dialog); manual On/Off/Auto + schedule
icon removed.

## 7. HOA control shape forced by §4-5 `[CERT-live]`
Because a fixed-value numeric write does not work in the browser, the per-horario HOA had to be a
**`BooleanWritable` (`Hor{n}_Eff`)** driven by `active`/`inactive`/`auto`, NOT the `BooleanSelect`+`Hor{n}_Mode`
(numeric select) of the B851 pilot. To avoid re-linking the 20 circuits, a **passthrough** was used: keep the
existing `Hor{n}_HOA` BooleanSelect, feed `Horario{n}.out → Hor{n}_Eff.in10`, re-point `Hor{n}_HOA.inA ←
Hor{n}_Eff.out`, and fix `Hor{n}_Mode = 1` so `Hor{n}_HOA` always mirrors `Hor{n}_Eff`. Circuits keep reading
`Hor{n}_HOA` untouched. The per-circuit selector (`SelR{k}`) stays NumericWritable (entered via the Set dialog).

---

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | Client is EC-Net 4.3.58.18; browser Px = Hx (kitPx-wb has hx/*, no kitPx-ux) | [CERT] | module.xml vendorVersion; extracted jar contents |
| 2 | Paste Special "Keep all links" preserves external-source links; plain paste drops them | [CERT-live] | operator paste of SchedMux+SelR ×19 |
| 3 | Added widgets need their module imported (converters, control) or PxDecoder errors | [CERT-live] | "Unknown type ObjectToString/Override" then fixed by import |
| 4 | `set` takes BDouble; Hx passes raw BString actionArg → fails; WB decodes | [CERT]/[CERT-live] | BNumericWritable set param; BHxPxValueBinding.handleAction; works WB not browser |
| 5 | No-arg active/inactive/auto invoke directly in Hx; empty `<Override actionArg/>` skips the duration dialog | [CERT-live] | tablero buttons work; the Override Duration popup then suppressed |
| 6 | ActionBinding to `/set` (no actionArg) opens the Set dialog (typed value works) | [CERT-live] | operator saw only Set + entered value |
| 7 | Working recipe: menu Hor{n}_Eff+active/inactive/auto; tablero LED=HorR{k} status, pill=Relay[k].BO value, selector=/set | [CERT-live]/[INFER] | applied on GM02 + menu, browser |

**Tally:** 7 claims — 5 [CERT-live] (browser), 1 [CERT] (client module code), 1 mixed. The deployment behavior
is empirical against the client's own station and module version.

## Connections
- **B851** — the control chain + pilot (numeric-Mode HOA); this block supersedes its HOA shape for the browser.
- **B852** — the UI design; this block is its browser-reality correction (Hx, no-arg buttons, /set selector).
- **B724 / web-hmi** — the WEB-HMI panel is a browser target, so the Hx behavior here applies to the panel too.
- **chihuahua / DashboardPan** — custom modules do numeric HOA in the browser via their OWN JS+servlet, which is
  exactly what stock Px + Hx cannot do (why the custom route exists).

## Open gaps (RESEARCH-STATE-harbor-greenmax-lighting) — for a future session
- **B853-G1** — confirm the final GM02 works fully in the browser (menu buttons no-popup, selector Set dialog, LEDs) end-to-end.
- **B853-G2** — a cleaner selector than the Set dialog: **EnumWritable `SelR{k}`** with horario-name range + a bound dropdown; check the Hx dropdown-write path.
- **B853-G3** — passthrough cleanup: re-point circuits to `Hor{n}_Eff` and remove the redundant `Hor{n}_HOA` BooleanSelect + `Hor{n}_Mode` for a clean final design.
- **B853-G4** — Hx vs bajaux web-profile differences across N4 versions (client 4.3 vs corpus 4.14): a broader reference of what stock Px bindings do/don't do in each browser engine.
- **B853-G5** — custom-module option: cost/feasibility of a GreenMAX dashboard module (JS+servlet, DashboardPan pattern) if the client wants slick numeric writes without the Set dialog.
