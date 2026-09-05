<!-- kit-retro -->
<!-- review-status: applied 2026-09-05 · kit 185ad74 · shipped: R2 (PROMPT-LOOP, PR #445 fe88d17), R1, R3 (§19 tool-vs-PoC + baked-in redaction, PR #448 185ad74) -->
<!--
  focus: jace-history-audit
  blocks: B699–B703 (5 blocks)
  date: 2026-08-30
  review-status: pending
  propose-never-apply: true
  absorb-targets: PROMPT-LOOP.md (§19 + secrets-tool delta) · METHODOLOGY.md §19 (tool classification)
-->

# §18 Retrospective — focus: jace-history-audit (B699–B703)

**Run summary:** CONTENT-ONLY focus — the `.hdb`/`.adb` file FORMAT was already known from focus
`database`; this run read the OPERATIONAL CONTENT of JACE_UMBRELLA's history/audit/alarm stores
extracted READ-ONLY from SD P2 via `tools/qnx6read.py`. 5 gaps; 4 closed by inline hdbread.py
invocation + review; 1 synthesis (HD5). Notable §19 deliverable: `tools/hdbread.py`, a self-contained
read-only reader with built-in `--mask` mode — generalizable to any Niagara station's history.
Central finding: the history/audit/alarm trace corroborates, from an INDEPENDENT data store, three
prior findings: the io34 down-module (B687), the seed-station verdict (B692), and the cleartext-at-rest
weakness (B698). SECRETS DISCIPLINE active throughout; `--mask` baked into the reader tool itself.

---

## Delta R1 — HIGH — §19 reusable-corpus-tool vs one-off-PoC: classification + promotion-flag discipline

**Target:** METHODOLOGY.md §19 (or §3b corpus-layout table) + PROMPT-LOOP.md §19 situational block

**Evidence:** B699 (`tools/hdbread.py`) and its predecessor from the `jace-data-at-rest` focus
(`tools/qnx6read.py`). Both were classified in the RESEARCH-STATE as `§19 build` but neither received
an explicit classification of their REUSE scope.

**What the kit already covers:** The corpus layout table (METHODOLOGY §3b) lists `<target>/tools/` as
"tools born inside the loop" with a provenance ledger (`tools/README.md`: name · path · WHY). The
layout also lists `<corpus>/codegen/` as "§19 build/PoC artifacts and round-trip evidence." An in-passing
mention ("flagged `promote` by its own retro") exists in §3b's narrative about `api-openness`, but the
kit does NOT define what `promote` means, where it promotes TO, or what criteria trigger the flag. There
is no classification of §19 outputs into reuse tiers, and `sweep-tools.sh`'s inability to find
`api-openness`'s `_extract/` tools is cited as a failure mode — yet the remedy (a canonical `tools/`
directory) is prescribed only for PLACEMENT, not for promotion decisions.

**What this run exposed:** Two §19 tools appeared across adjacent focuses on the same target:

- `qnx6read.py` (focus `jace-data-at-rest`) — reads QNX6 filesystem images; generalizes to ANY
  QNX6-based device (JACE-3000, JACE-8000, AX Controller running QNX). NOT one-station-specific.
- `hdbread.py` (this focus) — reads any Niagara N4 `.hdb` history file, not just JACE_UMBRELLA's.
  The magic / schema / record structure is the SAME across all N4 stations.

Both live in `tools/`, which is correct for placement. But neither was flagged as PROMOTABLE to the
kit toolbelt (`$KIT/toolbelt/`) despite being generally applicable beyond this target. The kit has no
rule for making this judgment.

**Three reuse tiers for §19 outputs (propose-never-apply):**

> **§19 TOOL CLASSIFICATION (added to §19 write-up step, PROMPT-LOOP and METHODOLOGY).**
> Every §19 output is classified into one of three tiers at the time of creation and recorded in
> `tools/README.md` as a `tier:` annotation:
>
> - **one-off PoC** — demonstrates a round-trip, generates evidence for one gap, not reusable beyond
>   this investigation. Lives in `codegen/` (as the layout table already specifies). No promotion.
>   Example: a script that decodes one specific encrypted field using a key extracted from `.km`.
>
> - **reusable corpus tool** — parses a format or artifact type that recurs across this TARGET
>   (multiple stations, multiple firmware versions, multiple focuses). Lives in `tools/`, registered
>   in `tools/README.md`. The §19 block documents its interface and scope. No kit-toolbelt promotion,
>   but a future focus on the same target MUST check `tools/README.md` before re-building.
>   Example: `hdbread.py` (any N4 station's .hdb), `qnx6read.py` (any QNX6 image).
>
> - **promotable** — parses a format or artifact type that is applicable to MULTIPLE REGISTERED TARGETS
>   in `TARGETS.md`, or to a class of targets the kit toolbelt could generically support. Flag with
>   `promote: YES` in `tools/README.md` and carry the same flag in the §18 retro. Promotion decision
>   is the kit maintainer's; the flag makes the candidate visible to `sweep-tools.sh`.
>   Example: a general `.hdb` reader for any Niagara N4 deployment (multiple targets), or a QNX6
>   image reader applicable to any QNX6-based JACE target.
>
> **Gate:** At §19 block write time, record the tier in RESEARCH-STATE's iteration-history `delegated?`
> column alongside the tier annotation: `no · inline (§19 reusable — hdbread.py · promote: YES)`.
> If the PoC produces an evidence file, it belongs in `sources/probes/<focus>/` per §5, NOT in
> `codegen/` — the evidence and the build artifact are distinct placements. A §19 block that does
> not declare a tier is implicitly `one-off`; that must be an explicit judgment, not an omission.

**Priority:** HIGH — two tools in the same target were built without a tier classification, and the kit
already has evidence of at least one tool in another target (`api-openness`) that was flagged `promote`
in a retro but was invisible to `sweep-tools.sh` because it was not in `tools/`. Naming the three tiers
and the `promote: YES` flag gives the sweep instrument something to find.

---

## Delta R2 — MED — "Corroboration-from-independent-store" as a named valid EVIDENCE block type

**Target:** PROMPT-LOOP.md step 4 WRITE ONE BLOCK / step 5 self-verify marker tally paragraph

**Evidence:** B700–B703. Every block in this focus closed a gap by CONFIRMING a prior finding from an
independent data store rather than by discovering a new fact. Specifically:

- B700 (SecurityHistory): confirmed the single-operator / admin-dominated access pattern already
  inferred from B692's seed-station verdict — from a different file.
- B701 (LogHistory): confirmed the io34 down-module fault first documented in B687 — from the station's
  own log trace, independent of the driver/config XML that B687 read.
- B702 (alarm.adb): confirmed io34 as the sole alarming point and confirmed 0 alarm recipients — both
  corroborated B687 + B689.
- B703 (synthesis): corroborated seed-station verdict B692 and cleartext-at-rest B698 from the
  provisioning .hdb emptiness and the audit trail content.

None of these produced a "new finding" in the sense of a previously unknown fact. All were
CORROBORATIONS of prior [INFER] or [CERT-hw] claims from a data source that was INDEPENDENT of the
source that generated the prior block.

**What the kit already covers:** The self-verify step says: "For an EVIDENCE block (decompilation/
reading), a high ratio (>~0.5) signals this gap's investigable evidence is nearly exhausted — say so."
This framing implicitly treats "nothing new" as a signal of low value and near-exhaustion. A run that
produces 4 consecutive low-INFER corroboration blocks would appear, by this metric, to be yielding
diminishing returns. The kit does not affirm that corroboration from an INDEPENDENT store is genuinely
valuable even when all findings are confirmatory.

**What this run showed:** Each corroboration block elevated the certainty of a prior [INFER] claim by
providing an INDEPENDENT [CERT-hw] witness. B692's seed-station verdict was [CERT-hw]+[INFER]; after
B700–B703 it has three independent data stores (audit trail + log + alarm + provisioning .hdb) all
consistent with it. This is a qualitative shift in confidence, not a failure of the investigation to
find new facts.

**Proposed rule (propose-never-apply):**

> **CORROBORATION-FROM-INDEPENDENT-STORE (a named valid EVIDENCE block type).** A block whose primary
> finding is the CONFIRMATION of one or more prior `[INFER]` or `[CERT-hw]` claims, from a data source
> INDEPENDENT of the source that generated those claims, is a valid, high-value EVIDENCE block —
> NOT a sign of investigable exhaustion. Declare it explicitly:
>
> `Block TYPE = EVIDENCE (corroboration — independent store)` in the self-verify tally.
>
> Two conditions must both hold for this classification to apply:
> (a) The data store is genuinely INDEPENDENT — a different file format, a different subsystem's
>     record type, or a different mechanism than the prior block (e.g. alarm.adb vs config.bog;
>     station log vs audit trail; a DIFFERENT decompiled module vs the one cited before).
> (b) The prior claims corroborated were at `[INFER]` or rested on a single `[CERT-hw]` witness;
>     confirming something already backed by three independent [CERT] sources is ordinary block work,
>     not a classified corroboration event.
>
> **Ratio guidance does not apply:** The "high INFER/CERT ratio signals exhaustion" rule applies to
> EVIDENCE blocks seeking NEW facts. A corroboration block's ratio is expected to be LOW (many
> [CERT-hw], few [INFER]) by construction — that is its point. Do not read a low ratio as "nothing
> interesting here"; read it as "prior [INFER] elevated toward [CERT-hw] by independent witness."
>
> **Gap design for corroboration:** when a new focus targets an independent data store whose content
> is likely to CONFIRM (not extend) prior findings, seed the gaps as CORROBORATION gaps at bootstrap
> time. A focus with 4-5 corroboration gaps is not lower-priority than a discovery focus — independent
> confirmation of a security finding (e.g. "physical-SD = cleartext audit trail, independently
> confirmed from alarm.adb, log, provisioning .hdb, and audit .hdb") is often the evidence that
> elevates a threat model from [INFER] to actionable [CERT-hw].

**Priority:** MED — the ratio guidance, if read naively, could discourage seeding a corroboration focus
at all. Naming the type affirms that the investigation is doing real work even when no new fact is
discovered. Most relevant when a multi-focus corpus matures and later focuses naturally tend to
corroborate rather than discover.

---

## Delta R3 — MED — Bake the redaction into §19 reader tools for secret-bearing corpora

**Target:** PROMPT-LOOP.md §19 situational block + SECRETS DISCIPLINE (after D1 from the
station-config retro; D1 remains unchanged, this is a named APPLICATION of D1 at tool-build time)

**Evidence:** B699 / `tools/hdbread.py --mask`. The reader tool was built with a `--mask` flag that
redacts user identities and long values before printing, rather than applying sed/awk masking as a
post-processing step per invocation. The mask logic (a `mask()` function with a regex allow-list of
role accounts + a catch for base64-length strings) is IN the tool code, versioned in git, auditable
by review, and consistent across all subsequent invocations of hdbread.py across B700–B703.

**What the kit already covers (D1, prior retro):** D1 (station-config retro) prescribes:
generate in scratchpad, verify with `grep -c = 0`, test the pattern on a known sample first. D1 fires
at INVOCATION TIME: given that you are about to generate a redacted evidence file, do it safely. D1
does NOT prescribe what the TOOL ITSELF should look like.

**This delta is a D1 APPLICATION at TOOL-BUILD TIME:** When the §19 deliverable is a reader tool
for a secret-bearing corpus artifact, the `--mask` mode is a REQUIRED interface of the tool — not an
optional post-processing step. The distinction matters because:

(a) Ad-hoc `tool | sed 's/[a-zA-Z0-9]/x/g'` per invocation drifts: different invocations use
    different patterns; one-off masking that passes D1's `grep -c` gate for one block may use a
    different regex for the next, making the discipline inconsistent.
(b) A built-in `--mask` is AUDITABLE in the tool's source (the allow-list of role accounts, the
    pattern for values, the apply-per-string loop) — a reviewer can audit the mask logic once and
    trust all `--mask` invocations.
(c) The canonical safe invocation becomes `hdbread.py <file> --strings --mask` rather than a
    per-block pipe. D1's `grep -c` check then verifies the TOOL, not the shell pipeline.

**Proposed rule (propose-never-apply):**

> **TOOL-BUILT-IN MASK (§19 reader tools for secret-bearing corpora — D1 APPLICATION AT BUILD TIME).**
> When a §19 reader tool is built to parse a corpus artifact that carries OPERATOR SECRETS (audit
> records, log records, credential fields, usernames), the `--mask` mode is a REQUIRED feature of
> the tool at BUILD TIME, not an optional post-processing step. Specifically:
>
> - The tool's `--mask` flag MUST implement a documented allow-list of non-personal role tokens
>   (e.g. `admin`, `guest`, `root`, `niagarad`, `station`, `daemon`) and a catch-all regex for
>   credential-length or base64-format strings (`[A-Za-z0-9+/]{16,}={0,2}` → `<VALUE-MASKED>`).
> - The allow-list and catch-all are IN THE TOOL's source, reviewed at tool-build time (not
>   per-invocation), and versioned in git as part of the §19 block's evidence.
> - D1's `grep -c = 0` verification step still applies — to the OUTPUT of `tool --mask`, not to
>   a bespoke sed pipeline. The D1 workflow gates the evidence file; this rule gates the tool.
> - `tools/README.md` records the safe canonical invocation: `<tool> <file> --strings --mask`.
>   A future focus reusing the tool inherits the mask without re-deriving the regex.
>
> Scope: applies to §19 reader tools built for `live-install` or `firmware` artifact types where
> corpus records carry operator or user activity. A PoC that produces no persisted evidence file
> and is classified `one-off` (Delta R1) is exempt — it never reaches `sources/probes/`.

**Priority:** MED — the alternative (ad-hoc per-invocation masking) was used in the prior focus
before `hdbread.py` existed, and D1 provided the workflow discipline for each use. With a dedicated
reader tool, baking the mask in is strictly better: one audit, consistent behavior, no per-invocation
drift. Most relevant when a target accumulates multiple reader-tool iterations across focuses.

---

## Deduplication — considered, not re-proposed

1. **PRE-DECLARE REMITTANCES FIRST (content-vs-format split).** This focus pre-declared the `.hdb`
   FORMAT (4 remittance entries pointing to focus `database`) and seeded NEW gaps for the CONTENT.
   This is a natural application of the existing "PRE-DECLARE REMITTANCES FIRST" rule (PROMPT-LOOP
   step e). The format-vs-content split is the REASON a gap is not a full remittance; the rule already
   says "close by REMITTANCE... cite [Block N] §N.x + no new substance." Pre-declaring half the gap
   as remittance and keeping the other half as a new gap is correct practice under the existing rule.
   Not a new delta — the run handled it correctly, the rule already covers it.

2. **Station-config retro D1 (mask-verification workflow).** Delta R3 of this retro is a named
   APPLICATION of D1 at tool-build time. It does NOT propose altering D1's three-step per-invocation
   workflow (scratchpad → grep -c = 0 → test on known sample). D1 remains unchanged and complementary.
   R3 fires EARLIER (when building the tool), D1 fires LATER (when invoking it to generate evidence).

3. **Data-at-rest retro Delta A (structure-only inspection recipe).** Delta A names four read-only
   techniques (magic bytes, size, distinct-byte-count, delimiter skeleton). Delta R3 names a TOOL DESIGN
   pattern. They operate at different levels: Delta A says "HOW to inspect without printing secrets
   inline"; R3 says "WHERE to encode the redaction logic (in the tool, not the shell pipeline)."
   No overlap.

4. **Data-at-rest retro Delta B (secrets-sensitive inline override).** Delta B fires at routing:
   "stay INLINE when the artifact is secret-bearing." This run applied Delta B correctly (all 5 blocks
   are `no · inline`). Delta R3 addresses tool design, not routing. No overlap.

5. **Corroboration vs. SCOPING JUDGMENTS ARE HYPOTHESES.** The SCOPING JUDGMENTS rule says a prior
   block's "not load-bearing" closure is a testable hypothesis. Delta R2 does not test a prior
   scope-out; it affirms that CONFIRMING a prior finding from a new data store is worth a block.
   Different trigger, different guidance.

---

## Run quality notes (not kit deltas — operational observations)

- All 5 self-verify sections passed; `hdbread.py --ops / --strings --mask` output was the primary
  evidence for every block; `grep -c key-pattern` verified 0 secret values in each evidence file.
- Delta R1 (tool classification) was NOT applied during the run: `tools/README.md` records
  `hdbread.py` but does not carry a `tier:` annotation or `promote: YES` flag. This is the gap R1
  would close for future runs.
- The SYNTHESIS BLOCK REGISTRATION RULE was followed: HD5 closed with 0 new gaps, consistent with
  focus STOP and `investigable_open = 0`.
- `block_scope: shared-global` was correctly declared and `verify-state.sh` passed.
- Focus angle — "CONTENT not FORMAT" — was declared explicitly in the RESEARCH-STATE header and in
  the remittance section. This prevented re-investigating format facts already covered by focus
  `database`.
- qnx6read.py (prior focus) and hdbread.py (this focus) are the second and third instances of §19
  producing reader tools on this target. A `tools/README.md` tier-annotation rule (Delta R1) would
  have applied retroactively to qnx6read.py as well — both are "reusable corpus tool" tier, possibly
  "promotable" given their cross-station applicability.
