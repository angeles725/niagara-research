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
> - FUENTE 1 (corpus): [B727], [B510], [B494], [B560], [B726].
> - `[CERT-live]` (relayed): probe `sources/probes/2026-09-03-panccadia-loginless-dashboard-live.md` — end-to-end
>   confirmation by a peer session at Pancaddia León (§728.5), credential value redacted.
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
- Inject the header at the edge with a **Cloudflare Transform Rule** ("HTTP Request Header Modification") that adds
  a static `Authorization: Basic …` header for the tunnel hostname — a Worker is only needed if you want logic. In
  the confirmed field deploy (§728.5) the rule sat in zone ruleset `default`, phase `http_request_late_transform`,
  matching `http.host eq <hostname>` `[CERT-live]`. The N4 requirement is only "the header must arrive".
- The base64 credential lives in the edge config → it is a **secret** (base64 is reversible), scope it like one.
- Access and the Basic injection are **different layers and coexist**: Access gates the human in front (its own
  JWT), and the origin request additionally carries the injected `Authorization: Basic` for the station.

## §728.3 — The security envelope — MANDATORY, not optional `[CERT]`/`[INFER]`

Because the proxy supplies the credential, **anyone who reaches the public URL is authenticated as that user.**
The credential stops being the gate, so you must reinstate a gate elsewhere and shrink what the user can do.

1. **Put Cloudflare Access (Zero Trust) in FRONT of the tunnel** `[INFER]` — an Access policy (email OTP, or a
   service token for machine viewers) restores a real gate so "no Niagara login" ≠ "open to the whole internet."
   This is the single highest-value mitigation. **Field note (§728.5):** the confirmed Pancaddia deploy went
   OPEN — no Access — by explicit user decision, replicating Mercato. That is a conscious deviation from this
   step, not something this runbook endorses; on an open URL steps 2–3 (read-only user + restricted categories)
   become the ONLY remaining gate and MUST be enforced.
2. **Dedicated user is read-only** — a role with only read/invoke over the dashboard's scope; no admin, no config
   write, no station-level actions ([B11]/[B30] enforcement model).
3. **Restrict the user's categories** — scope its category so even a poke at `/ord`, `/obix`, or another servlet
   returns nothing beyond the dashboard's components (CategoryService visibility, cap 256, [B561]) `[CERT]`.
4. **Firewall the JACE to the tunnel origin only** `[INFER]` — only the `cloudflared` origin may reach `:443`;
   block direct access to the LAN IP, or the tunnel is one door and the raw IP another.
5. **HTTPS end-to-end + short session timeout** — Basic sends the credential every request; TLS is non-negotiable
   ([B727 §727.6]).
6. **Concurrency cap awareness** `[INFER]` — the N4 license limits concurrent authenticated web sessions; a single
   shared public user exhausts it fast, and past the cap the dashboard starts refusing sessions. (For scale of the
   concern: an Alerton Compass AX install measured **18** concurrent web sessions — a DIFFERENT platform/product,
   cited only as context, NOT an N4 number; confirm the actual N4 cap from the running install's license.)
7. **Audit** — the dedicated user's actions land in the audit trail ([B564]); it does not distinguish the humans
   behind the shared credential, so Access (step 1) is what gives you per-person accountability.

## §728.5 — Live confirmation (relayed `[CERT-live]`) — the Pancaddia deploy

The full mechanism was verified end-to-end on live hardware (JACE-9000 "atlashost", N4 **4.15.3.28**). **Not run by
the author of this block** — executed and reported by a peer session at the customer site (Pancaddia León),
relayed 2026-09-03; preserved as `sources/probes/2026-09-03-panccadia-loginless-dashboard-live.md` (credential
value redacted). `[CERT-live]`:

- `GET /dashboardpan/` with `Authorization: Basic base64("API:<PASSWORD-REDACTED>")` → **HTTP 200** `text/html`;
  the SAME request with NO auth → **HTTP 302** (login). Confirms both halves: injected header lands you in the
  dashboard, absence of the header still gates (not anonymous).
- The `API` user already carried `HTTPBasicScheme` (same scheme as the reference Mercato install).
- Edge: Cloudflare **named tunnel** `panccadia-dashboard` (healthy, 4 conns), connector as a scheduled task on a
  separate mini-PC; ingress `panccadia.angeles-group.org` → `https://192.168.200.137:443` `noTLSVerify`; a
  **Transform Rule** (ruleset `default`, phase `http_request_late_transform`) injects the Basic header for
  `http.host eq panccadia.angeles-group.org`; proxied CNAME to `cfargotunnel.com`.
- Deployed **OPEN — no Cloudflare Access** — by explicit user decision, replicating Mercato (see §728.3 step 1 —
  this is the recorded deviation).

**Residual (honest):** an earlier `403` was on `/bajaux`, a path unrelated to the dashboard servlet — it does not
apply to `/dashboardpan/`. Not itemized by the reporter: whether the `API` user is confirmed read-only with
restricted categories — on the open pattern that is the ONLY remaining gate, so it is asserted-necessary but NOT
verified on this install.

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
| 10 | N4 license limits concurrent web sessions; a shared public user exhausts it (Compass AX=18 is cross-platform context, NOT an N4 number) | `[INFER]` | N4 license-gated cap; AX Compass 18 is a different product, confirm from the running N4 license |
| 11 | Full mechanism confirmed live: `GET /dashboardpan/` + injected Basic → 200; no auth → 302; Transform Rule (late_transform) injects the header; deployed OPEN by user choice | `[CERT-live]` (relayed) | peer session at Pancaddia León (JACE-9000, N4 4.15.3.28); probe `2026-09-03-panccadia-loginless-dashboard-live.md` |

**Tally:** 11 claims — 5 `[CERT]`, 1 `[CERT-doc]`, 2 mixed with `[INFER]`, 1 `[INFER]`, 1 `[CERT-live]` (relayed,
attributed per §3 relayed-live discipline), 0 unmarked. No claim cites the engram mirror as evidence (the prior
row-10 defect — citing `#6855` as `[CERT-live]` — is corrected; the mirror is background, not a source, §3).
Cloudflare-edge specifics stay `[INFER]` where unconfirmed. Scope left OUT (§8): Cloudflare Access policy
authoring; whether a custom `BSSOAuthenticationScheme` subclass could delegate to Cloudflare Access as an IdP
(a heavier alternative to Basic, not built here).

## Connections

- **[B727]** — the HTTPBasicScheme user setup this runbook depends on (add scheme, bind by name, HTTPS warning).
- **[B560]/[B726]** — sibling remote-access runbooks (cloudflared / SSH `-L`); this one is the login-less-UX layer on top.
- **[B510]/[B494]** — the auth-scheme SPI and the OEM scheme survey (SAML/SSO/clientCert) ruled out in §728.1.
- **[B509 §509.5]** — servlet receives an already-authenticated BUser; auth is the web tier's job.
- **[B561]/[B11]/[B30]/[B564]** — the RBAC/category/audit machinery the mitigations lean on.
- **probe `2026-09-03-panccadia-loginless-dashboard-live.md`** — the relayed `[CERT-live]` end-to-end confirmation (§728.5).

## Gaps opened

- **B728-G1** (medium) — the SSO alternative: a custom `BSSOAuthenticationScheme` that delegates to Cloudflare
  Access (or another IdP) as the login, giving per-person identity instead of one shared Basic credential. Not
  built; would remove the "everyone is the same user" weakness. Candidate for a real investigation block.
- **B728-G2** (low) — RESOLVED for the inject mechanism by §728.5: a Cloudflare **Transform Rule** (phase
  `http_request_late_transform`) adds the static Basic header — no Worker needed for a static value. Still open:
  a live deploy where Access AND the Basic injection run together on one hostname (the confirmed deploy was OPEN,
  no Access), to prove the two layers coexist in practice.
