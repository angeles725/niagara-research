# Block 277 — The BACnet export descriptor family (III): the four non-point descriptors — Schedule, Calendar, File, NotificationClass — and the one-object/one-list pattern that governs them

> Third pass against **B271-G1**, closing **B275-G2**. These four descriptors are the ones that
> [B275] §275.1 showed descend **directly from `BComponent`**, bypassing `BBacnetEventSource` entirely —
> they have no event/alarm surface and no `BControlPoint` behind them. Each wraps a completely different
> Niagara subsystem.
>
> This block also supplies the real implementations of `readRange` / `addListElements` /
> `removeListElements`, which [B276] §276.5 found were refusing stubs at the point tier.
>
> **Sources**: original Tridium source, `…/bacnet-rt/javax/baja/bacnet/export/`. Markers: `[CERT]` verbatim ·
> `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 277.1 — The governing pattern: one Niagara object, one list property `[CERT]`

All four follow the same shape, and it is worth stating before the details because it makes the rest
predictable:

| Descriptor | Lines | Niagara object exposed | Its **one** list property | List id |
|---|---|---|---|---|
| `BBacnetScheduleDescriptor` (abstract) | 2980 | `javax.baja.schedule.BWeeklySchedule` | `List_Of_Object_Property_References` | 54 |
| `BBacnetCalendarDescriptor` | 1601 | `javax.baja.schedule.BCalendarSchedule` | `Date_List` | 23 |
| `BBacnetNotificationClassDescriptor` | 1940 | `javax.baja.alarm.BAlarmClass` | `Recipient_List` | 102 |
| `BBacnetFileDescriptor` | 1460 | `javax.baja.file.BIFile` | *(none)* | — |

Each of the three list-bearing descriptors implements the three list methods with an identical guard
`[CERT]` — quoting the Schedule, with Calendar and NotificationClass differing only in the constant:

```java
public final RangeData readRange(RangeReference rangeReference) throws RejectException
{
  getSchedule();
  if (schedule == null)     return new ReadRangeAck(OBJECT, TARGET_NOT_CONFIGURED);
  int propertyId = rangeReference.getPropertyId();
  if (!hasProperty(propertyId))
                            return new ReadRangeAck(property, unknownProperty);
  if (propertyId != LIST_OF_OBJECT_PROPERTY_REFERENCES)
                            return new ReadRangeAck(services, propertyIsNotA_List);
  if (rangeReference.getPropertyArrayIndex() != NOT_USED)
                            return new ReadRangeAck(property, propertyIsNotAnArray);
  int rangeType = rangeReference.getRangeType();
  …
}
```

So the family's list contract is a **four-step ladder** repeated verbatim in every implementer:
`TARGET_NOT_CONFIGURED` → `unknownProperty` → `propertyIsNotA_List` → `propertyIsNotAnArray` → work.
B276 §276.5's point-tier stubs implement the first three rungs and stop — they are the degenerate case of
this same pattern, not a different design. `[INFER]`

The javadocs name the intent plainly `[CERT]`:
*"BBacnetFileDescriptor exposes a BIFile to Bacnet as a File object"*;
*"BBacnetNotificationClassDescriptor is the extension that allows a BAlarmClass …"*.

---

## 277.2 — Schedule: five subclasses that differ by one method `[CERT]`

`BBacnetScheduleDescriptor` is abstract, 2980 lines, and holds essentially all the behaviour. Its five
concrete subclasses (593 / 414 / 600 / 417 / 320 lines) exist almost entirely to answer **one question**:

```java
// BBacnetBooleanScheduleDescriptor
final boolean isScheduleTypeLegal(BWeeklySchedule sched) { return sched instanceof BBooleanSchedule; }
// BBacnetNumericScheduleDescriptor
final boolean isScheduleTypeLegal(BWeeklySchedule sched) { return sched instanceof BNumericSchedule; }
// BBacnetEnumScheduleDescriptor
final boolean isScheduleTypeLegal(BWeeklySchedule sched) { return sched instanceof BEnumSchedule; }
// BBacnetStringScheduleDescriptor
final boolean isScheduleTypeLegal(BWeeklySchedule sched) { return sched instanceof BStringSchedule; }
```

plus *"the ASN type to use in encoding the TimeValues for this schedule"* (javadoc verbatim) and, for
Boolean and Enum, an `isEqual(int ansTypeOfRefObj, int asnTypeOfSchedule)` override that reconciles the
schedule's datatype against the referenced object's.

**`BBacnetDynamicScheduleDescriptor` has no `isScheduleTypeLegal` at all** `[CERT]` — it is the untyped
variant, which is consistent with its name and its smaller size (320 lines).

So the type parallelism of the point branch (B275 §275.1's 3×2 matrix) reappears here as a 4+1 fan:
**one datatype per Niagara schedule class**, mapped straight through. `[INFER]`

Structural slots on the base `[CERT]`: `status`, `faultCause`, `scheduleOrd` (facet
`TARGET_TYPE = "baja:Component"`), `objectId`, `objectName`, `listOfObjectPropertyReferences`,
`priorityForWriting`, `description`, `reliability`, `writePresentValue`.

The BACnet properties it serves `[CERT]`: `Effective_Period`, `Weekly_Schedule`, `Exception_Schedule`,
`List_Of_Object_Property_References`, `Priority_For_Writing`, `Schedule_Default` — the full standard
Schedule object.

---

## 277.3 — The Schedule write-through: Niagara links are NOT the mechanism `[CERT]`

The most consequential finding in this block. Tridium's own javadoc on
`writeListOfObjectPropertyReferences`, verbatim:

> *"Write the list of target references. **Niagara links are NOT exposed to BACnet.** However, internal
> references may still be put into the list. Niagara will accomplish writes to **ALL** targets in the list
> using WriteProperty. For external writes, this is a BACnet WriteProperty-Request that is sent using the
> stack. For internal writes, we use the `writeProperty()` API of `BIBacnetExportObject`. Note that the
> target reference **must be BACnet-writable**, or this write will fail."*

Three things follow:

1. **A BACnet Schedule object exported from Niagara does not drive its targets through Niagara links.** It
   drives them through `WriteProperty` — the same service a remote client would use. The Niagara wiresheet
   links a scheduler normally has are invisible to and unused by the BACnet representation.
2. **The mechanism is uniform for local and remote targets.** An entry pointing at another object in the
   same station goes through `BIBacnetExportObject.writeProperty()` — the very API documented in
   B275 §275.5 — rather than through a direct component write. The station talks to itself over its own
   BACnet server API.
3. **"The target reference must be BACnet-writable or the write will fail"** connects straight to
   B276 §276.4: for a writable point target, "BACnet-writable" means *a `bacnetValueInN` slot exists for the
   priority the schedule writes at* (`priorityForWriting`). A schedule pointed at an exported point whose
   priority level was never linked will fail silently at write time, and the failure surfaces as a BACnet
   error on an internal call, not as a Niagara link fault. `[INFER]` on the diagnosis path; the quoted
   contract is `[CERT]`.

There is also a cross-descriptor fixup in the exception-schedule path `[CERT]`:

```java
if (specialEvent.getDays() instanceof BScheduleReference) {
  BOrd ref = ((BScheduleReference)specialEvent.getDays()).getRef();
  …
  BCalendarSchedule cal = (BCalendarSchedule)((BBacnetCalendarDescriptor)c).getObject();
  ((BScheduleReference)specialEvent.getDays()).setRef(cal.getSlotPathOrd());
}
```

A BACnet Exception_Schedule entry that references a Calendar **object** is rewritten into a Niagara
`BScheduleReference` pointing at the underlying `BCalendarSchedule`'s slot path — the descriptor resolves
the BACnet-level reference into a Niagara-level ORD. The two subsystems are stitched at write time.

---

## 277.4 — Calendar `[CERT]`

Thinnest of the four in behaviour. Exposes `BCalendarSchedule`; its single list property is `Date_List`
(23), served by the same four-rung ladder at lines 655 / 944 / 984 for readRange / addListElements /
removeListElements respectively.

Its role in the family is mostly to be *referenced*: §277.3 showed the Schedule descriptor reaching into
`BBacnetCalendarDescriptor.getObject()` to resolve exception-schedule references. `[INFER]`

---

## 277.5 — File: a `BIFile` as a BACnet File object `[CERT]`

Javadoc: *"BBacnetFileDescriptor exposes a BIFile to Bacnet as a File object."*

Properties served, and how each is derived:

| BACnet property | Source |
|---|---|
| `File_Size` | the `BIFile` |
| `Archive` | **computed**: `getArchiveTime().isAfter(file.getLastModified())` |
| `Read_Only` | `file.isReadonly()` — delegated to the Niagara file |
| `File_Access_Method` | the `fileAccessMethod` slot |
| `Description` | the descriptor's own slot |
| *anything else* | `UNKNOWN_PROPERTY` |

Two notes:

- **`Archive` is synthesised, not stored.** It is true when the recorded archive time is later than the
  file's last modification — i.e. "this file has not changed since it was archived". A clean derivation
  with no extra state. `[CERT]`
- **`File_Access_Method` defaults to `streamAccess` and the slot is `Flags.READONLY`** `[CERT]`:
  `newProperty(Flags.READONLY, BBacnetFileAccessMethod.streamAccess, null)`. Record access is therefore
  not selectable through this slot in the shipped configuration. `[INFER]` on the consequence.

**`AtomicReadFile` / `AtomicWriteFile` do not appear in this class** `[CERT]` (zero matches). The descriptor
exposes the File object's *properties* only; the atomic file services are handled by the service layer
above it. That is the mirror of B120's client-side `AtomicWriteFile` usage for Spyder downloads — the
service and the object model are separate concerns here.

---

## 277.6 — NotificationClass: a `BAlarmClass` as a BACnet Notification Class `[CERT]`

Javadoc: *"BBacnetNotificationClassDescriptor is the extension that allows a BAlarmClass …"*

Its list property is `Recipient_List` (102), on the same ladder (lines 676 / 964 / 1004). It also handles
`Priority` (1030) and reads through `getAlarmClass()`.

This is the export-side counterpart of the `bacnetAlarmRouter` documented in B34: that module routes
*inbound* BACnet intrinsic events into Niagara alarm classes; this descriptor publishes a Niagara
`BAlarmClass` *outward* as a BACnet Notification Class so remote devices can subscribe to it as an alarm
destination. `[INFER]` on the pairing; the class mapping is `[CERT]`.

---

## 277.7 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| Four descriptors, four different Niagara subsystems | imports + `getObject()` in each: `BWeeklySchedule`, `BCalendarSchedule`, `BAlarmClass`, `BIFile` | `[CERT]` |
| Each has exactly one list property | the `!= <CONSTANT>` guard in all three list methods of each class | `[CERT]` |
| Four-rung ladder repeated verbatim | Schedule `readRange` quoted; Calendar/NotifClass guards at the cited lines | `[CERT]` |
| Point-tier stubs are the degenerate case | comparison with B276 §276.5 | `[INFER]` |
| 5 schedule subclasses differ by `isScheduleTypeLegal` | all four bodies quoted; Dynamic has none | `[CERT]` |
| Schedule does NOT use Niagara links | javadoc verbatim: *"Niagara links are NOT exposed to BACnet"* | `[CERT]` |
| Internal writes go through `writeProperty()` API | javadoc verbatim | `[CERT]` |
| Target must be BACnet-writable | javadoc verbatim | `[CERT]` |
| ⇒ a schedule targeting an unlinked priority fails at write time | composed with B276 §276.4 | `[INFER]` |
| Exception-schedule calendar refs rewritten to Niagara ORDs | `setRef(cal.getSlotPathOrd())` | `[CERT]` |
| `Archive` is computed from timestamps | `getArchiveTime().isAfter(file.getLastModified())` | `[CERT]` |
| `File_Access_Method` is READONLY, default streamAccess | `newProperty(Flags.READONLY, BBacnetFileAccessMethod.streamAccess, null)` | `[CERT]` |
| No Atomic file services in the descriptor | zero matches for `atomicRead`/`atomicWrite` in the class | `[CERT]` (negative) |
| NotificationClass is the outbound pair of bacnetAlarmRouter | class mapping certain; pairing derived | `[INFER]` |

Tally: **[CERT] 10 / [INFER] 4.**

---

## 277.x — Connections and remaining scope

- **B275** — part I (hierarchy, contract, dispatch). §277.1 fills in the list-method half of the contract.
- **B276** — part II. §277.1 resolves its §276.5 stubs into the general pattern; §277.3 depends on its
  §276.4 access-control finding.
- **B34** — alarm framework; §277.6 is the outbound counterpart of `bacnetAlarmRouter`.
- **B120** — client-side `AtomicWriteFile`; §277.5 notes the service/object-model separation.
- **B23 §23.11-23.13** — the Schedule / Calendar / Notification Class object models this implements.

### B271-G1 remaining

| ID | Gap | Class |
|---|---|---|
| **B275-G3** | TrendLog (3018), NiagaraHistory (2600), EventEnrollment (4142) — the three largest classes in the package. **Next.** | STATIC-investigable |
| **B276-G1** | Rest of `BacnetDescriptorUtil`: `DiscreteTotalizerExt` linking, `getPointForElapsedActiveTime`, `areTrendLogAndPointCompatible`. | STATIC-investigable |
| **B276-G2** | Binary/MultiState writable descriptors assumed parallel to Analog; only Analog was read line-by-line. | STATIC-investigable |
| **B277-G1** (new) | The Schedule's `Weekly_Schedule` / `Exception_Schedule` **encoding** — how a Niagara `BWeeklySchedule`'s day-schedules become BACnet TimeValue arrays. Only the reference/list machinery was traced, not the value encoding. | STATIC-investigable |
| **P3-mstp** | MS/TP framing. Open since B133. | STATIC-investigable |
| **P3-sc** | BACnet/SC transport. Open since B133. | STATIC-investigable |
