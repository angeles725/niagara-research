package com.tridium.opcUaClient.point;

import com.tridium.ndriver.discover.BINDiscoveryHost;
import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import com.tridium.ndriver.point.BNPointFolder;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.BOpcUaNetwork;
import javax.baja.agent.AgentList;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BOpcUaClientPointFolder extends BNPointFolder implements BINDiscoveryHost {
   public static final Type TYPE = Sys.loadType(BOpcUaClientPointFolder.class);

   public Type getType() {
      return TYPE;
   }

   public final BOpcUaNetwork getOpcUaClientNetwork() {
      return (BOpcUaNetwork)this.getNetwork();
   }

   public final BOpcUaDevice getOpcUaClientDevice() {
      return (BOpcUaDevice)this.getDevice();
   }

   public BOrd submitDiscoveryJob(BNDiscoveryPreferences discoveryParams) {
      return this.getOpcUaClientDevice().getPoints().submitDiscoveryJob(discoveryParams);
   }

   public BNDiscoveryPreferences getDiscoveryPreferences() {
      return this.getOpcUaClientDevice().getPoints().getDiscoveryPreferences();
   }

   public BINDiscoveryObject[] getDiscoveryObjects(BNDiscoveryPreferences prefs) throws Exception {
      BOpcUaClientPointDeviceExt points = this.getOpcUaClientDevice().getPoints();
      return points.getDiscoveryObjects(points.getDiscoveryPreferences());
   }

   public final AgentList getAgents(Context cx) {
      AgentList list = super.getAgents(cx);
      list.remove("ndriver:NPointManager");
      return list;
   }
}
