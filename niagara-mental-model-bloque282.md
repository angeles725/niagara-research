# Block 282 — Trend log record storage: `BBacnetTrendRecord` over Niagara's history record, sequence numbering, and the buffer-purge synthetic record

> Partial close of **B278-G2**. The gap asked how a Niagara history record becomes a BACnet
> `BACnetLogRecord`. It turns out that question has **two halves**, and this block closes the first
> cleanly while declaring the second still open:
>
> 1. **Storage** — what Niagara persists per record so a BACnet Log_Buffer can later be served. **Closed.**
> 2. **ASN encoding at ReadRange time** — the bytes emitted by `readRangeByPosition`/`readRangeByTime`.
>    **Not traced.** See §282.6; the gap is narrowed, not closed.
>
> Three protocol steps run in order. **Step 1 paid off again**: B23 §23.15 already holds the ASN.1 record
> schema, so this block does not restate it.
>
> **Sources**: Vineflower decompile of `com.tridium.bacnet.history`. Markers: `[CERT]` verbatim ·
> `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 282.1 — What the three steps returned

**Step 1 — project blocks.** `B23 §23.15` already documents:

- the `BBacnetTrendLog` property set (`logEnable`, `stopWhenFull`, `bufferSize`, `recordCount`,
  `totalRecordCount`, `notifyType`, `objectPropertyReference`…)
- **the ASN.1 record schema itself**:

```
BacnetTrendLogEntry ::= CHOICE {
  logDatum     [0] CHOICE { null, realValue, enumValue, booleanValue, ... }
  timeChange   [1] REAL           -- delta
  statusFlags  [2] BIT STRING
}
```

- the Trend Log Multiple variant, and the remote-import flow ending in `BacnetTrendLogUtil.readLogResult()`
- **eleven local TrendLogExt variants**, not the four `B*CovTrendLogExt` that [B274] §274.5 found: there are
  also plain (`BBacnetNumericTrendLogExt`…), **Interval** (`BBacnetNumericIntervalTrendLogExt`…) and
  **BitString** flavours, plus five `*TrendLogRemoteExt` for import. B274's four were the COV subset only.
  `[CERT]` (B23's own inventory)

So the schema question was already answered in the corpus. What was missing is the runtime.

**Step 2 — niagara-help.** `find "log buffer trend log record"` → **zero matches** `[CERT]` (negative).
Recorded so this is not retried: there is no official Tridium guide on Log_Buffer encoding, same as the
schedule-encoding result in B281 §281.1.

**Step 3 — code.** `BacnetTrendLogUtil` (1147 ln) and `BBacnetTrendRecord`, both `com.tridium.bacnet.history`
(no `docSource` original — `com.tridium.*`).

---

## 282.2 — Storage model: a Niagara history record with BACnet fields bolted on `[CERT]`

```java
public abstract class BBacnetTrendRecord extends BTrendRecord implements BIStatus
```

`BTrendRecord` is Niagara's own history record type. The BACnet subclass adds exactly what BACnet needs on
top, and its persistence method shows what those are:

```java
out.writeLong(this.getSequenceNumber());
this.getLogEvent().encode(out);
```

Two extra fields per record: a **64-bit sequence number** and a **log event**. Everything else — timestamp,
value, status — comes from the Niagara `BTrendRecord` underneath.

This confirms and sharpens B274 §274.5's finding that *"a Niagara COV history and a BACnet TrendLog are the
same store"*: they are the same store because the BACnet record **is** a Niagara record subclass, not a
parallel structure. `[INFER]`

Two abstract setters define the per-datatype contract `[CERT]`:

```java
public abstract BBacnetTrendRecord set(BAbsTime, BStatus,      long seq, BTrendEvent, BTrendFlags);
public abstract BBacnetTrendRecord set(BAbsTime, BStatusValue, long seq, BTrendEvent, BTrendFlags);
```

— one overload for a status-only record (a log-status event) and one for a value record. Plus
`getLogDatumType()`, which is the CHOICE discriminator of the `logDatum` in B23's schema. `[INFER]`

---

## 282.3 — `writeRecord`: buffer-purge detection and the minus-one-millisecond trick `[CERT]`

The most interesting mechanism in this block.

```java
public static void writeRecord(BIBacnetTrendLogExt ext, BAbsTime timestamp, BStatusValue out) throws IOException {
  if (ext.getEnabled()) {
    BHistoryExt hext = (BHistoryExt)ext;
    BBacnetTrendRecord rec = ext.getRecord();
    BTrendFlags tf = rec.getTrendFlags();
    BBacnetTrendLogAlarmSourceExt almExt = getAlarmExt(ext);
    long sequenceNumber = incrementSequenceNumber(ext.getTotalRecordCount());
    boolean bufferPurged = false;

    try {
      HistorySpaceConnection conn = getHistoryDbConnection(null);
      …
      BIHistory history = getHistory(conn, ext);
      if (history == null || conn.getRecordCount(history) == 0) bufferPurged = true;
      …
    } catch (HistoryException var23) {
      bufferPurged = true;
    }

    if (bufferPurged) {
      BAbsTime timestampMinusOne = timestamp.subtract(BRelTime.make(1L));
      appendRecord(ext, rec.set(timestampMinusOne, out.getStatus(), sequenceNumber,
                                BTrendEvent.LOG_STATUS_ENABLED_BUFFER_PURGED, tf.set(4, true)));
      if (!hext.getStatus().isFault()) {
        ext.setTotalRecordCount(sequenceNumber);
        if (almExt != null) almExt.incrementRecordsSinceNotification();
      }
    }

    BTrendEvent event = BTrendEvent.DEFAULT;
    NErrorType errorFound = getFailureError(ext);
    if (errorFound != null) {
      event = BTrendEvent.makeFailure(errorFound);
      rec.setTrendFlags(tf.set(4, true));
    }
    …
```

Three findings:

1. **"Buffer purged" is detected, not tracked.** Before every write, the code opens a history connection and
   asks whether the history is missing or has **zero records**. Either condition — including a
   `HistoryException` — means the buffer was purged out from under the trend log. There is no flag or
   listener; it is re-derived on each write. `[CERT]`

2. **The purge marker is back-dated by exactly 1 millisecond** — `timestamp.subtract(BRelTime.make(1L))`.
   A synthetic `LOG_STATUS_ENABLED_BUFFER_PURGED` record is appended at `timestamp − 1 ms` so that it
   **sorts before** the real record being written at `timestamp`. That is how a BACnet client reading the
   buffer sees "purged, then data" in the correct order despite both records being created in the same
   call. `[INFER]` on the ordering intent; the subtraction is `[CERT]`.

3. **A read failure becomes a log event, not a dropped record.** `getFailureError(ext)` returning non-null
   produces `BTrendEvent.makeFailure(errorFound)` — the failure is *logged into the buffer* as a record.
   So gaps in a BACnet trend served by Niagara carry an explicit reason rather than simply being absent.
   `[CERT]`

Both the purge and the failure path set **trend flag bit 4** (`tf.set(4, true)`). Its meaning was not
established — gap **B282-G1**.

`writeEvent()` is the sibling entry point, taking a `BTrendEvent` directly and handling
`LOG_STATUS_ENABLED_BUFFER_PURGED` / `LOG_STATUS_DISABLED_BUFFER_PURGED` depending on `ext.getEnabled()`
`[CERT]`:

```java
BTrendEvent evt = ext.getEnabled() ? BTrendEvent.LOG_STATUS_ENABLED_BUFFER_PURGED
                                   : BTrendEvent.LOG_STATUS_DISABLED_BUFFER_PURGED;
```

---

## 282.4 — Sequence numbers `[CERT]`

```java
long sequenceNumber = incrementSequenceNumber(ext.getTotalRecordCount());
…
if (!hext.getStatus().isFault()) ext.setTotalRecordCount(sequenceNumber);
```

The sequence number is **derived from `Total_Record_Count`**, and the count only advances when the extension
is **not in fault**. So a faulted trend log writes its record but does **not** advance
`Total_Record_Count` — meaning the next record reuses the same sequence number. `[INFER]` on the reuse; the
guard is `[CERT]`.

This is also the concrete reason `BBacnetNiagaraHistoryDescriptor` rejects `BY_SEQUENCE_NUMBER` (B278 §278.2):
sequence numbers exist only on `BBacnetTrendRecord`, the BACnet subclass. A plain Niagara history exported
through the *other* descriptor has `BTrendRecord`s with no sequence field at all — exactly what that
descriptor's inline comment said. **The two findings corroborate each other from opposite directions.**
`[INFER]`

---

## 282.5 — What this block does NOT close

The gap asked for the encoding *"to a BACnetLogRecord"*. `BBacnetTrendRecord`'s `out.writeLong(...)` /
`getLogEvent().encode(out)` is **Niagara's persistence encoding**, not BACnet ASN — it writes to the history
store, not to a wire buffer. `[INFER]`, and stated as such rather than glossed.

Probing for the ASN side: `writeOpeningTag` in `bacnet-rt` for trend/log/record classes matches
`BBacnetEventLogRecord` (tags 0 and 1) and `BBacnetAccumulatorRecord` (tag 0), but **not**
`BBacnetTrendRecord` `[CERT]`. So the trend record's ASN emission happens somewhere else — most likely
inside the `readRangeByPosition` / `readRangeByTime` bodies of the two descriptors, which B278 §278.3 traced
only at dispatch level.

**Honest scope statement**: B278-G2 is **narrowed, not closed**. The storage half is documented; the wire
half needs the two `readRange*` bodies plus whichever `ReadLogResult` builder they call. Logged as
**B282-G2**.

> **CLOSED by [B284].** The emitter is **`BBacnetLogRecord.writeLogRecord`** — the probe above searched for
> `BBacnetTrendRecord` and could not find it because, in this package, the ASN emitter is named after the
> **BACnet concept**, not the Niagara class (the two hits the grep *did* return, `BBacnetEventLogRecord` and
> `BBacnetAccumulatorRecord`, follow the same convention and were the clue). The conclusion "emission
> happens elsewhere" was right; the search key was wrong. See B284 §284.6.

---

## 282.6 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| niagara-help has nothing on Log_Buffer encoding | one query, zero matches | `[CERT]` (negative) |
| B23 §23.15 already holds the ASN.1 record schema | quoted from the block | `[CERT]` |
| Eleven local TrendLogExt variants, not four | B23 §23.15 inventory | `[CERT]` |
| `BBacnetTrendRecord extends BTrendRecord` | class declaration | `[CERT]` |
| Adds sequenceNumber + logEvent on persist | `writeLong(getSequenceNumber())` + `getLogEvent().encode(out)` | `[CERT]` |
| ⇒ same store because same class lineage | composed with B274 §274.5 | `[INFER]` |
| Buffer purge detected per write | `getRecordCount(history) == 0` / `catch (HistoryException)` → `bufferPurged = true` | `[CERT]` |
| Purge record back-dated 1 ms | `timestamp.subtract(BRelTime.make(1L))` | `[CERT]` |
| ⇒ so it sorts before the real record | derived | `[INFER]` |
| Read failures logged as records | `BTrendEvent.makeFailure(errorFound)` | `[CERT]` |
| Purge and failure both set trend flag bit 4 | `tf.set(4, true)` in both paths | `[CERT]` |
| Meaning of bit 4 | not established | — (gap B282-G1) |
| Sequence derived from Total_Record_Count | `incrementSequenceNumber(ext.getTotalRecordCount())` | `[CERT]` |
| Count advances only when not in fault | `if (!hext.getStatus().isFault())` | `[CERT]` |
| ⇒ faulted log reuses a sequence number | derived | `[INFER]` |
| Corroborates B278's BY_SEQUENCE_NUMBER rejection | sequence field exists only on the BACnet subclass | `[INFER]` |
| Trend record ASN emission is elsewhere | `writeOpeningTag` matches EventLogRecord/AccumulatorRecord, not TrendRecord | `[CERT]` (negative) |

Tally: **[CERT] 12 / [INFER] 5.**

---

## 282.x — Connections and gaps

- **B278** — G2 **narrowed** here, not closed. §282.4 independently corroborates §278.2's
  `BY_SEQUENCE_NUMBER` finding.
- **B274 §274.5** — sharpened: the shared store is a shared *class lineage*. Also corrected in scope — B274
  saw four COV variants; B23 §23.15 lists eleven local ones.
- **B23 §23.15** — supplied the ASN.1 schema and the full variant inventory. No correction needed.
- **B281** — same shape of result: niagara-help empty, B23 holding the structure, code holding the runtime.

| ID | Gap | Class |
|---|---|---|
| **B282-G1** (new) | `BTrendFlags` bit 4 — set on both the purge and failure paths; meaning not established. | STATIC-investigable |
| **B282-G2** (new, replaces the wire half of B278-G2) | The ASN emission of a trend record: the `readRangeByPosition` / `readRangeByTime` bodies and the `ReadLogResult` builder (`resultFlags`, `itemCount`, `firstSequenceNumber`, `maxDataSize` honouring). | STATIC-investigable |
| **B282-G3** (new) | `BacnetTrendLogUtil.readLogResult()` — the **import** direction (B23 §23.15 step 5), i.e. decoding a remote device's Log_Buffer into a Niagara history. | STATIC-investigable |
| **Next** | **B278-G1** — `configureExt`, the second event-algorithm gate. | STATIC-investigable |
