package javax.baja.bacnet.device.overrides;

import javax.baja.bacnet.config.BBacnetDeviceObject;
import javax.baja.bacnet.datatypes.BBacnetBitString;

public interface ServiceOverride extends DeviceOverride {
   BBacnetBitString getProtocolServicesSupported(BBacnetDeviceObject var1, BBacnetBitString var2);
}
