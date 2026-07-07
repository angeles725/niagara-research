package com.tridiumemea.extras;

import com.tridiumemea.extras.enums.BPointerStates;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.sys.BEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "out",
      type = "BEnum",
      defaultValue = "BPointerStates.DEFAULT",
      flags = 9
   ), @NiagaraProperty(
      name = "measuredInput",
      type = "BStatusNumeric",
      defaultValue = "new BStatusNumeric()",
      flags = 8
   )})
public class BPointer extends BKitPxBuildingBaseEnum {
   public static final Property out = newProperty(9, BPointerStates.DEFAULT, null);
   public static final Property measuredInput = newProperty(8, new BStatusNumeric(), null);
   public static final Type TYPE = Sys.loadType(BPointer.class);

   @Override
   public BEnum getOut() {
      return (BEnum)this.get(out);
   }

   @Override
   public void setOut(BEnum v) {
      this.set(out, v, null);
   }

   public BStatusNumeric getMeasuredInput() {
      return (BStatusNumeric)this.get(measuredInput);
   }

   public void setMeasuredInput(BStatusNumeric v) {
      this.set(measuredInput, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop.equals(BPointer.measuredInput)) {
            BPointerStates enumIndex = BPointerStates.pointer$2d90;
            BStatusNumeric measuredInput = this.getMeasuredInput();
            if (measuredInput.getValue() >= 100.0) {
               enumIndex = BPointerStates.pointer$2b90;
            } else if (measuredInput.getValue() > 87.5) {
               enumIndex = BPointerStates.pointer$2b67;
            } else if (measuredInput.getValue() > 75.0) {
               enumIndex = BPointerStates.pointer$2b45;
            } else if (measuredInput.getValue() > 62.5) {
               enumIndex = BPointerStates.pointer$2b22;
            } else if (measuredInput.getValue() > 50.0) {
               enumIndex = BPointerStates.pointer0;
            } else if (measuredInput.getValue() > 37.5) {
               enumIndex = BPointerStates.pointer$2d22;
            } else if (measuredInput.getValue() > 25.0) {
               enumIndex = BPointerStates.pointer$2d45;
            } else if (measuredInput.getValue() > 12.5) {
               enumIndex = BPointerStates.pointer$2d67;
            } else if (measuredInput.getValue() >= 0.0) {
               enumIndex = BPointerStates.pointer$2d90;
            }

            this.setOut(enumIndex);
         }
      }
   }
}
