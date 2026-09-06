# B830 — Servlet-side re-authentication of a second operator: the legal `BUserService`/`BPasswordCache` call path, the Context that attributes the write, and what a `null` Context really bypasses

**Sources** (three, cumulative): FUENTE 1 corpus — B803 §803.3 (the re-verify API), B829 (audit gate), B813/B804; FUENTE 2
niagara-help (freshness OK, bajadoc 3,589 files) — devguide `security/authentication.txt`, guides `StationSecurity/
auth_NiagaraAuthentication.txt`, `LdapN4/baja-UserService.txt`, bajadoc class/slots for `BUserService`, `BUser`,
`BPasswordCache`, `BPasswordAuthenticator`, `BAbstractAuthenticator`, `BAuthenticationScheme`, `BasicContext`, `WebOp`,
`BAutoLogoffSettings`; FUENTE 3 code — **docSource ORIGINAL** `baja/javax/baja/{user/BUserService,user/BUser,
security/BPasswordCache,security/BPasswordAuthenticator,security/BAbstractAuthenticator,security/BPassword,
sys/BasicContext,sys/Context,naming/OrdTarget}.java`, `web-rt/javax/baja/web/{WebOp,BWebServlet}.java`; **vineflower**
`baja/com/tridium/sys/schema/ComplexSlotMap.java`, `baja/javax/baja/authn/BAuthenticationScheme.java`,
`baja/com/tridium/session/NiagaraSuperSession.java`. Client: `niagara-panccadia-leon` at **a109249** (read-only worktree).
Mapper-traced, every load-bearing cite re-read at the enclosing method by the author before `[CERT]`.
**Scope:** C9 R14 / design D8b — the user wants a SECOND login INSIDE DashboardPan (the HMI panel shares ONE kiosk login)
so an operator identifies themself before writing. Question: from a `BWebServlet` subclass whose request is already
authenticated as the kiosk user, how does module code re-authenticate a DIFFERENT username+password against the station's
own user database and obtain that user's `Context` for `parent.set(prop, value, cx)` — plus guards, lockout accounting,
password-policy hooks, audit attribution (B829-G2), and per-session token/TTL. Numbering continues the global sequence (CATALOG max 829).

---

## 830.1 Answer in one paragraph

It is legal, small, and needs no new scheme: `BUserService.getUser(name)` → `canLogin(user)` → cast the user's
authenticator to `BPasswordCache` (guarded by `instanceof`) → `validate(password)` → **call `user.authenticateOk/Failed(service)`
yourself** (the framework's lockout hooks are caller-invoked) → pass **the `BUser` itself as the `Context`** (it implements
`Context` and `getUser()` returns itself) to `component.set(prop, value, user)`. That single argument makes the framework
(a) enforce `OPERATOR_WRITE` via `user.checkWrite` and (b) attribute the `AuditEvent` to that operator's username. It works
only for password-scheme users (digest/password; not LDAP/SAML/OAuth), and the module must own the failed-attempt accounting
and its own short-TTL config token, bounded by the station auto-logoff. `[CERT — §830.2-830.5]`

## 830.2 The legal call path `[CERT]`

| Step | Call | Cite (docSource unless noted) | What it does |
|---|---|---|---|
| 1 | `BUserService svc = (BUserService) Sys.getService(BUserService.TYPE); BUser u = svc.getUser(name)` | `BUserService.java:590-602` | `get(SlotPath.escape(name))`; returns **null** when absent or when the slot's `getUsername()` ≠ `name` (`SecurityUtil.equals`). No `authenticate`/`validate` exists on `BUserService` (bajadoc member list + devguide: `authenticateBasic()` REMOVED in N4). |
| 2 | `svc.canLogin(u)` | `BUserService.java:662-684` | false when `!getEnabled()`, `getLockOut()`, `isExpired()`, name `"BACnet"`, or `u.getAuthenticator().canLogin(u)` false — the latter checks **password expiration** for `BPasswordAuthenticator` (`BAbstractAuthenticator.java:67-80`). |
| 3 | `BAbstractAuthenticator a = u.getAuthenticator(); if (!(a instanceof BPasswordCache)) → unsupported scheme` | `BUser.java:517`; hierarchy `BPasswordAuthenticator extends BPasswordCache` (`BPasswordAuthenticator.java:81`), `BPasswordCache extends BAbstractAuthenticator` (`BPasswordCache.java:38`) | The default authenticator IS-A `BPasswordCache`, so B803 §803.3's cast `((BPasswordCache) u.getAuthenticator())` is **confirmed** correct; guard it with `instanceof` for non-password schemes. |
| 4 | `boolean ok = ((BPasswordCache) a).validate(submittedPassword)` | `BPasswordCache.java:87-95` (`public final`, `String` or `SecretChars`) → `BPassword.validate(SecretChars)` `BPassword.java:564-575` | `encoder.validate(password)`: **false on mismatch**; an encoder failure is rethrown as `SecurityException`. **No side effects**: grep `authenticateFailed|authenticateOk|lockOut|authFail` in `BPasswordCache.java` + `BPassword.java` = ZERO. Does not check policy/expiry (step 2 does). |
| 5 | `if (ok) u.authenticateOk(svc); else u.authenticateFailed(svc);` | `BUser.java:1120-1123`, `:1130-1158` | Javadoc: "This method **should be called** whenever an authentication attempt succeeds/fails on this user. It provides the hook necessary to enforce the lock out policy." `authenticateFailed`: bails if `!lockOutEnabled`; enqueues `now`; drops entries older than `lockOutWindow`; if `size ≥ maxBadLoginsBeforeLockOut` → `setLockOut(true)`, clears the queue, `Clock.schedule(this, lockOutPeriod, clearLockOut, null)`. **The module must invoke these** — `validate()` never does. |
| 6 | `component.set(prop, value, (Context) u)` — or `new BasicContext(u)` | `BUser.java:300-303` (`implements … Context, Principal`), `:1049` (`getUser()` returns `this`), `:1046` (`getBase()` null); `BasicContext.java:55-60` | Either object satisfies `ComplexSlotMap.set`'s gate; `BasicContext` only adds facets/language, which `set` does not read. The client already uses the cast form today: `user.getPermissions((javax.baja.sys.Context) user)` `[CERT client DashboardRbacHelper.java:97 @a109249]`. |

## 830.3 Guard list — who enforces what `[CERT]`

| Guard | Enforced by | Cite |
|---|---|---|
| Unknown user / slot-name spoof | `getUser` returns null | `BUserService.java:590-602` |
| Disabled · locked-out · expired account | `canLogin(BUser)` | `BUserService.java:662-684` |
| Password expired (per-user `BUserPasswordConfiguration.expiration`) | `BAbstractAuthenticator.canLogin(BUser)` | `BAbstractAuthenticator.java:67-80` |
| Must-change-at-next-login | `BPasswordAuthenticator.requiresCredentialsReset()` → `getPasswordConfig().getForceResetAtNextLogin()` — **not** consulted by `validate`; the module decides whether to refuse a config login for such a user | bajadoc `BPasswordAuthenticator` + mapper `BPasswordAuthenticator.java:333-336` `[CERT-mapper, not re-read]` |
| Lockout accounting (5 failures within 30 s → locked 10 s; all on `BUserService`, per-user `clearLockOut` action) | **the caller**, via `authenticateOk/Failed` | `BUser.java:1120-1158`; defaults `lockOutEnabled=true`, `lockOutPeriod=10 s`, `maxBadLoginsBeforeLockOut=5` (1-10), `lockOutWindow=30 s` (bajadoc slots + guide `baja-UserService.txt:37-79` `[CERT-doc]`) |
| `OPERATOR_WRITE` on the target slot | **the framework**, inside `set` when a user-bearing Context is passed: `user.checkWrite(component, slot)` → `check(target, operatorWrite)` for `Flags.isOperator` slots, else `adminWrite`; throws `PermissionException` | `ComplexSlotMap.java:662-666` (vineflower); `BUser.java:1659-1671` |
| Scheme support | only `BPasswordCache` subclasses expose a local `validate`; LDAP binds a directory, SAML/OAuth redirect a browser — no local secret to check | `[INFER — hierarchy + devguide scheme model]` (= B803-G1) |

## 830.4 What a `null` Context really bypasses — an extension of B829 `[CERT]`

B829 established that `ComplexSlotMap.set:662` gates the `AuditEvent` on `context != null && context.getUser() != null`.
Reading the enclosing block (`:655-672`) shows the same `if` also wraps **`user.checkWrite(base.component, base.propertyPath[0])`**
and the `BIProtected` old-value check. Consequence: the client servlet's `parent.set(prop, toSet, null)`
(`BDashboardServlet.java:291` @a109249) is **neither audited nor permission-checked by Niagara** — today the ONLY write gate is
the module's own `DashboardRbacHelper.resolveOperatorWrite` (`:90-98`, `getUser` + `getPermissions((Context)user).has(OPERATOR_WRITE)`).
Passing the re-authenticated `BUser` as the Context therefore turns on TWO framework behaviours at once: enforcement
(`PermissionException` → the servlet maps it to 403) and attribution — `audit(base, user, "Changed", old, new)` (`:813-814`) →
`new AuditEvent(op, path, slot, old, new, user.getUsername())` (`:1687`), dispatched when `Nre.auditor != null` and the
component space class carries `@AuditableSpace` (`:1682-1685`). With `AuditHistoryService` installed (B829-G1 CLOSED) the
row in `/PANCCADIA/AuditHistory` names the **second operator**, not the kiosk user. This is exactly what R14/D8b needs and
what the R7 mirror will copy into `change_log` as `surface='servlet'` — surface B can attribute the real operator
**without** per-operator panel logins. `[ev: corpus B829]` `[ev: client BDashboardServlet.java:291, DashboardRbacHelper.java:90-98 @a109249]`

## 830.5 Per-session token and TTL `[CERT]` + recommendation `[INFER]`

- `WebOp.getRequest()` returns the servlet `HttpServletRequest` (`WebOp.java:185-188`); `getRequest().getSession()` is the
  standard servlet session — Niagara's web server keeps a per-browser session whose inactivity timeout is scheduled by
  `NiagaraSuperSession.updateSessionTimeout()/scheduleTimeout()` (`NiagaraSuperSession.java:249-262`, vineflower) from the
  logoff period. The station-side period is `BUserService.defaultAutoLogoffPeriod` = **15 min, MIN 2 min, MAX 4 h**
  (`BUserService.java:339`), overridable per user by `BUser.autoLogoffSettings` (`BUser.java:814`; `BAutoLogoffSettings`
  `autoLogoffEnabled/autoLogoffPeriod/useDefaultAutoLogoffPeriod`, bajadoc). Guide: "amount of time that a period of
  inactivity may last before a station connection is automatically disconnected" (`baja-UserService.txt:83-85` `[CERT-doc]`).
  The exact `getLogoffPeriod()` resolution chain inside `NiagaraSuperSession` was not read → **B830-G2**.
- **Recommendation for R14:** keep the config token **server-side in the module** (a map keyed by `HttpSession.getId()` +
  a random handle → `{username, issuedAt, lastActivity}`), with the module's own short absolute TTL and sliding inactivity
  (B803 §803.6 shape), revoked by `/config/logout` and by `HttpSessionListener`/session invalidation; never store the
  password. The station auto-logoff bounds the OUTER kiosk session; the module TTL must be shorter. Do not put the second
  identity into the container session as "the user" — the kiosk `WebOp.getUser()` must stay the kiosk user. `[INFER]`

## 830.6 What is impossible or unsupported

| Item | Verdict | Cite |
|---|---|---|
| `BUserService.authenticateBasic(...)` / `getAuthAgent()` | **removed in N4** — all authentication goes through the Authentication Service | devguide `security/authentication.txt:212-222` `[CERT-doc]` |
| A full scheme login mid-request via `BAuthenticationScheme.login(CallbackHandler)` | it is the JAAS entry (`new LoginContext("", null, handler, getLoginConfiguration()); lc.login()`), needs a `CallbackHandler`/LoginModule and yields a `LoginContext`, not a web session — heavy and not designed for an in-servlet re-check; use §830.2 | `BAuthenticationScheme.java:101-114` (vineflower) |
| Lockout accounting "for free" from `validate()` | none — caller's duty (§830.2 step 5) | `BUser.java:1120-1158`; zero grep |
| Re-verifying LDAP / SAML / OAuth / gauth users with `validate()` | not a `BPasswordCache` (or, for gauth, the second factor is not checked by `validate`) → `instanceof` guard → "unsupported scheme" | `[INFER]` (B803-G1; gauth → **B830-G3**) |
| `OrdTarget` as a user-carrying Context builder | it is the result of ORD resolution, every constructor needs a base Context; use the `BUser`/`BasicContext` | `OrdTarget.java` ctors `[CERT-mapper]` |
| `BasicContext.make(user)` static factory | does not exist; `new BasicContext(user)` / `(user, facets)` | `BasicContext.java:27-60` |
| A servlet-side `getPermissionsFor(target).has(OPERATOR_WRITE)` pre-check | not required once a Context is passed (the framework enforces); harmless if kept for a friendlier 403 before touching the station | `ComplexSlotMap.java:662-666` |

## 830.7 Implications for R14 / D8b (apply-ready shape) `[INFER — assembly]` on `[CERT]` parts

```java
// POST /dashboardpan/api/config/login  {username, password}   — kiosk request already authenticated (WebOp)
BUserService svc = (BUserService) Sys.getService(BUserService.TYPE);
BUser u = svc.getUser(unescape(username));                       // :590-602 → null on unknown
if (u == null || !svc.canLogin(u)) return 401;                    // :662-684 (disabled/locked/expired/pw-expired)
BAbstractAuthenticator a = u.getAuthenticator();                  // BUser.java:517
if (!(a instanceof BPasswordCache)) return 400 "unsupported scheme";
boolean ok = ((BPasswordCache) a).validate(password);             // BPasswordCache.java:87-95, no side effects
if (!ok) { u.authenticateFailed(svc); return 401; }               // BUser.java:1130-1158 → lockout policy
u.authenticateOk(svc);                                            // :1120-1123
token = configSessions.issue(req.getSession().getId(), u.getUsername());   // module-held, short TTL (§830.5)
// later, POST /api/setpoint with the token:
BUser op = svc.getUser(configSessions.userFor(token));            // re-resolve, never cache the BUser across requests
try { parent.set(prop, toSet, op); }                              // BUser IS a Context → checkWrite + AuditEvent(op.getUsername())
catch (PermissionException e) { return 403; }
```
RED pins the design must add (beyond the existing guard1-5): wrong password → 401 **and** `authenticateFailed` called (observable:
after `maxBadLoginsBeforeLockOut` failures `u.getLockOut()` is true and `canLogin` false); locked user → 401 with no `validate`
call; non-`BPasswordCache` authenticator → 400/unsupported, no cast; a user lacking `OPERATOR_WRITE` → 403 raised **by the
framework** (`PermissionException`), not by the helper; the audit row names the second operator (attribution pin, via a
recording `Auditor`/`AuditSink`); logout revokes; kiosk `WebOp.getUser()` unchanged after a config login. `schema-risk.sh`
stays SAFE (no slot touched). `[ev: corpus B830 §830.2-830.4]` `[ev: design D8]`

## 830.8 FUENTE 2 record (queries and zeros, literal)

Hits: devguide-search `BUserService` (authentication.txt L212/L222 — removed APIs), `BAuthenticationScheme` (L68/L72);
guide-search `auto logoff` (16 files; StationSecurity "Station Auto Logoff", LdapN4 baja-UserService), `session timeout`
(EngNotes client polling, OpcUa — not the web session), `session cookie` (HttpClient only). Bajadoc `class`/`slots` for the
nine classes above. **Zeros:** devguide-search `BPasswordAuthenticator`, `BPasswordCache`, `authenticate password validate`,
`lockout`; guide-search `lockout failed login attempts`, `autoLogoffPeriod`, `defaultAutoLogoffPeriod`, `web session`.
Tool note: the first pass FAILED (zsh did not word-split a `$H` variable holding "python3 path") — retried with the direct
invocation; the zeros above are from the successful pass, not the failure.

## 830.9 Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `BUser implements Context`; `getUser()` returns `this` | [CERT] | `BUser.java:300-303`, `:1049` |
| 2 | `BPasswordAuthenticator extends BPasswordCache extends BAbstractAuthenticator`; `validate` public final on `BPasswordCache` | [CERT] | `:81`, `:38`, `:87-95` |
| 3 | `validate` has no lockout side effect; `authenticateOk/Failed` are caller-invoked hooks with the window/threshold logic | [CERT] | zero grep; `BUser.java:1120-1158` + javadoc |
| 4 | `getUser` null semantics; `canLogin` gates; password-expiry in `BAbstractAuthenticator.canLogin` | [CERT] | `BUserService.java:590-602`, `:662-684`; `BAbstractAuthenticator.java:67-80` |
| 5 | null Context skips **both** `checkWrite` and the audit; user Context → `checkWrite` + `AuditEvent(user.getUsername())` | [CERT] | `ComplexSlotMap.java:655-672`, `:813-814`, `:1682-1690` |
| 6 | `checkWrite` → operatorWrite for OPERATOR slots, `PermissionException` | [CERT] | `BUser.java:1659-1671` |
| 7 | Lockout defaults 5 / 30 s / 10 s; auto-logoff 15 min (2 min–4 h) | [CERT] + [CERT-doc] | `BUserService.java:339`; bajadoc slots; `baja-UserService.txt:62-85` |
| 8 | `authenticateBasic/getAuthAgent` removed in N4 | [CERT-doc] | devguide `authentication.txt:212-222` |
| 9 | `scheme.login` = JAAS `LoginContext.login()` | [CERT] | `BAuthenticationScheme.java:101-114` |
| 10 | `WebOp.getRequest()` → `HttpServletRequest`; session timeout scheduled by `NiagaraSuperSession` | [CERT] | `WebOp.java:185-188`; `NiagaraSuperSession.java:249-262` |
| 11 | `requiresCredentialsReset` location; `OrdTarget` ctors | [CERT-mapper, not re-read] | mapper report |
| 12 | LDAP/SAML/OAuth/gauth not locally re-verifiable; token design; apply shape | [INFER] | §830.3, §830.5, §830.7 |
Tally: 10 [CERT]/[CERT-doc] · 1 [CERT-mapper] · 1 [INFER] · 0 unmarked.

## Connections
- **B803 §803.3** — CONFIRMED (the `BPasswordCache` cast is legal) and extended with steps 2/5/6 and the guard table.
- **B829** — EXTENDED: the `:662` gate also wraps `checkWrite`; a null Context bypasses permission enforcement, not only audit (pointer added in B829).
- **B813 / B804** — servlet hardening + AuditHistory as the station-side second record; S12 plan Part 3 (surface-B step-up) now has its legal path.
- C9 **R14 / design D8b / R7 mirror** — surface B attributes the real operator via the AuditEvent username; the R7 dedupe key `(ts, user, target, old, new)` gets a real `user`.

## Open gaps
- **B830-G1 (requires execution)**: on the live panel, confirm a config login + audited write shows the second operator's name in `/PANCCADIA/AuditHistory` while `WebOp.getUser()` stays the kiosk user (the B829-live pair).
- **B830-G2**: read `NiagaraSuperSession.getLogoffPeriod()` to pin the exact resolution `BUser.autoLogoffSettings` → `BUserService.defaultAutoLogoffPeriod` → `HttpSession.setMaxInactiveInterval`.
- **B830-G3**: gauth (two-factor) users — whether the TOTP second factor can be re-verified in-servlet (`gauth` palette classes not read).
