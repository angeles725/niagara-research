package javax.baja.bacnet.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("idle"), @Range("loading"), @Range("running"), @Range("waiting"), @Range("halted"), @Range("unloading")}
)
public final class BBacnetProgramState extends BFrozenEnum {
   public static final int IDLE = 0;
   public static final int LOADING = 1;
   public static final int RUNNING = 2;
   public static final int WAITING = 3;
   public static final int HALTED = 4;
   public static final int UNLOADING = 5;
   public static final BBacnetProgramState idle = new BBacnetProgramState(0);
   public static final BBacnetProgramState loading = new BBacnetProgramState(1);
   public static final BBacnetProgramState running = new BBacnetProgramState(2);
   public static final BBacnetProgramState waiting = new BBacnetProgramState(3);
   public static final BBacnetProgramState halted = new BBacnetProgramState(4);
   public static final BBacnetProgramState unloading = new BBacnetProgramState(5);
   public static final BBacnetProgramState DEFAULT = idle;
   public static final Type TYPE = Sys.loadType(BBacnetProgramState.class);

   public static BBacnetProgramState make(int ordinal) {
      return (BBacnetProgramState)idle.getRange().get(ordinal, false);
   }

   public static BBacnetProgramState make(String tag) {
      return (BBacnetProgramState)idle.getRange().get(tag);
   }

   private BBacnetProgramState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      return DEFAULT.getRange().getTag(id);
   }
}
