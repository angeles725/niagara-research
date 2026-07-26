# Block 275 — The BACnet export descriptor family (I): class hierarchy, the `BIBacnetExportObject` contract, the read/write property dispatch chain, `Property_List` construction, and the two point extensions

> First block against **B271-G1** — the `javax.baja.bacnet.export` package as a family. This is the
> **server-side object model**: how a Niagara `BControlPoint` is published *as* a BACnet object and answers
> `ReadProperty` / `ReadPropertyMultiple` / `WriteProperty` from remote clients. It is the mirror of the
> client-side pipeline documented in [B271].
>
> **COV is deliberately excluded** — that surface is already covered by [B272] §272.9, [B273] and [B274].
> This block covers everything else in the *core* of the family. The specialised descriptors
> (schedule / calendar / file / trendlog / notificationClass / eventEnrollment) and `BacnetDescriptorUtil`
> are deferred to a second pass — see §275.12.
>
> **Sources**: original Tridium source, `organized/docSource/docSource-doc/extracted/bacnet-rt/javax/baja/bacnet/export/**`
> (50 files, ~40 000 lines). Markers: `[CERT]` verbatim · `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 275.1 — The family: five roots, not one `[CERT]`

50 source files. The inheritance graph has **five independent roots**, all descending from `BComponent`:

```
BComponent
 │
 ├─ BBacnetEventSource                    (997 ln, abstract)   implements BIBacnetExportObject
 │   ├─ BBacnetPointDescriptor            (2523, abstract)     implements BIBacnetCovSource, BacnetPropertyListProvider
 │   │   ├─ BBacnetAnalogPointDescriptor       (760, abstract)
 │   │   │   ├─ BBacnetAnalogInputDescriptor        (441)
 │   │   │   ├─ BBacnetAnalogValueDescriptor        (348)
 │   │   │   │   ├─ BBacnetIntegerValueDescriptor         (173)
 │   │   │   │   ├─ BBacnetLargeAnalogValueDescriptor     (132)
 │   │   │   │   └─ BBacnetPositiveIntegerValueDescriptor (184)
 │   │   │   └─ BBacnetAnalogWritableDescriptor (645, abstract)  implements BacnetWritableDescriptor
 │   │   │       ├─ BBacnetAnalogOutputDescriptor              (203)
 │   │   │       ├─ BBacnetAnalogValuePrioritizedDescriptor    (220)
 │   │   │       ├─ BBacnetIntegerValuePrioritizedDescriptor   (333)
 │   │   │       ├─ BBacnetLargeAnalogValuePrioritizedDescriptor (271)
 │   │   │       └─ BBacnetPositiveIntegerValuePrioritizedDescriptor (331)
 │   │   ├─ BBacnetBinaryPointDescriptor       (517, abstract)
 │   │   │   ├─ BBacnetBinaryInputDescriptor        (496)
 │   │   │   ├─ BBacnetBinaryValueDescriptor        (422)
 │   │   │   └─ BBacnetBinaryWritableDescriptor (669, abstract)  implements BacnetWritableDescriptor
 │   │   │       ├─ BBacnetBinaryOutputDescriptor          (400)
 │   │   │       └─ BBacnetBinaryValuePrioritizedDescriptor (309)
 │   │   ├─ BBacnetMultiStatePointDescriptor   (1341, abstract)
 │   │   │   ├─ BBacnetMultiStateInputDescriptor     (502)
 │   │   │   ├─ BBacnetMultiStateValueDescriptor     (453)
 │   │   │   └─ BBacnetMultiStateWritableDescriptor (567, abstract) implements BacnetWritableDescriptor
 │   │   │       ├─ BBacnetMultiStateOutputDescriptor          (364)
 │   │   │       └─ BBacnetMultiStateValuePrioritizedDescriptor (339)
 │   │   └─ BBacnetCharacterStringDescriptor   (752)  implements BacnetWritableDescriptor
 │   ├─ BBacnetLoopDescriptor              (2530)  implements BIBacnetCovSource, BacnetPropertyListProvider
 │   ├─ BBacnetTrendLogDescriptor          (3018)  implements BacnetPropertyListProvider
 │   ├─ BBacnetNiagaraHistoryDescriptor    (2600)
 │   └─ BBacnetEventEnrollmentDescriptor   (4142)  implements BacnetPropertyListProvider
 │
 ├─ BBacnetScheduleDescriptor  (2980, abstract)  implements BIBacnetExportObject, BacnetPropertyListProvider
 │   ├─ BBacnetBooleanScheduleDescriptor (593) · BBacnetNumericScheduleDescriptor (414)
 │   ├─ BBacnetEnumScheduleDescriptor    (600) · BBacnetStringScheduleDescriptor  (417)
 │   └─ BBacnetDynamicScheduleDescriptor (320)
 │
 ├─ BBacnetCalendarDescriptor          (1601)  implements BIBacnetExportObject, …
 ├─ BBacnetFileDescriptor              (1460)  implements BIBacnetExportObject, …
 └─ BBacnetNotificationClassDescriptor (1940)  implements BIBacnetExportObject, …
```

Plus the local device itself (`BLocalBacnetDevice`, 4613 ln) and the support types
(`BOutOfServiceExt`, `BReliabilityAlarmSourceExt`, `BacnetPropertyList`, `BacnetDescriptorUtil`, `Cov`,
`BacnetCovSubscriber`, and the three interfaces).

Structural reading `[INFER]`:

- **`BBacnetEventSource` is the real root of the point world**, not `BBacnetPointDescriptor`. Loop, TrendLog,
  NiagaraHistory and EventEnrollment are siblings of the point descriptor, not children — they are export
  objects that can raise BACnet events but are not "points".
- The point branch is a clean **3×2 matrix**: {Analog, Binary, MultiState} × {read-only, Writable}, with the
  Writable half marked by the `BacnetWritableDescriptor` interface, plus `CharacterString` as a fourth
  datatype that skipped the matrix and implements `BacnetWritableDescriptor` directly.
- **"Prioritized" is not a separate layer** — `…ValuePrioritizedDescriptor` classes sit under
  `…WritableDescriptor` alongside the Output descriptors. Priority-array support is a property of the
  concrete leaf, not of a tier.
- Schedule, Calendar, File and NotificationClass **do not inherit from `BBacnetEventSource` at all** — they
  go straight from `BComponent`. They are export objects with no event/alarm surface.

---

## 275.2 — The contract: `BIBacnetExportObject` `[CERT]`

384 lines, mostly javadoc. The operative surface:

```java
BComplex getParent();          BObject getObject();
BOrd getObjectOrd();           void setObjectOrd(BOrd objectOrd, Context cx);
BStatus getStatus();           boolean isFatalFault();       void checkConfiguration();
BBacnetObjectIdentifier getObjectId();   void setObjectId(BBacnetObjectIdentifier objectId);
String getObjectName();                  void setObjectName(String objectName);
int[] getPropertyList();

PropertyValue   readProperty(PropertyReference propertyReference)        throws RejectException;
PropertyValue[] readPropertyMultiple(PropertyReference[] propertyReferences) throws RejectException;
RangeData       readRange(RangeReference rangeReference)                 throws RejectException;
ErrorType       writeProperty(PropertyValue propertyValue)               throws …;
ChangeListError addListElements(PropertyValue propertyValue);
ChangeListError removeListElements(PropertyValue propertyValue);

default void setTransportLayer(BBacnetTransportLayer transportLayer) {}
default boolean isDynamicallyCreated() …
```

So the whole BACnet server surface of an object is **six service methods**. Note the error-return
convention: reads return a `PropertyValue` that *carries* its own error (no exception for a BACnet-level
error), while writes return an `ErrorType` or `null` for success. `RejectException` is reserved for
malformed requests — the protocol-level Reject PDU, not an Error PDU. `[INFER]`

The interface also nests an `ObjectSubscriber` class (a Baja `Subscriber`) that mirrors
`BacnetCovSubscriber` of B272 §272.9 but for non-COV object changes — it watches `lastModified` among
others `[CERT]`.

---

## 275.3 — `readProperty`: a chain-of-responsibility by override `[CERT]`

Two-stage entry. The public method is `final` and just unpacks the reference:

```java
public final PropertyValue readProperty(PropertyReference ref) throws RejectException {
  ...
  return readProperty(ref.getPropertyId(), ref.getPropertyArrayIndex());
}
```

The protected two-arg form carries the dispatch, and its javadoc states the contract explicitly `[CERT]`:

> *"Subclasses with additional properties override this to check for their properties. If no match is
> found, call this superclass method to check these properties."*

```java
protected PropertyValue readProperty(int pId, int ndx)
{
  if (point == null)
    return new NReadPropertyResult(pId, ndx, new NErrorType(OBJECT, TARGET_NOT_CONFIGURED));

  if (ndx >= 0) {
    if (!isArray(pId))
      return new NReadPropertyResult(pId, ndx, new NErrorType(PROPERTY, PROPERTY_IS_NOT_AN_ARRAY));
  } else if (ndx < NOT_USED) {
      return new NReadPropertyResult(pId, ndx, new NErrorType(PROPERTY, INVALID_ARRAY_INDEX));
  }

  switch (pId) {
    case OBJECT_IDENTIFIER: return …AsnUtil.toAsnObjectId(getObjectId());
    case OBJECT_NAME:       return …AsnUtil.toAsnCharacterString(getObjectName());
    case OBJECT_TYPE:       return …AsnUtil.toAsnEnumerated(getObjectId().getObjectType());
    case PROPERTY_LIST:     return readPropertyList(ndx);            // ← the default method, §275.7
    case DESCRIPTION:       return …AsnUtil.toAsnCharacterString(getDescription());
    case STATUS_FLAGS:      return …AsnUtil.statusToAsnStatusFlags(getStatusFlags());
    case EVENT_STATE:       return readEventState();
    case RELIABILITY:       return …AsnUtil.toAsnEnumerated(getReliability());
    case OUT_OF_SERVICE:    return …AsnUtil.toAsnBoolean(getOosExt().getOutOfService());
    default:                return readOptionalProperty(pId, ndx);   // ← subclass hook
  }
}
```

Three findings:

1. **`TARGET_NOT_CONFIGURED` is the family-wide "descriptor with an unresolved point" error.** B273 §273.2
   found it as a COV-subscribe rejection; it is in fact the generic guard at the top of *every* read and
   write on the export. Class 1 (`OBJECT`), code 1000, Tridium-proprietary.
2. **The array-index guard is checked before the switch** and against a per-class `ARRAY_PROPS` table
   `[CERT]`:
   `{EVENT_TIME_STAMPS, EVENT_MESSAGE_TEXTS, EVENT_MESSAGE_TEXTS_CONFIG, PROPERTY_LIST, PRIORITY_ARRAY, STATE_TEXT}`.
   Anything else with `ndx >= 0` gets `PROPERTY_IS_NOT_AN_ARRAY`.
3. **`Event_State` is synthesised, not stored** `[CERT]`:

```java
private PropertyValue readEventState() {
  if (!getEventDetectionEnable())            return …BBacnetEventState.NORMAL;
  BAlarmSourceExt alarmExt = getAlarmExt();
  if (alarmExt == null)                      return …BBacnetEventState.NORMAL;   // "does not support event reporting"
  return …BBacnetEventState.fromBAlarmState(alarmExt.getAlarmState());
}
```

   So a Niagara point exported to BACnet reports `Event_State = NORMAL` whenever it has **no alarm
   extension**, regardless of its actual condition. The BACnet event state is a *projection of the Niagara
   alarm ext*, and absence of that ext reads as "normal" rather than as "unsupported". `[INFER]` on the
   operational consequence.

---

## 275.4 — `readPropertyMultiple`: the server side of `ALL` `[CERT]`

This is the exact counterpart of B271 §271.4, where the *client* was shown asking for property `ALL` (8):

```java
public final PropertyValue[] readPropertyMultiple(PropertyReference[] refs) throws RejectException
{
  getPoint();
  ArrayList<PropertyValue> results = new ArrayList<>(refs.length);
  for (int i = 0; i < refs.length; i++) {
    switch (refs[i].getPropertyId()) {
      case ALL:      for (int p : getRequiredProps()) results.add(readProperty(p, NOT_USED));
                     for (int p : getOptionalProps()) results.add(readProperty(p, NOT_USED));
                     break;
      case OPTIONAL: for (int p : getOptionalProps()) results.add(readProperty(p, NOT_USED));  break;
      case REQUIRED: for (int p : getRequiredProps()) results.add(readProperty(p, NOT_USED));  break;
      default:       results.add(readProperty(refs[i].getPropertyId(), refs[i].getPropertyArrayIndex()));
    }
  }
  return results.toArray(new PropertyValue[0]);
}
```

The loop closes: **B271's discovery job asks a remote device for `ALL`; this method is what a Niagara
station answers when it is the remote device.** All three special identifiers (`ALL`, `REQUIRED`,
`OPTIONAL`) are honoured, and every element is read with `NOT_USED` as the index — so an `ALL` read never
returns individual array elements, only whole arrays.

---

## 275.5 — `writeProperty`: a blacklist and four typed failures `[CERT]`

```java
protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException
{
  if (point == null) return new NErrorType(OBJECT, TARGET_NOT_CONFIGURED);
  … same array-index guards as read …

  try {
    switch (pId) {
      case OBJECT_IDENTIFIER: case OBJECT_NAME: case OBJECT_TYPE:
      case STATUS_FLAGS:      case EVENT_STATE: case PROPERTY_LIST:
      case RELIABILITY:
        return new NErrorType(PROPERTY, WRITE_ACCESS_DENIED);

      case DESCRIPTION:
        setString(description, AsnUtil.fromAsnCharacterString(val), BLocalBacnetDevice.getBacnetContext());
        return null;

      case OUT_OF_SERVICE:
        getOosExt().setBoolean(BOutOfServiceExt.outOfService, AsnUtil.fromOnlyAsnBoolean(val),
                               BLocalBacnetDevice.getBacnetContext());
        return null;

      default:
        return writeOptionalProperty(pId, ndx, val, pri);
    }
  }
  catch (AsnException e)        { … return new NErrorType(PROPERTY, INVALID_DATA_TYPE); }
  catch (PermissionException e) { … return new NErrorType(PROPERTY, WRITE_ACCESS_DENIED); }
}
```

Findings:

- **Seven properties are hard-refused with `WRITE_ACCESS_DENIED`** regardless of Niagara permissions:
  `Object_Identifier`, `Object_Name`, `Object_Type`, `Status_Flags`, `Event_State`, `Property_List`,
  `Reliability`. Note `Object_Name` is among them — **you cannot rename a Niagara export object over
  BACnet**, even though B271 §271.5 showed the *client* side treats `objectName` as writable on remote
  devices.
- Only **two** properties are writable at this tier: `Description` and `Out_Of_Service`. Everything else
  falls through to the subclass hook.
- **Writes execute under a dedicated context**: `BLocalBacnetDevice.getBacnetContext()`. That is what makes
  a `PermissionException` possible — Niagara's own permission model is enforced on a BACnet write, and the
  failure is mapped to `WRITE_ACCESS_DENIED`. So a remote BACnet client is subject to the station's
  security model, not exempt from it. `[CERT]` on the mapping; `[INFER]` that this is the mechanism by
  which BACnet writes are permission-checked.
- A decode failure maps to `INVALID_DATA_TYPE`, which is the correct BACnet error rather than an abort.

---

## 275.6 — `BacnetWritableDescriptor`: an empty marker `[CERT]`

Complete file, minus imports:

```java
/**
 * Marker interface so the UI can tell if a descriptor can be written from BACnet.
 * @author Joseph Chandler
 */
public interface BacnetWritableDescriptor
{
}
```

No methods. Added 2015 (copyright header), by a different author from the rest of the package (Craig
Gemmill). It carries **no runtime behaviour at all** — writability is implemented entirely by
`writeOptionalProperty` overrides in the `…WritableDescriptor` classes; this interface exists purely so
Workbench can render the distinction.

Worth stating because the name invites the opposite assumption: implementing it does not make anything
writable.

---

## 275.7 — `Property_List`: built dynamically, filtered on read `[CERT]`

Construction:

```java
@Override public int[] getPropertyList() {
  return BacnetPropertyList.makePropertyList(getRequiredProps(), getOptionalProps());
}

public int[] getRequiredProps() {
  if (requiredProps == null) {                       // cached
    Vector<BBacnetPropertyIdentifier> v = new Vector<>();
    v.add(objectIdentifier); v.add(objectName); v.add(objectType);
    addRequiredProps(v);                             // subclass hook
    …
  }
  return requiredProps;
}

public int[] getOptionalProps() {                    // NOT cached — rebuilt every call
  Vector<BBacnetPropertyIdentifier> v = new Vector<>();
  v.add(reliability); v.add(description);
  BAlarmSourceExt almExt = getAlarmExt();
  if (almExt != null) {
    v.add(timeDelay); v.add(notificationClass); v.add(eventEnable); v.add(ackedTransitions);
    v.add(notifyType); v.add(eventTimeStamps); v.add(eventMessageTexts);
    v.add(eventMessageTextsConfig); v.add(eventDetectionEnable);
  }
  addOptionalProps(v);                               // subclass hook
  …
}
```

Two behaviours that matter:

1. **The property list is conditional on Niagara configuration.** Adding an alarm extension to a Niagara
   point adds **nine** properties to the object's BACnet `Property_List`. A remote client that cached the
   list will be stale after an engineering change. `[CERT]`
2. **`requiredProps` is cached; `optionalProps` is not.** The asymmetry is real — every
   `getOptionalProps()` call allocates a `Vector`, walks it and builds a fresh `int[]`. Since
   `readPropertyMultiple(ALL)` calls it once per request and `getPropertyList()` calls it again, a
   `Read-Property-Multiple ALL` on an alarmed point rebuilds this list repeatedly. `[CERT]` on the code;
   `[INFER]` that it is a (minor) inefficiency rather than deliberate invalidation.

Serving is delegated to the `BacnetPropertyListProvider` **default method** `[CERT]`, which handles the
three BACnetARRAY access forms — whole array (`ndx == NOT_USED`), size (`ndx == 0`), single element — and
converts an out-of-range index into `INVALID_ARRAY_INDEX` rather than an exception.

The filter itself `[CERT]`:

```java
public static boolean shouldInclude(int propId) {
  for (int i = 0; i < requiredProps.length; i++)
    if (propId == requiredProps[i]) return false;
  return true;
}

/*
 * The Object_Name, Object_Type, Object_Identifier, and
 * Property_List properties are not included in the Property List
 */
private static final int[] requiredProps = new int[] {
  OBJECT_NAME, OBJECT_TYPE, OBJECT_IDENTIFIER,   // ← three, not four
};
```

Per ASHRAE 135, `Property_List` must exclude exactly those four. The array holds **three**.

---

## 275.8 — A suspicion investigated and discarded

The mismatch in §275.7 looked like a standards deviation: if `Property_List` (371) is not filtered, does the
object advertise `Property_List` inside its own `Property_List`?

**No. Verified, and there is no defect.** `[CERT]`

`getPropertyList()` is exactly `getRequiredProps() + getOptionalProps()`. Tracing both:
`getRequiredProps()` seeds with `objectIdentifier`, `objectName`, `objectType` and then calls
`addRequiredProps(v)`; `getOptionalProps()` seeds with `reliability`, `description` (plus the nine alarm
properties) and calls `addOptionalProps(v)`. **`PROPERTY_LIST` is never added to either.** It appears in
`BBacnetPointDescriptor` only inside `ARRAY_PROPS` — the table of *which properties are arrays* — which is
a different list serving a different purpose (§275.3).

So the filter never needs to exclude 371 because 371 never enters. The comment is simply broader than the
array it documents. Recording this because a comment/code mismatch in a standards-implementing filter is
exactly the shape of a real bug, and the honest result is that it is not one.

### A contained off-by-one

While verifying the above, `BacnetPropertyList.read()` does contain a genuine boundary error `[CERT]`:

```java
if (ndx < 1 || ndx > cleanProps.length + 1) return -1;
return cleanProps[ndx - 1];
```

`ndx == cleanProps.length + 1` passes the guard and indexes `cleanProps[cleanProps.length]` →
`ArrayIndexOutOfBoundsException`. The `+ 1` should not be there.

**It is unreachable from the only caller.** `BacnetPropertyListProvider.readPropertyList()` rejects
`ndx > BacnetPropertyList.size(propertyList)` *before* calling `read()`, and wraps the call in
`catch (Exception e) → getInvalidIdx(...)` anyway `[CERT]`. Two independent guards. Reported as latent
dead-end code, not as a live defect — same disposition as the `isSupported()` finding of B272 §272.8.

The same method also assumes the input contains exactly the three filtered properties:
`int[] cleanProps = new int[props.length - requiredProps.length];` — correct by construction here (§275.7
always seeds exactly those three), but it would overflow for any provider that omitted one. `[INFER]`

---

## 275.9 — `BOutOfServiceExt` `[CERT]`

```java
@NiagaraProperty(name = "outOfService",  type = "boolean", defaultValue = "false")
@NiagaraProperty(name = "presentValue",  type = "BValue",  defaultValue = "BBoolean.FALSE")
public class BOutOfServiceExt extends BPointExtension
```

206 lines. It is a **Niagara point extension**, not a descriptor member — it hangs off the exported
`BControlPoint` itself. That is why B271-adjacent code removes it via
`BBacnetPointDescriptor.removeOutOfServiceExt()` (`point.remove(outOfServiceExts[0])`) when an export is
torn down `[CERT]`.

The `presentValue` slot is the override value: BACnet `Out_Of_Service = TRUE` means "decouple the object
from its physical input and let clients write `Present_Value` directly", and this ext is where that
decoupled value lives. Writing `Out_Of_Service` over BACnet is one of only two properties permitted at the
base tier (§275.5).

---

## 275.10 — `BReliabilityAlarmSourceExt` `[CERT]`

796 lines, and its slot list shows what it is: a **full Niagara alarm source extension specialised for
BACnet reliability**:

```
alarmInhibit (BStatusBoolean false) · alarmState (BAlarmState.normal) · timeDelay (BRelTime, min 0)
alarmEnable (BAlarmTransitionBits.DEFAULT) · ackedTransitions (BAlarmTransitionBits.ALL)
notifyType (BBacnetNotifyType.alarm) · toFaultTimes (BAlarmTimestamps) · toFaultText / toNormalText (BFormat, multi-line)
```

This is the bridge that lets a BACnet `Reliability` transition raise a **Niagara alarm** with proper
to-fault/to-normal texts and transition bits — i.e. the inbound-alarm counterpart of the
`bacnetAlarmRouter` documented in B34. `notifyType` defaults to `alarm` (not `event`), so reliability
faults are reported as alarms by default.

---

## 275.11 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| Five independent roots under BComponent | `extends`/`implements` extracted from all 50 files | `[CERT]` |
| `BBacnetEventSource` is the point root | `abstract public class BBacnetPointDescriptor extends BBacnetEventSource` | `[CERT]` |
| Writable tier marked by an empty interface | `BacnetWritableDescriptor` full file, zero members | `[CERT]` |
| read dispatch is override-chain | javadoc: *"Subclasses … override this … call this superclass method"* + `default: readOptionalProperty` | `[CERT]` |
| `TARGET_NOT_CONFIGURED` guards every read and write | identical `if (point == null)` at the top of both | `[CERT]` |
| RPM honours ALL / REQUIRED / OPTIONAL | the three-case switch, verbatim | `[CERT]` |
| 7 properties hard-refuse writes | the seven fall-through `case`s → `WRITE_ACCESS_DENIED` | `[CERT]` |
| BACnet writes run under a Niagara context and can raise PermissionException | `BLocalBacnetDevice.getBacnetContext()` + `catch (PermissionException)` | `[CERT]` |
| Property_List grows by 9 when an alarm ext exists | `if (almExt != null)` block in `getOptionalProps()` | `[CERT]` |
| `requiredProps` cached, `optionalProps` not | `if (requiredProps == null)` vs unconditional rebuild | `[CERT]` |
| Property_List does NOT self-include | traced both seed lists; 371 never added; appears only in `ARRAY_PROPS` | `[CERT]` (negative) |
| `read()` off-by-one is unreachable | provider validates `ndx > size()` first **and** wraps in try/catch | `[CERT]` |
| `Event_State` NORMAL when no alarm ext | `if (alarmExt == null) return …NORMAL` | `[CERT]` |
| ⇒ an unalarmed export always reads NORMAL | composed | `[INFER]` |
| optionalProps rebuild is inefficiency, not invalidation | no observed mutation between calls | `[INFER]` |

Tally: **[CERT] 12 / [INFER] 3.**

---

## 275.12 — Connections and what remains of B271-G1

- **B271** — the client-side mirror. §275.4 is the server answering the `ALL` request of B271 §271.4.
- **B272 / B273 / B274** — the COV surface of this same family, deliberately not repeated here.
- **B34** — alarm framework; §275.10's `BReliabilityAlarmSourceExt` is its BACnet-facing sibling.
- **B23** — object model and property identifiers, which this package implements on the server side.

### B271-G1 remaining scope (this block closed roughly the core third)

| ID | Gap | Class |
|---|---|---|
| **B275-G1** | `BacnetDescriptorUtil` (924 ln) — the shared utility; untouched. | STATIC-investigable |
| **B275-G2** | The specialised descriptors: `BBacnetScheduleDescriptor` (2980) + its 5 subclasses, `BBacnetCalendarDescriptor` (1601), `BBacnetFileDescriptor` (1460), `BBacnetNotificationClassDescriptor` (1940). | STATIC-investigable |
| **B275-G3** | `BBacnetTrendLogDescriptor` (3018) + `BBacnetNiagaraHistoryDescriptor` (2600) + `BBacnetEventEnrollmentDescriptor` (4142) — the three largest classes in the package. | STATIC-investigable |
| **B275-G4** | `readRange` / `addListElements` / `removeListElements` — three of the six contract methods were not traced. | STATIC-investigable |
| **B275-G5** | `writeOptionalProperty` in the Prioritized/Writable leaves — how a BACnet priority write maps onto the Niagara priority array (the inverse of B271 §271.11). | STATIC-investigable |
| **P3-mstp** | MS/TP framing. Open since B133. **Next goal after B271-G1.** | STATIC-investigable |
| **P3-sc** | BACnet/SC transport. Open since B133. | STATIC-investigable |
