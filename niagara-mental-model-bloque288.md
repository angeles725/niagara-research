# Block 288 — Closing the export-family gaps: the three writable descriptors verified parallel (with a per-datatype domain check), and `Elapsed_Active_Time` as a Niagara totalizer

> Closes **B276-G1** and **B276-G2** — the last two second-order gaps of the BACnet sweep that began at B275.
>
> **B276-G2 exists because [B276] asserted something it had not read.** B276 §276.1-276.4 traced
> `BBacnetAnalogWritableDescriptor` line by line and then described the Binary and MultiState writables as
> "structurally parallel" — an assumption, flagged as such at the time. This block verifies it. **The
> assumption holds structurally**, and the interesting part is precisely where it *doesn't*: the
> value-domain check differs per datatype, and each is worth knowing.
>
> **Sources**: original Tridium source, `javax.baja.bacnet.export`. Markers: `[CERT]` verbatim ·
> `[INFER]` derived. **Corpus language: ENGLISH.**

---

## 288.1 — B276-G2: the three writables are parallel, and where they diverge `[CERT]`

Identical across all three:

| Element | Analog / Binary / MultiState |
|---|---|
| `bacnetWritable` slot | `newProperty(Flags.READONLY \| Flags.HIDDEN, lexNotWritable, null)` — same flags, same default |
| Dynamic slot naming | `inSlotName = "bacnetValueIn" + pri` — same line, same position (line 124 in both Binary and MultiState) |
| No-priority default | `if (pri == NOT_USED) pri = 16;` |
| Range guard | `if ((pri < 1) \|\| (pri > 16))` → `PARAMETER_OUT_OF_RANGE` |
| Unlinked level | `if (inSlot == null)` → `WRITE_ACCESS_DENIED` |
| Relinquish | `if (tag == ASN_NULL)` → refused when `Out_Of_Service`, else `setStatusNull(true)` |
| Out-of-service echo | writes `BOutOfServiceExt.presentValue` as well |
| Unexpected tag | `throw new AsnException(E_BACNET_ASN_INVALID_TAG + tag)` |
| Catches | `IllegalArgumentException` → `VALUE_OUT_OF_RANGE`; `PermissionException` → `WRITE_ACCESS_DENIED` |

**So B276's assumption was correct**: the priority-array mechanism of B276 §276.1-276.4 — dynamic
`bacnetValueInN` slots, `BLink` with `FAN_IN`, per-level access control — is the same in all three.

### Where they differ: the value carrier and the domain check

| | Analog | Binary | MultiState |
|---|---|---|---|
| Value type | `BStatusNumeric` | **`BStatusBoolean`** | (enum-ordinal) |
| Accepted tag | `asnType()` (REAL/Double/…) | **`ASN_ENUMERATED`** | **`ASN_UNSIGNED`** |
| Domain check | `real < min \|\| real > max` from `BFacets.MIN`/`MAX` | **`pv != 0 && pv != 1`** | **`writeVal <= 0`**, then `range.isOrdinal(writeVal)` |

Binary `[CERT]`:

```java
else if (tag == ASN_ENUMERATED) {
   int pv = asnIn.readEnumerated();
   if (pv != 0 && pv != 1)
      return new NErrorType(BBacnetErrorClass.PROPERTY, BBacnetErrorCode.VALUE_OUT_OF_RANGE);
   …
   bacval.setValue(pv == 1);
}
```

MultiState `[CERT]`:

```java
else if (tag == ASN_UNSIGNED) {
   int writeVal = asnIn.readUnsignedInt();
   //As per Bacnet Spec Rev 14 : present value should always be greater than 0 for multistate value objects.
   if (writeVal <= 0)
      return new NErrorType(BBacnetErrorClass.PROPERTY, BBacnetErrorCode.VALUE_OUT_OF_RANGE);

   BEnumRange range = (BEnumRange)pt.getFacets().getFacet(BFacets.RANGE);
   if (range != null && !range.isOrdinal(writeVal))
      return new NErrorType(BBacnetErrorClass.PROPERTY, BBacnetErrorCode.VALUE_OUT_OF_RANGE);
   …
```

Three observations:

1. **The accepted tags confirm B271 §271.6 from the write side.** Binary present-value is `ENUMERATED` on
   the wire (which is why B271's read path needed the `isBinaryPv` type-string check to map it to Boolean);
   MultiState is `UNSIGNED`. The server enforces exactly the types the client-side type-decision assumed.
   `[INFER]`
2. **MultiState carries a spec citation in the source**: *"As per Bacnet Spec Rev 14 : present value should
   always be greater than 0 for multistate value objects."* — the 1-based ordinal rule that B271 §271.8 and
   B281 §281.3 both found from the other direction. Three independent confirmations. `[CERT]`
3. **MultiState validates twice**: the `> 0` spec rule *and* membership in the point's `BEnumRange`
   (`range.isOrdinal(writeVal)`), the range being the one built from `stateText` per B271 §271.8. A remote
   client cannot command a state the point does not define. The check is skipped when no range facet exists
   (`range != null &&`). `[CERT]`

**Verdict on B276-G2**: the assumption was sound; the block is corrected only to *narrow* the claim from
"structurally parallel" to "structurally parallel, with a per-datatype value-domain check". §288.3.

---

## 288.2 — B276-G1: `Elapsed_Active_Time` needs a totalizer and a second point `[CERT]`

The guiding question was *why* this BACnet property needs special machinery. The answer is that BACnet's
`Elapsed_Active_Time` is a **derived, accumulating** value — how long a binary point has been active — and
Niagara already has a component for that: `BDiscreteTotalizerExt`.

```java
private static BControlPoint getPointForElapsedActiveTime(BBacnetObjectIdentifier objectId,
                                                          int propertyIndex, BControlPoint point)
{
  BDiscreteTotalizerExt[] extensions = point.getChildren(BDiscreteTotalizerExt.class);
  BDiscreteTotalizerExt extension;
  BControlPoint linkedPoint = null;

  if (extensions.length > 0) {
     //Use the first Discrete Totalizer for algorithmic reporting
     extension = extensions[0];
     extension.setEaTimeUpdateInterval(BRelTime.make(1000));
     linkedPoint = getNumericPointLinkedToDiscreteTotExt(objectId, extension);
  } else {
     extension = addDiscreteTotalizerExtToPoint(point);
  }

  if (linkedPoint == null)
     linkedPoint = addPropertyPoint(null, objectId, BBacnetPropertyIdentifier.ELAPSED_ACTIVE_TIME, propertyIndex);
  …
```

What this establishes:

1. **A `BDiscreteTotalizerExt` is attached to the monitored point** — reused if one already exists, created
   otherwise (`addDiscreteTotalizerExtToPoint`). The comment *"Use the first Discrete Totalizer for
   algorithmic reporting"* means an existing engineering totalizer is co-opted rather than duplicated.
   `[CERT]`
2. **Its update interval is forced to 1000 ms** — `setEaTimeUpdateInterval(BRelTime.make(1000))`. Whatever
   the engineer configured, exposing Elapsed_Active_Time over BACnet **overrides it to one second**. That is
   a side effect on existing station configuration worth knowing. `[CERT]`; the override characterisation is
   `[INFER]`.
3. **A separate numeric point is created** to carry the value (`addPropertyPoint(..., ELAPSED_ACTIVE_TIME, ...)`),
   linked to the totalizer. So one BACnet property becomes: the original point + a totalizer extension + a
   second point. `[CERT]`

This is the **fourth** instance of the pattern this sweep has catalogued — the BACnet subsystem creating
Niagara structure as a side effect: points (B276 §276.6), alarm extensions (B283 §283.5), users and
authentication schemes (B287 §287.4), and now **totalizer extensions plus derived points**. `[INFER]`

`areTrendLogAndPointCompatible` was located but not read — gap **B288-G1**.

---

## 288.3 — Corrections

| Target | Was | Is |
|---|---|---|
| **B276 §276.1-276.4** | Traced `BBacnetAnalogWritableDescriptor` and described Binary/MultiState as "structurally parallel" (flagged as an assumption, gap B276-G2) | **Verified.** The priority-array mechanism is identical in all three. The claim is narrowed: they differ in **value carrier and domain check** — Binary accepts `ASN_ENUMERATED` restricted to `{0,1}`; MultiState accepts `ASN_UNSIGNED` restricted to `> 0` *and* to the point's `BEnumRange`. §288.1 |

No other block needed correction.

---

## 288.4 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| niagara-help: nothing on writable-descriptor internals | query, zero matches (6th for internals) | `[CERT]` (negative) |
| `bacnetWritable` slot identical in all three | same `newProperty(READONLY\|HIDDEN, lexNotWritable, null)` | `[CERT]` |
| `bacnetValueIn` + pri identical (line 124 in both) | grep line numbers | `[CERT]` |
| Same four guards (NOT_USED→16, 1..16, inSlot null, ASN_NULL/OoS) | line-matched in all three | `[CERT]` |
| Binary uses `BStatusBoolean` + `ASN_ENUMERATED` + `{0,1}` | body quoted | `[CERT]` |
| MultiState uses `ASN_UNSIGNED` + `>0` + `range.isOrdinal` | body quoted | `[CERT]` |
| MultiState cites "Bacnet Spec Rev 14" in-source | comment verbatim | `[CERT]` |
| Range check skipped when no range facet | `range != null &&` | `[CERT]` |
| Accepted tags confirm B271 §271.6 from the write side | comparison | `[INFER]` |
| Existing totalizer is reused, not duplicated | `if (extensions.length > 0) … extensions[0]` + comment | `[CERT]` |
| Update interval forced to 1000 ms | `setEaTimeUpdateInterval(BRelTime.make(1000))` | `[CERT]` |
| ⇒ overrides engineer configuration | derived | `[INFER]` |
| A second point is created for the value | `addPropertyPoint(..., ELAPSED_ACTIVE_TIME, ...)` | `[CERT]` |
| Fourth instance of BACnet mutating station structure | composed with B276/B283/B287 | `[INFER]` |

Tally: **[CERT] 11 / [INFER] 3.**

---

## 288.x — Connections and remaining gaps

- **B276** — **G1 and G2 both closed.** §276.1-276.4's claim narrowed in §288.3.
- **B271 §271.6 / §271.8** — the write side confirms the read side's type decisions and the 1-based
  multi-state ordinals.
- **B281 §281.3** — third confirmation of the 1-based rule.
- **B283 §283.5 / B287 §287.4 / B276 §276.6** — the other three places BACnet creates Niagara structure.

### Third-order gaps still open across the sweep

| ID | Gap | Class |
|---|---|---|
| **B288-G1** | `areTrendLogAndPointCompatible` — located, not read. | STATIC |
| B281-G1/G2/G3 | `ScheduleSupport4` comparators; `makeDay()`; the schedule decode direction. | STATIC |
| B282-G1/G3 | `BTrendFlags` bit 4; `readLogResult()` (the import direction). | STATIC |
| B283-G1/G2/G3 | Mismatched out-of-range variant behaviour; the silent-drop `else`; the ext lifecycle. | STATIC |
| B284-G1/G2/G3 | `getLogDatumChoice` mapping; NiagaraHistory's private reader; `trendEvent` encoding. | STATIC |
| B285-G1..G4 | EMSTP opcodes 49/51; `SET_TX_THROTTLE` placement; the 777-line state machine; `EmstpStats`. | STATIC |
| B286-G1/G2 | Per-subclass option checks; whether Niagara ever *emits* header options. | STATIC |
| B287-G1/G2/G3 | `BHubConnectorHealth`; `BBacnetScAuthenticator` matching; the connection lifecycle. | STATIC |
| **P3-mstp** | MS/TP framing — **requires-native-RE** (SAM4S firmware) or live RS-485 capture. | blocked |
| B271-G4, B272-G4, B273-G4, B274-G1, B280-G4, P3-dyn | live-device confirmations. | requires-execution |
