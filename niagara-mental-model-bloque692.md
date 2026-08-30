# B692 — JACE_UMBRELLA supporting stores + focus SYNTHESIS (SC8): a provisioning-template seed controller, profiled against the supervisor

> Focus: **jace-station-config** · Gap **SC8** (supporting stores + synthesis) — FOCUS-CLOSING block. Sources:
> `platform.bog` + `registry.db` + `alarm.adb` (SD P2, READ-ONLY) and blocks [Block 685]–[Block 691].
> Redacted evidence: `sources/probes/B685-jace-station-config/supporting-stores.txt`.
> **SECRETS DISCIPLINE:** structure only. Marker `[CERT-hw]` (SD artifact). Block TYPE = **SYNTHESIS** (a high
> [INFER] ratio is expected here — this consolidates cited findings, it does not add raw evidence).

## 692.1 — Supporting stores

[CERT-hw] Beyond `config.bog` (the station), the station/platform storage on P2 holds:

- **`/opt/niagara/defaults/platform.bog`** (4546 B, **plain XML** — not zipped, unlike config.bog). Root:
  `reversibleEncodingKeySource="none"`, `FIPSEnabled="false"`. It is the **PlatformServiceContainer defaults** —
  a registry of the platform-daemon services (SystemService, SerialPortService, DataRecoveryService,
  CertManagerService, BacnetEthernet/BacnetMstp/Lon/Nrio PlatformServiceQnx, NtpPlatformService,
  TcpIpPlatformService, LicensePlatformService, SyslogPlatformService) each with a description + alarm-support
  text. **No credentials, no ports, no secrets** (`grep -c Password|port` = 0); `encoding=none` because a
  defaults file has nothing to protect. This is the same platform-service set the station's `services` NameList
  advertised (B685 §685.4) — the platform-layer capability, distinct from the station's own daemon config
  (machine-key-encrypted, live-only, focus `jace8000` [Block 466]).
- **`registry.db`** (842 911 B) and **`alarm.adb`** (17 408 B) are **binary** stores (module registry / alarm
  DB) — REMITTANCE: registry format → focus `jace8000-sd` [Block 674]; alarm/history persistence → focus
  `database` [Block 402]. Not re-parsed here (binary parsing is out of this focus's on-disk-config scope; a
  child gap could target them but they carry no station-application semantics beyond what config.bog declares).

## 692.2 — The field-controller profile (SC1–SC7 consolidated)

[CERT-hw across B685–B691] `JACE_UMBRELLA` is the **`NewJACEProvisioningStation.ntpl` v1.5 template
instantiated with a minimal delta**. Every axis says the same thing:

| axis | finding | block |
|---|---|---|
| identity | BOG 4.0, FIPS off, keyring-sourced field encoding | B685 |
| services | 21 services, 5 default-empty, rest template-configured; hardened transport (FOXS/HTTPS TLS-1.3-only, HTTP off, 5 sec-headers, web bound to localhost) | B685/B691 |
| upstream | NiagaraNetwork lists NO supervisor and NO subordinate (not itself a supervisor); reached inbound via foxs:4911 | B686 |
| field IO | one IO-34 (addr 1+2, FW 2.2, DOWN at snapshot), exactly ONE commissioned point: `ro1` relay output | B687 |
| RBAC | one super-user `admin`, no policy overrides, legacy AX scheme on, dangling cat-3 ref (nil current impact) | B688 |
| data | 3 local audit trails (Audit/Security/Log), alarms default + escalation disabled + 0 recipients, nothing archives off-box | B689 |
| tags | 100% stock Niagara v1.5 dictionary, 0 applied tags, empty hierarchy | B690 |
| logic/UI | 0 Program objects, /Apps empty, 3 top-level containers only | B691 |

**Thesis:** the deployed application is essentially the factory template plus (one relay output + one admin
user). It is a **seed / minimally-commissioned controller**, not a populated field application — captured at or
near commissioning (the one field module was down; no supervisor join; no site logic/UI/tags).

## 692.3 — Profiled against the SUPERVISOR (why this focus was distinct)

[INFER, grounded in the cited focuses] The operator asked for a focus "distinct from the supervisor." The
contrast is now concrete — the same N4 framework, two opposite deployment roles:

| dimension | JACE-8000 field controller (this focus) | Supervisor (OptimizerSupervisor N4, most of the corpus) |
|---|---|---|
| platform | QNX/ARM Cortex-A8 appliance (focus `jace8000` [Block 459]) | Windows x86 install (focus `platform-native`) |
| NiagaraNetwork role | leaf: lists no stations; reached inbound | holds the `BNiagaraStation` device-proxies pointing DOWN at JACEs (focus `niagara-network-supervisor` [Block 420]) |
| field IO | direct: NRIO RS-485 bus, physical points (B687) | none direct — aggregates via subordinates |
| UI / apps | none (`/Apps` empty, no PX) | the PX dashboards, HxApps, nav (focuses `px-*`, `chihuahua`) |
| egress | none configured (B689) | EmailService, reporting, RDBMS export (focuses `email`, `reports`, `database`) |
| executable logic | 0 Program objects (B691) | control programs, kitControl, custom modules (focus `chihuahua-source`) |
| data at rest | cleartext `.hdb`/`.bog` on removable SD → card = full data compromise (B684/B689) | disk-resident, but same per-field-only encryption (focus `signing-pki` [Block 393]) |

The field controller is the **thin edge**: it senses/actuates and records locally; the supervisor is the
**brain**: it reaches down, aggregates, presents, and pushes out. This focus read the edge from its own storage —
something the supervisor-centric corpus never did.

## 692.4 — Security posture of the deployed edge (consolidated)

[CERT-hw/INFER] Strong where the platform enforces it (secure boot + encrypted firmware + de-privileged daemons,
focus `jace8000-qnx-native` [Block 684]; TLS-1.3-only transport, B685). Weak where deployment discretion rules:
single super-user admin with a 10k-PBKDF2 hash sitting in cleartext-structured form on a removable card
(B685/B688), legacy AX auth scheme still on, FIPS off, all audit data local-only with no off-box replica and no
tamper-evidence (B689), and one latent config defect (dangling category-3 reference, B688 → SC4-G1). None of
these is exotic — they are the template defaults left un-hardened. **Operator actions:** rotate `admin` (also on
the card); consider disabling `AXDigestScheme`; if this unit goes to production, add least-privilege accounts
(which will make the SC4-G1 category-3 defect matter) and configure off-box audit/alarm egress.

## Connections

- Consolidates [Block 685]–[Block 691] (this focus). Edge vs supervisor: `jace8000` [Block 459], `jace8000-sd`
  [Block 674], `jace8000-qnx-native` [Block 684], `niagara-network-supervisor` [Block 420], `platform-native`,
  `email`, `reports`, `database`, `chihuahua-source`. Data-at-rest thesis → [Block 684]. Rotate-admin →
  [Block 468].

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | platform.bog = plain-XML platform-service defaults, encoding=none, no creds | [CERT-hw] | platform.bog root + grep=0 | grep-confirmed |
| 2 | registry.db/alarm.adb are binary stores (remittance) | [CERT-hw] | P2 tree B674 | confirmed |
| 3 | JACE_UMBRELLA = NewJACEProvisioningStation template + minimal delta (seed) | [CERT-hw]+[INFER] | B685–B691 table | synthesized |
| 4 | field-controller vs supervisor role contrast | [INFER] | cited focuses | reasoned |
| 5 | consolidated security posture (strong platform / weak deployment discretion) | [INFER] | B684/B685/B688/B689 | reasoned |

**Tally:** [CERT-hw] ×2 · [INFER] ×3 (+ synthesis references). Ratio high — EXPECTED for a SYNTHESIS block, not
an exhaustion signal. The two new [CERT-hw] facts (platform.bog structure) were grep-confirmed; the rest cite
already-verified blocks. Evidence-file secret-scan clean.

## Focus status

**SC8 CLOSED → jace-station-config investigable = 0 → focus STOP.** 8/8 investigable gaps closed (SC1–SC8);
one child gap **SC4-G1** remains **requires-execution** (runtime behavior of a dangling category index — needs a
live station with a non-admin user, or a read of `BCategoryService.getCategory`). No blocked-on-hardware gaps.
Next: §18 self-retrospective + archive + push.
