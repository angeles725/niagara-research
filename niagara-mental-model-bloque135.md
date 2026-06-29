# Block 135 — LON network-variable wire encoding: the SNVT byte serialization, the resolution/offset scaling, struct/bitfield packing, the LonTalk Network-Interface application buffer (NI command codes + ExpAppMessage header), the network-variable update message, and the LonMark file-transfer framing

> Research of the **Niagara N4 LonWorks (ANSI/CEA-709.1 LonTalk) protocol at the encoding level** as actually
> implemented in the shipped Java runtime jar — the bytes a network-variable update, a network-management
> command, and a file transfer put into the host↔network-interface buffer, NOT the device-model / config
> view. The LON CONFIG/INTEGRATION view (device model, XIF/LNML import, SNVT type catalog, ProgramId,
> ShortStack file transfer at the platform level) is already covered by B19/B77/B120; the native driver pipe
> (`ldvProxy`) by B127. This block documents the layers those abstractions serialize INTO: the **byte
> primitives** (big-endian integers, IEEE-754 floats, bit-field packing), the **SNVT value scaling** (the
> `resolution`/`offset` fixed-point transform and its `scaleA·10^scaleB` provenance from the SNVT master
> resource file), **struct + bitfield packing** of multi-element SNVTs/SCPTs, the **`UnprocessedNV`
> network-variable update message** (NV-selector + direction encoding), the **LonTalk Network-Interface (NI)
> application buffer** (`NAppBuffer` — the Echelon MIP/SLTA command byte + queue + ExpAppMessage header +
> 11-byte address + message), the **explicit network-management message codes**, and the **LonMark file
> transfer** (FTP over NVs + explicit `FileXferData` windowed packets). The physical layer (TP/FT-10
> 78.125 kbaud differential-Manchester line code + L2 CRC) is BELOW the Java jar — it lives in the native
> `ldv` adapter driver (B127) — and is registered here as a requires-execution/hardware gap, not invented.
> READ-ONLY. Corpus language: ENGLISH.
>
> Source (primary, decompiled with Vineflower from the live install):
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/lonworks-rt.jar` (sha256 defd41b1…) — 422 classes
> under `javax.baja.lonworks.*` and `com.tridium.lonworks.*`. Preserved under
> `sources/decompiled/lonworks-rt/` and registered in `sources/SOURCES.md`.
> Method: `decompile-java.sh` (Vineflower) + targeted reading of `javax.baja.lonworks.{io,londata,proxy}`,
> `com.tridium.lonworks.{loncomm,netmessages,file,resource,util}` + `grep` token confirmation of every
> literal byte/constant.
> Markers:
> `[CERT]` local primary source (`file:line`) · `[CERT-doc]` downloaded doc · `[CERT-web]` official web ·
> `[CERT-a]` secondary/forum · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — wire-level focus). Connects [Block 19] / [Block 77] / [Block 120]
> (LON device model, XIF/LNML, SNVT catalog, ShortStack file transfer — the CONFIG level) and [Block 127]
> (`ldvProxy` native driver pipe — the layer below this one), plus [Block 131] / [Block 132] / [Block 133] /
> [Block 134] (sibling wire-level protocol blocks — Modbus, OPC, BACnet, Fox).

---

## 135.1 — Module map: where each LON wire layer lives `[CERT]`

The entire LON codec is Tridium-written inside `lonworks-rt.jar`; it stops at the **native `ldv` (LonWorks
device) driver boundary** (`com.tridium.platLon.BLonPlatformService`, the JNI bridge in `platLon-rt.jar`,
cross-ref B127). The layers separate cleanly:

| Wire layer | Package | Key classes |
|---|---|---|
| Byte primitives (BE ints, IEEE floats, bitfields) | `javax.baja.lonworks.io` | `LonOutputStream`, `LonInputStream` |
| SNVT value (de)serialization + scaling | `javax.baja.lonworks.londata` | `BLonPrimitive`, `BLonFloat`, `BLonElementQualifiers`, `LonFacetsUtil` |
| Struct / bitfield packing (multi-element SNVT/SCPT) | `javax.baja.lonworks.londata` | `BLonData` (`toOutputStream`/`fromInputStream`), `BLonFileReq`, `BLonFilePos` |
| SNVT scale provenance (master resource file) | `com.tridium.lonworks.resource` | `TypeScalar`, `TypeScalar64`, `ResourceToXLon` |
| NV update message (implicit NV msg) | `com.tridium.lonworks.netmessages` | `UnprocessedNV`, `FetchNvRequest`/`FetchNvResponse` |
| Explicit net-management messages + codes | `com.tridium.lonworks.netmessages` | `NetMessages` (code constants), `Query*`/`Update*` |
| NI application buffer (host↔NI command framing) | `com.tridium.lonworks.loncomm` | `NAppBuffer`, `NLonComm`, `LonTransactionManager` |
| Native link layer (to the `ldv` adapter) | `com.tridium.lonworks.loncomm` | `NLonLinkLayer` (→ `BLonPlatformService.driverInit/read/write`) |
| LonMark file transfer (FTP over NV) | `com.tridium.lonworks.file` | `LonFileTransfer`, `FileXferData`, `FileXferResponse` |

The on-the-wire nesting is `TP/FT-10 line (native, B127) ▸ LonTalk L2/L3 (native NI) ▸ NI application buffer
(NAppBuffer) ▸ ExpAppMessage / NV message ▸ SNVT value bytes`. §135.2–§135.5 build the SNVT value bytes;
§135.6 wraps them in the NV update message; §135.7–§135.8 wrap that in the NI application buffer; §135.9 is
the native handoff; §135.10 is file transfer; §135.11 is the hardware gap.

## 135.2 — Byte primitives: big-endian integers, IEEE-754, and bit-field packing `[CERT]`

All multi-byte LON scalars are serialized **big-endian (high byte first / network order)** by
`LonOutputStream`, the exact opposite of Modbus-RTU's CRC quirk (B131) and of LON's own little-endian
*physical* convention (handled natively, §135.11). `writeSigned16`/`writeUnsigned16` emit
`highByte = l >> 8 & 0xFF` then `lowByte` `[CERT]` `LonOutputStream.java:86-98`; `writeSigned32` emits bytes
24→16→8→0 `[CERT]` `LonOutputStream.java:108-113`; `writeSigned64` bytes 56→…→0 `[CERT]`
`LonOutputStream.java:122-131`. Floats are raw **IEEE-754** big-endian: `writeFloat` does
`Float.floatToIntBits(value)` then the four bytes high-first `[CERT]` `LonOutputStream.java:155-161`,
`writeDouble` the eight `[CERT]` `LonOutputStream.java:163-173`. `LonInputStream` is the exact inverse —
`readUnsigned16 = highByte<<8 | lowByte` `[CERT]` `LonInputStream.java:56-60`, `readFloat =
Float.intBitsToFloat(readSigned32())` `[CERT]` `LonInputStream.java:172-174`.

**Bit-field packing** (for SNVT/SCPT struct members narrower than a byte) is offset-addressed, not
stream-sequential. `writeBit(val, byteOffset, bitOffset, bitCount)` builds a `bitCount`-wide mask, then
**ORs `(val & mask) << bitOffset` into the byte at `bitFieldMark + byteOffset`** `[CERT]`
`LonOutputStream.java:57-76` — so bits are LSB-first within the struct's marked base byte. `readBit` mirrors
it (`read() >> bitOffset & mask`) and `readSignedBit` sign-extends from `bitCount-1` `[CERT]`
`LonInputStream.java:84-114`. `setBitFieldMark`/`resetBitFieldMark` save the struct's base offset so nested
structs pack relative to their own start `[CERT]` `LonOutputStream.java:175-183`. `writeByteArray(a, count)`
right-pads with `0` to a fixed length and `writeCharArray`/`writeString` NUL-terminate `[CERT]`
`LonOutputStream.java:21-51` (used for SNVT string members).

## 135.3 — SNVT value encoding: the resolution/offset fixed-point scaling `[CERT]`

A SNVT scalar's engineering value is converted to its raw on-wire integer by a **fixed-point transform with
two parameters — `resolution` and `offset`** — carried per-element in `BLonElementQualifiers`. The element
qualifier defaults are `resolution = 1.0`, `offset = 0.0` `[CERT]`
`BLonElementQualifiers.java:22-23`. `BLonFloat.toOutputStream` is the encode path `[CERT]`
`BLonFloat.java:172-223`:

```
if (qualifier has invalidValue && value is NaN) raw = invalidValue          // NaN → the SNVT "invalid" sentinel
else { if (offset != 0)      value += offset;                               // 1. add offset
       if (resolution != 1)  value /= resolution; }                         // 2. divide by resolution
ival = (int) value;                                                          // 3. truncate to integer
write ival per element type (u8/s8/u16/s16/s32/float/bit)  — big-endian (§135.2)
```

`[CERT]` `BLonFloat.java:175-185, 187-222`. The decode path `fromInputStream` is the exact inverse —
read the raw per element type, then **`value *= resolution; value -= offset`** `[CERT]`
`BLonFloat.java:226-277` (lines 267-273), with the raw `== invalidValueL` sentinel mapping back to `NaN`
`[CERT]` `BLonFloat.java:264-265`. So the round-trip relationship is **`raw = (eng + offset) / resolution`**
and **`eng = raw·resolution − offset`** — note Tridium's sign convention: offset is *added* before dividing
on encode and *subtracted* after multiplying on decode.

The element type → wire width mapping (the `getOrdinal()` switch) `[CERT]` `BLonFloat.java:188-222` /
`BLonElementQualifiers.java:203-233`:

| Ordinal | Element type | Wire width | Encode call |
|---|---|---|---|
| 0 / 2 | `u8` / unsigned byte | 1 B | `writeUnsigned8` |
| 1 | `s8` | 1 B | `writeSigned8` |
| 3 | `s16` | 2 B BE | `writeSigned16` |
| 4 | `u16` | 2 B BE | `writeUnsigned16` |
| 5 / 16 | `s32` | 4 B BE | `writeSigned32` |
| 8 | `f` (IEEE float) | 4 B BE | `writeFloat` |
| 12 | unsigned bitfield | bits | `writeBit` (byte/bit offset, size) |
| 13 | signed bitfield | bits | `writeSignedBit` |
| 17/18/19 | 64-bit | 8 B BE | (via `BLonLong`/`BLonDouble`) |

`[CERT]` `BLonElementQualifiers.java:203-233` (byte-length table). The `min`/`max`/`invalidValue` are
clamped/validated in facets but the SNVT *wire* bytes are purely the scaled integer above. This is the
encoding-grade complement to B19/B77's config-level SNVT type catalog.

## 135.4 — SNVT scale provenance: `resolution = scaleA · 10^scaleB` from the SNVT master resource file `[CERT]`

The `resolution`/`offset` used in §135.3 are NOT invented by Tridium — they are derived from the **standard
LonMark SNVT scaling triple (scaleA, scaleB, scaleC)** read out of the binary SNVT **master resource file**
(`standard.typ`/`.fmt` resource set). `TypeScalar` parses each scalar type's range+scale record `[CERT]`
`TypeScalar.java:31-151`: a `rangeScaleControl` bitmask byte selects which fields are present
(`SA_BIT=4`, `SB_BIT=2`, `SC_BIT=1`) `[CERT]` `TypeScalar.java:11-13`, then `readScale` reads each present
factor as a **signed 16-bit** value `[CERT]` `TypeScalar.java:139-151` (`scaleA/scaleB/scaleC =
in.readSigned16()`), defaulting `scaleA=1, scaleB=0, scaleC=0` `[CERT]` `TypeScalar.java:17-19`.

The conversion of that triple into the §135.3 `resolution` is the **canonical LonMark formula**
`resolution = scaleA · 10^scaleB`, computed verbatim in two places `[CERT]`:
`TypeScalar64.java:41` — `float res = (float)(this.scaleA * Math.pow(10.0, this.scaleB))` — and
`ResourceToXLon.java:475` — `float res = (float)(node.scaleA * Math.pow(10.0, node.scaleB))`. (Worked
example, `[INFER]` from the formula: SNVT_temp uses scaleA=1, scaleB=−1, scaleC=−274 → resolution=0.1, so a
wire raw of `500` decodes to `500·0.1 − offset`; the precise offset/units come from the device's parsed
qualifier, not a literal in this jar.) The `invalid`/min/max possible values per node type are also
table-driven `[CERT]` `TypeScalar.java:55-109, 153-183`. This closes the gap between B19/B77's *named* SNVT
catalog and the *numeric* transform applied to the bytes.

## 135.5 — Struct + bitfield packing: multi-element SNVTs/SCPTs `[CERT]`

A multi-field SNVT/SCPT (e.g. `SNVT_switch = {value, state}`, or config structs) is a `BLonData` — a
`BVector` of `BLonPrimitive` slots. `BLonData.toNetBytes()` = `toOutputStream` into a `LonOutputStream`
`[CERT]` `BLonData.java:391-395`, which **iterates the active properties in slot order**, writing each
primitive and recursing into nested `BLonData` with a fresh bit-field mark `[CERT]` `BLonData.java:397-411`:

```
for each active prop:
   if primitive:  primitiveToOutputStream(prop, out)
   if nested BLonData: setBitFieldMark(); child.toOutputStream(out); resetBitFieldMark()
```

`primitiveToOutputStream` honors an explicit **byte offset** from the qualifier — if `byteOffset > 0` it
`out.setPosition(byteOffset)` (zero-padding the gap) before writing `[CERT]` `BLonData.java:413-421`, and
the symmetric read does `in.reset(byteOffset)` `[CERT]` `BLonData.java:447-464`. So struct layout = slot
order + optional absolute byte offsets + LSB-first bitfields within a marked base byte (§135.2). A concrete
struct in this jar is `BLonFileReq` (§135.10): slots `request` (e8, 1 B), `index` (u16, 2 B),
`recvTimeout` (u16), `address` (na, 4 B), `authenticate`/`priority` (bool) — packed in exactly that order
`[CERT]` `BLonFileReq.java:47-52`. The cached `byteLength` is just `toNetBytes().length` `[CERT]`
`BLonData.java:383-389`.

## 135.6 — The network-variable update message: `UnprocessedNV` (NV-bit + direction + 14-bit selector) `[CERT]`

An implicit network-variable update — the most common LON message — is `UnprocessedNV`. Its on-wire layout
is a **2-byte header + up to 31 SNVT data bytes** `[CERT]` `UnprocessedNV.java:13-42, 82-84`:

| Byte | Bits | Meaning | Cite |
|---|---|---|---|
| `msgData[0]` | bit 7 (`0x80`) | **NETVAR bit** — always set: marks this as an NV message vs an explicit message | `UnprocessedNV.java:30` |
| | bit 6 (`0x40`) | **direction** — 0 = input (`nvi`), 1 = output (`nvo`) | `UnprocessedNV.java:31-35, 44-45` |
| | bits 5-0 | upper 6 bits of NV selector: `(selector & 0x3F00) >> 8` | `UnprocessedNV.java:37` |
| `msgData[1]` | bits 7-0 | lower 8 bits of NV selector: `selector & 0xFF` | `UnprocessedNV.java:38` |
| `msgData[2..]` | | the SNVT value bytes (§135.3-§135.5), `MAX_NETVAR_DATA = 31` per LonTalk | `UnprocessedNV.java:19, 39` |

So the **NV selector is a 14-bit field** (`UPPER_SEL_MASK = 16128 = 0x3F00`, `LOWER_SEL_MASK = 0xFF`)
`[CERT]` `UnprocessedNV.java:11-13`, sharing byte 0 with the NV/direction flags — i.e. the high byte's top
two bits are flags and the bottom six are selector high bits. `getNvSelector` reassembles
`(msgData[0]<<8 & 0x3F00) | (msgData[1] & 0xFF)` `[CERT]` `UnprocessedNV.java:56-59`. A poll (NV read) sends
an `UnprocessedNV` with empty data and the **reversed direction**, the response carries the value `[CERT]`
`NmUtil.java:1052-1060`. The write path `NmUtil.setNvValue` builds the `UnprocessedNV(direction, selector,
data)` and hands it to an `NAppBuffer` (§135.7) with the configured service type `[CERT]`
`NmUtil.java:1100-1124`.

## 135.7 — The LonTalk Network-Interface application buffer: `NAppBuffer` (Echelon MIP/SLTA command framing) `[CERT]`

Every outbound message — NV update or explicit — is marshalled into a **fixed 16-byte-header application
buffer** that matches the Echelon **MIP/SLTA host↔network-interface** layout. `NAppBuffer` carries a
`byte[255]` and exposes the fields as bit-addressed accessors `[CERT]` `NAppBuffer.java:88, 22-99`:

**Byte 0 — NI command (high nibble `0xF0`) + queue (low nibble `0x0F`)** `[CERT]`
`NAppBuffer.java:115-131, 42-43`. The NI command codes (the standard MIP "niCMD" set) `[CERT]`
`NAppBuffer.java:23-41`:

| Const | Value | Role |
|---|---|---|
| `niTQ` / `niTQ_P` | 2 / 3 | transaction (priority) queue |
| `niNTQ` / `niNTQ_P` | 4 / 5 | non-transaction (priority) queue |
| `niRESPONSE` | 6 | response queue |
| `niINCOMING` | 8 | incoming-message queue |
| `niCOMM` | 16 (`0x10`) | comm / app message command |
| `niNETMGMT` | 32 (`0x20`) | local network-management command |
| `niRESET` | 80 (`0x50`) | reset the NI |
| `niFLUSH_CANCEL`/`niFLUSH_COMPLETE` | 96 | flush control |
| `niONLINE` / `niOFFLINE` | 112 / 128 | node online/offline |
| `niSLEEP` | 176 | sleep |
| `niSSTATUS` / `niIRQENA` / `niSERVICE` | 224 / 229 / 230 | status / IRQ-enable / service |

`setDestAddress` chooses the command: a remote subnet/node or Neuron-ID address → `niCOMM=16`, a
local-device address → `niNETMGMT=32` `[CERT]` `NAppBuffer.java:325-356, 449-451`.

**Byte 1** = buffer length; **byte 4** = data length `[CERT]` `NAppBuffer.java:142-148, 242-248`.

**Bytes 2-3 — the ExpAppMessage header** `[CERT]` `NAppBuffer.java:44-59, 150-323`:

| Byte.bit | Field | Cite |
|---|---|---|
| `2` bit7 (`0x80`) | NETVAR bit (NV vs explicit) | `NAppBuffer.java:46, 150-152` |
| `2` bits6-5 (`0x60`) | service type (0=acked,1=unackedRpt,2=unacked,3=request per `BLonServiceType`) | `NAppBuffer.java:57, 250-275` |
| `2` bit6 (`0x40`) as poll | poll bit | `NAppBuffer.java:301-311` |
| `2` bit4 (`0x10`) | **AUTH bit** (authenticated transaction) | `NAppBuffer.java:47, 277-287` |
| `2` bits3-0 (`0x0F`) | transaction tag | `NAppBuffer.java:58, 154-161` |
| `3` bit7 (`0x80`) | priority | `NAppBuffer.java:48, 163-173` |
| `3` bit6 (`0x40`) | path | `NAppBuffer.java:49, 175-185` |
| `3` bits5-4 (`0x30`) | completion code (notComp/succeeds/fails) | `NAppBuffer.java:59, 187-200` |
| `3` bit3 (`0x08`) | explicit-address mode | `NAppBuffer.java:50, 202-216` |
| `3` bit2 (`0x04`) | alt-path / turnaround | `NAppBuffer.java:51-52, 289-323` |
| `3` bit1 (`0x02`) | pool | `NAppBuffer.java:53, 218-228` |
| `3` bit0 (`0x01`) | response | `NAppBuffer.java:54, 230-240` |

**Bytes 5-15 — the 11-byte address block** (`ADDR_SIZE=11`, `ADDR_OFFSET=5`): type at byte 5, then
subnet/node (subnet at 9, node bits in 6), Neuron-ID (6 bytes at offset 10), or broadcast/domain `[CERT]`
`NAppBuffer.java:65-83, 325-356`. **Byte 16 onward — the message** (`MSG_OFFSET=16`,
`APP_BUFFER_HDR_LEN=16`): byte 16 is the message code, then the payload `[CERT]`
`NAppBuffer.java:84, 99, 480-482`. `setMessage` serializes the `LonMessage` into the buffer starting at 16
and sets `dataLength = out.size() − 16`; if the message is "far side" it prefixes `0x7E` (126) `[CERT]`
`NAppBuffer.java:484-518` (also `writeMessage` `NAppBuffer.java:606-614`). The NI on-wire length is
`16 + dataLength` for a message buffer, `2` for a bare local command `[CERT]` `NAppBuffer.java:596-604`.
Buffers are pooled (32 max) and zero-reset on reuse `[CERT]` `NAppBuffer.java:100-105, 660-690`.

`NLonComm` is the service API over this buffer — `sendRequest`/`sendAcked`/`sendUnacknowledged`/
`sendUnackRepeat` each build an `NAppBuffer`, set the matching `BLonServiceType`, and submit a transaction
`[CERT]` `NLonComm.java:57-204`; inbound buffers route to the **NV listener if byte-16 message code has bit
`0x80` set, else to explicit listeners** `[CERT]` `NLonComm.java:284-300` (`getMessageCode() & 128 == 128`).

## 135.8 — Explicit network-management message codes `[CERT]`

Explicit (non-NV) messages carry their command as the byte-16 message code. The full LonTalk
network-management code set is enumerated in `NetMessages` `[CERT]` `NetMessages.java:4-80` — requests
`0x51-0x7F`, successes `0x20|low`, failures `low`:

| Command | Request | Success | Failed | Cite |
|---|---|---|---|---|
| Query status | 81 (`0x51`) | 49 (`0x31`) | 17 | `NetMessages.java:4,8,12` |
| Query ID | 97 (`0x61`) | 33 (`0x21`) | 1 | `NetMessages.java:14,52,62` |
| Update domain | 99 | 35 | 3 | `NetMessages.java:16,…` |
| Update NV config | 107 | 43 | 11 | `NetMessages.java:24,…` |
| Read / write memory | 109 / 110 | 45 / 46 | 13 / 14 | `NetMessages.java` |
| Query SNVT | 114 | 50 | 18 | `NetMessages.java` |
| **NV fetch** | **115 (`0x73`)** | **51 (`0x33`)** | **19 (`0x13`)** | `NetMessages.java:35,57,78` |
| Service pin | 127 (`0x7F`) | — | — | `NetMessages.java` |

`FetchNvRequest` shows the live encoding `[CERT]` `FetchNvRequest.java:13-52`: write code `115`, then the NV
index as **1 byte, or the escape `0xFF` + a 2-byte BE index when `index > 254`** `[CERT]`
`FetchNvRequest.java:31-39`; the response is accepted only if its code is `51` (success), `19` → failure
`[CERT]` `FetchNvRequest.java:55-65`. The success-code pattern `success = 0x20 | (failed_low)` (e.g. NV
fetch `0x13`→`0x33`) is visible across the table `[INFER]` from the constants (Tridium does not state the
rule literally).

## 135.9 — The native boundary: link layer to the `ldv` adapter (cross-ref B127) `[CERT]`

The completed `NAppBuffer` does NOT go onto a Java socket — it is handed to the **native LON adapter driver**
through `BLonPlatformService` (the JNI bridge, `platLon-rt.jar`, cross-ref B127). `NLonLinkLayer.init`
resolves the platform service and opens the port: `ldvHandle = driver.driverInit(devName)` `[CERT]`
`NLonLinkLayer.java:54-60`. The transmit thread writes the raw buffer bytes:
`driver.write(ldvHandle, newMsg.getWriteBuffer(), newMsg.getWriteBufferLen())` `[CERT]`
`NLonLinkLayer.java:274` (also the shutdown `QueryStatusRequest` `NLonLinkLayer.java:144`); the receive
thread polls `driver.read(ldvHandle, netBytes)` into a fresh `NAppBuffer`'s 255-byte read buffer and applies
a length fixup (`if len>5 && netBytes[4]!=len-14 → netBytes[4]=len-14`) before dispatch `[CERT]`
`NLonLinkLayer.java:205-229`. Three threads (App/Rcv/Xmit) run the pipe `[CERT]` `NLonLinkLayer.java:106-122`.
**This is the exact handoff point B127 documented from the native side**: everything below `driver.write`/
`driver.read` (the `ldv`/`ldvProxy` pipe, the SLTA/FT-10 framing, L2 CRC, Manchester encoding) is native and
NOT in this jar (§135.11).

## 135.10 — LonMark file transfer: FTP-over-NV + windowed `FileXferData` packets `[CERT]`

LON file transfer (used for e.g. config-property download, cross-ref B120's platform ShortStack transfer) is
the **LonMark File Transfer Protocol**: a control plane over three NVs plus a data plane of explicit
windowed packets. `LonFileTransfer` drives it `[CERT]` `LonFileTransfer.java:35-786`:

- **Control NVs**: `SNVT_file_request` (`BLonFileReq` — request enum + file index + recvTimeout + requester
  address + auth/priority, §135.5) is written to the device's request-NV selector via `setNvValue` with
  **acked** service `[CERT]` `LonFileTransfer.java:541-550`; `SNVT_file_status` (`BLonFileStatus`) is polled
  via `fetchNv` to drive the state machine (statuses: idle/0, directory/1, transfer-underway/4,
  seek-wake/11, …) `[CERT]` `LonFileTransfer.java:512-535, 296-478`; optional `SNVT_file_pos`
  (`BLonFilePos` — pointer + length) for random access `[CERT]` `LonFileTransfer.java:563-568`.
- **Data plane — `FileXferData`, explicit message code 62 (`0x3E`)** `[CERT]` `FileXferData.java:15-24`.
  Each packet = `[code 0x3E][ (window<<4) | (packet & 0x0F) ][ ≤32 data bytes ]` `[CERT]`
  `FileXferData.java:50-56` (so window and packet are each 4 bits in one byte). The transfer is **windowed:
  32-byte data chunks (`MAX_DATA_LENGTH=32`), 6 packets per window** (`window = packet/6`, `pack =
  packet%6`, window masked to 4 bits) `[CERT]` `LonFileTransfer.java:38, 330-368`. Packets 0-4 of a window go
  **unacknowledged**; the 6th (or last) packet goes via **acked `sendRequest`** and the device replies
  `FileXferResponse` whose code = the next expected packet; if `!= 6` the sender rewinds to
  `window*6 + responseCode` (selective retransmit) `[CERT]` `LonFileTransfer.java:355-367`,
  `FileXferResponse.java:5-22`, `FileXferData.java:77-83`.
- **Receive side** registers a listener on message code 62, reassembles by (window, packet), and acks each
  window boundary with `FileXferResponse(expectedPacket)`; a sub-32-byte packet terminates the transfer
  `[CERT]` `LonFileTransfer.java:651-721`. This is the encoding-grade detail behind B120's config-level
  ShortStack file-transfer description.

## 135.11 — What is NOT in the Java jar: the TP/FT-10 physical layer (requires-execution / hardware) `[CERT]`/`[INFER]`

The jar stops at `driver.write(ldvHandle, …)` (§135.9). **Everything below that is native/hardware and is
NOT decodable from `lonworks-rt.jar`** — registered honestly as a gap, not inferred:

- The **TP/FT-10 line code** — 78.125 kbaud, differential-Manchester bit encoding, the ANSI/CEA-709.1 L2
  beta1/beta2 slot timing, the L2 16-bit CRC, predictive p-persistent CSMA backoff — is implemented in the
  Neuron/FT transceiver and the native `ldv` driver. None of these constants appear in the jar `[CERT]`
  (negative finding: `LonOutputStream` only emits the application buffer; the link is `driver.write`,
  `NLonLinkLayer.java:274`). → **requires-execution / hardware** (live FT-10 segment + protocol analyzer);
  candidate `[CERT-hw]`. Cross-ref B127 for the native `ldvProxy` side.
- The **little-endian byte order of LonTalk on the physical wire**: §135.2 proved the *Java application
  buffer* is big-endian; the Neuron firmware re-orders to the LonTalk wire convention natively `[INFER]` —
  not literal in this jar.
- Concrete **runtime NV selectors / device program-IDs / domain keys** are per-network values, not literals
  here → requires a live network.

## 135.12 — Self-verify

- **Token check**: grep-confirmed **all 12 load-bearing `[CERT]` token groups** present in their cited
  source (§135 grep pass, output retained): BE primitives `LonOutputStream.java:86-98` (`l >> 8 & 0xFF`);
  float scaling `BLonFloat.java:179,183` (`val += getOffset` / `val /= getResolution`) + decode
  `BLonFloat.java:268,272`; SNVT scale `TypeScalar64.java:41` + `ResourceToXLon.java:475`
  (`scaleA * Math.pow(10.0, …scaleB)`) + `TypeScalar.java:141` (`scaleA = in.readSigned16`); NI command codes
  `NAppBuffer.java:28-47` (`niCOMM=16`,`niNETMGMT=32`,`niRESET=80`,`NETVAR_BIT=128`,`AUTH_BIT=16`); NV
  selector `UnprocessedNV.java:30,37,38` (`| 128`, `nvSelector & 16128 >> 8`, `& 0xFF`, `MAX_NETVAR_DATA=31`);
  file transfer `FileXferData.java:16,53` (`code=62`, `window<<4 | packet&15`) + `LonFileTransfer.java:38`
  (`MAX_DATA_LENGTH=32`); native pipe `NLonLinkLayer.java:59,144,210,274` (`driverInit`, `driver.write/read`,
  `ldvHandle`); NM codes `NetMessages.java:35,57,78` + `FetchNvRequest.java:14` (115/51/19); bitfield
  `LonOutputStream.java:71`; far-side prefix `NAppBuffer.java:487,608` (`writeUnsigned8(126)`) +
  `setDataLength(out.size()-16)` `NAppBuffer.java:491`.
- **Marker tally**: ~95 `[CERT]` · 0 `[CERT-doc]` · 0 `[CERT-web]` · 0 `[CERT-a]` · 3 `[INFER]` (the SNVT_temp
  worked example §135.4, the success-code `0x20|low` rule §135.8, the physical-wire little-endian note
  §135.11). **[INFER]/[CERT] ratio ≈ 0.03** — the LON encoding layer (byte primitives, SNVT scaling, struct
  packing, NV update message, NI application buffer, NM codes, file transfer) is fully source-confirmed from
  `lonworks-rt.jar`. The genuinely non-static remainder is the **TP/FT-10 physical/link layer** below
  `driver.write` — registered as a requires-execution/hardware gap (§135.11), NOT padded with inference.
- **Artifacts**: block file written; `sources/decompiled/lonworks-rt/` (422 classes) preserved;
  `SOURCES.md` (lonworks-rt sha256 defd41b1…), `INDEX.md` Layer 26, `RESEARCH-STATE-protocols.md` updated;
  CATALOG regenerated.

## 135.x — Connections

- **[Block 19] / [Block 77]** — LON device model + XIF/LNML + SNVT type catalog (CONFIG level): B19/B77
  established *which* SNVT types exist and how devices are modeled/imported; B135 supplies the *byte
  encoding* of an SNVT value — the `resolution = scaleA·10^scaleB` scaling (§135.4), the big-endian
  fixed-point transform (§135.3), and struct/bitfield packing (§135.5) — the layer those configs serialize
  into.
- **[Block 120]** — ShortStack / platform file transfer (CONFIG level): B120 described LON file transfer at
  the platform/ShortStack level; B135 §135.10 pins the *wire framing* — the LonMark FTP control NVs
  (`SNVT_file_request`/`_status`/`_pos`) plus the explicit `FileXferData` message-code-62 windowed packets
  (32-byte chunks, 6/window, selective retransmit).
- **[Block 127]** — `ldvProxy` native driver pipe: B127 documented the native side of the LON adapter
  driver; B135 §135.9 pins the *Java handoff point* — `BLonPlatformService.driverInit/write/read(ldvHandle,…)`
  — and confirms everything below it (FT-10 line code, L2 CRC, Manchester) is native, not in the jar
  (§135.11). They meet exactly at `driver.write`.
- **[Block 131] / [Block 132] / [Block 133] / [Block 134]** — sibling wire-level protocol blocks.
  **Sharpest contrast**: LON's *application buffer* is **big-endian** (§135.2) like BACnet/OPC-UA structs but
  unlike Modbus-RTU's CRC quirk (B131); its SNVT values use a **per-element `resolution`/`offset` fixed-point
  scaling** (§135.3) — a transform absent from the other four (Modbus/OPC/BACnet/Fox carry raw or
  type-tagged values). And uniquely, **half of LON's real wire (TP/FT-10 line code) is below the Java
  layer** in a native driver (§135.9/§135.11) — whereas Modbus/OPC-UA/BACnet/IP/Fox are fully Java-framed on
  TCP; only LON (and OPC-DA's COM/DCOM, B132) push the lowest wire layer into native code.
