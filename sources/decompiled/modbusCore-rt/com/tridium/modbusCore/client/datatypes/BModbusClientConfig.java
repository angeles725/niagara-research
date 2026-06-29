package com.tridium.modbusCore.client.datatypes;

import com.tridium.modbusCore.datatypes.BModbusConfig;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "useForceMultipleCoil",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "usePresetMultipleRegister",
      type = "boolean",
      defaultValue = "false"
   )})
public class BModbusClientConfig extends BModbusConfig {
   public static final Property useForceMultipleCoil = newProperty(0, false, null);
   public static final Property usePresetMultipleRegister = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BModbusClientConfig.class);

   public boolean getUseForceMultipleCoil() {
      return this.getBoolean(useForceMultipleCoil);
   }

   public void setUseForceMultipleCoil(boolean v) {
      this.setBoolean(useForceMultipleCoil, v, null);
   }

   public boolean getUsePresetMultipleRegister() {
      return this.getBoolean(usePresetMultipleRegister);
   }

   public void setUsePresetMultipleRegister(boolean v) {
      this.setBoolean(usePresetMultipleRegister, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
