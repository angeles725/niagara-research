# Block 544 — The priority-array WRITE path end-to-end: link → InN → arbitration → proxyExt → driver wire, and how the N4 16-level array maps onto the BACnet WriteProperty priority (but collapses for Modbus)

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC8 — the priority-array write path, end-to-end)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY. Delegated sonnet sweep tracing the chain across `baja`, `control-rt`, `driver-rt`,
`bacnet-rt`, `modbusCore-rt`; the load-bearing links (proxyExt write trigger, BACnet priority mapping)
token-verified inline against vineflower.
**Primary sources** `[CERT]`:
- `organized/driver/driver-rt/vineflower/javax/baja/driver/point/BProxyExt.java`, `Tuning.java`
- `organized/bacnet/bacnet-rt/vineflower/javax/baja/bacnet/point/BBacnetProxyExt.java` + `PointCmd`
- `organized/control/control-rt/vineflower/javax/baja/control/{BControlPoint,WritableSupport}.java`
- `organized/baja/baja/…/javax/baja/sys/BLink.java`; `organized/modbusCore/.../BModbusClientNumericProxyExt.java`

**Scope**: how a value written into a writable point's priority level actually reaches the physical device —
the transport from `InN` slot to wire. [Block 536] covered the ARBITRATION (which level wins); this block
covers the CHAIN from that resolved `out` to the driver, and the crucial fact that only BACnet preserves the
16-level array to the wire. REMITTANCE: B536 arbitration, [Block 46] SPA writes, [Block 6] §6.2 link kernel.

---

## 544.1 The chain (six hops)

`kitControl block .out` → **link** → writable `InN` → **re-arbitration** (B536) → `out` → **proxyExt** →
**Tuning** (write policy) → **driver `write()`** → wire. Each hop below is code-cited.

## 544.2 Link → InN → re-arbitrate [CERT]

When a source `.out` changes, its knob fires `BLink.propagate`; for property→property it enters
`propagatePropertyToProperty()` `[CERT] BLink.java` which sets the exact target `InN` slot
(`t.setDouble(tProp, s.getDouble(sProp), null)`, type-coerced per [Block 6] §6.2.5). The non-`noExecute`
Context makes `BControlPoint.fwChanged()` call `this.execute()` `[CERT] BControlPoint.java:343-362`, which
re-runs `WritableSupport.onExecute` (B536 arbitration) → new `out`. `WritableSupport.changed()` strips any
stale `activeLevel` facet the incoming value carried, so the winning level is recomputed clean.

## 544.3 out → proxyExt → Tuning → driver [CERT]

`BControlPoint.doExecute()` runs `onExecute` (arbitration) then `executeExtensions` — `proxyExt` first
([Block 536] §536.8). The write trigger is in `BProxyExt.onExecute` `[CERT] BProxyExt.java:363-366`:
```java
if (this.getMode().isWrite() && (this.hasWorkingChanged(working) || this.forceWrite())) {
    this.forceWrite(false);
    this.convertProxyToDevice(working, this.getWriteValue());
    this.getTuning().writeDesired();
}
```
`Tuning.writeDesired()` `[CERT] Tuning.java` honors the **write policy**: if `minWriteTime > 0` the write is
DEFERRED (rate-limited), else it calls `write()` immediately, which calls the driver-specific `write(cx)`. So
a writable point does not necessarily write on every value change — `minWriteTime` throttles the wire (a
tuning-policy fact relevant to both performance and the KC13 safety picture: a fast-changing command is
coalesced).

## 544.4 Force-write on an override action (levels 1/8) [CERT]

An emergency/manual action writes NOW, not on next change. `WritableSupport.emergencyOverride`/`override`
call `notifyProxyExtForActionInvoked()` → `point.getProxyExt().writablePointActionInvoked()`
`[CERT] WritableSupport.java` → `BProxyExt.writablePointActionInvoked()` sets `forceWrite(true)`
`[CERT] BProxyExt.java:390-391`. That flag makes `onExecute` emit a write even when the computed value is
unchanged. Modbus goes further — `BModbusProxyExt.writablePointActionInvoked()` also calls
`getParentPoint().execute()` to force the point to re-run immediately `[CERT] BModbusProxyExt.java`. This is the
mechanism behind [Block 6] §6.3.6's "level 1/8 force an immediate device write."

## 544.5 THE PUNCHLINE — the 16-level array survives to BACnet, but COLLAPSES for Modbus [CERT]

**BACnet preserves the priority end-to-end.** `BBacnetProxyExt.write()` `[CERT] BBacnetProxyExt.java:528-545`:
```java
int writeLevel = 0;
if (getActiveLevel() != 17 || !getWriteValue().getStatus().isNull()) {
    if (isPrioritizedPresentValue()) writeLevel = this.getActiveLevel();   // :533  N4 level → BACnet priority
    ...
}
this.network().postWrite(new PointCmd(0x20000000, this, writeValue, this.lastWriteLevel, writeLevel));  // :545
```
`getActiveLevel()` reads the `activeLevel` status facet that `WritableSupport.onExecute` stamped on `out`
(B536). `PointCmd` (WRITE_POINT) sends a BACnet `WriteProperty` with that level as the **Priority parameter**
(1–16), so **N4 priority level 2 writes into the remote BACnet object's priority-array slot 2**. When the
winning level changes, `lastWriteLevel`/`clr` sends a `null` (relinquish) at the OLD level. A level-1 emergency
override thus writes at BACnet priority 1, superseding lower entries in the DEVICE's own priority array.
`readMetaData` reads back the device priority array (property 87) into a `"bac"` facet so N4 reflects the real
remote active level. **The N4 and BACnet priority arrays are the same 16-slot BACnet model, wired 1:1.**

**Modbus has NO priority concept.** `BModbusClientNumericProxyExt.updateOutput()` writes the raw register —
value → bytes (`setIntegerByteArray`/float converters), FC6 (preset single) or FC16 (preset multiple), to the
`dataAddress` register `[CERT] BModbusClientNumericProxyExt.java`. The 16-level arbitration is fully resolved
to ONE value BEFORE the wire; the register holds only that resolved value. So the priority array is an N4-side
construct for Modbus — the device sees a single number. (Same for any non-BACnet register/coil driver.)

This is the key architectural fact: **the 16-level priority model is native to BACnet and passes through
transparently; for every other driver it is an N4-internal arbitration whose OUTPUT is a single value.**

## 544.6 Write feedback → out status [CERT]

`BProxyExt.writeOk(writeValue)` `[CERT] BProxyExt.java:445-456` clears `writeFault`, stamps write ticks, and
`updateStatus()`; `writeFail(cause)` sets the fault string and ORs the fault bit into status
`[CERT] :466-471,225-241`, then `executePoint()` re-runs so `out` reflects the new status. BACnet additionally
`pollNow()`s an immediate ReadProperty after a write (`PointCmd`) to confirm the device value into `readValue`
→ `out`. So a failed write surfaces as a fault on the point's `out` (relevant to KC13: a write failure IS
visible on the point, unlike a plausible-but-wrong sensor read).

## 544.7 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Link out→InN via propagatePropertyToProperty; fwChanged→execute re-arbitrates | [CERT] | BLink.java; BControlPoint.java:343-362 | sweep-cited |
| 2 | proxyExt write trigger: isWrite && (changed \|\| forceWrite) → writeDesired | [CERT] | BProxyExt.java:363-366 | token-checked ✓ |
| 3 | Tuning.writeDesired defers if minWriteTime>0, else write() immediately | [CERT] | Tuning.java | sweep-cited |
| 4 | Action force-write: writablePointActionInvoked→forceWrite(true); Modbus also execute() | [CERT] | BProxyExt.java:390-391; BModbusProxyExt | token-checked ✓ |
| 5 | BACnet: getActiveLevel→writeLevel→PointCmd WriteProperty Priority (N4 level→BACnet priority) | [CERT] | BBacnetProxyExt.java:528-545 | token-checked ✓ |
| 6 | BACnet relinquish (null) at old level when active level changes | [CERT] | BBacnetProxyExt/PointCmd (lastWriteLevel/clr) | sweep-cited |
| 7 | Modbus collapses to a single register value (FC6/FC16), no priority | [CERT] | BModbusClientNumericProxyExt.java | token-checked (path) ✓ |
| 8 | writeOk clears fault; writeFail sets fault bit → out status; BACnet pollNow re-reads | [CERT] | BProxyExt.java:445-471,225-241 | sweep-cited |

**Marker tally**: [CERT] ×8 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 4 of 8 rows token-verified
inline (the proxyExt trigger, the force-write flag, the BACnet priority mapping, the Modbus path). Note: several
citations are procyon/decompiled paths where vineflower was absent (Modbus write body); line numbers pinned to
the tree read.

## Connections

- **[Block 536]** (KC1) — the arbitration whose `out` + `activeLevel` facet this path transports; §544.5 shows
  where the facet is consumed.
- **[Block 46]** — priority-array writes from an external SPA (the write ORIGIN; this is the write DESTINATION).
- **[Block 6]** §6.3.6 — "level 1/8 force immediate write" — §544.4 is the code mechanism.
- **`modbus` focus (B294–B315)** — the Modbus driver whose write path §544.5 samples (no priority).
- **BACnet blocks** — the priority-array-native driver; §544.5 is the end-to-end priority proof.
- **[Block 543]** (KC13) — `minWriteTime` coalescing and write-fault visibility are safety-relevant.

## Open gaps (this block)

- The BACnet Commandable-object relinquish-default interaction (does N4 write the device's Relinquish_Default?)
  is named, not traced — belongs to a BACnet-writable child gap, not kitControl.
- Modbus write FC selection (FC6 vs FC16) exact branch cited from procyon (vineflower absent); low risk.
