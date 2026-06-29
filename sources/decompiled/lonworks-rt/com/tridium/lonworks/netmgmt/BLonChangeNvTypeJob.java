package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.datatypes.BChangeNvTypeParameter;
import com.tridium.lonworks.device.DynaDev;
import com.tridium.lonworks.util.LonStringUtil;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.selfdoc.NvDoc;
import com.tridium.lonworks.util.selfdoc.SelfDoc;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.BConfigParameter;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.BNetworkConfig;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.enums.BLonNvTypeCategoryEnum;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.util.ScptUtil;
import javax.baja.lonworks.util.SnvtUtil;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonChangeNvTypeJob extends BLonNetmgmtJob {
   public static final Type TYPE = Sys.loadType(BLonChangeNvTypeJob.class);
   SelfDoc sdoc = null;
   BLonDevice dev;
   BChangeNvTypeParameter parm;
   BChangeNvTypeAction chngActn;
   BConfigParameter[] cps;
   BNetworkConfig[] ncis;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonChangeNvTypeJob() {
   }

   public BLonChangeNvTypeJob(BLonDevice dev, BChangeNvTypeParameter parm, BChangeNvTypeAction chngActn) {
      super((BLonNetwork)dev.getNetwork());
      this.dev = dev;
      this.parm = parm;
      this.chngActn = chngActn;
      this.cps = dev.getConfigParameters();
      this.ncis = dev.getNetworkConfigs();
   }

   @Override
   public void run() {
      try {
         int[] nvs = LonStringUtil.getIntArray(this.parm.getNvIndex());
         String[] cnfgs = LonStringUtil.getStringArray(this.parm.getNvTypeScpt());
         int snvtType = this.parm.getTypeScope() == 0 ? this.parm.getTypeIndex() : 0;
         BLonData ld = SnvtUtil.getLonData(snvtType);
         this.myProgress(2);

         try {
            this.sdoc = this.chngActn.getSelfDoc();
         } catch (Exception var11) {
         }

         this.myProgress(30);
         this.percentFactor = nvs.length;

         for (int i = 0; i < nvs.length; i++) {
            this.percentOffset = (int)(70.0 / this.percentFactor) * i + 30;
            this.doChangeNvType(nvs[i], cnfgs[i], snvtType, ld, this.parm.getUpdate());
         }
      } catch (JobCancelException var12) {
         this.canceled();
      } catch (Throwable var13) {
         this.fatal("failed ", var13);
      } finally {
         this.dev.changeNvTypeComplete();
      }

      this.end();
   }

   private void doChangeNvType(int nvIndex, String nvTypeScpt, int snvtType, BLonData ld, boolean update) {
      try {
         BNetworkVariable nv = this.dev.getNetworkVariable(nvIndex);
         if (update) {
            this.parm.setTypeScope(0);
            this.parm.setTypeIndex(nv.getNvProps().getSnvtType());
            this.parm.updateNvTypePara(nv.getData());
         } else {
            BLonData ldc = (BLonData)ld.newCopy();
            DynaDev.setNonCritical(ldc);
            nv.setData(ldc);
            nv.getNvProps().setSnvtType(snvtType);
            BLonComponent[] inherits = ScptUtil.findInheritedConfigsForNv(this.dev, nvIndex);

            for (int n = 0; n < inherits.length; n++) {
               inherits[n].setData((BLonData)ld.newCopy());
            }
         }

         if (!this.dev.getDeviceData().getHosted() && this.sdoc != null && this.sdoc.snvtPtr != 65535 && this.sdoc.getNvDocs() != null) {
            int offset = this.sdoc.hdrLength + this.getRecordNum(this.sdoc, nvIndex) * 2;
            byte[] data = new byte[]{this.parm.getTypeScope() == 0 ? (byte)this.parm.getTypeIndex() : 0};
            Neuron.writeMemory(0, this.dev, this.sdoc.snvtPtr + offset + 1, 1, data);
         }

         this.myProgress(80);
         BLonComponent lc = this.getTypeCp(nvTypeScpt);
         byte[] pid = this.parm.getTypeProgramId().getByteArray();
         lc.setLonFloat("typeProgramID0", pid[0], BLonNetwork.lonNoWrite);
         lc.setLonFloat("typeProgramID1", pid[1], BLonNetwork.lonNoWrite);
         lc.setLonFloat("typeProgramID2", pid[2], BLonNetwork.lonNoWrite);
         lc.setLonFloat("typeProgramID3", pid[3], BLonNetwork.lonNoWrite);
         lc.setLonFloat("typeProgramID4", pid[4], BLonNetwork.lonNoWrite);
         lc.setLonFloat("typeProgramID5", pid[5], BLonNetwork.lonNoWrite);
         lc.setLonFloat("typeProgramID6", pid[6], BLonNetwork.lonNoWrite);
         lc.setLonFloat("typeProgramID7", pid[7], BLonNetwork.lonNoWrite);
         lc.setLonFloat("typeScope", this.parm.getTypeScope(), BLonNetwork.lonNoWrite);
         lc.setLonFloat("typeIndex", this.parm.getTypeIndex(), BLonNetwork.lonNoWrite);
         lc.setLonEnum("typeCategory", BLonNvTypeCategoryEnum.make(this.parm.getTypeCategory()), BLonNetwork.lonNoWrite);
         lc.setLonFloat("typeLength", this.parm.getTypeLength(), BLonNetwork.lonNoWrite);
         lc.setLonFloat("scalingFactorA", this.parm.getScalingFactorA(), BLonNetwork.lonNoWrite);
         lc.setLonFloat("scalingFactorB", this.parm.getScalingFactorB(), BLonNetwork.lonNoWrite);
         lc.setLonFloat("scalingFactorC", this.parm.getScalingFactorC(), BLonNetwork.lonNoWrite);
         lc.doForceWrite();
         this.myProgress(100);
         this.pass("Change " + this.dev.getNetworkVariable(nvIndex).getDisplayName(null) + " type success ");
      } catch (Throwable var10) {
         this.error("Change nv type failed for " + this.dev.getDisplayName(null), var10);
      }
   }

   private BLonComponent getTypeCp(String name) {
      for (int i = 0; i < this.cps.length; i++) {
         if (this.cps[i].getName().equals(name)) {
            return this.cps[i];
         }
      }

      for (int ix = 0; ix < this.ncis.length; ix++) {
         if (this.ncis[ix].getName().equals(name)) {
            return this.ncis[ix];
         }
      }

      return null;
   }

   private int getRecordNum(SelfDoc selfDoc, int nvIndex) throws LonException {
      NvDoc[] nvDocs = selfDoc.getNvDocs();

      for (int i = 0; i < nvDocs.length; i++) {
         NvDoc nvDoc = nvDocs[i];
         int docNvIndex = nvDoc.getNvIndex();
         if (!nvDoc.isArray()) {
            if (nvIndex == docNvIndex) {
               return i;
            }
         } else if (nvIndex >= docNvIndex && nvIndex < docNvIndex + nvDoc.getArraySize()) {
            return i;
         }
      }

      throw new LonException("No descriptor for nv " + nvIndex);
   }
}
