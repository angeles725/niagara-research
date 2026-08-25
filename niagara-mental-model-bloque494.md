# Block 494 — `oem-honeywell-tail` U14: the pluggable authentication schemes beyond RBAC+Fox (SAML, LDAP/Kerberos, gauth TOTP, clientCertAuth, and oauth2-as-a-client) — architecture and security posture

> **Focus:** `oem-honeywell-tail`, gap U14 — extended auth/identity beyond [B11]/[B30] RBAC+federation
> (`electronicSignature*` excluded, done [B350-356]). READ-ONLY, decompiled source; no binary run. Markers §3.
> All modules vendorVersion 4.14.0.162. Pre-flight PRESENT: `saml`(~60), `samlEncryption`(3), `oauth2`(9),
> `ldap`(~40 incl Kerberos `v3/`), `gauth`(17), `clientCertAuth`(15).

## §494.1 — Shared framework `[CERT]`

Base `javax.baja.authn.BAuthenticationScheme` (`getSchemeName`, `getLoginConfiguration`→JAAS `Configuration`,
`getDefaultAuthenticator`→`BAbstractAuthenticator`); intermediates `BSSOAuthenticationScheme`
(`getLoginRedirectURL`) and `BPasswordAuthenticationScheme`. `com.tridium.authn.BAuthenticationService` holds the
`authenticationSchemes` folder + `defaultAuthenticationScheme` (default `"DigestScheme"`,
`BAuthenticationService.java:119-120`). **Plug model:** each scheme is `@AgentOn(types={"baja:AuthenticationScheme"})`
dropped into the folder; login runs its JAAS LoginModule with Fox + Web CallbackHandlers.

## §494.2 — SAML (`saml`+`samlEncryption`) `[CERT]`

`BSAMLAuthenticationScheme extends BSSOAuthenticationScheme`, scheme `"n4saml"`, redirect `/saml/samlrp`
(`BSAMLAuthenticationScheme.java:78-241`). **N4 is both SP and IdP** (IdP license-gated on feature `"samlDP"`,
`BSAMLIdPService.java:122,320-336`). SP flow: AuthnRequest **HTTP-Redirect** (DEFLATE+Base64) → IdP → ACS
**HTTP-POST** (`SAMLConsumerServlet.doPost:77-101`) → validate → **auto-provision `BUser`** → `/j_security_check`.
Validation (`rp/Response.checkValidity:130-177`): signature vs the IdP cert in **USER_TRUST_STORE** (onelogin
`Utils.validateSign`), NotBefore/NotOnOrAfter (skew 3 min), SubjectConfirmation Recipient==ACS + window,
Destination, Status=Success, exactly-one-assertion, XSD + XXE/DOCTYPE reject; **replay** via one-time
`SAMLUuidMap` (10-min TTL). Roles via prototype (+LDAP-DN `CN=` parse). `samlEncryption` decrypts
`<EncryptedAssertion>` (Apache Santuario, KEK=station private key) — optional.
- **Posture:** bundled **onelogin java-saml 1.x fork** (CVE-era) with Tridium's own re-validation + XSW mitigation
  (query re-anchored to the signed Reference); **RSA-SHA1** request signing; **audience check skipped if
  AudienceRestriction absent** (`Response.java:286`); **InResponseTo value-compare skipped** (bound instead by
  the one-time UuidMap). Trust = USER_TRUST_STORE (not cacerts). No hardcoded secrets.

## §494.3 — oauth2 (`oauth2`) — an outbound CLIENT library, not a login scheme `[CERT]`

No class extends `BAuthenticationScheme`; module.xml registers no types. It is an OAuth2/OIDC **client** (Nimbus
SDK) whose only in-corpus consumer is the **email module** (SASL **XOAUTH2** for SMTP/IMAP,
`BAbstractOAuthEmailAuthenticator.java:157-198`). Grants: `client_credentials` + `jwt-bearer` (RS256,
`private_key_jwt`); **no authorization_code/PKCE** (non-interactive, appropriate). Endpoint-integrity checked
(computed URI must equal OIDC-discovered `token_endpoint`); **no inbound token signature/JWKS validation** (token
consumed as opaque bearer — by design). Secrets = encrypted `BPassword`; no provider hardcoded.

## §494.4 — LDAP + Kerberos (`ldap`) `[CERT]` — the weakest defaults

`BLdapAuthenticationScheme` (`"n4LDAP"`, JAAS `LdapLoginModule`); `BKerberosAuthenticationScheme extends
BSSOAuthenticationScheme` (`"n4Kerberos"`, redirect `/login-kerb`).
- **Bind = bind-as-user, mechanism hardcoded `"simple"`** (`BLdapConfig.java:676-678`): initial bind → subtree
  search → `rebind` as the resolved DN with the user's password (no local hash compare).
- **TLS `SSL` default `false` and never enforced** (`BLdapConfig.java:107,112`) → **default is a cleartext simple
  bind** with the user password in `java.naming.security.credentials` (`:353`). When on: `ldaps` via
  `BajaSSLSocketFactory` (no StartTLS).
- **Referral `follow` default** (`:123`) → malicious-referral credential redirection.
- Roles via prototype → `user.setRoles(...)`; **auto-provisions** users; local cache (default **7 days**) enables
  offline login, re-hashed to `BPasswordCache` via **PBKDF2-HMAC-SHA256**.
- **Kerberos:** GSSAPI bind; keytabs AES-encrypted at rest (KeyRing); but **unconditional credential delegation**
  — `requestCredDeleg(true)`+`requestMutualAuth(true)` (`v3/GSSDelegCredAction.java:46-47`) + process-global
  `useSubjectCredsOnly=false` (`AcquireHttpCredentialsAction.java:32`) → a compromised station can impersonate the
  delegating user to other Kerberized services. TLS factory installed on the Kerberos path only if
  `CertManagerFactory.isCertManagerActive()`, else JVM-default trust.

## §494.5 — gauth (`gauth`) — TOTP 2FA (not OAuth) `[CERT]`

`BGoogleAuthenticationScheme extends BPasswordAuthenticationScheme`, `"gauth"`; JAAS `GoogleAuthLoginModule`
requires **both** SCRAM password AND a valid `token` (`GoogleAuthLoginModule.java:103`) — a true second factor.
RFC-6238 **TOTP, HMAC-SHA1, 6 digits, 30-s step** (`GoogleAuthenticator.java:80,129-140`); QR via bundled ZXing.
- **Posture (weak):** validation window **±3 steps ≈ 210 s** (wider than RFC ±1); secret **80-bit** (`SECRET_SIZE=10`,
  below RFC-4226's 128-bit min); replay protection is an **in-memory static HashMap keyed by the cleartext secret**
  (lost on restart, not cluster-shared); **no module rate-limit/lockout**; secret shown **cleartext in the QR reset
  form + HTTP session** during enrollment. No hardcoded secret (forced reset on first login).

## §494.6 — clientCertAuth (`clientCertAuth`) — mutual-TLS with cert pinning `[CERT]`

`BClientCertAuthScheme extends BSSOAuthenticationScheme implements TrustAnchorProvider`, `"clientcert"`,
`supportsRemoteUsers()=false`; registers with `CoreCryptoManager` as a trust-anchor provider. Cert from the TLS
layer (`javax.servlet.request.X509Certificate` / Fox `SSLSession.getPeerCertificates`). **App-layer auth = exact
whole-cert equality (pin)**: `X509Certificate.equals()` vs the one `BX509Certificate` stored on the user
(`BClientCertAuthenticator.java:41-65`); trust anchors = **each user's own stored cert** as its own `TrustAnchor`
(self-anchored — NOT cacerts, NOT the TPK). CN/SAN only *select* candidate usernames; equality decides (no
CN-spoof at app layer). **No in-module chain-signature, expiry (`checkValidity`), or revocation (CRL/OCSP) check**
— relies on the TLS handshake (key-possession) + the pin. Consistent with the revocation-off finding [B482]/[B489],
but the client-cert trust decision does not reuse cacerts/TPK.

## §494.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | Plug model: BAuthenticationScheme + @AgentOn(baja:AuthenticationScheme); default DigestScheme | `[CERT]` | `BAuthenticationService.java:119-120` | PASS |
| 2 | SAML SP+IdP (IdP gated samlDP); validate vs USER_TRUST_STORE; one-time UuidMap replay; auto-provision | `[CERT]` | `Response.java:130-262`; `SAMLConsumerServlet.java:77-183` | PASS |
| 3 | SAML posture: onelogin 1.x + RSA-SHA1; audience skipped if absent; InResponseTo value-compare skipped | `[CERT]` | `Response.java:157-162,286` | PASS |
| 4 | oauth2 = outbound client (email XOAUTH2), not a login scheme; no inbound token sig check | `[CERT]` | `oauth2` module.xml; `BAbstractOAuthEmailAuthenticator.java:157-198` | PASS |
| 5 | LDAP simple bind, TLS off/unenforced (cleartext), referral follow, 7-day cache; Kerberos unconditional deleg | `[CERT]` | `BLdapConfig.java:107,112,123,676-678`; `v3/GSSDelegCredAction.java:46-47` | PASS |
| 6 | gauth TOTP HMAC-SHA1, ±3 window, 80-bit secret, in-mem replay, no rate-limit | `[CERT]` | `GoogleAuthenticator.java:80,82,129-140` | PASS |
| 7 | clientCertAuth = exact cert pin + per-user self-anchor; no chain/expiry/revocation in-module | `[CERT]` | `BClientCertAuthenticator.java:41-65`; `BClientCertAuthScheme.java:83-93` | PASS |

**Tally:** 7 claims, all `[CERT]`, 0 `[INFER]`.

## §494.8 — Connections & security feed

- Advances `oem-honeywell-tail` (U14). Ties to the auth base [B11]/[B30] and trust infra [B482]/[B489]
  (USER_TRUST_STORE, self-anchors, revocation-off — NOT cacerts/TPK reuse here).
- **Security-audit feed ([B490]):** candidate items — LDAP cleartext-simple-bind default (SSL off) + referral
  follow; Kerberos unconditional delegation + `useSubjectCredsOnly=false`; gauth weak TOTP params + no lockout;
  clientCert no revocation/expiry. All are **default/config postures**, not hardcoded backdoors; secrets at rest
  are encrypted `BPassword`/KeyRing.
- Open (this focus): U10 (other-vendor OEM drivers, in flight → B495), U11-U13/U15 (LOW/out-of-mission).
