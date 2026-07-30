# Block 311 — Thread safety of the server maps: `IntHashMap` gives no guarantees, the master writes from a dedicated thread, and nothing synchronises the two writers

> Focus **modbus**, gap **M20** — the open question [Block 306] §306.5 deliberately refused to turn into a
> defect claim. This block answers it as far as the evidence allows and states precisely where the chain
> stops. READ-ONLY. Corpus language: ENGLISH.
>
> Sources (primary): `docSource` **original Tridium source with javadoc** —
> `nre/javax/baja/nre/util/IntHashMap.java` (372 lines);
> `sources/decompiled/modbusTcpSlave-rt/…/comm/ModbusUnsolicitedReceive.java`,
> `…/comm/ModbusTcpServer.java` (jar `8d78f0a5…`); `sources/decompiled/modbusCore-rt/server/` (jar `a0b67420…`).
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> Layer 26 (Communication protocols — driver focus). Connects [Block 306] (which opened this),
> [Block 298] (the four maps), [Block 303] (the master's write path), [Block 305] (the client-side Rx thread).

---

## 311.1 — `IntHashMap` offers no concurrency guarantee at all `[CERT]`

The container behind all four server banks ([Block 298] §298.1) is `javax.baja.nre.util.IntHashMap`, and it
is available as **original Tridium source with javadoc**, not decompiled `[CERT]`
`docSource/…/nre/javax/baja/nre/util/IntHashMap.java`.

Measured over the whole 372-line file `[CERT]`:

| Query | Count |
|---|---|
| `synchronized` | **0** |
| `volatile` | **0** |
| `thread` / `concurren` (any case, incl. javadoc) | **0** |

Its class javadoc describes only its purpose — *"IntHashMap is an optimized hashtable for hashing objects by
an integer keys. It removes the need to use wrapper Integers as with the standard collection classes"* — plus
a note that it predates generics and *"New API code shouldn't use this class"* `[CERT]` `:9-16`.

`[INFER]` so it is a plain, unsynchronised hashtable with no documented threading contract. It also
**rehashes** as it grows `[CERT]` `:121, 173` — the classic window in which a concurrent reader can traverse
a table being rebuilt.

## 311.2 — The master writes from a dedicated thread `[CERT]`

`ModbusUnsolicitedReceive implements Runnable` and starts **its own named thread** `[CERT]`
`modbusTcpSlave-rt/…/comm/ModbusUnsolicitedReceive.java:19, 23, 39-40`:

```java
this.myThread = new Thread(this, "ModTcpSlave:UnsolRcv");
this.myThread.start();
```

with a clean shutdown — `interrupt()` then `join(10000L)` `[CERT]` `:45-48`. Alongside it,
`ModbusTcpServer` runs the accept loop on a second thread, `"ModTcpSlave:Server"` `[CERT]`
`…/comm/ModbusTcpServer.java:30-31`.

So the master's writes ([Block 303] §303.3 — FC 5/6/15/16 → `device.setCoilStatusValue` /
`setHoldingRegisterValues`) execute on **`ModTcpSlave:UnsolRcv`**, a thread dedicated to processing inbound
frames. `[CERT]` by composition of the dispatch site and the thread that runs it.

## 311.3 — Nothing synchronises the two writers `[CERT]`

[Block 306] §306.3 established that the station point and the master call the **same setter** with no origin
flag and no lock. Re-checked here at the setter itself: `BModbusServerDevice.setHoldingRegisterValues`
is **not** `synchronized` and takes no lock `[CERT]`
`modbusCore-rt/…/server/BModbusServerDevice.java:398-407` — it reads, mutates and rebuilds the persistence
blob in a bare loop.

So the three facts stand together `[CERT]`:

1. the map is unsynchronised (§311.1);
2. the master mutates it from `ModTcpSlave:UnsolRcv` (§311.2);
3. neither the setter nor either call site holds a lock (§311.3, [Block 306] §306.3).

## 311.4 — What follows, and the one link I did NOT verify

`[INFER]` **if** the station point's `updateOutput` runs on any thread other than `ModTcpSlave:UnsolRcv`,
then two threads mutate an unsynchronised hashtable with no coordination, and the driver has a genuine data
race — with a rehash window (§311.1) that can corrupt more than a single cell.

The supporting argument that they *are* different threads `[INFER]`: the inbound thread's only job is
processing received frames, and the server side is strictly one-directional — points write **into** the maps
([Block 306] §306.2) and serving reads **out of** them ([Block 303] §303.4); nothing propagates from a map
back to a point. So `updateOutput` cannot be reached from the frame-processing thread; it is driven by
station point value changes.

**What I did not do**: read Niagara's engine threading contract to confirm which thread actually invokes
`updateOutput`/`writeDesired`. That would settle it outright. Without it the conclusion is a strong
conditional, not a measured fact — and per the discipline [Block 306] §306.5 set for this gap, **I am not
upgrading it to a defect claim.**

**Gap disposition**: M20 is closed as **partially determinable**. What is measured: the container's lack of
guarantees, the master's dedicated thread, the absence of locks. What is not: the engine-side thread
identity. The residue is recorded as **M22** rather than left implicit.

`[INFER]` a practical note that does not depend on the unresolved link: the exposure is bounded by
*traffic*. A slave device that no master writes to (all four banks read-only from the master's side) has one
writer and no race regardless. The risk is specific to **master-writable banks under concurrent station
updates** — i.e. exactly the setpoint scenario [Block 306] §306.3 warned about for a different reason.

## 311.5 — Collateral: a second debug print shipped in production `[CERT]`

`ModbusTcpServer.start()` opens with `System.out.println("ModbusTcpServer.start()")` `[CERT]`
`modbusTcpSlave-rt/…/comm/ModbusTcpServer.java:22-23`.

This is the **second** stdout debug statement found in the shipped driver, after
`System.out.println("how'd we get here")` in [Block 298] §298.6. `[INFER]` both are in `modbusTcpSlave`/
`modbusCore` server code, both write to stdout rather than the station log, and neither is trace-gated —
they print unconditionally on every station with a Modbus TCP slave network started.

## 311.6 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 25 | 24 |
| `[CERT-doc]` / `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 6 | 5 |
| **[INFER]/[CERT*] ratio** | | **5/24 = 0.21** |

Script exit 0.

**Block type: EVIDENCE (partially determinable gap).**

Load-bearing claims:

| # | Claim | Marker | Verified how |
|---|---|---|---|
| 1 | `IntHashMap` has 0 `synchronized`, 0 `volatile` | `[CERT]` | `rg -c` over the whole 372-line original source |
| 2 | Its javadoc never mentions threading | `[CERT]` | `rg -ci 'thread\|concurren'` → 0 over the same file |
| 3 | Javadoc text as quoted | `[CERT]` | `IntHashMap.java:9-16` verbatim |
| 4 | It rehashes on growth | `[CERT]` | `:121, 173` |
| 5 | `ModbusUnsolicitedReceive` starts thread `"ModTcpSlave:UnsolRcv"` | `[CERT]` | `:19, 23, 39-40` |
| 6 | Shutdown is interrupt + `join(10000L)` | `[CERT]` | `:45-48` |
| 7 | `ModbusTcpServer` runs a second thread `"ModTcpSlave:Server"` | `[CERT]` | `ModbusTcpServer.java:30-31` |
| 8 | `setHoldingRegisterValues` is not `synchronized` and takes no lock | `[CERT]` | `BModbusServerDevice.java:398-407` read in full |
| 9 | `System.out.println("ModbusTcpServer.start()")` | `[CERT]` | `ModbusTcpServer.java:22-23` |

Tokens grep-confirmed in their cited source: **9 / 9**. Claims 1 and 2 are ABSENCES measured over the
**complete file** with three differently-keyed queries, on the **original** Tridium source rather than
decompiled output — the strongest form available for this kind of negative.

**Discipline note**: the headline conclusion of this gap is stated as a **conditional** (§311.4), because
one link in the chain — the engine-side thread identity — was not measured. Stating "the driver has a data
race" flatly would have been the natural-sounding claim and is probably true; it is not what the evidence
in hand supports, so it is not what is written. This mirrors [Block 303] §303.5's deferral, which
[Block 307] later vindicated.

No new sources preserved. Model tier: **no delegation — inline**.

## 311.x — Connections

- **[Block 306]** — opened this question and set the discipline for answering it; §306.3's "no lock" is re-verified at the setter here.
- **[Block 298]** — the four maps; §298.6's debug print, now joined by a second (§311.5).
- **[Block 303]** — the master's write arms, now located on a named thread.
- **[Block 305]** — the client-side Rx thread; the slave has its own pair.

**Gaps opened by this block**:
- **M22** — which Niagara engine thread invokes `updateOutput`/`writeDesired` on a proxy extension. Settles
  §311.4's conditional outright. Not Modbus-specific: it is a framework question (`BTuningPolicy`/engine
  threading) that would serve every driver focus → **new low gap**.
