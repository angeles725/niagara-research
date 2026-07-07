package com.tridium.px.editor.sidebars.cellsheet.celleditors;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.fieldeditors.BIEnumToSimpleFE;
import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheet;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIEnum;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BDialog;
import javax.baja.util.BConverter;
import javax.baja.workbench.celleditor.BButtonCE;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
public class BConverterCE extends BButtonCE {
   public static final Type TYPE = Sys.loadType(BConverterCE.class);
   private static final BBrush BG = BColor.make(255, 255, 187).toBrush();
   private BPxEditorPane editorPane;
   private BPxCellSheet sheet;
   private BBinding binding;
   private BConverter orig;

   public Type getType() {
      return TYPE;
   }

   public BConverterCE(BPxEditorPane editorPane, BPxCellSheet sheet, BConverter orig) {
      this.editorPane = editorPane;
      this.sheet = sheet;
      this.binding = (BBinding)orig.getParent();
      this.orig = orig;
   }

   public BBrush getBackground() {
      return BG;
   }

   public void paint(Graphics g) {
      this.paintBackground(g);
      this.paintButton(g);
      this.drawString(g, this.binding.getOrd().toString());
   }

   protected BObject dialog() throws Exception {
      BWbFieldEditor fe = BWbFieldEditor.makeFor(this.value);
      Context cx = this.getCurrentContext();
      if (fe instanceof BIEnumToSimpleFE) {
         BObject from = this.sheet.getCellContext().resolveBindingTarget(new BBinding[]{this.binding})[0];
         if (from == null) {
            throw new BajaRuntimeException("Cannot resolve binding target " + this.binding.getOrd());
         }

         cx = ((BIEnum)from).getEnumFacets();
      }

      fe.loadValue(this.value, cx);
      int result = BDialog.open(this, this.getPropertyName(), fe, 3);
      return result == 2 ? null : fe.saveValue();
   }

   public BBinding binding() {
      return this.binding;
   }

   public BConverter converter() {
      return this.orig;
   }
}
