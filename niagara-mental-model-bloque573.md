# Block 573 — Template deployment: provisioning is a fleet wrapper over the generic `template` module — `.ntpl` files cached in `^templateCache`, deploy/upgrade/update steps that call `BulkDeployUtil.installTemplateToStation`, and two flavors (Template vs Application)

**Session**: 2026-08-28
**Focus**: `provisioning` (gap PV7 — the template deployment pipeline: supervisor template → N subordinates)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the 9-class `template` package + the generic-module delegation.
**Primary sources** `[CERT]`:
- `organized/provisioningNiagara/provisioningNiagara-wb/vineflower/com/tridium/provisioningNiagara/template/
  {BAbstractDeployStep,BDeployTemplateStep,BDeployApplicationStep,BUpgradeTemplateStep,BUpgradeApplicationStep,
  BUpdateConfigurationStep,BTemplateStationExt,ProvisionTemplateManager,ProvisioningBulkDeployUtil}.java`.

**Scope**: how provisioning pushes a template/application to a fleet. Like [Block 567] (batchJob) and [Block 511]
(BJob), this is a SPECIALIZATION of a reusable module (`com.tridium.template`); PV7 documents the provisioning
wrapper, not the template engine itself (parameterization/`.ntpl` format = a candidate `template` focus). Does
NOT re-open `BProvisioningStationExt` ([Block 571]) — uses it.

---

## 573.1 A thin specialization of the `template` module [CERT]

The provisioning template layer extends the generic `com.tridium.template` module `[CERT]`:
- `ProvisionTemplateManager extends TemplateManager` `[CERT] :7` — its `templateDir` resolves to
  `local:|file:^templateCache` `[CERT] :10-13` (templates are cached `.ntpl` files, `BNtplFile`, in the station's
  `^templateCache` directory).
- `ProvisioningBulkDeployUtil extends BulkDeployUtil` `[CERT] :70` — pulls in `BTemplateChannel`,
  `BTemplateConfig`, `BTemplateService`, `ApplicationTemplateUtil`, `BApplicationInstallSpecs`,
  `BInstallApplicationTemplateJob` `[CERT] :23-31` — the generic template service, its Fox channel, and its
  install job.

So provisioning does NOT reimplement templating; it wraps the `template` module to apply it across a fleet.

## 573.2 The deploy steps and the install call [CERT]

`abstract class BAbstractDeployStep extends BDeviceJobStep` `[CERT] :52` is the base; concrete steps `[CERT]`:
`BDeployTemplateStep`, `BDeployApplicationStep` (both extend `BAbstractDeployStep`), plus
`BUpgradeTemplateStep`, `BUpgradeApplicationStep`, `BUpdateConfigurationStep` (`BDeviceJobStep`). The actual push
is `BDeployTemplateStep.installTemplate(...)` `[CERT] :35-62`:
```java
deployedRoot.setDeployedTemplate(
   bulkDeploy.installTemplateToStation(deployedWorksheet, deployedRoot, details, util, opIn, componentDisplayNames));
```
It deploys via `ProvisioningBulkDeployUtil.installTemplateToStation` (inherited from `BulkDeployUtil`), threading
a `DeployedWorksheet`/`DeployedRoot` (the deploy plan), the step `details`, and a component **display-name map**
(`BNameMap`, merged per deployed-template parent `[CERT] :52-58`). So a deploy writes the template's components
into the subordinate's component space and records their display names.

## 573.3 Template vs Application [CERT]

The step distinguishes two flavors `[CERT] :44`: a plain **Template** and an **Application**
(`deployedWorksheet.templateType == "Application"`). `BDeployTemplateStep` handles templates and REJECTS an
Application worksheet (`details.failed(... "DeployTemplateStep.componentFileError")` `[CERT] :44-45`);
`BDeployApplicationStep` handles applications (an application = a template composed into a deployable station
config, `BApplicationInstallSpecs`/`ApplicationTemplateUtil`). Upgrade/update variants
(`BUpgrade*Step`/`BUpdateConfigurationStep`) re-apply a changed template to already-deployed instances.

## 573.4 Station-side and async [CERT]

`BTemplateStationExt extends BProvisioningStationExt` `[CERT] :56` is the per-station ext for template ops — so
it inherits the PV5 async-action protocol ([Block 571]): a bulk deploy across N stations reports each station's
completion through `asyncActionComplete`. It carries a `TemplateQuery`/`InstanceQuery` `[CERT] :222` to enumerate
which template instances are already deployed on a station (the basis for upgrade/diff).

## 573.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | ProvisionTemplateManager extends TemplateManager; templateDir = ^templateCache (.ntpl / BNtplFile) | [CERT] | ProvisionTemplateManager.java:7-13 | token-checked ✓ |
| 2 | ProvisioningBulkDeployUtil extends BulkDeployUtil; uses BTemplateService/Channel/Config + application specs | [CERT] | ProvisioningBulkDeployUtil.java:23-31,70 | token-checked ✓ |
| 3 | Deploy steps: BAbstractDeployStep (BDeviceJobStep) → BDeployTemplateStep/BDeployApplicationStep + upgrade/update variants | [CERT] | template/*.java:19-59 | token-checked ✓ |
| 4 | Push = bulkDeploy.installTemplateToStation(worksheet, root, details, util, op, displayNames) with BNameMap merge | [CERT] | BDeployTemplateStep.java:35-62 | token-checked ✓ |
| 5 | Template vs Application flavor split (templateType "Application"); BDeployTemplateStep rejects Application | [CERT] | :44-45 | token-checked ✓ |
| 6 | BTemplateStationExt extends BProvisioningStationExt (async via B571) + template instance query | [CERT] | BTemplateStationExt.java:56,222 | token-checked ✓ |

**Marker tally**: [CERT] ×6 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 6 of 6 rows token-verified
inline.

## Connections

- **[Block 567]** (PV1) — deploy steps are BDeviceJobSteps run by the batch engine across the fleet.
- **[Block 571]** (PV5) — BTemplateStationExt reports completion via the async-action protocol.
- **[Block 569]** (PV3) — sibling: software distribution is another supervisor→fleet push (modules vs templates).
- **`template` module** — the generic templating engine (`.ntpl`, parameter binding, BTemplateService); a
  candidate separate focus, out of provisioning scope.

## Open gaps (this block)

- The `.ntpl` template format, parameterization/binding, and `BApplicationInstallSpecs` internals are
  `template`-module territory — named, not opened (candidate `template` focus). Focus continues at PV8
  (credentials batch management).
