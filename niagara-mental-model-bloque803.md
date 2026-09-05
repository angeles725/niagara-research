# B803 · Step-up / re-authentication for a critical dashboard write — Niagara ships NO core step-up primitive; the clean `-ux` pattern re-verifies the session user through their auth scheme (and SAML users cannot be re-verified mid-session) `[CERT]`

> **Scope**: the user asked for an "internal login" so that changing a critically important control requires an explicit
> credential check. This block establishes what Niagara actually ships for step-up (answer: `Flags.CONFIRM_REQUIRED`
> is a UX-only confirm; the `electronicSignature` third-party module is the only true credential step-up), the clean
> SERVER-SIDE re-auth path a custom `-ux` servlet should use, the hard SAML/IdP constraint, a CSRF correction, and a
> copy-ready "critical-write with step-up auth" design sketch for `types/dashboard.md`.
>
> **Sources**: FUENTE 3 (read-only, file:line [CERT], 4.14.0.162 `organized/…/vineflower`) — `javax.baja.sys.Flags`/
> `BFacets`, `javax.baja.naming.OrdTarget`, `javax.baja.user.BUserService`/`BUser`, `javax.baja.authn.BAuthenticationScheme`,
> `javax.baja.web.CsrfUtil`, the `electronicSignature` module (`com.tridiumx.ps.model.Helper`/`BSecuredNumericWritable`,
> `com.secured.history.BSecuredTrendRecord`), `gauth`/`saml` scheme classes. FUENTE 1 (REMITTANCE, cited not re-derived):
> [B350]-[B356] (e-signature subsystem), [B558]-[B566] (RBAC/permissions), [B763] (the 5-gate write-surface + canWrite
> seam), [B776] (action protection flags), [B58]/[B507] (CSRF), [B777] (auth-scheme SPI), [B802] (`Sys.getService`).
> All load-bearing cites grep-verified this session.

---

## 803.1 — Niagara core ships NO credential step-up — only a UX confirm `[CERT for the flag; INFER for "UX-only"]`
`Flags.CONFIRM_REQUIRED = 128` (`Flags.java:16`; table entry `('c',"confirmRequired")` `:38`) sets a bit on an Action
slot that makes Workbench/HX pop an "Are you sure?" dialog before invoke. It carries NO credential re-entry — it is a
UX affordance, not authentication. `BFacets.FORCE_SIGN = "forceSign"` (`BFacets.java:53`) is a marker key the e-sign
module reads (its server-side consumer is not in the Java corpus → the sign-dialog trigger is [INFER], likely ux/JS).
**So there is no core "sign-before-invoke" interceptor** — step-up is module-level code.

## 803.2 — The `electronicSignature` module = the only true step-up, and how it does it `[CERT]`
It does NOT intercept invocation via a flag. Each secured point (`BSecuredNumericWritable`, …) declares custom actions
`setWithAuthentication`/`emergencyOverrideWithAuthentication`/`autoWithAuthentication` with `flags = 256` (OPERATOR)
(`BSecuredNumericWritable.java:85,88,90,98`); the action ARGUMENT (`ISecureParameter`, import `:9`) carries
`userName + Base64(password) + reasonForChange` (+ a secondary signer). The server verifies synchronously:
`Helper.verifyPrimaryCredentials(user,password,comp,action)` (`Helper.java:1308`) → if the user's scheme is LDAP,
`scheme.login(handler)`; else `((BPasswordCache)user.getAuthenticator()).validate(password)` (`Helper.java:1324-1326`).
Two facts the kit must carry:
- **The password rides in the action arg as Base64 — Base64 is NOT encryption.** Safe only over TLS; a servlet copy of
  this pattern that puts a Base64 password in JSON without TLS is an exposure.
- **The signature record is PLAINTEXT audit, not a PKI signature**: `BSecuredTrendRecord extends BStringTrendRecord`
  (`BSecuredTrendRecord.java:71`) with `BString` slots `PrimarySigner/FullNamePrimarySigner/SecondarySigner/
  reasonForChange/oldValue/oldStatus/pointName/operation` (`:27-83`). It is written to **`BHistoryService`** (its own
  `history9` channel, `Helper.java:367`), **NOT** `BAuditHistoryService`.

## 803.3 — The clean server-side re-auth path for a custom `-ux` servlet `[CERT for the API; INFER for the assembly]`
1. Current session user: `webOp.getUser()` (`OrdTarget.getUser() OrdTarget.java:124`; `Context.getUser() Context.java:106`)
   — the servlet already holds the authenticated `BUser`, no lookup.
2. Resolve the user + scheme: `((BUserService)Sys.getService(BUserService.TYPE)).getUser(name)` (`BUserService.java:298`);
   `user.getAuthenticationScheme()` (`BUser.java:580`, resolved via `BAuthenticationService`) / `getAuthenticationSchemeName()`
   (`BUser.java:398`).
3. Re-verify a SUBMITTED credential WITHOUT a new session: password/digest/gauth → `((BPasswordCache)user.getAuthenticator())
   .validate(pw)`; LDAP → `scheme.login(callbackHandler)` (`BAuthenticationScheme.java:101`). This is the same gate the
   e-sign module uses, applied in a servlet.

## 803.4 — The scheme constraint: SAML users CANNOT be re-verified mid-session `[CERT for the hierarchy; INFER for the runtime block]`
Re-auth is delegated to the user's scheme, so the answer depends on the scheme:
- **Local password / digest / gauth** — verify LOCALLY, no network. `BGoogleAuthenticationScheme extends
  BPasswordAuthenticationScheme` (`BGoogleAuthenticationScheme.java:19`) — TOTP is checked on-station.
- **SAML** — `BSAMLAuthenticationScheme extends BSSOAuthenticationScheme` (`BSAMLAuthenticationScheme.java:117`): SSO is a
  browser→IdP redirect protocol. There is no programmatic `login(CallbackHandler)` that re-verifies a SAML credential in
  the request thread [INFER, grounded in the SSO base]. **A SAML user's critical write cannot be step-up-re-verified
  server-side** — the only options are (a) reject and force an IdP re-login, or (b) trust the session + a short TTL. The
  kit must state this, not silently assume password schemes.

## 803.5 — CSRF correction: Niagara ships a real CSRF TOKEN, not only `X-Requested-With` `[CERT]`
`CsrfUtil` defines `CSRF_TOKEN_NAME = "csrfToken"` (query param) and `CSRF_TOKEN_HTTP_HEADER = "x-niagara-csrfToken"`
(`CsrfUtil.java:13-14`); `getCsrfToken(req)` reads the header then the param (`:48-55`) and `verifyCsrfToken` compares it
to the session's token (via `CsrfProtectedFilter`). This REFINES [B763] DWS1 gate 2 / [B58]: a critical-write endpoint
should verify the real **`x-niagara-csrfToken`** token (double-submit against the session), not rely on the
`X-Requested-With` heuristic alone (which the framework CSRF filter applies chiefly to `/rpc/*`).

## 803.6 — Kit design sketch (copy-ready for `types/dashboard.md` §"critical-write with step-up auth") `[INFER, grounded in 803.1-5]`
A mutating `-ux` endpoint whose target is a CRITICAL control adds step-up on top of the [B763] 5 gates:
1. **Server-side criticality list** — an allowlist of target ORDs/actions that REQUIRE step-up (never client-decided).
2. **Collect the credential over TLS in a SPA modal** → POST to a `/reauth` endpoint. Base64 is transport-encoding, not
   security — TLS is mandatory (§803.2).
3. **Re-verify the SESSION user only** — `submittedUser` MUST equal `webOp.getUser().getUsername()` (no impersonation);
   verify via the user's scheme (§803.3); **SAML → reject with "re-login required"** (§803.4).
4. **Issue a fresh short-TTL step-up TOKEN** (e.g. 2-5 min) bound to `(sessionId + user + target ORD + purpose)`, stored
   server-side; the critical write requires a valid, unexpired token FOR THAT EXACT ORD (bind to the ORD to stop replay
   to a different write).
5. **Enforce server-side**: the write handler checks the step-up token + RBAC (`user.getPermissionsFor(component)`,
   [B558]-[B566]) + the real CSRF token (§803.5) + the 5 DWS1 gates ([B763]) — client gating is advisory only.
6. **Audit every step-up** (who/when/target/old→new/reason), fire-and-forget, audit-failure-never-fails-the-write
   ([B763] gate 5); the e-sign precedent writes to `BHistoryService` (§803.2), or use an own ring.
**Anti-patterns**: client-only gating; a Base64/plaintext password in JSON without TLS; a token NOT bound to the target
ORD (replayable); reusing the login password as a fox credential; assuming all users are password-scheme (SAML breaks it).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Core `Flags.CONFIRM_REQUIRED=128` is a UX confirm, no credential re-entry; no core sign-before-invoke interceptor | [CERT]+[INFER] | Flags.java:16,38; BFacets.FORCE_SIGN:53 |
| 2 | e-sign step-up = credential in the action arg (`ISecureParameter`), verified server-side (verifyPrimaryCredentials→BPasswordCache.validate / LDAP scheme.login) | [CERT] | BSecuredNumericWritable.java:85,88,9; Helper.java:1308,1324-1326 |
| 3 | Signature record is PLAINTEXT audit (BString slots), no PKI hash; written to BHistoryService not BAuditHistoryService | [CERT] | BSecuredTrendRecord.java:71,27-83; Helper.java:367 |
| 4 | Servlet gets session user via `webOp.getUser()`; re-verify via `BUserService.getUser`→`getAuthenticationScheme`→BPasswordCache.validate / scheme.login | [CERT] | OrdTarget.java:124; BUserService.java:298; BUser.java:398,580; BAuthenticationScheme.java:101 |
| 5 | gauth verifies locally (extends BPasswordAuthenticationScheme); SAML is SSO/IdP-redirect → no mid-session server re-auth | [CERT]+[INFER] | BGoogleAuthenticationScheme.java:19; BSAMLAuthenticationScheme.java:117 |
| 6 | Niagara's real CSRF = token `x-niagara-csrfToken`/`csrfToken` (CsrfUtil), not only X-Requested-With | [CERT] | CsrfUtil.java:13-14,48 |

**Tally**: 4 [CERT] · 2 [CERT]+[INFER]. All file:line grep-verified this session (mapper's 15-cite map confirmed at the
enclosing method; SAML class line corrected to :117). §803.6 design sketch is [INFER] grounded in §803.1-5. Dedupe:
the RBAC/e-sign/CSRF/action-flag mechanisms are REMITTANCE ([B558]-[B566], [B350]-[B356], [B58], [B776]); this block adds
the STEP-UP residue + the SAML constraint + the CSRF-token correction + the servlet design.

## Connections
- **[B350]-[B356]** (electronic-signature subsystem — the e-sign mechanism this reuses), **[B558]-[B566]** (RBAC/
  `getPermissionsFor`), **[B763]** (5-gate write-surface + canWrite seam — step-up is gate 0.5 on top), **[B776]**
  (action OPERATOR/ADMIN flags), **[B58]**/**[B507]** (CSRF — refined by §803.5), **[B777]** (auth-scheme SPI),
  **[B802]** (`Sys.getService` for BUserService/BHistoryService). Kit: `types/dashboard.md` §"critical-write with step-up auth".

## Open gaps
- **B803-G1** (requires-execution): confirm the SAML mid-session re-auth BLOCK on a live station (does `scheme.login`
  throw / return no Subject for a SAML user?) — the hierarchy says SSO-redirect, the runtime block is [INFER].
- **B803-G2**: whether gauth's `BPasswordCache.validate` accepts a TOTP token as the "password" mid-session, or needs the
  gauth login module path — a live check; affects whether a gauth user's step-up is single-field or TOTP.
