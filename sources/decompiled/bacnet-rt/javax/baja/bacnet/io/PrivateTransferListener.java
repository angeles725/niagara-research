package javax.baja.bacnet.io;

import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;

public interface PrivateTransferListener extends BacnetServiceListener {
   int getVendorId();

   byte[] receiveConfirmedPrivateTransfer(long var1, long var3, byte[] var5, BBacnetAddress var6) throws BacnetException;

   void receiveUnconfirmedPrivateTransfer(long var1, long var3, byte[] var5, BBacnetAddress var6) throws BacnetException;
}
