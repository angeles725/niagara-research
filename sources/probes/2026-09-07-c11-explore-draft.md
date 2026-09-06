# Campaign 11 — exploration draft

Author: companero (Fable), 2026-09-06. Phase: explore (pre-proposal). Same shape as the C10 draft. Mandate = the seeds C10
produced + the P1-P5 product seeds still gated on Cristian. Kit v0.21.0 (main `2f3300f`, C10 lint-precision + client hygiene
merged); client main `00e7118`; tunnel PR4/5/7 blessed, awaiting Cristian's merge. `[ev: C10 close 9e110a946]` `[ev: kit main 2f3300f]`

## 0. PREREQUISITES — NOT C11 work, but they MUST precede any C11 CLIENT jar (unchanged from C10)
1. **The pending deploy chain** — base client jars (Paccadia 2.0.7 / Compresores 2.0.3 / Dashboard 2.1.1) + the C9 bumps
   (Compresores 2.2.0, Paccadia 2.1.0, Dashboard 2.2.0) deployed to PANCCADIA per the runbook delta BEFORE any C11 client jar.
2. **The niagaraTest harness session** — the C9 harness-only alarm pins (CRA1/2/3-live, CPB5) need one Windows run; C11
   alarm/adapter work inherits it.
3. **Tunnel merge** — PR4/5/7 (config login, audit schema, mirror) + `config.env` keys.
State to Cristian as gates; the KIT lanes (T1-T4) need none.

## 1. Ranked backlog (value × tractability)
| # | Item | Class | Value | Tract. | Requires-exec | RED to author |
|---|---|---|---|---|---|---|
| T1 | **Shared method-boundary parser** — extract the section-D parser now DUPLICATED in three toolbelt scripts into ONE sourced awk fragment. **The three copies are NOT behaviorally equivalent today (B832, reproduced):** lint-timers.sh:202 and lint-silent-protection.sh:326 gate the method-open on NET brace change (a one-liner `void arm(){ flag=true; Clock.schedule(...); }` has net change 0 → method NEVER detected → silent FALSE-NEGATIVE), while lint-ext-writable-shape.sh:139-147 uses PEAK depth (`max_d`) and catches it. | KIT | **High** (a live silent FN in two of three lints, not just drift risk) | Med (`toolbelt/lib/method-boundary.awk` sourced by all three + a golden-set bats) | WSL | tests/method-boundary.bats — golden tree ALL three agree on; **MUST include a one-liner-method fixture** (the ONLY case the copies disagree on today) |
| T2 | **`C9_CLIENT_ROOT` retarget hygiene** — **5** bats hardcode the default (all now `main-ff1b659` at dab0807; PR7 `b6b65a2` HAND-retargeted 3 of them a109249→ff1b659 at the C10 close — that hand-sed is exactly the churn T2 deletes); centralise to ONE sourced `tests/lib/client-root.bash` | KIT/test | Med (a moved worktree needs 5 edits; the PR7 retarget already paid that cost once) | High (one shared default + edit 5 files) | WSL | tests/client-root.bats — the default resolves from one source |
| T3 | **concept-row-drift lint** — a `[concept]` matrix row whose slot name LATER appears in source is a STALE MARKER (the exemption outlived its reason); flag it | KIT | Med (keeps the `[concept]` exemption honest — else a real slot hides behind a stale marker) | High (inverse of the S25 STALE pass: name ∈ source AND row has `[concept]` → WARN) | WSL | lint-write-path.bats — a `[concept]` row for a now-real slot → WARN stale-marker |
| T4 | **unpinned-guard meta-check** — a lint header's named OBSERVED mutation must map to a bats fixture (C10 lesson 7); a guard no fixture exercises is unpinnable | KIT/process | Med (three C10 guards were unpinned) | Med (a check that scans a lint's header mutations against its bats) | WSL | a meta-bats over the toolbelt |
| P1 | viewer per-user re-auth + configurator role list (write-server) | PRODUCT (tunnel) | High | Med | tunnel + Supabase | write-server re-auth pins |
| P2 | HMI per-operator kiosk login — **attribution is ALREADY shipped (R14: change_log names the acting operator, investigador1 C10 HMI probe)**; the OPEN decision is per-operator **RBAC/VIEW**: does login gate WRITES per operator, or only scope the VIEW? (not attribution — that is solved) | PRODUCT (client -ux) | Med | Med (B830 re-auth path) | client + harness | ConfigLogin per-user RBAC/VIEW branch |
| P3 | `airDefrost` module flag (rooms 1/2/4) | PRODUCT (client -rt) | Med | GATED — defrost trial green light | client + station trial | ColdRoomControl airDefrost test |
| P4 | intercambiador Cuarto 3 control point | PRODUCT (station+client) | Med | GATED — only if Cristian confirms it on a Niagara output | station wiring + link | (station; then a link pin) |
| P5 | `coolOnSensorFault` station link (all rooms) | STATION | Low-Med | High (Workbench link) | station only | bog-nav link-resolves pin |

## 2. Dependencies
- T1 first among the kit lanes — the other lints that reuse the parser (S21/S22/S23 shipped their own copies in C10) should
  migrate to the shared fragment; T1 lands the fragment, then each lint sources it (one PR per migration or one sweep).
- T3 depends on the S25 STALE pass (shipped in C10) — it is the inverse check on the same covered set.
- T2/T4 independent, WSL-only.
- P1 supersedes the C9 shared-password step-up (D-1 C10 seed); P2 depends on P1's identity model + the attribution/RBAC
  answer + a harness session. P3/P4/P5 gated on Cristian's three station answers.

## 3. Risks
- **Parser extraction (T1) is a refactor across three shipped lints that are NOT equivalent (B832 593019540, reproduced by me):**
  a golden-set bats MUST pin the parse BEFORE the extraction. **Decision: the fragment adopts PEAK depth (`max_d`) — net is
  simply wrong** (it drops one-liner methods; REPRODUCED: lint-timers gives 0 companion-flag on a one-liner `arm()`, 1 on the
  identical multi-line body). Invariants the fragment MUST carry: (1) `brace_depth >= 2` guard; (2) the Case-B backward scan
  stops at any line starting with `@` (the boundary that makes the single-vs-multi-line BMisparse pin bite); (3) BOTH
  keyword-exclusion lists, byte-identical across all three copies; (4) peak-depth (`max_d`) method-open; (5) a one-line
  getter/setter skip. Golden fixtures: BMisparse multi-line · `anyNoHardware` same-method local · CP-1 adapter · **PLUS the
  one-liner** (B832 §5.4). Two second-order gaps to close in the same lane: **B832-G1** (getter/setter-skip pin) and
  **B832-G2 / D3** (lint-silent-protection Case-B scans the RAW line with a `//`-only strip, missing `/* */` — a latent bug).
- **P2 attribution-vs-RBAC** is a product decision, not tooling — resolve it with Cristian before authoring the RED (the two
  designs share almost no code).
- **Harness dependency** — P2/P3/P4 alarm/adapter/login behaviour is station-only; REDs stay structural + SKIP until the
  harness session (C9 lesson — a SKIP is not a pass).

## 4. Requires-execution gates
- KIT (T1-T4): WSL bats + the golden-set/inverse smokes; no station.
- PRODUCT (P1-P5): P1 tunnel+Supabase; P2 client + harness + Cristian's attribution/RBAC answer; P3 station trial; P4 station
  wiring; P5 Workbench link. None start before §0.

## 5. Recommendation
Open C11 with the **parser-consolidation + hygiene wave (T1 → T3 → T2 → T4)** — WSL-only, closes the drift risk the three
C10 parser copies created, and needs nothing from Cristian. Hold P1-P5 behind §0's prerequisites and Cristian's answers
(defrost trial, intercambiador output, coolOnSensorFault link, and the P2 attribution-vs-RBAC decision). T1 is the keystone —
land it before any further lint touches method boundaries.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | three parser copies NOT equivalent: net-depth timers/silent drop one-liners, peak-depth ext-writable catches | [CERT, reproduced] | B832 593019540; my run: lint-timers 0 companion-flag on one-liner arm(), 1 on multi-line; ext-writable :139-147 max_d |
| 2 | C9_CLIENT_ROOT default in 5 bats (all main-ff1b659; PR7 b6b65a2 retargeted 3) | [CERT] | grep C9_CLIENT_ROOT(:-|:=) tests/*.bats @ dab0807 = 5 |
| 3 | `[concept]` per-row exemption shipped (S25) — T3 is its inverse | [CERT] | lint-write-path.sh:424-438 |
| 4 | Case-B `@`-line stop + depth-guard limitation | [CERT] | C10 lesson 7 / design validator |
| 5 | P2 attribution shipped (R14); open decision = per-operator RBAC/VIEW | [CERT] | investigador1 C10 HMI probe (2026-09-06-c10-hmi-per-operator-login-options.md) |
