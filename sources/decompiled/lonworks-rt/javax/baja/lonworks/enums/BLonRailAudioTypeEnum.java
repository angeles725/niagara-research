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
      value = "ratIcReq",
      ordinal = 0
   ), @Range(
      value = "ratIcJoin",
      ordinal = 1
   ), @Range(
      value = "ratIcQuit",
      ordinal = 2
   ), @Range(
      value = "ratIcEnd",
      ordinal = 3
   ), @Range(
      value = "ratHwRadioReq",
      ordinal = 4
   ), @Range(
      value = "ratHwRadioEnd",
      ordinal = 5
   ), @Range(
      value = "ratHwPaReq",
      ordinal = 6
   ), @Range(
      value = "ratHwPaEnd",
      ordinal = 7
   ), @Range(
      value = "ratSwPaReq",
      ordinal = 8
   ), @Range(
      value = "ratSwPaEnd",
      ordinal = 9
   ), @Range(
      value = "ratSwPaOrReq",
      ordinal = 10
   ), @Range(
      value = "ratSwPaOrEnd",
      ordinal = 11
   ), @Range(
      value = "ratPauReq",
      ordinal = 12
   ), @Range(
      value = "ratPauAccept",
      ordinal = 13
   ), @Range(
      value = "ratPauCall",
      ordinal = 14
   ), @Range(
      value = "ratPauEnd",
      ordinal = 15
   ), @Range(
      value = "ratEntertReq",
      ordinal = 16
   ), @Range(
      value = "ratEntertEnd",
      ordinal = 17
   ), @Range(
      value = "ratNul",
      ordinal = -1
   )},
   defaultValue = "ratNul"
)
public final class BLonRailAudioTypeEnum extends BFrozenEnum {
   public static final int RAT_IC_REQ = 0;
   public static final int RAT_IC_JOIN = 1;
   public static final int RAT_IC_QUIT = 2;
   public static final int RAT_IC_END = 3;
   public static final int RAT_HW_RADIO_REQ = 4;
   public static final int RAT_HW_RADIO_END = 5;
   public static final int RAT_HW_PA_REQ = 6;
   public static final int RAT_HW_PA_END = 7;
   public static final int RAT_SW_PA_REQ = 8;
   public static final int RAT_SW_PA_END = 9;
   public static final int RAT_SW_PA_OR_REQ = 10;
   public static final int RAT_SW_PA_OR_END = 11;
   public static final int RAT_PAU_REQ = 12;
   public static final int RAT_PAU_ACCEPT = 13;
   public static final int RAT_PAU_CALL = 14;
   public static final int RAT_PAU_END = 15;
   public static final int RAT_ENTERT_REQ = 16;
   public static final int RAT_ENTERT_END = 17;
   public static final int RAT_NUL = -1;
   public static final BLonRailAudioTypeEnum ratIcReq = new BLonRailAudioTypeEnum(0);
   public static final BLonRailAudioTypeEnum ratIcJoin = new BLonRailAudioTypeEnum(1);
   public static final BLonRailAudioTypeEnum ratIcQuit = new BLonRailAudioTypeEnum(2);
   public static final BLonRailAudioTypeEnum ratIcEnd = new BLonRailAudioTypeEnum(3);
   public static final BLonRailAudioTypeEnum ratHwRadioReq = new BLonRailAudioTypeEnum(4);
   public static final BLonRailAudioTypeEnum ratHwRadioEnd = new BLonRailAudioTypeEnum(5);
   public static final BLonRailAudioTypeEnum ratHwPaReq = new BLonRailAudioTypeEnum(6);
   public static final BLonRailAudioTypeEnum ratHwPaEnd = new BLonRailAudioTypeEnum(7);
   public static final BLonRailAudioTypeEnum ratSwPaReq = new BLonRailAudioTypeEnum(8);
   public static final BLonRailAudioTypeEnum ratSwPaEnd = new BLonRailAudioTypeEnum(9);
   public static final BLonRailAudioTypeEnum ratSwPaOrReq = new BLonRailAudioTypeEnum(10);
   public static final BLonRailAudioTypeEnum ratSwPaOrEnd = new BLonRailAudioTypeEnum(11);
   public static final BLonRailAudioTypeEnum ratPauReq = new BLonRailAudioTypeEnum(12);
   public static final BLonRailAudioTypeEnum ratPauAccept = new BLonRailAudioTypeEnum(13);
   public static final BLonRailAudioTypeEnum ratPauCall = new BLonRailAudioTypeEnum(14);
   public static final BLonRailAudioTypeEnum ratPauEnd = new BLonRailAudioTypeEnum(15);
   public static final BLonRailAudioTypeEnum ratEntertReq = new BLonRailAudioTypeEnum(16);
   public static final BLonRailAudioTypeEnum ratEntertEnd = new BLonRailAudioTypeEnum(17);
   public static final BLonRailAudioTypeEnum ratNul = new BLonRailAudioTypeEnum(-1);
   public static final BLonRailAudioTypeEnum DEFAULT = ratNul;
   public static final Type TYPE = Sys.loadType(BLonRailAudioTypeEnum.class);

   public static BLonRailAudioTypeEnum make(int ordinal) {
      return (BLonRailAudioTypeEnum)ratIcReq.getRange().get(ordinal, false);
   }

   public static BLonRailAudioTypeEnum make(String tag) {
      return (BLonRailAudioTypeEnum)ratIcReq.getRange().get(tag);
   }

   private BLonRailAudioTypeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
