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

## S25 — `lint-write-path.sh --strict`
**Current (`toolbelt/lint-write-path.sh`):** `Usage: lint-write-path.sh <module-root> [--bog <config.bog>] [--matrix <path>]`;
`FAILED=0` (:33); usage guard exit 3 (:38-42); arg loop (:48+) handles `--bog`/`--matrix`; exits **0 all covered · 1 any
uncovered · 3 usage/env** (:25). NOTE: lint-write-path ALREADY exits 1 on any uncovered slot (it is a FAIL lint, not
WARN-only) — so `--strict` here means the OPPOSITE of the WARN lints: today an uncovered slot is already exit 1. The design
assumed a `--strict` toggle; reconcile the semantics:
- If the intent is "WARN by default, FAIL under --strict" (match `lint-ext-writable-shape`): change the DEFAULT to emit
  `WARN` rows and exit 0, and `--strict` restores the current exit-1-on-uncovered. Insertion: add `STRICT=0` beside
  `FAILED=0` (:33); parse `--strict) STRICT=1; shift ;;` in the arg loop (:48, same shape as lint-ext-writable-shape:38);
  at the end, `exit $(( STRICT==1 ? (FAILED?1:0) : 0 ))` and print rows as `WARN` unless `--strict`.
- If the intent is only to KEEP exit-1 but allow a non-strict advisory pass, add `--strict` as the exit-1 gate and make the
  default exit 0 with WARN rows. Either way the exit contract stays **0 / 1 / 3** and MUST mirror `lint-ext-writable-shape`
  (0 clean-or-WARN · 1 under --strict · 3 usage) for consistency (the C9 design assumed this).
**Decision for QA/lead:** confirm which semantics (lint-write-path is currently a hard FAIL lint; the other C9 lints are
WARN-only+--strict). Recommend aligning to WARN-only+--strict so `report-module.sh` treats all lints uniformly.
**bats pin:** `--strict` on an uncovered tree → exit 1; without `--strict` → exit 0 with WARN rows; usage → exit 3;
covered tree → exit 0 both ways. `[ev: lint-write-path.sh:25,:33,:38-48; lint-ext-writable-shape.sh:35-45]`

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
