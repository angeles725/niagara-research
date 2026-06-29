package javax.baja.bacnet.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "normal",
      ordinal = 0
   ), @Range(
      value = "starting",
      ordinal = 1
   ), @Range(
      value = "recovered",
      ordinal = 2
   ), @Range(
      value = "abnormal",
      ordinal = 3
   ), @Range(
      value = "failed",
      ordinal = 4
   )}
)
public final class BBacnetAccumulatorStatus extends BFrozenEnum {
   public static final int NORMAL = 0;
   public static final int STARTING = 1;
   public static final int RECOVERED = 2;
   public static final int ABNORMAL = 3;
   public static final int FAILED = 4;
   public static final BBacnetAccumulatorStatus normal = new BBacnetAccumulatorStatus(0);
   public static final BBacnetAccumulatorStatus starting = new BBacnetAccumulatorStatus(1);
   public static final BBacnetAccumulatorStatus recovered = new BBacnetAccumulatorStatus(2);
   public static final BBacnetAccumulatorStatus abnormal = new BBacnetAccumulatorStatus(3);
   public static final BBacnetAccumulatorStatus failed = new BBacnetAccumulatorStatus(4);
   public static final BBacnetAccumulatorStatus DEFAULT = normal;
   public static final Type TYPE = Sys.loadType(BBacnetAccumulatorStatus.class);

   public static BBacnetAccumulatorStatus make(int ordinal) {
      return (BBacnetAccumulatorStatus)normal.getRange().get(ordinal, false);
   }

   public static BBacnetAccumulatorStatus make(String tag) {
      return (BBacnetAccumulatorStatus)normal.getRange().get(tag);
   }

   private BBacnetAccumulatorStatus(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
