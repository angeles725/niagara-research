# B791 · Web-tier exemplars — AUDIT-FIRST coverage map: the mechanism is fully covered (verdict THIN, 0 new blocks) + DashboardPan divergences

> **Scope**: the AUDIT-FIRST result for the bounded thread `module-web-tier-exemplars` — does the corpus already
> document how Tridium's OWN modules build the web tier that `types/dashboard.md` describes only from our modules?
> **Verdict: YES — the web-tier mechanism is fully covered; the residue is THIN (< 2 blocks), so NO new
> investigation blocks are written** (per the no-padding rule). This block IS the audit deliverable: the coverage
> map, the thin verdict, and the DashboardPan-vs-Tridium divergence punch-list. Focus: `module-web-tier-exemplars`
> (audit-only, STOPPED). Kit dest: `types/dashboard.md` (a pointer table + the divergence notes).
>
> **Sources**: FUENTE 1 corpus blocks (existence + on-topic verified this session via CATALOG): B9/B29/B58/B74,
> B433/B293 (hx), B421 (webEditors), B752/B762/B763 (ux serving + our servlet), B5/B12 (module:// ORDs), B36/B42/B48
> (BOX/agents), B16/B66 (analytics web), B361-B364 (report web), B604 (nss JSON), B509 (obix), B457/B508 (SCRAM),
> B32 (jsonToolkit), B37/B93 (tunnel = NOT web). READ-ONLY. English (post-B115).

---

## 791.1 — Coverage map (web-tier aspect → covered-by-block) `[CERT-corpus]`
| Aspect | Status | Covered by |
|---|---|---|
| a. `BWebServlet`/`BServletView` subclass + routing | COVERED | B29 (web tier + `/ord/*` dispatch), B9, B38 (`BFileUploadView extends BServletView`), B509 (`BObixServer extends BWebServlet`), B16 (`BNaServlet`), B752 §752.1 (servlet-SPA recipe) |
| b. hx views `BHxView`/`BHxProfile`/`HxOp` | COVERED | B433 (whole hx framework), B293 §293.12 (BHxProfile), B9 §9.2.3 |
| c. module `rc/` resources + `module://<mod>/rc/…` ORDs | COVERED | B5 (module:// scheme), B9 §9.2, B12, B752 §752.2 (`JsInfo.make(BOrd.make("module://…"))`), B421 §421.6 |
| d. `@AgentOn` WEB agents (view/JS registered on a type) | COVERED | B752 §752.2 (canonical `BSingleton implements BIJavaScript` + `@AgentOn` + `getJsInfo()`), B421 (66 webEditors), B48, B35 |
| e. auth/CSRF in Tridium servlets (vs our X-Requested-With) | COVERED | B58 §58.3 (`CsrfGuard.validate`), B29 §29.3.3 (`CsrfProtectedFilter` + auth-order gotcha), B18, B457/B508 (SCRAM); the CONTRAST to our hand-rolled guard is already drawn in B763 §763.3 (+ B74) |
| f. JSON/REST response patterns (report/analytics/nss/obix) | MOSTLY COVERED, thin residue | report B361-B364 (`BUxReportPane` grid), analytics B16 §16.6 + B66 (`BNaServlet` `/na`, `text/plain` quirk), nss B604 (`/nss/station/data` JSON), obix B509 (XML), the 34-class Response pattern B58 §58.2; jsonToolkit B32 (a serialization lib, not a web-shaping exemplar) |

## 791.2 — Residue verdict: THIN → 0 new investigation blocks `[CERT/INFER]`
Every aspect a–e is fully-covered MECHANISM. The only uncovered edge is a possible cross-cut synthesis of aspect f
("how Tridium `-ux` servlets shape response bodies" — `BNaServlet` text/plain vs report grid vs nss JSON vs obix XML),
but that would RESTATE facts already in B16/B361-364/B604/B509, not add new ones. Ranked residue check of the candidate
modules (analytics-ux, jsonToolkit, report, webEditors, nss, hx) = each is already documented; NONE would add anything
beyond B752/B762/B763/B421. `tunnel` is Fox serial/TCP tunneling (B37/B93), NOT browser web-tier — excluded.
**Recommendation: 0 new blocks; at most an OPTIONAL pointer table in `types/dashboard.md` (pure remittance).** Opening
2–4 blocks would loop over already-covered mechanism — the exact padding the thread's rule forbids.

## 791.3 — DashboardPan-ux divergence from the Tridium exemplar (punch-list) `[CERT, from B763/B752]`
- **RBAC seam COLLAPSED (punch-list)** — chihuahua-ux keeps a pure-vs-Baja RBAC seam; DashboardPan collapsed it
  (B763 §763.4). Re-split so the RBAC DECISION is Niagara-free/unit-testable (the B762 seam taxonomy).
- **CSRF via hand-rolled `X-Requested-With` in the pure `route()`** rather than the framework `CsrfProtectedFilter`/
  `/rpc/*` `CsrfGuard` (B763 §763.3 vs B58 §58.3). This is INTENTIONALLY STRONGER than vendor bajaux (which treat
  `requiredPermissions` as visibility-only and skip server RBAC, B752 §752.5) — a DELIBERATE divergence, not a defect;
  note it in the kit as such.
- **Open served-surface UX gaps** are the authoritative punch-list already in B753 §753.4 (from B752 §752.6).

## 791.4 — Kit implication (→ `types/dashboard.md`) `[INFER, grounded]`
The section currently documents the web tier only from OUR modules. Add a POINTER TABLE (aspect → the exemplar block
that documents it: servlet routing B29, hx B433, `module://` rc B5/B752, `@AgentOn` web agent B752/B421, Tridium CSRF
B58 vs our guard B763, JSON/REST B16/B361-364/B604/B509) so a builder reaches the Tridium exemplar, not just our
modules — WITHOUT re-deriving. Plus the §791.3 divergence notes (2 deliberate, 1 punch-list). NO new authoring content
is owed — the exemplars already exist in the corpus.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Web-tier aspects a–e are fully covered by existing blocks (servlet/hx/rc-ORD/@AgentOn/CSRF) | [CERT-corpus] | B29/B58/B433/B421/B752/B5/B48 (existence + on-topic verified via CATALOG) |
| 2 | Aspect f (JSON/REST) is mostly covered (report B361-364, analytics B16/B66, nss B604, obix B509); residue thin | [CERT-corpus] | those blocks exist + on-topic |
| 3 | No candidate module (analytics-ux/jsonToolkit/report/webEditors/nss/hx) adds a web-tier exemplar beyond existing blocks; tunnel is not web-tier | [CERT/INFER] | ranked residue check; [INFER] on "adds nothing" |
| 4 | Verdict THIN → 0 new investigation blocks (no padding) | [INFER] | §791.2 (composition of rows) |
| 5 | DashboardPan-ux diverges: RBAC-seam collapsed (punch-list) + hand-rolled CSRF (deliberate, stronger) | [CERT] | B763 §763.3/§763.4; B752 §752.5 |

**Tally**: 2 [CERT-corpus], 1 [CERT], 2 [CERT/INFER or INFER]. No unmarked claims. Cited coverage blocks verified to
exist + be on-topic this session; DashboardPan divergences are [CERT] from B763.

## Connections
- The whole web-tier corpus: **B29/B9/B58/B74** (web tier + servlets + CSRF), **B433/B293** (hx), **B421** (webEditors
  @AgentOn), **B752/B762/B763** (ux serving + our servlet — the contrast the kit already draws), **B5/B12** (module://
  ORDs), **B16/B66/B361-B364/B604/B509** (JSON/REST/XML response exemplars). This block is the INDEX over them.

## Open gaps
- None investigable — the thread is closed by the audit. (An optional future `types/dashboard.md` pointer table is a
  kit-doc task, not research.)

## Kit implication (→ `types/dashboard.md`)
Add a pointer table (web-tier aspect → exemplar block) + the DashboardPan divergence notes (§791.3). Do NOT open new
web-tier research blocks — the mechanism and the exemplars are already documented; the value is the INDEX + the
divergence punch-list, not new investigation. Thread STOPPED after this audit block, no padding.
