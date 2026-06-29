package com.tridium.bacnet.history;

import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.history.ext.BActivePeriod;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BTime;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "startTime",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()"
   ), @NiagaraProperty(
      name = "stopTime",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()"
   )})
public class BBacnetActivePeriod extends BActivePeriod {
   public static final Property startTime = newProperty(0, new BBacnetDateTime(), null);
   public static final Property stopTime = newProperty(0, new BBacnetDateTime(), null);
   public static final Type TYPE = Sys.loadType(BBacnetActivePeriod.class);

   public BBacnetDateTime getStartTime() {
      return (BBacnetDateTime)this.get(startTime);
   }

   public void setStartTime(BBacnetDateTime v) {
      this.set(startTime, v, null);
   }

   public BBacnetDateTime getStopTime() {
      return (BBacnetDateTime)this.get(stopTime);
   }

   public void setStopTime(BBacnetDateTime v) {
      this.set(stopTime, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isAlwaysActive() {
      return false;
   }

   public boolean isNeverActive() {
      return false;
   }

   public BAbsTime getActiveStart(BAbsTime time) {
      return BAbsTime.make(time, BTime.make(0, 0, 0, 0));
   }

   public BAbsTime getActiveEnd(BAbsTime time) {
      BAbsTime start = this.getStartTime().toBAbsTime();
      BAbsTime end = this.getStopTime().toBAbsTime();
      if (time.isBefore(start)) {
         return null;
      } else {
         return time.isAfter(end) ? null : end;
      }
   }

   public BAbsTime getNextActive(BAbsTime time) {
      BAbsTime active = this.getStartTime().toBAbsTime();
      return active.isAfter(time) ? active : null;
   }

   public BAbsTime getNextInactive(BAbsTime time) {
      BAbsTime inactive = this.getStopTime().toBAbsTime();
      return inactive.isAfter(time) ? inactive : null;
   }

   public boolean isActive(BAbsTime timestamp) {
      BBacnetDateTime ts = new BBacnetDateTime(timestamp);
      return ts.isNotBefore(this.getStartTime()) && ts.isNotAfter(this.getStopTime());
   }
}
