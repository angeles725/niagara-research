package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.datatypes.BCommissionParameter;
import com.tridium.lonworks.device.DeviceFacets;
import com.tridium.lonworks.enums.BServicePinState;
import com.tridium.lonworks.netmessages.ServicePin;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.RouterUtil;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BAliasTable;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonReplaceJob extends BLonServicePinJob implements NetMgmtConst {
   public static final Type TYPE = Sys.loadType(BLonReplaceJob.class);
   private static final BIcon icon = BIcon.std("buildSave.png");
   private BLonDevice lonDevice;
   private BSubnetNode[] sns = null;
   private boolean servicePin;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonReplaceJob() {
   }

   public BLonReplaceJob(BLonNetmgmt netMgmt, BCommissionParameter param) {
      super(netMgmt);
      this.sns = param.getSubnetNodes();
      this.servicePin = param.getServicePin();
      BNeuronId nId = param.getNeuronId();
      if (!nId.equals(BNeuronId.DEFAULT)) {
         this.lonDevice = this.lon.addressManager().getDeviceByAddress(this.sns[0]);
         this.lonDevice.getDeviceData().setNeuronId(nId);
      }
   }

   @Override
   public void run() {
      boolean tmpBrdg = this.netMgmt.getTempBridge();

      try {
         if (!tmpBrdg) {
            RouterUtil.setTemporaryBridge(this.lon);
         }

         this.percentFactor = this.sns.length;

         for (int i = 0; i < this.sns.length; i++) {
            this.lonDevice = this.lon.addressManager().getDeviceByAddress(this.sns[i]);
            this.percentOffset = (int)(100.0 / this.percentFactor) * i;
            this.doReplace();
         }

         this.end();
      } catch (JobCancelException var7) {
         this.canceled();
      } catch (Throwable var8) {
         this.fatal("Fatal error ", var8);
      } finally {
         if (!tmpBrdg) {
            RouterUtil.clearTemporaryBridge(this.lon);
         }
      }
   }

   private void doReplace() {
      BDeviceData devData = this.lonDevice.getDeviceData();
      boolean isLocal = this.lonDevice.isLocal();
      this.log().start("Replace " + this.lonDevice.getDisplayName(null));

      try {
         this.lonDevice.beginCommission();
         BNeuronId neuronId = null;
         this.myProgress(2);
         BProgramId pgrmId;
         if (isLocal) {
            devData.setNeuronId(Neuron.getNeuronId(this.lonDevice));
            pgrmId = Neuron.getProgramId(this.lonDevice);
         } else {
            this.lonDevice.clearFiles();
            if (this.servicePin) {
               this.setServicePinState(BServicePinState.waiting);
               ServicePin srvpin = this.receiveServicePin();
               if (srvpin == null) {
                  return;
               }

               this.setServicePinState(BServicePinState.received);
               neuronId = srvpin.getNeuronId();
               pgrmId = srvpin.getIdString();
               this.validateNeuronId(neuronId, this.lonDevice);
            } else {
               Neuron.verifyAuthenticate(this.lonDevice);
               pgrmId = Neuron.getProgramId(this.lonDevice);
            }
         }

         BProgramId devPrgmId = devData.getProgramId();
         if (!this.lonDevice.programIdChanges() && !devPrgmId.isZero()) {
            if (!pgrmId.equals(devPrgmId)) {
               this.fatal("Program id does not match");
               return;
            }
         } else {
            devData.setProgramId(pgrmId);
         }

         if (neuronId != null) {
            devData.setNeuronId(neuronId);
         }

         if (!isLocal && this.servicePin) {
            Neuron.verifyAuthenticate(this.lonDevice);
         }

         NmUtil.setDeviceState(this.lonDevice, BLonNodeState.configOffline);
         devData.set(BDeviceData.nodeState, BLonNodeState.configOffline, AddressManager.noDeviceChange);
         NmUtil.setDeviceState(this.lonDevice, BLonNodeState.hardOffline);
         NmUtil.wait(10);
         NmUtil.setDeviceState(this.lonDevice, BLonNodeState.unconfigured);
         this.myProgress(10);
         NmUtil.updateDomainTable(this.netMgmt, this.lonDevice);
         this.myProgress(25);
         NmUtil.updateAddressTable(this.lonDevice);
         if (!isLocal) {
            this.updateNvConfig();
            this.updateAliasTable();
         }

         this.myProgress(60);
         Neuron.updateConfigData(this.netMgmt, this.lonDevice);
         NmUtil.clearStatus(this.lonDevice);
         DeviceFacets.delayToReset(this.lonDevice);
         NmUtil.resetNode(this.lonDevice);
         if (this.lonDevice.getDeviceData().getHosted()) {
            NmUtil.wait(3000);
         }

         this.myProgress(80);
         NmUtil.setDeviceState(this.lonDevice, BLonNodeState.configOnline);
         if (!DeviceFacets.disableToggleMode(this.lonDevice)) {
            NmUtil.setDeviceState(this.lonDevice, BLonNodeState.configOffline);
            NmUtil.setDeviceState(this.lonDevice, BLonNodeState.configOnline);
         }

         devData.set(BDeviceData.nodeState, BLonNodeState.configOnline, AddressManager.noDeviceChange);
         this.lonDevice.pingOk();
         this.lonDevice.postCommission();
         this.pass("Replace success");
      } catch (LonException var6) {
         this.error("Replace failed for " + this.lonDevice.getDisplayName(null), var6);
      }
   }

   private void updateNvConfig() throws LonException {
      boolean hosted = this.lonDevice.getDeviceData().getHosted();
      BINetworkVariable[] nvs = this.lonDevice.getNetworkVariables();
      int nvCount = this.getNvCount();

      for (int nvIndex = 0; nvIndex < nvCount; nvIndex++) {
         BINetworkVariable nv = nvIndex < nvs.length ? nvs[nvIndex] : null;
         BNvConfigData cfgDat;
         if (nv != null) {
            cfgDat = nv.getNvConfigData();
         } else {
            int sel = 16383 - nvIndex;

            try {
               cfgDat = NmUtil.queryNvConfigData(this.lonDevice, nvIndex);
            } catch (LonException var9) {
               return;
            }

            if (cfgDat.getSelector() == sel) {
               continue;
            }

            cfgDat.setUnbound(nvIndex);
         }

         NmUtil.updateNvConfig(this.lonDevice, nvIndex, cfgDat);
         if (hosted) {
            NmUtil.wait(100);
         }
      }
   }

   private void updateAliasTable() throws LonException {
      BAliasTable aliasTable = this.lonDevice.getDeviceData().getAliasTable();
      int aliasCnt = aliasTable.getAliasCount();
      if (aliasCnt != 0) {
         NmUtil.updateAliasTable(this.lonDevice);
      }
   }

   private int getNvCount() throws LonException {
      int ao = this.lonDevice.getDeviceData().getAliasTable().getAliasOffset();
      if (ao > 0) {
         return ao;
      } else {
         boolean ext = NmUtil.isExtended(this.lonDevice);
         return Neuron.getAliasOffset(this.lonDevice.lonComm(), NmUtil.getSendAddress(this.lonDevice), this.lonDevice.authenticate(), ext);
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
