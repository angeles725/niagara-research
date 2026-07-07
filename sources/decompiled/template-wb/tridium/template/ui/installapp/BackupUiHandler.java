package com.tridium.template.ui.installapp;

import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.ui.wizard.step.IWizardView;
import com.tridium.ui.wizard.step.StepArtifact;
import com.tridium.ui.wizard.step.StepModel;
import com.tridium.ui.wizard.step.WidgetUiHandler;
import com.tridium.ui.wizard.step.WizardStep;
import com.tridium.ui.wizard.step.WidgetUiHandler.WidgetStepArtifact;
import javax.baja.gx.BInsets;
import javax.baja.sys.Context;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BLabel;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;

public class BackupUiHandler extends WidgetUiHandler {
   private static final BInsets BOTTOM_SPACER_INSET = BInsets.make(0.0, 0.0, 10.0, 0.0);
   private final ApplicationTemplateInstallUtil installInfo;

   public static WizardStep makeWizardStep(ApplicationTemplateInstallUtil installInfo) {
      return WizardUtil.makeStep("installApplication.backup.title", "installApplication.backup.description", new BackupUiHandler(installInfo));
   }

   private BackupUiHandler(ApplicationTemplateInstallUtil installInfo) {
      this.installInfo = installInfo;
      BEdgePane edgePane = new BEdgePane();
      BLabel label = new BLabel(WizardUtil.LEX.get("installApplication.backup.message"));
      label.setHalign(BHalign.left);
      edgePane.setTop(new BBorderPane(label, BOTTOM_SPACER_INSET));
      BCheckBox backupCheckBox = new BCheckBox(WizardUtil.LEX.get("installApplication.backup"), true);
      edgePane.setCenter(backupCheckBox);
      this.setWidget(WizardUtil.wrapInCenter(edgePane));
   }

   public StepArtifact create(WizardStep step, StepModel model, IWizardView view, Context cx) {
      WidgetStepArtifact stepArtifact = (WidgetStepArtifact)super.create(step, model, view, cx);
      return new BackupStepArtifact(stepArtifact, this.installInfo);
   }

   static boolean canPerformBackup(BWidget widget) {
      BEdgePane edgePane = (BEdgePane)WizardUtil.getInnerWrappedWidget(widget);
      BCheckBox checkBox = (BCheckBox)edgePane.getCenter();
      return checkBox.isSelected();
   }
}
