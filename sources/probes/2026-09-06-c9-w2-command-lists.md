# C9 W2 — exact command lists (execute-only): PR4 / PR5 / PR7 (tunnel) and PR6 / PR6b (client)

Author: companero (Fable), 2026-09-06. Every path verified on this machine today. `export PATH=/usr/bin:/bin:$PATH` first in
any loop (PATH gets mangled otherwise). Never commit on a `qa/*` branch (QA owns them). No Co-Authored-By trailers.
`[ev: tunnel clone @ 872c64c main, 9acb47c base; REDs e7e6615 / 0a14df8 local]` `[ev: client worktrees list 2026-09-06]`
`[ev: kit build-verify.md:108 run-pure-test.sh]`

## A. Tunnel repo — PR4 (config login), PR5 (audit schema + spool), PR7 (mirror)
```bash
export PATH=/usr/bin:/bin:$PATH
T=/home/cristian/tunnel/clientes/Leon-Guanajuato/Pancaddia
mkdir -p "$T/../Pancaddia-worktrees"                                   # sibling dir does not exist yet
git -C "$T" fetch -q origin
# one worktree per PR, base 9acb47c (the proposal base; main is 872c64c = 9acb47c + ONE SQL migration, NOT docs: `instalacion/pipeline/sql/2026-09-06-revoke-sessions-on-password-change.sql` (+31, a trigger on `auth.users` revoking all Supabase sessions on password change) — verified `git log --oneline 9acb47c..origin/main` 2026-09-06; PR4 note: write-server config tokens are server-held and NOT revoked by that trigger — drop them for the email on password change or rely on the short TTL)
git -C "$T" worktree add -b feat/c9-s12-config-login ../Pancaddia-worktrees/c9-s12-config-login 9acb47c
git -C "$T" worktree add -b feat/c9-s12-audit-schema ../Pancaddia-worktrees/c9-s12-audit-schema 9acb47c
git -C "$T" worktree add -b feat/c9-s12-audit-mirror ../Pancaddia-worktrees/c9-s12-audit-mirror 9acb47c
# RED test files (branches are LOCAL in this clone: qa/c9-s12-write-server e7e6615, qa/c9-s12-audit-mirror 0a14df8) — checkout the test file only, never cherry-pick the branch
git -C "$T/../Pancaddia-worktrees/c9-s12-config-login" checkout e7e6615 -- instalacion/pipeline/test/write-server.config-login.test.mjs
git -C "$T/../Pancaddia-worktrees/c9-s12-audit-schema" checkout e7e6615 -- instalacion/pipeline/test/write-server.config-login.test.mjs
git -C "$T/../Pancaddia-worktrees/c9-s12-audit-mirror" checkout 0a14df8 -- instalacion/pipeline/test/audit-mirror.test.mjs
```
PR4 and PR5 share ONE RED file (S12A-1/2/3/5 = PR4; S12A-4/6/7/8/9 = PR5) and one seam `buildServer(cfg, deps)` — whichever
lands first carries the seam; the second rebases onto it. `main` has NO `test/` dir and NO `test` script today.

### Run the tests (Node ≥ 18 built-in runner, no npm deps)
```bash
cd "$T/../Pancaddia-worktrees/<wt>/instalacion/pipeline"
node --version                                 # >= 18
node --test test/                              # RED: fails to import (buildServer/runMirror absent) — that is the RED
# add to package.json "scripts": { "test": "node --test test/" }  (F5 of the R5 package) then: npm test
node --test test/write-server.config-login.test.mjs    # single file
```
Expected GREEN counts: PR4+PR5 file 9/9 (S12A-1..9); PR7 5/5 (MIR1-5); all 14 once both land.

### config.env keys the GREEN reads (file `instalacion/pipeline/config.env`, NOT committed; `config.example.env` is)
Today's keys (`loadConfig` @ 9acb47c :24-58): `OBIX_BASE OBIX_USER OBIX_PASS SUPABASE_URL SUPABASE_SERVICE_KEY FACADE_PATH
WRITE_PORT ALLOWED_ORIGIN(S) ALLOWED_EMAILS JWKS_URL/SUPABASE_JWKS_URL JWT_ISS/SUPABASE_ISS RATE_MAX`. NEW (add to
`config.example.env` with comments, real values only in `config.env` on the mini-PC):
| Key | PR | Default / meaning |
|---|---|---|
| `CONFIG_PASSWORD` | PR4 | the ONE shared step-up password (D-1); no default — server refuses `/config/login` (401) when unset |
| `CONFIG_TTL_MS` | PR4 | sliding TTL for the config token, default 600000 (viewer 10 min); clock-injected in tests |
| `AUDIT_SPOOL` | PR5 | path of the JSON-lines FAILURE spool, e.g. `/var/lib/pancaddia/audit-spool.jsonl`; tests pass a temp file |
| `MIRROR_ENABLED` | PR7 | absent/false = OFF (never reads AuditHistory); `true` only after the B829-live gate |
| `MIRROR_STATE` | PR7 | high-water file for the mirror (last ts), e.g. `/var/lib/pancaddia/mirror-state.json` |
Tests never read `config.env`: they pass `cfg` objects (`WRITE_PORT:0`, temp `AUDIT_SPOOL`, etc.).

### SQL migrations — apply / rollback WITHOUT touching production Supabase
Files: existing `sql/2026-09-06-change-log-audit.sql` (creates `public.change_log`); NEW PR5 `sql/2026-09-06-change-log-extended.sql`
(additive: `+config_session, +result, +surface, +client_ip`); NEW PR7 `sql/2026-09-06-change-log-mirror-index.sql` (partial unique
index on the 5-tuple where `surface='servlet'`). All must be `add column if not exists` / `create index if not exists` (idempotent).
```bash
# local Postgres in Docker (no Supabase involved); Supabase's Postgres is 15 — match it
docker run -d --name c9pg -e POSTGRES_PASSWORD=pg -p 55432:5432 postgres:15
export PGURL=postgresql://postgres:pg@127.0.0.1:55432/postgres
psql "$PGURL" -v ON_ERROR_STOP=1 -f sql/2026-09-06-change-log-audit.sql            # baseline
psql "$PGURL" -v ON_ERROR_STOP=1 -f sql/2026-09-06-change-log-extended.sql         # PR5
psql "$PGURL" -v ON_ERROR_STOP=1 -f sql/2026-09-06-change-log-extended.sql         # idempotency: must succeed a 2nd time
psql "$PGURL" -c '\d public.change_log'                                             # columns present
psql "$PGURL" -v ON_ERROR_STOP=1 -f sql/2026-09-06-change-log-mirror-index.sql     # PR7
# dry-run form (no commit): wrap in a transaction and roll back
psql "$PGURL" -v ON_ERROR_STOP=1 -c 'begin;' -f sql/2026-09-06-change-log-extended.sql -c 'rollback;'
# rollback scripts (write them next to each migration as *-down.sql; additive columns only):
#   alter table public.change_log drop column if exists config_session, drop column if exists result, drop column if exists surface, drop column if exists client_ip;
#   drop index if exists change_log_dedupe_idx;
docker rm -f c9pg
```
Production apply is a SEPARATE human step (Supabase SQL editor or `psql` with the project's connection string): never from a PR
gate; PR acceptance = the Docker run above + the idempotency re-run.

### Per-PR checklist
| PR | Worktree | GREEN pins | Also |
|---|---|---|---|
| PR4 | c9-s12-config-login | S12A-1/2/3/5 (+ seam) | `config.example.env` keys; README §config-login |
| PR5 | c9-s12-audit-schema | S12A-4/6/7/8/9 | SQL extended + Docker proof; spool path; `replaySpool` cron line |
| PR7 | c9-s12-audit-mirror | MIR1-5 | `audit-mirror.mjs`; SQL index; flag OFF; kit doc line via PR12 |
Commit inside the worktree on its `feat/*` branch; push; PR against `main`.

## B. Client repo — PR6 (servlet guards) and PR6b / R14 (HMI config login)
```bash
export PATH=/usr/bin:/bin:$PATH
C=/home/cristian/modulos_niagara_n4/Cliente/Leon-Guanjuato        # main clone (HEAD 4f5f1c7 — STALE; never read from it)
KIT=/home/cristian/modulos_niagara_n4/niagara-tools/build-n4-module-kit
# PR6: new worktree on a109249 (the RED worktree c9-s12-servlet sits on qa/c9-s12-servlet — do not commit there)
git -C "$C" worktree add -b feat/c9-s12-servlet-guards ../Leon-Guanjuato-worktrees/pr6-servlet-guards a109249
W="$C/../Leon-Guanjuato-worktrees/pr6-servlet-guards"
git -C "$W" checkout 4c18837 -- Dashboard/DashboardPan/DashboardPan-ux/srcTest/test/com/angeles/DashboardPan/ux/DashboardWriteGuardsTest.java
# PR6b / R14: branch from PR6's tip once PR6 is pushed (NOT from a109249)
git -C "$C" worktree add -b feat/c9-s12-hmi-config-login ../Leon-Guanjuato-worktrees/pr6b-hmi-config-login feat/c9-s12-servlet-guards
git -C "$C/../Leon-Guanjuato-worktrees/pr6b-hmi-config-login" checkout cc1c948 -- Dashboard/DashboardPan/DashboardPan-ux/srcTest/test/com/angeles/DashboardPan/ux/ConfigLoginWiringTest.java Dashboard/DashboardPan/DashboardPan-ux/srcTest/test/com/angeles/DashboardPan/ux/ConfigLoginGuardTest.java   # BOTH RED files at cc1c948 (verified git diff --name-only a109249..cc1c948)   # confirm the RED file list with `git diff --name-only a109249 cc1c948`
```
### Pure JUnit in WSL (the ONLY executable coverage here; `niagaraTest` discovers 0 tests from WSL)
```bash
cd "$W"
# fetch the junit/hamcrest jars into ~/.gradle once (any gradle build does it): cd Dashboard && ./gradlew :DashboardPan-ux:compileJava
"$KIT/toolbelt/run-pure-test.sh" Dashboard/DashboardPan/DashboardPan-ux com.angeles.DashboardPan.ux.DashboardWriteGuardsTest
#   exit 0 = green; exit 3 = jar cache empty (run one gradle build first). RED today: "cannot find symbol DashboardWriteGuards".
"$KIT/toolbelt/run-pure-test.sh" Dashboard/DashboardPan/DashboardPan-ux com.angeles.DashboardPan.ux.DashboardDispatchTest   # existing, must stay green
# PR6b: ConfigLoginWiringTest is STRUCTURAL (reads the source) — same runner, PureClass = the wiring class it names (see R14 package §2)
```
### Build + gates (gradle root = `Dashboard/`, where `gradlew` lives)
```bash
cd "$W/Dashboard"
./gradlew clean slotomatic jar                    # the only correct build (BUILD-LOOP §4); or: "$KIT/toolbelt/build.sh" from the module root
"$KIT/toolbelt/verify-module.sh" DashboardPan/DashboardPan-ux/build/libs/*.jar        # THE gate, on the built jars
"$KIT/toolbelt/schema-risk.sh" DashboardPan/DashboardPan-ux/src                       # expect SAFE (no slot change)
"$KIT/toolbelt/lint-servlet.sh" DashboardPan/DashboardPan-ux/src                      # WARN-only; record the count (parseDouble catch stays)
"$KIT/toolbelt/lint-structure.sh" DashboardPan/DashboardPan-ux                        # L7
sed -n 33p build.gradle.kts                                                           # defaultModuleVersion("2.2.0") after PR6 (today 2.1.1)
# deploy is NOT a PR step: scripts/ng-deploy.sh --strict-slotomatic runs from this dir when Cristian says (station backup first)
```
Order: PR6 green → push → PR6b worktree from its tip → R14 green (CLW1-5 + SC13 fragment-merge on `:33`, same 2.2.0 value).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | tunnel paths, local RED branches/SHAs, no test dir/script on main | [CERT] | `ls`, `git branch -a`, package.json @ 872c64c |
| 2 | RED file names | [CERT] | `git diff --name-only 9acb47c e7e6615 / 0a14df8`; `a109249 4c18837` |
| 3 | today's config keys | [CERT] | loadConfig @ 9acb47c :24-58 |
| 4 | new keys (names) | [CERT for CONFIG_PASSWORD/AUDIT_SPOOL/MIRROR_ENABLED, INFER for CONFIG_TTL_MS/MIRROR_STATE names] | R5/PR7 packages |
| 5 | run-pure-test.sh args = TWO (`<module-rt-dir> <pkg.TestClass>`), niagaraTest 0 tests from WSL, gradle root rule | [CERT] | `toolbelt/run-pure-test.sh:11-13,:26` (corrected from 4 args), BUILD-LOOP.md:58,:80 |
| 6 | Docker/psql flow | [INFER, standard] | Supabase = Postgres 15; verify docker is installed on the apply machine |
| 7 | group gradle :33 = 2.1.1 | [CERT] | a109249 worktree |
