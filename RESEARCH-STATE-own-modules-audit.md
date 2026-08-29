# own-modules-audit — Research State

> Operational state consumed by the loop (Research-SDD). Mirrored in engram
> (`research/niagara/own-modules-audit/gaps`, `.../progress`). Visible and versionable source.
>
> **Focus angle (§16 / §b2).** Apply the module-anatomy REFERENCE SKELETON + rubric ([B636]/[B629]–[B635])
> to the OPERATOR'S OWN modules on the live install (`vendor=ANGELES`/`Angeles`/`SEJOFA` — the operator's
> shop; SEJOFA is the live signing identity, [B392]). Direct operator request: "el módulo chihuahua fue
> creado por nosotros… puedes ver los demás módulos." One module (or small group) per block: manifest +
> jar taxonomy vs the reference → deviations ranked → concrete fixes. DIRECT-ARTIFACT first (unzip the real
> signed jars — module-anatomy retro delta #1), decompile only where a class body is load-bearing.
> READ-ONLY. `nmodsreflow` (SEJOFA) = REMIT ([B138]–[B155], the Reflow OEM focus). chihuahua already audited
> ([B636] MA8).
>
> REAL SOURCE available (operator-pointed 2026-08-29): `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/` (root+part gradle, BUILD_WORKFLOW.md, ps1/sh, gradle.properties) + `/home/cristian/modulos_niagara_n4/niagara-tools/`. Operator corrections folded in: (a) version-targeting is DELIBERATE via niagara_home SDK PATH (compile 4.13 SDK → deploy 4.14 station) — reframes [B636] dev#2; (b) active station-signing identity = ANGELES; (c) build variants Clean+Slotomatic+Build vs Clean+Build; (d) programming TESTS reportedly useless/build-hindering (VERIFY). OMB1-3 = real-source build gaps (operator priority); OMA1-8 = per-module jar audit.
>
> Backlog from a DIRECT-ARTIFACT manifest scan + real-source pointer 2026-08-29. Next block B637.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 632
gaps_closed: 0
known_gaps: 11
investigable_open: 11
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

## Coverage

- **Covered blocks**: 0 in this focus (corpus-wide count synced by the tool; global prefix `niagara-mental-model-bloque`)
- **Coverage metric**: 0 / 11 closed
- **Last iteration**: 2026-08-29 — bootstrap (direct-artifact manifest scan + backlog seeded)

## Remittances (already covered — cite, do NOT re-derive)

- **Reference skeleton + rubric** — [B636] (MA8) + [B629]–[B635] (module-anatomy focus). Every OMA block grades against these.
- **chihuahua** — [B636] §Part B (already audited: over-permissioned, builds vs 4.13, profile drift, no palette, heavy ux). Not re-audited; the template for the others.
- **nmodsreflow** (SEJOFA) — [B138]–[B155] (Reflow OEM `-rt`/`-ux` deep focus). REMIT entirely.
- **Live signing identity SEJOFA** — [B392] (the real cert is SEJOFA, not "Angeles"). Signer-alias questions REMIT to signing-pki.
- **Live security posture** (`moduleVerificationMode=low`, `smDeveloperMode`) — [B398]/[B18]/[B635]. Explains why weakly-signed/over-permissioned modules load.

## Gap-backlog

<!-- Priority: high | medium | low | deferred. Status leading token: pending | requires-execution |
     blocked-on-<reason> | ✅ | ~~. Sources = real jars in the live install modules/ dir. -->

| Priority | Gap | Artifact type / source | Status |
|---|---|---|---|
| high | OMB1 — the REAL build workflow + its VARIANTS (Clean+Slotomatic+Build vs Clean+Build): the gradle task sequences, WHEN Slotomatic is needed vs skippable, build-and-deploy.ps1/deploy.sh/inspect-*.ps1 roles, and the TESTS question (run-tests-wsl.sh + niagaraTest — did tests gate/slow the build? verify, don't assume) | real source · modulos_niagara_n4/.../chihuahua/{BUILD_WORKFLOW.md,build.gradle.kts,*.ps1,deploy.sh,run-tests-wsl.sh} | pending |
| high | OMB2 — VERSION-TARGETING via compilation PATHS: how niagara_home/niagara_user_home in gradle.properties + settings.gradle.kts resolution chain point the build at a specific Niagara SDK (iSMA 4.13.2 vs 4.14 vs 4.15); compile-vs-deploy version split (build on 4.13 SDK → deploy to 4.14 station); Java-8 constraint. Reframes [B636] dev#2 as deliberate | real source · gradle.properties + settings.gradle.kts + BUILD_WORKFLOW.md | pending |
| high | OMB3 — SIGNING (active identity = ANGELES) + niagara-tools: the niagaraSigning gradle config (alias/keystore, ANGELES vs SEJOFA_C), how the signer is selected, and what the niagara-tools repo (scripts/docs/openspec) provides. SECRETS DISCIPLINE (structure not values) | real source · gradle files + .env.local (keys only) + niagara-tools/ | pending |
| high | OMA1 — SYSTEMIC cross-module patterns: the deviations shared by ALL/most operator modules (universal `<niagara-permission-groups type=all>`; stuck at vendorVersion 1.0; single build host DESKTOP-4AAQ77H; signer split NIAGARA4 vs SEJOFA_C; near-empty ux/wb shells) — the headline block, graded vs [B636] | direct-artifact · all custom jars' module.xml | pending |
| high | OMA5 — `mcpbridge-rt` (206 classes, 1 type, rt-ONLY): what it is (an MCP bridge?), what it bundles, why 206 classes for 1 Baja type, and its architecture vs the reference | direct-artifact + decompile · mcpbridge-rt.jar | pending |
| high | OMA4 — `sdash-rt` (2186 classes(!), 12 types, signer SEJOFA_C, permGroups=4): what is bundled (uberjar library?), the signer difference, and the size/packaging implications | direct-artifact + decompile · sdash-rt.jar | pending |
| medium | OMA3 — `datacenter` (ux = 220 classes / 1 type): heavy Java in the BROWSER profile (chihuahua #5 taken to extreme) — what those 220 ux classes are and whether they belong in rt | direct-artifact + decompile · datacenter-ux.jar/-rt.jar | pending |
| medium | OMA6 — `httpClientGAngeles` (the "correct" one: permGroups=0, vendorVersion 4.14.0.162, 87 types, tri-profile): why it is the best-built, likely a fork/rebrand of Tridium `httpClient` — the positive exemplar | direct-artifact · httpClientGAngeles-{rt,ux,wb}.jar | pending |
| medium | OMA2 — the ANGELES-namespace modules (`angeles`, `demoangeles`, `interfaz1`): grade the operator's direct-namespace modules vs [B636]; interfaz1 has 0-type/0-class ux+wb shells | direct-artifact · angeles/demoangeles/interfaz1 jars | pending |
| low | OMA7 — the small SEJOFA dashboards (`electri`, `sanluis`, `sejofadashboard`, `tr3z`, `multivistaspersonalizados`, `dashboardups`): grouped grade vs [B636]; several have empty ux shells | direct-artifact · 6 jars | pending |
| high | OMA8 — SYNTHESIS: the operator's module-building signature (recurring good + bad patterns across OMA1-OMA7), a consolidated prioritized remediation plan, and a corrected reference build template for the shop | design synthesis over OMA1-OMA7 + [B636] | pending |

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| 0 | 2026-08-29 | (bootstrap) manifest scan + real-source pointer + operator corrections | — | no·inline (unzip scan + gradle.properties read) | 11 |

## Blocked gaps (each tagged with what it needs)

- none — all 8 gaps are read-only investigable (real jars on disk).

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 11   ← the STATIC loop STOPS when this hits 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none

## Dismissed file types

- none (focus reuses real install jars + existing decompiled corpus; no new census)
