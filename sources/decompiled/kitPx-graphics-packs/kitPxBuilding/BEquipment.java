package com.tridiumemea.extras;

import com.tridiumemea.extras.enums.BEquipmentState;
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
      defaultValue = "BEquipmentState.DEFAULT",
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
   )})
public class BEquipment extends BKitPxBuildingBaseEnum {
   public static final Property out = newProperty(9, BEquipmentState.DEFAULT, null);
   public static final Property demand = newProperty(8, new BStatusBoolean(false), null);
   public static final Property isRunning = newProperty(8, new BStatusBoolean(false), null);
   public static final Property inAlarm = newProperty(8, new BStatusBoolean(false), null);
   public static final Type TYPE = Sys.loadType(BEquipment.class);

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

   @Override
   public Type getType() {
      return TYPE;
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop.equals(demand) | prop.equals(isRunning) | prop.equals(inAlarm)) {
            if (this.getInAlarm().getValue()) {
               this.setOut(BEquipmentState.alarm);
            } else {
               int enumIndex = this.getIsRunning().getValue() ? 2 : 0;
               enumIndex = this.getDemand().getValue() ? enumIndex + 1 : enumIndex;
               this.setOut(BEquipmentState.make(enumIndex));
            }
         }
      }
   }
}
