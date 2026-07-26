# Block 273 — BACnet COV server-side subscription table: admission, validation, deduplication, the 8-hour cap, the termination ticket, and clock-shift compensation

> Closes **B272-G1**. [B272] traced the server-side COV *send* path (`BacnetCovSubscriber` → `checkCov` →
> `Cov` → `postAsync`) but stopped at the point where a subscription already exists. This block documents
> **how it got there**: what happens when a remote BACnet client sends `SubscribeCOV` to a Niagara station,
> how the request is validated and rejected, how duplicates are folded, where the subscription is stored,
> and the three independent mechanisms that can remove it.
>
> **Sources**: Vineflower decompile of `com.tridium.bacnet.stack.CovHandler` (the only place the inbound
> request is handled) + original Tridium source for `BBacnetPointDescriptor`, `BLocalBacnetDevice`,
> `BBacnetCovSubscription`, `BBacnetErrorCode`, `BBacnetErrorClass`.
>
> Markers: `[CERT]` verbatim · `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 273.1 — The inbound path `[CERT]`

`CovHandler` is the single entry point. Its dispatch:

```java
case 5:  return this.processSubscribeCovRequest((SubscribeCovRequest)request, sourceAddress);
case 28: return this.processSubscribeCovPropertyRequest((SubscribeCovPropertyRequest)request, sourceAddress);
...
default: return new BacnetReject(9);
```

Confirmed service choices: **`SubscribeCOV = 5`**, **`SubscribeCOVProperty = 28`**. (Careful: in
`BBacnetServicesSupported` — the *services-supported bitstring* — the same two services sit at bits **5**
and **38** `[CERT]`. Same trap as Who-Is in B271 §271.2: choice ≠ bit position.)

The full flow for a `SubscribeCOV`:

```
SubscribeCovRequest(processId, objectId, issueConfirmed, lifetime)
   │
   ├─ local.lookupBacnetObject(objectId)          → the export descriptor, or null
   ├─ covRequestValidation(export, objectId, lifetime, 5)   → Error/null   ......... §273.2
   │
   ├─ point.findCovSubscription(subscriberAddress, processId, objectId)  ........... §273.4
   │
   ├─ request.isCancellation()?
   │     └─ YES → if (sub != null) removeCovSubscription(sub);  → SimpleAck(5)
   │
   └─ NO ─┬─ sub EXISTS  → setIssueConfirmedNotifications(...); startCovTimer(sub, lifetime)
          └─ sub IS NEW  → new BBacnetCovSubscription(...)
                           setLastValue(point.getObject().getOutStatusValue())   ← baseline seeded
                           startCovTimer(sub, lifetime)
                           addCovSubscription(sub)
                                                                → SimpleAck(5)
```

Two design points worth naming:

- **Renewal and first-subscribe are the same request.** A resubscribe with the same (address, processId,
  objectId) folds onto the existing subscription and just restarts the timer. No duplicate is created.
- **The baseline is seeded from the point's current value at subscribe time** (`setLastValue(getOutStatusValue())`),
  *before* the timer starts. So the first `checkCov()` after subscription compares against the value the
  subscriber implicitly already has — via the notification that `startCovTimer` sends immediately (§273.6).

---

## 273.2 — Validation: four gates, four distinct BACnet errors `[CERT]`

```java
private BacnetServicePrimitive covRequestValidation(BIBacnetExportObject export,
                                                    BBacnetObjectIdentifier objectId,
                                                    long lifetime, int requestType)
{
   if (export == null)                        return new SimpleError(requestType, new NErrorType(1, 31));
   else if (export.getObject() == null)       return new SimpleError(requestType, new NErrorType(1, 1000));
   else if (lifetime > 28800L) {
      logger.info("Lifetime is more that that allowed value of 28800 seconds :: " + lifetime);
                                              return new SimpleError(requestType, new NErrorType(5, 37));
   }
   else if (!(export instanceof BIBacnetCovSource)) {
      logger.info("Attempt to subscribe-" + (requestType == 28 ? "Cov" : "CovP") + " for non-Cov object " + objectId);
                                              return new SimpleError(requestType, new NErrorType(1, 45));
   }
   else                                       return null;
}
```

Decoded against `BBacnetErrorClass` / `BBacnetErrorCode` `[CERT]`:

| Condition | Error class | Error code | Meaning |
|---|---|---|---|
| Object not exported | 1 `OBJECT` | 31 `UNKNOWN_OBJECT` | the object-id maps to nothing in this station |
| Export exists but its ORD resolves to nothing | 1 `OBJECT` | **1000 `TARGET_NOT_CONFIGURED`** | a dangling export descriptor — **proprietary Tridium code, outside the ASHRAE range** |
| `lifetime > 28800` | 5 `SERVICES` | 37 `VALUE_OUT_OF_RANGE` | see §273.3 |
| Export is not a `BIBacnetCovSource` | 1 `OBJECT` | 45 `OPTIONAL_FUNCTIONALITY_NOT_SUPPORTED` | e.g. a File or Calendar export |

`TARGET_NOT_CONFIGURED = 1000` is worth flagging: it is a **Tridium-proprietary error code**, not a standard
one. A third-party client will not have a string for it and will typically render it as a raw number.
If you are writing your own BACnet client against a Niagara station, `(class 1, code 1000)` on a
SubscribeCOV means *"the export descriptor exists but its `object` ORD is broken"* — a station
configuration fault, not a protocol error on your side.

### COV-Property adds two more gates `[CERT]`

```java
private BacnetServicePrimitive covpRequestValidation(export, objectId, lifetime, propId) {
   validationError = covRequestValidation(export, objectId, lifetime, 28);
   if (validationError != null) return validationError;
   switch (propId) {
      case 28: case 75: case 77: case 79: case 371:
         return new SimpleError(28, new NErrorType(2, 44));      // PROPERTY / NOT_COV_PROPERTY
      default:
         boolean presentInPropertyList = Arrays.stream(export.getPropertyList()).anyMatch(i -> i == propId);
         return !presentInPropertyList ? new SimpleError(28, new NErrorType(2, 32)) : null;   // UNKNOWN_PROPERTY
   }
}
```

**Five properties are explicitly blacklisted for COV-Property**: `description` (28),
`objectIdentifier` (75), `objectName` (77), `objectType` (79), `propertyList` (371) — all static metadata.
Everything else must appear in the export's own `getPropertyList()`, or you get `UNKNOWN_PROPERTY`.

This is a real answer to a practical question: **you cannot subscribe COV-Property to an object's name or
description on a Niagara station.** The rejection is deliberate and typed.

---

## 273.3 — The cap: 8 hours, hardcoded `[CERT]`

```java
else if (lifetime > 28800L) {
   logger.info("Lifetime is more that that allowed value of 28800 seconds :: " + lifetime);
   return new SimpleError(requestType, new NErrorType(5, 37));
}
```

**28 800 seconds = 8 hours.** It is a literal in `CovHandler`, not a slot — there is no station property to
raise it. `[CERT]`

Consequences for anyone writing a client against a Niagara station:

- A `SubscribeCOV` asking for more than 8 hours is **rejected outright** with `VALUE_OUT_OF_RANGE` — the
  subscription is not created, not clamped. Ask for 8 hours or less.
- `lifetime = 0` (indefinite) **passes** — the check is `> 28800`, and zero is not greater. So the
  unbounded case is allowed while the merely-long case is refused. That asymmetry is intentional per
  ASHRAE (0 has a distinct meaning) but it does read backwards at first glance.
- Note the contrast with the **client** side (B272 §272.3): as a client, Niagara sends whatever its tuning
  policy says, floored at 300 s, with no upper bound. The 8-hour ceiling is a **server-only** rule.

There is **no cap on the NUMBER of subscriptions** anywhere in this path `[CERT]` — no counter, no maximum,
no admission control by count. `BLocalBacnetDevice.activeCovSubscriptions` is a `BBacnetListOf` that simply
grows. Contrast with the client side, where `maxCovSubscriptions` exists per device (B272 §272.4). As a
server, Niagara will accept subscriptions until it runs out of memory. `[INFER]` on the consequence; the
absence of a cap is `[CERT]`.

---

## 273.4 — Deduplication: a six-field match `[CERT]`

`BBacnetPointDescriptor.findSubscription()` walks the descriptor's own child slots:

```java
SlotCursor<Property> c = getProperties();
while (c.next(BBacnetCovSubscription.class))
{
  BBacnetCovSubscription sub = (BBacnetCovSubscription)c.get();
  if (sub.isCovProperty() == covProperty &&
      (sub.getRecipient().getRecipient().getAddress()
          .equals(subscriberAddress.getNetworkNumber(), subscriberAddress.getMacAddress().getBytes())) &&
      (sub.getRecipient().getProcessIdentifier().getUnsigned() == processId) &&
      (getObjectId().equals(objectId)) &&
      (sub.getMonitoredPropertyReference().getPropertyId() == propertyId) &&
      (sub.getMonitoredPropertyReference().getPropertyArrayIndex() == propertyArrayIndex))
    return sub;
}
return null;
```

The identity of a subscription is therefore:

**(isCovProperty, network+MAC, processId, objectId, propertyId, propertyArrayIndex)**

Notes that matter in practice:

1. **Address match is network-number + MAC bytes**, not a resolved device id. Two subscriptions from the
   same device via different routes (different network numbers) are **distinct subscriptions** and both
   will be served.
2. **COV and COV-Property never collide** — `isCovProperty` is part of the key. The same client can hold a
   plain COV on present-value *and* a COV-Property on present-value simultaneously, and receive two
   notifications per change.
3. The public `findCovSubscription()` hardcodes `PRESENT_VALUE` and `NOT_USED` as the last two arguments
   `[CERT]` — consistent with plain COV being present-value-only (B272 §272.4).
4. **`findCovPropertySubscription` passes `NOT_USED` as the array index, ignoring its own
   `propertyArrayIndex` parameter** `[CERT]`:

```java
return findSubscription(true, subscriberAddress, processId, objectId, propertyId, NOT_USED);
//                                                                                 ^^^^^^^^
//   the method's own propertyArrayIndex argument is never passed through
```

Consequence `[INFER]`: two COV-Property subscriptions from the same client to the **same property at
different array indices** collapse onto whichever one is stored with `NOT_USED`. The lookup cannot tell them
apart, so a second subscribe at a different index is treated as a renewal of the first rather than as a new
subscription. Whether this is reachable depends on `BBacnetCovSubscription` storing `NOT_USED` at creation
time — the constructor receives the full `PropertyReference` `[CERT]`, so a subscription created with a real
index would then never be found by this lookup, and a renewal would create a *second* one. Flagged as a
suspected defect, **not confirmed** — it needs either a careful read of the `BBacnetCovSubscription`
constructor's index handling or a live test with an array-indexed COV-Property subscribe.

> **CONFIRMED by [B274] §274.1.** The constructor **does** store the real index
> (`setPropertyArrayIndex(monitoredPropertyId.getPropertyArrayIndex())`), `NOT_USED = -1`, and the
> comparison is exact `==`. So an indexed COVP subscription is never found: **renewals duplicate it and
> cancellations silently fail while still returning `SimpleAck`.** Scalar COVP (index absent → `-1`) is
> unaffected. Gap **B273-G2 closed**.

---

## 273.5 — Storage: a dynamic slot, plus the standard property `[CERT]`

```java
public final void doAddCovSubscription(BBacnetCovSubscription sub)
{
  log.fine("Adding Cov subscription: " + sub + " on " + this);
  Property p = add("covSubscription?", sub, Flags.TRANSIENT | Flags.READONLY);
  BBacnetNetwork.localDevice().subscribeCov(this, getPoint(), p);
}
```

Each subscription becomes a **dynamic child slot on the export descriptor itself**, named
`covSubscription?` — the `?` is Baja's auto-numbering suffix, so they land as `covSubscription1`,
`covSubscription2`, … And crucially:

**`Flags.TRANSIENT | Flags.READONLY`** — subscriptions are **not persisted**. A station restart drops every
inbound COV subscription on the floor. Remote clients must resubscribe; nothing in the station remembers
they existed. `[CERT]`

Then `BLocalBacnetDevice.subscribeCov()` does the two-sided registration `[CERT]`:

```java
BBacnetCovSubscription cov = (BBacnetCovSubscription)((BComplex)export).get(p);
if (cov.isCovProperty()) covPropPoller.subscribe(cov);        // ← poller
else                     covSubscriber.subscribe(export, src); // ← event subscriber
BOrd covOrd = BOrd.make(((BComponent)export).getSlotPathOrd().toString() + "/" + p.getName());
Property sub = getActiveCovSubscriptions().addListElement(covOrd, null);
getActiveCovSubscriptions().setFlags(sub, Flags.READONLY);
```

So there are **two representations of the same subscription**: the child slot on the descriptor (the real
object) and an ORD pointing at it inside the local device's `activeCovSubscriptions` — which is the
BACnet-standard `Active_COV_Subscriptions` property (id 152), exposed so remote clients can read the
station's subscription table. That property is `HIDDEN | READONLY | TRANSIENT` `[CERT]`.

---

## 273.6 — COV-Property server-side is POLLED, not event-driven

This refines B272 §272.9, which described the server as event-driven without qualification. The dispatch
above is explicit `[CERT]`:

| Subscription kind | Mechanism | Class |
|---|---|---|
| Plain COV | **event-driven** — Baja `Subscriber` on `PROPERTY_CHANGED` | `BacnetCovSubscriber` |
| COV-Property | **polled** | `LocalBacnetCovPropPoll` (`private final LocalBacnetCovPropPoll covPropPoller = new LocalBacnetCovPropPoll(this)`) |

That is why `doCheckCov()`'s COV-Property branch (B272 §272.9) does a full `readProperty` + `PropertyInfo`
lookup + `asnToValue` decode on every evaluation: it runs on a poll tick, not on a change event. Plain COV
gets the cheap path (a slot comparison triggered by the actual write); COV-Property pays a decode per
sample.

Practical read: **a COV-Property subscription on a Niagara station has poll-rate latency, not
change-event latency.** If a client needs immediate notification, plain COV on present-value is
structurally faster.

`LocalBacnetCovPropPoll` itself was not opened — see gap **B273-G1**.

---

## 273.7 — The termination ticket `[CERT]`

```java
public final void startCovTimer(BBacnetCovSubscription covSub, long lifetime)
{
  Clock.Ticket ticket = covSub.getTicket();
  if (ticket != null) ticket.cancel();          // always cancel the old one first

  if (lifetime > 0)
  {
    BRelTime subLife = BRelTime.make(((int)lifetime) * BRelTime.MILLIS_IN_SECOND);
    covSub.setSubscriptionEndTime(BAbsTime.make().add(subLife));
    covSub.setTicket(Clock.schedule(this, subLife, removeCovSubscription, covSub));
  }
  sendCovNotification(covSub);                  // unconditional — fires even on renewal
}
```

Three behaviours fall out:

1. **Expiry is a scheduled `Clock` action, not a scan.** The ticket invokes `removeCovSubscription` on the
   descriptor when the lifetime elapses. There is no sweeper thread walking the table.
2. **Every subscribe — including a renewal — sends an immediate notification.** `sendCovNotification` is
   outside the `if`. That is standard-conformant (a fresh subscription must get the current value) but it
   means a client that resubscribes aggressively gets a notification per resubscribe regardless of whether
   anything changed.
3. **`lifetime = 0` cancels the old ticket and schedules nothing** → indefinite subscription. But it also
   **leaves `subscriptionEndTime` at its previous value.** For a brand-new subscription that value is
   `BAbsTime.NULL`, and `getTimeRemaining()` returns `0` for NULL (B272 §272.9), so the subscription lives
   forever — correct. For a *renewal from a finite lifetime down to 0*, the stale end-time survives, and
   once it passes, `getTimeRemaining()` returns `-1` and the next `doSendCovNotification` deletes the
   subscription:

```java
if (covSub.getTimeRemaining() < 0) { removeCovSubscription(covSub); return; }
```

So **downgrading an existing finite subscription to indefinite does not actually make it indefinite** — it
survives only until the original end time, then dies on the next notification attempt. `[INFER]` — the code
paths are `[CERT]`, the composed outcome is derived and would need a live test to confirm.

---

## 273.8 — Removal: three independent paths `[CERT]`

```java
public final void doRemoveCovSubscription(BBacnetCovSubscription sub)
{
  Clock.Ticket ticket = sub.getTicket();
  if (ticket != null) ticket.cancel();
  sub.setTicket(null);
  Property p = getProperty(sub.getName());
  if (p != null) remove(p);
  BBacnetNetwork.localDevice().unsubscribeCov(this, getPoint(), p);
}
```

Reachable from:

| Path | Trigger |
|---|---|
| **Client cancellation** | `SubscribeCOV` with no lifetime → `request.isCancellation()` → `removeCovSubscription` → `SimpleAck(5)` |
| **Timer expiry** | the `Clock.Ticket` scheduled in `startCovTimer` fires the action directly |
| **Lazy reap** | `doSendCovNotification` sees `getTimeRemaining() < 0` and removes it before sending |
| *(implicit)* | station restart — the slots are `TRANSIENT` (§273.5) |

Note **a cancellation for a subscription that does not exist still returns `SimpleAck`**, not an error
(`if (sub != null)` guards only the removal). That is correct per ASHRAE — cancelling an unknown
subscription is not an error condition.

The teardown in `BLocalBacnetDevice.unsubscribeCov` has a subtlety `[CERT]`:

```java
Object[] children = ((BComponent)export).getChildren(BBacnetCovSubscription.class);
// if no more cov subscription (expired or removed) on a object then,
// remove from cov subscription list
if (children.length <= 0) covSubscriber.unsubscribe(export, src);
```

The Baja event subscription on the underlying point is only torn down when the **last** COV subscription on
that export goes away — ten clients watching one point cost one Baja subscriber, not ten. The
`activeCovSubscriptions` list element, by contrast, is removed unconditionally, one per subscription.

---

## 273.9 — Clock-shift compensation `[CERT]`

An easy thing to get wrong, and Tridium handled it:

```java
/**
 * Clock Changed.
 * COV Subscriptions need to have their subscriptionEndTime adjusted by the
 * amount of the clock change.
 */
@Override
public void clockChanged(BRelTime shift) throws Exception
{
  SlotCursor<Property> sc = getProperties();
  while (sc.next(BBacnetCovSubscription.class))
  {
    BBacnetCovSubscription covSub = (BBacnetCovSubscription)sc.get();
    covSub.setSubscriptionEndTime(covSub.getSubscriptionEndTime().add(shift));
  }
}
```

`subscriptionEndTime` is an **absolute** time (`BAbsTime`), so an NTP correction or a manual clock change
would otherwise expire every subscription at once (jump forward) or extend them all (jump back). The
descriptor shifts every end-time by the delta.

Note the **`Clock.Ticket` is not re-scheduled** — only the recorded end time moves. So after a clock shift
the ticket and the end-time disagree by the shift amount, and whichever fires first wins: the ticket on its
original relative schedule, or the lazy reap in `doSendCovNotification` on the corrected end-time. For a
backward clock shift the end-time moves earlier relative to the ticket, so the lazy reap can delete a
subscription while its ticket is still pending — harmless, since `doRemoveCovSubscription` cancels the
ticket. `[INFER]`

---

## 273.10 — A logging defect

In `covRequestValidation` `[CERT]`:

```java
logger.info("Attempt to subscribe-" + (requestType == 28 ? "Cov" : "CovP") + " for non-Cov object " + objectId);
```

The two call sites pass `requestType = 5` (from `processSubscribeCovRequest`) and `requestType = 28`
(from `covpRequestValidation`) `[CERT]`. Since **5 = SubscribeCOV** and **28 = SubscribeCOVProperty**, the
ternary is **inverted**: a plain COV rejection logs `"subscribe-CovP"`, and a COV-Property rejection logs
`"subscribe-Cov"`.

Low severity — it does not affect the returned error, only the INFO log line. But it is exactly the log you
would grep while debugging why a subscribe is being refused, and it names the wrong service. Worth knowing
before trusting that message.

*(Unlike the dead `isSupported()` of B272 §272.8, this one is on the live path.)*

---

## 273.11 — Field notes

1. **Server-side lifetime ceiling is 8 hours (28 800 s), hardcoded, no slot.** Over that → rejected with
   `VALUE_OUT_OF_RANGE`, not clamped. `[CERT]` §273.3
2. **`lifetime = 0` is accepted** (indefinite) even though 28 801 is refused. `[CERT]` §273.3
3. **No cap on subscription COUNT** as a server. `[CERT]` §273.3
4. **Subscriptions are `TRANSIENT` — a station restart drops them all.** Clients must resubscribe.
   `[CERT]` §273.5
5. **COV-Property on a Niagara station is POLLED**, so it carries poll latency; plain COV is event-driven.
   `[CERT]` §273.6
6. **You cannot COV-Property-subscribe to properties 28/75/77/79/371** — rejected `NOT_COV_PROPERTY`.
   `[CERT]` §273.2
7. **Error `(class 1, code 1000)` is Tridium-proprietary** `TARGET_NOT_CONFIGURED` — a broken export ORD,
   not your client's fault. `[CERT]` §273.2
8. **Every subscribe and every renewal sends an immediate notification.** `[CERT]` §273.7
9. **Subscription identity includes network number + MAC**, so the same device on two routes = two
   subscriptions. `[CERT]` §273.4
10. **Rejection logs name the wrong service** (Cov ↔ CovP inverted). `[CERT]` §273.10

---

## 273.12 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| Service choices 5 / 28 | `CovHandler` dispatch `case 5:` / `case 28:` | `[CERT]` |
| 8-hour cap | `else if (lifetime > 28800L) … NErrorType(5, 37)` | `[CERT]` |
| Error code decode | `BBacnetErrorCode`: 31 `UNKNOWN_OBJECT`, 32 `UNKNOWN_PROPERTY`, 37 `VALUE_OUT_OF_RANGE`, 44 `NOT_COV_PROPERTY`, 45 `OPTIONAL_FUNCTIONALITY_NOT_SUPPORTED`, 1000 `TARGET_NOT_CONFIGURED` | `[CERT]` |
| Blacklisted COVP properties | `case 28: case 75: case 77: case 79: case 371: → NErrorType(2, 44)` | `[CERT]` |
| Dedup key = 6 fields | `findSubscription` conjunction, verbatim | `[CERT]` |
| Subscriptions not persisted | `add("covSubscription?", sub, Flags.TRANSIENT \| Flags.READONLY)` | `[CERT]` |
| COVP is polled | `if (cov.isCovProperty()) covPropPoller.subscribe(cov); else covSubscriber.subscribe(...)` | `[CERT]` |
| Expiry via Clock ticket | `covSub.setTicket(Clock.schedule(this, subLife, removeCovSubscription, covSub))` | `[CERT]` |
| Notification on every subscribe | `sendCovNotification(covSub)` outside the `if (lifetime > 0)` | `[CERT]` |
| Baja subscriber torn down only on last | `if (children.length <= 0) covSubscriber.unsubscribe(export, src)` | `[CERT]` |
| Clock shift adjusts end times | `clockChanged(BRelTime shift)` walks subscriptions | `[CERT]` |
| Log labels inverted | `(requestType == 28 ? "Cov" : "CovP")` vs call sites passing 5 and 28 | `[CERT]` |
| No count cap as server | absence of any counter/maximum in the admission path | `[CERT]` (absence) |
| COVP array-index lookup drops its argument | `findSubscription(true, …, propertyId, NOT_USED)` — parameter unused | `[CERT]` (the code) / `[INFER]` (the consequence) |
| Finite→indefinite renewal keeps stale end time | composed from `startCovTimer` + `getTimeRemaining` + lazy reap | `[INFER]` |
| Clock shift desynchronises ticket vs end-time | ticket not rescheduled in `clockChanged` | `[INFER]` |

Tally: **[CERT] 13 / [INFER] 3.**

---

## 273.x — Connections

- **B272** — the send path. **Gap B272-G1 CLOSED by this block.** §272.9's "server COV is event-driven" is
  refined here: true for plain COV, **false for COV-Property**, which is polled (§273.6).
- **B271** — `objectTypes.xml` / `getPropertyList()`, which §273.2's COV-Property validation consults.
- **B23 §23.10** — the SubscribeCOV PDU format this block's handler parses.
- **B34** — alarm framework; `BLocalBacnetDevice` hosts both the COV table and the event/alarm exports.

### Gaps

| ID | Gap | Class |
|---|---|---|
| **B273-G1** | `LocalBacnetCovPropPoll` — the COV-Property poller: its rate, whether it is configurable, and how it interacts with the station's other poll services. Named but not opened. | STATIC-investigable |
| **B273-G2** | `BBacnetCovSubscription` constructor: whether it stores the real `propertyArrayIndex` or `NOT_USED`, which decides whether the §273.4 lookup asymmetry is a live defect. | STATIC-investigable |
| **B273-G3** | `Active_COV_Subscriptions` (property 152) read path — how the ORD list is rendered back to a remote `ReadProperty`. | STATIC-investigable |
| ~~**B272-G1**~~ | ~~server subscription table~~ — **CLOSED by this block**. | closed |
| **B273-G4** | Live: confirm the 8-hour rejection, the finite→indefinite stale-end-time behaviour (§273.7), and the array-indexed COV-Property case (§273.4). | requires-execution |
