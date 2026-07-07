package com.tridium.template.ui.installapp;

import com.tridium.backup.ui.BBackupManager;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.BTemplateService;
import com.tridium.template.application.ApplicationTemplateUtil;
import com.tridium.template.application.BApplicationInstallSpecs;
import com.tridium.template.job.BInstallApplicationTemplateJob;
import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.template.ui.BulkDeployUtil;
import com.tridium.template.ui.file.BWbDeployableNtplFile;
import com.tridium.ui.wizard.step.WizardStep;
import com.tridium.ui.wizard.step.util.ProgressTextPaneUiHandler;
import com.tridium.ui.wizard.step.util.ProgressTextPaneUiHandler.WorkerRunnable;
import com.tridium.util.JobProgressMonitor;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.FilePath;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.job.BJobState;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Subscriber;

public class InstallingApplicationWorker extends WorkerRunnable {
   private final ApplicationTemplateInstallUtil installInfo;
   private static final List<String> APPLICATION_TEMPLATE_FLAG_HACK = Collections.singletonList(";");

   public static WizardStep makeWizardStep(ApplicationTemplateInstallUtil installInfo) {
      String title;
      String description;
      switch (installInfo.getSourceType()) {
         case 1:
         default:
            title = "installApplication.installingApplicationTemplate.title";
            description = "installApplication.installingApplicationTemplate.description";
            break;
         case 2:
            title = "installApplication.installingApplicationExcelFile.title";
            description = "installApplication.installingApplicationExcelFile.description";
      }

      return WizardUtil.makeStep(title, description, new ProgressTextPaneUiHandler(new InstallingApplicationWorker(installInfo)));
   }

   private InstallingApplicationWorker(ApplicationTemplateInstallUtil installInfo) {
      this.installInfo = installInfo;
   }

   public void workIt() {
      Context cx = this.getContext();
      Subscriber subscriber = Subscriber.make(event -> {});
      BFoxSession session = (BFoxSession)BOrd.toSession(this.installInfo.getStation());
      Object pauseToken = null;

      try {
         pauseToken = session.pauseActivityMonitor();
         String stationName = this.installInfo.getStation().getStationDisplayName(null);
         BWbDeployableNtplFile sourceTemplateFile = this.installInfo.getApplicationTemplateFile();
         if (sourceTemplateFile == null) {
            throw new LocalizableRuntimeException("template", "installApplication.templateFileNotFound");
         }

         String sourceTemplateFileName = sourceTemplateFile.getFileName();
         if (BackupStepArtifact.canPerformBackup(this.getStepModel())) {
            this.message("installApplication.backingUpStation", stationName);
            BOrd backupJobOrd = this.startBackup();
            BJobState jobState = new JobProgressMonitor(
                  backupJobOrd, this.installInfo.getStation(), JobProgressMonitor.makeJobLogItemMessageConsumer(x$0 -> this.message(x$0), cx)
               )
               .waitAndReadLog()
               .getJobState();
            if (!BJobState.success.equals(jobState)) {
               this.message("installApplication.failedBackup", stationName);
               return;
            }
         }

         BTemplateService templateService = this.resolveTemplateService();
         this.message("installApplication.copyingFile", sourceTemplateFileName, stationName);
         BIFile applicationTemplateFile = ApplicationTemplateUtil.copyApplicationTemplateToStation(sourceTemplateFile, this.installInfo.getStation());
         this.message("installApplication.finishedCopyingFile", applicationTemplateFile.getFileName());
         this.message("installApplication.startingApplicationTemplateInstallationJob");
         if (this.installInfo.getSourceType() == 2 && this.installInfo.getWorkbook() == null) {
            this.message("installApplication.excelFileWorkbookNotFound", this.installInfo.getSourceFile().getFileName());
            return;
         }

         if (!this.installInfo.isUpgradeMode() || this.installInfo.doesStationSupportUpgradeAndOptionals()) {
            BOrd applicationTemplateFileOrd = applicationTemplateFile.getOrdInSession();
            BOrd installApplicationTemplateJobOrd;
            if (this.installInfo.doesStationSupportUpgradeAndOptionals()) {
               BApplicationInstallSpecs installSpecs = BApplicationInstallSpecs.make();
               installSpecs.setCheckModules(false);
               installSpecs.setUpgrade(this.installInfo.isUpgradeMode());
               installSpecs.setFileOrd(applicationTemplateFileOrd);
               BOrdList toBeRemoved = BOrdList.DEFAULT;
               if (!this.installInfo.isUpgradeMode()) {
                  if (this.installInfo.getSourceType() == 2) {
                     toBeRemoved = this.getSelectedOptionalsFromWorkbook(sourceTemplateFile);
                  } else if (this.installInfo.doesTemplateHaveOptionals() && !OptionalComponentsArtifact.allOptionalsAreSelected(this.getStepModel())) {
                     toBeRemoved = OptionalComponentsArtifact.getOptionalComponentsToBeRemoved(this.getStepModel());
                  }
               }

               installSpecs.setToBeRemoved(toBeRemoved);
               installApplicationTemplateJobOrd = templateService.installApplication(installSpecs);
            } else {
               installApplicationTemplateJobOrd = templateService.installApplicationTemplate(applicationTemplateFileOrd);
            }

            JobProgressMonitor jobMonitor = new JobProgressMonitor(
               installApplicationTemplateJobOrd, this.installInfo.getStation(), JobProgressMonitor.makeJobLogItemMessageConsumer(x$0 -> this.message(x$0), cx)
            );
            BInstallApplicationTemplateJob job = (BInstallApplicationTemplateJob)jobMonitor.waitAndReadLog();
            if (BJobState.success.equals(job.getJobState())) {
               if (this.installInfo.isUpgradeMode()) {
                  this.message("upgradeApplication.applicationTemplateInstalled");
               } else {
                  this.message("installApplication.configuringApplicationTemplate");
                  switch (this.installInfo.getSourceType()) {
                     case 1:
                     default:
                        BWbDeployableNtplFile.doPostDeploy(
                           sourceTemplateFile,
                           this.getProgressTextBox().getShell(),
                           this.installInfo.getStation(),
                           APPLICATION_TEMPLATE_FLAG_HACK,
                           this::add,
                           cx
                        );
                        break;
                     case 2:
                        this.applyTemplateConfigs(sourceTemplateFile);
                  }

                  this.message("installApplication.applicationTemplateInstalled");
               }

               return;
            } else {
               this.message("installApplication.applicationTemplateFailedToInstall");
               return;
            }
         }

         this.message("upgradeApplication.stationDoesntSupportUpgrade", this.installInfo.getSourceFile().getFileName());
      } catch (Exception var17) {
         this.error(var17);
         return;
      } finally {
         subscriber.unsubscribeAll();
         session.resumeActivityMonitor(pauseToken);
      }
   }

   private BOrdList getSelectedOptionalsFromWorkbook(BIFile applicationTemplateFile) {
      if (!(applicationTemplateFile instanceof BWbDeployableNtplFile)) {
         return BOrdList.DEFAULT;
      } else {
         BWbDeployableNtplFile templateFile = (BWbDeployableNtplFile)applicationTemplateFile;
         BulkDeployUtil bulkDeployUtil = new BulkDeployUtil();
         BulkDeployUtil.DeployedWorksheet deployedWorksheet = this.installInfo.getDeployedWorksheet();
         BulkDeployUtil.DeployedRoot deployedRoot = this.installInfo.getDeployedRoot();
         return bulkDeployUtil.getSelectedOptionalsFromWorkbook(templateFile, deployedWorksheet, deployedRoot);
      }
   }

   private void applyTemplateConfigs(BWbDeployableNtplFile sourceTemplateFile) {
      BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(this.installInfo.getStation());
      if (templateConfig == null) {
         this.message("bulkDeploy.excelImport.invalidTemplateError", sourceTemplateFile.getTitle());
      } else {
         templateConfig.lease();
         BulkDeployUtil bulkDeployUtil = new BulkDeployUtil();
         BulkDeployUtil.DeployedWorksheet deployedWorksheet = this.installInfo.getDeployedWorksheet();
         BulkDeployUtil.DeployedRoot deployedRoot = this.installInfo.getDeployedRoot();
         if (deployedRoot.getConfigs() != null) {
            this.message("installApplication.applyingConfigs");
            BulkDeployUtil.setConfigsFromWorksheet(deployedWorksheet, deployedRoot, this.installInfo.getStation(), templateConfig, false);
         }

         if (deployedRoot.getOptionalConfigs() != null) {
            this.message("installApplication.applyingOptionalConfigs");
            BulkDeployUtil.setOptionalConfigsFromWorksheet(deployedWorksheet, deployedRoot, this.installInfo.getStation(), templateConfig, false);
         }

         if (deployedRoot.getTags() != null) {
            this.message("installApplication.applyingTags");
            bulkDeployUtil.setTagsFromWorksheet(deployedWorksheet, deployedRoot, this.installInfo.getStation());
         }

         if (deployedRoot.isEnableHistories()) {
            this.message("installApplication.applyingHistories");
            BHistoryExt[] unconfigurableHistories = BulkDeployUtil.unconfigurableHistoryExtensions(this.installInfo.getStation());

            for (BHistoryExt historyExt : unconfigurableHistories) {
               historyExt.setEnabled(true);
            }
         }
      }
   }

   private BTemplateService resolveTemplateService() {
      return (BTemplateService)BOrd.make("service:template:TemplateService").get(this.installInfo.getStation());
   }

   private BOrd startBackup() throws IOException {
      BFileSystem.INSTANCE.makeDir(new FilePath("~backups"));
      BFoxSession session = (BFoxSession)BOrd.toSession(this.installInfo.getStation());
      String backupFilePath = "~backups/" + BBackupManager.makeDefaultBackupFileName(session.getStationName());
      return BBackupManager.submitBackupJob(backupFilePath, session);
   }

   private void message(String lexKey, Object... args) {
      this.add(WizardUtil.LEX.getText(lexKey, args));
   }

   private void error(Throwable error) {
      this.add(error);
   }
}
