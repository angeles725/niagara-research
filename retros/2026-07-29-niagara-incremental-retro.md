<!-- review-status: applied 2026-07-29 · kit 73a6131 -->
# Retro — niagara-research · B271-B293 · 2026-07-29 · Research-SDD self-retrospective

> Run reviewed: B271-B293 (23 blocks), the increment after `2026-07-24-tags.md` (which covered through B270
> and was applied 2026-07-29 · kit cc5e13a). This retro covers ONLY the delta: blocks written AFTER B270.
> Trigger: MISSING-RETRO flag (corpus advanced past newest retro). Method: fresh-context agent read the
> current kit (`PROMPT-LOOP.md` full · `METHODOLOGY.md` full, including §18-§20) FIRST, deduped against
> all 9 existing retros under `niagara-research/retros/`, then sampled 6 of 23 new blocks directly
> (B271, B279, B280, B285, B290, B293) plus RESEARCH-STATE.md. READ-ONLY on the kit — this report only
> PROPOSES; kit changes are human-reviewed and human-committed (METHODOLOGY §18).

## Boundary

Newest existing retro: `2026-07-24-tags.md` covers focus `tags` B260-B270 (applied 2026-07-29 · kit
cc5e13a). `2026-07-24-px-chart-classic.md` covers B251-B259 (same kit commit). Both `applied`.

Blocks in scope: **B271-B293** (23 blocks). Block B271 is the first new block after the tags focus
closed. Block B293 is the current corpus tip as of this retro.

## Run summary

B271-B293 spans two distinct work modes:

**BACnet protocol deep-dives (B271-B289 approximately):** gap-filling research on BACnet internals that
B23 / B133 named but never fully opened. B271 documents the BACnet discovery-to-point pipeline (how N4
maps WHO-IS/I-AM to `BNumericPoint`/`BBooleanPoint` etc.), drawing on original Tridium source under
`docSource/`. B279 investigates P3-mstp (MS/TP data-link framing): finds that framing is NOT in Java —
it lives behind a JNI boundary in native `platMstp-rt` code — and reclassifies the gap. B280 closes
P3-sc (BACnet/SC transport), which IS fully in Java (40 classes). B285 documents the EMSTP co-processor
host protocol (17 commands, 4-prefix command byte, 10-state bring-up, SAM4S firmware behind JNI). The
investigation pattern mirrors the niagara corpus's established three-step protocol: project blocks first
→ `niagara-help` guides → module-navigator.

**DOCUMENT-MODE applied sessions (B290-B293, METHODOLOGY §20):** B290 documents the procedure for
reading a live Niagara station over HTTP from WSL (Basic-auth gotcha, `config.bog` as offline surface),
citing the preserved probe `sources/probes/live-20260727T012800Z-station-pruebas-filespace-and-obix.txt`.
B293 documents the `PxInclude` navigation-menu pattern (customer request: inherited menu with active-tab
shading), citing official Tridium guide plus live station `[CERT-live]`. Both blocks correctly apply §20
conventions: DOCUMENT-MODE header, outline-driven (not gap-driven), SECRETS DISCIPLINE active, `[CERT-live]`
markers for session-observed facts.

---

## Proposed kit deltas

> Only genuinely NEW items — anything the kit already encodes is listed under "Already covered", not here.
> Each delta: the concrete change · the target file/section · evidence · priority.

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | In PROMPT-LOOP.md NORMAL CYCLE step 3 INVESTIGATE, add a bullet BEFORE the decompile/read tools: "PRIOR COVERAGE CHECK: Before launching any decompile, source-scan, or tool sweep, read any existing corpus blocks whose INDEX.md description overlaps this gap's topic — especially the block that originally opened the gap. A gap carried forward from B_n may already document the sub-topic you are about to sweep; investigating without reading B_n risks confirming what the corpus already knows and missing corrections already issued to it. Cost: one INDEX.md lookup per gap. Best done at the CHOOSE step (step 1) when the gap source block is known." | `PROMPT-LOOP.md` NORMAL CYCLE step 3 INVESTIGATE — new bullet, first in the list | B279 header §0 (REVISED 2026-07-26): "The first version of this block ran only module-navigator and the `organized/` corpus, skipping two steps of the project's research protocol: it did not query `niagara-help`, and it did not check whether existing project blocks already covered MS/TP. Both omissions mattered." Gap P3-mstp was opened BY B133; B133 already documented the JNI boundary. The omission required a block revision to add §279.9. B280 §280.1 explicitly names what the corrected protocol does ("ran all three protocol steps before concluding: project blocks first, then `niagara-help`, then module-navigator / `organized/`"). | new | LOW |

For the delta above, one line of rationale:

- **#1** — The pre-loop setup already reads INDEX.md (PROMPT-LOOP.md step 5), but INVESTIGATE step 3
  gives no explicit reminder to cross-check existing blocks before opening tool sweeps. B279's first
  version read module-navigator without reading B133 (the block that opened P3-mstp), then had to be
  revised when it emerged that B133 had already documented the JNI boundary. The cost of a revision
  is low, but the pattern is preventable. Naming the check at the CHOOSE or INVESTIGATE step makes it
  explicit rather than implied by the INDEX.md pre-read.

---

## Already covered (dedupe — proof the retro read the kit first)

- **DOCUMENT-MODE (§20) applied to live-station captures** → already covered by METHODOLOGY §20 ("the
  SESSION is the evidence"; `[CERT-live]` markers; SECRETS DISCIPLINE hard invariant). B290 and B293
  apply it correctly. No delta.
- **SECRETS DISCIPLINE on live-install blocks** → already covered by PROMPT-LOOP.md HARD RULES. B290
  §0 states "no credential values, hashes, keystore or certificate material appear in this block" and
  describes how credentials were passed outside argv. No delta.
- **Sub-agent scope for proven-absence** → already in PROMPT-LOOP.md step 3 INVESTIGATE after
  cc5e13a: "SCOPE of a sub-agent's proven-absence is narrower than the full corpus." B279's finding
  ("MS/TP framing is NOT in Java") is a correctly-scoped negative at the corpus level (confirmed by
  existing B127 native-driver boundary block AND by the §279.9 revision). No additional scope rule
  needed — the existing rule applies.
- **Checking INDEX.md for existing coverage** → partially covered: PROMPT-LOOP.md step 5 pre-reads
  INDEX.md. Delta #1 is DISTINCT: it proposes a per-gap reminder at the INVESTIGATE step, not a
  change to the one-time pre-loop setup read. A step-5 INDEX.md read is necessary but insufficient
  when the investigator then dives into module-navigator without revisiting the specific source block.
- **`niagara-help` as a mandatory source before code-sweep** → this is a niagara-specific three-step
  protocol, NOT a kit-level rule. The kit says "Documents: if you find a relevant datasheet/manual/
  forum, DOWNLOAD it" (step 3). The niagara protocol's specific ordering (project blocks → niagara-help
  → module-navigator) is a target idiom. No kit delta.
- **Block self-revision vs §14 cross-block correction** → B279 revised itself on 2026-07-26 (adding
  §279.9) rather than having a later block issue a §14 to B279. The revision was correct (B280
  references B279 §279.9 to acknowledge the method correction). §14 says a LATER block corrects an
  earlier one — that is the preferred path when the correcting block exists. B279 was revised before
  B280 was written, so the §14 model was not available. The resulting corpus is consistent and
  auditable. No kit delta warranted.
- **EMSTP as a "fully-decompilable layer" when the wire framing is not** → already covered by METHODOLOGY
  §8 negative-closure criteria and by the reclassification-of-gap pattern (B127's native-driver boundary
  catalogue; B135's P5-phys LON pattern). B279 correctly names the analogous shape: "The shape is
  identical to P5-phys in [B135] and to the native driver boundaries catalogued in [B127]." No delta.

## Anti-patterns observed

- First version of B279 (P3-mstp) ran module-navigator without consulting existing blocks or
  `niagara-help` for MS/TP coverage. Both omissions mattered: B133 had already documented the JNI
  boundary; the official EngNote `bacnetUtil-Tokens.txt` supplied hardware identity and queue depth
  absent from the code. The block was revised (§279.9 added on 2026-07-26) rather than left as the
  initial narrow finding. → proposed delta #1.

## Tools built, adapted, or outgrown

| # | CREATED (path · purpose) | ADAPTED (kit tool · what the kit version could not express) | OUTGREW (kit tool · why stopped) | ORACLE (tool · what it SEEs, not recomputes) | VERDICT (decision · evidence) |
|---|---|---|---|---|---|
| T1 | — | — | — | — | No new tools identified in the sampled blocks. B290 uses a preserved probe file (§20 convention), not a new tool. Standard decompile wrappers and module-navigator used throughout. |

## Metrics

- **Blocks reviewed directly**: 6 of 23 (B271, B279, B280, B285, B290, B293) · balance understood from
  RESEARCH-STATE.md summaries and cross-references within read blocks
- **§14 cross-block corrections in this run**: at least 2 identified (B280 §280.7 corrects two numbers
  in B23; B279 revision corrects itself — see above); full count not established (not all 23 blocks read)
- **Rules skipped in practice**: 1 (B279 first version — prior-coverage check, per anti-pattern above)
- **Deltas proposed (new)**: 1 (LOW) · **Already-covered lessons**: 6

## Honest verdict

The 23-block increment surfaces one genuine operational gap: the research protocol does not explicitly
prompt for a prior-coverage check at the INVESTIGATE step, only implicitly via the pre-loop INDEX.md read.
B279's self-correction demonstrates the gap is real and self-resolvable within a single run — but naming
the check explicitly prevents the revision overhead on future runs.

The DOCUMENT-MODE blocks (B290, B293) are applied correctly: §20 format, `[CERT-live]` markers, SECRETS
DISCIPLINE held, probe files preserved. No gap there.

The single proposed delta is LOW priority: the failure was contained (one block revised, corpus consistent),
and the corrected protocol was then applied correctly in B280. Still worth naming because B279 ↔ B133 is
not the first time an investigation swept module-navigator without checking what an older block already said
about the same topic.
