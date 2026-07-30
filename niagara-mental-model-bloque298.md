# Block 298 — The server (slave) side: Niagara as a Modbus slave is an in-memory register map with four declared ranges, asymmetric persistence, and only three point types

> Focus **modbus**, gap **M4**. How a Niagara station **exposes** data to a foreign Modbus master: what
> `modbusSlave`/`modbusTcpSlave` actually build, how "which addresses exist" is declared, what survives a
> restart, and where the server side is genuinely NOT the mirror image of the client side that
> [Block 294] §294.4 first measured. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/modbusCore-rt/server/**` (18 classes, jar `a0b67420…`),
> `sources/decompiled/modbusSlave-rt/` (`5c18467c…`), `sources/decompiled/modbusTcpSlave-rt/`
> (`8d78f0a5…`), plus `docSource` original Tridium source for `javax.baja.sys.Flags`.
> Official documentation: `sources/manuals/docModbus-N4.14-guide.md` §Architecture,
> §Server (slave) configuration.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official Tridium guide (§topic) ·
> `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 294] (the client/server split and the
> 6-vs-3 ProxyExt count), [Block 296] (the client's four base addresses, which these four ranges mirror),
> [Block 297] (the client point model this one is compared against), [Block 131] (the wire).

---

## 298.1 — A slave device is four in-memory maps `[CERT]`

`BModbusServerDevice extends BModbusDevice` holds exactly four data structures `[CERT]`
`modbusCore-rt/…/server/BModbusServerDevice.java:71-74`:

```java
private IntHashMap inputRegisterByteArray  = null;
private IntHashMap holdingRegisterByteArray = null;
private IntHashMap coilStatusBitSet         = null;
private IntHashMap inputStatusBitSet        = null;
```

one per Modbus bank, keyed by address. `[INFER]` this is the structural answer to "how does Niagara serve
Modbus": it does **not** resolve an incoming request against the point tree on demand — it maintains a
materialised map that the points update, and requests are answered out of the map. That is the inverse of
the client side, where a poll reaches out to the wire ([Block 295]).

The guide frames the same thing loosely: *"the station exposes Modbus data and responds to Modbus
queries"* and *"Usage of these components is expected to be infrequent"* `[CERT-doc]` §Architecture.

## 298.2 — Four declared ranges: the mirror of the client's four base addresses `[CERT]`

The server device declares which addresses exist, one table per bank `[CERT]`
`BModbusServerDevice.java:31-49`:

| Property | Type |
|---|---|
| `validCoilsRange` | `BModbusRegisterRangeTable` |
| `validStatusRange` | `BModbusRegisterRangeTable` |
| `validHoldingRegistersRange` | `BModbusRegisterRangeTable` |
| `validInputRegistersRange` | `BModbusRegisterRangeTable` |
| `points` | `BModbusServerPointDeviceExt` |

Each defaults to a table holding one `BModbusRegisterRangeEntry` named from the lexicon key
`device.strings.defaultRange` `[CERT]` `:33,37,41,45`.

`[INFER]` this is the exact counterpart of the client's four base-address properties ([Block 296] §296.4):
the client says *"where the remote device's banks start"*, the server says *"which addresses of my banks
exist"*. Same four banks, opposite direction.

A range entry `[CERT]` `modbusCore-rt/…/server/datatypes/BModbusRegisterRangeEntry.java:20-42`:

| Property | Default | Facets |
|---|---|---|
| `enabled` | **true** | — |
| `criticalData` | **false** | — |
| `startingAddressOffset` | **1** | 1..`Integer.MAX_VALUE` |
| `size` | **64** | 1..`Integer.MAX_VALUE` |

So an unconfigured slave device exposes addresses **1–64** in every bank. Membership is a plain inclusive
range test, gated on `enabled` `[CERT]` `:137-145`:
`enabled && address >= start && address <= start + size - 1`.

`BModbusRegisterRangeTable.getValidAddressArray()` flattens all *enabled* entries into one address array,
sized by the sum of their `size` values `[CERT]` `…/datatypes/BModbusRegisterRangeTable.java:182-194`.
`[INFER]` since the maps are sized from that array, a large `size` (the facet allows `Integer.MAX_VALUE`)
allocates proportionally — the declared range is a memory decision, not just a validation rule.

## 298.3 — Persistence is asymmetric, and the asymmetry is correct `[CERT]`

The four maps are initialised by four sibling methods, and **they do not behave the same way** `[CERT]`
`BModbusServerDevice.java:251-345`:

| Bank | Init source | Persisted? |
|---|---|---|
| **Coils** (0x, master-writable) | `rangeEntry.getPersistedData()`, zero-filled if null, then written back with `setPersistedData()` `[CERT]` `:251-282` | **yes** |
| **Holding registers** (4x, master-writable) | same shape, 2 bytes per address (`hashSize = addresses.length * 2`) `[CERT]` `:300-307` | **yes** |
| **Status / discrete inputs** (1x, read-only to master) | every address set to `false` `[CERT]` `:284-298` | **no** |
| **Input registers** (3x, read-only to master) | same pattern `[CERT]` `:335-345` | **no** |

`[INFER]` the rule is: **only the banks a master can write are persisted.** That is the right call — the
read-only banks are fed by the station's own points, so their values are re-derived at startup, whereas a
setpoint a remote master wrote has no other source of truth and would otherwise be lost on restart.

## 298.4 — `criticalData` is a Baja flag, and it decides whether a master's write survives a crash `[CERT]`

`criticalData` looks like a label; it is a persistence-strength switch. On change it rewrites the flags of
the hidden `persistedData` property `[CERT]` `BModbusRegisterRangeEntry.java:117-128`:

```java
if (this.getCriticalData()) this.setFlags(prop, flags & -65537);   // clear 0x10000
else                        this.setFlags(prop, flags | 65536);    // set   0x10000
```

`0x00010000` is `Flags.NON_CRITICAL` ('N') `[CERT]`
`docSource/…/javax/baja/sys/Flags.java:199` — original Tridium source, not decompiled. The two flag
constants confirm it `[CERT]` `BModbusRegisterRangeEntry.java:45-46`:
`CRITICAL_FLAGS = 5` (= `READONLY|HIDDEN`) and `NON_CRITICAL_FLAGS = 65541` (= `NON_CRITICAL|READONLY|HIDDEN`).

`[INFER]` therefore: `persistedData` is always hidden and read-only in the property sheet, and
`criticalData` toggles only whether the station treats its saves as critical (immediate) or non-critical
(deferred, and losable in an ungraceful shutdown). **Default is `false` — non-critical.** An integrator
exposing setpoints to a remote master and expecting them to survive a power cut must set `criticalData`
per range; nothing in the property name says so.

## 298.5 — Three point types, and the missing three are confirmed from this side `[CERT]`

`BModbusServerProxyExt extends BModbusProxyExt implements ModbusErrorCodes, BIBasicPollable` `[CERT]`
`…/server/point/BModbusServerProxyExt.java:17` — note it implements `BIBasicPollable`, not the client's
`BIPollable` ([Block 297] §297.1).

The three concrete types and their properties `[CERT]`:

| ProxyExt | Properties | Lines |
|---|---|---|
| `BModbusServerNumericProxyExt` | `regType` (holding) · `dataType` (integerType) | 355 — `:25-31` |
| `BModbusServerBooleanProxyExt` | `statusType` (coil) | 214 — `:21-23` |
| `BModbusServerRegisterBitProxyExt` | `regType` (holding) · `bitNumber` (0) | 258 — `:24-30` |

These are property-for-property identical to their client counterparts ([Block 297] §297.3). What is
**absent** is confirmed by enumeration of the directory: no `ServerNumericBits`, no `ServerEnumBits`, no
`ServerStringProxyExt` `[CERT]` (`server/point/` holds exactly 6 files: the 3 above plus
`BModbusServerProxyExt`, `…PointDeviceExt`, `…PointFolder`).

`[INFER]` so a Niagara slave can expose a number, a bit-in-a-register and a coil — but it **cannot** expose
a bit-field as a scaled number or an enum, and it **cannot** expose a string. Note that
`BModbusServerStringRecord` *does* exist in `server/datatypes/` (95 lines) `[CERT]`, so string data is
served through the **file-record** channel (FC 20/21) rather than as a point — which is why there is no
string ProxyExt. That channel is gap **M5**.

## 298.6 — Debug residue shipped in a signed Tridium jar `[CERT]`

`BModbusRegisterRangeEntry.setPersistedData(IntHashMap, boolean)` contains, in the coil branch, an
unreachable-by-design case that prints to stdout `[CERT]` `BModbusRegisterRangeEntry.java:171-174`:

```java
Object value = map.get(addrKey);
if (value == null) {
   System.out.println("how'd we get here");
}
```

`[INFER]` a developer's placeholder that survived into the shipped `modbusCore-rt.jar` of N4.14.0.162.
Operationally minor — it writes to stdout rather than the station log, so it would not even appear in the
Modbus log an integrator is watching — but it marks the branch as genuinely unexpected: a coil address in
the declared range with no map entry. Consistent with the code-quality observations in [Block 295] §295.3
(four copy-pasted loops, duplicated accessor pairs).

## 298.7 — Network-level: the server adds policing, not configuration

Re-stating from [Block 294] §294.4 rather than re-deriving: `BModbusServerNetwork` declares **zero** own
properties, hides `retryCount`/`responseTimeout`, and keeps a `Set<Integer> deviceAddressSet` that faults a
duplicate with `"Duplicate Device Address"` `[CERT]` `…/server/BModbusServerNetwork.java:17-32,119-120`.
The two concrete server networks add their transport settings — the serial one `interMessageDelay`,
`serialPortConfig`, `modbusDataMode` (rtu), `snifferMode`; the TCP one `port` (502), `socketTimeoutInMillis`
(30000), `maximumConnections` (**5**), `currentConnections` `[CERT]` (property tables in [Block 294] §294.4).

`[INFER]` combining that with §298.1–§298.3: everything the slave role actually needs to be configured is
at the **device** level (the four ranges), not the network level. The network only owns the transport and
address uniqueness.

## 298.8 — What the official guide does NOT resolve

- **the in-memory map model** (§298.1) — the guide describes what a slave *is* topologically, never that
  requests are answered from a materialised map;
- **the persistence asymmetry** (§298.3) — no topic states that coils and holding registers survive a
  restart while status and input registers do not;
- **`criticalData`** (§298.4) — its meaning as a Baja `NON_CRITICAL` flag toggle, and that the default is
  the weaker setting, are undocumented;
- **the missing point types** (§298.5) — §Server (slave) configuration lists what exists without noting
  that bit-field, enum and string points have no server equivalent;
- **the memory implication** of a large declared `size` (§298.2).

What the guide does supply: that slave usage *"is expected to be infrequent"*, and that in many stations a
single child device represents the whole station `[CERT-doc]` §Architecture.

## 298.9 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 41 | 40 |
| `[CERT-doc]` | 3 | 2 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 9 | 8 |
| **[INFER]/[CERT*] ratio** | | **8/42 = 0.19** |

Script exit 0; citations resolve as `extern` and were token-checked by reading.

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | Four `IntHashMap` fields, one per bank | `[CERT]` | `BModbusServerDevice.java:71-74` |
| 2 | Four `valid*Range` properties + `points` | `[CERT]` | `:31-49` |
| 3 | Range entry defaults: enabled true, criticalData false, offset 1, size 64 | `[CERT]` | `BModbusRegisterRangeEntry.java:20-42` |
| 4 | `containsAddress` is enabled-gated inclusive range | `[CERT]` | `:137-145` |
| 5 | Coils + holding init from `getPersistedData()` | `[CERT]` | `BModbusServerDevice.java:251-282, 300-307` |
| 6 | Status + input registers init to false/zero, no persistence | `[CERT]` | `:284-298, 335-345` |
| 7 | `criticalData` clears/sets bit `0x10000` on `persistedData` | `[CERT]` | `BModbusRegisterRangeEntry.java:117-128` |
| 8 | `0x00010000` = `Flags.NON_CRITICAL` | `[CERT]` | `docSource` `Flags.java:199` (original source) |
| 9 | `CRITICAL_FLAGS = 5`, `NON_CRITICAL_FLAGS = 65541` | `[CERT]` | `:45-46` |
| 10 | Exactly 3 concrete server ProxyExt; 6 files in `server/point/` | `[CERT]` | directory enumeration + each property block; cross-checked against the 4 palettes ([Block 294] §294.3) |
| 11 | `BModbusServerStringRecord` exists in `server/datatypes/` | `[CERT]` | file present, 95 lines |
| 12 | `System.out.println("how'd we get here")` in shipped code | `[CERT]` | `BModbusRegisterRangeEntry.java:173` |
| 13 | `getValidAddressArray()` sums entry sizes | `[CERT]` | `BModbusRegisterRangeTable.java:182-194` |

Tokens grep-confirmed in their cited source: **13 / 13**. Claim 10 is an ABSENCE and was derived twice —
once by enumerating `server/point/` and once from the palette contents already measured in [Block 294]
§294.3, which list exactly three server ProxyExt. No new sources preserved. Model tier: **no delegation —
inline**.

## 298.x — Connections

- **[Block 294]** — §294.4 first measured 6-vs-3 ProxyExt; §298.5 confirms it from the server side and explains the string case (file records, not points).
- **[Block 296]** — the client's four base addresses (§296.4) and these four valid ranges are the same four banks seen from opposite ends.
- **[Block 297]** — the server's three ProxyExt are property-identical to their client twins; the datatype/byte-order model of §297.4–§297.5 applies unchanged.
- **[Block 295]** — no counterpart: the server has no poll engine, because it answers rather than asks.

**Gaps opened by this block**:
- **M4-a** — the actual **request-service path** (`server/messages/`, 7 classes: read/write, file read/write, and the FC 23 write-read) — how an incoming PDU is validated against the ranges and answered, including which exception code an out-of-range address returns → **new medium gap**.
- **M4-b** — how the station's own points **push** values into the four maps (`BModbusServerProxyExt` write-through) → folded into **M15** (the read/decode path gap opened by [Block 297]).
