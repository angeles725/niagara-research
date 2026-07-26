# Block 284 — The trend-record ASN encoder: three range types collapsing into one, 32-bit sequence wrap-around, encode-then-test size fitting, and the `BACnetLogRecord` byte layout

> **Closes B282-G2**, and with it the wire half of B278-G2 — the Log_Buffer question is now fully answered.
>
> [B282] §282.5 probed for the encoder by grepping `writeOpeningTag` against `BBacnetTrendRecord` and found
> nothing, concluding the emission "happens somewhere else". **It does — in `BBacnetLogRecord`.** The probe
> looked for the wrong class name. That is recorded as a method note in §284.6, not glossed over.
>
> **Sources**: Vineflower decompile of `com.tridium.bacnet.history.BacnetTrendLogUtil` (1147 ln) and
> `BBacnetLogRecord`. Markers: `[CERT]` verbatim · `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 284.1 — The three protocol steps

**Step 1 — project blocks.** B23 §23.15 holds the ASN.1 schema (`BacnetTrendLogEntry ::= CHOICE
{ logDatum [0] / timeChange [1] REAL / statusFlags [2] BIT STRING }`). §284.4 shows the emitted bytes match
it. Not restated.

**Step 2 — niagara-help.** `find "ReadRange trend log buffer encoding"` → **zero matches** `[CERT]`
(negative). **Fourth consecutive negative** for descriptor internals — the calibration recorded in the loop
brief holds exactly.

**Step 3 — code.**

---

## 284.2 — Two descriptors, two `readRangeByPosition`s `[CERT]`

```
BacnetTrendLogUtil.readRangeByPosition(BIBacnetTrendLogExt ext, long position, int count, int maxSize, Integer pointAsnType)
BBacnetNiagaraHistoryDescriptor.readRangeByPosition(long refIndex, int count, int maxSize)   // private, its own
```

The two history exports of B278 §278.1 **do not share a range reader**. `BBacnetTrendLogDescriptor`
delegates to the shared `BacnetTrendLogUtil`; `BBacnetNiagaraHistoryDescriptor` implements its own private
version. That is the structural reason their range-type support differs (B278 §278.3) rather than an
arbitrary restriction. `[INFER]`

This block traces the **shared** one.

---

## 284.3 — All three range types collapse into `readRangeBySequence` `[CERT]`

```java
// by position
long seqNumAtPosition = firstUsedSeqNum + position - 1L;
var17 = readRangeBySequence(ext, seqNumAtPosition, count, maxSize, pointAsnType);

// by time
seqNum = ((BBacnetTrendRecord)data.get()).getSequenceNumber();
return readRangeBySequence(ext, seqNum, count, maxSize, pointAsnType);

// (third call site, line 667)
var40 = readRangeBySequence(ext, firstPossibleSeqNum, recordCount, maxSize, pointAsnType);
```

**There is one reader.** BY_POSITION and BY_TIME are *translations into a sequence number*, then the same
code path runs. That explains why `BBacnetNiagaraHistoryDescriptor` — whose records have **no** sequence
numbers (B282 §282.4) — could not reuse this util and had to write its own.

### Sequence numbers are 32-bit and wrap `[CERT]`

```java
public static long MAX_SEQ_NUM = 4294967295L;          // 2^32 - 1
…
long recordCount     = conn.getRecordCount(dataHistory);
long lastUsedSeqNum  = ext.getTotalRecordCount();
long firstUsedSeqNum = lastUsedSeqNum - recordCount + 1L;
if (firstUsedSeqNum < 1L) firstUsedSeqNum += MAX_SEQ_NUM;      // wrap-around
long seqNumAtPosition = firstUsedSeqNum + position - 1L;
```

The oldest sequence number is derived from `Total_Record_Count` minus the buffer depth, and **wraps at
2³²−1** — sequence numbering is 1-based, so a computed value below 1 is pushed back up by `MAX_SEQ_NUM`.
`[CERT]`; the 1-based reading is `[INFER]` from the `+ 1L` and the `< 1L` test.

### BY_TIME excludes the reference timestamp `[CERT]`

```java
Cursor<BHistoryRecord> data = conn.timeQuery(dataHistory, referenceTime, null).cursor();
while (data.next()) {
  BBacnetDateTime recTime = new BBacnetDateTime(data.get().getTimestamp());
  if (!recTime.toBAbsTime().equals(referenceTime)) {
    firstDataTimestamp = data.get().getTimestamp();
    break;
  }
}
if (firstDataTimestamp == null) return EMPTY_RESULT;
seqNum = ((BBacnetTrendRecord)data.get()).getSequenceNumber();
```

The scan **skips records whose timestamp equals the reference** and takes the first one that differs — i.e.
strictly *after* the reference time, not *at or after*. A ReadRange BY_TIME at an exact record timestamp
returns the records following it, not including it. `[CERT]` on the loop; `[INFER]` on the "strictly after"
characterisation.

An empty or missing history returns `EMPTY_RESULT` — a shared constant
`new ReadLogResult(0L, -1L, new byte[0], false, false, false)` `[CERT]`.

---

## 284.4 — The size fit is **encode-then-test**, and the overflowing record is dropped `[CERT]`

```java
AsnOutputStream out = new AsnOutputStream();
…
do {
   writeLogRecord(recx, pointAsnType, out);
   if (exceedsMaxSize(maxSize, itemData, out)) {
      moreItems = true;
      break;                    // ← the record just encoded is discarded
   }
   appendToItemData(itemData, out);
   itemCount++;
   …
} while (itemCount < itemLimit);

return new ReadLogResult(itemCount, seqNum, itemData.toByteArray(), includesFirst, includesLast, moreItems);
```

with

```java
private static boolean exceedsMaxSize(int maxSize, ByteArrayOutputStream itemData, AsnOutputStream out) {
   return maxSize > 0 && itemData.size() + out.size() > maxSize;
}
```

Findings:

1. **Each record is fully encoded into a scratch buffer, then measured.** There is no size prediction — the
   driver encodes, checks, and on overflow **throws the encoding away** and sets `moreItems`. The record is
   not partially emitted; the client gets it on the next ReadRange. `[CERT]`
2. **`maxSize <= 0` means unbounded** — the guard short-circuits. B278 §278.3 showed `maxDataSize` is
   initialised to `-1` and only set when the request is a `BacnetConfirmedRequest`, so a non-confirmed path
   encodes without limit. `[CERT]`
3. **`ReadLogResult` carries the three BACnet result flags**: `includesFirst`, `includesLast`, `moreItems` —
   FIRST_ITEM / LAST_ITEM / MORE_ITEMS. Plus `itemCount` and the starting sequence number. `[CERT]`
4. There is a **`prependToItemData`** helper alongside `appendToItemData` `[CERT]`:

```java
private static void prependToItemData(ByteArrayOutputStream itemData, ByteArrayOutputStream temp, AsnOutputStream out) {
   temp.reset(); itemData.writeTo(temp); itemData.reset(); out.writeTo(itemData); temp.writeTo(itemData);
}
```

   — a full buffer shuffle to put a newly-encoded record **before** the accumulated ones. That is the
   backwards-read case: BACnet ReadRange accepts a **negative count**, meaning "N records *preceding* the
   reference", and the records must still come out in chronological order. `[INFER]` on the negative-count
   link; the helper is `[CERT]`. Note it is O(n) per record — a backwards read of N records copies the
   buffer N times. `[INFER]`

---

## 284.5 — The `BACnetLogRecord` byte layout `[CERT]`

`BacnetTrendLogUtil.writeLogRecord` prepares, `BBacnetLogRecord.writeLogRecord` emits:

```java
private static void writeLogRecord(BBacnetTrendRecord rec, Integer pointAsnType, AsnOutputStream out) {
   var logDatum = rec.getStatus().isNull() ? BBacnetNull.DEFAULT : rec.get(rec.getValueProperty());
   int logDatumChoice = getLogDatumChoice(rec, pointAsnType);
   BBacnetLogRecord.writeLogRecord(rec.getTimestamp(), logDatum, logDatumChoice,
                                   rec.getStatus(), rec.getLogEvent().getLong(), out);
}
```

and the emitter itself, in two overloads:

```java
// BBacnetDateTime overload
out.writeOpeningTag(0);  timestamp.writeAsn(out);                       out.writeClosingTag(0);
out.writeOpeningTag(1);  writeLogDatum(out, logDatum, logDatumChoice,
                                       trendEvent, BacnetBitStringUtil.getBStatus(statusFlags));
                                                                        out.writeClosingTag(1);
out.writeBitString(2, statusFlags);

// BAbsTime overload
out.writeOpeningTag(0);  out.writeDate(timestamp); out.writeTime(timestamp);  out.writeClosingTag(0);
out.writeOpeningTag(1);  writeLogDatum(…);                                    out.writeClosingTag(1);
out.writeBitString(2, BacnetBitStringUtil.getBacnetStatusFlags(statusFlags));
```

**This matches B23 §23.15's schema exactly** — context tag [0] timestamp, [1] logDatum, [2] statusFlags —
and adds what the schema could not show: `[0]` and `[1]` are **constructed** (opening/closing tag pairs)
while `[2]` is a primitive context-tagged bit string. The `BAbsTime` overload writes date and time as two
separate primitives inside tag 0. `[CERT]`

And the null rule, inside `writeLogDatum` `[CERT]`:

```java
if (status.isNull()) { out.writeNull(7); return; }
```

**A null-status record encodes as ASN NULL at context tag 7** — the `logDatum` CHOICE's null branch. So the
`logDatum` selection happens twice: once in Java (`BBacnetNull.DEFAULT` substituted for the value) and once
in the encoder (tag 7 short-circuit). `[INFER]` on the redundancy.

`getLogDatumChoice(rec, pointAsnType)` is the CHOICE discriminator, and it takes the **point's ASN type**
as a parameter — the same `pointAsnType` threaded through every `readRange*` signature. So the log datum is
tagged according to the *point's* declared type, not the record's runtime value. `[INFER]`; the threading is
`[CERT]`.

---

## 284.6 — Method note: B282's probe used the wrong name

B282 §282.5 concluded the ASN emission "happens somewhere else" after grepping `writeOpeningTag` and
finding `BBacnetEventLogRecord` and `BBacnetAccumulatorRecord` but not `BBacnetTrendRecord`.

**The conclusion was right; the reasoning was under-powered.** The emitter is `BBacnetLogRecord` — a class
whose name differs from both the record type (`BBacnetTrendRecord`) and the descriptor. A grep keyed on the
*record* class could not find an emitter named after the *BACnet object*. The correct probe was to follow
the call chain from `writeRecord`/`readRange*` rather than to search by class name.

Recorded because the same shape of mistake is easy to repeat: **in this package, the ASN emitter for X is
often named after the BACnet concept, not the Niagara class.** `BBacnetEventLogRecord` and
`BBacnetAccumulatorRecord` — the two the grep *did* find — follow the same convention, which in hindsight
was the clue.

---

## 284.7 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| niagara-help: nothing on ReadRange encoding | one query, zero matches (4th consecutive) | `[CERT]` (negative) |
| Two separate `readRangeByPosition` implementations | both signatures, different classes | `[CERT]` |
| ⇒ the two history exports don't share a reader | derived | `[INFER]` |
| All range types funnel into `readRangeBySequence` | three call sites quoted | `[CERT]` |
| `MAX_SEQ_NUM = 4294967295L` (2³²−1) | constant declaration | `[CERT]` |
| Oldest seq derived from Total_Record_Count − depth, wraps | the four-line computation | `[CERT]` |
| BY_TIME skips records equal to the reference | `if (!recTime.toBAbsTime().equals(referenceTime))` | `[CERT]` |
| Empty/missing history → shared `EMPTY_RESULT` | constant + two early returns | `[CERT]` |
| Size fit is encode-then-test | `writeLogRecord(...)` then `exceedsMaxSize(...)` then `break` | `[CERT]` |
| Overflowing record is discarded, not truncated | `break` before `appendToItemData` | `[CERT]` |
| `maxSize <= 0` = unbounded | `maxSize > 0 &&` short-circuit | `[CERT]` |
| Result carries FIRST/LAST/MORE flags | `ReadLogResult(itemCount, seqNum, bytes, includesFirst, includesLast, moreItems)` | `[CERT]` |
| `prependToItemData` exists (buffer shuffle) | method quoted in full | `[CERT]` |
| ⇒ it serves negative-count backwards reads, O(n) per record | derived | `[INFER]` |
| Record layout: [0] constructed timestamp, [1] constructed logDatum, [2] primitive bitstring | both overloads quoted | `[CERT]` |
| Matches B23 §23.15's schema | comparison | `[CERT]` |
| Null status → `writeNull(7)` | verbatim | `[CERT]` |
| logDatum tagged by the point's ASN type | `pointAsnType` threaded into `getLogDatumChoice` | `[INFER]` |

Tally: **[CERT] 14 / [INFER] 4.**

---

## 284.x — Connections and gaps

- **B282** — **G2 closed here**, and §282.5's probe reasoning corrected in §284.6. Together B282 + B284
  fully answer the original B278-G2.
- **B278 §278.1 / §278.3** — §284.2 supplies the structural reason the two history exports differ in
  range-type support: separate readers, and only one is sequence-based.
- **B282 §282.4** — the sequence-number finding; §284.3 adds the 32-bit wrap.
- **B23 §23.15** — schema confirmed against emitted bytes. No correction.
- **B133 §133.4** — the opening/closing context-tag codec used throughout.
- **B278 §278.3** — `maxDataSize` from the confirmed request; §284.4 shows how it is consumed.

| ID | Gap | Class |
|---|---|---|
| **B284-G1** (new) | `getLogDatumChoice(rec, pointAsnType)` — the full CHOICE-discriminator mapping (which tag each datatype gets). Only its null branch (tag 7) was read. | STATIC-investigable |
| **B284-G2** (new) | `BBacnetNiagaraHistoryDescriptor`'s **private** `readRangeByPosition` — the second, unshared implementation. | STATIC-investigable |
| **B284-G3** (new) | The `trendEvent` parameter's role in `writeLogDatum` — passed through but its encoding branch not traced. | STATIC-investigable |
| ~~B282-G2~~ | ~~trend record ASN encoder~~ | **closed** |
| **Next** | **B279-G1** — `EmstpStateMachine`, the host↔co-processor protocol. | STATIC-investigable |
