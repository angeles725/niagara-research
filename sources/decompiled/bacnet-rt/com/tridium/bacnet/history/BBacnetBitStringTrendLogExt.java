package com.tridium.bacnet.history;

import com.tridium.bacnet.datatypes.BTrendEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "propertyId",
      type = "int",
      defaultValue = "BBacnetPropertyIdentifier.STATUS_FLAGS",
      flags = 1
   ), @NiagaraProperty(
      name = "length",
      type = "int",
      defaultValue = "0",
      flags = 65
   )})
public class BBacnetBitStringTrendLogExt extends BBacnetNumericIntervalTrendLogExt {
   public static final Property propertyId = newProperty(1, 111, null);
   public static final Property length = newProperty(65, 0, null);
   public static final Type TYPE = Sys.loadType(BBacnetBitStringTrendLogExt.class);
   private BBacnetBitStringTrendRecord rec;
   private boolean trigger;
   private static Logger logger = Logger.getLogger("bacnet.server");
   private Object SEQUENCE_LOCK = new Object();

   public int getPropertyId() {
      return this.getInt(propertyId);
   }

   public void setPropertyId(int v) {
      this.setInt(propertyId, v, null);
   }

   public int getLength() {
      return this.getInt(length);
   }

   public void setLength(int v) {
      this.setInt(length, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.setFlags(precision, 4);
      this.setFlags(minRolloverValue, 4);
      this.setFlags(maxRolloverValue, 4);
      this.rec = new BBacnetBitStringTrendRecord(this.getPropertyId());
   }

   @Override
   public void atSteadyState() {
      if (this.isRunning()) {
         try {
            BAbsTime timestamp = BAbsTime.now();
            if (this.getEnabled() && this.getActivePeriod().isActive(timestamp)) {
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

   @Override
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
                     BacnetTrendLogUtil.writeEvent(
                        this,
                        timestamp,
                        this.getStatus(),
                        BacnetTrendLogUtil.incrementSequenceNumber(this.getTotalRecordCount()),
                        this.getEnabled() ? BTrendEvent.LOG_STATUS_ENABLED : BTrendEvent.LOG_STATUS_DISABLED
                     );
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

   @Override
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

   @Override
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

   public boolean isParentLegal(BComponent parent) {
      return Boolean.TRUE;
   }

   @Override
   public BBacnetTrendRecord getRecord() {
      return this.rec;
   }

   @Override
   public boolean getTrigger() {
      return this.trigger;
   }

   @Override
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
   public Type getRecordType() {
      return BBacnetBitStringTrendRecord.TYPE;
   }
}
