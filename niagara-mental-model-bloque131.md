# Block 131 — Modbus wire-level encoding (MBAP / PDU / RTU-CRC / ASCII-LRC / addressing & byte-order)

> Research of the **Niagara N4 Modbus driver wire protocol** as actually implemented in the shipped
> Java runtime jars: the on-the-wire ADU/PDU byte layout for Modbus **TCP** (MBAP header), **RTU**
> (CRC-16) and **ASCII** (LRC), the function-code request/response PDU structure, the register/coil/
> input addressing model, and the multi-register byte/word-order encode/decode (the classic Niagara
> "word swap" gotcha). This is the WIRE-level view; the architecture/integration view (driver
> framework, ProxyExt pipeline) is in B7. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary, decompiled with Vineflower from the live install):
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/modbusCore-rt.jar` (sha256 a0b67420…),
> `modbusTcp-rt.jar` (688bb50b…), `modbusTcpSlave-rt.jar` (8d78f0a5…), `modbusAsync-rt.jar` (451565…) —
> preserved under `sources/decompiled/<jar>/` and registered in `sources/SOURCES.md`.
> Method: `decompile-java.sh` (Vineflower) + `javap -p -c` bytecode confirmation + a Python re-derivation
> of the CRC-16/MODBUS value. Markers:
> `[CERT]` local primary source (`file:line`) · `[CERT-doc]` downloaded doc · `[CERT-web]` official web ·
> `[CERT-a]` secondary/forum · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — wire-level focus). Connects [Block 7] (driver framework register
> mapping), [Block 94] (Modbus byte-order gotcha at integration level), [Block 19] (vertical-driver wire).

---

## 131.1 — Where the wire encoding lives (module map) `[CERT]`

All Modbus encoding/decoding is centralized in `modbusCore-rt.jar`; the transport jars (`modbusTcp`,
`modbusAsync`, `modbusTcpSlave`, `modbusSlave`) only add socket/serial plumbing and call the core.

| Concern | Class | File |
|---|---|---|
| Protocol constants (FCs, exception codes, sizes) | `ModbusMessageConst` | `modbusCore-rt/.../messages/ModbusMessageConst.java` |
| Base message + CRC/LRC + transport dispatch | `ModbusMessage` | `.../messages/ModbusMessage.java` |
| Low-level byte writers (int/word/CRC/LRC/ASCII) | `ModbusOutputStream` | `.../messages/ModbusOutputStream.java` |
| Low-level byte readers | `ModbusInputStream` | `.../messages/ModbusInputStream.java` |
| Read-request PDU builder (FC 1/2/3/4) | `ModbusReadRequest` | `.../messages/ModbusReadRequest.java` |
| Write-request PDU builder (FC 5/6/15/16) | `ModbusWriteRequest` | `.../messages/ModbusWriteRequest.java` |
| Read/Write-multiple PDU builder (FC 23) | `ModbusFC23Request` | `.../messages/ModbusFC23Request.java` |
| Response parse + register/float/64-bit decode | `ModbusResponse` | `.../messages/ModbusResponse.java` |
| 16/32/64-bit datatype byte-order encode | `ByteConverterUtil` | `.../util/ByteConverterUtil.java` |
| Register-count per datatype | `DataTypeUtil` | `.../util/DataTypeUtil.java` |
| Address model (hex/decimal/Modbus 4xxxx) | `BFlexAddress` | `.../datatypes/BFlexAddress.java` |
| TCP client RX (MBAP parse) | `ModbusTcpRxDriver` | `modbusTcp-rt/.../comm/ModbusTcpRxDriver.java` |
| TCP slave session (server MBAP parse) | `ModbusTcpSlaveSession` | `modbusTcpSlave-rt/.../comm/ModbusTcpSlaveSession.java` |

`ModbusMessage` extends the generic `com.tridium.basicdriver.message.Message` and dispatches by
`comType` (`0`=ASCII, `1`=RTU, `2`=TCP) to `writeAscii`/`writeRtu`/`writeTcp` `[CERT]`
`ModbusMessage.java:556-573`. `comType` is taken from the device's `getModbusMode()` at construction
`[CERT]` `ModbusMessage.java:540-545`.

## 131.2 — Function codes implemented `[CERT]`

Verbatim from the constant interface `[CERT]` `ModbusMessageConst.java:4-14`:

| FC (dec) | Name | Constant | Direction |
|---|---|---|---|
| 1 | Read Coils | `READ_COIL_STATUS` | read bits |
| 2 | Read Discrete Inputs | `READ_INPUT_STATUS` | read bits |
| 3 | Read Holding Registers | `READ_HOLDING_REGISTER` | read 16-bit regs |
| 4 | Read Input Registers | `READ_INPUT_REGISTER` | read 16-bit regs |
| 5 | Write Single Coil | `FORCE_SINGLE_COIL` | write 1 bit |
| 6 | Write Single Register | `PRESET_SINGLE_REGISTER` | write 1 reg |
| 15 (0x0F) | Write Multiple Coils | `FORCE_MULTIPLE_COILS` | write bits |
| 16 (0x10) | Write Multiple Registers | `PRESET_MULTIPLE_REGISTER` | write regs |
| 20 (0x14) | Read File Record | `READ_FILE_RECORD` | file |
| 21 (0x15) | Write File Record | `WRITE_FILE_RECORD` | file |
| 23 (0x17) | Read/Write Multiple Registers | `WRITE_READ_REGISTER` | combined |
| 128 (0x80) | Exception bit mask | `ERROR_FUNCTION` | error flag |

FC 7 (Read Exception Status) is present as a dedicated message pair (`ModbusReadExceptionStatusRequest`/
`Response`) `[CERT]` `messages/ModbusReadExceptionStatusRequest.java`. **Not implemented**: FC 22
(Mask Write Register) — no constant, no builder `[CERT]` (absent from `ModbusMessageConst` and the
`messages/` package). The exception bit is detected as `functionCode & 0x80` `[CERT]`
`ModbusMessage.java:643, 732` (RTU/TCP path) / `ModbusTcpRxDriver.java:171` (`ibuf[7] & 128`).

## 131.3 — Modbus TCP: the MBAP header (ADU) `[CERT]`

A Modbus TCP request frame is built as a 7-byte MBAP header followed by the PDU. For a **read** request
the whole 12-byte frame is laid out explicitly `[CERT]` `ModbusReadRequest.java:48-68`:

| Offset | Field | Bytes | Encoding | Code |
|---|---|---|---|---|
| 0-1 | Transaction Identifier | 2 | **big-endian** (hi,lo) | `msgArray[0]=(txn&0xFF00)>>8; [1]=txn&0xFF` |
| 2-3 | Protocol Identifier | 2 | **always 0x0000** | `[2]=0; [3]=0` |
| 4-5 | Length | 2 | big-endian; **=6 for reads** | `[4]=0; [5]=6` |
| 6 | Unit / Slave Identifier | 1 | `deviceAddress` | `[6]=deviceAddress` |
| 7 | Function Code | 1 | | `[7]=functionCode` |
| 8-9 | Starting Address | 2 | **big-endian** | `[8]=(start&0xFF00)>>8; [9]=start&0xFF` |
| 10-11 | Quantity | 2 | **big-endian** | `[10]=(count&0xFF00)>>8; [11]=count&0xFF` |

Notes:
- **Length field = number of bytes that follow offset 5** (unit id + PDU). For a fixed read it is the
  constant `6` (unit+FC+addr+qty) `[CERT]` `ModbusReadRequest.java:59-60`. For writes it is computed as
  `msgArray.length` of the formatted PDU **including the unit id+FC** `[CERT]` `ModbusWriteRequest.java:45-47`
  (the write PDU built by `formatMessage()` starts with `deviceAddress` then `functionCode`, so the byte
  count after the length field matches). The protocol id is again hard-zeroed `[CERT]`
  `ModbusWriteRequest.java:43-44`.
- **Transaction id** is a per-message monotonically increasing counter, allocated lazily if `< 0`,
  wrapping at `maxTransactionId` (default 65535) `[CERT]` `ModbusMessage.java:851-873`,
  `ModbusReadRequest.java:51-53`.
- There is **no CRC and no LRC** on TCP — TCP framing relies on the length field alone `[CERT]`
  (`writeTcp` in all builders appends no checksum; cf. `writeRtu`/`writeAscii` which do).

### MBAP receive / parse (client RX) `[CERT]`
`ModbusTcpRxDriver.readMessageFromStream()` reads into a 261-byte buffer. In byte-mode it reads one byte
at a time and, once `rxSize == 6`, sets `dataLen` from the byte just read (the low Length byte at
`ibuf[5]`), then loops until `rxSize == dataLen + 6` `[CERT]` `ModbusTcpRxDriver.java:152-163`. It then:
strips the 6 header bytes (`rxData = ibuf[6..]`), recovers the transaction id as
`(ibuf[0]&0xFF)<<8 | ibuf[1]&0xFF`, flags an exception if `ibuf[7] & 0x80`, and rejects the frame on
transaction-id mismatch (unless `disableTransactionIdCheck`) `[CERT]` `ModbusTcpRxDriver.java:170-198`.
The slave/server side parses identically: `transactionId = ibuf[0]<<8 & 0xFF00 | ibuf[1]&0xFF`, `rxData =
ibuf[6..]` `[CERT]` `ModbusTcpSlaveSession.java:132-136`.

> **Implementation detail [CERT]**: Tridium strips only the first **6** MBAP bytes and keeps the **unit
> id as the first byte of the working buffer** (`rxData[0]=deviceAddress`, `rxData[1]=functionCode`,
> cf. `ModbusMessage.toResponse` `resp[0]`/`resp[1]` `ModbusMessage.java:678-679`). So internally the
> "PDU" buffer is really `unit-id + PDU`. The on-wire MBAP is the standard 7 bytes; the 7th byte (unit
> id) is simply treated as data[0] downstream. `dataLen` is derived from the **low** Length byte only
> (`ibuf[5]`), ignoring `ibuf[4]` — safe because Modbus TCP frames are < 256 bytes here (buffer = 261)
> `[CERT]` `ModbusTcpRxDriver.java:141,160-163`.

## 131.4 — Request PDU byte structure per function code `[CERT]`

The PDU body (after unit id + FC) for each builder. All multi-byte fields are **big-endian** via explicit
`(x & 0xFF00) >> 8` then `(x & 0xFF)` shifts.

**Read FC 1/2/3/4** — `[CERT]` `ModbusReadRequest.java:19-29` (RTU shown; TCP/ASCII share the body):
`[unit][FC][startHi][startLo][qtyHi][qtyLo]` (+CRC for RTU / +LRC for ASCII / MBAP for TCP).

**Write FC 5 (single coil) / FC 6 (single register)** — `[CERT]` `ModbusWriteRequest.java:59-70`:
`[unit][FC][addrHi][addrLo][data0][data1]` — `byteCount` forced to 2, raw `data` copied. (For FC 5 the
two data bytes are the caller-supplied coil value 0xFF00/0x0000.)

**Write FC 15 (multiple coils) / FC 16 (multiple registers)** — `[CERT]` `ModbusWriteRequest.java:71-77`:
`[unit][FC][addrHi][addrLo][qtyHi][qtyLo][byteCount][data…]` — `byteCount = data.length`.

**FC 23 (read/write multiple registers)** — `[CERT]` `ModbusFC23Request.java:68-83`:
`[unit][FC][readStartHi][readStartLo][readQtyHi][readQtyLo][writeStartHi][writeStartLo][writeQtyHi][writeQtyLo][writeByteCount][writeData…]`.
Read params map to `startAddress/numberPoints`, write params to `wrAddress/wrNumberPoints/wrData`
`[CERT]` `ModbusFC23Request.java:19-28`.

### Response PDU parse `[CERT]`
`ModbusResponse.formatBaseMessage()` (server/echo side) writes: FC 5/6 → `[unit][FC][addrHi][addrLo]
[data…]`; FC 15/16 → `[unit][FC][addrHi][addrLo][qtyHi][qtyLo]`; default (read) → `[unit][FC][byteCount]
[data…]` `[CERT]` `ModbusResponse.java:68-91` (note `writeWord` = big-endian, §131.7). On the client,
`ModbusMessage.toResponse` reads `byteCount = resp[2]` then copies `resp[3 + i]` for read FCs (1/2/3/4/23)
`[CERT]` `ModbusMessage.java:737-768`. Expected response size is pre-computed: bits → `5 + ceil(n/8)`,
registers → `5 + n*2`, single/multiple write → `8` `[CERT]` `ModbusMessage.java:575-606`.

## 131.5 — RTU framing + CRC-16/MODBUS `[CERT]` — and a non-standard byte order finding

RTU frame = `[unit][FC][…PDU…][CRClo?][CRChi?]`. The CRC is a **table-driven CRC-16/MODBUS** (reflected
polynomial **0xA001**, init **0xFFFF**), implemented with two 256-entry lookup tables `constCRCHi`/
`constCRCLo` `[CERT]` `ModbusMessage.java:14-271, 272-529`. The algorithm `[CERT]`
`ModbusMessage.java:823-835`:

```
hiCRC=0xFF; loCRC=0xFF;
for each byte b: index = hiCRC ^ b; hiCRC = loCRC ^ constCRCHi[index]; loCRC = constCRCLo[index];
return hiCRC*256 + loCRC;
```

I re-derived this independently: a bit-wise CRC-16/MODBUS (poly 0xA001, init 0xFFFF, refin/refout) gives
the standard check value `0x4B37` for `"123456789"`, and `0xCDC5` for the frame `01 03 00 00 00 0A` —
i.e. `calcCRC` returns the canonical CRC-16/MODBUS integer with the **high** byte in bits 15-8 `[INFER]`
(numerical re-derivation; the table is byte-identical to the Modicon reference, e.g. `constCRCHi[1]=0xC1`,
`constCRCLo[1]=0xC0`).

> **STANDOUT FINDING — CRC byte order on the wire `[CERT]` (code) / `[INFER]` (spec comparison).**
> `ModbusOutputStream.writeCRC()` appends `(crc & 0xFF00) >> 8` **first** (the high byte) then
> `(crc & 0xFF)` (the low byte) `[CERT]` `ModbusOutputStream.java:57-62`, **confirmed at bytecode level**
> (`javap -p -c`: `iload_2; ldc 65280; iand; bipush 8; ishr; i2b; write` then `iload_2; sipush 255; iand;
> i2b; write` — not a decompiler artifact). For `01 03 00 00 00 0A` this emits `… CD C5`.
> The Modbus serial-line spec appends the CRC **low byte first** (`… C5 CD`) `[INFER]` (well-known spec
> rule; not verified against a locally-preserved spec doc this iteration). The receive path is
> **internally self-consistent** with the transmit path: `toResponse` reads the CRC as
> `readCRC = (resp[len-2]&0xFF)<<8 | (resp[len-1]&0xFF)` — high byte first `[CERT]`
> `ModbusMessage.java:692-693`, and `verifyCRC` compares `msg[len-2]==hiCRC && msg[len-1]==loCRC`
> `[CERT]` `ModbusMessage.java:837-849`. So a Tridium master talking to a Tridium slave agrees; whether
> this interoperates with third-party RTU devices that follow the spec's low-first rule **must be
> confirmed on live wire** → deferred DYNAMIC gap (candidate `[CERT-hw]`). Do not treat "the driver is
> non-interoperable" as established — the code is `[CERT]`, the spec mismatch is the open question.

(Contrast: the **TCP** path has no CRC, so this question is RTU/ASCII-only; the OptimizerSupervisor
supervisor role is overwhelmingly Modbus **TCP**.)

## 131.6 — ASCII framing + LRC `[CERT]`

ASCII frame = `:` (0x3A) + hex-encoded bytes (each byte → two upper-case ASCII hex chars) + `CR` `LF`
`[CERT]` `ModbusOutputStream.toAsciiHexByteArray():22-40`. The checksum is an **LRC** = two's-complement
of the 8-bit sum of the message bytes: `lrc = (~sum + 1) & 0xFF` `[CERT]` `ModbusMessage.java:798-808`.
The LRC byte is appended **before** ASCII hex-encoding `[CERT]` `ModbusReadRequest.java:32-46` /
`ModbusWriteRequest.java:53-57`. On receive, ASCII is converted back to raw bytes via
`convertAscii2Rtu` and the LRC re-checked `[CERT]` `ModbusMessage.java:608-637`, `ModbusInputStream.java:37-49`.

## 131.7 — Word-level byte writers (the building blocks) `[CERT]`

Two primitive 16-bit writers with **opposite** byte order — a frequent source of confusion:

| Method | Order | Code | Used by |
|---|---|---|---|
| `writeWord(i)` | **big-endian** (hi,lo) | `write(i>>8); write(i&0xFF)` `ModbusOutputStream.java:47-50` | response PDU addr/qty fields (`formatBaseMessage`) |
| `writeInt(i)` | **little-endian** (lo,hi) | `write(i&0xFF); write(i>>8)` `ModbusOutputStream.java:42-45` | (not used in the standard request/response PDU path) |
| `readWord()` | **big-endian** | `(read()<<8)+read()` `ModbusInputStream.java:51-54` | response decode |

The request builders do **not** use `writeWord`; they inline big-endian shifts byte-by-byte (§131.4).

## 131.8 — Register / coil / input addressing model `[CERT]`

`BFlexAddress` carries an `addressFormat` (`hex` / `decimal` / `modbus`) + a string `address` `[CERT]`
`BFlexAddress.java:14-30`, `enums/BAddressFormatEnum` range `{hex, decimal, modbus}` `[CERT]`.
The crucial conversion to the **0-based protocol address** put on the wire is `getDataAddress()` `[CERT]`
`BFlexAddress.java:149-166`:

| Format | Wire address |
|---|---|
| `hex` | `parseInt(address,16)` (used as-is, 0-based) |
| `decimal` | `parseInt(address)` (used as-is, 0-based) |
| `modbus` | classic 1-based → 0-based: `4xxxx→ -40001`, `3xxxx→ -30001`, `2xxxx→ -20001`, `1xxxx→ -10001`, else `-1` |

So Modbus convention `40001` (first holding register) maps to wire address **0** `[CERT]`
`BFlexAddress.java:156-164`. The address-space boundaries (Modbus format) `[CERT]` `BFlexAddress.java:91-143`:

| Space | Range (1-based) | Predicate |
|---|---|---|
| Coils (0x) | `1 – 9999` | `isModbusCoilAddress` (`<10000`) |
| Discrete inputs / status (1x) | `10001 – 19999` | `isModbusStatusAddress` |
| Input registers (3x) | `30001 – 39999` | `isModbusInputAddress` |
| Holding registers (4x) | `40001 – 49999` | `isModbusHoldingAddress` |

Validity caps the address at `< 50000` (Modbus format) and `≤ 65535` overall `[CERT]`
`BFlexAddress.java:80-89, 204-212`. The register **type** (which read FC to use) is a separate
`BRegisterTypeEnum {holding=0, input=1}` `[CERT]` `enums/BRegisterTypeEnum.java:10-19` — `holding`→FC 3,
`input`→FC 4 `[INFER]` (mapping applied in the point/proxy layer; the enum itself only names the two
read register spaces).

## 131.9 — Multi-register datatypes & byte/word order (the Niagara "word swap" gotcha) `[CERT]`

A 32-bit value spans **2** Modbus registers, a 64-bit value spans **4** `[CERT]`
`util/DataTypeUtil.java:6-14`. Datatypes: `BDataTypeEnum {integer, long(32), float, signedInteger,
unsignedLong(32), double, signed64BitLong, unsigned64BitLong}` `[CERT]` `enums/BDataTypeEnum.java:10-30`.

**32-bit byte order** is selectable via `BDataByteOrderEnum {order1032(0,DEFAULT), order3210(1),
order0123(2)}` `[CERT]` `enums/BDataByteOrderEnum.java:10-21`. Encode (`to4ByteFloatArray`) `[CERT]`
`util/ByteConverterUtil.java:58-79`, decode (`getFloat`/`getRegister`) `[CERT]`
`ModbusResponse.java:151-166, 217-232`:

| Order | Byte arrangement | Meaning |
|---|---|---|
| `order3210` | `B3 B2 B1 B0` | full big-endian (ABCD) |
| `order0123` | `B0 B1 B2 B3` | full little-endian (DCBA) |
| `order1032` (**DEFAULT**) | `B1 B0 B3 B2` | **byte-swapped within each register, register order preserved** (BADC = "word swap") |

> **GOTCHA [CERT]**: the driver default is `order1032` (BADC), NOT plain big-endian. A 32-bit float/long
> read from a device that publishes big-endian (`order3210`) will be **mis-decoded** unless the point's
> byte order is changed — this is the on-the-wire root of the integration-level "word swap" symptom in
> B94. Decode and encode are symmetric (same three branches), so misconfiguration corrupts both reads
> and writes. `getRegister(index, size)` with no explicit order defaults to `order3210` at that helper,
> but the proxy layer supplies the point's configured `BDataByteOrderEnum` `[CERT]`
> `ModbusResponse.java:125-133`.

**64-bit byte order** has **eight** permutations `BDataByteOrder64BitEnum {order76543210, order67452301,
order54761032, order45670123, order01234567, order10325476, order23016745, order32107654}` `[CERT]`
`enums/BDataByteOrder64BitEnum.java` (range) — fully realized in both encode `to8ByteArrangedArray`
`[CERT]` `util/ByteConverterUtil.java:127-204` and decode `get64BitArrangedBits` `[CERT]`
`ModbusResponse.java:316-377`, covering every big/little/word-swapped/dword-swapped combination.

16-bit register encode applies signed/unsigned clamping and is plain big-endian (hi,lo) `[CERT]`
`util/ByteConverterUtil.java:81-101`; 16-bit decode sign-extends when `signed` `[CERT]`
`ModbusResponse.java:143-149`.

## 131.10 — Exception / status codes `[CERT]`

Modbus protocol exception codes (returned with FC|0x80) `[CERT]` `ModbusMessageConst.java:36-47`:
`1` illegal function, `2` illegal data address, `3` illegal data value, `4` slave device failure,
`5` acknowledge, `6` slave device busy, `7` negative acknowledge, `8` memory parity error,
`9` device timeout, `10` gateway path unavailable, `11` gateway target device failed to respond.
Tridium adds **negative** internal status codes (not on the wire): `-1` CRC error, `-4` invalid response,
`-5` LRC error, `-6/-7/-8` down/fault/disabled `[CERT]` `ModbusMessageConst.java:48-55`,
`ModbusResponse.getExceptionString():270-314`. CRC/LRC/transaction-id error counters are incremented on
the device on mismatch `[CERT]` `ModbusMessage.java:634, 697, 715`.

## 131.x — Connections

- **[Block 7]** — Drivers Framework: B7 documents the 4-level Container/Network/Device/Point hierarchy and
  ProxyExt pipeline and lists Modbus among supported drivers at the *architecture* level. B131 supplies
  the missing WIRE layer underneath B7's point→register mapping (which FC, which address, which byte order
  actually goes on the cable).
- **[Block 94]** — Modbus byte-order gotcha: B94 observed the word-swap symptom at the integration/config
  level; B131 §131.9 pins it to the code — `BDataByteOrderEnum.order1032` (BADC) being the driver DEFAULT
  in `ByteConverterUtil`/`ModbusResponse`, with the exact byte arrangements for 32- and 64-bit values.
- **[Block 19]** — Vertical drivers + wire protocols: same focus (wire encoding) for other buses; B131 is
  the Modbus entry of the wire-level protocol family (Layer 26).
- **[Block 129]** — Platform daemon: established the pattern that this corpus separates static wire
  *structure* (decompiled, `[CERT]`) from live wire *behavior* (deferred, requires-execution). B131's RTU
  CRC byte-order question (§131.5) is exactly such a deferred live-confirmation gap.
