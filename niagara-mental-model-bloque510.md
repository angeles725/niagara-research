# Block 510 — `apis` API5: the `BAuthenticationScheme` SPI — the framework contract for AUTHORING a custom N4 auth scheme (abstract methods + two-level registration + `BUser` binding + the `BAuthenticationService.authenticate()` JAAS orchestration + `NiagaraLoginModule`/`BCallbackHandler` wiring)

> **Focus:** `apis`, gap **API5** — the N4 pluggable-authentication FRAMEWORK from the scheme-author's side.
> READ-ONLY, decompiled; no run. Markers §3. **REMITTANCE-checked:** [B494] (oem-tail U14) documented the OEM
> scheme *implementations* (SAML/LDAP/gauth/clientCert) and named the SPI in one paragraph; it did NOT document
> the AUTHORING contract — this block is that delta, verified genuine.
> **Sources:** FUENTE 3 — `organized/baja/baja/vineflower/{javax/baja/authn, javax/baja/user, com/tridium/authn}`.
> FUENTE 1 — [B494] (implementations), [B508] (the web CallbackHandlers `BHttpDigestCallbackHandler`=SCRAM /
> `BWebHTTPBasicCallbackHandler`; SessionManager), [B134] (Fox key exchange). Evidence delegated to a `sonnet`
> REMITTANCE-aware sweep; ALL load-bearing file:line RE-VERIFIED inline.

## §510.1 — The base SPI: `javax.baja.authn.BAuthenticationScheme` `[CERT]`

`abstract class BAuthenticationScheme extends BComponent implements BIAgent` (`:26`). A scheme author implements
**three abstract methods**:
- `getSchemeName() : String` (`:35`) — the wire scheme key (e.g. `"n4digest"`);
- `getLoginConfiguration() : javax.security.auth.login.Configuration` (`:37`) — the JAAS `Configuration` naming
  which `LoginModule` to run;
- `getDefaultAuthenticator() : BAbstractAuthenticator` (`:123`) — the per-user credential holder assigned to a
  `BUser` who switches to this scheme.

Two overridable hooks (defaults given, NOT in [B494]):
- `supportsRemoteUsers() : boolean` (`:93`, default false) — override → true opts the scheme into the
  unknown-user path (SSO schemes do this);
- `getKeyExchangeMethodName() : String` (`:97`) — ties the scheme into Fox key-exchange negotiation.

The **concrete login entry** (a scheme author does NOT override it) is `login(CallbackHandler) : LoginContext`
(`:101`): `new LoginContext("", null, handler, getLoginConfiguration())` under the scheme's own classloader, then
`lc.login()`. This is the actual JAAS invocation.

## §510.2 — Registration: two levels + per-user binding `[CERT]`

- **Type-registry (compile-time):** the concrete class carries `@AgentOn(types={"baja:AuthenticationScheme"})`,
  so `Sys.getRegistry()` can enumerate all scheme types (`getSchemeFromName()` iterates without knowing them).
- **Instance (runtime):** a scheme must be ADDED to the `BAuthenticationService.authenticationSchemes` folder as
  a named child — `@AgentOn alone does NOT auto-populate the folder`. The service constructor seeds the defaults:
  `add("DigestScheme", new BDigestAuthenticationScheme())` (`BAuthenticationService.java:129`),
  `add("AXDigestScheme", new BLegacyDigestAuthenticationScheme())` (`:130`); `defaultAuthenticationScheme`
  property (`:74`) = `"DigestScheme"`.
- **Per-user binding:** `BUser.authenticationSchemeName` (String slot, default `"DigestScheme"`,
  `BUser.java:225-227`); changing it swaps the user's `authenticator` for the new scheme's
  `getDefaultAuthenticator()`. `BUserService.getAuthenticationSchemeForUser()` resolves it, falling back for an
  unknown user to the first `supportsRemoteUsers()` scheme — the SSO auto-handling.

## §510.3 — The login orchestration: `BAuthenticationService.authenticate()` `[CERT]`

The loop every login runs (`com/tridium/authn/BAuthenticationService.java:162+`):
1. **Scheme select** — legacy connection → legacy digest; explicit scheme → use it; else
   `userService.getAuthenticationSchemeForUser(requestedUser)`.
2. **Cached super-session** — if the session is already in `SessionManager`, return the cached user (no JAAS).
3. **JAAS** — `LoginContext lc = scheme.login(handler)` (`:183`) runs the scheme's `LoginModule` with the
   transport `CallbackHandler`.
4. **User extraction** — `subject.getPrincipals(BUser.class)` (`:187`) — the `LoginModule` must have added a
   `BUser` principal on commit.
5. **SuperSessionPrincipal** injected under `doPrivileged`; **`subject.setReadOnly()`** freezes it.
6. **`session.setAuthenticated(subject)`** (`:235`) — the moment the session becomes authenticated (what [B508]'s
   `SessionManager` tracks).
7. **Audit** — `processLoginAttempt(true, …)`; a `NiagaraFailedLoginException` triggers
   `user.authenticateFailed(...)` and re-throws.

## §510.4 — `NiagaraLoginModule` + `BCallbackHandler` discovery `[CERT]`

A scheme's LoginModule extends `abstract NiagaraLoginModule implements LoginModule` (`:15`); its `commit()` does
`subject.getPrincipals().add(this.user)` (`:36`) — the author sets `this.user` on success and the base handles
the principal add. The transport handler is discovered by the scheme via
`scheme.getAgentOn(BCallbackHandler.class)` (`BCallbackHandler` = `abstract BStruct implements CallbackHandler,
BIAgent`) — this walks the scheme type's agent list and returns the handler registered for that scheme/transport.
So [B508]'s `BHttpDigestCallbackHandler` (web/SCRAM) and `BWebHTTPBasicCallbackHandler` (web/Basic) are the WEB
`BCallbackHandler`s a scheme resolves; a Fox equivalent serves the Fox transport. `[INFER]`: a scheme therefore
plugs into every transport by registering one `BCallbackHandler` per transport as its agents — the author writes
`{scheme, LoginModule (via JAAS Configuration), per-transport CallbackHandler, default authenticator}`.

## §510.5 — The shipped schemes (the author's pattern catalog) `[CERT]`

| Folder slot | Class | `getSchemeName()` | Notes |
|---|---|---|---|
| `DigestScheme` (default) | `BDigestAuthenticationScheme` | **`"n4digest"`** | `BPasswordAuthenticationScheme`; the only scheme that overrides `getKeyExchangeMethodName()` (Fox key exchange); its wire handler is SCRAM ([B508]) |
| `AXDigestScheme` | `BLegacyDigestAuthenticationScheme` | (legacy) | AX back-compat |
| (not auto-added) | `BHTTPBasicAuthenticationScheme` | **`"n4HTTPbasic"`** | must be added to the folder to enable Basic ([B508]/[B290] §14) |
| (not auto-added) | `BSessionIdAuthenticationScheme` | **`"session"`** | extends `BAuthenticationScheme` directly; `getDefaultAuthenticator()` **throws** — no per-user credential model |

`[INFER]`: the scheme-name strings (`n4digest`/`n4HTTPbasic`/`session`) are the values that surface in the HTTP
`WWW-Authenticate`/`Authorization` scheme field ([B508]) and the Fox login — the author's `getSchemeName()`
literally becomes the on-wire token.

## §510.6 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | BAuthenticationScheme=BComponent+BIAgent; abstract getSchemeName/getLoginConfiguration/getDefaultAuthenticator + hooks + login() | `[CERT]` | `BAuthenticationScheme.java:26,35,37,93,97,101,123` | PASS |
| 2 | two-level registration: @AgentOn type + folder instance (defaults added in ctor); @AgentOn alone ≠ folder | `[CERT]` | `BAuthenticationService.java:129-130` | PASS |
| 3 | BUser.authenticationSchemeName slot default DigestScheme; swap → new authenticator | `[CERT]` | `BUser.java:225-227` | PASS |
| 4 | authenticate(): scheme.login → getPrincipals(BUser) → setAuthenticated(subject) | `[CERT]` | `BAuthenticationService.java:183,187,235` | PASS |
| 5 | NiagaraLoginModule.commit adds BUser principal; scheme discovers per-transport BCallbackHandler | `[CERT]` | `NiagaraLoginModule.java:15,36` | PASS |
| 6 | shipped schemes n4digest/n4HTTPbasic/session; SessionId throws on getDefaultAuthenticator | `[CERT]` | `BDigestAuthenticationScheme.java:31`; `BSessionIdAuthenticationScheme.java:30`; `BHTTPBasicAuthenticationScheme.java:30` | PASS |

**Tally:** 6 claims — all `[CERT]` load-bearing + 2 `[INFER]` (per-transport plug model, scheme-name-is-wire-token).
Block TYPE = **EVIDENCE**; API5 CLOSED. REMITTANCE-checked genuine delta over [B494]. All load-bearing tokens
re-verified inline.

## §510.7 — Connections & focus status

- **Extends [B494]** (which was a one-paragraph implementation summary) with the framework author contract — a
  forward pointer added to [B494] §494.1. NOT a correction; a depth extension.
- **Completes the auth picture with [B508]:** B508 found the web CallbackHandlers (SCRAM/Basic) and the session
  bind; this block shows the SCHEME that owns them and the `authenticate()` loop that drives them. `getSchemeName()`
  → the [B508] `WWW-Authenticate` scheme token.
- Fox side: `getKeyExchangeMethodName()` ties DigestScheme into [B134]'s Fox key exchange.
- **Focus status:** `apis` 4/8 (API1–API3, API5 closed). NEXT = API7 (`BJob`/`JobService` API).
