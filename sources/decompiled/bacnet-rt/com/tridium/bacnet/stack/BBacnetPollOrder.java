package com.tridium.bacnet.stack;

import java.util.List;
import javax.baja.bacnet.util.PollList;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = ""
   )})
public abstract class BBacnetPollOrder extends BComponent {
   public static final Property enabled = newProperty(0, true, null);
   public static final Property faultCause = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BBacnetPollOrder.class);

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent p) {
      return p instanceof BBacnetPoll;
   }

   public void started() {
      if (this.isRunning() && this.getEnabled()) {
         this.registerPollOrdering(this);
      }
   }

   public void changed(Property p, Context cx) {
      if (p.equals(enabled)) {
         if (this.getEnabled()) {
            this.registerPollOrdering(this);
         } else {
            this.registerPollOrdering(null);
         }
      }
   }

   private void registerPollOrdering(BBacnetPollOrder pollOrder) {
      BBacnetPoll poll = (BBacnetPoll)this.getParent();
      poll.setPollOrder(pollOrder);
   }

   public abstract void sort(List<PollList> var1);
}
