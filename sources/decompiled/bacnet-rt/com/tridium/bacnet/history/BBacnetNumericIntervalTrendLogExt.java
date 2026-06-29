package com.tridium.bacnet.history;

import com.tridium.bacnet.datatypes.BTrendEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.history.ext.BNumericIntervalHistoryExt;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BLong;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "totalRecordCount",
   type = "long",
   defaultValue = "BLong.make(0)",
   flags = 1
)
@NiagaraActions({@NiagaraAction(
      name = "startLogging"
   ), @NiagaraAction(
      name = "stopLogging"
   )})
public class BBacnetNumericIntervalTrendLogExt extends BNumericIntervalHistoryExt implements BIBacnetTrendLogExt {
   public static final Property totalRecordCount = newProperty(1, BLong.make(0L), null);
   public static final Action startLogging = newAction(0, null);
   public static final Action stopLogging = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetNumericIntervalTrendLogExt.class);
   private BBacnetNumericTrendRecord rec;
   private boolean trigger = false;
   private static Logger logger = Logger.getLogger("bacnet.server");
   private Object SEQUENCE_LOCK = new Object();

   @Override
   public long getTotalRecordCount() {
      return this.getLong(totalRecordCount);
   }

   @Override
   public void setTotalRecordCount(long v) {
      this.setLong(totalRecordCount, v, null);
   }

   public void startLogging() {
      this.invoke(startLogging, null, null);
   }

   public void stopLogging() {
      this.invoke(stopLogging, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.rec = new BBacnetNumericTrendRecord();
      if (!(this.getActivePeriod() instanceof BBacnetActivePeriod)) {
         this.setActivePeriod(new BBacnetActivePeriod());
      }

      if (Sys.atSteadyState() && this.initHistoryExt()) {
         BacnetTrendLogUtil.initHistoryExt(this);
      }
   }

   protected boolean initHistoryExt() {
      return true;
   }

   public void atSteadyState() {
      if (this.isRunning()) {
         try {
            BAbsTime timestamp = BAbsTime.make();
            if (this.getActivePeriod().isActive(timestamp) && this.getEnabled()) {
               synchronized (this.SEQUENCE_LOCK) {
                  this.trigger = true;
                  long sequenceNumber = BacnetTrendLogUtil.incrementSequenceNumber(this.getTotalRecordCount());
                  BTrendEvent event = BTrendEvent.LOG_STATUS_ENABLED;
                  BStatus status = BStatus.DEFAULT;
                  BacnetTrendLogUtil.writeEvent(this, timestamp, status, sequenceNumber, event);
               }

               BacnetTrendLogUtil.initHistoryExt(this);
            }
         } catch (Exception var13) {
            logger.log(Level.SEVERE, "Error storing log status enabled event", (Throwable)var13);
         } finally {
            this.trigger = false;
         }
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(enabled)) {
            if (!this.isParentLegal(this.getParent().asComponent())) {
               logger.severe("Tried to enable BacnetTrendLogExt inside an illegal parent!?");
               this.setEnabled(false);
            } else {
               try {
                  this.trigger = true;
                  BAbsTime timestamp = BAbsTime.make();
                  synchronized (this.SEQUENCE_LOCK) {
                     if (this.getEnabled()) {
                        BacnetTrendLogUtil.writeEvent(
                           this,
                           timestamp,
                           this.getStatus(),
                           BacnetTrendLogUtil.incrementSequenceNumber(this.getTotalRecordCount()),
                           BTrendEvent.LOG_STATUS_ENABLED
                        );
                     } else {
                        BacnetTrendLogUtil.writeEvent(
                           this,
                           timestamp,
                           this.getStatus(),
                           BacnetTrendLogUtil.incrementSequenceNumber(this.getTotalRecordCount()),
                           BTrendEvent.LOG_STATUS_DISABLED
                        );
                     }
                  }
               } catch (Exception var11) {
                  logger.log(Level.SEVERE, "Error storing event", (Throwable)var11);
               } finally {
                  this.trigger = false;
               }
            }
         }
      }
   }

   public void clockChanged(BRelTime shift) throws Exception {
      if (this.getEnabled()) {
         try {
            synchronized (this.SEQUENCE_LOCK) {
               this.trigger = true;
               long sequenceNumber = BacnetTrendLogUtil.incrementSequenceNumber(this.getTotalRecordCount());
               BAbsTime timestamp = BAbsTime.now();
               BTrendEvent event = BTrendEvent.makeTimeChange(shift.getSeconds());
               BStatus status = this.getStatus();
               BacnetTrendLogUtil.writeEvent(this, timestamp, status, sequenceNumber, event);
            }
         } catch (Exception var14) {
            logger.log(Level.SEVERE, "Error storing event", (Throwable)var14);
         } finally {
            this.trigger = false;
         }
      }
   }

   public void doStartLogging() {
      this.setEnabled(true);
   }

   public void doStopLogging() {
      try {
         synchronized (this.SEQUENCE_LOCK) {
            this.trigger = true;
            BAbsTime timestamp = BAbsTime.make();
            BacnetTrendLogUtil.writeEvent(
               this, timestamp, BStatus.DEFAULT, BacnetTrendLogUtil.incrementSequenceNumber(this.getTotalRecordCount()), BTrendEvent.LOG_STATUS_DISABLED
            );
         }
      } catch (Exception var9) {
         logger.log(Level.SEVERE, "Error storing event", (Throwable)var9);
      } finally {
         this.trigger = false;
         this.setEnabled(false);
      }
   }

   public Type getRecordType() {
      return BBacnetNumericTrendRecord.TYPE;
   }

   protected void writeRecord(BAbsTime timestamp, BStatusValue out) throws IOException {
      try {
         synchronized (this.SEQUENCE_LOCK) {
            this.trigger = true;
            BacnetTrendLogUtil.writeRecord(this, timestamp, out);
         }
      } finally {
         this.trigger = false;
      }
   }

   @Override
   public BBacnetTrendRecord getRecord() {
      return this.rec;
   }

   @Override
   public ErrorType setLogInterval(long logInterval, Context cx) {
      this.set(interval, BRelTime.make(logInterval), cx);
      return null;
   }

   @Override
   public boolean getTrigger() {
      return this.trigger;
   }
}
