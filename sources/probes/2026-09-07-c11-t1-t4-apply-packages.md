# C11 apply-packages — T1 shared parser · T2 client-root · T3 concept-drift · T4 unpinned-guard

Author: companero (Fable), 2026-09-06. Cut against kit main **`dab0807`** (v0.21.0). Read trees frozen: `main-ff1b659`
(pre-PR6) and `main-00e7118` (post-PR6, current client main). Execute-only. No Co-Authored-By / AI trailers. `[ev: kit dab0807]`
`[ev: B832 593019540]`

## T1 — shared method-boundary parser (fix the one-liner false-negative, then de-duplicate)
**Where it lives:** `toolbelt/lib/method-boundary.sh` — a sourced SHELL fragment exporting the parser as an awk-function
string (the kit's existing embed-awk-in-a-shell-var idiom), e.g. `MB_PARSER_AWK='function mb_open(...) { … }'`. Each lint
`. "$TOOLBELT/lib/method-boundary.sh"` then splices `$MB_PARSER_AWK` ahead of its own main awk. (An `awk -f lib.awk -f -`
split is the alternative; the shell-var embed matches how the lints already carry awk.)
**Standard = PEAK depth (B832, reproduced):** the fragment adopts `max_d` (peak depth during the line), NOT net brace change.
Reproduced: a one-liner `void arm(){ armed=true; Clock.schedule(...); }` → lint-timers 0 companion-flag; the identical
multi-line body → 1 (FAIL). Net-depth silently drops one-liner methods.
**Cut lines @ dab0807 (each copy → the shared fragment):**
| Copy | Current block | Behaviour | Action |
|---|---|---|---|
| `lint-ext-writable-shape.sh:132-176` | `max_d` peak (`:142/:147/:176`, comment :145-146) | CORRECT | this is the CANONICAL source for the fragment |
| `lint-timers.sh:188-202` | NET (`brace_depth > old_d && brace_depth >= 2` :202; header :190-193 only notes the annotation-line net-0 case, not the one-liner-METHOD net-0 case) | one-liner FN | replace with the fragment |
| `lint-silent-protection.sh:302-364` | NET (`brace_depth > old_depth && brace_depth >= 2` :326; `m_depth_at_open` :359) | one-liner FN | replace with the fragment |
**Invariants the fragment MUST carry (B832):** (1) `brace_depth >= 2` guard; (2) Case-B backward scan stops at any line
starting with `@` (the boundary that makes the single-vs-multi-line BMisparse pin bite); (3) BOTH keyword-exclusion lists,
byte-identical; (4) peak-depth (`max_d`) method-open; (5) a one-line getter/setter skip (B832-G1).
**Golden-set bats — QA RED `qa/c11-golden-parser` `ed2088f` (7 cases, one cross-lint contract for the shared parser):** one Java tree the fragment parses; assert all three lints agree. Fixtures
MUST include: BMisparse multi-line · `anyNoHardware` same-method local · CP-1 adapter · **the one-liner method** (§5.4 — the
ONLY case the three copies disagree on today, so without it the fragment silently inherits the author's copy). Also close
B832-G2/D3 in this lane: `lint-silent-protection` Case-B scans the RAW line with a `//`-only strip — add `/* */` stripping.
**PR order:** T1 lands the fragment (canonicalised from ext-writable's peak-depth) + migrates all three; a per-copy golden
diff (before/after counts identical on the shared golden tree EXCEPT the one-liner, which flips 0→1 for timers/silent).

## T2 — `tests/lib/client-root.bash` (centralise the C9_CLIENT_ROOT default)
**QA RED = `qa/c11-client-root` `54078f6` (path-based, WIDENED from 011d127)** to flag ANY absolute `Leon-Guanjuato` literal in
`tests/*.bats` outside `tests/lib/client-root.bash` — **10 offenders on dab0807 → 0**. Two pins: **C11-T2-lib-exists** and
**C11-T2-no-hardcode**. **Lib contract:** `tests/lib/client-root.bash` owns a single `CLIENT_READ_ROOT` default, exported as
`C9_CLIENT_ROOT`, `C9_CLIENT_REPO` AND `C8_CLIENT_REPO`; an env override of any wins; ONE place to retarget.
**Tree decision (lead): ONE default = `main-ff1b659` for all 10; NO second default in the lib.** The three live-checkout
reads (`c8-close`, `lint-delays`, `rc-scan` on `…/Cliente/Leon-Guanjuato`) are a client-reads-rule VIOLATION today: that
working copy is at **`4f5f1c7` with 4 uncommitted files** (VERIFIED read-only — the exact stale tree the C9 stale-checkout
lesson warns against, several campaigns behind `ff1b659`), so those three smokes read an outdated tree right now.
Retargeting them to the frozen `ff1b659` worktree is the fix; QA re-measures `c8-close`/`lint-delays`/`rc-scan` on `ff1b659`
and pins the new numbers. Never write to that checkout.

**QA re-measure on ff1b659 (`54078f6`) — the retarget CHANGES two pins (VERIFIED by me):** `lint-delays` LD5 and `c8-close`
SC1-smoke asserted `exit 1 + FAIL BDefrostController` — that is true ONLY on the stale `4f5f1c7` checkout (the defrost
time≤0 bug, `BDefrostController.java:556/566/620/664`, fixed post-C9). On `ff1b659` **`lint-delays` is CLEAN (exit 0)**
(reproduced: PASS rows on `BEvaporatorUnit` only). So T2 flips LD5 to **exit 0**, and `lint-delays` MUST gain a SYNTHETIC
fixture that pins the delay-floor RULE (a crafted zero-floor source), because the real tree no longer exhibits it. `RC8`
(`rc-scan.bats` :701 host literal) is 1 FAIL on BOTH trees — no delta, no change.

**C11 close-lesson seed:** *a real-tree smoke that asserts a FAIL pins a BUG, not a RULE, and rots silently when the bug is
fixed; synthetic fixtures pin RULES, real-tree smokes pin the CURRENT STATE.* (LD5/SC1-smoke pinned the defrost time≤0 bug on
4f5f1c7 and would have silently passed-by-failing once the tree moved to a fixed one.) Fold into the C11 close-process
meta-lessons.

**Where:** NEW `tests/lib/client-root.bash` exporting ONE blessed-worktree default and unifying the TWO var names
(`C9_CLIENT_ROOT` and `C9_CLIENT_REPO` both hold the same `…/Leon-Guanjuato-worktrees/main-ff1b659` value today):
`: "${CLIENT_READ_ROOT:=…/main-ff1b659}"; C9_CLIENT_ROOT="$CLIENT_READ_ROOT"; C9_CLIENT_REPO="$CLIENT_READ_ROOT"` (keep both
downstream names; only the source of the default moves). Each bats `load lib/client-root`.
**Scope @ dab0807 (VERIFIED — 10 offenders, one per file):**
- 5 blessed via `C9_CLIENT_ROOT:-…/main-ff1b659`: `ext-writable-shape.bats:26` · `demand-in-scope.bats:27` · `lint-silent-protection.bats:30` · `lint-timers.bats:418` · `lint-write-path.bats:338`
- 2 blessed via `C9_CLIENT_REPO:-…/main-ff1b659`: `c9-close.bats:108` · `c10-close.bats:90`
- 3 LIVE-checkout: `c8-close.bats:107` (`C8_CLIENT_REPO:-…/Leon-Guanjuato`, has an override) · `lint-delays.bats:53` (`$HOME/…/Leon-Guanjuato/…/ColdRoomPan-rt/src`, NO override) · `rc-scan.bats:75` (`…/DashboardPan-ux`, NO override — add the env-override form)
Motivating incident: PR7 `b6b65a2` hand-retargeted 3 of the ROOT holders a109249→ff1b659 at the C10 close — the churn T2 removes. All 10 `load lib/client-root`; `lint-delays:53`/`rc-scan:75` gain the env-override form they lack today. After: a moved read tree edits ONE file.
**Pin (`tests/client-root.bats`):** the default resolves from the helper; an override via the env var still wins.

## T3 — concept-row-drift (the inverse of the S25 STALE pass)
**Rule:** a `[concept]` matrix row whose backtick-inner slot name IS in the covered set (source `@NiagaraProperty`/
`@NiagaraAction` ∪ `--bog`) is a STALE MARKER — the exemption outlived its reason (the slot became real). Flag it.
**Seam (`lint-write-path.sh` @ dab0807):** the STALE machinery is already there — `STALE=0` :35, the matrix-root covered-set
harvest :144-149, and the per-row STALE pass at **:422-458** (`case "$_row" in *'[concept]'*) continue` skips marked rows :441; covered check `case "$_covered_flat" in *" $_name "*) continue` :450). T3 is the INVERSE branch in the SAME per-row loop: for a row that DOES carry `[concept]`, if its slot name ∈ the covered set → emit the stale-marker row (today `[concept]` rows are simply `continue`d).
**Row grammar (STATUS-first, matching S25):** `STALE-MARKER  lint-write-path  <matrix>:<line>  slot <name>: [concept] on a slot now present in source — remove the marker`.
**Exit contract:** advisory like STALE — default exit 0, `--strict` promotes to 1 (reuse the `STALE` flag or add `SMARK`);
exit 3 usage unchanged; the hard uncovered-FAIL is untouched.
**Pin:** a matrix with a `[concept]` row for a now-real slot → the marker row + exit 0 (1 under `--strict`); a `[concept]`
row for a genuine concept (freezeEnabled/hoaMode/inhibit) → no marker.

## T4 — unpinned-guard meta-check (close lesson 7)
**What it scans:** each `toolbelt/lint-*.sh` header for its named OBSERVED mutations / guards (the "NAMED MUTATION" /
"guard" lines) and checks each names a bats fixture/test that a mutation would flip. A guard no fixture exercises is
unpinnable (C10 had three: PR1 depth guard, the never-reached S21 single-line misparse, PR3 Pattern-B AND).
**Where:** a NEW `toolbelt/lint-guard-pins.sh <toolbelt-dir> <tests-dir>` (meta-lint, WSL). Harvest each lint's header
mutation clauses; for each, require a matching `@test`/fixture token in the sibling bats. **Exit:** 0 all guards pinned /
1 any unpinned under `--strict` / 3 usage. WARN-only by default (a guard may be legitimately structural).
**Pin:** a lint whose header names a mutation with no bats fixture → WARN; add the fixture → clean.

## Sequencing / risk
T1 first (keystone; a live FN). T3 depends on the shipped S25 STALE pass. T2/T4 independent. T1's golden set MUST include
the one-liner or the extraction silently inherits a copy's behaviour (B832). T4 is WARN-advisory (do not block on structural guards).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | ext-writable peak (:132-176), timers net (:202), silent net (:326) | [CERT] | git show @ dab0807 |
| 2 | RED widened to any absolute Leon-Guanjuato in tests/*.bats = 10 offenders (7 blessed + 3 live-checkout); lib owns C9_CLIENT_ROOT+REPO+C8_CLIENT_REPO; 3 tail retarget live→blessed (verify smokes) | [CERT] | grep Leon-Guanjuato tests/*.bats @ dab0807 = 10 |
| 3 | lint-write-path STALE machinery (STALE :35, harvest :144-149) — T3 is its inverse | [CERT] | git show @ dab0807 |
| 4 | one-liner FN reproduced; peak-depth is the standard | [CERT, reproduced] | B832; my run (0 vs 1 companion-flag) |
