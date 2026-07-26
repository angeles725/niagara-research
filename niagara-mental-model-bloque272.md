# Block 272 — BACnet COV deep-dive: client subscription state machine, resubscription arithmetic, the server-side notification engine, and the unsolicited-COV gate

> Research of **Change-of-Value** end-to-end in the Niagara N4.14 BACnet driver — **both directions**.
> B23 §23.10 documented the *service formats* (SubscribeCOV / UnconfirmedCOVNotification /
> SubscribeCOVProperty PDU shapes). This block documents **everything that decides when those PDUs are
> sent**: the four-condition client gate, the ten-state subscription machine, the resubscription
> arithmetic (which B23 got wrong), the failure→polling fallback, and — entirely unexplored until now —
> the **server side**: how a Niagara point exported as a BACnet object decides to notify *its* subscribers.
>
> Extends [B271] §271.10, which covered COV only at tuning-policy depth and left one `[INFER]` open.
> **That `[INFER]` is now resolved and it was pointing the wrong way** — see §272.2.
>
> **Sources**: original Tridium source (`docSource/…/bacnet-rt/javax/baja/bacnet/**`) for `BBacnetProxyExt`,
> `BBacnetDevice`, `BBacnetTuningPolicy`, `BBacnetObjectType`, and the whole `javax.baja.bacnet.export`
> package (`Cov.java`, `BacnetCovSubscriber.java`, `BIBacnetCovSource.java`, the `B*Descriptor` family);
> Vineflower decompile for `com.tridium.bacnet.stack.CovHandler`.
>
> Markers: `[CERT]` verbatim in source · `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 272.1 — The two COV engines

Niagara is both a COV **client** (it subscribes to field devices) and a COV **server** (other BACnet clients
subscribe to points Niagara exports). These are separate code paths that share almost nothing:

| | Client side | Server side |
|---|---|---|
| Entry | `BBacnetProxyExt.readSubscribed()` | `BacnetCovSubscriber.event()` (a Baja `Subscriber`) |
| Decision | `useCov() && canAddCov() && canSupportCov() && propId==85` | `checkCov(current, lastSent)` per descriptor type |
| Transport | `BBacnetDevice.subscribeCov()` → `client().subscribeCov(...)` | `Cov` (a `Runnable`) → `postAsync` → `client().{un}confirmedCovNotification` |
| State | `subState` (10 values) on the ProxyExt | `BBacnetCovSubscription` child components on the descriptor |
| Package | `javax.baja.bacnet.point` | `javax.baja.bacnet.export` |
| Logger | `bacnet.point` / `bacnet.client` | **`bacnet.server`** |

That last row is a practical debugging note: **client and server COV log under different logger names.**
Turning up `bacnet.client` tells you nothing about why your exported points are not notifying.

---

## 272.2 — Resubscription arithmetic: B23 and B271 were both wrong `[CERT]`

The constants, verbatim from `BBacnetProxyExt`:

```java
private static final int  DAY_IN_MINUTES = 1440;               // 24(hrs) * 60(min)
private static final long RESUBSCRIPTION_FACTOR = 30000L;      // 60sec/min * 1000ms/sec / 2 (safety factor)
private static final long MINIMUM_SUBSCRIPTION_LIFETIME = 5;   // in minutes
private static final int  INTERVAL_ON_POST_FAILURE = 10;       // in seconds

private static final BRelTime RELTIME_ON_POST_FAILURE = BRelTime.makeSeconds(INTERVAL_ON_POST_FAILURE);
private static final BRelTime ONCE_A_DAY_RELTIME = BRelTime.make(DAY_IN_MINUTES * RESUBSCRIPTION_FACTOR);
private static final BRelTime MINIMUM_RELTIME    = BRelTime.make(MINIMUM_SUBSCRIPTION_LIFETIME * RESUBSCRIPTION_FACTOR);
```

`RESUBSCRIPTION_FACTOR` is **not** a multiplier of 2 — it is `60000 / 2`, i.e. *minutes-to-milliseconds
divided by two*. The comment says so explicitly. So:

```java
return BRelTime.make(subLife * RESUBSCRIPTION_FACTOR);   // subLife in MINUTES
```

means **resubscribe at half the subscription lifetime**. Working the numbers:

| `covSubscriptionLifetime` | Wire lifetime sent | Actual resubscribe interval |
|---|---|---|
| 15 (default) | 900 s | 15 × 30 000 ms = **7 min 30 s** |
| 60 | 3 600 s | **30 min** |
| 5 (or anything ≤ 5) | 300 s | `MINIMUM_RELTIME` = 5 × 30 000 = **2 min 30 s** |
| 0 | 0 (indefinite) | `ONCE_A_DAY_RELTIME` = 1440 × 30 000 = **12 hours** |
| — (post failure) | — | `RELTIME_ON_POST_FAILURE` = **10 s** |

Three corrections fall out:

1. **B23 §23.10 says "Cada ~lifetime * 0.8".** Wrong — it is **lifetime × 0.5**, exactly. And that
   sentence describes the *server* refreshing the client, which is not what this code does at all: this is
   the **client re-subscribing on its own timer**, independent of whether notifications arrived.
2. **B23 §23.10 says "lifetime … 3600 default".** Wrong. The slot default is **15**, and its unit is
   **minutes**, declared with a facet: `newProperty(0, 15, BFacets.makeInt(UnitDatabase.getUnit("minute")))`.
   The 3600 s figure appears nowhere in the driver.
3. **B271 §271.10 flagged `subLife * 2` as `[INFER]` and guessed the interval was twice the lifetime.**
   It is half. Corrected below in §272.11.

And a naming trap worth stating plainly: **`ONCE_A_DAY_RELTIME` fires every 12 hours, not every 24.** The
constant is built from `DAY_IN_MINUTES` but passed through the same halving factor. The javadoc promise —
*"force a resubscription at least once per day"* — is still satisfied, just at double the advertised rate.

---

## 272.3 — Wire lifetime: minutes in, seconds out `[CERT]`

The slot is minutes; BACnet wants seconds. `BBacnetDevice` (note Tridium's own typo in the method name):

```java
/**
 * @param subLife desired subscription lifetime in minutes
 * @return minimum of desired subscription lifetime or MINIMUM_COV_SUBSCRIPTION_LIFETIME in seconds
 */
private int calculateSubcriptionLifetime(int subLife)
{
  subLife *= 60; //Converting from minutes to seconds.

  // Setting subscription lifetime to 0 stands for an un-subcription request.
  if ((subLife < 0) || (subLife > 0 && subLife < MINIMUM_COV_SUBSCRIPTION_LIFETIME))
  {
    subLife = MINIMUM_COV_SUBSCRIPTION_LIFETIME;
  }
  return subLife;
}
```

with `MINIMUM_COV_SUBSCRIPTION_LIFETIME = 300` (seconds = 5 minutes) `[CERT]` `BBacnetDevice:205`.

**Zero passes through untouched** — the guard is `subLife > 0 && subLife < 300`. So a policy of `0` sends
`lifetime = 0` on the wire.

There is a **documentation conflict inside Tridium's own source** here `[CERT]`:

- The slot javadoc says: *"A value of zero means an indefinite lifetime, although this is not guaranteed to
  persist across resets of the server device."*
- The method comment says: *"Setting subscription lifetime to 0 stands for an un-subcription request."*

Per ASHRAE 135, in a `SubscribeCOV` **with** the `issueConfirmedNotifications` parameter present, `lifetime = 0`
means an indefinite subscription; the *absence* of that parameter is what signals cancellation. Niagara
always passes `pt.useConfirmedCov()`, so the **slot javadoc is the accurate one** and the method comment is
misleading `[INFER]`. Practical read: `covSubscriptionLifetime = 0` gives you an indefinite subscription
refreshed every 12 hours — which is exactly why `ONCE_A_DAY_RELTIME` exists as the fallback for that branch.

---

## 272.4 — The client gate: four conditions, in order `[CERT]`

`BBacnetProxyExt.readSubscribed(Context)`:

```java
if (useCov() &&
    device.canAddCov() &&
    BBacnetObjectType.canSupportCov(getObjectId().getObjectType(), device) &&
    getPropertyId().getOrdinal() == PRESENT_VALUE)
{
   subscribeCov();
   forceRead();
}
else if (useCovProperty() && device.canAddCovProperty())
{
   subscribeCovProperty();
   forceRead();
}
else
{
   pollService = (BBacnetPoll)network().getPollService(this);
   pollService.subscribe(this);
   setSubState(SUB_STATE_POLLED);
}
```

Every gate, unpacked:

| Gate | Definition | Default / note |
|---|---|---|
| `useCov()` | `tuningPolicy.getUseCov()` | **`false`** — COV is opt-in per tuning policy |
| `canAddCov()` | `getUseCov() && (covSubscriptions < maxCovSubscriptions)` — **device-level** | `maxCovSubscriptions` default `Integer.MAX_VALUE`; `covSubscriptions` is `TRANSIENT｜READONLY` |
| `canSupportCov(objectType, device)` | protocol-revision table, §272.5 | reads `device.getProtocolRevision()` |
| `propertyId == PRESENT_VALUE` | 85 | **plain COV is present-value only** |

Note there are **two independent `useCov` flags**: one on the **tuning policy** (per point) and one on the
**device** (`getUseCov()` inside `canAddCov()`). Both must be true. A point whose tuning policy enables COV
still polls if the device-level flag is off — and nothing in the UI makes that obvious.

`canAddCovProperty()` uses the **same counter and the same maximum** as `canAddCov()`:

```java
public boolean canAddCov()         { return getUseCov()         && (getCovSubscriptions() < getMaxCovSubscriptions()); }
public boolean canAddCovProperty() { return getUseCovProperty() && (getCovSubscriptions() < getMaxCovSubscriptions()); }
```

So COV and COV-Property subscriptions **share one budget** per device. Setting `maxCovSubscriptions` to
throttle a weak controller throttles both.

COV-Property, by contrast, has **no property-id restriction and no object-type gate** — only the flag and
the counter. That is the supported route for subscribing to something other than present-value.

---

## 272.5 — `canSupportCov`: a protocol-revision table `[CERT]`

`BBacnetObjectType.canSupportCov(int ordinal, int pr)`. The single-arg form is `@Deprecated` "Since 3.5".

```java
if (ordinal > MAX_ASHRAE_ID) return false;
switch (ordinal) {
  case ANALOG_INPUT: case ANALOG_OUTPUT: case ANALOG_VALUE:
  case BINARY_INPUT: case BINARY_OUTPUT: case BINARY_VALUE:
  case MULTI_STATE_INPUT: case MULTI_STATE_OUTPUT: case LOOP:      return pr >= 0;
  case MULTI_STATE_VALUE:                        /* Add-135-1995b */ return pr >= 1;
  case LIFE_SAFETY_POINT: case LIFE_SAFETY_ZONE: /* Add-135-1995c */ return pr >= 2;
  case PULSE_CONVERTER:                          /* Add-135-2001c */ return pr >= 4;
  case LOAD_CONTROL: case ACCESS_DOOR:           /* Add-135-2004e/f */ return pr >= 6;
  case ACCESS_POINT: case CREDENTIAL_DATA_INPUT: /* Add-135-2008j */ return pr >= 9;
  case LARGE_ANALOG_VALUE: case INTEGER_VALUE: case POSITIVE_INTEGER_VALUE:
  case OCTET_STRING_VALUE: case CHARACTER_STRING_VALUE: case TIME_VALUE:
  case DATE_TIME_VALUE: case DATE_VALUE: case TIME_PATTERN_VALUE:
  case DATE_PATTERN_VALUE: case DATE_TIME_PATTERN_VALUE:
                                                 /* Add-135-2008w */ return pr >= 10;
  case LIGHTING_OUTPUT:                          /* Add-135-2010i */ return pr >= 14;
  case BINARY_LIGHTING_OUTPUT:                   /* Add-135-2012az */ return pr >= 16;
  //case STAGING:                                /* Add-135-2016bd */ return pr >= 20;
}
return false;
```

Two operational consequences:

1. **A device that does not answer `protocolRevision` gets `pr = 0`.** `canSupportCov(ordinal, device)`
   calls `device.getProtocolRevision()`; if the read failed, everything above `pr >= 0` is refused. An MSV
   on a device with an unreadable protocol revision **silently falls back to polling** — the classic
   "COV won't turn on and there's no error" symptom.
2. `STAGING` (PR 20) is **commented out in the shipped source**. Staging objects cannot use COV in N4.14
   regardless of what the device advertises.

---

## 272.6 — The subscription state machine `[CERT]`

Ten states. The comments are Tridium's own, including the two `// ???`:

```java
public void doSubscribeCov()
{
  if (!isSubscribedDesired()) return;          // nothing subscribed to the point → skip

  switch (subState)
  {
    // For these cases, go to PENDING
    case SUB_STATE_UNSUB:              // initial subscription attempt
    case SUB_STATE_POLLED:             // tuning policy change, device.useCov change
      setSubState(SUB_STATE_FIRST_COV_PENDING);  break;

    // For these cases, keep COV but identify pending
    case SUB_STATE_COV:                // already COV, just resubscribing
      setSubState(SUB_STATE_COV_PENDING);        break;

    // For these cases, we want to skip everything
    case SUB_STATE_FIRST_COV_PENDING:  // somehow invoked twice? don't repeat
      return;

    // For these cases, we want to leave the sub state alone
    case SUB_STATE_POLLED_PENDING:     // failed COV, retrying subscription
    case SUB_STATE_COV_PENDING:        // ???
      break;
  }
  submitSubscribeCmd(PointCmd.SUBSCRIBE_COV_POINT);
}
```

`doSubscribeCovProperty()` is the exact mirror with `FIRST_COVP_PENDING` / `COVP` / `COVP_PENDING`.

State meanings, from the constant comments `[CERT]`:

| # | State | Meaning |
|---|---|---|
| 0 | `UNSUB` | not subscribed |
| 1 | `POLLED` | polling, COV not in use |
| 2 | `COV` | COV active |
| 3 | `FIRST_COV_PENDING` | initial subscribe in flight |
| 4 | `POLLED_PENDING` | *"cov/covp failed, polled pending cov retry"* |
| 5 | `COV_PENDING` | resubscribe in flight, COV still active |
| 6 | `FIRST_COVP_PENDING` | initial COV-Property subscribe in flight |
| 7 | `COVP` | COV-Property active |
| 8 | `COVP_PENDING` | COV-Property resubscribe in flight |
| 9 | `COVP_FAILED` | *"covp failed, polled pending cov retry"* |

**`POLLED_PENDING` (4) is the state to look for in the field.** It means COV was attempted and failed, the
point reverted to polling, and a retry is scheduled. A point sitting there permanently is a device that
accepts the subscribe request and then never honours it, or one that keeps timing out.

### Post-failure path

```java
private void submitSubscribeCmd(int pointCmd)
{
  boolean postFailed = false;
  try { network().postAsync(new PointCmd(pointCmd, this)); }
  catch (Exception e)
  {
    postFailed = true;
    setSubState(SUB_STATE_POLLED_PENDING);
    pollService = (BBacnetPoll)network().getPollService(this);
    pollService.subscribe(this);              // start polling immediately
  }
  if (pointCmd == PointCmd.SUBSCRIBE_COVP_POINT) scheduleResubscribeProperty(postFailed);
  else                                          scheduleResubscribe(postFailed);
}
```

Note this catches a failure to **queue** the command, not a failure of the subscription itself. Data keeps
flowing via polling while the 10 s retry timer runs.

---

## 272.7 — What happens when the SubscribeCOV actually fails `[CERT]`

`BBacnetDevice.subscribeCov(BBacnetProxyExt pt)`:

```java
if (!device().isOperational()) return false;     // short-circuit; readUnsubscribed handles the counter

boolean covOK = true;
if (!canAddCov()) covOK = false;
else {
  int subLife = calculateSubcriptionLifetime(pt.getCovSubscriptionLifetime());
  try {
    client().subscribeCov(getAddress(), 1, pt.getObjectId(), pt.useConfirmedCov(), subLife);
    pingOk();
  }
  catch (BacnetException e) {
    if (getDisableDeviceOnCovSubscriptionFailure() && e instanceof TransactionTimeoutException) {
      device().pingFail(lex.get("bacnetDevice.subscribeCov.failure"));
      log.severe(this + ": Timeout detected, marking the device down due to no response.");
    }
    plog.log(Level.SEVERE, ...);
    covOK = false;
  }
}

BBacnetPoll pollService = (BBacnetPoll)network().getPollService(pt);
if (covOK) {
  pollService.unsubscribe(pt);                                  // stop polling
  if (!pt.isCOV()) setCovSubscriptions(getCovSubscriptions() + 1);
} else {
  if (!pt.isPolled()) pollService.subscribe(pt);                // start polling
  if (pt.isCOV())  setCovSubscriptions(getCovSubscriptions() - 1);
}
pt.setSubState(covOK ? BBacnetProxyExt.SUB_STATE_COV : BBacnetProxyExt.SUB_STATE_POLLED_PENDING);
return covOK;
```

Three things worth pinning down:

**(a) The subscriber process identifier is hardcoded to `1`.** Not a counter, not a per-point id — the
literal `1`, in all four call sites (`subscribeCov`, `subscribeCovProperty`, `unsubscribeCov`,
`unsubscribeCovProperty`) `[CERT]`. **Scope corrected by [B274] §274.6**: this is true of the *point-proxy*
path only. Niagara does have a real configurable process id — `device.getAlarms().getNiagaraProcessId()` —
and `BBacnetTrendLogRemoteExt` uses it. B23 §23.10 describes it as *"long (unique per client)"*, which is the
standard's intent but **not** what this driver does. Consequence `[INFER]`: from the device's point of
view every Niagara subscription arrives under the same process id, distinguished only by monitored-object.
That is legal — ASHRAE keys a subscription on (recipient, process-id, monitored-object) — but it means you
cannot tell two Niagara stations apart by process id, and a device with a buggy subscription table that
keys only on process id will collide.

**(b) `disableDeviceOnCovSubscriptionFailure` can take the whole device down.** It is a slot on the device.
When true, a `TransactionTimeoutException` on a *single point's* subscribe calls `pingFail()` — marking the
**device** down, not the point. Every other point on that device goes with it. Powerful, and dangerous if
one flaky object is enough to blind you to a working controller.

**(c) Polling and COV are strictly exclusive, and the swap is transactional.** Success unsubscribes from the
poll service; failure subscribes to it. The `covSubscriptions` counter only moves when the state actually
changes (`if (!pt.isCOV())` / `if (pt.isCOV())`), so repeated resubscribes do not inflate it.

`subscribeCovProperty()` is structurally identical, with one addition — it passes the property reference and
the increment:

```java
client().subscribeCovProperty(getAddress(), 1, pt.getObjectId(), pt.useConfirmedCovProperty(), subLife,
                              new BBacnetPropertyReference(pt.getPropertyId().getOrdinal(),
                                                           pt.getPropertyArrayIndex()),
                              pt.getCovPropertyIncrement());
```

`covPropertyIncrement` default is `1.0` `[CERT]` `BBacnetTuningPolicy`.

---

## 272.8 — Receiving notifications: `CovHandler` and the unsolicited gate `[CERT]`

`com.tridium.bacnet.stack.CovHandler.processCov(CovNotificationParameters)`:

```java
BBacnetDevice device = BBacnetNetwork.bacnet().doLookupDeviceById(cnp.getInitiatingDeviceId());
if (device == null) {
   logger.fine("Cov Notification from unmapped device:" + cnp.getInitiatingDeviceId());
   return;                                    // silently dropped at FINE level
}

if (serverLayer.getUpdateStatusOnCov() && !device.isDown()) device.pingOk();

for (int i = 0; i < propVals.length; i++) {
   if      (propVals[i].getPropertyId() == 111) status = AsnUtil.asnStatusFlagsToBStatus(...);   // statusFlags
   else if (propVals[i].getPropertyId() == 196) updateDevice(device, "lastRestartReason", ...);
   else if (propVals[i].getPropertyId() == 203) updateDevice(device, "timeOfDeviceRestart", ...);
   else                                         updatePointValue(device, objectId, propVals[i]);
}
updateStatusFlags(device, objectId, status);
```

Notes:

- **A notification from a device not in the station's database is discarded at `FINE` log level.** No
  warning, no counter. If you are debugging "the device says it's sending COV and Niagara shows nothing",
  this is the first thing to rule out — and you need `bacnet.client` at FINE to see it.
- `updateStatusOnCov` (a **server-layer** flag) makes an inbound COV count as a successful ping — **but it
  defaults to `false`** ([B274] §274.4). Only when it is enabled can a device on pure COV stay "up" without
  polling traffic; left at its default, a healthy COV-only device can still be marked down by the ping
  timer.
- Properties **196** (`lastRestartReason`) and **203** (`timeOfDeviceRestart`) are intercepted and written
  onto the *device object*, not onto a point. Device-restart COV is handled out-of-band.

### The unsolicited gate

```java
private static void updatePointValue(BBacnetDevice device, BBacnetObjectIdentifier objectId, PropertyValue propVal)
{
   BControlPoint[] points = device.getPoints().findPoints(objectId, propVal.getPropertyId(),
                                                          propVal.getPropertyArrayIndex());
   for (int i = 0; i < points.length; i++) {
      BBacnetProxyExt proxyExt = (BBacnetProxyExt)points[i].getProxyExt();
      if (!proxyExt.isPolled() || proxyExt.getAcceptUnsolicitedCov()) {
         proxyExt.fromEncodedValue(propVal.getPropertyValue(), null, BBacnetProxyExt.covContext);
      }
   }
}
```

`isPolled()` is `subState == POLLED || subState == POLLED_PENDING`. So:

> **A point in polled mode DISCARDS incoming COV notifications**, unless `acceptUnsolicitedCov` is set.
> Default: **`false`**.

This is the highest-value finding in the block for field work. Consider a device configured to blast
unconfirmed COV notifications on its own initiative (common in some controllers, and the standard's
`UnconfirmedCOVNotification` does not require a prior subscription). Niagara receives the packets, resolves
the device, resolves the point — and then **throws the value away** because that point happens to be in
`POLLED`. Meanwhile the same value arrives correctly a few seconds later via the poll, so the symptom is
not "no data" but "data that lags and ignores the device's own push". `acceptUnsolicitedCov = true` on the
tuning policy is the fix.

The same gate bites during the `POLLED_PENDING` retry window: a point that just failed a subscribe will
drop any COV that *does* arrive while it retries.

Status flags take a separate path — `updateStatusFlags()` — and are only applied to points whose
`useStatusFlags()` facet is set; when the notification carried no status property, it posts a
`PointCmd(0x10000000, ext)` to fetch it `[CERT]`.

### Dead code, flagged honestly

```java
private static boolean isSupported(int propertyId) {
   int[] propertyIds = new int[]{355, 354, 356, 357, 168, 113, 17, 35, 0, 72, 353, 45, 59, 25, 52};
   return Arrays.stream(propertyIds).filter(x -> x == propertyId).findFirst() == null;
}
```

`IntStream.findFirst()` returns an `OptionalInt`, which is never `null` — so this method always returns
`false`. **It has zero callers** (verified corpus-wide: the only match for `isSupported(` in `bacnet-rt` is
its own declaration) `[CERT]`. So this is dead private code carrying a latent `Optional == null` bug, not an
active defect. Worth knowing only so nobody "fixes" it into the live path without rewriting the predicate.

---

## 272.9 — The server side: how Niagara notifies *its* subscribers `[CERT]`

Entirely absent from the corpus before this block. Four moving parts.

### (1) The trigger — a Baja `Subscriber`, not a poll

`javax.baja.bacnet.export.BacnetCovSubscriber extends Subscriber`:

```java
public void event(BComponentEvent event)
{
  BComponent src = event.getSourceComponent();
  if (event.getId() == BComponentEvent.PROPERTY_CHANGED) {
    BIBacnetCovSource export = sublist.get(src);
    if (export != null) {
      if (event.getSlot() == export.getOutProperty())      export.checkCov();
      else if (event.getSlotName().equals("loopEnable"))   export.checkCov();
    }
    else logger.fine("BacnetCovSubscriber received event for unknown component: " + src);
  }
}
```

Server-side COV is **event-driven off the Niagara component model**, not timed. Every write to the exported
point's `out` slot synchronously calls `checkCov()`. `loopEnable` is special-cased for Loop objects.

> **REFINED by [B273] §273.6.** This applies to **plain COV only**. `BLocalBacnetDevice.subscribeCov()`
> dispatches COV-**Property** subscriptions to a separate `LocalBacnetCovPropPoll` **poller** instead of to
> `BacnetCovSubscriber`. That is why the COV-Property branch of `doCheckCov()` below does a full
> `readProperty` + decode on each evaluation: it runs on a poll tick, not on a change event. COV-Property
> therefore carries poll latency; plain COV does not.

### (2) The predicate — one `checkCov` per descriptor family

**Analog** (`BBacnetAnalogPointDescriptor`) `[CERT]`:

```java
boolean checkCov(BStatusValue currentValue, BStatusValue covValue)
{
  if (currentValue.getStatus().getBits() != covValue.getStatus().getBits()) return true;

  // Handle NaNs.
  double cur = ((BStatusNumeric)currentValue).getNumeric();
  double lst = ((BStatusNumeric)covValue).getNumeric();
  if (Double.isNaN(cur))      return !Double.isNaN(lst);
  else if (Double.isNaN(lst)) return true;

  return Math.abs(cur - lst) >= getCovIncrement();
}
```

**Binary** (`BBacnetBinaryPointDescriptor`) `[CERT]`:

```java
boolean checkCov(BStatusValue currentValue, BStatusValue covValue)
{
  if (currentValue.getStatus().getBits() != covValue.getStatus().getBits()) return true;
  return ((BStatusBoolean)currentValue).getBoolean() != ((BStatusBoolean)covValue).getBoolean();
}
```

Four things follow:

- **Any status-bit change notifies, regardless of value.** A point going to fault/stale/overridden fires a
  COV even if the number did not move. This is correct per standard, and it is why a flapping status
  generates notification storms.
- The comparison is **`>=`**, not `>`. With `covIncrement = 0.0`, `abs(diff) >= 0` is *always true* — so a
  zero increment means **notify on every single value change**, including a write of the identical value if
  it produces a PROPERTY_CHANGED event. Zero is not "disabled"; zero is "maximum chatter".
- NaN is handled explicitly and asymmetrically: NaN→NaN does not notify; NaN→number and number→NaN both do.
- Binary/multi-state have **no increment at all** — value inequality is the whole test.

### (3) The scan — `doCheckCov()` walks the subscription children

```java
public final void doCheckCov()
{
  SlotCursor<Property> c = getProperties();
  while (c.next(BBacnetCovSubscription.class))
  {
    BBacnetCovSubscription covSub = (BBacnetCovSubscription)c.get();
    if (covSub.isCovProperty()) { ... per-property comparison, see below ... }
    else if (checkCov(getCurrentStatusValue(), covSub.getLastValue())) sendCovNotification(covSub);
  }
}
```

Each subscription is a **child component** of the export descriptor, holding `recipient`,
`monitoredPropertyReference`, `issueConfirmedNotifications`, `subscriptionEndTime`, `covIncrement`, and the
last value sent. State is per-subscriber, so ten clients on one point compare against ten independent
baselines.

The COV-Property branch has its own increment resolution `[CERT]`:

```java
double covIncrement = covSub.getCovIncrement();
if (Double.isNaN(covIncrement)) {
   if (monitoredPropertyReference.getPropertyId() == PRESENT_VALUE) {
      BDouble d = (BDouble)this.get("covIncrement");     // fall back to the object's own covIncrement
      covIncrement = (d != null) ? d.getDouble() : 0.0D;
   } else {
      covIncrement = 0.0D;                                // any other property: notify on ANY change
   }
}
if (diff >= covIncrement) sendCovNotification(covSub);
```

So **a COV-Property subscription to anything other than present-value, with no explicit increment,
defaults to an increment of 0.0 — notify on every change.** Combined with the `>=` above, that is a
firehose. `[CERT]`

Non-REAL properties skip the arithmetic entirely: `if (!cv.equals(covSub.getLastPropValue())) send…`.

### (4) The send — `Cov`, a coalesceable `Runnable`

```java
public void doSendCovNotification(BBacnetCovSubscription covSub)
{
  if (covSub.getTimeRemaining() < 0) { removeCovSubscription(covSub); return; }   // expired → drop it
  Cov cov = new Cov(covSub, this, pt);
  BBacnetNetwork.bacnet().postAsync(cov);
  if (covSub.isCovProperty()) { covSub.setLastPropValue(...); covSub.setLastStatusBits(...); }
  else                          covSub.setLastValue(getCurrentStatusValue());
}
```

The baseline is updated **at post time, not at send time** — so a notification that fails on the wire still
moves the baseline, and that value will not be retried. `[INFER]`

`Cov implements Runnable, ICoalesceable`. Coalescing means bursts of changes on one (subscription, object,
point) triple collapse into a single queued notification. Tridium's own honesty about the implementation
`[CERT]`:

```java
public int hashCode() {
  //A constant hashCode is better than no hashCode.
  //If we start putting cov requests into maps of queues,
  //a better hashCode will be required.
  return 1;
}
```

`Cov.run()` `[CERT]`:

```java
int timeRemaining = sub.getTimeRemaining();
if (timeRemaining < 0) return;                            // second expiry check, at send time

BBacnetRecipient recipient = sub.getRecipient().getRecipient();
address = (recipient.getChoice() == BBacnetRecipient.DEVICE_TAG)
        ? DeviceRegistry.getDeviceAddress(recipient.getDevice())    // by device id
        : recipient.getAddress();                                   // by explicit address

buildPropertyValues(export);
CovNotificationParameters cnp = new CovNotificationParameters(
    sub.getRecipient().getProcessIdentifier().getUnsigned(),
    BBacnetNetwork.localDevice().getObjectId(), sub.getMonitoredPropertyReference().getObjectId(),
    timeRemaining, propertyValues);

if (sub.getIssueConfirmedNotifications())
     client().confirmedCovNotification(address, cnp);
else client().unconfirmedCovNotification(address, cnp);
```

Note the **subscriber's** process identifier is echoed back correctly here — unlike the client side, the
server honours whatever id the remote asked with.

### What actually goes in the notification

`buildPropertyValues()` `[CERT]`:

| Object type | Property values sent |
|---|---|
| **Loop** | `presentValue`(monitored), `statusFlags`, **`setpoint`**, **`controlledVariableValue`** — 4 values |
| Everything else | monitored property + `statusFlags` — 2 values |

And a caching asymmetry, with Tridium's comment verbatim:

```java
if (!sub.isCovProperty()) {
  //Standard COV still has to duplicate the read property (because the checkCov() abstraction
  //prevents caching the previously generated property value
  cov = readPropertyValue(export);
  status = readStatus(export);
} else {
  //CovPropertyValues cache the property to be sent on the subscription.
  PropertyValue last = sub.getLastPropertyValue();
  cov = (last != null) ? new NBacnetPropertyValue(last) : readPropertyValue(export);
  ...
}
```

**Standard COV re-reads the property on every notification; COV-Property serves it from cache.** On a
descriptor with an expensive `readProperty`, standard COV costs strictly more per notification than
COV-Property does — the opposite of what the names suggest.

### Expiry

`BBacnetCovSubscription.getTimeRemaining()` `[CERT]`:

```java
public int getTimeRemaining()
{
  if (getSubscriptionEndTime().equals(BAbsTime.NULL)) return 0;
  long curTime = BAbsTime.make().getMillis();
  int timeRemaining = (int)((getSubscriptionEndTime().getMillis() - curTime) / 1000);
  return timeRemaining > 0 ? timeRemaining : -1;
}
```

A **null end time returns `0`, not `-1`** — and both `Cov.run()` and `doSendCovNotification()` test
`< 0`. So an indefinite subscription (`endTime == NULL`) passes both guards and reports
`timeRemaining = 0` on the wire, which is exactly the standard's encoding for "indefinite". Clean.

---

## 272.10 — Field gotchas

1. **Resubscribe fires at half the lifetime.** Default 15 min → every 7 min 30 s. `[CERT]` §272.2
2. **`ONCE_A_DAY_RELTIME` is 12 hours.** `[CERT]` §272.2
3. **A polled point silently drops incoming COV** unless `acceptUnsolicitedCov = true` (default `false`).
   The classic "the device is pushing and Niagara ignores it". `[CERT]` §272.8
4. **COV needs `useCov` true in two places** — the point's tuning policy *and* the device. `[CERT]` §272.4
5. **COV and COV-Property share one subscription budget** (`maxCovSubscriptions`) per device. `[CERT]` §272.4
6. **A device with an unreadable `protocolRevision` gets `pr = 0`** and loses COV on MSV and everything
   newer, with no error. `[CERT]` §272.5
7. **`disableDeviceOnCovSubscriptionFailure` marks the whole DEVICE down** on one point's subscribe timeout.
   `[CERT]` §272.7
8. **Subscriber process id is hardcoded to `1`.** `[CERT]` §272.7
9. **`covIncrement = 0.0` means notify on every change, not "disabled"** — the test is `>=`. And
   COV-Property on a non-present-value property defaults to 0.0. `[CERT]` §272.9
10. **Any status-bit change notifies**, value unchanged. Flapping status → notification storm. `[CERT]` §272.9
11. **`SUB_STATE_POLLED_PENDING` (4) is the diagnostic state**: COV failed, polling, retrying. `[CERT]` §272.6
12. **Server COV logs under `bacnet.server`**, client under `bacnet.client` / `bacnet.point`. `[CERT]` §272.1
13. **Standard COV re-reads the property per notification; COV-Property caches it.** `[CERT]` §272.9

---

## 272.11 — Corrections to earlier blocks

| Target | Was | Is `[CERT]` |
|---|---|---|
| **B23 §23.10** "Refresh: cada ~lifetime * 0.8" | client refresh at 0.8 × lifetime | **0.5 × lifetime**, and it is a client-side timer, not a server refresh |
| **B23 §23.10** "lifetime … 3600 default" | 3600 s | **15 minutes** (`covSubscriptionLifetime`, facet `minute`) |
| **B23 §23.10** "subscriber-process-id long (unique per client)" | unique per client | **hardcoded `1`** in all four call sites |
| **B271 §271.10** `[INFER]`: "the resubscribe interval is *twice* the lifetime" | ×2 | **÷2**. `RESUBSCRIPTION_FACTOR = 30000L` is `60000/2`. `[INFER]` resolved to `[CERT]`, opposite direction. |
| **B271 §271.10** listed only the tuning-policy slots | — | superseded by this block end-to-end |
| **B271 open gap B271-G3** (COV lifetime unit reconciliation) | open | **CLOSED** — §272.2 + §272.3 |

---

## 272.12 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| Resubscribe = lifetime ÷ 2 | `RESUBSCRIPTION_FACTOR = 30000L; // 60sec/min * 1000ms/sec / 2 (safety factor)` | `[CERT]` |
| ONCE_A_DAY is 12 h | `BRelTime.make(1440 * 30000)` = 43 200 000 ms | `[CERT]` |
| Wire minimum 300 s | `MINIMUM_COV_SUBSCRIPTION_LIFETIME = 300`, `BBacnetDevice:205` | `[CERT]` |
| Process id hardcoded | `client().subscribeCov(getAddress(), 1, …)` ×4 call sites | `[CERT]` |
| Polled points drop COV | `if (!proxyExt.isPolled() \|\| proxyExt.getAcceptUnsolicitedCov())` | `[CERT]` |
| `acceptUnsolicitedCov` default false | `newProperty(0, false, null)` | `[CERT]` |
| COV/COVP share budget | both gates read `getCovSubscriptions() < getMaxCovSubscriptions()` | `[CERT]` |
| canSupportCov is PR-gated | full switch, `MULTI_STATE_VALUE → pr >= 1`, STAGING commented out | `[CERT]` |
| Analog test is `>=` | `Math.abs(cur - lst) >= getCovIncrement()` | `[CERT]` |
| Server COV is event-driven | `BacnetCovSubscriber extends Subscriber`, `PROPERTY_CHANGED → checkCov()` | `[CERT]` |
| Loop sends 4 property values | `propertyValues = new PropertyValue[]{cov, status, setpt, cvv}` | `[CERT]` |
| `isSupported()` is dead code | zero callers corpus-wide in `bacnet-rt`; `OptionalInt == null` always false | `[CERT]` |
| lifetime=0 means indefinite, not unsubscribe | slot javadoc vs method comment conflict; resolved against ASHRAE + the always-passed `useConfirmedCov` | `[INFER]` |
| Baseline advances at post time, not send time | `postAsync(cov)` precedes `setLastValue(...)`; no failure callback observed | `[INFER]` |

Tally: **[CERT] 12 / [INFER] 2.**

---

## 272.x — Connections

- **B271** — the discovery-to-point pipeline. §271.10 is superseded here; gap **B271-G3 closed**.
- **B23 §23.10** — the COV service PDU formats. Still valid as a wire reference; **three numbers corrected**
  in §272.11.
- **B133** — APDU encoding under `confirmedCovNotification` / `unconfirmedCovNotification`.
- **B34** — alarm framework. `BBacnetProxyExt.doAckAlarm` routes through the same server layer
  (`getServer().getEventHandler()`), and `bacnetAlarmRouter` is the intrinsic-event sibling of COV.
- **B7** — `BITunable` / `readSubscribed` / `readUnsubscribed` is the framework contract this implements.

### Gaps this block opens or leaves open

| ID | Gap | Class |
|---|---|---|
| ~~**B272-G1**~~ | ~~The COV subscription table on the server~~ — **CLOSED by [B273]**: admission, 4-gate validation, the hardcoded 8-hour cap, the 6-field dedup key, the `Clock` termination ticket, and clock-shift compensation. | closed |
| **B272-G2** | `BBacnetBooleanCovTrendLogExt` / COV-driven trend logging (`BBacnetClientCov` = "the choice for the COV increment used in acquiring data for a trend log via COV"). Untouched. | STATIC-investigable |
| **B272-G3** | `getUpdateStatusOnCov` on the server layer — where it is configured and what else it gates. | STATIC-investigable |
| **B271-G1** (still open) | The full export/descriptor family; this block opened only its COV surface. | STATIC-investigable |
| **B272-G4** | Live confirmation: COV notification storm behaviour under `covIncrement = 0`, and whether a failed confirmed notification retries at the transport layer (TSM) after the baseline has already advanced. | requires-execution |
