package com.tridium.bacnet.stack.link.sc;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "noHubConnection",
      ordinal = 0
   ), @Range(
      value = "connectedToPrimary",
      ordinal = 1
   ), @Range(
      value = "connectedToFailover",
      ordinal = 2
   )},
   defaultValue = "noHubConnection"
)
public final class BScHubConnectorState extends BFrozenEnum {
   public static final int NO_HUB_CONNECTION = 0;
   public static final int CONNECTED_TO_PRIMARY = 1;
   public static final int CONNECTED_TO_FAILOVER = 2;
   public static final BScHubConnectorState noHubConnection = new BScHubConnectorState(0);
   public static final BScHubConnectorState connectedToPrimary = new BScHubConnectorState(1);
   public static final BScHubConnectorState connectedToFailover = new BScHubConnectorState(2);
   public static final BScHubConnectorState DEFAULT = noHubConnection;
   public static final Type TYPE = Sys.loadType(BScHubConnectorState.class);

   public static BScHubConnectorState make(int ordinal) {
      return (BScHubConnectorState)noHubConnection.getRange().get(ordinal, false);
   }

   public static BScHubConnectorState make(String tag) {
      return (BScHubConnectorState)noHubConnection.getRange().get(tag);
   }

   private BScHubConnectorState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
