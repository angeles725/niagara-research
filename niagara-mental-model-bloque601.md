# Block 601 — The oBIX `rollup` op does server-side downsampling: a `HistoryRollupIn` (start/end + `reltime` interval) returns one dense bucket per interval carrying `count/min/max/avg/sum`, wrapped in `HistoryQueryOut` — validated live

**Session**: 2026-08-29
**Focus**: `api-access` (gap B458-G1 — the `rollup` op contract). §12 DYNAMIC phase, live validation.
**Distribution / live target**: OptimizerSupervisor-N4.14.0.162 station, `127.0.0.1` (serverName `DESKTOP-4AAQ77H`),
`live-install` → SECRETS DISCIPLINE.
**Method**: READ-ONLY (§12 rung-1) authenticated POST with `API2` over SCRAM, `no·inline`. Ground truth as
[Block 600] (same session, cert `C1:01:41:B2:…:E5:D2`).
**Primary source**: `[CERT-live]` `sources/probes/B601-obix-rollup/rollup-Ramp-PT1H.xml`.
**Scope**: the exact input/output contract of the oBIX History `rollup` op, executed live. Deepens [Block 458]
(which documented the raw `query` op) and [Block 600] (which cataloged the surface and named `rollup` as the
downsample op). Does NOT re-derive the raw-record `query` contract ([B458] REMITTANCE).

---

## 601.1 Input contract — `HistoryRollupIn` [CERT-live]

The op is `POST /obix/histories/<device>/<point>/~historyRollup/` with input `is="obix:HistoryRollupIn"`.
Validated live with:
```xml
<obj is="obix:HistoryRollupIn">
  <abstime name="start" val="2026-08-28T00:00:00.000-06:00"/>
  <abstime name="end"   val="2026-08-29T23:59:59.000-06:00"/>
  <reltime name="interval" val="PT1H"/>
</obj>
```
→ HTTP 200. The `interval` is an ISO-8601 `reltime` (`PT1H` = 1-hour buckets); `start`/`end` bound the range.
This is the server-side downsample control: the client picks the bucket width, the station does the aggregation
— no client-side resampling of raw records (contrast the raw `query` op, which streams every sample).

## 601.2 Output contract — dense buckets of `count/min/max/avg/sum` in a `HistoryQueryOut` [CERT-live]

The response `is="obix:HistoryQueryOut"` (the SAME wrapper type the raw `query` returns — the op reuses it, it
is NOT a distinct `HistoryRollupOut` element on the wire despite the op's declared `out` type). It carries one
anonymous `<obj>` per interval, each with SEVEN fields `[CERT-live]`:

| field | type | meaning |
|---|---|---|
| `start` | abstime | bucket start |
| `end` | abstime | bucket end |
| `count` | int | raw samples that fell in the bucket |
| `min` | real | minimum sample value |
| `max` | real | maximum sample value |
| `avg` | real | mean |
| `sum` | real | sum (avg = sum/count) |

Real non-empty bucket returned live (Ramp point) `[CERT-live]`:
```
count 176  min 0.20666666  max 99.79333496  avg 50.97056832  sum 8970.82002409
count 324  min 0.10666667  max 99.79333496  avg 51.33179032  sum 16631.50006361
```
`sources/probes/B601-obix-rollup/rollup-Ramp-PT1H.xml`. The stats are internally consistent
(`avg ≈ sum/count`: 8970.82/176 = 50.97 ✓), so the station computes true per-interval aggregates, not a
resampled point.

## 601.3 Output is DENSE, not sparse [CERT-live]

Every interval in `[start,end]` emits a bucket EVEN WHEN EMPTY — the early Aug-28 hours (before the point had
data) came back as `count 0 / min 0 / max 0 / avg 0 / sum 0` rather than being omitted. Consequence for a
client: bucket count = `ceil((end-start)/interval)` is predictable and index-alignable, but `count 0` must be
read as "no data", not "value 0" — the zero-valued stats of an empty bucket are placeholders, not measurements.
This is the practical trap in consuming `rollup` output.

## 601.4 What this does NOT resolve

- The `avg` weighting rule for interpolated/COV histories (whether it is a time-weighted or sample-count mean)
  — the live sample-count consistency (§601.2) shows a simple arithmetic mean over the samples present, but a
  COV-logged point with irregular spacing was not exercised; scoped-out.
- Write path (History `append`) — gap B458-G2, `⚠ CONFIG MUTATION`.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `rollup` input = HistoryRollupIn (start/end abstime + reltime interval) | [CERT-live] | probe (200 on PT1H) | ✓ live |
| 2 | Output wrapper is `obix:HistoryQueryOut` (reused, not RollupOut on wire) | [CERT-live] | `rollup-Ramp-PT1H.xml` | ✓ live |
| 3 | Each bucket carries count/min/max/avg/sum + start/end | [CERT-live] | `rollup-Ramp-PT1H.xml` | ✓ live |
| 4 | Real aggregation: count 176, avg 50.97 = sum 8970.82/176 (consistent) | [CERT-live] | `rollup-Ramp-PT1H.xml` | ✓ live |
| 5 | Output dense — empty intervals emit count-0 zero buckets | [CERT-live] | `rollup-Ramp-PT1H.xml` | ✓ live |

**Marker tally**: [CERT-live] ×5, [INFER] 0. **Block type: EVIDENCE (§12 live).** Ratio 0. CLOSES B458-G1.
**§12 verdict**: **CONFIRMED** — server-side downsample contract validated live end-to-end. Zero secrets. Read-only.

## Connections

- [Block 600] — named `rollup` in the op catalog; this block executes its contract.
- [Block 458] — raw `query` op (per-sample); `rollup` is its aggregating sibling on the same object.
- [Block 369] (webChart) — `samplingType=average` client-side downsample there LOSES peak crossings; here the
  server-side `rollup` PRESERVES both `min` AND `max` per bucket — a client wanting peak fidelity should use
  `rollup` (min/max) rather than webChart's single global `average` sampling. (Decision-relevant cross-link.)
- Points to: B458-G2 (History `append` write — `⚠ CONFIG MUTATION`, authorization-gated).
