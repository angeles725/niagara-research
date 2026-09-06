# S20 time-slice rotation — design (D1) second read at the re-anchored client tip a109249

Author: investigador1, 2026-09-06. Source read: `Compresores/CompPan/CompPan-rt/src/com/angeles/CompPan/CompressorControl.java`
(pure model) and `BCompressorControl.java` (adapter) at **a109249** (`niagara-panccadia-leon` origin/main), read at the
enclosing method, not grep-only. Scope: (1) tree anchoring of D1's cites, (2) insertion points, (3) the ordering rationale,
(4) `cmdSince[]` as the per-compressor clock, (5) make-before-break vs `stageDelay/minOn/minOff`, (6) the "0 = disabled
byte-identical" proof strategy. Verdict first, evidence after.

**Verdict:** D1's **ordering is correct** (rotation COMPLETION before the stage move, ARM after it). D1's **stated reason is
imprecise** — the incoming unit is not the thing at risk; the risk is that the stage move sheds the **wrong running unit**.
D1's **line cites are from the wrong tree** (`4f5f1c7`, the pre-fix clone) and must be re-anchored to a109249 before apply.
Four edge cases the arm/complete state machine must handle are missing from D1 (§5). The golden strategy is sound; strengthen
the trace format (§6).

---

## 1. Tree anchoring — D1 was read at 4f5f1c7, not a109249 `[CERT]`

Every D1 line number matches the OLD pre-fix tree exactly and none matches the re-anchored tip:

| Symbol | D1 cites | `4f5f1c7` (second local clone) | **`a109249`** (chain tip) |
|---|---|---|---|
| target clamp | `:212-216` | `:212` … `:216` | `:212` … **`:217`** |
| `stageReady =` | — | `:220` | **`:221`** |
| `pickLeastHoursOff(` call (stage-up) | — | `:225` | **`:226`** (matches the proposal) |
| `pickMostHoursOn(` call (stage-down) | `:230` | `:230` | **`:238`** (matches the proposal) |
| stage-move closing brace | `:233` | `:233` | **`:246`** |
| `// 4) Manual HOA override` | — | `:235` | **`:248`** |
| `pickLeastHoursOff` def | — | `:291` | **`:352`** |
| `pickMostHoursOn` def | `:304-314` | `:304` | **`:365-372`** |
| "MODE_ON re-forced ON at" | `:241` | (HAND clause) | **`:264-269`** — at a109249 `:241` is the stage-DOWN write `cmd[k]=false` |

The same drift hits `BEvaporatorUnit.java`: the design's `freezeTripped :1173 / valveInhibited :1047-1052` are `4f5f1c7`'s
numbers (proven across 12 tips: `f89e44e`/`4f5f1c7` → 1173/1047; `a109249` → **1287/1102**). **B824's `:1287,:1106` are
correct at a109249; the design, not B824, needs re-anchoring.** `[ev: git show 4f5f1c7 vs a109249, this session]`

**Why it matters beyond hygiene:** at a109249 the `:304-314` D1 points at is the **LP-floor HAND envelope** (`:300-305`:
`if (lpFloor && modes[k] == MODE_ON) { cmdSince[k] = now; cmd[k] = false; }`), a different shed mechanism from
`pickMostHoursOn`. An apply worker following D1's numbers literally would reason about the wrong code.

## 2. Insertion points at a109249 `[CERT]`

```
:212  target = Math.min(target, available);
:213  if (dischargeHigh) target = Math.min(target, onCount);
:215  if (suctionValid && c.suctionLowLimit > 0d && suction < c.suctionLowLimit) target = Math.min(target, onCount - 1);
:216  if (target < 0) target = 0;
:217  if (target > N) target = N;
        <<< step 2b — rotation COMPLETION goes here >>>
:219  // 3) Move ONE stage toward target, respecting stage-delay + min-on/min-off.
:221  boolean stageReady = (lastStageMs == Long.MIN_VALUE) || (now - lastStageMs) >= c.stageDelayMs;
:222  if (stageReady) { … :226 pickLeastHoursOff … :229 cmd[k]=true; cmdSince[k]=now; lastStageMs=now; …
:236    else if (onCount > target) { :238 pickMostHoursOn … :241 cmd[k]=false; cmdSince[k]=now; lastStageMs=now; … }
:246  }
        <<< step 3b — rotation ARM goes here >>>
:248  // 4) Manual HOA override …
:255  boolean[] cmdPreHoa = cmd.clone();
```

Both insertions sit AFTER the target is final (`:217`) and BEFORE the HOA loop snapshots `cmdPreHoa` (`:255`), so HOA
OFF/HAND still win over any rotation write, exactly as they win over staging. `[ev: client :212-255]`

## 3. Second read of the ordering rationale

**D1 says:** completion must precede the stage move because during the swap window `onCount == target + 1` and
`pickMostHoursOn` "would shed by hours and could drop the incoming unit."

**What the source says `[CERT]`:**
- `pickMostHoursOn(now, minOnMs)` (`:365-372`) skips any unit with `(now - cmdSince[k]) < minOnMs` (`:371`) and among the
  rest picks the LARGEST cumulative `hours[k]` (`:372`).
- The incoming unit was just armed (`cmdSince[in] = now`) and was chosen as the **least-hours** idle unit. So it is protected
  **twice**: by `minOn` (default `minOn` = 3 min > default `stageDelay` = 60 s, `BCompressorControl.java:774/:820`), and by
  being the least-hours unit while `pickMostHoursOn` selects the MOST-hours one. Dropping the incoming unit is not the
  realistic failure.
- **The real failure:** `pickMostHoursOn` selects by **lifetime `hours[]`**, not by the pending `rotOut`. In an N=3 rack at
  steady `target = 2`, after arming there are three running units; the one with the most lifetime hours may be the **third**
  unit, not the outgoing one (the outgoing was chosen by longest *continuous* run, D1 :56-57). The stage move would then shed
  the third unit: the outgoing keeps running, the rotation is corrupted, and `rotOut` dangles. In an N=2 rack the only other
  running unit IS the outgoing, so the bug is invisible in a two-compressor fixture — **the ROT pins must include an N=3 case.**
- Secondary: an operator can set `stageDelay ≥ minOn` (both are `OPERATOR` slots), removing the `minOn` shield; the
  least-hours property still protects the incoming unit, but the third-unit hazard above is unaffected either way.

**Conclusion:** completion-before-stage-move is the right design — an explicit step that drops exactly `rotOut` after
`stageDelay`, so the stage move then sees `onCount == target` and does nothing. D1's ordering stands; its rationale should be
rewritten to the third-unit hazard, and ROT1/ROT4 need an N=3 fixture where the third unit has the most hours.
`[ev: client :365-372, :229, :241]` `[ev: design D1 :49-58]`

## 4. `cmdSince[]` as the per-compressor clock `[CERT]`

`private final long[] cmdSince` — "ms of last command change (min-on/min-off)" (`:71`). Every command write stamps it:
stage-up `:229`, stage-down `:241`, HOA-OFF true→false edge `:261`, HAND start `:269`, LP-floor HAND shed `:305`. So
`now - cmdSince[k]` is the continuous ON time when `cmd[k]`, the continuous OFF time otherwise — D1's "no new state field"
claim holds. Two lifecycle writes the design must account for:

| Site | Effect on the rotation clock | Consequence |
|---|---|---|
| `seedRestart(now)` `:348` — sets ALL `cmdSince[k] = now`; called from the adapter at first steady state (`BCompressorControl.java:1756`) | continuous-run clock restarts at 0 after every station restart | rotation waits a full `rotationInterval` after a restart — conservative and acceptable; **document it** in the slot comment |
| `resetTransient()` `:330` — sets `cmdSince[k] = 0L` (not `now`); called on stop | if a step ran before `seedRestart`, `now - 0L` is huge → gate 8 satisfied instantly | verify the disable→enable path calls `seedRestart` before the first `step` (B821-era finding: `resetTransient` on stop, `seedRestart` on start); add a ROT pin "no swap on the first step after re-enable" |

The rotation's own pending state (`rotOut`, `rotArmedMs`) must be cleared in `resetTransient` too, or a disable→enable leaves a
phantom pending swap (D1 :83-85 already says so — correct). `[ev: client :71, :229, :241, :261, :269, :305, :330, :348]`

## 5. Make-before-break against `stageDelay` / `minOn` / `minOff` `[CERT]` + `[INFER]` edge cases

Timeline with D1's two writes:
1. **Arm** (3b, cycle t0): `cmd[in]=true; cmdSince[in]=t0; lastStageMs=t0; rotOut=out`. `onCount = target+1`.
2. Cycles t0 < t < t0+stageDelay: `stageReady` false (`:221`) → stage move idle; completion (2b) waits (D1 gate: stageDelay elapsed).
3. **Complete** (2b, t ≥ t0+stageDelay): `cmd[rotOut]=false; cmdSince[rotOut]=t; lastStageMs=t; rotOut=-1`. `onCount = target`.
4. Stage move sees `onCount == target` → nothing. Sequence = incoming ON → `stageDelay` → outgoing OFF (ROT4). ✔

Guards already satisfied by construction: the incoming passed `minOff` at pick time (same test as `:358`); the outgoing passed
`minOn` by gate 8 (`≥ max(rotationIntervalMs, minOnMs)`). Both units accrue `hours` during the window (`:158` integrates real
`cmd` time) — that is genuine run time, so ROT10 holds without special handling.

**Edge cases D1 does not state — each needs a pin `[INFER]`:**
| # | Event inside the swap window | Required behaviour | Why |
|---|---|---|---|
| E1 | demand RISES so `target == onCount` (the extra unit is now wanted) | completion **cancels** (`rotOut=-1`, keep both ON) instead of dropping | otherwise a needless stage-down followed by a stage-up |
| E2 | demand FALLS (`target < onCount-1`) | completion drops `rotOut`; the stage move sheds further via `pickMostHoursOn` as today | safe direction, no special case |
| E3 | `dischargeHigh` or LP floor asserts mid-window | completion still drops `rotOut` (shedding is the safe direction); arm must never fire (gates 9/10) | matches `:213/:215` cap semantics |
| E4 | `rotOut`'s HOA flips to HAND or OFF mid-window | completion **skips the write** and clears `rotOut` | ROT7 HAND untouched; OFF is handled by the HOA loop `:258-262` anyway |

## 6. The "0 = disabled byte-identical" proof (D1a) `[CERT]` + recommendation

With `c.rotationIntervalMs == 0`, gate 1 fails on every cycle, so 2b/3b never write `cmd[]`, `cmdSince[]`, `lastStageMs`,
`rotOut` or `rotArmedMs` — the inserted code is a pure no-op and the pre-change trace is reproduced exactly. D1a's strategy
(RED commits a fixed input trace + a **committed oracle** produced by the pre-change binary; test runs the same trace with
`rotationIntervalMs = 0` and `assertEquals(golden, actual)` on the joined string) is the right proof; asserting final state
only would miss a swap that reverts inside the trace (D1a already rejects that). Strengthen it:
- Trace line per step = `now|cmd[0..N-1]|stagesOn|lastStageMs` — including `lastStageMs` makes a timing-only regression visible.
- Generate the oracle from **a109249's `CompressorControl` before PR1 touches it** (the RED branch commits it; PR1 must not regenerate it).
- The input trace must exercise stage-up, hold, stage-down, HOA OFF and HAND, LP floor, `dischargeHigh`, and a restart
  (`seedRestart`) — the same events the ROT pins use, so one trace serves both SC-1 and the mutation flips.
`[ev: design D1a]` `[ev: proposal SC-1]`

## 7. Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | D1's cites match 4f5f1c7 and not a109249 | [CERT] | §1 table, `git show` both tips |
| 2 | Insertion points 2b after `:217`, 3b between `:246` and `:248` | [CERT] | §2 listing at a109249 |
| 3 | `pickMostHoursOn` skips `< minOn` and picks max `hours[]` | [CERT] | `:365-372` |
| 4 | Default `minOn` 3 min > default `stageDelay` 60 s | [CERT] | `BCompressorControl.java:774`, `:820` |
| 5 | Third-unit hazard in N=3 at target=2 | [INFER — derived from :372 + D1 :56-57] | needs the N=3 ROT fixture to become [CERT] |
| 6 | `cmdSince` stamped at :229/:241/:261/:269/:305; `seedRestart` :348 → now; `resetTransient` :330 → 0L | [CERT] | grep + read at a109249 |
| 7 | `seedRestart` called at `BCompressorControl.java:1756` | [CERT] | adapter grep |
| 8 | E1-E4 edge cases | [INFER] | not in D1; proposed pins |
Tally: 6 [CERT] · 2 [INFER] · 0 unmarked.
