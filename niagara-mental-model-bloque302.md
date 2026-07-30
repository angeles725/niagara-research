# Block 302 — The read path: how a point slices the shared buffer, how `absoluteAddress` is computed, and the two error codes that mean "you fell out of your group"

> Focus **modbus**, gap **M15** (opened by [Block 297] as M3-a, absorbing M4-b from [Block 298] and M5-b
> from [Block 299]). The inbound half of the data cycle: what `devicePoll(entry)` does with the shared
> buffer a group read produced, what the single-point path does instead, how `absoluteAddress` is derived
> from `dataAddress` plus the device base offset, and the subscription lifecycle that invalidates it all.
> READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/modbusCore-rt/` (Vineflower, jar `a0b67420…`) —
> `client/point/BModbusClientProxyExt`, `client/point/BModbusClientNumericProxyExt`,
> `client/BModbusClientDevice`.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 295] (the group read that fills the
> buffer), [Block 296] (the base addresses consumed here), [Block 297] (`absoluteAddress` as a property),
> [Block 299] (the write path, its exact counterpart), [Block 300] (error codes 102/103 made concrete).

---

## 302.1 — Two entry points, mirroring the two poll paths `[CERT]`

`BModbusClientProxyExt` declares one abstract method for the grouped path and implements the individual one
`[CERT]` `modbusCore-rt/…/client/point/BModbusClientProxyExt.java:254-268`:

| Path | Method | What it does |
|---|---|---|
| `pointPoll` | `poll()` | guards on `configFault`/`isUnoperational()`, traces, then calls `this.read()` — one Modbus transaction for this point |
| `devicePoll` | `devicePoll(BDevicePollConfigEntry)` *(abstract)* | **no transaction** — extracts this point's bytes from the buffer the group read already filled |

`[INFER]` this is the payoff of the grouping decision of [Block 295] §295.1 made concrete: for a grouped
point, `devicePoll` is pure local computation. The network cost was already paid once for the whole group.

## 302.2 — `devicePoll`: build a synthetic response from the shared buffer `[CERT]`

The numeric implementation `[CERT]` `…/client/point/BModbusClientNumericProxyExt.java:280-305`:

```java
int numRegisters = DataTypeUtil.getRegisterCount(this.getDataType());
ModbusResponse rsp = new ModbusResponse(device.modbusNet().getModbusMode(), device);
int pointAddress = this.getAbsoluteAddress().getDataAddress();
if (this.isHoldingRegisterType()) {
   rsp.data          = device.getHoldingRegisterValues(pointAddress, numRegisters, entry);
   rsp.exceptionCode = device.getHoldingRegistersReadStatus(pointAddress, numRegisters, entry).getErrorCode();
} else { … getInputRegisterValues / getInputRegistersReadStatus … }
rsp.byteCount    = (byte) rsp.data.length;
rsp.numberPoints = numRegisters;
this.setOutValues(rsp);
```

`[INFER]` the design is neat: rather than having two decoding paths, the grouped path **fabricates a
`ModbusResponse`** out of the shared buffer and hands it to `setOutValues()` — the same method the
single-point path feeds with a real response. Decoding (byte order, datatype, status) therefore has exactly
one implementation, shared by both paths. The comm status is copied per point from the entry, so each point
in a group reports the group's read status individually.

Failures are swallowed: a `ModbusException` is caught and only logged **if trace is on** `[CERT]` `:299-303`.
`[INFER]` with trace off — the normal production setting — a point that cannot extract its bytes simply
does not update, with nothing in the log. Its status still reflects whatever `setOutValues` last set.

## 302.3 — The slicing arithmetic, and the two ways it refuses `[CERT]`

`getHoldingRegisterValues(address, numRegisters, entry)` `[CERT]`
`…/client/BModbusClientDevice.java:412-432+`:

```java
byte[] registerData = new byte[numRegisters * 2];
startAddress = entry.getStartAddress().getDataAddress();      // throws → ModbusException(103)
if (address < startAddress)                                    throw new ModbusException(102);
int registerOffset = address - startAddress;
if (registerOffset + numRegisters > entry.getConsecutivePointsToPoll())
                                                               throw new ModbusException(102);
int registerByteOffset = registerOffset * 2;
if (entry.getByteArray() == null)                              throw new ModbusException(103);
if (entry.getByteArray().length < entry.getConsecutivePointsToPoll() * 2)  …
```

The two codes are the ones [Block 300] §300.2 catalogued `[CERT]` `ModbusErrorCodes.java:6-7`:

| Code | Name | Raised when |
|---|---|---|
| **102** | `REGISTER_NOT_POLLED_BY_DEVICE` | the point's span starts before the entry, or runs past its end |
| **103** | `DATA_NOT_AVAILABLE` | the entry has no start address, or its buffer is null/short |

`[INFER]` this is the runtime enforcement of the **full-containment** rule that [Block 297] §297.1 and
[Block 295] §295.1 established at subscription time. The containment is checked **twice**: once when
choosing the group (a failure there routes the point to `pointPoll`), and again on every extraction (a
failure here raises 102). The second check exists because the entry is mutable — `startAddress` or
`consecutivePointsToPoll` can be edited while points are bound to it. `[INFER]` so 102 in production most
often means *someone narrowed the poll-config entry under a live point*, which is exactly the trap
[Block 295] §295.2 described for the disabled-entry case.

Note the asymmetry `[INFER]`: `address < startAddress` throws immediately, but there is no upper bound check
on `address` alone — the overrun is caught by the `registerOffset + numRegisters` test instead, which is the
correct one because it accounts for multi-register datatypes.

## 302.4 — `absoluteAddress` = `dataAddress` + base, with a format-dependent twist `[CERT]`

`setCurrentAbsoluteAddress()` `[CERT]` `…/client/point/BModbusClientProxyExt.java:225-250`:

```java
int baseAddr = 0;
try { baseAddr = device.getRegisterBaseAddress(this.getRegisterType()); }
catch (NullPointerException e) { }            // silently leaves baseAddr = 0
catch (NumberFormatException e) { baseAddr = 0; }

BFlexAddress absAddr = (BFlexAddress) this.getDataAddress().newCopy();
int rawAddress = absAddr.isModbusFormat() ? Integer.valueOf(absAddr.getAddress())   // NOT getDataAddress()
                                          : absAddr.getDataAddress();
absAddr.setAddressFromInt(rawAddress + baseAddr);
this.setAbsoluteAddress(absAddr);
```

The branch matters `[INFER]`: in **modbus** format the *displayed* number is used (e.g. `40001`), so the
base offset is added in display space and the bank subtraction happens later, when `getDataAddress()` is
called on the result ([Block 296] §296.3). In hex/decimal format the raw value is used directly. Both end
up correct, but they add the base at different points in the conversion — which is why a device base address
must itself be in the matching bank, the validation [Block 296] §296.4 enforces.

`[INFER]` the two swallowed exceptions are a robustness choice with a cost: if the device reference is not
yet resolved (`NullPointerException`) the base offset silently becomes **0**, so a point can compute an
absolute address that ignores the device's base until the next recompute. Nothing reports it.

## 302.5 — Subscription lifecycle: where the cached decisions die `[CERT]`

Three methods keep the cached group decision honest `[CERT]`
`…/client/point/BModbusClientProxyExt.java:119-153`:

| Method | Behaviour |
|---|---|
| `readSubscribed(cx)` | **recomputes `absoluteAddress` first**, then delegates up, then sets `subscribed = true` under `pollSync` |
| `readUnsubscribed(cx)` | returns early if not subscribed; clears the flag under `pollSync` |
| `adjustPollSubscription()` | if subscribed: unsubscribe → **null the `lastPollGroupCode`** → resubscribe; if not: just null the cache |

`[INFER]` `adjustPollSubscription()` is the hinge. It is what `updateProxyPointSubscriptions()` reaches
([Block 296] §296.4, called on any base-address or poll-config change): nulling `lastPollGroupCode` forces
the next `getPollGroupCode()` to re-run the containment search, which is how a point migrates between
`devicePoll` and `pointPoll` without a station restart. The unsubscribe/resubscribe sandwich is what moves
it between group instances in `BBasicPollGroup`'s hashtable ([Block 295] §295.2).

Errors here are logged at **error** level rather than swallowed `[CERT]` `:146-148` — `[INFER]` the one
place in this path where a failure is visible without trace enabled, appropriately, since a failed
resubscribe would leave a point silently unpolled.

`started()` also recomputes the absolute address and, for a **writable** point, resolves the device `[CERT]`
`:155-158` — consistent with the write path of [Block 299].

## 302.6 — What this closes

- **M3-a** (the read/decode path) — answered: there is one decoder, fed either by a real response or by a
  fabricated one (§302.2).
- **M4-b** (server write-through) — *not* answered here and re-scoped: the server side has no `devicePoll`
  analogue because it has no poll engine ([Block 298] §298.1); how station points push into the four
  `IntHashMap`s belongs with the server request-service path, gap **M16**. Recorded rather than silently
  dropped.
- **M5-b** (`IPropertyValidator` on the preset register) — checked and found out of this path's scope: it is
  a write-side validator on `BModbusClientPresetRegister`, not part of the read cycle. Left inside **M16**'s
  neighbourhood as a minor item; it is not worth its own gap.

## 302.7 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 25 | 24 |
| `[CERT-doc]` | 1 | 1 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 11 | 10 |
| **[INFER]/[CERT*] ratio** | | **10/25 = 0.40** |

Script exit 0. Ratio 0.40 — the highest in this focus. Honest reading: this block documents a **mechanism**
(a slicing calculation and a lifecycle) whose consequences need stating, and every `[INFER]` hangs off a
cited line. It is not an exhaustion signal for the focus, but it does say this particular path is now
described rather than merely quoted.

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | `poll()` calls `read()`; `devicePoll` is abstract | `[CERT]` | `BModbusClientProxyExt.java:254-268` |
| 2 | `devicePoll` builds a `ModbusResponse` and calls `setOutValues` | `[CERT]` | `BModbusClientNumericProxyExt.java:280-298` read in full |
| 3 | Its `ModbusException` is logged only when trace is on | `[CERT]` | `:299-303` |
| 4 | Slicing arithmetic and the 102/103 throws | `[CERT]` | `BModbusClientDevice.java:412-432` read in full |
| 5 | 102 = `REGISTER_NOT_POLLED_BY_DEVICE`, 103 = `DATA_NOT_AVAILABLE` | `[CERT]` | `ModbusErrorCodes.java:6-7` |
| 6 | `absoluteAddress` branches on `isModbusFormat()` using the displayed value | `[CERT]` | `BModbusClientProxyExt.java:234-244` |
| 7 | Base-address lookup swallows NPE, leaving base 0 | `[CERT]` | `:227-232` |
| 8 | `readSubscribed` recomputes the absolute address before delegating | `[CERT]` | `:119-125` |
| 9 | `adjustPollSubscription()` nulls `lastPollGroupCode` on both branches | `[CERT]` | `:139-153` |
| 10 | Resubscribe failures log at error level | `[CERT]` | `:146-148` |

Tokens grep-confirmed in their cited source: **10 / 10**. No new sources preserved. Model tier:
**no delegation — inline**.

Marker note: the single `[CERT-doc]` the script counts is **this sentence naming the marker**, not a
citation — the guide has no topic on the read path, so no documentary evidence was available to cite.
Consistent with [Block 297] §297.7, where the code-only balance first appeared. Recorded as a real
measurement, not an omission.

## 302.x — Connections

- **[Block 295]** — fills the buffer this block reads; §302.3's double containment check is its §295.1 rule enforced at runtime.
- **[Block 296]** — `updateProxyPointSubscriptions()` reaches §302.5's `adjustPollSubscription()`; the base addresses feed §302.4.
- **[Block 297]** — `absoluteAddress` as a declared property; the datatypes that set `numRegisters`.
- **[Block 299]** — the write counterpart; both share `setCurrentAbsoluteAddress()` and the device resolution in `started()`.
- **[Block 300]** — codes 102/103 catalogued there, raised here.

**Gaps opened by this block**: none new. **M4-b** is re-scoped into **M16** (§302.6) rather than closed.
