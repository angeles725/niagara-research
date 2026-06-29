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
   range = {@Range("normal"), @Range("loadFailed"), @Range("internal"), @Range("program"), @Range("other")}
)
public final class BBacnetProgramError extends BFrozenEnum implements BacnetConst {
   public static final int NORMAL = 0;
   public static final int LOAD_FAILED = 1;
   public static final int INTERNAL = 2;
   public static final int PROGRAM = 3;
   public static final int OTHER = 4;
   public static final BBacnetProgramError normal = new BBacnetProgramError(0);
   public static final BBacnetProgramError loadFailed = new BBacnetProgramError(1);
   public static final BBacnetProgramError internal = new BBacnetProgramError(2);
   public static final BBacnetProgramError program = new BBacnetProgramError(3);
   public static final BBacnetProgramError other = new BBacnetProgramError(4);
   public static final BBacnetProgramError DEFAULT = normal;
   public static final Type TYPE = Sys.loadType(BBacnetProgramError.class);
   public static final int MAX_ASHRAE_ID = 4;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetProgramError make(int ordinal) {
      return (BBacnetProgramError)normal.getRange().get(ordinal, false);
   }

   public static BBacnetProgramError make(String tag) {
      return (BBacnetProgramError)normal.getRange().get(tag);
   }

   private BBacnetProgramError(int ordinal) {
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
      return id > 4 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 4;
   }
}
