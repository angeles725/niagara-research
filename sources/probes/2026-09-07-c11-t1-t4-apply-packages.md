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
**Where:** NEW `tests/lib/client-root.bash` exporting the one default:
`: "${C9_CLIENT_ROOT:=/home/cristian/modulos_niagara_n4/Cliente/Leon-Guanjuato-worktrees/main-ff1b659}"` (keep the var NAME —
downstream smokes read it; only the default source moves). Each bats `load lib/client-root` (bats `load` sources a helper).
**Exact lines to replace @ dab0807 (the `ROOT="${C9_CLIENT_ROOT:-…}"` default assignment):**
`tests/ext-writable-shape.bats:26` · `tests/lint-silent-protection.bats:30` · `tests/lint-timers.bats:418` ·
`tests/lint-write-path.bats:338` · `tests/demand-in-scope.bats:27` (five carry the `C9_CLIENT_ROOT:-` default;
`tests/c9-close.bats` references `C9_CLIENT_ROOT` but not as a default assignment — confirm at apply). After: each bats keeps
its own `ROOT="$C9_CLIENT_ROOT"` (or drops the local default entirely) and `load`s the helper; a moved read tree edits ONE file.
**Pin (`tests/client-root.bats`):** the default resolves from the helper; an override via the env var still wins.

## T3 — concept-row-drift (the inverse of the S25 STALE pass)
**Rule:** a `[concept]` matrix row whose backtick-inner slot name IS in the covered set (source `@NiagaraProperty`/
`@NiagaraAction` ∪ `--bog`) is a STALE MARKER — the exemption outlived its reason (the slot became real). Flag it.
**Seam (`lint-write-path.sh` @ dab0807):** the STALE machinery is already there — `STALE=0` :35, the matrix-root covered-set
harvest :144-149, and the per-row STALE pass. T3 is the INVERSE branch in the SAME per-row loop: for a row that DOES carry
`[concept]`, if its slot name ∈ the covered set → emit the stale-marker row (today `[concept]` rows are simply skipped).
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
| 2 | 5 bats carry the C9_CLIENT_ROOT default (lines above); c9-close references it otherwise | [CERT] | grep tests/*.bats @ dab0807 |
| 3 | lint-write-path STALE machinery (STALE :35, harvest :144-149) — T3 is its inverse | [CERT] | git show @ dab0807 |
| 4 | one-liner FN reproduced; peak-depth is the standard | [CERT, reproduced] | B832; my run (0 vs 1 companion-flag) |
