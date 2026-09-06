# B824 · The "silent-protection" lint — a static check that flags a protection TRIP (an output forced OFF / a stage shed-or-held, per B821's 22-trip taxonomy) that reaches NO operator surface; absence-of-surface is decidable (WARN), is-the-slot-the-reason is advisory; proven FLAG on CP-1 low-suction + CR-3 freeze-reason, CLEAN on PR#5 defrostSkipped/lastSkipReason, at client `fbe9009` `[CERT for the shapes; INFER for the check]`

> **Scope** (narrow, sibling of [B820] in shape — closes [B821]-G1): design the ONE static lint [B821] §821.8 named — a
> check over an `-rt` source tree that flags a protection trip with no operator-visible surface (the [B821] §821.4 tier-4
> "SILENT" case). Decides the statically-decidable (WARN) vs human-review (advisory) boundary, the row grammar, the
> false-positive controls proven on the real client trees, a mutated-copy proof, and the RED shape for a C9 PR. Does NOT
> re-derive the protection taxonomy ([B821]) nor the surface tiers ([B821] §821.4) — REMITTANCE.
>
> **Sources**: FUENTE 1 (own modules, `[CERT]` at `fbe9009` = deployed 2.x, confirmed at the enclosing method) —
> `CompPan-rt/{CompressorControl,BCompressorControl}.java`, `ColdRoomPan-rt/{BEvaporatorUnit,BDefrostController}.java`.
> REMITTANCE — [B821] (protection anatomy + the 22-trip taxonomy + the silent list), [B820] (the demand-in-scope lint =
> the sibling absence-decidable shape), [B819] (NaN-never-demand + the data-flow-rule gap [B819-G1]), [B808] (who-watches
> = the surface a trip must reach), [B805] (RT control mechanisms + the one-bit trace). Type: lint design (`[INFER]`
> grounded in [CERT] shapes).

---

## 824.1 — What it keys on: the [B821] §821.4 tier-4 shape `[CERT — the shape]`
A **protection trip** ([B821] §821.2 taxonomy) is a decision that, on a condition over a process variable / mode / timer,
**forces an output OFF** or **sheds/holds a stage**. It is *silent* when the SAME decision reaches no operator surface:
no SUMMARY/OPERATOR status-or-reason slot written from it, no `BAlarmSourceExt`/`BAlarmRecord`, no readable reason slot —
only the forced OUTPUT slot changes ([B821] §821.4 tiers: 4 = silent; 3 = an effect slot only; 2 = a named status slot;
1 = an alarm). The lint flags tier-4 (and optionally advises tier-3→2 and the tier-1 gap for safety trips). This is the
concrete form of [B808] "a LOGIC fault must reach the operator."

## 824.2 — The static key (two-part scan) `[INFER, grounded]`
Over an `-rt` tree (the pure model + its `BComponent` adapter, the [B817] §817.3 seam):
1. **Find TRIPS** — an output-forcing / stage-reducing statement under a guard on a process variable, a mode, or a timer:
   - `setBool(<out>, false)` / `<out>.setValue(false)` / `set<Out>(…false)` guarded by an interlock/limit/mode (CR-3..CR-7).
   - `target = Math.min(target, …)` / `cmd[k] = false` / `continue` in a stage-pick, guarded by a limit/timer (CP-1/CP-4/5/6).
   - a `boolean` returned by an `*Inhibited()`/`*Trip()`/`*High`/`*Low` decision that feeds an output-force.
2. **Find SURFACES** — for that trip, in scope (same method OR one-level field→slot into the adapter):
   - a write to a **status/reason** slot — `get<Xxx>().setValue(…)` / `set<Xxx>(…)` where `<Xxx>` matches the surface
     allowlist `{*Alarm, *Fault, *Skip*, *Reason, *Status, *Mismatch, *Stuck, *Available, *Fallback}` **and the slot is
     SUMMARY/OPERATOR** (not the forced output); OR
   - a `BAlarmSourceExt` on the point / a raised `BAlarmRecord`.
   **Absence of any surface for a trip → WARN** (§824.3).
**Row grammar:** `WARN  lint-silent-protection  <file>:<line>  <method> forces <output>/sheds stage on <cond> — no status/reason/alarm surface in scope; add a *Alarm/*Reason SUMMARY slot or a BAlarmSourceExt`

## 824.3 — Decidable (WARN) vs human-review (advisory) `[INFER, grounded in B821-G1/B819-G1]`
- **Statically decidable → WARN** (never hard FAIL): a trip with ZERO status/reason/alarm slot written on its path and no
  named trip-field surfaced to one. The ABSENCE is decidable by a name/type scan plus the one-level field→slot follow
  (§824.4). **Not FAIL**: a legitimate trip that surfaces only via a tier-3 EFFECT slot, or whose surface is written in a
  method the one-level scan can't reach, would false-positive on a hard FAIL — WARN keeps it advisory ([B820] §820.3 rule).
- **Human-review → advisory**: whether a PRESENT status slot is semantically THE reason for THIS trip (vs an unrelated
  status) is a data-flow + naming judgment a static scan cannot settle ([B819-G1]) — so the presence case is a review line,
  never an automatic PASS/FAIL. And a SAFETY trip (a cutout protecting equipment) that has a tier-2 slot but NO tier-1
  alarm is an advisory UPGRADE line (the [B821] §821.4 cross-cutting gap), not an automatic flag.

## 824.4 — False-positive controls, proven on the real trees `[CERT]`
Three controls make the check flag the genuinely-silent trips and leave the surfaced ones clean:
- **(a) Effect-slot exemption** — the OUTPUT slot the trip forces (`valveOut`, `evapOut`, `condenserN`) is a SUMMARY slot
  that IS written, but it is the tier-3 EFFECT, not the reason. A slot written to the trip's OWN forced output does NOT
  count as a surface; only a status/reason/alarm-named slot does. Without this, every trip looks "surfaced" by its own
  output (a false-negative). `[CERT — the output slots are SUMMARY, B821 §821.4]`
- **(b) Pure-model→adapter field→slot follow (one level)** — a trip in the pure model whose condition is captured in a
  NAMED field that the adapter writes to a status slot IS surfaced. This is the CP-1-vs-CP-2 discriminator, both in
  `CompressorControl.step()`:
  - **CP-2 high-discharge = CLEAN**: the trip sets a NAMED field `this.dischargeHigh` (`CompressorControl.java:82,140`),
    and the adapter writes it to a SUMMARY slot `getDischargeHighAlarm().setValue(ctl.dischargeHigh)`
    (`BCompressorControl.java:1539` slot / execute write). Field→slot follows → surfaced. `[CERT]`
  - **CP-1 low-suction = FLAG**: the shed is INLINE — `if (suctionValid && c.suctionLowLimit>0d && suction<c.suctionLowLimit)
    target = Math.min(target, onCount-1);` (`CompressorControl.java:215`) — NO named field, and `suctionLowAlarm` is
    **ABSENT** from the whole module (`grep suctionLowAlarm CompPan-rt/src` → 0). Nothing to follow → silent → WARN.
    (The asymmetry is the tell: the symmetric CP-2 has an alarm slot, CP-1 — the more damaging vacuum trip — has none.) `[CERT]`
- **(c) Surface-name allowlist is the advisory seam** — matching `{*Alarm,*Fault,*Skip*,*Reason,…}` is a heuristic; a
  differently-named reason slot could be missed (false-negative) — this is why the check is WARN/advisory, not FAIL
  ([B819-G1] data-flow gap). Proven CLEAN on **PR#5's defrost-skip surface**: the skip decision writes
  `getDefrostSkipped().setValue(true)` (`BDefrostController.java:746`) + `setLastSkipReason(reason)` (`:747`) — both
  SUMMARY slots (`:63` `Flags.SUMMARY`) whose names match `*Skip*`/`*Reason` → surfaced → NOT flagged. `[CERT]`

**Also FLAG (silent, [CERT])**: **CR-3 freeze trip** — `freezeTripped` is a **private field** (`BEvaporatorUnit.java:1287`),
consumed by `valveInhibited()` (`:1106`) to force `valveOut` OFF; no status/reason slot and no alarm derive from it →
WARN. The valve LED (tier-3 effect) is exempt by control (a).

## 824.5 — Mutated-copy proof `[CERT — mutation]`
On `mktemp` copies of the real trees (the [B821]/campaign-8 mutation discipline):
- **Flag CLEARS on adding a surface**: give CP-1 a named field + adapter write — add `boolean lowSuction` set on the shed
  condition and `getSuctionLowAlarm().setValue(ctl.lowSuction)` — the WARN for `CompressorControl.java:215` disappears.
  Proves the check keys on the SURFACE, not the trip.
- **Flag APPEARS on removing a surface**: delete `getDefrostSkipped().setValue(true)` + `setLastSkipReason(reason)`
  (`BDefrostController.java:746-747`) — the defrost-skip decision now forces the skip with no surface → WARN appears.
  Proves the check BITES on the absence, on a path that is clean today.

## 824.6 — RED shape for a C9 PR `[INFER]`
`lint-silent-protection.sh <module-root>` (WARN-only advisory; exit 0 with WARN rows, `--strict` → exit 1 on any WARN —
the [B820]/[B788] lintable-vs-advisory split). RED fixtures:
- a `-rt` fixture with a SILENT trip (an inline output-force on a limit, no status slot) → 1 WARN row (grammar §824.2);
  and a SURFACED trip (a named field → a `*Alarm` SUMMARY slot) → 0 rows.
- run against the REAL trees at `fbe9009` (the [B821] real-module-smoke discipline, campaign8-close L1): **MUST** emit a
  WARN for CP-1 (`CompressorControl.java:215`) and CR-3 (`BEvaporatorUnit.java` freeze path) and **MUST NOT** flag the
  defrost-skip path (`BDefrostController.java:746-747`) nor CP-2/CP-7/CP-8/CP-9 (which write named `*Alarm`/`*Fault` slots).
- the two §824.5 mutated copies flip (RED→GREEN and GREEN→RED).

## 824.7 — Kit implication `[INFER]`
Fold as a standalone advisory `toolbelt/lint-silent-protection.sh` OR a `verify-module` review-line (like [B820] into
`lint-write-path.sh`), NOT a hard FAIL. Doctrine line for `types/logic.md` §"Protection anatomy" ([B821]): *a protection
trip that forces an output or sheds a stage MUST write a named SUMMARY/OPERATOR status-or-reason slot on the same path
(tier 2), and a SAFETY trip should raise a `BAlarmSourceExt` (tier 1); the silent-protection lint WARNs the tier-4 gap.*
Cite `[ev: corpus B821, B824]`. Closes [B821]-G1's "what static shape distinguishes a silent trip" with the decidable
absence (WARN) + the honest advisory seam (name allowlist + is-it-the-reason = review).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | The check keys on a [B821] trip (output-force / stage-shed) with no status/reason/alarm surface on its path | `[INFER]+[CERT shape]` | [B821] §821.2/§821.4; the §824.2 grammar |
| 2 | Absence-of-surface is decidable → WARN; is-a-present-slot-the-reason is advisory (never hard FAIL) | `[INFER]` | [B820] §820.3, [B819-G1] data-flow gap |
| 3 | CP-1 low-suction FLAGS: inline `Math.min(target,onCount-1)` on `suction<suctionLowLimit`, no named field, `suctionLowAlarm` absent | `[CERT]` | `CompressorControl.java:215`; grep `suctionLowAlarm` CompPan-rt → 0 |
| 4 | CP-2 high-discharge CLEAN: named field `dischargeHigh` → `getDischargeHighAlarm()` SUMMARY slot (the field→slot follow) | `[CERT]` | `CompressorControl.java:82,140`; `BCompressorControl.java:1539` |
| 5 | CR-3 freeze FLAGS: `freezeTripped` is a PRIVATE field feeding `valveInhibited`, no slot/alarm | `[CERT]` | `BEvaporatorUnit.java:1287,1106` |
| 6 | defrost-skip CLEAN: `getDefrostSkipped().setValue`+`setLastSkipReason` (SUMMARY) on the skip path | `[CERT]` | `BDefrostController.java:63,746,747` |
| 7 | Effect-slot exemption: the forced output (valveOut/condenserN) does not count as its own surface | `[CERT]` | the output slots are SUMMARY effects ([B821] §821.4 tier-3) |

**Tally**: 5 `[CERT]` · 2 `[INFER]` (the check design + the decidable/advisory boundary), grounded in the [CERT] shapes +
[B820]/[B819-G1]. Tree stated: `fbe9009`. Dedupe: the trip taxonomy + surface tiers are REMITTANCE ([B821]); the
demand-in-scope sibling shape is [B820]; this block adds the silent-protection check design, the three false-positive
controls (effect-slot exemption + pure-model→adapter follow + name-allowlist seam), the CP-1/CR-3/defrostSkipped proof,
and the mutated-copy RED shape.

## Connections
- **[B821]** §821.4/§821.6/§821.8 (the surface tiers, the silent list this lints, the lint it named — closes [B821]-G1),
  **[B820]** (the demand-in-scope lint = the sibling absence-decidable shape + the WARN-not-FAIL rule), **[B819]**/**[B819-G1]**
  (NaN-never-demand + the data-flow gap the advisory seam inherits), **[B808]** (who-watches — the surface a trip must
  reach), **[B805]** (RT control + the one-bit trace), **[B817]** §817.3 (the pure-model/adapter seam the field→slot follow
  crosses), **[B788]** (lintable-vs-advisory doctrine). Kit: `toolbelt/lint-silent-protection.sh` (WARN-only) or a
  `verify-module` review-line + the `types/logic.md` §"Protection anatomy" doctrine line.

## Open gaps
- **B824-G1** (bounded): the trip-detection grammar (§824.2 pt1) is a set of syntactic shapes; a trip expressed unusually
  (e.g. an output forced via a computed index or a helper two levels deep) is missed — refine the shape set against the
  22-trip corpus so the false-NEGATIVE rate is known before it ships even as advisory (inherits [B819-G1]'s data-flow
  question, shared with [B820-G1]).
- **B824-G2** (bounded): the surface-name allowlist (§824.4c) — a reason slot named outside `{*Alarm,*Fault,*Skip*,*Reason,…}`
  is a false-negative; either widen the list or add a TYPE signal (a `BStatusBoolean`/`BString` SUMMARY slot written on the
  trip path, regardless of name).
