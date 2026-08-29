# Block 628 — ports (PO-G7w): the platform daemon credential frame is HTTP Basic/Digest(MD5) + a shared-secret-key init — resolved from code, transport confirmed live

> **What**: The on-the-wire credential handshake of the platform daemon (:3011/:5011) — the sub-gap B129
> §129.6 deferred as "N6-wire" and [B626] carried as PO-G7w (requires-execution). PROBE-THE-PREMISE result:
> the frame is NOT a bespoke protocol and NOT Fox-SCRAM — it is standard **HTTP Basic** (`Authorization:
> Basic base64(user:password)`) or **HTTP Digest (MD5)** over the daemon's HTTP transport, followed by an
> `InitializeSharedSecretKeyMessage` (salt + IV + verification = an AES session key). The frame is readable
> in Java (the wire IS Java, per B129), so most of PO-G7w dissolves to a DISK-FIRST read; the live probe
> confirms the transport.
> **Scope**: `com.tridium.platform.daemon` auth classes + a §12 live read-only probe of the local daemon.
> The 18-command set / daemon reach is REMITTANCE to [B129]; the auth MODEL (platform users) to [B626].
> **Block type**: EVIDENCE — code `[CERT]` (frame) + live `[CERT-hw]` (transport). **§12 dynamic phase**,
> rung 0/1 (passive + read-only probe), READ-ONLY, no mutation. **SECRETS DISCIPLINE**: the station account
> used for the live attempt was supplied out-of-band (a shredded scratchpad `-K` file); no credential value
> is recorded in this block, `sources/`, or engram — only the frame STRUCTURE.
> **Subject version**: Niagara N4.14.0.162. Live host: 127.0.0.1 (PRUEBAS test station), 2026-08-29.
> **Sources**:
> - `organized/platform/platform-rt/vineflower/com/tridium/platform/daemon/BasicAuthenticator.java`
> - `organized/platform/platform-rt/vineflower/com/tridium/platform/daemon/Authenticator.java`
> - `organized/platform/platform-rt/vineflower/com/tridium/platform/daemon/message/InitializeSharedSecretKeyMessage.java`
> - `organized/platform/platform-rt/vineflower/com/tridium/platform/daemon/LocalSessionUtil.java` (credential type)
> - `sources/probes/B628-platform-daemon-auth/probe-2026-08-29.txt` (live probe, `[CERT-hw]`)
> **Method**: DISK-FIRST (code) then a supervised §12 live probe (RE-MEASURE ground truth). String literals
> for "password" are scrubbed to `ln`/`lns` in this package — cited by structure/type per §5. Markers:
> `[CERT]` `file:line`; `[CERT-hw]` live probe; `[INFER]` deduction.

---

## 628.1 — The credential frame is HTTP Basic or Digest(MD5) `[CERT]`

The daemon auth is the `com.tridium.platform.daemon.Authenticator` family:
- **Basic**: `BasicAuthenticator extends Authenticator` builds `response.append("Basic ").append(
  Base64.getEncoder().encodeToString(userPass.getBytes()))` `[CERT]` (`BasicAuthenticator.java:9,33`; import
  `java.util.Base64` :5) — i.e. the standard `Authorization: Basic base64(user:password)` header.
- **Digest (MD5)**: the base `Authenticator` handles a `digest` scheme (`if (authScheme.equalsIgnoreCase(
  "digest"))`) and hashes with MD5 (`byte[] bytesToHash = md5.digest(in.getBytes())`) `[CERT]`
  (`Authenticator.java:36,106`) — RFC 2617-style HTTP Digest. `Authenticator` is abstract over
  `setAuthorization(HttpConnection, …)` `[CERT]` (`:68-76`), so the credential is set as an HTTP
  Authorization header on the daemon connection.
- The credential TYPE is `BUsernameAndPassword` (`BPassword.make(...)`) carried by a
  `SimpleAuthenticationClient` `[CERT]` (`LocalSessionUtil.java`, `lns`/`setCredentials`/`setAuthenticationClient`).

So the answer to "nonce-response? Fox-SCRAM reuse?" is: **neither** — it is ordinary HTTP Basic/Digest, the
same mechanism a browser uses, over the daemon's HTTP transport.

## 628.2 — Post-auth: a shared-secret AES session key `[CERT]`

After authentication the client sends an `InitializeSharedSecretKeyMessage` carrying a `SharedSecretKey` with
`name`, `salt`, `IV`, and a `verificationMessage`, each Base64-then-URL-encoded `[CERT]`
(`InitializeSharedSecretKeyMessage.java:8-43`). This establishes a symmetric (AES) session key for the
daemon session — the salt/IV/verification triple is the key-agreement material. So the handshake is:
HTTP Basic/Digest credential → shared-secret-key init → message session.

## 628.3 — Live transport confirmation `[CERT-hw]`

§12 read-only probe of the local daemon (`sources/probes/B628-platform-daemon-auth/probe-2026-08-29.txt`):
- **:3011** speaks HTTP/1.1; `OPTIONS /` and `GET /` return `403 Forbidden` with `x-frame-options: deny`,
  `Connection: close`, `Content-Length: 0`; a raw connect gets NO unsolicited greeting (client speaks first).
  No `WWW-Authenticate` on the 403 ⇒ **no 401 challenge** — credentials are presented up-front `[CERT-hw]`.
  Confirms [B460].
- **:5011** negotiates **TLS 1.3** (`TLS_AES_256_GCM_SHA384`), self-signed default cert
  `CN=Niagara4, O=ForRecoveryPurposes` (valid 2025-09-17 → 2026-09-17, SHA1 `1C:24:37:…:34:FA`) `[CERT-hw]`.
  Confirms [B156]/[B398]/[B460] (RE-MEASURED live, not inherited).

## 628.4 — What the blanket 403 does NOT prove (RE-MEASURE A DRAMATIC POSITIVE) `[CERT-hw]`/`[INFER]`

An authenticated HTTP Basic `GET /` and `GET /platform` on :5011 with a STATION account (API2) also returned
`403 Forbidden` — **identical to the unauthenticated case** `[CERT-hw]`. This is INCONCLUSIVE for auth, and it
is exactly the banner-vs-protocol trap the methodology warns against: a blanket GET does not exercise the
daemon's `DaemonMessage` protocol (a POST carrying the Authorization header + a message body). The 403 is a
transport-level refusal of an unsupported request shape, **NOT proof that the station credential was rejected
by the platform daemon** `[INFER]`. A definitive station-vs-platform acceptance test would require a real
`DaemonMessage` client (a §19 build) or platform credentials — it is NOT answerable from a browser-style GET.

The station-vs-platform SEPARATION itself is already established by [B626] `[CERT]` (platform users are a
distinct user space from station `BUser`s); this probe neither confirms nor refutes it at runtime, and I do
not claim it did.

## 628.5 — Resolution `[INFER]`

PO-G7w's core question (the credential frame) is **CLOSED from code** `[CERT]`: HTTP Basic (`base64(user:pass)`)
/ Digest (MD5) + shared-secret AES key init — the wire is standard HTTP auth, not a proprietary digest. The
live transport is confirmed `[CERT-hw]` (:3011 HTTP 403-no-401, :5011 TLS 1.3 ForRecoveryPurposes). The only
part that remains genuinely requires-execution is a runtime station-vs-platform ACCEPTANCE test via a real
DaemonMessage client — low value, since [B626] already answers the separation from code. **Security note**:
because the frame is HTTP Basic, using the plaintext daemon (:3011) transmits `base64(user:password)` in the
clear (trivially reversible) — the `sslOnly` posture ([B626]) is the mitigation, and it matters.

## 628.6 — Connections

- **[B626]** (PO-G7) — the platform-daemon auth MODEL (platform users, `BDaemonSSLStatus`); B628 adds the
  wire FRAME (Basic/Digest+MD5, shared-secret init) and the live transport, closing PO-G7w.
- **[B129] §129.6** — deferred this as "N6-wire"; B628 resolves it (mostly from code — the premise that it
  needed live was only partly true). **[B460]** — the prior live daemon probe (403 no-401, TLS 1.3), here
  RE-MEASURED.
- **[B627]** — the master port table; :3011/:5011 row's auth detail is now fully specified.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Basic frame: `Authorization: Basic base64(user:password)` | `[CERT]` | BasicAuthenticator.java:9,33 | ✓ read |
| 2 | Digest scheme uses MD5; credential set as HTTP Authorization header | `[CERT]` | Authenticator.java:36,106,68-76 | ✓ read |
| 3 | Credential type = `BUsernameAndPassword` / `SimpleAuthenticationClient` | `[CERT]` | LocalSessionUtil.java | ✓ read |
| 4 | Post-auth `InitializeSharedSecretKeyMessage` = salt+IV+verification (AES session key) | `[CERT]` | InitializeSharedSecretKeyMessage.java:8-43 | ✓ read |
| 5 | :3011 HTTP 403, no WWW-Authenticate (no 401 challenge); client speaks first | `[CERT-hw]` | probe-2026-08-29.txt | ✓ probe |
| 6 | :5011 TLS 1.3, self-signed ForRecoveryPurposes cert (RE-MEASURED) | `[CERT-hw]` | probe-2026-08-29.txt | ✓ probe |
| 7 | Authenticated GET returns identical 403 → INCONCLUSIVE for auth (not a rejection) | `[CERT-hw]`/`[INFER]` | probe-2026-08-29.txt | ✓ probe + reasoned |
| 8 | Basic over plaintext :3011 exposes base64(user:pass) → sslOnly matters | `[INFER]` | #1 + [B626] | ✓ reasoned |

**Tally**: `[CERT]` = 4 · `[CERT-hw]` = 3 · `[INFER]` = 2 (one shared). Block type = EVIDENCE (code + live).
PO-G7w frame CLOSED; runtime acceptance-test residual is low-value (separation already [CERT] in B626).
**Secrets check**: no credential value in this block, the probe file, `sources/`, or engram — only the frame
STRUCTURE (`Basic base64(user:password)`). The station account value was supplied via a shredded scratchpad
`-K` file and is compromised-in-transcript only (operator advised to rotate).
