# C10 product seed — HMI per-operator login: options for the PANCCADIA kiosk (one-page comparison for Cristian)

investigador1, 2026-09-06. Grounded in B830 (servlet re-auth path), B829 (audit gate), R14 (in-module config login, SHIPPED
in C9 as client PR#12-#13 / DashboardPan 2.2.0). Question: should each operator log into the PANCCADIA kiosk as themselves
instead of the one shared kiosk login? `[ev: corpus B830]` `[ev: corpus B829]` `[ev: C9 R14 shipped]`

## The need, stated precisely
Two different things are often conflated:
1. **Attribution** — every setpoint/HOA write is recorded against the REAL operator (who changed what, when).
2. **Per-operator VIEW/RBAC** — different operators see or can reach different screens/controls on the panel.

**R14 (already shipped) fully solves #1** and does NOT touch #2. So the decision is only: does Cristian also need #2?

## The three options
| # | Option | What it is | Attribution (#1) | Per-operator view (#2) | Cost | Risk |
|---|---|---|---|---|---|---|
| **A** | **R14 second login (SHIPPED)** — keep the one shared kiosk login; a "modo configuración" in DashboardPan re-auths the operator (station username+password) before any write; the write runs with THAT operator's `BUser` as Context so `/PANCCADIA/AuditHistory` names them (B829-G2/B830) | one shared kiosk account, per-write step-up | **YES — the real operator** (AuditEvent `userName` = the re-authed user, B830 §830.4) | NO — everyone sees the same dashboard | **ZERO more** (done in C9) | none new; the config token is short-TTL, revoked on logout; the C9 finding (ConfigSession HashMap) is being synchronized |
| **B** | **Per-operator Workbench user + kiosk auto-login per operator** — each operator gets a station account; the kiosk browser is configured to auto-login as that operator | per-browser-session station account | YES natively (the session user IS the operator) — but only if the kiosk is NOT shared | possible (Niagara category/role RBAC per user) | **HIGH** — one kiosk browser can only auto-login as ONE user; per-operator means either one kiosk per operator (hardware) or a login screen at the panel (defeats "kiosk") | shared-terminal problem: whoever the kiosk is logged in as, ALL writes attribute to them until logout; an operator walking away leaves the panel writable as them |
| **C** | **Panel login screen (no auto-login)** — the kiosk shows a Niagara login; each operator logs in with their own station account for the whole session | per-session station account | YES for the session (until logout/auto-logoff) | YES (per-user RBAC) | MED — a full login/logout cycle per operator at the touch panel; relies on auto-logoff (default 15 min) to avoid a stuck session | the same walk-away risk as B, bounded by auto-logoff; more friction than R14's per-write step-up |

## Recommendation
**If the need is attribution (#1) only — which is what "who changed the setpoint" asks — R14 already covers it; do nothing.**
R14's per-write step-up is actually SAFER than B/C for a shared touch panel: it re-auths at the moment of the write (not
for a whole session), so a walk-away cannot leave the panel writable as the last operator (the config token expires on
idle / logout, and each write re-checks the session). B and C reintroduce the shared-terminal walk-away risk that R14 was
designed to avoid.

**Consider B/C only if Cristian also needs per-operator VIEW/RBAC (#2)** — e.g., a junior operator who may see but not reach
the compressor staging config. That is a NEW requirement (not "who changed X") and would layer on top of R14, not replace
it: the kiosk session RBAC gates what's VISIBLE, R14 still gates and attributes the WRITE. A C10 seed if he wants it:
"per-operator panel RBAC (Niagara category/role) layered under the R14 write step-up."

## Open question for Cristian (one)
Is the requirement "record who changed each value" (→ R14 already done, nothing to build) OR "different operators get
different screens/permissions on the panel" (→ new per-operator RBAC work, option B/C under R14)?
`[ev: corpus B830 §830.4/§830.5]` `[ev: corpus B829]`
