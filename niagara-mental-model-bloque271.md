# Block 271 — BACnet discovery-to-point pipeline: how N4 finds devices, mines objects, decides Numeric/Boolean/Enum/String, derives facets, and separates read-only from writable

> Research of the **BACnet integration pipeline** of the Niagara N4.14 driver, from the first broadcast on
> the wire down to the `BControlPoint` that lands in the station database. This is the layer B23 named but
> never opened: B23 §23.4 lists the four `BBacnetProxyExt` subclasses and B23 §23.5 lists the Who-Is/I-Am
> PDU shapes, but neither documents **the decision algorithms** — which point class gets created and why,
> where units/min/max/stateText come from, and how "read-only vs writable" is actually decided. B133 covers
> the bytes below this layer (APDU/ASN.1/segmentation); this block covers the semantics above it.
>
> **Sources** (priority order per project protocol):
> - **(a) Original Tridium source, not decompiled** — `organized/docSource/docSource-doc/extracted/bacnet-rt/javax/baja/bacnet/**` (256 `.java` with real javadoc)
> - **(b) Vineflower decompile** — `bacnet-rt` / `bacnet-wb` (`com.tridium.bacnet.*`) via module-navigator
> - **(c) Packaged resource** — `com/tridium/bacnet/objectTypes.xml` (1 658 lines) and `vendors.xml`, extracted verbatim from `modules/bacnet-rt.jar`
>
> Markers: `[CERT]` = read verbatim in source/resource. `[INFER]` = derived. `[EXT-KB]` = external industry
> knowledge, not verifiable in this corpus.
>
> **Corpus language: ENGLISH** (convention since B115).

---

## 271.0 — Audit verdict on the existing BACnet blocks

The user asked whether B23 / B133 / B28 / B120 / B32 / B34 / B77 / B79 / B246 are still current. Verdict:

| Block | Status | Note |
|---|---|---|
| **B23** | **Accurate, incomplete** | Object model, property IDs, PDU shapes and the `BBacnetProxyExt` hierarchy all check out. §23.5 `WhoIs = 0x08` is **correct**. What is missing is every *algorithm* in this block. |
| **B133** | **Accurate** | Wire/APDU layer. No delta found. Sits cleanly below this block. |
| **B28** | **3 factual errors + 3 broken cross-refs** | See §271.15. The 10-step flow is directionally right but wrong in the details that matter operationally. |
| **B120 / B77** | **Accurate** | Spyder download/upload transport. Orthogonal to this block (AtomicWriteFile, not point discovery). |
| **B32 / B34** | **Accurate, tangential** | B32 is enterprise modules/licensing; B34 is the alarm framework. Neither is a BACnet-pipeline block — they only intersect it (`bacnetAlarmRouter` in B34.x). |
| **B79 / B246** | **Accurate** | Honeywell BACnet utility layer, sits on top of the standard driver. |

Net: **no block is wrong about BACnet's protocol**; B28 is wrong about the *driver's* discovery behaviour,
and the whole corpus has a hole exactly where the user was asking.

---

## 271.1 — The pipeline in one view `[CERT]`

```
 WIRE                    JOB LAYER                    TYPE DECISION            BOG
────────────────────────────────────────────────────────────────────────────────────────
 Who-Is  ───────────►  BBacnetDiscoverDevicesJob  ──►  (no type work)     ──►  BBacnetDevice
 I-Am    ◄───────────       (IAmListener)
 ReadProperty(objectList=76)
         ───────────►  BBacnetDiscoverPointsJob   ──┐
 ReadPropertyMultiple(oid, ALL=8)                   │
         ───────────►     BDiscoveryPoint[]         │
                                                    ▼
                                     objectTypes.xml ──► PropertyInfo.asnType
                                                    │
                                     PointLearn.toTypes(asnType, objectType, propId)
                                                    │
                                                    ▼
                              MgrTypeInfo[]  (ordered: default = element 0)
                                                    │
                              BBacnetPointManager.Model.newInstance()
                                                    ▼
                                BNumericPoint + BBacnetNumericProxyExt
                                BBooleanPoint + BBacnetBooleanProxyExt
                                BEnumPoint    + BBacnetEnumProxyExt
                                BStringPoint  + BBacnetStringProxyExt
```

Three independent discovery jobs exist, all subclassing `com.tridium.bacnet.job.BBacnetDiscoverJob`:
`BBacnetDiscoverDevicesJob` (401 lines), `BBacnetDiscoverPointsJob` (462), plus siblings
`BBacnetDiscoverSchedulesJob` (297), `BBacnetDiscoverTrendLogsJob` (198), `BBacnetDiscoverConfigJob` (59).
`[CERT]` module-navigator class index, `bacnet-rt`.

---

## 271.2 — Device discovery: the real Who-Is / I-Am algorithm `[CERT]`

`com.tridium.bacnet.job.BBacnetDiscoverDevicesJob.run(Context)`, verbatim structure:

```java
this.server().registerIAmListener(this);              // 1. become a listener FIRST
BDiscoveryNetworks networks = this.params.getNetworks();

if (networks.isAllNetworks()) {                        // 2a. global path
   BBacnetAddress addr = BBacnetAddress.GLOBAL_BROADCAST_ADDRESS;
   if (this.params.isDefaultRange())  this.client().whoIs(addr);
   else                               this.client().whoIs(addr, lowLimit, highLimit);
} else {                                               // 2b. per-network path
   for (int i = 0; i < nets.length; i++) {
      BBacnetAddress addr = new BBacnetAddress(nets[i], (BBacnetOctetString)null);
      ... same low/high branch ...
   }
}
```

Service choice, verified against the constant table — **not** the PDU type:

| Constant | Value | Where |
|---|---|---|
| `BacnetUnconfirmedServiceChoice.WHO_IS` | **8** | `bacnet-rt` `[CERT]` |
| `BBacnetServicesSupported.WHO_IS` | **34** | bit position in the services-supported bitstring `[CERT]` |
| `UNCONFIRMED_REQUEST` (APDU **type** nibble) | **0x10** | B23 §23.32 |

Those are three different numbers for three different things. B28 conflated the first and the third
(§271.15 delta #1).

### The collect loop is NOT a sleep

This is the operationally important correction. The code does **not** wait out the timeout and then mine.
It interleaves both in a single loop `[CERT]`:

```java
BAbsTime end = start.add(BRelTime.makeSeconds(params.getWaitResponseTime()));
while ((now.isBefore(end) || this.count < iAmSize) && this.isAlive()) {
   if (this.count == iAmSize) {
      Thread.sleep(500L);                    // nothing pending → idle 500 ms
   } else {
      iAmDev = this.iAmDevices.get(this.count);       // one I-Am off the buffer
      int deviceId = iAmDev.iAm.getObjectId().getInstanceNumber();
      if (params.isDefaultRange() || (deviceId >= low && deviceId <= high)) {
         BDiscoveryDevice dd = this.discoverDevice(iAmDev);   // ReadProperty mining, NOW
         if (iAmDev.dup) { dd.setDuplicate(true); dups.add(...); }
         this.add(null, dd);
      }
      this.count++;
   }
   ... progress ...
}
```

Consequences that matter in the field:

1. **Mining starts on the first I-Am**, not after the timeout. A slow device answering at t=29 s still gets
   mined, because the loop condition is `now.isBefore(end) || count < iAmSize` — the job outlives its own
   timeout until the backlog is drained.
2. **The device-instance range is filtered twice**: once on the wire (Who-Is low/high limits) and once in
   the loop. A device that ignores the range limits and answers anyway is still discarded locally.
3. **Progress is the max of two estimators** — time-based and device-count-based — and never moves
   backwards (`if (timeProgress > oldProgress)`). That is why the Workbench progress bar on a big network
   stalls and then jumps.
4. **Duplicate detection is two-pass**: `receiveIAm` flags the dup at insert time, then after the loop a
   second pass over `dups` re-marks *every* `BDiscoveryDevice` sharing that object-id, so the UI shows all
   siblings of a collision, not just the second one. `[CERT]`

---

## 271.3 — Object discovery: object-list mining `[CERT]`

`BBacnetDiscoverJob` (the shared parent, 648 lines) reads the device object's `objectList` (property **76**):

```java
this.deviceObject.readProperty(BBacnetDeviceObject.objectList);
...
while (c.next(BBacnetObjectIdentifier.class)) { ... }
```

`ObjectTypeList` declares the minimum probe set for anything it does not recognise:

```java
private static int[] BASIC_PROPS = new int[]{75, 77, 79};   // objectIdentifier, objectName, objectType
```

`[CERT]` `com.tridium.bacnet.ObjectTypeList:23`.

The mining path then branches on RPM support (§271.4) and, for the RPM path, walks the object list in
**windows** (`for (int i = next; i < lastP1; i++)` with a `do { } while (lastP1 <= a.size())` outer loop),
retrying with `it.previous()` on failure — i.e. a rewind-and-shrink strategy rather than a fixed chunk size.
`[CERT]` `BBacnetDiscoverJob:421-543`.

---

## 271.4 — Property discovery: `ALL` first, required-properties as fallback `[CERT]`

This is delta #3 vs B28. `BBacnetDiscoverPointsJob.discoverProperties()` verbatim:

```java
if (!this.device().isServiceSupported("readPropertyMultiple")) {
   return this.buildPropertyChildren(objectName, objectId,
                                     this.device().getRequiredProperties(objectId), facetMap);
}
Vector specs = new Vector();
specs.add(new NReadAccessSpec(objectId, 8));            // 8 == special property identifier ALL
Vector vals = client().readPropertyMultiple(this.device().getAddress(), specs);
...
while (it.hasNext()) {
   NReadPropertyResult rpr = (NReadPropertyResult)it.next();
   if (rpr.getPropertyId() == 8) {                      // device errored on ALL
      return this.buildPropertyChildren(..., this.device().getRequiredProperties(objectId), facetMap);
   }
   ...
}
```

So the driver asks for **`ALL` (property identifier 8)** in a single `ReadPropertyMultiple`, and degrades in
three steps:

```
RPM(oid, ALL=8)
   ├─ RPM unsupported by device          → ReadProperty loop over REQUIRED properties only
   ├─ BacnetException on the RPM         → ReadProperty loop over REQUIRED properties only
   ├─ null result                        → ReadProperty loop over REQUIRED properties only
   └─ result contains propertyId == 8    → (device rejected ALL) same fallback
```

`getRequiredProperties(objectId)` comes from `objectTypes.xml`'s `r="true"` attributes (§271.5).
Separately, `getPossibleProperties()` reads the device's `protocolRevision` and **strips property 139**
(`propertyList`) — `[CERT]` `BBacnetDevice:1506-1518`.

---

## 271.5 — `objectTypes.xml`: the master table that drives everything `[CERT]`

Packaged inside `bacnet-rt.jar` at `com/tridium/bacnet/objectTypes.xml`, 1 658 lines. Its own header
states scope verbatim:

> "The information is intended to be current with ANSI/ASHRAE 135-2012, which is Protocol_Version 1,
> Protocol_Revision 14, and addenda to 135-2012 for Protocol_Revision 16."

Attribute schema, verbatim from the file's comment block:

| Attr | Meaning | Optional |
|---|---|---|
| `n` | Name — used to create the Niagara Property object | no |
| `i` | Property Identifier | no |
| `a` | **Asn Type** (negative values are Niagara-defined) | no |
| `r` | Required flag | yes |
| `e` | Extensible enumeration flag | yes |
| `t` | Type — class name for constructed data types | yes |
| `b` | BitString name | yes |
| `s` | Size — fixed array size | yes |
| `f` | Facet — "true if this property configures a facet for display" | yes |
| `c` | **Facet-controlled** — configures facet control of property display | yes |
| `pr` | Protocol Revision when the property was added ("Reduces upload time of older devices") | yes |

ASN type codes (the axis the entire type decision turns on):

```
 0 NULL     1 BOOLEAN   2 Unsigned   3 INTEGER   4 REAL    5 Double
 6 OCTET_STRING   7 CharacterString  8 BIT_STRING   9 ENUMERATED
10 Date    11 Time     12 BACnetObjectIdentifier   13/14/15 ASHRAE_RESERVED

Niagara-defined:
-1 ASN_CONSTRUCTED_DATA   -2 ASN_BACNET_ARRAY   -3 ASN_BACNET_LIST
-4 ASN_ANY                -5 ASN_CHOICE         -6 ASN_UNKNOWN_PROPRIETARY
```

Example row — `AnalogInput` (type 0), abridged `[CERT]`:

```xml
<object n="AnalogInput" t="0">
  <property n="presentValue"  i="85"  a="4" r="true" c="all"/>
  <property n="statusFlags"   i="111" a="8" r="true" b="BacnetStatusFlags"/>
  <property n="units"         i="117" a="9" r="true" f="true" e="true" t="bacnet:BacnetEngineeringUnits"/>
  <property n="minPresValue"  i="69"  a="4" f="true" c="units"/>
  <property n="maxPresValue"  i="65"  a="4" f="true" c="units"/>
  <property n="resolution"    i="106" a="4" f="true" c="units"/>
  <property n="covIncrement"  i="22"  a="4" c="units"/>
  <property n="highLimit"     i="45"  a="4" c="units"/>
  <property n="faultHighLimit" i="388" a="4" c="units" pr="16"/>
</object>
```

Object types present in the file (line anchors): AnalogInput 0, AnalogOutput 1, AnalogValue 2, BinaryInput 3,
BinaryOutput 4, BinaryValue 5, Calendar 6, Command 7, Device 8, EventEnrollment 9, File 10, Group 11,
Loop 12, MultiStateInput 13, MultiStateOutput 14, NotificationClass 15, Averaging 18, MultiStateValue 19,
LifeSafetyPoint 21, LifeSafetyZone 22, … `[CERT]`

### `PropertyInfo`: the XML row becomes a decision object

`javax.baja.bacnet.util.PropertyInfo(XElem x, int objectPR)` — original Tridium source, ~2002 vintage,
author Craig Gemmill `[CERT]`. The `asnType` → Niagara type-spec map:

| ASN | Niagara type |
|---|---|
| 0 NULL | `bacnet:BacnetNull` |
| 1 BOOLEAN | `baja:Boolean` |
| 2 Unsigned | `bacnet:BacnetUnsigned` |
| 3 INTEGER | `baja:Integer` |
| 4 REAL | `baja:Float` |
| 5 Double | `baja:Double` |
| 6 OCTET_STRING | `bacnet:BacnetOctetString` |
| 7 CharacterString | `baja:String` |
| 8 BIT_STRING | `bacnet:BacnetBitString` |
| **9 ENUMERATED** | *(left blank — "handled later")*, then `type = x.get("t")` and **`facetControl = "enum"` is forced** |
| 10 Date / 11 Time / 12 ObjectId / ANY | `bacnet:BacnetDate` / `BacnetTime` / `BacnetObjectIdentifier` / `BacnetAny` |

The enumerated branch is the interesting one: an `ENUMERATED` property always overrides whatever `c=` said
in the XML and becomes facet-controlled as `"enum"`. That is why enum points always arrive with a range.

### Resolution order when a property is looked up

`BBacnetDevice.getPropertyInfo(objectType, propId)` `[CERT]`:

```
1. vendorObjectTypesList  (per-device BOrd → an external XML the integrator supplies)
2. ObjectTypeList.getInstance()   (the packaged objectTypes.xml)
3. if propInfo.isAws() && !device.isAws()  → discard (AWS = Alarm/Workstation extras)
4. still null → new PropertyInfo(tag(propId), propId, -6 /* ASN_UNKNOWN_PROPRIETARY */)
```

Step 1 is the documented extension point for proprietary objects: set `vendorObjectTypesFile` on the
`BBacnetDevice` to an ORD pointing at your own XML in the same schema, and it wins over Tridium's table.

---

## 271.6 — THE type decision: how N4 knows Numeric vs Boolean vs Enum vs String

There are **two** decisions, at different times, and they use different inputs. This is the part the corpus
was missing entirely.

### Decision 1 — discovery time (which point classes are offered)

`com.tridium.bacnet.ui.point.PointLearn.toTypes(Object dis)` `[CERT]` (bacnet-wb). Input is the **ASN type**,
not the object type:

```java
PropertyInfo propInfo = pointManager.getDevice().getPropertyInfo(objectType, propId);
int asnType = (propInfo == null)
            ? AsnUtil.getAsnType(lr.getValue().getType())     // fall back to the sampled value
            : propInfo.getAsnType();
```

Note the fallback: for an unknown proprietary property with no XML row, the driver types the point from the
**actual value it received during the learn**, not from a table.

The switch, verbatim in behaviour:

| asnType | Types offered (in order — element 0 is the default) |
|---|---|
| `-3` LIST, `-1` CONSTRUCTED, `0` NULL | **`return null`** → the row is not addable at all |
| `-2` BACNET_ARRAY | only if `propertyArrayIndex > 0`; then order depends on object type (below) |
| `1` BOOLEAN | Boolean |
| `2` Unsigned | **`isMultiStatePresentValue` ? Enum, Boolean, Numeric : Numeric, Enum, Boolean** |
| `3` INTEGER | Numeric, Enum, Boolean |
| `4` REAL / `5` Double | **Numeric only** |
| `9` ENUMERATED | **`isBinaryPv` ? Boolean first, then Enum : Enum only** |
| `6/7/8/10/11/12`, `-4`, `-5`, `-6`, default | (nothing type-specific) |
| **always, appended last** | `addStringTypes(typeList, false)` — String is the universal escape hatch |

The two predicates that carry all the nuance `[CERT]`:

```java
static boolean isMultiStatePresentValue(int propertyId, int objectType) {
   return propertyId == 85 && (objectType == 13 || objectType == 14 || objectType == 19);
}                                  //  MultiStateInput / MultiStateOutput / MultiStateValue

static boolean isBinaryPv(PropertyInfo info) {
   return info != null && "bacnet:BacnetBinaryPv".equals(info.getType());
}
```

So, in plain language:

- **AI/AO/AV present-value is `a="4"` (REAL) → Numeric, and *only* Numeric.** There is no ambiguity and no
  choice offered. This is why analog points never come in as anything else.
- **BI/BO/BV present-value is `a="9"` (ENUMERATED) with `t="bacnet:BacnetBinaryPv"` → Boolean first, Enum
  second.** Binary objects are enumerated on the wire; Niagara only maps them to Boolean because of that
  one type-string check. Any *other* enumerated property (eventState, reliability, units…) gets **Enum
  only** — no Boolean offered.
- **MSI/MSO/MSV present-value is `a="2"` (Unsigned) → Enum first**, because of the `propertyId == 85 &&
  objectType ∈ {13,14,19}` test. The exact same ASN type on any other object (e.g. an Unsigned
  `updateInterval`) yields **Numeric** first. The multi-state distinction is *not* in the ASN type — it is a
  hardcoded object-type triple.
- **String is always appended**, so any discoverable property can be forced into a `BStringPoint`.
- `-1`/`-3`/`0` return `null`: constructed data, lists and NULL are structurally not point-able. Arrays
  (`-2`) are addable only when a concrete index is selected.

> **SCOPE REFINED by [B276] §276.7.** This switch is the **Workbench learn** decision. A *second*
> implementation exists in `BacnetDescriptorUtil.makePointForPropertyInfo()` (bacnet-rt), used when a
> descriptor auto-creates a point while resolving a reference. It **disagrees for ASN 0 (NULL), -1
> (CONSTRUCTED) and -3 (LIST)** — where this path returns `null` (not addable), that one produces a
> `BStringWritable` — and it always produces the **Writable** variant, with no `writableFirst` test.

### Decision 2 — instantiation time (which ProxyExt gets attached)

`BBacnetPointManager.Model.newInstance(MgrTypeInfo)` `[CERT]`:

```java
BControlPoint pt = (BControlPoint)type.newInstance();
BAbstractProxyExt ext = new BNullProxyExt();
if      (pt instanceof BBooleanPoint) ext = new BBacnetBooleanProxyExt();
else if (pt instanceof BNumericPoint) ext = new BBacnetNumericProxyExt();
else if (pt instanceof BEnumPoint)    ext = new BBacnetEnumProxyExt();
else if (pt instanceof BStringPoint)  ext = new BBacnetStringProxyExt();

if (ext instanceof BProxyExt)
   ((BProxyExt)ext).setDeviceFacets(pt.getFacets().newCopy());
pt.setProxyExt(ext);
```

Note the direction: **the point class picks the ProxyExt, not the reverse.** The ProxyExt then enforces the
pairing at runtime — `BBacnetNumericProxyExt.isParentLegal(BComponent parent)` returns
`parent instanceof BNumericPoint` `[CERT]`, with the javadoc "BBacnetNumericProxyExt must be in a
NumericPoint."

### Decision 3 — runtime (the `dataType` slot self-heals)

The third, least-known mechanism. `BBacnetNumericProxyExt.fromEncodedValue()` `[CERT]`:

```java
int tag = asnIn.peekApplicationTag();
if (getDataType().length() == 0)              // ← first read only
   setDataType(AsnUtil.getAsnTypeName(tag));

switch (tag) {
   case ASN_NULL:             dv.setStatusNull(true); break;
   case ASN_BOOLEAN:          dv.setValue(asnIn.readBoolean() ? 1.0D : 0.0D); break;
   case ASN_UNSIGNED:         dv.setValue(asnIn.readUnsignedInteger()); break;
   case ASN_INTEGER:          dv.setValue(asnIn.readSignedInteger()); break;
   case ASN_REAL:             dv.setValue(asnIn.readReal()); break;
   case ASN_DOUBLE:           dv.setValue(asnIn.readDouble()); break;
   case ASN_OCTET_STRING:     dv.setValue(asnIn.readOctetString()[0]); break;
   case ASN_CHARACTER_STRING: dv.setValue(Double.parseDouble(asnIn.readCharacterString())); break;
   case ASN_ENUMERATED:       dv.setValue(asnIn.readEnumerated()); break;
   case ASN_BIT_STRING / DATE / TIME / OBJECT_IDENTIFIER: /* consumed, value untouched */ break;
   default:                   dv.setValue(asnIn.readReal()); break;
}
```

Three things follow:

1. `dataType` is a **`Flags.READONLY` slot that the driver fills from the first response it decodes**, not
   something the engineer sets. Its declaration is `newProperty(Flags.READONLY, "", null)` `[CERT]`.
2. A Numeric point will happily coerce a BOOLEAN (`true→1.0`), an ENUMERATED (ordinal), an OCTET_STRING
   (first byte!) or even a CharacterString (`Double.parseDouble`) into a double. **Mis-typing a point does
   not necessarily fault it** — it silently produces a plausible-looking number. This is the single most
   deceptive behaviour in the whole pipeline.
3. The write path mirrors it: `toEncodedValue()` switches on the cached `asnType` (set by `setAsnType()`
   from `getDataType()`), so **the driver writes back in whatever type it first read** — with
   `ASN_BIT_STRING`, `ASN_DATE`, `ASN_TIME`, `ASN_OBJECT_IDENTIFIER` and the reserved codes returning
   `NO_VALUE` (silently unwritable). `[CERT]`

---

## 271.7 — Read-only vs writable: the actual rule

Three mechanisms stack. None of them is "ask the device if it's writable" — BACnet has no such query.

### (a) Learn-time default: `writableFirst` by object type `[CERT]`

`PointLearn.toTypes()`:

```java
boolean writableFirst = false;
if (objectType == 1 || objectType == 4 || objectType == 14)   // AO, BO, MSO
   writableFirst = true;
```

`addNumericTypes(typeList, writableFirst)` then emits `BNumericWritable` before `BNumericPoint` (and the
mirror for Boolean/Enum/String). Since element 0 is the default selection in the learn table, **Output
objects default to Writable and Input objects default to read-only**, purely from the object type.

### (b) Value objects: probe for a priority array `[CERT]`

AV(2) / BV(5) / MSV(19) are ambiguous — the standard makes `priorityArray` optional on them. The driver
resolves it empirically, by trying to read property **87**:

```java
public static BBoolean checkForPriorityArray(BBacnetObjectIdentifier objectId, BBacnetDevice device) {
   if (!objectId.isValid()) return BBoolean.FALSE;
   try {
      client().readProperty(device.getAddress(), objectId, 87, 0);   // priorityArray, index 0
      return BBoolean.TRUE;
   } catch (Exception var3) {
      return BBoolean.FALSE;
   }
}
```

A **success/exception test on a live ReadProperty** — not a table lookup. The result is stored as the
`priPV` facet (`PRIORITIZED_PRESENT_VALUE`) and drives the learn row:

```java
if (pt.isWritablePoint()) {
   int objectType = lr.getObjectId().getObjectType();
   if (objectType == 1 || objectType == 4 || objectType == 14) writablePrioritized = true;
   if (objectType == 2 || objectType == 5 || objectType == 19)
      writablePrioritized = this.getDiscoveryJob().checkForPriorityArray(lr.getObjectId()).getBoolean();
   if (writablePrioritized) row.setCell(colEnabled, BBoolean.make(false));   // ← added DISABLED
}
```

**Operational consequence, and it surprises people**: a discovered writable-prioritized point is added with
`proxyExt.enabled = false`. Niagara refuses to auto-start commanding a priority array on a point you just
learned. You must enable it deliberately. `[CERT]` `BBacnetPointManager.Learn.toRow():281-295`

### (c) Runtime status and mode `[CERT]`

```java
// BBacnetProxyExt.started()
if (getParentPoint().isWritablePoint()) setWriteStatus(WRITABLE);

// BBacnetProxyExt.getMode()
return getParentPoint().isWritablePoint() ? BReadWriteMode.readWrite : BReadWriteMode.readonly;
```

`isWritablePoint()` is a `BControlPoint` property — it is simply "am I a `B*Writable` subclass". So the
final read/write mode is a **consequence of the class chosen at (a)/(b)**, and the learn table's
`writeStatus` column shows the literal strings `"readonly"` / `"writable"` computed the same way.

And `discoverPrioritizedPresentValue()` re-derives `priPV` on every start `[CERT]`:

```java
switch (objectType) {
  case ANALOG_OUTPUT: case BINARY_OUTPUT: case MULTI_STATE_OUTPUT:      // 1, 4, 14
     priPV = BBoolean.make(getPropertyId().getOrdinal() == PRESENT_VALUE);
     break;
  case ANALOG_VALUE: case LARGE_ANALOG_VALUE: case BINARY_VALUE:        // 2, 46, 5
  case MULTI_STATE_VALUE: case CHARACTER_STRING_VALUE:                  // 19, 40
  case INTEGER_VALUE: case POSITIVE_INTEGER_VALUE:                      // 45, ...
     if (propertyId == PRESENT_VALUE) {
        BBoolean ppv = (BBoolean)getDeviceFacets().getFacet(PRIORITIZED_PRESENT_VALUE);
        if (getParentPoint() instanceof BIWritablePoint && (ppv == null || force))
           network().postWrite(() -> checkForPriorityArray());   // async re-probe
        else priPV = ppv;
     } else priPV = BBoolean.FALSE;
     break;
}
```

Two notes: the probe is **cached in the facet** and only re-run when absent or forced; and it is posted onto
the **write queue**, not the poll queue, so it competes with commands rather than with reads.

---

## 271.8 — Facet derivation: where units, min/max, precision and stateText come from `[CERT]`

`com.tridium.bacnet.job.BacnetDiscoveryUtil`. A per-object-type table decides which properties get read
purely to build facets:

```java
facetPropsByType.put(0,  new int[]{106, 69, 65, 117});   // AnalogInput
facetPropsByType.put(1,  new int[]{106, 69, 65, 117});   // AnalogOutput
facetPropsByType.put(2,  new int[]{106, 69, 65, 117});   // AnalogValue
facetPropsByType.put(45, new int[]{106, 69, 65, 117});   // IntegerValue
facetPropsByType.put(46, new int[]{106, 69, 65, 117});   // LargeAnalogValue
facetPropsByType.put(48, new int[]{106, 69, 65, 117});   // PositiveIntegerValue
facetPropsByType.put(3,  new int[]{4, 46});              // BinaryInput   → activeText/inactiveText
facetPropsByType.put(4,  new int[]{4, 46});              // BinaryOutput
facetPropsByType.put(5,  new int[]{4, 46});              // BinaryValue
facetPropsByType.put(13, new int[]{110});                // MultiStateInput  → stateText
facetPropsByType.put(14, new int[]{110});                // MultiStateOutput
facetPropsByType.put(19, new int[]{110});                // MultiStateValue
```

Any object type **not** in this map returns an empty facet map — no probing at all.

`addFacet()` then converts each raw ASN payload into a Niagara facet:

| BACnet property | id | → Niagara facet | Conversion |
|---|---|---|---|
| `activeText` | 4 | `trueText` | `fromAsnCharacterString` |
| `inactiveText` | 46 | `falseText` | `fromAsnCharacterString` |
| `maxPresValue` | 65 | `max` | `fromAsnReal`, clamped to ±∞ |
| `minPresValue` | 69 | `min` | `fromAsnReal`, clamped to ±∞ |
| `resolution` | 106 | `resolution` **+ `precision`** | see below |
| `stateText` | 110 | `range` (`BEnumRange`) | see below |
| `units` | 117 | `units` (`BUnit`) | `BBacnetEngineeringUnits → getNiagaraUnits()` |

**Precision from resolution** — a derived facet with no BACnet counterpart `[CERT]`:

```java
float f = AsnUtil.fromAsnReal(propertyValue);
fmap.put("resolution", BFloat.make(f));
if (f > 0.0F) {
   double fPrec = -(Math.log(f) / LN_10);      // -log10(resolution)
   fPrec -= 1.0E-6;                            // epsilon against float noise
   BInteger precision = BInteger.make((int)Math.ceil(fPrec));
   if (precision.getInt() > 7) precision = BInteger.make(7);   // hard cap
   fmap.put("precision", precision);
}
```

`resolution=0.1` → `precision=1`; `0.01` → `2`; `0.5` → `1` (`ceil(0.301…)`). Cap at 7 digits.

**stateText → BEnumRange** `[CERT]`, with a duplicate-name mangling that is easy to miss:

```java
for (int tag = in.peekTag(); tag != -1; tag = in.peekTag()) {
   StringBuilder s = new StringBuilder(SlotPath.escape(in.readCharacterString()));
   while (v.contains(s.toString())) s.append("$2E");     // duplicate → append escaped '.'
   v.add(s.toString());
}
int[] ords = new int[v.size()];
for (int j = 0; j < ords.length; j++) ords[j] = j + 1;   // ordinals are 1-based
fmap.put("range", BEnumRange.make(ords, tags));
```

Two field-relevant facts: **ordinals start at 1**, matching BACnet's 1-based multi-state convention (so a
Niagara enum tag ordinal maps directly to the device's present-value); and a device with duplicate state
names gets `Name`, `Name$2E`, `Name$2E$2E`… rather than an error.

**Units failure is silent-ish** `[CERT]`:

```java
if (BBacnetEngineeringUnits.isFixed(unitEnum)) {
   fmap.put("units", BBacnetEngineeringUnits.make(unitEnum).getNiagaraUnits());
} else if (logger.isLoggable(Level.INFO)) {
   logger.info("Unit enumeration " + tag(unitEnum) + " is unknown!");
}
```

Proprietary unit enums (the ASHRAE reserved range is **47808–49999**, per `BBacnetEngineeringUnits:835/902`
`[CERT]`) produce **no `units` facet at all** and only an INFO log line. A point that shows up unitless
after a learn is usually this, not a wiring problem.

### `facetControl` — how much of the discovered facet set actually lands

`BBacnetPointManager.Learn.toRow()` `[CERT]`:

```java
if (info.getFacetControl().equals("all"))        facets = lrFacets;                    // c="all"
else if (info.getFacetControl().equals("units")) facets = BFacets.make("units", lrFacets.getFacet("units"));
else if (!info.getFacetControl().equals("no") && lrFacets != null && !lrFacets.isNull())
                                                 facets = lrFacets;
// enums first: if (info.isEnum()) lrFacets = BFacets.make(lrFacets, "range", r);
...
row.setCell(colDeviceFacets, facets);
facets = BFacets.makeRemove(facets, "priPV");     // priPV is device-side only
row.setCell(colFacets, facets);
```

So `c="all"` (present-value) inherits the full facet set; `c="units"` (minPresValue, highLimit, covIncrement,
deadband…) inherits **only** the unit, which is exactly right — a limit expressed in °C needs the unit but
not the parent's min/max. `c="no"` / absent inherits nothing. And `priPV` is stripped from the *point*
facets while surviving on `deviceFacets`, which is why you see it on the ProxyExt but not on the point.

Extensible enums resolve through the device's own enumeration list `[CERT]`:

```java
if (info.isEnum()) {
   BEnum en = (BEnum)BTypeSpec.make(info.getType()).getInstance();
   BEnumRange r = en.getRange();
   if (info.isExtensible())
      r = getDevice().getEnumerationList().getEnumRange(info.getType());   // per-device, from the wire
   if (r != null) lrFacets = BFacets.make(lrFacets, "range", r);
}
```

---

## 271.9 — Runtime algorithm: polling `[CERT]`

`com.tridium.bacnet.stack.BBacnetPoll extends BAbstractPollService` (1 370 lines). Three buckets with
Niagara's standard rates:

| Bucket | Property | Default |
|---|---|---|
| fast | `fastRate` | `BRelTime.make(1000L)` — 1 s |
| normal | `normalRate` | `BRelTime.make(5000L)` — 5 s |
| slow | `slowRate` | `BRelTime.make(30000L)` — 30 s |

(plus read-only telemetry slots `fastPolls`/`normalPolls`/`slowPolls`, `*Count`, `*CycleTime`.)

### Batching: pack until the APDU is full

The genuinely interesting algorithm. `BBacnetPoll.add(List<PollList>, PollListEntry, push, newList)`:

```java
while (!added && it.hasNext()) {
   PollList pl = it.next();
   if (device == pl.getDevice()
       && (!newList || pl.pollCount.get() == 0)
       && !pl.isPolling()
       && pl.getDataSize() + ple.getDataSize() < this.getMaxDataSize(pl)) {
      pl.add(ple);  added = true;  break;
   }
}
if (!added) list.add(new PollList(ple));    // start a new RPM batch
```

with

```java
private int getMaxDataSize(PollList pl) {
   int maxDataSize = pl.getDevice().getMaxAPDULengthAccepted();
   int myMax       = BBacnetNetwork.localDevice().getMaxAPDULengthAccepted();
   if (maxDataSize > myMax) maxDataSize = myMax;
   return maxDataSize - 5;                  // fixed 5-byte header reserve
}
```

So a `PollList` **is** one `ReadPropertyMultiple` request, bin-packed greedily against
`min(remote maxAPDU, local maxAPDU) − 5`. Points are grouped **per device**, never across devices. A batch
already in flight (`isPolling()`) is never appended to.

### The size estimate is adaptive

`PollListEntry.DEFAULT_DATASIZE = 11` bytes as the initial guess `[CERT]`. On a size miss:

```java
public final void doubleDataSize(int max) { this.dataSize = Math.min(max, dataSize * 2); }
```

and in `BBacnetPoll`:

```java
if (size == -1) entry.doubleDataSize(pl.getDevice().getMaxAPDULengthAccepted());
else            entry.setDataSize(Math.min(pl.getDevice().getMaxAPDULengthAccepted(), size));
```

Exponential back-off doubling, capped at the device's maxAPDU, with the real size latched once a response
arrives (`dataSize = encodedValue.length` in `fromEncodedValue` `[CERT]`). The driver *learns* how big each
point's encoding is and re-packs accordingly.

### Removal mid-poll

`remove()` cannot mutate a list that is polling or not done; it instead **clones the survivors into a new
`PollList`, marks it done, and swaps** `[CERT]`. That preserves the in-flight request's integrity.

---

## 271.10 — Runtime algorithm: COV subscription `[CERT]`

Tuning-policy slots (`BBacnetTuningPolicy`, original Tridium source):

| Slot | Default |
|---|---|
| `pollFrequency` | `BPollFrequency.normal` |
| `useCov` | `false` |
| `useConfirmedCov` | `true` |
| `covSubscriptionLifetime` | `15` (minutes) |
| `useCovProperty` | `false` |
| `useConfirmedCovProperty` | `true` |
| `covPropertyIncrement` | `1.0` |
| `covPropertySubscriptionLifetime` | `15` |
| `acceptUnsolicitedCov` | `false` |

**COV is opt-in.** Out of the box a BACnet point is polled, not subscribed.

Resubscription timing — javadoc verbatim: *"The parent device's Cov subscription lifetime is used, subject
to the following modifications: If this value is less than or equal to zero, force a resubscription at least
once per day. The minimum lifetime is 5 minutes. The resubscribe time is calculated from the subscription
lifetime by using a safety factor of 2 for resubscriptions."*

```java
public static BRelTime calculateResubscribeTime(boolean postFailed, int subscriptionLifeTime) {
   if (postFailed) return RELTIME_ON_POST_FAILURE;      // retry in 10 seconds
   int subLife = subscriptionLifeTime;
   if (subLife <= 0) return ONCE_A_DAY_RELTIME;
   if (subLife <= 5) return MINIMUM_RELTIME;            // 5 minutes
   return BRelTime.make(subLife * RESUBSCRIPTION_FACTOR);   // factor 2
}
```

> **CORRECTED by [B272] §272.2.** The "safety factor of 2" is a **division**, not a multiplication:
> `RESUBSCRIPTION_FACTOR = 30000L` is literally `60000 / 2` (minutes→millis, halved), so the resubscribe
> interval is **half the lifetime** — 7 min 30 s at the default of 15 minutes. `ONCE_A_DAY_RELTIME` is
> likewise **12 hours**, not 24. The gap **B271-G3 is closed**; see B272 for the full arithmetic and the
> three numbers it also corrects in B23 §23.10.

Subscription state machine — ten states, from `BBacnetProxyExt` `[CERT]`:

```
SUB_STATE_UNSUB=0  POLLED=1  COV=2  FIRST_COV_PENDING=3  POLLED_PENDING=4
COV_PENDING=5      FIRST_COVP_PENDING=6  COVP=7  COVP_PENDING=8  COVP_FAILED=9
```

`readSubscribed()` dispatches to `subscribeCov()` or `subscribeCovProperty()` per `useCov()`/
`useCovProperty()`; the actual request is posted async as a `PointCmd` (`SUBSCRIBE_COV_POINT` /
`SUBSCRIBE_COVP_POINT` / `READ_POINT`) onto the network queue rather than executed inline `[CERT]`.

---

## 271.11 — Runtime algorithm: write and the priority array `[CERT]`

`BBacnetProxyExt.write(Context)`, original Tridium source with its javadoc:

```java
int writeLevel = 0;
if (getActiveLevel() == BPriorityLevel.FALLBACK && getWriteValue().getStatus().isNull()) {
   if (isPriorityArrayPoint())                       writeValue = null;   // relinquish
   else if (isPrioritizedPresentValue()
            && network().setAndGetWriteOnFacetChange().getBoolean())
                                                     writeValue = null;
   else                                              return false;       // write nothing
} else {
   writeValue = (BStatusValue)getWriteValue().newCopy();
   if (isPrioritizedPresentValue()) writeLevel = getActiveLevel();
}

network().postWrite(new PointCmd(PointCmd.WRITE_POINT, this, writeValue, lastWriteLevel, writeLevel));
if (isPrioritizedPresentValue()) lastWriteLevel = writeLevel;

return false;   // "return false always to prevent point getting stuck"
```

with

```java
private int getActiveLevel() {
   return getWriteValue().getStatus().geti(BStatus.ACTIVE_LEVEL, BPriorityLevel.FALLBACK);
}
```

The mapping is direct and worth stating plainly: **Niagara's `BPriorityLevel` (the point's active level from
its own priority-array-like `BStatusValue`) becomes the BACnet WriteProperty priority.** A `null` write
value is the relinquish (BACnet NULL at that priority). `lastWriteLevel` is carried into the `PointCmd` so
the command layer knows which level to clear when the active level *changes* — javadoc: *"When the active
level changes, the old level is cleared, and the new level is written."*

`isPriorityArrayPoint()` = `propertyId == PRIORITY_ARRAY (87)`; `isPrioritizedPresentValue()` = the cached
`priPV` from §271.7. If `priPV` is somehow null the code logs a `severe` with `Thread.dumpStack()` and
defaults to `true` — a deliberate fail-loud `[CERT]`.

---

## 271.12 — Ports and link layers `[CERT]`

| Layer | Port / medium | Source |
|---|---|---|
| **BACnet/IP** | UDP **`0xBAC0` = 47808**, declared as `BBacnetIpLinkLayer.udpPort = newProperty(0, "0xBAC0", null)` with `UDP_PORT_DEFAULT = "0xBAC0"` | `bacnet-rt` `[CERT]` |
| BACnet MS/TP | RS-485 token passing (no IP port) | B23 §23.27 |
| BACnet Ethernet | raw ISO-8802-3, **bypasses IP entirely**, via `pcapBacEther.dll` on Npcap | B127 `[CERT]` |
| BACnet/SC | TCP + TLS, `stack.link.sc.*` websocket framing | B23 §23.24, gap **P3-sc** still open |
| PTP | point-to-point serial | B23 §23.27 |

The `udpPort` slot is a **String** holding a hex literal, not an int — a small but real gotcha when scripting
network config. Multiple BACnet/IP networks on one host use 47809, 47810… by convention (`0xBAC1`, `0xBAC2`)
`[EXT-KB]`; the driver imposes no such rule, it just reads the slot.

Note the collision hazard when reading code: **47808 is also `BBacnetEngineeringUnits.ASHRAE_RESERVED_RANGE_MIN`**
(and the ordinal of `standardCubicFeetPerDay`). A grep for `47808` in `bacnet-rt` returns five unit-enum hits
and zero port hits, because the port is written as hex. `[CERT]`

---

## 271.13 — Tooling

### Inside Niagara (verified in this corpus) `[CERT]`

| Tool | Module | What it does |
|---|---|---|
| **Bacnet Device Manager** | `bacnet-wb` | `Discover` → `BBacnetDiscoverDevicesJob`; learn table columns Network/Addr/maxAPDU/Vendor/Model/Id |
| **Bacnet Point Manager** | `BBacnetPointManager` (`bacnet-wb`, 415 lines) | `@AgentOn types={"bacnet:BacnetPointDeviceExt","bacnet:BacnetPointFolder"}, requiredPermissions="W"` — 18 columns incl. `dataType`, `deviceFacets`, `conversion` |
| Schedule / TrendLog discovery | `BBacnetDiscoverSchedulesJob`, `BBacnetDiscoverTrendLogsJob` | sibling jobs for object types 17/20 |
| **EDE import/export** | `bacnetEDE-wb` | Engineering Data Exchange CSV — offline point list (B23 §23.21) |
| Migrator | `bacnetMigrator-wb` | AX→N4 station conversion (B23 §23.23) |
| Alarm router | `bacnetAlarmRouter-rt` | intrinsic BACnet events → Niagara alarm classes (B34) |
| Virtual gateway | `BBacnetVirtualGateway` | browse device objects under `virtual:` **without** creating BOG proxies |
| Honeywell extras | `honBacnetHelper`, `honBACnetUtilities`, `honUtilityBacRestore` | descriptor extensions, license gate, private-transfer (B246) |
| **Vendor override** | `vendorObjectTypesFile` ORD on `BBacnetDevice` | supply your own `objectTypes.xml`-schema file for proprietary objects (§271.5) |

The permission gate is worth noting: the Point Manager view requires **`W`** (write) on the target, so a
read-only operator cannot even open the learn.

### Outside Niagara `[EXT-KB]`

Standard industry tooling for BACnet work — **not verifiable in this corpus**, listed for completeness:
Wireshark (has a native BACnet/BVLC dissector), YABE (Yet Another BACnet Explorer), VTS (Visual Test Shell,
the BACnet Testing Laboratories tool), and the `bacnet-stack` open-source utilities (`bacwi`, `bacrp`,
`bacwp`). Anything specific about their behaviour should be validated before being written down as fact.

---

## 271.14 — BACtalk / Alerton: negative finding `[CERT]`

Direct answer: **there is no BACtalk or Alerton driver in this corpus.**

Evidence:
- `rg -il "bactalk|alerton"` across `organized/` (all decompiled modules) returns **zero** hits for
  "bactalk" and hits "alerton" only inside `honIrmConfig-rt` (`AlertonDeviceModels.java`,
  `DeviceModelEnum.java`, `BBrandEnum.java`) — i.e. an **IRM/Nano branding enum**, not a protocol driver.
- The only first-class trace is one row in the packaged vendor registry:

```xml
<vendor id="18" n="Alerton / Honeywell"/>
```

`[CERT]` `com/tridium/bacnet/vendors.xml:28`.

Interpretation `[INFER]`: Alerton BACtalk controllers integrate through the **standard BACnet driver** and
are identified only by **vendor-id 18** in their I-Am. If they expose proprietary objects or properties, the
supported path is the `vendorObjectTypesFile` override of §271.5 — write an XML in Tridium's schema and hang
it off the `BBacnetDevice`. There is no Alerton-specific code path, no Alerton point type, and no
BACtalk-specific service handling anywhere in the 51 k-class corpus.

(Related but distinct: `honIrmConfig` carries Alerton *brand* metadata for IRM/Nano wall-module
configuration — that is the Honeywell product-line merge showing through the UI, not a protocol driver.)

---

## 271.15 — Corpus deltas: corrections to B28

Five findings. Three are factual errors, three are stale cross-references (one block has both).

### Delta #1 — B28 §28.2.2 Step 2: wrong service code `[CERT]`

B28 says:

> `APDU unconfirmed service 0x10 (Who-Is)`

**Wrong.** `0x10` is the **APDU PDU-type nibble** for `UNCONFIRMED_REQUEST` (B23 §23.32 lists it correctly as
such). The Who-Is **service choice** is **8** (`BacnetUnconfirmedServiceChoice.WHO_IS = 8`). A third number,
34, is Who-Is's **bit position** in `servicesSupported`. B23 §23.5 already had this right.

### Delta #2 — B28 §28.2.2 Steps 5-6: collect and mine are NOT sequential `[CERT]`

B28 describes:

> `Paso 5: timeout — fin del collect / job sleep(config.timeout)`
> `Paso 6: mining per device`

**Wrong.** There is no `sleep(timeout)`. `run()` is a single `while` loop that mines each I-Am as it arrives
and only sleeps 500 ms when the backlog is empty, and it **outruns its own timeout** while entries remain
(`now.isBefore(end) || this.count < iAmSize`). See §271.2.

### Delta #3 — B28 §28.2.3: wrong RPM property set `[CERT]`

B28 says:

> `ReadPropertyMultiple(devId, [objId], [PRESENT_VALUE=85, OBJECT_NAME=77, STATUS_FLAGS=111, UNITS=117])`

**Wrong.** The driver requests the special property identifier **`ALL` (8)** in one `NReadAccessSpec`, and
falls back to the object's **required** properties (from `objectTypes.xml` `r="true"`) on any of four failure
conditions. See §271.4. The facet properties (69/65/106/117 etc.) are read by a *separate* mechanism —
`BacnetDiscoveryUtil.discoverFacets`, §271.8 — not as part of that RPM.

### Delta #4 — B28: three stale cross-references into B23 `[CERT]`

| B28 says | B23 §  actually is | Correct target |
|---|---|---|
| "Bloque 23.10 cubrió protocol-level WhoIs/IAm" | 23.10 = COV subscription lifecycle | **23.5** |
| "segmentación (Bloque 23.11)" | 23.11 = Schedule object | **23.9** |
| "Bloque 23.15 cubrió BDT/FDT" | 23.15 = Trend Log | **23.26** |

B23 was evidently renumbered after B28 was written.

### Delta #5 — B23 §23.4: gap, not error `[INFER]`

B23 §23.4 correctly lists the four ProxyExt subclasses and their target point classes, but presents the
mapping as if it were a fixed table. It is not — it is the three-stage decision of §271.6, and the
`dataType` slot is discovered at runtime from the first ASN tag received. Recommend §23.4 gain a pointer to
this block rather than being rewritten.

---

## 271.16 — Gotchas worth carrying into the field

1. **A mis-typed point does not fault — it lies.** `BBacnetNumericProxyExt` coerces BOOLEAN→1.0/0.0,
   ENUMERATED→ordinal, OCTET_STRING→first byte, CharacterString→`parseDouble`. Verify `dataType` on the
   ProxyExt after a learn. `[CERT]` §271.6
2. **Writable-prioritized points are added disabled.** `colEnabled = false` for AO/BO/MSO and for any
   AV/BV/MSV that answered the priority-array probe. Nothing commands until you enable it. `[CERT]` §271.7
3. **Priority-array support is probed with a live read of property 87**, once, and cached in a facet. If the
   device was offline or busy during the learn, the cached answer is wrong until forced. `[CERT]` §271.7
4. **Unknown unit enums silently produce no `units` facet** — INFO log only. `[CERT]` §271.8
5. **`precision` is invented by Niagara** from `resolution` via `ceil(-log10(r) - 1e-6)`, capped at 7. It is
   not a BACnet property. `[CERT]` §271.8
6. **Multi-state ordinals are 1-based** and duplicate state names get `$2E` suffixes. `[CERT]` §271.8
7. **COV is off by default** (`useCov = false`); everything polls until you say otherwise. `[CERT]` §271.10
8. **Poll batching is per-device and capped at `min(remote, local) maxAPDU − 5`.** A device advertising a
   small maxAPDU fragments your point list into many RPMs regardless of how the local device is tuned.
   `[CERT]` §271.9
9. **`udpPort` is a String holding `"0xBAC0"`**, not an int. `[CERT]` §271.12
10. **Property 139 is stripped** from `getPossibleProperties()` unconditionally. `[CERT]` §271.4

---

## 271.17 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| Who-Is service choice = 8 | `BacnetUnconfirmedServiceChoice:12 int WHO_IS = 8` | `[CERT]` |
| Type decision keys on ASN type, not object type | `PointLearn.toTypes()` switch on `asnType`; object type only via `writableFirst` + 2 predicates | `[CERT]` |
| Multi-state → Enum is a hardcoded triple | `isMultiStatePresentValue: propId==85 && objectType ∈ {13,14,19}` | `[CERT]` |
| Binary → Boolean is a type-string check | `isBinaryPv: "bacnet:BacnetBinaryPv".equals(info.getType())` | `[CERT]` |
| `dataType` self-fills from first ASN tag | `if (getDataType().length() == 0) setDataType(AsnUtil.getAsnTypeName(tag))` | `[CERT]` |
| Priority array is probed, not tabled | `checkForPriorityArray` = try `readProperty(oid, 87, 0)` / catch → FALSE | `[CERT]` |
| Poll batch cap | `getMaxDataSize = min(device, local) maxAPDU - 5` | `[CERT]` |
| COV defaults off | `BBacnetTuningPolicy useCov defaultValue = "false"` | `[CERT]` |
| objectTypes.xml scope | file header: "ANSI/ASHRAE 135-2012 … Protocol_Revision 14 … addenda for Protocol_Revision 16" | `[CERT]` |
| No BACtalk/Alerton driver | zero `bactalk` hits corpus-wide; `alerton` only in `honIrmConfig` branding + `vendors.xml` id 18 | `[CERT]` |
| COV resubscribe = lifetime ÷ 2 | `RESUBSCRIPTION_FACTOR = 30000L; // 60sec/min * 1000ms/sec / 2` — resolved in B272 §272.2 | `[CERT]` |

Marker tally: **[CERT] 11 / [EXT-KB] 1 section (§271.13 external tools)**.

---

## 271.x — Connections

- **B23** — object model, property IDs, PDU shapes, COV/priority/networking. This block supplies the
  algorithms §23.4/§23.5 named but did not open. Delta #5.
- **B133** — the APDU/ASN.1/segmentation bytes *under* this pipeline. `AsnInputStream.peekApplicationTag()`,
  used here as the type oracle, is documented there.
- **B28** — cross-protocol discovery framework. **Corrected by §271.15** (3 factual, 3 cross-ref).
- **B7** — driver framework / ProxyExt pipeline. `BProxyExt`, `BPollFrequency`, `BReadWriteMode` are its
  vocabulary.
- **B127** — `pcapBacEther.dll`: the raw-Ethernet link layer beneath BACnet/IP.
- **B120 / B77** — Spyder AtomicWriteFile transport; a *different* use of the same stack (file transfer, not
  point discovery).
- **B246** — `honBacnetHelper` descriptor extensions, which extend `javax.baja.bacnet.export.*`
  (the 42 `B*Descriptor` classes this block did not open — **export/server side**, the mirror of everything
  documented here).

### Open gaps this block did NOT close

| ID | Gap | Class |
|---|---|---|
| **B271-G1** | The **export/server side**: 42 `javax.baja.bacnet.export.B*Descriptor` classes — how a Niagara point is published *as* a BACnet object (the inverse pipeline). Untouched here. | STATIC-investigable |
| ~~**B271-G2**~~ | ~~non-Numeric conversion bodies~~ — **CLOSED by [B274] §274.7**. Key result: Boolean coerces by non-zero (and by the `trueText` facet for strings), but **Enum validates against the `BEnumRange` via `msr.get()` instead of coercing** — so a bad ordinal faults rather than producing nonsense. | closed |
| ~~**B271-G3**~~ | ~~COV lifetime unit reconciliation~~ — **CLOSED by [B272] §272.2/§272.3**. | closed |
| **P3-mstp** (from B133) | MS/TP data-link framing: preamble `0x55 0xFF`, frame types, header/data CRC, token passing. Still open. | STATIC-investigable |
| **P3-sc** (from B133) | BACnet/SC websocket transport (`stack.link.sc.*`). Still open. | STATIC-investigable |
| **P3-dyn** (from B133) | Live segmentation window negotiation + SegmentACK/NAK timing. | requires-execution |
| **B271-G4** | Live confirmation of the adaptive `doubleDataSize` back-off and the priority-array probe against a real device. | requires-execution |
