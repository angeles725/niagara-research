# B710 — Module best practices, exemplar catalog + improvement roadmap (MBP6, focus close): what to copy from each reference module, and the ranked fixes for the shop's fleet

> Focus: **module-best-practices** · Gap **MBP6** (exemplar catalog + improvements + guide finalization) —
> FOCUS-CLOSING block. Block TYPE = **SYNTHESIS** (high [INFER] ratio expected). Consolidates [Block 705]–
> [Block 709] + the audited fleet. Finalizes `docs/module-best-practices.md`.

## 710.1 — Reference-exemplar catalog (learn from each)

[CERT+INFER]

| Module | Role | Copy this | Watch out |
|---|---|---|---|
| **`httpClientGAngeles`** | the clean EXEMPLAR ([Block 640]/[Block 647]) | the ONLY shop module without the empty-permissions scaffold; minimal, focused, correct vendor/signing | — |
| **`chihuahua`** (rt/ux/wb) | the production REFERENCE ([Block 648]–[Block 655]) | server-authoritative RBAC write-gate (`ChiRbacHelper`), per-ORD locking + off-thread protection, pure-Java wb `model/`, Fox-sub+REST live data, real version history 1.0→1.3, wb-added-last | the fault→0.0 protection-path defect (fix pending); the `AWAITING SLOTOMATIC REGEN` on `BChiUp` |
| **`control`** (Tridium core) | the slot/lifecycle CANON ([Block 631]/[Block 650]) | `BNumericWritable` slot flags + typed defaults; the `@NiagaraType` pipeline | — |
| **`sdash`** | dashboard, over-shaded ([Block 644]) | per-agent `requiredPermissions="r"` granularity | 2186 classes, 96% bundled Jackson/Commons — the uber-jar anti-pattern |
| **`mcpbridge`** | AI/MCP bridge ([Block 643]) | (the servlet auth gate is correct) | userless static dispatch = RBAC bypass; bundles its own Gson |
| **`datacenter-ux`** | dashboard ([Block 645]) | — | hardcoded rack/location data in the jar; bundles Gson |
| **`interfaz1-wb`** | empty scaffold ([Block 642]) | — | 0 classes, still costs boot load + verify — delete |
| **`nmodsreflow`** (OEM) | Vue SPA reference ([Block 151]) | the thin `BReflow` shim | `@AgentOn requiredPermissions` used as security (REST endpoint ungated) |

## 710.2 — The improvement roadmap (ranked, fleet-wide)

[INFER, consolidated from MBP1-5]

**Safety (do first):**
1. **chihuahua fault-status fix** — extend `readNumericNullable`→null from the display path into
   `applyProtections()`; a faulted amp sensor currently disables overload protection ([Block 651]/[Block 655]).

**Security:**
2. **mcpbridge per-user RBAC** — pass the authenticated `BUser` into `ToolDispatcher`, run each op via
   `runAsUser` so `OrdTarget.canWrite()` applies; add a least-privilege MCP role + tool-call audit; do this
   BEFORE it reaches any client station ([Block 643]).
3. **Sign strictly** — modules signed with `angelessignerCA`, and raise the station off
   `moduleVerificationMode=low` / `program.requireSigning=off` where policy allows ([Block 398]/[Block 639]).

**Build hygiene:**
4. **Slotomatic before release** on chihuahua; automate mode A/B by `git diff` on `@Niagara*` ([Block 637]/[Block 650]).
5. **Version discipline** — bump `vendorVersion` per release; 12/13 modules are frozen at 1.0 ([Block 640]).

**Packaging / structure:**
6. **Extract shared libs** (`gson-rt`/`jackson-rt`) instead of per-module shading ([Block 643]/[Block 644]/[Block 645]).
7. **Drop `interfaz1-wb`** (empty jar) ([Block 642]).
8. **Delete empty `<permissions>` scaffolds**; scope permissions at `@AgentOn` ([Block 635]/[Block 644]).
9. **Move hardcoded `-ux` site data into `-rt`** (datacenter) ([Block 645]).

**Defaults:**
10. Standard template = **rt + ux only**, add `-wb` deliberately; `module.palette` for component modules;
    Fox-sub+REST live data; no `<permissions>` block ([Block 647]).

## 710.3 — The deliverable

[CERT] `docs/module-best-practices.md` is finalized with six sections: rt (§1), ux (§2), wb (§3), cross-cutting
(§4), build (§5), and this exemplar catalog + roadmap (§6). Every rule cites a research block. It is the
actionable answer to "the most optimized, error-free way to program N4 modules — learn from the references,
and what to improve."

## 710.4 — Focus verdict

[INFER] The shop's modules are **structurally sound** (the skeleton, build, and signing are correct — [Block 636]/
[Block 639]) with **one production reference done well** (chihuahua's RBAC/threading/wb patterns) and a small set
of **fixable deviations** — the majority cosmetic (empty scaffolds, frozen versions, uber-jars), two that matter
(the chihuahua fault-path safety defect and the mcpbridge RBAC bypass). None require rearchitecting; all are
covered by the guide. The single highest-value action is the chihuahua fault-status fix (safety), then the
mcpbridge RBAC fix (security).

## Connections

- Consolidates MBP1-5 [Block 705]–[Block 709]. Exemplars/deviations from `own-modules-audit` [Block 637]–[Block 647],
  `chihuahua-source` [Block 648]–[Block 655], `module-anatomy` [Block 629]–[Block 636]. Deliverable:
  `docs/module-best-practices.md`.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | exemplar catalog (httpClientGAngeles clean; chihuahua reference; sdash/mcpbridge/etc deviations) | [CERT] | [Block 640]/[Block 648]/[Block 643]/[Block 644] | cited |
| 2 | ranked roadmap: safety→security→build→packaging | [INFER] | 710.2 | reasoned |
| 3 | top action = chihuahua fault fix, then mcpbridge RBAC | [INFER]+[CERT] | [Block 651]/[Block 643] | reasoned |
| 4 | guide finalized (6 sections, every rule cited) | [CERT] | docs/module-best-practices.md | delivered |

**Tally:** [CERT] ×2 · [INFER] ×2. Block TYPE = **SYNTHESIS** — ratio expected-high. Re-cites verified blocks.

## Focus status

**MBP6 CLOSED → module-best-practices investigable = 0 → focus STOP.** 6/6 gaps closed (MBP1–MBP6). Deliverable
`docs/module-best-practices.md` complete. No requires-execution, no blocked gaps. Next: §18 retro + push, then
focus B (`module-dev-workflow`).
