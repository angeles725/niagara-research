# Block 602 — Session lifecycle & CSRF on writes: `niagara_userid` is a Secure+HttpOnly+SameSite=Lax cookie persisted ~365 days, `JSESSIONID` is session-scoped; state-changing writes are gated by a synchronizer token (`x-niagara-csrfToken` header must equal the session token → else 403), but only for the HTTP methods each filter is mapped to

**Session**: 2026-08-29
**Focus**: `api-access` (gap B457-G2 — `niagara_userid` lifecycle + CSRF on writes). §12 DYNAMIC phase.
**Distribution / live target**: OptimizerSupervisor-N4.14.0.162, `127.0.0.1` (`DESKTOP-4AAQ77H`), `live-install`
→ SECRETS DISCIPLINE (cookie NAMES + flags cited, never values).
**Method**: DISK-FIRST (§12) — the mechanism is in code `[CERT]`; live capture confirms cookie flags
`[CERT-live]`. Authenticated `API2`/SCRAM, `no·inline`. Write-gate observed by SAFE gate-discovery (a
deliberately malformed request), no valid write performed.
**Primary sources**:
- `[CERT]` `organized/web/web-rt/vineflower/javax/baja/web/CsrfUtil.java`,
  `.../javax/baja/web/filters/CsrfProtectedFilter.java`, `.../com/tridium/web/CookieUtil.java`.
- `[CERT-live]` `sources/probes/B602-userid-csrf/cookies-live-flags.txt`.
**Scope**: how the authenticated session is carried (cookies) and how writes are CSRF-gated. Completes the
`api-access` READ gaps. Does NOT exercise a real oBIX write (gap B458-G2, `⚠ CONFIG MUTATION`).

---

## 602.1 The session cookie set — code constants [CERT]

`CookieUtil` declares the full cookie namespace `[CERT] :16-27`:
`niagara_userid` (CNAME_USERID) · `niagara_essential_session_support` (CNAME_ENCRYPTED_USERID) ·
`niagara_auth_scheme` · `JSESSIONID` · `super_session_id` · `niagara_sso_scheme` /
`niagara_current_sso_scheme` · `niagara_cfid` (current form id) · `niagara_csid` (current scheme id) ·
`niagara_origin_uri` · `niagara_failure_cause` / `niagara_failure_info`.
`COOKIE_AGE = TimeUnit.DAYS.toSeconds(365)` `[CERT] :28` — the persistent-cookie lifetime is **one year**.
`createCookie(...)` sets `HttpOnly(true)`, `MaxAge(age)`, `Path("/")` `[CERT] :101-103`.

## 602.2 Live cookie lifecycle — what the running station actually sets [CERT-live]

After a full SCRAM login + `acceptEula`, the station set exactly two cookies (this auth scheme) `[CERT-live]`
`sources/probes/B602-userid-csrf/cookies-live-flags.txt`:

| cookie | Secure | HttpOnly | SameSite | expiry | path |
|---|---|---|---|---|---|
| `JSESSIONID` | ✓ | ✓ | Lax | session (no expiry) | `/` |
| `niagara_userid` | ✓ | ✓ | Lax | **persistent, ~365 d** (unix 1819529929) | `/` |

Two lifecycle facts, both live-confirmed:
- **`niagara_userid` is a PERSISTENT cookie (~1 year, = `COOKIE_AGE`)** — it survives browser restart, unlike the
  session-scoped `JSESSIONID`. It carries the plaintext username; on this station it is `Secure`+`HttpOnly`, so
  it is NOT script-readable here (a hardened posture — some N4 deployments expose it to BajaScript; this one
  does not).
- **SameSite=Lax on both** — a cross-site top-level GET carries the cookie, but a cross-site POST does not; this
  is the browser-side half of CSRF defense, complementing the token below.

## 602.3 CSRF on writes — synchronizer token, method-scoped [CERT]

State-changing requests are gated by `CsrfProtectedFilter` + `CsrfUtil`:
- Token name `csrfToken`; transport = header **`x-niagara-csrfToken`** (URL-decoded), OR request parameter
  `csrfToken` as fallback `[CERT] CsrfUtil:13-14,46-55`.
- Verification is a **synchronizer-token equality**: `sessionToken.equals(requestToken)`; the session token is
  `session.getCsrfToken()` `[CERT] CsrfUtil:27-42`. Missing either token → `CsrfException` → **403**; mismatch →
  403; decode failure → 400 `[CERT] CsrfProtectedFilter:37-58`.
- **Method-scoped**: the filter only inspects requests whose method is in its `httpMethod` init-param
  (configured per servlet mapping in `web.xml`); any other method calls `chain.doFilter` UNCHECKED
  `[CERT] CsrfProtectedFilter:24-30,34-61`. So CSRF protection is applied per-servlet to the write verbs
  (typically POST/PUT/DELETE), not globally — a servlet not mapped to the filter, or a method not listed, is
  not token-checked. This is the exact surface a write-path audit (B458-G2) must confirm per endpoint.
- Logout has a special branch: a `CsrfException` on `/logout` redirects to `/logoutConfirm` instead of erroring
  `[CERT] CsrfProtectedFilter:45-48`.

## 602.4 Live gate observation (scope-limited) [CERT-live]/[INFER]

A POST to a non-existent oBIX write path (`/obix/config/NumericDelay/out/set/`) returned HTTP 200 with
`is="obix:BadUriErr"` — the oBIX layer rejected the URI BEFORE any CSRF 403 `[CERT-live]`. This is NOT sufficient
to claim "oBIX writes are CSRF-exempt": the request was malformed and never reached a real write handler
(RE-MEASURE A DRAMATIC POSITIVE — one malformed probe is not a gate proof). Whether a VALID oBIX write demands
`x-niagara-csrfToken` is `[INFER]` pending the authorized-write iteration (B458-G2), where a real write with and
without the token is the clean counterfactual.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Cookie namespace + `COOKIE_AGE`=365d + createCookie HttpOnly/Path | [CERT] | CookieUtil:16-28,101-103 | ✓ token |
| 2 | Live: JSESSIONID session-scoped, Secure+HttpOnly+Lax | [CERT-live] | cookies-live-flags.txt | ✓ live |
| 3 | Live: niagara_userid persistent ~365d, Secure+HttpOnly+Lax | [CERT-live] | cookies-live-flags.txt | ✓ live |
| 4 | CSRF token name/header + synchronizer equality → 403 | [CERT] | CsrfUtil:13-14,27-55 | ✓ token |
| 5 | CSRF filter is method-scoped (httpMethod init-param) | [CERT] | CsrfProtectedFilter:24-61 | ✓ token |
| 6 | oBIX rejected malformed POST with BadUriErr before any CSRF check | [CERT-live] | live probe | ✓ live |
| 7 | Valid-oBIX-write CSRF requirement | [INFER] | pending B458-G2 | honest gap |

**Marker tally**: [CERT] ×3, [CERT-live] ×3, [INFER] ×1. Ratio [INFER]/[CERT*] = 1/6 = 0.17. **Block type:
EVIDENCE (§12, disk+live).** CLOSES B457-G2. **§12 verdict: CONFIRMED** (mechanism) + one honest `[INFER]`
scoped to the write path. Zero secrets (no cookie values). Read-only (the one POST was a rejected malformed URI).

## Connections

- [Block 457] — SCRAM login → acceptEula; the cookies here are that session's output.
- [Block 600]/[Block 601] — the read query surface those cookies authorize.
- [Block 494]/[Block 510] — auth-scheme SPI (`niagara_auth_scheme` cookie ties a session to its scheme).
- Points to: B458-G2 (oBIX write) — the counterfactual for §602.4's `[INFER]` (does a valid write demand
  `x-niagara-csrfToken`?), `⚠ CONFIG MUTATION`, authorization-gated.
