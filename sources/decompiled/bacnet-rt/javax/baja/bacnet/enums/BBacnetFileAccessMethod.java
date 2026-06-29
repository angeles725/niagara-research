package javax.baja.bacnet.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("recordAccess"), @Range("streamAccess")}
)
public final class BBacnetFileAccessMethod extends BFrozenEnum {
   public static final int RECORD_ACCESS = 0;
   public static final int STREAM_ACCESS = 1;
   public static final BBacnetFileAccessMethod recordAccess = new BBacnetFileAccessMethod(0);
   public static final BBacnetFileAccessMethod streamAccess = new BBacnetFileAccessMethod(1);
   public static final BBacnetFileAccessMethod DEFAULT = recordAccess;
   public static final Type TYPE = Sys.loadType(BBacnetFileAccessMethod.class);

   public static BBacnetFileAccessMethod make(int ordinal) {
      return (BBacnetFileAccessMethod)recordAccess.getRange().get(ordinal, false);
   }

   public static BBacnetFileAccessMethod make(String tag) {
      return (BBacnetFileAccessMethod)recordAccess.getRange().get(tag);
   }

   private BBacnetFileAccessMethod(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      return DEFAULT.getRange().getTag(id);
   }
}
