<!-- review-status: pending -->
<!-- Marker lifecycle: maintainer flips 'pending' → 'applied <date> · kit <sha>' (or 'dismissed') once folded; sweep-retros.sh reads this (METHODOLOGY §18). -->
# Retro — niagara-research · research-sdd · 2026-09-05 · Campaign-7 research (2/2): the `-ux` write-surface EXEMPLAR (B796) — kit delta for types/dashboard.md

> Run reviewed: campaign-7 research candidate 2 (ranked HIGH). Tridium ships no clean SPA/servlet-split `-ux`
> exemplar ([B791] THIN), so B796 captures OUR proven `com.angeles.DashboardPan.ux` seam (§20 document-mode) as
> the reference the kit cites: pure `route()→RouteAction`, the thin servlet adapter, 14 off-station tests, and
> the five write-surface gates scored 4/5 with file:line. READ-ONLY on the build kit — PROPOSES only (§18).

## Proposed kit deltas (for `/build-n4-module`)

| # | Proposed change | Target (file) | Evidence (block · key cite) | Priority |
|---|---|---|---|---|
| UXΔ1 | `types/dashboard.md` "-ux testable seam" + "write-surface" sections POINT AT B796 (DashboardPan-ux, file:line) as the worked exemplar, replacing the missing Tridium reference. | `types/dashboard.md` | B796 §796.2/§796.4; B791 (THIN) | HIGH |
| UXΔ2 | Keep the RULE as [B763] DWS1 (five gates) + [B762] DUX1 (pure route seam); B796 is the "here is one that does it, WITH tests (14 @Test)" citation — rule + exemplar, not a new rule. | `types/dashboard.md` | B796 §796.6; B762/B763 | HIGH |
| UXΔ3 | Document gate 4 (per-Ord lock + HTTP 423) as REQUIRED-but-not-yet-in-the-exemplar so the kit does not teach a 4-gate ceiling; link the residue to issue #49. | `types/dashboard.md` | B796 §796.4 gate 4 (absent) | MED |

## The bite (why the exemplar is honest)
- The exemplar scores **4/5**, not 5/5, and says so: gates 1/2/3/5 met with file:line, gate 4 (per-Ord lock/423)
  ABSENT (targeted grep = 0; the open punch-list item, #49). Pointing at a real 4/5 module — plus the DUX2
  anti-pattern `DashboardReader` (live `BComponent`, 15+ baja imports) in the SAME module — teaches the boundary
  better than an idealized snippet.

## Already covered (dedupe)
- The five gates and the pure-seam rule are [B763]/[B762]; B796 adds only the file:line SCORING against a
  shipping module + the negative (gate 4 absent). CSRF `/rpc/*`-only framing = [B58]/[B507] (why gate 2 is
  hand-rolled), cited not re-derived.

## What went well (keep)
- Re-anchored every file:line against the REAL DashboardPan-ux source (not just B762/B763's prior cites) before
  writing — route()/RouteAction, checkCanWrite, SERVICE_ORD pin, audit fire-and-forget, and the 14 @Test all
  re-grepped. A document-mode exemplar the kit will cite must be [CERT] at today's line numbers.
- The negative (gate 4 absent) is carried as B796-G1 requires-execution, so the exemplar's honesty survives into
  the kit rather than being smoothed over.
