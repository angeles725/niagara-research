package com.tridium.opcUaClient.history;

import com.tridium.ndriver.discover.BNDiscoveryJob;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.point.BOpcUaNodeLearnEntry;
import java.util.ArrayList;
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
public class BOpcUaClientDiscoverHistoriesJob extends BNDiscoveryJob {
   public static final Property learnedPoints = newProperty(7, new BFolder(), null);
   public static final Type TYPE = Sys.loadType(BOpcUaClientDiscoverHistoriesJob.class);
   final BOpcUaClientHistoryDeviceExt histories;
   public static final Logger logger = Logger.getLogger("opcUaClient.history");

   public BFolder getLearnedPoints() {
      return (BFolder)this.get(learnedPoints);
   }

   public void setLearnedPoints(BFolder v) {
      this.set(learnedPoints, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcUaClientDiscoverHistoriesJob() {
      this.histories = null;
   }

   public BOpcUaClientDiscoverHistoriesJob(BOpcUaClientHistoryDeviceExt histories) {
      super(histories);
      this.histories = histories;
   }

   public void run(Context cx) {
      this.log().message("starting HistoryDiscovery Job...");

      try {
         BOpcUaDevice device = this.histories.device();
         ArrayList<BOpcUaNodeLearnEntry> historizingItems = this.histories.getHistorizingItems();
         if (historizingItems == null) {
            device.doLearn(this);
            historizingItems = this.histories.getHistorizingItems();
         }

         for (BOpcUaNodeLearnEntry item : historizingItems) {
            this.getLearnedPoints().add("histItem?", item.newCopy(true));
         }
      } catch (Exception var6) {
         logger.severe("exception received in LearnAsdPointsRequest - " + var6);
      }
   }
}
