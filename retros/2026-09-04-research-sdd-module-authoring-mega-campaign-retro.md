<!-- review-status: pending -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · tooling · 2026-09-04 · Research-SDD self-retrospective — module-authoring mega-campaign (B747-B760, three focuses)

> Continuation of the RT-authoring campaign (B729-B746). One continuous, operator-driven run that closed THREE
> focuses back-to-back — `interactive-composition` (B747-B750), `wb-ux-authoring` (B751-B753), `module-authoring`
> (B754-B760) — 14 corpus blocks, 17 parallel Explore sweeps, plus updates to the build-n4-module kit's
> corpus-index and a JUnit-location retro. READ-ONLY on the kit; PROPOSES only (§18). Captures the PROCESS
> lessons NOT already in the RT-campaign retro; content lives in the blocks.

## Context
The operator drove the axes one prompt at a time ("la distribución de los bloques" → Honeywell org → WB/UX →
"ve por todos" → "checa los bits"), each a deeper cut of the same thread: how modules are built/organized and
how OURS should be. Against the mature corpus the value was again APPLICATION + a code CENSUS across the
Honeywell/Tridium module families (the framework was largely covered). Ran alongside the "Codig" build session,
which mid-run began editing CompPan. Also closed a loop from the prior session: the defrost `started()` fix was
confirmed live by the operator.

## Proposed kit deltas

1. **The "census → taxonomy → playbook" triad is a repeatable heavy-mode template.** Every axis resolved to the
   same shape: fan out N parallel Explore sweeps (one per module family, SAME questions), synthesize a
   taxonomy block, then an applied-to-our-modules block. It produced comparable, mergeable evidence and kept
   parent context clean across ~30 modules. → PROPOSED METHODOLOGY §2/§16 note: for a "document everything
   about X across many modules" request, the canonical decomposition is per-family parallel census + one
   taxonomy block + one our-modules playbook block; seed the gaps that way up front.

2. **"Ve por todos" = parallelize the remaining seeded gaps, don't serialize.** I had offered a menu
   (AskUserQuestion), the operator picked one then said "all". The right response was to fan out ALL seeded
   axes' sweeps at once (6 concurrent), not run them sequentially. → PROPOSED §17/PROMPT-LOOP: when the operator
   says "all/todos" over a seeded backlog, launch the sweeps concurrently and synthesize on completion, rather
   than one iteration per axis.

3. **A cheap, cross-cutting mid-run operator sub-request becomes a BONUS block, not a deferral.** The "checa los
   bits" injection mid-synthesis became B755 (exact Flags/BStatus/BPermissions/BVersion values) as an extra
   gap (MA8) without derailing the campaign. → PROPOSED §8: a mid-run sub-request that is cheap and
   cross-cutting is best absorbed as a bonus block + a tracked extra gap, delivered inline.

4. **Numeric constants must be grepped VERBATIM, never quoted from memory or a prior block.** For B755 I pulled
   `Flags.java:23-44` / `BStatus.java:46-53` / `BPermissions.java:26-31` / `BVersion.java:58-66` directly; a
   remembered value (e.g. SUMMARY=8) is worthless without the file:line. → PROPOSED §11 (self-verify): bit
   values, ports, defaults, enum ordinals are [CERT] ONLY with a fresh file:line grep; a value recalled from
   memory or an older block is [INFER] until re-grepped.

5. **A dirty working tree owned by a parallel session is a hard read-only boundary.** When told "@Codig ya está
   realizando las modificaciones", I checked `git status` (CompPan files modified/uncommitted), STOPPED
   editing, and pivoted to review/reference + "verify-after-commit". → PROPOSED §14 (consistency)/coordination:
   before touching a shared subject, `git status` it; a dirty tree owned by a peer session is off-limits —
   deliver analysis, and offer to verify once they commit. (Complements the RT-retro's peer-review delta.)

6. **Engram semantic search false-zeros the same way corpus-nav does — fall back to the primary source.**
   `mem_search` returned nothing for the CompPan 8-findings observation across several phrasings (it is
   lexical), yet `mem_context` had surfaced it and the code carried the facts. Concluding "not found" would
   have been wrong. → PROPOSED tools/README + §heuristics: an engram `mem_search` zero is NOT proof of absence;
   on a miss, fall back to `mem_context`, the code, or the block — never assert "the other session didn't find
   it" from a search zero.

7. **A corpus campaign should feed the consuming kit's index at §18.** Each axis's blocks were wired into the
   build-n4-module `corpus-index` retro (updated 3× this session) so the build skill can find them. The
   research↔kit link is now explicit and should be a checklist item. → PROPOSED §18: at a campaign's retro,
   check whether a consuming kit (build-n4-module) has a corpus-index that needs the new blocks, and propose
   the wiring as a kit-side delta there (not in the research kit).

8. **Reusable TOOLCHAIN facts belong in the kit with an exact path, not "cacheado".** The JUnit-location hunt
   ("busco el JUnit") existed only because `build-verify.md:96` said "junit-4.13.2.jar cacheado" without a
   path. Documented the exact gradle-cache locations + a hash-robust `find` resolver in a build-n4-module
   retro. → PROPOSED §20 (document mode): a toolchain how-to that names a cached artifact must pin its resolved
   location OR a dynamic resolver, never an unqualified "cached".

## What went well (keep)
- The 6-concurrent-sweep fan-out per axis returned dense file:line evidence and closed each focus in a single
  run; notification-driven synthesis (wait for all, then write the blocks) scaled cleanly.
- Per-focus RESEARCH-STATE + FOCUSES + CATALOG + SOURCES + engram updates after each campaign kept a 760-block
  corpus consistent across a long multi-axis session.
- Closing the loop with the operator on the live defrost confirmation ([CERT-live]) validated the B729 idiom
  that seeded the entire RT→module-authoring arc — a good example of lived evidence closing a research thread.
- Coordinating with the Codig build session (research vs client repo; no edits to their dirty tree) avoided
  collision on CompPan.
