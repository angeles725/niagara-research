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
      value = "fsXferOk",
      ordinal = 0
   ), @Range(
      value = "fsLookupOk",
      ordinal = 1
   ), @Range(
      value = "fsOpenFail",
      ordinal = 2
   ), @Range(
      value = "fsLookupErr",
      ordinal = 3
   ), @Range(
      value = "fsXferUnderway",
      ordinal = 4
   ), @Range(
      value = "fsIoErr",
      ordinal = 5
   ), @Range(
      value = "fsTimeoutErr",
      ordinal = 6
   ), @Range(
      value = "fsWindowErr",
      ordinal = 7
   ), @Range(
      value = "fsAuthErr",
      ordinal = 8
   ), @Range(
      value = "fsAccessUnavail",
      ordinal = 9
   ), @Range(
      value = "fsSeekInvalid",
      ordinal = 10
   ), @Range(
      value = "fsSeekWake",
      ordinal = 11
   ), @Range(
      value = "fsNul",
      ordinal = -1
   )},
   defaultValue = "fsNul"
)
public final class BLonFileStatusEnum extends BFrozenEnum {
   public static final int FS_XFER_OK = 0;
   public static final int FS_LOOKUP_OK = 1;
   public static final int FS_OPEN_FAIL = 2;
   public static final int FS_LOOKUP_ERR = 3;
   public static final int FS_XFER_UNDERWAY = 4;
   public static final int FS_IO_ERR = 5;
   public static final int FS_TIMEOUT_ERR = 6;
   public static final int FS_WINDOW_ERR = 7;
   public static final int FS_AUTH_ERR = 8;
   public static final int FS_ACCESS_UNAVAIL = 9;
   public static final int FS_SEEK_INVALID = 10;
   public static final int FS_SEEK_WAKE = 11;
   public static final int FS_NUL = -1;
   public static final BLonFileStatusEnum fsXferOk = new BLonFileStatusEnum(0);
   public static final BLonFileStatusEnum fsLookupOk = new BLonFileStatusEnum(1);
   public static final BLonFileStatusEnum fsOpenFail = new BLonFileStatusEnum(2);
   public static final BLonFileStatusEnum fsLookupErr = new BLonFileStatusEnum(3);
   public static final BLonFileStatusEnum fsXferUnderway = new BLonFileStatusEnum(4);
   public static final BLonFileStatusEnum fsIoErr = new BLonFileStatusEnum(5);
   public static final BLonFileStatusEnum fsTimeoutErr = new BLonFileStatusEnum(6);
   public static final BLonFileStatusEnum fsWindowErr = new BLonFileStatusEnum(7);
   public static final BLonFileStatusEnum fsAuthErr = new BLonFileStatusEnum(8);
   public static final BLonFileStatusEnum fsAccessUnavail = new BLonFileStatusEnum(9);
   public static final BLonFileStatusEnum fsSeekInvalid = new BLonFileStatusEnum(10);
   public static final BLonFileStatusEnum fsSeekWake = new BLonFileStatusEnum(11);
   public static final BLonFileStatusEnum fsNul = new BLonFileStatusEnum(-1);
   public static final BLonFileStatusEnum DEFAULT = fsNul;
   public static final Type TYPE = Sys.loadType(BLonFileStatusEnum.class);

   public static BLonFileStatusEnum make(int ordinal) {
      return (BLonFileStatusEnum)fsXferOk.getRange().get(ordinal, false);
   }

   public static BLonFileStatusEnum make(String tag) {
      return (BLonFileStatusEnum)fsXferOk.getRange().get(tag);
   }

   private BLonFileStatusEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
