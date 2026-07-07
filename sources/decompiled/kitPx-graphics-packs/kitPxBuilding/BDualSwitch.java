package com.tridiumemea.extras;

import com.tridiumemea.extras.enums.BDualSwitchState;
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
      defaultValue = "BDualSwitchState.DEFAULT",
      flags = 9
   ), @NiagaraProperty(
      name = "switch1",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   ), @NiagaraProperty(
      name = "switch2",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)",
      flags = 8
   )})
public class BDualSwitch extends BKitPxBuildingBaseEnum {
   public static final Property out = newProperty(9, BDualSwitchState.DEFAULT, null);
   public static final Property switch1 = newProperty(8, new BStatusBoolean(false), null);
   public static final Property switch2 = newProperty(8, new BStatusBoolean(false), null);
   public static final Type TYPE = Sys.loadType(BDualSwitch.class);

   @Override
   public BEnum getOut() {
      return (BEnum)this.get(out);
   }

   @Override
   public void setOut(BEnum v) {
      this.set(out, v, null);
   }

   public BStatusBoolean getSwitch1() {
      return (BStatusBoolean)this.get(switch1);
   }

   public void setSwitch1(BStatusBoolean v) {
      this.set(switch1, v, null);
   }

   public BStatusBoolean getSwitch2() {
      return (BStatusBoolean)this.get(switch2);
   }

   public void setSwitch2(BStatusBoolean v) {
      this.set(switch2, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop.equals(switch1) || prop.equals(switch2)) {
            boolean switchOne = this.getSwitch1().getValue();
            boolean switchTwo = this.getSwitch2().getValue();
            int enumIndex = (switchOne ? 1 : 0) + (switchTwo ? 2 : 0);
            this.setOut(BDualSwitchState.make(enumIndex));
         }
      }
   }
}
