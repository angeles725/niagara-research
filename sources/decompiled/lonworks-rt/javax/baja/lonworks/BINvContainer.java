package javax.baja.lonworks;

import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BInterface;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BINvContainer extends BInterface {
   Type TYPE = Sys.loadType(BINvContainer.class);
   int NOT_POLLED = 0;
   int POLLED = 1;
   int AUTH_NOT_CONFIGURABLE = 2;
   int SERVICE_NOT_CONFIGURABLE = 4;
   int PRIORITY_CONFIGURABLE = 8;
   int AUTHENTICATE = 16;
   int PRIORITY = 32;
   int SERVICE_ACKED = 64;
   int SERVICE_UNACKED_RPT = 128;
   int CONFIG_OFFLINE = 256;
   int CHANGEABLE_NV = 512;
   int CHANGEABLE_NV_CONFIG = 512;

   BComponent asComponent();

   String getName();

   String getDisplayName(Context var1);

   BINetworkVariable[] getNetworkVariables();

   BDeviceData getDeviceData();

   BLonNetwork getLonNetwork();

   BLonDevice getLonDevice();

   boolean isLonObject();

   void linkUpdate();
}
