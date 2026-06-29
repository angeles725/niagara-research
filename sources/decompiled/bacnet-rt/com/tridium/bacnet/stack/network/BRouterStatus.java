package com.tridium.bacnet.stack.network;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("ok"), @Range("routerUnavailable"), @Range("routerBusy"), @Range("routerNotConnected"), @Range("unknown")}
)
public final class BRouterStatus extends BFrozenEnum {
   public static final int OK = 0;
   public static final int ROUTER_UNAVAILABLE = 1;
   public static final int ROUTER_BUSY = 2;
   public static final int ROUTER_NOT_CONNECTED = 3;
   public static final int UNKNOWN = 4;
   public static final BRouterStatus ok = new BRouterStatus(0);
   public static final BRouterStatus routerUnavailable = new BRouterStatus(1);
   public static final BRouterStatus routerBusy = new BRouterStatus(2);
   public static final BRouterStatus routerNotConnected = new BRouterStatus(3);
   public static final BRouterStatus unknown = new BRouterStatus(4);
   public static final BRouterStatus DEFAULT = ok;
   public static final Type TYPE = Sys.loadType(BRouterStatus.class);

   public static BRouterStatus make(int ordinal) {
      return (BRouterStatus)ok.getRange().get(ordinal, false);
   }

   public static BRouterStatus make(String tag) {
      return (BRouterStatus)ok.getRange().get(tag);
   }

   private BRouterStatus(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
