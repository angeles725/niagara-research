package javax.baja.bacnet.device.overrides;

public interface DeviceOverrideAware {
   boolean addDeviceOverride(DeviceOverride var1);

   boolean removeDeviceOverride(DeviceOverride var1);

   void updateServicesSupported();
}
