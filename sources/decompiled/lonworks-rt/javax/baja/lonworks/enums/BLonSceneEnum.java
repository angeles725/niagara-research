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
      value = "scRecall",
      ordinal = 0
   ), @Range(
      value = "scLearn",
      ordinal = 1
   ), @Range(
      value = "scDisplay",
      ordinal = 2
   ), @Range(
      value = "scGroupOff",
      ordinal = 3
   ), @Range(
      value = "scGroupOn",
      ordinal = 4
   ), @Range(
      value = "scStatusOff",
      ordinal = 5
   ), @Range(
      value = "scStatusOn",
      ordinal = 6
   ), @Range(
      value = "scStatusMixed",
      ordinal = 7
   ), @Range(
      value = "scGroupStatus",
      ordinal = 8
   ), @Range(
      value = "scFlick",
      ordinal = 9
   ), @Range(
      value = "scTimeout",
      ordinal = 10
   ), @Range(
      value = "scTimeoutFlick",
      ordinal = 11
   ), @Range(
      value = "scDelayoff",
      ordinal = 12
   ), @Range(
      value = "scDelayoffFlick",
      ordinal = 13
   ), @Range(
      value = "scDelayon",
      ordinal = 14
   ), @Range(
      value = "scEnableGroup",
      ordinal = 15
   ), @Range(
      value = "scDisableGroup",
      ordinal = 16
   ), @Range(
      value = "scCleanon",
      ordinal = 17
   ), @Range(
      value = "scCleanoff",
      ordinal = 18
   ), @Range(
      value = "scWink",
      ordinal = 19
   ), @Range(
      value = "scReset",
      ordinal = 20
   ), @Range(
      value = "scMode1",
      ordinal = 21
   ), @Range(
      value = "scMode2",
      ordinal = 22
   ), @Range(
      value = "scMode3",
      ordinal = 23
   ), @Range(
      value = "scNul",
      ordinal = -1
   )}
)
public final class BLonSceneEnum extends BFrozenEnum {
   public static final int SC_RECALL = 0;
   public static final int SC_LEARN = 1;
   public static final int SC_DISPLAY = 2;
   public static final int SC_GROUP_OFF = 3;
   public static final int SC_GROUP_ON = 4;
   public static final int SC_STATUS_OFF = 5;
   public static final int SC_STATUS_ON = 6;
   public static final int SC_STATUS_MIXED = 7;
   public static final int SC_GROUP_STATUS = 8;
   public static final int SC_FLICK = 9;
   public static final int SC_TIMEOUT = 10;
   public static final int SC_TIMEOUT_FLICK = 11;
   public static final int SC_DELAYOFF = 12;
   public static final int SC_DELAYOFF_FLICK = 13;
   public static final int SC_DELAYON = 14;
   public static final int SC_ENABLE_GROUP = 15;
   public static final int SC_DISABLE_GROUP = 16;
   public static final int SC_CLEANON = 17;
   public static final int SC_CLEANOFF = 18;
   public static final int SC_WINK = 19;
   public static final int SC_RESET = 20;
   public static final int SC_MODE_1 = 21;
   public static final int SC_MODE_2 = 22;
   public static final int SC_MODE_3 = 23;
   public static final int SC_NUL = -1;
   public static final BLonSceneEnum scRecall = new BLonSceneEnum(0);
   public static final BLonSceneEnum scLearn = new BLonSceneEnum(1);
   public static final BLonSceneEnum scDisplay = new BLonSceneEnum(2);
   public static final BLonSceneEnum scGroupOff = new BLonSceneEnum(3);
   public static final BLonSceneEnum scGroupOn = new BLonSceneEnum(4);
   public static final BLonSceneEnum scStatusOff = new BLonSceneEnum(5);
   public static final BLonSceneEnum scStatusOn = new BLonSceneEnum(6);
   public static final BLonSceneEnum scStatusMixed = new BLonSceneEnum(7);
   public static final BLonSceneEnum scGroupStatus = new BLonSceneEnum(8);
   public static final BLonSceneEnum scFlick = new BLonSceneEnum(9);
   public static final BLonSceneEnum scTimeout = new BLonSceneEnum(10);
   public static final BLonSceneEnum scTimeoutFlick = new BLonSceneEnum(11);
   public static final BLonSceneEnum scDelayoff = new BLonSceneEnum(12);
   public static final BLonSceneEnum scDelayoffFlick = new BLonSceneEnum(13);
   public static final BLonSceneEnum scDelayon = new BLonSceneEnum(14);
   public static final BLonSceneEnum scEnableGroup = new BLonSceneEnum(15);
   public static final BLonSceneEnum scDisableGroup = new BLonSceneEnum(16);
   public static final BLonSceneEnum scCleanon = new BLonSceneEnum(17);
   public static final BLonSceneEnum scCleanoff = new BLonSceneEnum(18);
   public static final BLonSceneEnum scWink = new BLonSceneEnum(19);
   public static final BLonSceneEnum scReset = new BLonSceneEnum(20);
   public static final BLonSceneEnum scMode1 = new BLonSceneEnum(21);
   public static final BLonSceneEnum scMode2 = new BLonSceneEnum(22);
   public static final BLonSceneEnum scMode3 = new BLonSceneEnum(23);
   public static final BLonSceneEnum scNul = new BLonSceneEnum(-1);
   public static final BLonSceneEnum DEFAULT = scRecall;
   public static final Type TYPE = Sys.loadType(BLonSceneEnum.class);

   public static BLonSceneEnum make(int ordinal) {
      return (BLonSceneEnum)scRecall.getRange().get(ordinal, false);
   }

   public static BLonSceneEnum make(String tag) {
      return (BLonSceneEnum)scRecall.getRange().get(tag);
   }

   private BLonSceneEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
