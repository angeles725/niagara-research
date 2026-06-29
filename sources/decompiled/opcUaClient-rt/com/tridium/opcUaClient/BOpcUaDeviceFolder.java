package com.tridium.opcUaClient;

import com.tridium.ndriver.BNDeviceFolder;
import javax.baja.agent.AgentList;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BOpcUaDeviceFolder extends BNDeviceFolder {
   public static final Type TYPE = Sys.loadType(BOpcUaDeviceFolder.class);

   public Type getType() {
      return TYPE;
   }

   public final BOpcUaNetwork getOpcUaNetwork() {
      return (BOpcUaNetwork)this.getNetwork();
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BOpcUaNetwork || parent instanceof BOpcUaDeviceFolder;
   }

   public final AgentList getAgents(Context cx) {
      AgentList list = super.getAgents(cx);
      list.remove("ndriver:NDeviceManager");
      return list;
   }
}
