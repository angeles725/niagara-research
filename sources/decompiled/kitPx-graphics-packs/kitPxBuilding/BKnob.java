package com.tridiumemea.extras;

import com.tridiumemea.extras.enums.BKnobPosition;
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
      defaultValue = "BKnobPosition.DEFAULT",
      flags = 9
   ), @NiagaraProperty(
      name = "knobPosition",
      type = "BStatusNumeric",
      defaultValue = "new BStatusNumeric()",
      flags = 8
   )})
public class BKnob extends BKitPxBuildingBaseEnum {
   public static final Property out = newProperty(9, BKnobPosition.DEFAULT, null);
   public static final Property knobPosition = newProperty(8, new BStatusNumeric(), null);
   public static final Type TYPE = Sys.loadType(BKnob.class);

   @Override
   public BEnum getOut() {
      return (BEnum)this.get(out);
   }

   @Override
   public void setOut(BEnum v) {
      this.set(out, v, null);
   }

   public BStatusNumeric getKnobPosition() {
      return (BStatusNumeric)this.get(knobPosition);
   }

   public void setKnobPosition(BStatusNumeric v) {
      this.set(knobPosition, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop.equals(knobPosition)) {
            BKnobPosition enumIndex = BKnobPosition.position270;
            BStatusNumeric position = this.getKnobPosition();
            if (position.getValue() >= 87.5) {
               enumIndex = BKnobPosition.position225;
            } else if (position.getValue() > 75.0) {
               enumIndex = BKnobPosition.position180;
            } else if (position.getValue() > 62.5) {
               enumIndex = BKnobPosition.position135;
            } else if (position.getValue() > 50.0) {
               enumIndex = BKnobPosition.position90;
            } else if (position.getValue() > 37.5) {
               enumIndex = BKnobPosition.position45;
            } else if (position.getValue() > 25.0) {
               enumIndex = BKnobPosition.position0;
            } else if (position.getValue() > 12.5) {
               enumIndex = BKnobPosition.position315;
            } else if (position.getValue() >= 0.0) {
               enumIndex = BKnobPosition.position270;
            }

            this.setOut(enumIndex);
         }
      }
   }
}
