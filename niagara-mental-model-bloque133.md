# Block 133 — BACnet APDU service-PDU encoding: PDU types + flags, ASN.1 application/context tag codec, primitive value encoding, segmentation & windowing, and the NPDU/BVLC transport it rides on

> Research of the **Niagara N4 BACnet driver wire protocol** at the **APDU / packet level** as actually
> implemented in the shipped Java runtime jar — the bytes on the wire, NOT the object model. The BACnet
> OBJECT model (objects, properties, ReadProperty/WriteProperty semantics) is already covered by
> B23/B77/B120/B127; this block documents the layer those services serialize INTO: the eight APDU
> PDU-types and their flag/header bytes, the ASN.1-style **tag encoder/decoder** (`AsnOutputStream` /
> `AsnInputStream`), the **primitive value encoding** for every BACnet application datatype, the
> **segmentation state machine** (segment splitting arithmetic, window negotiation, MOR handling,
> SegmentACK), the **AtomicWriteFile chunk sizing** arithmetic (the exact "maxAPDU minus overhead"
> referenced by B120), and the **NPDU + BVLC/BVLL** transport headers the APDU rides on (BACnet/IP,
> UDP 0xBAC0 = 47808). READ-ONLY. Corpus language: ENGLISH.
>
> Source (primary, decompiled with Vineflower from the live install):
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/bacnet-rt.jar` (sha256 25df8182…),
> `bacnetUtil-rt.jar` (sha256 09fe5bee…) — preserved under `sources/decompiled/bacnet-rt/`,
> `sources/decompiled/bacnetUtil-rt/` and registered in `sources/SOURCES.md`. The entire codec is in
> `bacnet-rt`; `bacnetUtil-rt` holds no wire-level encoding.
> Method: `decompile-java.sh` (Vineflower) + targeted reading of the `asn`, `stack.transport`,
> `stack.network`, `stack.link.ip` packages + Python sanity-check of the BACnet/IP port constant.
> Markers:
> `[CERT]` local primary source (`file:line`) · `[CERT-doc]` downloaded doc · `[CERT-web]` official web ·
> `[CERT-a]` secondary/forum · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — wire-level focus). Connects [Block 23] / [Block 77] / [Block 120] /
> [Block 127] (BACnet object model, integration, file transfer, native surface) and [Block 131] /
> [Block 132] (sibling protocol blocks — Modbus, OPC wire encoding).

---

## 133.1 — Module map: where each wire layer lives `[CERT]`

The whole BACnet codec is Tridium-written (no rebranded third-party stack, unlike the OPC-UA case in
B132) and lives entirely in `bacnet-rt.jar`. The wire layers separate cleanly by package:

| Wire layer | Package | Key classes |
|---|---|---|
| APDU PDU-types (transport layer) | `com.tridium.bacnet.stack.transport` | `ApplicationPdu`, `ConfirmedRequestPdu`, `UnconfirmedRequestPdu`, `SimpleAckPdu`, `ComplexAckPdu`, `SegmentAckPdu`, `ErrorPdu`, `RejectPdu`, `AbortPdu` |
| Segmentation state machines | `com.tridium.bacnet.stack.transport` | `ClientStateMachine`, `ServerStateMachine`, `TransportStateMachine` |
| ASN.1 tag + value codec | `com.tridium.bacnet.asn` | `AsnOutputStream` (encoder), `AsnInputStream` (decoder), `AsnConst`, `AsnUtil` |
| Service request/ack/error bodies | `com.tridium.bacnet.services.*` | `BacnetConfirmedRequest`, `AtomicWriteFileRequest`, `IAmRequest`, … |
| Network layer (NPDU) | `com.tridium.bacnet.stack.network` | `NetworkPdu` |
| BACnet/IP data link (BVLC/BVLL) | `com.tridium.bacnet.stack.link.ip` | `BvllConst`, `BvllMessage`, `OriginalUnicastNpdu`, `OriginalBroadcastNpdu`, `ForwardedNpdu`, `BBacnetIpLinkLayer` |

The on-the-wire nesting is `BVLC ▸ NPDU ▸ APDU ▸ (service-choice + ASN.1-tagged service data)`. §133.2–§133.7
document the APDU and ASN.1 layers (the focus); §133.8–§133.9 document the NPDU + BVLC the APDU rides on.

## 133.2 — APDU PDU-type byte and the per-type header layout `[CERT]`

Every APDU's first byte carries the **PDU type in the high nibble** (`(byte0 & 0xF0) >> 4`) `[CERT]`
`ConfirmedRequestPdu.java:298`, with type-specific flags in the low nibble. Each PDU class hand-writes
its bytes in `writeNetworkBytes(ByteArrayOutputStream)` and parses them in `readNetworkBytes`. The eight
types and their exact first byte / header:

| Type | PDU | First byte (encoder constant) | Header after byte0 | Cite (encoder) |
|---|---|---|---|---|
| 0 | **Confirmed-Request** | `0x00` base \| `SEG 0x08` \| `MOR 0x04` \| `SA 0x02` | byte1 = `maxSegs<<4 \| maxApduCode`; invokeId; (if SEG: seqNum, propWindow); serviceChoice; service data | `ConfirmedRequestPdu.java:253-294` |
| 1 | **Unconfirmed-Request** | `0x10` (`os.write(16)`) | serviceChoice; service data | `UnconfirmedRequestPdu.java:44-48` |
| 2 | **SimpleACK** | `0x20` (`os.write(32)`) | originalInvokeId; serviceAckChoice | `SimpleAckPdu.java:49-53` |
| 3 | **ComplexACK** | `0x30` base (`value=48`) \| `SEG 0x08` \| `MOR 0x04` | originalInvokeId; (if SEG: seqNum, propWindow); serviceAckChoice; ack data | `ComplexAckPdu.java:131-160` |
| 4 | **SegmentACK** | `0x40` (`byte0=64`) \| `SERVER 0x01` \| `NAK 0x02` | originalInvokeId; sequenceNumber; actualWindowSize | `SegmentAckPdu.java:69-83` |
| 5 | **Error** | `0x50` (`os.write(80)`) | originalInvokeId; errorChoice; error data | `ErrorPdu.java:60-65` |
| 6 | **Reject** | `0x60` (`os.write(96)`) | originalInvokeId; rejectReason | `RejectPdu.java:49-53` |
| 7 | **Abort** | `0x70` (`temp=112`) \| `SERVER 0x01` | originalInvokeId; abortReason | `AbortPdu.java:53-62` |

Flag-bit constants are declared on the classes: Confirmed-Request `SEG_FLAG=8, MOR_FLAG=4, SA_FLAG=2,
MAX_SEGS_MASK=112, MAX_APDU_LEN_MASK=15` `[CERT]` `ConfirmedRequestPdu.java:16-20`; SegmentACK
`SERVER_BIT=1, NEGATIVE_ACK_BIT=2` `[CERT]` `SegmentAckPdu.java:7-8`; Abort `SERVER_BIT=1` `[CERT]`
`AbortPdu.java:11`. **invokeId, serviceChoice, seqNum, propWindow, reason codes are each a single byte**
(plain `os.write(int)`), so invokeId wraps 0-255 and the proposed-window-size is one octet.

Decode validates the type nibble and throws `InvalidApduTypeException` on mismatch `[CERT]`
`ConfirmedRequestPdu.java:298-300`, `ComplexAckPdu.java:165-167`; Abort additionally asserts a 3-byte
total length (`is.available()!=2` after byte0) `[CERT]` `AbortPdu.java:69-70` (`ABORT_PDU_LENGTH=3`
`[CERT]` `AbortPdu.java:12`).

## 133.3 — Confirmed-Request byte1: the max-segments / max-APDU nibbles `[CERT]`

The Confirmed-Request second byte packs the requester's reception limits into two nibbles `[CERT]`
`ConfirmedRequestPdu.java:268-274`: `byte1 = (maxSegsCode << 4) | maxApduCode`. The high nibble is only
written when `protocolRevision >= 2` `[CERT]` `ConfirmedRequestPdu.java:269-271` (older peers leave
max-segments unspecified). The codes are NOT the raw counts — they are table indices:

**max-APDU-length-accepted nibble** (`MAX_APDU_LENGTHS = {50,128,206,480,1024,1476}`) `[CERT]`
`ConfirmedRequestPdu.java:28`, encoded/decoded by `getMaxAPDULengthCode`/`getMaxAPDULength` `[CERT]`
`ConfirmedRequestPdu.java:329-345`:

| Code | Max APDU octets | | Code | Max APDU octets |
|---|---|---|---|---|
| 0 | 50 (≤ MinMessageSize) | | 3 | 480 |
| 1 | 128 | | 4 | 1024 |
| 2 | 206 | | 5 | 1476 |

**max-segments-accepted nibble** (`MAX_SEGS = {0,2,4,8,16,32,64,255}`) `[CERT]`
`ConfirmedRequestPdu.java:37`, via `getMaxSegsCode`/`getMaxSegs` `[CERT]`
`ConfirmedRequestPdu.java:347-367`: code 0 = unspecified, 1→2, 2→4, 3→8, 4→16, 5→32, 6→64, **7 = "more
than 64"** (decodes to the sentinel 255) `[CERT]` `ConfirmedRequestPdu.java:36, 348-350`. `canFit`
treats `maxSegs<=0` (unspecified) as "fits anything" `[CERT]` `ConfirmedRequestPdu.java:369-371`.

## 133.4 — ASN.1 tag encoding: class / number / LVT, extended tag, extended length `[CERT]`

Inside the service data, every value is a BACnet ASN.1 tag = one **tag octet** then content. The tag
octet bit layout is fixed by `AsnConst` `[CERT]` `AsnConst.java:6-19`: `TAG_NUMBER` = high nibble
(`TAG_NUMBER_MASK=240, TAG_NUMBER_SHIFT=4`), `TAG_CLASS` bit `0x08` (`APPLICATION_TAG=0`,
`CONTEXT_TAG=8`), and the low 3 bits = the **Length/Value/Type (LVT)** field (`LVT_MASK=7`).

The single tag writer `[CERT]` `AsnOutputStream.java:631-643`:
- **tagNumber ≤ 14**: one byte = `(tagNumber << 4) | tagClass | lvt`.
- **tagNumber > 14**: emit `0xF0 | tagClass | lvt` (`EXTENDED_TAG_NUMBER=240` `[CERT]` `AsnConst.java:12`)
  then a second byte = the actual tag number; **tag number > 254 is rejected** `[CERT]`
  `AsnOutputStream.java:636-637` (255 reserved, `TAG_NUMBER_RESERVED=255`).

The LVT field encodes length for primitives, or the constructed markers: **opening tag** writes
`writeTag(n, CONTEXT, 6)` and **closing tag** `writeTag(n, CONTEXT, 7)` `[CERT]`
`AsnOutputStream.java:600-607` (`OPENING_TAG=6, CLOSING_TAG=7` `[CERT]` `AsnConst.java:18-19`). A
context-tagged constructed value is wrapped `[ openTag ][ body ][ closeTag ]` `[CERT]`
`AsnOutputStream.java:590-597` (`writeEncodedValue(tag, bytes)`).

**Primitive length → LVT** `[CERT]` `AsnOutputStream.java:617-629` (`writePrimitiveTag`): if `length ≤ 4`
the LVT field **is** the length; otherwise LVT = `5` (`LVT_EXTENDED_LENGTH` `[CERT]` `AsnConst.java:15`)
and an **extended length** follows `[CERT]` `AsnOutputStream.java:645-668`:

| Length range | Encoding | Cite |
|---|---|---|
| 5 … 253 | single byte = length | `AsnOutputStream.java:649-650` |
| 254 … 65535 | `0xFE`, then 2 bytes **big-endian** | `AsnOutputStream.java:651-656` (`LENGTH_FLAG_254_TO_65535=254`) |
| > 65535 | `0xFF`, then 4 bytes **big-endian** | `AsnOutputStream.java:657-667` (`LENGTH_FLAG_GREATER_THAN_65535=255`) |

The decoder is symmetric: `readTagNumber` reads the extended tag byte when `(tag & 0xF0)==0xF0` `[CERT]`
`AsnInputStream.java:773-784`, and `parsePrimitiveData` reads the same `0xFE`/`0xFF` extended-length
prefixes (2-byte then 4-byte big-endian) when `(tag & 7)==5` `[CERT]` `AsnInputStream.java:767-806`.

## 133.5 — Primitive value encoding (every BACnet application datatype) `[CERT]`

`AsnOutputStream` has one writer per BACnet application type; the **application tag number doubles as the
BACnet datatype id**. All multi-byte integers/floats are **big-endian** (high byte first) — the opposite
of OPC-UA's little-endian (B132 §132.2) and the same orientation as Modbus PDUs (B131).

| App tag | Type | Wire content | Encoder cite |
|---|---|---|---|
| 0 | Null | (no content; LVT length 0) | `AsnOutputStream.java:40-47` |
| 1 | Boolean | **value carried IN the LVT field** for application tags (no content byte); a context-tagged boolean writes LVT=1 + one content byte | `AsnOutputStream.java:50-66` |
| 2 | Unsigned | 1–4 bytes big-endian, minimal length | `AsnOutputStream.java:79-98` |
| 3 | Signed | 1–4 bytes two's-complement big-endian (length grows by 1 if the sign bit would be lost) | `AsnOutputStream.java:111-136` |
| 4 | Real | 4 bytes IEEE-754 single, **big-endian** (`Float.floatToIntBits`, MSB first) | `AsnOutputStream.java:186-205` |
| 5 | Double | 8 bytes IEEE-754 double, big-endian | `AsnOutputStream.java:218-237` |
| 6 | OctetString | raw bytes, length = octet count | `AsnOutputStream.java:250-277` |
| 7 | CharacterString | **first content byte = character-set id**, then encoded bytes; length = bytes + 1 | `AsnOutputStream.java:285-298` |
| 8 | BitString | first content byte = unused-bit count, then bits packed **MSB-first** | `AsnOutputStream.java:342-400` |
| 9 | Enumerated | like Unsigned (1–4 bytes BE) | `AsnOutputStream.java:413-432` |
| 10 | Date | 4 bytes: `year-1900`, month, day, weekday (`0xFF`=wildcard) | `AsnOutputStream.java:445-493` |
| 11 | Time | 4 bytes: hour, minute, second, hundredths | `AsnOutputStream.java:496-556` |
| 12 | ObjectIdentifier | 4 bytes big-endian, 10-bit type / 22-bit instance (§133.6) | `AsnOutputStream.java:559-570` |

**Integer minimal-length** is computed by `findIntegerLength` `[CERT]` `AsnOutputStream.java:138-166`
(picks 1/2/3/4 bytes from where the significant bits start, with sign-extension awareness for negatives)
and emitted MSB-first by `writeIntegerData` `[CERT]` `AsnOutputStream.java:168-173`.

**Character-set id byte** = `BCharacterSetEncoding` ordinal `[CERT]` `AsnOutputStream.java:293`, values
`[CERT]` `BCharacterSetEncoding.java:37-51`: **0 = UTF-8** (default `[CERT]`
`BCharacterSetEncoding.java:51`), 1 = DBCS (IBM/MS), 2 = JIS X 0208, 3 = UCS-4 (UTF-32BE), 4 = UCS-2
(UTF-16BE), 5 = ISO-8859-1. So a BACnet character string on the wire is `[tag][len][charsetByte][text…]`.

**BitString** packing `[CERT]` `AsnOutputStream.java:363-400`: data length = `bits/8 + 1` (rounded up)
including the leading unused-bits byte; `writeStatusFlags` is a fixed 4-bit BitString `{alarm, fault,
overridden, disabled}` `[CERT]` `AsnOutputStream.java:402-410`.

## 133.6 — ObjectIdentifier: the 10-bit type / 22-bit instance packing `[CERT]`

A BACnet `BACnetObjectIdentifier` is a single big-endian **32-bit** value: the high **10 bits** are the
object-type, the low **22 bits** are the instance number `[CERT]` `AsnOutputStream.java:561`
(`objectId = objectType << 22 & 0xFFC00000 | instanceNumber & 0x3FFFFF`, written as 4 bytes by
`writeIntegerData(objectId, 4)` `[CERT]` `AsnOutputStream.java:562`). The masks are pinned in
`BBacnetObjectIdentifier` `[CERT]` `BBacnetObjectIdentifier.java:21-27`: `OBJECT_TYPE_MASK=0xFFC00000`,
`OBJECT_TYPE_SHIFT=22`, `OBJECT_TYPE_MASK_SHIFTED=1023` (10 bits), `INSTANCE_NUMBER_MASK=0x3FFFFF`
(22 bits, max 4194302, `0x3FFFFF`=4194303 = the "unconfigured" sentinel). Decode reverses it:
`objectType = objectId >> 22 & 1023; instanceNumber = objectId & 0x3FFFFF` `[CERT]`
`BBacnetObjectIdentifier.java:55-58`.

> **Correction of the gap brief.** The iteration prompt described this as "22-bit type / 10-bit
> instance" — the code is the reverse and the spec-correct way: **10-bit object-type, 22-bit instance**
> `[CERT]` `AsnOutputStream.java:561` + `BBacnetObjectIdentifier.java:24` (`OBJECT_TYPE_SHIFT=22`).

## 133.7 — Confirmed service body framing (service-choice + tagged operands) `[CERT]`

The Confirmed-Request APDU header is followed by the **service-choice byte** then the service-specific
operands, each a tag from §133.4–§133.6. The choice byte is a single octet (`os.write(serviceChoice)`)
`[CERT]` `ConfirmedRequestPdu.java:281`, indexing `BacnetConfirmedServiceChoice.TAGS` for tracing
`[CERT]` `ConfirmedRequestPdu.java:220`. Service operands are written with the **context** tag overloads
(`writeXxx(int contextTag, …)`) so each operand is self-describing by its context tag number; e.g.
`AtomicWriteFileRequest` writes its fileData operand as an application-tagged OctetString
`outputStream.writeOctetString(this.fileData)` `[CERT]` `AtomicWriteFileRequest.java:116`. The
Complex-ACK and Error bodies are the same shape (service-ack-choice / error-choice byte + tagged data)
`[CERT]` `ComplexAckPdu.java:148`, `ErrorPdu.java:63`.

## 133.8 — Segmentation & windowing: splitting, MOR, SegmentACK, window negotiation `[CERT]`

A confirmed APDU is segmented when it will not fit the negotiated max-APDU. The decision and arithmetic
are in `ClientStateMachine.sendConfirmedRequest` `[CERT]` `ClientStateMachine.java:69-113`:

1. **Effective max length** = `min(deviceMaxApdu, ownMaxApdu)` `[CERT]` `ClientStateMachine.java:70-72`.
2. **Trigger**: segment if `apdu.getLength() + 4 > maxLength` `[CERT]` `ClientStateMachine.java:73` —
   the `+4` is the unsegmented Confirmed-Request APDU header (byte0, byte1, invokeId, serviceChoice).
3. **Segment count** = `length / (maxLength - 6) + 1` `[CERT]` `ClientStateMachine.java:93`. The `-6` is
   `SEG_HDR_LENGTH=6` `[CERT]` `ConfirmedRequestPdu.java:21` — a *segmented* Confirmed-Request header is
   6 bytes (byte0, byte1, invokeId, **seqNum, propWindow**, serviceChoice), so each segment carries
   `maxLength-6` payload bytes; `segmentOffset = (segmentSize-6) * segmentCounter` `[CERT]`
   `ConfirmedRequestPdu.java:85`.
4. **Pre-flight checks** (else `UnsupportedOperationException`): local device must be configured for
   segmented transmit, the peer must advertise segmented receive, and `canFit(deviceMaxSegs, numSegments)`
   `[CERT]` `ClientStateMachine.java:74-107`.

**MOR (more-follows)** drives the send loop `fillWindow` `[CERT]` `ClientStateMachine.java:146-173`:
every segment except the last sets `setMoreFollows(true)`; the final segment (`segCtr >= lastSegment`)
sets `setMoreFollows(false)` and its sequence number = `modulo(lastSegment, 256)` `[CERT]`
`ClientStateMachine.java:157-164` — **sequence numbers wrap mod 256**. The MOR flag is encoded as
`byte0 |= 0x04` (§133.2).

**Window negotiation.** The proposed window size defaults to `DEFAULT_SEGMENTATION_WINDOW_SIZE` =
`Integer.parseInt(System.getProperty("niagara.bacnet.segmentation.window.size", "10"))` `[CERT]`
`TransportStateMachine.java:9` — **default 10, tunable via a system property**. The sender starts with
`actualWindowSize = 1` and `proposedWindowSize = 10` `[CERT]` `ClientStateMachine.java:130-131`, then on
the peer's segmented response adopts the peer's proposed window: `setActualWindowSize(complexAck.
getProposedWindowSize())` `[CERT]` `ClientStateMachine.java:187`. The window thus opens from 1 to the
negotiated value after the first round-trip.

**SegmentACK** acknowledges received segments `[CERT]` `SegmentAckPdu.java:69-83`: `byte0 = 0x40 |
(server?1:0) | (negativeAck?2:0)`, then originalInvokeId, the **last in-order sequence number received**,
and `actualWindowSize`. A negative ACK (NAK, bit `0x02`) requests retransmission from a sequence number
`[CERT]` `ClientStateMachine.java:175-182` (`sendSegmentNak` → `sendSegmentAck(true, seqNum)`).

**Server side (segmented response).** `ServerStateMachine` mirrors this for Complex-ACKs `[CERT]`
`ServerStateMachine.java:152-222`: `maxSegmentLength = min(requestMaxApdu, deviceMaxApdu)` `[CERT]`
`ServerStateMachine.java:157-166`; send unsegmented if `ack.getLength() + 3 <= maxSegmentLength` (the
`+3` = unsegmented Complex-ACK header: byte0, invokeId, serviceAckChoice) `[CERT]`
`ServerStateMachine.java:168-173`; else `numSegments = length / (maxSegmentLength - 5) + 1` `[CERT]`
`ServerStateMachine.java:216` — the `-5` is the *segmented* **Complex-ACK** header (byte0, invokeId,
seqNum, propWindow, serviceAckChoice; one byte less than Confirmed-Request because Complex-ACK has no
maxSegs/maxApdu byte1) `[CERT]` `ComplexAckPdu.java:39, 153` (`segmentOffset = (segmentSize-5) *
segmentCounter`). If the client cannot accept the required segment count the server sends Abort with
reason 1 (buffer-overflow) `[CERT]` `ServerStateMachine.java:197-204`, or reason 4
(segmentation-not-supported) when transmit/accept is unconfigured `[CERT]`
`ServerStateMachine.java:180, 190`.

## 133.9 — AtomicWriteFile chunk sizing (the exact "maxAPDU − overhead" B120 referenced) `[CERT]`

B120 documented BACnet file transfer at the service level and stated the per-`AtomicWriteFile` chunk is
"maxAPDU minus overhead". The exact arithmetic is `BacnetConfirmedRequest.determineMaxDataLength`
`[CERT]` `BacnetConfirmedRequest.java:163-179`:

```
maxApduLength      = min(pdu.maxAPDULengthAccepted, localDevice.maxAPDULengthAccepted)   // :164
maxDataLengthNoSeg = maxApduLength - 4                                                    // :165
maxDataLength      = maxApduLength - 4                                                    // :166  (unsegmented)
if segmentedResponseAccepted:
    if maxSegments  < 0:  maxDataLength = -1                  // unbounded               // :169-170
    elif maxSegments == 0: (leave maxDataLength = maxApduLength-4)                        // :172-173
    else: maxDataLength = maxSegments * maxDataLengthNoSeg    // :176
```

So the **per-APDU overhead is exactly 4 bytes** (the unsegmented Confirmed-Request header: PDU-type/flags
byte, maxSegs/maxApdu byte, invokeId, serviceChoice), and a single unsegmented file chunk is
`maxAPDU − 4` octets `[CERT]` `BacnetConfirmedRequest.java:165-166`. With segmentation the total writable
payload scales to `maxSegments × (maxAPDU − 4)` `[CERT]` `BacnetConfirmedRequest.java:176` (or unbounded
when the peer advertised "more than 64"/unspecified segments). The driver-level stream/record file-write
entry points that consume this budget are `atomicWriteFileStream` / `atomicWriteFileRecord` `[CERT]`
`BBacnetClientLayer.java:361-399`, which build `AtomicWriteFileRequest` and send it confirmed-complex.
This is the code root of B120's chunk-sizing claim.

## 133.10 — Transport: the NPDU and BVLC/BVLL headers the APDU rides on `[CERT]`

The APDU is not sent bare — it is wrapped by a network-layer NPDU and (for BACnet/IP) a BVLC/BVLL header.

**NPDU** `[CERT]` `NetworkPdu.java:193-238`: byte0 = **version = 1** (constant) `[CERT]`
`NetworkPdu.java:194, 53`; byte1 = **control** with bits: `0x80` network-layer-message (vs APDU),
`0x20` destination present (DNET), `0x08` source present (SNET), `0x04` data-expecting-reply, and the
**low 2 bits = network priority** (0–3) `[CERT]` `NetworkPdu.java:195-213`. When DNET present:
2-byte big-endian DNET + 1-byte DLEN + DADR (DNET `0xFFFF` = global broadcast, DLEN 0) `[CERT]`
`NetworkPdu.java:214-227`; when SNET present: 2-byte big-endian SNET + SLEN + SADR `[CERT]`
`NetworkPdu.java:229-234`; then a 1-byte **hop count** (default 255) when DNET present `[CERT]`
`NetworkPdu.java:54, 236-238`. The APDU bytes follow the NPDU header. Decode rejects any version ≠ 1
`[CERT]` `NetworkPdu.java:242-244`.

**BVLC / BVLL (BACnet/IP)** `[CERT]` `OriginalUnicastNpdu.java:21-30`: byte0 = **`0x81`**
(`BVLC_TYPE_BACNET_IP = 129` `[CERT]` `BvllConst.java:4`); byte1 = **BVLC function**; bytes 2–3 = total
BVLL length **big-endian**, written as zeros first then back-patched to `buf.length` `[CERT]`
`OriginalUnicastNpdu.java:24-29` (`BVLL_LENGTH_OFFSET=2, BVLL_BASE_LENGTH=4` `[CERT]`
`BvllConst.java:5-6`); then the NPDU. BVLC function codes `[CERT]` `BvllConst.java:7-18`: 0 = BVLC-Result,
1/2/3 = Write/Read/Read-ACK Broadcast-Distribution-Table, 4 = Forwarded-NPDU, 5 = Register-Foreign-Device,
6/7 = Read/Read-ACK Foreign-Device-Table, 8 = Delete-FDT-Entry, 9 = Distribute-Broadcast-To-Network,
**10 = Original-Unicast-NPDU**, **11 = Original-Broadcast-NPDU**. The default UDP port is `0xBAC0` =
**47808** `[CERT]` `BBacnetIpLinkLayer.java:173, 196` (`udpPort` property default `"0xBAC0"`; Python-verified
`0xBAC0 == 47808`). MS/TP and other data links replace this BVLC/NPDU framing but carry the **same APDU**
bytes — the APDU layer (§133.2–§133.9) is link-independent `[INFER]` (only the BACnet/IP link is
decompiled here; the APDU classes have no IP-specific code).

## 133.11 — Self-verify

- **Token check**: grep-confirmed **30/30** load-bearing `[CERT]` citations present in their cited
  decompiled source — incl. the PDU-type nibble + per-type first-byte constants
  (`ConfirmedRequestPdu.java:253-294`, `UnconfirmedRequestPdu.java:44-48`, `SimpleAckPdu.java:49-53`,
  `ComplexAckPdu.java:131-160`, `SegmentAckPdu.java:69-83`, `ErrorPdu.java:60-65`, `RejectPdu.java:49-53`,
  `AbortPdu.java:53-62`), the maxSegs/maxApdu nibble tables (`ConfirmedRequestPdu.java:28,37,329-367`),
  the tag writer + extended-tag/extended-length (`AsnOutputStream.java:631-668`, `AsnConst.java:6-19`),
  the primitive writers (boolean :50-66, real :186-205, charstring :285-298, bitstring :342-400,
  objectId :559-570), the charset ids (`BCharacterSetEncoding.java:37-51`), the objectId 10/22 split
  (`BBacnetObjectIdentifier.java:21-58`), the segmentation arithmetic (`ClientStateMachine.java:73,93`,
  `ServerStateMachine.java:168,216`, `ConfirmedRequestPdu.java:21,85`, `ComplexAckPdu.java:39`), the
  window default (`TransportStateMachine.java:9`), the file chunk math (`BacnetConfirmedRequest.java:
  163-179`), and the NPDU/BVLC headers (`NetworkPdu.java:193-238`, `OriginalUnicastNpdu.java:21-30`,
  `BvllConst.java:4-18`, `BBacnetIpLinkLayer.java:173`).
- **Marker tally**: ~66 `[CERT]` · 0 `[CERT-doc]` · 0 `[CERT-web]` · 0 `[CERT-a]` · 2 `[INFER]`
  (MS/TP link-independence of the APDU; spec-naming of object-type/instance). **[INFER]/[CERT] ratio ≈
  0.03** — very low; the APDU + ASN.1 + segmentation + transport evidence is dense and entirely
  source-confirmed. Note: this iteration *refuted* the gap-brief's "22-bit type / 10-bit instance" against
  the code (§133.6) — the real packing is 10-bit type / 22-bit instance.
- **Artifacts**: block file written; `sources/decompiled/bacnet-rt/`, `sources/decompiled/bacnetUtil-rt/`
  preserved; `SOURCES.md`, `INDEX.md`, `RESEARCH-STATE-protocols.md` updated; CATALOG regenerated.

## 133.x — Connections

- **[Block 23]** — BACnet object model (objects/properties/services at the semantic level): B23 is the
  WHAT (which objects/properties exist); B133 is the HOW-ON-THE-WIRE (how a ReadProperty/WriteProperty
  request serializes into APDU + ASN.1 tags). B133's §133.5–§133.6 supply the byte encoding for the
  property values B23 describes abstractly.
- **[Block 77]** — BACnet integration/config view: B77 sits above the wire; B133 is the packet layer its
  services compile down to.
- **[Block 120]** — BACnet file transfer (AtomicReadFile/AtomicWriteFile at the service level): B120
  stated the chunk = "maxAPDU minus overhead"; B133 §133.9 pins the exact arithmetic
  (`maxDataLength = maxAPDU − 4`, segmented `maxSegments × (maxAPDU − 4)`,
  `BacnetConfirmedRequest.java:163-179`) — the code root of B120's claim.
- **[Block 127]** — Native driver surface: B127 covered the native side of other drivers; BACnet by
  contrast is **pure Java end-to-end** (no JNI boundary in the codec), unlike the OPC DA case (B132).
- **[Block 131]** — Sibling protocol block (Modbus wire-level): shared Layer-26 focus and citation
  discipline. Contrast: both Modbus PDUs and BACnet ASN.1 values are **big-endian**, but BACnet adds a
  self-describing tag (class/number/LVT) per value and a full segmentation/windowing transport layer
  Modbus lacks.
- **[Block 132]** — Sibling protocol block (OPC wire encoding): the sharpest endianness contrast —
  OPC-UA Binary is **little-endian** (B132 §132.2), BACnet ASN.1 is **big-endian** (B133 §133.5). Both
  are Tridium-or-third-party Java codecs; BACnet's is fully Tridium-written, OPC-UA's is the rebranded
  Prosys/OPC-Foundation stack.
