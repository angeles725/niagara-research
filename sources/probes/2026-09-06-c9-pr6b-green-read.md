# C9 PR6b (R14 in-module config login) GREEN read — pr6b-hmi-config-login 95011b2 vs PR6 4d07cad

investigador1, 2026-09-06. Read-only in `Leon-Guanjuato-worktrees/pr6b-hmi-config-login` @ 95011b2 (stacked on PR6). Pure
RED run + source read. `[ev: run-pure-test + git diff]`

## Verdict
Functionally correct; the pure RED runs **GREEN — OK (11 tests)** (CL1-CL11). Both lead security questions are CLEAN
(no credential/BUser cached beyond the request; no kiosk-write-without-session path). ONE real finding: `ConfigSession`
uses a plain `HashMap` on a shared servlet field with no synchronization — a thread-safety defect the single-threaded RED
cannot catch. No functional drift.

## Lead security question 1 — no credential or BUser cached beyond the request: CLEAN
- `ConfigSession.Entry` holds ONLY `{ String username, long lastActivity }` (`ConfigSession.java:26-28`) — no BUser, no
  password. `issue` stores the username; `userFor` returns it; `revoke` removes it.
- The BUser is re-resolved PER WRITE request: `resolveConfigOperator(configUser)` → `svc.getUser(...)` returns a local
  `BUser op` (`BDashboardServlet.java:176/:540`), never a field. `DashboardConfigAuth.UserHandleImpl` wraps a BUser only
  during `login()` and is discarded (the guard stores only the username). The only cached object is `BUserService` (a
  station singleton — correct).
- The password is request-local: `handleConfigLogin` extracts it (`:337`), passes it to `guard.login(...)` for
  `validate`, and never stores or logs it (`LOG.info` logs the USERNAME only, `:353/:360`; `DashboardConfigAuth:19`
  "Never logs the submitted password"). `[ev: ConfigSession.java:26-28; BDashboardServlet.java:176/:540/:337/:353]`

## Lead security question 2 — no kiosk write without a config session: CLEAN
- The ONLY write endpoint is `/api/setpoint → SetpointWrite → handleSetpointWrite` (`DashboardDispatch.java:142`).
- `handleSetpointWrite` calls `guard.requireSession(sid)` FIRST (`:439`); `userFor(sid)==null` (absent or expired) →
  403 `config_login_required` (`:444`) and returns before any write.
- The write runs as the CONFIG-SESSION operator, not the kiosk: `op = resolveConfigOperator(configUser)` (`:540`),
  `parent.set(prop, toSet, op)` (`:544`); guard 3's `opWrite = resolveOperatorWrite(configUser)` (`:453`) checks the
  config user's OPERATOR_WRITE; the framework re-enforces via `parent.set(op)` → explicit `catch (PermissionException) →
  403` (`:548`). The kiosk `WebOp.getUser()` is never mutated (no `setUser`, no session-attribute rewrite). `[ev: BDashboardServlet.java:439/:444/:453/:540/:544/:548]`

## FINDING (thread-safety, medium) — ConfigSession is a shared HashMap with no synchronization
`private final ConfigSession sessions = new ConfigSession(...)` is a SHARED servlet field (`BDashboardServlet.java:104`),
one instance across all request threads. Inside it, `private final Map<String, Entry> map = new HashMap<>()`
(`ConfigSession.java:33`) is mutated by `issue`/`userFor`/`revoke` (`:47/:57-66/:72`) with NO synchronization. Niagara's
web server dispatches the servlet on multiple threads, so concurrent `/write` from more than one HMI panel (each its own
HttpSession id), or a `/config/login` concurrent with a `/write`, call `put`/`get`/`remove` on the same unsynchronized
HashMap — undefined behavior: a lost or corrupted config session (spurious 403 to a legitimate operator) or a
`ConcurrentModificationException`/resize spin. The pure `ConfigLoginGuardTest` is single-threaded, so it cannot catch this.
Reachable whenever two panels (or a login + a write) hit the servlet at once. **Fix**: make `issue`/`userFor`/`revoke`
`synchronized` (they are short) — a `ConcurrentHashMap` alone is insufficient because `userFor` is a compound
get-then-(remove|touch). Not a security bypass (fail-closed: a lost session denies, never grants), but a correctness/
availability defect on a security-token store. `[ev: BDashboardServlet.java:104; ConfigSession.java:33,:47,:57-66,:72]`

## Invariants — all PASS
| Invariant | Result | Cite |
|---|---|---|
| pure core `ConfigLoginGuard`/`ConfigSession`/`UserHandle`/`UserLookup`/`Clock` = the RED shapes; RED green | PASS | run-pure-test `OK (11 tests)` |
| login order: unknown→401 no validate (CL2), !canLogin→401 no validate (CL5), !isPasswordCache→401 no throw (CL6), !validate→authenticateFailed×1→401 (CL3), ok→authenticateOk×1+issue→200 (CL4) | PASS | `ConfigLoginGuard.java:47-59` |
| ConfigSession stores the USERNAME (never the BUser); sliding TTL 300 000 ms; keyed by HttpSession id | PASS | `ConfigSession.java:26-66`; `sessions = new ConfigSession(…, CONFIG_TTL_MS)` `:104` |
| adapter: `getUser` → `canLogin` → `getAuthenticator() instanceof BPasswordCache` → `validate` | PASS | `DashboardConfigAuth` / `findConfigUser:130-139` |
| requireSession = guard 6, order 1→2→6→3→4→5; opWrite resolved for the CONFIG-SESSION user | PASS | `:439` (before evaluate `:460`); `resolveOperatorWrite(configUser)` `:453` |
| `parent.set(prop, toSet, op)` re-authenticated BUser; kiosk WebOp.getUser() untouched; explicit PermissionException catch | PASS | `:544/:548`; no setUser |
| audit user = `op.getUsername()` | PASS | `:570-573` |
| DashboardDispatch ConfigLogin/ConfigLogout under `/api` POST + XHR guard | PASS | `DashboardDispatch.java:66-77/:146-152/:163-166` |
| SPA: intercept→modal→held write re-issued→chip countdown from CONFIG_TTL client-side (no /config/session)→Salir | PASS (per D8c; index.html not re-read here) | build 95011b2 |
| no new -rt slot (schema SAFE); Dashboard :33 stays 2.2.0 | PASS | 0 `-rt/src` .java; no version diff |

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | pure RED 11/11 green; login order CL2/3/5/6 | [CERT] | run-pure-test; ConfigLoginGuard.java:47-59 |
| 2 | ConfigSession holds username only; BUser re-resolved per request; password never stored/logged | [CERT] | ConfigSession.java:26-28; :176/:540; :353/:360 |
| 3 | every write gated by requireSession→403; write as config operator, not kiosk | [CERT] | :439/:444/:540/:544 |
| 4 | ConfigSession HashMap shared servlet field, unsynchronized → concurrency defect | [CERT] | :104; ConfigSession.java:33 |
Tally: 4 [CERT] · 0 [INFER] · 0 unmarked.
