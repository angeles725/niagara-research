package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.NAddressManager;
import com.tridium.lonworks.RouterManager;
import com.tridium.lonworks.datatypes.BCommissionParameter;
import com.tridium.lonworks.enums.BLonRouterMode;
import com.tridium.lonworks.enums.BServicePinState;
import com.tridium.lonworks.netmessages.ServicePin;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.RouterUtil;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonCommissionRouterJob extends BLonServicePinJob implements NetMgmtConst {
   public static final Type TYPE = Sys.loadType(BLonCommissionRouterJob.class);
   private static final BIcon icon = BIcon.std("build.png");
   private BLonRouter router;
   private BSubnetNode[] sns = null;
   private boolean servicePin;
   private BNeuronId nId;
   private boolean tmpBrdg;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonCommissionRouterJob() {
   }

   public BLonCommissionRouterJob(BLonNetmgmt netMgmt, BCommissionParameter param) {
      super(netMgmt);
      this.sns = param.getSubnetNodes();
      this.servicePin = param.getServicePin();
      this.nId = param.getNeuronId();
   }

   @Override
   public void run() {
      this.tmpBrdg = this.netMgmt.getTempBridge();

      try {
         if (!this.tmpBrdg) {
            RouterUtil.setTemporaryBridge(this.lon);
         }

         this.percentFactor = this.sns.length;

         for (int i = 0; i < this.sns.length; i++) {
            this.router = this.lon.addressManager().getRouterByAddress(this.sns[i]);
            if (this.router == null) {
               throw new LonException("Router with s/n " + this.sns[i] + " has invalid subnet channelId configuration.");
            }

            if (!this.nId.equals(BNeuronId.DEFAULT)) {
               this.router.getNearDeviceData().setNeuronId(this.nId);
            }

            this.percentOffset = (int)(100.0 / this.percentFactor) * i;
            this.doCommission();
         }
      } catch (JobCancelException var6) {
         this.canceled();
      } catch (Throwable var7) {
         this.fatal("Fatal error ", var7);
      } finally {
         if (!this.tmpBrdg) {
            RouterUtil.clearTemporaryBridge(this.lon);
         }
      }

      this.end();
   }

   private void doCommission() {
      BDeviceData nearDevData = this.router.getNearDeviceData();
      BDeviceData farDevData = this.router.getFarDeviceData();
      this.log().start("Commissioning " + this.router.getDisplayName(null));

      try {
         BNeuronId neuronId = null;
         this.myProgress(2);
         BProgramId pgrmId;
         if (this.servicePin) {
            this.setServicePinState(BServicePinState.waiting);
            ServicePin srvpin = this.receiveServicePin();
            if (srvpin == null) {
               return;
            }

            this.setServicePinState(BServicePinState.received);
            neuronId = srvpin.getNeuronId();
            pgrmId = srvpin.getIdString();
            this.validateNeuronId(neuronId, this.router);
         } else {
            pgrmId = Neuron.getProgramId(this.router, false);
         }

         if (!pgrmId.isRouter()) {
            this.fatal("Device is not a router - neuronId " + neuronId);
            return;
         }

         BProgramId devPrgmId = nearDevData.getProgramId();
         if (!pgrmId.equals(devPrgmId)) {
            nearDevData.setProgramId(pgrmId);
            farDevData.setProgramId(pgrmId);
         }

         this.myProgress(10);
         if (neuronId != null) {
            nearDevData.setNeuronId(neuronId);
         }

         farDevData.setNeuronId(Neuron.getNeuronId(this.router, true));
         NmUtil.setDeviceState(this.router, BLonNodeState.configOffline, false);
         NmUtil.setDeviceState(this.router, BLonNodeState.configOffline, true);
         nearDevData.set(BDeviceData.nodeState, BLonNodeState.configOffline, AddressManager.noDeviceChange);
         farDevData.set(BDeviceData.nodeState, BLonNodeState.configOffline, AddressManager.noDeviceChange);
         NmUtil.setDeviceState(this.router, BLonNodeState.hardOffline, false);
         NmUtil.setDeviceState(this.router, BLonNodeState.hardOffline, true);
         this.myProgress(40);
         RouterUtil.updateDomainTable(this.netMgmt, this.router);
         Neuron.updateConfigData(this.netMgmt, this.router, false);
         Neuron.updateConfigData(this.netMgmt, this.router, true);
         this.myProgress(60);
         RouterUtil.setRouterType(this.router, this.router.getRouterType());
         RouterManager rtrMng = ((NAddressManager)this.lon.addressManager()).routerManager();
         rtrMng.updateRouteTables(this.router);
         RouterUtil.downloadRouterTables(this.router);
         if (!this.tmpBrdg) {
            RouterUtil.setRouterMode(this.router, BLonRouterMode.normal);
         }

         NmUtil.resetNode(this.router);
         this.myProgress(80);
         NmUtil.clearStatus(this.router);
         NmUtil.setDeviceState(this.router, BLonNodeState.configOnline, false);
         NmUtil.setDeviceState(this.router, BLonNodeState.configOnline, true);
         nearDevData.set(BDeviceData.nodeState, BLonNodeState.configOnline, AddressManager.noDeviceChange);
         farDevData.set(BDeviceData.nodeState, BLonNodeState.configOnline, AddressManager.noDeviceChange);
         rtrMng.verifySubnets();
         this.pass("Commission success");
      } catch (LonException var7) {
         this.error("Commission failed for " + this.router.getDisplayName(null), var7);
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
