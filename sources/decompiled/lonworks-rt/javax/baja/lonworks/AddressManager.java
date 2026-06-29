package javax.baja.lonworks;

import com.tridium.lonworks.BLonRouter;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.sys.BString;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;

public interface AddressManager {
   Context noDeviceChange = new BasicContext();

   void registerLonDevice(BLonDevice var1);

   void unregisterLonDevice(BLonDevice var1);

   void deviceDataChanged(BDeviceData var1, Context var2);

   BLonDevice[] getDeviceList(boolean var1);

   BLonDevice getDeviceByName(String var1);

   BLonDevice getDeviceByAddress(BSubnetNode var1);

   BLonDevice getDeviceByAddress(BNeuronId var1);

   BLonRouter getRouterByAddress(BSubnetNode var1);

   BLonRouter getRouterByAddress(BNeuronId var1);

   BString newAddress(BLonDevice var1, int var2, int var3, int var4);

   BLocalLonDevice getLocalDevice();

   BLonRouter[] getRouterList();
}
