package com.tridium.template.ui.installapp;

import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.ui.wizard.step.WidgetUiHandler.WidgetStepArtifact;

class GeneralStepArtifact extends AbstractStepArtifact {
   GeneralStepArtifact(WidgetStepArtifact innerArtifact, ApplicationTemplateInstallUtil installInfo) {
      super(innerArtifact, installInfo);
   }
}
