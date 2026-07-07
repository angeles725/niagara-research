package com.tridium.template.ui.installapp;

import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.ui.wizard.step.BWizardButtonMode;
import com.tridium.ui.wizard.step.IWizardView;
import com.tridium.ui.wizard.step.StepArtifact;
import com.tridium.ui.wizard.step.StepModel;
import com.tridium.ui.wizard.step.WidgetUiHandler;
import com.tridium.ui.wizard.step.WizardStep;
import com.tridium.ui.wizard.step.WidgetUiHandler.WidgetStepArtifact;
import javax.baja.sys.Context;
import javax.baja.ui.pane.BTextEditorPane;

public class CompatibilityMessageUiHandler extends WidgetUiHandler {
   private final ApplicationTemplateInstallUtil installInfo;

   public static WizardStep makeWizardStep(ApplicationTemplateInstallUtil installInfo) {
      String title = "installApplication.compatibilityMessage.title";
      String description = "installApplication.compatibilityMessage.description";
      WizardStep step = WizardUtil.makeStep(title, description, new CompatibilityMessageUiHandler(installInfo));
      step.addSkipChecker(CompatibilityMessageUiHandler::skipThisStep);
      return step;
   }

   private CompatibilityMessageUiHandler(ApplicationTemplateInstallUtil installInfo) {
      this.installInfo = installInfo;
      this.setWidget(new BTextEditorPane());
   }

   public StepArtifact create(WizardStep step, StepModel model, IWizardView view, Context cx) {
      this.resetStep(step, model);
      WidgetStepArtifact stepArtifact = (WidgetStepArtifact)super.create(step, model, view, cx);
      return new GeneralStepArtifact(stepArtifact, this.installInfo);
   }

   public boolean restoreFromNext(StepArtifact artifact, StepModel model, IWizardView view, Context cx) {
      this.resetStep(artifact.getStep(), model);
      return super.restoreFromNext(artifact, model, view, cx);
   }

   public boolean restoreFromBack(StepArtifact artifact, StepModel model, IWizardView view, Context cx) {
      this.resetStep(artifact.getStep(), model);
      return super.restoreFromBack(artifact, model, view, cx);
   }

   private void resetStep(WizardStep step, StepModel model) {
      this.installInfo.checkApplicationTemplateFileCompatibility();
      ((BTextEditorPane)this.getWidget()).setText(this.installInfo.hasCompatibilityMessage() ? this.installInfo.getCompatibilityMessage() : "");
      if (this.installInfo.isIncompatible()) {
         step.setOverrideMode(BWizardButtonMode.back.merge(BWizardButtonMode.cancel));
      } else {
         step.clearOverrideMode();
      }
   }

   private static boolean skipThisStep(StepArtifact artifact, StepModel model, IWizardView view, Context cx) {
      AbstractStepArtifact stepArtifact = (AbstractStepArtifact)artifact;
      return !stepArtifact.installInfo.hasCompatibilityMessage();
   }
}
