# niagara-research — Research State (Honeywell Spyder ECOSYSTEM focus)

> Operational state consumed by the Research-SDD loop. Mirrored in engram
> (`research/niagara/spyder-gaps`, `research/niagara/spyder-progress`). Visible, versionable source.
>
> **Scope of this state file**: the **Honeywell Spyder ecosystem** focus area (user-chosen). The broader
> niagara corpus (B1–B114) is mature and tracked in `INDEX.md`/`CATALOG.md`; this file tracks ONLY the
> Spyder-ecosystem gap-backlog for the loop. Corpus language for NEW blocks = **English** (the legacy
> niagara blocks are Spanish; new Spyder-ecosystem blocks switch to English, noted at the top of each).
>
> **Bootstrap note (iteration it.1, 2026-06-28)**: the launch prompt assumed the next free block number
> was B110; that was stale — B110–B114 already exist (honRemoteConfig, honEagleHawkHMI, security
> detection, module-signing hardening, BOG encryption). The next free number is **B115**, which this
> iteration wrote.

## Coverage

- **Covered blocks (Spyder ecosystem)**: B77 (Spyder BACnet+LON drivers), B96 (Venom TAB),
  B100 (ipcMigrator Spyder→IPC), B101 (airFlowBalancer/kitCat), B106 (honeywellSpyderTool/XL10NextGen
  engine+compiler+simulator), B115 (spyderToIrmNxMigrator), **B116 (docHoneywellSpyder — bundled
  SpyderTool help: official operator + per-block FB I/O reference — NEW this iteration)**.
- **Coverage metric (this focus)**: **2 / 8** Spyder-ecosystem gaps closed this loop session (G1, G2).
  (6 remain open; see backlog. Note: G3 substantially advanced by B116's `[CERT-doc]` per-block I/O,
  but kept open for the code-side `functionalBlocks/blocks/` algorithm verification.)
- **Last iteration**: 2026-06-28 — closed G2 (docHoneywellSpyder, the bundled JavaHelp doc module —
  yields `[CERT-doc]` per-block FB I/O + the controller I/O model) → B116.

## Gap-backlog (prioritized)

| Pri | ID | Gap | Artifact / source | Bucket | Status |
|---|---|---|---|---|---|
| — | G1 | `spyderToIrmNxMigrator` — Spyder XL10 → IRM Nx application transpiler | Java `organized/spyderToIrmNxMigrator/` | read-only | **COVERED → B115** |
| — | G2 | `docHoneywellSpyder` — bundled Spyder docs/help (operator + FB reference; yields `[CERT-doc]` for the FB catalog) | doc/HTML `organized/docHoneywellSpyder/` | read-only | **COVERED → B116** |
| high | G3 | XL10NextGen **complete FB catalog** — per-block I/O, params, semantics (B106 pending #1; B115 enumerated the 45 types; **B116 now documents pins/ranges/algorithm per block from the VENDOR `[CERT-doc]`**) — remaining: cross-verify the `[CERT-doc]` semantics against the actual decompiled FB algorithm code | Java `organized/honeywellSpyderTool/.../functionalBlocks/blocks/` | read-only | partial (taxonomy via B115; per-block vendor I/O via B116; code-side algorithm verification open) |
| high | G4 | `honeywellSpyderTool` **detailed I/O layer** (`io/`, ~87 cls: terminal assignment, linearization, sensor types) — B106 pending #2 | Java `organized/honeywellSpyderTool/.../io/` | read-only | pending |
| medium | G5 | `honeywellSpyderTool` **full UI/wizard layer** (`functionalBlocks/ui`, `kingfisher/ui`, wizardgen, ~250 cls) — B106 pending #3 | Java (wb UI) | read-only | pending |
| medium | G6 | Spyder **driver deep-dive vs B77** — download/upload protocol wire detail, BOAC, file writers, retry/restore version gating (B77 left `[CERT-a]`/`[INFER]`) | Java `honeywellBacnetSpyder`/`honeywellLonSpyder` | read-only | pending |
| medium | G7 | **Kingfisher / TR wall-module tool-side** deep-dive (`kingfisher/tr4x` ~101 cls, `sylk/fw` ~39, BKFStateMachine LCD sim) — B106 touched, not distilled | Java `honeywellSpyderTool/.../kingfisher`, `sylk/fw` | read-only | pending |
| low | G8 | Spyder→IRM migration **round-trip fidelity** — verify a migrated app behaves equivalently on a real IRM/BEATS (the lossy mappings of B115 §115.3/§115.7) | requires migrated app + IRM controller | requires-execution / blocked | deferred |

## Iteration history

| # | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| it.1 | 2026-06-28 | G1 — spyderToIrmNxMigrator (Spyder→IRM Nx transpiler) | B115 | G8 (round-trip fidelity, requires-execution); sharpened G3/G4/G5 from B106 pendings |
| it.2 | 2026-06-28 | G2 — docHoneywellSpyder (bundled JavaHelp doc module; `[CERT-doc]` per-block FB I/O + controller I/O model) | B116 | none net-new; G3 advanced (per-block vendor I/O now documented, code-side algorithm verification remains); G4 fed (`[CERT-doc]` physical-points/model-gating); escalated B77 XIF `[INFER]`→`[CERT-doc]` |

## Blocked / non-read-only gaps (tagged with what they need)

- **G8** — needs: a migrated `.bog` loaded onto a **live IRM/BEATS controller** to confirm the lossy FB
  reconstructions (BAggregation, BPsychrometric, BIrmSubFolder decompositions) are behaviorally
  equivalent. → DYNAMIC phase (METHODOLOGY §12) when hardware appears. `requires-execution`.

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: **5** (G3, G4, G5, G6, G7)  ← STATIC loop stops when this hits 0
- **Open gaps — requires-execution**: **1** (G8)
- **Open gaps — blocked (live system/hardware)**: **0**
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap (default safety net): none
- **Total Spyder-ecosystem gaps remaining**: **6** (5 read-only investigable + 1 requires-execution)
- **Next recommended gap**: **G3** (now best attacked code-side: verify the `[CERT-doc]` per-block
  semantics from B116 against the decompiled `functionalBlocks/blocks/` algorithms) — or **G4** (I/O layer).
