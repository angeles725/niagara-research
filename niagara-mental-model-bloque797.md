# B797 · `verify-module.sh --plano`: pinning the four-value agreement check (issue #47 spec) `[CERT]`

> Makes issue **#47** spec-ready: EXACTLY what `verify-module.sh --plano <html>` must check, derived from the
> REAL DashboardPan-ux SPA (read-only). The `types/dashboard.md` "Plano overlay" rule says the zone overlay
> aligns only when FOUR values agree; this block pins each to a `file:line` in our production SPA, states the
> pass condition as a formula, gives a PASS excerpt + a FAIL mutation, and records that the real SPA currently
> FAILS the check (a stale ratio masked by `aspect-ratio:auto`) — the "before" evidence for issue #49.
>
> **Source (read-only)**: `Cliente/Leon-Guanjuato/Dashboard/DashboardPan/DashboardPan-ux/src/rc/index.html`
> (2300 lines) + kit `build-n4-module-kit/types/dashboard.md` "Plano overlay". Driver-verified: the `#planoImg`
> base64 intrinsic size was decoded from the PNG header (not taken on faith). Markers: `[CERT]` value read at
> `file:line` / decoded · `[INFER]` deduction.
>
> **Type:** `standard`. Connects [Block 793]/[Block 794] (verify-module gate + scaffold), the DashboardPan-ux
> render retros cited in `types/dashboard.md`.

## 797.1 — The four plano values in the real SPA (each cited) `[CERT]`

| # | Value | Where (`index.html`) | Reading | Ratio (W/H) |
|---|---|---|---|---|
| 1 | `#planoImg` intrinsic size | `:544` `<image id="planoImg" xlink:href="data:image/png;base64,…">` | PNG header decode = **1248×891** | 1.40067 |
| 2 | `IMG_W`/`IMG_H` constants | `:791` `const IMG_W = 1248, IMG_H = 891` | 1248/891 | 1.40067 |
| 3 | zones `viewBox` | `:542` `<svg class="zonas" id="zonas" … viewBox="0 0 1248 891" preserveAspectRatio="xMidYMid meet">` | 1248/891 | 1.40067 |
| 4 | frame `aspect-ratio` | `:84` `.frame { … aspect-ratio:1247/771; … }` | 1247/771 — **STALE** (the orphaned `plano.png` dims, not the base64) | 1.61738 |

Values 1–3 agree at **1.40067**; value 4 (`.frame` = 1247/771) **DISAGREES** and is only MASKED by a
higher-specificity `#frame { aspect-ratio:auto; }` (`:96`, the i.MX8M panel fix). This is exactly the
"leftover `.frame{aspect-ratio:1247/771}` masked by a higher-specificity `#frame` rule" the
`types/dashboard.md` rule warns silently returns the offset if the id is renamed.

## 797.2 — What `--plano` must check — the pass condition as a formula `[CERT]`

Let, for the plano frame in the HTML:
- `Rc = IMG_W / IMG_H`  (from `const IMG_W = <a>, IMG_H = <b>`)
- `Rv = vbW / vbH`  (from the zones `<svg … viewBox="0 0 <vbW> <vbH>">`)
- `Ri = imgW / imgH`  (the `#planoImg` `<image>`/`<img>` INTRINSIC size — decode the `data:` URI header, or the on-disk src's natural size)
- `A  = { every CSS aspect-ratio: n/m declaration in the file whose value is NUMERIC (not auto) }`

**PASS iff**  `Rc == Rv == Ri`  **AND**  `∀ r ∈ A : r == Rc`
(compare ratios as exact cross-multiplication: `w1*h2 == w2*h1`, never float ==; `aspect-ratio:auto` is the
deliberate panel fix and is EXEMPT — it is not in `A`).

Equivalent operational check: `Rc == Rv == Ri`, and every numeric `aspect-ratio` in the file equals `Rc`
(ideally the stale numeric one is DELETED so `A` is empty and `#frame:auto` + the SVG `viewBox` carry the ratio —
per the rule "the fix for a stale one is to DELETE it, never to shadow it").

**Does it reduce to "all `aspect-ratio` declarations in the file are equal"? NO** — two reasons, both real in our
SPA: (a) `aspect-ratio:auto` (`:96`, `:105`) is deliberate (the WebView ignores CSS `aspect-ratio`) and must be
exempt, so an intra-`aspect-ratio` equality test would false-FAIL the panel fix; (b) equality must be against
`IMG_W/IMG_H` AND the `viewBox` AND the image intrinsic — a cross-source agreement — not merely among the
`aspect-ratio` declarations. The core defect is a NUMERIC `aspect-ratio` that disagrees with `IMG_W/IMG_H`.

## 797.3 — PASS fixture excerpt + FAIL mutation `[CERT]`

**PASS** (all four agree; one numeric aspect-ratio, or none + auto):
```html
<style> .frame { aspect-ratio:1248/891; } </style>
<svg class="zonas" viewBox="0 0 1248 891">…</svg>
<image id="planoImg" href="data:image/png;base64,…">   <!-- intrinsic 1248x891 -->
<script>const IMG_W = 1248, IMG_H = 891;</script>
```
`Rc=Rv=Ri=1248/891`, `A={1248/891}`, every `r==Rc` → PASS.

**FAIL** (the lead's mutation — two disagreeing numeric aspect-ratio declarations):
```html
<style> .frame { aspect-ratio:1248/891; } .frameLegacy { aspect-ratio:1247/771; } </style>
```
`A={1248/891, 1247/771}`; `1247/771 != Rc` → FAIL, naming the offending declaration. (This is also the REAL
state of our SPA at `:84` vs `IMG_W/IMG_H` — a numeric `1247/771` ≠ `1248/891`, masked but present → the real
DashboardPan-ux `index.html` FAILS `--plano` today.)

## 797.4 — Kit implication → `verify-module.sh --plano` (issue #47) `[CERT-grounded]`
- Add `--plano <html>`: parse `IMG_W/IMG_H`, the zones `viewBox`, the `#planoImg` intrinsic size (decode the
  `data:` URI PNG/JPEG header — the plano is base64, NOT the orphaned on-disk `plano.png`), and every numeric CSS
  `aspect-ratio`. Emit `PASS|FAIL plano <html> <detail>`; FAIL when `Rc/Rv/Ri` disagree or any numeric
  `aspect-ratio != Rc`; treat `aspect-ratio:auto` as exempt.
- Fixture pair for the RED→GREEN test: §797.3 PASS excerpt (GREEN) + the two-disagreeing-declarations mutation
  (RED). Anchors issue #47's biting test.
- **Before-picture for #49**: run against the real `DashboardPan-ux/src/rc/index.html` → FAIL (`.frame:84`
  1247/771 ≠ IMG_W/IMG_H 1248/891). The fix is to DELETE `.frame`'s numeric `aspect-ratio` (`:84`), leaving
  `#frame:auto` + the SVG `viewBox` — not to shadow it.

## 797.5 — Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | The four values: 1248×891 (image, IMG_W/IMG_H, viewBox) vs 1247/771 (frame) | `[CERT]` | index.html :544/:791/:542/:84 | Y — :544 base64 header decoded 1248×891 |
| 2 | `.frame` 1247/771 is stale, masked by `#frame:auto` (:96) | `[CERT]` | index.html :84,:96 | Y |
| 3 | Pass = `Rc==Rv==Ri` and every numeric aspect-ratio == Rc; auto exempt | `[CERT]` | derived from the rule + the real file | Y |
| 4 | Does NOT reduce to "all aspect-ratio equal" (auto exempt + cross-source) | `[CERT]` | §797.2 (a)(b) | Y |
| 5 | Real DashboardPan-ux index.html FAILS `--plano` today | `[CERT]` | 1247/771 present at :84 ≠ 1248/891 | Y |

**Tally:** `[CERT]` ×5. No unmarked claims.

## 797.6 — Connections & open gaps
- [Block 793]/[Block 794] (the verify-module gate + scaffold this check joins), `types/dashboard.md` "Plano overlay".
- No open gap: the check is fully specified. Implementation lands in campaign-7's `verify-module.sh --plano` (issue #47);
  the real-SPA fix (delete `.frame:84`) is issue #49's punch-list item.
