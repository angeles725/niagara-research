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
      value = "cmaSave",
      ordinal = 0
   ), @Range(
      value = "cmaCall",
      ordinal = 1
   ), @Range(
      value = "cmaRead",
      ordinal = 2
   ), @Range(
      value = "cmaNul",
      ordinal = -1
   )},
   defaultValue = "cmaNul"
)
public final class BLonCamActEnum extends BFrozenEnum {
   public static final int CMA_SAVE = 0;
   public static final int CMA_CALL = 1;
   public static final int CMA_READ = 2;
   public static final int CMA_NUL = -1;
   public static final BLonCamActEnum cmaSave = new BLonCamActEnum(0);
   public static final BLonCamActEnum cmaCall = new BLonCamActEnum(1);
   public static final BLonCamActEnum cmaRead = new BLonCamActEnum(2);
   public static final BLonCamActEnum cmaNul = new BLonCamActEnum(-1);
   public static final BLonCamActEnum DEFAULT = cmaNul;
   public static final Type TYPE = Sys.loadType(BLonCamActEnum.class);

   public static BLonCamActEnum make(int ordinal) {
      return (BLonCamActEnum)cmaSave.getRange().get(ordinal, false);
   }

   public static BLonCamActEnum make(String tag) {
      return (BLonCamActEnum)cmaSave.getRange().get(tag);
   }

   private BLonCamActEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
