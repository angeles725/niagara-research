# B820 · The "demand-in-scope" static check — the one NEW write-path lint from B819 §819.5: a stage/on-off decision that reads a PROCESS VARIABLE with NO demand input reachable is flaggable; where it is statically decidable (WARN) vs human-review (advisory); proven PASS on `CompressorControl.step`, FAIL on a demand-removed mutant `[CERT]`

> **Scope** (narrow, by the lead's rule — do NOT re-derive the rest of the write-path family, which campaign 8 already
> lands): the write-path lint family is mostly built — (a) matrix-row coverage for OPERATOR/dashboard-linked slots =
> PR19 `lint-write-path.sh`, (c) `Clock.schedule(<=0)` = PR1 `lint-delays.sh` (merged), (d) a dashboard write into a
> `LINK_TARGET` slot = bog-audit CHECK12. This block designs ONLY the remaining piece from [B819] §819.5: the
> **demand-in-scope** check — a staging/on-off decision that commands capacity from a process variable but has no
> demand/enable input in the same decision scope (the "pressure without demand" shape) — its statically-decidable vs
> advisory boundary, and a real-shape PASS/FAIL proof on our `CompressorControl`.
>
> **Sources**: FUENTE 1 — [B819] §819.5 (the lint candidate this concretizes), [B816] §816.6 (write-path matrix),
> [B788]/[B805] (the C6/C8 lintable-vs-advisory doctrine), `CompPan-rt/CompressorControl.java` (client main). Kit
> REMITTANCE (existing gates, cited not redesigned): PR19 `lint-write-path.sh` (wave3 R19.1-R19.6, RED `qa/c8-write-path`
> 5e357d1), PR1 `lint-delays.sh` (merged; present in `toolbelt/`), bog-audit CHECK12 (PR10/PR19 R19.5).

---

## 820.1 — What is ALREADY a gate (REMITTANCE — do not redesign) `[CERT-doc via wave3/kit]`
- **(a) matrix-row coverage** — every OPERATOR/dashboard-linked writable slot has a write-path matrix row/test →
  **PR19 `lint-write-path.sh`** (R19.1-R19.6, RED `qa/c8-write-path` 5e357d1).
- **(c) non-positive delay** — a `Clock.schedule`/`schedulePeriodically` reachable with a `<= 0` delay → **PR1
  `lint-delays.sh`** (merged; verified present in `build-n4-module-kit/toolbelt/`).
- **(d) link-target write** — a dashboard write into a `LINK_TARGET` slot (silently overwritten, [B816] §816.2) →
  **bog-audit CHECK12** (PR10/PR19 R19.5).
B820 adds none of these; it adds only (b) below.

## 820.2 — The NEW check: demand-in-scope `[INFER — the check design; CERT for the shape it keys on]`
The shape ([B819]): a method that COMMANDS an output as a function of a PROCESS VARIABLE (a suction/pressure/
temperature/`cv` input) must have a DEMAND/ENABLE input REACHABLE in the same decision scope. If a staging/on-off
decision reads a process variable to set capacity but NO demand-shaped input is in scope, the process can run to
defend the variable with no one calling — the "why can't it turn off" failure mode.
**The static key** (what a lint can look at): within a control decision method (one that writes an output slot / a
`cmd`/`target`/`setBool` from a numeric input), does the method's parameter list OR the enclosing class's fields
contain a DEMAND-shaped input — a name/type in {`demand*`, `*call*`, `enable`, `*count`, `loopEnable`, an `in`
BStatusBoolean} — in addition to the process-variable input? Absence of any demand-shaped input in scope = the flag.

## 820.3 — The boundary: statically decidable (WARN) vs human-review (advisory) `[INFER, grounded in B819-G1]`
- **Statically decidable → WARN** (never a hard FAIL): a control/staging method references a process-variable-typed
  input (suction/pressure/temp/cv) but its scope (params + enclosing-class fields) contains ZERO demand-shaped input.
  The ABSENCE of any demand input is decidable by name/type scan → WARN. (Not FAIL: a legitimate pure-modulator block
  driven by an upstream demand gate would false-positive on a hard FAIL — WARN keeps it advisory.)
- **Human-review → advisory**: whether a present input is SEMANTICALLY "demand" (decides whether) vs a "modulator"
  (decides how much) is a data-flow + naming judgment a static scan cannot settle ([B819-G1]) — so the presence case
  stays a review line, never an automatic PASS/FAIL. The check FAILs/WARNs only on the decidable ABSENCE.

## 820.4 — Real-shape proof on `CompressorControl` (client main) `[CERT]`
**PASS** — `CompressorControl.step(long now, int demandCount, …)` (deed38c) takes `demandCount` as a parameter and
gates on it: FASE 2 ends `if (demandCount <= 0) target = 0;` and the FASE 1 fallback is `target = demandCount;`. So a
demand-shaped input (`demandCount`) is reachable in the staging decision that also reads suction (`c.suctionSetpoint`,
`suction`) → the demand-in-scope check PASSES: pressure only modulates, demand gates.
**FAIL (mutant)** — remove the `demandCount` parameter + the `if (demandCount <= 0) target = 0` gate + the FASE-1
`target = demandCount`, staging on suction ALONE: the method then reads a process variable and has NO demand-shaped
input in scope → the check WARNs (the "pressure without demand" shape). This is the PASS(demand-in-scope)/
FAIL(demand-removed) discrimination the lint keys on.

## 820.5 — Kit implication `[INFER]`
Fold into `lint-write-path.sh` (PR19) as an ADVISORY sub-check (or a `verify-module` review-line), NOT a hard FAIL:
"a control/staging method that reads a process-variable input with NO demand/enable input in scope → WARN
(pressure-without-demand)". Cite `[ev: corpus B819, B820]`. It closes the [B819] §819.5 lint candidate with a decidable
scope + the honest boundary (absence = WARN; is-it-really-demand = advisory). It does NOT touch (a)/(c)/(d), which are
PR19/PR1/CHECK12.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | (a)/(c)/(d) of the write-path family are existing gates (PR19 lint-write-path, PR1 lint-delays, CHECK12) — not re-derived here | [CERT-doc] | wave3 R19.1-6 / R19.5; toolbelt/lint-delays.sh present; PR10 |
| 2 | The new check keys on: a control decision reading a process variable with NO demand-shaped input in scope (params + class fields) | [INFER]+[CERT shape] | B819 §819.5; the "pressure without demand" shape |
| 3 | Boundary: ABSENCE of a demand input is statically decidable → WARN; whether a present input is "demand" vs "modulator" is human-review → advisory (never hard FAIL) | [INFER] | B819-G1 (needs a data-flow rule); B788/B805 lintable-vs-advisory doctrine |
| 4 | PASS proof: CompressorControl.step has `demandCount` param + gate `if(demandCount<=0) target=0` alongside the suction read | [CERT] | CompressorControl.java:114/130 (step sig); FASE2 gate + FASE1 `target=demandCount` |
| 5 | FAIL proof: a mutant with demandCount + the gate removed stages on suction alone → no demand in scope → WARN | [CERT — mutation] | the §820.4 mutant of CompressorControl.step |

**Tally**: 2 [CERT] · 1 [CERT-doc] · 1 [CERT-mutation] · 1 [INFER]+[CERT-shape]. The check DESIGN + boundary are [INFER]
grounded in the [CERT] shape + B819-G1. Dedupe: (a)/(c)/(d) + the write-path matrix are REMITTANCE (PR19/PR1/CHECK12,
[B816]/[B819]); this block adds ONLY the demand-in-scope check + its decidable/advisory line + the PASS/FAIL proof.

## Connections
- **[B819]** §819.5 (the lint candidate this closes) + §819.1 (demand-gate = the CompressorControl PASS shape), **[B816]**
  §816.6 (the write-path matrix + LINK_TARGET line — (a)/(d)), **[B801]** (Clock ≤0 — (c) lint-delays.sh), **[B788]**/
  **[B805]** (lintable-vs-advisory doctrine). Kit: an advisory sub-check in `lint-write-path.sh`; existing gates
  PR19/PR1/CHECK12 cover the rest.

## Open gaps
- **B820-G1** (bounded): the demand-shaped-name/type list (`demand*`/`*call*`/`enable`/`*count`/`in:BStatusBoolean`) is
  a heuristic — refine it against a corpus of real control methods so the WARN's false-positive rate is known before it
  ships even as advisory (inherits B819-G1's data-flow question).
