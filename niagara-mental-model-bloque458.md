# Block 458 — Bulk data extraction from a live N4 station over oBIX: the History `query` op (POST + GET forms), pagination, incremental delta, and config dump

**Focus:** api-access (document-mode thread). Extends [B457] (the login) into the data-extraction method. Companion block.

**Mode:** DOCUMENT (§20) — CAPTURE of a reusable extraction method + its reference tool, not gap discovery.

**Origin:** the cross-session api-access work built and **validated live** a bulk exporter against the operator's OWN station (histories→CSV, config dump), reusing the [B457] login. Documented at the operator's explicit (direct) request.

**Scope:** the oBIX History query contract + the two invocation forms + pagination/delta + config traversal. NOT: BQL-over-REST; oBIX write/commit semantics; the rollup aggregation math.

**Evidence base & provenance.** Two channels:
- `[CERT]` — the oBIX contract from N4 code (`sources/decompiled/obix-contracts/`).
- `[CERT-live]` — the two invocation forms, pagination, delta, and config dump were verified **live** against the real station by the **cross-session** work (agent "camara"); **not re-run in this session**. Reference implementation preserved in-repo: `sources/probes/B457-n4-login/niagara-n4-export.py`.

**SECRETS DISCIPLINE.** User/role only (`API2`), never secrets. **Action item persists: rotate the exposed API2 credentials.**

---

## 458.1 — The oBIX History contract (from code) `[CERT]`

An oBIX `obix:History` object exposes two operations and a feed (`sources/decompiled/obix-contracts/History.java:17,18,19`):

- `<op name='query'  in='obix:HistoryFilter' out='obix:HistoryQueryOut'/>` — the workhorse.
- `<op name='rollup' in='obix:HistoryRollupIn' out='obix:HistoryRollupOut'/>` — server-side aggregation by interval (use to downsample instead of pulling raw).
- `<feed name='feed' in='obix:HistoryFilter' of='obix:HistoryRecord'/>` — streaming/watch.

**Input — `obix:HistoryFilter`** (`HistoryFilter.java:12-14`): `limit` (int, `null='true'`), `start` (abstime, `null='true'`), `end` (abstime, `null='true'`). All optional; null = unbounded.

**Output — `obix:HistoryQueryOut`** (`HistoryQueryOut.java:16,24`): `count` (int), `start`/`end` (abstime, effective range), and `data` = `<list of='obix:HistoryRecord'>`.

**Each `obix:HistoryRecord`** (`HistoryRecord.java:12,13`): `timestamp` (abstime) + `value` (obj, typed per the point — real/bool/enum; read its `@val`).

## 458.2 — Two ways to invoke the query op

**(a) POST an `obix:HistoryFilter`** (the pure oBIX form, from the code contract) `[CERT]`+`[CERT-live]`: read the History object, take the `query` op's `href`, and `POST` it an XML `obix:HistoryFilter` body (`start`/`end` abstime ISO-8601 with offset, `limit` int), `Content-Type: text/xml`.

**(b) GET the `~historyQuery` convenience href with query-params** (the simpler HTTP form) `[CERT-live]`: the station publishes ready-made refs on the History object itself, observed live —
`<ref name="unboundedQuery" href="~historyQuery?limit=1000"/>`, plus `today`, `last24Hours`, `lastWeek`, `lastMonth`, `yearToDate`. So:
`GET /obix/histories/<device>/<name>/~historyQuery?start=<ISO>&end=<ISO>&limit=<N>` returns the **same** `HistoryQueryOut`, no body to serialize. The reference tool uses this form (`niagara-n4-export.py:99-105,179`). Both forms are valid; (b) is easier for a plain HTTP client.

## 458.3 — Pagination contract `[CERT-live]`

The query returns at most `limit`/`page-size` records. To pull a full history: when a page fills to `page_size`, **re-query with `start` = last record's timestamp + 1 ms**, and **dedupe by timestamp** (the boundary record repeats). Stop when a page returns fewer than `page_size`, zero new, or the total cap is hit. Implemented at `niagara-n4-export.py:93-126` (the +1 ms advance at `:85-90,122`). Validated live with `page-size 3` → 5–6 pages, timestamps unique and ordered.

## 458.4 — Incremental / delta export `[CERT-live]`

For recurring pulls (cron): keep a per-history state file of the last exported timestamp (`.export-state.json`); next run sets `start = last + 1 ms` and **appends** only new records to the existing CSV (`niagara-n4-export.py:147-193`, esp. `:177-186`). Validated across two runs: 10 → 20 rows, no duplicates, state updated.

## 458.5 — Config dump `[CERT-live]`

Walk `/obix/config/` recursively following `<ref href>` children; for each object carrying a `val`, record `name`/`href`/`is`/`val` (`niagara-n4-export.py:197-231`). Depth-bounded (`--max-depth`, default 6). Caution: this station's `componentCount` is 9743 — an unbounded walk is large.

## 458.6 — Reference implementation + what it extracted

`sources/probes/B457-n4-login/niagara-n4-export.py` logs in ONCE (reuses `N4Client` from [B457] via importlib, `:29-34`), then `--histories DIR` (CSV per history) and/or `--config FILE`. Stdlib only; password via prompt/`$N4_PW`; no secrets embedded. Validated live over **26 histories**, including trended **UPS Panduit** points (`cargaActualPoint`, `nivelBateriaPoint`, `voltajeEntradaPoint`, `temperaturaBateriaPoint`, `tiempoRestantePoint`, `potenciaAparenteSalidaPoint`, `conteoFallasEntradaPoint`, …) plus `Ramp`/`Hum` and `AuditHistory`/`LogHistory`/`SecurityHistory`. `[CERT-live]` (cross-session).

## 458.7 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | History exposes `query` (in HistoryFilter, out HistoryQueryOut) + `rollup` + `feed` | `[CERT]` | `sources/decompiled/obix-contracts/History.java:17` |
| 2 | HistoryFilter = limit/start/end (all nullable); QueryOut = count/start/end + data list | `[CERT]` | `HistoryFilter.java:12`, `HistoryQueryOut.java:16` |
| 3 | HistoryRecord = timestamp (abstime) + value (typed obj, read @val) | `[CERT]` | `HistoryRecord.java:12`, `sources/probes/B457-n4-login/niagara-n4-export.py:64` |
| 4 | Two invocation forms: POST HistoryFilter, and GET ~historyQuery?params (convenience refs live) | `[CERT]`+`[CERT-live]` | `History.java:17` + `niagara-n4-export.py:179` |
| 5 | Pagination: re-query start=lastTs+1ms, dedupe by timestamp | `[CERT-live]` | `niagara-n4-export.py:93-126`; validated page-size 3 |
| 6 | Incremental delta via per-history last-timestamp state + CSV append | `[CERT-live]` | `niagara-n4-export.py:147-193`; validated 10→20 |
| 7 | config/ recursive ref-walk dumps name/href/is/val; 26 histories incl. UPS Panduit points | `[CERT-live]` | `niagara-n4-export.py:197-231`; cross-session run |

**Tally:** 7 claims — 3 `[CERT]` · 4 `[CERT-live]`/mixed (labelled, cross-session provenance stated) · 0 unmarked. Consistent with [B457].

**Left out (named):** the rollup aggregation contract math; oBIX write/invoke on non-history ops; BQL-over-REST; exact `value` sub-type tags per point.

## 458.8 — Connections
- Closes the api-access outline with [B457]: [B457] = get IN (login), [B458] = get DATA OUT (oBIX extraction). Same cross-session collaboration as the video runbooks [B453]–[B456].
- Exercises live the oBIX/history layers the corpus documented structurally (e.g. `BLocalHistoryDatabase`, oBIX server) but never ran against a station.

## 458.9 — Open gaps
- **B458-G1** — the `rollup` op contract (HistoryRollupIn/Out) for server-side downsampling — investigable live.
- **B458-G2** — write/commit paths over oBIX (this method is read-only) + CSRF/session behaviour.
- **Security action (not a gap):** rotate the exposed `API2` credentials.
