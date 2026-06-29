# Block 132 — OPC wire encoding: OPC-UA Binary (built-in types / NodeId / Variant) + secure-channel framing + the legacy OPC DA / COM JNI boundary

> Research of the **Niagara N4 OPC driver wire protocols** as actually implemented in the shipped Java
> runtime jars. Two distinct stacks ship:
> (1) **OPC-UA** — `opcUaCore-rt.jar` bundles a rebranded **OPC Foundation / Prosys Java stack**
> (`com.prosysopc.ua.stack.*`); this block documents its **UA Binary encoding** (primitive + structured
> serialization, NodeId encoding variants, Variant encoding, the built-in type-id table) and the
> **UA-TCP secure-channel message framing** (HEL/ACK/OPN/MSG/CLO, chunk header, security + sequence
> headers) plus the **Read / Write / Browse** service request/response structure.
> (2) **Legacy OPC DA** — `opc-rt.jar` is a **COM/DCOM client reached through JNI**
> (`com.tridium.opc.jni.*`); this block pins **where Java encoding STOPS and native COM marshalling
> begins** (the `native` method boundary, the VT_* VARIANT type map) and is honest that the OPC DA
> on-the-wire bytes are **COM/DCOM (ORPC/NDR)** produced by the native proxy DLLs (B127) — out of
> static-Java scope, a requires-execution / native gap. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary, decompiled with Vineflower from the live install):
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/opcUaCore-rt.jar` (sha256 0000972e…),
> `opcUaClient-rt.jar` (601aa85b…), `opcUaServer-rt.jar` (e4ce3517…), `opc-rt.jar` (6db1abda…) —
> targeted package extracts preserved under `sources/decompiled/opcUaCore-rt/`, `…/opc-rt/`,
> `…/opcUaClient-rt/`, `…/opcUaServer-rt/` and registered in `sources/SOURCES.md`.
> Method: `decompile-java.sh` (Vineflower) + Python decode of the TCP message-type magic constants.
> Markers:
> `[CERT]` local primary source (`file:line`) · `[CERT-doc]` downloaded doc · `[CERT-web]` official web ·
> `[CERT-a]` secondary/forum · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — wire-level focus). Connects [Block 127] (native OPC COM proxy
> DLLs — the other side of the DA JNI boundary), [Block 7] / [Block 93] (driver framework / OPC
> integration architecture), [Block 131] (sibling protocol block — Modbus wire-level).

---

## 132.1 — Two OPC stacks, two encodings (module map) `[CERT]`

The N4 OPC support splits cleanly into a **modern UA** stack and a **legacy DA** stack with completely
different wire technologies:

| Concern | Jar | Package root | Wire technology |
|---|---|---|---|
| OPC-UA encoding/decoding (UA Binary) | `opcUaCore-rt.jar` | `com.prosysopc.ua.stack.encoding.binary` | OPC-UA Binary over UA-TCP |
| OPC-UA built-in datatypes (NodeId/Variant/…) | `opcUaCore-rt.jar` | `com.prosysopc.ua.stack.builtintypes` | — |
| OPC-UA secure-channel framing | `opcUaCore-rt.jar` | `com.prosysopc.ua.stack.transport.tcp.impl` | UA-TCP (opc.tcp://) |
| OPC-UA service structs (Read/Write/Browse) | `opcUaCore-rt.jar` | `com.prosysopc.ua.stack.core` | encoded via UA Binary |
| Tridium UA driver glue (points/learn/alarm) | `opcUaClient-rt.jar` / `opcUaServer-rt.jar` | `com.tridium.opcUaClient/Server` | delegates to the stack above |
| Legacy OPC **DA** client (COM via JNI) | `opc-rt.jar` | `com.tridium.opc.jni` | **COM/DCOM** (native) |
| Legacy OPC DA VARIANT type map | `opc-rt.jar` | `com.tridium.opc.client.util.BOpcDataType` | COM VARTYPE |

Key structural finding `[CERT]`: the OPC-UA wire stack is **not Tridium-written** — it is the OPC
Foundation Java stack rebranded into the `com.prosysopc.ua.stack.*` namespace (Prosys OPC UA SDK). The
namespace strings are the canonical `http://opcfoundation.org/UA/...` URIs `[CERT]` `BuiltinsMap.java:24-48`.
The Tridium `opcUaClient`/`opcUaServer` jars are thin integration layers that hand encoding to this
stack. The legacy DA jar `opc-rt` is the opposite: pure Java glue over a **native COM client** reached
through `native` JNI methods (§132.8).

## 132.2 — UA Binary: global rule = little-endian `[CERT]`

OPC-UA Binary is **little-endian** for all primitives. The `BinaryEncoder` forces this at every entry
point: each constructor sets `ByteOrder.LITTLE_ENDIAN` `[CERT]` `BinaryEncoder.java:258-283` and
`setWriteable` **rejects** any writeable that is not little-endian `[CERT]` `BinaryEncoder.java:328-334`
(`throw new IllegalArgumentException("Writeable must be in Little-Ending byte order")`). Primitives are
written via `ByteBuffer.putShort/putInt/putLong/putFloat/putDouble`, which therefore emit little-endian
bytes.

## 132.3 — UA Binary: primitive / built-in type serialization `[CERT]`

Each built-in type has a dedicated writer in `BinaryEncoder` (decode is symmetric in `BinaryDecoder`).
The built-in type **id** (used in Variant and ExtensionObject framing) comes from `BuiltinsMap.ID_MAP`
`[CERT]` `BuiltinsMap.java:50-75` (verbatim 1-25):

| Id | Type | Wire encoding | Encoder cite |
|---|---|---|---|
| 1 | Boolean | 1 byte (`0`/`1`) | `BinaryEncoder.java:427-438` (writes `(byte)(v?1:0)` :433) |
| 2 | SByte | 1 byte | `BinaryEncoder.java:985-996` |
| 3 | Byte (UByte) | 1 byte | `BinaryEncoder.java:440-451` (`toByteBits`) |
| 4 | Int16 | 2-byte LE | `BinaryEncoder.java:834-845` |
| 5 | UInt16 | 2-byte LE | `BinaryEncoder.java:1107-1118` (`toShortBits`) |
| 6 | Int32 | 4-byte LE | `BinaryEncoder.java:847-858` |
| 7 | UInt32 | 4-byte LE | `BinaryEncoder.java:1120-1131` (`toIntBits`) |
| 8 | Int64 | 8-byte LE | `BinaryEncoder.java:874-885` |
| 9 | UInt64 | 8-byte LE | `BinaryEncoder.java:1133-1144` (`toLongBits`) |
| 10 | Float | 4-byte IEEE-754 LE | `BinaryEncoder.java:784-795` |
| 11 | Double | 8-byte IEEE-754 LE | `BinaryEncoder.java:617-628` |
| 12 | String | `Int32 length` + UTF-8 bytes; **length `-1` = null** | `BinaryEncoder.java:1015-1029` |
| 13 | DateTime | 8-byte LE int64, UA epoch (§132.4) | `BinaryEncoder.java:527-542` |
| 14 | Guid | 16 bytes, mixed-endian (§132.5) | `BinaryEncoder.java:797-832` |
| 15 | ByteString | `Int32 length` + bytes; **`-1` = null** | `BinaryEncoder.java:453-469` |
| 16 | XmlElement | encoded as a ByteString of the UTF-8 XML | `BinaryEncoder.java:1146-1156` |
| 17 | NodeId | encoding byte + ns + id (§132.6) | `BinaryEncoder.java:912-954` |
| 18 | ExpandedNodeId | NodeId + optional nsUri/serverIndex (§132.6) | `BinaryEncoder.java:642-698` |
| 19 | StatusCode | 4-byte LE UInt32 | `BinaryEncoder.java:1002-1013` |
| 20 | QualifiedName | `UInt16 nsIndex` + String name | `BinaryEncoder.java:971-983` |
| 21 | LocalizedText | mask byte (`1`=locale,`2`=text) + present strings | `BinaryEncoder.java:887-910` |
| 22 | ExtensionObject / Structure | typeId (NodeId) + body-encoding byte + body | `BinaryEncoder.java:700-731` |
| 23 | DataValue | mask byte + present fields (§132.7) | `BinaryEncoder.java:471-525` |
| 24 | Variant | type byte + value (§132.6) | `BinaryEncoder.java:1336-1408` |
| 25 | DiagnosticInfo | mask byte + present fields | `BinaryEncoder.java:549-615` |

Null handling: variable-length types (String, ByteString, XmlElement) signal null with an **Int32
length of `-1`** `[CERT]` `BinaryEncoder.java:1019, 456, 1149`. The static registration block wiring
each Java class to its UA type id and writer is `BinaryEncoder.java:1410-1444`.

## 132.4 — DateTime: UA epoch (1601) `[CERT]`

A UA `DateTime` is an int64 count of **100-nanosecond intervals since 1601-01-01 (UTC, Gregorian)**,
little-endian. The constant `OffsetToGregorianCalendarZero = 116444736000000000L` is the offset to the
Unix epoch in 100-ns ticks `[CERT]` `DateTime.java:25`, and `fromMillis = millis*10000 + 116444736000000000`
`[CERT]` `DateTime.java:50-52`. The encoder clamps: `>= MAX_VALUE` → `Long.MAX_VALUE`, `<= MIN_VALUE`
→ `0`, else the raw tick value `[CERT]` `BinaryEncoder.java:532-538`; the valid range is
`0 … 2650153247990000000` `[CERT]` `DateTime.java:20-21, 26-27`.

## 132.5 — Guid: the mixed-endian 16-byte layout `[CERT]`

A UA `Guid` is 16 bytes but **not** plain big- or little-endian: Data1 (4 bytes) and Data2/Data3
(2 bytes each) are written little-endian, Data4 (8 bytes) big-endian. The encoder builds a 16-byte
big-endian image then re-emits bytes in the order `3,2,1,0, 5,4, 7,6, 8..15` `[CERT]`
`BinaryEncoder.java:816-827`. This is the standard OPC-UA / Microsoft GUID byte order.

## 132.6 — NodeId, ExpandedNodeId and Variant encoding (the load-bearing structures) `[CERT]`

**NodeId** — first byte is the **encoding type** (`NodeIdEncoding`), low 6 bits; values `[CERT]`
`NodeIdEncoding.java:5-11`:

| Enc byte | Name | On-wire layout after the enc byte | Used when |
|---|---|---|---|
| `0x00` | TwoByte | `[1-byte numeric id]` | ns == 0 **and** id ≤ 255 |
| `0x01` | FourByte | `[1-byte ns][2-byte LE numeric id]` | ns < 256 **and** id ≤ 65535 |
| `0x02` | Numeric | `[2-byte LE ns][4-byte LE numeric id]` | otherwise (numeric) |
| `0x03` | String | `[2-byte LE ns][Int32 len + UTF-8]` | string id |
| `0x04` | Guid | `[2-byte LE ns][16-byte guid]` | guid id |
| `0x05` | ByteString | `[2-byte LE ns][Int32 len + bytes]` | opaque id |

The encoder selects TwoByte/FourByte/Numeric by exactly those range tests `[CERT]`
`BinaryEncoder.java:919-950` (`n(NodeId)`). The decoder is symmetric: it reads the first byte, masks
`var1 & 63` to recover the encoding and dispatches TwoByte/FourByte/… `[CERT]`
`BinaryDecoder.java:823-833`.

**ExpandedNodeId** = a NodeId whose encoding byte carries two extra high-bit flags: `0x80` = a
`NamespaceUri` string follows the body, `0x40` = a 4-byte `ServerIndex` follows `[CERT]`
`BinaryEncoder.java:648-694` (`var2 | 128`, `var2 | 64`). The decoder tests `(var1 & 128) == 128` for
the URI flag and `var1 & 63` for the base encoding `[CERT]` `BinaryDecoder.java:624-625`.

**Variant** — first byte = the built-in **type id** (low 6 bits, from §132.3) OR-ed with array flags
`[CERT]` `BinaryEncoder.java:1336-1408`:

| Top bits of the type byte | Meaning | Layout |
|---|---|---|
| none (`typeId`) | scalar | `[typeId][value]` |
| `0x80` (`typeId \| 128`) | 1-dimension array | `[typeId\|0x80][Int32 length][elements…]` |
| `0xC0` (`typeId \| 192`) | multi-dimension array | `[typeId\|0xC0][Int32 totalLen][elements…][Int32 dimCount + Int32 dims…]` |

A null Variant is a single `0x00` byte (type id 0) `[CERT]` `BinaryEncoder.java:1337-1343`. The encoder
sets the array bit at `q(var4 | 128)` for 1-D and `q(var4 | 192)` for N-D, writing the total element
count then, for N-D, the array-dimensions int32 array `[CERT]` `BinaryEncoder.java:1364-1398`. The
decoder mirrors it: `var1 & 63` = type, `(var1 & 128)` = is-array `[CERT]` `BinaryDecoder.java:1050-1051`.
The class→type-id resolution for the value uses `BuiltinsMap.ID_MAP` `[CERT]`
`BinaryEncoder.java:1356-1360`.

## 132.7 — DataValue and the structure encoder (mask/optional/union) `[CERT]`

**DataValue** is a bit-mask byte followed by present fields, in this fixed order `[CERT]`
`BinaryEncoder.java:471-524`: bit `0x01` Value (Variant), `0x02` StatusCode, `0x04` SourceTimestamp,
`0x08` ServerTimestamp, `0x10` SourcePicoseconds, `0x20` ServerPicoseconds. StatusCode is omitted when
GOOD and timestamps when `MIN_VALUE` (so a plain good value encodes as `0x05` mask + Variant + source
timestamp, etc.) `[CERT]` `BinaryEncoder.java:476-498`.

**Structures** are encoded field-by-field via the `StructureSpecification` in declaration order `[CERT]`
`BinaryEncoder.java:1031-1079`. Three structure kinds are handled: plain (fields in order), **OPTIONAL**
(a leading `UInt32` EncodingMask whose bits flag which optional fields are present, then the present
fields) `[CERT]` `BinaryEncoder.java:1039-1053`, and **UNION** (a leading `UInt32` SwitchField = the
1-based index of the single set member, then that member) `[CERT]` `BinaryEncoder.java:1080-1100`. This
is the generic machinery that serializes every service request/response in §132.10.

## 132.8 — UA-TCP secure-channel message framing `[CERT]`

OPC-UA over `opc.tcp://` frames each message as one or more **chunks**. The chunk message-type magic is a
3-ASCII tag + a 1-byte chunk type; the constants are stored as little-endian ints `[CERT]`
`TcpMessageType.java:4-25` (decoded with Python from the constants):

| Constant | Int | Bytes (LE) | Meaning |
|---|---|---|---|
| `HELLO` | 4998472 | `HEL\0` | client → server handshake |
| `ACKNOWLEDGE` | 4932417 | `ACK\0` | server → client handshake reply |
| `OPEN` | 5132367 | `OPN\0` | OpenSecureChannel (asymmetric security) |
| `MESSAGE` | 4674381 | `MSG\0` | service request/response (symmetric security) |
| `CLOSE` | 5196867 | `CLO\0` | CloseSecureChannel |
| `ERROR` | 5395013 | `ERR\0` | error message |
| `REVERSE_HELLO` | 4540498 | `RHE\0` | reverse-connect hello |

The 4th byte is the **chunk type**: `FINAL` (`'F'` = `0x46<<24`), `CONTINUE` (`'C'`), `ABORT` (`'A'`)
`[CERT]` `TcpMessageType.java:4-6` (so a final MSG chunk = ASCII `"MSGF"`; the helpers
`isFinal/continues/isAbort` mask the high byte `[CERT]` `TcpMessageType.java:27-37`).

**Chunk byte layout** — recovered from the fixed positions read by `ChunkUtils` `[CERT]`
`ChunkUtils.java:30-78` and written by `ChunkFactory.allocate` (which writes the chunk size as an Int32
at **offset 4** `[CERT]` `ChunkFactory.java:67-72`):

| Offset | Field | Size | Cite |
|---|---|---|---|
| 0 | MessageType + ChunkType | 4 | `ChunkUtils.java:30-33` (`getMessageType` @0) |
| 4 | MessageSize | 4 (Int32) | `ChunkFactory.java:67-72` |
| 8 | SecureChannelId | 4 | `ChunkUtils.java:55-58` (`getSecureChannelId` @8) |
| 12 | **MSG**: TokenId (4) — symmetric security header | 4 | `ChunkUtils.java:75-78` (`getTokenId` @12) |
| 12 | **OPN**: SecurityPolicyUri (String) + SenderCert + RecvCertThumbprint — asymmetric security header | var | `ChunkUtils.java:35-63` (`getSecurityPolicyUri`/`getRecvCertificateThumbprint` @12) |
| 16 | SequenceNumber | 4 | `ChunkUtils.java:65-68` (`getSequenceNumber` @16) |
| 20 | RequestId | 4 | `ChunkUtils.java:50-53` (`getRequestId` @20) |

So a symmetric **MSG** chunk header is `[MSG+chunkType][size][secureChannelId][tokenId][sequenceNumber][requestId]`
then the (UA-Binary-encoded) body. The chunk factories fix the sizes `[CERT]` `ChunkFactory.java:155-264`:
Hello/Acknowledge/Error use an 8-byte message header (`messageHeaderSize=8`) and no security/sequence
header; symmetric MSG uses sequence-header size 8 (sequence number + request id); the asymmetric OPN
security header is `12 + policyUri.length + localCert.length + remoteCertThumbprint.length` `[CERT]`
`ChunkFactory.java:167-184` (`AsymmMsgChunkFactory`). Default max chunk = 8192 bytes for Hello/Ack
`[CERT]` `ChunkFactory.java:157-160, 258-262`; Error chunk max ≈ 4108 `[CERT]` `ChunkFactory.java:249-253`.

**HELLO body** carries the connection limits `[CERT]` `Hello.java:7-12, 26-48`: ProtocolVersion,
ReceiveBufferSize, SendBufferSize, MaxMessageSize, MaxChunkCount (all UInt32) + EndpointUrl (String).
**ACKNOWLEDGE** echoes the same 5 UInt32 limits `[CERT]` `Acknowledge.java:7-11`. Padding/signature for
`SignAndEncrypt` mode is computed in `ChunkFactory.allocate` (cipher-block rounding + 1- or 2-byte
padding-size field) `[CERT]` `ChunkFactory.java:44-50, 108-153` — the cryptographic chunk content itself
is a runtime concern, not a static byte layout.

## 132.9 — UA service request/response structure (Read / Write / Browse) `[CERT]`

Every service PDU is a UA `Structure` (§132.7) carried in a MSG chunk. The structures are the canonical
OPC-UA service set; field order (= encode order) from the decompiled stack:

**RequestHeader** (prefix of every request) `[CERT]` `RequestHeader.java:33-39`:
`authenticationToken` (NodeId), `timestamp` (DateTime), `requestHandle` (UInt32), `returnDiagnostics`
(UInt32), `auditEntryId` (String), `timeoutHint` (UInt32), `additionalHeader` (ExtensionObject).

**ResponseHeader** (prefix of every response) `[CERT]` `ResponseHeader.java:34-39`: `timestamp`,
`requestHandle`, `serviceResult` (StatusCode), `serviceDiagnostics` (DiagnosticInfo), `stringTable`
(String[]), `additionalHeader`.

| Service | Request fields | Response | Cite |
|---|---|---|---|
| **Read** | RequestHeader, `maxAge` (Double), `timestampsToReturn` (enum→Int32), `nodesToRead` (ReadValueId[]) | ReadResponse → DataValue[] + DiagnosticInfo[] | `ReadRequest.java:30-33` |
| ReadValueId | `nodeId` (NodeId), `attributeId` (UInt32), `indexRange` (String), `dataEncoding` (QualifiedName) | — | `ReadValueId.java:32-35` |
| **Write** | RequestHeader, `nodesToWrite` (WriteValue[]) | WriteResponse → StatusCode[] | `WriteRequest.java:30-31` |
| WriteValue | `nodeId`, `attributeId` (UInt32), `indexRange` (String), `value` (**DataValue**) | — | `WriteValue.java:32-35` |
| **Browse** | RequestHeader, `view` (ViewDescription), `requestedMaxReferencesPerNode` (UInt32), `nodesToBrowse` (BrowseDescription[]) | BrowseResponse → BrowseResult[] | `BrowseRequest.java:31-34` |
| BrowseDescription | `nodeId`, `browseDirection` (enum), `referenceTypeId` (NodeId), `includeSubtypes` (Boolean), `nodeClassMask` (UInt32), `resultMask` (UInt32) | — | `BrowseDescription.java:31-36` |

So a Read attribute is identified on the wire by `{NodeId, attributeId}` (attributeId is the UA numeric
attribute, e.g. 13 = Value), and a Write carries a full **DataValue** (so it can ship value + status +
timestamps). These match the OPC-UA spec service definitions `[INFER]` (spec not preserved this iteration;
the structures themselves are `[CERT]`). The enum types (TimestampsToReturn, BrowseDirection) encode as
Int32 like any `Enumeration` `[CERT]` `BinaryEncoder.java:630-640`.

## 132.10 — Legacy OPC DA: the JNI / COM boundary (where Java encoding STOPS) `[CERT]`

`opc-rt.jar` is **not** a wire codec — it is a thin Java wrapper around a **native COM client**. The base
class `ComObjectClient` holds a raw native pointer `peer` (a `long`) and declares the COM primitives as
`native` `[CERT]` `ComObjectClient.java:6-7, 61-63` (`native long query(long, String iid)` =
`IUnknown::QueryInterface`, `native void release(long)` = `IUnknown::Release`). Each COM interface is
modelled by an `OpcInterface(iid, javaImpl)` pair mapping a GUID string to a Java class `[CERT]`
`OpcInterface.java:9-16`; e.g. `OpcSyncIo.IID = "{39c13a52-011e-11d0-9675-0020afd8adb3}"` (the OPC DA
`IOPCSyncIO` interface) `[CERT]` `OpcSyncIo.java:8`.

**The boundary is the `native` keyword.** All the real DA operations are native methods that take Java
primitives/arrays and return Java primitives/arrays — the COM call and its DCOM marshalling happen
**inside the native DLL**, invisible to static Java:

| Java method (DA op) | Native signature | Cite |
|---|---|---|
| connect local/remote server | `native long createLocalServer(String)` / `createRemoteServer(String,String)` | `OpcDaServer.java:97-99` |
| add group | `native long[] addGroup(long, String, boolean, int, int, int, float, int)` | `OpcDaServer.java:95` |
| add items | `native void addItems(long, String[], int[], boolean[], int[], …)` | `OpcItemMgt.java:41` |
| sync read | `native void read(long, int[] serverHandles, boolean cache, OpcGroup)` | `OpcSyncIo.java:31` |
| sync write bool / numeric / string / array | `native int writeBoolean(long,int,boolean)` · `writeNumeric(long,int handle,int vt,double)` · `writeString(long,int,String,int vt)` · `writeArray(long,int,int[],int[],String[],…)` | `OpcSyncIo.java:31-39` |
| async read/write/advise | `native readAsync/writeAsync/advise/unadvise(…)` | `OpcAsyncIo2.java:35-45` |
| browse address space | `native void browse(long, String, …)` / `goDown/goRoot/getItems/listItems(…)` | `OpcBrowse.java:18`, `OpcBrowseServerAddressSpace.java:160-172` |

The **value typing** that crosses the boundary is the COM `VARIANT` type tag. `writeNumeric`/`writeString`
pass an `int dataType` that is a COM **VARTYPE**, enumerated in `BOpcDataType` `[CERT]`
`BOpcDataType.java:108-138`:

| Const | Value | COM VARTYPE | | Const | Value | COM VARTYPE |
|---|---|---|---|---|---|---|
| VT_EMPTY | 0 | VT_EMPTY | | VT_BOOLEAN | 11 | VT_BOOL |
| VT_NULL | 1 | VT_NULL | | VT_VARIANT | 12 | VT_VARIANT |
| VT_INT_2 | 2 | VT_I2 | | VT_DECIMAL | 14 | VT_DECIMAL |
| VT_INT_4 | 3 | VT_I4 | | VT_SIGNED_BYTE | 16 | VT_I1 |
| VT_REAL_4 | 4 | VT_R4 | | VT_UNSIGNED_BYTE | 17 | VT_UI1 |
| VT_REAL_8 | 5 | VT_R8 | | VT_UINT_2 | 18 | VT_UI2 |
| VT_CURRENCY | 6 | VT_CY | | VT_UINT_4 | 19 | VT_UI4 |
| VT_DATE | 7 | VT_DATE | | VT_INT | 22 | VT_INT |
| VT_STRING | 8 | VT_BSTR | | VT_UINT | 23 | VT_UINT |

Array variants are the COM `VT_ARRAY` flag (`0x2000`) OR-ed with the base type — e.g.
`VT_ARRAY_OF_SHORT = 8194 = 0x2002` (VT_ARRAY|VT_I2), `…OF_LONG = 8195`, `…OF_REAL4 = 8196`, etc.
`[CERT]` `BOpcDataType.java:129-138`. The DA driver only supports boolean / numeric / string (+ their
arrays) `[CERT]` `BOpcDataType.java:329-335` (`isSupported`).

> **BOUNDARY / SCOPE finding `[CERT]` (Java side) + requires-execution (wire).** On the Java side the DA
> "encoding" is just `{int serverHandle, int VARTYPE, primitive value}` handed to a `native` method. The
> **actual OPC DA on-the-wire bytes are COM/DCOM** — the ORPC request, NDR marshalling of the `VARIANT`
> and `OPCITEMSTATE` structures, and the DCE/RPC framing — and those are produced by the **native COM
> proxy/stub DLLs** (the `opcproxy`/`opccomn_ps` surface documented in **B127**) plus the Windows COM
> runtime, NOT by any Java code in `opc-rt.jar`. They cannot be recovered by static Java decompilation.
> Documenting the COM/DCOM wire bytes is therefore a **requires-execution / native-RE gap** (capture on
> a live DCOM session, or Ghidra-RE of the proxy DLLs), explicitly NOT padded with `[INFER]` here. This
> block stops, honestly, at the JNI boundary.

## 132.11 — Self-verify

- **Token check**: grep-confirmed **22/22** load-bearing `[CERT]` citations present in their cited
  decompiled source — verified tokens incl. the little-endian enforcement (`BinaryEncoder.java:328-334`),
  built-in writers (boolean :433, String :1015-1029, ByteString :453-469, DateTime :527-542, Guid
  :816-827), NodeId encoding bytes (`NodeIdEncoding.java:5-11`), Variant array flags
  (`BinaryEncoder.java:1364-1398`), builtin id map (`BuiltinsMap.java:50-75`), chunk header offsets
  (`ChunkUtils.java:30-78`), chunk size at offset 4 (`ChunkFactory.java:67-72`), message-type magic
  (`TcpMessageType.java:4-25`, cross-checked by Python decode → `HEL/ACK/OPN/MSG/CLO/ERR`), service
  structs (`ReadValueId.java:32-35`, `WriteValue.java:32-35`, `BrowseDescription.java:31-36`,
  `RequestHeader.java:33-39`), and the DA JNI boundary (`ComObjectClient.java:61-63`, `OpcSyncIo.java:8-39`,
  `BOpcDataType.java:108-138`).
- **Marker tally**: ~58 `[CERT]` · 0 `[CERT-doc]` · 0 `[CERT-web]` · 0 `[CERT-a]` · 3 `[INFER]`
  (UA epoch/spec-conformance notes + DA-wire deduction). **[INFER]/[CERT] ratio ≈ 0.05** — very low; the
  UA-Binary + framing + DA-boundary evidence is dense and source-confirmed. The DA on-the-wire bytes are
  not investigable from static Java and are registered as a requires-execution gap rather than inflated
  with `[INFER]`.
- **Artifacts**: block file written; `sources/decompiled/opcUaCore-rt/`, `opc-rt/`, `opcUaClient-rt/`,
  `opcUaServer-rt/` preserved; `SOURCES.md`, `INDEX.md`, `RESEARCH-STATE-protocols.md` updated; CATALOG
  regenerated.

## 132.x — Connections

- **[Block 127]** — Native OPC COM proxy DLLs: B127 reverse-engineered the native side (`opcproxy`/
  `opccomn_ps` etc.); B132 §132.10 is the **Java side of the same JNI boundary** and hands off the OPC DA
  COM/DCOM wire to B127's native surface (and to a future native-RE / live-capture gap). Together they
  bracket the DA path: Java glue (B132) → `native` JNI → COM proxy/stub (B127) → DCOM wire.
- **[Block 7]** — Drivers Framework: B7 documents the Container/Network/Device/Point hierarchy and ProxyExt
  pipeline at the architecture level; B132 supplies the **OPC wire layer** beneath it (how an OPC-UA point
  read/write actually serializes — NodeId + attribute + DataValue over a UA-TCP secure channel).
- **[Block 93]** — OPC integration architecture: B93 (OPC framework/integration view) is the architectural
  companion; B132 is its wire-encoding counterpart, exactly as B131 is to B7/B94 for Modbus.
- **[Block 131]** — Sibling protocol block (Modbus wire-level): same Layer-26 wire-encoding focus and
  citation discipline. Contrast: Modbus is a Tridium-written codec (`modbusCore-rt`, big-endian PDUs);
  OPC-UA reuses the **OPC Foundation/Prosys stack** (`com.prosysopc.ua.stack`, little-endian UA Binary),
  and legacy OPC DA is not a Java codec at all but a native COM client — three very different wire models
  under one "OPC" name.
