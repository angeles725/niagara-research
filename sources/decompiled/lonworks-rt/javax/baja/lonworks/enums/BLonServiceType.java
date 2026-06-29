package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("acked"), @Range("unackedRpt"), @Range("unacked"), @Range("request")}
)
public final class BLonServiceType extends BFrozenEnum {
   public static final int ACKED = 0;
   public static final int UNACKED_RPT = 1;
   public static final int UNACKED = 2;
   public static final int REQUEST = 3;
   public static final BLonServiceType acked = new BLonServiceType(0);
   public static final BLonServiceType unackedRpt = new BLonServiceType(1);
   public static final BLonServiceType unacked = new BLonServiceType(2);
   public static final BLonServiceType request = new BLonServiceType(3);
   public static final BLonServiceType DEFAULT = acked;
   public static final Type TYPE = Sys.loadType(BLonServiceType.class);

   public static BLonServiceType make(int ordinal) {
      return (BLonServiceType)acked.getRange().get(ordinal, false);
   }

   public static BLonServiceType make(String tag) {
      return (BLonServiceType)acked.getRange().get(tag);
   }

   private BLonServiceType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public BLonServiceType getWriteServiceType() {
      return this == request ? acked : this;
   }
}
