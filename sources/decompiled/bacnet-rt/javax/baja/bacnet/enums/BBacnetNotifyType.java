package javax.baja.bacnet.enums;

import javax.baja.alarm.ext.BNotifyType;
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
   range = {@Range("alarm"), @Range("event"), @Range("ackNotification")}
)
public final class BBacnetNotifyType extends BFrozenEnum {
   public static final int ALARM = 0;
   public static final int EVENT = 1;
   public static final int ACK_NOTIFICATION = 2;
   public static final BBacnetNotifyType alarm = new BBacnetNotifyType(0);
   public static final BBacnetNotifyType event = new BBacnetNotifyType(1);
   public static final BBacnetNotifyType ackNotification = new BBacnetNotifyType(2);
   public static final BBacnetNotifyType DEFAULT = alarm;
   public static final Type TYPE = Sys.loadType(BBacnetNotifyType.class);

   public static BBacnetNotifyType make(int ordinal) {
      return (BBacnetNotifyType)alarm.getRange().get(ordinal, false);
   }

   public static BBacnetNotifyType make(String tag) {
      return (BBacnetNotifyType)alarm.getRange().get(tag);
   }

   private BBacnetNotifyType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      return DEFAULT.getRange().getTag(id);
   }

   public static BBacnetNotifyType make(BNotifyType notifyType) {
      return new BBacnetNotifyType(notifyType.getOrdinal());
   }

   public static int fromBNotifyType(BNotifyType notifyType) {
      return notifyType.getOrdinal();
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
