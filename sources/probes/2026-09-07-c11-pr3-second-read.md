# C11 PR3 (T2 client-root centralisation) second read — feat/c11-client-root-lib 54ee6d6

investigador1, 2026-09-07. T2 mechanism checks + the SC1-smoke vacuity the lead flagged, verified in a worktree. `[ev: git 54ee6d6; reproduced bats]`

## Verdict: PASS on the T2 mechanism. ONE finding (confirms the lead's flag): c8-close SC1-smoke is still vacuous and, post-retarget, must pin exit 0 on ff1b659 (like LD5). QA is tightening — must land before merge.

## T2 mechanism — all PASS
- **`tests/lib/client-root.bash`** matches design D3a exactly: `: "${CLIENT_READ_ROOT:=…/main-ff1b659}"` then
  `C9_CLIENT_ROOT`/`C9_CLIENT_REPO`/`C8_CLIENT_REPO` `:=$CLIENT_READ_ROOT`, all exported. `:=` = env override wins,
  unset/empty assigns — correct. `[ev: 54ee6d6:tests/lib/client-root.bash]`
- **10 sites converted**: all ten bats carry `load lib/client-root` (file scope, kit idiom); 0 raw Leon-Guanjuato literals
  in `tests/*.bats` outside the checker. `[ev: grep @ 54ee6d6]`
- **no-hardcode test**: `C11-T2-no-hardcode` greps `tests/*.bats` for `/Cliente/Leon-Guanjuato`, excludes `$SELF`
  (client-root-single-source.bats) and the lib (a `.bash` under `lib/`, not matched by `*.bats`); passes with **all three
  vars unset** (C11-T2-lib-exists + C11-T2-no-hardcode green). `[ev: bats run, env -u]`
- **LD5 retargeted correctly (pins-only)**: `[ "$status" -eq 0 ]` + `[[ "$output" != *"FAIL"* ]]` — the ff1b659 CLEAN
  state (defrost time<=0 fixed post-C9). LD1/LD3/LD6 carry the delay-floor RULE (synthetic FAILs). `[ev: lint-delays.bats:54-66; runs]`
- **RC8 unchanged**: DashboardPan-ux rc FAILs on the host literal (:701), 1 FAIL. `[ev: rc-scan RC8 run]`
- **K12**: no `toolbelt/*.sh` touched (tests only). **0 trailers**; **no new conflict markers**. `[ev: diff --name-only; git grep]`

## Finding — c8-close SC1-smoke is still VACUOUS and must pin exit 0 post-retarget (confirms the lead's flag)
`tests/c8-close.bats:107` SC1-smoke asserts `[ "$status" -eq 1 ] || [ "$status" -eq 0 ]` — accepts BOTH exits, so it can
**never fail** for a runnable tree (any verdict in {0,1} passes). Worse after the T2 retarget: its `CRP` now resolves from
`$C8_CLIENT_REPO` = the lib default `main-ff1b659` (D3c site 8) — the SAME fixed tree LD5 reads, where `lint-delays` is
**deterministically exit 0**. Its title still says "exits 1 on the pre-fix tree, 0 on the fixed tree", but the pre-fix
(4f5f1c7) branch is now dead — it always reads ff1b659. Per **R-T2.10** (which PR3's own spec enforces), a real-tree
smoke must PIN the current-tree verdict; SC1-smoke must become `[ "$status" -eq 0 ]` + `BDefrostController` absent —
identical to LD5's retarget. This is the exact LD5-class defect (a real-tree smoke that doesn't pin) reappearing in
c8-close. Confirmed STILL vacuous in 54ee6d6, so QA's tightening must land before merge. `[ev: c8-close.bats:107-115; LD5 contrast]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | client-root.bash matches D3a; := override semantics correct | [CERT] | 54ee6d6:lib/client-root.bash |
| 2 | 10 sites load the lib; 0 raw literals outside the checker; passes with vars unset | [CERT] | grep + bats env -u |
| 3 | LD5 pins exit 0 + no-FAIL (ff1b659 clean); LD1/LD3/LD6 carry the rule; RC8 1 FAIL | [CERT-live] | bats runs |
| 4 | K12 (no toolbelt); 0 trailers; no new conflict markers | [CERT] | diff + git grep |
| 5 | SC1-smoke `1 || 0` vacuous; CRP now ff1b659; must pin exit 0 like LD5 | [CERT] | c8-close.bats:107-115 |
Tally: 4 [CERT] · 1 [CERT-live] · 0 [INFER] · 0 unmarked.
