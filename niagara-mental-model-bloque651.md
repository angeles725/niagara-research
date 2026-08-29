# Niagara N4 — chihuahua-source (CS6): reconciling the internal 2026-05-06 audit — a thorough adversarial dual-auditor pass (CONDITIONAL PASS, 14/14) whose fixes hold in current source; its critical P1 fault-discrimination fix covers the DISPLAY path, leaving the PROTECTION path (B650) as the complementary residual

**Focus**: chihuahua-source · **Gap**: CS6 (reconcile audit-2026-05-06) · **Session**: 2026-08-29 · **Block**: B651
**Sources** (`[CERT]` real docs + source): `chihuahua/audit-2026-05-06/{veredicto,inconsistencias,pendientes_estado,live_updates_faltantes,fixes_verificados,futuras-mejoras-schedules-proteccion}.md` + `chihuahua-ux/src/com/angeles/chihuahua/ux/ChiEquipmentReader.java`.

**Scope**: verify the operator's own prior audit against CURRENT source; cross-link to the CS2 protection finding [B650]. This is the production module's existing QA record.

---

## 651.1 The internal audit was rigorous

`[CERT]` `veredicto.md:11` — the operator ran a **2-agent adversarial audit** (2026-05-06) and reached **CONDITIONAL PASS**: apt for production after the applied fixes, pending live end-to-end smoke test. Scope covered F1-F14 regressions, 7 pendientes (P1-P7), 8 anti-pattern categories, and a per-component live-update matrix. `fixes_verificados.md:6` — **14/14 PASS, 0 FAIL, 0 regression** after the F9 fix. The methodology was careful — e.g. `inconsistencias.md` did per-file grep of subscribe/unsubscribe pairing and CORRECTED a false positive (CarcamoDetail "add=3 rem=2" was a DOM `addEventListener` mismatch, not a listener leak). This is genuinely good QA practice for a production module.

---

## 651.2 The fixes hold in current source (spot-verified)

`[CERT]` — the critical P1 fix is present in current source: `ChiEquipmentReader.readNumericNullable(...)` used for all UP readings (`ChiEquipmentReader.java:439-444` — tempZona/tempAbasto/tempRetorno/tempExterior/humedadZona/setpoint…), returning `null` (JSON null) for a faulted/absent slot instead of `0.0`. Audit-reported status, reconciled:

| ID | Audit status | Current source |
|---|---|---|
| **P1** fault discrimination (25 slots: disconnected→0.0/false = "phantom antifreeze", indistinguishable) | ✅ IMPLEMENTED (ChiEquipmentReader null-aware + null DTOs + ChiJsonUtil overloads) | ✅ confirmed present (readNumericNullable) |
| **F9** alarm ack guard (`ackState !== 'ackPending'`) | ✅ Fixed | audit-attested (JS frontend) |
| P2 chart history perf | ⚠ MITIGATED (cap 7d→24h) | mitigation, not full refactor |
| P3 "fan dampingFactor" | ❌ REFUTED (it's OrbitControls, not the fan) | correct refutation |
| P4 config live-reload | ✅ (visibilitychange + ConfigManager.reload) | attested |
| P5 history array growth | ✅ (HISTORY_MINUTES 24h + splice) | attested |
| P6 alarm latch seed | ✅ (via F9) | attested |
| P7 cache-buster | 📝 DOCUMENTED (deferred) | open, low |
| AP5 direct status read (3 sites) | ✅ StatusResolver | attested |
| AP7 watchdog timer leak (pagehide) | ✅ clearInterval | attested |

Non-blockers still open (audit-acknowledged, post-deploy): Alarms `_renderShell` full rebuild every 5 s (operator focus loss), "hace X" timestamps don't tick, 7-chart `rebuildChart` per-notify perf.

---

## 651.3 The key reconciliation: two fault-discrimination paths

The audit's critical P1 and my CS2 finding ([B650]) are the SAME issue class — a disconnected/faulted slot reading as `0.0` — but on **two different code paths**:

- **Display/read path (P1, FIXED)**: `ChiEquipmentReader.readNumericNullable` → `null` for the dashboard JSON, so the UI shows "no data" not a false `0.0`. Comprehensive (25 slots). Confirmed in source (§651.2).
- **Protection-eval path ([B650], RESIDUAL)**: `BChiDashboardService.readSlotVal` still collapses a faulted slot to `0.0`, which drives `applyProtections` — antifreeze phantom-trips (fail-safe but nuisance) and overload fails-to-non-trip (fail-to-danger). The 2026-05-06 audit fixed the DISPLAY of phantom values but did NOT change the PROTECTION eval's use of `0.0`.

So the audit resolved the operator-visible symptom (phantom antifreeze on the dashboard) but the underlying protection logic retains the directional `0.0`-collapse. This is the highest-value cross-block output of CS6: **extend the P1 fault-discrimination fix from `ChiEquipmentReader` (display) into `readSlotVal`/`applyProtections` (protection)** — treat a faulted amp/temp sensor as a FAULT (surface an alarm; do not silently read 0.0 into the overload comparison), per [B650] §650.3.

The `futuras-mejoras-schedules-proteccion.md` doc suggests the operator already tracks protection improvements — a natural home for this recommendation.

---

## 651.4 Grade

The production module has a real, rigorous internal audit trail (adversarial dual-auditor, grep-verified, false-positives corrected) whose fixes are present in current source — strong evidence chihuahua is well-maintained, not ad-hoc. The one substantive carry-forward is the protection-path fault discrimination ([B650]); the rest are acknowledged post-deploy polish items.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | internal audit = 2-agent adversarial, CONDITIONAL PASS, 14/14 post-F9 | [CERT-doc] | veredicto.md:11 · fixes_verificados.md:6 | ✅ read |
| 2 | P1 = fault discrimination (disconnected slot→0.0/false=phantom antifreeze), 25 slots | [CERT-doc] | pendientes_estado.md P1 | ✅ read |
| 3 | P1 fix present in current source (readNumericNullable, null DTOs) | [CERT] | ChiEquipmentReader.java:439-444 | ✅ read verbatim |
| 4 | P2 mitigated (24h cap), P3 refuted, P4/P5/P6 fixed, P7 documented; AP5/AP7 fixed | [CERT-doc] | pendientes_estado.md + inconsistencias.md | ✅ read |
| 5 | P1 (display) ≠ B650 (protection): readSlotVal still collapses to 0.0 in applyProtections | [CERT] | [B650] §650.3 + ChiEquipmentReader (display only) | ✅ cross-ref |
| 6 | open non-blockers: renderShell focus loss, timestamp tick, chart perf | [CERT-doc] | veredicto.md non-bloqueantes | ✅ read |

**Tally**: [CERT-doc] ×4 · [CERT] ×2 · reconciliation block. Audit docs + the P1-in-source cross-check token-checked. SOURCES: the 6 audit md files are internal to the module repo (cited by path).

## Connections

- **[B650]** — CS2 protection finding; §651.3 shows P1 fixed display, [B650] is the protection residual. **[B648]** — the servlet layer (separate). **[B543]** — the fault-discrimination/fail-safe theme.
- Forward: CS8 (verdict folds in: internally audited + fixes present + the one protection carry-forward). CS4/CS5 (the ux helpers/frontend the audit exercised).

## Gaps uncovered

- None new. The open non-blockers (renderShell/timestamp/chart-perf) are the operator's own tracked post-deploy items, not new research gaps.
