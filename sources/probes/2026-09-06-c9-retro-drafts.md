# C9 retro drafts — skeletons for the kit `retros/` (campaign9-<slug>, ≥6-char slugs, `[ev:]` tokens)

Author: investigador1, 2026-09-06. Purpose: the lesson candidates already in hand, pre-shaped as kit retro stubs so each
PR's close writes its retro by filling counts, not by recalling. Convention: `retros/2026-09-<dd>-campaign9-<slug>.md`,
line 1 `<!-- review-status: pending -->`, lessons as PROPOSED deltas (propose-never-apply), each with an `[ev:]` token;
the close flips `pending → folded` only AFTER the core carries the slug token (C8 §19/§20 lesson). Client-PR lessons
(PR1/PR6/R14/PR8/PR9) have no kit retro of their own — they feed the close meta-lessons. `[ev: kit BUILD-LOOP §7]` `[ev: retro campaign8-close-process-meta-lessons]`

## campaign9-demand-scope (PR2)
- Δ1 **Name follows the kit convention, the RED follows the name.** The RED said `demand-in-scope.sh`, the proposal
  `lint-demand-scope.sh`; QA re-pinned the RED (d0f5942) instead of the kit bending. Rule: `toolbelt/lint-*.sh` is the
  script name; the row TOKEN may differ (`demand-in-scope`). `[ev: corpus B820 §820.3]` `[ev: RED d0f5942]`
- Δ2 **WARN-only is a decision, not a default** — B820's "statically-decidable absence" → advisory; `--strict` is the
  promotion. Record the DS2 OBSERVED flip + the four-root counts (lesson 11). `[ev: corpus B820]`
- Δ3 (fill at close) real-tree counts per root; the `step` absence pin.
- Δ4 **Fixture-GREEN over-flags the real tree** (W1 fact: 113 findings on the client roots → 2 after the fix). Fixtures prove
  the rule FIRES; only a real-tree PRESENCE pin (exact count + exact subjects + one ABSENCE) proves it fires on the right
  things. K22 / C8 lesson 11 recurred in W1 — second campaign in a row. `[ev: W1 apply, PR2/PR3 smoke]` `[ev: retro campaign8-close-process-meta-lessons Δ11]`
- Δ5 **K20 — an empty or missing input is NOT a clean run.** A no-source directory returned exit 0 until a lead-gate pin
  forced usage exit 3. Every lint's bats carries the "no sources / bad dir → exit 3" pin BEFORE the real-tree smoke.
  `[ev: W1 apply]` `[ev: corpus B820 §820.3]`

## campaign9-silent-protection (PR3)
- Δ1 **A cross-file follow needs a dir-wide index pass** — a per-file lint cannot see the adapter's `getXAlarm().setValue`
  (CP-2 clean via `BCompressorControl.java:1994`); Pass 0 builds the slot index first. `[ev: corpus B824 §824.4]`
- Δ2 **A private allowlist-named field is not a surface** (SP8) — name heuristics apply to SLOTS, fields need the
  follow. `[ev: corpus B824 §824.5]`
- Δ3 **Cross-lane pin ownership**: PR8 surfaces CR-3 → PR3's smoke expectation flips; whichever merges second owns the
  pin update. `[ev: design D9]`
- Δ4 **Line numbers drift, content does not**: B824's `:1539` was the generated getter; the write is `:1994` at
  a109249 — cite the DECL and the WRITE, re-anchor per tip. `[ev: corpus B824 §824.4(b)]`
- Δ5 **Fixture-GREEN over-flags the real tree** (W1 fact: 113 findings on the client roots → 2 after the fix). Fixtures prove
  the rule FIRES; only a real-tree PRESENCE pin (exact count + exact subjects + one ABSENCE) proves it fires on the right
  things. K22 / C8 lesson 11 recurred in W1 — second campaign in a row. `[ev: W1 apply, PR2/PR3 smoke]` `[ev: retro campaign8-close-process-meta-lessons Δ11]`
- Δ6 **K20 — an empty or missing input is NOT a clean run.** A no-source directory returned exit 0 until a lead-gate pin
  forced usage exit 3. Every lint's bats carries the "no sources / bad dir → exit 3" pin BEFORE the real-tree smoke.
  `[ev: W1 apply]` `[ev: corpus B820 §820.3]`

## campaign9-ext-writable-shape (PR10)
- Δ1 **The lint names the fix, not just the smell** — WARN text carries the child `…/value` leaf (the B826 preferred
  form) so the reader knows the write shape to use. `[ev: corpus B823]` `[ev: corpus B826]`
- Δ2 **EW10 real-tree pin SKIPs without the client tree — a SKIP is not a PASS**; record whether PR10's verify ran it.
  `[ev: retro campaign7 D9]`

## campaign9-write-path-rows (PR11)
- Δ1 **The matrix lives with the code it describes** (client repo, not `kit/docs/` which does not exist) — D11a.
- Δ2 **Row count is measured, never fixed** — "W14-W22" became `W14…W40+` from the measured uncovered set (27 slots +
  PR1's two). `[ev: design D11b]` `[ev: retro campaign8-write-path]`

## campaign9-doctrine-fold (PR12, promotion)
- Δ1 **K22 — one module-root/profile convention**: root = the dir containing the profiles; lints ITERATE profiles; no
  sources / no matrix → exit 3, never a silent 0. `[ev: retro campaign8-close-process-meta-lessons Δ11]`
- Δ2 **Slot types for externally written values** (SIMPLE value or a writing ACTION; bare complex OPERATOR = reject or
  silent default). `[ev: corpus B823]` `[ev: corpus B826]` `[ev: corpus B828 §828.7]`
- Δ3 **Alarm authoring A/B** (`BAlarmSourceExt` needs a `BControlPoint` parent → child point, or `BIAlarmSource` +
  `AlarmSupport` on the EDGE only). `[ev: corpus B827]`
- Δ4 **The rule-number-vs-text grep trap**: searching a K-number or a §-number finds the label, not the rule; grep the
  rule TEXT (K22 was found by its wording). `[ev: corpus C8 close-fold lesson 11]`

## campaign9-close-process-meta-lessons (PR13) — candidates, one line each, fill the evidence at close
1. **Stale-checkout reads are the campaign's recurring defect class.** The C9 design (D1/D8) cited `4f5f1c7`; my own
   evidence map repeated D8a's stale silent-zero claim; the S20 apply-package rev 1 copied line numbers from the design
   text. Fix that stuck: ONE read-only worktree at the chain tip (`main-a109249`), and "state the tree you read" on every
   cite. `[ev: probe c9-s20-rotation-design-evidence §1]` `[ev: memory client-reads-use-a109249-worktree]`
2. **Verify content, then COUNT the anchor** — verifying that `:230` is `pickMostHoursOn` is not verifying that it is
   `:230` at the tip. Re-anchor = re-count. `[ev: apply-package S20 rev 2 lesson]`
3. **Executable RED wins over prose** — the design's `Decision decide(...)` vs the RED's `int decide(...)`; the boolean
   `rotationMakeBeforeBreak` vs the RED's `int rotationMode`; CL6 400 vs the RED's 401. When they disagree, the compile
   contract is the spec. `[ev: design D1b/D10a]` `[ev: RED cc1c948:34]`
4. **Harness-only pins are DECLARED in the RED header, not skip-gated** — the WSL file has zero `@Ignore/Assume`; the
   verify gate counts against the header coverage map, and a WSL run never reports them green. `[ev: RED 70a357b header]` `[ev: RED 8b43488 header]`
5. **A design decision must be re-applied to every gate it touches** — the rotation clock moved to `rotSinceMs[]` but
   gate 8 kept `cmdSince[out]`, silently reintroducing the ROT16 bug the section claimed to close. After changing a
   primitive, grep every use of the old one. `[ev: probe c9-s20 second read]`
6. **The null-Context write is a security class, not an audit gap** — `ComplexSlotMap.set:662` gates BOTH the
   `AuditEvent` and `user.checkWrite`; `set(prop, v, null)` bypasses Niagara permission enforcement. Kit doctrine: a
   servlet write ALWAYS passes a user-bearing Context; a lint candidate (`set(` with a literal `null` Context in a
   servlet) for C10. `[ev: corpus B830 §830.4]` `[ev: corpus B829]`
7. **`BUser` IS a `Context`** — no wrapper; the client already relied on the cast. Small API facts save whole seams.
   `[ev: corpus B830 §830.2]`
8. **Lockout accounting is caller-invoked** — `validate()` has no side effects; a module that forgets
   `authenticateFailed` ships a password oracle. Pin it (CL3). `[ev: corpus B830 §830.3]`
9. **Never leak the scheme** — an unsupported authenticator answers 401, not 400 (CL6). `[ev: design D8c]`
10. **A tool failure is not a zero** — the first niagara-help pass failed on zsh word-splitting (`$H` unsplit); the
    zeros were recorded only from the successful re-run. `[ev: corpus B830 §830.8]`
11. **Public-repo publication is a user decision, and a peer relay is not the user's approval** — the classifier
    blocked a push to a repo that had become public mid-session; the decision was taken by Cristian in-session, then
    pushes resumed. Check `gh repo view` visibility before the first push of a session. `[ev: session 2026-09-06]`
12. **Identity vs session are different columns** — `config_session` is an opaque id (NULL for surface B because the
    `AuditEvent` has no session field, `ComplexSlotMap.java:1687`); the operator goes in the identity column. `[ev: design D7/D7a]`
13. **Second-read cadence works when the reader holds the source** — design → second read → 14 edits → validator PASS
    in one loop; the reads that caught defects were the ones that re-ran the grep at the tip. `[ev: niagara-tools d2857d1, fb9f0d4]`
14. **Repo-fixed ≠ deployed — every fix claim carries the DEPLOYED version.** The station runs ColdRoomPan-rt 2.0.3 /
    CompPan-rt 2.0.1 / DashboardPan 2.0 (RAR `module.xml` read, a34f9bdd2) while the repo tip a109249 is 2.0.7/2.0.3/2.1.1
    and was never deployed: the "silent zero" IS live on the panel (2.0 has no numeric guard) and the defrost `time<=0`
    bug IS live (2.0.3 `:555`), although both read as fixed in the repo. Rule: a claim "X is fixed" names the version
    where it is fixed AND the version that RUNS; the deploy chain is a precondition of every live gate. `[ev: a34f9bdd2]` `[ev: client PR #9 runbook]`
15. **Resume from the uncommitted tree, never from the package.** A worker was cut by a session limit with 111 lines
    of the core already in the worktree; the resume rule is: READ the uncommitted tree first and continue from it.
    Restarting from the apply-package re-derives and drifts — the tree is the state, the package was only the plan.
    `[ev: W1 apply, PR1 worker resume]`
16. **A negative claim needs a grep that actually asked the question.** "coolOnSensorFault is not in WRITABLE" was
    stated from a grep whose pattern never contained that name; the entry is at `write-server.mjs:94`. Before writing
    "X is absent", show the query that would have matched X. `[ev: session 2026-09-06, dead-panel-writes read]`
17. **Fixture-fitting production code is a defect, not a fix.** PR1 guarded `pickLeastHoursOff` with `cmdSince[k] != 0`
    and switched staging to `autoOnCount` — both to make rotation fixtures pass, both ungated production changes. The
    rule: when a fixture fails, the FIRST hypothesis is the fixture is wrong (missing `seedRestart`, `minOff>0` on a
    never-commanded unit), not that production semantics must move. A production line changed to satisfy a test is
    reviewed as a production change with its own spec, or reverted. `[ev: probe c9-pr1-green-read F1/F2]`
18. **A golden protects only the cases its sequence exercises — name the uncovered axes.** ROT5's byte-identical golden
    passed while `onCount→autoOnCount` silently changed HAND-present staging, because the golden `demandSeq` is all-AUTO.
    A byte-identical claim must state the axes the trace does NOT vary (here: HAND mode, `minOff>0` on a fresh unit); the
    fix is a second golden per uncovered axis (ROT5b-hand), not a stronger assertion on the same trace. `[ev: probe c9-pr1-green-read F2]` `[ev: retro campaign8-close-process-meta-lessons Δ11]`
19. **A lazy stamp that lags in the safe direction can still INVERT a lifecycle contract.** PR1's rotation clock lagged
    conservatively (rotation later, never earlier) — which looked safe — yet the enable-edge stamp of OFF units plus a
    stage-up that never re-stamped gave an OFF-at-enable unit a stale clock, making it eligible to rotate at ~0 runtime:
    the exact opposite of ROT16. "Conservative lag" is not "correct"; a lifecycle timer must be stamped at the event it
    measures (the command), never reconstructed lazily from a different clock. `[ev: probe c9-pr1-green-read F3]`
