package com.tridium.bacnet.util.device.overrides;

import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.config.BBacnetDeviceObject;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.device.overrides.ServiceOverride;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "protocolServicesSupported",
   type = "BBacnetBitString",
   defaultValue = "NO_SERVICES_SUPPORTED",
   facets = {@Facet("BacnetBitStringUtil.BACNET_SERVICES_SUPPORTED_FACETS")}
)
@NiagaraAction(
   name = "reset"
)
public class BServicesSupportedOverride extends BServiceOverride implements ServiceOverride {
   private static final BBacnetBitString NO_SERVICES_SUPPORTED = BBacnetBitString.make(
      new boolean[]{
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false
      }
   );
   public static final Property protocolServicesSupported = newProperty(0, NO_SERVICES_SUPPORTED, BacnetBitStringUtil.BACNET_SERVICES_SUPPORTED_FACETS);
   public static final Action reset = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BServicesSupportedOverride.class);

   public BBacnetBitString getProtocolServicesSupported() {
      return (BBacnetBitString)this.get(protocolServicesSupported);
   }

   public void setProtocolServicesSupported(BBacnetBitString v) {
      this.set(protocolServicesSupported, v, null);
   }

   public void reset() {
      this.invoke(reset, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void changed(Property prop, Context cx) {
      super.changed(prop, cx);
      if (prop == protocolServicesSupported) {
         this.updateServices();
      }
   }

   public void doReset() {
      BObject parent = null;
      BComplex var3;
      if ((var3 = this.getParent()) instanceof BBacnetDevice) {
         BBacnetDeviceObject device = ((BBacnetDevice)var3).getConfig().getDeviceObject();
         if (device != null) {
            this.setProtocolServicesSupported(device.getProtocolServicesSupported());
         }
      } else {
         this.setProtocolServicesSupported(NO_SERVICES_SUPPORTED);
      }
   }

   public BBacnetBitString getProtocolServicesSupported(BBacnetDeviceObject device, BBacnetBitString claimed) {
      return this.getProtocolServicesSupported();
   }
}
