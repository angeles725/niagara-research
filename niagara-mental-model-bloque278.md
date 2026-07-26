# Block 278 — The BACnet export descriptor family (IV): two different ways to expose history, the ReadRange range-type matrix, and Event Enrollment's supported-algorithm whitelist

> Fourth pass against **B271-G1**, closing **B275-G3** — the three largest classes in the package:
> `BBacnetEventEnrollmentDescriptor` (4142 ln), `BBacnetTrendLogDescriptor` (3018) and
> `BBacnetNiagaraHistoryDescriptor` (2600). Together they are **9 760 lines**, a quarter of the package.
>
> Two results worth the read: Niagara ships **two structurally different Trend Log exports** and the
> difference is not cosmetic; and Event Enrollment carries an explicit **whitelist of supported event
> algorithms** that answers "can this station do X" without a live test.
>
> Also records a **doc↔code delta inside Tridium's own javadoc** (§278.2).
>
> **Sources**: original Tridium source, `…/bacnet-rt/javax/baja/bacnet/export/`. Markers: `[CERT]` verbatim ·
> `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 278.1 — Two Trend Log exports, two different things `[CERT]`

Both publish a BACnet **Trend Log** object. They are not alternatives of convenience — they wrap different
Niagara concepts:

| | `BBacnetTrendLogDescriptor` | `BBacnetNiagaraHistoryDescriptor` |
|---|---|---|
| Javadoc | *"exports a Bacnet trend log **extension** to Bacnet"* | *"is the **archive component** which exposes **a Niagara history** to Bacnet as a trend log"* |
| Wraps | `com.tridium.bacnet.history.BIBacnetTrendLogExt` | a Niagara history |
| Agent | — | `@AgentOn(types = "history:IHistory")` |
| Author / date | Craig Gemmill, 12 Aug 03 | Scott Hoye + Craig Gemmill, 4 Nov 03 |
| Lines | 3018 | 2600 |

The distinction, stated plainly `[INFER]`:

- **`BBacnetTrendLogDescriptor`** exports a **trend log extension living on a point** — the
  `BIBacnetTrendLogExt` family. B274 §274.5 showed those are the four `B*CovTrendLogExt` classes extending
  Niagara's `B*CovHistoryExt`. So this path exports history that was **created under BACnet's own
  trend-log semantics** (buffer size, logging type, record count are all first-class).
- **`BBacnetNiagaraHistoryDescriptor`** is an **agent on `history:IHistory`** — it attaches to a history
  that Niagara already has, from any source, and projects it outward as a Trend Log. It is retrofitting a
  BACnet face onto pre-existing Niagara data.

The properties `BBacnetTrendLogDescriptor` serves confirm the first reading `[CERT]`: `Buffer_Size`,
`Log_Buffer`, `Record_Count`, `Total_Record_Count`, `Logging_Type` — the full standard Trend Log surface,
backed by `getLog()` returning a `BIBacnetTrendLogExt`.

---

## 278.2 — A doc↔code delta in Tridium's own javadoc `[CERT]`

`BBacnetNiagaraHistoryDescriptor`'s class javadoc says:

> *"It only supports **'By Time'** requests for the trend log data."*

**The code supports two range types, not one.** Its `readRange` switch:

```java
case RangeReference.BY_SEQUENCE_NUMBER:
  // By Sequence Number is not supported for exported Niagara histories, as the stored
  // history does not have sequence numbers on its records.
  logger.warning("BY_SEQUENCE_NUMBER is not supported for NiagaraHistoryDescriptor, transaction rejected");
  throw new RejectException(BBacnetRejectReason.PARAMETER_OUT_OF_RANGE);

case RangeReference.BY_POSITION:
  ReadLogResult rlr = readRangeByPosition(refIndex, count, maxDataSize);
  return new ReadRangeAck(…);

case RangeReference.BY_TIME:
  ReadLogResult rlr = readRangeByTime(refTime, count, maxDataSize);
  return new ReadRangeAck(…);
```

So `BY_POSITION` **is** implemented, with its own `readRangeByPosition()`. The javadoc is stale or was
never accurate.

The *inline* comment, by contrast, is precise and gives the real reason: **a stored Niagara history has no
sequence numbers on its records**, so `BY_SEQUENCE_NUMBER` cannot be served and is rejected at the protocol
level with `RejectException(PARAMETER_OUT_OF_RANGE)` — a **Reject PDU**, not an Error. That is the correct
BACnet response for a parameter the server cannot honour, and it is the only place in this family observed
throwing `RejectException` from a range request. `[CERT]`

Practical note for anyone writing a client against a Niagara station: **probe the range type.** A Trend Log
exported from a Niagara history will reject `BY_SEQUENCE_NUMBER` outright while accepting the other two,
and the class documentation understates it further by claiming only `BY_TIME`.

---

## 278.3 — The ReadRange matrix `[CERT]`

| Range type | `BBacnetTrendLogDescriptor` | `BBacnetNiagaraHistoryDescriptor` |
|---|---|---|
| `BY_POSITION` | ✅ (line 1083) | ✅ `readRangeByPosition` |
| `BY_TIME` | ✅ (1104) | ✅ `readRangeByTime` |
| `BY_SEQUENCE_NUMBER` | ✅ (1128) | ❌ **RejectException** |

Both wrap the same four-rung ladder documented in B277 §277.1, with `LOG_BUFFER` as their single list
property (guards at lines 1031 / 1244 / 1283 for the TrendLog's readRange / addListElements /
removeListElements).

One detail that connects to the wire layer `[CERT]`:

```java
int maxDataSize = -1;
if (rangeReference instanceof BacnetConfirmedRequest)
  maxDataSize = ((BacnetConfirmedRequest) rangeReference).getMaxDataLength();
…
ReadLogResult rlr = readRangeByPosition(refIndex, count, maxDataSize);
```

The range read is **bounded by the requesting client's max-APDU**, taken from the confirmed request itself
and pushed down into the record reader. The returned `ReadRangeAck` carries `resultFlags`, `itemCount` and
`firstSequenceNumber` so the client knows whether more records remain. That is the server-side counterpart
of the APDU sizing arithmetic in B133 §133.9 and the client-side poll batching of B271 §271.9 — the same
constraint, enforced at a third place. `[INFER]` on the pairing.

Note `firstSequenceNumber` is returned as `NOT_USED` when `itemCount == 0` `[CERT]`.

---

## 278.4 — Event Enrollment: the newest class in the package `[CERT]`

```java
/**
 * BBacnetEventEnrollmentDescriptor exposes a Niagara event to Bacnet.
 *
 * @author Sandipan Aich on 5/4/2017
 * @since Niagara 3 Bacnet 1.0
 */
```

**Author and date stand out**: every other class in this package is Craig Gemmill, 2002–2004 (with
`BacnetWritableDescriptor` by Joseph Chandler in 2015, B275 §275.6). This one is **Sandipan Aich, 2017** —
and it is simultaneously the **largest** class in the package at 4142 lines. `[INFER]`: Event Enrollment was
implemented, or substantially rewritten, more than a decade after the rest of the export family.

What it resolves to is the key structural fact `[CERT]`:

```java
BPointExtension pointExt = (BPointExtension) getObject();
…
target = (BComponent) pointExt.getParent();
```

**A BACnet Event Enrollment object materialises as a Niagara `BPointExtension`** — an alarm extension
hanging off a point — and the enrollment's *target* is that extension's parent component. So the mapping is:

```
BACnet Event Enrollment object
   └─ eventEnrollmentOrd / objectPropertyReference
        └─ BPointExtension  (a Niagara alarm ext)
             └─ getParent() → the monitored BComponent
```

`typeOfEvent` is a `BBacnetEventType` slot, `Flags.DEFAULT_ON_CLONE | Flags.READONLY`, defaulting to
`BBacnetEventType.none`, and it is **set from the incoming write** rather than configured:
`setTypeOfEvent(BBacnetEventType.make(eventParam.getChoice()))` `[CERT]`. The choice tag of the
`Event_Parameters` CHOICE *is* the event type — a remote client writing `Event_Parameters` implicitly
selects the algorithm.

---

## 278.5 — The supported-algorithm whitelist `[CERT]`

`checkEventType(int)` is an explicit accept/reject table, with Tridium's own comment:

> *"These event types are not supported at all. Other types may not be supported based on the referenced
> object — that is checked in `configureExt`."*

**Accepted (11):**

| `BBacnetEventType` | |
|---|---|
| `CHANGE_OF_STATE` | `COMMAND_FAILURE` |
| `FLOATING_LIMIT` | `OUT_OF_RANGE` |
| `DOUBLE_OUT_OF_RANGE` | `SIGNED_OUT_OF_RANGE` |
| `UNSIGNED_OUT_OF_RANGE` | `BUFFER_READY` |
| `CHANGE_OF_CHARACTERSTRING` | `CHANGE_OF_DISCRETE_VALUE` |
| `NONE` | |

**Rejected — `throw new OutOfRangeException(…)` (11 named + `RESERVED` + `default`):**

`CHANGE_OF_BITSTRING` · `CHANGE_OF_VALUE` · `COMPLEX_EVENT_TYPE` · `BUFFER_READY_DEPRECATED` ·
`CHANGE_OF_LIFE_SAFETY` · `EXTENDED` · `UNSIGNED_RANGE` · `RESERVED` · `ACCESS_EVENT` ·
`CHANGE_OF_STATUS_FLAGS` · `CHANGE_OF_RELIABILITY`

Two things worth flagging:

1. **`CHANGE_OF_VALUE` is rejected as an *event* algorithm.** COV as a *subscription* service is fully
   supported (B272/B273); COV as an Event Enrollment algorithm is not. Different mechanisms, easily
   conflated by name.
2. **The rejection is logged at `Level.FINE` only** `[CERT]`:
   `logger.fine(getObjectId() + ": event type " + tag(eventType) + " is not supported")`, and then the
   exception propagates. A client gets a typed error, but the station log says nothing at default verbosity
   about *which* algorithm was refused. `[INFER]` on the diagnostic consequence.

The whitelist is a **first gate only** — the comment says a type may still be refused later in
`configureExt` depending on the referenced object. So acceptance here is necessary, not sufficient.

---

## 278.6 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| Two structurally different history exports | the two class javadocs, quoted | `[CERT]` |
| NiagaraHistory is an agent on `history:IHistory` | `@AgentOn(types = "history:IHistory")` | `[CERT]` |
| TrendLog wraps `BIBacnetTrendLogExt` | import + `getLog()` returning that type | `[CERT]` |
| ⇒ one exports a point extension, the other retrofits an existing history | composed from both | `[INFER]` |
| NiagaraHistory javadoc says "only By Time" | verbatim | `[CERT]` |
| …but `BY_POSITION` is implemented | `case RangeReference.BY_POSITION: … readRangeByPosition(...)` | `[CERT]` |
| `BY_SEQUENCE_NUMBER` rejected, with reason | inline comment + `throw new RejectException(PARAMETER_OUT_OF_RANGE)` | `[CERT]` |
| TrendLog supports all three range types | cases at 1083 / 1104 / 1128 | `[CERT]` |
| ReadRange bounded by the client's max-APDU | `((BacnetConfirmedRequest) rangeReference).getMaxDataLength()` | `[CERT]` |
| `firstSequenceNumber` = NOT_USED when empty | `(rlr.itemCount > 0) ? rlr.firstSequenceNumber : NOT_USED` | `[CERT]` |
| EventEnrollment authored 2017 by a different author | `@author Sandipan Aich on 5/4/2017` | `[CERT]` |
| EventEnrollment resolves to a `BPointExtension` | `BPointExtension pointExt = (BPointExtension) getObject();` | `[CERT]` |
| `typeOfEvent` derived from the CHOICE tag | `BBacnetEventType.make(eventParam.getChoice())` | `[CERT]` |
| 11 algorithms accepted, 11+ rejected | the `checkEventType` switch, both arms enumerated | `[CERT]` |
| `CHANGE_OF_VALUE` rejected as an event algorithm | it is in the rejecting arm | `[CERT]` |
| Rejection logged at FINE only | `logger.fine(...)` before the throw | `[CERT]` |
| Whitelist is a first gate only | comment: *"Other types may not be supported based on the referenced object"* | `[CERT]` |

Tally: **[CERT] 15 / [INFER] 2.**

---

## 278.7 — Delta recorded

| Source | Says | Reality |
|---|---|---|
| `BBacnetNiagaraHistoryDescriptor` class javadoc (Tridium) | *"It only supports 'By Time' requests for the trend log data."* | `BY_POSITION` and `BY_TIME` are both implemented; only `BY_SEQUENCE_NUMBER` is rejected, because stored Niagara histories carry no sequence numbers. §278.2 |

No prior block of this corpus needed correction by this one.

---

## 278.x — Connections and remaining scope

- **B274 §274.5** — the `B*CovTrendLogExt` classes over `B*CovHistoryExt`; §278.1 places them as what
  `BBacnetTrendLogDescriptor` exports.
- **B275 / B276 / B277** — parts I–III of this family. **B275-G3 closed here.**
- **B133 §133.9** — APDU sizing; §278.3 is the third place the same max-APDU constraint is enforced.
- **B34** — alarm framework; §278.4's `BPointExtension` target is an alarm ext of that framework.
- **B23 §23.14-23.15** — the Event Enrollment and Trend Log object models this implements.

### B271-G1 remaining — the package is now substantially covered

| ID | Gap | Class |
|---|---|---|
| **B277-G1** | `Weekly_Schedule` / `Exception_Schedule` **value encoding** — how a `BWeeklySchedule`'s day schedules become BACnet TimeValue arrays. | STATIC-investigable |
| **B276-G1** | Rest of `BacnetDescriptorUtil`: `DiscreteTotalizerExt` linking, `getPointForElapsedActiveTime`, `areTrendLogAndPointCompatible`. | STATIC-investigable |
| **B276-G2** | Binary/MultiState writable descriptors assumed parallel to Analog; only Analog read line-by-line. | STATIC-investigable |
| **B278-G1** (new) | `configureExt` in `BBacnetEventEnrollmentDescriptor` — the **second** gate, which refuses algorithms based on the referenced object. §278.5 established only that it exists. | STATIC-investigable |
| **B278-G2** (new) | `Log_Buffer` **record encoding** — how a Niagara history record becomes a BACnet `BACnetLogRecord`. `readRangeByPosition`/`readRangeByTime` were traced at the dispatch level, not at the encoder. | STATIC-investigable |
| **P3-mstp** | MS/TP framing. Open since B133. **Next goal — META 1 is essentially complete.** | STATIC-investigable |
| **P3-sc** | BACnet/SC transport. Open since B133. | STATIC-investigable |
