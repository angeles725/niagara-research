# OptimizerSupervisor (live install N4.14.0.162) — Research State

> Operational state for the NEW focus arc "live installed OptimizerSupervisor" (Layer 24). Separate
> from the main `RESEARCH-STATE.md` (decompiled-module corpus) and from the STOPPED Spyder focus.
> Mirrored in engram (`research/niagara/optimizersupervisor-gaps`, `.../optimizersupervisor-progress`),
> project `niagara-research`. READ-ONLY over real production data; corpus language ENGLISH.

## Subject

The REAL Honeywell **OptimizerSupervisor N4.14.0.162** as installed on this host (WSL via /mnt/c).
Three real paths:
- `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/` — software install (bin, jre, lib, modules=973 jars, defaults, conversion, niagara-help, chihuahua-*.jar). N4.14 — ALIGNED with the existing module corpus, so reuse module blocks.
- `/mnt/c/ProgramData/Niagara4.14/OptimizerSupervisor/` — platform/daemon layer (registry, security, stations, daemon, etc/nre.properties, logging).
- `/mnt/c/Users/equipo/Niagara4.14/OptimizerSupervisor/` — workspace (stations/, config.bog, templates, certManagement, security, audits/ipchanges.bog, build.gradle.kts, spyder.config).

## Coverage

- **Covered blocks (this focus)**: 1 (B123)
- **Coverage metric**: 1 / 14 gaps closed
- **Last iteration**: 2026-06-28 — closed A1 (station config: what the supervisor controls), wrote B123.

## Profile findings (bootstrap, all paths)

- **Live deployment** = 2 daemon-managed stations: **PRUEBAS** (rich, ~1.29 MB decoded) + **REFLOW**
  (~412 KB). Both auto-start. HoneywellMX60 (5.1 MB, external-passphrase BOG) is the largest but a
  workspace copy, not daemon-live.
- **Install**: 973 module JARs (corpus references "974 JARs indexed" — ALIGNED, reuse module blocks).
- **Stations on disk** (workspace): CASINO, casinon, CIPER, Dev, HoneywellMX60(+2 variants), IPCStation,
  PRUEBAS, PRUEBAS_reflow, REFLOW. Plus .rar/.7z archived copies.
- **Security store** (programdata): `.kr` (keyring), `.km` (key-master), `keystore.jceks`,
  `cacerts.jceks`, `untrusted.jceks`, `exemptions.tes`, `signing/`.
- **Workspace dev**: `build.gradle.kts` uses Tridium plugins (`com.tridium.niagara`, `-vendor`,
  `-signing`), vendor `Angeles4657` (cf. B113 "trust anchor Angeles"). `spyder.config` toolVersion 10.5.64.
- `.bog` = ZIP(file.xml) Baja Object Graph; live stations keyring-encoded (no plaintext secrets).

## Gap-backlog (prioritized)

| Priority | ID | Gap | Domain | Source | Status |
|---|---|---|---|---|---|
| high | A1 | What the supervisor controls (networks/equipment/points/schedules/histories) | A station-config | PRUEBAS/REFLOW config.bog | **covered B123** |
| high | C1 | Security model: users/roles/categories/permissions actually defined in the live station | C security | station config.bog UserService/RoleService + station `security/` | pending (investigable) |
| high | B1 | Platform/daemon layer: full daemon.properties + nre.properties + provisioning of subordinates | B platform | ProgramData daemon/, etc/ | pending (investigable) |
| medium | C2 | Keystore/cert model: keystore.jceks / cacerts.jceks / signing/ roles + cert lifecycle (model only, no key material) | C security | ProgramData security/, workspace certManagement/ | pending (investigable, partial — JCEKS values blocked) |
| medium | D1 | Installed-module inventory vs corpus: which of 973 jars are OEM/custom vs stock Niagara; chihuahua-*.jar parity | D install | install modules/, registry.db | pending (investigable) |
| medium | A2 | Deep equipment decode: DtcrHvacEquip / SnlsRtu / IrmParameter object internals (what each piece of equipment is + control logic) | A station-config | PRUEBAS config.bog | pending (investigable) |
| medium | A3 | Control logic / wiresheet programs (ProgramService, kitControl links) in the live station | A station-config | config.bog c:/program: types | pending (investigable) |
| medium | B2 | registry.db format + installed-type registry contents | B platform | ProgramData registry/registry.db (binary) | pending (investigable — binary parse) |
| medium | E1 | Workspace dev setup: build.gradle.kts modules + settings.gradle.kts subprojects (what is being developed) | E workspace | workspace gradle files | pending (investigable) |
| low | A4 | Alarm model: alarm classes/recipients configured live (AlarmService) | A station-config | config.bog alarm: types | pending (investigable) |
| low | A5 | History detail: which points are trended, archive providers, supervisor collection | A station-config | config.bog h: types | pending (investigable) |
| low | E2 | audits/ipchanges.bog: what IP-change audit captures | E workspace | workspace audits/ipchanges.bog | pending (investigable) |
| low | B3 | logging configuration (daemonlog.properties, logging/) | B platform | ProgramData logging/, daemon/ | pending (investigable) |
| low | D2 | defaults/ + conversion/ : platform.bog defaults, unit/bacnet conversions, migrator | D install | install defaults/, conversion/ | pending (investigable) |
| low | C3 | HoneywellMX60 external-passphrase BOG: structure of the largest deployment (secrets blocked) | A/C | workspace HoneywellMX60 config.bog | pending (structure investigable, secret values blocked-on-passphrase) |

## Iteration history

| # | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| 1 | 2026-06-28 | A1 — what the supervisor controls (station config) | B123 | 13 (C1,B1,C2,D1,A2,A3,B2,E1,A4,A5,E2,B3,D2,C3 seeded; A1 closed) |

## Blocked gaps (each tagged with what it needs)

- C2 (cert/key material values) — needs: JCEKS keystore passphrase (model is investigable; secret VALUES blocked-on-keys, and out of scope by sensitivity contract).
- C3 (HoneywellMX60 secret values) — needs: external BOG passphrase (pbkdf2-sha256). Structure is investigable; encoded secret values blocked-on-passphrase per B114.

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 13 (C1,B1,C2*,D1,A2,A3,B2,E1,A4,A5,E2,B3,D2 ; C2/C3 investigable for MODEL, blocked only for secret values)
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked** (secret values only, not structure): C2 values, C3 values — does not block the loop (structure remains investigable)
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none
- **Loop length estimate**: ~13 investigable gaps → roughly 10-13 more iterations before investigable exhaustion.
