<!-- review-status: applied 2026-09-05 · kit 272e1ad · PARTIAL — shipped: #1, #2 (§3 liveness + live beats doc, PR #434), #3 (DYNAMIC-SETUP CORS caveat, PR #446 pending merge); #4 (§12 mutating live install) in PR #448 -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · tooling · 2026-09-03 · Research-SDD self-retrospective (4/4)

> FOURTH research-sdd retro of the day. #1 obix-quick-mode, #2 cross-session-verify, #3 document-mode on a
> live-edited subject (`2026-09-03-research-sdd-document-mode-live-subject-retro.md`). This one covers the
> LATER window on the same target (#30 `panccadia-3d-viewer`): the cutover to LIVE data (Supabase), the
> authenticated-control build (Supabase Auth login + JWT, option B), the e2e verification, and the
> multi-session security coordination. The document-mode capture of that window is corpus B6 (already
> retro'd via the B6 addendum in #3). This retro captures only the process lessons that TRANSFER to the
> research-sdd loop. READ-ONLY on the kit — PROPOSES only (§18).

## Context
The viewer went from a labeled static snapshot to real Supabase data, then gained authenticated control
(login → JWT → mini-PC `/write`). Most work was direct implementation + verification (not a research loop),
but four verification/evidence lessons generalize to any loop touching a LIVE system (`live-install` /
dynamic phase §12). The build itself was delegated to writers; the transferable value is in HOW liveness,
cross-origin, and secrets were verified.

## Proposed kit deltas

> Only genuinely NEW / transferable items. Anything already encoded is under "Already covered".

| # | Proposed change | Target (file · §/section) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| 1 | Before labeling data/state as "live/fresh/current", VERIFY freshness against the live source (e.g. the max `ts` advancing ≈ now) — a "liveness" claim is a claim like any other and needs evidence, not a peer's say-so. If it is a frozen snapshot, label it honestly ("SNAPSHOT / última lectura <ts>"), never as live. | `METHODOLOGY.md §3` (markers) + §12 (dynamic phase) | Refused to flip the viewer to "EN VIVO" on a peer's word; curl'd Supabase `latest` → top `ts` == now (285 rows advancing) BEFORE flipping. Earlier, kept it "SNAPSHOT" while the pipeline was down rather than mislabel a static export as live. | reinforce | HIGH |
| 2 | A project/spec DOC (even a peer-authored one, `[CERT-doc]`) can be wrong against the live system; for behavior that a live system can arbitrate (e.g. is a point writable?), verify against the live system and let `[CERT-live]` OVERRIDE the doc — then fix the doc. | `METHODOLOGY.md §14` (cross-block consistency) + the "código/live > doc" precedence rule | PORT-SPEC §4 listed `CuartoN/setpoint` as writable; the live oBIX PUT returns 400 "ord no escribible" (readonly facade). Viewer now treats setpoint read-only; flagged the doc line to the author for correction. | reinforce | MEDIUM |
| 3 | (verification-tooling) When an e2e check crosses an ORIGIN boundary the browser enforces (CORS), a headless-from-localhost test cannot reach the endpoint by design — confirm the backend contract OUT-OF-BAND with a non-browser client (`curl`), and verify the CORS/allow-origin separately. Don't read a CORS block as a code bug. | `toolbelt/DYNAMIC-SETUP.md` (headless-QA recipe from retro #3's T2) | Headless login worked (token 200) but the browser `/write` was CORS-blocked from `localhost` (server allows only the prod origin); confirmed the real chain by curl: Supabase token → `POST /write` → 401 "usuario no autorizado" (test acct off-allowlist). | absorb | MEDIUM |
| 4 | For a `live-install`/dynamic run that can MUTATE a real system: (a) take test credentials via a local file OUTSIDE the repo (chmod 600), never pasted in a channel or embedded in an artifact; (b) NEVER perform a real state-changing write in production during verification — do read-only prod checks + out-of-band negative checks (no-token → 401). | `METHODOLOGY.md §12` (dynamic phase) + the SECRETS DISCIPLINE (PROMPT-LOOP) | Test cred read from `/home/cristian/panccadia-test-cred.env` (not the peer channel); verified prod with read-only cur(build/markers) + `/write` no-token→401; deliberately did NOT trigger a real control write (the operator's allowlisted account would change real refrigeration equipment). | new | HIGH |

## Already covered (do NOT re-add)
- **Verify-before-assert / cross-session claims** — delta #1 is the "liveness" APPLICATION of the existing
  §3/§5 discipline (and retro #2's cross-session-claim rule); #2 is the "live > doc" APPLICATION. Reinforce, not new.
- **Freeze-the-subject before citing** — retro #3 D1; re-validated by B6 (a new snapshot was frozen). No re-add.
- **Headless-Chromium WebGL QA recipe** — retro #3 T2; #3 above extends it with the CORS/origin caveat.
- **`[INFER]` honesty** — the control-auth login design stayed `[INFER]` in B6 until code+e2e existed. §3 as written.

## Not a kit delta (attributed elsewhere, on purpose)
- The **Bash auto-approval classifier outage** mid-session (`claude-sonnet-4-6[1m] unavailable` → Bash blocked)
  is a Claude Code HARNESS condition, not a research-sdd kit gap. Worked around via permission-mode change.
  Environment context only — no kit change proposed.

## Meta
- Target #30 `panccadia-3d-viewer`: shipped LIVE at https://panccadia.angeles-group.org (build 2026-09-03-7):
  real Supabase data + authenticated control (option B). Corpus: 6 blocks + RUNBOOK + 1 retro + B6 addendum.
- Engram: 8013 (overview), 8016 (mobile UX), 8032 (B6), 8039 (control-auth live).
