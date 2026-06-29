package javax.baja.lonworks;

import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BINetworkVariable extends BInterface {
   Type TYPE = Sys.loadType(BINetworkVariable.class);
   int MAX_NV_INDEX = 4095;

   String getName();

   BNvConfigData getNvConfigData();

   void setNvConfigData(BNvConfigData var1);

   int getNvIndex();

   void setNvIndex(int var1);

   int getSnvtType();

   BLonData getData();

   void setUnbound();

   void receiveUpdate(byte[] var1);

   boolean isNetworkVariable();

   boolean isNetworkConfig();

   boolean isLocalNv();

   boolean isLocalNci();
}
