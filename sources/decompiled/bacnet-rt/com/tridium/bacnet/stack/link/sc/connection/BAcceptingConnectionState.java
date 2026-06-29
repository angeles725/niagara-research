package com.tridium.bacnet.stack.link.sc.connection;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("idle"), @Range("awaitingRequest"), @Range("connected"), @Range("disconnecting")},
   defaultValue = "idle"
)
public final class BAcceptingConnectionState extends BFrozenEnum {
   public static final int IDLE = 0;
   public static final int AWAITING_REQUEST = 1;
   public static final int CONNECTED = 2;
   public static final int DISCONNECTING = 3;
   public static final BAcceptingConnectionState idle = new BAcceptingConnectionState(0);
   public static final BAcceptingConnectionState awaitingRequest = new BAcceptingConnectionState(1);
   public static final BAcceptingConnectionState connected = new BAcceptingConnectionState(2);
   public static final BAcceptingConnectionState disconnecting = new BAcceptingConnectionState(3);
   public static final BAcceptingConnectionState DEFAULT = idle;
   public static final Type TYPE = Sys.loadType(BAcceptingConnectionState.class);

   public static BAcceptingConnectionState make(int ordinal) {
      return (BAcceptingConnectionState)idle.getRange().get(ordinal, false);
   }

   public static BAcceptingConnectionState make(String tag) {
      return (BAcceptingConnectionState)idle.getRange().get(tag);
   }

   private BAcceptingConnectionState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
