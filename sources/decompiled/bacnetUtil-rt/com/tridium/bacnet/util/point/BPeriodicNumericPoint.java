package com.tridium.bacnet.util.point;

import javax.baja.control.BNumericPoint;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "updateInterval",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(30)"
   )})
public abstract class BPeriodicNumericPoint extends BNumericPoint {
   public static final Property enabled = newProperty(0, true, null);
   public static final Property updateInterval = newProperty(0, BRelTime.makeSeconds(30), null);
   public static final Type TYPE = Sys.loadType(BPeriodicNumericPoint.class);
   private Ticket ticket = null;

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public BRelTime getUpdateInterval() {
      return (BRelTime)this.get(updateInterval);
   }

   public void setUpdateInterval(BRelTime v) {
      this.set(updateInterval, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.initTimer();
   }

   public void stopped() throws Exception {
      super.stopped();
   }

   public void changed(Property prop, Context cx) {
      super.changed(prop, cx);
      if (prop == updateInterval || prop == enabled) {
         if (this.getUpdateInterval().getMillis() < 1000L) {
            this.setUpdateInterval(BRelTime.make(1000L));
         }

         if (this.isRunning()) {
            this.initTimer();
         }
      }
   }

   private void initTimer() {
      if (this.ticket != null) {
         this.ticket.cancel();
      }

      if (this.getEnabled()) {
         this.ticket = Clock.schedulePeriodically(this, this.getUpdateInterval(), execute, null);
      }
   }
}
