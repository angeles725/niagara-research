package com.tridium.opcUaClient.learn;

import com.prosysopc.ua.UaAddress;
import com.prosysopc.ua.UaApplication.Protocol;
import com.prosysopc.ua.client.ServerList;
import com.prosysopc.ua.client.ServerListException;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.stack.core.ApplicationDescription;
import com.prosysopc.ua.stack.core.EndpointDescription;
import com.tridium.opcUaClient.BOpcUaNetwork;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.job.BSimpleJob;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFolder;

@NiagaraType
@NiagaraProperty(
   name = "learnedDevices",
   type = "BFolder",
   defaultValue = "new BFolder()",
   flags = 7
)
public class BOpcUaClientLearnDevicesJob extends BSimpleJob {
   public static final Property learnedDevices = newProperty(7, new BFolder(), null);
   public static final Type TYPE = Sys.loadType(BOpcUaClientLearnDevicesJob.class);
   final BOpcUaNetwork network;
   public static final Logger logger = Logger.getLogger("opcUaClient.learn");

   public BFolder getLearnedDevices() {
      return (BFolder)this.get(learnedDevices);
   }

   public void setLearnedDevices(BFolder v) {
      this.set(learnedDevices, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcUaClientLearnDevicesJob() {
      this.network = null;
   }

   public BOpcUaClientLearnDevicesJob(BOpcUaNetwork net) {
      this.network = net;
   }

   void addLearnedDevice(EndpointDescription ep) {
      String learnName = "ep?";
      if (this.getLearnedDevices().get(learnName) == null) {
         this.getLearnedDevices().add(learnName, new BOpcUaClientLearnDeviceEntry(ep));
         this.logMessage("found device " + learnName);
      }
   }

   void removeLearnedDevice(int address) {
      String learnName = "device" + address;
      if (this.getLearnedDevices().get(learnName) != null) {
         this.getLearnedDevices().remove(learnName);
      }
   }

   public void run(Context cx) throws Exception {
      this.logMessage("starting");
      List<EndpointDescription> edList = new ArrayList<>();
      ServerList serverList = new ServerList();

      try {
         serverList.discover(Protocol.OpcTcp, "localHost");
         ApplicationDescription[] servers = serverList.getServers();

         for (ApplicationDescription server : servers) {
            logger.info("Server Application name: " + server.getApplicationName().getText());
            this.discoverEndpoints(server, edList);
         }

         for (EndpointDescription endpointDescription : edList) {
            this.addLearnedDevice(endpointDescription);
         }
      } catch (URISyntaxException | ServerListException var9) {
         logger.info("Exception while adding a learned device: " + var9);
      }
   }

   protected void discoverEndpoints(ApplicationDescription serverApp, List<EndpointDescription> edList) throws URISyntaxException {
      String[] discoveryUrls = serverApp.getDiscoveryUrls();
      if (discoveryUrls != null) {
         UaClient discoveryClient = new UaClient();
         int i = 0;
         logger.info("Available endpoints: ");
         logger.info(String.format("%s - %-50s - %-20s - %-20s - %s", "#", "URI", "Security Mode", "Security Policy", "Transport Profile"));

         for (String url : discoveryUrls) {
            try {
               discoveryClient.setAddress(UaAddress.parse(url));
            } catch (IllegalArgumentException var14) {
               logger.info("URL is not valid " + url + ": " + var14);
            }

            try {
               for (EndpointDescription ed : discoveryClient.discoverEndpoints()) {
                  logger.info(
                     String.format(
                        "%s - %-50s - %-20s - %-20s - %s",
                        i++,
                        ed.getEndpointUrl(),
                        ed.getSecurityMode(),
                        ed.getSecurityPolicyUri().replaceFirst("http://opcfoundation\\.org/UA/SecurityPolicy#", ""),
                        ed.getTransportProfileUri().replaceFirst("http://opcfoundation\\.org/UA-Profile/Transport/", "")
                     )
                  );
                  edList.add(ed);
               }
            } catch (Exception var15) {
               logger.info("Cannot discover Endpoints from URL " + url + ": " + var15.getMessage());
            }
         }
      } else {
         logger.info("No suitable discoveryUrl available: using the current Url");
      }
   }

   private void logMessage(String message) {
      this.log().message(message);
      if (this.network != null && this.network.getLogger().isLoggable(Level.FINE)) {
         this.network.getLogger().log(Level.FINE, "Learn Access Devices Job: " + message);
      }
   }
}
