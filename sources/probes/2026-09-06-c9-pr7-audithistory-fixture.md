# C9 PR7 — recorded `/PANCCADIA/AuditHistory` fixture for `runMirror`'s real `readAuditHistory`

Author: companero (Fable), 2026-09-06. The RED `qa/c9-s12-audit-mirror` **`0a14df8`** injects `deps.readAuditHistory()`
returning the ALREADY-DECODED shape `Array<{ts, user, target, old, new}>` (its inline FIXTURE, §1). The DEFAULT dep in
`audit-mirror.mjs` must produce that array from the station's AuditHistory over oBIX. This file is that seam's fixture +
the decode mapping. **No live export exists on this machine** (the Workbench station copy under
`/mnt/c/Users/equipo/Niagara4.14/OptimizerSupervisor/stations/PANCCADIA/` has no `*audit*` export, no `history/` audit
file), and the station is not reachable from WSL — so the oBIX XML below is derived from B829 §829.4 and oBIX history
conventions and marked **[INFER]** until one live `~historyQuery` is captured (the B829-live / B830-G1 gate). The DECODED
array reuses the RED's own sample values so `mapRecord`'s fixture and the RED FIXTURE align 1:1.
`[ev: RED 0a14df8 audit-mirror.test.mjs:29-34]` `[ev: corpus B829 §829.4 (AuditRecord fields) — CERT]` `[ev: oBIX 1.1 §history — CERT-doc]`

## 1. The RED FIXTURE (the target decoded shape — VERBATIM, 0a14df8:29-34)
```js
[ { ts: 1725580800000, user: 'operador@panccadia.mx',   target: 'Programacion/ColdRoom_1/setpoint',        old: '4.0', new: '3.5' },
  { ts: 1725580800000, user: 'operador@panccadia.mx',   target: 'Programacion/ColdRoom_2/setpoint',        old: '5.0', new: '4.5' },   // same ts as row 1 → must stay distinct under the 5-tuple key
  { ts: 1725580860000, user: 'supervisor@panccadia.mx', target: 'Programacion/ColdRoom_1/differentialUp',  old: '1.5', new: '2.0' } ]
```
`key = [ts, user, target, old, new].join('|')` (0a14df8:34). Note `user` here is an EMAIL — but the real `AuditRecord.userName`
is a STATION username, not an email (B829 §829.4). The fixture uses emails only because the test never asserts the identity's
FORM; the real decode carries `userName` verbatim, and the sink maps it into `user_email` as the identity column (PR7 §4,
settled by the lead — `change_log.user_email` holds an email for surface A rows, a station username for surface B rows).

## 2. The RAW oBIX the default `readAuditHistory` decodes ([INFER] shape — confirm on one live query)
Request (`poller.mjs`'s `obixRequest(cfg, 'POST', path, body)` transport, `:56`):
```
POST /obix/histories/PANCCADIA/AuditHistory/~historyQuery
Content-Type: text/xml
<obj is="obix:HistoryFilter">
  <abstime name="start" val="<high-water ts, ISO-8601>"/>   <!-- MIRROR_STATE file; omit on first run to read the tail -->
  <int     name="limit" val="500"/>
</obj>
```
Response `obix:HistoryQueryOut` — one `<obj>` per record; child names are the `BAuditRecord` slot names (B829 §829.4):
```xml
<obj is="obix:HistoryQueryOut">
  <int  name="count" val="3"/>
  <list name="data" of="history:AuditRecord">
    <obj>
      <abstime name="timestamp" val="2024-09-05T20:00:00.000Z"/>   <!-- Clock.time() → epoch ms 1725580800000 -->
      <str  name="userName"  val="operador"/>                       <!-- station user, NOT an email -->
      <str  name="operation" val="Changed"/>                        <!-- mirror ONLY 'Changed'; skip Added/Removed/Invoked -->
      <str  name="target"    val="Programacion/ColdRoom_1"/>        <!-- component slot-path BODY -->
      <str  name="slotName"  val="setpoint"/>
      <str  name="oldValue"  val="4.0"/>
      <str  name="value"     val="3.5"/>
    </obj>
    <!-- … two more … -->
  </list>
</obj>
```
The child element NAMES (`timestamp/userName/operation/target/slotName/oldValue/value`) are [CERT] from B829 §829.4
(`BAuditRecord.fromEvent` copies these fields). Their oBIX ELEMENT TYPES (`abstime`/`str`) and the `~historyQuery` envelope
are [INFER] from oBIX 1.1 §history — the one thing to confirm live is whether the station serves AuditHistory as an
`obix:History` at all and the exact `val=` timestamp format (Niagara oBIX usually emits `abstime` ISO-8601 with millis).

## 3. `mapRecord` — the pure decode (fixture for its own unit test)
```js
// audit-mirror.mjs — pure, testable without the station
export function mapRecord(rec) {                       // rec = one decoded oBIX record object (string fields)
  return {
    ts:     Date.parse(rec.timestamp),                 // ISO-8601 → epoch ms (matches the RED's integer ms)
    user:   rec.userName,                              // station username, verbatim (null → '')
    target: rec.target + '/' + rec.slotName,           // 'Programacion/ColdRoom_1' + '/' + 'setpoint'
    old:    rec.oldValue,
    new:    rec.value,
  };
}
export function isMirrorable(rec) { return rec.operation === 'Changed'; }   // slot writes only
```
| oBIX child | → `{}` field | note |
|---|---|---|
| `timestamp` (abstime) | `ts` (ms int) | `Date.parse`; the RED FIXTURE uses ms integers |
| `userName` (str) | `user` | station username; empty for an anonymous/system change |
| `target` + `/` + `slotName` | `target` | composite, matches the RED's `Programacion/ColdRoom_1/setpoint` |
| `oldValue` (str) | `old` | |
| `value` (str) | `new` | |
| `operation` (str) | (filter) | keep `Changed`; drop the rest |
Unit fixture for `mapRecord`: the three `<obj>` records above → the §1 array (with `user` = `operador`/`supervisor`, not the
email — that is the ONLY value that differs from the RED FIXTURE, and it is the correct real shape). Assert
`map(records).map(r => [r.target, r.old, r.new])` equals the three expected triples and `ts` = `1725580800000 / …860000`.

## 3b. The durable dedupe half (cross-ref, not restated here)
`runMirror`'s in-memory `changeLog.has(key)` (the 5-tuple `(ts,user,target,old,new)`, MIR2/MIR3) is backed by a DB unique
index so a concurrent or restarted mirror cannot double-insert: `change_log_dedupe_idx` on `(ts, user_email, target,
old_value, new_value) where surface = 'servlet'` — defined in the PR7 mirror package F4 (`2026-09-06-c9-pr7-audit-mirror-apply-package.md`)
and shipped by R5's additive migration. An insert that collides on it is a `skipped`, not an error. `[ev: investigador1 — R5 additive migration]`

## 4. Recording a REAL fixture (when the station is reachable — B829-live gate, not a PR gate)
```bash
# read-only; needs the oBIX user creds from config.env and network to the JACE
curl -s -u "$OBIX_USER:$OBIX_PASS" -H 'Content-Type: text/xml' \
  --data '<obj is="obix:HistoryFilter"><int name="limit" val="20"/></obj>' \
  "$OBIX_BASE/histories/PANCCADIA/AuditHistory/~historyQuery" > sources/probes/fixtures/panccadia-audithistory-sample.xml
```
Then pin `mapRecord` against the captured XML and upgrade §2 from [INFER] to [CERT]. Until then, PR7's default
`readAuditHistory` stays behind `MIRROR_ENABLED` (OFF), so the unverified decode never runs in production.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | RED FIXTURE values + 5-tuple key + two same-ts rows | [CERT] | 0a14df8:29-34 |
| 2 | AuditRecord field set timestamp/userName/operation/target/slotName/oldValue/value | [CERT] | B829 §829.4 |
| 3 | oBIX `~historyQuery` envelope + element types | [INFER] | oBIX 1.1 §history; confirm on one live query |
| 4 | no audit export / no station reach on this machine | [CERT] | `find` on the station copy (0 hits); WSL has no route to the JACE |
| 5 | `user` is a station username, not an email | [CERT] | B829 §829.4 (`user.getUsername()`) |
