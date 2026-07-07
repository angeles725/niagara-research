package com.tridium.template.ui.installapp;

import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.ui.wizard.step.IWizardView;
import com.tridium.ui.wizard.step.StepArtifact;
import com.tridium.ui.wizard.step.StepModel;
import com.tridium.ui.wizard.step.WidgetUiHandler;
import com.tridium.ui.wizard.step.WizardStep;
import com.tridium.ui.wizard.step.WidgetUiHandler.WidgetStepArtifact;
import javax.baja.sys.Context;
import javax.baja.ui.BLabel;
import javax.baja.ui.enums.BHalign;

public class ConfirmInstallApplicationTemplateUiHandler extends WidgetUiHandler {
   private final ApplicationTemplateInstallUtil installInfo;
   private BLabel label = new BLabel();

   public static WizardStep makeWizardStep(ApplicationTemplateInstallUtil installInfo) {
      String title;
      String description;
      switch (installInfo.getSourceType()) {
         case 1:
         default:
            title = "installApplication.confirmInstallApplicationTemplate.title";
            description = "installApplication.confirmInstallApplicationTemplate.description";
            break;
         case 2:
            title = "installApplication.confirmInstallApplicationExcelFile.title";
            description = "installApplication.confirmInstallApplicationExcelFile.description";
      }

      WizardStep wizardStep = WizardUtil.makeStep(title, description, new ConfirmInstallApplicationTemplateUiHandler(installInfo));
      wizardStep.setForceFinishStep(true);
      return wizardStep;
   }

   private ConfirmInstallApplicationTemplateUiHandler(ApplicationTemplateInstallUtil installInfo) {
      this.label.setHalign(BHalign.left);
      this.installInfo = installInfo;
      this.setWidget(WizardUtil.wrapInCenter(this.label));
   }

   public StepArtifact create(WizardStep step, StepModel model, IWizardView view, Context cx) {
      this.updateLabelText(model);
      WidgetStepArtifact stepArtifact = (WidgetStepArtifact)super.create(step, model, view, cx);
      return new GeneralStepArtifact(stepArtifact, this.installInfo);
   }

   public boolean restoreFromNext(StepArtifact artifact, StepModel model, IWizardView view, Context cx) {
      this.updateLabelText(model);
      return super.restoreFromNext(artifact, model, view, cx);
   }

   public boolean restoreFromBack(StepArtifact artifact, StepModel model, IWizardView view, Context cx) {
      this.updateLabelText(model);
      return super.restoreFromBack(artifact, model, view, cx);
   }

   private void updateLabelText(StepModel model) {
      StringBuilder message = new StringBuilder();
      if (this.installInfo.getSourceType() == 2) {
         message.append(
            WizardUtil.LEX
               .getText(
                  "installApplication.confirmInstallApplicationExcelFile.message",
                  new Object[]{this.installInfo.getSourceFile().getFileName(), this.installInfo.getStation().getStationDisplayName(null)}
               )
         );
      } else if (this.installInfo.getSourceType() == 1) {
         message.append(
            WizardUtil.LEX
               .getText(
                  "installApplication.confirmInstallApplicationTemplate.message",
                  new Object[]{this.installInfo.getSourceFile().getFileName(), this.installInfo.getStation().getStationDisplayName(null)}
               )
         );
         if (this.installInfo.doesStationSupportUpgradeAndOptionals()) {
            int optionalComponentCount = OptionalComponentsArtifact.getOptionalComponentCount(model);
            if (optionalComponentCount > 0) {
               int installedOptionalComponents = OptionalComponentsArtifact.getInstalledOptionalComponentCount(model);
               message.append("\n\n");
               if (installedOptionalComponents == 0) {
                  message.append(
                     WizardUtil.LEX.getText("installApplication.confirmInstallApplicationTemplate.optionals.none", new Object[]{optionalComponentCount})
                  );
               } else if (optionalComponentCount == installedOptionalComponents) {
                  message.append(
                     WizardUtil.LEX.getText("installApplication.confirmInstallApplicationTemplate.optionals.all", new Object[]{optionalComponentCount})
                  );
               } else {
                  message.append(
                     WizardUtil.LEX
                        .getText(
                           "installApplication.confirmInstallApplicationTemplate.optionals.counts",
                           new Object[]{installedOptionalComponents, optionalComponentCount}
                        )
                  );
               }
            }
         }
      }

      message.append("\n\n");
      message.append(
         WizardUtil.LEX.get("installApplication.confirmInstallApplicationTemplate.backup." + (BackupStepArtifact.canPerformBackup(model) ? "yes" : "no"))
      );
      this.label.setText(message.toString());
   }
}
