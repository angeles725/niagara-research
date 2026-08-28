<!-- review-status: pending -->
<!-- kit-retro: include -->

# §18 Retrospective — kitControl focus (2026-08-28)

**Run**: niagara-research, focus `kitControl`, 2026-08-28
**Blocks written**: B536–B549 (13 investigable gaps KC1–KC13 + focus synthesis B549)
**Coverage**: 13/13 investigable gaps closed; 1 requires-execution deferred (KC13-G1)
**Reviewer**: fresh-context §18 retro agent (sonnet-4.6)

---

## Summary

14 blocks in one focus run. The run was clean on discipline: tier-tagging held, inline token-verify
picked up where verify-block couldn't, scoping judgments were tested rather than accepted, and a
mid-run operator request was absorbed cleanly. Three genuinely new kit gaps surfaced.

---

## Delta proposals

### D1 — verify-block citation gate is BLIND for all-decompiled-source blocks (NEW, HIGH)

**What happened.** verify-block.sh reported "ZERO file:line citations resolved — the citation gate
checked nothing and exits 0 silently" on B542, B543, B546, and B547. Every `[CERT]` citation in
those blocks points into `organized/<module>/vineflower/` trees — decompiled paths that verify-block
classifies as `extern` and explicitly skips. The script exits 0 regardless of how many `[CERT]`
rows go unresolved. The driver token-verified inline and the blocks are sound, but the mechanized
citation gate provided ZERO protection.

**What the kit says.** PROMPT-LOOP step 5: "`extern` citations (beautified/decompiled/snapshot)
are not script-verifiable — still token-check those by reading." This one sentence names the
limitation, but it does not tell the agent:
- that when ALL citations are in decompiled trees, verify-block's citation-resolution section is
  completely inert;
- that the WARN printed is expected, not a sign of a broken block;
- that inline token-verify is then the SOLE citation gate, and the self-verify report must explicitly
  state this (e.g., "verify-block: 0 file:line resolved — all citations in organized/vineflower
  trees (extern); sole gate = inline token-verify").

**Gap.** An agent seeing the WARN for the first time could interpret it as a block defect, waste time
chasing it, or (worse) silently accept the gate as "passed" without realizing it checked nothing.

**Proposed delta** — add to METHODOLOGY §11 (or the verify-block sub-section of PROMPT-LOOP step 5):

> **verify-block citation gate: BLIND for decompiled-tree blocks.** When a block's `[CERT]`
> citations all point into decompiled trees (`organized/*/vineflower/`, `organized/*/procyon/`,
> `audits/*.c`, etc.), verify-block classifies them as `extern` — it prints "ZERO file:line
> citations resolved" and exits 0. This WARN is EXPECTED, not an error: the script cannot follow
> a decompiler output path. The mechanized citation gate has checked nothing for that block; the
> burden falls ENTIRELY on the inline token-verify in step 5. Self-verify must record this
> explicitly — "verify-block: 0 resolved (all extern — decompiled trees); sole citation gate =
> inline token-verify N/M rows" — so the omission is visible, not silently assumed covered.
> Separately: a SOURCE_ROOT mapping (not yet implemented) would let the script resolve decompiled
> paths; the absence of that config is the root cause. Until it exists, inline token-verify is
> non-negotiable for any decompile-based block.

**Evidence**: B542/B543/B546/B547 self-verify sections; run driver transcript B542 note "5/10 rows
token-verified" and similar patterns across decompile-heavy blocks.
**Target file**: METHODOLOGY §11 (certainty audit / step-5 self-verify contract)
**Also touch**: PROMPT-LOOP step 5 extern note (one sentence → one paragraph)
**Priority**: HIGH — affects every decompile-based block across all targets.

---

### D2 — Mid-loop operator-injected gap run parallel to an in-flight sweep (NEW, MEDIUM)

**What happened.** At iteration 7 (KC7/B542 sweep in flight), the operator requested a new
high-priority gap: KC13 (HVAC control-logic SAFETY). The loop added KC13 to the backlog and ran
its sweep concurrently with the in-flight KC7 sweep — two independent `sonnet` sub-agents,
independent modules (`honeywellFunctionBlocks-rt` vs `kitControl-rt`+`clHVAC*`), no shared state.
Both returned. KC7 was written first (B542), KC13 second (B543). The pattern worked cleanly.

**What the kit says.** §16 covers multi-focus parallelism (two independent focus axes). BOOTSTRAP
step e says to seed gaps up-front. NORMAL CYCLE step 3 delegates heavy sweeps. But the kit has
NO rule for the specific "operator injects a new gap mid-loop → add to backlog + run its sweep
concurrently with an in-flight one" pattern. It is not mentioned as legal, illegal, or constrained.

**Why it needs a rule.** Without documentation the pattern is invisible to future runs, and the
natural instinct is to queue the new gap (wait until KC7 is done) or abandon the in-flight sweep.
The parallel route is safe only under two conditions:
1. The two sweeps read independent source trees — no shared mutable state (§16's same constraint).
2. Only ONE block is written at a time (the one-block-per-commit rule is not relaxed).

**Proposed delta** — add to NORMAL CYCLE step 1 (CHOOSE) or §16:

> **Operator-injected gap (mid-loop parallel).** When the operator adds a new high-priority gap
> while a sweep for the current gap is already in flight (two concurrent `Agent/Task` calls), it is
> safe to launch the new gap's sweep concurrently PROVIDED: (a) the two sweeps read INDEPENDENT
> source trees (no shared mutable state — the same constraint as §16 concurrent scouts); (b) the
> driver serializes BLOCK WRITING — one block per commit, as usual. Add the new gap to the backlog
> IMMEDIATELY with `status: pending` and record the injection timestamp in the iteration-history
> row. Both sweep results return; write the first-finishing block, then the second. This is NOT a
> §16 multi-focus split (the gaps share one focus); it is a cost-discipline exception to sequential
> sweep dispatch.

**Evidence**: iteration-history rows 7 (KC7→B542) and 8 (KC13→B543); KC13 marked
"OPERATOR-REQUESTED 2026-08-28" in RESEARCH-STATE; B543 header note.
**Target file**: PROMPT-LOOP NORMAL CYCLE step 1 (CHOOSE), or a short callout in §16
**Priority**: MEDIUM — the pattern is useful and safe; without documentation it stays invisible.

---

### D3 — Count discrepancy between blocks = scope-first diagnostic, not a §14 trigger (NEW, MEDIUM)

**What happened.** Three times in this focus, a new block's class count differed from a count in
a prior block:

| Block pair | Count A (old) | Count B (new) | Discrepancy |
|------------|---------------|---------------|-------------|
| B103 → B542 | 158 java (honeywellFunctionBlocks) | 146 rt-only | −12 |
| B87 → B540 | 264 clHVAC (B87) | 250 rt-only (B540) | −14 |
| B105/B242 → B546 | "163" IRM FBs | 203 vf / 140 factory FBs | +40/−23 |

In every case, the apparent discrepancy was a SCOPE DIFFERENCE: the older count measured rt+ux+wb
jars; the newer count measured the `*-rt` jar only. Once each count was labeled with its scope,
BOTH were correct. No §14 correction was issued. B542 §542.1 explicitly recorded: "not a B103
error — a scope clarification (same pattern as clHVAC 264=rt+wb vs 250=rt)."

**What the kit says.** METHODOLOGY §14 covers scope-clarification: "scope-clarification means the
prior claim was RIGHT for a DIFFERENT artifact/build." GAP NUMBERS ARE HYPOTHESES says to
re-derive counts before using them as denominators. But neither rule explicitly says: BEFORE
deciding whether a count difference is a §14 defect or a §14 scope-clarification (or neither),
first answer "are both counts measuring the same artifact set?" This diagnostic step is implied but
not named, and "artifact set" for compiled Java specifically means "which JARs were counted."

**Why it matters.** Without this heuristic, an agent finding 158 (old) vs 146 (new) is likely to
treat it as a discrepancy requiring §14 correction. The correct move — "check scope first" — is
not obvious from the existing rules.

**Proposed delta** — add to METHODOLOGY §14 (scope-clarification sub-section) or to BOOTSTRAP
step e2 (MEASURE):

> **Count discrepancy: check scope before §14.** When a newly measured count differs from a prior
> block's count for the same component, BEFORE deciding whether a §14 correction is needed, answer:
> "does each count measure the same artifact set?" For compiled Java (OSGi bundles), the artifact
> set is which JARs were counted — `-rt` only vs. `-rt` + `-ux` + `-wb`. A count over `*-rt` is
> correct for runtime classes; a count over all three jars is correct for "the full module." Both
> can be right. Document the scope of each count explicitly (e.g. "158 = rt+ux+wb; 146 = rt only")
> and record the resolution as a scope clarification in the new block's Connections, NOT as a §14
> correction on the old block. A §14 is warranted only when the SAME artifact set was counted twice
> and the counts differ. (Evidence: B542 §542.1 — honeywellFBs; B540 §540.1 — clHVAC;
> B546 §546.1 — honIrmControl; three occurrences in one focus.)

**Target file**: METHODOLOGY §14 (correction / scope-clarification sub-section)
**Priority**: MEDIUM — prevents both false §14 corrections AND invisible scope assumptions.

---

## Confirmed correct applications (no new delta needed)

### C1 — Scoping judgments as hypotheses (ALREADY IN KIT)

KC3 (programming rules) was re-scoped from a code re-derivation to an official-doc synthesis after
finding B6 §6.2 already covers the code kernel. KC9 (composites) was re-scoped from a full
composite deep-dive to a bounded code-model confirmation. Both re-scopes were clean: RESEARCH-STATE
updated, prior block cited as REMITTANCE, new block's scope adjusted.

This is ALREADY IN KIT (NORMAL CYCLE step 3 "SCOPING JUDGMENTS ARE HYPOTHESES" + §14 REMITTANCE).
The run applied it correctly. No delta needed — noting it as a confirmed pattern.

### C2 — Inline tier discipline with constraint annotation (ALREADY IN KIT)

KC9/B545, KC11/B547, KC12/B548 were done inline (no sub-agent) per cost discipline. Each was
recorded as `no · inline (constraint: narrow gap / bounded read / completeness enumeration)` in
the iteration-history tier column. The kit already requires this (PROMPT-LOOP step 3 DELEGATE:
"Record a constrained inline run as `inline (constraint: <reason>)` in the tier column").

Confirmed clean application. No delta needed.

---

## Run-level notes (not kit deltas)

- **Java 8 bytecode confirmed** [CERT]: class major 52 in `kitControl-rt` and `program` module
  (`program` bundles `javac` 1.8). This resolved the operator's open question. Recorded in B543.
- **Four-ecosystem finding** (B549 §549.1) is one of the most architecturally significant findings
  of the niagara corpus: N4 hosts kitControl (event-driven JVM), clHVAC (roster JVM), honeywellFBs
  (scan JVM), and honIrmControl (hardware scan) — four completely independent control stacks sharing
  no code. First time this was stated in one place with evidence citations.
- **FALSIFY-BEFORE-REPORTING** applied correctly to B543: every SAFE/UNSAFE verdict was
  token-verified against source before being stated as an operator recommendation. The kit rule
  (NORMAL CYCLE step 3) was followed without being prompted.
