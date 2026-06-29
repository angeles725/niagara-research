package com.tridium.bacnet.history;

import java.io.IOException;
import javax.baja.bacnet.export.BOutOfServiceExt;
import javax.baja.history.BCollectionInterval;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "activePeriod",
      type = "BActivePeriod",
      defaultValue = "new BBacnetActivePeriod()",
      facets = {@Facet("BFacets.make(\"alwaysExpand\", true)")},
      override = true
   ), @NiagaraProperty(
      name = "interval",
      type = "BRelTime",
      defaultValue = "BRelTime.makeMinutes(15)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(0L)"
      )},
      override = true
   )})
public class BBacnetStringTrendLogExt extends BBacnetStringIntervalTrendLogExt {
   public static final Property activePeriod = newProperty(0, new BBacnetActivePeriod(), BFacets.make("alwaysExpand", true));
   public static final Property interval = newProperty(0, BRelTime.makeMinutes(15), BFacets.make("min", BRelTime.make(0L)));
   public static final Type TYPE = Sys.loadType(BBacnetStringTrendLogExt.class);
   private BStatusValue lastValue;
   private boolean scheduleCollectionRunning;

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (p.equals(interval)) {
         try {
            if (this.getRecord() != null) {
               BStatusValue value = this.getParentPoint().getOutStatusValue();
               this.pointChanged(BAbsTime.make(), value);
            }

            if (this.getInterval().getMillis() == 0L) {
               this.getHistoryConfig().setInterval(BCollectionInterval.IRREGULAR);
            }
         } catch (IOException var4) {
         }
      }
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.getHistoryConfig().setInterval(BCollectionInterval.IRREGULAR);
   }

   @Override
   protected boolean initHistoryExt() {
      return false;
   }

   public void activated(BAbsTime startTime, BAbsTime currentTime, BStatusValue value) throws IOException {
      boolean isCovMode = this.isCovMode();
      if (isCovMode || this.checkCurrentTime(startTime, currentTime)) {
         this.writeLastValue(currentTime, value, false);
      }

      if (!isCovMode) {
         super.activated(startTime, currentTime, value);
         this.scheduleCollectionRunning = true;
      }
   }

   public void deactivated(BAbsTime currentTime, BStatusValue value) {
      super.deactivated(currentTime, value);
      this.scheduleCollectionRunning = false;
   }

   private boolean checkCurrentTime(BAbsTime startTime, BAbsTime currentTime) {
      return startTime.getMillis() + 5000L > currentTime.getMillis();
   }

   public void pointChanged(BAbsTime timestamp, BStatusValue out) throws IOException {
      if (this.isCovMode()) {
         this.deactivated(timestamp, out);
         this.writeLastValue(timestamp, out, true);
      } else if (!this.scheduleCollectionRunning) {
         this.activated(this.getActivePeriod().getActiveStart(BAbsTime.make().timeOfDay(0, 0, 0, 0)), timestamp, out);
      }
   }

   public boolean isCovMode() {
      return this.getInterval().getMillis() == 0L;
   }

   protected void writeLastValue(BAbsTime timestamp, BStatusValue out, boolean checkForChange) throws IOException {
      if (this.lastValue == null) {
         this.lastValue = (BStatusValue)out.newCopy(true);
      } else {
         if (checkForChange && !this.isChange(this.lastValue, out)) {
            return;
         }

         this.lastValue.copyFrom(out);
      }

      this.writeRecord(timestamp, out);
   }

   protected boolean isChange(BStatusValue oldValue, BStatusValue newValue) {
      BOutOfServiceExt oosext = BacnetTrendLogUtil.getOosExt(this);
      if (oosext != null && oosext.getOutOfService()) {
         newValue.setValueValue(oosext.getPresentValue());
      }

      if (!oldValue.getStatus().equals(newValue.getStatus())) {
         return true;
      } else {
         String newString = ((BStatusString)newValue).getValue();
         String oldString = ((BStatusString)oldValue).getValue();
         return !newString.equals(oldString);
      }
   }

   public void updateStatus() {
      BStatus prevStatus = this.getStatus();
      String prevFaultCause = this.getFaultCause();
      super.updateStatus();
      BStatus statusAfterUpdate = this.getStatus();
      String faultCauseAfterUpdate = this.getFaultCause();
      if (this.getInterval().getMillis() == 0L && this.getFaultCause().startsWith("Invalid interval")) {
         if (prevStatus.isFault() && !prevFaultCause.equals(faultCauseAfterUpdate)) {
            this.setStatus(prevStatus);
            this.setFaultCause(prevFaultCause);
         } else if (!prevStatus.isFault() && !prevFaultCause.equals(faultCauseAfterUpdate)) {
            this.setStatus(BStatus.makeFault(statusAfterUpdate, false));
            this.setFaultCause(prevFaultCause);
         } else {
            this.setStatus(BStatus.makeFault(statusAfterUpdate, false));
            this.setFaultCause("");
         }

         this.deactivated(BAbsTime.now(), this.getParentPoint().getOutStatusValue());
      }
   }

   protected boolean isValidInterval(BRelTime time) {
      return time.getMillis() >= 0L;
   }

   public BStatusValue getLastValue() {
      return this.lastValue;
   }

   public boolean isScheduleCollectionRunning() {
      return this.scheduleCollectionRunning;
   }

   public void setScheduleCollectionRunning(boolean scheduleCollectionRunning) {
      this.scheduleCollectionRunning = scheduleCollectionRunning;
   }
}
