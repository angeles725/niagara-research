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
      value = "lnRecall",
      ordinal = 0
   ), @Range(
      value = "lnLearnCurrent",
      ordinal = 1
   ), @Range(
      value = "lnLearnValue",
      ordinal = 2
   ), @Range(
      value = "lnReportValue",
      ordinal = 3
   ), @Range(
      value = "lnNul",
      ordinal = -1
   )}
)
public final class BLonLearnModeEnum extends BFrozenEnum {
   public static final int LN_RECALL = 0;
   public static final int LN_LEARN_CURRENT = 1;
   public static final int LN_LEARN_VALUE = 2;
   public static final int LN_REPORT_VALUE = 3;
   public static final int LN_NUL = -1;
   public static final BLonLearnModeEnum lnRecall = new BLonLearnModeEnum(0);
   public static final BLonLearnModeEnum lnLearnCurrent = new BLonLearnModeEnum(1);
   public static final BLonLearnModeEnum lnLearnValue = new BLonLearnModeEnum(2);
   public static final BLonLearnModeEnum lnReportValue = new BLonLearnModeEnum(3);
   public static final BLonLearnModeEnum lnNul = new BLonLearnModeEnum(-1);
   public static final BLonLearnModeEnum DEFAULT = lnRecall;
   public static final Type TYPE = Sys.loadType(BLonLearnModeEnum.class);

   public static BLonLearnModeEnum make(int ordinal) {
      return (BLonLearnModeEnum)lnRecall.getRange().get(ordinal, false);
   }

   public static BLonLearnModeEnum make(String tag) {
      return (BLonLearnModeEnum)lnRecall.getRange().get(tag);
   }

   private BLonLearnModeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
