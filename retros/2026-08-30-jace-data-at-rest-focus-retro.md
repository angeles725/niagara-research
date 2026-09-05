<!-- kit-retro -->
<!-- review-status: applied 2026-09-05 · kit 185ad74 · shipped: ΔA, ΔB (PROMPT-LOOP, PR #445 fe88d17), ΔC (§14 threat-model axis, PR #448 185ad74) -->
<!--
  focus: jace-data-at-rest
  blocks: B693–B698 (6 blocks)
  date: 2026-08-30
  review-status: pending
  propose-never-apply: true
  absorb-targets: METHODOLOGY.md (two deltas) · PROMPT-LOOP.md (one delta)
-->

# §18 Retrospective — focus: jace-data-at-rest (B693–B698)

**Run summary:** MAXIMUM-SECRETS focus — subject was the JACE-8000 boot microSD's secret stores
(keyrings `.kr`/`.km`/`.fskey`, OS creds `/etc/shadow`, TLS keystores, JRE crypto config). 6 gaps,
all closed inline. Central finding: physical SD possession = near-total data-at-rest compromise
(`.km` is a 32-byte cleartext master key on the same card the hardware root-of-trust was supposed to
protect; only 1-way login hashes resist). §14 refined B466's threat model scope. Zero secrets
leaked to corpus or transcript — Retro D1 lesson applied throughout and validated (6/6 blocks,
`grep -c` = 0 each time).

---

## Delta A — HIGH — Structure-only binary inspection recipe for secret-bearing artifacts

**Target:** METHODOLOGY.md §6 (append after the "Entropy + byte histogram as a read-only encryption
test" paragraph) — or, alternatively, PROMPT-LOOP.md SECRETS DISCIPLINE block (append after the
REDACTION CHECKLIST)

**Evidence:** B693 / B695 / commits `afe2df14d` / `bb74052c9`. Three techniques used in this focus
to determine FORMAT without ever printing a secret value:

1. **Magic bytes + file(1):** `.kr` identified as Java serialized object (`AC ED 00 05`) without
   reading its body. `keystore.jceks` identified as JCEKS (`CE CE CE CE`). `signing/signers`
   distinguished from JCEKS (`00 2E 5B 61`).
2. **Size + distinct-byte-count (entropy proxy):** `.km` identified as a bare 256-bit AES key (32 B,
   no wrapper, no magic) by measuring size + `od | awk` distinct-byte-count — high byte diversity
   with no header structure = raw key. `.kr` body identified as high-entropy (ciphertext/wrapped key
   material) vs the plaintext serialization envelope.
3. **Delimiter skeleton (new technique):** To identify the `/etc/shadow` hash FORMAT without showing
   any hash byte, the shadow line was run through an alnum→`x` substitution (e.g.
   `sed 's/[a-zA-Z0-9]/x/g'`), reducing `@v,pbkdf2-sha256.1=N=SALT:HASH=` to
   `@x,xxxxxxxxxxx.x=x=xxxx:xxxx=` — the FORMAT SKELETON. Field delimiters, separators, lengths,
   and the algorithm LABEL survive; no hash/salt value is ever printed. B695 §695.2 identified
   PBKDF2-HMAC-SHA256 (`@v,iter@salt@hash=`) as the format this way.

**What the kit already covers:**

- §6 "Entropy + byte histogram as a read-only encryption test" — answers "is this blob encrypted?"
  via Shannon entropy (bits/byte) and histogram shape. The question it answers is DIFFERENT: this
  test classifies encrypted vs. plaintext framing; it does NOT identify the FORMAT of a secret field
  without revealing its value. The kits's SECRETS DISCIPLINE says "cite structure, not value" but
  does not describe HOW to extract the structure safely.
- The delimiter-skeleton technique is NOT mentioned anywhere in METHODOLOGY.md or PROMPT-LOOP.md.

**Proposed rule (propose-never-apply):**

> **STRUCTURE-ONLY BINARY INSPECTION RECIPE (secret-bearing artifacts — live-install / firmware).**
> When a gap requires identifying the FORMAT or TYPE of a secret-bearing binary file or field without
> ever printing its value, use this ordered recipe. NONE of these steps print key/hash bytes:
>
> 1. **Magic bytes.** `od -A x -N 8 -t x1z <file>` — identifies the container format (Java
>    serialization `AC ED 00 05`, JCEKS `CE CE CE CE`, non-JCEKS `00 2E 5B 61`, etc.) from the
>    first 8 bytes only.
> 2. **Size.** `wc -c <file>` — identifies key length (e.g. 32 B = AES-256 raw key, 665 B = wrapped
>    keyring blob) without reading the body.
> 3. **Distinct-byte-count (entropy proxy).** `od -An -tu1 <file> | tr ' ' '\n' | sort -nu | wc -l`
>    — counts distinct byte values. High diversity (200+ distinct values) = ciphertext/wrapped key
>    material; low diversity = framing bytes or a plaintext structure. Cheaper than full Shannon
>    entropy; sufficient for a binary vs. structured judgment.
> 4. **Delimiter skeleton.** When the target is a TEXT-FORMAT secret field (Unix shadow line, a
>    hash record, a token), substitute all alnum characters with a single placeholder:
>    `sed 's/[a-zA-Z0-9]/x/g'`. The resulting skeleton reveals field separators, prefix tags,
>    and segment counts (format) while eliminating every hash/salt/key byte. Quote only the skeleton
>    in the block, never the original.
>
> This recipe answers "what FORMAT is this secret-bearing field?" The §6 entropy test answers the
> orthogonal question "is this blob encrypted?". Run whichever question the gap needs; they are
> complementary, not redundant.

**Priority:** HIGH — this was the enabling technique for every DAR1/DAR3/DAR4 claim in this focus.
Without it, citing the keyring format, the `.km` "bare key" nature, or the shadow hash algorithm
would require printing the actual value (exfil) or staying at [INFER] (epistemic failure). The
technique is not situational; it applies to any security-focused corpus that touches live secret
stores.

---

## Delta B — HIGH — Inline-over-delegate override for secrets-sensitive artifacts

**Target:** PROMPT-LOOP.md step 3 INVESTIGATE, delegation paragraph ("DELEGATE heavy sweeps to
sub-agents"), as a new carve-out bullet before the model-tier rule

**Evidence:** All 6 blocks (B693–B698), iteration-history column: `no · inline (secrets-sensitive:
magic/entropy only, no key bytes)` / `no · inline (analysis + §14 refine B466)` / `no · inline
(secrets-sensitive: skeleton only, hashes masked)` / etc. The rationale was explicit in every row:
delegating would route the sub-agent's CITED FINDINGS (which, for a secrets-bearing gap, would
include key/hash material) back through the sub-agent's report and into the driver context.

**What the kit already covers:**

- The delegation trigger is "more than ~3-4 files or classes" → delegate one sub-agent. The prior
  retro (2026-08-30-jace-station-config, Delta 2) extended this with "XML-path-scoped delegation
  for single-artifact config focus." Neither rule names a SECRETS-SENSITIVITY override.
- The SECRETS DISCIPLINE says "cite structure, not value" and covers the mask-verification workflow
  (D1 from the sibling retro). It does NOT say: "if the artifact is secret-bearing, stay INLINE even
  when the file count or artifact scope would normally trigger delegation."
- PROMPT-LOOP.md already allows recording a constrained inline run as `inline (constraint: <reason>)`,
  implying such overrides are valid — but the secrets-sensitivity case is not named as a trigger.

**Why this is not a duplicate of D1 (prior retro):** D1 prescribes the mask-verification WORKFLOW
after you've decided to inspect a secrets-bearing artifact inline. Delta B prescribes the
ROUTING DECISION: whether to delegate at all. They operate at different stages: Delta B fires at
"which agent runs the sweep?" and D1 fires at "now that you're running it inline, how do you
inspect safely?"

**Proposed rule (propose-never-apply):**

> **SECRETS-SENSITIVE INLINE OVERRIDE.** The file-count delegation trigger ("more than ~3-4 files")
> and the config-artifact delegation variant (XML-path scoped) are context-free routing heuristics.
> They are OVERRIDDEN when the artifacts under investigation are SECRET-BEARING (key files, shadow
> hashes, keystores, credential configs). In that case, stay INLINE regardless of file count,
> because:
>
> (a) A delegated sub-agent's CITED FINDINGS include the load-bearing excerpts that support each
>     claim. For a secrets-bearing artifact, those excerpts are key bytes, hash values, or credential
>     strings — exactly the material SECRETS DISCIPLINE forbids from appearing in the driver's context.
> (b) The driver is the only context subject to the SECRETS DISCIPLINE throughout; a sub-agent's
>     report is NOT governed by the mask-before-print / grep-c discipline in the same way.
>
> Record the choice explicitly in the iteration-history tier column as
> `no · inline (constraint: secrets-sensitive — <artifact type>)` so the override is auditable and
> does not read as a missed delegation. The STRUCTURE-ONLY INSPECTION RECIPE (Delta A, this retro)
> provides the safe inline technique.
>
> Scope: this override applies to gaps whose PRIMARY artifact is a secret-bearing file (keyring,
> shadow, keystore, config with embedded credentials). It does NOT apply to a gap that merely PASSES
> THROUGH a directory that happens to contain secret-bearing files — only when the SECRET STORE IS
> THE SUBJECT of the investigation.

**Priority:** HIGH — the delegation trigger is the default routing decision; without a named
override, a future run over a live-install secret store would correctly count "1 file" and conclude
delegation is not triggered, missing the dimension that matters. The risk is not a missed delegation
(inline is slower) but a correct delegation that exfiltrates key material through the sub-agent report.

---

## Delta C — MED — Extend §14 REFUTE vs CLARIFY-SCOPE to cover threat-model-axis scoping

**Target:** METHODOLOGY.md §14 (after the "REFUTE vs CLARIFY-SCOPE — distinguish them" paragraph,
lines 1700-1706)

**Evidence:** B694 §694.3 / commit `afd09225c`. B466 stated (from Tridium docs, [CERT-doc]):
"non-exportable machine key → secrets unrecoverable off-box, period" — flagging a raw filesystem
grab as insufficient. B694 found that the machine-key domain is anchored in a SOFTWARE keyring on
the SD card, not an ECC508 HSM. The §14 issued was labeled "§14 REFINE (threat-model scoping, not
a flat reversal)" — because B466's claim is STILL TRUE for the network/tool threat model (an
attacker who does not have physical SD possession cannot extract the key off-box via tool APIs or
the daemon), but it is FALSE for the physical-SD threat model (an attacker WITH the SD card has the
cleartext `.km` key on disk).

**What the kit already covers:** METHODOLOGY §14 lines 1700-1706:

> "**REFUTE vs CLARIFY-SCOPE — distinguish them.** A **refute** means the prior claim was WRONG. A
> **scope-clarification** means the prior claim was RIGHT for a DIFFERENT artifact/build... Only call
> it a refute when the two describe the SAME artifact."

The existing rule covers build/artifact divergence (dev-tree vs shipped binary; version X vs Y).
B694's case is: SAME artifact, SAME build, DIFFERENT THREAT MODEL AXIS. B466 is not wrong for one
version and right for another — it is right for one attacker model and wrong for another. This is a
third variant the kit does not name.

**Why this matters:** Without a named variant, the agent must choose between "REFUTE" (B466 was
wrong) or "CLARIFY-SCOPE" (B466 was right for a different build). Neither fits. Calling it a refute
overstates the error — B466 IS correct for the network attacker. Calling it a build-scope
clarification misdirects the reader (there is no version difference). A future run in a
security-focused corpus will encounter this pattern often (threat models multiply: local-user vs
remote-unauthenticated vs physical-attacker).

**Proposed rule (propose-never-apply):**

> **CLARIFY-THREAT-MODEL (a third §14 variant).** A prior claim may be simultaneously TRUE for one
> threat model and FALSE for another. When new evidence shows this, issue a §14 SCOPING that adds a
> threat-model qualifier to the prior block rather than a flat refutation:
>
> - The prior block's claim is NOT struck (it remains valid within its original threat model scope).
> - A scope note is appended to the prior block: "correct for the [network/tool] threat model; for
>   the [physical-SD] threat model see [Block N] §N.x (§14 CLARIFY-THREAT-MODEL)."
> - The new block carries its own affirmative finding for the additional threat model.
> - The iteration-history action is labeled `§14 CLARIFY-THREAT-MODEL` (not REFUTE, not CLARIFY-SCOPE).
>
> Distinguish from the existing CLARIFY-SCOPE: that variant covers build/artifact divergence (the
> claims describe objectively different objects). CLARIFY-THREAT-MODEL covers same-artifact, same-build
> divergence across attacker capability axes. "Correct for the network threat model; wrong for the
> physical-possession threat model" is the canonical trigger.

**Priority:** MED — the pattern is recurrent in security-focused corpora and in infrastructure
hardware research (JACE, PLC, firmware). Without the named variant, the §14 action is mislabeled
or skipped entirely. The risk is asymmetric: a missed threat-model scoping leaves a downstream reader
believing B466's "unrecoverable off-box, period" holds under ALL threat models.

---

## Deduplication — considered, not re-proposed

**Prior retro (2026-08-30-jace-station-config) D1 — redacted-evidence mask-verification workflow.**
Delta B of THIS retro addresses the earlier routing decision (inline vs. delegate). D1 addresses what
to do ONCE you are inline: mask-before-print, `grep -c`, test the pattern on a known sample. These
are complementary and operate at different stages. Delta B does NOT subsume D1.

**D1 application validity note (operational, not a new delta):** D1 was applied 6/6 times in this
focus (every block header cites "Retro-D1 lesson applied; `grep -c` verified evidence file = 0")
and zero leaks occurred. This is VALIDATION evidence for D1's promotion — the pattern was applied
under adversarial conditions (every artifact was a real secret) and held. The promotion decision
itself belongs to the retro review process for the prior retro.

**Prior retro D2 — XML-path-scoped delegation for single-artifact config focus.** No overlap with any
delta in this retro.

**§6 entropy test vs. Delta A delimiter-skeleton.** The entropy test is already in the kit and was
used here too (B693 distinct-byte-count on `.km`). Delta A does NOT re-propose the entropy test. It
proposes the ordered recipe (magic + size + distinct-byte-count + delimiter-skeleton) as a UNIFIED
structure-inspection pattern, with the delimiter-skeleton technique as the novel element.

**SCOPING JUDGMENTS ARE HYPOTHESES (PROMPT-LOOP step 3) vs Delta C.** The kit says prior scope-outs
are testable hypotheses. Delta C is about the TYPE of §14 correction issued once a scope-out is
refuted — specifically, when the prior claim was correct for one threat model but not another. The
existing rule says "issue a §14 correction"; Delta C names a variant to issue that is NOT a flat
REFUTE, saving the prior block from being read as erroneous when it was correct within its scope.

---

## Run quality notes (not kit deltas — operational observations)

- SECRETS DISCIPLINE held throughout: no key/hash value appears in corpus blocks, sources/, or this
  retro. The `sources/probes/B693-jace-data-at-rest/` evidence files were generated with the
  structure-only recipe and verified `grep -c key-pattern = 0` before commit.
- All 6 self-verify sections passed; `verify-block.sh` tally confirmed in each block.
- §14 back-pointer added to B466 (`§14 REFINE from B694`) in the same commit as B694 — ORPHANED
  CORRECTION rule followed correctly.
- DAR2-G1 (decryption PoC) correctly classified as requires-execution and registered in the backlog
  table (not only iteration-history) — SYNTHESIS-BLOCK REGISTRATION RULE followed.
- The "physical SD = near-total compromise" verdict was stated as [INFER] in DAR2 and carried
  [CERT-hw]+[INFER] in the synthesis (B698) — correct escalation given that the PoC was not
  executed.
