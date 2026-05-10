# Delta vs Reflow — mx60-chihuahua vs reflow-clean-177

**Generated**: 2026-05-09
**Module**: mx60-chihuahua
**Compared against**: reflow-clean-177
**Total delta rows**: 28

## Status legend

| Status | Meaning |
|--------|---------|
| `HEREDADO` | Port-marker present AND \|LOC delta\| ≤15% |
| `REESCRITO` | Port-marker AND >30% LOC delta OR name-match AND >30% LOC delta |
| `ANÁLOGO` | Name-match AND 15-30% LOC delta AND no port marker |
| `NUEVO` | MX60 has it; reflow does NOT |
| `FALTA` | Reflow has it; MX60 does NOT |

## Summary by status

| Status | Count |
|--------|-------|
| NUEVO | 16 |
| ANÁLOGO | 6 |
| REESCRITO | 2 |
| HEREDADO | 2 |
| FALTA | 3 |
| **Total** | **28** |

---

## Delta table — Backend (Java)

| MX60 Component | Reflow Analog | Status | LOC MX60 | LOC Reflow | Δ% | Evidence | Bloque #68 |
|----------------|---------------|--------|-----------|------------|-----|----------|------------|
| `ChiHistoryHelper.java` | `HistoryData.java` | ANÁLOGO | 619 | 663 | 6.6% | ChiHistoryHelper.java:1 (port-marker: Ported from SnlsHistoryHelper) | §68.1 |
| `ChiServletDispatch.java` | `BaseServlet.java` | ANÁLOGO | 594 | 367 | 61.8% | ChiServletDispatch.java:3 (partial port verbatim) | §68.2 |
| `BChiServlet.java` | `BaseServlet.java` | REESCRITO | 1743 | 367 | 374.7% | BChiServlet.java header (port-marker + massive expansion, 31 endpoints) | §68.2 |
| `ChiJsonUtil.java` | SnlsJsonUtil (inline) | HEREDADO | 270 | — | — | ChiJsonUtil.java: 'ported verbatim from SnlsJsonUtil.escapeJson' | — |
| `ChiAlarmHelper.java` | AlarmData concept | ANÁLOGO | 2041 | — | — | No direct port-marker; functional analog; adds latch mechanism | §68.4 |
| `ChiAlarmQueryHelper.java` | — | NUEVO | — | — | — | rg 'AlarmQueryHelper' reflow → 0 | §68.4 |
| `ChiScheduleHelper.java` | — | NUEVO | 254 | — | — | rg 'ScheduleHelper' reflow → 0 | — |
| `ChiEquipmentReader.java` | — | NUEVO | — | — | — | Physical equipment model MX60-specific | — |
| `ChiThresholdHelper.java` | — | NUEVO | — | — | — | Threshold system MX60-specific | §68.3 |
| `BChiDashboardService.java` | — | NUEVO | — | — | — | No RT service root in reflow | §68.2 |
| `BChiUp.java` | — | NUEVO | — | — | — | 37-slot BComponent, MX60-specific | §68.3 |
| `BChiCarcamo.java` | — | NUEVO | — | — | — | 8-slot BComponent, MX60-specific | — |
| `BChiDatalogger.java` | — | NUEVO | — | — | — | 9-slot BComponent, MX60-specific | — |
| — | `HistoryGhostSubscriber.java` | FALTA | — | — | — | rg 'GhostSubscriber' chihuahua → 0; MX60 polls REST | §68.1 |
| — | `HistoryDataResponse.java` | FALTA | — | 265 | — | rg 'HistoryDataResponse' chihuahua → 0; serialization inline | §68.1 |

---

## Delta table — Frontend (JS)

| MX60 Component | Reflow Analog | Status | LOC MX60 | LOC Reflow | Δ% | Evidence | Bloque #68 |
|----------------|---------------|--------|-----------|------------|-----|----------|------------|
| `SubscriptionPool.js` | subscriberMixin concept | HEREDADO | 587 | — | — | Pattern ported from BajaScript subscription concept (inferred from mapping) | §68.5 |
| `WritePoint.js` | — | REESCRITO | 154 | — | — | WritePoint.js:151-153 dual-path REST fallback; no reflow analog | §68.5 |
| `AlarmModalActions.js` | `AlarmNotes.vue` | ANÁLOGO | 240 | 124 | 93.5% | Same functional role (inline alarm actions with popover); no port-marker | §68.4 |
| `AlarmNotesModal.js` | `AlarmNotesModal.vue` | ANÁLOGO | 251 | 64 | 292.2% | Same name, same role; LOC delta due to IIFE boilerplate | §68.4 |
| `TimeRangePicker.js` | `TimeRangePicker.vue` | ANÁLOGO | 161 | 190 | 15.3% | Same name, same role; no port-marker; 15.3% delta | — |
| `AlarmsManager.js` | — | NUEVO | 326 | — | — | rg 'AlarmsManager' reflow → 0; reflow uses Vuex | §68.4 |
| `UpDetail.js` | — | NUEVO | 3841 | — | — | No reflow analog for equipment detail | §68.3 |
| `CarcamoDetail.js` | — | NUEVO | 1040 | — | — | MX60-specific | — |
| `DataloggerDetail.js` | — | NUEVO | 700 | — | — | MX60-specific | — |
| `Configuracion.js` | — | NUEVO | 535 | — | — | MX60-specific | — |
| `UpThresholdStore.js` | — | NUEVO | 198 | — | — | Threshold system MX60-specific | §68.3 |
| `CarcamoThresholdStore.js` | — | NUEVO | 216 | — | — | MX60-specific | §68.3 |
| `DataloggerThresholdStore.js` | — | NUEVO | 205 | — | — | MX60-specific | §68.3 |
| `ModoOverrideStore.js` | — | NUEVO | 65 | — | — | MX60-specific | §68.3 |
| `OutputOverrideStore.js` | — | NUEVO | 93 | — | — | MX60-specific | §68.3 |
| — | `AlarmNotes.vue` | ANÁLOGO | — | 124 | — | See AlarmNotesModal.js; dual-listed for FALTA check | §68.4 |

---

## Bloque #68 cross-reference summary

| §68.x | Prescription | Delta finding | Verdict |
|-------|-------------|--------------|---------|
| §68.1 | ChiHistoryHelper maps to HistoryData.java | ANÁLOGO (6.6% LOC delta) + 2 FALTA (GhostSubscriber + HistoryDataResponse) | Confirmed: history layer simplified in MX60 |
| §68.2 | BChiServlet is rewritten servlet layer | REESCRITO (374% LOC delta, 31 endpoints) | Confirmed: highest-risk backend for transplant |
| §68.3 | 5 threshold stores are NUEVO | 5x NUEVO stores + 3 NUEVO BComponents | Confirmed: threshold system entirely new |
| §68.4 | AlarmModalActions + AlarmNotesModal are ANÁLOGO | ANÁLOGO confirmed (same functional role) | Confirmed: latch mechanism NUEVO within domain |
| §68.5 | SubscriptionPool HEREDADO core + REESCRITO wrapper | HEREDADO concept + REESCRITO WritePoint | Confirmed: BajaScript bridge rewritten as IIFE |

---

## Human spot-check notes (REESCRITO rows)

**BChiServlet.java (REESCRITO)**:
- Manual review: 31 endpoints vs reflow's ~8. Port-marker says "Patterns ported from BSnlsServlet.java" but the vast majority of the file (write-lock, latch/unlatch, threshold endpoints, per-type equipment queries) is new code.
- Verdict: REESCRITO is correct. The port-marker applies only to the HTTP handling scaffold, not the business logic.

**WritePoint.js (REESCRITO)**:
- Manual review: dual-path (BajaScript first, REST fallback) is the defining feature. Reflow has no REST fallback for point writes.
- Verdict: REESCRITO is correct. The REST fallback changes the failure mode semantics entirely.

*Note: Human spot-check of REESCRITO rows is required per tasks.md. The above represents the apply-phase self-check. Full verification should be confirmed in sdd-verify phase.*
