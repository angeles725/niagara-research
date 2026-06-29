package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("enable"), @Range("disable"), @Range("disableInitiation")}
)
public final class BBacnetCommControl extends BFrozenEnum {
   public static final int ENABLE = 0;
   public static final int DISABLE = 1;
   public static final int DISABLE_INITIATION = 2;
   public static final BBacnetCommControl enable = new BBacnetCommControl(0);
   public static final BBacnetCommControl disable = new BBacnetCommControl(1);
   public static final BBacnetCommControl disableInitiation = new BBacnetCommControl(2);
   public static final BBacnetCommControl DEFAULT = enable;
   public static final Type TYPE = Sys.loadType(BBacnetCommControl.class);

   public static BBacnetCommControl make(int ordinal) {
      return (BBacnetCommControl)enable.getRange().get(ordinal, false);
   }

   public static BBacnetCommControl make(String tag) {
      return (BBacnetCommControl)enable.getRange().get(tag);
   }

   private BBacnetCommControl(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      return DEFAULT.getRange().getTag(id);
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
