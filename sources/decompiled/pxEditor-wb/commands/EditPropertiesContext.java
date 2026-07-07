package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.PxMediaValidationUtil;
import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheet;
import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheetBindingLabel;
import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheetLabel;
import com.tridium.px.editor.sidebars.cellsheet.CellSheetContext;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BConverterCE;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BPxLayerCE;
import com.tridium.px.editor.util.LayerManager;
import com.tridium.px.editor.util.SelectedWidgets;
import com.tridium.ui.px.PxPropertyComponentArray;
import com.tridium.ui.util.ValidationUtil;
import java.util.Map;
import javax.baja.naming.SyntaxException;
import javax.baja.naming.UnresolvedException;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.event.PxCompoundWidgetEvent;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Property;
import javax.baja.ui.BBinding;
import javax.baja.ui.BDialog;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.px.PxLayer;
import javax.baja.ui.px.PxProperty;
import javax.baja.ui.shape.BShape;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.util.BConverter;
import javax.baja.workbench.celleditor.BWbCellEditor;

public class EditPropertiesContext extends Command implements CellSheetContext {
   private BPxEditor editor;
   private BPxEditorPane editorPane;
   private BPxCellSheet sheet;
   private PxPropertyComponentArray oldGc;
   private PxPropertyComponentArray newGc;
   private BWidget[] oldWidgets;
   private BWidget[] newWidgets;

   public EditPropertiesContext(BPxEditor editor) {
      super(editor, BPxEditorPane.lexicon(), "commands.editProperties");
      this.editor = editor;
      this.editorPane = (BPxEditorPane)editor.getContent();
   }

   public CommandArtifact doInvoke() throws Exception {
      SelectedWidgets sel = this.editorPane.getSelectedWidgets();
      if (sel.size() == 0) {
         throw new IllegalStateException();
      } else {
         this.oldWidgets = this.editorPane.getSelectedWidgets().getWidgets();

         for (int i = 0; i < this.oldWidgets.length; i++) {
            if (this.oldWidgets[i] instanceof BNullWidget) {
               return null;
            }
         }

         this.newWidgets = this.editor.cloneWidgets(this.oldWidgets);
         this.oldGc = this.editorPane.getPxPropertyComponents();
         this.newGc = this.oldGc.deepClone();
         this.newGc.replaceWidgets(this.oldWidgets, this.newWidgets);
         this.sheet = new BPxCellSheet(this.editor, this);
         this.sheet.setReadonly(this.editor.isReadonly());
         this.sheet.editWidgets(this.newWidgets);
         BConstrainedPane cons = new BConstrainedPane(this.sheet);
         cons.setMinWidth(500.0);
         String title = this.editorPane.getLexicon().getText("cellsheet.label");
         if (this.editor.isReadonly()) {
            BDialog.open(this.editorPane, title, cons, 1);
            return null;
         } else if (BEditPropertiesDialog.open(this.sheet, this.editorPane, title, cons, 3) != 1) {
            return null;
         } else {
            Map<String, String> oldWarnings = ValidationUtil.getValidationWarnings(
               this.editor.getMedia(), this.oldWidgets, false, this.editor.getCurrentContext()
            );
            EditPropertiesContext.Artifact artifact = new EditPropertiesContext.Artifact();
            artifact.redo();
            Map<String, String> newWarnings = ValidationUtil.getValidationWarnings(
               this.editor.getMedia(), this.newWidgets, false, this.editor.getCurrentContext()
            );
            if (newWarnings.size() > oldWarnings.size()) {
               oldWarnings.forEach((ord, oldMessage) -> {
                  String newMessage = newWarnings.get(ord);
                  if (oldMessage != null && newMessage != null && oldMessage.equals(newMessage)) {
                     newWarnings.remove(ord);
                  }
               });
               if (newWarnings.size() > 0) {
                  int result = PxMediaValidationUtil.confirmNewWarningsDialog(newWarnings, this.editor);
                  if (result == 8) {
                     artifact.undo();
                  }
               }
            }

            return artifact;
         }
      }
   }

   @Override
   public boolean allowBindingDelete() {
      return true;
   }

   @Override
   public boolean allowGotoOrd() {
      return false;
   }

   @Override
   public boolean allowLayoutEdit(BWidget[] widgets) {
      for (int i = 0; i < this.oldWidgets.length; i++) {
         if (this.oldWidgets[i] instanceof BShape) {
            return false;
         }

         if (!(this.oldWidgets[i].getParentWidget() instanceof BCanvasPane)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public BObject[] resolveBindingTarget(BBinding[] bindings) {
      BObject[] from = new BObject[bindings.length];
      BObject base = this.editor.getWbShell().getActiveOrdTarget().get();

      for (int i = 0; i < bindings.length; i++) {
         try {
            from[i] = bindings[i].getOrd().get(base);
         } catch (UnresolvedException var6) {
            from[i] = null;
         } catch (SyntaxException var7) {
            from[i] = null;
         }
      }

      return from;
   }

   @Override
   public void bindingAdded(BWidget widget, Property property, BBinding binding) {
      this.sheet.editWidgets(this.newWidgets);
   }

   @Override
   public void bindingDeleted(BWidget widget, Property property, BBinding binding) {
      this.sheet.editWidgets(this.newWidgets);
   }

   @Override
   public void converterAdded(BBinding binding, Property property, BConverter converter) {
      this.sheet.editWidgets(this.newWidgets);
   }

   @Override
   public void converterDeleted(BBinding binding, Property property, BConverter converter) {
      this.sheet.editWidgets(this.newWidgets);
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
         LayerManager layerMgr = this.editorPane.getLayerManager();
         BPxLayerCE layerCE = (BPxLayerCE)ce;
         PxLayer layer = layerMgr.getLayerByName(layerCE.getListDropDown().getList().getSelectedItem().toString());

         for (int i = 0; i < this.newWidgets.length; i++) {
            layerMgr.removeTag(this.newWidgets[i]);
            if (layer != null) {
               layerMgr.addTag(this.newWidgets[i], layer);
            }
         }
      } else {
         String name = ce.getPropertyName();
         BValue value = (BValue)ce.getCurrentValue();

         for (int ix = 0; ix < this.newWidgets.length; ix++) {
            this.newWidgets[ix].set(name, value);
            this.newWidgets[ix].bindingsChanged();
         }
      }
   }

   @Override
   public PxPropertyComponentArray getPxPropertyComponents() {
      return this.newGc;
   }

   @Override
   public void pxPropertyLinked(PxProperty group, BComponent comp, String propName) {
      this.newGc.addTarget(group, comp, propName);
      this.sheet.editWidgets(this.newWidgets);
   }

   @Override
   public void pxPropertyUnlinked(PxProperty group, BComponent comp, String propName) {
      this.newGc.removeTarget(group, comp, propName);
      this.sheet.editWidgets(this.newWidgets);
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         this.go(EditPropertiesContext.this.oldWidgets, EditPropertiesContext.this.newWidgets, EditPropertiesContext.this.newGc);
      }

      public void undo() throws Exception {
         this.go(EditPropertiesContext.this.newWidgets, EditPropertiesContext.this.oldWidgets, EditPropertiesContext.this.oldGc);
      }

      private void go(BWidget[] from, BWidget[] to, PxPropertyComponentArray gc) {
         PxWidgetEvent[] events = new PxWidgetEvent[from.length];

         for (int i = 0; i < from.length; i++) {
            BWidget parent = from[i].getParentWidget();
            Property prop = from[i].getPropertyInParent();
            String propName = prop.getName();
            parent.set(prop, to[i]);
            events[i] = new PxWidgetEvent(2, parent, propName, to[i]);
         }

         EditPropertiesContext.this.editorPane.setPxPropertyComponents(gc);
         EditPropertiesContext.this.editorPane.getSelectedWidgets().setWidgets(to);
         EditPropertiesContext.this.editor.firePxEvent(new PxCompoundWidgetEvent(2, events));
      }
   }
}
