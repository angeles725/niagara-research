# B742 · ColdRoomPan / CompPan / DashboardPan — consolidated rt refactor & hardening plan (sequenced, deploy-safe)

> **Scope**: turn the rt campaign (B729-B741) into ONE actionable, sequenced plan for our modules —
> bug-fixes, hardening, structural refactor, and operator-gated features — batched so each deploy is safe
> under the schema/restart rules. Foco: `own-modules-audit`. This is a plan (forward-looking); every item
> traces to a cited block.
>
> **Sources**: FUENTE 1 B729 (lifecycle), B730 (idioms), B731 (audit/backlog), B732 (alarms), B733 (0-10V/
> PID/math), B734 (points), B735 (slots/facets/links), B736 (BStatus), B737 (engine/watchdog/composition),
> B738 (proxyExt/facets/propagateFlags/icons), B739 (schema/.bog), B740 (enum), B741 (QA/tests). Our source.

---

## 742.1 — Sequencing principles (what makes a batch safe) `[CERT]`
- **Add, don't retype** (B739): every new field is a NEW slot; never change an existing slot's type on a
  module with saved data. This lets us add slots freely across batches.
- **-rt change ⇒ station restart** (B729/B737): every code change needs a maintenance-window restart; group
  changes to minimize restarts.
- **Test before deploy** (B741): pure unit tests for any changed decision/safety logic; build-verify ALL
  PASS; live smoke after restart.
- **One theme per batch**: don't mix a bug-fix with a big refactor — keep the cold-boot verification clean
  (Codig's rule for the defrost build).

## 742.2 — Batch 1: correctness bugs (deploy first) `[CERT]`
| Item | Block | Status |
|---|---|---|
| `BDefrostController` `started()`+`clockChanged()`+`nextDefrostTime`+applyRunCmd guard | B729 | **compiled (Codig), pending deploy + cold-boot test** |
| `BCompressorControl` `started()` (tick: lead/lag hours + start-prove freeze on late-mount) | B729/B737 | open — add to a build |
Both are the SAME class of bug (timer armed only in atSteadyState). Verify the defrost one via the rigorous
cold-boot test (Modo=Intervalo + SAVE + restart + read Modo AND nextDefrostTime) BEFORE bundling more.

## 742.3 — Batch 2: testing + safety hardening `[CERT/INFER]`
| Item | Block | Type |
|---|---|---|
| Extract `DefrostControl` (pure) + unit tests: interval math, interlock FIFO, terminate-by-temp | B741 | test (HIGH — untested subsystem that failed) |
| Anti-anchor-stale guard in the interval math: `|elapsed|>3·interval → interval` | B729-G2 | hardening (kills future-`lastDefrostTime` giant delay) |
| `DEFAULT_ON_CLONE` on HOA mode slots (`valveMode/fanMode/resistanceMode`) | B731 | hardening (clone in commissioning inherits HAND/OFF) |
These are all ADD/behavior changes (no retype) → safe; deploy together after Batch 1 is verified.

## 742.4 — Batch 3: structure & readability refactor (bigger) `[INFER, grounded]`
| Item | Block | Note |
|---|---|---|
| Compose `BEvaporatorUnit` (25 flat slots) into child components: `timing/outputs/hoa/freeze` | B737 §B.3 | changes ORDs/link paths → migrate links; do in one deliberate refactor |
| Add `units`+`precision` facets to temp/pressure/percent slots (bare doubles today) | B735/B738 | HMI + links show engineering units |
| Distinct `getIcon()` (PNG/SVG resource) per component | B738 | readable tree/wire sheet |
| Set OVERRIDDEN on forced outputs; NULL on no-data; propagate DOWN/STALE | B736 | honest status to HMI/subscribers |
Higher effort + link migration; schedule as a dedicated refactor, not mixed with a bug-fix deploy. Composition
changes slot paths → re-point any external links/ORDs (dashboard facade, oBIX).

## 742.5 — Features (operator decision — separate track) `[CERT]`
Not bugs; gated on a requirement from the operator:
| Feature | Block | Decision |
|---|---|---|
| Real console alarms (room/coil/discharge) via `BAlarmSourceExt`+`BOutOfRangeAlgorithm` on the PROXY POINTS | B732 | do we want ack/log/escalate alarms? |
| History/trend on temps via `BHistoryExt` | B733/B731 | do we want temperature trends? |
| Modulating 0-10V (condenser-fan head pressure) via `BLoopPoint`→`BNumericWritable`→AO | B733 | only if modulating hardware is added |

## 742.6 — Recommended order
1. **Batch 1** (defrost build) → **cold-boot test** → confirm.
2. Add **CompPan `started()`** (can ride Batch 1's window or a next one).
3. **Batch 2** (DefrostControl+tests, anchor guard, DEFAULT_ON_CLONE) — one build, tested.
4. **Batch 3** (composition + facets + icons + status) — a dedicated refactor with link migration.
5. **Features** — only when the operator confirms each requirement.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Every plan item traces to a cited campaign block; no new framework claims | [CERT] | B729-B741 citations inline |
| 2 | Add-slot is safe, retype is not; -rt needs restart — the constraints on batching | [CERT] | B739; B729/B737 |
| 3 | Defrost fix is compiled-pending-cold-boot; CompPan started() is open; DefrostControl is untested | [CERT] | B729/B741; Codig status |
| 4 | Composition/facets/icons/status/features are correctly classed as refactor vs operator-feature | [INFER] | B731-B738 classifications |

**Tally**: 3 [CERT], 1 [INFER]. No unmarked claims. Forward-looking plan; execution is a build task, not research.

## Connections
- The whole rt campaign **B729-B741**; kit `types/logic.md` + `build-verify.md`; retro `started()`.

## Open gaps
- **B742-G1**: exact link/ORD migration list for the Batch-3 composition (which external links target
  `BEvaporatorUnit` slots that would move under a child) — an implementation survey, do before that refactor.
