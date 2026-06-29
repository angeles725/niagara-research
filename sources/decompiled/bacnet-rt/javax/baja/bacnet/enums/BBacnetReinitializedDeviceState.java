package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("coldStart"), @Range("warmStart"), @Range("startBackup"), @Range("endBackup"), @Range("startRestore"), @Range("endRestore"), @Range("abortRestore")}
)
public final class BBacnetReinitializedDeviceState extends BFrozenEnum {
   public static final int COLD_START = 0;
   public static final int WARM_START = 1;
   public static final int START_BACKUP = 2;
   public static final int END_BACKUP = 3;
   public static final int START_RESTORE = 4;
   public static final int END_RESTORE = 5;
   public static final int ABORT_RESTORE = 6;
   public static final BBacnetReinitializedDeviceState coldStart = new BBacnetReinitializedDeviceState(0);
   public static final BBacnetReinitializedDeviceState warmStart = new BBacnetReinitializedDeviceState(1);
   public static final BBacnetReinitializedDeviceState startBackup = new BBacnetReinitializedDeviceState(2);
   public static final BBacnetReinitializedDeviceState endBackup = new BBacnetReinitializedDeviceState(3);
   public static final BBacnetReinitializedDeviceState startRestore = new BBacnetReinitializedDeviceState(4);
   public static final BBacnetReinitializedDeviceState endRestore = new BBacnetReinitializedDeviceState(5);
   public static final BBacnetReinitializedDeviceState abortRestore = new BBacnetReinitializedDeviceState(6);
   public static final BBacnetReinitializedDeviceState DEFAULT = coldStart;
   public static final Type TYPE = Sys.loadType(BBacnetReinitializedDeviceState.class);

   public static BBacnetReinitializedDeviceState make(int ordinal) {
      return (BBacnetReinitializedDeviceState)coldStart.getRange().get(ordinal, false);
   }

   public static BBacnetReinitializedDeviceState make(String tag) {
      return (BBacnetReinitializedDeviceState)coldStart.getRange().get(tag);
   }

   private BBacnetReinitializedDeviceState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      return DEFAULT.getRange().getTag(id);
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
