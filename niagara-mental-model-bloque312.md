# Block 312 — The dispatcher queue: FIFO by design, fully synchronised, capped at 256 — and `QueueFullException` propagates raw to the caller

> Focus **modbus**, gap **M21** (opened by [Block 308] §308.4). Whether the serialised send queue is FIFO,
> whether a concurrent `dispatch()` can jump ahead, and what happens under saturation. Also a deliberate
> contrast with [Block 311]: the same Baja library, the same author, opposite synchronisation decisions.
> READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `docSource` **original Tridium source with javadoc** —
> `baja/javax/baja/util/Queue.java`; `organized/basicDriver/basicDriver-rt/vineflower/…/util/BBasicWorker.java`.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 308] (which opened this and depends on
> the answer), [Block 311] (the synchronisation contrast), [Block 295] (poll cadence, the queue's producer).

---

## 312.1 — FIFO, stated in the javadoc `[CERT]`

`javax.baja.util.Queue` is available as original Tridium source. Its class javadoc is one line `[CERT]`
`docSource/…/baja/javax/baja/util/Queue.java:6-13`:

> *"Queue is a linked list of objects designed for **FIFO access**."*

That settles the first half of [Block 308] §308.4: the dispatcher queue **is** FIFO by design, not by
accident.

## 312.2 — And `BBasicWorker` uses `enqueue`, not `push` `[CERT]`

The queue exposes **both** ends. `enqueue(Object)` appends; `push(Object)` inserts *"to the front of the
queue"* `[CERT]` `Queue.java:208-214` (javadoc verbatim). So a caller *could* jump the line — the mechanism
exists.

`BBasicWorker.post(Runnable)` does not use it `[CERT]`
`basicDriver-rt/…/util/BBasicWorker.java:39-45`:

```java
public final IFuture post(Runnable r) {
   if (this.isRunning() && this.queue != null) { this.queue.enqueue(r); return null; }
   else throw new NotRunningException();
}
```

`[INFER]` therefore **no Modbus request can jump ahead of another**: `dispatch()` → `post()` → `enqueue()`,
always the tail. Combined with [Block 308] §308.3's single consumer thread, the ordering guarantee is
complete — requests to every device on a network are served strictly in submission order.

That answers the fairness question [Block 308] §308.4 left open, and the answer is favourable: **no device
can starve another by queue position.** A slow device still delays everyone behind it (the consumer blocks
in `transmit()`), but it cannot be *overtaken* either — the serialisation is fair, not merely serial.

## 312.3 — The cap is 256, and overflow is not handled here `[CERT]`

`BBasicWorker` declares `maxQueueSize` with a default of **256** `[CERT]` `:17-22`, and the queue is
constructed from it on first use `[CERT]` `:48-52`:
`this.queue = this.makeQueue(this.getMaxQueueSize()); this.worker = new Worker(this.queue);`

`Queue.enqueue` *"@throws QueueFullException [if] the queue is already at max size"* `[CERT]`
`Queue.java:211-212` (javadoc verbatim).

`[INFER]` and `post()` does **not** catch it — the only exception it handles is the not-running case
(`NotRunningException`). So a `QueueFullException` propagates up through `dispatch()` into whatever called
`sendModbusMessage`. Nothing in the Modbus code observed so far catches it by name.

The saturation scenario is concrete `[INFER]`: sends are serialised ([Block 308] §308.3) and each occupies
the consumer for up to `responseTimeout × (retryCount + 1)` ([Block 308] §308.2). With the 1-minute
`socketOptionTimeout` of [Block 305] §305.2 in play on a hung link, the consumer can be blocked for a long
time while the poll scheduler keeps producing at `fastRate` (1 s, [Block 295] §295.6). **256 queued requests
is roughly four minutes of a 1-second poll cycle** — reachable on a network with several unresponsive
devices. This is a bound worth knowing; whether it is ever hit in practice is a live-system question, not a
static one.

## 312.4 — The contrast with `IntHashMap` is deliberate `[CERT]`

Every mutator and accessor on `Queue` is `synchronized` — `tail`, `peek`, `peek(int)`, `find`, `dequeue`,
`dequeue(int)`, `enqueue`, `push`, `toArray`, `clear` `[CERT]` `Queue.java:84-247`. (`size()` is the one
exception `[CERT]` `:47`.)

`[INFER]` set against [Block 311] §311.1 — `IntHashMap`, same `javax.baja` library, same author
(Brian Frank), **zero** `synchronized` — this is clearly a per-class decision rather than an era or a
house style. `Queue` was written to be handed between a producer and a consumer thread and is synchronised
throughout; `IntHashMap` was written as a fast single-threaded container and is not.

That does **not** prove [Block 311]'s conditional. It does remove one possible defence of it — "Baja code of
that vintage just wasn't synchronised" is not true; the library synchronises what it intends to share.
Recorded as a supporting observation, not as an upgrade of that block's conclusion.

## 312.5 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 21 | 20 |
| `[CERT-doc]` / `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 6 | 5 |
| **[INFER]/[CERT*] ratio** | | **5/20 = 0.25** |

Script exit 0.

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | `Queue` javadoc says "designed for FIFO access" | `[CERT]` | `Queue.java:6-13` verbatim, original source |
| 2 | `push()` inserts at the front | `[CERT]` | `:208-214` javadoc verbatim |
| 3 | `BBasicWorker.post()` calls `enqueue`, never `push` | `[CERT]` | `BBasicWorker.java:39-45` read in full |
| 4 | `post()` catches only the not-running case | `[CERT]` | same method — ABSENCE bounded to it |
| 5 | `maxQueueSize` default 256 | `[CERT]` | `:17-22` |
| 6 | Queue built from it on first `getWorker()` | `[CERT]` | `:48-52` |
| 7 | `enqueue` throws `QueueFullException` at max size | `[CERT]` | `Queue.java:211-212` javadoc |
| 8 | All 10 listed methods are `synchronized`; `size()` is not | `[CERT]` | `rg` over the full file, both directions checked |

Tokens grep-confirmed in their cited source: **8 / 8**. Claims 1, 2 and 7 come from the **original Tridium
javadoc**, not decompiled output. Claim 4 is an ABSENCE bounded to `post()` — it says that method does not
catch `QueueFullException`, not that nothing anywhere does; §312.3's wording keeps that distinction
("nothing in the Modbus code observed so far").

The 256 ≈ four-minutes arithmetic in §312.3 is `[INFER]` from the cited default and [Block 295] §295.6's
`fastRate`; it is an order-of-magnitude illustration, not a measurement.

No new sources preserved. Model tier: **no delegation — inline**.

## 312.x — Connections

- **[Block 308]** — opened this; §312.2 completes its serialisation picture with an ordering guarantee, and §312.3 adds the queue bound its blocking consumer implies.
- **[Block 311]** — §312.4's contrast; supporting, not conclusive.
- **[Block 295]** — the poll scheduler is the producer feeding this queue.
- **[Block 305]** — the 1-minute socket timeout that makes saturation reachable.

**Gaps opened by this block**: none. Whether the 256 cap is ever reached is a live-system question and
falls under the dynamic-phase disposition already tracked in `RESEARCH-STATE-protocols.md`; it is not
re-opened here.
