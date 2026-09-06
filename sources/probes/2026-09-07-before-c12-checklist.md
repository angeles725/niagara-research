# "Before C12" checklist — for Cristian (unchanged prerequisites + post-C11 state)

Author: companero (Fable), 2026-09-06. What must happen before any C12 CLIENT jar; the KIT lanes (S1-S4) need none.
Post-C11 state: kit **v0.22.0** (main `e55b369`, tag `66123a2`); client main `00e7118`; read trees `main-ff1b659` (frozen)
+ `main-00e7118`. Client group versions UNCHANGED: Compresores **2.2.0** / Paccadia **2.1.0** / Dashboard **2.2.0** (C11 is
kit-only, no client jar). `[ev: kit v0.22.0]` `[ev: C11 close]`

## A. Tunnel merge (mini-PC write-server) — Cristian's OK
Merge tunnel PR#1 (config login), #2 (audit schema+spool), #3 (mirror); set config.env (CONFIG_PASSWORD, CONFIG_TTL_MS=600000,
AUDIT_SPOOL, MIRROR_ENABLED=false, MIRROR_STATE); apply the SQL migrations (idempotent); pick the PG15 route for the dry-run.

## B. Client jar deploy chain (PANCCADIA) — the runbook delta, in order
Base (Paccadia 2.0.7 / Compresores 2.0.3 / Dashboard 2.1.1) then the C9 bumps (Compresores 2.1.0 PR1 → 2.2.0 PR9,
Paccadia 2.1.0 PR8, Dashboard 2.2.0 PR6/PR6b), each with station-snapshot → schema-risk SAFE → reload → §6.a verify;
rollback is CONFIG only (rotationInterval=0, MIRROR_ENABLED=false, alarm-ext off) — never a jar downgrade.

## C. niagaraTest harness session (Windows)
Run the C9 harness-only alarm pins (CRA1/2/3-live, CPB5) once; a SKIP is not a pass. C12 alarm/adapter work inherits it.

## D. Cristian's three station answers (unblock C12 product lanes)
1. Defrost trial (rooms 1/2/4) — the airDefrost green light.
2. Intercambiador Cuarto 3 — on a Niagara output? YES → create+link (P4); NO → drop the HMI control.
3. coolOnSensorFault link — approve `CuartoN → ColdRoom_N.coolOnSensorFault` (P5).

## E. What starts NOW without A-D
The KIT fragment-hardening pair (S1 @-stop param-annotation FN + S2 initializer reachability) and S3/S4 — all WSL-only.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | kit v0.22.0 e55b369; client 00e7118; versions unchanged | [CERT] | git @ kit/client main |
| 2 | prerequisites unchanged from C11 | [CERT] | C11 before-checklist |
| 3 | KIT S1-S4 need none of A-D | [CERT] | C12 seeds (WSL-only) |
