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
      value = "hvoOff",
      ordinal = 0
   ), @Range(
      value = "hvoPosition",
      ordinal = 1
   ), @Range(
      value = "hvoFlowValue",
      ordinal = 2
   ), @Range(
      value = "hvoFlowPercent",
      ordinal = 3
   ), @Range(
      value = "hvoOpen",
      ordinal = 4
   ), @Range(
      value = "hvoClose",
      ordinal = 5
   ), @Range(
      value = "hvoMinimum",
      ordinal = 6
   ), @Range(
      value = "hvoMaximum",
      ordinal = 7
   ), @Range(
      value = "hvoUnused8",
      ordinal = 8
   ), @Range(
      value = "hvoUnused9",
      ordinal = 9
   ), @Range(
      value = "hvoUnused10",
      ordinal = 10
   ), @Range(
      value = "hvoUnused11",
      ordinal = 11
   ), @Range(
      value = "hvoUnused12",
      ordinal = 12
   ), @Range(
      value = "hvoUnused13",
      ordinal = 13
   ), @Range(
      value = "hvoUnused14",
      ordinal = 14
   ), @Range(
      value = "hvoUnused15",
      ordinal = 15
   ), @Range(
      value = "hvoUnused16",
      ordinal = 16
   ), @Range(
      value = "hvoPosition1",
      ordinal = 17
   ), @Range(
      value = "hvoFlowValue1",
      ordinal = 18
   ), @Range(
      value = "hvoFlowPercent1",
      ordinal = 19
   ), @Range(
      value = "hvoOpen1",
      ordinal = 20
   ), @Range(
      value = "hvoClosed1",
      ordinal = 21
   ), @Range(
      value = "hvoMinimum1",
      ordinal = 22
   ), @Range(
      value = "hvoMaximum1",
      ordinal = 23
   ), @Range(
      value = "hvoUnused24",
      ordinal = 24
   ), @Range(
      value = "hvoUnused25",
      ordinal = 25
   ), @Range(
      value = "hvoUnused26",
      ordinal = 26
   ), @Range(
      value = "hvoUnused27",
      ordinal = 27
   ), @Range(
      value = "hvoUnused28",
      ordinal = 28
   ), @Range(
      value = "hvoUnused29",
      ordinal = 29
   ), @Range(
      value = "hvoUnused30",
      ordinal = 30
   ), @Range(
      value = "hvoUnused31",
      ordinal = 31
   ), @Range(
      value = "hvoUnused32",
      ordinal = 32
   ), @Range(
      value = "hvoPosition2",
      ordinal = 33
   ), @Range(
      value = "hvoFlowValue2",
      ordinal = 34
   ), @Range(
      value = "hvoFlowPercent2",
      ordinal = 35
   ), @Range(
      value = "hvoOpen2",
      ordinal = 36
   ), @Range(
      value = "hvoClosed2",
      ordinal = 37
   ), @Range(
      value = "hvoMinimum2",
      ordinal = 38
   ), @Range(
      value = "hvoMaximum2",
      ordinal = 39
   ), @Range(
      value = "hvoUnused40",
      ordinal = 40
   ), @Range(
      value = "hvoUnused41",
      ordinal = 41
   ), @Range(
      value = "hvoUnused42",
      ordinal = 42
   ), @Range(
      value = "hvoUnused43",
      ordinal = 43
   ), @Range(
      value = "hvoUnused44",
      ordinal = 44
   ), @Range(
      value = "hvoUnused45",
      ordinal = 45
   ), @Range(
      value = "hvoUnused46",
      ordinal = 46
   ), @Range(
      value = "hvoUnused47",
      ordinal = 47
   ), @Range(
      value = "hvoUnused48",
      ordinal = 48
   ), @Range(
      value = "hvoNul",
      ordinal = -1
   )},
   defaultValue = "hvoNul"
)
public final class BLonHvacOveridEnum extends BFrozenEnum {
   public static final int HVO_OFF = 0;
   public static final int HVO_POSITION = 1;
   public static final int HVO_FLOW_VALUE = 2;
   public static final int HVO_FLOW_PERCENT = 3;
   public static final int HVO_OPEN = 4;
   public static final int HVO_CLOSE = 5;
   public static final int HVO_MINIMUM = 6;
   public static final int HVO_MAXIMUM = 7;
   public static final int HVO_UNUSED_8 = 8;
   public static final int HVO_UNUSED_9 = 9;
   public static final int HVO_UNUSED_10 = 10;
   public static final int HVO_UNUSED_11 = 11;
   public static final int HVO_UNUSED_12 = 12;
   public static final int HVO_UNUSED_13 = 13;
   public static final int HVO_UNUSED_14 = 14;
   public static final int HVO_UNUSED_15 = 15;
   public static final int HVO_UNUSED_16 = 16;
   public static final int HVO_POSITION_1 = 17;
   public static final int HVO_FLOW_VALUE_1 = 18;
   public static final int HVO_FLOW_PERCENT_1 = 19;
   public static final int HVO_OPEN_1 = 20;
   public static final int HVO_CLOSED_1 = 21;
   public static final int HVO_MINIMUM_1 = 22;
   public static final int HVO_MAXIMUM_1 = 23;
   public static final int HVO_UNUSED_24 = 24;
   public static final int HVO_UNUSED_25 = 25;
   public static final int HVO_UNUSED_26 = 26;
   public static final int HVO_UNUSED_27 = 27;
   public static final int HVO_UNUSED_28 = 28;
   public static final int HVO_UNUSED_29 = 29;
   public static final int HVO_UNUSED_30 = 30;
   public static final int HVO_UNUSED_31 = 31;
   public static final int HVO_UNUSED_32 = 32;
   public static final int HVO_POSITION_2 = 33;
   public static final int HVO_FLOW_VALUE_2 = 34;
   public static final int HVO_FLOW_PERCENT_2 = 35;
   public static final int HVO_OPEN_2 = 36;
   public static final int HVO_CLOSED_2 = 37;
   public static final int HVO_MINIMUM_2 = 38;
   public static final int HVO_MAXIMUM_2 = 39;
   public static final int HVO_UNUSED_40 = 40;
   public static final int HVO_UNUSED_41 = 41;
   public static final int HVO_UNUSED_42 = 42;
   public static final int HVO_UNUSED_43 = 43;
   public static final int HVO_UNUSED_44 = 44;
   public static final int HVO_UNUSED_45 = 45;
   public static final int HVO_UNUSED_46 = 46;
   public static final int HVO_UNUSED_47 = 47;
   public static final int HVO_UNUSED_48 = 48;
   public static final int HVO_NUL = -1;
   public static final BLonHvacOveridEnum hvoOff = new BLonHvacOveridEnum(0);
   public static final BLonHvacOveridEnum hvoPosition = new BLonHvacOveridEnum(1);
   public static final BLonHvacOveridEnum hvoFlowValue = new BLonHvacOveridEnum(2);
   public static final BLonHvacOveridEnum hvoFlowPercent = new BLonHvacOveridEnum(3);
   public static final BLonHvacOveridEnum hvoOpen = new BLonHvacOveridEnum(4);
   public static final BLonHvacOveridEnum hvoClose = new BLonHvacOveridEnum(5);
   public static final BLonHvacOveridEnum hvoMinimum = new BLonHvacOveridEnum(6);
   public static final BLonHvacOveridEnum hvoMaximum = new BLonHvacOveridEnum(7);
   public static final BLonHvacOveridEnum hvoUnused8 = new BLonHvacOveridEnum(8);
   public static final BLonHvacOveridEnum hvoUnused9 = new BLonHvacOveridEnum(9);
   public static final BLonHvacOveridEnum hvoUnused10 = new BLonHvacOveridEnum(10);
   public static final BLonHvacOveridEnum hvoUnused11 = new BLonHvacOveridEnum(11);
   public static final BLonHvacOveridEnum hvoUnused12 = new BLonHvacOveridEnum(12);
   public static final BLonHvacOveridEnum hvoUnused13 = new BLonHvacOveridEnum(13);
   public static final BLonHvacOveridEnum hvoUnused14 = new BLonHvacOveridEnum(14);
   public static final BLonHvacOveridEnum hvoUnused15 = new BLonHvacOveridEnum(15);
   public static final BLonHvacOveridEnum hvoUnused16 = new BLonHvacOveridEnum(16);
   public static final BLonHvacOveridEnum hvoPosition1 = new BLonHvacOveridEnum(17);
   public static final BLonHvacOveridEnum hvoFlowValue1 = new BLonHvacOveridEnum(18);
   public static final BLonHvacOveridEnum hvoFlowPercent1 = new BLonHvacOveridEnum(19);
   public static final BLonHvacOveridEnum hvoOpen1 = new BLonHvacOveridEnum(20);
   public static final BLonHvacOveridEnum hvoClosed1 = new BLonHvacOveridEnum(21);
   public static final BLonHvacOveridEnum hvoMinimum1 = new BLonHvacOveridEnum(22);
   public static final BLonHvacOveridEnum hvoMaximum1 = new BLonHvacOveridEnum(23);
   public static final BLonHvacOveridEnum hvoUnused24 = new BLonHvacOveridEnum(24);
   public static final BLonHvacOveridEnum hvoUnused25 = new BLonHvacOveridEnum(25);
   public static final BLonHvacOveridEnum hvoUnused26 = new BLonHvacOveridEnum(26);
   public static final BLonHvacOveridEnum hvoUnused27 = new BLonHvacOveridEnum(27);
   public static final BLonHvacOveridEnum hvoUnused28 = new BLonHvacOveridEnum(28);
   public static final BLonHvacOveridEnum hvoUnused29 = new BLonHvacOveridEnum(29);
   public static final BLonHvacOveridEnum hvoUnused30 = new BLonHvacOveridEnum(30);
   public static final BLonHvacOveridEnum hvoUnused31 = new BLonHvacOveridEnum(31);
   public static final BLonHvacOveridEnum hvoUnused32 = new BLonHvacOveridEnum(32);
   public static final BLonHvacOveridEnum hvoPosition2 = new BLonHvacOveridEnum(33);
   public static final BLonHvacOveridEnum hvoFlowValue2 = new BLonHvacOveridEnum(34);
   public static final BLonHvacOveridEnum hvoFlowPercent2 = new BLonHvacOveridEnum(35);
   public static final BLonHvacOveridEnum hvoOpen2 = new BLonHvacOveridEnum(36);
   public static final BLonHvacOveridEnum hvoClosed2 = new BLonHvacOveridEnum(37);
   public static final BLonHvacOveridEnum hvoMinimum2 = new BLonHvacOveridEnum(38);
   public static final BLonHvacOveridEnum hvoMaximum2 = new BLonHvacOveridEnum(39);
   public static final BLonHvacOveridEnum hvoUnused40 = new BLonHvacOveridEnum(40);
   public static final BLonHvacOveridEnum hvoUnused41 = new BLonHvacOveridEnum(41);
   public static final BLonHvacOveridEnum hvoUnused42 = new BLonHvacOveridEnum(42);
   public static final BLonHvacOveridEnum hvoUnused43 = new BLonHvacOveridEnum(43);
   public static final BLonHvacOveridEnum hvoUnused44 = new BLonHvacOveridEnum(44);
   public static final BLonHvacOveridEnum hvoUnused45 = new BLonHvacOveridEnum(45);
   public static final BLonHvacOveridEnum hvoUnused46 = new BLonHvacOveridEnum(46);
   public static final BLonHvacOveridEnum hvoUnused47 = new BLonHvacOveridEnum(47);
   public static final BLonHvacOveridEnum hvoUnused48 = new BLonHvacOveridEnum(48);
   public static final BLonHvacOveridEnum hvoNul = new BLonHvacOveridEnum(-1);
   public static final BLonHvacOveridEnum DEFAULT = hvoNul;
   public static final Type TYPE = Sys.loadType(BLonHvacOveridEnum.class);

   public static BLonHvacOveridEnum make(int ordinal) {
      return (BLonHvacOveridEnum)hvoOff.getRange().get(ordinal, false);
   }

   public static BLonHvacOveridEnum make(String tag) {
      return (BLonHvacOveridEnum)hvoOff.getRange().get(tag);
   }

   private BLonHvacOveridEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
