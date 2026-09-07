# Retro — custom-login focus (B834) + a real Domo `DomoUserCustom` deploy on PRUEBAS

Date: 2026-09-07
Author: Opus 4.8 (research session, with Cristian)
Scope: new focus `custom-login` (B834 — the four ways to build a custom Niagara login: branding
slots, native `BLoginTemplate`, Domo turn-key, Domo `DomoUserCustom`) AND an end-to-end real
deployment: built a PANCCADIA login theme and installed NiagaraMods Domo on the PRUEBAS station
(N4.14 Beta), confirmed working by the operator.

## Why

Operator provided `nmxdomo-b4.zip` + README and asked how to build a custom login. The corpus had
the auth foundation (B9 login form, B11 slot customization, B18 CSRF) but **not** the actual
extension point (`javax.baja.web.BLoginTemplate` / the `WebService.loginTemplate` `BTypeSpec`) nor
Domo. B834 opens that; then it became a live build+deploy task.

## What got produced

- **B834** — the mental model: single control point `WebService.loginTemplate`; native
  `BLoginTemplate` (abstract `write()` + the `/j_security_check` form contract) `[CERT]`; Domo's
  three registered `web:LoginTemplate` types; the four paths (A branding slots · B native module ·
  C Domo turn-key · D `DomoUserCustom` HTML/HBS/JS/CSS in `file:^domo/`, no Java).
- **The PANCCADIA theme** (scratchpad + packaged to `Downloads\domo-panccadia\`): a `DomoUserCustom`
  theme — centered floating card over a full-screen bread photo, PANCCADIA + Rotzinger logos,
  embedded fonts, tuned for the Honeywell WEB-HMI10/CF (1280×800, Chromium/Linux kiosk).
- **A local Domo preview server** (`scratchpad/panccadia/preview-server.py`) that emulates the Domo
  runtime (`/login?domo/...`, a mock `$domo`, Handlebars vars) so the login renders on localhost
  exactly as on the station — iterated by headless-Chrome screenshots without any build/deploy.

## Lessons

1. **`file:^` login resources are locked down since 4.10u3** `[CERT-doc]` (Tridium *Customizing the
   login screen*). Domo survives on 4.14 because `BDomoLoginTemplate` (trusted login code) reads
   `file:^domo/` itself and re-serves at `login?domo/...` — the browser never fetches `file:^`
   directly. Operator's live use on 4.14 corroborates.
2. **Module load needs a restart, and the dropdown proves it.** After copying `nmxdomo-rt.jar` to
   the install `modules/`, the WebService **Login Template** dropdown showed only `web` until the
   station/daemon was restarted — the dropdown lists modules the station has ALREADY loaded. Not a
   trust problem: the jar is signed by GBO Digital via **DigiCert Trusted Root G4**, which Niagara
   trusts by default (verified with `keytool`).
3. **HMI kiosk = embed the fonts.** Chromium/Linux on the panel has no Palatino/Cormorant and may
   have no internet/CSP for Google Fonts. Self-hosting the woff2 in `domo/fonts/` makes the panel
   render identical to the preview with zero CSP edits. Same reason the casino theme fell back to
   system serifs.
4. **A wide brand photo fights a tall side panel.** The bread image (horizontal) only composed well
   once switched from a left split to a full-screen background + floating card; and the printed
   wordmark had to be cropped out (it was the blurriest region and redundant with the card logo).
5. **`hideEula` would hide our footer.** This theme puts the Rotzinger integrator credit inside
   `{{#unless hideEula}}` — a documented trap for whoever configures the WebService.

## Proposed deltas (for human review)

1. **Consider a reusable `domo-login-preview.py` toolbelt entry** (sibling of `dashboard-preview.py`)
   that serves a `DomoUserCustom` theme with the Domo runtime mock — this session hand-rolled it and
   it is reusable for every future login theme.
2. **Fold the operator's real casino + PANCCADIA themes as `[CERT-live]` worked examples** under the
   custom-login focus (B834-D) when a follow-up block is written — proven, deployed `DomoUserCustom`
   references beat any synthesized example.
