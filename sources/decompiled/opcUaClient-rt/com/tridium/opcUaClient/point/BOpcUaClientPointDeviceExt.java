package com.tridium.opcUaClient.point;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.UaClient;
import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import com.tridium.ndriver.point.BNPointDeviceExt;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.BOpcUaNetwork;
import com.tridium.opcUaClient.util.OpcUaClientUtil;
import com.tridium.opcUaClient.util.OpcUaSubscriptionAliveListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentList;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "discoveryPreferences",
      type = "NDiscoveryPreferences",
      defaultValue = "new BOpcUaClientPointDiscoveryPreferences()",
      override = true
   ), @NiagaraProperty(
      name = "discoveryFailCause",
      type = "String",
      defaultValue = "",
      flags = 7
   ), @NiagaraProperty(
      name = "publishInterval",
      type = "int",
      defaultValue = "500",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "0"
      ), @Facet(
         name = "BFacets.UNITS",
         value = "BUnit.getUnit(\"millisecond\")"
      )}
   ), @NiagaraProperty(
      name = "maxKeepAliveCount",
      type = "long",
      defaultValue = "1728000",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "1"
      )}
   )})
public class BOpcUaClientPointDeviceExt extends BNPointDeviceExt {
   public static final Property discoveryPreferences = newProperty(0, new BOpcUaClientPointDiscoveryPreferences(), null);
   public static final Property discoveryFailCause = newProperty(7, "", null);
   public static final Property publishInterval = newProperty(0, 500, BFacets.make(BFacets.make("min", 0), BFacets.make("units", BUnit.getUnit("millisecond"))));
   public static final Property maxKeepAliveCount = newProperty(0, 1728000, BFacets.make("min", 1));
   public static final Type TYPE = Sys.loadType(BOpcUaClientPointDeviceExt.class);
   private Subscription subscription;
   private static Logger logger = Logger.getLogger("opcUaClient.point");

   public String getDiscoveryFailCause() {
      return this.getString(discoveryFailCause);
   }

   public void setDiscoveryFailCause(String v) {
      this.setString(discoveryFailCause, v, null);
   }

   public int getPublishInterval() {
      return this.getInt(publishInterval);
   }

   public void setPublishInterval(int v) {
      this.setInt(publishInterval, v, null);
   }

   public long getMaxKeepAliveCount() {
      return this.getLong(maxKeepAliveCount);
   }

   public void setMaxKeepAliveCount(long v) {
      this.setLong(maxKeepAliveCount, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final BOpcUaNetwork getOpcUaClientNetwork() {
      return (BOpcUaNetwork)this.getNetwork();
   }

   public final BOpcUaDevice getOpcUaClientDevice() {
      return (BOpcUaDevice)this.getDevice();
   }

   public Type getDeviceType() {
      return BOpcUaDevice.TYPE;
   }

   public Type getPointFolderType() {
      return BOpcUaClientPointFolder.TYPE;
   }

   public Type getProxyExtType() {
      return BOpcUaClientProxyExt.TYPE;
   }

   public BINDiscoveryObject[] getDiscoveryObjects(BNDiscoveryPreferences prefs) throws Exception {
      boolean deviceNotRunning = this.getOpcUaClientDevice().isDisabled() || this.getOpcUaClientDevice().isFault() || this.getOpcUaClientDevice().isDown();
      boolean networkDown = this.getOpcUaClientNetwork().isDisabled() || this.getOpcUaClientNetwork().isFault() || this.getOpcUaClientNetwork().isDown();
      if (!deviceNotRunning && !networkDown) {
         this.getOpcUaClientDevice().doLearn(prefs.getJob());
         boolean showAddableOnly = prefs instanceof BOpcUaClientPointDiscoveryPreferences
            && ((BOpcUaClientPointDiscoveryPreferences)prefs).getShowAddableOnly();
         return this.getOpcUaClientDevice().getLearnedPoints(showAddableOnly);
      } else {
         prefs.getJob().log().message("Discovery job Failed.");
         throw new Exception("Device not Enabled.");
      }
   }

   public final AgentList getAgents(Context cx) {
      AgentList list = super.getAgents(cx);
      list.remove("ndriver:NPointManager");
      return list;
   }

   public void changed(Property property, Context context) {
      if (this.subscription != null && (property.equals(publishInterval) || property.equals(maxKeepAliveCount))) {
         try {
            this.subscription.setPublishingInterval(this.getPublishInterval());
            this.subscription.setMaxKeepAliveCount(this.getMaxKeepAliveCount());
            this.subscription.setLifetimeCount(this.getMaxKeepAliveCount() * 5L);
         } catch (Exception var4) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.SEVERE, "Exception while modifying subscription", (Throwable)var4);
            } else {
               logger.log(Level.SEVERE, "Exception while modifying subscription: " + var4);
            }
         }
      }

      super.changed(property, context);
   }

   public Subscription getSubscription() {
      if (this.subscription != null) {
         UaClient client = this.subscription.getClient();
         if (client == this.getOpcUaClientDevice().uaClient) {
            return this.subscription;
         }

         this.subscription = null;
      }

      this.subscription = new Subscription();

      try {
         this.subscription.setPublishingInterval(this.getPublishInterval());
         this.subscription.setMaxKeepAliveCount(this.getMaxKeepAliveCount());
         this.subscription.setLifetimeCount(this.getMaxKeepAliveCount() * 5L);
         this.subscription.addAliveListener(new OpcUaSubscriptionAliveListener());
         OpcUaClientUtil.addSubscription(this.getOpcUaClientDevice().uaClient, this.subscription);
      } catch (ServiceException var2) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception while adding a Subscription", (Throwable)var2);
         } else {
            logger.severe("Exception while adding a Subscription: " + var2);
         }
      }

      return this.subscription;
   }

   protected boolean useAutoManager() {
      return false;
   }
}
