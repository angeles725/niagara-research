package com.tridium.opcUaServer.point;

import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.discover.BNDiscoveryJob;
import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import javax.baja.job.BJobState;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.BComponent;
import javax.baja.sys.BModule;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BUnrestrictedFolder;
import javax.baja.util.Lexicon;

@NiagaraType
public class BOpcUaServerPointDiscoveryJob extends BNDiscoveryJob {
   public static final Type TYPE = Sys.loadType(BOpcUaServerPointDiscoveryJob.class);
   public static final Lexicon ND_LEX = Lexicon.make(BNDiscoveryJob.class);
   public static final Lexicon NCD_LEX = Lexicon.make("opcUaServer");
   private Exception jobException;
   private BOpcUaServerPointDiscoveryJob.OpcUaPtDiscJobThread thread;
   private BOpcUaServerPointDeviceExt pointDeviceExt;
   public static final BINDiscoveryObject[] EMPTY_DISCOVERY_OBJECTS = new BINDiscoveryObject[0];

   public Type getType() {
      return TYPE;
   }

   public BOpcUaServerPointDiscoveryJob() {
   }

   public BOpcUaServerPointDiscoveryJob(BOpcUaServerPointDeviceExt pointDeviceExt) {
      super(pointDeviceExt);
      this.pointDeviceExt = pointDeviceExt;
   }

   public void doRun(Context cx) {
      this.thread = new BOpcUaServerPointDiscoveryJob.OpcUaPtDiscJobThread(this.toPathString(), cx);
      this.thread.start();
   }

   public void doCancel(Context cx) {
      if (this.getJobState().isRunning()) {
         this.setJobState(BJobState.canceling);
         if (this.thread != null) {
            this.thread.interrupt();
         }
      }
   }

   public String toString(Context cx) {
      BModule mod = this.getDiscoveryPreferences().getType().getModule();
      String lexKey = this.getDiscoveryPreferences().getType().getTypeName() + ".discovery.jobBar.name";
      String driverName = mod.getLexicon().get(lexKey);
      if (driverName == null || driverName.isEmpty()) {
         driverName = mod.getModuleName();
         driverName = TextUtil.toFriendly(driverName) + ' ' + ND_LEX.getText("Discovery");
      }

      return driverName;
   }

   protected void end() {
      if (this.jobException != null) {
         this.failed(this.jobException);
      } else if (this.isCanceled()) {
         this.canceled();
      } else {
         this.success();
      }
   }

   public boolean isCanceled() {
      return this.getJobState() == BJobState.canceling || this.getJobState() == BJobState.canceled;
   }

   public void discoverFail(String reason) {
      this.log().message(reason);
   }

   public void discoverOk(BINDiscoveryObject[] discoveryObjects) {
      BUnrestrictedFolder discovered = new BUnrestrictedFolder();
      if (discoveryObjects != null) {
         for (int i = 0; i < discoveryObjects.length; i++) {
            discovered.add("d" + i, (BValue)discoveryObjects[i]);
         }

         this.add(NCD_LEX.getText("discoveredPoints"), discovered);
      }
   }

   public BINDiscoveryObject[] getRootDiscoveryObjects() {
      BValue dfVal = this.get(NCD_LEX.getText("discoveredPoints"));
      if (dfVal != null) {
         BComponent df = dfVal.asComponent();
         return (BINDiscoveryObject[])df.getChildren(BINDiscoveryObject.class);
      } else {
         return EMPTY_DISCOVERY_OBJECTS;
      }
   }

   public BComponent discoveryFolder() {
      return this;
   }

   class OpcUaPtDiscJobThread extends Thread {
      Context cx;

      OpcUaPtDiscJobThread(String name, Context cx) {
         super(name);
         this.cx = cx;
      }

      @Override
      public void run() {
         BOpcUaServerPointDiscoveryJob.this.clearRootDiscoveryObjects();
         BOpcUaServerPointDiscoveryJob.this.log().start("opcUaServer", "point.discover.start", null);

         try {
            BNDiscoveryPreferences prefs = BOpcUaServerPointDiscoveryJob.this.getDiscoveryPreferences();
            boolean done = false;

            while (!done) {
               BINDiscoveryObject[] discoveryObjects = BOpcUaServerPointDiscoveryJob.this.pointDeviceExt.getDiscoveryObjects(prefs);
               BOpcUaServerPointDiscoveryJob.this.progress(50);
               BOpcUaServerPointDiscoveryJob.this.log().message("opcUaServer", "point.discover.discovered");
               BOpcUaServerPointDiscoveryJob.this.discoverOk(discoveryObjects);
               BOpcUaServerPointDiscoveryJob.this.progress(90);
               BOpcUaServerPointDiscoveryJob.this.log().message("opcUaServer", "point.discover.added");
               if (!prefs.isMultiStep() || discoveryObjects == null || !BOpcUaServerPointDiscoveryJob.this.getJobState().isRunning()) {
                  done = true;
               }
            }
         } catch (Exception var4) {
            BOpcUaServerPointDiscoveryJob.this.jobException = var4;
         }

         BOpcUaServerPointDiscoveryJob.this.end();
      }
   }
}
