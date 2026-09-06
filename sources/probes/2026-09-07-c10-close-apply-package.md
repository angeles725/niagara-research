# C10 close — apply package (kit `chore/c10-close`, target v0.21.0) — mirrors the C9 PR13 FINAL package

Author: companero (Fable), 2026-09-06. EXECUTE-ONLY for a sonnet worker. Cut from REAL kit main **`f90b8d1`** (VERSION
`0.20.0`; no `## [Unreleased]` yet — the five C10 lint PRs open it as they merge, like C9 Wave 1). Fold targets are cited at
f90b8d1 line numbers. No Co-Authored-By / AI trailers (CONTRIBUTING §6). `[ev: kit main f90b8d1]` `[ev: client ff1b659]`

## 1. Retro drafts — one per kit PR (new-retro.sh names) with the fold target already named
Create each with `KIT=<kit> toolbelt/new-retro.sh kit <slug>` (writes the stub + INDEX row + BUILD-STATE retro_pending true).
| Slug | PR / slice | Lesson (one line) | FOLD TARGET @ f90b8d1 |
|---|---|---|---|
| `campaign10-lint-timers-scope` | S21 | companion-flag must use the shared method-boundary parser (brace_depth≥2, FIELD=depth-1), not a forward scan from the assignment; root cause = the `:147` candidate regex treating `@NiagaraProperty(` as a method sig | METHODOLOGY.md — NEW K24 (below), after K22 (:88); BUILD-LOOP.md:70 lint-timers routing bullet unchanged (no flag change) |
| `campaign10-ext-writable-per-slot` | S22 | exempt a complex OPERATOR slot only when an `@NiagaraAction` BODY writes THAT slot (B831-G1 name→doX mapping), not "class has any action" | types/logic-authoring.md:105 — refine the anti-shape line: "no writing action" → "no `@NiagaraAction` whose body writes the slot" |
| `campaign10-silent-protection-pattern-b` | S23 | the silent-protection surface follow must recognise the Pattern B adapter (`implements BIAlarmSource` + `newOffnormalAlarm`/`new AlarmSupport(`), not only Pattern A `BAlarmSourceExt`. Fold facts: SP-smoke's CompPan **1→0** comes from MODULE-WIDE surface application (`BCompressorControl` surfaces via `BAlarmRecord` too, so the whole module is surfaced); the Pattern-B token bites ONLY the S23-pos fixture; S23-and pins the AND (`implements BIAlarmSource` AND `newOffnormalAlarm`). | types/logic.md:102 (lint mention under §Protection anatomy :98) — add "the lint recognises BOTH surfaces (A: `BAlarmSourceExt` on a point; B: the B-adapter implementing `BIAlarmSource`), and the adapter→pure follow is the **`B<Pure>` naming pair ONLY** — an adapter not named `B`+`<PureClass>` is not followed" |
| `campaign10-run-pure-test-cwd` | S24 | run-pure-test.sh must resolve src from any cwd — normalise `rt=$(cd "$rt" && pwd)` (after the :30 guard) + run java in `( cd "$rt" && … )` (:62); also FIX the stale 4-arg usage doc | build-verify.md:108 — correct the `run-pure-test.sh <rt-dir> <pkg> <PureClass> <TestClass>` to the real 2-arg `<rt-dir> <pkg.TestClass>` and add "resolves src from any cwd" |
| `campaign10-write-path-stale` | S25 | lint-write-path keeps the hard uncovered-FAIL; adds a STALE advisory (matrix-root covered set of all @NiagaraProperty+@NiagaraAction names ∪ --bog; per-row; `[concept]` exemption; --strict promotes) | BUILD-LOOP.md:70 lint-write-path routing bullet — extend the exit desc: "…+ STALE advisory rows (matrix row with no source slot / --bog slot, exempt via `[concept]`); exit 0 unless --strict promotes STALE" |
### close-process meta-lessons draft (`campaign10-close-process-meta-lessons`)
Seeded from what C10 already taught (fold as NEW **METHODOLOGY K24** after K22 :88 — "verify a gate before refining it"):
1. **Verify a gate's real exit contract before proposing a flag** — S25's premise ("add --strict") was wrong: lint-write-path was ALREADY a hard exit-1 gate, so --strict would have weakened it; the fix was a new STALE class, not a flag. Read the exit behaviour first.
2. **Name-level vs per-row exemption** — a name-level exemption is a cross-row implicit exemption (one marked hoaMode row silently exempting two others); STALE is PER-ROW, each row carries its own `[concept]`.
3. **Matrix-root scope** — a coverage lint's covered set is harvested matrix-root-wide (all modules), else the count depends on which module root was passed.
4. **A design executor must read the artifact, not a peer's worktree** — cite anchors from the tree the design pins (kit main), not a session's scratch checkout.
5. **The single-line `@Niagara…name=` regex UNDER-counts** — annotations are multi-line; match the `name = "X"` field line (177 names, not 56) or the covered set is wrong and STALE over-produces.
6. **A heuristic pairing a flag with a call needs the shared method-boundary parser** (see also lesson 7).
7. **Every OBSERVED mutation named in a lead gate must name the fixture it flips, and QA must confirm that flip — RED-pre-fix / GREEN-post-fix is NOT enough for a GUARD pin.** C10 had three unpinned-guard instances: PR1's `brace_depth>=2` depth guard; PR1's S21 single-line-annotation misparse that was NEVER REACHED (the Case-B scan already breaks at a line starting with `@`, so the "single-line @NiagaraProperty" case can't misfire); PR3's Pattern-B AND. A guard that no fixture exercises is unpinnable — its mutation would not flip any test. Corollary: **a fixture shown in a note must match the SHAPE used in its proof** — a single-line code block cannot stand in for a multi-line proof (investigador1's note had a single-line block against a multi-line run).
 — S21: a forward brace-scan false-fired on a method-local; the section-D parser + FIELD=depth-1 is the reusable shape (S22/S23 reuse it).

## 2. CHANGELOG + VERSION
- Rename `## [Unreleased]` → `## [v0.21.0] - 2026-09-<dd>` (the C10 lint PRs will have populated it, like C9 Wave 1; if empty, write the block). Heading: `### Changed — Campaign 10: lint precision (S21-S25) + client hygiene (S26)`. Bullets (one per PR, `[ev: retro campaign10-<slug>]`):
  - lint-timers companion-flag → method-boundary parser (S21) [niagara-tools #89]
  - lint-ext-writable-shape per-slot action-body exemption (S22) — EW10 CompPan-rt re-pinned 0→1 (faultReset)
  - lint-silent-protection recognises Pattern B surfaces (S23) — CP-1 no longer false-WARNs
  - run-pure-test.sh cwd-independent + 2-arg usage fix (S24)
  - lint-write-path STALE advisory + `[concept]` (S25)
  - client: gitignore build cache + 5 `[concept]` matrix marks (S26, docs-only)
- `VERSION`: `0.20.0` → `0.21.0` — SAME commit as the CHANGELOG (CONTRIBUTING §5).

## 3. `tests/c10-close.bats` TODO(freeze) values QA will need
- `VERSION` == `0.21.0`; tag `v0.21.0`.
- **SC-13 client versions CARRY OVER (VERIFIED @ ff1b659):** Compresores **2.2.0**, Paccadia **2.1.0**, Dashboard **2.2.0** —
  unchanged from C9. **PR6 (S26) bumps NOTHING** — it is a docs/gitignore chore (no `build.gradle.kts` in its diff, no jar);
  confirm the PR6 diff touches only `.gitignore` + `docs/write-path-matrix.md`. So c10-close.bats pins the same three
  versions as v0.20.0's close, and asserts PR6 introduced no version delta.
- fold-audit: `sweep-fold-audit.sh --strict INDEX kit-root` → 0 uncited after the 6 INDEX flips (5 lint retros + meta-lessons).
- bats total: was 369 @test at C9 close; C10 adds the five slice REDs + c10-close — record the measured total, don't predict.

## 4. BUILD-STATE envelope delta + INDEX rows
- `BUILD-STATE.md` kit envelope: `retro_pending: true` → `false` (section-scoped, `sweep-build-state.sh` diffs the section);
  `last_commit:` → the chore/c10-close merge sha; `last_session:` → `2026-09-<dd> · Campaign 10 CLOSE v0.21.0 — lint precision S21-S25 + client hygiene S26; 6 retros folded; #89 resolved; client versions carry over 2.2.0/2.1.0/2.2.0 (PR6 no bump).`
- `retros/INDEX.md`: six rows flipped to `folded` — campaign10-lint-timers-scope, -ext-writable-per-slot,
  -silent-protection-pattern-b, -run-pure-test-cwd, -write-path-stale, -close-process-meta-lessons.

## 5. Gates + commit
```bash
export PATH=/usr/bin:/bin:$PATH; cd <kit>
C10_CLOSE=1 bats tests/c10-close.bats
toolbelt/sweep-build-state.sh
toolbelt/sweep-fold-audit.sh --strict build-n4-module-kit/retros/INDEX.md build-n4-module-kit
bats tests/
```
Commit (bare-id promotion trailer):
```
chore(c10-close): v0.21.0 — CHANGELOG+VERSION, 6 retros folded, BUILD-STATE flip

Retro: promotion (folds campaign10-lint-timers-scope campaign10-ext-writable-per-slot campaign10-silent-protection-pattern-b campaign10-run-pure-test-cwd campaign10-write-path-stale campaign10-close-process-meta-lessons)
```

## 6. Post-merge (lead)
`git tag v0.21.0 <merge-sha> && git push origin v0.21.0`; `scripts/install-skill.sh`; `sdd-archive` the C10 change; settle the
ledger; sync a niagara-research close note. (Client S26 chore rides separately on the client repo — no kit tag.)

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | kit main f90b8d1, VERSION 0.20.0, no campaign10 retros yet | [CERT] | git @ f90b8d1 |
| 2 | fold targets (BUILD-LOOP:70, logic-authoring:105, logic:98-128, build-verify:108, METHODOLOGY K22:88) | [CERT] | grep @ f90b8d1 |
| 3 | client versions carry over 2.2.0/2.1.0/2.2.0; PR6 no bump | [CERT] | build.gradle.kts:33 @ ff1b659; S26 is docs-only |
| 4 | build-verify:108 has the STALE 4-arg usage error to fix | [CERT] | build-verify.md:108 (run-pure-test 4-arg vs real 2-arg) |
