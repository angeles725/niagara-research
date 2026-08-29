<!-- review-status: pending -->
<!-- kit-retro: include -->
<!-- Marker lifecycle: the maintainer flips 'pending' above to 'applied <date> · kit <sha>' (or 'dismissed') once this retro's deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->

# §18 Retrospective — dynamic-validation campaign (2026-08-29)

**Run**: niagara-research, §12 dynamic-validation campaign against a live Niagara N4 TEST station (`127.0.0.1`, account API2 via SCRAM)
**Blocks written**: B600–B610 (11 blocks)
**Coverage**: closed requires-execution / live gaps across 9 focuses (api-access, kitControl, security-audit, px-menu, protocols, oBIX-write, jsonToolkit, webChart, database)
**Driver**: supervised §12 (read-first, write-supervised), `no·inline` per §12 discipline; write grant issued mid-session via explicit operator authorization + AskUserQuestion
**Method**: a FRESH-CONTEXT agent read the current kit (`PROMPT-LOOP.md` + `METHODOLOGY.md`) FIRST, then reviewed the run. READ-ONLY on the kit — PROPOSES only; kit changes are human-reviewed and human-committed (METHODOLOGY §18).

---

## What the run did

The static loop was long exhausted on these focuses; each of B600–B610 closed a `requires-execution` or `blocked` (live-arrival) gap by driving the real station:

- **B600–B602** — api-access: oBIX query / history rollup / CSRF handling (live reads).
- **B603** — kitControl KC13-G1 live safety audit.
- **B604** — security-audit SA-G2 dashboard.
- **B605** — px-menu B290-G1 SCRAM from a non-browser client.
- **B606** — protocols P4-dyn Fox handshake.
- **B607** — oBIX write ⚠CONFIG MUTATION (also settled the CSRF question).
- **B608** — jsonToolkit G1/G2, closed GATED-BY-DEPLOYMENT (component outbound-only, inbound surface not instantiated).
- **B609** — webChart W7-G1 (code confirmed; live deferred on the low-priv-principal wall).
- **B610** — database DB-G3 BBogSpace concurrency ⚠CONFIG MUTATION; DB-G2 closed GATED-BY-DEPLOYMENT (no RDBMS driver installed).

## What worked (kit doctrine that held)

- **Read-first / write-supervised cadence (§12).** The whole campaign started read-only; the write grant arrived MID-SESSION through explicit operator authorization + an AskUserQuestion confirmation, exactly the §12 "scoped authorization for irreversible ops / write-supervised" model. The two mutation blocks (B607, B610) each carried the **`⚠ CONFIG MUTATION`** label and before/after state per the PROMPT-LOOP SECRETS-DISCIPLINE BLOCK-LABEL rule — an audit can tell them from reads at a glance. This is the cadence working as designed.
- **`no·inline` tier discipline (§12).** No live probe or write credential was handed to a sub-agent; `no·inline` is the compliant record, not a skipped delegation.
- **DISK-FIRST honored on jsonToolkit/database** — the run did not detonate a live surface where on-disk evidence plus a deployment check already answered the gap (see D3).

## Proposed kit deltas

| # | Proposed change | Target (file · §) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| 1 | Multi-point load/concurrency test must ENUMERATE the full point set and snapshot every point's value AND link-status BEFORE the first write — not incrementally as the set grows | `METHODOLOGY.md §12` (extend *Backup-before-destroy*) | B610 | new | HIGH |
| 2 | Add a **GATED-BY-DEPLOYMENT** live-verdict verb (surface not instantiated on this deployment), distinct from **GATED** (auth-gated) | `METHODOLOGY.md §12` (*Live-verification verdicts*) | B608, B610 | new | HIGH |
| 3 | A self-built inbound/test scaffold validates the CODE (already `[CERT]`), not the DEPLOYMENT — it earns no `[CERT-hw]` deployment verdict; prefer DISK-FIRST + a deployment-instantiation check | `PROMPT-LOOP.md` HARD RULES (DISK-FIRST corollary) / `METHODOLOGY.md §12` | B608 | new | MEDIUM |
| 4 | "Mint a minimal-privilege ephemeral principal" is a distinct CAPABILITY not every surface provides — check for existing low-priv accounts first; oBIX cannot mint one (no add/setPassword op) | `METHODOLOGY.md §12` (caveat on the LIVE-WRITE recipe step d) / `PROMPT-LOOP.md` SECRETS DISCIPLINE | B609, ES4-G1, W7-G1 | new | MEDIUM |

---

### D1 — Load/concurrency test: snapshot the FULL point set (value + link-status) before the first write (NEW, HIGH)

**WHAT.** Before a multi-point load or concurrency test, ENUMERATE the complete target point set up front and, for EVERY point, snapshot (a) its current value and (b) whether it carries OUTBOUND LINKS (load-bearing vs inert) — BEFORE the first write. Do not capture baselines incrementally as the test grows its write set.

**WHY.** The existing §12 *Backup-before-destroy* rule reads "before overwriting a program/image/config, READ and SAVE the current one." It assumes a FIXED target known before the write. A load/concurrency test that DYNAMICALLY GROWS its write set defeats that assumption: baselines get captured only for the initial targets, and points added mid-test have no pre-mutation snapshot. It also says nothing about checking each point for outbound links — the difference between an inert scratch point and a load-bearing one whose value feeds real logic. Cost: one enumeration + read pass before the test. Impact: prevents an unrecoverable overwrite of a live value.

**EVIDENCE.** B610 — a concurrency load test EXPANDED its target set mid-test (from `NumericWritable5`/`NumericWritable6` to also `NumericWritable`/`NumericWritable1`), but baselines were captured only for the initial pair. `NumericWritable1`'s pre-mutation value became unrecoverable: no history, its only backup postdated the mutation, and it was LOAD-BEARING (linked to `Hvac01.supplyTemp`). Had the full set been enumerated and every point's value + link-status snapshotted before the first write, the loss would not have happened. Target: `METHODOLOGY.md §12`, as an explicit extension of *Backup-before-destroy* (the incremental/growing-write-set case it does not currently cover).

### D2 — Add GATED-BY-DEPLOYMENT as a live-verdict verb (NEW, HIGH)

**WHAT.** Add a verdict to §12's *Live-verification verdicts* list: **GATED-BY-DEPLOYMENT** — the code defect is real and confirmed by reading, but the live DEPLOYMENT does not INSTANTIATE the vulnerable surface at all (the component is present but wired outbound-only; the driver/handler is not installed). Distinct from the existing **GATED** (code-path real, live deployment AUTH-gates it — a control exists in front of an existing surface).

**WHY.** §12 currently offers CONFIRMED / NOT-REPRODUCED / GATED / CONFIRMED-BY-PARITY / DEFERRED-requires-execution. None cleanly names "the surface simply isn't there on this deployment." Folding it into GATED conflates two different facts: GATED means "you'd hit it but a gate stops you" (auth control), whereas GATED-BY-DEPLOYMENT means "there is nothing to hit — the surface was never instantiated." They have opposite remediation and re-test implications (add/adjust a control vs. deploy the missing component to even reach the code). Naming it keeps the per-defect verdict table honest and lets a reader tell "protected" from "absent." Cost: one vocabulary entry. Impact: two verdicts in THIS run were mislabelable as GATED without it.

**EVIDENCE.** B608 (jsonToolkit G1/G2) — the parsing defect is real in code, but the component is deployed OUTBOUND-ONLY; no inbound handler is instantiated, so the attackable surface does not exist on this station. B610 (DB-G2) — the tested code path is real, but no RDBMS driver is installed on the station, so the surface is not instantiated. Both are "defect real, deployment doesn't instantiate the surface," not "auth-gated." Target: `METHODOLOGY.md §12` *Live-verification verdicts* bullet.

### D3 — A self-built test scaffold validates CODE, not DEPLOYMENT (NEW, MEDIUM)

**WHAT.** Record the DISK-FIRST corollary: when a `requires-execution` gap could be "closed" by building a synthetic inbound handler / test harness and attacking it, that does NOT produce a live deployment finding. It re-validates the CODE (already `[CERT]` from static reading) against a scaffold the researcher wrote — it earns no `[CERT-hw]`/`[CERT-live]` claim about the REAL deployment. The honest move is DISK-FIRST plus a deployment-instantiation check (is the surface actually wired up?), yielding a GATED-BY-DEPLOYMENT verdict (D2) rather than manufactured scaffolding.

**WHY.** §12's *Synthetic-stimulus deploy-test* rule legitimately ENCOURAGES injecting a synthetic stimulus — but into the ACTUAL DEPLOYED flow, to test its LOGIC. That is different from standing up your OWN inbound surface to attack, when the real deployment never exposes one. The nuance is worth stating so the synthetic-stimulus rule is not mis-applied into building a straw target. It complements the existing DISK-FIRST HARD RULE (answer from disk before spending a live probe): here disk already answered, and the "live" alternative would only have been theatre. Cost: one caveat. Impact: avoids a false-`[CERT-hw]` deployment claim and wasted scaffolding effort.

**EVIDENCE.** B608 (jsonToolkit G1/G2) — the right call was NOT to deploy an inbound handler to feed my own scaffold: doing so would validate the already-`[CERT]` parsing code, not the live station (which instantiates no such surface — D2). Closed GATED-BY-DEPLOYMENT instead. Target: `PROMPT-LOOP.md` HARD RULES (as a DISK-FIRST corollary) and/or `METHODOLOGY.md §12` (as a caveat beside *Synthetic-stimulus deploy-test*).

### D4 — "Mint a minimal-privilege ephemeral principal" is a surface-dependent CAPABILITY (NEW, MEDIUM)

**WHAT.** Add a caveat to the LIVE-WRITE recipe step (d) — "drive it through a dedicated MINIMAL-PRIVILEGE ephemeral principal, revoked at session end." That step ASSUMES the surface can mint such a principal. It cannot always: some management surfaces expose no user-creation / password-set operation. The caveat: (i) minting a test principal is a distinct capability, not a given; (ii) CHECK FIRST for an existing low-privilege account before assuming you can create one; (iii) note the surfaces that CAN mint (Fox / BOX / Workbench for Niagara) vs. those that cannot (oBIX).

**WHY.** The recipe currently reads as though the ephemeral-principal step is always available; on a read-denied test that needs a low-priv principal, discovering mid-test that the surface cannot create one is an avoidable wall. Naming the capability, and prompting a check for an existing low-priv account, keeps the recipe honest about its own precondition. Cost: one caveat sentence. Impact: two live gaps in this run stalled on exactly this (see below).

**EVIDENCE.** B609 (W7-G1) and ES4-G1 both needed a READ-DENIED principal that could NOT be minted over the oBIX surface — oBIX exposes no add-user / setPassword op. A zero-role user existed (`BACnet`) but its password was not held. The capability to mint a principal lives on Fox / BOX / Workbench, not oBIX. Target: `METHODOLOGY.md §12` (caveat on LIVE-WRITE recipe step d) / `PROMPT-LOOP.md` SECRETS DISCIPLINE (LIVE-WRITE recipe).

---

## Already covered (dedupe — proof the retro read the kit first)

- Write grant scoped to the session, issued by the operator, re-armed at session end → already covered by `METHODOLOGY.md §12` *Scoped authorization for irreversible ops*.
- Config mutations labelled `⚠ CONFIG MUTATION` with before/after state → already covered by `PROMPT-LOOP.md` SECRETS DISCIPLINE *BLOCK LABEL*.
- Live probes / write credential kept inline, never delegated (`no·inline`) → already covered by `METHODOLOGY.md §12` *Supervised, not loop-blind*.
- Never trust the write's own 200; confirm via an independent read oracle → already covered by `METHODOLOGY.md §12` *Cross-protocol oracle for every write* (single-protocol independent-read variant).
- Backing up a value before overwriting it → already covered by `METHODOLOGY.md §12` *Backup-before-destroy* — D1 EXTENDS this rule to the growing-write-set case it does not currently reach, it does not duplicate it.
- CSRF / SCRAM auth handled out-of-band, no credential in argv/blocks/engram → already covered by `PROMPT-LOOP.md` SECRETS DISCIPLINE + LIVE-WRITE recipe step (a).

## Anti-patterns observed

- B610 concurrency test grew its write set without pre-snapshotting the added points → the delta that prevents it: **D1**. This is the one genuine loss of the run (an unrecoverable live value); it is the highest-priority delta for exactly that reason.

## Tools built, adapted, or outgrown

| # | CREATED | ADAPTED | OUTGREW | ORACLE | VERDICT |
|---|---|---|---|---|---|
| T1 | — | — | — | — | `no` · the campaign drove the live station with existing oBIX/Fox clients and probe.sh; no reusable tool was built or forked this run |

## Metrics

- **Blocks reviewed**: 11 (B600–B610) · **§14 cross-block corrections in this run**: 0 · **Rules skipped in practice**: 0 (D1 is a GAP in the rules, not a skipped rule)
- **Deltas proposed (new)**: 4 · **Already-covered lessons**: 6

## What stalled (honest note)

Two classes of gap did NOT close live, and both are legitimately blocked rather than incomplete:

- **Principal-blocked** — W7-G1 (webChart) and ES4-G1 needed a read-denied low-privilege principal that the oBIX surface cannot mint (no add-user / setPassword op), and the one existing zero-role account (`BACnet`) had no held password. W7-G1's code claim IS confirmed `[CERT]` by reading; the LIVE verdict is DEFERRED-on-principal. This is the evidence behind D4; it is a real capability wall, not a coverage hole.
- **Hardware/deployment-blocked** — DB-G2 (database) is GATED-BY-DEPLOYMENT: no RDBMS driver is installed on the station, so the surface is not instantiable without changing the deployment (out of scope for a read-first validation). Correctly closed with the D2 verdict rather than left pending.

## Honest verdict

This run genuinely surfaced FOUR new deltas, one of them (D1) paid for in a real, unrecoverable loss of a live value — the strongest possible evidence a rule is missing. D2 and D4 are precision fixes to §12 vocabulary and precondition assumptions the campaign exercised against a real station for the first time (GATED-BY-DEPLOYMENT as a first-class verdict; principal-minting as a surface-dependent capability). D3 is a smaller honesty corollary. The read-first / write-supervised cadence and the `⚠ CONFIG MUTATION` labelling held exactly as the kit prescribes — that half of the run is "already covered," and is listed as such rather than re-proposed.
