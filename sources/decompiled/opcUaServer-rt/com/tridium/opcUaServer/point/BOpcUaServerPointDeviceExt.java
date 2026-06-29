package com.tridium.opcUaServer.point;

import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import com.tridium.ndriver.point.BNPointDeviceExt;
import com.tridium.opcUaServer.BOpcUaNamespace;
import com.tridium.opcUaServer.BOpcUaServer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentList;
import javax.baja.collection.BITable;
import javax.baja.control.BControlPoint;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Cursor;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "discoveryPreferences",
      type = "OpcUaServerPointDiscoveryPreferences",
      defaultValue = "new BOpcUaServerPointDiscoveryPreferences()",
      override = true
   ), @NiagaraProperty(
      name = "uaNodeId",
      type = "String",
      defaultValue = "",
      flags = 67
   )})
public class BOpcUaServerPointDeviceExt extends BNPointDeviceExt implements BIOpcUaServerPointFolder {
   public static final Property discoveryPreferences = newProperty(0, new BOpcUaServerPointDiscoveryPreferences(), null);
   public static final Property uaNodeId = newProperty(67, "", null);
   public static final Type TYPE = Sys.loadType(BOpcUaServerPointDeviceExt.class);
   Logger logger = Logger.getLogger("opcUaServer.point");

   @Override
   public String getUaNodeId() {
      return this.getString(uaNodeId);
   }

   public void setUaNodeId(String v) {
      this.setString(uaNodeId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final BOpcUaServer getOpcUaServer() {
      return (BOpcUaServer)this.getNetwork();
   }

   public final BOpcUaNamespace getOpcUaServerDevice() {
      return (BOpcUaNamespace)this.getDevice();
   }

   public Type getDeviceType() {
      return BOpcUaNamespace.TYPE;
   }

   public Type getPointFolderType() {
      return BOpcUaServerPointFolder.TYPE;
   }

   public Type getProxyExtType() {
      return BOpcUaServerProxyExt.TYPE;
   }

   public BOrd doSubmitDiscoveryJob(BNDiscoveryPreferences preferences) {
      this.setDiscoveryPreferences((BNDiscoveryPreferences)preferences.newCopy());
      BOpcUaServerPointDiscoveryJob discoveryJob = new BOpcUaServerPointDiscoveryJob(this);
      discoveryJob.setDiscoveryPreferences((BNDiscoveryPreferences)preferences.newCopy());
      return discoveryJob.submit(null);
   }

   public BINDiscoveryObject[] getDiscoveryObjects(BNDiscoveryPreferences prefs) throws Exception {
      BOpcUaServerPointDiscoveryPreferences pref = (BOpcUaServerPointDiscoveryPreferences)prefs;
      this.logger.log(Level.INFO, "OpcUaServer discover points job");
      BOrd srchOrd = BOrd.make(pref.getSearchLocation().encodeToString().concat(" where proxyExt.type != opcUaServer:OpcUaServerProxyExt"));
      BITable<?> result = (BITable<?>)srchOrd.resolve(this).get();
      Cursor<?> c = result.cursor();
      String thisSlotPath = this.getSlotPath().toString();
      ArrayList<BOpcUaServerLearnPointEntry> points = new ArrayList<>();

      while (c.next()) {
         BControlPoint point = (BControlPoint)c.get();
         String slotPath = point.getSlotPath().toString();
         if (!slotPath.startsWith(thisSlotPath)) {
            points.add(BOpcUaServerLearnPointEntry.make(point));
         }
      }

      return points.toArray(new BOpcUaServerLearnPointEntry[0]);
   }

   public final AgentList getAgents(Context cx) {
      AgentList list = super.getAgents(cx);
      list.remove("ndriver:NPointManager");
      return list;
   }
}
