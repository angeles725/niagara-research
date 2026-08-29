<!-- kit-retro -->
# §18 Self-Retrospective — focus `own-modules-audit` (B637–B647)

**Date**: 2026-08-29 · **Focus**: own-modules-audit (niagara-research) · **Blocks**: B637–B647 (11) · **Mode**: heavy/auto, self-paced, Opus + sonnet sweeps · **Review-status**: pending
**Outcome**: investigable 8/8 (OMB1-3 build process, OMA1-7 per-module) + synthesis B647; MCP-G2 requires-execution deferred. Corrections issued: §14 to [B636] #2 (via B638), reframe of [B636] #6 (B639), self-correction of [B640] P4/P5 (B640/B645). Nace del pedido "puedes ver los demas modulos" + operator-pointed real source.

## What the focus established
- OMB1-3 (B637-639): the REAL build process — deploy modes A/B/C, the Clean+Slotomatic+Build variant rule, version-targeting via the `niagara_home` SDK path (compile 4.13.2 SDK → deploy 4.14), signing by convention with `angelessignerCA` (ANGELES; SEJOFA legacy), and the verified tests verdict (niagaraTest dead by plugin-7.6.17 bug; pure-JUnit works).
- OMA1-7 (B640-646): systemic patterns (over-permission, frozen 1.0, signer migration) + per-module audits (mcpbridge MCP authz-bypass, sdash uber-jar, datacenter Gson/3D, ANGELES-namespace, httpClientGAngeles=Tridium fork, six-dashboard template).
- OMA8 (B647): shop signature + corrected build knowledge + prioritized remediation + fixed reference template.

## What worked
- **REAL-ARTIFACT-FIRST (module-anatomy retro delta #1) VALIDATED at scale.** The entire focus ran on real jars (`unzip -l/-p`) + real source (BUILD_WORKFLOW.md, gradle) — findings the decompiled corpus could never give (SMA-neutralized fork, the tests bug, the CASINO asset, the MCP authz-bypass). Confirms the delta; recommend promoting it in the kit.
- **Mid-run operator corrections anchored severity.** The operator injected four corrections mid-focus (tests useless→plugin bug; version-targeting deliberate→§14; ANGELES active signer→reframe; **only chihuahua in production**). The production-scope correction reframed EVERY non-chihuahua finding from "risk" to "dev/demo hygiene" — without it the audit would have over-stated severity. Lesson: for an audit focus, establish PRODUCTION SCOPE early; it is the severity denominator.
- **FALSIFY-BEFORE-REPORTING earned its keep on mcpbridge.** The "AI-agent write surface — is it open?" fear was tested by decompiling `doPost` (401 gate → NOT open) then `ToolDispatcher`/`SetPropertyHandler` (static/userless → authz BYPASSED). Both the refutation (auth) and the confirmation (authz) came from reading the bytecode, not assuming.

## Proposed kit deltas (PROPOSE, do not apply)
1. **[MED] READ-THE-RESIDUE for packaging/jar audits (a specific rule).** A high class/asset COUNT in a jar is NOT evidence of a defect — it is usually a bundled library. This focus made the mistake TWICE ([B640] P4 called pure-web ux "empty shells" from class-count=0; [B640] P5/[B636] #5 feared "heavy Java in ux" — [B645] showed it was 99% bundled Gson, 2 own classes). Both needed a §14/self-correction. Propose a NORMAL-CYCLE line: before judging a jar's size/emptiness, read the PACKAGE HISTOGRAM (`unzip -l | package prefix | uniq -c`) AND the `rc/` listing — distinguish own-code from bundled-lib and web-assets before asserting. (Specialization of the existing "read the residue before theorising".)
2. **[LOW, REINFORCE] `verify-block` jar!entry + [BNNN] citation class.** Recurred on ~8 blocks this focus (every direct-artifact/synthesis block WARNed "N [CERT] but ZERO file:line resolved"). Second focus in a row hitting it — reinforces module-anatomy retro delta #2. The WARN reads as a defect on legitimately-cited direct-artifact blocks.
3. **[LOW, REINFORCE] Combined-sweep-for-independent-small-items.** The anomaly sweep (sdash+mcpbridge+datacenter in one sonnet agent → three blocks) worked again — reinforces module-anatomy retro delta #3.
4. **[LOW] AUDIT focuses need an explicit PRODUCTION-SCOPE step at bootstrap.** Establishing "which of these artifacts is actually in production" before grading turns severity from guessed to grounded (here: only 1 of 13 modules). Propose adding it to the AUDIT-FIRST bootstrap for audit-type focuses.

## Non-kit note
- Same RESEARCH-STATE row-edit friction as module-anatomy (exact-suffix python replaces; one status cell (OMA2) missed and needed a fix commit). Execution, not a rule.
