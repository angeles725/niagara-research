package com.tridium.bacnet.util.device.overrides;

import javax.baja.bacnet.device.overrides.DeviceOverrideAware;
import javax.baja.bacnet.device.overrides.ServiceOverride;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BServiceOverride extends BDeviceOverride implements ServiceOverride {
   public static final Type TYPE = Sys.loadType(BServiceOverride.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.updateServices();
   }

   @Override
   public void stopped() throws Exception {
      super.stopped();
      this.updateServices();
   }

   protected void updateServices() {
      DeviceOverrideAware target = this.getTarget();
      if (target != null) {
         target.updateServicesSupported();
      }
   }
}
