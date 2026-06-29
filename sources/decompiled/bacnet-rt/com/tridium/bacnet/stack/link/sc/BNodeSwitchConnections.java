package com.tridium.bacnet.stack.link.sc;

import com.tridium.bacnet.stack.link.sc.connection.BAddDirectInitiatingConnectionParams;
import com.tridium.bacnet.stack.link.sc.connection.BDirectAcceptingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BDirectInitiatingConnection;
import java.util.logging.Logger;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "idleCleanupInterval",
   type = "BRelTime",
   defaultValue = "BRelTime.makeDays(1)",
   facets = {@Facet(
      name = "BFacets.SHOW_SECONDS",
      value = "false"
   ), @Facet(
      name = "BFacets.MIN",
      value = "BRelTime.make(0)"
   )},
   override = true
)
@NiagaraActions({@NiagaraAction(
      name = "addConnection",
      parameterType = "BAddDirectInitiatingConnectionParams",
      defaultValue = "BAddDirectInitiatingConnectionParams.DEFAULT"
   ), @NiagaraAction(
      name = "disconnectAll",
      flags = 128
   ), @NiagaraAction(
      name = "removeAllIdleAccepted",
      flags = 128
   )})
public final class BNodeSwitchConnections extends BConnectionContainer {
   public static final Property idleCleanupInterval = newProperty(
      0, BRelTime.makeDays(1), BFacets.make(BFacets.make("showSeconds", false), BFacets.make("min", BRelTime.make(0L)))
   );
   public static final Action addConnection = newAction(0, BAddDirectInitiatingConnectionParams.DEFAULT, null);
   public static final Action disconnectAll = newAction(128, null);
   public static final Action removeAllIdleAccepted = newAction(128, null);
   public static final Type TYPE = Sys.loadType(BNodeSwitchConnections.class);
   private static final Logger logger = Logger.getLogger("bacnet.sc.nodeSwitch");

   public void addConnection(BAddDirectInitiatingConnectionParams parameter) {
      this.invoke(addConnection, parameter, null);
   }

   public void disconnectAll() {
      this.invoke(disconnectAll, null, null);
   }

   public void removeAllIdleAccepted() {
      this.invoke(removeAllIdleAccepted, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BNodeSwitch;
   }

   public boolean isChildLegal(BComponent child) {
      return child instanceof BDirectAcceptingConnection || child instanceof BDirectInitiatingConnection;
   }

   public void doAddConnection(BAddDirectInitiatingConnectionParams params) {
      BDirectInitiatingConnection connection = BDirectInitiatingConnection.make(params.getUriAddress(), params.getUriPath(), params.getVmac());
      String connectionName = params.getConnectionName().trim();
      if (connectionName.isEmpty()) {
         connectionName = "InitiatingConnection1?";
      }

      this.add(connectionName, connection);
   }

   @Override
   public Logger getLogger() {
      return logger;
   }
}
