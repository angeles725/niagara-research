# Campaign 8 CLOSE — apply package (ONE close PR on niagara-tools, execute-only)

Author: companero (Opus), 2026-09-06. Purpose: the ordered, exact edit list so the close worker EXECUTES without
re-deriving. Source of the full fold-line text: `2026-09-05-c8-close-fold-drafts.md` §1–§20 (this file quotes the
5 mandatory lines verbatim and points at the drafts for the recommended ones). CHANGELOG text:
`2026-09-06-c8-close-changelog-draft.md` (488f8a675). Close retro: `2026-09-05-c8-close-retro-draft.md` (7418165ba).

> **RE-CHECK AT APPLY:** §18 (`feat/c8-structure` `6742c76`, PR #82), §19 (`feat/c8-write-path` `b044cfb`, fix round in
> progress), §20 (`feat/c8-station-logic` `5e21f0e`, fix round in progress — its pins are presence-only per close-retro
> lesson 11(a); counts/semantics WILL move). Before editing, `git -C <niagara-tools> fetch` and re-grep every token count
> + re-read §18/§19/§20 merged retros. Do NOT trust the branch-tip numbers below for §19/§20 without re-reading.

---

## Order of operations (do NOT reorder — mandatory token lines land BEFORE any INDEX flip)

### Step A — the FIVE mandatory fold lines (0-token retros; land these first or `sweep-fold-audit --strict` fails)

Each is a `folded as code:` / promotion line. Target = `build-n4-module-kit/METHODOLOGY.md`; anchor = the §Conformance
"folded as code:" list (after the existing `triage-console.sh` line at ~:96), except §7/§15 which go in §Kit maintenance.

**A.1 — §3 lint-timers-ext** → `METHODOLOGY.md` §Conformance (after :93), and confirm `lint-timers.sh` is named in
BUILD-LOOP §5 + SKILL:
```
- folded as code: toolbelt/lint-timers.sh extensions (companion-flag: a flag set in the SAME method body as Clock.schedule* must be cleared in stopped()/started(), not on the expiry path only; jdk-thread: a BComponent using ScheduledExecutorService/Executors.*/new Thread( → FAIL; changed-sched: Clock.schedule* reachable from changed()/started() without an isRunning()/atSteadyState() guard in the scheduling body → FAIL). [ev: corpus B801] [ev: corpus B812] [ev: corpus B800 §800.3] [ev: corpus B816] [ev: retro campaign8-lint-timers-ext]
```

**A.2 — §7 campaign8-doctrine-fold** (PROMOTION) → `METHODOLOGY.md` §Kit maintenance:
```
- The campaign-8 R7.1–R7.10 corpus doctrine (8-layer timer index, non-positive-delay floor, inter-module comms, critical-write step-up, cert-chain trust, mandatory schema-risk gate, Excavador Técnico profile, K21, station load budget) was PROMOTED into the core in PR7 (doc-only). [ev: retro campaign8-doctrine-fold]
```

**A.3 — §13 post-deploy-checklist** → `METHODOLOGY.md` §Conformance (or add the token to the `BUILD-LOOP.md §6.a` line):
```
- folded as code: BUILD-LOOP.md §6.a post-deploy verification (ordered steps 1–4: pre-snapshot → hot-reload → console triage → schema-risk/bog-audit re-run, gated on CHECK11; the five step scripts hard-pinned in tests/kit-links.bats L7). [ev: retro campaign8-post-deploy-checklist]
```

**A.4 — §14 build-pipeline** → `METHODOLOGY.md` §Conformance:
```
- folded as code: BUILD-LOOP.md §4.a Gradle task matrix + §4.b vendorVersion/bajaVersion version-bump checklist (with the exit-31 station-lock BS-lock/BS-lock-hint regression pins in tests/build-sh.bats). [ev: corpus B807] [ev: corpus B795] [ev: retro campaign8-build-pipeline]
```

**A.5 — §15 campaign8-rt-doctrine** (PROMOTION) → `METHODOLOGY.md` §Kit maintenance:
```
- The campaign-8 RT-control doctrine (§RT-control-logic, history-ext authoring, and the "Slot types for externally written values" table — B805/B808/B804/B823/B822/B825/B826/B828/B816) was PROMOTED into the types/ core in PR15 (doc-only). [ev: retro campaign8-rt-doctrine]
```

### Step B — the recommended folded-as-code lines (convention completeness)

Paste each from the close-fold drafts verbatim (target `METHODOLOGY.md` §Conformance). Full text:
`2026-09-05-c8-close-fold-drafts.md` §9-§12, §16-§20.

- **Genuinely token-credited (non-blocking — skip only if the close PR is kept minimal):** §9 station-snapshot, §10
  bog-audit, §11 wb-audit, §12 lint-servlet, §16 retro-loop, §17 orchestration (+ its L7/L8 numbering doctrine line),
  §18 structure. Each has ≥1 core token TODAY (verified at `6742c76`: §18=4).
- **⚠ MERGE-DEPENDENT — §19 write-path and §20 station-logic are 0-token in the kit core UNTIL their PRs merge**
  (verified at `6742c76`: both 0; `lint-write-path.sh` absent). Their `[ev: retro campaign8-write-path]` /
  `[ev: retro campaign8-station-logic]` tokens ride into `BUILD-LOOP.md`/`skill/SKILL.md` via the K19 routing lines that
  land WITH PR19/PR20. Since the close PR is the LAST one (after both merge), they will be credited by then — BUT do NOT
  assume it. **GATE (required):** on the post-PR20 main, re-run `grep -rn 'ev: retro campaign8-write-path' <kit-root>`
  and the same for `campaign8-station-logic` (excl retros/); **if either is still 0, PROMOTE that retro's fold line to
  Step A tier (land it BEFORE its INDEX flip)** — otherwise `sweep-fold-audit --strict` FAILs on a folded row with no
  core token. (Alternative if PR19/PR20 slip past the close: flip only the present rows and defer the §19/§20 flips to
  their own merge PRs.)

### Step C — INDEX.md review-status flips (all 20 rows `pending → folded`)

Target `build-n4-module-kit/retros/INDEX.md`. Change the 4th column `pending → folded` on each of these 20 rows (row
texts from `cc428e5` for the 15 merged + the branch tips for §16–§20 — RE-CONFIRM exact text at apply). MANDATORY: do
A.1–A.5 first, else the fold-audit fails on those five rows.
```
| 2026-09-05-campaign8-lint-delays.md          | kit | … | folded | 4  |
| 2026-09-05-campaign8-triage-console.md        | kit | … | folded | 5  |
| 2026-09-05-campaign8-lint-timers-ext.md       | kit | … | folded | 3  |   ← needs A.1 first
| 2026-09-05-campaign8-facets-lint.md           | kit | … | folded | 5  |
| 2026-09-05-campaign8-slot-per-slot.md         | kit | … | folded | 4  |
| 2026-09-05-campaign8-rc-scan.md               | kit | … | folded | 5  |
| 2026-09-05-campaign8-doctrine-fold.md         | kit | … | folded | 10 |   ← needs A.2 first (promotion)
| 2026-09-05-campaign8-report-integration.md    | kit | … | folded | 4  |
| 2026-09-05-campaign8-station-snapshot.md      | kit | … | folded | 4  |
| 2026-09-05-campaign8-wb-audit.md              | kit | … | folded | 5  |
| 2026-09-05-campaign8-bog-audit.md             | kit | … | folded | 6  |
| 2026-09-05-campaign8-lint-servlet.md          | kit | … | folded | 5  |
| 2026-09-05-campaign8-post-deploy-checklist.md | kit | … | folded | 2  |   ← needs A.3 first
| 2026-09-05-campaign8-build-pipeline.md        | kit | … | folded | 3  |   ← needs A.4 first
| 2026-09-05-campaign8-rt-doctrine.md           | kit | … | folded | 4  |   ← needs A.5 first (promotion)
| 2026-09-06-campaign8-retro-loop.md            | kit | … | folded | 5  |
| 2026-09-06-campaign8-orchestration.md         | kit | … | folded | 3  |
| 2026-09-06-campaign8-structure.md             | kit | … | folded | 3  |   ← RE-CHECK (token-credited, 4)
| 2026-09-06-campaign8-write-path.md            | kit | … | folded | 5  |   ← RE-CHECK + Step-B GATE (0-token until PR19 merges)
| 2026-09-06-campaign8-station-logic.md         | kit | … | folded | 4  |   ← RE-CHECK + Step-B GATE (0-token until PR20 merges)
```
> **Do not flip the §19/§20 rows until their tokens are on main** (Step B GATE). A folded row with no core token fails
> `sweep-fold-audit --strict`. Either their PR-merge routing lines are present (credited), or their fold line was
> promoted to Step A first.

### Step D — the CHANGELOG v0.19.0 extension

Apply `2026-09-06-c8-close-changelog-draft.md` (488f8a675): move the wave-2 (`### Added`/`### Changed`) blocks INTO the
existing `## [v0.19.0]` section (append after the wave-1 `### Added`), and merge the two `### References` lists into ONE.
Fill the PR16–PR20 placeholder entries with the SHIPPED tool's real flags/exits/bats/smoke counts (real counts over
design estimates — C7-close L1). Leave `VERSION` at `0.19.0`. NO 0.19.1 bump (reasoning in the CHANGELOG draft).

### Step E — the close retro, folded in the SAME PR (so pending ends at 0)

Copy `2026-09-05-c8-close-retro-draft.md` (7418165ba) into the kit as
`build-n4-module-kit/retros/2026-09-06-campaign8-close-process-meta-lessons.md` (rename to the `campaignN-close-process-
meta-lessons` convention, matching campaign-7). It is itself a retro → it needs its OWN INDEX row AND a fold token, or
`retro_pending` cannot reach 0. Add its INDEX row already `folded`, and add its fold/promotion line to `METHODOLOGY.md`
§Kit maintenance:
```
INDEX row (add, already folded):
| 2026-09-06-campaign8-close-process-meta-lessons.md | kit | 2026-09-06 | folded | 11 |

METHODOLOGY §Kit maintenance fold line:
- The campaign-8 close process meta-lessons (11 lessons: fixture-green≠real-green, four-file merge, ledger budget, /tmp wipe, GitHub outage ff-from-tracking, git-not-TaskOutput, OBSERVED-flip mutation tables, real-counts-over-estimates, K11→commit-msg hook, ff-verify-before-settle + rebase parallel workers, and presence-pin/one-lint-convention) were folded; the actionable deltas land as BUILD-LOOP §5/§7 + METHODOLOGY K22 and C9 seeds S9/S17. [ev: retro campaign8-close-process-meta-lessons]
```

### Step F — the BUILD-STATE kit envelope flip (section-scoped)

Target `build-n4-module-kit/BUILD-STATE.md`, the `kit` `<!-- build-state.v1 -->` envelope: set `retro_pending: false`
(if not already) AND the summary-table `kit` row `retro_pending` cell to `no` — they MUST agree. Flip the named `kit`
section ONLY (awk section-scoped, per campaign8-retro-loop RL7 — never a global `sed` that flips sibling module
sections). Update `last_session:` to the campaign-8 close line.

### Step G — guards (all must be clean before commit) + commit message

```
bash toolbelt/sweep-build-state.sh BUILD-STATE.md retros/ retros/INDEX.md      # pending count → 0
bash toolbelt/sweep-fold-audit.sh --strict retros/INDEX.md .                   # every folded row has a token
bats tests/                                                                    # full suite green (incl kit-links L7/L8)
```
Commit message. The `Retro:` trailer MUST be canonical per BUILD-LOOP §7(c): `Retro: promotion (folds <bare id list>)`
— a space-separated id list ONLY, no `;` clauses and no `{…}` ellipsis (the real niagara-tools example is
`Retro: promotion (folds corpus B804 B805 B808 …)`). Put all the description in the commit BODY, not the trailer.
```
docs(build-n4-module): campaign 8 close — fold 20 retros, extend CHANGELOG v0.19.0, retro_pending=0

Folds all 20 campaign-8 retros + the close-process-meta-lessons retro into the kit:
5 mandatory 0-token fold lines (A.1-A.5) landed before their INDEX flips; 20 INDEX
rows pending->folded (+ the close retro's own folded row); BUILD-STATE kit
retro_pending=false (section-scoped). Guards clean: sweep-build-state pending=0,
sweep-fold-audit --strict, bats, kit-links.

Retro: promotion (folds campaign8-lint-delays campaign8-triage-console campaign8-lint-timers-ext campaign8-facets-lint campaign8-slot-per-slot campaign8-rc-scan campaign8-doctrine-fold campaign8-report-integration campaign8-station-snapshot campaign8-wb-audit campaign8-bog-audit campaign8-lint-servlet campaign8-post-deploy-checklist campaign8-build-pipeline campaign8-rt-doctrine campaign8-retro-loop campaign8-orchestration campaign8-structure campaign8-write-path campaign8-station-logic campaign8-close-process-meta-lessons)
```
NO AI-attribution trailer (K11 / CONTRIBUTING.md).

### Step H — post-merge, LEAD-owned (not the close worker)

1. `git tag v0.19.0 <final-merged-commit>` && `git push origin v0.19.0` (tasks.md C.5 — no bump, wave 1 already carried
   0.19.0 into VERSION).
2. Re-install the skill from the tagged kit (launcher `install-skill`) so `/build-n4-module` points at the closed kit.
3. `sdd-archive` the `build-n4-module-campaign8` OpenSpec change (moves it to `openspec/changes/archive/…`).

---

## Verify table
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 5 retros are 0-token → mandatory lines A.1–A.5 must precede the INDEX flip | [CERT] | grep-before at cc428e5/branch tips (fold-drafts §3/§7/§13/§14/§15) |
| 2 | 13 of the other 15 are token-credited (≥1 in-kit); §19 write-path + §20 station-logic are 0-token until PR19/PR20 merge their K19 routing lines → Step B GATE | [CERT] | git grep per slug at 6742c76 (§18=4; §19=0, §20=0, lint-write-path.sh absent) |
| 3 | §18/§19/§20 re-check — §19/§20 fix rounds in progress; §20 pins presence-only | [INFER] | lead-reported; close-retro lesson 11 |
| 4 | close = v0.19.0 tag, no bump | [CERT] | tasks.md C.3/C.5; VERSION=0.19.0 |
| 5 | close retro must be folded in the same PR to reach pending=0 | [CERT] | retro-enforcement gate (BUILD-STATE retro_pending) |

**Open at apply:** the §19/§20 merged retro deltas + token counts (fix rounds), the exact INDEX row texts as branches
merge, and the PR18–PR20 CHANGELOG real counts. Everything else is settled.
