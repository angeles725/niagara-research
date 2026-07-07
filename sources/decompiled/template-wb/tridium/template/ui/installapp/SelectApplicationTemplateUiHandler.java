package com.tridium.template.ui.installapp;

import com.tridium.template.file.NtplUtil;
import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.template.ui.file.BWbDeployableNtplFile;
import com.tridium.ui.wizard.step.IWizardView;
import com.tridium.ui.wizard.step.StepArtifact;
import com.tridium.ui.wizard.step.StepModel;
import com.tridium.ui.wizard.step.WidgetUiHandler;
import com.tridium.ui.wizard.step.WizardStep;
import com.tridium.ui.wizard.step.WidgetUiHandler.WidgetStepArtifact;
import com.tridium.workbench.fieldeditors.BOrdFE;
import java.util.Optional;
import javax.baja.file.BDirectory;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.ui.BWidget;
import javax.baja.util.CannotValidateException;

public class SelectApplicationTemplateUiHandler extends WidgetUiHandler {
   private final ApplicationTemplateInstallUtil installInfo;

   static WizardStep makeWizardStep(ApplicationTemplateInstallUtil installInfo) {
      String title = "installApplication.selectApplicationTemplate.title";
      String description = "installApplication.selectApplicationTemplate.description";
      WizardStep wizardStep = WizardUtil.makeStep(title, description, new SelectApplicationTemplateUiHandler(installInfo));
      wizardStep.addValidator(SelectApplicationTemplateUiHandler::validateTemplate);
      return wizardStep;
   }

   private SelectApplicationTemplateUiHandler(ApplicationTemplateInstallUtil installInfo) {
      this.installInfo = installInfo;

      BDirectory applicationsDir;
      try {
         applicationsDir = NtplUtil.getApplicationDirectory();
      } catch (Exception var5) {
         throw new LocalizableRuntimeException(getModuleName(), "installApplication.cannotAccessApplicationTemplatesDir", var5);
      }

      BOrdFE ordFe = new BOrdFE();
      Context cx = BFacets.make("targetType", BIFile.TYPE.toString());
      ordFe.loadValue(applicationsDir.getNavOrd(), cx);
      this.setWidget(WizardUtil.wrapInCenter(ordFe));
   }

   private static String getModuleName() {
      return Sys.getModuleForClass(SelectApplicationTemplateUiHandler.class).getModuleName();
   }

   private static void validateTemplate(StepArtifact artifact, StepModel model, IWizardView view, Context cx) throws CannotValidateException {
      ApplicationTemplateStepArtifact appArtifact = (ApplicationTemplateStepArtifact)artifact;
      BWbDeployableNtplFile templateFile = appArtifact.getApplicationTemplateFile().orElse(null);
      if (templateFile == null) {
         throw new CannotValidateException(WizardUtil.LEX.getText("installApplication.mustSelectApplicationTemplate", new Object[]{"napl"}));
      }
   }

   static Optional<BIFile> getSelectedFile(BWidget widget) {
      Optional<BIFile> fileOpt = Optional.empty();

      try {
         BOrdFE ordFe = (BOrdFE)WizardUtil.getInnerWrappedWidget(widget);
         BOrd ord = (BOrd)ordFe.saveValue();
         fileOpt = Optional.of((BIFile)ord.get());
      } catch (Exception var4) {
      }

      return fileOpt;
   }

   public StepArtifact create(WizardStep step, StepModel model, IWizardView view, Context cx) {
      WidgetStepArtifact stepArtifact = (WidgetStepArtifact)super.create(step, model, view, cx);
      return new ApplicationTemplateStepArtifact(stepArtifact, this.installInfo);
   }

   public StepArtifact updateStepArtifact(StepArtifact artifact, StepModel model, IWizardView view, Context cx) {
      ((ApplicationTemplateStepArtifact)artifact).loadSelectedFile();
      return super.updateStepArtifact(artifact, model, view, cx);
   }
}
