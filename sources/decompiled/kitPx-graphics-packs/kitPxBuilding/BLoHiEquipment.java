package com.tridiumemea.extras;

import com.tridiumemea.extras.enums.BLoHiStates;
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
      defaultValue = "BLoHiStates.DEFAULT",
      flags = 9
   ), @NiagaraProperty(
      name = "demandLo",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   ), @NiagaraProperty(
      name = "isRunningLo",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   ), @NiagaraProperty(
      name = "inAlarmLo",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   ), @NiagaraProperty(
      name = "demandHi",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   ), @NiagaraProperty(
      name = "isRunningHi",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   ), @NiagaraProperty(
      name = "inAlarmHi",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   )})
public class BLoHiEquipment extends BKitPxBuildingBaseEnum {
   public static final Property out = newProperty(9, BLoHiStates.DEFAULT, null);
   public static final Property demandLo = newProperty(8, new BStatusBoolean(false), null);
   public static final Property isRunningLo = newProperty(8, new BStatusBoolean(false), null);
   public static final Property inAlarmLo = newProperty(8, new BStatusBoolean(false), null);
   public static final Property demandHi = newProperty(8, new BStatusBoolean(false), null);
   public static final Property isRunningHi = newProperty(8, new BStatusBoolean(false), null);
   public static final Property inAlarmHi = newProperty(8, new BStatusBoolean(false), null);
   public static final Type TYPE = Sys.loadType(BLoHiEquipment.class);

   @Override
   public BEnum getOut() {
      return (BEnum)this.get(out);
   }

   @Override
   public void setOut(BEnum v) {
      this.set(out, v, null);
   }

   public BStatusBoolean getDemandLo() {
      return (BStatusBoolean)this.get(demandLo);
   }

   public void setDemandLo(BStatusBoolean v) {
      this.set(demandLo, v, null);
   }

   public BStatusBoolean getIsRunningLo() {
      return (BStatusBoolean)this.get(isRunningLo);
   }

   public void setIsRunningLo(BStatusBoolean v) {
      this.set(isRunningLo, v, null);
   }

   public BStatusBoolean getInAlarmLo() {
      return (BStatusBoolean)this.get(inAlarmLo);
   }

   public void setInAlarmLo(BStatusBoolean v) {
      this.set(inAlarmLo, v, null);
   }

   public BStatusBoolean getDemandHi() {
      return (BStatusBoolean)this.get(demandHi);
   }

   public void setDemandHi(BStatusBoolean v) {
      this.set(demandHi, v, null);
   }

   public BStatusBoolean getIsRunningHi() {
      return (BStatusBoolean)this.get(isRunningHi);
   }

   public void setIsRunningHi(BStatusBoolean v) {
      this.set(isRunningHi, v, null);
   }

   public BStatusBoolean getInAlarmHi() {
      return (BStatusBoolean)this.get(inAlarmHi);
   }

   public void setInAlarmHi(BStatusBoolean v) {
      this.set(inAlarmHi, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop == demandLo || prop == isRunningLo || prop == inAlarmLo || prop == demandHi || prop == isRunningHi || prop == inAlarmHi) {
            if (this.getInAlarmLo().getValue()) {
               this.setOut(BLoHiStates.alarmLo);
            } else {
               int hiStateIndex = this.getIsRunningHi().getValue() ? 2 : 0;
               hiStateIndex = this.getDemandHi().getValue() ? hiStateIndex + 1 : hiStateIndex;
               hiStateIndex = this.getInAlarmHi().getValue() ? 2 : hiStateIndex;
               int enumIndex = this.getIsRunningLo().getValue() ? 2 : 0;
               enumIndex = this.getDemandLo().getValue() ? enumIndex + 1 : enumIndex;
               enumIndex = enumIndex == 3 ? enumIndex + hiStateIndex : enumIndex;
               this.setOut(BLoHiStates.make(enumIndex));
            }
         }
      }
   }
}
