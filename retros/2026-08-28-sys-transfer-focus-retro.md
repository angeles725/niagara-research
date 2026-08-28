<!-- review-status: pending -->
<!-- kit-retro: include -->

# §18 Retrospective — sys-transfer focus (2026-08-28)

**Run**: niagara-research, focus `sys-transfer`, 2026-08-28
**Blocks written**: B595–B599 (5 gaps ST1–ST5)
**Coverage**: 5/5 investigable gaps closed; 0 requires-execution
**Driver**: self-paced /research-sdd (Opus), **direct seed (no delegated sweep)**, verified inline

---

## Summary

The first focus this session seeded WITHOUT a delegated AUDIT-FIRST sweep — a deliberate call for a 16-class,
clearly-factored package whose structure was self-evident from the class names (a strategy-pattern factory + named
source→dest strategies + results + a context). It worked cleanly: 5 tight blocks, each closing a real mechanic,
and the focus LOCATED the handle-preservation primitive (`ReplacingContext`) that four prior template blocks
([B578]/[B579]/[B583]/[B593]) had relied on as opaque. One kit observation about when a sweep is worth the cost.

---

## Delta proposals

### D1 — a delegated AUDIT-FIRST sweep is not always worth its cost; a small, self-evident package can be seeded directly (NEW, MEDIUM)

**What happened.** Every prior focus this session ran a delegated sonnet sweep (~80–120k tokens, ~3–6 min) before
seeding. For `sys-transfer` I skipped it: I did the prior-coverage reconciliation myself (grep for consumers,
confirmed B200/B578/B579 are consumer-only), read `TransferStrategy` + `Mark` inline to confirm the dispatch
model, and seeded 5 gaps directly from the 16 class names (a factory + 11 strategies + a context + results + a
listener — the taxonomy is legible from the names). The result was indistinguishable in quality from the swept
focuses, at a fraction of the cost. The sweep adds value when the surface is LARGE or AMBIGUOUS (which classes
matter, what's REMITTANCE) — e.g. provisioning (219 classes), hierarchy (rich engine + 32 docs). It adds little
when the package is small and its factoring is self-describing.

**Proposed delta.** METHODOLOGY §13 (AUDIT-FIRST) should state an explicit CHEAP-TRIAGE gate before delegating a
sweep: if the target is a single small package (≈ ≤20 classes) whose class names make the taxonomy legible AND a
quick inline read of the base/factory class confirms the model AND prior-coverage reconciliation is a
one-grep answer, SEED DIRECTLY and skip the delegated sweep. Reserve the sweep for large (30+ class), multi-module,
or ambiguous surfaces. This is the inverse of over-delegation: the delegation-triggers exist to avoid inflating
the parent context, but a 16-class package read inline costs less context than launching, waiting on, and
ingesting a sub-agent's report.

---

## What went well (keep)

- **Direct seed + inline base read was enough** to produce an accurate 5-gap backlog with correct
  priorities (ST1/ST2 high = factory + the template-relevant primitive; ST3/ST4/ST5 medium).
- **The focus paid a debt.** B596 LOCATED `ReplacingContext`'s handle map — the exact mechanism B578 §578.3 and
  B579 §579.2 named but treated as opaque. Opening a base primitive AFTER its consumers is a valid and valuable
  ordering: the consumer blocks told us what to look for (handle preservation), and this focus found where it
  lives. Worth noting as a pattern — a "used-but-unopened primitive" is a high-value focus precisely because the
  consumers already framed the questions.
- **The UNDO finding (B598)** — that every `TransferResult` carries `undo()` — was not on the seed radar; it
  emerged from reading `CompTransferResult` and turned out to be why WB paste is reversible. Reading the RESULT
  types, not just the strategies, caught it.

---

## Child gaps surfaced (named, out of scope)

- `BIDeployable.Step` internals — the deployable SIDE of deploy (lives on `BNtplFile`, [B583]); not transfer.
- `TransferCodec` value-encoding — overlaps the BOG/BOX serialization ([B5]/[B512]); a serialization cross-cut.
- `TransferConst` / `TransferEnvelope` / `TransferArtifact` (bajaui + workbench) — the clipboard/drag-drop DATA
  format; a WB-UI detail, low value.
