# Block 137 — INTEGRATION PLAN: Siemens LOGO! 8 (Modbus TCP slave) ↔ Niagara N4 (modbusTcp master) — driver component tree, register-map crosswalk, byte-order reconciliation, Workbench wiring (DESIGN; empirical validation DEFERRED)

> **APPLIED / INTEGRATION block (Nivel 1 — design).** This is NOT a wire-RE block and NOT an
> empirical/live block: no hardware was touched in this iteration. It is the **integration plan** for
> connecting the bench Siemens **LOGO! 8** PLC (`192.168.0.100:502`, Modbus TCP **server/slave**) to a
> Niagara **N4.14** station acting as Modbus TCP **master/client** via the `modbusTcp` driver. It fuses
> two existing corpora: (a) the Niagara `modbusTcp`/`modbusCore` **driver CONFIGURATION classes**
> decompiled here (the component tree a user actually wires in Workbench — network, device, points,
> address/datatype/byte-order slots), and (b) the **LOGO! 8 Modbus register map** already
> hardware-confirmed in the `logosoft` corpus (B66–B76). The deliverable is: a topology, a
> LOGO↔Niagara register-map crosswalk, the byte/word-order reconciliation, a step-by-step Workbench
> recipe with exact slot values, and a DEFERRED empirical-validation plan to run at the bench.
> READ-ONLY. Corpus language: ENGLISH.
>
> **Epistemic honesty (read this first).** The Niagara configuration facts are `[CERT]` (decompiled
> code, `file:line`). The LOGO facts are cited to the `logosoft` corpus with that corpus's own marker
> (`[CERT-hw]` = confirmed against the live bench LOGO; `[CERT]` = decompiled LOGO!Soft source;
> `[INFER]` = logosoft deduction). **The reconciliation itself — "these two configs will interoperate
> correctly" — is `[INFER]`** until validated on the wire against the real bench. A higher `[INFER]`
> ratio is EXPECTED and HONEST here: this is a design plan, not an empirical result. Do not read any
> `[INFER]` in this block as confirmed behavior.
>
> Sources (primary):
> Niagara driver (decompiled with Vineflower from the live install, reused from B131):
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/modbusTcp-rt.jar` (sha256 688bb50b…),
> `modbusCore-rt.jar` (sha256 a0b67420…) — preserved under `sources/decompiled/modbusTcp-rt/` and
> `sources/decompiled/modbusCore-rt/`, registered in `sources/SOURCES.md` (B131).
> LOGO! map: `logosoft` corpus blocks **B66, B67, B68, B69, B70, B72, B73, B75, B76** (Spanish).
> Method: read the `modbusTcp`/`modbusCore` **config** layer (network/device/point/datatype/enum
> classes — distinct from B131's message-wire layer) + cross-read the logosoft register map + `grep`
> token confirmation. Markers:
> `[CERT]` local primary source (`file:line`) · `[CERT-hw]` live-hardware-confirmed in the cited
> corpus · `[CERT-doc]` downloaded doc · `[CERT-web]` official web · `[CERT-a]` secondary · `[INFER]`
> deduction / predicted-not-yet-validated integration behavior.
>
> Layer 26 (Communication protocols) — **applied integration view**. Connects [Block 131] (the Modbus
> WIRE this config serializes into — MBAP, FCs, addressing, byte-order code), [Block 7] (driver
> Network/Device/Point framework), [Block 94] (the byte-order word-swap gotcha at integration level),
> and the `logosoft` LOGO! Modbus corpus (B66–B76).

---

## 137.1 — Topology & the integration thesis `[CERT]` / `[INFER]`

The LOGO! 8 is the Modbus TCP **server (slave)**; Niagara is the Modbus TCP **client (master)**. There
is exactly one TCP socket, opened by Niagara to `192.168.0.100:502`.

```
   ┌─────────────────────────────┐         Modbus TCP (MBAP, FC 1/2/3/5)          ┌────────────────────────────┐
   │  Niagara N4.14 Station       │   TCP   over port 502, Niagara opens socket    │  Siemens LOGO! 8           │
   │  modbusTcp DRIVER (MASTER)   │ ───────────────────────────────────────────▶ │  Modbus TCP SERVER (SLAVE) │
   │                              │                                                │  192.168.0.100 : 502       │
   │  BModbusTcpNetwork           │   poll: ReadCoils(1)/ReadDiscreteIn(2)/        │  unit id: ignored/any      │
   │   └ BModbusTcpDevice         │         ReadHoldingReg(3)                      │                            │
   │      (ip=192.168.0.100,      │   write: WriteSingleCoil(5) → free markers M  │  I  → discrete inputs (FC2)│
   │       port=502, unit=1)      │ ◀───────────────────────────────────────────  │  Q  → coils RO     (FC1)   │
   │      └ points (Boolean/      │            responses (big-endian regs)         │  M  → coils RW     (FC1/5) │
   │         Numeric proxy ext)   │                                                │  VW → holding regs (FC3)   │
   └─────────────────────────────┘                                                └────────────────────────────┘
```

- **Direction of connect**: Niagara is the only initiator. `BModbusTcpNetwork.getModbusMode()` returns
  `2` (= TCP) `[CERT]` `modbusTcp-rt/.../BModbusTcpNetwork.java:59-61`; the device opens the socket
  (`ModbusTcpRxDriver.initSocketConnection()` via `sendModbusMessage`) `[CERT]`
  `modbusTcp-rt/.../BModbusTcpDevice.java:240-256`.
- **The LOGO server must be enabled** — the Modbus TCP server is serialized INTO the LOGO program image
  (MRAM, the `EecpNetConfig` segment) and only persists if the enabled program was transferred; a
  power-cycle alone does not re-enable it `[CERT-hw]` logosoft B70 §70.5 / B76 §76.1. The bench was
  **left with Modbus enabled** `[CERT-hw]` logosoft B66 §66.1, so this precondition currently holds.
- **The thesis** (this whole block): with the slot values in §137.7 and the byte-order finding in
  §137.6, a default-configured `BModbusTcpNetwork` will correctly poll the LOGO map of §137.4. This is
  `[INFER]` — predicted from both code bases, **not yet validated on the wire** (see §137.9).

## 137.2 — The Niagara driver component tree a user configures `[CERT]`

The user builds a 3-level tree (Network ▸ Device ▸ Points) under `Drivers`. Every configurable slot
below is a real `@NiagaraProperty` in the decompiled config classes (this is the layer B131 did NOT
cover — B131 documented the bytes these slots serialize into).

### 137.2.1 — `BModbusTcpNetwork` (the master) `[CERT]`

| Slot | Type | Default | Citation |
|---|---|---|---|
| `socketOptionTimeout` | `BRelTime` | **1 minute** (min 1) | `BModbusTcpNetwork.java:19-26` |
| `responseTimeout` (inherited) | `BRelTime` | **2000 ms** (set in ctor) | `BModbusTcpNetwork.java:55-57` |
| `floatByteOrder` (inherited) | `BDataByteOrderEnum` | **`order3210`** | `modbusCore-rt/.../BModbusNetwork.java:24-26, 41` |
| `longByteOrder` (inherited) | `BDataByteOrderEnum` | **`order3210`** | `BModbusNetwork.java:28-30, 42` |
| `double64BitByteOrder` / `long64BitByteOrder` (inherited) | `BDataByteOrder64BitEnum` | `order76543210` | `BModbusNetwork.java:31-44` |
| `usePresetMultipleRegister` | boolean | `false` (→ FC 6 for single-reg writes) | `BModbusClientNetwork.java:17-19, 32` |
| `useForceMultipleCoil` | boolean | `false` (→ **FC 5** for coil writes) | `BModbusClientNetwork.java:20-22, 33` |
| `maxFailsUntilDeviceDown` | int | `2` | `BModbusClientNetwork.java:23-29, 34` |

`getModbusMode()=2` (TCP) and `getDeviceType()=BModbusTcpDevice` `[CERT]` `BModbusTcpNetwork.java:43-44,
59-61`. (The byte-order defaults are the heart of §137.6 — note they are `order3210`, big-endian, NOT
the `order1032` the enum names as its DEFAULT constant.)

### 137.2.2 — `BModbusTcpDevice` (one per LOGO) `[CERT]`

| Slot | Type | Default | Range / note | Citation |
|---|---|---|---|---|
| `ipAddress` | String | `"###.###.###.###"` (= unset sentinel `DEFAULT_IP`) | set to `192.168.0.100` | `BModbusTcpDevice.java:28-31, 59, 174-188` |
| `port` | int | **502** | 0–65535 | `BModbusTcpDevice.java:32-36, 60` |
| `deviceAddress` (unit/slave id) | int | **1** | 0–255 (overridden wider than core's 1–247) | `BModbusTcpDevice.java:22-27, 58` |
| `disableTransactionIdCheck` | boolean | `false` | keep false | `BModbusTcpDevice.java:42-45, 62` |
| `maxTransactionId` | int | `65535` | MBAP txn wrap | `BModbusTcpDevice.java:46-50, 63` |
| `rxProcessMode` | boolean | `false` (= **byte** mode) | packet/byte | `BModbusTcpDevice.java:51-56, 64` |
| `holdingRegisterBaseAddress` / `inputRegisterBaseAddress` / `coilStatusBaseAddress` / `inputStatusBaseAddress` (inherited) | `BFlexAddress` | empty (`0`) | per-space additive offset; **leave 0** for the LOGO | `BModbusClientDevice.java:67-100, 345-361` |
| `modbusConfig` (inherited) | `BModbusConfig` | `overrideNetwork=false` | per-device byte-order override (defaults same `order3210`) | `BModbusDevice.java:18-31, 64-82`; `BModbusConfig.java:15-41` |

The unit id goes on the wire as MBAP byte 6 (B131 §131.3); the LOGO server does not enforce a unit id
`[INFER]` logosoft B66 (none specified for the server; B73 §73.3 showed unit `7` only in a *different*
network-client use case), so `deviceAddress=1` is the safe default `[INFER]`.

### 137.2.3 — Point proxy extensions (one per LOGO datum) `[CERT]`

Points are Niagara control points (`BBooleanWritable`/`BNumericWritable` etc.) whose **proxy ext**
carries the Modbus address + type. Two proxy classes matter for the LOGO:

**`BModbusClientBooleanProxyExt`** (for I / Q / M bits) `[CERT]` `BModbusClientBooleanProxyExt.java`:

| Slot | Type | Default | Effect | Citation |
|---|---|---|---|---|
| `statusType` | `BStatusTypeEnum {coil, input}` | **`coil`** | read FC: `coil`→**FC 1**, `input`→**FC 2** | `:25-32, 62-76, 107-110` |
| `dataAddress` (inherited) | `BFlexAddress` | `hex:0` | the address (see §137.5) | `BModbusProxyExt.java:26-34` |
| `pollFrequency` (inherited) | `BPollFrequency` | `normal` | poll bucket | `BModbusProxyExt.java:26-30` |

Coil write path: `WriteSingleCoil` **FC 5** by default (`0xFF00`=ON, `0x0000`=OFF), or FC 15 if
`useForceMultipleCoil` `[CERT]` `BModbusClientBooleanProxyExt.java:224-249`.

**`BModbusClientNumericProxyExt`** (for VW holding registers) `[CERT]`
`BModbusClientNumericProxyExt.java`:

| Slot | Type | Default | Effect | Citation |
|---|---|---|---|---|
| `regType` | `BRegisterTypeEnum {holding, input}` | **`holding`** | read FC: `holding`→**FC 3**, `input`→**FC 4** | `:31-43, 84-97, 162-165` |
| `dataType` | `BDataTypeEnum` | **`integerType`** (16-bit) | register count + decode | `:31-43`; `DataTypeUtil.java:6-18` |
| `dataAddress` (inherited) | `BFlexAddress` | `hex:0` | the register address | `BModbusProxyExt.java:26-34` |

`dataType` register width `[CERT]` `DataTypeUtil.java:6-14`: `integerType`/`signedInteger` = **1 register
(16-bit)**; `longType`/`unsignedLong`/`floatType` = 2 registers (32-bit); `double`/`64BitLong` = 4
registers (64-bit). Write uses FC 6 (or FC 16 if `usePresetMultipleRegister`) `[CERT]`
`BModbusClientNumericProxyExt.java:348-354`.

## 137.3 — The LOGO! 8 Modbus map (from the logosoft corpus) `[CERT-hw]`

Established against the **live bench LOGO!** in the logosoft corpus. The decisive property for the
crosswalk: **the LOGO exposes RAW, 0-based PDU addresses** (not the 1-based `1xxxx/3xxxx/4xxxx`
convention), and its Modbus registers are **big-endian** 16-bit words.

| LOGO area | Modbus object | PDU address (0-based) | LOGO FC | Width / order | Marker |
|---|---|---|---|---|---|
| `I1..In` digital inputs | Discrete Input (1x) | `n-1` (I1=0) | FC 2 read | 1 bit | `[CERT-hw]` logosoft B66 §66.3 |
| `Q1..Qn` digital outputs | Coil (0x) | `8192 + (n-1)` (Q1=8192) | FC 1 read | 1 bit, **read-only** (program drives Q) | `[CERT-hw]` logosoft B66 §66.3 |
| `M1..Mn` markers | Coil (0x) | `8256 + (n-1)` (M1=8256) | FC 1 read / **FC 5 write** | 1 bit; `0xFF00`/`0x0000`; **only FREE markers persist** | `[CERT-hw]` logosoft B66 §66.3 (M7=8262, M9=8264 round-tripped) |
| `VW` / V-memory (analog & network words) | Holding Register (4x) | register `k` ⇒ VM bytes `[851+2k, 851+2k+1]` | FC 3 read/write | **16-bit BIG-ENDIAN** | `[CERT-hw]` logosoft B67 §67.3-§67.4 (reg 0 read `0xfe0c`; VW200 write `0x1234`→`12 34`) |

Operational facts that constrain the Niagara design `[CERT-hw]` logosoft:
- **One concurrent Modbus master only** — the LOGO server serves exactly ONE live Modbus session; a 2nd
  master is TCP-accepted but reset at the Modbus layer on first read `[CERT-hw]` logosoft B68 §68.2.
- **Latency** — a single FC 1/FC 3 round-trip ≈ 0.57 ms median `[CERT-hw]` logosoft B69 §69.1-§69.3.
- **Analog little-endian caveat is RPC-ONLY, not Modbus.** logosoft B75 §75.5 found the LOGO's *RPC*
  (port 10005 `GetAVB`) returns analog as signed **little-endian**; that channel is irrelevant here —
  over **Modbus FC 3** the same V-memory is **big-endian** `[CERT-hw]` logosoft B67 §67.1. Niagara
  speaks Modbus, so it sees big-endian. Do not import the RPC little-endian rule into the Modbus config.

## 137.4 — The register-map crosswalk (the heart of the block) `[CERT]` / `[INFER]`

Mapping each LOGO datum to a concrete Niagara point. **Recommended addressing = `decimal` format with
the RAW PDU address** (1:1 with the logosoft map; the point type is set explicitly). The `[INFER]` is
that this configuration reads/writes the intended LOGO datum — predicted, not yet wire-validated.

| LOGO datum | LOGO object / PDU addr | LOGO FC | Niagara proxy ext | `statusType`/`regType` | `dataType` | Niagara `dataAddress` (decimal) | FC Niagara emits | Cite (Niagara · LOGO) |
|---|---|---|---|---|---|---|---|---|
| `I1` (input) | Discrete Input / `0` | 2 | `…BooleanProxyExt` (read-only point) | `input` | — | `decimal:0` | **FC 2** | `BoolProxy.java:62-76,107-110` · B66 §66.3 |
| `In` | Discrete Input / `n-1` | 2 | Boolean RO | `input` | — | `decimal:n-1` | FC 2 | idem |
| `Q1` (output) | Coil / `8192` | 1 | Boolean **read-only** point | `coil` | — | `decimal:8192` | **FC 1** | `BoolProxy.java:62-76,170-172` · B66 §66.3 |
| `Qn` | Coil / `8192+(n-1)` | 1 | Boolean RO | `coil` | — | `decimal:8192+(n-1)` | FC 1 | idem |
| `M1` (free marker) | Coil / `8256` | 1 / **5** | Boolean **Writable** point | `coil` | — | `decimal:8256` | FC 1 read, **FC 5 write** | `BoolProxy.java:224-249` · B66 §66.3 |
| `Mn` | Coil / `8256+(n-1)` | 1 / 5 | Boolean Writable | `coil` | — | `decimal:8256+(n-1)` | FC 1 / FC 5 | idem |
| `VW0` (16-bit word) | Holding / reg `0` | 3 | `…NumericProxyExt` | `holding` | `integerType` (unsigned) or `signedInteger` | `decimal:0` | **FC 3** | `NumProxy.java:84-97,162-165` · B67 §67.3 |
| `VWk` | Holding / reg `k` (=VM`851+2k`) | 3 | Numeric | `holding` | `integerType`/`signedInteger` | `decimal:k` | FC 3 | idem |

Notes:
- **Address-space disambiguation**: `I1` (discrete-input PDU 0) and `VW0` (holding PDU 0) share the raw
  number `0` but live in **different Modbus object spaces** — Niagara separates them by point type
  (`statusType=input`→FC 2 vs `regType=holding`→FC 3), exactly as standard Modbus does `[CERT]`
  (the FC, not the address, selects the space; `BModbusClientBooleanProxyExt.java:70-74` vs
  `BModbusClientNumericProxyExt.java:91-95`).
- **Q is read-only over Modbus** `[CERT-hw]` logosoft B66 §66.3 — make Q points read-only (no write
  action); the LOGO program owns Q.
- **Marker writes are persistent state, not pulses, and only land on FREE markers** `[CERT-hw]` logosoft
  B66 §66.3 — a marker driven by a block is overwritten next scan. Model M writes as state changes.
- For a **VW value the LOGO program stores as 32-bit** (a VD double-word across two consecutive
  registers), use `dataType=longType` (2 registers) and see §137.6 for the order — but the LOGO's 32-bit
  Modbus layout is itself only `[INFER]` in logosoft (extrapolated, not bench-measured) logosoft B67.

## 137.5 — Addressing reconciliation: `BFlexAddress` raw-PDU vs Modbus-1based `[CERT]`

`BFlexAddress.getDataAddress()` converts the configured address to the **0-based wire address** Niagara
puts in the PDU `[CERT]` `BFlexAddress.java:149-166` (consistent with B131 §131.8):

| `addressFormat` | What you type | Wire (PDU) address | Type auto-derived? |
|---|---|---|---|
| `decimal` (**recommended for LOGO**) | the raw PDU number (`0`, `8192`, `8256`, reg `k`) | **used as-is** | **No** — set `statusType`/`regType` manually |
| `hex` | hex of the raw PDU number (`0x2000`=8192) | used as-is | No |
| `modbus` (1-based convention) | `10001+`/`8193`/`8257`/`40001+k` | subtract `10001`/`1`/`1`/`40001` | **Yes** (coil/input/holding inferred from range) |

Because the LOGO publishes **raw PDU offsets**, the **decimal/hex** path maps 1:1 to the logosoft numbers
and is the least error-prone. The `modbus` path also works and auto-types the point, but you must
re-encode every address to 1-based: `Q1`→`modbus:8193`, `M1`→`modbus:8257`, `I1`→`modbus:10001`,
`VW0`→`modbus:40001` `[CERT]` `BFlexAddress.java:149-166` (coil space `<10000`, status `10001-19999`,
holding `40001-49999`; auto-typing in `BModbusClientNumericProxyExt.isValidAddress:230-249` /
`BModbusClientBooleanProxyExt.isValidAddress:149-168`). **Leave all four device `…BaseAddress` slots at 0**
so no offset is added on top (`setCurrentAbsoluteAddress` adds `getRegisterBaseAddress`) `[CERT]`
`BModbusClientProxyExt.java:216-251`, `BModbusClientDevice.java:345-361`.

## 137.6 — Byte/word-order reconciliation (the #1 practical gotcha) `[CERT]` / `[INFER]`

This is the single most likely thing to corrupt values, and it is where this block REFINES B131.

**(a) 16-bit single registers (the dominant LOGO case: VW, and all I/Q/M bits).**
- LOGO side: **big-endian** (MSB first) `[CERT-hw]` logosoft B67 §67.1, §67.4 (`VW200=0x1234` measured
  on the wire as `12 34`).
- Niagara side: 16-bit register decode/encode is **fixed plain big-endian** `(hi,lo)` — there is **no
  byte-order slot** for 16-bit values `[CERT]` B131 §131.9 / `ByteConverterUtil.java:81-101`.
- **Verdict: MATCH, no override needed** `[INFER]` (predicted; bench-confirm in §137.9). Bits carry no
  byte order, so I/Q/M match trivially.

**(b) 32-bit values (only if a LOGO program packs a VD double-word across two consecutive registers).**
- Niagara 32-bit order is selectable via `BDataByteOrderEnum {order1032(BADC, swap-within-register),
  order3210(ABCD full big-endian), order0123(DCBA full little-endian)}` `[CERT]`
  `BDataByteOrderEnum.java:11-21`, applied per device/network (`BModbusDevice.getFloatDataByteOrder()` →
  config-override or network default) `[CERT]` `BModbusDevice.java:64-82`.
- LOGO 32-bit layout: **big-endian, high register first = ABCD = `order3210`** `[INFER]` logosoft B67
  (extrapolated from the 16-bit big-endian rule; NOT bench-measured — flag both-sides-inferred).

> **REFINEMENT of B131 §131.9 `[CERT]`.** B131 highlighted that the *enum's* DEFAULT constant is
> `order1032` (BADC) — `BDataByteOrderEnum.DEFAULT = order1032` `[CERT]` `BDataByteOrderEnum.java:21` —
> framing it as "the driver default, not big-endian." But the **configurable network/device PROPERTY
> defaults are `order3210` (full big-endian ABCD)**, not `order1032`: `BModbusNetwork.floatByteOrder`
> and `longByteOrder` default to `order3210` `[CERT]` `BModbusNetwork.java:26, 30, 41-42`, and the
> per-device `BModbusConfig` mirrors that (`overrideNetwork=false`, `order3210`) `[CERT]`
> `BModbusConfig.java:18-39`. So a **freshly dropped `BModbusTcpNetwork` reads 32-bit values as
> big-endian (ABCD)**, not BADC. The `order1032` "gotcha" bites only if a value's order slot is at the
> enum-DEFAULT (e.g. some constructed/legacy configs) — it is NOT what a default Workbench network uses.
> This reconciles B131's code-level finding with the integration reality and partially explains why B94's
> word-swap is a *misconfiguration* symptom, not the out-of-box behavior.

- **Verdict for 32-bit LOGO values: predicted MATCH at the DEFAULT `order3210`** `[INFER]` (both sides
  big-endian). **Action: leave `longByteOrder`/`floatByteOrder` at the default `order3210`.** Override to
  `order1032` ONLY if a live read shows a word-swap (registers correct individually but the 32-bit value
  is BADC-scrambled). This is `[INFER]` until §137.9.

**(c) Ignore the RPC little-endian analog rule** — it applies to the LOGO's RPC channel, not Modbus
(§137.3) `[CERT-hw]` logosoft B75 §75.5 vs B67 §67.1.

## 137.7 — Step-by-step Workbench configuration (exact slot values) `[CERT]`

Every value below is the decompiled slot from §137.2; `[INFER]` markers flag the choices whose
correctness depends on the (deferred) bench validation.

1. **Add the network.** `Drivers` ▸ New ▸ `ModbusTcpNetwork`. Leave `socketOptionTimeout=1min`,
   `responseTimeout=2000ms`, `floatByteOrder=order3210`, `longByteOrder=order3210`,
   `useForceMultipleCoil=false` (so marker writes use **FC 5**), `usePresetMultipleRegister=false`
   `[CERT]` `BModbusTcpNetwork.java:19-26, 55-57`, `BModbusNetwork.java:41-42`,
   `BModbusClientNetwork.java:32-33`.
2. **Add the device.** Under the network, New ▸ `ModbusTcpDevice`. Set:
   - `ipAddress = 192.168.0.100` `[CERT]` `BModbusTcpDevice.java:28-31` (replace the `###.###.###.###`
     sentinel).
   - `port = 502` (default) `[CERT]` `:32-36`.
   - `deviceAddress = 1` (unit id; LOGO ignores it) `[CERT]` `:22-27` / `[INFER]` (unit-id-irrelevance).
   - Leave `disableTransactionIdCheck=false`, `rxProcessMode=byte`, all four `…BaseAddress` empty
     `[CERT]` `:42-56`, `BModbusClientDevice.java:67-100`.
3. **Set a ping address** (so device health works): `pingAddress = decimal:0`,
   `pingAddressRegType = holding` → pings `VW0` via FC 3 `[CERT]` `BModbusClientDevice.java:50-61`
   (`[INFER]` that VW0 is readable on this LOGO program).
4. **Add points** (drag a `BooleanWritable`/`NumericWritable` into the device's `Points`, or use the
   Modbus point manager). For each, set the proxy ext slots per §137.4:
   - **Marker M7 (writable)**: Boolean point, proxy `statusType=coil`, `dataAddress=decimal:8262`
     → reads FC 1, writes FC 5 (`0xFF00`/`0x0000`) `[CERT]` `BModbusClientBooleanProxyExt.java:62-76,
     224-249` (LOGO M7=8262 `[CERT-hw]` logosoft B66 §66.3).
   - **Output Q1 (read-only)**: Boolean read-only point, `statusType=coil`, `dataAddress=decimal:8192`
     → FC 1 `[CERT]` `:62-76` (LOGO Q1=8192 `[CERT-hw]` B66 §66.3).
   - **Input I1 (read-only)**: Boolean read-only, `statusType=input`, `dataAddress=decimal:0` → FC 2
     `[CERT]` `:62-76` (LOGO I1=0 `[CERT-hw]` B66 §66.3).
   - **Register VW0**: Numeric point, `regType=holding`, `dataType=signedInteger` (or `integerType` for
     unsigned), `dataAddress=decimal:0` → FC 3, 1 register, big-endian `[CERT]`
     `BModbusClientNumericProxyExt.java:84-97`; `DataTypeUtil.java:16-18` (LOGO VW0 big-endian
     `[CERT-hw]` logosoft B67 §67.3).
5. **Poll rate**: keep `pollFrequency=normal` and set a conservative tuning policy. The bench sustains
   sub-ms reads `[CERT-hw]` logosoft B69, but operations should poll ≤2 Hz `[INFER]`.

## 137.8 — Operational constraints carried from the LOGO `[CERT-hw]` / `[INFER]`

- **Single Modbus master** `[CERT-hw]` logosoft B68 §68.2 — Niagara's `BModbusTcpDevice` must be the
  ONLY Modbus client on `192.168.0.100:502`. Any second master (a test poller, a SCADA) will be reset by
  the LOGO. Plan a single station, single device, multiplexed polls (the driver already serializes via
  poll groups `[CERT]` `BModbusClientProxyExt.java:68-105`).
- **Server-enable is not remotely settable** `[CERT-hw]` logosoft B70 §70.5 / B76 — Niagara cannot turn
  the LOGO Modbus server on; it requires a LOGO!Soft Comfort program re-transfer. The bench is already
  enabled `[CERT-hw]` B66 §66.1.
- **Marker write semantics** `[CERT-hw]` logosoft B66 §66.3 — only FREE markers hold a Modbus write;
  pick M points that the LOGO program does not drive.

## 137.9 — Empirical-validation plan (DEFERRED — Nivel-3 follow-up, NOT done here)

This block is design only. The following checks are the DYNAMIC (METHODOLOGY §12) follow-up to run at
the bench; each would upgrade a §137.6/§137.4 `[INFER]` to `[CERT-hw]`. **None were executed in this
iteration.**

1. **Connectivity & FC** — point Niagara at `192.168.0.100:502`, confirm the device pings (FC 3 on
   `VW0`) and capture the live frame (Wireshark) to confirm the **FC** Niagara emits matches §137.4
   (FC 1/2/3/5). Re-measure the LOGO IP/port LIVE (do not inherit `192.168.0.100` — METHODOLOGY §12
   "re-measure ground-truth").
2. **Known-register read** — set a known value in a LOGO `VW` via LOGO!Soft (or a known `I`/`Q` state),
   read it in Niagara, confirm the value AND that big-endian decode is correct (validates §137.6a).
3. **Byte-order live confirm** — if any 32-bit VD is mapped, read it at the default `order3210` and check
   for a word-swap; flip to `order1032` only if scrambled (validates the §137.6b refinement on the wire).
4. **Marker write round-trip** — write a FREE marker (e.g. M7) with FC 5, read it back, and confirm the
   LOGO logic responds (the logosoft B66 §66.4 RS-latch oracle) — confirms Niagara's FC 5 path end to end.
5. **Single-master behavior** — confirm Niagara holds the sole Modbus session and degrades gracefully if
   the slot is taken (validates §137.8 against `maxFailsUntilDeviceDown`).

Until checks 1–5 pass, the §137.1 thesis ("it will interoperate correctly") remains `[INFER]`.

## 137.10 — Self-verify

- **Token check** — load-bearing `[CERT]` Niagara tokens grep-confirmed in the decompiled sources this
  iteration: `BModbusTcpNetwork.java` (`getModbusMode`→`return 2` :60; `socketOptionTimeout` :19-26;
  `responseTimeout 2000` :56); `BModbusTcpDevice.java` (`port`/`502` :33-36,60; `deviceAddress`/`0,255`
  :22-27,58; `ipAddress`/`DEFAULT_IP` :28-31,59); `BModbusNetwork.java` (`floatByteOrder`/`longByteOrder`
  = `order3210` :24-30,41-42); `BModbusConfig.java` (`order3210` :18-39); `BDataByteOrderEnum.java`
  (range `order1032/order3210/order0123` + `DEFAULT=order1032` :11-21); `BModbusClientNetwork.java`
  (`useForceMultipleCoil`/`usePresetMultipleRegister` :17-33); `BModbusClientBooleanProxyExt.java`
  (`statusType=coil` :25-32; read `code=1`/`code=2` :70-74; write `code=5`/`code=15`, `0xFF00` :232-245);
  `BModbusClientNumericProxyExt.java` (`regType=holding`/`dataType=integerType` :32-43; read `code=3`/`4`
  :91-95); `BFlexAddress.java` (`getDataAddress` modbus subtract :149-166); `DataTypeUtil.java`
  (`getRegisterCount` :6-18); `BModbusClientProxyExt.java`/`BModbusClientDevice.java` (base-address add
  :216-251 / :345-361). **18/18 Niagara token groups confirmed present.** LOGO citations are to the
  `logosoft` corpus (B66 §66.3, B67 §67.1/§67.3-4, B68 §68.2, B69, B70 §70.5, B75 §75.5, B76 §76.1) and
  carry that corpus's `[CERT-hw]`/`[CERT]`/`[INFER]` markers verbatim — spot-checked: B66 (I=n-1/Q=8192+/
  M=8256+ PDU coil map), B67 (V=851 big-endian FC3), B68 (1 master) resolve to the cited sections.
- **Marker tally** — ≈ **41 `[CERT]`** (Niagara code) · ≈ **17 `[CERT-hw]`** + ≈ **5 `[CERT]`** carried
  from logosoft · 0 `[CERT-doc]`/`[CERT-web]`/`[CERT-a]` · ≈ **22 `[INFER]`** (predicted, unvalidated
  integration behavior). **`[INFER]`/`[CERT]` ratio ≈ 0.48** (against the ≈46 first-party + carried
  `[CERT]`/`[CERT-hw]`). **This high ratio is EXPECTED and HONEST: B137 is a DESIGN block — the
  configuration facts are certain, but the "they interoperate" conclusion is inference until the bench
  validates it (§137.9).** No `[CERT]` was padded to lower the ratio.
- **Artifacts** — block file written; reuses `sources/decompiled/modbusTcp-rt/` + `modbusCore-rt/`
  (registered in `SOURCES.md` by B131, no new decompile); `INDEX.md` B137 row added (flagged
  applied/design, NOT a protocols static-loop reopen — that stays 6/6 STOPPED);
  `RESEARCH-STATE-protocols.md` applied-integration coda added without changing the 6/6 stop;
  `CATALOG.md` regenerated; engram mirror `research/niagara/logo-integration`.

## 137.x — Connections

- **[Block 131]** — Modbus WIRE encoding. B131 documented the bytes (MBAP, FC PDUs, `BFlexAddress`
  0-based wire conversion, the `BDataByteOrderEnum` codec); B137 is the CONFIG layer that drives those
  bytes (the network/device/point slots a user sets) and APPLIES it to a concrete device (the LOGO). B137
  **refines B131 §131.9**: the `order1032` BADC "default" is the enum constant, but the network/device
  byte-order PROPERTY default is `order3210` (big-endian) — so a default Niagara network is big-endian.
- **[Block 7]** — Drivers framework (Network/Device/Point/ProxyExt). B137 instantiates B7's 3-level
  hierarchy for one concrete integration with the exact Modbus subclasses and slot values.
- **[Block 94]** — Modbus byte-order word-swap gotcha. B137 §137.6 ties it to the integration: the
  word-swap is a *misconfiguration* (order slot left at `order1032`), not the out-of-box default
  (`order3210`); for the big-endian LOGO the default already matches.
- **logosoft B66–B76** — the LOGO! 8 Modbus register map (I/Q/M coil PDU addresses, VW holding registers
  at V=851 big-endian, single-master limit, server-enable-in-program), all hardware-confirmed against the
  bench `192.168.0.100`. B137 is the Niagara-side counterpart that consumes that map.
- **DEFERRED → DYNAMIC phase** — §137.9 is the live validation that would convert this design's `[INFER]`
  reconciliation into `[CERT-hw]`, mirroring how the logosoft corpus validated the LOGO side.
