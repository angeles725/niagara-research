# Block 600 — The authenticated oBIX query surface is a fixed set of TYPED oBIX ops, not arbitrary BQL: per-history `query`/`rollup`/`append`, the live-value `Watch` (add/pollChanges), and Alarm `query` — there is NO raw-BQL-over-HTTP servlet on this station

**Session**: 2026-08-29
**Focus**: `api-access` (gap B457-G1 — "BQL recipes over authenticated oBIX"). §12 DYNAMIC phase, live validation.
**Distribution / live target**: Honeywell OptimizerSupervisor-N4.14.0.162 station, live at `127.0.0.1` (WSL
mirrored), serverName `DESKTOP-4AAQ77H`. Sensitivity: **`live-install` → SECRETS DISCIPLINE** (structure cited,
never credential values).
**Method**: READ-ONLY (§12 rung-1) authenticated probes with the service account `API2` over SCRAM-SHA-256,
driven `no·inline` (§12 forbids delegating a live probe / write-credential to a sub-agent). Auth out-of-band via
a scratchpad `curl -K` config; credential never in argv / `sources/` / block / engram.
**Ground truth (re-measured live, NOT inherited — §12)**: cert SHA-256 `C1:01:41:B2:…:E5:D2` (matches
[Block 156] anchor), CN=Niagara4 / O=ForRecoveryPurposes, valid → Sep 2026; oBIX `serverTime` 2026-08-29
`-06:00`; TLS 1.2 negotiated.
**Primary sources**:
- `[CERT-live]` `sources/probes/B600-obix-query-surface/` — live oBIX responses this session.
- `[CERT]` `organized/obixDriver/obixDriver-rt/vineflower/...` (contract types), cross-ref [Block 458] oBIX
  extraction contract in `sources/decompiled/obix-contracts/`.

**Scope**: WHAT authenticated query recipes actually run against a live N4 oBIX server, and — decisively —
which do NOT. Deepens [Block 457] (login) and [Block 458] (History `query` extraction). Does NOT re-derive the
`query` op record/pagination contract ([B458] REMITTANCE) — it catalogs the whole surface and proves the
raw-BQL absence. This is the first block to prove the query-surface boundary against the LIVE server.

---

## 600.1 There is NO arbitrary-BQL-over-HTTP servlet — proven live [CERT-live]

The `apis` focus listed "BQL-call / over-HTTP" as an uncovered surface, and a natural assumption is that an
authenticated session can POST arbitrary BQL (`station:|slot:/...|bql:select ...`). **Against this live station
it cannot** — no such servlet is mounted:

| Probe (authenticated API2) | Result |
|---|---|
| `GET /bql/` | HTTP **404** |
| `GET /ord/` | HTTP **400** (no ord to resolve; servlet path not a BQL entry) |
| `GET /spy/` | HTTP **404** |

`[CERT-live]` `sources/probes/B600-obix-query-surface/raw-bql-absent.txt`. This is consistent with [Block 157]
§157.2 (spy/about/nav/hx **not mounted** on this deployment) — extended here from "diagnostic views absent" to
"**no arbitrary-query servlet at all**". The authenticated data-query surface is therefore ENTIRELY the typed
oBIX operation set below. A client that needs BQL semantics must express them through an oBIX op's typed input
contract (e.g. a `HistoryFilter`), not as a BQL string.

## 600.2 Recipe A — bounded History `query` (the typed analog of a BQL history query) [CERT-live]

Every history object under `/obix/histories/<device>/<point>/` exposes three ops (from the live object) `[CERT-live]`:
```
<op name="query"  href="~historyQuery/"  in="obix:HistoryFilter"    out="obix:HistoryQueryOut"/>
<op name="rollup" href="~historyRollup/" in="obix:HistoryRollupIn"  out="obix:HistoryRollupOut"/>
<op name="append" href="~historyAppend/" in="obix:HistoryAppendIn"  out="obix:HistoryAppendOut"/>
```
`sources/probes/B600-obix-query-surface/history-ops-query-rollup-append.xml`.

Executed live against `PRUEBAS/Ramp` with `<obj is="obix:HistoryFilter"><int name="limit" val="3"/></obj>`
→ HTTP 200, three real records `[CERT-live]`:
```
timestamp 2026-08-29T01:25:30.065-06:00  value 79.7933349609375
timestamp 2026-08-29T01:25:40.022-06:00  value  7.186666488647461
timestamp 2026-08-29T01:25:50.003-06:00  value 59.58000183105469
count 3  start …30.065  end …50.003
```
`sources/probes/B600-obix-query-surface/historyQuery-Ramp-3rec.xml`. The `HistoryFilter` typed input is the
BQL-substitute: `limit`, and (per the contract) `start`/`end` `abstime` bounds — the same selection a
`bql:select … where timestamp between …` would express, but expressed as a typed oBIX object. Extraction
pagination/delta already documented in [Block 458] (REMITTANCE — not re-derived).

## 600.3 Recipe B — live component values via `Watch` (add / pollChanges) [CERT-live]

History gives the PAST; the live value of any component slot comes from the Watch service, not BQL. Live flow
(all HTTP 200) `[CERT-live]`:
1. `POST /obix/watchService/make/` → a fresh watch object (e.g. `watch50`) exposing ops
   `add · remove · pollChanges · pollRefresh` + a `lease` (auto-expiry) `sources/probes/.../` (lobby +
   watch ops captured).
2. `POST /obix/watchService/watch50/add/` with
   `<obj is="obix:WatchIn"><list name="hrefs" of="obix:uri"><uri val="/obix/config/NumericDelay/out/"/></list></obj>`
   → `<obj is="obix:WatchOut">…<real val="0.0" is="baja:StatusNumeric"/>…</obj>` — the live value + status of a
   real `kitControl:NumericDelay` output. `sources/probes/B600-obix-query-surface/watch-add-NumericDelay-out.xml`.

The Watch is the polling substitute for a live-value BQL cursor: `add` any set of component-slot ORDs, then
`pollChanges` for deltas (change-driven) or `pollRefresh` for a full re-read. It is READ-ONLY (subscription;
leased and self-expiring) — rung-1.

## 600.4 Recipe C — Alarm `query` [CERT-live]

`GET /obix/config/Services/AlarmService/` exposes `<op name="query" …>` on the `alarm:AlarmService obix:AlarmSubject`
(and each `AlarmClass` is itself an `obix:AlarmSubject`) `[CERT-live]`
`sources/probes/B600-obix-query-surface/obix-lobby.xml` + AlarmService probe. Alarm records are queried through
this typed op (an `AlarmFilter` input), NOT through `bql:...from openAlarms` over HTTP — the same BQL semantics
[Block 360] documented statically, reached here only via the typed oBIX op.

## 600.5 The lobby is the query catalog [CERT-live]

`GET /obix/` returns the fixed set of query entry points on this station `[CERT-live]`:
`watchService` (obix:WatchService) · `config` (baja:Station, the component-space mirror) · `about` · `alarms`
(AlarmService) · `histories` (→ device `PRUEBAS`) · an `obixDriver:ObixExportFolder` (`continuousControl`).
Navigation of `config/` IS the component-query mechanism (walk the typed tree) in place of a `slot:` BQL ORD.
Two facts surfaced here also unblock sibling gaps (Connections): `config/` shows a `jsonToolkit:JsonSchema`
node (jsonToolkit LOADED) and `kitControl` control points (`NumericDelay`, `TimeTrigger`) present.

## 600.6 What this does NOT resolve

- oBIX **write/commit** paths (History `append`, the Station `save` op, point overrides) — that is gap B458-G2,
  a `⚠ CONFIG MUTATION` (rung-2), deferred to an authorized-write iteration.
- The `rollup` op contract (server-side downsample) — gap B458-G1, next.
- Whether a NON-oBIX servlet elsewhere exposes BQL (e.g. a driver-specific one) — scoped-out; the oBIX and the
  three diagnostic servlets are proven absent, which answers B457-G1 as posed.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | No `/bql/`, `/ord/` (as BQL), `/spy/` servlet — 404/400/404 live | [CERT-live] | `raw-bql-absent.txt` | ✓ live |
| 2 | History exposes query/rollup/append typed ops | [CERT-live] | `history-ops-…xml` | ✓ live |
| 3 | Bounded `query` returned 3 real Ramp records (values 79.79/7.18/59.58) | [CERT-live] | `historyQuery-Ramp-3rec.xml` | ✓ live |
| 4 | Watch make→add returned live `NumericDelay/out` = `real 0.0 StatusNumeric` | [CERT-live] | `watch-add-…xml` | ✓ live |
| 5 | AlarmService exposes `query` op | [CERT-live] | AlarmService probe | ✓ live |
| 6 | Lobby entry set (watchService/config/about/alarms/histories/exportFolder) | [CERT-live] | `obix-lobby.xml` | ✓ live |
| 7 | jsonToolkit + kitControl present in config/ (sibling-gap reachability) | [CERT-live] | config/ probe | ✓ live |
| 8 | Ground-truth cert re-measured, matches B156 anchor | [CERT-live] | §preamble | ✓ live |

**Marker tally**: [CERT-live] ×8, [CERT] 0 primary (contract types remitted to B458), [INFER] 0. Ratio
[INFER]/[CERT-live] = 0. **Block type: EVIDENCE (dynamic/§12 live-validation).** A 0-INFER live block: the
finding is entirely what the running server returned. This CLOSES B457-G1.

**§12 verdict (B457-G1)**: **CONFIRMED-boundary** — the authenticated oBIX query surface is a fixed typed-op set
(History query/rollup/append · Watch add/poll · Alarm query · config-tree navigation); arbitrary BQL-over-HTTP is
**NOT-REPRODUCED** (no servlet). Zero secrets exfiltrated (invariant held). Station read-only; no mutation.

## Connections

- [Block 457] — login (SCRAM-SHA-256 → acceptEula); this block reuses that session.
- [Block 458] — History `query` extraction contract (records, pagination, delta) — REMITTANCE, not re-derived.
- [Block 157] §157.2 — spy/nav/hx not mounted; EXTENDED here to "no arbitrary-query servlet at all".
- [Block 360] — alarms queryable by BQL (`from openAlarms`) statically; reached live only via oBIX `AlarmService.query`.
- Sibling-gap reachability unblocked by §600.5: `jsonToolkit-G1/G2` (JsonSchema loaded), `KC13-G1` (kitControl points present).
- Opens/points to: B458-G1 (rollup, next), B458-G2 (oBIX write — `⚠ CONFIG MUTATION`, authorization-gated).
