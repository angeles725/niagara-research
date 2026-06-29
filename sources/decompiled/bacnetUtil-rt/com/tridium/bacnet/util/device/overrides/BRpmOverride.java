package com.tridium.bacnet.util.device.overrides;

import javax.baja.bacnet.config.BBacnetDeviceObject;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.device.overrides.ServiceOverride;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "useRPM",
   type = "boolean",
   defaultValue = "false"
)
public class BRpmOverride extends BServiceOverride implements ServiceOverride {
   public static final Property useRPM = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BRpmOverride.class);
   private static final int RPM_IDX = BacnetBitStringUtil.getBitIndex("BacnetServicesSupported", "readPropertyMultiple");

   public boolean getUseRPM() {
      return this.getBoolean(useRPM);
   }

   public void setUseRPM(boolean v) {
      this.setBoolean(useRPM, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void changed(Property prop, Context cx) {
      super.changed(prop, cx);
      if (prop == useRPM) {
         this.updateServices();
      }
   }

   public BBacnetBitString getProtocolServicesSupported(BBacnetDeviceObject device, BBacnetBitString claimed) {
      boolean[] bits = claimed.getBits();
      bits[RPM_IDX] = this.getUseRPM();
      return BBacnetBitString.make(bits, BacnetBitStringUtil.BACNET_SERVICES_SUPPORTED_FACETS);
   }
}
