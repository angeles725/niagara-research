<!-- kit-retro -->
<!-- review-status: pending -->
# Retro — niagara-research · oem-honeywell-tail closure + 4-focus chain · 2026-08-24 · Research-SDD self-retrospective

> Run reviewed: the licensing deep-dive TAIL of session 2026-08-24, blocks **B481–B495** (15 blocks), which
> chained FIVE focuses in one continuous driver session — licensing deep-dive (B481–B489) → security-audit
> CONSOLIDATION (B490) → secrets-at-rest NEW focus (B491) → signing-pki REOPEN (B492) → oem-honeywell-tail
> CLOSURE (B493 U1b/U1c · B494 U14 · B495 U10) — run as a two-session PEER split (Primero=Java decompile,
> Segundo=native RE) coordinating via SendMessage.
> Trigger: focus-completion (`oem-honeywell-tail` in-mission scope DONE at B495, commit `94d7207`) + a
> whole-run multi-focus review.
> Method: a FRESH-CONTEXT agent read the current kit (`PROMPT-LOOP.md` BOOTSTRAP/NORMAL-CYCLE +
> `METHODOLOGY.md` §5/§8/§13/§16/§18, `templates/retro.template.md`) FIRST, then deduped HARD against BOTH
> existing session retros — `retros/2026-08-24-licensing.md` (D1–D3) and `retros/2026-08-24-licensing-deepdive.md`
> (D4–D6) — and the workbench focus retro (`retros/2026-08-10-workbench-focus-retro.md`, delta #2). READ-ONLY on
> the kit — this report only PROPOSES; kit changes are human-reviewed and human-committed (METHODOLOGY §18).

## Run summary

The tail of the 2026-08-24 licensing session was unusual in TOPOLOGY: one driver touched five distinct focuses
back-to-back (deep-dive → consolidate → new-focus → reopen → close-out), while a peer session (Segundo) ran the
native RE half of the same subject in parallel. Three candidate signals were evaluated:

1. **A gap name asserted the WRONG PRODUCT.** In B495 (`oem-honeywell-tail` U10), two seeded gap descriptions
   were factually wrong about what the module IS — `axvelocity` was assumed "Andover Continuum Velocity
   access-control" but is **Apache Velocity template engine** (663 `.java` under `org/apache/velocity`);
   `silk` was assumed a "Sylk/S-Bus actuator driver" but is a **SOAP web-services toolkit** (`SoapClient`/
   `BSoapServlet`/`Wsdl`). The block had to spend §495.3 "Two scope corrections" fixing the premise, using
   exactly the check that would have prevented it: `module.xml` + top package root. → the one genuinely-new
   delta (D7).
2. **A five-focus chain in one session.** Novel topology, but every constituent operation (focus-closing
   synthesis, opening a new focus, reopening a stopped focus, sequential focus hand-off, peer split) is already
   individually documented across §8/§16 + PROMPT-LOOP TERMINAL TRIGGER + the deepdive retro's D6. Judged
   already-covered, not a new delta.
3. **Recurring hardcoded/weak OEM credentials** (honIrmConfig `irmn4encryption1`⊕serial B493; mcquay
   `"FFFFFFFF"`/`"86672775"` B495) — a TARGET finding, not a reusable kit heuristic. Not a delta.

The RE discipline was clean: corroborate-before-cite, module.xml/package-root identity anchoring in the B495
self-verify (rows 2–3), peer attribution to Segundo credited in blocks, `[CERT negative]` for ungated modules
with cited gate sites. The new-delta list is deliberately short (one), as a healthy §18 retro should be.

## Proposed kit deltas

> Numbered D7 to continue the licensing-fleet D1–D6 scheme (this fleet reads its licensing retros by hand).
> Only genuinely NEW items here; the strong existing coverage is under "Already covered".

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| D7 | **MODULE/VENDOR IDENTITY pre-check for gap names that assert what a module IS.** When a seeded gap name carries a PRODUCT/VENDOR ASSUMPTION about a module (e.g. "axvelocity = Andover Continuum Velocity access-control", "silk = Sylk/S-Bus actuator driver"), VERIFY the identity BEFORE sealing the gap and authoring — read the module's `module.xml` description + the top package root, not just that the artifact exists. A wrong-product premise burns the block's opening on a premise correction (B495 spent a whole §495.3 on it) instead of a finding. This is the IDENTITY sibling of the workbench retro's delta #2 (class-name EXISTENCE pre-check via `fd <ClassName>.java`): #2 asks "does the named thing exist?"; D7 asks "is the named thing the PRODUCT the gap claims?" — a module can exist AND `fd`-resolve while being an entirely different vendor/product. Both are specialisations of "GAP PREMISES ARE HYPOTHESES" (BOOTSTRAP §e). | `PROMPT-LOOP.md BOOTSTRAP §e` (extend "GAP PREMISES ARE HYPOTHESES" with the module-identity mechanism, beside the workbench #2 class-existence mechanism) + tie-in `METHODOLOGY.md §13` (audit-first backlog derivation) | B495 §495.3 "Two scope corrections" (commit `94d7207`): `axvelocity` = Apache Velocity template engine (`org/apache/velocity`, 663 `.java`), NOT Andover access-control; `silk` = SOAP web-services toolkit (`com/tridium/silk`, `SoapClient`/`BSoapServlet`), NOT Sylk/S-Bus. Self-verify rows 2–3 anchored the fix on `module.xml` + package roots — the exact pre-check D7 asks for. 2 wrong product premises in one U10 gap sweep. | new (sibling of workbench #2) | MEDIUM |

For the delta above, one line of rationale (WHY · cost · impact):

- **D7** — The kit already tells a driver to pre-check whether a class-named gap EXISTS (workbench #2), but not
  whether a module-named gap IS the product the gap claims. A module can exist, `fd`-resolve, and still be a
  different vendor entirely — the exact failure B495 hit twice. Cost: one `module.xml`-description + top-package
  read per identity-bearing gap name during backlog seeding (cheaper than the mid-block §495.3 correction it
  prevents). Impact: an identity-bearing gap name gets its premise validated before it scopes an investigation
  at the wrong product.

## Already covered (dedupe — proof the retro read the kit first)

- **Candidate E → proposed as D7 (genuinely new).** DISTINCT from the workbench retro delta #2 (`retros/2026-08-10-workbench-focus-retro.md`),
  which is a class-name EXISTENCE pre-check (`fd <ClassName>.java`). D7 is the IDENTITY twin (module IS the
  claimed product), a different failure mode: the module exists but is a different vendor. Both are
  specialisations of `PROMPT-LOOP.md BOOTSTRAP §e` "GAP PREMISES ARE HYPOTHESES". Not a duplicate — proposed.
- **Candidate F (multi-focus chain in one session) → already covered, NOT proposed (honest call).** Every
  constituent operation of the five-focus chain is already in the kit: focus-closing SYNTHESIS/CONSOLIDATION
  block (B490) → `METHODOLOGY.md §8` terminal-trigger focus-close synthesis; opening a NEW focus (B491
  secrets-at-rest) → `§16` (planned/active focus + `§b2` angle confirmation before a new focus); REOPENING a
  stopped focus (B492 signing-pki) → `§8` "Reopening a STOPPED loop"; sequential focus HAND-OFF within one loop
  → `PROMPT-LOOP.md` TERMINAL TRIGGER (reschedule re-entering the same prompt with FOCUS set to the next). The
  PEER-session native/Java split is already the deepdive retro's **D6**. A single consolidating cross-reference
  that names the {consolidate · new-focus · reopen} triad in one place would be a NICE-to-have, but it is
  non-load-bearing and every piece is documented — proposing it would be noise (§18 honesty clause). Not proposed.
- **Candidate G (recurring hardcoded/weak OEM credentials) → not a kit delta.** honIrmConfig
  `irmn4encryption1`⊕serial (B493), mcquay `"FFFFFFFF"`/`"86672775"` (B495) are TARGET security findings, not a
  reusable method heuristic. The kit's security-claim discipline (framework-semantic check step 5 · FALSIFY
  BEFORE REPORTING · RE-MEASURE A DRAMATIC NEGATIVE) already governs HOW to cite them. Recorded as a
  target-domain observation, not proposed.
- **Peer-session artifact-axis split + dedup handoff + peer attribution** (Primero=Java / Segundo=native;
  B489/B494 native RE credited to Segundo) → already the deepdive retro's **D6** (`retros/2026-08-24-licensing-deepdive.md`).
  This run is a clean second instance of D6, not a new delta.
- **Cross-block reconciliation instead of overwrite** (B492/B489 reconciled B392/B482 module-signing model
  without clobbering; B493 row 6 confirms B242 §242.9) → already `METHODOLOGY.md §14` (CONTRADICTIONS.md
  open→resolved) + the deepdive retro's positive-control note. Positive control, not a delta.
- **`[CERT negative]` for an ungated module with cited gate sites** (orion/alarmOrion/silk/BACnetFFTN4 in
  B495) → already `METHODOLOGY.md §8` negative-closure discipline (state the sample + the test applied). Clean
  instance, no delta.

## Anti-patterns observed / target-doc observations

- **A gap name asserted the wrong PRODUCT and the correction landed mid-block instead of pre-seed** (axvelocity,
  silk in B495 §495.3) → the delta that prevents recurrence: **D7**.
- **Recurring weak/hardcoded OEM credentials across drivers** (B493 `irmn4encryption1`⊕serial; B495 mcquay
  `"FFFFFFFF"`/`"86672775"`) → a TARGET finding for the security-audit focus (fed to B490), not a kit rule.
- **Positive controls (no anti-pattern):** peer-axis split with Segundo attribution (D6 in practice);
  B392/B482 reconciled without overwrite (§14 holds); module-identity anchored on module.xml + package root in
  the B495 self-verify — the very discipline D7 asks to move EARLIER.

## Tools built, adapted, or outgrown

| # | CREATED (path · purpose) | ADAPTED (kit tool · what it could not express) | OUTGREW | ORACLE | VERDICT (decision · evidence) |
|---|---|---|---|---|---|
| T1 | — | `decompile-java.sh` (Vineflower) · used AS-IS across B481–B495 for the OEM driver jars and the licensing/signing classes | — | — | `keep-kit` · existing wrapper sufficient; the gap was PROCESS (D7 identity pre-check), not tooling. |
| T2 | — | native RE toolbelt (`corroborate-native.sh` / `ExportDecompiledC.java` / `nverify.exe` artifacts) · used AS-IS by the peer session Segundo (B489 nverify, B494 DPAPI) | — | — | `keep-kit` · both routes are kit tools; the lesson is topology (already D6), not a new tool. |

No script was written this run; RE ran on existing wrappers. Honesty clause: nothing else invented.

## Metrics

- **Blocks reviewed**: 15 (B481–B495) · **Focuses touched**: 5 in one session (licensing deep-dive · security-audit
  consolidation B490 · secrets-at-rest new focus B491 · signing-pki reopen B492 · oem-honeywell-tail closure
  B493–B495) · **Peer sessions**: 2 (Primero=Java, Segundo=native)
- **§14 cross-block corrections in this run**: reconciliations B392/B482 (B489/B492), B242 §242.9 confirmed
  (B493), and the two B495 §495.3 scope corrections (axvelocity, silk identity) · **peer-closed native gaps**:
  B482-G1 (nverify B489), B482-G2/B491-G1 (DPAPI B491/B494) closed by Segundo
- **Deltas proposed (new)**: 1 (D7 MEDIUM) · **Already-covered / dismissed candidates**: 2 (F already-covered, G
  not-a-delta) + 4 already-covered lessons
- **Rules skipped in practice**: 1 — the identity premise for `axvelocity`/`silk` was not pre-checked before
  seeding, caught only mid-block at B495 §495.3 (the gap D7 closes)

## Honest verdict

This tail surfaced ONE genuinely-new, load-bearing delta: **D7** — a module/vendor IDENTITY pre-check that is a
distinct sibling (not a duplicate) of the workbench retro's class-EXISTENCE pre-check, evidenced by two
wrong-product premises corrected mid-block in B495. Candidate F (five-focus chain in one session) is a novel
TOPOLOGY but every operation it uses is already documented across §8/§16 + the TERMINAL TRIGGER + D6 — proposing
a rule for it would be noise, so it is honestly logged as already-covered. Candidate G (weak OEM credentials) is
a target finding, not a method lesson. The RE work itself was textbook kit discipline — corroborate-before-cite,
module.xml/package-root identity anchoring, peer attribution, `[CERT negative]` with cited gate sites — which is
why the already-covered list is substantial and the new-delta list is a single specific item, as a healthy §18
retro should be.
