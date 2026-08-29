# Niagara N4 — chihuahua-source (CS4): the ux data/query helpers are injection-safe and N4.14-gotcha-aware — history via the History API (not BBqlGrid), alarm BQL built from `long` epoch millis (not user strings), allowlisted threshold writes, and a complete hand-rolled JSON escaper

**Focus**: chihuahua-source · **Gap**: CS4 (ux data/query helpers) · **Session**: 2026-08-29 · **Block**: B652
**Sources** (`[CERT]` real source): `chihuahua-ux/src/com/angeles/chihuahua/ux/` — `ChiHistoryHelper.java`, `ChiAlarmHelper.java`, `ChiAlarmQueryHelper.java`, `ChiThresholdHelper.java`, `ChiScheduleHelper.java`, `ChiEquipmentReader.java`, `ChiJsonUtil.java`.

**Scope**: the read/query layer of the production dashboard. N4.14 query gotchas (BBqlGrid/BStruct NPE [B359], history sampling [B369], BAbsTime kernel) + injection safety. Servlet auth = [B648] (REMIT).

---

## 652.1 The helpers dodge the known N4.14 query traps

`[CERT]` — the corpus documented traps, and chihuahua avoids each:
- **History via the History API, not BBqlGrid** — `ChiHistoryHelper` queries `BHistoryDatabase` (`BOrd.make("history:").get()` → `HistorySpaceConnection.timeQuery()` → `BITable`/`TableCursor`, `:75,:155`). It never touches `BBqlGrid`/`BHistoryRecord` as `BStruct`, so the [B359] `ordInSession`→NPE wall is sidestepped entirely.
- **Stride downsampling by ACTUAL buffered count** (`ChiHistoryHelper.java:228-270`) — a prior density-assumption discarded ~98% of hourly cárcamo samples (comment `:216`); the fix strides by measured record count. `MAX_POINTS_HARD_CEILING=5000` (`:139`), `MAX_BUFFERED_RECORDS=100_000` OOM guard (`:147`). This is the [B369] sampling lesson applied.
- **Alarm queries via BQL cursor walk, not a grid** — `ChiAlarmHelper` (~2100 L) builds `buildAlarmBql(startMs, stopMs, ackState)` and walks a `BITable` cursor. N4.14 gotcha handled: `BAlarmRecord` is `final` (`:31`) so it can't be subclassed for tests → pure-Java helpers (`buildAlarmBql`/`bucketPriority`) are the WSL-testable seam. `ackAllUnacked` is collect-only (`:1104`) because `BAlarmService.ackAlarm` on a cursor record is a silent no-op — the ack is done client-side via BajaScript (`svc.ackAlarms({ids})`).
- **Component-tree walk with `final`-method awareness** — `ChiEquipmentReader` walks service→Planta→Monitor→equipment via `getPropertiesArray()`/`get(prop)`; because those are `final` in N4.14 (no mock), the testable surface is the DTO→JSON layer. Fault discrimination (`readNumericNullable`/`readBoolNullable`, `:578-630`) maps `isFault/isDown/isStale` → sparse `pointStatus` (the [B651] P1 fix).

---

## 652.2 Injection safety — clean

`[CERT]` — no query/JSON injection surface:
- **BQL uses `long` epoch millis, not user strings** — `buildAlarmBql` appends `timestamp.millis >= <long>` (`ChiAlarmHelper.java:1035-1037`); `ackState` is a fixed token (`'acked'`/`'ackPending'`, `:1066,:1075`), not raw user text; the per-source query escapes the ORD (`source='<escaped-ord>'`, `:1919`). `ChiScheduleHelper`/`ChiAlarmQueryHelper` likewise use fixed BQL + long millis.
- **Threshold writes are allowlisted + value-guarded** — `ChiThresholdHelper.writeThreshold(..., allowlist, ...)` (`:208`) rejects any slot not in the compile-time `UP/CARCAMO/DATALOGGER_THRESHOLD_KEYS` (`isInArray`, `:57-75`) and rejects NaN/Inf/negative (`isValidThresholdValue`, `:72`). A client cannot write an arbitrary slot.
- **Complete JSON escaping** — `ChiJsonUtil.escapeJson` (`:31`) covers `\ " \n \r \t \b \f`, all control chars U+0000–U+001F, and U+2028/U+2029 (the JS-breaking line separators). Every string value is escaped before emission; `appendKeyValueRaw` (`:132`) emits only pre-validated fragments (numeric literals / already-escaped content). Hand-rolled by design (NFR-004: no external lib in a BMS module — the shop pattern, cf. sdash/mcpbridge shading, [B644]/[B643]).

---

## 652.3 Grade

The query layer is well-engineered for N4.14: it avoids the BBqlGrid/BStruct NPE, applies the history-sampling fix, handles the `final`-class test constraints with a DTO seam, and is injection-safe (long-millis BQL, allowlisted writes, complete JSON escaping). No `System.out`, no TODO/FIXME across the 7 files. `ChiAlarmHelper` at ~2100 L is large but sectioned. This is production-quality read code.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | history via History API (timeQuery/cursor), not BBqlGrid → avoids B359 NPE | [CERT] | ChiHistoryHelper.java:75,155 | ✅ read |
| 2 | stride downsampling by actual buffered count (B369 fix); 5000/100k guards | [CERT] | ChiHistoryHelper.java:228-270,139,147 | ✅ read |
| 3 | alarm BQL cursor walk; BAlarmRecord final; ackAllUnacked collect-only (cursor ack no-op) | [CERT] | ChiAlarmHelper.java:31,1104 | ✅ read |
| 4 | BQL built from long epoch millis + fixed ackState + escaped ORD (no user-string interpolation) | [CERT] | ChiAlarmHelper.java:1035-1037,1066,1919 | ✅ read verbatim |
| 5 | threshold writes allowlisted (isInArray) + NaN/Inf/negative rejected | [CERT] | ChiThresholdHelper.java:57-75,208 | ✅ read verbatim |
| 6 | ChiJsonUtil.escapeJson complete (control chars + U+2028/2029); hand-rolled NFR-004; no injection | [CERT] | ChiJsonUtil.java:31,132 | ✅ read |
| 7 | ChiEquipmentReader DTO→JSON seam + fault discrimination (B651 P1) | [CERT] | ChiEquipmentReader.java:578-630 | ✅ read |

**Tally**: [CERT] ×7 · [INFER] ×0 · real-source block. Injection-safety (BQL long-millis, threshold allowlist, escapeJson) token-checked verbatim.

## Connections

- **[B359]** BBqlGrid/BStruct NPE — avoided. **[B369]** history sampling — applied. **[B651]** P1 fault discrimination (ChiEquipmentReader). **[B648]** servlet auth (the layer above). **[B644]/[B643]** hand-rolled-JSON-vs-shaded-lib shop pattern.
- Forward: CS5 (frontend consuming these helpers), CS8 (verdict: read layer is production-quality + injection-safe).

## Gaps uncovered

- None. The read layer is clean; no new gaps.
