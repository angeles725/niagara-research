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
      value = "dfrTermTemp",
      ordinal = 0
   ), @Range(
      value = "dfrTermTime",
      ordinal = 1
   ), @Range(
      value = "dfrTermFirst",
      ordinal = 2
   ), @Range(
      value = "dfrTermLast",
      ordinal = 3
   ), @Range(
      value = "dfrNul",
      ordinal = -1
   )}
)
public final class BLonDefrostTermEnum extends BFrozenEnum {
   public static final int DFR_TERM_TEMP = 0;
   public static final int DFR_TERM_TIME = 1;
   public static final int DFR_TERM_FIRST = 2;
   public static final int DFR_TERM_LAST = 3;
   public static final int DFR_NUL = -1;
   public static final BLonDefrostTermEnum dfrTermTemp = new BLonDefrostTermEnum(0);
   public static final BLonDefrostTermEnum dfrTermTime = new BLonDefrostTermEnum(1);
   public static final BLonDefrostTermEnum dfrTermFirst = new BLonDefrostTermEnum(2);
   public static final BLonDefrostTermEnum dfrTermLast = new BLonDefrostTermEnum(3);
   public static final BLonDefrostTermEnum dfrNul = new BLonDefrostTermEnum(-1);
   public static final BLonDefrostTermEnum DEFAULT = dfrTermTemp;
   public static final Type TYPE = Sys.loadType(BLonDefrostTermEnum.class);

   public static BLonDefrostTermEnum make(int ordinal) {
      return (BLonDefrostTermEnum)dfrTermTemp.getRange().get(ordinal, false);
   }

   public static BLonDefrostTermEnum make(String tag) {
      return (BLonDefrostTermEnum)dfrTermTemp.getRange().get(tag);
   }

   private BLonDefrostTermEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
