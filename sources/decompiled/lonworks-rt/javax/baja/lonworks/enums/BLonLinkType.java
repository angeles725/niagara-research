package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("unknown"), @Range("standard"), @Range("reliable"), @Range("critical"), @Range("authenticated"), @Range("pollOnly")}
)
public final class BLonLinkType extends BFrozenEnum {
   public static final int UNKNOWN = 0;
   public static final int STANDARD = 1;
   public static final int RELIABLE = 2;
   public static final int CRITICAL = 3;
   public static final int AUTHENTICATED = 4;
   public static final int POLL_ONLY = 5;
   public static final BLonLinkType unknown = new BLonLinkType(0);
   public static final BLonLinkType standard = new BLonLinkType(1);
   public static final BLonLinkType reliable = new BLonLinkType(2);
   public static final BLonLinkType critical = new BLonLinkType(3);
   public static final BLonLinkType authenticated = new BLonLinkType(4);
   public static final BLonLinkType pollOnly = new BLonLinkType(5);
   public static final BLonLinkType DEFAULT = unknown;
   public static final Type TYPE = Sys.loadType(BLonLinkType.class);

   public static BLonLinkType make(int ordinal) {
      return (BLonLinkType)unknown.getRange().get(ordinal, false);
   }

   public static BLonLinkType make(String tag) {
      return (BLonLinkType)unknown.getRange().get(tag);
   }

   private BLonLinkType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
