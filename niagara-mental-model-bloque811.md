# B811 · Station snapshot automation — how to copy a running station (bog, logs, logic, links) to inspect it, without mounting the filesystem `[CERT]`

> Answers the operator's request ("once the module is loaded, take a copy of the station to inspect logs, logic,
> links"): what a station BACKUP captures (and critically what it does NOT), how to pull the console + load
> metrics REMOTELY read-only, whether Tridium's own tooling snapshots after a deploy, and the safe
> `station-snapshot.sh` remote-mode contract. `live-install` → SECRETS DISCIPLINE (a backup carries keyrings/creds;
> cite structure, never values). Ties to [Block 806] (backup load), [Block 800] (console), [Block 795]/[Block 807]
> (bog schema drift).
>
> **Sources**: decompiled `backup-rt.jar` (`javax.baja.backup.BBackupService`, `com.tridium.backup.BFoxBackupJob`
> — driver-verified by grep); niagara-help `docPlatform.txt` + guides; REMITTANCE B402/B411/B800/B806/B672-676/
> B795/B807. Markers: `[CERT]` code `file:line` (decompiled = extern) · `[CERT-doc]` doc · `[INFER]`.
>
> **Type:** `mixed` (evidence + the snapshot-tool contract). Connects [Block 806], [Block 800], [Block 795]/[Block 807]/[Block 799].

## 811.1 — Station backup: `BBackupService`, and what the `.dist` DOES and does NOT contain `[CERT]`
- **Service:** `javax.baja.backup.BBackupService` (module `backup-rt`); it registers a FOX channel id `"backup"`
  (`BBackupService.java:340` `getFoxChannelId()→"backup"`), gated by `BPermissions.adminWrite`
  (`:356 user.check(this, adminWrite)`). There is **no `backup:` ORD scheme** — only the Fox channel (open the
  station over Fox, path `ip:<IP>|fox:|station:|slot:/Services/BackupService`). `[CERT / CERT-absent]`
- **Online backup** (station running): `BFoxBackupJob.run()` → `Station.saveSync(this, 10)` (`BFoxBackupJob.java:108`
  — a FULL bog flush FIRST) → `service.zip(...)` (`:111`) streams the `.dist` ZIP. **This is heavyweight** (bog save
  + file crawl). `[CERT]`
- **What the `.dist` captures** — from the default exclude sets (`BBackupService.java:242,244`):

| Artifact | Online (`excludeFiles`) | Offline (`offlineExcludeFiles`) |
|---|---|---|
| `config.bog` (logic + links) | **YES** | **YES** |
| `*.hdb` history / `*.adb` alarm db | **NO** (excluded) + dirs `^^history`,`^^alarm` excluded (`:243`) | **YES** (offlineExcludeDirectories = NULL, `:245`) |
| **`console.*` logs** | **NO — excluded** | **NO — excluded** |
| `config_backup_*` / `*.bog.b*` | NO | NO |
| platform config, keyring (re-keyed to system passphrase), licenses, TCP/IP, module list | YES | YES |

**Headline `[CERT]`:** `console.*` is in BOTH exclude patterns → **the console log is NOT in a station backup**.
A `.dist` gives you the logic+links (`config.bog`) — an OFFLINE backup additionally gives history/alarm db — but
NEVER the console. Logs must be fetched separately (§811.2).

## 811.2 — Pulling the console + load REMOTELY, read-only `[CERT / CERT-doc]`
There is NO simple unauthenticated HTTP GET for a JACE's `console_backup_*.txt`. Read-only surfaces:

| Surface | ORD / endpoint | Gives | Perm |
|---|---|---|---|
| Fox file | `ip:<IP>\|fox:\|station:\|file:^^console_backup_<ts>.txt` | the console backup file (must know/derive the timestamp) | station adminWrite |
| Application Director "Stream To File" | platform daemon `:5011` | LIVE station stdout/stderr piped to the connecting PC | platform creds `[CERT-doc docPlatform.txt]` |
| spy | `ip:<IP>\|fox:\|spy:/sys/engineManager`(+`/hogs`), `spy:/metrics`, `spy:/platform diagnostics/log` | engine load/hogs, globalCapacity counts, platform log | superuser `[CERT B806 §806.7]` |
| oBIX | `obix:\|` | component-tree reads only (no console) | station user |

**Encoding caveat ([Block 800] §800.5):** a Spanish-locale station writes `INFORMACIÓN/ADVERTENCIA/GRAVE` with
accents as non-UTF-8 mojibake — parse latin-1/bytes, never assume UTF-8; attribute by `com.angeles.*` frame OR
the `[coldRoomPan|dashboardpan|chihuahua]` tag OR the `[sys.xml]`/`Cannot load station` channel ([Block 800] §800.8).

## 811.3 — Tridium's own tooling snapshots NOTHING post-deploy `[CERT-doc]`
- **Software Manager** shows signature status; does NOT backup after a module install.
- **Station Copier** copies a station location-to-location; no snapshot.
- **Commissioning wizard** installs from `.dist`, offers Application Director; no auto-backup.
- **No `niagara`/`niagarad` CLI `backup` subcommand** exists (niagara-help = zero). JACE-9000 only: serial-shell
  "7 Create SD Backup" + an automatic daily SD backup ~02:00 (`docJ9BackupRestore.txt`).
- `provisioningNiagara` has a "Backup Stations" JOB step — an OPTIONAL configured batch step, not automatic.
**So a post-deploy snapshot must be scripted** — the gap `station-snapshot.sh` fills.

## 811.4 — `station-snapshot.sh` remote-mode contract (kit) `[CERT-grounded / INFER]`
Two safe modes on top of the existing local station-dir mode:
- **FOX read-only mode (station running) — safe to schedule:** `fox:|file:^^config.bog` (hash SHA-256),
  `fox:|file:^^console_backup_<latest>.txt` (latin-1 parse), `spy:/metrics` + `spy:/sys/engineManager` — all pure
  reads, no `saveSync`. Safe interval 15-30 min (per-event after a deploy). `[INFER, grounded in read-only spy/file ORDs]`
- **PLATFORM full-backup mode — MANUAL only:** the `.dist` via `BBackupService`/Platform Administration forces
  `Station.saveSync` (`BFoxBackupJob.java:108`) — HEAVYWEIGHT (bog flush + crawl), flash-wear risk on a JACE
  ([Block 806] §806.3). Use ONLY pre/post-deploy, NEVER on a polling cycle. `[CERT-grounded]`
- **HASH:** `config.bog` (schema/link drift), latest `console_backup_*.txt` (new-exception delta), `.hdb` sizes (history growth).
- **DIFF vs previous:** `config.bog` → slot-type/link/flag changes → feed `schema-risk.sh` ([Block 795]/[Block 799]/[Block 807]: a retype = OUTAGE); console delta → new `com.angeles.*` exceptions → feed `triage-console.sh` ([Block 800]); `spy:/metrics` → globalCapacity approaching >100% (warn) / >110% (no boot, [Block 806] §806.6).
- **RATE/SIZE limits:** never a full `.dist` on a poll; the FOX file/spy reads are cheap; the backup is not.

## 811.5 — Post-deploy checklist step (kit wording) `[INFER, grounded]`
```
PRE-deploy:  1) BackupService "Backup Station" → pre-deploy .dist   2) sha256 config.bog → pre.hash
POST-deploy (≤5 min of the hot module reload):
  3) fetch latest console_backup_*.txt (fox:|file:^^… or local), parse latin-1, scan
     "Cannot load station"/"Missing frozen property"/"ClassCastException"  [B800 §800.8 schema-OUTAGE]
  4) spy:/metrics → globalCapacity counts < 100% of limits                 [B806 §806.6]
  5) sha256 config.bog again → diff vs pre.hash; any retype/slot-remove → schema-risk.sh  [B795 MM3]
  6) station boots clean → commit the new config.bog hash as the baseline
```

## 811.6 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BBackupService` (backup-rt), Fox channel "backup", adminWrite-gated; no `backup:` ORD | `[CERT]` | `BBackupService.java:340,356`; grep-absent scheme | Y — grep |
| 2 | `.dist` = config.bog (both) + hdb/adb (offline only); **console.* excluded from BOTH** | `[CERT]` | `BBackupService.java:242,244,243,245` | Y — grep |
| 3 | Online backup forces `Station.saveSync` = heavyweight (don't poll) | `[CERT]` | `BFoxBackupJob.java:108,111` | Y |
| 4 | Console remotely via Fox `file:^^` or Application Director; spy for load; latin-1 caveat | `[CERT-doc/CERT]` | docPlatform; [B800] §800.5 | Y |
| 5 | No Tridium tool auto-snapshots post-deploy → script it | `[CERT-doc]` | niagara-help (zero CLI backup); provisioning step optional | Y |

**Tally:** `[CERT]` ×3 · `[CERT-doc]` ×1 · `[INFER]` (contract) ×1. Decompiled cites are `extern` — driver token-verified.

## 811.7 — Connections & open gaps
- [Block 806] (backup load / spy surfaces), [Block 800] (console parse), [Block 795]/[Block 799]/[Block 807] (bog-diff = schema-risk), [Block 402]/[Block 411] (bog save + backup naming), [Block 672-676] (JACE station tree).
- OPEN GAPS: (1) exact scriptable HTTP `GET /station/output` path on a JACE niagarad — unconfirmed ([Block 806] §806.7 concept); (2) a Fox directory-LIST ORD to find the latest `console_backup_*.txt` name — not confirmed; (3) JACE niagarad HTTP port specifics; (4) `backup:` ORD confirmed ABSENT.
- **B811-G1** (requires-execution): run the FOX read-only snapshot against a live JACE (`fox:|file:^^config.bog` + `spy:/metrics`) and confirm zero station-save side effect — the read-only proof `station-snapshot.sh` needs.
