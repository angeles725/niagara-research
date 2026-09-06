# C10 apply-packages — S24 (cwd-independent structural tests) · S25 (lint-write-path --strict) · S26 (client .gitignore)

Author: companero (Fable), 2026-09-06. Cut against kit `df8c7ec`/`cb79676` and client `ff1b659` (worktree
`Leon-Guanjuato-worktrees/c10-ff1b659`, read-only). Three independent refinements. `[ev: kit toolbelt @ df8c7ec]`
`[ev: client WiringTests @ ff1b659]` `[ev: git ls-files @ ff1b659]`

## S24 — make the structural WiringTests resolve src from ANY cwd
**Current behaviour (VERIFIED):** the tests read the module SOURCE by a cwd-relative path.
- `run-pure-test.sh` (`toolbelt/run-pure-test.sh`) takes the module dir as `$1` (`rt=$1` :27) and compiles with
  `-sourcepath "$rt/src:$testroot"` (:58-59); it runs `java` in the CALLER's cwd — it never `cd`s into `$rt`. So the JVM
  cwd = wherever the caller stood.
- `FreezeAlarmWiringTest.java` (`srcTest/test/com/angeles/ColdRoomPan/`): `SRC = Paths.get("src/com/angeles/ColdRoomPan/BEvaporatorUnit.java")`
  (:48, RELATIVE), `GROUP_BUILD = Paths.get("../../build.gradle.kts")` (:49), and `read()` (:54-60) tries
  `{ p, here.resolve(p), here.resolve("ColdRoomPan-rt").resolve(p) }` where `here = Paths.get("").toAbsolutePath()` (:56).
  It resolves ONLY if the JVM cwd is the module-rt-dir or its parent — brittle.
- `CompressorAlarmWiringTest.java`: same shape (`SRC` :26, `GROUP_BUILD` :27, `code()`/`read()` :31-33).
**Fix — two options:**
1. **Runner-side (RECOMMENDED, no test edits):** in `run-pure-test.sh`, run the final `java` with working directory `$rt`
   (a subshell, `$tmp`/`$JU`/`$HC` are already absolute): `( cd "$rt" && java -cp "$tmp:$JU:$HC" org.junit.runner.JUnitCore "$testfqcn" )`.
   From `$rt` (e.g. `…/CompPan-rt`), `Paths.get("src/…")` and `../../build.gradle.kts` both resolve for EVERY cwd-relative
   test, with no client-source churn. This is the minimal, kit-only fix.
2. **Test-side (robust, but edits every WiringTest):** resolve `SRC`/`GROUP_BUILD` against `System.getProperty("module.rt.dir")`,
   and have `run-pure-test.sh` pass `-Dmodule.rt.dir="$rt"`. Belt-and-braces for a test ever run outside the runner.
**Recommend option 1** (kit change only); note option 2 as the follow if a test must run standalone.
**RED/pin:** a bats case that runs `run-pure-test.sh <rt> <fqcn>` FROM A DIFFERENT cwd (e.g. `/tmp`) and asserts GREEN —
today it fails to locate src; after the fix it passes. `[ev: run-pure-test.sh:27,:58-59; WiringTest:48-56 @ ff1b659]`

## S25 — `lint-write-path.sh`: NEW advisory class STALE, PER-ROW, with a `[concept]` exemption (lead's decided contract)
**Decision (lead):** FAIL direction UNCHANGED (uncovered source OPERATOR slot with no matrix row → exit 1, always). ADD a
**PER-ROW** advisory **STALE**: emit ONE STALE line per matrix DATA ROW whose backtick-inner slot name is NOT in
(source `@NiagaraProperty` names ∪ source `@NiagaraAction` names ∪ `--bog` extras) AND which does not itself carry the
literal token **`[concept]`**. Per-row (not per-name): a name-level exemption would let one marked `hoaMode` row silently
exempt two unmarked `hoaMode` rows — the cross-row implicit exemption we ruled out. Default: STALE prints, exit **0**;
`--strict` promotes to exit **1**; exit **3** unchanged.
**Covered side — MATRIX-ROOT harvest (lead's rule), NOT the `:310` per-module scanner:** the covered set is harvested from
EVERY Java source under the MATRIX ROOT (the dir holding `docs/write-path-matrix.md`, which the lint already walks up to —
resolution loop `:98` `_candidate="$_dir/docs/write-path-matrix.md"`, `:114` `_parent=$(dirname "$_dir")`; the matrix root is
the parent of `docs/`), ALL modules, with `build/` and dot-dirs pruned, ∪ `--bog` extras. Do NOT reuse the `:310`
`_AWK_SCANNER` — it emits OPERATOR `@NiagaraProperty` only (`if (prop_name != "" && prop_op) print`, :338), per-module, so it
would (a) drop non-OPERATOR/SUMMARY slots and (b) drop every `@NiagaraAction`. The harvest must catch BOTH `@NiagaraProperty`
and `@NiagaraAction`, any flag, and (critically) MULTI-LINE annotations where `name = "X"` sits on its own line — so match
the `name = "X"` field line, NOT a single-line `@Niagara…name=` regex (that misses the multi-line majority; a single-line
regex gave a false 56-name set / 72 STALE — corrected). Two matrix rows document action-invoked scenarios and MUST stay
covered: `:64 intervalExpired` and `:65 forceDefrost` are `@NiagaraAction` (BDefrostController.java:148/:152); with the
`name=`-field harvest they are covered (STALE 5); with the OPERATOR-only `:310` output they would false-flag (STALE 7).
Deterministic harvest (uncovered FAIL stays per-module; only STALE's covered set is matrix-root-wide):
`find "$MATRIX_ROOT" -type d \( -name '.*' -o -name build \) -prune -o -name '*.java' -print | xargs grep -hoE 'name[[:space:]]*=[[:space:]]*"[A-Za-z][A-Za-z0-9_]*"' | sed -E 's/.*"([^"]+)".*/\1/' | sort -u` ∪ `_bog_extra`.
**Code seams (`toolbelt/lint-write-path.sh` @ cb79676):**
- `:161-173` `_matrix_slots=$(awk … | sort -u)` — currently NAME-LEVEL. For PER-ROW STALE, do NOT reuse this sort-u set;
  add a ROW pass over the matrix that, per data row, extracts the backtick-inner slot AND checks for `[concept]` in the row
  (`index($0,"[concept]")`), skipping marked rows BEFORE any de-dup. The `[concept]` filter is per-row, ahead of `sort -u`.
- covered side = the broader property+action harvest above ∪ `_bog_extra` (`:174`).
- `:374` FAIL emit; `:383` `exit "$FAILED"`.
**Insertion:** `STALE=0` beside `FAILED=0` (:33); `--strict) STRICT=1; shift ;;` in the arg loop (:48). The STALE row pass
(pseudocode): `awk` over the matrix emitting, per data row lacking `[concept]`, the backtick-inner name; for each such name
`case " $prop_and_action_names $_bog_extra " in *" $name "*) : ;; *) printf 'write-path  STALE  %s  no source slot with that name\n' "$name"; STALE=1 ;; esac`. Exit: `exit $(( FAILED ? 1 : (STRICT && STALE ? 1 : 0) ))` — FAIL always wins.
**Row grammar:** `write-path  STALE  <slot>  no source slot with that name`. **Exemption:** literal `[concept]` in the row.
**Real-tree ACTUAL @ ff1b659 (PER-ROW, R19.3 extractor, covered = all property+action names ∪ --bog):** STALE = **5 rows** —
`:31 hoaMode`, `:32 hoaMode`, `:33 inhibit`, `:36 freezeEnabled`, `:52 hoaMode`. (`inhibit` VERIFIED not a `--bog` slot:
`bog-nav links --slot inhibit`/`--slot-any` on the PANCCADIA config.bog → no links.) `:40 setpoint`+hoaMode is NOT STALE
(first backtick-inner is `setpoint`, a real slot). The action rows `:64 intervalExpired` / `:65 forceDefrost` are NOT STALE
because the covered side includes `@NiagaraAction` names.

## S25-PR6 — matrix `[concept]` marker edit (docs-only chore, rides in client PR6; no jar, no version bump)
Per-row → mark ALL FIVE STALE rows at `<client-root>/docs/write-path-matrix.md` @ ff1b659 (append ` [concept]` inside the
first cell). `:40` untouched (its slot is `setpoint`).
```
:31  | `hoaMode` [concept] | Dashboard operator | mid-cycle | HAND → immediate ON; OFF → immediate OFF; AUTO → autoValue | ✅ `w3_hoaFlipMidCycle` |
:32  | `hoaMode` [concept] + inhibit active | Dashboard operator | mid-defrost | inhibit forces OFF in ANY mode (HAND and AUTO) | ✅ `w4_inhibitBlocksAutoComputedOn` |
:33  | `inhibit` [concept] (defrost signal) | Engine link | mid-cycle | AUTO-computed ON → closed while inhibit active | ✅ `w4_inhibitBlocksAutoComputedOn` |
:36  | `freezeEnabled` [concept] | Workbench | any | disabled mode clears any persisted latch | ✅ `w5_freezeStatLatchSurvivesSpWrite` |
:52  | `hoaMode` [concept] + `setpoint` (two writes) | Dashboard (one engine frame) | mid-cycle | changed() ordering race — which write's value is seen first | ❌ C10 |
```
**OBSERVED-flip pins (real-tree acceptance):** at ff1b659 BEFORE the marker edit, `lint-write-path` STALE = **5**; AFTER the
PR6 edit, STALE = **0**; `--strict` before → exit 1, after → exit 0; FAIL (uncovered) count unchanged.

## S26 — client `.gitignore` for gradle build caches (keep the deploy jars)
**Tracked under `**/build/` @ ff1b659 (exact, `git ls-files`):**
| Path | Count | Disposition |
|---|---|---|
| `**/build/classes/java/main/**/*.class` | 43 | **UNTRACK** (compile cache; churns every build) |
| `**/build/tmp/compileJava/` (`previous-compilation-data.bin`) + `**/build/tmp/jar/` (`MANIFEST.MF`) | 8 | **UNTRACK** (incremental cache) |
| `**/build/libs/*.jar` (CompPan-rt, ColdRoomPan-rt, DashboardPan-rt, DashboardPan-ux) | 4 | **KEEP** — the deploy jars the station RARs consume |
| `**/build/manifest/writeModuleXml/module.xml` | 4 | **KEEP** — the deploy module.xml |
**`.gitignore` (root) — add:**
```gitignore
# Gradle incremental-compile cache — churns every build; NOT the deploy artifacts (libs/manifest stay tracked)
**/build/tmp/
**/build/classes/
```
**One-time untrack + PROOF no tracked jar/manifest is lost:**
```bash
cd <client-root>
before_keep=$(git ls-files '**/build/libs/*.jar' '**/build/manifest/**/module.xml' | sort)   # 8 lines
git rm -r --cached $(git ls-files '**/build/tmp' '**/build/classes')                           # untrack the 51 churn files
after_keep=$(git ls-files '**/build/libs/*.jar' '**/build/manifest/**/module.xml' | sort)      # must be UNCHANGED
diff <(printf '%s\n' "$before_keep") <(printf '%s\n' "$after_keep") && echo "OK: 8 deploy artifacts still tracked, none lost"
git status --porcelain | grep -c '^D '   # == 51 (the untracked churn), and 0 of them are libs/manifest
git commit -m "chore: gitignore gradle build cache (build/tmp, build/classes); keep libs+manifest deploy artifacts"
```
The `diff` exiting 0 is the proof: the 8 deploy artifacts are tracked before AND after; only the 51 cache files move to
untracked. **Chore, no RED.** `[ev: git ls-files @ ff1b659 — 43 class + 8 tmp churn, 4 jars + 4 module.xml keep]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | run-pure-test.sh doesn't cd into $rt; WiringTests read cwd-relative src | [CERT] | run-pure-test.sh:27,:58-59; WiringTest:48-56 @ ff1b659 |
| 2 | lint-write-path is already a hard FAIL (exit 1) lint; --strict must reconcile semantics | [CERT] | lint-write-path.sh:25,:33 |
| 3 | 43 class + 8 tmp churn tracked; 4 jars + 4 module.xml to keep | [CERT] | git ls-files @ ff1b659 |
| 4 | the diff-based proof shows no deploy artifact lost | [CERT] | git rm --cached scoped to tmp+classes only |
