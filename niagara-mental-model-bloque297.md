# Block 297 — The client point model: six ProxyExt types, three register-type enums, the 8 datatypes and their register counts, and the byte-order permutations the driver cannot express

> Focus **modbus**, gap **M3**. How a Modbus register becomes a Niagara point: which proxy extension you
> pick, how the driver derives how many registers to read from the datatype, and how the value is
> converted. The wire-level encode/decode itself is [Block 131] §131.7/§131.9 and is NOT re-derived —
> this block documents the **type model above it** and the configuration surface a point exposes.
> READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/modbusCore-rt/` (Vineflower, jar sha256 `a0b67420…`) —
> `client/point/` (10 classes), `point/` (14), `enums/` (10), `util/DataTypeUtil`, `util/ByteConverterUtil`.
> Official documentation: `sources/manuals/docModbus-N4.14-guide.md`.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official Tridium guide (§topic) ·
> `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 294] (6 client vs 3 server ProxyExt),
> [Block 295] (`determineRegisterType`/`determineNumRegisters` drive the grouping decision),
> [Block 296] (`BFlexAddress`, byte-order resolution), [Block 131] (the encoders these types feed).

---

## 297.1 — The base: two properties and two actions every Modbus point has `[CERT]`

`BModbusProxyExt extends BBasicProxyExt implements ModbusMessageConst, BIPollable` `[CERT]`
`modbusCore-rt/…/point/BModbusProxyExt.java:42` declares what is common to **both** client and server
points:

| Slot | Kind | Default |
|---|---|---|
| `pollFrequency` | property | `BPollFrequency.normal` |
| `dataAddress` | property | `BFlexAddress` |
| `forceRead` | action, flags 16 | — |
| `forceWrite` | action, flags 16 | — |

`[CERT]` `BModbusProxyExt.java:27-44`. The client layer adds two more `[CERT]`
`modbusCore-rt/…/client/point/BModbusClientProxyExt.java:24-31`:

| Slot | Default | Role |
|---|---|---|
| `absoluteAddress` | `BFlexAddress` | `dataAddress` **after** the device base-address offset of [Block 296] §296.4 is applied |
| `dataSource` | `pointPoll` | written by the driver — grouped or not ([Block 295] §295.1) |

`[INFER]` so a point carries *two* addresses: the one you type (`dataAddress`) and the one actually sent
(`absoluteAddress`). When a device base address is non-zero they differ, and it is `absoluteAddress` that
[Block 295]'s containment test and the poll request use.

## 297.2 — Three different "register type" enums, and that is deliberate `[CERT]`

The driver does not have one register-type enum; it has three, each with a different range `[CERT]`:

| Enum | Range | Used by |
|---|---|---|
| `BRegisterTypesEnum` | `holdingRegister`, `inputRegister`, `discreteCoil`, `discreteInput` (4) | the poll engine — `enums/BRegisterTypesEnum.java:12` |
| `BRegisterTypeEnum` | `holding`, `input` (2) | numeric and register-bit points — `enums/BRegisterTypeEnum.java:12` |
| `BStatusTypeEnum` | `coil`, `input` (2) | boolean points — `enums/BStatusTypeEnum.java:12` |

`[INFER]` this is a type-level constraint rather than a validation: a numeric point **cannot** be pointed at
a coil because its `regType` property has no coil value, and a boolean point cannot be pointed at a holding
register for the mirror reason. The illegal combination is unrepresentable, so no runtime check is needed.

Translation into the engine's 4-value enum happens per proxy type via `determineRegisterType()` — for the
numeric point `[CERT]` `client/point/BModbusClientNumericProxyExt.java:163-165`:

```
getRegType() == holding  →  BRegisterTypesEnum.holdingRegister
otherwise                →  BRegisterTypesEnum.inputRegister
```

That method plus `determineNumRegisters()` `[CERT]` `:172-175` are exactly the two inputs [Block 295] §295.1
uses to decide grouping, so the point's own type model is what feeds the acquisition engine.

## 297.3 — The six client proxy types `[CERT]`

| ProxyExt | Distinctive properties | Facets | Lines |
|---|---|---|---|
| `…NumericProxyExt` | `regType` (holding) · `dataType` (integerType) | — | 413 |
| `…BooleanProxyExt` | `statusType` (coil) | — | 264 |
| `…RegisterBitProxyExt` | `regType` (holding) · `bitNumber` (0) | **0..15** | 317 |
| `…NumericBitsProxyExt` | `beginningBit` (0) · `numberOfBits` (1) | **0..15** / **1..16** | 363 |
| `…EnumBitsProxyExt` | *inherits NumericBits* | — | 205 |
| `…StringProxyExt` | `numberRegisters` (1) | **1..MAX_VALUE** | 150 |

`[CERT]` property declarations at `BModbusClientNumericProxyExt.java:33-39`,
`BModbusClientBooleanProxyExt.java:27-29`, `BModbusClientRegisterBitProxyExt.java:31-38`,
`BModbusClientNumericBitsProxyExt.java:28-36`, `BModbusClientStringProxyExt.java:26-29`.

Two structural notes:

- **`EnumBitsProxyExt extends NumericBitsProxyExt`** `[CERT]`
  `BModbusClientEnumBitsProxyExt.java:21` — an enum point *is* a bit-field point with an enum facade; it
  declares no properties of its own. `[INFER]` the same `beginningBit`/`numberOfBits` window is read, then
  presented against a Niagara enum range instead of as a number.
- **Three ways to read a bit, and they are not interchangeable** `[INFER]`: `BooleanProxyExt` reads a real
  **coil/discrete input** (its own Modbus bank, FC 1/2); `RegisterBitProxyExt` reads **one bit out of a
  16-bit register** (FC 3/4) as a boolean; `NumericBits`/`EnumBits` read **a window of 1..16 bits** out of a
  register as a number/enum. Picking the wrong one aims at the wrong Modbus bank entirely.

`numberRegisters` on the string point is capped only by `Integer.MAX_VALUE` `[CERT]`
`BModbusClientStringProxyExt.java:26-29` — `[INFER]` no facet stops a nonsensical value; the fragmentation
of [Block 295] §295.4 would be what actually bounds the request.

## 297.4 — Datatype → register count, and which types are signed `[CERT]`

`DataTypeUtil` is 39 lines and is the whole mapping `[CERT]`
`modbusCore-rt/…/util/DataTypeUtil.java:6-38`:

| `BDataTypeEnum` | Registers | Signed? |
|---|---|---|
| `integerType` (**default**) | **1** | no (unsigned 16-bit) |
| `signedInteger` | 1 | **yes** |
| `longType` | 2 | **yes** |
| `unsignedLong` | 2 | no |
| `floatType` | 2 | (IEEE-754) |
| `doubleType` | **4** | (IEEE-754) |
| `signed64BitLongType` | **4** | **yes** |
| `unsigned64BitLongType` | **4** | no |

`getRegisterCount()` returns 4 for the three 64-bit types, 1 for the two 16-bit integers, and 2 for
everything else `[CERT]` `DataTypeUtil.java:6-14`; `isSigned()` is an explicit three-name list —
`signed64BitLongType`, `signedInteger`, `longType` `[CERT]` `:36-38`.

`[INFER]` two traps for an integrator: the **default `integerType` is UNSIGNED**, so a vendor register
holding a signed value reads as a large positive number until the type is changed to `signedInteger`; and
`longType` is the **signed** 32-bit type while `unsignedLong` is its unsigned pair — the naming is the
reverse of the 16-bit pair (`integerType` unsigned / `signedInteger` signed), which invites picking the
wrong one.

This is the surface [Block 296] §296.7 established has **zero documentation**: the three 64-bit types are
not mentioned anywhere in the 87-topic guide.

## 297.5 — Byte order: three permutations at 32-bit, eight at 64-bit — and one classic is missing `[CERT]`

`BDataByteOrderEnum` has exactly three values — `order1032`, `order3210`, `order0123` `[CERT]`
`modbusCore-rt/…/enums/BDataByteOrderEnum.java:12`. The tags are literal: each names the **source byte
indices in emission order**. From the writer `[CERT]`
`modbusCore-rt/…/util/ByteConverterUtil.java:38-53` (and identically for floats, `:61-76`), with the value's
bytes labelled A = MSB … D = LSB:

| Enum value | Emitted | Classic name |
|---|---|---|
| `order3210` (**property default**) | A B C D | big-endian |
| `order0123` | D C B A | little-endian |
| `order1032` (the `else` branch) | C D A B | **word swap** |
| *— absent —* | B A D C | **byte swap** — **not expressible** |

`[INFER]` this is a real integration limit: a slave that stores 32-bit values byte-swapped within
big-endian words (BADC) cannot be read correctly by any setting of this enum. The workaround would be a
`NumericBits`/raw read plus a control-logic recombination, not a driver setting.

At 64-bit the enum offers **eight** permutations `[CERT]`
`enums/BDataByteOrder64BitEnum.java:12`: `order76543210` (default, big-endian), `order01234567`
(little-endian), `order67452301`, `order54761032`, `order45670123`, `order10325476`, `order23016745`,
`order32107654`.

**16-bit values take no byte-order argument at all** — `to2ByteIntArray(double, boolean isSigned)` has no
`byteOrder` parameter and always emits MSB then LSB `[CERT]` `ByteConverterUtil.java:81,98-100`.
`[INFER]` correct by construction: Modbus defines a register as big-endian, so byte order is only a
question *between* registers, never inside one.

## 297.6 — Write conversion: round-half-away-from-zero, then clamp — with an asymmetric 32-bit floor `[CERT]`

Every writer normalises the same way `[CERT]` `ByteConverterUtil.java:23-35, 83-95`:

1. round away from zero — `value += 0.5` if non-negative, `value -= 0.5` if negative;
2. clamp into the type's range;
3. cast and emit.

The 16-bit clamp is exact `[CERT]` `:89-90`: `−32768.0 … 32767.0` signed, `0.0 … 65535.0` unsigned.

The 32-bit clamp is **not** `[CERT]` `:29-30`:

```
double minValue = isSigned ? -2.1474836E9F : 0.0;      // <-- float literal
double maxValue = isSigned ?  2.147483647E9 : 4.294967295E9;   // <-- double literal
```

`[INFER]` the floor is written as a `float`, which cannot represent −2 147 483 648; the nearest float is
≈ −2 147 483 600. The ceiling next to it is an exact double. So **signed 32-bit writes are clamped ~48
units short of `Integer.MIN_VALUE`**: values in roughly `[−2147483648, −2147483601]` are silently raised to
the clamp floor. The bug is in the *write* path only, is invisible without an oracle, and matters solely at
the extreme negative edge — recorded here rather than escalated. Confirmed by reading both literals in the
same expression; not reproduced against a live device (that would need the dynamic phase).

## 297.7 — What the guide covers, and what it does not

The guide does document the *workflow* of creating points — §Creating client proxy points,
§New Point Type Window, §New Point Properties Window `[CERT-doc]` — i.e. which dialog fields to fill. What
it does not resolve, cross-checked against §297.1–§297.6:

- the **two-address model** (`dataAddress` vs `absoluteAddress`) of §297.1;
- **why there are three register-type enums** and that the constraint is structural (§297.2);
- the **datatype → register-count table** of §297.4 — an integrator cannot look up that `doubleType`
  consumes 4 registers;
- that **`integerType`, the default, is unsigned** (§297.4);
- the **byte-order permutation set**, and that BADC is not expressible (§297.5);
- **anything 64-bit** — re-confirming [Block 296] §296.7's zero-hit measurement.

## 297.8 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 41 | 40 |
| `[CERT-doc]` | 2 | 1 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 10 | 9 |
| **[INFER]/[CERT*] ratio** | | **9/41 = 0.22** |

Script exit 0; citations resolve as `extern` (decompiled tree) and were token-checked by reading.
Note the low `[CERT-doc]` count: this gap is one the official guide barely touches (§297.7), so the
evidence is almost entirely code — the opposite balance to [Block 294].

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | Base ProxyExt: `pollFrequency`, `dataAddress` + `forceRead`/`forceWrite` actions | `[CERT]` | `BModbusProxyExt.java:27-44` |
| 2 | Client adds `absoluteAddress` + `dataSource` | `[CERT]` | `BModbusClientProxyExt.java:24-31` |
| 3 | Three distinct register-type enums with ranges 4/2/2 | `[CERT]` | the three `@Range` declarations, enumerated |
| 4 | `determineRegisterType()` maps holding→holdingRegister else inputRegister | `[CERT]` | `BModbusClientNumericProxyExt.java:163-165` |
| 5 | Six client ProxyExt with the listed facets | `[CERT]` | each file's `@NiagaraProperty` block |
| 6 | `EnumBits extends NumericBits`, declares no own properties | `[CERT]` | `BModbusClientEnumBitsProxyExt.java:21` + empty property grep |
| 7 | Register counts 1/2/4 per datatype | `[CERT]` | `DataTypeUtil.java:6-14` |
| 8 | `isSigned()` lists exactly three types | `[CERT]` | `DataTypeUtil.java:36-38` |
| 9 | 32-bit byte order has exactly 3 values | `[CERT]` | `BDataByteOrderEnum.java:12` |
| 10 | The three map to ABCD / DCBA / CDAB | `[CERT]` | bit-shift masks read in `ByteConverterUtil.java:38-53` |
| 11 | BADC is absent from the enum | `[CERT]` | exhaustive enumeration of the 3-value range (re-measured against the float writer at `:61-76`, same three branches) |
| 12 | 64-bit byte order has 8 values | `[CERT]` | `BDataByteOrder64BitEnum.java:12` |
| 13 | 16-bit conversion takes no byte-order argument | `[CERT]` | `ByteConverterUtil.java:81` signature |
| 14 | Round-away-from-zero then clamp | `[CERT]` | `:23-35`, `:83-95` |
| 15 | 32-bit signed floor is a float literal, ceiling a double | `[CERT]` | `:29-30` verbatim |

Tokens grep-confirmed in their cited source: **15 / 15**. Per the RE-MEASURE rule, claim 11 (an absence) was
derived twice — from the enum range and independently from the writer's three-branch dispatch, which has no
fourth case. No new sources preserved. Model tier: **no delegation — inline**.

## 297.x — Connections

- **[Block 294]** — §294.4 counted 6 client ProxyExt vs 3 server; this block names and characterises the six.
- **[Block 295]** — `determineRegisterType()`/`determineNumRegisters()` (§297.2) are the grouping inputs; `dataSource` (§297.1) is its output indicator.
- **[Block 296]** — `absoluteAddress` is `dataAddress` plus that block's base offsets; the byte orders of §297.5 are what its §296.1 override switch selects between.
- **[Block 131]** — §131.7/§131.9 document the wire encoders; §297.5 adds which permutations are *offered* and which classic one is not.

**Gaps opened by this block**:
- **M3-a** — the **read/decode** path per proxy type (`devicePoll(entry)` slicing the shared buffer, and the `readUnsubscribed` single-point path) was not opened; only the type model and the *write* converters were → **new medium gap**.
- **M3-b** — `BModbusClientStringProxyExt` (150 lines) and its relation to file records / `BModbusStringRecord` → folded into **M5**.
