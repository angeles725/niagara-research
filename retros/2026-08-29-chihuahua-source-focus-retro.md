<!-- kit-retro -->
# §18 Self-Retrospective — focus `chihuahua-source` (B648–B655)

**Date**: 2026-08-29 · **Focus**: chihuahua-source (niagara-research) · **Blocks**: B648–B655 (8) · **Mode**: heavy/auto, self-paced, Opus + sonnet sweeps · **Review-status**: pending
**Outcome**: 8/8 (CS1-CS8). Source-level audit of the ONLY production module ([B643]). Corrections: §14 to [B636] #1 + [B640] P1 (via B649). Verdict: chihuahua is production-grade; one HIGH safety fix (overload fault-to-danger).

## What the focus established
- CS3/B648: write-auth CORRECTLY RBAC-enforced (OPERATOR_WRITE, all 8 handlers, audit) — inverse of mcpbridge [B643].
- CS1/B649: build best-versioned of fleet; §14 — "over-permission" is empty Tridium scaffold, not real over-privilege.
- CS2/B650: strong defensive design; overload protection FAILS-TO-DANGER on faulted amp sensor (readSlotVal→0.0); antifreeze fails safe.
- CS6/B651: internal 2026-05-06 audit rigorous, fixes hold; P1 fixed DISPLAY-path fault-discrim, protection-path is the residual.
- CS4/B652: read layer injection-safe + N4.14-gotcha-aware. CS5/B653: ES5 SPA, RBAC server-authoritative. CS7/B654: wb BatchLinkEditor proper WB view.
- CS8/B655: verdict + prioritized fix list + template-inheritance recommendation.

## What worked
- **FALSIFY-BEFORE-REPORTING resolved TWO safety-critical questions the sweeps punted.** CS3's sweep and CS2's sweep both deferred the load-bearing security/safety question ("does it enforce authz?", "does it guard sensor fault?") — CS3 sweep under-counted RBAC call sites (6→actual 8), CS2 sweep left the fault-guard "in the service layer, unsurveyed." The driver read the bytecode/source directly and both were resolved (RBAC confirmed; overload fail-to-danger found). Lesson (reinforces): a sweep that PUNTS a safety/security question to "another layer" is not done — the driver must read that layer before the block asserts the conclusion.
- **SOURCE beat JAR for intent.** CS1 §14-corrected the whole audit's "over-permissioning" finding: the jar shows `type="all"` tags; the source shows they're EMPTY Tridium scaffold. Real-artifact-first (module-anatomy delta #1) extends to: when judging INTENT/config, source > packaged jar.
- **Reconciling against the subject's OWN prior audit (CS6) was high-yield.** audit-2026-05-06 surfaced the display-vs-protection fault-path split that made the CS2 finding precise and actionable.

## Proposed kit deltas (PROPOSE, do not apply)
1. **[MED] SWEEP-PUNT-TO-ANOTHER-LAYER is a driver-must-follow signal.** When a delegated sweep answers a SECURITY or SAFETY question with "the check, if it exists, is in <other file/layer> (not surveyed)", the driver MUST read that layer before authoring the conclusion — never let the block inherit the sweep's punt. (Twice load-bearing this focus.) Specialization of VERIFY-BEFORE-ACTING + FALSIFY-BEFORE-REPORTING for the specific "deferred to another layer" phrasing.
2. **[LOW] SOURCE > JAR for intent/config claims** — extend module-anatomy retro delta #1 (real-artifact-first): a packaged artifact shows declarations; the source shows whether they're real or scaffold. When a finding is about INTENT (over-permission, dead code, config), prefer source if available. (CS1 corrected two prior blocks this way.)
3. **[LOW] Reconcile against the subject's own QA docs.** When auditing an artifact that ships its own audit/CHANGELOG/ADR docs, add an explicit reconciliation gap (like CS6) — it grounds severity and finds fix-path splits the code alone hides.
4. **[LOW, 3rd REINFORCE] verify-block citation-class WARN** — jar!entry / `[BNNN]` / full-source-path citations WARNed on nearly every block across THREE focuses now (module-anatomy, own-modules-audit, chihuahua-source). Strong repeat signal to classify them as recognized non-resolvable citations.

## Non-kit note
- Same RESEARCH-STATE row-edit friction (exact-suffix python replaces). Execution, not a rule.
