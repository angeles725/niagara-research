# Block 728 — `access-control` RUNBOOK: login-less landing on an N4 web dashboard behind a public reverse proxy — the proxy injects `Authorization: Basic` for a dedicated `HTTPBasicScheme` user (there is NO native anonymous/auto-logon in N4), and how to keep "no login" from meaning "full control for anyone"

> **Focus:** `access-control`, **document-mode RUNBOOK** (METHODOLOGY §20), NOT a discovery gap. Fourth operator
> runbook the STOPPED `access-control` focus hosts, after [B560] cloudflared remote access, [B726] SSH `-L` jump
> host, [B727] oBIX server exposure. Captures the pattern used in the field (Mercato / Hilton installs) to let a
> user open a public URL and land straight in a Niagara web dashboard WITHOUT typing credentials.
> **The core truth up front:** N4 has **no** anonymous / auto-logon / guest web user. Every WebService request
> must resolve to an authenticated `BUser`. "Login-less" is therefore NOT no-auth — it is a real user whose
> credentials are **injected by the reverse proxy** so the human never sees the login screen.
> **Angle:** the operator procedure + its security envelope. Cite, do NOT re-derive: [B727] HTTPBasicScheme setup;
> [B510] the `BAuthenticationScheme` SPI; [B494] the OEM scheme survey; [B560]/[B726] the remote-access runbooks.
> **Sources (all consulted):**
> - FUENTE 3 (code): `organized/baja/baja/vineflower/javax/baja/security/` + `.../authn/` scheme inventory;
>   `javax/baja/user/BUser.java` (`getHomePage`, `authenticationSchemeName`); `javax/baja/authn/BSSOAuthenticationScheme.java`.
> - FUENTE 2 (Tridium doc): `niagara-help/devguide-clean/security/headerAuthentication.txt` (HTTP Header Auth = SCRAM, N4 4.4+).
> - FUENTE 1 (corpus): [B727], [B510], [B494], [B560], [B726]; concurrency cap cross-ref (Alerton Compass, engram #6855).
> READ-ONLY over the subject; no station mutated. Markers §3.

## §728.1 — What N4 does NOT give you (settle the wrong paths first) `[CERT]`/`[CERT-doc]`

- **No anonymous / auto-logon / guest scheme.** The complete built-in scheme set is `Digest`, `HTTPBasic`,
  `LegacyBasic`, `LegacyDigest`, `OpcUa`, `Password`, `SAML`, `SSO`, `SessionId`, `Username` `[CERT]`
  (`javax/baja/security/` + `javax/baja/authn/` class inventory). **None is anonymous.** AX's HxProfile
  auto-logon has no N4 equivalent — it was removed deliberately.
- **You cannot pre-seed the session cookie.** The N4 session cookie is **`niagaraSession`** `[CERT]` and is
  ISSUED BY THE SERVER after a successful SCRAM handshake; it is server-side session state, not forgeable
  client-side. `BSessionIdAuthenticationScheme` validates an ALREADY-valid session, it does not mint one.
- **The "HTTP Header Authentication Mechanism" (N4 4.4+) does not help here.** It is a multi-round **SCRAM
  challenge-response over headers** (`Authorization: HELLO` → `401 WWW-Authenticate: SCRAM …handshakeToken` →
  exchange → `Authentication-Info: authToken` → `Authorization: BEARER`) `[CERT-doc]`
  (`devguide-clean/security/headerAuthentication.txt`). It needs the live handshake; a proxy CANNOT satisfy it
  by stamping one static header. So it is for programmatic SCRAM clients, not for a login-less browser.
- **SSO (`BSSOAuthenticationScheme`) is a real login, just external.** It is abstract with
  `getLoginRedirectURL()`, `supportsRemoteUsers()=true`, and a "Log in with SSO" button `[CERT]`
  (`BSSOAuthenticationScheme.java`) — it redirects to an IdP (the SAML scheme extends this). Still an
  interactive login; not "no login."

**Conclusion:** the only scheme a reverse proxy can pre-satisfy with a single static header is **HTTP Basic**.

## §728.2 — The mechanism that works: proxy-injected `Authorization: Basic` `[CERT-doc]`/`[CERT]`

1. Create a **dedicated station user** bound to **`HTTPBasicScheme`** — full setup in [B727 §727.5]: bind via the
   user's `Authentication Scheme Name` = `HTTPBasicScheme` (`BUser.authenticationSchemeName`, resolved by name,
   `BUser.java:225,606`) `[CERT]`.
2. Point that user's **Home Page** at the dashboard so it lands directly: `BUser.getHomePage()` returns the
   per-user landing ORD (`BUser.java:533`) `[CERT]` → set it to the dashboard view (e.g. `/dashboardpan/` or the
   px/servlet ORD).
3. On the reverse proxy, inject the credential header on every request to the station origin:

   ```
   Authorization: Basic base64("<user>:<password>")
   ```

   `HTTPBasicScheme` authenticates straight from this header ([B727 §727.5]; the web tier does the check, the
   servlet just receives the authenticated `BUser` — [B509 §509.5]) `[CERT]`. The browser never renders a
   Niagara login page → the human opens the public URL and lands in the dashboard.
4. Why Basic and nothing else: the normal N4 browser login is **SCRAM** (JS challenge-response), so you cannot
   "pre-fill and POST the login form." Only a static header works, and only Basic reads a static header. §728.1.

### Cloudflare-tunnel shape (the field case)

- `cloudflared` fronts `https://<jace-ip>/` and publishes it as the public hostname.
- Inject the header at the edge: a **Cloudflare Worker** (or the origin config / a transform rule) adds the
  `Authorization: Basic …` header to requests reaching the origin. `[INFER]` (exact edge feature is a Cloudflare
  detail, not an N4 fact; the N4 requirement is only "the header must arrive").
- The base64 credential lives in the edge config → it is a **secret** (base64 is reversible), scope it like one.

## §728.3 — The security envelope — MANDATORY, not optional `[CERT]`/`[INFER]`

Because the proxy supplies the credential, **anyone who reaches the public URL is authenticated as that user.**
The credential stops being the gate, so you must reinstate a gate elsewhere and shrink what the user can do.

1. **Put Cloudflare Access (Zero Trust) in FRONT of the tunnel** `[INFER]` — an Access policy (email OTP, or a
   service token for machine viewers) restores a real gate so "no Niagara login" ≠ "open to the whole internet."
   This is the single highest-value mitigation.
2. **Dedicated user is read-only** — a role with only read/invoke over the dashboard's scope; no admin, no config
   write, no station-level actions ([B11]/[B30] enforcement model).
3. **Restrict the user's categories** — scope its category so even a poke at `/ord`, `/obix`, or another servlet
   returns nothing beyond the dashboard's components (CategoryService visibility, cap 256, [B561]) `[CERT]`.
4. **Firewall the JACE to the tunnel origin only** `[INFER]` — only the `cloudflared` origin may reach `:443`;
   block direct access to the LAN IP, or the tunnel is one door and the raw IP another.
5. **HTTPS end-to-end + short session timeout** — Basic sends the credential every request; TLS is non-negotiable
   ([B727 §727.6]).
6. **Concurrency cap awareness** — the license limits concurrent authenticated web sessions (Alerton Compass AX:
   **18**, engram #6855, `[CERT-live]` on that install; N4 has its own license-gated cap). A single shared public
   user exhausts it fast; size it, or the dashboard starts refusing sessions.
7. **Audit** — the dedicated user's actions land in the audit trail ([B564]); it does not distinguish the humans
   behind the shared credential, so Access (step 1) is what gives you per-person accountability.

## §728.4 — Decision summary

| Want | Native N4? | Do this |
|---|---|---|
| Anonymous / no user at all | **No** | not possible — every request needs a `BUser` |
| Pre-seed cookie / remember-me | **No** | cookie `niagaraSession` is server-issued post-SCRAM |
| Guest read-only user, no creds | **No** | must be a real user with a password |
| Land in dashboard without typing login | **Yes, via proxy** | HTTPBasicScheme user + proxy injects `Authorization: Basic` + Home Page = dashboard |
| Keep it safe on a public URL | — | Cloudflare Access + read-only user + categories + firewall to origin |

## Self-verify (METHODOLOGY §11)

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | N4 has no anonymous/auto-logon/guest scheme; built-in set is Digest/HTTPBasic/Legacy×2/OpcUa/Password/SAML/SSO/SessionId/Username | `[CERT]` | `javax/baja/security/` + `javax/baja/authn/` class inventory |
| 2 | Session cookie is `niagaraSession`, server-issued after SCRAM; not client-forgeable/pre-seedable | `[CERT]` | code string; `BSessionIdAuthenticationScheme` semantics |
| 3 | HTTP Header Auth (4.4+) is multi-round SCRAM (HELLO→401 SCRAM→BEARER), not a static injectable header | `[CERT-doc]` | `devguide-clean/security/headerAuthentication.txt` |
| 4 | `BSSOAuthenticationScheme` is an interactive external-IdP redirect (getLoginRedirectURL, "Log in with SSO"), not no-login | `[CERT]` | `BSSOAuthenticationScheme.java` |
| 5 | Only HTTP Basic can be pre-satisfied by one static proxy header → the login-less mechanism | `[CERT-doc]`+`[INFER]` | headerAuthentication.txt (rules out SCRAM); [B727] Basic-by-header |
| 6 | User bound to scheme by name via `authenticationSchemeName`; per-user Home Page via `getHomePage()` | `[CERT]` | `BUser.java:225,606,533` |
| 7 | Servlet receives an authenticated BUser; web tier does the credential check | `[CERT]` | [B509 §509.5] |
| 8 | Whoever reaches the public URL is authenticated as the injected user → gate must move to the proxy + RBAC | `[CERT]`+`[INFER]` | mechanism consequence; [B11]/[B30]/[B561] |
| 9 | Category restriction (cap 256) bounds what the shared user can see across servlets | `[CERT]` | [B561] |
| 10 | License caps concurrent web sessions; one shared user hits it (Compass=18) | `[CERT-live]` (cross-ref) | engram #6855 (Alerton Compass install) |

**Tally:** 10 claims — 5 `[CERT]`, 1 `[CERT-doc]`, 2 mixed with `[INFER]`, 1 `[CERT-live]` cross-ref, 0 unmarked.
Cloudflare-edge specifics are marked `[INFER]` (out-of-N4-scope). Scope left OUT (§8): the exact Cloudflare Worker
/ transform-rule syntax (vendor doc, not N4); Cloudflare Access policy authoring; whether a custom
`BSSOAuthenticationScheme` subclass could delegate to Cloudflare Access as an IdP (a heavier alternative to Basic,
not built here).

## Connections

- **[B727]** — the HTTPBasicScheme user setup this runbook depends on (add scheme, bind by name, HTTPS warning).
- **[B560]/[B726]** — sibling remote-access runbooks (cloudflared / SSH `-L`); this one is the login-less-UX layer on top.
- **[B510]/[B494]** — the auth-scheme SPI and the OEM scheme survey (SAML/SSO/clientCert) ruled out in §728.1.
- **[B509 §509.5]** — servlet receives an already-authenticated BUser; auth is the web tier's job.
- **[B561]/[B11]/[B30]/[B564]** — the RBAC/category/audit machinery the mitigations lean on.
- **engram #6855** — Alerton Compass concurrent-web-session cap (the constraint a shared public user hits).

## Gaps opened

- **B728-G1** (medium) — the SSO alternative: a custom `BSSOAuthenticationScheme` that delegates to Cloudflare
  Access (or another IdP) as the login, giving per-person identity instead of one shared Basic credential. Not
  built; would remove the "everyone is the same user" weakness. Candidate for a real investigation block.
- **B728-G2** (low) — exact Cloudflare edge mechanism to inject the header (Worker vs transform rule vs origin
  config) and whether Access + injected Basic can coexist cleanly on one hostname. Vendor-side, verify live.
