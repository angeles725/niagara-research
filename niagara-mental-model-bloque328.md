# Block 328 — OAuth2 SMTP: XOAUTH2 over SASL with the bearer token as the "password", `client_credentials` by secret or certificate, and a token fetched fresh on every send

> Focus **email** — evidence block E5. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the OAuth2 authenticators that let N4 send SMTP against providers that killed basic auth (Microsoft 365,
> Google): `com.tridium.email.BAbstractOAuthEmailAuthenticator` and its two concrete grants
> (`BOAuthEmailClientSecretAuthenticator`, `BOAuthEmailClientCertificateAuthenticator`). This is the
> `getEmailAuthenticator().make().connectOutgoingSession(transport)` path that [Block 325] §325.5 and
> [Block 326] §326.1 deferred to E5. It is why the module depends on `oauth2-rt`.
>
> Sources (primary, decompiled N4.14.0.162), sweep sonnet + driver re-verification of the 6 load-bearing lines:
> `organized/email/email-rt/vineflower/com/tridium/email/BAbstractOAuthEmailAuthenticator.java` (224),
> `.../BOAuthEmailClientSecretAuthenticator.java` (76), `.../BOAuthEmailClientCertificateAuthenticator.java` (141).
>
> EXCLUDES (gap E7): the basic/no-auth authenticators and the `BEmailClientAuthenticator` base contract beyond
> the OAuth override points; the `Session`/TLS assembly is [Block 326].
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. Layer 8 (notification — auth tier)
> + Layer 22 (security/credentials). Block TYPE: **evidence**.

---

## 328.1 — XOAUTH2 is negotiated over SASL, not JavaMail's `auth.mechanisms`

`setOutgoingAuthenticationProperties(Properties)` on the OAuth base sets, verbatim `[CERT]`
(`BAbstractOAuthEmailAuthenticator.java:161-165`):

```java
properties.put("mail.smtp.sasl.enable", "true");
properties.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
properties.put("mail.smtp.auth.login.disable", "true");
properties.put("mail.smtp.auth.plain.disable", "true");
```

So the OAuth path drives JavaMail's **SASL** layer (`mail.smtp.sasl.mechanisms=XOAUTH2`), NOT the
`mail.smtp.auth.mechanisms` shortcut, and it actively DISABLES the legacy `AUTH LOGIN` / `AUTH PLAIN`
mechanisms so a misconfigured server cannot silently downgrade to sending the token as a plaintext password
`[CERT]` (`:164-165`), `[INFER]` (the disable is a downgrade-guard). The prefix is hardcoded `mail.smtp` — this
authenticator is the OUTGOING/SMTP OAuth path `[CERT]` (`:161`).

## 328.2 — The token IS the password to `transport.connect`

`connectOutgoingSession(Transport)` `[CERT]` (`:185-193`):

```java
transport.connect(this.getAccount(), this.getAccessToken());   // :188 (under doPrivileged)
```

The bearer token is handed to JavaMail as the **password argument** of `Transport.connect(user, password)`; the
username is the account's `account` property. JavaMail's XOAUTH2 SASL client then wraps that string into the
`user=...\x01auth=Bearer <token>\x01\x01` XOAUTH2 blob `[CERT]` (`:188`), `[INFER]` (the SASL encoding is
JavaMail's, driven by §328.1). A failure wraps into `MessagingException("Unable to connect to SMTP transport!")`
`[CERT]` (`:190`).

## 328.3 — Token lifecycle: metadata cached, ACCESS TOKEN fetched fresh every time

`getAccessToken()` `[CERT]` (`:101-131`):

1. If `authServerMetadata == null`, resolve it via
   `resolveAuthServerMetadata(new OAuth2AuthorizationServerMetadataResolver())` — an OIDC discovery fetch of the
   token endpoint from `authServerMetadataEndpoint` `[CERT]` (`:103`, `:138-139`).
2. `OAuth2AuthorizationRequest tokenRequest = getAuthorizationRequest()` (the concrete grant, §328.4) `[CERT]`
   (`:106`).
3. `tokenResponse = doPrivileged(tokenRequest::send)` `[CERT]` (`:110`).
4. On `Result.SUCCESS` → `return tokenResponse.get("access_token")` `[CERT]` (`:116`).

**The OIDC metadata is cached** (`authServerMetadata` field, re-resolved only when the endpoint property changes)
`[CERT]` (`:57`, `:213-214`). **The access token is NOT** — there is no token field, no expiry check, no refresh:
`getAccessToken()` posts to the token endpoint on EVERY call `[CERT]` (`:101-131`, whole method). Since
`connectOutgoingSession` calls it per `transport.connect`, and `pollQueue` connects per drain cycle
([Block 325] §325.5), **every send batch triggers a fresh `client_credentials` token request** `[INFER]`. For a
busy alarm station this is extra latency and token-endpoint load, and it leans on the provider not rate-limiting
the client-credentials grant `[INFER]`.

## 328.4 — Two grants, both `client_credentials`; the cert one can also do JWT-bearer

`getAuthorizationRequest()` is abstract on the base; each concrete class builds a different `oauth2-rt` request:

**Client-secret** (`BOAuthEmailClientSecretAuthenticator`) `[CERT]` (`:41-70`, request at `:48`):

```java
new OAuth2ClientCredentialsGrantRequest(getAuthServerMetadata(), getClientId(),
                                        passwordChars.get(), new String[]{getScope()});
```
The shared secret is read as a `char[]` through `SecretChars` and zeroed in `finally` — no plaintext `String`
`[CERT]` (`:43`, `:53-63`). Grant: `client_credentials` with a shared secret `[CERT]` (`:48`).

**Client-certificate** (`BOAuthEmailClientCertificateAuthenticator`) `[CERT]` (`:93-135`, ternary `:126-128`):

```java
useJWTBearerAssertion
  ? new OAuth2JWTBearerGrantRequest(meta, clientId, certificate, privateKey, scope)      // :127
  : new OAuth2ClientCredentialsGrantRequest(meta, clientId, certificate, privateKey, scope); // :128
```
The cert + private key come from Niagara's crypto keystore (`ICoreCryptoManager.getKeyStore().getCertificate/
getKey(alias, pwd)`) `[CERT]` (`:96`, `:103`). Toggle `useJWTBearerAssertion` `[CERT]`:
- `false` (default) → `client_credentials` carrying the X.509 cert + key (mTLS / certificate credential to the
  token endpoint) `[INFER]`.
- `true` → `OAuth2JWTBearerGrantRequest` — a signed JWT client assertion posted to the token endpoint `[CERT]`
  (`:127`).

## 328.5 — Property surface and secret hygiene

| Class | Property | Type | Default |
|---|---|---|---|
| base | `authServerMetadataEndpoint` | String | `"https://endpoint.server.com/path/to/endpoint"` (OIDC discovery URL — subsumes tenant/authority) |
| base | `account` | String | `""` (the SMTP username / mailbox) |
| base | `clientId` | String | `""` |
| base | `scope` | String | `""` |
| secret | `clientSecret` | **`BPassword`** | `BPassword.DEFAULT` |
| cert | `clientCertificate` | String (keystore ALIAS, `CertificateAliasFE`) | `""` |
| cert | `privateKeyPassword` | **`BPassword`** | `BPassword.DEFAULT` |
| cert | `useJWTBearerAssertion` | boolean | `false` |

`[CERT]` (`BAbstractOAuthEmailAuthenticator.java:31-55`, `BOAuthEmailClientSecretAuthenticator.java:17-23`,
`BOAuthEmailClientCertificateAuthenticator.java:30-57`). There is NO tenant/authority property — the token
endpoint is discovered dynamically from `authServerMetadataEndpoint` `[CERT]`. Secrets are `BPassword`
(Niagara's keyring-encrypted-at-rest credential [Block 34] §34.6.5), never plaintext at rest; runtime access is
via zeroed `SecretChars` `[CERT]` — I cite STRUCTURE only, no values (SECRETS DISCIPLINE; source is decompiled,
carries no real secret). The certificate authenticator stores only an ALIAS; the key material stays in the
crypto keystore `[CERT]` (`:96`, `:103`).

## 328.6 — Why this exists (context)

Microsoft 365 and Google retired basic-auth SMTP; modern relays require OAuth2 `client_credentials` +
XOAUTH2 `[INFER]` (well-known industry change, not in-source). That is exactly what this trio implements, and why
`oauth2-rt` is a module dependency [Block 324] header. An operator on M365/Gmail configures an OAuth authenticator
(app registration → clientId + secret OR cert, tenant discovery URL, scope), not a basic `account`+`password`
`[INFER]`.

## 328.7 — What this block does NOT resolve

- The basic / no-auth authenticators (`BBasicEmailClientAuthenticator`, `BNoAuthEmailClientAuthenticator`) and
  the `BEmailClientAuthenticator` base + `BEmailAuthenticatorTypeConfig` migration → **E7**.
- `oauth2-rt` internals (`OAuth2*GrantRequest.send`, metadata resolver HTTP) — outside the email focus.
- The INCOMING OAuth path (IMAP XOAUTH2) — the base sets `mail.smtp` prefix here; whether incoming reuses it is
  an E7/E4 follow-up `[INFER]`.

## 328.8 — Connections

- [Block 325] §325.5 — `pollQueue` → `connectOutgoingSession`; §328.3 explains the per-drain token cost.
- [Block 326] §326.1 — the authenticator fills the SAME `Properties` map the TLS session uses; §328.1 is that fill.
- [Block 34] §34.6.5 — `BPassword`/keyring credential path, reused here for `clientSecret`/`privateKeyPassword`.
- [Block 324] — `oauth2-rt` module dependency, realized by this block.

## 328.9 — Self-verify

Block TYPE: **evidence**. Delegated sweep **sonnet**; driver re-verified all 6 load-bearing citations inline
(SASL/XOAUTH2 `:161-165`, no-cache `getAccessToken` `:101-131`, `transport.connect(account, token)` `:188`,
secret grant `:48`, cert ternary `:126-128`, `clientSecret` = `BPassword` `:17-23`) — 6/6 verbatim. The
"token fetched every send" claim (§328.3) was confirmed by reading the whole method for any cache/expiry field —
none present.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (local file:line) | 25 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 9 |
| INFER/CERT ratio | 0.36 |

`verify-block.sh` exit 0.

Evidence block: `[INFER]`s are the per-send token-cost chain, the mTLS-vs-JWT reading, and the industry context —
each anchored to a cited `[CERT]` behavior.
