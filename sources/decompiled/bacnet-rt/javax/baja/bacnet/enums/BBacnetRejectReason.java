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
   range = {@Range("other"), @Range("bufferOverflow"), @Range("inconsistentParameters"), @Range("invalidParameterDataType"), @Range("invalidTag"), @Range("missingRequiredParameter"), @Range("parameterOutOfRange"), @Range("tooManyArguments"), @Range("undefinedEnumeration"), @Range("unrecognizedService")}
)
public final class BBacnetRejectReason extends BFrozenEnum implements BacnetConst {
   public static final int OTHER = 0;
   public static final int BUFFER_OVERFLOW = 1;
   public static final int INCONSISTENT_PARAMETERS = 2;
   public static final int INVALID_PARAMETER_DATA_TYPE = 3;
   public static final int INVALID_TAG = 4;
   public static final int MISSING_REQUIRED_PARAMETER = 5;
   public static final int PARAMETER_OUT_OF_RANGE = 6;
   public static final int TOO_MANY_ARGUMENTS = 7;
   public static final int UNDEFINED_ENUMERATION = 8;
   public static final int UNRECOGNIZED_SERVICE = 9;
   public static final BBacnetRejectReason other = new BBacnetRejectReason(0);
   public static final BBacnetRejectReason bufferOverflow = new BBacnetRejectReason(1);
   public static final BBacnetRejectReason inconsistentParameters = new BBacnetRejectReason(2);
   public static final BBacnetRejectReason invalidParameterDataType = new BBacnetRejectReason(3);
   public static final BBacnetRejectReason invalidTag = new BBacnetRejectReason(4);
   public static final BBacnetRejectReason missingRequiredParameter = new BBacnetRejectReason(5);
   public static final BBacnetRejectReason parameterOutOfRange = new BBacnetRejectReason(6);
   public static final BBacnetRejectReason tooManyArguments = new BBacnetRejectReason(7);
   public static final BBacnetRejectReason undefinedEnumeration = new BBacnetRejectReason(8);
   public static final BBacnetRejectReason unrecognizedService = new BBacnetRejectReason(9);
   public static final BBacnetRejectReason DEFAULT = other;
   public static final Type TYPE = Sys.loadType(BBacnetRejectReason.class);
   public static final int MAX_ASHRAE_ID = 9;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetRejectReason make(int ordinal) {
      return (BBacnetRejectReason)other.getRange().get(ordinal, false);
   }

   public static BBacnetRejectReason make(String tag) {
      return (BBacnetRejectReason)other.getRange().get(tag);
   }

   private BBacnetRejectReason(int ordinal) {
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
      return id > 9 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 9;
   }
}
