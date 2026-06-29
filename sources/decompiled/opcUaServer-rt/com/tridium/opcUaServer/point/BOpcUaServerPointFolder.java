package com.tridium.opcUaServer.point;

import com.tridium.ndriver.discover.BINDiscoveryHost;
import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import com.tridium.ndriver.point.BNPointFolder;
import com.tridium.opcUaServer.BOpcUaNamespace;
import com.tridium.opcUaServer.BOpcUaServer;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentList;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "uaNodeId",
      type = "String",
      defaultValue = "",
      flags = 67
   ), @NiagaraProperty(
      name = "pointsFolderName",
      type = "String",
      defaultValue = ""
   )})
public class BOpcUaServerPointFolder extends BNPointFolder implements BINDiscoveryHost, BIOpcUaServerPointFolder {
   public static final Property uaNodeId = newProperty(67, "", null);
   public static final Property pointsFolderName = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BOpcUaServerPointFolder.class);
   private Logger logger = Logger.getLogger(BOpcUaServerPointFolder.class.getName());

   @Override
   public String getUaNodeId() {
      return this.getString(uaNodeId);
   }

   public void setUaNodeId(String v) {
      this.setString(uaNodeId, v, null);
   }

   public String getPointsFolderName() {
      return this.getString(pointsFolderName);
   }

   public void setPointsFolderName(String v) {
      this.setString(pointsFolderName, v, null);
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

   public void started() throws Exception {
      BOpcUaNamespace nodeSpace = this.getOpcUaServerDevice();
      AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
         String results = nodeSpace.addPointsFolder(this);
         if (!results.startsWith("*")) {
            this.setUaNodeId(results);
         } else {
            this.logger.log(Level.INFO, results);
         }

         return null;
      }));
      super.started();
   }

   public void stopped() throws Exception {
      if (!this.getUaNodeId().isEmpty()) {
         try {
            BOpcUaNamespace nodeSpace = (BOpcUaNamespace)this.getDevice();
            nodeSpace.deletePointsFolder(this);
         } catch (Exception var2) {
            this.logger.severe("Exception while getting Linkable Slots: " + var2);
         }
      }

      super.stopped();
   }

   public final AgentList getAgents(Context cx) {
      AgentList list = super.getAgents(cx);
      list.remove("ndriver:NPointManager");
      return list;
   }

   public BOrd submitDiscoveryJob(BNDiscoveryPreferences discoveryParams) {
      return this.getOpcUaServerDevice().getPoints().submitDiscoveryJob(discoveryParams);
   }

   public BNDiscoveryPreferences getDiscoveryPreferences() {
      return this.getOpcUaServerDevice().getPoints().getDiscoveryPreferences();
   }

   public BINDiscoveryObject[] getDiscoveryObjects(BNDiscoveryPreferences prefs) throws Exception {
      BOpcUaServerPointDeviceExt points = this.getOpcUaServerDevice().getPoints();
      return points.getDiscoveryObjects(points.getDiscoveryPreferences());
   }
}
