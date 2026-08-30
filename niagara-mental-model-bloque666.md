# B666 — `BRecoverableRecipient` anatomy for a CUSTOM alarm recipient: no constructor (lifecycle is `started()`/`stopped()`), a single lazy `RetryThread`, and a persistent queue of one `<uuid>.xml` value-document per pending alarm under `file:^^alarm/<name>AlarmQueue/` — the ONLY method a subclass overrides is `sendAlarm` (focus alarm-webhook, AW1; §14→B34 §34.6.4)

> **Focus:** `alarm-webhook` (§16). **Gap closed:** AW1 (decompile `BRecoverableRecipient` for a custom `-rt`
> webhook recipient). **Phase:** static, READ-ONLY on the decompiled corpus.
> **Sources:**
> - `[CERT]` Tridium doc-source (highest fidelity, full javadoc): `organized/docSource/docSource-doc/extracted/alarm-rt/javax/baja/alarm/BRecoverableRecipient.java` (verified 1:1 against the vineflower decompile `organized/alarm/alarm-rt/vineflower/javax/baja/alarm/BRecoverableRecipient.java`).
> - `[CERT]` `organized/docSource/docSource-doc/extracted/alarm-rt/javax/baja/alarm/BAlarmRecipient.java` (base class).
> - `[CERT]` `organized/alarm/alarm-rt/vineflower/META-INF/module.xml` (type registration).
> - `[CERT]` corpus [Block 34] §34.6.4 (prior coverage — this block DEEPENS it and CORRECTS its inferred path).
>
> **Bottom line for the PoC:** to build `BMiWebhookRecipient extends BRecoverableRecipient`, you write ONE
> method — `protected boolean sendAlarm(BAlarmRecord)` — plus `@NiagaraType` and any config slots. Everything
> else (queue, retry thread, disk persistence, lifecycle) is inherited. Two facts CORRECT the intake
> assumptions: (1) the on-disk queue path is **`file:^^alarm/<recipientName>AlarmQueue/`** (station home),
> **not** `${protected.station.home}/alarm/recipients/{name}/`; (2) `dequeueMemory()` / `dequeueDisk()` are
> **`private`** — not overridable, not part of the subclass contract.

---

## §666.1 — There is NO custom constructor; the lifecycle is `started()` / `stopped()` `[CERT]`

`BRecoverableRecipient` (and Baja components generally) do **not** initialize through a Java constructor. The
class declares no constructor at all; the framework instantiates the type via `Sys.loadType(...)` and drives it
through component-lifecycle callbacks. For the retry machinery the relevant callbacks are `started()` and
`stopped()` `[CERT BRecoverableRecipient.java:372-393]`:

```java
@Override
public void started() throws Exception {
  // start the retry thread and see if there were any queued alarms from
  // the last time the station was running
  persistenceDirectory = BOrd.make("file:^^alarm/"+getName()+"AlarmQueue");
  retryThread = new RetryThread();
  retryThread.start();
}

@Override
public void stopped() throws Exception {
  if (retryThread != null) { retryThread.kill(); retryThread = null; }
}
```

Implications for the custom recipient:
- `started()` runs when the component is mounted/enabled in a running station. If a subclass needs its own
  init, it overrides `started()` and **must call `super.started()`** (otherwise no retry thread, no disk
  recovery). Same for `stopped()`.
- `persistenceDirectory` is (re)computed from `getName()` at `started()` — so **renaming** the recipient
  changes its queue directory; alarms left in the old directory are orphaned until the component is renamed
  back. `[INFER — from the `getName()` interpolation]`

## §666.2 — The persistent queue: one `<uuid>.xml` value-document per pending alarm `[CERT]` — §14 corrects B34 §34.6.4

When a send fails **and** `persistent == true` (default `true`, §666.5), `handleAlarm` serializes the alarm to
disk `[CERT BRecoverableRecipient.java:453-471]`:

```java
File file = new File(dirFile(), alarmRecord.getUuid().toString() + ".xml");
try (ValueDocEncoder encoder = new ValueDocEncoder(file)) {
  encoder.encodeDocument(alarmRecord);          // Baja value-document XML
}
setQueuedAlarmCount(dirFile().listFiles().length);
```

- **Format:** one file per pending alarm, named `<BAlarmRecord.getUuid()>.xml`, containing the alarm encoded
  as a Baja **value-document** via `javax.baja.io.ValueDocEncoder` (the same encoder used for `.bog`-style
  value trees; see [Block 5]/[Block 33]). Decoded on recovery via `ValueDocDecoder.decodeDocument()`
  `[CERT:564-566]`.
- **Directory:** resolved by `dirFile()` `[CERT:520-527]` from the ORD `file:^^alarm/<name>AlarmQueue`:
  ```java
  FilePath path = (FilePath)persistenceDirectory.parse()[0];
  BDirectory bdir = BFileSystem.INSTANCE.makeDir(path);
  return ((BLocalFileStore)bdir.getStore()).getLocalFile();
  ```
  `^^` resolves to the **station home**. So the real path is `<stationHome>/alarm/<recipientName>AlarmQueue/`
  — e.g. a recipient named `telegramHook` writes to `<stationHome>/alarm/telegramHookAlarmQueue/`.

  **§14 correction to [Block 34] §34.6.4**, which stated the path as
  `${protected.station.home}/alarm/recipients/{recipientName}/` and flagged it *"inferido de permissions"*.
  The code shows the literal ORD is `file:^^alarm/<name>AlarmQueue` — there is **no `recipients/` segment**
  and the directory basename is `<name>AlarmQueue`, not the bare name. The `alarm/-` FilePermission in
  `alarm-rt/module.xml` (lines 115-116) covers this path recursively, which is why the inference landed in the
  right *directory tree* but the wrong *leaf*. (B34 §34.6.4 is being corrected in place with a pointer to this
  block.)

## §666.3 — Method signatures a subclass sees `[CERT]`

| Member | Signature | Visibility | Subclass role |
|---|---|---|---|
| `sendAlarm` | `protected abstract boolean sendAlarm(BAlarmRecord alarm) throws Exception` `[:588]` | abstract | **The one method you implement.** |
| `handleAlarm` | `public void handleAlarm(BAlarmRecord alarmRecord)` `[:424]` | public (override of base abstract) | Already implemented here — orchestrates send→retry. Do **not** override for a webhook. |
| `dequeueMemory` | `private void dequeueMemory() throws Exception` `[:529]` | **private** | Not visible, not overridable. |
| `dequeueDisk` | `private void dequeueDisk() throws Exception` `[:544]` | **private** | Not visible, not overridable. |
| `poll` | `private void poll()` `[:405]` | private | Internal retry tick. |
| `doClearAlarmQueue` | `public void doClearAlarmQueue()` `[:493]` | public | The `clearAlarmQueue` action handler (manual flush). |

**Correction to the intake request**, which asked for "the signatures of `sendAlarm`, `dequeueMemory()`,
`dequeueDisk()`": `dequeueMemory`/`dequeueDisk` are **`private`** — they are implementation detail, not part
of the extension contract. The subclass surface is exactly `sendAlarm` (mandatory) plus optional overrides of
the lifecycle/`getIcon`/`getSubscribedAlarmClasses`.

### The `sendAlarm` return-value contract `[CERT:581-589]` (the javadoc, verbatim)

> *"Override this method to do the actual sending of the alarm. @return true if alarm was sent successfully,
> false if alarm failed and no retry is needed. @throws Exception if alarm fails to send — causes alarm to be
> added to retry queue."*

Three outcomes, all consumed by `handleAlarm` `[CERT:431-435]`:
1. **`return true`** → success. `handleAlarm` sets `lastSendTime`, `status = ok`.
2. **`return false`** → give up silently, **no retry, not queued** (`if (!sucess) return;` `[:436]`).
3. **`throw Exception`** → failure WITH retry. `handleAlarm` sets `status = fault` + `lastFailureCause`,
   queues the alarm (disk if `persistent`, else memory), and (re)starts the `RetryThread`.

For a webhook: throw on a transient failure (network error, HTTP 5xx, timeout) so it gets retried; `return
false` on a permanent rejection you never want retried (e.g. HTTP 400/401 from a bad token — retrying won't
help); `return true` on 2xx.

## §666.4 — The retry thread: a single lazy `RetryThread`, self-terminating when the queue drains `[CERT]`

Inner class `RetryThread extends Thread` `[CERT:636-682]`, thread name literal **`"alarm:RecipRetryThread"`**
`[:641]`:

```java
public void run() {
  alive = true;
  while (alive) {
    try { sleep(Math.max(getRetryInterval().getMillis(), 1000)); }  // floor 1000 ms
    catch (InterruptedException e) { alive = false; break; }
    try { poll(); } catch (Exception e) { }
  }
}
```

- **Interval:** `retryInterval` property, default `BRelTime.make(15000)` = **15 s** `[:99-102]`, with a hard
  **floor of 1000 ms** (`Math.max(..., 1000)`).
- **Lifecycle:** started once in `started()`. `poll()` `[:405-421]` calls `dequeueDisk()` (if persistent &
  count>0) or `dequeueMemory()`, and when `getQueuedAlarmCount() == 0` it **kills itself and nulls the field**
  (`retryThread.kill(); retryThread = null;`). It is then **lazily re-created** in `handleAlarm` on the next
  failed send (`if (retryThread == null) { retryThread = new RetryThread(); ... }` `[:484-488]`). So there is
  **at most one** retry thread per recipient, and it exists only while the queue is non-empty.
- `kill()` sets `alive=false` and `interrupt()`s the sleep `[:644-648]`.

## §666.5 — The property slots (auto-generated, `@NiagaraProperty`) `[CERT:53-124]`

| Slot | Type | Default | Flags | Purpose |
|---|---|---|---|---|
| `status` | `BStatus` | `BStatus.DEFAULT` | READONLY | ok / fault |
| `lastSendTime` | `BAbsTime` | NULL | READONLY | last success |
| `lastAckSendTime` | `BAbsTime` | NULL | HIDDEN | last ack success |
| `lastFailureTime` | `BAbsTime` | NULL | READONLY | last failure |
| `lastFailureCause` | String | `""` | READONLY | last error text |
| `retryInterval` | `BRelTime` | `15000 ms` | — | retry cadence (floor 1 s) |
| `queuedAlarmCount` | int | 0 | READONLY \| TRANSIENT | live queue depth |
| `persistent` | boolean | **`true`** | — | queue survives restart (disk) vs in-memory-only |
| **action** `clearAlarmQueue` | — | — | — | manual flush (`doClearAlarmQueue`) |

`persistent=false` keeps the queue in an in-memory `javax.baja.util.Queue q` `[:629]` that is **lost on
station restart**. For guaranteed delivery leave `persistent=true` (the default).

## §666.6 — Minimal skeleton this block certifies (for the PoC)

```java
@NiagaraType
@NiagaraProperty(name="url",   type="String",    defaultValue="")
@NiagaraProperty(name="token", type="BPassword", defaultValue="BPassword.make(\"\")")  // see [B669]
public class BMiWebhookRecipient extends BRecoverableRecipient {
  // ... Slot-o-Matic generated block ...
  @Override
  protected boolean sendAlarm(BAlarmRecord alarm) throws Exception {
    // 1) serialize alarm → JSON  2) POST to getUrl() with Bearer getToken()
    // return true on 2xx; return false on non-retryable (400/401);
    // throw on transient (timeout/5xx) to be queued+retried.
  }
}
```
Threading caveat (why `sendAlarm` must be FAST or offload) is [Block 667]; registration/`module.xml` is
[Block 668]; the `BPassword` token is [Block 669].

## §666.7 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | No constructor; init via `started()`/`stopped()` | [CERT] | BRecoverableRecipient.java:372-393 |
| 2 | Queue path = `file:^^alarm/<name>AlarmQueue/` (station home), NOT `alarm/recipients/{name}/` | [CERT] | :378, :520-527 |
| 3 | Each pending alarm = one `<uuid>.xml` via `ValueDocEncoder.encodeDocument` | [CERT] | :460-471, :564-566 |
| 4 | `sendAlarm` is the only abstract method; `dequeueMemory`/`dequeueDisk` are private | [CERT] | :588, :529, :544 |
| 5 | `sendAlarm` contract: true=sent, false=no-retry, throw=queue+retry | [CERT] | :581-589, :431-436 |
| 6 | Single lazy `RetryThread` "alarm:RecipRetryThread", 15 s default / 1 s floor, self-terminates at count 0 | [CERT] | :636-682, :99-102, :405-421 |
| 7 | `persistent` default true; false = in-memory Queue lost on restart | [CERT] | :114-118, :629 |
| 8 | §14 corrects B34 §34.6.4 inferred path | [CERT] | this block §666.2 |

**Tally:** 8 claims — 8 [CERT], 0 [INFER standalone] (one [INFER] noted inline for the rename-orphan
consequence). 0 unmarked.

## §666.8 — Connections

- **[Block 34] §34.6.4** — prior coverage of `BRecoverableRecipient`; this block DEEPENS it (real method
  bodies, real path) and **§14-corrects** its inferred persistence path. B34 edited in place with a pointer.
- **[Block 667]** — threading: what thread calls `handleAlarm`/`sendAlarm`, and why a blocking POST here is
  dangerous (the OOM risk of B34 §34.1.2 / G1).
- **[Block 668]** — `module.xml` + type registration for the custom `-rt` recipient.
- **[Block 669]** — storing the webhook token as a `BPassword`.
- **[Block 5]/[Block 33]** — the value-document (`.bog`/`ValueDoc`) encoding used for the queued files.
