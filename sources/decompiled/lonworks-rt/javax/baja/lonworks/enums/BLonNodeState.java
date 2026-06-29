package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("unknown"), @Range("unconfigured"), @Range("configOnline"), @Range("configOffline"), @Range("applicationless"), @Range("hardOffline")}
)
public final class BLonNodeState extends BFrozenEnum {
   public static final int UNKNOWN = 0;
   public static final int UNCONFIGURED = 1;
   public static final int CONFIG_ONLINE = 2;
   public static final int CONFIG_OFFLINE = 3;
   public static final int APPLICATIONLESS = 4;
   public static final int HARD_OFFLINE = 5;
   public static final BLonNodeState unknown = new BLonNodeState(0);
   public static final BLonNodeState unconfigured = new BLonNodeState(1);
   public static final BLonNodeState configOnline = new BLonNodeState(2);
   public static final BLonNodeState configOffline = new BLonNodeState(3);
   public static final BLonNodeState applicationless = new BLonNodeState(4);
   public static final BLonNodeState hardOffline = new BLonNodeState(5);
   public static final BLonNodeState DEFAULT = unknown;
   public static final Type TYPE = Sys.loadType(BLonNodeState.class);

   public static BLonNodeState make(int ordinal) {
      return (BLonNodeState)unknown.getRange().get(ordinal, false);
   }

   public static BLonNodeState make(String tag) {
      return (BLonNodeState)unknown.getRange().get(tag);
   }

   private BLonNodeState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isConfigured() {
      return this.getOrdinal() == 2 || this.getOrdinal() == 3;
   }
}
