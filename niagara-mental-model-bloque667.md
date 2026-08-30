# B667 — Alarm dispatch threading: `sendAlarm` runs INLINE on the single shared `"Alarm:ServiceWorker"` thread, fed by an UNBOUNDED `CoalesceQueue` — so a blocking HTTP POST in a custom recipient stalls ALL alarm routing and risks OOM; `BRecoverableRecipient` decouples RETRIES only, never the first send (focus alarm-webhook, AW2; §14 confirms/refines B34 §34.1.3 + G1)

> **Focus:** `alarm-webhook` (§16). **Gap closed:** AW2 (what thread runs `handleAlarm`/`sendAlarm`; is the
> feeding queue bounded; does `BRecoverableRecipient` already decouple the send). **Phase:** static, READ-ONLY.
> **Sources** (Tridium doc-source vineflower tree, all `[CERT]`, re-verified by the driver):
> - `organized/docSource/docSource-doc/vineflower/alarm-rt/javax/baja/alarm/BAlarmService.java`
> - `organized/docSource/docSource-doc/vineflower/alarm-rt/javax/baja/alarm/BAlarmClass.java`
> - `organized/docSource/docSource-doc/vineflower/baja/javax/baja/util/{Worker,Queue,CoalesceQueue}.java`
> - `organized/docSource/docSource-doc/extracted/alarm-rt/javax/baja/alarm/{BAlarmRecipient,BRecoverableRecipient}.java`
> - `[CERT]` corpus [Block 34] §34.1.3 (routing pipeline) + §34.1.2/G1 (alarm-queue OOM) — this block CONFIRMS
>   and refines them with exact class/thread names.
>
> **Bottom line for the PoC (design-critical):** the initial delivery of every alarm to your recipient runs
> **synchronously on one process-wide thread** (`"Alarm:ServiceWorker"`), and the queue behind that thread is
> **unbounded**. If `sendAlarm` does a **blocking** HTTP POST, then during a network stall / storm that one
> thread is stuck, **no other recipient or alarm class routes**, and the unbounded queue grows in heap → OOM.
> `BRecoverableRecipient`'s retry thread does **not** save you here — it only replays *already-failed* sends.
> **You must make the POST short (aggressive connect/read timeouts) OR offload it to your own bounded worker
> and return fast from `sendAlarm`.**

---

## §667.1 — The dispatch chain, hop by hop `[CERT]`

```
BAlarmService.fireAlarm(record)
  └─ enqueues onto the service action queue                                  [async]
       (the shared alarmQueue, drained by ONE worker thread)
  ▼  Worker "Alarm:ServiceWorker" dequeues
BAlarmService.doRouteToRecipient(alarm)          BAlarmService.java:1048
  └─ ac.routeAlarm(alarm)                         BAlarmService.java:1054
       └─ BAlarmClass.routeAlarm  =  Flags.HIDDEN | Flags.ASYNC   BAlarmClass.java:482
            └─ post() → enqueues Invocation onto GET_ALARM_QUEUE  (= the same alarmQueue)
  ▼  Worker "Alarm:ServiceWorker" dequeues again
BAlarmClass.doRouteAlarm(alarm) → fires the 'alarm' topic to linked recipients
  ▼  (recipient's routeAlarm action = Flags.SUMMARY, NOT async → runs inline)
BAlarmRecipient.doRouteAlarm(alarm)              BAlarmRecipient.java:283-287
  └─ if (accept(alarm)) handleAlarm(alarm)
       ▼
BRecoverableRecipient.handleAlarm(alarm)         BRecoverableRecipient.java:424
  └─ boolean sucess = sendAlarm(alarm)   ◄── YOUR CODE, SYNCHRONOUS   :435
```

Every hop's flag and enqueue is `[CERT]`. The one deduced link is that the recipient's `routeAlarm`
(`Flags.SUMMARY`, `BAlarmRecipient.java:239`) runs on the firing (worker) thread rather than hopping again —
that follows from Baja action semantics (non-`ASYNC` actions execute synchronously on the invoking thread) and
from the single-worker/single-queue topology below. `[INFER — SUMMARY-action synchronous semantics]`

## §667.2 — The thread: ONE shared worker named `"Alarm:ServiceWorker"` `[CERT]`

`BAlarmService` owns exactly one queue + one worker for the whole service:

```java
private final Queue alarmQueue = new CoalesceQueue();   // BAlarmService.java:1736
private Worker alarmWorker;                              // :1737
...
alarmWorker = new Worker(alarmQueue);                    // :585 (and :723)
alarmWorker.start("Alarm:ServiceWorker");                // :587 (and :724)  ← thread name literal
```

`Worker` is a single-threaded run loop `[CERT Worker.java:100-110, 129-141, 165-168]`: `start()` creates one
`Thread(...)`, `run()` loops `while(isAlive)` dequeuing, and `process(work)` does `work.run()` — i.e. it runs
each enqueued `Invocation` **to completion, inline, before dequeuing the next**. So:

- There is **one** alarm-routing thread for the entire station, shared by **all** alarm classes and **all**
  recipients (`BAlarmService.java:1736-1737, 585`). There is **no per-recipient worker** for the primary send.
- A recipient whose `sendAlarm` blocks freezes `Worker.run()` → **every other recipient and every other alarm
  class stops routing** until it unblocks. `[CERT topology]`

## §667.3 — The queue: UNBOUNDED (`CoalesceQueue`, `maxSize = Integer.MAX_VALUE`) `[CERT]`

```java
public CoalesceQueue()          { this(Integer.MAX_VALUE); }   // CoalesceQueue.java:37-39
```
`Queue.enqueue` only rejects at that cap: `if (size >= maxSize) throw new QueueFullException();`
`[CERT Queue.java:188-192]` — with `maxSize = Integer.MAX_VALUE` (~2.1e9) there is **no practical cap, no drop,
no blocking backpressure**; the queue grows in heap until OOM.

Partial mitigation: it is a **coalescing** queue — *duplicate coalesceable* invocations merge instead of
adding. So a flood of *identical* alarms coalesces, but **distinct** alarm records still accumulate without
bound. `[CERT CoalesceQueue.enqueue]`

This CONFIRMS [Block 34] §34.1.3 (which drew the pipeline as `alarmQueue.put(record) ◄── unbounded Queue` and
an "alarmWorker thread") and §34.1.2 / **Gotcha G1** (the alarm-queue OOM scenario). §14: B34 named the thread
"alarmWorker" descriptively; the exact runtime thread name is **`"Alarm:ServiceWorker"`** and the exact class
is `CoalesceQueue`.

## §667.4 — `BRecoverableRecipient` decouples RETRIES, not the FIRST send `[CERT]`

The intake asked whether `BRecoverableRecipient` already moves sending off the dispatch thread. **It does
not.** Two distinct threads, two distinct roles:

| Attempt | Thread | Bounded? |
|---|---|---|
| **First delivery** of each alarm | `"Alarm:ServiceWorker"` (shared, §667.2) — `handleAlarm`→`sendAlarm` inline | queue UNBOUNDED (§667.3) |
| **Retries** of a *failed* send | `"alarm:RecipRetryThread"` (per-recipient, [Block 666] §666.4) | retry `Queue q` also UNBOUNDED (`new Queue()` ⇒ MAX_VALUE, BRecoverableRecipient.java:629) |

The retry thread only ever replays alarms that `sendAlarm` **already threw on** (`q.enqueue` at
BRecoverableRecipient.java:480 / disk path). The **first** attempt for every alarm is always inline on
`"Alarm:ServiceWorker"` (`handleAlarm` reached via §667.1). So the retry machinery protects **delivery
durability**, not **dispatch-thread liveness**. Both accumulation points (the service `alarmQueue` and the
per-recipient retry `q`) are unbounded.

## §667.5 — What this dictates for the webhook PoC `[INFER — engineering consequence of §§667.2-667.4]`

1. **Never do a blocking POST directly in `sendAlarm` without tight timeouts.** With `HttpURLConnection`:
   `setConnectTimeout(...)` and `setReadTimeout(...)` to small values (e.g. 2-5 s). A default
   `HttpURLConnection` has **no** timeout and can hang indefinitely — exactly the §667.2 stall.
2. **Prefer offloading.** Return fast from `sendAlarm` by handing the JSON payload to your own **bounded**
   `java.util.concurrent` executor / worker (with a max queue + a reject policy), and do the real POST there.
   Then decide the `sendAlarm` return value strategy:
   - Simplest correct option: do the POST with tight timeouts inline and use `throw`/`false`/`true` to drive
     `BRecoverableRecipient`'s own retry+persistence (you inherit durability for free — [Block 666]).
   - Offloaded option: return `true` immediately and manage your own retry — but then you **lose**
     `BRecoverableRecipient`'s disk persistence guarantee, so you must re-implement durability. Trade-off is
     yours.
3. **Bound anything you add.** The framework's own queues are unbounded; do not add a third unbounded queue.
   A bounded executor with a drop/reject policy converts an OOM into a dropped alarm + a logged warning, which
   is the safer failure mode for a notification side-channel.
4. **Backpressure reality:** because the dispatch queue can't push back, the only real defense during a storm
   is (a) fast/bounded `sendAlarm`, and (b) the alarm class's `coalesce`/priority config upstream (B34 §34.2).

## §667.6 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | One shared queue+worker for the whole service; thread name `"Alarm:ServiceWorker"` | [CERT] | BAlarmService.java:585,587,1736-1737 |
| 2 | Feeding queue is `CoalesceQueue`, maxSize `Integer.MAX_VALUE` = unbounded | [CERT] | CoalesceQueue.java:37-39; Queue.java:188-192 |
| 3 | `BAlarmClass.routeAlarm` is `HIDDEN\|ASYNC` → enqueues on the alarm queue | [CERT] | BAlarmClass.java:482 |
| 4 | `doRouteToRecipient` → `ac.routeAlarm(alarm)` | [CERT] | BAlarmService.java:1048,1054 |
| 5 | Recipient `routeAlarm` is `SUMMARY` (not async) → `handleAlarm`→`sendAlarm` inline on the worker | [CERT flag] + [INFER semantics] | BAlarmRecipient.java:239,283-287; BRecoverableRecipient.java:435 |
| 6 | Worker runs each invocation to completion inline (blocking recipient stalls all routing) | [CERT] | Worker.java:129-141,165-168 |
| 7 | Retry runs on separate per-recipient `"alarm:RecipRetryThread"`, only for failed sends; retry `q` also unbounded | [CERT] | BRecoverableRecipient.java:629,636-641,480 |
| 8 | Confirms/refines B34 §34.1.3 + G1 (OOM) | [CERT] | this block §667.3; [Block 34] |

**Tally:** 8 claims — 7 [CERT], 1 mixed [CERT flag + INFER semantics] (#5), plus §667.5 marked [INFER]
engineering consequence. 0 unmarked.

## §667.7 — Connections

- **[Block 666]** — the recipient anatomy: `sendAlarm` contract, the retry thread, the persistent queue.
- **[Block 34] §34.1.3 / §34.1.2 (G1)** — the routing pipeline and the alarm-queue OOM scenario this block
  confirms with exact names.
- **[Block 34] §34.6.3 (G6)** — `BStationRecipient`'s synchronous `handleAlarm` blocking the worker is the
  same hazard, already flagged for the supervisor bottleneck; a blocking webhook is the modern instance.
- **[Block 668]** — module.xml (the `NiagaraSocketPermission`/`URLPermission` your POST needs).
- **[Block 669]** — the `BPassword` token read inside `sendAlarm`.
