# B670 — `BAlarmRecord` field getters for the webhook `toJson`: exact signatures for uuid/timestamp/alarmClass/priority/sourceState/ackState/source — and the ONE correction, `getAckRequired()` NOT `isAckRequired()`; `getAlarmFacet(String)` returns `BObject` (null if absent), while `getFormattedAlarmDataValue(key,cx)` returns a String ("" if absent); keys enumerated via `getAlarmData().list()` (focus alarm-webhook, AW5; confirms/refines B34 §34.7)

> **Focus:** `alarm-webhook` (§16, re-opened). **Gap closed:** AW5 (exact getter signatures on `BAlarmRecord`
> for the recipient's JSON serialization). **Phase:** static, READ-ONLY.
> **Sources** (all `[CERT]`):
> - `organized/docSource/docSource-doc/extracted/alarm-rt/javax/baja/alarm/BAlarmRecord.java` (Tridium
>   doc-source, full javadoc; getters + alarmData contract + the well-known key set).
> - `organized/docSource/docSource-doc/extracted/baja/javax/baja/sys/BFacets.java` (`list()` / `getFacet()`).
> - `[CERT]` corpus [Block 34] §34.7 (BAlarmRecord fields/serialization) — this block gives the exact method
>   signatures; [Block 345] §345.4 (alarmData in jsonToolkit, REMITTANCE).
>
> **Bottom line for the PoC:** of the eight getters the skeleton assumed, **seven are correct**; the one wrong
> is `isAckRequired()` — the real method is **`getAckRequired()`** (a `boolean` slot whose generated accessor
> uses the `get` prefix, not `is`). For dynamic alarm data, `getAlarmFacet(key)` returns a **`BObject`** (or
> `null` if the key is absent) — for a JSON string field prefer `getFormattedAlarmDataValue(key, cx)`, which
> returns a `String` ("" when absent). Enumerate the keys actually present with `getAlarmData().list()`.

---

## §670.1 — The eight core getters (verify against the skeleton) `[CERT]`

`BAlarmRecord extends BStruct` `[CERT BAlarmRecord.java:181-183]` — a value type, not a live component. All
accessors are Slot-o-Matic generated from `@NiagaraProperty`. Exact signatures:

| Field (peer's ask) | Type | EXACT getter | Return | Cite | Skeleton assumed | Verdict |
|---|---|---|---|---|---|---|
| uuid | `BUuid` | `getUuid()` | `BUuid` | :230 | `getUuid()` | ✅ correct |
| timestamp | `BAbsTime` | `getTimestamp()` | `BAbsTime` | :204 | `getTimestamp()` | ✅ correct |
| alarmClass | String | `getAlarmClass()` | `String` | :362 | `getAlarmClass()` | ✅ correct |
| priority | int | `getPriority()` | `int` | :389 | `getPriority()` | ✅ correct |
| sourceState | `BSourceState` | `getSourceState()` | `BSourceState` | :256 | `getSourceState()` | ✅ correct |
| ackState | `BAckState` | `getAckState()` | `BAckState` | :282 | `getAckState()` | ✅ correct |
| **ackRequired** | boolean | **`getAckRequired()`** | `boolean` | :308 | `isAckRequired()` | ❌ **CORRECT to `getAckRequired()`** |
| source | `BOrdList` | `getSource()` | `BOrdList` | :334 | `getSource()` | ✅ correct |

**The one correction:** `ackRequired` is a `boolean` property, but its generated getter is `getAckRequired()`
`[CERT:308]`, **not** `isAckRequired()`. Baja's Slot-o-Matic emits `get<Name>()` for boolean slots (see also
`getAckRequired`/`getRouteAcks` in `BAlarmRecipient`), so `is…` accessors do not exist here. Using
`isAckRequired()` would not compile.

### Serializing the non-String types
- `getUuid()` → `BUuid`; use `.toString()` (the same UUID string the recipient's disk queue uses as the
  filename, [Block 666] §666.2, and that `toSummaryString()` prints, `[CERT:1044]`).
- `getTimestamp()`/`getNormalTime()`/`getAckTime()`/`getLastUpdate()` → `BAbsTime`; `.getMillis()` for epoch
  ms or `.toString()` for ISO-ish text.
- `getSourceState()`/`getAlarmTransition()` → `BSourceState`; `getAckState()` → `BAckState` — both frozen
  enums, `.toString()` gives the tag (`offnormal`/`fault`/`normal`/`alert`; `unacked`/`ackPending`/`acked`).
- `getSource()` → `BOrdList`; `.toString()` or iterate; the javadoc notes *"Should use getNavOrd()"* for a
  display path `[CERT:323]`.
- `getPriority()` → `int` (0 = high … 255 = low, `[CERT:119-120]`).

## §670.2 — Bonus getters the `toJson` will want `[CERT]`

Beyond the eight, these are populated and useful for a notification payload:
- `getUser()` → `String` (who acked; default `"Unknown User"`) `[:470]`.
- `getAlarmData()` → `BFacets` — the dynamic key/value bag (§670.3) `[:496]`.
- Convenience state predicates: `isAlarm()` `[:706]`, `isAcknowledged()` `[:714]`, `isAckPending()` `[:722]`,
  `isNormal()` `[:730]`, `isOpen()` `[:738]` — note these ARE `is…` methods (hand-written, not slot
  accessors), unlike `getAckRequired()`.
- `getAlarmValue()` → `BObject` — tries `alarmValue`→`offnormalValue`→`faultValue`, null if none `[:775]`.

## §670.3 — `alarmData`: `getAlarmFacet(String)` returns `BObject` (null if absent); the String-safe variant; how to list keys `[CERT]`

The intake asked for the exact `getAlarmFacet` signature, the absent-key behavior, and how to list keys.

```java
public BObject getAlarmFacet(String key) { return getAlarmData().get(key); }   // BAlarmRecord.java:764
```
- **Returns `BObject`**, NOT `String`. For a missing key it returns **`null`** (the value of
  `BFacets.get(absentKey)`; corroborated by `getAlarmValue()`'s `if (ret != null)` null-checks `[:775-788]`).
  `[CERT signature; INFER on null — from the null-guard usage]`
- **String-safe alternative (prefer this in `toJson`):**
  ```java
  public final String getFormattedAlarmDataValue(String field, Context cx)   // :1123
  ```
  Returns a `String` and — javadoc verbatim `[CERT:1109-1122]` — *"An empty string is returned if the key
  can't be found in the alarm data."* It also applies `BFormat` for `msgText`/`instructions`
  (`isAlarmDataFieldFormat`, `[:1104]`), so `%…%` substitutions resolve. Pass `cx = null` if you have no
  context. This is the method that gives you `""` (not null) for an absent key.

**Enumerating the keys actually present** in a record's `alarmData`:
```java
String[] keys = alarmRecord.getAlarmData().list();     // BFacets.list() — BFacets.java:835
BObject v    = alarmRecord.getAlarmData().getFacet(k); // per-key value — BFacets.java:972
```
`getAlarmData().list()` `[CERT BFacets.java:835]` returns the keys present in **this** record (the record only
carries the facets its algorithm set — not all possible ones).

**The well-known key catalog** (the "~32" the intake mentioned): the static
`BAlarmRecord.getAlarmDataFields()` `[CERT:1057-1092]` returns a **30-entry** array of the common
user-defined keys — `alarmValue, controlledValue, Count, deadband, errorLimit, faultValue, feedbackNumeric,
feedbackValue, fromState, highLimit, hyperlinkOrd, icon, instructions, lowLimit, msgText, newValue,
notifyType, numericValue, offnormalValue, presentValue, setptValue, soundFile, sourceName, status, TimeZone,
toState, setpointNumeric, highDiffLimit, lowDiffLimit`. Note the **literal key strings differ from the
constant names**: `Count` (capital C), `TimeZone` (not `timeZone`), `setpointNumeric` (for `SETPT_NUMERIC`),
`hyperlinkOrd`, etc. `[CERT:1176-1207]`. There are **two more** public constants NOT in that array —
`TIME_DELAY = "timeDelay"` and `TIME_DELAY_TO_NORMAL = "timeDelayToNormal"` `[:1206-1207]` — which is why the
count reads as "~32" (30 well-known + 2 extra constants). For `toJson`, iterate `getAlarmData().list()` to
capture whatever is actually present rather than probing all 30/32.

Common ones a Telegram message will want: `SOURCE_NAME = "sourceName"`, `MSG_TEXT = "msgText"`,
`PRESENT_VALUE = "presentValue"`, `HIGH_LIMIT`/`LOW_LIMIT`, `STATUS = "status"`, `NOTES = "notes"`
`[CERT:1176-1204]`.

## §670.4 — Note on ack semantics (feeds AW6) `[CERT]`

`BAlarmRecord.ackAlarm(String user)` `[:685-699]` sets `ackState` to `ackPending` (if `ackRequired`) or
`acked`, stamps `user`/`ackTime`/`lastUpdate`. This is the *in-record* mutation; routing the ack back to the
source/service is a separate path (the oBIX external-ack route is [Block 671]). The `uuid` is the stable
identity across the webhook payload, the disk-queue filename, and `toSummaryString()`.

## §670.5 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | getUuid/getTimestamp/getAlarmClass/getPriority/getSourceState/getAckState/getSource — all correct as assumed | [CERT] | BAlarmRecord.java:230,204,362,389,256,282,334 |
| 2 | `ackRequired` getter is `getAckRequired()` (boolean), NOT `isAckRequired()` | [CERT] | :308 |
| 3 | `getAlarmFacet(String)` returns `BObject` (null if absent) | [CERT sig] + [INFER null] | :764, :775-788 |
| 4 | `getFormattedAlarmDataValue(String,Context)` returns String, "" if absent | [CERT] | :1109-1123 |
| 5 | Keys present enumerated via `getAlarmData().list()`; per-key via `getFacet()` | [CERT] | BFacets.java:835,972 |
| 6 | `getAlarmDataFields()` = 30 well-known keys; literal strings differ from constant names; +2 extra constants | [CERT] | :1057-1092,1176-1207 |
| 7 | `getUser()`, state predicates `isAlarm/isAcknowledged/isOpen`, `getAlarmValue()` available | [CERT] | :470,706-751,775 |

**Tally:** 7 claims — 6 [CERT], 1 [CERT sig + INFER null] (#3). 0 unmarked.

## §670.6 — Connections

- **[Block 34] §34.7** — BAlarmRecord fields/serialization overview; this block gives exact getter signatures.
- **[Block 666]** — where `sendAlarm(BAlarmRecord)` calls these getters to build the JSON; uuid = queue
  filename.
- **[Block 671]** — the oBIX external-ack route (uses `uuid`/source to locate the alarm).
- **[Block 345] §345.4** — alarmData consumed by jsonToolkit (REMITTANCE; a sibling JSON marshaller).
