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
      value = "occupied",
      ordinal = 0
   ), @Range(
      value = "unoccupied",
      ordinal = 1
   ), @Range(
      value = "bypass",
      ordinal = 2
   ), @Range(
      value = "standby",
      ordinal = 3
   ), @Range(
      value = "occNull",
      ordinal = -1
   )},
   defaultValue = "occNull"
)
public final class BLonOccupancyEnum extends BFrozenEnum {
   public static final int OCCUPIED = 0;
   public static final int UNOCCUPIED = 1;
   public static final int BYPASS = 2;
   public static final int STANDBY = 3;
   public static final int OCC_NULL = -1;
   public static final BLonOccupancyEnum occupied = new BLonOccupancyEnum(0);
   public static final BLonOccupancyEnum unoccupied = new BLonOccupancyEnum(1);
   public static final BLonOccupancyEnum bypass = new BLonOccupancyEnum(2);
   public static final BLonOccupancyEnum standby = new BLonOccupancyEnum(3);
   public static final BLonOccupancyEnum occNull = new BLonOccupancyEnum(-1);
   public static final BLonOccupancyEnum DEFAULT = occNull;
   public static final Type TYPE = Sys.loadType(BLonOccupancyEnum.class);

   public static BLonOccupancyEnum make(int ordinal) {
      return (BLonOccupancyEnum)occupied.getRange().get(ordinal, false);
   }

   public static BLonOccupancyEnum make(String tag) {
      return (BLonOccupancyEnum)occupied.getRange().get(tag);
   }

   private BLonOccupancyEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
