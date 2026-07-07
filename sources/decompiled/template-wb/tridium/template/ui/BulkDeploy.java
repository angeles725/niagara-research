package com.tridium.template.ui;

import com.tridium.excel.ui.ExcelUiUtils;
import com.tridium.template.BTemplateService;
import com.tridium.template.file.NtplUtil;
import com.tridium.template.ui.file.BBulkDeployPasswordPrompt;
import com.tridium.template.ui.installapp.BackupUiHandler;
import com.tridium.template.ui.installapp.CompatibilityMessageUiHandler;
import com.tridium.template.ui.installapp.ConfirmInstallApplicationTemplateUiHandler;
import com.tridium.template.ui.installapp.InstallingApplicationWorker;
import com.tridium.ui.wizard.step.StepModel;
import com.tridium.ui.wizard.step.StepWizardModel;
import com.tridium.ui.wizard.step.WizardStep;
import java.io.File;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import javax.baja.security.BPassword;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BStation;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.file.BFileChooser;
import javax.baja.ui.file.ExtFileFilter;
import javax.baja.ui.pane.BTextEditorPane;
import javax.baja.ui.wizard.BWizard;
import javax.baja.util.BServiceContainer;
import javax.baja.util.Lexicon;
import javax.baja.workbench.view.BWbComponentView;

public abstract class BulkDeploy extends Command {
   protected BWidget owner;
   protected BulkDeployUtil deployUtil;
   protected Level IMPORT_LOG_LEVEL = Level.FINE;
   protected static final Lexicon LEX = Lexicon.make("template");
   protected static final Logger LOG = Logger.getLogger("template");

   public BulkDeploy(BWidget owner, String label) {
      super(owner, label);
   }

   public BulkDeploy(BWidget owner, Lexicon lexicon, String keyBase) {
      super(owner, lexicon, keyBase);
   }

   public CommandArtifact doInvoke() throws Exception {
      return null;
   }

   public CommandArtifact doDeploy() throws Exception {
      if (ExcelUiUtils.informIfNoExcelSupportIsInstalledLocally(this.getShell())) {
         return null;
      } else {
         LOG.log(this.IMPORT_LOG_LEVEL, "Bulk deploy");
         BComponent root = this.initBulkDeploy();
         if (root == null) {
            return null;
         } else {
            BFileChooser chooser = BFileChooser.makeOpen(this.getShell());
            chooser.setCurrentDirectory(NtplUtil.getTemplateDirectoryOrd());
            chooser.addFilter(new ExtFileFilter("Excel Files", new String[]{"xlsx", "xls"}));
            BOrd excelOrd = chooser.show();
            if (excelOrd == null) {
               return null;
            } else {
               BIFile excelFile = (BIFile)excelOrd.resolve().get();
               File importFile = BFileSystem.INSTANCE.pathToLocalFile(excelFile.getFilePath());
               BulkDeployWorkbook wb = null;

               Object var15;
               try {
                  wb = BulkDeployWorkbook.load(importFile);
                  if (wb.isEncrypted()) {
                     wb.close();
                     BPassword password = BBulkDeployPasswordPrompt.getPassword(this.getOwner(), importFile);
                     if (password == BBulkDeployPasswordPrompt.DLG_CANCELLED) {
                        return null;
                     }

                     wb = BulkDeployWorkbook.load(importFile, password.getValue());
                     if (!wb.isValid()) {
                        BDialog.message(this.getOwner(), LEX.getText("bulkDeploy.excelImport.invalidPassword"));
                        return null;
                     }
                  } else if (!wb.isValid()) {
                     BDialog.message(this.getOwner(), LEX.getText("bulkDeploy.excelImport.invalidFile"));
                     return null;
                  }

                  if (wb.hasPrivateData()
                     && (wb.usesOldExcelFormat() || !wb.isEncrypted())
                     && 4
                        != BDialog.confirm(
                           this.getOwner(),
                           LEX.getText("bulkDeploy.excelImport.insecureFormat.title"),
                           LEX.getText("bulkDeploy.excelImport.insecureFormat.message")
                        )) {
                     return null;
                  }

                  if (!wb.hasApplicationTemplate()) {
                     TemplateDeployWorker worker = new TemplateDeployWorker(wb, root, this.deployUtil);
                     BTemplateDeployProgressDialog.open(this.owner.getShell(), LEX.getText("bulkDeploy.progress.title"), worker);
                     if (worker.getDeployMessages() != null && !worker.getDeployMessages().isEmpty()) {
                        StringJoiner allResponses = new StringJoiner("\n");
                        worker.getDeployMessages().forEach((key, responseList) -> {
                           for (String responseMessage : responseList) {
                              allResponses.add(key + ": " + responseMessage);
                           }
                        });
                        BTextEditorPane resultsPane = new BTextEditorPane(
                           LEX.getText("bulkDeploy.excelImport.result.message", new Object[]{allResponses.toString()}), 12, 100, false
                        );
                        BDialog.message(this.owner.getShell(), LEX.getText("bulkDeploy.progress.title"), resultsPane);
                     }

                     return null;
                  }

                  if (root instanceof BStation) {
                     try {
                        BWizard.open(this.getOwner(), this.makeWizardModel((BStation)root, excelFile, wb));
                     } catch (Exception var13) {
                        BDialog.error(this.getOwner(), this.getLabel(), LEX.get("installApplication.cannotInstall"), var13);
                     }

                     return null;
                  }

                  BDialog.error(
                     this.getShell(),
                     LEX.getText("bulkDeploy.excelImport.applicationNotAllowedTitle"),
                     LEX.getText("bulkDeploy.excelImport.applicationNotAllowed")
                  );
                  var15 = null;
               } finally {
                  if (wb != null) {
                     wb.close();
                  }
               }

               return (CommandArtifact)var15;
            }
         }
      }
   }

   protected BComponent initBulkDeploy() {
      this.owner = this.getOwner();
      if (!this.owner.getType().is(BWbComponentView.TYPE)) {
         LOG.log(Level.WARNING, "Bulk Deploy command owner should be a manager view");
         return null;
      } else {
         BObject ownerObject = ((BWbComponentView)this.owner).getCurrentValue();
         if (!ownerObject.getType().is(BComponent.TYPE)) {
            LOG.log(Level.WARNING, "Bulk Deploy command owner target should be a component");
            return null;
         } else {
            BComponent ownerComponent = (BComponent)ownerObject;
            this.deployUtil = new BulkDeployUtil();
            BTemplateService templateService = this.deployUtil.getTemplateService(ownerComponent);
            if (templateService == null) {
               return null;
            } else {
               BComponentSpace space = templateService.getComponentSpace();
               if (space == null) {
                  return null;
               } else {
                  BComponent root = space.getRootComponent();
                  BServiceContainer[] serviceContainer = (BServiceContainer[])root.getChildren(BServiceContainer.class);
                  return serviceContainer != null && serviceContainer.length != 0 ? root : null;
               }
            }
         }
      }
   }

   protected BulkDeployWorkbook getDeployWorkbook() throws Exception {
      BFileChooser chooser = BFileChooser.makeOpen(this.getShell());
      chooser.setCurrentDirectory(NtplUtil.getTemplateDirectoryOrd());
      chooser.addFilter(new ExtFileFilter("Excel Files", new String[]{"xlsx", "xls"}));
      BOrd excelOrd = chooser.show();
      if (excelOrd == null) {
         return null;
      } else {
         BIFile excelFile = (BIFile)excelOrd.resolve().get();
         File importFile = BFileSystem.INSTANCE.pathToLocalFile(excelFile.getFilePath());
         BulkDeployWorkbook wb = BulkDeployWorkbook.load(importFile);
         if (wb.isEncrypted()) {
            wb.close();
            BPassword password = BBulkDeployPasswordPrompt.getPassword(this.getOwner(), importFile);
            if (password == BBulkDeployPasswordPrompt.DLG_CANCELLED) {
               return null;
            }

            wb = BulkDeployWorkbook.load(importFile, password.getValue());
            if (!wb.isValid()) {
               BDialog.message(this.getOwner(), LEX.getText("bulkDeploy.excelImport.invalidPassword"));
               wb.close();
               return null;
            }
         } else if (!wb.isValid()) {
            BDialog.message(this.getOwner(), LEX.getText("bulkDeploy.excelImport.invalidFile"));
            wb.close();
            return null;
         }

         if (wb.hasPrivateData()
            && (wb.usesOldExcelFormat() || !wb.isEncrypted())
            && 4
               != BDialog.confirm(
                  this.getOwner(), LEX.getText("bulkDeploy.excelImport.insecureFormat.title"), LEX.getText("bulkDeploy.excelImport.insecureFormat.message")
               )) {
            wb.close();
            return null;
         } else {
            return wb;
         }
      }
   }

   private StepWizardModel makeWizardModel(BStation station, BIFile excelFile, BulkDeployWorkbook workbook) {
      ApplicationTemplateInstallUtil installInfo = new ApplicationTemplateInstallUtil(station, excelFile, workbook);
      installInfo.checkApplicationTemplateFileCompatibility();
      WizardStep firstStep = null;
      if (installInfo.hasCompatibilityMessage()) {
         firstStep = CompatibilityMessageUiHandler.makeWizardStep(installInfo);
      }

      WizardStep backupStep = BackupUiHandler.makeWizardStep(installInfo);
      WizardStep confirmStep = ConfirmInstallApplicationTemplateUiHandler.makeWizardStep(installInfo);
      WizardStep installingStep = InstallingApplicationWorker.makeWizardStep(installInfo);
      if (firstStep == null) {
         firstStep = backupStep;
      } else {
         firstStep.setNext(backupStep);
      }

      backupStep.setNext(backupStep).setNext(confirmStep).setNext(installingStep);
      StepModel stepModel = StepModel.make(firstStep);
      return new StepWizardModel(stepModel, LEX.get("installApplication.title"));
   }
}
