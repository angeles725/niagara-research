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
      value = "ctrlrNo",
      ordinal = 0
   ), @Range(
      value = "ctrlrPend",
      ordinal = 1
   ), @Range(
      value = "ctrlrRel",
      ordinal = 2
   ), @Range(
      value = "ctrlrQuery",
      ordinal = 3
   ), @Range(
      value = "ctrlrRes",
      ordinal = 4
   ), @Range(
      value = "ctrlrErr",
      ordinal = 5
   ), @Range(
      value = "ctrlrNul",
      ordinal = -1
   )},
   defaultValue = "ctrlrNul"
)
public final class BLonControlRespEnum extends BFrozenEnum {
   public static final int CTRLR_NO = 0;
   public static final int CTRLR_PEND = 1;
   public static final int CTRLR_REL = 2;
   public static final int CTRLR_QUERY = 3;
   public static final int CTRLR_RES = 4;
   public static final int CTRLR_ERR = 5;
   public static final int CTRLR_NUL = -1;
   public static final BLonControlRespEnum ctrlrNo = new BLonControlRespEnum(0);
   public static final BLonControlRespEnum ctrlrPend = new BLonControlRespEnum(1);
   public static final BLonControlRespEnum ctrlrRel = new BLonControlRespEnum(2);
   public static final BLonControlRespEnum ctrlrQuery = new BLonControlRespEnum(3);
   public static final BLonControlRespEnum ctrlrRes = new BLonControlRespEnum(4);
   public static final BLonControlRespEnum ctrlrErr = new BLonControlRespEnum(5);
   public static final BLonControlRespEnum ctrlrNul = new BLonControlRespEnum(-1);
   public static final BLonControlRespEnum DEFAULT = ctrlrNul;
   public static final Type TYPE = Sys.loadType(BLonControlRespEnum.class);

   public static BLonControlRespEnum make(int ordinal) {
      return (BLonControlRespEnum)ctrlrNo.getRange().get(ordinal, false);
   }

   public static BLonControlRespEnum make(String tag) {
      return (BLonControlRespEnum)ctrlrNo.getRange().get(tag);
   }

   private BLonControlRespEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
