package com.tridium.program.ui.batch;

import com.tridium.program.batch.BRemoveSlotBatchRoutine;
import com.tridium.ui.BOptionDialog;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.util.Lexicon;

@NiagaraType
public class BRemoveDialog extends BOptionDialog {
   public static final Type TYPE = Sys.loadType(BRemoveDialog.class);
   private static Lexicon lex = Lexicon.make("program");
   BListDropDown dropDown = new BListDropDown();

   public Type getType() {
      return TYPE;
   }

   public static BRemoveSlotBatchRoutine open(BBatchEditor editor) {
      BRemoveDialog dlg = new BRemoveDialog(editor);
      dlg.setBoundsCenteredOnOwner();
      dlg.open();
      if (dlg.getResult() == 1) {
         try {
            String prop = (String)dlg.dropDown.getSelectedItem();
            return BRemoveSlotBatchRoutine.make(SlotPath.escape(prop));
         } catch (Exception var3) {
            BDialog.error(editor, "Error", "Failed", var3);
         }
      }

      return null;
   }

   private BRemoveDialog(BBatchEditor parent) {
      super(parent, lex.getText("batchEditor.commands.slotRemove.label"), new BNullWidget(), 3, null, null);
      String[] cols = parent.table.model.getAllColumns(false);

      for (String col : cols) {
         this.dropDown.getList().addItem(SlotPath.unescape(col));
      }

      BGridPane grid = new BGridPane(2);
      grid.add(null, new BLabel(lex.getText("batchEditor.property")));
      grid.add(null, this.dropDown);
      this.setContent(new BBorderPane(grid, 10.0, 10.0, 10.0, 10.0));
   }
}
