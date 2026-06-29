package com.tridium.bacnet.stack.link.sc.connection;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BAbstractScWebSocketInitiator extends BComponent {
   public static final Type TYPE = Sys.loadType(BAbstractScWebSocketInitiator.class);

   public Type getType() {
      return TYPE;
   }

   public abstract IScWebSocket initiateWebSocket(BInitiatingConnection var1) throws Exception;

   public abstract void linkCommStart() throws Exception;

   public abstract void linkCommStop();

   public abstract void updateWebSocketSettings();
}
