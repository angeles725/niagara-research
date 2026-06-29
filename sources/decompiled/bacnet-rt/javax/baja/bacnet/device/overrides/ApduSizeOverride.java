package javax.baja.bacnet.device.overrides;

import javax.baja.bacnet.config.BBacnetDeviceObject;

public interface ApduSizeOverride extends DeviceOverride {
   int getMaxAPDULengthAccepted(BBacnetDeviceObject var1);
}
