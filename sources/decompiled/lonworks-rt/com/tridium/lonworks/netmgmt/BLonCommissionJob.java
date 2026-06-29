package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.NAddressManager;
import com.tridium.lonworks.datatypes.BCommissionParameter;
import com.tridium.lonworks.device.DeviceFacets;
import com.tridium.lonworks.enums.BServicePinState;
import com.tridium.lonworks.netmessages.ServicePin;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.RouterUtil;
import com.tridium.lonworks.util.selfdoc.SelfDoc;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BAliasTable;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonConfigSourceEnum;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonCommissionJob extends BLonServicePinJob implements NetMgmtConst {
   public static final Type TYPE = Sys.loadType(BLonCommissionJob.class);
   private static final BIcon icon = BIcon.std("build.png");
   private BLonDevice lonDevice;
   private BSubnetNode[] sns = null;
   private boolean servicePin = false;
   private boolean devArray = false;
   private int aliasCnt;
   private int nvCount;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonCommissionJob() {
   }

   public BLonCommissionJob(BLonNetmgmt netMgmt, BCommissionParameter param) {
      super(netMgmt);
      this.servicePin = param.getServicePin();
      this.sns = param.getSubnetNodes();
      this.devArray = true;
      BNeuronId nId = param.getNeuronId();
      if (nId != null && !nId.equals(BNeuronId.DEFAULT)) {
         this.lonDevice = this.lon.addressManager().getDeviceByAddress(this.sns[0]);
         this.lonDevice.getDeviceData().setNeuronId(nId);
      }
   }

   public static void commission(BLonNetmgmt netMgmt, BLonDevice dev) {
      BLonCommissionJob req = new BLonCommissionJob(netMgmt, dev);
      req.run();
   }

   private BLonCommissionJob(BLonNetmgmt netMgmt, BLonDevice dev) {
      super(netMgmt);
      this.lonDevice = dev;
      this.servicePin = false;
      this.devArray = false;
   }

   @Override
   public void run() {
      boolean tmpBrdg = this.netMgmt.getTempBridge();

      try {
         if (!tmpBrdg) {
            RouterUtil.setTemporaryBridge(this.lon);
         }

         if (this.devArray) {
            this.percentFactor = this.sns.length;

            for (int i = 0; i < this.sns.length; i++) {
               this.lonDevice = this.lon.addressManager().getDeviceByAddress(this.sns[i]);
               this.percentOffset = (int)(100.0 / this.percentFactor) * i;
               this.doCommission();
            }
         } else {
            this.doCommission();
         }
      } catch (JobCancelException var7) {
         this.canceled();
      } catch (Throwable var8) {
         this.fatal("Fatal error ", var8);
      } finally {
         if (!tmpBrdg) {
            RouterUtil.clearTemporaryBridge(this.lon);
         }
      }

      this.end();
   }

   private void doCommission() {
      boolean isLocal = this.lonDevice.isLocal();
      BDeviceData dd = this.lonDevice.getDeviceData();
      NAddressManager adMan = (NAddressManager)this.lon.addressManager();
      this.log().start("Commissioning " + this.lonDevice.getDisplayName(null));

      try {
         this.lonDevice.beginCommission();
         if (adMan.isRouted()) {
            adMan.routerManager().verifySubnets();
         }

         BNeuronId neuronId = null;
         this.myProgress(2);
         BProgramId pgrmId;
         if (isLocal) {
            dd.setNeuronId(Neuron.getNeuronId(this.lonDevice));
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

         BProgramId devPrgmId = dd.getProgramId();
         if (!this.lonDevice.programIdChanges() && !devPrgmId.isZero()) {
            if (!pgrmId.equals(devPrgmId)) {
               this.fatal("Program id does not match");
               return;
            }
         } else {
            dd.setProgramId(pgrmId);
         }

         if (neuronId != null) {
            dd.setNeuronId(neuronId);
         }

         if (!isLocal) {
            if (this.servicePin) {
               Neuron.verifyAuthenticate(this.lonDevice);
            }

            if (!this.lonDevice.isExtended() && NmUtil.isExtended(this.lonDevice)) {
               this.fatal("Device requires extended netmgmt support. Import updated *.lnml or execute learnNv.");
               return;
            }

            NmUtil.setDeviceState(this.lonDevice, BLonNodeState.configOffline);
            dd.set(BDeviceData.nodeState, BLonNodeState.configOffline, AddressManager.noDeviceChange);
            DeviceFacets.delayToHardOffline(this.lonDevice);
            NmUtil.setDeviceState(this.lonDevice, BLonNodeState.hardOffline);
            NmUtil.wait(10);
            NmUtil.setDeviceState(this.lonDevice, BLonNodeState.unconfigured);
         }

         this.myProgress(10);
         Neuron.uploadDeviceData(this.lonDevice);
         NmUtil.updateDomainTable(this.netMgmt, this.lonDevice);
         this.myProgress(25);
         NmUtil.clearAddressTable(this.lonDevice);
         if (!isLocal) {
            this.getCounts();
            this.verifyAndUnbindNv();
            this.clearAliasTable();
         }

         this.myProgress(60);
         Neuron.updateConfigData(this.netMgmt, this.lonDevice);
         if (!isLocal) {
            NmUtil.setConfigSrc(this.lonDevice, BLonConfigSourceEnum.cfgExternal);
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
         }

         dd.set(BDeviceData.nodeState, BLonNodeState.configOnline, AddressManager.noDeviceChange);
         this.lonDevice.pingOk();
         this.lonDevice.postCommission();
         this.pass("Commission success");
      } catch (LonException var7) {
         this.fatal("Commission failed for " + this.lonDevice.getDisplayName(null), var7);
      }
   }

   private void verifyAndUnbindNv() throws LonException {
      BINetworkVariable[] nvs = this.lonDevice.getNetworkVariables();
      if (this.lonDevice.isExtended()) {
         NmUtil.extNvInitialize(this.lonDevice, 0, 65535);

         for (int i = 0; i < nvs.length; i++) {
            if (nvs[i] != null) {
               nvs[i].setUnbound();
            }
         }
      } else {
         for (int nvIndex = 0; nvIndex < this.nvCount; nvIndex++) {
            boolean configChanged = false;

            BNvConfigData deviceConfig;
            try {
               deviceConfig = NmUtil.queryNvConfigData(this.lonDevice, nvIndex);
            } catch (LonException var9) {
               return;
            }

            BINetworkVariable nv = nvIndex < nvs.length ? nvs[nvIndex] : null;
            BNvConfigData newConfig;
            if (nv != null) {
               BNvConfigData dbConfig = nv.getNvConfigData();
               if (deviceConfig.getDirection() != dbConfig.getDirection()) {
                  String err = "BINetworkVariable direction mismatch for " + ((BComponent)nv).getDisplayName(null) + "(" + nvIndex + ")";
                  if (this.netMgmt.getVerifyNvDir()) {
                     throw new LonException(err);
                  }

                  this.log().message(err);
               }

               nvs[nvIndex].setUnbound();
               if (!dbConfig.equivalent(deviceConfig)) {
                  configChanged = true;
               }

               newConfig = dbConfig;
            } else {
               newConfig = (BNvConfigData)deviceConfig.newCopy(true);
               newConfig.setUnbound(nvIndex);
               if (!deviceConfig.equivalent(newConfig)) {
                  configChanged = true;
               }
            }

            if (configChanged) {
               NmUtil.updateNvConfig(this.lonDevice, nvIndex, newConfig);
            }
         }
      }
   }

   private void clearAliasTable() throws LonException {
      if (this.lonDevice.isExtended()) {
         NmUtil.extAliasInitialize(this.lonDevice, 0, 65535);
      } else {
         BAliasTable aliasTable = this.lonDevice.getDeviceData().getAliasTable();
         aliasTable.setAliasCount(this.aliasCnt);
         if (this.aliasCnt != 0) {
            aliasTable.setAliasOffset(this.nvCount);
            aliasTable.clearTable();

            try {
               NmUtil.updateAliasTable(this.lonDevice);
            } catch (FailedResponseException var3) {
               System.out.println(var3);
            }
         }
      }
   }

   private void getCounts() throws LonException {
      if (!this.lonDevice.getDeviceData().getHosted()) {
         this.nvCount = Neuron.getNvCount(this.lonDevice);
         this.aliasCnt = Neuron.getAliasCount(this.lonDevice);
      } else if (!this.lonDevice.isExtended()) {
         SelfDoc sdoc = new SelfDoc(
            this.lonDevice.lonComm(), NmUtil.getSendAddress(this.lonDevice), this.lonDevice.authenticate(), true, this.lonDevice.isExtended()
         );
         this.aliasCnt = sdoc.getAliasCnt();
         this.nvCount = sdoc.getAliasOffset();
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
