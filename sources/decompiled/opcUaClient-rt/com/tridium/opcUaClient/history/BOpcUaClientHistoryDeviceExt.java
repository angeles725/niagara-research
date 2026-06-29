package com.tridium.opcUaClient.history;

import com.tridium.ndriver.discover.BINDiscoveryHost;
import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.point.BOpcUaNodeLearnEntry;
import com.tridium.util.CompUtil;
import java.util.ArrayList;
import javax.baja.driver.history.BHistoryDeviceExt;
import javax.baja.log.Log;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraAction(
   name = "submitTrendLogDiscoveryJob",
   returnType = "BOrd",
   flags = 4
)
public class BOpcUaClientHistoryDeviceExt extends BHistoryDeviceExt implements BINDiscoveryHost {
   public static final Action submitTrendLogDiscoveryJob = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BOpcUaClientHistoryDeviceExt.class);
   public static final Log log = Log.getLog("opcUaClient.history");

   public BOrd submitTrendLogDiscoveryJob() {
      return (BOrd)this.invoke(submitTrendLogDiscoveryJob, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BOpcUaDevice;
   }

   public void added(Property p, Context c) {
      super.added(p, c);
      if (this.isRunning()) {
         BObject o = this.get(p);
         if (o instanceof BOpcUaClientHistoryImport) {
            ((BOpcUaClientHistoryImport)o).execute();
         }
      }
   }

   public BOrd doSubmitTrendLogDiscoveryJob(Context cx) {
      BStatus status = this.device().getStatus();
      return !status.isDisabled() && !status.isDown() ? new BOpcUaClientDiscoverHistoriesJob(this).submit(cx) : null;
   }

   public final BOpcUaDevice device() {
      return (BOpcUaDevice)this.getDevice();
   }

   public Type getImportDescriptorType() {
      return BOpcUaClientHistoryImport.TYPE;
   }

   public Type getExportDescriptorType() {
      return null;
   }

   public ArrayList<BOpcUaNodeLearnEntry> getHistorizingItems() {
      ArrayList<BOpcUaNodeLearnEntry> list = new ArrayList<>();
      BValue bValue = this.device().get("serverRoot");
      if (bValue == null) {
         return null;
      } else {
         BComponent learnRoot = bValue.asComponent();
         BOpcUaNodeLearnEntry[] descendants = (BOpcUaNodeLearnEntry[])CompUtil.getDescendants(learnRoot, BOpcUaNodeLearnEntry.class);

         for (BOpcUaNodeLearnEntry descendant : descendants) {
            if (descendant.getHistorizing()) {
               list.add((BOpcUaNodeLearnEntry)descendant.newCopy(true));
            }
         }

         return list;
      }
   }

   public BOrd submitDiscoveryJob(BNDiscoveryPreferences discoveryParams) {
      return null;
   }

   public BNDiscoveryPreferences getDiscoveryPreferences() {
      return new BOpcUaClientHistoryDiscoveryPreferences();
   }

   public BINDiscoveryObject[] getDiscoveryObjects(BNDiscoveryPreferences prefs) throws Exception {
      ArrayList<BOpcUaNodeLearnEntry> items = this.getHistorizingItems();
      if (items == null) {
         ((BOpcUaDevice)this.getDevice()).doLearn();
         items = this.getHistorizingItems();
      }

      return items.toArray(new BOpcUaNodeLearnEntry[0]);
   }
}
