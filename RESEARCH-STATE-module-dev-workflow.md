# RESEARCH-STATE — focus: module-dev-workflow (the end-to-end N4 module dev process + tool mechanics, as a runbook)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-30** (operator "B" pick: how to call the
> tools, how they are used, what the process is — the optimal error-free dev loop). DOCUMENT/RUNBOOK focus —
> reorganizes the build facts as a STEP-BY-STEP process + tool mechanics; deliverable `docs/module-dev-workflow.md`.
> High [INFER] ratio expected (synthesis/runbook).
>
> **Ángulo:** distinct from `module-best-practices` (which gives RULES): this gives the PROCESS and the TOOL
> MECHANICS — what `@NiagaraType`/Slotomatic/gradle-niagara/niagara-signing actually DO, and the edit→build→
> sign→deploy→test→debug loop as a runbook with commands.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 707
gaps_closed: 1
known_gaps: 5
investigable_open: 4
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

focus: module-dev-workflow
status: active (bootstrapped 2026-08-30)
bootstrapped_on: 2026-08-30
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B711)

## Coverage

- **Covered blocks**: 706 corpus-wide (this focus: B711-) (shared-global)
- **Coverage metric**: 1 / 5 gaps closed (WF1 B711)
- **Deliverable**: `docs/module-dev-workflow.md` (a step-by-step runbook)

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | WF1 the toolchain inventory + what each tool DOES (gradle-niagara plugins, Slotomatic, niagara-signing, the SDK/user-home layout, niagara-tools/ng-deploy.sh) | synthesis+runbook | closed (B711 — two homes, gradle-niagara plugins, Slotomatic=codegen not JSR-269, ng-deploy.sh, wizards; tool->job map; runbook §1) |
| high | WF2 the codegen mechanics — @NiagaraType/@NiagaraProperty → Slotomatic → the AUTO region round-trip (how it works, inputs/outputs) | synthesis+code | pending |
| medium | WF3 the authoring artifacts — module-include.xml, module.palette, module.xml manifest: how to author each, what goes where | synthesis+code | pending |
| high | WF4 the end-to-end dev loop as a runbook — edit → slotomatic(if needed) → build → sign → deploy → verify → test/debug, with commands | runbook | pending |
| medium | WF5 testing + debugging — run-tests-wsl.sh (pure-Java model testing), station debug, common errors + fixes; + the runbook deliverable docs/module-dev-workflow.md | synthesis+deliverable | pending |

`tried:` (none blocked — all source is existing corpus + the real build repo referenced by own-modules-audit).

## Remittance (cited, not re-derived)

- Build/packaging RULES + signing + version-targeting → focus `module-best-practices` [Block 709] (MBP5).
- Build process detail (Clean+Slotomatic+Build, ng-deploy.sh modes, angelessignerCA, WSL bridge) → focus `own-modules-audit` [Block 637]–[Block 639].
- Type-registration pipeline (@NiagaraType→Registry) + manifest reader + module.palette + classloader → focus `module-anatomy` [Block 630]–[Block 636].
- Signing crypto/trust anchors → focus `signing-pki` [Block 392].

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-08-30 | (bootstrap) | — | no · inline | WF1–WF5 seeded |
| 1 | 2026-08-30 | WF1 toolchain inventory | B711 | no · inline (B631/B637-639 in-hand) | 0 new |

## Blocked gaps (each tagged with what it needs)

(none.)

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 4
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- Budget cap: none

## Dismissed file types

- (to be filled by the coverage pass.)
