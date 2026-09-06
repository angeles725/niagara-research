# Campaign 12 — exploration draft

Author: companero (Fable), 2026-09-06. Phase: explore (pre-proposal). Same shape as the C11 draft. Mandate = investigador1's
evidence-anchored C12 seeds (`2026-09-07-c12-seeds.md`, `850791f12`) + any C11-close follow-ups; product P1-P5 stay gated on
Cristian. Kit v0.22.0 (main `e55b369`, tag `66123a2`); client main `00e7118`; tunnel PR4/5/7 blessed, awaiting Cristian.
`[ev: C12 seeds 850791f12]` `[ev: kit v0.22.0]`

## 0. PREREQUISITES — NOT C12 work, but they MUST precede any C12 CLIENT jar (unchanged)
1. The pending deploy chain (base 2.0.7/2.0.3/2.1.1 + the C9 bumps → PANCCADIA, per the runbook delta).
2. The niagaraTest harness session (C9 harness-only alarm pins; C12 alarm/adapter work inherits it).
3. Tunnel merge (PR4/5/7 + config.env keys).
State to Cristian as gates; the KIT lanes (S1-S4) need none.

## 1. Ranked backlog (value × tractability)
| # | Item | Class | Value | Tract. | Requires-exec | RED to author |
|---|---|---|---|---|---|---|
| S1 | **shared parser `@`-stop param-annotation FN** (B832-G3, `method-boundary.sh:84`): a method whose signature spans lines ending in a PARAMETER annotation (`@Nullable`, `@NiagaraProperty` on a param) hits the `@`-stop in the Case-B backward scan → the method is NOT named → its body is invisible. REPRODUCED @ 127f25a; absent from the client corpus | KIT | **High** (a live FN in the fragment C11 just shipped — correctness, all 3 lints) | Med (B833 §3: make the `@`-scan **paren-aware** — stop only when paren-balanced, never inside an unclosed signature `(`; NOT the end-char rule, which a single-line annotation ending in `)` defeats; carry B833-G2: the paren counter must ignore generics `<K,V>`/casts) | WSL | golden case: multi-line signature with a param annotation → the method IS named |
| S2 | **initializer/`<clinit>` companion-flag MISS** (B833 `7d6250b40`, reframes B832-G4): the PRIMARY point is a POSITIVE missed hazard — an instance/static **initializer** BODY that sets a FIELD flag beside a `Clock.schedule` is a real companion-flag hazard the fragment MISSES today (a no-signature depth≥2 brace-only `{` is never named: Case A no-match, Case B breaks at the prior `;$`). REPRODUCED by me: `{ armed=true; t=Clock.schedule(…); }` → **0 companion-flag** (only timer-ticket); the same body in a method FAILs. Fix: NAME the no-signature depth≥2 brace-only block **`<init>`/`<clinit>`** and scan it (subject `<init>`), with a `was_kw` flag so I3 still REJECTS keyword blocks (`for`/`if`) | KIT | **High** (a live companion-flag FN — correctness) | Med (name+scan `<init>`; carry B833-G1: `<init>` must NOT over-catch a lambda `() -> {` / anon-class `new Foo() {`) | WSL | **TWO** golden cases: (a) POSITIVE initializer body field=true + Clock.schedule → FAIL subject `<init>`; (b) NEGATIVE `for`/`if` inside an initializer → not named |
| S3 | **`toolbelt/sweep-markers.sh` routed conflict-marker lint** — promote the C11 CLOSE-no-conflict-markers point-check (a bats assertion) to a REUSABLE routed lint over the tree (the C8-archive leftover markers, latent since C8, motivate it) | KIT/tooling | Med (a routed lint catches markers anywhere, not only at close) | High (a small grep-based lint + BUILD-LOOP §5 routing + kit-links) | WSL | sweep-markers.bats: a file with a marker → FAIL exit 1; clean tree → 0 |
| S4 | **S23 fixtures lack a comment-only decoy (SC-10)** — the cross-cutting rule requires every fixture to carry a comment-only decoy that does NOT satisfy the pin; the C10 S23 (silent-protection Pattern-B) fixtures were flagged in C10 verify as lacking one | KIT/test hygiene | Low-Med (closes an SC-10 gap; guards against a comment satisfying the Pattern-B pin) | High (add a `//`/`/* */` decoy token to each S23 fixture; re-confirm no verdict flip) | WSL | S23-decoy: a `newOffnormalAlarm` inside a comment does NOT surface the trip |
| P1-P5 | viewer re-auth · HMI per-operator RBAC/VIEW · airDefrost · intercambiador · coolOnSensorFault | PRODUCT | (unchanged from C11) | — | gated on Cristian | (unchanged) |

## 2. Dependencies
- S1 and S2 are both in `method-boundary.sh` (the C11 shared fragment) — the natural C12 KEYSTONE is hardening that fragment;
  land S1+S2 together (one parser PR). The keystone golden set is **3 cases** (S1 ×1 param-annotation-named; S2 ×2 =
  positive `<init>` hazard + negative keyword-in-initializer) **plus 2 edge fixtures** (B833-G1 lambda/anon-class not over-caught
  as `<init>`; B833-G2 paren counter ignores generics/casts) — NOT "two golden cases". Then every invariant has a
  reachable-shape fixture, closing the C11 "defensive not inert" reclassification.
- S3, S4 independent, WSL-only.
- P1 supersedes the C9 shared-password step-up; P2 depends on P1 + the attribution-vs-RBAC answer (attribution is already
  shipped via R14 — the open decision is per-operator RBAC/VIEW); P3/P4/P5 gated on Cristian's three station answers.

## 3. Risks
- **S1/S2 touch the fragment three lints now source** — a golden-set regression MUST be captured before the parser edit
  (same discipline as C11 T1); the fix must not shift any existing golden case, only add the two new ones.
- Harness dependency (P2/P3/P4 station-only) and the product-vs-tooling scope split — unchanged from C11.

## 4. Requires-execution gates
- KIT (S1-S4): WSL bats + golden/real-tree; no station. S1/S2 need the 3×3 baseline identity check (only the two new shapes flip).
- PRODUCT (P1-P5): unchanged; none start before §0.

## 5. Recommendation
Open C12 with the **fragment-hardening pair (S1 + S2)** — WSL-only, closes the two remaining reachable-shape FNs in the C11
parser, completes the invariant/fixture set. Then S3 (sweep-markers routed lint) and S4 (S23 decoy) as independent hygiene.
Hold P1-P5 behind §0 and Cristian's answers.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | S1 @-stop param FN; S2 initializer companion-flag MISS (positive hazard) — both reproduced | [CERT] | C12 seeds 850791f12 + B833 7d6250b40; my run: initializer body → 0 companion-flag, same body in a method → FAIL |
| 2 | S1/S2 both in the C11 shared fragment | [CERT] | method-boundary.sh @ kit main |
| 3 | S3 promotes the C11 CLOSE-no-conflict-markers point-check | [CERT] | C11 close (46d3eff) |
| 4 | S4 = C10-verify SC-10 gap on the S23 fixtures | [ev] | C12 seeds S4 |
