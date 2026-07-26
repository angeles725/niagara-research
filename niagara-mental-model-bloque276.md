# Block 276 — The BACnet export descriptor family (II): the priority-array write path via dynamic links, the read/write asymmetry that gates commandability, and `BacnetDescriptorUtil` — a third, divergent type-decision algorithm

> Second pass against **B271-G1**. Closes **B275-G4** and **B275-G5**, and opens `BacnetDescriptorUtil`
> (**B275-G1**). The headline is the inverse of [B271] §271.11: how a `WriteProperty` with a BACnet priority
> arriving from a remote client lands in a Niagara `BNumericWritable`'s priority array — and it is not a
> write at all, it is a **link**.
>
> Second finding, unplanned: `BacnetDescriptorUtil` contains a **third implementation** of the
> ASN-type → point-type decision documented in B271 §271.6, and it **disagrees** with the other two in
> three cases.
>
> **Sources**: original Tridium source, `…/bacnet-rt/javax/baja/bacnet/export/`. Markers: `[CERT]` verbatim ·
> `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 276.1 — BACnet priority → Niagara priority array: a link, not a write `[CERT]`

Niagara's `BNumericWritable` already *has* a 16-level priority array: its `in1`…`in16` slots. The export
descriptor does not translate or mirror it — it **wires itself into it**.

`BBacnetAnalogWritableDescriptor` builds the wiring dynamically:

```java
StringTokenizer st = new StringTokenizer(s, ",");            // s = the "bacnetWritable" slot
while (st.hasMoreTokens())
{
  String tgtSlotName = st.nextToken();                       // e.g. "in10"
  Slot tgtSlot = pt.getSlot(tgtSlotName);
  BValue value = pt.get(tgtSlotName).newCopy();
  String srcSlotName = "bacnetValue" + TextUtil.capitalize(tgtSlotName);   // "bacnetValueIn10"
  BStatusNumeric sf = new BStatusNumeric();
  sf.setStatusNull(true);
  add(srcSlotName, sf, Flags.OPERATOR | Flags.READONLY);     // slot ON THE DESCRIPTOR
  pt.setFlags(tgtSlot, pt.getFlags(tgtSlot) | Flags.FAN_IN);
  BLink link = new BLink(getHandleOrd(), srcSlotName, tgtSlotName, true);
  pt.add("bacnet" + tgtSlotName, link, Flags.READONLY);      // link INTO the point
  pt.set(tgtSlotName, value);
}
```

The resulting topology:

```
   remote BACnet client
        │  WriteProperty(Present_Value, value, priority = 10)
        ▼
   BBacnetAnalogOutputDescriptor
        │  writePriorityArray(10, val)
        ▼
   slot  bacnetValueIn10        (BStatusNumeric, OPERATOR|READONLY, on the descriptor)
        │  BLink  (FAN_IN on the target)
        ▼
   slot  in10                   (on the Niagara BNumericWritable)
        │  Niagara's own priority resolution
        ▼
   out
```

So **BACnet priority level N and Niagara priority slot `inN` are the same thing**, joined by a real
`BLink`. There is no shadow copy, no synchronisation, no drift. `[INFER]` on "no drift"; the wiring is
`[CERT]`.

The `bacnetWritable` slot that drives this is itself derived, not authored `[CERT]`:

```java
private void resetBacnetWritable() {
  StringBuilder sb = new StringBuilder();
  Knob[] knobs = getKnobs();
  for (int i = 0; i < knobs.length; i++) {
    BObject tgt = knobs[i].getTargetOrd().get(this);
    if (knobs[i].getTargetSlotName().startsWith("in") && tgt == getPoint())
      sb.append(knobs[i].getTargetSlotName()).append(',');
  }
  setBacnetWritable((sb.length() > 0) ? sb.substring(0, sb.length() - 1) : lexNotWritable);
}
```

It is rebuilt from the descriptor's actual **knobs** (outgoing links) on `knobAdded`/`knobRemoved`. The slot
is `Flags.READONLY | Flags.HIDDEN` — a cache of link state, not a setting. `[CERT]`

Type gate `[CERT]`: `isPointTypeLegal(pt) → pt instanceof BNumericWritable`, with the javadoc
*"BBacnetAnalogWritableDescriptors may only expose BNumericWritables"*. And
`isCommandable() → true`, whose javadoc cites *"the Clause 19 prioritization procedure"* of ASHRAE 135.

---

## 276.2 — `writePriorityArray`, step by step `[CERT]`

```java
private ErrorType writePriorityArray(int pri, byte[] val) throws BacnetException
{
  BNumericWritable pt = (BNumericWritable)getPoint();
  if (pt == null) return new NErrorType(OBJECT, TARGET_NOT_CONFIGURED);

  if (pri == NOT_USED) pri = 16;                                          // (1)
  if ((pri < 1) || (pri > 16))
    return new NErrorType(SERVICES, PARAMETER_OUT_OF_RANGE);              // (2)

  String inSlotName = "bacnetValueIn" + pri;
  Property inSlot = loadSlots().getProperty(inSlotName);
  if (inSlot == null)                                                     // (3)
    return new NErrorType(PROPERTY, WRITE_ACCESS_DENIED);

  BStatusNumeric bacval = (BStatusNumeric)get(inSlot).newCopy();
  BNumber nmin = (BNumber)pt.getFacets().getFacet(BFacets.MIN);           // (4)
  BNumber nmax = (BNumber)pt.getFacets().getFacet(BFacets.MAX);
  double min = (nmin != null) ? nmin.getDouble() : Double.NEGATIVE_INFINITY;
  double max = (nmax != null) ? nmax.getDouble() : Double.POSITIVE_INFINITY;

  synchronized (asnIn) {
    asnIn.setBuffer(val);
    int tag = asnIn.peekTag();
    if (tag == ASN_NULL) {                                                // (5) relinquish
      if (getOosExt().getOutOfService())
        return new NErrorType(PROPERTY, VALUE_OUT_OF_RANGE);
      else bacval.setStatusNull(true);
    }
    else if (tag == asnType()) {
      double real = readFromAsn(asnIn);
      if (real < min || real > max)
        return new NErrorType(PROPERTY, VALUE_OUT_OF_RANGE);              // (4)
      if (getOosExt().getOutOfService())                                  // (6)
        getOosExt().set(BOutOfServiceExt.presentValue, BDouble.make(real), getBacnetContext());
      bacval.setStatusNull(false);
      bacval.setValue(real);
    }
    else throw new AsnException(E_BACNET_ASN_INVALID_TAG + tag);          // (7)
  }

  set(inSlot, bacval, BLocalBacnetDevice.getBacnetContext());             // (8)
  return null;
}
catch (IllegalArgumentException e) { … return new NErrorType(PROPERTY, VALUE_OUT_OF_RANGE); }
catch (PermissionException e)      { … return new NErrorType(PROPERTY, WRITE_ACCESS_DENIED); }
```

1. **No priority means 16**, per ASHRAE (lowest priority / default level).
2. Out-of-range priority is a **SERVICES** class error (`PARAMETER_OUT_OF_RANGE`), not a PROPERTY one.
3. **Missing slot = `WRITE_ACCESS_DENIED`.** This is the whole access-control story — see §276.4.
4. The value is validated against the Niagara point's **`min`/`max` facets**, absent facets meaning
   ±infinity. A BACnet client cannot push a value outside the engineered range.
5. `ASN_NULL` is the relinquish, and it is **refused while `Out_Of_Service` is true** — you cannot
   relinquish a level on an out-of-service object.
6. When `Out_Of_Service` is true, the write **also** lands on `BOutOfServiceExt.presentValue` — the
   decoupled override value of B275 §275.9.
7. A tag that is neither NULL nor the descriptor's own ASN type throws, and the parent's handler maps it to
   `INVALID_DATA_TYPE` (B275 §275.5).
8. The actual store is a **`set()` on the descriptor's own slot** — the link propagates it to the point.
   And because it runs under `getBacnetContext()`, Niagara permissions apply, with `PermissionException`
   mapped to `WRITE_ACCESS_DENIED`.

---

## 276.3 — `readPriorityArray`: straight from the Niagara point `[CERT]`

```java
protected PropertyValue readPriorityArray(int ndx)
{
  BNumericWritable pt = (BNumericWritable)getPoint();
  if (pt == null) return …(OBJECT, TARGET_NOT_CONFIGURED);

  if (ndx == NOT_USED) {                       // whole array
    for (int i = 1; i <= 16; i++) {
      BStatusNumeric e = pt.getLevel(BPriorityLevel.make(i));
      if (e.getStatus().isNull()) asnOut.writeNull();
      else                        appendToAsn(asnOut, e.getValue());
    }
    return …asnOut.toByteArray();
  }
  else if (ndx == 0)  return …AsnUtil.toAsnUnsigned(16);          // array size
  else {                                                          // single element
    BStatusNumeric e = pt.getLevel(BPriorityLevel.make(ndx));
    return e.getStatus().isNull() ? …toAsnNull() : …convertToAsn(e.getValue());
  }
}
```

The read goes **directly to `pt.getLevel(BPriorityLevel.make(i))`** — the Niagara point's own accessor. No
descriptor-side state is consulted. A Niagara null-status level encodes as BACnet NULL, which is the correct
"nobody is commanding this level" representation.

---

## 276.4 — The asymmetry: everything readable, selectively writable

Putting §276.2(3) and §276.3 side by side gives the most operationally useful result in this block:

| Direction | Mechanism | Coverage |
|---|---|---|
| **Read** priority array | `pt.getLevel(i)` for i = 1…16 | **all 16 levels, always** |
| **Write** priority level N | requires slot `bacnetValueInN` to exist | **only the levels an engineer linked** |

So a remote BACnet client can always *see* the full priority array of an exported writable point, but can
only *command* the levels for which a `bacnetValueInN` slot — i.e. a link — was created. Every other level
returns `WRITE_ACCESS_DENIED`.

That is a **per-priority-level access control**, and it is configured by wiring rather than by a permission
setting. Two consequences `[INFER]`:

- The natural pattern is to expose exactly one level (say `in16` for a BAS operator, or `in8` for manual
  override) and leave emergency levels 1–2 unwritable from BACnet while still visible.
- A commissioning engineer who exports a writable point and forgets to link anything gets a point that
  reads fine and rejects every write with `WRITE_ACCESS_DENIED` — and the error says nothing about links.
  `bacnetWritable` shows `lexNotWritable`, but that slot is `HIDDEN`.

---

## 276.5 — B275-G4 CLOSED: `readRange` / list elements are stubs at the point tier `[CERT]`

```java
public RangeData readRange(RangeReference rangeReference) throws RejectException {
  getPoint();
  if (!hasProperty(rangeReference.getPropertyId())) return new ReadRangeAck(property, unknownProperty);
  return new ReadRangeAck(services, propertyIsNotA_List);
}

public ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
  getPoint();
  if (!hasProperty(propertyValue.getPropertyId())) return makeAddListElementError(property, unknownProperty);
  return makeAddListElementError(services, propertyIsNotA_List);
}
```

`removeListElements` is the mirror. All three do exactly two things: distinguish *"you asked for a property
I don't have"* (`unknownProperty`) from *"I have it but it isn't a list"* (`propertyIsNotA_List`), and then
refuse.

This is correct and complete for point objects — a point has no list-valued property. The real
implementations live on `BLocalBacnetDevice` (five list properties, ReadRange-able, documented in
B274 §274.3) and on the specialised descriptors. **Nothing was missing here**; the gap resolves to a
negative finding.

---

## 276.6 — B275-G1: `BacnetDescriptorUtil` is not a descriptor utility `[CERT]`

924 lines. The name suggests helpers for descriptors. The actual surface:

```java
static BControlPoint findOrAddPoint(BBacnetDeviceObjectPropertyReference objectPropRef)
static BControlPoint findOrAddLocalPoint(...)      static BControlPoint findOrAddRemotePoint(...)
static BComponent    findLocalObject(BBacnetObjectIdentifier objectId)
private static BControlPoint addPropertyPoint(...) private static BControlPoint makePropertyPoint(...)
private static BControlPoint makePointForPropertyInfo(int objectType, PropertyInfo propInfo)
private static BBacnetDevice findOrAddRemoteDevice(...)  private static BBacnetDevice addRemoteDevice(int)
private static void removePoint(BControlPoint point)
private static BBooleanWritable makeBacnetBooleanWritable()   // + Numeric / Enum / String
private static BDiscreteTotalizerExt addDiscreteTotalizerExtToPoint(BComponent point)
static boolean areTrendLogAndPointCompatible(...)
```

It is an **automatic point factory**. When a descriptor holds a
`BBacnetDeviceObjectPropertyReference` — an Event Enrollment pointing at an object property, a Trend Log
referencing a point that has not been proxied yet — this class **creates the Niagara point on demand**,
including creating the `BBacnetDevice` for a remote reference if needed (`addRemoteDevice`).

That is a significant capability the corpus had not recorded: parts of the BACnet subsystem **mutate the
station's component tree by themselves**, adding points and devices as a side effect of resolving a
reference. `[INFER]` on the significance; the methods are `[CERT]`.

---

## 276.7 — A third type-decision algorithm, and it disagrees `[CERT]`

`makePointForPropertyInfo` reimplements the ASN-type → point-type decision of B271 §271.6:

```java
switch (propInfo.getAsnType()) {
  case ASN_NULL:              return makeBacnetStringWritable();
  case ASN_BOOLEAN:           return makeBacnetBooleanWritable();
  case ASN_UNSIGNED:          return isMultiStatePresentValue(propInfo.getId(), objectType)
                                     ? makeBacnetEnumWritable() : makeBacnetNumericWritable();
  case ASN_INTEGER: case ASN_REAL: case ASN_DOUBLE:
                              return makeBacnetNumericWritable();
  case ASN_OCTET_STRING: case ASN_CHARACTER_STRING: case ASN_BIT_STRING:
                              return makeBacnetStringWritable();
  case ASN_ENUMERATED:        return propInfo.getType().equals("bacnet:BacnetBinaryPv")
                                     ? makeBacnetBooleanWritable() : makeBacnetEnumWritable();
  case ASN_DATE: case ASN_TIME: case ASN_OBJECT_IDENTIFIER:
  case ASN_CONSTRUCTED_DATA: case ASN_BACNET_ARRAY: case ASN_BACNET_LIST:
  case ASN_ANY: case ASN_CHOICE: case ASN_UNKNOWN_PROPRIETARY:
                              return makeBacnetStringWritable();
  default:                    throw new BacnetException("BACnet property type … is not supported…");
}
```

Compared against `PointLearn.toTypes()` (B271 §271.6), which is the Workbench learn path:

| ASN type | `PointLearn.toTypes()` (bacnet-**wb**) | `makePointForPropertyInfo()` (bacnet-**rt**) | Agree? |
|---|---|---|---|
| 0 NULL | **`return null`** — not addable | **`BStringWritable`** | **NO** |
| 1 BOOLEAN | Boolean | Boolean | yes |
| 2 Unsigned | multiState ? Enum : Numeric | multiState ? Enum : Numeric | yes |
| 3 INTEGER | Numeric (then Enum, Boolean) | Numeric | yes (default matches) |
| 4/5 REAL/Double | Numeric only | Numeric | yes |
| 6/7/8 | String (appended last) | String | yes |
| 9 ENUMERATED | isBinaryPv ? Boolean : Enum | isBinaryPv ? Boolean : Enum | yes |
| -1 CONSTRUCTED | **`return null`** | **String** | **NO** |
| -3 LIST | **`return null`** | **String** | **NO** |
| -6 UNKNOWN_PROPRIETARY | String | String | yes |

Three real divergences, all of the same shape: **the learn path refuses structurally non-point-able types;
the auto-creation path stuffs them into a `BStringWritable`.** So a property that a human cannot add through
the Point Manager can still be materialised as a point by a descriptor resolving a reference.

A fourth, systematic difference: **`makePointForPropertyInfo` always produces the Writable variant** —
`BNumericWritable`, never `BNumericPoint`. It has no equivalent of the `writableFirst` object-type test of
B271 §271.7. Auto-created points are unconditionally writable.

And the shared predicate is **literally duplicated**, in two modules `[CERT]`:

```java
// BacnetDescriptorUtil (bacnet-rt)
private static boolean isMultiStatePresentValue(int propertyId, int objectType) {
  return propertyId == PRESENT_VALUE &&
         (objectType == MULTI_STATE_INPUT || objectType == MULTI_STATE_OUTPUT || objectType == MULTI_STATE_VALUE);
}

// PointLearn (bacnet-wb) — B271 §271.6
static boolean isMultiStatePresentValue(int propertyId, int objectType) {
  return propertyId == 85 && (objectType == 13 || objectType == 14 || objectType == 19);
}
```

Same logic, one written with named constants and one with magic numbers, in different jars. `isBinaryPv` is
likewise duplicated (a helper in one, an inline `.equals("bacnet:BacnetBinaryPv")` in the other). Two copies
that currently agree — but nothing keeps them in step.

**This refines B271 §271.6**, which presented the learn-path switch as *the* type decision. It is the type
decision for the Workbench learn; the runtime auto-creation path has its own, and they differ at the edges.

---

## 276.8 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| BACnet priority N ↔ Niagara `inN` via BLink | the `bacnetValueIn` + `new BLink(...)` + `FAN_IN` construction loop | `[CERT]` |
| `bacnetWritable` is derived from knobs | `resetBacnetWritable()` walks `getKnobs()`; slot is READONLY\|HIDDEN | `[CERT]` |
| No priority → 16 | `if (pri == NOT_USED) pri = 16;` | `[CERT]` |
| Priority out of range → SERVICES/PARAMETER_OUT_OF_RANGE | verbatim | `[CERT]` |
| Missing slot → WRITE_ACCESS_DENIED | `if (inSlot == null) return …WRITE_ACCESS_DENIED` | `[CERT]` |
| Value clamped to point's min/max facets | `BFacets.MIN`/`MAX` → `VALUE_OUT_OF_RANGE` | `[CERT]` |
| Relinquish refused while Out_Of_Service | `if (tag == ASN_NULL) { if (oos) return VALUE_OUT_OF_RANGE; }` | `[CERT]` |
| Read uses the point's own levels | `pt.getLevel(BPriorityLevel.make(i))` | `[CERT]` |
| ⇒ read is total, write is selective | composition of the two | `[INFER]` |
| `readRange`/list elements are refusing stubs at point tier | all three return unknownProperty or propertyIsNotA_List | `[CERT]` |
| `BacnetDescriptorUtil` is a point factory | `findOrAddPoint`, `addRemoteDevice`, `makeBacnet*Writable` | `[CERT]` |
| Third type algorithm diverges in 3 cases | the two switches side by side | `[CERT]` |
| Auto-created points are always Writable | all four factory methods return `B*Writable` | `[CERT]` |
| `isMultiStatePresentValue` duplicated across jars | both bodies quoted | `[CERT]` |
| Auto-creation mutating the station tree is significant | derived from the factory's reach | `[INFER]` |

Tally: **[CERT] 13 / [INFER] 2.**

---

## 276.9 — Corrections

| Target | Was | Is |
|---|---|---|
| **B271 §271.6** | presented `PointLearn.toTypes()` as *the* ASN-type → point-type decision | It is the **Workbench learn** decision. `BacnetDescriptorUtil.makePointForPropertyInfo()` is a second, runtime decision that **disagrees for ASN 0 / -1 / -3** and always produces Writable variants. §276.7 |

---

## 276.x — Connections and remaining scope

- **B271 §271.11** — the client-side write-with-priority. §276.1-276.2 is its exact inverse. The pair now
  documents both directions of Niagara↔BACnet priority mapping.
- **B275** — part I of this family. G4 and G5 closed here; G1 opened.
- **B274 §274.3** — the device-level list properties that §276.5's stubs delegate to.

### B271-G1 remaining

| ID | Gap | Class |
|---|---|---|
| ~~**B276-G1**~~ | ~~`BacnetDescriptorUtil`'s remaining half~~ — **CLOSED by [B288] §288.2.** `Elapsed_Active_Time` attaches a `BDiscreteTotalizerExt` to the monitored point (reusing an existing one), **forces its update interval to 1000 ms**, and creates a *second* numeric point to carry the value. Only `areTrendLogAndPointCompatible` remains (B288-G1). | closed |
| **B275-G2** | Schedule (2980) + 5 subclasses, Calendar (1601), File (1460), NotificationClass (1940). | STATIC-investigable |
| **B275-G3** | TrendLog (3018), NiagaraHistory (2600), EventEnrollment (4142) — the three largest. | STATIC-investigable |
| ~~**B276-G2**~~ | ~~Binary/MultiState assumed parallel to Analog~~ — **VERIFIED by [B288] §288.1.** The assumption holds: identical `bacnetWritable` slot, identical `bacnetValueIn`+pri naming, identical four guards (NOT_USED→16 · 1..16 · unlinked→`WRITE_ACCESS_DENIED` · ASN_NULL refused under Out_Of_Service). They differ only in **value carrier and domain check**: Binary takes `ASN_ENUMERATED` restricted to `{0,1}`; MultiState takes `ASN_UNSIGNED` restricted to `> 0` (Spec Rev 14, cited in-source) *and* to the point's `BEnumRange`. | closed |
| **P3-mstp** | MS/TP framing. Open since B133. | STATIC-investigable |
| **P3-sc** | BACnet/SC transport. Open since B133. | STATIC-investigable |
