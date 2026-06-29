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
      value = "rastCuType1",
      ordinal = 0
   ), @Range(
      value = "rastCuType2",
      ordinal = 1
   ), @Range(
      value = "rastCuType3",
      ordinal = 2
   ), @Range(
      value = "rastCuType4",
      ordinal = 3
   ), @Range(
      value = "rastLsLine1",
      ordinal = 4
   ), @Range(
      value = "rastLsLine2",
      ordinal = 5
   ), @Range(
      value = "rastLsLine3",
      ordinal = 6
   ), @Range(
      value = "rastLsLine4",
      ordinal = 7
   ), @Range(
      value = "rastLsLine5",
      ordinal = 8
   ), @Range(
      value = "rastLsLine6",
      ordinal = 9
   ), @Range(
      value = "rastLsLine7",
      ordinal = 10
   ), @Range(
      value = "rastLsLine8",
      ordinal = 11
   ), @Range(
      value = "rastPau",
      ordinal = 12
   ), @Range(
      value = "rastCfaType1",
      ordinal = 13
   ), @Range(
      value = "rastCfaType2",
      ordinal = 14
   ), @Range(
      value = "rastCfaType3",
      ordinal = 15
   ), @Range(
      value = "rastCfaType4",
      ordinal = 16
   ), @Range(
      value = "rastDva",
      ordinal = 17
   ), @Range(
      value = "rastEtType1",
      ordinal = 18
   ), @Range(
      value = "rastEtType2",
      ordinal = 19
   ), @Range(
      value = "rastUserdefType1",
      ordinal = 20
   ), @Range(
      value = "rastUserdefType2",
      ordinal = 21
   ), @Range(
      value = "rastUserdefType3",
      ordinal = 22
   ), @Range(
      value = "rastUserdefType4",
      ordinal = 23
   ), @Range(
      value = "rastNul",
      ordinal = -1
   )},
   defaultValue = "rastNul"
)
public final class BLonRailAudioSensorTypeEnum extends BFrozenEnum {
   public static final int RAST_CU_TYPE_1 = 0;
   public static final int RAST_CU_TYPE_2 = 1;
   public static final int RAST_CU_TYPE_3 = 2;
   public static final int RAST_CU_TYPE_4 = 3;
   public static final int RAST_LS_LINE_1 = 4;
   public static final int RAST_LS_LINE_2 = 5;
   public static final int RAST_LS_LINE_3 = 6;
   public static final int RAST_LS_LINE_4 = 7;
   public static final int RAST_LS_LINE_5 = 8;
   public static final int RAST_LS_LINE_6 = 9;
   public static final int RAST_LS_LINE_7 = 10;
   public static final int RAST_LS_LINE_8 = 11;
   public static final int RAST_PAU = 12;
   public static final int RAST_CFA_TYPE_1 = 13;
   public static final int RAST_CFA_TYPE_2 = 14;
   public static final int RAST_CFA_TYPE_3 = 15;
   public static final int RAST_CFA_TYPE_4 = 16;
   public static final int RAST_DVA = 17;
   public static final int RAST_ET_TYPE_1 = 18;
   public static final int RAST_ET_TYPE_2 = 19;
   public static final int RAST_USERDEF_TYPE_1 = 20;
   public static final int RAST_USERDEF_TYPE_2 = 21;
   public static final int RAST_USERDEF_TYPE_3 = 22;
   public static final int RAST_USERDEF_TYPE_4 = 23;
   public static final int RAST_NUL = -1;
   public static final BLonRailAudioSensorTypeEnum rastCuType1 = new BLonRailAudioSensorTypeEnum(0);
   public static final BLonRailAudioSensorTypeEnum rastCuType2 = new BLonRailAudioSensorTypeEnum(1);
   public static final BLonRailAudioSensorTypeEnum rastCuType3 = new BLonRailAudioSensorTypeEnum(2);
   public static final BLonRailAudioSensorTypeEnum rastCuType4 = new BLonRailAudioSensorTypeEnum(3);
   public static final BLonRailAudioSensorTypeEnum rastLsLine1 = new BLonRailAudioSensorTypeEnum(4);
   public static final BLonRailAudioSensorTypeEnum rastLsLine2 = new BLonRailAudioSensorTypeEnum(5);
   public static final BLonRailAudioSensorTypeEnum rastLsLine3 = new BLonRailAudioSensorTypeEnum(6);
   public static final BLonRailAudioSensorTypeEnum rastLsLine4 = new BLonRailAudioSensorTypeEnum(7);
   public static final BLonRailAudioSensorTypeEnum rastLsLine5 = new BLonRailAudioSensorTypeEnum(8);
   public static final BLonRailAudioSensorTypeEnum rastLsLine6 = new BLonRailAudioSensorTypeEnum(9);
   public static final BLonRailAudioSensorTypeEnum rastLsLine7 = new BLonRailAudioSensorTypeEnum(10);
   public static final BLonRailAudioSensorTypeEnum rastLsLine8 = new BLonRailAudioSensorTypeEnum(11);
   public static final BLonRailAudioSensorTypeEnum rastPau = new BLonRailAudioSensorTypeEnum(12);
   public static final BLonRailAudioSensorTypeEnum rastCfaType1 = new BLonRailAudioSensorTypeEnum(13);
   public static final BLonRailAudioSensorTypeEnum rastCfaType2 = new BLonRailAudioSensorTypeEnum(14);
   public static final BLonRailAudioSensorTypeEnum rastCfaType3 = new BLonRailAudioSensorTypeEnum(15);
   public static final BLonRailAudioSensorTypeEnum rastCfaType4 = new BLonRailAudioSensorTypeEnum(16);
   public static final BLonRailAudioSensorTypeEnum rastDva = new BLonRailAudioSensorTypeEnum(17);
   public static final BLonRailAudioSensorTypeEnum rastEtType1 = new BLonRailAudioSensorTypeEnum(18);
   public static final BLonRailAudioSensorTypeEnum rastEtType2 = new BLonRailAudioSensorTypeEnum(19);
   public static final BLonRailAudioSensorTypeEnum rastUserdefType1 = new BLonRailAudioSensorTypeEnum(20);
   public static final BLonRailAudioSensorTypeEnum rastUserdefType2 = new BLonRailAudioSensorTypeEnum(21);
   public static final BLonRailAudioSensorTypeEnum rastUserdefType3 = new BLonRailAudioSensorTypeEnum(22);
   public static final BLonRailAudioSensorTypeEnum rastUserdefType4 = new BLonRailAudioSensorTypeEnum(23);
   public static final BLonRailAudioSensorTypeEnum rastNul = new BLonRailAudioSensorTypeEnum(-1);
   public static final BLonRailAudioSensorTypeEnum DEFAULT = rastNul;
   public static final Type TYPE = Sys.loadType(BLonRailAudioSensorTypeEnum.class);

   public static BLonRailAudioSensorTypeEnum make(int ordinal) {
      return (BLonRailAudioSensorTypeEnum)rastCuType1.getRange().get(ordinal, false);
   }

   public static BLonRailAudioSensorTypeEnum make(String tag) {
      return (BLonRailAudioSensorTypeEnum)rastCuType1.getRange().get(tag);
   }

   private BLonRailAudioSensorTypeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
