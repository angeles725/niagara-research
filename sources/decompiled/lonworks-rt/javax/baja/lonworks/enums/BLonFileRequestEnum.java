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
      value = "openToSend",
      ordinal = 0
   ), @Range(
      value = "openToReceive",
      ordinal = 1
   ), @Range(
      value = "closeFile",
      ordinal = 2
   ), @Range(
      value = "closeDeleteFile",
      ordinal = 3
   ), @Range(
      value = "directoryLookup",
      ordinal = 4
   ), @Range(
      value = "openToSendRa",
      ordinal = 5
   ), @Range(
      value = "openToReceiveRa",
      ordinal = 6
   ), @Range(
      value = "nul",
      ordinal = -1
   )},
   defaultValue = "nul"
)
public final class BLonFileRequestEnum extends BFrozenEnum {
   public static final int OPEN_TO_SEND = 0;
   public static final int OPEN_TO_RECEIVE = 1;
   public static final int CLOSE_FILE = 2;
   public static final int CLOSE_DELETE_FILE = 3;
   public static final int DIRECTORY_LOOKUP = 4;
   public static final int OPEN_TO_SEND_RA = 5;
   public static final int OPEN_TO_RECEIVE_RA = 6;
   public static final int NUL = -1;
   public static final BLonFileRequestEnum openToSend = new BLonFileRequestEnum(0);
   public static final BLonFileRequestEnum openToReceive = new BLonFileRequestEnum(1);
   public static final BLonFileRequestEnum closeFile = new BLonFileRequestEnum(2);
   public static final BLonFileRequestEnum closeDeleteFile = new BLonFileRequestEnum(3);
   public static final BLonFileRequestEnum directoryLookup = new BLonFileRequestEnum(4);
   public static final BLonFileRequestEnum openToSendRa = new BLonFileRequestEnum(5);
   public static final BLonFileRequestEnum openToReceiveRa = new BLonFileRequestEnum(6);
   public static final BLonFileRequestEnum nul = new BLonFileRequestEnum(-1);
   public static final BLonFileRequestEnum DEFAULT = nul;
   public static final Type TYPE = Sys.loadType(BLonFileRequestEnum.class);

   public static BLonFileRequestEnum make(int ordinal) {
      return (BLonFileRequestEnum)openToSend.getRange().get(ordinal, false);
   }

   public static BLonFileRequestEnum make(String tag) {
      return (BLonFileRequestEnum)openToSend.getRange().get(tag);
   }

   private BLonFileRequestEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
