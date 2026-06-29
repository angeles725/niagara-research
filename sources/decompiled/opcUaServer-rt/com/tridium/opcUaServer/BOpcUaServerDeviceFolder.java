package com.tridium.opcUaServer;

import com.tridium.ndriver.BNDeviceFolder;
import javax.baja.agent.AgentList;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BOpcUaServerDeviceFolder extends BNDeviceFolder {
   public static final Type TYPE = Sys.loadType(BOpcUaServerDeviceFolder.class);

   public Type getType() {
      return TYPE;
   }

   public final BOpcUaServer getOpcUaServer() {
      return (BOpcUaServer)this.getNetwork();
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BOpcUaServer || parent instanceof BOpcUaServerDeviceFolder;
   }

   public final AgentList getAgents(Context cx) {
      AgentList list = super.getAgents(cx);
      list.remove("ndriver:NDeviceManager");
      return list;
   }
}
