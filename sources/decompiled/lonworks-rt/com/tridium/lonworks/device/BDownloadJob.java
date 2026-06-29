package com.tridium.lonworks.device;

import com.tridium.lonworks.netmgmt.BLonNetmgmtJob;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.selfdoc.SelfDocUtil;
import javax.baja.driver.BIDeviceFolder;
import javax.baja.driver.loadable.BDownloadParameters;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.BConfigParameter;
import javax.baja.lonworks.BILonLoadable;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.BNetworkConfig;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BConfigProps;
import javax.baja.lonworks.datatypes.BModifyFlags;
import javax.baja.lonworks.datatypes.BNcProps;
import javax.baja.lonworks.enums.BLonConfigScope;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.IntHashMap;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BDownloadJob extends BLonNetmgmtJob implements Runnable {
   public static final Type TYPE = Sys.loadType(BDownloadJob.class);
   BILonLoadable ld;
   BDownloadParameters parm;
   Context cx;
   boolean resetRequired;
   boolean offlineRequired;
   boolean online;
   IntHashMap disabledObjects = null;
   BIDeviceFolder devFdr;
   boolean deviceFolder = false;
   String name;
   int lcCnt;
   int inc;
   int total;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BDownloadJob() {
   }

   public BDownloadJob(BIDeviceFolder df, BDownloadParameters p, Context cx) {
      super((BLonNetwork)df.getNetwork());
      this.ld = null;
      this.devFdr = df;
      this.parm = p;
      this.cx = cx;
      this.name = ((BComponent)df).getDisplayName(null);
      this.deviceFolder = true;
   }

   public BDownloadJob(BILonLoadable ld, BDownloadParameters p, Context cx) {
      super(ld.getLonDevice().lonNetwork());
      this.ld = ld;
      this.parm = p;
      this.cx = cx;
      this.name = ((BComponent)ld).getDisplayName(null);
      this.deviceFolder = false;
   }

   @Override
   public void run() {
      try {
         if (this.deviceFolder) {
            this.downloadDeviceFolder(this.devFdr);
         } else {
            this.initForDownload(this.ld);
            this.downloadLoadable(this.ld);
            this.resetAfterDownload(this.ld);
         }
      } catch (JobCancelException var2) {
         this.canceled();
      } catch (Throwable var3) {
         this.fatal("Download failed for " + this.name, var3);
      }

      this.end();
   }

   void downloadDeviceFolder(BIDeviceFolder df) {
      BLonDevice[] devs = NmUtil.getLonDevices(df);

      for (int i = 0; i < devs.length; i++) {
         if (!devs[i].isLocal()) {
            this.initForDownload(devs[i]);
            this.downloadLoadable(devs[i]);
            this.resetAfterDownload(devs[i]);
         }
      }
   }

   void downloadLoadable(BILonLoadable ldabl) {
      boolean isDevice = ldabl instanceof BLonDevice;
      if (!isDevice) {
         ldabl.getLonDevice().beginDownload();
      }

      this.downLoad(ldabl);
      if (!isDevice) {
         ldabl.getLonDevice().endDownload();
      }
   }

   void downLoad(BILonLoadable ldabl) {
      ldabl.beginDownload();
      this.status("download " + ldabl.getDisplayName(null));
      SlotCursor<Property> sc = ((BComponent)ldabl).getProperties();

      while (sc.nextComponent()) {
         BComponent c = (BComponent)sc.get();
         if (c.getType().is(BLonComponent.TYPE)) {
            BLonComponent lc = (BLonComponent)c;
            if (lc.isForeignPersistent() && lc.isWriteable()) {
               try {
                  lc.doForceWrite();
                  this.status("download " + lc.getDisplayName(null));
                  this.myProgress(this.inc * this.lcCnt++);
               } catch (Throwable var6) {
                  this.error("failed " + lc.getDisplayName(null), var6);
               }
            }
         } else if (c.getType().is(BILonLoadable.TYPE)) {
            this.downLoad((BILonLoadable)c);
         }
      }

      ldabl.endDownload();
   }

   private void initForDownload(BILonLoadable ld) {
      BLonDevice dev = ld.getLonDevice();
      this.resetRequired = false;
      this.offlineRequired = false;
      this.online = dev.isConfigOnline();
      this.total = this.init(ld);
      if (this.disabledObjects != null) {
         this.enableObjects(dev, false);
      }

      if (this.offlineRequired) {
         try {
            NmUtil.setDeviceState(dev, BLonNodeState.configOffline);
         } catch (LonException var4) {
            System.out.println(var4);
         }
      }

      this.inc = 80 / (this.total + 1);
      this.lcCnt = 0;
      this.myProgress(20);
   }

   private int init(BILonLoadable ld) {
      int cnt = 0;
      SlotCursor<Property> sc = ((BComponent)ld).getProperties();

      while (sc.nextComponent()) {
         BComponent c = (BComponent)sc.get();
         if (c.getType().is(BLonComponent.TYPE)) {
            BLonComponent lc = (BLonComponent)c;
            if (lc.isForeignPersistent() && lc.isWriteable()) {
               this.init(lc);
               cnt++;
            }
         } else if (c.getType().is(BILonLoadable.TYPE)) {
            cnt += this.init((BILonLoadable)c);
         }
      }

      return cnt;
   }

   private void init(BLonComponent lc) {
      BModifyFlags mod;
      BLonConfigScope scope;
      String select;
      if (lc.isConfigParameter()) {
         BConfigProps cfgProps = ((BConfigParameter)lc).getConfigProps();
         mod = cfgProps.getModifyFlag();
         scope = cfgProps.getScope();
         select = cfgProps.getSelect();
      } else {
         if (!lc.isNetworkConfig()) {
            return;
         }

         BNcProps ncProps = ((BNetworkConfig)lc).getNcProps();
         mod = ncProps.getModifyFlag();
         scope = ncProps.getScope();
         select = ncProps.getSelect();
      }

      if (this.online && mod.isDisabled() && scope == BLonConfigScope.object) {
         this.addObjects(select);
      }

      if (this.online && mod.isOffline()) {
         this.offlineRequired = true;
      }

      if (mod.isReset()) {
         this.resetRequired = true;
      }
   }

   private void enableObjects(BLonDevice dev, boolean en) {
      if (this.disabledObjects != null) {
         Integer[] sels = new Integer[this.disabledObjects.size()];
         this.disabledObjects.toArray(sels);

         for (int i = 0; i < sels.length; i++) {
            dev.enableObject(sels[i], en);
         }
      }
   }

   private void addObjects(String select) {
      int[] sels = SelfDocUtil.selectToIntArray(select);

      for (int i = 0; i < sels.length; i++) {
         this.addObject(sels[i]);
      }
   }

   private void addObject(int objNdx) {
      if (this.disabledObjects == null) {
         this.disabledObjects = new IntHashMap();
      }

      if (this.disabledObjects.get(objNdx) == null) {
         this.disabledObjects.put(objNdx, objNdx);
      }
   }

   private void resetAfterDownload(BILonLoadable ld) {
      BLonDevice dev = ld.getLonDevice();
      if (this.disabledObjects != null) {
         this.enableObjects(dev, true);
         this.disabledObjects.clear();
         this.disabledObjects = null;
      }

      if (this.offlineRequired) {
         try {
            NmUtil.setDeviceState(dev, BLonNodeState.configOnline);
         } catch (LonException var4) {
            System.out.println(var4);
         }

         this.offlineRequired = false;
      }

      if (this.resetRequired) {
         dev.doReset();
      }

      this.resetRequired = false;
   }
}
