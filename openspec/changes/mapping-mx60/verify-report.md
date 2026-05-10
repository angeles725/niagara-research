# Verify Report — mapping-mx60 (third run)

**Date**: 2026-05-09
**Phase**: sdd-verify (third run after 2 re-loops)
**Verdict**: PASS-WITH-WARNINGS
**History**: Run 1 FAIL (REQ-1 dependencies missing, REQ-3 backend.profile null), Run 2 FAIL (REQ-9 verified_at null on all 100), Run 3 PASS-WITH-WARNINGS

## Verdict summary

All 14 REQs pass (12 PASS, 2 PASS-WITH-WARNINGS). No CRITICALs. 3 carry-forward WARNINGs (W-1, W-2, W-3), all previously assessed as intentional. ARCHIVE-READY per spec acceptance verdict mapping.

---

## REQ verification (14 REQs — final)

| REQ | Run 1 | Run 2 | Run 3 | Evidence (run 3) | Notes |
|-----|-------|-------|-------|------------------|-------|
| REQ-1 | FAIL | PASS | PASS | null-field query → 0; `has("dependencies")` absent → 0; source_doc/verified_at keys present on all 100 | FIXED run 1→2, stable |
| REQ-2 | PASS | PASS | PASS | schema_version=1.0, module=mx60-chihuahua, generated_at=2026-05-10T03:02:59Z ISO 8601, entries=100, exclusions=20 | stable |
| REQ-3 | FAIL | PASS | PASS | 0 java-class with null backend.profile; BChiDashboardService profile=rt; BChiServlet profile=ux + 31 rest_endpoints | FIXED run 1→2, stable |
| REQ-4 | PASS | PASS | PASS | 0 iife-* missing frontend_iife; AlarmsManager ns=MX60 iife_pattern=iife-window role=producer; SubscriptionPool globals_read non-empty | stable |
| REQ-5 | PASS-WITH-WARNINGS | PASS-WITH-WARNINGS | PASS-WITH-WARNINGS | 10 unique kinds all valid; iife-util declared in schema but 0 entries use it (W-1) | intentional |
| REQ-6 | PASS | PASS | PASS | valid JSON; 100 entries = 100 data rows in index.md Entries section | stable |
| REQ-7 | PASS-WITH-WARNINGS | PASS-WITH-WARNINGS | PASS-WITH-WARNINGS | 17 domain docs; sections 4+5 names differ from spec ("Data Flow / Integration Points", "Notes & Cross-References" vs spec names) — W-2 | intentional |
| REQ-8 | PASS | PASS | PASS | 71 source / 57 in-scope = 124.5%; 40 entries spot-checked fidelity 100% | stable |
| REQ-9 | PASS (incorrect) | FAIL | PASS | `select(.verified_at != null) | length` → **40**; format "2026-05-09T00:00:00Z" ISO 8601; shards: s1=5 s2=9 s3=7 s4=11 s5=4 s6=4; 40 IDs match _validation.md Tier 3 exactly | FIXED run 2→3 |
| REQ-10 | PASS | PASS | PASS | exclusions=20; srcTest in excluded.md (22 hits); 0 srcTest paths in entries | stable |
| REQ-11 | PASS | PASS | PASS | delta schema_version=1.0 compared_against=reflow-clean-177; 0 null evidence; 0 invalid status; §68.1–§68.5 all covered | stable |
| REQ-12 | PASS | PASS | PASS | 88 xref edges ≥ 80; 0 null fields; UpDetail.js 10 outgoing edges; 0 orphan to_ids | stable |
| REQ-13 | PASS | PASS | PASS | S1=8 S2=9 S3=14 S4=16 S5=10 S6=43 all ≤75; total=100 in [100,140] | stable |
| REQ-14 | PASS | PASS | PASS | 45 "inferred from mapping" occurrences total (36 md + 9 json) ≥ 10; SubscriptionPool, WritePoint, BChiDashboardService controlTick annotated | stable |

**REQ pass: 12/14 PASS, 2/14 PASS-WITH-WARNINGS, 0/14 FAIL**

---

## Validator check (17 checks)

- File: `docs/mappings/mx60-chihuahua/scripts/validate-shard.jq` — 17 checks confirmed in source
- Run: `jq -f scripts/validate-shard.jq index.json` → **EMPTY** (exit 0, all 17 checks PASS)
- Cross-check 40 verified_at ids: index.json 40 verified_at IDs match _validation.md Tier 3 exactly
- Shard-level confirmation: s1(5) + s2(9) + s3(7) + s4(11) + s5(4) + s6(4) = 40 written to actual shard files

---

## Cross-cutting checks

- Hard cap 75/shard: PASS (max=S6 at 43)
- id == path: PASS (validator check #2, 0 violations)
- Prohibited fields: PASS (0 violations)
- Top-level profile/decompiled leaks: PASS (0 occurrences — W-4 resolved in run 1)
- Coverage: 124.5% PASS
- Inferred-from-mapping floor: 45 total PASS (≥10)
- xref orphan to_ids: 0 PASS

---

## CRITICAL findings

**NONE.**

---

## WARNING findings

- **W-1**: 4 rc/js/util/ files classified as iife-lib instead of iife-util; iife-util kind declared in schema but 0 entries use it — UNCHANGED (intentional)
- **W-2**: Domain section names 4+5 differ from spec REQ-7 spec names — UNCHANGED (intentional design decision)
- **W-3**: 6 delta rows with bloque68_section: null — UNCHANGED (legitimate non-reflow entries)

---

## SUGGESTION findings

- **S-1**: Validator extension — RESOLVED (17 checks, was 13 in run 1)
- **S-2**: Reclassify 4 iife-util files — UNCHANGED (acceptable, deferred)
- **S-3**: Populate dependencies with meaningful content — UNCHANGED (deferred to transplant SDD)

---

## Recommended next phase

**sdd-archive** — change is ARCHIVE-READY. All 14 REQs pass, 0 CRITICALs, W-1/W-2/W-3 carry-forward acceptable per orchestrator review.

## skill_resolution

injected
