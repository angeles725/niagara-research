package com.tridiumemea.extras;

import com.tridiumemea.extras.enums.BDamperState;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.sys.BEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "out",
      type = "BEnum",
      defaultValue = "BDamperState.DEFAULT",
      flags = 9
   ), @NiagaraProperty(
      name = "demand",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   ), @NiagaraProperty(
      name = "isRunning",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   ), @NiagaraProperty(
      name = "inAlarm",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   ), @NiagaraProperty(
      name = "demandMidway",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   )})
public class BDamperEquipment extends BKitPxBuildingBaseEnum {
   public static final Property out = newProperty(9, BDamperState.DEFAULT, null);
   public static final Property demand = newProperty(8, new BStatusBoolean(false), null);
   public static final Property isRunning = newProperty(8, new BStatusBoolean(false), null);
   public static final Property inAlarm = newProperty(8, new BStatusBoolean(false), null);
   public static final Property demandMidway = newProperty(8, new BStatusBoolean(false), null);
   public static final Type TYPE = Sys.loadType(BDamperEquipment.class);

   @Override
   public BEnum getOut() {
      return (BEnum)this.get(out);
   }

   @Override
   public void setOut(BEnum v) {
      this.set(out, v, null);
   }

   public BStatusBoolean getDemand() {
      return (BStatusBoolean)this.get(demand);
   }

   public void setDemand(BStatusBoolean v) {
      this.set(demand, v, null);
   }

   public BStatusBoolean getIsRunning() {
      return (BStatusBoolean)this.get(isRunning);
   }

   public void setIsRunning(BStatusBoolean v) {
      this.set(isRunning, v, null);
   }

   public BStatusBoolean getInAlarm() {
      return (BStatusBoolean)this.get(inAlarm);
   }

   public void setInAlarm(BStatusBoolean v) {
      this.set(inAlarm, v, null);
   }

   public BStatusBoolean getDemandMidway() {
      return (BStatusBoolean)this.get(demandMidway);
   }

   public void setDemandMidway(BStatusBoolean v) {
      this.set(demandMidway, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop == demand || prop == isRunning || prop == inAlarm || prop == demandMidway) {
            if (this.getInAlarm().getValue()) {
               this.setOut(BDamperState.alarm);
            } else {
               int state = 0;
               state = this.getIsRunning().getValue() ? 2 : 0;
               state = this.getDemand().getValue() ? state + 1 : state;
               if (this.getDemandMidway().getValue() && state == 3) {
                  state++;
               }

               this.setOut(BDamperState.make(state));
            }
         }
      }
   }
}
