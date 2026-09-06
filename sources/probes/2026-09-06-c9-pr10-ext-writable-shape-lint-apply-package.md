# C9 PR10 / R10 — S19 `ext-writable-shape` lint: apply package (kit)

Author: companero (Fable), 2026-09-06. Contract VERBATIM from QA's RED `qa/c9-ext-writable-shape` **`3726722`**
(`tests/ext-writable-shape.bats`, EW1–EW10). Rule = B823 §823.2 + the slot-type doctrine (folded at C8 PR15,
`types/logic-authoring.md:62-70`); skeleton = `lint-delays.sh` (src-dir), exactly like S7/S18. The rule already has a working
reference implementation in `tools/module-find.py ext-writable` (niagara-research) — the kit lint is its bash/awk twin with
the RED's stricter "matching action" test. `[ev: RED 3726722 tests/ext-writable-shape.bats]` `[ev: corpus B823 §823.2]` `[ev: kit types/logic-authoring.md:62-70]`

## 0. ONE decision for the lead (the S7 naming conflict, again)
| RED (3726722) | Proposal (:158 file table) / lead's message | Recommendation |
|---|---|---|
| `setup(): EW="$KIT/toolbelt/ext-writable-shape.sh"` (bats :3); SURFACE line :14 `ext-writable-shape.sh` | `toolbelt/lint-ext-writable-shape.sh` | Same resolution as S7 (d0f5942): **QA re-issues the RED's `EW=` path to `lint-ext-writable-shape.sh`** (kit `lint-*.sh` convention); the ROW token `ext-writable-shape` (EW1 asserts it in OUTPUT) stays; the bats filename may stay `tests/ext-writable-shape.bats`. No other pin depends on the script filename. |

## 1. The contract (verbatim)
- **CLI:** `[lint-]ext-writable-shape.sh [--strict] <java-src-dir>` — one src dir, recursive (EW10 copies ONE real file into a flat dir).
- **Row (bats :15):** `WARN  ext-writable-shape  <file>:<line>  <slot>: OPERATOR <type> with no writing action …` + the child-leaf
  note (the S19 fix: write the `…/<slot>/value` child leaf with a bare `<real>`, B826-G2, or add an OPERATOR action, B822).
- **Exits:** `0` no WARN or WARN without `--strict` · `1` any WARN under `--strict` (EW7) · `3` usage (EW9). WARN-only.
- **D9b prune** (EW8: `src/.deploy-baseline/` ignored; output must not mention `Stale`).
| Pin | Fixture (inline) | Expect |
|---|---|---|
| EW1 | `@NiagaraProperty(name="setpoint", type="BStatusNumeric", … flags=Flags.SUMMARY\|Flags.OPERATOR)`, NO `@NiagaraAction` | WARN; output contains `ext-writable-shape` AND `setpoint` |
| EW2 | `differentialUp` `type="double"` OPERATOR | no WARN (plain simple) |
| EW3 | the EW1 slot + `@NiagaraAction(name="setSetpoint", parameterType="BDouble", … flags=Flags.OPERATOR)` | no WARN — a **matching** writing action (`set<Slot>`) makes it clean |
| EW4 | `zoneTemp1` `BStatusNumeric` `flags=Flags.SUMMARY` only | no WARN — SUMMARY-only complex = display slot, not a write target |
| EW5 | `defrostInterval` `BRelTime` OPERATOR + `coolOnSensorFault` `boolean` OPERATOR | no WARN (plain types) |
| EW6 | `manualEnable` `BStatusBoolean` OPERATOR, no action | WARN naming `manualEnable` |
| EW7 | EW1 shape | plain exit 0; `--strict` exit 1 |
| EW8 | clean `differentialUp` in `src/` + a `setpoint` StatusNumeric under `src/.deploy-baseline/` | no WARN, no `Stale` |
| EW9 | no argument | exit 3 |
| EW10 | the REAL `DashboardPan-rt/…/BRoomPanel.java` copied alone (skip if absent) | WARN naming `setpoint` (the live regression: `BRoomPanel.setpoint` is `BStatusNumeric` `SUMMARY\|OPERATOR` with no action, `:124-130` @ a109249) |
| mutation (K13) | drop the `BStatusX` recognizer (treat complex as plain) | EW1/EW6 stop WARNing |

## 2. The rule as implementable passes (per `*.java`, paren-balanced `@NiagaraProperty`/`@NiagaraAction` join — C8 D9b)
1. Collect properties: `name`, `type`, `flags`. **Complex** = type matches `(baja:|B)?Status(Numeric|Boolean|Enum)`
   (both the module-name and the Java-class forms — the client writes `"BStatusNumeric"`). **OPERATOR** = flags contain `OPERATOR`.
2. Collect actions in the SAME file (same type): `@NiagaraAction(name="X" …)`.
3. Flag a property iff complex AND OPERATOR AND no action **matches** it. Matching (EW3 is the only positive pin): an action
   named `set<Slot>` (case-insensitive on the first letter) — implement as `set` + capitalized slot name; **also accept** an
   action whose name contains the slot name or an action taking a `parameterType` and declared OPERATOR on a type with
   exactly one complex OPERATOR slot? — NO: keep it to `set<Slot>` (+ optionally `apply<Slot>`/`<slot>Cmd` from B822's
   additive-action doctrine); document the accepted names in the script header so a reviewer knows the seam. SUMMARY-only
   complex → never flagged (EW4).
4. Emit one WARN per flagged slot: `WARN  ext-writable-shape  <file>:<line-of-@NiagaraProperty>  <slot>: OPERATOR <type> with no writing action — external oBIX write must use the child leaf …/<slot>/value (bare <real>, B826) or add an OPERATOR action (B822)`; `FAILED=1` only under `--strict`.
Cross-check at GREEN: `python3 tools/module-find.py <src> ext-writable` (niagara-research) must flag the same slots on the
four client roots (module-find's rule is "any action on the class" — looser than EW3's `set<Slot>`; expect the kit lint to
flag ≥ module-find's set). `[ev: tools/module-find.py ext-writable (7fa61cb53 lineage)]`

## 3. Skeleton — `lint-delays.sh` shape (see the S7 package §4; identical plumbing, name `lint-ext-writable-shape.sh`)
`set -u`; `--strict` flag; usage guard exit 3; `-d` guard exit 3; `find "$SRC" -type d -name '.*' -prune -o -name '*.java' -print`;
per-file awk: paren-balanced annotation join → properties + actions → rule → rows; `WARNED` flag; exit 1 only with `--strict`.
`shellcheck` 0; VCS-free (kit-links L2).

## 4. Real-tree smoke — RED pin + PR10 acceptance (lesson 11: count + subject + absence)
- RED (EW10): `BRoomPanel.java` alone → ≥1 WARN whose subject is `setpoint`.
- PR10 acceptance (proposal PR10 row): all four client module roots at `a109249` with exact counts + subjects + absence.
  Expected from the C8 measurement (`module-find slot-types`/`ext-writable`): **DashboardPan-rt: `BRoomPanel.setpoint` = 1
  WARN** (the only OPERATOR complex property with no action); ColdRoomPan-rt: 0 (its `BStatusNumeric`/`BStatusBoolean` are
  SUMMARY/TRANSIENT, none OPERATOR); CompPan-rt: 0 (same); DashboardPan-ux: 0 OPERATOR slots. Absence pins: no WARN on any
  `SUMMARY`-only `zoneTemp*`/`evapTemp*`/`*State` slot. Re-measure at GREEN; do not predict beyond these.
  `[ev: 2026-09-06-c9-r11-write-path-matrix-measurement.md (OPERATOR counts 10/46/20/0)]` `[ev: retro 2026-09-05 bog-nav/module-find §3 corrected]`

## 5. K19 routing + retro
- `BUILD-LOOP.md §5` pre-gate, beside `lint-wb-threading.sh`: ``` `toolbelt/lint-ext-writable-shape.sh [--strict] <src>` (ext-writable-shape: an OPERATOR complex property (BStatusNumeric/BStatusBoolean/BStatusEnum) with no writing action → WARN — write it via the oBIX child leaf …/value or add an OPERATOR action; exit 0 WARN-only / 1 under --strict / 3 usage) [ev: retro campaign9-ext-writable-shape] ```
- `skill/SKILL.md` toolbelt list: same. `kit-links.bats` resolves the name. `report-module.sh` member row (with R2/R3).
- Retro slug `campaign9-ext-writable-shape`; record the OBSERVED EW1 mutation flip + the four-root table.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | RED invokes `ext-writable-shape.sh`; proposal names `lint-ext-writable-shape.sh` | [CERT] | bats setup :3 @ 3726722; proposal :158 |
| 2 | EW1–EW10 fixtures/expectations, row grammar, exits, D9b | [CERT] | bats :14-16, :31-156 |
| 3 | EW3 = `set<Slot>` matching action is the only positive pin | [CERT] | bats :62 |
| 4 | real BRoomPanel.setpoint shape | [CERT] | BRoomPanel.java:124-130 @ a109249 |
| 5 | four-root expected counts (1/0/0/0) | [INFER, measured proxy] | module-find ext-writable; confirm with the kit lint at GREEN |
