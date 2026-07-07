package com.tridium.template.ui.installapp;

import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.ui.wizard.step.WidgetUiHandler.WidgetStepArtifact;

abstract class AbstractStepArtifact extends WidgetStepArtifact {
   final ApplicationTemplateInstallUtil installInfo;

   AbstractStepArtifact(WidgetStepArtifact innerArtifact, ApplicationTemplateInstallUtil installInfo) {
      super(innerArtifact.getStep(), innerArtifact.getWidget());
      this.installInfo = installInfo;
   }
}
