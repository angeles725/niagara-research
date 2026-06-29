package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Context;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("changeOfBitstring"), @Range("changeOfState"), @Range("changeOfValue"), @Range("commandFailure"), @Range("floatingLimit"), @Range("outOfRange"), @Range("complexEventType"), @Range("bufferReadyDeprecated"), @Range("changeOfLifeSafety"), @Range("extended"), @Range("bufferReady"), @Range("unsignedRange"), @Range("reserved"), @Range("accessEvent"), @Range("doubleOutOfRange"), @Range("signedOutOfRange"), @Range("unsignedOutOfRange"), @Range("changeOfCharacterstring"), @Range("changeOfStatusFlags"), @Range("changeOfReliability"), @Range("none"), @Range("changeOfDiscreteValue")}
)
public final class BBacnetEventType extends BFrozenEnum implements BacnetConst {
   public static final int CHANGE_OF_BITSTRING = 0;
   public static final int CHANGE_OF_STATE = 1;
   public static final int CHANGE_OF_VALUE = 2;
   public static final int COMMAND_FAILURE = 3;
   public static final int FLOATING_LIMIT = 4;
   public static final int OUT_OF_RANGE = 5;
   public static final int COMPLEX_EVENT_TYPE = 6;
   public static final int BUFFER_READY_DEPRECATED = 7;
   public static final int CHANGE_OF_LIFE_SAFETY = 8;
   public static final int EXTENDED = 9;
   public static final int BUFFER_READY = 10;
   public static final int UNSIGNED_RANGE = 11;
   public static final int RESERVED = 12;
   public static final int ACCESS_EVENT = 13;
   public static final int DOUBLE_OUT_OF_RANGE = 14;
   public static final int SIGNED_OUT_OF_RANGE = 15;
   public static final int UNSIGNED_OUT_OF_RANGE = 16;
   public static final int CHANGE_OF_CHARACTERSTRING = 17;
   public static final int CHANGE_OF_STATUS_FLAGS = 18;
   public static final int CHANGE_OF_RELIABILITY = 19;
   public static final int NONE = 20;
   public static final int CHANGE_OF_DISCRETE_VALUE = 21;
   public static final BBacnetEventType changeOfBitstring = new BBacnetEventType(0);
   public static final BBacnetEventType changeOfState = new BBacnetEventType(1);
   public static final BBacnetEventType changeOfValue = new BBacnetEventType(2);
   public static final BBacnetEventType commandFailure = new BBacnetEventType(3);
   public static final BBacnetEventType floatingLimit = new BBacnetEventType(4);
   public static final BBacnetEventType outOfRange = new BBacnetEventType(5);
   public static final BBacnetEventType complexEventType = new BBacnetEventType(6);
   public static final BBacnetEventType bufferReadyDeprecated = new BBacnetEventType(7);
   public static final BBacnetEventType changeOfLifeSafety = new BBacnetEventType(8);
   public static final BBacnetEventType extended = new BBacnetEventType(9);
   public static final BBacnetEventType bufferReady = new BBacnetEventType(10);
   public static final BBacnetEventType unsignedRange = new BBacnetEventType(11);
   public static final BBacnetEventType reserved = new BBacnetEventType(12);
   public static final BBacnetEventType accessEvent = new BBacnetEventType(13);
   public static final BBacnetEventType doubleOutOfRange = new BBacnetEventType(14);
   public static final BBacnetEventType signedOutOfRange = new BBacnetEventType(15);
   public static final BBacnetEventType unsignedOutOfRange = new BBacnetEventType(16);
   public static final BBacnetEventType changeOfCharacterstring = new BBacnetEventType(17);
   public static final BBacnetEventType changeOfStatusFlags = new BBacnetEventType(18);
   public static final BBacnetEventType changeOfReliability = new BBacnetEventType(19);
   public static final BBacnetEventType none = new BBacnetEventType(20);
   public static final BBacnetEventType changeOfDiscreteValue = new BBacnetEventType(21);
   public static final BBacnetEventType DEFAULT = changeOfBitstring;
   public static final Type TYPE = Sys.loadType(BBacnetEventType.class);
   public static final int MAX_ASHRAE_ID = 20;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetEventType make(int ordinal) {
      return (BBacnetEventType)changeOfBitstring.getRange().get(ordinal, false);
   }

   public static BBacnetEventType make(String tag) {
      return (BBacnetEventType)changeOfBitstring.getRange().get(tag);
   }

   private BBacnetEventType(int ordinal) {
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
      return id > 20 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 20;
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
