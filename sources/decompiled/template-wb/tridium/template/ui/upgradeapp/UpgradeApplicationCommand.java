package com.tridium.template.ui.upgradeapp;

import com.tridium.template.file.BNtplFile;
import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.template.ui.installapp.BackupUiHandler;
import com.tridium.template.ui.installapp.CompatibilityMessageUiHandler;
import com.tridium.template.ui.installapp.ConfirmInstallApplicationTemplateUiHandler;
import com.tridium.template.ui.installapp.InstallingApplicationWorker;
import com.tridium.template.ui.installapp.WizardUtil;
import com.tridium.ui.wizard.step.StepModel;
import com.tridium.ui.wizard.step.StepWizardModel;
import com.tridium.ui.wizard.step.WizardStep;
import javax.baja.naming.BOrd;
import javax.baja.sys.BStation;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.util.UiLexicon;
import javax.baja.ui.wizard.BWizard;
import javax.baja.util.Lexicon;

public class UpgradeApplicationCommand extends Command {
   private static final Lexicon LEX = Lexicon.make("template");
   private BStation station;
   private BOrd upgradeFileOrd;

   public UpgradeApplicationCommand(BWidget owner, BStation station, BOrd upgradeFileOrd) {
      super(owner, UiLexicon.bajaui(), "commands.upgradeApplication");
      this.station = station;
      this.upgradeFileOrd = upgradeFileOrd;
   }

   public CommandArtifact doInvoke() {
      try {
         BWizard.open(this.getOwner(), makeWizardModel(this.station, (BNtplFile)this.upgradeFileOrd.get()));
      } catch (Exception var2) {
         BDialog.error(this.getOwner(), this.getLabel(), WizardUtil.LEX.get("upgradeApplication.cannotUpgrade"), var2);
      }

      return null;
   }

   private static StepWizardModel makeWizardModel(BStation station, BNtplFile applicationTemplateFile) {
      ApplicationTemplateInstallUtil installInfo = new ApplicationTemplateInstallUtil(station, applicationTemplateFile, true);
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

      backupStep.setNext(confirmStep).setNext(installingStep);
      StepModel stepModel = StepModel.make(firstStep);
      return new StepWizardModel(stepModel, WizardUtil.LEX.get("upgradeApplication.title"));
   }
}
