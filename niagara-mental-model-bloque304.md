# Block 304 — The Workbench layer: six device managers, one 1299-line point manager, a dedicated address field editor — and NO discovery anywhere in the driver

> Focus **modbus**, gap **M8**. What the integrator actually operates: which views exist, what the point
> manager's columns expose, how the `BFlexAddress` field editor works, the parallel `-ux` (browser) layer,
> and the negative finding that shapes the entire Modbus workflow — **there is no discovery**.
> READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `organized/modbus*/…-wb/vineflower/` (11 classes) and `…-ux/vineflower/` (15),
> N4.14.0.162.
> Official documentation: `sources/manuals/docModbus-N4.14-guide.md` §Plugins (views), §Windows,
> §Configuring a device for polling.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official Tridium guide (§topic) ·
> `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 28] (cross-protocol discovery — this
> block is the Modbus-shaped hole in it), [Block 295] (`Learn Optimum Device Poll Config`, the one "learn"
> that does exist), [Block 296]/[Block 297] (the properties these columns edit), [Block 294] (the palettes
> these views serve).

---

## 304.1 — The negative finding first: Modbus has no discovery `[CERT]`

`rg -l 'Discover|LearnJob'` across **every** `modbus*` vineflower tree — `modbusCore`, `modbusTcp`,
`modbusAsync`, `modbusSlave`, `modbusTcpSlave`, `modbusTcpSlaveMigrator`, all runtime profiles — returns
**zero files** `[CERT]`. No `newDiscovery*`, no `DiscoveryLeaf`, no `doDiscover` either `[CERT]`.

**Control for the measurement** (RE-MEASURE rule, since this is a dramatic negative): the identical query
against `bacnet-wb` returns hits, including `com/tridium/bacnet/ui/point/PointLearn.java` `[CERT]` — so the
query and the corpus are both working; the absence is real, not a tooling artifact.

`[INFER]` and it is not an omission — it is the protocol. Modbus has no discovery service: no WhoIs/IAm
(BACnet, [Block 28] §28.2), no Query_Id/XIF (LON, §28.3), no multicast rollcall (Fox, §28.4). A Modbus slave
cannot be asked what it is or what registers it holds. **Everything must be typed in from the vendor's
register map.** That single fact explains the shape of the whole Workbench layer: it is an *editing*
surface, not a *learning* surface, and it is why [Block 295]'s `Learn Optimum Device Poll Config` is the
only thing in the driver called "learn" — it learns from the points **you already created**, not from the
device.

This is the Modbus-shaped hole in [Block 28]'s cross-protocol discovery survey, now measured and explained
rather than merely absent.

## 304.2 — Six device managers, thin by design `[CERT]`

The `-wb` classes, by module `[CERT]`:

| Module | Classes |
|---|---|
| `modbusCore-wb` (5) | `ui/BModbusDeviceManager` (163 L) · `ui/BModbusPointManager` (1299 L) · `ui/BFlexAddressFE` · `client/ui/BModbusClientPointManager` · `server/ui/BModbusServerPointManager` |
| `modbusTcp-wb` (2) | `ui/BModbusTcpDeviceManager` (56 L) · `ui/BModbusTcpGatewayDeviceManager` |
| `modbusAsync-wb` (1) | `ui/BModbusAsyncDeviceManager` |
| `modbusSlave-wb` (1) | `ui/BModbusSlaveDeviceManager` |
| `modbusTcpSlave-wb` (1) | `ui/BModbusTcpSlaveDeviceManager` |

`BModbusDeviceManager extends BDeviceManager` with two inner classes, `ModbusController extends
DeviceController` and `ModbusModel extends DeviceModel` `[CERT]`
`modbusCore-wb/…/ui/BModbusDeviceManager.java:33,50,120`. The transport managers are thinner still —
`BModbusTcpDeviceManager extends BModbusTcpGatewayDeviceManager` in 56 lines `[CERT]`
`modbusTcp-wb/…/BModbusTcpDeviceManager.java:21,34`.

`[INFER]` the inheritance direction is worth noting: the **plain TCP** manager extends the **gateway**
manager, not the reverse — the same relationship [Block 294] §294.1 found between the network classes, where
`BModbusTcpGateway extends BModbusTcpNetwork`. The UI inverts it. Both are 56-line specialisations, so
nothing hinges on it, but it means the gateway manager is the more general of the two in the UI layer while
the gateway *network* is the more specific in the runtime layer.

The guide lists exactly six views `[CERT-doc]` §Plugins (views): Modbus Async Device Manager, Modbus Client
Point Manager, Modbus Slave Device Manager, Modbus Server Point Manager, Modbus Tcp Gateway Device Manager,
Modbus Tcp Slave Device Manager — matching the class inventory.

## 304.3 — The point manager is where the driver's complexity surfaces `[CERT]`

`BModbusPointManager extends BPointManager` is **1299 lines**, by far the largest class in the Workbench
layer `[CERT]` `modbusCore-wb/…/ui/BModbusPointManager.java:74`. Its bulk is nine static `MgrColumn`
subclasses `[CERT]` (`:191, 208, 291, 368, 385, 462, 539, …`):

| Column class | Edits / shows |
|---|---|
| `ModbusAbsAddress` | the computed `absoluteAddress` ([Block 302] §302.4) |
| `ModbusRegType` | `regType` — holding/input ([Block 297] §297.2) |
| `ModbusStatusType` | `statusType` — coil/input |
| `ModbusDataType` | the 8 datatypes ([Block 297] §297.4) |
| `ModbusNumRegisters` | the derived register count |
| `ModbusBitNumber` | `bitNumber` 0..15 (register-bit points) |
| `ModbusBeginningBit` | `beginningBit` (bit-field points) |
| `ModbusNumberOfBits` | `numberOfBits` 1..16 |
| `ModbusDataSource` | **`devicePoll` vs `pointPoll`** ([Block 295] §295.1) |

`[INFER]` two of these are read-only diagnostics rather than settings — `ModbusAbsAddress` and
`ModbusDataSource` — and the second is the one that matters operationally: **the grouping decision of
[Block 295] is visible as a column in the point manager**, so an integrator can sort a device's points and
see at a glance which ones are still being polled individually. That is the practical instrument for the
whole optimisation story, and the guide never points at it.

Two profile-specific point managers sit beside it — `client/ui/BModbusClientPointManager` and
`server/ui/BModbusServerPointManager` `[CERT]` — matching the client/server split of [Block 294] §294.4.

## 304.4 — A dedicated field editor for the address, and the `-ux` twin `[CERT]`

`BFlexAddressFE extends BWbFieldEditor` `[CERT]` `modbusCore-wb/…/ui/BFlexAddressFE.java:28`.
`[INFER]` a purpose-built editor exists because `BFlexAddress` is a compound value (format enum + string,
[Block 296] §296.3) that a generic property editor would render as two unrelated fields; the FE is what
presents them as one address. Its browser counterpart is `ux/fe/BFlexAddressEditor` `[CERT]`.

The `-ux` layer mirrors the `-wb` layer almost class for class `[CERT]`:

| `modbusCore-ux` (6) | Role |
|---|---|
| `ux/BModbusCoreJsBuild` | the JS bundle descriptor |
| `ux/baja/BFlexAddressTypeExt` · `ux/baja/BModbusConfigExt` | type extensions exposing `BFlexAddress` / `BModbusConfig` to the browser |
| `ux/fe/BFlexAddressEditor` | the address field editor, browser side |
| `ux/mgr/BModbusClientPointUxManager` · `ux/mgr/BModbusServerPointUxManager` | the two point managers, browser side |

plus `modbusTcp-ux` (3: `BModbusTcpJsBuild` + the two device ux managers) and the `-ux` jars of
`modbusAsync`/`modbusSlave`/`modbusTcpSlave` `[CERT]`.

`[INFER]` so the Modbus driver is fully browser-capable: every manager has a `ux` twin. Note there is **no
`ux` twin of `BModbusDeviceManager` in core** — the ux device managers live in the transport modules
(`modbusTcp-ux/ux/mgr/…`), which mirrors the palette distribution of [Block 294] §294.3.

## 304.5 — What the guide's window topics cover `[CERT-doc]`

§Windows lists six modal windows: New device type, New device properties, New point type, New point
properties, Add Preset Register Value, Add Preset Coil Value `[CERT-doc]`. It also carries a caveat worth
recording: *"Windows do not support On View (F1) and Guide on Target help. To learn about the information
each contains, search the help system for key words."* `[CERT-doc]` §Windows — whereas views *do* support
F1 `[CERT-doc]` §Plugins.

`[INFER]` combined with §304.1, this is the honest description of the Modbus workflow: you drag a network
from a palette, drag devices under it, and then create every point by hand through the New Point Type /
New Point Properties windows, typing addresses from the vendor documentation — and only afterwards run
`Learn Optimum Device Poll Config` to collapse them into group reads. The guide's own ordering instruction
([Block 295] §295.1, "configure polling after the points exist") is a consequence of there being nothing to
discover.

## 304.6 — What the guide does NOT resolve

- that there is **no discovery at all** (§304.1) — the guide never says so; a reader coming from BACnet or
  LON would look for a Discover button that does not exist;
- that `Data Source` is a **column** in the point manager (§304.3) — the one place the grouping outcome is
  visible per point;
- the `-ux` browser layer (§304.4) is not mentioned in §Plugins, which lists only the Workbench views;
- that `BModbusTcpDeviceManager` extends the **gateway** manager (§304.2) — cosmetic, but it means the two
  views share behaviour in the opposite direction from the runtime classes.

## 304.7 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 28 | 27 |
| `[CERT-doc]` | 8 | 7 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 7 | 6 |
| **[INFER]/[CERT*] ratio** | | **6/34 = 0.18** |

Script exit 0.

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | Zero `Discover`/`LearnJob` hits across all `modbus*` trees | `[CERT]` | `rg -l` over every modbus module, all profiles → no files |
| 2 | Zero `newDiscovery`/`DiscoveryLeaf`/`doDiscover` in the `-wb` trees | `[CERT]` | second, differently-keyed query → no hits |
| 3 | The same query DOES hit `bacnet-wb` | `[CERT]` | control run; `PointLearn.java` among the hits |
| 4 | The 11 `-wb` classes by module | `[CERT]` | directory enumeration |
| 5 | `BModbusDeviceManager extends BDeviceManager` + 2 inner classes | `[CERT]` | `:33,50,120` |
| 6 | `BModbusTcpDeviceManager extends BModbusTcpGatewayDeviceManager` | `[CERT]` | `:21,34` |
| 7 | `BModbusPointManager` is 1299 lines, extends `BPointManager` | `[CERT]` | `wc -l` + `:74` |
| 8 | The nine `MgrColumn` subclasses and their names | `[CERT]` | `rg -o 'public static class (Modbus\w+) extends MgrColumn'` → all nine |
| 9 | `BFlexAddressFE extends BWbFieldEditor` | `[CERT]` | `:28` |
| 10 | The 15 `-ux` classes and their roles | `[CERT]` | directory enumeration across all modbus `-ux` trees |
| 11 | Guide lists exactly six views | `[CERT-doc]` | §Plugins (views), verbatim |
| 12 | Guide lists six windows + the no-F1 caveat | `[CERT-doc]` | §Windows, verbatim |

Tokens grep-confirmed in their cited source: **12 / 12**. Claim 1 is the load-bearing ABSENCE and was
derived **three** ways per the RE-MEASURE rule: two differently-keyed queries over the full modbus scope,
plus a positive control against `bacnet-wb` proving the query works. No new sources preserved (the `-wb`/
`-ux` trees are read from `organized/`, already registered as the decompilation corpus). Model tier:
**no delegation — inline**.

## 304.x — Connections

- **[Block 28]** — cross-protocol discovery; §304.1 is the measured explanation of why Modbus is absent from it.
- **[Block 295]** — `Learn Optimum Device Poll Config` is the only "learn"; `Data Source` is its outcome, surfaced as a column here.
- **[Block 296]/[Block 297]** — the properties the nine columns edit.
- **[Block 294]** — §294.3's palette distribution is mirrored by where the `ux` device managers live.

**Gaps opened by this block**: none. The `-ux` layer is inventoried but not read in depth; that is a
deliberate scope call, not an open question — the browser managers mirror the Workbench ones, and no
behaviour unique to them was observed at the class-inventory level.
