package com.tridium.template.ui.installapp;

import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.ui.wizard.step.StepModel;
import com.tridium.ui.wizard.step.StepWizardModel;
import com.tridium.ui.wizard.step.WizardStep;
import com.tridium.workbench.shell.BFontSize;
import com.tridium.workbench.shell.BGeneralOptions;
import javax.baja.gx.Size;
import javax.baja.sys.BStation;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.util.UiLexicon;
import javax.baja.ui.wizard.BWizard;

public class InstallApplicationCommand extends Command {
   private BStation station;

   public InstallApplicationCommand(BWidget owner, BStation station) {
      super(owner, UiLexicon.bajaui(), "commands.installApplication");
      this.station = station;
   }

   public CommandArtifact doInvoke() {
      try {
         BWizard.open(this.getOwner(), makeWizardModel(this.station));
      } catch (Exception var2) {
         BDialog.error(this.getOwner(), this.getLabel(), WizardUtil.LEX.get("installApplication.cannotInstall"), var2);
      }

      return null;
   }

   private static StepWizardModel makeWizardModel(BStation station) {
      ApplicationTemplateInstallUtil installInfo = new ApplicationTemplateInstallUtil(station);
      WizardStep selectApplicationTemplateStep = SelectApplicationTemplateUiHandler.makeWizardStep(installInfo);
      WizardStep compatibilityMessageStep = CompatibilityMessageUiHandler.makeWizardStep(installInfo);
      WizardStep optionalsStep = SelectOptionalComponentsUiHandler.makeWizardStep(installInfo);
      WizardStep backupStep = BackupUiHandler.makeWizardStep(installInfo);
      WizardStep confirmStep = ConfirmInstallApplicationTemplateUiHandler.makeWizardStep(installInfo);
      WizardStep installingStep = InstallingApplicationWorker.makeWizardStep(installInfo);
      selectApplicationTemplateStep.setNext(compatibilityMessageStep).setNext(optionalsStep).setNext(backupStep).setNext(confirmStep).setNext(installingStep);
      StepModel stepModel = StepModel.make(selectApplicationTemplateStep);
      boolean isLargeFont = BGeneralOptions.make().getFontSize() == BFontSize.large;
      Size scaledSize = new Size(isLargeFont ? 800.0 : 600.0, isLargeFont ? 480.0 : 400.0);
      return new StepWizardModel(stepModel, WizardUtil.LEX.get("installApplication.title"), scaledSize);
   }
}
