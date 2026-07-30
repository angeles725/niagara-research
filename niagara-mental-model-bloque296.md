# Block 296 — Modbus configuration surface: the network→device override with a single switch, the four base addresses, `BFlexAddress` formats, and the synthesized ping

> Focus **modbus**, gap **M2**. What an integrator actually configures on a Modbus client device, and how
> those settings resolve against the network defaults documented in [Block 294]. Covers the two-level
> override model, the device property inventory, the address datatype (`BFlexAddress`) and its three
> formats, the four base-address offsets with their live validation, and how "device down" is decided —
> because Modbus has no ping and the driver synthesizes one. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): decompiled Vineflower trees preserved under `sources/decompiled/` —
> `modbusCore-rt.jar` (`a0b67420…`), `modbusTcp-rt.jar` (`688bb50b…`), `modbusAsync-rt.jar` (`45156565…`).
> Official documentation: `sources/manuals/docModbus-N4.14-guide.md` §Device configuration,
> §Configuring network properties, §Client (master) operations.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official Tridium guide (§topic) ·
> `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 294] (network-level properties),
> [Block 295] (the poll engine these settings feed), [Block 131] (byte-order encoding on the wire),
> [Block 137] (the LOGO! 8 plan, whose §137.5 wrestled with exactly this address model).

---

## 296.1 — Two levels, and a single switch that moves ALL of them `[CERT]`

Configuration is a struct that exists at both levels. `BModbusConfig` carries `overrideNetwork` (default
**false**) plus the same four byte-order settings the network declares `[CERT]`
`modbusCore-rt/…/datatypes/BModbusConfig.java:16-36`; `BModbusClientConfig extends BModbusConfig` and adds
`useForceMultipleCoil` and `usePresetMultipleRegister`, both default **false** `[CERT]`
`modbusCore-rt/…/client/datatypes/BModbusClientConfig.java:13-21`.

Resolution is a plain ternary, repeated once per setting `[CERT]`
`modbusCore-rt/…/BModbusDevice.java:64-82` and `…/client/BModbusClientDevice.java:313-323`:

```
getLongDataByteOrder()  = config.getOverrideNetwork() ? config.getLongByteOrder()  : network.getLongByteOrder()
getFloatDataByteOrder() = config.getOverrideNetwork() ? config.getFloatByteOrder() : network.getFloatByteOrder()
getDouble64BitDataByteOrder() / getLong64BitDataByteOrder()   — same shape
isPresetMultiple()      = config.getOverrideNetwork() ? config.getUsePresetMultipleRegister() : network...
isForceMultiple()       = config.getOverrideNetwork() ? config.getUseForceMultipleCoil()      : network...
```

**The gotcha is that there is exactly one switch for six settings.** `[INFER]` Turning `overrideNetwork` on
because one device stores floats in a different byte order also detaches that device from the network's
long, double-64, long-64, preset-multiple and force-multiple defaults — all at once, silently, using
whatever values the device struct happens to hold. There is no per-property override. The guide describes
the intent (*"If needed, any (or all) of these settings can be overridden at the device-level"*
`[CERT-doc]` §Device configuration) without stating that it is all-or-nothing.

The guide is also right about *why* this exists, and it is worth quoting because it is the clearest
statement of the problem the whole driver exists to solve: *"The configuration requirements for these
devices vary greatly because the Modbus protocol does not specify which specific function codes are
necessary in a device. The data type and format of register-held data are left up to the vendor. And,
quite commonly, different byte-order storage schemes are used for storing 32-bit data types"*
`[CERT-doc]` §Device configuration.

## 296.2 — The client-device property inventory `[CERT]`

From the root device `[CERT]` `BModbusDevice.java:20-29`:

| Property | Type | Default | Facets |
|---|---|---|---|
| `deviceAddress` | int | **1** | **1..247** |
| `modbusConfig` | `BModbusConfig` | new | — |

The root constructor also hides the inherited `upload`/`download` actions, exactly as the network does
`[CERT]` `BModbusDevice.java:60-61` (same pattern as [Block 294] §294.5).

From the client device `[CERT]` `BModbusClientDevice.java:46-91`:

| Property | Default | Role |
|---|---|---|
| `modbusConfig` | `BModbusClientConfig` | overrides §296.1 |
| `pingAddress` | `BFlexAddress` (empty) | which register the synthesized ping reads (§296.5) |
| `pingAddressDataType` | `integerType` | decides how many registers the ping asks for |
| `pingAddressRegType` | `holding` | decides FC 3 vs FC 4 |
| `pollFrequency` | `normal` | the device default; per-point values drive the group (see [Block 295] §295.6) |
| `inputRegisterBaseAddress` | `BFlexAddress` | 3x offset |
| `holdingRegisterBaseAddress` | `BFlexAddress` | 4x offset |
| `coilStatusBaseAddress` | `BFlexAddress` | 0x offset |
| `inputStatusBaseAddress` | `BFlexAddress` | 1x offset |
| `devicePollConfig` | `BDevicePollConfigTable` | the grouping table of [Block 295] |
| `points` | `BModbusClientPointDeviceExt` | frozen points extension |

`[CERT-doc]` §Client (master) operations describes this same set in prose — *"overrides of network level
Modbus Config settings, ping address setup for the parent network's Monitor ping, device base address
configurations for Modbus data items, and slots for configuring device-level polling"* — and states the
constraint that the three client device types are **not interchangeable** across parent network types
(you cannot drag a `ModbusAsyncDevice` under a `ModbusTcpGateway`).

## 296.3 — `BFlexAddress`: three formats, and the default is NOT the Modbus one `[CERT]`

Every address in the driver is a `BFlexAddress`: a format enum plus a string. `BAddressFormatEnum` has
`hex` = 0, `decimal` = 1, `modbus` = 2 — **and `DEFAULT = hex`** `[CERT]`
`modbusCore-rt/…/enums/BAddressFormatEnum.java:12,18-21`.

`getDataAddress()` converts to the raw PDU address `[CERT]`
`modbusCore-rt/…/datatypes/BFlexAddress.java:149-166`:

| Format | Conversion |
|---|---|
| `hex` | `Integer.valueOf(addr, 16)` — used as-is |
| `decimal` | `Integer.valueOf(addr)` — used as-is |
| `modbus` | subtract the bank offset: `>40000 → −40001` · `>30000 → −30001` · `>20000 → −20001` · `>10000 → −10001` · else `−1` |

`[INFER]` this is the single most confusing knob for a newcomer, and the default makes it worse: typing the
familiar Modbus address `40001` while the format sits at its default `hex` yields
`0x40001 = 262145`, not register 0. The guide never mentions that the format field defaults to hex.
`getDataAddressNoModbusAltering()` exists as the escape hatch that skips the bank subtraction entirely
`[CERT]` `BFlexAddress.java:168-170`.

Bank predicates, all requiring `modbus` format `[CERT]` `BFlexAddress.java:91-143`:

| Predicate | Range |
|---|---|
| `isModbusCoilAddress` (0x) | `0 ≤ x < 10000` |
| `isModbusStatusAddress` (1x) | `10000 < x < 20000` |
| `isModbusInputAddress` (3x) | `30000 < x < 40000` |
| `isModbusHoldingAddress` (4x) | `40000 < x < 50000` |
| `isModbusDigitalAddress` | `0 ≤ x < 30000` |
| `isModbusAnalogAddress` | `30000 ≤ x < 50000` |
| `isValid` (modbus format) | `0 ≤ x < 50000` |

Two structural observations `[INFER]`:

- **There is no predicate for the 2x bank** (20000–30000), yet `getDataAddress()` *does* handle it with
  `−20001`. The encoder understands a bank the validators do not recognise — so a 2x address converts
  correctly but fails every `isModbusXxx` check, including the base-address validation of §296.4.
- **The bank boundaries themselves are unreachable.** The tests use strict `>`, so `10000`, `20000`,
  `30000` and `40000` belong to no bank; in `getDataAddress()` both `10000` and `20000` map to `9999`.
  These are the gaps *between* the 1-based Modbus banks (00001–09999, 10001–19999, 30001–39999,
  40001–49999), so no legal address lands there — the inconsistency is real but unreachable in practice.

## 296.4 — The four base addresses: map shifting, validated on write `[CERT]`

The four base-address properties let a whole device map be shifted — a vendor whose registers start at
40501 can be described once at the device instead of in every point. `[INFER]`

What makes them notable is the **live validation with silent revert** `[CERT]`
`BModbusClientDevice.java:194-239`. On every `changed()` while running, each base address is checked
against its own bank predicate, and on failure the driver:

1. **resets the property to its default value**, and
2. logs an error naming the required bank —
   `"Illegal holding register base address on <device>. Must be a Holding register address 4x"`,
   and the equivalents for `3x`, `0x`, `1x` `[CERT]` `BModbusClientDevice.java:201,214,223,232`.

Note the guard: validation only fires `if (baseAddress.isModbusFormat() && !isModbusXxxAddress())`
`[CERT]` `:198,209,220,229`. `[INFER]` in `hex` or `decimal` format — including the **default** `hex` —
**no validation happens at all**: any value is accepted into any base-address slot.

Every one of these branches, plus a change to `devicePollConfig`, calls `updateProxyPointSubscriptions()`
`[CERT]` `BModbusClientDevice.java:205,207,218,227,236`. `[INFER]` that is the invalidation path for the
`lastPollGroupCode` cache of [Block 295] §295.1 — editing a base address or the poll table re-derives every
point's `devicePoll`/`pointPoll` decision.

## 296.5 — "Device down" rests on a ping Modbus does not have `[CERT]`

Modbus defines no ping/keepalive. The driver synthesizes one: `getPingRequest()` builds an ordinary read of
the configured `pingAddress` `[CERT]` `BModbusClientDevice.java:292-311`:

- `count = 1`, doubled to **2** when the configured `pingAddressDataType` is long or float
  (`:295-301`);
- FC **3** when `pingAddressRegType == holding`, otherwise FC **4** (`:304-308`) — so the ping can only
  target holding or input registers, **never a coil**.

`doPing()` sends it and treats any response that is not exception code 10 or 11 as success; otherwise it
increments the fail counter and only declares the device down once it exceeds `maxFailsUntilDeviceDown`
`[CERT]` `BModbusClientDevice.java:259-290` — the network property from [Block 294] §294.5, default 2.

`[INFER]` the operational consequence is sharp: **`pingAddress` must point at a register the device
actually implements.** A default/empty or wrongly-banked ping address makes the slave answer with an
exception, and the device is marked down even though it is answering perfectly well on every real point.
The guide calls this "ping address setup for the parent network's Monitor ping" `[CERT-doc]` §Client
(master) operations without warning that a bad address fabricates a down device. The error path is
distinguishable in the log — `"Error in <device> pinging. Check Device Status Monitor Address:"`
`[CERT]` `:268`.

## 296.6 — What each transport adds `[CERT]`

`BModbusTcpDevice` `[CERT]` `modbusTcp-rt/…/BModbusTcpDevice.java:23-54`:

| Property | Default |
|---|---|
| `deviceAddress` | 1 |
| `ipAddress` | `ModbusMessageConst.DEFAULT_IP` = **`"###.###.###.###"`** `[CERT]` `ModbusMessageConst.java:62` |
| `port` | **502** |
| `socketStatus` | `closed` |
| `disableTransactionIdCheck` | **false** |
| `maxTransactionId` | **65535** |
| `rxProcessMode` | false |

`[INFER]` the placeholder default IP is deliberate: `"###.###.###.###"` cannot resolve, so a device dragged
from the palette and left unconfigured fails visibly rather than pointing somewhere real.
`disableTransactionIdCheck` is the escape hatch for slaves that do not echo the MBAP transaction id
correctly ([Block 131] §131.3) — turning it off stops the mismatch from being counted as an error
(the counter is `totalTransactionIdErrors`, [Block 294] §294.7).

`BModbusAsyncDevice` adds exactly one property `[CERT]`
`modbusAsync-rt/…/BModbusAsyncDevice.java:19-21`: `modbusDataMode`, of type `BDeviceDataModeEnum` whose
range is `useNetworkDataMode` = 0 (**the default**), `ascii` = 1, `rtu` = 2 `[CERT]`
`modbusCore-rt/…/enums/BDeviceDataModeEnum.java:12,18-21`.

`[INFER]` this is a distinct enum from the network's `BModbusDataModeEnum` (`ascii` = 0, `rtu` = 1,
[Block 294] §294.4) precisely to add the inherit option — so ordinals do **not** line up between the two
enums, and per-device ASCII on an otherwise-RTU trunk is legal. Given [Block 295] §295.4, a single device
switched to ASCII halves its own read ceiling while sharing the trunk with RTU devices.

## 296.7 — §14 against the official guide: it documents four of six, and zero of the 64-bit surface

§Configuring network properties instructs: *"Configure **Float Byte Order**, **Long Byte Order**, **Use
Preset Multiple Register**, and **Use Force Multiple Coil**, and click Save."* `[CERT-doc]`. [Block 294]
§294.4 verified the network actually declares **six** such settings — the two 64-bit byte orders
(`double64BitByteOrder`, `long64BitByteOrder`, both defaulting to `order76543210`) are absent from the
instruction, and so is the `overrideNetwork` switch that governs the device copy.

Re-measured, and it is not a wording slip — it is a coverage hole. `rg -ci '64.?bit|double64|long64'` over
the consolidated 87-topic guide returns **zero hits** `[CERT]`
`sources/manuals/docModbus-N4.14-guide.md` (literal query recorded so a later pass does not retry it).
Meanwhile the driver supports **eight** datatypes, three of them 64-bit `[CERT]`
`modbusCore-rt/…/enums/BDataTypeEnum.java:12,23-31`: `integerType` (default), `longType`, `floatType`,
`signedInteger`, `unsignedLong`, `doubleType`, `signed64BitLongType`, `unsigned64BitLongType`.

`[INFER]` consistent with the pre-N4 residue found in [Block 294] §294.2 (wrong jar name, AX-era jar
naming): the 64-bit surface was added to the code after the guide was written, and the guide was never
revised. **Anyone configuring a 64-bit point is working entirely without documentation.** How those three
types are actually encoded belongs to gap M3.

No corpus block is corrected by this one — B294 and B295 are extended, not contradicted.

## 296.8 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 47 | 46 |
| `[CERT-doc]` | 8 | 7 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 12 | 11 |
| **[INFER]/[CERT*] ratio** | | **11/53 = 0.21** |

Script exit 0; all `file:line` citations resolve as `extern` (decompiled trees) and were token-checked by
reading.

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | One `overrideNetwork` flag governs all six settings | `[CERT]` | `BModbusDevice.java:64-82` + `BModbusClientDevice.java:313-323` — six ternaries, one condition |
| 2 | `deviceAddress` facets 1..247, default 1 | `[CERT]` | `BModbusDevice.java:20-23` |
| 3 | `BAddressFormatEnum.DEFAULT = hex` | `[CERT]` | `BAddressFormatEnum.java:21` |
| 4 | modbus-format bank subtraction incl. the `−20001` branch | `[CERT]` | `BFlexAddress.java:149-166` |
| 5 | No `isModbusXxx` predicate covers 20000–30000 | `[CERT]` | enumerated all six predicates, `BFlexAddress.java:91-143` |
| 6 | Base-address validation reverts to default + logs the required bank | `[CERT]` | `BModbusClientDevice.java:194-239` |
| 7 | Validation is skipped unless format is `modbus` | `[CERT]` | the `isModbusFormat() &&` guard at `:198,209,220,229` |
| 8 | Base-address / poll-config changes call `updateProxyPointSubscriptions()` | `[CERT]` | `:205,207,218,227,236` |
| 9 | Ping = real read; count doubles for long/float; FC3 holding else FC4 | `[CERT]` | `BModbusClientDevice.java:292-311` |
| 10 | Down only after exceeding `maxFailsUntilDeviceDown` | `[CERT]` | `:277-287` |
| 11 | `DEFAULT_IP = "###.###.###.###"` | `[CERT]` | `ModbusMessageConst.java:62` |
| 12 | TCP device: `maxTransactionId` 65535, `disableTransactionIdCheck` false | `[CERT]` | `BModbusTcpDevice.java:43-49` |
| 13 | `BDeviceDataModeEnum` has 3 values incl. `useNetworkDataMode` default | `[CERT]` | `BDeviceDataModeEnum.java:12,18-21` |
| 14 | Guide names 4 network properties; 6 exist | `[CERT-doc]` + `[CERT]` | quote verbatim in the guide vs B294 §294.4 |
| 15 | Zero 64-bit mentions in 87 topics | `[CERT]` | `rg -ci '64.?bit\|double64\|long64'` → 0 (real zero, query recorded) |
| 16 | 8 datatypes, 3 of them 64-bit | `[CERT]` | `BDataTypeEnum.java:12,23-31` |

Tokens grep-confirmed in their cited source: **16 / 16**. No new sources preserved (all jars already
registered). Model tier: **no delegation — inline**.

## 296.x — Connections

- **[Block 294]** — the network half of §296.1; its §294.4 property table is what `overrideNetwork` detaches from.
- **[Block 295]** — `updateProxyPointSubscriptions()` (§296.4) invalidates that block's group cache; `pollFrequency` here is the device default feeding its §295.6 contagion rule.
- **[Block 131]** — the byte orders resolved in §296.1 are consumed by its §131.9 encoders; the MBAP transaction id of its §131.3 is what `disableTransactionIdCheck` waives.
- **[Block 137]** — its §137.5 reconciled `BFlexAddress` raw-PDU vs Modbus-1based by hand; §296.3 now gives the full conversion table and the hex-default trap.

**Gaps opened by this block**:
- **M2-a** — the serial line settings themselves (`serialPortConfig` is a `BSerialHelper` from `serial-rt`, outside the Modbus jars): baud, parity, data/stop bits and how `maxRxInterCharacterDelay`/`minRxFrameEnd` ([Block 294] §294.4) interact with RTU's 3.5-character silence rule → **new medium gap** (needs `serial-rt`).
- **M2-b** — `rxProcessMode` appears on both `BModbusTcpGateway` and `BModbusTcpDevice`, default false, and was not resolved here → folded into **M12** (`ModbusTcpRxDriver`).
