package com.tridium.opcUaClient;

import com.tridium.ndriver.BNNetwork;
import com.tridium.ndriver.discover.BINDiscoveryHost;
import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import com.tridium.opcUaClient.learn.BOpcUaClientDeviceDiscoveryPreferences;
import com.tridium.opcUaClient.learn.BOpcUaClientLearnDevicesJob;
import javax.baja.agent.AgentList;
import javax.baja.job.BJobService;
import javax.baja.license.Feature;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;
import javax.baja.units.UnitDatabase;
import javax.baja.units.UnitDatabase.Quantity;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperty(
   name = "discoveryPreferences",
   type = "BNDiscoveryPreferences",
   defaultValue = "new BOpcUaClientDeviceDiscoveryPreferences()"
)
@NiagaraActions({@NiagaraAction(
      name = "submitDiscoveryJob",
      parameterType = "BNDiscoveryPreferences",
      defaultValue = "new BNDiscoveryPreferences()",
      returnType = "BOrd"
   ), @NiagaraAction(
      name = "dumpUnits"
   )})
public class BOpcUaNetwork extends BNNetwork implements BINDiscoveryHost {
   public static final Property discoveryPreferences = newProperty(0, new BOpcUaClientDeviceDiscoveryPreferences(), null);
   public static final Action submitDiscoveryJob = newAction(0, new BNDiscoveryPreferences(), null);
   public static final Action dumpUnits = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BOpcUaNetwork.class);
   boolean debug = true;
   public static final Lexicon lex = Lexicon.make(BOpcUaNetwork.class);

   public BNDiscoveryPreferences getDiscoveryPreferences() {
      return (BNDiscoveryPreferences)this.get(discoveryPreferences);
   }

   public void setDiscoveryPreferences(BNDiscoveryPreferences v) {
      this.set(discoveryPreferences, v, null);
   }

   public BOrd submitDiscoveryJob(BNDiscoveryPreferences parameter) {
      return (BOrd)this.invoke(submitDiscoveryJob, parameter, null);
   }

   public void dumpUnits() {
      this.invoke(dumpUnits, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final Feature getLicenseFeature() {
      return Sys.getLicenseManager().getFeature("tridium", "opcUaClient");
   }

   public String getNetworkName() {
      return "OpcUaClientNetwork";
   }

   public Type getDeviceFolderType() {
      return BOpcUaDeviceFolder.TYPE;
   }

   public Type getDeviceType() {
      return BOpcUaDevice.TYPE;
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         ;
      }
   }

   public final AgentList getAgents(Context cx) {
      AgentList list = super.getAgents(cx);
      list.remove("ndriver:NDeviceManager");
      return list;
   }

   public BINDiscoveryObject[] getDiscoveryObjects(BNDiscoveryPreferences prefs) throws Exception {
      return new BINDiscoveryObject[0];
   }

   public BOrd doSubmitDiscoveryJob(BNDiscoveryPreferences parameter) {
      BComponent service = Sys.getService(BJobService.TYPE);
      return service == null ? BOrd.NULL : ((BJobService)service).submit(new BOpcUaClientLearnDevicesJob(), null);
   }

   public void doDumpUnits() {
      UnitDatabase unitDatabase = UnitDatabase.getDefault();
      Quantity[] q = unitDatabase.getQuantities();
      int c = 0;

      for (Quantity aQ : q) {
         System.out.println("// quantity: " + aQ.getName());
         BUnit[] u = aQ.getUnits();

         for (BUnit anU : u) {
            System.out.println("   new UnitId2Unit(" + c++ + ", \"" + anU.getUnitName() + "\"),// " + anU.getSymbol());
         }
      }
   }

   protected boolean useAutoManager() {
      return false;
   }
}
