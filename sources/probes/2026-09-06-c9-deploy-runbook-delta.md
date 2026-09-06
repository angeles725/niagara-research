# C9 deploy runbook DELTA — client jars + mini-PC, layered on the pending 2.0.7/2.0.3/2.1.1 deploy

Author: companero (Fable), 2026-09-06. This is a DELTA on the client's pending deploy runbook (client PR #9), which ships
the CURRENT group versions **Paccadia 2.0.7 · Compresores 2.0.3 · Dashboard 2.1.1** (confirmed at the a109249 worktree).
It does NOT restate the base runbook; it adds the C9 version bumps, the per-module gate (BUILD-LOOP §6/§6.a), the config-
driven rollback levers (never a jar downgrade), and the mini-PC write-server step. `[ev: group build.gradle.kts:33 @ a109249 / e5bee1c]`
`[ev: kit BUILD-LOOP §6, §6.a]` `[ev: proposal f610d21 PR table]`

## 1. Version map (group `build.gradle.kts:33` `defaultModuleVersion`)
| Group | Base (pending PR #9) | C9 target | Bumped by | Schema-risk |
|---|---|---|---|---|
| Compresores (CompPan-rt) | 2.0.3 | **2.1.0** then **2.2.0** | 2.1.0 = PR1 (rotation slots), 2.2.0 = PR9 (alarm ext) | additive slots only → SAFE (confirm with schema-risk.sh) |
| Paccadia (ColdRoomPan-rt) | 2.0.7 | **2.1.0** | PR8 (freezeAlarmPt + ext) | additive child point → SAFE |
| Dashboard (DashboardPan -rt/-ux) | 2.1.1 | **2.2.0** | PR6 (guards seam) + PR6b/R14 (config login) | no slot change → SAFE (DashboardWriteGuards is not a BComponent) |
Every bump is MINOR/additive — `schema-risk.sh <pre> <post>` MUST read **SAFE** before any station reload (a LOSSY/OUTAGE
retype crashed PANCCADIA once — BUILD-LOOP §6). No downgrade path: rollback is config, §4.

## 2. Ordered deploy sequence (one station, PANCCADIA)
Deploy the pending PR #9 jars FIRST (base runbook), then layer C9 in dependency order:
1. **Compresores 2.1.0** (PR1 rotation) — rotation OFF by default (`rotationInterval=0`), so no behaviour change until commissioned.
2. **Paccadia 2.1.0** (PR8 CR-3 alarm) — adds `freezeAlarmPt`; closes the R3 CR-3 silent-protection WARN.
3. **Compresores 2.2.0** (PR9 CP-1 alarm) — on TOP of 2.1.0 (fragment-merge on the version line).
4. **Dashboard 2.2.0** (PR6 + PR6b/R14) — servlet guards + in-HMI config login; deploy LAST (the write path depends on nothing new server-side).
Each module, in order: pre-snapshot → schema-risk SAFE → hot reload (or station restart) → §6.a post-deploy verification.

## 3. Per-module gate (BUILD-LOOP §6 / §6.a — run for EACH bump)
```bash
export PATH=/usr/bin:/bin:$PATH; K=/home/cristian/modulos_niagara_n4/niagara-tools/build-n4-module-kit
S=/mnt/c/Users/equipo/Niagara4.14/OptimizerSupervisor/stations/PANCCADIA
"$K/toolbelt/station-snapshot.sh" "$S" ./snap-pre-<module>          # baseline (config.bog + console + db pointers + sha256)
"$K/toolbelt/schema-risk.sh" ./snap-pre-<module>/... <post-build-src>  # MUST be SAFE; LOSSY/OUTAGE aborts the deploy
# … build+sign+copy the jar via scripts/ng-deploy.sh --strict-slotomatic from the group gradle root (Compresores/ | Paccadia/ | Dashboard/) …
# hot module reload OR station restart, then §6.a:
"$K/toolbelt/station-snapshot.sh" "$S" ./snap-post-<module>
"$K/toolbelt/schema-risk.sh" ./snap-pre-<module> ./snap-post-<module>   # before vs after = SAFE (bog still loads)
"$K/toolbelt/triage-console.sh" --console-dir "$S" "$S"/console*.txt     # own-module SEVERE/WARN after reload = 0
"$K/toolbelt/bog-audit.sh" "$S/config.bog" --module <Module> --source-dir <src>  # CHECK5/7 = 0 (no ghost/dangling slot)
```
Alarm modules (PR8/PR9): the ext ROUTING is station-only (harness cannot prove it in WSL) — after reload, force one trip on
the bench and confirm one `BAlarmRecord sourceState=offnormal` in the AlarmService, and that it clears on recovery.

## 4. Rollback levers — CONFIG, never a jar downgrade (a downgrade retype is the OUTAGE class)
| Symptom | Lever | Where |
|---|---|---|
| rotation misbehaving (PR1) | `rotationInterval = 0` (disarms; arm silently off) | CompPan config slot, Workbench |
| audit mirror noisy/failing (PR7) | `MIRROR_ENABLED=false` (never reads AuditHistory) | mini-PC `config.env` |
| alarm ext chattering (PR8/PR9) | disable the `BAlarmSourceExt` (set its `alarmEnable`/`ackRequired` off, or `faultReset`) | station, per unit |
| config login locking out writes (PR6b/R14) | keep the write-server config token TTL short / hand out the shared `CONFIG_PASSWORD` | mini-PC + operator |
A jar rollback to the pending version is NOT a lever: the C9 slots are additive, so the new bog has slots the old jar
doesn't declare → reload against the old registry is a schema-risk OUTAGE. Roll behaviour back by config; keep the jar.

## 5. Mini-PC (write-server) — PR4 + PR5 + PR7 (separate from the station jars)
The write-server runs on the mini-PC, not the JACE. Deploy the new `write-server.mjs` (+ `audit-mirror.mjs`) and the SQL
migrations (§ W2 command list), then set `config.env` keys:
| Key | PR | Value |
|---|---|---|
| `CONFIG_PASSWORD` | PR4 | the shared step-up password (no default; `/config/login` 401s when unset) |
| `CONFIG_TTL_MS` | PR4 | 600000 (10 min sliding) |
| `AUDIT_SPOOL` | PR5 | JSON-lines failure-spool path, e.g. `/var/lib/pancaddia/audit-spool.jsonl` |
| `MIRROR_ENABLED` | PR7 | `false` at first cutover (turn on only after the B829-live gate) |
| `MIRROR_STATE` | PR7 | high-water file, e.g. `/var/lib/pancaddia/mirror-state.json` |
SQL migrations (`change-log-extended.sql`, `change-log-mirror-index.sql`) apply to Supabase as a SEPARATE human step
(Supabase SQL editor); idempotent (`if not exists`). Restart the write-server; confirm `/config/login` → 200 and one
`change_log` row per authorized write.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | base 2.0.3/2.0.7/2.1.1; C9 targets 2.1.0/2.2.0 · 2.1.0 · 2.2.0 | [CERT] | build.gradle.kts:33 @ a109249 and PR1 tip e5bee1c |
| 2 | additive-only → SAFE; downgrade = OUTAGE | [CERT] | BUILD-LOOP §6; corpus B795/B800 |
| 3 | §6.a gate steps | [CERT] | BUILD-LOOP §6.a |
| 4 | write-server config.env keys | [CERT for names] | W2 command list / R5/PR7 packages |
| 5 | exact ng-deploy invocation per group | [INFER] | confirm the group gradle root at deploy time |
