package com.tridium.opcUaServer.learn;

import com.tridium.opcUaServer.BOpcUaServer;
import java.util.logging.Level;
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
public class BOpcUaServerLearnDevicesJob extends BSimpleJob {
   public static final Property learnedDevices = newProperty(7, new BFolder(), null);
   public static final Type TYPE = Sys.loadType(BOpcUaServerLearnDevicesJob.class);
   final BOpcUaServer network;

   public BFolder getLearnedDevices() {
      return (BFolder)this.get(learnedDevices);
   }

   public void setLearnedDevices(BFolder v) {
      this.set(learnedDevices, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcUaServerLearnDevicesJob() {
      this.network = null;
   }

   public BOpcUaServerLearnDevicesJob(BOpcUaServer net) {
      this.network = net;
   }

   void addLearnedDevice(String address) {
      String learnName = "device" + address;
      if (this.getLearnedDevices().get(learnName) == null) {
         this.getLearnedDevices().add(learnName, new BOpcUaServerLearnDeviceEntry(address));
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
   }

   private void logMessage(String message) {
      this.log().message(message);
      if (this.network != null && this.network.getLogger().isLoggable(Level.FINE)) {
         this.network.getLogger().log(Level.FINE, "Learn Access Devices Job: " + message);
      }
   }
}
