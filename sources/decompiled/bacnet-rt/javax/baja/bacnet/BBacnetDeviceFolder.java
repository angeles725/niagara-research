package javax.baja.bacnet;

import javax.baja.agent.AgentList;
import javax.baja.driver.BDeviceFolder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetDeviceFolder extends BDeviceFolder {
   public static final Type TYPE = Sys.loadType(BBacnetDeviceFolder.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetNetwork || parent instanceof BBacnetDeviceFolder;
   }

   public final BBacnetNetwork network() {
      return (BBacnetNetwork)this.getNetwork();
   }

   public AgentList getAgents(Context cx) {
      AgentList agents = super.getAgents(cx);
      agents.remove("driver:DeviceManager");
      agents.toBottom("bacnetEDE:EdeBacnetDeviceManager");
      return agents;
   }

   public String toString(Context cx) {
      return "BacnetDeviceFolder:" + this.getName();
   }
}
