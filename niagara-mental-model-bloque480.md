# Block 480 — Subscription onboarding and the trust model: OAuth device-code at niagara-community, register + entitlement at niagaracentralapis.honeywell, on-device ES256 JWT auth, vendor-cert signature verification of returned licenses, and monotonic-increment clone protection (closes B479-G1)

> **Focus:** `licensing` (gap B479-G1). **Question (operator):** cómo se maneja la suscripción, cómo se toma
> la petición, la confianza, quién la maneja, quién lo verifica. Completes [B477] (runtime check-in), [B478]
> (daemon enforce), [B479] (perpetual install/sync) with the SUBSCRIPTION onboarding + trust chain.
>
> **Sources:** decompiled corpus already present — `sources/decompiled/nre-ext/com/tridium/nre/subscription/*`,
> `sources/decompiled/niagarad-ext/…/servlet/UpdateDaemonServlet.java`,
> `organized/baja/…/com/tridium/sys/license/subscription/SubscriptionLicenseManager.java`,
> `organized/platform/platform-rt/…/BLicensePlatformService.java`. READ-ONLY: no binary run. No new artifacts.
> **SECRETS DISCIPLINE:** key/token structure only. Markers per §3.

## §480.1 — TWO servers, two auth schemes `[CERT]`

- **OAuth device-code + access-token** → `https://www.niagara-community.com/services/oauth2/token`
  (Salesforce-style). `DeviceCodeApi.java:15-32`, `AccessTokenApi.java:70-87`; host const
  `EntitlementUtil.java:52` (`DEVICE_REGISTRATION_HOST`).
- **register / entitlements / certificates / key-rotation** → `https://www.niagaracentralapis.honeywell.com`
  `/ncents/{register,entitlements,certificates,authn/api_key}`. `RegistrationApi` does NOT override
  `getConnectionUrl()`, so it inherits the Honeywell host from `EntitlementApi.java:31-33`;
  `RegistrationApi.java:24-27` (`/ncents/register`), `EntitlementUtil.java:45-65`.
- **§14 refinement of [B477 §477.3]:** the OAuth *approval* host is `niagara-community.com` (correct), but the
  actual `register` API call hits `niagaracentralapis.honeywell.com/ncents/register` — the two are different
  hosts in one flow.

## §480.2 — The device-code (OAuth2 device-authorization) flow `[CERT]`

- Hardcoded `DEVICE_REGISTRATION_CLIENT_ID` = an 85-char Salesforce consumer key (`EntitlementUtil.java:53`,
  overridable via `license.clientId`).
- `DeviceCodeApi`: POST `client_id=<id>&response_type=device_code` (`x-www-form-urlencoded`) → parses
  `device_code, user_code, verification_uri, interval` (`DeviceCodeApi.java:60-82`, 3 retries).
- `startAccessTokenPoll` relays `user_code`+`verification_uri`+`interval` to the UI, then polls in the
  background (`UpdateDaemonServlet.java:482-490`). User approves at the `verification_uri`.
- `AccessTokenApi`: POST `client_id=<id>&grant_type=device&code=<deviceCode>` → captures
  `access_token, signature, scope, instance_url, id, token_type, issued_at` (`AccessTokenApi.java:13-38`).
  Poll window **10 min** (`:10,120-121`); `authorization_pending`→keep, `too_fast`→**double interval**
  (`:148-151`); thread `"Nre:PollAccessToken"`.

## §480.3 — Register `[CERT]`

- License key format `XXXX-XXXX-XXXX-XXXX`, hex only: `SUBSCRIPTION_KEY_REGEX = "[A-F0-9]{4}-…"`
  (`SubscriptionLicenseUtil.java:41,571-573,599`); daemon rejects the literal placeholder
  (`UpdateDaemonServlet.java:540-545`).
- `RegistrationApi.register` → POST `/ncents/register` (Honeywell host) with JSON `{id, issued_at, signature
  (from the OAuth access-token), metadata{platform,type}, nreId, licenseKey, publicKey(Base64 EC pubkey),
  restoreId}`; header `Authorization: Bearer <access_token>` (`RegistrationApi.java:99-130`,
  `EntitlementUtil.java:154-159`).
- Response = a **bare status** (`type=="registration" && message=="success"`), NOT licenses/certs; on success
  writes the `.registered` marker (an `Instant`) (`RegistrationApi.java:136-178`). The actual license material
  is pulled afterward by the entitlement check-in (§480.5). Remote-device variant registers under
  `db/<licenseKey>/` (`:80-87`).

## §480.4 — Host-ID minting, markers, storage `[CERT]`

- `establishNreId`: `UUID.randomUUID()` reformatted + upper-cased + `"Nre-"` prefix → persisted (file-locked)
  in `<subscription>/nreId` (`SubscriptionLicenseUtil.java:346-367`). `regenerateNreId()` **deletes the entire
  subscription dir** and re-mints (`:178-186`; station `SubscriptionLicenseManager.java:567-578`; daemon
  `UpdateDaemonServlet.java:439-448`) → forces full re-onboarding.
- `getHostIdStatus()` states: `perpetual` / `unregistered` (not registered, not cloned) / `cloned` / `ok`
  (`SubscriptionLicenseUtil.java:68-82`).
- Subscription dir holds `licenses/`, `db/`, `certificates/`, `nreId`, `.restoreId`, `.registered`, `.cloned`,
  `.ecKeyPair`, `.refreshIncrement`, `.kr`/`.km` (`:52-57`). Licenses/certs written only if signature/`generated`
  changed (`:402-431,490-544`).

## §480.5 — Ordered onboarding state machine `[CERT]`

1. Subscription mode active (`license.subscriptionMode` or platform forces it; gated by
   `niagara.license.subscriptionLicenseAllowed`) (`SubscriptionLicenseUtil.java:84-96,583-585`).
2. Mint `Nre-…` HostId → status `unregistered`.
3. Device-code request → UI shows `user_code`+`verification_uri` (`UpdateDaemonServlet.java:461-490`).
4. Poll access token ≤10 min (`AccessTokenApi.java:113-176`).
5. Register with `licenseKey` → `.registered` → status `ok` (`UpdateDaemonServlet.java:532-586`).
6. First entitlement pull at `postInit` (if `ok` and no local licenses) → `/ncents/entitlements`
   (`SubscriptionLicenseManager.java:116-143`).
7. Schedule periodic entitlement check (**6 h** + 30 min + ≤900 s jitter, retry limit **3**) + key rotation
   (check daily, rotate **90 d**) (`SubscriptionLicenseManager.java:63-71,165-201,217-220`).

## §480.6 — TRUST & VERIFICATION (the operator's core question) `[CERT]`

**Cómo se toma la petición (outbound auth):**
- OAuth stage = `Bearer <access_token>`. Entitlement/cert/rotate stage = a **device-signed JWT** ES256, header
  `kid="K1"`, claims `sub=nreId`, `aud=niagaracentralapis.honeywell.com`, `exp=10 min`, signed with the
  on-device EC private key under a `GET_ENTITLEMENT_PRIVATE_KEY` permission (`EntitlementUtil.java:76-110`,
  `EntitlementApi.java:332-342`, `JwtSignatureKeys.java:162-168`).
- The key: **secp256r1 EC keypair generated ON-DEVICE**, private key never transmitted, stored AES-256-encrypted
  in `<subscription>/.ecKeyPair` (KeyRing alias `baja.licensing.subscription.ecKeyPair`)
  (`JwtSignatureKeys.java:42,125-133,246-267,362-385`). The server learns the **public** key at register and via
  `/ncents/authn/api_key` rotation (authenticated by the current key, commit-after-server-confirm)
  (`RotateKeys.java:14-39`, `JwtSignatureKeys.java:388-486`).

**Quién lo verifica (inbound response trust):**
- **Returned licenses ARE signature-verified before trust.** `RetrieveEntitlements.isLicenseValid`
  (`:212-263`): hostId must match local nreId, `<signature>` required, vendor `.certificate` fetched if absent,
  then `SubscriptionLicenseManager.isLicenseSignatureValid(XElem,File)` (`:394-436`) loads the vendor
  `CertificateFile`, strips the sig, `LicenseUtil.encode`, `LicenseUtil.verify(xml, sig, publicKey, algorithm)`.
  **Trust anchor = the vendor certificate's public key** (the cert chains to the embedded root per [B395]).
- **GAP `[CERT]`:** when running as the daemon (`Boolean.getBoolean("NiagaraDaemon")`), signature validation is
  **SKIPPED** ("running as niagara daemon, skipping license signature validation")
  (`RetrieveEntitlements.java:246-251`) — the daemon trusts TLS + the station's later re-validation.
- The entitlement **response envelope itself is NOT separately signed** — its transport trust is TLS; only the
  embedded license XML carries a verified signature. `[INFER]`
- **TLS:** `HttpConnectionlessTransport` uses `ConnectionSpec.RESTRICTED_TLS` in production; the trust-all path
  is entered ONLY if `!SecurityConstants.canCheckTpk()` (dev builds), and `AlwaysTrustManager` throws if used
  in prod (`:74,82-90,224-257`). No certificate pinning `[INFER]`; relies on the platform truststore +
  hostname verification.

**Quién la maneja (ownership):**
- Station: `SubscriptionLicenseManager` (schedulers, `checkEntitlement`, reload, `licenseFailure`, the signature
  verify) (`:54-201,242-321,394-436`).
- Platform: `BLicensePlatformService` — registers the `"license"` Fox channel and, as an
  `EntitlementStatusListener`, raises/clears a **platform alarm** (`alarmType "license"`) on every check-in
  (`BLicensePlatformService.java:124-133,180-198`).
- Daemon: `UpdateDaemonServlet` (device-code poll, register, regenerateNreId, updateSubscriptionLicense)
  (`:362-620`).
- Crypto material owners: `JwtSignatureKeys`, `RestoreId`, `RefreshIncrement` (all AES-256 + KeyRing).

## §480.7 — Replay / clone protection `[CERT]`

Each request carries `nreId, productId, refreshIncrement (monotonic, persisted, ++ per request), restoreId,
nonce (RANDOM.nextInt())` (`RetrieveEntitlements.java:69,138-145`; `RefreshIncrement.java:46-60`;
`LicenseRefreshToken.java:24-38`). `[INFER]` the server tracks the highest `refreshIncrement` per
(nreId,restoreId); a clone replays a stale increment → server `type="invalid refresh token"` → HTTP 409
`INVALID_REFRESH_TOKEN` (`RetrieveEntitlements.java:284-289`) → station writes `.cloned`, deletes licenses+certs,
`licenseFailure`, logs "This instance has been cloned…" (`SubscriptionLicenseManager.java:291-303`). An
authorized backup-restore is distinguished by `restoreId` + `reregistrationCause="backup-restoration"` +
`RefreshIncrement.reset()` (`SubscriptionLicenseUtil.java:231-253`). Secrets `publicKey/refreshIncrement/
restoreId` are redacted to `********` in logs (`EntitlementApi.java:252-270`).

## §480.8 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | OAuth device-code/token → niagara-community.com/services/oauth2/token; register/entitlement → niagaracentralapis.honeywell.com/ncents/* | `[CERT]` | `DeviceCodeApi.java:15-32`; `RegistrationApi.java:24-27`; `EntitlementApi.java:31-33` | PASS |
| 2 | Device-code grant: hardcoded client_id, user_code+verification_uri, 10-min poll, too_fast→double | `[CERT]` | `EntitlementUtil.java:53`; `DeviceCodeApi.java:60-82`; `AccessTokenApi.java:10,148-151` | PASS |
| 3 | Register sends nreId+licenseKey(`[A-F0-9]{4}-…`)+publicKey+restoreId, Bearer access_token; returns bare status + `.registered` | `[CERT]` | `RegistrationApi.java:99-178`; `SubscriptionLicenseUtil.java:41` | PASS |
| 4 | HostId `Nre-`+UUID in `<sub>/nreId`; regenerateNreId wipes subscription dir | `[CERT]` | `SubscriptionLicenseUtil.java:178-186,346-367` | PASS |
| 5 | Outbound entitlement auth = on-device-signed JWT ES256 kid=K1; EC key generated on-device, AES-256 at rest | `[CERT]` | `EntitlementUtil.java:76-110`; `JwtSignatureKeys.java:42,125-133,246-267` | PASS |
| 6 | Returned licenses signature-verified (vendor cert → LicenseUtil.verify); daemon SKIPS verify | `[CERT]` | `RetrieveEntitlements.java:212-263,246-251`; `SubscriptionLicenseManager.java:394-436` | PASS |
| 7 | TLS RESTRICTED_TLS in prod; trust-all only dev; no pinning | `[CERT]`/`[INFER]` | `HttpConnectionlessTransport.java:74,82-90,224-257` | PASS |
| 8 | Ownership: SubscriptionLicenseManager / BLicensePlatformService (alarm) / UpdateDaemonServlet | `[CERT]` | `SubscriptionLicenseManager.java:54-335`; `BLicensePlatformService.java:124-198`; `UpdateDaemonServlet.java:362-620` | PASS |
| 9 | Clone protection: monotonic refreshIncrement + nonce + restoreId; 409 INVALID_REFRESH_TOKEN → .cloned | `[CERT]` | `RefreshIncrement.java:46-60`; `SubscriptionLicenseManager.java:291-303` | PASS |

**Tally:** 9 claims, 9 `[CERT]` (7 with an `[INFER]` sub-part for server-side behavior/pinning), 0 unmarked.
Server-side logic is `[INFER]` (client-only evidence).

## §480.9 — Connections

- Completes the licensing focus: perpetual path [B479]→[B477]→[B478]; subscription path **[B480] onboarding →
  [B477 §477.3] check-in → [B478] enforce**.
- §14 refines [B477 §477.3] (register host is the Honeywell central API, not the community OAuth host).
- Trust thesis (consistent with [B392]/[B479]): the **license-level signature** (vendor cert → embedded root)
  is the integrity anchor even for server-returned entitlements; TLS is transport trust; the platform/daemon
  paths defer or skip crypto (daemon skips license-sig; platform install does no verify [B479 §479.6]).

## §480.10 — Open gaps

- **B480-G1** live §12 of a subscription onboarding (device-code approval + `nre -licenses` showing
  `Licenses (subscription)`) — requires a subscription-mode host (this install is perpetual). Deferred.
- **B478-G1** native watchdog `shmem` (liveness, deferred).
