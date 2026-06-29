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
      value = "rqNormal",
      ordinal = 0
   ), @Range(
      value = "rqDisabled",
      ordinal = 1
   ), @Range(
      value = "rqUpdateStatus",
      ordinal = 2
   ), @Range(
      value = "rqSelfTest",
      ordinal = 3
   ), @Range(
      value = "rqUpdateAlarm",
      ordinal = 4
   ), @Range(
      value = "rqReportMask",
      ordinal = 5
   ), @Range(
      value = "rqOverride",
      ordinal = 6
   ), @Range(
      value = "rqEnable",
      ordinal = 7
   ), @Range(
      value = "rqRmvOverride",
      ordinal = 8
   ), @Range(
      value = "rqClearStatus",
      ordinal = 9
   ), @Range(
      value = "rqClearAlarm",
      ordinal = 10
   ), @Range(
      value = "rqAlarmNotifyEnabled",
      ordinal = 11
   ), @Range(
      value = "rqAlarmNotifyDisabled",
      ordinal = 12
   ), @Range(
      value = "rqManualCtrl",
      ordinal = 13
   ), @Range(
      value = "rqRemoteCtrl",
      ordinal = 14
   ), @Range(
      value = "rqProgram",
      ordinal = 15
   ), @Range(
      value = "rqNul",
      ordinal = -1
   )}
)
public final class BLonObjectRequestEnum extends BFrozenEnum {
   public static final int RQ_NORMAL = 0;
   public static final int RQ_DISABLED = 1;
   public static final int RQ_UPDATE_STATUS = 2;
   public static final int RQ_SELF_TEST = 3;
   public static final int RQ_UPDATE_ALARM = 4;
   public static final int RQ_REPORT_MASK = 5;
   public static final int RQ_OVERRIDE = 6;
   public static final int RQ_ENABLE = 7;
   public static final int RQ_RMV_OVERRIDE = 8;
   public static final int RQ_CLEAR_STATUS = 9;
   public static final int RQ_CLEAR_ALARM = 10;
   public static final int RQ_ALARM_NOTIFY_ENABLED = 11;
   public static final int RQ_ALARM_NOTIFY_DISABLED = 12;
   public static final int RQ_MANUAL_CTRL = 13;
   public static final int RQ_REMOTE_CTRL = 14;
   public static final int RQ_PROGRAM = 15;
   public static final int RQ_NUL = -1;
   public static final BLonObjectRequestEnum rqNormal = new BLonObjectRequestEnum(0);
   public static final BLonObjectRequestEnum rqDisabled = new BLonObjectRequestEnum(1);
   public static final BLonObjectRequestEnum rqUpdateStatus = new BLonObjectRequestEnum(2);
   public static final BLonObjectRequestEnum rqSelfTest = new BLonObjectRequestEnum(3);
   public static final BLonObjectRequestEnum rqUpdateAlarm = new BLonObjectRequestEnum(4);
   public static final BLonObjectRequestEnum rqReportMask = new BLonObjectRequestEnum(5);
   public static final BLonObjectRequestEnum rqOverride = new BLonObjectRequestEnum(6);
   public static final BLonObjectRequestEnum rqEnable = new BLonObjectRequestEnum(7);
   public static final BLonObjectRequestEnum rqRmvOverride = new BLonObjectRequestEnum(8);
   public static final BLonObjectRequestEnum rqClearStatus = new BLonObjectRequestEnum(9);
   public static final BLonObjectRequestEnum rqClearAlarm = new BLonObjectRequestEnum(10);
   public static final BLonObjectRequestEnum rqAlarmNotifyEnabled = new BLonObjectRequestEnum(11);
   public static final BLonObjectRequestEnum rqAlarmNotifyDisabled = new BLonObjectRequestEnum(12);
   public static final BLonObjectRequestEnum rqManualCtrl = new BLonObjectRequestEnum(13);
   public static final BLonObjectRequestEnum rqRemoteCtrl = new BLonObjectRequestEnum(14);
   public static final BLonObjectRequestEnum rqProgram = new BLonObjectRequestEnum(15);
   public static final BLonObjectRequestEnum rqNul = new BLonObjectRequestEnum(-1);
   public static final BLonObjectRequestEnum DEFAULT = rqNormal;
   public static final Type TYPE = Sys.loadType(BLonObjectRequestEnum.class);

   public static BLonObjectRequestEnum make(int ordinal) {
      return (BLonObjectRequestEnum)rqNormal.getRange().get(ordinal, false);
   }

   public static BLonObjectRequestEnum make(String tag) {
      return (BLonObjectRequestEnum)rqNormal.getRange().get(tag);
   }

   private BLonObjectRequestEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
