# Block 334 — SYNTHESIS of the `email` focus (B324–B333): the service that actually sends, gated three ways, insecure by default and secure by wizard, with a spoofable inbound door nobody grades

> Focus **email** — closing synthesis. 10 evidence blocks, 10 gaps, one prioritized backlog exhausted.
> This block consolidates the cross-cutting threads and **remits** to the block that established each finding;
> it re-derives nothing. READ-ONLY. Corpus language: ENGLISH.
>
> Scope of the focus: the `email` module as a **service subsystem** — the SMTP engine that transmits
> (`BEmailService` + `BOutgoingAccount` + JavaMail session), its license gate, the inbound POP3/IMAP + alarm-ack
> path, OAuth2, the security dashboard, the account/auth base, and the wb/hx/ux UIs. NOT the alarm→email BRIDGE
> `BEmailRecipient`, which [Block 34] §34.6.5 already closed (REMITTANCE).
>
> Markers: this is a SYNTHESIS block — every `[CERT]` here is a **remission** to a block that verified it
> (citation `[Block N] §N.x`, not a fresh `file:line`); `[INFER]` marks connections drawn ACROSS blocks.
> Layer 8 (Alarm/notification — email subsystem). Consolidates [Block 324]–[Block 333].

---

## 334.1 — What the focus covered

| Area | Block |
|---|---|
| Service, license gate, JavaMail dependency, account container | [324] |
| Outgoing send pipeline: dual queue, 100/day cap, retry | [325] |
| TLS session (`MailPlatformHandlerSe`), SSL/STARTTLS, min-TLS | [326] |
| Inbound POP3/IMAP + `BEmailAlarmAcknowledger` | [327] |
| OAuth2 XOAUTH2 (client-secret / client-cert) | [328] |
| Security dashboard (22 posture filters) | [329] |
| Account base + authenticator hierarchy | [330] |
| Workbench UI | [331] |
| Address converters | [332] |
| Browser (bajaux) UI | [333] |

Bootstrapped 2026-08-04 on a measured surface of **61 distinct classes** (rt 43 / ux 11 / wb 7), audit-first.

## 334.2 — Thread 1: the service is gated THREE ways before a byte leaves

Sending is not one switch. `BEmailService` runs only if the license carries the feature
`getFeature("tridium","email")` [Block 324] §324.2; the service START then hard-fails if JavaMail (`mail.jar`) is
absent from `jre/lib/ext` [Block 324] §324.3; and `send`/`doSend` finally needs a default `BOutgoingAccount` or
throws "no default outgoing account" [Block 324] §324.5. **Three independent failure modes — license, provisioning,
configuration — each with a different symptom and remedy** `[INFER]`. The license and provisioning faults are the
two the corpus never had before this focus.

## 334.3 — Thread 2: insecure by default, secure by wizard, graded by dashboard

The component defaults are the unsafe ones — `useSsl=false`/`useStartTls=false` → cleartext SMTP on port 25
[Block 326] §326.5, `persistent=false` → the outbound queue is LOST on restart [Block 325] §325.2, and the base
auth is Basic. But two mechanisms compensate `[INFER]`:
- the Workbench Manager's **New** applies SSL + OAuth + submission ports on top of those defaults [Block 331]
  §331.2 — secure-by-wizard;
- the **security dashboard** grades the ACTUAL state with 22 filters, severity gated on `enabled`, turning
  cleartext/debug/weak-TLS/basic-auth into ALERT/WARNING [Block 329] §329.3 — graded-after-the-fact.
So an account created by the wizard starts safe; one dropped from a palette inherits the insecure defaults and is
caught only by the dashboard `[INFER]`. This is the SAME "the factory default is wrong" pattern the Modbus focus
closed on ([Block 315]) — `persistent=false` is email's `criticalData=false` `[INFER]`.

## 334.4 — Thread 3: two directions, symmetric plumbing, ASYMMETRIC trust

Outbound and inbound share the account base, the poller, and the JavaMail session builder ([Block 330],
[Block 326]). But the TRUST model is lopsided `[INFER]`:
- Outbound authenticates the CLIENT to the server strongly — Basic over TLS, or OAuth `client_credentials`
  XOAUTH2 [Block 328] §328.1.
- Inbound `BEmailAlarmAcknowledger` acks an alarm on an email whose sender is "authenticated" by **`From:`
  address string-equality only** — forgeable, no SPF/DKIM/token [Block 327] §327.6. The authorization is correct
  (enabled user + `hasAdminWrite`), but keyed to a spoofable identity.
And the security dashboard grades the outbound/transport posture but has **no item** for the inbound-ack exposure
[Block 329] §329.5. **The one genuinely dangerous default in the subsystem is the one the dashboard cannot see**
`[INFER]` — the focus's sharpest finding.

## 334.5 — Thread 4: the authentication timeline

Three generations coexist `[INFER]`: the DEPRECATED `account`/`password` fields (which `fw(x=11)` **discards,
not migrates**, on upgrade — a silent credential loss [Block 330] §330.3); `BBasicEmailClientAuthenticator`
(username/password, `mail.smtp.auth=true`, credential read behind a `GET_EMAIL_AUTHENTICATOR` permission
[Block 330] §330.5); and OAuth2 XOAUTH2 for Microsoft 365 / Google, `client_credentials` by secret or certificate
[Block 328]. The dashboard encodes the hierarchy as verdicts: OAuth=OK, Basic=WARNING/ALERT, NoAuth=WARNING
[Block 329] §329.3.

## 334.6 — Thread 5: two small quirks worth remembering

- **Retry off-by-one** — `maxNumberOfRetriesBeforeDiscard=6` yields ≤6 attempts on the disk queue (`>=`) but ≤7
  on the memory queue (`>`) [Block 325] §325.4. Same knob, different effective ceiling by `persistent`.
- **No token cache** — the OAuth path fetches a fresh access token on EVERY send batch (metadata cached, token
  not) [Block 328] §328.3 — extra token-endpoint load per drain.

## 334.7 — Thread 6: three UIs, none can test-send

Email is edited through Workbench Swing, legacy Hx HTML, and bajaux ux (over `webEditors`) — parallel stacks
[Block 331], [Block 333]. **None exposes a "send test email" affordance** [Block 331] §331.1, [Block 333] §333.4.
Commissioning an SMTP config can only be validated by triggering a real alarm through `BEmailRecipient`
[Block 34] §34.6.5 or invoking `BEmailService.send` from a Program [Block 324] §324.5 — a real field-commissioning
gap `[INFER]`.

## 334.8 — Closing the question that started the focus

The focus began from "can Workbench send alarm emails, and how?" The answer, now fully grounded:
**Yes, natively, no code** — `AlarmClass` routes to a `BEmailRecipient` [Block 34] §34.6.5, which sends through a
`BOutgoingAccount` [Block 325] under the licensed `BEmailService` [Block 324]. The real blockers are NOT
programming — they are (1) the `email` license feature, (2) JavaMail provisioning, (3) the insecure defaults, and
(4) no way to test-send from the UI `[INFER]`. A Program object or custom module (the alternatives raised at the
outset) is unnecessary for alarm email and only justified for logic the recipient cannot express.

## 334.9 — Connections & what remains

- [Block 34] §34.6.5 — the alarm→email recipient this subsystem carries (REMITTANCE, not re-opened).
- [Block 27] — SMTP ports / trust store, now grounded in the account-side TLS ([Block 326]).
- [Block 31] — the retry sysprop, whose class origin and off-by-one are [Block 324] §324.6 / [Block 325] §325.4.
- [Block 315] — the Modbus "wrong defaults" pattern this focus echoes.
- px-tail (`webEditors`) — the un-opened framework email-ux consumes ([Block 333] §333.2); a cross-focus pointer,
  NOT re-opened here.
- **Open child gap (named, not closed)**: the inbound-ack spoofing exposure ([Block 327] §327.6) is a candidate
  for a DYNAMIC (§12) validation against a live station — mark as `email-G1` (requires-execution / live-mailbox),
  out of scope for the static focus.

## 334.10 — Self-verify

Block TYPE: **synthesis** (remissions, no fresh `file:line` — a high `[INFER]` ratio is EXPECTED and correct for
this type per §11). Every `[CERT]`-remission points to a block that carries the verified `file:line`; the
`[INFER]`s are cross-block threads this synthesis draws. Coverage: 10/10 gaps closed (B324–B333);
`BEmailRecipient` REMITTANCE to [Block 34]; one named child gap (`email-G1`, requires-execution) deferred.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (remissions) | 1 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 11 |
| INFER/CERT ratio | 11.0 (EXPECTED for synthesis — remissions carry the verified citations, §11) |

`verify-block.sh` exit 0. The "zero file:line" WARN is BY DESIGN for a synthesis block: it cites `[Block N] §N.x`
remissions, not fresh `file:line` — each remitted block carries the token-checked citation.
