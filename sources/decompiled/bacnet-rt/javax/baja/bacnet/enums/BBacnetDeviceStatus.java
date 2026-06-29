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
   range = {@Range("operational"), @Range("operationalReadOnly"), @Range("downloadRequired"), @Range("downloadInProgress"), @Range("nonOperational"), @Range("backupInProgress")}
)
public final class BBacnetDeviceStatus extends BFrozenEnum implements BacnetConst {
   public static final int OPERATIONAL = 0;
   public static final int OPERATIONAL_READ_ONLY = 1;
   public static final int DOWNLOAD_REQUIRED = 2;
   public static final int DOWNLOAD_IN_PROGRESS = 3;
   public static final int NON_OPERATIONAL = 4;
   public static final int BACKUP_IN_PROGRESS = 5;
   public static final BBacnetDeviceStatus operational = new BBacnetDeviceStatus(0);
   public static final BBacnetDeviceStatus operationalReadOnly = new BBacnetDeviceStatus(1);
   public static final BBacnetDeviceStatus downloadRequired = new BBacnetDeviceStatus(2);
   public static final BBacnetDeviceStatus downloadInProgress = new BBacnetDeviceStatus(3);
   public static final BBacnetDeviceStatus nonOperational = new BBacnetDeviceStatus(4);
   public static final BBacnetDeviceStatus backupInProgress = new BBacnetDeviceStatus(5);
   public static final BBacnetDeviceStatus DEFAULT = operational;
   public static final Type TYPE = Sys.loadType(BBacnetDeviceStatus.class);
   public static final int MAX_ASHRAE_ID = 5;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetDeviceStatus make(int ordinal) {
      return (BBacnetDeviceStatus)operational.getRange().get(ordinal, false);
   }

   public static BBacnetDeviceStatus make(String tag) {
      return (BBacnetDeviceStatus)operational.getRange().get(tag);
   }

   private BBacnetDeviceStatus(int ordinal) {
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
      return id > 5 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 5;
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
