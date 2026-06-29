package com.tridium.bacnet.util.device.overrides;

import javax.baja.bacnet.config.BBacnetDeviceObject;
import javax.baja.bacnet.device.overrides.SegmentationOverride;
import javax.baja.bacnet.enums.BBacnetSegmentation;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "segmentationSupported",
   type = "BBacnetSegmentation",
   defaultValue = "BBacnetSegmentation.noSegmentation"
)
public class BSegmentationOverride extends BDeviceOverride implements SegmentationOverride {
   public static final Property segmentationSupported = newProperty(0, BBacnetSegmentation.noSegmentation, null);
   public static final Type TYPE = Sys.loadType(BSegmentationOverride.class);

   public BBacnetSegmentation getSegmentationSupported() {
      return (BBacnetSegmentation)this.get(segmentationSupported);
   }

   public void setSegmentationSupported(BBacnetSegmentation v) {
      this.set(segmentationSupported, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetSegmentation getSegmentationSupported(BBacnetDeviceObject device) {
      return this.getSegmentationSupported();
   }
}
