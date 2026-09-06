# C9 S18-lint — `lint-silent-protection.sh` (B824): apply package for PR3 / R3

Author: companero (Fable), 2026-09-06. Contract extracted from the QA RED `qa/c9-silent-protection` **`e38e503`**
(`tests/lint-silent-protection.bats`, SP1–SP8 + SP-smoke); rule + false-positive controls from B824 §824.2–§824.4;
skeleton = `lint-delays.sh` (src-dir shape). Real-tree anchors re-read at client `a109249` (one B824 line drift
corrected below). Script not on niagara-tools main today (RED state confirmed). No name conflict: RED, proposal (:35,
:76, :158) and B824 all say `toolbelt/lint-silent-protection.sh`. `[ev: qa/c9-silent-protection e38e503]` `[ev: corpus B824]`

## 1. The contract (verbatim from the RED)
- **CLI:** `lint-silent-protection.sh [--strict] <java-src-dir>` — one src dir, recursive; SP-smoke copies THREE real
  files into a FLAT dir (`CompressorControl.java` + `BCompressorControl.java` + `BEvaporatorUnit.java`), so the cross-file
  follow (§3 control b) must work by filename within the SAME dir, with no package-path assumption.
- **Row (B824 §824.2 grammar; SP1 asserts the tokens `lint-silent-protection`, the file name, and the phrase
  `no status/reason/alarm surface`):**
  `WARN  lint-silent-protection  <file>:<line>  <method> forces <output>/sheds stage on <cond> — no status/reason/alarm surface in scope; add a *Alarm/*Reason SUMMARY slot or a BAlarmSourceExt`
- **Exits:** `0` no WARN or WARN without `--strict` · `1` any WARN under `--strict` (SP5) · `3` usage (SP6). WARN-only,
  never hard FAIL (B824 §824.3).
- **D9b prune** (SP7, output must not contain `Stale`). Fixtures ALL INLINE in the bats.
- **Exactly-one-WARN pins:** SP1, SP3, SP8 assert `grep -c WARN == 1` — the scanner must emit ONE row per trip site, not
  one per matching line/regex (dedupe on `<file>:<line>`).

## 2. Pin-by-pin
| Pin | Fixture (inline) | Expect | It pins |
|---|---|---|---|
| SP1 | pure `CompressorControl.step(int target, int onCount, double suction, double suctionLowLimit, boolean suctionValid)` with `if (suctionValid && suction < suctionLowLimit) target = Math.min(target, onCount - 1);` — no field, no slot | exit 0; exactly 1 WARN; output has `lint-silent-protection` + `CompressorControl.java` + `no status/reason/alarm surface` | the inline silent shed (CP-1 shape) + row grammar |
| SP2 | pure class with `boolean dischargeHigh` and `if (discharge > dischargeLimit) { dischargeHigh = true; target = 0; }` **+ a second file** `BCompressorControl extends BComponent` declaring `@NiagaraProperty(name="dischargeHighAlarm", type="BStatusBoolean", …, flags=Flags.TRANSIENT|Flags.SUMMARY|Flags.READONLY)` and `sync(ctl){ getDischargeHighAlarm().setValue(ctl.dischargeHigh); }` | exit 0; NO WARN | control (b): named field → adapter → allowlisted SUMMARY slot = surfaced (CP-2 shape). CROSS-FILE. |
| SP3 | one adapter file `BEvaporatorUnit` with `@NiagaraProperty(name="valveOut", …, flags=Flags.SUMMARY\|Flags.READONLY)`; `recompute(){ boolean freezeTripped = coilValid && coil < freezeSp; if (freezeTripped) getValveOut().setValue(false); }` | exit 0; exactly 1 WARN; output contains `valveOut` OR `freezeTripped` | control (a): writing the trip's OWN forced output (`valveOut`, not allowlist-named) is an EFFECT, not a surface |
| SP4 | `BDefrostController` with `@NiagaraProperty defrostSkipped` (SUMMARY) + `lastSkipReason` (SUMMARY); `maybeSkip(){ if (doorOpen) { getDefrostSkipped().setValue(true); setLastSkipReason("door open"); return; } }` | exit 0; NO WARN | control (c): allowlist `*Skip*` / `*Reason` SUMMARY slots WRITTEN on the trip path = surfaced |
| SP5 | SP1's shed | plain exit 0; `--strict` exit 1 | promotion |
| SP6 | no arg | exit 3 | usage |
| SP7 | `Clean.java` (noop) + `.deploy-baseline/…/Stale.java` (SP1's shed) | exit 0; no WARN; no `Stale` | D9b |
| SP8 (NAMED MUTATION §824.5) | SP4's trip but `this.defrostSkipped = true;` on a **`private boolean defrostSkipped`** field, NO `@NiagaraProperty`, no slot write | exit 0; exactly 1 WARN | a PRIVATE FIELD — even allowlist-NAMED — is never a surface; only a written `@NiagaraProperty` SUMMARY/OPERATOR slot is |
| SP-smoke | flat dir of the 3 real files (skip if absent) | output contains `CompressorControl.java` (CP-1) AND `BEvaporatorUnit.java` (CR-3) AND NOT `dischargeHighAlarm` (CP-2 must not be a WARN subject) | the real-tree discrimination |

## 3. The detection algorithm (B824 §824.2 two-part scan + §824.4 controls → implementable)
Pass 0 — **index the dir** (all `*.java`, pruned):
- `SURF_SLOTS` = every `@NiagaraProperty(name="X", … flags=…)` (paren-balanced, the C8 multi-line join) whose `X`
  matches the allowlist `{*Alarm, *Fault, *Skip*, *Reason, *Status, *Mismatch, *Stuck, *Available, *Fallback}`
  (case-insensitive glob on the NAME) **and** whose flags contain `SUMMARY` or `OPERATOR`.
- `SURF_WRITES` = every `get<X>().setValue(<expr>)` / `set<X>(<expr>)` with `<X>` ∈ `SURF_SLOTS` (lower-camel-matched),
  recording the identifiers referenced in `<expr>` (e.g. `ctl.dischargeHigh` → `dischargeHigh`) and the enclosing method.
- `EFFECT_SLOTS` = every `@NiagaraProperty` whose name does NOT match the allowlist (e.g. `valveOut`, `evapOut`,
  `condenserN`) — writes to these are effects (control a).
Pass 1 — **find TRIPS** per method body (brace-balanced): a statement inside an `if (<cond>)` where `<cond>` compares a
process variable / mode / timer, and the statement is one of: `target = Math.min(target, …)` · `target = 0` /
`target--`/`target - 1` · `cmd[k] = false` · `continue` (inside a pick loop) · `get<Out>().setValue(false)` /
`set<Out>(…false)` / `setBool(<out>, false)` with `<Out>` ∈ `EFFECT_SLOTS` · a `return … || <trip-field>` in a
`*Inhibited()`/`*Trip()` method. Record `<file>:<line>`, method, the forced output (or "stage"), `<cond>`.
Pass 2 — **is the trip SURFACED?** (any one → clean):
  (i) the SAME method body contains a `SURF_WRITES` call (SP4); or
  (ii) the trip's guarded block assigns a NAMED FIELD (`this.F = …` / `F = …` where `F` is a class field, or the `<cond>`
      itself is stored into a field like `this.dischargeHigh = …`) AND some `SURF_WRITES` expression references `F`
      (one-level field→slot follow, cross-file within the dir — SP2, CP-2); or
  (iii) the file/dir declares a `BAlarmSourceExt` on the point or raises a `BAlarmRecord`.
  Writes to `EFFECT_SLOTS` never count (SP3); a `private` field with no `SURF_WRITES` referencing it never counts (SP8).
Pass 3 — emit ONE row per unsurfaced trip (dedupe `<file>:<line>`), `FAILED=1` only under `--strict`.
`[ev: corpus B824 §824.2, §824.4 (a)(b)(c)]` `[ev: bats SP2/SP3/SP4/SP8]`

## 4. Skeleton — as the S7 package §4 (`lint-delays.sh` shape), name `lint-silent-protection.sh`, row/exit per §1.
The only structural difference from S7: Pass 0 needs a DIR-WIDE index before the per-file loop (the cross-file follow),
so build `SURF_SLOTS`/`SURF_WRITES` into `$_TMP` first, then loop files. `shellcheck` 0, VCS-free.

## 5. Real-tree smoke — the RED pin AND the PR3 acceptance, with the anchors VERIFIED at `a109249`
| Subject | Kind | Anchor @ a109249 | Expected |
|---|---|---|---|
| **CP-1 low-suction shed** | FLAG | `CompressorControl.java:215` — inline `if (suctionValid && c.suctionLowLimit > 0d && suction < c.suctionLowLimit) target = Math.min(target, onCount - 1);` — no named field; **`suctionLowAlarm` is ABSENT from the whole module** (grep = 0) | **1 WARN**, subject `CompressorControl.java:215 step` |
| **CP-2 high-discharge** | CLEAN | named field `boolean dischargeHigh` (`:82`), set `this.dischargeHigh = …` (`:140`); adapter surface `getDischargeHighAlarm().setValue(ctl.dischargeHigh)` at **`BCompressorControl.java:1994`** (B824 cites :1539 — DRIFT; the property decl `name = "dischargeHighAlarm"` is `:361`) | **0 WARN** whose subject mentions `dischargeHigh`/`dischargeHighAlarm` (absence pin) |
| **CR-3 freeze trip** | FLAG | `BEvaporatorUnit.java:1287` `private boolean freezeTripped = false;` → consumed by `valveInhibited()` `:1102-1106` (`return resistHand \|\| freezeTripped;`) forcing `valveOut` OFF; no status/reason slot, no alarm | **1 WARN**, subject `BEvaporatorUnit.java` (`valveInhibited`/`freezeTripped`) |
| **defrostSkipped / lastSkipReason** | CLEAN | `BDefrostController.java:746` `getDefrostSkipped().setValue(true);` + `:747` `setLastSkipReason(reason);` — both `@NiagaraProperty` SUMMARY (`:130`, `:140`), names match `*Skip*`/`*Reason` | **0 WARN** on `maybeSkip`/`defrostSkipped` (absence pin) |
**PR3 acceptance = count + subject + absence on all four client module roots** (lesson 11): record the exact WARN count
per root; the two FLAG subjects above MUST appear; the two CLEAN subjects MUST NOT; other rows on DashboardPan-rt/-ux
are measured at GREEN, not predicted. `[ev: client @ a109249 lines above]` `[ev: corpus B824 §824.4]` `[ev: proposal PR3 row :100]`

## 6. K19 routing + retro (same PR)
- `BUILD-LOOP.md §5` pre-gate, beside `lint-wb-threading.sh`: ``` `toolbelt/lint-silent-protection.sh [--strict] <src>` (silent-protection: a trip that forces an output OFF / sheds a stage with NO status/reason/alarm surface in scope → WARN; effect-slot exemption, one-level field→slot follow, *Alarm/*Fault/*Skip*/*Reason/*Status allowlist; exit 0 WARN-only / 1 under --strict / 3 usage) [ev: retro campaign9-silent-protection] ```
- `skill/SKILL.md` toolbelt list: same. `kit-links.bats` resolves the name. Retro slug `campaign9-silent-protection`;
  record the OBSERVED SP8 flip + the §5 four-root table. `report-module.sh` member row lands with the R-row for all three
  C9 lints (proposal §3 "kit-module-report"). `[ev: retro campaign8-retro-loop]` `[ev: proposal §3]`

## 7. Correction input for B824 (one line)
`§824.4 (b)`: the CP-2 adapter write is `BCompressorControl.java:1994` (not :1539) at `a109249`; `:361` is the property.
Everything else in §824.4 (CP-1 :215, CR-3 :1287/:1106, defrost :746-747) is exact.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | CLI/row/exits/exactly-one-WARN/D9b as pinned | [CERT] | bats SP1-SP8 |
| 2 | cross-file follow required (SP2 two files; smoke flat dir of 3) | [CERT] | bats SP2, SP-smoke |
| 3 | private allowlist-named field is NOT a surface (SP8) | [CERT] | bats SP8 vs SP4 |
| 4 | real anchors CP-1 :215 / CP-2 :82,:140,:1994(:361) / CR-3 :1287,:1102-1106 / defrost :746-747,:130,:140; suctionLowAlarm absent | [CERT] | grep @ a109249 this session |
| 5 | four-root counts beyond the four anchors | [INFER] | measure at GREEN |
