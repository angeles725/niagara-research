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
  engine+compiler+simulator), **B115 (spyderToIrmNxMigrator — NEW this iteration)**.
- **Coverage metric (this focus)**: **1 / 8** Spyder-ecosystem gaps closed this loop session (gap G1).
  (7 remain open; see backlog.)
- **Last iteration**: 2026-06-28 — closed G1 (spyderToIrmNxMigrator, Spyder→IRM Nx transpiler) → B115.

## Gap-backlog (prioritized)

| Pri | ID | Gap | Artifact / source | Bucket | Status |
|---|---|---|---|---|---|
| — | G1 | `spyderToIrmNxMigrator` — Spyder XL10 → IRM Nx application transpiler | Java `organized/spyderToIrmNxMigrator/` | read-only | **COVERED → B115** |
| high | G2 | `docHoneywellSpyder` — bundled Spyder docs/help (operator + FB reference; may yield `[CERT-doc]` for the FB catalog) | doc/HTML/PDF `organized/docHoneywellSpyder/` | read-only | pending |
| high | G3 | XL10NextGen **complete FB catalog** — per-block I/O, params, semantics (B106 pending #1; B115 enumerated the 45 types but not each block's pins/algorithm) | Java `organized/honeywellSpyderTool/.../functionalBlocks/blocks/` | read-only | partial (taxonomy done via B115; per-block detail open) |
| high | G4 | `honeywellSpyderTool` **detailed I/O layer** (`io/`, ~87 cls: terminal assignment, linearization, sensor types) — B106 pending #2 | Java `organized/honeywellSpyderTool/.../io/` | read-only | pending |
| medium | G5 | `honeywellSpyderTool` **full UI/wizard layer** (`functionalBlocks/ui`, `kingfisher/ui`, wizardgen, ~250 cls) — B106 pending #3 | Java (wb UI) | read-only | pending |
| medium | G6 | Spyder **driver deep-dive vs B77** — download/upload protocol wire detail, BOAC, file writers, retry/restore version gating (B77 left `[CERT-a]`/`[INFER]`) | Java `honeywellBacnetSpyder`/`honeywellLonSpyder` | read-only | pending |
| medium | G7 | **Kingfisher / TR wall-module tool-side** deep-dive (`kingfisher/tr4x` ~101 cls, `sylk/fw` ~39, BKFStateMachine LCD sim) — B106 touched, not distilled | Java `honeywellSpyderTool/.../kingfisher`, `sylk/fw` | read-only | pending |
| low | G8 | Spyder→IRM migration **round-trip fidelity** — verify a migrated app behaves equivalently on a real IRM/BEATS (the lossy mappings of B115 §115.3/§115.7) | requires migrated app + IRM controller | requires-execution / blocked | deferred |

## Iteration history

| # | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| it.1 | 2026-06-28 | G1 — spyderToIrmNxMigrator (Spyder→IRM Nx transpiler) | B115 | G8 (round-trip fidelity, requires-execution); sharpened G3/G4/G5 from B106 pendings |

## Blocked / non-read-only gaps (tagged with what they need)

- **G8** — needs: a migrated `.bog` loaded onto a **live IRM/BEATS controller** to confirm the lossy FB
  reconstructions (BAggregation, BPsychrometric, BIrmSubFolder decompositions) are behaviorally
  equivalent. → DYNAMIC phase (METHODOLOGY §12) when hardware appears. `requires-execution`.

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: **6** (G2, G3, G4, G5, G6, G7)  ← STATIC loop stops when this hits 0
- **Open gaps — requires-execution**: **1** (G8)
- **Open gaps — blocked (live system/hardware)**: **0**
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap (default safety net): none
- **Total Spyder-ecosystem gaps remaining**: **7** (6 read-only investigable + 1 requires-execution)
