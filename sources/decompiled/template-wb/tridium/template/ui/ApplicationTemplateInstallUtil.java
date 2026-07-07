package com.tridium.template.ui;

import com.tridium.install.BVersion;
import com.tridium.template.BTemplateService;
import com.tridium.template.file.BNtplFile;
import com.tridium.template.file.TemplateManager;
import com.tridium.template.file.TemplateManager.TemplateInfo;
import com.tridium.template.ui.file.BWbDeployableNtplFile;
import com.tridium.template.ui.installapp.WizardUtil;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.baja.file.BDataFile;
import javax.baja.file.BIFile;
import javax.baja.file.types.application.BExcelFile;
import javax.baja.naming.BOrd;
import javax.baja.sys.BStation;
import javax.baja.util.Version;

public final class ApplicationTemplateInstallUtil {
   public static final int TEMPLATE_FILE = 1;
   public static final int EXCEL_FILE = 2;
   private final BStation station;
   private final BIFile suppliedFile;
   private final BulkDeployWorkbook suppliedWorkbook;
   private final boolean upgradeMode;
   private BDataFile loadedSelectedFile = null;
   private BWbDeployableNtplFile applicationTemplateFile = null;
   private BulkDeployUtil.DeployedWorksheet deployedWorksheet = null;
   private BulkDeployUtil.DeployedRoot deployedRoot = null;
   private boolean templateValidationComplete = false;
   private String compatibilityMessage = null;
   private boolean incompatible = false;
   private boolean stationSupportsUpgradeAndOptionals = false;
   private boolean templateHasOptionals = false;

   public ApplicationTemplateInstallUtil(BStation station) {
      this(station, null, null, false);
   }

   public ApplicationTemplateInstallUtil(BStation station, BNtplFile upgradeApplicationFile, boolean upgradeMode) {
      this(station, upgradeApplicationFile, null, upgradeMode);
   }

   public ApplicationTemplateInstallUtil(BStation station, BIFile suppliedFile, BulkDeployWorkbook suppliedWorkbook) {
      this(station, suppliedFile, suppliedWorkbook, false);
   }

   private ApplicationTemplateInstallUtil(BStation station, BIFile suppliedFile, BulkDeployWorkbook suppliedWorkbook, boolean upgradeMode) {
      this.station = station;
      this.suppliedFile = suppliedFile;
      this.suppliedWorkbook = suppliedWorkbook;
      this.upgradeMode = upgradeMode;
   }

   public BStation getStation() {
      return this.station;
   }

   public BIFile getSuppliedFile() {
      return this.suppliedFile;
   }

   public BulkDeployWorkbook getSuppliedWorkbook() {
      return this.suppliedWorkbook;
   }

   public void loadApplicationTemplateFile() {
      this.loadApplicationTemplateFile(null);
   }

   public void loadApplicationTemplateFile(BIFile file) {
      if (file == null) {
         file = this.loadedSelectedFile;
      }

      if (file == null) {
         file = this.getSuppliedFile();
      }

      if (!(file instanceof BDataFile)) {
         this.clearStateVariables();
      } else {
         BDataFile selectedFile = (BDataFile)file;
         if (this.loadedSelectedFile == null || !selectedFile.toPathString().equals(this.loadedSelectedFile.toPathString())) {
            this.clearStateVariables();
            BulkDeployUtil.DeployedWorksheet newDeployedWorksheet = null;
            BulkDeployUtil.DeployedRoot newDeployedRoot = null;
            BWbDeployableNtplFile templateFile = null;
            if (selectedFile instanceof BWbDeployableNtplFile) {
               templateFile = (BWbDeployableNtplFile)selectedFile;
            } else if (selectedFile instanceof BExcelFile) {
               if (this.getSuppliedWorkbook() == null) {
                  return;
               }

               List<BulkDeployUtil.DeployedWorksheet> deployedWorksheets = BulkDeployUtil.loadDeployedWorksheets(this.getSuppliedWorkbook());
               if (deployedWorksheets == null || deployedWorksheets.isEmpty()) {
                  return;
               }

               newDeployedWorksheet = deployedWorksheets.get(deployedWorksheets.size() - 1);
               newDeployedRoot = newDeployedWorksheet.deployedRoots.get(newDeployedWorksheet.deployedRoots.size() - 1);
               TemplateManager tmInstance = TemplateManager.INSTANCE;
               TemplateInfo deployTemplateInfo = tmInstance.getTemplate(newDeployedWorksheet.uid, newDeployedWorksheet.vendor);
               if (deployTemplateInfo == null) {
                  return;
               }

               BNtplFile ntplFile = deployTemplateInfo.getNtplFile();
               if (ntplFile instanceof BWbDeployableNtplFile) {
                  templateFile = (BWbDeployableNtplFile)ntplFile;
               } else {
                  templateFile = BWbDeployableNtplFile.make(ntplFile);
               }
            }

            if (templateFile != null) {
               if (templateFile.getTemplateManifest().isApplication) {
                  this.loadedSelectedFile = selectedFile;
                  this.applicationTemplateFile = templateFile;
                  this.deployedWorksheet = newDeployedWorksheet;
                  this.deployedRoot = newDeployedRoot;
               }
            }
         }
      }
   }

   public void checkApplicationTemplateFileCompatibility() {
      this.loadApplicationTemplateFile();
      if (!this.templateValidationComplete) {
         BTemplateService templateService = resolveTemplateService(this.getStation());
         Version remoteVersion = (Version)templateService.fw(404, "template", null, null, null);
         this.stationSupportsUpgradeAndOptionals = remoteVersion.compareTo(new Version("4.9")) >= 0;
         StringBuilder compatibilityMessageBuilder = new StringBuilder();
         boolean compatibilityWarnings = false;
         if (this.applicationTemplateFile == null) {
            this.incompatible = true;
            compatibilityMessageBuilder.append(WizardUtil.LEX.getText("installApplication.compatibility.noTemplate"));
            compatibilityMessageBuilder.append('\n');
         } else {
            List<Map<String, BVersion>> missingDependencies = this.applicationTemplateFile.checkRemoteModuleDependencies(this.getStation());
            Map<String, BVersion> missingModules = missingDependencies.get(0);
            Map<String, BVersion> mismatchedModules = missingDependencies.get(1);
            Map<String, BVersion> missingPxModules = missingDependencies.get(2);
            this.incompatible = missingModules.size() > 0;
            compatibilityWarnings = mismatchedModules.size() > 0 || missingPxModules.size() > 0;

            for (String module : missingModules.keySet()) {
               compatibilityMessageBuilder.append(WizardUtil.LEX.getText("installApplication.compatibility.missingModule", new Object[]{module}));
               compatibilityMessageBuilder.append('\n');
            }

            for (Entry<String, BVersion> moduleSpec : mismatchedModules.entrySet()) {
               compatibilityMessageBuilder.append(
                  WizardUtil.LEX
                     .getText("installApplication.compatibility.mismatchedModule", new Object[]{moduleSpec.getKey(), moduleSpec.getValue().toString()})
               );
               compatibilityMessageBuilder.append('\n');
            }

            for (String module : missingPxModules.keySet()) {
               compatibilityMessageBuilder.append(WizardUtil.LEX.getText("installApplication.compatibility.missingPxModule", new Object[]{module}));
               compatibilityMessageBuilder.append('\n');
            }

            this.templateHasOptionals = this.applicationTemplateFile.getTemplateManifest().optional.size() > 0;
            if (this.templateHasOptionals && !this.stationSupportsUpgradeAndOptionals) {
               compatibilityWarnings = true;
               compatibilityMessageBuilder.append(WizardUtil.LEX.getText("installApplication.compatibility.optionalsNotSupported"));
               compatibilityMessageBuilder.append('\n');
            }
         }

         if (this.incompatible) {
            compatibilityMessageBuilder.append(WizardUtil.LEX.getText("installApplication.compatibility.failed"));
         } else if (compatibilityWarnings) {
            compatibilityMessageBuilder.append(WizardUtil.LEX.getText("installApplication.compatibility.warnings"));
         }

         String message = compatibilityMessageBuilder.toString();
         this.compatibilityMessage = message.isEmpty() ? null : message;
         this.templateValidationComplete = true;
      }
   }

   public BIFile getSourceFile() {
      return this.loadedSelectedFile;
   }

   public int getSourceType() {
      if (this.loadedSelectedFile instanceof BExcelFile) {
         return 2;
      } else {
         return this.loadedSelectedFile instanceof BNtplFile ? 1 : 0;
      }
   }

   public BulkDeployWorkbook getWorkbook() {
      return this.suppliedWorkbook;
   }

   public BWbDeployableNtplFile getApplicationTemplateFile() {
      this.loadApplicationTemplateFile();
      return this.applicationTemplateFile;
   }

   public boolean isUpgradeMode() {
      return this.upgradeMode;
   }

   public boolean doesTemplateHaveOptionals() {
      this.checkApplicationTemplateFileCompatibility();
      return this.templateHasOptionals;
   }

   public boolean doesStationSupportUpgradeAndOptionals() {
      this.checkApplicationTemplateFileCompatibility();
      return this.stationSupportsUpgradeAndOptionals;
   }

   public BulkDeployUtil.DeployedWorksheet getDeployedWorksheet() {
      this.loadApplicationTemplateFile();
      return this.deployedWorksheet;
   }

   public BulkDeployUtil.DeployedRoot getDeployedRoot() {
      this.loadApplicationTemplateFile();
      return this.deployedRoot;
   }

   public boolean isTemplateValidationComplete() {
      this.checkApplicationTemplateFileCompatibility();
      return this.templateValidationComplete;
   }

   public boolean hasCompatibilityMessage() {
      this.checkApplicationTemplateFileCompatibility();
      return this.compatibilityMessage != null;
   }

   public String getCompatibilityMessage() {
      this.checkApplicationTemplateFileCompatibility();
      return this.compatibilityMessage;
   }

   public boolean isIncompatible() {
      this.checkApplicationTemplateFileCompatibility();
      return this.incompatible;
   }

   private static BTemplateService resolveTemplateService(BStation station) {
      return (BTemplateService)BOrd.make("service:template:TemplateService").get(station);
   }

   private void clearStateVariables() {
      this.loadedSelectedFile = null;
      this.applicationTemplateFile = null;
      this.deployedWorksheet = null;
      this.deployedRoot = null;
      this.templateValidationComplete = false;
      this.compatibilityMessage = null;
      this.incompatible = false;
      this.stationSupportsUpgradeAndOptionals = false;
      this.templateHasOptionals = false;
   }
}
