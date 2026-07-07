package com.tridium.px.editor.make;

import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheetBindingLabel;
import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheetLabel;
import com.tridium.px.editor.sidebars.cellsheet.CellSheetContext;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BConverterCE;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BPxLayerCE;
import com.tridium.px.editor.util.LayerManager;
import com.tridium.ui.px.PxPropertyComponentArray;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Property;
import javax.baja.ui.BBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.PxLayer;
import javax.baja.ui.px.PxProperty;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.util.BConverter;
import javax.baja.workbench.celleditor.BWbCellEditor;

class MakeWidgetContext implements CellSheetContext {
   private BMakeWidget mw;
   PxPropertyComponentArray workingGc;

   MakeWidgetContext(BMakeWidget mw) {
      this.mw = mw;
      this.workingGc = mw.getEditorPane().getPxPropertyComponents().deepClone();
   }

   @Override
   public boolean allowBindingDelete() {
      return false;
   }

   @Override
   public boolean allowGotoOrd() {
      return false;
   }

   @Override
   public boolean allowLayoutEdit(BWidget[] widgets) {
      return false;
   }

   @Override
   public BObject[] resolveBindingTarget(BBinding[] bindings) {
      return this.mw.getDrawingObjects();
   }

   @Override
   public void bindingAdded(BWidget widget, Property property, BBinding binding) {
      throw new IllegalStateException();
   }

   @Override
   public void bindingDeleted(BWidget widget, Property property, BBinding binding) {
      throw new IllegalStateException();
   }

   @Override
   public void converterAdded(BBinding binding, Property property, BConverter converter) {
      this.mw.editWorkingWidget();
   }

   @Override
   public void converterDeleted(BBinding binding, Property property, BConverter converter) {
      this.mw.editWorkingWidget();
   }

   @Override
   public int newBindingDragOver(BPxCellSheetLabel label, TransferContext cx) {
      return 0;
   }

   @Override
   public CommandArtifact newBindingDrop(TransferContext cx) throws Exception {
      throw new IllegalStateException();
   }

   @Override
   public int existingBindingDragOver(BPxCellSheetBindingLabel label, TransferContext cx) {
      return 0;
   }

   @Override
   public CommandArtifact existingBindingDrop(TransferContext cx, BWidget widget, BBinding binding) throws Exception {
      throw new IllegalStateException();
   }

   @Override
   public void bindingPropertyChanged(BBinding binding, Property property, BValue value) {
      binding.set(property.getName(), value);
   }

   @Override
   public void cellModified(BWbCellEditor ce) {
      try {
         ce.saveValue();
      } catch (Exception var6) {
         throw new BajaRuntimeException(var6);
      }

      if (ce instanceof BConverterCE) {
         BConverterCE convCE = (BConverterCE)ce;
         BBinding binding = convCE.binding();
         String propertyName = ce.getPropertyName();
         BConverter newConv = (BConverter)convCE.getCurrentValue();
         binding.set(propertyName, newConv);
      } else if (ce instanceof BPxLayerCE) {
         LayerManager layerMgr = this.mw.getEditorPane().getLayerManager();
         BPxLayerCE layerCE = (BPxLayerCE)ce;
         PxLayer layer = layerMgr.getLayerByName(layerCE.getListDropDown().getList().getSelectedItem().toString());
         layerMgr.removeTag(this.mw.getWorkingWidget());
         if (layer != null) {
            layerMgr.addTag(this.mw.getWorkingWidget(), layer);
         }
      } else {
         this.mw.getWorkingWidget().set(ce.getPropertyName(), (BValue)ce.getCurrentValue());
         this.mw.getWorkingWidget().bindingsChanged();
      }
   }

   @Override
   public PxPropertyComponentArray getPxPropertyComponents() {
      return this.workingGc;
   }

   @Override
   public void pxPropertyLinked(PxProperty group, BComponent comp, String propName) {
      this.workingGc.addTarget(group, comp, propName);
      this.mw.editWorkingWidget();
   }

   @Override
   public void pxPropertyUnlinked(PxProperty group, BComponent comp, String propName) {
      this.workingGc.removeTarget(group, comp, propName);
      this.mw.editWorkingWidget();
   }
}
