package com.tridium.px.editor.sidebars.cellsheet;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BConverterCE;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BPxLayerCE;
import com.tridium.px.editor.sidebars.cellsheet.commands.AddBinding;
import com.tridium.px.editor.sidebars.cellsheet.commands.ChangeBinding;
import com.tridium.px.editor.sidebars.cellsheet.commands.ChangeConverter;
import com.tridium.px.editor.sidebars.cellsheet.commands.ChangeLayer;
import com.tridium.px.editor.sidebars.cellsheet.commands.ChangeProperty;
import com.tridium.ui.px.PxPropertyComponentArray;
import com.tridium.util.ClassUtil;
import javax.baja.naming.BOrd;
import javax.baja.nav.BINavNode;
import javax.baja.px.editor.event.PxBindingEvent;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.space.Mark;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Property;
import javax.baja.ui.BBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.px.PxProperty;
import javax.baja.ui.shape.BShape;
import javax.baja.ui.transfer.TransferConst;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.ui.transfer.TransferFormat;
import javax.baja.util.BConverter;
import javax.baja.workbench.celleditor.BWbCellEditor;

class DefaultCellSheetContext implements CellSheetContext, TransferConst {
   private BPxEditorPane editorPane;
   private BPxCellSheet sheet;

   DefaultCellSheetContext(BPxEditorPane editorPane, BPxCellSheet sheet) {
      this.editorPane = editorPane;
      this.sheet = sheet;
   }

   @Override
   public boolean allowBindingDelete() {
      return true;
   }

   @Override
   public boolean allowGotoOrd() {
      return true;
   }

   @Override
   public boolean allowLayoutEdit(BWidget[] widgets) {
      for (int i = 0; i < widgets.length; i++) {
         if (widgets[i] instanceof BShape) {
            return false;
         }

         if (!(widgets[i].getParentWidget() instanceof BCanvasPane)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public BObject[] resolveBindingTarget(BBinding[] bindings) {
      BObject[] from = new BObject[bindings.length];

      for (int i = 0; i < bindings.length; i++) {
         try {
            from[i] = bindings[i].get();
         } catch (Exception var5) {
            System.out.println("could not resolve " + bindings[i].getOrd());
         }
      }

      return from;
   }

   @Override
   public void bindingAdded(BWidget widget, Property property, BBinding binding) {
      this.editorPane.getSelectedWidgets().setWidgets(new BWidget[]{widget});
      this.editorPane.getPxEditor().firePxEvent(new PxWidgetEvent(0, widget, property.getName(), binding));
   }

   @Override
   public void bindingDeleted(BWidget widget, Property property, BBinding binding) {
      this.editorPane.getSelectedWidgets().setWidgets(new BWidget[]{widget});
      this.editorPane.getPxEditor().firePxEvent(new PxWidgetEvent(1, widget, property.getName(), binding));
   }

   @Override
   public void converterAdded(BBinding binding, Property property, BConverter converter) {
      this.editorPane.getPxEditor().firePxEvent(new PxBindingEvent(0, binding, property.getName(), converter));
   }

   @Override
   public void converterDeleted(BBinding binding, Property property, BConverter converter) {
      this.editorPane.getPxEditor().firePxEvent(new PxBindingEvent(1, binding, property.getName(), converter));
   }

   @Override
   public int newBindingDragOver(BPxCellSheetLabel label, TransferContext cx) {
      if (this.sheet.canDrop() && cx.getEnvelope().supports(TransferFormat.mark)) {
         BObject[] objects = ((Mark)cx.getEnvelope().getData(TransferFormat.mark)).getValues();
         boolean dragging = ClassUtil.all(objects, BComponent.class) && !ClassUtil.any(objects, BWidget.class);
         label.setDragging(dragging);
         label.repaint();
         return dragging ? 16 : 0;
      } else {
         label.setDragging(false);
         label.repaint();
         return 0;
      }
   }

   @Override
   public CommandArtifact newBindingDrop(TransferContext cx) throws Exception {
      BObject[] objects = ((Mark)cx.getEnvelope().getData(TransferFormat.mark)).getValues();
      BOrd ord = ((BComponent)objects[0]).getSlotPathOrd();
      return new AddBinding(this.editorPane, this.sheet, ord).doInvoke();
   }

   @Override
   public int existingBindingDragOver(BPxCellSheetBindingLabel label, TransferContext cx) {
      if (this.sheet.canDrop() && cx.getEnvelope().supports(TransferFormat.mark)) {
         BObject[] objects = ((Mark)cx.getEnvelope().getData(TransferFormat.mark)).getValues();
         boolean dragging = ClassUtil.all(objects, BINavNode.class) && !ClassUtil.any(objects, BWidget.class);
         label.setDragging(dragging);
         label.repaint();
         return dragging ? 16 : 0;
      } else {
         label.setDragging(false);
         label.repaint();
         return 0;
      }
   }

   @Override
   public CommandArtifact existingBindingDrop(TransferContext cx, BWidget widget, BBinding binding) throws Exception {
      BObject[] objects = ((Mark)cx.getEnvelope().getData(TransferFormat.mark)).getValues();
      BOrd ord = ((BComponent)objects[0]).getSlotPathOrd();
      return new ChangeBinding(this.editorPane, this.sheet, binding, binding.getProperty("ord"), ord).doInvoke();
   }

   @Override
   public void cellModified(BWbCellEditor ce) {
      try {
         ce.saveValue();
      } catch (Exception var4) {
         throw new BajaRuntimeException(var4);
      }

      if (ce instanceof BConverterCE) {
         BConverterCE convCE = (BConverterCE)ce;
         new ChangeConverter(
               this.editorPane, this, this.sheet.getWidget(), convCE.binding(), ce.getPropertyName(), convCE.converter(), (BConverter)convCE.getCurrentValue()
            )
            .invoke();
      } else if (ce instanceof BPxLayerCE) {
         BPxLayerCE layerCE = (BPxLayerCE)ce;
         String layerName = layerCE.getListDropDown().getList().getSelectedItem().toString();
         new ChangeLayer(this.editorPane, this.sheet, this.editorPane.getLayerManager().getLayerByName(layerName)).invoke();
      } else {
         new ChangeProperty(this.editorPane, this.sheet, ce.getPropertyName(), (BValue)ce.getCurrentValue()).invoke();
      }
   }

   @Override
   public void bindingPropertyChanged(BBinding binding, Property property, BValue value) {
      new ChangeBinding(this.editorPane, this.sheet, binding, property, value).invoke();
   }

   @Override
   public PxPropertyComponentArray getPxPropertyComponents() {
      return this.editorPane.getPxPropertyComponents();
   }

   @Override
   public void pxPropertyLinked(PxProperty group, BComponent comp, String propName) {
      this.editorPane.getPxPropertyComponents().addTarget(group, comp, propName);
      this.editorPane.getPxPropertyComponents().updateTargets(this.editorPane.getPxEditor().getWidget(), this.editorPane.getPxEditor().getPxProperties());
      this.fireLinkageEvent(comp, propName);
   }

   @Override
   public void pxPropertyUnlinked(PxProperty group, BComponent comp, String propName) {
      this.editorPane.getPxPropertyComponents().removeTarget(group, comp, propName);
      this.editorPane.getPxPropertyComponents().updateTargets(this.editorPane.getPxEditor().getWidget(), this.editorPane.getPxEditor().getPxProperties());
      this.fireLinkageEvent(comp, propName);
   }

   private void fireLinkageEvent(BComponent comp, String propName) {
      if (comp instanceof BWidget) {
         this.editorPane.getPxEditor().firePxEvent(new PxWidgetEvent(2, (BWidget)comp, propName, comp.get(propName)));
      } else {
         if (!(comp instanceof BBinding)) {
            throw new IllegalStateException();
         }

         this.editorPane.getPxEditor().firePxEvent(new PxBindingEvent(2, (BBinding)comp, propName, comp.get(propName)));
      }
   }
}
