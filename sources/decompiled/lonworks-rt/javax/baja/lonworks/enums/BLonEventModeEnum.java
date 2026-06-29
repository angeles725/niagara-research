package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "endOfList",
      ordinal = 0
   ), @Range(
      value = "scene",
      ordinal = 1
   ), @Range(
      value = "mode",
      ordinal = 2
   )},
   defaultValue = "endOfList"
)
public final class BLonEventModeEnum extends BFrozenEnum {
   public static final int END_OF_LIST = 0;
   public static final int SCENE = 1;
   public static final int MODE = 2;
   public static final BLonEventModeEnum endOfList = new BLonEventModeEnum(0);
   public static final BLonEventModeEnum scene = new BLonEventModeEnum(1);
   public static final BLonEventModeEnum mode = new BLonEventModeEnum(2);
   public static final BLonEventModeEnum DEFAULT = endOfList;
   public static final Type TYPE = Sys.loadType(BLonEventModeEnum.class);

   public static BLonEventModeEnum make(int ordinal) {
      return (BLonEventModeEnum)endOfList.getRange().get(ordinal, false);
   }

   public static BLonEventModeEnum make(String tag) {
      return (BLonEventModeEnum)endOfList.getRange().get(tag);
   }

   private BLonEventModeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
