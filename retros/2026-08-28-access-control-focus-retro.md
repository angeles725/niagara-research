<!-- review-status: pending -->
<!-- kit-retro: include -->

# §18 Retrospective — access-control focus (2026-08-28)

**Run**: niagara-research, focus `access-control`, 2026-08-28
**Blocks written**: B558–B566 (8 investigable gaps AC1–AC8) + B560 (cloudflared runbook, document-mode)
**Coverage**: 8/8 investigable gaps closed; 0 requires-execution
**Driver**: self-paced /research-sdd (Opus), AUDIT-FIRST seed delegated to sonnet, verified inline

---

## Summary

Clean run. The AUDIT-FIRST sweep produced a good REMITTANCE/gap separation and one high-value catch (the B11
password-policy error). Inline token-verify held throughout and corrected two sweep claims before they became
`[CERT]`. One document-mode block was interleaved on operator request. Two genuinely new kit observations.

---

## Delta proposals

### D1 — AUDIT-FIRST sweep NUMERIC claims (counts, caps, totals) are unreliable and must be marked ESTIMATE (NEW, HIGH)

**What happened.** The delegated sweep asserted a "64-category hard limit" (AC3) and "8 encoders" (AC4). Inline
verification found the real values: the category working buffer is **256** (`BCategoryService.doUpdate`), and
there are **10** encoder classes. Both sweep numbers were stated as if factual. The blocks are sound because the
driver re-measured, but a less careful pass could have propagated a wrong `[CERT]`-looking number. This is the
same failure mode the `tags` retro flagged (4/4 permission claims needed correction) — it recurs for COUNTS and
CAPS specifically.

**Proposed delta.** The AUDIT-FIRST sweep prompt (PROMPT-LOOP BOOTSTRAP step e / METHODOLOGY §13) should
instruct the delegated agent to render every numeric quantity (class counts, enum sizes, limits/caps, iteration
counts) as an **explicit estimate** ("~N, verify inline") and to NEVER present a cap/limit as established. The
driver's block must re-measure any number it promotes to `[CERT]`. Consider a one-line rule in §13: "delegated
counts are estimates; a number is `[CERT]` only after the driver reads the defining site."

### D2 — document-mode blocks that mix corpus facts with external-product knowledge need a marker convention (NEW, MEDIUM)

**What happened.** B560 (cloudflared runbook) is a document-mode block whose Niagara facts are `[CERT]` (ports
from B460/B471, TLS-1.3 from B474) but whose tunnel mechanics are external Cloudflare product knowledge. I marked
those `[INFER-web]` and stated they should be validated against current Cloudflare docs. This worked, but the kit
(§20 document mode) does not define how to mark knowledge that is neither corpus-cited nor decompiled — it is
"true, external, version-drifting". Without a convention, a future runbook could present tool behavior as `[CERT]`.

**Proposed delta.** §20 (document mode) should name an explicit marker for external-product/operational knowledge
(`[INFER-web]` or `[CERT-web]` with a URL+date, `[INFER]` otherwise) and require runbook blocks to keep
corpus-`[CERT]` facts visually separate from external steps. B560 §560.4/§560.6 can serve as the worked example.

---

## What went well (keep)

- The B11 §14 correction was verified end-to-end (validator → wiring → caller chain) BEFORE writing, not
  asserted from the sweep's flag. This is the right bar for a correction that overwrites a prior block.
- Mid-turn operator additions (cloudflared, extra IT topics) were absorbed without derailing the loop —
  triaged against coverage, one document-mode block produced, the rest mapped to existing focuses.
- Every block carried a self-verify table with a marker tally and inline token-verification count.

---

## Child gaps surfaced (named, out of scope)

- `hierarchy` subsystem proper (BHierarchy/BHierarchyService) — candidate NEW focus (from AC7).
- `EncryptionKeySource` enum + at-rest transcode plumbing — [B393]/[B466] territory (from AC4).
- `BAuditHistoryService` record schema/retention — [B8] territory (from AC6).
