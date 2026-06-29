package com.tridium.lonworks.netmgmt;

import com.tridium.util.ThrowableUtil;
import javax.baja.job.BJob;
import javax.baja.job.BJobState;
import javax.baja.job.JobCancelException;
import javax.baja.job.JobLogItem;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraAction(
   name = "getLastLog",
   returnType = "BString"
)
@NiagaraTopic(
   name = "itemLogged",
   eventType = "BString"
)
public abstract class BLonNetmgmtJob extends BJob implements Runnable {
   public static final Action getLastLog = newAction(0, null);
   public static final Topic itemLogged = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BLonNetmgmtJob.class);
   protected BLonNetmgmt netMgmt;
   protected BLonNetwork lon;
   protected boolean error = false;
   protected double percentFactor = 1.0;
   protected int percentOffset = 0;

   public BString getLastLog() {
      return (BString)this.invoke(getLastLog, null, null);
   }

   public void fireItemLogged(BString event) {
      this.fire(itemLogged, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BLonNetmgmtJob() {
   }

   public BLonNetmgmtJob(BLonNetmgmt netMgmt) {
      this.netMgmt = netMgmt;
      this.lon = netMgmt.lonNetwork();
   }

   public BLonNetmgmtJob(BLonNetwork lon) {
      this.netMgmt = lon.netmgmt();
      this.lon = lon;
   }

   public void doRun(Context cx) {
      this.lon.postAsync(this);
   }

   public void doCancel(Context cx) {
      if (this.getJobState().isRunning()) {
         this.setJobState(BJobState.canceling);
      }
   }

   public boolean isAlive() {
      return this.getJobState() == BJobState.running;
   }

   public boolean isCanceling() {
      return this.getJobState() == BJobState.canceling;
   }

   public void myProgress(int percent) throws JobCancelException {
      if (this.isCanceling()) {
         throw new JobCancelException();
      } else {
         this.progress(this.percentOffset + (int)(percent / this.percentFactor));
      }
   }

   public void incrementProgress(int pct, int max) throws JobCancelException {
      if (this.isCanceling()) {
         throw new JobCancelException();
      } else {
         int p = this.getProgress() + (int)(pct / this.percentFactor);
         int adjustMax = this.percentOffset + (int)(max / this.percentFactor);
         if (p < adjustMax) {
            this.progress(p);
         }
      }
   }

   public void status(String s) {
      this.log().message(s);
      this.lon.log().fine(s);
      this.fireItemLogged(this.doGetLastLog());
   }

   public void warning(String s) {
      this.warning(s, null);
   }

   public void warning(String s, Throwable e) {
      this.log().failed(s, e);
      this.lon.log().warning(s);
      this.fireItemLogged(this.doGetLastLog());
   }

   public void error(String s, Throwable e) {
      this.log().failed(s, e);
      this.lon.log().severe(s);
      this.displayThrowable(e);
      this.error = true;
      this.fireItemLogged(this.doGetLastLog());
   }

   public void fatal(String s) {
      this.fatal(s, null);
   }

   public void fatal(String s, Throwable e) {
      this.log().failed(s, e);
      this.lon.log().severe(s);
      this.displayThrowable(e);
      this.error = true;
      this.fireItemLogged(this.doGetLastLog());
   }

   private void displayThrowable(Throwable e) {
      if (e != null) {
         e.printStackTrace();
         this.displayThrowable(ThrowableUtil.getCause(e));
      }
   }

   public void end() {
      BJobState js = this.getJobState();
      if (this.error) {
         this.complete(BJobState.failed);
      } else if (js != BJobState.canceling && js != BJobState.canceled) {
         this.complete(BJobState.success);
      } else {
         this.complete(BJobState.canceled);
      }
   }

   public void pass(String s) {
      this.log().success(s);
      this.fireItemLogged(this.doGetLastLog());
   }

   public BString doGetLastLog() {
      int numItems = this.log().size();
      if (numItems <= 0) {
         return BString.DEFAULT;
      } else {
         JobLogItem it = this.log().getItem(numItems - 1);
         return BString.make(it.encode());
      }
   }

   public void success() {
      super.success();
      this.fireItemLogged(this.doGetLastLog());
   }

   public void canceled() {
      super.canceled();
      this.fireItemLogged(this.doGetLastLog());
   }

   public void failed(Throwable cause) {
      super.failed(cause);
      this.fireItemLogged(this.doGetLastLog());
   }

   @Override
   public abstract void run();
}
