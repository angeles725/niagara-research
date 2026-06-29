package com.tridium.bacnet.util.device.overrides;

import javax.baja.bacnet.device.overrides.DeviceOverride;
import javax.baja.bacnet.device.overrides.DeviceOverrideAware;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BDeviceOverride extends BComponent implements DeviceOverride {
   public static final Type TYPE = Sys.loadType(BDeviceOverride.class);
   private static final BIcon icon = BIcon.make("module://bacnet/com/tridium/bacnet/ui/icons/override.png");

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof DeviceOverrideAware;
   }

   public void started() throws Exception {
      super.started();
      this.getTarget().addDeviceOverride(this);
   }

   public void stopped() throws Exception {
      super.stopped();
      this.getTarget().removeDeviceOverride(this);
   }

   protected DeviceOverrideAware getTarget() {
      return (DeviceOverrideAware)this.getParent();
   }

   public BIcon getIcon() {
      return icon;
   }
}
