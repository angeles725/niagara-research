# Block 309 — Serial framing: Tridium replaces RTU's baud-relative 3.5-character silence with fixed millisecond thresholds, and `serial-rt` is pure API

> Focus **modbus**, gap **M14** (opened by [Block 296] as M2-a). Where the serial line is configured, and —
> the real question — where RTU's inter-frame timing lives and what rule it actually implements. Unlike the
> MS/TP case of [Block 279], this one **is** reachable in Java. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `organized/serial/serial-rt/vineflower/javax/baja/serial/` (13 classes);
> `sources/decompiled/modbusAsync-rt/…/comm/ModbusAsyncRxDriver.java` (212 lines),
> `…/comm/ModbusAsyncSerialComm.java` (174 lines), `…/BModbusAsyncNetwork.java` (jar `45156565…`).
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 294] (the three delay properties),
> [Block 295] (the ASCII ceiling penalty), [Block 308] (serialised sends, which compounds the latency here),
> [Block 279] (the MS/TP precedent — where the equivalent timing was NOT in Java), [Block 131] (RTU framing
> on the wire).

---

## 309.1 — `serial-rt` is an API module, not an implementation `[CERT]`

The whole of `javax.baja.serial` is **13 classes** `[CERT]`, and every one is a type, an enum, an interface
or an exception: `BSerialHelper`, `BBaudRate`, `BSerialBaudRate`, `BSerialDataBits`, `BSerialStopBits`,
`BSerialParity`, `BSerialFlowControlMode`, `BISerialHelperParent`, `BISerialPort`, `BISerialService`,
`PortClosedException`, `PortDeniedException`, `PortNotFoundException`.

`BSerialHelper extends BComponent` carries the line settings `[CERT]`
`serial-rt/…/BSerialHelper.java:18-48`:

| Property | Default |
|---|---|
| `status` | `BStatus.down` |
| `portName` | `BSerialHelper.noPort` |
| `baudRate` | **`baud9600`** |
| `dataBits` | `dataBits8` |
| `stopBits` | `stopBit1` |
| `parity` | **`none`** |
| `flowControlMode` | `none` |

`[INFER]` the 9600/8/N/1 default matches the guide's prose exactly — *"Communications rates are typically at
9600 baud"* ([Block 294] §294.1, `[CERT-doc]` §Architecture) — so the documentation is describing the
shipped default rather than field practice.

`BSerialBaudRate` offers **18** rates from `baud50` to `baud115200` `[CERT]` (enumerated: 50, 75, 110, 134,
150, 200, 300, 600, 1200, 1800, 2400, 4800, 9600, 19200, 38400, 57600, 76800, 115200).

`[INFER]` the actual port implementation is not here — `BISerialPort`/`BISerialService` are interfaces, and
the platform modules `platSerial`, `platSerialNpsdk`, `platSerialQnx` exist alongside in the module set.
That is the same JNI boundary shape [Block 279] found for MS/TP. **The difference is what sits above it**
(§309.2).

## 309.2 — The RTU timing IS in Java — contrast with [Block 279] `[CERT]`

For MS/TP, [Block 279] established the framing (preamble, CRC, token passing) was **absent** from the Java
corpus and lived in native `mstpnpsdk`/`platmstp`. The equivalent question here resolves the other way:
`ModbusAsyncRxDriver` implements the inter-character and end-of-frame logic **in Java**, timestamping with
`javax.baja.sys.Clock` `[CERT]` `modbusAsync-rt/…/comm/ModbusAsyncRxDriver.java:11, 24, 53, 63-65, 189-190`:

```java
long rxCharTicks = 0L;                       // :24  — timestamp of the last received character
this.rxCharTicks = Clock.ticks();            // :53
long delta   = Clock.ticks() - this.rxCharTicks;                        // :63-64
long maxDelta = this.network.getMaxRxInterCharacterDelay().getMillis(); // :65
…
long millis = this.network.getMinRxFrameEnd().getMillis();              // :189
return Clock.ticks() - this.rxCharTicks < millis;                       // :190
```

and `ModbusAsyncSerialComm` enforces the inter-message gap the same way `[CERT]` `:132, 148-150`:
`long minDelay = ((BModbusAsyncNetwork) this.getNetwork()).getInterMessageDelay().getMillis();` compared
against `Clock.ticks() - this.lastRecvMessageTicks`.

So the three properties [Block 294] §294.4 inventoried are genuinely consumed here, in the Java layer:
`maxRxInterCharacterDelay` (**50 ms**), `minRxFrameEnd` (**20 ms**), `interMessageDelay` (**0**).

**This is a positive finding where [Block 279] had a negative one**, and the difference is worth naming:
`[INFER]` MS/TP is a token-passing MAC whose timing must be enforced at the driver/UART level, so it went
native; Modbus RTU framing is only *silence detection* on a received byte stream, which Java can do with
timestamps — imprecisely, but adequately (§309.3).

## 309.3 — The rule implemented is NOT the spec's rule `[CERT]` / `[INFER]`

Modbus RTU defines end-of-frame as a silence of **3.5 character times** (t3.5), and the maximum permitted
gap *within* a frame as 1.5 character times (t1.5). Both are **relative to the baud rate**. Tridium's
implementation compares against **fixed millisecond properties** that know nothing about the configured
baud `[CERT]` (`getMillis()` at `:65` and `:189`; no baud term appears in either expression).

An 11-bit Modbus character (start + 8 data + parity + stop) gives t3.5 = 3.5 × 11 / baud:

| Baud | t3.5 (spec) | `minRxFrameEnd` default | Ratio |
|---|---|---|---|
| 1200 | ≈ 32.1 ms | 20 ms | **threshold is SHORTER than t3.5** |
| 2400 | ≈ 16.0 ms | 20 ms | 1.2× |
| 9600 | ≈ 4.0 ms | 20 ms | 5× |
| 19200 | ≈ 2.0 ms | 20 ms | 10× |
| 38400 | ≈ 1.0 ms | 20 ms | 20× |
| 115200 | ≈ 0.33 ms | 20 ms | **60×** |

`[INFER]` two consequences, in opposite directions:

- **At high baud the threshold is enormously conservative.** At 115200 the driver waits 20 ms of silence to
  declare a frame complete where the spec needs 0.33 ms. That is ~20 ms of pure added latency **per received
  frame**, and since [Block 308] §308.3 showed sends are serialised per network, the latency is additive
  across every device on the trunk. Raising the baud rate on a Modbus serial network therefore buys far less
  than the baud figure suggests — the frame-end wait dominates.
- **At 1200 baud the threshold is shorter than t3.5.** It does not corrupt framing, because the relevant
  in-frame limit is t1.5 (≈ 13.75 ms at 1200), which is still below the 20 ms threshold — so a legitimate
  in-frame gap will not be mistaken for a frame end. But the margin is thin, and at 1200 baud the driver is
  no longer implementing the spec's rule in any meaningful sense.

`[INFER]` the properties are configurable precisely because the fixed defaults cannot suit 18 baud rates.
Neither the guide nor the property names hint at the relationship to baud; an integrator at 115200 has no
reason to suspect `minRxFrameEnd` is what is limiting throughput.

## 309.4 — Two behaviours worth knowing `[CERT]`

**Inter-character checking can be switched off.** If `maxRxInterCharacterDelay` is **0**, the driver logs
*"Not checking inter-char timing."* and sets `maxDelta = -1`, disabling the check entirely `[CERT]`
`ModbusAsyncRxDriver.java:66-69`.

**ASCII ignores the property.** When the protocol is ASCII and checking is on, `maxDelta` is **overwritten
with a hard-coded 1000 ms** `[CERT]` `:71-75`:

```java
if (this.isAsciiProtocol() && maxDelta > 0L) { maxDelta = 1000L; partialMessage = ASCII_PARTIAL_MESSAGE; }
```

`[INFER]` correct in principle — ASCII frames are delimited by `:` and CRLF ([Block 131] §131.6), not by
silence, so a one-second guard is just a stuck-stream backstop. But it means `maxRxInterCharacterDelay` is
**silently inert on an ASCII network**: setting it changes nothing except whether checking happens at all.

When the gap is exceeded, the partial frame is discarded and counted `[CERT]` `:77-87`:
`this.network.incrementPartialRxMsgs()` — the `totalPartialRxMsgs` counter of [Block 294] §294.7, which
[Block 300] §300.3 established is **spy-only and cannot be alarmed on**. `[INFER]` so the one signal that a
serial trunk's timing is mistuned is a counter no operator can trend.

The discard also logs the buffer as hex plus `" character deltaT = " + delta + ", expected: " +
this.responseSize + ", received: " + this.rcvCount` `[CERT]` `:81-84` — `[INFER]` which is the single most
useful diagnostic line in the serial driver, since it prints the measured gap next to the threshold that
rejected it.

## 309.5 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 27 | 26 |
| `[CERT-doc]` | 1 | 1 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 11 | 10 |
| **[INFER]/[CERT*] ratio** | | **10/27 = 0.37** |

Script exit 0. Ratio 0.37 — the timing table of §309.3 is arithmetic over one cited fact (the thresholds
are baud-independent), so the inference count is high relative to the small code surface. Not an
exhaustion signal.

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | `serial-rt` is 13 classes, all API | `[CERT]` | full directory enumeration |
| 2 | `BSerialHelper` defaults 9600/8/N/1, no flow control | `[CERT]` | `BSerialHelper.java:18-48` |
| 3 | 18 baud rates, 50…115200 | `[CERT]` | `rg -o 'baud\d+'` over `BSerialBaudRate.java`, sorted unique |
| 4 | `ModbusAsyncRxDriver` timestamps with `Clock.ticks()` | `[CERT]` | `:11, 24, 53` |
| 5 | Inter-character check compares `delta` to `maxRxInterCharacterDelay` | `[CERT]` | `:63-65` |
| 6 | Frame-end check compares to `minRxFrameEnd` | `[CERT]` | `:189-190` |
| 7 | `interMessageDelay` enforced in `ModbusAsyncSerialComm` | `[CERT]` | `:132, 148-150` |
| 8 | **No baud term appears in either timing expression** | `[CERT]` | both expressions read in full — ABSENCE, bounded to the two methods that implement the timing |
| 9 | `maxRxInterCharacterDelay == 0` disables the check | `[CERT]` | `:66-69` |
| 10 | ASCII overrides `maxDelta` to a hard-coded 1000 | `[CERT]` | `:71-75` |
| 11 | Exceeded gap → `incrementPartialRxMsgs()` + hex dump + deltaT line | `[CERT]` | `:77-87` |
| 12 | Defaults 50 ms / 20 ms / 0 | `[CERT]` | `BModbusAsyncNetwork.java:30-44` ([Block 294] §294.4) |

Tokens grep-confirmed in their cited source: **12 / 12**. The t3.5 column of §309.3 is **arithmetic**, not a
citation — 3.5 × 11 / baud, with the 11-bit character stated as the assumption; it is marked `[INFER]` and
the conclusion drawn from it (conservative at high baud) does not depend on the exact bit count.

Claim 8 is the load-bearing ABSENCE. Per RE-MEASURE it was checked two ways: reading both timing
expressions in full, and confirming no `getBaudRate()` call appears anywhere in `ModbusAsyncRxDriver` or
`ModbusAsyncSerialComm`.

**Contrast with [Block 279] recorded deliberately**: that block's MS/TP negative and this block's positive
come from the same style of question asked of two protocols. Documenting why they differ (§309.2) is the
finding, not just the outcome.

No new sources preserved (`serial-rt` read from `organized/`). Model tier: **no delegation — inline**.

## 309.x — Connections

- **[Block 294]** — §294.4 inventoried the three delay properties; this block shows where each is consumed.
- **[Block 295]** — the ASCII read-ceiling halving; §309.4 adds that ASCII also ignores the inter-character property.
- **[Block 308]** — serialised sends make §309.3's per-frame latency additive across the trunk.
- **[Block 279]** — the MS/TP precedent, resolved the opposite way; §309.2 explains the difference.
- **[Block 300]** — `totalPartialRxMsgs`, the spy-only counter this timing feeds.

**Gaps opened by this block**: none. The native port layer (`platSerial*`) is named but out of scope — it is
the JNI boundary below the framing, and nothing in this gap's question depends on it. Recorded as a scope
call, not an open question.
