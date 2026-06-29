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
      value = "gfciUnknown",
      ordinal = 0
   ), @Range(
      value = "gfciNormal",
      ordinal = 1
   ), @Range(
      value = "gfciTripped",
      ordinal = 2
   ), @Range(
      value = "gfciTestFailed",
      ordinal = 3
   ), @Range(
      value = "gfciTestPassed",
      ordinal = 4
   ), @Range(
      value = "gfciTestNow",
      ordinal = 5
   ), @Range(
      value = "gfciNul",
      ordinal = -1
   )}
)
public final class BLonGfciStatusEnum extends BFrozenEnum {
   public static final int GFCI_UNKNOWN = 0;
   public static final int GFCI_NORMAL = 1;
   public static final int GFCI_TRIPPED = 2;
   public static final int GFCI_TEST_FAILED = 3;
   public static final int GFCI_TEST_PASSED = 4;
   public static final int GFCI_TEST_NOW = 5;
   public static final int GFCI_NUL = -1;
   public static final BLonGfciStatusEnum gfciUnknown = new BLonGfciStatusEnum(0);
   public static final BLonGfciStatusEnum gfciNormal = new BLonGfciStatusEnum(1);
   public static final BLonGfciStatusEnum gfciTripped = new BLonGfciStatusEnum(2);
   public static final BLonGfciStatusEnum gfciTestFailed = new BLonGfciStatusEnum(3);
   public static final BLonGfciStatusEnum gfciTestPassed = new BLonGfciStatusEnum(4);
   public static final BLonGfciStatusEnum gfciTestNow = new BLonGfciStatusEnum(5);
   public static final BLonGfciStatusEnum gfciNul = new BLonGfciStatusEnum(-1);
   public static final BLonGfciStatusEnum DEFAULT = gfciUnknown;
   public static final Type TYPE = Sys.loadType(BLonGfciStatusEnum.class);

   public static BLonGfciStatusEnum make(int ordinal) {
      return (BLonGfciStatusEnum)gfciUnknown.getRange().get(ordinal, false);
   }

   public static BLonGfciStatusEnum make(String tag) {
      return (BLonGfciStatusEnum)gfciUnknown.getRange().get(tag);
   }

   private BLonGfciStatusEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
