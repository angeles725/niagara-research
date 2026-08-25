<!-- kit-retro -->
<!-- review-status: pending -->
# Retro — niagara-research · licensing deep-dive (B477–B485 + live-install incident + §14 self-correction) · 2026-08-24 · Research-SDD self-retrospective

> Run reviewed: the licensing DEEP-DIVE run — blocks B477–B485 (subscription layer from `nre.jar`, `niagarad`
> enforcement, platform delivery, subscription onboarding/trust, license anatomy, Java crypto/trust internals,
> plus dsfspi native crypto core + launcher gates from the sibling native session), a §12 LIVE-INSTALL incident,
> and a §14 self-correction that RETRACTED a wrong negative claim (B478).
> Trigger: USER-requested fresh-context §18 retro (deep-dive follow-on to the 2026-08-24 licensing document-mode
> retro; this run is the NORMAL-CYCLE investigation that followed that consolidation).
> Method: a FRESH-CONTEXT agent read the current kit (`PROMPT-LOOP.md` + `METHODOLOGY.md` §3/§5/§8/§12/§14/§16/
> §18/§21, `toolbelt/tool-registry.md`) FIRST, then the EXISTING retro `retros/2026-08-24-licensing.md`
> (deltas D1 name-the-jar⇒open-the-jar HIGH · D2 fire-§18-on-tool-failure MED · D3 registry-clarification LOW +
> §10 tooling report), deduped hard against both, and proposes only genuinely-new deltas. READ-ONLY on the kit —
> this report only PROPOSES; kit changes are human-reviewed and human-committed (METHODOLOGY §18).

## Run summary

A NORMAL-CYCLE licensing deep-dive across nine blocks (B477–B485), run as a TWO-SESSION peer split (Primero =
Java decompile, Segundo = native RE) coordinating via SendMessage. Three signals of kit interest surfaced, two
of them from an INCIDENT and an ERROR (the priority material for this retro):

1. **A live-install SAFETY incident.** During a §12 live-validation attempt, the driver executed a Niagara
   launcher of a DIFFERENT version (`iC-Niagara-4.13.2.18\nre.exe`) pointed at a REAL production install via
   `NIAGARA_HOME=C:\Honeywell\OptimizerSupervisor-4.14`. A Niagara launcher REBUILDS the module registry on
   boot; this rewrote the operator's Workbench registry with the wrong version and produced a
   `ModuleNotFoundException` (snmp/sukarne) plus operator confusion about the stopped service. Root cause:
   "READ-ONLY over the subject" was read as permitting execution of the install's OWN runtime tooling.

2. **A wrong NEGATIVE existence claim propagated into a §14 correction.** An agent that had decompiled ONLY
   `nre.jar` (not `niagarad.jar`) asserted "no `com.tridium.niagarad.license.*` package exists," and that
   absence was accepted and pushed as a §14 correction into 4 artifacts (B442 pointer, B477 §477.4, B477
   self-verify, docs §10) before a first-hand `niagarad.jar` decompile disproved it — retracted at B478
   ("RETRACT my wrong 'no niagarad.license' claim; B442 §442.3 stands", commit `f13a143`).

3. **A positive control: cross-block conflict handled correctly.** When B482 found a `cacerts` + embedded
   RSA-2048 TPK trust picture that diverged from B392's `truststore.jks`, the driver did NOT overwrite B392 —
   it opened a reconciliation child gap (B482-G1). Confirms the corrected §14/CONTRADICTIONS.md discipline
   works; not a new delta.

The RE discipline itself was clean (corroborate-before-cite, §21 fallbacks, disk-first for the enforcement
blocks). The genuinely-new material is one SAFETY delta (D4), one epistemic reinforcement + §14 hook (D5), and
one workflow delta (D6). The absence-verification CORE behind the B478 error is ALREADY in the kit and was
SKIPPED, not missing — see "Already covered".

## Proposed kit deltas

> Numbered D4–D6 to continue the sibling licensing retro's D1–D3 scheme (this fleet reads its licensing retros
> by hand). Only genuinely NEW items are here; the strong existing coverage is under "Already covered".

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| D4 | **LIVE-INSTALL READ-ONLY = DISK-READ + DECOMPILE ONLY; a vendor launcher/daemon is a MUTATION, not a read.** For a `live-install` target, make explicit that READ-ONLY forbids EXECUTING the install's own runtime — no launcher/daemon/station (`nre`/`console`/`wb`/`plat`/`niagarad`/`station`, and the general class: any vendor bootstrap that builds a module/plugin registry or extracts install-data on boot) — and forbids setting `NIAGARA_HOME` (or any runtime-home env) against a real install. Rationale: such a boot has SIDE-EFFECTS (module-registry rebuild, install-data extraction) that mutate the subject even when your INTENT is to read, and a VERSION-MISMATCHED launcher rewrites version state. Live license reads use disk inspection of `.license` / a preserved `nre -licenses` dump the OPERATOR runs on a throwaway/lab install — never the driver executing the production runtime. | `METHODOLOGY.md §12` (invasiveness ladder / read-first — add a "vendor-runtime execution is a write" rung-0 caveat) + `PROMPT-LOOP.md` HARD RULES (extend READ-ONLY + DISK-FIRST) + SECRETS-DISCIPLINE live-install line | §12 live-validation attempt ran `iC-Niagara-4.13.2.18\nre.exe` with `NIAGARA_HOME=…\OptimizerSupervisor-4.14` → registry rebuilt at wrong version → `ModuleNotFoundException` (snmp/sukarne) + stopped-service confusion; B477 §12 live-validation context (commit `b79390d`) | new | HIGH |
| D5 | **Negative-claim clause for D1 + a §14 correction-on-absence guard.** (a) STRENGTHEN pending D1 (name-the-jar⇒open-the-jar): the obligation is symmetric for ABSENCE — a NEGATIVE existence claim about a named jar/binary ("no `com.tridium.niagarad.license.*` exists") is `[CERT]` ONLY if that EXACT jar was decompiled/opened; an agent asserting absence about an artifact it did NOT open is `[INFER]`, never `[CERT]`. (b) NEW §14 hook: a §14 correction that RETRACTS/REFUTES a prior finding ON THE BASIS OF an absence must verify that absence in the EXACT named artifact first (open the jar), never a sibling — because a correction PROPAGATES. | reinforce D1 (`PROMPT-LOOP.md` HARD RULES / INVESTIGATE step 3) + `METHODOLOGY.md §14` (add correction-on-absence guard) + `§3` marker note (negative existence is `[CERT]` only for the opened artifact) | B478 §478.5 RETRACT of the wrong "no niagarad.license" claim (commit `f13a143`); wrong claim came from an agent that opened only `nre.jar`, not `niagarad.jar`; propagated to 4 artifacts before revert | new (reinforces D1) | MEDIUM |
| D6 | **Peer-session split along an artifact-TYPE axis + a dedup-handoff protocol + peer attribution.** §16 covers concurrent LOOPS under ONE orchestrator with INDEPENDENT focuses; it does not cover two PEER driver sessions splitting ONE subject along the native-vs-Java axis and coordinating via SendMessage. Add to §16: (a) an artifact-axis peer split (one session Java-decompiles, one native-REs the SAME subject) is a valid topology; (b) a DEDUP HANDOFF — each session announces completed SHARED read-steps ("checkFileSignature done, don't repeat") so the peer skips redundant expensive reads; (c) CREDIT the peer session's cited analysis in the block (Connections/method), exactly as a sub-agent's cited findings are credited. Shared CORPUS writes (RESEARCH-STATE/INDEX/commit) still serialize per §16's barrier rule. | `METHODOLOGY.md §16` (multi-focus / concurrent loops — add the peer-axis-split subsection) | This run split Primero=Java / Segundo=native; B484+B485 "native RE by sibling session Segundo" (commit `7752229`); dedup handoff on `checkFileSignature` | new | MEDIUM |

For each delta above, one line of rationale (WHY · cost · impact):

- **D4** — The highest-value lesson of the run and a genuine SAFETY gap: the kit's §12 read-first framing assumes
  a probe is knowably read-vs-write, but a vendor LAUNCHER hides a write (registry rebuild) behind a "read"
  intent. Cost: one §12 rung-0 caveat + one line each in READ-ONLY/DISK-FIRST. Impact: prevents an agent from
  bricking/mutating a real production install while believing it is only reading a license.
- **D5** — The absence-verification machinery already exists and was SKIPPED; the genuinely-new part is narrow
  but real: (a) makes D1 symmetric so a negative claim about a named jar carries the same open-it obligation as
  a positive one, and (b) routes the existing "grep-confirm absence / widen scope" discipline into the §14
  CORRECTION path, where the stakes are higher (a correction propagated to 4 artifacts here). Cost: three short
  clauses. Impact: a wrong absence can no longer drive a corpus-wide correction.
- **D6** — A working multi-session topology the kit does not name. §16's concurrency rules keep parallel loops
  SAFE but assume independent focuses; a peer split along the native-vs-Java axis of ONE subject needs dedup
  coordination and peer attribution that §16 omits. Cost: one §16 subsection. Impact: the next deep-dive can
  split RE work across sessions without double-doing shared reads or losing the peer's provenance in the block.

## Already covered (dedupe — proof the retro read the kit first)

- **The driver must grep-confirm a sub-agent's ABSENCE, and a proven-absence is narrower than the corpus (widen
  to all jars/modules before accepting)** → already `PROMPT-LOOP.md` VERIFY BEFORE ACTING (b) + the SCOPE clause
  ("a module-scoped 'not found' is evidence for the module only — widen the search", L312–322), and
  `METHODOLOGY.md §8` (overlay-absence rule L644–647 + negative-closure sampling L635–639) + RE-MEASURE A
  DRAMATIC NEGATIVE. **This is the CORE of the B478 error — it was SKIPPED, not missing.** D5 does NOT
  re-propose it; D5 only adds the §14-correction hook + the D1/§3 negative-marker clause the kit lacks.
- **DISK-FIRST — prefer on-disk artifacts over a live probe** → already `PROMPT-LOOP.md` DISK-FIRST HARD RULE.
  D4 is NOT a DISK-FIRST duplicate: its new content is that the vendor LAUNCHER ITSELF mutates the subject
  (a hazard DISK-FIRST does not name).
- **§12 read-first / "confirm read-only in code first" / invasiveness ladder rung-0 passive capture** → already
  `METHODOLOGY.md §12`. D4 adds the "executing the vendor runtime is a write" caveat §12 does not state.
- **Concurrent loops under one orchestrator — independent state, cross-loop barrier for shared writes** →
  already `METHODOLOGY.md §16`. D6 keeps that barrier and adds the peer-axis-split + dedup-handoff +
  attribution layer §16 omits.
- **A cross-block conflict that cannot yet be adjudicated → record it / open a reconciliation gap, don't
  overwrite** → already `METHODOLOGY.md §14` (CONTRADICTIONS.md `open`→`resolved`). Candidate C (B482-G1
  truststore.jks vs cacerts+RSA-2048 TPK) is a clean instance — positive control, NOT a delta.
- **RE corroboration-before-citation; name-the-jar⇒open-the-jar (D1, pending); ghidra-evidence routing (D3)**
  → already the sibling retro `retros/2026-08-24-licensing.md`. This run REINFORCES D1 (opening `niagarad.jar`
  at B478 was exactly the name-the-jar obligation) and D5 extends it to negatives.

## Anti-patterns observed / target-doc observations

- **Executing the subject's own runtime under a "read-only" intent** (live-install incident) → the delta that
  prevents recurrence: **D4**.
- **A wrong negative claim, scoped to the one jar the agent opened, promoted to a corpus-wide §14 correction and
  propagated to 4 artifacts** before a first-hand decompile disproved it → **D5** (and the already-covered SCOPE
  rule it should have obeyed).
- **Positive control (no anti-pattern):** B482-G1 opened a reconciliation child gap instead of overwriting B392
  — evidence the corrected §14 discipline holds. No delta.

## Tools built, adapted, or outgrown

| # | CREATED (path · purpose) | ADAPTED (kit tool · what it could not express) | OUTGREW | ORACLE | VERDICT (decision · evidence) |
|---|---|---|---|---|---|
| T1 | — | `decompile-java.sh` (Vineflower) · used AS-IS to open `niagarad.jar` and disprove the wrong absence claim (B478) | — | — | `keep-kit` · existing wrapper sufficient; the gap was PROCESS (D5 open-the-named-jar for negatives), not tooling. |
| T2 | — | native RE toolbelt (`corroborate-native.sh` / `ExportDecompiledC.java`) · used AS-IS by the sibling session on `dsfspi`/launcher binaries (B484/B485) | — | — | `keep-kit` · both routes are kit tools; the lesson is the WORKFLOW split (D6), not a new tool. |

No script was written this run; the RE ran on existing wrappers. Honesty clause: nothing else invented.

## Metrics

- **Blocks reviewed**: B477–B485 (~9 blocks, incl. B477 static + B477 §12 live-validation, and B484/B485 from
  the sibling native session) · **§14 self-corrections in this run**: 1 major RETRACTION (B478, wrong-absence
  claim) + 1 reconciliation child gap (B482-G1) + scope refinements (B479/B480/B481) · **live-install
  incidents**: 1 (wrong-version launcher rebuilt the production registry) · **errors + revert**: 1 (wrong
  negative claim propagated to 4 artifacts, reverted at B478)
- **Deltas proposed (new)**: 2 new (D4 HIGH, D6 MED) + 1 new-hybrid reinforcing pending D1 (D5 MED) ·
  **Already-covered lessons**: 6
- **Rules skipped in practice**: 1 (the VERIFY-BEFORE-ACTING absence + SCOPE-widen rule, skipped when the
  wrong `niagarad` negative was accepted — see D5 / Already covered)

## Honest verdict

This run surfaced one genuinely load-bearing SAFETY delta (D4 — a vendor launcher is a mutation, not a read),
one narrow-but-real epistemic delta (D5 — the §14 correction-on-absence guard + D1's negative twin), and one
workflow delta (D6 — peer-session artifact-axis split with dedup handoff). Candidate C is correctly NOT a delta
(it is a positive control proving the §14/CONTRADICTIONS.md discipline now works). The B478 error's PREVENTIVE
rule already lives in the kit (VERIFY BEFORE ACTING + SCOPE-widen) and was skipped rather than absent — so D5 is
scoped to the hook the kit genuinely lacks (routing that discipline into corrections), not a re-proposal of the
covered core. That is why the "Already covered" list is substantial and the new-delta list is short and
specific, as a healthy §18 retro should be.
