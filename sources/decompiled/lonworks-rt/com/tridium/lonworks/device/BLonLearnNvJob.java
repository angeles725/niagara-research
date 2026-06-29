package com.tridium.lonworks.device;

import com.tridium.lonworks.netmgmt.BLonNetmgmtJob;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.selfdoc.DocToXDevice;
import com.tridium.lonworks.xml.XLonDevice;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.BDynamicDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.datatypes.BLearnNvParameters;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonLearnNvJob extends BLonNetmgmtJob {
   public static final Type TYPE = Sys.loadType(BLonLearnNvJob.class);
   private static final BIcon icon = BIcon.std("build.png");
   private BDynamicDevice dev;
   private BLearnNvParameters param;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonLearnNvJob() {
   }

   public BLonLearnNvJob(BLonNetwork lonworks, BDynamicDevice dev, BLearnNvParameters param) {
      super(lonworks);
      this.dev = dev;
      this.param = param;
   }

   @Override
   public void run() {
      try {
         XLonDevice xdev = DocToXDevice.extract(this.dev.getNeuronIdAddress(), this.lon.lonComm(), this.dev.authenticate());
         DynaDev.importXLon(this.dev, xdev, this.param.getUseLonObjects());
         NmUtil.setWorkingDomain(this.dev, this.lon.getLonNetmgmt());
      } catch (JobCancelException var2) {
         this.canceled();
      } catch (Throwable var3) {
         this.fatal("Fatal error ", var3);
      }

      this.end();
   }

   public BIcon getIcon() {
      return icon;
   }
}
