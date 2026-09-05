# B804 · History-extension authoring — a `BHistoryExt` IS a point extension; pick Interval vs COV; `BHistoryConfig` sets capacity + rollover; ONE ext per logged slot `[CERT]`

> **Scope**: the AUTHOR-side recipe for logging a component's value to a Niagara history — the residue the exemplar
> census ([B772]-[B791]) did not cover. How you attach a history extension to a point, the two collection modes
> (timed Interval vs change-of-value COV), the storage config (capacity + full/rollover policy), where the data lands,
> and the one real gotcha (an independent extension per logged slot), grounded in our own `chihuahua` module.
>
> **Sources**: FUENTE 3 (read-only, file:line [CERT], 4.14.0.162) — `javax.baja.history.ext.{BHistoryExt,
> BIntervalHistoryExt,BCovHistoryExt,BNumericCovHistoryExt}`, `javax.baja.history.{BHistoryConfig,BFullPolicy}`. FUENTE 1
> (REMITTANCE, cited not re-derived): [B772] (point-extension mechanism — a HistoryExt is one), [B33]/[B14]/[B16] (the
> history SUBSYSTEM / database / oBIX read side), [B801] (the `Clock` interval floor), [B802] (`Sys.getService`),
> [B163]-[B177] (chihuahua). Own-module consumer: `chihuahua-rt/BChiDatalogger`. All cites grep-verified this session.

---

## 804.1 — A history extension IS a point extension `[CERT]`
`BHistoryExt extends BPointExtension implements BIHistorySource` (`BHistoryExt.java:116`) — so you author and place it
exactly like any [B772] point extension: drop the matching typed HistoryExt under a control point, and it logs that
point. Base slots (the authoring surface): `enabled` (`:119`), `activePeriod` (`:120`, a `BBasicActivePeriod` — WHEN to
collect), `historyName` (`:122`, a `BFormat` defaulting to `"%parent.name%"` — the history is named after the parent
point), `historyConfig` (`:124`, the `BHistoryConfig` below), plus read-only `status`/`active`/`faultCause`.

## 804.2 — Two collection modes — pick by change rate `[CERT for the classes; INFER for "pick by"]`
Both are abstract bases with a typed subclass per value type (Numeric/Boolean/Enum/String):
- **Interval (timed)** — `BIntervalHistoryExt extends BHistoryExt` (`BIntervalHistoryExt.java:36`): an `interval`
  `BRelTime` (default 15 min, **facet `min = 1 s`** `:37`) drives a periodic `intervalElapsed` action (`:38,:51`) via a
  `Clock.Ticket` (`:23`). Steady sampling of a continuously-changing analog. (The 1 s min ties to [B801] — the engine
  rejects a `<= 0` delay; the facet floor keeps the author above it.)
- **COV (change-of-value)** — `BCovHistoryExt extends BHistoryExt` (`BCovHistoryExt.java:11`), typed e.g.
  `BNumericCovHistoryExt extends BCovHistoryExt` (`BNumericCovHistoryExt.java:51`): records only when the value moves
  beyond a tolerance. Event-driven — right for booleans/enums/setpoints and slow analogs; avoids logging noise.

## 804.3 — `BHistoryConfig` = the storage contract (capacity + rollover) `[CERT]`
`BHistoryConfig extends BComponent` (`BHistoryConfig.java:85`) carries: `id` (a `BHistoryId`, `:86`), `source`/
`sourceHandle`, `timeZone`, and the two the author MUST set consciously:
- **`capacity`** — `BCapacity`, default `BCapacity.makeByRecordCount(500)` (`:93`). Bound it by record count (or time).
- **`fullPolicy`** — `BFullPolicy`, default **`roll`** in the config (`:94`; `BFullPolicy` `STOP=0`/`ROLL=1`, tags
  `"stop"/"roll"`, `BFullPolicy.java:12,15-18`). `roll` = circular buffer (oldest record dropped when full); `stop` =
  collection halts when full. **Subtlety**: the `BFullPolicy` enum's own `DEFAULT` constant is `stop` (`:19`), but a
  fresh `BHistoryConfig` defaults `fullPolicy` to `roll` — so a hand-built config rolls unless you set `stop`.

## 804.4 — Where the data lands `[CERT — REMITTANCE]`
Collection writes into `BHistoryService`'s database (`Sys.getService(BHistoryService.TYPE)` — [B802] service discovery),
keyed by the config's `BHistoryId`; the read/query/oBIX side is the history SUBSYSTEM ([B33]/[B14]/[B16], REMITTANCE).
The author's job ends at "place the ext + set the config"; the service does the persistence and rollover.

## 804.5 — Own-module consumer + the one real gotcha `[CERT]`
`chihuahua-rt/BChiDatalogger` logs `pressurePsi`/`pressureBar` via `BHistoryExt` on control points and states the rule
in-source: *"Both slots MUST have independent `BHistoryExt` instances — separate history records"*
(`BChiDatalogger.java:59-64`). **ONE history extension per logged slot** — a single ext cannot fan out to two slots;
each logged value needs its own ext (and thus its own `BHistoryId`/history). Sharing collides.

## 804.6 — Kit implication `[INFER, grounded in 804.1-5]`
PROPOSED `types/logic.md` §"logging a point to history": to log a value, add the matching typed HistoryExt under the
point — **Interval** for steady analog sampling (set `interval`, ≥ 1 s), **COV** for booleans/enums/setpoints/slow
analogs (set the tolerance); set `historyConfig.capacity` (a real record-count/time bound, not unbounded) and
`fullPolicy` (`roll` = circular, `stop` = halt-when-full); the history auto-names after the parent point. **One ext per
logged slot.** Anti-patterns: one ext shared across two slots (chihuahua's rule); unbounded/oversized capacity on a JACE;
an Interval below the 1 s facet floor ([B801]); logging a fast noisy analog by COV without a sane tolerance (log storm).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `BHistoryExt extends BPointExtension` — a history ext is authored/placed like any point extension; base slots enabled/activePeriod/historyName/historyConfig | [CERT] | BHistoryExt.java:116,119,120,122,124 |
| 2 | Interval mode = `BIntervalHistoryExt`, `interval` BRelTime (15min default, min 1s facet) driving periodic `intervalElapsed` via Clock.Ticket | [CERT] | BIntervalHistoryExt.java:36,37,38,51,23 |
| 3 | COV mode = `BCovHistoryExt` + typed (`BNumericCovHistoryExt`), records on change beyond tolerance | [CERT] | BCovHistoryExt.java:11; BNumericCovHistoryExt.java:51 |
| 4 | `BHistoryConfig` sets `capacity` (default 500 records) + `fullPolicy` (config default `roll`; enum stop=0/roll=1, enum DEFAULT=stop) | [CERT] | BHistoryConfig.java:85,93,94; BFullPolicy.java:12,15-19 |
| 5 | Data lands in BHistoryService keyed by BHistoryId; read side = history subsystem (REMITTANCE) | [CERT]+REMITTANCE | BHistoryConfig.java:86; B33/B14/B16 |
| 6 | Own-module rule: ONE independent BHistoryExt per logged slot (can't share) | [CERT] | BChiDatalogger.java:59-64 |

**Tally**: 5 [CERT] · 1 [CERT]+REMITTANCE. All file:line grep-verified this session. §804.6 kit implication + the
"pick by change rate" guidance are [INFER] grounded in the [CERT] class model. Dedupe: the history DATABASE/query/oBIX
side is [B33]/[B14]/[B16]; the point-extension mechanism is [B772]; this block adds only the author-side attach+config residue.

## Connections
- **[B772]** (point-extension authoring — a HistoryExt is one), **[B33]/[B14]/[B16]** (history subsystem/database/oBIX —
  the read side), **[B801]** (the Interval `min 1s` sits above the `Clock` `<= 0` floor), **[B802]** (`Sys.getService`
  for BHistoryService), **[B163]-[B177]** (chihuahua — BChiDatalogger consumer). Kit: `types/logic.md` §"logging a point to history".

## Open gaps
- **B804-G1** (bounded): the `BCapacity` time-based variant (`makeByTime`?) + the retention/rollup interaction with a
  supervisor archive — named, not traced. The record-count path + rollover policy are fully [CERT].
