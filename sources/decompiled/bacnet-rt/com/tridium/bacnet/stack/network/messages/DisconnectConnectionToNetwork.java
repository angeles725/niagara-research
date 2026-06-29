package com.tridium.bacnet.stack.network.messages;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.InvalidNetworkMsgException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class DisconnectConnectionToNetwork extends NetworkLayerMsg {
   private int networkNumber;

   public DisconnectConnectionToNetwork(ByteArrayInputStream is) throws BacnetStackException {
      super(9);
      this.readNetworkBytes(is);
   }

   public DisconnectConnectionToNetwork(int networkNumber) {
      super(9);
      this.networkNumber = networkNumber;
   }

   public int getNetworkNumber() {
      return this.networkNumber;
   }

   @Override
   public String getMsgString() {
      return "DisconnectConnectionToNetwork; network=" + this.networkNumber;
   }

   @Override
   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      if (is.available() != 2) {
         throw new InvalidNetworkMsgException();
      } else {
         this.networkNumber = is.read() << 8;
         this.networkNumber = this.networkNumber | is.read();
      }
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      super.writeNetworkBytes(os);
      os.write(this.networkNumber >> 8 & 0xFF);
      os.write(this.networkNumber & 0xFF);
   }
}
