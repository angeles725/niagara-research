# RESEARCH-STATE — focus: module-best-practices (an actionable rt/ux/wb best-practices guide distilled from the reference modules — what to copy, what to avoid, what to improve)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-30** (operator "A" pick: optimized,
> error-free module programming; good structure/logic; learn from reference examples; what to improve).
> DESIGN/APPLIED corpus — synthesizes EXISTING evidence blocks into an actionable guide + improvement
> recommendations; produces a deliverable `docs/module-best-practices.md`. A high [INFER] ratio is EXPECTED
> (synthesis, not new extraction).
>
> **Ángulo:** NOT re-deriving the module skeleton (already in `module-anatomy`), but distilling the reference
> modules — Tridium core + the `httpClientGAngeles` exemplar + the production `chihuahua` — into per-layer
> best practices (rt/ux/wb), named anti-patterns, and concrete improvement recommendations. Read-only over the
> corpus + the real module jars in `organized/`.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 705
gaps_closed: 5
known_gaps: 6
investigable_open: 1
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

focus: module-best-practices
status: active (bootstrapped 2026-08-30; backlog seeded from the closed module focuses)
bootstrapped_on: 2026-08-30
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B705)

## Coverage

- **Covered blocks**: 700 corpus-wide (this focus: B705-) (shared-global)
- **Coverage metric**: 5 / 6 gaps closed (MBP1-5)
- **Deliverable**: `docs/module-best-practices.md` (the human-readable guide, built as gaps close)

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | MBP1 rt-layer best practices — BComponent structure, @NiagaraProperty/Action/Topic slots, lifecycle, threading, type-registration; clean-rt patterns + anti-patterns | synthesis+code | closed (B705 — 8 patterns/7 anti-patterns/5 fixes; 2 code cites spot-checked; docs/module-best-practices.md rt section) |
| high | MBP2 ux-layer best practices — bajaux, BSingleton+@AgentOn, the JS/web layer, module-side registration, front-end structure; patterns + anti-patterns | synthesis+code | closed (B706 — thin-shim+JS, requiredPermissions=visibility-not-security, server-RBAC, ES5-strict, Fox-sub+REST; guide §2) |
| high | MBP3 wb-layer best practices — Workbench Swing (managers/views/field editors), when wb is actually needed vs over-built | synthesis+code | closed (B707 — when-needed decision rule; Manager/View/FieldEditor patterns; chihuahua-wb exemplar; wb-invisible-to-daemon; guide §3) |
| medium | MBP4 cross-cutting — RBAC write-gate pattern (chihuahua), permissions/over-permission anti-pattern (own-modules-audit), audit, error handling | synthesis+code | closed (B708 — permission model BPermissions+BCategoryService; audit framework+module; engine-thread error handling; signing; guide §4) |
| medium | MBP5 build/packaging best practices — module.xml/module-include.xml, dependencies, signing, version-targeting; the optimal error-free build loop | synthesis+code | closed (B709 — gradle-niagara build, Slotomatic mode rule, convention signing angelessignerCA, version-targeting by SDK path, deploy loop; guide §5) |
| low | MBP6 reference-exemplar catalog + improvement recommendations + the deliverable guide (docs/module-best-practices.md) | synthesis+deliverable | pending |

`tried:` (none blocked — all source is existing corpus blocks + real jars in organized/; SOURCE-BEFORE-AGENT passes).

## Remittance (the FACTS these best practices are distilled FROM — cited, not re-derived)

- Module SKELETON / build / type-registration / classloader / manifest / palette / permissions → focus `module-anatomy` [Block 629]–[Block 636].
- Operator's OWN modules audit + build process (Clean+Slotomatic+Build, version-targeting, signing) + `httpClientGAngeles`=exemplar + systemic over-permission → focus `own-modules-audit` [Block 637]–[Block 647].
- `chihuahua` source-level audit (rt/ux/wb, RBAC write-gate, ES5 front-end, production-readiness) → focus `chihuahua-source` [Block 648]–[Block 655] + `chihuahua` [Block 163]–[Block 177].
- Workbench framework (managers/views/wire sheet/property sheet/field editors) → focus `workbench` [Block 427]–[Block 432].
- bajaux / ux web layer (webEditors, @AgentOn delegation) → [Block 421] + focus `px-editor-core`.
- Reflow OEM module (backend/ux) as a comparison point → focus `nmodsreflow` [Block 138]–[Block 155].

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-08-30 | (bootstrap — from the closed module focuses) | — | no · inline | MBP1–MBP6 seeded |
| 1 | 2026-08-30 | MBP1 rt best practices | B705 | yes · sonnet (synthesis of module blocks + jars) + inline spot-check | 0 new |
| 2 | 2026-08-30 | MBP2 ux best practices | B706 | yes · sonnet (synthesis) | 0 new |
| 3 | 2026-08-30 | MBP3 wb best practices | B707 | yes · sonnet (synthesis) | 0 new |
| 4 | 2026-08-30 | MBP4 cross-cutting | B708 | no · inline (consolidation + targeted read) | 0 new |
| 5 | 2026-08-30 | MBP5 build/packaging | B709 | no · inline (OMB1-3 targeted read) | 0 new |

## Blocked gaps (each tagged with what it needs)

(none — all synthesis over existing corpus + jars.)

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 1
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- Budget cap: none

## Dismissed file types

- (to be filled by the coverage pass.)
