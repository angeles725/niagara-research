<!-- review-status: pending -->
<!-- kit-retro -->
# Campaign 8 close: meta-lessons from the close process (DRAFT for the kit close retro)

Date: 2026-09-05 · Module: kit · SDD: build-n4-module-campaign8 (PRs #63–#70)

> Drafted by companero for the campaign-8 close, modeled on `retros/2026-09-05-campaign7-close-process-meta-lessons.md`.
> Campaign 8 ran as tool PRs on a stacked auto-chain: lint-delays (PR1), triage-console (PR2), lint-timers-ext (PR3),
> facets-lint (PR4), slot-per-slot (PR5), rc-scan (PR6), + doctrine/close (PR7/PR8) → GitHub #63–#70. Each lesson has
> its evidence, a proposed kit delta, and a target §. `[INFER]` marks a lead-observed process event with no retro record.

## Meta-lessons

1. **Fixture-green is NOT real-green — three workers reported "done" on green fixtures while the tool still
   mis-fired on real modules.** The lead re-ran each new tool on ColdRoomPan/CompPan/DashboardPan/chihuahua and
   found defects the fixtures could not surface: PR5 (slot-per-slot) STALE inflated by counting non-OPERATOR lexicon
   keys (ColdRoomPan 28→6, CompPan 35→1) and by omitting type-display-name / `@Range` tag keys (SP5, SP6 pins), and
   PR6 (rc-scan) the `h:/` narrowing that missed every `h:<hex>` handle literal (RC10). Every lead-found defect was
   locked in with a RED pin proven on a `mktemp` PRE-FIX copy. **Delta:** a new lint/check is not "done" on a green
   fixture — the close gate requires a REAL-MODULE smoke on all four client modules, and every lead-found defect gets
   a mktemp-pre-fix RED pin. **Target:** `BUILD-LOOP.md` §5 (pre-gate) + §7 (close). `[ev: retro campaign8-slot-per-slot]` `[ev: retro campaign8-rc-scan]`
2. **Parallel workers in separate worktrees conflict on the SAME four files every PR.** Every tool PR touched the
   BUILD-LOOP routing line, the SKILL toolbelt line, the `retros/INDEX.md` row, and the `BUILD-STATE.md` envelope —
   a mechanical conflict resolved by FRAGMENT merge (keep BOTH rows, regenerate nothing). **Delta:** document the
   four always-conflict files + the fragment-merge rule (append, keep both rows, never overwrite) as the standing
   multi-session merge protocol. **Target:** `METHODOLOGY.md` §Multi-session coordination (extends K12). `[ev: retro campaign8-slot-per-slot]` [INFER — the four-file set is lead-observed across PRs]
3. **The SDD ledger budget blocks after every merged base (the merge is charged to the attempt).** Each PR's ledger
   had to be reset with delegated authority before the next attempt. **Delta:** a close-process note — reset the
   attempt budget per PR under delegated authority; this is a gentle-SDD ledger property, not a kit-file bug.
   **Target:** the ORCHESTRATION.md handoff note (PR17). `[INFER — lead-observed, no retro record]`
4. **The `/tmp` session scratchpad was WIPED mid-campaign, losing audit notes.** Every draft now goes to a repo path
   (`sources/probes/`) — this is WHY `sources/probes/` exists as the draft sink now. **Delta:** draft artifacts (PR
   bodies, audit notes, fold drafts) land in a repo, never only in `/tmp`. **Target:** `METHODOLOGY.md` §Multi-session
   / a process rule; ties to C9 seed S17. `[ev: corpus S17 (C9 seed a5a2e5cba)]` [INFER — the wipe is lead/companero-observed this session]
5. **A GitHub outage hit mid-merge.** Recovery: fast-forward from the remote-TRACKING ref (`origin/main`) and push
   later when the remote returned — never block the chain on the outage. **Delta:** a resilience note in the deploy/
   merge flow — ff from the tracking ref, defer the push. **Target:** `BUILD-LOOP.md` §6/§7 note. `[INFER — lead-observed]`
6. **`TaskOutput` on a worker dumps its whole transcript into the lead's context.** Peek at a worker's state with
   `git log`/`git status`/`git diff` on its branch instead of pulling the transcript. **Delta:** an orchestration
   note — inspect a worker via git, not TaskOutput, to protect the lead's context budget. **Target:** ORCHESTRATION.md
   (PR17) / delegation note. `[INFER — lead-observed]`
7. **Worker retro named-mutation tables that say "would flip" are not evidence — demand OBSERVED flips.** A mutation
   is proven only by a verbatim before/after (RED then GREEN) on a real copy, not by a prose "this would flip"
   claim. **Delta:** a retro's named-mutation table must record the OBSERVED flip (verbatim output), reinforcing the
   mutation-provability rule (campaign7-close L3). **Target:** `METHODOLOGY.md` §Kit maintenance. `[ev: retro campaign8-lint-timers-ext]` (Lesson 2: the authoritative demo is the bats fixture, not prose)
8. **Design estimates are hypotheses; the retro records the REAL counts.** Real smokes disagreed with every design
   estimate: facets-lint 12→**25** WARN (`campaign8-facets-lint`:114), slot-per-slot 19→**9** MISSING
   (`campaign8-slot-per-slot`:63), rc-scan null-branch `:863`→**`:852/:853`** (`campaign8-rc-scan` D2). **Delta:**
   this REINFORCES campaign7-close L1 ("pin the REAL output, not the design's prose estimate") with campaign-8
   evidence — pointer + new counts, NOT a new rule (K6: the rule is already folded). **Target:** `METHODOLOGY.md`
   §Kit maintenance (pointer to the existing rule). `[ev: retro campaign8-facets-lint]` `[ev: retro campaign8-slot-per-slot]` `[ev: retro campaign8-rc-scan]`
9. **AI-attribution trailers keep reappearing in commits.** K11 (grep for `co-authored|generated with|claude`) is
   advisory only; the durable fix is a `commit-msg` git hook that REJECTS them. **Delta:** promote K11 from an
   advisory grep to a machine-enforced `commit-msg` hook (C9 seed S9). **Target:** C9 S9 (`toolbelt` + `.githooks`);
   `METHODOLOGY.md` K11 gains a pointer once S9 ships. `[ev: corpus S9 (C9 seed a5a2e5cba)]` [INFER — recurrence is lead/companero-observed]

## Proposed kit deltas (summary → target §)
| # | Delta | Target § | Token |
|---|---|---|---|
| 1 | real-module smoke on all 4 modules + mktemp-pre-fix RED pin per lead-found defect | `BUILD-LOOP.md` §5/§7 | `[ev: retro campaign8-slot-per-slot]` `[ev: retro campaign8-rc-scan]` |
| 2 | four-file fragment-merge protocol | `METHODOLOGY.md` §Multi-session (K12) | `[ev: retro campaign8-slot-per-slot]` |
| 3 | ledger-budget reset per PR | ORCHESTRATION.md (PR17) | `[INFER]` |
| 4 | drafts to a repo not /tmp | §Multi-session / S17 | `[ev: corpus S17]` |
| 5 | ff from tracking ref on remote outage | `BUILD-LOOP.md` §6/§7 | `[INFER]` |
| 6 | inspect worker via git not TaskOutput | ORCHESTRATION.md | `[INFER]` |
| 7 | mutation tables record OBSERVED flips | `METHODOLOGY.md` §Kit maintenance | `[ev: retro campaign8-lint-timers-ext]` |
| 8 | real counts over design estimates (reinforces C7-close L1) | `METHODOLOGY.md` §Kit maintenance | `[ev: retro campaign8-facets-lint]` |
| 9 | K11 → commit-msg hook | C9 S9 | `[ev: corpus S9]` |

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | slot-per-slot STALE + rc-scan h:/ defects were fixture-green but real-red, caught in lead review | [CERT] | `campaign8-slot-per-slot`:102/105 (SP5/SP6), `campaign8-rc-scan`:53 (RC10) |
| 2 | Real counts ≠ design estimates (facets 12→25, slot 19→9, rc :863→:852/853) | [CERT] | `campaign8-facets-lint`:114, `campaign8-slot-per-slot`:63, `campaign8-rc-scan` D2 |
| 3 | Lessons 3/5/6 are process events with no retro record | [INFER] | lead-observed this campaign |
| 4 | Lessons 4/9 tie to C9 seeds S17/S9 | [CERT] | `campaign9-research-candidates.md` a5a2e5cba |

**Tally:** [CERT] ×3 · [INFER] on lessons 3/5/6 (+ partial on 2/4/9), all honestly marked. Nothing invented; PR
range #63–#70 and the pin IDs (SP5/SP6/RC10) verified against the campaign-8 retros.
