package com.tridium.bacnet.stack.link.sc.connection;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("idle"), @Range("awaitingWebSocket"), @Range("awaitingAccept"), @Range("connected"), @Range("disconnecting")},
   defaultValue = "idle"
)
public final class BInitiatingConnectionState extends BFrozenEnum {
   public static final int IDLE = 0;
   public static final int AWAITING_WEB_SOCKET = 1;
   public static final int AWAITING_ACCEPT = 2;
   public static final int CONNECTED = 3;
   public static final int DISCONNECTING = 4;
   public static final BInitiatingConnectionState idle = new BInitiatingConnectionState(0);
   public static final BInitiatingConnectionState awaitingWebSocket = new BInitiatingConnectionState(1);
   public static final BInitiatingConnectionState awaitingAccept = new BInitiatingConnectionState(2);
   public static final BInitiatingConnectionState connected = new BInitiatingConnectionState(3);
   public static final BInitiatingConnectionState disconnecting = new BInitiatingConnectionState(4);
   public static final BInitiatingConnectionState DEFAULT = idle;
   public static final Type TYPE = Sys.loadType(BInitiatingConnectionState.class);

   public static BInitiatingConnectionState make(int ordinal) {
      return (BInitiatingConnectionState)idle.getRange().get(ordinal, false);
   }

   public static BInitiatingConnectionState make(String tag) {
      return (BInitiatingConnectionState)idle.getRange().get(tag);
   }

   private BInitiatingConnectionState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
