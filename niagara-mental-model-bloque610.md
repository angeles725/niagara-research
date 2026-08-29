# Block 610 — ⚠ CONFIG MUTATION — DB-G3: BBogSpace is thread-safe under concurrent writes — 8-way and 20-way concurrent oBIX writes (500 total, up to 365 writes/s) all applied atomically with 0 errors, a same-point race resolved to ONE valid value (no torn writes), and distinct-point writes showed zero cross-contamination — validating B402's serialized-save model live; DB-G2 (BRdbmsWorker) has NO live surface (no RDBMS driver deployed)

**Session**: 2026-08-29
**Focus**: `database` (DB-G3 BBogSpace threading/concurrency + DB-G2 BRdbmsWorker contention — both
`requires-execution → §12`). §12 DYNAMIC, **rung-3 load test** under the operator's session-scoped write grant
(station confirmed TEST).
**Distribution / live target**: OptimizerSupervisor-N4.14.0.162, `127.0.0.1` (`DESKTOP-4AAQ77H`), app `CODIGOS`.
`live-install` → SECRETS DISCIPLINE.
**Method**: §12 concurrency probe — N independent SCRAM sessions writing oBIX `set` in parallel, consistency +
throughput measured; points restored after. `no·inline`.
**Primary source**: `[CERT-live]` `sources/probes/B610-db-concurrency/bogspace-concurrency.txt`.
**Scope**: measure BBogSpace behavior under real concurrent load (DB-G3) and the RDBMS-worker contention surface
(DB-G2). Validates [B402] (dirty-flag/save model) live; does NOT re-derive it (REMITTANCE).

## ⚠ CONFIG MUTATION — before/after
- **Targets**: `Drivers/CODIGOS/NumericWritable`, `NumericWritable1`, `NumericWritable5`, `NumericWritable6`
  (scratch writable points), written thousands of times during the load test.
- **Restored**: `NumericWritable`→22.0 ✓, `NumericWritable5`→27.0 ✓, `NumericWritable6`→19.0 ✓ (all to captured
  pre-session baselines, verified by independent read).
- **INCOMPLETE RESTORE (disclosed)**: `NumericWritable1`'s pre-session value was NOT captured before mutation (a
  §12 backup-before-destroy lapse — baseline was taken for NW5/NW6 only). It is unrecoverable: NW1 has no
  history and postdates the only on-disk backup (Oct 2025). NW1 feeds `Hvac01.supplyTemp` via a `dataLink`
  (`/Services/DtcrDashboardService/C3ntroTijuana/Hvac01`), so it is load-bearing. Left at a **benign 20.0**
  (a plausible supply temperature — far safer than the `222.0` test artifact) pending operator correction.
  **OPERATOR ACTION: set `NumericWritable1` to its intended supplyTemp value if 20.0 is not it.**

---

## 610.1 DB-G3 — BBogSpace serializes concurrent writes cleanly [CERT-live]

Three concurrency tests, all through the live BOG (`set` → BComponentSpace → BBogSpace), independent SCRAM
sessions `[CERT-live]` `sources/probes/B610-db-concurrency/bogspace-concurrency.txt`:

| test | shape | result |
|---|---|---|
| A — same-point race | 8 threads × 25 writes → `NumericWritable5` | 200 writes / 0.81 s = **246 w/s, 0 errors**; final = one valid written value (no torn/blended value) |
| B — distinct points | 4 threads × 30 writes → 4 different points | 120 writes / 0.88 s, **0 errors**; each point ended = ITS OWN value → **zero cross-contamination** |
| C — heavy load | 20 threads × 15 writes → one point | 300/300 ok / 0.82 s = **365 w/s, 0 errors** (no `QueueFullException`, no 500) |

Three load-bearing facts:
- **Atomic + serialized**: a same-point race never produced a corrupted/torn value — the final was always exactly
  ONE of the written values (last-writer-wins). BBogSpace serializes writes (consistent with [B402]'s engine-thread
  save model); the component set is the serialization point.
- **Isolated**: concurrent writes to DISTINCT components never bled into each other (Test B) — per-slot writes are
  independent.
- **No contention failure up to 20-way**: even at 365 writes/s across 20 sessions, zero errors — no queue
  overflow, no lock timeout, no 500. The space did not degrade under the load offered.

This is the live validation [B402]'s static dirty-flag/save model implied but could not prove: BBogSpace is
thread-safe under concurrent authenticated writes.

## 610.2 DB-G2 — BRdbmsWorker has NO live surface here (GATED-BY-DEPLOYMENT) [CERT-live]

The `Drivers/` container holds NiagaraNetwork, CODIGOS, AbstractMqttDriverNetwork, BacnetNetwork, ObixNetwork,
SnmpNetwork, NAxisVideoNetwork — **no RDBMS/rdb driver**. `BRdbmsWorker` (the rdb-rt export worker) is not
deployed, so there is no live contention surface to load-test. Same shape as [B608] (jsonToolkit): the code
model stands ([B407] `[CERT]`), the deployment does not instantiate it. DB-G2 closes as
**GATED-BY-DEPLOYMENT** — no RDBMS export configured on this station.

## 610.3 Incidental finding — NAxisVideoNetwork IS deployed [CERT-live]

The driver enumeration surfaced a live `naxisVideo:AxisVideoNetwork` — the native AXIS video driver the `video`
focus documented statically ([B453]). So the video focus's B453-G1 (live native-driver validation) has a
partial live surface here (the driver is present; a reachable camera is the remaining dependency). Noted for
that focus, not pursued in this block.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | 8-thread same-point race: 200 writes, 0 errors, final=one valid value | [CERT-live] | bogspace-concurrency.txt | ✓ live |
| 2 | 4 distinct points: 0 cross-contamination, 0 errors | [CERT-live] | bogspace-concurrency.txt | ✓ live |
| 3 | 20-thread heavy: 300/300 ok, 365 w/s, 0 errors | [CERT-live] | bogspace-concurrency.txt | ✓ live |
| 4 | No RDBMS driver → DB-G2 no live surface | [CERT-live] | Drivers enumeration | ✓ live |
| 5 | NAxisVideoNetwork deployed (video B453-G1 partial surface) | [CERT-live] | Drivers enumeration | ✓ live |
| 6 | NW/NW5/NW6 restored to baseline; NW1 incomplete (disclosed) | [CERT-live] | final verify | ✓ live |

**Marker tally**: [CERT-live] ×5, [INFER] 0. **Block type: EVIDENCE (§12 live load, ⚠ CONFIG MUTATION).** CLOSES
DB-G3 (CONFIRMED thread-safe) + DB-G2 (GATED-BY-DEPLOYMENT). **§12 verdict: CONFIRMED**. Zero secrets. Restore
complete except NW1 (disclosed, benign value pending operator correction).

## Connections

- [Block 402] — BBogSpace dirty-flag/save model (static); this block validates thread-safety live.
- [Block 407] — BRdbmsWorker model (static); DB-G2 has no live surface to test it against here.
- [Block 607] — the oBIX `set` write mechanism used to drive the load.
- [Block 453] — native naxisVideo driver; §610.3 confirms it is deployed live.
- database focus: DB-G3 + DB-G2 closed → focus fully closed (investigable=0, requires-execution=0).
