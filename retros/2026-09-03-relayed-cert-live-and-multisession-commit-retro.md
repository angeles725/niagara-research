<!-- review-status: pending -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · access-control (document-mode) · 2026-09-03 · Research-SDD self-retrospective

> Run reviewed: a DOCUMENT-mode capture (§20) — one runbook block, **B726** "Remote Workbench to a JACE behind
> a jump host over SSH port-forwarding". Not a discovery loop. Trigger: operator asked to document a support
> case. The evidence base was unusual: the procedure was **executed by a peer/operator session at a customer
> site** (Pancaddia León) and relayed to this session over cross-session messages; this session authored the
> block and preserved the report as a probe. READ-ONLY on the kit — PROPOSES only (METHODOLOGY §18).

## Context

The block began as a consult (three-sources verification: niagara-help + corpus + decompiled Fox wire) and
became a runbook once a peer session confirmed the tunnel worked on live hardware. Two things stretched the
kit's assumptions:

1. **`[CERT-live]` evidence I did not run myself.** The `openssl s_client` cert readback, the Windows
   `Test-NetConnection` results, and the Open Platform/Open Station connections were all performed by the
   operator/peer, not by this session. I marked them `[CERT-live]` with an explicit "not run by the author of
   this block — reported by the operator/peer session" caveat, preserved the relayed commands/outputs under
   `sources/probes/2026-09-02-jace-ssh-tunnel-panccadia.md` (secrets parameterized), and recorded the honest
   residual the peer flagged (operator confirmed "both connected and worked" but did NOT itemize whether the
   Foxs cert dialog was separate, nor formally measure session stability).
2. **A regenerated `CATALOG.md` picked up an untracked sibling block from a concurrent session.** `gen-catalog.py`
   reads blocks from disk; the shared checkout held another session's still-untracked `B725`, so the regen added
   a `B725` row. Committing it as-is would have pushed a link to a file not in the committed tree.

## Proposed kit deltas

> Only genuinely NEW items. Anything already encoded is under "Already covered".

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | Name the **relayed-`[CERT-live]`** pattern in the marker rules: a live fact confirmed by a *teammate/operator*, not the block author, is still `[CERT-live]` — BUT the block must (a) attribute WHO ran it, (b) preserve their report as the probe under `sources/probes/`, and (c) record any sub-fact the reporter did NOT itemize as an explicit residual rather than smoothing it into the claim. Overclaiming a relayed live fact is the failure this prevents. | `METHODOLOGY.md §3` (markers) + `§20` (document mode, procedure/how-to genre) | B726 §726.4 + self-verify claim 5 + Open gaps; probe `2026-09-02-jace-ssh-tunnel-panccadia.md`; peer messages from `Panccadia`/`pancaddia-bf` | new | MEDIUM |
| 2 | Add a **shared-checkout commit guard** to the delivery/versioning guidance: before committing a regenerated `CATALOG.md` (or any generated index) in a working tree that concurrent sessions share, check for rows/links pointing to UNTRACKED files from another session; exclude them from your commit (their owner's next regen restores them) so you never push a dangling link. | `METHODOLOGY.md §15` (corpus versioning / commits) + PROMPT-LOOP delivery HARD RULES | This run: `gen-catalog.py` added a `B725` row (another session's untracked block); removed it from the commit, committed B726's unit clean, restored the row only when B725 itself was committed. commits `8d8614dcf` (B726) then `5f3eebb5f` (B725+CATALOG) | new | MEDIUM |
| 3 | Note that document-mode blocks capturing a **still-unfolding live operation** are authored provisionally (open gap) and PROMOTED as evidence lands — one block, revised in place as the operator reports back, not a new block per update. | `METHODOLOGY.md §20` (document mode) | B726 written with the Fox/Station leg as an open gap, then promoted to `[CERT-live]` in the same run when the peer relayed the operator's confirmation minutes later | new | LOW |

## Already covered (do NOT re-add)
- **Three-sources verification before asserting.** The consult consulted niagara-help (guides), the corpus
  (B560/B134), and the decompiled Fox wire before answering — exactly the §3/three-sources rule. No new rule.
- **SECRETS DISCIPLINE on a live-install runbook.** Parameterizing the jump-host FQDN, SSH principal, key
  filenames, and JACE MAC (kept OUI `00:01:f0`=Tridium as structure) is the existing live-install rule; it
  applies to cross-session-relayed content the same as to a file scanned locally. Delta #1 only adds the
  *attribution/residual* half, not the redaction half.
- **Probe preservation + Engram mirror for a procedure.** `sources/probes/` + the mandatory §20 Engram mirror
  were followed as written.

## Honest scope note
One block, one probe, one closed gap (Fox/Station leg). The three deltas are method transfers, not subject
findings. If the maintainer judges delta #3 to be already implicit in §20's "capture what you just did", dismiss
it — it is the weakest of the three. Deltas #1 and #2 are genuine gaps the kit does not currently name.
