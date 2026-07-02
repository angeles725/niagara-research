<!-- review-status: pending -->
# Retro — niagara-research · live-station · 2026-07-02 · Research-SDD self-retrospective

> Run reviewed: focus `live-station` B156-B162 (6 iterations + terminal synthesis). Trigger: focus-completion (§8 terminal).
> Method: a FRESH-CONTEXT agent read the current kit (`PROMPT-LOOP.md` + `METHODOLOGY.md`) FIRST, then the run's
> blocks/commits/§14 corrections/probes, and proposes kit deltas. READ-ONLY on the kit — this report only PROPOSES;
> kit changes are human-reviewed and human-committed (METHODOLOGY §18).
>
> Context: this is the FIRST §12 DYNAMIC-phase focus in the corpus (live Niagara N4 station on localhost, WSL mirrored),
> AND the first `live-install` SECRETS-DISCIPLINE run, validating the 14 static defects of [Block 150] against the live
> runtime. The intersection DYNAMIC + SECRETS-DISCIPLINE + live-install is where the kit is thinnest — most deltas below
> live there. The static-loop machinery (§1-§11) is well-covered and needed nothing.

## Proposed kit deltas

> Only genuinely NEW items — anything the kit already encodes is listed under "Already covered", not here.
> Each delta: the concrete change · the target file/section · evidence · priority.

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | Codify a SECRETS-safe live-WRITE recipe: authenticate via an out-of-band credential (curl `-K` config in scratchpad) NEVER in argv/probe cmdline/sources/engram; handle secret-bearing bodies (config) as a scratchpad backup cited by `sha256`+byte-count NEVER the body; mutate with a BENIGN disposable marker (not real data) + independent oracle + byte-identical restore verified per write; drive it through a dedicated MINIMAL-PRIVILEGE ephemeral test principal revoked at session end | `PROMPT-LOOP.md` SECRETS DISCIPLINE hard rule (+ cross-ref `METHODOLOGY §12`) | B160 §160.1-4, B161 §161.1; probes `bash-...195551Z.txt` / `...200405Z.txt` (cmdlines record only the scratchpad script name — cred absent from every preserved artifact); RESEARCH-STATE "cero secretos exfiltrados", restore `bf70f28f…` 60154 B | new | HIGH |
| 2 | Name a DYNAMIC-phase live-verification VERDICT taxonomy (distinct from §13's certainty-audit ESCALATED/CONFIRMED/DOWNGRADED/REFUTED): **CONFIRMED** (live oracle) / **NOT-REPRODUCED** / **GATED** (code-path real, live deployment auth-gates it) / **CONFIRMED-BY-PARITY** (sibling sink sharing an already-proven privileged path, deliberately NOT re-detonated to minimize live mutations) / **DEFERRED-requires-execution** (needs a built probe → §19) | `METHODOLOGY §12` (add a per-defect verdict table) | B159-B161 verdicts, consolidated in B162 §162.2 table; CONFIRMED-BY-PARITY rationale B160 §160.2 ("una prueba viva del patrón basta; el resto es riesgo sin información nueva") | new | HIGH |
| 3 | Add the HARDWARE-SCOPE-CLARIFIES-CODE direction. §12 currently states only "Hardware refutes code"; §14 covers refute-vs-scope-clarify only for static-vs-static artifacts. Add: a `[CERT-hw]` live finding can SCOPE-CLARIFY (narrow) a `[CERT]` static claim WITHOUT refuting it when the code-path is real but the live DEPLOYMENT gates/limits its exploitability — label it a scope divergence ("correct for the code-path; the live deployment adds a control the static source could not express"), not an error | `METHODOLOGY §12` ("Hardware refutes code" bullet) + `§14` (REFUTE vs CLARIFY-SCOPE) | B161 §161.2 (backups POST+403 gate) + §161.6, B159 §159.2 (`?file=`→500) — both explicitly "§14 clarificación de comportamiento vivo, NO refuta"; B162 §162.3 | new | MEDIUM-HIGH |
| 4 | Codify SAFE method/gate discovery on destructive endpoints: learn the allowed verbs and the auth gate WITHOUT triggering the op, via `OPTIONS` or a deliberately wrong-method request (405 + `Allow` header). A recon rung BELOW rung-2 on the invasiveness ladder | `METHODOLOGY §12` (invasiveness ladder) | B161 §161.2 (GET on POST-only `backups/reset` → `405 Allow: POST`, learned the method without detonating the wipe); probe `...200405Z.txt` matrix | new | MEDIUM |
| 5 | State that the SUPERVISED dynamic/write phase legitimately runs INLINE (no delegated sweeps): narrow probes + a live write-credential must NOT be handed to a sub-agent, so `no·inline` is the COMPLIANT tier record for §12 iterations, not a skipped delegation. The delegation / MODEL-TIER rules are written for the static loop's heavy sweeps and do not apply here | `PROMPT-LOOP.md` (DELEGATE / MODEL TIER notes) + `METHODOLOGY §12` | RESEARCH-STATE iteration history: all 6 iterations `no·inline`; §12 "Supervised, not loop-blind" | new | MEDIUM |
| 6 | Refine the cross-protocol oracle rule for single-protocol (web/REST) live targets: when the target exposes only ONE protocol, an INDEPENDENT READ request/endpoint (GET after the POST write) is a valid oracle — the essential property is that confirmation does NOT come from the write's own response, not that a second wire protocol exists. The current Modbus-vs-RPC example can mislead a web operator into thinking a second protocol is required | `METHODOLOGY §12` ("Cross-protocol oracle for every write") | B160 §160.1 (POST `config_update` write, oracle = independent GET `/nmodsreflow/config` → earned `[CERT-hw]`); probe `...195551Z.txt` | refinement | LOW |

For each delta above, one line of rationale (WHY it matters, what it costs, expected impact):

- **#1** — The kit's SECRETS DISCIPLINE states the INVARIANT (cite structure, never value) but not the operational HOW of a live AUTHENTICATED WRITE; this run improvised a clean, reusable recipe that kept the credential and a 60 KB config body out of every preserved artifact while still earning `[CERT-hw]`. Costs a short recipe block; impact: every future `live-install` write phase inherits a proven zero-leak procedure instead of re-inventing it under risk.
- **#2** — §12 has no verdict vocabulary, so each block phrased its outcome ad hoc; naming the five verdicts (esp. GATED and CONFIRMED-BY-PARITY) makes dynamic reports comparable and encodes a SAFETY principle — prove a defect pattern live once, confirm siblings by parity instead of detonating each. Cheap (a table); high leverage on every future dynamic run's readability and blast-radius.
- **#3** — The kit only lets hardware REFUTE code; this run repeatedly needed the softer move (code-path real, but the live deployment gates it) and correctly called it a scope-clarification, but had to lean on §14's static-only wording. Naming the hardware→scope-clarify direction prevents a future operator from either wrongly refuting a correct static block or wrongly claiming a live exploit. Low cost, prevents a real mis-classification.
- **#4** — Discovering that `backups/reset` is POST-only and auth-gated via a harmless GET (405 Allow) instead of firing the wipe is exactly the kind of low-blast-radius recon the ladder should teach; the kit's ladder says "escalate deliberately" but never names this pre-rung probe. Tiny addition, real safety on destructive endpoints.
- **#5** — A future reviewer auditing the tier column would see `no·inline ×6` and could flag it as a skipped delegation rule; stating that supervised live-write phases correctly stay inline (you don't give a sub-agent a live destructive credential) closes that false-positive and records the real reason. Doc-only cost.
- **#6** — The Modbus/RPC example is great for field devices but a web-only target has no second protocol; without this note an operator might refuse to write for lack of a "second channel" or, worse, trust the write's own 200. Clarifies an existing rule for the common web case.

## Already covered (dedupe — proof the retro read the kit first)

- Invasiveness ladder, backup-before-destroy (citable + verified-restorable), scoped authorization for irreversible ops with explicit session expiry and re-arm, read-first/write-supervised, "supervised not loop-blind" → already covered by `METHODOLOGY §12`. The run applied all of these faithfully.
- Re-measure ground-truth LIVE, never inherit (cert SHA-256, hostAddress, Fox version, module version re-measured in B156 §156.7) → already covered by `PROMPT-LOOP HARD RULES (RE-MEASURE GROUND-TRUTH)` + `METHODOLOGY §12` + golden rule 8.
- Device identity ≠ program identity (B156 §156.7 explicitly separates program/service identity from the physical box) → already covered by `METHODOLOGY §12`.
- Per-step explicit user OK / AskUserQuestion at rung transitions, session-scoped rung-3 grant with expiry → already covered by `METHODOLOGY §12` (scoped authorization "for this session only", explicit user OK per step).
- Per-block orchestrator Bash gate is JUSTIFIED in the dynamic/destructive phase (verifying physical/external state the self-report can't vouch for: restore, byte-identical config, gate matrix) → already covered by `METHODOLOGY §11` scope note.
- SECRETS DISCIPLINE invariant itself (cite structure, never value; zero secrets to block/sources/engram) → already covered by `PROMPT-LOOP HARD RULES`; the NEW part (delta #1) is the operational write recipe, not the invariant.
- WSL `networkingMode=mirrored` environment setup to reach the station → already covered by `METHODOLOGY §12` (Environment setup).
- Reclassify `blocked` gaps to investigable on hardware arrival; requires-execution stays out of the static count (V7/V8 BQL deferred to a WS probe) → already covered by `METHODOLOGY §8` + `§12` + `§19`.
- Focus-closing terminal SYNTHESIS block at focus exhaustion (B162) → already covered by `METHODOLOGY §8` terminal trigger + `§16`.
- §14 cross-block corrections applied (B157 §157.2 resolves B138 §178 mount; B161 §161.2 scope-clarifies B144) → the HABIT is covered by `§14`; only the hardware→scope-clarify DIRECTION (delta #3) is new.

## Anti-patterns observed (optional)

- No kit rule was BROKEN this run. The only latent trap is the tier column reading `no·inline ×6` looking like a skipped delegation rule (a false positive) → prevented by delta #5.
- Latent risk if the recipe stays uncodified: a future live-write phase could leak a credential via argv or dump a secret-bearing body into a block → prevented by delta #1.

## Metrics

- **Blocks reviewed**: 7 (B156-B162)  ·  **§14 cross-block corrections in this run**: 3 (B157→B138 §178 mount; B161→B144 GET/cero-auth→POST/gated; B159 read-behavior clarification)  ·  **Rules skipped in practice**: 0 (all `no·inline` was correct for §12, see #5)
- **Deltas proposed (new)**: 6 (5 new + 1 refinement)  ·  **Already-covered lessons**: 9

## Honest verdict

This run genuinely surfaced new material — but ONLY at the DYNAMIC + SECRETS-DISCIPLINE + live-install intersection, which the kit under-specifies precisely because this is the first focus to hit it. The static-loop core (§1-§11), the ladder, ground-truth re-measurement, scoped authorization, and the §11 dynamic-gate scope note all held up with zero friction and are correctly listed as already-covered. The signal concentrates in three high-value, genuinely-absent items: the secrets-safe live-write recipe (#1), the live-verification verdict taxonomy (#2), and the hardware-scope-clarifies-code direction (#3). Deltas #4-#6 are smaller but real. Nothing here is invented to look productive — a purely static run would have produced "no new deltas".
