# Block 305 — `ModbusTcpRxDriver`: a three-state socket machine, the two receive modes behind `rxProcessMode`, and why the length field is read one byte wide

> Focus **modbus**, gap **M12** (opened by [Block 295] as M11-b; also closing **M2-b**, the `rxProcessMode`
> property [Block 296] left unresolved). The TCP receive side: the socket lifecycle and its four states,
> the finite state machine the Rx thread runs, the two framing strategies, and how a response is matched to
> its request. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/modbusTcp-rt/…/comm/ModbusTcpRxDriver.java` (358 lines,
> jar `688bb50b…`), `…/BSocketStatusEnum.java`.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 295] (the per-device socket and Rx
> thread), [Block 294] (`socketOptionTimeout`, the network counters), [Block 296] (`rxProcessMode`,
> `disableTransactionIdCheck`, `socketStatus`), [Block 131] (the MBAP header being parsed here),
> [Block 303] (§303.5's deferred exception-byte question, partly answered).

---

## 305.1 — The receiver owns the socket `[CERT]`

`ModbusTcpRxDriver extends CommReceiver` `[CERT]`
`modbusTcp-rt/…/comm/ModbusTcpRxDriver.java:24` and holds the connection state as fields `[CERT]` `:25-39`:
`Socket commSocket`, `OutputStream out`, `numOutstandingRequests`, `numConnectionFailures`, `state`,
plus two separate monitors — `idleMonitor` and `connectMonitor`.

This confirms from the receive side what [Block 295] §295.7 inferred from the transmit side: the **Tx driver
does not own a stream**. `writeOutputStream(Message)` is a method *on the receiver* `[CERT]` `:70`, which is
exactly what `ModbusTcpTxDriver` calls. `[INFER]` one object owns the socket, both directions go through it,
and that is why there is one Rx thread per Comm rather than a Tx/Rx pair.

Socket state is published to the component tree through `BSocketStatusEnum`, a four-value enum —
`closed` = 0, `openPending` = 1, `openFailed` = 2, `opened` = 3 `[CERT]`
`modbusTcp-rt/…/BSocketStatusEnum.java:12`. `[INFER]` this resolves the magic numbers of [Block 295] §295.7:
`sendModbusMessage` reconnects when `socketStatus` is 0 or 2 (**closed or openFailed**) and only dispatches
when it is 3 (**opened**).

`initSocketConnection()` is the reconnect sequence, `synchronized` `[CERT]` `:332-346`:
`nullStreams()` → `closeSocket()` → `connectSocket()` → `createStreams()` → `successSocketConnectionInit()`,
with any exception setting `socketStatus = openFailed` on the device *or* the gateway, whichever owns this
receiver.

## 305.2 — A three-state machine that idles for five seconds `[CERT]`

Three internal states `[CERT]` `:41-43`: `STATE_IDLE = 0`, `STATE_NO_SOCKET = 1`, `STATE_GOT_SOCKET = 2`,
switched through a `synchronized switchState(int)` `[CERT]` `:100`.

The loop body `[CERT]` `:118-133`:

```java
if (this.exit)            return false;
else if (this.state == 2) return this.readMessage();      // GOT_SOCKET → block on the stream
else {
   synchronized (this.idleMonitor) { this.idleMonitor.wait(5000L); }
   return false;
}
```

`[INFER]` so in any state other than `GOT_SOCKET` the thread parks on a monitor with a **5-second** ceiling.
It is a wait, not a sleep — `initSocketConnection()` can notify it awake — so five seconds is the *worst*
case before a re-check, not a fixed reconnect interval. Compare the slave side, whose reconnect timing
[Block 294] §294.4 recorded as a `socketTimeoutInMillis` of 30000: the two sides use different mechanisms
and different magnitudes.

Before every read, `setSocketTimeout()` applies the network's `socketOptionTimeout` as the socket's
`SO_TIMEOUT` `[CERT]` `:227-234` — the property [Block 294] §294.4 measured at a **1-minute** default.
`[INFER]` that is the real ceiling on a hung read: the Rx thread blocks up to one minute per attempt unless
the property is lowered, which for a poll cycle running at [Block 295]'s 1-second `fastRate` is a very long
stall. Failure to set it is swallowed into a trace line `[CERT]` `:231-233`.

## 305.3 — `rxProcessMode`: packet mode vs byte mode `[CERT]` — M2-b closed

The property [Block 296] §296.6 found on both `BModbusTcpDevice` and `BModbusTcpGateway` (default **false**)
selects between two framing strategies, named by two constants `[CERT]` `:44-45`:
`RX_MODE_PACKET = true`, `RX_MODE_BYTE = false`.

`readMessageFromStream()` reads the flag from the device or, for a gateway network, from the gateway
`[CERT]` `:143`, then branches `[CERT]` `:145-164`:

**Packet mode (`true`)** — one call, trusting the stack to deliver a whole frame:

```java
rxSize = this.getInputStream().read(ibuf, 0, 261);
```

**Byte mode (`false`, the default)** — read one byte at a time, discovering the length as it goes:

```java
int dataLen = 256;
do {
   int x = in.read();
   if (x < 0) throw new SocketException("End of stream.");
   ibuf[rxSize++] = (byte) x;
   if (rxSize == 6) dataLen = x;          // <-- the 6th byte becomes the expected length
} while (rxSize < dataLen + 6);
```

`[INFER]` byte mode is the robust one: it never over-reads into the next frame and never under-reads a
fragmented one, at the cost of a syscall per byte plus a trace line per byte (`rxLog.trace("rc= "…)`
`[CERT]` `:153`). Packet mode is the fast one and assumes the frame arrives in a single TCP segment — which
is exactly the assumption that breaks on a gateway or a busy link. The default being byte mode is the safe
choice; `rxProcessMode = true` is the performance escape hatch.

**The length field is read one byte wide.** At `rxSize == 6` the byte just stored is MBAP offset 5 — the
**low** byte of the 2-byte Length field ([Block 131] §131.3) — and it alone becomes `dataLen` `[CERT]`
`:160-161`. `[INFER]` the high byte is never consulted, so a frame declaring a length above 255 would be
mis-framed. In practice this is unreachable: a Modbus PDU maxes at 253 bytes, and the buffer is sized
`byte[261]` = 6 + 255 `[CERT]` `:141` to match. It is a deliberate simplification, not a latent overflow —
but it does mean the receiver is not a general MBAP parser.

## 305.4 — Framing, exception detection and transaction matching `[CERT]`

Once a frame is in, `[CERT]` `:170-180`:

```java
if (rxSize >= 9) {
   if (0 != (ibuf[7] & 128) && network.getLog().isTraceOn())
      network.getLog().trace("MODBUS exception response - type " + ibuf[8]);
   byte[] rxData = new byte[rxSize - 6];
   System.arraycopy(ibuf, 6, rxData, 0, rxData.length);
   int transactionId = (ibuf[0] & 255) << 8 | (ibuf[1] & 255);
   this.numOutstandingRequests = 0;
   …
}
```

Three things fall out `[INFER]`:

- **the 6-byte split** — everything from offset 6 onward is handed on as `rxData`, so the unit id travels
  with the PDU rather than with the header. That matches [Block 303] §303.1, where the slave dispatcher
  reads `newMessage[0]` as the device address and `newMessage[1]` as the function code;
- **exception detection is a trace line only** — the `ibuf[7] & 128` test (the `| 0x80` marker
  [Block 303] §303.5 sets on the way out) logs *"MODBUS exception response - type " + ibuf[8]* and does
  nothing else here; the actual handling happens upstream via the response's exception code;
- **`ibuf[8]` is the exception code**, which **partially answers** [Block 303] §303.5's deferred question:
  on the wire, byte 8 of a TCP frame (PDU offset 2) carries the exception type. This is the *receive* view;
  it does not by itself prove what the slave's `byteCount` field emits, so M19 stays open — but it fixes the
  position the code must land in.

`numOutstandingRequests` is reset to 0 on every successful frame `[CERT]` `:179`. The transaction id is
reassembled big-endian from `ibuf[0..1]` `[CERT]` `:177-178`, which is what
`disableTransactionIdCheck`/`maxTransactionId` ([Block 296] §296.6) govern and what feeds
`totalTransactionIdErrors` ([Block 294] §294.7).

## 305.5 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 37 | 36 |
| `[CERT-doc]` | 1 | 1 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 8 | 7 |
| **[INFER]/[CERT*] ratio** | | **7/37 = 0.19** |

Script exit 0. (The single `[CERT-doc]` counted is the sentence below naming the marker, not a citation.)

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | `extends CommReceiver`; owns socket, streams, counters, two monitors | `[CERT]` | `:24-39` |
| 2 | `writeOutputStream` is a method on the **receiver** | `[CERT]` | `:70` |
| 3 | `BSocketStatusEnum` = closed/openPending/openFailed/opened | `[CERT]` | `BSocketStatusEnum.java:12` |
| 4 | `initSocketConnection()` sequence and its `openFailed` handling | `[CERT]` | `:332-346` |
| 5 | Three states 0/1/2 with those names | `[CERT]` | `:41-43` |
| 6 | Idle branch waits 5000 ms on `idleMonitor` | `[CERT]` | `:118-133` |
| 7 | `setSocketTimeout()` applies `socketOptionTimeout` as SO_TIMEOUT | `[CERT]` | `:227-234` |
| 8 | `RX_MODE_PACKET`/`RX_MODE_BYTE` constants | `[CERT]` | `:44-45` |
| 9 | Packet mode = single 261-byte read | `[CERT]` | `:147` |
| 10 | Byte mode loop, `dataLen` taken at `rxSize == 6` | `[CERT]` | `:149-164` read in full |
| 11 | Buffer is `byte[261]` | `[CERT]` | `:141` |
| 12 | Exception test `ibuf[7] & 128`, logs `ibuf[8]` | `[CERT]` | `:171-173` |
| 13 | `rxData` copied from offset 6; transaction id from `ibuf[0..1]` | `[CERT]` | `:174-178` |

Tokens grep-confirmed in their cited source: **13 / 13**. Claim 10 is the load-bearing one and was read in
full rather than grepped; the one-byte length reading (claim 10 + 11) was cross-checked against the buffer
size to confirm the simplification is bounded rather than a defect — 6 + 255 = 261 exactly.

No new sources preserved. `[CERT-doc]`: none — the guide has no topic on the receive driver.
Model tier: **no delegation — inline**.

## 305.x — Connections

- **[Block 295]** — §295.7's per-device socket and Rx thread; this block is that thread's body. The `socketStatus == 3` magic number is resolved in §305.1.
- **[Block 294]** — `socketOptionTimeout` (1 min) applied in §305.2; `totalTransactionIdErrors` fed by §305.4.
- **[Block 296]** — closes **M2-b** (`rxProcessMode`, §305.3) and supplies the mechanism behind `disableTransactionIdCheck`/`maxTransactionId`.
- **[Block 131]** — §131.3's MBAP header is what §305.3/§305.4 parse; the one-byte length reading is a Tridium simplification of it.
- **[Block 303]** — §305.4 fixes the exception code at wire offset 8, partially answering its §303.5 deferral; **M19 remains open**.

**Gaps opened by this block**: none. `connectSocket()`/`createStreams()` were located but not read line by
line; they are ordinary socket plumbing and nothing observed suggests otherwise — recorded as a scope call,
not an open question.
