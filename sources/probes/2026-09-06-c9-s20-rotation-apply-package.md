# C9 S20 — time-slice compressor rotation: apply package (R1, FIRST client PR — CompPan-rt)

Author: companero (Fable), 2026-09-06. Mechanics are FIXED by C9 `design.md` D1 (niagara-tools main); this package turns
D1 into the execute-only FILE-LEVEL diff plan, plus the client FASE 1/2/3 explainer, the schema-risk expectation and the
ROT5 golden method. **Client tip re-anchored: `a109249` (origin/main, 2026-09-06 — unchanged since the seed).** Every
line number below was re-read at `a109249` this session; where D1's citation drifted, the VERIFIED line is used.
`[ev: client CompressorControl.java @ a109249]` `[ev: design.md D1]` `[ev: seed S20 e7b614523]`

> **D1 citation drift (correction input for design.md):** D1's insertion-point anchors are exact (target clamp
> `:212-216`, stage move `:218-233`, `stageReady :220`, HOA loop `:235`). Its lower-file citations are off by ~60 lines:
> `resetTransient` is **:328** (D1 says :267-274), `seedRestart` **:346** (D1 :285-288), `pickLeastHoursOff` **:352**
> (D1 :291-301). Use the verified lines. `[ev: client CompressorControl.java:328,:346,:352 @ a109249]`

---

## 0. FASE 1/2/3 — the explainer for the client (what S20 changes and what it does not)

The CompPan rack control is staged in three FASES (`CompressorControl.java` javadoc `:28-47`):
- **FASE 1 (shipped)** — demand-count staging: the rooms decide who needs cooling (`room1..4Calling` → `demandCount`);
  the rack turns compressors on/off ONE stage at a time toward that count, with amperage proof-of-run (visual/alarm only,
  never controls staging) and **lead/lag rotation by real run-hours** persisted in `condenserNHours`. Suction/discharge
  pressures are SAFETY LIMITS only (block stage-up on high head, shed on the LP floor).
- **FASE 2** — the suction-pressure MODULATOR (a hysteresis band replaces the demand count as the primary target source
  when commissioned; `suctionBand > 0`). Same slots, pure-logic.
- **FASE 3** — floating suction (`floatingSuction`/`coilTD`/`localAtmPsi`; `SSTreq = setpoint − coilTD`; León ~1,800 m →
  ~11.82 psia). Operator-enabled after on-site measurement.

**What S20 adds:** today FASE 1's rotation acts ONLY at a stage event (stage UP picks the least-hours idle unit, stage DOWN
drops the most-hours running unit, `:226/:238`). Under steady demand there is no stage event, so the same units run for
as long as demand holds. S20 adds a **time-slice**: after a configurable continuous run (`rotationInterval`, e.g. 3 h) a
running compressor is swapped for the idle least-hours unit — make-before-break by default (start the incoming, then one
`stageDelay` later stop the outgoing), so the rack never loses a stage. **`rotationInterval = 0` (the default) disables it
and the control is byte-identical to today** (proven by ROT5). It does not touch FASE 2/3 and never overrides the operator
(HOA OFF stays out, HAND is untouched) or a safety (no swap on high head, on the LP floor, or with one unit available).
`[ev: client CompressorControl.java:28-47,:226,:238 @ a109249]` `[ev: seed S20]`

---

## 1. File-level diff plan (ordered; ALL pure-model changes first, adapter second, registration third, tests last)

### F1 — `CompPan-rt/src/com/angeles/CompPan/CompressorControl.java` (PURE model, no Baja) — 6 edits

| # | Anchor (verified @ a109249) | Edit |
|---|---|---|
| F1.1 | `Cfg` class, after `long stageDelayMs;` (`:88`) | add `long rotationIntervalMs; // 0 = time-slice rotation DISABLED` and `boolean rotationMakeBeforeBreak = true;` |
| F1.2 | transient block `:66-75`, after `private long belowSince` (`:75`) | add `private int  rotOut     = -1;             // S20: outgoing unit of a pending swap (-1 = none)` and `private long rotArmedMs = Long.MIN_VALUE; // S20: ms the pending swap was armed` |
| F1.3 | `resetTransient()` `:328` body | add `rotOut = -1; rotArmedMs = Long.MIN_VALUE;` — else a disable→enable leaves a phantom pending swap (D1) |
| F1.4 | **step 2b — COMPLETION**, insert AFTER the clamp `if (target > N) target = N;` (`:216`) and BEFORE the `// 3) Move ONE stage` comment (`:218`) | see block below. MUST precede the stage move: during the pending window `onCount == target + 1`, so ordinary staging would `pickMostHoursOn` and could drop the INCOMING unit (D1) |
| F1.5 | **step 3b — ARM**, insert AFTER the stage block's closing `}` (`:233`) and BEFORE `// 4) Manual HOA override` (`:235`) | see block below. The HOA loop stays the LAST word (D1) |
| F1.6 | new private helper beside `pickLeastHoursOff` (`:352`) | `pickLongestRunOn(now)` — the running AUTO unit with the largest `now - cmdSince[k]`, tie-broken by most `hours[k]`; returns -1 if none |

**F1.4 — step 2b (completion), verbatim:**
```java
    // 2b) S20 time-slice rotation — COMPLETION of a pending swap. Runs BEFORE the stage move so the
    //     onCount == target+1 window is resolved by dropping the INTENDED outgoing unit, never by
    //     pickMostHoursOn shedding the incoming one (design D1).
    if (rotOut >= 0 && (now - rotArmedMs) >= c.stageDelayMs)
    {
      if (c.rotationMakeBeforeBreak) { cmd[rotOut] = false; cmdSince[rotOut] = now; }   // drop the outgoing
      else                           { int in = pickLeastHoursOff(now, c.minOffMs);     // break-before-make: add the incoming now
                                       if (in >= 0) { cmd[in] = true; cmdSince[in] = now; } }
      lastStageMs = now; rotOut = -1; rotArmedMs = Long.MIN_VALUE;
    }
```

**F1.5 — step 3b (arm), verbatim — the 10 gates in D1 order:**
```java
    // 3b) S20 time-slice rotation — ARM a swap. Steady demand only; every staging safety re-checked
    //     with the SAME expressions as steps 2/3 (never a copied constant). HOA loop below stays last.
    if (c.rotationIntervalMs > 0                                                          // 1 enabled (0 = disabled sentinel)
        && rotOut < 0                                                                     // 2 one swap in flight
        && onCount == target                                                              // 3 steady demand only
        && stageReady                                                                     // 4 same expr as :220
        && !dischargeHigh                                                                 // 5 never add on high head (:213)
        && !(suctionValid && c.suctionLowLimit > 0d && suction < c.suctionLowLimit)      // 6 LP floor sheds, never adds (:214)
        && available >= 2)                                                                // 7 a single unit cannot rotate (:173-175)
    {
      int out = pickLongestRunOn(now);                                                    // 8 running AUTO unit, longest continuous run
      if (out >= 0 && (now - cmdSince[out]) >= Math.max(c.rotationIntervalMs, c.minOnMs))
      {
        int in = pickLeastHoursOff(now, c.minOffMs);                                      // 9 inherits HOA-OFF + min-off + least-hours
        if (in >= 0 && hours[in] < hours[out])                                            // 10 only if it REDUCES divergence
        {
          if (c.rotationMakeBeforeBreak) { cmd[in] = true; cmdSince[in] = now; }          // make-before-break: start incoming now
          else                           { cmd[out] = false; cmdSince[out] = now; }       // break-before-make: drop outgoing now
          lastStageMs = now; rotOut = out; rotArmedMs = now;
        }
      }
    }
```
NOTE `onCount`/`available`/`stageReady` are the step-local values already computed at `:173-178` and `:220`; no
recomputation. `hours[k]` integration at `:158` runs BEFORE both blocks (feedback loop `:145-168`), so a swap changes which
unit accrues from the NEXT cycle and never rewrites accrued hours — `condenserNHours` stay monotonic (D1).
`[ev: client CompressorControl.java:145-168,:158,:173-178,:212-233 @ a109249]` `[ev: design.md D1 gates 1-10]`

**F1.6 — helper, verbatim (place after `pickLeastHoursOff`, `:352+`):**
```java
  /** S20: the running AUTO compressor with the LONGEST continuous run (now - cmdSince), tie → most hours. -1 if none. */
  private int pickLongestRunOn(long now)
  {
    int best = -1; long bestRun = -1L;
    for (int k = 0; k < N; k++)
    {
      if (!cmd[k] || modes[k] != MODE_AUTO) continue;          // HAND (MODE_ON) untouched — re-forced ON at the HOA loop anyway
      long run = now - cmdSince[k];
      if (run > bestRun || (run == bestRun && best >= 0 && hours[k] > hours[best])) { best = k; bestRun = run; }
    }
    return best;
  }
```

### F2 — NEW `CompPan-rt/src/com/angeles/CompPan/BRotationMode.java` (frozen enum — B828 §828.7-safe)

CompPan has NO frozen enum today (only `BCompressorControl`, `CompressorControl`, `CpLog`), so this is a brand-new type.
It is NEW and NON-LINKED, so it may be a frozen enum legally; the existing HOA slots `condenser1..3Mode` are `double`
(`BCompressorControl.java:392-409`) and dashboard-LINKED — they MUST NOT be retrofitted (the live "Missing class" trap,
B828 §828.7). A frozen enum carries its range intrinsically — NO `BFacets.RANGE` needed (B828).
```java
package com.angeles.CompPan;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.*;

/** S20 rotation mode: makeBefore (start incoming, then stop outgoing after stageDelay) | breakBefore. */
@NiagaraType
@NiagaraEnum(range = { @Range("makeBefore"), @Range("breakBefore") }, defaultValue = "makeBefore")
public final class BRotationMode extends BFrozenEnum
{
  /*+ slotomatic +*/   // makeBefore = 0 (default), breakBefore = 1
}
```
`[ev: corpus B828 §828.7]` `[ev: corpus B4 (BFrozenEnum/@NiagaraEnum/@Range model)]` `[ev: design.md D1]`

### F3 — `CompPan-rt/src/com/angeles/CompPan/BCompressorControl.java` (adapter) — 2 edits

| # | Anchor (verified) | Edit |
|---|---|---|
| F3.1 | after the `powerOnDelay` property (`:380-389`, the shape precedent) | add the two `@NiagaraProperty` below |
| F3.2 | in `execute()` "1) Config -> Cfg" block, beside `cfg.dischargeHighLimit = …` (`:1904`) | add `cfg.rotationIntervalMs = getRotationInterval().getMillis();` and `cfg.rotationMakeBeforeBreak = getRotationMode().getOrdinal() == 0;` — the `ctl.step(...)` call (`:1972-1973`) is UNCHANGED, the fields ride the existing `Cfg` |

```java
// S20 time-slice rotation: continuous run after which a running compressor is swapped for the
// idle least-hours unit. 0 (default) = DISABLED = today's stage-event-only rotation, byte-identical.
@NiagaraProperty(
  name = "rotationInterval",
  type = "BRelTime",
  defaultValue = "BRelTime.make(0)",
  facets = @Facet("BFacets.make(BFacets.MIN, BRelTime.make(0), BFacets.MAX, BRelTime.makeHours(24))"),
  flags = Flags.SUMMARY | Flags.OPERATOR
)
// S20: makeBefore (default: start the incoming, then stop the outgoing after stageDelay — the rack
// never loses a stage) | breakBefore (drop first, then add after stageDelay).
@NiagaraProperty(
  name = "rotationMode",
  type = "BRotationMode",
  defaultValue = "BRotationMode.makeBefore",
  flags = Flags.SUMMARY | Flags.OPERATOR
)
```
MIN **and** MAX are both supplied: `verify-module.sh --src facets-req` FAILs a numeric OPERATOR slot with only one (C8
D5); MIN = 0 is correct because 0 IS the disabled sentinel. No `lint-delays` `facet-min-zero` collision: `getRotationInterval()`
is never a `Clock.schedule*` argument (the adapter's only schedule sites are `powerOnDelay` and the TICK constant).
`[ev: client BCompressorControl.java:380-389,:392-409,:1904,:1972-1973 @ a109249]` `[ev: retro campaign8-facets-lint]` `[ev: retro campaign8-lint-delays]`

### F4 — `CompPan-rt/module-include.xml` — register the enum type
Today it holds ONE type (`:3`). Add: `<type class="com.angeles.CompPan.BRotationMode" name="RotationMode"/>` — and add the
`@NiagaraType` + `module-include.xml` line in the SAME change (a dangling type = live "Missing class", B818/C8 PR14
checklist). `[ev: client module-include.xml:3 @ a109249]` `[ev: corpus B818]`

### F5 — `CompPan-rt/module.lexicon` — the `@Range` tags need lexicon keys (SP6 known set)
Add `rotationInterval=Rotation interval`, `rotationMode=Rotation mode`, `makeBefore=Make before break`,
`breakBefore=Break before make` (the per-slot lexicon lint `slot-coverage.sh per-slot` FAILs a MISSING OPERATOR key).
`[ev: corpus B828 (SP6 lexicon keys for @Range)]` `[ev: retro campaign8-slot-per-slot]`

### F6 — tests: `CompPan-rt/srcTest/test/com/angeles/CompPan/CompressorControlTest.java` (+5) and the golden oracle
The suite has 37 `@Test`s with helpers `cfg()` (`:30`) and `drive(ctl, now, d, c, steps)` (`:57`, uses the 9-arg
`step` that delegates with `AUTO_MODES`, `:120`). RED-first: commit ROT1–ROT5 failing (no rotation fields → compile
fail is the RED), then GREEN with F1–F3.

| Pin | Scenario | Assertion |
|---|---|---|
| ROT1 swap-after-interval | 2 units on under steady demand, `rotationIntervalMs = 3h`, `minOnMs` small; drive past 3 h | the longest-running unit is OFF and the least-hours idle unit ON; `stagesOn` unchanged |
| ROT2 no-swap-below-interval | same, drive to 2 h 59 min | commands byte-identical to the no-rotation run |
| ROT3 no-swap-inside-minOff | the idle candidate's `cmdSince` within `minOffMs` | no swap (gate 9 via `pickLeastHoursOff`) |
| ROT4 make-before-break ordering | arm cycle then next cycles | cycle N: incoming ON + outgoing STILL ON (`stagesOn == target+1`); cycle after `stageDelayMs`: outgoing OFF, `stagesOn == target` — never a cycle with both off |
| **ROT5 disabled-is-byte-identical** | the D1a golden (below) | `assertEquals(golden, actual)` on the joined trace with `rotationIntervalMs = 0` |
Plus a schema pin (§2) and the write-path matrix rows (§3). `[ev: client CompressorControlTest.java:30,:57,:120 @ a109249]` `[ev: design.md D1a]`

---

## 2. Schema-risk expectation — SAFE (additive only)
Two NEW slots (`rotationInterval`, `rotationMode`) + one NEW type; no existing slot retyped or removed; the linked
`condenserNMode` doubles untouched. Run `toolbelt/schema-risk.sh <pre-snapshot> <post-snapshot>` on the CompPan-rt
`@NiagaraProperty` set → expected verdict **SAFE, exit 0** (additive). Pin it in the PR (the C8 schema-risk gate is
mandatory before any slot-touching bump, BUILD-LOOP §4.b/§6). Bump `vendorVersion` (schema changed, even if SAFE).
`[ev: corpus B795]` `[ev: retro campaign8-build-pipeline]`

## 3. Cross-lane obligation — write-path matrix rows (R11)
Both new slots are `Flags.OPERATOR`, so `lint-write-path.sh` will FAIL the module unless `<client-root>/docs/write-path-
matrix.md` gains two rows: `rotationInterval · writer=dashboard/write-server (oBIX <reltime>) · timing=next execute() ·
test=ROT1`; `rotationMode · writer=dashboard (<enum val="makeBefore"/>) · timing=next execute() · test=ROT4`. Dashboard
exposure per the slot-type doctrine: `BRelTime` is written as a bare `<reltime val="PT3H"/>` (a simple, no complex-write
hazard); the frozen enum renders `<enum val="makeBefore" range=…/>` and decodes `<enum val="…"/>` with no range facet.
`[ev: corpus B823 (slot-type doctrine)]` `[ev: corpus B828]` `[ev: retro campaign8-write-path]`

---

## 4. ROT5 — the byte-identical golden (D1a), the method
1. On the RED branch (pre-change `CompressorControl`), write a deterministic ~40-step input trace: `{now, demandCount,
   amps[3], ampsValid[3], suction, suctionValid, discharge, dischargeValid, modes[3]}` — include a demand ramp 0→3→1→2,
   an HOA OFF on one unit, a high-discharge window and a suction dip below the LP floor.
2. Run it through the PRE-change class; emit one canonical line per step: `<idx>:<cmd0><cmd1><cmd2>|<stagesOn>|<demand>|<pressureFallback>`.
3. **Commit the output as `srcTest/resources/rotation-golden.txt`** (text — satisfies the no-binary-fixture rule).
4. Post-change ROT5 replays the SAME trace with `cfg.rotationIntervalMs = 0` and asserts `assertEquals(golden, actual)`
   on the joined string. Rejected: final-state-only (a swap that reverts inside the trace would pass); regenerating the
   golden from the post-change class (circular).
5. Mutation proof (K13): set `rotationIntervalMs = 3h` on the same trace → ROT5 MUST fail (proves the golden sees swaps).
`[ev: design.md D1a]` `[ev: proposal SC-1]`

## 5. Gates before the PR (in order)
`verify-module.sh --src` (facets-req: both MIN+MAX present) → `lint-delays.sh` (no new schedule site) → `slot-coverage.sh
per-slot` (4 new lexicon keys) → `lint-structure.sh` (L2: one `@NiagaraType` per file — `BRotationMode.java` is its own
file) → `lint-write-path.sh` (2 matrix rows) → `schema-risk.sh` (SAFE) → pure `CompressorControlTest` 42/42 → build +
`bog-audit.sh` on the pre-deploy snapshot (S20 adds no links; CHECK14 own-output-unlinked unaffected).
`[ev: BUILD-LOOP §5 pre-gate]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | rotation today only at stage events (least-hours up / most-hours down) | [CERT] | `CompressorControl.java:31-36,:226,:238 @ a109249` |
| 2 | insertion anchors :216/:218 and :233/:235; `cmdSince` :71; hours :158; `available` :173-175 | [CERT] | re-read @ a109249 this session |
| 3 | `resetTransient` :328, `seedRestart` :346, `pickLeastHoursOff` :352 (D1 drift ~60 lines) | [CERT] | grep @ a109249 |
| 4 | no BFrozenEnum exists in CompPan; module-include.xml has 1 type | [CERT] | `git ls-tree`/`module-include.xml:3 @ a109249` |
| 5 | the two blocks + gates + actions | [CERT-design] | design.md D1 (fixed) — verbatim code is my rendering of D1, to be compiled at apply |
| 6 | schema-risk SAFE | [INFER, expected] | additive-only by construction; confirmed by `schema-risk.sh` at apply |
**Tally:** [CERT] ×4 · [CERT-design] ×1 · [INFER] ×1 (the SAFE verdict, pinned at apply).
