<!-- review-status: pending -->
# Retro — niagara-research · email · 2026-08-04 · Research-SDD self-retrospective

> Run reviewed: email focus, B324–B334 (10 evidence blocks + 1 synthesis), focus-completion trigger.
> Blocks: B324 BEmailService, B325 BOutgoingAccount, B326 TLS session, B327 inbound + ack (security),
> B328 OAuth2, B329 security dashboard, B330 account base, B331 email-wb UI, B332 converters,
> B333 email-ux, B334 synthesis. One-line outcome: the email subsystem is fully static-read (61 classes,
> 10/10 gaps), gated three ways, insecure-by-default/secure-by-wizard, with a spoofable inbound door the
> security dashboard cannot see; one requires-execution child gap (email-G1) deferred.
>
> Method: a FRESH-CONTEXT agent read the current kit (`PROMPT-LOOP.md` + `METHODOLOGY.md`) FIRST, then the
> run's blocks/commits, and proposes kit deltas. READ-ONLY on the kit — this report only PROPOSES; kit
> changes are human-reviewed and human-committed (METHODOLOGY §18).

---

## Proposed kit deltas

> Only genuinely NEW items; kit-existing rules are under "Already covered".

| # | Proposed change | Target (file · §/section) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| 1 | Add explicit rule: a synthesis block MUST register newly uncovered `requires-execution` gaps in the backlog table (not only in the iteration-history "New gaps uncovered" column), even when the focus is being declared STOPPED, so `requires_execution_open` and `verify-state.sh` see them. | `PROMPT-LOOP.md` step 6 UPDATE STATE + §8 STOP criterion footnote | B334 iteration history row records `email-G1 (requires-execution)` but the backlog has no row and `requires_execution_open: 0`; commit `11142b9` (`7890c87` FOCUSES mirror) | new | HIGH |
| 2 | Codify the "bare-`:line` single-source shorthand" pattern: when a block's body uses bare `:line` citations (no filename prefix) against one declared source for readability, the self-verify MUST list the filename-qualified form for every load-bearing anchor — that is the only form `verify-block.sh` cannot silently pass as `extern` while leaving a reader unable to audit it. Name the pair: "bare `:line` body + full `filename:line` self-verify anchor" as a named convention, not an improvisation. | `METHODOLOGY.md §11` self-verify contract (token-check paragraph) | B329 §329.7: body uses `:144`, `:188`, `:210` throughout; compensates in self-verify with `BEmailServiceSecurityDashboardProviderAgent.java:144` etc.; `verify-block.sh` exits 0 but classifies all as `extern`; commit `27e6cf7` | new | HIGH |
| 3 | Add to §16 a prior-coverage check as the FIRST step before the audit-first sweep when opening a NEW focus on a MATURE multi-focus corpus: (a) read FOCUSES.md + INDEX.md to find existing block coverage overlapping the new angle; (b) pre-declare those items REMITTANCE with their `[Block N] §N.x` citation BEFORE delegating the audit sweep; (c) THEN delegate the sweep so it seeds only genuinely new gaps. Without this step, the audit sweep proposes gaps the corpus already closes, inflating the backlog. | `METHODOLOGY.md §16` + `PROMPT-LOOP.md` BOOTSTRAP step e (AUDIT-FIRST paragraph) | RESEARCH-STATE-email.md header: BEmailRecipient pre-declared REMITTANCE to [B34] §34.6.5 before the audit sweep ran; [B27]/[B31] also pre-identified; if skipped, the sweep would have seeded E4/E5 overlap as fresh gaps; commit `0382863` (bootstrap row, iteration 1) | new | MEDIUM |
| 4 | Clarify when an evidence block containing a cross-block synthesis section becomes MIXED: if a section within an evidence block draws `[INFER]` connections ACROSS blocks (not deductions from THIS block's own `[CERT]` sources), declare the type MIXED rather than evidence. The threshold exists; the trigger rule does not. This keeps the `[INFER]/[CERT]` ratio advisory rather than a false exhaustion signal. | `METHODOLOGY.md §11` marker-tally / block-type declaration | B333 §333.5 ("The three UI layers, consolidated") is a cross-block synthesis section; the block is declared `evidence` not MIXED; `[INFER]/[CERT]` ratio 0.50 sits exactly at the exhaustion boundary and reads ambiguously; commit `202fdf6` | refinement | LOW |

For each delta above:

- **#1** — Email-G1 named in the iteration-history "New gaps" column but absent from the backlog means `verify-state.sh` and `research-sdd-status.sh` cannot gate on it. A synthesis block is a closing iteration; the UPDATE STATE rule applies equally, but the "closing feel" creates a blind spot. Impact: prevents silent gap loss on every focus that uncovers requires-execution work in its synthesis block.
- **#2** — The bare-`:line` shorthand is organic for single-source inline reads (B329, 402 lines, one class) but degrades auditability: a reviewer cannot follow `:144` to a source. The improvised fix (list filename:line in self-verify) works but is unnamed, so it will be rediscovered or skipped in future runs. Naming it makes the pattern reusable and the self-verify gate enforceable.
- **#3** — Without a prior-coverage check, an audit sweep over a mature corpus seeds false gaps that bloat the backlog and waste iteration budget on remittance blocks. The email focus avoided this by checking first; the kit does not prescribe the check or its timing. Low cost (read FOCUSES.md + skim INDEX.md); high value on any mature multi-focus corpus.
- **#4** — B333's ratio at 0.50 is a judgment call (declared acceptable); declaring MIXED makes the call mechanical. Low priority because the ratio was read correctly in practice — only the declaration was wrong.

---

## What held well (kit rules confirmed by this run)

- **Audit-first backlog** (`PROMPT-LOOP.md` BOOTSTRAP step e): delegating a sonnet sweep before writing any block seeded 10 well-shaped gaps in one shot, zero redundant gaps (excluding the pre-declared REMITTANCE). Confirmed the pattern works for a new focus on a mature corpus.
- **Model-tier declaration every iteration**: every row in the iteration history carries `delegated? · model tier`. Auditable from disk. No silent inline reads that inherited the driver model.
- **Security-claim driver verification** (PROMPT-LOOP §INVESTIGATE framework-semantic-check): B327 spoofing finding and B330 credential-discard (`fw(x=11)` discards vs migrates) were both re-read by the driver after the sonnet sweep, per the framework-semantic-check rule. B327 explicitly says "the full `BEmailAlarmAcknowledger.java` was read in this iteration" and grep-confirms absence of `token|hmac|signature|verify|password`.
- **Focus commit message discipline** (`research(niagara/email): B<n> <slug>`): all 11 commits use the multi-focus scope form. Git log is scannable for resume.
- **Synthesis block declared SYNTHESIS in self-verify**: B334 §334.10 correctly flags the `[INFER]/[CERT]` = 11.0 ratio as expected for a synthesis block, per §11. `verify-block.sh` would have flagged "zero file:line" without this declaration.
- **email-G1 classified requires-execution, not blocked**: B334 §334.9 explicitly labels it `requires-execution / live-mailbox, out of scope for the static focus` — correct §8 classification, not a free-floating "needs investigation" note.

---

## Anti-patterns observed

- **email-G1 uncovered but not registered in backlog** (#1 above): the iteration-history row records it; the backlog table and `requires_execution_open` counter do not. The "closing feel" of a synthesis block creates a consistent blind spot where the register-new-gaps rule is relaxed.
- **B329 bare `:line` body with extern compensation** (#2 above): the pattern was improvised correctly (self-verify compensates) but represents friction — the driver had to know to add filename qualifiers in the self-verify because the body used shorthand. Without the compensation the self-verify would be non-auditable.

---

## Tools built, adapted, or outgrown

No new tools this run. All analysis was decompiled-Java read via the vineflower pipeline (existing toolbelt). No tool decisions.

---

> PROPOSAL ONLY — kit NOT edited. Kit changes require human review and a branch/PR per METHODOLOGY §18.
