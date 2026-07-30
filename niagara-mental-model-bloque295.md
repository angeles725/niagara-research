# Block 295 — The Modbus acquisition engine: devicePoll vs pointPoll, how groups are formed, register-run coalescing, request fragmentation (125/2000 and the ASCII penalty), and the transport threading model

> Focus **modbus**, gap **M11**. This block answers the operational question *"how does the driver get
> many points fast"*: how N proxy points collapse into the minimum number of Modbus transactions, who
> decides the polling rate, where a request gets split, and how much of it runs in parallel. It is the
> engine between the component tree of [Block 294] and the wire bytes of [Block 131]. READ-ONLY.
> Corpus language: ENGLISH.
>
> Sources (primary, all decompiled with Vineflower and preserved under `sources/decompiled/`):
> `modbusCore-rt.jar` (`a0b67420…`), `modbusTcp-rt.jar` (`688bb50b…`), `modbusAsync-rt.jar` (`45156565…`),
> plus the two framework classes the engine sits on, preserved in THIS iteration:
> `driver-rt.jar` (`dd44eab5…`) → `BPollScheduler`, and `basicDriver-rt.jar` (`ff68126f…`) →
> `BBasicPollGroup`/`BBasicPollScheduler`.
> Official documentation: `sources/manuals/docModbus-N4.14-guide.md` §Client (master) operations,
> §Configuring a device for polling.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official Tridium guide (§topic) ·
> `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 294] (driver architecture),
> [Block 131] (wire encoding), [Block 4]/[Block 7] (driver framework, poll scheduler),
> [Block 137] (LOGO! 8 integration — its point list is exactly what this engine would group).

---

## 295.1 — Every point takes one of two paths, and the point itself decides `[CERT]`

The whole engine turns on one method: `BModbusClientProxyExt.getPollGroupCode()` `[CERT]`
`modbusCore-rt/…/client/point/BModbusClientProxyExt.java:68-105`. For each point, at subscription time:

1. resolve its register type (`determineRegisterType()`) and its register span (`determineNumRegisters()`);
2. fetch the device's **active** poll-config entries of that type;
3. ask `BDevicePollConfigTable.findPollConfigEntry(entries, pointAddress, numRegisters)` whether some entry
   **fully contains** the point's address span;
4. **found** → `setDataSource(BDataSourceEnum.devicePoll)` and the group code becomes **the entry object**;
   **not found** → `setDataSource(BDataSourceEnum.pointPoll)` and the group code becomes **the point itself**.

`BDataSourceEnum` has exactly these two ordinals — `devicePoll` = 0, `pointPoll` = 1, **default
`pointPoll`** `[CERT]` `modbusCore-rt/…/enums/BDataSourceEnum.java:12,18-20`.

Two consequences worth stating plainly:

- **`dataSource` is a diagnostic, not a setting.** It is written *by* the driver to report which path the
  point took. Reading it on a live station tells an integrator, per point, whether it is being grouped or
  polled alone. `[INFER]`
- **The default is the slow path.** A freshly added point is `pointPoll` — one Modbus transaction per point
  per cycle — until a poll-config entry exists that covers it. This matches the guide's workflow, which
  tells you to add all proxy points *first* and configure polling afterwards: *"Device polling should be
  configured until after proxy points are created, and typically already receiving values from
  (individual) point polling."* `[CERT-doc]` §Configuring a device for polling.

The decision is **cached** in `lastPollGroupCode` under a `pollSync` monitor `[CERT]`
`BModbusClientProxyExt.java:41,68-71`, and cleared on unsubscribe / config change `[CERT]`
`BModbusClientProxyExt.java:141-150`. So the containment search runs once per subscription, not per poll.

`findPollConfigEntry` requires **full containment**, not overlap `[CERT]`
`modbusCore-rt/…/client/datatypes/BDevicePollConfigTable.java:237-266`:

```
lastRegisterAddress = registerAddress + numAddresses - 1
endAddress          = entry.startAddress + entry.consecutivePointsToPoll - 1
isItPolled = registerAddress      >= startAddress && registerAddress      <= endAddress
          && lastRegisterAddress  >= startAddress && lastRegisterAddress  <= endAddress
```

`[INFER]` a 32-bit point (2 registers) straddling the end of an entry is therefore **not** grouped — it
silently falls back to `pointPoll`. Nothing warns; only `dataSource` shows it.

## 295.2 — How the group objects themselves are formed `[CERT]`

Grouping is not Modbus-specific machinery — it is `basicDriver`'s, and Modbus plugs into it by choosing the
*code*. `BBasicPollGroup.getPollGroup(proxy)` keeps a static two-level `Hashtable`
`typesToCodes: typeId → (code → group)` and returns the existing group for a code, creating it only on
first use `[CERT]` `basicDriver-rt-poll/…/BBasicPollGroup.java:116-133`.

That is the mechanism: **every point whose `getPollGroupCode()` returns the *same* `BDevicePollConfigEntry`
object lands in the *same* group instance**, and one group = one `poll()` = one Modbus transaction. Points
returning `this` each get a private group. `[INFER]` from the identity-keyed hashtable plus §295.1.

`BModbusClientPollGroup.poll()` then branches on what the code is `[CERT]`
`modbusCore-rt/…/client/point/BModbusClientPollGroup.java:23-76`:

| Code type | Action |
|---|---|
| `BDevicePollConfigEntry`, `enabled` | one bulk read → `entry.setByteArray(...)` → then loop the subscribed proxies calling `devicePoll(entry)`, each slicing its own bytes out of the shared buffer |
| `BDevicePollConfigEntry`, `enabled == false` | **returns immediately — the points are not polled at all** |
| `BModbusClientProxyExt` | `((BModbusClientProxyExt)obj).poll()` — the single-point path |

Function codes are selected per register type in the same method `[CERT]`
`BModbusClientPollGroup.java:52-60`: input register → **FC 4**, holding register → **FC 3**, discrete coil →
**FC 1**, discrete input → **FC 2**. This closes the `P1-fc` note that [Block 131] left open for the
*read* side: the register-type→FC mapping is not in the message classes, it is here in the poll group.

`[INFER]` the disabled-entry branch is a real operational trap: unchecking `enabled` on a poll-config entry
does not fall back to individual polling — the points bound to that entry simply stop updating, because
their cached `lastPollGroupCode` still points at the disabled entry.

## 295.3 — `Learn Optimum Device Poll Config`: strict consecutive runs, no gap tolerance `[CERT]`

The guide presents it as a right-click convenience: *"To organize points, add child DevicePollConfigEntry
objects manually in this container or (optionally) use the container's right-click action: Learn Optimum
Device Poll Config."* `[CERT-doc]` §Configuring a device for polling, whose stated purpose is *"configuring
the use a single message to poll consecutively addressed values. This reduces network messaging traffic."*

The action is declared on the table `[CERT]` `BDevicePollConfigTable.java:23-33` with parameter
`BReplaceExistingEnum`, default `replaceExistingEntries`, and delegates to
`BModbusClientDevice.getOptimumDevicePollConfigEntryList()` `[CERT]`
`BDevicePollConfigTable.java:324-342`.

The algorithm `[CERT]` `modbusCore-rt/…/client/BModbusClientDevice.java:683-818`:

1. walk every point under the device, bucket its ProxyExt into one of **four** vectors by register type;
2. per bucket: `sortByAbsoluteAddress(...)`, then scan for runs where each address differs from the
   previous by exactly 1 — `if (difference != 1) break` (`BModbusClientDevice.java:721-728`);
3. emit a `BDevicePollConfigEntry(true, <type>, <addr>, consecutiveCount, 1)` **only if
   `consecutiveCount > 1`** (`:731-736`);
4. rebuild the displayed address by adding the classic Modbus offset — **+40001** holding, **+30001** input,
   **+1** coil (`:734`, `:771`, `:808`).

Three properties of this algorithm that decide real-world performance `[INFER]`:

- **No gap tolerance.** One unused register between two points splits the run. Reading a few extra useless
  registers to save an entire round-trip is a standard Modbus optimization; this implementation never does
  it. A register map with holes stays fragmented no matter how many times you run Learn.
- **Isolated points stay isolated.** `consecutiveCount > 1` means a lone point never gets an entry — it
  remains `pointPoll` forever.
- **Learn is a snapshot, not a policy.** It reads the points that exist *now*; adding points later does not
  re-run it. Combined with `replaceExistingEntries` as the default parameter, a re-run **wipes hand-tuned
  entries** unless the caller passes the other enum value.

Code-quality note `[CERT]`: the four buckets are literal copy-paste of the same loop
(`BModbusClientDevice.java:709-744` / `746-781` / `783-818` / `820-…`), and `BDevicePollConfigTable` carries
four `getActiveXxxPollEntries()` / `getPossibleXxxPollEntries()` pairs whose bodies are **identical** apart
from the `synchronized` modifier `[CERT]` `BDevicePollConfigTable.java:62-204`.

## 295.4 — Fragmentation is real, and ASCII costs exactly half `[CERT]` — hypothesis REFUTED

The M11 scouting note in `RESEARCH-STATE-modbus.md` raised a hypothesis: since
`consecutivePointsToPoll` accepts up to **9999** by facets `[CERT]`
`modbusCore-rt/…/client/datatypes/BDevicePollConfigEntry.java:52` while Modbus caps a FC 3 read at 125
registers, and since `MAX_READ_DATA_SIZE = 255` / `MAX_WRITE_DATA_SIZE = 16` are declared but have **no
consumer anywhere in the five driver jars** `[CERT]`
`modbusCore-rt/…/messages/ModbusMessageConst.java:26-27` (`rg` across `modbusCore/Tcp/Async/TcpSlave/
Slave-rt` returns only the declaration), a long run might emit an illegal request.

**The hypothesis is false.** The clamp exists — it is just not those constants. It lives in the read
methods `[CERT]` `BModbusClientDevice.java:582-627` and `:629-681`:

| Read | cap | ASCII adjustment | loop |
|---|---|---|---|
| `readRegisters` (FC 3 / FC 4) | `maxReadSize = 125 - 125 % minReadSize` | `if (modbusMode == 0) maxReadSize /= 2` | `do { … numRegisters -= maxReadSize; startAddress += maxReadSize; } while (numRegisters > 0)` |
| `readStatusRegisters` (FC 1 / FC 2) | `maxReadSize = 2000` | same halving | same |

`modbusMode == 0` is **ASCII**, confirmed by the enum: `ascii = 0`, `rtu = 1`, default `rtu` `[CERT]`
`modbusCore-rt/…/enums/BModbusDataModeEnum.java:12-20`.

So a 9999-register entry does not produce an illegal PDU — it produces **80 sequential transactions**
(`ceil(9999/125)`), transparently, inside one `poll()` call. The correction matters in the other direction:
`consecutivePointsToPoll` is not a "message size", it is a **span** that the driver will silently fragment.

**The ASCII penalty is the finding.** On an ASCII serial network the driver halves its own ceiling:
125 → **62** registers, 2000 → **1000** coils per request. `[INFER]` this is on top of ASCII already
spending two wire characters per data byte ([Block 131] §131.6), so switching a network from RTU to ASCII
roughly **quadruples** the time to acquire the same span — half the payload per message *and* double the
bytes per payload. Neither number appears in the guide.

Also verified: an entry with `consecutivePointsToPoll == 0` short-circuits to comm status `-2` and returns
an empty buffer without sending anything `[CERT]` `BModbusClientDevice.java:593-595, 647-649`, and only
FC 3/4 (registers) and FC 1/2 (status) are accepted — anything else throws `ModbusException(100)` `[CERT]`
`:596-597, 650-651`.

## 295.5 — `readGroupSize`: the 32-bit alignment guard `[CERT]`

`BDevicePollConfigEntry.readGroupSize` is an int with facets **1..2**, default **1** `[CERT]`
`BDevicePollConfigEntry.java:53`, passed into `readRegisters` as `minReadSize` `[CERT]`
`BModbusClientPollGroup.java:49,53-55`. It does two things `[CERT]` `BModbusClientDevice.java:584-585,600-604`:

- shrinks the ceiling to a multiple of itself — `maxReadSize = 125 - 125 % 2` = **124** when set to 2, so a
  fragment boundary can never land in the middle of a 2-register value;
- rounds the final short read up — `count = numRegisters + numRegisters % minReadSize`.

`[INFER]` this is the setting for a device whose map is all 32-bit floats/longs: without it, a span of 125
registers would split after an odd count and cut a float in half across two responses.

## 295.6 — The polling rate: the group inherits its FASTEST member `[CERT]`

Modbus does not schedule; it inherits `driver`'s scheduler. `BPollScheduler` declares the three rates
`[CERT]` `driver-rt-poll/javax/baja/driver/util/BPollScheduler.java:23-35`:

| Rate | Default |
|---|---|
| `fastRate` | **1 000 ms** |
| `normalRate` | **5 000 ms** |
| `slowRate` | **30 000 ms** |

The group's own frequency is computed in `BBasicPollGroup.getPollFrequency()` `[CERT]`
`basicDriver-rt-poll/…/BBasicPollGroup.java:101-114`: it starts at `slow` and walks every subscribed proxy,
keeping the **fastest** one found (`if (fr.compareTo(fastestYet) < 0) fastestYet = fr`).

This is the least obvious consequence of grouping, and it cuts both ways `[INFER]`:

- **contagion** — one point set to `fast` inside a 100-register group drags the whole group to 1 s. The 99
  other points, individually configured `slow`, are now read every second. The bus load is real even though
  no one configured it;
- **it is also the mitigation** — because the group is a single transaction, one fast point costs one
  message per second, not 100.

`[INFER]` the practical rule: rate is a property of the *group*, so tune rates **after** deciding grouping,
and keep a fast point out of a large slow group unless you want the whole span at that rate.

## 295.7 — The transport: one socket and one thread per DEVICE on TCP, one per NETWORK otherwise `[CERT]`

`sendModbusMessage(Message)` is abstract on the client device `[CERT]`
`BModbusClientDevice.java:247` — each transport implements it. The TCP implementation wraps the message in
a `ModbusTcpSendRequest`, lazily opens the socket if `socketStatus` is closed/errored, then
`modbusNet().dispatch(req)` and **blocks** on `req.getResponse(0)` `[CERT]`
`modbusTcp-rt/…/BModbusTcpDevice.java:241-255`. Success/failure feeds the same `maxFailsUntilDeviceDown`
counter documented in [Block 294] §294.5 `[CERT]` `BModbusTcpDevice.java:257-266`.

Where the `Comm` object is constructed decides the concurrency, and the three transports differ `[CERT]`:

| Transport | `Comm` created at | Scope | Concurrency |
|---|---|---|---|
| Modbus TCP | `BModbusTcpDevice.java:145` (`new ModbusTcpComm(this.modbusNet())`) | **per DEVICE** | N devices → N sockets → N Rx threads, genuinely parallel |
| Modbus TCP Gateway | `BModbusTcpGateway.java:103` (`return new ModbusTcpComm(this)`) | **per NETWORK** | one socket for all children — serialized |
| Modbus Async (serial) | `BModbusAsyncNetwork.java:242` (`return new ModbusAsyncSerialComm(this)`) | **per NETWORK** | one physical port — serialized |

`ModbusTcpComm.started()` spawns a dedicated receive thread named `"ModTcp:" + devName + <serial>` at
priority **5** (`Thread.NORM_PRIORITY`) and starts a socket manager `[CERT]`
`modbusTcp-rt/…/comm/ModbusTcpComm.java:12-23`; `stopped()` tears down the socket manager, interrupts the
thread, closes the socket and nulls the streams `[CERT]` `:25-33`. The transmit side is thin — it only
traces and hands the bytes to the Rx driver's output stream `[CERT]`
`modbusTcp-rt/…/comm/ModbusTcpTxDriver.java:11-36`, so the socket is owned by the receiver.

`[INFER]` this is the single most important performance fact in the whole driver, and it is nowhere in the
guide: **Modbus TCP scales with device count, the gateway and serial do not.** Twenty TCP devices poll
concurrently on twenty sockets; twenty devices behind a TCP/serial gateway queue on one. A slow device on a
gateway or a serial trunk delays every other device on it; a slow device on Modbus TCP delays only itself.
Combined with §295.4, the worst case is an ASCII serial trunk: one shared port, halved message ceiling,
double the bytes.

## 295.8 — What the official guide does NOT resolve

- **the two paths** — `devicePoll` vs `pointPoll` and the `dataSource` indicator are never named;
- **the containment rule** — that a point must be *fully* inside an entry, so a 32-bit point straddling the
  end is silently left ungrouped;
- **the disabled-entry trap** — §295.2: disabling an entry stops its points rather than reverting them to
  individual polling;
- **fragmentation and the ASCII halving** — §295.4: no topic states the 125/2000 ceilings, that the driver
  splits automatically, or that ASCII halves them;
- **rate contagion** — §295.6: that a group polls at its fastest member's rate;
- **the threading/socket model** — §295.7: the guide's §Architecture describes topology, never that TCP is
  per-device-socket while the gateway is per-network.

What the guide does supply and code cannot: the *intent* of the feature and the correct order of operations
(add points → verify individual polling → then configure device polling) `[CERT-doc]` §Configuring a device
for polling, plus the warning that the three client device types are not interchangeable across parent
network types `[CERT-doc]` §Client (master) operations.

## 295.9 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 57 | 56 |
| `[CERT-doc]` | 6 | 5 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 14 | 13 |
| **[INFER]/[CERT*] ratio** | | **13/61 = 0.21** |

Script exit 0; all `file:line` citations resolve as `extern` (decompiled trees), so each was token-checked
by reading.

**Block type: EVIDENCE.** Ratio 0.21 — higher than B294's 0.16 because this block reasons about
*consequences* of verified mechanisms (contagion, serialization, the ASCII multiplier). Every `[INFER]` is
stated as a deduction from a cited `[CERT]`, never as an observation.

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | `getPollGroupCode()` returns the entry or `this`, setting `devicePoll`/`pointPoll` | `[CERT]` | `BModbusClientProxyExt.java:68-105` read in full |
| 2 | `BDataSourceEnum` default is `pointPoll` | `[CERT]` | `BDataSourceEnum.java:20` |
| 3 | Same code object ⇒ same group instance (identity-keyed hashtable) | `[CERT]` | `BBasicPollGroup.java:116-133` |
| 4 | FC map 4/3/1/2 by register type, in the poll group | `[CERT]` | `BModbusClientPollGroup.java:52-60` |
| 5 | Disabled entry returns without polling | `[CERT]` | `BModbusClientPollGroup.java:27-29` |
| 6 | Learn breaks runs on `difference != 1`, emits only `count > 1` | `[CERT]` | `BModbusClientDevice.java:721-736` |
| 7 | Offsets +40001 / +30001 / +1 | `[CERT]` | `BModbusClientDevice.java:734,771,808` |
| 8 | `maxReadSize = 125 - 125 % minReadSize`, halved when `modbusMode == 0` | `[CERT]` | `BModbusClientDevice.java:585-589` |
| 9 | `readStatusRegisters` cap 2000, same halving | `[CERT]` | `BModbusClientDevice.java:632-636` |
| 10 | `modbusMode == 0` is ASCII | `[CERT]` | `BModbusDataModeEnum.java:18-20` |
| 11 | `MAX_READ_DATA_SIZE`/`MAX_WRITE_DATA_SIZE` have no consumer | `[CERT]` | `rg` over all 5 driver jars → declaration only (re-run, second method) |
| 12 | `consecutivePointsToPoll` facets 0..9999; `readGroupSize` facets 1..2 | `[CERT]` | `BDevicePollConfigEntry.java:52-53` |
| 13 | Rates 1 s / 5 s / 30 s | `[CERT]` | `BPollScheduler.java:23-35` |
| 14 | Group frequency = fastest subscribed proxy | `[CERT]` | `BBasicPollGroup.java:101-114` |
| 15 | Comm per device on TCP; per network on gateway and serial | `[CERT]` | `BModbusTcpDevice.java:145` · `BModbusTcpGateway.java:103` · `BModbusAsyncNetwork.java:242` |
| 16 | One Rx thread per Comm, priority 5 | `[CERT]` | `ModbusTcpComm.java:16-23` |

Tokens grep-confirmed in their cited source: **16 / 16**.

**HYPOTHESIS REFUTED, recorded per METHODOLOGY §8** (a refuted premise is a finding): the scouting note in
`RESEARCH-STATE-modbus.md` suspected an unclamped request size. It is wrong — the clamp is in
`readRegisters`/`readStatusRegisters`, not in the unused `ModbusMessageConst` fields. The RESEARCH-STATE
note is superseded by §295.4. Per the RE-MEASURE rule, the "no consumer" negative was re-derived by a second
`rg` pass across all five driver jars before being written.

Sources preserved this iteration: `sources/decompiled/driver-rt-poll/` (`BPollScheduler`) and
`sources/decompiled/basicDriver-rt-poll/` (`BBasicPollGroup`, `BBasicPollScheduler`) — targeted classes, not
whole jars, registered in `sources/SOURCES.md`. Model tier: **no delegation — inline**.

## 295.x — Connections

- **[Block 294]** — the component tree these mechanisms live in; §294.5's `maxFailsUntilDeviceDown` is the
  counter `sendModbusMessage` feeds.
- **[Block 131]** — the bytes each transaction emits; this block closes the *read* half of its open `P1-fc`
  note by locating the register-type→FC mapping (§295.2), and quantifies the cost of its §131.6 ASCII framing
  (§295.4).
- **[Block 137]** — the LOGO! 8 register map is exactly the input `Learn Optimum Device Poll Config` would
  chew on; whether that map coalesces well is now answerable and belongs to a future dynamic pass.
- **[Block 4]/[Block 7]** — `BPollScheduler` and the basic-driver poll machinery Modbus inherits.

**Gaps opened by this block**:
- **M11-a** — the WRITE path (`usePresetMultipleRegister` / `useForceMultipleCoil` from [Block 294] §294.4,
  FC 5/6 vs FC 15/16, and `MAX_WRITE_DATA_SIZE = 16` being dead) → folded into **M5**.
- **M11-b** — `ModbusTcpRxDriver` (358 lines: socket manager, reconnect policy, transaction-id matching,
  partial-frame handling) was NOT opened here; only its ownership of the socket was established → **new
  medium gap**.
- **M11-c** — whether `dispatch()`/`getResponse(0)` serializes per Comm or allows pipelining, and what the
  `0` argument means (timeout? no-wait?) → requires reading `basicDriver`'s `Comm`/dispatch layer →
  **new medium gap**.
