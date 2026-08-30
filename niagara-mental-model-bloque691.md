# B691 — JACE_UMBRELLA platform/orchestration services (SC7): zero executable Program logic, orchestration all default, three top-level containers, web locked to localhost

> Focus: **jace-station-config** · Gap **SC7** (platform/orchestration services deployed + Program objects).
> Sources: `config.bog` file.xml (SD P2, READ-ONLY). Redacted evidence:
> `sources/probes/B685-jace-station-config/platform-orchestration.txt`.
> **SECRETS DISCIPLINE:** structure only; cert alias `default` is non-secret. Marker `[CERT-hw]` (SD artifact).
> Web/Fox/Box hardening already established in [Block 685] §685.3 — confirmed, not re-derived here.

## 691.1 — ZERO Program objects: no executable logic on the controller

[CERT-hw] The most security-relevant question for a field controller — does it run freeform executable logic? —
resolves to **NO**. A whole-file grep for `t="p:Program"` / `BProgram` / `ProgramModule` returns ONLY the
`ProgramService` shell itself (L266, empty body). **Zero `BProgram` instances, zero robots, zero
ProgramModules** are deployed anywhere in the station. There is no site-authored executable logic on this JACE —
consistent with the seed-station read (B685–B690). (Program-object framework internals = REMITTANCE; the
finding here is the DEPLOYMENT fact: none.)

## 691.2 — Orchestration services: all default

[CERT-hw]

| Service | deployed state | L |
|---|---|---|
| JobService | empty body — framework defaults | 37 |
| BatchJobService | `ThreadPoolJobQueue`, `maxThreads=1` | 794/796 |
| ProgramService | empty (see 691.1) | 266 |
| TemplateService | empty — no registered templates | 732 |
| SearchService | 2 default scopes: Config→`station:`, scope1→`sys:` (both isDefault) | 269 |
| ProvisioningNiagara ext | present, empty (SupervisorLicenses + StationPollScheduler empty) — confirms [Block 686] | 865 |

No persisted or scheduled job objects under either job service. `BatchJobService maxThreads=1` = single-threaded
batch, the template default for a small controller.

## 691.3 — Web/Fox/Box: hardened transport, web bound to localhost

[CERT-hw] Confirms [Block 685] §685.3: WebService `httpEnabled=false` / `httpsOnly=true` / TLS≥1.3 (L739/L744/
L745); FoxService `foxsOnly=true` / TLS≥1.3 (L215/L216); BoxService 13 channels (L177–203); Jetty + UserDataConfig
empty (all defaults). One deployment detail worth surfacing: **`validHostHeaders="localhost"`** (L782) — the
Host-header allowlist is `localhost` only. On a template-instantiated station this is the provisioned default;
until overridden post-provisioning it constrains which Host header the web server accepts. [INFER] this is a
template default, not a per-site hardening decision (same origin as every other default in SC1–SC6).

## 691.4 — Top-level structure: three containers, nothing else

[CERT-hw] Under root `b:Station` (L3) there are exactly THREE depth-1 containers:

| container | type | contents | L |
|---|---|---|---|
| Services | b:ServiceContainer | the 21 services (B685) | 5 |
| Drivers | d:DriverContainer | 2 networks: NiagaraNetwork + NrioNetwork (B686/B687) | 801 |
| Apps | app:AppContainer | **empty** | 954 |

No `Logic`/wiresheet container, no `Files`/nav-tree structure, no fourth top-level component. `/Apps` empty
confirms B685 (no HxApp/PxView/UI deployed on the JACE). The root also carries the provisioning-template marker
`ntpl:fileName="NewJACEProvisioningStation.ntpl"` v1.5 (L958/L960).

## 691.5 — SC7 verdict

[CERT-hw]+[INFER] The platform/orchestration layer adds no site content: no executable logic, default job/
template/search services, hardened-but-template transport, and only the three standard top-level containers with
an empty Apps. This is the sixth and final per-gap confirmation that `JACE_UMBRELLA` is the
`NewJACEProvisioningStation` template instantiated with a minimal delta (one relay point, one admin) — a seed
station, not a working field application. SC8 will consolidate this across all gaps and contrast it with the
supervisor.

## Connections

- Web/Fox/Box hardening → [Block 685] §685.3 (this focus). Provisioning ext empty → [Block 686]. Field IO →
  [Block 687]. Program/BatchJob/Template framework → focuses `provisioning` [Block 567] / `template` [Block 577].
  Template origin marker → [Block 685] §685.4.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | zero Program/BProgram/robot instances (only ProgramService shell) | [CERT-hw] | grep p:Program = 1 (service only), L266 | grep-confirmed |
| 2 | JobService empty; BatchJobService maxThreads=1 | [CERT-hw] | L37/L796 | grep-confirmed |
| 3 | TemplateService empty; SearchService 2 default scopes | [CERT-hw] | L732/L269 | grep-confirmed |
| 4 | WebService validHostHeaders=localhost | [CERT-hw] | L782 | grep-confirmed |
| 5 | 3 top-level containers (Services/Drivers/Apps), Apps empty, no Logic/Files | [CERT-hw] | L5/L801/L954 | grep-confirmed |
| 6 | template default, not per-site hardening | [INFER] | ntpl marker L958 + SC1–SC6 | reasoned |

**Tally:** [CERT-hw] ×5 · [INFER] ×1. Ratio 0.2. Block TYPE = **EVIDENCE**. The critical absence (0 Program
objects) was independently grep-measured. 6/6 load-bearing citations confirmed. Evidence-file secret-scan clean.

## Open gaps (this focus)

SC7 CLOSED. Investigable remaining: **SC8** (supporting stores registry.db/alarm.adb/platform.bog + the
focus-closing SYNTHESIS: the field-controller profile vs the supervisor). SC4-G1 stays requires-execution.
After SC8, investigable → 0 → focus STOP.
