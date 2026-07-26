# Block 274 — Gap-closing sweep over B271/B272/B273: a confirmed COV-Property duplication defect, the COVP poller rate, the Active_COV_Subscriptions read path, COV-driven trend logging, and the three non-Numeric conversion bodies

> **TYPE: GAP-CLOSING SWEEP.** Not a new subject — this block resolves six open gaps left by [B271],
> [B272] and [B273], each of which was small enough not to justify its own block but large enough to matter.
> The headline result is **B273-G2**: a suspicion I raised in B273 §273.4 and explicitly refused to assert.
> It is now **confirmed as a real defect**, with the two halves of the evidence in hand.
>
> **Sources**: original Tridium source (`docSource/…/bacnet-rt/javax/baja/bacnet/**`) + Vineflower decompile
> for `com.tridium.bacnet.stack.server.*` and `com.tridium.bacnet.history.*`.
>
> Markers: `[CERT]` verbatim · `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 274.0 — Gaps addressed

| Gap | From | Result |
|---|---|---|
| **B273-G2** | B273 §273.4 | **CLOSED — defect CONFIRMED**, §274.1 |
| **B273-G1** | B273 §273.6 | CLOSED — §274.2 |
| **B273-G3** | B273 §273.5 | CLOSED — §274.3 |
| **B272-G3** | B272 §272.8 | CLOSED — §274.4 |
| **B272-G2** | B272 §272.x | CLOSED — §274.5, **plus counter-evidence to B272 §272.7**, §274.6 |
| **B271-G2** | B271 §271.6 | CLOSED — §274.7 |

Still open and deliberately not attempted here (each deserves its own block): **B271-G1** (the 42 export
descriptors as a family), **P3-mstp** and **P3-sc** (from B133), and every `requires-execution` item.

---

## 274.1 — B273-G2 CLOSED: COV-Property subscriptions with an array index duplicate instead of renewing

In B273 §273.4 I flagged that `findCovPropertySubscription` discards its own `propertyArrayIndex` argument,
and wrote: *"Flagged as a suspected defect, **not confirmed** — it needs either a careful read of the
`BBacnetCovSubscription` constructor's index handling or a live test."*

I read the constructor. **The defect is real.** Both halves, verbatim:

**Half 1 — the constructor stores the REAL index** `[CERT]`
(`BBacnetCovSubscription`, the SubscribeCOVProperty constructor):

```java
getMonitoredPropertyReference().setObjectId(monitoredObjectId);
getMonitoredPropertyReference().setPropertyId(monitoredPropertyId.getPropertyId());
getMonitoredPropertyReference().setPropertyArrayIndex(monitoredPropertyId.getPropertyArrayIndex());
```

**Half 2 — the lookup hardcodes `NOT_USED`** `[CERT]` (`BBacnetPointDescriptor`):

```java
public final BBacnetCovSubscription findCovPropertySubscription(BBacnetAddress subscriberAddress,
                                                                long processId,
                                                                BBacnetObjectIdentifier objectId,
                                                                int propertyId,
                                                                int propertyArrayIndex)   // ← never used
{
  return findSubscription(true, subscriberAddress, processId, objectId, propertyId, NOT_USED);
}
```

**Half 3 — the comparison is exact equality** `[CERT]` (`findSubscription`):

```java
(sub.getMonitoredPropertyReference().getPropertyArrayIndex() == propertyArrayIndex)
```

**Half 4 — `NOT_USED = -1`** `[CERT]` (`BacnetConst:22`), and it is also the default:
`propertyArrayIndex = newProperty(0, NOT_USED, null)` in `BBacnetPropertyReference` `[CERT]`.

### The consequence

| Subscription | Stored index | Lookup compares | Found? |
|---|---|---|---|
| COVP **without** array index | `-1` (default) | `-1 == -1` | **yes** — works correctly |
| COVP **with** array index, e.g. `3` | `3` | `3 == -1` | **no** — never found |

So for any COV-Property subscription that names a concrete array index, in `CovHandler`:

```java
BBacnetCovSubscription sub = point.findCovPropertySubscription(...);   // always null
if (request.isCancellation()) {
   if (sub != null) point.removeCovSubscription(sub);                  // ← never runs
   return new BacnetSimpleAck(28);                                     // ← acks anyway
} else {
   if (sub != null) { ...renew... }
   else { sub = new BBacnetCovSubscription(...); ...; point.addCovSubscription(sub); }   // ← always this
}
```

Two distinct failures, both confirmed by composition `[CERT]` on each fragment, `[INFER]` on the composed
outcome:

1. **Renewal creates a duplicate.** Every resubscribe adds another `covSubscription?` slot. A well-behaved
   client renews at roughly half its lifetime — so it renews faster than the old subscriptions expire, and
   the table **grows monotonically**. Recall from B273 §273.3 that there is **no cap on subscription
   count** on the server side. The only brake is each duplicate's own `Clock` ticket eventually firing.
2. **Cancellation silently fails.** `isCancellation()` finds nothing to remove, but still returns
   `SimpleAck(28)`. The client believes it unsubscribed. The subscription keeps running until its
   lifetime expires — and if the client was renewing, several of them are.

Severity assessment, stated plainly: this only triggers for **COV-Property subscriptions that specify an
array index**, which is a narrow case — most COVP use is on scalar properties where the index is absent and
the `-1 == -1` path works. It is not a general COV defect. But where it does apply, both the duplication
and the un-cancellable subscription are real, and neither produces an error the client can see.

Not verified: whether any shipped Niagara code path ever *issues* such a subscription against another
station (the client side sends `new BBacnetPropertyReference(propertyId, propertyArrayIndex)` with a real
index `[CERT]` B272 §272.7, so Niagara-to-Niagara COVP on an array element would hit this). Confirming the
end-to-end trigger needs a live test — logged as **B274-G1**.

---

## 274.2 — B273-G1 CLOSED: the COV-Property poller

`com.tridium.bacnet.stack.server.LocalBacnetCovPropPoll extends LocalBacnetPoll`, 44 lines total `[CERT]`:

```java
@Override protected BRelTime getPollRate()   { return this.local.getCovPropertyPollRate(); }
@Override protected String  getThreadName()  { return "Local Bacnet COVProperty Poll"; }
@Override protected Type    getPolledType()  { return BBacnetCovSubscription.TYPE; }
@Override protected boolean poll(BObject o) throws Exception {
   BBacnetCovSubscription sub = (BBacnetCovSubscription)o;
   BIBacnetCovSource covSrc = (BIBacnetCovSource)sub.getParent();
   if (covSrc == null) return false;
   covSrc.checkCov();
   return true;
}
```

And the rate `[CERT]` (`BLocalBacnetDevice`):

```java
public static final Property covPropertyPollRate = newProperty(Flags.HIDDEN, BRelTime.makeSeconds(5), null);
```

Findings:

- **Default rate: 5 seconds.** So a COV-Property subscriber on a Niagara station sees at worst ~5 s of
  latency, versus event-latency for plain COV (B273 §273.6).
- The slot is **`Flags.HIDDEN`** — it exists and is settable programmatically, but does **not** appear in
  the normal property sheet. If you need faster COVP on a station, this is the knob, and you will not find
  it by browsing.
- It runs on its **own named thread** — `"Local Bacnet COVProperty Poll"` — separate from the client-side
  `BBacnetPoll` buckets of B271 §271.9. Useful when reading a thread dump.
- The unit of polling is the **subscription**, not the point (`getPolledType() = BBacnetCovSubscription.TYPE`).
  Ten COVP subscriptions on one object cost ten `checkCov()` calls per tick, each doing the full
  `readProperty` + `PropertyInfo` + `asnToValue` decode described in B272 §272.9. `[INFER]` on the cost,
  `[CERT]` on the polled type.
- A subscription whose parent has gone away returns `false` rather than throwing.

---

## 274.3 — B273-G3 CLOSED: `Active_COV_Subscriptions` read path

Property **152** `[CERT]` (`BBacnetPropertyIdentifier.ACTIVE_COV_SUBSCRIPTIONS = 152`), declared as an
**optional** property of the device object `[CERT]`:

```java
protected void addOptionalProps(Vector<BBacnetPropertyIdentifier> v)
{
  v.add(BBacnetPropertyIdentifier.activeCovSubscriptions);
  v.add(BBacnetPropertyIdentifier.restartNotificationRecipients);
  ...
}
```

The storage is a list of **ORDs**, not of subscriptions (B273 §273.5). They are resolved **at read time**
`[CERT]`:

```java
case BBacnetPropertyIdentifier.ACTIVE_COV_SUBSCRIPTIONS:
  BOrd[] covOrdList = getActiveCovSubscriptions().getChildren(BOrd.class);
  BBacnetCovSubscription[] covList = new BBacnetCovSubscription[covOrdList.length];
  int j = 0;
  try {
    for (j = 0; j < covOrdList.length; j++) covList[j] = (BBacnetCovSubscription)covOrdList[j].get(this);
    return readRange(rangeReference, covList, BBacnetCovSubscription.MAX_ENCODED_SIZE);
  } catch (Exception e) {
    log.warning("Exception building Active_COV_Subscriptions[" + j + "] for ReadRange: " + e);
    return new ReadRangeAck(BBacnetErrorClass.DEVICE, BBacnetErrorCode.OPERATIONAL_PROBLEM);
  }
```

Three things:

1. **It is ReadRange-able.** Only five device properties are `[CERT]`:

```java
if (!(propertyId == DEVICE_ADDRESS_BINDING || propertyId == ACTIVE_COV_SUBSCRIPTIONS ||
      propertyId == RESTART_NOTIFICATION_RECIPIENTS || propertyId == TIME_SYNCHRONIZATION_RECIPIENTS ||
      propertyId == UTC_TIME_SYNCHRONIZATION_RECIPIENTS))
  return new ReadRangeAck(services, propertyIsNotA_List);
```

   Everything else returns `propertyIsNotA_List`. This matters if you are writing a client: on a busy
   station this list can be long, and **ReadRange is the supported way to page it** rather than pulling the
   whole thing in one segmented ReadProperty.

2. **A ReadRange with an array index is refused** — `if (rangeReference.getPropertyArrayIndex() != NOT_USED)
   return new ReadRangeAck(property, propertyIsNotAnArray)` `[CERT]`.

3. **A single dangling ORD fails the whole read.** One unresolvable entry throws inside the loop and the
   entire request comes back `(DEVICE, OPERATIONAL_PROBLEM)`. The `j` in the log line tells you which index
   broke. Given that the subscription slots are `TRANSIENT` and removed asynchronously by tickets
   (B273 §273.7-273.8), a race between removal and read is plausible `[INFER]`.

---

## 274.4 — B272-G3 CLOSED: `updateStatusOnCov`

It is a slot on **`BBacnetServerLayer`**, not on the device or the network `[CERT]`:

```java
public static final Property updateStatusOnCov = newProperty(4, false, null);   // Flags.SUMMARY
```

**Default `false`.** Recall its only use, from B272 §272.8:

```java
if (serverLayer.getUpdateStatusOnCov() && !device.isDown()) device.pingOk();
```

So by default an inbound COV notification does **not** count as a ping. The claim in B272 §272.8 —
*"That is why a device on pure COV can stay 'up' without any polling traffic"* — was stated without noting
that the behaviour is **off unless enabled**. Correction recorded in §274.8.

Practical consequence: a device that is genuinely healthy and pushing COV, but never polled (because all
its points are COV-subscribed), can still be marked **down** by the ping timer unless `updateStatusOnCov`
is turned on. It is a single station-wide flag on the server layer, one place to set it.

---

## 274.5 — B272-G2 CLOSED: COV-driven trend logging

Four parallel classes, 217 lines each, in `com.tridium.bacnet.history` `[CERT]`:

`BBacnetNumericCovTrendLogExt` · `BBacnetBooleanCovTrendLogExt` · `BBacnetEnumCovTrendLogExt` ·
`BBacnetStringCovTrendLogExt`

Shape `[CERT]`:

```java
public class BBacnetNumericCovTrendLogExt extends BNumericCovHistoryExt implements BIBacnetTrendLogExt
```

That single line is the whole design: they extend Niagara's **own COV history extensions**
(`B*CovHistoryExt` — the standard "log on change" history ext) and add the `BIBacnetTrendLogExt` interface
so the resulting history is exportable as a BACnet **TrendLog object**. They add one property,
`totalRecordCount`, and two actions, `startLogging` / `stopLogging` `[CERT]`.

All record writing funnels through a shared utility `[CERT]`:

```java
BacnetTrendLogUtil.writeEvent(this, timestamp, status, sequenceNumber, event);
...
protected void writeRecord(BAbsTime timestamp, BStatusValue out) throws IOException {
   BacnetTrendLogUtil.writeRecord(this, timestamp, out);
}
```

So: a Niagara COV history and a BACnet TrendLog are **the same store**, with `BacnetTrendLogUtil` doing
the BACnet-record encoding on top of Niagara's history engine. There is no separate BACnet trend database.

`BBacnetClientCov` — which B272 flagged as unexplored — is the datatype for the increment on the *client*
side of trend acquisition. Its javadoc `[CERT]`: *"BBacnetClientCov represents the choice for the COV
increment to be used in acquiring data for a trend log via COV"*, with the semantics *"if null, then the
default-increment choice is used. if non-null, then the real-increment choice is used"* — i.e. it models
the BACnet CHOICE between `defaultIncrement` and `realIncrement`, using a null `BStatusNumeric` as the
discriminator.

---

## 274.6 — Counter-evidence: the hardcoded process id is NOT universal

B272 §272.7 reported that the subscriber process identifier is hardcoded to `1`. That remains **true for
`BBacnetDevice`'s four point-proxy call sites** `[CERT]`. But it is not the whole picture, and the block
implied more generality than the evidence supported.

`BBacnetTrendLogRemoteExt` — the client-side remote trend log — does the opposite `[CERT]`:

```java
long processId = this.fetchProcessId();
SubscribeCovRequest request = new SubscribeCovRequest(processId, this.getObjectId(), true,
                                                      this.getCovResubscriptionInterval() * 2);
...
private long fetchProcessId() {
   long processId = 0L;
   if (this.device != null) processId = this.device.getAlarms().getNiagaraProcessId();
   return processId;
}
```

So Niagara **does** have a real, configurable process id — `getNiagaraProcessId()`, living on the device's
**alarms** extension — and the remote-trend-log path uses it. Only the point-proxy path ignores it in
favour of the literal `1`.

Bonus cross-confirmation of B272 §272.2: here the lifetime is `covResubscriptionInterval * 2`. That is the
**inverse** of the ProxyExt arithmetic (`resubscribe = lifetime / 2`) and therefore consistent — two
independent code paths agreeing that the resubscribe interval is **half** the lifetime. That closes any
remaining doubt about the direction of the factor.

---

## 274.7 — B271-G2 CLOSED: the three non-Numeric conversion bodies

B271 §271.6 traced `BBacnetNumericProxyExt.fromEncodedValue()` line by line and left the other three at
signature depth. All three share the same skeleton (status first, `peekApplicationTag`, auto-fill
`dataType`, switch on tag) and differ only in coercion. `[CERT]`

### Boolean — coerce by "is it non-zero"

```java
case ASN_BOOLEAN:          dv.setValue(asnIn.readBoolean());
case ASN_UNSIGNED:         dv.setValue(asnIn.readUnsignedInteger() != 0);
case ASN_INTEGER:          dv.setValue(asnIn.readSignedInteger() != 0);
case ASN_REAL:             dv.setValue(!BFloat.equals(asnIn.readReal(), 0.0F));
case ASN_DOUBLE:           dv.setValue(!BDouble.equals(asnIn.readDouble(), 0.0));
case ASN_OCTET_STRING:     byte[] b = asnIn.readOctetString();
                           dv.setValue((b.length > 0) && (b[0] != 0));
case ASN_CHARACTER_STRING: String cs = asnIn.readCharacterString();
                           dv.setValue(cs.equals(getParentPoint().getFacets()
                                                 .getFacet(BFacets.TRUE_TEXT).toString()));
```

The CharacterString case is the elegant one: it compares the received string against the point's
**`trueText` facet** — which B271 §271.8 showed is derived from BACnet property **4 (`activeText`)**. So a
device that reports its binary state as text round-trips correctly *provided the facet was discovered*. If
`trueText` is absent, `getFacet(...)` returns null and `.toString()` throws → caught by the enclosing
handler → `readFail`. `[INFER]` on the null path; the code is `[CERT]`.

### Enum — validate against the range, do not coerce

```java
BEnum ms = ((BEnumPoint)getParentPoint()).getEnum();
BEnumRange msr = (BEnumRange)((BEnumPoint)getParentPoint()).getEnumFacets().getFacet(BFacets.RANGE);
if (msr == null) msr = ms.getRange();          // fallback to the enum's own range
...
case ASN_BOOLEAN:          dv.setValue(msr.get(asnIn.readBoolean() ? 1 : 0));
case ASN_UNSIGNED:         dv.setValue(msr.get(asnIn.readUnsignedInt()));
case ASN_INTEGER:          dv.setValue(msr.get(asnIn.readSignedInteger()));
case ASN_REAL:             dv.setValue(msr.get((int)asnIn.readReal()));      // truncates
case ASN_DOUBLE:           dv.setValue(msr.get((int)asnIn.readDouble()));    // truncates
case ASN_OCTET_STRING:     dv.setValue(msr.get(asnIn.readOctetString()[0]));
case ASN_ENUMERATED:       dv.setValue(msr.get(asnIn.readEnumerated()));
```

**This is the important structural difference from Numeric.** Every path goes through `msr.get(ordinal)`
— a lookup into the `BEnumRange` that B271 §271.8 showed is built from BACnet property **110 (`stateText`)**
with 1-based ordinals. An ordinal outside the range fails the lookup rather than producing a bogus value.

Where Numeric silently coerces anything into a plausible double (B271 §271.6, gotcha #1), **Enum
validates**. That asymmetry is worth carrying: an enum point with a wrong or missing `stateText` range will
*fault*, which is far better diagnostics than a numeric point quietly reporting nonsense.

The CharacterString case carries a dated author comment `[CERT]`:

```java
case ASN_CHARACTER_STRING:
  String cs = asnIn.readCharacterString();
  if (msr.isTag(cs)) dv.setValue(ms.getRange().get(msr.tagToOrdinal(cs)));
// 2006-12-07 CPG This was a long shot anyway, and it causes problems if the value isn't a number,
//                so remove it.
//                else dv.setValue(ms.getRange().get(Integer.parseInt(cs)));
```

A string that is not a known tag leaves the value **unchanged** — no fault, no update. (CPG = Craig
Gemmill, the author on every COV/point class in this subsystem since 2002.)

### String

Structurally the simplest; it takes `readCharacterString()` directly and stringifies the other primitives.
Not reproduced — no decision logic worth citing.

---

## 274.8 — Corrections to earlier blocks

| Target | Was | Is |
|---|---|---|
| **B273 §273.4** | "suspected defect, **not confirmed**" | **CONFIRMED** — constructor stores the real index, lookup passes `NOT_USED = -1`, comparison is `==`. §274.1 |
| **B272 §272.8** | "*That is why a device on pure COV can stay 'up' without any polling traffic*" | Only when **`updateStatusOnCov = true`**; the slot defaults to **`false`**. §274.4 |
| **B272 §272.7** | "the subscriber process identifier is hardcoded to `1`" | True for the **four point-proxy call sites**. Niagara does have a real `getNiagaraProcessId()`, used by `BBacnetTrendLogRemoteExt`. §274.6 |
| **B271 §271.6** | Numeric coercion documented; others at signature depth | Boolean coerces by non-zero (and by `trueText` for strings); **Enum validates against `BEnumRange` instead of coercing**. §274.7 |

---

## 274.9 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| COVP constructor stores real array index | `setPropertyArrayIndex(monitoredPropertyId.getPropertyArrayIndex())` | `[CERT]` |
| COVP lookup passes NOT_USED | `findSubscription(true, …, propertyId, NOT_USED)` | `[CERT]` |
| `NOT_USED = -1` and is the slot default | `BacnetConst:22`; `newProperty(0, NOT_USED, null)` | `[CERT]` |
| ⇒ indexed COVP duplicates / cannot be cancelled | composition of the four fragments above | `[INFER]` |
| COVP poll rate 5 s, HIDDEN | `newProperty(Flags.HIDDEN, BRelTime.makeSeconds(5), null)` | `[CERT]` |
| COVP poller has its own thread | `getThreadName() → "Local Bacnet COVProperty Poll"` | `[CERT]` |
| Poller unit is the subscription | `getPolledType() → BBacnetCovSubscription.TYPE` | `[CERT]` |
| Active_COV_Subscriptions = 152, optional | `ACTIVE_COV_SUBSCRIPTIONS = 152`; `addOptionalProps` | `[CERT]` |
| Only 5 properties are ReadRange-able | the negated 5-way `if` returning `propertyIsNotA_List` | `[CERT]` |
| One bad ORD fails the whole read | single `try` around the resolve loop → `(DEVICE, OPERATIONAL_PROBLEM)` | `[CERT]` |
| `updateStatusOnCov` default false | `newProperty(4, false, null)` on `BBacnetServerLayer` | `[CERT]` |
| COV trend logs extend Niagara CovHistoryExt | `extends BNumericCovHistoryExt implements BIBacnetTrendLogExt` | `[CERT]` |
| A real process id exists and is used elsewhere | `fetchProcessId() → device.getAlarms().getNiagaraProcessId()` | `[CERT]` |
| lifetime = resubInterval × 2 (inverse confirmation) | `new SubscribeCovRequest(processId, objectId, true, getCovResubscriptionInterval() * 2)` | `[CERT]` |
| Enum validates via `msr.get()`; Numeric coerces | both switches read side by side | `[CERT]` |
| Boolean string case uses the `trueText` facet | `cs.equals(getFacets().getFacet(BFacets.TRUE_TEXT).toString())` | `[CERT]` |

Tally: **[CERT] 15 / [INFER] 1.**

---

## 274.x — Connections and remaining gaps

- **B271** — G2 closed here (§274.7). §271.6's coercion asymmetry now complete.
- **B272** — G2 and G3 closed (§274.5, §274.4); two corrections recorded (§274.8).
- **B273** — G1, G2, G3 closed (§274.2, §274.1, §274.3). **All of B273's static gaps are now closed.**

### Open

| ID | Gap | Class |
|---|---|---|
| **B271-G1** | The 42 `javax.baja.bacnet.export.B*Descriptor` classes as a family — the full export/server object model. Its COV surface is now covered by B272/B273/B274; the rest (property read/write dispatch, `BacnetWritableDescriptor`, `BOutOfServiceExt`, `BReliabilityAlarmSourceExt`, the schedule/calendar/file descriptors) is untouched. **This is the largest remaining static gap in the BACnet subsystem.** | STATIC-investigable |
| **P3-mstp** | MS/TP data-link framing (preamble `0x55 0xFF`, frame types, header/data CRC, token passing). Open since B133. | STATIC-investigable |
| **P3-sc** | BACnet/SC websocket transport (`stack.link.sc.*`). Open since B133. | STATIC-investigable |
| **B274-G1** (new) | Whether any shipped path actually issues an array-indexed SubscribeCOVProperty, i.e. whether §274.1's defect is reachable in practice or only by a third-party client. | requires-execution |
| **B274-G2** (new) | `LocalBacnetPoll` — the shared base class of the COVP poller; its scheduling model was not read. | STATIC-investigable |
| — | All prior `requires-execution` items (B271-G4, B272-G4, B273-G4, P3-dyn). | requires-execution |
