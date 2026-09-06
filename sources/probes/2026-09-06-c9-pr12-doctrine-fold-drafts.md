# C9 PR12 / R12 — doctrine fold drafts (kit `docs/c9-doctrine`)

Author: companero (Fable), 2026-09-06. Four folds, each with its insertion anchor at kit `main` and the exact text to paste.
Verified BEFORE drafting (grep at main): `types/logic.md` has NO `## Protection anatomy` heading — the only B827 mention is
the R15.2 health line at `:95`; `types/logic-authoring.md:62-70` ALREADY carries the slot-type doctrine (C8 PR15) → verify-only;
`BUILD-LOOP.md:68 ## 5. Verify gate` has no K22 cross-reference; no `change_log`/unified-write-audit line exists anywhere in
the kit. `[ev: kit main grep 2026-09-06]`

## REFRESH 2026-09-06 — token-resolution state
`retros/2026-09-06-campaign9-demand-scope.md` and `…-silent-protection.md` are ALREADY on niagara-tools main, so
`[ev: retro campaign9-demand-scope]` / `[ev: retro campaign9-silent-protection]` resolve today — Fold 3's demand/silent
references are safe to land now. `campaign9-ext-writable-shape`, `campaign9-doctrine-fold`, `campaign9-wave-lessons`, and
`campaign9-close-process-meta-lessons` are created by their own PRs (PR10/PR12/PR13), so keep the ordering rule: land PR12
AFTER those retros exist, or `sweep-fold-audit --strict` fails on the dangling token.

## Fold 1 — NEW `## Protection anatomy [ev: corpus B821][ev: corpus B827]` in `types/logic.md`, after `## RT control logic` (:87-95)
Insert as a new H2 immediately after the RT-control-logic block (keep :95's health line where it is; this section is what it points to).
```markdown
## Protection anatomy [ev: corpus B821][ev: corpus B827]

A protection is a latch (`freezeTripped`; or the CP-1 LP-floor shed, which is inline with NO named field — the silent case B824 flags) that OVERRIDES the normal command path. It has four tiers, and each
tier that exists must be VISIBLE — a protection that silently holds an output is indistinguishable from a broken relay
(`lint-silent-protection.sh`, [ev: retro campaign9-silent-protection]):

1. **Setpoint + hysteresis** — the trip and the restart thresholds are two slots (`freezeSetpoint`/`freezeDiffStop`/`freezeDiffRestart`), never one.
2. **Latch** — a private boolean assigned in ONE pure function (`ColdRoomControl.freezeTrip(...)`), tested without Baja.
3. **Override** — the latch wins over HOA/mode at the apply stage (`valveInhibited()`), and the override is logged once per edge.
4. **Alarm** — the latch is SURFACED as an alarm record, one per edge, using one of two patterns:

   - **Pattern A — child point + `BAlarmSourceExt`** (when the latch can be expressed as a boolean point): declare a frozen
     child `BBooleanPoint` on the unit, hang a `BAlarmSourceExt` with `BBooleanChangeOfStateAlgorithm(alarmValue=true)` on
     it, and write the point's `out` from the latch in the recompute. The ext owns the edge (raises on false→true, clears on
     true→false) — the module keeps NO alarm state. Legality: the ext's PARENT must be a `BControlPoint` (`BPointExtension.isParentLegal` :64-66, narrowed by `BAlarmSourceExt.isParentLegal` :1073-1078) and the ALGORITHM's grandparent must be a `BBooleanPoint`
     (`BBooleanChangeOfStateAlgorithm.isGrandparentLegal` :86-89), so the ext cannot sit on the unit itself. [ev: corpus B827 §827.3]
   - **Pattern B — `BIAlarmSource` + transient `AlarmSupport`** (when the source is not a point, or several trips share one
     source): the component implements `BIAlarmSource` (a VISIBLE `@NiagaraAction BBoolean ackAlarm(BAlarmRecord)` whose `doAckAlarm` delegates to `support.ackAlarm` — never hidden: the console must invoke it and hidden actions are not invocable [ev: retro hidden-actions-not-invocable-and-runtime-anchor-verification]), creates
     `new AlarmSupport(this, alarmClass)` in `started()`, and calls `newOffnormalAlarm(alarmData)` / `toNormal(...)` ONLY on
     the edge computed by a pure edge machine (`AlarmEdge.decide(trip, nowOffnormal, recoveredPastDeadband) → FIRE|CLEAR|NONE`)
     that is re-seeded from the CURRENT condition in `started()` (a restart never re-fires). [ev: corpus B827 §827.4 §827.6]

   Both route a `BAlarmRecord` with `sourceState = offnormal`; high/low is an `alarmData` key, never a sourceState. The
   dashboard's alarm strip selects `sourceState = 'offnormal' or 'fault'`. The live routing is HARNESS-ONLY (station);
   WSL tests pin the structure (child declared, ext + algorithm present, drive line in the recompute; `implements
   BIAlarmSource`, `new AlarmSupport(` in `started()`, `newOffnormalAlarm` inside the FIRE branch) and the pure edge machine.
   [ev: RED qa/c9-alarm-cr3 70a357b][ev: RED qa/c9-alarm-cp1 8b43488][ev: corpus B827 §827.3 §827.4]
```

## Fold 2 — slot-type doctrine: VERIFY ONLY (already at `types/logic-authoring.md:62-70`, C8 PR15)
No new text. One optional cross-reference at the END of that section (only if PR10 lands first):
```markdown
`toolbelt/lint-ext-writable-shape.sh <src>` flags the anti-shape (an OPERATOR complex property with no writing action) [ev: retro campaign9-ext-writable-shape].
```

## Fold 3 — `BUILD-LOOP.md §5 Verify gate` (:68): K22 cross-reference line
Append to the §5 bullet list (after the pre-gate lints):
```markdown
- Every lint that ships in a campaign carries a REAL-TREE smoke on the four client module roots: exact COUNT + SUBJECT of each
  finding + one ABSENCE pin (a slot that must NOT be flagged) — a bats fixture alone is not acceptance (METHODOLOGY K22)
  [ev: retro campaign8-close-process-meta-lessons §lesson 11].
```

## Fold 4 — unified-write-audit doctrine line (`types/dashboard.md`, §write path / servlet section — or BUILD-LOOP §6 if the file has no write-path section)
```markdown
**One audit sink for every write surface.** A dashboard with two write surfaces (an external write-server and the in-station
servlet) keeps ONE canonical table (`public.change_log`): write-server rows carry `surface='write-server'` and an opaque
`config_session` id; servlet writes are audited by the framework (`AuditEvent` fires only when the `Context` carries a user,
`ComplexSlotMap.set`) and MIRRORED into the same table with `surface='servlet'`, `config_session` NULL (the framework record
has no session field), deduplicated on the full `(ts, user, target, old, new)` tuple, behind a flag that is OFF by default.
Audit-append never fails the write (spool + replay); enabling the mirror live is a station gate, not a PR gate.
[ev: corpus B829][ev: corpus B830 §830.4]
```

## Apply notes
- `[ev:]` tokens must resolve: `campaign9-*` retro slugs are created by PR13 (or by each kit PR's own retro) — sequence PR12
  after those retros exist, or `sweep-fold-audit --strict` fails on a dangling token (C8 lesson).
- `kit-links.bats` resolves script names: `lint-silent-protection.sh` (PR3) and `lint-ext-writable-shape.sh` (PR10) must
  exist on main before Fold 1/2 reference them.
- Section-scoped: do not touch `:95`'s existing health line; Fold 1 is a NEW H2 so `sweep-build-state` sees a heading add, not an edit.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | no `Protection anatomy` heading; B827 only at logic.md:95 | [CERT] | `grep -n 'Protection anatomy\|B827' types/logic.md` @ main |
| 2 | slot-type section at logic-authoring.md:62-70 | [CERT] | grep @ main (C8 PR15) |
| 3 | BUILD-LOOP §5 at :68, no K22 mention | [CERT] | grep @ main; METHODOLOGY.md:86 K22 |
| 4 | no change_log line in the kit | [CERT] | `grep -rn change_log <kit>` = 0 |
| 5 | Pattern A/B mechanics | [CERT] | B827 §827.3/.4/.6 |
