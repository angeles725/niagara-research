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
> PRODUCTION SCOPE (operator 2026-08-29): ONLY `chihuahua` [B636] is in production on a maquila/client station; ALL other modules are dev/demo/experimental on the DEV supervisor DESKTOP-4AAQ77H. Non-chihuahua findings = code-quality/learning + 'fix before productionizing', not live risk.
>
> Backlog from a DIRECT-ARTIFACT manifest scan + real-source pointer 2026-08-29. Next block B637.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 652
gaps_closed: 8
known_gaps: 8
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

## Coverage

- **Covered blocks**: 0 in this focus (corpus-wide count synced by the tool; global prefix `niagara-mental-model-bloque`)
- **Coverage metric**: investigable 8/8 (OMB1-3,OMA1-7)+synthesis CLOSED; MCP-G2 requires-execution deferred
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
| high | OMB1 — the REAL build workflow + its VARIANTS (Clean+Slotomatic+Build vs Clean+Build): the gradle task sequences, WHEN Slotomatic is needed vs skippable, build-and-deploy.ps1/deploy.sh/inspect-*.ps1 roles, and the TESTS question (run-tests-wsl.sh + niagaraTest — did tests gate/slow the build? verify, don't assume) | real source · modulos_niagara_n4/.../chihuahua/{BUILD_WORKFLOW.md,build.gradle.kts,*.ps1,deploy.sh,run-tests-wsl.sh} | ✅ B637 — build modes A/B/C by changed profile (clean+jar, sign implicit no explicit sign/dist task); VARIANT rule: Clean+Slotomatic+Build iff @Niagara* annotation changed else Clean+Build (stale AUTO region if skipped when needed); TESTS VERIFIED: niagaraTest dead by plugin 7.6.17 moduleTestAnnotationProcessor bug (Total tests run:0) — operator right to drop station tests; BUT pure-JUnit type-(a) run-tests-wsl.sh 9 suites WORK, keep. Deploy=backup→copy to modules/ (ng-deploy.sh/build-and-deploy.ps1/deploy.sh --bump), bypasses install sig-gate |
| high | OMB2 — VERSION-TARGETING via compilation PATHS: how niagara_home/niagara_user_home in gradle.properties + settings.gradle.kts resolution chain point the build at a specific Niagara SDK (iSMA 4.13.2 vs 4.14 vs 4.15); compile-vs-deploy version split (build on 4.13 SDK → deploy to 4.14 station); Java-8 constraint. Reframes [B636] dev#2 as deliberate | real source · gradle.properties + settings.gradle.kts + BUILD_WORKFLOW.md | ✅ B638 — version-targeting = niagara_home SDK PATH (4-level resolution chain); deps resolve via flatDir from niagara_home/bin/ext+modules → compile against iSMA 4.13.2 SDK, deploy to Honeywell 4.14 station (deliberate split, §14 reframes B636 dev#2); switch version = repoint niagara_home + match gradlePluginVersion (7.3.40; no profile system); 7.6.17 test-bug hazard on newer SDK; Java-8 hard floor |
| high | OMB3 — SIGNING (active identity = ANGELES) + niagara-tools: the niagaraSigning gradle config (alias/keystore, ANGELES vs SEJOFA_C), how the signer is selected, and what the niagara-tools repo (scripts/docs/openspec) provides. SECRETS DISCIPLINE (structure not values) | real source · gradle files + .env.local (keys only) + niagara-tools/ | ✅ B639 — signing is convention (NO niagaraSigning{} block); niagara-signing plugin uses niagara_user_home/security/keystore.jceks (JCEKS); ACTIVE alias=angelessignerCA (ANGELES, validated C1+C2); SEJOFA=legacy 'falla en CI' (sdash SEJOFA_C); NIAGARA4=jar block-name (REMIT B392). niagara-tools v0.3.0=deploy wrapper ng-deploy.sh + KB (slotomatic/wsl/hot-reload/bql). Build Windows-side w/ Robocopy WSL bridge |
| high | OMA1 — SYSTEMIC cross-module patterns: the deviations shared by ALL/most operator modules (universal `<niagara-permission-groups type=all>`; stuck at vendorVersion 1.0; single build host DESKTOP-4AAQ77H; signer split NIAGARA4 vs SEJOFA_C; near-empty ux/wb shells) — the headline block, graded vs [B636] | direct-artifact · all custom jars' module.xml | ✅ B640 — 5 systemic patterns vs B636: (1 MED) universal over-permission permGroups=3 (all except httpClientGAngeles=0); (2 MED) frozen vendorVersion 1.0 (except httpClientGAngeles 4.14); (3 INFO) single build host DESKTOP-4AAQ77H + SEJOFA→ANGELES signer migration (sdash straggler SEJOFA_C); (4 LOW) empty 0/0 ux/wb shells (interfaz1/dashboardups/sejofadashboard/tr3z); (5) size anomalies sdash 2186/mcpbridge 206/datacenter-ux 220. Palette habit GOOD (chihuahua the outlier). httpClientGAngeles=corrected template |
| high | OMA5 — `mcpbridge-rt` (206 classes, 1 type, rt-ONLY): what it is (an MCP bridge?), what it bundles, why 206 classes for 1 Baja type, and its architecture vs the reference | direct-artifact + decompile · mcpbridge-rt.jar | ✅ B643 — MCP (Model Context Protocol) server the shop built (undocumented until now); BMcpServlet+ToolDispatcher+6 handlers (list/read/SET/CREATE/LINK) + 204 Gson. AUTH ok (401 if no getRemoteUser) but AUTHZ BYPASSED: dispatch is static/userless, SetPropertyHandler writes with NO canWrite/RBAC → any authed user gets full station write (RBAC B11/B558 bypassed). MCP-G1 CLOSED. LATENT+dev-only (not in prod, jar installed not mounted). MCP-G2 deferred (config.bog mount check) |
| high | OMA4 — `sdash-rt` (2186 classes(!), 12 types, signer SEJOFA_C, permGroups=4): what is bundled (uberjar library?), the signer difference, and the size/packaging implications | direct-artifact + decompile · sdash-rt.jar | ✅ B644 — most sophisticated dashboard (WebSocket BSdashWebSocketAcceptor, BSdashSyncService, own ORD scheme sdash:, 6 command agents w/ requiredPermissions=r — AHEAD of P1); 2186 classes=uber-jar 96% (Jackson 1042+commons-collections4 543+lang3 362+io 212+zjsonpatch 22, own 105); dual-signed NIAGARA4+SEJOFA_C (legacy straggler); REFLECTION req-perm (Jackson). Fix: factor shared Jackson, re-sign ANGELES. dev/demo |
| medium | OMA3 — `datacenter` (ux = 220 classes / 1 type): heavy Java in the BROWSER profile (chihuahua #5 taken to extreme) — what those 220 ux classes are and whether they belong in rt | direct-artifact + decompile · datacenter-ux.jar/-rt.jar | ✅ B645 — 220 classes = 99% bundled Gson (only 2 own BDtcrServlet+cache); REFUTES 'heavy ux=misplaced logic' (B640 P5/B636 #5) — real payload is rc/ 3D dashboard (Three.js+Chart.js bundled, floorplan/rack/map, HARDCODED racks-large.js/locations.js, stray CASINO asset x2). Profile ux CORRECT. Fixes: externalize data, share libs, prune CASINO. dev/demo |
| medium | OMA6 — `httpClientGAngeles` (the "correct" one: permGroups=0, vendorVersion 4.14.0.162, 87 types, tri-profile): why it is the best-built, likely a fork/rebrand of Tridium `httpClient` — the positive exemplar | direct-artifact · httpClientGAngeles-{rt,ux,wb}.jar | ✅ B641 — REFRAME: NOT own-built exemplar; it is Tridium's com.tridiumx.httpClient add-on repackaged (vendor SEJOFA, all classes com/tridiumx/httpClient/*) with SMA license gate 'neutralized' + re-signed. Clean manifest (0 perms, real vendorVersion 4.14) is INHERITED from Tridium, not authored. Licensing/compliance exposure (runs unlicensed, enabled by moduleVerificationMode=low). Copy the SHAPE not the provenance |
| medium | OMA2 — the ANGELES-namespace modules (`angeles`, `demoangeles`, `interfaz1`): grade the operator's direct-namespace modules vs [B636]; interfaz1 has 0-type/0-class ux+wb shells | direct-artifact · angeles/demoangeles/interfaz1 jars | ✅ B642 — clean equipment+Monitor model (angeles com.sejofa.angeles HVAC: Chiller/CoolingTower/DieselGenerator/…+Monitor twins; demoangeles=demo; interfaz1 com.angeles Dashboard+NotifierHoneywell); namespace split com.sejofa vs com.angeles under one ANGELES vendor; interfaz1-ux=pure-web (legit), interfaz1-wb=genuinely empty (drop); systemic deviations only |
| low | OMA7 — the small SEJOFA dashboards (`electri`, `sanluis`, `sejofadashboard`, `tr3z`, `multivistaspersonalizados`, `dashboardups`): grouped grade vs [B636]; several have empty ux shells | direct-artifact · 6 jars | ✅ B646 — ONE repeatable TEMPLATE cloned per site: BXxxDashboardService + domain Monitors (com.sejofa.<mod>.components) + pure-web -ux (9-26 rc/ assets, 0 Java, all six). sejofadashboard=richer base (BDashboardConfig+BSejoFaServlet). Same as chihuahua/angeles shape. Only systemic deviations (ver1.0, type=all) → fix TEMPLATE once + re-clone. No vendor forks, palettes shipped |
| deferred | MCP-G2 — is a BMcpServlet instance actually MOUNTED/reachable in any running station? (config.bog / live check) — determines whether the B643 authz-bypass is live vs latent | requires-execution · config.bog / live station | ✅ B656 (§12 LIVE) — /mcp is MOUNTED (405 GET; 404 other names); POST tools/list as API2 → HTTP 200 + tools[create_component,set_property,…]. Servlet dispatches mutating tool surface to ANY authed user (getRemoteUser-only, no RBAC per B643). Bypass LIVE-REACHABLE on dev supervisor (not maquila prod). Read-only probe, no mutating tool. Rec: unmount/disable or add runAsUser RBAC; rotate API2 |
| high | OMA8 — SYNTHESIS: the operator's module-building signature (recurring good + bad patterns across OMA1-OMA7), a consolidated prioritized remediation plan, and a corrected reference build template for the shop | design synthesis over OMA1-OMA7 + [B636] | ✅ B647 — shop signature (DashboardService+Monitor+pure-web-ux template, palettes, documented build; strengths+5 recurring template deviations) + corrected build knowledge (variants/version=SDK-path/signing angelessignerCA/tests dead by 7.6.17) + mcpbridge authz-bypass (latent/dev-only) + prioritized remediation (chihuahua first=prod) + FIXED reference build template. FOCUS CLOSED |

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| 0 | 2026-08-29 | (bootstrap) manifest scan + real-source pointer + operator corrections | — | no·inline (unzip scan + gradle.properties read) | 11 |
| 1 | 2026-08-29 | OMB1 real build workflow + variants + tests verdict | B637 | yes · sonnet (source map) + inline verify | 0 |
| 2 | 2026-08-29 | OMB2 version-targeting via niagara_home SDK path (§14 B636) | B638 | no·inline (source map + verify) | 0 |
| 3 | 2026-08-29 | OMB3 signing (angelessignerCA/ANGELES) + niagara-tools | B639 | no·inline (source + keystore struct + verify) | 0 |
| 4 | 2026-08-29 | OMA1 systemic cross-module patterns (5) | B640 | no·inline (manifest scan synthesis) | 0 |
| 5 | 2026-08-29 | OMA6 httpClientGAngeles = Tridium fork, SMA neutralized | B641 | no·inline (direct artifact + verify) | 0 |
| 6 | 2026-08-29 | OMA2 ANGELES-namespace modules (angeles/demoangeles/interfaz1) | B642 | no·inline (direct artifact + verify) | 0 |
| 7 | 2026-08-29 | OMA5 mcpbridge MCP server + authz-bypass (MCP-G1 closed) | B643 | no·inline (artifact+javap decompile) | MCP-G2 |
| 8 | 2026-08-29 | OMA4 sdash uber-jar (WebSocket/sync/scheme; Jackson+Commons) | B644 | yes·sonnet (anomaly sweep) + inline verify | 0 |
| 9 | 2026-08-29 | OMA3 datacenter-ux (Gson uber-jar, 3D rc/; 'heavy ux' refuted) | B645 | yes·sonnet (anomaly sweep) + inline verify | 0 |
| 10 | 2026-08-29 | OMA7 six small SEJOFA dashboards = one template | B646 | no·inline (manifest scan + verify) | 0 |
| 11 | 2026-08-29 | OMA8 SYNTHESIS + fixed template (FOCUS CLOSED) | B647 | no·inline (synthesis over B637-B646) | 0 |
| 12 | 2026-08-29 | MCP-G2 §12 LIVE — /mcp mounted, tool surface reachable by any authed user | B656 | no·inline (§12 live read-only probe) | 0 |

## Blocked gaps (each tagged with what it needs)

- none — all 8 gaps are read-only investigable (real jars on disk).

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 0   ← the STATIC loop STOPS when this hits 0
- **Open gaps — requires-execution**: 0 (MCP-G2 closed live by B656)
- **Open gaps — blocked**: 0
- **FOCUS STOPPED** 2026-08-29: investigable 8/8 + synthesis (OMB1-3, OMA1-8); MCP-G2 requires-execution deferred; §18 retro pending
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none

## Dismissed file types

- none (focus reuses real install jars + existing decompiled corpus; no new census)
