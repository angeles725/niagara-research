# C10 design validator read — design.md e359fd8 (+ specs 2ff4a6e) — 3 mechanism claims verified with OBSERVED runs

investigador1, 2026-09-07. Read-only; the design executor had no shell, so the three root-cause claims are verified here
against source at client **ff1b659** + reproduced runs. `[ev: git + reproduced lint runs]`

## Verdict: PASS to launch tasks — claims 1 & 2 CONFIRMED, claim 3 fix CONFIRMED but its stated mechanism PARTIALLY REFUTED.

## Claim 1 — S21 root cause (lint-timers.sh:147 loose candidate regex) — CONFIRMED
`lint-timers.sh:147` `match(line, /[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*\(/)` extracts any `identifier(` as a
method-signature candidate; `@NiagaraProperty(` matches (yields `NiagaraProperty`, not a keyword), so the forward
brace-walk crosses the annotation/field region and mispairs the method-LOCAL `anyNoHardware` (`BDefrostController.java:718`)
with the `:808/:810/:850` schedules. **REPRODUCED** on `main-ff1b659`: `bash lint-timers.sh …/ColdRoomPan-rt/src` →
`FAIL  companion-flag  …/ColdRoomPan-rt/…` (the anyNoHardware companion-flag FP). The fix (a real method-signature guard:
class FIELD + same-enclosing-method schedule) addresses it. `[ev: lint-timers.sh:147 @ cb79676; reproduced @ ff1b659]`

## Claim 2 — Case-B parser needs `brace_depth >= 2` guard — CONFIRMED (fix safe for C9 SP)
`lint-silent-protection.sh:250-306` (section D) opens a "method" on `brace_depth > old_depth` (ANY increase), with a
backward scan (Case B) for `identifier(` before a lone `{`, excluding only `class/interface/enum` KEYWORDS. At
`BCompressorControl.java` the class body `{` (`:449`, Case B) is preceded by `defaultValue = "new BAlarmRecord()"` (`:442`)
→ the backward scan matches `BAlarmRecord(` and names the CLASS BODY a method (the keyword-exclusion doesn't catch it — it
checks the identifier, not that the line is a class decl). A method opens 1→2 in brace depth; the class body opens 0→1, so
`brace_depth >= 2` (new depth) is the correct guard. **C9 SP pins do NOT move**: current `lint-silent-protection` on the
ff1b659 CompPan-rt = **1 WARN** (the legit CP-1 shed, which S23/PR3 will fix), NOT a misparse artifact — the misparse is
latent (mis-names the method but flips no pin today). The PR should still OBSERVED-verify the SP bats stay green after the
guard. `[ev: lint-silent-protection.sh:250-306 @ cb79676; BCompressorControl.java:442,:449 @ ff1b659; SP run = 1 WARN]`

## Claim 3 — "S24 needs two edits or a relative `.` breaks `-sourcepath` at :59" — FIX CONFIRMED, MECHANISM PARTIALLY REFUTED
The S24 fix (cwd-independence for structural tests that read `src/…` via `Paths.get`) is real and needed. The design's D4a
chooses **a subshell around the JAVA call only** (`:186` `( cd "$rt" && java … )`) plus `rt=$(cd "$rt" && pwd)` after `:30`.
But **`javac` (`:58-60`, `-sourcepath "$rt/src:$testroot"`) runs OUTSIDE that subshell**, so the cd does not touch it, and I
**empirically confirmed the CURRENT script with a relative `$rt` from a foreign cwd works**: from `/tmp`,
`run-pure-test.sh ../../…/CompPan-rt com.angeles.CompPan.CompressorControlTest` → **OK (37 tests)**. So `-sourcepath` is
NOT broken today and is NOT broken by a java-only subshell. The design's stated failure mode (`:192` "once anything cds,
-sourcepath at :59 breaks") is inaccurate for its OWN chosen approach — it would only be true if the cd wrapped the `javac`
too. The `rt=$(cd && pwd)` absolutize edit is still worth keeping (robust `$testroot`/`$tmp`/future-proof against cd
placement), but it is DEFENSIVE, not the `-sourcepath` fix the design claims. Recommend: keep both edits; correct the
design's rationale so it doesn't assert a break that the java-only subshell avoids. `[ev: run-pure-test.sh:30,:58-62 @ cb79676; reproduced OK(37) with relative rt from /tmp]`

## The rest of the validator checklist — PASS
- **Spec (2ff4a6e)**: 5 RED tips on their FINAL commits (S21 52ebd11, S22 954ebd7, S23 f981754, S24 a792d7a, S25 **a56a72e**);
  S22 scenarios cover the doAckAlarm neg-pin (R-S22.4 + scenario writing a DIFFERENT slot) and the execute()/changed()/
  generated-setter exclusion (R-S22.3); S25 carries per-row / `[concept]` / action-covered / summary-covered / real-tree
  (DashboardPan @ ff1b659 = 5 STALE before S26 → 0 after); S23 states the `B<Pure>` convention as a LIMITATION (R-S23.3);
  the two `[ev:]` empties are PROSE (backtick-quoting the convention in cross-cutting.md:39/:41), not leaks.
- **OBSERVED-flip per gate**: present — the PR matrix (`design:365-370`) has an OBSERVED-flip column per PR; "would flip"
  prose is rejected (SC-7); a smoke that cannot run is a BLOCKER, not advisory. ✓
- **STALE grammar STATUS-first**: `STALE  lint-write-path  <matrix>:<line>  slot <name>: no source slot with that name`
  (`design:241`). ✓
- **Client cites at ff1b659**: all verified — BDefrostController :148/:713, BCompressorControl :375/:381/:442/:447/:448/:2025,
  BEvaporatorUnit :193, CompressorControl :294. ✓

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | S21 regex matches @NiagaraProperty(; FAIL reproduced | [CERT] | lint-timers.sh:147; run @ ff1b659 |
| 2 | Case-B misparse at :449 via defaultValue :442; needs brace_depth>=2; SP pins don't move (1 legit WARN) | [CERT] | :250-306; :442/:449; SP run |
| 3 | -sourcepath NOT broken today (relative rt from /tmp = OK 37); design's java-only subshell doesn't touch javac | [CERT] | reproduced run; :58-62/:186 |
| 4 | spec RED tips final; S22/S23/S25 scenarios; OBSERVED gate; STALE grammar; anchors | [CERT] | spec + design greps |
Tally: 4 [CERT] · 0 [INFER] · 0 unmarked.
