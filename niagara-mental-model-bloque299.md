# Block 299 — The write path, presets and file records: why `usePresetMultipleRegister` is a correctness setting and not an optimization, and how strings ride FC 20/21

> Focus **modbus**, gap **M5** (absorbing M11-a from [Block 295] and M3-b from [Block 297]). How the
> driver WRITES: which function code it picks and why the default choice makes a 32/64-bit write
> **non-atomic**; the preset components that write on demand; and the file-record channel (FC 20/21) that
> carries strings because no string point exists on the server side ([Block 298] §298.5).
> READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/modbusCore-rt/` (Vineflower, jar `a0b67420…`) —
> `client/datatypes/` presets, `datatypes/BModbusFileRecord`/`BModbusStringRecord`, `client/point/*ProxyExt`
> write paths, `messages/ModbusMessageConst`.
> Official documentation: `sources/manuals/docModbus-N4.14-guide.md` §Adding client presets,
> §Adding client file records, §Configuring network properties.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official Tridium guide (§topic) ·
> `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 294] (`usePresetMultipleRegister` /
> `useForceMultipleCoil` as network properties), [Block 296] (the override that resolves them),
> [Block 297] (the datatypes whose register count drives this), [Block 298] (why strings need this channel),
> [Block 131] (the FC 20/21 PDU skeleton it left open as `P1-fc`).

---

## 299.1 — The four write function codes, and who picks between them `[CERT]`

`ModbusMessageConst` names them `[CERT]` `modbusCore-rt/…/messages/ModbusMessageConst.java:8-13`:

| Constant | FC |
|---|---|
| `FORCE_SINGLE_COIL` | 5 |
| `PRESET_SINGLE_REGISTER` | 6 |
| `FORCE_MULTIPLE_COILS` | 15 |
| `PRESET_MULTIPLE_REGISTER` | 16 |
| `READ_FILE_RECORD` | 20 |
| `WRITE_FILE_RECORD` | 21 |

The choice is made per write, from the two resolved flags of [Block 296] §296.1 —
`device.isPresetMultiple()` and `device.isForceMultiple()`. Both are consumed in **seven** places `[CERT]`
(grep over `modbusCore-rt`): `BModbusClientNumericProxyExt.java:350`,
`BModbusClientRegisterBitProxyExt.java:294`, `BModbusClientNumericBitsProxyExt.java:252`,
`BModbusClientEnumBitsProxyExt.java:148`, `BModbusClientBooleanProxyExt.java:232`,
`BModbusClientPresetRegisters.java:66`, `BModbusClientPresetCoils.java:43`.

Register write `[CERT]` `BModbusClientNumericProxyExt.java:348-352`:

```java
int count = DataTypeUtil.getRegisterCount(this.getDataType());
int code = 6;
if (device.isPresetMultiple()) { code = 16; }
```

Coil write `[CERT]` `BModbusClientBooleanProxyExt.java:232-245`: FC **15** when `isForceMultiple()`, else
FC **5**, with different data encoding per branch (`0x01` for the multiple form, `0xFF` for the single form).

## 299.2 — With the default settings, a 32/64-bit write is NOT atomic `[CERT]`

This is the finding of the block. `usePresetMultipleRegister` defaults to **false** ([Block 294] §294.4), so
`code = 6` — and FC 6 writes exactly **one** register. When the point's datatype spans more than one
register the driver compensates by **looping** `[CERT]` `BModbusClientNumericProxyExt.java:364-383`:

```java
if (code == 6 && count > 1) {
   for (int writeRegNum = 2; writeRegNum <= count; writeRegNum++) {
      int startOffset = (writeRegNum - 1) * 2;
      dataOut[0] = dataOut[startOffset];
      dataOut[1] = dataOut[startOffset + 1];
      req = new ModbusWriteRequest(..., code, pointAddress + writeRegNum - 1, 1, dataOut);
      resp = (ModbusResponse) device.sendModbusMessage(req);
      ...
   }
}
```

`[INFER]` the consequence, stated plainly: **writing one `floatType` point produces two separate FC 6
messages; writing a `doubleType` or 64-bit point produces four.** Between those messages the slave holds a
value that is half-new and half-old. Any master read — or any PLC logic acting on those registers — during
that window sees a **torn value**, which for a float is not a slightly-wrong number but an arbitrary one.

Setting `usePresetMultipleRegister = true` collapses it to a single FC 16 carrying all N registers, which
is atomic at the slave.

So the flag the guide presents as one of four things to configure — *"Configure Float Byte Order, Long Byte
Order, **Use Preset Multiple Register**, and Use Force Multiple Coil"* `[CERT-doc]` §Configuring network
properties — is, for any station writing 32-bit or 64-bit values, a **correctness** setting. The guide gives
no indication of that, and the default is the unsafe one. `[INFER]`

The same reasoning does not apply to coils: a boolean is one bit, so FC 5 vs FC 15 is a compatibility
choice (which form the slave accepts), not an atomicity one. `[INFER]`

Two secondary details from the same method `[CERT]` `BModbusClientNumericProxyExt.java:361-362, 372-375`:
exception code **5** is explicitly *not* treated as an error (`resp.isError() && resp.exceptionCode != 5`) —
that is Modbus ACKNOWLEDGE, meaning the slave accepted a slow write; and a null response is synthesised
with `exceptionCode = 9` before the loop `break`s.

## 299.3 — Presets: a write component, not a point `[CERT]`

`BModbusClientPresetComponent extends BComponent` (269 lines) is the shared base `[CERT]`
`modbusCore-rt/…/client/datatypes/BModbusClientPresetComponent.java:30-56`:

| Slot | Kind | Default |
|---|---|---|
| `startingAddress` | property | `BFlexAddress` |
| `absoluteStartingAddress` | property | `BFlexAddress` (base-address adjusted, as in [Block 297] §297.1) |
| `status` | property | `BStatus.down` |
| `writeOnInputChange` | property | **false** |
| `write` | action | — |
| `writeSuccessful` | topic | — |

It carries a `BaseAddressSubscriber` inner class `[CERT]` `:226` — `[INFER]` the same pattern as
[Block 296] §296.4: when the device base address changes, the absolute address is recomputed.

Two concrete containers, each holding a list of value components `[CERT]`:

- `BModbusClientPresetRegisters extends BModbusClientPresetComponent` — adds `dataType` (default
  `integerType`) and an `addPresetRegisterValue` action taking a `BModbusClientPresetRegister`
  `:30-39`;
- `BModbusClientPresetCoils` — the coil equivalent, consuming `isForceMultiple()` at `:43`.

The leaf components carry the value plus a small audit trail `[CERT]`
`BModbusClientPresetRegister.java:29-48` and `BModbusClientPresetCoil.java:19-38`:
`value` (`BDouble` / boolean) · `lastSuccessfulWrite` (`BAbsTime.NULL`) · `lastFailedWrite` ·
`writeStatus` (`BCommStatus(OK_NOT_ACTIVE)`). Both implement `MessageListener`; the register leaf also
implements `IPropertyValidator`.

`[INFER]` the design intent: a preset is a **command**, not a monitored value. It writes a block of
consecutive registers/coils either on invocation of the `write` action or, if `writeOnInputChange` is set,
whenever its inputs change — with per-value success/failure timestamps so an operator can see whether the
command landed. That is why it is a plain `BComponent` and not a `ProxyExt`: nothing polls it.

## 299.4 — File records: the FC 20/21 channel `[CERT]`

`BModbusFileRecord extends BComponent` (97 lines) `[CERT]`
`modbusCore-rt/…/datatypes/BModbusFileRecord.java:20-40`:

| Property | Default |
|---|---|
| `data` | `BBlob.DEFAULT` |
| `fileNumber` | 0 |
| `startingRecordNumber` | 0 |
| `recordLength` | 0 |

`BModbusStringRecord extends BModbusFileRecord` (211 lines) adds the string facade `[CERT]`
`…/datatypes/BModbusStringRecord.java:25-54`: `writeOnInputChange` (false) · `padding` (false) ·
`input` ("") · `output` ("") · a `write` action · a `writeSuccessful` topic.

Both client and server specialise it — `BModbusClientStringRecord` (`client/datatypes/`) and
`BModbusServerStringRecord` (`server/datatypes/`, 95 lines) `[CERT]` (files present in both trees).

`[INFER]` this closes the question [Block 298] §298.5 left open: Niagara's Modbus **server** has no string
`ProxyExt` because string data is not modelled as a point at all — on either side. It is a file record,
addressed by `fileNumber` + `startingRecordNumber` + `recordLength` and moved with FC 20/21, with `input`/
`output` as the station-facing strings and `padding` controlling fixed-width behaviour. The client's
`BModbusClientStringProxyExt` ([Block 297] §297.3, `numberRegisters` 1..MAX) is the *other* way to read
text — out of ordinary registers — so the driver offers two unrelated string mechanisms.

The message classes for the channel exist on both sides `[CERT]`: client
`messages/ModbusReadFileRequest`/`ModbusWriteFileRequest`, server
`server/messages/ModbusServerReadFileRequest`/`…ReadFileResponse`/`…WriteFileRequest`/`…WriteFileResponse`.
Response sizing constants are declared `READ_FILE_RESPONSE_SIZE = 7`, `WRITE_FILE_RESPONSE_SIZE = 12`
`[CERT]` `ModbusMessageConst.java:24-25`.

The guide documents the workflow for both — §Adding client presets, §Adding client file records,
§Adding server file records `[CERT-doc]` — as drag-and-configure procedures.

## 299.5 — What this closes and what the guide does NOT resolve

**Closes** the *write* half of `P1-fc`, the gap [Block 131] left open ("register-type→FC mapping & FC20/21
file-record PDU live behavior (static skeleton only)"): the FC selection logic is here (§299.1), not in the
message classes; the read half was closed by [Block 295] §295.2. What remains of `P1-fc` is genuinely
**live behaviour**, which stays requires-execution.

Not resolved by the guide:

- that `usePresetMultipleRegister = false` makes multi-register writes **non-atomic** (§299.2) — the single
  most consequential undocumented default found in this focus so far;
- that FC 6 with a multi-register datatype silently becomes **N sequential messages**;
- that exception code 5 (ACKNOWLEDGE) is treated as success;
- that there are **two** unrelated string mechanisms (string ProxyExt over registers vs string file record
  over FC 20/21) and no guidance on which to use.

## 299.6 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 32 | 31 |
| `[CERT-doc]` | 3 | 2 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 7 | 6 |
| **[INFER]/[CERT*] ratio** | | **6/33 = 0.18** |

Script exit 0; citations resolve as `extern` and were token-checked by reading.

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | FC constants 5/6/15/16/20/21 | `[CERT]` | `ModbusMessageConst.java:8-13` |
| 2 | Register write picks 6, or 16 when `isPresetMultiple()` | `[CERT]` | `BModbusClientNumericProxyExt.java:348-352` |
| 3 | Coil write picks 5, or 15 when `isForceMultiple()`, with different encodings | `[CERT]` | `BModbusClientBooleanProxyExt.java:232-245` |
| 4 | FC 6 + `count > 1` loops one message per extra register | `[CERT]` | `:364-383` verbatim |
| 5 | Seven consumers of the two flags | `[CERT]` | grep over `modbusCore-rt`, all seven paths listed |
| 6 | Exception code 5 excluded from the error test | `[CERT]` | `:361-362` |
| 7 | Preset base: 4 properties + `write` action + `writeSuccessful` topic | `[CERT]` | `BModbusClientPresetComponent.java:30-56` |
| 8 | Preset leaves carry `lastSuccessfulWrite`/`lastFailedWrite`/`writeStatus` | `[CERT]` | `BModbusClientPresetRegister.java:29-48`, `BModbusClientPresetCoil.java:19-38` |
| 9 | `BModbusFileRecord` 4 properties | `[CERT]` | `BModbusFileRecord.java:20-40` |
| 10 | `BModbusStringRecord extends BModbusFileRecord`, adds input/output/padding | `[CERT]` | `BModbusStringRecord.java:25-54` |
| 11 | Client and server string-record subclasses both exist | `[CERT]` | files present in both trees |
| 12 | File-record message classes exist on both sides | `[CERT]` | directory enumeration of `messages/` and `server/messages/` |

Tokens grep-confirmed in their cited source: **12 / 12**. Claim 4 is the load-bearing one and was read in
full rather than grepped; the in-place `dataOut` shuffle was traced for a 4-register datatype (offsets
2→0, 4→0, 6→0) to confirm it does **not** corrupt later fragments — it always copies from a higher index
to 0/1. No new sources preserved. Model tier: **no delegation — inline**.

## 299.x — Connections

- **[Block 294]** — declares `usePresetMultipleRegister`/`useForceMultipleCoil` and their `false` defaults; this block shows what those defaults cost.
- **[Block 296]** — the single `overrideNetwork` switch that decides whether the device or the network value is used here.
- **[Block 297]** — `DataTypeUtil.getRegisterCount()` is the `count` in §299.2; the byte converters produce `dataOut`.
- **[Block 298]** — §298.5's missing server string ProxyExt is explained by §299.4.
- **[Block 131]** — closes the write half of its `P1-fc` note.

**Gaps opened by this block**:
- **M5-a** — the FC 20/21 **PDU layout** itself (sub-request records, reference type 6, per-record framing) was not decoded; only the component model was. [Block 131] §131.4 documented the request skeleton → **new medium gap**, distinct from the live-behaviour half that stays requires-execution.
- **M5-b** — `IPropertyValidator` on `BModbusClientPresetRegister`: what it validates and when → minor, folded into **M15**.
