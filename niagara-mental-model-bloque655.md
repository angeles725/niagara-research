# Niagara N4 — chihuahua-source (CS8, SYNTHESIS): production-readiness verdict — chihuahua is genuinely well-built (RBAC + audit + defensive design + internal QA + injection-safety), clearly the production-grade module of the fleet; ONE real fix matters (overload protection fails-to-danger on sensor fault), the rest is polish

**Focus**: chihuahua-source · **Gap**: CS8 (production-readiness synthesis) · **Session**: 2026-08-29 · **Block**: B655 · **Type**: DESIGN/synthesis (consolidates B648-B654 + B636/B647).
**Sources**: [B648]-[B654] (this focus), [B636]/[B647] (jar audit + reference), [B643] (mcpbridge contrast).

**Scope**: answer the operator's question — "no estoy seguro que tan bien esté chihuahua" — with a source-grounded verdict + a prioritized fix list. chihuahua is the ONLY production module ([B643]).

---

## 655.1 Verdict: WELL-BUILT — production-grade

Source-level, chihuahua is a genuinely well-engineered module — and measurably the best of the operator's fleet (fitting, since it is the only one in production). The evidence across CS1-CS7:

| Dimension | Finding | Grade |
|---|---|---|
| **Write authorization** ([B648]) | `ChiRbacHelper.checkCanWrite` = `BPermissions.OPERATOR_WRITE`, fail-closed, FIRST line of all 8 write handlers, before any mutation | ✅ strong — the correct inverse of mcpbridge's bypass ([B643]) |
| **Audit** ([B648]) | every write records `{ts,user,action,ord,old,new}` (ring ~500) | ✅ present (plaintext/bounded, acceptable) |
| **Defensive design** ([B650]) | null/NaN/epsilon/JSON/event-thread(ADR-D7)/steady-state guards; per-Ord lock | ✅ strong |
| **Read layer** ([B652]) | History API (dodges B359 NPE), long-millis BQL, allowlisted writes, complete JSON escaping | ✅ injection-safe, N4.14-aware |
| **Frontend** ([B653]) | ES5 store/subscription SPA, Fox-subscription live + polling fallback, RBAC server-authoritative | ✅ production-quality |
| **WB tooling** ([B654]) | `BBatchLinkEditor` transactional bulk-link authoring | ✅ a real strength |
| **Build** ([B649]) | best-versioned of fleet (1.0→1.3), correct template, over-perm was empty scaffold (§14) | ✅ good |
| **Internal QA** ([B651]) | adversarial dual-auditor CONDITIONAL PASS 14/14, fixes hold in source | ✅ rigorous |

This is NOT an ad-hoc module. It has authorization, audit, fault-handling, injection-safety, an internal audit trail, and testable pure-Java seams. The operator's uncertainty is understandable (fast-moving, AI-assisted, some stale Javadoc) but the source does not bear out a "poorly built" fear.

---

## 655.2 The ONE fix that matters — overload protection fault-to-danger

`[CERT]` [B650] §650.3 — the single substantive defect: `BChiDashboardService.readSlotVal` collapses a faulted/null sensor to `0.0`, so in `applyProtections`:
- **antifreeze** (low-limit): faulted temp → 0.0 < threshold → TRIPS → **fails safe** (nuisance shutdown, but safe).
- **overload** (high-limit): faulted amp → 0.0 > threshold is false → **does NOT trip → the overload protection silently disables if its current sensor faults**. Fail-to-danger.

The internal audit ([B651] P1) already fixed fault-discrimination on the DISPLAY path (`ChiEquipmentReader` → null), but NOT on the PROTECTION path. **Fix (HIGH)**: extend fault-discrimination into `readSlotVal`/`applyProtections` — treat a faulted amp/temp sensor as a FAULT (raise an alarm, do not read 0.0 into the overload comparison), so a dead sensor is surfaced rather than silently disabling protection. Mitigant: the physical BACnet controller likely has its own overload trip; but the dashboard-layer guard should not fail-to-danger. This is the one item with a safety dimension.

---

## 655.3 Prioritized fix list

| # | Fix | Severity | Source |
|---|---|---|---|
| 1 | Overload protection: treat faulted amp sensor as FAULT, not 0.0 (extend P1 fault-discrimination into `readSlotVal`/`applyProtections`) | **HIGH** (safety) | [B650]/[B651] |
| 2 | Run `gradlew slotomatic` — the `protXActive`/`resetAlarmas` AUTO region is stale (AWAITING REGEN) | MED (build hygiene) | [B649]/[B650] |
| 3 | Add a `module.palette` (chihuahua exports reusable components but ships none) | LOW | [B636]/[B649] |
| 4 | Drop dead `jacoco`/`niagaraTest` wiring (plugin-7.6.17 bug); keep `run-tests-wsl.sh` (pure JUnit) | LOW | [B637]/[B649] |
| 5 | Optional: delete the empty `<permissions>` scaffold (cosmetic — not over-privilege, §14 [B649]) | LOW | [B649] |
| 6 | Post-deploy polish (audit's own list): Alarms `_renderShell` focus loss, "hace X" timestamp tick, chart rebuild perf, CSRF token | LOW | [B651] |

Priority: **#1 first** (the only safety item), then #2 (before the next release build). #3-#6 are hygiene/polish.

---

## 655.4 chihuahua vs the fleet + the shop takeaway

chihuahua demonstrates the shop CAN build production-grade N4 modules — RBAC ([B648]), audit, defensive design, injection-safety, internal QA. The gap to the dev/demo modules ([B640]-[B646]) is not capability but PROCESS: the dashboard template ([B646]) and the fixed build template ([B647]) should inherit chihuahua's practices (server-RBAC servlet pattern, per-agent permissions à la sdash [B644], version-bumping, fault-discrimination) — and chihuahua's own #1 fix (protection fault-handling) should be baked into the template so future site-clones don't repeat it. The MCP write-authorization lesson ([B643]) and chihuahua's correct RBAC ([B648]) are the two poles that define the shop's "how to gate writes" standard.

**Bottom line for the operator**: chihuahua is well-built and production-appropriate. Fix the overload-sensor-fault behavior (#1); everything else is polish. Your uncertainty was worth checking — but the source says you built it right.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | write-auth RBAC-enforced (OPERATOR_WRITE, all 8 handlers, audit) | [CERT] | [B648] | ✅ cross-ref |
| 2 | strong defensive design + injection-safe read layer + server-authoritative RBAC frontend + WB tooling | [CERT] | [B650]/[B652]/[B653]/[B654] | ✅ cross-ref |
| 3 | internal audit rigorous, fixes hold; build best-versioned; over-perm = empty scaffold | [CERT]/[CERT-doc] | [B651]/[B649] | ✅ cross-ref |
| 4 | ONE real fix: overload protection fails-to-danger on faulted amp (readSlotVal→0.0) | [CERT] | [B650] §650.3 | ✅ cross-ref |
| 5 | verdict: production-grade, clearly the best of the fleet; process (not capability) is the fleet gap | [INFER]/synthesis | §655.1-655.4 | ✅ derived |

**Tally**: [CERT] refs to [B648]-[B654] + [CERT-doc] [B651] · DESIGN/synthesis (ratio = consolidation). Every claim back-references a verified evidence block.

## Connections

- Consolidates **[B648]-[B654]** (chihuahua-source). **[B643]** — mcpbridge (the write-auth counter-pole). **[B636]/[B647]** — jar audit + fleet template (which should inherit chihuahua's practices). **[B640]-[B646]** — the dev/demo fleet.
- **FOCUS chihuahua-source CLOSED** — 8/8 (CS1-CS8).

## Gaps uncovered

- None investigable on disk. Live §12 validation of the overload-fault fix + the config.bog check whether mcpbridge is mounted (MCP-G2, [B643]) are the only requires-execution residues — deferrable, need the live station (API2). Focus STOPS.
