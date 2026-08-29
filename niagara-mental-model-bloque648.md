# Niagara N4 — chihuahua-source (CS3): the production module's write path DOES enforce authorization — `ChiRbacHelper.checkCanWrite` (BPermissions.OPERATOR_WRITE, fail-closed) is the first line of all 8 write handlers, with audit on every mutation — the correct inverse of mcpbridge's bypass

**Focus**: chihuahua-source · **Gap**: CS3 (ux servlet write-auth/RBAC) · **Session**: 2026-08-29 · **Block**: B648
**Sources** (`[CERT]` REAL source, not decompiled): `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-ux/src/com/angeles/chihuahua/ux/` — `BChiServlet.java`, `ChiServletDispatch.java`, `ChiRbacHelper.java`, `ChiAuditHelper.java`.

**Scope**: the security-critical question of the production module — does its servlet gate writes by AUTHORIZATION, not just authentication? Direct contrast to `mcpbridge` ([B643], authz bypassed). N4 permission model = [B11]/[B558] (REMIT).

---

## 648.1 Verdict: authorization is enforced correctly (the inverse of mcpbridge)

Chihuahua's ux write path does exactly what `mcpbridge` ([B643]) failed to do. `[CERT]` `ChiRbacHelper.java` — the design is explicit (documented ADRs):
- **ADR D1** (`:15-17`): capability = the **`OPERATOR_WRITE` permission BIT** from `javax.baja.security.BPermissions`, NOT role-name matching (`canWrite = BPermissions.has(OPERATOR_WRITE)`).
- **ADR D2** (`:20`): `checkCanWrite(req, resp)` is the SOLE server-side write gate.

`checkCanWrite` `[CERT]` `ChiRbacHelper.java:142-175`:
```java
static boolean checkCanWrite(HttpServletRequest req, HttpServletResponse resp) {
  // no user → 401
  if (remoteUser==null||isEmpty) { …SC_UNAUTHORIZED; return false; }      // :151-157
  String lookupUser = unescapeUsername(remoteUser);                        // :161  ($20→space)
  boolean hasWrite = resolveOperatorWrite(lookupUser);                     // BUserService.getUser→BPermissions.has(OPERATOR_WRITE)
  if (!hasWrite) { LOG "403 FORBIDDEN … lacks OPERATOR_WRITE"; SC_FORBIDDEN; return false; }  // :168-175
  return true;
}
```
`resolveOperatorWrite` `[CERT]` `ChiRbacHelper.java:256-289`: `BUserService.getUser(username)` → `user.getPermissions(ctx).has(BPermissions.OPERATOR_WRITE)`, and **fail-closed** — any exception logs and returns `false` (deny). Username is SlotPath-unescaped first (`$20`→space) so operator names with spaces aren't false-denied.

**It gates EVERY write handler as the first statement** `[CERT]` `BChiServlet.java` — 8 call sites, more than the initial sweep found:
```
:680 setpoint · :1321 alarmLatch · :1409 alarmUnlatch · :1526 upThreshold
:1641 carcamoThreshold · :1756 dataloggerThreshold · :2014 · :2080
```
each: `if (!ChiRbacHelper.checkCanWrite(req, resp)) return;` — before any body parse or mutation. A viewer (authenticated, no `OPERATOR_WRITE`) gets 403 and the handler returns; the mutation (`parent.set(...)`, `:792`) is only reached after the gate passes. This is real authorization, at the permission-bit level, applied uniformly.

---

## 648.2 Audit on every mutation

`[CERT]` `ChiAuditHelper.java` (ADR D3, `:26-27`) — every write also records an audit entry: a compact JSON-line `{ts,user,action,ord,oldValue,newValue}` (`buildEntry`, `:99-149`), with the setpoint handler capturing the OLD value BEFORE `parent.set` (`BChiServlet.java:789-793`). Stored in `BChiDashboardService.auditLog` — a **String-slot ring buffer, ~500 newline-delimited entries**. The audit read path (`GET /api/audit`) merges these module records with native `SecurityHistory` login events, excluding the hidden `api` service account (`:95-96`). Audit is **fire-and-forget** — a failed audit write does NOT fail the operation (`BChiServlet.java:802-816`).

---

## 648.3 Dispatch + CSRF (design-conscious, with a caveat)

`[CERT]` `ChiServletDispatch.route(method, path, headerLookup, paramLookup)` is a **pure, user-free function** returning a typed `RouteAction`. Unlike mcpbridge, being userless here is fine — the dispatcher only classifies (GET→read, POST→named write actions, unknown→405) and applies two guards: **path-traversal block** (`:390` rejects `..`/`\`/`\0`) and the **XHR-header CSRF guard** (`:397` — `POST /api/*` without `X-Requested-With: XMLHttpRequest` → redirect, not execute). User resolution + RBAC happen in the execution layer (BChiServlet) after routing. So the "userless dispatch" that was a red flag in mcpbridge is safe here because the RBAC gate is unconditionally applied downstream.

**CSRF caveat (honest)**: the guard is the XHR-header check, NOT an HMAC/session CSRF token. A `csrf-probe` endpoint (`BChiServlet.java:586-621`) detects whether the iSMA runtime supports `HttpSession` (it may not), and the XHR-header check is the chosen mitigation given that uncertainty. The XHR header blocks classic cross-origin form POSTs but is weaker than a per-request token; acceptable for the runtime constraint, worth a note.

---

## 648.4 Honest limitations (documented in source)

1. **Global permission, not category-scoped** `[CERT]` `ChiRbacHelper.java:278-281` — `OPERATOR_WRITE` is checked per-USER, not per-component-category. If the station configures category-scoped permissions, a partially-privileged user could over-admit. The source itself flags this pending a per-plant `plants[]` extension.
2. **`parent.set(prop, toSet, null)` uses ambient context, not `runAsUser`** (`BChiServlet.java:792`) — so N4's per-slot `canWrite` is not invoked; RBAC lives at the servlet gate, not the slot. Safe because the gate runs first, but it means the write executes with station privilege, not the user's — the same `null`-Context pattern to tighten if category scoping is added.
3. **Audit is plaintext + bounded** (~500 ring buffer, no HMAC/tamper-evidence) — consistent with N4's own audit posture ([B351]/[B396] REMIT), but old entries roll off and records aren't signed.
4. **Theme writes are RBAC-exempt** by design (`BChiServlet.java:997`) — cosmetic, still auth-gated (401 for anonymous). Reasonable.

---

## 648.5 Grade

chihuahua's write-auth is **well-engineered**: permission-bit RBAC, fail-closed, uniformly applied before mutation on all 8 write handlers, audited, with traversal + CSRF-ish guards and documented ADRs. It is a genuine positive exemplar — and the concrete proof that the shop CAN do write-authorization right (mcpbridge is the counterexample, [B643]). The improvements are refinements: category-scoped permissions (+ `runAsUser` context on `set`) for multi-tenant stations, and a stronger CSRF token if the runtime supports sessions.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | RBAC = BPermissions.has(OPERATOR_WRITE) via BUserService (ADR D1/D2), not role-name | [CERT] | ChiRbacHelper.java:15-20,256-283 | ✅ read verbatim |
| 2 | checkCanWrite: 401 no-user, 403 lacks OPERATOR_WRITE, fail-closed on exception; unescapes username | [CERT] | ChiRbacHelper.java:151-175,284-289 | ✅ read verbatim |
| 3 | checkCanWrite is first line of ALL 8 write handlers (680/1321/1409/1526/1641/1756/2014/2080) | [CERT] | BChiServlet.java (rg) | ✅ grep verbatim |
| 4 | mutation parent.set only after gate; setpoint captures oldValue pre-set | [CERT] | BChiServlet.java:789-793 | ✅ read |
| 5 | audit {ts,user,action,ord,old,new} JSON-lines → BChiDashboardService.auditLog ring ~500; fire-and-forget | [CERT] | ChiAuditHelper.java:26-27,99-149 | ✅ read |
| 6 | dispatch userless+pure; guards = traversal (390) + XHR-header CSRF (397); RBAC downstream | [CERT] | ChiServletDispatch.java:390-397 | ✅ read verbatim |
| 7 | limitations: global (not category) perm, set(null) ambient ctx, plaintext audit — documented | [CERT] | ChiRbacHelper.java:278-281 + BChiServlet.java:792 | ✅ read |

**Tally**: [CERT] ×7 · [INFER] ×0 · real-source block. Load-bearing RBAC/audit/dispatch citations token-checked verbatim (own grep of the source). Sweep under-counted call sites (6→actual 8) — driver re-grep corrected it.

## Connections

- **[B643]** — mcpbridge (authz BYPASSED); this is the correct inverse. **[B11]/[B558]** — N4 BPermissions/OPERATOR_WRITE model (REMIT). **[B636]/[B647]** — chihuahua jar audit + template. **[B351]/[B396]** — N4 audit-trail posture (plaintext/bounded).
- Forward: CS2 (rt control/protection), CS6 (reconcile audit-2026-05-06 — its findings may touch this), CS8 (production-readiness verdict).

## Gaps uncovered

- None new. Category-scoped RBAC + `runAsUser` context is a documented refinement, not an open gap (single-category stations are correctly served today). CS4 will cover the read-side helpers.
