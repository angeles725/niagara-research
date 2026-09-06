# C10 PR6 (S26 client gitignore + [concept] rows) second read — niagara-panccadia-leon chore/c10-gitignore-concept-rows 00e7118

investigador1, 2026-09-07. Client repo, base ff1b659. Eight checks, all PASS, no findings. `[ev: git 00e7118; check-ignore; kit-main lint 82b60e3]`

## Verdict: clean PASS. Deploy artifacts untouched, cache untracked+ignored, 5 concept rows exact, STALE 5→0.

Two commits: 1ccc1b5 (.gitignore `**/build/tmp/` + `**/build/classes/`; untrack 51 cache files), 00e7118 (five `[concept]` rows).

## Check 1 — .gitignore does NOT cover deploy artifacts — PASS
`git check-ignore` on all 4 `build/libs/*.jar` and all 4 `build/manifest/writeModuleXml/module.xml` → NOT ignored (ok
tracked). The two patterns match only `build/classes/**` and `build/tmp/**`, which are disjoint from `build/libs/` and
`build/manifest/`. `[ev: check-ignore 00e7118]`

## Check 2 — keep-set 8/8 byte-identical — PASS
4 jars + 4 module.xml, same blob SHAs before (ff1b659) and after (00e7118); keep-set total = 8. `[ev: git ls-tree diff]`

## Check 3 — .class 43→0, build/tmp untracked — PASS
Tracked `.class`: 43 (ff1b659) → 0 (00e7118). Tracked `build/tmp/`: 0. The 51 removed = 43 `.class` (under
`build/classes/java/main/`) + 8 tmp (4 `previous-compilation-data.bin` + 4 `build/tmp/jar/MANIFEST.MF`). All now
`git check-ignore`-matched. `[ev: git ls-tree count; check-ignore]`

## Check 4 — five `[concept]` at the exact lines — PASS
`[concept]` present on matrix lines :31, :32, :33, :36, :52 (count 5); lines :40, :64, :65 clean. `[ev: grep -nF write-path-matrix.md]`

## Check 5 — STALE 5→0 with the merged kit lint — PASS
kit main lint-write-path (82b60e3, PR5 merged), CompPan-rt `--strict`: ff1b659 (no marks) → 5 STALE exit 1; 00e7118
(marked) → 0 STALE exit 0. This is the PR6 OBSERVED flip and it uses the real PR5 rule. `[ev: lint runs both trees]`

## Check 6 — no code/version change — PASS
`git diff --name-only` has no `.java`, `.kts`, `VERSION`, `vendorVersion`, or `defaultModuleVersion`. Only `.gitignore`,
`docs/write-path-matrix.md`, and the 51 removals. `[ev: diff --name-only]`

## Check 7 — 0 attribution trailers — PASS. `[ev: git log bodies]`

## Check 8 (extra) — patterns do not shadow deploy/build reads — PASS
No deploy/build script references `build/classes` or `build/tmp`. The deploy runbook (docs/deploy-runbook-2026-09-05.md),
Dashboard/deploy.sh, and other scripts read only `build/libs` (6 refs) — the tracked, non-ignored jar location — plus the
tracked `module.xml`. So ignoring `build/classes`/`build/tmp` shadows nothing the deploy path consumes. `[ev: grep deploy/build scripts]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | jars + module.xml NOT ignored (check-ignore) | [CERT] | check-ignore 00e7118 |
| 2 | keep-set 8/8 same blobs | [CERT] | ls-tree diff |
| 3 | .class 43→0; build/tmp 0 tracked; 51 removed | [CERT] | ls-tree count |
| 4 | 5 [concept] at :31/:32/:33/:36/:52; :40/:64/:65 clean | [CERT] | grep matrix |
| 5 | STALE 5→0 (ff1b659→marked) with kit main 82b60e3 | [CERT] | lint runs |
| 6 | no java/kts/version change | [CERT] | diff --name-only |
| 7 | 0 trailers | [CERT] | git log |
| 8 | deploy/build read only build/libs; classes/tmp shadow nothing | [CERT] | grep scripts |
Tally: 8 [CERT] · 0 [INFER] · 0 unmarked.
