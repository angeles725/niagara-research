# Campaign 10 — exploration draft

Author: companero (Fable), 2026-09-06. Phase: explore (pre-proposal). Same shape as the C9 explore draft. Mandate = the C9
seed cluster (`campaign9-research-candidates.md` S21/S22/S23) + the product seeds Cristian named at C9 close. Kit v0.20.0
(archived `df8c7ec`); client main `ff1b659`; tunnel PR4/5/7 blessed, awaiting Cristian's merge. `[ev: C9 close]`
`[ev: campaign9-research-candidates.md]` `[ev: niagara-tools #89]`

## 0. PREREQUISITES — NOT C10 work, but they MUST precede any C10 CLIENT jar
These are Cristian-owned, one-time, and block C10 client builds/deploys (not the kit lanes):
1. **The pending deploy chain** — the current client jars (Paccadia 2.0.7 / Compresores 2.0.3 / Dashboard 2.1.1) plus the
   C9 bumps (Compresores 2.2.0, Paccadia 2.1.0, Dashboard 2.2.0) must be deployed to PANCCADIA per the deploy runbook delta
   (`2026-09-06-c9-deploy-runbook-delta.md`) BEFORE any C10 client change stacks on them. A C10 client jar on an
   undeployed base risks a schema-risk mismatch at the station.
2. **The niagaraTest harness session** — the C9 harness-only pins (CRA1/2/3-live, CPB5, the alarm ROUTING) were never
   WSL-green; they need one Windows `niagaraTest` run to close the C9 close gate's 14th item. C10 alarm/adapter work
   inherits the same harness dependency, so the harness session should run first and its result recorded.
State these to Cristian as gates, not tasks; the KIT lanes (S21/S22/S23) need neither.

## 1. Ranked backlog (value × tractability)
| # | Item | Class | Value | Tract. | Requires-exec | RED to author |
|---|---|---|---|---|---|---|
| S21 | lint-timers companion-flag: class-FIELD + same-method scope | KIT | Med (removes a spurious FAIL blocking clean report-module) | High (bats + awk scope fix) | WSL only | tests/lint-timers.bats NEG(local)+POS(field) |
| S23 | lint-silent-protection: recognise Pattern B (BIAlarmSource/AlarmSupport) adapter surface | KIT | Med (CP-1 now surfaced but still WARNs — false alarm on the very fix C9 shipped) | High (adapter→pure follow) | WSL only | tests/silent-protection.bats adapter-surface NEG |
| S22 | lint-ext-writable-shape: per-slot writing-action exemption | KIT | Med-High (closes the faultReset false-negative) | Med (reuse S18 slot→writer follow; **contract change**) | WSL + QA RED re-cut | re-cut EW10 CompPan-rt 0→1 |
| P1 | viewer per-user re-auth + configurator role list (write-server) | PRODUCT (tunnel) | High (real operator identity, GxP-grade audit) | Med (Supabase auth re-check per operator; role table) | tunnel + Supabase | write-server.config-login re-auth pins |
| P2 | HMI per-operator kiosk login option (surface B) | PRODUCT (client -ux) | Med (per-operator attribution at the panel) | Med (B830 re-auth path; C9 shipped shared password) | client + harness | ConfigLogin per-user wiring RED |
| P3 | `airDefrost` module flag (rooms 1/2/4) | PRODUCT (client -rt) | Med (Cristian's station trial) | Med — GATED on the defrost trial green light | client + station trial | ColdRoomControl airDefrost decision test |
| P4 | intercambiador Cuarto 3 control point | PRODUCT (station+client) | Med | GATED — only if Cristian confirms it exists on a Niagara output | station wiring + link | (station; then a link pin) |
| P5 | `coolOnSensorFault` station link (all rooms) | STATION | Low-Med (closes a dead HMI write) | High (Workbench link) | station only | bog-nav link-resolves pin |
| S24 | cwd-independent structural REDs | KIT/test | Med (FreezeAlarmWiringTest/CompressorAlarmWiringTest read src relative to cwd — brittle) | High (resolve src from the test's own location) | WSL | a from-any-cwd harness pin |
| S25 | `lint-write-path --strict` | KIT | Low-Med (the C9 design assumed it; add the flag) | High | WSL | lint-write-path.bats --strict exit-1 pin |
| S26 | gitignore client build caches | CLIENT hygiene | Low (stops build/tmp+classes churn) | High (the repo-hygiene issue's fix) | client | (no RED — chore) |

## 2. Dependencies
- S21/S23 independent, land first (FP-only, no contract change). S22 waits on a QA RED re-cut (contract change). Cluster
  all three as a "lint precision" wave — see `2026-09-07-c10-lint-refinement-apply-packages.md`.
- P1 (viewer re-auth) supersedes the C9 shared-password step-up (D-1's C10 seed); P2 (HMI per-operator) depends on P1's
  identity model + a harness session. P3/P4 are GATED on Cristian's station answers (defrost trial; intercambiador output).
  P5 is the station-side half of the dead-panel-writes issue.
- S24 (cwd-independent REDs) should land before more structural REDs are authored (C10 alarm/adapter work needs them).

## 3. Risks
- **S22 contract change:** re-cutting EW10 (CompPan-rt 0→1) can surprise an in-flight apply; sequence the RED re-cut before
  any consumer. K13 (RED wins) means the lint and the RED move together.
- **Harness dependency:** P2/P3/P4 alarm/adapter behaviour is station-only; without the harness session their REDs stay
  structural + SKIP, never WSL-green (C9 lesson — a SKIP is not a pass).
- **Product-vs-tooling scope:** P1-P5 are client/station product work; S21-S26 are kit tooling. Keep them separate lanes so
  a station gate never blocks a WSL kit refinement.

## 4. Requires-execution gates
- KIT (S21-S26): WSL bats + the four-root smokes; no station. S22 additionally needs a QA RED re-cut.
- PRODUCT (P1-P5): P1 tunnel+Supabase; P2 client build + harness; P3 station trial (Cristian) + client; P4 station wiring
  (Cristian) + client; P5 Workbench link (Cristian). None start before §0's prerequisites.

## 5. Recommendation
Open C10 with the **lint-precision wave (S21→S23→S22)** — WSL-only, high tractability, closes the two verified false
results + the C9-shipped false alarm, and needs nothing from Cristian. Hold P1-P5 behind §0's prerequisites and Cristian's
three station answers (defrost trial, intercambiador output, coolOnSensorFault link). S24 (cwd-independent REDs) pairs with
the wave since C10 will author more structural REDs.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | S21/S22/S23 current FP/FN state | [CERT] | lint runs @ ff1b659/a109249 (lint-refinement package) |
| 2 | prerequisites (deploy chain, harness) precede C10 client jars | [CERT] | deploy runbook delta; C9 harness-only pins |
| 3 | P3/P4 gated on Cristian's station answers | [CERT] | dead-panel-writes issue; defrost trial link-list |
| 4 | S22 is a contract change | [CERT] | EW10 @ 269be48 |
