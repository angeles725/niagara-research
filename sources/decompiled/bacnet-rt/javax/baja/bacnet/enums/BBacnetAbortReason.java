package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("other"), @Range("bufferOverflow"), @Range("invalidApduInThisState"), @Range("preemptedByHigherPriorityTask"), @Range("segmentationNotSupported"), @Range("securityError"), @Range("insufficientSecurity"), @Range("windowSizeOutOfRange"), @Range("applicationExceededReplyTime"), @Range("outOfResources"), @Range("tsmTimeout"), @Range("apduTooLong")}
)
public final class BBacnetAbortReason extends BFrozenEnum implements BacnetConst {
   public static final int OTHER = 0;
   public static final int BUFFER_OVERFLOW = 1;
   public static final int INVALID_APDU_IN_THIS_STATE = 2;
   public static final int PREEMPTED_BY_HIGHER_PRIORITY_TASK = 3;
   public static final int SEGMENTATION_NOT_SUPPORTED = 4;
   public static final int SECURITY_ERROR = 5;
   public static final int INSUFFICIENT_SECURITY = 6;
   public static final int WINDOW_SIZE_OUT_OF_RANGE = 7;
   public static final int APPLICATION_EXCEEDED_REPLY_TIME = 8;
   public static final int OUT_OF_RESOURCES = 9;
   public static final int TSM_TIMEOUT = 10;
   public static final int APDU_TOO_LONG = 11;
   public static final BBacnetAbortReason other = new BBacnetAbortReason(0);
   public static final BBacnetAbortReason bufferOverflow = new BBacnetAbortReason(1);
   public static final BBacnetAbortReason invalidApduInThisState = new BBacnetAbortReason(2);
   public static final BBacnetAbortReason preemptedByHigherPriorityTask = new BBacnetAbortReason(3);
   public static final BBacnetAbortReason segmentationNotSupported = new BBacnetAbortReason(4);
   public static final BBacnetAbortReason securityError = new BBacnetAbortReason(5);
   public static final BBacnetAbortReason insufficientSecurity = new BBacnetAbortReason(6);
   public static final BBacnetAbortReason windowSizeOutOfRange = new BBacnetAbortReason(7);
   public static final BBacnetAbortReason applicationExceededReplyTime = new BBacnetAbortReason(8);
   public static final BBacnetAbortReason outOfResources = new BBacnetAbortReason(9);
   public static final BBacnetAbortReason tsmTimeout = new BBacnetAbortReason(10);
   public static final BBacnetAbortReason apduTooLong = new BBacnetAbortReason(11);
   public static final BBacnetAbortReason DEFAULT = other;
   public static final Type TYPE = Sys.loadType(BBacnetAbortReason.class);
   public static final int MAX_ASHRAE_ID = 11;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetAbortReason make(int ordinal) {
      return (BBacnetAbortReason)other.getRange().get(ordinal, false);
   }

   public static BBacnetAbortReason make(String tag) {
      return (BBacnetAbortReason)other.getRange().get(tag);
   }

   private BBacnetAbortReason(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      if (DEFAULT.getRange().isOrdinal(id)) {
         return DEFAULT.getRange().getTag(id);
      } else if (isAshrae(id)) {
         return ASHRAE_PREFIX + id;
      } else if (isProprietary(id)) {
         return PROPRIETARY_PREFIX + id;
      } else {
         throw new InvalidEnumException(id);
      }
   }

   public static int ordinal(String tag) {
      try {
         return DEFAULT.getRange().tagToOrdinal(tag);
      } catch (InvalidEnumException var2) {
         if (tag.startsWith(ASHRAE_PREFIX)) {
            return Integer.parseInt(tag.substring(ASHRAE_PREFIX_LENGTH));
         } else if (tag.startsWith(PROPRIETARY_PREFIX)) {
            return Integer.parseInt(tag.substring(PROPRIETARY_PREFIX_LENGTH));
         } else {
            throw var2;
         }
      }
   }

   public static boolean isProprietary(int id) {
      return id > 63 && id <= 65535;
   }

   public static boolean isAshrae(int id) {
      return id > 11 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 11;
   }
}
