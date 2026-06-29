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
   range = {@Range("idle"), @Range("preparingForBackup"), @Range("preparingForRestore"), @Range("performingABackup"), @Range("performingARestore"), @Range("backupFailure"), @Range("restoreFailure")}
)
public final class BBacnetBackupState extends BFrozenEnum {
   public static final int IDLE = 0;
   public static final int PREPARING_FOR_BACKUP = 1;
   public static final int PREPARING_FOR_RESTORE = 2;
   public static final int PERFORMING_ABACKUP = 3;
   public static final int PERFORMING_ARESTORE = 4;
   public static final int BACKUP_FAILURE = 5;
   public static final int RESTORE_FAILURE = 6;
   public static final BBacnetBackupState idle = new BBacnetBackupState(0);
   public static final BBacnetBackupState preparingForBackup = new BBacnetBackupState(1);
   public static final BBacnetBackupState preparingForRestore = new BBacnetBackupState(2);
   public static final BBacnetBackupState performingABackup = new BBacnetBackupState(3);
   public static final BBacnetBackupState performingARestore = new BBacnetBackupState(4);
   public static final BBacnetBackupState backupFailure = new BBacnetBackupState(5);
   public static final BBacnetBackupState restoreFailure = new BBacnetBackupState(6);
   public static final BBacnetBackupState DEFAULT = idle;
   public static final Type TYPE = Sys.loadType(BBacnetBackupState.class);

   public static BBacnetBackupState make(int ordinal) {
      return (BBacnetBackupState)idle.getRange().get(ordinal, false);
   }

   public static BBacnetBackupState make(String tag) {
      return (BBacnetBackupState)idle.getRange().get(tag);
   }

   private BBacnetBackupState(int ordinal) {
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
