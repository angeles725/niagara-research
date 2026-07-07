package com.tridium.template.ui.installapp;

import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.ui.wizard.step.StepModel;
import com.tridium.ui.wizard.step.WidgetUiHandler.WidgetStepArtifact;

class BackupStepArtifact extends AbstractStepArtifact {
   BackupStepArtifact(WidgetStepArtifact innerArtifact, ApplicationTemplateInstallUtil installInfo) {
      super(innerArtifact, installInfo);
   }

   private boolean canPerformBackup() {
      return BackupUiHandler.canPerformBackup(this.getWidget());
   }

   static boolean canPerformBackup(StepModel model) {
      BackupStepArtifact stepArtifact = (BackupStepArtifact)model.getStepArtifactHistory().searchForArtifact(BackupStepArtifact.class);
      return stepArtifact == null || stepArtifact.canPerformBackup();
   }
}
