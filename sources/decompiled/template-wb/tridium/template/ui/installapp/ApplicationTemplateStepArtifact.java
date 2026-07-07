package com.tridium.template.ui.installapp;

import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.template.ui.file.BWbDeployableNtplFile;
import com.tridium.ui.wizard.step.WidgetUiHandler.WidgetStepArtifact;
import java.util.Optional;
import javax.baja.file.BIFile;

class ApplicationTemplateStepArtifact extends AbstractStepArtifact {
   ApplicationTemplateStepArtifact(WidgetStepArtifact innerArtifact, ApplicationTemplateInstallUtil installInfo) {
      super(innerArtifact, installInfo);
   }

   Optional<BWbDeployableNtplFile> getApplicationTemplateFile() {
      this.loadSelectedFile();
      return this.installInfo.getApplicationTemplateFile() == null ? Optional.empty() : Optional.of(this.installInfo.getApplicationTemplateFile());
   }

   void loadSelectedFile() {
      this.installInfo.loadApplicationTemplateFile(this.getSelectedFile().orElse(null));
   }

   private Optional<BIFile> getSelectedFile() {
      return SelectApplicationTemplateUiHandler.getSelectedFile(this.getWidget());
   }
}
