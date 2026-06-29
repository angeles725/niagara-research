package com.tridium.bacnet.stack.link;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("ip"), @Range("ethernet"), @Range("arcnet"), @Range("mstp"), @Range("ptp"), @Range("lontalk"), @Range("ipTunneling"), @Range("sc")}
)
public final class BLinkLayerChoice extends BFrozenEnum {
   public static final int IP = 0;
   public static final int ETHERNET = 1;
   public static final int ARCNET = 2;
   public static final int MSTP = 3;
   public static final int PTP = 4;
   public static final int LONTALK = 5;
   public static final int IP_TUNNELING = 6;
   public static final int SC = 7;
   public static final BLinkLayerChoice ip = new BLinkLayerChoice(0);
   public static final BLinkLayerChoice ethernet = new BLinkLayerChoice(1);
   public static final BLinkLayerChoice arcnet = new BLinkLayerChoice(2);
   public static final BLinkLayerChoice mstp = new BLinkLayerChoice(3);
   public static final BLinkLayerChoice ptp = new BLinkLayerChoice(4);
   public static final BLinkLayerChoice lontalk = new BLinkLayerChoice(5);
   public static final BLinkLayerChoice ipTunneling = new BLinkLayerChoice(6);
   public static final BLinkLayerChoice sc = new BLinkLayerChoice(7);
   public static final BLinkLayerChoice DEFAULT = ip;
   public static final Type TYPE = Sys.loadType(BLinkLayerChoice.class);

   public static BLinkLayerChoice make(int ordinal) {
      return (BLinkLayerChoice)ip.getRange().get(ordinal, false);
   }

   public static BLinkLayerChoice make(String tag) {
      return (BLinkLayerChoice)ip.getRange().get(tag);
   }

   private BLinkLayerChoice(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
