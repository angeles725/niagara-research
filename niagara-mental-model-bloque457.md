# Block 457 — Programmatic login to a live Niagara N4 station: the real SCRAM-SHA-256 handshake + the acceptEula commit step (reusable recipe)

**Focus:** api-access (new axis — legitimate programmatic access to a live N4 station). Extends the SCRAM cross-finding first noted in [B453] §453.1.

**Mode:** DOCUMENT (§20) — CAPTURE of a reusable how-to (the login METHOD), not gap discovery.

**Origin:** the cross-session api-access work reverse-engineered and **verified live** the web login of the operator's OWN N4 station (`https://localhost`, valid service-account `API2` credentials). This is legitimate authentication by the real protocol — the same handshake Workbench and the browser perform — **not** an auth bypass. Documented at the operator's explicit request.

**Scope:** the end-to-end login recipe + what it unlocks (oBIX/BQL). NOT: the sjcl/JS client internals line-by-line; a password cracker (none — valid credentials are required).

**Evidence base & provenance.** Three converging channels:
- `[CERT]` — N4 code (corpus 4.14): the scheme IS SCRAM and the server stores PBKDF2-HMAC-**SHA256**.
- `[CERT-live]` — the handshake, the `acceptEula` wall, and the oBIX reads were verified **live** against the real station by the **cross-session** api-access work (agent "camara"); **not re-run in this session**. The reference implementation is preserved in-repo as the citable artifact.
- The reference tool: `sources/probes/B457-n4-login/niagara-n4-client.py` (stdlib, no secrets embedded — password via prompt/`$N4_PW` only).

**SECRETS DISCIPLINE (§ live-install).** User/role cited (`API2`, a service account), never the password or any secret value. **Action item: the operator must ROTATE the test credentials — they were exposed in chat.**

---

## 457.1 — The scheme is SCRAM-SHA-256, not HTTP Digest (three-way confirmation)

N4's default "Digest" auth scheme is **SCRAM-SHA-256** over the web login servlet — NOT HTTP Digest (RFC 7616). Confirmed three ways:

1. **Code** `[CERT]`: `BDigestAuthenticationScheme` configures `DigestLoginModule` (`sources/decompiled/video/BDigestAuthenticationScheme.java:44`), which drives `ScramServer`/`ScramServerCallback` (`sources/decompiled/video/DigestLoginModule.java:26`). The stored verifier is **PBKDF2-HMAC-SHA256**: `sources/decompiled/video/UserKeyFactory.java:14` (`BPbkdf2HmacSha256PasswordEncoder`).
2. **Probe** `[CERT-live]`: `GET /` → 302 → `/prelogin` with **no** `WWW-Authenticate: Digest` challenge (cross-session).
3. **Direct test** `[CERT-live]`: a Python `HTTPDigestAuthHandler` (RFC 7616) never authenticates — every path lands on `/prelogin` (cross-session).

**Gotcha** `[CERT-live]`+`[CERT]`: the login JS labels the mechanism `scram-sha512`, but the real hash is **SHA-256** — sjcl defaults to sha256, and only sha256 produced a valid server-signature live. The code side agrees (SHA256 encoder). Documented in the tool: `sources/probes/B457-n4-login/niagara-n4-client.py:21`.

## 457.2 — The full handshake recipe `[CERT-live]`

- **Endpoint:** `POST https://<host>/prelogin/j_security_check/` (`niagara-n4-client.py:36`)
- **Content-Type:** `application/x-niagara-login-support` (`:37`)
- **userPrep:** NFKC-normalize, then escape `=`→`=3D` and `,`→`=2C` (`:40`)

1. `GET /prelogin` — seed the `JSESSIONID` cookie.
2. `POST action=sendClientFirstMessage&clientFirstMessage=n,,n=<userPrep>,r=<clientNonce>` → server-first `r=<nonce>,s=<salt b64>,i=<iterations>`.
3. Compute (SHA-256): `SaltedPassword = PBKDF2-HMAC-SHA256(NFKC(pw), b64decode(salt), i, 32)`; `ClientKey = HMAC(SaltedPassword,"Client Key")`; `StoredKey = SHA256(ClientKey)`; `AuthMessage = clientFirstBare + "," + serverFirst + "," + "c=biws,r="+nonce`; `proof = b64(ClientKey XOR HMAC(StoredKey, AuthMessage))`. (`niagara-n4-client.py:92-97`)
4. `POST action=sendClientFinalMessage&clientFinalMessage=c=biws,r=<nonce>,p=<proof>` → server-final `v=<serverSignature>`. Verify mutual auth: `v == b64(HMAC(HMAC(SaltedPassword,"Server Key"), AuthMessage))` (`:104-106`).

## 457.3 — The acceptEula wall (the step that almost got missed) `[CERT-live]`

SCRAM validating is **not enough**: after a correct client-final, `/ord` still redirected to `/prelogin`. The missing piece is a pending **EULA** that blocks the session. The commit step:

5. `POST action=acceptEula` to the same endpoint (`niagara-n4-client.py:17,123`). → session authenticated (`JSESSIONID` + `niagara_userid`); any path now serves.

Without step 5 the credentials verify but the session never activates — a silent wall that reads as "auth failed" when it is really "EULA pending".

## 457.4 — What it unlocks (validation) `[CERT-live]`

Post-auth, `GET /obix/about` (HTTP 200) returned: `serverName=DESKTOP-4AAQ77H`, `vendor=Tridium`, `productVersion=4.14.0.162`, `componentCount=9743`, `localHistoryCount=26`. The oBIX lobby `/obix/` exposes `config/` (the Station), `histories/` (`BLocalHistoryDatabase`), `watchService/` — all readable over authenticated oBIX (cross-session). This is the payoff: authenticated read of station data (oBIX/BQL/histories) with an owned account.

## 457.5 — Reference implementation

`sources/probes/B457-n4-login/niagara-n4-client.py` runs the full login (SCRAM-SHA-256 + `acceptEula`, verifies the server signature, auto-falls-back to sha512 `:114`) then GET/POSTs any path. Companion `http-digest.py` is the RFC-7616 client for **cameras/IoT** (explicitly NOT for N4). Both stdlib, password via prompt/env, no secrets embedded. Registered in `SOURCES.md`.

## 457.6 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | N4 scheme = SCRAM (not RFC 7616); server verifier = PBKDF2-HMAC-SHA256 | `[CERT]` | `sources/decompiled/video/BDigestAuthenticationScheme.java:44`, `DigestLoginModule.java:26`, `UserKeyFactory.java:14` |
| 2 | Hash is SHA-256 despite the "scram-sha512" JS label | `[CERT-live]`+`[CERT]` | live sha256-only valid sig + SHA256 encoder; `niagara-n4-client.py:21` |
| 3 | Handshake: GET /prelogin → sendClientFirstMessage → sendClientFinalMessage (v= verified) | `[CERT-live]` | cross-session; `niagara-n4-client.py:36,83,98,104` |
| 4 | Endpoint `/prelogin/j_security_check/`, CT `application/x-niagara-login-support`, userPrep NFKC+=3D/=2C | `[CERT-live]` | `niagara-n4-client.py:36,37,40` |
| 5 | `action=acceptEula` is required to activate the session past a pending EULA | `[CERT-live]` | cross-session; `niagara-n4-client.py:17,123` |
| 6 | Post-auth oBIX read: 4.14.0.162, componentCount 9743, config/histories/watchService | `[CERT-live]` | cross-session `GET /obix/about`, `/obix/` |
| 7 | Reference tool authenticates legitimately (valid creds), no bypass, no embedded secrets | `[CERT]` | `sources/probes/B457-n4-login/niagara-n4-client.py` (read in full) |

**Tally:** 7 claims — 2 `[CERT]` · 5 `[CERT-live]`/mixed (labelled, cross-session provenance stated) · 0 unmarked. Consistent with [B453] §453.1.

**Left out (named):** the sjcl/auth.min.js internals; BQL-over-REST specifics; whether other stations on this site use SHA-512; the exact `niagara_userid` cookie lifecycle.

## 457.7 — Connections
- Extends the SCRAM cross-finding in [B453] §453.1 into a full, reproducible login method. Sibling of the video runbooks [B453]–[B456] (same cross-session collaboration).
- Enables data-side follow-ups (oBIX/BQL) that the corpus documents structurally (e.g. oBIX/history layers) but never exercised against a live station.

## 457.8 — Open gaps
- **B457-G1** — BQL/history read recipes over authenticated oBIX (what queries, what shapes) — investigable live with the tool.
- **B457-G2** — the `niagara_userid`/session lifecycle and CSRF/token behavior on write endpoints (this block only exercised reads).
- **Security action (not a gap):** rotate the exposed `API2` test credentials.
