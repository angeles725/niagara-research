package com.tridium.bacnet.stack.network.messages;

import com.tridium.bacnet.stack.BacnetStackException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class NetworkNumberIs extends NetworkLayerMsg {
   private int networkNumber;
   private boolean configured = true;
   private int recvOnNetwork;

   public NetworkNumberIs(ByteArrayInputStream is, int recvOnNetwork) throws BacnetStackException {
      super(19);
      this.readNetworkBytes(is);
      this.recvOnNetwork = recvOnNetwork;
   }

   public NetworkNumberIs(int networkNumber) {
      super(19);
      this.networkNumber = networkNumber;
   }

   public int getNetworkNumber() {
      return this.networkNumber;
   }

   public int getRecvOnNetwork() {
      return this.recvOnNetwork;
   }

   @Override
   public String getMsgString() {
      StringBuilder sb = new StringBuilder("NetworkNumberIs:");
      sb.append(this.networkNumber);
      return sb.toString();
   }

   @Override
   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      int nn = 0;
      nn = 0xFF00 & is.read() << 8;
      nn |= 0xFF & is.read();
      this.networkNumber = nn;
      int conf = is.read();
      this.configured = conf > 0;
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      super.writeNetworkBytes(os);
      os.write(this.networkNumber >> 8);
      os.write(this.networkNumber);
      if (this.configured) {
         os.write(1);
      } else {
         os.write(0);
      }
   }
}
