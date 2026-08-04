# Block 325 — The outgoing SMTP pipeline: `BOutgoingAccount`'s dual queue, the 100/day cap, and a retry ceiling that is off-by-one between memory and disk

> Focus **email** — evidence block E2. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: `javax.baja.email.BOutgoingAccount` (961 lines) — the SEND side that [Block 324] §324.5 routes to.
> Specifically: the `send` action and `doSend` admission gate, the memory-vs-disk queue, the `maxSendablePerDay`
> rate limiter and its self-renewing midnight reset, and the retry-then-discard loop that consumes the
> `niagara.email.maxNumberOfRetriesBeforeDiscard` ceiling [Block 324] §324.6.
>
> Sources (primary, decompiled N4.14.0.162):
> `organized/email/email-rt/vineflower/javax/baja/email/BOutgoingAccount.java` (sweep: sonnet; all 5
> load-bearing citations re-verified inline by the driver). Constant cross-ref:
> `.../javax/baja/email/BEmailService.java:33-36`.
>
> EXCLUDES by design (other gaps): the JavaMail `Session`/TLS/STARTTLS `Properties` assembly →
> **E3** (`MailPlatformHandlerSe`); authenticator/credentials/OAuth → **E7**; inbound POP3/IMAP → **E4**.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. Layer 8 (Alarm/notification
> subsystem — transport tier). Block TYPE: **evidence**.

---

## 325.1 — The admission gate: `doSend` decides discard vs enqueue BEFORE any network work

`send` is the same action shape as the service's: `@NiagaraAction(name="send", parameterType="BEmail")`
`[CERT]` (`BOutgoingAccount.java:160-163`, `:196`, `:333-335`). The framework routes it to
`doSend(BEmail, Context)` `[CERT]` (`:471-502`), which is a pure ADMISSION gate — it never touches SMTP; it
either enqueues or discards, in this order `[CERT]`:

1. **Disabled** — unless `allowDisabledQueueing` OR (`enabled` AND `isOperational()`) → discard `[CERT]` (`:471-502`).
2. **Queue full** — `getQueueSize() >= getMaxQueueSize()` (default 100) → discard `[CERT]` (`:471-502`, `maxQueueSize` `:189`).
3. **Daily cap hit** — `getNumberSent() >= getMaxSendablePerDay()` → discard AND `doClearQueue()` `[CERT]` (`:471-502`).
4. **From-stamp** — if `email.getFrom().equals(BEmailAddress.DEFAULT)`, the account's `replyTo` is stamped as
   the sender `[CERT]` (`:471-502`, `replyTo` `:184`).
5. **Enqueue** — `enqueueDisk(email, cx)` if `persistent`, else the in-memory `q.enqueue(email)` `[CERT]`.

So back-pressure is by DISCARD, not blocking: an over-quota or over-full account silently drops mail (counted
in `numberDiscarded`) rather than stalling the caller `[INFER]`.

## 325.2 — Two queues, selected by one flag; and the default loses mail on restart

There is a **memory queue** — `private final Queue q = new Queue()` (`javax.baja.util.Queue`) `[CERT]` (`:205`)
— and a **disk queue**. Which one is used is decided by a single boolean property:

`@NiagaraProperty(name="persistent", type="boolean", defaultValue="false")` `[CERT]` (`:179`). Default **false**.

- `persistent=false` (default) → memory queue. **Emails in the queue are lost across a station restart**
  `[INFER]` (a `javax.baja.util.Queue` is not persisted). This is the same class of default-unsafe gotcha the
  Modbus driver has with `criticalData=false` [Block 315] — the safe-for-delivery choice is OFF by default.
- `persistent=true` → disk queue under Protected Station Home.

The disk directory is a UUID-namespaced template: `BOrd.make("file:^^email/$(uuid)")` `[CERT]` (`:202`). The
`$(uuid)` is substituted ONCE at startup (migration from the legacy `file:^email`) with a fresh
`BUuid.make().toString()` `[CERT]` (`:591-604`, migration `:415-433`); an already-migrated path is recognised
by the pattern `file:\^\^email/<uuid>` `[CERT]` (`:201`). Each queued email is one file
`new File(persistenceDir, BUuid.make() + ".xml")` `[CERT]` (`:748`), encoded via `ValueDocEncoder`/decoded via
`ValueDocDecoder` `[CERT]` (`:752-758`, `:667-686`). Net on-disk shape: `file:^^email/<dir-uuid>/<email-uuid>.xml`
`[INFER]`. Emails larger than `maxPersistedEmailSize` (default 25 000 KB) are deleted and discarded `[CERT]`
(`maxPersistedEmailSize` `:186`).

## 325.3 — The rate limiter and its self-renewing midnight reset

`maxSendablePerDay` = `newProperty(0, 100, min=1)` — default **100/day**, floor 1 `[CERT]` (`:192`). The counter
`numberSent` is a transient read-only int (flags=3) `[CERT]` (`:191`), incremented per successful send `[INFER]`.

The daily reset is a **one-shot, self-renewing** `Clock` chain, NOT a `scheduleDaily`:

```java
private Ticket midnight() {
   BAbsTime next = BAbsTime.now().nextDay();
   BAbsTime midnight = next.timeOfDay(0, 0, 0, 0);
   return Clock.schedule(this, midnight, resetNumberSent, null);
}
```
`[CERT]` (`:906-911`). When `resetNumberSent` fires, `doResetNumberSent` cancels the old ticket and calls
`midnight()` again, arming the next one-shot `[CERT]` (`:532-537`). So the "daily" cadence is an actual chain of
single-shot tickets re-armed at each local-midnight, which means it tracks the station's wall clock / DST
rather than a fixed 24 h period `[INFER]`.

## 325.4 — Retry then discard — and the off-by-one between the two queues

The ceiling is the service constant [Block 324] §324.6:
`MAX_NUMBER_OF_RETRIES_BEFORE_DISCARD = Integer.getInteger("niagara.email.maxNumberOfRetriesBeforeDiscard", 6)`
`[CERT]` (`BEmailService.java:34-35`). Both queues consume it — but with a DIFFERENT comparison:

- **Memory path** (`dequeueMemory`): `email.incrementNumberOfRetries()` each attempt; on `MessagingException`,
  if `email.getNumberOfRetries() > MAX` → discard (do not re-enqueue, `addDiscarded(1)`), else `q.enqueue(email)`
  to retry `[CERT]` (`:853`, `:878-883`). Strict `>` means the email is retried until its counter EXCEEDS 6 —
  i.e. up to **7 send attempts** before discard `[INFER]`.
- **Disk path** (`dequeueDisk`): if `retriedPersistedEmails.getOrDefault(file,0) >= MAX` → `deleteFile(...)`
  and throw `email.outgoingAccount.maxRetryLimitReached` `[CERT]` (`:662-664`); the counter is bumped via
  `compute(...)` (null→0 else +1) `[CERT]` (`:711-712`). `>=` means discard at **6** `[INFER]`.

So the SAME `maxNumberOfRetriesBeforeDiscard=6` yields a different effective attempt count depending on whether
`persistent` is on (disk: ≤6) or off (memory: ≤7) `[INFER]`. It is a small correctness asymmetry, invisible from
the property sheet, that an operator tuning the sysprop would not expect. `retriedPersistedEmails` is a
`Map<File,Integer>` `[CERT]` (`:207`); a decode failure (email==null) is discarded immediately, not retried
`[CERT]` (`:711-712` guard).

This also resolves [Block 34] §34.6.5 Gotcha G8 ("the recipient side has no retry queue"): the retry/queue
machinery lives HERE on `BOutgoingAccount`, downstream of the recipient — the recipient hands off and the
account owns durability `[INFER]` (cross-block).

## 325.5 — The drain worker: `pollQueue`

Draining is driven by the account poller thread (defined on the base `BEmailAccount`, gap E7): a
`Poller extends Thread` named `"EmailAccountPoller"` looping `doPoll()` with a sleep of
`max(pollrate.getMillis(), 1000)` — floor 1 s, default `pollrate` 60 s `[CERT]`
(`BEmailAccount.java:400-426`, `:58`/`:112`). Chain: `Poller.run() → doPoll() → poll() → pollQueue()` `[CERT]`
(`BOutgoingAccount.java:436`, `:448`).

`pollQueue()` `[CERT]` (`:448-469`) re-checks the daily cap first (`doClearQueue()` if hit), then per drain:

```java
Session session = this.mailPlatformHandler.getOutgoingSession(this);   // E3 owns this
Transport smtpTransport = session.getTransport("smtp");
((BEmailClientAuthenticator)getEmailAuthenticator().make()).connectOutgoingSession(smtpTransport);  // E7
if (getPersistent()) dequeueDisk(smtpTransport, session); else dequeueMemory(smtpTransport, session);
smtpTransport.close();
```
`[CERT]` (`:448-469`). The actual `Transport.sendMessage(msg, recipients)` runs inside a
`PrivilegedExceptionAction` `[CERT]` (`:697` disk, `:868` memory). Note the transport type string is hardcoded
`"smtp"` — there is no `"smtps"`; implicit-SSL on 465 is `smtp` + `useSsl` on the account `[CERT]` (`:458`; enum
noted by the E1 audit). The `getOutgoingSession` (TLS/Properties) and `connectOutgoingSession` (auth) hand-offs
are E3 and E7 respectively — deliberately not opened here.

## 325.6 — Sending-relevant property surface

| Property | Type | Default | Role | Cite |
|---|---|---|---|---|
| `port` | int | **25** | SMTP port (override of base) | `:170` |
| `transport` | BTransport | `smtp` | only enum value | `:172`-ish (E1 audit) |
| `connectionTimeout` | BRelTime | 10 s | JavaMail connect+read timeout (E3) | `:182` |
| `replyTo` | BEmailAddress | DEFAULT | stamped as From when From is default | `:184` |
| `persistent` | boolean | **false** | disk vs memory queue (§325.2) | `:179` |
| `persistenceDirectory` | BOrd | `file:^email`→migrated | disk queue root | `:415-433` |
| `maxPersistedEmailSize` | int | 25 000 KB | oversize → discard | `:186` |
| `allowDisabledQueueing` | boolean | false | enqueue while disabled | (`doSend` gate) |
| `maxQueueSize` | int | 100 | full → discard | `:189` |
| `maxSendablePerDay` | int | **100** | daily rate cap | `:192` |
| `queueSize`/`numberSent`/`numberDiscarded` | int | 0 | transient read-only counters | `:190-191` |

`port=25` confirms [Block 27]'s port row from the sending side; 465/587 come from `useSsl`/`useStartTls`
(E3), not a distinct transport enum `[INFER]`.

## 325.7 — What this block does NOT resolve

- `getOutgoingSession(this)` — the JavaMail `Session`/`Properties` (host, port, TLS, STARTTLS, timeouts) → **E3**.
- `getEmailAuthenticator().make().connectOutgoingSession(transport)` — SMTP AUTH / OAuth → **E7**.
- The `Poller`/`BEmailAccount` base (`isOperational`, `updateStatus`, `beginPolling`) → **E7**.

## 325.8 — Connections

- [Block 324] §324.5 — `BEmailService.doSend` routes to `BOutgoingAccount.send`; §324.6 defines the retry
  constant this block consumes.
- [Block 34] §34.6.5 — `BEmailRecipient`; its Gotcha G8 ("no retry on recipient side") is explained here — the
  retry queue is on the account.
- [Block 27] — SMTP ports; `port=25` default confirmed from the account.
- [Block 315] — the Modbus `criticalData=false` default-unsafe pattern; `persistent=false` is its email analog.

## 325.9 — Self-verify

Block TYPE: **evidence**. Delegated sweep: **sonnet** (961-line class); the driver re-verified all 5
load-bearing citations inline against the source before writing (`:202` disk template, `:448-469` pollQueue,
`:906-911` midnight one-shot, `:662-664` disk retry `>=`, `BEmailService.java:34-35` constant) — 5/5 resolved
verbatim. The memory-path `>` vs disk-path `>=` asymmetry was read directly at `:878-883` and `:662-664`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (local file:line) | 32 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 11 |
| INFER/CERT ratio | 0.34 |

`verify-block.sh` exit 0.

Evidence block: `[INFER]`s are framework-behaviour deductions (discard-not-block, restart mail loss, DST-tracking
reset, effective attempt counts) and cross-block links, not padding.
