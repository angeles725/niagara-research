# Block 303 — Serving a request as a slave: the function-code dispatcher, which FCs Niagara answers, and the FC 23 handler that exists but is never reached

> Focus **modbus**, gap **M16** (opened by [Block 298] as M4-a; also the landing place of M4-b re-scoped by
> [Block 302]). What happens when a foreign master sends a PDU to a Niagara slave: how the frame is routed
> by function code, which codes are served and which are refused, how a read is answered from the in-memory
> maps of [Block 298], and how the exception response is built. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/` (Vineflower) —
> `modbusTcpSlave-rt/…/comm/ModbusUnsolicitedReceive.java` (338 lines, jar `8d78f0a5…`),
> `modbusSlave-rt/…/comm/ModbusUnsolicitedReceive.java` (326 lines, jar `5c18467c…`),
> `modbusCore-rt/server/messages/` (7 classes, jar `a0b67420…`).
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 298] (the four maps this reads from),
> [Block 299] (the client-side FC selection this answers), [Block 300] (the exception vocabulary),
> [Block 131] (the PDU layout on the wire).

---

## 303.1 — One class, one switch, per transport `[CERT]`

Both slave transports carry a class literally named `ModbusUnsolicitedReceive` — "unsolicited" because from
the station's point of view an incoming master request is unprompted `[CERT]`
(`modbusTcpSlave-rt/…/comm/ModbusUnsolicitedReceive.java`, 338 lines;
`modbusSlave-rt/…/comm/ModbusUnsolicitedReceive.java`, 326 lines).

The TCP one routes as follows `[CERT]` `modbusTcpSlave-rt/…/ModbusUnsolicitedReceive.java:100-156`:

1. take the raw frame; `deviceAddr = newMessage[0] & 255`;
2. `findModbusDevice(deviceAddr)` — if null, log at trace and drop `[CERT]` `:155`;
3. `device.incrementRequest()`;
4. if the device status is **disabled**, log at trace and drop `[CERT]` `:151-153`;
5. otherwise `switch (newMessage[1])` — the function code.

`[INFER]` two silent-drop paths (unknown address, disabled device) are both trace-gated, so on a production
station with trace off a master addressing the wrong unit id gets **no response and no log entry** — which
from the master's side is indistinguishable from a dead bus.

## 303.2 — What Niagara answers as a slave `[CERT]`

The switch arms `[CERT]` `modbusTcpSlave-rt/…/ModbusUnsolicitedReceive.java:112-156`:

| FC | Arm | Handler |
|---|---|---|
| **1, 2, 3, 4** | read | `ModbusServerReadRequest` → `processReadRequest()` |
| **5, 6, 15, 16** | write | `ModbusServerWriteRequest` → `processWriteRequest()` |
| **20** | read file record | `ModbusServerReadFileRequest` → `processReadFileRequest()` |
| **21** | write file record | `ModbusServerWriteFileRequest` → `processWriteFileRequest()` |
| **7–14, 17, 18, 19, `default`** | *refused* | synthesised exception response |

`[INFER]` so the slave's answer set is exactly the client's request set of [Block 295] §295.2 and
[Block 299] §299.1 — reads 1–4, writes 5/6/15/16, files 20/21 — which is what makes a Niagara-to-Niagara
Modbus pairing work symmetrically. Everything else on the wire is refused, including the diagnostics codes
(FC 7 Read Exception Status, FC 8 Diagnostics, FC 11/12 counters) that many field masters probe with.

The serial dispatcher has the same arms — `case 15/16` at `:122-123`, `case 20/21` at `:147-151` `[CERT]`
`modbusSlave-rt/…/ModbusUnsolicitedReceive.java`. `[INFER]` the two files are near-duplicates differing
only in session/transport handling, continuing the copy-paste pattern already recorded in [Block 295]
§295.3 and [Block 298] §298.6.

## 303.3 — FC 23 has a handler class that nothing calls `[CERT]`

`ModbusServerWriteReadRequest` exists in `modbusCore-rt/…/server/messages/` (32 lines) `[CERT]`. It is
**not imported or referenced by either dispatcher** `[CERT]` — `rg -ln 'ModbusServerWriteReadRequest'` over
`modbusCore-rt`, `modbusSlave-rt` and `modbusTcpSlave-rt` returns the declaring file only, and neither
switch has a `case 23`.

`[INFER]` FC 23 (Read/Write Multiple Registers) is therefore **dead code on the server side**: the message
class was written, but no dispatch arm reaches it, so a master issuing FC 23 to a Niagara slave falls into
`default` and receives an exception. Note the asymmetry with the client side, where [Block 131] §131.2
found `ModbusFC23Request` implemented as an outgoing request — **Niagara can send FC 23 but cannot answer
it**. Recorded as an absence derived by enumeration over all three jars, not a single grep.

## 303.4 — A read is answered straight from the maps `[CERT]`

`processReadRequest()` `[CERT]` `modbusTcpSlave-rt/…/ModbusUnsolicitedReceive.java:175-216` copies the
request's device address, function code and transaction identifier onto the response, then switches again:

| FC | Call |
|---|---|
| 1 | `device.getCoilStatusValues(address, numberPoints)` |
| 2 | `device.getInputStatusValues(...)` |
| 3 | `device.getHoldingRegisterValues(...)` |
| 4 | `device.getInputRegisterValues(...)` |

`[INFER]` these are the four `IntHashMap`s of [Block 298] §298.1 read directly — no point-tree traversal, no
device round trip. That is why a Niagara slave can answer at wire speed regardless of how many station
points feed the map: serving is O(range), not O(points). It also confirms [Block 302] §302.6's re-scoping —
the station's own points must have written into the maps *beforehand*; serving never pulls from them.

A `ModbusException` from any of the four is caught and converted into the error path `[CERT]` `:203-205`.
`[INFER]` since [Block 298] §298.2 showed membership is the `containsAddress` range test, an out-of-range
address is precisely what raises it — i.e. **Modbus exception 02 (illegal data address) is what a master
sees when it asks for an address outside the declared `valid*Range`**.

## 303.5 — The exception response, and an inconsistency worth flagging `[CERT]`

Both refusal paths set the high bit of the function code, the standard Modbus exception marker `[CERT]`:

```java
resp.functionCode = (byte)(resp.functionCode | 128);   // 0x80
```

at `:144` (unsupported FC) and `:206` (read failure), and at three more sites in the serial twin `[CERT]`
`modbusSlave-rt/…/ModbusUnsolicitedReceive.java:205, 249, 276, 307`.

But the two TCP paths disagree on `byteCount` while both send an empty `data` array `[CERT]`:

| Path | `byteCount` | `data` |
|---|---|---|
| unsupported FC (`:144-146`) | **1** | `new byte[0]` |
| read failure (`:207-208`) | **2** | `new byte[0]` |

`[INFER]` one of the two is wrong, or `byteCount` carries a different meaning on an exception frame than on
a data frame — a standard Modbus exception response has a one-byte payload (the exception code), and neither
path writes an exception code into `data` at all. Resolving which byte actually reaches the wire requires
reading `ModbusResponse`'s serialiser, which belongs to [Block 131]'s wire scope and was not opened here.
Flagged rather than guessed; see the gap below.

The unsupported-FC path also sets `resp.setResponseExpected(false)` `[CERT]` `:142`.

## 303.6 — What this closes, and what it does not

- **M4-a** — closed: the dispatch, the served FC set, the read path and the exception shape are documented.
- **M4-b** (re-scoped here by [Block 302] §302.6) — **not closed**: how station points push values *into*
  the four maps is a write-through on `BModbusServerProxyExt`, and this block only established that serving
  reads the maps and never the points. Left open as **M18**, honestly, rather than declared covered.
- The **file-record arms** (FC 20/21) were located but their PDU handling was not decoded — that is
  **M17**, already open from [Block 299].

## 303.7 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 31 | 30 |
| `[CERT-doc]` | 1 | 1 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 8 | 7 |
| **[INFER]/[CERT*] ratio** | | **7/31 = 0.23** |

Script exit 0. (The single `[CERT-doc]` the script counts is the sentence below naming the marker, not a
citation.)

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | Dispatch order: address → find device → increment → disabled check → FC switch | `[CERT]` | `modbusTcpSlave-rt/…:100-156` read in full |
| 2 | Both drop paths are trace-gated | `[CERT]` | `:151-155` |
| 3 | The exact switch arms 1-4 / 5,6,15,16 / 20 / 21 / default | `[CERT]` | every `case` label enumerated at `:112-156` |
| 4 | The serial twin has the same arms | `[CERT]` | `modbusSlave-rt/…:122-123, 147-151` |
| 5 | `ModbusServerWriteReadRequest` is referenced only by its own file | `[CERT]` | `rg -ln` over all three jars → one hit (ABSENCE, full-scope enumeration) |
| 6 | Neither switch has a `case 23` | `[CERT]` | case labels enumerated in both dispatchers |
| 7 | Niagara *sends* FC 23 (`ModbusFC23Request` exists client-side) | `[CERT]` | file present in `modbusCore-rt/messages/`; cross-ref [Block 131] §131.2 |
| 8 | Read arms call the four map getters | `[CERT]` | `:175-216` |
| 9 | `ModbusException` → error path | `[CERT]` | `:203-205` |
| 10 | `functionCode \| 128` at both TCP sites and four serial sites | `[CERT]` | `:144`, `:206`; `modbusSlave-rt/…:205,249,276,307` |
| 11 | `byteCount` 1 vs 2 with empty `data` in the two paths | `[CERT]` | `:144-146` and `:207-208` read verbatim |

Tokens grep-confirmed in their cited source: **11 / 11**. Claims 5 and 6 are ABSENCES, both derived by
enumeration over the complete relevant scope (three jars; every case label in both dispatchers) rather than
a spot check. Claim 11 is reported as an observation with its interpretation explicitly **deferred** — the
serialiser was not read, so no conclusion about the wire bytes is asserted.

No new sources preserved. `[CERT-doc]`: none — the guide has no topic on slave request handling.
Model tier: **no delegation — inline**.

## 303.x — Connections

- **[Block 298]** — the four maps read here; `containsAddress` is what makes an out-of-range read raise.
- **[Block 299]** — the client's FC selection; the slave answers exactly that set.
- **[Block 302]** — its re-scoping of M4-b lands here and is passed on to M18.
- **[Block 131]** — §131.2's FC inventory, including the client-side `ModbusFC23Request` that has no server twin; §303.5's unresolved `byteCount` belongs to its serialiser.
- **[Block 300]** — exception 02 (illegal data address) is the master-visible result of §303.4's range failure.

**Gaps opened by this block**:
- **M18** — server **write-through**: how `BModbusServerProxyExt` and the station's own points populate the
  four `IntHashMap`s, and what happens when a master writes a coil that a station point also drives
  (last-writer-wins, or is the point authoritative?) → **new medium gap**, carrying M4-b forward.
- **M19** — the exception-response byte layout (§303.5): what `byteCount` means on a frame whose function
  code has bit 7 set, and where the Modbus exception code is actually written → **new low gap**, resolvable
  by reading `ModbusResponse`'s serialiser alongside [Block 131] §131.4.
