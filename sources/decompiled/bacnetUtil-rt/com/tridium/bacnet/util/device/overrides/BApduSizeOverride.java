package com.tridium.bacnet.util.device.overrides;

import javax.baja.bacnet.config.BBacnetDeviceObject;
import javax.baja.bacnet.device.overrides.ApduSizeOverride;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "maxAPDULengthAccepted",
   type = "int",
   defaultValue = "480",
   facets = {@Facet(
      name = "BFacets.MAX",
      value = "1476"
   ), @Facet(
      name = "BFacets.MIN",
      value = "50"
   )}
)
public class BApduSizeOverride extends BDeviceOverride implements ApduSizeOverride {
   public static final Property maxAPDULengthAccepted = newProperty(0, 480, BFacets.make(BFacets.make("max", 1476), BFacets.make("min", 50)));
   public static final Type TYPE = Sys.loadType(BApduSizeOverride.class);

   public int getMaxAPDULengthAccepted() {
      return this.getInt(maxAPDULengthAccepted);
   }

   public void setMaxAPDULengthAccepted(int v) {
      this.setInt(maxAPDULengthAccepted, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public int getMaxAPDULengthAccepted(BBacnetDeviceObject device) {
      return this.getMaxAPDULengthAccepted();
   }
}
