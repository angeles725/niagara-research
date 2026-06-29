package com.tridium.opcUaClient.alarm;

import com.tridium.ndriver.discover.BNDiscoveryJob;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.point.BOpcUaNodeLearnEntry;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFolder;

@NiagaraType
@NiagaraProperty(
   name = "learnedPoints",
   type = "BFolder",
   defaultValue = "new BFolder()",
   flags = 7
)
public class BOpcUaClientDiscoverAlarmsJob extends BNDiscoveryJob {
   public static final Property learnedPoints = newProperty(7, new BFolder(), null);
   public static final Type TYPE = Sys.loadType(BOpcUaClientDiscoverAlarmsJob.class);
   private final BOpcUaClientAlarmDeviceExt alarmSources;
   private static Logger logger = Logger.getLogger("opcUaClient.alarm");

   public BFolder getLearnedPoints() {
      return (BFolder)this.get(learnedPoints);
   }

   public void setLearnedPoints(BFolder v) {
      this.set(learnedPoints, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcUaClientDiscoverAlarmsJob() {
      this.alarmSources = null;
   }

   public BOpcUaClientDiscoverAlarmsJob(BOpcUaClientAlarmDeviceExt alarmSources) {
      super(alarmSources);
      this.alarmSources = alarmSources;
   }

   public void run(Context cx) {
      this.log().message("starting");

      try {
         BOpcUaDevice device = (BOpcUaDevice)this.alarmSources.getDevice();
         ArrayList<BOpcUaNodeLearnEntry> alarmingItems = this.alarmSources.getAlarmItems();
         if (alarmingItems == null) {
            device.doLearn(this);
            alarmingItems = this.alarmSources.getAlarmItems();
         }

         for (BOpcUaNodeLearnEntry item : alarmingItems) {
            this.getLearnedPoints().add("alarmItem?", item.newCopy(true));
         }
      } catch (Exception var6) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Exception while discovering Alarm: " + var6);
         }
      }
   }
}
