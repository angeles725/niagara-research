package javax.baja.bacnet.datatypes;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BBacnetActionList extends BComponent implements BIBacnetDataType {
   public static final Type TYPE = Sys.loadType(BBacnetActionList.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isChildLegal(BComponent child) {
      return child instanceof BBacnetActionCommand;
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      List<BBacnetActionCommand> actionCommands = new ArrayList<>();
      in.skipOpeningTag(0);

      for (int tag = in.peekTag(); !in.isClosingTag(0); tag = in.peekTag()) {
         if (tag == -1) {
            throw new AsnException("Invalid tag: -1");
         }

         BBacnetActionCommand actionCommand = new BBacnetActionCommand();
         actionCommand.readAsn(in);
         actionCommands.add(actionCommand);
      }

      in.skipClosingTag(0);
      this.removeAll(noWrite);

      for (BBacnetActionCommand actionCommand : actionCommands) {
         this.add(null, actionCommand, noWrite);
      }
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeOpeningTag(0);

      for (BBacnetActionCommand actionCommand : (BBacnetActionCommand[])this.getChildren(BBacnetActionCommand.class)) {
         actionCommand.writeAsn(out);
      }

      out.writeClosingTag(0);
   }

   public String toString(Context context) {
      if (context != null) {
         return super.toString(context);
      } else {
         this.loadSlots();
         StringJoiner joiner = new StringJoiner(",", "{", "}");

         for (BBacnetActionCommand actionCommand : (BBacnetActionCommand[])this.getChildren(BBacnetActionCommand.class)) {
            joiner.add(actionCommand.toString(context));
         }

         return joiner.toString();
      }
   }
}
