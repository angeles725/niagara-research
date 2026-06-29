package com.tridium.lonworks.device;

import com.tridium.lonworks.netmgmt.BLonNetmgmtJob;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.driver.BIDeviceFolder;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.BILonLoadable;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BUploadJob extends BLonNetmgmtJob implements Runnable {
   public static final Type TYPE = Sys.loadType(BUploadJob.class);
   BLonDevice dev;
   BUploadParameters parm;
   Context cx;
   BILonLoadable ld;
   BIDeviceFolder devFdr;
   boolean deviceFolder = false;
   String name;
   int lcCnt;
   int inc;
   boolean trans;
   boolean pers;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BUploadJob() {
   }

   public BUploadJob(BIDeviceFolder df, BUploadParameters p, Context cx) {
      super((BLonNetwork)df.getNetwork());
      this.ld = null;
      this.devFdr = df;
      this.parm = p;
      this.cx = cx;
      this.trans = this.parm.getUploadTransient();
      this.pers = this.parm.getUploadPersistent();
      this.name = ((BComponent)df).getDisplayName(null);
      this.deviceFolder = true;
   }

   public BUploadJob(BILonLoadable ld, BUploadParameters p, Context cx) {
      super(ld.getLonDevice().lonNetwork());
      this.ld = ld;
      this.parm = p;
      this.cx = cx;
      this.trans = this.parm.getUploadTransient();
      this.pers = this.parm.getUploadPersistent();
      this.name = ((BComponent)ld).getDisplayName(null);
      this.deviceFolder = false;
   }

   @Override
   public void run() {
      try {
         if (this.deviceFolder) {
            this.uploadDeviceFolder(this.devFdr);
         } else {
            this.uploadLoadable(this.ld);
         }
      } catch (JobCancelException var2) {
         this.canceled();
      } catch (Throwable var3) {
         this.fatal("Upload failed for " + this.name, var3);
      }

      this.end();
   }

   void uploadDeviceFolder(BIDeviceFolder df) {
      BLonDevice[] devs = NmUtil.getLonDevices(df);

      for (int i = 0; i < devs.length; i++) {
         if (!devs[i].isLocal()) {
            this.uploadLoadable(devs[i]);
         }
      }
   }

   void uploadLoadable(BILonLoadable ldabl) {
      this.myProgress(20);
      int cnt = this.countReadable(ldabl);
      this.inc = 80 / (cnt + 1);
      this.lcCnt = 0;
      this.upload(ldabl);
   }

   void checkChanges(BINvContainer nvCntr) {
      if (nvCntr.getLonDevice().hasChangeableNvs()) {
         ChangeableNvUtil.uploadChangeables(nvCntr);
      }
   }

   void upload(BILonLoadable ldabl) {
      BComponent asComp = (BComponent)ldabl;
      ldabl.beginUpload();
      this.status("Upload " + asComp.getDisplayName(null));
      if (asComp.getType().is(BINvContainer.TYPE)) {
         this.checkChanges((BINvContainer)asComp);
      }

      SlotCursor<Property> sc = asComp.getProperties();

      while (sc.nextComponent()) {
         BComponent c = (BComponent)sc.get();
         if (c.getType().is(BLonComponent.TYPE)) {
            BLonComponent lc = (BLonComponent)c;
            boolean fp = lc.isForeignPersistent();
            if (fp && this.pers || !fp && this.trans) {
               try {
                  lc.doForceRead();
                  this.status("Uploaded " + lc.getDisplayName(null));
               } catch (Throwable var8) {
                  this.error("failed " + lc.getDisplayName(null), var8);
               }

               this.myProgress(this.inc * this.lcCnt++);
            }
         } else if (c.getType().is(BILonLoadable.TYPE)) {
            this.upload((BILonLoadable)c);
         }
      }

      ldabl.endUpload();
   }

   int countReadable(BILonLoadable ldabl) {
      int cnt = 0;
      SlotCursor<Property> sc = ((BComponent)ldabl).getProperties();

      while (sc.nextComponent()) {
         BComponent c = (BComponent)sc.get();
         if (c.getType().is(BLonComponent.TYPE)) {
            boolean fp = ((BLonComponent)c).isForeignPersistent();
            if (fp && this.pers || !fp && this.trans) {
               cnt++;
            }
         } else if (c.getType().is(BILonLoadable.TYPE)) {
            cnt += this.countReadable((BILonLoadable)c);
         }
      }

      return cnt;
   }
}
