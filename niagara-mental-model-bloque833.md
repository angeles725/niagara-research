# B833 — C12 T1 deep-dive: name initializer blocks `<init>` (S2/B832-G4) and make the Case-B `@`-stop paren-aware (S1/B832-G3)

**Focus**: build-n4-module-kit (meta / kit-tooling). **Scope**: source-backed evidence for the two C12 seeds that touch
the shared method-boundary parser — S2 (B832-G4, initializer reach) and S1 (B832-G3, `@`-stop param-annotation FN).
**Sources**: shared fragment `toolbelt/lib/method-boundary.sh` @ kit **66123a2**; reproduced lint-timers runs; client
corpus @ ff1b659 (42 `.java`). Read-only research for the investigador1 lane requested for QA's C12 RED direction.
Companion to [[observed-mutation-must-flip-a-fixture]] and the C12 seeds probe (niagara-research 850791f12).

---

## 1. S2 — where an initializer block lands today, and the minimal change to name it `<init>`

**Reproduced (kit 66123a2, lint-timers):** a multi-line instance initializer whose body sets a FIELD flag beside a
`Clock.schedule` — a real companion-flag hazard (the flag can stay stuck across a stop/restart) — yields **0
companion-flag**. The initializer body is invisible. `[ev: reproduced @ 66123a2]`

**Why**, from `method-boundary.sh`: the initializer's brace-only `{` opens depth 1→2 with `!in_m`. Case A
(`:70`, `identifier(...)[throws]{`) does not match (an initializer line carries no `identifier(`). Case B (`:79-95`,
brace-only backward scan) breaks at the prior field's `;$` (`:85`) before finding any identifier, so `mname == ""` and
the block is never entered (`:99` requires `mname != ""`). Its `startingUp = true` + `Clock.schedule` sit at depth 2 with
`in_m == 0` → in no method body → unscanned. `[ev: method-boundary.sh:70/:79-95/:99 @ 66123a2]`

**A keyword block and an initializer BOTH end at `mname == ""`, but for different reasons** — this is the discriminator
for the fix. A control block `if (x) {` DOES match Case A (`if(...)  {`) and is cleared by I3 (`:74` keyword exclusion) →
`mname == ""`. An initializer matches NOTHING (Case A no-match, Case B no-identifier) → `mname == ""`. **Minimal change
(matches the lead's direction):** carry a `was_kw` flag set when Case A matched an excluded keyword; on a depth-≥2 open
with `mname == ""`, if `!was_kw` (no signature at all) name it **`<init>`** (`<clinit>` when the opening line carries
`static`) and enter it as a body (subject `<init>`); if `was_kw`, keep rejecting (I3 unchanged). This scans initializer
bodies while control blocks stay out. `[ev: method-boundary.sh:70/:74/:99; lead direction 2026-09-07]`

## 2. S2 real-tree expectation — the client corpus has ZERO initializers

Scanned all 42 client `.java` @ ff1b659: **0 static initializers** (`grep -lE 'static[[:space:]]*\{'` → 0 files). Genuine
**instance initializers = 0**: a first-pass "bare `{` with a non-signature prior line" detector reported 14 candidates,
but every one is a FALSE POSITIVE — a multi-line CLASS-body open (`…extends BComponent`\n`{`, 10×, correctly ignored by
the `max_d >= 2` depth guard at depth 1), a `finally {` block (2×), or a multi-line `if`-condition continuation ending in
`)` (2×, `CompressorControl.java:308/:361`). None is an initializer. So **S2's `<init>` fix changes no real-tree count**
— its real-tree pin is "0 `<init>` subjects at ff1b659", and the behaviour is proven only by a synthetic fixture (same
posture as B832-G3/G4). `[ev: client scan @ ff1b659; reproduced multi-line class decl still detects arm()]`

## 3. S1 — the `@`-stop discriminator: paren-awareness beats the end-char rule

**Reproduced FN (66123a2):** `void arm(\n  @Ann int x)\n{ startingUp=true; Clock.schedule(…); }` → **0 companion-flag**
(the method is missed). The Case-B backward scan from `{` hits `@Ann int x)` (starts with `@`) and the unconditional
`@`-stop (`:84`) breaks before reaching `arm(`. `[ev: method-boundary.sh:84 @ 66123a2; reproduced]`

**The `@`-stop's residual purpose is narrow.** With the depth guard already rejecting the class body (depth 1), and a
real method's signature found on the scan BEFORE its leading annotation (`@Override\n void m()\n{` → the scan finds `m(`
first, `@`-stop moot), the `@`-stop only bites when NO signature sits between the `{` and the `@` — i.e. exactly the two
edge shapes: a parameter-annotation continuation (the `@` is INSIDE the signature parens — the FN) or a genuine
no-signature block (now handled by §1's `<init>` naming).

**Two candidate discriminators, with the shapes that break each:**
| rule | param continuation `@Ann int x)` | single-line leading annotation `@Deprecated("x")` | verdict |
|------|----------------------------------|--------------------------------------------------|---------|
| **(a) end-char**: stop only if the `@`-line does NOT end in `)`/`{`/`,` | ends `)` → don't stop ✓ | ends `)` → don't stop ✗ (would over-scan a leading annotation that ends in `)`; mostly MASKED because the method signature is found first, but fragile) | fragile heuristic |
| **(b) paren-aware**: stop only when paren-balanced at the `@`-line; do NOT stop while inside an unclosed signature `(` | `@Ann int x)` is inside the unclosed `arm(` → don't stop → scan reaches `arm(` ✓ | its own `("x")` is balanced → stop ✓ | robust; encodes the semantic (a parameter annotation lives inside the signature parens) |

**Recommendation: rule (b).** It directly models "a parameter annotation is inside the method signature's parens";
rule (a) is an end-char proxy that a single-line annotation ending in `)` defeats. Implementation: during the Case-B
backward scan, maintain a running paren balance (count `(` and `)` per scanned line); treat an `@`-line as a stop ONLY
when the balance is ≥ 0 (not inside an open signature paren). An even simpler equivalent, since the scan's goal is the
method name: do not `@`-stop at all while a `)` has been seen without its matching `(` (i.e. still inside the arg list) —
keep scanning to the `identifier(` that owns it. `[ev: method-boundary.sh:79-95 @ 66123a2; reproduced shapes]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | an initializer body (field+schedule) yields 0 companion-flag today — not entered (Case A no-match, Case B breaks at `;$`) | [CERT] | method-boundary.sh:70/:85/:99; run |
| 2 | keyword block vs initializer both end mname=="" but Case A matched-keyword vs matched-nothing → the `was_kw` discriminator for `<init>` | [CERT] | method-boundary.sh:70/:74 |
| 3 | client corpus @ ff1b659 has 0 static + 0 genuine instance initializers (14 detector hits are class-body/finally/if false positives) | [CERT] | client scan; class-decl reproduction |
| 4 | S1 param-annotation continuation reproduces the FN (0 companion-flag) via the `:84` `@`-stop | [CERT] | reproduced @ 66123a2 |
| 5 | rule (a) end-char breaks on a single-line annotation ending in `)`; rule (b) paren-aware is robust | [INFER] | shape table; §3 reasoning |
Tally: 4 [CERT] · 1 [INFER] · 0 unmarked.

## Connections
- [[observed-mutation-must-flip-a-fixture]] — S1/S2 are the "defensive on the corpus, reachable-but-absent shape" class
  (B832-G3/G4): 0 real-tree count change, synthetic-fixture-pinned only.
- B832 (three-parser-copies) and its gaps G3 (S1) / G4 (S2); this block is their C12 resolution direction.
- C12 seeds probe (niagara-research 850791f12): S1/S2 are seeds 1-2 there.

## Open gaps
- **B833-G1**: the `<init>` naming (§1) may also catch a lambda body `() -> {` or an anonymous-class `new Foo() {` (both
  unnamed depth-≥2 opens) — confirm the `was_kw`/no-signature rule does not over-name those, or scope `<init>` to a
  bare `{`/`static {` opener; needs a lambda + anon-class fixture in QA's RED.
- **B833-G2**: rule (b) paren-awareness must survive a generic method signature carrying `<...>` and a cast `(Foo)` on
  the continuation line; a fixture with `void m(\n  @Ann Map<K,V> x)\n{` confirms the paren counter ignores the generic
  angle brackets and the annotation.
