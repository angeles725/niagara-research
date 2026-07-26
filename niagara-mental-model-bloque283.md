# Block 283 — Event Enrollment's second gate: the per-algorithm `instanceof` matrix, the configure pipeline, and a Tridium TODO that leaves the out-of-range variants unchecked

> Closes **B278-G1**. [B278] §278.5 documented the *first* gate — `checkEventType`, a flat whitelist of 11
> accepted vs 11+ rejected `BBacnetEventType`s — and quoted Tridium's own comment pointing at the second:
>
> > *"Other types may not be supported based on the referenced object — that is checked in `configureExt`."*
>
> This block opens `configureExt` and finds that the real per-object gate is **not** in `configureExt`
> itself but one level deeper, in the eight algorithm-specific configurators it dispatches to. Each is a
> plain `instanceof` test against the **Niagara point** behind the reference.
>
> Also records a **Tridium `TODO`** that admits the four out-of-range variants are not distinguished.
>
> **Sources**: original Tridium source, `javax.baja.bacnet.export.BBacnetEventEnrollmentDescriptor`
> (4142 ln). Markers: `[CERT]` verbatim · `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 283.1 — What the three steps returned

**Step 1 — project blocks.** `B23 §23.14` holds the Event Enrollment *object model*: the property set, and
the polymorphic `eventParameters` per event type (`OutOfRange {deadband, lowLimit, highLimit}`,
`ChangeOfValue {timeDelay, coVIncrement}`, `ChangeOfBitstring {timeDelay, bitMask}`, `FloatingLimit`,
`SignedOutOfRange`, `CommandFailure {feedbackValue}`). **Not restated here.** `[CERT]`

**Step 2 — niagara-help.** `find "Event Enrollment algorithm"` → **zero matches** `[CERT]` (negative).
Third consecutive negative for encoding/configuration internals; the official guides cover Workbench
workflows, not descriptor internals. Registered.

**Step 3 — code.** Original Tridium source (this class is `javax.baja.bacnet.export`).

---

## 283.2 — `configureExt` is a dispatcher, not the gate `[CERT]`

```java
private ErrorType configureExt(BBacnetEventParameter eventParam,
                               BBacnetDeviceObjectPropertyReference objPropRef,
                               BPointExtension pointExt, BComponent target)
{
  try {
    int eventType = eventParam.getChoice();
    switch (eventType) {
      case CHANGE_OF_STATE:            configureChangeOfStateExt(eventParam, objPropRef, pointExt, target); break;
      case COMMAND_FAILURE:            configureCommandFailureExt(eventParam, pointExt, target);            break;
      case FLOATING_LIMIT:             configureFloatingLimitExt(eventParam, pointExt, target);             break;
      case OUT_OF_RANGE:
      case DOUBLE_OUT_OF_RANGE:
      case SIGNED_OUT_OF_RANGE:
      case UNSIGNED_OUT_OF_RANGE:      configureOutOfRangeExt(eventParam, pointExt, target);                break;
      case BUFFER_READY:               configureTrendAlarmExt(eventParam, pointExt, target);                break;
      case CHANGE_OF_CHARACTERSTRING:  configureStringChangeOfStateExt(eventParam, pointExt, target);       break;
      case NONE:                       configureNoneExt();                                                  break;
      case CHANGE_OF_DISCRETE_VALUE:   configureChangeOfDiscreteValueExt(eventParam, objPropRef, pointExt, target); break;

      case CHANGE_OF_BITSTRING: case CHANGE_OF_VALUE: case COMPLEX_EVENT_TYPE:
      case BUFFER_READY_DEPRECATED: case CHANGE_OF_LIFE_SAFETY: case EXTENDED:
      case UNSIGNED_RANGE: case RESERVED: case ACCESS_EVENT:
      case CHANGE_OF_STATUS_FLAGS: case CHANGE_OF_RELIABILITY:
      default:
        throw new EventEnrollmentException("event type " + tag(eventType) + " is not supported",
            new NErrorType(PROPERTY, OPTIONAL_FUNCTIONALITY_NOT_SUPPORTED));
    }
    return null;
  }
  catch (EventEnrollmentException e) { …FINE log…; resetDescriptor(); return e.errorType; }
  catch (PermissionException e)      { …INFO log…; resetDescriptor(); return new NErrorType(PROPERTY, WRITE_ACCESS_DENIED); }
  catch (Exception e)                { …INFO log…; resetDescriptor(); return new NErrorType(PROPERTY, OTHER); }
}
```

Findings:

1. **Its reject list is identical to `checkEventType`'s** (B278 §278.5) — the same eleven types, in the same
   order. The whitelist is enforced twice, and this second copy is what supplies the concrete BACnet error:
   **`(PROPERTY, OPTIONAL_FUNCTIONALITY_NOT_SUPPORTED)`**. B278 §278.5 could only report that
   `checkEventType` threw `OutOfRangeException`; this is the error the client actually receives. `[CERT]`
2. **Eleven accepted types collapse into eight configurators** — the four out-of-range variants
   (`OUT_OF_RANGE`, `DOUBLE_`, `SIGNED_`, `UNSIGNED_`) all fall through to a single
   `configureOutOfRangeExt`. That fall-through is the subject of §283.4.
3. **Every failure path calls `resetDescriptor()`** — a rollback. A partially-configured enrolment is torn
   down rather than left half-built. `[CERT]`
4. **Three distinct error mappings**: `EventEnrollmentException` carries its own typed error;
   `PermissionException` → `WRITE_ACCESS_DENIED` (the Niagara security model reaching BACnet again, as in
   B275 §275.5); anything else → `OTHER`. `[CERT]`

---

## 283.3 — The real second gate: an `instanceof` matrix `[CERT]`

Each configurator opens with a type check on the **Niagara point** behind the reference. The complete
matrix, from the eight guards:

| Line | Guard | Required Niagara type | For |
|---|---|---|---|
| 2664 | `!(sourcePoint instanceof BBooleanPoint)` | Boolean | (source point check) |
| 3006 | `!(target instanceof BBooleanPoint \|\| target instanceof BEnumPoint \|\| target instanceof BNumericPoint)` | **any of three** | change-of-state |
| 3264 | `!(feedbackPoint instanceof BBooleanPoint)` | Boolean | command-failure feedback |
| 3417 | `!(feedbackPoint instanceof BEnumPoint)` | Enum | command-failure feedback (enum variant) |
| 3488 | `!(target instanceof BNumericPoint)` | Numeric | floating-limit |
| 3530 | `!(setpoint instanceof BNumericPoint)` | Numeric | floating-limit **setpoint** |
| 3593 | `!(target instanceof BNumericPoint)` | Numeric | out-of-range |
| 3706 | `!(target instanceof BStringPoint)` | String | change-of-characterstring |

Representative body `[CERT]`:

```java
if (!(target instanceof BNumericPoint))
{
  throw new EventEnrollmentException(
    "referenced object is of type " + target.getType() +
      " and not instanceof NumericPoint, which is required for out-of-range extensions; event type: " +
      BBacnetEventType.tag(eventParam.getChoice()),
    new NErrorType(BBacnetErrorClass.PROPERTY, BBacnetErrorCode.VALUE_OUT_OF_RANGE));
}
```

Three things worth stating:

- **The error is `VALUE_OUT_OF_RANGE`, not `OPTIONAL_FUNCTIONALITY_NOT_SUPPORTED`.** So a client can
  distinguish the two gates by the error it gets back: *"this station never supports that algorithm"* vs
  *"that algorithm exists but not on this object"*. `[INFER]` on the diagnostic use; the two error codes are
  `[CERT]`.
- **Command-failure checks a `feedbackPoint`, not the target** — and it has *two* guards (Boolean at 3264,
  Enum at 3417), meaning the feedback reference is validated separately from the monitored object. `[CERT]`
- **Floating-limit validates the setpoint too** (3530), a second reference beyond the target. `[CERT]`

So the second gate is not one test but a **per-algorithm, sometimes multi-reference type contract**, all
expressed as Java `instanceof` against Niagara point classes rather than against BACnet object types.

---

## 283.4 — The TODO: the four out-of-range variants are not distinguished `[CERT]`

Immediately above the guard at 3593:

```java
// TODO should we check that the object property reference points to a property type that matches the event type?
//  For example, DOUBLE_OUT_OF_RANGE is placed on a NumericPoint tied to a Double parameter?
//  int objType = getRemoteObjectType();
if (!(target instanceof BNumericPoint))
```

Tridium is asking, in its own source, whether it should verify that the *variant* matches the underlying
datatype — and the commented-out `getRemoteObjectType()` call is the check that was not written.

Consequence `[INFER]`: `OUT_OF_RANGE`, `DOUBLE_OUT_OF_RANGE`, `SIGNED_OUT_OF_RANGE` and
`UNSIGNED_OUT_OF_RANGE` are **accepted interchangeably** on any `BNumericPoint`. A client enrolling
`SIGNED_OUT_OF_RANGE` on a point whose values are REAL gets no error at configure time. Whether the
mismatch surfaces later depends on the parameter decoding, which was not traced — logged as **B283-G1**.

The class carries five further TODOs, all in the same "is this correct in every case?" register `[CERT]`:

```
903:  // TODO Remove and replace with getEventState()?  Or, keep and ensure it will work with null returns?
1021: // TODO Ensure this is compatible with all calls to this method.
1105: // TODO What about when event state returns null?
1117: // TODO Check the offnormal algorithm of the BAlarmSourceExt
1930: // TODO Include fault algorithm?
1979: // TODO Check that descriptor object type is valid binary type
```

Six TODOs in one class is unusual for this corpus. Combined with B278 §278.4's finding that this is the
**newest** class in the package (Sandipan Aich, 2017, versus Craig Gemmill 2002-2004 elsewhere), the
picture is of a late, still-settling addition rather than mature code. `[INFER]`

---

## 283.5 — The configure pipeline `[CERT]`

Past the gate, each configurator runs the same five-step sequence — quoting the out-of-range one:

```java
BAlarmSourceExt alarmExt = updateToAlarmExt(pointExt);      // 1. get or convert the Niagara alarm ext
configureAlarmExt(eventParam, alarmExt);                    // 2. common parameters
configureOutOfRangeOffnormal(eventParam, alarmExt);         // 3. offnormal algorithm
configureOutOfRangeFault(eventParam, alarmExt);             // 4. fault algorithm
addExtIfMissing(alarmExt, target);                          // 5. attach to the point if not already there
updateDescriptor(alarmExt);                                 // 6. point the descriptor at it
```

And the algorithm configuration is itself guarded by an `instanceof` on the **Niagara algorithm object**
`[CERT]`:

```java
BOffnormalAlgorithm offnormalAlgorithm = ext.getOffnormalAlgorithm();
if (offnormalAlgorithm instanceof BOutOfRangeAlgorithm)
  configureOutOfRangeOffnormal(eventParam, (BOutOfRangeAlgorithm) offnormalAlgorithm);
```

— i.e. if the point already carries an alarm ext with a *different* offnormal algorithm, the BACnet
parameters are **silently not applied** (no else branch observed at this call site). `[INFER]`; worth
confirming — logged as **B283-G2**.

**`addExtIfMissing` is the mechanism that makes BACnet Event Enrollment create Niagara alarm extensions on
demand**, the same "the BACnet subsystem mutates the station tree" behaviour B276 §276.6 found in
`BacnetDescriptorUtil`. Two independent places in the driver do this. `[INFER]`

---

## 283.6 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| niagara-help has nothing on EE algorithms | one query, zero matches | `[CERT]` (negative) |
| `configureExt` reject list == `checkEventType`'s | both switches, same eleven types | `[CERT]` |
| Second-gate rejection error is `OPTIONAL_FUNCTIONALITY_NOT_SUPPORTED` | the `throw` in the default arm | `[CERT]` |
| 11 accepted types → 8 configurators | the switch, quoted | `[CERT]` |
| Every failure path calls `resetDescriptor()` | all three catch blocks | `[CERT]` |
| `PermissionException` → `WRITE_ACCESS_DENIED` | catch block, verbatim | `[CERT]` |
| Per-algorithm gate is `instanceof` on the Niagara point | eight guards enumerated with line numbers | `[CERT]` |
| Object-mismatch error is `VALUE_OUT_OF_RANGE` | the out-of-range guard, quoted in full | `[CERT]` |
| ⇒ the two gates are distinguishable by error code | derived | `[INFER]` |
| Command-failure validates a `feedbackPoint`, twice | guards at 3264 (Boolean) and 3417 (Enum) | `[CERT]` |
| Floating-limit validates the setpoint separately | guard at 3530 | `[CERT]` |
| Tridium TODO: variants not matched to datatype | the three comment lines + commented-out `getRemoteObjectType()` | `[CERT]` |
| ⇒ the four out-of-range variants are interchangeable | derived from the single shared configurator + missing check | `[INFER]` |
| Six TODOs in the class | all six quoted with line numbers | `[CERT]` |
| ⇒ late, still-settling code | composed with B278 §278.4's authorship finding | `[INFER]` |
| Six-step configure pipeline | quoted from `configureOutOfRangeExt` | `[CERT]` |
| Algorithm config skipped if a different algorithm is present | `if (… instanceof BOutOfRangeAlgorithm)` with no observed else | `[INFER]` |
| `addExtIfMissing` creates Niagara alarm exts on demand | method call in the pipeline | `[CERT]` / `[INFER]` on the parallel with B276 §276.6 |

Tally: **[CERT] 12 / [INFER] 5.**

---

## 283.x — Connections and gaps

- **B278 §278.5** — the first gate. **G1 closed here**, and §278.5 is *completed*: it could not say which
  BACnet error a rejection produces; §283.2 supplies it.
- **B23 §23.14** — the object model and the polymorphic parameter shapes. No correction needed.
- **B275 §275.5** — `PermissionException → WRITE_ACCESS_DENIED`, the same mapping in the write path.
- **B276 §276.6** — the other place the BACnet subsystem mutates the station's component tree.
- **B34** — the alarm framework whose `BAlarmSourceExt` / `BOffnormalAlgorithm` this configures.

| ID | Gap | Class |
|---|---|---|
| **B283-G1** (new) | Whether a mismatched out-of-range variant (e.g. `SIGNED_` on a REAL point) fails later during parameter decoding, or silently misbehaves. | STATIC-investigable |
| **B283-G2** (new) | Confirm there is no `else` when the existing offnormal algorithm is of another type — i.e. whether BACnet parameters are silently dropped. | STATIC-investigable |
| **B283-G3** (new) | `updateToAlarmExt` / `addExtIfMissing` / `resetDescriptor` — the ext lifecycle; named but not traced. | STATIC-investigable |
| **Next** | **B282-G2** — the trend record ASN encoder. | STATIC-investigable |
