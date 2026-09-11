# Block 862 — `BDomoLoginTemplate` internals (G1) and `BDefaultLoginTemplate` + `LoginState` (G4): decompile evidence

> **Focus:** custom-login. **Closes:** B834-G1 (decompile `BDomoLoginTemplate`) and B834-G4
> (`BDefaultLoginTemplate` render + `LoginState` machine). **Supersedes** the [INFER] claim in
> B834 §2 row 14. See **Correction** section below.
>
> **Sources consulted (all three, METHODOLOGY §6):**
> - **FUENTE 1 (corpus):** B834 (custom-login bootstrap + claim 14), B9, B11, B18, B289.
> - **FUENTE 2 (Tridium docSource + vineflower):**
>   `organized/web/web-rt/vineflower/com/tridium/web/BDefaultLoginTemplate.java` (527 l.) ·
>   `organized/web/web-rt/vineflower/com/tridium/web/servlets/LoginServlet.java` ·
>   `organized/web/web-rt/vineflower/com/tridium/web/servlets/LoginFileServlet.java` ·
>   `organized/docSource/docSource-doc/extracted/web-rt/javax/baja/web/LoginState.java` (44 l.) ·
>   `organized/docSource/docSource-doc/extracted/web-rt/javax/baja/web/IStateLoginTemplate.java` ·
>   `organized/docSource/docSource-doc/extracted/web-rt/javax/baja/web/ILoginTemplateEx.java` ·
>   `organized/web/web-rt/vineflower/javax/baja/web/BLoginTemplate.java`.
> - **FUENTE 3 (3rd-party decompile, vineflower):**
>   `sources/decompiled/nmxdomo-rt/com/niagaramods/nmxdomo/BDomoLoginTemplate.java` (392 l.) +
>   `BDomoFloatingCard.java` / `BDomoPanelLeft.java` / `BDomoUserCustom.java`.
>   Source: `nmxdomo-rt.jar` (sha256 `aa0a003f...`, inside `/mnt/c/Users/equipo/Downloads/nmxdomo-b4.zip`).
> - **niagara-help:** `niagara_help.py find "login template"` → guide `web-WebService.txt:L193`;
>   `niagara_help.py find "LoginState"` → `bajadoc-clean/javax/baja/web/LoginState.txt` (4 methods, no
>   named integer constants); `find "two factor"` → 0 web-related results.

---

## Correction to B834 claim 14 (and §2 [INFER])

B834 §2 stated (row 14 of self-verify, marker `[INFER]`):

> "Domo's `BDomoLoginTemplate` implements these interfaces — that is how it drives password-reset and
> 2FA flows and serves theme files"

**This is partially wrong. The decompile shows:**

1. `BDomoLoginTemplate` (`com.niagaramods.nmxdomo.BDomoLoginTemplate`) is a **plain Java class** — it is
   NOT a subclass of `BLoginTemplate` and implements NO interfaces (not `ILoginTemplateEx`, not
   `IStateLoginTemplate`).
2. The three thin subclasses (`BDomoFloatingCard`, `BDomoPanelLeft`, `BDomoUserCustom`) extend
   `BLoginTemplate` directly — but they ALSO do NOT implement either interface. Each just delegates to
   `BDomoLoginTemplate.writeTemplate(file, request, response)`.
3. Password reset and 2FA are driven via **HTTP session attributes and request parameters**, not `LoginState`.

Claim 14 as written also stated the mechanism was "ILoginTemplateEx/IStateLoginTemplate-based" — that part
is wrong. The _rest_ of claim 14 (that `BDomoLoginTemplate` reads `file:^domo/` server-side and re-serves
files at `login?domo/...`) is **CONFIRMED** (see §1 below).

**B834 pointer:** A corrective note has been added at the bottom of B834 (see §14 cross-block consistency).

---

## 1. Gap B834-G1: `BDomoLoginTemplate` internals `[CERT]`

All evidence from `sources/decompiled/nmxdomo-rt/com/niagaramods/nmxdomo/`.

### 1.1 Class taxonomy

| Class | Extends | Implements | Size | Role |
|-------|---------|------------|------|------|
| `BDomoLoginTemplate` | `java.lang.Object` | none | 392 l. | Engine: static helper methods |
| `BDomoFloatingCard` | `BLoginTemplate` | none | 25 l. | Thin delegate → built-in HBS |
| `BDomoPanelLeft` | `BLoginTemplate` | none | ~25 l. | Thin delegate → built-in HBS |
| `BDomoUserCustom` | `BLoginTemplate` | none | 44 l. | Thin delegate → user HBS/HTML |

`[CERT]` `BDomoFloatingCard.java:13`, `BDomoUserCustom.java:13`. Neither implements `ILoginTemplateEx`
or `IStateLoginTemplate`.

### 1.2 Entry point and request dispatch

`BDomoLoginTemplate.writeTemplate(BIFile file, req, resp)` is the single entry point called by the three
subclasses. It branches on query parameters in this order `[CERT]` `BDomoLoginTemplate.java:41-86`:

| Query condition | Handler | Effect |
|----------------|---------|--------|
| `?image=<slot>` | `writeImage()` | reads `BWebService.get(slot)` as a `BOrd`, resolves to a file, streams it |
| `?domo.js` | `writeDomoJs()` | streams `module://nmxdomo/rc/js/domo.js` + appends `window.$domo.config = {...}` |
| `?unsplash.js` | `writeUnsplashJs()` | streams `module://nmxdomo/rc/js/unsplash.js` |
| queryString starts with `domo/` | `writeLocalFile()` | resolves `file:^<queryString>`, streams user file |
| `?has2fa=<username>` | `write2FaResponse()` | returns "true" or "false" as text |
| else | Handlebars render | reads the `BIFile` passed in, Handlebars-parses, writes HTML |

`[CERT]` `BDomoLoginTemplate.java:41-86`.

### 1.3 `file:^domo/` serving — confirms B834 claim 14

`writeLocalFile()` `[CERT]` (`BDomoLoginTemplate.java:149-170`):

```java
String path = request.getQueryString();   // e.g. "domo/logo.png"
BIFile file = (BIFile) BOrd.make("file:^" + path).get();
// → BOrd.make("file:^domo/logo.png").get() — server-side, trusted login code
response.setContentType(file.getMimeType());
response.setContentLength((int)file.getSize());
// ... streams file bytes
```

The browser fetches `/login?domo/logo.png`; `LoginServlet` dispatches to
`BDomoLoginTemplate.writeTemplate()`, which then resolves `file:^domo/logo.png` internally. The
`file:^` ORD is resolved on the **server side** (trusted login code), never by the browser. This is
exactly why Path D (`DomoUserCustom`) works on N4.10u3+ despite `file:^` being locked out for browser
direct access. **B834 claim 14 confirmed: marker upgraded to [CERT].**

`resourceToOrd()` maps `/login/<path>` for module resources `[CERT]` (`BDomoLoginTemplate.java:145-147`):

```java
public static BOrd resourceToOrd(String path) {
    return BOrd.make("module://nmxdomo/rc" + path);
}
```

So `/login/floating-card/domo.css` → `module://nmxdomo/rc/floating-card/domo.css` (served through
`LoginFileServlet` → `UnauthenticatedCache` pipeline).

### 1.4 Handlebars template binding

Template compilation `[CERT]` (`BDomoLoginTemplate.java:73-84`):

```java
Handlebars hb = new Handlebars();
for (ConditionalHelpers helper : ConditionalHelpers.values()) {
    hb.registerHelper(helper.name(), helper);  // #if, #unless, eq, neq, lt, …
}
Context hbContext = Context.newBuilder(parameterMap(request))
    .resolver(MapValueResolver.INSTANCE).build();
Template template = hb.compileInline(templateData.toString());
String output = template.apply(hbContext);
```

The `.hbs` (or `.html`) file is compiled inline on every request — no caching, no pre-compilation.

`parameterMap(req)` assembles the template variable map `[CERT]` (`BDomoLoginTemplate.java:88-143`):

| Variable | Source |
|----------|--------|
| `title` | `Sys.getStation().getStationDisplayName(ctx)` |
| `username` | `NiagaraWebSession.getAttribute("username")` |
| `hasError` | `request.getParameter("auth") == "fail"` |
| `hasPasswordReset` | `session.getAttribute("forceReset") != null && == true` |
| `hasPasswordResetError` | `session.getAttribute("resetError") != null` |
| `passwordResetObj` | Map with reset messages from `BPasswordStrength` (length, digits, lower, upper, special, max) + Lexicon HTML-safe strings |
| `hasLogo`, `hasLoginCss`, `hasBackground` | `BWebService.get(slotName) != null && != BOrd.NULL` |
| `primaryColor` | `BWebService.get("primaryColor")` or default `"#2d8cf0"` |
| `backgroundColor` | `primaryColor` unless `backgroundColor` slot set |
| `pageBackground` | `BWebService.get("pageBackground")` or `""` |
| `unsplashSearch`, `noUnsplash`, `hideEula`, `forgetUserId`, `transparent`, `hasGradient`, etc. | `BWebService` slot existence/value |
| Custom slots | Read from `file:^domo/custom.slots` line by line; each line = a slot name (`SlotPath.escape`-d); generates `<slotName>` + `has<SlotName>` pairs |

`[CERT]` `BDomoLoginTemplate.java:88-143`.

### 1.5 2FA detection

`write2FaResponse()` checks whether a username has 2FA enabled `[CERT]` (`BDomoLoginTemplate.java:379-391`):

```java
BAuthenticationScheme authnScheme = userService.getAuthenticationSchemeForUser(user);
return authnScheme.toString().equals("Google Authentication Scheme");
```

2FA detection is **TOTP-only via Google Authenticator** — the scheme's `toString()` is literally checked
for the string `"Google Authentication Scheme"`. The `$domo.check2FA(username)` JS call hits
`GET /login?has2fa=<username>` and gets "true"/"false" back.

### 1.6 Password reset flow

B834's claim that Domo drives the reset flow via `IStateLoginTemplate` is wrong. Actual mechanism:

1. An external actor (the auth framework) sets `session.setAttribute("forceReset", true)` and optionally
   `session.setAttribute("resetError", <message>)`.
2. On the next `GET /login`, `writeTemplate()` checks `session.getAttribute("forceReset")` →
   `hasPasswordReset = true` in `parameterMap`.
3. If `resetError` is set, the servlet writes `response.setHeader("resetError", ...)` and returns HTTP 400.
4. The HBS template conditional `{{#if hasPasswordReset}}` renders the reset form; the `passwordResetObj`
   map contains all strength-rule messages and Lexicon-localized labels.
5. `$domo.resetPass()` POSTs to `j_security_check/` with `resetToken` (SJCL base64-encoded new password).

`[CERT]` `BDomoLoginTemplate.java:53-58`, `BDomoLoginTemplate.java:337-377`.

### 1.7 Built-in templates (FloatingCard / PanelLeft)

Both subclasses resolve their file from the module jar `[CERT]` (`BDomoFloatingCard.java:18-19`):

```java
BIFile file = (BIFile) BOrd.make("module://nmxdomo/rc/floating-card/login.hbs").get();
BDomoLoginTemplate.writeTemplate(file, request, response);
```

The built-in `.hbs` files (extracted from `nmxdomo-rt.jar` at `rc/floating-card/login.hbs` and
`rc/panel-left/login.hbs`) use the same `$domo.*` JS API as `DomoUserCustom` and include:
- `<script src="/login?domo.js">` — config injection
- `<script src="/login/core/auth.min.js">` — SCRAM/digest auth engine
- `<link href="//cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css">` — external CSS
- Handlebars blocks: `{{#if hasPasswordReset}}`, `{{#unless hasPasswordReset}}`, `{{#if hasLogo}}`,
  `{{#if hasError}}`, `{{#unless hideEula}}` etc.
- The login form POSTs to `action="j_security_check"` via `$domo.login({...})`.

`[CERT]` `rc/floating-card/login.hbs:1-60` (jar extract via `jar tf nmxdomo-rt.jar`).

---

## 2. Gap B834-G4: `BDefaultLoginTemplate` render + `LoginState` machine `[CERT]`

### 2.1 Template engine: Apache Velocity, not Handlebars

`BDefaultLoginTemplate` uses Velocity `.vm` templates via `Template.process()`, not Handlebars
`[CERT]` (`BDefaultLoginTemplate.java:68-78`):

```java
private static final BOrd loginTemplate    = BOrd.make("module://web/com/tridium/web/rc/loginN4.vm");
private static final BOrd preLoginTemplate = BOrd.make("module://web/com/tridium/web/rc/preLoginFormN4.vm");
// + logoN4.vm, loginCssN4.vm, eulaN4.vm, ssoForm.vm, ssoButton.vm,
//   passwordExpirationFormN4.vm, networkPasswordExpirationFormN4.vm,
//   webstartN4.vm, schemeSelect.vm, passwordResetFormN4.vm (in BDigestLoginHTMLForm)
```

### 2.2 Two-phase username-first flow

`write()` branches on `username = LoginSupport.getRequestedUsername(request)`:

- `username == null` → redirect to `/prelogin` (pre-login form, username entry only,
  `preLoginFormN4.vm`, optional scheme-select dropdown).
- `username != null` → serve full auth form via `getInnerForm(username, ...)`.

`[CERT]` `BDefaultLoginTemplate.java:99-166`.

### 2.3 Login state machine — session attributes, not `LoginState`

`BDefaultLoginTemplate` does **NOT** implement `IStateLoginTemplate`. State is conveyed through session
attributes and HTTP cookies `[CERT]`:

| State | Signal | Rendered form |
|-------|--------|---------------|
| Normal (username obtained) | `session.getAttribute("forceReset") == null && getAttribute("passwordExpires") == null` | `BILoginHTMLForm.getLoginFormHTML(ctx, false)` |
| Auth error / failure | Cookie `niagara_failure_cause` or param `loginFailureCause` → `LoginFailureCause` enum | `loginFailed` string in `loginN4.vm` |
| Forced password reset | `session.getAttribute("forceReset") == true` | `BILoginHTMLForm.getLoginFormHTML(ctx, true)` → `passwordResetFormN4.vm` |
| Password expiration warning | `session.getAttribute("passwordExpires") != null` (Long = expiry epoch) | `passwordExpirationFormN4.vm` (local user) or `networkPasswordExpirationFormN4.vm` (network user) |

`[CERT]` `BDefaultLoginTemplate.java:279-335`.

SSO schemes are rendered as buttons in `extraForms` alongside the normal form, unless `forceReset` is
active (in which case `extraForms` returns empty). Strict-authentication mode adds a scheme-select
dropdown via `schemeSelect.vm`. `[CERT]` `BDefaultLoginTemplate.java:337-416`.

### 2.4 `LoginState` class — bare int holder, no named constants in corpus

`LoginState` (`docSource`, `[CERT]`) is:

```java
public class LoginState {
    private int state;
    private Object data;
    public static LoginState make(int state) { ... }
    public static LoginState make(int state, Object data) { ... }
    public int getState() { ... }
    public Object getData() { ... }
}
```

No named integer constants are defined in the class. The bajadoc (`bajadoc-clean/javax/baja/web/
LoginState.txt`) lists only the 4 methods — no fields. A corpus-wide search (`grep "IStateLoginTemplate\|
LoginState" organized/ -r`) finds no class in `web-rt` that calls `IStateLoginTemplate.write()` or
instantiates `LoginState`. `BDefaultLoginTemplate` and all three Domo subclasses bypass `LoginState`
entirely.

**Finding:** `LoginState` and `IStateLoginTemplate` are extension hooks for **third-party** custom login
templates only. The N4.14 stock template (`BDefaultLoginTemplate`) and the Domo engine ignore them.
The integer state constants (`NORMAL`, `ERROR`, `FORCE_RESET`, `TWO_FA`, etc.) are not visible in the
N4.14 corpus — they may be defined in an implementation not decompiled here. This becomes child gap
**B862-G1** (locate `LoginState` constant definitions).

### 2.5 `LoginFileServlet` — how `/login/<path>` is served

`LoginFileServlet.doGet()` handles `/login/<path>` `[CERT]` (`LoginFileServlet.java:29-88`):

1. Special paths `/eula` and `/thirdPartyLicenses` → `LicenseGenerator`.
2. Paths starting `/core/...` → `NRE jetty rc` resources (`/com/tridium/nre/jetty/rc/<name>`).
3. Everything else → `BLoginTemplate.getLoginTemplate().resourceToOrd(pathInfo)` → resolves via the
   active template's `resourceToOrd` implementation → `UnauthenticatedCache.allowOrd()` whitelist check
   → cached bytes → streamed.

For Domo templates, `resourceToOrd("/floating-card/domo.css")` = `module://nmxdomo/rc/floating-card/domo.css`
(module scheme → served from `nmxdomo-rt.jar`). This path does NOT read `file:^`. `[CERT]`
`LoginFileServlet.java:68-75`.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | `BDomoLoginTemplate` is a plain class (not `BLoginTemplate` subclass); three thin subclasses extend `BLoginTemplate` directly | [CERT] | `BDomoLoginTemplate.java:40`; `BDomoFloatingCard.java:13`; `BDomoUserCustom.java:13` |
| 2 | None of the three Domo subclasses implement `ILoginTemplateEx` or `IStateLoginTemplate` | [CERT] | Full import lists + class declarations in decompiled files |
| 3 | `writeLocalFile()` resolves `BOrd.make("file:^" + queryString).get()` server-side and streams the file — confirms B834 claim 14 | [CERT] | `BDomoLoginTemplate.java:149-170` |
| 4 | `writeTemplate()` dispatch order: `?image`, `?domo.js`, `?unsplash.js`, `?domo/<path>`, `?has2fa`, else main render | [CERT] | `BDomoLoginTemplate.java:41-86` |
| 5 | Handlebars: `Handlebars()` + `ConditionalHelpers` + `MapValueResolver` + `compileInline` on every request | [CERT] | `BDomoLoginTemplate.java:73-84` |
| 6 | `parameterMap` assembles ~20 built-in vars; custom vars from `file:^domo/custom.slots` line by line | [CERT] | `BDomoLoginTemplate.java:88-143` |
| 7 | 2FA detection: `authnScheme.toString().equals("Google Authentication Scheme")` — TOTP only | [CERT] | `BDomoLoginTemplate.java:379-391` |
| 8 | Password reset: session attribute `forceReset` drives `hasPasswordReset`; `resetError` drives error display | [CERT] | `BDomoLoginTemplate.java:53-58`, `337-377` |
| 9 | Built-in FloatingCard/PanelLeft: file read from `module://nmxdomo/rc/<layout>/login.hbs` | [CERT] | `BDomoFloatingCard.java:18-19`; `BDomoPanelLeft.java` (same pattern) |
| 10 | `BDefaultLoginTemplate` uses Velocity `.vm` templates (NOT Handlebars); module scheme `module://web/...` | [CERT] | `BDefaultLoginTemplate.java:68-78` |
| 11 | Stock template: two-phase flow (null username → `/prelogin`; known username → auth form) | [CERT] | `BDefaultLoginTemplate.java:99-107`, `279-335` |
| 12 | Stock template state via session attributes: `forceReset`, `passwordExpires`, `resetError`; cookie `niagara_failure_cause` | [CERT] | `BDefaultLoginTemplate.java:279-335` |
| 13 | `BDefaultLoginTemplate` does NOT implement `IStateLoginTemplate`; `LoginState` not instantiated anywhere in corpus `web-rt` classes | [CERT] | Class declaration + corpus-wide grep |
| 14 | `LoginState` defines no named integer constants (bajadoc + docSource agree); they are absent from the corpus | [CERT] | `LoginState.java:1-44`; `bajadoc-clean/javax/baja/web/LoginState.txt` |
| 15 | `LoginFileServlet` serves `/login/<path>` via `resourceToOrd()` → module scheme; does NOT touch `file:^` | [CERT] | `LoginFileServlet.java:68-75` |
| 16 | `resourceToOrd()` in Domo: `module://nmxdomo/rc` + path | [CERT] | `BDomoLoginTemplate.java:145-147` |

**Tally:** 16 claims — all [CERT]. No unmarked assertions.

---

## Connections

- **B834** — parent bootstrap; claim 14 marker upgraded from [INFER] to [CERT]; §2 [INFER] corrected.
- **B9** — `BILoginHTMLForm` generates the inner auth form that `BDefaultLoginTemplate` delegates to.
- **B11 §11.3.5** — `logo`/`loginCss`/`favicon` slots: same slot names read by `parameterMap` (`hasLogo`,
  `hasLoginCss`).
- **B18** — CSRF token in login HTML — delivered by `auth.min.js` / the inner auth form.
- **B558** — `BPasswordStrength` — used verbatim by `PasswordResetObj()` to assemble the strength-rule
  message map.
- **B289** — `^ = shared/`; `file:^domo/` resolves there — the ORD prefix `BDomoLoginTemplate` reads.

---

## Open gaps

- **B862-G1** *(new, minor)* — `LoginState` integer constants: no named constants found in the corpus.
  They may be in a login-servlet class not fully decompiled, or in a Tridium-internal implementation
  that uses `IStateLoginTemplate`. Query result: 0 usages in `web-rt` organized classes.
- **B834-G2** — Native Path B recipe (minimal `BLoginTemplate` module, slotomatic, module-scheme, sign).
- **B834-G3** — Live-station probe (authorized): read `WebService.loginTemplate` + `shared/domo/` contents.
