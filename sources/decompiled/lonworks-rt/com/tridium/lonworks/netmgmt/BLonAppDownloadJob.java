package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.datatypes.BAppDownloadParameter;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBlob;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonAppDownloadJob extends BLonNetmgmtJob implements NetMgmtConst {
   public static final Type TYPE = Sys.loadType(BLonAppDownloadJob.class);
   private BLonDevice dev;
   private BAppDownloadParameter param;
   private BLonAppDownloadJob.NxeRecordStream appStream = null;
   private BLonAppDownloadJob.NvDirRecordStream nvDirStream = null;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonAppDownloadJob() {
   }

   public BLonAppDownloadJob(BLonNetmgmt netMgmt, BAppDownloadParameter param) {
      super(netMgmt);
      this.param = param;
   }

   @Override
   public void run() {
      try {
         BBlob blob = this.param.getAppFile();
         if (blob.length() > 0) {
            this.appStream = new BLonAppDownloadJob.NxeRecordStream(blob.copyBytes());
         }

         blob = this.param.getNvDirFile();
         if (blob.length() > 0) {
            this.nvDirStream = new BLonAppDownloadJob.NvDirRecordStream(blob.copyBytes());
         }

         BSubnetNode[] sns = this.param.getSubnetNodes();
         this.percentFactor = sns.length;

         for (int i = 0; i < sns.length; i++) {
            this.dev = this.lon.addressManager().getDeviceByAddress(sns[i]);
            this.percentOffset = (int)(100.0 / this.percentFactor) * i;
            this.doAppDownload();
         }
      } catch (JobCancelException var4) {
         this.canceled();
      } catch (Throwable var5) {
         this.fatal("Fatal error ", var5);
      }

      this.end();
   }

   private void doAppDownload() {
      if (this.dev.isLocal()) {
         this.fatal("Can not download application to local device " + this.dev.getDisplayName(null));
      } else {
         try {
            if (Neuron.isHostedNode(this.dev)) {
               this.fatal("Hosted node - application download not allowed " + this.dev.getDisplayName(null));
               return;
            }
         } catch (LonException var2) {
            this.fatal("Unable to communicate with device " + this.dev.getDisplayName(null), var2);
            return;
         }

         this.downLoadApp();
      }
   }

   private void downLoadApp() {
      BDeviceData dd = this.dev.getDeviceData();
      this.log().start("Application Download to " + this.dev.getDisplayName(null));

      try {
         NmUtil.setDeviceState(this.dev, BLonNodeState.applicationless);
         dd.set(BDeviceData.nodeState, BLonNodeState.applicationless, AddressManager.noDeviceChange);
         NmUtil.resetNode(this.dev);
         if (this.appStream != null) {
            this.appStream.reset();
            this.appStream.next();
            int n = 0;
            this.myProgress(5);

            while (this.appStream.available()) {
               this.appStream.writeRecord();
               if (++n % 20 == 0) {
                  this.myProgress(5 + (int)(this.appStream.getPercentComplete() * 0.65));
               }
            }

            this.myProgress(70);
            this.appStream.reset();
            this.appStream.writeRecord();
            NmUtil.resetNode(this.dev);
            NmUtil.recalculateChecksum(this.dev, 1);
            this.dev.updateMaxMessageLength();
         }

         if (this.nvDirStream != null) {
            this.nvDirStream.reset();

            for (int n = 0; this.nvDirStream.available(); this.nvDirStream.updateNv()) {
               if (++n % 20 == 0) {
                  this.myProgress(70 + (int)(this.appStream.getPercentComplete() * 0.15));
               }
            }
         }

         this.myProgress(85);
         BLonCommissionJob.commission(this.netMgmt, this.dev);
         if (this.param.getBind()) {
            BLonBindJob.bindDevice(this.netMgmt, this.dev);
         }

         this.pass("Application Download success");
      } catch (LonException var3) {
         this.fatal("Application Download failed for " + this.dev.getDisplayName(null), var3);
      }
   }

   private class NvDirRecordStream {
      byte[] data;
      int pos;

      NvDirRecordStream(byte[] d) {
         this.data = d;
         this.reset();
      }

      void updateNv() throws LonException {
         BLonNvDirection dir = this.getDirection();
         int arraySize = this.getArraySize();
         int ndx = this.getIndex();

         for (int i = 0; i < arraySize; i++) {
            int nvIndex = ndx + i;
            BNvConfigData deviceConfig = NmUtil.queryNvConfigData(BLonAppDownloadJob.this.dev, nvIndex);
            if (dir != deviceConfig.getDirection()) {
               deviceConfig.setDirection(dir);
               deviceConfig.setSelector(16383 - nvIndex);
               NmUtil.updateNvConfig(BLonAppDownloadJob.this.dev, nvIndex, deviceConfig);
            }
         }

         this.next();
      }

      void next() {
         if (this.available()) {
            this.pos += 5;
         }
      }

      boolean available() {
         return this.pos < this.data.length;
      }

      void reset() {
         this.pos = 0;
      }

      int getIndex() {
         return ((this.data[this.pos + 0] & 0xFF) << 8) + (this.data[this.pos + 1] & 0xFF);
      }

      int getArraySize() {
         int sz = ((this.data[this.pos + 2] & 255) << 8) + (this.data[this.pos + 3] & 255);
         return sz == 0 ? 1 : sz;
      }

      BLonNvDirection getDirection() {
         return this.data[this.pos + 4] == 0 ? BLonNvDirection.input : BLonNvDirection.output;
      }

      int getPercentComplete() {
         return this.pos / this.data.length * 100;
      }
   }

   private class NxeRecordStream {
      byte[] data;
      int pos;

      NxeRecordStream(byte[] d) {
         this.data = d;
         this.reset();
      }

      void writeRecord() throws LonException {
         Neuron.writeMemory(0, BLonAppDownloadJob.this.dev, this.getAddress(), 0, this.getRecordData());
         this.next();
      }

      void next() {
         if (this.available()) {
            this.pos = this.pos + this.getLength() + 3;
         }
      }

      boolean available() {
         return this.getLength() > 0;
      }

      void reset() {
         this.pos = 0;
      }

      int getLength() {
         return this.data[this.pos] & 0xFF;
      }

      int getAddress() {
         return ((this.data[this.pos + 1] & 0xFF) << 8) + (this.data[this.pos + 2] & 0xFF);
      }

      byte[] getRecordData() {
         int len = this.getLength();
         byte[] a = new byte[len];
         System.arraycopy(this.data, this.pos + 3, a, 0, len);
         return a;
      }

      int getPercentComplete() {
         return (int)((float)this.pos / this.data.length * 100.0F);
      }
   }
}
