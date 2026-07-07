package com.tridium.template.ui.installapp;

import com.tridium.template.ui.ApplicationTemplateInstallUtil;
import com.tridium.template.ui.file.BWbDeployableNtplFile;
import com.tridium.ui.theme.Theme;
import com.tridium.ui.wizard.step.IWizardView;
import com.tridium.ui.wizard.step.StepArtifact;
import com.tridium.ui.wizard.step.StepModel;
import com.tridium.ui.wizard.step.WidgetUiHandler;
import com.tridium.ui.wizard.step.WizardStep;
import com.tridium.ui.wizard.step.WidgetUiHandler.WidgetStepArtifact;
import java.util.ArrayList;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.ui.BBorder;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BScrollPane;

public class SelectOptionalComponentsUiHandler extends WidgetUiHandler {
   private BWbDeployableNtplFile templateFile = null;
   private BGridPane optionals = new BGridPane(1);
   private final ApplicationTemplateInstallUtil installInfo;

   static WizardStep makeWizardStep(ApplicationTemplateInstallUtil installInfo) {
      WizardStep step = WizardUtil.makeStep(
         "installApplication.optionals.title", "installApplication.optionals.description", new SelectOptionalComponentsUiHandler(installInfo)
      );
      step.addSkipChecker(SelectOptionalComponentsUiHandler::skipThisStep);
      return step;
   }

   private SelectOptionalComponentsUiHandler(ApplicationTemplateInstallUtil installInfo) {
      this.installInfo = installInfo;
      BScrollPane scrollPane = new BScrollPane(this.optionals);
      scrollPane.setViewportBackground(Theme.scrollPane().getControlBackground());
      this.setWidget(new BEdgePane(null, null, new BEdgePane(new BBorderPane(scrollPane, BBorder.none), null, null, null, null), null, null));
   }

   private static BGridPane getOptionals(BWidget widget) {
      return (BGridPane)((BScrollPane)((BBorderPane)((BEdgePane)((BEdgePane)widget).getLeft()).getTop()).getContent()).getContent();
   }

   public StepArtifact create(WizardStep step, StepModel model, IWizardView view, Context cx) {
      this.loadOptionalComponentList();
      WidgetStepArtifact stepArtifact = (WidgetStepArtifact)super.create(step, model, view, cx);
      return new OptionalComponentsArtifact(stepArtifact, this.installInfo);
   }

   public boolean restoreFromNext(StepArtifact artifact, StepModel model, IWizardView view, Context cx) {
      this.loadOptionalComponentList();
      return super.restoreFromNext(artifact, model, view, cx);
   }

   public boolean restoreFromBack(StepArtifact artifact, StepModel model, IWizardView view, Context cx) {
      this.loadOptionalComponentList();
      return super.restoreFromBack(artifact, model, view, cx);
   }

   static int getOptionalComponentCount(BWidget widget) {
      BGridPane optionals = getOptionals(widget);
      return optionals.getDynamicPropertiesArray().length;
   }

   static int getInstalledOptionalComponentCount(BWidget widget) {
      int count = 0;
      BGridPane optionals = getOptionals(widget);
      Property[] checkBoxProperties = optionals.getDynamicPropertiesArray();

      for (Property prop : checkBoxProperties) {
         BCheckBox optionalComponentCheckBox = (BCheckBox)optionals.get(prop);
         if (optionalComponentCheckBox.isSelected()) {
            count++;
         }
      }

      return count;
   }

   static BOrdList getOptionalComponentsToBeRemoved(BWidget widget) {
      ArrayList<BOrd> ords = new ArrayList<>();
      BGridPane optionals = getOptionals(widget);
      Property[] checkBoxProperties = optionals.getDynamicPropertiesArray();

      for (Property prop : checkBoxProperties) {
         BCheckBox optionalComponentCheckBox = (BCheckBox)optionals.get(prop);
         if (!optionalComponentCheckBox.isSelected()) {
            ords.add(BOrd.make(new SlotPath(optionalComponentCheckBox.getText())));
         }
      }

      return BOrdList.make(ords.toArray(new BOrd[0]));
   }

   private void loadOptionalComponentList() {
      BWbDeployableNtplFile newFile = this.installInfo.getApplicationTemplateFile();
      if (newFile == null) {
         this.optionals.removeAll();
         this.templateFile = null;
      } else if (this.templateFile == null || !newFile.toPathString().equals(this.templateFile.toPathString())) {
         this.optionals.removeAll();
         this.templateFile = newFile;

         for (BOrd optionalComponentOrd : this.templateFile.getTemplateManifest().optional) {
            OrdQuery[] queries = optionalComponentOrd.parse();
            SlotPath path = (SlotPath)queries[queries.length - 1];
            String[] names = path.getNames();
            StringBuilder friendlyName = new StringBuilder(names[1]);

            for (int i = 2; i < names.length; i++) {
               friendlyName.append('/');
               friendlyName.append(names[i]);
            }

            BCheckBox optionalComponentCheckBox = new BCheckBox(friendlyName.toString(), true);
            this.optionals.add(null, optionalComponentCheckBox);
         }
      }
   }

   private static boolean skipThisStep(StepArtifact artifact, StepModel model, IWizardView view, Context cx) {
      AbstractStepArtifact stepArtifact = (AbstractStepArtifact)artifact;
      return !stepArtifact.installInfo.doesStationSupportUpgradeAndOptionals() ? true : !stepArtifact.installInfo.doesTemplateHaveOptionals();
   }
}
