package com.tridium.program.ui;

import com.tridium.program.BProgram;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.Mark;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.ui.transfer.TransferFormat;
import javax.baja.ui.transfer.UnsupportedFormatException;
import javax.baja.ui.util.UiLexicon;
import javax.baja.workbench.mgr.BMgrTable;
import javax.baja.workbench.mgr.MgrModel;

@NiagaraType
public class BBuilderTable extends BMgrTable {
   public static final Type TYPE = Sys.loadType(BBuilderTable.class);

   public Type getType() {
      return TYPE;
   }

   public BBuilderTable(MgrModel model) {
      super(model);
   }

   public int getRowOfProgram(String name) {
      int numRows = this.getModel().getRowCount();

      for (int i = 0; i < numRows; i++) {
         if (this.getComponentAt(i).getDisplayName(null).equals(name)) {
            return i;
         }
      }

      return -1;
   }

   public int dragOver(TransferContext cx) {
      return this.isAllPrograms(cx) ? super.dragOver(cx) : 0;
   }

   public boolean isPasteSpecialEnabled() {
      return false;
   }

   public CommandArtifact insertTransferData(TransferContext cx) throws Exception {
      if (!this.isAllPrograms(cx)) {
         throw new UnsupportedFormatException(UiLexicon.bajaui().getText("command.paste.unsupportedFormat"));
      } else {
         return this.drop(cx);
      }
   }

   private boolean isAllPrograms(TransferContext cx) {
      Mark mark = (Mark)cx.getEnvelope().getData(TransferFormat.mark);
      BObject[] items = mark.getValues();

      for (int i = 0; i < items.length; i++) {
         if (!(items[i] instanceof BProgram)) {
            return false;
         }
      }

      return true;
   }
}
