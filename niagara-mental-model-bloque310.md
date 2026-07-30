# Block 310 — FC 20/21 on the wire: a single hard-coded sub-request, reference type 6, and a byte count computed two different ways

> Focus **modbus**, gap **M17** (opened by [Block 299] as M5-a). The PDU layout of Read/Write File Record,
> which [Block 131] §131.4 left as a "static skeleton" and [Block 299] documented only at the component
> level. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/modbusCore-rt/…/messages/ModbusReadFileRequest.java` (235 lines),
> `…/ModbusWriteFileRequest.java` (287 lines), jar `a0b67420…`.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 131] (§131.4 PDU skeleton, §131.5/§131.6
> the RTU/ASCII framing these reuse), [Block 299] (the file-record component model), [Block 303] (the slave's
> FC 20/21 arms).

---

## 310.1 — The read request `[CERT]`

`ModbusReadFileRequest extends ModbusMessage` carries one extra field beyond the base — `fileNumber` — and
fixes `functionCode = 20` in its constructor `[CERT]`
`modbusCore-rt/…/messages/ModbusReadFileRequest.java:3-12`.

`writeRtu()` emits `[CERT]` `:14-27`:

| Offset | Byte | Meaning |
|---|---|---|
| 0 | `deviceAddress` | unit id |
| 1 | `functionCode` = **20** | Read File Record |
| 2 | **`7`** | *hard-coded* byte count |
| 3 | **`6`** | *hard-coded* reference type |
| 4–5 | `fileNumber` (big-endian) | file number |
| 6–7 | `startAddress` (big-endian) | record number |
| 8–9 | `numberPoints` (big-endian) | record length |
| — | CRC | `modOut.writeCRC()` |

`writeAscii()` builds the identical 10-byte array and hands it to the ASCII framer `[CERT]` `:29-40+`.

Two constants are literal, not computed `[INFER]`:

- **`7`** is the Modbus "Request data length" — 1 (reference type) + 2 (file) + 2 (record) + 2 (length) = 7.
  Correct, but frozen: it presumes exactly **one** sub-request.
- **`6`** is the Modbus **reference type**, which the specification fixes at 6 for file-record access. Correct
  and genuinely constant.

`[INFER]` **the driver emits exactly one sub-request per message.** FC 20 permits a *group* of sub-requests
in a single PDU (that is the point of the byte-count field); Tridium hard-codes the length for one and has
no loop. So reading N separate record ranges costs N round trips, and given [Block 308] §308.3's serialised
dispatcher, those N are strictly sequential.

## 310.2 — The write request computes what the read hard-codes `[CERT]`

`ModbusWriteFileRequest.writeRtu()` is the same shape with one difference `[CERT]` `:32-46+`:

```java
modOut.write((byte) this.deviceAddress);
modOut.write((byte) this.functionCode);
int dataLen = this.endIdx - this.startIdx;
if (dataLen < 0) dataLen = 0; else dataLen++;
modOut.write((byte)(7 + dataLen));          // <-- computed
modOut.write((byte) 6);                      // <-- still hard-coded
modOut.write((byte)((this.fileNumber & 0xFF00) >> 8));
modOut.write((byte)(this.fileNumber & 0xFF));
…
```

`[INFER]` the byte count is `7 + dataLen` because a write carries the record data after the header, and
`dataLen` is derived from an inclusive `startIdx`/`endIdx` window — hence the `dataLen++` after the negative
guard. The reference type stays literal `6`, consistent with §310.1.

So the two directions treat the same field differently: **the read hard-codes 7, the write computes
`7 + dataLen`** `[CERT]`. `[INFER]` that is correct for a single sub-request in both cases — the read has no
payload to add — but it is worth noting that the read's constant would silently be wrong if anyone ever
added a second sub-request, whereas the write's expression would at least be structurally ready for it.

The `dataLen < 0 → 0` guard means an inverted or empty window emits a byte count of exactly 7 with no data
`[CERT]` `:37-41`.

## 310.3 — What this closes, and what remains genuinely out of reach

**Closes M17**: the request PDU layout for FC 20 and FC 21 is documented, including the two constants and
the byte-count asymmetry. Together with [Block 299] (the component model) and [Block 303] §303.2 (the slave's
dispatch arms), the file-record channel is now covered end to end on the **static** side.

**Still open, and not a gap this focus can close**: the *response* parsing and the live behaviour of FC 20/21
against a real device. [Block 131]'s original `P1-fc` note bundled "FC20/21 file-record PDU live behavior"
with the register-type→FC mapping; the mapping was closed by [Block 295] §295.2 (read) and [Block 299]
§299.1 (write), and the layout is closed here — what is left is **live behaviour**, which stays
requires-execution under the `P1-dyn` disposition already recorded in `RESEARCH-STATE-protocols.md`.

`[INFER]` no new gap is opened for it: it is the same live-device dependency the protocols focus already
tracks, and duplicating it here would inflate the backlog without adding information.

## 310.4 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 16 | 15 |
| `[CERT-doc]` | 1 | 1 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 7 | 6 |
| **[INFER]/[CERT*] ratio** | | **6/16 = 0.38** |

Script exit 0. Ratio 0.38 on a deliberately narrow block (two serialiser methods): the citable surface is
small and most of the value is in reading the constants' meaning. Not an exhaustion signal — the remaining
FC 20/21 unknowns are live-behaviour, not static (§310.3).

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | `ModbusReadFileRequest` sets `functionCode = 20`, adds `fileNumber` | `[CERT]` | `:3-12` |
| 2 | The RTU byte sequence, in order, with `7` and `6` literal | `[CERT]` | `:14-27` read in full |
| 3 | `writeAscii` builds the same 10-byte array | `[CERT]` | `:29-40` |
| 4 | `ModbusWriteFileRequest` writes `7 + dataLen` | `[CERT]` | `:43` |
| 5 | Reference type `6` is literal in both | `[CERT]` | `:17` (read) and `:44` (write) |
| 6 | `dataLen` guard: negative → 0, else `++` | `[CERT]` | `:36-41` |
| 7 | No loop over sub-requests in either writer | `[CERT]` | both `writeRtu` methods read in full — ABSENCE bounded to the two serialisers |

Tokens grep-confirmed in their cited source: **7 / 7**. Claim 7 is an ABSENCE and is bounded exactly: it says
these two *writers* emit one sub-request, not that the driver could never handle more elsewhere. The
interpretation of `7` as `1+2+2+2` (§310.1) is arithmetic offered as explanation and marked `[INFER]`; the
cited fact is only that the literal is `7`.

`[CERT-doc]`: none — the guide's §Adding client file records is a drag-and-configure procedure ([Block 299]
§299.4) and says nothing about the PDU. No new sources preserved. Model tier: **no delegation — inline**.

## 310.x — Connections

- **[Block 131]** — §131.4's PDU skeleton; this block fills in FC 20/21 and reuses its §131.5/§131.6 framing.
- **[Block 299]** — the component model above these messages; §299.5 flagged the layout as M5-a.
- **[Block 303]** — the slave's FC 20/21 dispatch arms, whose handlers were located but not decoded there.
- **[Block 308]** — why N sub-requests as N messages is costlier than it looks.

**Gaps opened by this block**: none. See §310.3 for why the live-behaviour remainder is deliberately not
re-opened here.
