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
   range = {@Range("idle"), @Range("inProgress"), @Range("successful"), @Range("failed")}
)
public final class BBacnetWriteStatus extends BFrozenEnum implements BacnetConst {
   public static final int IDLE = 0;
   public static final int IN_PROGRESS = 1;
   public static final int SUCCESSFUL = 2;
   public static final int FAILED = 3;
   public static final BBacnetWriteStatus idle = new BBacnetWriteStatus(0);
   public static final BBacnetWriteStatus inProgress = new BBacnetWriteStatus(1);
   public static final BBacnetWriteStatus successful = new BBacnetWriteStatus(2);
   public static final BBacnetWriteStatus failed = new BBacnetWriteStatus(3);
   public static final BBacnetWriteStatus DEFAULT = idle;
   public static final Type TYPE = Sys.loadType(BBacnetWriteStatus.class);
   public static final int MAX_ASHRAE_ID = 3;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetWriteStatus make(int ordinal) {
      return (BBacnetWriteStatus)idle.getRange().get(ordinal, false);
   }

   public static BBacnetWriteStatus make(String tag) {
      return (BBacnetWriteStatus)idle.getRange().get(tag);
   }

   private BBacnetWriteStatus(int ordinal) {
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
      return id > 3 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 3;
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
