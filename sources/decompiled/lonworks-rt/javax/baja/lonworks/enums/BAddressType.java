package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("none"), @Range("group"), @Range("subnetNode"), @Range("broadcast"), @Range("turnaround")}
)
public final class BAddressType extends BFrozenEnum {
   public static final int NONE = 0;
   public static final int GROUP = 1;
   public static final int SUBNET_NODE = 2;
   public static final int BROADCAST = 3;
   public static final int TURNAROUND = 4;
   public static final BAddressType none = new BAddressType(0);
   public static final BAddressType group = new BAddressType(1);
   public static final BAddressType subnetNode = new BAddressType(2);
   public static final BAddressType broadcast = new BAddressType(3);
   public static final BAddressType turnaround = new BAddressType(4);
   public static final BAddressType DEFAULT = none;
   public static final Type TYPE = Sys.loadType(BAddressType.class);

   public static BAddressType make(int ordinal) {
      return (BAddressType)none.getRange().get(ordinal, false);
   }

   public static BAddressType make(String tag) {
      return (BAddressType)none.getRange().get(tag);
   }

   private BAddressType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
