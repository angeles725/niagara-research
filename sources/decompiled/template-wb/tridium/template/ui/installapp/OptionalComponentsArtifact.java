package com.tridium.template.ui.installapp;

import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.ui.wizard.step.StepModel;
import com.tridium.ui.wizard.step.WidgetUiHandler.WidgetStepArtifact;
import javax.baja.naming.BOrdList;

class OptionalComponentsArtifact extends AbstractStepArtifact {
   OptionalComponentsArtifact(WidgetStepArtifact innerArtifact, ApplicationTemplateInstallUtil installInfo) {
      super(innerArtifact, installInfo);
   }

   static int getOptionalComponentCount(StepModel model) {
      OptionalComponentsArtifact stepArtifact = (OptionalComponentsArtifact)model.getStepArtifactHistory().searchForArtifact(OptionalComponentsArtifact.class);
      return stepArtifact == null ? 0 : stepArtifact.getOptionalComponentCount();
   }

   static int getInstalledOptionalComponentCount(StepModel model) {
      OptionalComponentsArtifact stepArtifact = (OptionalComponentsArtifact)model.getStepArtifactHistory().searchForArtifact(OptionalComponentsArtifact.class);
      return stepArtifact == null ? 0 : stepArtifact.getInstalledOptionalComponentCount();
   }

   static boolean allOptionalsAreSelected(StepModel model) {
      OptionalComponentsArtifact stepArtifact = (OptionalComponentsArtifact)model.getStepArtifactHistory().searchForArtifact(OptionalComponentsArtifact.class);
      return stepArtifact == null || stepArtifact.getInstalledOptionalComponentCount() == stepArtifact.getOptionalComponentCount();
   }

   static BOrdList getOptionalComponentsToBeRemoved(StepModel model) {
      OptionalComponentsArtifact stepArtifact = (OptionalComponentsArtifact)model.getStepArtifactHistory().searchForArtifact(OptionalComponentsArtifact.class);
      return stepArtifact == null ? BOrdList.DEFAULT : stepArtifact.getOptionalComponentsToBeRemoved();
   }

   private int getOptionalComponentCount() {
      return SelectOptionalComponentsUiHandler.getOptionalComponentCount(this.getWidget());
   }

   private int getInstalledOptionalComponentCount() {
      return SelectOptionalComponentsUiHandler.getInstalledOptionalComponentCount(this.getWidget());
   }

   private BOrdList getOptionalComponentsToBeRemoved() {
      return SelectOptionalComponentsUiHandler.getOptionalComponentsToBeRemoved(this.getWidget());
   }
}
