package com.tridium.px.editor.commands;

import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheet;
import com.tridium.ui.BOptionDialog;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BWindowEvent;

@NiagaraType
public class BEditPropertiesDialog extends BOptionDialog {
   public static final Type TYPE = Sys.loadType(BEditPropertiesDialog.class);
   private BPxCellSheet sheet;

   public Type getType() {
      return TYPE;
   }

   private BEditPropertiesDialog(BPxCellSheet sheet, BWidget owner, String title, BWidget content, int buttons) {
      super(owner, title, content, buttons, null, (String)null);
      this.sheet = sheet;
   }

   public BEditPropertiesDialog() {
   }

   public void windowOpened(BWindowEvent event) {
      this.sheet.selectTextEditor();
   }

   public static int open(BPxCellSheet sheet, BWidget parent, String title, BWidget content, int buttons) {
      BEditPropertiesDialog dialog = new BEditPropertiesDialog(sheet, parent, title, content, buttons);
      dialog.setBoundsCenteredOnOwner();
      dialog.open();
      if (content instanceof BWidget && content.getPropertyInParent() != null) {
         BWidget wp = content.getParentWidget();
         wp.set(content.getPropertyInParent(), new BNullWidget());
      }

      return dialog.getResult();
   }
}
