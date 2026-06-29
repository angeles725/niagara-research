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
      value = "alNoCondition",
      ordinal = 0
   ), @Range(
      value = "alAlmCondition",
      ordinal = 1
   ), @Range(
      value = "alTotSvcAlm1",
      ordinal = 2
   ), @Range(
      value = "alTotSvcAlm2",
      ordinal = 3
   ), @Range(
      value = "alTotSvcAlm3",
      ordinal = 4
   ), @Range(
      value = "alLowLmtClr1",
      ordinal = 5
   ), @Range(
      value = "alLowLmtClr2",
      ordinal = 6
   ), @Range(
      value = "alHighLmtClr1",
      ordinal = 7
   ), @Range(
      value = "alHighLmtClr2",
      ordinal = 8
   ), @Range(
      value = "alLowLmtAlm1",
      ordinal = 9
   ), @Range(
      value = "alLowLmtAlm2",
      ordinal = 10
   ), @Range(
      value = "alHighLmtAlm1",
      ordinal = 11
   ), @Range(
      value = "alHighLmtAlm2",
      ordinal = 12
   ), @Range(
      value = "alFirAlm",
      ordinal = 13
   ), @Range(
      value = "alFirPreAlm",
      ordinal = 14
   ), @Range(
      value = "alFirTrbl",
      ordinal = 15
   ), @Range(
      value = "alFirSupv",
      ordinal = 16
   ), @Range(
      value = "alFirTestAlm",
      ordinal = 17
   ), @Range(
      value = "alFirTestPreAlm",
      ordinal = 18
   ), @Range(
      value = "alFirEnvcompMax",
      ordinal = 19
   ), @Range(
      value = "alFirMonitorCond",
      ordinal = 20
   ), @Range(
      value = "alFirMaintAlert",
      ordinal = 21
   ), @Range(
      value = "alNul",
      ordinal = -1
   )}
)
public final class BLonAlarmTypeEnum extends BFrozenEnum {
   public static final int AL_NO_CONDITION = 0;
   public static final int AL_ALM_CONDITION = 1;
   public static final int AL_TOT_SVC_ALM_1 = 2;
   public static final int AL_TOT_SVC_ALM_2 = 3;
   public static final int AL_TOT_SVC_ALM_3 = 4;
   public static final int AL_LOW_LMT_CLR_1 = 5;
   public static final int AL_LOW_LMT_CLR_2 = 6;
   public static final int AL_HIGH_LMT_CLR_1 = 7;
   public static final int AL_HIGH_LMT_CLR_2 = 8;
   public static final int AL_LOW_LMT_ALM_1 = 9;
   public static final int AL_LOW_LMT_ALM_2 = 10;
   public static final int AL_HIGH_LMT_ALM_1 = 11;
   public static final int AL_HIGH_LMT_ALM_2 = 12;
   public static final int AL_FIR_ALM = 13;
   public static final int AL_FIR_PRE_ALM = 14;
   public static final int AL_FIR_TRBL = 15;
   public static final int AL_FIR_SUPV = 16;
   public static final int AL_FIR_TEST_ALM = 17;
   public static final int AL_FIR_TEST_PRE_ALM = 18;
   public static final int AL_FIR_ENVCOMP_MAX = 19;
   public static final int AL_FIR_MONITOR_COND = 20;
   public static final int AL_FIR_MAINT_ALERT = 21;
   public static final int AL_NUL = -1;
   public static final BLonAlarmTypeEnum alNoCondition = new BLonAlarmTypeEnum(0);
   public static final BLonAlarmTypeEnum alAlmCondition = new BLonAlarmTypeEnum(1);
   public static final BLonAlarmTypeEnum alTotSvcAlm1 = new BLonAlarmTypeEnum(2);
   public static final BLonAlarmTypeEnum alTotSvcAlm2 = new BLonAlarmTypeEnum(3);
   public static final BLonAlarmTypeEnum alTotSvcAlm3 = new BLonAlarmTypeEnum(4);
   public static final BLonAlarmTypeEnum alLowLmtClr1 = new BLonAlarmTypeEnum(5);
   public static final BLonAlarmTypeEnum alLowLmtClr2 = new BLonAlarmTypeEnum(6);
   public static final BLonAlarmTypeEnum alHighLmtClr1 = new BLonAlarmTypeEnum(7);
   public static final BLonAlarmTypeEnum alHighLmtClr2 = new BLonAlarmTypeEnum(8);
   public static final BLonAlarmTypeEnum alLowLmtAlm1 = new BLonAlarmTypeEnum(9);
   public static final BLonAlarmTypeEnum alLowLmtAlm2 = new BLonAlarmTypeEnum(10);
   public static final BLonAlarmTypeEnum alHighLmtAlm1 = new BLonAlarmTypeEnum(11);
   public static final BLonAlarmTypeEnum alHighLmtAlm2 = new BLonAlarmTypeEnum(12);
   public static final BLonAlarmTypeEnum alFirAlm = new BLonAlarmTypeEnum(13);
   public static final BLonAlarmTypeEnum alFirPreAlm = new BLonAlarmTypeEnum(14);
   public static final BLonAlarmTypeEnum alFirTrbl = new BLonAlarmTypeEnum(15);
   public static final BLonAlarmTypeEnum alFirSupv = new BLonAlarmTypeEnum(16);
   public static final BLonAlarmTypeEnum alFirTestAlm = new BLonAlarmTypeEnum(17);
   public static final BLonAlarmTypeEnum alFirTestPreAlm = new BLonAlarmTypeEnum(18);
   public static final BLonAlarmTypeEnum alFirEnvcompMax = new BLonAlarmTypeEnum(19);
   public static final BLonAlarmTypeEnum alFirMonitorCond = new BLonAlarmTypeEnum(20);
   public static final BLonAlarmTypeEnum alFirMaintAlert = new BLonAlarmTypeEnum(21);
   public static final BLonAlarmTypeEnum alNul = new BLonAlarmTypeEnum(-1);
   public static final BLonAlarmTypeEnum DEFAULT = alNoCondition;
   public static final Type TYPE = Sys.loadType(BLonAlarmTypeEnum.class);

   public static BLonAlarmTypeEnum make(int ordinal) {
      return (BLonAlarmTypeEnum)alNoCondition.getRange().get(ordinal, false);
   }

   public static BLonAlarmTypeEnum make(String tag) {
      return (BLonAlarmTypeEnum)alNoCondition.getRange().get(tag);
   }

   private BLonAlarmTypeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
