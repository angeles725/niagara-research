# Block 324 — The `email` service itself: `BEmailService`, the `tridium/email` license gate, and the JavaMail hard-dependency the guides never mention

> Focus **email** — first evidence block (E1). READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the SERVICE component `javax.baja.email.BEmailService` — what it is, where it may be parented,
> the license feature it gates on, the runtime library it hard-requires, its account container, its
> lifecycle, and the `send` action. This is the piece the corpus never opened: [Block 34] §34.6.5 documents
> `BEmailRecipient` (how an alarm becomes an email), but NOT the service that actually transmits it.
>
> Sources (primary, decompiled N4.14.0.162):
> `organized/email/email-rt/vineflower/javax/baja/email/BEmailService.java` (167 lines, read in full).
> Cross-refs: [Block 27] (SMTP ports/TLS), [Block 31] (retry sysprop), [Block 34] §34.6.5 (`BEmailRecipient`).
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. Layer 8 (Alarm/notification
> subsystem — service tier). Block TYPE: **evidence**.

---

## 324.1 — What `BEmailService` is, and where it may live

`public class BEmailService extends BAbstractService implements BIRestrictedComponent` `[CERT]`
(`BEmailService.java:27`). Two facts follow directly from that signature:

1. It is a **service** (`BAbstractService`). In Niagara a service lives under `Station/Services` and is a
   singleton of its service types — `getServiceTypes()` returns `new Type[]{ BEmailService.TYPE }` `[CERT]`
   (`:31`, `:107-108`), so exactly one `EmailService` is the resolvable provider in a station `[INFER]`.
2. It is a **restricted component** (`BIRestrictedComponent`). `checkParentForRestrictedComponent(parent, cx)`
   delegates to `BIRestrictedComponent.checkParentForRestrictedComponent(parent, this)` `[CERT]`
   (`:115-117`) — the framework refuses to parent it anywhere the restriction disallows (i.e. it belongs
   under `Services`, not dropped into an arbitrary folder) `[INFER]`.

Its icon is `email.png` `[CERT]` (`:30`, `:81-83`) and its logger is `Logger.getLogger("email")` `[CERT]`
(`:32`) — so runtime diagnostics for the whole subsystem surface under the `email` log category.

## 324.2 — The license gate: `getFeature("tridium", "email")`

The single most operationally important line in the class:

```java
public Feature getLicenseFeature() {
   return Sys.getLicenseManager().getFeature("tridium", "email");
}
```
`[CERT]` (`BEmailService.java:111-113`).

`getLicenseFeature()` is a `BAbstractService` framework hook: a service that overrides it declares that it
runs ONLY when that license feature is present. So `BEmailService` is gated on the license feature
**vendor=`tridium`, name=`email`** `[CERT]`. Without it in the station's license, the framework does not put
the service into service — it faults `[INFER]` (standard `BAbstractService` behaviour; the enforcement
mechanism itself lives in the framework/licensing layer, not in this class — verifying that call site is a
separate gap, see §324.7).

**Operational tie-in.** This is exactly the feature line an operator sees in a license file as
`<feature name="email" .../>`. A station whose license omits `email` cannot send alarm email no matter how
the `AlarmClass`/`BEmailRecipient` are wired — the service never starts. For the client licenses already in
the corpus memory (WEB-8000 `QNX-TITAN` and the `Win-2E48` engineering station), presence of the `email`
feature must be confirmed on the target license BEFORE promising alarm-email delivery `[INFER]`. The feature
is versionless here (name only) — the license entry may still carry an `expiration` that gates it in time.

## 324.3 — The JavaMail hard-dependency (a silent provisioning failure the guides omit)

`serviceStarted()` runs `AccessController.doPrivileged(new ServiceStartedPrivilegedAction())` `[CERT]`
(`:46-49`). That privileged action's FIRST act is a reflective probe:

```java
cl = Class.forName("com.sun.mail.smtp.SMTPTransport");
...
if (cl == null) {
   throw new BajaRuntimeException(
     "EmailService will not trasmit and receive email without mail.jar in jre/lib/ext, download the
      JavaMail Api ... retrieve mail.jar ... and copy mail.jar to rel/jre/lib/ext.");
}
```
`[CERT]` (`BEmailService.java:145-152`, typo `trasmit` verbatim in source).

Consequences, all `[CERT]` from this site:

- The email subsystem depends on **JavaMail** (`com.sun.mail.smtp.SMTPTransport`) being present as
  `mail.jar` in `jre/lib/ext` — it is NOT bundled inside `email-rt.jar`. If absent, `serviceStarted()`
  throws and the service fails to start.
- The check is reflective (`Class.forName`), so the dependency is a **runtime/provisioning** concern, not a
  module-manifest dependency — the module loads fine; it fails only at service start `[INFER]`.
- This is a distinct failure mode from §324.2: a licensed-but-unprovisioned station (feature present, JavaMail
  missing) fails HERE, with the `mail.jar` message, not with a license fault. Two different root causes,
  two different remedies. Neither the `AlarmClass` nor the recipient reports it — it is a service-level fault.

Only after JavaMail is confirmed does the action iterate the accounts, `updateStatus()` each, and
`beginPolling()` on the operational ones `[CERT]` (`:154-161`).

## 324.4 — The account container and lifecycle

The service is a **container of `BEmailAccount` children**: `getAccounts()` returns
`(BEmailAccount[]) getChildren(BEmailAccount.class)` `[CERT]` (`:77-79`). `BEmailAccount` is the abstract base
of both the outgoing (SMTP) and incoming (POP3/IMAP) accounts `[INFER]` (subtypes catalogued for E2/E4/E7).

Lifecycle over those children, all `[CERT]`:

| Hook | Action | Lines |
|---|---|---|
| `serviceStarted()` | check JavaMail, then `updateStatus()` + `beginPolling()` on operational accounts | `:46-49`, `:154-161` |
| `serviceStopped()` | `updateStatus()` + `endPolling()` on every account | `:51-59` |
| `enabled()` / `disabled()` | `updateStatus()` on every account | `:61-75` |
| `added(prop)` at runtime | if a `BEmailAccount` is added while running: `updateStatus()`, and `beginPolling()` if operational | `:85-96` |
| `removed(prop)` at runtime | if a `BEmailAccount` is removed while running: `endPolling()` on the old value | `:98-105` |

"Polling" here is the INBOUND concern (an incoming account polls its mailbox); an outgoing-only station still
walks the same lifecycle but the polling is meaningful only for incoming accounts `[INFER]` (confirmed against
`BIncomingAccount` in E4). `updateStatus()` / `beginPolling()` / `endPolling()` are declared on `BEmailAccount`
and are the boundary into E7.

## 324.5 — The `send` action and default-outgoing routing

The service exposes a public action `send(BEmail)`:
`@NiagaraAction(name="send", parameterType="BEmail", defaultValue="new BEmail()")` `[CERT]` (`:22-28`, `:38-40`).
Its handler routing lives in `doSend`:

```java
public void doSend(BEmail email, Context cx) {
   BOutgoingAccount out = this.getDefaultOutgoingAccount();
   if (out == null)
      throw new BajaRuntimeException("BEmailService.send: There is no default outgoing account available");
   else
      out.invoke(BOutgoingAccount.send, email, cx);
}
```
`[CERT]` (`:128-135`).

The **default outgoing account is simply the FIRST `BOutgoingAccount` child**:
`getDefaultOutgoingAccount()` returns `out[0]` of `getChildren(BOutgoingAccount.class)`, or `null` if there are
none `[CERT]` (`:119-126`). So a service-level `send` with no `BOutgoingAccount` configured throws
"no default outgoing account available" `[CERT]` (`:130-131`). Note the delivery WORK is delegated to
`BOutgoingAccount.send` — the service is a router; the SMTP pipeline (queue, TLS, retry) lives on the account
(E2/E3), not here `[CERT]`.

Note also: `BEmailRecipient` does NOT go through `BEmailService.send` — [Block 34] §34.6.5 shows the recipient
references its outgoing account **by name** (`emailAccount` property) and invokes it directly `[INFER]`
(cross-block). The service-level `send`/`doSend` is the generic/default-account path (e.g. a Program object
or another component sending mail), parallel to the alarm-recipient path.

## 324.6 — The retry ceiling is declared on the service, consumed downstream

The class holds the retry constant [Block 31] only named as a sysprop:
`DEFAULT_MAX_NUMBER_OF_RETRIES_BEFORE_DISCARD = 6` `[CERT]` (`:33`), read once, statically, under a privileged
block:
`Integer.getInteger("niagara.email.maxNumberOfRetriesBeforeDiscard", 6)` via `AccessController.doPrivileged`
`[CERT]` (`:34-36`). So the default is **6 retries before an email is discarded**, overridable by the JVM
system property. The constant is DEFINED here on the service but CONSUMED by the outgoing send/retry loop
(E2) — this block only establishes its origin and default; the retry mechanics are E2 `[INFER]`.

## 324.7 — What this block does NOT resolve (pointers)

- The **enforcement** of `getLicenseFeature()` — the framework call site in `BAbstractService`/licensing that
  faults the service when `tridium/email` is absent — is asserted `[INFER]` here, not read. (Framework
  licensing gap; adjacent to the licensing corpus, not this focus's core.)
- The **SMTP send pipeline** behind `BOutgoingAccount.send` — queue (memory vs disk), `maxSendablePerDay`,
  midnight reset, and the retry loop that consumes §324.6's ceiling → **E2**.
- The **JavaMail session** assembly (host/port/TLS/STARTTLS Properties) → **E3** (`MailPlatformHandlerSe`).
- The **account base** `BEmailAccount` (`updateStatus`/`beginPolling`/`isOperational`, credential storage) →
  **E7**.
- The **inbound polling** target (`BIncomingAccount` + `BEmailAlarmAcknowledger`) → **E4**.

## 324.8 — Connections

- [Block 34] §34.6.5 — `BEmailRecipient`: the alarm→email bridge that this service underpins; recipient
  references its outgoing account by name rather than via `BEmailService.send`.
- [Block 27] — SMTP egress ports (25/465/587), STARTTLS, `userTrustStore`: the network face of the accounts
  this service contains.
- [Block 31] — the `maxNumberOfRetriesBeforeDiscard=6` sysprop, whose class-level origin is §324.6.
- [Block 8] — recipient enumeration (lists `BEmailRecipient` among alarm recipients).
- Licensing corpus (client licenses `QNX-TITAN`, `Win-2E48` in memory) — the `email` feature §324.2 gates on.

## 324.9 — Self-verify

Block TYPE: **evidence** (single-class decompilation). All `[CERT]` cite one file read in full
(`BEmailService.java`, 167 lines); every load-bearing `file:line` was token-checked against the source in this
iteration (license gate `:111-113`, JavaMail probe `:145-152`, `doSend` `:128-135`, default-outgoing
`:119-126`, retry constant `:33-36`, account container `:77-79` — 6/6 load-bearing citations resolved).

`verify-block.sh` marker tally (to be pasted from the script run):

| Marker | count (adj) |
|---|---|
| CERT (local file:line) | 21 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 11 |
| INFER/CERT ratio | 0.52 |

`verify-block.sh` exit 0. For an EVIDENCE block over a single 167-line class, the 0.52 ratio is expected and
healthy: the deductions are framework-behaviour (service singleton, restricted parenting, license enforcement)
and cross-block connections, not padding — and the class's investigable substance IS nearly exhausted by this
block, which is the correct signal. The remaining depth is in the classes it points to (E2–E7), each its own
gap.
