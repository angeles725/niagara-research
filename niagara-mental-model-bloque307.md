# Block 307 — The exception response: `byteCount` IS the exception code — §14 correcting Block 303's "inconsistency"

> Focus **modbus**, gap **M19** (opened by [Block 303] §303.5, partially answered by [Block 305] §305.4).
> The serialiser reveals that the two "disagreeing" `byteCount` values [Block 303] flagged are not a defect
> at all: on a frame whose function code carries bit 7, that field is the Modbus exception code. This block
> closes the gap and **corrects [Block 303] §303.5**. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/modbusCore-rt/…/messages/ModbusResponse.java` (378 lines,
> jar `a0b67420…`); cross-checked against `sources/decompiled/modbusTcpSlave-rt/…/comm/ModbusUnsolicitedReceive.java`
> and `sources/decompiled/modbusTcp-rt/…/comm/ModbusTcpRxDriver.java`.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 303] (**corrected**), [Block 305]
> (the receive-side view that constrained the answer), [Block 131] (§131.4 PDU layout, §131.10 exception
> codes), [Block 300] (the exception vocabulary).

---

## 307.1 — The serialiser `[CERT]`

`ModbusResponse.formatBaseMessage()` builds every outgoing response `[CERT]`
`modbusCore-rt/…/messages/ModbusResponse.java:68-90`:

```java
ModbusOutputStream out = new ModbusOutputStream();
out.write((byte) this.deviceAddress);
out.write((byte) this.functionCode);
switch (this.functionCode) {
   case 5: case 6:
      out.writeWord(this.startAddress);  out.write(this.data);        break;
   case 15: case 16:
      out.writeWord(this.startAddress);  out.writeWord(this.numberPoints); break;
   default:
      out.write((byte) this.byteCount);
      if ((this.functionCode & 128) == 0) out.write(this.data);
}
```

Read the `default` arm carefully. It emits `byteCount` as **one byte**, unconditionally — and then emits
`data` **only when bit 7 of the function code is clear**.

## 307.2 — Therefore `byteCount` occupies the exception-code position `[CERT]` / `[INFER]`

For a normal read response (FC 1–4, bit 7 clear) the frame is
`deviceAddress · functionCode · byteCount · data…` — `byteCount` is a genuine byte count, exactly as
[Block 131] §131.4 documented.

For an **exception** response (FC | 0x80, bit 7 set) the same code emits
`deviceAddress · functionCode|0x80 · byteCount` and **stops** — no `data` `[CERT]` `:84-86`.

`[INFER]` so on an exception frame the third byte — the position the Modbus specification reserves for the
exception code — is whatever was put in `byteCount`. The field is **reused as the exception-code carrier**.
There is no separate exception-code field in the serialised frame; `ModbusResponse.exceptionCode` is an
in-memory field used for `isError()` `[CERT]` `:93-95` and for the debug string `[CERT]` `:253`, and it is
**never written to the wire** by this method.

**Cross-check against the receiver, independently derived.** [Block 305] §305.4 found the TCP Rx driver
reading the exception type from `ibuf[8]` `[CERT]` `ModbusTcpRxDriver.java:171-173`. Counting the frame it
parses: `ibuf[6]` = deviceAddress, `ibuf[7]` = functionCode (the byte it tests with `& 128`), `ibuf[8]` =
the next byte — which by §307.1 is precisely `byteCount`. **Emitter and receiver agree**, and the agreement
was reached from two files read in different iterations for different gaps. That is the RE-MEASURE
condition satisfied by construction.

## 307.3 — §14 — [Block 303] §303.5 was wrong, and here is what it should have said

[Block 303] §303.5 recorded the two refusal paths of the slave dispatcher and flagged them as inconsistent:

| Path | `byteCount` | `data` |
|---|---|---|
| unsupported FC (`ModbusUnsolicitedReceive.java:144-146`) | 1 | `new byte[0]` |
| read failure (`:207-208`) | 2 | `new byte[0]` |

and concluded *"one of the two is wrong, or `byteCount` carries a different meaning on an exception frame"*.

**The second alternative is the correct one, and the values are not arbitrary** `[INFER]`:

| Path | Value | Modbus exception |
|---|---|---|
| unsupported function code | **1** | **01 — Illegal Function** |
| read failed (address outside `valid*Range`) | **2** | **02 — Illegal Data Address** |

Both are exactly right for their situation, and they match the vocabulary [Block 300] §300.1 catalogued
(`illegalFunction` = 1, `illegalDataAddress` = 2) and the guide's own troubleshooting entry, which tells a
user chasing *"Read fault: illegal data address"* to check the point address `[CERT-doc]`
([Block 300] §300.4) — that message is produced by this very path.

The empty `data` array is not a bug either: §307.1 shows `data` is **not written** on an exception frame, so
its contents are irrelevant.

**What [Block 303] got right** was refusing to guess: it recorded the observation, named the two competing
explanations, and deferred rather than asserting a defect. The cost of that discipline was one extra block;
the cost of guessing would have been a wrong defect claim sitting in the corpus. [Block 303] §303.5 is
hereby superseded by this section — a pointer has been added there.

## 307.4 — What this means for the client side

`[INFER]` the same reuse applies when Niagara is the **master** reading a response: the driver populates
`byteCount` from the third byte and, when bit 7 is set, that value is the exception code. That is consistent
with [Block 295] §295.4, where a null response is turned into `exceptionCode = 9` in memory rather than
being parsed off the wire — code 9 (`deviceTimeout`) never travels, it is synthesised locally
([Block 300] §300.1).

So the two numbering spaces of [Block 300] meet here: **positive codes arrive in the `byteCount` position of
an exception frame; negative codes are local and never serialised.**

## 307.5 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 15 | 14 |
| `[CERT-doc]` | 1 | 1 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 6 | 5 |
| **[INFER]/[CERT*] ratio** | | **5/15 = 0.33** |

Script exit 0. Ratio 0.33 in a deliberately short block: the evidence is one 23-line method, and the value
is in what it *resolves* about three earlier blocks rather than in new surface area.

**Block type: EVIDENCE (§14 correction).**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | `formatBaseMessage()` arms for 5/6, 15/16 and `default` | `[CERT]` | `ModbusResponse.java:68-90` read in full |
| 2 | `default` writes `byteCount` unconditionally as one byte | `[CERT]` | `:84` |
| 3 | `data` is written only when `(functionCode & 128) == 0` | `[CERT]` | `:85-86` |
| 4 | `exceptionCode` is an in-memory field, used by `isError()` and the debug string | `[CERT]` | `:14, 93-95, 253` — and absent from `formatBaseMessage()` |
| 5 | Receiver reads the exception type at `ibuf[8]` | `[CERT]` | `ModbusTcpRxDriver.java:171-173` ([Block 305] §305.4) |
| 6 | Frame offsets place `byteCount` exactly at `ibuf[8]` | `[INFER]` | arithmetic over claims 1–2 and the 6-byte split of [Block 305] §305.4 |
| 7 | The two slave values are 1 and 2 | `[CERT]` | `ModbusUnsolicitedReceive.java:144-146, 207-208` ([Block 303] §303.5) |
| 8 | 1 = illegalFunction, 2 = illegalDataAddress | `[CERT]` | `BCommStatusEnum.java:36-37` ([Block 300] §300.1) |

Tokens grep-confirmed in their cited source: **8 / 8**. Claim 4 is an ABSENCE (`exceptionCode` never
serialised) verified by reading the whole of `formatBaseMessage()` plus the write path above it `:55-66`,
not by grep. The emitter/receiver agreement (claims 2 + 5 + 6) is an independent cross-derivation: the two
files were read in different iterations, for different gaps, before this block connected them.

No new sources preserved. Model tier: **no delegation — inline**.

## 307.x — Connections

- **[Block 303]** — **CORRECTED**: its §303.5 "inconsistency" is resolved here; the two values are correct exception codes.
- **[Block 305]** — its §305.4 receive-side finding is the independent half of the cross-check.
- **[Block 131]** — §131.4's PDU layout for normal responses; this block adds the exception-frame variant.
- **[Block 300]** — the code vocabulary; §307.4 joins its two numbering spaces to the wire.

**Gaps opened by this block**: none. **M19 closed.**
