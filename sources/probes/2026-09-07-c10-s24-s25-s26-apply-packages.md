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

## S25 — `lint-write-path.sh`: NEW advisory class STALE (lead's decided contract — supersedes my WARN-only recommendation)
**Decision (lead):** do NOT weaken the shipped hard gate — a worker forgetting `--strict` must never silently ship an
uncovered OPERATOR slot. So the FAIL direction is UNCHANGED (uncovered source slot with no matrix row → exit 1). ADD a new
ADVISORY class **STALE**: a matrix data row whose slot name matches NO `@NiagaraProperty` / `--bog` link-traced slot in
source. Default: STALE rows print but exit stays **0** (advisory). `--strict` promotes STALE to **exit 1**. Exit **3**
(usage/env/missing-matrix) unchanged. So: uncovered → 1 always; STALE → 0 advisory / 1 under --strict; usage → 3.
**Code seams (`toolbelt/lint-write-path.sh` @ cb79676):**
- `:161` `_matrix_slots=$(awk -F'|' …` — the matrix-slot extractor (R19.3: first cell is a backtick-wrapped identifier).
  The STALE pass reuses THIS set as its left side.
- `:310` `_AWK_SCANNER='…` — the per-profile source scan that yields the source OPERATOR slots (and, with `--bog`, the
  link-traced extras merged into the required set). The STALE pass's right side = the UNION of source `@NiagaraProperty`
  names (all flags, not only OPERATOR — a covered slot can be SUMMARY) + the `--bog` slots, collected here.
- `:374` `printf 'FAIL  lint-write-path … no matrix row'` + `:376` `FAILED=1`; `:383` `exit "$FAILED"`.
**Insertion:** after both sets exist (matrix slots from :161, source+bog slots from the :310 scan), add a STALE pass:
`for m in $_matrix_slots; do case " $source_and_bog_slots " in *" $m "*) : ;; *) printf 'write-path  STALE  %s  no source slot with that name\n' "$m"; STALE=1 ;; esac; done`.
Add `STALE=0` beside `FAILED=0` (:33); parse `--strict) STRICT=1; shift ;;` in the arg loop (:48, mirror lint-ext-writable-shape:38);
change `:383` to `exit $(( FAILED ? 1 : (STRICT && STALE ? 1 : 0) ))`. FAIL always wins (uncovered is still a hard 1).
**Row grammar:** `write-path  STALE  <slot>  no source slot with that name` (per lead).
**Real-tree ACTUAL @ ff1b659 (measured, R19.3 backtick-inner extractor vs all source `@NiagaraProperty` names):** NOT 0 — it
is **3**: `freezeEnabled` (:36), `hoaMode` (:31), `inhibit` (:33). **All three are LEGITIMATE conceptual/scenario rows, not
renamed-stale rows:** `freezeEnabled` is a mode, `hoaMode` is the abstract HOA command concept (the real slots are
`evapNValveMode`/`fanMode`…), `inhibit` is an Engine-link defrost signal (`| Engine link |`). So the literal STALE rule
FLAGS 3 intentional rows. **Decision needed (flag to QA/lead):** either (a) exempt rows whose first-cell identifier is
followed by a parenthetical concept marker or an `| Engine link` / non-`@NiagaraProperty` writer (so conceptual rows are not
STALE), or (b) accept the 3 and require the matrix to mark them (e.g. a trailing `(concept)`), or (c) confirm `inhibit` is a
`--bog` link-traced slot (it may be covered under `--bog`, dropping the count to 2). Expected-0 does NOT hold on the real
tree; report the 3 and the characterization, do not force 0.
**bats pin:** a matrix with one row for a slot absent from source → STALE row printed, exit 0; same under `--strict` → exit 1;
an uncovered source slot → exit 1 both ways (unchanged); usage → exit 3.

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
