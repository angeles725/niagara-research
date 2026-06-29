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
   range = {@Range("none"), @Range("silence"), @Range("silenceAudible"), @Range("silenceVisual"), @Range("reset"), @Range("resetAlarm"), @Range("resetFault"), @Range("unsilence"), @Range("unsilenceAudible"), @Range("unsilenceVisual")}
)
public final class BBacnetLifeSafetyOperation extends BFrozenEnum implements BacnetConst {
   public static final int NONE = 0;
   public static final int SILENCE = 1;
   public static final int SILENCE_AUDIBLE = 2;
   public static final int SILENCE_VISUAL = 3;
   public static final int RESET = 4;
   public static final int RESET_ALARM = 5;
   public static final int RESET_FAULT = 6;
   public static final int UNSILENCE = 7;
   public static final int UNSILENCE_AUDIBLE = 8;
   public static final int UNSILENCE_VISUAL = 9;
   public static final BBacnetLifeSafetyOperation none = new BBacnetLifeSafetyOperation(0);
   public static final BBacnetLifeSafetyOperation silence = new BBacnetLifeSafetyOperation(1);
   public static final BBacnetLifeSafetyOperation silenceAudible = new BBacnetLifeSafetyOperation(2);
   public static final BBacnetLifeSafetyOperation silenceVisual = new BBacnetLifeSafetyOperation(3);
   public static final BBacnetLifeSafetyOperation reset = new BBacnetLifeSafetyOperation(4);
   public static final BBacnetLifeSafetyOperation resetAlarm = new BBacnetLifeSafetyOperation(5);
   public static final BBacnetLifeSafetyOperation resetFault = new BBacnetLifeSafetyOperation(6);
   public static final BBacnetLifeSafetyOperation unsilence = new BBacnetLifeSafetyOperation(7);
   public static final BBacnetLifeSafetyOperation unsilenceAudible = new BBacnetLifeSafetyOperation(8);
   public static final BBacnetLifeSafetyOperation unsilenceVisual = new BBacnetLifeSafetyOperation(9);
   public static final BBacnetLifeSafetyOperation DEFAULT = none;
   public static final Type TYPE = Sys.loadType(BBacnetLifeSafetyOperation.class);
   public static final int MAX_ASHRAE_ID = 9;
   public static final int MAX_RESERVED_ID = 64;
   public static final int MAX_ID = 65535;

   public static BBacnetLifeSafetyOperation make(int ordinal) {
      return (BBacnetLifeSafetyOperation)none.getRange().get(ordinal, false);
   }

   public static BBacnetLifeSafetyOperation make(String tag) {
      return (BBacnetLifeSafetyOperation)none.getRange().get(tag);
   }

   private BBacnetLifeSafetyOperation(int ordinal) {
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
      return id > 64 && id <= 65535;
   }

   public static boolean isAshrae(int id) {
      return id > 9 && id <= 64;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 9;
   }
}
