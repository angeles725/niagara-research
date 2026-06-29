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
      value = "telNotinuse",
      ordinal = 0
   ), @Range(
      value = "telOffhook",
      ordinal = 1
   ), @Range(
      value = "telDialing",
      ordinal = 2
   ), @Range(
      value = "telDialcomp",
      ordinal = 3
   ), @Range(
      value = "telRingback",
      ordinal = 4
   ), @Range(
      value = "telIncoming",
      ordinal = 5
   ), @Range(
      value = "telRinging",
      ordinal = 6
   ), @Range(
      value = "telAnswered",
      ordinal = 7
   ), @Range(
      value = "telTalking",
      ordinal = 8
   ), @Range(
      value = "telHangingup",
      ordinal = 9
   ), @Range(
      value = "telHungupx",
      ordinal = 10
   ), @Range(
      value = "telHold",
      ordinal = 11
   ), @Range(
      value = "telUnhold",
      ordinal = 12
   ), @Range(
      value = "telRelease",
      ordinal = 13
   ), @Range(
      value = "telFulldup",
      ordinal = 14
   ), @Range(
      value = "telBlocked",
      ordinal = 15
   ), @Range(
      value = "telCwait",
      ordinal = 16
   ), @Range(
      value = "telDestbusy",
      ordinal = 17
   ), @Range(
      value = "telNetbusy",
      ordinal = 18
   ), @Range(
      value = "telError",
      ordinal = 19
   ), @Range(
      value = "telNul",
      ordinal = -1
   )}
)
public final class BLonTelcomStatesEnum extends BFrozenEnum {
   public static final int TEL_NOTINUSE = 0;
   public static final int TEL_OFFHOOK = 1;
   public static final int TEL_DIALING = 2;
   public static final int TEL_DIALCOMP = 3;
   public static final int TEL_RINGBACK = 4;
   public static final int TEL_INCOMING = 5;
   public static final int TEL_RINGING = 6;
   public static final int TEL_ANSWERED = 7;
   public static final int TEL_TALKING = 8;
   public static final int TEL_HANGINGUP = 9;
   public static final int TEL_HUNGUPX = 10;
   public static final int TEL_HOLD = 11;
   public static final int TEL_UNHOLD = 12;
   public static final int TEL_RELEASE = 13;
   public static final int TEL_FULLDUP = 14;
   public static final int TEL_BLOCKED = 15;
   public static final int TEL_CWAIT = 16;
   public static final int TEL_DESTBUSY = 17;
   public static final int TEL_NETBUSY = 18;
   public static final int TEL_ERROR = 19;
   public static final int TEL_NUL = -1;
   public static final BLonTelcomStatesEnum telNotinuse = new BLonTelcomStatesEnum(0);
   public static final BLonTelcomStatesEnum telOffhook = new BLonTelcomStatesEnum(1);
   public static final BLonTelcomStatesEnum telDialing = new BLonTelcomStatesEnum(2);
   public static final BLonTelcomStatesEnum telDialcomp = new BLonTelcomStatesEnum(3);
   public static final BLonTelcomStatesEnum telRingback = new BLonTelcomStatesEnum(4);
   public static final BLonTelcomStatesEnum telIncoming = new BLonTelcomStatesEnum(5);
   public static final BLonTelcomStatesEnum telRinging = new BLonTelcomStatesEnum(6);
   public static final BLonTelcomStatesEnum telAnswered = new BLonTelcomStatesEnum(7);
   public static final BLonTelcomStatesEnum telTalking = new BLonTelcomStatesEnum(8);
   public static final BLonTelcomStatesEnum telHangingup = new BLonTelcomStatesEnum(9);
   public static final BLonTelcomStatesEnum telHungupx = new BLonTelcomStatesEnum(10);
   public static final BLonTelcomStatesEnum telHold = new BLonTelcomStatesEnum(11);
   public static final BLonTelcomStatesEnum telUnhold = new BLonTelcomStatesEnum(12);
   public static final BLonTelcomStatesEnum telRelease = new BLonTelcomStatesEnum(13);
   public static final BLonTelcomStatesEnum telFulldup = new BLonTelcomStatesEnum(14);
   public static final BLonTelcomStatesEnum telBlocked = new BLonTelcomStatesEnum(15);
   public static final BLonTelcomStatesEnum telCwait = new BLonTelcomStatesEnum(16);
   public static final BLonTelcomStatesEnum telDestbusy = new BLonTelcomStatesEnum(17);
   public static final BLonTelcomStatesEnum telNetbusy = new BLonTelcomStatesEnum(18);
   public static final BLonTelcomStatesEnum telError = new BLonTelcomStatesEnum(19);
   public static final BLonTelcomStatesEnum telNul = new BLonTelcomStatesEnum(-1);
   public static final BLonTelcomStatesEnum DEFAULT = telNotinuse;
   public static final Type TYPE = Sys.loadType(BLonTelcomStatesEnum.class);

   public static BLonTelcomStatesEnum make(int ordinal) {
      return (BLonTelcomStatesEnum)telNotinuse.getRange().get(ordinal, false);
   }

   public static BLonTelcomStatesEnum make(String tag) {
      return (BLonTelcomStatesEnum)telNotinuse.getRange().get(tag);
   }

   private BLonTelcomStatesEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
