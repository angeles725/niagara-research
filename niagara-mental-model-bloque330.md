# Block 330 — The account base and the authenticator hierarchy: an account is "operational" iff not-disabled-and-not-fault, auth is a pluggable type-config, and the `fw(x=11)` upgrade DISCARDS legacy credentials rather than migrating them

> Focus **email** — evidence block E7. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the parts of `javax.baja.email.BEmailAccount` not yet covered (status/operational model, the
> `emailAuthenticator` type-config, the deprecated-credential migration) plus the authenticator hierarchy:
> `BEmailClientAuthenticator` (base), `BBasicEmailClientAuthenticator`, `BNoAuthEmailClientAuthenticator`, and
> the `BEmailAuthenticatorTypeConfig` machinery that gates which authenticators Workbench offers.
>
> Sources (primary, decompiled N4.14.0.162), sweep sonnet + driver re-verification of the migration + guard:
> root `organized/email/email-rt/vineflower/`, files `javax/baja/email/BEmailAccount.java` (428),
> `.../BEmailClientAuthenticator.java` (53), `com/tridium/email/BBasicEmailClientAuthenticator.java` (107),
> `.../BNoAuthEmailClientAuthenticator.java` (40), `javax/baja/email/BEmailAuthenticatorTypeConfig.java` (93).
>
> ALREADY COVERED (cross-ref only): poller/`poll()` + TLS props + `changed()` → [Block 326]; send queue/retry →
> [Block 325]; OAuth authenticators → [Block 328]; `BPassword`/keyring → [Block 34] §34.6.5.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. Layer 8 (notification — account/auth
> tier) + Layer 22 (credentials). Block TYPE: **evidence**.

---

## 330.1 — The operational model: status drives polling

An account is **operational iff its status is neither disabled nor fault** `[CERT]`
(`BEmailAccount.java:295-298`):

```java
public boolean isOperational() {
   BStatus status = this.getStatus();
   return !status.isDisabled() && !status.isFault();
}
```

`updateStatus()` recomputes the status bits from the account's `enabled` flag and the SERVICE's state — disabled
when `!getEnabled() || service.isDisabled()`, fault mirrored from `service.isFault()` — and when the
`isOperational()` result FLIPS, it calls `beginPolling()` (became operational) or `endPolling()` (stopped)
`[CERT]` (`BEmailAccount.java:300-328`). `beginPolling()` refuses on a fatal-fault service and otherwise starts
one `Poller` thread if none exists; `endPolling()` kills it `[CERT]` (`BEmailAccount.java:338-354`). `started()`
wires the `mailPlatformHandler` then `updateStatus()` `[CERT]` (`:356-360`). So the account's whole liveness is
derived, not directly set — it is a function of (its own `enabled`) AND (the EmailService state) [Block 324]
§324.4 `[INFER]`.

## 330.2 — Authentication is a pluggable type-config

The account holds its authenticator as a `BEmailAuthenticatorTypeConfig` property, HIDDEN (flags=4) with a
`security` facet `[CERT]` (`BEmailAccount.java:107`, accessor `:143-145`):

```java
public static final Property emailAuthenticator =
   newProperty(4, new BEmailAuthenticatorTypeConfig(), BFacets.make("security", BBoolean.TRUE));
```

The send/poll paths resolve the concrete authenticator with `getEmailAuthenticator().make()` — the `BTypeConfig`
pattern that instantiates whichever type (Basic/OAuth/NoAuth) is selected [Block 325] §325.5, [Block 326] §326.1
`[CERT]`. One call site, polymorphic authenticator `[INFER]`.

## 330.3 — The `fw(x=11)` migration DISCARDS the legacy credentials

`BEmailAccount` carries two `@Deprecated` credential properties from before the pluggable authenticator:
`account` (String username, default `""`) and `password` (`BPassword`, default `BPassword.DEFAULT`) `[CERT]`
(`BEmailAccount.java` field decls, `:105-121`). The framework-upgrade hook migrates them at level 11 `[CERT]`
(`:255-262`):

```java
public final Object fw(int x, ...) {
   if (x == 11) { this.migrateDeprecatedAuthProperties(); this.fwStarted(x, ...); }
   return super.fw(x, ...);
}
void migrateDeprecatedAuthProperties() {
   if (!Flags.isUserDefined1(this, emailAuthenticator)) {
      this.set(account,  account.getDefaultValue());   // → "" (reset)
      this.set(password, BPassword.DEFAULT);           // → default (reset)
      this.setFlags(account,  getFlags(account)  | 4 | 1);   // hidden + readonly
      this.setFlags(password, getFlags(password) | 4 | 1);   // hidden + readonly
      this.setFlags(emailAuthenticator, getFlags(emailAuthenticator) & -5);          // un-hide
      this.setFlags(emailAuthenticator, getFlags(emailAuthenticator) | 268435456);   // mark user-defined-1
   }
}
```
`[CERT]` (`BEmailAccount.java:267-276`).

**The finding.** This does **NOT construct a `BBasicEmailClientAuthenticator` from the old `account`/`password`**
— it RESETS both to their defaults (empty username, default password), hides+readonly-locks them, un-hides the
new `emailAuthenticator`, and marks it user-defined-1 so the migration is idempotent `[CERT]`. The guard
`!Flags.isUserDefined1(emailAuthenticator)` means it fires only on a config that never set an authenticator —
exactly a pre-authenticator (legacy) station `[CERT]`. Consequence: **on upgrade to fw≥11, a station that
authenticated SMTP via the deprecated `account`/`password` fields loses those credentials silently** — the new
`emailAuthenticator` defaults to a Basic type-config with an EMPTY username/password, so an admin must
re-enter the credentials post-upgrade `[INFER]` (operational consequence of the reset). There is no
`useAuthentication` boolean in this class — verified absent across 428 lines `[CERT]`.

## 330.4 — The authenticator base contract

`BEmailClientAuthenticator` declares four ABSTRACT methods and one concrete default `[CERT]`
(`BEmailClientAuthenticator.java:26-36`):

| Method | Kind |
|---|---|
| `setOutgoingAuthenticationProperties(Properties)` | abstract |
| `setIncomingAuthenticationProperties(Properties, String type)` | abstract |
| `connectOutgoingSession(Transport)` | abstract |
| `connectIncomingSession(Store, String type)` | abstract |
| `getAuthenticator()` | concrete — returns **`null`** by default `[CERT]` (`:30-32`) |

So a subclass overrides `getAuthenticator()` only when it supplies credentials via a JavaMail `Authenticator`
(Basic does; OAuth passes a token directly instead [Block 328] §328.2) `[INFER]`.

## 330.5 — Basic auth: `mail.smtp.auth=true`, a `PasswordAuthentication`, and a permission-gated credential read

`BBasicEmailClientAuthenticator` implements BOTH the outgoing and incoming markers `[CERT]`
(`BBasicEmailClientAuthenticator.java:35-36`) and holds its own `account`/`password` (`BPassword`) `[CERT]`
(`:37-38`). It sets the JavaMail auth flag `mail.smtp.auth=true` (outgoing) / `mail.<store>.auth=true` (incoming)
`[CERT]` (`:65-72`), and supplies credentials through a `PasswordAuthentication` `[CERT]` (`:75-84`):

```java
public Authenticator getAuthenticator() {
   checkAuthenticatorAccess();                                   // permission gate
   return new Authenticator() {
      public PasswordAuthentication getPasswordAuthentication() {
         return new PasswordAuthentication(getAccount(),
            AccessController.doPrivileged(getPassword()::getValue));   // privileged BPassword read
      }
   };
}
```

`connectOutgoingSession(Transport)` here calls `transport.connect()` with **no arguments** `[CERT]` (`:86-89`) —
it relies on the JavaMail `Session`'s `Authenticator` (the one above) to supply user/pass on demand. This is the
structural CONTRAST with OAuth, which passes the token explicitly as `connect(account, token)` [Block 328]
§328.2 `[INFER]`.

**Permission guard.** `checkAuthenticatorAccess()` calls `SecurityManager.checkPermission(new
NiagaraBasicPermission("GET_EMAIL_AUTHENTICATOR"))` `[CERT]` (`:40`, `:101-106`). So extracting the live
credential (the only path that decrypts the `BPassword`) requires the `GET_EMAIL_AUTHENTICATOR` permission — a
caller without it is denied by the SecurityManager. The `BPassword` decrypt itself runs under `doPrivileged`, so
the privileged read happens regardless of the caller's own access context once the permission check passes
`[CERT]` (SECRETS DISCIPLINE: structure only — `BPassword`/`PasswordAuthentication`, no values).

## 330.6 — NoAuth: outgoing-only, incoming throws

`BNoAuthEmailClientAuthenticator` implements ONLY the outgoing marker `[CERT]`
(`BNoAuthEmailClientAuthenticator.java`). `setOutgoingAuthenticationProperties` is a no-op and
`connectOutgoingSession` calls bare `transport.connect()` (anonymous relay) `[CERT]` (`:33`). BOTH incoming
methods throw `UnsupportedOperationException("NoAuthEmailClientAuthentication cannot be used for
BIncomingAccounts")` `[CERT]` (`:28`, `:38`) — a mailbox you read FROM always needs auth `[INFER]`.

## 330.7 — TypeConfig: which authenticators each direction offers

`BEmailAuthenticatorTypeConfig` defaults its type-spec to `BBasicEmailClientAuthenticator.TYPE` `[CERT]`
(`BEmailAuthenticatorTypeConfig.java:25-26`) and validates a chosen type against the account's
`getTargetEmailAuthenticatorType()`, rejecting mismatches `[CERT]` (`:85-92`). The two direction-specific
subclasses narrow the Workbench dropdown by `getTargetType()` `[CERT]`:

| Config | Target marker | Offered authenticators |
|---|---|---|
| `BOutgoingAccountClientAuthenticatorTypeConfig` | `BIOutgoingAccountClientAuthenticator` | Basic + **NoAuth** + OAuth (secret/cert) |
| `BIncomingAccountClientAuthenticatorTypeConfig` | `BIIncomingAccountClientAuthenticator` | Basic + OAuth — **no NoAuth** (§330.6) |

`[CERT]` (the marker interfaces §330.5-330.6 implement). A type-config is restricted to live inside a
`BEmailAccount`, one per parent `[CERT]` (`:39-51`).

## 330.8 — Connections

- [Block 324] §324.4 — the service whose state `updateStatus()` reads; [Block 326] §326.5 — the `Poller` that
  §330.1's `beginPolling` starts.
- [Block 325] §325.5 / [Block 326] §326.1 — the `getEmailAuthenticator().make()` call sites §330.2 feeds.
- [Block 328] §328.2 — OAuth's `connect(account, token)`, contrasted with Basic's bare `connect()` (§330.5).
- [Block 329] §329.3 — the dashboard grades the authenticator TYPE this block enumerates (Basic=WARNING/ALERT,
  OAuth=OK, NoAuth=WARNING).
- [Block 34] §34.6.5 — the `BPassword`/keyring credential path §330.5 reads under `doPrivileged`.

## 330.9 — Self-verify

Block TYPE: **evidence**. Delegated sweep **sonnet** (7 files, 763 lines); the driver re-verified the two
consequential claims directly against source: the `fw(x=11)` migration RESET (`BEmailAccount.java:255-276`, read
verbatim — confirmed it sets defaults, not a constructed authenticator) and the `GET_EMAIL_AUTHENTICATOR`
permission gate (`BBasicEmailClientAuthenticator.java:75-106`, read verbatim), plus `isOperational` `:295-298`
and NoAuth's incoming throw `:28/:38`. Load-bearing anchors (extern — token-checked by read):
`BEmailAccount.java:267`, `BEmailAccount.java:295`, `BBasicEmailClientAuthenticator.java:65`,
`BBasicEmailClientAuthenticator.java:101`, `BNoAuthEmailClientAuthenticator.java:28`,
`BEmailAuthenticatorTypeConfig.java:25`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 30 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 7 |
| INFER/CERT ratio | 0.23 |

`verify-block.sh` exit 0 (citations extern — token-checked by read).

Evidence block: `[INFER]`s are the operational-consequence of the credential reset, the Basic-vs-OAuth connect
contrast, and derived-liveness reasoning — each anchored to a cited `[CERT]` behavior.
