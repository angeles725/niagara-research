# C9 PR6 / S12-B — `DashboardWriteGuards.evaluate` seam + real-Context set: apply package (client DashboardPan-ux)

Author: companero (Fable), 2026-09-06. Contract VERBATIM from QA's RED `qa/c9-s12-servlet` **`4c18837`** (one file:
`Dashboard/DashboardPan/DashboardPan-ux/srcTest/test/com/angeles/DashboardPan/ux/DashboardWriteGuardsTest.java`, 126 lines;
worktree `Leon-Guanjuato-worktrees/c9-s12-servlet`). Anchors counted at the **a109249** worktree. B829-G2 answer (which Context
the servlet passes) from B829/B830. R14 (`feat/c9-s12-hmi-config-login`, RED cc1c948) sits ON TOP of this PR — see §5.
`[ev: RED 4c18837]` `[ev: corpus B829 §829.4 / B829-G2]` `[ev: corpus B830 §830.2]` `[ev: S12 plan 1ecdf437c §4]`

## 0. Settled
- Pure seam, no Baja: `DashboardWriteGuards` is plain Java (the RED runs under `run-pure-test.sh`, no station).
- Guard order = the plan's (1 XHR-302 → 2 kiosk-401 → 3 OPERATOR_WRITE-403 → 4 value-400 → 5 ord-400 → 200 + ONE audit entry).
  Guards 1/2/3 are decided OUTSIDE the seam today (dispatch + `checkCanWrite`); the seam re-decides them from booleans so the
  ORDER and the "no audit on any rejection" invariant are testable. Guards 4/5 are regression pins (a109249 already enforces
  them at `:274-288` and `:224`); the mutation is the parseDouble-swallow (→ silent 0.0 → WG4 flips back to 200).
- **PR6 passes the KIOSK user's `BUser` as the Context** (B829-G2, §3): the request's `getRemoteUser()` → unescape →
  `BUserService.getUser(name)`. That single change makes the framework audit fire (`ComplexSlotMap.set:662` needs
  `context.getUser() != null`) and makes `op.checkWrite` (→ `PermissionException`) real. PR6b/R14 SWAPS that `BUser` for the
  re-authenticated second operator (CLW3) — the removal of the literal `parent.set(prop, toSet, null)` is PR6b's pin, but
  PR6 already removes it (the null is what B829-G2 closes); PR6b then only changes WHICH `BUser` is resolved.

## 1. The contract (verbatim, `DashboardWriteGuardsTest` @ 4c18837)
| Pin | Call (`eval(xhr, user, operatorWrite, ord, value, sink)`) | Expect |
|---|---|---|
| seam `:51-53` | `static int DashboardWriteGuards.evaluate(boolean xhr, String user, boolean operatorWrite, String ord, String value, AuditSink sink)` | HTTP status int |
| sink `:45-48` | `DashboardWriteGuards.AuditSink` = functional interface `(user, ord, oldVal, newVal)` (4 args, lambda-able; `void`) | called ONLY on the 200 path |
| WG1 `:57-63` | `xhr=false` | **302**, sink count 0 |
| WG2 `:66-72` | `user=null` (xhr true) | **401**, 0 |
| WG3 `:75-81` | `operatorWrite=false` | **403** fail-closed, 0 |
| WG4 `:84-90` | `value="abc"` | **400** (never a silent 0.0), 0 |
| WG4b `:93-99` | `value=""` and `value="NaN"` | **400** both, 0 |
| WG5 `:102-108` | `ord="../secret"` and `ord="Cuarto1\\x"` | **400** both, 0 |
| OK `:111-125` | `(true,"bob",true,"Cuarto1/setpoint","4.0")` | **200**; sink called EXACTLY once with `user=="bob"`, `ord=="Cuarto1/setpoint"` |
| mutation (K13) `:36-38` | coerce non-numeric to 0.0 instead of 400 | WG4 → 200 (flips) |
Note the RED never asserts old/new values (the seam has no station to read `current`); `oldVal`/`newVal` are passed through as
strings — `oldVal` may be `null` in the pure path.

## 2. File-level diff plan (`Dashboard/DashboardPan/DashboardPan-ux/src/com/angeles/DashboardPan/ux/`)
| # | Anchor @ a109249 | Edit |
|---|---|---|
| F1 | NEW `DashboardWriteGuards.java` (same package; package-private or public final class, private ctor) | `interface AuditSink { void append(String user, String ord, String oldVal, String newVal); }` + `static int evaluate(boolean xhr, String user, boolean operatorWrite, String ord, String value, AuditSink sink)`: `if (!xhr) return 302; if (user == null \|\| user.isEmpty()) return 401; if (!operatorWrite) return 403; if (!isValidOrd(ord)) return 400; if (!isFiniteNumber(value)) return 400; sink.append(user, ord, null, value); return 200;` — `isValidOrd` = non-empty, no `..`, no `\\`, no NUL, has a `/` with a non-empty last segment (mirrors `:222-238`); `isFiniteNumber` = `JsonUtil.parseFiniteDouble(value).isPresent()` if `JsonUtil` is Baja-free (it is a plain helper — confirm at apply; else inline `Double.parseDouble` + `!isNaN && !isInfinite` in a try/catch that returns false). Order matters: the ord check BEFORE the value check only affects which 400 a doubly-bad request gets; the RED never sends both bad. Constants `HttpServletResponse.SC_*` must NOT be referenced (pure class): use literals 302/401/403/400/200. |
| F2 | `BDashboardServlet.handleSetpointWrite` `:195` | keep `checkCanWrite` (`:198`) as the live guard 2/3 source, then compute the seam inputs once: `boolean xhr = "XMLHttpRequest".equals(req.getHeader("X-Requested-With"))` (already true here — dispatch redirected otherwise); `String kioskName = DashboardRbacHelper.unescapeUsername(req.getRemoteUser())`; `boolean opWrite = DashboardRbacHelper.resolveOperatorWrite(kioskName)` (or the boolean `checkCanWrite` already computed); call `int st = DashboardWriteGuards.evaluate(xhr, kioskName, opWrite, relOrd, value, sink)` where `sink` = the existing fire-and-forget audit (`svc.appendAudit(JsonUtil.buildAuditEntry(user, "setpoint", ord, oldVal, newVal, now))`) wrapped in the same try/catch (`:303-317`); `if (st != 200) { resp.setStatus(st); …error JSON…; return; }` — **but the station write itself stays in the servlet** (the seam has no Baja): resolve parent/prop (`:243-268`), read `current` (`:271`), coerce (`:290`), then §3's set, THEN let the sink append with the real `oldValStr`. Practical shape: evaluate with a sink that only RECORDS the decision, and after a successful set call the real audit once — or pass a sink that performs the audit and call `evaluate` AFTER the set has succeeded (simplest: the seam decides status; the servlet orders set → audit). Either way: ONE audit entry per 200, ZERO on any rejection (WG1-5), and the existing `:274-288` numeric guard and `:224` traversal guard become the seam's (delete the duplicates or keep them as belt-and-braces — the RED does not care; `lint-servlet.sh` prefers one place). |
| F3 | `parent.set(prop, toSet, null)` `:291` | → `BUser op = DashboardRbacHelper.resolveUser(kioskName)` (new small helper next to `resolveOperatorWrite` `:76-100`, returning the `BUser` from `BUserService.getUser`) then `try { parent.set(prop, toSet, op); } catch (javax.baja.security.PermissionException e) { resp.setStatus(HttpServletResponse.SC_FORBIDDEN); out.print("{\"error\":\"forbidden\"}"); out.flush(); return; }` — `BUser implements Context` (`BUser.java:300-303`, R14 package row 6); precedent cast in this codebase: `user.getPermissions((javax.baja.sys.Context) user)` `DashboardRbacHelper.java:97`. Keep the catch body FLAT with `SC_FORBIDDEN` before any `}` (R14's CLW4 regex will read this same code later). |
| F4 | `:293-317` audit | `auditUser` = `op.getUsername()` (falls back to the unescaped remote user); unchanged fire-and-forget; this is the module's JSON-lines audit, DISTINCT from the framework `AuditHistory` record that §3 now produces — both name the same user. |
| F5 | `Dashboard/build.gradle.kts:33` | `defaultModuleVersion("2.1.1")` → `("2.2.0")` (SC13; PR6b/R14 sets the same value → fragment-merge, no second bump). NOTE: the proposal table says "2.0.3 → 2.2.0"; the GROUP file at a109249 reads **2.1.1** (module-version key memory: group `build.gradle.kts`, not the module `.kts`). |
| F6 | tests | the RED lands as-is. GREEN run (WSL): `KIT/toolbelt/run-pure-test.sh Dashboard/DashboardPan/DashboardPan-ux com.angeles.DashboardPan.ux DashboardWriteGuards DashboardWriteGuardsTest` (see the W2 command list). `DashboardDispatchTest` (existing) unchanged. Live guards 1/2/3 + the AuditHistory record: station smoke only (B829-live gate), never reported green from WSL. |

## 3. Which Context PR6 passes, and how PR6b swaps it (B829-G2)
| | PR6 (this PR) | PR6b / R14 (on top) |
|---|---|---|
| `op` | the KIOSK user: `BUserService.getUser(unescape(req.getRemoteUser()))` — the station user the panel is logged in as | the SECOND operator: `configAuth.resolveOperator(sessions.userFor(sid))` (B830 §830.2 path: `getUser → canLogin → BPasswordCache.validate`) |
| framework audit | fires: `AuditEvent(…, op.getUsername())` at `ComplexSlotMap.java:1687` because `context.getUser() != null` (`:662`) — names the kiosk user (all panel writes look alike) | names the re-authed operator (the point of R14) |
| permission | `op.checkWrite` real → `PermissionException` → 403 (guard 3 now enforced by the framework too) | same, for the second operator |
| `parent.set` literal | `parent.set(prop, toSet, op)` — the `null` literal is GONE here | unchanged shape; CLW3 regex only requires a bare identifier as 3rd arg — `op` already satisfies it; PR6b changes how `op` is resolved (one line) |
| `config_session` | n/a (no session yet) | NULL in the mirror stays (MIR5) — the session id is never put in the framework record |
So PR6b's "removal pin" CLW3 will already be green after PR6; what PR6b adds is the `requireSession(` gate (CLW5) and the
operator swap. If PR6 slips, R14 carries both (R14 DEP-1).

## 4. Gates
`schema-risk.sh` → **SAFE** (no slot touched — `DashboardWriteGuards` is not a BComponent). `lint-servlet.sh` on the ux src:
the `parseDouble` catch→0.0 at `:403-407` keeps its catch-no-400 WARN (defence-in-depth; the seam owns the 400) — record the
count. `lint-structure.sh` L7. `run-pure-test.sh` exit 0 with 7 tests. Mutation (K13): restore the swallow in `evaluate` →
WG4 200 (record OBSERVED). Version 2.2.0 on `:33`.

## 5. Sequencing
PR6 → R14 (`feat/c9-s12-hmi-config-login`, worktree `c9-config-login`) → PR7 mirror only matters once AuditHistory has
user-named rows (PR6 makes that true for the kiosk user; R14 for the real operator).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | seam signature, sink arity, WG1-5/OK expectations, mutation | [CERT] | RED :45-53, :57-125, :36-38 @ 4c18837 |
| 2 | anchors :195 :198 :222-238 :243-268 :271 :274-288 :290 :291 :293-317 :403-407 | [CERT] | sed @ a109249 worktree (2026-09-06) |
| 3 | group gradle :33 = 2.1.1 today | [CERT] | `Dashboard/build.gradle.kts:33` @ a109249 (proposal's 2.0.3 is stale) |
| 4 | BUser is a Context; audit gate needs a user; PermissionException path | [CERT] | B829 §829.4, B830 §830.2; R14 package rows 6/33 |
| 5 | `JsonUtil.parseFiniteDouble` is Baja-free | [INFER] | confirm at apply (else inline) |
| 6 | run-pure-test.sh args order | [CERT] | build-verify.md:108 |
