# "Before C11" checklist — for Cristian (deploy chain + harness prerequisites)

Author: companero (Fable), 2026-09-06. One page: what must happen between the C9 close (kit v0.20.0, client `ff1b659`,
tunnel PR4/5/7 blessed) and any C10 CLIENT jar. The KIT lint-precision lane (S21/S22/S23) needs NONE of this and can start
now. Post-C10 state: kit v0.21.0 (main dab0807); client main 00e7118 (post-PR6 hygiene); read trees main-ff1b659 (frozen, pre-PR6) + main-00e7118 (post-PR6). Client group versions UNCHANGED: Compresores 2.2.0 / Paccadia 2.1.0 / Dashboard 2.2.0 (C10 added no client jar). `[ev: 2026-09-06-c9-deploy-runbook-delta.md]` `[ev: C9 harness-only pins]`

## A. Tunnel merge (mini-PC write-server) — Cristian's OK
- Merge tunnel PR#1 (config login), PR#2 (audit schema + spool), PR#3 (AuditHistory mirror). Set `config.env`:
  `CONFIG_PASSWORD` (shared step-up), `CONFIG_TTL_MS=600000`, `AUDIT_SPOOL=<path>`, `MIRROR_ENABLED=false` (on only after
  the B829-live gate), `MIRROR_STATE=<path>`. Apply the SQL migrations to Supabase (SQL editor; idempotent `if not exists`).
- PG15 route for the migration dry-run (this WSL has no Docker/PG server): pick Docker Desktop WSL integration, OR
  `brew install postgresql@15`, OR `supabase start` (see `2026-09-06-c9-w2-command-lists.md` §PR5 pre-stage).

## B. Client jar deploy chain (PANCCADIA) — the runbook delta, in order
Deploy the pending base (Paccadia 2.0.7 / Compresores 2.0.3 / Dashboard 2.1.1) FIRST, then the C9 bumps IN ORDER:
1. Compresores 2.1.0 (PR1 rotation) → 2. Paccadia 2.1.0 (PR8 CR-3 alarm) → 3. Compresores 2.2.0 (PR9 CP-1 alarm) →
4. Dashboard 2.2.0 (PR6/PR6b guards + config login). Each: station-snapshot → schema-risk SAFE → hot reload → §6.a verify
(triage-console 0 own-module SEVERE, bog-audit CHECK5/7 0). Rollback is CONFIG only (rotationInterval=0, MIRROR_ENABLED=false,
alarm-ext off) — NEVER a jar downgrade (additive slots → downgrade is a schema-risk OUTAGE). Full steps in the runbook delta.

## C. niagaraTest harness session (Windows) — closes the C9 gate's 14th item + unblocks C10 alarm work
- Run the C9 harness-only pins on the Windows `niagaraTest`: CRA1/2/3-live (CR-3 freeze alarm routes a `BAlarmRecord
  sourceState=offnormal`, clears on recovery), CPB5 (CP-1 alarm sourceState=offnormal). A SKIP is not a pass — record the
  real result. This is the C9 close gate's pending 13/14 → 14/14.
- C10 P2/P3/P4 (per-operator login, airDefrost, intercambiador) inherit the same station/harness dependency — their REDs
  stay structural + SKIP in WSL until this session runs.

## D. Cristian's three station answers (unblock C10 product lanes)
1. **Defrost trial** (rooms 1/2/4) — the green light for the `airDefrost` flag work (P3) + the defrost trial link-list.
2. **Intercambiador Cuarto 3** — is it on a Niagara output? YES → create the control point + link (P4); NO → drop the HMI
   control (dead-panel-writes issue exit 2).
3. **coolOnSensorFault link** — approve the station-side link `CuartoN → ColdRoom_N.coolOnSensorFault` (P5), on the
   station-change list with the defrost trial.

## E. What can start NOW without any of the above
The KIT parser-consolidation wave: T1 (shared method-boundary parser, peak-depth), T3 (concept-drift), T2 (client-root), T4 (guard meta-check). All WSL-only. See `2026-09-07-c10-lint-refinement-apply-packages.md`.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | deploy order + config-only rollback | [CERT] | deploy runbook delta |
| 2 | harness closes the 14th gate item; C10 alarm work inherits it | [CERT] | C9 close gate 13/14; harness-only pins |
| 3 | three station answers gate P3/P4/P5 | [CERT] | dead-panel-writes issue; defrost link-list |
| 4 | KIT wave needs none of A-D | [CERT] | lint-refinement package (WSL-only) |
