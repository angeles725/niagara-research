<!-- kit-retro -->
<!-- review-status: pending -->
# Retro — niagara-research · workbench · 2026-08-10 · Research-SDD self-retrospective

> Run reviewed: **workbench** focus bootstrap + 12 gaps closed in one autonomous orchestrated session
> (B427-B438, commits `9bb0a8d`–`514c437`). Trigger: focus-level STOP after 12/12 gaps, investigable=0.
> Method: fresh-context agent read `PROMPT-LOOP.md` + `METHODOLOGY.md` (esp. §5, §13, §18) FIRST, then
> `RESEARCH-STATE-workbench.md` (gap backlog, iteration history), skimmed B427 / B431 / B434 / B438, and
> the 2026-08-10 platform-native retro for deduplication. READ-ONLY on the kit — proposes only, does NOT
> edit. Kit changes are human-reviewed and human-committed (METHODOLOGY §18).

## Proposed kit deltas

Only genuinely new items. See "Already covered" section for the deduplication audit.

| # | Proposed change | Target (file · §/section) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| 1 | **VINEFLOWER PARTIAL-MANGLE citation convention** — Vineflower sometimes fails to reconstruct the class-name token in the decompiled `.java` body (e.g., `public abstract class ln extends BWbFieldEditor`) while the **file name** (which Java tooling preserves from the `.class` entry) and **parent type** stay real. This is a DECOMPILER ARTIFACT, not intentional obfuscation. The citation strategy: (a) cite by FILE PATH (the file name IS the real class name per Java convention) + EXISTENCE and PARENT TYPE, never by the garbled in-body token; (b) mark any claim about body-level behaviour as `[INFER]`; (c) declare the mangling explicitly in the block header caveat (`"decompiles MANGLED (\`abstract ln ln\`) — body-level mechanics are \`[INFER]\`"`). This is distinct from the §5 obfuscated-bytecode rule (ProGuard/R8/DexGuard — intentional, all names mangled, file names also renamed); here ONLY the in-body class-name token is garbled while the file name and structural fingerprint (parent type, package path) are trustworthy. Add as a THIRD case in §5 after the obfuscated-bytecode block. | `METHODOLOGY.md §5` (add a named "Vineflower partial-mangle" block after the obfuscated-bytecode section) | B427 (`AwtShellManager` → `abstract ln ln`), B429 (LinkCommand token `ln`, cited by existence from states/), B435 (password FE token `n`, parent `BWbFieldEditor` real), B436 (`BDaemonCnxHandler` port `n`, `BPlatformConnectionOptions` absent — honesty downgrade), B437 (config FEs token, parent real), B438 (class token `l`, extends clause real) — pattern recurred in **6 of 12** blocks, improvised correctly each time but the convention was never named | new | HIGH |
| 2 | **CLASS-NAME-BEARING gap names: existence pre-check before backlog sealing** — when the audit-first sweep names a gap using a SPECIFIC CLASS NAME as the focal point (e.g., "BWManager framework", "BAbstractDiscovery pattern"), run `fd <ClassName>.java` over the decompiled corpus BEFORE sealing that name in the backlog. A gap named after a class that does not exist will be refuted on first investigation, wasting the block's opening on a premise correction instead of a finding. Specialisation of the existing "GAP PREMISES ARE HYPOTHESES" rule in BOOTSTRAP §e — add the concrete mechanism: class-name-carrying gap names get a pre-backlog existence check. | `PROMPT-LOOP.md BOOTSTRAP §e` (extend the "GAP PREMISES ARE HYPOTHESES" paragraph with this specific pre-check) | B431: `BWManager` does not exist (base is `BAbstractManager`) and `BCellTable` ≠ manager table (premise conflation); B437: no `BAbstractDiscovery` (base is `MgrLearn`); B434: devkit-wb is not an SDK (nature misconception — harder to pre-check, but the pattern is the same); **4 refuted premises in 12 gaps = 33%**, unusually high for an audit-first backlog; all four had concrete class/nature claims the sweep asserted without verifying against the decompiled corpus | new | MEDIUM |
| 3 | **"Do NOT nest sub-agents" as a STANDING instruction in delegation prompts** — the kit's current "nesting caveat" (PROMPT-LOOP.md §3 DELEGATE) is framed as a passive preference ("prefer ONE level"). This run showed the cost: a sweep agent nested a sub-agent, context was cut off, and only the tail (WB02 data) was returned; the driver had to use `SendMessage` to recover the full consolidated Q1-Q5 findings. The fix the driver applied ad-hoc — adding "Do NOT nest sub-agents" to subsequent sweep delegation prompts — should become a STANDING INSTRUCTION included in every delegation prompt template by default. The existing capability ("MAY spawn a sub-sweep for a punctual need") remains, but the DEFAULT is explicit prohibition unless justified. | `PROMPT-LOOP.md §3 DELEGATE` (append to the delegation guidance: "Include in every sweep delegation prompt: 'Do NOT nest sub-agents. Return ONLY cited findings to the driver.'") | Iteration 2 (WB02, B428): "Sweep anidó sub-agente→pedí Q1-Q5 consolidados vía SendMessage; drift de línea WbSys 111→88 corregido en verify" — driver recovered, but the round-trip to `SendMessage` cost context and introduced a line-drift that self-verify had to catch | new | LOW |

For each delta, one line of rationale:

- **#1** — The §5 obfuscated-bytecode rule covers intentional APK/DEX renaming. Vineflower's partial-mangle is UNINTENTIONAL and structurally different: the file name + parent are reliable identity anchors that the existing rule does not name as safe-to-cite. Without a named convention, a future driver facing `ln` tokens must re-improvise the same resolution every time; with it, the block header caveat + `[INFER]` discipline is a one-line lookup.

- **#2** — The existing "gap premises are hypotheses" reminder is correct but passive. A 33% refutation rate (4/12) in a single focus signals the audit sweep was seeding class-specific names without existence-checking. A `fd <ClassName>.java` takes seconds; catching a non-existent class before the backlog is sealed avoids spending the first investigation turn on a §14 correction.

- **#3** — The current wording ("prefer ONE level") leaves the sweep agent to judge whether this is a "punctual, well-scoped" nesting need. That judgment failed once; making the prohibition explicit and DEFAULT in the delegation boilerplate removes the per-call judgment, which is where the cost was incurred.

## Already covered (dedupe — proof the retro read the kit first)

- **"GAP PREMISES ARE HYPOTHESES"** — already in `PROMPT-LOOP.md BOOTSTRAP §e`. Delta #2 is a SPECIALISATION (adds a concrete class-existence pre-check mechanism), not a duplicate. Holding.

- **Nesting caveat ("prefer ONE level")** — already in `PROMPT-LOOP.md §3 DELEGATE` under MODEL TIER. Delta #3 is an ACTION UPGRADE (from preference to standing boilerplate instruction), not a duplicate. Holding.

- **Obfuscated-bytecode rule ("never cite a renamed symbol")** — already in `METHODOLOGY.md §5`. Delta #1 is a THIRD CASE covering a structurally different failure mode (decompiler artifact vs intentional obfuscation), not a duplicate. Holding.

- **PRE-STOP ARTIFACT AUDIT** (2026-08-10 platform-native retro, delta #1) — not applicable to this run (workbench hit genuine investigable=0; no uncited dump files left in `tools/`). Not re-proposed.

- **Twin-binary offset provenance** (2026-08-10 platform-native retro, delta #2) — native RE concern, no native binaries in the workbench focus. Not re-proposed.

- **§14 premise corrections** — the RESEARCH-STATE stop-control already records all four corrections ("correcciones de premisa: BWManager no existe, devkit no es SDK, celltable ≠ tabla del manager, no hay BAbstractDiscovery"). Delta #2 proposes a PRE-RUN prevention; the §14 correction mechanism itself needs no change.

## Run statistics

- Blocks written: 12 (B427-B438)
- Commits: `9bb0a8d` (bootstrap) + `de66adc`→`514c437` (12 blocks + 1 mirror refresh)
- Gaps closed: 12/12 (WB01-WB12)
- Premise corrections (§14): 4 (BWManager, devkit≠SDK, celltable≠manager-table, BAbstractDiscovery)
- Blocks with partial-mangle: 6 of 12 (B427, B429, B435, B436, B437, B438)
- Model tier: `sonnet` for 11 of 12 delegated sweeps; `inline` for WB12 (bucket census — deliberate)
- Coverage: 12/12 — focus STOPPED
- Proposed deltas: 3 (1 HIGH, 1 MEDIUM, 1 LOW)
