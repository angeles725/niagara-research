# C9 S20 — time-slice compressor rotation: apply package (R1, FIRST client PR — CompPan-rt) — rev 2 (RE-CUT)

Author: companero (Fable), 2026-09-06. **Rev 2 re-cut** against the CURRENT design D1 (niagara-tools `d2857d1`, second-read
fixes) and the CURRENT RED `qa/c9-comppan-rotation` **`cf28572`** (`CompPan-rt/srcTest/test/com/angeles/CompPan/CompressorRotationTest.java`,
383 lines, **17 pins ROT1–ROT10 + ROT7b + ROT11–ROT16 + the ROT5 golden**). Rev 1 (`e86726205`) was STALE: it named
`CompressorControlTest (+5)`, a boolean `rotationMakeBeforeBreak`, no `swaps`, a `cmdSince`-based clock, the existing
`pickLeastHoursOff`, and 4f5f1c7 line numbers for the two insertion points — **do NOT apply rev 1**. Every line below was
counted at client **`a109249`** this session (not taken from any design text). `[ev: RED cf28572]` `[ev: design D1 @ d2857d1]`
`[ev: client CompressorControl.java @ a109249]`

> **Lesson recorded:** rev 1 verified the CONTENT of the insertion anchors but copied their LINE NUMBERS from D1 — which
> were 4f5f1c7's. Anchors are counted at the tip, or they are not anchors.

## 0. Dependencies / settled
- The pure-model contract is the RED's, verbatim: `Cfg.rotationIntervalMs` (long, **0 = disabled**), `Cfg.rotationMode`
  (**int**, `CompressorControl.ROTATION_MAKE_BEFORE_BREAK` default / `ROTATION_BREAK_BEFORE_MAKE`), a **package-visible
  `int swaps`** on the controller counting COMPLETED swaps only (`ctl.swaps`). `[ev: RED :19-21, :63-64]`
- Adapter slots: `rotationInterval` (`BRelTime`, SUMMARY|OPERATOR, MIN 0 + MAX facets) and `rotationMode` (NEW frozen enum
  `BRotationMode`, non-linked → B828 §828.7-safe). CompPan has NO frozen enum today → new file + `module-include.xml` line.
- ROT5 golden: with `rotationIntervalMs = 0` the 120-step trace (`GoldenCapture` machinery, RED `:91-111`) must be
  BYTE-IDENTICAL to the a109249 golden. Neither `rotSinceMs` nor `rotArmedMs` nor `swaps` is in the trace line, so the new
  fields cannot break it — only a behaviour change at interval 0 can. `[ev: RED :31, :91-111, :175-181]` `[ev: D1 §arm action]`

## 1. Verified anchors @ a109249 (`CompressorControl.java`)
| What | Line |
|---|---|
| `N = 3` / `MODE_AUTO=0 MODE_ON=1 MODE_OFF=2` | `:53`, `:56-58` |
| transient block (`cmd…belowSince`), `cmdSince[]` | `:66-75`, `:71` |
| `Cfg` fields (`minOnMs`, `minOffMs`, `stageDelayMs`) | `:83-95` (`stageDelayMs :88`) |
| feedback loop; hours integrate on the COMMANDED state | `:145-168`, `:158` |
| `available` computed | `:173-174` |
| target clamp — LP floor `:215`, high-head `:213`, **`if (target > N) target = N;`** | `:212-217` |
| **`// 3) Move ONE stage`** — `stageReady` — stage-up write `cmd[k] = true; cmdSince[k] = now; lastStageMs = now;` — stage-down via `pickMostHoursOn` | **`:219`**, `:221`, **`:229`**, `:238` |
| **stage block closes** / **`// 4) Manual HOA override`** / `cmdPreHoa = cmd.clone()` snapshot / HOA loop / safety envelope | **`:246`** / **`:248`** / `:255` / `:256-271` / `:273-306` |
| `resetTransient()` / `seedRestart(now)` / `pickLeastHoursOff` / `pickMostHoursOn` | `:328` / `:346` / `:352-363` / `:365-376` |
`BCompressorControl.java`: `powerOnDelay` precedent `:380-389`; `condenser1..3Mode` doubles `:392-409` (linked — never retype);
cfg wiring block `:1903-1940` (the `minOn/minOff/stageDelay` trio `:1907-1909`); `ctl.step(...)` call `:1971`; hours persist
`:1975-1977`. `Dashboard`-style group version: `Compresores/build.gradle.kts` `defaultModuleVersion` → **2.1.0** (proposal :115: CompPan-rt 2.0.3 → 2.1.0 in PR1).

## 2. File-level diff plan

### F1 — `CompressorControl.java` (PURE model) — 8 edits
| # | Anchor | Edit |
|---|---|---|
| F1.1 | class constants beside `MODE_*` (`:56-58`) | `static final int ROTATION_MAKE_BEFORE_BREAK = 0, ROTATION_BREAK_BEFORE_MAKE = 1;` |
| F1.2 | `Cfg` (`:83-95`), after `stageDelayMs` (`:88`) | `long rotationIntervalMs; // 0 = time-slice rotation DISABLED` · `int rotationMode = ROTATION_MAKE_BEFORE_BREAK;` |
| F1.3 | transient block (`:66-75`) | `int swaps;` (**package-visible**, completions only) · `private int rotOut = -1;` · `private long rotArmedMs = Long.MIN_VALUE;` · `private final long[] rotSinceMs = new long[N];` (the per-unit ROTATION clock — NOT `cmdSince`, see §3) · `private long lastRotIntervalMs;` (to detect the 0→non-zero enable edge) |
| F1.4 | `resetTransient()` (`:328-`) | additionally clear `rotOut = -1; rotArmedMs = Long.MIN_VALUE; Arrays.fill(rotSinceMs, 0L); swaps = 0; lastRotIntervalMs = 0;` (D1d — a disable→enable must not leave a phantom pending swap) |
| F1.5 | `seedRestart(now)` (`:346-`) | re-seed `rotSinceMs[k] = now` for every unit alongside `cmdSince[k]` (ROT16: after a restart wait a FULL interval) |
| F1.6 | **top of `step()`**, after `modes` is set and before the feedback loop (`:145`) | enable edge: `if (c.rotationIntervalMs > 0 && lastRotIntervalMs == 0) for (k) rotSinceMs[k] = now; lastRotIntervalMs = c.rotationIntervalMs;` (ROT16: enabling mid-run waits a FULL interval; a lead unit already running 5 h must NOT swap on the first step) |
| F1.7 | stage-up write (`:229`) | `cmd[k] = true; cmdSince[k] = now; rotSinceMs[k] = now; lastStageMs = now;` (rule: `rotSinceMs` stamped whenever a unit is commanded ON, by staging OR by arm) |
| F1.8 | **step 2b COMPLETION** inserted between `:217` (`if (target > N) target = N;`) and `:219` (`// 3) Move ONE stage`); **step 3b ARM** inserted between `:246` (stage block `}`) and `:248` (`// 4) Manual HOA override`) — both BEFORE `:255` `cmdPreHoa` so the HOA loop and the safety envelope still run last and still win | blocks below |
| F1.9 | new private helper beside `pickLeastHoursOff` (`:352`) | `pickLeastHoursOffAuto(now, minOffMs)` — same as `pickLeastHoursOff` but skips `if (cmd[k] \|\| modes[k] != MODE_AUTO) continue;` (excludes HAND too — ROT7/ROT7b); `pickLeastHoursOff` itself is UNCHANGED (ROT5) |

**F1.8 — step 2b (COMPLETION / mid-window edges), verbatim — runs BEFORE the stage move so the `onCount == target+1` window is
resolved by dropping `rotOut` EXPLICITLY, never by `pickMostHoursOn` (ROT11, N=3):**
```java
    // 2b) S20 time-slice rotation — resolve a PENDING swap before ordinary staging can shed the wrong unit (ROT11).
    if (rotOut >= 0)
    {
      if (modes[rotOut] != MODE_AUTO)                        // ROT15 (E4): operator took the unit (HAND/OFF) -> skip & clear, no write, no swap
      { rotOut = -1; rotArmedMs = Long.MIN_VALUE; }
      else if (target >= onCount)                            // ROT12 (E1): demand rose -> the extra unit is now wanted -> CANCEL, keep both on
      { rotOut = -1; rotArmedMs = Long.MIN_VALUE; }          //             (swaps NOT incremented — it counts completions only)
      else if ((now - rotArmedMs) >= c.stageDelayMs          // normal completion after one stageDelay
               || target < onCount - 1                       // ROT13 (E2): demand fell -> drop rotOut FIRST, the normal shed takes the next later
               || dischargeHigh)                             // ROT14 (E3): high head -> still drop rotOut; gate 5 then blocks any re-arm
      {
        if (c.rotationMode == ROTATION_MAKE_BEFORE_BREAK)
        { cmd[rotOut] = false; cmdSince[rotOut] = now; }     // BREAK the outgoing (explicit, targets rotOut)
        else
        { int in = pickLeastHoursOffAuto(now, c.minOffMs);   // break-before-make: MAKE the incoming now
          if (in >= 0) { cmd[in] = true; cmdSince[in] = now; rotSinceMs[in] = now; } }
        lastStageMs = now; rotOut = -1; rotArmedMs = Long.MIN_VALUE; swaps++;
      }
    }
```
**F1.8 — step 3b (ARM), verbatim — the 10 gates in D1 order:**
```java
    // 3b) S20 time-slice rotation — ARM a swap. Steady demand only; the same safety expressions as steps 2/3.
    if (c.rotationIntervalMs > 0                                                          // 1 enabled (0 = disabled sentinel)
        && rotOut < 0                                                                     // 2 one swap in flight
        && onCount == target                                                              // 3 steady demand only
        && stageReady                                                                     // 4 same expr as :221
        && !dischargeHigh                                                                 // 5 never add on high head (:213) — also blocks re-arm after ROT14
        && !(suctionValid && c.suctionLowLimit > 0d && suction < c.suctionLowLimit)      // 6 LP floor sheds, never adds (:215) — keep the > 0d term
        && available >= 2)                                                                // 7 (:173-174)
    {
      int out = pickLongestRotOn(now);                                                    // 8 running AUTO unit, largest now - rotSinceMs, tie -> most hours
      if (out >= 0 && (now - rotSinceMs[out]) >= c.rotationIntervalMs                    // 8 interval on the ROTATION clock (ROT16)
                    && (now - cmdSince[out]) >= c.minOnMs)                                //   AND min-on on the command clock
      {
        int in = pickLeastHoursOffAuto(now, c.minOffMs);                                  // 9 min-off + HOA-OFF + HOA-HAND excluded (ROT3/ROT7/ROT7b)
        if (in >= 0 && hours[in] < hours[out])                                            // 10 only if it REDUCES divergence
        {
          if (c.rotationMode == ROTATION_MAKE_BEFORE_BREAK)
          { cmd[in] = true; cmdSince[in] = now; rotSinceMs[in] = now; }                   // MAKE first (default)
          else
          { cmd[out] = false; cmdSince[out] = now; }                                      // BREAK first (alternative)
          lastStageMs = now; rotOut = out; rotArmedMs = now;
        }
      }
    }
```
`pickLongestRotOn(now)`: running (`cmd[k]`) AUTO unit with the largest `now - rotSinceMs[k]`, tie-broken by most `hours[k]`;
-1 if none. `pickMostHoursOn` (`:365-376`) is UNTOUCHED — it knows nothing of `rotOut` by design; the 2b block is what
protects the swap window. `[ev: D1 §"Do not confuse the two shed mechanisms", gates table, ROT11-ROT16 table]`

### F2 — NEW `BRotationMode.java` (frozen enum: `makeBefore(0) | breakBefore(1)`; `@NiagaraType` + `@NiagaraEnum(range={@Range("makeBefore"),@Range("breakBefore")})`; no RANGE facet — B828) + `module-include.xml` `<type class="com.angeles.CompPan.BRotationMode" name="RotationMode"/>` in the SAME change (B818) + lexicon keys `rotationInterval`, `rotationMode`, `makeBefore`, `breakBefore`.

### F3 — `BCompressorControl.java` (adapter)
| # | Anchor | Edit |
|---|---|---|
| F3.1 | after `powerOnDelay` (`:380-389`) | `@NiagaraProperty(name="rotationInterval", type="BRelTime", defaultValue="BRelTime.make(0)", facets=@Facet("BFacets.make(BFacets.MIN, BRelTime.make(0), BFacets.MAX, BRelTime.makeHours(24))"), flags=Flags.SUMMARY\|Flags.OPERATOR)` and `@NiagaraProperty(name="rotationMode", type="BRotationMode", defaultValue="BRotationMode.makeBefore", flags=Flags.SUMMARY\|Flags.OPERATOR)` — MIN+MAX both present (`facets-req`, C8 D5); no `lint-delays` collision (`getRotationInterval()` never feeds `Clock.schedule*`) |
| F3.2 | cfg block `:1903-1940`, beside the `minOn/minOff/stageDelay` trio (`:1907-1909`) | `cfg.rotationIntervalMs = getRotationInterval().getMillis();` · `cfg.rotationMode = getRotationMode().getOrdinal();` (0 = makeBefore) |
| F3.3 | `ctl.step(...)` (`:1971`), hours persist (`:1975-1977`) | UNCHANGED — the two fields ride the existing `Cfg`; `condenserNHours` keep integrating on the COMMANDED state (`:158`) |

### F4 — tests: the RED file lands as-is (`CompressorRotationTest.java` @ cf28572, 17 pins + golden); GREEN = F1–F3. RED-first
on `qa/c9-comppan-rotation`. `CompressorControlTest` (37) must stay green (ROT5 is the byte-identical guard for it).

## 3. The pins and what each one forbids (RED cf28572 verbatim)
| Pin | Forbids |
|---|---|
| ROT1 swap after interval (`swaps` 0 → exactly 1; still 1 unit on at D=1) | dropping the interval trigger |
| ROT2 no swap below interval | premature swap |
| ROT3 candidate inside `minOff` (24 h) → no swap | dropping the candidate min-off guard (gate 9 via `pickLeastHoursOffAuto`) |
| ROT4 make-before-break ordering | two writes in one cycle / break first by default |
| **ROT5 golden** — `rotationIntervalMs = 0` → trace byte-identical to the a109249 golden, `swaps == 0` | ANY behaviour change at interval 0 |
| ROT6 one available (B, C `MODE_OFF`) → no swap | gate 7 |
| ROT7 B `MODE_OFF`, C `MODE_ON` → "no auto candidate → no swap" | touching HAND / picking OFF |
| **ROT7b** C is HAND, OFF and PAST minOff with the least hours → NEVER the incoming pick | reusing `pickLeastHoursOff` (it skips only `MODE_OFF`) |
| ROT8 `dischargeHigh` → no swap | gate 5 |
| ROT9 LP floor → no swap | gate 6 (with the `> 0d` term) |
| ROT10 hours ledger unaffected by a swap | rewriting accrued hours |
| **ROT11 (N=3)** the swap window sheds `rotOut`, NOT the most-hours unit | delegating the drop to `pickMostHoursOn` |
| ROT12 (E1) demand rises mid-window → cancel, both stay on, `swaps == 0` | counting a cancelled swap |
| ROT13 (E2) demand falls mid-window → drop `rotOut` first, then the normal shed; `onCount` reaches demand | shedding the wrong unit first |
| ROT14 (E3) `dischargeHigh` mid-window → still drop `rotOut`; `swaps` stays 1 (no re-arm while high) | re-arming under high head |
| ROT15 (E4) `rotOut` → HAND mid-window → skipped and cleared, `swaps == 0`, no dangling arm fires later | writing a HAND unit |
| **ROT16** enable (0 → interval) or restart → no swap on the first step, none for a FULL interval, then a swap | a `cmdSince`-based clock |

## 4. Schema-risk / gates / version
Two NEW slots + one NEW type, no retype/remove (the linked `condenserNMode` doubles untouched) → `schema-risk.sh` **SAFE**;
CompPan-rt `vendorVersion` **2.0.3 → 2.1.0** (proposal :115 / SC-13). Gate order: `verify-module.sh --src` (facets-req) →
`lint-delays.sh` → `slot-coverage.sh per-slot` (4 lexicon keys) → `lint-structure.sh` (L2: `BRotationMode.java` own file) →
`lint-write-path.sh` (2 matrix rows — R11 cross-lane: `rotationInterval`, `rotationMode`) → `schema-risk.sh` → pure tests
(37 + 17 + golden) → build. Dashboard exposure per the slot-type doctrine: `BRelTime` → bare `<reltime val="PT3H"/>`;
the frozen enum decodes `<enum val="makeBefore"/>` with no range facet (B828).

## 5. FASE 1/2/3 — the client explainer (unchanged from rev 1, still accurate)
FASE 1 (shipped) demand-count staging + amp proof-of-run + run-hours lead/lag rotation at stage events; FASE 2 the
suction-pressure modulator; FASE 3 floating suction. S20 adds a TIME-SLICE to FASE 1's rotation: after `rotationInterval`
of continuous run a unit swaps for the idle least-hours AUTO unit, make-before-break, never touching HAND/OFF or a safety;
`rotationInterval = 0` = today, byte-identical (ROT5). `[ev: CompressorControl.java:28-47 @ a109249]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | insertion anchors :217/:219 and :246/:248, both before :255; stage-up write :229; :328/:346/:352/:365 | [CERT] | counted @ a109249 this session (grep) |
| 2 | RED contract: `rotationMode` int + constants, `swaps`, ROT1-16 + 7b + golden | [CERT] | `CompressorRotationTest.java:19-21,:63-64,:126-373` @ cf28572 |
| 3 | `rotSinceMs[]` clock, enable-edge re-seed, `pickLeastHoursOffAuto`, explicit `rotOut` drop, E1-E4 rules | [CERT-design] | D1 @ d2857d1 (gates table, ROT11-16 table, D1d) |
| 4 | new fields absent from the trace line → ROT5 holds | [CERT] | RED golden machinery :91-111 + D1 arm-action note |
| 5 | rev 1's four contract gaps (boolean mode, no swaps, cmdSince clock, old picker) and its 4f5f1c7 anchors | [CERT] | investigador1 2nd read; e86726205 diff |
| 6 | schema-risk SAFE | [INFER, expected] | additive by construction; pinned at apply |
