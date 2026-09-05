<!-- review-status: pending -->
<!-- Marker lifecycle: maintainer flips 'pending' → 'applied <date> · kit <sha>' (or 'dismissed') once folded; sweep-retros.sh reads this (METHODOLOGY §18). -->
# Retro — niagara-research · research-sdd · 2026-09-05 · thread `module-web-tier-exemplars` — AUDIT-FIRST verdict THIN (1 audit block, 0 investigation blocks)

> Bounded thread (lead-requested, ≤5 blocks, no padding): document how Tridium's OWN modules build the web tier
> that `types/dashboard.md` covers only from our modules. AUDIT-FIRST result: the web-tier MECHANISM is fully
> covered by the existing corpus; the residue is THIN (< 2 blocks). Outcome = ONE audit block (B791) + this retro;
> NO investigation blocks opened. No focus was scaffolded (a full RESEARCH-STATE for a 1-block audit would itself be
> padding). READ-ONLY on the kit — PROPOSES only.

## Verdict
- Web-tier aspects a–e (BWebServlet/BServletView routing · hx BHxView/BHxProfile/HxOp · `module://<mod>/rc/…` ORDs ·
  `@AgentOn` web agents · Tridium-servlet auth/CSRF) are FULLY covered: B29/B9/B58/B74/B433/B293/B421/B752/B5/B12/B48.
- Aspect f (JSON/REST response shaping) is mostly covered: report B361-B364, analytics B16/B66, nss B604, obix B509,
  the 34-class Response pattern B58 §58.2; jsonToolkit is a serialization lib (B32), not a web-shaping exemplar.
- No candidate module (analytics-ux, jsonToolkit, report, webEditors, nss, hx) adds a web-tier exemplar beyond the
  above; `tunnel` is Fox tunneling (B37/B93), not browser web-tier. → **0 new investigation blocks.** Full map in B791.

## Proposed kit delta (→ `types/dashboard.md`)
- **DUX-WEB1** — add a POINTER TABLE (web-tier aspect → the exemplar block that documents it: servlet routing B29,
  hx B433, `module://` rc B5/B752, `@AgentOn` web agent B752/B421, Tridium CSRF B58 vs our guard B763, JSON/REST
  B16/B361-364/B604/B509), so the section reaches the Tridium exemplars, not just our modules. Pure remittance/index,
  no new investigation. `[ev: corpus B791]`
- **DUX-WEB2** — record the DashboardPan-ux divergences from the Tridium exemplar: (a) hand-rolled `X-Requested-With`
  CSRF inside `route()` = a DELIBERATE, stronger-than-vendor divergence (note, not defect); (b) the collapsed RBAC seam
  = a punch-list item (re-split to a Niagara-free testable decision, B762/B763). `[ev: corpus B791/B763]`

## Client punch-list
- DashboardPan-ux: re-split the RBAC decision into a pure (Niagara-free) seam + a Baja adapter (B763 §763.4) so the
  write-auth is unit-testable — the one real divergence from the vendor pattern (the CSRF divergence is intentional).

## Already covered (dedupe)
The entire web-tier mechanism + every candidate module's concrete pattern is already in the corpus (see the Verdict).
B791 is an INDEX over those blocks, not a re-derivation.

## What went well (keep)
- AUDIT-FIRST prevented a 2–4 block loop over already-covered mechanism — the thread's "stop after the audit block if
  the residue is thin, no padding" rule worked exactly as intended.
- The honest output is a small, high-value one: a pointer-table delta + a divergence punch-list, not manufactured blocks.
