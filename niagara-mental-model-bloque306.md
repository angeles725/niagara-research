# Block 306 — Server write-through: station points and remote masters write the SAME map with no arbitration, and the persistence blob is rebuilt once per byte

> Focus **modbus**, gap **M18** — the item carried since [Block 298] opened it as M4-a/M4-b and twice
> refused to declare covered ([Block 302] §302.6, [Block 303] §303.6). How a station point pushes its value
> into the four `IntHashMap`s of [Block 298], what happens when a remote master writes the same address,
> and what that write costs. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `sources/decompiled/modbusCore-rt/server/` (jar `a0b67420…`) —
> `point/BModbusServerProxyExt`, `point/BModbusServerNumericProxyExt`, `BModbusServerDevice`,
> `datatypes/BModbusRegisterRangeEntry`; `sources/decompiled/modbusTcpSlave-rt/…/comm/ModbusUnsolicitedReceive`
> (jar `8d78f0a5…`).
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 298] (the maps and their persistence),
> [Block 303] (the master's write path), [Block 297] (the byte converters), [Block 299] (the client write
> path, its mirror), [Block 302] (which re-scoped this gap here).

---

## 306.1 — A server point is an ordinary writable point whose "device" is a hash map `[CERT]`

`BModbusServerProxyExt extends BModbusProxyExt implements ModbusErrorCodes, BIBasicPollable` `[CERT]`
`modbusCore-rt/…/server/point/BModbusServerProxyExt.java:17`. Its mode is derived, not configured `[CERT]`
`:27-29`:

```java
public BReadWriteMode getMode() {
   return this.getParentPoint().isWritablePoint() ? BReadWriteMode.readWrite : BReadWriteMode.readonly;
}
```

`[INFER]` so whether a server point *pushes* into the map is decided by the Niagara point type above it
(`NumericWritable` vs `NumericPoint`), not by any Modbus setting. A read-only point exposes nothing —
it is the writable ones that populate the bank.

On an address change the extension goes stale, re-checks configuration and, for a writable point,
**re-issues the write** `[CERT]` `:39-50`:

```java
if (prop.equals(dataAddress)) {
   this.setStale(true, null);
   this.checkConfiguration();
   if (this.getParentPoint().isWritablePoint()) this.getTuning().writeDesired();
}
```

`[INFER]` that is the re-population path: move a point's address and its value is immediately written to the
new cell — but nothing clears the **old** cell, which keeps its last value and stays served to masters.

## 306.2 — The write-through itself `[CERT]`

`updateOutput(BStatusValue out)` on the numeric server point `[CERT]`
`…/server/point/BModbusServerNumericProxyExt.java:287-317`:

1. reject early if the address is invalid — `writeFail("Illegal Modbus address")`;
2. convert by datatype, using the **device's** byte order for the multi-register cases —
   `to8ByteLongArray(fValue, device.getLong64BitDataByteOrder(), …)`,
   `to8ByteDoubleArray(fValue, device.getDouble64BitDataByteOrder())`, and the 16/32-bit helpers of
   [Block 297] §297.5–§297.6;
3. `device.setHoldingRegisterValues(pointAddress, dataOut)` or `setInputRegisterValues(...)` depending on
   `isHoldingRegisterType()`.

Two details worth naming `[INFER]`:

- **it uses `getDataAddress()`, not `absoluteAddress`** `[CERT]` `:295`. Correct, and structural: the server
  has no base-address offsets — it declares *ranges* instead ([Block 298] §298.2). The two-address model of
  [Block 302] §302.4 is a client-only concept;
- exceptions are caught and logged **only when trace is on** `[CERT]` `:314-318` — the same silent-failure
  shape as the client read path ([Block 302] §302.2). A server point that cannot write its cell simply
  stops updating what masters see.

## 306.3 — The master and the station write the same setter — last writer wins `[CERT]`

This is the question M18 was carried for. The master's path, on the TCP slave dispatcher `[CERT]`
`modbusTcpSlave-rt/…/comm/ModbusUnsolicitedReceive.java:217-247`:

| FC | Validation | Call |
|---|---|---|
| 5 | `device.isCoilAddressValid(address, numberPoints)` | `device.setCoilStatusValue(address, (data[0] & 255) == 255)` |
| 6, 16 | `device.isHoldingRegisterAddressValid(...)` | **`device.setHoldingRegisterValues(address, writeRequest.data)`** |
| 15 | `device.isCoilAddressValid(...)` | `device.setCoilStatusValue(address, numberPoints, data)` |
| else | — | error |

`setHoldingRegisterValues` is **the same method** the station point calls in §306.2 `[CERT]`. There is no
origin flag, no ownership check, no lock visible on either path, and no arbitration of any kind.

`[INFER]` therefore: **a writable station point and a remote master compete for the same map cell, and the
last write wins.** In practice the station point is the more persistent writer, because §306.1's
`writeDesired()` fires whenever its value changes — so a master's write to a holding register that a station
point also drives survives only until that point next updates. An integrator exposing a *setpoint* for a
master to write must therefore leave that address **unbound** on the station side; binding it to a writable
point makes the station silently authoritative.

Note also the two validation styles for the same operation `[CERT]`: the master path checks
`isHoldingRegisterAddressValid()` **before** calling and returns a Modbus exception on failure `:232-236`,
while the station path discovers the problem **inside** the setter, which throws `ModbusException(103)`
`[CERT]` `BModbusServerDevice.java:400-401`. Same invariant, two enforcement points.

FC 5's `(data[0] & 255) == 255` `[CERT]` `:226` is the `0xFF` encoding the client emits for a single-coil
force ([Block 299] §299.1) — the two halves of the driver agree on the wire convention.

## 306.4 — The persistence blob is rebuilt once per byte `[CERT]`

`setHoldingRegisterValues` `[CERT]` `BModbusServerDevice.java:398-407`:

```java
for (int i = 0; i < data.length; i++) {
   if (this.holdingRegisterByteArray.get(address * 2 + i) == null)
      throw new ModbusException(103);
   this.holdingRegisterByteArray.put(address * 2 + i, data[i]);
   this.getValidHoldingRegistersRange().setPersistedData(this.holdingRegisterByteArray, true);   // <-- inside the loop
}
```

The `setPersistedData(IntHashMap, boolean)` call is **inside** the byte loop. That method walks the
**entire** declared range, reading every address out of the map to rebuild the blob — [Block 298] §298.3
documented it at `BModbusRegisterRangeEntry.java:152-185`.

`[INFER]` the cost, stated concretely: writing one `floatType` point (4 bytes) rebuilds the whole range's
persistence blob **four times**; a `doubleType` or 64-bit point, **eight times**. With the default range of
64 registers ([Block 298] §298.2) that is 8 × 128 bytes of pointless copying per write. The `size` facet
allows up to `Integer.MAX_VALUE`, so the work grows linearly with the declared range while the number of
rebuilds grows with the datatype width — a large declared range makes every single-point write expensive.

Hoisting the call out of the loop would be behaviour-preserving: the blob is derived entirely from the map,
so rebuilding it once after the loop yields the identical result. **This is a genuine performance defect,
not a design choice** — recorded here rather than escalated, since it is invisible without profiling and
harmless at default range sizes. It is consistent with the code-quality pattern already recorded in
[Block 295] §295.3, [Block 298] §298.6 and [Block 303] §303.2.

Also note `[INFER]`: because the rebuild happens on **every** map write, and the master's path calls the
same setter (§306.3), a busy master hammering holding registers pays the same cost — on the station's
thread, not its own.

## 306.5 — M18 closed, and what it did not answer

**Closed**: the write-through mechanism (§306.2), the collision semantics (§306.3), and the cost (§306.4).

**Not answered, and not turned into a new gap**: whether the `IntHashMap` accesses are safe under genuine
concurrency. The master writes from the Rx thread ([Block 305] §305.1) and the station point writes from the
engine thread; neither path shows a lock, and `javax.baja.nre.util.IntHashMap` was not read. Stating
"there is a race" would require reading that class and knowing Niagara's threading contract for
`updateOutput` — I have neither, so the honest record is: **no synchronisation was observed on either
path**, and whether that matters is unresolved. Logged as **M20** rather than asserted.

## 306.6 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 28 | 27 |
| `[CERT-doc]` | 1 | 1 |
| `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 7 | 6 |
| **[INFER]/[CERT*] ratio** | | **6/28 = 0.21** |

Script exit 0. (The single `[CERT-doc]` counted is the sentence below naming the marker, not a citation.)

**Block type: EVIDENCE.**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | `getMode()` derives read/write from `isWritablePoint()` | `[CERT]` | `BModbusServerProxyExt.java:27-29` |
| 2 | Address change → stale + `checkConfiguration()` + `writeDesired()` | `[CERT]` | `:39-50` |
| 3 | `updateOutput` converts then calls the device setter | `[CERT]` | `BModbusServerNumericProxyExt.java:287-317` read in full |
| 4 | It uses `getDataAddress()`, not `absoluteAddress` | `[CERT]` | `:295` |
| 5 | Its exception is trace-gated | `[CERT]` | `:314-318` |
| 6 | Master FC 6/16 calls `setHoldingRegisterValues` — the same method | `[CERT]` | `ModbusUnsolicitedReceive.java:231-236` vs `BModbusServerNumericProxyExt.java:310` |
| 7 | No origin flag / ownership check / lock on either path | `[CERT]` | both call sites and the setter read in full (ABSENCE, full-scope over the three methods involved) |
| 8 | Master validates before calling; station discovers inside the setter | `[CERT]` | `:232-236` vs `BModbusServerDevice.java:400-401` |
| 9 | FC 5 uses the `0xFF` convention | `[CERT]` | `:226` |
| 10 | `setPersistedData` is called inside the byte loop | `[CERT]` | `BModbusServerDevice.java:398-406` verbatim |
| 11 | `setPersistedData` walks the whole range | `[CERT]` | `BModbusRegisterRangeEntry.java:152-185`, established in [Block 298] §298.3 and re-read here |

Tokens grep-confirmed in their cited source: **11 / 11**. Claim 10 — the load-bearing one — was read
verbatim and the loop boundaries checked twice, since "call inside a loop" is exactly the kind of claim a
decompiler's brace placement could distort; the `for` header and the closing brace were both located.
Claim 7 is an ABSENCE bounded to the three methods that make up the write path; it is **not** a claim that
the driver is unsynchronised overall — see §306.5 and M20.

No new sources preserved. `[CERT-doc]`: none — the guide has no topic on server write-through.
Model tier: **no delegation — inline**.

## 306.x — Connections

- **[Block 298]** — opened this gap; §298.3's persistence machinery is what §306.4 shows being over-called, and §298.2's ranges are the invariant both write paths enforce differently.
- **[Block 303]** — the master's dispatcher; §306.3 joins its write arms to the station's setter.
- **[Block 302]** — re-scoped this gap here; its two-address model is confirmed client-only in §306.2.
- **[Block 297]/[Block 299]** — the byte converters and the client's mirror-image write path.
- **[Block 305]** — the Rx thread the master's write arrives on, relevant to M20.

**Gaps opened by this block**:
- **M20** — thread-safety of the four `IntHashMap`s: master writes arrive on the Rx thread, station writes
  on the engine thread, and no synchronisation was observed on either path. Needs
  `javax.baja.nre.util.IntHashMap` plus the threading contract of `updateOutput` → **new low gap**, stated
  as an open question rather than a defect claim.
