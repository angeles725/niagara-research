package com.tridium.px.editor.make;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.ui.px.PxPropertyComponent;
import com.tridium.ui.px.PxPropertyComponentArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.baja.px.editor.factory.ImageFileFactory;
import javax.baja.sys.BComponent;
import javax.baja.ui.BWidget;

public class WidgetCopier {
   BPxEditorPane editorPane;
   Map<BWidget, List<BWidget>> hash = new HashMap<>();

   public WidgetCopier(BPxEditorPane editorPane) {
      this.editorPane = editorPane;
   }

   public BWidget copyWidget(BWidget oldWidget) {
      BWidget newWidget = this.editorPane.getPxEditor().cloneWidget(oldWidget);
      ImageFileFactory factory = new ImageFileFactory(this.editorPane.getPxEditor());
      factory.convertImages(newWidget);
      List<BWidget> arr = this.hash.get(oldWidget);
      if (arr == null) {
         this.hash.put(oldWidget, arr = new ArrayList<>());
      }

      arr.add(newWidget);
      return newWidget;
   }

   public PxPropertyComponentArray makePxPropertyComponents(PxPropertyComponentArray oldGc) {
      PxPropertyComponentArray newGc = new PxPropertyComponentArray();

      for (int i = 0; i < oldGc.size(); i++) {
         PxPropertyComponent oc = oldGc.get(i);
         BComponent oldComp = oc.getComponent();
         List<BWidget> arr = this.hash.get(oc.getWidget());
         if (arr == null) {
            newGc.add(oc);
         } else {
            BWidget[] newWidgets = arr.toArray(new BWidget[0]);

            for (int j = 0; j < newWidgets.length; j++) {
               BComponent newComp = (BComponent)(oldComp instanceof BWidget
                  ? newWidgets[j]
                  : (BComponent)newWidgets[j].get(oldComp.getPropertyInParent().getName()));
               PxPropertyComponent nc = new PxPropertyComponent(newComp);
               nc.cloneTargets(oc);
               newGc.add(nc);
            }
         }
      }

      return newGc;
   }
}
