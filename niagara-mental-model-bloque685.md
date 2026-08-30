# B685 — JACE_UMBRELLA deployed station skeleton + Services inventory (SC1): a 21-service field-controller station, hardened-by-template, read from the boot microSD

> Focus: **jace-station-config** · Gap **SC1** (station skeleton + Services inventory) · next global block after B684.
> Sources: the deployed `config.bog` of station `JACE_UMBRELLA`, extracted READ-ONLY from the JACE-8000 boot
> microSD (P2 QNX6) with `tools/qnx6read.py`. `config.bog` = ZIP(deflate) → `file.xml` (51378 B, ~1400 lines
> of BOG XML). Redacted structural evidence: `sources/probes/B685-jace-station-config/services-inventory.txt`
> (secrets masked). config.bog sha256 `b8bbea6e…1719498`.
> **SECRETS DISCIPLINE (live-install):** structure only — password/keyring/IP values MASKED, never cited.
> Marker convention: facts read from the physical SD artifact are `[CERT-hw]` (same lineage as the
> `jace8000-sd` focus, B672–B676).

This is the FIRST block of the `jace-station-config` focus: not the framework internals (already covered — see
Remittance), but what a real JACE-8000 field controller ACTUALLY runs, read from its own storage at rest. The
`config.bog` component tree is plaintext BOG XML; only per-field `BPassword` values are ciphertext (B393). So
the deployed application is fully readable offline.

## 685.1 — Station identity

[CERT-hw] Root `<bajaObjectGraph version="4.0" reversibleEncodingKeySource="keyring" FIPSEnabled="false">`
over `<p m="b=baja" t="b:Station">` with `stationName="JACE_UMBRELLA"` (file.xml L2–L4).

- **BOG format 4.0**, the standard station serialization (format itself = REMITTANCE [Block 15]/[Block 33]).
- **`reversibleEncodingKeySource="keyring"`** — reversible field encoding (the `BPassword`/reversible values)
  is keyed from the machine **keyring**, not a static key. This is the disk-side confirmation of the
  data-at-rest model B466 described from the live unit: config.bog off-machine cannot decrypt those fields
  without the keyring (`.km`/`.kr`, which are NOT serialized into the .bog — see 685.4).
- **`FIPSEnabled="false"`** — this JACE-8000 runs with FIPS mode OFF at the station layer. (Contrast: the
  JACE-9000 is FIPS 140-2 by requirement, focus `jace9000` [Block 657]. The AX-era digest scheme is also
  still present — see 685.3.)

## 685.2 — Services inventory: 21 services under /Services

[CERT-hw] `<p n="Services" t="b:ServiceContainer">` (L5) has **21 direct service children** (measured:
`grep -c '<!-- /Services/… -->'` = 21). §14 CORRECTION to this focus's bootstrap note, which said "23
Services" — that was a miscount of `/Services/…` comments; the serialized container holds 21. The two extra
comment paths were the `/Drivers/*` networks (685-companion gap SC3), not services.

| # | Service | type | handler | default? | key configured content | L |
|---|---|---|---|---|---|---|
| 1 | AlarmService | a:AlarmService | alarm | configured | FileAlarmDbConfig; defaultAlarmClass; escalationTimeTrigger (60 s) | 7 |
| 2 | BackupService | bk:BackupService | backup | configured | excludeDirectories = history, alarm, webFileCache, dataRecovery | 21 |
| 3 | CategoryService | b:CategoryService | baja | configured | ordMap (security/audit/log/nav/px → cat 2/3); categories User(1), Admin(2) | 25 |
| 4 | JobService | b:JobService | baja | default | empty | 37 |
| 5 | SecurityService | nss:SecurityService | nss | configured | certificates/default (CertificateInfo + CertificateExpiryPoint) | 40 |
| 6 | RoleService | b:RoleService | baja | configured | `admin` role + hierarchy:RoleHierarchies ext | 51 |
| 7 | UserService | b:UserService | baja | configured | defaultPrototype + 1 real account `admin` (pwd ciphertext); SMA/license notify | 59 |
| 8 | AuthenticationService | b:AuthenticationService | baja | configured | DigestScheme + AXDigestScheme (legacy) + ssoConfiguration (SSO off) | 154 |
| 9 | LoggingService | b:LoggingService | baja | default | empty | 172 |
| 10 | BoxService | box:BoxService | box | configured | full channel set + hierarchy:HierarchyBoxChannel | 175 |
| 11 | FoxService | f:FoxService | fox | configured | FOX 1911 disabled; FOXS 4911 only, TLS≥1.3 (see 685.3) | 206 |
| 12 | HierarchyService | hierarchy:HierarchyService | hierarchy | default | empty | 225 |
| 13 | HistoryService | h:HistoryService | history | configured | archiveHistoryProviders + historyGroupings (empty containers) | 228 |
| 14 | AuditHistoryService | h:AuditHistoryService | history | configured | historyConfig /JACE_UMBRELLA/AuditHistory (TZ Mexico_City) + SecurityAuditHistorySource | 235 |
| 15 | LogHistoryService | h:LogHistoryService | history | configured | historyConfig /JACE_UMBRELLA/LogHistory | 256 |
| 16 | ProgramService | p:ProgramService | program | default | empty | 266 |
| 17 | SearchService | s:SearchService | search | configured | defaultScopes: Config (station:) + sys: | 269 |
| 18 | TagDictionaryService | td:TagDictionaryService | tagdictionary | configured | neqlizeOptions + Niagara dictionary v1.5 (full tag defs) — see SC6 | 285 |
| 19 | TemplateService | ntp:TemplateService | template | default | empty | 732 |
| 20 | WebService | w:WebService | web | configured | HTTP 80 off, HTTPS 443 only TLS≥1.3 + 5 security headers (see 685.3) | 735 |
| 21 | BatchJobService | bjb:BatchJobService | batchJob | configured | jobQueue/maxThreads = 1 | 794 |

Default (empty body): JobService, LoggingService, HierarchyService, ProgramService, TemplateService (5/21).
The rest carry configured content. Handlers span 15 modules (baja, alarm, backup, nss, box, fox, history,
hierarchy, program, search, tagdictionary, template, web, batchJob) — the module set a JACE deploys.

## 685.3 — Hardening posture is real and non-default

[CERT-hw] The transport-facing services are hardened beyond a bare station's defaults:

- **FoxService** (L206–223): plain FOX 1911 `foxEnabled="false"` (L210); FOXS 4911 `foxsEnabled="true"`,
  `foxsOnly="true"` (L215), `foxsMinProtocol="tlsv1_3"` (L216). A bare station leaves plain FOX enabled.
  Disk-side confirmation of the live TLS-1.3-only finding (focus `jace8000` [Block 474]).
- **WebService** (L735–792): `httpsOnly="true"` (L744), `httpsMinProtocol="tlsv1_3"` (L745), HTTP 80 off,
  and five explicit HTTP security-header providers — CSP (L766), X-Frame-Options `sameorigin` (L776),
  X-Content-Type-Options, Cross-Origin-Opener-Policy same-origin, X-XSS-Protection; `validHostHeaders=localhost`
  (L782). This is a deliberately hardened header posture.
- **AuthenticationService** (L154): SCRAM-family `DigestScheme` PLUS the legacy `AXDigestScheme` still present;
  SSO off. The AX digest presence is the FIPS-off counterpart of 685.1.

[INFER] These are exactly the settings the **provisioning template** sets (685.4): a template-hardened field
controller, not a hand-tuned one. Residual weakness is the certificate/credential layer, not the protocol
floor — consistent with the live posture verdict (focus `jace8000` [Block 468], "hardening minado por certs
default + credencial admin expuesta").

## 685.4 — What is NOT here: pure field-controller posture + provisioned-from-template

[CERT-hw] The platform capability list (`services` NameList, L819 — 46 platform+station service names)
confirms the platform layer offers `platBacnet:BacnetEthernetPlatformServiceQnx`,
`platMstp:BacnetMstpPlatformServiceQnx`, `platLon:LonPlatformServiceQnx`, `platNrio:NrioPlatformServiceQnx`,
`platSerialQnx`, NTP/Syslog/TcpIp/License/CertManager/DataRecovery/`SystemPlatformServiceQnxTitan`. Yet at the
STATION layer:

- **No EmailService / notification-transport service** — this JACE cannot send alarm email itself; alarm
  notification must relay upstream (relevant to the operator's Telegram-egress design, focus `alarm-webhook`).
- **No BACnet/MSTP/LON driver network** under /Drivers despite the platform capability — only NiagaraNetwork
  (upstream) + NrioNetwork (field IO) are wired (SC2/SC3).
- **`/Apps` empty** (L954) — no HX/PX dashboards deployed directly on the JACE; the UI lives on the supervisor.
- **Single real user account** (`admin`) — no operator/technician accounts provisioned yet.
- **No WorkbenchService** — expected; confirms pure field-controller, not an engineering workstation.

[CERT-hw] The station carries a **provisioning-template marker**: `ntpl:fileName=NewJACEProvisioningStation.ntpl`,
`ntpl:vendor=Tridium`, `ntpl:version=1.5` (L957–963). The whole station was materialized from a Tridium JACE
provisioning template (template engine = REMITTANCE focus `template` [Block 577]–[Block 583]) — which is why
the hardening is uniform: it is template default, not per-site tuning.

## 685.5 — Credential fields: format only

[CERT-hw] The only password serialized into config.bog is the `admin` user authenticator:
`<p n="password" t="b:Password" v="[pbkdf2-sha256.1]=&lt;SALT&gt;:&lt;10000&gt;:&lt;SHA256&gt;"/>` (L112) —
PBKDF2-HMAC-SHA256, salted, **10 000 iterations**, format `[pbkdf2-sha256.1]=salt:iterations:hash`. VALUE
MASKED (SECRETS DISCIPLINE). This matches the AC4 encoder finding (focus `access-control` [Block 561]:
PBKDF2-10k for login credentials). Keyring material (`.km`/`.kr`) is NOT serialized into the .bog — it lives
in `/home/niagara/security/` + `/etc/km/` on the QNX FS (focus `jace8000-sd` [Block 674]); that is what
`reversibleEncodingKeySource="keyring"` (685.1) points at. **Operator note:** this admin PBKDF2 hash sits in
cleartext-structured form on the SD; anyone with the card can offline-attack it → the standing
rotate-admin recommendation ([Block 468]) applies to the card as well.

## Connections

- Framework BOG format → [Block 15]/[Block 33]. Data-at-rest keyring/machine-key encryption → [Block 466]/[Block 393].
- This same station extracted LIVE over Fox → focus `jace8000` [Block 473]; the SD partition/tree it lives on
  → focus `jace8000-sd` [Block 674]. TLS-1.3-only live confirmation → [Block 474]. Live hardening verdict →
  [Block 468].
- RBAC encoders (PBKDF2-10k) → focus `access-control` [Block 561]. Provisioning template → focus `template`
  [Block 577]. FIPS-by-requirement sibling controller → focus `jace9000` [Block 657].
- SEEDS/CONFIRMS sibling gaps: NrioNetwork io34 field device (L873–950) → SC3; NiagaraNetwork localStation-only
  → SC2; TagDictionary v1.5 (L285) → SC6; ntpl template marker → SC7/SC8.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Station JACE_UMBRELLA, BOG 4.0, keyring encoding, FIPS off | [CERT-hw] | file.xml L2–4 | grep-confirmed |
| 2 | 21 services under /Services (not 23) | [CERT-hw] | grep -c comments = 21 | measured |
| 3 | 5 services default-empty, 16 configured | [CERT-hw] | L per table | grep-confirmed |
| 4 | FOX 1911 off, FOXS 4911 only, TLS≥1.3 | [CERT-hw] | L210/L215/L216 | grep-confirmed |
| 5 | HTTP 80 off, HTTPS only TLS≥1.3, 5 sec headers | [CERT-hw] | L744/L745/L766/L776/L782 | grep-confirmed |
| 6 | admin pwd = PBKDF2-SHA256 10k salted (value masked) | [CERT-hw] | L112 | grep-confirmed, redacted |
| 7 | platform offers BACnet/MSTP/LON but no station driver network | [CERT-hw] | L819 NameList vs /Drivers | grep-confirmed |
| 8 | station provisioned from Tridium NewJACEProvisioningStation.ntpl v1.5 | [CERT-hw] | L957–963 | grep-confirmed |
| 9 | hardening is template-default (not hand-tuned) | [INFER] | 685.3+685.4 template marker | reasoned |

**Tally:** [CERT-hw] ×8 · [INFER] ×1. Ratio [INFER]/[CERT] = 0.125. Block TYPE = **EVIDENCE** (disk read). Low
ratio → this gap's evidence is well-sourced, not near-exhausted; the config tree has 7 more gaps of substance.
Token-checks: 8/8 load-bearing citations grep-confirmed against file.xml before writing. Secret scan of the
committed evidence file: clean (password/hash masked; only the config.bog sha256 integrity anchor remains).

## Open gaps (this focus)

SC1 CLOSED. Next: **SC2** (NiagaraNetwork deployed config — is this JACE subordinate to the supervisor?).
Confirmed-ahead: NiagaraNetwork has `localStation` + ProvisioningNiagaraNetworkExt (L865); NrioNetwork has a
real `io34_1_2` Nrio34Module with relay output (L904–931) for SC3.
