package com.tridium.program.ui.batch;

import com.tridium.program.batch.BBatchRoutine;
import com.tridium.program.batch.BRenameBatchRoutine;
import com.tridium.program.batch.BRenameSlotBatchRoutine;
import com.tridium.ui.BOptionDialog;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BTextField;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.util.Lexicon;

@NiagaraType
public class BRenameDialog extends BOptionDialog {
   public static final Type TYPE = Sys.loadType(BRenameDialog.class);
   private static Lexicon lex = Lexicon.make("program");
   private BTextField find = new BTextField("", 30);
   private BTextField replace = new BTextField("", 30);
   private BCheckBox matchCase = new BCheckBox(lex.getText("batchEditor.matchCase"));
   private BCheckBox matchWord = new BCheckBox(lex.getText("batchEditor.matchWord"));

   public Type getType() {
      return TYPE;
   }

   public static BBatchRoutine open(BBatchEditor editor, boolean slot) {
      BRenameDialog dlg = new BRenameDialog(editor, slot);
      dlg.setBoundsCenteredOnOwner();
      dlg.open();
      if (dlg.getResult() == 1) {
         try {
            String find = SlotPath.escape(dlg.find.getText());
            String replace = SlotPath.escape(dlg.replace.getText());
            if (slot) {
               return BRenameSlotBatchRoutine.make(find, replace, dlg.matchCase.isSelected(), dlg.matchWord.isSelected());
            }

            return BRenameBatchRoutine.make(find, replace, dlg.matchCase.isSelected(), dlg.matchWord.isSelected());
         } catch (Exception var5) {
            BDialog.error(editor, "Error", "Failed", var5);
         }
      }

      return null;
   }

   private BRenameDialog(BBatchEditor parent, boolean slot) {
      super(parent, lex.getText(slot ? "batchEditor.commands.slotRename.label" : "batchEditor.commands.rename.label"), new BNullWidget(), 3, null, null);
      BGridPane grid = new BGridPane(2);
      grid.add(null, new BLabel(lex.getText("batchEditor.find")));
      grid.add(null, this.find);
      grid.add(null, new BLabel(lex.getText("batchEditor.replace")));
      grid.add(null, this.replace);
      BGridPane grid2 = new BGridPane(1);
      grid2.add(null, this.matchCase);
      grid2.add(null, this.matchWord);
      BGridPane grid3 = new BGridPane(1);
      grid3.add(null, grid);
      grid3.add(null, grid2);
      this.setContent(new BBorderPane(grid3, 10.0, 10.0, 10.0, 10.0));
   }
}
