# C12 explore-draft second read — niagara-research bf39a4657 (companero) + B833 fold

investigador1, 2026-09-07. Second read of companero's C12 explore draft
(`sources/probes/2026-09-07-c12-explore-draft.md` @ bf39a4657). `[ev: git; B833 7d6250b40]`

## Verdict: sound consolidation, recommendation right (S1+S2 fragment-hardening keystone). ONE substantive refinement — S2's scope, because the draft predates B833.

## Accurate + endorsed
- S1 (@-stop param-annotation FN): anchor `method-boundary.sh:84`, reproduced, corpus-absent — correct; RED "a multi-line
  signature with a param annotation → the method IS named" is the right positive case.
- S3 (sweep-markers.sh routed lint), S4 (S23 comment-only decoy) — faithful to seeds S3/S4; REDs correct.
- Dependencies/§0/recommendation: S1+S2 land together as the parser keystone (same discipline as C11 T1: capture the
  golden-set regression first, only the new shapes may flip); P1-P5 gated. All sound.

## Finding — S2 is under-scoped: it names only the NEGATIVE guard, not the primary hazard the initializer body carries
The draft frames S2 (line 18) as "I3 keyword exclusion reachable in a non-entered initializer … a `for`/`if` inside it
can be mis-named … golden case: a `for` inside an instance initializer → NOT named a method." That is only half the story —
the **negative** guard (I3 keeps rejecting keyword blocks). It comes straight from my seeds file, which framed B832-G4 as
"I3 reachability". **B833 (7d6250b40) and the lead's direction reframe S2's primary point**: an initializer BODY that sets
a FIELD flag beside a `Clock.schedule` is a real companion-flag hazard that the fragment MISSES today (reproduced: 0
companion-flag — the brace-only initializer `{` is never entered, Case A no-match + Case B breaks at the prior `;$`). The
fix is to **NAME a no-signature depth-≥2 brace-only block `<init>`/`<clinit>` and scan it as a body (subject `<init>`)**,
using a `was_kw` flag to keep I3 rejecting keyword blocks. So S2 needs updating:
- **Scope**: "name initializer/`<clinit>` blocks `<init>` and scan them; I3 still rejects keyword blocks" — not just
  "keep I3".
- **Golden cases (TWO, not one)**: (a) POSITIVE — an initializer body with `field=true` + `Clock.schedule` → **FAIL,
  subject `<init>`** (the missed hazard); (b) NEGATIVE — a `for`/`if` inside an initializer → NOT named a method.
- **S1 discriminator** (also from B833): recommend the paren-aware rule (stop the `@`-scan only when paren-balanced, never
  inside an unclosed signature `(`), not the end-char rule (which a single-line annotation ending in `)` defeats).
- **Two edge gaps to carry** (B833-G1/G2): does `<init>` naming over-catch a lambda `() -> {` or anon-class `new Foo() {`;
  and the paren counter must ignore generics `<K,V>`/casts on the continuation line.
Recommend folding B833 (§1/§3) into the draft's S1/S2 and bumping the keystone from "two golden cases" to **three**
(S1 ×1, S2 ×2) plus the B833-G1/G2 edge fixtures. `[ev: B833 7d6250b40 §1/§3; draft line 18]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | S1/S3/S4 accurate to seeds; recommendation (S1+S2 keystone) sound | [CERT] | draft vs seeds/B833 |
| 2 | draft S2 names only the negative guard (I3 keeps rejecting); misses the initializer-body hazard | [CERT] | draft line 18 |
| 3 | B833: initializer body field+schedule = 0 companion-flag today → name `<init>` + scan (positive case) | [CERT] | B833 §1 (reproduced) |
| 4 | S2 needs 2 golden cases + S1 paren-aware discriminator + B833-G1/G2 edges | [INFER] | B833 §1/§3 |
Tally: 3 [CERT] · 1 [INFER] · 0 unmarked.
