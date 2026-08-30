# B669 — Storing the webhook token: a `@NiagaraProperty type="BPassword"` is directly reusable for an `Authorization: Bearer` header — declare it, read plaintext at send time via `AccessController.doPrivileged(pwd::getValue)`, exactly as `BBasicEmailClientAuthenticator` does for SMTP; the token MUST be a REVERSIBLE password, encrypted at rest by a module-specific key + the station keyring (`.km`/`.kr` DPAPI on Windows) (focus alarm-webhook, AW4; confirms B34 §34.6.5)

> **Focus:** `alarm-webhook` (§16). **Gap closed:** AW4 (is the `BPassword` + keyring pattern documented for
> SMTP directly reusable for a Bearer-token header on an outbound POST). **Phase:** static, READ-ONLY.
> **Sources** (all `[CERT]`):
> - `organized/docSource/docSource-doc/extracted/baja/javax/baja/security/BPassword.java` (the `getValue()` /
>   `getSecretChars()` contract + the module-key/permission note).
> - `organized/email/email-rt/vineflower/com/tridium/email/BBasicEmailClientAuthenticator.java` (the LIVE
>   reference: a `BPassword` property read via `doPrivileged` at send time).
> - `[CERT]` corpus [Block 34] §34.6.5 (SMTP credentials via keyring/BPassword), REMITTANCE [Block 5] §5.4 +
>   [Block 13] §13.2 (station keyring `.km`/`.kr`, DPAPI on Windows), [Block 330] (email authenticators).
>
> **Bottom line for the PoC:** YES — directly reusable. Add `@NiagaraProperty(name="token", type="BPassword")`
> to `BMiWebhookRecipient`, and in `sendAlarm` read the plaintext with
> `String t = AccessController.doPrivileged((PrivilegedAction<String>) getToken()::getValue);` then set header
> `Authorization: Bearer <t>`. The token is encrypted at rest by a **module-specific key** (since Niagara 4.6)
> and the station **keyring** (`.km`/`.kr`, DPAPI-wrapped on Windows) — the same protection SMTP passwords get.
> One caveat: it must be a **reversible** password (you have to recover the cleartext to send it), which is the
> normal case for outbound-credential slots.

---

## §669.1 — `BPassword` is a first-class slot type; the read API `[CERT]`

`javax.baja.security.BPassword extends BSimple` `[CERT BPassword.java:54-56]` — so it is a valid
`@NiagaraProperty` type, persisted in the component's slot map like any other value.

Reading the cleartext (for reversible passwords) `[CERT BPassword.java:434-475]`, javadoc verbatim:

> *"If the password is in a reversible format, `getValue()` retrieves the plain text String value of the
> password. … Since Niagara 4.6, reversible passwords are encrypted by module specific keys and protected by a
> permission check. To avoid permissions issues, it is recommended to wrap all calls to `getValue()` in a
> doPrivileged block like this:* `AccessController.doPrivileged((PrivilegedAction<String>)password::getValue)`*"*

```java
public String getValue() throws SecurityException {     // BPassword.java:450
  if (encoder.isReversible() || encoder instanceof BNullPasswordEncoder)
    return encoder.getValue();                           // ← plaintext for reversible
  else { byte[] val = new byte[16]; new SecureRandom().nextBytes(val);
         return Base64.getEncoder().encodeToString(val); }  // ← random junk for NON-reversible
}
```
- **Reversible → plaintext.** Non-reversible (hashed) → returns a **random 16-byte Base64 string**, never the
  original. So a token stored as a non-reversible password is unrecoverable — you MUST store a reversible one.
- `getSecretChars()` `[CERT:492]` is the security-hygiene variant returning a closeable `SecretChars`
  (try-with-resources; avoids the cleartext lingering as an interned `String`). Prefer it for a bearer token.
- Both throw `SecurityException` (subtypes `MissingEncodingKeyException`/`AccessControlException`) if the key
  is missing or the permission check fails — hence the `doPrivileged` wrap.

## §669.2 — The LIVE reference: how SMTP does it — copy this verbatim `[CERT]`

`com.tridium.email.BBasicEmailClientAuthenticator` is the exact pattern to mirror:

```java
@NiagaraProperty(name="password", type="BPassword", defaultValue="BPassword.DEFAULT")   // :29-31
public static final Property password = newProperty(0, BPassword.DEFAULT, null);        // :38
public BPassword getPassword() { return (BPassword)get(password); }                     // :51-53
...
// at authentication/send time:
AccessController.doPrivileged(BBasicEmailClientAuthenticator.this.getPassword()::getValue)  // :80
```
`[CERT BBasicEmailClientAuthenticator.java:29-31, 38, 51-56, 80]`. Note `defaultValue = "BPassword.DEFAULT"`
and the read wrapped in `AccessController.doPrivileged(...)` — precisely the javadoc-recommended shape. This is
the SMTP credential path referenced by [Block 34] §34.6.5; [Block 330] covers the authenticator family. This
block confirms B34 §34.6.5's claim ("credentials via keyring/BPassword") at the API level and shows it applies
unchanged to a bearer token.

## §669.3 — Where the secret actually lives (encryption at rest) `[CERT-remit]`

Per the `getValue()` javadoc + corpus:
- **Since Niagara 4.6**, a reversible `BPassword`'s stored form is encrypted with a **module-specific key** and
  gated by a **permission check** (`[CERT BPassword.java:439-441]`) — this is why the `doPrivileged` wrap is
  needed from your own module's protection domain.
- The module keys are wrapped by the **station keyring** — on Windows the `.km`/`.kr` files are **DPAPI**-
  wrapped (REMITTANCE [Block 5] §5.4, [Block 13] §13.2; [Block 34] §34.6.5 records the same for SMTP, and
  §34.6.5 already CORRECTED the earlier "master.jceks" assumption to `.km`/`.kr` DPAPI). The persisted `.bog`
  therefore holds only the encoded/encrypted form, never the cleartext token.
- Consequence for the PoC: the token is protected at rest exactly like the SMTP password — you get keyring
  encryption for free by choosing the `BPassword` slot type; you do not implement any crypto yourself.

## §669.4 — Making the slot reversible (the one caveat) `[CERT + INFER]`

`BPassword` decides reversibility at construction from the `PasswordEncodingContext` / encryption-key source
`[CERT BPassword.java:178-207]`: with no key source and no explicit encoding type it can't encode
(`handleConstructorMissingDecodingKey`); with a key source it uses the **default reversible encoder**
(`BReversiblePasswordEncoder.getDefaultEncodingType()`, `[CERT:205]`). In a mounted station component the
encoding context is supplied by the station, so a `BPassword` set through the Workbench property sheet (or via
`setToken(BPassword.make(cleartext))` while mounted) becomes a **reversible, keyring-encrypted** password —
the same lifecycle as the SMTP password. `[INFER — from the constructor key-source logic + the live SMTP
precedent]`

Practical rule for the PoC:
- Declare `@NiagaraProperty(name="token", type="BPassword", defaultValue="BPassword.DEFAULT")`.
- Let the operator set it in the Workbench property sheet (it renders as a masked password field), OR set it
  programmatically while the component is mounted so the station's encoding context applies.
- Read it in `sendAlarm` via `getSecretChars()`/`getValue()` inside `AccessController.doPrivileged(...)`.
- Because reading requires the module-specific key + permission, ensure the module is properly signed/loaded
  ([Block 668] §668.4) — an unsigned/side-loaded module can hit the permission check.

## §669.5 — Minimal `sendAlarm` credential snippet this block certifies

```java
@NiagaraProperty(name="token", type="BPassword", defaultValue="BPassword.DEFAULT")
// ... generated slot ...

protected boolean sendAlarm(BAlarmRecord alarm) throws Exception {
  String bearer = AccessController.doPrivileged(
      (PrivilegedAction<String>) getToken()::getValue);        // plaintext only inside the privileged block
  HttpURLConnection c = (HttpURLConnection) new URL(getUrl()).openConnection();
  c.setConnectTimeout(3000); c.setReadTimeout(3000);           // [Block 667] — keep it fast
  c.setRequestMethod("POST");
  c.setRequestProperty("Authorization", "Bearer " + bearer);
  c.setRequestProperty("Content-Type", "application/json");
  c.setDoOutput(true);
  // ... write JSON body from `alarm`, read response code ...
  int code = c.getResponseCode();
  if (code / 100 == 2) return true;      // sent
  if (code == 400 || code == 401) return false;   // non-retryable (bad token/payload)
  throw new IOException("webhook HTTP " + code);   // transient → queue+retry ([Block 666])
}
```
(Prefer `getSecretChars()` + try-with-resources over `getValue()` to avoid the cleartext lingering as a
`String` — hygiene, not correctness.)

## §669.6 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | `BPassword` is a `BSimple` slot type usable as `@NiagaraProperty` | [CERT] | BPassword.java:54-56 |
| 2 | `getValue()` returns plaintext for reversible; random junk for non-reversible | [CERT] | BPassword.java:450-465 |
| 3 | Recommended read = `AccessController.doPrivileged(pwd::getValue)`; needs module key + permission (since 4.6) | [CERT] | BPassword.java:439-443 |
| 4 | Live SMTP reference declares `type="BPassword"` and reads via `doPrivileged(getPassword()::getValue)` | [CERT] | BBasicEmailClientAuthenticator.java:29-31,80 |
| 5 | `getSecretChars()` is the closeable hygienic variant | [CERT] | BPassword.java:492 |
| 6 | Token encrypted at rest by module key + station keyring (.km/.kr DPAPI on Windows) | [CERT-remit] | BPassword.java:439-441; [B34]§34.6.5; [B5]§5.4; [B13]§13.2 |
| 7 | Must be a REVERSIBLE password (default reversible encoder when a key source is present) | [CERT + INFER] | BPassword.java:178-207,205 |
| 8 | Directly reusable for `Authorization: Bearer` — confirms B34 §34.6.5 | [CERT] | this block §669.2 |

**Tally:** 8 claims — 6 [CERT], 1 [CERT-remit] (#6), 1 [CERT+INFER] (#7). 0 unmarked.

## §669.7 — Connections

- **[Block 34] §34.6.5** — SMTP credentials via keyring/BPassword; this block confirms the API and generalizes
  it to a bearer token.
- **[Block 330]** — the email authenticator family (`BBasicEmailClientAuthenticator` and OAuth variants).
- **[Block 5] §5.4 / [Block 13] §13.2** — the station keyring (`.km`/`.kr`, DPAPI) that wraps module keys.
- **[Block 666]** — where `sendAlarm` uses this token; the return-value contract.
- **[Block 667]** — why the POST holding the token must be fast/bounded.
- **[Block 668]** — the module.xml (the `BPassword` slot registers the same way; signing gate matters for the
  permission check).
