package com.tridium.bacnet.stack.network;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("normal"), @Range("urgent"), @Range("criticalEquipment"), @Range("lifeSafety")}
)
public final class BNetworkPriority extends BFrozenEnum {
   public static final int NORMAL = 0;
   public static final int URGENT = 1;
   public static final int CRITICAL_EQUIPMENT = 2;
   public static final int LIFE_SAFETY = 3;
   public static final BNetworkPriority normal = new BNetworkPriority(0);
   public static final BNetworkPriority urgent = new BNetworkPriority(1);
   public static final BNetworkPriority criticalEquipment = new BNetworkPriority(2);
   public static final BNetworkPriority lifeSafety = new BNetworkPriority(3);
   public static final BNetworkPriority DEFAULT = normal;
   public static final Type TYPE = Sys.loadType(BNetworkPriority.class);

   public static BNetworkPriority make(int ordinal) {
      return (BNetworkPriority)normal.getRange().get(ordinal, false);
   }

   public static BNetworkPriority make(String tag) {
      return (BNetworkPriority)normal.getRange().get(tag);
   }

   private BNetworkPriority(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public final int getPriorityCode() {
      return this.getOrdinal();
   }
}
