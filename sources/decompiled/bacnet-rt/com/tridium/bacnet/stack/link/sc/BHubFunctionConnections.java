package com.tridium.bacnet.stack.link.sc;

import com.tridium.bacnet.stack.link.sc.connection.BHubAcceptingConnection;
import java.util.logging.Logger;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "disconnectAll",
      flags = 128
   ), @NiagaraAction(
      name = "removeAllIdleAccepted",
      flags = 128
   )})
public final class BHubFunctionConnections extends BConnectionContainer {
   public static final Action disconnectAll = newAction(128, null);
   public static final Action removeAllIdleAccepted = newAction(128, null);
   public static final Type TYPE = Sys.loadType(BHubFunctionConnections.class);
   private static final Logger logger = Logger.getLogger("bacnet.sc.hubFunction");

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
      return parent instanceof BHubFunction;
   }

   public boolean isChildLegal(BComponent child) {
      return child instanceof BHubAcceptingConnection;
   }

   @Override
   public Logger getLogger() {
      return logger;
   }
}
