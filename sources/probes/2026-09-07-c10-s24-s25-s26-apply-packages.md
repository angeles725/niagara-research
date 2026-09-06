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

## S25 — `lint-write-path.sh`: NEW advisory class STALE with a `[concept]` exemption (lead's decided contract)
**Decision (lead):** FAIL direction UNCHANGED (uncovered source OPERATOR slot with no matrix row → exit 1, always — a
shipped hard gate must not silently pass). ADD an advisory **STALE**: a matrix data row whose backtick-inner slot name
matches NO source `@NiagaraProperty` and NO `--bog` link-traced slot. A row is EXEMPT only when it carries the LITERAL token
**`[concept]`** anywhere in the row — nothing implicit (no parenthetical heuristics, no "Engine link" string match; an
implicit exemption is a comment-satisfiable pin by another name). Default: STALE prints, exit **0**. `--strict` promotes
STALE to exit **1**. Exit **3** unchanged.
**Code seams (`toolbelt/lint-write-path.sh` @ cb79676):**
- `:161-173` `_matrix_slots=$(awk … | sort -u)` — the matrix-slot extractor. It is a DE-DUPED NAME SET (`sort -u` :173),
  so STALE is NAME-LEVEL. Add a sibling extractor `_concept_slots` over the SAME awk but gated on the row containing
  `[concept]`: `if (index($0, "[concept]") == 0) { …emit the backtick-inner name… }` for `_matrix_slots` (the STALE
  candidates now EXCLUDE marked rows at extraction), OR keep `_matrix_slots` whole and build `_concept_slots` from rows
  WITH the token and subtract. Either way a name is a STALE candidate only if NO row bearing it carries `[concept]`.
- `:174-` `_bog_extra` (the `--bog` link-traced slot set). STALE's "covered" side = source `@NiagaraProperty` names ∪ `_bog_extra`.
- `:310` `_AWK_SCANNER` — the per-profile source scan; collect ALL `@NiagaraProperty` names here (not only OPERATOR — a
  covered/real slot may be SUMMARY) as the STALE right side.
- `:374` FAIL emit; `:383` `exit "$FAILED"`.
**Insertion:** `STALE=0` beside `FAILED=0` (:33); parse `--strict) STRICT=1; shift ;;` in the arg loop (:48). After both
sets exist: `for m in $_matrix_slots; do case " $src_names $_bog_extra $_concept_slots " in *" $m "*) : ;; *) printf 'write-path  STALE  %s  no source slot with that name\n' "$m"; STALE=1 ;; esac; done`. Exit:
`exit $(( FAILED ? 1 : (STRICT && STALE ? 1 : 0) ))` — FAIL always wins.
**Row grammar:** `write-path  STALE  <slot>  no source slot with that name`. **Exemption token:** literal `[concept]` in the row.
**Real-tree ACTUAL @ ff1b659 (measured):** STALE = **3** — `freezeEnabled`, `hoaMode`, `inhibit`. `inhibit` is NOT a
`--bog` slot (VERIFIED: `bog-nav links --slot inhibit` and `--slot-any` on the PANCCADIA config.bog → no matching links;
`lint-write-path --bog` traces none), so it stays STALE → the count is 3, not 2. All three are conceptual/scenario rows.

## S25-PR6 — matrix `[concept]` marker edit (docs-only chore, rides in client PR6; no jar, no version bump)
`_matrix_slots` is name-level, so ONE marked row per conceptual NAME clears it (its name enters `_concept_slots`; the other
rows sharing that name — hoaMode also at :32 and :52 — are then covered). Edit exactly THREE rows at
`<client-root>/docs/write-path-matrix.md` @ ff1b659 (append ` [concept]` inside the first cell, after the slot):
```
:31  | `hoaMode` [concept] | Dashboard operator | mid-cycle | HAND → immediate ON; OFF → immediate OFF; AUTO → autoValue | ✅ `w3_hoaFlipMidCycle` |
:33  | `inhibit` [concept] (defrost signal) | Engine link | mid-cycle | AUTO-computed ON → closed while inhibit active | ✅ `w4_inhibitBlocksAutoComputedOn` |
:36  | `freezeEnabled` [concept] | Workbench | any | disabled mode clears any persisted latch | ✅ `w5_freezeStatLatchSurvivesSpWrite` |
```
(Do NOT mark :40 `setpoint`+hoaMode — its backtick-inner is `setpoint`, a real slot, not STALE. :32 and :52 need no edit —
their name `hoaMode` is already exempt via :31. If QA prefers strict PER-ROW emit instead of name-level, mark :32 and :52
too — flag that choice; name-level is the minimal set and matches `:161`'s `sort -u`.)
**OBSERVED-flip pins (the real-tree acceptance):** at ff1b659 BEFORE the marker edit, `lint-write-path` STALE = **3**
(freezeEnabled, hoaMode, inhibit); AFTER the PR6 edit, STALE = **0**. `--strict` before → exit 1; after → exit 0. FAIL
(uncovered) count unchanged by this edit.

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
