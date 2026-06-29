package javax.baja.bacnet.device.overrides;

import javax.baja.bacnet.config.BBacnetDeviceObject;
import javax.baja.bacnet.enums.BBacnetSegmentation;

public interface SegmentationOverride extends DeviceOverride {
   BBacnetSegmentation getSegmentationSupported(BBacnetDeviceObject var1);
}
