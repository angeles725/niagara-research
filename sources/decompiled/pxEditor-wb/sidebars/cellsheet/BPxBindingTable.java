package com.tridium.px.editor.sidebars.cellsheet;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BPxPropertyCE;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.px.PxProperty;
import javax.baja.workbench.celleditor.BWbCellEditor;

@NiagaraType
@NiagaraAction(
   name = "cellModified",
   parameterType = "BWidgetEvent",
   defaultValue = "new BWidgetEvent()"
)
public class BPxBindingTable extends BPxCellTable {
   public static final Action cellModified = newAction(0, new BWidgetEvent(), null);
   public static final Type TYPE = Sys.loadType(BPxBindingTable.class);
   protected BWidget widget;
   protected BBinding binding;

   public void cellModified(BWidgetEvent parameter) {
      this.invoke(cellModified, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BPxBindingTable(BPxEditorPane editorPane, BPxCellSheet sheet, BWidget widget, BBinding binding) {
      super(editorPane, sheet);
      this.widget = widget;
      this.binding = binding;
      Property[] props = binding.getPropertiesArray();
      int row = 0;

      for (int i = 0; i < props.length; i++) {
         if (!props[i].isDynamic() && !Flags.isHidden(binding, props[i])) {
            BWbCellEditor ce = this.makeCell(props[i]);
            ce.setReadonly(sheet.isReadonly() || Flags.isReadonly(binding, props[i]));
            ce.setRowIndex(row++);
            ce.loadValue(binding.get(props[i]), binding.getSlotFacets(props[i]));
            ce.setPropertyName(props[i].getName());
            this.addCellEditor(ce);
            this.linkTo(ce, BWbCellEditor.pluginModified, cellModified);
         }
      }

      this.sizeColumnsToFit();
   }

   public void doCellModified(BWidgetEvent event) {
      BWbCellEditor ce = (BWbCellEditor)event.getWidget();

      try {
         ce.saveValue();
      } catch (Exception var5) {
         throw new BajaRuntimeException(var5);
      }

      Property prop = this.binding.getProperty(ce.getPropertyName());
      BValue value = (BValue)ce.getCurrentValue();
      this.sheet.getCellContext().bindingPropertyChanged(this.binding, prop, value);
   }

   @Override
   protected CellTableContext makeContext(final BWbCellEditor ce) {
      return new CellTableContext() {
         @Override
         public boolean allowAnimate() {
            return false;
         }

         @Override
         public BComponent[] getGroupableComponents() {
            return new BComponent[]{BPxBindingTable.this.binding};
         }

         @Override
         public void doPaste() {
            try {
               ce.saveValue();
            } catch (Exception var3) {
               throw new BajaRuntimeException(var3);
            }

            Property prop = BPxBindingTable.this.binding.getProperty(ce.getPropertyName());
            BValue value = (BValue)ce.getCurrentValue();
            BPxBindingTable.this.sheet.getCellContext().bindingPropertyChanged(BPxBindingTable.this.binding, prop, value);
         }
      };
   }

   private BWbCellEditor makeCell(Property prop) {
      PxProperty group = this.sheet.getCellContext().getPxPropertyComponents().getPxProperty(this.binding, prop.getName());
      return (BWbCellEditor)(group == null
         ? this.editorPane.getPxEditor().getController().getCellEditor(this.binding, prop)
         : new BPxPropertyCE(new PxProperty[]{group}));
   }
}
