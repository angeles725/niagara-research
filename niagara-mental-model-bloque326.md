# Block 326 — The TLS session: `MailPlatformHandlerSe` builds JavaMail Properties through Niagara's own cert manager, SSL and STARTTLS are mutually exclusive, and transport security is OFF by default

> Focus **email** — evidence block E3. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: where the JavaMail `Session`/`Properties` are assembled for both directions —
> `com.tridium.email.se.MailPlatformHandlerSe.getOutgoingSession` / `getIncomingSession` — plus the three TLS
> properties on `BEmailAccount` (`useSsl`, `useStartTls`, `tlsMinProtocol`) and the `BSslTlsEnum` ordinals they
> resolve to. This is the `getOutgoingSession(this)` hand-off that [Block 325] §325.5 deferred.
>
> Sources (primary, decompiled N4.14.0.162), all read inline:
> `organized/email/email-rt/vineflower/com/tridium/email/se/MailPlatformHandlerSe.java` (118 lines),
> `.../javax/baja/email/BEmailAccount.java` (TLS props + `changed`),
> `.../javax/baja/security/crypto/BSslTlsEnum.java` (ordinals).
>
> EXCLUDES (other gaps): the authenticator that fills `setOutgoingAuthenticationProperties` / `getAuthenticator`
> → **E7/E5**; the queue/retry that calls this → [Block 325]; POP3/IMAP consumption → **E4**.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. Layer 8 (Alarm/notification
> subsystem — transport-security tier). Block TYPE: **evidence**.

---

## 326.1 — One handler, two symmetric methods

`MailPlatformHandlerSe extends MailPlatformHandler` is the platform (`-se`, Standard Edition) implementation
that turns an account into a JavaMail `Session` `[CERT]` (`MailPlatformHandlerSe.java:21`). It has exactly two
public methods, near-mirror images:

- `getOutgoingSession(BOutgoingAccount)` `[CERT]` (`:67-113`) — builds `mail.smtp.*` properties.
- `getIncomingSession(BIncomingAccount, String type)` `[CERT]` (`:22-65`) — builds `mail.<type>.*` where
  `type` is `pop3` or `imap` (E4).

Both end in `Session.getInstance(props, authenticator)` under `AccessController.doPrivileged` `[CERT]`
(`:56-58`, `:105-106`). The authenticator and its `set*AuthenticationProperties(props)` come from
`acct.getEmailAuthenticator().make()` — that is the E7/E5 boundary; this block documents everything EXCEPT what
the authenticator injects `[CERT]` (`:69`, `:73`, `:24`, `:26`).

## 326.2 — The base (non-TLS) SMTP properties

For outgoing, before any TLS branch, these are always set `[CERT]` (`:78-86`):

| JavaMail property | Value | Cite |
|---|---|---|
| `mail.smtp.localhost` | `System.getProperty("mail.smtp.localhost", Sys.getHostName())` (privileged) | `:78-80` |
| `mail.smtp.host` | `acct.getHostname()` | `:81` |
| `mail.smtp.port` | `acct.getPort()` (default 25 — [Block 325] §325.6) | `:82` |
| `mail.smtp.socketFactory.port` | same port | `:83` |
| `mail.smtp.socketFactory.fallback` | `"true"` (⚠ see §326.3) | `:84` |
| `mail.smtp.connectiontimeout` / `mail.smtp.timeout` | `connectionTimeout` millis (default 10 s) — BOTH set from ONE property | `:85-86` |

So the account's single `connectionTimeout` drives both the JavaMail connect timeout AND the read timeout
`[CERT]` (`:70`, `:85-86`). `mail.smtp.localhost` (the EHLO name) defaults to the station host name unless the
JVM overrides it `[CERT]` (`:78-80`).

## 326.3 — The TLS branch: through Niagara's cert manager, not raw JSSE

The TLS block runs **only if `useSsl || useStartTls`** `[CERT]` (`:87`). Inside it, all `[CERT]` (`:87-103`):

1. `useSsl` → `mail.smtp.ssl.enable=true`; `useStartTls` → `mail.smtp.starttls.enable=true` (`:88-94`). These
   are set independently here, but the two flags are mutually exclusive upstream (§326.4).
2. **`mail.smtp.socketFactory.fallback` is flipped to `"false"`** (`:96`). This is the security-load-bearing
   line: once TLS is requested, JavaMail is forbidden from silently falling back to a plaintext socket if the
   TLS socket fails. In the non-TLS path it stays `"true"` (`:84`) `[INFER]` (the contrast is the point).
3. The socket factory is obtained from **Niagara's own crypto stack**, not the default JSSE:
   `CertManagerFactory.getInstance()` → `ICryptoManager.getClientSocketFactory(new ClientTlsParameters(acct.getTlsMinProtocol().getTag()))` (`:97-99`). So the SMTP/POP3/IMAP TLS peer is validated against the station's
   **`userTrustStore`** [Block 27], and the client cert / protocol policy is Niagara-managed `[INFER]`
   (cross-block — [Block 27] established `userTrustStore` validates SMTP peers).
4. `mail.smtp.ssl.socketFactory` = that factory; `mail.smtp.ssl.socketFactory.port` = the port (`:100-101`).
5. `mail.smtp.ssl.protocols` = `getConfiguredTlsMinimumProtocolVersions(acct)` (`:102`), which is
   `String.join(" ", CryptoSupport.TYPE_LISTS.get(acct.getTlsMinProtocol().getTag()))` `[CERT]` (`:115-116`) —
   i.e. the ENABLED protocol list derived from the configured MINIMUM, not just the single minimum `[INFER]`.

The incoming method is identical in shape (`mail.<type>.ssl.enable` / `.starttls.enable` / same cert-manager
factory), plus a POP3-only `mail.pop3.rsetbeforequit` keyed on `deliveryPolicy != delete` `[CERT]` (`:36-53`).

## 326.4 — SSL and STARTTLS are mutually exclusive (enforced live)

The three TLS properties live on the base `BEmailAccount`, all flagged `security` `[CERT]`
(`BEmailAccount.java:119-121`):

| Property | Type | Default |
|---|---|---|
| `useSsl` | boolean | **false** |
| `useStartTls` | boolean | **false** |
| `tlsMinProtocol` | `BSslTlsEnum` | **`tlsv1_2`** |

`changed()` enforces mutual exclusion at runtime `[CERT]` (`:362-378`): when running and `cx != doNotChange`,
setting `useSsl=true` forces `useStartTls=false`, and setting `useStartTls=true` forces `useSsl=false` (each via
`set(..., doNotChange)` to avoid re-entrancy). So an account is **implicit-SSL (465) XOR STARTTLS (587), never
both** `[CERT]`. This is why [Block 27]'s ports 465 and 587 are two account configurations of ONE `smtp`
transport [Block 325] §325.6, not two transports `[INFER]` (cross-block).

## 326.5 — The security defaults: cleartext, and the bit-flag ordinal that gates the dashboard

**Both TLS flags default to `false`** `[CERT]` (`:119-120`). So a freshly-added `BOutgoingAccount` with only
host+port set sends **cleartext SMTP on port 25** — no encryption, no auth necessarily `[INFER]`. Transport
security is opt-in. This is the state the email security-dashboard agent (E6) flags as `ALERT` on a live
account.

`tlsMinProtocol` resolves through `BSslTlsEnum`, whose ordinals are **bit flags**, not a dense sequence
`[CERT]` (`BSslTlsEnum.java:19-29`):

| Tag | Ordinal |
|---|---|
| `tlsv1` | 1 |
| `tlsv1_1` | 2 |
| `tlsv1_2` | 4 |
| `tlsv1_3` | 8 |

The account default is `tlsv1_2` = **ordinal 4** `[CERT]` (`BEmailAccount.java:121`) — note this differs from
the enum's OWN `DEFAULT = tlsv1` (ordinal 1) `[CERT]` (`BSslTlsEnum.java:30`); the account raises the floor to
1.2. This resolves the E1 audit's open question about the dashboard's "`tlsMinProtocol` ordinal < 4 → ALERT"
threshold (E6): ordinal < 4 is exactly `tlsv1` (1) or `tlsv1_1` (2) — anything below TLS 1.2 `[INFER]`. The
bit-flag encoding (1/2/4/8) is why the comparison is an ordinal threshold rather than an enum-position check.

## 326.6 — The debug flag: a plaintext leak the dashboard watches

If `acct.getDebug()` is true, `mail.debug=true` is set and `session.setDebug(true)` routes JavaMail's protocol
trace to `System.out` (outgoing) / `System.err` with a `"*** type = "` banner (incoming) `[CERT]`
(`:74-76`, `:107-110`, `:27-30`, `:59-62`). On a live account this dumps the SMTP conversation — a reason the
security dashboard (E6) flags `debug` on a live account as `ALERT` `[INFER]` (cross-gap, confirmed by the E1
audit's posture matrix).

## 326.7 — What this block does NOT resolve

- `setOutgoingAuthenticationProperties(props)` / `getAuthenticator()` — the AUTH properties injected into the
  same `props` map (basic vs OAuth) → **E7** (base authenticators) and **E5** (OAuth/XOAUTH2).
- `CertManagerFactory` / `ICryptoManager` internals (the crypto module) — out of the email focus.
- POP3/IMAP `getIncomingSession` consumption and `deliveryPolicy` → **E4**.

## 326.8 — Connections

- [Block 325] §325.5 — `pollQueue` calls `getOutgoingSession(this)`; this block IS that method.
- [Block 27] — `userTrustStore` validates SMTP peers; §326.3 shows the socket factory comes from
  `CertManagerFactory`, tying email TLS to that trust store.
- [Block 324] — the service the accounts hang under.
- E6 (security dashboard) — §326.5 (ordinal-4 threshold) and §326.6 (debug) are the posture items it evaluates.

## 326.9 — Self-verify

Block TYPE: **evidence**. Inline read (constraint: focused 3-file read — `MailPlatformHandlerSe` 118 lines +
targeted `BEmailAccount`/`BSslTlsEnum` sections — no sub-agent needed). Load-bearing citations token-checked
against source this iteration: TLS branch `:87-103`, cert-manager factory `:97-99`, `socketFactory.fallback`
flip `:96`, mutual exclusion `BEmailAccount.java:362-378`, TLS defaults `:119-121`, `BSslTlsEnum` ordinals
`:19-29` — 6/6 resolved verbatim.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (local file:line) | 20 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 8 |
| INFER/CERT ratio | 0.40 |

`verify-block.sh` exit 0.

Evidence block: `[INFER]`s are the security contrasts (fallback true→false, cleartext default, ordinal
threshold) and cross-block trust-store/port links — not padding.
