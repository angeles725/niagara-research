# Block 308 — The dispatch layer: `getResponse(0)` waits forever by design, and the network-wide dispatcher SERIALISES transmission — §14 qualifying Block 295's parallelism claim

> Focus **modbus**, gap **M13** (opened by [Block 295] as M11-c). What `modbusNet().dispatch(req)` +
> `req.getResponse(0)` actually do: which thread carries the send, what the `0` means, where the real
> timeout lives, and whether requests to different devices overlap. The answer **qualifies [Block 295]
> §295.7's parallelism conclusion**. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/modbusTcp-rt/…/comm/ModbusTcpSendRequest.java` (69 lines,
> jar `688bb50b…`); `organized/basicDriver/basicDriver-rt/vineflower/…/BBasicNetwork.java`,
> `…/comm/Comm.java` (387 lines); `docSource` original Tridium source (with javadoc) for
> `javax.baja.util.Worker` and `BWorker`.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official Tridium javadoc in the
> ORIGINAL (non-decompiled) source · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 295] (**qualified**), [Block 305]
> (the Rx side, which stays genuinely parallel), [Block 294] (`responseTimeout`/`retryCount`),
> [Block 4] (the driver framework).

---

## 308.1 — `getResponse(0)`: the `0` means "wait forever" `[CERT]`

`ModbusTcpSendRequest implements Runnable` and is a one-shot request/response holder `[CERT]`
`modbusTcp-rt/…/comm/ModbusTcpSendRequest.java:6-11`. The two halves `[CERT]` `:32-68`:

```java
public synchronized void execute() {
   this.response = null;
   try {
      if (!this.responseExpected) this.tcpDevice.getComm().transmitNoResponse(this.msg);
      else                        this.response = this.tcpDevice.getComm().transmit(this.msg);
   } catch (Exception e) { …; this.complete = true; this.notify(); return; }
   this.complete = true;
   this.notify();
}

public synchronized Message getResponse(int timeout) {
   if (!this.complete) {
      try { this.wait(timeout); } catch (Exception e) { … }
   }
   return this.response;
}
```

`[INFER]` `getResponse(0)` therefore calls `Object.wait(0)`, and in Java **`wait(0)` blocks
indefinitely** — it is not "poll without waiting". So the caller of `sendModbusMessage`
([Block 295] §295.7) parks with **no timeout of its own**.

That is safe only because the timeout lives downstream (§308.2) and because `execute()` calls `notify()` on
**both** paths — success and exception `[CERT]` `:47-49, 53-54`. `[INFER]` the failure mode it does not
cover: if `transmit()` were ever to block without returning or throwing, the caller would wait forever with
nothing to break it. Nothing observed suggests that happens; recorded as the shape of the risk, not as a
defect.

## 308.2 — The real timeout, and the retry loop `[CERT]`

`Comm.transmit(msg)` delegates to the three-argument form with the network's own settings `[CERT]`
`basicDriver-rt/…/comm/Comm.java:142-144`:

```java
return this.transmit(msg, this.basicNetwork.getResponseTimeout(), this.basicNetwork.getRetryCount());
```

which loops `for (int i = 0; i < retryCount + 1; i++) respMsg = this.processTransmit(msg, responseTimeout);`
`[CERT]` `:157-158`, and `processTransmit` is where the bounded wait happens `[CERT]` `:197-213`:

```java
CommTransaction transaction = this.transactionManager.getCommTransaction(msg);
synchronized (transaction) {
   this.tDriver.writeMessage(msg);
   this.getNetwork().incrementSent();
   if (!transaction.isComplete()) {
      transaction.wait(responseTimeout.getMillis());
      if (!transaction.isComplete()) { … "CommTransaction timed out (tag: …)" … }
   }
}
```

`[INFER]` so the total bound on a caller's indefinite `wait(0)` is
`responseTimeout × (retryCount + 1)`, both network properties ([Block 294] §294.4 — and note
[Block 294] §294.5 found the **server** network hides both, correctly, since a slave never initiates).
The lock taken here is on the **transaction**, not on the `Comm` — `[INFER]` so this is not where
serialisation between devices comes from.

## 308.3 — §14 — the dispatcher is network-wide and single-threaded, so sends DO serialise `[CERT]`

`dispatch()` on the network posts to a worker `[CERT]`
`basicDriver-rt/…/BBasicNetwork.java:244-246`:

```java
public final IFuture dispatch(Runnable r) { return this.getDispatcher().post(r); }
```

and `dispatcher` is a **property of the network** — `newProperty(4, new BBasicWorker(), null)` `[CERT]`
`BBasicNetwork.java:35, 65, 89`. Flag 4 = `Flags.HIDDEN` ([Block 294] §294.5), so it is invisible in the
property sheet.

`BBasicWorker extends BWorker` `[CERT]`
`basicDriver-rt/…/util/BBasicWorker.java:21`, and `BWorker` starts its worker with a **single** thread name
`[CERT]` `docSource/…/javax/baja/util/BWorker.java:85` — `getWorker().start(getWorkerThreadName())` — over a
`Worker` whose original Tridium javadoc reads: *"Worker is used to asynchronously perform 'work' on **a
background thread**. The 'work' is Runnables returned by the ITodo interface. The common case is to use a
Queue as the ITodo."* `[CERT-doc]` `docSource/…/javax/baja/util/Worker.java:14-18`, with a single
`Thread thread;` field `[CERT]` `:248`.

**Therefore** `[INFER]`: every `ModbusTcpSendRequest` on a given network is queued to **one** background
thread, and because `execute()` blocks inside `transmit()` until the response arrives or the timeout
expires (§308.2), **the dispatcher cannot start the next request until the current one finishes**. Sends on
a Modbus network are serialised, device count notwithstanding.

**This qualifies [Block 295] §295.7.** That block concluded, from the per-device `Comm` construction, that
Modbus TCP "scales with device count… twenty TCP devices poll concurrently on twenty sockets". The socket
and thread inventory it measured is correct — `ModbusTcpComm` is built per device
([Block 295] §295.7 `BModbusTcpDevice.java:145`) and each spawns its own Rx thread ([Block 305] §305.1).
What was **not** measured then is that the *outbound* path funnels through one network-level dispatcher.
The corrected picture:

| Direction | Granularity | Concurrency |
|---|---|---|
| **Send** (`dispatch` → `execute` → `transmit`) | **one worker per NETWORK** | **serialised** — one outstanding request at a time |
| **Receive** (Rx thread, socket) | one per **DEVICE** (TCP) | parallel |

`[INFER]` the practical consequence is materially different from §295.7's reading: adding devices to a
Modbus TCP network does **not** multiply throughput, because the request stream is serialised at the
dispatcher; what per-device sockets buy is that a slow or dead device does not corrupt another's framing and
its Rx thread blocks independently — isolation, not parallelism. The gateway/serial contrast of §295.7 still
holds in direction (they share a *port* as well), but the gap between TCP and gateway is narrower than that
block implied.

A pointer has been added to [Block 295] §295.7.

## 308.4 — What was NOT established

`[INFER]` I did not verify whether `Worker`'s queue is FIFO, nor whether a second `dispatch()` from a
different thread can interleave ahead of a queued one — that needs `javax.baja.util.Queue` and the `ITodo`
contract, which were not read. The serialisation claim rests on "one thread consuming the queue", which the
javadoc and the single `Thread` field do support; **ordering** within the queue does not follow from that
and is not claimed. Logged as **M21**.

## 308.5 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 27 | 26 |
| `[CERT-doc]` | 3 | 2 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 8 | 7 |
| **[INFER]/[CERT*] ratio** | | **7/28 = 0.25** |

Script exit 0. Note the `[CERT-doc]` here is NOT the Modbus guide (which says nothing about dispatch) —
it is the original Tridium **javadoc** in `docSource`, the project's highest-fidelity source tier.

**Block type: EVIDENCE (§14 qualification).**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | `getResponse(int)` calls `this.wait(timeout)` | `[CERT]` | `ModbusTcpSendRequest.java:57-65` |
| 2 | Callers pass `0` | `[CERT]` | `BModbusTcpDevice.java:254` ([Block 295] §295.7) |
| 3 | `execute()` notifies on both success and exception | `[CERT]` | `:47-49, 53-54` |
| 4 | `Comm.transmit` uses network `responseTimeout`/`retryCount` | `[CERT]` | `Comm.java:142-144` |
| 5 | Retry loop `retryCount + 1` | `[CERT]` | `:157-158` |
| 6 | `processTransmit` waits on the transaction with a millis bound | `[CERT]` | `:197-213` |
| 7 | The lock is on the transaction, not the Comm | `[CERT]` | `:202` |
| 8 | `dispatch()` posts to `getDispatcher()` | `[CERT]` | `BBasicNetwork.java:244-246` |
| 9 | `dispatcher` is a hidden property of the NETWORK | `[CERT]` | `:35, 65, 89` (flag 4 = HIDDEN) |
| 10 | `BBasicWorker extends BWorker` | `[CERT]` | `BBasicWorker.java:21` |
| 11 | `BWorker` starts one named worker thread | `[CERT]` | `BWorker.java:85` |
| 12 | `Worker` javadoc says "a background thread"; one `Thread` field | `[CERT-doc]` + `[CERT]` | `Worker.java:14-18` (original source javadoc) and `:248` |

Tokens grep-confirmed in their cited source: **12 / 12**. Claim 12 uses `docSource` — the **original
Tridium source with javadoc**, the highest-fidelity tier in this project's source hierarchy, not
decompiled output. The §14 in §308.3 rests on claims 8–12 taken together: network-scoped dispatcher (9) +
single thread (11, 12) + blocking `execute()` (1, 4, 6). Each was verified separately before the
conclusion was drawn.

No new sources preserved. Model tier: **no delegation — inline**.

## 308.x — Connections

- **[Block 295]** — **QUALIFIED**: §295.7's per-device socket/thread inventory stands; its throughput reading does not. See §308.3.
- **[Block 305]** — the Rx side, which remains genuinely per-device and parallel.
- **[Block 294]** — `responseTimeout`/`retryCount` are the bound in §308.2; the server network hides both.
- **[Block 4]** — the basic-driver framework these workers belong to.

**Gaps opened by this block**:
- **M21** — `Worker`/`Queue` ordering semantics: whether the dispatcher queue is FIFO and whether a
  concurrent `dispatch()` can jump ahead. Needed to say anything about *fairness* between devices on a
  serialised network → **new low gap**.
