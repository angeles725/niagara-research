# B763 · The `-ux` servlet write-surface — build + test + secure playbook (OPERATOR_WRITE fail-closed, the pure RBAC seam, hand-rolled CSRF, audit) and a U5 re-grading

> **Scope**: the reusable BUILD-KIT playbook for a module `-ux` servlet WRITE endpoint — the five gates a write
> must pass, WHERE each is enforced in our real modules, and WHERE each is testable (the pure-vs-Baja seam from
> [B762]). Synthesis + application: it wires the already-documented authorization mechanics ([B648]/[B655] CS3
> write-auth, [B752] OPERATOR_WRITE RBAC contrast, [B558]/[B11] BPermissions model, [B58]/[B165] CSRF) onto the
> B762 test seams. Also re-grades the BUILD-STATE `U5` open issue against the code. Focus:
> `module-ux-testing-and-write-surface` (UXT2).
>
> **Sources**: FUENTE 3 our real modules (grep-verified this session): chihuahua-ux (`ChiRbacHelper`,
> `BChiServlet`, `ChiServletDispatch`, `ChiAuditHelper`, `ChiThresholdHelper`) under
> `Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-ux/`; DashboardPan-ux (`DashboardRbacHelper`,
> `BDashboardServlet`, `DashboardDispatch`) under `Cliente/Leon-Guanjuato/Dashboard/DashboardPan/DashboardPan-ux/`.
> FUENTE 1: [B648]/[B652]/[B653]/[B655] (chihuahua-source CS3), [B752]/[B753], [B165]/[B166], [B558]/[B11],
> [B58]/[B507], [B762] (the test seams). READ-ONLY. English (post-B115).

---

## 763.1 — The write-surface contract: five gates `[CERT]`
Every mutating `-ux` request must pass, in order, before it touches a slot. Our production module (chihuahua) and
DashboardPan both implement this shape; the gates are the reusable kit checklist:
1. **AuthZ** — caller has `OPERATOR_WRITE` (fail-closed).  2. **CSRF** — `X-Requested-With: XMLHttpRequest` present.
3. **Target scoping** — the write ORD is pinned/allowlisted, not arbitrary.  4. **Concurrency** — mutate under a
per-Ord lock (423 on contention).  5. **Audit** — record who/what/when/old→new, fire-and-forget.

## 763.2 — Gate 1: OPERATOR_WRITE, fail-closed `[CERT]`
Both modules resolve capability by reading the **`OPERATOR_WRITE` permission bit** off `BPermissions` (via
`BUserService.getUser → BUser.getPermissions`), NOT by role-name matching — chihuahua ADR D1:
*"canWrite = BPermissions.has(OPERATOR_WRITE)"* (`ChiRbacHelper.java:15-17`); DashboardPan mirrors it,
`perms != null && perms.has(BPermissions.OPERATOR_WRITE)` (`DashboardRbacHelper.java:98`, *"Trimmed from chihuahua
ChiRbacHelper"* :20). It runs as the **first line of every write handler**: `BDashboardServlet.java:198`
(`if (!DashboardRbacHelper.checkCanWrite(req,resp)) return;`); chihuahua wires the same guard into all 8 write
handlers.
**Deny-by-default at every branch**: no `BUserService` → false, user not found → false, no authenticated user → 401,
has-user-but-no-bit → 403, and the load-bearing **`catch(Exception) → return false`** (`DashboardRbacHelper.java:100`;
chihuahua ChiRbacHelper) — *"a broken station config causes a safe 403 rather than an open door."* One documented
residual: `OPERATOR_WRITE` is treated as a single GLOBAL capability; a category-scoped station could false-positive
`has()` — flagged for future `plants[]` work (matches [B752]).

## 763.3 — Gate 2: CSRF via a hand-rolled X-Requested-With check, inside the PURE route() `[CERT]`
These are plain `BWebServlet`s, so the framework `CsrfProtectedFilter` (which guards `/rpc/*` only, [B58]/[B507])
does NOT cover them — they hand-roll the guard. Crucially it lives in the **pure `route()` dispatcher**, not the
Baja servlet: `DashboardDispatch.java:123` reads `X-Requested-With` and returns `RouteAction.Redirect` (302) on miss
(:27); chihuahua does the same in `ChiServletDispatch` (POST + GET branches), exempting only a CSRF-probe endpoint.
Because the guard is in `route()`, it is unit-testable off-station (the B762 seam) — a real security check with a
real test, not a station-only assumption. [B653]: the server `X-Requested-With` is the operative guard; a token
flow is deferred.

## 763.4 — The pure-vs-Baja RBAC seam: chihuahua drew it, DashboardPan collapsed it `[CERT]`
This is the KEY build-kit finding, extending [B762] §762.3 to the authorization decision:
- **chihuahua isolates the DECISION as pure** — a documented *"TESTABILITY SEAM"* (`ChiRbacHelper.java:26-28`):
  `canWrite(boolean hasOperatorWrite)` (:54), `getRole(boolean)`, `buildCapabilityJson(...)`, `buildForbiddenJson()`
  are Baja-free and unit-tested (`ChiRbacHelperTest`); only the LOOKUP (`resolveOperatorWrite` → `BUserService`/
  `BPermissions`) is Baja-bound and *"validated by integration smoke, NOT WSL unit tests"* (:236-245).
- **DashboardPan collapsed the seam** — `DashboardRbacHelper` has NO `canWrite(boolean)`/`getRole` pure methods; the
  403 JSON is inlined and the whole class is Baja-bound. So DashboardPan's RBAC *decision* has no off-station
  test surface — the build-kit gap. The fix template = port chihuahua's pure `canWrite(boolean)`/`buildForbiddenJson`.

## 763.5 — Gate 5: audit trail `[CERT]`
chihuahua `ChiAuditHelper.buildEntry(user,action,ord,old,new,ts)` emits one JSON line (who/what/when/old→new),
fired fire-and-forget after every successful mutation, with a read side that merges module actions + native
`SecurityHistory` login events. **DashboardPan mirrors the WRITE-side audit** (`BDashboardServlet.java:287-297`,
same "audit failure MUST NOT fail the write" guard) — so the write record is equivalent; DashboardPan only lacks
the login-event / merged read view. (Matches [B655] readiness table.)

## 763.6 — U5 re-grading: the BUILD-STATE open issue is partly wrong `[CERT]`
BUILD-STATE says *"DashboardPan servlet generic write endpoint lacks OPERATOR-flag/whitelist check."* Against the code:
- **OPERATOR-flag half = NOT a gap.** DashboardPan runs `checkCanWrite` as the first line of `/api/setpoint`
  (`BDashboardServlet.java:198`) with identical `OPERATOR_WRITE` fail-closed logic — as gated as chihuahua.
- **Whitelist half = real, but SYMMETRIC with chihuahua's own setpoint.** DashboardPan `/api/setpoint` writes any
  resolvable property (`parent.set(prop,toSet,null)`, `BDashboardServlet.java:274`) with no slot allowlist — but
  chihuahua's `/api/setpoint` is ALSO allowlist-free; chihuahua's allowlist (`ChiThresholdHelper`) guards only the
  separate `/threshold` endpoints ([B652]). So "add an allowlist" is a BOTH-modules decision, not a DashboardPan
  regression.
- **DashboardPan is actually TIGHTER on ORD scope**: it pins every write under the service facade
  (`parentOrd = DashboardReader.SERVICE_ORD + "/" + parentRel`, `BDashboardServlet.java:241`) + a traversal reject,
  whereas chihuahua's setpoint resolves a raw station-wide body ORD.
- **DashboardPan is LOOSER on concurrency**: chihuahua takes a `ReentrantLock` and returns **HTTP 423 Locked** when
  `controlTick` holds it (`BChiServlet.java:769-784`); DashboardPan writes with no lock/423 — a concurrency-safety
  gap, not an authz gap.
**Verdict**: U5's "OPERATOR-flag" is CLOSED; the genuine deltas are (a) the lost pure-decision test seam §763.4,
(b) no per-Ord lock/423, (c) an optional setpoint allowlist (both modules), (d) no login-event audit view.

## 763.7 — Build-kit implications (proposed deltas — for the §18 retro)
- **`types/dashboard.md` — add "The write-surface: five gates" checklist** (§763.1) as the servlet-write authoring
  rule: (1) `checkCanWrite` first line = `OPERATOR_WRITE` fail-closed (deny on no-user / no-service / exception);
  (2) hand-rolled `X-Requested-With` guard IN `route()` (the framework CSRF filter covers `/rpc/*` only); (3) pin
  the write ORD under a `SERVICE_ORD` facade + traversal reject (or an explicit slot allowlist); (4) mutate under a
  per-Ord lock, return 423 on contention; (5) audit every mutation (who/what/when/old→new), fire-and-forget,
  audit-failure-never-fails-the-write.
- **`types/dashboard.md` — the pure RBAC seam** (§763.4): extract the auth DECISION as a Baja-free
  `canWrite(boolean)`/`getRole`/`buildForbiddenJson` seam (chihuahua ADR D1) so the write-auth logic is
  unit-testable off-station; `DashboardRbacHelper`'s collapsed Baja-only form is the anti-pattern to refactor.
- **DashboardPan punch-list note** (client-side, not kit): port the pure `canWrite` seam + add the per-Ord lock/423;
  re-grade the U5 issue text (OPERATOR-flag already enforced).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Both modules gate writes on the `OPERATOR_WRITE` bit (BPermissions.has), not role names | [CERT] | ChiRbacHelper.java:15-17 (ADR D1); DashboardRbacHelper.java:98 |
| 2 | checkCanWrite is the first line of the write handler(s), fail-closed incl. catch→false | [CERT] | BDashboardServlet.java:198; DashboardRbacHelper.java:46,61,87,94,100 |
| 3 | CSRF X-Requested-With guard lives in the PURE route() (302 on miss), not the framework filter | [CERT] | DashboardDispatch.java:27,123; framework CsrfProtectedFilter = /rpc/* only (B58/B507) |
| 4 | chihuahua documents a pure "TESTABILITY SEAM" (canWrite(boolean) etc.); DashboardPan has no pure canWrite | [CERT] | ChiRbacHelper.java:26-28,54; DashboardRbacHelper.java (no canWrite method) |
| 5 | DashboardPan pins writes under SERVICE_ORD (tighter ORD scope than chihuahua's raw body ORD) | [CERT] | BDashboardServlet.java:241,274 |
| 6 | U5 OPERATOR-flag half is closed; the real deltas are pure-seam / per-Ord-lock / allowlist-both / login-view | [CERT/INFER] | §763.6 cites; [CERT] on the enforcement, [INFER] on the verdict |
| 7 | Write-side audit is equivalent in both; DashboardPan only lacks the merged login-event read view | [CERT] | BDashboardServlet.java:287-297; ChiAuditHelper.java:122-149 |

**Tally**: 6 [CERT], 1 [CERT/INFER]. No unmarked claims. Load-bearing cites (OPERATOR_WRITE fail-closed both modules,
CSRF-in-route, TESTABILITY SEAM, SERVICE_ORD pinning) grep-verified inline this session; the audit/lock line
numbers are from the delegated sweep, this session, at the real paths.

## Connections
- **[B762]** — the test seams this playbook secures (route()→RouteAction; the pure-vs-Baja gradient §762.3 that
  §763.4 extends to the RBAC decision).
- **[B648]/[B655]/[B652]/[B653]** (chihuahua-source CS3) — the write-auth = OPERATOR_WRITE / audit / allowlisted
  thresholds / X-Requested-With facts this block cites and applies.
- **[B752]/[B753]** (OPERATOR_WRITE fail-closed RBAC contrast; DashboardPan = strongest RBAC recipe), **[B558]/[B11]**
  (N4 BPermissions model), **[B58]/[B507]/[B165]/[B166]** (framework CSRF `/rpc/*` vs hand-rolled servlet guard).

## Open gaps
- **UXT2-G1** — DashboardPan lacks the pure RBAC test seam (canWrite(boolean)); porting it is an IMPL task (client
  module), the kit delta is the PATTERN (§763.7).
- **UXT2-G2** — no per-Ord write lock / 423 in DashboardPan (concurrency safety, not authz).
- **UXT3** — `-wb` view PAINT (gx) off-station testability: determined station-only in [B762] §762.6 / UXT1-G4;
  folds into the §18 retro rather than earning its own block (keeps the focus honest/small). → focus ready to STOP.
