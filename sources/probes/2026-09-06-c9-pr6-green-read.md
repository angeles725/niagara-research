# C9 PR6 (S12-B servlet guards) GREEN read — feat pr6-servlet-guards 4d07cad vs a109249

investigador1, 2026-09-06. Read-only in `Leon-Guanjuato-worktrees/pr6-servlet-guards` @ 4d07cad. Source read (client Java,
no WSL run harness). `[ev: git diff a109249..4d07cad]`

## Verdict
Feature matches D8/D8a/D8b on every functional invariant; the lead's two specific checks (op-shadowing, XHR-302 reality)
both resolve favourably. One hygiene finding (committed build artifacts) and one defence-in-depth note (the seam's 302 is
non-functional but unreachable). No functional drift.

## Functional invariants — all PASS
| Invariant | Result | Cite (BDashboardServlet.java / DashboardWriteGuards.java) |
|---|---|---|
| `evaluate(...)` is the FIRST/primary guard in handleSetpointWrite; `checkCanWrite` removed | PASS | evaluate call `:227`; old `checkCanWrite` line deleted |
| status mapped 302/401/403/400(default)/200; rejection `return`s before the write | PASS | `if (st != 200)` switch `:231-241` (302/401/403/default→400, `out.flush(); return`) |
| guard order 1 xhr→302, 2 auth→401, 3 opWrite→403, **6 config stub (always passes)**, 4 value→400, 5 ord→400, else 200 | PASS | `DashboardWriteGuards.java:64/67/70/72/78/82/86` |
| D8a numeric guard kept as regression protection AFTER the 200 (type-scoped; boolean/enum/string not rejected) | PASS | `parseFiniteDouble` `:290`; comment `:285-289` |
| `parent.set(prop, toSet, op)` where `op` = `resolveUser(kioskName)` (`javax.baja.user.BUser`), the null literal gone | PASS | `op` resolved `:306`, set `:309` |
| explicit `catch (PermissionException) → SC_FORBIDDEN`, inner, BEFORE the outer catch-all | PASS | inner catch `:311-318` returns 403; outer `catch (Exception)` `:347` |
| audit-append failure never fails the write (gated on `sinkFired`, wrapped in `catch (Exception auditEx)`) | PASS | `if (svc != null && sinkFired[0])` `:333`, `catch (auditEx)` `:342` |
| `resolveOperatorWrite` widened `private`→package-private (additive); new `resolveUser` package-private | PASS | RbacHelper diff (`static` both) |
| BUser-as-Context is the same relationship RbacHelper uses | PASS | `set(prop,toSet,op)` op is BUser (implements Context); cf. RbacHelper `:97` `(Context) user` |
| no slot change (schema-neutral; schema-risk SAFE) | PASS | no `@NiagaraProperty` in the diff |
| `Dashboard/build.gradle.kts:33` 2.1.1 → **2.2.0** | PASS | gradle diff |

## Lead check 1 — `op` shadowing: RESOLVED (no collision)
`doPost(WebOp op)` (`:133`) has a `WebOp op` parameter, but `handleSetpointWrite(HttpServletRequest req, HttpServletResponse
resp)` (`:196`) takes NO `op` — doPost calls it with `(req, resp)`, so the `WebOp op` never enters that scope. The local
`javax.baja.user.BUser op` (`:306`) is a clean method-local; nothing shadows it. Confirmed. `[ev: :133 vs :196/:306]`

## Lead check 2 — XHR-302: real at DISPATCH, redundant + non-functional in the seam
The REAL XHR guard is `DashboardDispatch.route` (unchanged in PR6 — not in the diff): a non-XHR `/api/*` POST is
`sendRedirect`ed to home BEFORE routing to `SetpointWrite`, so `handleSetpointWrite` is only reached for XHR requests and
`evaluate`'s `xhr` is always true there. So `evaluate`'s `!xhr → 302` (`:64`) is redundant defence-in-depth, **unreachable
for /api/setpoint** in production. Minor: if it were ever reached, `doPost` maps `st==302` to `resp.setStatus(302)` + a JSON
body with **no `Location` header** — a bare, non-functional 302. Not a defect (dispatch owns the real redirect), but the
seam's 302 is a dead, non-redirecting branch — either wire a real `sendRedirect` there or document it as pin-only. The
`lint-servlet` `csrf-xrw-only` WARN (X-Requested-With as the sole CSRF defence) is PRE-EXISTING and unchanged; B803 §803.5
(`x-niagara-csrfToken`) is a C10 seed, so the 4th WARN is not a PR6 regression. `[ev: DashboardDispatch unchanged; :231-236]`

## Hygiene finding — 13 build artifacts committed
The diff commits 13 files under `build/` incl. `DashboardPan-rt/build/tmp/compileJava/previous-compilation-data.bin`
(gradle INCREMENTAL CACHE) and `DashboardPan-ux/build/classes/java/main/**/*.class`. A root `.gitignore` exists but does not
cover these. `build/libs/*.jar` + `writeModuleXml/module.xml` may be the client's deploy convention (the deployed RAR ships
`build/libs` jars), but `build/tmp/` and `build/classes/` are pure build cache that will churn on every build and pollute
diffs. Recommend gitignoring `**/build/tmp/` and `**/build/classes/` (keep `build/libs/*.jar` + `module.xml` if deploy
depends on them). Hygiene, not blocking. `[ev: git diff --name-only a109249..4d07cad | grep build/]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | evaluate first/primary; order + status map; D8a kept after 200 | [CERT] | :227/:231; DashboardWriteGuards :64-86; :290 |
| 2 | parent.set(op=BUser); explicit PermissionException→403 inner before catch-all | [CERT] | :306/:309; :311-318 vs :347 |
| 3 | no WebOp op shadowing in handleSetpointWrite; resolveUser/resolveOperatorWrite package-private | [CERT] | :196 sig; RbacHelper diff |
| 4 | XHR-302 real at dispatch (unchanged), seam's 302 unreachable + no Location | [CERT] | dispatch not in diff; :231-236 |
| 5 | 13 build artifacts committed incl. build/tmp cache + build/classes | [CERT] | git diff --name-only |
Tally: 5 [CERT] · 0 [INFER] · 0 unmarked.
