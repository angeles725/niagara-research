# RESEARCH-STATE — focus: jace-station-config (the DEPLOYED station application of the JACE-8000 field controller, read from the SD data-at-rest)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-30** a pedido del operador
> ("qué otros focuses podemos hacer distintos al supervisor, usando la copia del microSD").
> Fuente = el `config.bog` + stores de la station `JACE_UMBRELLA` extraídos READ-ONLY del microSD
> (`local-sd-image/jace-sd.img`, P2 QNX6, via `tools/qnx6read.py`). Artefacto `live-install` →
> **SECRETS DISCIPLINE**: se cita ESTRUCTURA (servicios, redes, puntos, roles, formatos), nunca
> valores secretos (hashes de usuario, keyring, credenciales de red, campos `BPassword`).
>
> **Ángulo:** NO los internals del framework (ya cubiertos), sino la CONFIGURACIÓN REAL DESPLEGADA de
> un controlador de campo JACE-8000 — qué servicios, redes (NiagaraNetwork upstream + NrioNetwork field
> IO), puntos, usuarios/roles/categorías, tags y lógica corre esta unidad, y en qué se diferencia del
> SUPERVISOR. El `config.bog` es BOG XML EN CLARO (ZIP+file.xml, 51 KB, ~1400 líneas); solo los campos
> `BPassword` van cifrados GCM por-campo (B393). Data-at-rest OFFLINE — no requiere hardware vivo.
> Sibling de datos del focus `jace8000` (red/serie) y `jace8000-sd` (particiones/boot).

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 688
gaps_closed: 8
known_gaps: 9
investigable_open: 0
requires_execution_open: 1
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

focus: jace-station-config
status: stopped (read-only-investigable exhausted 8/8; only SC4-G1 requires-execution remains)
bootstrapped_on: 2026-08-30
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B685)

## Coverage

- **Covered blocks**: 684 corpus-wide (this focus: B685-) (shared-global)
- **Coverage metric**: 8 / 9 gaps closed (SC1-SC8 investigable=0; SC4-G1 requires-execution parked)
- **Source (out of git)**: `config.bog` (7843 B ZIP → 51378 B file.xml) + `alarm.adb` + `registry.db` +
  history `.hdb` + `/opt/niagara/defaults/platform.bog`, all under `local-sd-image/` (gitignored,
  secret-bearing) — extracted READ-ONLY from SD P2 via `tools/qnx6read.py`.
- **Measured skeleton (bootstrap scan)**: station `JACE_UMBRELLA`, `FIPSEnabled="false"`,
  `reversibleEncodingKeySource="keyring"`; 21 Services (bootstrap note said 23 — §14 corrected to 21 in B685); 2 Drivers networks (NiagaraNetwork `nd:` +
  NrioNetwork `nrio:`); module handlers used = baja, alarm, app, basicDriver, batchJob, backup, box,
  control, driver, fox, history, hierarchy, hx, jetty, niagaraDriver, nrio, nss, template,
  niagaraVirtual, program, provisioningNiagara, search, tagdictionary, web; heavy tagging (`td:` ×144).

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | SC1 station skeleton + Services inventory — the 21 deployed Services, station identity (FIPS off, keyring encoding), which are default vs configured | bog-xml disk | closed (B685 — 21 services, 16 configured/5 default; template-hardened, pure field-controller posture; §14 corrects "23"→21) |
| high | SC2 NiagaraNetwork (nd) deployed config — is this JACE subordinate to the supervisor? upstream link, station connections, provisioning/exportTags, credential STRUCTURE | bog-xml disk | closed (B686 — 0 remote BNiagaraStation; not a supervisor, lists no upstream; inbound-supervisor not determinable from JACE config; foxs:4911 TLS1.3 only door) |
| high | SC3 NrioNetwork (nrio) deployed config — the physical field IO wired to this controller: NRIO modules, points, addresses, control vs monitor | bog-xml disk | closed (B687 — 1 IO-34 (addr 1+2, FW2.2, DOWN); exactly 1 commissioned point: ro1 relay OUTPUT; minimally-commissioned seed station) |
| medium | SC4 UserService/RoleService/CategoryService/AuthenticationService — the REAL deployed RBAC (users, roles, categories, auth schemes) — SECRETS: structure only | bog-xml disk | closed (B688 — 1 super-user admin, no policy overrides, legacy AX scheme on, dangling cat-3 ref w/ nil current impact) |
| medium | SC5 AlarmService + AuditHistoryService + HistoryService + LoggingService deployed config — alarm classes, histories actually collected, audit/log config | bog-xml + hdb disk | closed (B689 — alarms default+escalation disabled+0 recipients; 3 local audit trails; nothing archives off-box; confirms B684 weak-data-at-rest) |
| medium | SC6 TagDictionaryService + HierarchyService deployed — the 144 td: references: which dictionaries, hierarchies, relations this station uses | bog-xml disk | closed (B690 — 100% stock Niagara v1.5 dict, 0 applied tags, empty hierarchy; 144 td: = measurement artifact of stock verbosity) |
| medium | SC7 platform/orchestration services deployed — WebService/FoxService/BoxService/JobService/BatchJobService/ProgramService/TemplateService/ProvisioningNiagara + Program objects (freeform logic) | bog-xml disk | closed (B691 — 0 Program/executable-logic objects; orchestration all default; 3 top-level containers, Apps empty; web validHostHeaders=localhost) |
| low | SC8 supporting stores + synthesis — registry.db + alarm.adb + platform.bog contents; SYNTHESIS: the field-controller profile vs the supervisor | disk + synthesis | closed (B692 — platform.bog=svc defaults/no creds; registry.db/alarm.adb binary REMITTANCE; SYNTHESIS: seed-station profile vs supervisor) |
| medium | SC4-G1 runtime access outcome (open vs denied) of an ORD mapped to an UNDEFINED category index | requires-execution | requires-execution (live probe w/ non-admin user, or code read of BCategoryService.getCategory) |

`tried:` (none blocked yet — all 8 gaps have confirmed on-disk source: config.bog + supporting stores
extracted from SD P2 via qnx6read.py; SOURCE-BEFORE-AGENT passes for the whole backlog).

## Remittance (ya cubiertos por bloques existentes — NO son gaps de este focus)

- BOG format (ZIP + file.xml, module handlers `m=`, `reversibleEncodingKeySource`, encoding) → [Block 15]/[Block 33]/[Block 34] (base corpus).
- Este MISMO config.bog extraído VIVO sobre Fox (backup channel, .dist) → focus `jace8000` [Block 473]; extraído del SD (árbol QNX6) → focus `jace8000-sd` [Block 674].
- Cifrado per-campo `BPassword` GCM en reposo (data-at-rest) → [Block 393]/[Block 466].
- NRIO field-bus DRIVER internals (libplatnrio JNI, wire) → focus `jace8000-qnx-native` [Block 680]; framework driver model → focus `framework-drivers`.
- NiagaraNetwork framework (device-proxy BNiagaraStation, Fox join, exportTags) → focus `niagara-network-supervisor` [Block 414]–[Block 420].
- RBAC model internals (users/roles/categories/password-policy/encoders) → focus `access-control` [Block 558]–[Block 566].
- Tag subsystem internals → focus `tags` [Block 260]–[Block 270]; hierarchy engine → focus `hierarchy` [Block 584]–[Block 590].
- Alarm routing/recipients → [Block 34] + focus `alarm-webhook`; history/DB persistence → focus `database` [Block 402]–[Block 413].
- BackupService over Fox (backup channel, bit-48 gate) → focus `jace8000` [Block 472]/[Block 475].
- Provisioning subsystem internals → focus `provisioning` [Block 567]–[Block 576]; template engine → focus `template` [Block 577]–[Block 583].

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-08-30 | (bootstrap — AUDIT-FIRST scan of config.bog tree) | — | no · inline (qnx6read.py extract + XML structure scan) | SC1–SC8 seeded |
| 1 | 2026-08-30 | SC1 skeleton + 21 Services | B685 | yes · sonnet (config.bog structural sweep) + inline verify | 0 new (SC2/SC3/SC6 confirmed-ahead: localStation, io34 device, tagdict v1.5) |
| 2 | 2026-08-30 | SC2 NiagaraNetwork | B686 | yes · sonnet (subtree sweep) + inline re-measure/REFINE | 0 new (inbound-supervisor confirm = existing J-series live wall) |
| 3 | 2026-08-30 | SC3 NrioNetwork field IO | B687 | yes · sonnet (subtree sweep) + inline verify | 0 new (IO subtree fully read: 1 module, 1 point) |
| 4 | 2026-08-30 | SC4 deployed RBAC | B688 | yes · sonnet (RBAC sweep) + inline framework-semantic REFINE | SC4-G1 (requires-execution: dangling category-index runtime behavior) |
| 5 | 2026-08-30 | SC5 alarms/histories/audit | B689 | yes · sonnet (sweep) + inline verify | 0 new (egress-none confirmed 3 ways) |
| 6 | 2026-08-30 | SC6 tags + hierarchy | B690 | yes · sonnet (sweep) + inline verify | 0 new (stock dict, 0 applied tags) |
| 7 | 2026-08-30 | SC7 platform/orchestration | B691 | yes · sonnet (sweep) + inline verify | 0 new (0 Program objects; 3 top-level containers) |
| 8 | 2026-08-30 | SC8 stores + SYNTHESIS (focus close) | B692 | no · inline (platform.bog extract + synthesis of B685-691) | 0 new (focus STOP; SC4-G1 stays req-exec) |

## Blocked gaps (each tagged with what it needs)

(none — the whole backlog is read-only investigable from on-disk artifacts already extracted.)

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 0
- **Open gaps — requires-execution**: 1 (SC4-G1)
- **Open gaps — blocked**: 0
- Budget cap: none

## Dismissed file types

- (to be filled by the census/coverage pass as gaps close — the primary artifact is one BOG XML;
  supporting stores registry.db/alarm.adb/.hdb are claimed by SC5/SC8.)
