package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.NAddressManager;
import com.tridium.lonworks.RouterManager;
import com.tridium.lonworks.util.RouterUtil;
import javax.baja.job.JobCancelException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonUpdateRoutersJob extends BLonNetmgmtJob {
   public static final Type TYPE = Sys.loadType(BLonUpdateRoutersJob.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonUpdateRoutersJob() {
   }

   public BLonUpdateRoutersJob(BLonNetmgmt netMgmt) {
      super(netMgmt);
   }

   @Override
   public void run() {
      boolean tmpBrdg = this.netMgmt.getTempBridge();

      try {
         if (!tmpBrdg) {
            RouterUtil.setTemporaryBridge(this.lon);
            this.netMgmt.setTempBridge(true);
         }

         this.doUpdate();
      } catch (JobCancelException var7) {
         this.canceled();
      } catch (Throwable var8) {
         this.fatal("Update routers failed ", var8);
      } finally {
         if (!tmpBrdg) {
            RouterUtil.clearTemporaryBridge(this.lon);
            this.netMgmt.setTempBridge(false);
         }
      }

      this.end();
   }

   private void doUpdate() {
      NAddressManager adMan = (NAddressManager)this.lon.addressManager();
      RouterManager rtrMan = adMan.routerManager();
      this.myProgress(2);
      BLonRouter[] routers = adMan.getRouterList();

      for (int i = 0; i < routers.length; i++) {
         try {
            RouterUtil.uploadRouterTables(routers[i]);
         } catch (Throwable var6) {
         }
      }

      this.myProgress(33);
      rtrMan.verifySubnets();
      this.myProgress(66);
      rtrMan.updateGroupRouteFlags();
   }
}
