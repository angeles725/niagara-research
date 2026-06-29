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
      value = "motorStopped",
      ordinal = 0
   ), @Range(
      value = "motorStarting",
      ordinal = 1
   ), @Range(
      value = "motorAccelerating",
      ordinal = 2
   ), @Range(
      value = "motorAtStandby",
      ordinal = 3
   ), @Range(
      value = "motorAtNormal",
      ordinal = 4
   ), @Range(
      value = "motorAtReference",
      ordinal = 5
   ), @Range(
      value = "motorDecelerating",
      ordinal = 6
   ), @Range(
      value = "motorStopping",
      ordinal = 7
   ), @Range(
      value = "motorNul",
      ordinal = -1
   )}
)
public final class BLonMotorStateEnum extends BFrozenEnum {
   public static final int MOTOR_STOPPED = 0;
   public static final int MOTOR_STARTING = 1;
   public static final int MOTOR_ACCELERATING = 2;
   public static final int MOTOR_AT_STANDBY = 3;
   public static final int MOTOR_AT_NORMAL = 4;
   public static final int MOTOR_AT_REFERENCE = 5;
   public static final int MOTOR_DECELERATING = 6;
   public static final int MOTOR_STOPPING = 7;
   public static final int MOTOR_NUL = -1;
   public static final BLonMotorStateEnum motorStopped = new BLonMotorStateEnum(0);
   public static final BLonMotorStateEnum motorStarting = new BLonMotorStateEnum(1);
   public static final BLonMotorStateEnum motorAccelerating = new BLonMotorStateEnum(2);
   public static final BLonMotorStateEnum motorAtStandby = new BLonMotorStateEnum(3);
   public static final BLonMotorStateEnum motorAtNormal = new BLonMotorStateEnum(4);
   public static final BLonMotorStateEnum motorAtReference = new BLonMotorStateEnum(5);
   public static final BLonMotorStateEnum motorDecelerating = new BLonMotorStateEnum(6);
   public static final BLonMotorStateEnum motorStopping = new BLonMotorStateEnum(7);
   public static final BLonMotorStateEnum motorNul = new BLonMotorStateEnum(-1);
   public static final BLonMotorStateEnum DEFAULT = motorStopped;
   public static final Type TYPE = Sys.loadType(BLonMotorStateEnum.class);

   public static BLonMotorStateEnum make(int ordinal) {
      return (BLonMotorStateEnum)motorStopped.getRange().get(ordinal, false);
   }

   public static BLonMotorStateEnum make(String tag) {
      return (BLonMotorStateEnum)motorStopped.getRange().get(tag);
   }

   private BLonMotorStateEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
