<!-- kit-retro -->
<!--
  focus: jace-station-config
  blocks: B685–B692 (8 blocks)
  date: 2026-08-30
  review-status: pending
  propose-never-apply: true
  absorb-targets: PROMPT-LOOP.md (two deltas)
-->

# §18 Retrospective — focus: jace-station-config (B685–B692)

**Run summary:** Opened a new focus over the deployed `config.bog` of a JACE-8000 field controller,
extracted READ-ONLY from its boot microSD (P2 QNX6). Source = one plaintext BOG XML (51 KB, ~1400
lines). 8 gaps, each targeting a distinct XML container subtree; 7 closed by delegated per-subtree
sonnet sweep + inline framework-semantic re-verify; 1 closed inline (synthesis). One requires-execution
child gap surfaced (SC4-G1). Thesis — "seed/template station" — confirmed 6+ independent ways and
synthesized in B692. SECRETS DISCIPLINE active throughout (live-install). One near-miss caught
before commit.

---

## Delta 1 — HIGH — Redacted-evidence-file mask-verification discipline

**Target:** PROMPT-LOOP.md — SECRETS DISCIPLINE block (append after the "MECHANIZED at the close"
sentence, before the ONE-BLOCK-PER-ITERATION bullet)

**Evidence:** B685 / commit `f03b6bcbb`. The SC1 sweep generated a redacted evidence file
(`sources/probes/B685-jace-station-config/services-inventory.txt`) by running a sed mask over
the extracted XML. The sed pattern did NOT match the actual password-field format silently —
the masked output still contained the raw PBKDF2 admin hash. The failure was discovered when
a `grep` diagnostic (used to verify the mask worked) printed the raw hash into the driver
transcript. The file was fixed before commit and the close-gate `scan-secrets.sh` would have
caught it anyway — but the raw hash transiently appeared in the driver's bash stdout during
the diagnostic step.

**What the kit already covers:** The SECRETS DISCIPLINE covers what NOT to write into blocks /
sources / engram, the conversation-as-exfil rule, the scratchpad-and-sha256 recipe for
secret-bearing bodies, and the `scan-secrets.sh` mechanized close gate. It does NOT describe
the workflow for generating a REDACTED copy of a secret-bearing artifact intended for
`sources/probes/`, nor does it warn against using `grep <pattern> <file>` (grep-print) as the
masking-verification step.

**Proposed rule (propose-never-apply):**

> **REDACTED-EVIDENCE-FILE GENERATION WORKFLOW (live-install / firmware).** When the plan is to
> preserve a REDACTED copy of a secret-bearing artifact in `sources/probes/`:
>
> 1. **Generate in scratchpad, never directly in `sources/`.** Run the mask (sed/awk/python) over
>    the raw extract into a scratchpad temp, not into the final destination.
> 2. **Verify the mask worked BEFORE printing or moving the file.** Use a SILENT count check:
>    `grep -c '<secret-pattern>' <masked-temp>` must return `0`. Do NOT use `grep <pattern>` (with
>    no `-c`) as the verification step — if the mask failed, that command prints the raw secret
>    value into stdout, placing it in the driver transcript and session logs. A silent count that
>    returns non-zero means the mask failed; fix and re-run before proceeding.
> 3. **Test the mask pattern on a known-sample first.** Before running the mask over the full
>    artifact, confirm the sed/awk pattern actually transforms a short snippet containing the secret
>    form (e.g. `echo '[pbkdf2-sha256.1]=salt:10000:hash' | sed 's/pattern/MASKED/'`). A pattern
>    that silently misses never produces an error — only a test on known input exposes it.
> 4. **Only after a verified zero-match, move to `sources/probes/` and register in SOURCES.md.**
>    The verified-clean redacted copy is preserved evidence; the scratchpad original (still
>    secret-bearing) is never committed.

**Priority:** HIGH — the existing scan-secrets.sh gate is a backstop, not a preventive. A mask
failure that passes silently may not always produce a grep diagnostic — it might go unnoticed
and reach `sources/` or the conversation if the gate is not exercised promptly. The mask-before-scan
discipline is cheapest at the generation step.

---

## Delta 2 — MED — XML-path-scoped sub-agent delegation for single-artifact config focus

**Target:** PROMPT-LOOP.md — step 3 INVESTIGATE, delegation paragraph ("DELEGATE heavy sweeps to
sub-agents")

**Evidence:** B685–B691 (7 of 8 gaps). Each iteration targeted a distinct named container within
the single `config.bog` XML: `/Services` (SC1), `/Drivers/NiagaraNetwork` (SC2), `/Drivers/NrioNetwork`
(SC3), `/Services/{UserService,RoleService,CategoryService,AuthenticationService}` (SC4),
`/Services/{AlarmService,AuditHistoryService,HistoryService,LoggingService}` (SC5),
`/Services/{TagDictionaryService,HierarchyService}` (SC6),
`/Services/{WebService,FoxService,BoxService,...}` (SC7). All 7 used a sonnet-tier sub-agent
scoped to the named container path, returning cited structural findings per subtree.

**What the kit already covers:** The delegation trigger is "more than ~3-4 files or classes." For
a single-artifact config focus (one XML/BOG/JSON), the relevant scoping dimension is XML CONTAINER
PATH, not file count. A single large XML does not cross the "3-4 files" trigger, yet delegating
each subtree sweep is clearly correct (the full XML at 1400 lines would bloat driver context and
a sub-agent sweeping one named container returns tight, scoped findings). This variant is NOT
named.

**Proposed rule (propose-never-apply):**

> **CONFIG-ARTIFACT DELEGATION VARIANT (single large XML/BOG/JSON, N container gaps).** When a
> focus targets a single large config artifact (e.g. a BOG XML, a JSON config, a registry dump)
> whose N investigation gaps each correspond to a DISTINCT NAMED CONTAINER or SUBTREE, scope
> each sub-agent delegation by CONTAINER PATH rather than file count — the "3-4 files" trigger
> does not apply to a single-file artifact. The delegation prompt specifies:
> (a) the full artifact reference (file path + line range if known), and (b) the EXACT container
> path to examine (e.g. `/Drivers/NiagaraNetwork`, L803-870). Each gap = one container = one
> block. The file:line citation convention holds; the container path substitutes for the
> class/module scope the file-count heuristic targets. This is the config-artifact sibling of
> the multi-file delegation trigger, not a replacement of it.

**Priority:** MED — this pattern was applied 7/8 times in this run and produced well-scoped, tight
blocks with minimal context bleed between gaps. Naming it makes it reusable for any future
single-artifact config focus (platform.bog, registry.db exports, system XML).

---

## Dedupes — considered, already in kit

The following candidates were evaluated and rejected because the kit already covers them:

1. **PRE-DECLARE REMITTANCES FIRST.** This run pre-declared ~10 remittances before any sweep
   (BOG format, NiagaraNetwork internals, RBAC model, tag subsystem, etc.). This is already in
   PROMPT-LOOP.md (step e, ~line 171): "PRE-DECLARE REMITTANCES FIRST (new focus over a mature
   MULTI-FOCUS corpus — before the sweep)." The deployed-config vs framework-internals scoping is
   the natural application of that rule, not a new one.

2. **Framework-semantic DE-ESCALATION.** Two DE-ESCALATIONs occurred (B686: "standalone" → leaf
   that lists no upstream; B688: "open-access fallback" → nil current impact). Both are exactly the
   named outcome the kit prescribes in PROMPT-LOOP.md step 5's framework-semantic check: "DE-ESCALATION:
   driver re-read subtracts a false finding. Record it by name." Already covered and working correctly.

3. **RE-MEASURE A DRAMATIC NEGATIVE.** Used in B686 (independently re-measured the 0 BNiagaraStation
   count before accepting it). Already in PROMPT-LOOP.md step 3 under VERIFY BEFORE ACTING.

4. **Requires-execution child gap classification.** SC4-G1 (dangling category-index runtime behavior)
   was correctly classified as requires-execution and registered in the backlog table. The three-bucket
   stopping criterion (read-only-investigable / requires-execution / blocked) already covers this in
   METHODOLOGY §8. The SYNTHESIS-BLOCK REGISTRATION RULE (register requires-execution gaps in the
   backlog table, not only in iteration-history) is also already in PROMPT-LOOP.md step 6.

5. **scan-secrets.sh close gate.** The mechanized gate at `research-sdd-archive.sh` already covers
   the backstop role. Delta 1 above proposes the PREVENTIVE workflow, which is complementary and not
   already in the kit.

---

## Run quality notes (not kit deltas — operational observations)

- All 8 self-verify sections passed; framework-semantic re-reads caught both DE-ESCALATIONs before
  block finalization. No §14 corrections were needed across the focus.
- The "seed/template station" thesis was stated as [INFER] in B685 and upgraded to [CERT-hw]
  in B692 after 6 independent subtree confirmations — correct escalation ladder.
- SC4-G1 was added to the backlog table (not only iteration-history) — SYNTHESIS-BLOCK REGISTRATION
  RULE followed correctly.
- The PBKDF2 hash was visible in B685's block text in MASKED form (`<SALT>:&lt;10000&gt;:&lt;SHA256&gt;`)
  — the format is cited, the value is not; SECRETS DISCIPLINE held in the final corpus content.
