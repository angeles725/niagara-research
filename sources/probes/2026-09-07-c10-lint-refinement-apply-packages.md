# C10 lint-precision cluster — apply-packages for S21 / S22 / S23 (kit)

Author: companero (Fable), 2026-09-06. Three kit lint refinements, one theme: **coarse class-level / whole-file heuristics
→ per-slot / per-method-body / cross-adapter matching.** Cited against the C9 kit lints at their REAL merged code (kit
archived `df8c7ec`, v0.20.0). Current four-root behaviour VERIFIED at client `ff1b659` (post-PR1/6/8/9) + a109249. Each is
WARN/FAIL-preserving for the true defect and only removes the false positive/negative. `[ev: kit lint scripts @ df8c7ec]`
`[ev: lint runs @ ff1b659 / a109249, 2026-09-06]` `[ev: niagara-tools #89]`

## S21 — `lint-timers.sh` companion-flag: require a class FIELD + a correctly-scoped same-method schedule
**Defect (VERIFIED still FAILing now):** `lint-timers.sh` on ColdRoomPan-rt/src → `FAIL companion-flag … 'anyNoHardware'`
(exit 1) at df8c7ec. `anyNoHardware` is a method-LOCAL (`BDefrostController.java:718`, only decl, inside
`requestDefrostCycle()` :713 which has NO Clock.schedule; the schedules are :808/:810/:850 in other methods).
**Code anchors (`toolbelt/lint-timers.sh`):** the companion-flag check is the awk at **:135-212**; Pass 1 (:135-168) is
meant to "find a method body that contains both Clock.schedule and an `= true` assignment" but (a) it scans FORWARD from the
assignment line brace-balancing the NEXT block, not the ENCLOSING method — so a schedule later in the file can be pulled in;
and (b) it never checks whether the identifier is a class FIELD vs a method LOCAL. The FAIL row is emitted at **:212**.
**Refinement (both guards; either alone kills this case):**
1. Only consider a flag that is a **class FIELD** — its declaration is at class scope (`^\s*(private|protected|public|static|final|volatile|transient|\s)*\b(boolean|int)\b\s+<name>\s*[;=]` OUTSIDE any method body), NOT a `type name = …;` inside a method. Collect field names in a class-scope pass (reuse the property/field collector idiom already in the script).
2. Pair the flag with a Clock.schedule* only when the schedule is in the **same ENCLOSING method body** as the assignment — anchor the body scan at the method the assignment sits IN (brace-count the method opening `{` back from the signature), not forward from the assignment.
**RED fixture (`tests/lint-timers.bats`, add):**
- NEG: a class with a method-local `boolean armed=false;` set true, a `Clock.schedule*()` in a DIFFERENT method, ticket cancelled in stopped() → companion-flag **no FAIL** (exit 0). (Reproduces `anyNoHardware`.)
- POS (regression, must stay FAIL): a class FIELD `private boolean startingUp;` set true in the SAME method as a `Clock.schedule*()` and never `= false` in stopped()/started() → companion-flag **still FAILs** (the real CompPan `startingUp`/`powerOnTicket` shape the header cites at :1760/:1764).
**Expected smoke after fix:** `lint-timers.sh` on ColdRoomPan-rt/src @ ff1b659 → exit 0 (BDefrostController no longer flagged); a synthetic field-flag case still FAILs. `[ev: FAIL reproduced df8c7ec, exit 1]`

## S22 — `lint-ext-writable-shape.sh`: exempt only when an @NiagaraAction BODY writes THAT slot
**Defect (VERIFIED false-negative):** CompPan-rt @ ff1b659 → 0 WARN; `BCompressorControl.faultReset` (complex OPERATOR
`BStatusBoolean`, :375) is exempted only by the unrelated HIDDEN `powerOnExpired`/`tick` actions (:411-413).
**Code anchors (`toolbelt/lint-ext-writable-shape.sh`):** the exemption is the class-level `has_action` flag set at
**:82-91** (any `@NiagaraAction` on the class, hidden included — comment :70-73 states the coarse "developer thought about
the action model" rationale). The rule fires WARN only when `has_action == 0`.
**Refinement:** replace class-level `has_action` with a **per-slot writing-action** check — a complex OPERATOR slot `X` is
exempt only when the class declares an `@NiagaraAction` whose METHOD BODY writes `X` (`set<X>(…)`, `setX(`, or
`.set(<Xprop>,`), reusing S18/`lint-silent-protection`'s slot→writer body-follow (`SURF_WRITE` idiom, silent-protection
:124-165). Keep EW3's `setSetpoint` positive (its body writes setpoint); EW6 stays WARN; the C9 EW-token/child-leaf note
unchanged. **This is a CONTRACT change** (EW10 pinned CompPan-rt=0 under the loose rule) → QA must re-issue the RED's EW10
to CompPan-rt **1** (faultReset) before merge; flag to QA as a C10 RED re-cut (K13 — the RED is the contract).
**RED fixture:** a class with a complex OPERATOR slot + an unrelated HIDDEN action that does NOT write it → WARN; a class
whose action body writes the slot → clean.
**Expected smoke after fix:** CompPan-rt gains `faultReset` (1 WARN); DashboardPan-rt still 1 (`BRoomPanel.setpoint`);
ColdRoomPan-rt / DashboardPan-ux 0. `[ev: faultReset exempt, 0 WARN @ ff1b659]`

## S23 — `lint-silent-protection.sh`: recognise the Pattern B (`BIAlarmSource`/`AlarmSupport`) adapter surface
**Defect (VERIFIED false-positive):** CompPan-rt @ ff1b659 → `WARN … CompressorControl.java:294 step forces/sheds stage …
no alarm surface`, even though PR9 wired CP-1 as Pattern B: `BCompressorControl implements BIAlarmSource` + `new
AlarmSupport(` + `newOffnormalAlarm` driven by the pure `AlarmEdge`. The trip lives in the pure `CompressorControl` (:294);
the alarm surface lives in the ADAPTER `BCompressorControl` — the follow does not cross that boundary.
**Code anchors (`toolbelt/lint-silent-protection.sh`):** the surface recogniser accepts a surface at **:222** (`index(…,
"BAlarmSourceExt")` or `"BAlarmRecord"`) and criterion (1) "file has BAlarmSourceExt" at **:424** — Pattern A only. There
is no recognition of `implements BIAlarmSource` / `newOffnormalAlarm` / `AlarmSupport`, and no adapter→pure cross-file link.
**Refinement:** extend the surface allowlist so a trip in a pure class `C` is exempt when its ADAPTER (the `B<C>` in the same
module that constructs `C`) carries a Pattern B surface: source contains `implements … BIAlarmSource` AND (`newOffnormalAlarm`
OR `new AlarmSupport(`), OR a child `BAlarmSourceExt` on a point the pure trip field drives (Pattern A already handled). Add
the adapter→pure follow (the `B<PureClass>` naming pair is the existing convention). Keep the private-field / effect-slot
exemptions and the exactly-one-WARN-per-trip dedupe.
**RED fixture:** a pure class with a shed/force trip + its `B`-adapter implementing `BIAlarmSource` with `newOffnormalAlarm`
→ no WARN; the same trip with NO adapter surface → WARN stays.
**Expected smoke after fix:** CompPan-rt silent-protection → **0** (CP-1 now recognised as surfaced via Pattern B);
ColdRoomPan-rt 0 (CR-3 already surfaced via Pattern A freezeAlarmPt since PR8); DashboardPan 0. `[ev: WARN reproduced @ ff1b659 :294]`

## Sequencing / risk
- Independent kit PRs; each ships its RED + a four-root smoke re-pin. S22 needs a QA RED re-cut (contract change); S21/S23
  are FP-only (no contract change — S23 removes a WARN, S21 removes a FAIL).
- All three are the same coarse-heuristic family; land them together as a "lint precision" wave and update the C9 lints'
  BUILD-LOOP §5 rows only if a flag/exit changes (they do not).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | S21 companion-flag STILL FAILs on anyNoHardware; scan-forward + no field check | [CERT] | lint-timers.sh :135-212 @ df8c7ec; run exit 1 |
| 2 | S22 faultReset exempt via class-level has_action (hidden actions count) | [CERT] | lint-ext-writable-shape.sh :82-91; run 0 WARN @ ff1b659 |
| 3 | S23 CompressorControl:294 still WARNs post-PR9 Pattern B; recogniser is Pattern-A-only | [CERT] | lint-silent-protection.sh :222,:424; run WARN @ ff1b659 |
| 4 | S22 is a contract change (EW10 CompPan-rt 0→1) needing a QA RED re-cut | [CERT] | EW10 @ 269be48 |
