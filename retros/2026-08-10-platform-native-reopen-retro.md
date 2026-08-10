<!-- kit-retro -->
<!-- review-status: pending -->
# Retro — niagara-research · platform-native · 2026-08-10 · Research-SDD self-retrospective

> Run reviewed: platform-native focus reopening (B424 getHostId, B425 DSF crypto SPI), commits `2470673`
> and `1fef9d5`. Trigger: focus re-STOP after a reopen — the 2026-08-07 STOP (investigable=0) was a
> false negative corrected on 2026-08-10 when two uncaptured Ghidra dump files were discovered in
> `tools/ghidra-scripts/`.
> Method: a FRESH-CONTEXT agent read `PROMPT-LOOP.md` + `METHODOLOGY.md` (esp. §8, §11, §17, §18) first,
> then reviewed `RESEARCH-STATE-platform-native.md` (stop-control section), `niagara-mental-model-bloque424.md`,
> `niagara-mental-model-bloque425.md`, git log, and the dump files in `tools/ghidra-scripts/`. READ-ONLY on
> the kit — this report only PROPOSES; kit changes are human-reviewed and human-committed (METHODOLOGY §18).

## Proposed kit deltas

> Only genuinely NEW items. Anything the kit already encodes is listed under "Already covered" below.
> Each delta: the concrete change · the target file/section · evidence · priority.

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | **PRE-STOP ARTIFACT AUDIT** — before honoring a STOP / `investigable=0` declaration, the closing agent MUST check `$TARGET/tools/` and `$CORPUS/audits/` for decompiler output artifacts (dump files: `.txt`, `.c`, `.json` produced by Ghidra/r2/jadx scripts) that are NOT cited in any corpus block. An uncited artifact = body-grade evidence produced but never captured = FALSE-NEGATIVE exhaustion signal. Gate: if any uncited dump exists whose content covers a gap with no block-level body evidence, the STOP must NOT be honored — the gap is investigable, not exhausted. | `PROMPT-LOOP.md §7 STOPPING` (add a bullet before "MECHANIZE THE CLOSE") + `METHODOLOGY.md §8` (add to "Stopping criterion" note) | `RESEARCH-STATE-platform-native.md §Stop control` "REOPENED 2026-08-10 — coverage audit found `decomp-hostid.txt`/`decomp-dsfspi.txt` never captured"; `tools/ghidra-scripts/decomp-hostid.txt` + `decomp-dsfspi.txt` dated 2026-08-01 (nine days before capture); the 2026-08-07 `investigable=0` declaration was false | new | HIGH |
| 2 | **DECOMPILER DUMP OFFSET PROVENANCE (twin-binary check)** — when a preserved decompiler dump carries VMA/RVA function offsets, verify those offsets against the SPECIFIC shipped binary (sha256-anchored) with a live disassembler (r2 or Ghidra) BEFORE citing any offset. The same function compiled into two related binaries (twin binaries) sits at DIFFERENT offsets; a dump produced from binary A cannot be cited for binary B even if the C source is identical. Action: (a) record the sha256 of the binary that produced the dump at the time of decompilation, (b) confirm each cited VMA/RVA is present at that address in the sha256-anchored binary via `r2 -q -c "s <addr>;pd 4" <binary>` or equivalent, (c) if the anchored binary differs, re-decompile against the correct binary before citing. | `METHODOLOGY.md §5` (citation provenance — add a subsection after the "docSource dual-tree" note) or `METHODOLOGY.md §6` (native RE toolbelt note) | `niagara-mental-model-bloque424.md §424.1` "twin binary caveat" — Ghidra image offsets (`getHostVendor@0x5090`, `getVolume@0x5fa0`) did NOT match shipped `njre.dll` (sha256 `7007ff82…`); the offsets belonged to the twin `nre.dll`; resolved by re-verifying live in r2 against njre.dll. The fix was correct but undirected — the kit had no rule naming this failure mode. | new | MEDIUM |

For each delta above, one line of rationale:

- **#1** — A produced-but-uncaptured decompiler dump is the most silent false-negative exhaustion signal: the
  evidence EXISTS on disk, but the corpus BELIEVES it never ran. The fix costs one `fd`/`rg` sweep at STOP time;
  the miss costs a full reopen (two iterations, a commit, a retro). The rule closes the "tool ran but block
  never followed" gap that §8's investigable-classification audit does not see because it operates on the
  backlog, not on `tools/`.

- **#2** — Citing wrong function offsets would publish a FALSE `[CERT]` into the corpus — a reproducibility
  failure (a reviewer following the `audits/B424-…:offset` citation would land at the wrong instruction).
  The existing RE-MEASURE GROUND-TRUTH rule (PROMPT-LOOP HARD RULES) covers dynamic/hardware identifiers;
  it does not address static decompilation provenance, where the hazard is a twin binary whose dump was
  preserved before the shipped binary was sha256-anchored. Cost: one r2 spot-check per cited offset before
  authoring. Already caught inline in B424 — now needs to be a named pre-authoring gate for native RE blocks.

## Already covered (dedupe — proof the retro read the kit first)

- **VERIFY-BEFORE-ACTING on delegated sonnet sweep (B425 Mocana findings)** — already covered by
  `PROMPT-LOOP.md` HARD RULES "VERIFY BEFORE ACTING on a sub-agent's report … token-check every
  load-bearing token". Applied correctly: every Mocana string + import table entry was verified live via
  r2/rabin2 against `dsfspi.dll` (sha256 `82e8c7f0…`) before authoring B425. Outcome: held, no new delta.

- **RE-MEASURE GROUND-TRUTH (general dynamic phase rule)** — already in `PROMPT-LOOP.md` HARD RULES
  ("RE-MEASURE GROUND-TRUTH, never inherit it. When entering a DYNAMIC/hardware phase … re-measure
  ground-truth identifiers LIVE from the real system"). The B424 twin-binary case triggered a STATIC
  analog of this (re-verify offsets against the shipped binary), but the existing rule's wording scopes
  it to dynamic/hardware phases only — so the static extension (delta #2 above) is genuinely new.

- **One-block-per-commit, investigable re-check after each block** — correctly applied: B424 (commit
  `2470673`) and B425 (commit `1fef9d5`) landed in separate commits; STOP was re-evaluated after each.

## Anti-patterns observed

- **Decompiler dumps produced but not tracked** → delta #1 prevents it: a pre-STOP artifact audit
  catches any dump in `tools/` or `audits/` with no corpus citation before the STOP is honored.
- **Twin binary offset confusion** → delta #2 prevents it: a sha256-anchored live offset-verify before
  any VMA/RVA citation is authoured catches the mismatch before the block is written, not after.

## Tools built, adapted, or outgrown

- `tools/ghidra-scripts/DecompileHostId.java`, `DecompileDsfspi.java` — Ghidra headless scripts producing
  the dump files whose outputs triggered both deltas above. Not new this run (produced 2026-08-01);
  surfaced as relevant in the reopen. No new tool decisions this run.

## Run statistics

- Blocks written: 2 (B424, B425)
- Commits: `2470673` (B424), `1fef9d5` (B425)
- Gaps closed: NG5 getHostId (body grade), NG6 DSF crypto SPI (body grade)
- Coverage: 7/7 static gaps closed (platform-native focus)
- New gaps uncovered: 0
- Investigable after reclose: 0 (static loop re-STOPPED)
- Proposed deltas: 2 (1 HIGH, 1 MEDIUM)
