# C9 PR10 / R10 — S19 `ext-writable-shape` lint: apply package (kit)

Author: companero (Fable), 2026-09-06 (rev 3 — RED tip is **`269be48`**; chain 3726722 → 28feb42 (script path) → 717d585
(`C9_CLIENT_ROOT`) → 3387c58 (EW11, K20 exit 3) → 269be48 (EW10 EXACT four-root contract). Rev 2 wrongly cited 717d585 as the
tip and called the four-root counts "acceptance, not pinned" — they ARE pinned at 269be48; retracted.) Contract VERBATIM from
QA's RED `qa/c9-ext-writable-shape` **`269be48`** (`tests/ext-writable-shape.bats`, EW1–EW11). Rule = B823 §823.2 + the slot-type doctrine (folded at C8 PR15,
`types/logic-authoring.md:62-70`); skeleton = `lint-delays.sh` (src-dir), exactly like S7/S18. The rule already has a working
reference implementation in `tools/module-find.py ext-writable` (niagara-research) — the kit lint is its bash/awk twin with
the RED's stricter "matching action" test. `[ev: RED 3726722 tests/ext-writable-shape.bats]` `[ev: corpus B823 §823.2]` `[ev: kit types/logic-authoring.md:62-70]`

## 0. D-a CLOSED (28feb42): the script is `toolbelt/lint-ext-writable-shape.sh` (bats :24); row token `ext-writable-shape` unchanged
Real-tree root (717d585, RP1): `ROOT="${C9_CLIENT_ROOT:-/home/cristian/modulos_niagara_n4/Cliente/Leon-Guanjuato-worktrees/main-a109249}"`
— the blessed read tree, never the local working copy. EW10 SKIPs unless `$ROOT/Dashboard`, `$ROOT/Compresores`, `$ROOT/Paccadia` all exist.

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
| EW10 (269be48) | **EXACT four-root contract at `$ROOT`**: `run $EW $ROOT/Dashboard/DashboardPan/DashboardPan-rt/src` → exit 0, `grep -c '^WARN'` == **1**, output contains `BRoomPanel` and `setpoint`; then for `Compresores/CompPan/CompPan-rt`, `Paccadia/ColdRoomPan/ColdRoomPan-rt`, `Dashboard/DashboardPan/DashboardPan-ux`: exit 0 and `grep -c '^WARN'` == **0** (CompPan-rt's `faultReset` has an action → clean) | the live regression `BRoomPanel.setpoint` (`BStatusNumeric` `SUMMARY\|OPERATOR`, no action, `:124-130`) is the ONLY hit on the four roots; rows must start with `WARN` at column 1 |
| EW11 (3387c58) | an EMPTY dir, and a dir with only a non-Java file | exit **3** + a row containing `ERROR` and `ext-writable-shape`, and NO `WARN` — never a silent 0 (K20) |
| mutation (K13) | drop the `BStatusX` recognizer (treat complex as plain) | EW1/EW6 stop WARNing |

## 2. The rule as implementable passes (per `*.java`, paren-balanced `@NiagaraProperty`/`@NiagaraAction` join — C8 D9b)
1. Collect properties: `name`, `type`, `flags`. **Complex** = type matches `(baja:|B)?Status(Numeric|Boolean|Enum)`
   (both the module-name and the Java-class forms — the client writes `"BStatusNumeric"`). **OPERATOR** = flags contain `OPERATOR`.
2. Collect actions in the SAME file (same type): `@NiagaraAction(name="X" …)`.
3. Flag a property iff complex AND OPERATOR AND the class exposes **NO `@NiagaraAction` at all**. **CORRECTION (2026-09-06,
   from investigador1's faultReset finding + the EW10 exact contract):** the shipped rule is a CLASS-LEVEL "any action"
   exemption, NOT a per-slot `set<Slot>` match. Proof from the RED at 269be48: EW3 clean (class has `setSetpoint`), EW6 WARN
   (class has NO action), and EW10 pins **CompPan-rt = 0** even though `BCompressorControl.faultReset` (complex OPERATOR
   `BStatusBoolean`, :375) has NO `setFaultReset` — it is exempted only by the unrelated HIDDEN `powerOnExpired`/`tick`
   actions (:411-413). A `set<Slot>` rule would WARN faultReset → EW10 would expect 1, not 0. So implement: complex OPERATOR
   slot + the enclosing class declares **≥1 `@NiagaraAction` (hidden counts)** → EXEMPT; else → WARN. SUMMARY-only complex →
   never flagged (EW4). This matches `module-find ext-writable` (`ok(has-action) … class exposes an action`).
   **Known imprecision (C10, filed cluster with S21 / niagara-tools #89):** "any action on the class" is a coarse heuristic —
   it false-NEGATIVEs `faultReset` (a hidden unrelated action shouldn't exempt an operator write). The C10 precision follow
   is a per-slot action-body match (does an action actually WRITE this slot?), the same coarse-heuristic family as the
   lint-timers companion-flag FP. C9 ships the loose rule as the RED pins it (K13); the refinement is C10, doc-note only.
4. Emit one WARN per flagged slot: `WARN  ext-writable-shape  <file>:<line-of-@NiagaraProperty>  <slot>: OPERATOR <type> with no writing action — external oBIX write must use the child leaf …/<slot>/value (bare <real>, B826) or add an OPERATOR action (B822)`; `FAILED=1` only under `--strict`.
Cross-check at GREEN: `python3 tools/module-find.py <src> ext-writable` (niagara-research) must flag the same slots on the
four client roots (module-find's rule is "any action on the class" — looser than EW3's `set<Slot>`; expect the kit lint to
flag ≥ module-find's set). `[ev: tools/module-find.py ext-writable (7fa61cb53 lineage)]`

## 3. Skeleton — `lint-delays.sh` shape (see the S7 package §4; identical plumbing, name `lint-ext-writable-shape.sh`)
`set -u`; `--strict` flag; usage guard exit 3; `-d` guard exit 3; `find "$SRC" -type d -name '.*' -prune -o -name '*.java' -print`;
per-file awk: paren-balanced annotation join → properties + actions → rule → rows; `WARNED` flag; exit 1 only with `--strict`.
`shellcheck` 0; VCS-free (kit-links L2).

## 4. Real-tree smoke — PINNED by the RED (EW10 @ 269be48 = count + subject + absence on the four roots)
- DashboardPan-rt exactly 1 WARN (`BRoomPanel.setpoint`); CompPan-rt 0 (`faultReset` has an action); ColdRoomPan-rt 0;
  DashboardPan-ux 0 — all with exit 0. This IS the K22 smoke; nothing extra to run for acceptance beyond `bats tests/ext-writable-shape.bats`
  with the blessed worktree present (or `C9_CLIENT_ROOT` set). K20 is pinned by EW11 (exit 3 + ERROR on no sources).
- Implementation consequence: the WARN row must start at column 1 with `WARN` (the pin counts `^WARN`), and the ERROR row for
  no-sources must contain `ERROR` + `ext-writable-shape` and exit 3 BEFORE any scan.
  `[ev: 2026-09-06-c9-r11-write-path-matrix-measurement.md (OPERATOR counts 10/46/20/0)]` `[ev: retro 2026-09-05 bog-nav/module-find §3 corrected]`

## 5. K19 routing + retro
- `BUILD-LOOP.md §5` pre-gate, beside `lint-wb-threading.sh`: ``` `toolbelt/lint-ext-writable-shape.sh [--strict] <src>` (ext-writable-shape: an OPERATOR complex property (BStatusNumeric/BStatusBoolean/BStatusEnum) with no writing action → WARN — write it via the oBIX child leaf …/value or add an OPERATOR action; exit 0 WARN-only / 1 under --strict / 3 usage) [ev: retro campaign9-ext-writable-shape] ```
- `skill/SKILL.md` toolbelt list: same. `kit-links.bats` resolves the name. `report-module.sh` member row (with R2/R3).
- Retro slug `campaign9-ext-writable-shape`; record the OBSERVED EW1 mutation flip + the four-root table.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | RED tip 269be48; script `lint-ext-writable-shape.sh`; EW10 exact four-root counts; EW11 exit 3 | [CERT] | `git log origin/qa/c9-ext-writable-shape`; bats setup :24/:27, EW10/EW11 bodies @ 269be48 (read 2026-09-06) |
| 2 | EW1–EW10 fixtures/expectations, row grammar, exits, D9b | [CERT] | bats :14-16, :31-156 |
| 3 | exemption = class exposes ANY @NiagaraAction (incl. hidden); NOT set<Slot> — proven by EW10 CompPan-rt=0 with faultReset exempt via hidden powerOnExpired | [CERT] | EW3/EW6 fixtures + EW10 @ 269be48; BCompressorControl.java:375,:411-413 @ a109249 |
| 4 | real BRoomPanel.setpoint shape | [CERT] | BRoomPanel.java:124-130 @ a109249 |
| 5 | four-root counts 1/0/0/0 | [CERT, pinned] | EW10 @ 269be48 (matches the module-find ext-writable measurement) |
