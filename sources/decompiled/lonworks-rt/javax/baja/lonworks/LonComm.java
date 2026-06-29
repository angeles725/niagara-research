package javax.baja.lonworks;

import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.datatypes.LonAddress;

public interface LonComm {
   LonMessage sendRequest(LonAddress var1, LonMessage var2) throws LonException;

   void sendAcked(LonAddress var1, LonMessage var2) throws LonException;

   void sendResponse(LonMessage var1, LonMessage var2);

   void sendUnacknowledged(LonAddress var1, LonMessage var2) throws LonException;

   void sendUnackRepeat(LonAddress var1, LonMessage var2) throws LonException;

   void registerLonListener(LonListener var1, int var2, BSubnetNode var3, Class<?> var4);

   void unregisterLonListener(LonListener var1, int var2, BSubnetNode var3);

   BLonNetwork lonNetwork();
}
