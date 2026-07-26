# Block 281 — Schedule value encoding: the `ScheduleSupport` protocol-revision chain, the Weekly_Schedule 7-array, and the Niagara↔BACnet calendar conversions

> Closes **B277-G1**. [B277] traced the Schedule descriptor's *reference and list* machinery (targets,
> write-through by `WriteProperty`, the calendar-reference fixup) but not the **value encoding**: how a
> Niagara `BWeeklySchedule` becomes BACnet `BACnetTimeValue` arrays, and how Exception_Schedule
> `SpecialEvent`s are serialised.
>
> Three protocol steps run in order, per the B279 §279.9 correction. **Step 1 paid off**: B23 §23.11
> already held the object structure, so this block covers only the encoding it lacked.
>
> **Sources**: original Tridium source for `BBacnetScheduleDescriptor` (`javax.baja.bacnet.export`);
> Vineflower decompile for `com.tridium.bacnet.schedule.ScheduleSupport*`. Markers: `[CERT]` verbatim ·
> `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 281.1 — What each protocol step returned

**Step 1 — project blocks.** `B23 §23.11` already documents the *structure*: the Schedule object's
properties, the `BWeeklySchedule { BWeekSchedule week; BCalendarSchedule holidays; BDailySchedule[] byDay[7] }`
shape, and — importantly — it already names the `ScheduleSupport0 / 4 / 16` split by protocol revision.
**Nothing here is re-derived**; this block starts where §23.11 stops, at the ASN bytes.

**Step 2 — niagara-help.** Queries `"Weekly Schedule BACnet export"` and `guide-search "schedule export bacnet"`
both return **no matches** `[CERT]` (negative). There is no official Tridium guide on the schedule *export*
encoding. Recorded so the next pass does not retry it.

**Step 3 — code.** Original source for the descriptor, decompile for the encoders.

---

## 281.2 — The `ScheduleSupport` chain: encoding is versioned by protocol revision `[CERT]`

```java
ScheduleSupport16 extends ScheduleSupport4 extends ScheduleSupport0
```

| Class | Lines | Role |
|---|---|---|
| `ScheduleSupport0` | **1477** | the base encoder — all the real work |
| `ScheduleSupport4` | 158 | overrides `getVersion()` and two `Comparator`s |
| `ScheduleSupport16` | 26 | overrides `getVersion()` and little else |

Selection happens once, from the peer's protocol revision `[CERT]`:

```java
supp = ScheduleSupport0.makeForProtocolRevision(protocolRevision, supp);
```

So **the same Niagara schedule serialises differently depending on the protocol revision of the device
asking**. The deltas at revision 4 are ordering rules (the two overridden comparators sort special events),
not layout `[INFER]` — the comparator bodies were not read, gap **B281-G1**.

The descriptor delegates every encode to `supp`; it holds no serialisation logic of its own `[CERT]`.

---

## 281.3 — `Weekly_Schedule`: a BACnetARRAY of 7 `[CERT]`

```java
private PropertyValue readWeeklySchedule(int ndx) {
  synchronized (asnOut) {
    asnOut.reset();
    switch (ndx) {
      case 0:                     // array size
        return …AsnUtil.toAsnUnsigned(7);

      case -1:                    // whole array
        for (int i = BAC_MONDAY; i <= BAC_SUNDAY; i++)
          supp.encodeDailySchedule(schedule.get(BWeekday.make(i % 7)),
                                   schedule.getDefaultOutput(), asnOut, getAsnType());
        return …asnOut.toByteArray();

      case BAC_MONDAY: … case BAC_SUNDAY:      // single element
        supp.encodeDailySchedule(schedule.get(BWeekday.make(ndx % 7)),
                                 schedule.getDefaultOutput(), asnOut, getAsnType());
        return …asnOut.toByteArray();

      default:
        return …new NErrorType(PROPERTY, INVALID_ARRAY_INDEX);
    }
  }
}
```

Three things:

1. **The three BACnetARRAY access forms** are the same ones B275 §275.7 found for `Property_List` and
   B274 §274.3 for `Active_COV_Subscriptions`: `ndx == 0` → size, `ndx == -1` → whole array, `ndx == N` →
   element. A consistent family-wide idiom. `[INFER]`
2. **`BAC_MONDAY = 1`** `[CERT]` (`BacnetConst:52`), so the array is indexed 1..7 Monday-first — the BACnet
   convention.
3. **`% 7` is the calendar bridge.** `BWeekday.make(i % 7)` maps BACnet's Monday=1..Sunday=7 onto Niagara's
   `BWeekday` ordinals, where Sunday is 0. Index 7 (BACnet Sunday) becomes `7 % 7 = 0` (Niagara Sunday).
   The whole conversion is that one modulo. `[CERT]` on the code; `[INFER]` on the Sunday=0 reading, which
   §281.5 independently corroborates.

The **default output** is passed into every encode (`schedule.getDefaultOutput()`) — Niagara's schedule
default participates in the encoding rather than being a separate property write. `[CERT]`

---

## 281.4 — `Exception_Schedule` `[CERT]`

```java
case 0:                      // count the special events
  SlotCursor<Property> c = schedule.getSpecialEvents().getProperties();
  int cnt = 0;
  while (c.next(BDailySchedule.class)) cnt += 1;
  return …AsnUtil.toAsnUnsigned(cnt);

case -1:
  supp.encodeExceptionScheduleWithIdx(schedule.getSpecialEvents(), schedule.getDefaultOutput(),
                                      asnOut, getAsnType(),
                                      BBacnetNetwork.localDevice().getObjectId());
  return …asnOut.toByteArray();

default:
  if (ndx < 0) return …INVALID_ARRAY_INDEX;
  supp.encodeSpecialEvent(ndx, …);
```

Note the array size is **counted live** by walking the `specialEvents` container for `BDailySchedule`
children — there is no stored count. And `encodeExceptionScheduleWithIdx` receives the **local device's
object id**, which is how a SpecialEvent referencing a Calendar gets its `BACnetObjectPropertyReference`
filled in with this device's identity `[INFER]` — the counterpart of the reference *rewrite* B277 §277.3
documented on the write path.

---

## 281.5 — `TimeValue` encoding `[CERT]`

The array wrapper:

```java
private void encodeTimeValues(TimeValue[] tvs, int contextTag, AsnOutput out, int asnType) {
  if (tvs != null) {
    out.writeOpeningTag(contextTag);
    for (int i = 0; i < tvs.length; i++) this.encodeTimeValue(tvs[i], out, asnType);
    out.writeClosingTag(contextTag);
  }
}
```

A standard ASN.1 constructed sequence — opening tag, N elements, closing tag (B133 §133.4 documents the tag
codec itself). **A null `tvs` writes nothing at all**, not even an empty pair.

Each element:

```java
private void encodeTimeValue(TimeValue tv, AsnOutput out, int asnType) {
  out.writeTime(tv.hour, tv.minute, tv.second, tv.hund());
  if (tv.value == null)                    out.writeNull();
  else if (tv.value instanceof Boolean)    this.encodeValue((Boolean)tv.value, out, asnType);
  else if (tv.value instanceof Double)     this.encodeValue((Double)tv.value, out, asnType);
  else if (tv.value instanceof Integer)    this.encodeValue((Integer)tv.value, out, asnType);
  else if (tv.value instanceof String)     this.encodeValue((String)tv.value, out, asnType);
  else if (log().isTraceOn())              log().trace("tv value=" + tv.value + …);
}
```

So a `BACnetTimeValue` is **Time followed by a primitive value**, and:

- **A null value encodes as ASN NULL** — the BACnet idiom for "no scheduled value at this time".
- **An unrecognised Java type is silently dropped**: the final `else if` only logs, and only when trace is
  on. The Time was *already written* by then, so the output would be a Time with no value — a malformed
  TimeValue. Reachable only for a value type outside {Boolean, Double, Integer, String}. `[INFER]` on the
  malformation; the fall-through is `[CERT]`.

### Value coercion to the schedule's ASN type

```java
private void encodeValue(Boolean b, AsnOutput out, int asnType) {
  switch (asnType) {
    case 1: out.writeBoolean(b);                          break;   // BOOLEAN
    case 2: out.writeUnsignedInteger(b ? 1L : 0L);        break;   // Unsigned
    case 3: out.writeSignedInteger(b ? 1 : 0);            break;   // INTEGER
    case 4: out.writeReal(b ? 1.0 : 0.0);                 break;   // REAL
    case 5: out.writeDouble(b ? 1.0 : 0.0);               break;   // Double
    …
```

The same **coerce-to-declared-type** pattern as the client-side `BBacnetNumericProxyExt` of B271 §271.6,
running in the opposite direction. `getAsnType()` is the per-subclass method B277 §277.2 identified as one
of the two things distinguishing the five Schedule subclasses — this is where it is consumed. `[CERT]`

---

## 281.6 — Date encoding: four separate Niagara↔BACnet conversions `[CERT]`

```java
public void encodeDate(BDateSchedule dsch, AsnOutput out, boolean calculateWeekDay) {
  int year  = dsch.getYear();
  int month = dsch.getMonth();
  int day   = bajaScheduleDayToBacnetDayOfMonth(dsch.getDay());
  int weekDay = dsch.getWeekday();
  if (calculateWeekDay && month >= 0 && year >= 0 && day >= 0)
    weekDay = BAbsTime.getWeekday(year, BMonth.make(month), day).getOrdinal();

  out.writeDate(year  < 0 ? 255   : year - 1900,
                month < 0 ? month : month + 1,
                day,
                weekDay == 0 ? 7 : weekDay);
}
```

Four conversions in one call:

| Field | Niagara | BACnet | Rule |
|---|---|---|---|
| **year** | absolute (e.g. 2026) | offset from 1900 | `year - 1900`; **negative → 255** (the BACnet "unspecified" wildcard) |
| **month** | 0-based | 1-based | `month + 1`; negative passes through as the wildcard |
| **day** | Baja schedule day | day-of-month | via `bajaScheduleDayToBacnetDayOfMonth()` |
| **weekday** | **Sunday = 0** | **Sunday = 7** | `weekDay == 0 ? 7 : weekDay` |

The weekday rule **independently confirms** the `% 7` reading of §281.3: Niagara puts Sunday at 0, BACnet at
7, and both directions of that mapping appear in the code. `[CERT]`

`calculateWeekDay` lets the encoder *derive* the weekday from the date rather than trust the stored one —
but only when year, month and day are all concrete (no wildcards). A partially-specified date keeps whatever
weekday was stored. `[CERT]`

There is a second overload taking a `contextTag` (`encodeDate(dsch, contextTag, out)`) which applies the
same four rules but **without** the `calculateWeekDay` option `[CERT]`.

---

## 281.7 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| niagara-help has nothing on schedule export encoding | two queries, zero matches | `[CERT]` (negative) |
| `ScheduleSupport16 → 4 → 0` chain | class declarations | `[CERT]` |
| Encoder chosen by peer protocol revision | `makeForProtocolRevision(protocolRevision, supp)` | `[CERT]` |
| SS0 holds the work (1477 vs 158 vs 26 ln) | line counts | `[CERT]` |
| Rev-4 deltas are ordering, not layout | only `getVersion()` + two `Comparator`s overridden | `[INFER]` |
| Weekly_Schedule is a 7-element array | `toAsnUnsigned(7)` for `ndx == 0` | `[CERT]` |
| `BAC_MONDAY = 1` | `BacnetConst:52` | `[CERT]` |
| `% 7` bridges the two weekday conventions | `BWeekday.make(i % 7)` | `[CERT]` |
| Exception_Schedule count is walked live | `while (c.next(BDailySchedule.class)) cnt += 1` | `[CERT]` |
| TimeValues wrapped in opening/closing context tags | `encodeTimeValues` quoted | `[CERT]` |
| Null value → ASN NULL | `if (tv.value == null) out.writeNull()` | `[CERT]` |
| Unknown value type falls through to a trace log | the final `else if (log().isTraceOn())` | `[CERT]` |
| ⇒ that path emits a Time with no value | composed (Time already written) | `[INFER]` |
| Values coerced to the schedule's ASN type | `encodeValue(Boolean, …)` switch quoted | `[CERT]` |
| year−1900, 255 wildcard; month+1; weekday 0→7 | `out.writeDate(...)` quoted | `[CERT]` |
| Weekday derived only when date fully specified | `if (calculateWeekDay && month >= 0 && year >= 0 && day >= 0)` | `[CERT]` |

Tally: **[CERT] 13 / [INFER] 3.**

---

## 281.x — Connections and gaps

- **B277** — G1 closed here. §281.4's device-id argument is the read-side counterpart of §277.3's
  calendar-reference rewrite on the write side.
- **B23 §23.11** — supplied the structure; this block supplies the bytes. No correction needed — §23.11 was
  accurate, including the `ScheduleSupport0/4/16` split.
- **B271 §271.6** — the same coerce-to-declared-type pattern, client side.
- **B133 §133.4** — the opening/closing context-tag codec §281.5 relies on.
- **B275 §275.7 / B274 §274.3** — the shared BACnetARRAY access idiom (size / whole / element).

| ID | Gap | Class |
|---|---|---|
| **B281-G1** (new) | The two `Comparator` overrides in `ScheduleSupport4` — what ordering rule revision 4 imposes on special events. | STATIC-investigable |
| **B281-G2** (new) | `makeDay()` — how a `BDaySchedule` becomes the `TimeValue[]`, including how the default output is folded in. Only its call site was read. | STATIC-investigable |
| **B281-G3** (new) | `writeWeeklySchedule` / the decode direction; only the read/encode path was traced. | STATIC-investigable |
| **Next** | **B278-G2** — Log_Buffer → `BACnetLogRecord` encoding. | STATIC-investigable |
