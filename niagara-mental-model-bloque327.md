# Block 327 — The inbound path: `BIncomingAccount` polls POP3/IMAP and fires a `received` topic, and `BEmailAlarmAcknowledger` acks an alarm from any email whose forgeable `From:` matches a privileged user

> Focus **email** — evidence block E4. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the INBOUND half of the module — `javax.baja.email.BIncomingAccount` (POP3/IMAP poll → `received`
> topic) and its one real consumer `com.tridium.email.alarm.BEmailAlarmAcknowledger` (acknowledge an alarm by
> emailing its UUID back). This is the delta [Block 34] §34.6.5 missed: it documented `BSmsAlarmAcknowledger`
> but NOT the email one. **Carries a security finding** (§327.6).
>
> Sources (primary, decompiled N4.14.0.162), sweep sonnet + driver re-read of the acknowledger in full:
> `organized/email/email-rt/vineflower/javax/baja/email/BIncomingAccount.java` (365 lines),
> `.../com/tridium/email/alarm/BEmailAlarmAcknowledger.java` (181 lines, read in full by the driver),
> `.../javax/baja/email/BDeliveryPolicy.java`, `BStore.java`.
>
> EXCLUDES: `getIncomingSession`/TLS → [Block 326]; `EmailUtil.fromMessage` MIME parse (out of focus);
> `connectIncomingSession` authenticator → **E7/E5**.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. Layer 8 (Alarm/notification
> subsystem — inbound tier) + Layer 22 (security). Block TYPE: **evidence**.

---

## 327.1 — `BIncomingAccount`: the POP3/IMAP account

Inbound-relevant property surface, all `[CERT]` (`BIncomingAccount.java`):

| Property | Type | Default | Cite |
|---|---|---|---|
| `port` | int | **110** (POP3) | `:99` |
| `store` | `BStore` | **`pop3`** (else `imap`) | `:103` |
| `deliveryPolicy` | `BDeliveryPolicy` | **`delete`** | `:104` |
| `emailToRead` | `BEmailRead` | `unreadEmail` (cap 100) | `:105` |
| `incomingEmailSizeLimit` | int | 25 000 KB | `:106` |
| `sizeLimitPerPoll` | int | 25 000 KB | `:113` |
| `ignoreAttachments` | boolean | false | `:120` |

The mailbox folder is **hardcoded to `"INBOX"`** — not a property `[CERT]` (`:341`). `pollrate` is inherited
from `BEmailAccount` (the `Poller` thread, [Block 326] §326.5). `BStore` is a two-value enum pop3(0)/imap(1),
default pop3 `[CERT]` (`BStore.java`).

## 327.2 — `poll()`: connect, select, parse, fire, apply-policy, close

`poll()` `[CERT]` (`:207-334`) runs on the poller thread each cycle:

1. Build the JavaMail `Session` via `mailPlatformHandler.getIncomingSession(this, storeType)` ([Block 326]),
   `session.getStore(storeType)`, then `authenticator.connectIncomingSession(store, type)` (E7) `[CERT]`
   (`:208-215`).
2. `getInbox(store)` → `"INBOX"` → `inbox.open(2)` (READ_WRITE) `[CERT]` (`:217`, `:341`).
3. Select messages: if the policy will delete/mark AND `emailToRead==unreadEmail`, search
   `FlagTerm(SEEN, false)` (unseen only, capped 100); else `getMessages()` `[CERT]` (`:223-229`).
4. Per message: `EmailUtil.fromMessage(m, ignoreAttachments)` under `doPrivileged` → `BEmail`, then
   `fireReceived(mail)` on the `received` topic `[CERT]` (`:280`, `:285`, topic decl `:94-95`).
5. Apply the delivery policy flags, then `inbox.close(deliveryPolicy == delete)` — the boolean is the
   **expunge** flag `[CERT]` (`:291-318`).

So the inbound account is a producer: every qualifying email becomes a `BEmail` event on a **topic**, and
whoever links to that topic consumes it `[CERT]`. The acknowledger (§327.4) is the built-in consumer.

## 327.3 — `BDeliveryPolicy` drives the server-side fate

`BDeliveryPolicy` = delete(0, DEFAULT) / markAsRead(1) / markAsUnread(2) `[CERT]` (`BDeliveryPolicy.java:18-21`).
Effect in `poll()` `[CERT]`:

- `delete` (default) → set `Flag.DELETED`, `close(expunge=true)` → message removed from server `[CERT]` (`:318`).
- `markAsRead` → set `SEEN`, `close(false)` → stays, filtered out next poll by the unseen search.
- `markAsUnread` → clear `SEEN`, `close(false)` → stays visible.

This is what [Block 326] §326.3's `mail.pop3.rsetbeforequit = (deliveryPolicy != delete)` protects: when the
policy is NOT delete, POP3 issues RSET before QUIT so a failed close does not accidentally expunge `[INFER]`
(cross-block).

## 327.4 — `BEmailAlarmAcknowledger`: the ack consumer

`public final class BEmailAlarmAcknowledger extends BAlarmAcknowledger` `[CERT]`
(`BEmailAlarmAcknowledger.java:36`). It exposes a `received(BEmail)` **action** (`flags=24`) `[CERT]` (`:30-37`)
that is link-wired to `BIncomingAccount`'s `received` topic `[INFER]` (the topic→action link is the standard
Niagara binding; the class provides the action side). `post()` queues the invocation asynchronously
(`Invocation` on the worker thread) so polling never blocks on ack work `[CERT]` (`:55-62`); the real logic is
`doReceived(BEmail)` `[CERT]` (`:64`).

## 327.5 — The UUID parse: In-Reply-To → subject → body

Two compiled patterns `[CERT]` (`:39-40`, `:75-76`):

- `replyToRegExp = "(alarm\\.)(<uuid>)"` — an `In-Reply-To` header value shaped `alarm.<UUID>`.
- `uuidRegExp = "()(<uuid>)"` — a bare canonical UUID (`<uuid>` = `[A-Fa-f0-9]{8}-…-[A-Fa-f0-9]{12}`).

Search order in `doReceived`, first hit wins, UUID always from capture `group(2)` `[CERT]` (`:82-102`):
1. `In-Reply-To` header via `replyToPattern` (`:82-91`);
2. else `Subject` via `uuidPattern` (`:83`, `:92-93`);
3. else `BTextPart` body text via `uuidPattern` (`:95-101`).
No match → `doFail("Unable to find UUID…")` `[CERT]` (`:96-98`). The `alarm.<uuid>` reply-to shape is exactly
what a `BEmailRecipient` alarm email would carry so a plain "Reply" acks it `[INFER]` (cross-gap; recipient in
[Block 34]).

## 327.6 — Security: authorization IS gated, but authentication is a forgeable `From:` header

Once a UUID is found, `doReceived` `[CERT]` (`:104-177`):
1. `alarmService.getAlarmDb().getDbConnection(null).getRecord(uuid)` — single-record lookup; `null` → `doFail`
   (`:107-136`).
2. `alarmClass = alarmService.lookupAlarmClass(record.getAlarmClass())` (`:138`).
3. Walk `BUserService` children; a user "matches" iff
   **`user.getEmail().equalsIgnoreCase(email.getFrom().getAddress())`** `[CERT]` (`:144`).
4. The matched user must be `getEnabled()` `[CERT]` (`:145-152`) AND
   `getPermissionsFor(alarmClass).hasAdminWrite()` `[CERT]` (`:154-161`); else `doFail`.
5. No match → `doFail("User not found…")` (`:168-170`); else `ackAlarm(uuid, user.getUsername())` `[CERT]`
   (`:176`).

**The finding.** The AUTHORIZATION is correct in structure — the acting identity must resolve to an *enabled*
Niagara user with *admin-write on that alarm class*. But the AUTHENTICATION of that identity is **only string
equality against the SMTP `From:` address** `[CERT]` (`:144`), which is trivially forgeable — the code performs
NO SPF/DKIM/DMARC check, no signed token, no challenge. I read all 181 lines and grep-confirmed the absence of
`token|hmac|signature|verify|password` — zero matches `[CERT]` (whole-file). So the ack is **spoofing-exposed**:
an attacker who can (a) deliver an email to the monitored INBOX, (b) knows the email address of a Niagara user
holding admin-write on the target alarm class, and (c) knows a live alarm record UUID, can acknowledge that
alarm with no credential `[INFER]`. The UUID travels in outbound alarm emails (§327.5), and user emails are
often organizationally guessable — so (b) and (c) are weak barriers `[INFER]`. Mitigation lives OUTSIDE this
class: SPF/DKIM enforcement at the mail server and a locked-down inbox — Niagara itself does not authenticate
the sender. This is a design-level exposure, not a coding bug; it belongs with the corpus security thread as an
inbound-trust antipattern `[INFER]`.

## 327.7 — What this block does NOT resolve

- `EmailUtil.fromMessage` — the MIME→`BEmail` conversion (attachments, encodings) — out of the email focus.
- `connectIncomingSession(store, type)` — the inbound authenticator (basic/OAuth) → **E7/E5**.
- `BAlarmAcknowledger.ackAlarm` internals (the alarm-side write) — alarm focus, [Block 34].

## 327.8 — Connections

- [Block 34] §34.6.5 — `BEmailRecipient` (outbound alarm email); §34.6.7 named `BSmsAlarmAcknowledger` but NOT
  this email acknowledger — §327.4-327.6 close that delta. The `alarm.<uuid>` reply shape pairs the two.
- [Block 326] §326.3 — `getIncomingSession` / `pop3.rsetbeforequit`, whose `!= delete` condition §327.3 explains.
- [Block 27] — the security/egress thread; §327.6 is an INBOUND-trust counterpart (email that can mutate alarm
  state).
- E7 — `BEmailAccount` poller/`connectIncomingSession` that drives §327.2.

## 327.9 — Self-verify

Block TYPE: **evidence**, with a security finding the driver verified directly (not on sub-agent trust): the
full `BEmailAlarmAcknowledger.java` was read in this iteration and the `From:`-only gate (`:144`), the
enabled + `hasAdminWrite` authorization (`:145-161`), and the absence of any token/signature check were
confirmed by reading, per the framework-semantic-check and falsify-before-reporting rules. Inbound defaults
(`port 110` `:99`, `store pop3` `:103`, `deliveryPolicy delete` `:104`, `received` topic `:94-95`, expunge
close `:318`) spot-checked verbatim.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (local file:line) | 28 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 7 |
| INFER/CERT ratio | 0.25 |

`verify-block.sh` exit 0.

Evidence block: `[INFER]`s are the exploitability chain (§327.6) and cross-block links, each grounded in a cited
`[CERT]` behavior.
