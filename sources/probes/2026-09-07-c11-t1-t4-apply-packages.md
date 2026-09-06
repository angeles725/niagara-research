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
**Golden-set bats (`tests/method-boundary.bats`):** one Java tree the fragment parses; assert all three lints agree. Fixtures
MUST include: BMisparse multi-line · `anyNoHardware` same-method local · CP-1 adapter · **the one-liner method** (§5.4 — the
ONLY case the three copies disagree on today, so without it the fragment silently inherits the author's copy). Also close
B832-G2/D3 in this lane: `lint-silent-protection` Case-B scans the RAW line with a `//`-only strip — add `/* */` stripping.
**PR order:** T1 lands the fragment (canonicalised from ext-writable's peak-depth) + migrates all three; a per-copy golden
diff (before/after counts identical on the shared golden tree EXCEPT the one-liner, which flips 0→1 for timers/silent).

## T2 — `tests/lib/client-root.bash` (centralise the C9_CLIENT_ROOT default)
**Where:** NEW `tests/lib/client-root.bash` exporting ONE blessed-worktree default and unifying the TWO var names
(`C9_CLIENT_ROOT` and `C9_CLIENT_REPO` both hold the same `…/Leon-Guanjuato-worktrees/main-ff1b659` value today):
`: "${CLIENT_READ_ROOT:=…/main-ff1b659}"; C9_CLIENT_ROOT="$CLIENT_READ_ROOT"; C9_CLIENT_REPO="$CLIENT_READ_ROOT"` (keep both
downstream names; only the source of the default moves). Each bats `load lib/client-root`.
**Scope @ dab0807 (VERIFIED — 7 blessed-worktree defaults, NOT 5; my ROOT-only grep first missed the 2 REPO holders):**
- 5 via `C9_CLIENT_ROOT:-…/main-ff1b659`: `ext-writable-shape.bats:26` · `demand-in-scope.bats:27` · `lint-silent-protection.bats:30` · `lint-timers.bats:418` · `lint-write-path.bats:338`
- 2 via `C9_CLIENT_REPO:-…/main-ff1b659`: `c9-close.bats:108` · `c10-close.bats:90` (same default value, different var name)
Motivating incident: PR7 `b6b65a2` hand-retargeted 3 of the ROOT holders a109249→ff1b659 at the C10 close — the exact churn T2 removes.
**+3 live-main-checkout tail (worse — the LIVE `…/Leon-Guanjuato` working copy, not a blessed SHA worktree — what the client-reads rule warns against):**
`c8-close.bats:107` (`C8_CLIENT_REPO:-…/Leon-Guanjuato`, has an override) · `lint-delays.bats:53` (`$HOME/…/Leon-Guanjuato/…/ColdRoomPan-rt/src`, NO override) · `rc-scan.bats:75` (`…/DashboardPan-ux`, NO override). CONVERT the 2 override-less to the helper (they cannot be redirected at all today); flag/convert c8-close's `C8_CLIENT_REPO`. After: a moved read tree edits ONE file.
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
| 2 | 7 blessed-worktree defaults (5 C9_CLIENT_ROOT + 2 C9_CLIENT_REPO @ c9-close:108/c10-close:90) + 3 live-checkout (c8-close:107, lint-delays:53, rc-scan:75; 2 override-less) | [CERT] | grep C9_CLIENT_(ROOT\|REPO) + Leon-Guanjuato non-worktree @ dab0807 |
| 3 | lint-write-path STALE machinery (STALE :35, harvest :144-149) — T3 is its inverse | [CERT] | git show @ dab0807 |
| 4 | one-liner FN reproduced; peak-depth is the standard | [CERT, reproduced] | B832; my run (0 vs 1 companion-flag) |
