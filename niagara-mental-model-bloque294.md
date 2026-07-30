# Block 294 — Modbus driver architecture: five network types, the 3-level class hierarchy, the module/palette map, and the client-vs-server asymmetry

> Focus **modbus** (bootstrapped 2026-07-30), gap **M1**. This block reconstructs the **driver** that
> surrounds the Modbus wire protocol: which network types exist, how they inherit from each other, which
> jar ships what, and how much of the master (client) implementation is actually mirrored on the
> slave (server) side. The **wire encoding** — MBAP, PDU per function code, RTU CRC-16, ASCII LRC,
> addressing and byte order — is NOT re-derived here; it is already closed by [Block 131]. READ-ONLY.
> Corpus language: ENGLISH.
>
> Sources (primary):
> - **Code** — decompiled with Vineflower from the live install, preserved under `sources/decompiled/`:
>   `modbusCore-rt.jar` (sha256 `a0b67420…`), `modbusTcp-rt.jar` (`688bb50b…`),
>   `modbusAsync-rt.jar` (`45156565…`), `modbusTcpSlave-rt.jar` (`8d78f0a5…`),
>   `modbusSlave-rt.jar` (`5c18467c…`, preserved in THIS iteration).
> - **Official Tridium documentation — first use in this corpus** — `docModbus-doc.jar` (`0aca2127…`),
>   87 help topics consolidated into `sources/manuals/docModbus-N4.14-guide.md`. Verified before the run:
>   `rg -il 'docModbus' *.md` returned **zero hits** across the 290 existing blocks. B131 was built
>   100 % from decompilation, so the guide is new evidence, not a restatement.
> - **Packaging** — `META-INF/module.xml` and `module.palette` read directly out of the shipped jars.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official Tridium guide (§topic) ·
> `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 131] (wire encoding),
> [Block 137] (LOGO! 8 integration plan), [Block 7] (driver framework), [Block 4] (driver framework
> architecture), [Block 94] (Honeywell Modbus device manager).

---

## 294.1 — Five network types, three levels of inheritance `[CERT]`

The official guide opens with the count: *"The driver provides four Modbus modules that support **five
Modbus network types**. Three networks serve as clients… In the other two networks, the controller and
station serve as servers (slaves)"* `[CERT-doc]` `sources/manuals/docModbus-N4.14-guide.md` §Architecture.

The code confirms the split and adds the shape the guide never draws — a **three-level hierarchy** whose
middle level is what actually decides master-vs-slave behaviour:

| Level | Class | Extends | File |
|---|---|---|---|
| 1 (root) | `BModbusNetwork` (abstract) | `BBasicNetwork` | `modbusCore-rt/…/BModbusNetwork.java:40` |
| 2 (role) | `BModbusClientNetwork` (abstract) | `BModbusNetwork` | `modbusCore-rt/…/client/BModbusClientNetwork.java:31` |
| 2 (role) | `BModbusServerNetwork` (abstract) | `BModbusNetwork` | `modbusCore-rt/…/server/BModbusServerNetwork.java:18` |
| 3 (concrete, client) | `BModbusTcpNetwork` | `BModbusClientNetwork` | `modbusTcp-rt/…/BModbusTcpNetwork.java:25` |
| 3 (concrete, client) | `BModbusAsyncNetwork` | `BModbusClientNetwork` | `modbusAsync-rt/…/BModbusAsyncNetwork.java:69` |
| 3 (concrete, client) | `BModbusTcpGateway` | **`BModbusTcpNetwork`** | `modbusTcp-rt/…/BModbusTcpGateway.java:47` |
| 3 (concrete, server) | `BModbusSlaveNetwork` | `BModbusServerNetwork` | `modbusSlave-rt/…/BModbusSlaveNetwork.java:45` |
| 3 (concrete, server) | `BModbusTcpSlaveNetwork` | `BModbusServerNetwork` | `modbusTcpSlave-rt/…/BModbusTcpSlaveNetwork.java:43` |

The root sits on `BBasicNetwork` (`com.tridium.basicdriver`), not directly on `javax.baja.driver.BNetwork`
`[CERT]` `BModbusNetwork.java:3,40` — so Modbus inherits the basic-driver ping/monitor/tuning machinery
documented in [Block 4] rather than reimplementing it.

**The gateway is the odd one out.** `BModbusTcpGateway extends BModbusTcpNetwork` `[CERT]`
`BModbusTcpGateway.java:47` — the "TCP/Serial gateway" the guide describes as *"a network-level object
that also represents a particular device"* `[CERT-doc]` §Architecture is, structurally, **a Modbus TCP
network that carries its own `ipAddress` and `port`** `[CERT]` `BModbusTcpGateway.java:27-35`
(`defaultValue = "ModbusMessageConst.DEFAULT_IP"`, `port` default `502`). The two serial-side children
(`ModbusTcpGatewayDevice`, `…DeviceFolder`) hang off it. So the count "five network types" is a count of
*five distinct network classes*, and the gateway is the third client one — not a device. `[INFER]`

## 294.2 — The module map, and what the official table gets wrong `[CERT]` §14

The guide's §Modules table maps four palettes to four jars `[CERT-doc]` §Modules:

| Palette name (per the guide) | Network component | `.jar` per the guide |
|---|---|---|
| `modbusAsync` | ModbusAsyncNetwork | **`modbuscore.jar`** |
| `modbusSlave` | ModbusSlaveNetwork | `modbusSlave.jar` |
| `modbusTcp` | ModbusTcpNetwork | `modbusTcp.jar` |
| `modbusTcpSlave` | ModbusTcpSlaveNetwork | `modbusTcpSlave.jar` |

Two defects, both verified against the shipped install:

1. **The `modbusAsync` row names the wrong jar.** `BModbusAsyncNetwork.class` is inside
   `modbusAsync-rt.jar`, not in `modbusCore-rt.jar` `[CERT]` (`unzip -l modbusAsync-rt.jar` →
   `com/tridium/modbusAsync/BModbusAsyncNetwork.class`; the same listing over `modbusCore-rt.jar` contains
   `com/tridium/modbusCore/BModbusNetwork.class` and no `modbusAsync` package). `modbusAsync-rt.jar` also
   carries its own `module.palette`, so the palette does not come from core either.
2. **The jar names are pre-N4.** The guide writes `modbuscore.jar` / `modbusTcp.jar`; N4.14 ships
   runtime-profile-suffixed jars — `modbusTcp-rt.jar`, `-wb.jar`, `-ux.jar`
   `[CERT]` (`module.xml`: `moduleName="modbusTcp" runtimeProfile="rt"`). The unsuffixed form is the
   NiagaraAX convention. `[INFER]` the table was carried over from the AX guide and never re-verified.

**What the guide gets right, and why "four" is not an error.** `modbusCore-rt.jar` has **no
`module.palette`**, while the other four do `[CERT]` (`unzip -l` over the five `-rt` jars: `module.palette`
present in `modbusTcp-rt`, `modbusAsync-rt`, `modbusSlave-rt`, `modbusTcpSlave-rt`; absent in
`modbusCore-rt`). The guide is counting *palettes an integrator can drag from*, and by that measure four is
correct — `modbusCore` is an internal library, never dragged directly. This corrects my own working
assumption at bootstrap time (that the guide had omitted a module); it had not.

Module identity, verbatim from the manifests `[CERT]` (`META-INF/module.xml`):

| Module | `preferredSymbol` | `description` | version |
|---|---|---|---|
| `modbusCore` | `mc` | Modbus Core Driver | 4.14.0.162 |
| `modbusTcp` | `mt` | ModbusTcp Driver | 4.14.0.162 |
| `modbusAsync` | `ma` | — | 4.14.0.162 |
| `modbusSlave` | `ms` | — | 4.14.0.162 |
| `modbusTcpSlave` | **`modTcpSlave`** | — | 4.14.0.162 |

**Symbol trap** `[CERT]`: `modbusTcpSlave`'s global `preferredSymbol` is `modTcpSlave`, but its own
`module.palette` declares the *local* alias `m="ms=modbusTcpSlave"` — reusing the letter `ms` that is the
global symbol of the **different** module `modbusSlave`. Inside that one palette document `ms:` means
`modbusTcpSlave`; anywhere else `ms:` means `modbusSlave`. A `ms:ModbusTcpSlaveDevice` string is therefore
not evidence that the type lives in `modbusSlave`.

## 294.3 — `modbusCore` ships inside the other four palettes `[CERT]`

Reading the four `module.palette` files shows why core needs no palette of its own: **each transport
palette re-exports the core types the integrator needs.** Types listed per palette, by module symbol:

| Palette | own types | `modbusCore` (`mc:`) types re-exported |
|---|---|---|
| `modbusTcp` | 6 (`Network`, `Device`, `DeviceFolder`, `Gateway`, `GatewayDevice`, `GatewayDeviceFolder`) | 14 — 6 client ProxyExt + `ModbusClientPointFolder` + `ModbusClientExceptionStatus` + `DevicePollConfigEntry` + 4 preset types + `ModbusClientStringRecord` |
| `modbusAsync` | 3 (`Network`, `Device`, `DeviceFolder`) | 14 — identical client set |
| `modbusSlave` | 3 (`Network`, `Device`, `DeviceFolder`) | 6 — 3 server ProxyExt + `ModbusServerPointFolder` + `ModbusRegisterRangeEntry` + `ModbusServerStringRecord` |
| `modbusTcpSlave` | 3 (`Network`, `Device`, `DeviceFolder`) | 6 — identical server set |

The two client palettes expose an **identical** core set, and so do the two server palettes `[CERT]`.
Transport is genuinely orthogonal to the data model: what you drag is the same regardless of whether the
wire is RS-485 or Ethernet — which is exactly the "all Modbus networks use the standard Framework network
architecture" claim of §Architecture `[CERT-doc]`, now measured rather than asserted.

## 294.4 — The client/server asymmetry, measured `[CERT]`

The guide says the slave side is symmetric enough to skip: *"Usage of these components is expected to be
infrequent. When used, basic Modbus principles remain the same."* `[CERT-doc]` §Architecture. The code
disagrees in a quantifiable way.

**Point types** (concrete `ProxyExt` classes, `modbusCore-rt`):

| Side | Concrete ProxyExt | Which |
|---|---|---|
| Client | **6** | `Boolean`, `Numeric`, `String`, `RegisterBit`, `NumericBits`, `EnumBits` |
| Server | **3** | `Boolean`, `Numeric`, `RegisterBit` |

`[CERT]` `modbusCore-rt/client/point/` contains `BModbusClient{Boolean,Numeric,String,RegisterBit,
NumericBits,EnumBits}ProxyExt.java` plus the base `BModbusClientProxyExt`, `…PointDeviceExt`,
`…PointFolder`, `…PollGroup` (10 files); `modbusCore-rt/server/point/` contains
`BModbusServer{Boolean,Numeric,RegisterBit}ProxyExt.java` plus `BModbusServerProxyExt`,
`…PointDeviceExt`, `…PointFolder` (6 files). **As a Modbus slave, Niagara cannot expose a string point or a
multi-bit enum point** — those two exist only on the master side. `[INFER]` from the absence, corroborated
by the palettes in §294.3 which list exactly 3 server ProxyExt.

**Network properties**:

| Class | own `@NiagaraProperty` count | Properties |
|---|---|---|
| `BModbusNetwork` (root) | 4 | `floatByteOrder`, `longByteOrder`, `double64BitByteOrder`, `long64BitByteOrder` |
| `BModbusClientNetwork` | 3 | `usePresetMultipleRegister` (false), `useForceMultipleCoil` (false), `maxFailsUntilDeviceDown` (**2**) |
| `BModbusServerNetwork` | **0** | — |

`[CERT]` `BModbusNetwork.java:22-39`, `BModbusClientNetwork.java:17-34`, `BModbusServerNetwork.java:17-19`.

The server role adds **no** configuration of its own at the network level; it adds *state*: a
`Set<Integer> deviceAddressSet` `[CERT]` `BModbusServerNetwork.java:20` that enforces address uniqueness,
faulting a duplicate device with the cause string `"Duplicate Device Address"` `[CERT]`
`BModbusServerNetwork.java:119`. A master does not need this (it addresses outward); a slave does (it *is*
the addressee). That is the real asymmetry: the client is configured, the server is policed. `[INFER]`

Concrete-network properties, for the same comparison `[CERT]`:

| Network | Properties (defaults) |
|---|---|
| `BModbusAsyncNetwork` (client, serial) | `interMessageDelay` 0 · `maxRxInterCharacterDelay` 50 ms · `minRxFrameEnd` 20 ms · `rxPriority` false · `serialPortConfig` `BSerialHelper` · `modbusDataMode` **rtu** · `snifferMode` false · `rtuSnifferModeBufferSize` 8 — `BModbusAsyncNetwork.java:30-66` |
| `BModbusTcpNetwork` (client, TCP) | `socketOptionTimeout` **1 minute** — `BModbusTcpNetwork.java:20-22` |
| `BModbusTcpGateway` (client, TCP→serial) | inherits the above + `ipAddress` · `port` **502** · `socketStatus` closed · `rxProcessMode` false — `BModbusTcpGateway.java:27-45` |
| `BModbusSlaveNetwork` (server, serial) | `interMessageDelay` 0 · `serialPortConfig` · `modbusDataMode` **rtu** · `snifferMode` false — `BModbusSlaveNetwork.java:28-43` |
| `BModbusTcpSlaveNetwork` (server, TCP) | `port` `BServerPort(502, TCP)` · `socketTimeoutInMillis` **30000** · `maximumConnections` **5** · `currentConnections` 0 — `BModbusTcpSlaveNetwork.java:24-40` |

Two operational numbers worth carrying forward: the TCP slave accepts **5 simultaneous master connections
by default** (`maximumConnections`), and the serial client's default framing is **RTU**, not ASCII, on both
roles (`modbusDataMode` default `BModbusDataModeEnum.rtu`).

## 294.5 — What the driver hides from the property sheet `[CERT]`

Three deliberate `setFlags` calls remove inherited driver-framework surface. `Flags.HIDDEN = 0x04`,
`Flags.READONLY = 0x01` `[CERT]` `docSource/…/javax/baja/sys/Flags.java:183-185` (original Tridium source
with javadoc, not decompiled).

| Where | Call | Effect |
|---|---|---|
| `BModbusNetwork()` ctor | `setFlags(upload, 4)` · `setFlags(download, 4)` `[CERT]` `BModbusNetwork.java:89-92` | the basic-driver `upload`/`download` actions are **hidden on every Modbus network**, client and server |
| `BModbusServerNetwork()` ctor | `setFlags(retryCount, 4)` · `setFlags(responseTimeout, 4)` `[CERT]` `BModbusServerNetwork.java:29-32` | a **slave** shows no retry/timeout tuning — it never initiates |
| `BModbusClientNetwork.started()` | `getMonitor().setNumRetriesUntilPingFail(0)` then `setFlags(…, flags \| 4 \| 1)` `[CERT]` `BModbusClientNetwork.java:66-70` | the ping monitor's retry count is **forced to 0 and then hidden + made read-only** |

The third is the load-bearing one: on a Modbus client network the ping-monitor retry is not a tunable the
integrator forgot to set — the driver **overwrites it at every start** and then hides the evidence. Device
down-detection is instead governed by the client-network property `maxFailsUntilDeviceDown`, default **2**
`[CERT]` `BModbusClientNetwork.java:34`. `[INFER]` an integrator chasing "why does my device drop after two
polls" will not find the knob on the ping monitor, because that is not where it lives.

## 294.6 — Byte order is a NETWORK property, and the defaults confirm B137 `[CERT]` §14

All four byte-order settings are declared on the **root** network class, so they apply identically to
client and server networks `[CERT]` `BModbusNetwork.java:22-44`:

| Property | Type | Default |
|---|---|---|
| `floatByteOrder` | `BDataByteOrderEnum` | `order3210` |
| `longByteOrder` | `BDataByteOrderEnum` | `order3210` |
| `double64BitByteOrder` | `BDataByteOrder64BitEnum` | `order76543210` |
| `long64BitByteOrder` | `BDataByteOrder64BitEnum` | `order76543210` |

This **confirms the correction B137 issued against B131 §131.9**: the effective network default is
`order3210` (big-endian, high word first), which is the `@NiagaraProperty` `defaultValue`, not the enum's
own default constant `order1032` that B131 had read. The declaration and the `newProperty(…)` call agree
`[CERT]` `BModbusNetwork.java:26,41`. No new §14 correction is needed — B137 already fixed it; this block
re-confirms it from the property declaration rather than from the integration plan. The guide states the
same intent in prose: *"you can configure the default order for float and long numeric data (overrideable
within each child device)"* `[CERT-doc]` §Architecture (TCP/IP networks).

## 294.7 — Instrumentation: four counters and a log named after the network `[CERT]`

`BModbusNetwork` keeps four private counters — `totalCrcErrors`, `totalLrcErrors`,
`totalTransactionIdErrors`, `totalPartialRxMsgs` `[CERT]` `BModbusNetwork.java:48-51` — incremented through
`incrementCrcErrors()` / `incrementLrcErrors()` / `incrementTransactionIdErrors()` /
`incrementPartialRxMsgs()` `[CERT]` `BModbusNetwork.java:206-220`. They are **not** Niagara properties:
they surface only through the spy page `[CERT]` `BModbusNetwork.java:156-204`, which renders a per-device
table whose columns differ by role — server: `Device / Address / Mode / Messages`; client:
`Device / Address / Mode / Request / NoResponse / CRC-LRC / TransactionId` (with the header
*"Device Transaction Info (Retries not counted)"*) `[CERT]` `BModbusNetwork.java:169-175`. `[INFER]` since
they are neither properties nor histories, these error counts cannot be trended or alarmed on — they are
live-diagnostic only, readable from the station's spy tree.

The Modbus log is named after the **network component's own name**, escaped through `SlotPath` if it is not
a legal slot name `[CERT]` `BModbusNetwork.java:108-115`. A `NameSubscriber` watches the parent and, on a
rename event (`event.getId() == 3` matching the network's own property slot), deletes the old log and
re-creates it under the new name, carrying the severity across `[CERT]` `BModbusNetwork.java:117-150,
238-248`. `[INFER]` renaming a Modbus network in Workbench therefore silently changes the log category an
operator has to enable for tracing — the severity follows, but any external reference to the old log name
(a filter, a runbook step) breaks.

Trace output is gated on `getModbusLog().isTraceOn()` at comm start/stop and in the helper lifecycle
`[CERT]` `BModbusServerNetwork.java:37-39,49-50,74-75,82-83`, which is the mechanism behind the guide's
§DebuggingMessages topic — deferred to gap M6.

## 294.8 — §14 review: no prior block is contradicted

Checked against every corpus block that mentions Modbus (`rg -il modbus niagara-mental-model-bloque*.md` →
42 blocks). The three with real Modbus substance:

- **[Block 131]** (wire level) — no overlap: it documents encoding, this block documents component
  structure. Its `order1032` reading was already corrected by B137 and is re-confirmed here (§294.6).
- **[Block 137]** (LOGO! 8 plan) — §137.2 sketched `BModbusTcpNetwork`/`BModbusTcpDevice` *as configured by
  a user*; this block supplies the hierarchy above them and the property inventory. Consistent.
- **[Block 94]/[Block 95]/[Block 250]** (Honeywell OEM) — sit on top of the driver, not inside it. Deferred
  to gap M9.

No correction is issued by this block.

## 294.9 — What the official guide does NOT resolve

Recorded per the doc-synthesis convention (PROMPT-LOOP step 4). The 87-topic guide is silent on:

- the **class hierarchy** — no topic names `BModbusClientNetwork`/`BModbusServerNetwork`; the client/server
  split is described only in prose about network *types*;
- the **hidden knobs** of §294.5 — `upload`/`download`, `retryCount`/`responseTimeout` on the slave, and the
  forced `numRetriesUntilPingFail = 0` are documented nowhere; the guide's configuration topics describe the
  visible property sheet only;
- the **missing server point types** (§294.4) — no topic states that a slave cannot expose a string or
  enum-bits point; §ServerslaveConfiguration lists what exists without noting what does not;
- the **error counters and their spy-only visibility** (§294.7);
- the `maximumConnections = 5` ceiling on the TCP slave, which appears in code but not in the architecture
  or configuration topics.

Conversely the guide supplies what code cannot: the *intent* of each network type, the RS-485 device-count
limits (31 full-load to 127 quarter-load), the 1–247 address range, and the typical 9600 baud figure
`[CERT-doc]` §Architecture — none of which are compiled-in constants.

## 294.10 — Self-verify

`verify-block.sh` tally (COMPUTED, not remembered — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 50 | **49** |
| `[CERT-doc]` | 10 | **9** |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 10 | **9** |
| **[INFER]/[CERT*] ratio** | | **9/58 = 0.16** |

Citation resolution: all 25 distinct `file:line` citations resolve as `extern` (decompiled trees under
`sources/decompiled/`, plus `docSource` for `Flags.java`) — not script-verifiable by design, so each was
token-checked by reading. Script exit 0.

**Block type: EVIDENCE.** A ratio of 0.16 is low — this gap's investigable evidence is far from exhausted,
consistent with 10 of 11 backlog items still open.

Load-bearing claims and their verification:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | 3-level hierarchy; 5 concrete network classes | `[CERT]` | `rg -m1 'public.*class B\w+Network'` over all 5 files; line numbers resolved |
| 2 | `BModbusTcpGateway extends BModbusTcpNetwork` | `[CERT]` | `BModbusTcpGateway.java:47` read directly |
| 3 | `BModbusAsyncNetwork.class` is in `modbusAsync-rt.jar`, not core | `[CERT]` | `unzip -l` on both jars — positive hit in one, absent in the other |
| 4 | `modbusCore-rt.jar` has no `module.palette`; the other four do | `[CERT]` | `unzip -l` over all five `-rt` jars |
| 5 | Guide's §Modules table names `modbuscore.jar` for the `modbusAsync` palette | `[CERT-doc]` | token present verbatim in `sources/manuals/docModbus-N4.14-guide.md` §Modules |
| 6 | 6 client ProxyExt vs 3 server ProxyExt | `[CERT]` | file enumeration of both `point/` dirs + cross-check against the 4 palettes |
| 7 | `maxFailsUntilDeviceDown` default 2 | `[CERT]` | `BModbusClientNetwork.java:28,34` (annotation and `newProperty` agree) |
| 8 | `Flags.HIDDEN = 4`, `READONLY = 1` | `[CERT]` | `docSource` original Tridium source, `Flags.java:183-185` — not decompiled |
| 9 | ping-monitor retry forced to 0 then hidden+readonly | `[CERT]` | `BModbusClientNetwork.java:66-70` |
| 10 | byte-order defaults `order3210` / `order76543210` | `[CERT]` | `BModbusNetwork.java:26-44`, annotation and `newProperty` agree |
| 11 | `maximumConnections` default 5 on the TCP slave | `[CERT]` | `BModbusTcpSlaveNetwork.java:33-35` |
| 12 | `"Duplicate Device Address"` fault string | `[CERT]` | `BModbusServerNetwork.java:119` |
| 13 | `docModbus` had zero citations in the corpus before this block | `[CERT]` | `rg -il 'docModbus' *.md` → no hits (run at bootstrap) |

Tokens grep-confirmed in their cited source: **13 / 13**. Sources preserved: `modbusSlave-rt` decompiled
tree added to `sources/decompiled/`; `docModbus-N4.14-guide.md` (87 topics) added to `sources/manuals/`;
both registered in `sources/SOURCES.md`. MCP-doc snapshots: none used (N/A). Model tier: **no delegation —
inline**, per the session's no-subagent constraint.

Self-correction recorded: at bootstrap I read the guide's "four Modbus modules" as an omission of
`modbusCore`. Checking `module.palette` before writing showed the guide counts *palettes*, and core has
none — the claim was withdrawn before it entered the block (§294.2).

## 294.x — Connections

- **[Block 131]** — the wire encoding underneath every component named here. Complementary, no overlap.
- **[Block 137]** — the applied LOGO! 8 integration plan; its §137.2 component tree is this block's level 3.
- **[Block 4]**, **[Block 7]** — the driver framework (`BBasicNetwork`, ping monitor, tuning) that
  `BModbusNetwork` inherits.
- **[Block 28]** — cross-protocol discovery; whether Modbus has a discovery job at all is gap **M8**.
- **[Block 94]**, **[Block 95]**, **[Block 250]** — Honeywell OEM modules layered on this driver; gap **M9**.

**Gaps opened by this block**: `M1-lic` — the §LimitsImposedByTheModbusLicenses topic illustrates limits
with `<feature name="mstp" … port.limit="5"/>`, an **MS/TP** feature tag, then says "other device or
platform limits in the license's `modbus` feature also apply"; whether `port.limit` is even a `modbus`
feature attribute is unverified → folded into **M7**. `M1-gw` — how a `BModbusTcpGateway` counts for
licensing and for the poll scheduler, given it is a network subclass that behaves like a device → folded
into **M2**/**M11**.
