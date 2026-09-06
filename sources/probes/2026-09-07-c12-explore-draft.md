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
| S1 | **shared parser `@`-stop param-annotation FN** (B832-G3, `method-boundary.sh:84`): a method whose signature spans lines ending in a PARAMETER annotation (`@Nullable`, `@NiagaraProperty` on a param) hits the `@`-stop in the Case-B backward scan → the method is NOT named → its body is invisible. REPRODUCED @ 127f25a; absent from the client corpus | KIT | **High** (a live FN in the fragment C11 just shipped — correctness, all 3 lints) | Med (tighten the `@`-stop to skip a param-annotation continuation; a golden fixture) | WSL | golden-parser case: multi-line signature with a param annotation → the method IS named |
| S2 | **I3 keyword exclusion reachable in a non-entered initializer** (B832-G4, `method-boundary.sh:74/:89`): the `!in_m` gate blocks nested control blocks inside a method, but an instance/static **initializer** `{` opens depth ≥ 2, so a `for`/`if` inside it can be mis-named. REPRODUCED: a `for` in an instance initializer flips 0 (I3) → 1 (drop I3) on lint-timers; absent from the client corpus | KIT | Med-High (decides whether I3 stays or the initializer shape earns its own fixture — closes the "each invariant defensive with a reachable shape" set) | Med | WSL | golden case: a `for` inside an instance initializer → not named a method |
| S3 | **`toolbelt/sweep-markers.sh` routed conflict-marker lint** — promote the C11 CLOSE-no-conflict-markers point-check (a bats assertion) to a REUSABLE routed lint over the tree (the C8-archive leftover markers, latent since C8, motivate it) | KIT/tooling | Med (a routed lint catches markers anywhere, not only at close) | High (a small grep-based lint + BUILD-LOOP §5 routing + kit-links) | WSL | sweep-markers.bats: a file with a marker → FAIL exit 1; clean tree → 0 |
| S4 | **S23 fixtures lack a comment-only decoy (SC-10)** — the cross-cutting rule requires every fixture to carry a comment-only decoy that does NOT satisfy the pin; the C10 S23 (silent-protection Pattern-B) fixtures were flagged in C10 verify as lacking one | KIT/test hygiene | Low-Med (closes an SC-10 gap; guards against a comment satisfying the Pattern-B pin) | High (add a `//`/`/* */` decoy token to each S23 fixture; re-confirm no verdict flip) | WSL | S23-decoy: a `newOffnormalAlarm` inside a comment does NOT surface the trip |
| P1-P5 | viewer re-auth · HMI per-operator RBAC/VIEW · airDefrost · intercambiador · coolOnSensorFault | PRODUCT | (unchanged from C11) | — | gated on Cristian | (unchanged) |

## 2. Dependencies
- S1 and S2 are both in `method-boundary.sh` (the C11 shared fragment) — the natural C12 KEYSTONE is hardening that fragment;
  land S1+S2 together (one parser PR, two golden cases) so the fragment's invariant set is complete (every invariant has a
  reachable-shape fixture — closes the C11 "defensive not inert" reclassification).
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
| 1 | S1 @-stop param-annotation FN; S2 initializer reachability — both reproduced | [CERT] | C12 seeds 850791f12 (method-boundary.sh:84, :74/:89; reproduced @ 127f25a) |
| 2 | S1/S2 both in the C11 shared fragment | [CERT] | method-boundary.sh @ kit main |
| 3 | S3 promotes the C11 CLOSE-no-conflict-markers point-check | [CERT] | C11 close (46d3eff) |
| 4 | S4 = C10-verify SC-10 gap on the S23 fixtures | [ev] | C12 seeds S4 |
