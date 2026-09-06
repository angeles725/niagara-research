# C9 R14 / D8b — in-module config login for the HMI panel: apply package (client DashboardPan-ux, on top of PR6)

Author: companero (Fable), 2026-09-06 (rev 3 — investigador1 2nd-read fixes applied: MIR5 wording, guard-3 on the config-session user per lead d2857d1, /alarms/ack settled, CLW3/CLW4 regex constraints, anchors; rev 2 matched VERBATIM to QA's RED `qa/c9-s12-config-login` **`cc1c948`**:
`ConfigLoginGuardTest.java` CL1–CL11 + `ConfigLoginWiringTest.java` CLW1–CLW5 + SC13). Product decision (Cristian): ONE
shared kiosk login stays; a SECOND identifying login INSIDE DashboardPan before any write; writes run with THAT user's
Context so AuditHistory (and the R7 `change_log` mirror) name the real operator; NO credential storage. Legal Niagara API =
**B830** (`778d3b64b`). UX contract = the mock `tools/dashboard-preview.py --config-login` (re-pathed to `/api/config/*`).
Servlet anchors re-read at client **`a109249`**. `[ev: client qa/c9-s12-config-login cc1c948]` `[ev: corpus B830 §830.2-830.7]` `[ev: corpus B829]` `[ev: design D8/D8b]`

## 0. Dependencies + settled decisions
| # | Item | State |
|---|---|---|
| DEP-1 | **PR6 (R6, RED `qa/c9-s12-servlet` 4c18837) lands first** — creates `DashboardWriteGuards.evaluate(xhr, user, operatorWrite, ord, value, sink)` + guard1–5/4b + one-audit-entry and moves the write off the `null` Context. R14 sits on top: adds the config-session gate on the write path (CLW5) and swaps the Context to the SECOND operator (CLW3). If PR6 slips, R14 carries both REDs. |
| D-1 | **Endpoints = `POST /api/config/login`, `POST /api/config/logout`** (CLW1) — under the existing `/api/*` POST branch, so the XHR guard + 405 semantics come for free. NO `/api/config/session` is pinned; the chip derives its countdown from the login response (`ttl`) — a GET `/api/config/session` may be added as additive, unpinned. |
| D-2 | **Non-`BPasswordCache` authenticator → 401** (CL6), overriding B830 §830.7's 400 — and it must NOT throw (CL6 asserts no crash, no validate). |
| D-3 | **Session stores the USERNAME, never the `BUser`**; the servlet re-resolves the `BUser` on every write (CL9 pins the stored user is the re-authenticated one, not the kiosk user). |
| D-4 | **Audit failure never fails the write; a permission failure does** — `statusForWrite(permissionDenied, auditFailed)` (CL10/CL11). |
| D-5 | `Dashboard/build.gradle.kts:33` → `defaultModuleVersion("2.2.0")` (SC13). PR6 sets the same value → fragment-merge, no second bump. |
| D-6 | **guard 3 evaluates the CONFIG-SESSION user, not the kiosk user** (lead decision niagara-tools `d2857d1`; the kiosk may be viewer-only; CL10 authoritative). Write-path order becomes **1 XHR-302 → 2 kiosk-auth-401 → 6 session-403 → 3 OPERATOR_WRITE(config user)-403 → 4 value-400 → 5 ORD-400**. |
| D-7 | **`/alarms/ack` IS gated by `requireSession`** (lead decision; additive, QA adds a pin at GREEN-verify). Settled — no longer open. |

## 1. The legal call path (B830 §830.2 — cite in code; CLW2 greps the tokens in the servlet source)
| Step | Call | Cite | Rule |
|---|---|---|---|
| 1 | `BUserService svc = (BUserService) Sys.getService(BUserService.TYPE); BUser u = svc.getUser(name)` | `BUserService.java:590-602` | null on unknown → **401**, no validate (CL2) |
| 2 | `svc.canLogin(u)` | `BUserService.java:662-684`; `BAbstractAuthenticator.java:67-80` | disabled · lockOut · expired · pw-expired → **401**, NO `validate` (CL5) |
| 3 | `u.getAuthenticator() instanceof BPasswordCache` | `BUser.java:517`; `BPasswordAuthenticator.java:81`; `BPasswordCache.java:38` | not a password scheme → **401**, no cast, no validate, no throw (CL6, D-2) |
| 4 | `((BPasswordCache) a).validate(password)` | `BPasswordCache.java:87-95` → `BPassword.java:564-575` | **zero side effects**; false on mismatch (CL3 asserts exactly 1 validate call) |
| 5 | `u.authenticateOk(svc)` / `u.authenticateFailed(svc)` | `BUser.java:1120-1123`, `:1130-1158` | **the module calls these** (CL3: failed ×1, ok ×0; CL4: ok ×1) — lockout accounting is caller-invoked (5 bad / 30 s → 10 s lock) |
| 6 | `parent.set(prop, toSet, op)` with `BUser op` as the Context | `BUser.java:300-303`, `:1049`; `ComplexSlotMap.java:662-666`, `:1687` | framework does `op.checkWrite` (→ `PermissionException` → **403**, CLW4/CL10) AND `AuditEvent(…, op.getUsername())` |
| — | kiosk `WebOp.getUser()` | untouched | the second identity never becomes the container "user" (B830 §830.5) |
| — | http session id | `WebOp.getRequest().getSession().getId()` (`WebOp.java:185-188`) | the `ConfigSession` key; module TTL < station auto-logoff (MIN 2 min) |
Client precedent for the Context cast: `user.getPermissions((javax.baja.sys.Context) user)` `DashboardRbacHelper.java:95-97`.

## 2. The pure core — EXACTLY the RED's shape (`ConfigLoginGuardTest.java:16-30` @ cc1c948)
```java
interface Clock      { long now(); }
interface UserHandle { String username(); boolean canLogin(); boolean isPasswordCache();
                       boolean validate(String pw); void authenticateOk(); void authenticateFailed(); }
interface UserLookup { UserHandle find(String username); }          // svc.getUser -> null on unknown

final class ConfigSession {                                          // (Clock, long ttlMs)
  String issue(String httpSessionId, String username);               // one entry per http session; replaces
  String userFor(String httpSessionId);                              // null if none/expired; TOUCHES on a hit (sliding)
  void   revoke(String httpSessionId);
}
final class ConfigLoginGuard {                                       // (UserLookup, ConfigSession)
  static final int OK = 200, UNAUTHORIZED = 401, FORBIDDEN = 403;
  static final String REASON_LOGIN_REQUIRED = "config_login_required";
  int    login(String sid, String username, String password);        // 200 | 401
  int    logout(String sid);                                          // 200
  int    requireSession(String sid);                                  // 200 (renews) | 403
  String reason(int status);                                          // 403 -> "config_login_required"
  static int statusForWrite(boolean permissionDenied, boolean auditFailed);   // 403 if denied, else 200
}
```
**`login` order (the CL pins fix it):** blank/`find()==null` → 401 (no validate, CL2) · `!canLogin()` → 401 (no validate, CL5)
· `!isPasswordCache()` → 401 (no validate, no throw, CL6) · `!validate(pw)` → `authenticateFailed()` ×1 → 401 (CL3)
· else `authenticateOk()` ×1 → `sessions.issue(sid, username)` → 200 (CL4; `userFor(sid)` = the re-authenticated username, CL9).
`requireSession`: `userFor(sid)==null` → 403 else 200 (touch = renewal, CL8 sliding: activity at TTL/2 keeps it alive;
silence past TTL expires it). `logout` → `revoke` → next `requireSession` 403 (CL7). `statusForWrite(true, *)` = 403;
`statusForWrite(false, true)` = 200 (CL10/CL11). Pure Java, injected `Clock` (the RED's `FakeClock`).

## 3. File-level diff plan (DashboardPan-ux @ a109249 + PR6)
| # | File | Edit |
|---|---|---|
| F1 | NEW `src/com/angeles/DashboardPan/ux/ConfigSession.java` | §2, pure; `SecureRandom` token optional (the RED keys by http session id only — keep the token internal if added) |
| F2 | NEW `…/ConfigLoginGuard.java` (+ the three interfaces, package-private, same file or `ConfigLoginPorts.java`) | §2 verbatim |
| F3 | NEW `…/DashboardConfigAuth.java` — the thin Baja adapter | `UserLookup.find(name)`: unescape (`DashboardRbacHelper.unescapeUsername :68-70`) → `svc.getUser` → wrap the `BUser` in a `UserHandle` whose `canLogin()` = `svc.canLogin(u)`, `isPasswordCache()` = `u.getAuthenticator() instanceof BPasswordCache`, `validate(pw)` = `((BPasswordCache) u.getAuthenticator()).validate(pw)`, `authenticateOk/Failed()` = `u.authenticateOk(svc)/authenticateFailed(svc)`; `resolveOperator(username) → BUser` for write time (re-resolve per request, D-3). Fail-closed on exceptions (the `resolveOperatorWrite :76-100` pattern); never log the password. CLW2 greps these tokens in the servlet tree. |
| F4 | `DashboardDispatch.java` | POST `/api/*` branch (`:116-135`, after the `SetpointWrite` match): `"/api/config/login" → RouteAction.ConfigLogin.INSTANCE`, `"/api/config/logout" → RouteAction.ConfigLogout.INSTANCE`; two nested singletons beside `SetpointWrite` (`:59-63`) named EXACTLY `ConfigLogin` / `ConfigLogout` (CLW1 regex `class\s+ConfigLogin\b`). The existing XHR guard wraps them (non-XHR → 302). |
| F5 | `BDashboardServlet.java` `doPost` (`:132-160`) | dispatch `ConfigLogin → handleConfigLogin(req,resp)`, `ConfigLogout → handleConfigLogout`. Login: parse `{username,password}`, `guard.login(req.getSession().getId(), …)` → 200 `{ok,user,ttl}` or 401 `{"error":"auth"}`; logout → `guard.logout(sid)` → 200. |
| F6 | `BDashboardServlet.java` `handleSetpointWrite` (`:195`) | (a) FIRST on the write path (after the kiosk 401): `int st = guard.requireSession(sid); if (st != OK) → 403 {"error": guard.reason(st)}` (CLW5: the source must contain `config_login_required` + `requireSession(`); then **guard 3 (`OPERATOR_WRITE`) is evaluated for the CONFIG-SESSION user** — `DashboardRbacHelper.checkCanWrite` (`:33`) / `resolveOperatorWrite` (`:76-100`) take `sessions.userFor(sid)`, not `req.getRemoteUser()` (D-6, lead d2857d1); (b) after PR6's guards/coercion: `BUser op = configAuth.resolveOperator(sessions.userFor(sid));` and **replace `parent.set(prop, toSet, null)` (`:291`) with `parent.set(prop, toSet, op)`** — the literal `parent.set(prop, toSet, null)` must be GONE, and the CLW3 regex is `parent\.set\(\s*prop\s*,\s*toSet\s*,\s*[A-Za-z_][A-Za-z0-9_]*\s*\)`: **the third argument must be a BARE IDENTIFIER** (`op` passes; `(Context) op` or an inline `configAuth.resolveOperator(...)` FAILS) — resolve into a local first; (c) wrap the set: `try { parent.set(prop, toSet, op); } catch (PermissionException e) { resp.setStatus(HttpServletResponse.SC_FORBIDDEN); … }` — a `catch (PermissionException …)` whose body reaches `SC_FORBIDDEN` — the CLW4 regex is `catch\s*\(\s*PermissionException[^)]*\)[^}]*SC_FORBIDDEN` (DOTALL): **`SC_FORBIDDEN` must appear in the catch body BEFORE any `}`**, so keep that body FLAT (no nested `if {}` / lambda before `setStatus`), and a catch-all `Exception` must not precede it (a catch-all alone also fails `contains("PermissionException")`); (d) the module audit `svc.appendAudit(…)` at `:312` names `op.getUsername()`; keep it fire-and-forget (CL11: audit failure never fails the write — map through `statusForWrite(false, auditFailed)`). `coerceValue :357`, `parseDouble :403-407`, the numeric guard `:274-288` unchanged (PR6). |
| F7 | `src/rc/index.html` | the modal from the mock: wrap the two write call sites `fetch("/dashboardpan/api/setpoint", …)` at **`:1335`** and **`:1929`** — on `403 config_login_required` open the modal, HOLD the write, `POST /api/config/login {username,password}`, on 200 re-issue; `Cancelar` drops it; chip with countdown from the login `ttl` + `Salir` → `POST /api/config/logout`. Reuse `.card/.ch/.acts` (`:392-398`) + `:root` vars (from `:9`); targets ≥ 44 px. Do NOT ship the mock's change_log strip. |
| F8 | `Dashboard/build.gradle.kts:33` | `defaultModuleVersion("2.2.0")` (SC13; fragment-merge with PR6, same value) |
| F9 | tests | the RED's two files land as-is (`ConfigLoginGuardTest`, `ConfigLoginWiringTest`); GREEN = F1–F8. No extra fixtures. |

## 4. CL1–CL11 + CLW1–CLW5 + SC13 — the RED verbatim (what each fixes)
| Pin | Asserts |
|---|---|
| CL1 | `requireSession(SID)` == FORBIDDEN with no login; `reason(FORBIDDEN)` == `REASON_LOGIN_REQUIRED` == `"config_login_required"` |
| CL2 | unknown user → UNAUTHORIZED; `validateCalls == 0`; `userFor(SID)` null |
| CL3 | wrong password → UNAUTHORIZED; `validateCalls == 1`; `failedCalls == 1`; `okCalls == 0`; no session |
| CL4 | correct → OK; `okCalls == 1`, `failedCalls == 0`; `userFor(SID) == "alice"`; `requireSession` OK |
| CL5 | locked-out (`canLogin()` false) → UNAUTHORIZED; `validateCalls == 0` (canLogin gates BEFORE validate) |
| CL6 | `isPasswordCache()` false → UNAUTHORIZED, no `RuntimeException`, `validateCalls == 0` |
| CL7 | logout → OK; then `requireSession` FORBIDDEN; `userFor` null |
| CL8 | after TTL with no activity → FORBIDDEN; re-login then activity at TTL/2 → OK, and again later → OK (sliding) |
| CL9 | the session stores the re-authenticated username, never `"kiosk"` |
| CL10 | `statusForWrite(true, false)` == 403 (a permission failure is never swallowed) |
| CL11 | `statusForWrite(false, true)` == 200 (an audit failure never fails the write) |
| CLW1 | `DashboardDispatch.java` contains `/api/config/login`, `/api/config/logout`, `class ConfigLogin`, `class ConfigLogout` |
| CLW2 | the servlet tree contains the legal-path tokens (`getUser`, `canLogin`, `BPasswordCache`, `validate`, `authenticateOk`, `authenticateFailed` …) |
| CLW3 | `parent.set(prop, toSet, null)` ABSENT; the write runs with the re-resolved `BUser` as Context |
| CLW4 | `catch (PermissionException …)` reaching `SC_FORBIDDEN` |
| CLW5 | `config_login_required` present + `requireSession(` / `ConfigLoginGuard` on the write path |
| SC13 | `Dashboard/build.gradle.kts` `defaultModuleVersion("2.2.0")` |

## 5. change_log / R7 consequence — MIR5 (re-worded per D7 + investigador1)
D7 defines `config_session` as an **opaque session id** and pins it **NULL for surface B** (AuditHistory carries no
session id) — the mirrored row is `{ ts, user, target, old_value, new_value, surface: 'servlet', config_session: null }`
(D7 :64). After R14, the **identity** of a surface-B row = the `user` column = the AuditEvent's user (the SECOND operator,
now that the write runs with that `BUser` Context); **`config_session` STAYS NULL** — the station username must NOT be
written into it. Pre-R14 rows keep the kiosk user in `user` (what AuditHistory recorded then), never faked. MIR5 stands as
written in D7a; R14 changes what `user` contains, not the column contract. `[ev: design D7 :39, :64, :72-73]` `[ev: proposal :131]`

## 6. Gates
`schema-risk.sh` → **SAFE** (no slot touched); `vendorVersion` 2.2.0 (with PR6). `lint-servlet.sh` → clean/WARN-only (auth
gate = kiosk 401 + `requireSession` 403; `input-400` on blank login fields; `log-in-handler` logs outcomes WITHOUT the
password; `csrf-xrw-only` stays WARN — B803 §803.5 is NOT in C9, proposal :131). `lint-write-path.sh`, `bog-audit.sh`
unchanged. Retro slug `campaign9-config-login`; record OBSERVED CL1/CL3 flips + the B830-G1 live confirm.

## 7. Open (named, not absorbed)
- **B830-G1 (requires execution):** live panel — config login + write shows the second operator in `/PANCCADIA/AuditHistory`
  while `WebOp.getUser()` stays the kiosk user.
- **B830-G3:** gauth two-factor users take CL6's 401 path; TOTP re-check out of scope.
- **B830-G2:** exact station auto-logoff resolution — only bounds the module TTL choice. The TTL value is a PRODUCT call: a very short idle (≤ 90 s) is aggressive for a multi-room setpoint session; sliding renewal on every write mitigates it — pick with Cristian.
- A GET `/api/config/session` for the chip (additive; the RED does not pin it).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | interfaces/classes/constants/method contracts + CL1–CL11 semantics | [CERT] | `ConfigLoginGuardTest.java:16-154` @ cc1c948 |
| 2 | CLW1–CLW5 + SC13 source pins (paths, class names, `parent.set(prop, toSet, null)` absent, `catch (PermissionException …) … SC_FORBIDDEN`, `config_login_required` + `requireSession(`, 2.2.0) | [CERT] | `ConfigLoginWiringTest.java:37-79` @ cc1c948 |
| 3 | legal call path + lockout caller-invoked + BUser is a Context | [CERT] | B830 §830.2 table |
| 4 | servlet/dispatch/RBAC/gradle anchors | [CERT] | read @ a109249 this session |
| 5 | CL6 = 401 (not B830's 400) | [CERT] | RED CL6 + lead decision |
| 6 | TTL value, `/session` GET | [INFER] | recommendations / product call |
| 7 | anchors + CLW3/CLW4 regexes + login order re-verified by investigador1 at the a109249 worktree; guard-3 config-user order = lead d2857d1 | [CERT] | 2nd read 2026-09-06 |
