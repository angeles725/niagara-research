# Block 834 — How to build a custom Niagara login screen: the four paths, the native `BLoginTemplate` hook, and NiagaraMods Domo (`nmxdomo`)

> **Focus:** custom-login (new). **Scope:** every way to customize/replace the Niagara WebService
> login page — from zero-code branding slots, to the raw Tridium `web:LoginTemplate` extension point,
> to the third-party **Domo** module (turn-key themes + a from-scratch HTML/HBS/JS/CSS path). Answers
> the operator questions "do we already have a login corpus?" and "what do we need to build a login?".
>
> **Sources (all three consulted):**
> - **FUENTE 1 (corpus):** B9 (login form / auth schemes / `BILoginHTMLForm`), B11 §11.3.5 (login-screen
>   customization via slots + failed-login lockout + audit; §11.3.5 corrected by B558 on password strength),
>   B18 (CSRF token embedded in login HTML), B187/B289 (`domo` folder observed in the live station `shared/`),
>   B558 (`BPasswordStrength`).
> - **FUENTE 2 (Tridium docs):** guide *Customizing the login screen*
>   (`docGraphics_CreateACustomizedLoginScreen-1D8736F4`) — module-scheme best practice + the version matrix
>   for `file:^` login resources; guide *WebService (web-WebService)* — the **Login Template** property UI.
> - **FUENTE 3 (code):** `javax.baja.web.BLoginTemplate` / `ILoginTemplateEx` / `IStateLoginTemplate`
>   (web-rt docSource), `javax.baja.web.BWebService` `loginTemplate` slot, `nmxdomo-rt.jar`
>   (`META-INF/module.xml` + the four `BDomo*` classes), Domo example themes
>   (`sources/nmxdomo/example-themes/`). Provenance + hashes: `sources/nmxdomo/MANIFEST.md`,
>   `sources/SOURCES.md`.
> - **[CERT-live] operator:** Cristian has installed and used `nmxdomo` on his station(s) and built a
>   working login with the `DomoUserCustom` template; he confirms the vendor README describes the exact
>   workflow he already performs.

---

## 1. The single control point: `WebService.loginTemplate`

Everything routes through one WebService property.

- `[CERT]` `BWebService.loginTemplate` is a `BTypeSpec` property, `defaultValue = BTypeSpec.NULL`,
  `ALLOW_NULL = true`, `TARGET_TYPE = "web:LoginTemplate"`
  (`organized/docSource/docSource-doc/extracted/web-rt/javax/baja/web/BWebService.java:243-249,770`).
- `[CERT]` `BLoginTemplate.getLoginTemplate()` reads that spec and resolves it to the instance, or falls
  back to `com.tridium.web.BDefaultLoginTemplate.INSTANCE` when the spec is NULL (`BLoginTemplate.java`,
  `getLoginTemplate()` util method).
- `[CERT-doc]` In Workbench this is the **Login Template** property on the WebService property sheet
  (Config > Services > WebService). "Any" (= null) selects the stock template; otherwise the drop-downs
  list installed `web:LoginTemplate` types (guide *WebService* L193, L198–200).
- `[CERT-web]` The Domo README's Usage section is exactly this: uncheck null → first drop-down `nmxdomo`
  (the module) → second drop-down the template type (`DomoPanelLeft` / `DomoFloatingCard` /
  `DomoUserCustom`). The two drop-downs are the module+type halves of the `BTypeSpec`.

So "how do I get a custom login" always reduces to: **provide a `web:LoginTemplate` type (or use the
stock one's slots) and point `WebService.loginTemplate` at it.**

## 2. `javax.baja.web.BLoginTemplate` — the native extension point `[CERT]`

`BLoginTemplate extends BSingleton` and is `abstract`. Two abstract methods define the contract
(`BLoginTemplate.java`):

```java
public abstract void write(HttpServletRequest req, HttpServletResponse resp)
    throws IOException, ServletException;   // write the FULL HTML of the login page
public abstract BOrd resourceToOrd(String path);   // map /login/<path> → an ORD for images/CSS/JS
```

Javadoc contract (`[CERT]`, verbatim points):

- The template's job is "to write the full HTML content required to display the login form."
- To use Niagara's standard cookie auth, the form **must** contain at minimum (attributes may be added,
  none removed/renamed):
  ```html
  <form method='post' action='/j_security_check'>
    <input type='text' name='j_username' />
    <input type='text' name='j_password' />
    <input type='submit' name='submit' />
  </form>
  ```
- `resourceToOrd` resolves web resources only (JavaScript, CSS, images); the runtime serves them at
  `/login/<path>` and strips `/login/` before calling the method.
- "In a lot of cases it's unnecessary to override `BLoginTemplate` directly" — the stock template already
  honors WebService slots **`logo`** (ORD to a logo) and **`loginCss`** (ORD to a CSS file).

Two auxiliary interfaces (2015) let a template participate in request handling and login state without a
full servlet (`[CERT]`):

- `ILoginTemplateEx` — `processLoginGet(...)` / `processLoginPost(...)` (intercept the GET render and the
  POST submit).
- `IStateLoginTemplate` — `write(service, req, resp, LoginState state)` (render per login state: normal,
  error, forced password reset, 2FA token step).

`[INFER]` Domo's `BDomoLoginTemplate` implements these interfaces — that is how it drives password-reset
and 2FA flows and serves theme files — but this is not yet decompiled (gap **B834-G1**).

## 3. The four paths to a custom login

| # | Path | Code? | Flexibility | Where it lives |
|---|------|-------|-------------|----------------|
| **A** | Stock template + branding slots | none | logo + CSS only | `WebService.logo` / `loginCss` (+ favicon per B11) |
| **B** | Native custom `BLoginTemplate` module | Java (signed module) | total | your `web:LoginTemplate` subclass; resources via **module scheme** |
| **C** | Domo built-in template | none | high, fixed layout | install `nmxdomo`; set `loginTemplate = nmxdomo:DomoPanelLeft` / `DomoFloatingCard`; tune via slots |
| **D** | Domo `DomoUserCustom` theme | HTML/HBS/JS/CSS, **no Java** | total | `loginTemplate = nmxdomo:DomoUserCustom` + files in `file:^domo/` |

### 3.A Branding slots (zero code) `[CERT]/[CERT-doc]`
Add `logo` and `loginCss` ORDs (and `favicon`, B11 §11.3.5) to the WebService. Only swaps the logo/CSS on
the stock form — no layout change, no CAPTCHA, form HTML stays boilerplate (B11).

### 3.B Native `BLoginTemplate` (Tridium-sanctioned, from scratch) `[CERT]/[CERT-doc]`
Write a module with a `BLoginTemplate` subclass, register the type as `web:LoginTemplate`, implement
`write()` to emit the whole page (including the `/j_security_check` form), and select it in
`WebService.loginTemplate`. **Best practice per Tridium is the "module scheme"**: ship your login
resources inside the module and reference them with module ORDs, *not* from the station file system
(guide *Customizing the login screen*; see §4). This is the raw API Domo itself is built on.

### 3.C Domo built-in templates (turn-key) `[CERT-web]`
`DomoPanelLeft` (split pane) and `DomoFloatingCard` (centered card), both full-screen responsive with an
optional random Unsplash wallpaper. Customized purely by adding WebService slots: `primaryColor`,
`background` (Ord), `backgroundColor`, `backgroundGradient`, `pageBackground`, `transparent` (left-pane
only), `noUnsplash`, `unsplashSearch`, `hideEula`, `forgetUserId`, plus the standard `logo`/`loginCss`.

### 3.D Domo `DomoUserCustom` — full custom login, no Java `[CERT-web]/[CERT-live]`
This is the operator's confirmed path. Set `loginTemplate = nmxdomo:DomoUserCustom`, then drop a theme
into **`file:^domo/`** (the station file root; per B289 `^` resolves to `shared/`, so this is
`shared/domo/` — the `domo` folder already observed live in B187/B289). Mechanics:

- **Required file:** `login.hbs` or `login.html` at `file:^domo/login.(hbs|html)`. Parsed by Handlebars
  even with a `.html` extension. `[CERT]` example: `sources/nmxdomo/example-themes/basic/login.hbs`.
- **Handlebars templating** (handlebars.java / jknack, bundled in the jar `[CERT]`): variable interpolation
  `{{title}}`, `{{primaryColor}}`, `{{username}}`, `{{#if hasError}}`, `{{#unless hideEula}}`, the
  `passwordResetObj.*` bag, etc. No partials; block helpers `#if`/`#unless` supported.
- **Custom variables:** list extra WebService slot names in `file:^domo/custom.slots` (one per line) to
  expose them as `{{slotName}}` + `{{hasSlotName}}` (slot-path syntax, e.g. `Another$20Slot`).
- **Serving assets:** files in `domo/` are served publicly at `login?domo/<file>` (only files that exist
  there; nothing else) — the security surface to respect. Background image at `login?image=background`;
  stock logo at `/login/logo`; stock CSS at `/login/loginCss`.
- **The `$domo` JS API** (`login?domo.js`, after `login/core/auth.min.js`): `$domo.config` (all template
  vars), `$domo.login()`, `$domo.check2FA()`, `$domo.twoFactorLogin()`, `$domo.resetPass()`,
  `$domo.checkPassword()`, `$domo.checkMatchPassword()`, `$domo.updateFormVisibility()`,
  `$domo.display()`, and `$domo.unsplash()` (`login?unsplash.js`). The example base template wires the
  submit button to `return $domo.login()` and the reset button to `$domo.resetPass(...)` `[CERT]`.

## 4. The critical version caveat: `file:^` login resources on 4.10u3+ `[CERT-doc]`

Tridium locked down file-system login resources (guide *Customizing the login screen*, version matrix):

| Niagara version | `file:^` login resources |
|---|---|
| 4.10 | allowed (`file:^` ord) + module scheme |
| **4.10u3** | **NOT permitted** — resources must be in a module (module ORD) |
| 4.10u4+ | only `file:^^public` subfolder of the *protected* station home; module scheme |

This matters because Domo's `DomoUserCustom` path uses `file:^domo/`. It keeps working on locked-down
versions (incl. 4.14) because **the browser never fetches `file:^` directly** — `BDomoLoginTemplate`
(trusted server-side login code) reads the theme files itself and re-serves them at `login?domo/...`
(README Security Notes; `[INFER]`, confirmed in spirit by the operator's live use on 4.14 — verify by
decompile, gap **B834-G1**). Path B's raw `file:^` approach, by contrast, is the thing 4.10u3 disallows —
hence Tridium's "module scheme" best practice.

## 5. Domo module facts `[CERT]`

`nmxdomo-rt.jar` `META-INF/module.xml`: `name=nmxdomo-rt`, `vendor=NiagaraMods`,
`vendorVersion=1.0.0.4`, `preferredSymbol=nmxdomo`, `runtimeProfile=rt`, deps `web-rt`+`baja` 4.10.
Registered types: `DomoFloatingCard`, `DomoPanelLeft`, `DomoUserCustom` (each a thin class ~1.5–2 KB);
the engine is `BDomoLoginTemplate` (17.5 KB). Bundles jknack **handlebars** + antlr4-runtime +
commons-lang3/commons-text.

Install (README): copy `nmxdomo-rt.jar` to the modules dir (local) or push via Software Manager
(remote); relaunch Workbench; restart the station.

## 6. Domo limitations & operational requirements `[CERT-web]`

- **Digest auth only.** Built-in templates + the `$domo` JS lib support digest (Niagara default) only.
  No SSO/SAML/OAuth/Kerberos via Domo — you'd hand-write that in a `DomoUserCustom` theme.
- **No Lexicon / localization** in the built-in templates (roadmap item).
- **Beta known issues (2024-03-22):** 4.14 Beta 2FA + password reset broken (worked with Tridium);
  Firefox login bug.
- **CSP edits required** for external assets (WebService > HTTP Header Providers > Content Security Policy):
  `style-src` += `cdnjs.cloudflare.com` (animate.css) and `fonts.googleapis.com`; `font-src` +=
  `fonts.gstatic.com`; `connect-src` += `unsplash.niagaramodules.com`; `img-src` += `images.unsplash.com`.
  Unsplash traffic is browser-side only — the station makes no outbound internet calls.
- **Public exposure:** anything in `domo/` is served unauthenticated at `login?domo/<file>`. Never put
  sensitive files there.

## 7. Corpus state before this block (operator question 1)

We already had **login corpus**, but only the auth/branding foundation, not the extension mechanism:
- **B9** — `BILoginHTMLForm` generates the login form HTML; each auth scheme customizes UI.
- **B11 §11.3.5** — login-screen customization via `logo`/`loginCss`/`favicon` slots, failed-login lockout
  (`lockOutEnabled`, `maxBadLoginsBeforeLockOut` 1–10, `lockOutWindow` 30s, `lockOutPeriod` 10s), audit
  scope. (Its "no built-in password complexity" claim is CORRECTED by B558: `BPasswordStrength` is
  built-in since AX 3.8 / N4.)
- **B18** — the CSRF token embedded as `<input id="csrfToken">` in every rendered HTML incl. login.
- **B187/B289** — the `domo` folder already present in the live station `shared/` space (Path D footprint).

**Gap that this focus fills:** none of the above documented `BLoginTemplate` / the `loginTemplate` slot /
`ILoginTemplateEx` / `IStateLoginTemplate` (the actual "from scratch" API), nor Domo. B834 opens that.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | `WebService.loginTemplate` is a `BTypeSpec`, null-allowed, target type `web:LoginTemplate` | [CERT] | `BWebService.java:243-249,770` |
| 2 | Null `loginTemplate` → `BDefaultLoginTemplate.INSTANCE`; else the spec's instance | [CERT] | `BLoginTemplate.java` `getLoginTemplate()` |
| 3 | `BLoginTemplate` is abstract `BSingleton`; `write(req,resp)` emits full HTML; `resourceToOrd` maps `/login/<path>` | [CERT] | `BLoginTemplate.java` |
| 4 | Standard auth needs `<form action='/j_security_check'>` with `j_username`/`j_password`/submit | [CERT] | `BLoginTemplate.java` javadoc |
| 5 | Stock template honors `logo` + `loginCss` slots | [CERT] | `BLoginTemplate.java` javadoc; B11 §11.3.5 |
| 6 | `ILoginTemplateEx` (get/post) and `IStateLoginTemplate` (write with `LoginState`) exist | [CERT] | `ILoginTemplateEx.java`, `IStateLoginTemplate.java` |
| 7 | `nmxdomo-rt` registers `DomoFloatingCard`/`DomoPanelLeft`/`DomoUserCustom`; engine `BDomoLoginTemplate`; deps web-rt/baja 4.10 | [CERT] | `nmxdomo-rt.jar` `META-INF/module.xml` |
| 8 | Domo bundles handlebars.java (jknack) + antlr4 + commons-lang3/text | [CERT] | jar listing |
| 9 | `DomoUserCustom` theme = `login.hbs`/`.html` in `file:^domo/`, Handlebars-parsed, `$domo` JS API | [CERT-web] + [CERT] example | README; `example-themes/basic/login.hbs` |
| 10 | From 4.10u3, `file:^` login resources are not permitted (module scheme / 4.10u4 `file:^^public`) | [CERT-doc] | guide *Customizing the login screen* version matrix |
| 11 | Domo digest-only; no Lexicon; 4.14-beta 2FA/reset + Firefox known issues; CSP edits for external assets | [CERT-web] | README Beta Notes / CSP section |
| 12 | `^` resolves to `shared/`; a `domo` folder is present in the live station | [CERT-live] | B289, B187 |
| 13 | Operator has used `nmxdomo` `DomoUserCustom` to build a working login; README matches his workflow | [CERT-live] | operator statement 2026-09-06 |
| 14 | `BDomoLoginTemplate` serves `file:^domo/` files itself via `login?domo/...` (why Path D survives 4.10u3+) | [INFER] | README Security Notes; not yet decompiled → B834-G1 |

**Tally:** 14 claims — 8 [CERT], 2 [CERT-doc]/[CERT-web] mix (rows 9/10/11 span doc+code), 3 [CERT-live],
1 [INFER]. No unmarked assertions.

## Connections

- **B9** login form / auth schemes — the layer `write()` output plugs into.
- **B11 §11.3.5** login customization slots + lockout — Path A; the slot layer Domo also reuses.
- **B18** CSRF token in login HTML — any custom template inherits this delivery.
- **B558** `BPasswordStrength` — the real password rules `$domo.checkPassword()` mirrors client-side.
- **B187 / B289** `^ = shared/`, `domo` folder live — Path D footprint on the operator's station.
- **web-rt (B813)** `BWebServlet` base — sibling of the login-template servlet path.

## Open gaps (RESEARCH-STATE-custom-login)

- **B834-G1** *(next)* — Decompile `BDomoLoginTemplate` (`nmxdomo-rt.jar`): confirm it implements
  `ILoginTemplateEx`/`IStateLoginTemplate`, how it reads `file:^domo/`, serves `login?domo/...`, resolves
  Handlebars vars, and drives 2FA/reset. Verifies claim 14 → [CERT].
- **B834-G2** — Native Path B end-to-end recipe: a minimal `BLoginTemplate` module (slotomatic type reg
  as `web:LoginTemplate`, module-scheme resources, build/sign) — the sanctioned no-Domo route.
- **B834-G3** — Live-station probe (operator-authorized): read the client station's actual
  `WebService.loginTemplate` value and `shared/domo/` contents to record the deployed reality [CERT-live].
- **B834-G4** — The stock (`BDefaultLoginTemplate`) render + `LoginState` machine (error / forced reset /
  2FA token) that every template must handle.
