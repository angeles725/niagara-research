package com.tridium.template.ui.installapp;

import com.tridium.ui.wizard.step.WizardStep;
import com.tridium.ui.wizard.step.WizardStep.IUiHandler;
import com.tridium.ui.wizard.step.util.WizardHeaderUiHandler;
import javax.baja.gx.BImage;
import javax.baja.sys.BIcon;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BGridPane;
import javax.baja.util.Lexicon;

public final class WizardUtil {
   public static final Lexicon LEX = Lexicon.make(WizardUtil.class);
   private static final BImage WIZARD_ICON = BImage.make(BIcon.std("wizard.png"));
   private static final String WRAPPED_PROPERTY_NAME = "wrapped";

   private WizardUtil() {
   }

   static BWidget wrapInCenter(BWidget widget) {
      BGridPane gridPane = new BGridPane();
      gridPane.setColumnCount(1);
      gridPane.setColumnAlign(BHalign.center);
      gridPane.add("wrapped", widget);
      return gridPane;
   }

   static BWidget getInnerWrappedWidget(BWidget widget) {
      return (BWidget)widget.get("wrapped");
   }

   public static WizardStep makeStep(String titleKey, String desciptionKey, IUiHandler uiHandler) {
      WizardHeaderUiHandler headerUiHandler = new WizardHeaderUiHandler(WIZARD_ICON, LEX.get(titleKey), LEX.get(desciptionKey), uiHandler);
      return new WizardStep(headerUiHandler);
   }
}
