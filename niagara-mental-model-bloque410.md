# Block 410 — .hdb Retention and Rollover Policy: In-Place Circular Eviction, Not File Rotation

> **Research focus:** `database` (gap **DB9**, low-priority). Answers the file-level behavior
> of BFullPolicy when BCapacity is reached — specifically whether ROLL causes per-record
> deletion from the oldest page (trimToCapacity) or file rotation — and what happens to
> existing records when the collection-interval changes.
>
> Scope: the `BFileHistoryTable` hierarchy (the `.hdb` write path), the concrete subclasses
> `BFixedLengthHistoryTable`/`PageManager` (VERSION_1) and `BRecordStoreHistoryTable`/
> `RecordStore` (VERSION_2), and the docSource API classes. Does NOT re-derive the `.hdb`
> binary format (remitted to [Block 33]) or the archive provider chain (remitted to [Block 407]).
>
> Subject version: N4.14.0.162 (Vineflower decompile + docSource shipped-source tree).
>
> Sources — Vineflower decompiled (cited as `[CERT] file:line`):
> - `BFixedLengthHistoryTable.java` → full path:
>   `/home/cristian/modules/Prototipos/modulos/organized/history/history-rt/vineflower/com/tridium/history/file/fixed/BFixedLengthHistoryTable.java`
> - `PageManager.java` →
>   `/home/cristian/modules/Prototipos/modulos/organized/history/history-rt/vineflower/com/tridium/history/file/fixed/PageManager.java`
> - `Page.java` (fixed) →
>   `/home/cristian/modules/Prototipos/modulos/organized/history/history-rt/vineflower/com/tridium/history/file/fixed/Page.java`
> - `BRecordStoreHistoryTable.java` →
>   `/home/cristian/modules/Prototipos/modulos/organized/history/history-rt/vineflower/com/tridium/history/file/recstore/BRecordStoreHistoryTable.java`
> - `RecordStore.java` →
>   `/home/cristian/modules/Prototipos/modulos/organized/history/history-rt/vineflower/com/tridium/history/file/recstore/RecordStore.java`
> - `BFileHistoryTable.java` →
>   `/home/cristian/modules/Prototipos/modulos/organized/history/history-rt/vineflower/com/tridium/history/file/BFileHistoryTable.java`
> - `BHistoryDbTable.java` →
>   `/home/cristian/modules/Prototipos/modulos/organized/history/history-rt/vineflower/com/tridium/history/db/BHistoryDbTable.java`
>
> Sources — docSource shipped-source tree (cited as `[CERT] file:line`):
> - `BHistoryConfig.java` (docSource) →
>   `/home/cristian/modules/Prototipos/modulos/organized/docSource/docSource-doc/extracted/history-rt/javax/baja/history/BHistoryConfig.java`
> - `BFullPolicy.java` (docSource) →
>   `/home/cristian/modules/Prototipos/modulos/organized/docSource/docSource-doc/extracted/history-rt/javax/baja/history/BFullPolicy.java`
> - `BCapacity.java` (docSource) →
>   `/home/cristian/modules/Prototipos/modulos/organized/docSource/docSource-doc/extracted/history-rt/javax/baja/history/BCapacity.java`
> - `BCollectionInterval.java` (docSource) →
>   `/home/cristian/modules/Prototipos/modulos/organized/docSource/docSource-doc/extracted/history-rt/javax/baja/history/BCollectionInterval.java`
>
> **docSource dual-tree note (METHODOLOGY §5):** citations to docSource files use the
> `docSource/...extracted/history-rt/javax/baja/history/` prefix; citations to the Vineflower
> decompiled tree use the `history/history-rt/vineflower/com/tridium/history/...` prefix. Both
> exist for the same logical class; line numbers differ between trees.
>
> Method: Vineflower decompile — call chain traced from `append()` → policy check → store trim;
> docSource for API defaults and slot flags.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction.
>
> `database` focus. Connects [Block 33] (`.hdb` format + truncation — this block refines §33.6.2
> cost claim and §33.5.3 DIRTY_CACHE_SIZE). [Block 45]. [Block 174].

---

## 410.1 — Gap Answer: ROLL = In-Place Oldest-Record Eviction; No File Rotation `[CERT]`

When `BFullPolicy.ROLL` is active and capacity is reached, the `.hdb` file is **never rotated**
during normal append operations. No new file is created, no rename chain (`name.tmp` →
`name.chk` → `name.hdb`) is triggered. Instead:

1. The new record is appended to the **last (newest) page** of the file.
2. The oldest record is removed in-place from the **first (oldest) page** via `trimFromStart()`.
3. When an entire oldest page is fully drained, its slot is logically "freed" and the `firstPage`
   pointer advances to the next circular slot — the physical page bytes remain in the file and will
   be **overwritten** when the circular write head wraps around.
4. File size does **not** shrink; the circular ring of pages reuses space naturally.

`[CERT]` `PageManager.java:215-222` (VERSION_1 fixed-length):
```java
if (this.recordCount > this.capacity) {
    if (this.firstPage.trimFromStart(this.recordCount - this.capacity) == 0) {
        this.nextFirst();      // advance firstPage pointer
        this.writeHeader();
    }
    this.recordCount = this.capacity;
}
```

`[CERT]` `RecordStore.java:234-244` (VERSION_2 recstore):
```java
if (roll) {
    while (this.header.getRecordCount() > maxRecords) {
        this.firstPage.trimFromStart();   // remove oldest record slot
        this.header.decrementRecordCount();
        if (this.firstPage.isEmpty()) {
            this.flush();
            this.firstPage = this.firstPage.nextPage();
            this.header.setFirstPage(this.firstPage.index);
            this.flush();
        }
    }
}
```

**File rotation NEVER happens during ordinary append**. The only context in which a
rename-chain rotation occurs is the explicit `doResize()` path, which is NOT the
normal capacity-full path (see §410.5).

---

## 410.2 — Capacity Enforcement Call Chain `[CERT]`

The table below maps the full call chain from append entry to policy dispatch:

| Step | Class | Method | Effect |
|---|---|---|---|
| 1 | `BFileHistoryTable` | `append(BIHistoryRecordSet)` | Iterates records; calls `doAppend(rec)` per record |
| 2a | `BFixedLengthHistoryTable` | `doAppend(rec)` | Checks record-count capacity; dispatches to `PageManager.append()` |
| 2b | `BRecordStoreHistoryTable` | `doAppend(rec)` | Checks record-count capacity; dispatches to `RecordStore.append()` |
| 3a | `PageManager` | `append(rec)` | Appends to lastPage; trims from firstPage if count > capacity |
| 3b | `RecordStore` | `append(rec)` | Appends to lastPage; trims from firstPage while count > maxRecords |

`[CERT]` `BFixedLengthHistoryTable.java:108-128` (VERSION_1 doAppend):
```java
protected boolean doAppend(BHistoryRecord newRecord) throws IOException {
    int recordCount = this.pageManager.getRecordCount();
    BCapacity capacity = this.getConfig().getCapacity();
    if (capacity.isByRecordCount()) {
        int maxRecords = capacity.getMaxRecords();
        if (recordCount >= maxRecords) {
            BFullPolicy fullPolicy = this.getConfig().getFullPolicy();
            if (fullPolicy == BFullPolicy.stop) {
                return false;           // STOP: record silently dropped
            }
        }
    }
    this.pageManager.append(newRecord); // ROLL: always reaches here
    return true;
}
```

`[CERT]` `BRecordStoreHistoryTable.java:99-119` (VERSION_2 doAppend — note `==` not `>=`):
```java
if (recordCount == maxRecords) {
    if (fullPolicy == BFullPolicy.stop) { return false; }
}
this.store.append(newRecord);
return true;
```

`[INFER]` The `>=` vs `==` difference between implementations is unlikely to be
observable in practice because `recordCount` increments by exactly 1 per append, but it is a
code-level discrepancy worth flagging: VERSION_1 gates at `>= maxRecords`, VERSION_2 at `== maxRecords`
(with the trim loop inside `RecordStore.append()` restoring the invariant immediately after
the new record is written).

---

## 410.3 — BFullPolicy.STOP: Silent Drop, No Exception `[CERT]`

When `BFullPolicy.STOP` fires (capacity reached and policy is stop):

- `doAppend()` returns `false` — no exception is thrown.
- `BFileHistoryTable.append()` increments `added` by 0 for that record.
- The caller receives an `added` count less than `newRecords.getRecordCount()`.
- **No log warning is emitted** for the drop — the caller must detect the discrepancy by
  comparing requested vs. returned count.

`[CERT]` `BFileHistoryTable.java:663-668`:
```java
if (!this.doAppend(rec)) {
    return added;    // returns immediately; remaining records in the set not written
}
added++;
```

`[INFER]` This means `STOP` policy stops writing the *entire record set* on the first
dropped record, not just that single record — `return added` exits the loop for all
remaining records in `newRecords`.

---

## 410.4 — BFullPolicy Default: slot vs. class-level `[CERT]`

There is a split between the class-level static DEFAULT and the effective slot default:

| Source | Value |
|---|---|
| `BFullPolicy.DEFAULT` (class static) | `stop` |
| `BHistoryConfig.fullPolicy` slot default | `BFullPolicy.roll` |
| `BHistoryConfig.capacity` slot default | `BCapacity.makeByRecordCount(500)` |
| `BCapacity.DEFAULT` (class static) | `UNLIMITED` |

`[CERT]` `BFullPolicy.java:66`: `public static final BFullPolicy DEFAULT = stop;`
`[CERT]` `BHistoryConfig.java:415`: `public static final Property fullPolicy = newProperty(0, BFullPolicy.roll, null);`
`[CERT]` `BHistoryConfig.java:386`: `public static final Property capacity = newProperty(0, BCapacity.makeByRecordCount(500), null);`
`[CERT]` `BCapacity.java:237`: `public static final BCapacity DEFAULT = UNLIMITED;`

**Operational default is ROLL (500 records)** — `BHistoryConfig` overrides the class-level
STOP default with `BFullPolicy.roll` in its slot declaration. [Block 33] §33.2.3 and §33.6.2
correctly document the effective 500-record/ROLL defaults.

---

## 410.5 — File Rotation: doResize(), NOT a Capacity-Full Path `[CERT]`

The only path that physically rotates the `.hdb` file is `doResize()`, invoked from two triggers:

**Trigger 1 — Open with excess records:**
`[CERT]` `BHistoryDbTable.java:99-103`:
```java
if (capacity.isByRecordCount() && this.getRecordCount() > capacity.getMaxRecords()) {
    BFullPolicy fullPolicy = this.getConfig().getFullPolicy();
    this.resize(capacity, fullPolicy);
}
```
Fires when the file is opened and contains more records than the current capacity
(e.g. capacity was reduced externally). Calls `doResize()` which triggers the rotation.

**Trigger 2 — Config section overflow:**
`[CERT]` `BFileHistoryTable.java:731`:
```java
if (numBytes > this.dataOffset - 12) {
    this.doResize(this.getConfig().getCapacity(), this.getConfig().getFullPolicy());
}
```
Fires from `writeConfig()` when the serialized `BHistoryConfig` no longer fits in the
pre-allocated config section (bytes 12 to dataOffset). Rare in practice because the config
section is allocated at 1600 bytes (`DEFAULT_DATA_OFFSET`) on creation.

**The rotation sequence** (`ResizePrivilegedAction.run()`, `[CERT]` `BFileHistoryTable.java:902-979`):
1. Create `name.tmp` (new `BFileHistoryTable` with new capacity/policy)
2. Copy all records from old file to new file via `newTable.append()` — the ROLL policy is applied during this copy, naturally trimming to the new capacity
3. Delete old file
4. Rename `name.tmp` → `name.chk`
5. Rename `name.chk` → `name.hdb`
6. Reopen the updated file

`[INFER]` The two-step rename (`.tmp` → `.chk` → `.hdb`) provides a partial crash barrier:
if the station crashes between steps 4 and 6, both `.chk` and `.hdb` may coexist, but
the original `.hdb` was already deleted so the `.chk` must be re-renamed manually.
There is no explicit `.chk` recovery at station startup (not observed in this tree).

---

## 410.6 — Collection-Interval Change: Metadata Update, Records Untouched `[CERT]`

When `BCollectionInterval` changes on a live `BHistoryConfig`:

**Event chain:**
`[CERT]` `BHistoryConfig.java:681-697` (`changed()` callback):
- `historyConfigChanged(this, interval_property)` fires on the parent `BIHistorySource`
- This propagates to `BHistoryDbTable.configChanged()` → `doConfigChanged()`

`[CERT]` `BFileHistoryTable.java:749-757` (`doConfigChanged()`):
```java
public void doConfigChanged() {
    if (this.isOpen() && !this.resizing) {
        try {
            this.writeConfig();
        } catch (Exception var2) { ... }
    }
}
```

`[CERT]` `BFileHistoryTable.java:718-746` (`writeConfig()`):
- Serializes the new `BHistoryConfig` (including new interval) to a temp byte buffer
- If serialized size ≤ `dataOffset - 12`: writes in-place to `.hdb` bytes 12..dataOffset
- If serialized size > `dataOffset - 12`: calls `doResize()` (rotation — rare)

**Existing records in the data section are never touched.** The interval value is metadata
only — it does NOT trigger resampling, timestamp adjustment, or any migration of existing
records. Records written before the interval change remain exactly as stored.

`[CERT]` `BCollectionInterval.java:192-198` — the interval field is purely descriptive
metadata with pre-defined constants for 1min/5min/15min/30min/1hr and an IRREGULAR variant:
```java
public static final BCollectionInterval DEFAULT = new BCollectionInterval(BRelTime.MINUTE);
public static final BCollectionInterval IRREGULAR = makeIrregular();
```

`[CERT]` `BHistoryConfig.java:469` — the `interval` slot is `Flags.OPERATOR | Flags.READONLY`,
making it read-only for operators and queryable; the `BIntervalHistoryExt` scheduler is what
uses this value at collection time, not the `.hdb` write path.

**Gotcha:** changing `interval` on an existing history does not retroactively label past
records with a new interval. A chart rendering the history will use the *current* interval
setting to interpolate gaps, not the interval that was active when older records were written.
`[INFER]`

---

## 410.7 — Storage-Size Capacity: Declared, Not Enforced at Append Time `[CERT]`

`BCapacity.makeByStorageSize(long bytes)` is fully constructible and serializable, but
the append-time enforcement only applies to `isByRecordCount()`:

`[CERT]` `RecordStore.java:184`:
```java
if (capacity.isByRecordCount()) {
    ...
    roll = true;
}
// No else-if for isByStorageSize()
```

`[CERT]` `BFixedLengthHistoryTable.java:112`:
```java
if (capacity.isByRecordCount()) { ... }
// No storage-size branch
```

`[CERT]` `BHistoryDbTable.java:100`:
```java
if (capacity.isByRecordCount() && this.getRecordCount() > capacity.getMaxRecords()) {
```
(The open-time resize gate also only fires on record-count capacity.)

`[INFER]` A history configured with `BCapacity.makeByStorageSize()` effectively acts
as unlimited capacity at append time — neither the STOP nor the ROLL enforcement fires,
and `maxRecords` stays at `Integer.MAX_VALUE` in `RecordStore.append()`. Storage-size
capacity appears to be an API placeholder that was not wired through the implementation
in this version.

---

## 410.8 — Corrections to Block 33 §33.5.3 and §33.6.2

**Correction 1 — DIRTY_CACHE_SIZE actual value:**
[Block 33] §33.5.3 states: `"RecordStore.DIRTY_CACHE_SIZE = ? (constante, empírico ~16 páginas)"`.

`[CERT]` `RecordStore.java:27`: `private static final int DIRTY_CACHE_SIZE = 5;`

The actual value is **5** dirty pages, not the empirically guessed ~16. Block 33 used `javap -p`
(no bodies) and estimated from context; the Vineflower body now shows the literal.

**Correction 2 — ROLL cost is O(1), not O(page_size):**
[Block 33] §33.6.2 states: `"Cost O(page_size). Con recsPerPage=1000 y capacity=500 →
trimFromStart debe saltar 500 pages → degenerado."` This is wrong.

`[CERT]` `Page.java:46-53` (`Page.trimFromStart(int trimCount)`):
```java
public int trimFromStart(int trimCount) {
    this.first += trimCount;   // O(1) — just increment an integer
    this.dirty = true;
    return this.getRecordCount();
}
```
`trimFromStart(N)` increments the `first` pointer by N in **O(1)** — it does not iterate
over records or pages. When a page is fully drained, `nextFirst()` advances the `firstPage`
pointer (O(1) with a possible disk read for the new firstPage). The per-append cost of ROLL
is therefore **O(1)** regardless of capacity or recsPerPage.

**Correction 3 — ROLL append sequence:**
[Block 33] §33.6.2 says `Page.trimFromStart()` fires when capacity is reached. The actual
sequence is:
1. New record appended first (to lastPage)
2. `recordCount` incremented
3. `trimFromStart()` fires afterward (if count now exceeds capacity)

The oldest record is removed AFTER the new one is written, not before. This is an
overwrite-then-evict model, not an evict-then-overwrite model.

---

## 410.9 — Connections

- **[Block 33]** — §33.5.3/§33.5.4 documents the `.hdb` binary format (MAGIC, header layout,
  RecordStore structure, data section organization). Block 410 refines §33.5.3 (DIRTY_CACHE_SIZE
  actual = 5, not ~16) and §33.6.2 (ROLL cost is O(1), not O(page_size); append-then-evict order;
  no file rotation during normal ROLL). The format remit stands — Block 410 covers the behavioral
  retention/rollover layer, not the format.

- **[Block 45]** — touches history operational behavior in station context. Block 410 documents
  the underlying `.hdb` ROLL/STOP mechanics that determine what operators see when capacity limits
  are hit.

- **[Block 174]** — history context. Block 410 provides the storage-layer retention detail that
  informs capacity planning decisions (ROLL is O(1) per record, file does not grow on ROLL,
  storage-size capacity is unenforced at the implementation level).
