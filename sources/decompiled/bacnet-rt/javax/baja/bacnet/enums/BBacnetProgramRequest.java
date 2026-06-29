package javax.baja.bacnet.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("ready"), @Range("load"), @Range("run"), @Range("halt"), @Range("restart"), @Range("unload")}
)
public final class BBacnetProgramRequest extends BFrozenEnum {
   public static final int READY = 0;
   public static final int LOAD = 1;
   public static final int RUN = 2;
   public static final int HALT = 3;
   public static final int RESTART = 4;
   public static final int UNLOAD = 5;
   public static final BBacnetProgramRequest ready = new BBacnetProgramRequest(0);
   public static final BBacnetProgramRequest load = new BBacnetProgramRequest(1);
   public static final BBacnetProgramRequest run = new BBacnetProgramRequest(2);
   public static final BBacnetProgramRequest halt = new BBacnetProgramRequest(3);
   public static final BBacnetProgramRequest restart = new BBacnetProgramRequest(4);
   public static final BBacnetProgramRequest unload = new BBacnetProgramRequest(5);
   public static final BBacnetProgramRequest DEFAULT = ready;
   public static final Type TYPE = Sys.loadType(BBacnetProgramRequest.class);

   public static BBacnetProgramRequest make(int ordinal) {
      return (BBacnetProgramRequest)ready.getRange().get(ordinal, false);
   }

   public static BBacnetProgramRequest make(String tag) {
      return (BBacnetProgramRequest)ready.getRange().get(tag);
   }

   private BBacnetProgramRequest(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      return DEFAULT.getRange().getTag(id);
   }
}
