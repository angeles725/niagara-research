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
      value = "dfsStandby",
      ordinal = 0
   ), @Range(
      value = "dfsPumpdown",
      ordinal = 1
   ), @Range(
      value = "dfsDefrost",
      ordinal = 2
   ), @Range(
      value = "dfsDraindown",
      ordinal = 3
   ), @Range(
      value = "dfsInjectDly",
      ordinal = 4
   ), @Range(
      value = "dfsNul",
      ordinal = -1
   )}
)
public final class BLonDefrostStateEnum extends BFrozenEnum {
   public static final int DFS_STANDBY = 0;
   public static final int DFS_PUMPDOWN = 1;
   public static final int DFS_DEFROST = 2;
   public static final int DFS_DRAINDOWN = 3;
   public static final int DFS_INJECT_DLY = 4;
   public static final int DFS_NUL = -1;
   public static final BLonDefrostStateEnum dfsStandby = new BLonDefrostStateEnum(0);
   public static final BLonDefrostStateEnum dfsPumpdown = new BLonDefrostStateEnum(1);
   public static final BLonDefrostStateEnum dfsDefrost = new BLonDefrostStateEnum(2);
   public static final BLonDefrostStateEnum dfsDraindown = new BLonDefrostStateEnum(3);
   public static final BLonDefrostStateEnum dfsInjectDly = new BLonDefrostStateEnum(4);
   public static final BLonDefrostStateEnum dfsNul = new BLonDefrostStateEnum(-1);
   public static final BLonDefrostStateEnum DEFAULT = dfsStandby;
   public static final Type TYPE = Sys.loadType(BLonDefrostStateEnum.class);

   public static BLonDefrostStateEnum make(int ordinal) {
      return (BLonDefrostStateEnum)dfsStandby.getRange().get(ordinal, false);
   }

   public static BLonDefrostStateEnum make(String tag) {
      return (BLonDefrostStateEnum)dfsStandby.getRange().get(tag);
   }

   private BLonDefrostStateEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
