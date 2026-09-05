# `station-load.sh` — probe recipe (§19 build-PoC design, NOT YET EXECUTED)

> **Status:** requires-execution DESIGN for campaign 9. This is the concrete recipe distilled from
> [Block 806] §806.7/§806.8 + [Block 811] §811.2/§811.4 so the probe can be implemented without re-reading both
> blocks. Nothing here has run against a live JACE yet — the field names / JSON shapes / auth model are marked
> `[requires-execution]` and are exactly the B806-G1 / B811-G1 gaps.
>
> **Scope split (do not merge with `station-snapshot.sh`):**
> - `station-snapshot.sh` ([Block 811]) = config/console **DRIFT** → HASH `config.bog` + latest
>   `console_backup_*.txt`, DIFF vs previous, feed `schema-risk.sh` / `triage-console.sh`.
> - `station-load.sh` (THIS) = engine/capacity **SATURATION** → SAMPLE numeric metrics, THRESHOLD vs limits.
>   Same read-only Fox/spy transport, different output (samples, not hashes). Ship as two tools.

## 1 — Transport (read-only, no station-save side effect) `[CERT-doc / CERT]`
All reads go over Fox/spy ORDs; NONE forces `Station.saveSync` (contrast the `.dist` full backup, which does —
`BFoxBackupJob.java:108`, [Block 811] §811.4, MANUAL-only). Base ORD form:
`ip:<IP>|fox:|spy:/<path>` (Fox session; superuser for spy). ([Block 806] §806.7, [Block 811] §811.2)

| Surface | ORD / endpoint | Gives | Perm |
|---|---|---|---|
| Resource Manager | `ip:<IP>\|fox:\|spy:/metrics` | globalCapacity counts + limits | superuser |
| Engine metrics | `ip:<IP>\|fox:\|spy:/sys/engineManager` | `engine.scan.lifetime/peak/usage`, queue sizes (`EngineManager.java:414-431`) | superuser |
| Engine hogs | `ip:<IP>\|fox:\|spy:/sys/engineManager/hogs` | top-100 components by CUMULATIVE engine time (`EngineManager.java:437-443`) | superuser |
| Platform log | `ip:<IP>\|fox:\|spy:/platform diagnostics/log` | platform daemon log | superuser |
| Console (live) | Application Director "Stream To File", platform daemon `:5011` | live stdout/stderr | platform creds |
| Console (file) | `ip:<IP>\|fox:\|station:\|file:^^console_backup_<ts>.txt` | console backup (derive `<ts>`) | station adminWrite |

There is NO unauthenticated HTTP GET for these — a Fox login (or platform creds for `:5011`) is required
([Block 811] §811.2). oBIX (`obix:|`) gives component-tree reads only, no console/metrics.

## 2 — Fields to sample + threshold `[CERT for the limits; requires-execution for exact spy field spelling]`
From the kit-citable budget table ([Block 806] §806.8):

| Metric (source ORD) | Field | Budget / threshold → action |
|---|---|---|
| `spy:/metrics` | devices / points / links / histories vs limits | typical JACE cap 25 dev · 500 pt · 400 link · 125 hist (`docPlatform.txt:5528,5585`). **>100% of any limit = WARN on boot · >110% = station will NOT boot** (`docPlatform.txt:2458-2459`) → block the deploy |
| `spy:/sys/engineManager` | `engine.scan.usage` (+ `lifetime`/`peak`) | sustained high usage = engine saturating; no per-scan % threshold exists in code — see the OPEN GAP below (`EngineManager.java:414-431`) |
| `spy:/sys/engineManager` | timer queue sizes (short ≤2100 ms · medium ≤61000 ms · long) | queues are UNBOUNDED (`EngineManager.java:61-65`) — a growing long-queue = tickets outrunning the engine |
| `spy:/sys/engineManager/hogs` | top-100 cumulative | a NEW component entering the top-100 between samples = a slow callback (the closest thing to a per-callback signal) |

**OPEN GAP carried from [Block 806] §806.8:** there is NO per-callback slow-threshold in the engine (hog accounting
is cumulative only, `EngineManager.java:437-443`). So the PROBE is the threshold: alert on a hog's *delta* between
samples and on any timer queue that grows monotonically. Jetty worker-pool size and the kRU cap are also OPEN GAPs
(not in the code) — do not assert them; read them live if a future field exposes them.

## 3 — Polling interval `[CERT-grounded / INFER]`
- **Read-only sampling: 15–30 min** (the safe interval from [Block 811] §811.4 FOX read-only mode; per-event
  immediately after a deploy). All the §1 spy/file reads are cheap and carry no `saveSync`. `[INFER, grounded]`
- **NEVER** trigger a `.dist` backup on a polling cycle (bog flush + crawl, flash-wear on a JACE —
  [Block 806] §806.3 / [Block 811] §811.4). The load probe must not call BackupService at all.

## 4 — What to capture / hash / diff `[CERT-grounded]`
- **Capture (per sample):** the four metric rows above → one timestamped line/JSON record. This is a NUMERIC sample
  set, not a hash — you threshold and trend it, you do not hash it.
- **Hashing belongs to `station-snapshot.sh`, not here:** `config.bog` (schema/link drift → `schema-risk.sh`,
  [Block 795]/[Block 799]/[Block 807]) and the latest `console_backup_*.txt` (new-exception delta →
  `triage-console.sh`, [Block 800]). The load probe may co-emit the `config.bog` SHA-256 for correlation, but the
  drift analysis is the snapshot tool's job. ([Block 811] §811.4)
- **Diff/trend (vs baseline):** globalCapacity % of each limit (warn ≥100%, block ≥110%); `engine.scan.usage`
  trend; top-hogs set delta (new entrant = investigate); long timer-queue monotonic growth.

## 5 — Console encoding caveat `[CERT]`
If the probe also pulls the console (`file:^^console_backup_*.txt` or `:5011` stream): a Spanish-locale station
writes `INFORMACIÓN/ADVERTENCIA/GRAVE` with accents as non-UTF-8 mojibake — **parse latin-1/bytes, never assume
UTF-8**; attribute a line by its `com.angeles.*` frame OR the `[coldRoomPan|dashboardpan|chihuahua]` tag OR the
`[sys.xml]`/`Cannot load station` channel. ([Block 800] §800.5/§800.8, [Block 811] §811.2)

## 6 — Exit / gate wiring (proposed) `[INFER]`
- exit **0** = all sampled metrics under WARN thresholds.
- exit **1** = a WARN threshold crossed (globalCapacity ≥100%, a hog delta, a growing queue) — surfaced, non-fatal.
- exit **2** = a BLOCK threshold crossed (globalCapacity ≥110% → station won't boot) — fail a pre-deploy gate.
- exit **3** = environment (no Fox creds / host unreachable / spy denied) — NOT a load verdict.
Mirror the `station-snapshot.sh` env-vs-verdict exit discipline so both compose in one post-deploy checklist.

## 7 — Requires-execution gaps (campaign 9 must resolve on a live JACE)
- **PoC-G1** ([Block 806]-G1): read `spy:/metrics` + `spy:/sys/engineManager` + `/hogs` on a live JACE to capture
  the EXACT field names, value types, and serialization (Fox spy returns — HTML table? typed BValue? JSON?). The §2
  field spellings are from code/doc, not from a live read.
- **PoC-G2** ([Block 811]-G1): confirm the FOX read-only reads produce ZERO station-save side effect (watch the
  `config.bog` mtime across a sample) — the read-only proof the scheduled probe needs.
- **PoC-G3:** the auth model — does spy require a superuser Fox session, and can a dedicated read-only service
  account reach `spy:/metrics` without adminWrite? (drives how the probe stores creds.)

## Cites
`niagara-mental-model-bloque806.md` §806.7/§806.8 (`EngineManager.java:40,61-65,414-431,437-443,558`;
`docPlatform.txt:2458-2459,5528,5585`) · `niagara-mental-model-bloque811.md` §811.2/§811.4
(`BFoxBackupJob.java:108`) · `niagara-mental-model-bloque800.md` §800.5/§800.8 (console encoding + channels).
