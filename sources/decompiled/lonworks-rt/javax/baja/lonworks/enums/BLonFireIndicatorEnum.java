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
      value = "fnUndefined",
      ordinal = 0
   ), @Range(
      value = "fnStrobeU",
      ordinal = 1
   ), @Range(
      value = "fnStrobeS",
      ordinal = 2
   ), @Range(
      value = "fnHorn",
      ordinal = 3
   ), @Range(
      value = "fnChime",
      ordinal = 4
   ), @Range(
      value = "fnBell",
      ordinal = 5
   ), @Range(
      value = "fnSounder",
      ordinal = 6
   ), @Range(
      value = "fnSpeaker",
      ordinal = 7
   ), @Range(
      value = "fnUniversal",
      ordinal = 8
   ), @Range(
      value = "fnNul",
      ordinal = -1
   )}
)
public final class BLonFireIndicatorEnum extends BFrozenEnum {
   public static final int FN_UNDEFINED = 0;
   public static final int FN_STROBE_U = 1;
   public static final int FN_STROBE_S = 2;
   public static final int FN_HORN = 3;
   public static final int FN_CHIME = 4;
   public static final int FN_BELL = 5;
   public static final int FN_SOUNDER = 6;
   public static final int FN_SPEAKER = 7;
   public static final int FN_UNIVERSAL = 8;
   public static final int FN_NUL = -1;
   public static final BLonFireIndicatorEnum fnUndefined = new BLonFireIndicatorEnum(0);
   public static final BLonFireIndicatorEnum fnStrobeU = new BLonFireIndicatorEnum(1);
   public static final BLonFireIndicatorEnum fnStrobeS = new BLonFireIndicatorEnum(2);
   public static final BLonFireIndicatorEnum fnHorn = new BLonFireIndicatorEnum(3);
   public static final BLonFireIndicatorEnum fnChime = new BLonFireIndicatorEnum(4);
   public static final BLonFireIndicatorEnum fnBell = new BLonFireIndicatorEnum(5);
   public static final BLonFireIndicatorEnum fnSounder = new BLonFireIndicatorEnum(6);
   public static final BLonFireIndicatorEnum fnSpeaker = new BLonFireIndicatorEnum(7);
   public static final BLonFireIndicatorEnum fnUniversal = new BLonFireIndicatorEnum(8);
   public static final BLonFireIndicatorEnum fnNul = new BLonFireIndicatorEnum(-1);
   public static final BLonFireIndicatorEnum DEFAULT = fnUndefined;
   public static final Type TYPE = Sys.loadType(BLonFireIndicatorEnum.class);

   public static BLonFireIndicatorEnum make(int ordinal) {
      return (BLonFireIndicatorEnum)fnUndefined.getRange().get(ordinal, false);
   }

   public static BLonFireIndicatorEnum make(String tag) {
      return (BLonFireIndicatorEnum)fnUndefined.getRange().get(tag);
   }

   private BLonFireIndicatorEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
