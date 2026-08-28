# RESEARCH-STATE — focus: provisioning (ACTIVE)

> Multi-focus corpus (METHODOLOGY §16). SEEDED by an AUDIT-FIRST coverage sweep (§13) on 2026-08-28 (delegated
> sonnet, verified inline) that mapped the `provisioningNiagara` fleet subsystem against the corpus and separated
> REMITTANCE (already covered) from genuine gaps.
>
> **Angle (§b2):** the N4 fleet/provisioning subsystem — the `provisioningNiagara` module: supervisor→subordinate
> batch operations, the Step/Robot model, software distribution, backup/restore, bootstrap/discovery, credential
> and license batch ops — as a dedicated subsystem. DISPERSED across B16 (Provisioning Service intro), B39
> (46-step catalog + network ext + platform connection + backup + poll scheduler), B472/B475 (backup-over-Fox),
> B511 (BJob), B14 (BBatchJobService), but never a subsystem focus. Read-only, decompiled-Java + `docProvisioning`.
> Corpus language = **English**.
>
> **Scope:** the provisioning ENGINE and its step/robot/channel/software model — NOT the generic BJob framework
> ([B511]), NOT the supervisor↔sub join device-proxy model ([B414–B420]), NOT backup-over-Fox wire ([B472]).

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 1
gaps_closed: 1
known_gaps: 10
investigable_open: 9
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: provisioning
status: active (1/10; PV1→B567 DONE; NEXT PV2 BNiagaraProvisioningChannel)
seeded_from: AUDIT-FIRST coverage sweep 2026-08-28 (delegated sonnet; pre-flight verified inline)
seeded_on: 2026-08-28
gaps_total: 10 investigable (PV1–PV10)
gaps_closed: 0
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Surface (audit, source-confirmed)

`provisioningNiagara-wb` = 219 classes (NO separate `-rt`; runtime lives in `-wb`): top-level (11), `software`
(18) + `ui/software` (31), `ui/station` (16), `ui/credentials` (14), `bootstrap` (10), `template` (9), `backup`
(4), `certificate` (4), `license` (5), `program` (5), `credentials` (7), plus tls/saml/systempassphrase/time
step packages; public API `javax.baja.provisioningNiagara` (BNiagaraNetworkJob, BForEachStationStage,
ProvisioningNiagaraManager). `provisioningNiagara-ux` = 48 classes (BProvisioningNiagaraRpcUtil + 40 BUx*Factory
step builders). Foundation: `batchJob` module (95 classes; `batchJob-rt/driver/` = 13: BDeviceNetworkJob,
BNetworkJobStage, BDeviceJobStep…). `docProvisioning` guide jar present (chapters unread). No base `provisioning`
module and no `javax/baja/provisioning` package (confirmed absent).

## REMITTANCE — already covered (cite, do NOT re-derive)

- BNiagaraNetworkJob 2-stage structure (initial + forEachStation) → **[B16 §16.10]**, **[B39 §39.2]**
- Full ~46-step catalog (types/base classes/factories) → **[B39 §39.3]** (authoritative)
- BProvisioningNiagaraNetworkExt properties (software/licenses/pollScheduler/timeouts) → **[B39 §39.4]**
- BPlatformConnection / BPlatformWorker per-station → **[B39 §39.5]**
- BStationPollScheduler (fast/normal/slow) → **[B39 §39.4]**
- Backup/restore flow + BProvisioningBackupStep operational → **[B39 §39.1.3]**, **[B16 §16.11.1]**
- Backup-over-Fox mechanism (BBackupChannel, Interest, permission bit 48) → **[B472]**, **[B475]**
- Retention policy / backup path / disk gotcha → **[B39 §39.2.5]**
- BBatchJobService high-level (license gate `provisioning`, alarm source, persistence) → **[B14 §14.11]**
- BJob/BJobService base framework → **[B511]**
- BProgram/Robot runtime execution (program module) → **[B541]**
- Scale limits / concurrent job cap / Fox channel exhaustion → **[B39 §39.2.4]**, **[B13]**

## Gap-backlog (prioritized) — genuine uncovered provisioning surfaces

| Priority | Gap | Scope | Where (`organized/…`) | Status |
|---|---|---|---|---|
| high | ~~**PV1 batchJob driver sub-framework**~~ | generic driver-agnostic batch engine (BBatchJob extends BJob); jobs serialized (queue=1), per-device fan-out parallel (cap 2); @AgentOn driver:DeviceNetwork = every driver; provisioning = specialization | — | **CLOSED → B567** |
| high | **PV2 BNiagaraProvisioningChannel** | the custom Fox channel carrying daemon-session tasks, installable queries, license archive, platform file ops (≠ backup-over-Fox B472) | `provisioningNiagara-wb/…/com/tridium/provisioningNiagara/BNiagaraProvisioningChannel.java`, `BPlatformConnection.java` | **NEXT** |
| high | **PV3 software distribution engine** | ProvisioningRegistry/BSoftwareContainer/BInstallableSummary + the adjacent-step combining algorithm | `provisioningNiagara-wb/…/software/` (18) + `NiagaraNetworkJobOp.java` | open |
| medium-high | **PV4 bootstrap/discovery** | BDeviceBootstrapExt/BDhcpDiscoveryStep/BNiagaraNetworkDiscoveryStep/BEnableBootstrapStep/BSetupReciprocalConnectionStep — new-controller discover→bootstrap→rename→join | `provisioningNiagara-wb/…/bootstrap/` (10) + doc `Edge10Startup/DeviceProvisioning*` | open |
| medium-high | **PV5 async-action protocol** | BProvisioningStationExt: cancelAsyncAction/makeInvokeId/asyncActionComplete — the long-running Fox op pattern all StationExts inherit | `provisioningNiagara-wb/…/BProvisioningStationExt.java`, `BAsyncActionEvent.java` | open |
| medium | **PV6 BProvisioningRobot permission masking** | custom getPermissions strips to operator-read+admin-read; robot runs under supervisor creds via BProgramService — the security "escape hatch" step | `provisioningNiagara-wb/…/program/BProvisioningRobot.java`, `BRobotJobStep.java` | open |
| medium | **PV7 template deployment pipeline** | BAbstractDeployStep → Deploy/Upgrade template + application, ProvisionTemplateManager, ProvisioningBulkDeployUtil — supervisor template → N subordinates | `provisioningNiagara-wb/…/template/` (9) | open |
| medium | **PV8 credentials batch management** | BSetPlatformCredentialsJobStep/BAddStationUserStep/BSetStationConnectionCredentialsStep — how creds are encrypted, transmitted via daemon session, persisted | `provisioningNiagara-wb/…/credentials/` (7) + `systempassphrase/` | open |
| low-medium | **PV9 license management chain** | BSupervisorLicenses/BLicenseStationExt/BUpdateLicensesJobStep — portal → supervisor DB → subordinate; BConvertToPerpetualLicenseModeJobStep | `provisioningNiagara-wb/…/license/` (5) + public API | open |
| low | **PV10 ux RPC surface** | BProvisioningNiagaraRpcUtil @NiagaraRpc methods over BOX + BUx*Factory step-builder pattern + BNiagaraNetworkUxJobBuilder AgentOn entry | `provisioningNiagara-ux/…/ux/BProvisioningNiagaraRpcUtil.java` | open |

## Proven-absent / notes

- No `provisioning-rt.jar` (runtime lives in `-wb`; B39 hallazgo #0). No `javax/baja/provisioning` base package.
  No docSource javadoc for provisioningNiagara (only vineflower/decompiled).
- No dedicated `guides-clean/Provisioning/` dir; official content scattered in `Edge10Startup`, `AXtoN4Migration`,
  and `BBatchJobService.txt` javadoc. Literal zeros: `guide-search "bootstrap discovery"`,
  `guide-search "batch job provisioning"` → 0.
- B169 "auto-provisioning" = MX60 equipment reader config, NOT this module (not a remittance candidate).

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: 9 (PV2–PV10). Focus ACTIVE.
- **Gaps closed**: 1 (PV1→B567).
- **requires-execution / blocked**: 0.
- **Coverage metric**: 0 / 10.
