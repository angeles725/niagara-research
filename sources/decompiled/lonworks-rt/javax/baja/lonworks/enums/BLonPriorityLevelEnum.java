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
      value = "prLevel0",
      ordinal = 0
   ), @Range(
      value = "prLevel1",
      ordinal = 1
   ), @Range(
      value = "prLevel2",
      ordinal = 2
   ), @Range(
      value = "prLevel3",
      ordinal = 3
   ), @Range(
      value = "pr1",
      ordinal = 4
   ), @Range(
      value = "pr2",
      ordinal = 5
   ), @Range(
      value = "pr3",
      ordinal = 6
   ), @Range(
      value = "pr4",
      ordinal = 7
   ), @Range(
      value = "pr6",
      ordinal = 8
   ), @Range(
      value = "pr8",
      ordinal = 9
   ), @Range(
      value = "pr10",
      ordinal = 10
   ), @Range(
      value = "pr16",
      ordinal = 11
   ), @Range(
      value = "prNul",
      ordinal = -1
   )}
)
public final class BLonPriorityLevelEnum extends BFrozenEnum {
   public static final int PR_LEVEL_0 = 0;
   public static final int PR_LEVEL_1 = 1;
   public static final int PR_LEVEL_2 = 2;
   public static final int PR_LEVEL_3 = 3;
   public static final int PR_1 = 4;
   public static final int PR_2 = 5;
   public static final int PR_3 = 6;
   public static final int PR_4 = 7;
   public static final int PR_6 = 8;
   public static final int PR_8 = 9;
   public static final int PR_10 = 10;
   public static final int PR_16 = 11;
   public static final int PR_NUL = -1;
   public static final BLonPriorityLevelEnum prLevel0 = new BLonPriorityLevelEnum(0);
   public static final BLonPriorityLevelEnum prLevel1 = new BLonPriorityLevelEnum(1);
   public static final BLonPriorityLevelEnum prLevel2 = new BLonPriorityLevelEnum(2);
   public static final BLonPriorityLevelEnum prLevel3 = new BLonPriorityLevelEnum(3);
   public static final BLonPriorityLevelEnum pr1 = new BLonPriorityLevelEnum(4);
   public static final BLonPriorityLevelEnum pr2 = new BLonPriorityLevelEnum(5);
   public static final BLonPriorityLevelEnum pr3 = new BLonPriorityLevelEnum(6);
   public static final BLonPriorityLevelEnum pr4 = new BLonPriorityLevelEnum(7);
   public static final BLonPriorityLevelEnum pr6 = new BLonPriorityLevelEnum(8);
   public static final BLonPriorityLevelEnum pr8 = new BLonPriorityLevelEnum(9);
   public static final BLonPriorityLevelEnum pr10 = new BLonPriorityLevelEnum(10);
   public static final BLonPriorityLevelEnum pr16 = new BLonPriorityLevelEnum(11);
   public static final BLonPriorityLevelEnum prNul = new BLonPriorityLevelEnum(-1);
   public static final BLonPriorityLevelEnum DEFAULT = prLevel0;
   public static final Type TYPE = Sys.loadType(BLonPriorityLevelEnum.class);

   public static BLonPriorityLevelEnum make(int ordinal) {
      return (BLonPriorityLevelEnum)prLevel0.getRange().get(ordinal, false);
   }

   public static BLonPriorityLevelEnum make(String tag) {
      return (BLonPriorityLevelEnum)prLevel0.getRange().get(tag);
   }

   private BLonPriorityLevelEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
