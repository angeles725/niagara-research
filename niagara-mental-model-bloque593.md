# Block 593 — The application-template install wizard: a guided flow (select → optional-components → compatibility → BACKUP → confirm) whose worker runs backup-THEN-install — because an application install replaces the station tree, a full station backup is offered by default and taken before the swap

**Session**: 2026-08-28
**Focus**: `template-wb` (gap TW3 — the `installapp/` wizard; the guided, safety-wrapped application install)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the 13-class `installapp/` package; the step flow and the
backup-then-install worker token-verified inline.
**Primary sources** `[CERT]`:
- `organized/template/template-wb/vineflower/com/tridium/template/ui/installapp/{SelectApplicationTemplateUiHandler,
  SelectOptionalComponentsUiHandler,CompatibilityMessageUiHandler,BackupUiHandler,
  ConfirmInstallApplicationTemplateUiHandler,InstallingApplicationWorker,InstallApplicationCommand}.java`.

**Scope**: the Workbench wizard that installs an application template onto the current station. It drives the rt
installer ([Block 578] T2) and is reused by bulk deploy ([Block 592] TW2). Does NOT re-open the rt install
mechanics ([Block 578]) or the backup service ([Block 39]) — orchestrates them.

---

## 593.1 The step flow [CERT]

`InstallApplicationCommand extends Command` `[CERT] :18` launches a `StepWizardModel` of `WidgetUiHandler` steps,
each producing a `StepArtifact` (its collected state, `AbstractStepArtifact extends WidgetStepArtifact`
`[CERT] :6`). The steps `[CERT]`:
1. `SelectApplicationTemplateUiHandler` `[CERT] :24` — choose the application `.ntpl`.
2. `SelectOptionalComponentsUiHandler` `[CERT] :27` — pick which OPTIONAL components to include (the manifest
   `<optionals>`, [Block 580] T3 / [Block 578] keep-vs-remove).
3. `CompatibilityMessageUiHandler` `[CERT] :14` — show the module-compatibility result ([Block 578 §578.2]:
   missing modules block, mismatched warn).
4. `BackupUiHandler` `[CERT] :19` — a **checkbox "backup before install", defaulting to TRUE** `[CERT] :33`.
5. `ConfirmInstallApplicationTemplateUiHandler` `[CERT] :14` — final confirmation.
6. `InstallingApplicationWorker extends WorkerRunnable` `[CERT] :31` — the executor.

## 593.2 The worker: backup, THEN install [CERT]

`InstallingApplicationWorker` `[CERT]` runs the install as a monitored job sequence. When the backup box was
checked `[CERT] :73-75`:
```java
BOrd backupJobOrd = this.startBackup();
JobProgressMonitor... (backupJobOrd, this.installInfo.getStation(), ... message consumer ...)   // wait on backup
```
`startBackup()` `[CERT] :215-218`:
```java
BFileSystem.INSTANCE.makeDir(new FilePath("~backups"));
String backupFilePath = "~backups/" + BBackupManager.makeDefaultBackupFileName(session.getStationName());
return BBackupManager.submitBackupJob(backupFilePath, session);
```
So it submits a FULL station backup to `~backups/<default-name>` via `BBackupManager` ([Block 39]), waits for it
(JobProgressMonitor), and only THEN runs the `ApplicationTemplateInstaller` ([Block 578]) to perform the tree
swap. The backup is taken BEFORE the destructive step, not after.

## 593.3 Why the ceremony [CERT-synthesis]

An application-template install is the most destructive template operation: it CLEARS and REPLACES the station's
application component tree under a `ReplacingContext` ([Block 578] §578.3). The wizard therefore wraps it in four
safety layers the rt installer alone does not impose: (1) an explicit optional-components choice (you decide what
survives), (2) a compatibility GATE (missing modules abort before anything changes), (3) a **default-on
pre-install backup** (a rollback point taken before the swap), and (4) an explicit confirm. The `BackupUiHandler`
defaulting to TRUE is the important operational default — an engineer applying an application template to a live
station gets a restore point unless they deliberately opt out. Bulk deploy ([Block 592]) reuses this exact worker,
so a fleet-wide Excel deploy also backs up each station first.

## 593.4 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | InstallApplicationCommand launches a StepWizardModel of WidgetUiHandler steps with StepArtifacts | [CERT] | InstallApplicationCommand.java:18; AbstractStepArtifact.java:6 | token-checked ✓ |
| 2 | Steps: Select template → Select optional components → Compatibility message → Backup → Confirm → InstallingApplicationWorker | [CERT] | installapp/*.java:14-31 | token-checked ✓ |
| 3 | BackupUiHandler = checkbox defaulting to TRUE | [CERT] | BackupUiHandler.java:33 | token-checked ✓ |
| 4 | Worker runs backup THEN install: startBackup → BBackupManager.submitBackupJob(~backups/...) waited via JobProgressMonitor, then installer | [CERT] | InstallingApplicationWorker.java:73-75,215-218 | token-checked ✓ |
| 5 | Ceremony wraps the destructive B578 tree-swap in optional-choice + compat gate + default backup + confirm | [CERT-synthesis] | rows 1-4 + [B578] | reasoned ✓ |

**Marker tally**: [CERT] ×4 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 4 of 5
rows token-verified inline.

## Connections

- **[Block 578]** (T2) — the rt ApplicationTemplateInstaller this wizard drives (with the ReplacingContext swap).
- **[Block 592]** (TW2) — bulk deploy reuses `InstallingApplicationWorker` (so fleet deploys back up per station).
- **[Block 39]** — `BBackupManager`/BackupService, the pre-install backup this submits.
- **[Block 580]** (T3) — the manifest `<optionals>` the optional-components step chooses from.

## Open gaps (this block)

- The optional-components chooser's exact node model (`OptionalComponentsArtifact`/`SelectOptionalComponentsUiHandler`
  tree) is UI detail, low value. Focus continues at TW4/TW5 (Relation editor + .ntpl WB file integration — likely
  collapsed into one tail block).
