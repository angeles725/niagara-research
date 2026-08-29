# §18 Self-Retrospective — graphql-admin focus, 2026-08-29

> **Run summary.** Focus: `graphql-admin` (new focus over the mature `niagara-research` multi-focus corpus).
> Blocks written: B611–B619 (9 blocks). Gaps: GQL-G1..G8 closed by investigation + G9 synthesis at STOP.
> Question investigated: can GraphQL administer an N4 station from a custom dashboard module?
> Answer: yes — 100% DIY, SEPARATE module, buildable with graphql-java ≤ v20.
> Date: 2026-08-29. **Review-status: pending.**

This retro covers the kit as of the state read at the start of this run
(`research-sdd/METHODOLOGY.md` + `research-sdd/PROMPT-LOOP.md`). A proposed delta is only raised
when the kit does NOT already cover the technique or rule. Each delta is evidence-backed by block
or iteration-history references.

---

## Proposed deltas

---

### DELTA 1 — Add ENVIRONMENT/VERSION claims as a third named trigger class in the §11 framework-semantic check

**WHAT.** The PROMPT-LOOP §11 framework-semantic check names two trigger classes that require the
driver to re-verify a delegated sweep's interpretation against corpus framework knowledge:
`(a) SECURITY/PERMISSION` and `(b) BEHAVIORAL CAPABILITY`. A third class is missing:
`(c) ENVIRONMENT/RUNTIME-VERSION` — any claim from a delegated sweep about what compiler target,
JVM version, SDK level, or runtime environment the subject actually runs under.

**WHY.** B616 / B617 (iteration 6 → 7):
- The GQL-G5 classloader sweep (sonnet) returned a load-bearing environmental claim: "Niagara N4
  runs on Java 11 (target)." This claim passed the token-check (line references checked out; the
  code citations for the classloader logic were all correct).
- The claim was WRONG: direct class-file major-version measurement of three core runtime classes
  (`BComponent`, `BWebServlet`, `ModuleClassLoader`) gave major = 52 = **Java 8**, which also
  agrees with [B176] (the module build toolchain).
- The driver caught the error via the §11 framework-semantic check against existing corpus knowledge
  ([B176]), not via any explicit "verify version claims" rule.
- A §14 correction was issued (B616 §616.1 corrects B617, back-pointer added), and B617 was written
  with the corrected fact.
- The error class: the sweep INFERRED the Java version from expected/configuration signals (e.g. the
  presence of a Java-11 setup in module tooling artifacts) rather than measuring the actual compiled
  class files. This is exactly the failure mode `(b) BEHAVIORAL CAPABILITY` is designed to catch —
  but the current kit formulation does not signal to the driver that "what runtime does the subject
  actually compile to/run under" is in the same risk class.

**WHERE.** PROMPT-LOOP, §11 framework-semantic check trigger classes. Add:

> **(c) ENVIRONMENT/RUNTIME-VERSION: any claim about what compiler target, JDK version, JVM, SDK
> level, or platform environment the subject actually runs under.** An inference ("target = Java 11")
> passes the token-check even when the actual class files are compiled to a different version.
> Verify ENVIRONMENT/VERSION claims by DIRECT CLASS-FILE MEASUREMENT (`od -An -j6 -N2 -tu2
> --endian=big X.class`, major field = bytes 6-7 big-endian unsigned) before writing or accepting
> the claim — cross-check with any corpus blocks that document the build toolchain.

**PRIORITY.** High. A wrong runtime-version claim can cascade: it directly determines which
library release lines are compatible (graphql-java ≤ v20 vs v21+ are incompatible with Java 8 vs
Java 11 respectively). A wrong version sealed into a block produces wrong feasibility verdicts
downstream without any per-field citation error — the token-check passes because the code citation
lines are correct; only the INTERPRETATION is wrong.

**Partially covered?** The kit's "VERIFY BEFORE ACTING" (PROMPT-LOOP §INVESTIGATE) covers
delegated findings in general: "(a) resolve at least the file:line citations that support a key
claim." But this instruction targets citation-path validation, not semantic-interpretation
validation for a different claim CATEGORY (environmental/version inferences). The §11
framework-semantic check explicitly names two trigger classes; this run demonstrated a real third
class that is not named and that the driver must hunt for independently. The METHODOLOGY §5
twin-binary check (decompiler-dump offset provenance) covers a sibling idea — "verify which
binary produced an offset before citing it" — but is scoped to native-binary dumps and offsets,
not to Java class-file version determination.

---

### DELTA 2 — Add class-file major-version measurement as a named recipe in tool-registry.md

**WHAT.** The one-liner `od -An -j6 -N2 -tu2 --endian=big <ClassName>.class` reads bytes 6-7 of
a Java class file as a big-endian unsigned 16-bit integer, giving the `major_version` field (major
52 = Java 8, 55 = Java 11, 61 = Java 17, etc.). This is a [CERT] measurement from the class file
itself — cheaper than decompiling, does not require `javap`, and works on any `.class` on disk.

**WHY.** B616 §616.1: this command was improvised inline during the run to establish [CERT] proof
of the Java 8 ceiling. It is not listed in tool-registry.md under the JAR/`.class` Java entry.
The tool-registry already lists `javap -p -c` (decompile), `corroborate-java.sh` (full
corroboration suite), and `krak2` (bytecode round-trip) — but none of these is the lightweight
"what Java version is this class file?" answer. A future agent facing the same question will
improvise the command again rather than finding it in the registry.

**WHERE.** `toolbelt/tool-registry.md`, in the JAR / `.class` Java row (or as an adjacent
lightweight-check row). Recipe:

```bash
od -An -j6 -N2 -tu2 --endian=big <ClassName>.class
# → prints the major_version field (big-endian uint16 at offset 6)
# major 52 = Java 8 · 55 = Java 11 · 61 = Java 17 · 65 = Java 21
# (class file magic = 0xCAFEBABE at offset 0; minor at offset 4; major at offset 6)
```

**PRIORITY.** Medium. It is a one-liner, so an agent can rediscover it. But naming it in the
registry (a) gives it a [CERT]-admissible citation form, (b) associates it with the
ENVIRONMENT/VERSION trigger class (DELTA 1), and (c) prevents the exact-same improvisation in
every focus that touches a JAR corpus with version constraints.

**Partially covered?** No. The tool-registry has `javap -p -c` but that decompiles to bytecode
mnemonics — it does not make the major-version field explicit or cheap to extract. The METHODOLOGY
§5 mentions `javap -c <ClassName>` and `rg -a <expected-literal> <ClassName.class>` for string
presence, but neither is a version-measurement recipe.

---

### DELTA 3 — Name "EVIDENCE-grounded DESIGN" as a first-class focus type in BOOTSTRAP §b2

**WHAT.** A focus type that sits between pure EVIDENCE (all new decompilation) and pure
DESIGN/APPLIED (no local source, synthesis only). In an EVIDENCE-grounded DESIGN focus:
- The infrastructure layers are ALL PRE-DECLARED REMITTANCES (the existing multi-focus corpus
  already covers them — cited by [Block N] §N.x, never re-derived).
- Each gap reads a NEW SEAM: 2-4 local source files that are NOT covered by any prior block.
- Each block has TWO halves: an EVIDENCE section (the seam, [CERT] from code) and a DESIGN MAPPING
  section ([INFER] rows mapping the seam to the architectural question). The [INFER]/[CERT] ratio
  is LOW in the evidence sections (~0.2) and jumps in the synthesis/design sections; the whole-block
  ratio lands mid-range (~0.3-0.5), which is EXPECTED for this type and is NOT an exhaustion signal.
- The focus can be bootstrapped with AUDIT-FIRST + PRE-DECLARE REMITTANCES in one pass — the gap
  list is mostly knowable before the first block because the question is architectural (seams are
  known; answers are not).

**WHY.** The graphql-admin focus (B611–B619) is this pattern exactly. The RESEARCH-STATE had to
declare it explicitly ("EVIDENCE-grounded DESIGN/APPLIED") because no kit concept covered it at
the FOCUS level. Without the concept:
- The bootstrap b2 ANGLE prompt ("DECLARE AN EXPLICIT INVESTIGATION ANGLE/AXIS") doesn't distinguish
  this focus type from a pure DESIGN corpus (no local source) or a pure EVIDENCE focus (no design
  mapping). An agent picking the wrong type would either skip the seam reads (treating it as
  DESIGN-only, producing [INFER]-heavy blocks with no [CERT] from code) or skip the design mapping
  (treating it as EVIDENCE-only, missing the architectural output the focus exists to produce).
- The KNOWN-OUTLINE DESIGN CORPUS variant (PROMPT-LOOP BOOTSTRAP) is for pure DESIGN with a
  pre-fixed gap list and no local binary source — it applies only the scout-and-author pattern, not
  the seam-read + design-map dual-section shape.

**WHERE.** PROMPT-LOOP BOOTSTRAP §b2 (ANGLE declaration for mature/large targets), as an
additional named axis type alongside existing examples. Suggested addition:

> **EVIDENCE-grounded DESIGN axis.** Use when the question is architectural/feasibility over a
> mature corpus whose infrastructure layers are already covered by prior blocks. Pattern: (1)
> PRE-DECLARE all covered layers as REMITTANCES (see BOOTSTRAP AUDIT-FIRST backlog); (2) each gap
> reads a NEW SEAM (2-5 local files) and maps the seam to the architectural question; (3) each
> block has two halves — EVIDENCE (seam, [CERT] from file:line) and DESIGN MAPPING ([INFER] rows,
> expected). The [INFER]/[CERT] ratio per block is moderate (~0.3-0.5) and is EXPECTED, not an
> exhaustion signal. Declare `Type: EVIDENCE-grounded DESIGN` in the focus angle for clarity.

**PRIORITY.** Medium. The run succeeded without this concept, and the block-level EVIDENCE /
DESIGN/APPLIED typing (§11 marker tally) provided enough signal. But naming the focus-level type
prevents a future agent from treating the [INFER] ratio as a problem and either over-delegating
(trying to decompile 20 files per gap to "fix" the ratio) or under-investigating (treating
high-ratio blocks as done when the design mapping is the entire deliverable).

**Partially covered?** Partially. The per-BLOCK typing (EVIDENCE / DESIGN/APPLIED / collaborative
bridge) is covered in METHODOLOGY §4 and PROMPT-LOOP §11. The §16 multi-focus pattern covers
parallel axes in the same corpus. But the FOCUS-LEVEL type "hybrid: EVIDENCE seam reads +
DESIGN mapping, with heavy PRE-DECLARED REMITTANCES" is not named anywhere; it is a different thing
from both a per-block type declaration and a multi-focus axis split.

---

### DELTA 4 — Note that investigating a sibling gap while a delegated sweep is in flight is a valid momentum tactic

**WHAT.** When a high-priority gap's sweep is delegated to a sub-agent (still running), the driver
can advance a sibling gap of equal priority inline, then return to the delegated gap once its sweep
arrives. The gap-order in the backlog is then NOT the block-write order (G3 before G2 here, while
G2's sweep was in flight).

**WHY.** Iteration history: GQL-G1 (B611, inline) → GQL-G3 (B612, inline, while G2 sweep ran) →
GQL-G2 (B613, delegated sonnet + inline verify). Both G2 and G3 were high-priority; G3 was a
2-file read suitable for inline work while G2's 4-file rpc sweep was delegated. The PROMPT-LOOP
says "take the highest-priority NOT covered gap from the backlog" — when two gaps share the same
priority tier, the driver has latitude, but the specific pattern of "advance a sibling gap while
a sweep is in flight" is not mentioned.

**WHERE.** PROMPT-LOOP NORMAL CYCLE §INVESTIGATE (DELEGATE heavy sweeps rule), as a one-sentence
note:

> When a heavy sweep for gap N is in flight (delegated sub-agent), investigate a sibling gap of
> equal priority inline rather than waiting. The block-write order may then differ from the
> gap-order; this is fine — number blocks sequentially and record the actual gap-closed in the
> iteration-history row.

**PRIORITY.** Low. The kit does not forbid this and the loop's idempotent state tracking handles
it naturally (gaps tracked by ID, not by position). The RESEARCH-STATE iteration history correctly
records which gap was closed in each iteration regardless of order. The kit's "CHOOSE: take the
highest-priority NOT covered gap" already permits latitude within a priority tier. This is a small
clarity note, not a rule gap.

**Partially covered?** Implicitly yes (priority-tier latitude in CHOOSE; sequential block numbering
convention). The specific in-flight sweep tactic is not named, but no kit rule contradicts it.

---

## NOT proposed (already covered — dedupe evidence)

The following were examined and found covered by the kit. Not proposing them avoids duplicates.

| Candidate | Kit location | Coverage verdict |
|---|---|---|
| PRE-DECLARE REMITTANCES before audit sweep (new focus over mature corpus) | PROMPT-LOOP BOOTSTRAP AUDIT-FIRST BACKLOG — "PRE-DECLARE REMITTANCES FIRST … before delegating the audit sweep" | **Fully covered.** This run followed it correctly. |
| DESIGN/APPLIED block ratio (high [INFER]/[CERT] expected) | PROMPT-LOOP §11 marker tally — "For a DESIGN/APPLIED block … a high ratio is EXPECTED and healthy, NOT an exhaustion signal" | **Fully covered at block level.** (DELTA 3 addresses the missing FOCUS-level type, not the per-block ratio.) |
| Synthesis block at focus close | PROMPT-LOOP §7 TERMINAL TRIGGER — "OPTIONALLY write a focus-closing SYNTHESIS block first" | **Fully covered.** G9/B619 followed this. |
| Delegated sweep + driver inline re-verify pattern (general case) | PROMPT-LOOP INVESTIGATE — "VERIFY BEFORE ACTING on a sub-agent's report" + §11 framework-semantic check | **Covered in general.** DELTA 1 adds the specific ENVIRONMENT/VERSION trigger class that the general rule does not call out. |
| §14 correction back-pointer to OLD block | PROMPT-LOOP §5 — "§14 BACK-POINTER CHECK … confirm the OLD BLOCK was actually edited to add the back-pointer note" | **Fully covered.** B616 corrects B617's Java-version claim; the back-pointer was added to B617. |
| String-scrubbed decompiled Java caveat | METHODOLOGY §5 — "Decompiler-string-scrubbed Java (Vineflower / Procyon)" | **Fully covered.** B611 §611.2 applied it correctly (ContextFilter producer cited by class+structure, not scrubbed literal). |
| SOURCES.md preservation for load-bearing [CERT-web] claims | METHODOLOGY §5 + PROMPT-LOOP §5 MCP-doc snapshots gate | **Fully covered.** B616 snapshotted the graphql-java Java-11 announcement to sources/web-snapshots/ and registered it in SOURCES.md. |
| Focus opened with no local binary to profile (DESIGN/APPLIED census) | PROMPT-LOOP BOOTSTRAP a/a2 DESIGN corpus exception | **Covered.** The focus correctly noted "no new census — subject artifacts already extracted under organized/". |

---

## Summary

Four deltas proposed (descending priority):
1. **High** — §11 third trigger class: ENVIRONMENT/RUNTIME-VERSION claims from delegated sweeps must be driver-verified by direct class-file measurement, not accepted on the sweep's inference.
2. **Medium** — tool-registry recipe: `od -An -j6 -N2 -tu2 --endian=big X.class` for Java major-version [CERT] measurement.
3. **Medium** — BOOTSTRAP §b2: name "EVIDENCE-grounded DESIGN" as a first-class focus axis type, distinct from pure DESIGN/APPLIED and pure EVIDENCE corpora.
4. **Low** — NORMAL CYCLE note: investigating a sibling gap while a delegated sweep is in flight is a valid momentum tactic; block-write order may differ from backlog order.
