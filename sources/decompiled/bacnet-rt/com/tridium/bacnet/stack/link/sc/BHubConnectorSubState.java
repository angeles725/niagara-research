package com.tridium.bacnet.stack.link.sc;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("noHubConnection"), @Range("connectingToPrimary"), @Range("connectedToPrimary"), @Range("connectingToFailover"), @Range("connectedToFailover"), @Range("reconnectingToPrimary")},
   defaultValue = "noHubConnection"
)
public final class BHubConnectorSubState extends BFrozenEnum {
   public static final int NO_HUB_CONNECTION = 0;
   public static final int CONNECTING_TO_PRIMARY = 1;
   public static final int CONNECTED_TO_PRIMARY = 2;
   public static final int CONNECTING_TO_FAILOVER = 3;
   public static final int CONNECTED_TO_FAILOVER = 4;
   public static final int RECONNECTING_TO_PRIMARY = 5;
   public static final BHubConnectorSubState noHubConnection = new BHubConnectorSubState(0);
   public static final BHubConnectorSubState connectingToPrimary = new BHubConnectorSubState(1);
   public static final BHubConnectorSubState connectedToPrimary = new BHubConnectorSubState(2);
   public static final BHubConnectorSubState connectingToFailover = new BHubConnectorSubState(3);
   public static final BHubConnectorSubState connectedToFailover = new BHubConnectorSubState(4);
   public static final BHubConnectorSubState reconnectingToPrimary = new BHubConnectorSubState(5);
   public static final BHubConnectorSubState DEFAULT = noHubConnection;
   public static final Type TYPE = Sys.loadType(BHubConnectorSubState.class);

   public static BHubConnectorSubState make(int ordinal) {
      return (BHubConnectorSubState)noHubConnection.getRange().get(ordinal, false);
   }

   public static BHubConnectorSubState make(String tag) {
      return (BHubConnectorSubState)noHubConnection.getRange().get(tag);
   }

   private BHubConnectorSubState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
